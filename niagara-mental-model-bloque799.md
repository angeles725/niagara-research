# B799 · `schema-risk.sh` two-snapshot fixtures + the slot-diff rules (PR5 / issue #46) `[CERT-artifact]`

> **§20 DOCUMENT-mode capture** — the RED→GREEN fixture set and the diff RULES for `schema-risk.sh` (issue #46,
> math-model MM3). [Block 795] is the decision TABLE (change_kind → SAFE/LOSSY/OUTAGE); THIS block owns the
> two-snapshot slot-diff PARSER contract + the fixtures QA pins `qa/c7-schema-risk` from and the PR5 writer
> implements against. The fixtures are derived from the built MinimalPan skeleton [Block 794].
>
> **Sources**: [Block 795] §795.3/§795.4 (the classifier CSV + vocabulary), [Block 754] (the code-grounded
> matrix), [Block 739] (retype_simple OUTAGE). Fixtures live in scratch:
> `~/modulos_niagara_n4/_scratch/schema-risk-fixtures/` (untracked; this block is the citable record).
> Method: generate before/after pairs, one per B795 change class; verdicts transcribed from B795. Markers:
> `[CERT-artifact]` the fixtures/rules defined here · `[CERT]` cite into B795/B754.
>
> **Type:** `capture`. Connects [Block 795] (decision table — cite for verdicts), [Block 794] (MinimalPan base),
> [Block 754]/[Block 739] (evidence).

## 799.1 — The fixture set (7 before/after pairs) `[CERT-artifact]`

Each pair: `before/` + `after/` (each = `com/angeles/MinimalPan/BMinimalPan.java` with the `@Niagara*`
declarations + `module-include.xml`) + `expected.txt` (verdict + the B795 `change_kind` row it maps to). Base =
MinimalPan's `{setpoint:double OPERATOR, interval:baja:RelTime, tickExpired:action HIDDEN}`.

| Pair | The one change (after vs before) | change_kind (B795 key) | Verdict | B795 row |
|---|---|---|---|---|
| `add_slot` | + `@NiagaraProperty hysteresis:double` | `add_slot` | **SAFE** | r1 new-default |
| `remove_slot` | − simple `setpoint:double` | `remove_slot_simple` | **LOSSY** | r7 warningAndSkip |
| `retype_simple` | `setpoint` type `double`→`int` | `retype_simple` | **OUTAGE** | r11 / B739 |
| `reorder` | swap decl order `setpoint`↔`interval` | `reorder_slot` | **SAFE** | r2 byName index-free |
| `rename_slot` | `setpoint`→`targetTemp` (same type) | `rename_slot` | **LOSSY** | r9 orphaned |
| `unknown_kind` | `setpoint` property→ACTION at same name (a slot-KIND swap) | `UNKNOWN` (no row) | **OUTAGE** | §795.2 fail-safe |
| `mixed` | + `hysteresis` (SAFE) **and** `setpoint` retype `double`→`int` (OUTAGE) | `add_slot`+`retype_simple` | **OUTAGE** | worst-cell |

All seven use B795's existing vocabulary — no new `change_kind` was invented; `unknown_kind` deliberately
exercises the `UNKNOWN` fail-safe (a property↔action kind-swap the table has no specific row for).

## 799.2 — The slot-diff rules the parser must implement `[CERT-artifact, grounded in B795 §795.1]`

A "slot" is one `@NiagaraProperty` / `@NiagaraAction` / `@NiagaraTopic` declaration. Diff `before` vs `after`:

1. **Key a slot by its `name=`** (NOT by position). `.bog` binds by name, ungated (B795 §795.1) — this is the
   whole reason name is the key.
2. **add_slot** = a `name` present in `after`, absent in `before`. **remove_slot** = present in `before`, absent
   in `after` (→ resolve `_simple`/`_complex` from the type; if unresolved → `remove_slot_unknown`).
3. **retype** = same `name`, different `type=` in the annotation. Resolve `_simple` (a `BSimple` primitive —
   `double`/`int`/`boolean`/`long`/`float`/`String`/`baja:*` scalar) vs `_complex` (a component/BStruct type);
   if unresolved → `retype_unknown` (OUTAGE). (`retype_simple` is the B739 boot-loop cell.)
