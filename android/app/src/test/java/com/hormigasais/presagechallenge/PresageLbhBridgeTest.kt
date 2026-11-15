package com.hormigasais.presagechallenge

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Solo vectores sintéticos; no se conecta a SmartSpectra. */
class PresageLbhBridgeTest {
    private fun pulse(time: Long = 1_000L, confidence: Float = 90f) =
        PresageLbhBridge.SignalSample(72.5f, confidence, time)

    private fun breathing(time: Long = 1_100L, confidence: Float = 80f) =
        PresageLbhBridge.SignalSample(16f, confidence, time)

    @Test fun accepts_aligned_synthetic_pair_and_uses_experimental_encoder() {
        val result = PresageLbhBridge.encodePair(pulse(), breathing(), maxSkewMs = 100)
        assertTrue(result is PresageLbhBridge.Result.Accepted)
        assertArrayEquals(
            "ae429100004180000000000000000000".chunked(2).map { it.toInt(16).toByte() }.toByteArray(),
            (result as PresageLbhBridge.Result.Accepted).frame,
        )
    }

    @Test fun rejects_pair_outside_caller_supplied_skew() {
        val result = PresageLbhBridge.encodePair(pulse(), breathing(time = 1_101L), maxSkewMs = 100)
        assertEquals(
            PresageLbhBridge.Rejection.SAMPLES_NOT_TIME_ALIGNED,
            (result as PresageLbhBridge.Result.Rejected).reason,
        )
    }

    @Test fun rejects_nonpositive_confidence_and_nonfinite_values() {
        val noConfidence = PresageLbhBridge.encodePair(pulse(confidence = 0f), breathing(), 200)
        assertEquals(
            PresageLbhBridge.Rejection.NON_POSITIVE_CONFIDENCE,
            (noConfidence as PresageLbhBridge.Result.Rejected).reason,
        )
        val notFinite = PresageLbhBridge.encodePair(
            pulse(), breathing().copy(value = Float.NaN), 200,
        )
        assertEquals(
            PresageLbhBridge.Rejection.NON_FINITE_VALUE_OR_CONFIDENCE,
            (notFinite as PresageLbhBridge.Result.Rejected).reason,
        )
    }

    @Test fun rejects_invalid_range_and_timestamp() {
        val outOfRange = PresageLbhBridge.encodePair(
            pulse(), breathing().copy(value = 4f), 200,
        )
        assertEquals(
            PresageLbhBridge.Rejection.BREATHING_OUT_OF_EXPERIMENTAL_RANGE,
            (outOfRange as PresageLbhBridge.Result.Rejected).reason,
        )
        val invalidTime = PresageLbhBridge.encodePair(
            pulse(time = -1L), breathing(), 200,
        )
        assertEquals(
            PresageLbhBridge.Rejection.INVALID_TIMESTAMP,
            (invalidTime as PresageLbhBridge.Result.Rejected).reason,
        )
    }

    @Test fun rejects_negative_skew_configuration() {
        val result = PresageLbhBridge.encodePair(pulse(), breathing(), maxSkewMs = -1)
        assertEquals(
            PresageLbhBridge.Rejection.INVALID_MAX_SKEW,
            (result as PresageLbhBridge.Result.Rejected).reason,
        )
    }
}
