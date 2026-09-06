# B828 · HOA modes as a real `BFrozenEnum` over oBIX (for future modules) — a `@NiagaraEnum(@Range …)` `BHoaMode` renders `<enum val="hand" display="…" range="…"/>` and decodes `<enum val="hand"/>` WITHOUT an explicit range facet (a frozen enum carries its range intrinsically — the encoder/decoder fall back to the value's `getRange()`); today's `double` 0/1/2 is an opaque `<real>`; the switch is a LOSSY retype so new modules only `[CERT]`

> **Scope**: today `BRoomPanel`'s HOA modes are a `double` 0/1/2 written as a bare `<real>` — opaque over oBIX (no labels,
> no legal-value list). [B803]/[B823] noted an oBIX `<enum>` decode "needs a range facet". This block designs the
> `BFrozenEnum` HOA slot (`BHoaMode`) with the exact `@NiagaraEnum`/`@Range` so oBIX ADVERTISES and DECODES `<enum val="hand"/>`,
> shows what the façade GET renders, the lexicon keys per tag (the SP6 known-set), the DashboardPan reader/writer changes,
> and the migration caveat (retype = LOSSY → future modules only, existing keep `double`). Refines the [B803]/[B823]
> "needs a range facet" to: a FROZEN enum does NOT need an explicit facet; a DYNAMIC enum does. REMITTANCE — [B4] (the
> `BFrozenEnum`/`@NiagaraEnum`/`@Range` model).
>
> **Sources**: FUENTE 3 (`[CERT]`, decompiled, read at the enclosing method) — `obixDriver-rt` `com/tridium/obix/util/ObixUtils.java`
> (the enum encode), `javax/baja/obix/io/ObixDecoder.java` (the enum decode), `kitControl-rt` `enums/BDisableAction.java`
> (the `@NiagaraEnum`/`@Range` exemplar). FUENTE 1 — [B4] (Baja enums, REMITTANCE), [B803]/[B823] (the enum-needs-range
> doctrine, refined), [B795] (retype schema-risk), `ColdRoomControl` (`HOA_AUTO=0/HOA_HAND=1/HOA_OFF=2`), the slot-per-slot
> retro (SP6 — `@Range` tags are live lexicon keys).

---

## 828.1 — Today: HOA is a `double` 0/1/2, opaque over oBIX `[CERT]`
`BRoomPanel`'s `valveMode`/`fanMode`/`resistanceMode` (and `BEvaporatorUnit`'s) are `double` slots carrying `0/1/2`,
matching `ColdRoomControl.HOA_AUTO=0, HOA_HAND=1, HOA_OFF=2`. Over oBIX they GET as a bare `<real val="2.0"/>` — no tag,
no display, no legal-value list; a client must KNOW that 2 = OFF. Writable as a bare `<real>` (a `BSimple`), but
semantically opaque. The doctrine ([B823] slot-type table) already flags: for a mode, "a `double` written as `<real>` or
an ENUM WITH a range facet." This block designs the enum form and settles the facet question.

## 828.2 — The `BHoaMode` `BFrozenEnum` design `[INFER, grounded in the BDisableAction exemplar]`
Model on `kitControl`'s `BDisableAction` (`@NiagaraEnum(range={@Range("maxValue"),@Range("minValue"),@Range("hold"),@Range("zero")}) final class … extends BFrozenEnum`, ordinals 0-3, `make(int)`/`make(String)` via `getRange().get(…)`):
```java
@NiagaraEnum(range = { @Range("auto"), @Range("hand"), @Range("off") })   // ordinals 0,1,2 — MATCH ColdRoomControl
public final class BHoaMode extends BFrozenEnum {
  public static final int AUTO = 0, HAND = 1, OFF = 2;
  public static final BHoaMode auto = new BHoaMode(AUTO), hand = new BHoaMode(HAND), off = new BHoaMode(OFF);
  public static BHoaMode make(int ord){ return (BHoaMode)auto.getRange().get(ord,false); }
  public static BHoaMode make(String tag){ return (BHoaMode)auto.getRange().get(tag); }
  public Type getType(){ return TYPE; } public static final Type TYPE = Sys.loadType(BHoaMode.class); ...
}
```
The slot becomes a `BStatusEnum` of range `BHoaMode` (status + value) or a plain `BHoaMode` property. Ordinals match the
existing `double` so the semantic is preserved (though the retype is still LOSSY, §828.6).

