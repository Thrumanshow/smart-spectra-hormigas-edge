# SmartSpectra–LBH Interoperability Contract

**Project:** SmartSpectra Edge Sentinel — Presage SmartSpectra SDK Challenge  
**Status:** Android prototype with separate laboratory format tests  
**Document date:** November 8, 2026

## Purpose and scope

This document records how this challenge project uses Presage SmartSpectra and the public Lenguaje Binario HormigasAIS (LBH) Python SDK, while keeping the challenge repository separate from HormigasAIS production services and infrastructure.

The Android application uses the SmartSpectra SDK to process camera input and display a pulse metric. The laboratory separately tests the public Python package `lbh-sdk` and an experimental fixed-width 16-byte encoder. These are distinct components and are not represented as an already completed Presage-to-LBH integration.

The project does not include production credentials, production agents, private source code, or a connection to HormigasAIS production systems. Any future production integration is outside this repository’s scope and would require separate authorization and applicable agreements.

---

## Android application and SmartSpectra

The Android prototype integrates SmartSpectra SDK version 3.4.0. In the current activity code, the app requests cardio and breathing metric groups, observes SmartSpectra metrics, reads the latest available value from `cardio.pulseRateList`, and displays pulse in beats per minute. The user sees a consent dialog before measurement, and Android camera permission is checked before starting the SDK. The interface provides start and stop controls.

The project author has reported and recorded a device test in which the app displays a changing pulse value during camera measurement. This is prototype evidence of a live SmartSpectra pulse flow, not a clinical validation or medical claim. The current callback shown in this project reads and displays pulse; it does not yet encode or transmit that metric as an LBH packet.

Although the app requests cardio and breathing metric groups, the specific breathing-rate and confidence accessors have not yet been incorporated into the app callback. They must be verified against the installed SmartSpectra API before the app uses them. No missing metric or confidence value is to be replaced with invented or simulated data.

The app sets `imageOutputEnabled` to `false` and does not deliberately save video or send LBH frames in the current prototype. Presage SDK telemetry or other processing must be described according to Presage’s current documentation and privacy materials; this repository does not make claims about undocumented SDK behavior.

## Public Python LBH SDK

The project has inspected and tested the public PyPI package `lbh-sdk` version 0.3.1. Its visible Python API includes `encode_packet(header, type_code, payload)`, `decode_packet(frame)`, and `validate_packet(frame)`.

The public packet representation documented for the package is a hexadecimal string with this layout:

```text
HEADER (8 hex characters)
| TYPE_CODE (8 hex characters)
| LENGTH (8 hex characters)
| PAYLOAD (UTF-8 bytes represented as hexadecimal)
```
---

`LENGTH` represents the number of bytes in the original UTF-8 payload, before hexadecimal conversion. When decoded from hexadecimal into bytes, the three fixed fields occupy 12 bytes, followed by the variable-length payload. The package therefore does not directly return the experimental fixed 16-byte frame described below.

Local tests exercise the public package using synthetic payload data. They verify packet encoding, declared payload byte length, validation, and decode round-trip. The test values and payloads are not measurements from a person and do not establish a biometric type code or a production transport contract.

The public specification lists `FUEL` as an activation/pheromone event. This project uses it only in explicitly synthetic SDK tests where stated. It does not claim that `FUEL` is a biometric type or use it to label a live pulse or breathing measurement.

## Experimental fixed 16-byte laboratory format

A separate local laboratory adapter encodes test metrics into a fixed 16-byte frame. Its tests check the experimental header byte, two big-endian float32 values, seven reserved zero bytes, and rejection of invalid inputs such as zero confidence or out-of-range metrics.

This fixed-width format is documented only as an experimental laboratory representation. Its local tests show that this adapter produces and validates the described test frame. They do not establish that the public Python package produces the same frame, that the two representations are equivalent or automatically convertible, or that this experimental format is part of the public PyPI SDK contract. No private production implementation is included or reconstructed here.

## Interoperability boundary and current status

The project currently has three independently testable pieces:

1. An Android SmartSpectra prototype that displays pulse from the SDK callback.
2. A public `lbh-sdk` Python package test using a synthetic, variable-length public packet.
3. A separate experimental local test for a fixed 16-byte frame.

Passing the Python tests validates those laboratory boundaries separately. It does not prove an Android Presage-to-LBH bridge, conversion between the two LBH representations, network transmission, or compatibility with an unspecified biometric packet type.

A future bridge must define an authorized payload schema and event meaning, use actual SmartSpectra values obtained through verified API accessors, reject missing or invalid metrics, and add tests that distinguish synthetic data from live measurements. Until that work is implemented and tested, this repository describes the integration as a planned boundary rather than a completed feature.

## Consent, privacy, and non-medical use

The app presents a consent dialog describing camera use and physiological signal processing. The user can cancel, and measurement can be stopped from the interface. The project’s consent and data-handling documentation must disclose relevant storage, sharing, retention, deletion, SDK telemetry, and any optional external service use based on the actual implementation and current provider documentation.

This prototype is not a medical device and must not be presented as providing diagnosis, treatment, or medical decision-making. Covert or non-consensual monitoring is outside the project’s scope.

## Reproducibility and release notes

The Python laboratory tests run locally in the project’s virtual environment. The public SDK packet tests and experimental 16-byte tests are separate suites and use synthetic values. Their results should be reported as laboratory serialization tests, not as proof that the Android app emits LBH frames.

The development APK may contain a Presage development API key if the key is injected into Android `BuildConfig`. Such a key is not secret once embedded in an APK. Do not publish or distribute an APK containing an active development key; follow Presage’s recommended key restrictions, rotation, and release guidance before distribution. Keep credential files out of version control.

## References

- [Presage SmartSpectra SDK Challenge: prompt, judging criteria, and submission requirements](https://dev.to/challenges/presage-2026-11-04)
- [Presage SmartSpectra SDK repository and platform documentation links](https://github.com/Presage-Security/SmartSpectra)
- [LBH SDK on PyPI: public package API and documented packet layout](https://pypi.org/project/lbh-sdk/)
- [Public LBH v2.0 specification](https://github.com/Thrumanshow/Lenguaje-Binario-HormigasAIS-/blob/main/LBH_SPEC_v2.0.md)
- [Public LBH Python SDK source repository](https://github.com/Thrumanshow/lbh-sdk)
