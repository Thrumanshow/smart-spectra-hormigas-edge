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

<!-- SMART-SPECTRA-CHALLENGE-README -->
# SmartSpectra Edge Sentinel

A Presage SmartSpectra SDK Challenge prototype built and tested on a Samsung Galaxy A16 from Termux. The Android app uses the front camera, presents an in-app consent dialog, and displays the live pulse metric reported by SmartSpectra. This is an experimental prototype, not a medical device.

## Current implementation and boundaries

- The Android app uses SmartSpectra 3.4.0 and displays the latest available value from `cardio.pulseRateList`.
- The app requests cardio and breathing metric groups, but the current callback displays pulse; breathing-rate extraction and confidence handling are not yet implemented in that callback.
- The camera flow includes an in-app consent step and an Android camera-permission check. The app sets `imageOutputEnabled = false`; the current implementation does not deliberately save video or emit LBH frames.
- The repository tests the public `lbh-sdk` packet format and an experimental fixed 16-byte format separately. The Android app does not currently convert a live Presage metric into an LBH frame.

See the [interoperability contract](docs/contract-smart-spectra-lbh.md) for the tested boundaries and [consent and data handling](docs/consent-and-data.md) for the app's data disclosures.

## Development setup

### Requirements

Use a JDK and Android SDK compatible with the Android Gradle Plugin declared in this project. The Android module targets API 36 and uses SmartSpectra 3.4.0. The Gradle distribution must be compatible with the declared plugin. In the Termux environment, system Gradle 9.8.2 failed during Android plugin configuration before app compilation; a prior successful build used the Termux Studio Gradle 8.13 distribution. Do not change Gradle or plugin versions blindly. The exact build command should be followed from the local Termux Studio setup and revalidated after toolchain changes.

Python 3 is required for the laboratory tests. Public SDK packet tests use `lbh-sdk` 0.3.1.

### Configure the Presage development key locally

Do not put an API key in source code, this README, shell command arguments, Git, or a shared chat. Run the local helper and enter the key only in Termux when prompted; do not continue if the helper visibly echoes the key or asks you to place it in a command:

```bash
bash android/configure-presage-dev.sh
```

The helper is intended to store the development credential outside the repository under `~/.config/smart-spectra-challenge/`. Verify that local configuration remains outside Git. The build injects the key into Android `BuildConfig`, so it can be extracted from a built APK. Do not commit or publicly distribute an APK containing an active development key. Revoke or rotate an exposed key.

The challenge requests a public source repository, setup instructions, and a demo video; it does not list an APK upload as a submission requirement. See the [official challenge requirements](https://dev.to/challenges/presage-2026-11-04).

## Run the Python laboratory tests

Create or activate the project's Python virtual environment and install the public package used by SDK packet tests:

```bash
python3 -m venv lab/.venv
lab/.venv/bin/python -m pip install lbh-sdk==0.3.1
```

Run the experimental fixed-frame tests:

```bash
python3 -m unittest discover -s lab/tests -v
```

Run the adapter and public-contract tests:

```bash
cd lab/simulator
../../lab/.venv/bin/python -m unittest -v \
  test_adapter.py \
  test_public_and_internal_boundaries.py \
  test_public_lbh_v2_contract.py
```

The test payloads are synthetic. Passing these suites validates the Python serialization boundaries separately; it does not show that Android emits LBH frames or that the experimental 16-byte format is equivalent to the public variable-length packet.

## Presage attribution and telemetry

This project depends on the Presage SmartSpectra Android artifact `com.presagetech:smartspectra:3.4.0`. Presage describes SmartSpectra as proprietary and subject to its [Terms of Service](https://physiology.presagetech.com/tos). This repository does not include the SDK binary, provider demo media, or provider sample files.

Presage documents session telemetry as enabled by default. It describes one aggregate diagnostic summary per measurement session and states that it excludes raw video, measured vital values, and stable user/device identifiers. This app does not explicitly disable telemetry. See Presage's [telemetry and privacy documentation](https://github.com/Presage-Security/SmartSpectra/blob/main/docs/telemetry-and-privacy.md) and this project's [consent and data handling](docs/consent-and-data.md). This project has not independently inspected SDK network traffic.

## Demo

The demo should show this app's own consent flow, camera-based measurement, and live SmartSpectra pulse value. The provider's Quickstart GIF is illustrative reference material, not a required app design or a substitute for the project's own demo video.

<!-- END SMART-SPECTRA-CHALLENGE-README -->
