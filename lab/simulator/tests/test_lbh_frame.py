#!/usr/bin/env python3
"""Pruebas del formato LBH experimental del prototipo."""

import pathlib
import struct
import sys
import unittest

SIMULATOR_DIR = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SIMULATOR_DIR))

from simulate_lbh import build_frame


class TestLBHFrame(unittest.TestCase):
    def test_frame_has_16_bytes_and_header(self):
        frame = build_frame(72.5, 16.0)
        self.assertEqual(len(frame), 16)
        self.assertEqual(frame[0], 0xAE)

    def test_values_are_big_endian_float32(self):
        pulse = 72.5
        breathing = 16.0
        frame = build_frame(pulse, breathing)

        self.assertEqual(struct.unpack_from(">f", frame, 1)[0], pulse)
        self.assertEqual(struct.unpack_from(">f", frame, 5)[0], breathing)

    def test_reserved_bytes_are_zero(self):
        frame = build_frame(72.5, 16.0)
        self.assertEqual(frame[9:], bytes(7))


if __name__ == "__main__":
    unittest.main(verbosity=2)
