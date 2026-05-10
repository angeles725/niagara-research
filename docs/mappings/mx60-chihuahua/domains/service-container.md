# Dominio: service-container

## Overview

El dominio `service-container` es el núcleo de la jerarquía de componentes RT de MX60-Chihuahua.
`BChiDashboardService` actúa como servicio raíz Niagara (BComponent) que aloja la jerarquía de plantas
y equipos. Debajo de él viven instancias de `BPlanta`, y dentro de cada planta se anidan los tres tipos
de equipo (`BChiUp`, `BChiCarcamo`, `BChiDatalogger`). Esta jerarquía de 4 niveles es el modelo de
datos primario que el backend UX consulta mediante BQL y que el frontend consume vía REST.

Jerarquía (4 niveles):
```
BChiDashboardService
  └─ BPlanta (1..N por planta física)
       ├─ BChiUp (bombas de agua caliente/fría)
       ├─ BChiCarcamo (cárcamos)
       └─ BChiDatalogger (dataloggers)
```

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `BChiDashboardService.java` | java-class (BComponent, RT) | Raíz del servicio; expone `controlTick` y bloqueo de escritura concurrente |
| `BPlanta.java` | java-class (BComponent, RT) | Nodo de planta; contiene referencias a equipos hijos |

## Components / classes

| Clase | LOC | Slots | Propósito |
|-------|-----|-------|-----------|
| `BChiDashboardService` | — | 1 | Servicio RT raíz; orquesta ciclo `controlTick` ~10 s; provee lock de escritura por ORD para evitar race conditions en escrituras concurrentes desde BChiServlet |
| `BPlanta` | — | 2 | Nodo planta; agrupa equipos por planta física; el slot `name` es la clave de filtrado en BQL |

## Data Flow / Integration Points

```
BChiDashboardService.controlTick()  →  itera BChiUp/BChiCarcamo/BChiDatalogger
                                    →  actualiza monitores de alarma vía BChiUpMonitor etc.

BChiServlet (UX)  →  resuelve ORD al BChiDashboardService
                  →  adquiere lock por equipo antes de write
                  →  libera lock en finally block
```

- `controlTick` execución ~10 s (inferred from mapping, not verified empirically): activa los monitores de alarma de cada equipo en cada ciclo.
- `BChiServlet` referencia directa `BChiDashboardService` en `/api/write` para adquirir lock (evidencia: `BChiServlet.java:739-742`).

## Notes & Cross-References

- **Bloque #68 §68.2**: `BChiDashboardService` es el punto de resolución de ORD que usa `BChiServlet`; lock de escritura por ORD mitigando race conditions es NUEVO vs reflow (reflow no tiene escritura directa vía REST de puntos Niagara).
- **Delta vs reflow**: `BChiDashboardService` → NUEVO (no existe análogo en reflow-clean-177 que expose un servicio raíz con write-lock).
- `BPlanta` → NUEVO (reflow no tiene jerarquía explícita de plantas).
- Claim `controlTick` ~10 s: (inferred from mapping, not verified empirically).
- TODO: verificar empíricamente el intervalo real de `controlTick` al deployar en MX60.
