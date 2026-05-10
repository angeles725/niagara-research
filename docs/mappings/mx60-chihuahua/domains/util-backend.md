# Dominio: util-backend

## Overview

El dominio `util-backend` es un helper stateless que provee serialización JSON segura para todos los
otros helpers UX. `ChiJsonUtil` (270 LOC) contiene un método `escapeJson` portado verbatim de
`SnlsJsonUtil` (evidencia: `ChiJsonUtil.java:header`) y métodos adicionales para serializar tipos
Niagara (`BString`, `BDouble`, `BBoolean`) a JSON sin dependencias en librerías externas. Al ser
puramente stateless y sin BComponent, es el utilitario de bajo riesgo más simple del backend.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ChiJsonUtil.java` | java-class (UX) | Serialización JSON stateless; portado de SnlsJsonUtil; 270 LOC |

## Components / classes

| Clase | LOC | Propósito |
|-------|-----|-----------|
| `ChiJsonUtil.java` | 270 | Métodos estáticos de escapeJson y serialización de tipos Niagara a JSON string; sin estado; sin BComponent |

## Data Flow / Integration Points

```
ChiEquipmentReader / ChiAlarmHelper / ChiHistoryHelper / ChiScheduleHelper
  → ChiJsonUtil.escapeJson(value)     → string JSON-safe
  → ChiJsonUtil.toJson(bString/bDouble/bBoolean) → JSON primitive
  → PrintWriter (HTTP response body)
```

- Todos los helpers UX dependen de `ChiJsonUtil` — es la única capa de serialización; sin librerías Gson/Jackson (Niagara RT no incluye estas librerías).
- Port-marker: `"Patterns ported from:"` + `"escapeJson — ported verbatim from SnlsJsonUtil.escapeJson"` (evidencia: `ChiJsonUtil.java:header`).

## Notes & Cross-References

- **Delta vs reflow**: `ChiJsonUtil` → HEREDADO de `SnlsJsonUtil` (port-marker presente; LOC 270 vs reflow sin entrada directa — el util era inline en BaseServlet de reflow).
- Sin efectos secundarios: pure function para todos sus métodos. Candidato a test unitario fácil.
- TODO: comparar si los métodos de ChiJsonUtil cubren todos los tipos Niagara necesarios para los nuevos endpoints de threshold.
