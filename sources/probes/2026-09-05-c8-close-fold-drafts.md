# Campaign-8 CLOSE — fold drafts for the pending kit retros

> For the close-fold apply worker: the campaign-8 close needs every campaign-8 retro `folded` (sweep-build-state
> pending = 0). This drafts the FOLD for the 6 pending kit retros. Per retro: the exact fold line(s) at the named
> target §, one `[ev: retro <slug>]` token per line, the grep-before-fold (K6) hit counts, and which deltas are
> already folded-as-code (pointer only). Kit root: `build-n4-module-kit`. **These are TOOL retros (campaign-8 PRs
> 1-6) — most folds are a `folded as code:` pointer line in `METHODOLOGY.md` §Conformance (matching the convention
> at lines 86-93), since the tool IS the fold.**
>
> **Load-bearing K6 finding:** `sweep-fold-audit.sh` credits a `folded` INDEX row if its `[ev: retro <slug>]` token
> appears in ANY non-`retros/` corpus file. 5 of 6 already carry their token in `BUILD-LOOP.md`/`skill/SKILL.md`
> (so they are ALREADY credited and safe to flip to `folded`); **only `lint-timers-ext` has ZERO token in the corpus
> — its fold line below is MANDATORY before the flip, or the audit fails.**

---

## 1. campaign8-lint-delays  (slug: `campaign8-lint-delays`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-lint-delays' <kit> --include='*.md'` (excl retros/) → **~3 hits**
  (`BUILD-LOOP.md` §5, `skill/SKILL.md` step 5, + PR7-branch — RE-RUN at apply). Already CREDITED. No `METHODOLOGY.md`
  folded-as-code line yet.
- **Deltas** (from the retro): D2b cross-file static-`long` helper resolution one level; D9b `.deploy-baseline` dot-dir
  prune; D2c same-method positivity-guard recognition; Delta 3 = a PROCESS lesson (a worker going silent mid-apply
  must leave the worktree resumable) — already covered by the multi-session/worktree discipline (`METHODOLOGY.md`
  §Multi-session coordination + K5), pointer only, no new line.
- **FOLD (recommended, convention completeness — add after `METHODOLOGY.md:93`):**
```
- folded as code: toolbelt/lint-delays.sh (Clock.schedule*/schedulePeriodically non-positive-delay floor lint; cross-file static-long helper resolution one level [D2b], same-method positivity-guard recognition [D2c], .deploy-baseline dot-dir prune [D9b]). [ev: corpus B801] [ev: retro campaign8-lint-delays]
```

## 2. campaign8-triage-console  (slug: `campaign8-triage-console`)
- **Grep-before (K6):** `grep -n 'folded as code.*triage-console' METHODOLOGY.md` → **1 hit** (line 93 on main; **:96
  on the PR7/doctrine-fold branch**), with `[ev: corpus B800] [ev: retro campaign8-triage-console]`; the token is
  credited ~6× across the corpus. **FULLY FOLDED — do NOT re-add.**
