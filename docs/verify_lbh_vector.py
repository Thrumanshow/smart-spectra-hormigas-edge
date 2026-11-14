#!/usr/bin/env python3
# ============================================================================
# verify_lbh_vector.py
# ============================================================================
# Verificación independiente del vector canónico LBH.
#
# No depende de ningún componente del proyecto. Cualquier revisor con Python 3
# puede ejecutar este script para confirmar que el vector canónico decodifica
# a los valores declarados (pulse=72.5 BPM, breathing=16.0 brpm) y que la
# codificación en sentido inverso reproduce el mismo vector.
#
# Uso:
#     python3 verify_lbh_vector.py
#
# Salida esperada:
#     vector    : ae429100004180000000000000000000
#     header    : 0xAE
#     pulse     : 72.5   (esperado 72.5)
#     breathing : 16.0   (esperado 16.0)
#     reserved  : 00000000000000 (7 bytes)
#     OK: el vector canónico decodifica a pulse=72.5, breathing=16.0
#     OK: la codificación produce el vector canónico
# ============================================================================

import struct
import sys

CANONICAL = "ae429100004180000000000000000000"
EXPECTED_PULSE = 72.5
EXPECTED_BREATHING = 16.0
EXPECTED_HEADER = 0xAE
FRAME_SIZE = 16
RESERVED_SIZE = 7


def decode(vector_hex: str) -> tuple[int, float, float, bytes]:
    """Decodifica una trama LBH hex y retorna (header, pulse, breathing, reserved)."""
    if len(vector_hex) != 32:
        raise ValueError(f"Se esperaban 32 caracteres hex, se obtuvieron {len(vector_hex)}")
    b = bytes.fromhex(vector_hex)
    if len(b) != FRAME_SIZE:
        raise ValueError(f"Se esperaban {FRAME_SIZE} bytes, se obtuvieron {len(b)}")
    if b[0] != EXPECTED_HEADER:
        raise ValueError(f"Cabecera 0x{b[0]:02x} != 0x{EXPECTED_HEADER:02X}")
    pulse, breathing = struct.unpack(">ff", b[1:9])
    reserved = b[9:]
    if len(reserved) != RESERVED_SIZE:
        raise ValueError(
            f"Se esperaban {RESERVED_SIZE} bytes reserved, se obtuvieron {len(reserved)}"
        )
    return b[0], pulse, breathing, reserved


def encode(pulse: float, breathing: float) -> bytes:
    """Codifica pulse y breathing en una trama LBH de 16 bytes."""
    return (
        bytes([EXPECTED_HEADER])
        + struct.pack(">ff", pulse, breathing)
        + bytes(RESERVED_SIZE)
    )


def main() -> int:
    header, pulse, breathing, reserved = decode(CANONICAL)

    print(f"vector    : {CANONICAL}")
    print(f"header    : 0x{header:02X}")
    print(f"pulse     : {pulse}   (esperado {EXPECTED_PULSE})")
    print(f"breathing : {breathing}   (esperado {EXPECTED_BREATHING})")
    print(f"reserved  : {reserved.hex()} ({len(reserved)} bytes)")

    if pulse != EXPECTED_PULSE:
        print(f"FAIL: pulse {pulse} != {EXPECTED_PULSE}", file=sys.stderr)
        return 1
    if breathing != EXPECTED_BREATHING:
        print(f"FAIL: breathing {breathing} != {EXPECTED_BREATHING}", file=sys.stderr)
        return 1
    if any(reserved):
        print(f"FAIL: reserved no es cero: {reserved.hex()}", file=sys.stderr)
        return 1

    print("OK: el vector canónico decodifica a pulse=72.5, breathing=16.0")

    # Verificación inversa: codificar debe reproducir el vector canónico
    reencoded = encode(EXPECTED_PULSE, EXPECTED_BREATHING)
    if reencoded.hex() != CANONICAL:
        print(
            f"FAIL: la codificación produjo {reencoded.hex()} != {CANONICAL}",
            file=sys.stderr,
        )
        return 1
    print("OK: la codificación produce el vector canónico")

    return 0


if __name__ == "__main__":
    sys.exit(main())