## 828.3 — A FROZEN enum self-describes over oBIX — NO explicit range facet needed `[CERT]`
The oBIX enum ENCODE (`ObixUtils.encode`, `:351-378`):
```
obj.setElement("enum");                                           // :353
BEnumRange r = (BEnumRange)cx.getFacets().get("range");           // :356 — the SLOT facet first…
if (r == null || r.getOrdinals().length == 0) r = e.getEnum().getRange();  // :358 — …else the VALUE's own range
obj.setVal(r.getTag(ordinal));            // :360 — val = the tag, e.g. "hand"
obj.setDisplay(r.getDisplayTag(ordinal, cx));  // :361 — display = the LOCALIZED tag (module lexicon)
… obj.setRange(makeRangeUri(makeContractUri(enc, r.getFrozenType())));  // :375 — range = a URI to BHoaMode's oBIX contract
```
So a `BHoaMode` slot GETs as **`<enum val="hand" display="<lexicon>" range="…/BHoaMode"/>`** — the tag, a localized
display, and a `range` URI a client can dereference for the full legal-value list. The DECODE (`ObixDecoder.java:184/245/333`)
reads `cx.getFacets().get("range")` and (in `setFromVal`) falls back to `((BStatusEnum)cpx).getValue().getRange()` — so
`<enum val="hand"/>` decodes against the FROZEN enum's own range. **Because both encode and decode fall back to the enum
value's `getRange()`, a FROZEN enum carries its range intrinsically — NO explicit `BFacets.RANGE` on the slot is required.**
This REFINES [B803]/[B823]'s "an enum needs a range facet": the facet is needed only for a DYNAMIC enum (`BDynamicEnum`,
where the range isn't intrinsic); a `@NiagaraEnum`/`@Range` `BFrozenEnum` supplies its own. `[CERT]`

## 828.4 — Lexicon keys per tag (the SP6 known-set) `[CERT-grounded]`
`obj.setDisplay(r.getDisplayTag(ordinal, cx))` (`:361`) reads the LOCALIZED display for each tag from the module lexicon.
So `BHoaMode`'s three tags need `module.lexicon` entries (the frozen-enum display convention), e.g. `BHoaMode.auto=Automático`,
`BHoaMode.hand=Manual`, `BHoaMode.off=Apagado` (Spanish, per the client). These are exactly the **`@Range` enum-tag keys the
slot-per-slot SP6 known-set treats as LIVE lexicon translations** (not dead) — so a lexicon-coverage lint ([B824]/slot-coverage)
must count them. Without the keys, the GET `display` falls back to the raw tag ("hand"). `[CERT for the mechanism; the keys are the authoring task]`

## 828.5 — DashboardPan reader/writer changes `[INFER, grounded]`
- **Reader** (`DashboardReader`): the HOA slots move from a numeric read to an enum read — emit the TAG (`hand`) or the
  ordinal + a display, instead of `2.0`. The status tag ([B821] §821.4) still applies (fault/stale on the `BStatusEnum`).
- **Writer**: over oBIX, PUT `<enum val="hand"/>` to the enum slot (decodes via §828.3, no facet). Via the servlet
  ([B823] channel 3), coerce the incoming value to `BHoaMode.make(tag)` (or ordinal) instead of a `BStatusNumeric`. The
  wrapped-`<obj>`/child-`value` question ([B825]/[B826]) is moot for an enum written as `<enum>` — the enum is a `BSimple`
  written directly.
- The control code reads `((int)mode.getOrdinal())` where it reads `((int)getValveMode())` today — a mechanical swap
  preserving the `applyHoa`/`resistanceCommand` logic ([B805] §805.11 precedence unchanged).

