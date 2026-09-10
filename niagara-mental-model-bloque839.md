# Block 839 — Spyder model-family decode (Spyder II / Enhanced / Micro / Bacnet) and JACE bus routing — CAUTION: the family NAME does not reliably imply the bus; the terminal label does

> **[CERT-hw] CORRECTION (2026-09-08).** This block first concluded "'Spyder Sylk Enhanced' → LON." A LIVE
> unit the operator identified as a "Spyder Sylk Enhanced" presented **BACnet MS/TP** terminals labeled
> **`BAC+`(7) / `BAC-`(8) / `SHIELD`(9)** — i.e. RS-485, NOT LON. Per the contradiction rule (empirical live
> system > doc), the **physical terminal label is authoritative**. The N4.14 `honeywellLonSpyder` help lists
> the "Spyder Enhanced" part numbers (`PVL/PUL…ES`) under LON (§1, still `[CERT-doc]`), but the marketing
> family name is NOT a safe proxy for the field bus: a BACnet-comms "Enhanced" variant exists (or the help
> family list is version-stale). **Decide the JACE bus from the model number's 3rd letter AND, above all,
> from the terminal labels on the unit — never from the word "Enhanced" alone.** See §5.

> **Focus:** spyder-commissioning. **Gap:** operator asked how to wire a JACE-8000 to "a Spyder Sylk
> Enhanced." The corpus had the two JACE↔Spyder buses (B835 = BACnet MS/TP, B837 = LON/FTT-10) but NOT the
> mapping from the marketing family name **"Spyder Enhanced"** to a physical bus. This block certifies that
> mapping and CORRECTS a first-pass session answer that assumed MS/TP.
>
> **Scope:** (1) the four Honeywell Spyder model families and their part-number decode; (2) which bus each
> family speaks to the JACE; (3) where "Sylk / S-Bus" fits (it is the wall-module bus, off the Spyder, not
> the JACE); (4) the routing rule that picks B835 vs B837 from the model number on the label.
>
> **Sources (all three):**
> - FUENTE 2 (niagara-help): `guides/HoneywellSpyder/honeywellLonSpyder-LonAlarmsView.html` (the verbatim
>   family→model lists, L174-181) — a **`honeywellLonSpyder`** (LON driver) guide; and
>   `docs-text/Spyder_PUL-PUB-PVB-PVL_Controllers_User_Guide_-_63-2662.txt` (one Programming Tool covers both
>   LON and BACnet controllers; S-Bus WM + Sylk actuator sections). Hashes in SOURCES.
> - FUENTE 1 (corpus): **B835** (BACnet MS/TP path), **B837** (LON/FTT-10 path), B77 (`BLonSpyder`/`BBacnetSpyder`
>   driver split), B121 (TR wall-module Sylk/S-Bus tool-side).
> - FUENTE 3 (code): `organized/honeywellLonSpyder/.../module.xml` (`BLonSpyder`, deps `lonworks-rt/wb`) —
>   already registered by B837.

## 1. The four Spyder model families `[CERT-doc]`

Verbatim from `honeywellLonSpyder-LonAlarmsView.html` (L174-181):

| Family | Models (verbatim) | Guide that lists them |
|--------|-------------------|-----------------------|
| **Spyder II** | `PVL6436AS`, `PVL6438NS`, `PUL6438S` | honeywellLonSpyder (LON) |
| **Spyder Enhanced** ← this block | `PVL6436AES`, `PVL6438NES`, `PUL6438ES` | honeywellLonSpyder (LON) |
| **Spyder Micro** | `PVL4024NS`, `PVL4022AS`, `PUL4024S`, `PUL1012S`, `PVL0000AS` | honeywellLonSpyder (LON) |
| **Spyder Bacnet** | `PVB6436AS`, `PVB6438NS`, `PUB6438S` | (BACnet) |

The alarms guide groups them repeatedly as "**LonSpyder I, Spyder II, Spyder Enhanced,** and **BacnetSpyder**
models" (L219/221/230/232) — i.e. the LON families (LonSpyder I / II / Enhanced / Micro) are named separately
from the one **BacnetSpyder** family. "Spyder Enhanced" sits on the **LON** side of that split.

## 2. Part-number decode `[INFER]` (from the family lists) + `[CERT-doc]` anchor

The prefix pattern across all four verbatim lists resolves cleanly:

- **P** = Programmable Spyder.
- **2nd letter** = form factor: **V** = VAV, **U** = Unitary.
- **3rd letter = the field bus to the JACE:** **L = LonWorks (FTT-10)**, **B = BACnet (MS/TP)**.
  Every "Bacnet" model carries `B` in position 3 (`PVB…`, `PUB…`); every LON family carries `L`
  (`PVL…`, `PUL…`). This is the load-bearing bit.
