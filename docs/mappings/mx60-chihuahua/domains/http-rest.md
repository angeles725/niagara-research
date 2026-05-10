# Dominio: http-rest

## Overview

El dominio `http-rest` implementa la capa de transporte HTTP entre el frontend MX60 y el runtime
Niagara N4. `BChiServlet` extiende `BWebServlet` y expone 31 endpoints REST agrupados en ~8 recursos:
`/api/equipment`, `/api/history`, `/api/alarms`, `/api/schedules`, `/api/write`, `/api/thresholds`,
`/api/config` y variantes por tipo de equipo. `ChiServletDispatch` actúa como dispatcher de rutas,
con parte de su lógica portada verbatim desde `BSnlsServlet` (reflow reference module) con adaptaciones
chihuahua-específicas. Ver §68.2.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `BChiServlet.java` | java-class (BWebServlet, UX) | Servlet principal; 1743 LOC; 31 endpoints REST; despacha a helpers |
| `ChiServletDispatch.java` | java-class (UX) | Dispatcher de rutas; 594 LOC; partes portadas verbatim de BSnlsServlet |

## Components / classes

| Clase | LOC | Rol |
|-------|-----|-----|
| `BChiServlet.java` | 1743 | Entry point HTTP; authn CSRF; despacha por método+ruta a helpers especializados; adquiere lock de escritura de BChiDashboardService para POSTs |
| `ChiServletDispatch.java` | 594 | Router interno; contiene handlers de rutas complejas (equipos por tipo, thresholds); porta lógica de BSnlsServlet para manejo de MIME y encoding |

### Endpoints REST (31 totales)

| Grupo | Endpoints |
|-------|-----------|
| Equipment | GET /api/equipment, GET /api/equipment/up, GET /api/equipment/carcamo, GET /api/equipment/datalogger |
| History | GET /api/history, GET /api/history/range |
| Alarms | GET /api/alarms, POST /api/alarms/latch, POST /api/alarms/unlatch, POST /api/alarms/note |
| Schedules | GET /api/schedules, POST /api/schedules/update |
| Write | POST /api/write |
| Thresholds | GET /api/thresholds, POST /api/thresholds/up, POST /api/thresholds/carcamo, POST /api/thresholds/datalogger |
| Config | GET /api/config |
| Otros | ~12 endpoints adicionales de utility y admin |

## Data Flow / Integration Points

```
Browser (MX60 SPA)
  → GET /rc/index.html  (Niagara HTTP server)
  → fetch('/api/...')   → BChiServlet.doGet/doPost
                        → ChiServletDispatch.route()
                        → ChiEquipmentReader / ChiHistoryHelper / ChiAlarmHelper / ChiScheduleHelper
                        → JSON response via ChiJsonUtil
```

- CSRF probe: `DashboardApp.js` hace un `fetch('/api/config')` al startup para detectar soporte CSRF (evidencia: `DashboardApp.js:66-88`).
- Write lock: `BChiServlet.java:724-742` adquiere lock de `BChiDashboardService` antes de cualquier escritura.

## Notes & Cross-References

- **Bloque #68 §68.2**: `BChiServlet` es REESCRITO vs `BaseServlet`/`BSnlsServlet` de reflow — misma estructura HTTP pero 31 endpoints MX60-específicos vs los ~15 de reflow; port-marker en header del archivo.
- **ChiServletDispatch** → ANÁLOGO a lógica interna de `BaseServlet` de reflow (portado verbatim en secciones de MIME y encoding; evidencia: `ChiServletDispatch.java:3`).
- 31 endpoints = mayor cobertura funcional que reflow (~15); agrega thresholds, latch/unlatch de alarmas, write por tipo de equipo.
