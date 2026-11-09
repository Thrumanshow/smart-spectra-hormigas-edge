import struct
import unittest

from lbh_presage_adapter import (
    InvalidSignal,
    PresageMetrics,
    encode_lbh,
    encode_or_reject,
)


class AdapterTests(unittest.TestCase):
    def test_encodes_valid_metrics_into_16_bytes(self):
        metrics = PresageMetrics(
            pulse_bpm=72.5,
            breathing_rate_bpm=16.0,
            pulse_confidence=0.9,
            breathing_confidence=0.8,
        )

        frame = encode_lbh(metrics)

        self.assertEqual(len(frame), 16)
        self.assertEqual(frame[0], 0xAE)
        self.assertEqual(struct.unpack_from(">f", frame, 1)[0], 72.5)
        self.assertEqual(struct.unpack_from(">f", frame, 5)[0], 16.0)
        self.assertEqual(frame[9:], bytes(7))

    def test_rejects_zero_confidence(self):
        metrics = PresageMetrics(
            pulse_bpm=72.5,
            breathing_rate_bpm=16.0,
            pulse_confidence=0.0,
            breathing_confidence=0.8,
        )

        self.assertIsNone(encode_or_reject(metrics))

    def test_rejects_out_of_range_metric(self):
        metrics = PresageMetrics(
            pulse_bpm=120.0,
            breathing_rate_bpm=16.0,
            pulse_confidence=0.9,
            breathing_confidence=0.8,
        )

        with self.assertRaises(InvalidSignal):
            encode_lbh(metrics)


if __name__ == "__main__":
    unittest.main(verbosity=2)
