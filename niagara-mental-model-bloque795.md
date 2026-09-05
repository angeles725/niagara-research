# B795 · MM3 decision table — the slot-change survival matrix as a machine-readable classifier for schema-risk.sh (mechanizes B754)

> **Scope**: turn B754's saved-data survival matrix into the decision table that `schema-risk.sh` (niagara-tools
> issue #46, math-model MM3) embeds verbatim. One row per detectable schema-change kind; the verdict is the
> effect on an EXISTING `.bog` station when the changed module is installed over it: **SAFE** (data + boot
> intact), **LOSSY** (boots, but some saved data is silently dropped/orphaned), **OUTAGE** (station will not
> boot). Every cell carries its evidence. The classifier's contract closes with a fail-safe: **any change kind,
> or any subtype, the parser cannot positively resolve → OUTAGE**. No new code was read that B754 did not already
> ground; this is a re-expression of [B754] for machine consumption + the ext/package-move extrapolations.
>
> **Sources**: FUENTE 1 — [B754] §754.5/§754.6 (the code-grounded matrix; `Station.java:171-186`,
> `ValueDocDecoder.parseSlot:273,375,389,394,467-473,503-529`), [B739] (simple-retype OUTAGE), [B740]/[B631]
> (missing-class classloader layer). No live probe. The matrix cells are [CERT] via B754's code cites; the
> ext-slot and package-move rows are [INFER] grounded in the frozen/complex-slot and missing-type cells.

---

## 795.1 — The one rule the whole table encodes `[CERT via B754 §754.5]`
A change is survivable **iff** its decode path is wrapped in `warningAndSkip` (non-fatal — that slot is dropped
or shunted to a dynamic slot, boot continues); it is an OUTAGE **iff** it throws UNWRAPPED past the child
`failFast=false` guard to `Station.java:174` (fatal). SAFE vs LOSSY then splits the survivable side: SAFE = the
saved value still applies (or the slot is new/reordered); LOSSY = it boots but the value is dropped or orphaned.
`.bog` binds slots BY NAME with no version/hash gate (§754.4), so this is decided purely by name-based
reconciliation against whatever module version is installed now.

## 795.2 — Why the classifier must fail-safe to OUTAGE `[INFER, grounded]`
Two verdicts depend on a distinction a slot-diff parser may not resolve from `module-include.xml`/decompiled
slots alone:
- **simple vs complex** property (a `BSimple` value with `v=` vs a `BComplex` subtree with `t=`): it flips
  REMOVE (simple→LOSSY, complex→SAFE) and RETYPE (simple→OUTAGE, complex→LOSSY).
- **which class-load layer** a type-identity change hits (a `.bog` CHILD ref → decode-time warn-skip=LOSSY; an
  OWNING/superclass/frozen-enum class → classloader-time fatal=OUTAGE, B740).

When the parser can resolve the subtype, it uses the precise row; when it can only see the coarse change kind,
it uses the `*_unknown` row (the worse of the two); an unrecognized change kind → OUTAGE. **Never downgrade on
uncertainty** — a false SAFE ships a boot-loop (the ClassCastException the retro `slot-type-change-rompe-bog`
describes), a false OUTAGE only over-warns.

## 795.3 — The MM3 decision table `[CERT cells cite B754 §754.6]`