- **FOLD:** none. Flip INDEX only. (PR7 task 7.5 separately adds the CONTRACT prose line in §Conformance beside
  line 93 — lead-confirmed ADD; that is the PR7 worker's job, not this close fold.)

## 3. campaign8-lint-timers-ext  (slug: `campaign8-lint-timers-ext`)  ← THE unfolded one
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-lint-timers-ext' <kit>` (excl retros/) → **0 hits**; the tool
  name appears only in `BUILD-STATE.md`. **MANDATORY fold before the INDEX flip** or `sweep-fold-audit` fails.
- **Deltas:** companion-flag (same-method-body extraction, NOT a ±N line window — the retro's core lesson);
  jdk-thread (a `BComponent` using `ScheduledExecutorService`/`Executors.*`/`new Thread(` → FAIL); changed-sched
  (`Clock.schedule*` reachable from `changed()`/`started()` without an `isRunning()`/`atSteadyState()` guard in the
  scheduling body); D9b dot-dir prune.
- **FOLD (MANDATORY — add after `METHODOLOGY.md:93`, and confirm the tool is named in BUILD-LOOP §5 + SKILL):**
```
- folded as code: toolbelt/lint-timers.sh extensions (companion-flag: a flag set in the SAME method body as Clock.schedule* must be cleared in stopped()/started(), not on the expiry path only; jdk-thread: a BComponent using ScheduledExecutorService/Executors.*/new Thread( → FAIL; changed-sched: Clock.schedule* reachable from changed()/started() without an isRunning()/atSteadyState() guard in the scheduling body → FAIL). [ev: corpus B801] [ev: corpus B812] [ev: corpus B800 §800.3] [ev: corpus B816] [ev: retro campaign8-lint-timers-ext]
```

## 4. campaign8-facets-lint  (slug: `campaign8-facets-lint`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-facets-lint' <kit>` (excl retros/) → **2 hits** (`BUILD-LOOP.md`,
  `skill/SKILL.md`). Already CREDITED. No `METHODOLOGY.md` folded-as-code line yet.
- **Deltas:** D1 WARN-not-FAIL (K13 applied — the RED pinned WARN); D2/D3 false-positive prevention (multi-line
  `@NiagaraProperty` awk END-block scan + `seen[]` dedup + pass ordering); presence-only (never reads facet values,
  MIN=0 is a valid bound). Lesson 4 (multi-profile `--src` = the parent, e.g. `DashboardPan` not `DashboardPan-ux`)
  → a one-line note in BUILD-LOOP §5 pre-gate steps IF other multi-profile smokes are added (optional).
- **FOLD (recommended — add after `METHODOLOGY.md:93`):**
```
- folded as code: toolbelt/facets-lint.sh (presence-only: OPERATOR numeric slot without a facets= key → WARN [facets-req]; name-pattern slot *Setpoint*/*Temp*/*Limit*/*Band*/*Psi without UNITS, demand/*count*/stages without PRECISION → WARN; a station:/local:/slot:/ ORD string literal under src (excl srcTest) → WARN [ord-literal]; multi-line @NiagaraProperty awk END-block scan + seen[] dedup; never reads facet VALUES). [ev: corpus B787] [ev: corpus B801] [ev: retro campaign8-facets-lint]
```

## 5. campaign8-slot-per-slot  (slug: `campaign8-slot-per-slot`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-slot-per-slot' <kit>` (excl retros/) → **~4 hits**
  (`BUILD-LOOP.md`, `skill/SKILL.md`, + PR7-branch tokens — RE-RUN at apply). The retro's Proposed-deltas table records
  "Already updated: per-slot mode listed beside parse mode in `BUILD-LOOP.md` §5". The per-slot MODE is folded (a mode
  of `slot-coverage.sh`, folded-as-code at `METHODOLOGY.md:88`).
- **FOLD — one dropped delta to RESTORE (do NOT defer):** the retro's Proposed-deltas row (1) `D6a-behaviour-doc` is
  neither folded nor pointed, and `types/logic-authoring.md` has ZERO stale/known-keys/@Range content (grep-before →
  0 hits). Add ONE doc line in the lexicon/authoring section of `types/logic-authoring.md` (near the `## Minimal
  module` / lexicon guidance):
```
- **STALE lexicon detection — the known-keys set:** a lexicon key is STALE only if it is NOT in `{ all @NiagaraProperty slot names } ∪ { declared type display names (module-include.xml <type name>) } ∪ { @Range enum tags }` — READONLY/SUMMARY slots and type/enum keys are LIVE Niagara translations, not dead. Track fixes off the MISSING list (an OPERATOR slot with no key), not STALE. `[ev: retro campaign8-slot-per-slot]`
```
- **Optional:** extend the line-88 fold to name the per-slot mode:
```
- folded as code: toolbelt/slot-coverage.sh --strict per-slot mode (per-slot lexicon coverage beside the type-set parse mode; dup-bare-keys). [ev: retro campaign8-slot-per-slot]
```

## 6. campaign8-rc-scan  (slug: `campaign8-rc-scan`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-rc-scan' <kit>` (excl retros/) → **2 hits** (`BUILD-LOOP.md`,
  `skill/SKILL.md`). Already CREDITED. No `METHODOLOGY.md` folded-as-code line yet.
- **Deltas:** ord-literal (incl `h:<hex>` HANDLE scheme, anchored at ORD segment boundaries — NOT `h:/path`, the
  lead-corrected pattern); host-literal (`http://` non-w3.org / bare IPv4); bare-catch (empty `.catch(()=>{})`);
  null-branch (`? null :` on a process field); dot-dir prune first in the `find`.
