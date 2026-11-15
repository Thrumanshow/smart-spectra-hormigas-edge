package com.hormigasais.presagechallenge

/**
 * Contrato experimental entre una futura capa SDK y el encoder fijo de 16 bytes.
 * Estos tipos NO son tipos ni accesores de Presage. El laboratorio solo usa
 * muestras sintéticas; la correspondencia con SmartSpectra queda pendiente.
 */
object PresageLbhBridge {
    data class SignalSample(
        val value: Float,
        val confidence: Float,
        val timestampMs: Long,
    )

    enum class Rejection {
        INVALID_MAX_SKEW,
        INVALID_TIMESTAMP,
        NON_FINITE_VALUE_OR_CONFIDENCE,
        NON_POSITIVE_CONFIDENCE,
        PULSE_OUT_OF_EXPERIMENTAL_RANGE,
        BREATHING_OUT_OF_EXPERIMENTAL_RANGE,
        SAMPLES_NOT_TIME_ALIGNED,
    }

    sealed class Result {
        data class Accepted(val frame: ByteArray) : Result()
        data class Rejected(val reason: Rejection) : Result()
    }

    /**
     * Empareja solo muestras cuyo desfase no supera maxSkewMs, valida
     * campos experimentales y llama al encoder. El llamador decide la
     * tolerancia temporal; aquí no se afirma un umbral del SDK.
     */
    fun encodePair(
        pulse: SignalSample,
        breathing: SignalSample,
        maxSkewMs: Long,
    ): Result {
        if (maxSkewMs < 0) return Result.Rejected(Rejection.INVALID_MAX_SKEW)
        if (pulse.timestampMs < 0 || breathing.timestampMs < 0) {
            return Result.Rejected(Rejection.INVALID_TIMESTAMP)
        }
        if (!pulse.value.isFinite() || !breathing.value.isFinite() ||
            !pulse.confidence.isFinite() || !breathing.confidence.isFinite()) {
            return Result.Rejected(Rejection.NON_FINITE_VALUE_OR_CONFIDENCE)
        }
        if (pulse.confidence <= 0f || breathing.confidence <= 0f) {
            return Result.Rejected(Rejection.NON_POSITIVE_CONFIDENCE)
        }
        if (pulse.value !in 40f..110f) {
            return Result.Rejected(Rejection.PULSE_OUT_OF_EXPERIMENTAL_RANGE)
        }
        if (breathing.value !in 5f..40f) {
            return Result.Rejected(Rejection.BREATHING_OUT_OF_EXPERIMENTAL_RANGE)
        }

        // Ambos timestamps son no negativos, así que la resta entre el mayor
        // y el menor no desborda Long.
        val newer = maxOf(pulse.timestampMs, breathing.timestampMs)
        val older = minOf(pulse.timestampMs, breathing.timestampMs)
        if (newer - older > maxSkewMs) {
            return Result.Rejected(Rejection.SAMPLES_NOT_TIME_ALIGNED)
        }

        val frame = try {
            LbhEncoder.encode(
                LbhEncoder.Metrics(
                    pulseBpm = pulse.value,
                    breathingRateBpm = breathing.value,
                    pulseConfidence = pulse.confidence,
                    breathingConfidence = breathing.confidence,
                )
            )
        } catch (_: LbhEncoder.InvalidSignal) {
            return Result.Rejected(Rejection.NON_FINITE_VALUE_OR_CONFIDENCE)
        }
        return Result.Accepted(frame)
    }
}
