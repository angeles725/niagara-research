# B748 · Making our refrigeration modules interactive and low-cognitive-load — a ranked, buildable playbook, plus the modern connection options (browser flow view, Node-RED bridge)

> **Scope**: the operator's actual ask — take what Node-RED does well and what Niagara's Wire Sheet already
> is (B747), and turn it into concrete, ranked, buildable changes so OUR modules (ColdRoomPan / CompPan /
> DashboardPan) feel interactive, discoverable, and NOT overwhelming ("que no desborde y que no sea cansino").
> Every recommendation ties to a code-grounded block and states cost/risk. Then IC3: the modern connection
> options — a browser-native flow layer and a Niagara↔Node-RED bridge — and where a low-code interactive
> layer fits WITHOUT displacing the control engine. Foco: **interactive-composition** (IC2 + IC3). This is a
> DESIGN block: high [INFER] is expected; the FACTS it builds on are cited.
>
> **Sources**: FUENTE 1 — B747 (the comparison), B737/B744 (composition), B735 (SUMMARY pins/facets), B738
> (icons), B746 (palette templates), B745 (units), B729/B730 (lifecycle/idioms), B5 (ORD/BQL/tags/obix
> scheme), B24 (kitControl), B116 (Spyder compile/download). Node-RED + oBIX/MQTT integration = `[CERT-web]`
> (public project knowledge). Our modules' current state — B731/B742 (the audited backlog).

---

## 748.1 — The one-sentence diagnosis `[INFER, from B747]`

Niagara already gives us a Node-RED-class flow surface (typed draggable blocks, status-colored live pins,
routed wires, palette, persisted layout, undo — B747). What makes OUR modules feel "cansino" is not the
platform; it is six AUTHORING choices we control. Fix them in the block source and the palette and the SAME
Wire Sheet becomes the interactive, legible surface the operator wants — no new runtime, no rewrite.

## 748.2 — The ranked playbook (each = a buildable change) `[INFER, grounded per row]`

Ranked by **impact-on-clutter ÷ cost**. All are additive and deploy-safe (no slot retyping — B739).

| # | Change | What it fixes (the "desborde") | Cost / risk | Grounding |
|---|---|---|---|---|
| **1** | **Curate SUMMARY pins**: mark only real I/O (`runCmd`, `valveOut`, `coilTemp`, setpoints) `SUMMARY`; make interim state non-summary; `HIDDEN` on internal timer actions | The wire sheet shows ONLY summary slots (B747 §747.2). Today un-curated blocks dump every slot as a pin → a wall. This alone declutters the sheet | Trivial · flag-only, no data change | B735 §735.4; B747 |
| **2** | **Compose flat slots into child components**: `BEvaporatorUnit`'s 25 flat slots → `timing/outputs/hoa/freeze/alarms` child `BComponents` | The property sheet + Link picker collapse into a small tree; each concern drills in. This is the biggest single "no desborde" win | Medium · new frozen child types, link ORDs gain a level (`unit/freeze/…`); additive | B737 §B.3; B744 |
| **3** | **Put units/precision facets** on every temp/pressure/percent slot | The live pin value renders "‑18.0 °C" not "‑18.0"; the status-colored readout (B747) becomes legible at a glance — the Node-RED "status line" done right | Low · facet-only | B735 §735.2; B745; B747 |
| **4** | **Give each block a distinct icon/glyph** (`getIcon()` → SVG) — a snowflake for ColdRoom, a fan for the evaporator, a compressor glyph | Visual recognition on the sheet instead of reading titles; Node-RED's node-color/icon legibility | Low · one SVG resource per type | B738 |
| **5** | **Ship pre-wired palette assembly templates**: "1-evap room", "2-evap room + defrost", "compressor rack N stages" as nested `<p>` palette items with the right `hasDefrost`/flags baked in | Commissioning becomes **drag-one-thing** instead of drag-nest-flag-link; bakes in correct structure (avoids the `hasDefrost=false → never defrosts` trap) | Low · palette is a resource, not code | B746 §746.3; B731 |
| **6** | **Tag + relate** blocks (semantic tags: `room`, `evaporator`, `compressor`) and expose a BQL/NEQL view | Discoverability: "show me every evaporator" as a query instead of tree-walking; feeds the dashboard and any future flow view | Medium · tag dictionary + tagging pass | B5 (tags/BQL) |

