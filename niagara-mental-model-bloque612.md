# Block 612 — graphql-admin (GQL-G3): `OrdTarget.canRead()/canWrite()/canInvoke()` — the per-field RBAC primitive a resolver calls, and its fail-open default

> **What**: The per-node authorization primitive a GraphQL resolver uses to enforce Niagara RBAC on EVERY
> field it returns or mutates: `OrdTarget.canRead()/canWrite()/canInvoke()`. B611 established that a
> resolver runs as the session user by threading the request `Context` (`op`); this block shows what that
> `Context`, once resolved to an `OrdTarget`, actually evaluates — and the one dangerous default (no
> protected ancestor ⇒ **fail-open**).
> **Scope**: `javax.baja.naming.OrdTarget` (the resolve-result that IS a `Context`) and the
> `javax.baja.security.BIProtected` contract it delegates to. The concrete category/permission-bit
> evaluation inside `BIProtected` is REMITTANCE to [B11] (bits/category model) and [B30] (slot-level
> `BIProtected` enforcement). The servlet `Context` that seeds the `OrdTarget` is [B611] (GQL-G1).
> **Block type**: EVIDENCE (code seam) + a short DESIGN mapping (the `[INFER]` rows).
> **Subject version**: Niagara N4.14.0.162. `OrdTarget`/`BIProtected` are core `baja` (`@since Baja 1.0`).
> **Sources**:
> - `organized/baja/baja/vineflower/javax/baja/naming/OrdTarget.java` (decompiled; structure + method bodies intact, no scrubbed literals in the cited region)
> - `organized/docSource/docSource-doc/extracted/baja/javax/baja/security/BIProtected.java` (Tridium original source, real javadoc)
> **Method**: docSource-first for the `BIProtected` contract; vineflower for the `OrdTarget` bodies (verified
> readable, non-scrubbed). Markers: `[CERT]` = verbatim `file:line`; `[INFER]` = design deduction.

---

## 612.1 — `OrdTarget` IS a `Context` carrying the resolving user `[CERT]`

`public class OrdTarget implements Context` with fields `BUser user` and `BPermissions permissions`
`[CERT]` (`OrdTarget.java:18,19,26`). Crucially, an `OrdTarget` **inherits its user from the `Context` that
resolved the ORD**: the package constructor `OrdTarget(Context cx, BOrd ord, OrdQuery[] queries, BObject
object)` does `this.user = new BasicContext(cx).getUser()` `[CERT]` (`:93-95`), and `getUser()` returns that
field `[CERT]` (`:124-126`).

This is the direct continuation of [B611]: when a resolver calls `someOrd.resolve(op)` with the `WebOp op`
(the session `Context` from GQL-G1), the returned `OrdTarget` carries the **session user**. Every
authorization decision the `OrdTarget` then makes is evaluated for that user. Resolve with the wrong context
(or none) and the `OrdTarget` carries the wrong user — the silent-escalation failure mode named in
[B611] §611.3.

## 612.2 — `canRead/canWrite/canInvoke` delegate to the nearest protected ancestor `[CERT]`

The three gates are thin delegators `[CERT]` (`OrdTarget.java:281-294`):

```java
public boolean canRead()   { BIProtected st = getSecurityTarget(); return st != null ? st.canRead(this)   : true; }
public boolean canWrite()  { BIProtected st = getSecurityTarget(); return st != null ? st.canWrite(this)  : true; }
public boolean canInvoke() { BIProtected st = getSecurityTarget(); return st != null ? st.canInvoke(this) : true; }
```

`getSecurityTarget()` finds the securable object by walking `object → container → base` (the parent chain)
for the nearest `BIProtected`, returning `null` if none exists `[CERT]` (`:258-266`). Note the argument
passed to `st.canRead(...)` is `this` — the `OrdTarget` itself, so the check sees the resolving user.

The `BIProtected` contract (docSource javadoc) fixes what those calls evaluate:
- `BPermissions getPermissions(Context cx)` — javadoc: returns `cx.getUser().getPermissionsFor(this)`; an
  unprotectable/unmounted target should return `BPermissions.all` `[CERT]` (`BIProtected.java:44,46,53`).
- `boolean canRead(OrdTarget cx)` / `canWrite(OrdTarget cx)` / `canInvoke(OrdTarget cx)` `[CERT]`
  (`BIProtected.java:60,67,74`).

So `OrdTarget.canRead()` ⇒ `securityTarget.canRead(this)` ⇒ evaluates `user.getPermissionsFor(target)` — the
session user's permission BITS for the target's security CATEGORY. The bit-set (admin / admin-write /
operator / operator-write / read-only) and the category-mask evaluation are REMITTANCE to [B11] and [B30];
`OrdTarget` is the per-ORD ENTRY POINT into that model. `getPermissionsForTarget()` exposes the raw
`BPermissions` (`target.getPermissions(this)`, else `BPermissions.all`) `[CERT]` (`:268-279`).

## 612.3 — The fail-open default: no `BIProtected` ancestor ⇒ `true` `[CERT]` — the load-bearing caveat

When `getSecurityTarget()` returns `null` (the resolved object has NO `BIProtected` in its `object →
container → base` chain), all three gates return **`true`** `[CERT]` (`:283,288,293` — the `: true` branch),
and `getPermissionsForTarget()` returns `BPermissions.all` `[CERT]` (`:274`).

