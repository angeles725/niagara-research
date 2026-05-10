# Dominio: equipment-frontend

## Overview

El dominio `equipment-frontend` implementa la vista de lista y resumen de equipos. `EquipmentData.js`
(406 LOC) es el store central de datos de equipo: hace fetch a `/api/equipment`, suscribe puntos via
`SubscriptionPool`, y mantiene el estado en memoria. `EquipmentCard.js` (645 LOC) renderiza tarjetas
de equipo por tipo. `EquipmentDetail.js` (191 LOC) es la vista de detalle base. `HomeMap.js` (889 LOC)
es el mapa interactivo de planta con popover de equipo. `EquipmentSnapshotStore.js` (339 LOC) cachea
snapshots para comparación histórica.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `EquipmentData.js` | iife-lib (406 LOC) | Store de datos; fetch + suscripción BajaScript; producer de MX60.EquipmentData |
| `HomeMap.js` | iife-app (889 LOC) | Mapa de planta interactivo; homepage principal de la SPA |
| `EquipmentCard.js` | iife-app (645 LOC) | Vista de tarjetas de equipo con status visual |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `EquipmentData.js` | 406 | iife-lib | Fetch /api/equipment; subscribe puntos via SubscriptionPool; expone MX60.EquipmentData y MX60.AlarmLatchStore como consumer |
| `EquipmentCard.js` | 645 | iife-app | Renderiza lista de EquipmentCards; expone MX60.EquipmentPage; usa StatusResolver, ThresholdStores |
| `EquipmentDetail.js` | 191 | iife-app | Vista base de detalle de equipo; expone MX60.DetailPage y MX60.EquipmentDetail |
| `EquipmentSnapshotStore.js` | 339 | iife-store | Cachea snapshots de EquipmentData para comparación; expone MX60.EquipmentSnapshotStore |
| `HomeMap.js` | 889 | iife-app | Mapa SVG interactivo de planta; popover con estado de equipo; expone MX60.HomePage |

## Data Flow / Integration Points

```
DashboardApp (mount #/ o #/home)
  → MX60.HomeMap.mount(container)
    → MX60.EquipmentData.fetch()  → GET /api/equipment
    → MX60.EquipmentData.subscribe(pointOrds)  → SubscriptionPool
    → renderiza SVG mapa con estado actual

Router (#/equipment)
  → MX60.EquipmentPage.mount(container)
    → MX60.EquipmentCard.renderAll(MX60.EquipmentData.getAll())
    → click en tarjeta → Router.navigate('#/equipment/up/123')
    → MX60.UpDetail.mount / MX60.CarcamoDetail.mount / MX60.DataloggerDetail.mount
```

- `EquipmentData` es el único productor del estado de equipo — todos los otros módulos lo consumen via `MX60.EquipmentData`.
- `EquipmentSnapshotStore` no se suscribe a puntos — solo cachea el último snapshot de `EquipmentData` para comparación en detalle.

## Notes & Cross-References

- **NUEVO vs reflow**: reflow no tiene un dominio de equipos físicos industriales. El pattern más cercano sería `BuildingAlarmSummary.vue` pero el scope es diferente.
- **Bloque #68 §68.3**: `EquipmentCard` consume los 5 threshold stores para colorizar cards por estado de umbral.
- `HomeMap.js` 889 LOC es el archivo más grande del dominio — candidato a split en transplante.
- `EquipmentSnapshotStore` usa `iife-store` kind; `subscriber_role: none` (no subscribe a puntos, solo cachea).
