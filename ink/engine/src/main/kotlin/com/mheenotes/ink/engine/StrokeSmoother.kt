package com.mheenotes.ink.engine

import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.StrokeSample

enum class SmoothingLevel(val retainedPrevious: Float) {
    Raw(0f),
    Low(0.10f),
    Medium(0.22f),
    High(0.36f),
}

object StrokeSmoother {
    fun smooth(samples: List<StrokeSample>, level: SmoothingLevel): List<StrokeSample> {
        if (samples.size < 3 || level == SmoothingLevel.Raw) return samples
        val previousWeight = level.retainedPrevious
        val newWeight = 1f - previousWeight
        val result = ArrayList<StrokeSample>(samples.size)
        result += samples.first()
        for (index in 1 until samples.lastIndex) {
            val previous = result.last()
            val current = samples[index]
            result += current.copy(
                position = PagePoint(
                    previous.position.x * previousWeight + current.position.x * newWeight,
                    previous.position.y * previousWeight + current.position.y * newWeight,
                ),
            )
        }
        result += samples.last()
        return result
    }
}

