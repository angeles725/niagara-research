# Dominio: alarms-frontend

## Overview

El dominio `alarms-frontend` es el dominio más grande del frontend MX60 (~3000 LOC total). Implementa
la consola de alarmas completa: polling REST, display en lista y tarjeta, latch/unlatch custom,
reconocimiento con nota, y barra de acciones masivas. `AlarmsManager.js` (326 LOC) es el orchestrador
de polling y estado de alarmas. `AlarmsPage.js` (824 LOC) es la página principal de la consola.
`AlarmCards.js` (368 LOC) y `AlarmDetailsTable.js` (386 LOC) son las dos vistas de datos.
El mecanismo de latch (enclavamiento de alarma con nota de operador) es exclusivo de MX60 — no existe
análogo en reflow-clean-177. Ver §68.4.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `AlarmsManager.js` | iife-lib (326 LOC) | Orquestador de polling y estado; producer de MX60.AlarmsManager |
| `AlarmsPage.js` | iife-app (824 LOC) | Página principal de consola de alarmas |
| `AlarmDetailPage.js` | iife-app (481 LOC) | Detalle de una alarma individual con historial |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `AlarmsManager.js` | 326 | iife-lib | Polling periódico a GET /api/alarms; mantiene estado en memoria; producer |
| `AlarmsPage.js` | 824 | iife-app | Vista principal: AlarmCards + BulkActionBar + AlarmsManager; filtros por tipo |
| `AlarmCards.js` | 368 | iife-lib | Renderiza tarjetas de alarma; consumer de AlarmDetailsTable y AlarmNotesModal |
| `AlarmDetailsTable.js` | 386 | iife-lib | Tabla de detalles de alarma con RelativeTime; consumer |
| `AlarmDetailPage.js` | 481 | iife-app | Página de detalle individual; consumer de AlarmsManager |
| `AlarmLatchStore.js` | 268 | iife-store | Store de estado latch por ORD; subscriber_role: none |
| `AlarmModalActions.js` | 240 | iife-lib | Acciones modales: latch/unlatch/nota; usa Popover + AlarmsManager |
| `AlarmNotesModal.js` | 251 | iife-lib | Modal de nota de alarma; usa Toast |
| `BulkActionBar.js` | 110 | iife-lib | Barra de acciones masivas (latch all, unlatch all); standalone |

## Data Flow / Integration Points

```
AlarmsPage.mount()
  → AlarmsManager.startPolling()      → GET /api/alarms (setInterval, inferred from mapping)
  → AlarmsManager.onData(records)
    → AlarmLatchStore.update(latches) → estado de latch local
    → StatusResolver.resolve(status)  → CSS class
    → AlarmCards.render(records)
    → BulkActionBar.update(count)

AlarmModalActions.latch(ord, key)
  → POST /api/alarms/latch            → ChiAlarmHelper.latchAlarm()
  → AlarmLatchStore.setLatch(ord)
  → Toast.show("Alarma enclavada", "success")
```

- Polling inline en `AlarmsManager` (inferred from mapping, not verified empirically) — reflow usaba WebSocket push. MX60 no tiene WebSocket para alarmas (evidencia empírica bloque #69).
- `AlarmLatchStore.subscriber_role = none` — no suscribe a puntos Niagara; solo trackea estado latch local.
- `AlarmsManager.subscriber_role = producer` — produce el estado de alarmas para los demás consumidores.

## Notes & Cross-References

- **Bloque #68 §68.4**: `AlarmModalActions` + `AlarmNotesModal` → ANÁLOGO a `AlarmNotes.vue` + `AlarmNotesModal.vue` de reflow. El mecanismo de latch/unlatch es NUEVO (reflow no tiene latch custom).
- **REQ-14**: el intervalo de polling de `AlarmsManager` es (inferred from mapping, not verified empirically).
- `BulkActionBar.js` 110 LOC — sin dependencias de MX60.* excepto escritura a DOM; candidato a standalone web component.
- `AlarmLatchStore` usa `iife-self` pattern — window.MX60.AlarmLatchStore asignado directamente.
