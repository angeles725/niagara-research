# B708 — Module best practices, cross-cutting (MBP4): the permission model, the audit pattern, error-handling on the engine thread, and signing — the concerns that span rt/ux/wb

> Focus: **module-best-practices** · Gap **MBP4** (cross-cutting: RBAC/permissions, audit, error handling,
> signing). Block TYPE = **DESIGN/SYNTHESIS** (consolidates threads from MBP1-3 + the access-control /
> security-audit / signing-pki focuses; high [INFER] ratio expected). Feeds `docs/module-best-practices.md` §4.
> Marker `[CERT]` where re-citing verified code; `[INFER]` for guidance.

## 708.1 — The permission model (how authorization actually works)

[CERT] Two layers, both server-authoritative:
- **Permission bits** — check `BPermissions.has(OPERATOR_WRITE)` (or ADMIN_WRITE / OPERATOR_INVOKE), the BIT,
  never a role-NAME string. This is the correct write-gate ([Block 648] `ChiRbacHelper`; the inverse anti-pattern
  is mcpbridge's userless dispatch, [Block 643]).
- **Categories** — `BCategoryService` partitions the component space; a user's `BCategoryMask` gates what they
  see/touch. It is ORD-PREFIX inheritance over a fixed **256-slot** mask, recomputed by a **60 s** periodic
  daemon (not a live tree walk) ([Block 561], access-control). A module does not implement this — it RELIES on
  it: resolve the acting `BUser`, let `OrdTarget.canWrite()` / `hasOperatorRead()` apply.

**Rule:** every mutation runs as the authenticated `BUser` (via `runAsUser` when off the request thread), so the
category+permission model applies uniformly. Client-side gating (`@AgentOn requiredPermissions`, hidden buttons)
is convenience only (ADR D6).

## 708.2 — The audit pattern

[CERT+INFER] Two complementary trails:
- **Framework audit** — `AuditHistoryService` records every slot change with schema
  `timestamp, operation, target, slotName, oldValue, value, userName` (confirmed on-disk, [Block 689]/[Block 699]).
  It is automatic for component mutations; a module gets it for free when it mutates through the normal slot API
  as the real user.
- **Module audit** — for servlet/REST writes that don't go through a slot set, write your OWN audit record per
  mutation `{ts, user, action, ord, oldValue, newValue}` (chihuahua's audit helper, [Block 648] audit-2026-05-06).
- **Weakness to know:** the audit trail is **local-only, cleartext, and not tamper-evident** ([Block 689]/
  [Block 566]/[Block 698]) — do not treat it as a security control against someone with disk/SD access; it is an
  operational record, not an integrity guarantee.

## 708.3 — Error handling

[CERT+INFER]
- **Engine thread never throws.** `changed()`/`started()`/`stopped()` wrap the body in `catch(Throwable){ log; }`
  and swallow — an uncaught exception can destabilize the station ([Block 650], ADR-D7). This is the single most
  important cross-layer rule.
- **Security decisions fail CLOSED** — any exception in an authorization check → deny ([Block 648]).
- **Don't `System.exit`** from module code (the framework uses it for signing failures — `System.exit(-6)`,
  [Block 392] — but a module must not).
- **Log at the right level** through the module's logger; a `SEVERE`/`Exception` should be actionable. LogHistory
  captures these ([Block 701]).

## 708.4 — Signing + module security

[CERT+INFER]
- **Sign your modules.** Modules are RSA-2048 signed; on an OEM unit even Tridium core is re-signed by the
  vendor PKI ([Block 392]). The station's `moduleVerificationMode` decides how strictly signatures are checked;
  a production station should not run `moduleVerificationMode=low` + `program.requireSigning=off`
  ([Block 398] found the live supervisor did — a hardening gap, not a module gap, but modules should be signed
  so strict mode is possible).
- **Declare `<permissions>` only if you use them.** The base grant (read/write of `niagara.<module>.*` + module
  keyring) covers a normal component/service/dashboard module; the empty New-Module-Wizard scaffold in 12/13
  shop modules should be deleted ([Block 635]/[Block 649]).

## 708.5 — Cross-cutting checklist

[INFER] For any module, across all layers:
1. Every write path gated by `BPermissions.has(…)`, fail-closed, running as the real `BUser`.
2. Every mutation audited (framework slot-audit + module audit for servlet writes).
3. No throw on the engine thread; heavy work off-thread.
4. Signed; no empty `<permissions>` scaffold; permission scoping at the `@AgentOn` level.
5. Never trust the browser for authorization; re-check server-side.

## Connections

- Permission model → focus `access-control` [Block 558]–[Block 566] (esp. [Block 561]). RBAC write-gate →
  [Block 648]; bypass anti-pattern → [Block 643]. Audit trails → [Block 689]/[Block 699]/[Block 566].
  Engine-thread errors → [Block 650]. Signing → [Block 392]; live hardening gaps → [Block 398]. Permissions
  scaffold → [Block 635]/[Block 649]. Layer siblings → [Block 705]/[Block 706]/[Block 707]. Deliverable:
  `docs/module-best-practices.md` §4.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | permission = BPermissions bit + BCategoryService 256-mask/60s | [CERT] | [Block 648]/[Block 561] | cited |
| 2 | audit = framework slot-audit + module servlet-audit; local-only not tamper-evident | [CERT] | [Block 689]/[Block 648]/[Block 566] | cited |
| 3 | engine thread never throws; security fails closed | [CERT] | [Block 650]/[Block 648] | cited |
| 4 | sign modules; delete empty permissions scaffold | [CERT] | [Block 392]/[Block 635] | cited |
| 5 | cross-cutting checklist | [INFER] | 708.5 | reasoned |

**Tally:** [CERT] ×4 · [INFER] ×1. Block TYPE = **DESIGN/SYNTHESIS** — ratio healthy. Re-cites verified blocks.

## Open gaps (this focus)

MBP4 CLOSED. Next: **MBP5** (build/packaging — module.xml/module-include.xml, dependencies, signing, version-
targeting, the optimal error-free build loop). Then MBP6 (exemplar catalog + guide finalization).