- **FOLD (recommended — add after `METHODOLOGY.md:93`):**
```
- folded as code: toolbelt/rc-scan.sh (scans -ux rc/ assets under **/rc/** *.html/*.js/*.css, excl rc/ext, *.min.js, srcTest: station:/local:/slot:/ or a segment-anchored h:<hex> handle ORD literal → FAIL [ord-literal]; http:// non-w3.org or bare IPv4 → FAIL [host-literal]; empty .catch fetch-swallow → WARN/--strict FAIL [bare-catch]; ? null : process-field branch → WARN [null-branch]). [ev: corpus B801] [ev: corpus B803] [ev: retro campaign8-rc-scan]
```

---

## Apply checklist (the close gate)
For EACH of the 6 retros:
1. **INDEX flip:** `retros/INDEX.md` — flip that retro's `review-status` column `pending → folded`. (Safe for all
   6: triage-console already folded; lint-delays/facets-lint/slot-per-slot/rc-scan already token-credited;
   **lint-timers-ext ONLY after its MANDATORY fold line lands** — else `sweep-fold-audit --strict` fails.)
2. **Fold lines:** add §3 (lint-timers-ext, MANDATORY) now; §1/§4/§6 (recommended, convention completeness) and the
   §5 line-88 extension are optional-but-preferred — they satisfy the "folded as code: <script> [ev: retro <token>]"
   convention (`METHODOLOGY.md:94` rule). §2 (triage-console) = no line, already at :93.
3. **BUILD-STATE:** once ALL campaign-8 retros are `folded` (these 6 + the two PR7/PR8 retros when they land), set
   `retro_pending: false` in the `kit` self-envelope of `BUILD-STATE.md` (pair it in the same push range —
   envelope-pairing, BUILD-LOOP §7).
4. **Sweep to prove:** `toolbelt/sweep-build-state.sh <BUILD-STATE.md> retros/ INDEX.md` (pending count) +
   `toolbelt/sweep-fold-audit.sh --strict INDEX.md <kit-root>` (every folded row has a token). Close is done when
   pending = 0 and the fold audit is clean.

---

## 7. campaign8-doctrine-fold  (PR7, slug: `campaign8-doctrine-fold`)  ← PROMOTION retro, MANDATORY token
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-doctrine-fold' <kit>` (excl retros/) → **0 hits**. The retro is
  a PURE PROMOTION: its Δ1–Δ10 are ALL already applied across 7 files (`types/logic.md` §Safety timers + lint-delays
  pointer, `types/logic-authoring.md` §Inter-module comms, `types/dashboard.md` DWS1-gate-2 CsrfUtil + §Critical-write
  step-up, `METHODOLOGY.md` §Conformance triage-console + non-positive-delay + Excavador/K21/load-budget,
  `toolbelt/verify-module.sh` cert-chain caveat, `BUILD-LOOP.md` §6 schema-risk gate, `skill/SKILL.md`) — but each
  folded line carries `[ev: corpus B<n>]`, NOT the retro slug, so the retro's own token is absent. Per BUILD-LOOP §7
  exit (c) the INDEX flip is the promotion anchor, but `sweep-fold-audit --strict` still needs ONE `[ev: retro
  campaign8-doctrine-fold]` token on the folded row.
- **FOLD (MANDATORY — one promotion-attribution line in `METHODOLOGY.md` §Kit maintenance):**
```
- The campaign-8 R7.1–R7.10 corpus doctrine (8-layer timer index, non-positive-delay floor, inter-module comms, critical-write step-up, cert-chain trust, mandatory schema-risk gate, Excavador Técnico profile, K21, station load budget) was PROMOTED into the core in PR7 (doc-only). [ev: retro campaign8-doctrine-fold]
```

## 8. campaign8-report-integration  (PR8, slug: `campaign8-report-integration`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-report-integration' <kit>` (excl retros/) → **2 hits**
  (`BUILD-LOOP.md`, `skill/SKILL.md`). Already CREDITED → safe to flip. (`report-module.sh` itself was folded in
  campaign-7; this retro is the campaign-8 INTEGRATION + v0.19.0.)
- **Deltas:** `report-module.sh` extended to compose `lint-delays` (step 5) + `schema-risk` (step 6) + `triage-console`
  (step 7) with a `--console-dir` flag; `schema-risk.sh` dot-dir prune fix (`-mindepth 1`); VERSION → 0.19.0.
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance folded-as-code line):**
```
- folded as code: toolbelt/report-module.sh --console-dir (aggregates lint-delays + schema-risk + triage-console into one per-module punch-list; v0.19.0). [ev: retro campaign8-report-integration]
```

