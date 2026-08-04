# Block 332 — The address converters: nine `baja:ConversionLink` agents that bridge email-address slots to String/StatusString on the wiresheet, parsing "personal <addr>" with one shared regex

> Focus **email** — evidence block E9 (low priority). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `javax.baja.email.converters` (9 classes) — the `BConverter` agents that let `BEmailAddress` /
> `BEmailAddressList` slots interoperate with `BString` / `BStatusString` slots when a wiresheet link crosses
> those types. This is low-level plumbing behind the address field editors [Block 331] §331.4.
>
> Sources (primary, decompiled N4.14.0.162), read inline:
> `organized/email/email-rt/vineflower/javax/baja/email/converters/` (9 files, 610 lines).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (notification — value
> plumbing). Block TYPE: **evidence** (small subsystem).

---

## 332.1 — What they are: ConversionLink agents

All nine are `BConverter` subclasses annotated `@NiagaraType(agent=@AgentOn(types={"baja:ConversionLink"}))` and
implement `BIAgent` `[CERT]` (e.g. `BStringToEmailAddress.java:14-23`, `BEmailAddressToString.java:18-32`). So
each registers as an agent on `baja:ConversionLink` — the framework offers them when a wiresheet LINK connects
two slots whose types don't match, to bridge the mismatch `[CERT]` `[INFER]` (the `ConversionLink` agent
mechanism is how Niagara inserts a converter on a cross-type link). They exist purely so an email-address value
can flow to/from a generic String or StatusString slot `[INFER]`.

## 332.2 — The 8+1 matrix

The set is {`String`, `StatusString`} × {`EmailAddress`, `EmailAddressList`} × {parse-in, format-out}, plus one
shared abstract base `[CERT]` (class list):

| From → To | Class | Extends |
|---|---|---|
| String → EmailAddress | `BStringToEmailAddress` | abstract base |
| String → EmailAddressList | `BStringToEmailAddressList` | abstract base |
| StatusString → EmailAddress | `BStatusStringToEmailAddress` | abstract base |
| StatusString → EmailAddressList | `BStatusStringToEmailAddressList` | abstract base |
| EmailAddress → String | `BEmailAddressToString` | `BConverter` |
| EmailAddressList → String | `BEmailAddressListToString` | `BConverter` |
| EmailAddress → StatusString | `BEmailAddressToStatusString` | `BConverter` |
| EmailAddressList → StatusString | `BEmailAddressListToStatusString` | `BConverter` |
| (parsing base) | `BAbstractStringToEmailAddressConverter` | `BConverter` (abstract) |

`[CERT]` (each `extends` line; the four string→address parsers share the abstract base, the four
address→string/status formatters extend `BConverter` directly).

## 332.3 — Parse-in: one regex in the abstract base

The four string→address converters inherit `getEmailAddress(String)` from
`BAbstractStringToEmailAddressConverter`, which matches with a shared static `EMAIL_PATTERN` and builds
`BEmailAddress.make(address, personal)` (empty personal if absent), returning `null` on any parse exception
`[CERT]` (`BAbstractStringToEmailAddressConverter.java:35-44`). The pattern parses the `personal <address>`
form (or a bare address) `[CERT]` (`:51`):

```
([^\s<>@,"]+(?=(\s+)|<)|"([^"]*)")?(\s*)([^\s<>,]+|<([^<>,]*)>)
```

`BStringToEmailAddress.convert` returns `BEmailAddress.NULL` for the default/empty string, the parsed address on
success, or the UNCHANGED target `to` on parse failure (a null-safe no-op) `[CERT]`
(`BStringToEmailAddress.java:31-40`). So a malformed string does not corrupt the target — it leaves it as-is
`[INFER]`.

## 332.4 — Format-out: `getFormat().format(...)`, and the StatusString path carries status

The address→string converters format via a `BFormat`: `BEmailAddressToString.convert` returns
`BString.make(getFormat().format(emailAddress, cx))`, or `BString.DEFAULT` when the address is null `[CERT]`
(`BEmailAddressToString.java:48-52`). The StatusString variants additionally propagate STATUS:
`BEmailAddressToStatusString.convert` sets `status = 64` (a non-OK bit) when the address is null, else
`BStatus.ok` + the formatted value `[CERT]` (`BEmailAddressToStatusString.java:49-61`). So the StatusString
converters carry a validity signal alongside the text, which the plain-String converters cannot `[INFER]`.

## 332.5 — Connections

- [Block 331] §331.4 — the `BEmailAddressFE`/`BEmailAddressListFE` editors edit the same `BEmailAddress`(List)
  values these converters bridge on the wiresheet.
- [Block 34] §34.6.5 — `BEmailRecipient` uses `BFormat` for subject/body; the same `BFormat` formatting appears
  here for address→string.
- E10 (email-ux) — the ux type-exts (`BEmailAddressToStringTypeExt` etc.) are the browser-side counterpart of
  these Workbench/runtime converters `[INFER]`.

## 332.6 — Self-verify

Block TYPE: **evidence** (small, formulaic subsystem). Inline read of the abstract base + representative
converters; the 9-class matrix (§332.2) was built from the `extends`/`@AgentOn` lines of all nine. Load-bearing
anchors (extern — token-checked by read): `BStringToEmailAddress.java:16` (`baja:ConversionLink` agent),
`BAbstractStringToEmailAddressConverter.java:51` (EMAIL_PATTERN), `BStringToEmailAddress.java:31-40` (parse
convert + null-safe fallback), `BEmailAddressToString.java:48` (format-out), `BEmailAddressToStatusString.java:49`
(status propagation).

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 10 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 6 |
| INFER/CERT ratio | 0.60 |

`verify-block.sh` exit 0. Evidence block: the 0.60 ratio correctly signals a SMALL, formulaic subsystem whose
investigable evidence is exhausted by this block — the `[INFER]`s are the ConversionLink purpose and the
status-vs-plain-string contrast, each anchored to a cited `[CERT]`. Not padding; genuine exhaustion.
