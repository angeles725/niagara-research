# RESEARCH-STATE — focus: harbor-greenmax-lighting (B841–B845)

> Multi-focus corpus (METHODOLOGY §16). Focus **bootstrapped 2026-09-08** for the **Distech-MERIDA / HARBOR**
> job: redesign the GreenMAX lighting control so 16 panels (~330 circuits) run off **4 master schedules** with
> an **HOA-style per-panel + per-circuit selector** and a **force-ON override**, plus assess an **HTML+CSS+JS
> dashboard**. Site = the **HM_Central Distech EC-Net JACE-8000 (4.3.58.18)** of B838, with the **HM_BMS
> Supervisor** (a Windows Workstation EC-Net4 install, Host ID `Win-D5C2-…`, Java 8, 37,050 components) hosting
> the schedules.
>
> **Evidence base:** FUENTE 1 = the two real station backups under `clients/distech-merida-harbor/` read with
> `tools/bog-nav.py` (all `[CERT-live]`), plus the SEJOFA proposal PDF. Framework claims verified against the
> existing corpus (B9 UI stack) and decompiled modules. Design claims are explicitly `[INFER]`.
> SECRETS DISCIPLINE: Host ID / daemonspawn tokens cited as structure only, never full secret values.
>
> **Outline (5 blocks):**
> - **B841** — station map + the current per-circuit schedule→writable→BACnet chain (`[CERT-live]` floor).
> - **B842** — the unified control model (4 masters + two-level selector + HOA override), route-independent.
> - **B843** — Route A: a custom N4 module (BGreenMaxPanel/BGreenMaxCircuit + LightingResolver).
> - **B844** — Route B: kitControl-only (BBooleanSelect + BNumericSwitch, replicated ~330×).
> - **B845** — HTML+CSS+JS dashboard feasibility (Px vs BWebServlet+rc/ SPA vs oBIX; the 4.3 servlet constraint).
> - **B846** — the points-matrix extraction (`tools/greenmax-matrix.py`): two panel generations, write-priority correction, 323/349 circuits described from the station. Closes B841-G1.
> - **B847** — Route B wiresheet: the verified `kitControl:BBooleanSelect` 1-circuit template (2-stage mux → existing `HorR{k}.in10`), no module.
> - **B848** — engagement context (DOCUMENT §20): what/who/how — SEJOFA vs client responsibilities, remote execution, the 13-account access model.
> - **B851** — the VERIFIED control chain + LIVE Route B pilot on GM02 circuit 1 (build + cutover, no flicker). Closes the gate (Numeric→Enum direct), corrects the model to HOA-per-horario + 5 masters, and confirms B841's supervisor-side thesis. Also fixes `bog-nav --to/--from` handle matching.
> - **B852** — the UI (Px) rework: existing screen structure, client-approved design (menu 5 horarios with Auto/ON/OFF + green/red LED; tablero interruptor = LED + horario selector, manual widgets removed), backing-point spec, and the `px-render.py` preview method. Advances B851-G2.
> - **B853** — Px in the BROWSER (Hx profile, EC-Net 4.3.58.18): `set(value)` fails in Hx (works in WB) → HOA must be BooleanWritable (active/inactive/auto, empty Override); numeric entry via `/set` dialog; Paste Special "Keep all links"; import needs; the working GM02+menu recipe. Deployment reality behind B851/B852. Status/handoff in `clients/distech-merida-harbor/docs/greenmax-redesign-status.md`.
 - **B856** — Hx vs bajaux/ux Px rendering engine reference: module structure per N4 version, exact `ActionBinding` fixed-arg failure mechanism (`BHxPxActionBinding:114-116` + `BHxPxValueBinding:196-212`), bajaux `decodeAsync` fix, version gates (N4.0 bajaux intro / N4.10 UxMedia / N4.13 WbWebProfile deprecated), and practical design rules for Hx-only stations. Closes B853-G4. Seeds B856-G1/G2/G3.
 - **B857** — GreenMAX custom dashboard module (BWebServlet + rc/ SPA): cost/feasibility verdict, correction to B845 §2 web.xml inference (BWebServlet uses BComponent-tree dynamic registration on 4.3, not web.xml), write path (standard Niagara session auth + OPERATOR_WRITE, DashboardRbacHelper pattern), GreenMAX vs cold-room differences (enum+boolean writes, -ux only, no rt), cost vs Px+dialog baseline and EnumWritable middle path, upgrade recompile tie to B855. Closes B853-G5.
 - **B858** — Hx image rendering cluster (B853-G6/G8/G10 + B856-G3): hypothesis (a) REFUTED (`file:^` assets served fresh via `max-age=0` + `Last-Modified`); hypothesis (b) [INFER] dominant for G6 (`<button>` UA opaque background covers Picture, `buttonStyle="toolBar"` no transparent bg in CSS); G8 CLOSED [CERT] (`max-age=0`, no restart needed, `BModule` images long-term cached 30 days); B856-G3 CLOSED [CERT] (toolBar hover-only CSS, `selected` class only for BToggleButton.getSelected()==true, ImageButton never gets it); G10 logic CLOSED [CERT] (BStatusDemux.overridden + LED beside button). Seeds B858-G1 (live confirmation of button opaque background).
 - **B859** — bog-emitter feasibility: link grammar, handle mechanics, and programmatic control-graph generation (closes B853-G9). CERT-live: all 835 sourceOrds are `h:`-based; link lives in target; max handle `0x8bf9e`; no handle counter in XML. Strategy C (offline greenmax-panel-gen.py emitter) recommended — PARTIAL: cross-subtree handles (10 Horarios handles) required as emitter parameters; cutover link permanently manual. Seeds B859-G1 (write+prove the emitter), B859-G2 (confirm Workbench 4.3 paste-from-XML path).
 - **B861** — B853-G2 PARTIAL: `BEnumWritable` (`control-rt`, `javax.baja.control.BEnumWritable`) source-level mechanics: `set` action takes `BDynamicEnum` (same Hx fixed-arg failure as numeric); `BEnumRange` facet is STATIC (cannot track `Horario{n}_Nombre` changes); `BHxEnumFE` (N4.14 `hx-wb`) renders `<select>` (FrozenEnum/`preferFrozenEditor=true` facet, jQuery only) or text+datalist (DynamicEnum, `jquery.relevant-dropdown`); `ActionDialog` confirmed in client N4.3 (`BHxPxActionBinding$ActionDialog.class`); N4.3 exact render unconfirmed. Net verdict: yes, UX improvement (names vs. numbers), static-range caveat. Seeds B861-G1 (req-exec live test on EC-Net 4.3).

