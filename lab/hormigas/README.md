# HormigasAIS: contratos y controles locales

Esta carpeta incorpora contratos declarativos versionados, un lector/validador de esquema y una barrera de inspección en modo solo lectura. No implementa agentes, no inicia XOXO-BUS, no ejecuta scripts de ceremonia y no toca la configuración externa de Presage.

## Contenido

- `contracts/v1/`: copias versionadas de los cinco contratos del laboratorio.
- `schema/hormiga-contract-v1.schema.json`: JSON Schema Draft 2020-12, identificador `urn:hormigasais:contract:schema:1.0`.
- `tools/contract_loader.py`: valida versión, protocolo, castas y reglas obligatorias; carga JSON como datos, nunca como código.
- `tools/security_gate.py`: inspecciona archivos versionados/no ignorados o un APK; informa rutas y códigos de regla, nunca valores coincidentes. Omite expresamente `local.properties`, `.env`, `presage.properties` y almacenes de firma.
- `tests/`: pruebas negativas/positivas, incluida una clave canaria sintética.

## Verificación

Desde la raíz del repositorio:

```sh
python3 lab/hormigas/tools/contract_loader.py
python3 -m unittest discover -s lab/hormigas/tests -v
python3 lab/hormigas/tools/security_gate.py --repo .
python3 lab/hormigas/tools/security_gate.py --artifact android/app/build/outputs/apk/debug/app-debug.apk --public-candidate
```

La última orden debe bloquear el APK actual si el bytecode contiene el símbolo `PRESAGE_API_KEY`. Es una política deliberadamente conservadora: un APK cliente con un campo de credencial Presage no es candidato público, incluso si una compilación concreta lo dejó vacío. La herramienta nunca imprime el valor de una clave.

## Límites importantes

La prueba canaria invoca el gate en modo candidato público, comprueba el código de bloqueo y verifica que el valor de prueba no aparezca en stdout/stderr. Placeholders reconocibles en plantillas (`YOUR_*`, `PLACEHOLDER`, `REPLACE`, etc.) no cuentan como claves. No es una prueba matemática de que se detectará cualquier secreto desconocido, codificado u ofuscado. Una publicación real necesita además un build sin credenciales de cliente, revisión del APK y autenticación compatible con el proveedor. No se debe publicar el APK debug actual con una clave activa.

El campo `destino_local` de relevo se limita a diagnóstico no sensible. Solo se permiten estados y contadores; se prohíben biometría, tramas LBH, claves, tokens y secretos. No se usa Logcat como canal de carga.

La presencia de `emision_feromona: XOXO-BUS` en un contrato no demuestra que exista un bus. El contrato de la hormiga maestra declara explícitamente `runtime_implementado: false` hasta que un ejecutor y sus pruebas existan.
