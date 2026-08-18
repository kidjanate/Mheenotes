package com.mheenotes.app.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View

class HoverCalibrationView(context: Context) : View(context) {
    var onAttemptFinished: (HoverCalibrationResult?) -> Unit = {}
    var onHoverChanged: (Boolean) -> Unit = {}

    private val samples = mutableListOf<StylusHoverSample>()
    private val trace = ArrayDeque<StylusHoverSample>()
    private var hovering = false
    private var preparing = false
    private var recording = false
    private val beginRecording = Runnable {
        preparing = false
        recording = true
        samples.clear()
        invalidate()
        postDelayed(finishRecording, 1_500L)
    }
    private val finishRecording = Runnable {
        recording = false
        val result = HoverShortcutCalibrator.analyze(samples.toList(), resources.displayMetrics.density)
        onAttemptFinished(result)
        invalidate()
    }

    init {
        isFocusable = true
        setBackgroundColor(Color.rgb(16, 20, 27))
    }

    fun startAttempt() {
        removeCallbacks(beginRecording)
        removeCallbacks(finishRecording)
        preparing = true
        recording = false
        samples.clear()
        invalidate()
        postDelayed(beginRecording, 900L)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_STYLUS) &&
            (0 until event.pointerCount).none { index ->
                event.getToolType(index) == MotionEvent.TOOL_TYPE_STYLUS ||
                    event.getToolType(index) == MotionEvent.TOOL_TYPE_ERASER
            }
        ) return super.onGenericMotionEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                val index = event.actionIndex.coerceIn(0, event.pointerCount - 1)
                if (!hovering) {
                    hovering = true
                    onHoverChanged(true)
                }
                val sample = StylusHoverSample(
                    eventTimeMillis = event.eventTime,
                    x = event.getX(index),
                    y = event.getY(index),
                    distance = event.getAxisValue(MotionEvent.AXIS_DISTANCE, index).takeIf(Float::isFinite),
                    tiltRadians = event.getAxisValue(MotionEvent.AXIS_TILT, index).takeIf(Float::isFinite),
                    orientationRadians = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, index).takeIf(Float::isFinite),
                    deviceDescriptor = InputDevice.getDevice(event.deviceId)?.descriptor.orEmpty(),
                )
                trace.addLast(sample)
                while (trace.size > 48) trace.removeFirst()
                if (recording) samples += sample
                invalidate()
                return true
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                hovering = false
                onHoverChanged(false)
                invalidate()
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val centerX = width / 2f
        val centerY = height / 2f
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 130, 180, 255)
            strokeWidth = density
        }
        val spacing = 32f * density
        var x = centerX % spacing
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), grid); x += spacing }
        var y = centerY % spacing
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, grid); y += spacing }

        val target = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(94, 177, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawCircle(centerX, centerY, 42f * density, target)
        canvas.drawCircle(centerX, centerY, 7f * density, target)

        if (trace.size > 1) {
            val path = Path()
            trace.forEachIndexed { index, sample ->
                if (index == 0) path.moveTo(sample.x, sample.y) else path.lineTo(sample.x, sample.y)
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 125, 211, 252)
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            })
        }
        trace.lastOrNull()?.let { sample ->
            canvas.drawCircle(sample.x, sample.y, 11f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(55, 94, 177, 255)
            })
            canvas.drawCircle(sample.x, sample.y, 7f * density, target)
        }

        val status = when {
            preparing -> "Get ready…"
            recording -> "DOUBLE-TAP THE PEN BODY NOW"
            !hovering -> "Bring the pen into hover range"
            else -> "Keep the pointer near the target"
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (recording) Color.rgb(255, 213, 79) else Color.WHITE
            textSize = 16f * density
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(status, centerX, 31f * density, text)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(beginRecording)
        removeCallbacks(finishRecording)
        super.onDetachedFromWindow()
    }
}