4. **reorder_slot** = the SET of `name`s is identical before/after but the DECLARATION ORDER differs. Detected
   from source/`module-include.xml` order. SAFE.
5. **rename_slot** = by pure name-keying a rename reads as `remove_slot`(old) + `add_slot`(new). The verdict is
   LOSSY either way (remove_simple=LOSSY + add=SAFE → worst LOSSY; and B795 r9 rename=LOSSY). A parser MAY emit
   `rename_slot` when a same-type slot appears added while another is removed; if it does not, the remove+add
   decomposition yields the same LOSSY — never downgrade.
6. **slot-KIND change** (property↔action↔topic at the same name), or any diff shape not resolvable to a table
   `change_kind` → emit `UNKNOWN` → OUTAGE. **Never downgrade on uncertainty** (B795 §795.2): a false SAFE ships
   a boot-loop.
7. **type-identity** (the `module-include.xml` `<type class=…>`): a changed class / package for the same `name`
   → `class_rename` / `package_move` (OUTAGE fail-safe, B795) — the parser cannot see the classloader layer.
8. **Overall module verdict = the worst cell** across all detected changes: `OUTAGE > LOSSY > SAFE`.

Emit ONLY B795 §795.4 vocabulary keys; look each up in the embedded CSV; fall to the matching `*_unknown` (or
`UNKNOWN`) when a subtype/kind can't be resolved.

## 799.3 — Kit implication → `schema-risk.sh` + `qa/c7-schema-risk` (PR5 / #46) `[CERT-grounded]`
- `schema-risk.sh <before-dir> <after-dir>` parses the two snapshots per §799.2, classifies each change into a
  B795 `change_kind`, looks up the §795.4 CSV, prints one row per change + the worst-cell module verdict; a
  pre-deploy gate FAILs on OUTAGE, WARNs on LOSSY.
- **RED→GREEN fixture contract** (what QA pins): for each of the 7 pairs, `schema-risk.sh before after` must
  print the `expected.txt` verdict; RED = no script (exit 127) / a classifier that mislabels a pair; GREEN = all
  7 match. Biting mutations: drop the retype_simple→OUTAGE mapping → `retype_simple` + `mixed` flip to
  SAFE/LOSSY (caught); drop the UNKNOWN fail-safe → `unknown_kind` flips off OUTAGE (caught).
- B799 owns the parser + fixtures; [Block 795] owns the verdict table. A change to decode behavior updates
  B754 → B795 → the CSV, never B799's parse rules.

## 799.4 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 7 before/after pairs exist, each with Java + module-include.xml + expected.txt | `[CERT-artifact]` | fixture tree (28 snapshot files + 7 expected.txt) | Y — generated + listed |
| 2 | Each pair's verdict matches a B795 row (SAFE/LOSSY/OUTAGE) | `[CERT]` | §799.1 vs B795 §795.3 | Y |
| 3 | All change_kinds are B795 vocabulary; unknown_kind = the UNKNOWN fail-safe (no new key) | `[CERT]` | §799.1 + B795 §795.4 | Y |
| 4 | Diff rules: name-keyed, retype=type= attr, reorder=decl-order, worst-cell, never-downgrade | `[CERT-artifact]` | §799.2, grounded in B795 §795.1/§795.2 | Y |
| 5 | retype_simple fixture is setpoint double→int (the B739 OUTAGE cell) | `[CERT]` | fixture diff: `type="double"`→`type="int"` | Y — diffed |

**Tally:** `[CERT-artifact]` ×2 · `[CERT]` ×3. Capture block — ratio not an exhaustion signal (§11).

## 799.5 — Connections & open gaps
- [Block 795] (the decision table this parser feeds), [Block 794] (MinimalPan base), [Block 754]/[Block 739] (evidence).
- No open gap: fixtures + rules are complete. Implementation = campaign-7 PR5 `schema-risk.sh` (#46); QA pins
  `qa/c7-schema-risk` from `~/modulos_niagara_n4/_scratch/schema-risk-fixtures/`.
