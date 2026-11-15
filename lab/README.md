# SmartSpectra Edge Sentinel Laboratory

This laboratory isolates serialization tests and prototype experiments submitted for the challenge. It is not an operating system sandbox and does not grant camera access.

## Components

- `simulator/`: synthetic value generator, experimental 16-byte LBH format tests, and public contracts (lbh-sdk).
- `android-integration/`: dedicated space for integration contracts and validation between the Android app, SmartSpectra, and the LBH bridge.
- `hormigas/`: five versioned declarative contracts, schema, contract loader, read-only security gate, test suites, and deterministic local runtime.
- `../android/`: main Android project directory (including the 20-second session consensus evaluator, PresageLbhBridge, and LbhEncoder).

## Rules for Experiments

- Identify each run as simulated or as a real Presage signal from the app.
- Do not present random values as measurements obtained from a person.
- Do not save or transmit video or biometric signals; bridge data operates strictly in volatile memory and is discarded after encoding.
- Do not include API keys, passwords, tokens, or private keys in the repository.
- Do not perform diagnosis, treatment, or medical decisions.

The current local 16-byte binary format — header 0xAE, two big-endian float32 values, and seven reserved bytes — operates alongside the 20-second temporal consensus evaluator implemented in the app's codebase.
