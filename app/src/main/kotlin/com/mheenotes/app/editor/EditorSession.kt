package com.mheenotes.app.editor

import android.view.MotionEvent
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mheenotes.core.commands.AddStroke
import com.mheenotes.core.commands.AddStrokes
import com.mheenotes.core.commands.PageHistory
import com.mheenotes.core.commands.DeleteStroke
import com.mheenotes.core.commands.DeleteStrokes
import com.mheenotes.core.commands.ReplaceStroke
import com.mheenotes.core.commands.ReplaceStrokes
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import com.mheenotes.ink.engine.PressureNormalizer
import com.mheenotes.ink.engine.ScribbleEraseDetector
import com.mheenotes.ink.engine.ScribbleSensitivity
import com.mheenotes.ink.engine.StrokeHitTest
import com.mheenotes.ink.engine.StrokeSpatialIndex
import com.mheenotes.ink.engine.SmoothingLevel
import com.mheenotes.ink.engine.StrokeSmoother
import com.mheenotes.core.storage.PageJournal
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

data class ViewportTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val pageWidth: Float,
    val pageHeight: Float,
) {
    fun toPage(x: Float, y: Float): PagePoint = PagePoint(
        x = (x - offsetX) / scale,
        y = (y - offsetY) / scale,
    )

    fun toScreen(point: PagePoint): PagePoint = PagePoint(
        x = offsetX + point.x * scale,
        y = offsetY + point.y * scale,
    )
}

data class StylusDiagnostics(
    val action: String = "Waiting for stylus",
    val toolType: String = "—",
    val x: Float = 0f,
    val y: Float = 0f,
    val rawPressure: Float? = null,
    val normalizedPressure: Float? = null,
    val tiltRadians: Float? = null,
    val orientationRadians: Float? = null,
    val distance: Float? = null,
    val buttonState: Int = 0,
    val pointerCount: Int = 0,
    val deviceId: Int = 0,
    val source: Int = 0,
    val historicalCount: Int = 0,
)

sealed interface SaveStatus {
    data class Saved(val operation: Long) : SaveStatus
    data object Saving : SaveStatus
    data class Failed(val message: String) : SaveStatus
}

enum class EraserMode { WholeStroke, Partial }

enum class ToolPreset { BlackPen, BluePen, Pencil, YellowHighlighter }

