# B818 · Forensics: `Missing class for "ColdRoomPan:HoaMode"` was a DANGLING module-include.xml registration (a dead `<type>` for a class never built), not a bog retype — the runtime triage string for the kit's existing build-time check `[CERT]`

> **Scope** (small forensic): the PANCCADIA consoles show `SEVERE [sys.registry] Missing class for "ColdRoomPan:HoaMode"`
> right after `Out-of-date: Module changed` reloads. This traces what `HoaMode` was, why it went missing, whether the bog
> still references it, what the registry does, and pins the EXACT runtime string so the triage/bog-audit channel matches
> the real console form. Answer: it is the DANGLING-registration class (B12), already fixed + documented in the kit; the
> delta is the triage regex + the forensic timeline. Connects [B800] (console census), [B740] (cross-module enum),
> [B795]/[B807] (schema-risk/reload), [B12] (module.xml).
>
> **Sources**: FUENTE 1/3 — client git `Cliente/.../Paccadia/ColdRoomPan` (commit 85e4395), current `BEvaporatorUnit.java`,
> `module-include.xml`; the extracted PANCCADIA bog (Sep-4 snapshot); kit `build-n4-module-kit/{METHODOLOGY.md,
> build-verify.md,corpus-index.md,toolbelt/slot-coverage.sh}` @ read time. Console evidence [CERT-live] from
> console_backup_260902_0027.txt (00:14:14) + 260902_0120.txt (00:27:38).

---

## 818.1 — What `HoaMode` was `[CERT]`
`BHoaMode` was a HOA (Hand/Off/Auto) mode type registered in `ColdRoomPan-rt/module-include.xml` as
`<type class="com.angeles.ColdRoomPan.BHoaMode" name="HoaMode"/>` — but **the class was never built into the jar**
(`git log -- '*BHoaMode.java'` = no file ever committed; `git log -S HoaMode` shows the STRING only in module-include.xml
across 7d017f7/85e4395/0d8358b). So from the start it was a **dead `<type>` line pointing at a non-existent class** — a
dangling registration, NOT a live enum that was later retyped. Current source has NO `HoaMode`; the HOA function now lives
as `BEvaporatorUnit.valveMode`, a NUMERIC `double` (`newProperty(Flags.SUMMARY|OPERATOR|TRANSIENT, 0d, null)`
`BEvaporatorUnit.java:535`, `getDouble` `:542`) — plus the new `BFanMode` enum.

## 818.2 — When it was removed `[CERT]`
Commit **85e4395** (`feat(niagara/coldroompan): … + fix BHoaMode`) removed it — verbatim message: *"module-include.xml:
removido tipo muerto BHoaMode (daba 'Missing class' en la JACE y bloqueaba el gate); registrado BFanMode."* So the fix was
to DELETE the dangling `<type>` (and register the real `BFanMode`) in the same change.

## 818.3 — Does the bog still reference it? `[CERT]`
NO. The Sep-4 PANCCADIA bog snapshot references `ValveMode` **131×** + `valveMode` 18× and **`HoaMode` 0×** — the bog was
re-saved after the fix, so the HoaMode-typed slots are gone. The Sep-2 consoles (00:14 / 00:27) predate that: at reload
time the JACE's `module-include.xml` still declared `BHoaMode` while the class was absent → the registry could not resolve
the type spec. (The transient window: dead registration on the JACE → `Missing class` on every module reload → fixed at
85e4395 → bog re-saved clean by Sep 4.)

## 818.4 — What the registry does with a missing class `[CERT-live + INFER]`
`SEVERE [sys.registry] Missing class for "ColdRoomPan:HoaMode"` — the type-registry (not the decoder) fails to load the
class for a registered `<type>`. This is the CLASSLOADER/registry layer ([B740]/[B631]), DISTINCT from a bog slot retype
([B795] schema-risk, which is a decode-time `warningAndSkip`/throw on a slot VALUE). Here the CLASS itself is absent while
still registered — the station logs SEVERE and continues (the type is simply unresolvable; any instance/slot of it can't be
introspected). It is the DANGLING-REGISTRATION defect ([B12]), whose SOURCE the kit already catches at build time.

