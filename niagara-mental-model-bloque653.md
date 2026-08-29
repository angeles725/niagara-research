# Niagara N4 — chihuahua-source (CS5): the frontend is a strict-ES5 store/subscription SPA (window.MX60 IIFEs) with BajaScript-Fox-subscription-primary + 5s-REST-fallback live data, optimistic-write-with-rollback stores, and a server-authoritative RBAC model (CapabilityStore is decorative)

**Focus**: chihuahua-source · **Gap**: CS5 (frontend architecture) · **Session**: 2026-08-29 · **Block**: B653
**Sources** (`[CERT]` real source): `chihuahua-ux/src/rc/js/**` (`app/`, `lib/`, `ext/`) + `FRONTEND_ARCHITECTURE.md`. Behavioral corpus = [B151]-[B155]/[B163]-[B177] (REMIT).

**Scope**: the browser tier of the production dashboard — architecture, live-data flow, write model, and confirmation that RBAC is server-enforced ([B648]) not client-trusted.

---

## 653.1 Strict ES5, module-namespace IIFEs

`[CERT]` — grep for `let `/`const `/`=>` across the core app files returns nothing (only a `/* never let … */` comment); everything is `var`/`function` inside `(function(window){…})(window)` IIFEs attached to `window.MX60`. Reason stated `[CERT]` `DashboardApp.js:17` "ES5 STRICT" — the Niagara WebKit/JxBrowser runtime targets ES5. No bundler, no AMD/CommonJS — classic `<script>` loads. (Exception: `UpDetail.js`/`CarcamoDetail.js` are ES modules because they `import * as THREE from 'three'` via an importmap — Three.js is the one modern-module island; Chart.js is loaded UMD as `window.Chart`.)

This confirms/updates [B163]-[B177]'s "ES5 IIFE frontend" against current source: still ES5, now with a store/subscription architecture layered on.

---

## 653.2 Live-data flow: Fox-subscription primary, 5s REST fallback

`[CERT]` `EquipmentData.js` — a hybrid primary-source store: happy path = BajaScript Fox subscriptions via `SubscriptionPool.subscribeEquipment` (per-monitor); fallback = REST polling every 5 s (`cfg.pollMs.restFallbackMs`). An initial REST fetch always fires for fast first-paint, and subscription updates arriving before it settles are buffered in `_pendingSubUpdates` and replayed (CRIT-2 fix, `:51-56`). A merge-bug fix (`:293-310`) mirrors subscription data into both `equip.*` and `equip.summary.*` because consumers read `equip.summary.*` but the pool delivers a flat object. No SSE. The alarm badge polls `GET /api/alarms/summary` every 30 s independently (`DashboardApp.js:355`).

So the dashboard is genuinely live (Fox subscriptions) with a resilient polling fallback — a sound pattern for an N4 bajaux app ([B421]/[B36] BajaScript context, REMIT).

---

## 653.3 Write model: optimistic + rollback, server-authoritative RBAC

`[CERT]` `AlarmLatchStore.js:125-188` — optimistic write with rollback: it updates the store immediately, POSTs `/mx60/api/alarms/latch|unlatch`, and rolls back on failure. `seedFromEquipment` resets the store from the server DTO on every REST fetch (`:81`). `resetAll` uses a native Niagara action via BajaScript (`baja.Ord.make(ord).get().then(up => up.resetAlarmas())`, `:278+`) for transactional semantics, then `EquipmentData.refresh()` (cache-invalidation-after-write).

**RBAC is server-authoritative** `[CERT]` — `CapabilityStore.js` fetches `GET /api/user/capability` (deny-by-default until resolved) and is documented "DECORATIVE ONLY (ADR D6)"; `DashboardApp._applyWriteGuard(canWrite)` only shows/hides controls. The actual enforcement is the server 403 via `ChiRbacHelper` ([B648]). This is the CORRECT split — the frontend hides write UI for viewers as a convenience, but a viewer who forges a POST is still blocked server-side ([B648] §648.1). Confirms the [B648] finding from the client side.

CSRF: a probe fires at boot (`GET /api/csrf-probe`) but the full token flow is deferred (`DashboardApp.js:138`); the operative guard is the server's `X-Requested-With` check ([B648] §648.3).

---

## 653.4 Grade

The frontend is a well-structured ES5 store/subscription SPA: live Fox subscriptions with polling fallback, optimistic writes with rollback, cache-invalidation-after-write, and — importantly — RBAC that is decorative on the client and enforced on the server ([B648]). No `alert()`, detailed comments, no TODO/FIXME. Three.js/Chart.js are local-bundled (no CDN — the [B645] datacenter pattern, but here justified for an offline BMS). Production-quality client code.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | strict ES5 IIFE on window.MX60 (no let/const/arrow); WebKit/JxBrowser ES5 target | [CERT] | grep + DashboardApp.js:17 | ✅ grep+read |
| 2 | live = BajaScript Fox subscriptions primary + 5s REST fallback; initial REST + buffered replay; no SSE | [CERT] | EquipmentData.js:51-56,293-310 | ✅ read |
| 3 | AlarmLatchStore optimistic write + rollback; resetAll via native resetAlarmas action; refresh after write | [CERT] | AlarmLatchStore.js:125-188,278 | ✅ read |
| 4 | RBAC server-authoritative; CapabilityStore DECORATIVE (ADR D6); confirms B648 | [CERT] | CapabilityStore.js (ADR D6) + [B648] | ✅ read+cross-ref |
| 5 | CSRF probe at boot, token deferred; server X-Requested-With guard operative | [CERT] | DashboardApp.js:138 + [B648] | ✅ read |
| 6 | Three.js ES-module island + Chart.js UMD, local-bundled (no CDN) | [CERT] | index.html importmap + ext/ | ✅ read |

**Tally**: [CERT] ×6 · [INFER] ×0 · real-source block. ES5/subscription/RBAC-decorative claims token-checked (grep + read).

## Connections

- **[B648]** — server-authoritative RBAC (CapabilityStore is its decorative client half). **[B163]-[B177]** — the ES5 IIFE frontend, updated to the store/subscription architecture. **[B421]/[B36]** — bajaux/BajaScript client context. **[B645]** — local-bundled heavy JS (justified here).
- Forward: CS8 (verdict: client is production-quality, RBAC correctly split).

## Gaps uncovered

- None. The deferred CSRF token is an acknowledged item ([B648]/audit-2026-05-06), not a new gap.
