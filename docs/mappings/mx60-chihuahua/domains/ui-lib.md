# Dominio: ui-lib

## Overview

El dominio `ui-lib` agrupa los componentes de interfaz reutilizables sin estado de dominio de negocio.
`Toast` y `Confirm` son modales ligeros. `StatusResolver` traduce el estado de alarma a clases CSS y
etiquetas. `Dropdown` y `Popover` son componentes de interacción sin framework. `RelativeTime` formatea
tiempos relativos. `CsvExport` genera CSVs en el cliente. Todos usan `iife-self` pattern (IIFE sin
argumentos que escribe a `window.MX60.*`).

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `Toast.js` | iife-lib (117 LOC) | Notificaciones toast; `MX60.Toast.show(msg, type)` |
| `Confirm.js` | iife-lib (116 LOC) | Diálogo de confirmación; `MX60.Confirm.show(opts)` |
| `StatusResolver.js` | iife-lib (48 LOC) | Traduce estado de alarma a CSS class + label |

## Components / classes

| Archivo | LOC | Propósito |
|---------|-----|-----------|
| `Toast.js` | 117 | Toast notifications; escucha `window.MX60.Toast = { show }` |
| `Confirm.js` | 116 | Modal de confirmación Promise-based; expone `MX60.Confirm.show(msg, onConfirm)` |
| `StatusResolver.js` | 48 | Resolver stateless de status a {class, label}; lee `MX60.AlarmLatchStore` para estado latch |
| `Dropdown.js` | 223 | Dropdown accesible sin framework; expone `MX60.util.Dropdown` |
| `Popover.js` | 151 | Popover posicionado; expone `MX60.util.Popover`; usado por `AlarmModalActions` y `HomeMap` |
| `RelativeTime.js` | 73 | Formatea tiempos relativos ("hace 3 min"); expone `MX60.util.RelativeTime` |
| `CsvExport.js` | 92 | Genera y descarga CSV desde array de objetos; expone `MX60.util.CsvExport` |

## Data Flow / Integration Points

```
Cualquier módulo IIFE
  → MX60.Toast.show("Guardado", "success")  → Toast DOM injection

AlarmsPage.js (consumer)
  → MX60.util.CsvExport.download(rows, filename)

AlarmDetailsTable.js (consumer)
  → MX60.util.RelativeTime.format(timestamp)

HomeMap.js / AlarmModalActions.js
  → MX60.util.Popover.create(target, content)
```

- `Dropdown.js`, `Popover.js`, `RelativeTime.js`, `CsvExport.js` están bajo `rc/js/util/` — uso de `iife-lib` kind por ser reutilizables cross-dominio.
- `StatusResolver` lee `MX60.AlarmLatchStore` (dominio `alarms-frontend`) para verificar estado latch — única dependencia cross-dominio de ui-lib.

## Notes & Cross-References

- **NUEVO vs reflow**: reflow usa componentes Vue (`Toast.vue`, `ConfirmModal.vue`). MX60 los reimplementó como IIFEs sin framework. Clasificado NUEVO en delta.
- `iife-self` pattern: todos en este dominio usan IIFE sin argumentos `(function() { ... }())` para evitar poluir el scope global más allá de `window.MX60.*`.
- TODO: en transplante Pinia, estos componentes se pueden mantener como IIFEs o convertir a composables Vue 3 — decisión pendiente sprint-2.
