# B721 — module-permissions.xml: the Java Security Manager permission-request manifest (module-dev-workflow addendum)

> Focus: **module-dev-workflow** (closed 5/5) · ADDENDUM block. Closes an item the focus only CATALOGUED, never
> explained: `module-permissions.xml`. Born from an operator question while building the `ColdRoomPan` cold-room
> module manual ("is it how the module looks, or what has permissions?"). Sources: official devguide (FUENTE 2),
> real client module + decompiled code (FUENTE 3), corpus (FUENTE 1). Deliverable touched:
> `docs/manuals/new-module-creation/main.tex` + `main-es.tex` (Part D).

## 721.1 — What it is

[CERT-doc] `module-permissions.xml` is a **permission-request manifest for the Java Security Manager** — the file
where a module developer declares which restricted host/JVM capabilities the module's own CODE needs granted, so
protected operations run without an `AccessControlException`. devguide `security/requestingPermissions.txt` §1:
"the Policy is determined by the contents of a module's 'module.xml' file, in which the module can request which
permissions it needs. This allows third party modules to request and be granted permissions that they would
otherwise not have." Build reference labels it verbatim [CERT-doc] `build.txt:160`: "`module-permissions.xml` -
Declares permissions for module" (sibling `build.txt:155`: "`module.palette` - Defines Palette information").

The build layout places it in the module part's source dir alongside `module-include.xml` / `module.palette`; its
requests are carried into the built `module.xml`, which becomes the runtime Policy.

## 721.2 — Schema / elements

[CERT-doc] `security/requestingPermissions.txt` §2.1.1:
- **`<permissions>`** — root element ("root element of the permission request format").
- **`<niagara-permission-groups type="...">`** — a block; `type` ∈ `station` (running station) / `workbench`
  (Workbench) / `all` (both). Multiple blocks allowed.
- **`<req-permission>`** (required) / **`<opt-permission>`** (optional) — one request each. §2.1.3: a required
  permission is "necessary for the module to either run or perform its primary function"; optional is handled
  gracefully if denied.
  - **`<name>`** (required) — the permission-group name requested.
  - **`<purposeKey>`** (required) — lexicon/justification string shown to the end user.
  - **`<parameters>`** (optional) → **`<parameter name=".." value=".."/>`** — narrows the grant (e.g. `hosts`,
    `ports`, `type`).

[CERT-doc] §2.1.3 nuance: "In Niagara 4.2, all permissions requested by a module are automatically granted... the
'optional' vs 'required' setting has no real effect other than informing the end user." So the file is
declarative/advisory at 4.2+, not an enforcement gate that can deny a signed module.

## 721.3 — The permission-group catalog (risk-rated)

[CERT-doc] `security/requestingPermissions.txt` §2.2 — the allowed `<name>` values, each mapped to concrete Java
permissions and a Risk Level (SEVERE / MODERATE / MILD): `ACCESS_CLASS` (L164), `AUTHENTICATION` (L183, →
`javax.security.auth.AuthPermission "modifyPrincipals"`), `BACKUPS` (L204, → `FilePermission` on backups),
`DIAGNOSTICS` (L227), `GET_ENVIRONMENT_VARIABLES` (L246), `LOAD_LIBRARIES` (L265), `LOGGING` (L284),
`MANAGE_EXECUTION` (L305), `MBEAN_PERMISSION` (L330), `MODIFY_IO_STREAMS` (L407), `NETWORK_COMMUNICATION` (L424, →
socket access), `SET_SYSTEM_TIME` (L508), `SYSTEM_PROPERTIES` (L562), `THIRD_PARTY_PERMISSION` (L583), `KEY_STORE`
(L631). The mapping to real JVM permissions (files/sockets/auth/system-time/native-libs/keystore) confirms the
file governs **JVM capabilities of the module code**, not UI.

## 721.4 — Real example + how it is produced/consumed

[CERT] The real client module `chihuahua` ships the default template with EVERY permission commented out — it
requests no extra capabilities. `chihuahua-rt/module-permissions.xml:1-19` (byte-identical `chihuahua-ux/`):

```xml
<permissions>
  <niagara-permission-groups type="all">
    <!-- Insert any global permissions here. -->
  </niagara-permission-groups>
  <niagara-permission-groups type="workbench">
    <!-- Insert any workbench specific permissions here. -->
  </niagara-permission-groups>
  <niagara-permission-groups type="station">
    <!--<req-permission>-->
    <!--<name>NETWORK_COMMUNICATION</name>-->
    <!--<purposeKey>Outside access for Driver</purposeKey>-->
    <!--<parameters>-->
      <!--<parameter name="hosts" value="127.0.0.1"/>-->
      <!--<parameter name="ports" value="*"/>-->
      <!--<parameter name="type" value="all"/>-->
    <!--</parameters>-->
    <!--</req-permission>-->
  </niagara-permission-groups>
</permissions>
```

