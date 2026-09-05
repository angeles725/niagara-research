# B776 · Action protection — declarative `@NiagaraAction` permission gating + the correct-use vs AP-27 `doPrivileged` line (MAE5, D3)

> **Scope**: how an author PROTECTS an action (operator vs admin) and when `AccessController.doPrivileged` is
> legitimate vs the AP-27 anti-pattern. The positive recipe (the kit could previously only say "don't bypass"). Focus:
> `module-authoring-exemplars` (MAE5 / dimension D3). Kit destination: `types/logic.md`.
>
> **Sources**: FUENTE 3 decompiled `baja` (`BComponent.canInvoke`, `Flags`, `security/BPermissions`),
> `control-rt` (`BEnumWritable`/`BBooleanWritable` action decls), `net-rt` (`BHttpProxyService` doPrivileged),
> `gauth-rt`/`electronicSignature` (secret reads); verified this session at `organized/`. FUENTE 1: B18/B48/B755,
> access-control B558-B566 (REMITTANCE). READ-ONLY. English (post-B115).

---

## 776.1 — The positive gating recipe: declare, don't code `[CERT]`
An author does NOT call a permission API inside the action body. Gating is DECLARATIVE via the action slot's `Flags`,
enforced by the framework at invoke time. `BComponent.canInvoke(OrdTarget)` (`baja/…/sys/BComponent.java:972`):
`if (Flags.isOperator(this, slot)) return permissions.has(4);` (`:981-982`) else `return permissions.has(64);` —
i.e. the caller's `BPermissions` (from `cx.getUser().getPermissionsFor(this)`) must carry `OPERATOR_INVOKE`(4) for an
operator-flagged action, or `ADMIN_INVOKE`(64) otherwise. The author's only job is the flag on the annotation.

## 776.2 — Operator vs admin: `Flags.OPERATOR`(256), and admin is the DEFAULT `[CERT]`
`Flags.OPERATOR = 256` (`Flags.java:31`); `isOperator = (flags & 0x100)!=0`, `isAdmin` = its negation (`:143-149`).
`BPermissions`: `OPERATOR_INVOKE=4`, `ADMIN_INVOKE=64` (`security/BPermissions.java:28,31`). So:
- **Operator-invokable** → `@NiagaraAction(name="…", flags=256)` (requires bit 4).
- **Admin-only** → OMIT the operator flag (requires bit 64) — **admin is the default** for any action without the flag.
Real contrast in ONE component (`control-rt/…/BEnumWritable.java:61`): `emergencyOverride`/`emergencyAuto` carry NO
operator flag → ADMIN-only (highest-privilege config change), while routine `override`/`auto`/`set` are `flags=256`
→ operator-invokable. (Same in `BBooleanWritable`: `set` is `flags=256`; `cancelMinTimer` has no OPERATOR → admin.)

## 776.3 — Server-side enforcement is automatic `[CERT/INFER]`
Remote invokes resolve through `OrdTarget.canInvoke()` → `canInvoke(this)`; the fox/box channel throws
`javax.baja.security.PermissionException` on failure (box `BOrdChannel` even ships `"ci": target.canInvoke()` to the
client so the UI can pre-disable). So do NOT re-implement the check in the action body — the framework + channel
enforce it. (Note: there is no `BComponent.getPermissionsForAction` getter — the mapping is `canInvoke` + `Flags.isOperator`.)

## 776.4 — `doPrivileged`: correct-use is JVM-permission-only; AP-27 wraps RBAC `[CERT]`
Every corpus `doPrivileged` obtains a JVM SecurityManager permission the trusted framework legitimately holds — it
NEVER touches Niagara RBAC. Exemplars (`net-rt/…/BHttpProxyService.java`): `:81` `AccessController.doPrivileged(
ProxySelector::getDefault)`; `:270` `doPrivileged(() -> ((BPassword)getPassword()).getValue())` (read a password
secret); `:299` a `doPrivileged` block that installs `ProxySelector`/`Authenticator.setDefault` + a system property
(needs `NetPermission`/`PropertyPermission`). Same shape in `gauth-rt GoogleAuthLoginModule` (`getSecretKey().getValue()`)
and `electronicSignature`.
- **CORRECT use**: elevate to satisfy a JVM permission (read a `BPassword`/secret, `setDefault` an authenticator, set a
  system property, load a resource) — while the Niagara `Context`/`BPermissions` check STILL governs the operation.
- **AP-27 anti-pattern**: wrapping a NIAGARA RBAC check (`invoke`/`canInvoke`/`getPermissionsFor`/a superUser switch)
  inside `doPrivileged` to sidestep the station user's `BPermissions`. **Find-zero in the corpus** (every `doPrivileged`
  grep'd for `invoke|canInvoke|getPermissionsFor|SuperUser` = only benign reflection/host-ID reads) — AP-27 is a
  described HAZARD, not a Tridium practice (two search terms agree; not proof of absence).

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Action gating is declarative: `canInvoke` = `Flags.isOperator ? has(OPERATOR_INVOKE) : has(ADMIN_INVOKE)`; no API call in the body | [CERT] | BComponent.java:972,981-982 |
| 2 | `Flags.OPERATOR=256`; operator flag set → operator-invoke, omitted → admin-invoke (admin is the default) | [CERT] | Flags.java:31,143-149 |
| 3 | `OPERATOR_INVOKE=4`, `ADMIN_INVOKE=64` | [CERT] | BPermissions.java:28,31 |
| 4 | Real contrast: BEnumWritable emergencyOverride/emergencyAuto admin-only (no flag) vs override/auto/set `flags=256` | [CERT] | BEnumWritable.java:61 |
| 5 | Correct `doPrivileged` = JVM permission (read BPassword, setDefault, system property); net-rt exemplars | [CERT] | BHttpProxyService.java:81,270,299 |
| 6 | AP-27 = wrapping a Niagara RBAC check in `doPrivileged`; find-zero in corpus (hazard, not practice) | [CERT/INFER] | grep doPrivileged×RBAC terms = 0; [CERT] on the benign uses |

**Tally**: 5 [CERT], 1 [CERT/INFER]. No unmarked claims. Spine grep-verified inline this session at `organized/`.

## Connections
- **B755** (BPermissions bit values — OPERATOR_INVOKE=4/ADMIN_INVOKE=64 confirmed), **B18** (signing/security),
  **B48** (frontend RBAC), **access-control B558-B566** (the RBAC subsystem). **B763** (our -ux write-surface gates on
  OPERATOR_WRITE the same declarative way — this is the rt/action analog). **B778** (a service's actions gate the same way).

## Open gaps
- **MAE5-G1** — the `requiredPermissions` string form on `@AgentOn`/views (B780 §MAE9) vs the `Flags.OPERATOR` action
  form is a related-but-distinct surface (view-visibility vs action-invoke); the reconciliation is a bounded follow-up.

## Kit implication (→ `types/logic.md`)
Add an "action protection" recipe: gate an action DECLARATIVELY — `@NiagaraAction(name="…", flags=Flags.OPERATOR)`
(256 = operator-invoke; OMIT the flag = admin-invoke, which is the DEFAULT), enforced by `BComponent.canInvoke` +
the fox/box `PermissionException`; do NOT re-check in the body. Reserve the operator flag for low-privilege writes;
leave config/emergency/security actions admin-only (the `BEnumWritable.emergencyOverride` pattern). Use
`AccessController.doPrivileged` ONLY to obtain a JVM permission (read a `BPassword` secret, `setDefault` an
authenticator, set a system property) — NEVER wrap a Niagara permission/invoke/user check in it (AP-27 = privilege
escalation past station RBAC).
