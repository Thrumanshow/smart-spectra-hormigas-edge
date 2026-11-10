# Android application

This repository contains the Android challenge prototype in `app/`. It integrates Presage SmartSpectra 3.4.0, requests camera consent and Android camera permission, and displays the latest pulse value exposed by the SDK callback.

The current callback does not yet extract breathing rate or confidence, and the Android app does not create or transmit LBH frames. The Python laboratory tests exercise synthetic payloads and a separate experimental 16-byte representation; they are not an Android-to-LBH bridge.

Build and local development-key instructions are documented in the root [README](../README.md). A key injected into `BuildConfig` can be extracted from a built APK. Do not commit a credential or distribute an APK/AAB containing an active development key. The Presage SDK remains subject to Presage's own terms; this repository does not include its binary or provider demo assets.