## 828.6 — Migration caveat: the switch is a LOSSY retype — future modules only `[CERT-doc via B795]`
Changing an existing `double` HOA slot to a `BHoaMode`/`BStatusEnum` slot is a **RETYPE** — the saved `.bog` holds a
`double`, the new module expects an enum → a decode type-mismatch on load (the [B800] §800.8 class: `WARNING [sys.xml]
Cannot set property … ClassCastException` at best = LOSSY, the value dropped; a frozen owning-class change = OUTAGE).
Per [B795], a retype is never SAFE. **So `BHoaMode` is for NEW modules (or a deliberate bog-migration deploy), not an
in-place change to the shipped ColdRoomPan/CompPan** — those keep the `double` 0/1/2 (the [B823] doctrine's "today `double`"
row). A future module authors HOA as `BHoaMode` from the start and gets the self-describing oBIX enum for free. `[CERT-doc]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Today HOA is a `double` 0/1/2 (AUTO/HAND/OFF), opaque `<real>` over oBIX | `[CERT]` | `ColdRoomControl` HOA_* ordinals; `BRoomPanel` mode slots |
| 2 | `BFrozenEnum` via `@NiagaraEnum(range={@Range …})` + `make(int/String)` — the BHoaMode pattern | `[CERT]` (exemplar) | `BDisableAction.java:11-31` |
| 3 | oBIX enum encode emits `val`=tag, `display`=localized tag, `range`=frozen-enum contract URI | `[CERT]` | `ObixUtils.java:353,356,358,360-361,375` |
| 4 | The range falls back to the enum value's `getRange()` (encode :358, decode `setFromVal`) → a FROZEN enum needs NO explicit facet; a DYNAMIC enum does (refines B803/B823) | `[CERT]` | `ObixUtils.java:358`; `ObixDecoder.java:184/245/333` + setFromVal |
| 5 | The GET `display` is the module-lexicon tag → the three `@Range` tags need lexicon keys (SP6 live-key set) | `[CERT]`+`[CERT-grounded]` | `ObixUtils.java:361` getDisplayTag; slot-per-slot SP6 |
| 6 | double→enum is a RETYPE = LOSSY/OUTAGE → future modules only | `[CERT-doc]` | [B795] retype; [B800] §800.8 ClassCastException |

**Tally**: 4 `[CERT]` · 1 `[CERT]`+exemplar · 1 `[CERT-doc]`. The load-bearing encode/decode range-fallback (the "frozen
enum needs no facet" refinement) was read at the enclosing method. §828.2/§828.5 (the BHoaMode design + reader/writer) are
`[INFER]` grounded in the [CERT] mechanism + the `BDisableAction` exemplar. Dedupe: the `BFrozenEnum`/`@Range` model is
REMITTANCE ([B4]); the enum-decode-needs-range is [B803]/[B823] (refined here); this block adds the oBIX enum ENCODE
(val/display/range emit), the frozen-vs-dynamic facet refinement, the `BHoaMode` design, and the migration caveat.

## Connections
- **[B803]**/**[B823]** (the "enum needs a range facet" doctrine — REFINED here: frozen=no facet, dynamic=facet; the
  slot-type table's mode row), **[B4]** (`BFrozenEnum`/`@NiagaraEnum`/`@Range` — REMITTANCE), **[B795]**/**[B800]** §800.8
  (the retype LOSSY/OUTAGE = the migration caveat), **[B822]**/**[B825]**/**[B826]** (the setpoint write forms — enum is
  simpler, written as `<enum>` directly), **[B805]** §805.11 (the HOA precedence the control code keeps), **[B821]** §821.4
  (the `BStatusEnum` status tag the reader still emits), slot-per-slot SP6 (`@Range` tags = live lexicon keys). Kit: a
  `types/logic-authoring.md` line — "a mode/state slot a future module exposes externally = a `BFrozenEnum` (`@NiagaraEnum`/`@Range`),
  which self-describes over oBIX (`<enum val>` + range, no facet); `double` 0/1/2 is legacy; never retype an existing one."

## Open gaps
- **B828-G1** (bounded): the exact `module.lexicon` key SYNTAX for a frozen-enum tag display (`BHoaMode.hand` vs a
  module-global convention) — confirm against a Tridium frozen-enum lexicon (`kitControl`'s `BDisableAction` display keys)
  before authoring, so the GET `display` localizes rather than falling back to the raw tag.
- **B828-G2** (requires-execution): a live GET/PUT of a `BFrozenEnum` slot on a station to confirm the `<enum val="…" range="…"/>`
  shape and that `<enum val="hand"/>` decodes without a slot facet (pairs with [B826]'s live-probe pattern).
