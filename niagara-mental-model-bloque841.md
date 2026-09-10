# Block 841 — HARBOR (HM_BMS + HM_Central): the GreenMAX lighting station and its current per-circuit schedule chain

> **Focus:** harbor-greenmax-lighting (new). **Scope:** the RE map of the HARBOR BMS as it exists in the
> two station backups the operator brought in — what the plant is, and **exactly how a lighting circuit is
> commanded today**, end to end, from the Supervisor schedule down to the physical GreenMAX relay. This is
> the evidence floor the redesign blocks (B842 concept, B843 custom-module route, B844 kitControl route,
> B845 HTML dashboard) build on.
>
> **Live context `[CERT-live]`:** the JACE **HM_Central** is the **same Distech EC-Net 4 Pro JACE-8000 on
> Baja 4.3.58.18** documented in **B838** (focus backup-licensing-ops). So "HARBOR" is that site. Supervisor
> station = **HM_BMS** (N4).
>
> **Sources (FUENTE 1 = the two bogs, read with `tools/bog-nav.py`; all evidence is `[CERT-live]` = verbatim
> from the operator's real station backups):**
> - `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/config.bog` (Supervisor; sha256 `4515de94…272e0`)
> - `clients/distech-merida-harbor/HM_Central_backup_07_09_2026/config.bog` (JACE; sha256 `04906eed…0db960`)
> - Commercial scope: `docs/Cotizacion_HARBOR_..._SEJOFA_Sin_Licencias.pdf` (Ingeniería SEJOFA). Registered in `sources/SOURCES.md`.

---

## 1. What the plant is `[CERT-live]`

Two Niagara stations, joined by **NiagaraNetwork** (each station proxies the other):

- **HM_BMS** (Supervisor, N4) — hosts the operator dashboards (`shared/px`, 106 `.px` files) and, importantly,
  **hosts and runs the lighting schedules**.
- **HM_Central** (JACE-8000, Distech EC-Net, 4.3.58.18) — the field controller. It talks to equipment over
  **BACnet** through a Distech stack, `BcpBacnetNetwork` (`bcs3:BcpBacnetNetwork`).

Plant inventory, read from `HM_Central/points` (76 proxy folders on the Supervisor side; devices on the JACE
side): **22 fan-coils (FC01–FC22)**, **20 air handlers (UMA01–UMA20, `bcs3:BcpBacnetDevice`)**, **3 chillers
(BACnet)**, ventiladores (GM*VENT, Sótano 1/2), cisterna, cárcamo, planta de emergencia, **PTAR**, monóxidos,
cuartos eléctricos — and the subject of this job: **16 GreenMAX lighting panels, `GM01ilum … GM16ilum`**, each
a `bac:BacnetDevice` on the JACE's `BcpBacnetNetwork` whose Binary Outputs (`Relay[k].BO`) drive the physical
relays. GreenMAX = the Wattstopper/Legrand relay-panel lighting system, integrated here over BACnet.

## 2. The lighting scale — why the client wants a redesign `[CERT-live]`

Per-panel circuit counts (BooleanSchedule count inside each `GMnnilum` proxy folder, Supervisor bog):

| Panel | Circuits | Panel | Circuits | Panel | Circuits | Panel | Circuits |
|-------|----------|-------|----------|-------|----------|-------|----------|
| GM01 | 14 | GM05 | 15 | GM09 | 16 | GM13 | 30 |
| GM02 | 20 | GM06 | 20 | GM010 | 32 | GM14 | 32 |
| GM03 | 7 | GM07 | 23 | GM011 | 26 | GM15 | 32 |
| GM04 | 20 | GM08 | 32 | GM012 | 12 | GM16 | 13 |

**~330+ lighting circuits across the 16 panels**, and the JACE's BACnet side exposes **412 `Relay[k].BO`**
Binary Outputs total. The decisive fact: **today each circuit has its own dedicated `BooleanSchedule`.** That
is ~330 separate schedule objects an operator must open and edit one by one — precisely the pain the client
named ("no tener que ir uno por uno"). *(User said GM04 = "24 iluminaciones"; the bog shows 20 BooleanSchedules
— the physical panel likely has 24 relay positions with 20 in use. See gap B841-G1.)*

## 3. The command chain of one circuit, end to end `[CERT-live]`

Traced with `bog-nav path/handle` for GM01 R1 across both bogs:

```
[Supervisor HM_BMS]
  Drivers/NiagaraNetwork/HM_Central/points/GM01ilum/
      Schedule1  (sch:BooleanSchedule, runs HERE)
        .out ──dataLink──▶ SchdlGM01 R1 (c:BooleanWritable) .in10      ← priority 10 = schedule default
                                              │
                              (JACE subscribes to this point over NiagaraNetwork)
                                              ▼
[JACE HM_Central]
  Drivers/NiagaraNetwork/HM_BMS/points/GreenMAX1/SchdlGM01 R1 (c:BooleanPoint, read-back)
        .out ──dataLink──▶ Drivers/BcpBacnetNetwork/GM01ilum/points/Relay[1].BO .in16   ← priority 16 = fallback
                                              │
                                              ▼
                            physical GreenMAX relay 1 (BACnet Binary Output)
```

Confirmed facts that drive the redesign:
- **The BooleanSchedules execute on the Supervisor** (they are real `sch:BooleanSchedule` components with
  `.out → .in10` links present in the Supervisor bog), not on the JACE. **→ the schedule redesign belongs on
  the Supervisor**; the JACE↔BACnet plumbing stays untouched.
- **`SchdlGMnn Rk` is a plain local `BooleanWritable`** — it has `in10` (schedule) and `in16` (fallback `true`),
  a full 16-level **priority array**, and **no proxyExt / no outgoing link in the Supervisor bog**. Its value
  reaches the field only because the **JACE reads it back** and re-emits it to BACnet.
- The physical relay is a **BACnet Binary Output** written from the JACE. **Correction / refinement (matrix
  extraction 2026-09-09, `tools/greenmax-matrix.py`):** the write priority is **NOT uniform** — GM01 (the old
  panel) writes the BO at `in16` (fallback), but **329 of ~349 circuits (GM02–GM16) write at `in10`**; 1 at
  `in9`; 13 undriven. So the earlier "commanded at in16" holds only for GM01. Either way higher priorities
  remain free for a **manual override** (write a higher priority on the Supervisor writable or the JACE BO).
- `Relay[k].BO` on the **Supervisor** side is a read-only `c:BooleanPoint` (status mirror); the writable side
  is `Relay[k].BO` (BACnet BO) on the **JACE**.

**Two panel generations (matrix extraction, `[CERT-live]`):** GM01 uses the OLD pattern — `Schedule{k}` →
`SchdlGM01 R{k}` → BACnet `in16`. **GM02–GM16 use a NEWER, richer pattern** — schedules `R{k}` → writables
`HorR{k}` → BACnet `in10`, **plus `labelR{k}` / `tabR{k}` `kitControl:StringConst` holding the per-circuit
DESCRIPTION and tablero in the station itself** (e.g. GM02 R1 label = "Sótano1 N1 a N11", tab = "TAB AL-6").
This means **the matriz de puntos is already ~90% digitized**: `greenmax-matrix.py` extracted **312 of 349
circuits with descriptions** straight from the two bogs. Only GM01 (old, no labels) + a few "Pendiente"
entries need filling from the Px labels or the client's cuadro de cargas. Output:
`clients/distech-merida-harbor/docs/matriz-puntos-DRAFT.csv`. (Closes most of B841-G1.)

## 4. Where the pieces to reuse already are `[CERT-live]`

- **Schedules to consolidate**: the ~330 per-circuit `BooleanSchedule`s under `HM_Central/points/GMnnilum/` on
  the Supervisor.
- **Write targets**: the `SchdlGMnn Rk` `BooleanWritable`s (Supervisor) — the redesign re-feeds these; the
  JACE→BACnet path downstream needs no change.
- **Dashboards**: `HM_BMS/shared/px` has one `.px` per panel (`GreenMAXnn TabALxx.px`) plus `GreenMAX
  Iluminacion.px` (overview) and **`plantilla greenmax.px`** (a template) — the client edits these in **PX
  Editor**. The JACE also has `HorGMnn` folders (Hor = Horario), currently thin — a prior/aborted attempt at
  consolidation (gap B841-G2).

## 5. Commercial frame `[CERT-live]` (SEJOFA proposal, `docs/…SEJOFA…pdf`)

$63,000 MXN + IVA, **remote** work, 2–3 weeks, 90-day warranty. Scope: "GreenMAX 16 tableros, horarios,
**Selector Manual/Horario** y control **ON/OFF manual por salida independiente**", dashboards, final backup +
basic operation guide. Client provides licenses/Host ID and the **matriz de puntos**; wiring and point mapping
are excluded. **Scope note:** the proposal's selector is **2-state (Manual/Horario) per output**; the operator's
verbal ask is richer — **4 master schedules + a 4-way selection per panel and per circuit + override**. That
delta is designed in B842 and should be confirmed as in-scope or a priced extension (proposal term 6).

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | HARBOR = HM_BMS Supervisor + HM_Central JACE joined by NiagaraNetwork | [CERT-live] | both bogs, `bog-nav tree`; NiagaraNetwork/HM_Central + NiagaraNetwork/HM_BMS |
| 2 | HM_Central = Distech EC-Net JACE-8000 4.3.58.18 (same as B838) | [CERT-live] | B838 live context; console/config of HM_Central backup |
| 3 | JACE field bus = BACnet via `bcs3:BcpBacnetNetwork` | [CERT-live] | HM_Central bog, `find --type bcs3:BcpBacnetDevice` (UMA*, GM*ilum) |
| 4 | 16 GreenMAX lighting panels GM01ilum–GM16ilum, each a bac:BacnetDevice | [CERT-live] | both bogs; per-panel folders + BACnet devices |
| 5 | Per-panel circuit counts (table §2); ~330+ circuits; 412 BACnet BOs | [CERT-live] | grep of BooleanSchedule per GM folder; Relay[k].BO count on JACE |
| 6 | Today = one dedicated BooleanSchedule per circuit | [CERT-live] | GM01 slot: Schedule1..14 + SchdlGM01 R1..14; pattern across panels |
| 7 | Chain: Schedule.out→writable.in10 (Supervisor) → JACE reads back → BO.in16 (BACnet) | [CERT-live] | bog-nav handle h:225dc/h:227d4 (Supervisor), h:21d94/h:21d96 (JACE) |
| 8 | Schedules RUN on the Supervisor (real components + links there) | [CERT-live] | Supervisor bog holds sch:BooleanSchedule + dataLink to in10 |
| 9 | SchdlGMnn Rk = plain BooleanWritable, in16 fallback, no proxyExt/no out-link | [CERT-live] | Supervisor bog XML of h:227d4 (in10, in16, one Link from Schedule) |
| 10 | Relay BO write priority NOT uniform: GM01=in16, 329 circuits (GM02–16)=in10, 1=in9, 13 undriven; higher priorities free for override | [CERT-live] | greenmax-matrix.py link survey (JACE bog): in10×329, in16×6, in9×1 |
| 13 | Two panel generations (GM01 old Schedule/SchdlGM Rk/in16 vs GM02–16 new R/HorR/in10 + labelR/tabR StringConst descriptions); 312/349 circuits described in-station | [CERT-live] | greenmax-matrix.py over both bogs; GM02 labelR1="Sótano1 N1 a N11" |
| 11 | Dashboards = per-panel .px + template in shared/px; edited in PX Editor | [CERT-live] | `ls HM_BMS/shared/px` (106 files incl. plantilla greenmax.px) |
| 12 | SEJOFA proposal: $63k MXN, remote, selector Manual/Horario per output | [CERT-live] | the PDF, pp.1–2 |

**Tally:** 13 claims — all [CERT-live] (verbatim from the two station backups + the proposal PDF; claims 10 & 13 refined 2026-09-09 by `tools/greenmax-matrix.py`). No unmarked assertions, no [INFER].

## Connections
- **B838 (backup-licensing-ops)** — same JACE HM_Central (Distech EC-Net 4.3.58.18); backup/restore/licensing procedure for this exact controller.
- **B842** — the unified control-logic design (4 master schedules + per-panel/per-circuit selector + override) common to both build routes.
- **B843 / B844** — the two implementation routes (custom N4 module vs kitControl-only) that realize B842.
- **B845** — HTML+CSS+JS dashboard feasibility for the operator surface.
- **B9 (UI stack)** — Px / HxProfile / BWebServlet background for B845.

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B841-G1** — Exact physical relay count per panel from the client's **matriz de puntos** (bog BooleanSchedule count ≠ necessarily wired relays; GM04 shows 20 vs the "24" the operator recalled).
- **B841-G2** — What the JACE `HorGMnn` folders currently hold and whether they are a reusable consolidation seam or dead scaffolding.
- **B841-G3** — Confirm the writable naming per panel (only GM01 verified as `SchdlGM01 Rk`; other panels may name their command writables differently) — matters for the re-feed step in B843/B844.
