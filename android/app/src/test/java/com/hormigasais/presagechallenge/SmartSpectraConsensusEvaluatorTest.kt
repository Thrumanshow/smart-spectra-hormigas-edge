package com.hormigasais.presagechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmartSpectraConsensusEvaluatorTest {
    private val baseMicros = 1_800_000_000_000_000L

    private fun field(value: Float, timestampMicros: Long, stable: Boolean = true, confidence: Float = 80f) =
        SmartSpectraFieldMapper.MeasurementWithConfidenceFields(
            value = value,
            confidence = confidence,
            timestampMicros = timestampMicros,
            stable = stable,
        )

    private fun feed(evaluator: SmartSpectraConsensusEvaluator, second: Int, stable: Boolean = true) =
        evaluator.ingest(
            pulse = field(72f, baseMicros + second * 1_000_000L, stable),
            breathing = field(16f, baseMicros + second * 1_000_000L + 50_000L, stable),
        )

    private fun evaluator(windowMs: Long = 20_000L) = SmartSpectraConsensusEvaluator(
        SmartSpectraConsensusEvaluator.Policy(
            windowDurationMs = windowMs,
            maxPairSkewMs = 100L, // mirrors the existing provisional app pairing tolerance
            maxInterSampleGapMs = 3_000L, // test fixture only; not medical evidence
        )
    )

    @Test fun twentyAndThirtySecondWindowsRequireTheirFullElapsedSpan() {
        for (duration in listOf(20, 30)) {
            val instance = evaluator(duration * 1_000L)
            var result = feed(instance, 0)
            for (second in 1 until duration) result = feed(instance, second)
            assertEquals(SmartSpectraConsensusEvaluator.State.COLLECTING, result.state)
            assertNull(result.acceptedPulse)
            result = feed(instance, duration)
            assertEquals(SmartSpectraConsensusEvaluator.State.SESSION_STABLE_EXPERIMENTAL, result.state)
            assertEquals(duration * 1_000L, result.coverageMs)
            assertNotNull(result.acceptedPulse)
            assertNotNull(result.acceptedBreathing)
        }
    }

    @Test fun gapStartsANewWindowInsteadOfReusingOldSamples() {
        val instance = evaluator()
        feed(instance, 0)
        feed(instance, 1)
        val afterGap = feed(instance, 5)
        assertEquals(SmartSpectraConsensusEvaluator.State.COLLECTING, afterGap.state)
        assertEquals("gap_restarted_window", afterGap.reason)
        assertEquals(1, afterGap.sampleCount)
        assertNull(afterGap.acceptedPulse)
    }

    @Test fun sdkUnstableSampleClearsWindow() {
        val instance = evaluator()
        feed(instance, 0)
        val result = feed(instance, 1, stable = false)
        assertEquals(SmartSpectraConsensusEvaluator.State.RESET, result.state)
        assertEquals("sdk_marked_unstable", result.reason)
        assertEquals(0, result.sampleCount)
    }

    @Test fun duplicateOrReversedTimestampClearsWindow() {
        val instance = evaluator()
        feed(instance, 2)
        val result = feed(instance, 1)
        assertEquals(SmartSpectraConsensusEvaluator.State.RESET, result.state)
        assertEquals("timestamp_not_increasing", result.reason)
    }

    @Test fun nonPositiveConfidenceClearsWindow() {
        val instance = evaluator()
        feed(instance, 0)
        val badPulse = field(72f, baseMicros + 1_000_000L, confidence = 0f)
        val goodBreath = field(16f, baseMicros + 1_050_000L)
        val result = instance.ingest(badPulse, goodBreath)
        assertEquals(SmartSpectraConsensusEvaluator.State.RESET, result.state)
        assertEquals("invalid_value_or_confidence", result.reason)
    }

    @Test fun pairSkewOutsideExistingToleranceClearsWindow() {
        val instance = evaluator()
        val pulse = field(72f, baseMicros)
        val breath = field(16f, baseMicros + 101_000L)
        val result = instance.ingest(pulse, breath)
        assertEquals(SmartSpectraConsensusEvaluator.State.RESET, result.state)
        assertEquals("pair_skew_exceeded", result.reason)
    }
}
