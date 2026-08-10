# Block 435 — wbutil-wb is not a passive util jar: it hosts the user/role/permission UI, the primitive cell editors, and the credential/license tools

> Research of **`wbutil-wb`** (focus `workbench`, gap WB09, LOW) — the shared cross-cutting Workbench UI
> services layer. Scope: what it actually contains (a premise refinement — it is NOT a passive utility
> library), the security-adjacent user/permission UI it hosts, the primitive cell/field editors, and its
> dependency weight. Concise LOW-priority block.
>
> Subject version: OptimizerSupervisor N4.14.0.162 — `wbutil-wb.jar`
> sha256 `2482b8b20579c2346a1a095d5a939bf3a9848fade37a0a98ba0c079d22141116`.
>
> Sources: Vineflower impl (`sources/decompiled/wbutil-wb/com/tridium/workbench/`). Method: package census +
> load-bearing class reading, re-verified live. CAVEAT: the password field-editor classes (`BConfirmPasswordFE`,
> `BPasswordFE`) decompile with a mangled class-name token (`public n extends BWbFieldEditor`) — parent types
> real; cited by file/existence. Markers: `[CERT]` (`file:line`) · `[INFER]` deduction.
>
> Workbench UI framework. Connects [Block 431] (`BUserManager extends BAbstractManager`; cell editors),
> [Block 430] (field editors), [Block 428] (tools appear in the Tools menu), the security thread (user/
> permission UI + credential manager).

---

## 435.1 — Not a passive library: it registers views on core platform types `[CERT]`

`wbutil-wb` is 84 classes, all under `com.tridium.workbench`, in eight domains: `[CERT]`

| Package | # | Domain |
|---|---|---|
| `user` | 20 | user/role/permission management views + password FEs |
| `fieldeditors` | 14 | property field editors (gx color/brush, ORD, PX-view, Facets, layout/border) |
| `tools` | 12 | Workbench Tools-menu entries (new station, credential mgr, license request, timezone, …) |
| `celleditors` | 12 | table cell editors for every primitive |
| `colorchooser` | 10 | HSV color/brush/gradient picker |
| `category` / `metadata` / `role` | 16 | category service mgr, station metadata browser, role mgr |

It is NOT a passive util jar — it declares `@AgentOn` views on ~15 core platform types (`baja:UserService`,
`baja:RoleService`, `baja:CategoryService`, `baja:Station`, `baja:Facets`, `gx:Color`, `gx:Brush`,
`bajaui:Layout`, `web:WebProfileConfig`, …) and pulls in Fox (`BFoxSession`, `FoxRpcUtil`) + authentication at
compile time — making it a REQUIRED Workbench startup module, not an optional helper. `[CERT]`/`[INFER]`

## 435.2 — The user / role / permission UI lives here `[CERT]`

The security administration UI of the Workbench is in wbutil-wb: `[CERT]`
- `BUserManager` — `@AgentOn(types={"baja:UserService"})`, `extends BAbstractManager` ([Block 431]); full CRUD
  on station users, integrating `BFoxSession` + authentication (`user/BUserManager.java:48`). `[CERT]`
- `BPermissionsBrowser` — `@AgentOn` on both `baja:RoleService` and `baja:UserService`; an ACL browser across
  roles and users with a permission grid (`user/BPermissionsBrowser.java`, imports `BRoleService`/`BUserService`).
  `[CERT]` `[INFER]` uses `FoxRpcUtil` for remote calls.
