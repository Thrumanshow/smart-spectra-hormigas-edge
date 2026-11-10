# Consent and data handling

This notice describes the current SmartSpectra Edge Sentinel challenge prototype. It is not a medical device and is not intended for diagnosis, treatment, or medical decision-making.

## Camera consent and controls

Before measurement, the app presents an in-app dialog explaining that the front camera is used to process physiological signals such as pulse. The participant can cancel; the app then leaves measurement inactive. If the participant accepts, Android camera permission is checked/requested before the SmartSpectra session starts. The app provides a stop control. When the activity is paused, the app requests SDK shutdown for the `RUNNING`, `STARTING`, or `STOPPING` states.

## Signals and app-controlled storage

The current callback reads and displays the latest available pulse value from `cardio.pulseRateList`. Although the app requests cardio and breathing metric groups, the current callback does not extract breathing rate or confidence. Missing values are not substituted with simulated data.

The app sets `imageOutputEnabled = false` and its current source code does not deliberately record/export camera video, write biometric measurements to a file, or create/transmit LBH frames. This describes app-controlled behavior in this repository; it is not a claim that the SDK performs no network communication or has no provider-side retention. The project has not independently inspected SDK network traffic.

## SmartSpectra telemetry and provider handling

Presage documents SmartSpectra session telemetry as enabled by default. Its documentation describes an aggregate diagnostic summary per measurement session, including SDK/package, platform/runtime/device characteristics, session source/outcome, performance/validation counts, coarse error code for a failed session, and a session timestamp. Presage states that this telemetry excludes raw video or image frames, measured vital values, free-text errors, user identifiers, and stable device identifiers. This app does not explicitly disable telemetry. These are provider statements, not an independent network audit. Consult the [Presage telemetry and privacy documentation](https://github.com/Presage-Security/SmartSpectra/blob/main/docs/telemetry-and-privacy.md) and [Presage privacy policy](https://api.physiology.presagetech.com/privacypolicy) for provider-side handling and retention details.

## Retention, deletion, and external services

The app source does not implement persistent storage or a user-facing history/deletion feature for biometric measurements. The project has not independently verified the SDK's provider-side retention or deletion practices; refer to Presage's privacy documentation. This prototype does not send measurements to an optional AI service and does not implement an LBH network transport.

## Development credential and APK

The development setup reads the Presage API key from a local properties file outside the repository and exposes it to the app through `BuildConfig`. This keeps it out of source control when configured correctly, but the value is extractable from a compiled APK. Do not publish or distribute an APK/AAB containing an active development key. Rotate/revoke an exposed key and use provider-supported restrictions where available.

## Demonstration and scope

The [linked demonstration video](https://youtube.com/shorts/s257P0V4-iQ?si=WVPEBpQh4bsWr1Ku) shows the consent flow, camera-based measurement, and a changing pulse value. It does not demonstrate breathing extraction, confidence output, an LBH bridge, or clinical validity.
