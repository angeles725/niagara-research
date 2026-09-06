# Exploration (draft) — build-n4-module-campaign9

**Date**: 2026-09-06 | **Phase**: explore (terrain draft for the C9 SDD chain) | **Status**: draft
**Engram**: `sdd/build-n4-module-campaign9/explore` (proposed)
**Kit**: v0.19.0 at C8 close | **Client modules**: ColdRoomPan 2.0.7, DashboardPan 2.0.3, CompPan 2.1.1
**Tunnel (mini-PC poller/write-server)**: e4b42b0
**Author**: research chain (this draft = terrain so the C9 SDD chain starts from a real explore, not a blank one)

> Shape mirrors `openspec/changes/build-n4-module-campaign8/explore.md`. Every claim carries a token; SHAs are
> research-repo (`niagara-research`) unless noted `[tunnel]`/`[client]`. Nothing here is invented: where a C9 seed has
> no authored RED branch yet, it is marked **RED not yet authored**, not given a fabricated tip.

---

## 1. Mandate

The user's C9 asks, as relayed and recorded across the C8 close (each is a live seed, not a wish-list line):

1. **Config-mode step-up login + write audit — TWO surfaces (S12, TOP-RANKED, user-explicit 2026-09-05).** A "modo
   configuración" re-auth gating every `/write` and `/alarms/ack`, with a who-changed-what audit trail. Surface A =
   viewer + write-server (mini-PC, PRIMARY); surface B = DashboardPan-ux servlet (HMI panel, per B803), same audit
   schema so the trails merge. `[ev: corpus S12]` `[ev: corpus B829]`
2. **Setpoint child-ORD write path — shipped.** Additive `applySetpoint(BDouble)` / `setpointCmd` decision recorded;
   tunnel write-server targets the CHILD leaf `${ord}/value` with a bare `<real val="N"/>` (B826 preferred form),
   shipped `e4b42b0` / write-server v3.3 `[tunnel]`; two proven forms = child bare-`<real>` (preferred, B826-G1/G2) +
   wrapped-`obj`-to-parent-slot (fallback, B825). `[ev: corpus B826]` `[ev: corpus B825]`
3. **Slot-type doctrine in the kit.** The ext-writable-shape rule (SIMPLE value OR writing ACTION; a bare complex
   `OPERATOR` property rejects or silent-zeroes) belongs in `types/` doctrine + a lint (S19). `[ev: corpus B823]`
4. **Alarm raising (S18 / B827).** RT modules must raise tier-1 alarm-console events (they raise ZERO today); B827
   spec's the two legal paths (declarative child-point + `BAlarmSourceExt`; programmatic `BIAlarmSource` +
   `AlarmSupport.newOffnormalAlarm`). `[ev: corpus B821 §821.4]` `[ev: corpus B827]`
