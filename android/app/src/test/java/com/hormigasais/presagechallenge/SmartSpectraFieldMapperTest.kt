package com.hormigasais.presagechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vectores sintéticos: no se construyen objetos reales del AAR ni se inicia Presage. */
class SmartSpectraFieldMapperTest {
    @Test fun maps_public_measurement_fields_to_lab_sample_without_value_conversion() {
        val pulse = SmartSpectraFieldMapper.fromMeasurementWithConfidenceFields(
            value = 72.5f, confidence = 0.8f, timestamp = 10_000L, stable = true,
        )
        assertEquals(72.5f, pulse.signal.value, 0f)
        assertEquals(0.8f, pulse.signal.confidence, 0f)
        assertEquals(10_000L, pulse.signal.timestampMs)
        assertTrue(pulse.stable)
    }

    @Test fun maps_breathing_fields_and_preserves_stable_false_as_metadata() {
        val breathing = SmartSpectraFieldMapper.fromMeasurementWithConfidenceFields(
            value = 16f, confidence = 0.7f, timestamp = 10_050L, stable = false,
        )
        assertEquals(16f, breathing.signal.value, 0f)
        assertEquals(0.7f, breathing.signal.confidence, 0f)
        assertEquals(10_050L, breathing.signal.timestampMs)
        assertEquals(false, breathing.stable)
    }

    @Test fun mapped_synthetic_fields_can_flow_through_lab_pair_validation() {
        val pulse = SmartSpectraFieldMapper.fromMeasurementWithConfidenceFields(
            72.5f, 0.8f, 10_000L, true,
        )
        val breathing = SmartSpectraFieldMapper.fromMeasurementWithConfidenceFields(
            16f, 0.7f, 10_050L, true,
        )
        val result = PresageLbhBridge.encodePair(
            pulse.signal, breathing.signal, maxSkewMs = 100L,
        )
        assertTrue(result is PresageLbhBridge.Result.Accepted)
        assertEquals(
            "ae429100004180000000000000000000",
            (result as PresageLbhBridge.Result.Accepted).frame.joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
    }
}