<!-- research-state.v1 -->
schema: research-state.v1
method: normal-cycle
block_scope: shared-global
covered_blocks: 841,842,843,844,845,846,847,848,851,852,853,854,855,856,857,858,859,860,861,866
gaps_closed: 21
known_gaps: 53
investigable_open: 15
requires_execution_open: 13
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Design corpus complete for the feasibility decision. Both build routes and the dashboard options are specified
and cite the same `[CERT-live]` floor (B841). What remains is client input (matriz de puntos, scope/design
decisions) and Workbench-side execution proof.

**Post-B866 investigable status:** Of the 15 remaining investigable_open gaps, all but two require the live
station or client input. Exceptions: B854-G4 (local tool dry-run — greenmax-tablero-gen.py naming for
single-digit panels; testable without a live station) and B856-G1 (unavailable wall — needs an N4.7/N4.8/N4.9
corpus install outside this workspace). All others are req-exec (live Workbench/station) or client-input-blocked.

**CLOSED:**
- **B841-G1** — matriz de puntos → **closed by B846**: extracted from both bogs via `tools/greenmax-matrix.py`, 349 circuits, 323 with in-station descriptions → `docs/matriz-puntos-DRAFT.csv`. Residual (26 undescribed + GM01 count) reopened narrowly as B846-G1/G2.
- **B847-G1 / B844-G1** — link-type test → **closed by B851 (live)**: `SelR{k}.out` (StatusNumeric) links **directly** to `BBooleanSelect.select` (StatusEnum) in 4.3, no converter block.
- **B842-G1** — HOA granularity → **closed by client (B851)**: Auto/ON/OFF lives **per horario** (5 shared), NOT per circuit; each circuit only selects its horario.
- **B842-G2** — override priority → **resolved by design (B851)**: HOA is done by *selection* (BBooleanSelect), not by priority-slot juggling; everything lands on `HorR{k}.in10`, no null-release problem. No live priority-array test needed.

