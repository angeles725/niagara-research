# Dominio: baja-integration

## Overview

El dominio `baja-integration` implementa el bridge entre el frontend IIFE y el runtime BajaScript de
Niagara N4. `SubscriptionPool.js` (587 LOC) gestiona el pool de suscripciones a puntos Niagara vía
API BajaScript — registra subscribers, maneja teardown al destruir la página, y actúa como producer
del namespace `MX60.SubscriptionPool`. `WritePoint.js` (154 LOC) implementa escritura dual-path:
intenta primero `p.set()` (BajaScript nativo) y cae back a REST si la versión de Niagara no soporta
la API (inferred from mapping, not verified empirically). `SharedEnv.js` (45 LOC) es un ES module que
expone el entorno compartido entre módulos ES (Three.js vía importmap).

**ADVERTENCIA REQ-14**: este dominio contiene el mayor número de claims inferred (SubscriptionPool
lifecycle, WritePoint fallback behavior) — verificar empíricamente en sprint-1 antes de depender de
estos como ground-truth.

## Entry points

| Archivo | Tipo | Rol |
|---------|------|-----|
| `SubscriptionPool.js` | iife-lib (587 LOC) | Pool de suscripciones BajaScript; producer del namespace MX60.SubscriptionPool |
| `WritePoint.js` | iife-lib (154 LOC) | Escritura dual-path BajaScript/REST a puntos Niagara |
| `SharedEnv.js` | iife-lib (45 LOC, ES module) | Entorno compartido; importmap para Three.js |

## Components / classes

| Archivo | LOC | Rol |
|---------|-----|-----|
| `SubscriptionPool.js` | 587 | Registra subscribers via BajaScript `baja.load()` / `subscribe()` (inferred from mapping, not verified empirically); gestiona teardown en `destroy()`; expone `MX60.SubscriptionPool.subscribe`, `unsubscribe`, `destroy` |
| `WritePoint.js` | 154 | Intenta `p.set(value)` primero; si Niagara lanza error de versión, swichea a `fetch(POST /api/write)` para requests subsiguientes; log en console.warn/info (evidencia: `WritePoint.js:151-153`) |
| `SharedEnv.js` | 45 | ES module; `import * as THREE from 'three'` vía importmap; no expone `MX60.*`; cargado por UpDetail.js y CarcamoDetail.js (iife_pattern: iife-other) |

## Data Flow / Integration Points

```
EquipmentData.js (consumer)
  → MX60.SubscriptionPool.subscribe(pointOrd, callback)
    → BajaScript subscribe a punto Niagara (inferred from mapping, not verified empirically)
    → callback invocado en cada cambio de valor
    → MX60.EquipmentData actualiza UI

UpDetail.js / DataloggerDetail.js (consumer)
  → MX60.writePoint.set(pointOrd, value)
    → intenta BajaScript p.set()
    → fallback a POST /api/write si Niagara <4.9 (inferred from mapping, not verified empirically)
```

- `SubscriptionPool` es el único productor de subscripciones BajaScript en el frontend (evidencia: `SubscriptionPool.js:570`, `subscriber_role: "producer"`).
- WritePoint dual-path fallback detectado vía `console.warn` y `console.info` en el código fuente (evidencia: `WritePoint.js:151,153`).

## Notes & Cross-References

- **Bloque #68 §68.5**: `SubscriptionPool.js` → HEREDADO core (pool de subscripciones equivalente al pattern de reflow) + REESCRITO wrapper (API surface distinta; MX60 no usa `subscriberMixin` de Vue — es IIFE puro).
- **REQ-14**: todos los claims sobre el lifecycle de BajaScript subscribers son (inferred from mapping, not verified empirically). Verificar en sprint-1 con Niagara 4.x real.
- `SharedEnv.js` no expone `MX60.*` — no tiene globals_written. Es un módulo ES puro (deviation PR-2).
- `WritePoint.js` 154 LOC es el más corto del dominio; su simpleza es su fortaleza — dual-path está bien acotado.
