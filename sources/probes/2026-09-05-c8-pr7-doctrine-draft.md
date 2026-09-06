# C8 wave-1 PR7 doctrine fold — apply-ready draft

> For the PR7 APPLY worker: paste each block's "READY" lines at the named anchor. investigador1 is the independent
> fidelity reader. Every block carries the grep-before-fold evidence (K6) and `[ev:]` tokens. Kit root:
> `/home/cristian/modulos_niagara_n4/niagara-tools/build-n4-module-kit`. Headings quoted are the EXACT current text.
> NOTE: the kit's conformance rules are **K-numbered (K1–K20 today)**, there is NO R-series/§0/SC in the kit — the
> retro's "R7.x" IDs are mapped below to the kit's real mechanism (a new K-rule or a new section). Apply worker +
> investigador1: reconcile the K-numbering (next free is **K21**; K10 is already a historical gap).

---

## D1 — Timer defense-in-depth (8 layers)
- **Target:** `types/logic.md`, under the existing heading `## Safety fail-modes & timers` (line 13) — insert as an
  index bullet at the TOP of the section (right after the heading).
- **Grep-before (K6):** `grep -niE 'defense.in.depth|8 layer|eight layer' types/logic.md` → 0 hits. The individual
  layers already exist as scattered bullets in this section — the fold ADDS an INDEX, restating nothing. (Layer 4, the
  independent monitor, is the only genuinely new author-design element; it is B812, marked INFER.)
- **READY (one bullet):**
```
- **The 8 timer defense-in-depth layers (index — most already below; each stated once):** (1) anchor a free-running interval to a persistent `BAbsTime`, not `atSteadyState` [see "Anchor a free-running interval…"]; (2) arm in BOTH `started()`+`atSteadyState()` `[ev: corpus B729]`; (3) floor every delay `> 0` before `Clock.schedule`/`schedulePeriodically` [see "Guard any Clock.schedule…"] `[ev: corpus B801]`; (4) an INDEPENDENT liveness monitor on the producer's `lastTick` (stall > 3× period → fault + alarm) — the layer Tridium does NOT ship, author design `[ev: corpus B812]` (INFER); (5) ONE shared cancel path at both command edges + mode enter/exit, not only `stopped()` [see "Cancel EVERY actuation ticket…"]; (6) cancel every ticket in `stopped()` `[ev: corpus B775]`; (7) a manual run-now action + surfaced/safe-defaulted preconditions + a seeded first fire [see "A time-gated auto control needs THREE things…"]; (8) exemplar to copy — the `BAbstractAlarmMonitor`/`BTimeTrigger` self-heal `[ev: corpus B775 §775.6]`.
```

## D2 — Non-positive Clock delay = a lintable hard-fail
- **Target:** `METHODOLOGY.md`, under `## Conformance rules — lintable vs advisory` (line 98) — append the clause to
  the first bullet (the statically-decidable HARD-FAIL list, "A `verify-module.sh`/lint may HARD-FAIL only on…").
- **Grep-before (K6):** `grep -niE 'non.positive|delay.{0,3}(<=|≤).{0,3}0|lint-delays' METHODOLOGY.md` → 0 hits. The
  AUTHORING rule already lives at `types/logic.md:80` ("validate `period > 0`…") and the TOOL at `BUILD-LOOP.md` §5 +
  `SKILL.md` step 5 (`lint-delays.sh`). Do NOT restate either — this fold only CLASSIFIES the check as a lintable
  hard-fail in METHODOLOGY (the one place that list lives).
- **READY (append to the hard-fail bullet, before its closing `.`):**
```
, and **a non-positive `Clock.schedule`/`schedulePeriodically` delay floor** (`toolbelt/lint-delays.sh`, exit 1) — a `≤ 0` delay throws `IllegalArgumentException` and silently kills the timer at runtime, statically decidable from the delay expression `[ev: corpus B801]`
```

## D3 — Inter-module communication (B802)
- **Target:** `types/logic-authoring.md`, NEW `##` subsection immediately AFTER `## Author-side SPIs` (its last
  bullet is the B773 analytics EXCEPTION). NB post-split `## Author-side SPIs` is a short section at
  `logic-authoring.md:4–11` (next heading `## Authoring a point extension` at `:12`); the grep-before (0 hits) holds.
- **Grep-before (K6):** `grep -niE 'inter.module|cross.module|fox:|another station|module-agnostic' types/logic-authoring.md`
  → 0 hits (only same-space `Sys.getService`, B778). New content; extends B778.
