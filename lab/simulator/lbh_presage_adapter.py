"""Adaptador de métricas normalizadas a trama LBH experimental de 16 bytes.

No accede a la cámara ni importa el SDK SmartSpectra. La app que integra
Presage debe entregar las métricas usando el contrato documentado abajo.
"""

from __future__ import annotations

import math
import struct
from dataclasses import dataclass

FRAME_SIZE = 16
HEADER = 0xAE


class InvalidSignal(ValueError):
    """La señal falta, está fuera de rango o no tiene confianza suficiente."""


@dataclass(frozen=True)
class PresageMetrics:
    # Valores normalizados por la capa de integración con SmartSpectra.
    pulse_bpm: float
    breathing_rate_bpm: float
    pulse_confidence: float
    breathing_confidence: float


def _validate_metric(name: str, value: float, low: float, high: float) -> float:
    value = float(value)
    if not math.isfinite(value):
        raise InvalidSignal(f"{name}: debe ser un número finito")
    if not low <= value <= high:
        raise InvalidSignal(f"{name}: fuera del rango documentado [{low}, {high}]")
    return value


def _validate_confidence(name: str, confidence: float) -> None:
    confidence = float(confidence)
    if not math.isfinite(confidence) or confidence <= 0:
        raise InvalidSignal(f"{name}: confianza no válida; no se emite trama")


def encode_lbh(metrics: PresageMetrics) -> bytes:
    """Codifica pulso y respiración validados en el formato experimental.

    Layout:
      byte 0:      header 0xAE
      bytes 1..4:  pulso, IEEE-754 float32 big-endian
      bytes 5..8:  respiración, IEEE-754 float32 big-endian
      bytes 9..15: reservados, cero

    Ambas métricas son obligatorias en esta versión del formato. No sustituir
    una señal ausente por datos simulados.
    """
    pulse = _validate_metric("pulse_bpm", metrics.pulse_bpm, 40, 110)
    breathing = _validate_metric(
        "breathing_rate_bpm", metrics.breathing_rate_bpm, 5, 40
    )
    _validate_confidence("pulse_confidence", metrics.pulse_confidence)
    _validate_confidence("breathing_confidence", metrics.breathing_confidence)

    frame = bytearray(FRAME_SIZE)
    frame[0] = HEADER
    struct.pack_into(">f", frame, 1, pulse)
    struct.pack_into(">f", frame, 5, breathing)
    return bytes(frame)


def encode_or_reject(metrics: PresageMetrics) -> bytes | None:
    """Devuelve la trama válida; None significa que aún no debe emitirse."""
    try:
        return encode_lbh(metrics)
    except (InvalidSignal, TypeError, struct.error):
        return None
