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
  reason, explicit operator reset) — and B821 §821.5 confirms NONE of our current 22 trips latch a first-out cause.
  Evidence: B805 §805.3 gap; B821 §821.5; PoC **e31bd60a1** (§19, tests GREEN). Requires execution: NO (pure-Java +
  JUnit). RED: a JUnit a naive latch fails — set-dominant, first-out, reset-guard. Baja wrapper: `step()` from
  `execute()`/`changed()`, a step-up-gateable reset ACTION (B803), and first-out → a **`BAlarmSourceExt`** (the
  tier-1 surface our modules lack, S13). `[ev: corpus B821 §821.5]`
- **S3 · Heartbeat/liveness monitor seam + "owns-tickets-but-no-`lastTick`" lint.** Gap: no independent monitor for
  a STALLED producer (Tridium ships none) — B821 §821.5 frames it as "who watches the watcher": our modules have
  CR-11 self-guards but NO independent monitor that a whole control loop has stalled. Evidence: B812 / B775 §775.6;
  B821 §821.5; PoC **fc9caa1ff** (§19, tests GREEN). Requires execution: NO. RED: JUnit on stall-detection
  (`age > factor×period`, strict; backward clock jump = fresh tick, fail-safe alive) + a lint that FAILs a component
  with a `Clock.Ticket` field but no `lastTick`; Baja wrapper raises a `BAlarmRecord` on STALLED, clears on RECOVERED.
  `[ev: corpus B821 §821.5]`
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
- **S12 · Config-mode step-up login + write audit — TWO surfaces `[TOP-RANKED — user-explicit 2026-09-05]`.**
  **REAL access model (corrected per the viewer's account, verified this session against the code):** the browser
  NEVER talks to the station. READ path = `poller.mjs` (site mini-PC) oBIX Batch every 5 s with a READ-ONLY station
  user → Supabase `latest`/`history`/`events`/`alarms` (`tunnel …/pipeline/poller.mjs:2-5,44` @ 8d738a2). WRITE path
  = 3D viewer → `POST https://api-panccadia…/write {ord,value}` with a Supabase-Auth JWT (app user, ES256, JWKS,
  email allowlist) → `write-server.mjs` (mini-PC `127.0.0.1:18080` behind cloudflared) → oBIX PUT of an
  OPERATOR-writable façade slot with ONE write-capable station user (`write-server.mjs:6,42,51,157,235` @ 8d738a2;
  viewer `panccadia-3d-viewer/index.html` @ dbbbc5c). Writable set = HOA modes + intercambiador (TRANSIENT+OPERATOR,
  LOST on station restart, `:87`); `setpoint` is READONLY in the façade (oBIX "Cannot translate") so the 2D writes it
  via the module SERVLET, not write-server (`:74-75`); alarm ack carries `ackUser` (`:246`).
  **The gap:** NO who-changed-what record today — `write-server` verifies the operator (`verifyJwt` → `by:user.email`,
  `:237`) then DISCARDS it; Supabase `events` are poller-detected with no actor; Niagara AuditHistory sees only the
  single oBIX write user. **Rewrite as two surfaces, same audit schema so the trails merge:**
  - **(A) PRIMARY — viewer + write-server (mini-PC):** a "modo configuración" step-up = re-auth of the Supabase user
    → a short-TTL CONFIG TOKEN bound to `user + purpose`, held server-side IN write-server; every `/write` and
    `/alarms/ack` requires it; explicit `/config/logout` REVOKES it; inactivity expiry `[INFER]`. Audit = JSON-lines
    on the mini-PC AND a Supabase `audit` table `{ts, operator email, ord, old (oBIX GET before the PUT), new,
    result, client ip, config-session id}`, viewable/filterable from the viewer. `[ev: viewer message; tunnel write-server.mjs @ 8d738a2]`
  - **(B) SECONDARY — DashboardPan-ux on the HMI panel via the station servlet (per B803):** station-user re-auth,
    same audit schema so (A) and (B) merge into one trail. `[ev: corpus B803 §803.3/5/6]`
    Two B829-driven client items for surface B: **(B-1)** pass a REAL user Context from the servlet —
    `parent.set(prop, toSet, cx)` with the request user instead of `null` — so the servlet write becomes
    Niagara-audited (the `AuditEvent` gate `ComplexSlotMap.set:662` needs `context.getUser() != null`); small,
    schema-neutral change (**B829-G2**). **(B-2)** confirm an `AuditHistoryService` is installed so `Nre.auditor != null`
    (`:1685`) actually dispatches the event — **B829-G1 is CLOSED by a bog read `[CERT]`**: `tools/bog-nav.py find
    --type h:AuditHistoryService` on PANCCADIA `config.bog` returns `Services/AuditHistoryService` (id
    `/PANCCADIA/AuditHistory`), so the service IS present and the servlet suppression is purely the null-Context gate,
    not a missing service. `[ev: corpus B829]`
  Grounding: B803 §803.3 (server-side re-verify via `BUserService.getUser` + `BPasswordCache.validate`; LDAP via
  `scheme.login`; SAML likely cannot re-verify mid-session `[INFER]`/B803-G1), §803.5 (`x-niagara-csrfToken` for
  surface B), §803.6 (short-TTL token bound to session+user+ORD+purpose, server-side allowlist, audit every step-up);
  B816 (write path); B804 (`AuditHistoryService` as a Niagara-native second record for surface B).
  **Caveats:** the write-server Supabase `audit` (config-session email) is the SINGLE SOURCE OF TRUTH for operator
  identity — now on a `[CERT]` footing, settled by CODE in **B829 (`d26305d21`)**: the framework gates the audit event
  at `ComplexSlotMap.set:662` `if (context != null && context.getUser() != null)`, so surface B's servlet
  `parent.set(prop, val, null)` (NULL Context) is NOT audited at all — the `AuditEvent` is SUPPRESSED, not merely
  unattributed, regardless of an installed `AuditHistoryService`; surface A's oBIX PUT DOES pass `ot.getUser()`
  (`ObixUtils:558`) so it IS audited, but to the SHARED station login user, never the real operator. Either way Niagara
  cannot attribute the operator, so the write-server trail is the only identity record. Writes are TRANSIENT (lost on
  station restart); public signup disabled + email allowlist. Requires execution: PARTIAL — write-server tests + pure
  `DashboardDispatch` router tests run off-station; the live oBIX-PUT + Supabase audit + AuditHistory smoke needs the
  mini-PC + station. `[ev: corpus B829]`
  **oBIX body form (settled):** the write-server should target the CHILD leaf ORD `${ord}/value` with a bare
  `<real val="N"/>` — a simple `BSimple` write with NO silent-zero hazard, served + `writable="true"` and proven to
  propagate ~1.5 s (B826-G1/G2, records §8/§9). The write-server's NUM builder already emits a bare `<real>` (record
  §4), so this needs no new wrapped-`obj` body type. The wrapped-`obj`-to-parent-slot (B825) stays the fallback only.
  `[ev: corpus B826]` `[ev: corpus B825]`
  **RED shapes — (A) write-server:** write WITHOUT a config token → 403; step-up with WRONG password → 401; valid →
  200 + exactly ONE audit row `{email, ord, old, new}`; `/config/logout` → next write 403; token of user A after
  logout → 403; audit APPEND FAILURE → the write still lands + an error/alarm row. **(B) `DashboardDispatchTest`:**
  the same six cases against the servlet (no-step-up→401/403, wrong-pw→401+no-token, valid→200+one audit line,
  logout→invalid, A-after-logout→rejected, audit-fail→write-lands+alarm; audit-failure never fails the write, DWS1
  gate 5). `[ev: corpus B803]` `[ev: corpus B816]` `[ev: corpus B804]` `[ev: corpus B813]`
  **Write-path detail (viewer-confirmed, verified `write-server.mjs`):** every write lands on
  `${FACADE_PATH}/CuartoN/<slot>` = `/config/Services/DashboardService/CuartoN/<slot>` (`FACADE_PATH`:41, ord regex
  `^Cuarto([1-5])/(.+)$`:229, PUT:235) — NEVER `ColdRoom_N`/`CompressorControl`. Body per family (`obixBody`:95-120):
  NUM/HOA/AUTOOFF → `<real>` (HOA `0=Auto 1=On 2=Off` as a double), BOOL → `<bool>`, reltime → `<reltime val="PT..S"/>`;
  the `WRITABLE` map (:77-88) lists the writable slots and **does NOT include `setpoint`**.
  **The setpoint refusal — root cause (verified):** `BRoomPanel.setpoint` ALREADY carries `Flags.SUMMARY|Flags.OPERATOR`
  and is a `BStatusNumeric` COMPLEX (`BRoomPanel.java:125-128` annotation / `:654` generated, client `deed38c`; lead
  cited a109249) — a bare `<real>` PUT to a complex sink → oBIX "Cannot translate"; an earlier RETYPE attempt crashed
  the station on bog load (B800 §800.8). **Decision recorded with the user:** an ADDITIVE `setpointCmd` (plain `double`,
  `SUMMARY|OPERATOR`) on `BRoomPanel`; `changed(setpointCmd)` → `setSetpoint(new BStatusNumeric(v))`; init from
  `setpoint` at start; LINKS UNTOUCHED; `schema-risk.sh` must read SAFE against the real bog before deploy; write-server
  adds `setpointCmd` to `WRITABLE` as NUM; the viewer drops the readonly and writes `setpointCmd` while DISPLAYING
  `setpoint`. RED (additive path): a pure panel-core test (`cmd → setpoint value + status ok`), `DashboardDispatchTest`
  servlet path, a `schema-risk` SAFE pin, a write-server unit test for the new key. **Whether the additive slot is even
  NEEDED is under research:** B822 (investigador1) = additive alternatives + retype schema-risk; **companero's no-code
  block** (next § after B822) = can a plain-double oBIX PUT reach `setpoint` with NO module change at all. Pending
  evidence: B822, the no-code block, + a read-only oBIX GET on that ORD from the mini-PC if Cristian authorizes.
  **Requires-execution live tests (authorized, TEST room only):** **B822-G1** — a live smoke that
  `POST /obix/config/…/applySetpoint` with `<real val=".."/>` INVOKES and that the oBIX login user's `OPERATOR_INVOKE`
  gates it (evidence B822 `9d1a336b1`); **B823-G1** — read-only bog/GET to settle the link-target + the oBIX
  `BStatusNumeric` GET encoding; **B823-G2** — the servlet `POST /dashboardpan/api/setpoint` proof (200 + one auditLog
  line). Pair all three in one read-only-first live session. `[ev: corpus B822]` `[ev: corpus B823]`
