"""Ensayo simulado de payload del reto codificado con el SDK público LBH."""

import json
import unittest

import lbh_sdk


LAB_HEADER = "4c420000"       # ID neutral "LB" + versión 0000
FUEL_TYPE = "4655454c"        # TYPE_CODE FUEL de la especificación


class LbhSdkPayloadTests(unittest.TestCase):
    def setUp(self):
        # Datos inventados para probar el transporte; no son mediciones Presage.
        self.payload = json.dumps(
            {
                "project": "presage-challenge-lab",
                "origin": "simulated_test",
                "signal": "pulse",
                "value": 72.5,
                "unit": "bpm",
                "is_live": False,
            },
            separators=(",", ":"),
        )

    def test_sdk_encodes_and_validates_packet(self):
        frame = lbh_sdk.encode_packet(LAB_HEADER, FUEL_TYPE, self.payload)

        self.assertIsInstance(frame, str)
        self.assertTrue(lbh_sdk.validate_packet(frame))

    def test_header_type_and_payload_round_trip(self):
        frame = lbh_sdk.encode_packet(LAB_HEADER, FUEL_TYPE, self.payload)
        decoded = lbh_sdk.decode_packet(frame)

        self.assertEqual(decoded["header"], LAB_HEADER)
        self.assertEqual(decoded["type_code"], FUEL_TYPE)
        self.assertEqual(decoded["payload"], self.payload)

        data = json.loads(decoded["payload"])
        self.assertEqual(data["project"], "presage-challenge-lab")
        self.assertEqual(data["origin"], "simulated_test")
        self.assertIs(data["is_live"], False)

    def test_declared_length_matches_utf8_payload_bytes(self):
        frame = lbh_sdk.encode_packet(LAB_HEADER, FUEL_TYPE, self.payload)
        decoded = lbh_sdk.decode_packet(frame)

        self.assertEqual(decoded["length"], len(self.payload.encode("utf-8")))


if __name__ == "__main__":
    unittest.main(verbosity=2)
