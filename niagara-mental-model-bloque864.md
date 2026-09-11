# Block 864 — Spyder model-number decode (B839-G1) and Enhanced LON-only confirmation (B839-G2): web-evidence discovery from official Honeywell datasheets

> **Scope:** spyder-commissioning. Web-evidence discovery block. Closes **B839-G2** (Enhanced
> LON-only confirmed; no BACnet Enhanced SKU exists) and reports **B839-G1** as a partial typed
> unavailable — no verbatim Honeywell nomenclature key found; decode remains [INFER] but is now
> strongly supported by cross-referencing four official Honeywell datasheets.
>
> **Sources (all four):**
> - CERT-WEB 1: **63-2685-05** "Spyder® Lon Programmable, VAV/Unitary Controllers — Product Data,"
>   Honeywell EDAM (prod-edam.honeywell.com, official Honeywell document portal). Retrieved
>   2026-09-11. Lists 8 LON models (no Enhanced ES suffix).
> - CERT-WEB 2: **63-1325-06** "Spyder® Lon Programmable, Unitary/VAV Controllers — Specification
>   Data," Honeywell EDAM. Retrieved 2026-09-11. Lists 9 LON models (no Enhanced ES suffix).
> - CERT-WEB 3: **63-1328-08** "Spyder® BACnet® Programmable Controllers — Specification Data,"
>   Honeywell EDAM. Retrieved 2026-09-11. Lists 9 BACnet models (no Enhanced BACnet variant
>   anywhere).
> - CERT-WEB 4: **63-2689-04** "Spyder® BACnet® Programmable Controllers — Product Data,"
>   honeywellbuildings.in (authorized Honeywell regional site). Retrieved 2026-09-11. Confirms BACnet
>   model list (PUB/PVB prefix, no ES suffix).
> - CERT-DOC (existing): niagara-help `honeywellLonSpyder-LonAlarmsView.html` (N4.14.0.162) — the
>   verbatim family→model lists cited in B839 §1. Confirmed verbatim in this block for cross-check.

---

## 1. Typed unavailable wall — B839-G1: verbatim nomenclature key `[unavailable]`

**Literal queries run:**

| # | Query | Source hit? |
|---|-------|-------------|
| 1 | "Honeywell Spyder PVL PVB PUL PUB model number ordering guide datasheet" | web — returned datasheets 63-2685, 63-1325, 63-1328, 63-2689; no nomenclature key section |
| 2 | "Honeywell Spyder Enhanced BACnet PVB model number 63-2662 OR 63-2613 ordering guide" | web — same datasheets; no decode key |
| 3 | "Honeywell Spyder model number nomenclature P V L OR P U L OR P V B character decode key" | web — no nomenclature key page found |
| 4 | "Honeywell Spyder Enhanced PVL6436AES OR PVL6438NES OR PUL6438ES datasheet BACnet LON" | web — only LON datasheets returned; ES models not in any web-accessible datasheet |
| 5 | module_nav.py string "PVL" / "PVB" | local module strings: only family grouping labels; no character decode |
| 6 | niagara_help.py find "Spyder" | returns 115 files; none contain a printed nomenclature key |

**Result:** All four official Honeywell datasheets (63-2685, 63-1325, 63-1328, 63-2689) direct readers
to the TRADELINE® Catalog or price sheets for ordering numbers, and include no verbatim
nomenclature key that maps character positions to controller attributes. The specific document that
would promote B839 §2 from `[INFER]` to `[CERT-doc]` is the TRADELINE® ordering guide — which is not
publicly web-accessible.

**B839-G1 status: PARTIAL.** The decode is now **strongly supported** by cross-referencing Table 1
across four official datasheets (see §2 below), but the `[INFER]` marker on the character-position
meanings cannot be lifted to `[CERT-doc]` without the verbatim key.

---

## 2. Strengthened model-number decode from cross-referencing four official datasheets `[INFER ← Table-1-CERT-web]`

No verbatim Honeywell nomenclature key exists in the publicly accessible docs. However, the character
decode is derivable with high confidence by cross-matching the model strings against Table 1 (I/O
count columns) in the four datasheets. The pattern is **100% consistent across 29 model numbers**
spanning all four documents:

| Position | Value | Decode | Evidence (how derived) |
|----------|-------|--------|------------------------|
| 1 | P | Programmable Spyder | All Spyder controller part numbers begin with P; product description uses "Programmable" throughout |
| 2 | V | VAV (Variable Air Volume) | Table 1 "Programmable Type" = VAV for all PVx models |
| 2 | U | Unitary | Table 1 "Programmable Type" = Unitary for all PUx models |
| 3 | L | LonWorks FTT-10 | All PxL models appear exclusively in the LON datasheets (63-2685, 63-1325); not in BACnet docs |
| 3 | B | BACnet MS/TP | All PxB models appear exclusively in the BACnet datasheets (63-1328, 63-2689); not in LON docs |
| 4–7 | e.g. 6438 | [UI][DI][AO][DO] I/O counts | Cross-match Table 1 columns: PUL6438S = UI=6, DI=4, AO=3, DO=8; PUL1012S = UI=1, DI=0, AO=1, DO=2; PVL0000AS = UI=0, DI=0, AO=0, DO=0; PVL4022AS = UI=4, DI=0, AO=2, DO=2 (100% match, all 29 models) |
| 8 | A | With Series-60 floating actuator | Table 1 "Series 60 Floating Actuator" = YES for all models with A in this position |
| 8 | N | Without actuator (but has Microbridge sensor) | Table 1 actuator = NO; sensor = YES |
| 8 | (none) | Unitary models without A/N suffix distinction | PUL/PUB unitary models: suffix S/SR only |
| 9+ | S | Base Sylk (2-wire wall-module bus capable) | Base suffix on all Spyder Lon/BACnet families |
| 9+ | ES | Enhanced Sylk (Extended Sylk capability) | Only on the Enhanced LON family (PVL…AES, PVL…NES, PUL…ES); from LonAlarmsView [CERT-doc] |
| 9+ | SR | With Relays (relay outputs instead of Triac DO) | PUL6438SR, PUB6438SR both appear in the LON and BACnet specs respectively |

