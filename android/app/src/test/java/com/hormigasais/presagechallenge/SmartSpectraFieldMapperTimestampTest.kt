package com.hormigasais.presagechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Datos sintéticos con escala/confianza documentadas; no son lecturas reales. */
class SmartSpectraFieldMapperTimestampTest {
    @Test fun field_mapper_converts_epoch_microseconds_and_rejects_missing_time() {
        assertEquals(1_700_000_000_123L,
            SmartSpectraFieldMapper.epochMicrosToMillis(1_700_000_000_123_456L))
        assertNull(SmartSpectraFieldMapper.epochMicrosToMillis(0L))
        assertNull(SmartSpectraFieldMapper.epochMicrosToMillis(-1L))
        assertNull(SmartSpectraFieldMapper.fromSdkFields(
            SmartSpectraFieldMapper.MeasurementWithConfidenceFields(72.5f, 80f, 0L, true)
        ))
    }

    @Test fun sdk_fields_map_to_signal_model_without_rescaling_confidence() {
        val result = SmartSpectraFieldMapper.fromSdkFields(
            SmartSpectraFieldMapper.MeasurementWithConfidenceFields(
                value = 72.5f,
                confidence = 80f,
                timestampMicros = 1_700_000_000_123_456L,
                stable = false,
            )
        )!!
        assertEquals(72.5f, result.signal.value, 0f)
        assertEquals(80f, result.signal.confidence, 0f)
        assertEquals(1_700_000_000_123L, result.signal.timestampMs)
        assertEquals(false, result.stable)
    }

    @Test fun validates_skew_in_microseconds_then_encodes_expected_lbh_frame() {
        val t = 1_700_000_000_000_000L
        val pulse = SmartSpectraFieldMapper.MeasurementWithConfidenceFields(72.5f, 80f, t, true)
        val breathingWithin = SmartSpectraFieldMapper.MeasurementWithConfidenceFields(16f, 75f, t + 100_000L, true)
        val accepted = SmartSpectraFieldMapper.encodeSdkPair(pulse, breathingWithin, maxSkewMs = 100L)
        assertTrue(accepted is PresageLbhBridge.Result.Accepted)
        assertEquals(
            "ae429100004180000000000000000000",
            (accepted as PresageLbhBridge.Result.Accepted).frame.joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            },
        )

        // 100.001 ms debe rechazarse aunque truncar ambas marcas a ms pudiera ocultar la fracción.
        val breathingOutside = breathingWithin.copy(timestampMicros = t + 100_001L)
        val rejected = SmartSpectraFieldMapper.encodeSdkPair(pulse, breathingOutside, maxSkewMs = 100L)
        assertEquals(
            PresageLbhBridge.Rejection.SAMPLES_NOT_TIME_ALIGNED,
            (rejected as PresageLbhBridge.Result.Rejected).reason,
        )
    }

    @Test fun rejects_overflowing_skew_setting() {
        val t = 1_700_000_000_000_000L
        val sample = SmartSpectraFieldMapper.MeasurementWithConfidenceFields(72f, 80f, t, true)
        val result = SmartSpectraFieldMapper.encodeSdkPair(sample, sample, Long.MAX_VALUE)
        assertEquals(
            PresageLbhBridge.Rejection.INVALID_MAX_SKEW,
            (result as PresageLbhBridge.Result.Rejected).reason,
        )
    }
}