- **S13 · Health surface — raise the RT protection tier from 2 to 1 (alarm console).** Gap (B821 §821.4, verified
  `fbe9009`): our RT protection surfacing TOPS OUT at tier 2 (a plain `SUMMARY` slot only someone at Workbench/SPA
  sees); tier 1 (the alarm console) is ENTIRELY unused — a clean grep of `ColdRoomPan-rt/src` + `CompPan-rt/src` for
  `BAlarmSourceExt|BAlarmRecord|BAlarmService` returns **ZERO**. So no freeze trip, stuck contactor, or failed
  compressor start ever reaches the operator's alarm queue. The **concrete silent slots to add** (B821 §821.6):
  CR-3 freeze-reason (`freezeTripped` is a private field, `BEvaporatorUnit.java:1287`), **CP-1 low-suction — the
  asymmetry tell**: `dischargeHighAlarm` exists but there is NO symmetric `suctionLowAlarm`, and the low-suction
  (vacuum) trip is the more damaging of the pair; CP-4/CP-5/CP-6 anti-cycle (minOff/minOn/stageDelay in private
  arrays); CR-10/CR-11; the HOA TRANSIENT restart-revert (HAND silently lost on restart). Evidence: B821 §821.4/§821.6;
  B808 (a LOGIC fault must reach the operator) / B805 §805.4. Requires execution: PARTIAL (station smoke for the
  alarm). RED: a faulted component sets a fault-status slot AND raises a `BAlarmRecord`/`BAlarmSourceExt` (tier 1),
  reaching the alarm queue — not just a SUMMARY slot. Highest-value protection improvement across all three modules.
  (S13 is the concrete per-module discharge of the cross-cutting finding **S18**.)
  `[ev: corpus B821 §821.4]` `[ev: corpus B821 §821.6]` `[ev: corpus B808]`
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