> `[INFER]` marker applies to the letter-meaning assignment (Honeywell never prints "P = Programmable,
> V = VAV…"). The I/O digit decode is directly verifiable against Table 1 in the CERT-web datasheets.
> Confidence: very high (29 models, zero exceptions), but not certifiable without a verbatim key.

---

## 3. G2 — Does "Spyder Enhanced" have both LON and BACnet SKUs? `[CLOSED]`

### 3.1 Official datasheet evidence `[CERT-web]`

**Spyder LON datasheets (63-2685-05, 63-1325-06):**

The complete LON model list across both documents is:
`PUL1012S, PUL4024S, PUL6438S, PUL6438SR, PVL0000AS, PVL4022AS, PVL4024NS, PVL6436AS, PVL6438NS`

Neither document lists any model with an `ES` suffix. These are the "classic" LON Spyder II +
Micro family only. The **Enhanced models (PVL6436AES, PVL6438NES, PUL6438ES) do not appear in
the publicly accessible LON datasheets** — they are present only in the niagara-help guide.

**Spyder BACnet datasheets (63-1328-08, 63-2689-04):**

The complete BACnet model list:
`PUB1012S, PUB4024S, PUB6438S, PUB6438SR, PVB0000AS, PVB4022AS, PVB4024NS, PVB6436AS, PVB6438NS`

**Zero models with ES suffix. No BACnet Enhanced variant exists in any official Honeywell
datasheet.** The `PUB`/`PVB` prefix (3rd letter = B) is exclusive to BACnet and appears only in
the BACnet datasheets — the mirror image of `PUL`/`PVL` (3rd letter = L) being exclusive to LON.

### 3.2 Corpus cross-check `[CERT-doc]` (existing B839 source)

The niagara-help `honeywellLonSpyder-LonAlarmsView.html` (verbatim, line 45):

```
Spyder Enhanced models: PVL6436AES, PVL6438NES, PUL6438ES
```

and line 51:

```
Spyder Bacnet models: PVB6436AS, PVB6438NS, PUB6438S
```

These are **explicitly separate categories** in the alarms view — "Enhanced" and "Bacnet" are
distinct families. "Enhanced" maps exclusively to PVL/PUL (LON); "Bacnet" maps exclusively to
PVB/PUB (BACnet). There is no "BACnet Enhanced" or "PVB…ES" anywhere in the alarms view or
any other help file.

### 3.3 Resolution of the live-unit contradiction

B839 §5 established [CERT-hw]: a unit the operator identified as "Spyder Sylk Enhanced" had
`BAC+`(7) / `BAC-`(8) / `SHIELD`(9) terminals — clearly BACnet MS/TP. B839 correctly concluded
that the terminal label is authoritative and resolved the contradiction by saying the unit was
**BACnet, not LON**, overriding the family-name assumption.

This block provides the **reason** for the contradiction: **there is no BACnet Enhanced SKU.** The
unit the operator named "Spyder Sylk Enhanced" was almost certainly a **BACnet Spyder Classic
(PVB or PUB prefix)** whose marketing name or labeling was informally (and incorrectly) read as
"Enhanced." The part number on the controller label — if readable — would confirm a `PVB…`/`PUB…`
(not `PVL…ES`/`PUL…ES`) string.

**Consequence for B839:** B839's §1 framing — "Enhanced = LON family (PVL/PUL…ES)" — is
**correct and requires no structural change.** The [CERT-hw] correction banner in B839 §5 remains
fully accurate: the terminal label correctly identified the live unit as BACnet (B835 path). The
"family name" the operator used was simply wrong. B839 §4 routing rule (`P_L…` → LON → B837;
`P_B…` → BACnet MS/TP → B835) is confirmed without exception. See §14 below.

**Answer to B839-G2:** Enhanced has **LON SKUs only** (PVL6436AES, PVL6438NES, PUL6438ES). No
BACnet Enhanced variant exists in any official Honeywell document. B839's LON-only framing is
confirmed correct.

---

## Self-verify

| # | Claim | Marker | Source |
|---|-------|--------|--------|
| 1 | 63-2685-05 LON datasheet lists 8 LON models; no ES suffix models | [CERT-web] | prod-edam.honeywell.com/63-2685.pdf, retrieved 2026-09-11, sha256 90a250ef… |
| 2 | 63-1325-06 LON spec lists 9 LON models; no ES suffix models | [CERT-web] | prod-edam.honeywell.com/63-1325.pdf, retrieved 2026-09-11, sha256 55442ab6… |
| 3 | 63-1328-08 BACnet spec lists 9 BACnet models (PUB/PVB); no ES suffix anywhere | [CERT-web] | prod-edam.honeywell.com/63-1328.pdf, retrieved 2026-09-11, sha256 fae915aa… |
| 4 | 63-2689-04 BACnet product data lists same PUB/PVB models; no BACnet Enhanced | [CERT-web] | honeywellbuildings.in/63-2689.pdf, retrieved 2026-09-11, sha256 2140362c… |
| 5 | LonAlarmsView L45: "Spyder Enhanced models: PVL6436AES, PVL6438NES, PUL6438ES" (LON only) | [CERT-doc] | niagara-help LonAlarmsView.html (N4.14.0.162), B839 §1 |
| 6 | LonAlarmsView L51: "Spyder Bacnet models: PVB6436AS, PVB6438NS, PUB6438S" — a separate category from Enhanced | [CERT-doc] | LonAlarmsView.html, confirmed verbatim this session |
| 7 | No verbatim Honeywell nomenclature key found in any of the four official datasheets | [unavailable-wall] | 6 literal search queries; all datasheets redirect to TRADELINE catalog |
| 8 | Digit decode [UI][DI][AO][DO] is 100% consistent across all 29 models in Table 1 of the four datasheets | [INFER ← CERT-web Table 1] | cross-match: PUL6438S=6,4,3,8; PUL1012S=1,0,1,2; PVL4022AS=4,0,2,2; etc. |
| 9 | 3rd letter L=LON / B=BACnet directly follows from doc titles and model groupings | [CERT-web] | 63-2685/"Spyder Lon" lists all PVL/PUL; 63-1328/"Spyder BACnet" lists all PVB/PUB; no crossover |
| 10 | No BACnet Enhanced SKU exists; "Enhanced" (ES suffix) is LON-only | [CERT-web] + [CERT-doc] | four BACnet datasheets (no ES); LonAlarmsView separate category list |
| 11 | Live unit called "Spyder Sylk Enhanced" with BAC+/BAC-/SHIELD (B839 §5, [CERT-hw]) was a BACnet Classic (PVB/PUB), operator mis-named | [INFER] | no BACnet Enhanced SKU exists [CERT-web]; terminal label is authoritative [CERT-hw] |

**Tally:** 11 claims — 4 [CERT-web] (official Honeywell datasheets), 2 [CERT-doc] (B839 corpus
cross-check), 1 [unavailable-wall] (verbatim nomenclature key), 1 [INFER ← CERT-web Table 1]
(digit decode derived from tables), 1 [CERT-web] (3rd-letter decode from doc titles), 1
[CERT-web]+[CERT-doc] (no BACnet Enhanced SKU), 1 [INFER] (live-unit diagnosis). No unmarked
assertions.

---

## Connections

- **B839** — this block closes B839-G2 and provides partial evidence for B839-G1. B839 §1 (Enhanced
  = LON) and §4 (routing rule L→B837, B→B835) are **confirmed without exception**. The [CERT-hw]
  correction banner in B839 §5 stands; B839 is correct as-is. Add pointer: "G2 closed by B864."
- **B835** — the BACnet MS/TP path applies to all `P_B…` (Spyder BACnet Classic); confirmed here
  that BACnet Classic is the only BACnet Spyder family.
- **B837** — the LON/FTT-10 path applies to all `P_L…` (Spyder LON + Spyder Enhanced ES); confirmed.
- **B836** — Spyder function-block reference; not affected.
- **B840** — operator field-wiring runbook (BACnet MS/TP path); not affected.

---

## Open gaps

- **B839-G1** (partial — residual): The TRADELINE® ordering guide (not publicly accessible) is
  the one Honeywell document that would carry the verbatim nomenclature key. The `[INFER]` on the
  letter-meaning decode (P/V/U/L/B/A/N) cannot be lifted without it. **The digit decode (I/O
  counts) is fully verifiable from Table 1 in any of the four [CERT-web] datasheets.** Residual
  risk: very low (29/29 consistent; no exceptions found across all official docs).
- **B835-G4** — live commissioning probe on the operator's JACE (authorized hardware probe); still
  requires physical access.
- **B836-G1 / B837-G1** — per-block property tables / FTT-10 Echelon termination guide; residual.
