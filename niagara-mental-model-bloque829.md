# B829 · One audit trail for PANCCADIA — what Niagara's config-audit records for each write path, settled by CODE: a slot `set()` fires an `AuditEvent` ONLY inside `if (context != null && context.getUser() != null)`, so the DashboardPan servlet's NULL-Context set is NOT audited (suppressed, not just unattributed); an oBIX PUT IS audited to the oBIX login user; both need an installed `AuditHistoryService` — so the write-server's Supabase `audit` (real operator email) is the single source of truth `[CERT]`

> **Scope**: the operator's question "who set Cuarto3 to X at HH:MM" needs ONE answer, but a setpoint can be written four
> ways (Workbench fox, the DashboardPan servlet, an oBIX PUT, the write-server) into THREE different trails (Niagara
> AuditHistory, the module `auditLog`, the write-server Supabase). This block settles by CODE what each path actually
> records — resolving the [S12] plan's `[INFER]` on the null-Context servlet set — and gives a unified schema + query.
> REMITTANCE — [B564] (the `Sys.getAuditor()`/`AuditEvent` wiring), [B804] (history authoring), [B816] (the set/threading
> path), [B823]/[S12] (the write channels + the servlet null-Context set).
>
> **Sources**: FUENTE 3 (`[CERT]`, decompiled, crux cites confirmed at the enclosing method) —
> `com/tridium/sys/schema/{ComplexSlotMap,ComponentSlotMap}.java`, `javax/baja/security/AuditEvent.java`,
> `com/tridium/sys/Nre.java`, `javax/baja/sys/Sys.java`, `history-rt/com/tridium/history/audit/{BAuditHistoryService,BAuditRecord,BAbstractAuditHistorySource}.java`,
> `obixDriver-rt/com/tridium/obix/util/ObixUtils.java`, `fox-rt/com/tridium/fox/session/Tuner.java`, `javax/baja/naming/OrdTarget.java`.
> FUENTE 1 — [B564] (audit wiring, REMITTANCE), [S12] plan (`sources/probes/2026-09-06-c9-s12-config-login-audit-plan.md`),
> our `DashboardPan-rt/BDashboardService.java` (`auditLog`).

## 829.1 — The config-audit fire path, and its NULL-Context gate `[CERT]`
A slot value write goes `BComponent.set() → ComplexSlotMap.set(prop, old, …, new, …, context, …)`. The audit is built
ONLY inside a Context+user gate (`ComplexSlotMap.java:662`):
```
if (context != null && context.getUser() != null) {          // :662 — THE GATE
   user = context.getUser();
   base = getComponentBase(prop, context);
   if (base != null){ user.checkWrite(…); auditOldValueStr = toAuditString(base, oldValue, context); }  // :671
}
…
if (auditOldValueStr != null)                                 // :813
   this.audit(base, user, "Changed", auditOldValueStr, toAuditString(base, newValue, context));  // :814
```
→ dispatcher `audit(…)` → `ComplexSlotMap.java:1684-1690`:
```
Auditor auditor = Nre.auditor;
if (auditor != null && targetPath != null)                   // :1685 — needs an installed auditor
   auditor.audit(new AuditEvent(op, target, slotName, oldValue, value, user.getUsername()));  // :1687
```
**Two gates**: (1) the Context must be non-null AND carry a user (`:662`) — else `auditOldValueStr` stays null and the
audit call is never reached (`:813`); (2) `Nre.auditor` must be non-null (`:1685`) — set ONLY by
`BAuditHistoryService.auditStarted() → Sys.setAuditor(this)` (`Sys.java:178`; `Nre.auditor` defaults null). Also the space
must be `@AuditableSpace` — `BComponentSpace` is (`:77`), so the station space qualifies. `[CERT]`

