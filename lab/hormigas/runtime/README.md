# Runtime local determinista

Este paquete convierte la demostración temporal en módulos Python persistentes. `local_runtime.py` carga los contratos v1 como datos y orquesta cinco funciones locales: exploración por conteo, revisión de Git/JDK, escaneo de seguridad, filtrado de relevo y coordinación.

No ejecuta scripts ni código contenido en los contratos. No usa red, Logcat, APK, configuración local/externa, claves reales ni modelos de IA. El relevo permite únicamente estados y contadores enteros; rechaza campos de biometría, credenciales, tramas y cualquier campo no permitido. Los canarios de prueba son ficticios y sus valores no se registran.

## Ejecutar

Desde la raíz del repo:

```sh
python3 lab/hormigas/runtime/local_runtime.py --repo .
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s lab/hormigas/runtime/tests -v
```

`RUNTIME_DEMO=PASS` demuestra que estas cinco funciones locales se ejecutaron y pasaron las comprobaciones configuradas. No demuestra agentes autónomos, entrenamiento, comunicación invisible ni un transporte XOXO-BUS. El transporte de este prototipo son llamadas locales a funciones.
