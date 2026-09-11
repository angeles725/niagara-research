# Block 859 — HARBOR GreenMAX bog-emitter feasibility: handle mechanics, link grammar, and programmatic control-graph generation (closes B853-G9)

**Focus:** harbor-greenmax-lighting
**Sources:**
- `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/config.bog` (ZIP → `file.xml`, 82,519 lines, sha256 `4515de9498e7…`) — read with `tools/bog-nav.py` and direct ZIP inspection. Registered in SOURCES.md; this block adds B859 to its citing-blocks list.
- B851 `[CERT-live]` — verified control model, component types, live pilot on GM02 c1.
- B852 `[CERT-live]` — client-approved UI design; NameMux{k} identified as BStringSelect.
- B854 `[CERT-live]` — per-panel relay counts; confirms SelR/SchedMux/NameMux absent from the 2026-09-08 backup.
- B855 `[CERT/INFER]` — recommended the bog-subtree emitter over BajaScript/manual; seeded B855-G1 (live proof).
- `tools/bog-nav.py` and `tools/greenmax-tablero-gen.py` — tool infrastructure.

**Scope:** close B853-G9 — the design and feasibility of a programmatic emitter that generates the per-panel control subtree (`SelR{k}` + `SchedMux{k}` + `NameMux{k}`) as a paste-able or importable bog fragment, avoiding ~330 manual Workbench operations. This block does NOT repeat B855 §2 (option comparison already done there); it goes deeper on the bog mechanics that B855 could only infer.

**Marker discipline:** `[CERT-live]` = verified against the 2026-09-08 HM_BMS backup via `bog-nav.py` or direct ZIP read with cited command. `[CERT]` = code/tool file:line. `[INFER-from-design]` = shape derived from B851/B852 design; components absent from the backup. `[INFER]` = derived without direct evidence.

---

## 1. The real bog-XML link grammar `[CERT-live]`

The fundamental serialization question: what does a link look like in the bog file, and where does it live?

### 1.1 Observed raw XML for `R1.out → HorR1.in10`

Extracted from `file.xml` at line 31724 via direct ZIP read:

```xml
<p n='HorR1' h='6a4d5' t='c:BooleanWritable'>
 <p n="in10" f="tsL"/>
 <p n="in16" f="t"/>
 <p n="wsAnnotation" t="b:WsAnnotation" v="17,2,8"/>
 <p n='Link' t='b:Link'>
  <p n="sourceOrd" v="h:6a35f"/>
  <p n="relationTags" v=""/>
  <p n="relationId" v="n:dataLink"/>
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="in10"/>
 </p>
</p>
```

[CERT-live: `python3 -c "import zipfile; ..."` on `HM_BMS_backup_08_09_2026/config.bog`, line 31724 in `file.xml`]

Key grammar observations:
- The **link lives on the TARGET component** (HorR1), not the source (R1).
- `sourceOrd = "h:6a35f"` — a bare hex handle reference to the source component (R1, `h='6a35f'` at line 30633).
- `sourceSlotName` and `targetSlotName` are separate child attributes (always `out` and `in10` for the schedule→writable cutover).
- Handles are 4–5 hex digits in this station; `h='6a4d5'` = decimal 434,389.

### 1.2 Adjacent HorR2 handle — sequential allocation `[CERT-live]`

```xml
<p n='HorR2' h='6a4d7' t='c:BooleanWritable'>
 <p n='Link' t='b:Link'>
  <p n="sourceOrd" v="h:6a38a"/>
  ...
</p>
```

HorR1 = `6a4d5`, HorR2 = `6a4d7` — delta of 2 (R1/R2 each take one handle for the `effective` sub-component). The allocation is sequential within a session; gap = +2 because R1 (`6a35f`) and R2 are themselves multi-component objects with internal subgraphs.

[CERT-live: lines 31720–31737 of `file.xml`]

### 1.3 ALL 835 sourceOrds are handle-based — no path-based ords `[CERT-live]`

