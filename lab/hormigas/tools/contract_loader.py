#!/usr/bin/env python3
"""Carga contratos declarativos LBH v1 sin ejecutar su contenido."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

SCHEMA_VERSION = "1.0"
PROTOCOL = "LBH-v2.0"
EXPECTED_CASTES = {
    "hormiga_de_relevo",
    "hormiga_de_seguridad",
    "hormiga_enfermera",
    "hormiga_exploradora",
    "hormiga_maestra",
}
REQUIRED = {
    "hormiga_de_relevo": {"funcion", "politica_descarte", "destino_local", "salida_permitida", "datos_prohibidos", "registro_payload"},
    "hormiga_de_seguridad": {"alcance", "restricciones", "modo_ejecucion", "salida_permitida", "politica_publicacion"},
    "hormiga_enfermera": {"funciones", "modo"},
    "hormiga_exploradora": {"objetivo", "entorno_operativo", "modo"},
    "hormiga_maestra": {"modo", "emision_feromona", "sincronizacion", "runtime_implementado"},
}
ALLOWED_FIELDS = {
    "schema_version", "casta", "protocolo", "funcion", "politica_descarte", "destino_local",
    "salida_permitida", "datos_prohibidos", "registro_payload", "alcance", "restricciones",
    "modo_ejecucion", "politica_publicacion", "funciones", "modo", "objetivo",
    "entorno_operativo", "emision_feromona", "sincronizacion", "runtime_implementado",
}


class ContractError(ValueError):
    """Error de estructura o política en un contrato."""


def validate_contract(data: Any, source: str = "<memory>") -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ContractError(f"{source}: la raíz debe ser un objeto JSON")
    extra = set(data) - ALLOWED_FIELDS
    if extra:
        raise ContractError(f"{source}: campos no permitidos: {', '.join(sorted(extra))}")
    caste = data.get("casta")
    if caste not in EXPECTED_CASTES:
        raise ContractError(f"{source}: casta ausente o no reconocida")
    missing = REQUIRED[caste] - set(data)
    if missing:
        raise ContractError(f"{source}: faltan campos requeridos: {', '.join(sorted(missing))}")
    if data.get("schema_version") != SCHEMA_VERSION:
        raise ContractError(f"{source}: schema_version no compatible")
    if data.get("protocolo") != PROTOCOL:
        raise ContractError(f"{source}: protocolo no compatible")

    for key in ("funcion", "politica_descarte", "destino_local", "alcance", "modo_ejecucion",
                "politica_publicacion", "modo", "objetivo", "entorno_operativo",
                "emision_feromona", "sincronizacion"):
        if key in data and not isinstance(data[key], str):
            raise ContractError(f"{source}: tipo inválido para {key}")
    for key in ("salida_permitida", "datos_prohibidos", "restricciones", "funciones"):
        if key in data and (not isinstance(data[key], list) or not all(isinstance(x, str) for x in data[key])):
            raise ContractError(f"{source}: tipo inválido para {key}")
    if "registro_payload" in data and not isinstance(data["registro_payload"], bool):
        raise ContractError(f"{source}: tipo inválido para registro_payload")
    if "runtime_implementado" in data and not isinstance(data["runtime_implementado"], bool):
        raise ContractError(f"{source}: tipo inválido para runtime_implementado")

    if caste == "hormiga_de_relevo":
        if data["destino_local"] != "diagnostico_local_no_sensible":
            raise ContractError(f"{source}: el relevo no puede escribir en Logcat ni en un destino no aprobado")
        if set(data["salida_permitida"]) - {"estado", "contadores"}:
            raise ContractError(f"{source}: salida del relevo limitada a estado y contadores")
        required_bans = {"biometria", "payload_lbh", "api_keys", "tokens", "secretos"}
        if not required_bans.issubset(set(data["datos_prohibidos"])):
            raise ContractError(f"{source}: la lista de datos prohibidos está incompleta")
        if data["registro_payload"] is not False:
            raise ContractError(f"{source}: registro de payload debe permanecer desactivado")
    if caste == "hormiga_de_seguridad":
        if data["modo_ejecucion"] != "solo_lectura":
            raise ContractError(f"{source}: la barrera debe ser solo lectura")
        if "prohibido_tocar_config_externa" not in data["restricciones"]:
            raise ContractError(f"{source}: falta la restricción de configuración externa")
        if data["politica_publicacion"] != "fail_closed_si_hay_indicio_de_clave":
            raise ContractError(f"{source}: política de publicación no segura")
    if caste == "hormiga_maestra" and data["runtime_implementado"] is not False:
        raise ContractError(f"{source}: el contrato no puede afirmar runtime no verificado")
    return data


def load_contracts(directory: str | Path) -> dict[str, dict[str, Any]]:
    root = Path(directory)
    if not root.is_dir():
        raise ContractError("No existe el directorio de contratos")
    schema_path = root.parent.parent / "schema" / "hormiga-contract-v1.schema.json"
    if not schema_path.is_file():
        raise ContractError("No se encuentra el esquema de contratos v1")
    try:
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        raise ContractError("El esquema JSON no se puede leer") from None
    if schema.get("$id") != "urn:hormigasais:contract:schema:1.0":
        raise ContractError("Identificador de esquema no compatible")

    loaded: dict[str, dict[str, Any]] = {}
    for path in sorted(root.glob("*.contract.json")):
        try:
            raw = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            raise ContractError(f"JSON inválido: {path.name}") from None
        contract = validate_contract(raw, path.name)
        caste = contract["casta"]
        if caste in loaded:
            raise ContractError(f"Casta duplicada: {caste}")
        loaded[caste] = contract
    if set(loaded) != EXPECTED_CASTES:
        missing = EXPECTED_CASTES - set(loaded)
        extra = set(loaded) - EXPECTED_CASTES
        details = []
        if missing:
            details.append("faltan " + ", ".join(sorted(missing)))
        if extra:
            details.append("sobran " + ", ".join(sorted(extra)))
        raise ContractError("Conjunto de castas incompleto: " + "; ".join(details))
    return loaded


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida contratos HormigasAIS v1 sin ejecutar agentes")
    parser.add_argument("--contracts", type=Path, default=Path(__file__).resolve().parents[1] / "contracts" / "v1")
    args = parser.parse_args()
    try:
        contracts = load_contracts(args.contracts)
    except ContractError as exc:
        print(f"BLOCK: contract_validation_failed ({exc})")
        return 1
    for caste in sorted(contracts):
        print(f"PASS: contract={caste} schema={SCHEMA_VERSION} protocol={PROTOCOL}")
    print("PASS: contratos válidos; ningún agente fue ejecutado")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
