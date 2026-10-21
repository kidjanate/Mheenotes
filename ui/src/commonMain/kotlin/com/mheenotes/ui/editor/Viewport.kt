package com.mheenotes.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mheenotes.core.model.Id

/**
 * Screen mapping published by the canvas: screen = document * scale + translation.
 * Compose overlays read it only in layout/draw lambdas so pans never recompose them.
 */
class ViewportState {
    var scale by mutableFloatStateOf(1f)
    var tx by mutableFloatStateOf(0f)
    var ty by mutableFloatStateOf(0f)
    var layout by mutableStateOf<PageLayout?>(null)

    fun screenX(pageId: Id, x: Float): Float {
        val l = layout ?: return 0f
        val i = l.indexOf(pageId)
        return if (i < 0) 0f else (l.left(i) + x) * scale + tx
    }

    fun screenY(pageId: Id, y: Float): Float {
        val l = layout ?: return 0f
        val i = l.indexOf(pageId)
        return if (i < 0) 0f else (l.tops[i] + y) * scale + ty
    }
}