## 9. campaign8-station-snapshot  (PR9, slug: `campaign8-station-snapshot`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-station-snapshot' <kit>` (excl retros/) → **3 hits**
  (`BUILD-LOOP.md:61` post-deploy snapshot line, `skill/SKILL.md:68` toolbelt list, `toolbelt/station-snapshot.sh:25`
  header). Already CREDITED via K19 routing → safe to flip. No `METHODOLOGY.md` folded-as-code line yet.
- **Deltas** (INDEX row: 4): new `toolbelt/station-snapshot.sh`; K19 routing into BUILD-LOOP §Post-deploy (before the
  triage-console line) + SKILL toolbelt list. Design deviations (K13, RED wins): `manifest.json` not `.txt`; FLAT output
  not a nested dir. Doctrine the retro's table proposes: (a) the **NTFS/0777-mount guard** — no output file is executable
  even when the source is +x (SN5), and `cp -p` preserves the original Windows mtimes, so callers must NOT depend on
  output-file mtime for ordering; (b) the manifest is the LAST file written, making it the reliable `find -newer`
  source-write marker; (c) the snapshot is a baseline for `schema-risk.sh` + `bog-audit.sh` after the deploy.
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance folded-as-code line, after :96):**
```
- folded as code: toolbelt/station-snapshot.sh (pre/post-deploy audit-surface snapshot — copies config.bog + console*.txt, records history/alarm db pointers by path+size not the db files, manifest.json with per-file sha256; source never opened for write; the NTFS/0777 guard strips +x from outputs and cp -p keeps the Windows mtimes, so output-file mtime is NOT an ordering signal; the manifest is the last file written = the find-newer "after" marker). [ev: retro campaign8-station-snapshot]
```

## 10. campaign8-bog-audit  (PR10, slug: `campaign8-bog-audit`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-bog-audit' <kit>` (excl retros/) → **3 hits** (`BUILD-LOOP.md:63`
  audit line, `skill/SKILL.md:68` toolbelt list, `toolbelt/bog-audit.sh:3` header). Already CREDITED via K19 → safe to
  flip. No `METHODOLOGY.md` folded-as-code line yet.
- **Deltas** (INDEX row: 6): new `toolbelt/bog-audit.sh` (CHECK1-CHECK12, embedded python3 D10 grammar engine).
  Design/doctrine the retro's table proposes: **CHECK11 proxy-link-safety is FAIL** (not WARN — an own-module output
  linked to a Boolean/NumericWritable with no explicit fallback holds last state on stop/reload; PANCCADIA: 17 FAIL);
  **CHECK12 dashboard-write-to-link-target is advisory WARN**; the **inherited-frozen-slot rule (BA12)** — a bog frozen
  slot (no `t=`) absent from OUR source but on a class extending a FRAMEWORK superclass is CHECK5 **WARN "possibly
  inherited"**, not a ghost FAIL (e.g. `servletName` from `BWebServlet`); the D10 grammar tracks value slots on the
  DIRECT parent component only (compound-property sub-slots like `StatusNumeric.value` are ignored to avoid false CHECK5)
  and skips platform-managed `wsAnnotation` READONLY slots.
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance line + one doctrine line):**
```
- folded as code: toolbelt/bog-audit.sh (station config.bog auditor CHECK1-CHECK12; CHECK11 proxy-link-safety = FAIL, CHECK12 dashboard-write-to-link-target = advisory WARN; --source-dir adds the source-coupled CHECK2-7). [ev: retro campaign8-bog-audit]
- A bog FROZEN slot (no t=) absent from our @NiagaraProperty set but on a class extending a framework superclass is CHECK5 WARN "possibly inherited" — NOT a ghost FAIL (e.g. servletName from BWebServlet); a DYNAMIC slot (t= present) or a frozen slot on a BComponent-extending class stays FAIL. [ev: retro campaign8-bog-audit]
```

---

## 11. campaign8-wb-audit  (PR11, slug: `campaign8-wb-audit`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-wb-audit' <kit>` (excl retros/) → **4 hits**
  (`toolbelt/lint-wb-threading.sh` header, `BUILD-LOOP.md:70` pre-gate line, `skill/SKILL.md`, + the DWB1 doctrine).
  Already CREDITED → safe to flip. Re-run at apply.
