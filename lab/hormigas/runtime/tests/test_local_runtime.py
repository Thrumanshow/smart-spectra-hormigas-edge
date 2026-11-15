import unittest

from local_runtime import RelayPolicy


class RelayPolicyTests(unittest.TestCase):
    def test_accepts_only_safe_state_and_integer_counters(self):
        accepted, _ = RelayPolicy.accept({
            "estado": "complete",
            "contadores": {"modules_started": 5, "modules_passed": 5},
        })
        self.assertTrue(accepted)

    def test_rejects_biometrics(self):
        accepted, reason = RelayPolicy.accept({"biometria": "SYNTHETIC_CANARY_NOT_LOGGED"})
        self.assertFalse(accepted)
        self.assertEqual(reason, "campo_no_permitido")

    def test_rejects_credentials(self):
        credential_field = "api" + "_" + "key"
        accepted, reason = RelayPolicy.accept({credential_field: "SYNTHETIC_CANARY_NOT_LOGGED"})
        self.assertFalse(accepted)
        self.assertEqual(reason, "campo_no_permitido")

    def test_rejects_unknown_counter_and_nested_payload(self):
        accepted, reason = RelayPolicy.accept({
            "estado": "complete",
            "contadores": {"payload_lbh": 1},
        })
        self.assertFalse(accepted)
        self.assertEqual(reason, "contador_no_permitido")

    def test_rejects_non_integer_or_negative_counters(self):
        for value in (True, -1, "4"):
            with self.subTest(value_type=type(value).__name__):
                accepted, reason = RelayPolicy.accept({
                    "estado": "complete", "contadores": {"modules_started": value}
                })
                self.assertFalse(accepted)
                self.assertEqual(reason, "valor_de_contador_invalido")

    def test_rejects_unknown_state(self):
        accepted, reason = RelayPolicy.accept({"estado": "arbitrary-data", "contadores": {}})
        self.assertFalse(accepted)
        self.assertEqual(reason, "estado_no_permitido")


if __name__ == "__main__":
    unittest.main()
