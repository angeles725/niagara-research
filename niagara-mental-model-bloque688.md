# B688 — JACE_UMBRELLA deployed RBAC (SC4): one super-user admin, no policy overrides, legacy AX scheme on, and a dangling category-3 reference whose current impact is nil

> Focus: **jace-station-config** · Gap **SC4** (deployed RBAC — UserService/RoleService/CategoryService/
> AuthenticationService/SecurityService). Sources: `config.bog` file.xml, extracted READ-ONLY from the JACE-8000
> boot microSD. Redacted evidence: `sources/probes/B685-jace-station-config/rbac-deployed.txt`.
> **SECRETS DISCIPLINE (live-install):** password/hash values MASKED; only role/template account names
> ("admin", "defaultPrototype") cited verbatim. Marker `[CERT-hw]` (SD artifact). RBAC framework internals =
> REMITTANCE focus `access-control` [Block 558]–[Block 566]; this is the DEPLOYED posture.

## 688.1 — Users: one real account, super-user

[CERT-hw] UserService holds one template + one real account:
- `defaultPrototype` (`t="b:User"`, L61): the new-user template; `roles` field `f="1"` (null — new users
  inherit no roles); empty `UserPasswordConfiguration` (framework defaults); Web+Mobile HTML5HxProfile.
- `admin` (`t="b:User"`, L109): roles = `admin`; `PasswordAuthenticator` = PBKDF2-HMAC-SHA256,
  `[pbkdf2-sha256.1]` format, 10 000 iterations (B685 §685.5), **value MASKED**; `expiration` null (L110); no
  disabled flag; same HTML5HxProfile.

**One real account, `admin`.** No operator/technician/viewer accounts. [CERT-hw]

## 688.2 — Roles: a single AdminRole (super-user)

[CERT-hw] RoleService defines ONE role: `admin` = `t="b:AdminRole"` (L52), with an empty
`hierarchy:RoleHierarchies` extension (L54). `BAdminRole` is Niagara's super-user role — full permissions on
all categories, bypassing category ACLs (framework fact; category editing itself is super-user-gated per
`access-control` [Block 561]: `checkContextForSuperUser`). No least-privilege role is defined.

## 688.3 — Categories + ordMap, and the dangling category-3 reference

[CERT-hw] CategoryService (L25) defines **two** categories: `User` index=1 (L28), `Admin` index=2 (L32). The
`ordMap` (L26) assigns resources to category NUMBERS:

| ORD | → category | defined? |
|---|---|---|
| history:^securityhistory / audithistory / loghistory | 2 (Admin) | yes |
| file:^ (file root) | 2 (Admin) | yes |
| file:^nav | **3** | **NO category index 3 is defined** |
| file:^px | **3** | **NO category index 3 is defined** |

So `file:^nav` and `file:^px` (station navigation + PX presentation files) map to category index **3**, which
has no `b:Category` component — a **dangling category reference** (a latent misconfiguration in the deployed
config). [CERT-hw]

**Framework-semantic REFINE (DE-ESCALATION of the SC4 sweep).** The sweep concluded this "→ open-access
fallback, nav/px exposed." That is NOT supported by the evidence:

1. The corpus [CERT] fact (`access-control` [Block 561], `BCategoryService.java:73,162`) is that an **UNMAPPED**
   ORD falls to the default category (`DEFAULT_MASK=make("1")`). My case is different — the ORD **is** mapped,
   to an index whose Category component is absent. Whether that resolves to open or to denied is **not settled**
   by the corpus and is not readable from the config alone → [INFER], not a fact.
2. **Current impact is nil regardless:** the only account is `admin` (AdminRole super-user, 688.2), which
   bypasses category ACLs entirely. The dangling reference can only affect a **non-admin/least-privilege**
   user — of which this station has none. So nav/px are not "exposed" today; the dangling ref is a latent
   defect that would bite only if a restricted user were added.

