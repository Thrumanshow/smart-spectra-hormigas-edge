### What I Built

**SmartSpectra Edge Sentinel** is an early prototype exploring how live biometric signals can become lightweight, structured events for edge applications. It maps pulse and breathing-rate values into a compact 16-byte binary frame using a simple header and numeric fields, illustrating a possible bridge between Presage SmartSpectra signals and the HormigasAIS Binary Language (LBH).

The current code is a proof of concept: it generates simulated pulse and breathing values and prints the resulting frame locally. It does not yet connect to the SmartSpectra SDK or transmit data to another device or service.

### How It Responds to Human Signals

The prototype is designed around two human signals: pulse and breathing rate. In its current demonstration, it generates sample values for both every five seconds, encodes them into an LBH frame, and prints the frame in hexadecimal. This shows the intended data flow—from signal values to a compact representation that an edge application could consume.

The current implementation uses simulated data, not live Presage measurements. A future SDK integration would need to obtain consent before camera-based sensing, receive the SDK’s live signals, validate them, and map them into the frame. At this stage, the prototype does not make decisions or provide medical interpretation; it only demonstrates a possible encoding approach.
