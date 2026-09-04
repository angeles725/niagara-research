# B739 · Safe schema evolution — why retyping an existing slot breaks the `.bog` and won't boot, and the add-don't-retype rule ([CERT-live] outage)

> **Scope**: how a station's `.bog` encodes/decodes component slots, why changing an EXISTING slot's TYPE in a
> deployed module can make the station fail to load, and the safe rules for evolving a module's schema.
> Grounded in a real production outage (PANCCADIA León, 2026-09-03) + the `ValueDocDecoder` mechanism.
>
> **Sources**: FUENTE 3 docSource `extracted/baja/javax/baja/io/{ValueDocDecoder,ValueDocEncoder}.java`
> (read this session). FUENTE (empirical [CERT-live]) the outage + kit retro
> `build-n4-module-kit/retros/2026-09-03-slot-type-change-rompe-bog-station-no-arranca.md`. FUENTE 1: B4
> (slots), module-anatomy (slotomatic/type hash).

---

## 739.1 — How the `.bog` stores a component `[CERT]`
The station config is a `.bog` — an XML value document. `ValueDocEncoder` writes each slot's value **encoded
per its declared TYPE**: a simple (`double`, `boolean`) writes as a scalar; a `BComplex`/`BStruct`
(`BStatusNumeric`, `BStatusBoolean`) writes as a nested structure with child `<p>` elements
(`value`/`status`/facets…). On load, `ValueDocDecoder` reads each element and **sets it into the CURRENT
type's slot of that name**. When it can't, it does `plugin.warningAndSkip("Cannot set property <Type>.<name>:
<e>")` / `"Missing frozen property …"` / `"Missing slot …"` (`ValueDocDecoder.java`).

## 739.2 — Why retyping an existing slot breaks decode `[CERT-live]`
If a slot that already has SAVED data is redefined with an incompatible TYPE, the decoder cannot fit the
saved encoding into the new slot, and — critically — the parse **DESALIGNS**: the saved complex's child
`<p>` elements get read as if they were slots of the PARENT, producing a cascade of missing/cast warnings,
then a structural SEVERE that aborts the load. Exact signature from the outage (retyping
`BRoomPanel.setpoint` from `BStatusNumeric` → `double`, Supervisor N4.14, 17:04):
```
WARNING [sys.xml] Cannot set property RoomPanel.setpoint: BStatusNumeric cannot be cast to BDouble
WARNING [sys.xml] Missing frozen property: differentialUp / zoneHighLimit / zoneLowLimit / evapLowLimit
WARNING [sys.xml] Missing slot StatusNumeric.startDelay   ← decoder now thinks startDelay is a child of the old StatusNumeric
SEVERE  [sys] Cannot load station — java.lang.ClassCastException: BRelTime cannot be cast to BComplex
        at javax.baja.io.ValueDocDecoder.parseSlots(...)          → "App Failed"
```
So a slot RE-TYPE is not "the value resets" — it can **desync the whole document and prevent boot**.

## 739.3 — Safe vs unsafe schema changes `[CERT/CERT-live]`
| Change | Safe? | Why |
|---|---|---|
| **ADD** a new slot | ✅ safe | absent in old `.bog` → decoder uses the new default; nothing to mis-decode |
| **REMOVE** a slot | ✅ mostly | old element has no target → `warningAndSkip` (noise, not fatal) |
| **RENAME** a slot | ⚠️ loses value | = remove old + add new; the saved value is skipped (new slot at default) |
| **RETYPE** an existing slot, simple↔simple compatible (e.g. int→double) | ⚠️ risky | may coerce or skip; test |
| **RETYPE BComplex ↔ simple** (`BStatusNumeric`↔`double`, `BStatusBoolean`↔`boolean`) | ❌ **UNSAFE** | the saved nested structure cannot decode into a scalar → desync → SEVERE → no boot |

**Rule (hard):** in a module already instantiated with saved data on a live station, **NEVER change an
existing slot's TYPE** — above all BComplex↔simple. **ADD a new slot** instead. The real fix for "make the
setpoint oBIX-writable" was to add a NEW `double` `setpointCmd` (SUMMARY|OPERATOR), NOT retype `setpoint`
(retro rt slot-type). The slotomatic type-hash changing is normal and not the breaker; the breaker is the
value-decode mismatch on EXISTING saved data.

## 739.4 — Recovery `[CERT-live]`
When a retype has already tumbled a live station: revert the module to the compatible slot type and
redeploy + restart (what recovered PANCCADIA); or hand-edit the `.bog` to drop/repair the offending slot
before load. Prevention beats recovery — this took production refrigeration down.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | .bog encodes each slot per its declared type; simple=scalar, BComplex=nested `<p>` children | [CERT] | ValueDocEncoder.java; B4 |
| 2 | ValueDocDecoder sets each element into the current type's slot; on failure warningAndSkip("Cannot set property…"/"Missing…") | [CERT] | ValueDocDecoder.java |
| 3 | Retyping a saved slot desyncs the parse → cascade → SEVERE ClassCastException in parseSlots → station won't load | [CERT-live] | outage log 2026-09-03 17:04; retro |
| 4 | ADD slot = safe; RETYPE existing (esp. BComplex↔simple) = unsafe/no-boot; RENAME loses value | [CERT/CERT-live] | §739.3; the fix = add setpointCmd not retype |

**Tally**: 3 [CERT], 1 [CERT-live]. No unmarked claims.

## Connections
- **B4** (slots/BComplex), **B734/B735** (slot types/facets), **B729-B738** (rt authoring), module-anatomy
  (slotomatic/type hash). Kit retro (slot-type outage) + types/logic.md.

## Open gaps
- **B739-G1**: exact simple↔simple coercions the decoder DOES tolerate (int→double, enum ordinal changes) —
  not exhaustively tested; treat all retypes as risky until proven.