→ New child gap **SC4-G1** (requires-execution): confirm the runtime access outcome (open vs denied) of an
ORD mapped to an undefined category index — needs a live station probe with a non-admin user, or a read of the
category-resolution code path (`BCategoryService.getCategory`/mask resolution). Not closable from this config.

## 688.4 — Authentication: modern + legacy digest, no policy overrides, no MFA

[CERT-hw] AuthenticationService (L154) has two schemes:
- `DigestScheme` = `b:DigestAuthenticationScheme` (L157) — modern N4 SCRAM/digest.
- `AXDigestScheme` = `b:LegacyDigestAuthenticationScheme` (L162) — **legacy AX (N3-compat) still active**.

Both carry an EMPTY `GlobalPasswordConfiguration` (L159/L164) → no deployed password policy overrides (length/
expiration/history/lockout all at framework default). `ssoConfiguration` present, `autoAttemptSingleSignOn`
null → SSO off (L167). No LDAP/Kerberos/SAML scheme. `FIPSEnabled="false"` at root (B685 §685.1).

## 688.5 — SecurityService + deployed-RBAC verdict

[CERT-hw] SecurityService (`nss:SecurityService`, L40): one certificate alias `default` + a
`CertificateExpiryPoint`; no extra certs, no cipher/truststore overrides at this node.

**Verdict — DEFAULT/MINIMAL RBAC, but scoped honestly.** The station runs with: one super-user account, no
second account, no password-policy overrides, no lockout/expiration, no MFA/SSO, FIPS off, and the legacy AX
digest scheme still enabled. This mirrors the weak-posture pattern the `security-audit` focus found on the
supervisor ([Block 398]) — here confirmed on the field controller from its own disk. It is consistent with the
template-seed profile of SC1–SC3 (B685–B687): factory/template defaults, no site hardening beyond the
transport TLS floor. The dangling category-3 reference (688.3) is a real latent defect but has **zero current
access impact** given the single-admin deployment — flagged, not escalated.

**Operator note:** the standing rotate-`admin` recommendation ([Block 468]) applies; on the SD the PBKDF2 hash
is offline-attackable (B685 §685.5). Consider disabling `AXDigestScheme` if no N3 client needs it.

## Connections

- RBAC framework (encoders PBKDF2-10k, BCategoryService super-user gate, RoleHierarchies) → focus
  `access-control` [Block 558]/[Block 561]/[Block 565]. Live weak-posture on the supervisor → focus
  `security-audit` [Block 398]. Rotate-admin + data-at-rest hash → [Block 468] / [Block 685] §685.5. Skeleton
  (FIPS off, AX scheme present) → [Block 685] (this focus).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | one real account `admin`, roles=admin, PBKDF2-10k (masked) | [CERT-hw] | L109/L111/L117 | grep-confirmed |
| 2 | one role = b:AdminRole (super-user), RoleHierarchies empty | [CERT-hw] | L52/L54 | grep-confirmed |
| 3 | 2 categories User(1)/Admin(2); ordMap file:^nav/px → undefined index 3 | [CERT-hw] | L26/L28/L32 | grep-confirmed |
| 4 | dangling cat-3 → open vs denied NOT settled by corpus; nil current impact (admin bypasses) | [INFER] | B561 [CERT] scope + single-admin | reasoned (REFINE) |
| 5 | DigestScheme + legacy AXDigestScheme active; empty GlobalPasswordConfiguration; SSO off | [CERT-hw] | L157/L162/L159/L167 | grep-confirmed |
| 6 | SecurityService: 1 cert alias default; FIPS off | [CERT-hw] | L40/L2 | grep-confirmed |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE** (security posture read). The one
[INFER] is the DE-ESCALATION of the sweep's over-broad "open access" claim (§11). 6/6 load-bearing citations
grep-confirmed; password value masked; evidence-file secret-scan clean.

## Open gaps (this focus)

SC4 CLOSED. Uncovered: **SC4-G1** (requires-execution — runtime behavior of a dangling category index).
Next investigable: **SC5** (AlarmService + AuditHistoryService + HistoryService + LoggingService deployed
config; the histories actually collected on this JACE).
