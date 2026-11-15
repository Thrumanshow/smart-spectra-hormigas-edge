package com.hormigasais.presagechallenge

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Todo valor de esta suite es sintético; no se conecta a SmartSpectra. */
class LbhEncoderTest {
    private fun sample() = LbhEncoder.Metrics(
        pulseBpm = 72.5f,
        breathingRateBpm = 16.0f,
        pulseConfidence = 90.0f,
        breathingConfidence = 80.0f,
    )

    @Test fun matches_known_synthetic_vector() {
        val expected = "ae429100004180000000000000000000"
        assertEquals(expected, LbhEncoder.encode(sample()).joinToString("") { "%02x".format(it.toInt() and 0xff) })
    }

    @Test fun has_16_bytes_header_and_big_endian_values() {
        val frame = LbhEncoder.encode(sample())
        assertEquals(16, frame.size)
        assertEquals(0xAE.toByte(), frame[0])
        val bytes = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN)
        assertEquals(72.5f, bytes.getFloat(1), 0.0001f)
        assertEquals(16.0f, bytes.getFloat(5), 0.0001f)
    }

    @Test fun reserved_bytes_are_zero() {
        assertArrayEquals(ByteArray(7), LbhEncoder.encode(sample()).copyOfRange(9, 16))
    }

    @Test fun rejects_nonpositive_or_nonfinite_confidence() {
        assertNull(LbhEncoder.encodeOrReject(sample().copy(pulseConfidence = 0f)))
        assertNull(LbhEncoder.encodeOrReject(sample().copy(breathingConfidence = Float.NaN)))
    }

    @Test fun rejects_nonfinite_and_out_of_range_signals() {
        assertThrows(LbhEncoder.InvalidSignal::class.java) {
            LbhEncoder.encode(sample().copy(pulseBpm = Float.NaN))
        }
        assertThrows(LbhEncoder.InvalidSignal::class.java) {
            LbhEncoder.encode(sample().copy(pulseBpm = 120f))
        }
        assertThrows(LbhEncoder.InvalidSignal::class.java) {
            LbhEncoder.encode(sample().copy(breathingRateBpm = 4f))
        }
    }
}
