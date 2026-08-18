package com.mheenotes.app.editor

import kotlin.math.hypot

data class StylusHoverSample(
    val eventTimeMillis: Long,
    val x: Float,
    val y: Float,
    val distance: Float? = null,
    val tiltRadians: Float? = null,
    val orientationRadians: Float? = null,
    val deviceDescriptor: String = "",
)

data class HoverShortcutConfig(
    val impulseThresholdDp: Float = 2.2f,
    val maximumDriftDp: Float = 28f,
    val maximumIntervalMillis: Long = 520L,
)

data class HoverCalibrationResult(
    val impulseThresholdDp: Float,
    val maximumDriftDp: Float,
    val maximumIntervalMillis: Long,
    val detectedIntervalMillis: Long,
)

class HoverDoubleTapRecognizer(
    private var config: HoverShortcutConfig,
    private var density: Float,
) {
    private data class MotionStep(val at: Long, val distanceDp: Float)
    private data class Impulse(
        val anchorX: Float,
        val anchorY: Float,
        val startedAt: Long,
        val peakX: Float,
        val peakY: Float,
        val peakAt: Long,
        val peakDistanceDp: Float,
    )
    private data class CompletedTap(val at: Long, val anchorX: Float, val anchorY: Float)

    private val recentMotion = ArrayDeque<MotionStep>()
    private var previous: StylusHoverSample? = null
    private var impulse: Impulse? = null
    private var firstTap: CompletedTap? = null
    private var cooldownUntil = Long.MIN_VALUE

    fun update(config: HoverShortcutConfig, density: Float) {
        this.config = config
        this.density = density.coerceAtLeast(.1f)
    }

    fun reset() {
        clearGesture()
        cooldownUntil = Long.MIN_VALUE
    }

    fun onHoverExit() {
        clearGesture()
    }

    private fun clearGesture() {
        recentMotion.clear()
        previous = null
        impulse = null
        firstTap = null
    }

    fun add(sample: StylusHoverSample): Boolean {
        val before = previous
        previous = sample
        if (before == null) return false
        val dt = sample.eventTimeMillis - before.eventTimeMillis
        if (dt !in 1L..120L) {
            recentMotion.clear()
            impulse = null
            firstTap = null
            return false
        }

        val now = sample.eventTimeMillis
        while (recentMotion.firstOrNull()?.at?.let { now - it > StabilityWindowMillis } == true) {
            recentMotion.removeFirst()
        }
        val stepDp = hypot(sample.x - before.x, sample.y - before.y) / density.coerceAtLeast(.1f)
        val stableMotionBeforeImpulse = recentMotion.sumOf { it.distanceDp.toDouble() }.toFloat()

        if (now < cooldownUntil) {
            recentMotion.addLast(MotionStep(now, stepDp))
            return false
        }

        impulse?.let { active ->
            val age = now - active.peakAt
            val distanceFromAnchorDp = hypot(sample.x - active.anchorX, sample.y - active.anchorY) / density
            if (distanceFromAnchorDp > active.peakDistanceDp && now - active.startedAt <= MaximumImpulseMillis) {
                impulse = active.copy(
                    peakX = sample.x,
                    peakY = sample.y,
                    peakAt = now,
                    peakDistanceDp = distanceFromAnchorDp,
                )
                return false
            }
            val recoilStepDp = hypot(sample.x - active.peakX, sample.y - active.peakY) / density
            val returnedFraction = 1f - distanceFromAnchorDp / active.peakDistanceDp.coerceAtLeast(.01f)
            val recoiled = age in MinimumRecoilMillis..MaximumRecoilMillis &&
                recoilStepDp >= maxOf(config.impulseThresholdDp * .28f, .55f) &&
                returnedFraction >= MinimumReturnFraction
            if (recoiled) {
                impulse = null
                recentMotion.clear()
                return completeTap(
                    CompletedTap(now, active.anchorX, active.anchorY),
                )
            }
            if (age > MaximumRecoilMillis) {
                impulse = null
                firstTap = null
                recentMotion.clear()
            } else {
                return false
            }
        }

        firstTap?.let { tap ->
            val driftDp = hypot(sample.x - tap.anchorX, sample.y - tap.anchorY) / density
            if (now - tap.at > config.maximumIntervalMillis || driftDp > allowedAnchorDriftDp()) {
                firstTap = null
                recentMotion.clear()
            }
        }

        val dwellMillis = recentMotion.firstOrNull()?.let { now - it.at } ?: 0L
        val firstTapPending = firstTap != null
        val motionLimitDp = if (firstTapPending) {
            maxOf(config.impulseThresholdDp * 1.8f, 4.5f)
        } else {
            maxOf(config.impulseThresholdDp * 1.25f, 3.2f)
        }
        val enoughDwell = firstTapPending || dwellMillis >= MinimumStationaryDwellMillis
        val enoughTapSpacing = firstTap?.let { now - it.at >= MinimumTapIntervalMillis } ?: true
        if (enoughDwell && enoughTapSpacing && dt <= MaximumImpulseSampleMillis &&
            stepDp >= config.impulseThresholdDp && stableMotionBeforeImpulse <= motionLimitDp
        ) {
            impulse = Impulse(
                anchorX = before.x,
                anchorY = before.y,
                startedAt = now,
                peakX = sample.x,
                peakY = sample.y,
                peakAt = now,
                peakDistanceDp = stepDp,
            )
            return false
        }

        recentMotion.addLast(MotionStep(now, stepDp))
        return false
    }

    private fun completeTap(tap: CompletedTap): Boolean {
        val first = firstTap
        if (first == null) {
            firstTap = tap
            return false
        }
        val interval = tap.at - first.at
        val driftDp = hypot(tap.anchorX - first.anchorX, tap.anchorY - first.anchorY) / density
        if (interval in MinimumTapIntervalMillis..config.maximumIntervalMillis &&
            driftDp <= allowedAnchorDriftDp()
        ) {
            firstTap = null
            cooldownUntil = tap.at + TriggerCooldownMillis
            recentMotion.clear()
            return true
        }
        if (interval > config.maximumIntervalMillis) firstTap = tap
        return false
    }

    private fun allowedAnchorDriftDp(): Float = minOf(
        config.maximumDriftDp,
        maxOf(10f, config.impulseThresholdDp * 4.5f),
    )

    private companion object {
        const val StabilityWindowMillis = 180L
        const val MinimumStationaryDwellMillis = 130L
        const val MaximumImpulseSampleMillis = 80L
        const val MaximumImpulseMillis = 85L
        const val MinimumRecoilMillis = 4L
        const val MaximumRecoilMillis = 115L
        const val MinimumReturnFraction = .30f
        const val MinimumTapIntervalMillis = 78L
        const val TriggerCooldownMillis = 1_200L
    }
}

