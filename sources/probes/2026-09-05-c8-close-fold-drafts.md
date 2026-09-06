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

## Apply checklist — the FULL campaign-8 set (10 retros)
Flip all 10 INDEX rows `pending → folded`. MANDATORY token lines FIRST (else `sweep-fold-audit --strict` fails):
**§3 lint-timers-ext** and **§7 campaign8-doctrine-fold** (both 0-token). The other 8 are token-credited already
(§1/§4/§5/§6/§8 via BUILD-LOOP/SKILL; §2 triage-console at METHODOLOGY:96; **§9 station-snapshot** 3 tokens + **§10
bog-audit** 3 tokens, both via K19 routing). The §9/§10 folded-as-code lines are recommended-but-preferred (the
"folded as code: <script> [ev: retro <token>]" convention, METHODOLOGY:94 rule) — neither blocks the strict audit since
both are already token-credited. **PR11/PR12 will add two more retros** (12 total at campaign close). Then set
`retro_pending: false` in the kit self-envelope + sweep (pending=0, fold-audit clean).
