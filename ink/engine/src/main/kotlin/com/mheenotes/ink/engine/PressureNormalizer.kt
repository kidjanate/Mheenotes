package com.mheenotes.ink.engine

import com.mheenotes.core.model.PressureCurve
import com.mheenotes.core.model.StrokeStyle
import kotlin.math.pow

object PressureNormalizer {
    private const val NominalPressure = 0.5f

    fun normalize(rawPressure: Float?, curve: PressureCurve): Float {
        if (rawPressure == null || !rawPressure.isFinite()) return NominalPressure

        val range = curve.maximumUsefulPressure - curve.minimumUsefulPressure
        val linear = ((rawPressure - curve.minimumUsefulPressure) / range).coerceIn(0f, 1f)
        return linear.pow(curve.exponent)
    }

    fun width(rawPressure: Float?, style: StrokeStyle): Float {
        if (!style.pressureEnabled) {
            return (style.minimumWidth + style.maximumWidth) / 2f
        }
        val response = normalize(rawPressure, style.pressureCurve)
        return style.minimumWidth + (style.maximumWidth - style.minimumWidth) * response
    }
}

