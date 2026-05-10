# Dominio: schedules-backend

## Overview

El dominio `schedules-backend` implementa la consulta y actualización de schedules Niagara
(`BNumericSchedule`) asociados a equipos UP. `ChiScheduleHelper` (254 LOC) usa un patrón BQL para
descubrir todos los `BNumericSchedule` cuyo padre es un `BChiUp` y los serializa a JSON. La
actualización de schedules se delega a la API nativa de Niagara (`BNumericSchedule.set()`), con
`BChiServlet` como proxy HTTP. El dominio es relativamente pequeño y acotado.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ChiScheduleHelper.java` | java-class (UX) | Consulta BQL de BNumericSchedule bajo BChiUp; serialización JSON; 254 LOC |

## Components / classes

| Clase | LOC | Propósito |
|-------|-----|-----------|
| `ChiScheduleHelper.java` | 254 | Filtra schedules via BQL: solo los cuyo parent es `BChiUp`; serializa `start`, `stop`, `equipLabel`, `equipOrd`; helper stateless |

## Data Flow / Integration Points

```
BChiServlet GET /api/schedules
  → ChiScheduleHelper.getSchedules(context)
    → BQL: SELECT * FROM BNumericSchedule
    → filtra: parent instanceof BChiUp
    → serializa: {start, stop, parent:{name, displayName, slotPath}, equipLabel, equipOrd}
    → JSON array

BChiServlet POST /api/schedules/update
  → resuelve BNumericSchedule via ORD
  → BNumericSchedule.set(newValue) (Niagara API)
```

- Filtrado explícito `instanceof BChiUp`: solo los schedules de bombas UP, no de cárcamos ni dataloggers (evidencia: `ChiScheduleHelper.java:94-97`).
- El campo `equipLabel` cae back al slot name si `BChiUp.label` no tiene valor legible (evidencia: `ChiScheduleHelper.java:215`).

## Notes & Cross-References

- **ANÁLOGO vs reflow**: reflow no tiene `ChiScheduleHelper` directo, pero tiene un patrón equivalente de consulta BQL de schedules en `BReflowChartData` (diferente dominio). MX60 tiene la lógica inline en `ChiScheduleHelper`.
- El frontend `ScheduleView.js` consume `GET /api/schedules` y renderiza cada schedule en iframe modal con `BWeeklySchedule` auto-discover (inferred from mapping, not verified empirically).
- `ChiScheduleHelper.readUpPlanta()` (línea 234) es helper interno para obtener número de planta del UP padre.
