<!-- SMART-SPECTRA-CHALLENGE-README -->
# SmartSpectra Edge Sentinel
<!-- DEMO_VIDEO_HERO_START -->

[![Watch the SmartSpectra Edge Sentinel live pulse demo](https://img.youtube.com/vi/s257P0V4-iQ/hqdefault.jpg)](https://youtube.com/shorts/s257P0V4-iQ)

<p align="center"><strong>Watch the unlisted live app demonstration</strong> · Opens on YouTube</p>

<!-- DEMO_VIDEO_HERO_END -->


A Presage SmartSpectra SDK Challenge prototype built and tested on a Samsung Galaxy A16 from Termux. The Android app uses the front camera, presents an in-app consent dialog, and displays the live pulse metric reported by SmartSpectra. This is an experimental prototype, not a medical device.

## Current implementation and boundaries

- The Android app uses SmartSpectra 3.4.0 and displays the latest available value from `cardio.pulseRateList`.
- The app requests cardio and breathing metric groups, but the current callback displays pulse; breathing-rate extraction and confidence handling are not yet implemented in that callback.
- The camera flow includes an in-app consent step and an Android camera-permission check. The app sets `imageOutputEnabled = false`; the current implementation does not deliberately save video or emit LBH frames.
- The repository tests the public `lbh-sdk` packet format and an experimental fixed 16-byte format separately. The Android app does not currently convert a live Presage metric into an LBH frame.

See the [interoperability contract](docs/contract-smart-spectra-lbh.md) for the tested boundaries and [consent and data handling](docs/consent-and-data.md) for the app's data disclosures.

## Development setup

### Requirements and verified build

The verified Termux build uses JDK 21, Android SDK API 36.1, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin Gradle Plugin 2.3.10, and SmartSpectra 3.4.0. AGP 8.13 supports API 36.1, and the Kotlin plugin version is within the published compatibility range for this AGP/Gradle combination. Other environments need JDK 17 or newer and the Android SDK platform/build tools required by the project.

Set `ANDROID_HOME` to the installed Android SDK (in the Termux setup used here, `$HOME/android-sdk`) and use the checked Gradle Wrapper rather than a system Gradle version:

```bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
cd android
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`. A configured development key is embedded in the APK and is extractable; do not publish or distribute that APK with an active key.

Python 3 is required for laboratory tests. Public SDK packet tests use `lbh-sdk` 0.3.1.

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

- **Live app demonstration:** [Watch the demonstration video](https://youtube.com/shorts/s257P0V4-iQ?si=WVPEBpQh4bsWr1Ku).

The recording shows the consent screen, front-camera measurement flow, and a changing pulse value reported by SmartSpectra. It demonstrates pulse only; it does not show breathing-rate extraction, confidence handling, clinical validation, or an Android-to-LBH packet bridge. This repository does not host or distribute an APK.

## Related public references

- Presage [SmartSpectra Android documentation](https://smartspectra.presagetech.com/docs/android/) and [official repository](https://github.com/Presage-Security/SmartSpectra).
- Public HormigasAIS [Python package on PyPI](https://pypi.org/project/lbh-sdk/), [Python SDK source](https://github.com/Thrumanshow/lbh-sdk), and [JavaScript package on npm](https://www.npmjs.com/package/lbh-sdk-hormigasais).
- Public LBH specification repository: [Lenguaje-Binario-HormigasAIS](https://github.com/Thrumanshow/Lenguaje-Binario-HormigasAIS-).
- Related Zenodo record: [10.5281/zenodo.19177759](https://doi.org/10.5281/zenodo.19177759).
- [Presage SmartSpectra SDK Challenge](https://dev.to/challenges/presage-2026-11-04).

The Android app depends on Presage's proprietary SmartSpectra artifact under Presage's terms. The public LBH SDKs are separate dependencies; consult each package's published license before reuse. No provider SDK binary or demo assets are copied into this repository.

<!-- RELATED-PUBLIC-REFERENCES -->

<!-- END SMART-SPECTRA-CHALLENGE-README -->
