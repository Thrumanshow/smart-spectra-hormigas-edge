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

No publicar estas declaraciones como hechos hasta verificar el comportamiento
real de la app integrada y del SDK. SmartSpectra todavía no está integrado.