object HoverShortcutCalibrator {
    private data class Peak(val at: Long, val distanceDp: Float, val x: Float, val y: Float)

    fun analyze(samples: List<StylusHoverSample>, density: Float): HoverCalibrationResult? {
        if (samples.size < 6) return null
        val safeDensity = density.coerceAtLeast(.1f)
        val steps = samples.zipWithNext().mapNotNull { (before, after) ->
            val dt = after.eventTimeMillis - before.eventTimeMillis
            if (dt !in 1L..100L) null else Peak(
                at = after.eventTimeMillis,
                distanceDp = hypot(after.x - before.x, after.y - before.y) / safeDensity,
                x = after.x,
                y = after.y,
            )
        }
        if (steps.size < 4) return null
        val sortedDistances = steps.map(Peak::distanceDp).sorted()
        val noise = sortedDistances[sortedDistances.size / 2]
        val minimumPeak = maxOf(.65f, noise * 2.1f)
        val peaks = steps.sortedByDescending(Peak::distanceDp).fold(mutableListOf<Peak>()) { selected, candidate ->
            if (candidate.distanceDp >= minimumPeak && selected.none { kotlin.math.abs(it.at - candidate.at) < 72L }) {
                selected += candidate
            }
            selected
        }.sortedBy(Peak::at)
        val pair = peaks.indices.asSequence().flatMap { first ->
            ((first + 1)..peaks.lastIndex).asSequence().map { second -> peaks[first] to peaks[second] }
        }.filter { (first, second) -> second.at - first.at in 78L..750L }
            .minByOrNull { (first, second) -> second.at - first.at }
            ?: return null
        val (first, second) = pair
        val interval = second.at - first.at
        val origin = samples.first()
        val observedDrift = samples.maxOf { hypot(it.x - origin.x, it.y - origin.y) / safeDensity }
        return HoverCalibrationResult(
            impulseThresholdDp = (minOf(first.distanceDp, second.distanceDp) * .58f).coerceIn(.7f, 14f),
            maximumDriftDp = (observedDrift * 1.8f + 5f).coerceIn(12f, 56f),
            maximumIntervalMillis = (interval * 1.65f).toLong().coerceIn(300L, 800L),
            detectedIntervalMillis = interval,
        )
    }

    fun combine(results: List<HoverCalibrationResult>): HoverCalibrationResult? {
        if (results.isEmpty()) return null
        fun median(values: List<Float>) = values.sorted()[values.size / 2]
        fun medianLong(values: List<Long>) = values.sorted()[values.size / 2]
        return HoverCalibrationResult(
            impulseThresholdDp = median(results.map(HoverCalibrationResult::impulseThresholdDp)),
            maximumDriftDp = median(results.map(HoverCalibrationResult::maximumDriftDp)),
            maximumIntervalMillis = medianLong(results.map(HoverCalibrationResult::maximumIntervalMillis)),
            detectedIntervalMillis = medianLong(results.map(HoverCalibrationResult::detectedIntervalMillis)),
        )
    }
}
