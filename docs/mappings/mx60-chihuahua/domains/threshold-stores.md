# Dominio: threshold-stores

## Overview

El dominio `threshold-stores` es completamente NUEVO vs reflow-clean-177. Agrupa los 5 stores de
estado de thresholds y overrides de MX60: `UpThresholdStore`, `CarcamoThresholdStore`,
`DataloggerThresholdStore` (thresholds de alarma por tipo de equipo), `ModoOverrideStore` (override
del modo de operación), y `OutputOverrideStore` (override de salida). Todos usan `iife-self` pattern
y escriben a `window.MX60.<StoreName>`. Son los consumers primarios de los slots de threshold de los
BComponents RT. Ver §68.3.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `UpThresholdStore.js` | iife-store (198 LOC) | Store de thresholds de UP; producer de MX60.UpThresholdStore |
| `CarcamoThresholdStore.js` | iife-store (216 LOC) | Store de thresholds de Cárcamo |
| `DataloggerThresholdStore.js` | iife-store (205 LOC) | Store de thresholds de Datalogger |
| `ModoOverrideStore.js` | iife-store (65 LOC) | Override de modo de operación |
| `OutputOverrideStore.js` | iife-store (93 LOC) | Override de salida de equipo |

## Components / classes

| Archivo | LOC | Propósito |
|---------|-----|-----------|
| `UpThresholdStore.js` | 198 | Lee thresholds de `/api/thresholds/up`; cachea en memoria; expone `get(ord)`, `set(ord, key, val)` (inferred from mapping, not verified empirically) |
| `CarcamoThresholdStore.js` | 216 | Análogo para cárcamos; 216 LOC (el más grande — más thresholds de tipo cárcamo) |
| `DataloggerThresholdStore.js` | 205 | Análogo para dataloggers; incluye thresholds de temperatura |
| `ModoOverrideStore.js` | 65 | Trackea el modo de operación override (MANUAL/AUTO/SETPOINT) por ORD de UP |
| `OutputOverrideStore.js` | 93 | Trackea el override de salida (encendido/apagado forzado) por ORD |

## Data Flow / Integration Points

```
EquipmentData.js / EquipmentCard.js (consumer)
  → MX60.UpThresholdStore.get(ord)          → thresholds para colorización visual
  → MX60.CarcamoThresholdStore.get(ord)
  → MX60.DataloggerThresholdStore.get(ord)

UpDetail.js (consumer)
  → MX60.ModoOverrideStore.get(ord)         → modo actual (MANUAL/AUTO/SETPOINT)
  → MX60.OutputOverrideStore.get(ord)       → estado de override de salida
  → writePoint.set() para actualizar override en Niagara
```

- Los 5 stores usan `iife-self` pattern — IIFE sin argumentos que asigna directamente a `window.MX60.*`.
- `ModoOverrideStore` (65 LOC) y `OutputOverrideStore` (93 LOC) son los más pequeños — tracks simples de un único valor por ORD (inferred from mapping, not verified empirically).
- `UpThresholdStore.subscriber_role = none` — no subscribe a BajaScript; hace fetch REST de thresholds.

## Notes & Cross-References

- **Bloque #68 §68.3**: los 5 threshold stores son la implementación frontend del sistema de thresholds custom de MX60. NUEVO vs reflow — reflow no tiene thresholds de alarma custom de este tipo.
- **NUEVO vs reflow**: ningún store de reflow-clean-177 tiene análogo funcional. Los threshold stores son una feature exclusiva MX60 para Honeywell Chihuahua.
- TODO: en transplante Pinia, cada store se convierte a `defineStore` separado — mapping 1:1 viable.
- `CarcamoThresholdStore` 216 LOC > `UpThresholdStore` 198 LOC — los cárcamos tienen más parámetros de threshold que los UPs.
