package com.mheenotes.ui.editor

import com.mheenotes.ui.res.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mheenotes.core.geometry.Rect
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.ImageItem
import com.mheenotes.core.model.withCrop
import com.mheenotes.ui.design.Button
import com.mheenotes.ui.design.ButtonStyle
import com.mheenotes.ui.design.M
import com.mheenotes.ui.design.Modal
import com.mheenotes.ui.design.ModalActions
import com.mheenotes.ui.design.ModalTitle
import com.mheenotes.ui.design.Spinner
import kotlin.math.abs

/** Loads an image asset for previews (platform cache). */
expect suspend fun loadImageAsset(notebookId: Id, assetId: Id, maxPixels: Int): ImageBitmap?

/** Crop window drawn over the whole image; drag the corners or the middle. */
@Composable
fun CropModal(tab: EditorTab, image: ImageItem?, onDismiss: () -> Unit, onApply: (ImageItem) -> Unit) {
    var bmp by remember(image) { mutableStateOf<ImageBitmap?>(null) }
    var crop by remember(image) { mutableStateOf(image?.crop ?: Rect(0f, 0f, 1f, 1f)) }
    androidx.compose.runtime.LaunchedEffect(image) { if (image != null) bmp = loadImageAsset(tab.notebookId, image.assetId, 2048) }
    Modal(image != null, onDismiss, width = 640.dp) {
        ModalTitle(stringResource(Res.string.crop))
        val b = bmp
        if (b == null) {
            Box(Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) { Spinner() }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = 460.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(b.width.toFloat() / b.height)) {
                    Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    val accent = M.colors.accent
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            var handle = -1 // 0..3 corners, 4 = move
                            detectDragGestures(
                                onDragStart = { p ->
                                    val c = crop
                                    val corners = listOf(c.left to c.top, c.right to c.top, c.right to c.bottom, c.left to c.bottom)
                                    handle = corners.indexOfFirst { (x, y) -> abs(x * size.width - p.x) < 40 && abs(y * size.height - p.y) < 40 }
                                    if (handle < 0 && c.contains(p.x / size.width, p.y / size.height)) handle = 4
                                },
                            ) { ch, d ->
                                ch.consume()
                                val dx = d.x / size.width; val dy = d.y / size.height
                                val c = crop
                                val min = 0.05f
                                crop = when (handle) {
                                    0 -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - min), top = (c.top + dy).coerceIn(0f, c.bottom - min))
                                    1 -> c.copy(right = (c.right + dx).coerceIn(c.left + min, 1f), top = (c.top + dy).coerceIn(0f, c.bottom - min))
                                    2 -> c.copy(right = (c.right + dx).coerceIn(c.left + min, 1f), bottom = (c.bottom + dy).coerceIn(c.top + min, 1f))
                                    3 -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - min), bottom = (c.bottom + dy).coerceIn(c.top + min, 1f))
                                    4 -> {
                                        val mx = dx.coerceIn(-c.left, 1f - c.right); val my = dy.coerceIn(-c.top, 1f - c.bottom)
                                        c.offset(mx, my)
                                    }
                                    else -> c
                                }
                            }
                        },
                    ) {
                        val c = crop
                        val l = c.left * size.width; val t = c.top * size.height; val r = c.right * size.width; val bt = c.bottom * size.height
                        val dim = Color(0x99000000)
                        // Darken everything outside the crop window.
                        drawRect(dim, Offset.Zero, Size(size.width, t))
                        drawRect(dim, Offset(0f, bt), Size(size.width, size.height - bt))
                        drawRect(dim, Offset(0f, t), Size(l, bt - t))
                        drawRect(dim, Offset(r, t), Size(size.width - r, bt - t))
                        drawRect(Color.White, Offset(l, t), Size(r - l, bt - t), style = Stroke(2.dp.toPx()))
                        for ((x, y) in listOf(l to t, r to t, r to bt, l to bt)) {
                            drawCircle(Color.White, 9.dp.toPx(), Offset(x, y))
                            drawCircle(accent, 9.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                        }
                    }
                }
            }
        }
        ModalActions {
            Button(stringResource(Res.string.cancel), onDismiss)
            Button(stringResource(Res.string.done), {
                val img = image ?: return@Button
                onApply(img.withCrop(crop))
                onDismiss()
            }, style = ButtonStyle.Primary, enabled = bmp != null)
        }
    }
}