- **Deltas** (INDEX row: 5): new `toolbelt/lint-wb-threading.sh`; `slot-coverage.sh` WB-LEX1 (missing-lexicon exit-1
  path); `verify-module.sh` WB-SCAFFOLD1 + WB-DEP1 (`check_wb_scaffold` + `check_phantom_dep`); `types/wb-widgets.md`
  DWB1 10-rule doctrine + chihuahua-wb tree; K19 routing. THREAD1/AGENT1 are WARN-only (K13, RED wins). Nothing dropped
  (slot-per-slot D6a): the doctrine delta is pointed even though the script deltas are the load-bearing ones.
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance folded-as-code line):**
```
- folded as code: toolbelt/lint-wb-threading.sh (two heuristic WARNs over a -wb src tree — ui-thread-traversal: a doInvoke body OR any same-class private/protected method reachable within 3 call levels (the callee chain, brace-counted + cycle-safe) that calls getNavChildren/getNavNodes/BqlQuery WITHOUT invokeLater/BJobService/JobThread anywhere on the expanded chain; + an agent-breadth heuristic; WARN-only, exit 1 only under --strict). [ev: retro campaign8-wb-audit]
```

## 12. campaign8-lint-servlet  (PR12, slug: `campaign8-lint-servlet`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-lint-servlet' <kit-root>` (excl retros/) → **3 hits in the KIT
  corpus** (`BUILD-LOOP.md` §5 pre-gate line, `skill/SKILL.md`, `toolbelt/lint-servlet.sh` header) — that is the scope
  `sweep-fold-audit.sh --strict INDEX.md <kit-root>` actually scans. Repo-wide the count is **4**: a 4th literal token
  mention lives in `openspec/changes/build-n4-module-campaign8/tasks.md:12.5`, which the fold-audit does NOT scan
  (2nd-read by investigador1, cc428e5). ≥1 either way → Already CREDITED → safe to flip. Re-run at apply (the count
  drifts as later folds land). NOTE the merge-order caveat (close-retro lesson 10): PR12 branched the same `3f666a0` as PR11 and needed a
  rebase + re-bless; the attempt-12 ledger evidence revision is `main-at-1f5201e`.
- **Deltas** (INDEX row: 5): new `toolbelt/lint-servlet.sh` (six checks: auth-gate, input-400, unbounded-set,
  cache-nofinger, log-in-handler, csrf-xrw-only); `tests/fixtures/lint-servlet/CsrfXrwOnly.java`; LSV4 pin; K19 routing.
  Design deviations (K13): a dedicated `lint-servlet.sh`, NOT an `rc-scan --servlet` flag; csrf is WARN not FAIL (B813);
  the input-400 prediction was wrong (corrected against DashboardPan-ux post-PR#7 `parseFiniteDouble→400`).
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance folded-as-code line):**
```
- folded as code: toolbelt/lint-servlet.sh (BWebServlet security lint over a -ux src, following callees ~depth-3: auth-gate, input-400 (a numeric parse not inside a try/catch that returns 400), unbounded-set, cache-nofinger, log-in-handler, and csrf-xrw-only = an X-Requested-With guard with NO CsrfUtil/csrfToken → WARN, B813; exit 0 clean/WARN-only, 1 any FAIL). [ev: retro campaign8-lint-servlet]
```

## 13. campaign8-post-deploy-checklist  (PR13, slug: `campaign8-post-deploy-checklist`)  ← 0-token, MANDATORY line
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-post-deploy-checklist' <kit>` (excl retros/) → **0 hits**. The
  deltas are doc-applied but carry no retro slug → `sweep-fold-audit --strict` needs ONE token before the INDEX flip.
- **Deltas** (INDEX row: 2): `BUILD-LOOP.md §6.a` — ordered post-deploy verification subsection (steps 1–4 + the CHECK11
  proxy-link-safety gate); `tests/kit-links.bats L7` — hard pin for the five §6.a step scripts.
- **FOLD (MANDATORY — add the token to the `BUILD-LOOP.md` §6.a subsection line, or one `METHODOLOGY.md` §Conformance line):**
```
- folded as code: BUILD-LOOP.md §6.a post-deploy verification (ordered steps 1–4: pre-snapshot → hot-reload → console triage → schema-risk/bog-audit re-run, gated on CHECK11; the five step scripts hard-pinned in tests/kit-links.bats L7). [ev: retro campaign8-post-deploy-checklist]
```