**NEW (requires execution):**
- **B851-G1** — replicate GM02 circuits 2–20 and TIME them to firm the per-panel / per-campaign estimate.
- **B851-G2** — the Px/UI rework: per-circuit horario selector → `SelR{k}`; per-horario Auto/ON/OFF + editable name → `Hor{n}_Mode` / `Horario{n}_Nombre`; across 13 panel screens + the menu. **Design specified in B852**; apply live still open.
- **B852-G1** — confirm the exact write action/priority for `Hor{n}_Mode/set` and `SelR{k}` from the buttons/selector, live in Workbench (as the pilot gate was confirmed). **RESOLVED in the browser by B853** (Hx needs no-arg actions / `/set` dialog).

**REGISTERED for a future session (do NOT investigate now):**
- **B853-G1** (req-exec) — confirm the final GM02 works end-to-end in the browser (menu no-popup, selector Set dialog, LEDs).
- ~~**B853-G2**~~ **PARTIAL → B861** — cleaner selector: source mechanics proven [CERT N4.14]. `BEnumWritable.set` takes `BDynamicEnum`; `BEnumRange` is static (no live tracking of `Horario{n}_Nombre`); `BHxEnumFE` renders `<select>` with `preferFrozenEditor=true` facet (jQuery only, safe for 4.3); ActionDialog write path confirmed. Live test B861-G1 needed to close fully.
- **B853-G3** (req-exec) — passthrough cleanup: re-point circuits to `Hor{n}_Eff` and remove the redundant `Hor{n}_HOA` BooleanSelect + `Hor{n}_Mode`.
- **B853-G4** ~~(investigable)~~ **CLOSED by B856** — Hx vs bajaux web-profile behavior differences across N4 versions (client 4.3 vs corpus 4.14) as a reference of stock-Px binding support per browser engine.
- ~~**B853-G5**~~ **CLOSED by B857** — custom GreenMAX dashboard module (JS+servlet, DashboardPan pattern) cost/feasibility: BWebServlet N4.0+ dynamic registration works on 4.3 (not web.xml); write gate = standard session auth + OPERATOR_WRITE; -ux module only; ~2–4 dev-days; EnumWritable dropdown (B853-G2) is the no-module intermediate path.
- ~~**B853-G8**~~ **CLOSED by B858** — `file:^` asset lifecycle: `Cache-Control: private, must-revalidate, max-age=0` + `Last-Modified`; browser revalidates every request; newly uploaded file served without station restart; `BModule`/`BZipSpace` images get `max-age=2592000` (30 days). [CERT] FileServlet.java.
- ~~**B856-G3**~~ **CLOSED by B858** — `buttonStyle="toolBar"` Hx highlight: UxButtonUtil appends `ux-Button-buttonStyle-toolBar` CSS class (hover-only box-shadow); `selected` state requires `BToggleButton.getSelected()==true`; BImageButton is NOT a BToggleButton → never gets `selected` class → no value-driven persistent highlight. [CERT] UxButtonUtil.java:38-43.
- **B853-G6** — PARTIAL (closed dominant hypothesis [INFER], live confirmation open as B858-G1). Hypothesis (a) REFUTED [CERT]; hypothesis (b) [INFER]-dominant: `<button>` UA opaque background covers Picture placed behind ImageButton; toolBar CSS has no `background: transparent`.
- ~~**B853-G10**~~ **CLOSED by B858** — Logic side [CERT]: BStatusDemux.overridden → IBooleanToSimple → LED beside button (not behind it); `IStatusToSimple ok` for Auto. Rendering side: LED/pill widget beside ImageButton is the working Hx approach; picture-behind-button approach fails per G6.

