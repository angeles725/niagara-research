# Dominio: alarms-backend

## Overview

El dominio `alarms-backend` implementa las consultas de alarmas de Niagara N4 y la lógica de latch
custom para MX60. `ChiAlarmHelper` (2041 LOC) ejecuta BQL contra `BAlarmDatabase` y serializa
resultados. `ChiAlarmQueryHelper` encapsula la construcción de filtros BQL para las queries de alarmas.
A diferencia de reflow, MX60 NO usa `BReflowAlarmCommands` (BOX RPC) — la comunicación es REST pura
vía `BChiServlet`. El mecanismo de latch de alarmas (latch/unlatch + nota) está implementado como
slots en `BChiUp` y endpoints REST dedicados — también NUEVO vs reflow.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ChiAlarmHelper.java` | java-class (UX) | Consultas BQL a BAlarmDatabase; serialización JSON; 2041 LOC |
| `ChiAlarmQueryHelper.java` | java-class (UX) | Constructor de filtros BQL para queries de alarmas |

## Components / classes

| Clase | LOC | Propósito |
|-------|-----|-----------|
| `ChiAlarmHelper.java` | 2041 | Consulta `BAlarmDatabase` con BQL; soporta filtros por estado/prioridad/fuente; serializa records a JSON; implementa latch/unlatch vía slots de `BChiUp`; exportación CSV |
| `ChiAlarmQueryHelper.java` | — | Construye `BOrd` queries para `BAlarmDatabase`; encapsula lógica de filtrado por clase, prioridad y rango de fechas |

## Data Flow / Integration Points

```
BChiServlet GET /api/alarms
  → ChiAlarmQueryHelper.buildQuery(filters)
  → ChiAlarmHelper.queryAlarms(query, context)
    → BAlarmDatabase (Niagara native)
    → serializa AlarmRecord[] a JSON
    → response

BChiServlet POST /api/alarms/latch
  → ChiAlarmHelper.latchAlarm(ord, key, note)
    → resuelve BChiUp vía ORD
    → escribe slots latch + note en BChiUp
    → JSON { "ok": true }
```

- Sin `BReflowAlarmCommands` ni BOX RPC — todo es REST puro. El polling del frontend reemplaza el push WebSocket de reflow (evidencia empírica bloque #69: `$niagara.alarmSubscribe` NO existe en MX60).
- El latch de alarma escribe directamente slots de `BChiUp` (no una tabla de base de datos separada).

## Notes & Cross-References

- **Bloque #68 §68.4**: `ChiAlarmHelper` es ANÁLOGO a `AlarmData.java` de reflow pero con diferencias críticas: sin BOX methods, sin `BReflowAlarmCommands`, latch custom en slots BChiUp.
- `ChiAlarmHelper` 2041 LOC vs `AlarmData.java` (reflow) sin entrada directa en index → delta ANÁLOGO con expansión significativa.
- TODO (sprint-1): evaluar si la carga de `ChiAlarmHelper` se puede paralelizar (actualmente serializado en el thread HTTP).
