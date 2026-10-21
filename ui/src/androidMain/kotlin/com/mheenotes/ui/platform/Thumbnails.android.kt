package com.mheenotes.ui.platform

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.mheenotes.core.model.Id
import com.mheenotes.core.model.Notebook
import com.mheenotes.ui.AppGraph
import com.mheenotes.ui.design.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Decoded covers, keyed by notebook id and thumbnail version. */
object ThumbCache {
    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val io = Dispatchers.IO.limitedParallelism(3)

    fun key(id: Id, version: Long) = "$id:$version"
    fun peek(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bmp: ImageBitmap) { cache.put(key, bmp) }

    suspend fun load(id: Id, version: Long): ImageBitmap? = withContext(io) {
        val f = File(AppGraph.files.thumb(id))
        if (!f.exists()) return@withContext null
        BitmapFactory.decodeFile(f.path)?.asImageBitmap()?.also { cache.put(key(id, version), it) }
    }
}

@Composable
actual fun NotebookThumbnail(n: Notebook, modifier: Modifier) {
    val key = ThumbCache.key(n.id, n.thumbVersion)
    val cached = remember(key) { ThumbCache.peek(key) }
    var bmp by remember(key) { mutableStateOf(cached) }
    val alpha = remember(key) { Animatable(if (cached != null) 1f else 0f) }
    if (bmp == null) {
        LaunchedEffect(key) {
            bmp = ThumbCache.load(n.id, n.thumbVersion)
            if (bmp != null) alpha.animateTo(1f, Motion.fade())
        }
    }
    Box(modifier.background(Color.White)) {
        bmp?.let {
            Image(
                it, null,
                Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value },
                contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
            )
        }
    }
}