| change_kind | verdict | evidence | note |
|---|---|---|---|
| add_slot (frozen prop/action/topic) | SAFE | B754 §754.6 r1 | old `.bog` has no entry → new default |
| reorder_slot | SAFE | B754 §754.6 r2 (`byName.get`, index-free) | — |
| change_flags | SAFE | B754 §754.6 r3 | semantics may shift |
| change_default | SAFE | B754 §754.6 r3 | unsaved slots adopt new default |
| change_facets | SAFE | B754 §754.6 r3 | display/range only |
| remove_slot_complex | SAFE | B754 §754.6 r6 (recreated as dynamic slot) | value preserved on a dynamic slot |
| remove_slot_simple | LOSSY | B754 §754.6 r7 (`warningAndSkip "Missing frozen property"`) | value dropped, boots |
| remove_action_or_topic | LOSSY | B754 §754.6 r8 (`warningAndSkip`) | — |
| remove_slot_unknown | LOSSY | fail-safe of r6/r7 | use when simple/complex unresolved |
| rename_slot | LOSSY | B754 §754.6 r9 | old data orphaned/warn-skipped, NOT auto-migrated |
| retype_complex | LOSSY | B754 §754.6 r10 (`set()` throws→`warningAndSkip "Cannot set property"`) | reverts to default |
| retype_simple | OUTAGE | B754 §754.6 r11 / B739 (`decodePrimitive:503-529` unwrapped) | saved `v=` cannot parse → propagates |
| retype_unknown | OUTAGE | fail-safe of r10/r11 | use when simple/complex unresolved |
| add_enum_tag | SAFE | B754 §754.6 (`.bog` stores tag STRING) | old tags still resolve |
| remove_or_rename_enum_tag | OUTAGE | B754 §754.6 r13 (`getRange().get`→`InvalidEnumException`, `:447,543-548`) | a `.bog` still storing the tag is fatal |
| renumber_enum_ordinals | SAFE | B754 §754.6 r5 | ⚠ UNSAFE for Fox/binary sync (encodes ordinal) |
| add_ext | SAFE | [INFER] = add_slot; a BPointExtension is a complex child | new ext takes its default |
| remove_ext | LOSSY | [INFER] = remove_slot_complex fail-safe | complex ext shunts to dynamic (SAFE-ish); if the ext TYPE is also removed → class_rename→OUTAGE |
| class_rename (type id changes) | OUTAGE | fail-safe of B754 §754.6 r12/r13 | child-ref alone = `TypeNotFound` warn-skip (LOSSY); owning/superclass/enum class = classloader fatal (OUTAGE, B740); can't tell → OUTAGE |
| package_move | OUTAGE | [INFER] grounded in r12/B631 | if the REGISTERED type name is unchanged it may be SAFE, but the classifier cannot verify the classloader → OUTAGE |
| *(unknown change_kind)* | OUTAGE | §795.2 fail-safe | documented default |

## 795.4 — Embeddable classifier table (CSV — schema-risk.sh reads this verbatim) `[CERT/INFER as §795.3]`

```csv
change_kind,verdict,evidence,note
add_slot,SAFE,B754-754.6-r1,new-default
reorder_slot,SAFE,B754-754.6-r2,index-free-byName
change_flags,SAFE,B754-754.6-r3,semantics-shift
change_default,SAFE,B754-754.6-r3,adopt-new-default
change_facets,SAFE,B754-754.6-r3,display-only
remove_slot_complex,SAFE,B754-754.6-r6,shunt-to-dynamic
remove_slot_simple,LOSSY,B754-754.6-r7,value-dropped
remove_action_or_topic,LOSSY,B754-754.6-r8,warn-skip
remove_slot_unknown,LOSSY,B754-754.6-r6r7-failsafe,use-when-subtype-unknown
rename_slot,LOSSY,B754-754.6-r9,orphaned-not-migrated
retype_complex,LOSSY,B754-754.6-r10,reverts-to-default
retype_simple,OUTAGE,B754-754.6-r11-B739,unparseable-v-propagates
retype_unknown,OUTAGE,B754-754.6-r10r11-failsafe,use-when-subtype-unknown
add_enum_tag,SAFE,B754-754.6,tag-string-stored
remove_or_rename_enum_tag,OUTAGE,B754-754.6-r13,InvalidEnumException
renumber_enum_ordinals,SAFE,B754-754.6-r5,fox-sync-unsafe
add_ext,SAFE,INFER-add_slot,complex-child-default
remove_ext,LOSSY,INFER-remove_slot_complex,dynamic-shunt-or-type-removed
class_rename,OUTAGE,B754-754.6-r12r13-failsafe,child-ref-LOSSY-else-classloader-OUTAGE
package_move,OUTAGE,INFER-r12-B631,classloader-unverifiable
UNKNOWN,OUTAGE,B795-795.2-failsafe,default-fail-safe
```

