# RESEARCH-STATE — focus: custom-login

<!-- research-state.v1
blocks_in_focus: 3
first_block: 834
last_block: 863
open_gaps: 2
next_gap: B834-G3
coverage: bootstrap + decompile-internals (G1+G4 closed) + build recipe (G2 closed); G3=live-probe open; G3 is the only investigable gap remaining (B862-G1 is minor/parked)
-->

## Scope

Every way to customize or replace the Niagara WebService login screen: the native
`javax.baja.web.BLoginTemplate` extension point + the `WebService.loginTemplate` (`BTypeSpec`,
target `web:LoginTemplate`) control point, and the third-party **NiagaraMods Domo** (`nmxdomo`)
module (turn-key themes `DomoPanelLeft`/`DomoFloatingCard` + the no-Java `DomoUserCustom` HTML/HBS/JS/CSS
path). Includes the auth/branding foundation already in B9/B11/B18 and the 4.10u3+ `file:^` restriction.

Operator context: Cristian has installed `nmxdomo` and built a working login via `DomoUserCustom`; the
vendor README matches his real workflow. Live footprint: `domo` folder observed in the station `shared/`
(B187/B289).

## Coverage

- **Done (B834):** the four paths (A branding slots · B native `BLoginTemplate` module · C Domo built-in
  templates · D Domo `DomoUserCustom` theme); the `loginTemplate` control point; `BLoginTemplate` /
  `ILoginTemplateEx` / `IStateLoginTemplate` contracts; Domo module facts + `$domo` JS API + slots +
  limitations; the 4.10u3/4.10u4 `file:^` version matrix.
- **Done (B862):** decompile internals of `BDomoLoginTemplate` (G1) and `BDefaultLoginTemplate` + `LoginState`
  (G4). Key findings: Domo engine is a plain utility class (not a `BLoginTemplate` subclass); none of the
  three thin subclasses implement `ILoginTemplateEx`/`IStateLoginTemplate`; `file:^domo/` serving confirmed
  as server-side read; Handlebars compiled inline on each request; 2FA detection via Google Authenticator
  scheme name string; stock template uses Velocity `.vm` files; `LoginState` has no named integer constants
  in the corpus; B834 §2 [INFER] corrected.
- **Done (B863):** native Path B build recipe (G2). Module skeleton (`niagara-module.xml` / `module-include.xml`
  / `@NiagaraType` source / `src/rc/` assets / part + root Gradle scripts), build/sign steps, deploy and
  select-in-station procedure, `module://` asset pipeline, version/compat caveats (min AX 3.2.2 →
  all N4; `IStateLoginTemplate` optional in N4.14; 4.10u3 `file:^` lockdown irrelevant to module:// scheme).
- **Pending:** live-station deployed reality (G3); `LoginState` integer constants (B862-G1, minor).

## Open gaps (prioritized)

| Gap | Title | Status |
|-----|-------|--------|
| **B834-G3** | Live-station probe (authorized): read `WebService.loginTemplate` value + `shared/domo/` contents | **NEXT** |
| B862-G1 | `LoginState` integer constants: no named constants found in corpus; need the caller/implementation class | open (minor, parked) |
| ~~B834-G2~~ | ~~Native Path B recipe: minimal `BLoginTemplate` module~~ | **CLOSED** by B863 |
| ~~B834-G1~~ | ~~Decompile `BDomoLoginTemplate`~~ | **CLOSED** by B862 |
| ~~B834-G4~~ | ~~Stock `BDefaultLoginTemplate` render + `LoginState` machine~~ | **CLOSED** by B862 |

## Iteration history

- 2026-09-06 — B834 bootstrap. Triaged the operator request (nmxdomo-b4.zip + README + "investigate all
  ways to build a custom login"). All three sources consulted. Wrote foundational overview block; seeded
  4 gaps. Sources registered: `nmxdomo-rt.jar`, example themes, vendor README manifest.
- 2026-09-11 — B862. Decompiled `nmxdomo-rt.jar` (vineflower); read `BDefaultLoginTemplate` + `LoginState`
  + `LoginServlet` + `LoginFileServlet` docSource/vineflower. Closed G1 + G4. Corrected B834 §2 [INFER].
  Seeded B862-G1 (LoginState constants). Advanced `next_gap` to B834-G2.
- 2026-09-11 — B863. Built native Path B recipe from framework sources + our module tree. Closed G2.
  Key findings: no `<agent>` in type registration; `src/rc/` → `module://` pipeline; NCCB-4026 is
  AX-era (not N4.14); `IStateLoginTemplate` optional. Advanced `next_gap` to B834-G3 (only investigable
  gap remaining; B862-G1 parked as minor).
