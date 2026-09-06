# Retro — bog-nav.py + module-find.py: what STRUCTURE-aware tools find that grep cannot

Date: 2026-09-05
Author: companero (Opus 4.8)
Scope: two READ-ONLY toolbelt tools for the niagara-research team — `tools/bog-nav.py`
(station `config.bog` navigator) and `tools/module-find.py` (module Java-source finder).
Both REUSE the grammar + handle-graph / source-scan engine from
`build-n4-module-kit/toolbelt/bog-audit.sh` (kit main 3f666a0) rather than reparsing.
Proven on PANCCADIA `config.bog` and the client tree
(`~/modulos_niagara_n4/Cliente/Leon-Guanjuato`, HEAD 4f5f1c7; client main baseline a109249).

## Why these exist

The four session tools (corpus-nav, module-navigator, niagara-help, hdbread) navigate the
CORPUS and the decompiled framework. Neither answers questions about a LIVE STATION's saved
graph or a specific client MODULE's slot/action surface. A `config.bog` is a ZIP whose
`file.xml` is a BOG-XML tree; the killer feature grep lacks is that a link stores its source
as an opaque handle (`sourceOrd='h:44d51'`) on the TARGET component — there is no textual path
to grep for. And a module's slots are declared in multi-line annotations whose paren-balanced
tail (the `flags=`/`type=`/MIN facet) wraps across lines a line-oriented grep splits.

## What the tools found that grep missed — five concrete wins

1. **Handle → path link resolution.** `bog-nav links --from Cuarto1 --slot setpoint` returns
   `Services/DashboardService/Cuarto1.setpoint --> Programacion/ColdRoom_1.setpoint`. grep on
   the bog sees only `<p n="sourceOrd" v="h:44d51"/>` and `<p n="targetSlotName" v="setpoint"/>`
   on two unrelated lines in a component 4000 lines away from `Cuarto1`; it cannot join them.
   `bog-nav handle 44d51` prints the full fed-by/feeds graph of a component in one call.

2. **The setpoint dataflow direction was the opposite of the naive read.** The RoomPanel
   (dashboard facade `Cuarto1`) is the SOURCE; `ColdRoom_1` (the logic) is the TARGET. The
   operator's setpoint edit lands on the facade and PROPAGATES down the link into the logic —
   confirming the B816 link-target write-path model against a real station, not a diagram.

3. **BRoomPanel.setpoint is a SUMMARY | OPERATOR complex slot — the exact S19 target.**
   `module-find slots --flags OPERATOR --type BStatusNumeric` on DashboardPan-rt returns
   `BRoomPanel.setpoint  flags=Flags.SUMMARY | Flags.OPERATOR  [COMPLEX]` — an OPERATOR-visible
   `BStatusNumeric` config property with no action, which is precisely the shape the S19
   ext-writable-shape lint must flag (written externally via the oBIX child-leaf, B826-G2
   [CERT-live], not via a native action).
   **CORRECTION (defect found by the lead re-running acceptance, 2026-09-05):** the FIRST cut
   of this tool reported this set as EMPTY and I wrote "no complex slot is OPERATOR." That was a
   FALSE NEGATIVE from a flags-parser bug — the regex captured only the first token of
   `Flags.SUMMARY | Flags.OPERATOR` (the ` | ` spaces defeated a single-token char class), so
   every multi-flag slot lost its OPERATOR. Fixed to capture the whole `flags = … ,` expression;
   selftest pinned with a `SUMMARY | OPERATOR` case. Lesson below (#6) — a truncating parser
   produced a confident, wrong "finding"; only the second reader re-running the query caught it.

4. **The type-string form differs from doctrine assumption.** The client writes
   `type = "BStatusNumeric"` (Java-class form), not `"baja:StatusNumeric"` (module-name form).
   My first complex-detection regex (`baja:Status\w+$`, lifted straight from a corpus block)
   silently matched NOTHING. The tool's own selftest did not catch it — the REAL tree did.
   Fix: `(?:baja:|B)?Status\w+$`. Lesson: a doctrine regex validated only on canonical
   examples must be re-proven on the client's actual annotation style.

5. **The servlet setpoint writer is DYNAMIC, not static.** `module-find writers setpoint`
   finds no `setSetpoint(`/`.set("setpoint",` — the servlet does `parent.set(prop, toSet, null)`
   where `prop` is resolved from the request ORD at runtime (BDashboardServlet:274). A grep for
   `setpoint` in the servlet finds comments and the handler name but never the write. The tool
   now reports the dynamic writer explicitly: "slot chosen at runtime — any slot could be the
   target" — which is the security-relevant truth (the RBAC/validation guard, not a slot
   allow-list, is what bounds it; ties to PR#7's 400 numeric-validation guard).

## Lessons (fold-worthy)

- **Reuse paid off, but the engine's assumptions are corpus-shaped.** The bog grammar (TAG_RE,
  the `ga()` single-then-double-quote reader, the link_buf handle capture) transplanted with
  zero changes and parsed a 400 KB real bog correctly. But two value-judgement regexes (complex
  type form, MIN facet form) were tuned to canonical corpus examples and needed the client tree
  to expose the gap. A self-test built from a synthetic tree can only prove the shapes you
  already thought of — PROVE ON THE REAL TREE is not optional.
