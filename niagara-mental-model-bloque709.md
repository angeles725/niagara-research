# B709 — Module best practices, build & packaging (MBP5): the gradle-niagara build, when to run Slotomatic, convention-driven signing, version-targeting, and the deploy loop

> Focus: **module-best-practices** · Gap **MBP5** (build/packaging). Block TYPE = **DESIGN/SYNTHESIS**
> (distilled from own-modules-audit OMB1-3 + module-anatomy; high [INFER] ratio expected). Feeds
> `docs/module-best-practices.md` §5. Marker `[CERT]` where re-citing verified code; `[INFER]` for guidance.

## 709.1 — The build stack

[CERT] N4 modules build with the **Tridium gradle-niagara plugins** applied per part:
- `id("com.tridium.niagara-signing")` on the ROOT (registers a signing factory) + per part (rt/ux/wb) — no
  explicit `niagaraSigning{}` block is needed; the `jar` task auto-signs ([Block 639] §639.1).
- `module-include.xml` per part declares the exported types (one `<type name= class=>` per component) — Slotomatic
  reads it and generates the AUTO slot region; the classloader/registry read it at boot ([Block 631]/[Block 636]).

## 709.2 — When to run Slotomatic (the build-mode rule)

[CERT] The shop's deploy wrapper (`ng-deploy.sh`, [Block 637] §637.4) has three modes:
- **Mode A — Clean + Build** (default): use when NO `@Niagara*` annotation changed.
- **Mode B — Clean + Slotomatic + Build**: use when a `@NiagaraProperty`/`Action`/`Topic`/`Type` was added or
  modified. Slotomatic regenerates the AUTO region + its hash.
- **Mode C** — variant per the repo.

**Rule:** run `:slotomatic` ONLY on annotation changes. A STALE AUTO region (skipped regen) → compile errors or
silent runtime slot failures; chihuahua's `BChiUp` currently carries `AWAITING SLOTOMATIC REGEN` ([Block 650]).
Automating the choice via `git diff` on `@Niagara*` is the clean approach.

## 709.3 — Signing is convention-driven

[CERT] There is NO explicit signing config block. The niagara-signing plugin takes the key from the
**`niagara_user_home/security/keystore.jceks`** of the active SDK home. The shop's active alias is
**`angelessignerCA`** (the ANGELES chain), which replaced the legacy **SEJOFA** chain (fails CI) ([Block 639]
§639.2). Every jar emerges already carrying `META-INF/*.SF/.RSA` ([Block 632]). Vendor identity = `ANGELES`
(`defaultVendor("ANGELES")`, [Block 638]). The crypto trust-anchor detail is REMITTANCE ([Block 392]).

**Rule:** keep one signing alias per shop, aligned with the vendor identity; don't mix chains across jars
(the SEJOFA→ANGELES migration left mixed signer-block names — avoid that going forward).

## 709.4 — Version-targeting by SDK path

[CERT+INFER] The module is built against a specific Niagara version by pointing `niagara_home`/`niagara_user_home`
at that SDK (4.13 / 4.14 / 4.15) — version-targeting is by PATH, and it is DELIBERATE ([Block 638], operator
correction). Build against the version of the station you deploy to. Bump `vendorVersion` per release — 12/13
shop modules are frozen at `1.0`; only chihuahua tracks a real history (`1.0→1.3`), which is the correct habit
([Block 640]/[Block 649]).

## 709.5 — The deploy loop + the WSL/NTFS bridge

[CERT] The canonical deploy (`ng-deploy.sh`, [Block 637] §637.4): **backup → `./gradlew` (mode A/B/C) → copy
jars to `STATION_MODULES_DIR` → verify emitted types against `EXPECTED_*_TYPES`** (phase exit codes
10/20/30/40/50 for scriptable failure). Because the security store lives on the Windows side, the build uses a
**Robocopy WSL→Win→WSL** bridge for slotomatic + jar ([Block 639] §639.1). Post-copy type verification is what
catches a silent registration failure (a class dropped from `module-include.xml`) before it reaches the station.

## 709.6 — Packaging rules

[INFER, consolidating]
- **Profiles:** put station logic in `-rt`, browser UI in `-ux`, Workbench Swing in `-wb`; `runtimeProfile` is
  load-bearing (§705.1 P5 / §707.2).
- **Dependencies:** reference shared/common modules from `$NIAGARA_HOME/modules/`; do NOT shade heavy libs
  (Gson/Jackson) into each module (§706.2 AP3). Extract a shared `gson-rt`/`jackson-rt` if needed.
- **Palette:** ship a `module.palette` for component modules (§705.1 P8).
- **Permissions:** no empty `<permissions>` scaffold; scope at `@AgentOn` (§708.4).
- **No empty jars** (interfaz1-wb) — they still cost boot load + signature verify (§707.5 AP4).

## Connections

- Build process → focus `own-modules-audit` [Block 637]–[Block 639]; version-targeting → [Block 638]; signing →
  [Block 639]/[Block 392]; type-registration → [Block 631]/[Block 636]; Slotomatic staleness → [Block 650].
  Layer siblings → [Block 705]–[Block 708]. Deliverable: `docs/module-best-practices.md` §5.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | gradle-niagara plugins per part; module-include.xml declares types | [CERT] | [Block 639]/[Block 631] | cited |
| 2 | Slotomatic only on annotation change (mode A vs B); stale=broken | [CERT] | [Block 637]/[Block 650] | cited |
| 3 | signing convention-driven, alias angelessignerCA, no explicit block | [CERT] | [Block 639] §639.1-2 | cited |
| 4 | version-targeting by SDK path, deliberate; bump vendorVersion | [CERT] | [Block 638]/[Block 649] | cited |
| 5 | deploy = backup→gradlew→copy→verify-types; WSL/NTFS bridge | [CERT] | [Block 637]/[Block 639] | cited |
| 6 | packaging rules consolidated | [INFER] | 709.6 | reasoned |

**Tally:** [CERT] ×5 · [INFER] ×1. Block TYPE = **DESIGN/SYNTHESIS** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

MBP5 CLOSED. Next: **MBP6** (the reference-exemplar catalog + consolidated improvement recommendations + the
finalized `docs/module-best-practices.md` — focus-closing block).