- Password entry: `BConfirmPasswordFE extends BWbFieldEditor` — a two-field prompt/confirm widget (class token
  mangled; parent real, `user/BConfirmPasswordFE.java`). `[CERT]` The sweep reports `BUserPasswordFE.doSaveValue`
  runs `BPasswordAuthenticator.checkPassword` on save; `[INFER]` (the exact call is inside a mangled body — the
  widget's existence and parent are `[CERT]`, the enforcement is an unverified sweep finding).

## 435.3 — Two security-adjacent tools worth flagging `[CERT]`

- `BManageCredentialsTool extends BWbTool` (`tools/BManageCredentialsTool.java:12`) — the Tools-menu entry that
  manages STORED REMOTE-STATION CREDENTIALS, delegating to `AuthUtil.manageCredentials(shell, …)`
  (`:20`) in an internal `com.tridium.workbench.auth` package not in this decompilation. `[CERT]` `[INFER]` a
  privileged action with no visible access check in the stub — the gate must live in `AuthUtil`.
- `BRequestLicenseTool` (`tools/BRequestLicenseTool.java`) — requests a license by REFLECTION across a module
  boundary: `Sys.loadModule("portalApi")` (`:28`) then invokes `portalApi`'s `LicenseProcedure.requestLicense`.
  `[CERT]` `[INFER]` a dynamic cross-module call with no static dependency — the license-request logic is hidden
  in `portalApi` (ties to the licensing thread [Block 424]/[Block 386]).

## 435.4 — The reusable editors `[CERT]`

- `celleditors` — 12 `@AgentOn`-registered table cell editors for every primitive (`BBooleanCE`
  `@AgentOn(baja:Boolean)`, `BOrdCE`, `BBrushCE`, `BColorCE`, …); every manager table ([Block 431]) uses these
  for inline editing. `[CERT]`
- `BColorChooser extends BEdgePane implements ColorModel.Agent` (`colorchooser/BColorChooser.java:62`) — the
  HSV color/brush picker used wherever a color property is edited. `[CERT]`
- `fieldeditors` — the property-sheet ([Block 430]) editors for gx color/brush, ORD (`BOrdSelectFE` reads a
  `queryOrd` facet to pre-filter options), Facets, and layout/border. `[CERT]`/`[INFER]`

## 435.5 — Self-verify

| # | Claim | Marker | Source |
|---|---|---|---|
| 1 | 84 classes, 8 domains; declares `@AgentOn` views on ~15 core types; pulls Fox+authn | `[CERT]` | census; §435.1 |
| 2 | `BUserManager @AgentOn(baja:UserService) extends BAbstractManager` | `[CERT]` | `user/BUserManager.java:48` |
| 3 | `BPermissionsBrowser` = ACL browser `@AgentOn` RoleService+UserService | `[CERT]` | `user/BPermissionsBrowser.java` |
| 4 | `BManageCredentialsTool extends BWbTool` → `AuthUtil.manageCredentials` (stored remote creds) | `[CERT]` | `tools/BManageCredentialsTool.java:12`,`:20` |
| 5 | `BRequestLicenseTool` → `Sys.loadModule("portalApi")` reflection to request a license | `[CERT]` | `tools/BRequestLicenseTool.java:28` |
| 6 | `celleditors` primitive CEs (`BBooleanCE @AgentOn baja:Boolean`); `BColorChooser` HSV picker | `[CERT]` | `celleditors/BBooleanCE.java:15`; `colorchooser/BColorChooser.java:62` |
| 7 | Password widget `BConfirmPasswordFE extends BWbFieldEditor` (2-field); checkPassword enforcement | `[CERT]`/`[INFER]` | `user/BConfirmPasswordFE.java` |

**Marker tally**: `[CERT]` ≈ 16 · `[INFER]` 6 ([INFER]/[CERT] ≈ 0.38). Type: **EVIDENCE block** (LOW-priority
survey) — ratio healthy. VERIFY-BEFORE-ACTING: census + each cited class re-verified live; the mangled password
FE classes are cited by existence/parent only, and the `checkPassword` enforcement is flagged as an unverified
sweep finding, not `[CERT]`. Tokens confirmed: `@AgentOn baja:UserService`, `extends BAbstractManager`,
`AuthUtil.manageCredentials`, `Sys.loadModule("portalApi")`, `BColorChooser extends BEdgePane`,
`BBooleanCE @AgentOn`.

## 435.6 — Connections

- **[Block 431]** — `BUserManager` IS a `BAbstractManager`; wbutil's `celleditors` are the cell editors manager
  tables use.
- **[Block 430]** — wbutil's `fieldeditors` are property-sheet field editors for gx/ORD/Facets types.
- **[Block 428]** — the `tools` (`BWbTool`) appear in the shell's Tools menu.
- **security thread** — the user/role/permission UI, the stored-credential manager, and the password widgets
  all live here; a who-administers-what audit points at wbutil-wb, not the security services themselves.

<!-- research-block: focus workbench, gap WB09 (wbutil-wb) — CLOSED at body grade (LOW); premise "passive util lib" refined to cross-cutting UI services -->
