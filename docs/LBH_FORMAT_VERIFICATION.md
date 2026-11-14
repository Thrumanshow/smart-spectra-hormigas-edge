# LBH Frame Format Verification

**Verification date:** 2026-11-14
**Component verified:** `LbhEncoder` (local LBH encoder)
**Mode:** offline verification — no network transmission, no ADB, no Logcat.
**Status:** experimental. The frame is **not** a clinical measurement and must not be used for diagnosis, treatment or any medical decision.

## 1. Summary

This document describes the binary layout of the LBH frame and provides a canonical test vector that any reviewer can verify independently, without access to a device or to the private integration source code.

Scope of the claims made here:

- In the private integration build, the app evaluates samples with an experimental session-consensus layer (states `COLLECTING`, `RESET`, `SESSION_STABLE_EXPERIMENTAL`) and hands the results to `PresageLbhBridge`, which returns `Accepted` or `Rejected`. Frames are built in-process by the bridge and the encoder.
- A keyword search of the bridge source (2026-11-14) found no network, file-output or logging calls. This is a code search, not a formal audit.
- This document makes no claim about the network behaviour of the SmartSpectra SDK. That is covered separately in the privacy and consent text of the project.

## 2. LBH frame format

An LBH frame is 16 bytes (32 hexadecimal characters), big-endian.

| Offset | Bytes | Field     | Type          | Unit           |
|-------:|------:|-----------|---------------|----------------|
| 0      | 1     | header    | uint8         | —              |
| 1      | 4     | pulse     | float32 (BE)  | beats/min      |
| 5      | 4     | breathing | float32 (BE)  | breaths/min    |
| 9      | 7     | reserved  | uint8[7]      | — (zeros)      |

- The header byte is `0xAE` in every valid frame.
- `pulse` and `breathing` are IEEE 754 32-bit floats, big-endian.
- The 7 reserved bytes are always emitted as zero.

## 3. Canonical vector

For the reference values

```
pulse     = 72.5  BPM
breathing = 16.0  brpm
```

the encoder produces exactly:

```
ae429100004180000000000000000000
```

Byte-by-byte breakdown:

```
ae                     header   (0xAE)
42 91 00 00            pulse    = 72.5   (float32 BE)
41 80 00 00            breathing = 16.0  (float32 BE)
00 00 00 00 00 00 00   reserved (7 zero bytes)
```

## 4. Independent reproduction

The following Python scripts do not depend on any project component. Any reviewer can run them to confirm the canonical vector. Each one ends with an `OK:` line when it succeeds.

Decoding (bytes → values):

```python
import struct

CANONICAL = "ae429100004180000000000000000000"

assert len(CANONICAL) == 32, "expected 32 hex characters"
b = bytes.fromhex(CANONICAL)
assert len(b) == 16, "expected 16 bytes"
assert b[0] == 0xAE, f"header 0x{b[0]:02x} != 0xAE"

pulse, breathing = struct.unpack(">ff", b[1:9])

print(f"vector    : {CANONICAL}")
print(f"header    : 0x{b[0]:02X}")
print(f"pulse     : {pulse}  (expected 72.5)")
print(f"breathing : {breathing}  (expected 16.0)")
print(f"reserved  : {b[9:].hex()} (7 bytes)")

assert pulse == 72.5
assert breathing == 16.0
assert b[9:] == bytes(7)
print("OK: the canonical vector decodes to pulse=72.5, breathing=16.0")
```

Encoding (values → bytes):

```python
import struct

pulse = 72.5
breathing = 16.0

frame = bytes([0xAE]) + struct.pack(">ff", pulse, breathing) + bytes(7)
assert len(frame) == 16
assert frame.hex() == "ae429100004180000000000000000000"
print("OK: encoding produces the canonical vector")
```

## 5. Defensive behaviour of the encoder

The encoder validates its inputs before emitting a frame. Invalid input is never encoded. `encode` throws an exception for it; `encodeOrReject` wraps `encode` and returns `null` instead. An input is rejected when any of the following holds:

- Any float field is `NaN` or `±Infinity`.
- `pulse` is outside the configured experimental range (for example 300 BPM).
- `breathing` is outside the configured experimental range (for example 50 brpm).
- `pulseConfidence` or `breathingConfidence` is not finite, or is not greater than 0.

The ranges are experimental, configurable engineering limits (currently pulse 40–110 and breathing 5–40). They are not clinical or physiological reference values.

The encoder is a last line of defence: even if an upstream layer lets an invalid sample through, no frame is produced for it. Intended separation of responsibilities:

- **Consensus layer (`SmartSpectraConsensusEvaluator`, experimental):** temporal logic over incoming samples. It evaluates samples first.
- **Bridge and encoder (`PresageLbhBridge`, `LbhEncoder`):** value validation and binary serialisation of what the bridge receives.

## 6. How to verify on an installation

A frame is generated only inside the app process. To confirm that the encoder produces the canonical vector, in an environment with access to the encoder source, run a unit test that calls:

```kotlin
LbhEncoder.encode(LbhEncoder.Metrics(
    pulseBpm            = 72.5f,
    breathingRateBpm    = 16.0f,
    pulseConfidence     = 0.9f,
    breathingConfidence = 0.9f,
))
```

and checks that the returned `ByteArray` has exactly 16 bytes and that its hexadecimal form is `ae429100004180000000000000000000`.

The confidence fields (`pulseConfidence`, `breathingConfidence`) are not serialised into the 16-byte frame; only `pulse` and `breathing` are. Their purpose is validation before encoding.

## 7. Limitations

- The frame has no integrity mechanism (checksum, HMAC, nonce). Integrity and confidentiality, where required for a given transport, are the responsibility of that transport, not of the encoder.
- The 7-byte `reserved` field is fixed to zero in the current version. Its meaning may change in future format versions without affecting the decoding of `pulse` and `breathing`.
- The values are experimental signal estimates, not clinical measurements.