```
bog-nav.py ... (direct ZIP scan):
  Total sourceOrds in bog: 835
  Handle-based (h:):        835
  Path-based (station:, slot:, etc.): 0
```

[CERT-live: `python3 -c "import zipfile, re; ... re.findall(r'<p n=\"sourceOrd\" v=\"([^\"]+)\"', content)"` on the full bog]

This is the dominant constraint for the emitter: **there is no path-based link encoding in this station**. Every link resolves by handle, not by component path. A component's handle is its identity in the link graph.

---

## 2. Handle allocation mechanics `[CERT-live]`

### 2.1 Station bog header — no explicit handle counter

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bajaObjectGraph version='4.0' reversibleEncodingKeySource='external' ...>
 <p h='2' c='1' m='b=baja' t='b:Station'>
```

[CERT-live: `file.xml` line 3]

- The `bajaObjectGraph` root element has NO `nextHandle` or `handleCount` attribute.
- The station root `<p>` carries `c='1'` — the only `c=` attribute in the entire file (confirmed: `re.findall(r'<p h=... c=...', content)` returns only `[(h:2, c=1)]`). Its meaning is NOT a handle counter; `c='1'` with max handle `0x8bf9e` makes no sense as a next-handle pointer. Its most likely interpretation is a station-version or encoding-epoch marker.
- Max handle in the full bog: **`0x8bf9e`** (decimal 573,342), from 34,799 total handle assignments.

[CERT-live: `max(int(h, 16) for h in re.findall(r"h='([0-9a-fA-F]+)'", content))` → 573342]

### 2.2 Where the next-handle counter lives

The bog XML is a **serialized snapshot**. The platform's live handle allocator lives in the **running station's runtime state** (in-memory), not in the serialized file. When Workbench creates or pastes components via the FOX protocol, it asks the running station for the next available handle. There is no way to read the next available handle from the bog file alone.

**Practical consequence for an emitter:**
- An offline emitter cannot know what handle the station will assign to a new component.
- An emitter that uses PLACEHOLDER handles (e.g., `aa001`, `aa002`, …) is safe only if Workbench re-assigns those handles on paste/import.
- An emitter that uses handles > max-existing (e.g., `0x90000`) reduces collision risk but is NOT guaranteed to be conflict-free (the station may have allocated handles > `0x8bf9e` since the backup was taken).

### 2.3 Workbench handle re-assignment on paste

When Workbench copies a component subtree to the clipboard (bog XML fragment) and pastes it:
1. The platform parses the fragment and identifies all `h=` handles in it.
2. For each component, it allocates a NEW station handle from the running allocator.
3. It builds a remapping table: `{old_placeholder → new_station_handle}`.
4. For each `sourceOrd = "h:old"` whose `old` is IN the fragment (intra-blob link), it rewrites the sourceOrd with the new handle.
5. For `sourceOrd = "h:real"` whose `real` is NOT in the fragment (cross-blob link), it keeps the original handle — treating it as a live external reference.

This behavior is the mechanism that makes copy-paste work in Workbench. It is also the mechanism an emitter must target. [INFER: standard Workbench paste semantics; not directly confirmed from bog-nav on this backup since the pilot components are absent from the backup. Relies on B851 §5 pilot success as indirect evidence.]

---

## 3. The design-inferred bog-XML shape for one panel `[INFER-from-design + CERT-live anchor]`

B854 confirms SelR/SchedMux/NameMux are absent from the 2026-09-08 backup. The shapes below are `[INFER-from-design]` (derived from B851/B852) anchored at `[CERT-live]` for the components they link TO (HorR{k}).

### 3.1 Component types (from B851/B852/RESEARCH-STATE)

| Component | Type | Module alias | Source |
|---|---|---|---|
| `SelR{k}` | `constants:BNumericWritable` | `constants=constants` | B852 §5 — pilot's BNumericConst replaced by writable |
| `SchedMux{k}` | `util:BBooleanSelect` `numberValues=5` | `util=utilities` (inferred) | B851 §4 `[CERT-live]` |
| `NameMux{k}` | `BStringSelect` | `util=utilities` (inferred) | RESEARCH-STATE 2026-09-11 note |

The module alias `util=` is NOT in the 2026-09-08 bog (the control subtree is absent). When the emitter blob is pasted, Workbench adds the `util=` alias to the bog if the module is already loaded. [INFER-from-design]

### 3.2 Annotated bog-XML skeleton for one circuit `k` of panel `GM{n}` [INFER-from-design]

The following XML is the emitter's TARGET OUTPUT for one circuit. Placeholder handles `ee{k}01`–`ee{k}04` are used and will be remapped by Workbench on paste.

```xml
<!-- INFER-from-design: component absent from 2026-09-08 backup -->
<!-- Placeholder handles ee{k}01..ee{k}04 will be re-assigned on paste -->