- **A true-empty is a finding, not a null result — but PROVE the empty is real.** "No static
  setpoint writer" is a substantive answer (the servlet writes dynamically). "No OPERATOR
  complex slot" LOOKED like one and was a parser bug (see #3). The distinction between a real
  empty and a truncation/parse artifact cannot be made from the empty result alone — it needs a
  second query that would be NON-empty if the data were there (here: dumping the raw flags string
  showed `Flags.SUMMARY` where the source had `SUMMARY | OPERATOR`). A tool that emits a
  confident empty must be cross-checked against the raw underlying text before the empty becomes
  a claim.
- **(#6) A truncating parser is worse than a crashing one.** The flags regex did not error; it
  silently returned a shorter-but-plausible string, which read as a real fact. Every field a
  tool extracts by regex from a multi-token expression needs a selftest whose fixture puts the
  wanted token SECOND — a fixture with the token first (my original) passes even while the parser
  truncates. Reordering the selftest fixture to `SUMMARY | OPERATOR` is what now pins it.
- **The complex-slot write doctrine is now closed [CERT-live].** Both tools encode the final
  B826-G1/G2 result (child-leaf bare `<real>` preferred, parent wrapped-obj = silent-zero
  fallback) in the `writable` classifier note. Fold pending into B823 §823.7 / B825 §825.3 /
  the S12 write-server plan (peers investigador + investigador1, records b4e6d8a4f / f99f2e45b,
  pushes 3e8dc8b45).

## Acceptance proven

| Question | Tool + command | Result |
|---|---|---|
| Which link feeds ColdRoom_1.setpoint? | `bog-nav links --from Cuarto1 --slot setpoint` | `Cuarto1.setpoint --> Programacion/ColdRoom_1.setpoint` |
| servletName inherited from BWebServlet? | `module-find … extends --of BDashboardServlet` | `BDashboardServlet -> BWebServlet` |
| Every OPERATOR complex slot? | `module-find … slots --flags OPERATOR --type BStatusNumeric` | `BRoomPanel.setpoint  flags=Flags.SUMMARY \| Flags.OPERATOR  [COMPLEX]` (after the flags-truncation fix) |
| Who writes setpoint? | `module-find … writers setpoint` | no static writer; dynamic `parent.set(prop,…)` runtime write |

Both tools: python3 stdlib only, read-only, `--json`, and a `selftest` subcommand
(11 checks / 11 checks) that needs no external file.

## Second round (2026-09-05, lead's real-question expansion) — what each command replaced

Each new command was pinned with a selftest whose expected output is a fact the corpus already
holds, and proven on the real trees. What each replaced from today's manual work:

- **bog-nav `relays`** (CHECK11) — replaced running bog-audit and eyeballing the FAIL lines:
  22 own-module→writable proxy targets, **17 without a fallback** (hold last state on
  stop/reload, B810). Matches bog-audit exactly.
- **bog-nav `hoa`** (CHECK8) — replaced a manual scan for stuck overrides: 19 own-module
  mode/HOA slots AUTO, 10 persisted config-mode (fanRunMode=runOnDelay), **0 active
  priority-array overrides**. RoomPanel `*Mode` slots are TRANSIENT (absent from the bog).
- **bog-nav `tiles`** (CHECK18) — replaced hand-tracing evapN link numbering: **Cuarto1 has
  units 1/3 CROSSED** (dashboard tile evap1 ← EvaporatorUnit_3, evap3 ← EvaporatorUnit_1);
  Cuarto2 consistent. A wiring foot-gun that no grep surfaces.
- **bog-nav `links --dangling --src`** (CHECK7) — replaced a manual "does this link target a
  real slot" check: **0 dangling** when `--src` covers all own modules (like bog-audit, one
  root must contain every own module's source, else non-scanned modules read as false dangles).
- **bog-nav `path` / `find`** — handle→path reverse and a flat type list (shorthands).
- **bog-nav `diff`** now reads a `.dist` station backup (nested config.bog): proven on
  PANCCADIA config.bog vs the PRUEBAS `.dist` (497 real component/slot deltas).
- **`--json`/`--csv` on every command** — via an argparse parent parser; fixed the reported
  bug that `--json` was rejected AFTER the subcommand.
- **module-find `slot-types`** — replaced tallying slot types by hand: per-Java-type count with
  OPERATOR / complex / TRANSIENT — the input table for the slot-type doctrine + S19.
- **module-find `ext-writable`** — the **S19 lint preview**: an OPERATOR complex property with
  no action on its class is a WARN (write it via the oBIX child-leaf bare `<real>` B826-G2, or
  add an OPERATOR action B822). On DashboardPan-rt: **BRoomPanel.setpoint = 1 WARN**.
- **module-find `compare <root> <srcB>`** — replaced reading a git diff for schema changes:
  annotation-level added/removed/retyped/reflagged. Proven **4f5f1c7→a109249**:
  `defrostSkipped` + `lastSkipReason` slots and `forceDefrost()` action ADDED, **0 schema-risk**
  (additive, schema-SAFE — the schema-risk.sh companion at annotation granularity).
- **module-find `callers <method>`** — call sites of `setSetpoint`/`forceDefrost` across the tree.

Lesson from this round: the expected-output-is-a-held-fact discipline is what made the pins
trustworthy — every new command's selftest asserts a number we already had from bog-audit or the
corpus (22/17, 19, the 1/3 crossing, the a109249 delta), so a regression shows up as a wrong
known number, not a silently-plausible one (the trap that produced the flags false-negative).
Selftests now: bog-nav 24, module-find 17.
