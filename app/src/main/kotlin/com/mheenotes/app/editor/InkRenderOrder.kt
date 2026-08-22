package com.mheenotes.app.editor

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.InkTool

/**
 * Highlighter ink is paper annotation, so it always sits below writing ink even when it was
 * authored later. Keeping this ordering in one place prevents the editor, thumbnails, and
 * flattened exports from disagreeing about the same page.
 */
internal fun Iterable<InkStroke>.inInkRenderOrder(): List<InkStroke> = sortedWith(
    compareBy<InkStroke> { if (it.style.tool == InkTool.Highlighter) 0 else 1 }
        .thenBy(InkStroke::zIndex),
)
