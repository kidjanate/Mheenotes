package com.mheenotes.ui.editor

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.X
import com.mheenotes.core.doc.NotebookDocument
import com.mheenotes.core.model.Id
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.Card
import com.mheenotes.ui.design.IconButton
import com.mheenotes.ui.design.LocalOverlays
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Motion
import com.mheenotes.ui.design.Radii
import com.mheenotes.ui.design.Spinner
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Renders a lassoed area to an image and inserts, saves or shares it. Platform-provided. */
expect object LassoCapture {
    suspend fun render(tab: EditorTab, doc: NotebookDocument, pageId: Id, lasso: FloatArray): ImageBitmap?
    suspend fun insert(tab: EditorTab, doc: NotebookDocument, image: ImageBitmap, pageId: Id, lasso: FloatArray)
    suspend fun saveToPhotos(image: ImageBitmap, title: String): Boolean
    suspend fun share(image: ImageBitmap, title: String)
}

/** What a capture was taken from, so "insert" can place the copy next to it. */
class CaptureRequest(val pageId: Id, val lasso: FloatArray)

/** Floating bar for a lassoed area that has nothing selectable in it. */
@Composable
fun CaptureAreaBar(tab: EditorTab, onCapture: () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        tab.captureArea != null && tab.selection == null,
        enter = scaleIn(Motion.bouncy(), initialScale = 0.8f) + fadeIn(Motion.fade()),
        exit = scaleOut(Motion.snappy(), targetScale = 0.9f) + fadeOut(Motion.fade()),
    ) {
        var w by remember { mutableStateOf(0) }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxW = constraints.maxWidth
            Card(
                Modifier
                    .onSizeChanged { w = it.width }
                    .offset {
                        val b = tab.captureAreaScreenBounds() ?: return@offset IntOffset.Zero
                        val x = (b.centerX - w / 2).roundToInt().coerceIn(16, (maxW - w - 16).coerceAtLeast(16))
                        val above = b.top - 64.dp.toPx()
                        IntOffset(x, (if (above > 80.dp.toPx()) above else b.bottom + 16.dp.toPx()).roundToInt())
                    },
                shape = RoundedCornerShape(Radii.lg), color = M.colors.elevated, elevation = 14.dp,
            ) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(stringResource(Res.string.capture), onCapture, icon = Lucide.Camera, style = ButtonStyle.Ghost, compact = true)
                    IconButton(Lucide.X, { tab.captureArea = null }, tint = M.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun CaptureModal(tab: EditorTab, doc: NotebookDocument, request: CaptureRequest?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val overlays = LocalOverlays.current
    var image by remember(request) { mutableStateOf<ImageBitmap?>(null) }
    var busy by remember(request) { mutableStateOf(false) }
    LaunchedEffect(request) {
        if (request != null) image = LassoCapture.render(tab, doc, request.pageId, request.lasso)
    }
    val title = AppGraph.library.notebook(tab.notebookId)?.title ?: stringResource(Res.string.app_name)
    Modal(request != null, onDismiss, width = 560.dp) {
        ModalTitle(stringResource(Res.string.capture_title))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 360.dp).clip(RoundedCornerShape(Radii.md)).background(M.colors.fill).padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val img = image
            if (img == null) Spinner() else Image(img, null, Modifier.fillMaxWidth().heightIn(max = 336.dp), contentScale = ContentScale.Fit)
        }
        val savedText = stringResource(Res.string.saved_to_photos)
        val failedText = stringResource(Res.string.export_failed)
        ModalActions {
            val img = image
            val enabled = img != null && !busy
            Button(stringResource(Res.string.share), { if (img != null) scope.launch { LassoCapture.share(img, title) } }, icon = Lucide.Share2, enabled = enabled, compact = true)
            Button(stringResource(Res.string.save_to_photos), {
                if (img != null) scope.launch {
                    busy = true
                    overlays.toast(if (LassoCapture.saveToPhotos(img, title)) savedText else failedText)
                    busy = false
                    onDismiss()
                }
            }, icon = Lucide.Download, enabled = enabled, compact = true)
            Spacer(Modifier.width(4.dp))
            Button(stringResource(Res.string.insert_into_note), {
                val req = request
                if (img != null && req != null) scope.launch {
                    busy = true
                    LassoCapture.insert(tab, doc, img, req.pageId, req.lasso)
                    busy = false
                    onDismiss()
                }
            }, icon = Lucide.ImagePlus, style = ButtonStyle.Primary, enabled = enabled, compact = true)
        }
    }
}
