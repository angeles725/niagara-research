# B715 — Module dev workflow, testing & debugging + runbook finalization (WF5, focus close): pure-Java model tests in WSL, station-side debug, and the common errors

> Focus: **module-dev-workflow** · Gap **WF5** (test/debug + deliverable) — FOCUS-CLOSING block. Block TYPE =
> **DESIGN/RUNBOOK**. Finalizes `docs/module-dev-workflow.md`. Marker `[CERT]` where re-citing verified code;
> `[INFER]` for the runbook framing.

## 715.1 — Unit testing: pure-Java model classes in a plain JRE

[CERT] The shop's `run-tests-wsl.sh` pattern runs JUnit against the module's **pure-Java model classes** — the
classes that carry ZERO Niagara types ([Block 637] §637.3, [Block 654]). chihuahua-wb's `model/` package
(`PendingLink`, `PendingLinkBuilder`, `LinkSlotNameUtil`) and the parsing/name-generation logic are testable in
WSL without a running station. **The design enabler:** keep business/parse/compute logic in pure-Java helpers,
away from `BComponent`; then it unit-tests fast in plain JUnit. Anything that touches `BComponent`/the station
API needs a station (or a heavier harness) and is slower to test — minimize it.

## 715.2 — Station-side debugging

[CERT+INFER]
- **Logs** — the station's `LogHistory` captures `logName/severity/message/exception` ([Block 701]); log through
  your module's logger at meaningful levels. A `SEVERE`/`Exception` there is your first debug signal.
- **The `changed()` swallow** means exceptions on the engine thread are logged, not thrown ([Block 650]) — so
  check the logs, not a crash, when a slot update misbehaves.
- **Type/registration issues** surface as `BTypeSpec.resolve` failures or a type "not found" — check the class is
  in `module-include.xml` (WF2 guard).
- **Profile issues** — if a `-wb` feature "isn't there" on a JACE/supervisor, that is the daemon not loading `wb`
  ([Block 630]); move station logic to `-rt`.
- **Signing/verify** — if the station refuses the module, check `moduleVerificationMode` + the signature
  ([Block 398]/[Block 639]).

## 715.3 — Common errors → fixes (quick table)

[INFER, consolidating WF1-4]

| Symptom | Likely cause | Fix |
|---|---|---|
| compile error in a slot constant | stale AUTO region | run `:slotomatic` |
| `BTypeSpec.resolve` fails / type not found | class not in `<type>` | add to `module-include.xml` |
| feature missing on headless station | logic in `-wb` | move to `-rt` |
| station won't load the module | signing/verify mode | check keystore alias + `moduleVerificationMode` |
| slot change does nothing, no crash | exception swallowed on engine thread | read `LogHistory` |
| jitter / slow station | blocking the engine thread | dispatch off-thread |
| write allowed for a read-only user | userless/ungated write path | gate with `BPermissions.has` + `runAsUser` |

## 715.4 — The runbook

[CERT] `docs/module-dev-workflow.md` is finalized with five sections: the toolchain (§1), the codegen round-trip
(§2), the authoring artifacts (§3), the dev loop (§4), and testing/debugging (§5). Together with
`module-best-practices.md` it is the complete answer to "how the tools are used and what the process is" — the
runbook (process) beside the guide (rules).

## 715.5 — Focus verdict

[INFER] The shop's dev workflow is **sound and already tooled**: a real deploy wrapper (`ng-deploy.sh` with
type-verify + phase exit codes), convention-driven signing, version-targeting by SDK home, and a WSL unit-test
harness for pure-Java logic. The two habits worth reinforcing (both surfaced as best-practice fixes): run
Slotomatic reliably on annotation changes (automate the mode decision), and keep testable logic in pure-Java
helpers. Nothing in the workflow needs rebuilding.

## Connections

- Testing → focus `own-modules-audit` [Block 637], `chihuahua-source` [Block 654]. Station logs → [Block 701];
  engine-thread errors → [Block 650]; profile → [Block 630]; signing/verify → [Block 398]/[Block 639]. Prior WF
  blocks → [Block 711]–[Block 714]. Companion guide → [Block 710] (`docs/module-best-practices.md`). Deliverable:
  `docs/module-dev-workflow.md`.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | pure-Java model classes unit-tested in WSL (run-tests-wsl.sh) | [CERT] | [Block 637]/[Block 654] | cited |
| 2 | station debug via LogHistory + swallowed engine-thread exceptions | [CERT] | [Block 701]/[Block 650] | cited |
| 3 | common-error→fix table | [INFER] | 715.3 | reasoned |
| 4 | runbook finalized (5 sections) | [CERT] | docs/module-dev-workflow.md | delivered |

**Tally:** [CERT] ×2 · [INFER] ×2. Block TYPE = **DESIGN/RUNBOOK** — ratio expected-high. Re-cites verified blocks.

## Focus status

**WF5 CLOSED → module-dev-workflow investigable = 0 → focus STOP.** 5/5 gaps closed (WF1–WF5). Deliverable
`docs/module-dev-workflow.md` complete. No requires-execution, no blocked gaps. Next: §18 retro + push.
