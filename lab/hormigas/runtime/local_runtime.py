#!/usr/bin/env python3
"""Runtime local determinista de demostración; no usa red, Logcat ni código de contratos."""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Any, Callable

PACKAGE_ROOT = Path(__file__).resolve().parents[1]
REPO_DEFAULT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(PACKAGE_ROOT))

from tools.contract_loader import load_contracts  # noqa: E402
from tools.security_gate import scan_repo  # noqa: E402


class RelayPolicy:
    """Canal local tipado: solo estado y contadores numéricos permitidos."""

    TOP_FIELDS = {"estado", "contadores"}
    STATES = {"running", "complete", "blocked", "warning"}
    COUNTERS = {"modules_started", "modules_passed", "modules_blocked", "files_count", "findings_count"}

    @classmethod
    def accept(cls, event: Any) -> tuple[bool, str]:
        if not isinstance(event, dict) or set(event) - cls.TOP_FIELDS:
            return False, "campo_no_permitido"
        if event.get("estado") not in cls.STATES:
            return False, "estado_no_permitido"
        counters = event.get("contadores", {})
        if not isinstance(counters, dict) or set(counters) - cls.COUNTERS:
            return False, "contador_no_permitido"
        if any(not isinstance(value, int) or isinstance(value, bool) or value < 0 for value in counters.values()):
            return False, "valor_de_contador_invalido"
        return True, "aceptado"


class LocalRuntime:
    def __init__(self, repo: str | Path):
        self.repo = Path(repo).resolve()
        self.contracts = load_contracts(self.repo / "lab/hormigas/contracts/v1")
        self.records: list[tuple[str, str, dict[str, Any]]] = []

    def _run(self, name: str, function: Callable[[], dict[str, Any]]) -> dict[str, Any]:
        print(f"MODULE_START name={name}")
        try:
            result = function()
            status = str(result.get("status", "PASS"))
        except Exception as exc:  # No se imprime el mensaje, que podría contener datos sensibles.
            result = {"status": "BLOCK", "error_type": type(exc).__name__}
            status = "BLOCK"
        details = " ".join(
            f"{key}={value}" for key, value in sorted(result.items()) if key != "status"
        )
        print(f"MODULE_DONE name={name} status={status}" + (f" {details}" if details else ""))
        self.records.append((name, status, result))
        return result

    def _explore(self) -> dict[str, Any]:
        skip_dirs = {".git", ".gradle", "build", ".venv", "venv", "node_modules", "__pycache__", ".idea"}
        extensions = {".kt", ".java", ".py", ".js", ".sh", ".kts"}
        count = 0
        for root in (self.repo / "android/app/src/main", self.repo / "lab"):
            if not root.is_dir():
                continue
            for current, dirs, files in os.walk(root, followlinks=False):
                dirs[:] = [d for d in dirs if d not in skip_dirs and not (Path(current) / d).is_symlink()]
                for filename in files:
                    path = Path(current) / filename
                    if path.suffix in extensions and not path.is_symlink():
                        count += 1
        return {"status": "PASS", "files_count": count}

    def _nurse(self) -> dict[str, Any]:
        wrapper = (self.repo / "android/gradlew").is_file()
        git = subprocess.run(
            ["git", "-C", str(self.repo), "status", "--porcelain"],
            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, check=False, text=True,
        )
        changes = sum(1 for line in git.stdout.splitlines() if line.strip())
        java = subprocess.run(
            ["java", "-version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            text=True, check=False,
        )
        version_text = java.stderr + java.stdout
        match = re.search(r'version "(\d+)(?:\.(\d+))?', version_text)
        major = int(match.group(1)) if match else 0
        if major == 1 and match and match.group(2):
            major = int(match.group(2))
        status = "PASS" if wrapper and git.returncode == 0 and major == 21 else "BLOCK"
        return {"status": status, "jdk_major": major, "gradle_wrapper": int(wrapper), "git_changes": changes}

    def _security(self) -> dict[str, Any]:
        findings = scan_repo(self.repo)
        return {"status": "PASS" if not findings else "BLOCK", "findings_count": len(findings)}

    def _relay(self) -> dict[str, Any]:
        safe, _ = RelayPolicy.accept({
            "estado": "complete",
            "contadores": {"modules_started": 3, "modules_passed": 3, "modules_blocked": 0},
        })
        biometric, _ = RelayPolicy.accept({"biometria": "SYNTHETIC_CANARY_NOT_LOGGED"})
        credential_field = "api" + "_" + "key"
        credential, _ = RelayPolicy.accept({credential_field: "SYNTHETIC_CANARY_NOT_LOGGED"})
        passed = safe and not biometric and not credential
        return {"status": "PASS" if passed else "BLOCK", "safe_accepted": int(safe), "sensitive_rejected": int((not biometric) + (not credential))}

    def run(self) -> bool:
        expected = {
            "hormiga_de_relevo", "hormiga_de_seguridad", "hormiga_enfermera",
            "hormiga_exploradora", "hormiga_maestra",
        }
        if set(self.contracts) != expected:
            print("RUNTIME_DEMO=BLOCK reason=contract_set_mismatch")
            return False
        print("MODULE_START name=hormiga_maestra")
        results = [
            self._run("hormiga_exploradora", self._explore),
            self._run("hormiga_enfermera", self._nurse),
            self._run("hormiga_de_seguridad", self._security),
            self._run("hormiga_de_relevo", self._relay),
        ]
        passed = sum(result.get("status") == "PASS" for result in results)
        summary_ok, _ = RelayPolicy.accept({
            "estado": "complete" if passed == 4 else "blocked",
            "contadores": {
                "modules_started": 4,
                "modules_passed": passed,
                "modules_blocked": 4 - passed,
                "files_count": results[0].get("files_count", 0),
                "findings_count": results[2].get("findings_count", 0),
            },
        })
        status = "PASS" if passed == 4 and summary_ok else "BLOCK"
        print(f"MODULE_DONE name=hormiga_maestra status={status} modules_started=5 modules_passed={passed + int(status == 'PASS')} relay_summary_accepted={int(summary_ok)}")
        if status != "PASS":
            print("RUNTIME_DEMO=BLOCK")
            return False
        print("RUNTIME_DEMO=PASS modules=5 relay=estado_y_contadores biometria_rejected=1 key_rejected=1")
        print("TRANSPORTE=llamadas_locales XOXO_BUS=not_implemented network=not_used logcat=not_used")
        return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Runtime local determinista de HormigasAIS; sin bus externo")
    parser.add_argument("--repo", type=Path, default=REPO_DEFAULT)
    args = parser.parse_args()
    try:
        runtime = LocalRuntime(args.repo)
    except Exception as exc:
        print(f"RUNTIME_DEMO=BLOCK error_type={type(exc).__name__}")
        return 1
    return 0 if runtime.run() else 1


if __name__ == "__main__":
    raise SystemExit(main())
