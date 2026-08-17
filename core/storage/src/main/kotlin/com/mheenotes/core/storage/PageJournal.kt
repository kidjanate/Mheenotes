package com.mheenotes.core.storage

import com.mheenotes.core.commands.AddStroke
import com.mheenotes.core.commands.AddStrokes
import com.mheenotes.core.commands.DeleteStroke
import com.mheenotes.core.commands.DeleteStrokes
import com.mheenotes.core.commands.PageCommand
import com.mheenotes.core.commands.ReplaceStroke
import com.mheenotes.core.commands.ReplaceStrokes
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.PressureCurve
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32

data class JournalLoadResult(
    val page: PageState,
    val recoveredTail: Boolean,
    val operationCount: Long,
)

class PageJournal(
    private val file: File,
    private val pageId: PageId,
) {
    private var nextSequence = 1L

    @Synchronized
    fun load(): JournalLoadResult {
        if (!file.exists() || file.length() == 0L) {
            nextSequence = 1L
            return JournalLoadResult(PageState(pageId), recoveredTail = false, operationCount = 0)
        }

        var page = PageState(pageId)
        var recoveredTail = false
        var operationCount = 0L
        var lastValidOffset = 0L

        RandomAccessFile(file, "r").use { input ->
            if (!readHeader(input)) {
                throw CorruptJournalException("Invalid journal header")
            }
            lastValidOffset = input.filePointer

            while (input.filePointer < input.length()) {
                val remainingBeforeLength = input.length() - input.filePointer
                if (remainingBeforeLength < Int.SIZE_BYTES) {
                    recoveredTail = true
                    break
                }

                val recordSize = input.readInt()
                if (recordSize !in 1..MaximumRecordBytes) {
                    recoveredTail = true
                    break
                }
                if (input.length() - input.filePointer < recordSize + Int.SIZE_BYTES) {
                    recoveredTail = true
                    break
                }

                val payload = ByteArray(recordSize)
                input.readFully(payload)
                val expectedCrc = input.readInt().toLong() and UnsignedIntMask
                val actualCrc = CRC32().apply { update(payload) }.value
                if (actualCrc != expectedCrc) {
                    recoveredTail = true
                    break
                }

                val record = try {
                    decodeRecord(payload)
                } catch (_: Exception) {
                    recoveredTail = true
                    break
                }
                if (record.sequence != operationCount + 1) {
                    recoveredTail = true
                    break
                }

                page = try {
                    record.command.apply(page)
                } catch (_: Exception) {
                    recoveredTail = true
                    break
                }
                operationCount = record.sequence
                lastValidOffset = input.filePointer
            }
        }

        if (recoveredTail) quarantineAndTruncateTail(lastValidOffset)

        nextSequence = operationCount + 1
        return JournalLoadResult(page, recoveredTail, operationCount)
    }

    @Synchronized
    fun append(command: PageCommand): Long {
        ensureHeader()
        val sequence = nextSequence
        val payload = encodeRecord(sequence, command)
        val crc = CRC32().apply { update(payload) }.value.toInt()

        FileOutputStream(file, true).use { stream ->
            DataOutputStream(stream).use { output ->
                output.writeInt(payload.size)
                output.write(payload)
                output.writeInt(crc)
                output.flush()
                stream.fd.sync()
            }
        }
        nextSequence += 1
        return sequence
    }

    private fun ensureHeader() {
        if (file.exists() && file.length() > 0L) return
        file.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "Cannot create journal directory" }
        }
        FileOutputStream(file, false).use { stream ->
            DataOutputStream(stream).use { output ->
                output.write(Magic)
                output.writeInt(FormatVersion)
                output.writeUTF(pageId.value)
                output.flush()
                stream.fd.sync()
            }
        }
    }

    private fun quarantineAndTruncateTail(validLength: Long) {
        val tailLength = file.length() - validLength
        if (tailLength <= 0) return
        val quarantine = File(file.parentFile, "${file.name}.recovered-tail-${System.currentTimeMillis()}")
        val temporary = File(quarantine.parentFile, "${quarantine.name}.partial")

        RandomAccessFile(file, "r").use { input ->
            input.seek(validLength)
            FileOutputStream(temporary, false).use { stream ->
                val buffer = ByteArray(8 * 1024)
                var remaining = tailLength
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    check(read > 0) { "Unable to preserve damaged journal tail" }
                    stream.write(buffer, 0, read)
                    remaining -= read
                }
                stream.flush()
                stream.fd.sync()
            }
        }
        check(temporary.renameTo(quarantine)) { "Unable to publish recovered journal tail" }
        RandomAccessFile(file, "rw").use { repaired ->
            repaired.setLength(validLength)
            repaired.fd.sync()
        }
    }

    private fun readHeader(input: RandomAccessFile): Boolean {
        if (input.length() < Magic.size + Int.SIZE_BYTES) return false
        val magic = ByteArray(Magic.size)
        input.readFully(magic)
        if (!magic.contentEquals(Magic)) return false
        if (input.readInt() != FormatVersion) return false
        return runCatching { input.readUTF() == pageId.value }.getOrDefault(false)
    }

    private fun encodeRecord(sequence: Long, command: PageCommand): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeLong(sequence)
            when (command) {
                is AddStroke -> {
                    output.writeByte(AddStrokeTag)
                    output.writeStroke(command.stroke)
                }
                is AddStrokes -> {
                    output.writeByte(AddStrokesTag)
                    output.writeInt(command.strokes.size)
                    command.strokes.forEach { output.writeStroke(it) }
                }
                is DeleteStroke -> {
                    output.writeByte(DeleteStrokeTag)
                    output.writeUTF(command.strokeId.value)
                }
                is DeleteStrokes -> {
                    output.writeByte(DeleteStrokesTag)
                    output.writeInt(command.strokeIds.size)
                    command.strokeIds.forEach { output.writeUTF(it.value) }
                }
                is ReplaceStroke -> {
                    output.writeByte(ReplaceStrokeTag)
                    output.writeUTF(command.strokeId.value)
                    output.writeInt(command.replacements.size)
                    command.replacements.forEach { replacement -> output.writeStroke(replacement) }
                }
                is ReplaceStrokes -> {
                    output.writeByte(ReplaceStrokesTag)
                    output.writeInt(command.strokeIds.size)
                    command.strokeIds.forEach { output.writeUTF(it.value) }
                    output.writeInt(command.replacements.size)
                    command.replacements.forEach { output.writeStroke(it) }
                }
            }
        }
        return bytes.toByteArray()
    }

    private fun decodeRecord(payload: ByteArray): JournalRecord {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val sequence = input.readLong()
            require(sequence > 0)
            val command = when (input.readUnsignedByte()) {
                AddStrokeTag -> AddStroke(input.readStroke())
                DeleteStrokeTag -> DeleteStroke(StrokeId(input.readUTF()))
                DeleteStrokesTag -> {
                    val count = input.readInt()
                    require(count in 1..MaximumReplacementStrokes)
                    DeleteStrokes(List(count) { StrokeId(input.readUTF()) }.toSet())
                }
                ReplaceStrokeTag -> {
                    val strokeId = StrokeId(input.readUTF())
                    val count = input.readInt()
                    require(count in 0..MaximumReplacementStrokes)
                    ReplaceStroke(strokeId, List(count) { input.readStroke() })
                }
                AddStrokesTag -> {
                    val count = input.readInt()
                    require(count in 1..MaximumReplacementStrokes)
                    AddStrokes(List(count) { input.readStroke() })
                }
                ReplaceStrokesTag -> {
                    val oldCount = input.readInt()
                    require(oldCount in 1..MaximumReplacementStrokes)
                    val ids = List(oldCount) { StrokeId(input.readUTF()) }.toSet()
                    val replacementCount = input.readInt()
                    require(replacementCount in 0..MaximumReplacementStrokes)
                    ReplaceStrokes(ids, List(replacementCount) { input.readStroke() })
                }
                else -> error("Unknown journal operation")
            }
            require(input.available() == 0) { "Unexpected trailing journal data" }
            return JournalRecord(sequence, command)
        }
    }

    private fun DataOutputStream.writeStroke(stroke: InkStroke) {
        writeUTF(stroke.id.value)
        writeUTF(stroke.pageId.value)
        writeLong(stroke.zIndex)
        writeInt(stroke.style.tool.ordinal)
        writeInt(stroke.style.argb)
        writeFloat(stroke.style.minimumWidth)
        writeFloat(stroke.style.maximumWidth)
        writeFloat(stroke.style.opacity)
        writeBoolean(stroke.style.pressureEnabled)
        writeFloat(stroke.style.pressureCurve.minimumUsefulPressure)
        writeFloat(stroke.style.pressureCurve.maximumUsefulPressure)
        writeFloat(stroke.style.pressureCurve.exponent)
        writeInt(stroke.samples.size)
        stroke.samples.forEach { sample ->
            writeFloat(sample.position.x)
            writeFloat(sample.position.y)
            writeLong(sample.elapsedMicros)
            writeOptionalFloat(sample.pressure)
            writeOptionalFloat(sample.tiltRadians)
            writeOptionalFloat(sample.orientationRadians)
        }
    }

    private fun DataInputStream.readStroke(): InkStroke {
        val strokeId = StrokeId(readUTF())
        val storedPageId = PageId(readUTF())
        require(storedPageId == pageId) { "Stroke belongs to another journal" }
        val zIndex = readLong()
        val toolOrdinal = readInt()
        require(toolOrdinal in InkTool.entries.indices)
        val style = StrokeStyle(
            tool = InkTool.entries[toolOrdinal],
            argb = readInt(),
            minimumWidth = readFloat(),
            maximumWidth = readFloat(),
            opacity = readFloat(),
            pressureEnabled = readBoolean(),
            pressureCurve = PressureCurve(
                minimumUsefulPressure = readFloat(),
                maximumUsefulPressure = readFloat(),
                exponent = readFloat(),
            ),
        )
        val sampleCount = readInt()
        require(sampleCount in 1..MaximumSamplesPerStroke)
        val samples = List(sampleCount) {
            StrokeSample(
                position = PagePoint(readFloat(), readFloat()),
                elapsedMicros = readLong(),
                pressure = readOptionalFloat(),
                tiltRadians = readOptionalFloat(),
                orientationRadians = readOptionalFloat(),
            )
        }
        return InkStroke(
            id = strokeId,
            pageId = storedPageId,
            style = style,
            samples = samples,
            bounds = PageRect.enclosing(samples),
            zIndex = zIndex,
        )
    }

    private fun DataOutputStream.writeOptionalFloat(value: Float?) {
        writeBoolean(value != null)
        if (value != null) writeFloat(value)
    }

    private fun DataInputStream.readOptionalFloat(): Float? =
        if (readBoolean()) readFloat() else null

    private data class JournalRecord(val sequence: Long, val command: PageCommand)

    companion object {
        private val Magic = byteArrayOf(0x4D, 0x48, 0x4A, 0x31)
        private const val FormatVersion = 1
        private const val AddStrokeTag = 1
        private const val DeleteStrokeTag = 2
        private const val ReplaceStrokeTag = 3
        private const val DeleteStrokesTag = 4
        private const val AddStrokesTag = 5
        private const val ReplaceStrokesTag = 6
        private const val MaximumRecordBytes = 32 * 1024 * 1024
        private const val MaximumSamplesPerStroke = 1_000_000
        private const val MaximumReplacementStrokes = 10_000
        private const val UnsignedIntMask = 0xFFFF_FFFFL
    }
}

class CorruptJournalException(message: String) : Exception(message)
