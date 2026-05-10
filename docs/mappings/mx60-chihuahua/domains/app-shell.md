# Dominio: app-shell

## Overview

El dominio `app-shell` implementa el bootstrap y el routing de la SPA MX60. `DashboardApp` (309 LOC)
es el orquestador principal: inicializa la sesión CSRF, coordina el ciclo de vida de la app y expone
`MX60.DashboardApp` al namespace global. `Router` (170 LOC) implementa hash routing (`#/equipment`,
`#/alarms`, `#/schedules`) sin dependencias en librerías externas. `ConfigManager` (141 LOC) gestiona
la configuración de la app vía fetch al backend. `SharedEnv.js` (45 LOC) es un ES module (importmap)
que expone el entorno compartido. `ParticleAnimation.js` (169 LOC) es un efecto visual de fondo,
también IIFE. Todos los componentes de este dominio se cargan temprano (load_order_hint 1-3).

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `DashboardApp.js` | iife-app (309 LOC) | Orquestador principal; probe CSRF; inicializa Router y ConfigManager |
| `Router.js` | iife-app (170 LOC) | Hash router; maneja `hashchange`; monta/desmonta páginas |
| `ConfigManager.js` | iife-app (141 LOC) | Fetch de configuración backend; cachea config en memoria |

## Components / classes

| Archivo | LOC | Tipo | Propósito |
|---------|-----|------|-----------|
| `DashboardApp.js` | 309 | iife-app | Bootstrap SPA; CSRF probe `GET /api/config`; expone `MX60.DashboardApp`, `MX60.PageStub` |
| `Router.js` | 170 | iife-app | Hash routing; `window.addEventListener('hashchange')`; expone `MX60.Router` |
| `ConfigManager.js` | 141 | iife-app | Carga y cachea configuración desde backend; expone `MX60.ConfigManager` |
| `SharedEnv.js` | 45 | iife-lib (ES module) | ES module vía importmap; no expone `MX60.*` global; comparte entorno entre módulos ES |
| `ParticleAnimation.js` | 169 | iife-lib | Animación de partículas (fondo visual); expone `MX60.ParticleAnimation`; cargado desde `index.html` |

## Data Flow / Integration Points

```
index.html (iife-entry)
  → carga scripts en orden: ConfigManager → Router → DashboardApp → ParticleAnimation → ...
  → DashboardApp.init()
    → CSRF probe: fetch('/api/config')  → MX60._csrfProbeResult
    → Router.init()
    → window.dispatchEvent('app-ready')

Router.hashchange
  → desmonta página anterior
  → monta nueva: MX60.AlarmsPage / MX60.EquipmentDetail / MX60.ScheduleView / etc.
```

- Hash routing: URL pattern `#/route/subpath` (evidencia: `Router.js:load_order_hint=2`).
- `SharedEnv.js` usa `import * as THREE from 'three'` vía importmap — ES module, no IIFE; clasificado `iife_pattern: "iife-other"` per PR-2 deviation.
- `ParticleAnimation.js` fue detectado empíricamente en PR-2 (no asignado en tasks.md original).

## Notes & Cross-References

- **NUEVO vs reflow**: reflow usa Vue Router + Vue CLI bundle. MX60 tiene hash router custom sin framework.
- **Bloque #68 §68.5**: `SubscriptionPool.js` (dominio `baja-integration`) es inicializado por `DashboardApp` en el ciclo de arranque (inferred from mapping, not verified empirically).
- `index.html` tiene `kind: iife-entry` — es el único bootstrap HTML de toda la SPA.
- Deviation PR-2: `ParticleAnimation.js` y `SharedEnv.js` agregados por cobertura REQ-8.