**Investigable (need client info or bog re-inspection):**
- **B846-G2** — client cuadro de cargas to fill the 26 undescribed circuits (GM05×13, GM16×6, GM01×5) and validate the 323 in-station descriptions.
- ~~**B846-G3**~~ **CLOSED by B860** — normalize GM01: concrete change set (14 × HorR{k} Supervisor writables + relink Schedule{k} → HorR{k}.in10 + labelR{k}/tabR{k} from client matrix + populate HorGM01 on JACE + upgrade in16→in10 + decommission GreenMAX1 links). Prerequisite for single-path emitter (B859-G1). Seeds B860-G1 (live priority-array check before commissioning).
- ~~**B841-G2**~~ **CLOSED by B860** — HorGMnn verdict: HorGM02-16 are the active production JACE-side relay seam (subscribe Supervisor HorR{k} writables, link .out → Relay[k].BO.in10, circuit counts match exactly); HorGM01 is an empty pending-migration placeholder (0 children, 0 links, structurally ready).
- ~~**B841-G3**~~ **CLOSED by B860** — command-writable naming: GM01 = `SchdlGM01 R{k}` (c:BooleanWritable with space, R1..R14, JACE GreenMAX1 folder, in16 relay); GM02-GM16 = `HorR{k}` (c:BooleanWritable, JACE HorGMnn folder, in10 relay). [CERT-live]
- **B842-G1** — HOA granularity wanted: per-circuit only, per-panel only, or both.
- **B842-G3** — whether the 4 masters need shared calendar/holiday exceptions or flat weekly schedules suffice.
- **B843-G1/G2/G3 — CLOSED by B855**: slot facets (`Flags.OPERATOR`/`READONLY|TRANSIENT`, `@NiagaraEnum`+`@Range` auto-range, no `@Facet` for enum types), instantiation tooling (bog-subtree emitter recommended over BajaScript/manual), and upgrade recompile plan (set `niagara_home` + bump `defaultModuleVersion` in GROUP build.gradle.kts + sign; Route B zero-cost by comparison). Original B843-G1 (write mechanism for HOA null-status) is a SEPARATE gap that remains in requires-execution.
- **B845-G1** — which dashboard route the client/SEJOFA scope funds (Px in-scope vs HTML module upsell).

**Requires execution (Workbench / build-deploy on an authorized station):**
- **B861-G1** (req-exec) — Live confirmation for B853-G2 on EC-Net 4.3.58.18: create `BEnumWritable` with `BEnumRange {0-4}` + facet `preferFrozenEditor=true`, bind Px `ActionBinding SelR{k}/set` (empty actionArg), open in client browser — confirm (a) ActionDialog opens, (b) `<select>` with 5 named options renders, (c) submit writes correct ordinal. Closes B853-G2 from [INFER] to [CERT-live].
- **B860-G1** (req-exec) — Before upgrading GM01 relay priority from in16 to in10, read the live priority array on `GM01ilum/Relay[k].BO` in Workbench to confirm no write occupies priorities 11–15. Prerequisite for §4.3 step 7 of B860.
- **B846-G1** — verify GM01's real relay count live (14 proxied vs BACnet BOs to 21 on the JACE) — spare or wired?
- **B842-G2** — decide + prove the override priority (in8 vs emergency in1 + operator in8) against the live priority array.
- **B844-G1** — Workbench link-type test: `NumericWritable.out → BBooleanSelect.select` before bulk replication.
- **B844-G2/G3** — template copy-paste at ~330× and config.bog-bloat / performance measurement.
- **B843** build/sign/deploy proof of the module on the 4.3 Supervisor (if Route A is chosen).

## Seeded 2026-09-11 — name-selector + full replication session (NOT yet investigated)
Context: this session shipped editable schedule names (`Horario{n}_Nombre` + `NameMux{k}` BStringSelect),
reverted the tablero LED to `Relay[k].BO` status, replicated the redesign to all 13 operative tableros via
a reusable generator, and added a read-only "Horarios" legend. Left open:

