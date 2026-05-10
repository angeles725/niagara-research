# Dominio: schedules-frontend

## Overview

El dominio `schedules-frontend` implementa la vista y edición de schedules de bombas UP. `ScheduleView.js`
(550 LOC) es el único archivo del dominio. Presenta la lista de schedules via `GET /api/schedules` y
abre cada schedule en un iframe modal apuntando al editor nativo de Niagara `BWeeklySchedule`. La
auto-discovery de `BWeeklySchedule` para el iframe está (inferred from mapping, not verified empirically)
— el código construye una URL ORD y la abre en el iframe, pero la lógica exacta de resolución del
widget Niagara no es verificable desde static analysis.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ScheduleView.js` | iife-app (550 LOC) | Vista de schedules; fetch + renderizado; apertura de iframe modal de edición |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `ScheduleView.js` | 550 | iife-app | Fetch GET /api/schedules; renderiza tabla de schedules por UP; abre BWeeklySchedule en iframe modal para edición; expone MX60.ScheduleView |

## Data Flow / Integration Points

```
Router (#/schedules)
  → MX60.ScheduleView.mount(container)
    → fetch GET /api/schedules  → ChiScheduleHelper.getSchedules()
    → renderiza tabla: {start, stop, equipLabel, equipOrd}
    → click en schedule
      → construye URL: /ord?<ORD-to-BWeeklySchedule>#|view:name=WbWeeklyScheduleEditor
      → abre iframe modal
      → onMessage para detectar cierre y recargar

POST update (si aplica)
  → fetch POST /api/schedules/update  → BNumericSchedule.set() en backend
```

- Apertura en iframe: el editor nativo `BWeeklySchedule` de Niagara corre en el contexto Niagara, no en el SPA. El puente `postMessage` / `onMessage` es (inferred from mapping, not verified empirically).
- `ScheduleView` solo consume `MX60.ConfigManager` y `MX60.DashboardApp` — el dominio más independiente del frontend.

## Notes & Cross-References

- **ANÁLOGO vs reflow**: reflow tiene componentes Vue para schedule editing (`ScheduleForm.vue`, etc.). MX60 delega el editing al widget nativo de Niagara via iframe — decisión architectónica diferente.
- **REQ-14**: la comunicación postMessage con el iframe BWeeklySchedule es (inferred from mapping, not verified empirically).
- `ScheduleView.js` 550 LOC es auto-contenido — candidato a transplante aislado en sprint-2.
