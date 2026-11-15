package com.hormigasais.presagechallenge

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encoder aislado de un formato EXPERIMENTAL fijo de 16 bytes.
 * No implementa el paquete público variable de lbh-sdk/LBH v2.0.
 * Los umbrales/rangos son decisiones de laboratorio, no límites de Presage.
 */
object LbhEncoder {
    const val FRAME_SIZE = 16
    private const val HEADER = 0xAE

    data class Metrics(
        val pulseBpm: Float,
        val breathingRateBpm: Float,
        val pulseConfidence: Float,
        val breathingConfidence: Float,
    )

    class InvalidSignal(message: String) : IllegalArgumentException(message)

    /** Lanza InvalidSignal si un valor es no finito, fuera de rango o sin confianza positiva. */
    fun encode(metrics: Metrics): ByteArray {
        validateFinite("pulse", metrics.pulseBpm)
        validateFinite("breathing", metrics.breathingRateBpm)
        validateFinite("pulse confidence", metrics.pulseConfidence)
        validateFinite("breathing confidence", metrics.breathingConfidence)
        if (metrics.pulseConfidence <= 0f) throw InvalidSignal("pulse confidence must be > 0")
        if (metrics.breathingConfidence <= 0f) throw InvalidSignal("breathing confidence must be > 0")
        if (metrics.pulseBpm !in 40f..110f) throw InvalidSignal("pulse outside experimental range")
        if (metrics.breathingRateBpm !in 5f..40f) throw InvalidSignal("breathing outside experimental range")

        val buffer = ByteBuffer.allocate(FRAME_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.put(HEADER.toByte())
        buffer.putFloat(metrics.pulseBpm)
        buffer.putFloat(metrics.breathingRateBpm)
        // ByteBuffer.allocate initializes the remaining seven reserved bytes to zero.
        return buffer.array()
    }

    /** Variante sin excepción para un ensayo: null significa rechazo. */
    fun encodeOrReject(metrics: Metrics): ByteArray? = try {
        encode(metrics)
    } catch (_: InvalidSignal) {
        null
    }

    private fun validateFinite(label: String, value: Float) {
        if (!value.isFinite()) throw InvalidSignal("$label must be finite")
    }
}
