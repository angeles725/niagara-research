# Block 345 — The JSON alarm recipient: a `BAlarmRecipient` that renders each alarm through an embedded schema and auto-links the output to a publish point — the `BEmailRecipient` twin without the SMTP

> Focus **jsonToolkit** — evidence block J11. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the OUTBOUND ALARM path — `BJsonAlarmRecipient` (an alarm recipient that emits JSON per alarm) and its
> two alarm-data resolvers (`BJsonSchemaAlarmRecordProperty`, `BBFormatString`). The direct parallel of the email
> module's `BEmailRecipient` [Block 34] §34.6.5 / email focus.
>
> Sources (primary, decompiled N4.14.0.162 + docs), sweep sonnet + driver re-verification of the load-bearing
> claims: `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/alarm/`
> (`BJsonAlarmRecipient.java`, `property/BJsonSchemaAlarmRecordProperty.java`, `property/BBFormatString.java`,
> `property/BIJsonAlarmDataResolver.java`) and `docJsonToolkit` (`AlarmsJson-A4617E57.html`,
> `SettingUpAnAlarm-Json-6EB38683.html`, `jsonToolkit-JsonAlarmRecipient.html`,
> `jsonToolkit-JsonSchemaAlarmRecordProperty.html`).
>
> Markers: `[CERT]` local decompiled source (`file:line`) · `[CERT-doc]` official doc · `[INFER]` deduction.
> Layer 8 (alarm/notification). Block TYPE: **evidence**.

---

## 345.1 — `BJsonAlarmRecipient`: an alarm recipient with an embedded schema

`public class BJsonAlarmRecipient extends BAlarmRecipient` `[CERT]` (`BJsonAlarmRecipient.java:62`) — the SAME
`javax.baja.alarm.BAlarmRecipient` base as the email module's `BEmailRecipient` `[INFER]`. It embeds a
`BJsonSchema` (`jsonSchema` property, default `JsonSchemaBuilder().withRootObject().withUpdateStrategy(
onDemandOnly).build()`) `[CERT]` (`:48-49`) and a `BEngineCycleAlarmQueue` (`queue`) `[CERT]` (`:52-53`, `:68`).
So an `AlarmClass` routed to this recipient renders each alarm as JSON via a schema `[INFER]`.

## 345.2 — Flow: enqueue → walk tree resolving the record → generate

