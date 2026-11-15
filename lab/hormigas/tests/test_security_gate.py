import tempfile
import subprocess
import sys
import unittest
import zipfile
from pathlib import Path

from tools.security_gate import format_findings, scan_artifact, scan_repo


def fake_canary():
    # Se construye por fragmentos para que el valor completo no aparezca en el repo.
    return "CANARY_" + "TEST_SECRET_" + "VALUE_0123456789ABCDEF"


class SecurityGateTests(unittest.TestCase):
    def test_fake_key_blocks_repo_scan_without_leaking_value(self):
        canary = fake_canary()
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "fixture.properties").write_text("PRESAGE_API_KEY=" + canary + "\n")
            findings = scan_repo(root)
        report = format_findings(findings, context="repo")
        self.assertTrue(findings)
        self.assertIn("BLOCK", report)
        self.assertNotIn(canary, report)

    def test_fake_key_and_presage_symbol_block_apk_without_leaking_value(self):
        canary = fake_canary()
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "fake-canary.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("classes.dex", b"PRESAGE_API_KEY=" + canary.encode("ascii"))
            findings = scan_artifact(apk)
        report = format_findings(findings, context="artifact")
        self.assertTrue(any(item.rule == "client_presage_key_symbol" for item in findings))
        self.assertTrue(any(item.rule == "secret_assignment" for item in findings))
        self.assertNotIn(canary, report)
        self.assertIn("value=[SUPPRESSED]", report)

    def test_publication_cli_blocks_canary_and_never_logs_its_value(self):
        canary = fake_canary()
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "public-candidate.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("classes.dex", b"PRESAGE_API_KEY=" + canary.encode("ascii"))
            gate = Path(__file__).resolve().parents[1] / "tools" / "security_gate.py"
            result = subprocess.run(
                [sys.executable, str(gate), "--artifact", str(apk), "--public-candidate"],
                text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
            )
        self.assertEqual(result.returncode, 1)
        self.assertIn("BLOCK", result.stdout)
        self.assertNotIn(canary, result.stdout + result.stderr)

    def test_public_apk_with_presage_symbol_is_blocked_even_without_value(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "marker-only.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("classes.dex", b"BuildConfig PRESAGE_API_KEY empty")
            findings = scan_artifact(apk)
        self.assertTrue(any(item.rule == "client_presage_key_symbol" for item in findings))

    def test_clean_fake_apk_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "clean.apk"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("classes.dex", b"application class with no credential field")
            findings = scan_artifact(apk)
        self.assertEqual(findings, [])

    def test_private_key_material_is_reported_without_contents(self):
        canary = "PRIVATE_" + "CANARY_MATERIAL"
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            marker = "-----BEGIN " + "PRIVATE KEY-----"
            (root / "sample.txt").write_text(marker + "\n" + canary)
            findings = scan_repo(root)
        report = format_findings(findings, context="repo")
        self.assertTrue(any(item.rule == "private_key_material" for item in findings))
        self.assertNotIn(canary, report)

    def test_local_properties_is_not_read_by_repo_scan(self):
        canary = fake_canary()
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "local.properties").write_text("PRESAGE_API_KEY=" + canary)
            findings = scan_repo(root)
        self.assertEqual(findings, [])

    def test_example_local_properties_placeholder_is_not_a_secret(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "local.properties.example").write_text(
                "PRESAGE_API_KEY=YOUR_PRESAGE_API_KEY_HERE\n"
            )
            findings = scan_repo(root)
        self.assertEqual(findings, [])


if __name__ == "__main__":
    unittest.main()
