package com.mheenotes.app.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.widget.Toast
import java.util.Locale

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.mheenotes.app.audio.AudioFileTranscriber
import com.mheenotes.app.audio.AudioSpeechTranscriber
import com.mheenotes.app.audio.AudioWaveformBar
import com.mheenotes.app.audio.AudioWaveformExtractor

import com.mheenotes.app.audio.TranscriptData
import com.mheenotes.app.audio.TranscriptWord


import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShapeLine
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import com.mheenotes.app.document.ImportedPdf
import com.mheenotes.app.MainActivity
import com.mheenotes.app.R
import com.mheenotes.app.document.PageExporter
import com.mheenotes.app.document.PdfServices
import com.mheenotes.app.ui.AppSettings
import com.mheenotes.app.ui.AppThemePreset
import com.mheenotes.app.ui.ActionActivationMode
import com.mheenotes.app.ui.defaultThemePresets
import com.mheenotes.app.ui.shimmer
import com.mheenotes.app.ui.shimmerPlaceholder
import com.mheenotes.app.widget.InkActivityTracker
import com.mheenotes.app.widget.RecentNotesWidgetProvider
import com.mheenotes.app.editor.CommittedInkStroke
import com.mheenotes.app.editor.EditorSession
import com.mheenotes.app.editor.EraserMode
import com.mheenotes.app.editor.FinishedInkStroke
import com.mheenotes.app.editor.FingerObjectGesture
import com.mheenotes.app.editor.HoverDoubleTapRecognizer
import com.mheenotes.app.editor.HoverCalibrationView
import com.mheenotes.app.editor.HoverShortcutCalibrator
import com.mheenotes.app.editor.HoverShortcutConfig
import com.mheenotes.app.editor.HoverCalibrationResult
import com.mheenotes.app.editor.InkAuthoringOverlayView
import com.mheenotes.app.editor.SaveStatus
import com.mheenotes.app.editor.ViewportTransform
import com.mheenotes.app.editor.StylusHoverSample
import com.mheenotes.app.editor.inInkRenderOrder
import com.mheenotes.app.editor.toAndroidXStroke
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.AssetId
import com.mheenotes.core.model.ActionMapping
import com.mheenotes.core.model.GestureDrawing
import com.mheenotes.core.model.GesturePoint
import com.mheenotes.core.model.MappedAction
import com.mheenotes.core.model.NotebookDocument
import com.mheenotes.core.model.NotebookId
import com.mheenotes.core.model.NotebookPage
import com.mheenotes.core.model.ObjectId
import com.mheenotes.core.model.ObjectCrop
import com.mheenotes.core.model.ObjectTransform
import com.mheenotes.core.model.PageObject
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.PageSize
import com.mheenotes.core.model.PageState
import com.mheenotes.core.model.PageTemplate
import com.mheenotes.core.model.PdfPageSource
import com.mheenotes.core.model.SourceDocument
import com.mheenotes.core.model.SourceDocumentId
import com.mheenotes.core.model.ShapeKind
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import com.mheenotes.core.model.TextAlignment
import com.mheenotes.core.model.ImageObject as ModelImageObject
import com.mheenotes.core.model.ShapeObject as ModelShapeObject
import com.mheenotes.core.model.TextObject as ModelTextObject
import com.mheenotes.core.model.AudioObject as ModelAudioObject
import com.mheenotes.core.storage.NativeNotebookContainer
import com.mheenotes.core.storage.ActionMapStore
import com.mheenotes.app.editor.AutomaticOcrResult
import com.mheenotes.app.editor.AutomaticTextRecognizer
import com.mheenotes.app.editor.RecognizedPageText
import com.mheenotes.app.editor.RecognizedTextToken
import com.mheenotes.app.editor.TextRangeSelection
import com.mheenotes.app.editor.TextSelectionHandle
import com.mheenotes.app.editor.nearestRecognizedTokenIndex
import com.mheenotes.gesture.engine.ActionMapRecognizer
import com.mheenotes.ink.engine.ScribbleSensitivity
import com.mheenotes.ink.engine.LassoSelection
import com.mheenotes.ink.engine.SelectionTransform
import com.mheenotes.ink.engine.DrawHoldShapeDetector
import com.mheenotes.ink.engine.PressureNormalizer
import com.mheenotes.ink.engine.CorrectedShape
import androidx.ink.strokes.Stroke as AndroidXStroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Properties
import java.util.UUID
import kotlin.random.Random
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.hypot

private const val A4_WIDTH = 210_000f
private const val A4_HEIGHT = 297_000f

private enum class AppScreen { Library, Editor }
private val BackOutEasing = Easing { progress ->
    val shifted = progress - 1f
    1f + 2.70158f * shifted * shifted * shifted + 1.70158f * shifted * shifted
}

@Composable
private fun Modifier.staggeredEntrance(key: Any, index: Int): Modifier {
    if (index > 12) return this
    val reveal = remember(key) { Animatable(0f) }
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(key) {
        delay(index * 28L)
        reveal.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        alpha = reveal.value
        translationY = (1f - reveal.value) * rise
        val scale = .96f + reveal.value * .04f
        scaleX = scale
        scaleY = scale
    }
}

private enum class LibrarySection { Main, Recent, Favorites, Trash }
private enum class LibraryLayout { Grid, List }
private enum class LibrarySort { Name, Recent, PageCount }
private enum class PdfImportDestination { NewNotebook, CurrentNotebook }
private enum class ExportFormat(val extension: String, val mediaType: String) {
    Pdf("pdf", "application/pdf"),
    Image("png", "image/png"),
    Notebook("mynote", "application/x-mheenote"),
}
private data class ExportDialogRequest(
    val format: ExportFormat,
    val tabId: Int,
    val pageIndex: Int = 0,
)
private data class PendingSaveExport(
    val request: ExportDialogRequest,
    val pageIndices: List<Int> = emptyList(),
)
private enum class DockEdge { Top, Left, Right }
private enum class EditorMode { Ink, Lasso, Object, Image, Text, Shape, Audio }
private data class RadialToolItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: MappedAction,
)
private enum class AppSettingsSection { General, Appearance, Drawing, Fonts, Input }

private fun normalizedFolderPaths(rawPaths: Iterable<String>): List<String> {
    val result = linkedSetOf<String>()
    rawPaths.forEach { rawPath ->
        var current = ""
        rawPath.trim().trim('/').split('/').filter(String::isNotBlank).forEach { segment ->
            current = if (current.isEmpty()) segment else "$current/$segment"
            result += current
        }
    }
    return result.sortedBy(String::lowercase)
}

private fun immediateFolderChildren(paths: List<String>, parent: String?): List<String> {
    val prefix = parent?.let { "$it/" }.orEmpty()
    return paths.filter { path ->
        path.startsWith(prefix) && path.removePrefix(prefix).let { it.isNotEmpty() && '/' !in it }
    }
}

private fun isFolderOrDescendant(path: String?, root: String): Boolean =
    path != null && (path == root || path.startsWith("$root/"))

private fun replaceFolderRoot(path: String, oldRoot: String, newRoot: String): String =
    if (path == oldRoot) newRoot else newRoot + path.removePrefix(oldRoot)

private fun folderTrashMembersKey(folder: String) = "trashed-folder-members::$folder"

private fun normalizedExportFileName(requested: String, format: ExportFormat): String {
    val leaf = requested.trim()
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[\\p{Cntrl}:*?\"<>|]"), "_")
        .ifBlank { "Export" }
    return if (leaf.endsWith(".${format.extension}", ignoreCase = true)) leaf else "$leaf.${format.extension}"
}
private enum class PaperPreset(val label: String, val widthMm: Float, val heightMm: Float) {
    A3("A3", 297f, 420f),
    A4("A4", 210f, 297f),
    A5("A5", 148f, 210f),
    Letter("Letter", 215.9f, 279.4f),
    Tablet("Screen", 160f, 256f),
    Widescreen("16:9", 160f, 90f),
    Custom("Custom", 210f, 297f),
}

private data class NotebookCreationSpec(
    val title: String,
    val size: PageSize,
    val template: PageTemplate,
    val backgroundArgb: Int,
    val templateLineArgb: Int,
    val coverArgb: Int,
)

private data class PageInsertSpec(
    val size: PageSize,
    val template: PageTemplate,
    val backgroundArgb: Int,
    val templateLineArgb: Int,
)

private data class PdfNotebookHit(val notebookPageIndex: Int, val matchCount: Int)
private data class PdfNotebookLink(val sourceNotebookPageIndex: Int, val targetNotebookPageIndex: Int)

private sealed interface SceneObject {
    val id: ObjectId
    val zIndex: Long

    data class Image(
        val bitmap: Bitmap,
        val asset: File,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rotation: Float = 0f,
        val opacity: Float = 1f,
        val crop: ObjectCrop = ObjectCrop(),
        val locked: Boolean = false,
        override val id: ObjectId = ObjectId(UUID.randomUUID().toString()),
        val assetId: AssetId = AssetId(asset.nameWithoutExtension),
        override val zIndex: Long = 0L,
    ) : SceneObject

    data class TextBox(
        val text: String,
        val x: Float,
        val y: Float,
        val size: Float,
        val argb: Int = 0xFF20242B.toInt(),
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val alignment: TextAlignment = TextAlignment.Start,
        val fontFamily: String = "sans-serif",
        val outlineArgb: Int? = null,
        val outlineWidth: Float = 0f,
        val curveDegrees: Float = 0f,
        val width: Float = 70_000f,
        val height: Float = size * 2f,
        val rotation: Float = 0f,
        override val id: ObjectId = ObjectId(UUID.randomUUID().toString()),
        override val zIndex: Long = 0L,
    ) : SceneObject

    data class Rectangle(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val kind: ShapeKind = ShapeKind.Rectangle,
        val strokeArgb: Int = 0xFF2563EB.toInt(),
        val strokeWidth: Float = 1_000f,
        val fillArgb: Int? = null,
        val rotation: Float = 0f,
        override val id: ObjectId = ObjectId(UUID.randomUUID().toString()),
        override val zIndex: Long = 0L,
    ) : SceneObject

    data class Audio(
        val asset: File,
        val x: Float,
        val y: Float,
        val width: Float = 12_000f,
        val height: Float = 12_000f,
        val durationMillis: Long = 0L,
        val transcript: String = "",
        override val id: ObjectId = ObjectId(UUID.randomUUID().toString()),
        val assetId: AssetId = AssetId(asset.nameWithoutExtension),
        override val zIndex: Long = 0L,
    ) : SceneObject
}

private fun SceneObject.toPageObject(): PageObject = when (this) {
    is SceneObject.Image -> ModelImageObject(
        id = id,
        assetId = assetId,
        transform = ObjectTransform(x, y, width, height, rotation),
        zIndex = zIndex,
        opacity = opacity,
        crop = crop,
        locked = locked,
    )
    is SceneObject.TextBox -> ModelTextObject(
        id = id,
        text = text,
        argb = argb,
        textSizeMicrons = size,
        bold = bold,
        italic = italic,
        underline = underline,
        alignment = alignment,
        fontFamily = fontFamily,
        outlineArgb = outlineArgb,
        outlineWidthMicrons = outlineWidth,
        curveDegrees = curveDegrees,
        transform = ObjectTransform(x, y, width, height, rotation),
        zIndex = zIndex,
    )
    is SceneObject.Rectangle -> ModelShapeObject(
        id = id,
        kind = kind,
        strokeArgb = strokeArgb,
        strokeWidthMicrons = strokeWidth,
        fillArgb = fillArgb,
        transform = ObjectTransform(x, y, width, height, rotation),
        zIndex = zIndex,
    )
    is SceneObject.Audio -> ModelAudioObject(
        id = id,
        assetId = assetId,
        durationMillis = durationMillis,
        transcript = transcript,
        transform = ObjectTransform(x, y, width, height),
        zIndex = zIndex,
    )
}

private fun SceneObject.toExportObject(): PageExporter.ExportObject? = when (this) {
    is SceneObject.Image -> PageExporter.ExportImage(
        bitmap, x, y, width, height, rotation, opacity, crop, zIndex,
    )
    is SceneObject.TextBox -> PageExporter.ExportText(
        text, x, y, size, argb, rotation, bold, italic, underline, alignment, fontFamily,
        outlineArgb, outlineWidth, curveDegrees, zIndex,
    )
    is SceneObject.Rectangle -> PageExporter.ExportShape(
        kind, x, y, width, height, rotation, strokeArgb, strokeWidth, fillArgb, zIndex,
    )
    is SceneObject.Audio -> null
}

private fun PageEntry.toExportPage() = PageExporter.ExportPage(
    page = session.page,
    background = pdfBackground,
    widthMicrons = size.widthMicrons,
    heightMicrons = size.heightMicrons,
    template = template,
    backgroundArgb = backgroundArgb,
    templateLineArgb = templateLineArgb,
    objects = objects.mapNotNull(SceneObject::toExportObject),
)

private data class RuntimeInkStroke(
    val stroke: AndroidXStroke,
    val inputOriginX: Float,
    val inputOriginY: Float,
)

private data class StrokeSizePreview(
    val tool: InkTool,
    val widthMicrons: Float,
)

private data class ActiveAudioRecording(
    val pageIndex: Int,
    val position: PagePoint,
    val assetId: AssetId,
    val file: File,
    val recorder: AudioRecord,
    val worker: Thread,
    val recording: AtomicBoolean,
    val startedAtMillis: Long,
)




private data class PageEntry(
    val id: PageId,
    val session: EditorSession,
    val bookmarked: Boolean = false,
    val rotation: Int = 0,
    val template: PageTemplate = PageTemplate.Blank,
    val size: PageSize = PageSize(A4_WIDTH, A4_HEIGHT),
    val backgroundArgb: Int = 0xFFFFFEFA.toInt(),
    val templateLineArgb: Int = 0x402563EB,
    val pdfSource: Pair<File, Int>? = null,
    val pdfBackground: Bitmap? = null,
    val thumbnail: Bitmap? = null,
    val objects: androidx.compose.runtime.snapshots.SnapshotStateList<SceneObject> = mutableStateListOf(),
    // This map is read directly by the permanent-ink Canvas. It must participate in
    // Compose snapshots so the wet stroke is not removed before the dry layer redraws.
    val renderStrokes: MutableMap<StrokeId, RuntimeInkStroke> = mutableStateMapOf(),
    // Native AndroidX Ink reconstruction is intentionally optional. A failed stroke remains in
    // the domain model and is drawn by the direct Canvas fallback instead of disappearing.
    val renderStrokeFailures: MutableMap<StrokeId, String> = mutableStateMapOf(),
) {
    private var runtimeSourcePage: PageState? = null
    var renderRevision by mutableIntStateOf(0)
        private set

    fun runtimeStrokesMatch(source: PageState): Boolean = runtimeSourcePage === source

    fun markRuntimeStrokesChanged() {
        renderRevision++
    }

    fun markRuntimeStrokesSynchronized() {
        runtimeSourcePage = session.page
    }

    fun invalidateRuntimeStrokeSource() {
        runtimeSourcePage = null
    }
}

internal fun pagePreviewAspectRatio(size: PageSize, rotationDegrees: Int): Float =
    if (((rotationDegrees % 360) + 360) % 360 in setOf(90, 270)) {
        size.heightMicrons / size.widthMicrons
    } else {
        size.widthMicrons / size.heightMicrons
    }

private fun PageEntry.previewAspectRatio(): Float = pagePreviewAspectRatio(size, rotation)

internal fun <T> MutableList<T>.replaceContentsWithoutEmptying(values: List<T>) {
    require(values.isNotEmpty()) { "A notebook must keep at least one page" }
    val sharedCount = minOf(size, values.size)
    repeat(sharedCount) { index -> this[index] = values[index] }
    while (size > values.size) removeAt(lastIndex)
    if (size < values.size) addAll(values.subList(size, values.size))
}

private data class PageListSnapshot(
    val entries: List<PageEntry>,
    val selectedPageId: PageId,
)

private data class SavedNotebookViewport(
    val centerX: Float,
    val centerY: Float,
    val zoomRatio: Float,
)

private data class ObjectListSnapshot(
    val pageId: PageId,
    val objects: List<SceneObject>,
)

private sealed interface EditorHistoryEntry {
    data class Ink(val pageId: PageId) : EditorHistoryEntry
    data object Objects : EditorHistoryEntry
    data object Pages : EditorHistoryEntry
    data class Composite(val entries: List<EditorHistoryEntry>) : EditorHistoryEntry
}

internal enum class SelectionUndoAction { None, CancelSelection, UndoEditAndCancelSelection }

internal fun selectionUndoAction(
    baselineRevision: Long?,
    currentRevision: Long,
): SelectionUndoAction = when {
    baselineRevision == null -> SelectionUndoAction.None
    currentRevision > baselineRevision -> SelectionUndoAction.UndoEditAndCancelSelection
    else -> SelectionUndoAction.CancelSelection
}

private data class PendingImagePlacement(
    val tabId: Int,
    val pageId: PageId,
    val position: PagePoint,
)

private class EditorTab(
    val id: Int,
    initialTitle: String,
    private val root: File,
    initialDocument: NotebookDocument? = null,
    private val pdfServices: PdfServices? = null,
    private val mainHandler: Handler? = null,
) {
    private val pendingPdfRenders = mutableSetOf<PageId>()
    private val pendingThumbnailRenders = mutableSetOf<PageId>()
    private val thumbnailCacheOrder = ArrayDeque<PageId>()
    private var penMaximumWidthMicrons = 5_000f
    private var pencilMaximumWidthMicrons = 5_000f
    private var highlighterMaximumWidthMicrons = 5_000f
    private var eraserMaximumWidthMicrons = 5_000f
    private val ownedPageEntries = mutableListOf<PageEntry>()
    private val pageUndo = ArrayDeque<PageListSnapshot>()
    private val pageRedo = ArrayDeque<PageListSnapshot>()
    private val objectUndo = ArrayDeque<ObjectListSnapshot>()
    private val objectRedo = ArrayDeque<ObjectListSnapshot>()
    private val editorUndo = ArrayDeque<EditorHistoryEntry>()
    private val editorRedo = ArrayDeque<EditorHistoryEntry>()
    private var historyRevision = 0L
    private var historyStateVersion by mutableIntStateOf(0)
    private var historyTransactionDepth = 0
    private val transactionEntries = mutableListOf<EditorHistoryEntry>()
    private var historyRecordingSuspended = false
    private var savedViewport = loadSavedViewport()
    private val viewportSaveRunnable = Runnable { persistViewportNow() }
    private var toolDefaults: AppSettings? = null
    var flushPendingInk: () -> Unit = {}
    var deferUndoForWetInk: () -> Boolean = { false }
    var clearSelectionForHistory: () -> Unit = {}
    private var selectionUndoBaselineRevision by mutableStateOf<Long?>(null)
    private var wetInkUndoAvailable by mutableStateOf(false)
    private val startingDocument = initialDocument ?: runCatching {
        NativeNotebookContainer.loadManifest(root)
    }.getOrNull()
    private val notebookId = startingDocument?.id ?: NotebookId(UUID.randomUUID().toString())
    val persistentId: String get() = notebookId.value
    private val createdAtEpochMillis = startingDocument?.createdAtEpochMillis ?: System.currentTimeMillis()
    var title by mutableStateOf(startingDocument?.title ?: initialTitle)
    var folderPath by mutableStateOf(startingDocument?.folderPath)
        private set
    var favorite by mutableStateOf(startingDocument?.favorite ?: false)
        private set
    var trashedAtEpochMillis by mutableStateOf(startingDocument?.trashedAtEpochMillis)
        private set
    var lastOpenedAtEpochMillis by mutableStateOf(startingDocument?.lastOpenedAtEpochMillis ?: System.currentTimeMillis())
        private set
    var coverArgb by mutableIntStateOf(startingDocument?.coverArgb ?: 0xFF303747.toInt())
        private set
    val pages = mutableStateListOf<PageEntry>().apply {
        startingDocument?.pages?.forEach { add(existingPage(it)) }
        if (isEmpty()) add(newPage())
    }
    private var selectedPageIndexState by mutableIntStateOf(0)
    var pageNavigationGeneration by mutableIntStateOf(0)
        private set
    var viewportResetGeneration by mutableIntStateOf(0)
        private set
    var selectedPageIndex: Int
        get() = selectedPageIndexState
        set(value) {
            selectedPageIndexState = value.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
            requestPdfBackgroundsNear(selectedPageIndexState)
        }
    val session get() = pages[selectedPageIndex].session

    fun navigateToPage(index: Int) {
        selectedPageIndex = index
        pageNavigationGeneration++
    }
    fun resetViewport() { viewportResetGeneration++ }

    fun restoredViewport(): SavedNotebookViewport? = savedViewport

    fun rememberViewport(transform: ViewportTransform, minimumScale: Float, viewWidth: Int, viewHeight: Int) {
        if (minimumScale <= 0f || viewWidth <= 1 || viewHeight <= 1) return
        val center = transform.toPage(viewWidth / 2f, viewHeight / 2f)
        val candidate = SavedNotebookViewport(
            centerX = center.x,
            centerY = center.y,
            zoomRatio = (transform.scale / minimumScale).coerceIn(1f, 8f),
        )
        if (!candidate.centerX.isFinite() || !candidate.centerY.isFinite() || !candidate.zoomRatio.isFinite()) return
        savedViewport = candidate
        mainHandler?.removeCallbacks(viewportSaveRunnable)
        mainHandler?.postDelayed(viewportSaveRunnable, 500L)
    }

    private fun loadSavedViewport(): SavedNotebookViewport? = runCatching {
        val file = root.resolve("viewport.properties")
        if (!file.isFile) return@runCatching null
        val values = Properties().apply { file.inputStream().buffered().use(::load) }
        SavedNotebookViewport(
            centerX = values.getProperty("centerX").toFloat(),
            centerY = values.getProperty("centerY").toFloat(),
            zoomRatio = values.getProperty("zoomRatio").toFloat().coerceIn(1f, 8f),
        ).takeIf { it.centerX.isFinite() && it.centerY.isFinite() && it.zoomRatio.isFinite() }
    }.getOrNull()

    private fun persistViewportNow() {
        val value = savedViewport ?: return
        runCatching {
            root.mkdirs()
            val target = root.resolve("viewport.properties")
            val temporary = root.resolve("viewport.properties.tmp")
            Properties().apply {
                setProperty("centerX", value.centerX.toString())
                setProperty("centerY", value.centerY.toString())
                setProperty("zoomRatio", value.zoomRatio.toString())
            }.also { values -> temporary.outputStream().buffered().use { values.store(it, null) } }
            if (target.exists()) target.delete()
            check(temporary.renameTo(target)) { "Unable to save notebook viewport" }
        }
    }

    fun markOpened() {
        lastOpenedAtEpochMillis = System.currentTimeMillis()
        persistManifest()
    }

    fun rename(value: String) {
        val cleaned = value.trim()
        if (cleaned.isNotEmpty()) { title = cleaned; persistManifest() }
    }

    fun editNotebook(spec: NotebookCreationSpec) {
        val cleaned = spec.title.trim()
        if (cleaned.isEmpty()) return
        title = cleaned
        coverArgb = spec.coverArgb
        pages.indices.forEach { index ->
            val page = pages[index]
            if (page.pdfSource == null) {
                pages[index] = page.copy(
                    size = spec.size,
                    template = spec.template,
                    backgroundArgb = spec.backgroundArgb,
                    templateLineArgb = spec.templateLineArgb,
                )
            }
        }
        persistManifest()
    }

    fun updateFavorite(value: Boolean) { favorite = value; persistManifest() }
    fun moveToFolder(value: String?) { folderPath = value?.trim()?.trim('/')?.takeIf(String::isNotBlank); persistManifest() }
    fun moveToTrash() { trashedAtEpochMillis = System.currentTimeMillis(); persistManifest() }
    fun restoreFromTrash() { trashedAtEpochMillis = null; persistManifest() }
    fun setCoverColor(argb: Int) { coverArgb = argb; persistManifest() }

    fun configureInitialPage(
        size: PageSize,
        template: PageTemplate,
        backgroundArgb: Int,
        templateLineArgb: Int,
    ) {
        val page = pages.first()
        pages[0] = page.copy(
            size = size,
            template = template,
            backgroundArgb = backgroundArgb,
            templateLineArgb = templateLineArgb,
        )
        persistManifest()
    }

    fun updateMaximumToolWidths(settings: AppSettings) {
        penMaximumWidthMicrons = settings.penMaximumWidthMicrons
        pencilMaximumWidthMicrons = settings.pencilMaximumWidthMicrons
        highlighterMaximumWidthMicrons = settings.highlighterMaximumWidthMicrons
        eraserMaximumWidthMicrons = settings.eraserMaximumWidthMicrons
        pages.forEach { sessionPage ->
            sessionPage.session.updateMaximumToolWidths(
                penMaximumWidthMicrons,
                pencilMaximumWidthMicrons,
                highlighterMaximumWidthMicrons,
                eraserMaximumWidthMicrons,
            )
        }
    }

    fun updateToolDefaults(settings: AppSettings) {
        toolDefaults = settings
        pages.forEach { configureToolDefaults(it.session, settings) }
    }

    private fun configureToolDefaults(session: EditorSession, settings: AppSettings) {
        session.initializeToolDefaults(
            penWidthMicrons = settings.penWidthPresets.getOrElse(1) { 1_400f },
            pencilWidthMicrons = settings.pencilWidthPresets.getOrElse(1) { 1_200f },
            highlighterWidthMicrons = settings.highlighterWidthPresets.getOrElse(1) { 4_000f },
            penArgb = settings.penColorPresets.firstOrNull() ?: 0xFF111827.toInt(),
            pencilArgb = settings.pencilColorPresets.firstOrNull() ?: 0xFF30343B.toInt(),
            highlighterArgb = settings.highlighterColorPresets.firstOrNull() ?: 0xFFF7D34A.toInt(),
        )
    }

    init {
        persistManifest()
        restorePdfBackgrounds()
    }

    val canUndo: Boolean
        get() {
            historyStateVersion
            return wetInkUndoAvailable || selectionUndoBaselineRevision != null || editorUndo.isNotEmpty()
        }
    val canRedo: Boolean
        get() {
            historyStateVersion
            return editorRedo.isNotEmpty()
        }
    val canUndoPageEdit get() = canUndo
    val canRedoPageEdit get() = canRedo
    val canUndoObjectEdit get() = canUndo
    val canRedoObjectEdit get() = canRedo

    fun updateWetInkUndoAvailable(available: Boolean) {
        wetInkUndoAvailable = available
    }

    fun beginSelectionUndo() {
        if (selectionUndoBaselineRevision == null) {
            selectionUndoBaselineRevision = historyRevision
        }
    }

    fun endSelectionUndo() {
        selectionUndoBaselineRevision = null
    }

    private fun recordHistory(entry: EditorHistoryEntry) {
        if (historyRecordingSuspended) return
        if (historyTransactionDepth > 0) {
            transactionEntries += entry
            return
        }
        editorUndo.addLast(entry)
        while (editorUndo.size > 100) editorUndo.removeFirst()
        editorRedo.clear()
        historyRevision++
        historyStateVersion++
    }

    fun beginHistoryTransaction() {
        if (historyTransactionDepth == 0) transactionEntries.clear()
        historyTransactionDepth++
    }

    fun endHistoryTransaction() {
        if (historyTransactionDepth <= 0) return
        historyTransactionDepth--
        if (historyTransactionDepth == 0 && transactionEntries.isNotEmpty()) {
            val entries = transactionEntries.toList()
            transactionEntries.clear()
            recordHistory(if (entries.size == 1) entries.single() else EditorHistoryEntry.Composite(entries))
        }
    }

    fun <T> historyTransaction(operation: () -> T): T {
        beginHistoryTransaction()
        return try { operation() } finally { endHistoryTransaction() }
    }

    fun invalidateThumbnail(pageId: PageId? = null) {
        val targetId = pageId ?: pages.firstOrNull()?.id ?: return
        val index = pages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            val old = pages[index].thumbnail
            pages[index] = pages[index].copy(thumbnail = null)
            old?.recycle()
            thumbnailCacheOrder.remove(targetId)
        }
    }

    private fun recordInkEdit(pageId: PageId) {
        invalidateThumbnail(pageId)
        recordHistory(EditorHistoryEntry.Ink(pageId))
    }

    fun recordObjectEdit(page: PageEntry) {
        invalidateThumbnail(page.id)
        objectUndo.addLast(ObjectListSnapshot(page.id, page.objects.toList()))
        while (objectUndo.size > 100) objectUndo.removeFirst()
        objectRedo.clear()
        recordHistory(EditorHistoryEntry.Objects)
    }

    private fun restoreObjects(snapshot: ObjectListSnapshot): ObjectListSnapshot? {
        val page = pages.firstOrNull { it.id == snapshot.pageId } ?: return null
        invalidateThumbnail(page.id)
        val current = ObjectListSnapshot(page.id, page.objects.toList())
        page.objects.clear()
        page.objects.addAll(snapshot.objects)
        persistObjects()
        return current
    }

    private fun applyObjectUndo() {
        if (objectUndo.isEmpty()) return
        restoreObjects(objectUndo.removeLast())?.let(objectRedo::addLast)
    }

    private fun applyObjectRedo() {
        if (objectRedo.isEmpty()) return
        restoreObjects(objectRedo.removeLast())?.let(objectUndo::addLast)
    }

    private fun pageSnapshot() = PageListSnapshot(pages.toList(), pages[selectedPageIndex].id)

    private fun recordPageEdit() {
        invalidateThumbnail()
        pageUndo.addLast(pageSnapshot())
        while (pageUndo.size > 100) pageUndo.removeFirst()
        pageRedo.clear()
        recordHistory(EditorHistoryEntry.Pages)
    }

    private fun restorePageSnapshot(snapshot: PageListSnapshot) {
        val selectedIndex = snapshot.entries.indexOfFirst { it.id == snapshot.selectedPageId }.coerceAtLeast(0)
        if (selectedIndex < pages.size) selectedPageIndexState = selectedIndex
        pages.replaceContentsWithoutEmptying(snapshot.entries)
        navigateToPage(pages.indexOfFirst { it.id == snapshot.selectedPageId }.coerceAtLeast(0))
        invalidateThumbnail()
        persistManifest()
    }

    private fun applyPageUndo() {
        if (pageUndo.isEmpty()) return
        pageRedo.addLast(pageSnapshot())
        restorePageSnapshot(pageUndo.removeLast())
    }

    private fun applyPageRedo() {
        if (pageRedo.isEmpty()) return
        pageUndo.addLast(pageSnapshot())
        restorePageSnapshot(pageRedo.removeLast())
    }

    private fun applyUndo(entry: EditorHistoryEntry) {
        when (entry) {
            is EditorHistoryEntry.Ink -> {
                invalidateThumbnail(entry.pageId)
                pages.firstOrNull { it.id == entry.pageId }?.session?.undo()
            }
            EditorHistoryEntry.Objects -> applyObjectUndo()
            EditorHistoryEntry.Pages -> {
                invalidateThumbnail()
                applyPageUndo()
            }
            is EditorHistoryEntry.Composite -> entry.entries.asReversed().forEach(::applyUndo)
        }
    }

    private fun applyRedo(entry: EditorHistoryEntry) {
        when (entry) {
            is EditorHistoryEntry.Ink -> {
                invalidateThumbnail(entry.pageId)
                pages.firstOrNull { it.id == entry.pageId }?.session?.redo()
            }
            EditorHistoryEntry.Objects -> applyObjectRedo()
            EditorHistoryEntry.Pages -> {
                invalidateThumbnail()
                applyPageRedo()
            }
            is EditorHistoryEntry.Composite -> entry.entries.forEach(::applyRedo)
        }
    }


    fun undo() {
        flushPendingInk()
        if (deferUndoForWetInk()) return
        when (selectionUndoAction(selectionUndoBaselineRevision, historyRevision)) {
            SelectionUndoAction.None -> Unit
            SelectionUndoAction.CancelSelection -> {
                clearSelectionForHistory()
                selectionUndoBaselineRevision = null
                return
            }
            SelectionUndoAction.UndoEditAndCancelSelection -> {
                clearSelectionForHistory()
                selectionUndoBaselineRevision = null
            }
        }
        if (editorUndo.isEmpty()) return
        val entry = editorUndo.removeLast()
        historyRecordingSuspended = true
        try { applyUndo(entry) } finally { historyRecordingSuspended = false }
        editorRedo.addLast(entry)
        historyStateVersion++
    }

    fun redo() {
        if (selectionUndoBaselineRevision != null) {
            clearSelectionForHistory()
            selectionUndoBaselineRevision = null
        }
        if (editorRedo.isEmpty()) return
        val entry = editorRedo.removeLast()
        historyRecordingSuspended = true
        try { applyRedo(entry) } finally { historyRecordingSuspended = false }
        editorUndo.addLast(entry)
        historyStateVersion++
    }

    fun addPage(after: Boolean = true, spec: PageInsertSpec? = null) {
        flushPendingInk()
        recordPageEdit()
        val insertionIndex = if (after) selectedPageIndex + 1 else selectedPageIndex
        val source = pages[selectedPageIndex]
        pages.add(
            insertionIndex,
            newPage().copy(
                size = spec?.size ?: source.size,
                template = spec?.template ?: source.template,
                backgroundArgb = spec?.backgroundArgb ?: source.backgroundArgb,
                templateLineArgb = spec?.templateLineArgb ?: source.templateLineArgb,
            ),
        )
        navigateToPage(insertionIndex)
        persistManifest()
    }
    fun duplicatePage() {
        flushPendingInk()
        historyTransaction {
            recordPageEdit()
            val source = pages[selectedPageIndex]
            val duplicate = newPage().copy(
                bookmarked = source.bookmarked, rotation = source.rotation,
                template = source.template, size = source.size,
                backgroundArgb = source.backgroundArgb, templateLineArgb = source.templateLineArgb,
                pdfSource = source.pdfSource, pdfBackground = source.pdfBackground,
                objects = mutableStateListOf<SceneObject>().apply {
                    addAll(source.objects.map(SceneObject::duplicate))
                },
            )
            source.session.page.strokes.forEach { stroke ->
                duplicate.session.acceptCompletedStroke(stroke.samples, stroke.style, detectScribbleErase = false)
            }
            pages.add(selectedPageIndex + 1, duplicate)
            navigateToPage(selectedPageIndex + 1)
            persistManifest()
        }
    }
    fun deletePage() {
        flushPendingInk()
        if (pages.size <= 1) return
        recordPageEdit()
        pages.removeAt(selectedPageIndex)
        navigateToPage(selectedPageIndex.coerceAtMost(pages.lastIndex))
        persistManifest()
    }

    fun movePage(pageId: PageId, direction: Int) {
        val from = pages.indexOfFirst { it.id == pageId }
        if (from < 0) return
        movePageTo(pageId, from + direction)
    }

    fun movePageTo(pageId: PageId, targetIndex: Int) {
        val from = pages.indexOfFirst { it.id == pageId }
        val to = targetIndex.coerceIn(0, pages.lastIndex)
        if (from < 0 || from == to) return
        recordPageEdit()
        val selectedId = pages[selectedPageIndex].id
        val moved = pages.removeAt(from)
        pages.add(to, moved)
        selectedPageIndex = pages.indexOfFirst { it.id == selectedId }
        persistManifest()
    }
    fun toggleBookmark() { recordPageEdit(); pages[selectedPageIndex] = pages[selectedPageIndex].let { it.copy(bookmarked = !it.bookmarked) }; persistManifest() }
    fun rotatePage() { recordPageEdit(); pages[selectedPageIndex] = pages[selectedPageIndex].let { it.copy(rotation = (it.rotation + 90) % 360) }; persistManifest() }
    fun cycleTemplate() {
        recordPageEdit()
        val page = pages[selectedPageIndex]
        pages[selectedPageIndex] = page.copy(template = PageTemplate.entries[(page.template.ordinal + 1) % PageTemplate.entries.size])
        persistManifest()
    }
    fun cyclePageBackground() {
        val choices = listOf(0xFFFFFEFA.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFF7E6.toInt(), 0xFFF4F1E8.toInt(), 0xFF1D222B.toInt())
        recordPageEdit()
        val page = pages[selectedPageIndex]
        pages[selectedPageIndex] = page.copy(backgroundArgb = choices[(choices.indexOf(page.backgroundArgb) + 1).mod(choices.size)])
        persistManifest()
    }
    fun cycleTemplateLineColor() {
        val choices = listOf(0x402563EB, 0x40777777, 0x40508A60, 0x40B45353, 0x60D6D9E0)
        recordPageEdit()
        val page = pages[selectedPageIndex]
        pages[selectedPageIndex] = page.copy(templateLineArgb = choices[(choices.indexOf(page.templateLineArgb) + 1).mod(choices.size)])
        persistManifest()
    }
    fun copyCurrentPageTo(target: EditorTab, move: Boolean) {
        if (target === this || (move && pages.size <= 1)) return
        flushPendingInk()
        val source = pages[selectedPageIndex]
        target.recordPageEdit()
        val copiedPdf = source.pdfSource?.let { (file, pageIndex) ->
            val targetFile = target.root.resolve("sources/${UUID.randomUUID()}.pdf")
            targetFile.parentFile?.mkdirs()
            file.copyTo(targetFile, overwrite = true)
            targetFile to pageIndex
        }
        val copiedObjects = source.objects.map { item ->
            when (item) {
                is SceneObject.Image -> {
                    val (assetId, assetFile) = item.asset.inputStream().use(target::storeImageAsset)
                    item.copy(
                        id = ObjectId(UUID.randomUUID().toString()),
                        asset = assetFile,
                        assetId = assetId,
                    )
                }
                else -> item.duplicate()
            }
        }
        val page = target.newPage().copy(
            bookmarked = source.bookmarked,
            rotation = source.rotation,
            template = source.template,
            size = source.size,
            backgroundArgb = source.backgroundArgb,
            templateLineArgb = source.templateLineArgb,
            pdfSource = copiedPdf,
            objects = mutableStateListOf<SceneObject>().apply { addAll(copiedObjects) },
        )
        source.session.page.strokes.forEach { stroke ->
            page.session.acceptCompletedStroke(stroke.samples, stroke.style, detectScribbleErase = false)
        }
        val insertion = (target.selectedPageIndex + 1).coerceAtMost(target.pages.size)
        target.pages.add(insertion, page)
        target.navigateToPage(insertion)
        target.persistManifest()
        if (move) {
            recordPageEdit()
            pages.remove(source)
            navigateToPage(selectedPageIndex.coerceAtMost(pages.lastIndex))
            persistManifest()
        }
    }
    fun importPdf(
        imported: ImportedPdf,
        pdf: PdfServices,
        handler: Handler,
        replaceWithPdf: Boolean = false,
    ) {
        flushPendingInk()
        val notebookSource = root.resolve("sources/${imported.sourceId}.pdf")
        notebookSource.parentFile?.mkdirs()
        imported.localFile.copyTo(notebookSource, overwrite = true)
        val newPages = imported.pageSizes.mapIndexed { index, dimensions ->
            val width = dimensions.first.toFloat() * 1_000f
            val height = dimensions.second.toFloat() * 1_000f
            newPage().copy(size = PageSize(width, height), pdfSource = notebookSource to index)
        }
        val insertionIndex = if (replaceWithPdf) 0 else selectedPageIndex + 1
        val proposedPages = if (replaceWithPdf) {
            newPages
        } else {
            pages.toMutableList().apply { addAll(insertionIndex, newPages) }
        }
        runCatching { persistManifest(proposedPages) }.onFailure {
            newPages.forEach { page -> page.session.close() }
            notebookSource.delete()
            throw it
        }
        if (replaceWithPdf) {
            pages.forEach { page -> page.session.close() }
            pages.clear()
            pages.addAll(newPages)
            selectedPageIndex = 0
        } else {
            recordPageEdit()
            pages.addAll(insertionIndex, newPages)
            selectedPageIndex = insertionIndex
        }
        requestPdfBackgroundsNear(selectedPageIndex, pdf, handler)
    }
    fun backup(output: java.io.OutputStream) {
        persistManifest()
        persistViewportNow()
        NativeNotebookContainer.pack(root, output)
    }

    fun storeImageAsset(input: java.io.InputStream): Pair<AssetId, File> {
        val assetId = AssetId(UUID.randomUUID().toString())
        val target = imageAssetFile(assetId)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.partial")
        java.io.FileOutputStream(temporary, false).use { output ->
            input.copyTo(output)
            output.flush()
            output.fd.sync()
        }
        check(temporary.renameTo(target)) { "Unable to publish image asset" }
        return assetId to target
    }

    fun persistObjects() {
        persistManifest()
    }

    fun requestThumbnail(pageId: PageId) {
        val entry = pages.firstOrNull { it.id == pageId } ?: return
        if (entry.thumbnail != null || pageId in pendingThumbnailRenders) return
        val handler = mainHandler ?: return

        val pdfSource = entry.pdfSource
        if (pdfSource != null) {
            val (file, pageIndex) = pdfSource
            val pdf = pdfServices ?: return
            if (!file.isFile) return
            pendingThumbnailRenders += pageId
            pdf.renderPage(file, pageIndex, 240) { result ->
                handler.post {
                    pendingThumbnailRenders -= pageId
                    result.onSuccess { bitmap ->
                        applyThumbnailBitmap(pageId, bitmap)
                    }
                }
            }
        } else {
            pendingThumbnailRenders += pageId
            Thread({
                val bitmap = renderInkPageThumbnail(entry, targetWidth = 240)
                handler.post {
                    pendingThumbnailRenders -= pageId
                    if (bitmap != null) {
                        applyThumbnailBitmap(pageId, bitmap)
                    }
                }
            }, "Mheenotes-thumb-$pageId").start()
        }
    }

    private fun applyThumbnailBitmap(pageId: PageId, bitmap: Bitmap) {
        val currentIndex = pages.indexOfFirst { it.id == pageId }
        if (currentIndex >= 0) {
            pages[currentIndex] = pages[currentIndex].copy(thumbnail = bitmap)
            thumbnailCacheOrder.remove(pageId)
            thumbnailCacheOrder.addLast(pageId)
            while (thumbnailCacheOrder.size > 64) {
                val evictedId = thumbnailCacheOrder.removeFirst()
                val evictedIndex = pages.indexOfFirst { it.id == evictedId }
                if (evictedIndex >= 0) {
                    pages[evictedIndex].thumbnail?.recycle()
                    pages[evictedIndex] = pages[evictedIndex].copy(thumbnail = null)
                }
            }
        } else {
            bitmap.recycle()
        }
    }


    fun searchPdf(query: String, result: (List<PdfNotebookHit>) -> Unit) {
        val service = pdfServices ?: return result(emptyList())
        val handler = mainHandler ?: return result(emptyList())
        val groups = pages.mapIndexedNotNull { index, page -> page.pdfSource?.let { Triple(index, it.first, it.second) } }
            .groupBy { it.second.canonicalFile }
        if (groups.isEmpty() || query.isBlank()) return result(emptyList())
        val collected = java.util.Collections.synchronizedList(mutableListOf<PdfNotebookHit>())
        val remaining = java.util.concurrent.atomic.AtomicInteger(groups.size)
        groups.forEach { (file, mappings) ->
            service.search(file, query) { searchResult ->
                searchResult.getOrDefault(emptyList()).forEach { hit ->
                    mappings.filter { it.third == hit.pageIndex }.forEach { mapping ->
                        collected += PdfNotebookHit(mapping.first, hit.matchCount)
                    }
                }
                if (remaining.decrementAndGet() == 0) handler.post { result(collected.sortedBy(PdfNotebookHit::notebookPageIndex)) }
            }
        }
    }

    fun pdfLinks(result: (List<PdfNotebookLink>) -> Unit) {
        val service = pdfServices ?: return result(emptyList())
        val handler = mainHandler ?: return result(emptyList())
        val groups = pages.mapIndexedNotNull { index, page -> page.pdfSource?.let { Triple(index, it.first, it.second) } }
            .groupBy { it.second.canonicalFile }
        if (groups.isEmpty()) return result(emptyList())
        val collected = java.util.Collections.synchronizedList(mutableListOf<PdfNotebookLink>())
        val remaining = java.util.concurrent.atomic.AtomicInteger(groups.size)
        groups.forEach { (file, mappings) ->
            service.outline(file) { outlineResult ->
                outlineResult.getOrDefault(emptyList()).forEach { link ->
                    val source = mappings.firstOrNull { it.third == link.sourcePageIndex }?.first
                    val target = mappings.firstOrNull { it.third == link.targetPageIndex }?.first
                    if (source != null && target != null) collected += PdfNotebookLink(source, target)
                }
                if (remaining.decrementAndGet() == 0) handler.post { result(collected.sortedBy(PdfNotebookLink::sourceNotebookPageIndex)) }
            }
        }
    }

    fun pdfTextNear(pageId: PageId, point: PagePoint, result: (Result<String?>) -> Unit) {
        val page = pages.firstOrNull { it.id == pageId }
            ?: return result(Result.failure(IllegalArgumentException("Page is unavailable")))
        val (file, pdfPageIndex) = page.pdfSource
            ?: return result(Result.success(null))
        val service = pdfServices
            ?: return result(Result.failure(IllegalStateException("PDF service is unavailable")))
        val handler = mainHandler
            ?: return result(Result.failure(IllegalStateException("Main handler is unavailable")))
        service.textNear(
            file = file,
            pageIndex = pdfPageIndex,
            xFraction = point.x / page.size.widthMicrons,
            yFraction = point.y / page.size.heightMicrons,
        ) { extraction ->
            handler.post { result(extraction.map { it?.text }) }
        }
    }

    fun renderPdfForText(pageId: PageId, result: (Result<Bitmap>) -> Unit) {
        val page = pages.firstOrNull { it.id == pageId }
            ?: return result(Result.failure(IllegalArgumentException("Page is unavailable")))
        val (file, pdfPageIndex) = page.pdfSource
            ?: return result(Result.failure(IllegalArgumentException("Page is not PDF-backed")))
        val service = pdfServices
            ?: return result(Result.failure(IllegalStateException("PDF service is unavailable")))
        val handler = mainHandler
            ?: return result(Result.failure(IllegalStateException("Main handler is unavailable")))
        service.renderPage(file, pdfPageIndex, 1_800) { rendered ->
            handler.post { result(rendered) }
        }
    }
    private fun persistManifest(pageEntries: List<PageEntry> = pages) {
        val now = System.currentTimeMillis()
        val canonicalRoot = root.canonicalFile
        val pdfSources = pageEntries.mapNotNull(PageEntry::pdfSource)
            .groupBy { it.first.canonicalFile }
            .map { (file, mappings) ->
                SourceDocument(
                    id = SourceDocumentId(file.nameWithoutExtension),
                    assetPath = file.relativeTo(canonicalRoot).invariantSeparatorsPath,
                    mediaType = "application/pdf",
                    sha256 = NativeNotebookContainer.sha256(file),
                    pageCount = (mappings.maxOfOrNull { it.second } ?: 0) + 1,
                )
            }
        NativeNotebookContainer.saveManifest(
            NotebookDocument(
                id = notebookId, title = title,
                createdAtEpochMillis = createdAtEpochMillis, modifiedAtEpochMillis = now,
                pages = pageEntries.map { page ->
                    NotebookPage(
                        id = page.id,
                        size = page.size,
                        rotationDegrees = page.rotation,
                        template = page.template,
                        pdfSource = page.pdfSource?.let { (file, pageIndex) ->
                            PdfPageSource(SourceDocumentId(file.nameWithoutExtension), pageIndex)
                        },
                        objects = page.objects.sortedBy(SceneObject::zIndex).map(SceneObject::toPageObject),
                        bookmarked = page.bookmarked,
                        backgroundArgb = page.backgroundArgb,
                        templateLineArgb = page.templateLineArgb,
                    )
                },
                sourceDocuments = pdfSources,
                folderPath = folderPath,
                favorite = favorite,
                trashedAtEpochMillis = trashedAtEpochMillis,
                lastOpenedAtEpochMillis = lastOpenedAtEpochMillis,
                coverArgb = coverArgb,
            ),
            root,
        )
    }
    fun close() {
        mainHandler?.removeCallbacks(viewportSaveRunnable)
        persistViewportNow()
        flushPendingInk()
        flushPendingInk = {}
        persistManifest()
        ownedPageEntries.distinctBy { it.id }.forEach { it.session.close() }
        ownedPageEntries.mapNotNull(PageEntry::pdfBackground).distinct().forEach { if (!it.isRecycled) it.recycle() }
        ownedPageEntries.mapNotNull(PageEntry::thumbnail).distinct().forEach { if (!it.isRecycled) it.recycle() }
        ownedPageEntries.flatMap { it.objects }.filterIsInstance<SceneObject.Image>()
            .map(SceneObject.Image::bitmap).distinct().forEach { if (!it.isRecycled) it.recycle() }
    }
    fun permanentlyDelete() {
        close()
        check(root.deleteRecursively()) { "Unable to permanently delete notebook" }
    }
    private fun newPage(): PageEntry {
        val id = PageId(UUID.randomUUID().toString())
        return PageEntry(
            id,
            EditorSession(root.resolve("pages/${id.value}.journal"), id) { recordInkEdit(id) }.apply {
                updateMaximumToolWidths(
                    penMaximumWidthMicrons,
                    pencilMaximumWidthMicrons,
                    highlighterMaximumWidthMicrons,
                    eraserMaximumWidthMicrons,
                )
                toolDefaults?.let { configureToolDefaults(this, it) }
            },
        ).also(ownedPageEntries::add)
    }
    private fun existingPage(page: NotebookPage) = PageEntry(
        id = page.id,
        session = EditorSession(root.resolve("pages/${page.id.value}.journal"), page.id) {
            recordInkEdit(page.id)
        }.apply {
            updateMaximumToolWidths(
                penMaximumWidthMicrons,
                pencilMaximumWidthMicrons,
                highlighterMaximumWidthMicrons,
                eraserMaximumWidthMicrons,
            )
            toolDefaults?.let { configureToolDefaults(this, it) }
        },
        bookmarked = page.bookmarked,
        rotation = page.rotationDegrees,
        template = page.template,
        size = page.size,
        backgroundArgb = page.backgroundArgb,
        templateLineArgb = page.templateLineArgb,
        pdfSource = page.pdfSource?.let { mapping ->
            startingDocument?.sourceDocuments
                ?.firstOrNull { it.id == mapping.documentId }
                ?.let { source -> root.resolve(source.assetPath) to mapping.pageIndex }
        },
        objects = mutableStateListOf<SceneObject>().apply {
            addAll(page.objects.sortedBy(PageObject::zIndex).mapNotNull(::restoreObject))
        },
    ).also(ownedPageEntries::add)

    private fun restoreObject(value: PageObject): SceneObject? = when (value) {
        is ModelImageObject -> {
            val asset = imageAssetFile(value.assetId)
            val bitmap = BitmapFactory.decodeFile(asset.path) ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            SceneObject.Image(
                bitmap = bitmap,
                asset = asset,
                x = value.transform.x,
                y = value.transform.y,
                width = value.transform.width,
                height = value.transform.height,
                rotation = value.transform.rotationDegrees,
                opacity = value.opacity,
                crop = value.crop,
                locked = value.locked,
                id = value.id,
                assetId = value.assetId,
                zIndex = value.zIndex,
            )
        }
        is ModelTextObject -> SceneObject.TextBox(
            text = value.text,
            x = value.transform.x,
            y = value.transform.y,
            size = value.textSizeMicrons,
            argb = value.argb,
            bold = value.bold,
            italic = value.italic,
            underline = value.underline,
            alignment = value.alignment,
            fontFamily = value.fontFamily,
            outlineArgb = value.outlineArgb,
            outlineWidth = value.outlineWidthMicrons,
            curveDegrees = value.curveDegrees,
            width = value.transform.width,
            height = value.transform.height,
            rotation = value.transform.rotationDegrees,
            id = value.id,
            zIndex = value.zIndex,
        )
        is ModelShapeObject -> SceneObject.Rectangle(
            x = value.transform.x,
            y = value.transform.y,
            width = value.transform.width,
            height = value.transform.height,
            kind = value.kind,
            strokeArgb = value.strokeArgb,
            strokeWidth = value.strokeWidthMicrons,
            fillArgb = value.fillArgb,
            rotation = value.transform.rotationDegrees,
            id = value.id,
            zIndex = value.zIndex,
        )
        is ModelAudioObject -> SceneObject.Audio(
            asset = audioAssetFile(value.assetId),
            x = value.transform.x,
            y = value.transform.y,
            width = value.transform.width,
            height = value.transform.height,
            durationMillis = value.durationMillis,
            transcript = value.transcript,
            id = value.id,
            assetId = value.assetId,
            zIndex = value.zIndex,
        )
    }

    private fun imageAssetFile(assetId: AssetId): File = root.resolve("assets/${assetId.value}.image")
    fun newAudioAsset(): Pair<AssetId, File> {
        val assetId = AssetId(UUID.randomUUID().toString())
        return assetId to root.resolve("assets/${assetId.value}.wav").also { it.parentFile?.mkdirs() }
    }
    private fun audioAssetFile(assetId: AssetId): File {
        val wav = root.resolve("assets/${assetId.value}.wav")
        return if (wav.isFile) wav else root.resolve("assets/${assetId.value}.m4a")
    }

    private fun restorePdfBackgrounds() {
        requestPdfBackgroundsNear(selectedPageIndex)
    }

    private fun requestPdfBackgroundsNear(
        centerIndex: Int,
        pdf: PdfServices? = pdfServices,
        handler: Handler? = mainHandler,
    ) {
        if (pdf == null || handler == null || pages.isEmpty()) return
        val wanted = ((centerIndex - 1)..(centerIndex + 1))
            .filter { it in pages.indices }
            .toSet()

        pages.indices.filter { it !in wanted && pages[it].pdfBackground != null }.forEach { index ->
            pages[index].pdfBackground?.let { if (!it.isRecycled) it.recycle() }
            pages[index] = pages[index].copy(pdfBackground = null)
        }
        wanted.forEach { index ->
            val entry = pages[index]
            val (file, pageIndex) = entry.pdfSource ?: return@forEach
            if (entry.pdfBackground != null || entry.id in pendingPdfRenders || !file.isFile) return@forEach
            pendingPdfRenders += entry.id
            pdf.renderPage(file, pageIndex, 1280) { result ->
                handler.post {
                    pendingPdfRenders -= entry.id
                    result.onSuccess { bitmap ->
                        val currentIndex = pages.indexOfFirst { it.id == entry.id }
                        if (currentIndex >= 0 && kotlin.math.abs(currentIndex - selectedPageIndex) <= 1) {
                            pages[currentIndex] = pages[currentIndex].copy(pdfBackground = bitmap)
                        } else {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
    }
}

private fun loadEditorTabs(
    notebooksRoot: File,
    pdfServices: PdfServices,
    handler: Handler,
): List<EditorTab> {
    notebooksRoot.mkdirs()
    val restored = notebooksRoot.listFiles()
        .orEmpty()
        .filter { it.isDirectory }
        .sortedBy { it.name }
        .mapNotNull { directory ->
            runCatching { NativeNotebookContainer.loadManifest(directory) }.getOrNull()
                ?.let { document -> directory to document }
        }
        .mapIndexed { index, (directory, document) ->
            EditorTab(index + 1, document.title, directory, document, pdfServices, handler)
        }
    return restored.ifEmpty {
        listOf(EditorTab(1, "My first note", notebooksRoot.resolve("note-1"), pdfServices = pdfServices, mainHandler = handler))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MheenotesEditor(
    appSettings: AppSettings,
    initialOpenUri: android.net.Uri? = null,
    initialNotebookId: String? = null,
    hardwareKeySequence: Int = 0,
    hardwareKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN,
    onAppSettingsChanged: (AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val pdf = remember { PdfServices(context.contentResolver, context.filesDir.resolve("pdf")) }
    val actionMapStore = remember { ActionMapStore(context.filesDir.resolve("action-maps.bin")) }
    val actionMappings = remember {
        mutableStateListOf<ActionMapping>().apply {
            addAll(runCatching { actionMapStore.load() }.getOrDefault(emptyList()))
        }
    }
    val actionRecognizer = remember { ActionMapRecognizer() }
    var consumedOpenUri by rememberSaveable { mutableStateOf<String?>(null) }
    val tabs = remember {
        mutableStateListOf<EditorTab>().apply {
            addAll(loadEditorTabs(context.filesDir.resolve("notebooks"), pdf, handler))
        }
    }
    val openTabIds = remember { mutableStateListOf<Int>() }
    var screen by remember { mutableStateOf(AppScreen.Library) }
    var selectedTabId by remember { mutableIntStateOf(tabs.first().id) }
    var nextTabId by remember { mutableIntStateOf((tabs.maxOfOrNull { it.id } ?: 0) + 1) }
    var editorMode by remember { mutableStateOf(EditorMode.Ink) }
    var dockEdge by remember { mutableStateOf(runCatching { DockEdge.valueOf(appSettings.toolbarDock) }.getOrDefault(DockEdge.Top)) }
    var showSettings by remember { mutableStateOf(false) }
    var showActionMaps by remember { mutableStateOf(false) }
    var showAppSettings by remember { mutableStateOf(false) }
    var showFloatingNotes by remember { mutableStateOf(false) }
    var lastFloatingNotesTriggerAt by remember { mutableLongStateOf(0L) }
    var radialMenuRequestGeneration by remember { mutableIntStateOf(0) }
    var lastRadialMenuTriggerAt by remember { mutableLongStateOf(0L) }
    var exportDialogRequest by remember { mutableStateOf<ExportDialogRequest?>(null) }
    var pendingSaveExport by remember { mutableStateOf<PendingSaveExport?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pdfImportDestination by remember { mutableStateOf(PdfImportDestination.CurrentNotebook) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var openingNoteTitle by remember { mutableStateOf<String?>(null) }
    var pendingImagePlacement by remember { mutableStateOf<PendingImagePlacement?>(null) }
    val currentTab = { tabs.firstOrNull { it.id == selectedTabId } ?: tabs.first() }
    val sharingScope = rememberCoroutineScope()

    fun triggerFloatingNotes() {
        val now = SystemClock.uptimeMillis()
        if (now - lastFloatingNotesTriggerAt < 180L) return
        lastFloatingNotesTriggerAt = now
        showFloatingNotes = !showFloatingNotes
    }

    fun triggerRadialMenu() {
        val now = SystemClock.uptimeMillis()
        if (now - lastRadialMenuTriggerAt < 180L) return
        lastRadialMenuTriggerAt = now
        radialMenuRequestGeneration++
    }

    LaunchedEffect(hardwareKeySequence) {
        if (hardwareKeySequence > 0 && screen == AppScreen.Editor && !showAppSettings) {
            when (hardwareKeyCode) {
                appSettings.radialMenuKeyCode -> if (hardwareKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    triggerRadialMenu()
                }
                appSettings.floatingNotesKeyCode -> triggerFloatingNotes()
            }
        }
    }

    BackHandler(enabled = screen == AppScreen.Editor) {
        val tab = currentTab()
        tab.flushPendingInk()
        tab.pages.firstOrNull()?.let { tab.invalidateThumbnail(it.id) }
        screen = AppScreen.Library
    }


    fun shareGeneratedFile(fileName: String, mediaType: String, writer: (java.io.OutputStream) -> Unit) {
        sharingScope.launch {
            val generated = withContext(Dispatchers.IO) {
                runCatching {
                    val directory = context.cacheDir.resolve("shared").apply { mkdirs() }
                    val target = directory.resolve(fileName)
                    target.outputStream().buffered().use(writer)
                    target
                }
            }
            generated.onSuccess { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = mediaType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share with"))
            }.onFailure { importError = it.message ?: "The file could not be shared." }
        }
    }

    fun shareExport(request: ExportDialogRequest, requestedName: String, pageIndices: List<Int>) {
        val tab = tabs.firstOrNull { it.id == request.tabId } ?: return
        val fileName = normalizedExportFileName(requestedName, request.format)
        when (request.format) {
            ExportFormat.Pdf -> shareGeneratedFile(fileName, request.format.mediaType) { output ->
                PageExporter.writePdf(pageIndices.mapNotNull { tab.pages.getOrNull(it)?.toExportPage() }, output)
            }
            ExportFormat.Image -> shareGeneratedFile(fileName, request.format.mediaType) { output ->
                val page = tab.pages[request.pageIndex.coerceIn(0, tab.pages.lastIndex)]
                PageExporter.renderImage(page.toExportPage()).compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            ExportFormat.Notebook -> shareGeneratedFile(fileName, request.format.mediaType, tab::backup)
        }
    }

    LaunchedEffect(
        appSettings.penMaximumWidthMicrons,
        appSettings.pencilMaximumWidthMicrons,
        appSettings.highlighterMaximumWidthMicrons,
        appSettings.eraserMaximumWidthMicrons,
        appSettings.penWidthPresets,
        appSettings.pencilWidthPresets,
        appSettings.highlighterWidthPresets,
        appSettings.penColorPresets,
        appSettings.pencilColorPresets,
        appSettings.highlighterColorPresets,
        tabs.size,
    ) {
        tabs.forEach {
            it.updateMaximumToolWidths(appSettings)
            it.updateToolDefaults(appSettings)
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val placement = pendingImagePlacement
        pendingImagePlacement = null
        if (uri != null && placement != null) {
            val tab = tabs.firstOrNull { it.id == placement.tabId }
                ?: return@rememberLauncherForActivityResult
            val pageIndex = tab.pages.indexOfFirst { it.id == placement.pageId }
            val page = tab.pages.getOrNull(pageIndex) ?: return@rememberLauncherForActivityResult
            val storedAsset = runCatching {
                context.contentResolver.openInputStream(uri)?.use(tab::storeImageAsset)
                    ?: error("The selected image could not be opened.")
            }.onFailure { importError = it.message ?: "The image could not be imported." }.getOrNull()
            storedAsset?.let { (assetId, asset) ->
                val bitmap = BitmapFactory.decodeFile(asset.path)
                if (bitmap == null) {
                    asset.delete()
                    importError = "The selected image format could not be decoded."
                    return@let
                }
                val maxWidth = page.size.widthMicrons * .55f
                val maxHeight = page.size.heightMicrons * .45f
                var width = maxWidth
                var height = width * bitmap.height / bitmap.width.toFloat()
                if (height > maxHeight) { height = maxHeight; width = height * bitmap.width / bitmap.height.toFloat() }
                val zIndex = (page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
                tab.recordObjectEdit(page)
                page.objects += SceneObject.Image(
                    bitmap, asset,
                    (placement.position.x - width / 2f).coerceIn(0f, (page.size.widthMicrons - width).coerceAtLeast(0f)),
                    (placement.position.y - height / 2f).coerceIn(0f, (page.size.heightMicrons - height).coerceAtLeast(0f)),
                    width, height,
                    assetId = assetId,
                    zIndex = zIndex,
                )
                tab.persistObjects()
                tab.navigateToPage(pageIndex)
                if (tab.id == selectedTabId) editorMode = EditorMode.Object
            }
        }
    }
    val importPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pdf.importPdf(uri) { result ->
            result.onSuccess { imported ->
                handler.post {
                    if (pdfImportDestination == PdfImportDestination.NewNotebook) {
                        val id = nextTabId++
                        val importedTitle = imported.displayName.substringBeforeLast('.').ifBlank { "Imported PDF" }
                        val newTab = EditorTab(
                            id = id,
                            initialTitle = importedTitle,
                            root = context.filesDir.resolve("notebooks/note-$id"),
                            pdfServices = pdf,
                            mainHandler = handler,
                        )
                        runCatching { newTab.importPdf(imported, pdf, handler, replaceWithPdf = true) }
                            .onSuccess {
                                tabs += newTab
                                if (id !in openTabIds) openTabIds += id
                                selectedTabId = id
                                screen = AppScreen.Editor
                            }
                            .onFailure { error ->
                                runCatching { newTab.close() }
                                importError = error.message ?: "The PDF could not be imported."
                            }
                    } else {
                        runCatching { currentTab().importPdf(imported, pdf, handler) }
                            .onSuccess { screen = AppScreen.Editor }
                            .onFailure { importError = it.message ?: "The PDF could not be imported." }
                    }
                }
            }.onFailure { error ->
                handler.post { importError = error.message ?: "The PDF could not be opened." }
            }
        }
    }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val pending = pendingSaveExport
        if (uri != null && pending?.request?.format == ExportFormat.Pdf) runCatching {
            val tab = tabs.first { it.id == pending.request.tabId }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                PageExporter.writePdf(pending.pageIndices.mapNotNull { tab.pages.getOrNull(it)?.toExportPage() }, output)
            } ?: error("The selected PDF location could not be opened.")
        }.onFailure { importError = it.message ?: "The PDF could not be saved." }
        pendingSaveExport = null
    }
    val exportImage = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val pending = pendingSaveExport
        if (uri != null && pending?.request?.format == ExportFormat.Image) runCatching {
            val tab = tabs.first { it.id == pending.request.tabId }
            val page = tab.pages[pending.request.pageIndex.coerceIn(tab.pages.indices)]
            context.contentResolver.openOutputStream(uri)?.use { output ->
                PageExporter.renderImage(page.toExportPage()).compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: error("The selected image location could not be opened.")
        }.onFailure { importError = it.message ?: "The page image could not be saved." }
        pendingSaveExport = null
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val pending = pendingSaveExport
        if (uri != null && pending?.request?.format == ExportFormat.Notebook) runCatching {
            val tab = tabs.first { it.id == pending.request.tabId }
            context.contentResolver.openOutputStream(uri)?.use(tab::backup)
                ?: error("The selected notebook location could not be opened.")
        }.onFailure { importError = it.message ?: "The editable notebook could not be saved." }
        pendingSaveExport = null
    }
    val openNote = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val id = nextTabId++
            val root = context.filesDir.resolve("notebooks/import-$id")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val document = NativeNotebookContainer.unpack(input, root)
                tabs += EditorTab(id, document.title, root, document, pdf, handler)
                if (id !in openTabIds) openTabIds += id
                selectedTabId = id
                screen = AppScreen.Editor
            }
        }
    }


    LaunchedEffect(initialOpenUri) {
        val uri = initialOpenUri ?: return@LaunchedEffect
        if (consumedOpenUri == uri.toString()) return@LaunchedEffect
        consumedOpenUri = uri.toString()
        val mediaType = context.contentResolver.getType(uri).orEmpty()
        if (mediaType == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            pdf.importPdf(uri) { result ->
                result.onSuccess { imported ->
                    handler.post {
                        val id = nextTabId++
                        val title = imported.displayName.substringBeforeLast('.').ifBlank { "Imported PDF" }
                        val tab = EditorTab(id, title, context.filesDir.resolve("notebooks/note-$id"), pdfServices = pdf, mainHandler = handler)
                        runCatching { tab.importPdf(imported, pdf, handler, replaceWithPdf = true) }
                            .onSuccess { tabs += tab; openTabIds += id; selectedTabId = id; screen = AppScreen.Editor }
                            .onFailure { importError = it.message ?: "The PDF could not be opened." }
                    }
                }.onFailure { handler.post { importError = it.message ?: "The PDF could not be opened." } }
            }
        } else {
            runCatching {
                val id = nextTabId++
                val root = context.filesDir.resolve("notebooks/import-$id")
                val document = context.contentResolver.openInputStream(uri)?.use { NativeNotebookContainer.unpack(it, root) }
                    ?: error("The notebook could not be opened.")
                tabs += EditorTab(id, document.title, root, document, pdf, handler)
                openTabIds += id
                selectedTabId = id
                screen = AppScreen.Editor
            }.onFailure { importError = it.message ?: "The notebook could not be opened." }
        }
    }

    LaunchedEffect(initialNotebookId) {
        val requestedId = initialNotebookId ?: return@LaunchedEffect
        tabs.firstOrNull { it.persistentId == requestedId }?.let { tab ->
            if (tab.id !in openTabIds) openTabIds += tab.id
            selectedTabId = tab.id
            tab.markOpened()
            RecentNotesWidgetProvider.updateAll(context)
            screen = AppScreen.Editor
        }
    }

    DisposableEffect(Unit) { onDispose { tabs.forEach(EditorTab::close); pdf.close() } }

    val floatingNotesBlur by animateDpAsState(
        targetValue = if (showFloatingNotes) 18.dp else 0.dp,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "floating-notes-blur",
    )
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .blur(floatingNotesBlur),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 14 } +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing))
                ).togetherWith(
                    fadeOut(tween(200)) +
                    slideOutVertically(tween(200)) { -it / 18 } +
                    scaleOut(targetScale = 1.02f, animationSpec = tween(200))
                )
            },
            label = "library-editor-transition",
        ) { activeScreen -> if (activeScreen == AppScreen.Library) {
            LibraryScreen(
                tabs = tabs,
                onOpen = { id ->
                    val target = tabs.firstOrNull { it.id == id } ?: return@LibraryScreen
                    openingNoteTitle = target.title
                    sharingScope.launch {
                        delay(140)
                        selectedTabId = id
                        if (id !in openTabIds) openTabIds += id
                        target.markOpened()
                        RecentNotesWidgetProvider.updateAll(context)
                        screen = AppScreen.Editor
                        openingNoteTitle = null
                    }
                },
                onCreate = { spec ->
                    val id = nextTabId++
                    val tab = EditorTab(
                        id, spec.title, context.filesDir.resolve("notebooks/note-$id"),
                        pdfServices = pdf, mainHandler = handler,
                    )
                    tab.configureInitialPage(spec.size, spec.template, spec.backgroundArgb, spec.templateLineArgb)
                    tab.setCoverColor(spec.coverArgb)
                    tabs += tab
                    openTabIds += id
                    selectedTabId = id
                    screen = AppScreen.Editor
                },
                onImportPdf = {
                    pdfImportDestination = PdfImportDestination.NewNotebook
                    importPdf.launch(arrayOf("application/pdf"))
                },
                onOpenNote = { openNote.launch(arrayOf("application/zip", "application/octet-stream")) },
                onSettings = { showAppSettings = true },
                onExportNotebook = { target ->
                    exportDialogRequest = ExportDialogRequest(ExportFormat.Notebook, target.id)
                },
                onDuplicate = { source ->
                    val id = nextTabId++
                    runCatching {
                        val bytes = java.io.ByteArrayOutputStream().also(source::backup).toByteArray()
                        val root = context.filesDir.resolve("notebooks/note-$id")
                        val imported = NativeNotebookContainer.unpack(java.io.ByteArrayInputStream(bytes), root)
                        val now = System.currentTimeMillis()
                        val copy = imported.copy(
                            id = NotebookId(UUID.randomUUID().toString()),
                            title = "${source.title} copy",
                            createdAtEpochMillis = now,
                            modifiedAtEpochMillis = now,
                            lastOpenedAtEpochMillis = now,
                            trashedAtEpochMillis = null,
                        )
                        NativeNotebookContainer.saveManifest(copy, root)
                        tabs += EditorTab(id, copy.title, root, copy, pdf, handler)
                    }.onFailure { importError = it.message ?: "The notebook could not be duplicated." }
                },
                onPermanentDelete = { target ->
                    runCatching { target.permanentlyDelete() }
                        .onSuccess { openTabIds.remove(target.id); tabs.remove(target) }
                        .onFailure { importError = it.message ?: "The notebook could not be deleted." }
                },
            )
        } else {
            EditorScreen(
                tabs = openTabIds.mapNotNull { openId -> tabs.firstOrNull { it.id == openId } },
                allNotebooks = tabs,
                selectedTabId = selectedTabId,
                onSelectTab = { selectedTabId = it },
                onMoveTab = { id, direction ->
                    val from = openTabIds.indexOf(id)
                    val to = (from + direction).coerceIn(0, openTabIds.lastIndex)
                    if (from >= 0 && from != to) {
                        val moved = openTabIds.removeAt(from)
                        openTabIds.add(to, moved)
                    }
                },
                onCloseTab = { id ->
                    openTabIds.remove(id)
                    val remaining = tabs.filter { it.id in openTabIds }
                    if (remaining.isEmpty()) screen = AppScreen.Library else selectedTabId = remaining.last().id
                },
                tab = currentTab(),
                mode = editorMode,
                onMode = { editorMode = it },
                dockEdge = dockEdge,
                onDockEdge = { edge -> dockEdge = edge; onAppSettingsChanged(appSettings.copy(toolbarDock = edge.name)) },
                onHome = {
                    val tab = currentTab()
                    tab.flushPendingInk()
                    tab.pages.firstOrNull()?.let { tab.invalidateThumbnail(it.id) }
                    screen = AppScreen.Library
                },

                onImage = { pageId, position ->
                    pendingImagePlacement = PendingImagePlacement(currentTab().id, pageId, position)
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onImportPdf = {
                    pdfImportDestination = PdfImportDestination.CurrentNotebook
                    importPdf.launch(arrayOf("application/pdf"))
                },
                onExportPdf = { exportDialogRequest = ExportDialogRequest(ExportFormat.Pdf, currentTab().id) },
                onExportImage = {
                    val tab = currentTab()
                    exportDialogRequest = ExportDialogRequest(ExportFormat.Image, tab.id, tab.selectedPageIndex)
                },
                onExportNotebook = { exportDialogRequest = ExportDialogRequest(ExportFormat.Notebook, currentTab().id) },
                onSettings = { showSettings = true },
                actionMappings = actionMappings,
                actionRecognizer = actionRecognizer,
                showDiagnostics = showDiagnostics,
                radialMenuRequestGeneration = radialMenuRequestGeneration,
                appSettings = appSettings,
                onAppSettingsChanged = onAppSettingsChanged,
                onStylusButtonPressed = { button ->
                    if (!showAppSettings) {
                        when (floatingNotesShortcutCode(button)) {
                            appSettings.radialMenuKeyCode -> if (appSettings.radialMenuKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                                triggerRadialMenu()
                            }
                            appSettings.floatingNotesKeyCode -> triggerFloatingNotes()
                        }
                    }
                },
            )
        } }
    }

    openingNoteTitle?.let { title ->
        Box(Modifier.fillMaxSize().background(Color(0xB30B0D11)).zIndex(100f), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 12.dp) {
                Row(Modifier.padding(horizontal = 26.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text("Opening $title", style = MaterialTheme.typography.titleMedium)
                        Text("Preparing pages and ink…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showFloatingNotes && screen == AppScreen.Editor) FloatingNotesOverlay(
        notebooks = tabs.filter { it.trashedAtEpochMillis == null },
        selectedTabId = selectedTabId,
        onOpen = { target ->
            if (target.id !in openTabIds) openTabIds += target.id
            selectedTabId = target.id
            target.markOpened()
            RecentNotesWidgetProvider.updateAll(context)
            showFloatingNotes = false
        },
        onDismiss = { showFloatingNotes = false },
    )

    if (showSettings) EditorSettingsSheet(
        session = currentTab().session,
        dockEdge = dockEdge,
        onDockEdge = { edge -> dockEdge = edge; onAppSettingsChanged(appSettings.copy(toolbarDock = edge.name)) },
        onDiagnostics = { showDiagnostics = !showDiagnostics },
        onActionMaps = { showSettings = false; showActionMaps = true },
        onDismiss = { showSettings = false },
    )
    if (showActionMaps) ActionMapManager(
        mappings = actionMappings,
        onSave = { mapping ->
            val existingIndex = actionMappings.indexOfFirst { it.id == mapping.id }
            if (existingIndex >= 0) actionMappings[existingIndex] = mapping else actionMappings += mapping
            actionMapStore.save(actionMappings)
        },
        onDelete = { mapping ->
            actionMappings.remove(mapping)
            actionMapStore.save(actionMappings)
        },
        onDismiss = { showActionMaps = false },
    )
    if (showAppSettings) AppSettingsDialog(
        settings = appSettings,
        hardwareKeySequence = hardwareKeySequence,
        hardwareKeyCode = hardwareKeyCode,
        onSettingsChanged = onAppSettingsChanged,
        onDismiss = { showAppSettings = false },
    )
    exportDialogRequest?.let { request ->
        val target = tabs.firstOrNull { it.id == request.tabId }
        if (target != null) ExportFileDialog(
            request = request,
            notebookTitle = target.title,
            pageCount = target.pages.size,
            currentPage = request.pageIndex.coerceIn(0, target.pages.lastIndex),
            onSave = { requestedName, indices ->
                val fileName = normalizedExportFileName(requestedName, request.format)
                pendingSaveExport = PendingSaveExport(request, indices)
                exportDialogRequest = null
                when (request.format) {
                    ExportFormat.Pdf -> exportPdf.launch(fileName)
                    ExportFormat.Image -> exportImage.launch(fileName)
                    ExportFormat.Notebook -> backup.launch(fileName)
                }
            },
            onShare = { requestedName, indices ->
                exportDialogRequest = null
                shareExport(request, requestedName, indices)
            },
            onDismiss = { exportDialogRequest = null },
        )
    }
    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun LibraryScreen(
    tabs: List<EditorTab>,
    onOpen: (Int) -> Unit,
    onCreate: (NotebookCreationSpec) -> Unit,
    onImportPdf: () -> Unit,
    onOpenNote: () -> Unit,
    onSettings: () -> Unit,
    onExportNotebook: (EditorTab) -> Unit,
    onDuplicate: (EditorTab) -> Unit,
    onPermanentDelete: (EditorTab) -> Unit,
) {
    val context = LocalContext.current
    val folderPreferences = remember { context.getSharedPreferences("library-folders", android.content.Context.MODE_PRIVATE) }
    val folders = remember {
        mutableStateListOf<String>().apply { addAll(folderPreferences.getStringSet("folders", emptySet()).orEmpty().sorted()) }
    }
    val trashedFolderRoots = remember {
        mutableStateListOf<String>().apply {
            addAll(folderPreferences.getStringSet("trashed-folder-roots", emptySet()).orEmpty().sorted())
        }
    }
    val documentFolders = tabs.mapNotNull(EditorTab::folderPath).distinct().sorted()
    LaunchedEffect(documentFolders) {
        val missing = documentFolders.filterNot(folders::contains)
        if (missing.isNotEmpty()) {
            folders.addAll(missing)
            folderPreferences.edit().putStringSet("folders", folders.toSet()).apply()
        }
    }
    var query by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var section by remember { mutableStateOf(LibrarySection.Main) }
    var layout by remember { mutableStateOf(LibraryLayout.Grid) }
    var sort by remember { mutableStateOf(LibrarySort.Name) }
    var sortMenu by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var mainExpanded by rememberSaveable { mutableStateOf(true) }
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }
    var mainBounds by remember { mutableStateOf<Rect?>(null) }
    val folderDropTargets = remember { mutableStateMapOf<String, Pair<String, Rect>>() }
    var dragTargetFolder by remember { mutableStateOf<String?>(null) }
    var dragTargetsRoot by remember { mutableStateOf(false) }
    var draggedTabId by remember { mutableStateOf<Int?>(null) }
    var draggedFolderPath by remember { mutableStateOf<String?>(null) }
    var draggedNotePoint by remember { mutableStateOf(Offset.Zero) }
    val allFolders = normalizedFolderPaths(folders + documentFolders)
    val activeFolders = allFolders.filterNot { path -> trashedFolderRoots.any { isFolderOrDescendant(path, it) } }

    fun persistFolders(updated: List<String>) {
        folders.clear()
        folders.addAll(updated.distinct().sortedBy(String::lowercase))
        folderPreferences.edit().putStringSet("folders", folders.toSet()).apply()
    }

    fun persistTrashedFolderRoots(updated: List<String>) {
        trashedFolderRoots.clear()
        trashedFolderRoots.addAll(updated.distinct().sortedBy(String::lowercase))
        folderPreferences.edit().putStringSet("trashed-folder-roots", trashedFolderRoots.toSet()).apply()
    }

    fun relocateFolder(source: String, desiredDestination: String) {
        if (source == desiredDestination || isFolderOrDescendant(desiredDestination, source)) return
        val occupied = allFolders.filterNot { isFolderOrDescendant(it, source) }.toSet()
        var destination = desiredDestination
        var suffix = 2
        while (destination in occupied) {
            destination = "$desiredDestination ($suffix)"
            suffix++
        }
        val updatedFolders = allFolders.map { path ->
            if (isFolderOrDescendant(path, source)) replaceFolderRoot(path, source, destination) else path
        }
        tabs.filter { isFolderOrDescendant(it.folderPath, source) }.forEach { tab ->
            tab.moveToFolder(replaceFolderRoot(tab.folderPath!!, source, destination))
        }
        val updatedTrashedRoots = trashedFolderRoots.map { root ->
            if (isFolderOrDescendant(root, source)) {
                val movedRoot = replaceFolderRoot(root, source, destination)
                val members = folderPreferences.getStringSet(folderTrashMembersKey(root), emptySet()).orEmpty()
                folderPreferences.edit()
                    .remove(folderTrashMembersKey(root))
                    .putStringSet(folderTrashMembersKey(movedRoot), members)
                    .apply()
                movedRoot
            } else root
        }
        persistFolders(updatedFolders)
        persistTrashedFolderRoots(updatedTrashedRoots)
        selectedFolder = selectedFolder?.let { path ->
            if (isFolderOrDescendant(path, source)) replaceFolderRoot(path, source, destination) else path
        }
        val expandedSnapshot = expandedFolders.toMap()
        expandedFolders.clear()
        expandedSnapshot.forEach { (path, expanded) ->
            expandedFolders[if (isFolderOrDescendant(path, source)) replaceFolderRoot(path, source, destination) else path] = expanded
        }
    }

    fun moveFolderTree(source: String, destinationParent: String?) {
        if (destinationParent != null && isFolderOrDescendant(destinationParent, source)) return
        val leaf = source.substringAfterLast('/')
        val destination = listOfNotNull(destinationParent, leaf).joinToString("/")
        relocateFolder(source, destination)
    }

    fun renameFolderTree(source: String, requestedName: String) {
        val leaf = requestedName.trim().trim('/').substringAfterLast('/').takeIf(String::isNotBlank) ?: return
        val parent = source.substringBeforeLast('/', missingDelimiterValue = "").takeIf(String::isNotBlank)
        relocateFolder(source, listOfNotNull(parent, leaf).joinToString("/"))
    }

    fun trashFolderTree(folder: String) {
        val nestedTrashRoots = trashedFolderRoots.filter { isFolderOrDescendant(it, folder) }
        val movedIds = linkedSetOf<String>()
        nestedTrashRoots.forEach { nested ->
            movedIds += folderPreferences.getStringSet(folderTrashMembersKey(nested), emptySet()).orEmpty()
        }
        tabs.filter { it.trashedAtEpochMillis == null && isFolderOrDescendant(it.folderPath, folder) }.forEach { tab ->
            movedIds += tab.persistentId
            tab.moveToTrash()
        }
        val editor = folderPreferences.edit()
        nestedTrashRoots.forEach { editor.remove(folderTrashMembersKey(it)) }
        editor.putStringSet(folderTrashMembersKey(folder), movedIds).apply()
        persistTrashedFolderRoots(trashedFolderRoots.filterNot { it in nestedTrashRoots } + folder)
        if (selectedFolder?.let { isFolderOrDescendant(it, folder) } == true) selectedFolder = null
    }

    fun restoreFolderTree(folder: String) {
        val members = folderPreferences.getStringSet(folderTrashMembersKey(folder), emptySet()).orEmpty()
        tabs.filter { it.persistentId in members }.forEach(EditorTab::restoreFromTrash)
        folderPreferences.edit().remove(folderTrashMembersKey(folder)).apply()
        persistTrashedFolderRoots(trashedFolderRoots - folder)
    }

    LaunchedEffect(section, selectedFolder, mainExpanded, activeFolders) { folderDropTargets.clear() }
    val visibleTabs = tabs.asSequence()
        .filter { tab ->
            when (section) {
                LibrarySection.Main -> tab.trashedAtEpochMillis == null &&
                    (query.isNotBlank() || tab.folderPath == selectedFolder)
                LibrarySection.Recent -> tab.trashedAtEpochMillis == null
                LibrarySection.Favorites -> tab.trashedAtEpochMillis == null && tab.favorite
                LibrarySection.Trash -> tab.trashedAtEpochMillis != null &&
                    trashedFolderRoots.none { isFolderOrDescendant(tab.folderPath, it) }
            }
        }
        .filter { it.title.contains(query, ignoreCase = true) || it.folderPath.orEmpty().contains(query, ignoreCase = true) }
        .let { sequence -> when {
            section == LibrarySection.Recent || sort == LibrarySort.Recent -> sequence.sortedByDescending(EditorTab::lastOpenedAtEpochMillis)
            sort == LibrarySort.PageCount -> sequence.sortedByDescending { it.pages.size }
            else -> sequence.sortedBy { it.title.lowercase() }
        } }
        .toList()
    val heading = when (section) {
        LibrarySection.Main -> selectedFolder ?: stringResource(R.string.main)
        LibrarySection.Recent -> stringResource(R.string.recently_opened)
        LibrarySection.Favorites -> stringResource(R.string.favorites)
        LibrarySection.Trash -> stringResource(R.string.trash)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxWidth < 720.dp
        val sidebarWidth by animateDpAsState(if (compact) 72.dp else 220.dp, tween(300, easing = FastOutSlowInEasing), label = "sidebar-width")
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(sidebarWidth).fillMaxHeight().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = if (compact) 8.dp else 20.dp, vertical = 16.dp),
                horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(if (compact) "M" else "Mheenotes", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    MainNavigationItem(
                        expanded = mainExpanded,
                        selected = section == LibrarySection.Main && selectedFolder == null,
                        highlighted = dragTargetsRoot,
                        compact = compact,
                        modifier = Modifier.onGloballyPositioned { mainBounds = it.boundsInRoot() },
                        onToggle = { mainExpanded = !mainExpanded },
                        onClick = { section = LibrarySection.Main; selectedFolder = null },
                    )
                    if (mainExpanded && !compact) {
                        LibraryHierarchy(
                            tabs = tabs.filter { it.trashedAtEpochMillis == null },
                            folderPaths = activeFolders,
                            selectedFolder = selectedFolder.takeIf { section == LibrarySection.Main },
                            expandedFolders = expandedFolders,
                            dragTargetFolder = dragTargetFolder,
                            onSelectFolder = { folder ->
                                section = LibrarySection.Main
                                selectedFolder = folder
                            },
                            onToggleFolder = { folder -> expandedFolders[folder] = expandedFolders[folder] != true },
                            onOpenNote = onOpen,
                            onFolderPositioned = { folder, bounds ->
                                folderDropTargets["tree:$folder"] = folder to bounds
                            },
                        )
                    }
                    NavigationItem(Icons.Outlined.History, stringResource(R.string.recent), section == LibrarySection.Recent, compact = compact) { section = LibrarySection.Recent }
                    NavigationItem(Icons.Outlined.Star, stringResource(R.string.favorites), section == LibrarySection.Favorites, compact = compact) { section = LibrarySection.Favorites }
                    NavigationItem(Icons.Outlined.Delete, stringResource(R.string.trash), section == LibrarySection.Trash, compact = compact) { section = LibrarySection.Trash }
                }
                NavigationItem(Icons.Outlined.Settings, stringResource(R.string.settings), false, compact = compact, onClick = onSettings)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(if (compact) 12.dp else 28.dp)) {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(heading, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.notes_count, visibleTabs.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = onOpenNote) { Icon(Icons.Outlined.FileOpen, stringResource(R.string.open_notebook)) }
                            IconButton(onClick = onImportPdf) { Icon(Icons.Outlined.PictureAsPdf, stringResource(R.string.import_pdf)) }
                            LibrarySortAndLayout(sortMenu, { sortMenu = it }, sort, { sort = it }, layout, { layout = it })
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = query.isNotEmpty(),
                                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
                                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.7f),
                                ) {
                                    IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Outlined.Close, "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            placeholder = { Text(stringResource(R.string.search_notes)) },
                            shape = RoundedCornerShape(100),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(heading, style = MaterialTheme.typography.headlineMedium)
                            Text("${visibleTabs.size} notebook${if (visibleTabs.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = query.isNotEmpty(),
                                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
                                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.7f),
                                ) {
                                    IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Outlined.Close, "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            placeholder = { Text(stringResource(R.string.search_notes)) },
                            shape = RoundedCornerShape(100),
                            modifier = Modifier.width(300.dp),
                        )
                        IconButton(onClick = onOpenNote) { Icon(Icons.Outlined.FileOpen, stringResource(R.string.open_notebook)) }
                        IconButton(onClick = onImportPdf) { Icon(Icons.Outlined.PictureAsPdf, stringResource(R.string.import_pdf)) }
                        LibrarySortAndLayout(sortMenu, { sortMenu = it }, sort, { sort = it }, layout, { layout = it })
                    }
                }
                Spacer(Modifier.height(if (compact) 14.dp else 28.dp))
                val folderCards = when {
                    section == LibrarySection.Main && query.isBlank() -> immediateFolderChildren(activeFolders, selectedFolder)
                    section == LibrarySection.Trash -> trashedFolderRoots.filter {
                        query.isBlank() || it.contains(query, ignoreCase = true)
                    }
                    else -> emptyList()
                }
                if (visibleTabs.isEmpty() && folderCards.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val pulseTransition = rememberInfiniteTransition(label = "empty-pulse")
                        val pulseAlpha by pulseTransition.animateFloat(
                            initialValue = 0.5f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                            label = "empty-pulse-alpha",
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (section == LibrarySection.Trash) Icons.Outlined.Delete else Icons.Outlined.Edit,
                                null,
                                Modifier.size(72.dp).graphicsLayer { alpha = 0.18f },
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(if (section == LibrarySection.Trash) R.string.trash_empty else R.string.no_notebooks),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (section != LibrarySection.Trash) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Tap + to create your first notebook",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                                Spacer(Modifier.height(24.dp))
                                Surface(
                                    onClick = { showCreate = true },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha),
                                    modifier = Modifier.size(56.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Add, "Create notebook", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                } else if (layout == LibraryLayout.Grid || folderCards.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (compact) 140.dp else 190.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
                    ) {
                        items(folderCards, key = { "folder:$it" }) { folder ->
                            val folderIsTrashed = section == LibrarySection.Trash
                            Box(Modifier.fillMaxWidth().staggeredEntrance("folder:$folder", folderCards.indexOf(folder))) {
                                FolderCard(
                                folder = folder,
                                noteCount = if (folderIsTrashed) {
                                    tabs.count { it.trashedAtEpochMillis != null && isFolderOrDescendant(it.folderPath, folder) }
                                } else tabs.count {
                                    it.trashedAtEpochMillis == null && isFolderOrDescendant(it.folderPath, folder)
                                },
                                highlighted = dragTargetFolder == folder,
                                trashed = folderIsTrashed,
                                modifier = if (folderIsTrashed) Modifier else Modifier.onGloballyPositioned {
                                    folderDropTargets["grid:$folder"] = folder to it.boundsInRoot()
                                },
                                onClick = {
                                    if (!folderIsTrashed) {
                                        section = LibrarySection.Main
                                        selectedFolder = folder
                                        mainExpanded = true
                                        expandedFolders[folder] = true
                                    }
                                },
                                onRename = { name -> renameFolderTree(folder, name) },
                                onTrash = { trashFolderTree(folder) },
                                onRestore = { restoreFolderTree(folder) },
                                onDragPosition = { point ->
                                    if (!folderIsTrashed) {
                                        draggedFolderPath = folder
                                        draggedNotePoint = point
                                        dragTargetsRoot = mainBounds?.contains(point) == true
                                        dragTargetFolder = if (dragTargetsRoot) null else {
                                            folderDropTargets.values.firstOrNull { (target, bounds) ->
                                                bounds.contains(point) && target != folder && !isFolderOrDescendant(target, folder)
                                            }?.first
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (!folderIsTrashed) {
                                        when {
                                            dragTargetsRoot -> moveFolderTree(folder, null)
                                            dragTargetFolder != null -> moveFolderTree(folder, dragTargetFolder)
                                        }
                                    }
                                    draggedFolderPath = null
                                    draggedNotePoint = Offset.Zero
                                    dragTargetsRoot = false
                                    dragTargetFolder = null
                                },
                                onDragCancel = {
                                    draggedFolderPath = null
                                    draggedNotePoint = Offset.Zero
                                    dragTargetsRoot = false
                                    dragTargetFolder = null
                                },
                                )
                            }
                        }
                        items(visibleTabs.size, key = { index -> visibleTabs[index].id }) { index ->
                            val tab = visibleTabs[index]
                            Box(
                                Modifier.fillMaxWidth().staggeredEntrance(
                                    "notebook:${tab.id}",
                                    folderCards.size + index,
                                ),
                            ) {


                                NotebookCard(
                                    tab, activeFolders, onClick = { onOpen(tab.id) }, onDuplicate, onPermanentDelete, onExportNotebook,
                                onDragPosition = { point ->
                                    draggedTabId = tab.id
                                    draggedNotePoint = point
                                    dragTargetsRoot = mainBounds?.contains(point) == true
                                    dragTargetFolder = if (dragTargetsRoot) null else {
                                        folderDropTargets.values.firstOrNull { (_, bounds) -> bounds.contains(point) }?.first
                                    }
                                },
                                onDragEnd = {
                                    when {
                                        dragTargetsRoot -> tab.moveToFolder(null)
                                        dragTargetFolder != null -> tab.moveToFolder(dragTargetFolder)
                                    }
                                    val handled = dragTargetsRoot || dragTargetFolder != null
                                    draggedTabId = null
                                    draggedNotePoint = Offset.Zero
                                    dragTargetsRoot = false; dragTargetFolder = null
                                    handled
                                },
                                onDragCancel = {
                                    draggedTabId = null
                                    draggedNotePoint = Offset.Zero
                                    dragTargetsRoot = false
                                    dragTargetFolder = null
                                },
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(visibleTabs, key = { _, tab -> tab.id }) { index, tab ->
                            Box(Modifier.fillMaxWidth().staggeredEntrance("notebook-row:${tab.id}", index)) {
                                NotebookListRow(tab, activeFolders, onClick = { onOpen(tab.id) }, onDuplicate, onPermanentDelete, onExportNotebook)
                            }
                        }
                    }
                }
            }
        }
        draggedTabId?.let { id ->
            tabs.firstOrNull { it.id == id }?.let { tab ->
                val density = LocalDensity.current
                Surface(
                    modifier = Modifier
                        .size(width = 142.dp, height = 176.dp)
                        .offset {
                            IntOffset(
                                draggedNotePoint.x.roundToInt() - with(density) { 71.dp.roundToPx() },
                                draggedNotePoint.y.roundToInt() - with(density) { 88.dp.roundToPx() },
                            )
                        }
                        .zIndex(200f),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(tab.coverArgb),
                    shadowElevation = 24.dp,
                ) {
                    tab.pages.firstOrNull()?.let { page ->
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            PageThumbnailPreview(
                                page,
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(page.previewAspectRatio())
                                    .clip(RoundedCornerShape(5.dp)),
                            )
                        }
                    }
                }
            }
        }
        draggedFolderPath?.let { folder ->
            val density = LocalDensity.current
            Surface(
                modifier = Modifier
                    .size(width = 142.dp, height = 176.dp)
                    .offset {
                        IntOffset(
                            draggedNotePoint.x.roundToInt() - with(density) { 71.dp.roundToPx() },
                            draggedNotePoint.y.roundToInt() - with(density) { 88.dp.roundToPx() },
                        )
                    }
                    .zIndex(200f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 24.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Folder, folder.substringAfterLast('/'), Modifier.size(70.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        var fabExpanded by remember { mutableStateOf(false) }
        val fabRotation by animateFloatAsState(
            if (fabExpanded) 45f else 0f, tween(250, easing = FastOutSlowInEasing), label = "fab-rotation",
        )
        if (fabExpanded) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fabExpanded = false },
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(if (compact) 16.dp else 28.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(260, easing = BackOutEasing)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                        Text("New folder", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    FloatingActionButton(onClick = { fabExpanded = false; showNewFolder = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.CreateNewFolder, stringResource(R.string.create_folder))
                    }
                }
            }
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(180, delayMillis = 40)) + scaleIn(initialScale = 0.6f, animationSpec = tween(260, delayMillis = 40, easing = BackOutEasing)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f, animationSpec = tween(120)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                        Text("New notebook", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                    }
                    FloatingActionButton(onClick = { fabExpanded = false; showCreate = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.NoteAdd, stringResource(R.string.create_notebook))
                    }
                }
            }
            FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                Icon(
                    Icons.Outlined.Add, "Create",
                    modifier = Modifier.graphicsLayer { rotationZ = fabRotation },
                )
            }
        }
    }
    if (showCreate) NewNotebookDialog(
        onCreate = { showCreate = false; onCreate(it) },
        onDismiss = { showCreate = false },
    )
    if (showNewFolder) AlertDialog(
        onDismissRequest = { showNewFolder = false },
        title = { Text(stringResource(R.string.new_folder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    newFolderName,
                    { newFolderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    supportingText = {
                        Text(
                            selectedFolder?.takeIf { section == LibrarySection.Main }
                                ?.let { stringResource(R.string.folder_created_inside, it) }
                                ?: stringResource(R.string.folder_created_in_main),
                        )
                    },
                )
                Text(stringResource(R.string.folder_nested_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                enabled = newFolderName.trim().trim('/').isNotEmpty(),
                onClick = {
                    val relativeName = newFolderName.trim().trim('/')
                    val parent = selectedFolder.takeIf { section == LibrarySection.Main }
                    val requested = listOfNotNull(parent, relativeName).joinToString("/")
                    var name = requested
                    var suffix = 2
                    while (name in allFolders) {
                        name = "$requested ($suffix)"
                        suffix++
                    }
                    folders += name
                    folderPreferences.edit().putStringSet("folders", folders.toSet()).apply()
                    section = LibrarySection.Main
                    selectedFolder = name
                    mainExpanded = true
                    normalizedFolderPaths(listOf(name)).forEach { expandedFolders[it] = true }
                    newFolderName = ""
                    showNewFolder = false
                },
            ) { Text("Create") }
        },
    )
}

@Composable
private fun NewNotebookDialog(
    onCreate: (NotebookCreationSpec) -> Unit,
    onDismiss: () -> Unit,
    initial: NotebookCreationSpec? = null,
) {
    val initialWidthMm = (initial?.size?.widthMicrons ?: 210_000f) / 1_000f
    val initialHeightMm = (initial?.size?.heightMicrons ?: 297_000f) / 1_000f
    val initialPaper = PaperPreset.entries.firstOrNull { preset ->
        preset != PaperPreset.Custom &&
            kotlin.math.abs(minOf(initialWidthMm, initialHeightMm) - minOf(preset.widthMm, preset.heightMm)) < .2f &&
            kotlin.math.abs(maxOf(initialWidthMm, initialHeightMm) - maxOf(preset.widthMm, preset.heightMm)) < .2f
    } ?: PaperPreset.Custom
    var title by remember(initial) { mutableStateOf(initial?.title ?: "Untitled note") }
    var paper by remember(initial) { mutableStateOf(initialPaper) }
    var landscape by remember(initial) { mutableStateOf(initialWidthMm > initialHeightMm) }
    var template by remember(initial) { mutableStateOf(initial?.template ?: PageTemplate.Blank) }
    var customWidth by remember(initial) { mutableStateOf(initialWidthMm.toString()) }
    var customHeight by remember(initial) { mutableStateOf(initialHeightMm.toString()) }
    var background by remember(initial) { mutableIntStateOf(initial?.backgroundArgb ?: 0xFFFFFEFA.toInt()) }
    var lines by remember(initial) { mutableIntStateOf(initial?.templateLineArgb ?: 0x402563EB) }
    var cover by remember(initial) { mutableIntStateOf(initial?.coverArgb ?: 0xFF303747.toInt()) }
    val widthMm = if (paper == PaperPreset.Custom) customWidth.toFloatOrNull() else paper.widthMm
    val heightMm = if (paper == PaperPreset.Custom) customHeight.toFloatOrNull() else paper.heightMm
    val valid = title.isNotBlank() && widthMm != null && heightMm != null && widthMm in 50f..1_000f && heightMm in 50f..1_000f
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.82f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(if (initial == null) "Create notebook" else "Edit notebook", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(.9f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Paper size", style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PaperPreset.entries.chunked(4).forEach { choices ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    choices.forEach { choice -> SelectablePill(choice.label, paper == choice) { paper = choice } }
                                }
                            }
                        }
                        if (paper == PaperPreset.Custom) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(customWidth, { customWidth = it }, label = { Text("Width mm") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(customHeight, { customHeight = it }, label = { Text("Height mm") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Landscape", Modifier.weight(1f))
                            Switch(landscape, { landscape = it })
                        }
                        Text("Page template", style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PageTemplate.entries.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { choice -> SelectablePill(choice.name.replace("MusicStaff", "Music"), template == choice) { template = choice } }
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(.7f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Paper color", style = MaterialTheme.typography.titleSmall)
                        ColorChoiceRow(listOf(0xFFFFFEFA.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFF7E6.toInt(), 0xFFF4F1E8.toInt(), 0xFF1D222B.toInt()), background) { background = it }
                        Text("Template lines", style = MaterialTheme.typography.titleSmall)
                        ColorChoiceRow(listOf(0x402563EB, 0x40777777, 0x40508A60, 0x40B45353, 0x60D6D9E0), lines) { lines = it }
                        Text("Cover", style = MaterialTheme.typography.titleSmall)
                        ColorChoiceRow(listOf(0xFF303747.toInt(), 0xFF315C8C.toInt(), 0xFF6A3F72.toInt(), 0xFF386A55.toInt(), 0xFF8A4A3B.toInt()), cover) { cover = it }
                        Box(
                            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(background)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(template.name, color = if (Color(background).luminance() > .5f) Color.DarkGray else Color.White)
                        }
                        if (initial != null) Text(
                            "Paper settings update regular pages. Imported PDF pages keep their original size.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            var width = widthMm!! * 1_000f
                            var height = heightMm!! * 1_000f
                            if (landscape != (width > height)) width = height.also { height = width }
                            onCreate(NotebookCreationSpec(title.trim(), PageSize(width, height), template, background, lines, cover))
                        },
                    ) { Text(if (initial == null) "Create" else "Save changes") }
                }
            }
        }
    }
}

@Composable
private fun InsertPageDialog(
    currentPage: PageEntry,
    onInsert: (PageInsertSpec, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var paper by remember { mutableStateOf(PaperPreset.A4) }
    var landscape by remember { mutableStateOf(false) }
    var template by remember { mutableStateOf(currentPage.template) }
    var background by remember { mutableIntStateOf(currentPage.backgroundArgb) }
    var lines by remember { mutableIntStateOf(currentPage.templateLineArgb) }
    var customWidth by remember { mutableStateOf((currentPage.size.widthMicrons / 1_000f).toString()) }
    var customHeight by remember { mutableStateOf((currentPage.size.heightMicrons / 1_000f).toString()) }
    var doNotAskAgain by remember { mutableStateOf(false) }
    val widthMm = if (paper == PaperPreset.Custom) customWidth.toFloatOrNull() else paper.widthMm
    val heightMm = if (paper == PaperPreset.Custom) customHeight.toFloatOrNull() else paper.heightMm
    val valid = widthMm != null && heightMm != null && widthMm in 50f..1_000f && heightMm in 50f..1_000f

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.76f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Text("Insert page", style = MaterialTheme.typography.headlineSmall)
                Text("Choose the paper before it is added", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Size", style = MaterialTheme.typography.titleSmall)
                        PaperPreset.entries.chunked(4).forEach { choices ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                choices.forEach { choice -> SelectablePill(choice.label, paper == choice) { paper = choice } }
                            }
                        }
                        if (paper == PaperPreset.Custom) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(customWidth, { customWidth = it }, label = { Text("Width mm") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(customHeight, { customHeight = it }, label = { Text("Height mm") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Landscape", Modifier.weight(1f))
                            Switch(landscape, { landscape = it })
                        }
                        Text("Template", style = MaterialTheme.typography.titleSmall)
                        PageTemplate.entries.chunked(3).forEach { choices ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                choices.forEach { choice ->
                                    SelectablePill(choice.name.replace("MusicStaff", "Music"), template == choice) { template = choice }
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(.75f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Paper color", style = MaterialTheme.typography.titleSmall)
                        ColorChoiceRow(listOf(0xFFFFFEFA.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFF7E6.toInt(), 0xFFF4F1E8.toInt(), 0xFF1D222B.toInt()), background) { background = it }
                        Text("Template lines", style = MaterialTheme.typography.titleSmall)
                        ColorChoiceRow(listOf(0x402563EB, 0x40777777, 0x40508A60, 0x40B45353, 0x60D6D9E0), lines) { lines = it }
                        Box(
                            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(background)),
                            contentAlignment = Alignment.Center,
                        ) { Text(template.name, color = if (Color(background).luminance() > .5f) Color.DarkGray else Color.White) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Don't ask again")
                                Text("You can turn this back on in Settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(doNotAskAgain, { doNotAskAgain = it })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            var width = widthMm!! * 1_000f
                            var height = heightMm!! * 1_000f
                            if (landscape && height > width) width = height.also { height = width }
                            onInsert(PageInsertSpec(PageSize(width, height), template, background, lines), doNotAskAgain)
                        },
                    ) { Text("Insert") }
                }
            }
        }
    }
}

@Composable
private fun SelectablePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, tween(120), label = "pill-press")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        interactionSource = interaction,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) { Text(label, Modifier.padding(horizontal = 11.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun ColorChoiceRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        colors.forEach { argb ->
            val size by animateDpAsState(if (selected == argb) 38.dp else 34.dp, tween(160), label = "color-choice")
            Surface(
                onClick = { onSelect(argb) },
                modifier = Modifier.size(size),
                shape = RoundedCornerShape(50),
                color = Color(argb),
                border = androidx.compose.foundation.BorderStroke(if (selected == argb) 3.dp else 1.dp, MaterialTheme.colorScheme.primary),
            ) {}
        }
        IconButton(onClick = { showPicker = true }, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Outlined.Palette, "Choose any color")
        }
    }
    if (showPicker) ThemeColorPickerDialog(
        title = "Color",
        initialArgb = selected,
        onSelect = { onSelect(it); showPicker = false },
        onDismiss = { showPicker = false },
    )
}

@Composable
private fun MainNavigationItem(
    expanded: Boolean,
    selected: Boolean,
    highlighted: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .965f else 1f, tween(120), label = "main-nav-press")
    Surface(
        onClick = onClick,
        color = if (selected || highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interaction,
    ) {
        Row(
            Modifier.then(if (compact) Modifier.size(48.dp) else Modifier.fillMaxWidth()).padding(if (compact) 12.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
        ) {
            if (!compact) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                        if (expanded) "Collapse Main" else "Expand Main",
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            Icon(Icons.Outlined.Home, null, Modifier.size(20.dp))
            if (!compact) {
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.main), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LibraryHierarchy(
    tabs: List<EditorTab>,
    folderPaths: List<String>,
    selectedFolder: String?,
    expandedFolders: Map<String, Boolean>,
    dragTargetFolder: String?,
    onSelectFolder: (String) -> Unit,
    onToggleFolder: (String) -> Unit,
    onOpenNote: (Int) -> Unit,
    onFolderPositioned: (String, Rect) -> Unit,
    parent: String? = null,
    depth: Int = 0,
) {
    val childFolders = immediateFolderChildren(folderPaths, parent)
    val childNotes = tabs.filter { it.folderPath == parent }.sortedBy { it.title.lowercase() }
    childFolders.forEach { folder ->
        val expanded = expandedFolders[folder] == true
        Surface(
            onClick = {
                onSelectFolder(folder)
                if (!expanded) onToggleFolder(folder)
            },
            color = if (selectedFolder == folder || dragTargetFolder == folder) {
                MaterialTheme.colorScheme.primaryContainer
            } else Color.Transparent,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 10 + 12).dp)
                .onGloballyPositioned { onFolderPositioned(folder, it.boundsInRoot()) },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onToggleFolder(folder) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                        if (expanded) "Collapse folder" else "Expand folder",
                        Modifier.size(17.dp),
                    )
                }
                Icon(Icons.Outlined.Folder, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    folder.substringAfterLast('/'),
                    Modifier.padding(start = 7.dp).weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (expanded) LibraryHierarchy(
            tabs = tabs,
            folderPaths = folderPaths,
            selectedFolder = selectedFolder,
            expandedFolders = expandedFolders,
            dragTargetFolder = dragTargetFolder,
            onSelectFolder = onSelectFolder,
            onToggleFolder = onToggleFolder,
            onOpenNote = onOpenNote,
            onFolderPositioned = onFolderPositioned,
            parent = folder,
            depth = depth + 1,
        )
    }
    childNotes.forEach { note ->
        Surface(
            onClick = { onOpenNote(note.id) },
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = (depth * 10 + 46).dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Description, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    note.title,
                    Modifier.padding(start = 8.dp).weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .965f else 1f, tween(120), label = "nav-press")
    val navBg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tween(220), label = "nav-bg",
    )
    Surface(
        onClick = onClick,
        color = navBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interaction,
    ) {
        Row(
            Modifier.then(if (compact) Modifier.size(48.dp) else Modifier.fillMaxWidth()).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
        ) {
            Icon(icon, null, Modifier.size(20.dp))
            if (!compact) {
                Spacer(Modifier.width(12.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun LibrarySortAndLayout(
    sortMenu: Boolean,
    onSortMenu: (Boolean) -> Unit,
    sort: LibrarySort,
    onSort: (LibrarySort) -> Unit,
    layout: LibraryLayout,
    onLayout: (LibraryLayout) -> Unit,
) {
    Box {
        IconButton(onClick = { onSortMenu(true) }) { Icon(Icons.Outlined.History, "Sort notebooks") }
        DropdownMenu(
            expanded = sortMenu,
            onDismissRequest = { onSortMenu(false) },
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface),
        ) {
            LibrarySort.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.name) },
                    onClick = { onSort(choice); onSortMenu(false) },
                    trailingIcon = { if (choice == sort) Icon(Icons.Outlined.Star, null, Modifier.size(15.dp)) },
                )
            }
        }
    }
    IconButton(onClick = { onLayout(if (layout == LibraryLayout.Grid) LibraryLayout.List else LibraryLayout.Grid) }) {
        Icon(if (layout == LibraryLayout.Grid) Icons.Outlined.ViewList else Icons.Outlined.GridView, "Change layout")
    }
}

@Composable
private fun FolderCard(
    folder: String,
    noteCount: Int,
    highlighted: Boolean,
    trashed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDragPosition: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var actionsSignal by remember(folder) { mutableIntStateOf(0) }
    var suppressNextOpen by remember(folder) { mutableStateOf(false) }
    var cardOrigin by remember(folder) { mutableStateOf(Offset.Zero) }
    var dragPointer by remember(folder) { mutableStateOf(Offset.Zero) }
    var dragTranslation by remember(folder) { mutableStateOf(Offset.Zero) }
    var movedAfterHold by remember(folder) { mutableStateOf(false) }
    var stationaryMenuJob by remember(folder) { mutableStateOf<Job?>(null) }
    DisposableEffect(folder) { onDispose { stationaryMenuJob?.cancel() } }
    Card(
        modifier = modifier
            .onGloballyPositioned { cardOrigin = it.positionInRoot() }
            .graphicsLayer { alpha = if (dragTranslation != Offset.Zero) .28f else 1f }
            .pointerInput(folder, trashed) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        dragPointer = start
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        stationaryMenuJob?.cancel()
                        stationaryMenuJob = scope.launch {
                            delay(260)
                            if (!movedAfterHold) {
                                suppressNextOpen = true
                                actionsSignal++
                            }
                        }
                    },
                    onDragEnd = {
                        stationaryMenuJob?.cancel()
                        if (movedAfterHold && !trashed) onDragEnd()
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        scope.launch { delay(300); suppressNextOpen = false }
                    },
                    onDragCancel = {
                        stationaryMenuJob?.cancel()
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        onDragCancel()
                        scope.launch { delay(300); suppressNextOpen = false }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        if (!trashed) {
                            dragTranslation += amount
                            if (dragTranslation.getDistance() > 10f) {
                                movedAfterHold = true
                                stationaryMenuJob?.cancel()
                                suppressNextOpen = true
                                onDragPosition(cardOrigin + dragPointer + dragTranslation)
                            }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(16.dp),
        border = if (highlighted) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Surface(
            onClick = {
                if (suppressNextOpen) suppressNextOpen = false else onClick()
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(.76f),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Folder, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(folder.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.notes_count, noteCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FolderActionsButton(
                folder = folder,
                trashed = trashed,
                externalOpenSignal = actionsSignal,
                onRename = onRename,
                onTrash = onTrash,
                onRestore = onRestore,
            )
        }
    }
}

@Composable
private fun FolderActionsButton(
    folder: String,
    trashed: Boolean,
    externalOpenSignal: Int,
    onRename: (String) -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
) {
    var menu by remember(folder) { mutableStateOf(false) }
    var rename by remember(folder) { mutableStateOf(false) }
    var confirmTrash by remember(folder) { mutableStateOf(false) }
    var editedName by remember(folder) { mutableStateOf(folder.substringAfterLast('/')) }
    LaunchedEffect(externalOpenSignal) {
        if (externalOpenSignal > 0) menu = true
    }
    Box {
        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.folder_actions)) }
        DropdownMenu(menu, { menu = false }) {
            if (trashed) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.restore)) },
                    leadingIcon = { Icon(Icons.Outlined.History, null) },
                    onClick = { menu = false; onRestore() },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename)) },
                    leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                    onClick = { menu = false; editedName = folder.substringAfterLast('/'); rename = true },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.move_to_trash)) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                    onClick = { menu = false; confirmTrash = true },
                )
            }
        }
    }
    if (rename) AlertDialog(
        onDismissRequest = { rename = false },
        title = { Text(stringResource(R.string.rename_folder)) },
        text = { OutlinedTextField(editedName, { editedName = it }, singleLine = true, label = { Text(stringResource(R.string.folder_name)) }) },
        dismissButton = { TextButton(onClick = { rename = false }) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(enabled = editedName.trim().trim('/').isNotEmpty(), onClick = {
                onRename(editedName)
                rename = false
            }) { Text(stringResource(R.string.rename)) }
        },
    )
    if (confirmTrash) AlertDialog(
        onDismissRequest = { confirmTrash = false },
        title = { Text(stringResource(R.string.move_folder_trash_title)) },
        text = { Text(stringResource(R.string.move_folder_trash_message, folder.substringAfterLast('/'))) },
        dismissButton = { TextButton(onClick = { confirmTrash = false }) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(onClick = { confirmTrash = false; onTrash() }) {
                Text(stringResource(R.string.move_to_trash), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun NotebookCard(
    tab: EditorTab,
    folders: List<String>,
    onClick: () -> Unit,
    onDuplicate: (EditorTab) -> Unit,
    onPermanentDelete: (EditorTab) -> Unit,
    onExportNotebook: (EditorTab) -> Unit,
    onDragPosition: (Offset) -> Unit = {},
    onDragEnd: () -> Boolean = { false },
    onDragCancel: () -> Unit = {},
) {
    val previewPage = tab.pages.firstOrNull()
    val scope = rememberCoroutineScope()
    val hoverInteraction = remember { MutableInteractionSource() }
    val hovered by hoverInteraction.collectIsHoveredAsState()
    val pressed by hoverInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        when { pressed -> .975f; hovered -> 1.025f; else -> 1f },
        tween(140),
        label = "notebook-interaction",
    )
    var actionsSignal by remember(tab.id) { mutableIntStateOf(0) }
    var cardOrigin by remember(tab.id) { mutableStateOf(Offset.Zero) }
    var dragPointer by remember(tab.id) { mutableStateOf(Offset.Zero) }
    var dragTranslation by remember(tab.id) { mutableStateOf(Offset.Zero) }
    var movedAfterHold by remember(tab.id) { mutableStateOf(false) }
    var stationaryMenuJob by remember(tab.id) { mutableStateOf<Job?>(null) }
    var suppressNextOpen by remember(tab.id) { mutableStateOf(false) }
    DisposableEffect(tab.id) { onDispose { stationaryMenuJob?.cancel() } }
    LaunchedEffect(tab.id, previewPage?.id, previewPage?.thumbnail) {
        if (previewPage != null && previewPage.thumbnail == null) {
            tab.requestThumbnail(previewPage.id)
        }
    }

    Card(
        onClick = {
            if (suppressNextOpen) suppressNextOpen = false else onClick()
        },
        interactionSource = hoverInteraction,
        modifier = Modifier
            .onGloballyPositioned { cardOrigin = it.positionInRoot() }
            .hoverable(hoverInteraction)
            .graphicsLayer {
                scaleX = cardScale; scaleY = cardScale
                alpha = if (dragTranslation != Offset.Zero) .28f else 1f
            }
            .pointerInput(tab.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        dragPointer = start
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        stationaryMenuJob?.cancel()
                        stationaryMenuJob = scope.launch {
                            delay(260)
                            if (!movedAfterHold) {
                                suppressNextOpen = true
                                actionsSignal++
                            }
                        }
                    },
                    onDragEnd = {
                        stationaryMenuJob?.cancel()
                        if (movedAfterHold) onDragEnd()
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        scope.launch { delay(300); suppressNextOpen = false }
                    },
                    onDragCancel = {
                        stationaryMenuJob?.cancel()
                        dragTranslation = Offset.Zero
                        movedAfterHold = false
                        onDragCancel()
                        scope.launch { delay(300); suppressNextOpen = false }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragTranslation += amount
                        if (dragTranslation.getDistance() > 10f) {
                            movedAfterHold = true
                            stationaryMenuJob?.cancel()
                            suppressNextOpen = true
                            onDragPosition(cardOrigin + dragPointer + dragTranslation)
                        }
                    },
                )
            },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(.76f)
                    .drawBehind {
                        if (hovered) drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f)),
                            ),
                            cornerRadius = CornerRadius(0f),
                        )
                    },
                color = Color(tab.coverArgb),
            ) {
                Box(
                    Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewPage != null) {
                        PageThumbnailPreview(
                            previewPage,
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(previewPage.previewAspectRatio())
                                .clip(RoundedCornerShape(5.dp))
                                .shadow(5.dp, RoundedCornerShape(5.dp)),
                        )
                    } else {
                        Icon(Icons.Outlined.Edit, null, Modifier.size(42.dp), tint = Color.White.copy(alpha = .9f))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tab.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (tab.favorite) {
                            val starScale = remember(tab.id) { Animatable(1f) }
                            LaunchedEffect(tab.favorite) { starScale.animateTo(1.35f, tween(120)); starScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f)) }
                            Icon(Icons.Outlined.Star, null, Modifier.padding(start = 4.dp).size(15.dp).graphicsLayer { scaleX = starScale.value; scaleY = starScale.value }, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("${tab.pages.size} pages${tab.folderPath?.let { " · $it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                NotebookActionsButton(tab, folders, onDuplicate, onPermanentDelete, onExportNotebook, actionsSignal)
            }
        }
    }
}

@Composable
private fun NotebookListRow(
    tab: EditorTab,
    folders: List<String>,
    onClick: () -> Unit,
    onDuplicate: (EditorTab) -> Unit,
    onPermanentDelete: (EditorTab) -> Unit,
    onExportNotebook: (EditorTab) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        when { pressed -> .975f; hovered -> 1.012f; else -> 1f },
        tween(130),
        label = "notebook-row-interaction",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        interactionSource = interaction,
        modifier = Modifier.hoverable(interaction).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(Color(tab.coverArgb)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Edit, null, tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(tab.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tab.pages.size} pages${tab.folderPath?.let { " · $it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (tab.favorite) Icon(Icons.Outlined.Star, "Favorite", tint = MaterialTheme.colorScheme.primary)
            NotebookActionsButton(tab, folders, onDuplicate, onPermanentDelete, onExportNotebook)
        }
    }
}

@Composable
private fun NotebookActionsButton(
    tab: EditorTab,
    folders: List<String>,
    onDuplicate: (EditorTab) -> Unit,
    onPermanentDelete: (EditorTab) -> Unit,
    onExportNotebook: (EditorTab) -> Unit,
    externalOpenSignal: Int = 0,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var editNotebook by remember { mutableStateOf(false) }
    var confirmPermanentDelete by remember { mutableStateOf(false) }
    LaunchedEffect(externalOpenSignal) {
        if (externalOpenSignal > 0) menu = true
    }
    Box {
        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Notebook actions") }
        DropdownMenu(menu, { menu = false }) {
            if (tab.trashedAtEpochMillis != null) {
                DropdownMenuItem(text = { Text("Restore") }, onClick = { menu = false; tab.restoreFromTrash() })
                DropdownMenuItem(text = { Text("Delete permanently") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; confirmPermanentDelete = true })
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open_new_window)) },
                    leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                    onClick = {
                        menu = false
                        context.startActivity(Intent(context, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_NOTEBOOK_ID, tab.persistentId)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        })
                    },
                )
                DropdownMenuItem(text = { Text(stringResource(if (tab.favorite) R.string.remove_favorite else R.string.add_favorite)) }, leadingIcon = { Icon(Icons.Outlined.Star, null) }, onClick = { menu = false; tab.updateFavorite(!tab.favorite) })
                DropdownMenuItem(
                    text = { Text("Edit notebook") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { menu = false; editNotebook = true },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; onDuplicate(tab) })
                DropdownMenuItem(text = { Text(stringResource(R.string.export_mynote)) }, leadingIcon = { Icon(Icons.Outlined.FileOpen, null) }, onClick = { menu = false; onExportNotebook(tab) })
                DropdownMenuItem(text = { Text(stringResource(R.string.move_out_folder)) }, enabled = tab.folderPath != null, leadingIcon = { Icon(Icons.Outlined.Folder, null) }, onClick = { menu = false; tab.moveToFolder(null) })
                folders.forEach { folder ->
                    DropdownMenuItem(text = { Text("Move to $folder") }, onClick = { menu = false; tab.moveToFolder(folder) })
                }
                DropdownMenuItem(text = { Text(stringResource(R.string.move_to_trash)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { menu = false; tab.moveToTrash() })
            }
        }
    }
    if (editNotebook) {
        val firstPage = tab.pages.first()
        NewNotebookDialog(
            initial = NotebookCreationSpec(
                title = tab.title,
                size = firstPage.size,
                template = firstPage.template,
                backgroundArgb = firstPage.backgroundArgb,
                templateLineArgb = firstPage.templateLineArgb,
                coverArgb = tab.coverArgb,
            ),
            onCreate = { spec -> tab.editNotebook(spec); editNotebook = false },
            onDismiss = { editNotebook = false },
        )
    }
    if (confirmPermanentDelete) AlertDialog(
        onDismissRequest = { confirmPermanentDelete = false },
        title = { Text("Delete notebook permanently?") },
        text = { Text("This notebook and its embedded files will be deleted. This cannot be undone.") },
        dismissButton = { TextButton(onClick = { confirmPermanentDelete = false }) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = { confirmPermanentDelete = false; onPermanentDelete(tab) }) {
                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun EditorScreen(
    tabs: List<EditorTab>, selectedTabId: Int, onSelectTab: (Int) -> Unit,
    allNotebooks: List<EditorTab>,
    onMoveTab: (Int, Int) -> Unit, onCloseTab: (Int) -> Unit,
    tab: EditorTab, mode: EditorMode, onMode: (EditorMode) -> Unit,
    dockEdge: DockEdge, onDockEdge: (DockEdge) -> Unit,
    onHome: () -> Unit, onImage: (PageId, PagePoint) -> Unit, onImportPdf: () -> Unit,
    onExportPdf: () -> Unit, onExportImage: () -> Unit, onExportNotebook: () -> Unit,
    onSettings: () -> Unit,
    actionMappings: List<ActionMapping>, actionRecognizer: ActionMapRecognizer,
    showDiagnostics: Boolean,
    radialMenuRequestGeneration: Int,
    appSettings: AppSettings,
    onAppSettingsChanged: (AppSettings) -> Unit,
    onStylusButtonPressed: (Int) -> Unit,
) {
    var sidebarVisible by remember { mutableStateOf(true) }
    var zenMode by rememberSaveable { mutableStateOf(false) }
    var strokeSizePreview by remember { mutableStateOf<StrokeSizePreview?>(null) }
    var showPdfNavigation by remember { mutableStateOf(false) }
    fun executeAction(action: MappedAction) {
        when (action) {
            MappedAction.Undo -> tab.undo()
            MappedAction.Redo -> tab.redo()
            MappedAction.Pen -> { tab.session.selectTool(InkTool.Pen); onMode(EditorMode.Ink) }
            MappedAction.Pencil -> { tab.session.selectTool(InkTool.Pencil); onMode(EditorMode.Ink) }
            MappedAction.Highlighter -> { tab.session.selectTool(InkTool.Highlighter); onMode(EditorMode.Ink) }
            MappedAction.Eraser -> { tab.session.selectTool(InkTool.Eraser); onMode(EditorMode.Ink) }
            MappedAction.Lasso -> onMode(EditorMode.Lasso)
            MappedAction.Shapes -> onMode(EditorMode.Shape)
            MappedAction.Text -> onMode(EditorMode.Text)
            MappedAction.AddPage -> tab.addPage()
            MappedAction.PreviousPage -> tab.navigateToPage((tab.selectedPageIndex - 1).coerceAtLeast(0))
            MappedAction.NextPage -> tab.navigateToPage((tab.selectedPageIndex + 1).coerceAtMost(tab.pages.lastIndex))
            MappedAction.Image -> onMode(EditorMode.Image)
            MappedAction.Bookmark -> tab.toggleBookmark()
            MappedAction.PageOverview -> sidebarVisible = !sidebarVisible
            MappedAction.ResetZoom -> tab.resetViewport()
            MappedAction.WidthPreset1 -> appSettings.widthPresets(tab.session.selectedTool).getOrNull(0)?.let { tab.session.updateToolWidth(tab.session.selectedTool, it) }
            MappedAction.WidthPreset2 -> appSettings.widthPresets(tab.session.selectedTool).getOrNull(1)?.let { tab.session.updateToolWidth(tab.session.selectedTool, it) }
            MappedAction.WidthPreset3 -> appSettings.widthPresets(tab.session.selectedTool).getOrNull(2)?.let { tab.session.updateToolWidth(tab.session.selectedTool, it) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !zenMode,
                enter = expandVertically(tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(260)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Column {
                    DocumentTabs(tabs, selectedTabId, onSelectTab, onMoveTab, onCloseTab)
                    EditorCommandBar(
                        tab, onHome, { sidebarVisible = !sidebarVisible }, onImportPdf, onExportPdf,
                        onExportImage, onExportNotebook,
                        { showPdfNavigation = true }, onSettings,
                        zenMode = zenMode, onToggleZenMode = { zenMode = !zenMode },
                    )
                    if (dockEdge == DockEdge.Top) ToolPalette(
                        false, tab.session, mode, onMode, onSettings,
                        onStrokeSizePreview = { strokeSizePreview = it },
                        appSettings = appSettings,
                        onAppSettingsChanged = onAppSettingsChanged,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .5f))
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (dockEdge == DockEdge.Left) ToolPalette(
                    true, tab.session, mode, onMode, onSettings,
                    onStrokeSizePreview = { strokeSizePreview = it },
                    appSettings = appSettings,
                    onAppSettingsChanged = onAppSettingsChanged,
                )
                AnimatedVisibility(
                    visible = sidebarVisible && !zenMode,
                    enter = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(280)),
                    exit = slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(200)),
                ) { PageThumbnailRail(tab, allNotebooks, appSettings, onAppSettingsChanged) }
                ContinuousPageCanvas(
                    tab, mode, actionMappings, actionRecognizer, ::executeAction,
                    strokeSizePreview, appSettings.actionActivationMode, appSettings.drawAndHoldEnabled,
                    appSettings.customFonts, onStylusButtonPressed, onImage,
                    showDiagnostics, appSettings, radialMenuRequestGeneration,
                    Modifier.weight(1f),
                )
                if (showDiagnostics) DiagnosticsPanel(tab.session)
                if (dockEdge == DockEdge.Right) ToolPalette(
                    true, tab.session, mode, onMode, onSettings,
                    onStrokeSizePreview = { strokeSizePreview = it },
                    appSettings = appSettings,
                    onAppSettingsChanged = onAppSettingsChanged,
                )
            }
        }
        ZenModeFloatingToggle(
            zenMode = zenMode,
            onToggle = { zenMode = false },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        )
    }
    if (showPdfNavigation) PdfNavigationDialog(tab, onDismiss = { showPdfNavigation = false })
}

@Composable
private fun DocumentTabs(
    tabs: List<EditorTab>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tabWidth = 180.dp
    val tabWidthPx = with(density) { tabWidth.toPx() }
    val scrollState = rememberScrollState()
    var draggedTabId by remember { mutableStateOf<Int?>(null) }
    var draggedOffsetX by remember { mutableStateOf(0f) }
    val currentTabs by rememberUpdatedState(tabs)
    BoxWithConstraints(Modifier.fillMaxWidth().height(34.dp).background(MaterialTheme.colorScheme.background)) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        Row(
            Modifier.fillMaxSize().horizontalScroll(scrollState),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEach { tab ->
                key(tab.id) {
                    val isDragged = draggedTabId == tab.id
                    Surface(
                    onClick = { onSelect(tab.id) },
                    color = if (tab.id == selectedId) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    shadowElevation = if (isDragged) 8.dp else 0.dp,
                    modifier = Modifier
                        .height(32.dp)
                        .width(tabWidth)
                        .zIndex(if (isDragged) 10f else 0f)
                        .graphicsLayer { translationX = if (isDragged) draggedOffsetX else 0f }
                        .pointerInput(tab.id, tabs.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedTabId = tab.id
                                    draggedOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggedTabId = null
                                    draggedOffsetX = 0f
                                },
                                onDragEnd = {
                                    draggedTabId = null
                                    draggedOffsetX = 0f
                                },
                            ) { change, amount ->
                                change.consume()
                                val activeTabs = currentTabs
                                val currentIndex = activeTabs.indexOfFirst { it.id == tab.id }
                                if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                val slotScreenX = currentIndex * tabWidthPx - scrollState.value
                                draggedOffsetX = (draggedOffsetX + amount.x).coerceIn(
                                    -slotScreenX,
                                    (viewportWidthPx - tabWidthPx - slotScreenX).coerceAtLeast(-slotScreenX),
                                )
                                while (draggedOffsetX > tabWidthPx / 2f && currentIndex < activeTabs.lastIndex) {
                                    onMove(tab.id, 1)
                                    draggedOffsetX -= tabWidthPx
                                    break
                                }
                                while (draggedOffsetX < -tabWidthPx / 2f && currentIndex > 0) {
                                    onMove(tab.id, -1)
                                    draggedOffsetX += tabWidthPx
                                    break
                                }
                            }
                        },
                    ) {
                        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.title, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { onClose(tab.id) }, Modifier.size(30.dp)) {
                                Icon(Icons.Outlined.Close, "Close tab", Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorCommandBar(
    tab: EditorTab, onHome: () -> Unit, onSidebar: () -> Unit,
    onImportPdf: () -> Unit, onExportPdf: () -> Unit, onExportImage: () -> Unit,
    onExportNotebook: () -> Unit, onPdfNavigation: () -> Unit, onSettings: () -> Unit,
    zenMode: Boolean = false, onToggleZenMode: () -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(42.dp).zIndex(20f).background(MaterialTheme.colorScheme.surface).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onHome, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.ArrowBack, "Back to library") }
        IconButton(onClick = onSidebar, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.ViewSidebar, "Toggle page thumbnails") }
        Text(tab.title, Modifier.padding(horizontal = 8.dp).weight(1f), style = MaterialTheme.typography.titleSmall)
        SaveStatusIcon(tab.session.saveStatus)
        val undoAlpha by animateFloatAsState(if (tab.canUndo) 1f else 0.3f, tween(200), label = "undo-alpha")
        val redoAlpha by animateFloatAsState(if (tab.canRedo) 1f else 0.3f, tween(200), label = "redo-alpha")
        IconButton(onClick = tab::undo, enabled = tab.canUndo, modifier = Modifier.size(40.dp).graphicsLayer { alpha = undoAlpha }) { Icon(Icons.Outlined.Undo, "Undo") }
        IconButton(onClick = tab::redo, enabled = tab.canRedo, modifier = Modifier.size(40.dp).graphicsLayer { alpha = redoAlpha }) { Icon(Icons.Outlined.Redo, "Redo") }
        IconButton(onClick = onToggleZenMode, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Fullscreen, "Zen full-screen mode") }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.MoreVert, "Document menu") }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                MenuItem(Icons.Outlined.Fullscreen, "Zen full-screen mode") { menu = false; onToggleZenMode() }
                MenuItem(Icons.Outlined.PictureAsPdf, "Import PDF") { menu = false; onImportPdf() }
                MenuItem(Icons.Outlined.PictureAsPdf, "Export PDF") { menu = false; onExportPdf() }
                MenuItem(Icons.Outlined.Image, "Export current page image") { menu = false; onExportImage() }
                MenuItem(Icons.Outlined.FileOpen, "Export editable notebook") { menu = false; onExportNotebook() }
                MenuItem(Icons.Outlined.Search, "Search PDF and bookmarks") { menu = false; onPdfNavigation() }
                HorizontalDivider()
                MenuItem(Icons.Outlined.Settings, "Editor settings") { menu = false; onSettings() }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
private fun ToolPalette(
    vertical: Boolean, session: EditorSession, mode: EditorMode,
    onMode: (EditorMode) -> Unit, onSettings: () -> Unit,
    onStrokeSizePreview: (StrokeSizePreview?) -> Unit,
    appSettings: AppSettings,
    onAppSettingsChanged: (AppSettings) -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var editingPresetColor by remember { mutableStateOf<Pair<InkTool, Int>?>(null) }
    val dragRangePx = with(LocalDensity.current) { 120.dp.toPx() }
    fun previewFor(tool: InkTool): StrokeSizePreview {
        val style = session.currentStyle(tool)
        return StrokeSizePreview(tool, style.maximumWidth)
    }
    fun activate(tool: InkTool) {
        session.selectTool(tool)
        onMode(EditorMode.Ink)
    }
    val modifier = if (vertical) Modifier.width(58.dp).fillMaxHeight() else Modifier.fillMaxWidth().height(58.dp)
    Surface(modifier.zIndex(20f), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        val content: @Composable () -> Unit = {
            ToolIcon(
                Icons.Outlined.Create, stringResource(R.string.pen), mode == EditorMode.Ink && session.selectedTool == InkTool.Pen,
                onClick = { activate(InkTool.Pen) },
            )
            ToolIcon(
                Icons.Outlined.Edit, stringResource(R.string.pencil), mode == EditorMode.Ink && session.selectedTool == InkTool.Pencil,
                onClick = { activate(InkTool.Pencil) },
            )
            ToolIcon(
                Icons.Outlined.Highlight, stringResource(R.string.highlighter), mode == EditorMode.Ink && session.selectedTool == InkTool.Highlighter,
                onClick = { activate(InkTool.Highlighter) },
            )
            ToolIcon(
                Icons.Outlined.AutoFixHigh, stringResource(R.string.eraser), mode == EditorMode.Ink && session.selectedTool == InkTool.Eraser,
                onClick = { activate(InkTool.Eraser) },
            )
            ToolIcon(Icons.Outlined.Gesture, stringResource(R.string.lasso), mode == EditorMode.Lasso) { onMode(EditorMode.Lasso) }
            ToolIcon(Icons.Outlined.Image, stringResource(R.string.insert_image), mode == EditorMode.Image) { onMode(EditorMode.Image) }
            ToolIcon(Icons.Outlined.TextFields, stringResource(R.string.text_tool), mode == EditorMode.Text) { onMode(EditorMode.Text) }
            ToolIcon(Icons.Outlined.ShapeLine, stringResource(R.string.shapes), mode == EditorMode.Shape) { onMode(EditorMode.Shape) }
            ToolIcon(Icons.Outlined.Mic, stringResource(R.string.audio_note), mode == EditorMode.Audio) { onMode(EditorMode.Audio) }
            if (!vertical) Spacer(Modifier.width(12.dp))
            if (session.selectedTool != InkTool.Eraser) {
                val activeTool = session.selectedTool
                appSettings.colorPresets(activeTool).forEachIndexed { index, argb ->
                    Box {
                        ColorSwatch(
                            color = Color(argb),
                            selected = session.currentStyle().argb == argb,
                            onClick = { session.setCurrentColor(argb) },
                            onLongClick = { editingPresetColor = activeTool to index },
                        )
                        ToolColorDropdown(
                            expanded = editingPresetColor == (activeTool to index),
                            initialArgb = argb,
                            onApply = { color ->
                                val updated = appSettings.colorPresets(activeTool).toMutableList().apply {
                                    this[index] = color
                                }
                                onAppSettingsChanged(appSettings.withColorPresets(activeTool, updated))
                                session.setCurrentColor(color)
                                editingPresetColor = null
                            },
                            onDismiss = { editingPresetColor = null },
                        )
                    }
                }
                Box {
                    ToolIcon(Icons.Outlined.Palette, stringResource(R.string.custom_color), showColorPicker) { showColorPicker = true }
                    ToolColorDropdown(
                        expanded = showColorPicker,
                        initialArgb = session.currentStyle().argb,
                        onApply = { session.setCurrentColor(it); showColorPicker = false },
                        onDismiss = { showColorPicker = false },
                    )
                }
                appSettings.widthPresets(session.selectedTool).forEachIndexed { index, width ->
                    StrokeWidthPreset(
                        widthMicrons = width.coerceAtMost(session.maximumToolWidthMicrons),
                        maximumWidthMicrons = session.maximumToolWidthMicrons,
                        color = Color(session.currentStyle().argb).copy(alpha = session.currentStyle().opacity),
                        selected = kotlin.math.abs(
                            session.toolWidthMicrons(session.selectedTool) - width.coerceAtMost(session.maximumToolWidthMicrons),
                        ) < 1f,
                        onClick = { session.updateToolWidth(session.selectedTool, width) },
                        onDragStart = {
                            session.updateToolWidth(session.selectedTool, width)
                            onStrokeSizePreview(previewFor(session.selectedTool))
                        },
                        onDrag = { delta ->
                            val updated = (session.toolWidthMicrons(session.selectedTool) -
                                delta / dragRangePx * session.maximumToolWidthMicrons)
                                .coerceIn(100f, session.maximumToolWidthMicrons)
                            session.updateToolWidth(session.selectedTool, updated)
                            val presets = appSettings.widthPresets(session.selectedTool).toMutableList().apply {
                                this[index] = updated
                            }
                            onAppSettingsChanged(appSettings.withWidthPresets(session.selectedTool, presets))
                            onStrokeSizePreview(previewFor(session.selectedTool))
                        },
                        onDragEnd = { onStrokeSizePreview(null) },
                    )
                }
            }
            ToolIcon(Icons.Outlined.Settings, stringResource(R.string.tool_settings), false, onClick = onSettings)
        }
        if (vertical) Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
        else Row(
            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    }
}

@Composable
private fun ToolColorDropdown(
    expanded: Boolean,
    initialArgb: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(320.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface),
    ) {
        CircularColorPickerContent(initialArgb, onApply, onDismiss, wheelSize = 210.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val tooltipScope = rememberCoroutineScope()
    val hoverModifier = Modifier.pointerInput(description, tooltipState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val isStylus = event.changes.any {
                    it.type == PointerType.Stylus || it.type == PointerType.Eraser
                }
                if (isStylus) {
                    when (event.type) {
                        PointerEventType.Enter -> tooltipScope.launch { tooltipState.show() }
                        PointerEventType.Exit -> tooltipState.dismiss()
                    }
                }
            }
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description) } },
        state = tooltipState,
    ) {
        val backgroundColor by animateColorAsState(
            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            tween(200),
            label = "tool-bg",
        )
        val toolScale by animateFloatAsState(
            if (selected) 1f else 0.95f, tween(160), label = "tool-scale",
        )
        Surface(
            onClick = onClick,
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(46.dp).graphicsLayer { scaleX = toolScale; scaleY = toolScale }.then(hoverModifier),
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, description, Modifier.size(23.dp)) } }
    }
}

@Composable
private fun StrokeWidthPreset(
    widthMicrons: Float,
    maximumWidthMicrons: Float,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val diameter = (8f + 16f * widthMicrons / maximumWidthMicrons).dp
    val outlineColor = MaterialTheme.colorScheme.primary
    val presetBg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tween(200),
        label = "preset-bg",
    )
    val scale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "preset-scale",
    )
    Surface(
        onClick = onClick,
        color = presetBg,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(maximumWidthMicrons) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragCancel = onDragEnd,
                    onDragEnd = onDragEnd,
                ) { change, amount ->
                    change.consume()
                    onDrag(amount)
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                drawCircle(color)
                if (selected) drawCircle(outlineColor, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val size by animateDpAsState(
        if (selected) 32.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "color-swatch-size",
    )
    val borderWidth by animateDpAsState(
        if (selected) 3.dp else 1.dp,
        animationSpec = tween(150),
        label = "color-swatch-border",
    )
    val scale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "color-swatch-scale",
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = color,
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        border = androidx.compose.foundation.BorderStroke(
            borderWidth,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        ),
    ) {}
}

@Composable
private fun PageThumbnailRail(
    tab: EditorTab,
    openTabs: List<EditorTab>,
    appSettings: AppSettings,
    onAppSettingsChanged: (AppSettings) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var bookmarksOnly by remember(tab.id) { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var jumpPage by remember { mutableStateOf((tab.selectedPageIndex + 1).toString()) }
    var draggedPageId by remember { mutableStateOf<PageId?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var dragPointerY by remember { mutableStateOf(0f) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragTargetBeforePageId by remember { mutableStateOf<PageId?>(null) }
    var dragTargetAfterPageId by remember { mutableStateOf<PageId?>(null) }
    val thumbnailBounds = remember(tab.id, bookmarksOnly) { mutableStateMapOf<PageId, Rect>() }
    var pendingInsertAfter by remember { mutableStateOf<Boolean?>(null) }
    val visiblePages = tab.pages.mapIndexedNotNull { index, page ->
        if (!bookmarksOnly || page.bookmarked) index to page else null
    }

    fun clearPageDrag() {
        draggedPageId = null
        dragOffset = 0f
        dragPointerY = 0f
        dragTargetIndex = -1
        dragTargetBeforePageId = null
        dragTargetAfterPageId = null
    }

    fun updatePageDragTarget(pageId: PageId, pointerY: Float) {
        val remainingVisible = visiblePages
            .filterNot { it.second.id == pageId }
            .mapNotNull { entry -> thumbnailBounds[entry.second.id]?.let { bounds -> entry to bounds } }
            .sortedBy { it.second.top }
        if (remainingVisible.isEmpty()) {
            dragTargetIndex = tab.pages.indexOfFirst { it.id == pageId }
            dragTargetBeforePageId = null
            dragTargetAfterPageId = null
            return
        }
        val gapCandidates = buildList {
            add(0 to remainingVisible.first().second.top)
            for (gap in 1 until remainingVisible.size) {
                val previous = remainingVisible[gap - 1].second
                val next = remainingVisible[gap].second
                add(gap to (previous.bottom + next.top) / 2f)
            }
            add(remainingVisible.size to remainingVisible.last().second.bottom)
        }
        val gap = gapCandidates.minBy { (_, y) -> kotlin.math.abs(pointerY - y) }.first
        val remainingAll = tab.pages.filterNot { it.id == pageId }
        val before = remainingVisible.getOrNull(gap)?.first?.second
        val after = if (gap == remainingVisible.size) remainingVisible.last().first.second else null
        dragTargetIndex = when {
            before != null -> remainingAll.indexOfFirst { it.id == before.id }
            after != null -> remainingAll.indexOfFirst { it.id == after.id } + 1
            else -> tab.pages.indexOfFirst { it.id == pageId }
        }.coerceIn(0, remainingAll.size)
        dragTargetBeforePageId = before?.id
        dragTargetAfterPageId = after?.id
    }

    Column(Modifier.width(94.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(visiblePages, key = { _, entry -> entry.second.id.value }) { _, entry ->
                val (index, page) = entry
                LaunchedEffect(page.id) { tab.requestThumbnail(page.id) }
                DisposableEffect(page.id) { onDispose { thumbnailBounds.remove(page.id) } }
                val isDragged = draggedPageId == page.id
                Box(
                    Modifier.fillMaxWidth()
                        .aspectRatio(page.previewAspectRatio())
                        .animateItem()
                        .zIndex(if (isDragged) 20f else 0f)
                        .onGloballyPositioned { thumbnailBounds[page.id] = it.boundsInRoot() },
                ) {
                    if (draggedPageId != null && dragTargetBeforePageId == page.id) PageInsertionMarker(
                        Modifier.align(Alignment.TopCenter).offset(y = (-7).dp).zIndex(30f),
                    )
                    if (draggedPageId != null && dragTargetAfterPageId == page.id) PageInsertionMarker(
                        Modifier.align(Alignment.BottomCenter).offset(y = 7.dp).zIndex(30f),
                    )
                    if (isDragged) Surface(
                        Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(7.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .45f)),
                    ) {}
                    Surface(
                        onClick = { tab.navigateToPage(index) },
                        shape = RoundedCornerShape(7.dp),
                        color = if (index == tab.selectedPageIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = if (isDragged) 10.dp else 0.dp,
                        modifier = Modifier.fillMaxSize()
                            .zIndex(if (isDragged) 10f else 0f)
                            .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                            .pointerInput(page.id, tab.pages.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { start ->
                                    draggedPageId = page.id
                                    dragOffset = 0f
                                    dragPointerY = (thumbnailBounds[page.id]?.top ?: 0f) + start.y
                                    updatePageDragTarget(page.id, dragPointerY)
                                },
                                onDragCancel = ::clearPageDrag,
                                onDragEnd = {
                                    if (dragTargetIndex >= 0) tab.movePageTo(page.id, dragTargetIndex)
                                    clearPageDrag()
                                },
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                dragPointerY += amount.y
                                updatePageDragTarget(page.id, dragPointerY)
                            }
                        },
                    ) {
                        Box(Modifier.padding(5.dp)) {
                            PageThumbnailPreview(page, Modifier.fillMaxSize().clip(RoundedCornerShape(3.dp)))
                            Surface(
                                color = Color(0xDDF8F7F2),
                                shape = RoundedCornerShape(bottomEnd = 4.dp),
                            ) {
                                Text(
                                    if (page.bookmarked) "★ ${index + 1}" else "${index + 1}",
                                    Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    color = Color(0xFF20242B),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { bookmarksOnly = !bookmarksOnly }) {
                Icon(
                    Icons.Outlined.Bookmark,
                    if (bookmarksOnly) "Show all pages" else "Show bookmarked pages",
                    tint = if (bookmarksOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, "Page actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MenuItem(Icons.Outlined.Add, "Insert page before") {
                    menu = false
                    if (appSettings.askBeforeInsertingPage) pendingInsertAfter = false else tab.addPage(after = false)
                }
                MenuItem(Icons.Outlined.Add, "Insert page after") {
                    menu = false
                    if (appSettings.askBeforeInsertingPage) pendingInsertAfter = true else tab.addPage(after = true)
                }
                MenuItem(Icons.Outlined.NoteAdd, "Duplicate page") { menu = false; tab.duplicatePage() }
                MenuItem(Icons.Outlined.Bookmark, "Bookmark page") { menu = false; tab.toggleBookmark() }
                MenuItem(Icons.Outlined.GridView, "Change template") { menu = false; tab.cycleTemplate() }
                MenuItem(Icons.Outlined.DriveFileRenameOutline, "Rotate page") { menu = false; tab.rotatePage() }
                MenuItem(Icons.Outlined.Delete, "Delete page") { menu = false; tab.deletePage() }
                MenuItem(Icons.Outlined.Search, "Jump to page") { menu = false; jumpPage = (tab.selectedPageIndex + 1).toString(); showJump = true }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Undo page change") },
                    leadingIcon = { Icon(Icons.Outlined.Undo, null) },
                    enabled = tab.canUndoPageEdit,
                    onClick = { menu = false; tab.undo() },
                )
                DropdownMenuItem(
                    text = { Text("Redo page change") },
                    leadingIcon = { Icon(Icons.Outlined.Redo, null) },
                    enabled = tab.canRedoPageEdit,
                    onClick = { menu = false; tab.redo() },
                )
                DropdownMenuItem(text = { Text("Change paper color") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = { menu = false; tab.cyclePageBackground() })
                DropdownMenuItem(text = { Text("Change template line color") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = { menu = false; tab.cycleTemplateLineColor() })
                openTabs.filterNot { it === tab }.forEach { target ->
                    DropdownMenuItem(
                        text = { Text("Copy page to ${target.title}") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                        onClick = { menu = false; tab.copyCurrentPageTo(target, move = false) },
                    )
                    DropdownMenuItem(
                        text = { Text("Move page to ${target.title}") },
                        enabled = tab.pages.size > 1,
                        leadingIcon = { Icon(Icons.Outlined.NavigateNext, null) },
                        onClick = { menu = false; tab.copyCurrentPageTo(target, move = true) },
                    )
                }
            }
            }
        }
    }
    pendingInsertAfter?.let { after ->
        InsertPageDialog(
            currentPage = tab.pages[tab.selectedPageIndex],
            onInsert = { spec, doNotAskAgain ->
                tab.addPage(after, spec)
                if (doNotAskAgain) onAppSettingsChanged(appSettings.copy(askBeforeInsertingPage = false))
                pendingInsertAfter = null
            },
            onDismiss = { pendingInsertAfter = null },
        )
    }
    if (showJump) AlertDialog(
        onDismissRequest = { showJump = false },
        title = { Text("Jump to page") },
        text = { OutlinedTextField(jumpPage, { jumpPage = it }, label = { Text("Page number") }, singleLine = true) },
        dismissButton = { TextButton(onClick = { showJump = false }) { Text("Cancel") } },
        confirmButton = {
            val page = jumpPage.toIntOrNull()?.minus(1)
            TextButton(enabled = page != null && page in tab.pages.indices, onClick = { tab.navigateToPage(page!!); showJump = false }) { Text("Go") }
        },
    )
}

private fun renderInkPageThumbnail(entry: PageEntry, targetWidth: Int = 240): Bitmap? = runCatching {
    val aspectRatio = entry.size.heightMicrons / entry.size.widthMicrons
    val targetHeight = (targetWidth * aspectRatio).toInt().coerceIn(120, 600)
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 1. Draw page background
    canvas.drawColor(entry.backgroundArgb)

    val scaleX = targetWidth.toFloat() / entry.size.widthMicrons
    val scaleY = targetHeight.toFloat() / entry.size.heightMicrons
    val strokeScale = (scaleX + scaleY) / 2f

    // 2. Draw template lines if any
    val templateColor = entry.templateLineArgb
    val linePaint = android.graphics.Paint().apply {
        color = templateColor
        strokeWidth = 1f
        style = android.graphics.Paint.Style.STROKE
        isAntiAlias = true
    }
    val spacing = if (entry.template == PageTemplate.Ruled) 9_000f else 8_000f
    when (entry.template) {
        PageTemplate.Blank -> Unit
        PageTemplate.Ruled -> {
            var y = spacing
            while (y < entry.size.heightMicrons) {
                canvas.drawLine(0f, y * scaleY, targetWidth.toFloat(), y * scaleY, linePaint)
                y += spacing
            }
        }
        PageTemplate.Grid -> {
            var y = spacing
            while (y < entry.size.heightMicrons) {
                canvas.drawLine(0f, y * scaleY, targetWidth.toFloat(), y * scaleY, linePaint)
                y += spacing
            }
            var x = spacing
            while (x < entry.size.widthMicrons) {
                canvas.drawLine(x * scaleX, 0f, x * scaleX, targetHeight.toFloat(), linePaint)
                x += spacing
            }
        }
        PageTemplate.Dotted -> {
            val dotPaint = android.graphics.Paint().apply {
                color = templateColor
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            var y = spacing
            while (y < entry.size.heightMicrons) {
                var x = spacing
                while (x < entry.size.widthMicrons) {
                    canvas.drawCircle(x * scaleX, y * scaleY, 0.7f, dotPaint)
                    x += spacing
                }
                y += spacing
            }
        }
        PageTemplate.Cornell -> {
            var y = 36_000f
            while (y < entry.size.heightMicrons) {
                canvas.drawLine(0f, y * scaleY, targetWidth.toFloat(), y * scaleY, linePaint)
                y += 9_000f
            }
            canvas.drawLine(45_000f * scaleX, 0f, 45_000f * scaleX, targetHeight.toFloat(), linePaint)
            canvas.drawLine(0f, 27_000f * scaleY, targetWidth.toFloat(), 27_000f * scaleY, linePaint)
        }
        PageTemplate.MusicStaff -> {
            var top = 18_000f
            while (top < entry.size.heightMicrons) {
                repeat(5) { line ->
                    val y = (top + line * 2_500f) * scaleY
                    canvas.drawLine(0f, y, targetWidth.toFloat(), y, linePaint)
                }
                top += 24_000f
            }
        }
    }

    // 3. Draw SceneObjects (Images, Rectangles, TextBoxes)
    entry.objects.sortedBy(SceneObject::zIndex).forEach { item ->
        when (item) {
            is SceneObject.Image -> {
                val left = item.x * scaleX
                val top = item.y * scaleY
                val right = (item.x + item.width) * scaleX
                val bottom = (item.y + item.height) * scaleY
                canvas.drawBitmap(item.bitmap, null, android.graphics.RectF(left, top, right, bottom), null)
            }
            is SceneObject.Rectangle -> {
                val rectPaint = android.graphics.Paint().apply {
                    color = item.strokeArgb
                    strokeWidth = (item.strokeWidth * strokeScale).coerceAtLeast(1f)
                    style = android.graphics.Paint.Style.STROKE
                    isAntiAlias = true
                }
                val left = item.x * scaleX
                val top = item.y * scaleY
                val right = (item.x + item.width) * scaleX
                val bottom = (item.y + item.height) * scaleY
                canvas.drawRect(left, top, right, bottom, rectPaint)
            }
            is SceneObject.TextBox -> {
                val textPaint = android.graphics.Paint().apply {
                    color = item.argb
                    textSize = (item.size * scaleY).coerceAtLeast(8f)
                    isAntiAlias = true
                }
                canvas.drawText(item.text.take(40), item.x * scaleX, (item.y + item.size) * scaleY, textPaint)
            }
            is SceneObject.Audio -> Unit
        }
    }

    // 4. Draw ink strokes
    val strokePaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    val fillPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = true
    }

    val pageState = entry.session.page
    val strokes = pageState.strokes.inInkRenderOrder()
    for (stroke in strokes) {
        val samples = stroke.samples
        if (samples.isEmpty()) continue
        val colorArgb = stroke.style.argb
        val opacity = stroke.style.opacity
        val a = ((colorArgb ushr 24) * opacity).toInt().coerceIn(0, 255)
        val r = (colorArgb shr 16) and 0xFF
        val g = (colorArgb shr 8) and 0xFF
        val b = colorArgb and 0xFF
        val finalColor = (a shl 24) or (r shl 16) or (g shl 8) or b

        val representativePressure = samples.mapNotNull(StrokeSample::pressure)
            .takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val strokeWidth = (PressureNormalizer.width(representativePressure, stroke.style) * strokeScale)
            .coerceAtLeast(0.6f)

        if (samples.size == 1) {
            fillPaint.color = finalColor
            canvas.drawCircle(
                samples[0].position.x * scaleX,
                samples[0].position.y * scaleY,
                strokeWidth / 2f,
                fillPaint,
            )
        } else {
            strokePaint.color = finalColor
            strokePaint.strokeWidth = strokeWidth
            val path = android.graphics.Path()
            path.moveTo(samples[0].position.x * scaleX, samples[0].position.y * scaleY)
            for (i in 1 until samples.size) {
                path.lineTo(samples[i].position.x * scaleX, samples[i].position.y * scaleY)
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    bitmap
}.getOrNull()


@Composable
private fun PageThumbnailPreview(page: PageEntry, modifier: Modifier = Modifier) {
    val thumbnailBitmap = page.thumbnail
    val pdfBackground = page.pdfBackground

    Canvas(
        modifier
            .shimmerPlaceholder(thumbnailBitmap == null && pdfBackground == null)
            .background(Color(page.backgroundArgb)),
    ) {
        val normalizedRotation = ((page.rotation % 360) + 360) % 360
        val quarterTurn = normalizedRotation == 90 || normalizedRotation == 270
        val contentWidth = if (quarterTurn) size.height else size.width
        val contentHeight = if (quarterTurn) size.width else size.height
        val scaleX = contentWidth / page.size.widthMicrons
        val scaleY = contentHeight / page.size.heightMicrons
        val strokeScale = (scaleX + scaleY) / 2f
        val canvas = drawContext.canvas.nativeCanvas
        val pageCheckpoint = canvas.save()
        if (normalizedRotation != 0) {
            canvas.translate(size.width / 2f, size.height / 2f)
            canvas.rotate(normalizedRotation.toFloat())
            canvas.translate(-contentWidth / 2f, -contentHeight / 2f)
        }

        // Fast path: Cached thumbnail bitmap contains everything pre-rendered!
        if (thumbnailBitmap != null && !thumbnailBitmap.isRecycled) {
            drawImage(
                thumbnailBitmap.asImageBitmap(),
                dstSize = IntSize(contentWidth.roundToInt(), contentHeight.roundToInt()),
            )
            canvas.restoreToCount(pageCheckpoint)
            return@Canvas
        }

        drawRect(Color(page.backgroundArgb), size = Size(contentWidth, contentHeight))
        pdfBackground?.let {
            if (!it.isRecycled) {
                drawImage(
                    it.asImageBitmap(),
                    dstSize = IntSize(contentWidth.roundToInt(), contentHeight.roundToInt()),
                )
            }
        }
        if (pdfBackground == null) {
            val templateColor = Color(page.templateLineArgb)
            val spacing = if (page.template == PageTemplate.Ruled) 9_000f else 8_000f
            when (page.template) {
                PageTemplate.Blank -> Unit
                PageTemplate.Ruled -> {
                    var y = spacing
                    while (y < page.size.heightMicrons) {
                        drawLine(templateColor, Offset(0f, y * scaleY), Offset(contentWidth, y * scaleY), 1f)
                        y += spacing
                    }
                }
                PageTemplate.Grid -> {
                    var y = spacing
                    while (y < page.size.heightMicrons) {
                        drawLine(templateColor, Offset(0f, y * scaleY), Offset(contentWidth, y * scaleY), 1f)
                        y += spacing
                    }
                    var x = spacing
                    while (x < page.size.widthMicrons) {
                        drawLine(templateColor, Offset(x * scaleX, 0f), Offset(x * scaleX, contentHeight), 1f)
                        x += spacing
                    }
                }
                PageTemplate.Dotted -> {
                    var y = spacing
                    while (y < page.size.heightMicrons) {
                        var x = spacing
                        while (x < page.size.widthMicrons) {
                            drawCircle(templateColor, .7f, Offset(x * scaleX, y * scaleY))
                            x += spacing
                        }
                        y += spacing
                    }
                }
                PageTemplate.Cornell -> {
                    var y = 36_000f
                    while (y < page.size.heightMicrons) {
                        drawLine(templateColor, Offset(0f, y * scaleY), Offset(contentWidth, y * scaleY), 1f)
                        y += 9_000f
                    }
                    drawLine(templateColor, Offset(45_000f * scaleX, 0f), Offset(45_000f * scaleX, contentHeight), 1f)
                    drawLine(templateColor, Offset(0f, 27_000f * scaleY), Offset(contentWidth, 27_000f * scaleY), 1f)
                }
                PageTemplate.MusicStaff -> {
                    var top = 18_000f
                    while (top < page.size.heightMicrons) {
                        repeat(5) { line ->
                            val y = (top + line * 2_500f) * scaleY
                            drawLine(templateColor, Offset(0f, y), Offset(size.width, y), 1f)
                        }
                        top += 24_000f
                    }
                }
            }
        }
        page.objects.sortedBy(SceneObject::zIndex).forEach { item ->
            drawThumbnailObject(item, scaleX, scaleY)
        }
        val pageState = page.session.page
        pageState.strokes.inInkRenderOrder().forEach { stroke ->
            val samples = stroke.samples
            val color = Color(stroke.style.argb).copy(alpha = stroke.style.opacity)
            val representativePressure = samples.mapNotNull(StrokeSample::pressure)
                .takeIf(List<Float>::isNotEmpty)?.average()?.toFloat()
            val strokeWidth = (PressureNormalizer.width(representativePressure, stroke.style) * strokeScale)
                .coerceAtLeast(.6f)
            if (samples.size == 1) {
                drawCircle(
                    color,
                    strokeWidth / 2f,
                    Offset(samples[0].position.x * scaleX, samples[0].position.y * scaleY),
                )
            } else {
                val path = Path().apply {
                    moveTo(samples[0].position.x * scaleX, samples[0].position.y * scaleY)
                    for (sample in samples.drop(1)) {
                        lineTo(sample.position.x * scaleX, sample.position.y * scaleY)
                    }
                }
                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
        canvas.restoreToCount(pageCheckpoint)
    }
}


private fun DrawScope.drawThumbnailObject(item: SceneObject, scaleX: Float, scaleY: Float) {
    val rotation = when (item) {
        is SceneObject.Image -> item.rotation
        is SceneObject.Rectangle -> item.rotation
        is SceneObject.TextBox -> item.rotation
        is SceneObject.Audio -> 0f
    }
    val pivot = when (item) {
        is SceneObject.Image -> Offset((item.x + item.width / 2f) * scaleX, (item.y + item.height / 2f) * scaleY)
        is SceneObject.Rectangle -> Offset((item.x + item.width / 2f) * scaleX, (item.y + item.height / 2f) * scaleY)
        is SceneObject.TextBox -> item.visualBounds().let {
            Offset((it.left + it.right) / 2f * scaleX, (it.top + it.bottom) / 2f * scaleY)
        }
        is SceneObject.Audio -> Offset((item.x + item.width / 2f) * scaleX, (item.y + item.height / 2f) * scaleY)
    }
    val canvas = drawContext.canvas.nativeCanvas
    val checkpoint = canvas.save()
    if (rotation != 0f) canvas.rotate(rotation, pivot.x, pivot.y)
    when (item) {
        is SceneObject.Image -> drawImage(
            item.bitmap.asImageBitmap(),
            srcOffset = IntOffset(
                (item.bitmap.width * item.crop.leftFraction).roundToInt(),
                (item.bitmap.height * item.crop.topFraction).roundToInt(),
            ),
            srcSize = IntSize(
                (item.bitmap.width * (1f - item.crop.leftFraction - item.crop.rightFraction)).roundToInt()
                    .coerceAtLeast(1),
                (item.bitmap.height * (1f - item.crop.topFraction - item.crop.bottomFraction)).roundToInt()
                    .coerceAtLeast(1),
            ),
            dstOffset = IntOffset((item.x * scaleX).roundToInt(), (item.y * scaleY).roundToInt()),
            dstSize = IntSize(
                (item.width * scaleX).roundToInt().coerceAtLeast(1),
                (item.height * scaleY).roundToInt().coerceAtLeast(1),
            ),
            alpha = item.opacity,
        )
        is SceneObject.Rectangle -> {
            val topLeft = Offset(item.x * scaleX, item.y * scaleY)
            val objectSize = Size(item.width * scaleX, item.height * scaleY)
            val stroke = Stroke((item.strokeWidth * (scaleX + scaleY) / 2f).coerceAtLeast(.6f))
            when (item.kind) {
                ShapeKind.Line, ShapeKind.Arrow -> drawLine(
                    Color(item.strokeArgb), topLeft, topLeft + Offset(objectSize.width, objectSize.height), stroke.width,
                )
                ShapeKind.Rectangle -> {
                    item.fillArgb?.let { drawRect(Color(it), topLeft, objectSize) }
                    drawRect(Color(item.strokeArgb), topLeft, objectSize, style = stroke)
                }
                ShapeKind.RoundedRectangle -> {
                    val corner = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    item.fillArgb?.let { drawRoundRect(Color(it), topLeft, objectSize, corner) }
                    drawRoundRect(Color(item.strokeArgb), topLeft, objectSize, corner, style = stroke)
                }
                ShapeKind.Ellipse -> {
                    item.fillArgb?.let { drawOval(Color(it), topLeft, objectSize) }
                    drawOval(Color(item.strokeArgb), topLeft, objectSize, style = stroke)
                }
                ShapeKind.Triangle -> {
                    val path = Path().apply {
                        moveTo(topLeft.x + objectSize.width / 2f, topLeft.y)
                        lineTo(topLeft.x + objectSize.width, topLeft.y + objectSize.height)
                        lineTo(topLeft.x, topLeft.y + objectSize.height)
                        close()
                    }
                    item.fillArgb?.let { drawPath(path, Color(it)) }
                    drawPath(path, Color(item.strokeArgb), style = stroke)
                }
            }
        }
        is SceneObject.TextBox -> {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.argb
                textSize = item.size * scaleY
                isUnderlineText = item.underline
                typeface = android.graphics.Typeface.create(
                    if (item.fontFamily.startsWith("file:")) null else item.fontFamily,
                    when {
                        item.bold && item.italic -> android.graphics.Typeface.BOLD_ITALIC
                        item.bold -> android.graphics.Typeface.BOLD
                        item.italic -> android.graphics.Typeface.ITALIC
                        else -> android.graphics.Typeface.NORMAL
                    },
                )
                textAlign = when (item.alignment) {
                    TextAlignment.Start -> Paint.Align.LEFT
                    TextAlignment.Center -> Paint.Align.CENTER
                    TextAlignment.End -> Paint.Align.RIGHT
                }
            }
            if (item.fontFamily.startsWith("file:")) runCatching {
                paint.typeface = android.graphics.Typeface.createFromFile(item.fontFamily.removePrefix("file:"))
            }
            val baseX = when (item.alignment) {
                TextAlignment.Start -> item.x
                TextAlignment.Center -> item.x + item.width / 2f
                TextAlignment.End -> item.x + item.width
            } * scaleX
            item.text.lines().forEachIndexed { index, line ->
                val baseline = (item.y + index * item.size * 1.2f) * scaleY
                item.outlineArgb?.takeIf { item.outlineWidth > 0f }?.let { outlineColor ->
                    val fillColor = paint.color
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (item.outlineWidth * scaleX).coerceAtLeast(.5f)
                    paint.color = outlineColor
                    canvas.drawText(line, baseX, baseline, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = fillColor
                }
                canvas.drawText(line, baseX, baseline, paint)
            }
        }
        is SceneObject.Audio -> {
            val radius = (minOf(item.width * scaleX, item.height * scaleY) / 2f).coerceAtLeast(2f)
            drawCircle(Color(0xFFEF4444), radius, pivot)
            drawCircle(Color.White, radius * .24f, pivot)
        }
    }
    canvas.restoreToCount(checkpoint)
}

@Composable
private fun PageInsertionMarker(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
        Box(Modifier.weight(1f).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
    }
}

private const val DOCUMENT_PAGE_GAP = 18_000f

private data class PagePlacement(
    val index: Int,
    val page: PageEntry,
    val left: Float,
    val top: Float,
) {
    val right get() = left + page.size.widthMicrons
    val bottom get() = top + page.size.heightMicrons
    fun contains(point: PagePoint) = point.x in left..right && point.y in top..bottom
    fun toLocal(point: PagePoint) = PagePoint(point.x - left, point.y - top)
}

private data class VisibleInkPageKey(
    val pageId: PageId,
    val left: Float,
    val top: Float,
    val renderRevision: Int,
)

private data class PendingInkStroke(
    val page: PageEntry,
    val nativeStroke: FinishedInkStroke,
    val inputOriginX: Float,
    val inputOriginY: Float,
)

private data class MixedSelection(
    val pageIndex: Int,
    val strokeIds: Set<StrokeId>,
    val objectIds: Set<ObjectId>,
)

private data class LassoCaptureRegion(
    val pageIndex: Int,
    val bounds: PageRect,
)

private data class LassoScreenshot(
    val region: LassoCaptureRegion,
    val bitmap: Bitmap,
)

private data class SelectionClipboard(
    val strokes: List<InkStroke>,
    val objects: List<SceneObject>,
)

private data class DocumentLayout(
    val width: Float,
    val height: Float,
    val pages: List<PagePlacement>,
) {
    fun pageAt(point: PagePoint): PagePlacement? = pages.firstOrNull { it.contains(point) }
}

private fun createDocumentLayout(pages: List<PageEntry>): DocumentLayout {
    val width = pages.maxOfOrNull { it.size.widthMicrons } ?: A4_WIDTH
    var top = DOCUMENT_PAGE_GAP
    val placements = pages.mapIndexed { index, page ->
        PagePlacement(index, page, (width - page.size.widthMicrons) / 2f, top).also {
            top += page.size.heightMicrons + DOCUMENT_PAGE_GAP
        }
    }
    return DocumentLayout(width, top, placements)
}

private fun AutomaticOcrResult.toRecognizedPageText(
    pageSize: PageSize,
    bitmapWidth: Int,
    bitmapHeight: Int,
): RecognizedPageText {
    fun android.graphics.Rect.toPageBounds(): PageRect? {
        if (width() <= 0 || height() <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) return null
        val pageLeft = (left.toFloat() / bitmapWidth * pageSize.widthMicrons)
            .coerceIn(0f, pageSize.widthMicrons)
        val pageTop = (top.toFloat() / bitmapHeight * pageSize.heightMicrons)
            .coerceIn(0f, pageSize.heightMicrons)
        val pageRight = (right.toFloat() / bitmapWidth * pageSize.widthMicrons)
            .coerceIn(pageLeft, pageSize.widthMicrons)
        val pageBottom = (bottom.toFloat() / bitmapHeight * pageSize.heightMicrons)
            .coerceIn(pageTop, pageSize.heightMicrons)
        return PageRect(pageLeft, pageTop, pageRight, pageBottom)
    }

    val recognizedTokens = tokens.mapNotNull { token ->
        token.text.trim().takeIf(String::isNotBlank)?.let { tokenText ->
            token.bounds.toPageBounds()?.let { bounds ->
                RecognizedTextToken(tokenText, bounds, token.lineIndex)
            }
        }
    }
    return RecognizedPageText(recognizedTokens, languageDisplayName())
}

private fun PageEntry.ensureRuntimeStrokes() {
    val sourcePage = session.page
    if (runtimeStrokesMatch(sourcePage)) return
    val strokes = sourcePage.strokes
    val currentIds = strokes.mapTo(hashSetOf(), InkStroke::id)
    var changed = renderStrokes.keys.removeAll { it !in currentIds }
    changed = renderStrokeFailures.keys.removeAll { it !in currentIds } || changed
    strokes.forEach { stroke ->
        if (stroke.id !in renderStrokes && stroke.id !in renderStrokeFailures) {
            runCatching { stroke.toAndroidXStroke() }
                .onSuccess {
                    renderStrokes[stroke.id] = RuntimeInkStroke(it, 0f, 0f)
                    changed = true
                }
                .onFailure { error ->
                    renderStrokeFailures[stroke.id] = error.message ?: error::class.java.simpleName
                    changed = true
                }
        }
    }
    markRuntimeStrokesSynchronized()
    if (changed) markRuntimeStrokesChanged()
}

private fun PageEntry.syncRuntimeStrokes() {
    invalidateRuntimeStrokeSource()
    ensureRuntimeStrokes()
}

@Composable
private fun ContinuousPageCanvas(
    tab: EditorTab,
    mode: EditorMode,
    actionMappings: List<ActionMapping>,
    actionRecognizer: ActionMapRecognizer,
    onAction: (MappedAction) -> Unit,
    strokeSizePreview: StrokeSizePreview?,
    actionActivationMode: ActionActivationMode,
    drawAndHoldEnabled: Boolean,
    customFonts: List<String>,
    onStylusButtonPressed: (Int) -> Unit,
    onImagePlacementRequested: (PageId, PagePoint) -> Unit,
    showDiagnostics: Boolean,
    appSettings: AppSettings,
    radialMenuRequestGeneration: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val layout = createDocumentLayout(tab.pages)
    var viewport by remember(tab.id) {
        mutableStateOf(ViewportTransform(1f, 0f, 0f, layout.width, layout.height))
    }
    var minimumScale by remember(tab.id) { mutableStateOf(1f) }
    var restoredSavedViewport by remember(tab.id) { mutableStateOf(false) }
    var viewportWidth by remember { mutableIntStateOf(1) }
    var viewportHeight by remember { mutableIntStateOf(1) }
    var selectedObject by remember(tab.id) { mutableIntStateOf(-1) }
    var selectedObjectPage by remember(tab.id) { mutableIntStateOf(-1) }
    var copiedImage by remember(tab.id) { mutableStateOf<SceneObject.Image?>(null) }
    var selectionClipboard by remember(tab.id) { mutableStateOf<SelectionClipboard?>(null) }
    var clipboardPasteTargetPage by remember(tab.id) { mutableIntStateOf(-1) }
    var mixedSelection by remember(tab.id) { mutableStateOf<MixedSelection?>(null) }
    var lassoPoints by remember(tab.id) { mutableStateOf<List<PagePoint>>(emptyList()) }
    var lassoCaptureRegion by remember(tab.id) { mutableStateOf<LassoCaptureRegion?>(null) }
    var lassoScreenshot by remember(tab.id) { mutableStateOf<LassoScreenshot?>(null) }
    var pendingLassoScreenshotSave by remember(tab.id) { mutableStateOf<Bitmap?>(null) }
    var lassoScreenshotBusy by remember(tab.id) { mutableStateOf(false) }
    var lassoScreenshotError by remember(tab.id) { mutableStateOf<String?>(null) }
    var selectionDrag by remember(tab.id) { mutableStateOf(PagePoint(0f, 0f)) }
    var movingSelection by remember(tab.id) { mutableStateOf(false) }
    var showCropEditor by remember(tab.id) { mutableStateOf(false) }
    var showTextEditor by remember(tab.id) { mutableStateOf(false) }
    var showShapeEditor by remember(tab.id) { mutableStateOf(false) }
    var showAudioPlayer by remember(tab.id) { mutableStateOf(false) }
    var fingerResizingImage by remember(tab.id) { mutableStateOf(false) }
    var stylusObjectPage by remember(tab.id) { mutableIntStateOf(-1) }
    var stylusObjectIndex by remember(tab.id) { mutableIntStateOf(-1) }
    var stylusResizingObject by remember(tab.id) { mutableStateOf(false) }
    var stylusCanTransformObject by remember(tab.id) { mutableStateOf(false) }
    var stylusObjectEditRecorded by remember(tab.id) { mutableStateOf(false) }
    var stylusShapeAnchor by remember(tab.id) { mutableStateOf<PagePoint?>(null) }
    var stylusShapeCurrent by remember(tab.id) { mutableStateOf<PagePoint?>(null) }
    var ocrText by remember(tab.id) { mutableStateOf<String?>(null) }
    var ocrTitle by remember(tab.id) { mutableStateOf("Text from image") }
    var ocrDetail by remember(tab.id) { mutableStateOf("The result stays on your device.") }
    var ocrBusy by remember(tab.id) { mutableStateOf(false) }
    val recognizedPdfText = remember(tab.id) { mutableStateMapOf<PageId, RecognizedPageText>() }
    var textRangeSelection by remember(tab.id) { mutableStateOf<TextRangeSelection?>(null) }
    var draggedTextHandle by remember(tab.id) { mutableStateOf<TextSelectionHandle?>(null) }
    var draggedTextHandlePoint by remember(tab.id) { mutableStateOf<PagePoint?>(null) }
    var activeAudioRecording by remember(tab.id) { mutableStateOf<ActiveAudioRecording?>(null) }
    var pendingAudioPoint by remember(tab.id) { mutableStateOf<PagePoint?>(null) }
    var audioError by remember(tab.id) { mutableStateOf<String?>(null) }
    var confirmSelectedDelete by remember(tab.id) { mutableStateOf(false) }
    var confirmMixedDelete by remember(tab.id) { mutableStateOf(false) }
    var shapeEraseUndoRecorded by remember(tab.id) { mutableStateOf(false) }
    var eraserHistoryActive by remember(tab.id) { mutableStateOf(false) }
    var fingerObjectEditRecorded by remember(tab.id) { mutableStateOf(false) }
    var lastStylusHover by remember(tab.id) { mutableStateOf<StylusHoverSample?>(null) }
    var radialMenuCenter by remember(tab.id) { mutableStateOf<Offset?>(null) }
    var radialMenuHighlighted by remember(tab.id) { mutableIntStateOf(-1) }
    var previousMode by remember(tab.id) { mutableStateOf(mode) }
    var deferredWetUndoCount by remember(tab.id) { mutableIntStateOf(0) }

    fun clearHistorySelection() {
        textRangeSelection = null
        draggedTextHandle = null
        draggedTextHandlePoint = null
        mixedSelection = null
        lassoPoints = emptyList()
        lassoCaptureRegion = null
        selectionDrag = PagePoint(0f, 0f)
        movingSelection = false
        selectedObject = -1
        selectedObjectPage = -1
        stylusObjectPage = -1
        stylusObjectIndex = -1
        stylusCanTransformObject = false
        stylusResizingObject = false
        fingerResizingImage = false
        showTextEditor = false
        showShapeEditor = false
    }

    val clearSelectionForHistory by rememberUpdatedState(::clearHistorySelection)
    val hasUndoableSelection = textRangeSelection != null || mixedSelection != null ||
        lassoCaptureRegion != null || lassoPoints.isNotEmpty() || selectedObject >= 0

    LaunchedEffect(hasUndoableSelection) {
        if (hasUndoableSelection) tab.beginSelectionUndo() else tab.endSelectionUndo()
    }

    DisposableEffect(tab.id) {
        tab.clearSelectionForHistory = { clearSelectionForHistory() }
        onDispose {
            tab.clearSelectionForHistory = {}
            tab.endSelectionUndo()
        }
    }

    val textRecognizer = remember { AutomaticTextRecognizer(context) }
    DisposableEffect(textRecognizer) { onDispose { textRecognizer.close() } }

    val saveLassoScreenshot = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        val bitmap = pendingLassoScreenshotSave
        pendingLassoScreenshotSave = null
        if (uri != null && bitmap != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                        "The screenshot could not be encoded."
                    }
                } ?: error("The selected file could not be opened.")
            }.onSuccess {
                lassoScreenshot?.takeIf { it.bitmap === bitmap }?.let {
                    lassoScreenshot = null
                }
                bitmap.takeUnless(Bitmap::isRecycled)?.recycle()
            }.onFailure { error ->
                lassoScreenshotError = "Screenshot could not be saved: ${error.message ?: "Unknown error"}"
            }
        }
    }

    LaunchedEffect(mode) {
        if (previousMode == EditorMode.Lasso && mode != EditorMode.Lasso) {
            mixedSelection = null
            lassoPoints = emptyList()
            lassoCaptureRegion = null
            selectionDrag = PagePoint(0f, 0f)
            movingSelection = false
        }
        previousMode = mode
    }

    BackHandler(
        enabled = radialMenuCenter != null || lassoScreenshot != null || lassoCaptureRegion != null || mixedSelection != null ||
            lassoPoints.isNotEmpty() || clipboardPasteTargetPage >= 0 || selectedObject >= 0 ||
            showCropEditor || showTextEditor || showShapeEditor || showAudioPlayer ||
            confirmSelectedDelete || confirmMixedDelete || textRangeSelection != null,
    ) {
        when {
            radialMenuCenter != null -> {
                radialMenuCenter = null
                radialMenuHighlighted = -1
            }
            textRangeSelection != null -> {
                textRangeSelection = null
                draggedTextHandle = null
                draggedTextHandlePoint = null
            }
            lassoScreenshot != null -> {
                lassoScreenshot?.bitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
                lassoScreenshot = null
            }
            showCropEditor -> showCropEditor = false
            showTextEditor -> showTextEditor = false
            showShapeEditor -> showShapeEditor = false
            showAudioPlayer -> showAudioPlayer = false
            confirmSelectedDelete -> confirmSelectedDelete = false
            confirmMixedDelete -> confirmMixedDelete = false
            clipboardPasteTargetPage >= 0 -> clipboardPasteTargetPage = -1
            else -> clearHistorySelection()
        }
    }

    lateinit var beginAudioRecording: (PagePoint) -> Unit
    val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val point = pendingAudioPoint
        pendingAudioPoint = null
        if (granted && point != null) beginAudioRecording(point)
        else if (!granted) audioError = "Microphone permission is needed to record an audio note."
    }
    beginAudioRecording = begin@{ point ->
        if (activeAudioRecording != null) return@begin
        val placement = layout.pageAt(point) ?: return@begin
        val local = placement.toLocal(point)
        val (assetId, file) = tab.newAudioAsset()
        val sampleRate = 16_000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4_096)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2,
        )

        val recording = AtomicBoolean(true)
        val worker = Thread({
            FileOutputStream(file).buffered().use { output ->
                output.write(ByteArray(44))
                val buffer = ByteArray(bufferSize)
                while (recording.get()) {
                    val count = recorder.read(buffer, 0, buffer.size)
                    if (count > 0) output.write(buffer, 0, count)
                }
            }
        }, "Mheenotes-audio-record")

        runCatching {
            check(recorder.state == AudioRecord.STATE_INITIALIZED) { "The microphone could not be initialized." }
            recorder.startRecording()
            worker.start()
            activeAudioRecording = ActiveAudioRecording(
                placement.index, local, assetId, file, recorder, worker, recording, SystemClock.elapsedRealtime(),
            )
            tab.selectedPageIndex = placement.index
        }.onFailure { error ->
            recording.set(false)
            runCatching { recorder.release() }
            file.delete()
            audioError = "Recording could not start: ${error.message ?: "Unknown error"}"
        }
    }

    fun requestAudioRecording(point: PagePoint) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginAudioRecording(point)
        } else {
            pendingAudioPoint = point
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAudioRecording() {
        val active = activeAudioRecording ?: return
        activeAudioRecording = null
        active.recording.set(false)
        val stopped = runCatching { active.recorder.stop() }.isSuccess
        runCatching { active.worker.join(1_500) }
        runCatching { active.recorder.release() }
        val duration = (SystemClock.elapsedRealtime() - active.startedAtMillis).coerceAtLeast(500L)
        if (stopped && active.file.length() > 44L) runCatching { writeWavHeader(active.file, 16_000, 1, 16) }
        if (!stopped || !active.file.isFile || active.file.length() <= 44L) {
            active.file.delete()
            audioError = "The recording was too short and was not saved."
            return
        }
        val page = tab.pages.getOrNull(active.pageIndex) ?: return
        tab.recordObjectEdit(page)
        val zIndex = (page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
        val diameter = (minOf(viewportWidth, viewportHeight).coerceAtLeast(1) / viewport.scale) * .12f
        page.objects += SceneObject.Audio(
            asset = active.file,
            x = (active.position.x - diameter / 2f).coerceIn(0f, (page.size.widthMicrons - diameter).coerceAtLeast(0f)),
            y = (active.position.y - diameter / 2f).coerceIn(0f, (page.size.heightMicrons - diameter).coerceAtLeast(0f)),
            width = diameter,
            height = diameter,
            durationMillis = duration,
            transcript = "",
            assetId = active.assetId,
            zIndex = zIndex,
        )
        selectedObjectPage = active.pageIndex
        selectedObject = page.objects.lastIndex
        showAudioPlayer = true
        tab.persistObjects()
    }

    fun addCorrectedShape(page: PageEntry, corrected: CorrectedShape, style: StrokeStyle) {
        tab.recordObjectEdit(page)
        val zIndex = (page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
        val lineStart = corrected.lineStart
        val lineEnd = corrected.lineEnd
        val lineWidth = if (lineStart != null && lineEnd != null) {
            kotlin.math.abs(lineEnd.x - lineStart.x).coerceAtLeast(2_000f)
        } else null
        val lineHeight = if (lineStart != null && lineEnd != null) {
            kotlin.math.abs(lineEnd.y - lineStart.y).coerceAtLeast(2_000f)
        } else null
        val lineCenter = if (lineStart != null && lineEnd != null) {
            PagePoint((lineStart.x + lineEnd.x) / 2f, (lineStart.y + lineEnd.y) / 2f)
        } else null
        val lineRotation = if (lineStart != null && lineEnd != null && lineWidth != null && lineHeight != null) {
            val desiredAngle = kotlin.math.atan2(lineEnd.y - lineStart.y, lineEnd.x - lineStart.x)
            val storedAngle = kotlin.math.atan2(lineHeight, lineWidth)
            Math.toDegrees((desiredAngle - storedAngle).toDouble()).toFloat()
        } else 0f
        page.objects += SceneObject.Rectangle(
            x = lineCenter?.let { it.x - lineWidth!! / 2f } ?: corrected.bounds.left,
            y = lineCenter?.let { it.y - lineHeight!! / 2f } ?: corrected.bounds.top,
            width = lineWidth ?: (corrected.bounds.right - corrected.bounds.left).coerceAtLeast(2_000f),
            height = lineHeight ?: (corrected.bounds.bottom - corrected.bounds.top).coerceAtLeast(2_000f),
            kind = corrected.kind,
            strokeArgb = (style.argb and 0x00FF_FFFF) or
                ((style.opacity * 255f).roundToInt().coerceIn(0, 255) shl 24),
            strokeWidth = style.maximumWidth,
            rotation = lineRotation,
            zIndex = zIndex,
        )
        tab.persistObjects()
    }

    DisposableEffect(tab.id) {
        onDispose {
            if (eraserHistoryActive) tab.endHistoryTransaction()
            activeAudioRecording?.let { active ->
                active.recording.set(false)
                runCatching { active.recorder.stop() }
                runCatching { active.worker.join(1_500) }
                runCatching { active.recorder.release() }
                active.file.delete()
            }
        }
    }




    fun recognizeImageText(image: SceneObject.Image) {
        if (ocrBusy) return
        ocrBusy = true
        ocrTitle = "Text from image"
        ocrDetail = "The result stays on your device."
        val cropped = runCatching {
            val left = (image.bitmap.width * image.crop.leftFraction).roundToInt()
            val top = (image.bitmap.height * image.crop.topFraction).roundToInt()
            val width = (image.bitmap.width * (1f - image.crop.leftFraction - image.crop.rightFraction)).roundToInt().coerceAtLeast(1)
            val height = (image.bitmap.height * (1f - image.crop.topFraction - image.crop.bottomFraction)).roundToInt().coerceAtLeast(1)
            Bitmap.createBitmap(image.bitmap, left, top, width, height)
        }.getOrDefault(image.bitmap)
        textRecognizer.process(cropped) { recognition ->
            recognition.onSuccess { result ->
                ocrBusy = false
                ocrDetail = "Detected ${result.languageDisplayName()}. The result stays on your device."
                ocrText = result.text.ifBlank { "No readable text was found in this image." }
            }.onFailure { error ->
                ocrBusy = false
                ocrText = "Text recognition failed: ${error.message ?: "Unknown error"}"
            }
            if (cropped !== image.bitmap && !cropped.isRecycled) cropped.recycle()
        }
    }

    fun beginPdfTextSelection(page: PageEntry, point: PagePoint) {
        if (ocrBusy || page.pdfSource == null || page.objects.any { it.contains(point) }) return
        ocrText = null
        ocrTitle = "Text selection"
        ocrDetail = "No selectable text was found near that point."
        fun fail(message: String) {
            ocrBusy = false
            ocrText = message
        }

        fun activate(recognized: RecognizedPageText) {
            val nearest = nearestRecognizedTokenIndex(
                tokens = recognized.tokens,
                point = point,
                maxDistance = minOf(page.size.widthMicrons, page.size.heightMicrons) * .055f,
            ) ?: return fail("No selectable text was found near that point.")
            ocrBusy = false
            textRangeSelection = TextRangeSelection(
                pageId = page.id,
                tokens = recognized.tokens,
                startIndex = nearest,
                endIndex = nearest,
                languageName = recognized.languageName,
            )
            draggedTextHandle = null
            draggedTextHandlePoint = null
            mixedSelection = null
            lassoPoints = emptyList()
            lassoCaptureRegion = null
            selectedObject = -1
            selectedObjectPage = -1
            clipboardPasteTargetPage = -1
        }

        recognizedPdfText[page.id]?.let {
            activate(it)
            return
        }

        ocrBusy = true
        tab.renderPdfForText(page.id) { rendered ->
            rendered.onFailure { error ->
                fail("Text could not be read: ${error.message ?: "Unknown error"}")
            }.onSuccess { bitmap ->
                textRecognizer.process(bitmap) { recognition ->
                    recognition.onSuccess { result ->
                        val recognized = result.toRecognizedPageText(page.size, bitmap.width, bitmap.height)
                        recognizedPdfText[page.id] = recognized
                        activate(recognized)
                    }.onFailure { error ->
                        fail("Text recognition failed: ${error.message ?: "Unknown error"}")
                    }
                    bitmap.takeUnless(Bitmap::isRecycled)?.recycle()
                }
            }
        }
    }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var inkOverlay by remember { mutableStateOf<InkAuthoringOverlayView?>(null) }
    val pendingInk = remember(tab.id) { mutableStateListOf<PendingInkStroke>() }
    var actionRecognitionJob by remember(tab.id) { mutableStateOf<Job?>(null) }
    val actionScope = rememberCoroutineScope()
    val pageJumpPadding = with(LocalDensity.current) { 20.dp.toPx() }
    val objectHandleRadius = with(LocalDensity.current) { 30.dp.toPx() }
    val textHandleVisualOffset = with(LocalDensity.current) { 15.dp.toPx() }
    val textSelectionPadding = with(LocalDensity.current) { 4.dp.toPx() }
    val radialMenuRadius = with(LocalDensity.current) { 132.dp.toPx() }
    val hoverRecognizer = remember(tab.id) {
        HoverDoubleTapRecognizer(
            HoverShortcutConfig(
                appSettings.hoverTapImpulseThresholdDp,
                appSettings.hoverTapMaximumDriftDp,
                appSettings.hoverTapMaximumIntervalMillis.toLong(),
            ),
            density = context.resources.displayMetrics.density,
        )
    }
    val hapticFeedback = LocalHapticFeedback.current
    val selectionColor = MaterialTheme.colorScheme.primary

    fun syncWetInkUndoAvailability(activeInk: Boolean = inkOverlay?.hasUncommittedInk == true) {
        tab.updateWetInkUndoAvailable(activeInk || pendingInk.isNotEmpty())
    }

    val radialTools = remember {
        listOf(
            RadialToolItem("Pen", Icons.Outlined.Create, MappedAction.Pen),
            RadialToolItem("Pencil", Icons.Outlined.Edit, MappedAction.Pencil),
            RadialToolItem("Highlighter", Icons.Outlined.Highlight, MappedAction.Highlighter),
            RadialToolItem("Eraser", Icons.Outlined.AutoFixHigh, MappedAction.Eraser),
            RadialToolItem("Lasso", Icons.Outlined.Gesture, MappedAction.Lasso),
            RadialToolItem("Shapes", Icons.Outlined.ShapeLine, MappedAction.Shapes),
            RadialToolItem("Text", Icons.Outlined.TextFields, MappedAction.Text),
            RadialToolItem("Image", Icons.Outlined.Image, MappedAction.Image),
        )
    }

    fun openRadialMenu(requested: Offset) {
        val margin = radialMenuRadius + with(context.resources.displayMetrics) { 18f * density }
        radialMenuCenter = Offset(
            requested.x.coerceIn(margin, (viewportWidth - margin).coerceAtLeast(margin)),
            requested.y.coerceIn(margin, (viewportHeight - margin).coerceAtLeast(margin)),
        )
        radialMenuHighlighted = -1
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun updateRadialHighlight(sample: StylusHoverSample) {
        val center = radialMenuCenter ?: return
        val dx = sample.x - center.x
        val dy = sample.y - center.y
        val distance = hypot(dx, dy)
        val next = if (distance < radialMenuRadius * .28f) -1 else {
            val normalized = ((atan2(dy, dx) + Math.PI / 2.0 + Math.PI * 2.0) % (Math.PI * 2.0))
            ((normalized / (Math.PI * 2.0) * radialTools.size).roundToInt() % radialTools.size)
        }
        if (next != radialMenuHighlighted) {
            radialMenuHighlighted = next
            if (next >= 0) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(
        appSettings.hoverTapImpulseThresholdDp,
        appSettings.hoverTapMaximumDriftDp,
        appSettings.hoverTapMaximumIntervalMillis,
    ) {
        hoverRecognizer.update(
            HoverShortcutConfig(
                appSettings.hoverTapImpulseThresholdDp,
                appSettings.hoverTapMaximumDriftDp,
                appSettings.hoverTapMaximumIntervalMillis.toLong(),
            ),
            context.resources.displayMetrics.density,
        )
    }

    LaunchedEffect(radialMenuRequestGeneration) {
        if (radialMenuRequestGeneration > 0) {
            val hover = lastStylusHover
            openRadialMenu(hover?.let { Offset(it.x, it.y) } ?: Offset(viewportWidth / 2f, viewportHeight / 2f))
        }
    }

    fun captureLassoScreenshot() {
        if (lassoScreenshotBusy) return
        val region = lassoCaptureRegion ?: return
        val page = tab.pages.getOrNull(region.pageIndex) ?: return
        val exportPage = page.toExportPage()
        lassoScreenshotBusy = true
        actionScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val rendered = PageExporter.renderImage(exportPage)
                    val scaleX = rendered.width / page.size.widthMicrons
                    val scaleY = rendered.height / page.size.heightMicrons
                    val left = kotlin.math.floor(region.bounds.left * scaleX).toInt()
                        .coerceIn(0, rendered.width - 1)
                    val top = kotlin.math.floor(region.bounds.top * scaleY).toInt()
                        .coerceIn(0, rendered.height - 1)
                    val right = kotlin.math.ceil(region.bounds.right * scaleX).toInt()
                        .coerceIn(left + 1, rendered.width)
                    val bottom = kotlin.math.ceil(region.bounds.bottom * scaleY).toInt()
                        .coerceIn(top + 1, rendered.height)
                    val cropped = if (
                        left == 0 && top == 0 && right == rendered.width && bottom == rendered.height
                    ) rendered else Bitmap.createBitmap(rendered, left, top, right - left, bottom - top).also {
                        rendered.recycle()
                    }
                    LassoScreenshot(region, cropped)
                }
            }
            lassoScreenshotBusy = false
            result.onSuccess { screenshot ->
                if (lassoCaptureRegion == region) {
                    lassoScreenshot?.bitmap?.takeIf { !it.isRecycled }?.recycle()
                    lassoScreenshot = screenshot
                } else {
                    screenshot.bitmap.takeIf { !it.isRecycled }?.recycle()
                }
            }.onFailure { error ->
                lassoScreenshotError = "Screenshot could not be created: ${error.message ?: "Unknown error"}"
            }
        }
    }

    fun insertLassoScreenshot() {
        val screenshot = lassoScreenshot ?: return
        val page = tab.pages.getOrNull(screenshot.region.pageIndex) ?: return
        val stored = runCatching {
            val encoded = java.io.ByteArrayOutputStream().use { output ->
                check(screenshot.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "The screenshot could not be encoded."
                }
                output.toByteArray()
            }
            java.io.ByteArrayInputStream(encoded).use(tab::storeImageAsset)
        }.getOrElse { error ->
            lassoScreenshotError = "Screenshot could not be inserted: ${error.message ?: "Unknown error"}"
            return
        }
        val bounds = screenshot.region.bounds
        tab.recordObjectEdit(page)
        page.objects += SceneObject.Image(
            bitmap = screenshot.bitmap,
            asset = stored.second,
            assetId = stored.first,
            x = bounds.left,
            y = bounds.top,
            width = bounds.right - bounds.left,
            height = bounds.bottom - bounds.top,
            zIndex = (page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L,
        )
        lassoScreenshot = null
        lassoCaptureRegion = null
        mixedSelection = null
        selectedObjectPage = screenshot.region.pageIndex
        selectedObject = page.objects.lastIndex
        tab.selectedPageIndex = screenshot.region.pageIndex
        tab.persistObjects()
    }

    val visibleWorldTop = -viewport.offsetY / viewport.scale
    val visibleWorldBottom = (viewportHeight - viewport.offsetY) / viewport.scale
    val visiblePlacements = layout.pages.asSequence()
        .filter { it.bottom >= visibleWorldTop && it.top <= visibleWorldBottom }
        .toList()
    visiblePlacements.forEach { it.page.ensureRuntimeStrokes() }
    val hiddenStrokeIds = if (movingSelection) mixedSelection?.strokeIds.orEmpty() else emptySet()
    val visibleInkKey = visiblePlacements.map { placement ->
        VisibleInkPageKey(
            placement.page.id,
            placement.left,
            placement.top,
            placement.page.renderRevision,
        )
    }
    val permanentCommittedStrokes = remember(visibleInkKey, hiddenStrokeIds) {
        visiblePlacements.flatMap { placement ->
            placement.page.session.page.strokes.inInkRenderOrder().mapNotNull { stroke ->
                if (stroke.id in hiddenStrokeIds) return@mapNotNull null
                placement.page.renderStrokes[stroke.id]?.let { runtime ->
                    CommittedInkStroke(
                        stroke = runtime.stroke,
                        documentOffsetX = placement.left - runtime.inputOriginX,
                        documentOffsetY = placement.top - runtime.inputOriginY,
                        isHighlighter = stroke.style.tool == InkTool.Highlighter,
                    )
                }
            }
        }
    }
    val pendingInkSnapshot = pendingInk.toList()
    val stagedCommittedStrokes = remember(visibleInkKey, pendingInkSnapshot) {
        pendingInkSnapshot.mapNotNull { pending ->
            val placement = visiblePlacements.firstOrNull { it.page === pending.page }
                ?: return@mapNotNull null
            CommittedInkStroke(
                stroke = pending.nativeStroke.renderStroke,
                documentOffsetX = placement.left - pending.inputOriginX,
                documentOffsetY = placement.top - pending.inputOriginY,
                isHighlighter = pending.nativeStroke.style.tool == InkTool.Highlighter,
            )
        }
    }
    val visibleCommittedStrokes = remember(permanentCommittedStrokes, stagedCommittedStrokes) {
        permanentCommittedStrokes + stagedCommittedStrokes
    }

    fun selectedImage(): Pair<PageEntry, SceneObject.Image>? {
        val page = tab.pages.getOrNull(selectedObjectPage) ?: return null
        val image = page.objects.getOrNull(selectedObject) as? SceneObject.Image ?: return null
        return page to image
    }

    fun selectedSceneObject(): Pair<PageEntry, SceneObject>? {
        val page = tab.pages.getOrNull(selectedObjectPage) ?: return null
        return page to (page.objects.getOrNull(selectedObject) ?: return null)
    }

    fun replaceSelectedObject(operation: (SceneObject) -> SceneObject) {
        val (page, item) = selectedSceneObject() ?: return
        tab.recordObjectEdit(page)
        page.objects[selectedObject] = operation(item)
        tab.persistObjects()
    }

    fun removeSelectedObject() {
        val (page, _) = selectedSceneObject() ?: return
        tab.recordObjectEdit(page)
        page.objects.removeAt(selectedObject)
        page.reindexObjects()
        selectedObject = -1
        selectedObjectPage = -1
        tab.persistObjects()
    }

    fun moveSelectedObjectToFront(front: Boolean) {
        val (page, item) = selectedSceneObject() ?: return
        tab.recordObjectEdit(page)
        page.objects.removeAt(selectedObject)
        if (front) page.objects.add(item) else page.objects.add(0, item)
        page.reindexObjects()
        selectedObject = if (front) page.objects.lastIndex else 0
        tab.persistObjects()
    }

    fun replaceSelectedImage(operation: (SceneObject.Image) -> SceneObject.Image) {
        val page = tab.pages.getOrNull(selectedObjectPage) ?: return
        val image = page.objects.getOrNull(selectedObject) as? SceneObject.Image ?: return
        tab.recordObjectEdit(page)
        page.objects[selectedObject] = operation(image)
        tab.persistObjects()
    }

    fun removeSelectedImage(copyFirst: Boolean) {
        val page = tab.pages.getOrNull(selectedObjectPage) ?: return
        val image = page.objects.getOrNull(selectedObject) as? SceneObject.Image ?: return
        if (copyFirst) {
            copiedImage = image
            selectionClipboard = null
        }
        tab.recordObjectEdit(page)
        page.objects.removeAt(selectedObject)
        page.reindexObjects()
        selectedObject = -1
        selectedObjectPage = -1
        tab.persistObjects()
    }

    fun moveSelectedImageToFront(front: Boolean) {
        val page = tab.pages.getOrNull(selectedObjectPage) ?: return
        val image = page.objects.getOrNull(selectedObject) as? SceneObject.Image ?: return
        tab.recordObjectEdit(page)
        page.objects.removeAt(selectedObject)
        if (front) page.objects.add(image) else page.objects.add(0, image)
        page.reindexObjects()
        selectedObject = if (front) page.objects.lastIndex else 0
        tab.persistObjects()
    }

    fun copyMixedSelection(): SelectionClipboard? {
        val selection = mixedSelection ?: return null
        val page = tab.pages.getOrNull(selection.pageIndex) ?: return null
        copiedImage = null
        return SelectionClipboard(
            strokes = page.session.page.strokes.filter { it.id in selection.strokeIds },
            objects = page.objects.filter { it.id in selection.objectIds },
        ).also { selectionClipboard = it }
    }

    fun deleteMixedSelection() {
        val selection = mixedSelection ?: return
        val page = tab.pages.getOrNull(selection.pageIndex) ?: return
        tab.historyTransaction {
            if (selection.strokeIds.isNotEmpty()) {
                page.session.deleteStrokes(selection.strokeIds)
                page.syncRuntimeStrokes()
            }
            if (selection.objectIds.isNotEmpty()) {
                tab.recordObjectEdit(page)
                page.objects.removeAll { it.id in selection.objectIds }
                page.reindexObjects()
                tab.persistObjects()
            }
        }
        mixedSelection = null
        lassoCaptureRegion = null
    }

    fun pasteMixedSelection(targetPageIndex: Int = tab.selectedPageIndex) {
        val clipboard = selectionClipboard ?: return
        val page = tab.pages.getOrNull(targetPageIndex) ?: return
        lateinit var newStrokeIds: Set<StrokeId>
        lateinit var newObjects: List<SceneObject>
        tab.historyTransaction {
            newStrokeIds = page.session.pasteStrokes(clipboard.strokes)
            if (newStrokeIds.isNotEmpty()) page.syncRuntimeStrokes()
            newObjects = clipboard.objects.map { source ->
                source.duplicate().move(5_000f, 5_000f).withZIndex((page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L)
            }
            if (newObjects.isNotEmpty()) {
                tab.recordObjectEdit(page)
                page.objects.addAll(newObjects)
                page.reindexObjects()
                tab.persistObjects()
            }
        }
        val pastedSelection = MixedSelection(
            targetPageIndex,
            newStrokeIds,
            newObjects.mapTo(linkedSetOf(), SceneObject::id),
        )
        mixedSelection = pastedSelection
        lassoCaptureRegion = mixedSelectionBounds(pastedSelection, tab.pages)?.let {
            LassoCaptureRegion(targetPageIndex, it)
        }
        clipboardPasteTargetPage = -1
    }

    fun pasteCopiedImage(targetPageIndex: Int = tab.selectedPageIndex) {
        val source = copiedImage ?: return
        val page = tab.pages.getOrNull(targetPageIndex) ?: return
        tab.recordObjectEdit(page)
        page.objects += source.copy(
            id = ObjectId(UUID.randomUUID().toString()),
            x = source.x + 5_000f,
            y = source.y + 5_000f,
            locked = false,
            zIndex = (page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L,
        )
        selectedObjectPage = targetPageIndex
        selectedObject = page.objects.lastIndex
        tab.selectedPageIndex = targetPageIndex
        clipboardPasteTargetPage = -1
        tab.persistObjects()
    }

    fun transformMixedSelection(translation: PagePoint = PagePoint(0f, 0f), rotationRadians: Float = 0f) {
        val selection = mixedSelection ?: return
        val page = tab.pages.getOrNull(selection.pageIndex) ?: return
        val bounds = mixedSelectionBounds(selection, tab.pages) ?: return
        val pivot = PagePoint((bounds.left + bounds.right) / 2f, (bounds.top + bounds.bottom) / 2f)
        val selectedStrokes = page.session.page.strokes.filter { it.id in selection.strokeIds }
        val previousRuntimes = selectedStrokes.associate { it.id to page.renderStrokes[it.id] }
        lateinit var newStrokeIds: Set<StrokeId>
        tab.historyTransaction {
            newStrokeIds = page.session.replaceStrokes(selection.strokeIds) { stroke ->
                LassoSelection.transform(stroke, SelectionTransform(
                    translateX = translation.x,
                    translateY = translation.y,
                    rotationRadians = rotationRadians,
                    pivot = pivot,
                ))
            }
            if (selection.strokeIds.isNotEmpty()) {
                selection.strokeIds.forEach(page.renderStrokes::remove)
                if (rotationRadians == 0f && selectedStrokes.size == newStrokeIds.size) {
                    selectedStrokes.zip(newStrokeIds).forEach { (before, newId) ->
                        previousRuntimes[before.id]?.let { runtime ->
                            page.renderStrokes[newId] = runtime.copy(
                                inputOriginX = runtime.inputOriginX - translation.x,
                                inputOriginY = runtime.inputOriginY - translation.y,
                            )
                        }
                    }
                }
                page.markRuntimeStrokesChanged()
                page.syncRuntimeStrokes()
            }
            if (selection.objectIds.isNotEmpty()) {
                tab.recordObjectEdit(page)
                page.objects.indices.forEach { index ->
                    val item = page.objects[index]
                    if (item.id in selection.objectIds) {
                        val moved = item.move(translation.x, translation.y)
                        page.objects[index] = if (rotationRadians == 0f) moved else moved.rotateBy(Math.toDegrees(rotationRadians.toDouble()).toFloat())
                    }
                }
                tab.persistObjects()
            }
        }
        val transformedSelection = selection.copy(strokeIds = newStrokeIds.ifEmpty { selection.strokeIds })
        mixedSelection = transformedSelection
        lassoCaptureRegion = mixedSelectionBounds(transformedSelection, tab.pages)?.let {
            LassoCaptureRegion(transformedSelection.pageIndex, it)
        }
    }

    fun commitPendingInk(strokes: List<PendingInkStroke> = pendingInk.toList()) {
        if (strokes.isEmpty()) return
        strokes.forEach { pending ->
            val strokeId = pending.page.session.acceptCompletedStroke(
                pending.nativeStroke.samples,
                pending.nativeStroke.style,
                detectScribbleErase = false,
            )
            if (strokeId != null) {
                InkActivityTracker.recordStroke(context)
                pending.page.renderStrokes[strokeId] = RuntimeInkStroke(
                    stroke = pending.nativeStroke.renderStroke,
                    inputOriginX = pending.inputOriginX,
                    inputOriginY = pending.inputOriginY,
                )
                pending.page.markRuntimeStrokesChanged()
                pending.page.markRuntimeStrokesSynchronized()
            }
            pendingInk.remove(pending)
        }
        syncWetInkUndoAvailability()
    }

    fun resolvePendingInk() {
        val strokes = pendingInk.toList()
        if (strokes.isEmpty()) return
        val drawing = GestureDrawing(strokes.map { pending ->
            pending.nativeStroke.samples.map { sample ->
                GesturePoint(sample.position.x, sample.position.y)
            }
        })
        val match = actionRecognizer.recognize(drawing, actionMappings)
        if (match == null) {
            commitPendingInk(strokes)
        } else {
            strokes.forEach { pending ->
                pendingInk.remove(pending)
            }
            syncWetInkUndoAvailability()
            onAction(match.mapping.action)
            actionFeedback = "${match.mapping.name} · ${(match.confidence * 100).roundToInt()}%"
        }
    }

    fun finishDeferredWetUndo() {
        val count = deferredWetUndoCount
        if (count <= 0) return
        deferredWetUndoCount = 0
        actionRecognitionJob?.cancel()
        commitPendingInk()
        repeat(count) { tab.undo() }
    }

    DisposableEffect(tab.id) {
        tab.flushPendingInk = {
            actionRecognitionJob?.cancel()
            commitPendingInk()
        }
        tab.deferUndoForWetInk = {
            if (inkOverlay?.hasUncommittedInk == true) {
                deferredWetUndoCount++
                true
            } else false
        }
        onDispose {
            tab.flushPendingInk()
            tab.flushPendingInk = {}
            tab.deferUndoForWetInk = { false }
            tab.updateWetInkUndoAvailable(false)
        }
    }

    LaunchedEffect(actionMappings.isEmpty()) {
        if (actionMappings.isEmpty()) {
            actionRecognitionJob?.cancel()
            commitPendingInk()
        }
    }

    fun updateViewport(value: ViewportTransform) {
        viewport = value.copy(pageWidth = layout.width, pageHeight = layout.height)
            .clampedToDocument(viewportWidth.toFloat(), viewportHeight.toFloat())
        tab.rememberViewport(viewport, minimumScale, viewportWidth, viewportHeight)
    }

    fun restoreSavedViewportIfReady() {
        if (restoredSavedViewport || viewportWidth <= 1 || viewportHeight <= 1) return
        val nextMinimum = (viewportWidth * .90f / layout.width).coerceAtLeast(.0001f)
        minimumScale = nextMinimum
        val saved = tab.restoredViewport()
        restoredSavedViewport = true
        if (saved == null) {
            updateViewport(viewport.copy(scale = nextMinimum, offsetX = 0f, offsetY = 0f))
        } else {
            val scale = (nextMinimum * saved.zoomRatio).coerceIn(nextMinimum, nextMinimum * 8f)
            updateViewport(
                viewport.copy(
                    scale = scale,
                    offsetX = viewportWidth / 2f - saved.centerX * scale,
                    offsetY = viewportHeight / 2f - saved.centerY * scale,
                ),
            )
        }
    }

    LaunchedEffect(tab.id, viewportWidth, viewportHeight, layout.width, layout.height) {
        restoreSavedViewportIfReady()
    }

    fun scrollDocument(deltaY: Float) {
        updateViewport(viewport.copy(offsetY = viewport.offsetY + deltaY))
        val center = viewport.toPage(viewportWidth / 2f, viewportHeight / 2f)
        layout.pageAt(center)?.let { placement ->
            if (tab.selectedPageIndex != placement.index) tab.selectedPageIndex = placement.index
        }
    }

    LaunchedEffect(tab.pageNavigationGeneration) {
        if (tab.pageNavigationGeneration == 0 || viewportHeight <= 1) return@LaunchedEffect
        val placement = layout.pages.getOrNull(tab.selectedPageIndex) ?: return@LaunchedEffect
        val renderedPageHeight = placement.page.size.heightMicrons * viewport.scale
        val desiredTop = if (renderedPageHeight + pageJumpPadding * 2f <= viewportHeight) {
            (viewportHeight - renderedPageHeight) / 2f
        } else {
            pageJumpPadding
        }
        val target = viewport.copy(
            offsetY = desiredTop - placement.top * viewport.scale,
        ).clampedToDocument(viewportWidth.toFloat(), viewportHeight.toFloat())
        val startingViewport = viewport
        Animatable(startingViewport.offsetY).animateTo(
            target.offsetY,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        ) {
            updateViewport(startingViewport.copy(offsetY = value))
        }
    }

    LaunchedEffect(tab.viewportResetGeneration) {
        if (viewportWidth <= 1 || viewportHeight <= 1) return@LaunchedEffect
        updateViewport(viewport.copy(scale = minimumScale, offsetX = 0f, offsetY = 0f))
    }

    Box(
        modifier.fillMaxSize().background(Color(0xFF0E1014)).clip(RoundedCornerShape(2.dp))
            .onSizeChanged { size ->
                val previousWidth = viewportWidth
                val previousHeight = viewportHeight
                val previousMinimum = minimumScale
                val previousCenter = if (restoredSavedViewport && previousWidth > 1 && previousHeight > 1) {
                    viewport.toPage(previousWidth / 2f, previousHeight / 2f)
                } else null
                val previousZoomRatio = if (previousMinimum > 0f) viewport.scale / previousMinimum else 1f
                viewportWidth = size.width
                viewportHeight = size.height
                val nextMinimum = (size.width * .90f / layout.width).coerceAtLeast(.0001f)
                minimumScale = nextMinimum
                if (previousCenter != null) {
                    val nextScale = (nextMinimum * previousZoomRatio).coerceIn(nextMinimum, nextMinimum * 8f)
                    updateViewport(
                        viewport.copy(
                            scale = nextScale,
                            offsetX = size.width / 2f - previousCenter.x * nextScale,
                            offsetY = size.height / 2f - previousCenter.y * nextScale,
                        ),
                    )
                } else {
                    restoreSavedViewportIfReady()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val worldTop = -viewport.offsetY / viewport.scale
            val worldBottom = (size.height - viewport.offsetY) / viewport.scale
            layout.pages.filter { it.bottom >= worldTop && it.top <= worldBottom }.forEach { placement ->
                drawPlacedPage(placement, viewport, placement.index == tab.selectedPageIndex)
                placement.page.objects.forEachIndexed { objectIndex, item ->
                    val previewItem = if (
                        movingSelection && mixedSelection?.pageIndex == placement.index &&
                        item.id in mixedSelection!!.objectIds
                    ) item.move(selectionDrag.x, selectionDrag.y) else item
                    drawPlacedObject(
                        previewItem, placement, viewport,
                        selectedObjectPage == placement.index && selectedObject == objectIndex,
                    )
                }
                if (placement.page.renderStrokeFailures.isNotEmpty()) {
                    placement.page.session.page.strokes.asSequence()
                        .filter { it.id in placement.page.renderStrokeFailures }
                        .filterNot { stroke -> stroke.id in hiddenStrokeIds }
                        .toList()
                        .inInkRenderOrder()
                        .forEach { stroke -> drawSavedStroke(stroke, placement, viewport) }
                }
            }
        }
        AndroidView(
            factory = { context -> InkAuthoringOverlayView(context).also { inkOverlay = it } },
            update = { view ->
                view.viewport = viewport
                view.committedStrokes = visibleCommittedStrokes
                view.minimumScale = minimumScale
                view.stylusInputEnabled = mode == EditorMode.Ink
                view.alwaysRecognizeActionGestures = actionMappings.isNotEmpty() && actionActivationMode != ActionActivationMode.Disabled
                view.style = tab.session.currentStyle()
                view.showHoverDiagnostics = showDiagnostics
                view.onObjectStylusDown = stylusDown@{ point ->
                    if (radialMenuCenter != null) {
                        radialTools.getOrNull(radialMenuHighlighted)?.let { onAction(it.action) }
                        radialMenuCenter = null
                        radialMenuHighlighted = -1
                        return@stylusDown true
                    }
                    stylusObjectEditRecorded = false
                    val placement = layout.pageAt(point)
                    val local = placement?.toLocal(point)
                    val selectedImageForStylus = placement?.page?.objects?.getOrNull(selectedObject) as? SceneObject.Image
                    val selectedImageHandleHit = mode != EditorMode.Image && placement != null && local != null &&
                        selectedObjectPage == placement.index && selectedImageForStylus != null && !selectedImageForStylus.locked &&
                        selectedImageForStylus.resizeHandleContains(local, objectHandleRadius / viewport.scale)
                    when {
                        placement == null -> false
                        mixedSelection != null && mode != EditorMode.Lasso -> false
                        mode == EditorMode.Image -> {
                            tab.selectedPageIndex = placement.index
                            onImagePlacementRequested(placement.page.id, placement.toLocal(point))
                            true
                        }
                        selectedImageHandleHit -> {
                            stylusObjectPage = placement.index
                            stylusObjectIndex = selectedObject
                            stylusCanTransformObject = true
                            stylusResizingObject = true
                            true
                        }
                        mode == EditorMode.Lasso -> {
                            selectedObject = -1
                            selectedObjectPage = -1
                            val local = placement.toLocal(point)
                            val bounds = mixedSelection?.takeIf { it.pageIndex == placement.index }
                                ?.let { mixedSelectionBounds(it, tab.pages) }
                            movingSelection = bounds != null &&
                                local.x in bounds.left..bounds.right && local.y in bounds.top..bounds.bottom
                            selectionDrag = PagePoint(0f, 0f)
                            if (!movingSelection) {
                                mixedSelection = null
                                lassoCaptureRegion = null
                                lassoPoints = listOf(point)
                            }
                            true
                        }
                        mode == EditorMode.Object -> {
                            val local = placement.toLocal(point)
                            val selectedItem = placement.page.objects.getOrNull(selectedObject)
                                ?.takeIf { selectedObjectPage == placement.index }
                            val selectedHandleHit = selectedItem?.resizeHandleContains(
                                local,
                                objectHandleRadius / viewport.scale,
                            ) == true
                            val index = if (selectedHandleHit) selectedObject else {
                                placement.page.objects.indexOfLast { it.contains(local) }
                            }
                            if (index < 0) false else {
                                val alreadySelected = selectedObjectPage == placement.index && selectedObject == index
                                val item = placement.page.objects[index]
                                val locked = item is SceneObject.Image && item.locked
                                stylusObjectPage = placement.index
                                stylusObjectIndex = index
                                selectedObjectPage = placement.index
                                selectedObject = index
                                tab.selectedPageIndex = placement.index
                                stylusCanTransformObject = alreadySelected && !locked
                                stylusResizingObject = stylusCanTransformObject && item.resizeHandleContains(
                                    local,
                                    objectHandleRadius / viewport.scale,
                                )
                                true
                            }
                        }
                        mode == EditorMode.Text -> {
                            val local = placement.toLocal(point)
                            val index = placement.page.objects.indexOfLast { it is SceneObject.TextBox && it.contains(local) }
                            if (index >= 0) {
                                val alreadySelected = selectedObjectPage == placement.index && selectedObject == index
                                selectedObjectPage = placement.index
                                selectedObject = index
                                stylusObjectPage = placement.index
                                stylusObjectIndex = index
                                stylusCanTransformObject = alreadySelected
                            } else {
                                val zIndex = (placement.page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
                                tab.beginSelectionUndo()
                                tab.recordObjectEdit(placement.page)
                                stylusObjectEditRecorded = true
                                placement.page.objects += SceneObject.TextBox("Text", local.x, local.y, 8_000f, zIndex = zIndex)
                                stylusObjectPage = placement.index
                                stylusObjectIndex = placement.page.objects.lastIndex
                                selectedObjectPage = placement.index
                                selectedObject = stylusObjectIndex
                                stylusCanTransformObject = false
                                showTextEditor = true
                            }
                            stylusResizingObject = false
                            tab.selectedPageIndex = placement.index
                            true
                        }
                        mode == EditorMode.Shape -> {
                            val local = placement.toLocal(point)
                            val selectedShape = placement.page.objects.getOrNull(selectedObject)
                                ?.takeIf { selectedObjectPage == placement.index } as? SceneObject.Rectangle
                            val selectedHandleHit = selectedShape?.resizeHandleContains(
                                local,
                                objectHandleRadius / viewport.scale,
                            ) == true
                            val shapeIndex = if (selectedHandleHit) selectedObject else {
                                placement.page.objects.indexOfLast {
                                    it is SceneObject.Rectangle && it.contains(local)
                                }
                            }
                            if (shapeIndex >= 0) {
                                val shape = placement.page.objects[shapeIndex] as SceneObject.Rectangle
                                val alreadySelected = selectedObjectPage == placement.index && selectedObject == shapeIndex
                                selectedObjectPage = placement.index
                                selectedObject = shapeIndex
                                tab.selectedPageIndex = placement.index
                                stylusObjectPage = placement.index
                                stylusObjectIndex = shapeIndex
                                stylusCanTransformObject = alreadySelected
                                stylusResizingObject = alreadySelected && selectedHandleHit
                                if (stylusCanTransformObject) {
                                    if (stylusResizingObject) {
                                        stylusShapeAnchor = PagePoint(shape.x, shape.y)
                                        stylusShapeCurrent = PagePoint(shape.x + shape.width, shape.y + shape.height)
                                    }
                                }
                            } else {
                                val zIndex = (placement.page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
                                tab.beginSelectionUndo()
                                tab.recordObjectEdit(placement.page)
                                stylusObjectEditRecorded = true
                                placement.page.objects += SceneObject.Rectangle(
                                    local.x,
                                    local.y,
                                    1f,
                                    1f,
                                    zIndex = zIndex,
                                )
                                stylusObjectPage = placement.index
                                stylusObjectIndex = placement.page.objects.lastIndex
                                selectedObjectPage = placement.index
                                selectedObject = stylusObjectIndex
                                tab.selectedPageIndex = placement.index
                                stylusCanTransformObject = true
                                stylusResizingObject = true
                                stylusShapeAnchor = local
                                stylusShapeCurrent = local
                            }
                            true
                        }
                        mode == EditorMode.Audio -> {
                            val localPoint = placement.toLocal(point)
                            val index = placement.page.objects.indexOfLast {
                                it is SceneObject.Audio && it.contains(localPoint)
                            }
                            if (index < 0) {
                                requestAudioRecording(point)
                                true
                            } else {
                                val alreadySelected = selectedObjectPage == placement.index && selectedObject == index
                                selectedObjectPage = placement.index
                                selectedObject = index
                                tab.selectedPageIndex = placement.index
                                stylusObjectPage = placement.index
                                stylusObjectIndex = index
                                stylusCanTransformObject = alreadySelected
                                stylusResizingObject = false
                                showAudioPlayer = true
                                true
                            }

                        }
                        else -> false
                    }
                }
                view.onObjectStylusDrag = { dx, dy ->
                    if (mode == EditorMode.Lasso) {
                        if (movingSelection) {
                            selectionDrag = PagePoint(selectionDrag.x + dx, selectionDrag.y + dy)
                        } else {
                            val previous = lassoPoints.lastOrNull()
                            if (previous != null) lassoPoints = lassoPoints + PagePoint(previous.x + dx, previous.y + dy)
                        }
                    } else {
                        val page = tab.pages.getOrNull(stylusObjectPage)
                        val item = page?.objects?.getOrNull(stylusObjectIndex)
                        if (page != null && item != null && stylusCanTransformObject) {
                            if (!stylusObjectEditRecorded && (dx != 0f || dy != 0f)) {
                                tab.recordObjectEdit(page)
                                stylusObjectEditRecorded = true
                            }
                            page.objects[stylusObjectIndex] = if (
                                stylusResizingObject && item is SceneObject.Rectangle && stylusShapeAnchor != null
                            ) {
                                val anchor = stylusShapeAnchor!!
                                val previous = stylusShapeCurrent ?: PagePoint(item.x + item.width, item.y + item.height)
                                val current = PagePoint(previous.x + dx, previous.y + dy)
                                stylusShapeCurrent = current
                                item.copy(
                                    x = minOf(anchor.x, current.x),
                                    y = minOf(anchor.y, current.y),
                                    width = kotlin.math.abs(current.x - anchor.x).coerceAtLeast(1f),
                                    height = kotlin.math.abs(current.y - anchor.y).coerceAtLeast(1f),
                                )
                            } else if (stylusResizingObject) {
                                val delta = if (item is SceneObject.Image) item.toUnrotatedDelta(dx, dy) else PagePoint(dx, dy)
                                if (item is SceneObject.Image) item.resizeFromLeftBottom(delta.x, delta.y)
                                else item.resize(delta.x, delta.y)
                            } else {
                                item.move(dx, dy)
                            }
                        }
                    }
                }
                view.onObjectStylusUp = {
                    if (mode == EditorMode.Lasso) {
                        if (movingSelection) {
                            if (selectionDrag.x != 0f || selectionDrag.y != 0f) transformMixedSelection(selectionDrag)
                        } else if (lassoPoints.size >= 3) {
                            val placement = layout.pageAt(lassoPoints.first())
                            if (placement != null && lassoPoints.all(placement::contains)) {
                                val polygon = lassoPoints.map(placement::toLocal)
                                val captureBounds = PageRect(
                                    polygon.minOf(PagePoint::x).coerceIn(0f, placement.page.size.widthMicrons),
                                    polygon.minOf(PagePoint::y).coerceIn(0f, placement.page.size.heightMicrons),
                                    polygon.maxOf(PagePoint::x).coerceIn(0f, placement.page.size.widthMicrons),
                                    polygon.maxOf(PagePoint::y).coerceIn(0f, placement.page.size.heightMicrons),
                                )
                                if (
                                    captureBounds.right - captureBounds.left >= 100f &&
                                    captureBounds.bottom - captureBounds.top >= 100f
                                ) {
                                    lassoCaptureRegion = LassoCaptureRegion(placement.index, captureBounds)
                                    tab.selectedPageIndex = placement.index
                                }
                                val strokes = LassoSelection.select(placement.page.session.page.strokes, polygon)
                                val objects = placement.page.objects.filter { item ->
                                    val center = when (item) {
                                        is SceneObject.Image -> PagePoint(item.x + item.width / 2f, item.y + item.height / 2f)
                                        is SceneObject.Rectangle -> PagePoint(item.x + item.width / 2f, item.y + item.height / 2f)
                                        is SceneObject.TextBox -> PagePoint(item.x + item.width / 2f, item.y + item.height / 2f)
                                        is SceneObject.Audio -> PagePoint(item.x + item.width / 2f, item.y + item.height / 2f)
                                    }
                                    LassoSelection.contains(polygon, center)
                                }
                                if (strokes.isNotEmpty() || objects.isNotEmpty()) {
                                    mixedSelection = MixedSelection(
                                        placement.index,
                                        strokes.mapTo(linkedSetOf(), InkStroke::id),
                                        objects.mapTo(linkedSetOf(), SceneObject::id),
                                    )
                                }
                            }
                        }
                        lassoPoints = emptyList()
                        movingSelection = false
                        selectionDrag = PagePoint(0f, 0f)
                    } else if (stylusObjectPage >= 0) tab.persistObjects()
                    stylusObjectPage = -1
                    stylusObjectIndex = -1
                    stylusResizingObject = false
                    stylusCanTransformObject = false
                    stylusObjectEditRecorded = false
                    stylusShapeAnchor = null
                    stylusShapeCurrent = null
                }
                view.onMotionEventObserved = tab.session::observeMotionEvent
                view.onStylusButtonPressed = onStylusButtonPressed
                view.onViewportChanged = ::updateViewport
                view.onOneFingerScroll = ::scrollDocument
                view.onCanvasTouchAt = { point ->
                    layout.pageAt(point)?.let { tab.selectedPageIndex = it.index }
                }
                view.onFingerObjectDown = fingerObjectDown@{ point ->
                    fingerObjectEditRecorded = false
                    val placement = layout.pageAt(point)
                    val activeTextSelection = textRangeSelection
                    if (activeTextSelection != null) {
                        val selectionPlacement = layout.pages.firstOrNull {
                            it.page.id == activeTextSelection.pageId
                        }
                        if (selectionPlacement != null) {
                            val local = selectionPlacement.toLocal(point)
                            val visualOffset = textHandleVisualOffset / viewport.scale
                            val hitRadius = objectHandleRadius / viewport.scale
                            val handleHit = TextSelectionHandle.entries
                                .map { handle ->
                                    val boundary = activeTextSelection.handlePosition(handle)
                                    val center = PagePoint(boundary.x, boundary.y + visualOffset)
                                    handle to hypot(local.x - center.x, local.y - center.y)
                                }
                                .minByOrNull { it.second }
                                ?.takeIf { it.second <= hitRadius }
                                ?.first
                            if (handleHit != null) {
                                draggedTextHandle = handleHit
                                draggedTextHandlePoint = local
                                return@fingerObjectDown FingerObjectGesture.Move
                            }
                            if (selectionPlacement.contains(point) && activeTextSelection.contains(
                                    local,
                                    textSelectionPadding / viewport.scale,
                                )
                            ) {
                                return@fingerObjectDown FingerObjectGesture.SelectOnly
                            }
                        }
                        return@fingerObjectDown FingerObjectGesture.Canvas
                    }
                    if (placement == null) {
                        fingerResizingImage = false
                        FingerObjectGesture.Canvas
                    } else {
                        val local = placement.toLocal(point)
                        val activeMixedSelection = mixedSelection
                        if (activeMixedSelection != null) {
                            val bounds = mixedSelectionBounds(activeMixedSelection, tab.pages)
                            val inside = activeMixedSelection.pageIndex == placement.index && bounds != null &&
                                local.x in bounds.left..bounds.right && local.y in bounds.top..bounds.bottom
                            movingSelection = inside
                            selectionDrag = PagePoint(0f, 0f)
                            if (inside) FingerObjectGesture.Move else FingerObjectGesture.Canvas
                        } else if (mode == EditorMode.Image) {
                            FingerObjectGesture.Canvas
                        } else if (mode == EditorMode.Text) {
                            val textIndex = placement.page.objects.indexOfLast { it is SceneObject.TextBox && it.contains(local) }
                            val alreadySelected = textIndex >= 0 &&
                                selectedObjectPage == placement.index && selectedObject == textIndex
                            if (alreadySelected) {
                                FingerObjectGesture.Move
                            } else FingerObjectGesture.Canvas
                        } else {
                            val item = placement.page.objects.getOrNull(selectedObject)
                                ?.takeIf { selectedObjectPage == placement.index }
                            if (item == null || !item.contains(local) && !item.resizeHandleContains(
                                    local,
                                    objectHandleRadius / viewport.scale,
                                )
                            ) {
                                fingerResizingImage = false
                                FingerObjectGesture.Canvas
                            } else {
                                val image = item as? SceneObject.Image
                                fingerResizingImage = image != null && !image.locked && image.resizeHandleContains(
                                    local, objectHandleRadius / viewport.scale,
                                )
                                if (image?.locked == true) FingerObjectGesture.SelectOnly else {
                                    FingerObjectGesture.Move
                                }
                            }
                        }
                    }
                }
                view.onFingerObjectDrag = { dx, dy ->
                    val textHandle = draggedTextHandle
                    val activeTextSelection = textRangeSelection
                    val previousTextPoint = draggedTextHandlePoint
                    if (textHandle != null && activeTextSelection != null && previousTextPoint != null) {
                        val current = PagePoint(previousTextPoint.x + dx, previousTextPoint.y + dy)
                        draggedTextHandlePoint = current
                        nearestRecognizedTokenIndex(activeTextSelection.tokens, current)?.let { tokenIndex ->
                            val previousIndex = when (textHandle) {
                                TextSelectionHandle.Start -> activeTextSelection.startIndex
                                TextSelectionHandle.End -> activeTextSelection.endIndex
                            }
                            if (tokenIndex != previousIndex) {
                                textRangeSelection = activeTextSelection.move(textHandle, tokenIndex)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    } else if (movingSelection && mixedSelection != null) {
                        selectionDrag = PagePoint(selectionDrag.x + dx, selectionDrag.y + dy)
                    } else {
                        val page = tab.pages.getOrNull(selectedObjectPage)
                        val item = page?.objects?.getOrNull(selectedObject)
                        if (page != null && item != null && (item !is SceneObject.Image || !item.locked)) {
                            if (!fingerObjectEditRecorded && (dx != 0f || dy != 0f)) {
                                tab.recordObjectEdit(page)
                                fingerObjectEditRecorded = true
                            }
                            page.objects[selectedObject] = if (fingerResizingImage && item is SceneObject.Image) {
                                val delta = item.toUnrotatedDelta(dx, dy)
                                item.resizeFromLeftBottom(delta.x, delta.y)
                            } else item.move(dx, dy)
                        }
                    }
                }
                view.onFingerObjectUp = {
                    if (draggedTextHandle != null) {
                        draggedTextHandle = null
                        draggedTextHandlePoint = null
                    } else if (movingSelection && mixedSelection != null) {
                        if (selectionDrag.x != 0f || selectionDrag.y != 0f) transformMixedSelection(selectionDrag)
                        selectionDrag = PagePoint(0f, 0f)
                        movingSelection = false
                    } else if (selectedSceneObject() != null) tab.persistObjects()
                    fingerResizingImage = false
                    fingerObjectEditRecorded = false
                }
                view.onFingerTap = fingerTap@{ point ->
                    if (radialMenuCenter != null) {
                        radialMenuCenter = null
                        radialMenuHighlighted = -1
                        return@fingerTap
                    }
                    textRangeSelection?.let { activeTextSelection ->
                        val activePlacement = layout.pages.firstOrNull {
                            it.page.id == activeTextSelection.pageId
                        }
                        val insideSelection = activePlacement?.toLocal(point)?.let { local ->
                            val padding = objectHandleRadius / viewport.scale
                            val overText = activeTextSelection.contains(local, padding)
                            val overHandle = TextSelectionHandle.entries.any { handle ->
                                val boundary = activeTextSelection.handlePosition(handle)
                                val center = PagePoint(
                                    boundary.x,
                                    boundary.y + textHandleVisualOffset / viewport.scale,
                                )
                                hypot(local.x - center.x, local.y - center.y) <= padding
                            }
                            overText || overHandle
                        } == true
                        if (!insideSelection) {
                            textRangeSelection = null
                            draggedTextHandle = null
                            draggedTextHandlePoint = null
                        }
                        return@fingerTap
                    }
                    val placement = layout.pageAt(point) ?: return@fingerTap
                    val local = placement.toLocal(point)
                    if (mixedSelection != null) return@fingerTap
                    when (mode) {
                        EditorMode.Image -> {
                            tab.selectedPageIndex = placement.index
                            onImagePlacementRequested(placement.page.id, local)
                        }
                        EditorMode.Text -> {
                            val textIndex = placement.page.objects.indexOfLast {
                                it is SceneObject.TextBox && it.contains(local)
                            }
                            if (textIndex >= 0) {
                                selectedObjectPage = placement.index
                                selectedObject = textIndex
                                tab.selectedPageIndex = placement.index
                                showAudioPlayer = false
                            } else if (selectedObject >= 0) {
                                selectedObject = -1
                                selectedObjectPage = -1
                                showTextEditor = false
                            } else {
                                val zIndex = (placement.page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L
                                tab.beginSelectionUndo()
                                tab.recordObjectEdit(placement.page)
                                placement.page.objects += SceneObject.TextBox("Text", local.x, local.y, 8_000f, zIndex = zIndex)
                                selectedObjectPage = placement.index
                                selectedObject = placement.page.objects.lastIndex
                                tab.selectedPageIndex = placement.index
                                showTextEditor = true
                                tab.persistObjects()
                            }
                        }
                        EditorMode.Audio -> {
                            val audioIndex = placement.page.objects.indexOfLast {
                                it is SceneObject.Audio && it.contains(local)
                            }
                            if (audioIndex >= 0) {
                                selectedObjectPage = placement.index
                                selectedObject = audioIndex
                                tab.selectedPageIndex = placement.index
                                showAudioPlayer = true
                            } else if (selectedObject >= 0) {
                                selectedObject = -1
                                selectedObjectPage = -1
                                showAudioPlayer = false
                            } else requestAudioRecording(point)
                        }
                        else -> {
                            val candidateIndex = placement.page.objects.indexOfLast { item ->
                                (item is SceneObject.Image || item is SceneObject.Audio ||
                                    item is SceneObject.TextBox || item is SceneObject.Rectangle) && item.contains(local)
                            }
                            if (candidateIndex >= 0) {
                                val item = placement.page.objects[candidateIndex]
                                selectedObjectPage = placement.index
                                selectedObject = candidateIndex
                                tab.selectedPageIndex = placement.index
                                showAudioPlayer = (item is SceneObject.Audio)
                            } else {
                                selectedObject = -1
                                selectedObjectPage = -1
                                showAudioPlayer = false
                            }
                        }

                    }
                }
                view.onFingerDoubleTap = doubleTap@{ point ->
                    if (copiedImage == null && selectionClipboard == null) return@doubleTap
                    val placement = layout.pageAt(point) ?: return@doubleTap
                    clipboardPasteTargetPage = placement.index
                    tab.selectedPageIndex = placement.index
                    mixedSelection = null
                    lassoCaptureRegion = null
                    selectedObject = -1
                    selectedObjectPage = -1
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                view.onFingerLongPress = longPress@{ point ->
                    val placement = layout.pageAt(point) ?: return@longPress
                    beginPdfTextSelection(placement.page, placement.toLocal(point))
                }
                view.onStylusHover = { sample ->
                    lastStylusHover = sample
                    if (radialMenuCenter != null) {
                        updateRadialHighlight(sample)
                    } else if (appSettings.hoverShortcutEnabled && hoverRecognizer.add(sample)) {
                        openRadialMenu(Offset(sample.x, sample.y))
                    }
                }
                view.onStylusHoverExit = hoverRecognizer::onHoverExit
                view.onStrokeFinished = { finished ->
                    layout.pageAt(finished.samples.first().position)?.let { placement ->
                        tab.selectedPageIndex = placement.index
                        val localized = finished.copy(
                            samples = finished.samples.map { sample ->
                                sample.copy(position = placement.toLocal(sample.position))
                            },
                        )
                        val corrected = if (
                            drawAndHoldEnabled &&
                            localized.style.tool in setOf(InkTool.Pen, InkTool.Pencil, InkTool.Highlighter)
                        ) DrawHoldShapeDetector.detect(
                            localized.samples,
                            linesOnly = localized.style.tool == InkTool.Highlighter,
                        ) else null
                        if (corrected != null) {
                            addCorrectedShape(placement.page, corrected, localized.style)
                            view.removeFinishedStroke(localized.id)
                        } else if (placement.page.session.eraseScribbleIfDetected(
                                localized.samples,
                                localized.style,
                            )
                        ) {
                            view.removeFinishedStroke(localized.id)
                        } else {
                            if (pendingInk.any { it.page !== placement.page }) commitPendingInk()
                            val pending = PendingInkStroke(
                                page = placement.page,
                                nativeStroke = localized,
                                inputOriginX = placement.left,
                                inputOriginY = placement.top,
                            )
                            pendingInk += pending
                            syncWetInkUndoAvailability()
                            // Display pending Action Map ink in the viewport-aware renderer. The native
                            // wet layer uses release-time screen coordinates and otherwise floats while panning.
                            view.removeFinishedStroke(localized.id)
                            actionRecognitionJob?.cancel()
                            if (actionMappings.isEmpty() || actionActivationMode == ActionActivationMode.Disabled || !localized.actionCandidate) {
                                commitPendingInk(listOf(pending))
                            } else {
                                val pendingStrokeCount = pendingInk.size
                                val mappedStrokeCounts = actionMappings.flatMap { mapping ->
                                    mapping.examples.map { it.strokes.size }
                                }
                                if (pendingStrokeCount in mappedStrokeCounts && mappedStrokeCounts.none { it > pendingStrokeCount }) {
                                    resolvePendingInk()
                                } else {
                                    actionRecognitionJob = actionScope.launch {
                                        // Keep the gesture batch open long enough for deliberate multi-stroke maps.
                                        delay(1_300)
                                        resolvePendingInk()
                                    }
                                }
                            }
                        }
                    } ?: view.removeFinishedStroke(finished.id)
                    finishDeferredWetUndo()
                }
                view.onStrokeProgress = { _, samples, style, _ ->
                    val placement = samples.firstOrNull()?.position?.let(layout::pageAt)
                    val corrected = if (
                        placement != null && drawAndHoldEnabled &&
                        style.tool in setOf(InkTool.Pen, InkTool.Pencil, InkTool.Highlighter)
                    ) {
                        DrawHoldShapeDetector.detect(
                            samples.map { it.copy(position = placement.toLocal(it.position)) },
                            linesOnly = style.tool == InkTool.Highlighter,
                        )
                    } else null
                    if (placement != null && corrected != null) {
                        addCorrectedShape(placement.page, corrected, style)
                        finishDeferredWetUndo()
                        true
                    } else false
                }
                view.onUncommittedInkChanged = ::syncWetInkUndoAvailability
                view.onErase = { point ->
                    if (!eraserHistoryActive) {
                        tab.beginHistoryTransaction()
                        eraserHistoryActive = true
                    }
                    layout.pageAt(point)?.let { placement ->
                        tab.selectedPageIndex = placement.index
                        val local = placement.toLocal(point)
                        placement.page.session.eraseAt(local)
                        val eraserRadius = (tab.session.currentStyle().maximumWidth / 2f).coerceAtLeast(2_500f)
                        val shapeIndex = placement.page.objects.indexOfLast { item ->
                            item is SceneObject.Rectangle && item.hitByEraser(local, eraserRadius)
                        }
                        if (shapeIndex >= 0) {
                            if (!shapeEraseUndoRecorded) {
                                tab.recordObjectEdit(placement.page)
                                shapeEraseUndoRecorded = true
                            }
                            placement.page.objects.removeAt(shapeIndex)
                            if (selectedObjectPage == placement.index && selectedObject == shapeIndex) {
                                selectedObject = -1
                                selectedObjectPage = -1
                            }
                            tab.persistObjects()
                        }
                    }
                }
                view.onEraseFinished = {
                    shapeEraseUndoRecorded = false
                    if (eraserHistoryActive) {
                        tab.endHistoryTransaction()
                        eraserHistoryActive = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer { clip = true },
        )
        textRangeSelection?.let { selection ->
            val placement = layout.pages.firstOrNull { it.page.id == selection.pageId }
            if (placement != null) Canvas(Modifier.fillMaxSize().zIndex(34f)) {
                val cornerRadius = 2.dp.toPx()
                val handleRadius = 6.dp.toPx()
                val handleStrokeWidth = 2.dp.toPx()
                selection.selectedLineBounds().forEach { bounds ->
                    val topLeftPoint = viewport.toScreen(PagePoint(
                        placement.left + bounds.left,
                        placement.top + bounds.top,
                    ))
                    val bottomRightPoint = viewport.toScreen(PagePoint(
                        placement.left + bounds.right,
                        placement.top + bounds.bottom,
                    ))
                    val topLeft = Offset(topLeftPoint.x, topLeftPoint.y)
                    val bottomRight = Offset(bottomRightPoint.x, bottomRightPoint.y)
                    drawRoundRect(
                        color = selectionColor.copy(alpha = .28f),
                        topLeft = topLeft,
                        size = Size(
                            (bottomRight.x - topLeft.x).coerceAtLeast(1f),
                            (bottomRight.y - topLeft.y).coerceAtLeast(1f),
                        ),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    )
                }
                TextSelectionHandle.entries.forEach { handle ->
                    val boundary = selection.handlePosition(handle)
                    val boundaryPoint = viewport.toScreen(PagePoint(
                        placement.left + boundary.x,
                        placement.top + boundary.y,
                    ))
                    val boundaryScreen = Offset(boundaryPoint.x, boundaryPoint.y)
                    val center = Offset(boundaryScreen.x, boundaryScreen.y + textHandleVisualOffset)
                    drawLine(selectionColor, boundaryScreen, center, handleStrokeWidth)
                    drawCircle(Color.White, handleRadius + handleStrokeWidth, center)
                    drawCircle(selectionColor, handleRadius, center)
                }
            }
        }
        radialMenuCenter?.let { center ->
            RadialToolWheel(
                center = center,
                items = radialTools,
                highlightedIndex = radialMenuHighlighted,
                modifier = Modifier.fillMaxSize().zIndex(90f),
            )
        }
        if (lassoPoints.isNotEmpty() || mixedSelection != null || lassoCaptureRegion != null) {
            Canvas(Modifier.fillMaxSize().zIndex(30f)) {
                if (lassoPoints.size >= 2) {
                    val path = Path()
                    lassoPoints.forEachIndexed { index, point ->
                        val screen = viewport.toScreen(point)
                        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
                    }
                    drawPath(path, selectionColor, style = Stroke(2.dp.toPx()))
                }
                mixedSelection?.let { selection ->
                    val placement = layout.pages.getOrNull(selection.pageIndex)
                    val bounds = mixedSelectionBounds(selection, tab.pages)
                    if (placement != null && bounds != null) {
                        if (movingSelection) {
                            placement.page.session.page.strokes.filter { it.id in selection.strokeIds }
                                .inInkRenderOrder().forEach { stroke ->
                                val path = Path()
                                stroke.samples.forEachIndexed { index, sample ->
                                    val screen = viewport.toScreen(PagePoint(
                                        placement.left + sample.position.x + selectionDrag.x,
                                        placement.top + sample.position.y + selectionDrag.y,
                                    ))
                                    if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
                                }
                                drawPath(
                                    path,
                                    Color(stroke.style.argb).copy(alpha = stroke.style.opacity),
                                    style = Stroke((stroke.style.maximumWidth * viewport.scale).coerceAtLeast(1f), cap = StrokeCap.Round),
                                )
                            }
                        }
                        val left = viewport.offsetX + (placement.left + bounds.left + selectionDrag.x) * viewport.scale
                        val top = viewport.offsetY + (placement.top + bounds.top + selectionDrag.y) * viewport.scale
                        val width = (bounds.right - bounds.left) * viewport.scale
                        val height = (bounds.bottom - bounds.top) * viewport.scale
                        drawRect(selectionColor.copy(alpha = .08f), Offset(left, top), Size(width, height))
                        drawRect(selectionColor, Offset(left, top), Size(width, height), style = Stroke(2.dp.toPx()))
                    }
                }
                if (mixedSelection == null) lassoCaptureRegion?.let { region ->
                    val placement = layout.pages.getOrNull(region.pageIndex)
                    if (placement != null) {
                        val left = viewport.offsetX + (placement.left + region.bounds.left) * viewport.scale
                        val top = viewport.offsetY + (placement.top + region.bounds.top) * viewport.scale
                        val width = (region.bounds.right - region.bounds.left) * viewport.scale
                        val height = (region.bounds.bottom - region.bounds.top) * viewport.scale
                        drawRect(selectionColor.copy(alpha = .08f), Offset(left, top), Size(width, height))
                        drawRect(selectionColor, Offset(left, top), Size(width, height), style = Stroke(2.dp.toPx()))
                    }
                }
            }
        }
        textRangeSelection?.let { selection ->
            TextRangeSelectionBar(
                languageName = selection.languageName,
                onSelectAll = { textRangeSelection = selection.selectAll() },
                onCopy = {
                    val text = selection.selectedText()
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as
                        android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Selected PDF text", text))
                    actionFeedback = "Copied selected text"
                },
                onDismiss = {
                    textRangeSelection = null
                    draggedTextHandle = null
                    draggedTextHandlePoint = null
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(48f),
            )
        }
        mixedSelection?.let { selection ->
            MixedSelectionBar(
                hasClipboard = selectionClipboard != null,
                canMovePrevious = selection.pageIndex > 0,
                canMoveNext = selection.pageIndex < tab.pages.lastIndex,
                onScreenshot = ::captureLassoScreenshot,
                onCut = { copyMixedSelection(); deleteMixedSelection() },
                onCopy = { copyMixedSelection() },
                onPaste = { pasteMixedSelection(selection.pageIndex) },
                onDuplicate = { copyMixedSelection(); pasteMixedSelection(selection.pageIndex) },
                onDelete = { confirmMixedDelete = true },
                onRecolor = {
                    val page = tab.pages.getOrNull(selection.pageIndex) ?: return@MixedSelectionBar
                    val color = tab.session.currentStyle().argb
                    val nextIds = page.session.replaceStrokes(selection.strokeIds) { stroke -> stroke.copy(style = stroke.style.copy(argb = color)) }
                    page.syncRuntimeStrokes()
                    mixedSelection = selection.copy(strokeIds = nextIds.ifEmpty { selection.strokeIds })
                },
                onWidth = {
                    val page = tab.pages.getOrNull(selection.pageIndex) ?: return@MixedSelectionBar
                    val width = tab.session.toolWidthMicrons(tab.session.selectedTool)
                    val nextIds = page.session.replaceStrokes(selection.strokeIds) { stroke ->
                        val ratio = if (stroke.style.maximumWidth > 0f) stroke.style.minimumWidth / stroke.style.maximumWidth else 1f
                        stroke.copy(style = stroke.style.copy(minimumWidth = width * ratio, maximumWidth = width))
                    }
                    page.syncRuntimeStrokes()
                    mixedSelection = selection.copy(strokeIds = nextIds.ifEmpty { selection.strokeIds })
                },
                onRotate = { transformMixedSelection(rotationRadians = (Math.PI / 2.0).toFloat()) },
                onUndo = tab::undo,
                onRedo = tab::redo,
                onMovePage = { direction ->
                    val source = tab.pages.getOrNull(selection.pageIndex) ?: return@MixedSelectionBar
                    val targetIndex = (selection.pageIndex + direction).coerceIn(0, tab.pages.lastIndex)
                    if (targetIndex != selection.pageIndex) {
                        tab.historyTransaction {
                            copyMixedSelection()
                            deleteMixedSelection()
                            tab.selectedPageIndex = targetIndex
                            pasteMixedSelection(targetIndex)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(46f),
            )
        }
        if (mixedSelection == null && lassoCaptureRegion != null) {
            LassoCaptureBar(
                onScreenshot = ::captureLassoScreenshot,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(46f),
            )
        }
        if (mixedSelection == null) selectedImage()?.let { (_, image) ->
            ImageSelectionBar(
                locked = image.locked,
                canPaste = copiedImage != null,
                onCrop = { showCropEditor = true },
                onCut = { removeSelectedImage(copyFirst = true) },
                onCopy = {
                    copiedImage = image
                    selectionClipboard = null
                },
                onPaste = { pasteCopiedImage(selectedObjectPage) },
                onLock = { replaceSelectedImage { it.copy(locked = true) } },
                onUnlock = { replaceSelectedImage { it.copy(locked = false) } },
                onRotate = { replaceSelectedImage { it.copy(rotation = (it.rotation + 90f) % 360f) } },
                onRecognizeText = { selectedImage()?.second?.let(::recognizeImageText) },
                onBringToFront = { moveSelectedImageToFront(front = true) },
                onSendToBack = { moveSelectedImageToFront(front = false) },
                onDelete = { confirmSelectedDelete = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(45f),
            )
        }
        if (mixedSelection == null && !showTextEditor && !showShapeEditor) selectedSceneObject()
            ?.takeIf { it.second !is SceneObject.Image && it.second !is SceneObject.Audio }
            ?.let { (_, item) ->
            ObjectSelectionBar(
                onEdit = { if (item is SceneObject.TextBox) showTextEditor = true else showShapeEditor = true },
                onCopy = {
                    copiedImage = null
                    selectionClipboard = SelectionClipboard(emptyList(), listOf(item))
                },
                onDuplicate = {
                    val page = tab.pages.getOrNull(selectedObjectPage) ?: return@ObjectSelectionBar
                    tab.recordObjectEdit(page)
                    page.objects += item.duplicate().move(5_000f, 5_000f)
                        .withZIndex((page.objects.maxOfOrNull(SceneObject::zIndex) ?: -1L) + 1L)
                    selectedObject = page.objects.lastIndex
                    tab.persistObjects()
                },
                onRotate = { replaceSelectedObject { it.rotateBy(15f) } },
                onFront = { moveSelectedObjectToFront(true) },
                onBack = { moveSelectedObjectToFront(false) },
                onDelete = { confirmSelectedDelete = true },
                onUndo = tab::undo,
                onRedo = tab::redo,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(45f),
            )
        }
        if (
            mixedSelection == null && lassoCaptureRegion == null && selectedObject < 0 &&
            clipboardPasteTargetPage >= 0 && (copiedImage != null || selectionClipboard != null)
        ) {
            ClipboardPasteBar(
                onPaste = {
                    if (copiedImage != null) pasteCopiedImage(clipboardPasteTargetPage)
                    else pasteMixedSelection(clipboardPasteTargetPage)
                },
                onDismiss = { clipboardPasteTargetPage = -1 },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(45f),
            )
        }
        if (mixedSelection == null) (selectedSceneObject()?.second as? SceneObject.Audio)?.let { audio ->
            AudioSelectionBar(
                onListen = { showAudioPlayer = true },
                onDelete = { confirmSelectedDelete = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).zIndex(45f),
            )
            if (showAudioPlayer) AudioNotePanel(
                audio = audio,
                referenceShortSide = (minOf(viewportWidth, viewportHeight).coerceAtLeast(1) / viewport.scale),
                onScale = { diameter ->
                    replaceSelectedObject {
                        val current = it as SceneObject.Audio
                        current.copy(
                            x = current.x + (current.width - diameter) / 2f,
                            y = current.y + (current.height - diameter) / 2f,
                            width = diameter,
                            height = diameter,
                        )
                    }
                },
                onUpdateTranscript = { newTranscript ->
                    replaceSelectedObject {
                        val current = it as SceneObject.Audio
                        current.copy(transcript = newTranscript)
                    }
                },
                onDelete = ::removeSelectedObject,
                onDismiss = { showAudioPlayer = false },
            )

        }
        if (confirmSelectedDelete) AlertDialog(
            onDismissRequest = { confirmSelectedDelete = false },
            title = { Text("Delete selected item?") },
            text = { Text("The selected item will be removed from this page.") },
            dismissButton = { TextButton(onClick = { confirmSelectedDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmSelectedDelete = false; removeSelectedObject() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
        if (confirmMixedDelete) AlertDialog(
            onDismissRequest = { confirmMixedDelete = false },
            title = { Text("Delete selection?") },
            text = { Text("All selected ink and objects will be removed from this page.") },
            dismissButton = { TextButton(onClick = { confirmMixedDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmMixedDelete = false; deleteMixedSelection() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
        if (showCropEditor) {
            selectedImage()?.second?.let { image ->
                ImageCropDialog(
                    image = image,
                    onApply = { crop ->
                        replaceSelectedImage { current ->
                            val croppedWidth = current.bitmap.width *
                                (1f - crop.leftFraction - crop.rightFraction)
                            val croppedHeight = current.bitmap.height *
                                (1f - crop.topFraction - crop.bottomFraction)
                            val croppedAspectRatio = croppedWidth / croppedHeight.coerceAtLeast(1f)
                            val nextHeight = (current.width / croppedAspectRatio).coerceAtLeast(5_000f)
                            current.copy(
                                y = current.y + (current.height - nextHeight) / 2f,
                                height = nextHeight,
                                crop = crop,
                            )
                        }
                        showCropEditor = false
                    },
                    onDismiss = { showCropEditor = false },
                )
            } ?: run { showCropEditor = false }
        }
        if (showTextEditor) {
            (selectedSceneObject()?.second as? SceneObject.TextBox)?.let { textBox ->
                TextObjectDialog(
                    value = textBox,
                    customFonts = customFonts,
                    onApply = { updated -> replaceSelectedObject { updated }; showTextEditor = false },
                    onDismiss = { showTextEditor = false },
                )
            } ?: run { showTextEditor = false }
        }
        if (showShapeEditor) {
            (selectedSceneObject()?.second as? SceneObject.Rectangle)?.let { shape ->
                ShapeObjectDialog(
                    value = shape,
                    onApply = { updated -> replaceSelectedObject { updated }; showShapeEditor = false },
                    onDismiss = { showShapeEditor = false },
                )
            } ?: run { showShapeEditor = false }
        }
        strokeSizePreview?.let { preview ->
            StrokeSizePreviewOverlay(
                preview = preview,
                viewportScale = viewport.scale,
                modifier = Modifier.align(Alignment.Center).zIndex(35f),
            )
        }
        if (ocrBusy) {
            Surface(
                modifier = Modifier.align(Alignment.Center).zIndex(60f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF020242B),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Reading text…")
                }
            }
        }
        ocrText?.let { result ->
            OcrResultDialog(
                text = result,
                title = ocrTitle,
                detail = ocrDetail,
                onDismiss = { ocrText = null },
            )
        }
        if (lassoScreenshotBusy) {
            Surface(
                modifier = Modifier.align(Alignment.Center).zIndex(70f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF020242B),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Creating screenshot…")
                }
            }
        }
        lassoScreenshot?.let { screenshot ->
            LassoScreenshotDialog(
                bitmap = screenshot.bitmap,
                onCancel = {
                    screenshot.bitmap.takeIf { !it.isRecycled }?.recycle()
                    lassoScreenshot = null
                },
                onSave = {
                    pendingLassoScreenshotSave = screenshot.bitmap
                    saveLassoScreenshot.launch(
                        normalizedExportFileName("${tab.title}-screenshot", ExportFormat.Image),
                    )
                },
                onInsert = ::insertLassoScreenshot,
            )
        }
        lassoScreenshotError?.let { message ->
            AlertDialog(
                onDismissRequest = { lassoScreenshotError = null },
                title = { Text("Lasso screenshot") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { lassoScreenshotError = null }) { Text("OK") } },
            )
        }
        activeAudioRecording?.let { recording ->
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).zIndex(70f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF02A2023),
                shadowElevation = 10.dp,
            ) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFFEF4444)))
                    Spacer(Modifier.width(10.dp))
                    Text("Recording audio note")
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = ::stopAudioRecording) {
                        Icon(Icons.Outlined.Stop, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Stop")
                    }
                }
            }
        }
        audioError?.let { message ->
            AlertDialog(
                onDismissRequest = { audioError = null },
                title = { Text("Audio note") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { audioError = null }) { Text("OK") } },
            )
        }
        actionFeedback?.let { message ->
            LaunchedEffect(message) {
                delay(1_800)
                actionFeedback = null
            }
            Surface(
                color = Color(0xEE20242B),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).zIndex(40f),
            ) { Text(message, Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
        }
        PageScrollBadge(
            currentPage = tab.selectedPageIndex,
            totalPages = tab.pages.size,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        )
        ViewportInfoPill(
            zoomScale = viewport.scale,
            currentPageIndex = tab.selectedPageIndex,
            totalPages = tab.pages.size,
            onZoomIn = {
                val nextScale = (viewport.scale * 1.25f).coerceIn(minimumScale, minimumScale * 8f)
                val cx = viewportWidth / 2f
                val cy = viewportHeight / 2f
                updateViewport(
                    viewport.copy(
                        scale = nextScale,
                        offsetX = cx - (cx - viewport.offsetX) * (nextScale / viewport.scale),
                        offsetY = cy - (cy - viewport.offsetY) * (nextScale / viewport.scale),
                    ),
                )
            },
            onZoomOut = {
                val nextScale = (viewport.scale / 1.25f).coerceIn(minimumScale, minimumScale * 8f)
                val cx = viewportWidth / 2f
                val cy = viewportHeight / 2f
                updateViewport(
                    viewport.copy(
                        scale = nextScale,
                        offsetX = cx - (cx - viewport.offsetX) * (nextScale / viewport.scale),
                        offsetY = cy - (cy - viewport.offsetY) * (nextScale / viewport.scale),
                    ),
                )
            },
            onFitWidth = {
                val page = tab.pages.getOrNull(tab.selectedPageIndex)
                if (page != null && viewportWidth > 10) {
                    val targetScale = (viewportWidth * 0.94f / page.size.widthMicrons).coerceIn(minimumScale, minimumScale * 8f)
                    updateViewport(
                        viewport.copy(
                            scale = targetScale,
                            offsetX = (viewportWidth - page.size.widthMicrons * targetScale) / 2f,
                        ),
                    )
                }
            },
            onFitPage = { tab.resetViewport() },
            onResetZoom = {
                val targetScale = minimumScale
                updateViewport(viewport.copy(scale = targetScale, offsetX = 0f, offsetY = 0f))
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun LassoScreenshotDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onInsert: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.78f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Text("Screenshot preview", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "The rectangular area inside the lasso bounds.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 14.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                        val scale = minOf(
                            size.width / bitmap.width.coerceAtLeast(1),
                            size.height / bitmap.height.coerceAtLeast(1),
                        )
                        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstOffset = IntOffset(
                                ((size.width - width) / 2f).roundToInt(),
                                ((size.height - height) / 2f).roundToInt(),
                            ),
                            dstSize = IntSize(width, height),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onSave) { Text("Save") }
                    TextButton(onClick = onInsert) { Text("Insert") }
                }
            }
        }
    }
}

@Composable
private fun RadialToolWheel(
    center: Offset,
    items: List<RadialToolItem>,
    highlightedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val diameter = 300.dp
    val diameterPx = with(density) { diameter.toPx() }
    val radius = diameterPx / 2f
    val ambientPrimary = MaterialTheme.colorScheme.primary
    val reveal = remember(center) { Animatable(0f) }
    LaunchedEffect(center) { reveal.animateTo(1f, tween(330, easing = BackOutEasing)) }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().blur((8.dp * reveal.value).coerceAtLeast(0.dp))) {
            drawCircle(Color.Black.copy(alpha = .28f * reveal.value), radius * 1.35f, center)
            drawCircle(ambientPrimary.copy(alpha = .14f * reveal.value), radius * 1.15f, center)
        }
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (center.x - diameterPx / 2f).roundToInt(),
                        (center.y - diameterPx / 2f).roundToInt(),
                    )
                }
                .size(diameter)
                .graphicsLayer {
                    val scale = .62f + reveal.value * .38f
                    scaleX = scale
                    scaleY = scale
                    alpha = reveal.value
                    rotationZ = (1f - reveal.value) * -10f
                },
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            val outline = MaterialTheme.colorScheme.outline
            Canvas(Modifier.fillMaxSize()) {
                val wheelCenter = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f
                val innerRadius = outerRadius * .31f
                val bounds = Rect(
                    wheelCenter.x - outerRadius,
                    wheelCenter.y - outerRadius,
                    wheelCenter.x + outerRadius,
                    wheelCenter.y + outerRadius,
                )
                items.indices.forEach { index ->
                    val centerDegrees = -90f + index * (360f / items.size)
                    val startDegrees = centerDegrees - (180f / items.size)
                    val path = Path().apply {
                        moveTo(wheelCenter.x, wheelCenter.y)
                        arcTo(bounds, startDegrees, 360f / items.size, false)
                        close()
                    }
                    drawPath(
                        path,
                        if (index == highlightedIndex) primary.copy(alpha = .94f) else surface.copy(alpha = .92f),
                    )
                    drawPath(
                        path,
                        if (index == highlightedIndex) Color.White.copy(alpha = 0.8f) else outline.copy(alpha = .5f),
                        style = Stroke(if (index == highlightedIndex) 2.dp.toPx() else 1.2.dp.toPx()),
                    )
                }
                drawCircle(Color(0xE61A1F28), innerRadius, wheelCenter)
                drawCircle(primary.copy(alpha = .72f), innerRadius, wheelCenter, style = Stroke(2.dp.toPx()))
                drawCircle(primary.copy(alpha = .22f), outerRadius * 1.04f, wheelCenter, style = Stroke(8.dp.toPx()))
            }
            items.forEachIndexed { index, item ->
                val angle = Math.toRadians((-90.0 + index * 360.0 / items.size))
                val iconRadius = radius * .67f
                val selected = index == highlightedIndex
                val iconScale by animateFloatAsState(if (selected) 1.20f else 1f, tween(110), label = "radial-icon")
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (radius + kotlin.math.cos(angle).toFloat() * iconRadius - 23.dp.toPx()).roundToInt(),
                                (radius + kotlin.math.sin(angle).toFloat() * iconRadius - 23.dp.toPx()).roundToInt(),
                            )
                        }
                        .size(46.dp)
                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon,
                        item.label,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(
                Modifier.align(Alignment.Center).width(88.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    items.getOrNull(highlightedIndex)?.label ?: "Tools",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    if (highlightedIndex >= 0) "Tap to choose" else "Hover",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = .65f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StrokeSizePreviewOverlay(
    preview: StrokeSizePreview,
    viewportScale: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(preview.tool) {
        entrance.snapTo(0f)
        entrance.animateTo(1f, tween(durationMillis = 320, easing = BackOutEasing))
    }
    val maximumDiameter = with(density) { 72.dp.toPx() }
    val diameterPx = (preview.widthMicrons * viewportScale).coerceIn(3f, maximumDiameter)
    val diameterDp = with(density) { diameterPx.toDp() }
    val sizeMillimeters = preview.widthMicrons / 1_000f
    val sizeLabel = if (sizeMillimeters < 10f) {
        "${(sizeMillimeters * 10f).roundToInt() / 10f} mm"
    } else {
        "${sizeMillimeters.roundToInt()} mm"
    }
    val previewColor = when (preview.tool) {
        InkTool.Eraser -> Color(0xFFF3F5F8)
        InkTool.Highlighter -> Color(0x88F7D34A)
        InkTool.Pencil -> Color(0xFF59636F)
        InkTool.Pen -> Color(0xFF17212B)
    }
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = entrance.value.coerceIn(0f, 1f)
            scaleX = .82f + .18f * entrance.value
            scaleY = .82f + .18f * entrance.value
            translationY = with(density) { 12.dp.toPx() } * (1f - entrance.value)
        },
        color = Color(0xE61B1F26),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(diameterDp.coerceAtLeast(3.dp))) {
                    drawCircle(
                        color = previewColor,
                        radius = size.minDimension / 2f,
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = .8f),
                        radius = (size.minDimension / 2f - 1f).coerceAtLeast(1f),
                        style = Stroke(width = 2f),
                    )
                }
            }
            Text(
                "${preview.tool.name} · $sizeLabel",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
            Text("Drag vertically", color = Color(0xFFB9C0CA), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ExportFileDialog(
    request: ExportDialogRequest,
    notebookTitle: String,
    pageCount: Int,
    currentPage: Int,
    onSave: (String, List<Int>) -> Unit,
    onShare: (String, List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultName = when (request.format) {
        ExportFormat.Pdf -> "$notebookTitle.pdf"
        ExportFormat.Image -> "$notebookTitle-${currentPage + 1}.png"
        ExportFormat.Notebook -> "$notebookTitle.mynote"
    }
    var fileName by remember(request) { mutableStateOf(defaultName) }
    var scope by remember(request) { mutableStateOf("all") }
    var range by remember(request) { mutableStateOf("1-$pageCount") }
    fun parsedRange(): List<Int> = range.split(',').flatMap { token ->
        val values = token.trim().split('-').mapNotNull(String::toIntOrNull)
        when (values.size) {
            1 -> listOf(values[0] - 1)
            2 -> (minOf(values[0], values[1])..maxOf(values[0], values[1])).map { it - 1 }
            else -> emptyList()
        }
    }.filter { it in 0 until pageCount }.distinct().sorted()
    val pages = if (request.format == ExportFormat.Pdf) when (scope) {
        "current" -> listOf(currentPage)
        "range" -> parsedRange()
        else -> (0 until pageCount).toList()
    } else if (request.format == ExportFormat.Image) listOf(currentPage) else emptyList()
    val title = when (request.format) {
        ExportFormat.Pdf -> "Export PDF"
        ExportFormat.Image -> "Export page image"
        ExportFormat.Notebook -> "Export editable notebook"
    }
    val canContinue = fileName.trim().isNotEmpty() && (request.format != ExportFormat.Pdf || pages.isNotEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (request.format == ExportFormat.Pdf) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectablePill("All pages", scope == "all") { scope = "all" }
                        SelectablePill("Current", scope == "current") { scope = "current" }
                        SelectablePill("Range", scope == "range") { scope = "range" }
                    }
                    if (scope == "range") OutlinedTextField(
                        range,
                        { range = it },
                        label = { Text("Pages, e.g. 1-3, 7") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${pages.size} page${if (pages.size == 1) "" else "s"} will be exported",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else Text(
                    if (request.format == ExportFormat.Image) "Exports the current page as a PNG image."
                    else "Exports a self-contained editable Mheenotes notebook.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(enabled = canContinue, onClick = { onSave(fileName, pages) }) { Text("Save") }
                TextButton(enabled = canContinue, onClick = { onShare(fileName, pages) }) { Text("Share") }
            }
        },
    )
}

@Composable
private fun PdfNavigationDialog(tab: EditorTab, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<PdfNotebookHit>>(emptyList()) }
    var links by remember { mutableStateOf<List<PdfNotebookLink>>(emptyList()) }
    LaunchedEffect(tab.id) { tab.pdfLinks { links = it } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.7f).fillMaxHeight(.72f), shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp) {
            Column(Modifier.padding(22.dp)) {
                Text("Navigate document", style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Search PDF text") }, modifier = Modifier.weight(1f))
                    TextButton(enabled = query.isNotBlank() && !searching, onClick = {
                        searching = true
                        tab.searchPdf(query) { hits = it; searching = false }
                    }) { Text(if (searching) "Searching…" else "Search") }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Results", style = MaterialTheme.typography.titleMedium)
                        LazyColumn {
                            itemsIndexed(hits) { _, hit ->
                                TextButton(onClick = { tab.navigateToPage(hit.notebookPageIndex); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Page ${hit.notebookPageIndex + 1} · ${hit.matchCount} match${if (hit.matchCount == 1) "" else "es"}", Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
                        tab.pages.forEachIndexed { index, page -> if (page.bookmarked) {
                            TextButton(onClick = { tab.navigateToPage(index); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Page ${index + 1}", Modifier.fillMaxWidth()) }
                        } }
                        Spacer(Modifier.height(12.dp))
                        Text("PDF links / outline", style = MaterialTheme.typography.titleMedium)
                        if (links.isEmpty()) Text("No navigable outline links found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        links.take(100).forEach { link ->
                            TextButton(onClick = { tab.navigateToPage(link.targetNotebookPageIndex); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Page ${link.sourceNotebookPageIndex + 1} → ${link.targetNotebookPageIndex + 1}", Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Close") } }
            }
        }
    }
}

@Composable
private fun ObjectSelectionBar(
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
    onFront: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectionActionStrip(
        listOf(
            ImageActionSpec(Icons.Outlined.Edit, stringResource(R.string.edit), onEdit),
            ImageActionSpec(Icons.Outlined.ContentCopy, stringResource(R.string.copy), onCopy),
            ImageActionSpec(Icons.Outlined.Add, stringResource(R.string.duplicate), onDuplicate),
            ImageActionSpec(Icons.Outlined.RotateRight, stringResource(R.string.rotate), onRotate),
            ImageActionSpec(Icons.Outlined.FlipToFront, stringResource(R.string.front), onFront),
            ImageActionSpec(Icons.Outlined.FlipToBack, stringResource(R.string.back), onBack),
            ImageActionSpec(Icons.Outlined.Undo, stringResource(R.string.undo), onUndo),
            ImageActionSpec(Icons.Outlined.Redo, stringResource(R.string.redo), onRedo),
            ImageActionSpec(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete),
        ),
        modifier,
    )
}

@Composable
private fun TextObjectDialog(
    value: SceneObject.TextBox,
    customFonts: List<String>,
    onApply: (SceneObject.TextBox) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(value.id) { mutableStateOf(value.text) }
    var size by remember(value.id) { mutableStateOf(value.size) }
    var color by remember(value.id) { mutableIntStateOf(value.argb) }
    var bold by remember(value.id) { mutableStateOf(value.bold) }
    var italic by remember(value.id) { mutableStateOf(value.italic) }
    var underline by remember(value.id) { mutableStateOf(value.underline) }
    var alignment by remember(value.id) { mutableStateOf(value.alignment) }
    var font by remember(value.id) { mutableStateOf(value.fontFamily) }
    var outlineEnabled by remember(value.id) { mutableStateOf(value.outlineArgb != null) }
    var outlineColor by remember(value.id) { mutableIntStateOf(value.outlineArgb ?: 0xFFFFFFFF.toInt()) }
    var outlineWidth by remember(value.id) { mutableStateOf(value.outlineWidth.coerceAtLeast(400f)) }
    var curveDegrees by remember(value.id) { mutableStateOf(value.curveDegrees) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Edit text", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(text, { text = it }, label = { Text("Text") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Text("Size ${(size / 1_000f).let { ((it * 10).roundToInt() / 10f) }} mm")
                Slider(size, { size = it }, valueRange = 2_000f..20_000f)
                ColorChoiceRow(listOf(0xFF20242B.toInt(), 0xFFFFFFFF.toInt(), 0xFF2563EB.toInt(), 0xFFD43D3D.toInt(), 0xFF2B8A57.toInt()), color) { color = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectablePill("Bold", bold) { bold = !bold }
                    SelectablePill("Italic", italic) { italic = !italic }
                    SelectablePill("Underline", underline) { underline = !underline }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAlignment.entries.forEach { choice -> SelectablePill(choice.name, alignment == choice) { alignment = choice } }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("sans-serif", "serif", "monospace") + customFonts.map { "file:$it" }).forEach { choice ->
                        val label = if (choice.startsWith("file:")) File(choice.removePrefix("file:")).nameWithoutExtension
                        else choice.substringBefore('-').replaceFirstChar(Char::uppercase)
                        SelectablePill(label, font == choice) { font = choice }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Outline", Modifier.weight(1f))
                    Switch(outlineEnabled, { outlineEnabled = it })
                }
                if (outlineEnabled) {
                    ColorChoiceRow(listOf(0xFFFFFFFF.toInt(), 0xFF20242B.toInt(), 0xFF2563EB.toInt(), 0xFFD43D3D.toInt()), outlineColor) { outlineColor = it }
                    Text("Outline ${(outlineWidth / 1_000f * 10f).roundToInt() / 10f} mm")
                    Slider(outlineWidth, { outlineWidth = it }, valueRange = 200f..3_000f)
                }
                Text("Curve ${curveDegrees.roundToInt()}°")
                Slider(curveDegrees, { curveDegrees = it }, valueRange = -180f..180f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(enabled = text.isNotBlank(), onClick = {
                        onApply(value.copy(
                            text = text, size = size, argb = color, bold = bold, italic = italic,
                            underline = underline, alignment = alignment, fontFamily = font,
                            outlineArgb = outlineColor.takeIf { outlineEnabled },
                            outlineWidth = outlineWidth.takeIf { outlineEnabled } ?: 0f,
                            curveDegrees = curveDegrees,
                        ))
                    }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun ShapeObjectDialog(
    value: SceneObject.Rectangle,
    onApply: (SceneObject.Rectangle) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember(value.id) { mutableStateOf(value.kind) }
    var strokeColor by remember(value.id) { mutableIntStateOf(value.strokeArgb) }
    var lineWidth by remember(value.id) { mutableStateOf(value.strokeWidth) }
    var fillEnabled by remember(value.id) { mutableStateOf(value.fillArgb != null) }
    var fillColor by remember(value.id) { mutableIntStateOf(value.fillArgb ?: 0x332563EB) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit shape", style = MaterialTheme.typography.headlineSmall)
                ShapeKind.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { choice -> SelectablePill(choice.name.replace("RoundedRectangle", "Rounded"), kind == choice) { kind = choice } }
                    }
                }
                Text("Stroke")
                ColorChoiceRow(listOf(0xFF20242B.toInt(), 0xFFFFFFFF.toInt(), 0xFF2563EB.toInt(), 0xFFD43D3D.toInt(), 0xFF2B8A57.toInt()), strokeColor) { strokeColor = it }
                Text("Line ${(lineWidth / 1_000f * 10).roundToInt() / 10f} mm")
                Slider(lineWidth, { lineWidth = it }, valueRange = 300f..8_000f)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Fill", Modifier.weight(1f)); Switch(fillEnabled, { fillEnabled = it }) }
                if (fillEnabled) ColorChoiceRow(listOf(0x332563EB, 0x33D43D3D, 0x332B8A57, 0x66FFD84D, 0x33000000), fillColor) { fillColor = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onApply(value.copy(kind = kind, strokeArgb = strokeColor, strokeWidth = lineWidth, fillArgb = fillColor.takeIf { fillEnabled })) }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun MixedSelectionBar(
    hasClipboard: Boolean,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onScreenshot: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRecolor: () -> Unit,
    onWidth: () -> Unit,
    onRotate: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMovePage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildList {
        add(ImageActionSpec(Icons.Outlined.PhotoCamera, "Screenshot", onScreenshot))
        add(ImageActionSpec(Icons.Outlined.ContentCut, stringResource(R.string.cut), onCut))
        add(ImageActionSpec(Icons.Outlined.ContentCopy, stringResource(R.string.copy), onCopy))
        if (hasClipboard) add(ImageActionSpec(Icons.Outlined.ContentPaste, stringResource(R.string.paste), onPaste))
        add(ImageActionSpec(Icons.Outlined.Add, stringResource(R.string.duplicate), onDuplicate))
        add(ImageActionSpec(Icons.Outlined.FormatColorFill, "Use current color", onRecolor))
        add(ImageActionSpec(Icons.Outlined.LineWeight, "Use current width", onWidth))
        add(ImageActionSpec(Icons.Outlined.RotateRight, "Rotate 90 degrees", onRotate))
        if (canMovePrevious) add(ImageActionSpec(Icons.Outlined.NavigateBefore, "Move to previous page") { onMovePage(-1) })
        if (canMoveNext) add(ImageActionSpec(Icons.Outlined.NavigateNext, "Move to next page") { onMovePage(1) })
        add(ImageActionSpec(Icons.Outlined.Undo, stringResource(R.string.undo), onUndo))
        add(ImageActionSpec(Icons.Outlined.Redo, stringResource(R.string.redo), onRedo))
        add(ImageActionSpec(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete))
    }
    SelectionActionStrip(actions, modifier)
}

@Composable
private fun LassoCaptureBar(
    onScreenshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectionActionStrip(
        listOf(ImageActionSpec(Icons.Outlined.PhotoCamera, "Screenshot", onScreenshot)),
        modifier,
    )
}

@Composable
private fun TextRangeSelectionBar(
    languageName: String,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectionActionStrip(
        listOf(
            ImageActionSpec(Icons.Outlined.SelectAll, "Select all $languageName text", onSelectAll),
            ImageActionSpec(Icons.Outlined.ContentCopy, "Copy selected text", onCopy),
            ImageActionSpec(Icons.Outlined.Close, "Close text selection", onDismiss),
        ),
        modifier,
    )
}

@Composable
private fun ClipboardPasteBar(
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectionActionStrip(
        listOf(
            ImageActionSpec(Icons.Outlined.ContentPaste, stringResource(R.string.paste), onPaste),
            ImageActionSpec(Icons.Outlined.Close, stringResource(R.string.cancel), onDismiss),
        ),
        modifier,
    )
}

@Composable
private fun ImageSelectionBar(
    locked: Boolean,
    canPaste: Boolean,
    onCrop: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    onRotate: () -> Unit,
    onRecognizeText: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = if (locked) {
        listOf(ImageActionSpec(Icons.Outlined.LockOpen, stringResource(R.string.unlock), onUnlock))
    } else {
        buildList {
            add(ImageActionSpec(Icons.Outlined.Crop, stringResource(R.string.crop), onCrop))
            add(ImageActionSpec(Icons.Outlined.ContentCut, stringResource(R.string.cut), onCut))
            add(ImageActionSpec(Icons.Outlined.ContentCopy, stringResource(R.string.copy), onCopy))
            if (canPaste) add(ImageActionSpec(Icons.Outlined.ContentPaste, stringResource(R.string.paste), onPaste))
            add(ImageActionSpec(Icons.Outlined.Lock, stringResource(R.string.lock), onLock))
            add(ImageActionSpec(Icons.Outlined.RotateRight, stringResource(R.string.rotate), onRotate))
            add(ImageActionSpec(Icons.Outlined.Search, stringResource(R.string.read_text), onRecognizeText))
            add(ImageActionSpec(Icons.Outlined.FlipToFront, stringResource(R.string.front), onBringToFront))
            add(ImageActionSpec(Icons.Outlined.FlipToBack, stringResource(R.string.back), onSendToBack))
            add(ImageActionSpec(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete))
        }
    }
    SelectionActionStrip(actions, modifier)
}

@Composable
private fun AudioSelectionBar(
    onListen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectionActionStrip(
        listOf(
            ImageActionSpec(Icons.Outlined.PlayArrow, "Listen", onListen),
            ImageActionSpec(Icons.Outlined.Delete, stringResource(R.string.delete), onDelete),
        ),
        modifier,
    )
}

@Composable
private fun OcrResultDialog(
    text: String,
    title: String = "Text from image",
    detail: String = "The result stays on your device.",
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.72f).fillMaxHeight(.72f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(text, Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Recognized text", text))
                    }) { Text("Copy") }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun AudioNotePanel(
    audio: SceneObject.Audio,
    referenceShortSide: Float,
    onScale: (Float) -> Unit,
    onUpdateTranscript: (String) -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var player by remember(audio.id) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(audio.id) { mutableStateOf(false) }
    var position by remember(audio.id) { mutableStateOf(0f) }
    var playbackSpeed by remember(audio.id) { mutableStateOf(1.0f) }
    var playbackError by remember(audio.id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(audio.id) { mutableStateOf(false) }
    var isTranscribing by remember(audio.id) { mutableStateOf(false) }
    var liveDictationText by remember(audio.id) { mutableStateOf("") }
    var speechRecognizerInstance by remember(audio.id) { mutableStateOf<SpeechRecognizer?>(null) }
    var iconFraction by remember(audio.id) {
        mutableStateOf((audio.width / referenceShortSide.coerceAtLeast(1f)).coerceIn(.08f, .40f))
    }

    val transcriptData = remember(audio.transcript, audio.durationMillis) {
        TranscriptData.fromRaw(audio.transcript, audio.durationMillis)
    }
    val amplitudes = remember(audio.asset) {
        AudioWaveformExtractor.extractAmplitudes(audio.asset, 80)
    }

    fun applySpeed(speed: Float) {
        playbackSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                player?.playbackParams = PlaybackParams().apply { this.speed = speed }
            }
        }
    }

    fun preparedPlayer(): MediaPlayer? {
        player?.let { return it }
        return runCatching {
            check(audio.asset.isFile && audio.asset.length() > 44L) { "Recording file is missing or empty" }
            MediaPlayer().apply {
                setDataSource(audio.asset.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1.0f) {
                    playbackParams = PlaybackParams().apply { speed = playbackSpeed }
                }
                setOnCompletionListener {
                    playing = false
                    position = 0f
                }
            }.also { player = it; playbackError = null }
        }.onFailure { playbackError = "Playback unavailable: ${it.message ?: "invalid recording"}" }.getOrNull()
    }

    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableStateOf("") }

    fun extractFromAudioFile() {
        if (isExtracting) return
        isExtracting = true
        extractionProgress = "Extracting text from recorded audio..."
        AudioFileTranscriber.transcribe(
            context = context,
            wavFile = audio.asset,
            durationMillis = audio.durationMillis,
            onProgress = { partial ->
                extractionProgress = partial
            },
            onComplete = { result ->
                isExtracting = false
                if (result.fullText.isNotBlank()) {
                    onUpdateTranscript(result.toJson())
                    Toast.makeText(context, "Audio transcript extracted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No speech detected in recorded audio", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    LaunchedEffect(audio.id) {
        if (transcriptData.fullText.isBlank() && audio.asset.isFile && audio.asset.length() > 44L) {
            extractFromAudioFile()
        }
    }


    DisposableEffect(audio.id) {
        onDispose {
            runCatching { player?.release() }
            player = null
            runCatching {
                speechRecognizerInstance?.stopListening()
                speechRecognizerInstance?.destroy()
            }
            speechRecognizerInstance = null
        }
    }
    LaunchedEffect(audio.id) {
        preparedPlayer()
    }
    LaunchedEffect(playing) {
        while (playing && isActive) {
            val active = player
            if (active != null) position = active.currentPosition.toFloat()
            delay(50)
        }
    }

    val duration = (player?.duration?.toFloat() ?: audio.durationMillis.toFloat()).coerceAtLeast(1f)
    val activeWordIndex = remember(transcriptData, position) {
        transcriptData.wordIndexAt(position.toLong())
    }

    val annotatedTranscript = remember(transcriptData, activeWordIndex) {
        buildAnnotatedString {
            if (transcriptData.words.isNotEmpty()) {
                transcriptData.words.forEachIndexed { index, word ->
                    val isCurrent = (index == activeWordIndex)
                    pushStringAnnotation(tag = "SEEK", annotation = word.startMillis.toString())
                    if (isCurrent) {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF111827),
                                background = Color(0xFFFACC15),
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append(word.text)
                        }
                    } else {
                        withStyle(
                            SpanStyle(
                                color = Color.Unspecified,
                                fontWeight = FontWeight.Normal,
                            ),
                        ) {
                            append(word.text)
                        }
                    }
                    pop()
                    append(" ")
                }
            } else if (transcriptData.fullText.isNotBlank()) {
                append(transcriptData.fullText)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x22EF4444),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Mic, null, tint = Color(0xFFEF4444))
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Audio note", style = MaterialTheme.typography.titleLarge)
                            transcriptData.languageTag?.let { lang ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text(
                                        text = Locale.forLanguageTag(lang).displayLanguage.ifBlank { lang.uppercase(Locale.ROOT) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            if (transcriptData.fullText.isNotBlank()) "Tap any word to jump to that moment" else "Audio recording ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Copy Text button
                    IconButton(
                        onClick = {
                            val textToCopy = transcriptData.fullText
                            if (textToCopy.isNotBlank()) {
                                clipboard.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No transcript available to copy", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy transcript")
                    }

                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, "Delete audio note")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "Close audio note")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Scale slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Icon size ${(iconFraction * 100f).roundToInt()}%", Modifier.width(112.dp), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = iconFraction,
                        onValueChange = { iconFraction = it },
                        onValueChangeFinished = { onScale(referenceShortSide * iconFraction) },
                        valueRange = 0.08f..0.40f,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Top section: Transcript
                Surface(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Box(Modifier.fillMaxSize().padding(18.dp)) {
                        when {
                            transcriptData.fullText.isNotBlank() -> {
                                ClickableText(
                                    text = annotatedTranscript,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 28.sp,
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    onClick = { offset ->
                                        annotatedTranscript.getStringAnnotations(tag = "SEEK", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                val targetMillis = annotation.item.toLongOrNull() ?: 0L
                                                preparedPlayer()?.let { p ->
                                                    p.seekTo(targetMillis.toInt())
                                                    position = targetMillis.toFloat()
                                                    if (!playing) {
                                                        p.start()
                                                        playing = true
                                                    }
                                                }
                                            }
                                    },
                                )
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isExtracting) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(56.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isExtracting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(28.dp),
                                                    strokeWidth = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Outlined.Mic,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (isExtracting) "Extracting text from audio..." else "No transcript available",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (isExtracting) extractionProgress.ifBlank { "Extracting speech from recorded audio..." } else "Extract speech directly from your recorded audio file",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    if (!isExtracting) {
                                        Button(
                                            onClick = ::extractFromAudioFile,
                                        ) {
                                            Icon(Icons.Outlined.Mic, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Extract from Audio")
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

                // Middle section: Amplitude Waveform Progress Bar
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    AudioWaveformBar(
                        amplitudes = amplitudes,
                        currentPositionMillis = position,
                        totalDurationMillis = duration,
                        onSeek = { targetPos ->
                            position = targetPos
                            preparedPlayer()?.seekTo(targetPos.roundToInt())
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }

                // Bottom section: Playback controls
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${formatAudioTime(position.toLong())} / ${formatAudioTime(duration.toLong())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp),
                    )

                    Spacer(Modifier.weight(1f))

                    // -10s button
                    IconButton(onClick = {
                        preparedPlayer()?.let { active ->
                            active.seekTo((active.currentPosition - 10_000).coerceAtLeast(0))
                            position = active.currentPosition.toFloat()
                        }
                    }) {
                        Icon(Icons.Outlined.NavigateBefore, "Back 10 seconds")
                    }

                    // Play/Pause button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp).padding(horizontal = 4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                preparedPlayer()?.let { active ->
                                    if (active.isPlaying) {
                                        active.pause()
                                        playing = false
                                    } else {
                                        active.start()
                                        playing = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                                if (playing) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    // +10s button
                    IconButton(onClick = {
                        preparedPlayer()?.let { active ->
                            active.seekTo((active.currentPosition + 10_000).coerceAtMost(active.duration))
                            position = active.currentPosition.toFloat()
                        }
                    }) {
                        Icon(Icons.Outlined.NavigateNext, "Forward 10 seconds")
                    }

                    Spacer(Modifier.weight(1f))

                    // Speed toggle button
                    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val nextIndex = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                            applySpeed(speeds[nextIndex])
                        },
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete audio note?") },
            text = { Text("This recording will be removed from the page. This action cannot be undone from this popup.") },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}



private fun formatAudioTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private data class ImageActionSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun SelectionActionStrip(actions: List<ImageActionSpec>, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color.Transparent, shadowElevation = 0.dp) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            actions.forEachIndexed { index, action ->
                key(action.label) { StaggeredImageAction(index, action) }
            }
        }
    }
}

@Composable
private fun StaggeredImageAction(index: Int, action: ImageActionSpec) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 44L)
        visible = true
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(150), label = "selection-action-alpha")
    val scale by animateFloatAsState(if (visible) 1f else .76f, tween(300, easing = BackOutEasing), label = "selection-action-scale")
    val translationY by animateFloatAsState(if (visible) 0f else 14f, tween(300, easing = BackOutEasing), label = "selection-action-y")
    Box(Modifier.graphicsLayer {
        this.alpha = alpha
        scaleX = scale
        scaleY = scale
        this.translationY = translationY
    }) {
        ImageActionButton(action.icon, action.label, action.onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val stylusHover = Modifier.pointerInput(label) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.type == PointerType.Stylus || it.type == PointerType.Eraser }) {
                    when (event.type) {
                        PointerEventType.Enter -> scope.launch { tooltipState.show() }
                        PointerEventType.Exit -> tooltipState.dismiss()
                    }
                }
            }
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = tooltipState,
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp,
            shadowElevation = 5.dp,
            modifier = Modifier.size(42.dp).then(stylusHover),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ImageCropDialog(
    image: SceneObject.Image,
    onApply: (ObjectCrop) -> Unit,
    onDismiss: () -> Unit,
) {
    var left by remember(image.id) { mutableStateOf(image.crop.leftFraction) }
    var top by remember(image.id) { mutableStateOf(image.crop.topFraction) }
    var right by remember(image.id) { mutableStateOf(image.crop.rightFraction) }
    var bottom by remember(image.id) { mutableStateOf(image.crop.bottomFraction) }
    var previewSize by remember { mutableStateOf(IntSize(1, 1)) }
    var activeCorner by remember { mutableIntStateOf(0) }
    val cropHandleColor = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 18.dp) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Crop image", style = MaterialTheme.typography.headlineSmall)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(image.bitmap.width / image.bitmap.height.toFloat())
                        .clip(RoundedCornerShape(12.dp))
                        .onSizeChanged { previewSize = it }
                        .pointerInput(image.id, previewSize) {
                            detectDragGestures(
                                onDragStart = { point ->
                                    val corners = listOf(
                                        Offset(left * size.width, top * size.height),
                                        Offset((1f - right) * size.width, top * size.height),
                                        Offset(left * size.width, (1f - bottom) * size.height),
                                        Offset((1f - right) * size.width, (1f - bottom) * size.height),
                                    )
                                    activeCorner = corners.indices.minBy { index ->
                                        val delta = corners[index] - point
                                        delta.x * delta.x + delta.y * delta.y
                                    }
                                },
                            ) { change, amount ->
                                change.consume()
                                val dx = amount.x / size.width.coerceAtLeast(1)
                                val dy = amount.y / size.height.coerceAtLeast(1)
                                if (activeCorner == 0 || activeCorner == 2) left = (left + dx).coerceIn(0f, .9f - right)
                                else right = (right - dx).coerceIn(0f, .9f - left)
                                if (activeCorner == 0 || activeCorner == 1) top = (top + dy).coerceIn(0f, .9f - bottom)
                                else bottom = (bottom - dy).coerceIn(0f, .9f - top)
                            }
                        },
                ) {
                    drawImage(
                        image.bitmap.asImageBitmap(),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    )
                    val cropLeft = left * size.width
                    val cropTop = top * size.height
                    val cropRight = (1f - right) * size.width
                    val cropBottom = (1f - bottom) * size.height
                    val shade = Color.Black.copy(alpha = .58f)
                    drawRect(shade, Offset.Zero, Size(size.width, cropTop))
                    drawRect(shade, Offset(0f, cropBottom), Size(size.width, size.height - cropBottom))
                    drawRect(shade, Offset(0f, cropTop), Size(cropLeft, cropBottom - cropTop))
                    drawRect(shade, Offset(cropRight, cropTop), Size(size.width - cropRight, cropBottom - cropTop))
                    drawRect(
                        Color.White,
                        Offset(cropLeft, cropTop),
                        Size(cropRight - cropLeft, cropBottom - cropTop),
                        style = Stroke(2.dp.toPx()),
                    )
                    listOf(
                        Offset(cropLeft, cropTop), Offset(cropRight, cropTop),
                        Offset(cropLeft, cropBottom), Offset(cropRight, cropBottom),
                    ).forEach { handle ->
                        drawCircle(Color.White, 8.dp.toPx(), handle)
                        drawCircle(cropHandleColor, 8.dp.toPx(), handle, style = Stroke(2.dp.toPx()))
                    }
                }
                Text("Drag any corner. The dark area will be cropped out.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { left = 0f; top = 0f; right = 0f; bottom = 0f }) { Text("Reset") }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onApply(ObjectCrop(left, top, right, bottom)) }) { Text("Apply") }
                }
            }
        }
    }
}

private fun ViewportTransform.clampedToDocument(viewWidth: Float, viewHeight: Float): ViewportTransform {
    val margin = 28f
    val contentWidth = pageWidth * scale
    val contentHeight = pageHeight * scale
    val x = if (contentWidth <= viewWidth) (viewWidth - contentWidth) / 2f
    else offsetX.coerceIn(viewWidth - margin - contentWidth, margin)
    val y = if (contentHeight <= viewHeight) (viewHeight - contentHeight) / 2f
    else offsetY.coerceIn(viewHeight - margin - contentHeight, margin)
    return copy(offsetX = x, offsetY = y)
}

private fun SceneObject.Image.toUnrotatedPoint(point: PagePoint): PagePoint {
    if (rotation == 0f) return point
    val centerX = x + width / 2f
    val centerY = y + height / 2f
    val radians = Math.toRadians((-rotation).toDouble())
    val dx = point.x - centerX
    val dy = point.y - centerY
    return PagePoint(
        centerX + (dx * kotlin.math.cos(radians) - dy * kotlin.math.sin(radians)).toFloat(),
        centerY + (dx * kotlin.math.sin(radians) + dy * kotlin.math.cos(radians)).toFloat(),
    )
}

private fun SceneObject.Image.toUnrotatedDelta(dx: Float, dy: Float): PagePoint {
    if (rotation == 0f) return PagePoint(dx, dy)
    val radians = Math.toRadians((-rotation).toDouble())
    return PagePoint(
        (dx * kotlin.math.cos(radians) - dy * kotlin.math.sin(radians)).toFloat(),
        (dx * kotlin.math.sin(radians) + dy * kotlin.math.cos(radians)).toFloat(),
    )
}

private fun SceneObject.Rectangle.toUnrotatedPoint(point: PagePoint): PagePoint {
    if (rotation == 0f) return point
    val centerX = x + width / 2f
    val centerY = y + height / 2f
    val radians = Math.toRadians((-rotation).toDouble())
    val dx = point.x - centerX
    val dy = point.y - centerY
    return PagePoint(
        centerX + (dx * kotlin.math.cos(radians) - dy * kotlin.math.sin(radians)).toFloat(),
        centerY + (dx * kotlin.math.sin(radians) + dy * kotlin.math.cos(radians)).toFloat(),
    )
}

private fun SceneObject.TextBox.visualBounds(): PageRect {
    val lineCount = text.lines().size.coerceAtLeast(1)
    val contentHeight = size + (lineCount - 1) * size * 1.2f + size * .25f
    val visualHeight = maxOf(height, contentHeight)
    val bend = width * (kotlin.math.abs(curveDegrees) / 180f) * .45f
    val extraTop = bend.takeIf { curveDegrees > 0f } ?: 0f
    val extraBottom = bend.takeIf { curveDegrees < 0f } ?: 0f
    return PageRect(x, y - size - extraTop, x + width, y - size + visualHeight + extraBottom)
}

private fun SceneObject.TextBox.toUnrotatedPoint(point: PagePoint): PagePoint {
    if (rotation == 0f) return point
    val bounds = visualBounds()
    val centerX = (bounds.left + bounds.right) / 2f
    val centerY = (bounds.top + bounds.bottom) / 2f
    val radians = Math.toRadians((-rotation).toDouble())
    val dx = point.x - centerX
    val dy = point.y - centerY
    return PagePoint(
        centerX + (dx * kotlin.math.cos(radians) - dy * kotlin.math.sin(radians)).toFloat(),
        centerY + (dx * kotlin.math.sin(radians) + dy * kotlin.math.cos(radians)).toFloat(),
    )
}

private fun SceneObject.contains(point: PagePoint): Boolean = when (this) {
    is SceneObject.Image -> toUnrotatedPoint(point).let { it.x in x..(x + width) && it.y in y..(y + height) }
    is SceneObject.Rectangle -> if (kind == ShapeKind.Line || kind == ShapeKind.Arrow) {
        hitByEraser(point, 3_500f)
    } else {
        toUnrotatedPoint(point).let { it.x in x..(x + width) && it.y in y..(y + height) }
    }
    is SceneObject.TextBox -> {
        val bounds = visualBounds()
        toUnrotatedPoint(point).let { it.x in bounds.left..bounds.right && it.y in bounds.top..bounds.bottom }
    }
    is SceneObject.Audio -> point.x in x..(x + width) && point.y in y..(y + height)
}

private fun SceneObject.Rectangle.hitByEraser(point: PagePoint, radius: Float): Boolean {
    val localPoint = toUnrotatedPoint(point)
    val tolerance = radius + strokeWidth / 2f
    fun distanceToSegment(start: PagePoint, end: PagePoint): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= 1f) return hypot(localPoint.x - start.x, localPoint.y - start.y)
        val t = (((localPoint.x - start.x) * dx + (localPoint.y - start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        return hypot(localPoint.x - (start.x + t * dx), localPoint.y - (start.y + t * dy))
    }
    val leftTop = PagePoint(x, y)
    val rightTop = PagePoint(x + width, y)
    val leftBottom = PagePoint(x, y + height)
    val rightBottom = PagePoint(x + width, y + height)
    return when (kind) {
        ShapeKind.Line, ShapeKind.Arrow -> distanceToSegment(leftTop, rightBottom) <= tolerance
        ShapeKind.Rectangle, ShapeKind.RoundedRectangle -> listOf(
            leftTop to rightTop,
            rightTop to rightBottom,
            rightBottom to leftBottom,
            leftBottom to leftTop,
        ).any { (start, end) -> distanceToSegment(start, end) <= tolerance }
        ShapeKind.Triangle -> {
            val peak = PagePoint(x + width / 2f, y)
            listOf(peak to rightBottom, rightBottom to leftBottom, leftBottom to peak)
                .any { (start, end) -> distanceToSegment(start, end) <= tolerance }
        }
        ShapeKind.Ellipse -> {
            val radiusX = width / 2f
            val radiusY = height / 2f
            if (radiusX <= 0f || radiusY <= 0f) false else {
                val normalized = hypot((localPoint.x - x - radiusX) / radiusX, (localPoint.y - y - radiusY) / radiusY)
                kotlin.math.abs(normalized - 1f) * minOf(radiusX, radiusY) <= tolerance
            }
        }
    }
}

private fun SceneObject.move(dx: Float, dy: Float): SceneObject = when (this) {
    is SceneObject.Image -> copy(x = x + dx, y = y + dy)
    is SceneObject.Rectangle -> copy(x = x + dx, y = y + dy)
    is SceneObject.TextBox -> copy(x = x + dx, y = y + dy)
    is SceneObject.Audio -> copy(x = x + dx, y = y + dy)
}

private fun SceneObject.resizeHandleContains(point: PagePoint, radius: Float): Boolean {
    val hitPoint = when (this) {
        is SceneObject.Image -> toUnrotatedPoint(point)
        is SceneObject.Rectangle -> toUnrotatedPoint(point)
        is SceneObject.TextBox -> toUnrotatedPoint(point)
        else -> point
    }
    val handle = when (this) {
        is SceneObject.Image -> PagePoint(x, y + height)
        is SceneObject.Rectangle -> PagePoint(x + width, y + height)
        is SceneObject.TextBox -> visualBounds().let { PagePoint(it.right, it.bottom) }
        is SceneObject.Audio -> PagePoint(x + width, y + height)
    }
    val dx = hitPoint.x - handle.x
    val dy = hitPoint.y - handle.y
    return dx * dx + dy * dy <= radius * radius
}

private fun SceneObject.resize(deltaWidth: Float, deltaHeight: Float): SceneObject = when (this) {
    is SceneObject.Image -> {
        val horizontalScale = (width + deltaWidth) / width
        val verticalScale = (height + deltaHeight) / height
        val requestedScale = if (kotlin.math.abs(horizontalScale - 1f) >= kotlin.math.abs(verticalScale - 1f)) {
            horizontalScale
        } else {
            verticalScale
        }
        val minimumScale = maxOf(5_000f / width, 5_000f / height)
        val scale = requestedScale.coerceAtLeast(minimumScale)
        copy(width = width * scale, height = height * scale)
    }
    is SceneObject.Rectangle -> copy(
        width = (width + deltaWidth).coerceAtLeast(5_000f),
        height = (height + deltaHeight).coerceAtLeast(5_000f),
    )
    is SceneObject.TextBox -> copy(
        width = (width + deltaWidth).coerceAtLeast(10_000f),
        height = (height + deltaHeight).coerceAtLeast(size * 1.2f),
    )
    is SceneObject.Audio -> copy(
        width = (width + deltaWidth).coerceAtLeast(6_000f),
        height = (height + deltaHeight).coerceAtLeast(6_000f),
    )
}

private fun SceneObject.Image.resizeFromLeftBottom(deltaX: Float, deltaY: Float): SceneObject.Image {
    val horizontalScale = (width - deltaX) / width
    val verticalScale = (height + deltaY) / height
    val requestedScale = if (kotlin.math.abs(horizontalScale - 1f) >= kotlin.math.abs(verticalScale - 1f)) {
        horizontalScale
    } else {
        verticalScale
    }
    val minimumScale = maxOf(5_000f / width, 5_000f / height)
    val scale = requestedScale.coerceAtLeast(minimumScale)
    val nextWidth = width * scale
    val nextHeight = height * scale
    return copy(x = x + width - nextWidth, width = nextWidth, height = nextHeight)
}

private fun mixedSelectionBounds(selection: MixedSelection, pages: List<PageEntry>): PageRect? {
    val page = pages.getOrNull(selection.pageIndex) ?: return null
    val strokeBounds = page.session.page.strokes.filter { it.id in selection.strokeIds }.map(InkStroke::bounds)
    val objectBounds = page.objects.filter { it.id in selection.objectIds }.map { item ->
        when (item) {
            is SceneObject.Image -> PageRect(item.x, item.y, item.x + item.width, item.y + item.height)
            is SceneObject.Rectangle -> PageRect(
                minOf(item.x, item.x + item.width), minOf(item.y, item.y + item.height),
                maxOf(item.x, item.x + item.width), maxOf(item.y, item.y + item.height),
            )
            is SceneObject.TextBox -> item.visualBounds()
            is SceneObject.Audio -> PageRect(item.x, item.y, item.x + item.width, item.y + item.height)
        }
    }
    val values = strokeBounds + objectBounds
    if (values.isEmpty()) return null
    return PageRect(values.minOf(PageRect::left), values.minOf(PageRect::top), values.maxOf(PageRect::right), values.maxOf(PageRect::bottom))
}

private fun SceneObject.duplicate(): SceneObject = when (this) {
    is SceneObject.Image -> copy(id = ObjectId(UUID.randomUUID().toString()))
    is SceneObject.Rectangle -> copy(id = ObjectId(UUID.randomUUID().toString()))
    is SceneObject.TextBox -> copy(id = ObjectId(UUID.randomUUID().toString()))
    is SceneObject.Audio -> copy(id = ObjectId(UUID.randomUUID().toString()))
}

private fun PageEntry.reindexObjects() {
    objects.indices.forEach { index -> objects[index] = objects[index].withZIndex(index.toLong()) }
}

private fun SceneObject.withZIndex(value: Long): SceneObject = when (this) {
    is SceneObject.Image -> copy(zIndex = value)
    is SceneObject.Rectangle -> copy(zIndex = value)
    is SceneObject.TextBox -> copy(zIndex = value)
    is SceneObject.Audio -> copy(zIndex = value)
}

private fun SceneObject.rotateBy(degrees: Float): SceneObject = when (this) {
    is SceneObject.Image -> copy(rotation = (rotation + degrees) % 360f)
    is SceneObject.Rectangle -> copy(rotation = (rotation + degrees) % 360f)
    is SceneObject.TextBox -> copy(rotation = (rotation + degrees) % 360f)
    is SceneObject.Audio -> this
}

private fun DrawScope.drawPlacedPage(placement: PagePlacement, viewport: ViewportTransform, selected: Boolean) {
    val left = viewport.offsetX + placement.left * viewport.scale
    val top = viewport.offsetY + placement.top * viewport.scale
    val width = placement.page.size.widthMicrons * viewport.scale
    val height = placement.page.size.heightMicrons * viewport.scale
    drawRect(Color(0x44000000), Offset(left + 5f, top + 7f), Size(width, height))
    drawRect(Color(placement.page.backgroundArgb), Offset(left, top), Size(width, height))
    val templateColor = Color(placement.page.templateLineArgb)
    if (selected) drawRect(Color(0xFF5B8DEF), Offset(left, top), Size(width, height), style = Stroke(2.dp.toPx()))
    placement.page.pdfBackground?.let { bitmap ->
        drawImage(bitmap.asImageBitmap(), dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = IntSize(width.roundToInt(), height.roundToInt()))
        return
    }
    when (placement.page.template) {
        PageTemplate.Blank -> Unit
        PageTemplate.Ruled -> {
            var y = 9_000f
            while (y < placement.page.size.heightMicrons) {
                val screenY = top + y * viewport.scale
                drawLine(templateColor, Offset(left, screenY), Offset(left + width, screenY), 1f)
                y += 9_000f
            }
        }
        PageTemplate.Grid -> {
            var y = 8_000f
            while (y < placement.page.size.heightMicrons) {
                val screenY = top + y * viewport.scale
                drawLine(templateColor, Offset(left, screenY), Offset(left + width, screenY), 1f)
                y += 8_000f
            }
            var x = 8_000f
            while (x < placement.page.size.widthMicrons) {
                val screenX = left + x * viewport.scale
                drawLine(templateColor, Offset(screenX, top), Offset(screenX, top + height), 1f)
                x += 8_000f
            }
        }
        PageTemplate.Dotted -> {
            var y = 8_000f
            while (y < placement.page.size.heightMicrons) {
                var x = 8_000f
                while (x < placement.page.size.widthMicrons) {
                    drawCircle(templateColor, (viewport.scale * 500f).coerceAtLeast(.7f), Offset(left + x * viewport.scale, top + y * viewport.scale))
                    x += 8_000f
                }
                y += 8_000f
            }
        }
        PageTemplate.Cornell -> {
            var y = 36_000f
            while (y < placement.page.size.heightMicrons) {
                val screenY = top + y * viewport.scale
                drawLine(templateColor, Offset(left, screenY), Offset(left + width, screenY), 1f)
                y += 9_000f
            }
            drawLine(templateColor, Offset(left + 45_000f * viewport.scale, top), Offset(left + 45_000f * viewport.scale, top + height), 1f)
            drawLine(templateColor, Offset(left, top + 27_000f * viewport.scale), Offset(left + width, top + 27_000f * viewport.scale), 1f)
        }
        PageTemplate.MusicStaff -> {
            var staffTop = 18_000f
            while (staffTop < placement.page.size.heightMicrons) {
                repeat(5) { line ->
                    val screenY = top + (staffTop + line * 2_500f) * viewport.scale
                    drawLine(templateColor, Offset(left, screenY), Offset(left + width, screenY), 1f)
                }
                staffTop += 24_000f
            }
        }
    }
}

private fun DrawScope.drawSavedStroke(
    stroke: InkStroke,
    placement: PagePlacement,
    viewport: ViewportTransform,
) {
    val samples = stroke.samples
    if (samples.isEmpty()) return
    val originX = viewport.offsetX + placement.left * viewport.scale
    val originY = viewport.offsetY + placement.top * viewport.scale
    fun screen(sample: StrokeSample) = Offset(
        originX + sample.position.x * viewport.scale,
        originY + sample.position.y * viewport.scale,
    )
    val reportedPressures = samples.mapNotNull(StrokeSample::pressure)
    val representativePressure = reportedPressures.takeIf(List<Float>::isNotEmpty)
        ?.average()?.toFloat()
    val width = (PressureNormalizer.width(representativePressure, stroke.style) * viewport.scale)
        .coerceAtLeast(.75f)
    val color = Color(stroke.style.argb).copy(alpha = stroke.style.opacity)
    if (samples.size == 1) {
        drawCircle(color, width / 2f, screen(samples.first()))
        return
    }
    val path = Path().apply {
        val first = screen(samples.first())
        moveTo(first.x, first.y)
        samples.drop(1).forEach { sample ->
            val point = screen(sample)
            lineTo(point.x, point.y)
        }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
}

private fun DrawScope.drawPlacedObject(item: SceneObject, placement: PagePlacement, viewport: ViewportTransform, selected: Boolean) {
    val originX = viewport.offsetX + placement.left * viewport.scale
    val originY = viewport.offsetY + placement.top * viewport.scale
    val rotation = when (item) {
        is SceneObject.Image -> item.rotation
        is SceneObject.Rectangle -> item.rotation
        is SceneObject.TextBox -> item.rotation
        is SceneObject.Audio -> 0f
    }
    val pivot = when (item) {
        is SceneObject.Image -> Offset(originX + (item.x + item.width / 2f) * viewport.scale, originY + (item.y + item.height / 2f) * viewport.scale)
        is SceneObject.Rectangle -> Offset(originX + (item.x + item.width / 2f) * viewport.scale, originY + (item.y + item.height / 2f) * viewport.scale)
        is SceneObject.TextBox -> item.visualBounds().let {
            Offset(originX + (it.left + it.right) / 2f * viewport.scale, originY + (it.top + it.bottom) / 2f * viewport.scale)
        }
        is SceneObject.Audio -> Offset(originX + (item.x + item.width / 2f) * viewport.scale, originY + (item.y + item.height / 2f) * viewport.scale)
    }
    val canvasCheckpoint = drawContext.canvas.nativeCanvas.save()
    if (rotation != 0f) drawContext.canvas.nativeCanvas.rotate(rotation, pivot.x, pivot.y)
    val bounds = when (item) {
        is SceneObject.Image -> {
            drawImage(
                item.bitmap.asImageBitmap(),
                srcOffset = IntOffset(
                    (item.bitmap.width * item.crop.leftFraction).roundToInt(),
                    (item.bitmap.height * item.crop.topFraction).roundToInt(),
                ),
                srcSize = IntSize(
                    (item.bitmap.width * (1f - item.crop.leftFraction - item.crop.rightFraction)).roundToInt(),
                    (item.bitmap.height * (1f - item.crop.topFraction - item.crop.bottomFraction)).roundToInt(),
                ),
                dstOffset = IntOffset(
                    (originX + item.x * viewport.scale).roundToInt(),
                    (originY + item.y * viewport.scale).roundToInt(),
                ),
                dstSize = IntSize(
                    (item.width * viewport.scale).roundToInt(),
                    (item.height * viewport.scale).roundToInt(),
                ),
                alpha = item.opacity,
            )
            floatArrayOf(item.x, item.y, item.width, item.height)
        }
        is SceneObject.Rectangle -> {
            val topLeft = Offset(originX + item.x * viewport.scale, originY + item.y * viewport.scale)
            val shapeSize = Size(item.width * viewport.scale, item.height * viewport.scale)
            val stroke = Stroke((item.strokeWidth * viewport.scale).coerceAtLeast(2f))
            when (item.kind) {
                ShapeKind.Line, ShapeKind.Arrow -> drawLine(
                    Color(item.strokeArgb), topLeft, topLeft + Offset(shapeSize.width, shapeSize.height),
                    stroke.width,
                )
                ShapeKind.Rectangle -> {
                    item.fillArgb?.let { drawRect(Color(it), topLeft, shapeSize) }
                    drawRect(Color(item.strokeArgb), topLeft, shapeSize, style = stroke)
                }
                ShapeKind.RoundedRectangle -> {
                    item.fillArgb?.let { drawRoundRect(Color(it), topLeft, shapeSize, androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())) }
                    drawRoundRect(Color(item.strokeArgb), topLeft, shapeSize, androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = stroke)
                }
                ShapeKind.Ellipse -> {
                    item.fillArgb?.let { drawOval(Color(it), topLeft, shapeSize) }
                    drawOval(Color(item.strokeArgb), topLeft, shapeSize, style = stroke)
                }
                ShapeKind.Triangle -> {
                    val path = Path().apply {
                        moveTo(topLeft.x + shapeSize.width / 2f, topLeft.y)
                        lineTo(topLeft.x + shapeSize.width, topLeft.y + shapeSize.height)
                        lineTo(topLeft.x, topLeft.y + shapeSize.height)
                        close()
                    }
                    item.fillArgb?.let { drawPath(path, Color(it)) }
                    drawPath(path, Color(item.strokeArgb), style = stroke)
                }
            }
            floatArrayOf(item.x, item.y, item.width, item.height)
        }
        is SceneObject.TextBox -> {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.argb
                textSize = item.size * viewport.scale
                isUnderlineText = item.underline
                typeface = android.graphics.Typeface.create(
                    if (item.fontFamily.startsWith("file:")) null else item.fontFamily,
                    when {
                        item.bold && item.italic -> android.graphics.Typeface.BOLD_ITALIC
                        item.bold -> android.graphics.Typeface.BOLD
                        item.italic -> android.graphics.Typeface.ITALIC
                        else -> android.graphics.Typeface.NORMAL
                    },
                )
                textAlign = when (item.alignment) {
                    TextAlignment.Start -> Paint.Align.LEFT
                    TextAlignment.Center -> Paint.Align.CENTER
                    TextAlignment.End -> Paint.Align.RIGHT
                }
            }
            if (item.fontFamily.startsWith("file:")) runCatching {
                paint.typeface = android.graphics.Typeface.createFromFile(item.fontFamily.removePrefix("file:"))
            }
            val baseX = originX + when (item.alignment) {
                TextAlignment.Start -> item.x
                TextAlignment.Center -> item.x + item.width / 2f
                TextAlignment.End -> item.x + item.width
            } * viewport.scale
            item.text.lines().forEachIndexed { index, line ->
                val baseline = originY + (item.y + index * item.size * 1.2f) * viewport.scale
                val curvePath = item.curveDegrees.takeIf { kotlin.math.abs(it) >= 1f }?.let { curve ->
                    android.graphics.Path().apply {
                        val startX = originX + item.x * viewport.scale
                        val width = item.width * viewport.scale
                        moveTo(startX, baseline)
                        quadTo(startX + width / 2f, baseline - width * (curve / 180f) * .45f, startX + width, baseline)
                    }
                }
                item.outlineArgb?.takeIf { item.outlineWidth > 0f }?.let { outlineColor ->
                    val fillColor = paint.color
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = item.outlineWidth * viewport.scale
                    paint.color = outlineColor
                    if (curvePath == null) drawContext.canvas.nativeCanvas.drawText(line, baseX, baseline, paint)
                    else drawContext.canvas.nativeCanvas.drawTextOnPath(line, curvePath, 0f, 0f, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = fillColor
                }
                if (curvePath == null) drawContext.canvas.nativeCanvas.drawText(line, baseX, baseline, paint)
                else drawContext.canvas.nativeCanvas.drawTextOnPath(line, curvePath, 0f, 0f, paint)
            }
            item.visualBounds().let { floatArrayOf(it.left, it.top, it.right - it.left, it.bottom - it.top) }
        }
        is SceneObject.Audio -> {
            val center = Offset(
                originX + (item.x + item.width / 2f) * viewport.scale,
                originY + (item.y + item.height / 2f) * viewport.scale,
            )
            val radius = minOf(item.width, item.height) * viewport.scale / 2f
            drawCircle(Color(0xFFEF4444), radius, center)
            drawRoundRect(
                Color.White,
                topLeft = Offset(center.x - radius * .22f, center.y - radius * .48f),
                size = Size(radius * .44f, radius * .70f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * .24f),
            )
            drawLine(
                Color.White,
                Offset(center.x, center.y + radius * .20f),
                Offset(center.x, center.y + radius * .55f),
                (radius * .12f).coerceAtLeast(2f),
            )
            floatArrayOf(item.x, item.y, item.width, item.height)
        }
    }
    if (selected) {
        val selectionTopLeft = Offset(
            originX + bounds[0] * viewport.scale,
            originY + bounds[1] * viewport.scale,
        )
        val selectionSize = Size(bounds[2] * viewport.scale, bounds[3] * viewport.scale)
        drawRect(Color(0xFF3B82F6), selectionTopLeft, selectionSize, style = Stroke(2.dp.toPx()))
        if (item !is SceneObject.Image || !item.locked) {
            val handle = if (item is SceneObject.Image) {
                selectionTopLeft + Offset(0f, selectionSize.height)
            } else {
                selectionTopLeft + Offset(selectionSize.width, selectionSize.height)
            }
            drawCircle(Color.White, 10.dp.toPx(), handle)
            drawCircle(Color(0xFF3B82F6), 10.dp.toPx(), handle, style = Stroke(3.dp.toPx()))
        }
    }
    drawContext.canvas.nativeCanvas.restoreToCount(canvasCheckpoint)
}

private fun floatingNotesShortcutCode(stylusButton: Int): Int = -stylusButton

private fun readableKeyName(keyCode: Int): String {
    if (keyCode < 0) return when (-keyCode) {
        android.view.MotionEvent.BUTTON_STYLUS_PRIMARY -> "Stylus primary button"
        android.view.MotionEvent.BUTTON_STYLUS_SECONDARY -> "Stylus secondary button"
        else -> "Stylus button ${-keyCode}"
    }
    return KeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .lowercase()
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
}

private class FloatingNoteBody(
    val noteId: Int,
    initialPosition: Offset,
    initialVelocity: Offset,
) {
    var position by mutableStateOf(initialPosition)
    var velocity by mutableStateOf(initialVelocity)
    var dragging by mutableStateOf(false)
}

@Composable
private fun FloatingNotesOverlay(
    notebooks: List<EditorTab>,
    selectedTabId: Int,
    onOpen: (EditorTab) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var query by rememberSaveable { mutableStateOf("") }
    val bodies = remember { mutableStateMapOf<Int, FloatingNoteBody>() }
    val backdropReveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { backdropReveal.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().zIndex(95f),
    ) {
        val noteAspectRatio = 136f / 184f
        val usableHeight = (maxHeight - 124.dp).coerceAtLeast(180.dp)
        val targetAreaPerNote = maxWidth.value * usableHeight.value * .56f / notebooks.size.coerceAtLeast(1)
        val adaptiveNoteWidth = sqrt(targetAreaPerNote * noteAspectRatio).coerceIn(64f, 136f)
        val noteWidth = adaptiveNoteWidth.dp
        val noteHeight = (adaptiveNoteWidth / noteAspectRatio).dp
        val noteWidthPx = with(density) { noteWidth.toPx() }
        val noteHeightPx = with(density) { noteHeight.toPx() }
        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }
        val edgePaddingPx = with(density) { 14.dp.toPx() }
        val searchFloorPx = with(density) { 124.dp.toPx() }
        val notebookKey = notebooks.joinToString("|") { "${it.id}:${it.folderPath}" }

        LaunchedEffect(notebookKey, canvasWidthPx, canvasHeightPx) {
            val activeIds = notebooks.mapTo(mutableSetOf()) { it.id }
            bodies.keys.filterNot(activeIds::contains).forEach(bodies::remove)
            val maxX = (canvasWidthPx - noteWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
            val maxY = (canvasHeightPx - noteHeightPx - edgePaddingPx).coerceAtLeast(searchFloorPx)
            notebooks.forEach { note ->
                val groupSeed = (note.folderPath ?: "Main").hashCode()
                val groupRandom = Random(groupSeed)
                val groupCenter = Offset(
                    edgePaddingPx + groupRandom.nextFloat() * (maxX - edgePaddingPx).coerceAtLeast(1f),
                    searchFloorPx + groupRandom.nextFloat() * (maxY - searchFloorPx).coerceAtLeast(1f),
                )
                val random = Random(note.persistentId.hashCode())
                bodies.getOrPut(note.id) {
                    val angle = random.nextFloat() * (Math.PI * 2.0).toFloat()
                    val position = Offset(
                        (groupCenter.x + (random.nextFloat() - .5f) * noteWidthPx * 2.8f).coerceIn(edgePaddingPx, maxX),
                        (groupCenter.y + (random.nextFloat() - .5f) * noteHeightPx * 1.8f).coerceIn(searchFloorPx, maxY),
                    )
                    val speed = with(density) { (7f + random.nextFloat() * 11f).dp.toPx() }
                    FloatingNoteBody(note.id, position, Offset(cos(angle) * speed, sin(angle) * speed))
                }.also { body ->
                    body.position = Offset(body.position.x.coerceIn(edgePaddingPx, maxX), body.position.y.coerceIn(searchFloorPx, maxY))
                }
            }

            var previousFrame = withFrameNanos { it }
            while (isActive) {
                val frame = withFrameNanos { it }
                val dt = ((frame - previousFrame) / 1_000_000_000f).coerceIn(.001f, .034f)
                previousFrame = frame
                val activeBodies = notebooks.mapNotNull { bodies[it.id] }
                activeBodies.forEach { body ->
                    if (body.dragging) return@forEach
                    var position = body.position + body.velocity * dt
                    var velocity = body.velocity * .9994f
                    if (position.x < edgePaddingPx) {
                        position = position.copy(x = edgePaddingPx); velocity = velocity.copy(x = kotlin.math.abs(velocity.x) * .88f)
                    } else if (position.x > maxX) {
                        position = position.copy(x = maxX); velocity = velocity.copy(x = -kotlin.math.abs(velocity.x) * .88f)
                    }
                    if (position.y < searchFloorPx) {
                        position = position.copy(y = searchFloorPx); velocity = velocity.copy(y = kotlin.math.abs(velocity.y) * .88f)
                    } else if (position.y > maxY) {
                        position = position.copy(y = maxY); velocity = velocity.copy(y = -kotlin.math.abs(velocity.y) * .88f)
                    }
                    body.position = position
                    body.velocity = velocity
                }
                for (firstIndex in 0 until activeBodies.lastIndex) {
                    val first = activeBodies[firstIndex]
                    for (secondIndex in firstIndex + 1 until activeBodies.size) {
                        val second = activeBodies[secondIndex]
                        val overlapX = minOf(first.position.x + noteWidthPx, second.position.x + noteWidthPx) -
                            maxOf(first.position.x, second.position.x)
                        val overlapY = minOf(first.position.y + noteHeightPx, second.position.y + noteHeightPx) -
                            maxOf(first.position.y, second.position.y)
                        if (overlapX <= 0f || overlapY <= 0f) continue
                        if (overlapX < overlapY) {
                            val direction = if (first.position.x < second.position.x) -1f else 1f
                            val firstShare = if (first.dragging) 0f else if (second.dragging) 1f else .5f
                            val secondShare = if (second.dragging) 0f else if (first.dragging) 1f else .5f
                            first.position += Offset(direction * overlapX * firstShare, 0f)
                            second.position -= Offset(direction * overlapX * secondShare, 0f)
                            if (!first.dragging && !second.dragging) {
                                val firstX = first.velocity.x
                                first.velocity = first.velocity.copy(x = second.velocity.x * .9f)
                                second.velocity = second.velocity.copy(x = firstX * .9f)
                            } else if (!first.dragging) first.velocity = first.velocity.copy(x = direction * kotlin.math.abs(first.velocity.x).coerceAtLeast(80f))
                            else if (!second.dragging) second.velocity = second.velocity.copy(x = -direction * kotlin.math.abs(second.velocity.x).coerceAtLeast(80f))
                        } else {
                            val direction = if (first.position.y < second.position.y) -1f else 1f
                            val firstShare = if (first.dragging) 0f else if (second.dragging) 1f else .5f
                            val secondShare = if (second.dragging) 0f else if (first.dragging) 1f else .5f
                            first.position += Offset(0f, direction * overlapY * firstShare)
                            second.position -= Offset(0f, direction * overlapY * secondShare)
                            if (!first.dragging && !second.dragging) {
                                val firstY = first.velocity.y
                                first.velocity = first.velocity.copy(y = second.velocity.y * .9f)
                                second.velocity = second.velocity.copy(y = firstY * .9f)
                            } else if (!first.dragging) first.velocity = first.velocity.copy(y = direction * kotlin.math.abs(first.velocity.y).coerceAtLeast(80f))
                            else if (!second.dragging) second.velocity = second.velocity.copy(y = -direction * kotlin.math.abs(second.velocity.y).coerceAtLeast(80f))
                        }
                    }
                }
            }
        }

        Box(
            Modifier.fillMaxSize().background(Color(0xB3070A0F).copy(alpha = .70f * backdropReveal.value)).pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        )

        if (notebooks.isEmpty()) Text(
            "No notes yet",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )

        notebooks.forEachIndexed { index, note ->
            val body = bodies[note.id] ?: return@forEachIndexed
            val matches = query.isBlank() || note.title.contains(query.trim(), ignoreCase = true)
            FloatingNotebookCard(
                note = note,
                body = body,
                width = noteWidth,
                height = noteHeight,
                selected = note.id == selectedTabId,
                matchesSearch = matches,
                revealDelayMillis = (index.coerceAtMost(12) * 38) + (kotlin.math.abs(note.persistentId.hashCode()) % 90),
                onOpen = { onOpen(note) },
            )
        }

        val searchReveal = backdropReveal.value
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .fillMaxWidth(.58f)
                .widthIn(max = 480.dp)
                .graphicsLayer { alpha = searchReveal; scaleX = .86f + .14f * searchReveal; scaleY = .86f + .14f * searchReveal }
                .zIndex(10f),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
            },
            placeholder = { Text("Search notes") },
            shape = RoundedCornerShape(100),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
        )
    }
}

@Composable
private fun FloatingNotebookCard(
    note: EditorTab,
    body: FloatingNoteBody,
    width: Dp,
    height: Dp,
    selected: Boolean,
    matchesSearch: Boolean,
    revealDelayMillis: Int,
    onOpen: () -> Unit,
) {
    val previewPage = note.pages.firstOrNull()
    val compact = width < 104.dp
    val veryCompact = width < 80.dp
    val reveal = remember(note.id) { Animatable(0f) }
    var dragSampleAt by remember(note.id) { mutableLongStateOf(0L) }
    val matchAlpha by animateFloatAsState(if (matchesSearch) 1f else .30f, tween(240), label = "floating-match")
    LaunchedEffect(note.id, previewPage?.id) { previewPage?.let { note.requestThumbnail(it.id) } }
    LaunchedEffect(note.id) {
        delay(revealDelayMillis.toLong())
        reveal.animateTo(1f, tween(470, easing = BackOutEasing))
    }
    Card(
        onClick = onOpen,
        modifier = Modifier
            .size(width, height)
            .graphicsLayer {
                translationX = body.position.x
                translationY = body.position.y
                val entranceScale = .54f + .46f * reveal.value
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = reveal.value * matchAlpha
                rotationZ = if (body.dragging) 0f else ((note.persistentId.hashCode() % 7) - 3) * .32f
            }
            .zIndex(if (body.dragging) 8f else 2f)
            .pointerInput(note.id, body) {
                detectDragGestures(
                    onDragStart = {
                        body.dragging = true
                        body.velocity = Offset.Zero
                        dragSampleAt = SystemClock.uptimeMillis()
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        body.position += amount
                        val now = SystemClock.uptimeMillis()
                        val seconds = ((now - dragSampleAt).coerceAtLeast(1L)) / 1_000f
                        body.velocity = Offset(
                            (amount.x / seconds).coerceIn(-2_400f, 2_400f),
                            (amount.y / seconds).coerceIn(-2_400f, 2_400f),
                        )
                        dragSampleAt = now
                    },
                    onDragEnd = { body.dragging = false },
                    onDragCancel = { body.dragging = false; body.velocity = Offset.Zero },
                )
            },
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) if (compact) 2.dp else 3.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .55f),
        ),
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = Color(note.coverArgb),
            ) {
                Box(Modifier.fillMaxSize().padding(if (compact) 6.dp else 10.dp), contentAlignment = Alignment.Center) {
                    if (previewPage != null) PageThumbnailPreview(
                        previewPage,
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(previewPage.previewAspectRatio())
                            .clip(RoundedCornerShape(4.dp))
                            .shadow(4.dp, RoundedCornerShape(4.dp)),
                    ) else Icon(Icons.Outlined.Edit, null, Modifier.size(38.dp), tint = Color.White)
                    if (!matchesSearch) Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)))
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 7.dp else 11.dp, vertical = if (compact) 5.dp else 8.dp)) {
                Text(
                    note.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                )
                if (!compact) Text(
                    "${note.pages.size} pages · ${note.folderPath ?: "Main"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                ) else if (!veryCompact) Text(
                    "${note.pages.size}p",
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun AppSettingsDialog(
    settings: AppSettings,
    hardwareKeySequence: Int,
    hardwareKeyCode: Int,
    onSettingsChanged: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull().orEmpty().ifBlank { "Custom font.ttf" }
        val extension = displayName.substringAfterLast('.', "ttf").lowercase().takeIf { it in setOf("ttf", "otf") } ?: "ttf"
        val directory = File(context.filesDir, "fonts").apply { mkdirs() }
        val safeName = displayName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._ -]"), "_").take(64).ifBlank { "Custom font" }
        val target = generateSequence(0) { it + 1 }
            .map { suffix -> File(directory, if (suffix == 0) "$safeName.$extension" else "$safeName ($suffix).$extension") }
            .first { !it.exists() }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
                ?: error("Font file could not be opened")
            android.graphics.Typeface.createFromFile(target)
        }.onSuccess {
            onSettingsChanged(settings.copy(customFonts = (settings.customFonts + target.absolutePath).distinct()))
        }.onFailure { target.delete() }
    }
    var settingsSection by remember { mutableStateOf(AppSettingsSection.General) }
    var editingId by remember { mutableStateOf<String?>(settings.selectedThemeId) }
    var name by remember { mutableStateOf("") }
    var dark by remember { mutableStateOf(true) }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var surface by remember { mutableStateOf("") }
    var themePendingDelete by remember { mutableStateOf<AppThemePreset?>(null) }
    var waitingForFloatingNotesKey by remember { mutableStateOf(false) }
    var waitingForRadialMenuKey by remember { mutableStateOf(false) }
    var showHoverCalibration by remember { mutableStateOf(false) }
    val shortcutFocusRequester = remember { FocusRequester() }

    fun captureShortcut(keyCode: Int) {
        when {
            waitingForRadialMenuKey -> {
                onSettingsChanged(settings.copy(radialMenuKeyCode = keyCode))
                waitingForRadialMenuKey = false
            }
            waitingForFloatingNotesKey -> {
                onSettingsChanged(settings.copy(floatingNotesKeyCode = keyCode))
                waitingForFloatingNotesKey = false
            }
        }
    }

    LaunchedEffect(hardwareKeySequence) {
        if ((waitingForFloatingNotesKey || waitingForRadialMenuKey) &&
            hardwareKeySequence > 0 && hardwareKeyCode != KeyEvent.KEYCODE_UNKNOWN
        ) {
            captureShortcut(hardwareKeyCode)
        }
    }
    LaunchedEffect(waitingForFloatingNotesKey, waitingForRadialMenuKey) {
        if (waitingForFloatingNotesKey || waitingForRadialMenuKey) {
            delay(40)
            shortcutFocusRequester.requestFocus()
        }
    }

    fun edit(preset: AppThemePreset) {
        editingId = preset.id
        name = preset.name
        dark = preset.dark
        primary = formatThemeColor(preset.primaryArgb)
        secondary = formatThemeColor(preset.secondaryArgb)
        background = formatThemeColor(preset.backgroundArgb)
        surface = formatThemeColor(preset.surfaceArgb)
    }

    fun newTheme() {
        editingId = null
        name = ""
        val source = settings.activeTheme
        dark = source.dark
        primary = formatThemeColor(source.primaryArgb)
        secondary = formatThemeColor(source.secondaryArgb)
        background = formatThemeColor(source.backgroundArgb)
        surface = formatThemeColor(source.surfaceArgb)
    }

    LaunchedEffect(Unit) { edit(settings.activeTheme) }
    val parsedColors = listOf(primary, secondary, background, surface).map(::parseThemeColor)
    val colorsAreValid = parsedColors.all { it != null }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.90f)
                .focusRequester(shortcutFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val keyCode = event.nativeKeyEvent.keyCode
                    if ((waitingForFloatingNotesKey || waitingForRadialMenuKey) &&
                        event.type == KeyEventType.KeyDown && keyCode != KeyEvent.KEYCODE_UNKNOWN
                    ) {
                        captureShortcut(keyCode)
                        true
                    } else false
                }
                .pointerInteropFilter { event ->
                    val stylus = event.isFromSource(android.view.InputDevice.SOURCE_STYLUS) ||
                        event.getToolType(event.actionIndex) == android.view.MotionEvent.TOOL_TYPE_STYLUS ||
                        event.getToolType(event.actionIndex) == android.view.MotionEvent.TOOL_TYPE_ERASER
                    val buttons = event.buttonState and (
                        android.view.MotionEvent.BUTTON_STYLUS_PRIMARY or
                            android.view.MotionEvent.BUTTON_STYLUS_SECONDARY or
                            android.view.MotionEvent.BUTTON_SECONDARY or
                            android.view.MotionEvent.BUTTON_TERTIARY
                        )
                    if ((waitingForFloatingNotesKey || waitingForRadialMenuKey) && stylus && buttons != 0) {
                        captureShortcut(floatingNotesShortcutCode(Integer.lowestOneBit(buttons)))
                    }
                    false
                },
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_settings), style = MaterialTheme.typography.headlineSmall)
                        Text("Appearance and drawing limits", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close app settings") }
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSettingsSection.entries.forEach { section ->
                        val label = when (section) {
                            AppSettingsSection.General -> stringResource(R.string.general)
                            AppSettingsSection.Appearance -> stringResource(R.string.appearance)
                            AppSettingsSection.Drawing -> stringResource(R.string.drawing)
                            AppSettingsSection.Fonts -> "Fonts"
                            AppSettingsSection.Input -> stringResource(R.string.input_gestures)
                        }
                        SelectablePill(label, settingsSection == section) { settingsSection = section }
                    }
                }
                HorizontalDivider()
                if (settingsSection == AppSettingsSection.Appearance) Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.width(280.dp).fillMaxHeight().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.theme_presets), style = MaterialTheme.typography.titleSmall)
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(settings.themes, key = { _, item -> item.id }) { _, preset ->
                                Surface(
                                    onClick = {
                                        onSettingsChanged(settings.copy(selectedThemeId = preset.id))
                                        edit(preset)
                                    },
                                    color = if (preset.id == settings.selectedThemeId) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(start = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier.size(14.dp).clip(RoundedCornerShape(50))
                                                .background(Color(preset.primaryArgb)),
                                        )
                                        Text(preset.name, Modifier.padding(start = 9.dp).weight(1f), maxLines = 1)
                                        IconButton(onClick = { edit(preset) }, Modifier.size(34.dp)) {
                                            Icon(Icons.Outlined.Edit, "Edit theme", Modifier.size(17.dp))
                                        }
                                        IconButton(
                                            onClick = { if (settings.themes.size > 1) themePendingDelete = preset },
                                            modifier = Modifier.size(34.dp),
                                        ) { Icon(Icons.Outlined.Delete, "Delete theme", Modifier.size(17.dp)) }
                                    }
                                }
                            }
                        }
                        TextButton(onClick = ::newTheme, Modifier.fillMaxWidth()) { Text("Add preset") }
                        TextButton(
                            onClick = {
                                val reset = settings.copy(
                                    themes = defaultThemePresets,
                                    selectedThemeId = defaultThemePresets.first().id,
                                )
                                onSettingsChanged(reset)
                                edit(reset.activeTheme)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Restore 30 defaults") }
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(if (editingId == null) "New theme" else "Edit theme", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Preset name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeColorField("Primary", primary, { primary = it }, Modifier.weight(1f))
                            ThemeColorField("Secondary", secondary, { secondary = it }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThemeColorField("Background", background, { background = it }, Modifier.weight(1f))
                            ThemeColorField("Surface", surface, { surface = it }, Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Dark appearance")
                                Text("Also controls status-bar icon contrast", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(dark, { dark = it })
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    val preset = AppThemePreset(
                                        id = editingId ?: "custom-${UUID.randomUUID()}",
                                        name = name.trim().ifBlank { "Custom theme" },
                                        dark = dark,
                                        primaryArgb = parsedColors[0]!!,
                                        secondaryArgb = parsedColors[1]!!,
                                        backgroundArgb = parsedColors[2]!!,
                                        surfaceArgb = parsedColors[3]!!,
                                    )
                                    val existing = settings.themes.indexOfFirst { it.id == preset.id }
                                    val updated = settings.themes.toMutableList().apply {
                                        if (existing >= 0) set(existing, preset) else add(preset)
                                    }
                                    onSettingsChanged(settings.copy(themes = updated, selectedThemeId = preset.id))
                                    edit(preset)
                                },
                                enabled = colorsAreValid,
                            ) { Text(if (editingId == null) "Add theme" else "Save theme") }
                        }
                    }
                } else {
                    AppPreferencesSection(
                        settingsSection,
                        settings,
                        onSettingsChanged,
                        onImportFont = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/vnd.ms-opentype")) },
                        onRemoveFont = { path -> onSettingsChanged(settings.copy(customFonts = settings.customFonts - path)) },
                        waitingForFloatingNotesKey = waitingForFloatingNotesKey,
                        onCaptureFloatingNotesKey = {
                            waitingForRadialMenuKey = false
                            waitingForFloatingNotesKey = true
                        },
                        waitingForRadialMenuKey = waitingForRadialMenuKey,
                        onCaptureRadialMenuKey = {
                            waitingForFloatingNotesKey = false
                            waitingForRadialMenuKey = true
                        },
                        onCalibrateHoverShortcut = { showHoverCalibration = true },
                    )
                }
            }
        }
    }
    themePendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { themePendingDelete = null },
            title = { Text("Delete theme preset?") },
            text = { Text("${preset.name} will be removed. This cannot be undone unless you restore the defaults.") },
            dismissButton = { TextButton(onClick = { themePendingDelete = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    val remaining = settings.themes.filterNot { it.id == preset.id }
                    val selected = if (settings.selectedThemeId == preset.id) remaining.first().id else settings.selectedThemeId
                    onSettingsChanged(settings.copy(themes = remaining, selectedThemeId = selected))
                    edit(remaining.first { it.id == selected })
                    themePendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    if (showHoverCalibration) {
        HoverShortcutCalibrationDialog(
            settings = settings,
            onSave = { calibration ->
                onSettingsChanged(
                    settings.copy(
                        hoverShortcutEnabled = true,
                        hoverTapImpulseThresholdDp = calibration.impulseThresholdDp,
                        hoverTapMaximumDriftDp = calibration.maximumDriftDp,
                        hoverTapMaximumIntervalMillis = calibration.maximumIntervalMillis.toInt(),
                    ),
                )
                showHoverCalibration = false
            },
            onDismiss = { showHoverCalibration = false },
        )
    }
}

@Composable
private fun HoverShortcutCalibrationDialog(
    settings: AppSettings,
    onSave: (HoverCalibrationResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val attempts = remember { mutableStateListOf<HoverCalibrationResult>() }
    var calibrationView by remember { mutableStateOf<HoverCalibrationView?>(null) }
    var hovering by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Hover over the target, then record three double-taps.") }
    val combined = HoverShortcutCalibrator.combine(attempts)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.90f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 24.dp,
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Calibrate hover double-tap", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Keep the pen airborne and near the center. Tap the pen body twice when prompted.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close calibration") }
                }
                AndroidView(
                    factory = { context ->
                        HoverCalibrationView(context).also { view ->
                            calibrationView = view
                            view.onHoverChanged = { hovering = it }
                            view.onAttemptFinished = { result ->
                                if (result == null) {
                                    status = "I couldn't separate two taps. Keep the pointer steadier and try again."
                                } else {
                                    attempts += result
                                    status = if (attempts.size >= 3) {
                                        "Calibration ready. Save it or record another sample."
                                    } else {
                                        "Captured ${attempts.size} of 3. Record another double-tap."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    if (hovering) "Pen detected · $status" else "Waiting for pen hover · $status",
                    color = if (hovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                combined?.let { result ->
                    Text(
                        "Detected ${result.detectedIntervalMillis} ms rhythm · " +
                            "${"%.1f".format(result.impulseThresholdDp)} dp sensitivity",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } ?: Text(
                    "Current fallback: ${"%.1f".format(settings.hoverTapImpulseThresholdDp)} dp · " +
                        "${settings.hoverTapMaximumIntervalMillis} ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            status = "Get ready…"
                            calibrationView?.startAttempt()
                        },
                    ) { Text(if (attempts.isEmpty()) "Record attempt" else "Record again") }
                    TextButton(
                        enabled = combined != null,
                        onClick = { combined?.let(onSave) },
                    ) { Text("Save calibration") }
                }
            }
        }
    }
}

@Composable
private fun AppPreferencesSection(
    section: AppSettingsSection,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onImportFont: () -> Unit,
    onRemoveFont: (String) -> Unit,
    waitingForFloatingNotesKey: Boolean,
    onCaptureFloatingNotesKey: () -> Unit,
    waitingForRadialMenuKey: Boolean,
    onCaptureRadialMenuKey: () -> Unit,
    onCalibrateHoverShortcut: () -> Unit,
) {
    var editingColorPreset by remember { mutableStateOf<Pair<InkTool, Int>?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        when (section) {
            AppSettingsSection.General -> {
                Text(stringResource(R.string.general), style = MaterialTheme.typography.titleLarge)
                Text("UI scale · ${(settings.uiScale * 100).roundToInt()}%")
                Slider(
                    value = settings.uiScale,
                    onValueChange = { onSettingsChanged(settings.copy(uiScale = it)) },
                    valueRange = .75f..1.35f,
                    steps = 11,
                )
                Text("Changes the size of menus, tabs, toolbars, and text.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ask before inserting a page")
                        Text("Show size, orientation, color, and template choices", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(settings.askBeforeInsertingPage, { onSettingsChanged(settings.copy(askBeforeInsertingPage = it)) })
                }
            }
            AppSettingsSection.Drawing -> {
                Text(stringResource(R.string.drawing), style = MaterialTheme.typography.titleLarge)
                Text("Maximum tool widths", style = MaterialTheme.typography.titleMedium)
                InkTool.entries.forEach { tool ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tool.name, Modifier.weight(1f))
                            Text(
                                "${(settings.maximumWidthMicrons(tool) / 100f).roundToInt() / 10f} mm",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            value = settings.maximumWidthMicrons(tool),
                            onValueChange = {
                                onSettingsChanged(settings.withMaximumWidthMicrons(tool, it))
                            },
                            valueRange = 1_000f..40_000f,
                            steps = 38,
                        )
                    }
                }
                Text(
                    "Each tool keeps its own limit. The default is 5 mm per tool.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.ink_color_presets), style = MaterialTheme.typography.titleMedium)
                listOf(InkTool.Pen, InkTool.Pencil, InkTool.Highlighter).forEach { tool ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tool.name, Modifier.width(92.dp))
                        settings.colorPresets(tool).forEachIndexed { index, argb ->
                            ColorSwatch(Color(argb), onClick = { editingColorPreset = tool to index })
                        }
                        Text(stringResource(R.string.tap_color_edit), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Draw and hold correction")
                        Text("Hold at the end to create a clean line, rectangle, or ellipse", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(settings.drawAndHoldEnabled, { onSettingsChanged(settings.copy(drawAndHoldEnabled = it)) })
                }
            }
            AppSettingsSection.Fonts -> {
                Text("Custom fonts", style = MaterialTheme.typography.titleLarge)
                Text("Import TrueType or OpenType fonts. Existing text keeps its selected font.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onImportFont) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import font")
                }
                settings.customFonts.forEach { path ->
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.TextFields, null)
                            Text(File(path).nameWithoutExtension, Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onRemoveFont(path) }) { Icon(Icons.Outlined.Delete, "Remove font") }
                        }
                    }
                }
                if (settings.customFonts.isEmpty()) Text("No custom fonts imported yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppSettingsSection.Input -> {
                Text(stringResource(R.string.input_gestures), style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hover double-tap radial menu")
                        Text(
                            "While the pen is hovering, tap its body twice. No screen contact is needed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.hoverShortcutEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(hoverShortcutEnabled = it)) },
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Hover calibration", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Impulse ${"%.1f".format(settings.hoverTapImpulseThresholdDp)} dp · " +
                                    "${settings.hoverTapMaximumIntervalMillis} ms window",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = onCalibrateHoverShortcut) { Text("Calibrate") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Radial menu button shortcut")
                        Text(
                            when {
                                waitingForRadialMenuKey -> "Press a keyboard, remote, or stylus button…"
                                settings.radialMenuKeyCode == KeyEvent.KEYCODE_UNKNOWN -> "Not assigned"
                                else -> "Currently ${readableKeyName(settings.radialMenuKeyCode)}"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onCaptureRadialMenuKey) {
                        Text(if (waitingForRadialMenuKey) "Listening…" else "Change key")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Floating notes shortcut")
                        Text(
                            if (waitingForFloatingNotesKey) "Press the key you want to use…"
                            else "Currently ${readableKeyName(settings.floatingNotesKeyCode)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onCaptureFloatingNotesKey) {
                        Text(if (waitingForFloatingNotesKey) "Listening…" else "Change key")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recognize Action Maps while writing")
                        Text(
                            "Works with pen, pencil, and highlighter. A stroke is kept normally when it does not match.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.actionActivationMode != ActionActivationMode.Disabled,
                        onCheckedChange = { enabled ->
                            onSettingsChanged(settings.copy(actionActivationMode = if (enabled) ActionActivationMode.Explicit else ActionActivationMode.Disabled))
                        },
                    )
                }
            }
            AppSettingsSection.Appearance -> Unit
        }
    }
    editingColorPreset?.let { (tool, index) ->
        ThemeColorPickerDialog(
            title = "${tool.name} preset ${index + 1}",
            initialArgb = settings.colorPresets(tool)[index],
            onSelect = { color ->
                val updated = settings.colorPresets(tool).toMutableList().apply { this[index] = color }
                onSettingsChanged(settings.withColorPresets(tool, updated))
                editingColorPreset = null
            },
            onDismiss = { editingColorPreset = null },
        )
    }
}

@Composable
private fun ThemeColorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val color = parseThemeColor(value) ?: 0xFF000000.toInt()
    Surface(
        onClick = { showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(Color(color)))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text("Tap to choose a color", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Outlined.Palette, null)
        }
    }
    if (showPicker) ThemeColorPickerDialog(
        title = label,
        initialArgb = color,
        onSelect = { onValueChange(formatThemeColor(it)); showPicker = false },
        onDismiss = { showPicker = false },
    )
}

@Composable
private fun ThemeColorPickerDialog(
    title: String,
    initialArgb: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp, shadowElevation = 18.dp) {
            Column(Modifier.width(360.dp).padding(top = 18.dp)) {
                Text("Choose $title", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp))
                CircularColorPickerContent(initialArgb, onSelect, onDismiss, wheelSize = 230.dp)
            }
        }
    }
}

@Composable
private fun CircularColorPickerContent(
    initialArgb: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
    wheelSize: Dp,
) {
    val initialHsv = remember(initialArgb) { FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb, it) } }
    var hue by remember(initialArgb) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialArgb) { mutableStateOf(initialHsv[1]) }
    var brightness by remember(initialArgb) { mutableStateOf(initialHsv[2]) }
    var alpha by remember(initialArgb) { mutableIntStateOf(android.graphics.Color.alpha(initialArgb)) }
    var hex by remember(initialArgb) { mutableStateOf(formatThemeColor(initialArgb)) }

    fun updateHex() {
        hex = formatThemeColor(android.graphics.Color.HSVToColor(alpha, floatArrayOf(hue, saturation, brightness)))
    }
    fun updateWheel(point: Offset, width: Float, height: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val dx = point.x - centerX
        val dy = point.y - centerY
        val radius = minOf(width, height) / 2f
        saturation = (sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
        hue = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f)
        updateHex()
    }
    fun updateBrightness(y: Float, height: Float) {
        brightness = (1f - y / height).coerceIn(0f, 1f)
        updateHex()
    }

    val selected = parseThemeColor(hex)
        ?: android.graphics.Color.HSVToColor(alpha, floatArrayOf(hue, saturation, brightness))
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(
                Modifier.size(wheelSize).pointerInput(initialArgb) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateWheel(down.position, size.width.toFloat(), size.height.toFloat())
                        drag(down.id) { change ->
                            updateWheel(change.position, size.width.toFloat(), size.height.toFloat())
                            change.consume()
                        }
                    }
                },
            ) {
                val radius = minOf(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val native = drawContext.canvas.nativeCanvas
                val huePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.SweepGradient(
                        center.x, center.y,
                        intArrayOf(
                            android.graphics.Color.RED,
                            android.graphics.Color.YELLOW,
                            android.graphics.Color.GREEN,
                            android.graphics.Color.CYAN,
                            android.graphics.Color.BLUE,
                            android.graphics.Color.MAGENTA,
                            android.graphics.Color.RED,
                        ),
                        null,
                    )
                }
                native.drawCircle(center.x, center.y, radius, huePaint)
                val saturationPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.RadialGradient(
                        center.x, center.y, radius,
                        intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
                        null,
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                }
                native.drawCircle(center.x, center.y, radius, saturationPaint)
                val angle = Math.toRadians(hue.toDouble())
                val marker = Offset(
                    center.x + cos(angle).toFloat() * saturation * radius,
                    center.y + sin(angle).toFloat() * saturation * radius,
                )
                drawCircle(Color.Black.copy(alpha = .8f), 7.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
                drawCircle(Color.White, 5.5.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
            }
            Spacer(Modifier.width(22.dp))
            Canvas(
                Modifier.width(30.dp).height(wheelSize).clip(RoundedCornerShape(8.dp)).pointerInput(initialArgb) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateBrightness(down.position.y, size.height.toFloat())
                        drag(down.id) { change ->
                            updateBrightness(change.position.y, size.height.toFloat())
                            change.consume()
                        }
                    }
                },
            ) {
                val fullColor = android.graphics.Color.HSVToColor(alpha, floatArrayOf(hue, saturation, 1f))
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, size.height,
                        fullColor, android.graphics.Color.BLACK,
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                }
                drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                val markerY = (1f - brightness) * size.height
                drawLine(Color.White, Offset(-2f, markerY), Offset(size.width + 2f, markerY), 3.dp.toPx())
                drawLine(Color.Black, Offset(-2f, markerY), Offset(size.width + 2f, markerY), 1.dp.toPx())
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(selected)))
            OutlinedTextField(
                value = hex,
                onValueChange = { value ->
                    hex = value
                    parseThemeColor(value)?.let { parsed ->
                        alpha = android.graphics.Color.alpha(parsed)
                        FloatArray(3).also { android.graphics.Color.colorToHSV(parsed, it) }.let {
                            hue = it[0]; saturation = it[1]; brightness = it[2]
                        }
                    }
                },
                label = { Text(stringResource(R.string.hex_color)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(enabled = parseThemeColor(hex) != null, onClick = { parseThemeColor(hex)?.let(onApply) }) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}

private fun formatThemeColor(argb: Int): String = if (android.graphics.Color.alpha(argb) == 0xFF) {
    "#%06X".format(argb and 0xFFFFFF)
} else {
    "#%08X".format(argb)
}

private fun parseThemeColor(value: String): Int? = runCatching {
    val digits = value.trim().removePrefix("#")
    require(digits.length == 6 || digits.length == 8)
    val parsed = digits.toLong(16)
    if (digits.length == 6) (0xFF000000L or parsed).toInt() else parsed.toInt()
}.getOrNull()

private fun writeWavHeader(file: File, sampleRate: Int, channelCount: Int, bitsPerSample: Int) {
    val dataSize = (file.length() - 44L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val byteRate = sampleRate * channelCount * bitsPerSample / 8
    val blockAlign = channelCount * bitsPerSample / 8
    RandomAccessFile(file, "rw").use { output ->
        fun ascii(value: String) = output.write(value.toByteArray(Charsets.US_ASCII))
        fun littleInt(value: Int) {
            output.write(value and 0xFF)
            output.write(value ushr 8 and 0xFF)
            output.write(value ushr 16 and 0xFF)
            output.write(value ushr 24 and 0xFF)
        }
        fun littleShort(value: Int) {
            output.write(value and 0xFF)
            output.write(value ushr 8 and 0xFF)
        }
        output.seek(0)
        ascii("RIFF")
        littleInt(dataSize + 36)
        ascii("WAVE")
        ascii("fmt ")
        littleInt(16)
        littleShort(1)
        littleShort(channelCount)
        littleInt(sampleRate)
        littleInt(byteRate)
        littleShort(blockAlign)
        littleShort(bitsPerSample)
        ascii("data")
        littleInt(dataSize)
    }
}

@Composable
private fun EditorSettingsSheet(
    session: EditorSession,
    dockEdge: DockEdge,
    onDockEdge: (DockEdge) -> Unit,
    onDiagnostics: () -> Unit,
    onActionMaps: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.76f).fillMaxHeight(.82f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Editor settings", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close settings") }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
            Text("Current tool width", style = MaterialTheme.typography.titleSmall)
            Text("${(session.toolWidthMicrons(session.selectedTool) / 100f).roundToInt() / 10f} mm")
            Slider(value = session.widthScale, onValueChange = session::updateWidthScale, valueRange = .02f..1f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Pressure sensitivity"); Text("Vary width with stylus pressure", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(session.pressureEnabled, session::updatePressureEnabled)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Scribble to erase")
                    Text("Scratch over existing ink to remove it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(session.scribbleEraseEnabled, session::updateScribbleEraseEnabled)
            }
            if (session.scribbleEraseEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScribbleSensitivity.entries.forEach { sensitivity ->
                        Surface(
                            onClick = { session.updateScribbleSensitivity(sensitivity) },
                            color = if (session.scribbleSensitivity == sensitivity) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                        ) { Text(sensitivity.name, Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) }
                    }
                }
            }
            Text("Toolbar position", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DockEdge.entries.forEach { edge -> TextButton(onClick = { onDockEdge(edge) }) { Text(edge.name) } }
            }
            if (session.selectedTool == InkTool.Eraser) {
                TextButton(onClick = session::toggleEraserMode) {
                    Text("Eraser: ${if (session.eraserMode == EraserMode.WholeStroke) "whole stroke" else "partial"}")
                }
            }
            TextButton(onClick = onActionMaps) { Text("Manage Action Maps") }
            TextButton(onClick = onDiagnostics) { Text("Toggle stylus diagnostics") }
            Spacer(Modifier.height(24.dp))
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun ActionMapManager(
    mappings: List<ActionMapping>,
    onSave: (ActionMapping) -> Unit,
    onDelete: (ActionMapping) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(MappedAction.Undo) }
    var actionMenu by remember { mutableStateOf(false) }
    var rotationInvariant by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(.78f) }
    var padStrokes by remember { mutableStateOf<List<List<GesturePoint>>>(emptyList()) }
    var examples by remember { mutableStateOf<List<GestureDrawing>>(emptyList()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var mappingPendingDelete by remember { mutableStateOf<ActionMapping?>(null) }

    fun resetEditor() {
        editingId = null
        name = ""
        action = MappedAction.Undo
        rotationInvariant = false
        threshold = .78f
        padStrokes = emptyList()
        examples = emptyList()
    }

    fun edit(mapping: ActionMapping) {
        editingId = mapping.id
        name = mapping.name
        action = mapping.action
        rotationInvariant = mapping.rotationInvariant
        threshold = mapping.confidenceThreshold
        padStrokes = emptyList()
        examples = mapping.examples
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.90f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 20.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Action Maps", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Draw any symbol 3–5 times. Recognition works with pen, pencil, and highlighter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close Action Maps") }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.width(260.dp).fillMaxHeight().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Saved gestures", style = MaterialTheme.typography.titleSmall)
                        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(mappings, key = { _, item -> item.id }) { _, mapping ->
                                Surface(
                                    onClick = { edit(mapping) },
                                    color = if (editingId == mapping.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                            Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(mapping.name, Modifier.weight(1f), maxLines = 1)
                                IconButton(onClick = { edit(mapping) }, Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.Edit, "Edit Action Map", Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    mappingPendingDelete = mapping
                                }, Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.Delete, "Delete Action Map", Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                        TextButton(onClick = ::resetEditor, Modifier.fillMaxWidth()) { Text("New gesture") }
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
            Text(if (editingId == null) "New gesture" else "Edit gesture", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gesture name (optional)") },
                    placeholder = { Text(nextGestureName(mappings, editingId)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    Surface(
                        onClick = { actionMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) { Text(action.label, Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) }
                    DropdownMenu(actionMenu, onDismissRequest = { actionMenu = false }) {
                        MappedAction.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label) },
                                onClick = { action = choice; actionMenu = false },
                            )
                        }
                    }
                }
            }
            GestureCapturePad(padStrokes, onStrokesChanged = { padStrokes = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Examples: ${examples.size} of 5 · ${if (examples.size >= 3) "ready" else "3 required"}",
                    Modifier.weight(1f),
                )
                TextButton(onClick = { padStrokes = emptyList() }, enabled = padStrokes.isNotEmpty()) { Text("Clear") }
                TextButton(onClick = { examples = emptyList() }, enabled = examples.isNotEmpty()) { Text("Reset examples") }
                TextButton(
                    onClick = {
                        examples = examples + GestureDrawing(padStrokes)
                        padStrokes = emptyList()
                    },
                    enabled = padStrokes.isNotEmpty() && examples.size < 5,
                ) { Text("Add example") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ignore rotation")
                    Text("Match the symbol at any angle", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(rotationInvariant, { rotationInvariant = it })
            }
            Text("Minimum confidence ${(threshold * 100).roundToInt()}%")
            Slider(value = threshold, onValueChange = { threshold = it }, valueRange = .65f..0.95f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (editingId != null) TextButton(onClick = ::resetEditor) { Text("Cancel edit") }
                TextButton(
                    onClick = {
                        val resolvedName = name.trim().ifBlank { nextGestureName(mappings, editingId) }
                        onSave(ActionMapping(
                            id = editingId ?: UUID.randomUUID().toString(),
                            name = resolvedName,
                            action = action,
                            examples = examples,
                            rotationInvariant = rotationInvariant,
                            confidenceThreshold = threshold,
                        ))
                        resetEditor()
                    },
                    enabled = examples.size >= 3,
                ) { Text(if (editingId == null) "Save map" else "Update map") }
            }
                    }
                }
            }
        }
    }
    mappingPendingDelete?.let { mapping ->
        AlertDialog(
            onDismissRequest = { mappingPendingDelete = null },
            title = { Text("Delete Action Map?") },
            text = { Text("${mapping.name} will be deleted. This cannot be undone.") },
            dismissButton = { TextButton(onClick = { mappingPendingDelete = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(mapping)
                    if (editingId == mapping.id) resetEditor()
                    mappingPendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

private fun nextGestureName(mappings: List<ActionMapping>, editingId: String?): String {
    val usedNames = mappings.asSequence()
        .filterNot { it.id == editingId }
        .map { it.name.lowercase() }
        .toSet()
    var number = 1
    while ("gesture $number" in usedNames) number++
    return "Gesture $number"
}

@Composable
private fun GestureCapturePad(
    strokes: List<List<GesturePoint>>,
    onStrokesChanged: (List<List<GesturePoint>>) -> Unit,
) {
    var active by remember { mutableStateOf<List<GesturePoint>>(emptyList()) }
    Canvas(
        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101318))
            .pointerInput(strokes) {
                detectDragGestures(
                    onDragStart = { active = listOf(GesturePoint(it.x, it.y)) },
                    onDragCancel = { active = emptyList() },
                    onDragEnd = {
                        if (active.isNotEmpty()) onStrokesChanged(strokes + listOf(active))
                        active = emptyList()
                    },
                ) { change, _ ->
                    change.consume()
                    active = active + GesturePoint(change.position.x, change.position.y)
                }
            },
    ) {
        (strokes + listOf(active).filter { it.isNotEmpty() }).forEach { stroke ->
            stroke.zipWithNext().forEach { (a, b) ->
                drawLine(Color(0xFF7CA7FF), Offset(a.x, a.y), Offset(b.x, b.y), 5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DiagnosticsPanel(session: EditorSession) {
    val value = session.diagnostics
    Column(Modifier.width(230.dp).fillMaxHeight().background(Color(0xFF15181E)).padding(16.dp)) {
        Text("Stylus diagnostics", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text("${value.action} · ${value.toolType}")
        Text("Pressure ${value.rawPressure?.let { "%.3f".format(it) } ?: "—"}")
        Text("Tilt ${value.tiltRadians?.let { "%.3f".format(it) } ?: "—"}")
        Text("Pointers ${value.pointerCount}")
    }
}

@Composable
private fun SaveStatusIcon(status: SaveStatus) {
    val spinTransition = rememberInfiniteTransition(label = "save-spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "save-spin-angle",
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status is SaveStatus.Failed) {
            repeat(3) { shake.animateTo(6f, tween(50)); shake.animateTo(-6f, tween(50)) }
            shake.animateTo(0f, tween(50))
        }
    }
    Row(
        Modifier.padding(horizontal = 8.dp).graphicsLayer { translationX = shake.value },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = { fadeIn(tween(200)) + scaleIn(initialScale = 0.8f) togetherWith fadeOut(tween(150)) },
            label = "save-status",
        ) { current ->
            when (current) {
                SaveStatus.Saving -> Icon(
                    Icons.Outlined.History, null,
                    Modifier.size(14.dp).graphicsLayer { rotationZ = spinAngle },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is SaveStatus.Saved -> Icon(
                    Icons.Outlined.Bookmark, null,
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                )
                is SaveStatus.Failed -> Icon(
                    Icons.Outlined.Close, null,
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(
            when (status) { SaveStatus.Saving -> "Saving…"; is SaveStatus.Saved -> "Saved"; is SaveStatus.Failed -> "Save failed" },
            color = if (status is SaveStatus.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
