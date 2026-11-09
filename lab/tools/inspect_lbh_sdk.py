#!/usr/bin/env python3
"""Inspección local y no destructiva del SDK LBH instalado en lab/.venv."""

import inspect
import json
import lbh_sdk

FUNCTIONS = ("encode_packet", "decode_packet", "validate_packet")


def main():
    print("Módulo:", lbh_sdk.__file__)
    print("\nAPI detectada:")

    for name in FUNCTIONS:
        function = getattr(lbh_sdk, name, None)
        if function is None:
            print(f"- {name}: no disponible")
            continue

        try:
            signature = inspect.signature(function)
        except (TypeError, ValueError):
            signature = "(firma no introspectable)"

        print(f"- {name}{signature}")
        try:
            print("  Doc:", inspect.getdoc(function) or "(sin docstring)")
        except TypeError:
            pass

    # Prueba inocua con payload EXPLÍCITAMENTE SIMULADO.
    # FUEL es el tipo de paquete usado aquí para una señal/evento de prueba.
    if all(callable(getattr(lbh_sdk, name, None)) for name in FUNCTIONS):
        header = "41313600"       # A16, versión 00 según ejemplo de la spec
        type_code = "4655454c"    # FUEL
        payload = json.dumps(
            {
                "origin": "simulated_test",
                "signal": "pulse",
                "value": 72.5,
                "unit": "bpm",
                "is_live": False,
            },
            separators=(",", ":"),
        )

        print("\nPrueba de codificación simulada:")
        try:
            frame = lbh_sdk.encode_packet(header, type_code, payload)
            print("Tipo de retorno:", type(frame).__name__)
            print("Frame:", repr(frame))
            print("Validación:", lbh_sdk.validate_packet(frame))
            print("Decodificado:", repr(lbh_sdk.decode_packet(frame)))
        except Exception as exc:
            print("La llamada de ejemplo no coincide con la API instalada:")
            print(type(exc).__name__ + ":", exc)
            print("Usa las firmas/documentación impresas arriba para ajustar la prueba.")


if __name__ == "__main__":
    main()
