# RESEARCH-STATE — focus: custom-login

<!-- research-state.v1
blocks_in_focus: 1
first_block: 834
last_block: 834
open_gaps: 4
next_gap: B834-G1
coverage: bootstrap (foundational overview done; per-path depth pending)
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
- **Pending:** decompile-level Domo internals; a native Path-B build recipe; live-station deployed reality;
  the stock template + `LoginState` machine.

## Open gaps (prioritized)

| Gap | Title | Status |
|-----|-------|--------|
| **B834-G1** | Decompile `BDomoLoginTemplate` — interfaces, `file:^domo/` read, `login?domo/...` serve, Handlebars binding, 2FA/reset flow (verifies B834 claim 14) | **NEXT** |
| B834-G2 | Native Path B recipe: minimal `BLoginTemplate` module (slotomatic `web:LoginTemplate`, module scheme, build/sign/select) | open |
| B834-G3 | Live-station probe (authorized): read `WebService.loginTemplate` value + `shared/domo/` contents | open |
| B834-G4 | Stock `BDefaultLoginTemplate` render + `LoginState` machine (error / forced reset / 2FA token) | open |

## Iteration history

- 2026-09-06 — B834 bootstrap. Triaged the operator request (nmxdomo-b4.zip + README + "investigate all
  ways to build a custom login"). All three sources consulted. Wrote foundational overview block; seeded
  4 gaps. Sources registered: `nmxdomo-rt.jar`, example themes, vendor README manifest.
