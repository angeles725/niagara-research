# B689 — JACE_UMBRELLA alarms/histories/audit (SC5): records three local audit trails, escalation disabled, zero recipients, nothing leaves the box

> Focus: **jace-station-config** · Gap **SC5** (AlarmService + HistoryService + AuditHistoryService +
> LogHistoryService + LoggingService deployed config). Sources: `config.bog` file.xml (SD P2, READ-ONLY).
> Redacted evidence: `sources/probes/B685-jace-station-config/history-alarm-audit.txt`.
> **SECRETS DISCIPLINE:** structure only. Marker `[CERT-hw]` (SD artifact). Alarm routing / history persistence
> framework = REMITTANCE [Block 34] + focus `database` [Block 402]–[Block 413]; this is the DEPLOYED config.

## 689.1 — AlarmService: defaults, escalation DISABLED, no routing

[CERT-hw] AlarmService (L7): `alarmDbConfig` = `a:FileAlarmDbConfig` empty (default file DB, no custom limits);
one `defaultAlarmClass` (L10), no other classes. The `escalationTimeTrigger` (`c:TimeTrigger`, L13) has
`triggerMode="false;00:00:00.000;23:59:59.999;60000;7f"` (L14) — the leading field is **`false` = the timer is
DISABLED**; `nextTrigger="2015-02-27…"` (L16) is a stale past date, confirming the timer never re-armed.

[CERT-hw] **Zero alarm recipients**: `grep -c 'Recipient'` over the whole config = **0**. No ConsoleRecipient,
EmailRecipient, StationRecipient, or webhook recipient. The `escalateAlarms` action exists (L18) but has no
targets. **Nothing is routed anywhere.** This is the disk-side confirmation of the operator's Telegram-egress
premise (focus `alarm-webhook`): a JACE with no recipient wired needs egress BUILT, not just enabled.

## 689.2 — Three local audit trails collected

[CERT-hw]

| Service | history id | recordType | schema fields | L |
|---|---|---|---|---|
| AuditHistoryService | /JACE_UMBRELLA/AuditHistory | history:AuditRecord | timestamp, operation, target, slotName, oldValue, value, userName | 237/240 |
| SecurityAuditHistorySource (nested) | /JACE_UMBRELLA/SecurityHistory | history:SecurityAuditRecord | timestamp, operation, userName, message | 246/249 |
| LogHistoryService | /JACE_UMBRELLA/LogHistory | history:LogRecord | timestamp, logName, severity, message, exception | 258 |

All three use TZ `America/Mexico_City;-21600000;0` (UTC−6, no DST) with millisecond timestamps. LogHistory has
no severity filter → captures all levels. These three map 1:1 to the on-disk files seen in the P2 tree
(`AuditHistory.hdb`, `SecurityHistory.hdb`, `LogHistory.hdb`, focus `jace8000-sd` [Block 674]).

[CERT-hw] **LoggingService** (L172): empty body — no appenders/handlers, factory default.

## 689.3 — The three provisioning .hdb are driver-generated, not declared

[CERT-hw]+[INFER] The P2 tree also holds `DeviceStepHistoryRecord.hdb`, `NetworkStepHistoryRecord.hdb`,
`DeviceNetworkJobHistoryRecord.hdb`. None of these appears in any `historyConfig` (`grep -c` for their record
types in the config = **0**). [INFER] They are written automatically by the provisioning subsystem
(`pn:ProvisioningNiagaraNetworkExt`, L865 — B686 §686.3) as job-tracking bookkeeping, not declared histories,
and hold provisioning metadata, not operational/user data. (Provisioning history internals = REMITTANCE focus
`provisioning` [Block 567].) **§14 CONFIRMED by [Block 703] (HD5):** this [INFER] is now [CERT-hw] — the three
files' `record_type` is `batchJob:DeviceStepHistoryRecord` / `NetworkStepHistoryRecord` /
`DeviceNetworkJobHistoryRecord`, and their record regions are empty (no provisioning job ever ran).

## 689.4 — Nothing leaves the box

[CERT-hw] HistoryService (L228): `archiveHistoryProviders` (L229) and `historyGroupings` (L231) are BOTH empty
→ **no archive provider, no off-box history push**. Combined with 689.1 (zero alarm recipients) and B685 (no
EmailService): **no configured egress of any kind**. A supervisor on foxs:4911 could pull on demand (B686), but
nothing is pushed. All operational + security + audit data is self-contained on the JACE storage.

**Security corollary (confirms B684).** Every audit/security/log record sits in cleartext `.hdb` on the SD with
no off-box copy. This is the data-side of the `jace8000-qnx-native` [Block 684] verdict — strong boot/process,
WEAK data-at-rest: whoever holds the card holds the entire audit trail, and there is no external replica to
detect tampering or deletion (the audit trail is local-only and, per `access-control` [Block 566], has no
tamper-evident chaining).

## Connections

- Alarm routing framework → [Block 34]; the operator's webhook egress design → focus `alarm-webhook`
  [Block 666]–[Block 671]. History persistence / .hdb → focus `database` [Block 402]; on-disk history files →
  focus `jace8000-sd` [Block 674]. Data-at-rest weakness verdict → focus `jace8000-qnx-native` [Block 684];
  audit non-tamper-evidence → focus `access-control` [Block 566]. Provisioning job records → focus
  `provisioning` [Block 567]. Deployed skeleton → [Block 685] (this focus).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | AlarmService defaults; escalation timer enabled=false; nextTrigger stale 2015 | [CERT-hw] | L14/L16 | grep-confirmed |
| 2 | zero alarm recipients | [CERT-hw] | grep -c Recipient = 0 | grep-confirmed |
| 3 | 3 histories: Audit/Security/Log with schemas + TZ Mexico_City | [CERT-hw] | L237/240/246/249/258 | grep-confirmed |
| 4 | LoggingService empty (default) | [CERT-hw] | L172 | grep-confirmed |
| 5 | archiveHistoryProviders + historyGroupings empty → no off-box push | [CERT-hw] | L229/L231 | grep-confirmed |
| 6 | 3 provisioning .hdb driver-generated, not declared | [INFER] | grep -c = 0 + prov ext L865 | reasoned |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. 6/6 load-bearing citations
grep-confirmed. Evidence-file secret-scan clean. The "nothing leaves the box" negative was cross-checked three
independent ways (0 recipients, empty archive providers, no EmailService) — RE-MEASURE satisfied.

## Open gaps (this focus)

SC5 CLOSED. Next investigable: **SC6** (TagDictionaryService + HierarchyService deployed — the heavy `td:`
tagging: which dictionaries, hierarchies, relations this station actually uses).
