package com.mheenotes.core.storage

import com.mheenotes.core.model.ActionMapping
import com.mheenotes.core.model.GestureDrawing
import com.mheenotes.core.model.GesturePoint
import com.mheenotes.core.model.MappedAction
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ActionMapStore(private val file: File) {
    fun load(): List<ActionMapping> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
            require(input.readInt() == Magic) { "Invalid Action Map file" }
            require(input.readInt() == Version) { "Unsupported Action Map version" }
            val count = input.readInt()
            require(count in 0..MaximumMappings)
            List(count) {
                val id = input.readUTF()
                val name = input.readUTF()
                val actionOrdinal = input.readInt()
                require(actionOrdinal in MappedAction.entries.indices)
                val rotationInvariant = input.readBoolean()
                val confidenceThreshold = input.readFloat()
                val exampleCount = input.readInt()
                require(exampleCount in 3..MaximumExamples)
                ActionMapping(
                    id = id,
                    name = name,
                    action = MappedAction.entries[actionOrdinal],
                    examples = List(exampleCount) { input.readDrawing() },
                    rotationInvariant = rotationInvariant,
                    confidenceThreshold = confidenceThreshold,
                )
            }.also { require(input.read() == -1) { "Unexpected Action Map data" } }
        }
    }

    fun save(mappings: List<ActionMapping>) {
        require(mappings.size <= MaximumMappings)
        file.parentFile?.let { check(it.exists() || it.mkdirs()) }
        val temporary = File(file.parentFile, "${file.name}.partial")
        FileOutputStream(temporary).use { stream ->
            DataOutputStream(BufferedOutputStream(stream)).use { output ->
                output.writeInt(Magic)
                output.writeInt(Version)
                output.writeInt(mappings.size)
                mappings.forEach { mapping ->
                    output.writeUTF(mapping.id)
                    output.writeUTF(mapping.name)
                    output.writeInt(mapping.action.ordinal)
                    output.writeBoolean(mapping.rotationInvariant)
                    output.writeFloat(mapping.confidenceThreshold)
                    output.writeInt(mapping.examples.size)
                    mapping.examples.forEach { drawing -> output.writeDrawing(drawing) }
                }
                output.flush()
                stream.fd.sync()
            }
        }
        if (file.exists()) check(file.delete()) { "Cannot replace Action Maps" }
        check(temporary.renameTo(file)) { "Cannot publish Action Maps" }
    }

    private fun DataInputStream.readDrawing(): GestureDrawing {
        val strokeCount = readInt()
        require(strokeCount in 1..MaximumStrokes)
        return GestureDrawing(List(strokeCount) {
            val pointCount = readInt()
            require(pointCount in 1..MaximumPoints)
            List(pointCount) { GesturePoint(readFloat(), readFloat()) }
        })
    }

    private fun DataOutputStream.writeDrawing(drawing: GestureDrawing) {
        require(drawing.strokes.size <= MaximumStrokes)
        writeInt(drawing.strokes.size)
        drawing.strokes.forEach { stroke ->
            require(stroke.size <= MaximumPoints)
            writeInt(stroke.size)
            stroke.forEach { point -> writeFloat(point.x); writeFloat(point.y) }
        }
    }

    private companion object {
        const val Magic = 0x4D48414D
        const val Version = 1
        const val MaximumMappings = 100
        const val MaximumExamples = 12
        const val MaximumStrokes = 16
        const val MaximumPoints = 20_000
    }
}
