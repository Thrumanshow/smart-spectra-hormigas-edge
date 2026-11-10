### What I Built

**SmartSpectra Edge Sentinel** is an Android prototype built and tested on a Samsung Galaxy A16 from Termux. It integrates Presage SmartSpectra 3.4.0, presents an in-app camera-consent step, checks Android camera permission, and displays the latest pulse value available from `cardio.pulseRateList`. The linked demonstration video shows the app flow and changing pulse readings. This is an experimental prototype, not a medical device.

The Python laboratory is separate from the Android measurement flow. Its tests use synthetic/example inputs to exercise the public `lbh-sdk` packet format and a distinct experimental fixed 16-byte representation. They do not show that the Android app converts a live SmartSpectra value into an LBH packet.

### How It Responds to Human Signals

The Android app currently displays pulse in beats per minute from the latest available SmartSpectra `cardio.pulseRateList` value. The demonstration shows a waiting period before a value appears and then changing pulse readings during the session. The current callback does not extract breathing rate or confidence, even though the app requests cardio and breathing metric groups.

No simulated values are substituted into the Android app's live display. Synthetic values appear only in the separate Python laboratory tests. The Android app does not currently create or transmit LBH frames, and the experimental 16-byte tests are not claimed to be identical to the public variable-length LBH packet format. The prototype does not provide medical interpretation or make treatment decisions.