## 829.2 — The write-path audit matrix `[CERT]`
| Write path | Context passed to `set()` | Niagara AuditHistory `AuditEvent`? | Attributed to |
|---|---|---|---|
| **Workbench fox set** | server-side `BasicContext(user)` — `Tuner.authenticateSuccess:796-797` sets it | **YES** (if service installed) | the fox login user (`getUsername()`) |
| **oBIX PUT** (channel 1, [B825]/[B826]) | `ot.getUser()` — `ObixUtils.serviceWrite:558` `parent.set(pary[idx], val, ot.getUser())` | **YES** (if service installed) | the oBIX login user |
| **DashboardPan servlet** (channel 3, [B823] §823.4) | **`null`** — `parent.set(prop, toSet, null)` | **NO — suppressed at `:662`** | — (invisible to Niagara audit) |
| module `auditLog` (BDashboardService) | n/a — the servlet appends its own JSON-lines | n/a (a module ring, not AuditHistory) | `req.getRemoteUser()` (SlotPath-escaped) |
| write-server (S12) | n/a — its own Supabase `audit` row | n/a | the Supabase config-session **email** |

**The two attribution facts that decide the design**: (a) the servlet write leaves NO Niagara audit record at all (the
[S12] `[INFER]` is now `[CERT]` — suppressed, not merely unattributed); (b) the fox/oBIX writes ARE audited but to the
STATION/oBIX login user, which — for the write-server — is the ONE shared write user, not the real operator. So **no
Niagara-side trail carries the REAL operator identity for a remote write.** `[CERT]`