5. **HOA frozen enum — NEW modules only (B828 §828.7).** `BFrozenEnum` carries its range intrinsically (no facet);
   the carve-out: a cross-module-LINKED HOA stays a double (a deleted `BHoaMode` caused a live "Missing class
   ColdRoomPan:HoaMode"), so the frozen-enum doctrine is for NEW modules, not a retrofit of the linked ones.
   `[ev: corpus B828 §828.7]`
6. **Unified audit trail (B829).** The write-server Supabase `audit` (config-session email) is the SINGLE SOURCE OF
   TRUTH for operator identity: servlet `parent.set(prop,val,null)` (NULL Context) is NOT audited (`AuditEvent`
   suppressed at `ComplexSlotMap.set:662`); oBIX PUT IS audited but to the shared station login user. `[ev: corpus B829]`
7. **Silent-protection lint (S18-lint / B824).** A protection trip whose only surface is a private field / bare
   SUMMARY slot, with no `BAlarmSourceExt`, → WARN; sibling of B820's demand-in-scope check. `[ev: corpus B824]`
8. **Demand-in-scope lint (S7 / B820).** A staged process must decide WHETHER to run (zero-demand idle) before HOW.
   `[ev: corpus B820]`
9. **Ext-writable-shape lint (S19).** The static lint discharging ask #3. `[ev: corpus B823]`
10. **Air-defrost trial (rooms 1/2/4).** Station-only config OR the `airDefrost` module flag seed — path not yet
    chosen. `[ev: corpus S16]` `[ev: memory coldroompan-defrost-time-le-0-bug]`
11. **Write-path matrix completion W14-W22.** The PR19 smoke exposed the matrix stops at W13; the remaining rows
    W14-W22 need authoring against the real writable set. `[ev: corpus S5]` `[ev: corpus B816 §816.6]`
12. **Kit conventions delta from lesson 11.** One module-root convention + K22 (the conventions delta the C8 close
    retro named). `[ev: corpus C8 close-fold lesson 11]` `[INFER — K22 label from the close retro; confirm exact K-number at chain start]`

**Standing constraints (chain-wide):** tests must genuinely bite (mutation-proven, campaign-7 meta-lesson 1);
conventional commits, NEVER Co-Authored-By / AI attribution; Spanish = neutral/international. `[ev: corpus C7 close]`

---

## 2. Current State

| Axis | State at C9 open | Token |
|---|---|---|
| Kit | v0.19.0 (C8 close) — lint-delays, lint-timers(+ext), lint-servlet, lint-structure, verify-module, report-module, schema-risk mandatory gate, sweep-fold-audit, new-retro, kit-ticket | `[ev: corpus C8 close]` |
| Client — ColdRoomPan | 2.0.7 | `[client]` |
| Client — DashboardPan | 2.0.3 | `[client]` |
| Client — CompPan | 2.1.1 | `[client]` |
| Tunnel (poller + write-server) | e4b42b0; write-server v3.3 ships the child-ORD `${ord}/value` bare-`<real>` setpoint path | `[tunnel]` `[ev: corpus B826]` |
| Station (PANCCADIA) | `AuditHistoryService` INSTALLED (`/PANCCADIA/AuditHistory`, bog read `[CERT]`, B829-G1 CLOSED); the 22 relays still null-fallback (S15 STATION fix open) | `[ev: corpus B829]` `[ev: corpus B810 §810.8]` |

**Protection-surface baseline (the finding that drives asks #4/#7):** the RT modules (`ColdRoomPan-rt/src`,
`CompPan-rt/src`) raise ZERO alarm-console events — a clean grep of `BAlarmSourceExt|BAlarmRecord|BAlarmService`
returns nothing; every protection tops out at a plain `SUMMARY` slot (tier 2). Verified at client `fbe9009`.
`[ev: corpus B821 §821.4]` `[ev: corpus S18]`

---

## 3. Evidence Inventory

### 3.1 C9 seeds (campaign9-research-candidates.md, `a5a2e5cba`+)

S1 rt component-lifecycle testable seam (exec: YES, Windows/JACE) · S2 protection-latch seam (PoC `e31bd60a1`) ·
S3 heartbeat/liveness monitor (PoC `fc9caa1ff`) · S4 HOA-precedence seam (PoC `5a9020fd6`, client `20f74f8`) ·
S5 write-path coverage lint + W-matrix (PR19 `0590c2b7f`) · S6 structure lint L1-L11 (PR18 `f7a4521ee`) ·
S7 demand-in-scope lint (B820) · S8 `station-load.sh` (exec: YES; recipe `2309d87cd`) · S9 commit-msg AI-trailer hook
(K11) · S10 schema-risk slot→declaring-class · S11 lint-delays cross-module helper · S12 config step-up + write audit
(TOP-RANKED) · S13 health surface tier-1 per-module (B821 §821.6) · S14 tag dictionary `angeles` (exec: YES) ·
S15 `fallback=false` on 22 relays (STATION) · S16 Cuarto-1 links + bog-audit CHECK12 (STATION) · S17 persist drafts
to repo · S18 protection trips never reach console (CROSS-CUTTING; B827 spec'd) · S19 ext-writable-shape lint (B823).
`[ev: corpus campaign9-research-candidates.md]`

### 3.2 Delivered research blocks (B820-B829, one line each)

| Block | SHA | One-line finding | Token |
|---|---|---|---|
| B820 | `16b635f0f` | demand-in-scope lint — a staged process decides WHETHER before HOW (zero-demand idle) | `[CERT]` |
| B821 | `f960f2997` (re-anchored `fbe9009`) | protection anatomy — RT modules raise ZERO alarm-console events; 22 trips classified what/who-fires/who-watches; none latch first-out | `[CERT]` |
| B822 | `9d1a336b1` | additive-code setpoint write — `applySetpoint(BDouble)` action oBIX-native (recommended); `setpointCmd` double (fallback); retype = OUTAGE | `[CERT]` |
| B824 | `b5060f60b` | silent-protection lint — effect-slot exemption + pure-model→adapter follow + name allowlist; CP-1 FLAG, CP-2/defrostSkipped clean | `[CERT]` |
| B825 | `c5ac2ca57` (upd `3e8dc8b45`) | propagation mechanism — oBIX write = top-slot replacement, SYNCHRONOUS propagation; lag is the reader poll; bubbling `[CERT-live]` via B826-G2 | `[CERT]` |
| B826 | `b53fdea9d` (upd `3e8dc8b45`) | child-ORD routability — child `/value` NOT advertised (agent leaf-collapse) but RESOLVABLE; both gaps CLOSED `[CERT-live]`; child bare-`<real>` = PREFERRED write form | `[CERT-live]` |
| B827 | `ff0ce3f5b` | alarm authoring — `BAlarmSourceExt` needs a `BControlPoint` parent → child-point OR programmatic `BIAlarmSource`+`AlarmSupport`; corrects B8 §8.1.4/§8.1.5 | `[CERT]` |
| B828 | `e1d58acc7` (§828.7 `c1cdef272`) | HOA frozen enum — `BFrozenEnum` carries range intrinsically; §828.7 carve-out: cross-module-LINKED HOA stays double | `[CERT]` |
| B829 | `d26305d21` (G1 CLOSED `7218fdad7`) | audit trail — servlet null-Context set NOT audited (`:662` gate); oBIX PUT audited to oBIX login user (`:558`); write-server Supabase = single source of truth; AuditHistoryService installed | `[CERT]` |

### 3.3 QA RED branches (by branch + tip)

**The five C9 REDs.** The three kit branches are on `origin` and VERIFIED this session — tips match the lead's report,
pin counts confirmed, and each is a **true test-first RED**: the lint script is ABSENT on the branch while the bats
file is present, so the suite FAILs until C9-apply builds the lint. Two carry explicit mutation-bite pins (SP8
`mutation_removing_reason_slot_makes_flag_appear`, DS2 `fail_demand_removed_mutant`) plus real-tree smokes — they bite,
not just fixture-green. The two client REDs remain lead-reported (tunnel + client repos, not verified from this machine).

| Seed | Branch | Tip | Repo | Pins | Seam it needs | Token |
|---|---|---|---|---|---|---|
| S19 | qa/c9-ext-writable-shape | `3726722` | niagara-tools (origin) | EW1-EW10 (EW10 = real BRoomPanel.setpoint WARN) | none (static lint) | `[CERT]` |
| S18-lint | qa/c9-silent-protection | `e38e503` | niagara-tools (origin) | SP1-SP8 + SP-smoke (mutation pin SP8; smoke flags CP-1/CR-3, not CP-2) | none (static lint) | `[CERT]` |
| S7 | qa/c9-demand-in-scope | `2916954` | niagara-tools (origin) | DS1-DS7 + DS-smoke (mutant pin DS2; smoke on real CompressorControl.step) | none (standalone script) | `[CERT]` |
| S12-A | qa/c9-s12-write-server | `24adcba` | pancaddia-leon-tunnel (base e4b42b0) | node:test | buildServer seam | `[INFER — lead-reported]` |
| S12-B | qa/c9-s12-servlet | `4c18837` | niagara-panccadia-leon (rebased a109249) | guard-4 = regression pin | `DashboardWriteGuards.evaluate` seam | `[INFER — lead-reported]` |

**The two C8 QA REDs that map to standing C9 seeds** (`git rev-parse`, verified 2026-09-06):

| Branch | Tip | C9-seed mapping | Token |
|---|---|---|---|
| qa/c8-write-path | `5e357d1` | **S5** — write-path coverage lint RED (W-matrix; extends to W14-W22) | `[CERT]` |
| qa/c8-structure | `c32cb5a` | **S6** — structure lint L1-L11 RED (L4/L7/L9/L10/L11 fixtures all FAIL) | `[CERT]` |

`[ev: git ls-remote origin qa/c9-* 2026-09-06]` `[ev: lead cross-session 2026-09-06]`

### 3.4 Plans & live records

- **S12 config-login + write-audit plan** — `1ecdf437c` (fixed `80adc279e`): config-login step-up + audited setpoint
  write; DashboardDispatch executable guard `:121-126`, SetpointWrite `:59-60`; servlet `handleSetpointWrite:195`,
  `parent.set:291`, `coerceValue:357`, `appendAudit:312`; guards XHR-302 / auth-401 / OPERATOR_WRITE-403 / invalid-400.
  `[ev: corpus S12 plan 1ecdf437c]`
- **Live oBIX probe record** — `aa7054702` (§1-9): viewer's live probes closed B823-G1, B826-G1/G2 (child `/value`
  served + `writable="true"`, propagates ~1.5 s). `[ev: corpus live record aa7054702]`

---

## 4. Candidate Slices (ranked, with dependencies)

| Rank | Slice | Class | Value | Dependency / seam needed | RED status | Token |
|---|---|---|---|---|---|---|
| 1 | **S12-A** config step-up + audit — viewer + write-server (mini-PC) | CLIENT/tunnel | HIGH | write-server `buildServer` seam (config-token store, `/config/logout`); off-station testable | RED `qa/c9-s12-write-server` `24adcba` (node:test) | `[ev: corpus S12]` `[ev: corpus B829]` |
| 2 | **S12-B** config step-up + audit — DashboardPan servlet (HMI) | CLIENT | HIGH | `DashboardWriteGuards.evaluate` seam + B829-G2 real-Context change | RED `qa/c9-s12-servlet` `4c18837` (guard-4 = regression pin) | `[ev: corpus S12]` `[ev: corpus B829]` |
| 3 | **S7** demand-in-scope lint | KIT | HIGH | none (standalone script); fixture defined in B820 | RED `qa/c9-demand-in-scope` `2916954` (DS1-DS7 + smoke) | `[ev: corpus B820]` |
| 4 | **S18-lint** silent-protection lint | KIT | HIGH | none (static); effect-slot exemption + adapter-follow + allowlist from B824 | RED `qa/c9-silent-protection` `e38e503` (SP1-SP8 + smoke) | `[ev: corpus B824]` |
| 5 | **S19** ext-writable-shape lint | KIT | MED | none (static) | RED `qa/c9-ext-writable-shape` `3726722` (EW1-EW10) | `[ev: corpus B823]` |
| 6 | **S18/S13** alarm PoC — `BAlarmSourceExt` on CR-3 freeze + CP-1 low-suction | CLIENT | HIGH | B827 patterns A/B; schema-SAFE (additive) | RED not yet authored (B827 §827.3/§827.4/§827.6 sketches) | `[ev: corpus B827]` |
| 7 | **S5-cont** write-path matrix W14-W22 | KIT | HIGH | S5 lint already RED (`5e357d1`); needs the writable set | matrix rows to author | `[ev: corpus S5]` `[ev: corpus B816 §816.6]` |
| 8 | **airDefrost flag** (rooms 1/2/4) | CLIENT or STATION | MED | path not chosen (module flag vs station config) | RED not yet authored | `[ev: corpus S16]` |

**Recommended C9 core (mirrors the seed-file recommendation):** S12-A + S12-B (the user's top-ranked ask) discharge
the config-login + unified-audit mandate; the two static lints (S7, S18-lint) + the alarm PoC (S18/S13) are the
next KIT/CLIENT tier and extend the C6/C8 lintable-vs-advisory doctrine. `[ev: corpus campaign9-research-candidates.md]`

---

## 5. Risks

1. **Real-module smoke per code PR** (campaign-7 meta-lesson 1): every lint/seam needs a fixture that FAILS without the
   fix. S7/S18-lint/S19 have design blocks but the RED fixtures are unwritten — author the FAIL case first, mutation-prove
   it. `[ev: corpus C7 meta-lesson 1]`
2. **S12 audit-fail semantics**: an audit APPEND FAILURE must never fail the write (DWS1 gate 5) — the write lands + an
   error/alarm row. A RED that fails the write on audit-fail is the wrong contract. `[ev: corpus S12]`
3. **B829-G2 real-Context change (surface B)**: `parent.set(prop, toSet, cx)` with the request user is
   schema-neutral but changes the servlet's audit behavior — pin `schema-risk.sh` SAFE and prove the `AuditEvent`
   actually fires (needs AuditHistoryService, CLOSED by B829-G1). `[ev: corpus B829]`
4. **Alarm edge detection (B827-G1)**: Pattern B must fire `newOffnormalAlarm` only on the normal→offnormal transition
   (a `wasOffnormal[]` per-trip state + `toNormal` on recovery + `started()` re-seed) — a naive level-triggered PoC
   re-fires every execute. `[ev: corpus B827]`
5. **HOA frozen-enum retrofit trap (B828 §828.7)**: applying the frozen-enum doctrine to a cross-module-LINKED HOA
   deletes the linked type → live "Missing class" crash. Frozen enum is NEW-modules-only. `[ev: corpus B828 §828.7]`
6. **Stale-tree reads**: the client tip moves (B821 read v2.0.0 while main was `fbe9009`; B823 cited `deed38c` vs main
   `fbe9009`). Re-anchor every load-bearing client cite at the tip the chain actually builds on before `[CERT]`.
   `[ev: corpus B821 re-anchor]` `[ev: corpus B823]`

---

## 6. Open Questions (requires-execution gates — pair read-only-first in one live session)

| Gate | Question | Token |
|---|---|---|
| B822-G1 | Live smoke: `POST /obix/config/…/applySetpoint` with `<real val=".."/>` INVOKES, and the oBIX login user's `OPERATOR_INVOKE` gates it. | `[ev: corpus B822]` |
| B827-G2 | Live: a routed `defaultAlarmClass` alarm reaches the REFLOW/PANCCADIA console + the DashboardPan panel end-to-end (= B821-G2 tier-1 confirm). | `[ev: corpus B827]` |
| B828-G2 | Live confirm of the frozen-enum behavior on a NEW-module deploy (range serialization, no missing-class on a NON-linked HOA). | `[ev: corpus B828]` |
| B829 live | End-to-end: surface-A oBIX PUT audits to the station login user; surface-B servlet real-Context set produces an `AuditEvent` in `/PANCCADIA/AuditHistory`; the two trails merge on the shared schema. | `[ev: corpus B829]` |
| airDefrost | Decision (not execution): the air-defrost trial for rooms 1/2/4 is a STATION config change or a new `airDefrost` module flag — pick the path before spec. | `[ev: corpus S16]` |

**Not blockers, but chain-start confirmations:** the exact K-number for the conventions delta (ask #12, `[INFER]` as
K22 from the close retro) and whether S12-B's `DashboardWriteGuards` seam is authored on the client tip the chain
builds on. `[ev: corpus C8 close-fold lesson 11]`
