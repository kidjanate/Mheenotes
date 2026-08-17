package com.mheenotes.app.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.SystemClock
import android.view.MotionEvent
import android.view.InputDevice
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.rendering.android.view.ViewStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import com.mheenotes.core.model.PagePoint
import com.mheenotes.core.model.InkTool
import com.mheenotes.core.model.StrokeSample
import com.mheenotes.core.model.StrokeStyle
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.abs

data class FinishedInkStroke(
    val id: InProgressStrokeId,
    val renderStroke: Stroke,
    val samples: List<StrokeSample>,
    val style: StrokeStyle,
    val actionCandidate: Boolean = false,
)

data class CommittedInkStroke(
    val stroke: Stroke,
    val strokeToViewTransform: Matrix,
)

enum class FingerObjectGesture { Canvas, SelectOnly, Move }

class InkAuthoringOverlayView(context: Context) : FrameLayout(context) {
    private val inkView = InProgressStrokesView(context)
    private val committedStrokeRenderer = ViewStrokeRenderer(CanvasStrokeRenderer.create(), this)
    private val predictor = MotionEventPredictor.newInstance(inkView)
    private val stylesByStrokeId = mutableMapOf<InProgressStrokeId, StrokeStyle>()
    private val capturedSamplesByStrokeId = mutableMapOf<InProgressStrokeId, MutableList<StrokeSample>>()
    private val actionCandidateByStrokeId = mutableMapOf<InProgressStrokeId, Boolean>()
    private val strokeStartedAtMillis = mutableMapOf<InProgressStrokeId, Long>()
    private var activePointerId: Int? = null
    private var activeStrokeId: InProgressStrokeId? = null
    private val deliverHeldStrokeProgress = Runnable {
        val strokeId = activeStrokeId ?: return@Runnable
        val samples = capturedSamplesByStrokeId[strokeId]?.toList().orEmpty()
        val last = samples.lastOrNull() ?: return@Runnable
        val elapsed = (SystemClock.uptimeMillis() - (strokeStartedAtMillis[strokeId] ?: return@Runnable)) * 1_000L
        val stationary = samples + listOf(
            last.copy(elapsedMicros = (elapsed - 160_000L).coerceAtLeast(last.elapsedMicros + 1L)),
            last.copy(elapsedMicros = elapsed.coerceAtLeast(last.elapsedMicros + 2L)),
        )
        val replaced = onStrokeProgress(
            strokeId,
            stationary,
            stylesByStrokeId[strokeId] ?: style,
            actionCandidateByStrokeId[strokeId] ?: false,
        )
        if (replaced) suppressActiveStroke(strokeId)
    }
    private var activeEraserPointerId: Int? = null
    private var activeObjectStylusPointerId: Int? = null
    private var objectStylusPosition: SingleFingerGesture? = null
    private var fingerGesture: FingerGesture? = null
    private var singleFingerGesture: SingleFingerGesture? = null
    private var fingerTapPointerId: Int? = null
    private var fingerTapStart: SingleFingerGesture? = null
    private var fingerTapMaximumDistance = 0f
    private var activeFingerObjectGesture = FingerObjectGesture.Canvas
    private var observedStylusButtonState = 0
    private var fingerVelocityTracker: VelocityTracker? = null
    private val fingerScroller = OverScroller(context)
    private val minimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maximumFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private var lastFlingY = 0
    private val deliverFingerFling = object : Runnable {
        override fun run() {
            if (!fingerScroller.computeScrollOffset()) return
            val currentY = fingerScroller.currY
            val delta = currentY - lastFlingY
            lastFlingY = currentY
            if (delta != 0) onOneFingerScroll(delta.toFloat())
            if (!fingerScroller.isFinished) postOnAnimation(this)
        }
    }
    private var pendingFingerScrollY = 0f
    private var fingerScrollFrameScheduled = false
    private val deliverFingerScroll = Runnable {
        fingerScrollFrameScheduled = false
        val delta = pendingFingerScrollY
        pendingFingerScrollY = 0f
        if (delta != 0f) onOneFingerScroll(delta)
    }

