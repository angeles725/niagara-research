# B777 · Security-module end-to-end authoring — the `saml-rt` skeleton (service → auth-scheme SPI → inline permissions → signing → integration) (MAE6, D12)

> **Scope**: a synthesis walk of ONE security module (`saml-rt`, never opened before) as the canonical
> security-module authoring skeleton — module.xml → `BAbstractService` → `BAuthenticationScheme` SPI → permissions →
> signing → station integration. The D12 analog of what B778 did for a plain service. Focus:
> `module-authoring-exemplars` (MAE6 / dimension D12). Kit destination: `types/logic.md` + `corpus-index.md`.
>
> **Sources**: FUENTE 3 decompiled/packaged `saml-rt` (`BSAMLIdPService`, `BSAMLAuthenticationScheme`,
> `SAMLLoginModule`, `BSamlIdpSecurityDashboardProviderAgent`, module.xml, META-INF), `gauth-rt`
> (`BGoogleAuthenticationScheme`); verified this session at `organized/`. FUENTE 1: B510 (BAuthenticationScheme),
> B563 (SecurityDashboard SPI), B721 (module-permissions.xml), B18 (signing), B757/B778 (service registration).
> READ-ONLY. English (post-B115).

---

## 777.1 — `saml-rt` map (the never-opened module) `[CERT]`
50 decompiled top-level classes (48 `com.tridium.saml.*` + 2 bundled `com.onelogin.saml`); module.xml registers 26
`<type>`s. Header (`saml-rt/…/META-INF/module.xml:2`): `name="saml-rt" vendor="Tridium" vendorVersion="4.14.0.162"
description="SAML Authentication Module" preferredSymbol="saml" runtimeProfile="rt"` + 22 deps (baja/fox-rt/web-rt/
jetty-rt/net-rt…). Key parts: the IdP **service** `BSAMLIdPService`, the RP **auth scheme** `BSAMLAuthenticationScheme`,
a **dashboard** provider agent, a rank mix-in on `baja:AuthenticationScheme`, config model (`idp/*`, `attributes/*`),
and 4 servlets (web.xml). moduleParts `saml-ux`/`saml-wb`.

## 777.2 — The security-module skeleton `[CERT]`
- **(a) module.xml** — `runtimeProfile="rt"`, deps on `baja` + `web-rt`/`fox-rt` for the auth+servlet surface, every
  B-type under `<types>`.
- **(b) The service** — `public final class BSAMLIdPService extends BAbstractService implements
  BIRestrictedComponent` (`BSAMLIdPService.java:104-105`); registration-by-placement via `getServiceTypes()` returning
  `{TYPE}` (:275) — a singleton under `/Services` (ties B757/B778). License gate `LICENSE_FEATURE="samlDP"`.
- **(c) The scheme SPI** — `public final class BSAMLAuthenticationScheme extends BSSOAuthenticationScheme`
  (`BSAMLAuthenticationScheme.java:78-79`), scheme name `"n4saml"`.
- **(d) The dashboard SPI (B563)** — `BSamlIdpSecurityDashboardProviderAgent` `@NiagaraType(agent={@AgentOn(types=
  {"saml:SAMLIdPService"})})` implementing `BISecurityDashboardProviderAgent` (setItemsSource/getSectionHeader/
  getItems…).

