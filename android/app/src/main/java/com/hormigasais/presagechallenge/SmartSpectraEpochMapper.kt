package com.hormigasais.presagechallenge

/** Compatibilidad/nombre semántico; la conversión vive ahora en FieldMapper. */
object SmartSpectraEpochMapper {
    data class MappedMeasurement(
        val signal: PresageLbhBridge.SignalSample,
        val stable: Boolean,
    )

    fun epochMicrosToMillis(timestampMicros: Long): Long? =
        SmartSpectraFieldMapper.epochMicrosToMillis(timestampMicros)

    fun fromMeasurementWithConfidenceFields(
        value: Float,
        confidence: Float,
        timestampMicros: Long,
        stable: Boolean,
    ): MappedMeasurement? {
        val mapped = SmartSpectraFieldMapper.fromSdkFields(
            SmartSpectraFieldMapper.MeasurementWithConfidenceFields(
                value = value,
                confidence = confidence,
                timestampMicros = timestampMicros,
                stable = stable,
            )
        ) ?: return null
        return MappedMeasurement(mapped.signal, mapped.stable)
    }
}