- **Suffix** = wall-module / feature grade: `…S` (Sylk/S-Bus base), **`…ES` = "Enhanced Sylk"** (the
  Enhanced family), `A`/`N` = analog/… variant of the box.

`[INFER]` marker note: the letter *decode* is inferred from the four verbatim family lists (§1), which are
`[CERT-doc]`; the doc does not print a nomenclature key. It is strongly supported (100% consistent across the
15 listed part numbers) but not verbatim — confirm against the model-number key on a Honeywell datasheet if a
part ever breaks the pattern.

## 3. "Sylk" / "S-Bus" is the WALL-MODULE bus, not the JACE bus `[CERT-doc]`

The "**Sylk**" in "Sylk Enhanced" names the 2-wire bus between the Spyder and its **wall module / actuator**,
NOT the link to the JACE:

- The alarms guide's `S-BUS WM Communication Alarm` / `S-BUS WM Fail Detect` (L160-163, 237-247) are about the
  **S-Bus Wall Module** link "between the SbusWallModule and the controller" — i.e. downstream of the Spyder.
- The controllers guide (63-2662) has both an **S-BUS WALL MODULE** configuration section (§L146, figs
  226-231) and a **SYLK BUS / Sylk-enabled actuator** section (§L254-256, figs 370-380). Honeywell uses
  "Sylk" and "S-Bus" for the same 2-wire, polarity-insensitive wall-module bus family.
- This matches B835 §3 (Spyder MS/TP terminals) and B821-style wall-module notes: the Sylk terminals
  (`WM1/WM2` on the BACnet units) "hang off the Spyder, not the JACE."

**Consequence:** the "Sylk Enhanced" grade tells you about the wall-module capability. It does **not** decide
the JACE connection — the **3rd letter of the model number** does.

## 4. Routing rule — which block wires the JACE `[CERT]` (B835/B837)

```
Read the model number on the controller label:
  P_L…  (e.g. PVL6438NES  = Spyder Enhanced)  → LON / FTT-10   → wire per B837
  P_B…  (e.g. PVB6438NS   = Spyder Bacnet)    → BACnet MS/TP    → wire per B835
```

So a **"Spyder Sylk Enhanced" (PVL/PUL…ES) → LON.** The JACE connection is:

- **Option module:** **NPB-8000-LON (part 12978)**, an FTT-10A adapter with a 2-position screw plug — seated
  in one of the JACE-8000's 4 option slots. Power the JACE **down** to seat it. (B837 §1)
- **Bus:** FTT-10A twisted pair, **26–12 AWG**, **polarity-insensitive**, **free topology** (bus/star/loop).
  No RS-485-style "120 Ω at both ends" — FTT-10 uses network-level termination per Echelon (B837 §2;
  termination detail is `[INFER]`/open as B837-G1). (B837 §2)
- **Commissioning:** license `lonworks`; add a **LonNetwork** under `Config > Drivers`; drag **`LonSpyder`**
  from the `honeywellSpyderTool` palette; **Discover** or press the physical **service pin** (48-bit Neuron
  ID); **Match**. (B837 §3)

## 5. Ground truth: read the terminal label, not the family name `[CERT-hw]`

The routing rule in §4 is a *hint from the catalog*, not the final word. The **terminal label on the unit is
authoritative**:

- A live unit the operator called a "Spyder Sylk Enhanced" had **`BAC+`(7) / `BAC-`(8) / `SHIELD`(9)** — a
  **BACnet MS/TP (RS-485)** port. So that specific unit wires per **B835** (MS/TP), NOT B837 (LON), despite
  the "Enhanced" name. The terminal numbers (7/8/9) differ from B835's WEB-RS5N (40/41/42) / WEB-RL6N
  (62/63/64) — a different housing; our N4.14 docs do not carry the 7/8/9 map, so the operator's label stands.
