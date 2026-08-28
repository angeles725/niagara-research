# Block 564 — The audit-trail wiring: two parallel channels (config `AuditEvent` with a full old→new diff, security `SecurityAuditEvent` login-centric) through a PLUGGABLE `Sys.getAuditor()` singleton, landing in history-rt — rich content, but presence-gated and (per B393) tamper-evident-free

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC6 — the audit wiring [Block 30] named as bare interfaces; who calls
`audit()`, what the events carry, and where they land)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the two interfaces + two event classes + the `Sys` accessors + the
`BAuthenticationService` call site; sink located by sweep.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/security/{Auditor,SecurityAuditor,AuditEvent,SecurityAuditEvent}.java`.
- `organized/baja/baja/vineflower/javax/baja/sys/Sys.java:174-183`.
- `organized/baja/baja/vineflower/com/tridium/authn/BAuthenticationService.java:338-356`.
- Sink: `organized/history/history-rt/vineflower/com/tridium/history/audit/{BAuditHistoryService,
  BSecurityAuditHistorySource,BAbstractAuditHistorySource}.java` (existence-confirmed).

**Scope**: the end-to-end audit path. [Block 30] declared the `Auditor`/`SecurityAuditor` interfaces exist;
this traces who fires events, their fields, the pluggable install point, and the sink. Does NOT re-open history
storage internals ([Block 8]) or the tamper-evidence verdict ([Block 393]/[Block 351]) — connects.

---

## 564.1 Two channels, two event shapes [CERT]

N4 has **two parallel audit interfaces** `[CERT]`, each a one-method sink:
- `interface Auditor { void audit(AuditEvent); }` `[CERT] Auditor.java:3-4` — the **config-change** trail.
- `interface SecurityAuditor { void audit(SecurityAuditEvent); }` `[CERT] SecurityAuditor.java:3-4` — the
  **security-event** trail.

`AuditEvent` `[CERT] AuditEvent.java:9-39` is the rich one — a **full change diff**:
`(operation, target, slotName, oldValue, value, userName, timestamp=Clock.time())`. Its operation vocabulary is
13 constants `[CERT] :10-22`: `Changed`, `Added`, `Removed`, `Renamed`, `Reordered`, `Flags Changed`,
`Facets Changed`, `Recategorized`, `Invoked`, `Login`, `Logout`, `Login Failure`, `Logout (Timeout)`. So a
config edit records **what changed, from which old value to which new value, on which slot of which target, by
whom, when** — a complete change ledger row.

`SecurityAuditEvent` `[CERT] SecurityAuditEvent.java:9-23` is leaner:
`(operation, userName, message, timestamp)` — 7 operations (`Login`/`Logout`/`Login Failure`/`Timeout`/
`Changed`/`Recategorized`/`Invoked`). No old→new diff; just a formatted message. It is the login/security-centric
stream.

## 564.2 The install point is PLUGGABLE and null-guarded [CERT]

Both auditors are settable singletons on `Sys` `[CERT] Sys.java:174-183`:
```java
public static Auditor         getAuditor()          { return Nre.auditor; }
public static void            setAuditor(Auditor a)  { Nre.auditor = a; }
public static SecurityAuditor getSecurityAuditor()   { return Nre.getSecurityAuditor(); }
```
So the audit SINK is installed at runtime (by the history/nss layer at station boot), not hard-wired. Every call
site guards `auditor != null` before firing — meaning **if no auditor is installed, events are silently
dropped**. Audit is presence-gated: it works because something installed a sink, not because the framework
forces one. (This is a design seam, and a hardening check: confirm an auditor is actually installed.)

## 564.3 Who fires events [CERT]

The producers span the framework `[CERT]` (caller sweep): `com.tridium.sys.schema.ComponentSlotMap` /
`ComplexSlotMap` (every add/remove/rename/reorder/change/flags/facets/recategorize → an `AuditEvent`),
`CategoryValidator` (recategorize), `BUserService` + `AutoLogoffSettingsTransferUtil` (user changes), and
`com.tridium.authn.BAuthenticationService`. The login path is explicit `[CERT]
BAuthenticationService.java:349-356`:
```java
Auditor auditor = Sys.getAuditor();
if (auditor != null) {
   AuditEvent event = auditInfo.makeAuditEvent(loginSuccessful ? "Login" : "Login Failure", user);
   auditor.audit(event);
}
```
So **every login and every failed login is audited** (with the user + source in `auditInfo`), and **every
component-model mutation is audited** at the slot-map layer — the trail is comprehensive by construction, not
per-feature opt-in.

## 564.4 The sink: history-rt audit histories [CERT]

The installed auditor routes into `history-rt` `[CERT]`:
`com.tridium.history.audit.BAuditHistoryService` + `BSecurityAuditHistorySource` +
`BAbstractAuditHistorySource`. So audit events become **history records** in a dedicated audit `BHistory` (and a
separate SECURITY audit history), queryable like any other history ([Block 8]). This is why the audit trail is
visible in the AuditHistory view and exportable by BQL.

## 564.5 Thesis — rich content, weak durability [CERT-synthesis]

The audit trail is **content-rich** (full old→new diff, actor, slot, timestamp, 13 operation types, login
failures included) and **comprehensive** (fired at the slot-map + auth layers, not per-module). Its two
weaknesses are structural, not content: (1) it is **presence-gated** — no installed `Auditor` ⇒ silent
(§564.2); and (2) once landed, it inherits the audit-history storage posture, which [Block 393]/[Block 351]
already measured as **plaintext, purgeable via `BHistoryMaintenance`, and without per-record signature/MAC**. So
the trail answers "what changed and who did it" in detail, but does NOT provide non-repudiation — consistent
with the [Block 392] cross-cut: N4 signs "who may run what", not "what happened". For a compliance posture,
`electronicSignature` ([Block 351]) is the layer that adds signing on top; the base audit trail does not.

## 564.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Two interfaces: Auditor.audit(AuditEvent) + SecurityAuditor.audit(SecurityAuditEvent) | [CERT] | Auditor.java:3-4; SecurityAuditor.java:3-4 | token-checked ✓ |
| 2 | AuditEvent = operation+target+slotName+oldValue+value+userName+timestamp; 13 operation constants incl. Login Failure | [CERT] | AuditEvent.java:10-39 | token-checked ✓ |
| 3 | SecurityAuditEvent = operation+userName+message+timestamp (no diff); 7 operations | [CERT] | SecurityAuditEvent.java:10-23 | token-checked ✓ |
| 4 | Sys.getAuditor/setAuditor + getSecurityAuditor = pluggable singleton on Nre; call sites null-guard | [CERT] | Sys.java:174-183 | token-checked ✓ |
| 5 | BAuthenticationService audits Login/Login Failure via Sys.getAuditor() | [CERT] | BAuthenticationService.java:349-356 | token-checked ✓ |
| 6 | Config mutations audited at ComponentSlotMap/ComplexSlotMap; user changes at BUserService | [CERT] | caller sweep (paths) | grep-confirmed ✓ |
| 7 | Sink = history-rt BAuditHistoryService + BSecurityAuditHistorySource | [CERT] | history-rt/…/audit/ (paths) | existence-confirmed ✓ |
| 8 | Rich+comprehensive content but presence-gated + storage tamper-evident-free (per B393/B351) | [CERT-synthesis] | rows 2,4 + [B393]/[B351] | reasoned ✓ |

**Marker tally**: [CERT] ×7 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 7 of 8
rows token-verified inline.

## Connections

- **[Block 30]** — declared the Auditor/SecurityAuditor interfaces; this traces the wiring end-to-end.
- **[Block 393]/[Block 351]** — the audit-history storage is plaintext, purgeable, unsigned; this block explains
  what fills it.
- **[Block 8]** — history subsystem; the audit sink is a BHistory.
- **[Block 510]** — BAuthenticationService is the login orchestrator; here it is also the audit producer for
  Login/Login Failure.
- **[Block 392]** — the cross-cut: signs "who may run what", not "what happened" — the audit trail is the "what
  happened" side, and it is unsigned.

## Open gaps (this block)

- `BAuditHistoryService`/`BSecurityAuditHistorySource` internals (record schema, retention, whether login-failure
  can trigger lockout counting) are named-not-decompiled — history-rt/[Block 8] territory, low value here. Focus
  continues at AC7 (BRoleHierarchies mixin).
