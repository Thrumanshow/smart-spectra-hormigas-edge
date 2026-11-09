# SmartSpectra–LBH Interoperability Contract

**Project:** SmartSpectra Edge Sentinel — Presage SmartSpectra SDK Challenge  
**Status:** Android prototype with separate laboratory format tests  
**Document date:** November 8, 2026

## Purpose and scope

This document records how this challenge project uses Presage SmartSpectra and the public Lenguaje Binario HormigasAIS (LBH) Python SDK[span_0](start_span)[span_0](end_span), while keeping the challenge repository separate from HormigasAIS production services and infrastructure.

The Android application uses the SmartSpectra SDK to process camera input and display a pulse metric[span_1](start_span)[span_1](end_span). The laboratory separately tests the public Python package `lbh-sdk` and an experimental fixed-width 16-byte encoder[span_2](start_span)[span_2](end_span). These are distinct components and are not represented as an already completed Presage-to-LBH integration[span_3](start_span)[span_3](end_span).

The project does not include production credentials, production agents, private source code, or a connection to HormigasAIS production systems[span_4](start_span)[span_4](end_span). Any future production integration is outside this repository’s scope and would require separate authorization and applicable agreements[span_5](start_span)[span_5](end_span).

## Android application and SmartSpectra

The Android prototype integrates SmartSpectra SDK version 3.4.0[span_6](start_span)[span_6](end_span). In the current activity code, the app requests cardio and breathing metric groups, observes SmartSpectra metrics, reads the latest available value from `cardio.pulseRateList`, and displays pulse in beats per minute[span_7](start_span)[span_7](end_span). The user sees a consent dialog before measurement, and Android camera permission is checked before starting the SDK[span_8](start_span)[span_8](end_span). The interface provides start and stop controls[span_9](start_span)[span_9](end_span).

The project author has reported and recorded a device test in which the app displays a changing pulse value during camera measurement[span_10](start_span)[span_10](end_span). This is prototype evidence of a live SmartSpectra pulse flow, not a clinical validation or medical claim[span_11](start_span)[span_11](end_span). The current callback shown in this project reads and displays pulse; it does not yet encode or transmit that metric as an LBH packet[span_12](start_span)[span_12](end_span).

Although the app requests cardio and breathing metric groups, the specific breathing-rate and confidence accessors have not yet been incorporated into the app callback[span_13](start_span)[span_13](end_span). They must be verified against the installed SmartSpectra API before the app uses them[span_14](start_span)[span_14](end_span). No missing metric or confidence value is to be replaced with invented or simulated data[span_15](start_span)[span_15](end_span).

The app sets `imageOutputEnabled` to `false` and does not deliberately save video or send LBH frames in the current prototype[span_16](start_span)[span_16](end_span). Presage SDK telemetry or other processing must be described according to Presage’s current documentation and privacy materials; this repository does not make claims about undocumented SDK behavior[span_17](start_span)[span_17](end_span).

## Public Python LBH SDK

The project has inspected and tested the public PyPI package `lbh-sdk` version 0.3.1[span_18](start_span)[span_18](end_span). Its visible Python API includes `encode_packet(header, type_code, payload)`, `decode_packet(frame)`, and `validate_packet(frame)`[span_19](start_span)[span_19](end_span).

The public packet representation documented for the package is a hexadecimal string with this layout:

```text
HEADER (8 hex characters)
| TYPE_CODE (8 hex characters)
| LENGTH (8 hex characters)
| PAYLOAD (UTF-8 bytes represented as hexadecimal)