**Sequencing**: 1 and 3 are same-day flag/facet passes with immediate visible payoff; 5 is a low-risk
resource add; 2 is the structural one (do it once, carefully, per B742's deploy-safe plan); 4 and 6 are
polish/discoverability. This slots directly into the existing consolidated backlog B742 as a "UX/legibility"
batch.

## 748.3 — Borrow the two Node-RED affordances Niagara lacks `[INFER · CERT-web Node-RED]`

Only two Node-RED strengths are genuinely absent (B747 §747.3); both are addable at the OUR-module level:

- **A packaged "watch the flow" operator view.** Node-RED's debug sidebar makes data motion visible. Niagara
  paints live values on pins but only in the engineering Wire Sheet (Workbench, Write permission). For
  operators, build a **read-only dashboard flow view** in the ux layer (bajaux/HTML5) that renders the room's
  blocks + live links as a simple animated diagram — we already ship `DashboardPan-ux`; this is a new view,
  not a new engine. Data comes from the same slots via the servlet/BQL the dashboard already uses.
- **A stage/commit boundary for interactive edits.** kitControl/Wire Sheet is edit-is-effect on the live
  engine (dangerous — a bad link is immediate, B737 §A.2). Node-RED's "Deploy" button and Spyder's
  compile→download (B116) both interpose a commit step. If we ever expose an interactive editor to
  operators, copy that: edit a staged copy, validate, then commit — never live-mutate the control engine
  from a touch panel.

## 748.4 — Modern connections: where a low-code interactive layer fits `[INFER · CERT-web]`

The operator asked about "nueva tecnología… nuevos métodos… conexiones". The pragmatic modern move is NOT to
rebuild Niagara's editor, but to BRIDGE the control engine to a low-code interactive layer over standard
protocols the install already speaks:

- **Niagara already exposes standard northbound protocols**: an **oBIX** server/driver (`BObixOrdScheme`,
  `obixDriver-rt`, B5) — REST/XML; **BACnet/IP**; and **MQTT** (the install carries an `mqtt` license
  feature and a `honLoRaMqtt` module, B1-3). `[CERT/CERT-web]`
- **Node-RED speaks all three** (community `node-red-contrib-oBIX`, BACnet, and core MQTT nodes).
  `[CERT-web]` So the interactive/fun layer the operator likes can literally BE Node-RED, running beside the
  JACE/Supervisor: Node-RED subscribes to room state over MQTT/oBIX, draws the flashy interactive flows,
  and (if ever needed) writes setpoints back through the SAME authenticated write path we already hardened
  for the 3D viewer (oBIX PUT / REST + Bearer). Control logic stays in Niagara (safe, watchdog-guarded);
  presentation/experimentation lives in the low-code layer.
- **Division of labor** (the recommendation): **Niagara = the control engine + system of record** (typed,
  type-checked links, transactional, watchdog-guarded); **the interactive layer (Node-RED or our bajaux
  dashboard) = presentation + operator interaction**, talking to Niagara over oBIX/MQTT/REST, never
  embedding control logic. This is safer than porting control into a low-code tool and gives exactly the
  "interactive, no desborda, no cansino" experience without risking the refrigeration control.

## 748.5 — What NOT to do `[INFER]`

- Do NOT move control logic into Node-RED or any low-code tool — it has no engine watchdog, no
  type-checked links, no transactional BOG; a refrigeration control loop belongs on the hardened engine
  (B737).
- Do NOT retype existing slots to "clean up" — it breaks the `.bog` and won't boot (B739, a real outage).
  Compose by ADDING child components (rec #2), never by retyping.
- Do NOT expose a live edit-is-effect editor to operators without a stage/commit boundary (§748.3).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | The clutter is authoring, not platform: SUMMARY-curation, composition, facets, icons, palette templates, tags each address it | [INFER] | B735/B737/B738/B745/B746/B5 via B747 |
| 2 | Wire sheet shows only SUMMARY slots → curating pins is the cheapest declutter | [CERT] | B747 §747.2 (`SlotBarGlyph.java:56`) |
| 3 | Composing 25 flat slots into child components collapses the sheet/property-sheet/picker | [CERT/INFER] | B737 §B.3 (fact); "biggest win" [INFER] |
| 4 | Live pin values render with facets + status color → units make them legible (Node-RED status-line done richer) | [CERT] | B747 §747.2 (`PropertyBarGlyph.java:35-46`) |
| 5 | Palette can ship pre-wired assembly templates → drag-one-thing commissioning | [CERT/INFER] | B746 §746.3 |
| 6 | Niagara exposes oBIX/BACnet/MQTT; Node-RED speaks all three → a bridged low-code layer is viable | [CERT/CERT-web] | B5 (obix scheme); B1-3 (mqtt feature); Node-RED public nodes |
| 7 | Recommended division: Niagara=control engine of record, low-code layer=presentation over oBIX/MQTT/REST | [INFER] | grounded in B737 (engine safety) + §748.4 |
| 8 | Anti-patterns: no control-in-low-code, no slot retyping, no live editor without stage/commit | [CERT/INFER] | B739 (retype outage, [CERT]); rest [INFER] |

**Tally**: 3 [CERT], 1 [CERT/CERT-web], 4 [INFER/CERT-INFER]. High [INFER] is expected (design block); every
FACT is cited. No unmarked claims.

## Connections
- **B747** (the comparison this acts on), **B737/B744** (composition), **B735/B745** (pins/facets/units),
  **B738** (icons), **B746** (palette templates), **B742** (the deploy-safe backlog this batch joins),
  **B731** (hasDefrost trap templates avoid), **B739** (why not to retype), **B5** (tags/BQL/obix),
  **B116** (Spyder compile/download = the stage/commit model), and the panccadia-3d-viewer control-auth work
  (the authenticated write path a low-code layer would reuse).

## Open gaps
- **B748-G1**: a worked bajaux "operator flow view" prototype (render room blocks + live links from the
  dashboard servlet) — an implementation task, not research.
- **B748-G2**: a concrete Node-RED↔Niagara PoC over MQTT/oBIX (subscribe room temps, draw an interactive
  flow) — requires-execution; would confirm the [CERT-web] node availability against our install's broker.