**Investigable (worth a research block):**
- **B853-G6** — WHY the selected-mode highlight image (`file:^…/BotonVerde.png` via `IStatusToSimple`/`IBooleanToSimple`) did NOT render in the client Hx browser while pill/LED images DO. Hypotheses to test: Hx serves station assets from a cache (new `BotonVerde.png` not picked up without a nav/station reload); `buttonStyle="toolBar"` paints over the Picture behind it; z-order/scale of a Picture-behind-ImageButton in Hx. Concrete parked failure of the Nivel-2 highlight.
- **B853-G8** — Hx asset lifecycle: how added `file:^` files propagate to a running Hx browser (station restart? nav rebuild? cache-bust?). Generalizes G6 and governs every future asset change.
- ~~**B853-G9**~~ **CLOSED (PARTIAL) by B859** — programmatic control-graph generation: Strategy C (offline `greenmax-panel-gen.py` emitter) recommended. CERT-live: all 835 bog sourceOrds are `h:`-based; link lives in target component; max handle `0x8bf9e`; no handle counter in XML — offline emitter uses placeholder handles (remapped by Workbench on paste). PARTIAL: cross-subtree links to Hor{n}_HOA and Horario{n}_Nombre require 10 real station handles as emitter parameters (readable after Horarios folder is built). Cutover link permanently manual (lives in existing HorR{k}, requires live value-match pre-check). Seeds B859-G1 (write + prove emitter) and B859-G2 (confirm 4.3 paste-from-XML path).
- **B853-G10** — full selected-mode-highlight reference: the verified control recipe (`BStatusDemux.overridden` + `BAnd`/`BNot` → `isOn`/`isOff`; Auto via `IStatusToSimple` `ok`). Fold into a block once G6 resolves the Hx rendering side.

**Requires execution (Workbench / live station):**
- **B853-G7** — confirm the `BStringWritable` `/set` dialog accepts TEXT entry in the client Hx profile (analogous to the numeric `/set` used by the selector), for editing `Horario{n}_Nombre` from the menu. Never confirmed live this session.
- **B858-G1** — confirm hypothesis (b) from B853-G6 live: in a browser-opened tablero Px, inspect the ImageButton `<button>` element background in DevTools; toggle `background: transparent` and verify the Picture behind it becomes visible. Closes B853-G6 from [INFER] to [CERT-live].

**Investigable (seeded by B854):**
- **B854-G1** (investigable) — Confirm where SelR/SchedMux/NameMux pilot components (GM02 c1) actually landed in the live station; a fresh export post-redesign resolves whether `Config/Iluminacion/GM{n}` or just `Iluminacion/GM{n}` is the actual path.
- ~~**B854-G2**~~ **CLOSED by B866** — `GreenMAX11 TabAL9.px` is an orphaned empty placeholder (200 bytes, `ScrollPane > CanvasPane viewSize='500,400'` with no children, 0 widgets, 0 relay bindings, imports `gx`+`bajaui` only). The real GM11 tablero is `GreenMAX11 TabAL10.px` (1614 lines, 26 relays). [CERT] `shared/px/GreenMAX11 TabAL9.px:1-14`.
- ~~**B854-G3**~~ **CLOSED (PARTIAL) by B860** — Full cross-station survey: panels 10 and 12 have extra zero on HM_BMS side only; panel 11 is consistent (both GM011ilum); panel 15 is inverted (HM_Central=GM015ilum, HM_BMS=GM15ilum); panels 1-9, 13-14, 16 consistent. HorGMnn naming is independent (plain decimal, no extra zeros). PARTIAL: which side to correct for panel 15 is an operator decision (seeds B854-G3 residual).
- **B854-G4** (investigable) — Verify greenmax-tablero-gen.py control-folder naming for single-digit panels: does device `GM02ilum` → `Iluminacion/GM2` or `Iluminacion/GM02`? Docstring says "plain number" but a dry-run confirms which.

**Seeded by B855:**
- **B855-G1** (req-exec) — Prove the bog-subtree emitter: write `tools/greenmax-panel-gen.py` (GM02, 20 circuits), import into a scratch station with `GreenMaxLighting-rt` loaded, confirm `BGreenMaxCircuit` children resolve and output links bind.
- **B855-G2** (investigable) — Confirm whether Workbench 4.3 bog import preserves `@NiagaraProperty` enum defaults (e.g., `circuitScheduleSel = FollowPanel`) or resets to ordinal 0 on import, requiring a post-import fixup step.
- ~~Note: B853-G9 (programmatic control-graph generation) design portion addressed by B855 §2; implementation pending B855-G1.~~ Superseded by B859 which closes B853-G9 with concrete bog mechanics.

**Seeded by B859:**
- **B859-G1** (req-exec) — Write `tools/greenmax-panel-gen.py` for GM02 (N=20), read the 10 Horarios handles from the live bog (after Horarios folder is confirmed created), paste into a scratch station with kitControl loaded, confirm SchedMux/SelR/NameMux children resolve and intra-subtree links bind. Depends on B854-G1 (exact folder path) and confirmed Horarios handles.
- **B859-G2** (investigable) — Confirm whether Workbench 4.3 BogEditor or Paste-from-XML accepts the emitter's XML format. If not, fall back to Strategy B (BajaScript) as the paste mechanism. Determines `greenmax-panel-gen.py`'s output format.