## CROSS-CUTTING FINDING
- **S18 · Protection trips NEVER reach the alarm console (KIT + CLIENT, value HIGH).** Finding (B821, `f960f2997`,
  verified `fbe9009`): our RT control modules raise ZERO alarm-console events — a clean grep of `ColdRoomPan-rt/src`
  + `CompPan-rt/src` for `BAlarmSourceExt|BAlarmRecord|BAlarmService` returns **ZERO**. Every protection surface
  tops out at a plain `SUMMARY` slot (`dischargeHighAlarm`, `stuckAlarm`, `condenserNFault`, `freezeTripped`-is-a-
  private-field), so no trip reaches the operator's alarm queue (only someone actively at Workbench/SPA sees it).
  B821 §821.4 = the four surface tiers (ours top out at tier 2; tier 1 unused); §821.6 = the silent list. **Fix:** a
  `BAlarmSourceExt` on the critical trips → pushes to the alarm queue with ack/unack/history. This is the CROSS-CUTTING
  design gap; **S13 is its concrete per-module discharge** (which slots to add). **Kit doctrine target:**
  `types/logic.md` §"Protection anatomy" (the four surface tiers) + a SILENT-PROTECTION lint candidate (a sibling of
  B820's demand-in-scope check: a protection trip whose only surface is a private field / bare SUMMARY slot, with no
  `BAlarmSourceExt`, → WARN). Requires execution: PARTIAL (station smoke) — **B821-G2** is the live alarm-console
  confirm. RED: a critical trip raises a `BAlarmRecord`/`BAlarmSourceExt` reaching the alarm DB, not just a SUMMARY
  slot; the lint FAILs a trip with no alarm surface. `[ev: corpus B821 §821.4]` `[ev: corpus B821 §821.6]`
  **THE FIX IS NOW SPEC'D — B827 (`ff0ce3f5b`) `[CERT]`:** `BAlarmSourceExt` needs a `BControlPoint` parent, so a
  custom `BComponent` raises the alarm one of TWO legal ways: **(A) declarative** — add a child point (`BBooleanPoint`
  / `BNumericPoint`) to the component, drive it from the trip, attach a `BAlarmSourceExt` with a pluggable
  `offnormalAlgorithm` (`BBooleanChangeOfStateAlgorithm` for CR-3 freeze, `BOutOfRangeAlgorithm` for CP-1 low-suction);
  cost = one child point per trip. **(B) programmatic** — the component implements `BIAlarmSource` (its `ackAlarm`
  declared as a `@NiagaraAction`), builds `AlarmSupport(this, "defaultAlarmClass")` in `started()`, and on the
  OFFNORMAL edge calls `support.newOffnormalAlarm(alarmData)`; ONE source per component fires MANY trips (scales for
  CompPan's ~5). Both route a `BAlarmRecord` whose **`sourceState = offnormal`** — exactly what the DashboardPan alarm
  `bql` (`where sourceState='offnormal' or 'fault'`, `BDashboardServlet.java:502`) already selects, so the console AND
  the dashboard surface it with ack/unack/history. low/high is a key in `alarmData`, NOT a `sourceState` value, so the
  offnormal query still catches an out-of-range trip. **Adding it is schema-SAFE** (additive child point / additive
  action + transient `AlarmSupport`; no retype/remove — B795). Copy-ready **CP-1** (low-suction numeric) + **CR-3**
  (freeze boolean) sketches are the RED shape (B827 §827.3/§827.4/§827.6). Open gaps: **B827-G1** (bounded — the
  offnormal EDGE detection: Pattern B must fire `newOffnormalAlarm` only on the normal→offnormal transition with a
  `toNormal` on recovery + `started()` re-seed, a `wasOffnormal[]` per-trip state) and **B827-G2** (requires-execution —
  confirm on a live station that a routed `defaultAlarmClass` alarm reaches the REFLOW/PANCCADIA console + the
  DashboardPan panel end-to-end, = the B821-G2 tier-1 live confirm). `[ev: corpus B827]`
- **S19 · `ext-writable-shape` lint (KIT, value MED).** Gap: a slot that EXTERNAL clients write must be a SIMPLE value
  or have a writing ACTION — a bare complex `OPERATOR` property (`BStatusNumeric`/`BStatusBoolean`/`BStatusEnum`)
  either rejects the oBIX write ("Cannot translate") or, via the wrapped-`obj` shorthand, SILENTLY writes a default
  (the live silent-zero, B823 §823.2). Evidence: B823; retro `obix-statusnumeric-wrapped-put`; the slot-type doctrine
  (PR15). Requires execution: NO (static lint). RED: a fixture with a bare `Flags.OPERATOR` `BStatusNumeric` and no
  writing action → **WARN** `ext-writable-shape`; a plain `double`/`boolean` OR a complex-with-`@NiagaraAction` → clean.
  `[ev: corpus B823]` `[ev: retro obix-statusnumeric-wrapped-put]`
- **S20 · Time-slice compressor rotation — CLIENT module change, CompPan-rt (value HIGH, user-explicit 2026-09-06).**
  Gap (verified `CompressorControl.java @ a109249`): rotation happens ONLY at stage events — stage UP picks the available
  compressor with the LEAST hours (`pickLeastHoursOff`, `:226`), stage DOWN drops the running one with the MOST hours
  (`pickMostHoursOn`, `:238`); §Rotation `:31-36`. So under STEADY demand (`onCount == target`, no stage event) the same
  compressors run for as long as demand holds — no wear equalization until demand changes. The client wants a
  **TIME-SLICE rotation**: a running compressor swaps out after a configurable CONTINUOUS run (e.g. 3 h) for the idle
  available compressor with the least hours. **Design (additive, pure-core):** two new `@NiagaraProperty` on
  `BCompressorControl` — `rotationInterval` (`BRelTime`, `SUMMARY|OPERATOR`, **0 = disabled = today's behaviour
  byte-identical**) and `rotationMode` (an enum — **make-before-break** default: start the incoming compressor first,
  then after `stageDelay` stop the outgoing; **break-before-make** the alternative). Logic lives in
  `CompressorControl.step` AFTER the target computation, gated by the SAME rules as staging: `minOn`/`minOff`,
  `stageDelay`, HOA **OFF excluded**, **HAND untouched**, **never a stage-up on `dischargeHigh`**, **never below the LP
  floor**, and **no swap when only one compressor is available**. The rotation clock is per-compressor from `cmdSince[]`
  (already tracked, `:71`); `condenserNHours` keep integrating on the COMMANDED state (`:44`), so a swap does not distort
  the hours ledger. **RED shape:** pure `CompressorControlTest` cases (swap after interval; NO swap below interval; no
  swap while the idle candidate is inside `minOff`; make-before-break ordering — incoming ON, then outgoing OFF one
  `stageDelay` later; disabled at 0 keeps today's behaviour BYTE-IDENTICAL). **Schema-risk SAFE** pin (two additive
  slots, no retype/remove — B795). Dashboard exposure of `rotationInterval` per the slot-type doctrine (a `BRelTime` is
  written by the write-server as `<reltime val="PT3H"/>` — a simple value, no complex-write hazard). Requires execution:
  build + pure tests off-station; live confirm on PANCCADIA. `[ev: client CompressorControl.java @ a109249 §Rotation :31-36, :226, :238, :44, :71]` `[ev: corpus B822 (additive-code)]` `[ev: corpus B823 (slot-type doctrine)]` `[ev: corpus B795 (schema-risk)]`
  - **User FASE 1/2/3 explanation — ANSWERED this cycle** (recorded per the user's ask): the CompPan control is staged
    (`CompressorControl.java` javadoc @ a109249). **FASE 1** (shipped) = demand-count staging (rooms' `room1..4Calling` →
    `demandCount`) + amperage proof-of-run (running/fault/stuck by measured amps, VISUAL-only, never controls staging) +
    real run-hours lead/lag rotation persisted in `condenserNHours`; suction/discharge pressures wired as SAFETY LIMITS
    only. **FASE 2** = the pressure MODULATOR (suction-band control on the same slots, pure-logic, no re-slot).
    **FASE 3** = floating suction (`floatingSuction`/`coilTD`/`localAtmPsi`; `SSTreq = setpoint − coilTD`; León ~1,800 m →
    ~11.82 psia). S20's time-slice rotation is an additive FASE-1 refinement (rotation cadence), independent of 2/3.

**Ties to the already-drafted C8 doc PRs:** S5→PR19 (0590c2b7f), S6→PR18 (f7a4521ee); the orchestration/retro loop
(PR16 110f583ad / PR17 d5f979f88) frames how every S-seed closes: research block → spec → RED → apply → retro → fold.

**Delivered research blocks (C9):**
- **B821 · Protection anatomy** — the RT deep-dive the user asked for (protections: what fires · who fires · who
  watches; heartbeat; latch). AUDIT-first at client `fbe9009`: 22 protections classified on three axes; cross-cutting
  finding = the RT modules raise ZERO alarm-console events (every surface is a plain SUMMARY slot); honest silent list
  (CR-3/CR-10/CR-11, CP-1/CP-4/CP-5/CP-6). GROUNDS **S13** (health surface = the tier-1 `BAlarmSourceExt` fix),
  **S2** (protection-latch = latched first-out surface), **S3** (heartbeat = independent monitor). Kit: `types/logic.md`
  §"Protection anatomy" + a silent-protection lint candidate (sibling of B820). Gaps B821-G1 (lint shape) / B821-G2
  (live alarm-console confirm).

| S21 | lint-timers companion-flag false positive (require class-FIELD + same-method schedule) | KIT | Low-noise: unblocks a clean report-module on ColdRoomPan-rt; removes a spurious FAIL | High (bats + awk scope fix) | C10 | Evidence BDefrostController.java :718 local vs :808/:810/:850; not a client bug; kit ticket drafted 2026-09-06 (probe 3bfa5743f) |