Produced/consumed by:
- [CERT] devkit emits it from a template: `organized/devkit/devkit-wb/decompiled/com/tridium/gradle/plugins/templates/NiagaraModulePartGenerator.java:41` → `addTemplateWrite("gradle/module/module-permissions.xml.vm", "module-permissions.xml")`.
- [CERT] program-module builder writes it in a build step: `organized/program/program-wb/decompiled/com/tridium/program/ui/module/BuildHelper.java:352` (`new File(getModuleDir(), "module-permissions.xml")`) inside `WriteModulePermissionsStep` (`BuildHelper.java:345`).
- [CERT] at runtime the Security Dashboard buckets modules by requested-permission risk and bad signatures:
  `organized/nss/nss-rt/decompiled/com/tridium/nss/dashboard/BSecurityDashboardModulePermissions.java:57`, severity
  constants `SEVERE_/MODERATE_/MILD_PERMISSIONS_SUMMARY` L61-63, `BAD_SIGNATURE_MODULE_SUMMARY` L67; doc [CERT-doc]
  `guides-clean/StationSecurity/nss-SecurityDashboardView.txt:83` ("Module Permissions (for example, SEVERE
  permissions requested)").
- [CERT] `docSource/.../baja/javax/baja/security/BICertificateAliasAndPasswordContainer.java:80` references "your
  module-permissions.xml entry for the station" for keystore access (KEY_STORE) — reinforcing capability-grant use.

## 721.5 — What it is NOT (corrects the operator guess)

[CERT-doc] It does **NOT** control how the module looks — that is `module.palette` (populates the Workbench
palette), a distinct sibling file (`build.txt:155` vs `:160`). It is **NOT** per-user RBAC: it does not gate which
USER can invoke actions / read-write / admin on the module's components — that is Niagara's separate Category /
Role / Permissions user model. `module-permissions.xml` = what the module's own code is permitted to do on the
host JVM (files, sockets, native libs, auth, system time, backups, keystore), enforced (pre-4.2) by the Java
Security Manager and surfaced to admins by the Security Dashboard.

## Connections

- Focus `module-dev-workflow` file table catalogued this file without explaining it — this block closes it.
  Sibling `module.palette` → [Block 634]. Slotomatic / `module-include.xml` → [Block 631]/[Block 636]. Real
  example `chihuahua` → focus `chihuahua` [Block 169]. Security Dashboard → nss module. Applied to
  `docs/manuals/new-module-creation/main.tex` + `main-es.tex` (Part D).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | it is a JSM permission-request manifest (module code capabilities, via module.xml Policy) | [CERT-doc] | requestingPermissions.txt §1; build.txt:160 | cited |
| 2 | schema: `<permissions>` → `<niagara-permission-groups type=station|workbench|all>` → `<req/opt-permission>`(`<name>`,`<purposeKey>`,`<parameters>`) | [CERT-doc] | requestingPermissions.txt §2.1.1 | cited |
| 3 | 4.2+ auto-grants all requested; req vs opt only informs the user | [CERT-doc] | requestingPermissions.txt §2.1.3 | cited |
| 4 | permission-group catalog (15 names) maps to concrete JVM permissions + risk levels | [CERT-doc] | requestingPermissions.txt §2.2 | cited |
| 5 | chihuahua ships the fully-commented stub (no extra permissions) | [CERT] | chihuahua-rt/module-permissions.xml:1-19 | cited |
| 6 | devkit/program write it; Security Dashboard flags by permission risk | [CERT] | NiagaraModulePartGenerator.java:41; BuildHelper.java:345,352; BSecurityDashboardModulePermissions.java:57-67 | cited |
| 7 | NOT appearance (that is module.palette) and NOT per-user RBAC | [CERT-doc]/[INFER] | build.txt:155 vs :160; §1 | cited/derived |

**Tally:** [CERT-doc] ×5 · [CERT] ×2 · [INFER] ×1 (claim 7 partial). Block TYPE = **artifact-documentation**
(FUENTE-2 official doc + FUENTE-3 real example/code corroborated).

## Open gaps

- None investigable. Optional child gap **B721-G1** (LOW): the exact enforcement path pre-4.2 vs the 4.2+
  auto-grant transition (which release removed hard denial) — not needed for module authoring. Not opened.
