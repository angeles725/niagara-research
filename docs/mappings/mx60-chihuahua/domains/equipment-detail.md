# Dominio: equipment-detail

## Overview

El dominio `equipment-detail` contiene las vistas de detalle por tipo de equipo y sus helpers.
`UpDetail.js` (3841 LOC) es el archivo más grande de toda la SPA y el primary entry point del dominio
— implementa la vista de detalle de UP con modos MANUAL/SETPOINT/SCHEDULE, gráficos históricos
(`LiveHistoryBuffer`), override de modo y setpoints, y control de thresholds. `CarcamoDetail.js`
(1040 LOC) y `DataloggerDetail.js` (700 LOC) implementan vistas análogas para sus tipos.
`Configuracion.js` (535 LOC) implementa la página de configuración global de la planta.

**NOTA ES Modules** (deviation PR-2): `UpDetail.js`, `CarcamoDetail.js` y `SharedEnv.js` usan
`import * as THREE from 'three'` vía importmap — clasificados `iife_pattern: "iife-other"` por ser
ES modules con export, no IIFEs puros.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `UpDetail.js` | iife-app (3841 LOC) | Vista de detalle UP; 3 modos (MANUAL/SETPOINT/SCHEDULE); primary entry point del dominio |
| `CarcamoDetail.js` | iife-app (1040 LOC) | Vista de detalle Cárcamo |
| `DataloggerDetail.js` | iife-app (700 LOC) | Vista de detalle Datalogger |
| `Configuracion.js` | iife-app (535 LOC) | Página de configuración de planta |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `UpDetail.js` | 3841 | iife-app (ES module) | Detalle UP: modos MANUAL/SETPOINT/SCHEDULE; gráficos `LiveHistoryBuffer`; override via WritePoint; thresholds via UpThresholdStore; MX60.HistoryIndex + MX60.HistoryListCache internos |
| `CarcamoDetail.js` | 1040 | iife-app (ES module) | Detalle Cárcamo: lectura de slots CarcamoThresholdStore; override de salidas |
| `DataloggerDetail.js` | 700 | iife-app | Detalle Datalogger: historia de temperatura; DataloggerThresholdStore; writePoint para setpoints |
| `Configuracion.js` | 535 | iife-app | Página de configuración de planta: ModoOverrideStore; writePoint global |
| `EquipmentDetail.js` | 191 | iife-app | Página base de detalle; expone MX60.EquipmentDetail como base class IIFE |
| `LiveHistoryBuffer.js` | 212 | iife-lib | Ring-buffer de puntos históricos; expone MX60.LiveHistoryBuffer |
| `TimeRangePicker.js` | 161 | iife-lib | Selector de rango temporal; expone MX60.TimeRangePicker |

## Data Flow / Integration Points

```
Router (#/equipment/up/ORD)
  → MX60.UpDetail.mount(container, ord)
    → MX60.EquipmentData.get(ord)            → datos básicos
    → MX60.UpThresholdStore.get(ord)         → thresholds
    → MX60.LiveHistoryBuffer.init(ord)       → fetch /api/history
    → MX60.writePoint.set(ord, value)        → modo/setpoint (MANUAL/SETPOINT)
    → MX60.ModoOverrideStore / MX60.OutputOverrideStore
```

- `UpDetail.js` expone dos sub-namespaces internos: `MX60.HistoryIndex` y `MX60.HistoryListCache` (evidencia: `UpDetail.js:408,472`).
- Modos: MANUAL (override directo), SETPOINT (setpoint numérico), SCHEDULE (delegado a BWeeklySchedule Niagara).
- ES modules via importmap: `import * as THREE from 'three'` detectado en UpDetail + CarcamoDetail (deviation PR-2).

## Notes & Cross-References

- **Bloque #68 §68.3**: `UpDetail` consumer de `ModoOverrideStore`, `OutputOverrideStore`, `UpThresholdStore` — los 3 stores más críticos del dominio `threshold-stores`.
- **REQ-14**: claims sobre MANUAL/SETPOINT/SCHEDULE behavior son observados del código; la lógica de transición de modo en Niagara es (inferred from mapping, not verified empirically).
- `UpDetail.js` 3841 LOC → candidato prioritario a split en transplante (ver SDD `mx60-transplant-updetail`).
- `Configuracion.js` detectado empíricamente en PR-2 — no estaba en tasks.md original.
