# Dominio: equipment-reader

## Overview

El dominio `equipment-reader` implementa la capa de lectura de datos de equipo en el UX server.
`ChiEquipmentReader` ejecuta BQL sobre el árbol de componentes para enumerar los tres tipos de equipo
y serializa los resultados al formato JSON consumido por el frontend. `ChiThresholdHelper` extrae y
serializa los valores de threshold de los slots de cada equipo. Juntos forman la capa DTO Layer-1
(read) de la arquitectura UX.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `ChiEquipmentReader.java` | java-class (UX) | Layer-1 DTO: consulta BQL, itera equipos, serializa JSON |
| `ChiThresholdHelper.java` | java-class (UX) | Layer-2 DTO: lee slots de threshold de BChiUp/BChiCarcamo/BChiDatalogger |

## Components / classes

| Clase | LOC | Propósito |
|-------|-----|-----------|
| `ChiEquipmentReader.java` | — | Ejecuta `BOrd` queries sobre `BChiDashboardService`; itera `BChiUp`, `BChiCarcamo`, `BChiDatalogger`; delega serialización a `ChiJsonUtil`; NOT a BComponent (sin slots) |
| `ChiThresholdHelper.java` | — | Lee `BChiUp.threshold*` / `BChiCarcamo.threshold*` / `BChiDatalogger.threshold*` slots; serializa como JSON array; sin estado (stateless) |

## Data Flow / Integration Points

```
BChiServlet GET /api/equipment
  → ChiEquipmentReader.readAll(context)
    → BQL: SELECT * FROM BChiUp UNDER service
    → BQL: SELECT * FROM BChiCarcamo UNDER service
    → BQL: SELECT * FROM BChiDatalogger UNDER service
    → ChiJsonUtil.escapeJson() para cada slot
    → JSON array al PrintWriter

BChiServlet GET /api/thresholds
  → ChiThresholdHelper.readThresholds(up/carcamo/datalogger)
    → lee slots threshold directamente del BComponent
    → JSON response
```

- `ChiEquipmentReader` no extiende BComponent — es una clase utilitaria pura (Layer-1 DTO) sin ciclo de vida Niagara.
- `ChiThresholdHelper` también es stateless; no tiene caché — cada request lee los slots frescos.

## Notes & Cross-References

- **NUEVO vs reflow**: reflow no tiene un reader de equipos físicos; su único análogo sería `AlarmData.java` (BQL sobre alarmas), pero el patrón de equipos físicos es MX60-específico.
- **Bloque #68 §68.3**: los thresholds leídos por `ChiThresholdHelper` son los mismos valores que los 5 stores del frontend (`UpThresholdStore` etc.) almacenan localmente para comparación visual.
- TODO: verificar si `ChiEquipmentReader` cachea resultados entre requests o relanza BQL en cada GET.