    var viewport = ViewportTransform(1f, 0f, 0f, 1f, 1f)
        set(value) {
            field = value
            invalidate()
        }
    var committedStrokes: List<CommittedInkStroke> = emptyList()
        set(value) {
            field = value
            invalidate()
        }
    var minimumScale = 1f
    var stylusInputEnabled = true
    var alwaysRecognizeActionGestures = false
    var style = StrokeStyle()
    var onMotionEventObserved: (MotionEvent) -> Unit = {}
    var onStrokeFinished: (FinishedInkStroke) -> Unit = {}
    var onStrokeProgress: (InProgressStrokeId, List<StrokeSample>, StrokeStyle, Boolean) -> Boolean = { _, _, _, _ -> false }
    var onErase: (PagePoint) -> Unit = {}
    var onEraseFinished: () -> Unit = {}
    var onViewportChanged: (ViewportTransform) -> Unit = {}
    var onOneFingerScroll: (Float) -> Unit = {}
    var onCanvasTouched: () -> Unit = {}
    var onCanvasTouchAt: (PagePoint) -> Unit = {}
    var onFingerTap: (PagePoint) -> Unit = {}
    var onFingerObjectDown: (PagePoint) -> FingerObjectGesture = { FingerObjectGesture.Canvas }
    var onFingerObjectDrag: (Float, Float) -> Unit = { _, _ -> }
    var onFingerObjectUp: () -> Unit = {}
    var onObjectStylusDown: (PagePoint) -> Boolean = { false }
    var onObjectStylusDrag: (Float, Float) -> Unit = { _, _ -> }
    var onObjectStylusUp: () -> Unit = {}
    var onStylusButtonPressed: (Int) -> Unit = {}

