# Android Integration Tests

This folder groups workflow contracts and validations between the Android app, the SmartSpectra SDK, and the in-memory LBH encoder.

The actual implementation code (integrated in `android/app/`) uses `SmartSpectraConsensusEvaluator` to enforce a 20-second stability window over pulse/respiratory pairs, channeling them through `PresageLbhBridge` and `LbhEncoder` to build a 16-byte LBH frame strictly in volatile memory.

The tests ensure threshold and range validation, avoiding saving video or biometric data to disk or transmitting it over the network. Do not store API keys, private OAuth configurations, tokens, participant videos, or identifiable data here.
