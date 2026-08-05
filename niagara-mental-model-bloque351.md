# Block 351 — `electronicSignature`: the Part 11 audit trail is a plaintext trend history with NO crypto tamper-evidence, and its own `BHistoryMaintenance` exposes three unauthenticated purge actions — the §11.10(e) gap is real

> Focus **electronicSignature** — gap **ES4** (audit-trail protection + purge-without-signature). READ-ONLY. Corpus language: ENGLISH.
>
> Question (opened in [B350] §350.7 / §133-135): is `BSecuredTrendRecord` tamper-evident, and does the module let an
> admin PURGE the 21 CFR Part 11 audit trail WITHOUT an electronic signature? Answer: the record carries no hash/HMAC/
> chain/signature (it is a plaintext `::`-delimited `BStringTrendRecord`), the secured history is an ORDINARY Niagara
> history, and the module ships its OWN `BHistoryMaintenance` component whose three purge actions call the core history
> API directly with no re-authentication, no `BReasons`, no `SecureHelper`. §11.10(e) ("protected … computer-generated,
> time-stamped audit trails") is NOT met at the enforcement layer; the only guard is a UX-view gate that the invocation
> layer bypasses.
>
> Sources (primary, N4.14, vendor **TridiumPS**), module root `organized/electronicSignature/`:
> - `electronicSignature-rt/vineflower/com/tridiumx/ps/model/BHistoryMaintenance.java` — decompiled STRUCTURE + control-flow (bodies here are call-graph-reliable; only string literals are scrubbed).
> - `electronicSignature-rt/extracted/com/tridiumx/ps/model/BHistoryMaintenance.class` — bytecode (`javap -p`).
> - `electronicSignature-rt/vineflower/com/secured/history/BSecuredTrendRecord.java` — record structure + serialization.
> - `electronicSignature-rt/extracted/com/secured/history/BSecuredTrendRecord.class` — bytecode (`javap -c -p`, confirms no crypto).
> - `electronicSignature-rt/extracted/com/tridiumx/ps/model/BSecuredDashboardConfiguration.class` — bytecode string pool (real property/flag names).
> - `electronicSignature-ux/vineflower/electronicSignature-ux.lexicon` — view display names (CLEAN resource).
> - `docSource/docSource-doc/extracted/history-rt/javax/baja/history/BHistoryConfig.java` — CORE Tridium source: `fullPolicy` default.
> - `electronicSignature-rt/extracted/META-INF/module.xml` — type registrations.
>
> Markers: `[CERT]` local primary (`file:line`) · `[CERT-doc]` official reg/lexicon text · `[INFER]` deduction.
> Layer 22 (license/security) + compliance axis. Block TYPE: **evidence** (security).
>
> ⚠ **OBFUSCATION CAVEAT ([B350] header).** The decompiled/vineflower `.java` is string-scrubbed (literals → `n`/`ln`).
> This block cites STRINGS only from bytecode/lexicon; it uses decompiled `.java` for STRUCTURE and CONTROL-FLOW
> (superclass, `@NiagaraAction`, method call-graph) — which are intact — never for string literals. Every real name
> (`useSingleAuthenticationForDatabaseMaintenanceView`, the view classes) was re-confirmed against bytecode/lexicon.

---

## 351.1 — The record: `BSecuredTrendRecord extends BStringTrendRecord`, twelve `BString` fields, ZERO integrity

`BSecuredTrendRecord` (and its alarm twin `BSecuredTrendRecordForAlarm`) subclass the ordinary core history record
`javax.baja.history.BStringTrendRecord` `[CERT]` (`BSecuredTrendRecord.java:71`, `import javax.baja.history.BStringTrendRecord`).
Its state is twelve `@NiagaraProperty` slots, all `BString` `[CERT]` (`BSecuredTrendRecord.java:22-70`): `oldStatus`,
`oldValue`, `reasonForChange`, `PrimarySigner`, `FullNamePrimarySigner`, `SecondarySigner`, `FullNameSecondarySigner`,
`PrimarySignerComments`, `SecondarySignerComments`, `pointName`, `operation`, `remoteAuthenticationAction`.

Serialization is a single plaintext string. `doWriteTrend(DataOutput)` `[CERT]` (`BSecuredTrendRecord.java:270`) concatenates
the fields with a `"::"` separator and emits `out.writeUTF(temp)`; `doReadTrend(DataInput)` `[CERT]` (`:187-189`) does
`in.readUTF()` then `temp.split("::")`. There is **no** hash, HMAC, digest, per-record sequence number, chain link to the
prior record, or signature-over-the-record. Bytecode confirms the absence: `javap -c -p BSecuredTrendRecord.class` over
`doWriteTrend` shows only the field getters, string concatenation, and `writeUTF` — no `MessageDigest`/`Mac`/`Cipher`/
`Signature` opcode or constant `[CERT]`. The module JAR bundles `org.apache.commons.codec.digest.*` (`DigestUtils`,
`HmacUtils`) but **no class in `com.secured.*` or `com.tridiumx.ps.*` imports or calls it** `[CERT]` (grep over the rt
tree: zero hits for those symbols in the module's own packages).

**Verdict (a):** each audit record is a plaintext data row. Its ONLY protection is access control on the history store —
there is no cryptographic means to detect that a record was altered or removed. This is the definition of a
non-tamper-evident audit trail. (Contrast: a Part 11-grade trail chains or signs each entry so deletion/edit is detectable.)

## 351.2 — The history: an ORDINARY Niagara history created through the standard `HistorySpaceConnection` API

`BSecuredDashboardConfiguration` is the single owner of the secured history. It holds a `securedHistoryName` property
whose default is the literal `"SecuredPointsHistory"` `[CERT]` (`BSecuredDashboardConfiguration.java:292`,
`newProperty(0, "SecuredPointsHistory", null)`; an alarm history parallels it). On startup it resolves/creates the
history via the standard service chain `[CERT]` (`:779-785`): `BHistoryId.make(historyDb.getDeviceName(), getSecuredHistoryName())`
→ `HistorySpaceConnection` → `createHistory(getSecuredHistoryConfig())`. This is the SAME `BHistoryService` /
`BHistoryDatabase` / `HistorySpaceConnection` path the core History subsystem exposes ([B34] history layer): the secured
audit trail is a **first-class ordinary Niagara history**, distinguished only by its `recordType`. Nothing in the config
build sets an ACL, a signature requirement, or a write-once flag `[CERT]` (no such call in the config builder; the builder
methods themselves are string-scrubbed to `conn.n(...)`, but the STRUCTURE shows only id/name/recordType/timeZone wiring).

Because it is an ordinary history, the CORE History Manager's own clear/delete facilities operate on it with no knowledge
of, and no call into, the eSignature module — a second, module-independent purge path `[INFER, from 351.2+351.3: standard
history + no ACL ⇒ core tools reach it]`.

## 351.3 — The purge surface: `BHistoryMaintenance extends BComponent` — three `newAction(0,…)` actions straight to the core API

The module ships its OWN maintenance component, `com.tridiumx.ps.model.BHistoryMaintenance`, registered in `module.xml:68`
as type `HistoryMaintenance` `[CERT]`. It is a plain `BComponent` `[CERT]` (`BHistoryMaintenance.java:32`) — no mixin, no
Level-2/secured-user gate, no `@NiagaraProperty` of its own (it carries no retention config; the action parameters supply
the target). It declares three actions `[CERT]` (`:19-35`), each built with `newAction(0, …)`:

| Action | Param | `do*` body → core call | Auth / signature in the path |
|---|---|---|---|
| `clearAllRecords` | `BOrdList` | `doClearAllRecords` `:55` → `HistorySpaceConnection conn = historyDb.getDbConnection(null); conn.clearAllRecords(ordList.toArray())` `:60,64` | NONE |
| `deleteHistories` | `BOrdList` | `doDeleteHistories` `:86` → `conn.deleteHistories(ordList.toArray())` `:91,95` | NONE |
| `clearOldRecords` | `BComponent` | `doClearOldRecords` `:117` → `conn.clearOldRecords(ordList.toArray(), beforeTime)` `:124,128` | NONE |

All three `do*` bodies are decompiled cleanly (only string literals scrubbed; the CALL GRAPH is intact) and `javap -p`
confirms the exact method set (`doClearAllRecords`/`doDeleteHistories`/`doClearOldRecords` + the public action invokers)
`[CERT]`. None of them calls `SecureHelper`, `verify*Credentials`, `authenticate*`, `BReasons`, or any
`*WithAuthentication` verb `[CERT]` (grep of the file for those tokens: zero hits). They resolve the `BHistoryService`,
open a `HistorySpaceConnection`, and delete — nothing else.

**The action flag `newAction(0,…)` — framework-semantic reading.** Flag `0` is the default: no `CONFIRM_REQUIRED`, no
`OPERATOR`/`ADMIN`-only restriction expressed on the slot, and (critically for Part 11) no re-authentication and no reason
capture. A user with ordinary Niagara **invoke permission** on the component can fire these actions — over `fox:`, a
`station:` ORD, the Niagara API, or `HistorySpaceConnection` directly. Ordinary RBAC ≠ an electronic signature: Part 11
requires the *signed, re-authenticated, reason-bearing* ceremony the module enforces on POINT WRITES (the `*WithAuthentication`
set, [B350] §350.5) — and that ceremony is entirely absent from the audit-trail deletion path.

**Verdict (b):** purge-without-signature is reachable by (1) the module's own `BHistoryMaintenance` actions, and (2) the
core History Manager acting on the same ordinary history (§351.2).

## 351.4 — The only guard is a UX-view gate, and it does not cover the invocation layer

The module's protection for maintenance is a Workbench/UX **view**, not an enforcement hook. `BSecuredDashboardConfiguration`
injects the agents `electronicSignature:ProtectedHistoryDatabaseMaintenanceView` and its alarm sibling into its agent list
programmatically (not in `module.xml`) `[CERT]` (view classes exist:
`electronicSignature-ux/.../webeditors/BProtectedHistoryDatabaseMaintenanceView.class` and
`BProtectedAlarmDatabaseMaintenanceView.class`; lexicon display names `Protected History Database Maintenance View` /
`Protected Alarm Database Maintenance View` `[CERT-doc]` `electronicSignature-ux.lexicon`). Whether that view demands a
single authentication is controlled by a boolean property whose real name — recovered from the bytecode string pool, since
the decompiled name is scrubbed to `n` — is `useSingleAuthenticationForDatabaseMaintenanceView` `[CERT]`
(`BSecuredDashboardConfiguration.class` string pool; sibling `useSingleAuthenticationForAlarm`).

Two structural limits make this UX gate insufficient for §11.10(e):
1. **It is a view, over the action layer.** The `BHistoryMaintenance` actions (§351.3) are invocable directly on the
   component; a caller who does not open the protected view (API/fox/ORD) never encounters the gate `[INFER, from 351.3:
   actions are default-flag and reachable without the view]`.
2. **It is one authentication, and it is toggleable.** Even for the UX path, the guard is a *single* authentication (not
   the dual/second-signer Part 11 ceremony), and setting `useSingleAuthenticationForDatabaseMaintenanceView=false` removes
   even that `[INFER, from the property's boolean semantics]`.

## 351.5 — Automatic loss: `fullPolicy` inherits the core default `roll` (capacity requested UNLIMITED)

The secured `BHistoryConfig` does not pin a `fullPolicy`; the core default applies. Core Tridium source
`javax/baja/history/BHistoryConfig.java` declares `fullPolicy` with default value `BFullPolicy.roll` `[CERT]`
(`newProperty(0, BFullPolicy.roll, null)`). Under `roll`, when a history reaches capacity the engine silently discards the
oldest records — no signature, no reason, no audit event. The module requests `BCapacity` on the config (the bytecode
string pool of the rt tree references `javax/baja/history/BCapacity` and the string `fullPolicy` `[CERT]`; the sub-agent
sweep read the requested capacity as UNLIMITED), so the AUTOMATIC roll-off is DORMANT by default — an unlimited history
never fills. But the policy remains `roll`, not `stop`: any later capacity reconfiguration (a finite `BCapacity`, common
on space-constrained JACEs) **re-arms silent, unsigned deletion of the oldest audit records** `[INFER, from core default
roll + finite-capacity semantics]`. A Part 11 trail should use a `stop`/alarm policy, never `roll`.

## 351.6 — Why this matters: the module secures the WRITE but not the RECORD OF the write

[B350] established the module's strength: a point write is intercepted, re-authenticated, reason-tagged, optionally
dual-signed, and appended to `BSecuredTrendRecord`. ES4 shows the asymmetry: that carefully-signed record then lands in an
ordinary, non-tamper-evident history whose deletion requires no signature at all. The compliance envelope is therefore
**open at the back**: an actor with admin-level station access (or filesystem/DB access to the history store — the records
are plaintext `::` strings, §351.1) can erase or alter the evidence of a signed change without leaving a Part 11 trace. The
strong front-door ceremony ([B350] §350.5) and the weak back-door audit protection (this block) are the two halves of the
same §11.10(e) requirement — and only the first is met.

This does not require a live station to assert: it is structural, from decompiled control-flow + bytecode + core source.
Whether a stock RBAC role actually grants the invoke permission on `BHistoryMaintenance` in a shipped `ProtectedDashboard`
instance — i.e. the exploit's real-world reachability for a non-super-user — is a live-permission question (child gap
**ES4-G1**, requires-execution).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BSecuredTrendRecord extends BStringTrendRecord` | `[CERT]` | `BSecuredTrendRecord.java:71` (import :8) | ✅ read |
| 2 | 12 fields, all `BString` | `[CERT]` | `BSecuredTrendRecord.java:22-70` | ✅ read |
| 3 | Serialization = `writeUTF` of `"::"`-joined string, `readUTF`+`split("::")` | `[CERT]` | `BSecuredTrendRecord.java:270 / 187-189` | ✅ read |
| 4 | No digest/HMAC/cipher/signature in the record (bytecode) | `[CERT]` | `javap -c -p BSecuredTrendRecord.class` (no crypto opcode/const) | ✅ ran |
| 5 | commons-codec present in JAR, never called by module packages | `[CERT]` | grep `Digest`/`Hmac` in `com.secured.*`/`com.tridiumx.ps.*` = 0 | ✅ ran |
| 6 | Secured history created via standard `HistorySpaceConnection.createHistory` | `[CERT]` | `BSecuredDashboardConfiguration.java:779-785` | ✅ read |
| 7 | `securedHistoryName` default `"SecuredPointsHistory"` | `[CERT]` | `BSecuredDashboardConfiguration.java:292` | ✅ read |
| 8 | `BHistoryMaintenance extends BComponent`, registered `module.xml:68` | `[CERT]` | `BHistoryMaintenance.java:32`; `module.xml:68` | ✅ read |
| 9 | 3 actions `newAction(0,…)`: clearAllRecords/deleteHistories/clearOldRecords | `[CERT]` | `BHistoryMaintenance.java:19-35`; `javap -p` | ✅ read+ran |
| 10 | `do*` bodies call `conn.clearAllRecords/deleteHistories/clearOldRecords` directly | `[CERT]` | `BHistoryMaintenance.java:55-64/86-95/117-128` | ✅ read |
| 11 | No SecureHelper/auth/BReasons/*WithAuthentication in the purge path | `[CERT]` | grep of `BHistoryMaintenance.java` = 0 hits | ✅ ran |
| 12 | Real flag name `useSingleAuthenticationForDatabaseMaintenanceView` (bytecode, scrubbed in .java) | `[CERT]` | `BSecuredDashboardConfiguration.class` string pool | ✅ ran |
| 13 | View classes `BProtectedHistory/AlarmDatabaseMaintenanceView` exist; lexicon display names | `[CERT]`/`[CERT-doc]` | ux `webeditors/*.class`; `electronicSignature-ux.lexicon` | ✅ ran |
| 14 | Core `BHistoryConfig.fullPolicy` default = `BFullPolicy.roll` | `[CERT]` | `docSource/.../history-rt/.../BHistoryConfig.java` | ✅ read |
| 15 | Purge reachable without signature via actions + core tools; roll re-armable on finite capacity | `[INFER]` | derived from 351.2-351.5 | ⚠ deduction |
| 16 | UX view does not cover the invocation layer | `[INFER]` | derived from 351.3 (default-flag actions) + 351.4 | ⚠ deduction |

Marker tally: `[CERT]` ×12 · `[CERT-doc]` ×1 (item 13 dual) · `[INFER]` ×3 (items 15, 16, and the core-reach note in 351.2).
[INFER]/[CERT] ≈ 3/12 = 0.25 — LOW for an evidence block: the load-bearing purge-and-no-crypto findings are all `[CERT]`;
the `[INFER]`s are the exploitability/consequence deductions built on top. Not an exhaustion signal — ES4's core evidence
is closed; the residual is the live-permission reachability (ES4-G1).

Framework-semantic check applied to items 9-11 (a delegated-sweep security claim): confirmed the actions' `newAction(0,…)`
default flag and the empty auth call-graph MYSELF against source+bytecode, and read the flag as ordinary RBAC-invoke (not
"unguarded") — the compliance gap is the ABSENCE OF A SIGNATURE, not the absence of all permission.

## Connections

- [Block 350] — ES1 foundation: opened ES4 (§350.7, §133-135); the `*WithAuthentication` write ceremony this block shows the audit trail does NOT mirror (§350.5); `BSecuredTrendRecord` field schema (§350.6 item 9).
- [Block 34] — the core history / alarm layer (`BHistoryService`, `BEmailRecipient`); the secured history is an ordinary member of that subsystem.
- Core history maintenance semantics (`HistorySpaceConnection.clearAllRecords/deleteHistories/clearOldRecords`, `BFullPolicy`) — the API the module's actions delegate to.

## Open gaps after this block

- **ES4-G1** (requires-execution, live-permission): does a stock RBAC role in a shipped `ProtectedDashboard` actually grant invoke permission on `BHistoryMaintenance` to a non-super-user? Reachability of the purge for an ordinary operator is a live-station question.
- ES2 (sign-flow end-to-end), ES3 (dual/remote transport), ES5 (credential handling — Base64/sleep), ES6 (ux/wb layers), ES7 (mutable ESignAcknowledgement) remain queued.
