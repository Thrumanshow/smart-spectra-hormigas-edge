package com.hormigasais.presagechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas sintéticas; no se leen muestras biométricas reales. */
class SmartSpectraEpochMapperTest {
    @Test fun converts_epoch_microseconds_to_milliseconds() {
        assertEquals(1_700_000_000_123L,
            SmartSpectraEpochMapper.epochMicrosToMillis(1_700_000_000_123_456L))
    }

    @Test fun rejects_zero_or_negative_timestamp() {
        assertNull(SmartSpectraEpochMapper.epochMicrosToMillis(0L))
        assertNull(SmartSpectraEpochMapper.epochMicrosToMillis(-1L))
        assertNull(SmartSpectraEpochMapper.fromMeasurementWithConfidenceFields(72f, 80f, 0L, true))
    }

    @Test fun maps_sdk_fields_with_confidence_percentage_and_stable_metadata() {
        val mapped = SmartSpectraEpochMapper.fromMeasurementWithConfidenceFields(
            value = 72.5f,
            confidence = 80f,
            timestampMicros = 1_700_000_000_123_456L,
            stable = false,
        )!!
        assertEquals(72.5f, mapped.signal.value, 0f)
        assertEquals(80f, mapped.signal.confidence, 0f)
        assertEquals(1_700_000_000_123L, mapped.signal.timestampMs)
        assertEquals(false, mapped.stable)
    }

    @Test fun epoch_millisecond_samples_flow_to_pairing_and_encoder() {
        val baseUs = 1_700_000_000_000_000L
        val pulse = SmartSpectraEpochMapper.fromMeasurementWithConfidenceFields(
            72.5f, 80f, baseUs, true,
        )!!
        val breath = SmartSpectraEpochMapper.fromMeasurementWithConfidenceFields(
            16f, 75f, baseUs + 50_000L, true,
        )!!
        val within = PresageLbhBridge.encodePair(pulse.signal, breath.signal, maxSkewMs = 100L)
        assertTrue(within is PresageLbhBridge.Result.Accepted)
        assertEquals(
            "ae429100004180000000000000000000",
            (within as PresageLbhBridge.Result.Accepted).frame.joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            },
        )

        val tooFar = SmartSpectraEpochMapper.fromMeasurementWithConfidenceFields(
            16f, 75f, baseUs + 150_000L, true,
        )!!
        val outside = PresageLbhBridge.encodePair(pulse.signal, tooFar.signal, maxSkewMs = 100L)
        assertEquals(
            PresageLbhBridge.Rejection.SAMPLES_NOT_TIME_ALIGNED,
            (outside as PresageLbhBridge.Result.Rejected).reason,
        )
    }
}
