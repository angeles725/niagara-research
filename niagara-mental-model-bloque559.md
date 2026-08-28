# Block 559 — `BUserPrototypes`: the built-in user-templating subsystem for auto-provisioning (LDAP/SAML) — per-property `overridable` locks + a merge policy whose defaults are security-conservative for everything EXCEPT roles (which UNIONS)

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC2 — the `BUserPrototypes` subsystem that [Block 494] USES for SAML/LDAP user
provisioning but never documents)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 8 prototype classes + original Tridium javadoc (`docSource`).
Property tables and merge-mode ordinals token-verified inline.
**Primary sources** `[CERT]`:
- `organized/docSource/docSource-doc/extracted/baja/javax/baja/user/BUserPrototype.java` (javadoc, `@since 4.4`).
- `organized/baja/baja/vineflower/javax/baja/user/{BUserPrototype,BUserPrototypes,BUserPrototypeProperty,
  BUserPrototypeMergePolicy,BRolesMergeMode,BExpirationMergeMode,BAutoLogoffSettingsMergeMode,
  BAllowConcurrentSessionsMergeMode}.java`.

**Scope**: [Block 494] documented the OEM auth-scheme *implementations* (SAML/LDAP) and mentioned that they
auto-provision users, but the templating MECHANISM they call — `BUserPrototypes` — was never opened. This block
documents the prototype model, the per-property override lock, and the multi-prototype merge policy. It does NOT
re-derive the auth schemes ([Block 494]) or the RBAC concept model ([Block 11]) — REMITTANCE.

---

## 559.1 `BUserPrototype` — a template mirroring BUser property-for-property [CERT]

`public class BUserPrototype extends BComponent implements IPropertyValidator` `[CERT] :72`, `@since Niagara 4.4`
(Patrick Sager, 2016) `[CERT-doc]`. Per the javadoc: *"acts as a template to create a `BUser` from a set of
default values. Each property on `BUser` … has a matching property on `BUserPrototype` of type
`BUserPrototypeProperty`."* The 11 mirrored properties `[CERT] :113-193`: `fullName`, `enabled`, `expiration`,
`language`, `email`, `facets`, `navFile`, `cellPhoneNumber`, **`roles`**, `allowConcurrentSessions`,
`autoLogoffSettings`. (Credential/password properties are deliberately NOT templated.)

## 559.2 `BUserPrototypeProperty` — value + the `overridable` lock [CERT]

`public class BUserPrototypeProperty extends BComponent implements IPropertyValidator` `[CERT] :22`. Two things:
- `getValue()` / `setValue(BValue)` `[CERT] :51-63` — the templated value.
- `overridable` boolean property, **default `false`** `[CERT] :17-23,26`.

Per the javadoc: `overridable` *"determines whether or not the `BUser` property can be modified after being set
by the `BUserPrototype`."* So the DEFAULT posture is **locked**: a value pushed from the prototype cannot be
changed on the provisioned user unless the admin explicitly marks that property `overridable=true`. This is the
mechanism by which an SSO-provisioned user's roles (or expiration) can be made immutable at the station.

## 559.3 `BUserPrototypes` — the container, with default + alternate prototypes [CERT]

`public class BUserPrototypes extends BComponent implements IPropertyValidator` `[CERT] :50`. Properties
`[CERT] :30-51`:
- `defaultPrototype` (typed `BUser`, get returns `BUser` `:64`) — the base template applied to a new user.
- `alternateDefaultPrototype` — a second template (selected by the provisioning scheme, e.g. a fallback group).
- `userEvent` topic + an inner `PrototypeSubscriber extends Subscriber` `[CERT] :218` — reacts to changes so
  edits to a prototype can propagate.

A companion interface `IHasPrototypeMergePolicy` `[CERT]` (present in bajadoc) is the SPI hook a provisioning
scheme implements to supply its `BUserPrototypeMergePolicy` — this is the seam [Block 494]'s SAML/LDAP schemes
plug into.

## 559.4 `BUserPrototypeMergePolicy` — how MULTIPLE prototypes combine [CERT]

When a user maps to more than one prototype (e.g. an SSO user in two directory groups, each mapped to a
prototype), the policy resolves each conflicting field. `public final class BUserPrototypeMergePolicy extends
BComponent` `[CERT] :42`, `enabled` default `false` `[CERT] :43`. Four merge-mode properties, each a **binary
enum** `{merge-strategy, useFirst}` `[CERT] :26-47`:

| Field | Merge mode type | DEFAULT | Alternative | Direction of default |
|-------|-----------------|---------|-------------|----------------------|
| roles | `BRolesMergeMode` | **`union`** (0) | `useFirst` (1) | **PERMISSION-EXPANDING** |
| expiration | `BExpirationMergeMode` | `preferEarliest` (0) | `useFirst` (1) | restrictive |
| allowConcurrentSessions | `BAllowConcurrentSessionsMergeMode` | `preferFalse` (0) | `useFirst` (1) | restrictive |
| autoLogoffSettings | `BAutoLogoffSettingsMergeMode` | `preferShortest` (0) | `useFirst` (1) | restrictive |

Ordinals token-verified `[CERT]` (`BRolesMergeMode.java:17-21` etc.: `UNION=0`/`USE_FIRST=1`, `DEFAULT=union`;
same binary shape for the other three).

## 559.5 The asymmetry [CERT-synthesis]

The merge defaults are **security-conservative for three of four fields**: earliest expiration wins, shortest
auto-logoff wins, concurrent-sessions-false wins — when two prototypes disagree, the MORE RESTRICTIVE value is
chosen. But **`roles` defaults to `union`** — the two prototypes' role sets are COMBINED, which is
permission-EXPANDING. So an SSO user who lands in two groups gets the INTERSECTION of session restrictions but
the UNION of authority. This is a deliberate, defensible model (a user needs all roles their groups grant), but
it is the one field where "more prototypes" means "more access", and it is worth calling out in a fleet where
group→prototype mappings are maintained by different admins. To make role assignment first-wins instead, set
`rolesMergeMode=useFirst`; to lock a provisioned user's roles entirely, leave the prototype's `roles` property
`overridable=false` (the default, §559.2).

## 559.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BUserPrototype extends BComponent + IPropertyValidator; @since 4.4; 11 mirrored BUser properties | [CERT]/[CERT-doc] | BUserPrototype.java:72,113-193; docSource:31-57 | token-checked ✓ |
| 2 | BUserPrototypeProperty = value + overridable (default false); locks the BUser property post-set | [CERT] | BUserPrototypeProperty.java:17-63 | token-checked ✓ |
| 3 | BUserPrototypes container: defaultPrototype(BUser) + alternateDefaultPrototype + PrototypeSubscriber | [CERT] | BUserPrototypes.java:30-68,218 | token-checked ✓ |
| 4 | IHasPrototypeMergePolicy is the SPI seam a provisioning scheme implements | [CERT] | bajadoc IHasPrototypeMergePolicy | logic-checked |
| 5 | MergePolicy: 4 binary merge modes; roles=union, expiration=preferEarliest, concurrent=preferFalse, logoff=preferShortest; enabled default false | [CERT] | BUserPrototypeMergePolicy.java:26-47 | token-checked ✓ |
| 6 | Merge-mode ordinals UNION=0/USE_FIRST=1, DEFAULT=merge-strategy (all four enums) | [CERT] | B*MergeMode.java:12-21 | token-checked ✓ |
| 7 | Asymmetry: 3 fields restrictive-by-default, roles union = permission-expanding | [CERT-synthesis] | rows 5-6 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-doc] ×1 (row 1 shared) · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE =
EVIDENCE (decompilation). 5 of 7 rows token-verified inline.

## Connections

- **[Block 494]** — the SAML/LDAP auth-scheme implementations that CONSUME this templating via
  `IHasPrototypeMergePolicy`; AC2 documents the mechanism they never opened.
- **[Block 11]** — the RBAC concept model: prototypes assign the `roles` this model governs.
- **[Block 558]** (AC1) — sibling: prototypes do NOT template password/credentials (those go through
  `BPasswordStrength`/`BPasswordAuthenticator`).
- **[Block 510]** — the auth-scheme SPI; a scheme both authenticates (B510) and provisions (here).

## Open gaps (this block)

- The exact SAML/LDAP call-site that selects default-vs-alternate prototype and invokes the merge lives in the
  auth-scheme module ([Block 494] territory) — named, not re-opened here. Focus continues at AC3
  (`BCategoryService` runtime).
