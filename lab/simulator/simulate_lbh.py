#!/usr/bin/env python3
"""Simulador experimental de tramas LBH para SmartSpectra Edge Sentinel.

Los valores son ficticios. Este script no se conecta al SDK de Presage.
Formato propuesto para el prototipo: 16 bytes.
  byte 0:     encabezado 0xAE
  bytes 1-4:   pulso, float IEEE-754 big-endian
  bytes 5-8:   respiración, float IEEE-754 big-endian
  bytes 9-15:  reservado, inicializado en cero
"""

import random
import struct
import time

FRAME_SIZE = 16
HEADER = 0xAE


def build_frame(pulse: float, breathing: float) -> bytes:
    """Codifica valores simulados en una trama experimental de 16 bytes."""
    frame = bytearray(FRAME_SIZE)
    frame[0] = HEADER
    struct.pack_into(">f", frame, 1, pulse)
    struct.pack_into(">f", frame, 5, breathing)
    return bytes(frame)


def main() -> None:
    print("SmartSpectra Edge Sentinel — simulación LBH")
    print("Datos ficticios; no hay conexión con el SDK de Presage.")
    print("Formato experimental, pendiente de especificación formal.\\n")

    try:
        while True:
            pulse = 72.5 + random.random() * 2.0
            breathing = 16.0 + random.random() * 0.5
            frame = build_frame(pulse, breathing)

            print(
                f"Pulso: {pulse:.2f} | "
                f"Respiración: {breathing:.2f} | "
                f"Trama ({len(frame)} bytes): {frame.hex()}"
            )
            time.sleep(5)
    except KeyboardInterrupt:
        print("\\nSimulación detenida.")


if __name__ == "__main__":
    main()
