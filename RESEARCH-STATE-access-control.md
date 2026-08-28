# RESEARCH-STATE — focus: access-control (ACTIVE)

> Multi-focus corpus (METHODOLOGY §16). SEEDED by an AUDIT-FIRST coverage sweep (§13) on 2026-08-28 (delegated
> sonnet, verified inline) that mapped the N4 RBAC/authorization subsystem against the corpus and separated
> REMITTANCE (already covered) from genuine gaps.
>
> **Angle (§b2):** the N4 authorization subsystem — users, roles, permissions, categories, password policy, and
> the enforcement/audit wiring — as a dedicated subsystem. The thread was DISPERSED (B11 concept model, B30
> slot-level enforcement, B48 frontend gating, B341 runAsUser, B398/B490 hardening, B435 WB UI, B494/B510 auth
> schemes) but never given a subsystem focus. Read-only, decompiled-Java (`organized/…`) + original Tridium
> javadoc (`organized/docSource/…`) + devguide (`niagara-help`). Corpus language = **English**.
>
> **Scope:** the authorization/identity model and its enforcement — NOT the auth-scheme SPI (already [B510]),
> NOT the OEM scheme implementations ([B494]), NOT SCRAM wire ([B457]/[B134]), NOT TLS/cert crypto
> (`security.crypto`, out of RBAC scope).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 1
gaps_closed: 1
known_gaps: 8
investigable_open: 7
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: access-control
status: active (1/8; AC1 → B558 DONE; NEXT AC2 BUserPrototypes)
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; pre-flight + AC1 verified inline)
seeded_on: 2026-08-28
gaps_total: 8 investigable (AC1–AC8)
gaps_closed: 0
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Class surface (audit, source-confirmed)

~113 RBAC-relevant public classes: `javax.baja.user` (16: BUser, BUserService, BPasswordStrength, BUserPrototype[s],
merge-mode enums, UserMonitor, BUserEvent), `javax.baja.role` (7: BRole/BRoleService/BAdminRole/BIRole…),
`javax.baja.security` (50: BPermissions, BPermissionsMap, BIProtected, AccessSlotCursor, BPassword + 8 encoders,
Auditor/SecurityAuditor, AuditEvent), `javax.baja.security.dashboard` (8 SPI), `javax.baja.category` (7:
BCategoryService, BCategoryMask, BOrdToCategoryMap), `com.tridium.user` (BGlobalPasswordConfiguration,
BUserPasswordConfiguration). Out of scope: `security.crypto` (8) + `authn` (12, → [B510]).

## REMITTANCE — already covered (cite, do NOT re-derive)

- RBAC concept model (BUser/BRole/BCategory/BPermissions 6-bit, lockout params, scheme survey) → **[B11]**
- Slot-level enforcement (AccessSlotCursor, BIProtected, silent-deny vs PermissionException, Auditor iface) → **[B30]**
- Frontend RBAC visibility gating (server canX truth, role-name client match) → **[B48]**
- Inbound write authorization via runAsUser identity → **[B341]**
- Security hardening checklist (SEC-01..SEC-22) → **[B398]** + **[B490]**
- Workbench user/role/permission management UI → **[B435]**
- Pluggable auth-scheme implementations (SAML/LDAP/gauth/clientCert) → **[B494]**
- BAuthenticationScheme SPI authoring + authenticate() loop → **[B510]**
- SCRAM-SHA-256 programmatic login recipe → **[B457]**
- SecurityDashboard aggregate view (nss consumer side) → **[B112]**

## Gap-backlog (prioritized) — genuine uncovered RBAC surfaces

| Priority | Gap | Scope | Where (`organized/…`) | Status |
|---|---|---|---|---|
| high | ~~**AC1 password policy enforcement**~~ | BPasswordStrength (built-in IPropertyValidator) + the checkPassword wiring + per-user BUserPasswordConfiguration; **CORRECTED B11 §11.3.5** ("complexity NO built-in" = FALSE) | — | **CLOSED → B558** |
| high | **AC2 BUserPrototypes** | 8-class user-templating subsystem for auto-provisioning from LDAP/SAML; per-property override flags + 4 merge-mode enums; [B494] USES it, never documents it | `baja/…/javax/baja/user/BUserPrototype[s].java` (+ docSource) | **NEXT** |
| high | **AC3 BCategoryService runtime** | category enforcement mechanics: BOrdToCategoryMap.resolve(), ORD→category-index, 64-category limit, history auto-categorization, propagation | `baja/…/javax/baja/category/BCategoryService.java`, `BOrdToCategoryMap.java`, `BCategoryMode.java` | open |
| medium | **AC4 password encoder chain** | 8 encoders; default BPbkdf2HmacSha256; BReversible/BPlain risk; migration on upgrade; how BPassword stores encoded credentials | `baja/…/javax/baja/security/BPbkdf2HmacSha256PasswordEncoder.java`, `BPassword.java`, `BReversiblePasswordEncoder.java` | open |
| medium | **AC5 SecurityDashboard SPI** | the CONTRIBUTOR side (BISecurityDashboardProvider/ItemProvider/ItemBuilder) — [B112] only covered the nss consumer | `baja/…/javax/baja/security/dashboard/` | open |
| medium | **AC6 audit-trail wiring** | who calls Auditor.audit(), AuditEvent vs SecurityAuditEvent fields, path from authenticateFailed() to SecurityHistory | `baja/…/javax/baja/security/AuditEvent.java`, `SecurityAuditEvent.java`, `Auditor.java`, `SecurityAuditor.java` | open |
| medium | **AC7 BRoleHierarchies mixin** | `@AgentOn(baja:IRole)` mixin assigning hierarchy names to a role — RBAC↔hierarchy seam, undocumented | `hierarchy/hierarchy-rt/vineflower/javax/baja/hierarchy/BRoleHierarchies.java` | open |
| low | **AC8 UserMonitor + BUserEvent** | framework hooks for reactive RBAC (userEvent topic on BUser slot changes; cache invalidation on role change) | `baja/…/javax/baja/user/UserMonitor.java`, `BUserEvent.java` | open |

## Proven-absent / notes

- devguide multi-word queries returned 0 (`devguide-search "security rbac permission"`, `"user role"`) but the
  devguide DOES contain `security/{roles,security,authentication,requestingPermissions,headerAuthentication,
  csrfProtection,scramshaexample}.txt` — available for documentary depth on AC1–AC3.
- `PermissionsManager` iface (1 method `getPermissionsMap(BUser, BIProtected)`) = trivial; fold into AC3/AC6.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: 8 (AC1–AC8). Focus ACTIVE.
- **Gaps closed**: 0.
- **requires-execution / blocked**: 0.
- **Coverage metric**: 0 / 8.