## 777.3 — The `BAuthenticationScheme` SPI contract; real auth is in a LoginModule `[CERT]`
Hierarchy: `BAuthenticationScheme` (baja `javax.baja.authn`) → `BSSOAuthenticationScheme` → `BSAMLAuthenticationScheme`;
sibling `BPasswordAuthenticationScheme` → `BGoogleAuthenticationScheme` (gauth). Author overrides on the scheme
(`BSAMLAuthenticationScheme.java`): `getSchemeName()` → `"n4saml"` (:200); `getLoginConfiguration()` (:226) returns a
JAAS `Configuration` wiring the module's `NiagaraLoginModule` (`new NiagaraLoginConfiguration(SAMLLoginModule.class…
REQUIRED)`); `getDefaultAuthenticator()`; `getLoginRedirectURL()` → `"/saml/samlrp"`. **The authenticate/challenge
logic lives in the JAAS LoginModule, not the scheme**: `SAMLLoginModule extends NiagaraLoginModule` with `login()`
(issues a `SAMLCallback`, pulls auth info, throws `NiagaraFailedLoginException` on lockout). Second exemplar
`BGoogleAuthenticationScheme` (gauth): `getSchemeName()`→`"gauth"`, `getLoginConfiguration()` wiring
`GoogleAuthLoginModule`. **Author obligation (B510)**: subclass a `BAuthenticationScheme` family class, name the
scheme, return a `Configuration` pointing at your `NiagaraLoginModule`, supply a default authenticator.

## 777.4 — Permissions: the source `module-permissions.xml` is INLINED into the jar's module.xml by the plugin `[CERT]`

> **§14 ADDENDUM (2026-09-05 — corrected after a real-jar + PR7 check, QA/lead-verified):** an earlier version of this
> section claimed §777.4 "CORRECTS B721". That is an OVER-REACH, retracted. It is ONE source file and ONE
> `<permissions>` ELEMENT with TWO kinds of CHILDREN — not two mechanisms:
> - the wizard SOURCE `module-permissions.xml` (`wbutil-wb/rc/module-permissions.xml`) = a `<permissions>` element with
>   `<niagara-permission-groups type="all|workbench|station">` + `<req-permission>` entries (the RBAC groups — B636/B721);
> - the gradle plugin INLINES that element into the built jar's `META-INF/module.xml` (see the CompPan-rt jar
>   module.xml lines 11-15 — inlined groups, NO separate file in the jar);
> - a SECURITY module's inlined `<permissions>` ADDITIONALLY carries `<java-permissions type="station">` (SAMLPermission,
>   KeyStorePermission, FilePermission — saml-rt).
> So B777 = ARTIFACT level + the `<java-permissions>` child; B636/line 15 = SOURCE file + the `<niagara-permission-groups>`
> child; B721 (source) was right. The real fact is the source→artifact INLINING of ONE element, not a correction of B721.
> [ev: wbutil-wb/rc/module-permissions.xml (source wizard template); CompPan-rt jar module.xml:11-15 (inlined groups);
> saml-rt jar (adds java-permissions); QA/lead-verified 2026-09-05]

saml-rt's DECOMPILED JAR ships **NO separate `module-permissions.xml`** (find-zero; the only such files in the whole corpus are under
`wbutil-wb/rc/`). A security module declares its Java permissions INLINE in module.xml under `<permissions>
<java-permissions type="station">` (`module.xml:72-73`): a custom `com.tridium.security.SAMLPermission`
(`addAuthnInfo`/`removeAuthnInfo`/`createAuthnRequest`, :76), `NiagaraBasicPermission "UNAUTHENTICATED_SERVLET"` /
`"GET_AUTHENTICATED_USER"`, `KeyStorePermission "userKeyStore" read`, `NiagaraSocketPermission`. gauth-rt does the same.
**Source→artifact (NOT a correction of B721, per the §14 addendum)**: for a security module, the permission grants are — in the built JAR — the inline `<permissions>` block in module.xml (the plugin having inlined the source file),
granting the exact JAAS/Niagara permissions the login flow needs (unauthenticated-servlet reach, keystore read, the
custom scheme permission) — not a rc `module-permissions.xml`.

## 777.5 — Signing (B18): required for a permission-granting module `[CERT]`
saml-rt is jar-signed — `META-INF/` carries `NIAGARA4.RSA` (PKCS#7) + `NIAGARA4.SF` + a `MANIFEST.MF` with
`Sealed: true` and per-entry SHA-256 digests. **Author obligation**: a security module that grants privileged
permissions MUST be signed with the Niagara code-signing cert; the station verifies RSA/SF against the manifest
digests BEFORE trusting the inline `<permissions>` grants.

## 777.6 — Station integration `[CERT/INFER]`
Service placed under `/Services` (getServiceTypes→{TYPE}); at start it wires to `BUserService`, `BNiagaraNetwork`, and
`BAuthenticationService`. The scheme is registered into the auth-scheme registry via `@AgentOn "baja:AuthenticationScheme"`
(gauth directly; SAML via a rank mix-in) — `BAuthenticationService` discovers all agents-on-AuthenticationScheme, and
an operator adds a scheme instance + assigns users; the scheme name (`"n4saml"`/`"gauth"`) is the registry key.
Servlets (web.xml): `/samlrp/*`, `/assertionConsumerService`, IdP endpoints; the login redirect `/saml/samlrp` is the
browser entry.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | saml-rt = 50 classes / 26 registered types; service+scheme+dashboard+config; header at module.xml:2 | [CERT] | saml-rt module.xml:2; class count |
| 2 | Service = `BSAMLIdPService extends BAbstractService`, registered via `getServiceTypes()→{TYPE}` | [CERT] | BSAMLIdPService.java:104-105,275 |
| 3 | Scheme = `BSAMLAuthenticationScheme extends BSSOAuthenticationScheme`; `getSchemeName`/`getLoginConfiguration` | [CERT] | BSAMLAuthenticationScheme.java:78-79,200,226 |
| 4 | Real auth is in a `NiagaraLoginModule` wired by `getLoginConfiguration()`; gauth is a second exemplar | [CERT] | SAMLLoginModule (extends NiagaraLoginModule); BGoogleAuthenticationScheme.java:30,40 |
| 5 | In the built JAR, permissions are INLINE `<permissions><java-permissions type="station">` in module.xml — the plugin inlined the SOURCE `module-permissions.xml` (reconciles, does NOT correct, B721 — see §14 addendum) | [CERT] | saml-rt jar module.xml:72-76; find module-permissions.xml in jar = 0; DashboardPan-ux source has one |
| 6 | Signed module: NIAGARA4.RSA + NIAGARA4.SF + sealed manifest (B18) | [CERT] | saml-rt/META-INF/NIAGARA4.{RSA,SF} |
| 7 | Registration = `@AgentOn "baja:AuthenticationScheme"` + service placement /Services | [CERT] | gauth BGoogleAuthenticationScheme.java:28; saml rank mix-in |

**Tally**: 6 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session at `organized/`.

## Connections
- **B510** (BAuthenticationScheme — this walks a concrete subclass), **B563** (SecurityDashboard SPI — the dashboard
  agent), **B721/B636** (source `module-permissions.xml` — §777.4 RECONCILES with it: the plugin inlines the SOURCE file into the jar module.xml; source and artifact are both right, §14 addendum),
  **B18** (signing), **B757/B778** (service registration-by-placement). **B776** (action protection — the RBAC bits a
  scheme's users get). **access-control B558-B566** (the RBAC subsystem the scheme feeds).

## Open gaps
- **MAE6-G1** — the `SAMLLoginModule.login()` callback/assertion-validation flow and the `attributes/*` user-property
  mapping are summarized, not walked; a bounded follow-up if a builder writes a full SSO scheme.

## Kit implication (→ `types/logic.md` + `corpus-index.md`)
Add `saml-rt` as the canonical "security-module authoring skeleton": module.xml (with the permission grants INLINE in a
`<permissions><java-permissions type="station">` block — NOT a separate module-permissions.xml) → a `BAbstractService`
subclass (`getServiceTypes`, B757/B778) → a `BAuthenticationScheme` subclass whose real auth lives in a
`NiagaraLoginModule` wired via `getLoginConfiguration()` (B510), optionally a `BISecurityDashboardProviderAgent`
(B563) → jar-signing (NIAGARA4.RSA/SF, B18) is MANDATORY for the privileged grants → registration by
`@AgentOn "baja:AuthenticationScheme"` + service placement under /Services. Record the source→artifact relationship
(NOT a B721 correction, §14 addendum): a security module's permissions live in a SOURCE `module-permissions.xml`
(B636/B721) that the gradle plugin INLINES into the built jar's `module.xml` `<permissions>` — so a decompiled jar
shows them inline with no separate file.
