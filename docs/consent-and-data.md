# Consentimiento y manejo de datos — borrador

Este documento es un borrador de planificación. Antes de publicar la app,
completarlo según la implementación real y la documentación vigente de Presage.

## Consentimiento de cámara

La app deberá explicar quién inicia la medición, qué señal intenta obtener y
cuándo se activa la cámara. La cámara solo se activará después de una acción
afirmativa de la persona participante. La app deberá permitir detener la medición.

## Procesamiento y almacenamiento

Estado previsto, aún por verificar en la integración:

- Video: no guardar ni transmitir video por defecto.
- Señales biométricas: no guardar ni transmitir por defecto.
- Tramas LBH: no guardar ni transmitir por defecto.
- Eliminación: al no persistir datos en el diseño previsto, no debería quedar
  historial de mediciones; confirmar este comportamiento en el código final.

## SDK y telemetría

Revisar la documentación vigente de Presage sobre telemetría y privacidad.
Describir aquí qué telemetría envía el SDK, cómo se configura y si la app la
mantiene habilitada o la desactiva.

## Servicios externos e IA

No se prevé usar servicios de IA externos en el prototipo actual. Si se añade
alguno, documentar qué datos se envían, con qué finalidad y bajo qué
consentimiento.

## Estado de este borrador

Estado verificado del prototipo: SmartSpectra 3.4.0 está integrado en la app. El callback actual muestra el pulso de `cardio.pulseRateList`; aunque se solicitan métricas cardio y respiratorias, la extracción de respiración y confianza aún no está implementada en ese callback. La app muestra consentimiento antes de iniciar la cámara y permite detener la medición. La demo observada muestra pulso en vivo; esto no constituye validación clínica ni uso médico.

<!-- PRESAGE-SDK-TELEMETRY-DETAILS -->
## SmartSpectra SDK telemetry

Presage documents SmartSpectra telemetry as **enabled by default**. Its documentation describes one aggregate diagnostic summary per measurement session. The listed fields include SDK build/package origin, platform and runtime/device characteristics, source and session outcome, counts and performance statistics, validation-state counts, a coarse error code for failed sessions, and a session timestamp. Presage states that this telemetry does not include raw video or image frames, measured vital values, free-text errors, user identifiers, or stable device identifiers. See [Presage's telemetry and privacy documentation](https://github.com/Presage-Security/SmartSpectra/blob/main/docs/telemetry-and-privacy.md).

The current app code does not explicitly set `enableTelemetry = false`; therefore this project does not claim that SDK telemetry is disabled. Presage documents an opt-out through `SmartSpectraConfig.enableTelemetry = false` before starting the SDK. Any future change to that setting must be reflected in this notice and tested in the actual build. This repository describes the provider's stated collection and does not independently verify network traffic.

The app's visible consent text summarizes the provider-documented telemetry and camera-based signal processing. Users can cancel before starting the measurement and can stop an active measurement in the app. Consult Presage's [privacy policy](https://api.physiology.presagetech.com/privacypolicy) and current product terms for provider-side handling details.

## Development API key and APK handling

The local development flow loads the Presage key from a properties file outside the Git repository and exposes it to app code through `BuildConfig`. This keeps the key out of source control when configured correctly, but **does not keep it secret in the resulting APK**. Do not publish or distribute an APK containing an active development key. Keep the local credential file outside version control, apply provider-supported restrictions where available, and rotate/revoke a key if it is exposed. The challenge requires a public source repository and demo video; the APK is not listed as a required upload.
<!-- END PRESAGE-SDK-TELEMETRY-DETAILS -->