- **READY (new subsection):**
```
## Inter-module communication `[ev: corpus B802]`
- **Within a station, runtime comms is module-AGNOSTIC:** `BLink`, service discovery (`Sys.getService(Type)`), and `Subscriber` NEVER check the source module — a cross-module link / lookup / subscription is identical to a same-module one. The only real boundaries are (a) the COMPILE-TIME `<dependency>` on the other module's `Type`, and (b) the `fox:` ORD hop to a SEPARATE station (a real JVM boundary). Extends B778 (same-space services + `Subscriber.event`) with the cross-module + distributed picture. `[ev: corpus B802]`
```

## D8 — signed ≠ trusted (cert-chain)
- **Target:** `build-verify.md`, `## Verify` — add as a PROSE bullet under the `## Verify` heading, NOT inside the
  by-hand bash fence where the `# signed` (`grep NIAGARA4.SF`) presence check lives (~build-verify.md:86). The note is
  doctrine, so it goes in prose next to the fence, not as a shell comment inside it.
- **Grep-before (K6):** `grep -niE 'signed.*(≠|!=|not).*trust|cert.chain|presence only' build-verify.md` → 0 hits.
  Absent. Evidence: B800 §800.8(2) — REFLOW logged `Could not validate cert chain` ×7 for `BChiDashboardService.class`;
  the chihuahua-rt jar is SIGNED but its cert is NOT trusted by that station → it loads as UNSIGNED.
- **READY (bullet under the signed check):**
```
- **`signed` checks PRESENCE, not TRUST:** the gate greps for `META-INF/NIAGARA4.SF` — it confirms a signature EXISTS, not that the signer's cert chains to the target station's trust store. A self-signed / untrusted-CA jar PASSES this gate yet a station enforcing cert-chain trust rejects it at load (`Could not validate cert chain` → the jar loads as UNSIGNED). Verify = presence; trust is a station-side property you cannot assert from the jar alone (candidate: a `--target-trust <station>` check). `[ev: corpus B800 §800.8]`
```

## schema-risk — a MANDATORY pre-deploy gate (live OUTAGE evidence)
- **Target:** `BUILD-LOOP.md`, `## 6. Deploy (station) — operator` (line 51) — add as the FIRST bullet (a hard
  pre-deploy gate).
- **Grep-before (K6):** `grep -niE 'schema.risk|OUTAGE' BUILD-LOOP.md` → 1 hit at §5 (line 47, the pre-GATE lint on
  before/after dirs). §6 has NONE. This fold does NOT re-add the tool — it elevates the §5 check to a hard
  live-deploy gate and attaches the LIVE OUTAGE evidence (B800 §800.8: PANCCADIA `Cannot load station`).
- **READY (first bullet of §6):**
```
- **MANDATORY before a live-station deploy — the §5 schema-risk verdict must be SAFE:** a **LOSSY/OUTAGE** verdict means the new jar's slots no longer match the station's saved `.bog` and it will fail to load. Proven LIVE: an OUTAGE-class retype (`BStatusNumeric`↔`BDouble`, `BRelTime`↔`BComplex`) crashed PANCCADIA after a ColdRoomPan reload — `SEVERE [sys] Cannot load station`, a full outage, not a warning. NEVER deploy a LOSSY/OUTAGE change to a station holding saved data without a bog migration. `[ev: corpus B800 §800.8]`
```

## R7.7 — a cite into a moving tree carries its commit / build
- **Target:** `METHODOLOGY.md`, NEW conformance rule **K21** (append after K20, line 82, in the K-list under
  `## Kit maintenance — retro promotion discipline`).
- **Grep-before (K6):** `grep -niE 'carry the commit|client tree|build-specific|name the build' METHODOLOGY.md` → 0
  hits. NOTE the distinction from **K13** (line 75: "cite QA RED branches by NAME, hashes go stale") — K13 is about
  an IN-FLIGHT peer branch (name is durable); K21 is about ARCHIVAL evidence into an external/decompiled tree
  (pin the exact commit/build so the `file:line` is reproducible). Not a conflict; flag both to investigador1.
- **READY:**
```
- **K21 — A cite into a MOVING tree carries its commit; a decompiled line number names its build:** a `file:line` into a CLIENT module tree or a decompiled install goes stale the moment the tree moves — pin the commit SHA for a client-tree cite, and NAME the build for a decompiled one (the PANCCADIA Linux-snap `Clock.java` line ≠ the `organized/` Windows build's — SAME check, different compile). [ev: corpus B801 §801.4] [ev: corpus B815 §815.10]
```

## R7.10 — Station load budget
- **Target:** `METHODOLOGY.md`, NEW top-level section `## Station load budget` (place after `## Live-verify safety`,
  the current last section, line 104).
