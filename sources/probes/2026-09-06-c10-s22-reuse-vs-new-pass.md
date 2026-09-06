# C10 S22 design input — does lint-silent-protection.sh's slot→writer follow already know action→doX? (reuse vs new pass)

investigador1, 2026-09-06. Read of `build-n4-module-kit/toolbelt/lint-silent-protection.sh` at kit **cb79676**. Decides whether
S22 (lint-ext-writable per-slot writing-action) reuses the existing follow or adds a new pass. `[ev: kit lint-silent-protection.sh @ cb79676]`

## Answer: HYBRID — reuse the method-boundary + write-pattern machinery, ADD a new @NiagaraAction-aware pass
The lint has **zero @NiagaraAction awareness** (`grep -c '@NiagaraAction'` = 0). It knows @NiagaraProperty only. Its
slot→writer follow (`:124-165`, Pass-1) is a LINE-LEVEL field→slot scan with no method or action scoping. So S22 cannot be
a pure reuse — but it is not all-new either: the method-boundary parser already exists.

## What EXISTS and is reusable
| Machinery | Lines | What it does | S22 reuse |
|---|---|---|---|
| Pass-0 @NiagaraProperty parser | `:58-68` | harvests @NiagaraProperty decls (single + multi-line, paren-balanced) | reuse to enumerate the OPERATOR complex slots (the S22 candidates) |
| Pass-1 slot→writer follow | `:124-165` | for each surface slot, line-scan for `getX().setValue(arg)` / `setX(arg)` and extract the ARGUMENT field — a FIELD→SLOT (what feeds the surface) follow, **whole-file, no method awareness** | reuse the write-PATTERN regexes (`setX(`, `.set(X,`, `getX().setValue(`) to detect "a write to slot X" |
| Main-awk method-boundary parser | section **D** `:250-306` | brace-depth tracking; a method opens only on net-brace-change > 0 (skips single-line getters/setters); excludes class/interface/enum; walks back to the signature line, stopping at annotation lines | **reuse to bind a write to its enclosing method** — exactly what S22 needs to say "this write is inside method M" |

## What is NEW machinery S22 must add
1. **An @NiagaraAction pass** — the lint never reads @NiagaraAction (`:0`). S22 must harvest the action NAMES and the
   method each annotates (the same paren-balanced join Pass-0 uses for @NiagaraProperty, applied to @NiagaraAction).
2. **The action→doX handler resolution (B831-G1)** — the NRE convention binds `@NiagaraAction ackAlarm` to a
   `doAckAlarm(...)` method, so a write to slot X inside `doAckAlarm` IS the action's body. The follow must map the action
   name to its `do<Action>` handler before scanning for the write. (Open gate B831-G1.)
3. **The scoping FILTER** — a write to X counts as "an @NiagaraAction writes X" ONLY if it is inside an action-annotated
   method body OR its `doX` handler, and NOT inside `execute()`/`changed()`/a generated setter. This is the exact trap:
   `faultReset` IS written by `setFaultReset(false)` at `BCompressorControl.java:2025` — but inside `execute()`, so it must
   NOT exempt it (that is why the coarse class-level rule false-negatives and the precise rule flags it). `[ev: corpus B831 §831.2]` `[ev: client BCompressorControl.java:2025 @ ff1b659]`

## Design implication
S22 is **a new @NiagaraAction-aware pass layered on the existing method-boundary parser (section D)** — not a tweak to the
line-level slot→writer follow (:124-165), which is field→slot and orthogonal. It should be built in
`lint-ext-writable-shape.sh` (S22's home per the explore) by (a) lifting the section-D method-boundary logic + the Pass-0
annotation join, (b) adding the @NiagaraAction harvest + doX resolution, (c) the action-body scope filter. Reuse ≈ 60%
(parsers), new ≈ 40% (action awareness + doX + scope). `[ev: lint-silent-protection.sh:58-306]`

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | lint reads @NiagaraProperty but never @NiagaraAction | [CERT] | grep -c @NiagaraAction = 0; Pass-0 :58-68 |
| 2 | Pass-1 slot→writer is line-level field→slot, no method scoping | [CERT] | :124-165 |
| 3 | method-boundary parser exists (section D, reusable) | [CERT] | :250-306 |
| 4 | faultReset write at :2025 is in execute() — the scope trap | [CERT] | BCompressorControl.java @ ff1b659 |
Tally: 4 [CERT] · 0 [INFER] · 0 unmarked.