    init {
        setWillNotDraw(false)
        addView(
            inkView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        inkView.eagerInit()
        inkView.addFinishedStrokesListener(
            object : InProgressStrokesFinishedListener {
                override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                    strokes.forEach { (strokeId, stroke) ->
                        val completedStyle = stylesByStrokeId.remove(strokeId) ?: style
                        strokeStartedAtMillis.remove(strokeId)
                        val captured = capturedSamplesByStrokeId.remove(strokeId).orEmpty()
                        val samples = captured.takeUnless { it.isEmpty() }
                            ?: stroke.toDomainSamples().orEmpty()
                        if (samples.isNotEmpty()) {
                            onStrokeFinished(FinishedInkStroke(
                                strokeId, stroke, samples, completedStyle,
                                actionCandidateByStrokeId.remove(strokeId) ?: false,
                            ))
                        } else {
                            actionCandidateByStrokeId.remove(strokeId)
                            inkView.removeFinishedStrokes(setOf(strokeId))
                        }
                    }
                    invalidate()
                }
            },
        )
        inkView.setOnTouchListener { _, event -> handleTouch(event) }
        inkView.setOnGenericMotionListener { _, event ->
            observeStylusButtons(event)
            false
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        observeStylusButtons(event)
        return super.onGenericMotionEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        committedStrokeRenderer.drawWithStrokes(canvas) { scope ->
            committedStrokes.forEach { placedStroke ->
                val saveCount = canvas.save()
                canvas.concat(placedStroke.strokeToViewTransform)
                scope.drawStroke(placedStroke.stroke)
                canvas.restoreToCount(saveCount)
            }
        }
    }

    fun removeFinishedStroke(id: InProgressStrokeId) {
        inkView.postOnAnimation {
            inkView.removeFinishedStrokes(setOf(id))
            invalidate()
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        observeStylusButtons(event)
        onMotionEventObserved(event)
        observeFingerTap(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            stopFingerFling()
            onCanvasTouched()
            val point = viewport.toPage(event.x, event.y)
            onCanvasTouchAt(point)
            if (event.getToolType(event.actionIndex) == MotionEvent.TOOL_TYPE_FINGER) {
                activeFingerObjectGesture = onFingerObjectDown(point)
            }
        }

        if ((event.flags and MotionEvent.FLAG_CANCELED) != 0) {
            cancelActive(event)
            if (activeEraserPointerId != null) onEraseFinished()
            activeEraserPointerId = null
            return true
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            -> if (event.isStylus(event.actionIndex)) {
                fingerGesture = null
                when {
                    startObjectStylusIfHandled(event) -> true
                    !stylusInputEnabled -> true
                    style.tool == InkTool.Eraser -> startEraser(event)
                    else -> startIfStylus(event)
                }
            } else if (activePointerId != null) {
                true
            } else {
                updateFingerGesture(event, pointerLifted = false)
            }

            MotionEvent.ACTION_MOVE -> if (activeObjectStylusPointerId != null) {
                updateObjectStylus(event)
            } else if (activeEraserPointerId != null) {
                updateEraser(event)
            } else if (activePointerId != null) {
                updateActive(event)
            } else {
                updateFingerGesture(event, pointerLifted = false)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            -> if (activeObjectStylusPointerId != null) {
                finishObjectStylus(event)
            } else if (activeEraserPointerId != null) {
                finishEraser(event)
            } else if (activePointerId != null) {
                finishIfActive(event)
            } else {
                updateFingerGesture(event, pointerLifted = true)
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelActive(event)
                if (activeObjectStylusPointerId != null) onObjectStylusUp()
                activeObjectStylusPointerId = null
                objectStylusPosition = null
                if (activeFingerObjectGesture != FingerObjectGesture.Canvas) onFingerObjectUp()
                activeFingerObjectGesture = FingerObjectGesture.Canvas
                activeEraserPointerId = null
                onEraseFinished()
                fingerGesture = null
                recycleFingerVelocityTracker()
                true
            }

            else -> activePointerId != null
        }
    }

    private fun observeStylusButtons(event: MotionEvent) {
        val isStylus = event.isFromSource(InputDevice.SOURCE_STYLUS) ||
            (0 until event.pointerCount).any { index ->
                event.getToolType(index) == MotionEvent.TOOL_TYPE_STYLUS ||
                    event.getToolType(index) == MotionEvent.TOOL_TYPE_ERASER
            }
        if (!isStylus) return
        val supportedButtons = event.buttonState and (
            MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY or
                MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY
            )
        val newlyPressed = supportedButtons and observedStylusButtonState.inv()
        observedStylusButtonState = supportedButtons
        if (newlyPressed != 0) onStylusButtonPressed(Integer.lowestOneBit(newlyPressed))
    }

    private fun startObjectStylusIfHandled(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val point = viewport.toPage(event.getX(index), event.getY(index))
        val handled = onObjectStylusDown(point)
        if (handled) {
            activeObjectStylusPointerId = event.getPointerId(index)
            objectStylusPosition = SingleFingerGesture(event.getX(index), event.getY(index))
        }
        return handled
    }

    private fun updateObjectStylus(event: MotionEvent): Boolean {
        val pointerId = activeObjectStylusPointerId ?: return false
        val index = event.findPointerIndex(pointerId)
        if (index < 0) return true
        val current = SingleFingerGesture(event.getX(index), event.getY(index))
        objectStylusPosition?.let { previous ->
            onObjectStylusDrag(
                (current.x - previous.x) / viewport.scale,
                (current.y - previous.y) / viewport.scale,
            )
        }
        objectStylusPosition = current
        return true
    }

    private fun finishObjectStylus(event: MotionEvent): Boolean {
        val pointerId = activeObjectStylusPointerId ?: return false
        if (event.getPointerId(event.actionIndex) == pointerId) {
            onObjectStylusUp()
            activeObjectStylusPointerId = null
            objectStylusPosition = null
        }
        return true
    }

    private fun startIfStylus(event: MotionEvent): Boolean {
        if (activePointerId != null) return true
        val pointerIndex = event.actionIndex
        if (!event.isStylus(pointerIndex)) return false

        val pointerId = event.getPointerId(pointerIndex)
        parent?.requestDisallowInterceptTouchEvent(true)
        val eventToWorld = viewport.screenToPageMatrix()
        val brush = style.toAndroidXBrush()
        inkView.requestUnbufferedDispatch(event)
        predictor.record(event)
        val strokeId = inkView.startStroke(
            event,
            pointerId,
            brush,
            eventToWorld,
            Matrix(),
        )
        stylesByStrokeId[strokeId] = style
        strokeStartedAtMillis[strokeId] = event.eventTime
        activeStrokeId = strokeId
        capturedSamplesByStrokeId[strokeId] = mutableListOf<StrokeSample>().also {
            appendSamples(event, pointerId, it, event.eventTime)
        }
        actionCandidateByStrokeId[strokeId] = alwaysRecognizeActionGestures || event.buttonState != 0 ||
            (0 until event.pointerCount).any { event.getToolType(it) == MotionEvent.TOOL_TYPE_FINGER }
        activePointerId = pointerId
        scheduleHeldStrokeProgress()
        return true
    }

    private fun startEraser(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        parent?.requestDisallowInterceptTouchEvent(true)
        activeEraserPointerId = pointerId
        inkView.requestUnbufferedDispatch(event)
        onErase(viewport.toPage(event.getX(pointerIndex), event.getY(pointerIndex)))
        return true
    }

    private fun updateEraser(event: MotionEvent): Boolean {
        val pointerId = activeEraserPointerId ?: return false
        val pointerIndex = event.findPointerIndex(pointerId)
        if (pointerIndex < 0) {
            activeEraserPointerId = null
            onEraseFinished()
            return true
        }
        for (historyIndex in 0 until event.historySize) {
            onErase(
                viewport.toPage(
                    event.getHistoricalX(pointerIndex, historyIndex),
                    event.getHistoricalY(pointerIndex, historyIndex),
                ),
            )
        }
        onErase(viewport.toPage(event.getX(pointerIndex), event.getY(pointerIndex)))
        return true
    }

    private fun finishEraser(event: MotionEvent): Boolean {
        val pointerId = activeEraserPointerId ?: return false
        if (event.getPointerId(event.actionIndex) == pointerId) {
            onErase(viewport.toPage(event.getX(event.actionIndex), event.getY(event.actionIndex)))
            activeEraserPointerId = null
            onEraseFinished()
        }
        return true
    }

    private fun updateActive(event: MotionEvent): Boolean {
        val pointerId = activePointerId ?: return false
        if (event.findPointerIndex(pointerId) < 0) {
            cancelActive(event)
            return true
        }

        predictor.record(event)
        var progress: Pair<InProgressStrokeId, List<StrokeSample>>? = null
        activeStrokeId?.let { strokeId ->
            capturedSamplesByStrokeId[strokeId]?.let { samples ->
                appendSamples(event, pointerId, samples, strokeStartedAtMillis[strokeId] ?: event.downTime)
                progress = strokeId to samples.toList()
            }
        }
        val prediction = predictor.predict()
        try {
            inkView.addToStroke(event, pointerId, prediction)
        } finally {
            prediction?.recycle()
        }
        progress?.let { (strokeId, samples) ->
            val replaced = onStrokeProgress(
                strokeId,
                samples,
                stylesByStrokeId[strokeId] ?: style,
                actionCandidateByStrokeId[strokeId] ?: false,
            )
            if (replaced) suppressActiveStroke(strokeId) else scheduleHeldStrokeProgress()
        }
        return true
    }

    private fun finishIfActive(event: MotionEvent): Boolean {
        val pointerId = activePointerId ?: return false
        if (event.getPointerId(event.actionIndex) != pointerId) return true

        predictor.record(event)
        activeStrokeId?.let { strokeId ->
            capturedSamplesByStrokeId[strokeId]?.let { samples ->
                val startedAt = strokeStartedAtMillis[strokeId] ?: event.downTime
                val elapsedMicros = (event.eventTime - startedAt).coerceAtLeast(0L) * 1_000L
                samples.lastOrNull()?.takeIf { elapsedMicros - it.elapsedMicros >= 320_000L }?.let { last ->
                    samples += last.copy(elapsedMicros = (elapsedMicros - 160_000L).coerceAtLeast(last.elapsedMicros + 1L))
                }
                appendSamples(event, pointerId, samples, startedAt)
            }
        }
        inkView.finishStroke(event, pointerId)
        removeCallbacks(deliverHeldStrokeProgress)
        activePointerId = null
        activeStrokeId = null
        activeEraserPointerId = null
        return true
    }

    private fun cancelActive(event: MotionEvent?) {
        val pointerId = activePointerId ?: return
        removeCallbacks(deliverHeldStrokeProgress)
        if (event != null) {
            inkView.cancelStroke(event, pointerId)
        } else {
            inkView.cancelUnfinishedStrokes()
        }
        activePointerId = null
        activeStrokeId = null
        stylesByStrokeId.clear()
        strokeStartedAtMillis.clear()
        capturedSamplesByStrokeId.clear()
        actionCandidateByStrokeId.clear()
    }

    private fun updateFingerGesture(event: MotionEvent, pointerLifted: Boolean): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            fingerVelocityTracker?.run {
                addMovement(event)
                computeCurrentVelocity(1_000)
                if (viewport.scale <= minimumScale * 1.01f) startFingerFling(yVelocity)
            }
            recycleFingerVelocityTracker()
        }
        val fingerIndices = buildList {
            for (index in 0 until event.pointerCount) {
                if (pointerLifted && index == event.actionIndex) continue
                if (event.getToolType(index) == MotionEvent.TOOL_TYPE_FINGER) add(index)
            }
        }

        if (activeFingerObjectGesture != FingerObjectGesture.Canvas && fingerIndices.size >= 2) {
            onFingerObjectUp()
            activeFingerObjectGesture = FingerObjectGesture.Canvas
            singleFingerGesture = null
        }

        if (activeFingerObjectGesture != FingerObjectGesture.Canvas) {
            if (event.actionMasked == MotionEvent.ACTION_UP || fingerIndices.isEmpty()) {
                onFingerObjectUp()
                activeFingerObjectGesture = FingerObjectGesture.Canvas
                singleFingerGesture = null
                recycleFingerVelocityTracker()
                return true
            }
            val index = fingerIndices.single()
            val current = SingleFingerGesture(event.getX(index), event.getY(index))
            val previous = singleFingerGesture
            singleFingerGesture = current
            if (previous != null && event.actionMasked == MotionEvent.ACTION_MOVE &&
                activeFingerObjectGesture == FingerObjectGesture.Move
            ) {
                onFingerObjectDrag(
                    (current.x - previous.x) / viewport.scale,
                    (current.y - previous.y) / viewport.scale,
                )
            }
            return true
        }

        if (fingerIndices.size == 1) {
            fingerGesture = null
            val index = fingerIndices.single()
            val current = SingleFingerGesture(event.getX(index), event.getY(index))
            val previous = singleFingerGesture
            singleFingerGesture = current
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                stopFingerFling()
                recycleFingerVelocityTracker()
                fingerVelocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
            } else {
                fingerVelocityTracker?.addMovement(event)
            }
            if (previous != null && event.actionMasked == MotionEvent.ACTION_MOVE) {
                val dx = current.x - previous.x
                val dy = current.y - previous.y
                if (viewport.scale > minimumScale * 1.01f) {
                    val candidate = viewport.copy(
                        offsetX = viewport.offsetX + dx,
                        offsetY = viewport.offsetY + dy,
                    ).clamped(width.toFloat(), height.toFloat())
                    viewport = candidate
                    onViewportChanged(candidate)
                } else {
                    enqueueFingerScroll(dy)
                }
            }
            if (pointerLifted) singleFingerGesture = null
            return true
        }

        if (fingerIndices.size < 2) {
            singleFingerGesture = null
            fingerGesture = null
            return true
        }
        singleFingerGesture = null
        stopFingerFling()
        recycleFingerVelocityTracker()
        parent?.requestDisallowInterceptTouchEvent(true)

        val first = fingerIndices[0]
        val second = fingerIndices[1]
        val centerX = (event.getX(first) + event.getX(second)) / 2f
        val centerY = (event.getY(first) + event.getY(second)) / 2f
        val distance = hypot(
            event.getX(first) - event.getX(second),
            event.getY(first) - event.getY(second),
        ).coerceAtLeast(1f)
        val previous = fingerGesture
        fingerGesture = FingerGesture(centerX, centerY, distance)
        if (previous == null || event.actionMasked != MotionEvent.ACTION_MOVE) return true

        val scaleFactor = (distance / previous.distance).coerceIn(0.75f, 1.25f)
        val newScale = (viewport.scale * scaleFactor).coerceIn(minimumScale, minimumScale * 8f)
        val worldX = (previous.centerX - viewport.offsetX) / viewport.scale
        val worldY = (previous.centerY - viewport.offsetY) / viewport.scale
        val candidate = viewport.copy(
            scale = newScale,
            offsetX = centerX - worldX * newScale,
            offsetY = centerY - worldY * newScale,
        ).clamped(width.toFloat(), height.toFloat())
        viewport = candidate
        onViewportChanged(candidate)
        return true
    }