`handleAlarm(record)` enqueues a defensive `record.newCopy()` to the `BEngineCycleAlarmQueue` (engine-cycle
coalescing, J12) `[CERT]`, and `doProcessAlarm(record)` does the work `[CERT]` (`:130-135`):
`process(record, jsonSchema)` recursively walks the schema tree, and for every node that implements
`BIJsonAlarmDataResolver` calls `resolve(record)` (setting that property's value from the alarm), else recurses
into children `[CERT]` (`:137-146`); then `getJsonSchema().doGenerateJson(null)` serializes the tree to the
schema's `output` slot [Block 337] §337.1 `[CERT]` (`:133`). So the alarm record is the DATA, dispatched to
resolver leaves, and the schema produces one JSON per alarm `[INFER]`.

## 345.3 — Delivery: an auto-`BLink` from `output` to a publish point (still no transport)

`started()` calls `linkOutputToPublish()` `[CERT]` (`:155-159`), which resolves a configured publish-point ORD to
a `BStringWritable` and creates a LIVE link `[CERT]` (`:196-198`):

```java
this.publishLink = point.makeLink(getJsonSchema(), BJsonSchema.output, BStringWritable.in10, Context.NULL);
point.add("jsonLink?", this.publishLink, 2);
this.publishLink.activate();
```

So the recipient auto-wires `BJsonSchema.output` → a `BStringWritable.in10`. This is still the [Block 339] §339.1
no-autonomous-transport pattern — the recipient writes the JSON into a writable POINT, and whoever owns that
point (an MQTT/HTTP/file driver) does the actual push `[CERT]`. The recipient adds convenience (auto-link) over a
bare schema, but ships NO network code `[INFER]`.

## 345.4 — The two alarm-data resolvers

Both implement the one-method `BIJsonAlarmDataResolver { void resolve(BAlarmRecord); }` `[CERT]`
(`BIJsonAlarmDataResolver.java`) and extend `BJsonSchemaProperty`:

- **`BJsonSchemaAlarmRecordProperty`** — ENUM-driven field pick. `alarmProperty` is a `BDynamicEnum` built from
  `BAlarmRecord.getFrozenPropertiesArray()` (typed slots: uuid, sourceName, timestamp, ackState, priority, …)
  concatenated with the open `alarmData` fields `[CERT]` (`:19`, `:27`, `:75`). `resolve` splits on the ordinal:
  below `MAX_FROZEN_PROP_ORDINAL` → `record.get(tag)`; at/above → `record.getAlarmFacet(tag)` `[CERT]` (`:52`);
  the special `msgText`/`instructions` fields are themselves run through `BFormat.make(s)` `[CERT]` (`:61-63`).
- **`BBFormatString`** — `BFormat`-driven, exactly the email pattern. A `BFormat format` property `[CERT]`
  (`BBFormatString.java:23-24`) resolved by `FormatResolveUtil.resolveFormat(record, format, errorSubstitute)`
  (`format.format(alarmRecord)` — `%slotName%` / `%alarmData.key%` tokens) `[CERT]`. With `attemptTypeConversion`
  (default TRUE) `[CERT]` (`:31`), `"true"/"false"` → Boolean and numeric strings → Double, so the JSON value is
  TYPED, not always a string `[CERT]` — a difference from the email body, which is always text `[INFER]`.

## 345.5 — Comparison to the email `BEmailRecipient`

Both extend `BAlarmRecipient` and both expand `%alarmData.X%` `BFormat` tokens against the `BAlarmRecord`
`[CERT]` (cross-focus). The difference is the last mile `[INFER]`: `BEmailRecipient` OWNS an SMTP transport and
actively pushes the alarm over the network [Block 324]/[Block 325]; `BJsonAlarmRecipient` is a pure
schema-render pipeline that writes `output` and auto-links it to a writable point — transport delegated
downstream (§345.3). Same alarm-source contract, opposite transport philosophy — consistent with jsonToolkit's
"marshaller not transport" identity [Block 339] §339.4.

## 345.6 — The official model `[CERT-doc]`

> "Linking the alarm topic of an alarm class into the route action of a JsonAlarmRecipient triggers the
> generation of a new payload each time the alarm class receives an alarm." — `AlarmsJson-A4617E57.html`
> `[CERT-doc]`; "The JsonAlarmRecipient comes with a nested schema whose payload output depends on the alarms
> passed through" — same `[CERT-doc]`.
> The BFormat property "defines the alarm data to be extracted … `alarmData.location` to include in the payload"
> — same doc `[CERT-doc]`. AlarmRecordProperty: "these properties are only supported on the JsonAlarmRecipient's
> Schema … includes the selected Alarm Property (sourceState, uuid, alarmClass …)" —
> `jsonToolkit-JsonSchemaAlarmRecordProperty.html` `[CERT-doc]`.

## 345.7 — What this block does NOT resolve

- `BEngineCycleAlarmQueue` coalescing/backpressure → **J12** (the util/engine-cycle-queue block).
- The alarm-side of export markers (`BAlarmExportMarkerFilter`) — touched in J7 as the registry; filter detail is
  minor.

## 345.8 — Connections

- [Block 34] §34.6.5 / email focus — `BEmailRecipient`, the SMTP twin; §345.5 is the comparison.
- [Block 337] §337.1 — the `doGenerateJson` this recipient drives per alarm.
- [Block 339] §339.1 — the no-transport identity; the auto-`BLink` to a writable is the delivery.
- [Block 342] §342.3 — `BFormat`/naming; the alarm resolvers reuse `BFormat` token expansion.

## 345.9 — Self-verify

Block TYPE: **evidence** (code + `[CERT-doc]`). Delegated sweep **sonnet**; driver re-verified verbatim: `extends
BAlarmRecipient` (`BJsonAlarmRecipient.java:62`), the embedded onDemandOnly schema (`:48-49`), `doProcessAlarm`→
`process`+`doGenerateJson` (`:130-135`), the `publishLink` auto-`BLink` (`:196-198`), the enum frozen-vs-alarmData
split (`property/BJsonSchemaAlarmRecordProperty.java:27`, `:52`), and `BBFormatString`'s `BFormat` +
`attemptTypeConversion` (`property/BBFormatString.java:23-31`). `[CERT-doc]` token-checked; docs registered +
preserved under `sources/manuals/jsonToolkit-docs/`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 20 |
| CERT-doc | 8 |
| CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.25 |

`verify-block.sh` exit 0; `verify-sources.sh` no FABRICATED-CITE for B345.

Evidence block: `[INFER]`s are the JSON-per-alarm reading and the email-comparison synthesis, each anchored to a
cited `[CERT]`/`[CERT-doc]`.
