<!-- review-status: pending -->
# Retro — research-sdd FOLD LIST · companero campaign-8 blocks (B806–B815 + §800.3 Clock doctrine) · 2026-09-05

> §18 consolidated FOLD retro: one row per kit delta, target file/§ + `[ev: corpus B<n>]`, so the campaign-8
> doc PRs cite everything mechanically. Propose-only (no $KIT edit). Companion to the earlier automate-retros
> retro (2026-09-05-research-sdd-retro-automation…). All blocks are on origin/main.

## Fold table — kit delta → target file/§ → evidence

> **B800 split (agreed with investigador1, no double-fold):** D7 (triage-console.sh) + D8 (verify-module
> cert-chain-trust) and the B795-G1/#50 schema-OUTAGE closure (meta-delta #1) live in investigador1's
> consolidated retro (`2026-09-05-research-sdd-build-kit-campaign8-consolidated-fold-retro.md`) — POINTER only, not re-folded here.
> The Clock-not-`java.util.concurrent` doctrine (§800.3) IS folded here (first row).

| Kit delta | Target file / § | Evidence |
|---|---|---|
| Doctrine: a `BComponent` schedules periodic work via `Clock.schedule`/`schedulePeriodically`, NEVER a raw `java.util.concurrent`/JDK executor (SecurityManager denies `modifyThread` at shutdown — 21× live across two stations) + a lint on JDK-thread use in a BComponent | `types/logic.md` anti-pattern + lint candidate | `[ev: B800 §800.3]` |
| STATION LOAD BUDGET table (engine 1thread/20ms, watchdog 3min, timer buckets, fox pool 2, persisted-slot cost, globalCapacity 25/500/400/125 >110% no-boot, poll 5s) + 8-count viability checklist | `METHODOLOGY.md` "Station load budget" (R7.10/SC11) | `[ev: B806]` |
| BUILD-TASK MATRIX (clean/slotomatic/writeModuleXml/jar plugin-cited) + station-lock (jar→modules copy) mirror recipe cite + version-bump checklist (patch/minor/major → schema-risk) | `BUILD-LOOP §4` + `build-verify.md` | `[ev: B807]` |
| HEALTH/FEEDBACK-SURFACE checklist for rt components (fault-status slot in catch + BAlarmSourceExt + heartbeat + isRunning-guard; value-status vs logic-health) | `types/logic.md` health surface | `[ev: B808]` |
| `station-snapshot.sh` remote-mode contract (FOX read-only safe / PLATFORM .dist manual-only; console NOT in .dist; bog-diff→schema-risk, console-delta→triage-console) + pre/post-deploy checklist | new `toolbelt/station-snapshot.sh` + post-deploy step | `[ev: B811]` |
| LIVENESS-WATCHDOG recipe (producer lastTick TRANSIENT + independent monitor, floor max(1,)ms, stall>3×period → BStatus fault + BAlarmRecord) — the monitor Tridium doesn't ship | `types/logic.md` liveness watchdog | `[ev: B812]` |
| UX SERVLET section (registration=lifecycle/no module.xml, route()→sealed action, rc/ classloader+traversal, REST JSON) + PR12 lint L1-L6 (facet/cache/log/coerce/RBAC lint; CSRF review) + 2 contract tests; CSRF = x-niagara-csrfToken via CsrfUtil | `types/dashboard.md` servlet section + PR12 | `[ev: B813]` |
| "SHIP A TAG DICTIONARY" recipe (BNamespace + BSmartTagDictionary rules auto-tag by TYPE; palette placement under TagDictionaryService; NEQL/nav/hierarchy addressable) | `types/logic.md` tag dictionary | `[ev: B814]` |
| TEST LAYER: `moduleTest` `BTestNg` station-lifecycle test (mount via `createTestStation()` try-with-resources + drive the slot + read the `Clock.Ticket` handle by reflection, NEVER wait on wall-clock timers) asserting the client four-ticket PR#1/#2 invariants (defrost PRESERVES powerOnTicket; stopped() cancels all four + clears flags) — grounded in live `com.angeles.ColdRoomPan.BEvaporatorUnit` c66e412; `build.sh` gains a `moduleTestJar` compile-gate step (WSL compile-only, `niagaraTest` runs native/JACE) + scaffold `-rt` gradle ships `testImplementation` on `test-wb`. A WSL `run-station-test.sh` is NOT possible (`niagaraTest`=0 from WSL). | `BUILD-LOOP` test layer + `types/logic.md` lifecycle-test recipe + scaffold gradle | `[ev: B815]` |

## Notes
- Every row's evidence block is on origin/main; the doc PR flips this retro's review-status to applied once folded.
- Cross-refs: B806 already bound as R7.10/SC11; B807→PR14; B808→PR13/PR15; B811→PR9; B813→PR12; B800 findings feed triage-console + verify-module.