    override fun onDetachedFromWindow() {
        cancelActive(null)
        if (activeObjectStylusPointerId != null) onObjectStylusUp()
        activeObjectStylusPointerId = null
        objectStylusPosition = null
        removeCallbacks(deliverFingerScroll)
        stopFingerFling()
        recycleFingerVelocityTracker()
        super.onDetachedFromWindow()
    }

    private fun enqueueFingerScroll(deltaY: Float) {
        pendingFingerScrollY += deltaY
        if (!fingerScrollFrameScheduled) {
            fingerScrollFrameScheduled = true
            postOnAnimation(deliverFingerScroll)
        }
    }

    private fun scheduleHeldStrokeProgress() {
        removeCallbacks(deliverHeldStrokeProgress)
        postDelayed(deliverHeldStrokeProgress, 430L)
    }

    private fun suppressActiveStroke(strokeId: InProgressStrokeId) {
        if (activeStrokeId != strokeId) return
        removeCallbacks(deliverHeldStrokeProgress)
        inkView.cancelUnfinishedStrokes()
        stylesByStrokeId.remove(strokeId)
        strokeStartedAtMillis.remove(strokeId)
        capturedSamplesByStrokeId.remove(strokeId)
        actionCandidateByStrokeId.remove(strokeId)
        activeStrokeId = null
        activePointerId = null
        invalidate()
    }

