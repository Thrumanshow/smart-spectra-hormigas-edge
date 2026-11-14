# Verificación del formato de trama LBH

**Fecha de verificación:** 2026-11-14
**Componente verificado:** `LbhEncoder` (codificador LBH local)
**Modalidad:** verificación offline, sin transmisión por red, sin ADB, sin Logcat.

---

## 1. Resumen

La aplicación **no transmite tramas LBH** por red. El codificador LBH existe
únicamente como puente local entre la salida del SDK de radar y la capa de
consenso de la app. Este documento describe el formato binario de la trama y
provee un vector canónico verificable independientemente, de modo que cualquier
revisor pueda confirmar que el codificador produce los bytes correctos sin
necesidad de acceso al dispositivo ni al código fuente privado.

## 2. Formato de la trama LBH

Una trama LBH ocupa **16 bytes** (32 caracteres hexadecimales) en big-endian.

| Offset | Bytes | Campo        | Tipo            | Unidad         |
|-------:|------:|--------------|-----------------|----------------|
| 0      | 1     | header       | `uint8`         | —              |
| 1      | 4     | pulse        | `float32` (BE)  | latidos/min    |
| 5      | 4     | breathing    | `float32` (BE)  | respiraciones/min |
| 9      | 7     | reserved     | `uint8[7]`      | — (zeros)      |

- El byte de cabecera es **`0xAE`** en todas las tramas válidas.
- `pulse` y `breathing` son floats IEEE 754 de 32 bits en big-endian.
- Los 7 bytes reservados se emiten siempre a cero.

## 3. Vector canónico

Para los valores de referencia

```
pulse    = 72.5  BPM
breathing = 16.0 brpm
```

el codificador produce exactamente:

```
ae429100004180000000000000000000
```

Desglose byte a byte:

```
ae                  header  (0xAE)
42 91 00 00         pulse   = 72.5    (float32 BE)
41 80 00 00         breath  = 16.0    (float32 BE)
00 00 00 00 00 00 00   reserved (7 bytes a cero)
```

## 4. Reproducción independiente

El siguiente script de Python no depende de ningún componente del proyecto.
Cualquier revisor puede ejecutarlo para confirmar la decodificación del vector
canónico:

```python
import struct

CANONICAL = "ae429100004180000000000000000000"

assert len(CANONICAL) == 32, "Se esperaban 32 caracteres hex"
b = bytes.fromhex(CANONICAL)
assert len(b) == 16, "Se esperaban 16 bytes"
assert b[0] == 0xAE, f"Cabecera 0x{b[0]:02x} != 0xAE"

pulse, breathing = struct.unpack(">ff", b[1:9])

print(f"vector    : {CANONICAL}")
print(f"header    : 0x{b[0]:02X}")
print(f"pulse     : {pulse}     (esperado 72.5)")
print(f"breathing : {breathing}  (esperado 16.0)")
print(f"reserved  : {b[9:].hex()} (7 bytes)")

assert pulse == 72.5
assert breathing == 16.0
print("OK: el vector canónico decodifica a pulse=72.5, breathing=16.0")
```

Salida esperada:

```
vector    : ae429100004180000000000000000000
header    : 0xAE
pulse     : 72.5     (esperado 72.5)
breathing : 16.0     (esperado 16.0)
reserved  : 00000000000000 (7 bytes)
OK: el vector canónico decodifica a pulse=72.5, breathing=16.0
```

Para codificar en sentido inverso (valores → bytes) y comprobar que se obtiene
el mismo vector:

```python
import struct

pulse = 72.5
breathing = 16.0

frame = bytes([0xAE]) + struct.pack(">ff", pulse, breathing) + bytes(7)
assert frame.hex() == "ae429100004180000000000000000000"
print("OK: la codificación produce el vector canónico")
```

## 5. Comportamiento defensivo del codificador

El codificador valida las entradas antes de emitir una trama. Una métrica se
rechaza (la función `encodeOrReject` retorna `null` sin emitir trama) cuando
cualquiera de los siguientes casos se cumple:

- Cualquier campo float es `NaN` o `±Infinity`.
- `pulse` está fuera del rango fisiológico admisible (por ejemplo `300 BPM`).
- `breathing` está fuera del rango admisible (por ejemplo `50 brpm`).
- `pulseConfidence` o `breathingConfidence` es `0`.

Esto significa que el codificador es la **última línea de defensa** antes de
emitir una trama: incluso si la capa de consenso deja pasar un sample
inválido, el codificador lo rechaza y no se produce trama. La separación de
responsabilidades es:

- **Capa de consenso** — lógica temporal: ventana mínima de emisión, gestión
  de huecos, marcador `stable`, agregación de muestras.
- **Codificador LBH** — validación de valores + serialización binaria.

## 6. Cómo verificar en una instalación

La trama se genera únicamente dentro del proceso de la app; no se transmite
por red. Para confirmar que el codificador instalado produce el vector
canónico, en un entorno con acceso al código fuente del codificador puede
ejecutarse un test unitario que invoque:

```
LbhEncoder.encode(LbhEncoder.Metrics(
    pulseBpm           = 72.5f,
    breathingRateBpm   = 16.0f,
    pulseConfidence    = 0.9f,
    breathingConfidence = 0.9f,
))
```

y verifique que el `ByteArray` retornado tiene exactamente 16 bytes y su
hexadecimal es `ae429100004180000000000000000000`.

Los campos de confianza (`pulseConfidence`, `breathingConfidence`) **no** se
serializan en la trama binaria de 16 bytes; sólo `pulse` y `breathing` se
incluyen. Su propósito es la validación previa a la codificación, no la
transmisión.

## 7. Limitaciones

- La trama LBH descrita aquí **no incluye** mecanismos de integridad
  (checksum, HMAC, nonce). La integridad y confidencialidad, si se requieren
  para un transporte concreto, son responsabilidad del canal de transporte,
  no del codificador.
- El campo reserved de 7 bytes está fijado a cero en la versión actual. Su
  significado podría cambiar en versiones futuras del formato sin afectar la
  decodificación de `pulse` y `breathing`.