<!-- SelR{k}: operator writes 1..5 to pick the horario -->
<p n='SelRK'  h='eeK01' t='constants:BNumericWritable'>
 <!-- no child slot override: default value=1 -->
</p>

<!-- SchedMux{k}: 5-way boolean mux; .out goes to HorR{k}.in10 -->
<p n='SchedMuxK' h='eeK02' t='util:BBooleanSelect'>
 <p n="numberValues" v="5"/>
 <!-- inA..inE fed from Horarios/Hor1_HOA.out .. Hor5_HOA.out (CROSS-SUBTREE links below) -->
 <p n='Link' t='b:Link'>
  <!-- sourceOrd = REAL handle of Hor1_HOA — must be provided as emitter parameter -->
  <p n="sourceOrd"      v="h:REAL_HOR1_HOA_HANDLE"/>
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="inA"/>
 </p>
 <p n='Link' t='b:Link'>
  <p n="sourceOrd"      v="h:REAL_HOR2_HOA_HANDLE"/>
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="inB"/>
 </p>
 <!-- ... inC, inD, inE for Hor3..Hor5 -->
 <!-- INTRA-SUBTREE link: SelR{k}.out → SchedMux{k}.select -->
 <p n='Link' t='b:Link'>
  <p n="sourceOrd"      v="h:eeK01"/>   <!-- placeholder; remapped on paste -->
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="select"/>
 </p>
</p>

<!-- NameMux{k}: 5-way string mux; .out = selected horario's display name -->
<p n='NameMuxK' h='eeK03' t='util:BStringSelect'>
 <p n="numberValues" v="5"/>
 <!-- inA..inE fed from Horarios/Horario1_Nombre.out .. Horario5_Nombre.out (CROSS-SUBTREE) -->
 <p n='Link' t='b:Link'>
  <p n="sourceOrd"      v="h:REAL_HOR1_NOMBRE_HANDLE"/>
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="inA"/>
 </p>
 <!-- ... inB..inE for Horario2..5_Nombre -->
 <!-- INTRA-SUBTREE link: SelR{k}.out → NameMux{k}.select -->
 <p n='Link' t='b:Link'>
  <p n="sourceOrd"      v="h:eeK01"/>   <!-- placeholder; remapped on paste -->
  <p n="sourceSlotName" v="out"/>
  <p n="targetSlotName" v="select"/>
 </p>
</p>
```

**What is NOT in this blob:** the cutover link `SchedMux{k}.out → HorR{k}.in10`. That link lives in `HorR{k}` (the TARGET), which already exists in the station. It must be added as a separate step after paste (§4.3 below).

### 3.3 Cross-subtree handle dependencies

The emitter requires the REAL station handles of:
- `Hor1_HOA` .. `Hor5_HOA` (5 handles) — sources for SchedMux inA..inE
- `Horario1_Nombre` .. `Horario5_Nombre` (5 handles) — sources for NameMux inA..inE

These 10 handles are queryable from the live station bog AFTER the `Config/Iluminacion/Horarios/` folder is created (B851 §5 confirms this folder was built on the live pilot but is absent from the 2026-09-08 backup). The emitter takes these 10 handles as parameters:

```
python3 tools/greenmax-panel-gen.py \
  --bog HM_BMS_backup_08_09_2026/config.bog \   # reads max handle; will warn if handles absent
  --gm GM02 --count 20 \
  --hor-hoa h:XXXX h:YYYY h:ZZZZ h:AAAA h:BBBB \
  --hor-nombre h:CCCC h:DDDD h:EEEE h:FFFF h:GGGG
