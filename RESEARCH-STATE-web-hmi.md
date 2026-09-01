# RESEARCH-STATE — focus: web-hmi (ACTIVE, 1/1 seeded — B724; datasheet-only hardware reference)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-31** porque el proyecto está desplegando
> el dashboard **DashboardPan** sobre un panel táctil físico **WEB-HMI10/CF** y no existía bloque destilado del
> hardware. B724 captura los hechos citables del panel para futuras sesiones y la skill planeada `build-n4-module`.
>
> **NO es terreno del corpus** — este es hardware OEM Honeywell, AUSENTE de FUENTE-1 (corpus) y FUENTE-3
> (decompilado). Verificado: `python3 tools/corpus-nav.py find "WEB-HMI"` → **"No matches."**. El ZERO es dato,
> registrado explícitamente en B724. La ÚNICA fuente legítima son los 3 datasheets oficiales (FUENTE 2): cada
> hecho hardware es [CERT-doc] con doc#+línea; cada derivación es [INFER].
>
> **Ángulo:** el panel es un CLIENTE Chromium HTML5 sobre Linux que muestra una PÁGINA WEB en una URL — no
> renderiza Px. El fit nativo del servlet `/dashboardpan/` del `cold-room-module` es [INFER] la conclusión
> operativa (§724.3). B724 = lineup + specs 10/CF + modelo display/programación + power/mount/connectivity +
> implicaciones responsive/kiosk a 1280×800 capacitivo.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 721
gaps_closed: 1
known_gaps: 3
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: web-hmi
status: active
bootstrapped_on: 2026-08-31
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B725)

## Coverage

- **Covered blocks**: this focus B724 (shared-global corpus counter = 721 distinct blocks)
- **Coverage metric**: 1 / 1 seeded gap closed (WH1 hardware distillation). Datasheet-only source is exhausted
  for the read-only angle; deeper facts need a live panel.
- **Last iteration**: 2026-08-31 — WH1 closed (B724). Focus stays *active* (not stopped): the 3 open gaps are
  answerable only against a live WEB-HMI10/CF unit (requires-execution), not from the datasheets.

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | WH1 distill the WEB-HMI hardware from the 3 datasheets — family lineup, WEB-HMI10/CF specs, display/programming model (Linux+Chromium/URL), power/mount/connectivity, responsive/kiosk implications | datasheet | closed (B724) |
| medium | WH1-G1 exact Chromium/engine version on SB78 firmware — gates which HTML5/CSS/JS the DashboardPan page may use | requires-execution (live panel: System Settings → System/Applications) | open |
| low | WH1-G2 kiosk/toolbar lockdown hardening specifics — how fully the operator UI locks vs the always-present tap-hold / Tap-Tap recovery paths | requires-execution / policy | open |
| low | WH1-G3 doc inconsistencies — SD "not supported" (31-00389) vs "maintenance only" (31-00390); WEB-HMI7/C USB "1 host port" (ordering) vs "No" (Table 2) | datasheet (recorded, non-blocking) | noted (B724 Open gaps) |

### Remittance (ya cubiertos / referencias, no son gaps)

- UI/servlet rendering stack (station WEB server, servlets, HTML) → [Block 9] UI Stack.
- Controller hardware that the panel browses → JACE-9000 [Block 657], JACE-8000 [Block 672]–[Block 683].
- The DashboardPan module internals (servlet at /dashboardpan/) → NOT a block; owned by the `cold-room-module` /
  `build-n4-module` work, referenced by name in B724 §724.3.

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 1 | 2026-08-31 | WH1 WEB-HMI hardware distillation (3 datasheets, read in full) | B724 | no · inline (3 doc-source datasheets, single hardware topic) | WH1-G1, WH1-G2 (requires-execution); WH1-G3 (doc inconsistency, noted) |

## Blocked / requires-execution gaps (each tagged with what it needs)

- WH1-G1 (requires-execution): the datasheets name *where* the Chromium version lives (System Settings → System
  Info; Applications) but not the *value*. Needs a live WEB-HMI10/CF to read it. Safe default meanwhile: target a
  conservative HTML5/ES5-friendly baseline for the DashboardPan page.
- WH1-G2 (requires-execution / policy): lockdown completeness is an operational posture, not a datasheet fact;
  the tap-hold-corner (5 s) and Tap-Tap paths to System Settings always exist by design (31-00456 L332-361).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 → the datasheet source is exhausted (all 3 read in full).
- **Open gaps — requires-execution**: 2 (WH1-G1, WH1-G2) — need a live panel.
- **Open gaps — blocked**: 0.
- Focus kept **active** (single deploy-target hardware; may reopen when a live panel is available or the
  DashboardPan deploy raises a rendering question).

## Dismissed file types

- none (scoped hardware focus; no census — sources are the 3 named datasheets only, FUENTE-1/FUENTE-3 = ZERO).