## 14. campaign8-build-pipeline  (PR14, slug: `campaign8-build-pipeline`)  ← 0-token, MANDATORY line
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-build-pipeline' <kit>` (excl retros/) → **0 hits**. §4.a/§4.b
  carry `[ev: corpus B807]`/`[ev: corpus B795]` but NOT the retro slug → MANDATORY token before the flip.
- **Deltas** (INDEX row: 3): `BUILD-LOOP.md §4.a` — Gradle `niagara-module` task matrix (what each task does + the safe
  `clean slotomatic jar` combo); `BUILD-LOOP.md §4.b` — `vendorVersion`/`bajaVersion` version-bump checklist + reload
  consequence; `tests/build-sh.bats BS-lock + BS-lock-hint` — exit-31 station-lock + `mirror-niagara-home.sh` hint
  regression pins.
- **FOLD (MANDATORY — one `METHODOLOGY.md` §Conformance line):**
```
- folded as code: BUILD-LOOP.md §4.a Gradle task matrix + §4.b vendorVersion/bajaVersion version-bump checklist (with the exit-31 station-lock BS-lock/BS-lock-hint regression pins in tests/build-sh.bats). [ev: corpus B807] [ev: corpus B795] [ev: retro campaign8-build-pipeline]
```

## 15. campaign8-rt-doctrine  (PR15, slug: `campaign8-rt-doctrine`)  ← PROMOTION retro (like §7), MANDATORY token
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-rt-doctrine' <kit>` (excl retros/) → **0 hits**. A PURE PROMOTION:
  its Δ1–Δ4 are ALL applied across `types/logic.md` (§RT-control-logic), `types/logic-authoring.md` (§history-ext +
  §"Slot types for externally written values"), `types/dashboard.md` (pointer) — but each folded line carries
  `[ev: corpus B<n>]`, not the retro slug, so the retro's own token is absent. Per BUILD-LOOP §7 exit (c) the INDEX
  flip is the promotion anchor, but `sweep-fold-audit --strict` still needs ONE `[ev: retro campaign8-rt-doctrine]`.
- **Deltas** (INDEX row: 4): Δ1 `types/logic.md` §RT-control-logic (9 entries, B805+B808); Δ2 `types/logic-authoring.md`
  §history-ext (5 bullets, B804); Δ3 `types/logic-authoring.md` §"Slot types for externally written values" (the
  value-class→slot table + prose, B823/B822/B825/B826/B828/B816 — the doctrine this session helped ground); Δ4
  `types/dashboard.md` pointer.
- **FOLD (MANDATORY — one promotion-attribution line in `METHODOLOGY.md` §Kit maintenance, mirroring §7):**
```
- The campaign-8 RT-control doctrine (§RT-control-logic, history-ext authoring, and the "Slot types for externally written values" table — B805/B808/B804/B823/B822/B825/B826/B828/B816) was PROMOTED into the types/ core in PR15 (doc-only). [ev: retro campaign8-rt-doctrine]
```

---

## 16. campaign8-retro-loop  (PR16, slug: `campaign8-retro-loop`)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-retro-loop' <kit-root>` (excl retros/) → **8 hits across 5 files**
  (`toolbelt/new-retro.sh`, `toolbelt/kit-ticket.sh`, `BUILD-LOOP.md §7`, `skill/SKILL.md` step 7, `ORCHESTRATION.md §8`)
  at `633f2bb`. Well CREDITED → safe to flip. Re-run at apply.
- **Deltas** (INDEX row: 5): Δ1 `toolbelt/new-retro.sh` (atomic triple write — retro stub + one `INDEX.md` row +
  `BUILD-STATE` `retro_pending` flip, idempotent); Δ2 `toolbelt/kit-ticket.sh` (gh issue create from a retro; gh
  absent/unauth → SKIP + fallback file, never fails a run — RL4); Δ3 `BUILD-LOOP.md §7` retro gate; Δ4 `skill/SKILL.md`
  step 7 close-of-run; Δ5 the **section-scoped envelope flip** — a live defect the lead surfaced: `new-retro.sh`'s `sed`
  was flipping ALL `retro_pending: false → true` (falsely marking every module section); fixed to an awk section-scoped
  flip (RL7 pin: kit=true, siblings=false). Plus two review-found guards: **RL8 the 6-char slug floor** (the case pattern
  enforced only 5 chars, but `sweep-fold-audit.sh` DROPS tokens < 6 chars as unfoldable → a 5-char slug's token is never
  credited; fixed to six bracket classes) and **RL9 the secondary INDEX guard** (retro file ABSENT + INDEX row PRESENT →
  SKIP + 1 row, no duplicate — defense-in-depth behind the primary file-exists exit-3).
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance folded-as-code line):**
```
- folded as code: toolbelt/new-retro.sh (atomic triple write — retro stub + one INDEX row + a BUILD-STATE retro_pending flip AWK-SCOPED to the named module section so it never marks siblings; idempotent; a 6-char slug FLOOR because sweep-fold-audit drops tokens < 6 chars) + toolbelt/kit-ticket.sh (gh issue-create from a retro; gh absent/unauth → SKIP + fallback file, never fails a run). [ev: retro campaign8-retro-loop]
```