```

[INFER-from-design: tool interface not yet written; this is the DESIGN of the tool]

---

## 4. The cutover constraint — why it is permanently manual `[CERT-live + INFER]`

### 4.1 The link lives in HorR{k} (the existing target)

The verified bog grammar (§1.1) places the link record INSIDE the target component. `SchedMux{k}.out → HorR{k}.in10` would be a `Link` child of `HorR{k}`. But `HorR{k}` already exists in the station with its OWN existing link (`R{k}.out → HorR{k}.in10`). An import blob cannot modify an EXISTING component; it can only ADD new components under a target folder.

[CERT-live: link grammar from §1.1; HorR1 confirmed at `h='6a4d5'`, existing R1 link confirmed active]

### 4.2 The cutover is a live safety operation

B851 §5 records the correct procedure:
1. Match `SchedMux{k}.out` value to current `HorR{k}` value before switching (avoids relay flicker).
2. Delete the `R{k} → HorR{k}.in10` link.
3. Add `SchedMux{k} → HorR{k}.in10` link.

Step 1 requires observing live values in Workbench. It cannot be scripted safely without live monitoring. The emitter handles steps 2+3 ONLY at the operator's explicit instruction per circuit. [CERT-live: pilot procedure B851 §5, cutover with no flicker confirmed]

### 4.3 Cutover as a separate Workbench WireSheet operation

Each cutover is one WireSheet drag after the SchedMux batch is imported. The import blast (307 SchedMux + SelR + NameMux components) is the automation target. The 307 cutovers happen panel-by-panel in live Workbench, as in the B851 pilot — one verified drag per circuit. This is ~307 verified manual operations, but each is ~10 seconds with the matching-values pre-check.

[INFER: time estimate from B851 §7: "per-circuit build is now ~2 objects + copy-paste + cutover; the estimate firms after timing GM02 circuits 2–20"]

---

## 5. Feasibility verdict — three emit strategies `[CERT-live + INFER]`

### Strategy A — Offline bog replacement (whole-station import)

Modify `config.bog`'s `file.xml` directly offline: add the new subtree under `Config/Iluminacion/`, assign handles > `0x8bf9e`, rezip, and import the modified bog into Workbench as a station replacement.

**Verdict: NOT RECOMMENDED.** A full station reimport wipes all runtime state, resets all priorities, and is disruptive on a production station. The handle range above `0x8bf9e` is safe for the backup state but not for the live station (which has been running since 2026-09-08 and may have allocated further handles). Re-importing the entire station also destroys the pilot work already done on GM02. Additionally, the `HorR{k}` cutover links would need to be manually added in either approach, so this saves no final steps.

### Strategy B — BajaScript runtime program

A `BProgram` component in the live station runs JavaScript that calls the Baja runtime API to create SelR/SchedMux/NameMux and wire the cross-subtree links (the 5 Hor{n}_HOA handles are known at runtime — no parameter needed).

```javascript
// Pseudocode: for each circuit k in panel GM02
var ilum = BOrd.make("station:|slot:/Config/Iluminacion/GM2").get();
var hor1 = BOrd.make("station:|slot:/Config/Iluminacion/Horarios/Hor1_HOA").get();
var mux  = BObject.make("util:BBooleanSelect");
mux.set("numberValues", BInteger.make(5));
ilum.add("SchedMux" + k, mux, null);
// add links programmatically
```

**Verdict: VIABLE but medium risk.** BajaScript runs in the live station; a partial-create crash leaves an incomplete component tree. BajaScript is available on 4.3 (confirmed by B855 §2.3: `bajaScript-ux` in `kitPx-wb/module.xml`). Link creation requires the `BLink` API, whose ORD construction needs careful path addressing. The benefit over Strategy C is that it resolves cross-subtree handles at runtime without reading them from the bog first.

### Strategy C — Extend bog-nav with an emit mode (RECOMMENDED)

Write `tools/greenmax-panel-gen.py` following the `greenmax-tablero-gen.py` pattern (same infrastructure, same ORD conventions, same usage pattern). The emitter:
1. Reads the target bog to extract the Hor1..5_HOA and Horario1..5_Nombre handles (after Horarios folder is created).
2. Generates one bog XML fragment per panel with the correct N circuits.
3. Produces placeholder-handle XML (handles `ee001`..`eeNNN`) for the intra-subtree links.
4. Produces REAL-handle references for the cross-subtree links (the 10 Horarios handles).
5. Writes the output as fenced Markdown (as `greenmax-tablero-gen.py` does) or as a raw `.xml` fragment.
6. The operator pastes this XML into Workbench's BogEditor (or uses File→Import Bog Fragment if available).

| | Strategy A | Strategy B | Strategy C |
|---|---|---|---|
| Disrupts production state? | YES — full station reimport | No (live addition) | No (paste into a folder) |
| Requires Hor{n}_HOA handles? | No (runtime resolves) | No (runtime resolves) | YES — read from bog after Horarios created |
| Handles cutover links? | No — still manual | Can automate if no pre-check | No — still manual |
| Offline review? | YES — but full-station | No | YES — per-panel XML reviewable |
| Risk of partial-create? | Low (atomic bog import) | Medium (live crash) | Low (paste is atomic per paste) |
| Tooling effort | Low | Medium (BajaScript authoring) | Low (sibling of tablero-gen.py) |
| Workbench version dependency | 4.x import | 4.3 BajaScript confirmed | BogEditor paste (standard 4.x) |

**Recommendation: Strategy C (bog-nav extension / greenmax-panel-gen.py).**

The emitter is a direct extension of `greenmax-tablero-gen.py` — the proven sibling that already generates the Px side at scale. The handle dependency on Horarios is a one-time read. The output is offline-reviewable and version-controllable. Cutover links remain manual by necessity (§4.2).

The CRUX of feasibility (B853-G9's original question):
> Can an offline emitter mint handles safely?

**Answer:** Not globally-unique handles — the next available handle is in the running station's state, not in the bog XML. But an emitter CAN use placeholder handles (e.g., `ee000`–`eefff`) that Workbench re-assigns on paste, making the intra-subtree links self-consistent. Cross-subtree links REQUIRE real handles of the Horarios components, which are read from the bog after the Horarios folder is built. An emitter therefore CANNOT be fully parameter-free; it requires a one-time post-Horarios bog read.

---

## 6. Implementation sketch for `tools/greenmax-panel-gen.py` `[INFER-from-design]`

Minimal design (not yet written; B855-G1 proves the full flow):

```python
#!/usr/bin/env python3
"""greenmax-panel-gen.py — emit the SelR/SchedMux/NameMux subtree for one panel.

Usage:
  python3 tools/greenmax-panel-gen.py \
    --bog <HM_BMS_backup>/config.bog \
    --gm GM02 --count 20 \
    --read-horarios-handles        # reads Hor{n}_HOA + Horario{n}_Nombre handles from the bog
    --out /path/GM02-control.xml   # or --stdout

Reads Hor1..5_HOA and Horario1..5_Nombre handles from the bog under
slot:/Config/Iluminacion/Horarios/ (or slot:/Iluminacion/Horarios/ per B854-G1).
Emits one <p n='GM{n}' ...> subtree with N SchedMux/SelR/NameMux children + links.
Placeholder handles ee{k}{0-3} are used for intra-subtree links; Workbench re-assigns.
"""
```

Per-panel output is ~(10 + 15×N) XML lines (header + 3 components per circuit × N circuits × ~5 child slots each). For GM02 N=20: ~310 lines. For GM08/10/14/15 N=32: ~490 lines. Total across 13 panels: ~5,000 lines — manageable as a single file or 13 per-panel files.

The cutover checklist accompanies each panel file as a comment:
```
<!-- CUTOVER for GM02 Rk -> SchedMuxk:
     For each k=1..20:
       1. In Workbench WireSheet: verify SchedMuxK.out == HorRk current value
       2. Delete R{k}.out -> HorR{k}.in10 link
       3. Add SchedMux{k}.out -> HorR{k}.in10 link
