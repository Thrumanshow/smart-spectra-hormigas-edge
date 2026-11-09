# Laboratorio SmartSpectra Edge Sentinel

Este laboratorio separa los ensayos del prototipo de la futura aplicación
presentada al reto. No es un sandbox del sistema operativo ni concede acceso
a la cámara.

## Componentes

- `simulator/`: generador de valores ficticios y pruebas del formato LBH
  experimental.
- `android-integration/`: espacio reservado para pruebas de integración
  con la app Android y SmartSpectra.
- `../android/`: espacio reservado para el proyecto Android.

## Reglas para los ensayos

1. Identificar cada ejecución como simulada o como señal Presage real.
2. No presentar valores aleatorios como mediciones obtenidas de una persona.
3. No guardar ni transmitir video o señales biométricas salvo que sea necesario,
   esté explicado y exista consentimiento explícito.
4. No incluir API keys, contraseñas, tokens ni claves privadas en el repositorio.
5. No realizar diagnóstico, tratamiento ni decisiones médicas.

El formato binario actual —16 bytes, encabezado `0xAE`, dos float32
big-endian y siete bytes reservados— es experimental. No se considera una
especificación oficial de LBH hasta verificar la documentación y el SDK público
correspondientes.

## Estado de integración

SmartSpectra aún no está integrado en este laboratorio. Antes de declarar una
prueba como integración real, debe ejecutarse una app compatible con el SDK,
obtener consentimiento para cámara y mostrar al menos una señal Presage en vivo.
