package com.mheenotes.ink.engine

import com.mheenotes.core.model.InkStroke
import com.mheenotes.core.model.PageId
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.PageRect
import com.mheenotes.core.model.StrokeId
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle

class StrokeBuilder(
    private val strokeId: StrokeId,
    private val pageId: PageId,
    private val style: StrokeStyle,
    private val zIndex: Long,
    private val smoothing: Float = 0.18f,
) {
    private val samples = mutableListOf<StrokeSample>()

    init {
        require(smoothing in 0f..1f) { "Smoothing must be between 0 and 1" }
    }

    fun append(sample: StrokeSample) {
        if (samples.isNotEmpty() && sample.elapsedMicros < samples.last().elapsedMicros) return

        val previous = samples.lastOrNull()
        if (previous == null || smoothing == 0f) {
            samples += sample
            return
        }

        val keepNew = 1f - smoothing
        samples += sample.copy(
            position = PagePoint(
                x = previous.position.x * smoothing + sample.position.x * keepNew,
                y = previous.position.y * smoothing + sample.position.y * keepNew,
            ),
        )
    }

    fun snapshot(): List<StrokeSample> = samples.toList()

    fun finish(): InkStroke {
        check(samples.isNotEmpty()) { "Cannot finish an empty stroke" }
        return InkStroke(
            id = strokeId,
            pageId = pageId,
            style = style,
            samples = samples.toList(),
            bounds = PageRect.enclosing(samples),
            zIndex = zIndex,
        )
    }
}