-->
```

---

## Self-verify table

| # | Claim | Marker | Evidence |
|---|---|---|---|
| LG-1 | Link lives on the TARGET component; `sourceOrd = "h:xxxx"` points back to the source | [CERT-live] | `file.xml` line 31724: HorR1 contains `<p n='Link'>` + `<p n="sourceOrd" v="h:6a35f"/>` |
| LG-2 | `sourceSlotName` and `targetSlotName` are separate child attributes of the Link record | [CERT-live] | Same raw XML |
| LG-3 | All 835 sourceOrds in the full bog are handle-based (`h:xxxx`); zero path-based ords | [CERT-live] | `re.findall(r'<p n="sourceOrd" v="([^"]+)"')` → 835 total, 835 `h:` prefix, 0 others |
| LG-4 | HorR1 = `h='6a4d5'`; HorR2 = `h='6a4d7'`; R1 = `h='6a35f'` (sequential allocation, delta=2 per pair) | [CERT-live] | `file.xml` lines 31720, 31732, 30633 |
| HA-1 | Station root `c='1'` is the ONLY `c=` attribute in the bog; it is NOT the next-handle counter | [CERT-live] | `re.findall(r'<p h=.([0-9a-f]+). c=.([0-9]+).')` → exactly 1 match: `(2, 1)`; max handle `0x8bf9e` makes `c=1` nonsensical as a handle counter |
| HA-2 | Max handle in the bog: `0x8bf9e` (decimal 573,342); total handles: 34,799 | [CERT-live] | `max(int(h,16) for h in re.findall(r"h='([0-9a-fA-F]+)'", content))` |
| HA-3 | The bog XML carries NO explicit next-handle counter; the platform allocator lives in runtime state | [INFER] | Absence of counter field in XML header and body; bog grammar from bog-nav.py (stdlib parser, no counter field in Comp class) |
| DE-1 | SelR/SchedMux/NameMux absent from 2026-09-08 backup (confirmed by B854 §2 + bog-nav find) | [CERT-live] | B854 §2 explicitly: "No `SelR`, `SchedMux`, or `NameMux` component in either bog as of the 2026-09-08 snapshot" |
| DE-2 | `SelR{k}` = `constants:BNumericWritable` (production, per B852); pilot used `BNumericConst` | [CERT-live (pilot)] | B852 §5: "Per circuit (×288): `SelR{k}` (NumericWritable 1..5…). Replace the pilot's NumericConst" |
| DE-3 | `SchedMux{k}` = `util:BBooleanSelect`, `numberValues=5` | [CERT-live] | B851 §4: "SchedMuxk (util:BBooleanSelect, numberValues=5)" — live-confirmed on pilot |
| DE-4 | `NameMux{k}` = `BStringSelect` (kitControl utility) | [CERT] | RESEARCH-STATE 2026-09-11 note: "shipped editable schedule names (`Horario{n}_Nombre` + `NameMux{k}` BStringSelect)" |
| EM-1 | Intra-subtree links (SelR{k}.out → SchedMux{k}.select and → NameMux{k}.select) can use placeholder handles in the emitter blob; Workbench remaps them on paste | [INFER] | Standard Workbench paste semantics (handle remapping for components within the clipboard blob) |
| EM-2 | Cross-subtree links (Hor{n}_HOA → SchedMux{k}.inA..inE) REQUIRE the real station handles of the 10 Horarios components as emitter parameters | [CERT-live + INFER] | All sourceOrds are `h:`-based [CERT-live, LG-3]; Hor{n}_HOA is outside the emitter blob; runtime-side handle unknown without live bog query [INFER] |
| EM-3 | Cutover link (SchedMux{k}.out → HorR{k}.in10) cannot be in the emitter blob — it modifies an EXISTING component (HorR{k}) which is not part of the paste | [CERT-live + INFER] | Links live on target [LG-1]; HorR{k} is an existing component confirmed in the bog [CERT-live]; paste only ADDS to a target folder [INFER] |
| EM-4 | Strategy C (greenmax-panel-gen.py, bog-XML emitter) is recommended over A (whole-station replace) and B (BajaScript) | [INFER] | Trade-off analysis §5; follows proven tablero-gen.py infrastructure; no production-state disruption; offline review possible |
| EM-5 | BajaScript (Strategy B) IS available on 4.3 Supervisor (`bajaScript-ux` in `kitPx-wb/module.xml`) | [CERT] | B855 §2.3: `kitPx-wb/module.xml` dependency list |

**Tally:** 7 `[CERT-live]`, 3 `[CERT]` or `[CERT-live (pilot)]`, 5 `[INFER]` (of which EM-1/EM-3/EM-4 are built on CERT-live anchors). 0 unverified tool failures. The component shapes (DE-1..DE-4) are the main inference cluster; EM-1/EM-3 are inferences about Workbench paste mechanics that B855-G1 (live execution) must confirm.

---

## Connections

- **[B851]** — live pilot; verified `SchedMux{k}` type (`util:BBooleanSelect`, `numberValues=5`), SelR{k} = BNumericConst (pilot) → BNumericWritable (production per B852). The cutover procedure (§4.2) is the exact B851 §5 sequence.
- **[B852]** — `NameMux{k}` identified as BStringSelect; SelR{k} confirmed as BNumericWritable (production); backing-point spec. The emitter design (§3.2) directly implements B852 §5.
- **[B854]** — confirms SelR/SchedMux/NameMux absent from backup; per-panel relay counts (emitter `--count` parameter); handle `h='6a4d5'` for HorR1 as the real bog anchor.
- **[B855]** — recommended the bog-subtree emitter in §2.4 and seeded B855-G1 (live proof of the emitter on a scratch station). This block specifies the emitter concretely and closes the feasibility question B855 left as `[INFER]`.
- **[tools/greenmax-tablero-gen.py]** — the proven sibling: generates the Px side at scale; `greenmax-panel-gen.py` follows the same pattern (same ORD conventions, same output format, same infrastructure).
- **[B853-G9]** — this block closes the gap: programmatic control-graph generation is feasible via the Strategy C emitter (offline, cross-subtree handles as parameters, cutover permanently manual).

---

## Open gaps

- **B859-G1** (child of B855-G1, requires-execution) — Write `tools/greenmax-panel-gen.py` for GM02 (N=20), read the 10 Horarios handles from the live bog (after Horarios folder is confirmed created), paste into a scratch station with kitControl loaded, confirm that SchedMux/SelR/NameMux children resolve and intra-subtree links bind. Validates the placeholder-handle re-assignment behavior and the cross-subtree handle parameter approach. This is the EXECUTION step B855-G1 requires.
- **B859-G2** (investigable) — Confirm whether Workbench 4.3's BogEditor or Paste-from-XML feature exists and accepts the emitter's XML format. If the paste-from-XML path does not exist in 4.3, fall back to Strategy B (BajaScript). This affects which output format `greenmax-panel-gen.py` should produce.
- **B854-G1** (pre-requisite) — Confirm the exact folder path (`/Iluminacion/GM{n}` vs `/Config/Iluminacion/GM{n}`) and the real Hor{n}_HOA handles by reading a fresh station export after the Horarios folder is committed. Both are emitter input parameters and must be resolved before B859-G1 can run.
