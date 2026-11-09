"""Prueba mecánica del contrato público LBH v2.0.

Usa solo un evento FUEL sintético; no representa datos Presage.
No afirma que FUEL sea un tipo biométrico ni que este paquete sea el
frame experimental interno de 16 bytes.
"""

import json
import unittest

import lbh_sdk


# Ejemplos publicados en LBH_SPEC_v2.0.md:
# A16 / versión 00, y FUEL como evento de activación de prueba.
HEADER = "41313600"
TYPE_CODE_FUEL = "4655454c"


class PublicLbhContractTests(unittest.TestCase):
    def setUp(self):
        self.payload = json.dumps(
            {
                "event": "synthetic_fuel_test",
                "origin": "local_test",
                "is_live": False,
            },
            separators=(",", ":"),
        )
        self.payload_bytes = self.payload.encode("utf-8")

    def test_packet_layout_matches_public_spec(self):
        packet = lbh_sdk.encode_packet(
            HEADER,
            TYPE_CODE_FUEL,
            self.payload,
        )

        expected_length = format(len(self.payload_bytes), "08x")
        expected = (
            HEADER
            + TYPE_CODE_FUEL
            + expected_length
            + self.payload_bytes.hex()
        )

        self.assertEqual(packet, expected)
        self.assertTrue(lbh_sdk.validate_packet(packet))

    def test_decoded_byte_length_and_payload_round_trip(self):
        packet = lbh_sdk.encode_packet(
            HEADER,
            TYPE_CODE_FUEL,
            self.payload,
        )
        decoded = lbh_sdk.decode_packet(packet)

        self.assertEqual(decoded["header"], HEADER)
        self.assertEqual(decoded["type_code"], TYPE_CODE_FUEL)
        self.assertEqual(decoded["length"], len(self.payload_bytes))
        self.assertEqual(decoded["payload"], self.payload)

        # Tras convertir la cadena hexadecimal a bytes:
        # 4 bytes HEADER + 4 TYPE_CODE + 4 LENGTH + payload.
        raw_packet = bytes.fromhex(packet)
        self.assertEqual(len(raw_packet), 12 + len(self.payload_bytes))
        self.assertNotEqual(len(raw_packet), 16)

    def test_wrong_declared_payload_length_is_rejected(self):
        packet = lbh_sdk.encode_packet(
            HEADER,
            TYPE_CODE_FUEL,
            self.payload,
        )
        wrong_length = format(len(self.payload_bytes) + 1, "08x")
        malformed = packet[:16] + wrong_length + packet[24:]

        self.assertFalse(lbh_sdk.validate_packet(malformed))

    def test_event_is_explicitly_synthetic(self):
        packet = lbh_sdk.encode_packet(
            HEADER,
            TYPE_CODE_FUEL,
            self.payload,
        )
        decoded = lbh_sdk.decode_packet(packet)
        data = json.loads(decoded["payload"])

        self.assertEqual(data["event"], "synthetic_fuel_test")
        self.assertIs(data["is_live"], False)


if __name__ == "__main__":
    unittest.main(verbosity=2)