## 17. campaign8-orchestration  (PR17, slug: `campaign8-orchestration`, doc-only)
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-orchestration' <kit-root>` (excl retros/) → **1 hit**
  (`skill/SKILL.md`) at `633f2bb` (the token was added in the `633f2bb` "cite the landed retro slug" commit). ≥1 →
  CREDITED, so the fold line below is recommended-but-preferred, NOT a mandatory 0-token line. Re-run at apply.
- **Deltas** (INDEX row: 3): `build-n4-module-kit/ORCHESTRATION.md` created (8-section session contract: roles, model
  table, delegation triggers, escalation gate, artifact store, evidence discipline, retro/ticket loop, recovery);
  `skill/SKILL.md` steps 1b (explore shard, audit-first >3 files, sonnet), 1c (design shard for schema/new-slot, opus),
  5b (peer QA RED-by-branch session before every code PR, K13); `tests/kit-links.bats L8` (every ORCHESTRATION-named
  script exists at `toolbelt/<script>.sh`). Δ2 records the temporary `toolbelt/` prefix-drop on the PR16 script refs to
  avoid an L1 failure before PR16 merged — RESTORED at `633f2bb` (nothing to carry).
- **Numbering lesson (fold-worthy doctrine line):** `wave3.md` called this an "L7 extension", but L7 already existed
  (PR13 §6.a step scripts), so the new assertion is **L8**. The correct check before naming a `kit-links.bats` pin is
  `grep -c '"L[0-9]' tests/kit-links.bats` (the current max L-number), NOT the wave spec's label.
- **FOLD (recommended — one `METHODOLOGY.md` §Conformance line + the numbering doctrine line):**
```
- folded as code: ORCHESTRATION.md (8-section session contract — roles, model table, delegation/escalation, artifact store, evidence discipline, retro/ticket loop, recovery) + skill/SKILL.md steps 1b/1c/5b + tests/kit-links.bats L8 (every ORCHESTRATION-named script resolves to toolbelt/<script>.sh). [ev: retro campaign8-orchestration]
- Before naming a new kit-links.bats structural pin, confirm the current max L-number with `grep -c '"L[0-9]' tests/kit-links.bats` — not the wave spec's label (wave3.md said "extend L7" but L7 already existed from PR13 §6.a, so the assertion is L8). [ev: retro campaign8-orchestration]
```

---

## Apply checklist — the FULL campaign-8 set (17 retros so far → 20 at close)
Flip all 17 INDEX rows `pending → folded` (as they merge). **MANDATORY token lines FIRST** (else
`sweep-fold-audit --strict` fails) — the five 0-token retros: **§3 lint-timers-ext**, **§7 campaign8-doctrine-fold**,
**§13 post-deploy-checklist**, **§14 build-pipeline**, and **§15 campaign8-rt-doctrine** (PROMOTION). The other 12 are
token-credited already (§1/§4/§5/§6/§8 via BUILD-LOOP/SKILL; §2 triage-console at METHODOLOGY:96; **§9 station-snapshot**
3, **§10 bog-audit** 3, **§11 wb-audit** 4, **§12 lint-servlet** 3 in-kit (4 repo-wide incl tasks.md), **§16 retro-loop**
8, **§17 orchestration** 1 — all via K19 routing). Their folded-as-code lines (§9/§10/§11/§12/§16/§17) are
recommended-but-preferred (the "folded as code: <script> [ev: retro <token>]" convention, METHODOLOGY:94) — none blocks
the strict audit since all are already token-credited. **PR18–PR20 will add the final three retros** (structure,
write-path, station-logic → 20 total at campaign close). Then set `retro_pending: false` in the kit self-envelope +
sweep (pending=0, fold-audit clean).
