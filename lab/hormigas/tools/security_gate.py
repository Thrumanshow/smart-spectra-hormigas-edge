#!/usr/bin/env python3
"""Barrera local de lectura: detecta indicios de secreto sin imprimir valores.

No publica archivos, no lee configuración externa y no modifica el repositorio.
Es deliberadamente conservadora con APKs que contienen PRESAGE_API_KEY.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

SECRET_ASSIGNMENT = re.compile(
    rb"(?i)\b(?:PRESAGE_API_KEY|API_KEY|SECRET_KEY|ACCESS_TOKEN|PRIVATE_KEY)\b"
    rb"\s*[:=]\s*[\"']?([A-Za-z0-9][A-Za-z0-9_./+=:-]{7,})"
)
PRIVATE_KEY_MARKER = re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")
PRESAGE_SYMBOL = b"PRESAGE_API_KEY"
PLACEHOLDERS = {
    b"YOUR_API_KEY", b"REPLACE_ME", b"CHANGEME", b"REDACTED", b"EXAMPLE_KEY",
    b"EMPTY", b"NULL", b"FALSE", b"TODO", b"YOUR_KEY_HERE", b"PRESAGE_API_KEY",
}
PLACEHOLDER_FRAGMENTS = (
    b"YOUR_", b"PLACEHOLDER", b"REPLACE", b"CHANGEME", b"CHANGE_ME",
    b"REDACTED", b"EXAMPLE", b"SAMPLE_", b"TODO", b"INSERT_", b"PASTE_",
)
SKIP_DIRS = {
    ".git", ".gradle", "build", ".venv", "venv", "node_modules", "__pycache__",
    ".idea", ".kotlin", ".pytest_cache",
}
SKIP_CONFIG_NAMES = {
    "local.properties", "presage.properties", "secrets.properties",
    ".env", ".env.local", ".env.production",
}
MAX_FILE_BYTES = 32 * 1024 * 1024
MAX_APK_TOTAL_BYTES = 256 * 1024 * 1024


@dataclass(frozen=True)
class Finding:
    location: str
    rule: str


def _secret_assignment(data: bytes) -> bool:
    for match in SECRET_ASSIGNMENT.finditer(data):
        value = match.group(1).strip(b"\\\"' ")
        upper = value.upper()
        is_placeholder = (
            upper in PLACEHOLDERS
            or any(fragment in upper for fragment in PLACEHOLDER_FRAGMENTS)
            or (len(upper) >= 8 and set(upper) == {ord("X")})
        )
        if value and not is_placeholder:
            return True
    return False


def inspect_bytes(data: bytes, location: str, *, public_artifact: bool = False) -> list[Finding]:
    findings: list[Finding] = []
    if PRIVATE_KEY_MARKER.search(data):
        findings.append(Finding(location, "private_key_material"))
    if _secret_assignment(data):
        findings.append(Finding(location, "secret_assignment"))
    if public_artifact and PRESAGE_SYMBOL in data:
        # Denegar incluso si el valor parece vacío: un cliente público no debe
        # llevar el campo de credencial Presage embebido en su bytecode.
        findings.append(Finding(location, "client_presage_key_symbol"))
    return findings


def _repo_files(root: Path) -> Iterable[Path]:
    """Usa la lista de Git y excluye archivos ignorados como local.properties."""
    try:
        result = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-co", "--exclude-standard", "-z"],
            check=True, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
        )
        names = result.stdout.split(b"\0")
        for raw in names:
            if not raw:
                continue
            try:
                rel = raw.decode("utf-8")
            except UnicodeDecodeError:
                continue
            if any(part in SKIP_CONFIG_NAMES for part in Path(rel).parts):
                continue
            if Path(rel).suffix.lower() in {".jks", ".keystore"}:
                continue
            path = root / rel
            if path.is_file() and not path.is_symlink():
                yield path
        return
    except (OSError, subprocess.CalledProcessError):
        pass

    # Fallback para un scratch directory no versionado. No sigue symlinks.
    for current, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS and not (Path(current) / d).is_symlink()]
        for name in files:
            path = Path(current) / name
            if path.is_symlink() or name in SKIP_CONFIG_NAMES or path.suffix.lower() in {".jks", ".keystore"}:
                continue
            try:
                if path.stat().st_size <= MAX_FILE_BYTES:
                    yield path
            except OSError:
                continue


def scan_repo(root: str | Path) -> list[Finding]:
    base = Path(root).resolve()
    if not base.is_dir():
        return [Finding("<repo>", "repo_not_found")]
    findings: list[Finding] = []
    for path in _repo_files(base):
        try:
            if path.stat().st_size > MAX_FILE_BYTES:
                continue
            data = path.read_bytes()
        except OSError:
            findings.append(Finding(path.relative_to(base).as_posix(), "file_unreadable"))
            continue
        findings.extend(inspect_bytes(data, path.relative_to(base).as_posix()))
    return findings


def scan_artifact(path: str | Path) -> list[Finding]:
    artifact = Path(path)
    if not artifact.is_file():
        return [Finding(artifact.name, "artifact_not_found")]
    findings: list[Finding] = []
    if zipfile.is_zipfile(artifact):
        try:
            with zipfile.ZipFile(artifact) as archive:
                total = 0
                for info in archive.infolist():
                    if info.is_dir():
                        continue
                    if info.file_size > MAX_FILE_BYTES:
                        findings.append(Finding(info.filename, "artifact_entry_too_large_scan_incomplete"))
                        continue
                    total += info.file_size
                    if total > MAX_APK_TOTAL_BYTES:
                        findings.append(Finding(artifact.name, "artifact_scan_limit_exceeded"))
                        break
                    try:
                        data = archive.read(info)
                    except (OSError, RuntimeError, zipfile.BadZipFile):
                        findings.append(Finding(info.filename, "artifact_entry_unreadable"))
                        continue
                    findings.extend(inspect_bytes(data, info.filename, public_artifact=True))
        except (OSError, zipfile.BadZipFile):
            findings.append(Finding(artifact.name, "artifact_archive_invalid"))
    else:
        try:
            if artifact.stat().st_size > MAX_APK_TOTAL_BYTES:
                return [Finding(artifact.name, "artifact_too_large_scan_incomplete")]
            data = artifact.read_bytes()
        except OSError:
            return [Finding(artifact.name, "artifact_unreadable")]
        findings.extend(inspect_bytes(data, artifact.name, public_artifact=True))
    # Deduplicate when the same rule matches multiple code locations only if
    # both location and rule are exactly equal.
    return sorted(set(findings), key=lambda f: (f.location, f.rule))


def format_findings(findings: list[Finding], *, context: str) -> str:
    if not findings:
        return f"PASS: {context}; no configured secret marker found"
    lines = [f"BLOCK: {context}; {len(findings)} finding(s), values suppressed"]
    for finding in sorted(set(findings), key=lambda f: (f.location, f.rule)):
        lines.append(f"BLOCK: rule={finding.rule} location={finding.location} value=[SUPPRESSED]")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Barrera de publicación local, solo lectura")
    parser.add_argument("--repo", type=Path, help="Escanear archivos versionados/no ignorados del repo")
    parser.add_argument("--artifact", type=Path, help="Escanear APK/ZIP candidato a publicación")
    parser.add_argument("--public-candidate", action="store_true", help="Aplicar regla conservadora para APK pública")
    args = parser.parse_args()
    if not args.repo and not args.artifact:
        parser.error("indica --repo y/o --artifact")
    findings: list[Finding] = []
    context_parts = []
    if args.repo:
        findings.extend(scan_repo(args.repo))
        context_parts.append("repo")
    if args.artifact:
        findings.extend(scan_artifact(args.artifact))
        context_parts.append("artifact")
    context = "+".join(context_parts)
    print(format_findings(findings, context=context))
    if args.public_candidate and args.artifact and any(f.rule == "client_presage_key_symbol" for f in findings):
        return 1
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
