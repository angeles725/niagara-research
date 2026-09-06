# B832 — The three C10 method-boundary parser copies diverge: net-depth vs peak-depth is a real one-liner false-negative (T1 shared-fragment evidence)

**Focus**: build-n4-module-kit (meta / kit-tooling). **Scope**: source-backed evidence for C11 T1 (extract the
method-boundary parser shared by `lint-timers.sh`, `lint-ext-writable-shape.sh`, `lint-silent-protection.sh` into one
fragment + a golden set). **Sources**: kit `2f3300f` (all C10 lint PRs merged), the three toolbelt scripts; reproduced
lint runs. Read-only analysis for the investigador1 research lane requested by the C11 explore draft (niagara-research
`9d4024ad0`). Companion to [[build-n4-campaign9-close]]-era lint work and the C10 second-read probes
(`sources/probes/2026-09-07-c10-pr1..pr6-second-read.md`).

---

## 1. The three copies at a glance (kit 2f3300f)

| Copy | Parser region | Depth metric | Post-detect filter | Consumes body |
|------|--------------|--------------|--------------------|---------------|
| `lint-timers.sh` | Phase 2, `:194-234` | **net** `brace_depth` (`:202`) | none — records ALL methods | `meth_start/meth_end[]` array (Phase 3 same-method binding) |
| `lint-ext-writable-shape.sh` | Pass 2, `:136-183` | **peak** `max_d` (`:147`) | `mname in do_methods` (`:175`) | inline `_scan_writes(body)` (`:181`) |
| `lint-silent-protection.sh` | section D, `:309-372` | **net** `brace_depth` (`:326`) | none — records ALL methods+names | `meth_start/meth_end/meth_name[]` array |

The Case-A regex, Case-B backward scan, `@`-line stop, and both keyword-exclusion lists are byte-for-byte equivalent
across all three (see §3). The divergences are in the **depth metric**, the **comment-strip source in Case B**, and the
**caller-specific filter/consumption** — evidence they are three hand-copied variants of one parser, drifting.

## 2. Divergence D1 (BEHAVIORAL, load-bearing): net-depth misses one-liner methods — a real false-negative

`lint-timers.sh:202` / `lint-silent-protection.sh:326` gate the method-open on `brace_depth > old_depth` — the NET depth
at end-of-line vs start-of-line. A one-liner method `void arm() { flag = true; Clock.schedule(...); }` opens and closes
its brace on the same line, so net change is **0** → `brace_depth > old_depth` is false → **the method is never
detected**, and everything inside it is invisible to the check. `lint-ext-writable-shape.sh:139-147` instead tracks
`max_d` (the PEAK depth reached during the line) and gates on `max_d > old_d`, so the one-liner IS detected.

**REPRODUCED** (kit 2f3300f, `lint-timers.sh`): a class whose `arm()` sets a field true beside `Clock.schedule` on ONE
line → **0 companion-flag** (missed); the identical body split across multiple lines → **1 companion-flag** (caught). So
`lint-timers.sh` and `lint-silent-protection.sh` carry a silent false-negative for one-liner methods that
`lint-ext-writable-shape.sh` does not. `[ev: reproduced lint-timers runs @ 2f3300f]`

**Decision for the shared fragment: adopt peak-depth (`max_d`).** Net-depth is simply wrong for one-liners; ext-writable's
`max_d` is the correct primitive. Extracting the parser without this decision would either freeze the bug (net) or
silently change timers/silent behavior (peak) — so the golden set MUST contain a one-liner fixture (§5.4).

## 3. Divergence table (every difference, with lines)

