# Campaign 9 — research candidates + kit/client/station backlog

Author: investigador1 (Opus). Seeds recorded across Campaign 8 (B805/B808/B810/B812/B813/B814/B815/B816/B817 +
station findings). Ranked by **value × tractability**; each tagged **KIT** (build-n4-module kit), **CLIENT** (our
module source), or **STATION** (PANCCADIA/REFLOW config). Same shape as campaign8-research-candidates.md.

## Ranked backlog

| # | Item | Class | Value | Tract. | Seed | Note |
|---|---|---|---|---|---|---|
| 1 | **Protection-latch seam scaffold** — pure-Java SR latch (set-dominant, first-out capture, trip reason, explicit operator reset, optional B803 step-up on reset, BAlarmSourceExt hookup) + JUnit | **KIT** (fixture) | HIGH | HIGH | B805 §805.3 gap (Tridium ships no SR latch) | **C9 PR1 fixture — built + tested now (§19 PoC below)** |
| 2 | **Write-path coverage lint + the W1-W13 matrix tests** — every dashboard-writable slot has a matrix row/test; lint on `Clock.schedule(≤0)` + link-target writes | **KIT** | HIGH | HIGH | B816 (the user's "solid" mandate) | pure-seam, deterministic; QA has the W-list |
| 3 | **Station config: `fallback=false` on the 22 relays** | **STATION** | HIGH | HIGH | B810 §810.8 / B816 | deploy-safety: relay HOLDS (resistance/compressor ON) on stop/reload with null fallback; config-only, no build |
| 4 | **rt component-lifecycle testable seam** + kit runner `run-station-test.sh` + `build.sh` moduleTestJar step | **KIT** | HIGH | MED | B815 | backs the lifecycle-test mandate; the RUN needs a station (BTest has no deterministic clock) |
| 5 | **DashboardPan servlet hardening** — server-side facet enforcement, `x-niagara-csrfToken` (not just X-Requested-With), parse-error→400, per-ORD write lock | **CLIENT** | HIGH | MED | B813/B803 §803.5/B796 | security; the CSRF-token correction is live |
| 6 | **Structure lints L1-L9** (module layout conformance) | **KIT** | MED | HIGH | B817 | mechanical; statically decidable |
| 7 | **Heartbeat/liveness monitor scaffold** + lint "component owns tickets but no `lastTick`" | **KIT** | MED | HIGH | B812/B775 §775.6 | the author-built independent monitor Tridium doesn't ship |
| 8 | **Health surface in ColdRoomPan/CompPan** — fault-status slot + alarm ext + `lastTick` | **CLIENT** | MED | MED | B808/B805 §805.4 | who-watches at the component level |
| 9 | **`station-load.sh`** probe recipe (companero) | **KIT** (tool) | MED | MED | companero | copy a running station without mounting the FS |
| 10 | **bog-audit CHECK12 + station-logic CHECK13+** (Cuarto 1 tile-number crossing, relay double-source, link direction) | **KIT** (checks) / **STATION** (fixes) | MED | MED | B816 + station | some need station-topology knowledge |
| 11 | **Station config: Cuarto 1 links + re-hide `intervalExpired`** | **STATION** | MED | HIGH | station | config fixes |
| 12 | **Tag dictionary `angeles`** | **CLIENT** | LOW-MED | MED | B814 | nav/search/hierarchy addressability |

## Recommendation
C9 research/build core = **#1 protection-latch fixture** (built below) + **#2 write-path coverage lint & matrix tests**
— together they discharge the user's "I want the change/overlap-testing part solid" mandate. Pair with the one urgent
**STATION** safety fix **#3 (`fallback=false` on the 22 relays)**. #4-#7 are the next KIT tier; #5/#8/#12 are CLIENT;
#10/#11 are station-topology-dependent. Kit lints (#2/#6/#7) extend the C6/C8 lintable-vs-advisory doctrine.

---

# Definitive C9 seed list (consolidated · companero · 2026-09-05)

> One entry per candidate: **gap it closes · evidence (block/retro/PoC SHA) · requires-execution · proposed RED
> shape**. Consolidates the ranked backlog above with every seed recorded across campaign 8. Evidence verified this
> session; anything with no block/PoC/retro is marked `[INFER]`. SHAs are research-repo unless noted.

## KIT — build/test seams & lints (mostly WSL-runnable)
- **S1 · rt component-lifecycle testable seam + `run-station-test.sh` (Windows interop).** Gap: no EXECUTABLE
  coverage of BComponent lifecycle invariants (powerOnTicket-survives-defrost, stopped()-cancels-all) — lint +
  pure-JUnit cannot mount. Evidence: B815 §815.12; PoC **7dd981e2d** (research) + client **c271d36** (worktree
  `poc/lifecycle-btest`, UNPUSHED). **Requires execution: YES — Windows/JACE** (`niagaraTest` launches native
  `bin/test.exe`; WSL interop can run it against a Windows-side COPY of the install). RED: `BColdRoomLifecycleTest`
  GREEN on the fixed tree, FAIL on pre-fix `4f5f1c7`; plus a `run-station-test.sh` that drives `test.exe` via interop.
- **S2 · Protection-latch seam scaffold.** Gap: Tridium ships NO SR latch (set-dominant, first-out capture, trip
  reason, explicit operator reset). Evidence: B805 §805.3 gap; PoC **e31bd60a1** (§19, tests GREEN). Requires
  execution: NO (pure-Java + JUnit). RED: a JUnit a naive latch fails — set-dominant, first-out, reset-guard.
- **S3 · Heartbeat/liveness monitor seam + "owns-tickets-but-no-`lastTick`" lint.** Gap: no independent monitor for
  a STALLED producer (Tridium ships none). Evidence: B812 / B775 §775.6; PoC **fc9caa1ff** (§19, tests GREEN).
  Requires execution: NO. RED: JUnit on stall-detection (age > factor×period) + a lint that FAILs a component with a
  `Clock.Ticket` field but no `lastTick`.
- **S4 · HOA-precedence seam (OFF-lockout dominates the sequence).** Gap: OFF must lock out the actuator even during
  a sequence that "owns" the output (defrost); plain-double HOA leaks. Evidence: B805 §805.11 (`[CERT` bug + `INFER`
  rule); PoC **5a9020fd6**; client fix **20f74f8** (v2.0.6). Requires execution: NO (pure `resistanceCommand` seam).
  RED: `resistanceCommand(inDefrost, mode, auto)` mutation swaps OFF/defrost order → RED.
- **S5 · Write-path coverage lint (`lint-write-path.sh`) + W1-W13 matrix.** Gap: dashboard-writable slots overlap
  control/links silently; no coverage enforcement. Evidence: B816 §816.6; PR19 draft **0590c2b7f**. Requires
  execution: NO (static lint + the pure-seam W-tests already exist). RED: FAIL an OPERATOR-writable dashboard-written
  slot with no matrix row, or a row whose Test column names a test absent from `srcTest/`.
- **S6 · Structure lint `lint-structure.sh` (L1-L11).** Gap: no module-layout conformance gate. Evidence: B817; PR18
  draft **f7a4521ee**; RED already authored: `qa/c8-structure` **c32cb5a**. Requires execution: NO. RED: the c32cb5a
  fixtures (L4 empty lexicon, L7 2-part floor, L9 empty skeleton, L10 `C:\` path, L11 mixed srcTest) all FAIL.
- **S7 · Demand-in-scope lint (B820).** Gap: a staged process must decide WHETHER to run (zero-demand idle) before
  HOW. Evidence: B819 §819.5 (`[INFER]`) concretized by **B820** (on main). Requires execution: NO (static). RED: per
  B820's demand-in-scope contract (the fixture is defined in B820) `[INFER — RED not yet authored]`.
- **S8 · `station-load.sh` probe.** Gap: no way to sample engine/capacity saturation without Workbench. Evidence:
  B806 §806.7/§806.9; recipe **2309d87cd**. **Requires execution: YES — a live JACE** (B806-G1/B811-G1: spy field
  spelling, zero-save proof, auth model). RED: run vs a live station; assert `spy:/metrics` fields parse + `config.bog`
  mtime unchanged (zero side-effect).
- **S9 · commit-msg hook rejecting AI trailers (K11 recurrence).** Gap: AI-attribution trailers keep slipping into
  commits — K11 is advisory only. Evidence: METHODOLOGY K11; PR3 (recurrence). Requires execution: NO (git hook +
  bats). RED: a `commit-msg` hook rejects `co-authored|generated with|claude`; bats proves it blocks the dirty msg
  and passes a clean one.
- **S10 · schema-risk slot attribution to the DECLARING class.** Gap: schema-risk/triage names a slot but not which
  `@NiagaraType` declares it (`ColdRoom.*` vs `DefrostController`). Evidence: B818 (Missing-class forensic) + B800
  §800.8 (the OUTAGE named `RoomPanel.setpoint`/`startDelay`); client PR #5. Requires execution: NO (static — parse
  `module-include.xml` + slot decls). RED: a fixture with a slot on `BColdRoom` vs `BDefrostController` resolves each
  to its declaring class.
- **S11 · `lint-delays.sh` cross-MODULE helper resolution.** Gap: the delay-floor lint resolves a `static long`
  helper one level WITHIN a module, but a floor helper in a shared/dependency module isn't resolved → false FAIL/miss.
  Evidence: retro **campaign8-lint-delays** (D2b — the one-level resolver). Requires execution: NO. RED: a fixture
  where the floor helper lives in a dependency module; the lint must resolve it (or WARN, not false-FAIL).

## CLIENT — our module source
- **S12 · DashboardPan servlet hardening + step-up auth.** Gap: server-side facet enforcement,
  `x-niagara-csrfToken` (not just `X-Requested-With`), parse-error→400, per-ORD write lock, step-up re-auth on a
  critical write. Evidence: B813; B803 §803.5 (Niagara ships no step-up). Requires execution: PARTIAL — pure
  `DashboardDispatch` router tests (WSL) + a live RBAC/CSRF smoke (station). RED: a pure test for the CSRF-header
  contract + a step-up-required test on a critical write path.
- **S13 · Health surface in ColdRoomPan/CompPan.** Gap: a LOGIC fault reaches only the engine console, not the
  operator. Evidence: B808 / B805 §805.4. Requires execution: PARTIAL (station smoke for the alarm). RED: a test that
  a faulted component sets a fault-status slot AND raises a `BAlarmRecord`.
- **S14 · Tag dictionary `angeles`.** Gap: components addressable only by ORD, not by tag/nav/search. Evidence: B814.
  **Requires execution: YES** — B814-G1 (a NEQL query on a live station). RED: build a `BSmartTagDictionary` + one
  `BTagRule`; a NEQL query (`n:equip and angeles:coldRoom`) returns the components with NO per-instance tag.

## STATION — config only (no build)
- **S15 · `fallback=false` on the 22 relays.** Gap: a null fallback HOLDS the relay (resistance/compressor ON) on
  stop/reload. Evidence: B810 §810.8. Requires execution: STATION config. RED: n/a (config); verify each relay's
  effective output goes SAFE on link-down.
- **S16 · Cuarto 1 links + re-hide `intervalExpired`; bog-audit CHECK12 + station-logic CHECK13+.** Gap: station
  topology (tile-number crossing, relay double-source, link direction). Evidence: B816 + station findings. Requires
  execution: STATION (some need live topology). RED: n/a for config; the kit CHECKs are static bog-diff rows.

## PROCESS
- **S17 · Persist drafts to the repo, not `/tmp` scratchpad.** Gap: the `/tmp` scratchpad was WIPED mid-campaign
  today, losing audit notes. `[INFER — observed this session; no block]`. Requires execution: NO. Rule: draft
  artifacts (PR bodies, audit notes) land in `sources/probes/`, never only in `/tmp`.

**Ties to the already-drafted C8 doc PRs:** S5→PR19 (0590c2b7f), S6→PR18 (f7a4521ee); the orchestration/retro loop
(PR16 110f583ad / PR17 d5f979f88) frames how every S-seed closes: research block → spec → RED → apply → retro → fold.
