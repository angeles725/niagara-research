# RESEARCH-STATE — focus: access-control (STOPPED)

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
covered_blocks: 8
gaps_closed: 8
known_gaps: 8
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: access-control
status: stopped (8/8, investigable=0; AC1→B558 … AC8→B566). B560 = cloudflared runbook (document-mode). §18 retro pending.. Note: B560 = cloudflared runbook (document-mode, not a gap).
seeded_from: AUDIT-FIRST coverage sweep 2026-08-28 (delegated sonnet; pre-flight + AC1 verified inline)
seeded_on: 2026-08-28
gaps_total: 8 investigable (AC1–AC8)
gaps_closed: 8
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
| high | ~~**AC2 BUserPrototypes**~~ | 8-class user-templating subsystem for auto-provisioning from LDAP/SAML; per-property override lock + 4 merge-mode enums (roles=union expands, rest restrictive) | — | **CLOSED → B559** |
| high | ~~**AC3 BCategoryService runtime**~~ | ORD-prefix inheritance, 60s periodic daemon recompute, cap=**256** (not 64), union default, super-user gated | — | **CLOSED → B561** |
| medium | ~~**AC4 password encoder chain**~~ | 10 encoders, two families: HASHED (PBKDF2 10k iter, login, one-way) vs ENCRYPTED (AES-256 reversible, replayable); fork=isReversible; plain.1 risk; 10k iter low | — | **CLOSED → B562** |
| medium | ~~**AC5 SecurityDashboard SPI**~~ | 2-level provider SPI (ItemProvider/Provider/Agent), 4-level status, versioned-JSON aggregate, broadly adopted (email/web/orion/program/bacnet-SC/mqtt) | — | **CLOSED → B563** |
| medium | ~~**AC6 audit-trail wiring**~~ | two channels (AuditEvent diff / SecurityAuditEvent login) via pluggable Sys.getAuditor() singleton (null=silent); sink=history-rt; rich but tamper-evident-free (B393) | — | **CLOSED → B564** |
| medium | ~~**AC7 BRoleHierarchies mixin**~~ | BIMixIn @AgentOn baja:IRole auto-attached to every role; stores comma-set of hierarchy names; seam = roles scope custom nav trees (orthogonal to category visibility) | — | **CLOSED → B565** |
| low | ~~**AC8 UserMonitor + BUserEvent**~~ | userEvent topic + typed BUserEvent (added/removed/renamed/modified); real consumer = supervisor user replication (BNiagaraUserDeviceExt) | — | **CLOSED → B566** |

## Proven-absent / notes

- devguide multi-word queries returned 0 (`devguide-search "security rbac permission"`, `"user role"`) but the
  devguide DOES contain `security/{roles,security,authentication,requestingPermissions,headerAuthentication,
  csrfProtection,scramshaexample}.txt` — available for documentary depth on AC1–AC3.
- `PermissionsManager` iface (1 method `getPermissionsMap(BUser, BIProtected)`) = trivial; fold into AC3/AC6.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **0** — ALL 8 closed (AC1–AC8). Focus STOPPED (§8).
- **Gaps closed**: 8 (AC1→B558, AC2→B559, AC3→B561, AC4→B562, AC5→B563, AC6→B564, AC7→B565, AC8→B566).
- **requires-execution / blocked**: 0.
- **Coverage metric**: 8 / 8 (100%). Plus B560 (cloudflared remote-access runbook, document-mode).
- **Child gaps surfaced (named, out-of-scope)**: hierarchy subsystem proper (BHierarchy/BHierarchyService, candidate focus); at-rest EncryptionKeySource enum ([B393]/[B466]); BAuditHistoryService record schema ([B8]).
