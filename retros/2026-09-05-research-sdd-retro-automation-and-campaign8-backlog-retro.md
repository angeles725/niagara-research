<!-- review-status: pending -->
<!-- Marker lifecycle: maintainer flips 'pending' → 'applied <date> · kit <sha>' (or 'dismissed') once folded; sweep-retros.sh reads this (METHODOLOGY §18). -->
# Retro — research-sdd KIT · 2026-09-05 · make §18 retros AUTOMATIC in the loop (+ the campaign-8 retro-debt this proves)

> §18 kit retro for explorador's research-sdd team. PROPOSES only (never edits `$KIT`). Two parts: **(A)** the
> process defect the operator flagged — retros are a manual at-STOP step and get skipped during continuous
> chaining; **(B)** the concrete campaign-8 kit-delta backlog that WOULD have been captured incrementally had
> the loop forced retros. Part B is itself the overdue retro for this session's B794–B800.

## A. THE DEFECT — §18 retros do not fire automatically `[CERT — observed this session]`

**Evidence (this session, `companero`):** 1 retro written (`2026-09-05-companero-session-B792-B793-…`) against
**~8 blocks landed after it** (B794, B797, B798, B799, B800 + the §772.4/§799.1/§793-C1/§800.8 addenda) — until
the operator asked directly why retros weren't happening. So block production ran ~8:1 ahead of retros.

**Root cause:** METHODOLOGY §18 fires the retro at **STOP / focus-close**. In a continuous, lead-delegated chain
(one block → next task → next block), a STOP is **never reached** — each block ends by picking up the next lane,
so the at-STOP trigger never arms. The retro is also reconstructed *from memory at the end*, which is exactly
when context is thinnest. Nothing in the loop's RETURN CONTRACT or RESCHEDULE CADENCE makes a retro a
precondition for continuing.

**Why it matters:** each block already emits a "Kit implication" (a kit-delta candidate). Those deltas are the
retro's raw material, but with no incremental sink they must be re-gathered at STOP — and when STOP never comes,
they are only in git commit messages and the operator's trust, not in a `retros/` file the kit sweep reads.

### Proposals (propose-never-apply) — make it automatic, cheapest first
1. **Retro-debt counter in the loop state.** Track `blocks_since_retro` in `RESEARCH-STATE` (or a sidecar).
   `research-sdd-status.sh --next` returns `RETRO-DUE | <n> blocks since last retro` (a typed state like STOP/
   STALE) once `n ≥ threshold` (default 5, or a campaign/PR boundary). The driver MUST write the retro before
   the next block — a hard gate, same shape as the verify-block gate. This is the one that would have fired here.
2. **Incremental retro-debt sink.** Each block's "Kit implication" / RETURN CONTRACT appends one line to a
   running `retros/_debt-<focus>.md` at close (cheapest moment, context still warm). The §18 retro then
   *aggregates* that file instead of reconstructing from memory — turning the retro from an essay into a
   roll-up. Removes the "thin context at STOP" failure.
3. **Cadence hook.** The self-paced RESCHEDULE step checks retro-debt and, when due, schedules a *retro
   iteration* (not a block iteration) next — so an unattended `/loop` run self-inserts retros.
4. **Lint it.** A `verify-state.sh` check (or `sweep-retros.sh`) FAILS when `blocks_since_retro > threshold`, so
   the debt is visible in the same place envelope drift is. Ties into the existing `review-status` machinery.

**Doctrine one-liner for METHODOLOGY §18:** "A retro is not only an at-STOP step. The loop tracks blocks-since-
retro and forces a retro (RETRO-DUE gate) at a threshold or a campaign/PR boundary, assembling it from the
per-block Kit-implication debt sink — never reconstructing from memory."

## B. Campaign-8 kit-delta backlog (the overdue roll-up, would-be incremental) `[CERT unless noted]`

→ **build-n4-module-kit:**
- **triage-console.sh (new tool, B800):** row shape `station·module·exception·message·own_frame·count·first_ts·
  last_ts·attribution·cause`. THREE attribution channels (a frame-only tool misses real outages): (a) `com.angeles.*`
  stack frame; (b) module logger tag `[coldRoomPan|dashboardpan|chihuahua]` — caught 9 chihuahua modifyThread
  findings with no frame; (c) SEVERE `Cannot load station` + `[sys.xml]` warnings naming our types/slots — caught a
  total station-boot OUTAGE (B800 §800.8). Plus locale/encoding robustness: Spanish `INFORMACIÓN/ADVERTENCIA/GRAVE`
  + mojibake (non-UTF-8). [ev: B800]
- **verify-module cert-chain gap (B800 §800.8):** the gate checks `NIAGARA4.SF` PRESENCE only, not chain TRUST
  against the deploy target — a signed jar (chihuahua-rt) loaded UNSIGNED on REFLOW (cert not trusted). Candidate
  `--target-trust <station>` or a documented "presence ≠ trust". [ev: B800 §800.8, CERT-live]
- **Clock-not-executor doctrine (B800 §800.3):** station components must schedule periodic work via
  `javax.baja.sys.Clock`, never `java.util.concurrent` — the SecurityManager denies `modifyThread`; chihuahua's
  `ScheduledExecutorService` (`BChiDashboardService.java:305/314/455`) tripped it 21× across two stations. → a
  `types/logic.md` anti-pattern + a lint candidate (`grep ScheduledExecutorService|Executors\\.` in rt/wb src). [ev: B800]
- **schema-risk OUTAGE is real live (B800 §800.8 closes B795-G1):** a ColdRoomPan-rt reload retyped slots vs the
  persisted `.bog` → `Cannot load station`. Confirms B799's schema-risk fixtures against a live station. [ev: B800 §800.8]

→ **research-sdd kit (prior, still pending in the companero retro):** shared-global STALE bleed in `verify-state.sh`;
  `tool-registry.md` lists no module_nav commands; `preflight.sh` jdk8 false-negative on WSL (fixed in campaign-7 PR8).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 1 retro vs ~8 blocks after it this session → retros ran 8:1 behind | [CERT] | `retros/` listing + git log B794-B800 |
| 2 | §18 fires at STOP; continuous chaining never reaches STOP → retros skipped | [CERT] | METHODOLOGY §18 + this session's chain |
| 3 | Fix = a RETRO-DUE gate + per-block debt sink, not memory reconstruction | [INFER] | proposals A1/A2, grounded in the verify-block gate pattern |
| 4 | Campaign-8 backlog (triage-console 3-channel, cert-chain gap, Clock doctrine) | [CERT] | B800 / B800 §800.8 |

**Tally:** 3 [CERT], 1 [INFER]. Propose-never-apply; no `$KIT` edited.
