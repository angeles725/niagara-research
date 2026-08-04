# Block 341 — The inbound handlers: point writes ARE authorized as `runAsUser` (real gate), but the JSON sender picks the priority slot, the alarm-ack records a spoofable acker name, and export-marker registration has NO ACL at all

> Focus **jsonToolkit** — evidence block J7 (SECURITY). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the INBOUND HANDLERS that WRITE to the station from received JSON — `BJsonSetPointHandler` (point write),
> `BAlarmUuidAckHandler` (alarm ack), and the export-marker registration handlers — and their authorization under
> the `runAsUser` identity [Block 335] §335.4. Fed by the routers of [Block 340] §340.4.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + **driver re-verification of every security
> claim** (the sweep OVERSTATED the setpoint case; corrected below):
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/inbound/handler/*`,
> `.../outbound/schema/support/JsonSchemaSecurity.java`, `.../exportMarker/register/BExportMarkerRegister.java`,
> and `docJsonToolkit` (`SetpointHandlerAndWritingToPoints-A651B788.html`,
> `HandlersAndAlarmAcknowledgments-Jso-A64F641F.html`, `ExportSetpointHandlerAndExportRegis-A663DE65.html`,
> `jsonToolkit-JsonExportRegistrationHandler.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Layer 22 (security). Block TYPE: **evidence**.

---

## 341.1 — The authority: a `runAsUser` `BasicContext`

Handlers obtain their write authority from `JsonSchemaSecurity.createServiceContext()`, which reads
`BJsonSchemaService.service().getRunAsUserInternal()` (the superuser-only, tamper-guarded getter [Block 335]
§335.4), looks up the `BUser`, and returns `new BasicContext(user)` — or `null` if `runAsUser` is unset `[CERT]`
(`JsonSchemaSecurity.java:60-84`). So every handler executes as the configured `runAsUser` `[CERT]`. The doc
confirms it is mandatory: "`runAsUser` is a mandatory property for the setpoint handler" —
`SetpointHandlerAndWritingToPoints-A651B788.html` `[CERT-doc]`.

## 341.2 — Setpoint write: authorized as `runAsUser`, but sender picks the priority slot

`BJsonSetPointHandler.setValue()` calls `checkPermissions(target, slot, ...)` FIRST `[CERT]`
(`BJsonSetPointHandler.java:151`), which `[CERT]`:
1. builds the `runAsUser` context (`createServiceContext()`); `null` → `SetpointValueRejectedException` (`:167-173`);
2. calls `JsonSchemaSecurity.userHasWritePermission(target, ctx)` → `ctx.getUser().getPermissionsFor(point)
   .hasAdminWrite() || hasOperatorWrite()` `[CERT]` (`JsonSchemaSecurity.java:57-64`); false →
   `SetpointValueRejectedException("Rejected write based on lack of permissions for <user>")` (`:175-179`).

**So inbound point writes ARE authorized** — the write only proceeds if `runAsUser` genuinely holds
operator/admin-write on that point `[CERT]`. (The sweep initially called this a bypass; that is WRONG — the
manual pre-flight is a real authorization gate. Correction recorded per the framework-semantic-check rule.)

Two accurate nuances remain `[CERT]`/`[INFER]`:
- **Sender-chosen priority.** The target slot name comes from the JSON (`slotNameKey`, default `"slotName"`) with
  NO priority-level whitelist — a sender can name `in1` (highest) … `in16` or `fallback` `[CERT]`
  (`BJsonSetPointHandler.java:96-115`). So within `runAsUser`'s write rights, the SENDER picks which priority-array
  level to drive — a point-control-integrity consideration `[INFER]`.
- **Context-free commit.** The actual write is the 2-arg `target.set(slotName, value)` — no `Context` passed
  `[CERT]` (`:153`); the manual check (not the framework re-check) is the gate, and the AUDIT is emitted
  separately WITH the `runAsUser` context `[CERT]` (`:161-163`). So writes are attributed to `runAsUser` in the
  audit log `[CERT]`.

## 341.3 — Alarm-ack: genuinely gated, but the recorded acker name is spoofable

`BAlarmUuidAckHandler` gates the ack on `runAsUser` `[CERT]` (`BAlarmUuidAckHandler.java:129`, `:168-177`):
`checkAlarmClassPermissions` builds the `runAsUser` context and calls
`ctx.getUser().check(alarmClass, BPermissions.adminWrite)` — a `PermissionException` if `runAsUser` lacks
admin-write on the record's alarm class. **This is a REAL capability gate — stronger than the email module's
`From:`-only check** [Block 327] §327.6 `[INFER]`.

But the acker IDENTITY written onto the record is taken verbatim from the JSON `"user"` field, unvalidated
(fallback constant `"AlarmUuidAckUser"`), then `record.setUser(ackUserName)` before `ackAlarm(record)` `[CERT]`
(`:66-69`, `:134-135`). So **the recorded "who acknowledged" is attacker-controlled** — an AUDIT-INTEGRITY
spoof, not an authorization bypass: the ACT requires `runAsUser` to be authorized, but the alarm record will
display whatever acker name the JSON claimed `[CERT]`. This is a narrower defect than email B327 (which let an
unauthorized party ack at all); here the party must be authorized-as-`runAsUser`, but can forge the attribution
`[INFER]`. The doc names the gate ("must have admin write permissions for the alarm class") but does NOT flag the
unvalidated `user` field — `HandlersAndAlarmAcknowledgments-Jso-A64F641F.html` `[CERT-doc]`.

## 341.4 — Export-marker registration: NO ACL (the sharpest finding)

`BJsonExportRegistrationHandler.routeValue()` builds an ORD straight from the untrusted JSON and resolves it
against the station with NO permission check `[CERT]` (`BJsonExportRegistrationHandler.java:62-63`):

```java
BOrd handleOrd = BOrd.make(niagaraId);                 // attacker-controlled
BComponent p = (BComponent) handleOrd.get(Sys.getStation());   // resolved, no ACL / no Context check
...
marker.setId(platformId);                               // attacker-controlled marker ID  (:84)
```

I grep-confirmed the ONLY `Context`/permission tokens in the class are an `import` and the `routeValue(..., Context
cx)` signature — the `cx` is never used to authorize the registration; there is NO `JsonSchemaSecurity`, NO
`BPermissions`, NO ORD whitelist `[CERT]`. `BExportMarkerRegister.registerMarker()` only checks the ID is
non-empty before inserting into its map `[CERT]` (`BExportMarkerRegister.java:109-115`). The only structural guard
is that the ORD must resolve to a component carrying exactly one `BJsonExportMarker` child `[CERT]`.

**Verdict:** an inbound JSON message can register or RE-register any export-marked component to an
attacker-controlled ID, with no ACL. Consequence: marker-registry poisoning — an attacker who can inject messages
can redirect a later `BJsonExportSetpointHandler` write from its intended point to any other export-marked point
by re-registering that point's marker under a known ID `[INFER]`. The doc frames this as a feature ("allows the
cloud to assign its own identifier … to export-marked points") with no security note —
`ExportSetpointHandlerAndExportRegis-A663DE65.html` `[CERT-doc]`; the design assumes the sender IS the trusted
cloud `[INFER]`.

## 341.5 — The trust model

jsonToolkit inbound trusts the SENDER `[INFER]`. `runAsUser` bounds what point-writes and alarm-acks CAN do (real
gates, §341.2/§341.3), but (a) the sender chooses the priority slot, (b) the ack attribution is forgeable, and
(c) export-marker registration is entirely ungated (§341.4). This is the same shape as the email inbound-ack
[Block 327] §327.6 — a module built for a trusted upstream, safe only behind an authenticated, access-controlled
transport [Block 339] (which the module does NOT itself provide) `[INFER]`.

## 341.6 — What this block does NOT resolve

- The export-marker FILTERS (`BAlarmExportMarkerFilter`, `BHistoryExportMarkerFilter`) and the outbound export
  side of markers → touched here only as the registry the inbound handler poisons; deeper filter detail is
  adjacent to J11 (alarm) / J8.
- `ModuleThreadPool` sizing for the `post` dispatch → J12.

## 341.7 — Connections

- [Block 335] §335.4 — `runAsUser`, the authority these handlers assume; the superuser-only guard is what keeps
  it trustworthy.
- [Block 340] §340.4-340.5 — the routers that place values into the slots these handlers act on; `learnMode`
  auto-create + these handlers together are the inbound write surface.
- [Block 327] §327.6 (email) — the comparison throughout: jsonToolkit's ack is BETTER-gated (runAsUser adminWrite)
  but shares the spoofable-attribution defect; jsonToolkit ADDS the ungated export-registration.
- [Block 32] §32.3 — the jsonToolkit dependency-CVE thread; this block adds the application-layer inbound-trust
  surface.

## 341.8 — Self-verify

Block TYPE: **evidence** (SECURITY; code + `[CERT-doc]`). Delegated sweep **sonnet**, but per the
framework-semantic-check + falsify-before-reporting rules the driver RE-READ every security claim: the setpoint
`checkPermissions`→`userHasWritePermission` gate (`BJsonSetPointHandler.java:151`, `JsonSchemaSecurity.java:57-64`)
— **corrected the sweep's "bypass" overstatement to "authorized, sender-picks-priority"**; the context-free
`target.set` (`:153`); the alarm-ack `adminWrite` gate + verbatim `record.setUser` (`BAlarmUuidAckHandler.java:134`,
`:177`); and the export-registration NO-ACL, verifying the 2 grep "Context" hits were an import + a parameter, not
a check (`BJsonExportRegistrationHandler.java:62-84`). `[CERT-doc]` token-checked; docs registered + preserved
under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 19 |
| CERT-doc | 5 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 9 |
| INFER/CERT ratio | 0.38 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B341.

Evidence block: `[INFER]`s are the exploit-consequence readings (priority choice, attribution forgery, registry
poisoning) and the trust-model synthesis — each anchored to a driver-verified `[CERT]`. The most important act of
this block was DOWNGRADING an over-stated delegated finding, not adding one.
