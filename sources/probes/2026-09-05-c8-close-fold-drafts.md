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
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-lint-delays' <kit> --include='*.md'` (excl retros/) → **2 hits**
  (`BUILD-LOOP.md` §5, `skill/SKILL.md` step 5). Already CREDITED. No `METHODOLOGY.md` folded-as-code line yet.
- **Deltas** (from the retro): D2b cross-file static-`long` helper resolution one level; D9b `.deploy-baseline` dot-dir
  prune; D2c same-method positivity-guard recognition; Delta 3 = a PROCESS lesson (a worker going silent mid-apply
  must leave the worktree resumable) — already covered by the multi-session/worktree discipline (`METHODOLOGY.md`
  §Multi-session coordination + K5), pointer only, no new line.
- **FOLD (recommended, convention completeness — add after `METHODOLOGY.md:93`):**
```
- folded as code: toolbelt/lint-delays.sh (Clock.schedule*/schedulePeriodically non-positive-delay floor lint; cross-file static-long helper resolution one level [D2b], same-method positivity-guard recognition [D2c], .deploy-baseline dot-dir prune [D9b]). [ev: corpus B801] [ev: retro campaign8-lint-delays]
```

## 2. campaign8-triage-console  (slug: `campaign8-triage-console`)
- **Grep-before (K6):** `grep -n 'folded as code.*triage-console' METHODOLOGY.md` → **1 hit (line 93)**, with
  `[ev: corpus B800] [ev: retro campaign8-triage-console]`. **FULLY FOLDED — do NOT re-add.**
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
- **Grep-before (K6):** `grep -rn 'ev: retro campaign8-slot-per-slot' <kit>` (excl retros/) → **2 hits** (`BUILD-LOOP.md`,
  `skill/SKILL.md`). The retro's own Proposed-deltas table records "Already updated: per-slot mode listed beside parse
  mode in `BUILD-LOOP.md` §5". **Already folded** (per-slot is a MODE of `slot-coverage.sh`, whose folded-as-code line
  is `METHODOLOGY.md:88`). 
- **FOLD:** none new required. Flip INDEX. Optional: extend the existing line 88 fold to name the per-slot mode:
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

**PENDING (add when they land on main):** the PR7 doctrine-fold retro and the PR8 retro — draft their folds the same
way (grep-before, token, folded-as-code line) once their retros are committed. This draft covers the 6 that exist now.