- **Grep-before (K6):** `grep -niE 'station load|globalCapacity|load budget|kRU' METHODOLOGY.md` → 0 hits. New. The
  budget CHECKLIST references the timer/persistence rules by verb (count/check), it does not restate them; the full
  budget TABLE + limits stay in corpus B806 §806.8 (pointer only, to keep this ≤ the block cap).
- **READY (new section):**
```
## Station load budget `[ev: corpus B806]`
COUNT before deploying an rt module to a JACE (full budget table + cited limits: corpus B806 §806.8):
- engine-thread cost = Σ(periodic callbacks × frequency); each `execute()`/tick must run ≪ 20 ms.
- every `Clock.Ticket` cancelled in `stopped()`; every delay floored `> 0` (see §Conformance / `lint-delays.sh`).
- no `java.util.concurrent` executor (use `Clock`); no large PERSISTED `String` slot rewritten per action (mark `Flags.TRANSIENT`).
- globalCapacity budget: proxy points < 500 · histories < 125 (incl Audit + Log) · links < 400 · devices < 25 — **> 110 % = the station will NOT boot** (`[CERT-doc]`: Tridium's documented boot semantic, `docPlatform.txt:2458-2459`; the JACE-8000/9000 kRU cap stays OPEN, B806 §806.11).
- guard servlet / linked writes with `isRunning()`; poll with backoff. `[ev: corpus B806 §806.9]`
```

## U — Working profile: Excavador Técnico
- **Target A:** `skill/SKILL.md`, NEW `##` section after the intro line ("Thin launcher…", line 10), before
  `## Resolve the kit` (line 12) — it becomes the first `##` section (SKILL.md is ~60 lines). **Target B:** `METHODOLOGY.md` — a ONE-LINE pointer near the top (after line 1
  `# Common checklist…`), NOT a second copy (K6: full text lives in SKILL.md).
- **Grep-before (K6):** `grep -niE 'Excavador|working profile|first.principle|mindset' skill/SKILL.md METHODOLOGY.md`
  → 0 hits. New in both.
- **READY A (SKILL.md):**
```
## Working profile — Excavador Técnico

Improve the kit and the modules as an "Excavador Técnico" (R&D engineer + system architect + deep-tech investigator + full-stack systems engineer), each stance bound to a kit mechanism:
- **first-principles** (dismantle to physics / binary, rebuild) → why `lint-delays.sh` exists: `Math.max(x,0L)` "fixed" the defrost timer but Niagara rejects a delay `≤ 0`, so the bug lived — root-cause to the framework rule, not the symptom. `[ev: corpus B801]`
- **obsessive rigor** — do NOT stop when it works; stop only when you know exactly WHY it worked AND how to make sure it never fails → every check must BITE, mutation-proven on a real module (K2). `[ev: retro campaign7-plano]`
- **systems thinking** (one bit in a protection latch moves the whole industrial process) → trace each finding to its process consequence. `[ev: corpus B805]`
- **only what RAN** — report a build/test result, never "should work" (B815: build GREEN, run blocked — both recorded). `[ev: corpus B815 §815.12]`
```
- **READY B (METHODOLOGY.md pointer):**
```
> **Working profile:** do kit/module work as an *Excavador Técnico* — first-principles, every check bites, only what ran. Full text: `skill/SKILL.md` § Working profile. `[ev: corpus B801]` `[ev: corpus B815]`
```

## #49 — client-guidance pointer
- **Status:** ALREADY FOLDED — `types/dashboard.md:48` (gate-4 REQUIRED-but-absent → issue #49) and
  `METHODOLOGY.md:56` (the "a 4/5 exemplar that names its gap beats an unqualified 5/5" rule cites #49).
- **Grep-before (K6):** `grep -rniE '#49' <kit>` → 6 hits (the 2 canonical doc-folds `METHODOLOGY.md:56` +
  `types/dashboard.md:48`, plus 4 retro mentions). **Per K6, do NOT re-add.** If C9 intends a
  DISTINCT new client-guidance target (a separate doc/section), the apply worker must specify that target; otherwise
  **SKIP** this delta — the pointer already exists.

---

## Apply-worker checklist
- Paste each READY block VERBATIM at its named anchor; do not merge two deltas into one bullet (K6).
- Every pasted line keeps its `[ev:]` token(s); no token invented.
- K-numbering: use K21 for R7.7; reconcile with investigador1 if another worker also claims K21.
- Close gate: this is a PROMOTION (folds corpus B801/B802/B806/B812/B815/B800 + retros) → close-exit (c),
  `BUILD-LOOP.md §7`: `Retro: promotion (folds …)` trailer + a `retros/INDEX.md` or `BUILD-STATE.md` anchor.