## 829.3 — What settles the [S12] [INFER] `[CERT]`
[S12] Part 2 marked "null-Context → no Baja AuditHistory event" as `[INFER-grounded]` (module has no wiring; station-level
unverified). B829 settles it in the FRAMEWORK code: `ComplexSlotMap.set` builds the AuditEvent ONLY under
`context != null && context.getUser() != null` (`:662`) → a null Context suppresses it entirely, independent of any
station AuditHistoryService. **Upgrade [S12] Part 2 to `[CERT]`**: the servlet path's only records are the module
`auditLog` + the write-server Supabase; Niagara logs nothing for it. (And even a non-null-Context servlet set would
attribute to the servlet's session user, not the real operator — the real identity still lives only in Supabase.)

## 829.4 — The three schemas `[CERT]`
- **Niagara `AuditEvent` / `BAuditRecord`** (`history:AuditRecord`, appended to `station:/AuditHistory`): `operation`
  (Changed/Added/Removed/Invoked/…), `target` (component slot-path body), `slotName`, `oldValue`, `value`, `userName`
  (`user.getUsername()`), `timestamp` (`Clock.time()`). `BAuditRecord.fromEvent` copies field-by-field, null→"". `[CERT]`
- **Module `auditLog`** (`BDashboardService`, PERSISTENT ring 500): `{ts, user (SlotPath-escaped remoteUser), action:"setpoint", ord, oldValue, newValue}`. `[CERT — B823 §823.4/BDashboardService]`
- **Write-server Supabase `audit`** (S12): `{ts, email (real operator, config-session), ord, old (GET-before-write), new, result, ip, config_session}`. `[INFER — S12 design]`

## 829.5 — One trail: the unified schema + query `[INFER, grounded]`
No single Niagara trail answers "who set Cuarto3 to X at HH:MM" for a remote write (§829.2). The **write-server Supabase
`audit` is the source of truth for operator identity** (the real `email`), because it is the only trail that (a) is
written for EVERY external write regardless of path and (b) carries the real person, not a shared station user. A unified
view (one row per write) keys on `(ord, ts)` and carries: `who` = Supabase `email`; `what` = `ord` + `old→new`; `when` =
`ts`; `how` = the channel (servlet / oBIX-child / oBIX-wrapped); `result`; and a CROSS-CHECK column = the Niagara
AuditHistory `userName` (present for oBIX/fox, absent for servlet) + the module `auditLog` line. **Discipline**: the
write-server records after a read-back-settle ([B825] — ~1 s control) so `new` is the propagated value, and it must record
EVERY path. The Niagara AuditHistory is the SECONDARY trail that catches OUT-OF-BAND writes (a direct Workbench edit, a
console operator) the write-server never saw — so keep `AuditHistoryService` installed for defence-in-depth, but do not
rely on it for the servlet path. `[INFER]`

## 829.6 — Kit implication `[INFER]`
- **Doctrine** (`METHODOLOGY.md`/`types/logic-authoring.md`): a slot write is audited by Niagara ONLY if the `set()`
  Context carries a user AND an `AuditHistoryService` is installed; a null-Context servlet write is NOT audited — a module
  that writes on behalf of a remote user MUST keep its own audit (a persistent `auditLog` ring, [B823]/BDashboardService)
  and/or pass a real user Context. Cite `[ev: corpus B829]`.
- **For S12**: keep the Supabase `audit` as the authoritative trail; install `AuditHistoryService` for the out-of-band
  cross-check; if a Niagara-native per-operator record is wanted, the oBIX child-value PUT ([B826]) audits to the oBIX
  login user — so a per-operator oBIX login (not one shared user) would put the identity in Niagara too (a future option).

## Self-verify
| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | A slot `set()` builds the audit ONLY under `if (context != null && context.getUser() != null)`; the fire is gated on `auditOldValueStr != null` | `[CERT]` | `ComplexSlotMap.java:662,671,813-814` |
| 2 | Dispatch is a no-op unless `Nre.auditor != null`; `AuditEvent(op,target,slotName,old,value,userName)` | `[CERT]` | `ComplexSlotMap.java:1684-1690`; `AuditEvent.java` |
| 3 | `Nre.auditor` is null by default; set only by `BAuditHistoryService.auditStarted → Sys.setAuditor(this)` | `[CERT]` | `Nre.java`; `Sys.java:178`; `BAuditHistoryService.auditStarted` |
| 4 | Servlet null-Context set (`parent.set(prop,val,null)`) → NOT audited (gate fails) — settles the S12 [INFER] | `[CERT]` | `:662` gate; [B823] §823.4 null Context |
| 5 | oBIX set passes `ot.getUser()` → audited to the oBIX login user | `[CERT]` | `ObixUtils.java:558`; `OrdTarget.java:124`; `BUser` is a Context |
| 6 | Fox/Workbench set is attributed (server-side `BasicContext(user)`) | `[CERT]` | `Tuner.java:796-797` |
| 7 | Persisted `BAuditRecord` schema = operation/target/slotName/oldValue/value/userName/timestamp | `[CERT]` | `BAuditRecord.fromEvent`; `BAbstractAuditRecord` |

**Tally**: 7 `[CERT]`. The load-bearing null-Context gate (which settles the S12 `[INFER]`) was confirmed at the enclosing
method this session. §829.5/§829.6 (the unified view + doctrine) are `[INFER]` grounded in the [CERT] matrix. Dedupe: the
`Sys.getAuditor()`/`AuditEvent` wiring is REMITTANCE ([B564]); this block adds the null-Context gate finding, the
per-write-path audit matrix, the S12 settle, and the unified-trail design.

## Connections
- **[B564]** (the audit-trail wiring — REMITTANCE; this block adds the null-Context gate + the per-path matrix), **[B816]**
  (the `set()`/threading path the audit hooks into), **[B804]** (history authoring — the AuditHistory sink), **[B823]** §823.4 +
  **[S12]** (the servlet null-Context write this settles; the Supabase trail), **[B825]**/**[B826]** (the oBIX write that IS
  attributed; read-back-settle before recording `new`), **[B803]** (step-up — the config-session that supplies the real
  email). Kit: the audit doctrine line + the S12 note. CLIENT: keep BDashboardService.auditLog; the write-server Supabase is
  the source of truth.

## Open gaps
- **B829-G1** (requires-execution): confirm on PANCCADIA whether an `AuditHistoryService` is installed+started (so the
  oBIX/fox writes actually persist to `station:/AuditHistory`), and read one AuditRecord to confirm the userName for an
  oBIX write = the write user. Pairs with [B825]-G1 / the live probe.
- **B829-G2** (bounded): whether passing a real user Context from the servlet (instead of `null`) is feasible — the servlet
  authenticates `req.getRemoteUser()`; resolving it to a `BUser` and passing it as the `set()` Context would make the
  servlet write Niagara-audited (a design change to evaluate vs the [B816] overlap discipline).
