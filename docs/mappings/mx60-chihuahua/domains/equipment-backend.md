# Dominio: equipment-backend

## Overview

El dominio `equipment-backend` define los tres tipos de equipo físico que modela MX60-Chihuahua en
Niagara N4: `BChiUp` (bombas/UP), `BChiCarcamo` (cárcamos) y `BChiDatalogger` (dataloggers de
temperatura). Cada tipo tiene su BComponent con slots tipados y su Monitor correspondiente
(`BChiUpMonitor`, `BChiCarcamoMonitor`, `BChiDataloggerMonitor`) que implementa la lógica de alarma
periódica. Este dominio es completamente NUEVO vs reflow-clean-177, que no modela equipos físicos
industriales de este tipo.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `BChiUp.java` | java-class (BComponent, RT) | Tipo principal de equipo; 37 slots; fuente primaria de datos operativos |
| `BChiCarcamo.java` | java-class (BComponent, RT) | Equipo cárcamo; 8 slots |
| `BChiDatalogger.java` | java-class (BComponent, RT) | Equipo datalogger; 9 slots |

## Components / classes

| Clase | LOC | Slots | Propósito |
|-------|-----|-------|-----------|
| `BChiUp.java` | — | 37 | Bomba UP: slots para modo (manual/auto/setpoint), temperaturas de entrada/salida, estado de alarma, threshold limits, latch flag, nota de operador |
| `BChiUpMonitor.java` | — | 0 | Monitor de alarmas BChiUp; implementa BWorker para evaluación periódica (inferred from mapping, not verified empirically) |
| `BChiCarcamo.java` | — | 8 | Cárcamo: slots para nivel, bombas, estado |
| `BChiCarcamoMonitor.java` | — | 0 | Monitor de alarmas BChiCarcamo (inferred from mapping, not verified empirically) |
| `BChiDatalogger.java` | — | 9 | Datalogger de temperatura: slots para puntos de medición, estado |
| `BChiDataloggerMonitor.java` | — | 0 | Monitor de alarmas BChiDatalogger (inferred from mapping, not verified empirically) |

## Data Flow / Integration Points

```
BChiDashboardService.controlTick()
  → BChiUpMonitor.evaluate()      → actualiza BChiUp.alarmState
  → BChiCarcamoMonitor.evaluate() → actualiza BChiCarcamo.alarmState
  → BChiDataloggerMonitor.evaluate()

BChiServlet /api/equipment  → ChiEquipmentReader.readAll()
                            → itera BChiUp / BChiCarcamo / BChiDatalogger via BQL
                            → serializa a JSON con ChiJsonUtil
```

- Los monitores implementan evaluación de alarmas basada en thresholds (inferred from mapping, not verified empirically).
- `BChiUp` tiene 37 slots — el mayor de los tres tipos; sus slots son el contrato de datos del frontend para UpDetail.js.

## Notes & Cross-References

- **NUEVO vs reflow**: reflow-clean-177 no tiene análogo de equipment-backend. El modelo de equipos físicos (UP/Cárcamo/Datalogger) es MX60-específico.
- **Bloque #68 §68.3**: los 5 threshold stores del frontend (`UpThresholdStore`, `CarcamoThresholdStore`, `DataloggerThresholdStore`, `ModoOverrideStore`, `OutputOverrideStore`) leen los thresholds definidos en estos BComponents.
- `BChiUp` 37 slots → dominio `equipment-detail` consume todos estos slots en `UpDetail.js` (3841 LOC).
- Los 3 Monitor classes tienen `slots: 0` en la lectura empírica — probablemente extienden BWorker sin slots propios (inferred from mapping, not verified empirically).