- **MS/TP wiring for a `BAC+/BAC-/SHIELD` unit:** the JACE-8000 RS-485 port is a **3-position** screw plug
  **`−` / `+` / `S`**, where **`S` is the shield terminal** (`[CERT-doc]` JACE-8000_MtgWiringGuide_-TRI:
  *"'minus to minus', 'plus to plus,' and 'shield to shield.' Connect the shield wire to earth ground at one
  end only, for example at the controller"*). So: `BAC+` → **`+`(A+)**; `BAC-` → **`−`(A-)**; **`SHIELD` →
  `S`** (do NOT leave it floating — the operator asked; the answer is it lands on `S`). Two separate rules:
  (1) *shield-to-shield* = the shield is carried on the `S` terminals continuously across the bus; (2)
  *earth at ONE end only* = the shield is bonded to earth ground at a single point (typically the JACE), not
  at both ends → no ground loop. JACE bias **BIA** (mid-bus) or **END** (physical end, adds 150 Ω term);
  **120 Ω** at the two physical ends of the segment; STP 18–22 AWG. Commission via `BacnetNetwork` → MS/TP
  port → baud **76800** → `BACnetSpyder` → Discover → Match → Full Download (B835 §6). `honeywellBacnetSpyder`.
  Note the WEB-RS5N/RL6N third terminal is `GND` (signal reference, B835 §3) whereas this unit's is `SHIELD`
  (cable drain); both land on the JACE `S` terminal.
- A `C1+/C1-/GND` label = the WEB-RS5N/RL6N MS/TP layout (B835 §3). A **2-position polarity-insensitive**
  plug with no +/- and a LON/FTT-10 module = the LON path (B837).

**Takeaway:** the word "Enhanced" describes the Sylk wall-module grade, not the JACE bus. Confirm the bus from
the printed terminal labels (`BAC+/BAC-` or `C1+/C1-` ⇒ BACnet MS/TP ⇒ B835; 2-pin no-polarity FTT-10 ⇒ LON
⇒ B837). The earlier session reply and this block's original title both over-trusted the family name.

## Self-verify

| # | Claim | Marker | Source |
|---|-------|--------|--------|
| 1 | Four families: Spyder II, Enhanced, Micro (LON) + Spyder Bacnet | [CERT-doc] | LonAlarmsView L174-181 |
| 2 | "Spyder Enhanced" models = PVL6436AES / PVL6438NES / PUL6438ES | [CERT-doc] | LonAlarmsView L176 |
| 3 | Enhanced is documented under `honeywellLonSpyder` and grouped with the LON families, distinct from BacnetSpyder | [CERT-doc] | LonAlarmsView L219-232 |
| 4 | Part-number 3rd letter: L=LON, B=BACnet (V=VAV, U=Unitary; ES=Enhanced Sylk) | [INFER] | derived from §1 verbatim lists (15/15 consistent) |
| 5 | Sylk / S-Bus = wall-module bus off the Spyder, not the JACE link | [CERT-doc] | LonAlarmsView L160-163,237-247; 63-2662 §S-Bus/Sylk |
| 6 | One Programming Tool (63-2662) covers both LON and BACnet Spyders | [CERT-doc] | 63-2662 TOC (BACnet + Lon controller add) |
| 7 | An `P_L…` model + FTT-10 module → JACE via NPB-8000-LON (LON path) | [CERT] | B837 §1-3 |
| 8 | An `P_B…` model + `BAC+/BAC-`/`C1+/C1-` port → JACE via RS-485 MS/TP | [CERT] | B835 §3 |
| 9 | A live "Spyder Sylk Enhanced" unit had `BAC+`(7)/`BAC-`(8)/`SHIELD`(9) = BACnet MS/TP; terminal label overrides the family name | [CERT-hw] | operator-reported live unit, 2026-09-08 |

**Tally:** 9 claims — 6 [CERT-doc], 2 [CERT] (via B835/B837), 1 [CERT-hw] (live terminal labels — highest
rank, overrides the family-name inference), 1 [INFER] (part-number decode, explicitly flagged). No unmarked
assertions. **Left out:** the 7/8/9 terminal map is not in our N4.14 docs (operator label stands); the full
model-number key from a Honeywell datasheet (B839-G1); whether "Enhanced" has both LON and BACnet SKUs
(B839-G2, opened below).

## Connections

- **B835** — the BACnet MS/TP JACE↔Spyder wiring; applies to `P_B…` (Spyder Bacnet) only. This block is the
  routing gate that says when B835 does **not** apply.
- **B837** — the LON/FTT-10 JACE↔Spyder path; the actual wiring for a "Spyder Sylk Enhanced" (`P_L…ES`).
- **B77** — the driver split `BLonSpyder` vs `BBacnetSpyder` that mirrors the L/B part-number letter.
- **B121** — the TR wall-module tool-side (Sylk/S-Bus), i.e. what the "Sylk" grade drives downstream.

## Open gaps

- **B839-G1** — verbatim Honeywell datasheet model-number key (to promote §2 claim 4 from [INFER] to
  [CERT-doc]). Low priority.
- **B839-G2** — does the "Spyder Enhanced" line have BOTH a LON (`P_L…ES`) and a BACnet SKU? The N4.14
  `honeywellLonSpyder` help lists Enhanced only under LON, but a live unit named "Sylk Enhanced" had BACnet
  MS/TP terminals (§5, [CERT-hw]). Resolve against a current Honeywell datasheet / ordering guide. Opened by
  the live contradiction; the family name is NOT a reliable bus proxy until this is closed.
- (inherits **B837-G1** — FTT-10 termination from the Echelon guide, not in our sources.)
