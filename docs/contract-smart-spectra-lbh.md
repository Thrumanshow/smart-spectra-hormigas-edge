# SmartSpectra–LBH Interoperability Contract
Project: SmartSpectra Edge Sentinel — Presage SmartSpectra SDK Challenge
Status: Android prototype with experimental in-memory LBH bridge and unit test suite (27 tests passing)
Document date: November 14, 2026

## Purpose and scope
This document records how this challenge project integrates Presage SmartSpectra with an experimental in-memory LBH (Lenguaje Binario HormigasAIS) encoder and bridge, while keeping the repository separate from HormigasAIS production services and infrastructure.

The Android application uses the SmartSpectra SDK version 3.4.0 to process camera input, evaluate session stability via `SmartSpectraConsensusEvaluator`, and pass data through `PresageLbhBridge` and `LbhEncoder` to generate a 16-byte fixed LBH frame in memory. Laboratory tests separately evaluate public Python SDK packets (`lbh-sdk`) and the experimental 16-byte format.

The project does not include production credentials, production agents, private source code, or a connection to HormigasAIS production systems.

## Android application and SmartSpectra integration
The Android prototype integrates SmartSpectra SDK version 3.4.0. The app presents an in-app consent dialog, checks camera permissions, and runs an experimental session consensus layer requiring a 20-second stability window over valid pulse/breathing pairs (`stable=true`, finite confidence, increasing timestamps). 

When stability criteria are met, `PresageLbhBridge` and `LbhEncoder` construct a validated 16-byte LBH frame in memory. The app sets `imageOutputEnabled = false` and does not transmit LBH frames over the network or persist them to disk. 

Presage documents session telemetry as enabled by default. This app does not explicitly disable telemetry. See Presage’s telemetry documentation and this project's consent notice.

## Local laboratory test suites
The repository includes automated test suites covering:
1. Public Python package tests (`lbh-sdk` 0.3.1).
2. Experimental 16-byte fixed-frame serialization and boundary validation in Python (`verify_lbh_vector.py`).
3. Android unit tests for the consensus evaluator, field mappers, bridge, and encoder (27 tests passing).

## Consent, privacy, and non-medical use
The app presents a clear consent dialog detailing the experimental nature of the 20-second session consensus and in-memory LBH encoding. This prototype is not a medical device and must not be presented as providing diagnosis, treatment, or medical decision-making.

## References
- Presage SmartSpectra SDK documentation
- LBH SDK on PyPI
- Public LBH v2.0 specification (Zenodo DOI 10.5281/zenodo.19177759)
