# SmartSpectra Edge Sentinel

Prototipo para explorar el flujo entre señales humanas capturadas con
Presage SmartSpectra y el protocolo público Lenguaje Binario HormigasAIS
(LBH).

## Estado actual

El laboratorio contiene un simulador y pruebas del adaptador. Las pruebas
actuales usan valores de ejemplo y verifican un formato experimental de
16 bytes. **No representan señales Presage en vivo ni prueban compatibilidad
con LBH v2.0.**

La aplicación Android todavía debe integrar SmartSpectra, solicitar
consentimiento antes de activar la cámara y demostrar al menos una señal
Presage real en vivo. El código de cámara y esa integración aún no están
implementados aquí.

## Componentes públicos relacionados

- Web y referencias legales de HormigasAIS: https://hormigasais.com/
- SDK Python LBH: https://pypi.org/project/lbh-sdk/
- Repositorio del SDK Python: https://github.com/Thrumanshow/lbh-sdk
- SDK JavaScript LBH: https://www.npmjs.com/package/lbh-sdk-hormigasais
- Repositorio del SDK JavaScript: https://github.com/Thrumanshow/lbh-sdk-js
- Registro Zenodo relacionado: https://doi.org/10.5281/zenodo.19177759
- Documentación SmartSpectra: https://smartspectra.presagetech.com/docs/
- Repositorio SmartSpectra: https://github.com/Presage-Security/SmartSpectra
- Convocatoria Presage SmartSpectra SDK Challenge:
  https://dev.to/devteam/join-the-presage-smartspectra-sdk-challenge-3000-in-prizes-across-10-winners-1370

## Plan de integración

1. Revisar la especificación LBH v2.0 y la API publicada del SDK Python.
2. Integrar SmartSpectra en una aplicación Android compatible siguiendo la
   documentación vigente de Presage.
3. Validar la señal recibida y convertirla a un payload documentado.
4. Codificar y validar ese payload mediante el SDK LBH público.
5. Probar con consentimiento explícito y documentar el manejo real de datos.

El formato de 16 bytes del laboratorio se conserva solo como experimento
hasta que se sustituya o se demuestre su correspondencia con la especificación
pública LBH.

## Privacidad y seguridad

No incluir API keys, tokens, contraseñas ni claves privadas en el repositorio.
No guardar ni transmitir video o métricas biométricas por defecto. Documentar
el consentimiento, almacenamiento, transmisión, retención, eliminación y
telemetría del SDK según el comportamiento final verificado.

## Licencias

Consultar las licencias de cada SDK y componente antes de reutilizar o
modificar código. La ficha pública de PyPI y la del paquete npm indican
MPL-2.0 para esos paquetes; conservar los avisos aplicables y revisar las
licencias incluidas en las distribuciones antes de publicar una integración.

## Ensayos locales

Pruebas del simulador existente:

```bash
python3 -m unittest discover -s lab/tests -v
python3 -m unittest discover -s lab/simulator/tests -v
```

Los tests del simulador no sustituyen la integración real ni la demo requerida
por el concurso.