class EditorSession(
    journalFile: File? = null,
    private val pageId: PageId = PageId("milestone-1-page"),
    private val onEditRecorded: (() -> Unit)? = null,
) : AutoCloseable {
    private val journal = journalFile?.let { PageJournal(it, pageId) }
    private val loaded = journal?.load()
    private val history = PageHistory(loaded?.page ?: PageState(pageId))
    private val writer = Executors.newSingleThreadExecutor { work ->
        Thread(work, "mheenotes-autosave").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val spatialIndex = StrokeSpatialIndex().apply { rebuild(history.current.strokes) }
    private var nextZIndex = (history.current.strokes.maxOfOrNull { it.zIndex } ?: -1L) + 1L

    var page by mutableStateOf(history.current)
        private set

    var pressureEnabled by mutableStateOf(true)
        private set

    var selectedTool by mutableStateOf(InkTool.Pen)
        private set

    var eraserMode by mutableStateOf(EraserMode.WholeStroke)
        private set

    var scribbleEraseEnabled by mutableStateOf(true)
        private set

    var scribbleSensitivity by mutableStateOf(ScribbleSensitivity.Medium)
        private set

    var lastScribbleEraseCount by mutableStateOf(0)
        private set

    var smoothingLevel by mutableStateOf(SmoothingLevel.Low)
        private set

    private var penWidthMicrons by mutableStateOf(1_400f)
    private var pencilWidthMicrons by mutableStateOf(1_200f)
    private var highlighterWidthMicrons by mutableStateOf(4_000f)
    private var eraserWidthMicrons by mutableStateOf(5_000f)
    private var toolDefaultsInitialized = false
    var maximumToolWidthMicrons by mutableStateOf(5_000f)
        private set

    val widthScale: Float
        get() = toolWidthMicrons(selectedTool) / maximumToolWidthMicrons

    var pressureExponent by mutableStateOf(0.72f)
        private set

    var penColor by mutableStateOf(0xFF111827.toInt())
        private set

    var pencilColor by mutableStateOf(0xFF30343B.toInt())
        private set

    var highlighterColor by mutableStateOf(0xFFF7D34A.toInt())
        private set

    var observedPressureMinimum by mutableStateOf(Float.POSITIVE_INFINITY)
        private set

    var observedPressureMaximum by mutableStateOf(0f)
        private set

    var calibratedPressureMinimum by mutableStateOf(0.02f)
        private set

    var calibratedPressureMaximum by mutableStateOf(0.85f)
        private set

    var diagnostics by mutableStateOf(StylusDiagnostics())
        private set

    var saveStatus by mutableStateOf<SaveStatus>(
        SaveStatus.Saved(loaded?.operationCount ?: 0),
    )
        private set

    val recoveryAvailable: Boolean = loaded?.recoveredTail == true

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo

    fun updatePressureEnabled(enabled: Boolean) {
        pressureEnabled = enabled
    }

    fun selectTool(tool: InkTool) {
        selectedTool = tool
    }

    fun toggleEraserMode() {
        eraserMode = if (eraserMode == EraserMode.WholeStroke) {
            EraserMode.Partial
        } else {
            EraserMode.WholeStroke
        }
    }

    fun updateScribbleEraseEnabled(enabled: Boolean) {
        scribbleEraseEnabled = enabled
    }

    fun updateScribbleSensitivity(sensitivity: ScribbleSensitivity) {
        scribbleSensitivity = sensitivity
    }

    fun setSmoothing(level: SmoothingLevel) {
        smoothingLevel = level
    }

    fun updateWidthScale(scale: Float) {
        updateToolWidth(selectedTool, scale * maximumToolWidthMicrons)
    }

    fun toolWidthMicrons(tool: InkTool): Float = when (tool) {
        InkTool.Pen -> penWidthMicrons
        InkTool.Pencil -> pencilWidthMicrons
        InkTool.Highlighter -> highlighterWidthMicrons
        InkTool.Eraser -> eraserWidthMicrons
    }

    fun updateToolWidth(tool: InkTool, widthMicrons: Float) {
        val clamped = widthMicrons.coerceIn(100f, maximumToolWidthMicrons)
        when (tool) {
            InkTool.Pen -> penWidthMicrons = clamped
            InkTool.Pencil -> pencilWidthMicrons = clamped
            InkTool.Highlighter -> highlighterWidthMicrons = clamped
            InkTool.Eraser -> eraserWidthMicrons = clamped
        }
    }

    fun updateMaximumToolWidth(widthMicrons: Float) {
        maximumToolWidthMicrons = widthMicrons.coerceIn(1_000f, 20_000f)
        InkTool.entries.forEach { tool -> updateToolWidth(tool, toolWidthMicrons(tool)) }
    }

    fun initializeToolDefaults(
        penWidthMicrons: Float,
        pencilWidthMicrons: Float,
        highlighterWidthMicrons: Float,
        penArgb: Int,
        pencilArgb: Int,
        highlighterArgb: Int,
    ) {
        if (toolDefaultsInitialized) return
        this.penWidthMicrons = penWidthMicrons.coerceIn(100f, maximumToolWidthMicrons)
        this.pencilWidthMicrons = pencilWidthMicrons.coerceIn(100f, maximumToolWidthMicrons)
        this.highlighterWidthMicrons = highlighterWidthMicrons.coerceIn(100f, maximumToolWidthMicrons)
        penColor = penArgb
        pencilColor = pencilArgb
        highlighterColor = highlighterArgb
        toolDefaultsInitialized = true
    }

    fun updatePressureExponent(exponent: Float) {
        pressureExponent = exponent.coerceIn(0.35f, 2.5f)
    }

    fun setCurrentColor(argb: Int) {
        when (selectedTool) {
            InkTool.Pen -> penColor = argb
            InkTool.Pencil -> pencilColor = argb
            InkTool.Highlighter -> highlighterColor = argb
            InkTool.Eraser -> Unit
        }
    }

    fun applyPreset(preset: ToolPreset) {
        when (preset) {
            ToolPreset.BlackPen -> {
                selectedTool = InkTool.Pen
                penColor = 0xFF111827.toInt()
                penWidthMicrons = 1_400f.coerceAtMost(maximumToolWidthMicrons)
                pressureEnabled = true
            }
            ToolPreset.BluePen -> {
                selectedTool = InkTool.Pen
                penColor = 0xFF2563EB.toInt()
                penWidthMicrons = 1_150f.coerceAtMost(maximumToolWidthMicrons)
                pressureEnabled = true
            }
            ToolPreset.Pencil -> {
                selectedTool = InkTool.Pencil
                pencilColor = 0xFF30343B.toInt()
                pencilWidthMicrons = 1_800f.coerceAtMost(maximumToolWidthMicrons)
                pressureEnabled = true
            }
            ToolPreset.YellowHighlighter -> {
                selectedTool = InkTool.Highlighter
                highlighterColor = 0xFFF7D34A.toInt()
                highlighterWidthMicrons = 5_000f.coerceAtMost(maximumToolWidthMicrons)
                pressureEnabled = false
            }
        }
    }

    fun applyObservedPressureCalibration() {
        val minimum = observedPressureMinimum
        val maximum = observedPressureMaximum
        if (minimum.isFinite() && maximum > minimum + 0.01f) {
            calibratedPressureMinimum = minimum.coerceAtLeast(0f)
            calibratedPressureMaximum = maximum
        }
    }

    fun undo() {
        val before = page
        page = history.undo()
        spatialIndex.rebuild(page.strokes)
        persistDifference(before, page)
    }

    fun redo() {
        val before = page
        page = history.redo()
        spatialIndex.rebuild(page.strokes)
        persistDifference(before, page)
    }

    fun observeMotionEvent(event: MotionEvent) {
        updateDiagnostics(event)
    }

    fun acceptCompletedStroke(
        samples: List<StrokeSample>,
        style: StrokeStyle,
        detectScribbleErase: Boolean = true,
    ): StrokeId? {
        if (samples.isEmpty()) return null
        if (detectScribbleErase && eraseScribbleIfDetected(samples, style)) {
            return null
        }
        lastScribbleEraseCount = 0
        val stroke = InkStroke(
            id = StrokeId(UUID.randomUUID().toString()),
            pageId = pageId,
            style = style,
            samples = samples,
            bounds = PageRect.enclosing(samples),
            zIndex = nextZIndex++,
        )
        val command = AddStroke(stroke)
        page = execute(command)
        spatialIndex.insert(stroke)
        persist(command)
        return stroke.id
    }

    fun eraseScribbleIfDetected(
        samples: List<StrokeSample>,
        style: StrokeStyle,
        alreadySmoothed: Boolean = false,
    ): Boolean {
        if (samples.isEmpty()) return false
        val smoothedSamples = if (alreadySmoothed) samples else StrokeSmoother.smooth(samples, smoothingLevel)
        if (scribbleEraseEnabled && (style.tool == InkTool.Pen || style.tool == InkTool.Pencil) &&
            ScribbleEraseDetector.isScribble(smoothedSamples, scribbleSensitivity)
        ) {
            val path = smoothedSamples.map { it.position }
            val bounds = PageRect.enclosing(smoothedSamples).expanded(1_800f)
            val hitIds = spatialIndex.query(bounds)
                .filter { StrokeHitTest.intersectsPath(it, path, 1_200f) }
                .mapTo(linkedSetOf()) { it.id }
            if (hitIds.isNotEmpty()) {
                val command = DeleteStrokes(hitIds)
                page = execute(command)
                hitIds.forEach(spatialIndex::remove)
                lastScribbleEraseCount = hitIds.size
                persist(command)
                return true
            }
        }
        lastScribbleEraseCount = 0
        return false
    }

    fun eraseAt(point: PagePoint) {
        eraseAt(point, currentStyle(InkTool.Eraser).maximumWidth / 2f)
    }

    fun eraseAt(point: PagePoint, radius: Float) {
        val query = PageRect(
            point.x - radius,
            point.y - radius,
            point.x + radius,
            point.y + radius,
        )
        val candidates = spatialIndex.query(query).filter {
            StrokeHitTest.intersectsCircle(it, point, radius)
        }
        candidates.forEach { stroke ->
            when (eraserMode) {
                EraserMode.WholeStroke -> {
                    val command = DeleteStroke(stroke.id)
                    page = execute(command)
                    spatialIndex.remove(stroke.id)
                    persist(command)
                }
                EraserMode.Partial -> {
                    val fragments = StrokeHitTest.outsideFragments(stroke, point, radius)
                    val replacements = fragments.mapIndexed { index, samples ->
                        stroke.copy(
                            id = StrokeId(UUID.randomUUID().toString()),
                            samples = samples,
                            bounds = PageRect.enclosing(samples),
                            zIndex = stroke.zIndex + index,
                        )
                    }
                    val command = ReplaceStroke(stroke.id, replacements)
                    page = execute(command)
                    spatialIndex.remove(stroke.id)
                    replacements.forEach(spatialIndex::insert)
                    persist(command)
                }
            }
        }
    }

    fun deleteStrokes(ids: Set<StrokeId>) {
        val existing = page.strokes.filter { it.id in ids }.mapTo(linkedSetOf(), InkStroke::id)
        if (existing.isEmpty()) return
        val command = DeleteStrokes(existing)
        page = execute(command)
        existing.forEach(spatialIndex::remove)
        persist(command)
    }

    fun duplicateStrokes(ids: Set<StrokeId>, offset: Float = 5_000f) {
        pasteStrokes(page.strokes.filter { it.id in ids }, offset)
    }

    fun pasteStrokes(strokes: Collection<InkStroke>, offset: Float = 5_000f): Set<StrokeId> {
        if (strokes.isEmpty()) return emptySet()
        val copies = strokes.map { stroke ->
            val samples = stroke.samples.map { sample ->
                sample.copy(position = PagePoint(sample.position.x + offset, sample.position.y + offset))
            }
            stroke.copy(
                id = StrokeId(UUID.randomUUID().toString()),
                pageId = pageId,
                samples = samples,
                bounds = PageRect.enclosing(samples),
                zIndex = nextZIndex++,
            )
        }
        val command = AddStrokes(copies)
        page = execute(command)
        copies.forEach(spatialIndex::insert)
        persist(command)
        return copies.mapTo(linkedSetOf(), InkStroke::id)
    }

    fun replaceStrokes(
        ids: Set<StrokeId>,
        operation: (InkStroke) -> InkStroke,
    ): Set<StrokeId> {
        if (ids.isEmpty()) return emptySet()
        val replacements = page.strokes.filter { it.id in ids }.map { original ->
            val changed = operation(original)
            changed.copy(
                id = StrokeId(UUID.randomUUID().toString()),
                pageId = pageId,
                bounds = PageRect.enclosing(changed.samples),
            )
        }
        if (replacements.isEmpty()) return emptySet()
        val command = ReplaceStrokes(ids, replacements)
        page = execute(command)
        ids.forEach(spatialIndex::remove)
        replacements.forEach(spatialIndex::insert)
        persist(command)
        return replacements.mapTo(linkedSetOf(), InkStroke::id)
    }

    private fun persistDifference(before: PageState, after: PageState) {
        val beforeById = before.strokes.associateBy { it.id }
        val afterById = after.strokes.associateBy { it.id }
        beforeById.keys.minus(afterById.keys).forEach { persist(com.mheenotes.core.commands.DeleteStroke(it)) }
        afterById.keys.minus(beforeById.keys).forEach { persist(AddStroke(afterById.getValue(it))) }
    }

    private fun execute(command: com.mheenotes.core.commands.PageCommand): PageState {
        onEditRecorded?.invoke()
        return history.execute(command)
    }

    private fun persist(command: com.mheenotes.core.commands.PageCommand) {
        val target = journal ?: return
        saveStatus = SaveStatus.Saving
        writer.execute {
            runCatching { target.append(command) }
                .onSuccess { sequence ->
                    mainHandler.post { saveStatus = SaveStatus.Saved(sequence) }
                }
                .onFailure { error ->
                    mainHandler.post {
                        saveStatus = SaveStatus.Failed(error.message ?: "Autosave failed")
                    }
                }
        }
    }

    override fun close() {
        writer.shutdown()
    }

    fun currentStyle(tool: InkTool = selectedTool): StrokeStyle {
        val curve = com.mheenotes.core.model.PressureCurve(
            calibratedPressureMinimum,
            calibratedPressureMaximum,
            pressureExponent,
        )
        return when (tool) {
        InkTool.Pen -> StrokeStyle(
            tool = InkTool.Pen,
            argb = penColor,
            minimumWidth = if (pressureEnabled) penWidthMicrons * .43f else penWidthMicrons,
            maximumWidth = penWidthMicrons,
            pressureEnabled = pressureEnabled,
            pressureCurve = curve,
        )
        InkTool.Pencil -> StrokeStyle(
            tool = InkTool.Pencil,
            argb = pencilColor,
            minimumWidth = if (pressureEnabled) pencilWidthMicrons * .28f else pencilWidthMicrons,
            maximumWidth = pencilWidthMicrons,
            opacity = 0.72f,
            pressureEnabled = pressureEnabled,
            pressureCurve = curve,
        )
        InkTool.Highlighter -> StrokeStyle(
            tool = InkTool.Highlighter,
            argb = highlighterColor,
            minimumWidth = highlighterWidthMicrons,
            maximumWidth = highlighterWidthMicrons,
            opacity = 0.34f,
            pressureEnabled = false,
            pressureCurve = curve,
        )
        InkTool.Eraser -> StrokeStyle(
            tool = InkTool.Eraser,
            minimumWidth = eraserWidthMicrons,
            maximumWidth = eraserWidthMicrons,
            pressureEnabled = false,
            pressureCurve = curve,
        )
        }
    }

    private fun updateDiagnostics(event: MotionEvent) {
        val index = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val style = currentStyle()
        val rawPressure = event.getPressure(index).takeIf(Float::isFinite)
        if (rawPressure != null && rawPressure > 0f && event.getToolType(index) == MotionEvent.TOOL_TYPE_STYLUS) {
            observedPressureMinimum = minOf(observedPressureMinimum, rawPressure)
            observedPressureMaximum = maxOf(observedPressureMaximum, rawPressure)
        }
        diagnostics = StylusDiagnostics(
            action = MotionEvent.actionToString(event.action),
            toolType = toolName(event.getToolType(index)),
            x = event.getX(index),
            y = event.getY(index),
            rawPressure = rawPressure,
            normalizedPressure = rawPressure?.let {
                PressureNormalizer.normalize(it, style.pressureCurve)
            },
            tiltRadians = event.getAxisValue(MotionEvent.AXIS_TILT, index).takeIf(Float::isFinite),
            orientationRadians = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, index)
                .takeIf(Float::isFinite),
            distance = event.getAxisValue(MotionEvent.AXIS_DISTANCE, index).takeIf(Float::isFinite),
            buttonState = event.buttonState,
            pointerCount = event.pointerCount,
            deviceId = event.deviceId,
            source = event.source,
            historicalCount = event.historySize,
        )
    }

    private fun toolName(toolType: Int): String = when (toolType) {
        MotionEvent.TOOL_TYPE_STYLUS -> "Stylus"
        MotionEvent.TOOL_TYPE_ERASER -> "Eraser"
        MotionEvent.TOOL_TYPE_FINGER -> "Finger"
        MotionEvent.TOOL_TYPE_MOUSE -> "Mouse"
        else -> "Unknown ($toolType)"
    }
}
