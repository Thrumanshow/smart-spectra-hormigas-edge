import json
import tempfile
import unittest
from pathlib import Path

from tools.contract_loader import ContractError, load_contracts, validate_contract

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
CONTRACTS = PACKAGE_ROOT / "contracts" / "v1"


class ContractLoaderTests(unittest.TestCase):
    def test_all_versioned_contracts_load(self):
        loaded = load_contracts(CONTRACTS)
        self.assertEqual(len(loaded), 5)
        self.assertEqual(loaded["hormiga_de_relevo"]["salida_permitida"], ["estado", "contadores"])
        self.assertFalse(loaded["hormiga_maestra"]["runtime_implementado"])

    def test_rejects_unknown_schema_version(self):
        data = json.loads((CONTRACTS / "hormiga_exploradora.contract.json").read_text())
        data["schema_version"] = "9.9"
        with self.assertRaises(ContractError):
            validate_contract(data)

    def test_rejects_logcat_destination_for_relay(self):
        data = json.loads((CONTRACTS / "hormiga_de_relevo.contract.json").read_text())
        data["destino_local"] = "adb_logcat_tag_LBH_EDGE"
        with self.assertRaises(ContractError):
            validate_contract(data)

    def test_rejects_biometric_relay_output(self):
        data = json.loads((CONTRACTS / "hormiga_de_relevo.contract.json").read_text())
        data["salida_permitida"].append("biometria")
        with self.assertRaises(ContractError):
            validate_contract(data)

    def test_rejects_missing_security_restriction(self):
        data = json.loads((CONTRACTS / "hormiga_de_seguridad.contract.json").read_text())
        data["restricciones"].remove("prohibido_tocar_config_externa")
        with self.assertRaises(ContractError):
            validate_contract(data)

    def test_rejects_duplicate_or_missing_contract_set(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            schema_dir = root / "schema"
            schema_dir.mkdir()
            (schema_dir / "hormiga-contract-v1.schema.json").write_text(
                (PACKAGE_ROOT / "schema" / "hormiga-contract-v1.schema.json").read_text()
            )
            contract_dir = root / "contracts" / "v1"
            contract_dir.mkdir(parents=True)
            (contract_dir / "only.contract.json").write_text(
                (CONTRACTS / "hormiga_de_relevo.contract.json").read_text()
            )
            with self.assertRaises(ContractError):
                load_contracts(contract_dir)


if __name__ == "__main__":
    unittest.main()
