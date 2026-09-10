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

<!-- research-state.v1 -->
schema: research-state.v1
method: normal-cycle
block_scope: shared-global
covered_blocks: 841,842,843,844,845,846,847,848,851
gaps_closed: 5
known_gaps: 26
investigable_open: 15
requires_execution_open: 6
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Design corpus complete for the feasibility decision. Both build routes and the dashboard options are specified
and cite the same `[CERT-live]` floor (B841). What remains is client input (matriz de puntos, scope/design
decisions) and Workbench-side execution proof.

**CLOSED:**
- **B841-G1** — matriz de puntos → **closed by B846**: extracted from both bogs via `tools/greenmax-matrix.py`, 349 circuits, 323 with in-station descriptions → `docs/matriz-puntos-DRAFT.csv`. Residual (26 undescribed + GM01 count) reopened narrowly as B846-G1/G2.
- **B847-G1 / B844-G1** — link-type test → **closed by B851 (live)**: `SelR{k}.out` (StatusNumeric) links **directly** to `BBooleanSelect.select` (StatusEnum) in 4.3, no converter block.
- **B842-G1** — HOA granularity → **closed by client (B851)**: Auto/ON/OFF lives **per horario** (5 shared), NOT per circuit; each circuit only selects its horario.
- **B842-G2** — override priority → **resolved by design (B851)**: HOA is done by *selection* (BBooleanSelect), not by priority-slot juggling; everything lands on `HorR{k}.in10`, no null-release problem. No live priority-array test needed.

**NEW (requires execution):**
- **B851-G1** — replicate GM02 circuits 2–20 and TIME them to firm the per-panel / per-campaign estimate.
- **B851-G2** — the Px/UI rework: per-circuit horario selector → `SelR{k}`; per-horario Auto/ON/OFF + editable name → `Hor{n}_Mode` / `Horario{n}_Nombre`; across 13 panel screens + the menu.

**Investigable (need client info or bog re-inspection):**
- **B846-G2** — client cuadro de cargas to fill the 26 undescribed circuits (GM05×13, GM16×6, GM01×5) and validate the 323 in-station descriptions.
- **B846-G3** — normalize GM01 to the newer `R{k}`/`HorR{k}`/`labelR{k}` pattern during the redesign (uniformity for B843/B844).
- **B841-G2** — what the JACE `HorGMnn` folders currently hold (reusable consolidation seam or dead scaffolding).
- **B841-G3** — confirm the command-writable naming per panel (only GM01 verified as `SchdlGM01 Rk`).
- **B842-G1** — HOA granularity wanted: per-circuit only, per-panel only, or both.
- **B842-G3** — whether the 4 masters need shared calendar/holiday exceptions or flat weekly schedules suffice.
- **B843-G1/G2/G3** — custom-module design details (slot facets, instantiation tooling, upgrade recompile plan).
- **B845-G1** — which dashboard route the client/SEJOFA scope funds (Px in-scope vs HTML module upsell).

**Requires execution (Workbench / build-deploy on an authorized station):**
- **B846-G1** — verify GM01's real relay count live (14 proxied vs BACnet BOs to 21 on the JACE) — spare or wired?
- **B842-G2** — decide + prove the override priority (in8 vs emergency in1 + operator in8) against the live priority array.
- **B844-G1** — Workbench link-type test: `NumericWritable.out → BBooleanSelect.select` before bulk replication.
- **B844-G2/G3** — template copy-paste at ~330× and config.bog-bloat / performance measurement.
- **B843** build/sign/deploy proof of the module on the 4.3 Supervisor (if Route A is chosen).

## Decision summary (for the operator)
- **Feasible: yes**, both routes. Redesign lives entirely on the **Supervisor** feeding the existing `SchdlGMnn Rk` writables; JACE↔BACnet untouched.
- **Route A (custom module)** = clean, one reusable component, easy dashboard binding; cost = build/sign/deploy + recompile on any N4 major upgrade.
- **Route B (kitControl)** = zero module maintenance, survives upgrades; cost = ~330 replicated containers, no global-change mechanism.
- **Dashboard** = keep Px for the quoted scope; HTML web module (BWebServlet + rc/ SPA, DashboardPan pattern) as a priced upsell — on 4.3 the servlet registers via `web.xml` (BWebService is ≥N4.13). Writes must use Niagara auth (R14 second-login) + the priority array.
- **Scope flag:** SEJOFA proposal reads "Selector Manual/Horario" (2-state); the operator's 4-schedule + per-circuit ask is richer → confirm in-scope or price as an extension.
