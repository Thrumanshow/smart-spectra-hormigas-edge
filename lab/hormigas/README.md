# HormigasAIS: Contracts and Local Runtime

## Scope

This folder contains five versioned declarative contracts, a JSON schema, a contract loader, a read-only security gate, test suites, and a deterministic local runtime.

The runtime loads contracts as data and coordinates five explicit Python functions: count exploration, Git/JDK review, security scan, relay filtering, and coordination. It does not execute code contained within JSON files or ceremony scripts. It uses no network, Logcat, APKs, external Presage configuration, real keys, or AI models; nor does it compile the application.

This runtime demonstrates the execution of these local modules and configured checks. **It does not demonstrate autonomous agents, training, invisible communication, or an XOXO bus.** The transport of the prototype consists of local function calls.

In the master ant contract, `runtime_implementado: false` refers to the XOXO runtime or bus declared by the contract. It does not deny the existence of the local runtime described here. A more specific name for that field should be considered in a future schema version, such as `xoxo_bus_runtime_implementado`.

## Content

- `contracts/v1/`: Versioned declarative contracts for the relay, security, nurse, scout, and master ants. Contracts describe rules and responsibilities; they are not executable agents on their own.
- `schema/hormiga-contract-v1.schema.json`: JSON Schema Draft 2020-12, identified as `urn:hormigasais:contract:schema:1.0`.
- `tools/contract_loader.py`: Validates version, protocol, castes, and mandatory rules. Loads JSON as data, never as code.
- `tools/security_gate.py`: Inspects repository files or a candidate APK, reporting paths and rule codes, never detected values. Explicitly omits `local.properties`, `.env`, `presage.properties`, and signing keystores.
- `tests/`: Tests for the loader and security gate, including synthetic canary keys.
- `runtime/local_runtime.py`: Orchestrates the five local functions and emits states and counters.
- `runtime/tests/`: Tests for the relay filter. It allows only approved status values and integer counters, and rejects unauthorized fields, including biometrics and credentials.

## Verification

Run these commands from the repository root:

```sh
python3 lab/hormigas/tools/contract_loader.py

PYTHONDONTWRITEBYTECODE=1 \
  python3 -m unittest discover -s lab/hormigas/tests -v

python3 lab/hormigas/tools/security_gate.py --repo .

python3 lab/hormigas/runtime/local_runtime.py --repo .

PYTHONDONTWRITEBYTECODE=1 \
  python3 -m unittest discover -s lab/hormigas/runtime/tests -v
```

`RUNTIME_DEMO=PASS` means the local functions and configured checks completed successfully. It does not demonstrate autonomous agents, training, invisible communication, or an XOXO bus.

To inspect an existing candidate APK:
```sh
python3 lab/hormigas/tools/security_gate.py \
  --artifact android/app/build/outputs/apk/debug/app-debug.apk \
  --public-candidate
```

The gate deliberately blocks a client APK containing the `PRESAGE_API_KEY` symbol, even if the field is empty. It does not publish or upload files. A client APK containing customer credentials must not be published.

## Security Boundaries

Canary tests verify that a test key blocks the candidate and that its value does not appear in `stdout` or `stderr`. Recognizable placeholders —such as `YOUR_*`, `PLACEHOLDER`, or `REPLACE`— are not considered active keys.

These tests do not guarantee the detection of all possible secrets, especially unknown, encoded, or obfuscated values. A real publication additionally requires a build without client credentials, APK review, and an authentication flow compatible with the provider.

The relay allows only non-sensitive states and counters. It rejects biometrics, LBH frames, keys, tokens, and other unauthorized fields; it does not use Logcat as a transport channel.
