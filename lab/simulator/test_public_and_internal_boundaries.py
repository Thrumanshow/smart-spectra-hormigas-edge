"""Prueba de laboratorio: límites público e interno, evaluados por separado.

No expone ni reproduce la implementación privada de HormigasAIS.
Los datos son sintéticos. No representa una medición Presage en vivo.

Límite A: adaptador experimental local -> frame fijo de 16 bytes.
Límite B: payload de prueba -> interfaz pública Python lbh_sdk.
No se afirma que ambas representaciones sean equivalentes o convertibles.
"""

import json
import struct
import unittest

import lbh_sdk
from lbh_presage_adapter import (
    PresageMetrics,
    encode_lbh,
    encode_or_reject,
)

# Valores de ensayo tomados de las pruebas locales existentes.
# No se declaran aquí como identificadores oficiales para datos biométricos.
PUBLIC_TEST_HEADER = "4c420000"
PUBLIC_TEST_TYPE = "4655454c"


class BoundaryTests(unittest.TestCase):
    def setUp(self):
        # Solo datos sintéticos para probar validación y serialización.
        self.metrics = PresageMetrics(
            pulse_bpm=72.5,
            breathing_rate_bpm=16.0,
            pulse_confidence=0.9,
            breathing_confidence=0.8,
        )

    def test_experimental_fixed_16_byte_boundary(self):
        """Valida solo el formato experimental local de 16 bytes."""
        frame = encode_lbh(self.metrics)

        self.assertIsInstance(frame, bytes)
        self.assertEqual(len(frame), 16)
        self.assertEqual(frame[0], 0xAE)
        self.assertEqual(struct.unpack_from(">f", frame, 1)[0], 72.5)
        self.assertEqual(struct.unpack_from(">f", frame, 5)[0], 16.0)
        self.assertEqual(frame[9:], bytes(7))

    def test_public_lbh_sdk_boundary_round_trip(self):
        """Valida por separado el contrato visible del SDK Python público."""
        payload = json.dumps(
            {
                "project": "presage-challenge-lab",
                "origin": "synthetic_test",
                "signal": "pulse",
                "value": 72.5,
                "unit": "bpm",
                "is_live": False,
            },
            separators=(",", ":"),
        )

        packet = lbh_sdk.encode_packet(
            PUBLIC_TEST_HEADER,
            PUBLIC_TEST_TYPE,
            payload,
        )

        self.assertIsInstance(packet, str)
        self.assertTrue(lbh_sdk.validate_packet(packet))

        decoded = lbh_sdk.decode_packet(packet)
        self.assertEqual(decoded["header"], PUBLIC_TEST_HEADER)
        self.assertEqual(decoded["type_code"], PUBLIC_TEST_TYPE)
        self.assertEqual(decoded["payload"], payload)
        self.assertEqual(decoded["length"], len(payload.encode("utf-8")))

        data = json.loads(decoded["payload"])
        self.assertIs(data["is_live"], False)
        self.assertEqual(data["origin"], "synthetic_test")

    def test_invalid_confidence_is_rejected_by_experimental_adapter(self):
        """No se crea un frame experimental si la confianza no es válida."""
        invalid = PresageMetrics(
            pulse_bpm=72.5,
            breathing_rate_bpm=16.0,
            pulse_confidence=0.0,
            breathing_confidence=0.8,
        )
        self.assertIsNone(encode_or_reject(invalid))

    def test_two_representations_are_not_claimed_to_be_identical(self):
        """Documenta que son formatos distintos, sin intentar convertirlos."""
        internal_experimental = encode_lbh(self.metrics)
        public_payload = json.dumps(
            {"origin": "synthetic_test", "is_live": False},
            separators=(",", ":"),
        )
        public_packet = lbh_sdk.encode_packet(
            PUBLIC_TEST_HEADER,
            PUBLIC_TEST_TYPE,
            public_payload,
        )

        self.assertIsInstance(internal_experimental, bytes)
        self.assertEqual(len(internal_experimental), 16)
        self.assertIsInstance(public_packet, str)
        # No se compara igualdad ni se afirma equivalencia entre formatos.


if __name__ == "__main__":
    unittest.main(verbosity=2)
