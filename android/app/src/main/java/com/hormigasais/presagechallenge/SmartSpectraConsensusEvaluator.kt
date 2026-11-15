package com.hormigasais.presagechallenge

import java.util.ArrayDeque

/**
 * Experimental, in-memory gate over paired SmartSpectra fields.
 * It does not encode frames, log readings, or claim clinical validity.
 * Window duration is a research parameter (20s or 30s), not a medical rule.
 */
class SmartSpectraConsensusEvaluator(
    private val policy: Policy,
) {
    data class Policy(
        val windowDurationMs: Long,
        val maxPairSkewMs: Long,
        val maxInterSampleGapMs: Long,
    ) {
        init {
            require(windowDurationMs == 20_000L || windowDurationMs == 30_000L) {
                "Only the 20s and 30s experimental windows are supported"
            }
            require(maxPairSkewMs >= 0L)
            require(maxInterSampleGapMs > 0L)
        }
    }

    enum class State { COLLECTING, SESSION_STABLE_EXPERIMENTAL, RESET }

    data class Update(
        val state: State,
        val reason: String,
        val sampleCount: Int,
        val coverageMs: Long,
        /** Present only when a full qualifying window has been observed. */
        val acceptedPulse: SmartSpectraFieldMapper.MeasurementWithConfidenceFields? = null,
        val acceptedBreathing: SmartSpectraFieldMapper.MeasurementWithConfidenceFields? = null,
    )

    private data class PairPoint(
        val eventTimestampMicros: Long,
        val pulse: SmartSpectraFieldMapper.MeasurementWithConfidenceFields,
        val breathing: SmartSpectraFieldMapper.MeasurementWithConfidenceFields,
    )

    private val points = ArrayDeque<PairPoint>()
    private var lastEventTimestampMicros: Long? = null

    /**
     * Supply the latest pulse/breathing fields after temporal pairing checks.
     * Uses source epoch-microsecond timestamps; no wall-clock or hash is used.
     */
    fun ingest(
        pulse: SmartSpectraFieldMapper.MeasurementWithConfidenceFields,
        breathing: SmartSpectraFieldMapper.MeasurementWithConfidenceFields,
    ): Update {
        val pulseTime = pulse.timestampMicros
        val breathingTime = breathing.timestampMicros
        if (pulseTime <= 0L || breathingTime <= 0L) return reset("invalid_timestamp")
        if (!pulse.value.isFinite() || !breathing.value.isFinite() ||
            !pulse.confidence.isFinite() || !breathing.confidence.isFinite() ||
            pulse.confidence <= 0f || breathing.confidence <= 0f
        ) return reset("invalid_value_or_confidence")
        if (!pulse.stable || !breathing.stable) return reset("sdk_marked_unstable")

        val skewMicros = if (pulseTime >= breathingTime) pulseTime - breathingTime else breathingTime - pulseTime
        if (skewMicros > policy.maxPairSkewMs * 1_000L) return reset("pair_skew_exceeded")
        val eventTime = maxOf(pulseTime, breathingTime)
        val previous = lastEventTimestampMicros
        if (previous != null && eventTime <= previous) return reset("timestamp_not_increasing")

        val windowMicros = policy.windowDurationMs * 1_000L
        val gapMicros = policy.maxInterSampleGapMs * 1_000L
        if (previous != null && eventTime - previous > gapMicros) {
            points.clear()
            points.addLast(PairPoint(eventTime, pulse, breathing))
            lastEventTimestampMicros = eventTime
            return update(State.COLLECTING, "gap_restarted_window", null)
        }

        points.addLast(PairPoint(eventTime, pulse, breathing))
        lastEventTimestampMicros = eventTime
        while (points.isNotEmpty() && eventTime - points.first().eventTimestampMicros > windowMicros) {
            points.removeFirst()
        }

        val coverageMicros = if (points.isEmpty()) 0L else eventTime - points.first().eventTimestampMicros
        if (coverageMicros < windowMicros) return update(State.COLLECTING, "window_incomplete", null)

        val latest = points.last()
        return update(State.SESSION_STABLE_EXPERIMENTAL, "full_window_sdk_stable", latest)
    }

    /** Clear transient samples at session start/stop/pause. */
    fun clear() {
        points.clear()
        lastEventTimestampMicros = null
    }

    private fun reset(reason: String): Update {
        clear()
        return Update(State.RESET, reason, 0, 0L)
    }

    private fun update(state: State, reason: String, accepted: PairPoint?): Update {
        val first = points.firstOrNull()?.eventTimestampMicros
        val last = points.lastOrNull()?.eventTimestampMicros
        val coverage = if (first == null || last == null) 0L else (last - first) / 1_000L
        return Update(
            state = state,
            reason = reason,
            sampleCount = points.size,
            coverageMs = coverage,
            acceptedPulse = accepted?.pulse,
            acceptedBreathing = accepted?.breathing,
        )
    }
}
