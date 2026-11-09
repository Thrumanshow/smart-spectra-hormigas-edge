# SmartSpectra–LBH Interoperability Contract

**Project:** SmartSpectra Edge Sentinel — Presage SmartSpectra SDK Challenge  
**Status:** Android prototype with separate laboratory format tests  
**Document date:** November 8, 2026

## Purpose and scope

This document records how this challenge project uses Presage SmartSpectra and the public Lenguaje Binario HormigasAIS (LBH) Python SDK, while keeping the challenge repository separate from HormigasAIS production services and infrastructure.

The Android application uses the SmartSpectra SDK to process camera input and display a pulse metric. The laboratory separately tests the public Python package `lbh-sdk` and an experimental fixed-width 16-byte encoder. These are distinct components and are not represented as an already completed Presage-to-LBH integration.

The project does not include production credentials, production agents, private source code, or a connection to HormigasAIS production systems. Any future production integration is outside this repository’s scope and would require separate authorization and applicable agreements.

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