    private fun observeFingerTap(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (event.getToolType(event.actionIndex) == MotionEvent.TOOL_TYPE_FINGER) {
                fingerTapPointerId = event.getPointerId(event.actionIndex)
                fingerTapStart = SingleFingerGesture(event.x, event.y)
                fingerTapMaximumDistance = 0f
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                fingerTapPointerId = null
                fingerTapStart = null
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerId = fingerTapPointerId ?: return
                val index = event.findPointerIndex(pointerId)
                val start = fingerTapStart
                if (index >= 0 && start != null) {
                    fingerTapMaximumDistance = max(
                        fingerTapMaximumDistance,
                        hypot(event.getX(index) - start.x, event.getY(index) - start.y),
                    )
                }
            }
            MotionEvent.ACTION_UP -> {
                val pointerId = fingerTapPointerId
                if (pointerId != null && event.getPointerId(event.actionIndex) == pointerId &&
                    fingerTapMaximumDistance <= ViewConfiguration.get(context).scaledTouchSlop * 1.25f
                ) {
                    onFingerTap(viewport.toPage(event.x, event.y))
                }
                fingerTapPointerId = null
                fingerTapStart = null
                fingerTapMaximumDistance = 0f
            }
            MotionEvent.ACTION_CANCEL -> {
                fingerTapPointerId = null
                fingerTapStart = null
                fingerTapMaximumDistance = 0f
            }
        }
    }

    private fun recycleFingerVelocityTracker() {
        fingerVelocityTracker?.recycle()
        fingerVelocityTracker = null
    }

    private fun startFingerFling(rawVelocityY: Float) {
        if (abs(rawVelocityY) < minimumFlingVelocity) return
        stopFingerFling()
        lastFlingY = 0
        fingerScroller.fling(
            0,
            0,
            0,
            rawVelocityY.toInt().coerceIn(-maximumFlingVelocity, maximumFlingVelocity),
            0,
            0,
            Int.MIN_VALUE,
            Int.MAX_VALUE,
        )
        postOnAnimation(deliverFingerFling)
    }

    private fun stopFingerFling() {
        removeCallbacks(deliverFingerFling)
        if (!fingerScroller.isFinished) fingerScroller.forceFinished(true)
    }

    private fun Stroke.toDomainSamples(): List<StrokeSample>? = runCatching {
        var lastTime = 0L
        List(inputs.size) { index ->
            val input = inputs[index]
            val elapsed = max(lastTime, input.elapsedTimeMillis * 1_000L)
            lastTime = elapsed
            StrokeSample(
                position = PagePoint(input.x, input.y),
                elapsedMicros = elapsed,
                pressure = input.pressure.takeIf { input.hasPressure && it.isFinite() },
                tiltRadians = input.tiltRadians.takeIf { input.hasTilt && it.isFinite() },
                orientationRadians = input.orientationRadians.takeIf {
                    input.hasOrientation && it.isFinite()
                },
            )
        }
    }.getOrNull()

    private fun appendSamples(
        event: MotionEvent,
        pointerId: Int,
        target: MutableList<StrokeSample>,
        startedAtMillis: Long,
    ) {
        val index = event.findPointerIndex(pointerId)
        if (index < 0) return
        fun append(x: Float, y: Float, pressure: Float, tilt: Float, orientation: Float, eventTime: Long) {
            val time = max(
                (target.lastOrNull()?.elapsedMicros ?: -1L) + 1L,
                (eventTime - startedAtMillis).coerceAtLeast(0L) * 1_000L,
            )
            target += StrokeSample(
                position = viewport.toPage(x, y), elapsedMicros = time,
                pressure = pressure.takeIf(Float::isFinite),
                tiltRadians = tilt.takeIf(Float::isFinite),
                orientationRadians = orientation.takeIf(Float::isFinite),
            )
        }
        for (history in 0 until event.historySize) {
            append(
                event.getHistoricalX(index, history), event.getHistoricalY(index, history),
                event.getHistoricalPressure(index, history),
                event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, index, history),
                event.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, index, history),
                event.getHistoricalEventTime(history),
            )
        }
        append(
            event.getX(index), event.getY(index), event.getPressure(index),
            event.getAxisValue(MotionEvent.AXIS_TILT, index), event.getAxisValue(MotionEvent.AXIS_ORIENTATION, index),
            event.eventTime,
        )
    }

    private fun MotionEvent.isStylus(pointerIndex: Int): Boolean =
        getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_STYLUS ||
            getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_ERASER

    private fun ViewportTransform.screenToPageMatrix(): Matrix = Matrix().apply {
        val inverseScale = 1f / scale
        setValues(
            floatArrayOf(
                inverseScale, 0f, -offsetX * inverseScale,
                0f, inverseScale, -offsetY * inverseScale,
                0f, 0f, 1f,
            ),
        )
    }

    private fun ViewportTransform.clamped(viewWidth: Float, viewHeight: Float): ViewportTransform {
        val margin = 48f
        val contentWidth = pageWidth * scale
        val contentHeight = pageHeight * scale
        val clampedX = if (contentWidth <= viewWidth) {
            (viewWidth - contentWidth) / 2f
        } else {
            offsetX.coerceIn(viewWidth - margin - contentWidth, margin)
        }
        val clampedY = if (contentHeight <= viewHeight) {
            (viewHeight - contentHeight) / 2f
        } else {
            offsetY.coerceIn(viewHeight - margin - contentHeight, margin)
        }
        return copy(offsetX = clampedX, offsetY = clampedY)
    }

    private data class FingerGesture(
        val centerX: Float,
        val centerY: Float,
        val distance: Float,
    )

    private data class SingleFingerGesture(val x: Float, val y: Float)
}