This is **fail-open by design**: authorization is a property of the securable target, not of the caller. For
a mounted `BComponent` this is safe — the component (or a category-bearing ancestor) is `BIProtected`, so
the check is real. But it means:
- **Non-component data reached by a resolver is unguarded.** If a GraphQL field returns a `BObject` that is
  not `BIProtected` and has no protected ancestor (a bare value, a computed structure, an unmounted object),
  `canRead()` is `true` for ANY authenticated user. The servlet-wide `hasOperatorRead()` floor ([B611]
  §611.2) is then the ONLY gate.
- **`BPermissions.all` on an unprotected target is a footgun.** A resolver that reads
  `getPermissionsForTarget()` to decide field visibility gets "all permissions" for an unmounted target and
  may wrongly expose an admin-only field.

## 612.4 — Design map (GQL-G3) `[INFER]`

A correct GraphQL admin resolver enforces authorization PER FIELD, not once per request:
1. For each node/field, resolve its ORD as the session user: `OrdTarget t = ord.resolve(op)` (op = GQL-G1).
2. Before RETURNING a value: `if (!t.canRead()) → omit/null the field (or error)`.
3. Before a MUTATION: `if (!t.canWrite()) → reject`; before invoking an action: `if (!t.canInvoke()) → reject`.
4. **Do not trust a `true` from an unprotected target as "authorized".** For any field whose backing object
   is not a `BIProtected` component, apply an explicit resolver-level allowlist — never rely on `canRead()`
   alone, because §612.3 makes it fail-open. This is the GraphQL-specific hardening the framework does not
   give you for free.

This is exactly the gate WebChart's rt uses ("three query routes gated by `OrdTarget.canRead()` running as
the session user", [B374]) — B612 generalizes it into the per-field resolver rule and surfaces the fail-open
caveat B374 did not need to state.

## 612.5 — Connections

- **[B611] (GQL-G1)** — supplies the session `Context` (`op`) that seeds the `OrdTarget`; §612.1 closes the
  loop (the OrdTarget copies its user from that context at `:95`). Together G1+G3 are the auth identity
  (who) + the per-field authz (what) of a GraphQL admin layer.
- **[B11]** — the `BUser`/`BRole`/`BCategory`/`BPermissions` 6-bit model that `user.getPermissionsFor(target)`
  evaluates. B612 is the ORD-level entry into it.
- **[B30]** — slot-level `BIProtected` enforcement, `AccessSlotCursor`, silent-deny vs `PermissionException`.
  B612's `canRead/canWrite` are the ORD-target face of the same `BIProtected` contract B30 documents at the
  slot cursor.
- **[B561]** — `BCategoryService` runtime (ORD-prefix inheritance, cap 256) — where a target's category (and
  thus its `getPermissions`) actually comes from.
- **[B374]** — WebChart rt's `OrdTarget.canRead()` query gate: the real precedent B612 generalizes.
- Forward: **GQL-G4** (the concrete BQL/`BComponent` call-site that resolves the ORD and calls these gates).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `OrdTarget implements Context`; holds `BUser user`, `BPermissions permissions` | `[CERT]` | OrdTarget.java:18,19,26 | ✓ token |
| 2 | `OrdTarget` copies its user from the resolving `Context`: `this.user = new BasicContext(cx).getUser()` | `[CERT]` | OrdTarget.java:93-95 | ✓ token |
| 3 | `getUser()` returns that field | `[CERT]` | OrdTarget.java:124-126 | ✓ token |
| 4 | `canRead/canWrite/canInvoke` → `getSecurityTarget().canX(this)` else `true` | `[CERT]` | OrdTarget.java:281-294 | ✓ token |
| 5 | `getSecurityTarget()` walks object→container→base for nearest `BIProtected`, else null | `[CERT]` | OrdTarget.java:258-266 | ✓ token |
| 6 | `getPermissionsForTarget()` → `target.getPermissions(this)` else `BPermissions.all` | `[CERT]` | OrdTarget.java:268-279 | ✓ token |
| 7 | `BIProtected.getPermissions(cx)` = `cx.getUser().getPermissionsFor(this)` (javadoc) | `[CERT]` | BIProtected.java:44,53 | ✓ token |
| 8 | `BIProtected.canRead/canWrite/canInvoke(OrdTarget)` are the contract methods | `[CERT]` | BIProtected.java:60,67,74 | ✓ token |
| 9 | No protected ancestor ⇒ gates return `true` / permissions = all (fail-open) | `[CERT]` | OrdTarget.java:274,283,288,293 | ✓ token |
| 10 | Resolver must call canRead/canWrite/canInvoke per field, resolving ORD with `op` | `[INFER]` | design map from #1-#9 + [B611] | ✓ reasoned |
| 11 | Unprotected-target `true` must NOT be trusted as authorized (resolver allowlist) | `[INFER]` | fail-open deduction from #9 | ✓ reasoned |

**Tally**: `[CERT]` = 9 · `[INFER]` = 2 · others = 0. **[INFER]/[CERT] ratio** ≈ 0.22 — LOW; block type =
EVIDENCE, the two `[INFER]` rows are the design guidance. G3 closed.
**Tokens checked**: all 9 `[CERT]` read-confirmed against the cited lines (OrdTarget bodies at :93-95,
:258-294; BIProtected docSource javadoc at :44/:53/:60/:67/:74). No scrubbed literals in the cited regions.