## 818.5 — Kit implication (already partly folded — confirm the runtime string) `[CERT]`
The kit ALREADY documents the BUILD-TIME side of this exact defect:
- `METHODOLOGY.md:29` — "When you DELETE a `@NiagaraType` class, delete its `module-include.xml` registration line in the
  SAME change … a leftover surfaces live as `Missing class <Module>:<Type>`. [ev: retro coldroompan-fan-mode-defrost · B12]"
- `build-verify.md:95` — the known-bad: `ColdRoomPan-rt.jar` fails the `types` check because module-include.xml still
  declares `BHoaMode` after the class was deleted.
- `toolbelt/slot-coverage.sh:7` + `corpus-index.md` (B740).
**The residual delta = the RUNTIME triage string.** The real console form is `SEVERE [sys.registry] Missing class for
"<Module>:<Type>"` — with the word **`for`** and **quotes** — whereas the kit's documented form is `Missing class
<Module>:<Type>`. So the `triage-console.sh` / bog-audit "own-prefix missing class" channel must match a regex like
`Missing class( for)? "?<own-prefix>:` (own-prefix ∈ ColdRoomPan|DashboardPan|CompPan|chihuahua), so it catches the live
line. Then verify-module (build-time source) + triage-console (runtime log) are the complementary halves of the same defect.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | BHoaMode was a dangling module-include.xml `<type>` for a class never built (no *.java ever committed; string only in module-include.xml) | [CERT] | git log -- '*BHoaMode.java' (none); git log -S HoaMode (7d017f7/85e4395/0d8358b) |
| 2 | HOA function is now `BEvaporatorUnit.valveMode`, a numeric double (+ new BFanMode enum) | [CERT] | BEvaporatorUnit.java:535,542; module-include.xml (StagingMode/DefrostMode/FanMode, no HoaMode) |
| 3 | Removed at 85e4395 (deleted the dead `<type>`, registered BFanMode) | [CERT] | commit 85e4395 message |
| 4 | Sep-4 bog references ValveMode 131×, HoaMode 0× (re-saved clean); Sep-2 consoles caught the transient | [CERT] + [CERT-live] | bog grep; console_backup_260902_0027/0120 |
| 5 | It is the registry/classloader dangling-registration defect (B12/B740), NOT a bog retype (B795); kit catches the SOURCE via verify-module `types`; residual = the runtime triage string `Missing class for "<prefix>:<Type>"` | [CERT] | METHODOLOGY.md:29; build-verify.md:95; slot-coverage.sh:7 |

**Tally**: 4 [CERT] + 1 [CERT]+[CERT-live]. §818.5 triage-regex proposal is [INFER] grounded in the exact console string.
Dedupe: the build-time rule + verify-module check are REMITTANCE ([B12]/[B740]/METHODOLOGY); this block adds the forensic
trace + the runtime-string match for the triage channel.

## Connections
- **[B800]** (console census — this is one census finding, forensically resolved), **[B740]** ("cross-module links use a
  double not a shared enum" — the same HoaMode outage, design-rule side), **[B795]** (schema-risk — CONTRAST: this is a
  missing CLASS/dangling registration, not a slot-value retype), **[B807]** (module reload path), **[B12]** (module.xml
  registration). Kit: the `triage-console.sh`/bog-audit "own-prefix Missing class" channel regex; verify-module `types`
  already catches the build-time source.

## Open gaps
- **B818-G1** (bounded): confirm on a live station whether an unresolved registered `<type>` degrades its instances to a
  `BUnknown`/placeholder or just drops the slot — the SEVERE-and-continue behavior is [CERT-live], the per-instance
  placeholder is [INFER] (pairs with B795-G1/B807).
