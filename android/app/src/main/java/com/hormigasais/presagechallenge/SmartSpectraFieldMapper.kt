package com.hormigasais.presagechallenge

/**
 * Mapeo de campos públicos observados en SmartSpectra 3.4.0.
 * No depende del AAR ni construye objetos protobuf reales.
 *
 * Documentación oficial: MeasurementWithConfidence.timestamp es microsegundos
 * absolutos desde Linux epoch; confidence es porcentaje [0,100].
 * https://smartspectra.presagetech.com/docs/data-types/
 */
object SmartSpectraFieldMapper {
    data class MappedMeasurement(
        val signal: PresageLbhBridge.SignalSample,
        val stable: Boolean,
    )

    /** Paquete sintético de campos que extraería el llamador de un objeto SDK. */
    data class MeasurementWithConfidenceFields(
        val value: Float,
        val confidence: Float,
        val timestampMicros: Long,
        val stable: Boolean,
    )

    /** Conversión de epoch microseconds a epoch milliseconds; sub-ms se trunca. */
    fun epochMicrosToMillis(timestampMicros: Long): Long? {
        if (timestampMicros <= 0L) return null
        return timestampMicros / 1_000L
    }

    /**
     * Capa de compatibilidad sintética para pruebas preexistentes que ya pasan
     * timestamp en ms. Para campos del SDK, usar fromSdkFields/encodeSdkPair.
     */
    fun fromMeasurementWithConfidenceFields(
        value: Float,
        confidence: Float,
        timestamp: Long,
        stable: Boolean,
    ): MappedMeasurement = MappedMeasurement(
        signal = PresageLbhBridge.SignalSample(
            value = value,
            confidence = confidence,
            timestampMs = timestamp,
        ),
        stable = stable,
    )

    /** Mapea una muestra SDK: getValue/getConfidence/getTimestamp/getStable. */
    fun fromSdkFields(fields: MeasurementWithConfidenceFields): MappedMeasurement? {
        val timestampMs = epochMicrosToMillis(fields.timestampMicros) ?: return null
        return fromMeasurementWithConfidenceFields(
            value = fields.value,
            confidence = fields.confidence,
            timestamp = timestampMs,
            stable = fields.stable,
        )
    }

    /**
     * Verifica el desfase en microsegundos ANTES de truncar timestamps a ms,
     * luego entrega las métricas al encoder experimental del laboratorio.
     * maxSkewMs es una tolerancia de laboratorio, no un umbral del SDK.
     */
    fun encodeSdkPair(
        pulse: MeasurementWithConfidenceFields,
        breathing: MeasurementWithConfidenceFields,
        maxSkewMs: Long,
    ): PresageLbhBridge.Result {
        if (maxSkewMs < 0L || maxSkewMs > Long.MAX_VALUE / 1_000L) {
            return PresageLbhBridge.Result.Rejected(PresageLbhBridge.Rejection.INVALID_MAX_SKEW)
        }
        if (pulse.timestampMicros <= 0L || breathing.timestampMicros <= 0L) {
            return PresageLbhBridge.Result.Rejected(PresageLbhBridge.Rejection.INVALID_TIMESTAMP)
        }
        val newer = maxOf(pulse.timestampMicros, breathing.timestampMicros)
        val older = minOf(pulse.timestampMicros, breathing.timestampMicros)
        val maxSkewMicros = maxSkewMs * 1_000L
        if (newer - older > maxSkewMicros) {
            return PresageLbhBridge.Result.Rejected(PresageLbhBridge.Rejection.SAMPLES_NOT_TIME_ALIGNED)
        }
        val mappedPulse = fromSdkFields(pulse)
            ?: return PresageLbhBridge.Result.Rejected(PresageLbhBridge.Rejection.INVALID_TIMESTAMP)
        val mappedBreathing = fromSdkFields(breathing)
            ?: return PresageLbhBridge.Result.Rejected(PresageLbhBridge.Rejection.INVALID_TIMESTAMP)
        return PresageLbhBridge.encodePair(
            mappedPulse.signal,
            mappedBreathing.signal,
            maxSkewMs = maxSkewMs,
        )
    }
}
