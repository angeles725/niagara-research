# Block 840 — Operator field-wiring runbook: connecting a BACnet MS/TP Spyder to a JACE-8000 (terminal map, shield/`S`, bias switch decision) — the panel-level how-to distilled from B835/B839

> **Focus:** spyder-commissioning. **Mode:** document/capture (METHODOLOGY §20) — this block CONSOLIDATES
> the operator how-to worked out live this session into one panel-level runbook. It does **not** re-derive
> the reference facts; it cites **B835** (full MS/TP electrical + N4 workflow), **B839** (model-family
> routing + why the terminal label is authoritative), and the JACE-8000 wiring guide. Read those for the
> full detail; read THIS when you are standing at the panel with a screwdriver.
>
> **Sources:** FUENTE 1 — B835, B839. FUENTE 2 — `niagara-help/docs-text/JACE-8000_MtgWiringGuide_-TRI.txt`
> (RS-485 3-pos `−/+/S`, "shield to shield", bias switch values verbatim). All `[CERT-doc]` unless flagged.

## 0. First: confirm it IS a BACnet MS/TP Spyder `[CERT-hw]` (B839)

The bus is decided by the **terminal label**, not the model name:
- **`BAC+ / BAC-`** or **`C1+ / C1-`** on the Spyder ⇒ **BACnet MS/TP (RS-485)** ⇒ this runbook applies.
- A **2-pin, polarity-insensitive** plug + a LON/FTT-10 option module ⇒ **LON** ⇒ use **B837** instead, not this.
- The word "Enhanced"/"Sylk" describes the wall-module grade, NOT the JACE bus (B839 §5).

## 1. Terminal map — Spyder → JACE `[CERT-doc]`

| Spyder port (BACnet MS/TP) | → | JACE-8000 RS-485 (3-pos `−/+/S`) |
|---|---|---|
| `BAC+` (data +) | → | **`+`** (A+/NET+) |
| `BAC-` (data −) | → | **`−`** (A−/NET−) |
| `SHIELD` | → | **`S`** (shield terminal) |

Rule: **"minus to minus, plus to plus, shield to shield."** Never cross + and −.
(WEB-RS5N/RL6N variant: the third Spyder terminal is `GND` instead of `SHIELD`; it still lands on the JACE
`S`. Terminal NUMBERS vary by housing — trust the printed label, not a remembered number: RS5N 40/41/42,
RL6N 62/63/64, and a live "Sylk Enhanced" unit used 7/8/9 = `BAC+/BAC-/SHIELD`.)

## 2. Shield — lands on `S`, earthed at ONE end `[CERT-doc]`

Two separate rules people conflate:
1. **Shield-to-shield (continuity):** the cable shield is carried on the `S` terminals continuously across
   every node. The Spyder `SHIELD` **does connect** — to the JACE `S`. It is **not** left floating.
2. **Earth at ONE end only:** bond the shield to earth ground at a **single point** of the whole run
   ("for example at the controller"). Do not earth it at both ends → that creates a ground loop.

## 3. Bias switch — one 3-position switch per RS-485 port `[CERT-doc]`

The switch sets **bias** (forces a defined idle state on the pair; a bus-wide need — one node suffices) and
**termination** (a resistor at the physical ends that absorbs reflections; ends only) together:

| Position | Bias | Termination | Use when the JACE is… |
|---|---|---|---|
| **BIA** (default, mid) | 2.7 kΩ | none | …in the **middle**, and the trunk still needs biasing |
| **END** | 562 Ω | **150 Ω** | …at a **physical end** of a trunk not already biased |
| **MID** | 47.5 kΩ (≈none) | none | …in the **middle of an already-biased** trunk (don't over-bias) |

Decision: JACE is a **bus end?** → **END**. JACE **in the middle**, nobody biasing? → **BIA**. JACE
**in the middle**, another node already biases? → **MID**.

## 4. The common case: JACE ↔ ONE Spyder `[INFER]` (applying §1–§3)

A 2-node bus: JACE at one end, Spyder at the other.
1. Wire `BAC+→+`, `BAC-→−`, `SHIELD→S` with shielded twisted-pair 18–22 AWG (Belden 9842 or equivalent).
2. **JACE bias switch → END** (it is a physical end: bias + its internal 150 Ω termination).
3. **Terminate the Spyder end** with **120 Ω** across `BAC+`/`BAC-` (enable its EOL jumper/DIP if it has one,
   else fit a 120 Ω resistor). → both physical ends terminated, bus biased once.
4. Shield to earth **at the JACE only**.
5. Power JACE and Spyder from **separate transformers** (B835 §2/§3).
`[INFER]` marker: the per-step *composition* is inferred by applying the `[CERT-doc]` rules of §1–§3 to a
2-node topology; each underlying fact is cited. The JACE 150 Ω + cable 120 Ω at the two ends is intentional
(the JACE END value is internal and fixed).

## 5. Then commission (not electrical) → B835 §6

`BacnetNetwork` → add the **MS/TP port** on that COM → **baud 76800**, JACE as master → drag **BACnetSpyder**
from the `honeywellSpyderTool` palette → **Discover → Match → Full Download**. Module = `honeywellBacnetSpyder`.

## Self-verify

| # | Claim | Marker | Source |
|---|-------|--------|--------|
| 1 | Bus decided by terminal label; BAC+/BAC- or C1+/C1- ⇒ MS/TP | [CERT-hw]/[CERT-doc] | B839 §5 |
| 2 | JACE RS-485 = 3-pos `−/+/S`, `S`=shield; "shield to shield" | [CERT-doc] | JACE wiring guide §305–309 |
| 3 | `BAC+→+`, `BAC-→−`, `SHIELD→S`; never cross +/− | [CERT-doc] | §1 map + guide |
| 4 | Shield continuous on `S`, earthed at ONE end only | [CERT-doc] | guide §285/308–309 |
| 5 | BIA=2.7 kΩ/no term; END=562 Ω+150 Ω; MID=47.5 kΩ/no term | [CERT-doc] | guide §288–307 (B835 §2) |
| 6 | 2-node case: JACE END + Spyder 120 Ω, shield earthed at JACE | [INFER] | composition of §1–§3, flagged |
| 7 | Commission = BacnetNetwork→MS/TP→76800→BACnetSpyder→Discover→Match→Full Download | [CERT-doc] | B835 §6 |

**Tally:** 7 claims — 5 [CERT-doc], 1 [CERT-hw]/[CERT-doc], 1 [INFER] (2-node composition, flagged). No
unmarked assertions. **Left out:** the far-end Spyder EOL detail (jumper vs discrete 120 Ω) depends on the
specific model's board — confirm on the unit; multi-drop bus with 3+ Spyders (then JACE END at one end,
last Spyder EOL at the other, middle nodes no termination).

## Connections

- **B835** — the full MS/TP reference (power, terminals, cable/loading, N4 workflow); this runbook is its
  panel-level condensation, now enriched with the verbatim bias values + `S`/shield rule.
- **B839** — model-family routing + `[CERT-hw]` terminal-label authority; §0 here is its one-line gate.
- **B837** — the LON alternative, if the terminal check in §0 says FTT-10 instead.

## Open gaps

- (inherits **B839-G2** — whether "Spyder Enhanced" ships both LON and BACnet SKUs; and **B835-G4** — a live
  JACE probe for `[CERT-live]` values.)