| # | Divergence | lint-timers | lint-ext-writable | lint-silent-protection | Class |
|---|-----------|-------------|-------------------|------------------------|-------|
| D1 | method-open depth metric | net `brace_depth` `:202` | **peak `max_d`** `:147` | net `brace_depth` `:326` | **behavioral** (§2) |
| D2 | close depth var | `m_dep=brace_depth` `:230/:232` | `m_dep=max_d` `:176/:179` | `m_depth_at_open=brace_depth` `:359/:364` | naming + follows D1 |
| D3 | Case-B backward-scan source | `slines[k]` (// + /* */ stripped) `:217` | `slines[k]` `:162` | **`lines[k]; sub(/\/\/.*$/,"",lk)`** — RAW line, `//`-only strip `:340` | behavioral edge (block comments) |
| D4 | Case-A input var | `ln` (=slines) `:203` | `ln` `:150` | `stripped` `:329` | cosmetic (both fully stripped) |
| D5 | Case-B char-class spelling | `[<>\[\]]` `:220` | `[<>\[\]]` `:165` | `[<>[\]]` `:347` | cosmetic (equivalent) |
| D6 | post-detect filter | none (all methods) | `mname in do_methods` `:175` | none (all methods) | **caller-specific** |
| D7 | body consumption | array for later pass | inline `_scan_writes` `:181` | array+names for later pass | **caller-specific** |

D6/D7 are the CALLER's job, not the parser's: the shared fragment should emit every method as a `[start, end, name]`
triple and let each caller filter (do_methods gate) and consume (array vs inline). D3 is a second latent behavioral bug:
`lint-silent-protection`'s Case-B scan strips only `//` from the RAW line, so a `/* ... ( ... */` block comment before a
brace-only line is scanned un-stripped — the shared fragment must scan the FULLY-stripped source (`slines[]`) uniformly.

## 4. Invariants the shared fragment MUST carry (all verified present in all three unless noted)

1. **Depth guard `>= 2`** — `lint-timers.sh:202`, `lint-ext-writable-shape.sh:147`, `lint-silent-protection.sh:326`. The
   class body opens at depth 1 and is never named a method. (In ext-writable it is redundant with the `do_methods` gate
   — see [[observed-mutation-must-flip-a-fixture]] / PR2 read — but it is harmless and belongs in the shared fragment.)
2. **Case-B `@`-line stop** — all three `if (substr(lk_t,1,1) == "@") break` (`:218`/`:163`/`:342`). This is the exact
   boundary that made the C10 S21-misparse pin bite ONLY in the multi-line annotation form (a single-line
   `@NiagaraProperty(... "new BAlarmRecord()")` stops the scan at the `@`; the multi-line form puts `defaultValue` on its
   own line and reaches the constructor). The shared fragment MUST preserve it verbatim, and the golden set MUST use the
   multi-line BMisparse (§5.1) or the invariant is untested. `[ev: PR1 pins bite-check, niagara-research 7877e48b5]`
3. **Keyword exclusion** — Case A `/^(if|for|while|switch|catch|try|else|do|new)$/`; Case B additionally
   `class|interface|enum`. Byte-identical across all three (`:208/:222`, `:153/:170`, `:332/:353`).
4. **Peak-depth one-liner detection (`max_d`)** — adopt ext-writable's `:139-147`; reject the net-depth of timers/silent
   (§2). Corollary invariant: **one-line getter/setter skip** — once one-liners are detected, a trivial generated
   `get<X>()`/`set<X>()` one-liner must NOT be treated as a scannable write-bearing body (today ext-writable dodges this
   only via its `do_methods` filter; a shared fragment used by timers/silent would newly see them). The fragment (or a
   documented caller hook) must skip a method whose name matches `get[A-Z]`/`set[A-Z]` and whose body is a single
   `return`/assignment.

## 5. Golden fixtures all three must agree on (the T1 golden set)

1. **BMisparse (multi-line)** — `@NiagaraProperty(` with `defaultValue = "new BAlarmRecord()"` on its OWN line, before a
   brace-only class open, plus a field-writer method and a schedule/trip in a DIFFERENT method. Expected: class body is
   NOT a method → the two real methods stay separate → CLEAN. Pins invariants 1+2. (The single-line form does NOT pin the
   depth guard — see [[observed-mutation-must-flip-a-fixture]].)
2. **anyNoHardware same-method local** — `BDefrostController.requestDefrostCycle()` shape: a method-LOCAL `boolean` set
   true beside `Clock.schedule` in the SAME method body. Expected: the parser bounds the method so a caller can scope the
   local correctly (timers: LOCAL → not a stuck-flag → CLEAN). Pins method-boundary correctness.
3. **CP-1 adapter** — `CompressorControl.java` pure trip + `BCompressorControl` adapter (`implements BIAlarmSource` +
   `newOffnormalAlarm`). Expected: the parser bounds each real method so the trip's enclosing method is identified
   (silent-protection surfacing). Pins real-tree method bounding.
4. **One-liner method (NEW — from §2)** — `void arm() { flag = true; Clock.schedule(...); }` on ONE line. Expected: the
   method IS detected (peak-depth). This is the fixture that currently DISAGREES across the three copies (timers/silent
   miss it, ext-writable catches it); without it the shared fragment silently inherits whichever behavior the author
   copies. It is the single most important golden fixture for T1.

## 6. Recommended shared-fragment contract (for the C11 T1 proposal)

- Input: `slines[]` (fully // and /* */ stripped), `NR`. Output: `n_meth`, `meth_start[]`, `meth_end[]`, `meth_name[]`.
- Detection: peak-depth (`max_d`), depth guard `>= 2`, Case A regex + Case B `@`-stop backward scan, both keyword lists.
- No caller-specific filter inside the fragment: callers apply `mname in do_methods` (ext-writable), or scan all
  (timers/silent). The fragment is pure "source text → method spans".
- The golden set (§5) is run against a tiny harness that calls the fragment directly and asserts the emitted spans, so
  all three lints inherit ONE verified parser instead of three drifting copies.

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | three parser copies differ in depth metric (net vs peak), Case-B strip source, and caller filter | [CERT] | §1/§3, the three scripts @ 2f3300f |
| 2 | net-depth MISSES a one-liner method; multi-line caught — reproduced on lint-timers | [CERT] | §2, lint-timers runs @ 2f3300f |
| 3 | depth guard, Case-B `@`-stop, both keyword lists are byte-identical across all three | [CERT] | §3/§4, `:202/:147/:326`, `:218/:163/:342` |
| 4 | silent-protection Case-B scans the RAW line with `//`-only strip (D3), unlike the other two | [CERT] | lint-silent-protection.sh:340 |
| 5 | ext-writable's depth guard is redundant with its do_methods gate (still belongs in the fragment) | [INFER] | PR2 read (probe eca8f4c9b); §4.1 |
| 6 | golden set must add a one-liner fixture — the only case the three copies disagree on today | [CERT] | §2 + §5.4 |

Tally: 5 [CERT] · 1 [INFER] · 0 unmarked.

## Connections
- [[observed-mutation-must-flip-a-fixture]] — the C10 close-lesson (a guard is unpinned unless a fixture flips when it is
  dropped); the single-vs-multi-line BMisparse distinction and the depth-guard/do_methods redundancy come from it.
- C10 second-read probes on niagara-research main: `2026-09-07-c10-pr1-second-read.md` (depth guard load-bearing),
  `-pr2-` (do_methods gate + redundant guard), `-pr3-` (silent-protection Pattern B), `-pr1-pins-bite-check`
  (single vs multi-line misparse).
- B831 (C10 lint-precision block) — the S21/S22/S23 root causes these parsers fix.

## Open gaps
- **B832-G1**: the one-line getter/setter skip (§4.4 corollary) is stated as a requirement but not yet pinned — needs a
  golden fixture: a generated `set<X>()` one-liner beside an OPERATOR slot must NOT exempt/scan (measures the peak-depth
  side-effect). Belongs in the T1 golden set alongside §5.4.
- **B832-G2**: D3 (silent-protection Case-B raw-line `//`-only strip) is a latent bug — not shown to flip a real client
  pin yet; needs a `/* ( */`-before-brace fixture to confirm it can mis-name a method before the shared fragment lands.