**Seeded by B856:**
- **B856-G1** (investigable, PARTIAL — unavailable wall) — Exact N4 minor version where `kitPx-ux.jar` first shipped. B866 §2 attempted: `module_nav.py resources kitPx-ux` → "JAR not found"; `niagara_help.py find "kitPx-ux"` → "No results"; `organized/` has no N4.7–N4.11 install. N4.7–N4.10 bound from B856 §2.3 stays [INFER]. Requires an N4.7/N4.8/N4.9 install outside the current corpus to close.
- **B856-G2** (investigable) — Test fixed `actionArg` on a station running N4.10+ with UxMedia enabled: confirm bajaux `ActionBinding.js` resolves the arg correctly via `decodeAsync` (`[CERT]` from code, needs `[CERT-live]`).
- **B856-G3** (investigable) — `ImageButton buttonStyle="toolBar"` hover/highlight rendering in Hx: does `UxImageButtonUtil.write()` vs the legacy path in `BHxPxImageButton` explain the Nivel-2 highlight not rendering (B853-G6 root cause complement).

**New block candidates (corpus, when time):**
- **B854 (covered)** — HARBOR HM_BMS Supervisor control-folder and device-naming architecture. Device folder naming in HM_BMS: GM010ilum/GM011ilum/GM012ilum carry extra leading zero (Workbench typo), GM01–09 and GM13–16 do not. Per-panel relay counts confirmed [CERT-live] for all 13 operative panels. `GreenMAX07 TabAL8.px` = GM08's 32-relay view (mislabeled). Pills/LEDs bind relative `slot:Relay[k].BO` [CERT]. Control folder path = `/Iluminacion/GM{n}` plain (no leading zero, no Config/ prefix) [CERT from tool]. SelR/SchedMux/NameMux absent from 2026-09-08 backup [INFER for location]. Two config.bog files in backup: root (82k lines, full) vs HM_BMS/ (17k, partial). Seeded B854-G1..G4.
- **Tool block** — DONE 2026-09-11: promoted to `tools/greenmax-tablero-gen.py` (argparse + docstring + `--legend`, registered in `tools/README.md`). Auto-detects pill positions + circuit count, removes HOA buttons + calendar popups, keeps pill/LED, appends the `NameMux/SelR` selector, writes absolute `Iluminacion/{gm}/` ords, validates XML, bumps `viewSize`.
- **kitControl focus pointer** — the verified select+logic block family used here (`BStringSelect` 1-based inA..inJ; `BMuxSwitch`/`BSwitch` `select`=`BStatusEnum`, `numberValues`, `zeroBasedSelect`; `BStatusDemux` flag outputs incl. `overridden`; `BAnd` extends `BQuadLogic`; `BNot`) belongs in the **kitControl** focus, not here — seed it there.

## Decision summary (for the operator)
- **Feasible: yes**, both routes. Redesign lives entirely on the **Supervisor** feeding the existing `SchdlGMnn Rk` writables; JACE↔BACnet untouched.
- **Route A (custom module)** = clean, one reusable component, easy dashboard binding; cost = build/sign/deploy + recompile on any N4 major upgrade.
- **Route B (kitControl)** = zero module maintenance, survives upgrades; cost = ~330 replicated containers, no global-change mechanism.
- **Dashboard** = keep Px for the quoted scope; HTML web module (BWebServlet + rc/ SPA, DashboardPan pattern) as a priced upsell. **Correction (B857):** on 4.3, BWebServlet registers via the BComponent tree (N4.0+), NOT via web.xml — the pre-4.13 delta is HTTP/TLS server config only. Writes use Niagara session auth + OPERATOR_WRITE check (DashboardRbacHelper pattern). EnumWritable dropdown (B853-G2) is the no-module intermediate option before quoting the full module.
- **Scope flag:** SEJOFA proposal reads "Selector Manual/Horario" (2-state); the operator's 4-schedule + per-circuit ask is richer → confirm in-scope or price as an extension.
