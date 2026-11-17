### What I Built

**SmartSpectra Edge Sentinel** is an Android prototype built and tested on a Samsung Galaxy A16 from Termux. It integrates Presage SmartSpectra 3.4.0, presents an in-app camera-consent step, checks Android camera permission, and displays the pulse value reported by SmartSpectra together with the status of an experimental session check. That check, `SmartSpectraConsensusEvaluator`, looks for a 20-second window of valid pulse/breathing pairs. When its criteria are met, `PresageLbhBridge` and `LbhEncoder` build a 16-byte LBH frame in memory. This is an experimental prototype, not a medical device.

A separate Python laboratory exercises the public `lbh-sdk` packet format and the experimental fixed 16-byte format with synthetic inputs. Those tests do not show that the two formats are equivalent. The Android unit tests (27) cover the evaluator, the field mappers, the bridge and the encoder.

### How It Responds to Human Signals

The app shows the latest pulse in beats per minute from SmartSpectra and reports the state of the stability window (collecting, reset, or stable). The experimental evaluator accepts only samples with `stable=true`, finite and positive confidence, increasing timestamps, a pair skew of at most 100 ms and gaps between pairs of at most 3 seconds. The 20 seconds, 100 ms and 3 seconds are provisional trial parameters, not medical thresholds and not SDK requirements.

When the window is stable, the encoder builds a 16-byte frame: header `0xAE`, two big-endian float32 values (pulse, breathing) and seven zero bytes. It rejects NaN or infinite values, values outside experimental ranges (pulse 40 to 110, breathing 5 to 40) and non-positive confidence. The byte array is cleared after encoding. As far as the current source code shows, the app does not deliberately store the fields or the frame, write them to logs, or transmit them over a network.

No simulated values are used in the app's live display; synthetic values appear only in the Python laboratory and the unit tests. The Presage SDK's own processing and telemetry are independent of this app, and the app does not disable the SDK telemetry. The prototype does not provide medical interpretation or make treatment decisions.
