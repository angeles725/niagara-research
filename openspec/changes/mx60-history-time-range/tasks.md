# Tasks: mx60-history-time-range

## Workflow constraints (MX60 chihuahua repo) — READ BEFORE APPLY

Per previous sessions (engram #1279, #1283, #1284, #1287):

- **No remote** in `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`. **No PR workflow.** Pattern is: feat branch → bisectable commits → `git merge --no-ff` a main.
- **Tests are documentation-only** (#1284). Plugin `com.tridium.niagara-module` 7.6.17 bug: `./gradlew test` retorna `NO-SOURCE`/`Total tests run: 0`. **NO correr** `:chihuahua-rt:test`, `:chihuahua-ux:test`, ni `niagaraTest`. Strict TDD = disciplina de escritura (red→green→refactor ORDEN de commits), NO gate ejecutable. Marcar gates como `user-verified-post-deploy`.
- **No JS test runner** in `chihuahua-ux/`. Manual regression checklist (WU7) ES el test gate frontend — no `vitest/jest/karma/mocha` setup pending, decisión final.
- **Build mode selector** (#1284): solo ux changes → mode B (`:chihuahua-ux:clean :chihuahua-ux:jar`); solo rt → C; ambos → A. Verificar con `git diff main..HEAD --name-only -- chihuahua/`. Este SDD toca solo ux → modo B.

## Commit bisectability forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~355 (prod + test skeletons) |
| Target commits | 7 (one per work-unit) |
| Branch | `feat/mx60-history-time-range` (new) |
| Merge style | `git merge --no-ff` to main at end |
| First user-visible fix | WU2 commit (canonical range keys land — 4 existing tabs unblocked) |
| Backup pre-apply | `_backups/chihuahua-pre-sdd-history-time-range-YYYYMMDD-HHMMSS.tar.gz` |

### Work Units → Commits

| Unit | Goal | Commit | Bisectable? | Notes |
|------|------|--------|-------------|-------|
| WU1 | RANGE_TO_BACKEND const + RANGES shape | C1 | YES | Declarative only, no behavior change |
| WU2 | Wire mapping in _fetchSlotHistory + filterHistoryByRange dynamic cutoff | C2 | YES | First visible fix — 4 tabs start returning correct backend keys |
| WU3 | Backend computeTargetPoints + WARNING log | C3 | YES | Helper + log, not yet wired into response |
| WU4 | Backend stride wiring + fullResolution flag | C4 | YES | Second big fix — 7d gap resolved |
| WU5 | Expose 4 new UI tabs | C5 | YES | Feature gap closed |
| WU6 | _maxHistoryEntries() wiring | C6 | YES | Perf guard, 1 call site |
| WU7 | Regression checklist artifact (no code) | C7 | YES (docs) | Manual gate run by user post-deploy |

---

## Pre-flight

- [ ] P.1 — Verify MX60 repo state per #1279 pattern: `cd /home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/ && git status && git branch --show-current` — expect clean tree, branch `main`. If dirty, STOP and resolve before apply.
- [ ] P.2 — Create backup tarball: `tar -czf _backups/chihuahua-pre-sdd-history-time-range-$(date +%Y%m%d-%H%M%S).tar.gz chihuahua/`
- [ ] P.3 — Create + checkout feat branch: `git checkout -b feat/mx60-history-time-range`
- [ ] P.4 — Inject context into `sdd-apply` prompt (orchestrator responsibility): "Tests are documentation-only — DO NOT run `./gradlew test`. Build verification via mode B only (`:chihuahua-ux:clean :chihuahua-ux:jar`). No JS test runner — manual regression checklist (WU7) is the frontend test gate. All TDD gates marked `user-verified-post-deploy`."

## Open BLOCKED Decisions

None — proceed. All open questions resolved by design or by previous-session workflow constraints:
- **OQ-S4/OQ-D2 (JS runner)**: RESOLVED — no JS runner exists; manual regression checklist (WU7) is the gate. Final, not pending.
- **OQ-S3 (gradle test command)**: RESOLVED — tests are docs (#1284), no gradle test execution. Gates marked `user-verified-post-deploy`.
- **OQ-D1 (tab strip vs dropdown)**: pre-elected tab strip; escalate only if visual overflow observed in WU7.M9.
- **OQ-D4 (fixture management)**: not blocking sprint-1 — JUnit test skeletons documented as `srcTest/`, not executed.

---

## Phase 1: Foundation — RANGE_TO_BACKEND + RANGES shape extension (WU1)

**Spec**: REQ-1, REQ-3 | **File**: `chihuahua-ux/src/rc/js/app/UpDetail.js`

- [ ] 1.1 (RED) Write assertion: `RANGES.every(r => RANGE_TO_BACKEND[r.id])` must be true — run manually, confirm fails (no const yet)
- [ ] 1.2 (GREEN) Add `const RANGE_TO_BACKEND` after `RANGES` at UpDetail.js:176 with 8 entries per REQ-1 table
- [ ] 1.3 (GREEN) Extend `RANGES` with 4 new entries (`today/yesterday/30d/mtd`) using shape `{id, label, dynamic?: true, points, step}` per design Decision 5; keep `hours` only on static entries
- [ ] 1.4 (VERIFY) Re-run assertion from 1.1 — must be true; confirm 8 tabs visible in DOM (4 may not function yet)

## Phase 2: Core Implementation — Wire mapping + filterHistoryByRange dynamic (WU2)

**Spec**: REQ-1, REQ-3 | **Files**: UpDetail.js L552, L894-907

- [ ] 2.1 (RED) Manual check: click "7d" tab, confirm DevTools Network shows `range=7d` (wrong — must become `last7Days`)
- [ ] 2.2 (GREEN) In `_fetchSlotHistory` (L552): replace raw range usage with `RANGE_TO_BACKEND[rangeId] || (console.warn('[UpDetail] unknown range id: ' + rangeId), 'lastHour')`
- [ ] 2.3 (GREEN) In `filterHistoryByRange` (L894-907): add `_computeRangeCutoff(r, now)` helper; for `r.dynamic === true` compute cutoff from clock (today/yesterday/mtd boundaries); fallback to `now - r.hours * 3600000` for static ranges
- [ ] 2.4 (VERIFY) Click all 4 original tabs — DevTools Network must show canonical keys (`lastHour`, `last8Hours`, `last24Hours`, `last7Days`)

## Phase 3: Backend — computeTargetPoints helper + WARNING log (WU3)

**Spec**: REQ-2 | **Files**: `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiHistoryHelper.java`, `ChiHistoryHelperTest.java`

- [ ] 3.1 Inventory `ChiHistoryHelperTest.java` existing assertions; save summary to `openspec/changes/mx60-history-time-range/test-inventory.md` (resolves OQ-D3)
- [ ] 3.2 (RED — test-as-doc) Write JUnit skeleton: `computeTargetPoints("last7Days")` == 168; `computeTargetPoints("lastHour")` == 60; `computeTargetPoints("last30Days")` == 360; `computeTargetPoints("foo")` == 60 with WARNING captured. Commit test BEFORE production code (red discipline preserved). `user-verified-post-deploy` since gradle test is `NO-SOURCE` per #1284.
- [ ] 3.3 (GREEN) Add `private static int computeTargetPoints(String rangeName)` to `ChiHistoryHelper.java` with lookup table per design Decision 2; add `LOG.warning` for unknown key
- [ ] 3.4 (RED — test-as-doc) Write JUnit skeleton: `computeRange("foo")` falls back to `lastHour` AND emits `LOG.warning("computeRange unknown range: foo")`. Commit before 3.5.
- [ ] 3.5 (GREEN) Add `LOG.warning("computeRange unknown range: " + name)` inside the `else` branch at ChiHistoryHelper.java:312
- [ ] 3.6 (VERIFY — `user-verified-post-deploy`) **NO correr `gradle test`** (NO-SOURCE per #1284). Build verification: `./gradlew :chihuahua-ux:clean :chihuahua-ux:jar` succeeds without compile errors → test skeletons compilan = doc-quality green.

## Phase 4: Backend — Stride wiring + fullResolution flag (WU4)

**Spec**: REQ-2 (S-2) | **File**: ChiHistoryHelper.java L180-220

- [ ] 4.1 (RED — test-as-doc) Write JUnit skeleton: `queryHistoryData` with cursor mock 10080 records + range `last7Days` returns JSON ≤ 169 points with first + last timestamps present and monotonic. Commit BEFORE 4.3.
- [ ] 4.2 (RED — test-as-doc) Write JUnit skeleton: `queryHistoryData` with `fullResolution=true` param + cursor mock 3000 records returns all 3000 (no stride). Commit with 4.1.
- [ ] 4.3 (GREEN) Remove `int maxPoints = 2000` at L194; replace with `int targetPoints = computeTargetPoints(rangeName)` and add `final int MAX_POINTS_HARD_CEILING = 5000`
- [ ] 4.4 (GREEN) Replace truncate-head `while` loop with single-pass stride: collect records with modulo counter (`stride = max(1, estimatedTotal / targetPoints)`), always include last record
- [ ] 4.5 (GREEN) Parse `?fullResolution=true` query param in `queryHistoryData`; if true, bypass stride (cap at `MAX_POINTS_HARD_CEILING`)
- [ ] 4.6 (VERIFY — `user-verified-post-deploy`) **NO correr `gradle test`**. Build mode B: `./gradlew :chihuahua-ux:clean :chihuahua-ux:jar` → deploy → smoke test manual: click 7d en station real, verify chart shows complete series to `now` (no gap at end).

## Phase 5: Frontend UI — Expose 4 new tabs (WU5)

**Spec**: REQ-3 (S-3) | **File**: UpDetail.js tab strip render

- [ ] 5.1 (RED) Manual check: confirm "Today" / "Yest" / "30d" / "MTD" tabs NOT visible (RANGES extended in Phase 1 but not yet rendered if loop was missing)
- [ ] 5.2 (GREEN) Verify tab strip render loop already iterates full `RANGES` array; if not, fix loop to include all 8 entries
- [ ] 5.3 (GREEN) Confirm `dynamic: true` entries call `_computeRangeCutoff` (wired in Phase 2) — no additional code if Phase 2 correct
- [ ] 5.4 (VERIFY) Open UP detail — 8 tabs visible; click each of 4 new tabs; DevTools Network shows correct backend key + non-empty chart series returned

## Phase 6: Frontend — _maxHistoryEntries() cap dinámico (WU6)

**Spec**: REQ-4 (S-4) | **File**: UpDetail.js L396 + L3535

- [ ] 6.1 (RED) Manual check: set `_currentHistoryRange = '7d'`, fill `fullHistory` with 1500 entries via console, trigger `_appendLiveSample` — confirm current code trims at 1440+600=2040, which is wrong (should NOT trim for 7d)
- [ ] 6.2 (GREEN) Rename `const HISTORY_MINUTES = 24 * 60` to `const HISTORY_MINUTES_FALLBACK = 24 * 60` at L396
- [ ] 6.3 (GREEN) Add `function _maxHistoryEntries() { const r = RANGES.find(x => x.id === _currentHistoryRange); return r ? Math.max(HISTORY_MINUTES_FALLBACK, Math.floor(r.points * 1.5)) : HISTORY_MINUTES_FALLBACK; }` adjacent to L396
- [ ] 6.4 (GREEN) Change L3535: `if (fullHistory.length > HISTORY_MINUTES + 600)` → `if (fullHistory.length > _maxHistoryEntries() + 600)`
- [ ] 6.5 (VERIFY) Switch to 7d range, accumulate samples via live append — `_appendLiveSample` must not trigger trim while `fullHistory.length < Math.floor(168 * 1.5) + 600 = 852`

## Phase 7: Verification Gate — Regression smoke + manual checklist (WU7)

**Spec**: REQ-5 (S-6) | **Output**: `openspec/changes/mx60-history-time-range/regression-checklist.md`

- [ ] 7.1 Create `regression-checklist.md` with 10 manual check items (Chart.js v4, IIFE caches, _appendLiveSample O(1), RAF stagger, requestIdleCallback, comfortBandPlugin, htmlLegendPlugin, filterHistoryByRange defense-in-depth, detectEquipmentHistories, tab strip visual fit)
- [ ] 7.2 Execute checklist against running MX60 station; mark each item PASS/FAIL with timestamp
- [ ] 7.3 If any item FAIL: stop, do NOT merge, file issue and link from checklist
- [ ] 7.4 If WU7.M9 (tab strip fit) shows overflow: escalate to sdd-design future; document in checklist; do NOT block merge if functional
- [ ] 7.5 Persist completed checklist as artifact for sdd-verify (save to engram `sdd/mx60-history-time-range/verify-report` — NO PR, no remote per #1279)
- [ ] 7.6 **Merge feat→main**: `git checkout main && git merge --no-ff feat/mx60-history-time-range -m "feat(chihuahua-ux): MX60 history time-range — vocabulary mapping + stride + 4 ranges"`; delete feat branch local (no remote to push)

---

## Cross-refs

- `proposal.md` — engram #1308
- `spec.md` — engram #1309 (5 REQs + 6 Scenarios)
- `design.md` — engram #1310 (7 Decisions + implementation sequence)
- Bloque #73 — engram #1265
- Implications #299/#300/#305/#311/#312
- Implementation repo: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`
- Key files: `chihuahua-ux/src/rc/js/app/UpDetail.js`, `chihuahua-ux/src/com/angeles/chihuahua/ux/ChiHistoryHelper.java`, `chihuahua-ux/srcTest/test/com/angeles/chihuahua/ux/ChiHistoryHelperTest.java`
