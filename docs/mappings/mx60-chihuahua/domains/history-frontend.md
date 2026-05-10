# Dominio: history-frontend

## Overview

El dominio `history-frontend` implementa el buffer de históricos y el selector de rango temporal para
las vistas de detalle de equipo. `LiveHistoryBuffer.js` (212 LOC) mantiene un ring-buffer de N puntos
históricos en memoria y hace fetch periódico a `/api/history` para mantenerlo actualizado. `TimeRangePicker.js`
(161 LOC) es el control de selección de rango temporal (preset o custom). Ambos viven como iife-lib en
el dominio `equipment-detail` en el index.json — se documenta aquí como dominio conceptual separado
para claridad arquitectónica.

**FALTA vs reflow**: reflow-clean-177 tiene `historyCache.js` (516 LOC, bloque #70 §70.3) con un
patrón de caché más sofisticado con separación module-level vs Vuex. MX60 tiene solo el ring-buffer
simple. Esta asimetría es una decisión de diseño intencional para MX60 (menor complejidad operacional).

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `LiveHistoryBuffer.js` | iife-lib (212 LOC) | Ring-buffer de históricos; fetch periódico; expone MX60.LiveHistoryBuffer |
| `TimeRangePicker.js` | iife-lib (161 LOC) | Selector de rango temporal; usa MX60.util.Dropdown |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `LiveHistoryBuffer.js` | 212 | iife-lib | Mantiene ring-buffer de últimos N registros históricos; fetch a GET /api/history; expone `init`, `getBuffer`, `destroy` (inferred from mapping, not verified empirically) |
| `TimeRangePicker.js` | 161 | iife-lib | Dropdown de rangos: last1h/last24h/last7d/custom; emite evento de cambio de rango; usa MX60.util.Dropdown |

## Data Flow / Integration Points

```
UpDetail.js (consumer)
  → MX60.LiveHistoryBuffer.init(ord, range)
    → fetch GET /api/history?ord=&range=   → ChiHistoryHelper.queryHistory()
    → mantiene ring-buffer en memoria
    → expone getBuffer() para renderizado de chart

TimeRangePicker.js
  → MX60.util.Dropdown.create(target, ranges)
  → onSelect(range) → callback a UpDetail para reinit LiveHistoryBuffer
```

- Ring-buffer size es configurable (inferred from mapping, not verified empirically) — el parámetro exacto no es visible en static analysis.
- `TimeRangePicker` usa `MX60.util.Dropdown` — única dependencia cross-dominio de history-frontend.

## Notes & Cross-References

- **FALTA vs historyCache.js reflow**: reflow tiene `historyCache.js` con separación xa-cache + Sa-service + Ia-builder (bloque #70 §70.3). MX60 tiene el ring-buffer simple — la falta es intencional (MX60 es más simple).
- **Bloque #68 §68.1**: `LiveHistoryBuffer` consume `ChiHistoryHelper` via REST. Sin HistoryGhostSubscriber analogue.
- `TimeRangePicker` 161 LOC — componente pequeño reutilizable; también usado por `AlarmsPage.js`.
