# Dominio: history-backend

## Overview

El dominio `history-backend` implementa las consultas de históricos de Niagara N4 para MX60.
`ChiHistoryHelper` (619 LOC) fue portado desde `SnlsHistoryHelper.java` con adaptaciones
chihuahua-específicas — es el único archivo backend con port-marker explícito que no involucra
reestructuración mayor (ANÁLOGO según delta §68.1). Soporte para rangos nombrados (`last24Hours`,
`last7Days`, `lastMonth`) y rangos custom por timestamp. No existe `HistoryData.java` equivalente
(reflow) — la serialización de históricos está inline en `ChiHistoryHelper`.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ChiHistoryHelper.java` | java-class (UX) | Consultas a Niagara HistoryService; serialización JSON; portado de SnlsHistoryHelper |

## Components / classes

| Clase | LOC | Propósito |
|-------|-----|-----------|
| `ChiHistoryHelper.java` | 619 | Ejecuta `BHistoryService` queries con rangos nombrados o timestamp; itera `BHistoryRecord`; serializa a JSON array; portado con adaptaciones (evidencia: `ChiHistoryHelper.java:1`) |

## Data Flow / Integration Points

```
BChiServlet GET /api/history?range=last24Hours
  → ChiHistoryHelper.queryHistory(historyOrd, range)
    → BHistoryService (Niagara native)
    → BHistoryQuery con AbsTime o RelTime
    → itera BHistoryRecord[]
    → serializa a JSON array

Frontend LiveHistoryBuffer.js
  → fetch /api/history periódicamente
  → mantiene ring-buffer de N puntos en memoria
```

- Port-marker: `"Ported from SnlsHistoryHelper.java with chihuahua-specific adaptations"` (evidencia: `ChiHistoryHelper.java:1`).
- `ChiHistoryHelper` 619 LOC vs `HistoryData.java` de reflow 663 LOC → |delta| = 6.6% ≤ 15% → ANÁLOGO (si mismo nombre-match hubiera) pero la diferencia es que el nombre cambió (Chi vs History).

## Notes & Cross-References

- **Bloque #68 §68.1**: `ChiHistoryHelper` referencia §68.1 como ANÁLOGO a `HistoryData.java` de reflow. La diferencia clave: sin `HistoryGhostSubscriber` analogue en MX60 (keepalive está dormant per bloque #69 audit).
- Port-check: `rg "Ported" ChiHistoryHelper.java` → `ChiHistoryHelper.java:1` confirma port-marker.
- LOC delta: 619 (MX60) vs 663 (reflow HistoryData) = 6.6% → ANÁLOGO threshold (15%).
- Claim sobre scheduling de queries y timeout: (inferred from mapping, not verified empirically).