**Contract for `schema-risk.sh`**: resolve the most specific `change_kind` the slot-diff can prove; fall back to
the matching `*_unknown` row when a subtype is unresolved; any `change_kind` not in the table → look up `UNKNOWN`
(= OUTAGE). Overall module verdict = the worst cell across all detected changes (OUTAGE > LOSSY > SAFE).

## 795.5 — Kit implication `[INFER]`
This IS the MM3 contract's decision table. `schema-risk.sh` embeds §795.4 verbatim, parses a two-snapshot
slot-diff into `change_kind` rows, and returns the worst verdict; `verify-module.sh` (or a pre-deploy gate) fails
on OUTAGE, warns on LOSSY. The table is the single source of truth — a change to Niagara decode behavior updates
B754 → this table → the embedded CSV, never the script's logic. Motivating failure: a persisted-slot retype
(`retype_simple`/`retype_unknown` = OUTAGE) is exactly the ClassCastException boot-loop the classifier must catch
BEFORE deploy.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Survivability = `warningAndSkip` (SAFE/LOSSY) vs unwrapped throw (OUTAGE); `.bog` binds by name, ungated | [CERT] | B754 §754.4/§754.5 (Station.java:171-186; ValueDocEncoder:1081-1088) |
| 2 | SAFE rows: add/reorder/flags/default/facets/add-tag/renumber-ordinals; remove-complex shunts to dynamic (data kept) | [CERT] | B754 §754.6 r1,r2,r3,r5,r6 |
| 3 | LOSSY rows: remove-simple, remove-action/topic, rename, retype-complex | [CERT] | B754 §754.6 r7,r8,r9,r10 |
| 4 | OUTAGE rows: retype-simple (B739), remove/rename enum tag, missing owning/enum class | [CERT] | B754 §754.6 r11,r13; B739; B740 |
| 5 | Fail-safe: unresolved subtype → `*_unknown` (worse cell); unknown change_kind → OUTAGE; module verdict = worst cell | [INFER] | §795.2 grounded in the r10/r11 + r12/r13 splits |
| 6 | ext add=SAFE / ext remove=LOSSY; package_move=OUTAGE fail-safe | [INFER] | ext = complex child (r1/r6); package_move = type-identity risk (r12/B631) |
| 7 | CSV in §795.4 is the verbatim classifier source; verdict order OUTAGE>LOSSY>SAFE | [CERT-artifact] | §795.4 (this block) |

**Tally**: 4 [CERT], 2 [INFER], 1 [CERT-artifact]. Every OUTAGE/SAFE/LOSSY cell traces to a B754 §754.6 row or
B739/B740; the two [INFER] rows (ext, package_move) are flagged as such in the table and CSV.

## Connections
- **B754** (the code-grounded matrix this mechanizes), **B739** (simple-retype OUTAGE = one cell), **B740**/**B631**
  (missing-class classloader layer = the class_rename/package_move OUTAGE), **B784** (module.xml `<dependency>`
  floors — the version half of the same deploy-safety story), **B790/B793** (the scaffold that would author
  add-only-safe modules). Kit: MM3 / niagara-tools issue #46; retro `slot-type-change-rompe-bog`.

## Open gaps
- **B795-G1** (requires-execution): confirm `retype_complex` reverts-to-default vs orphans on a LIVE station with a
  seeded `.bog` (the matrix predicts LOSSY; not station-verified). Feeds the same station backlog as B793-G1.
- **B795-G2**: `package_move` when the registered type name is unchanged — is it truly SAFE? Needs a built
  before/after pair (an implementation probe), so the table keeps the OUTAGE fail-safe until proven.
