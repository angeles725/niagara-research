# Block 329 — The email security dashboard: 22 filters that grade each account, where severity is gated on `enabled` and the whole posture of E3/E5 becomes an ALERT/WARNING/OK verdict

> Focus **email** — evidence block E6. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: `com.tridium.email.BEmailServiceSecurityDashboardProviderAgent` — the agent that decorates the
> `EmailService` in the platform Security Dashboard and turns every account's config (debug, TLS, min-TLS,
> auth type, OAuth endpoint) into a graded item. It is the consumer that VALIDATES the security defaults
> established in [Block 326] (E3) and [Block 328] (E5).
>
> Sources (primary, decompiled N4.14.0.162), read in full inline:
> `organized/email/email-rt/vineflower/com/tridium/email/BEmailServiceSecurityDashboardProviderAgent.java`
> (402 lines).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 22 (security posture) +
> Layer 8 (notification). Block TYPE: **evidence**.

---

## 329.1 — What it is: an `@AgentOn` decorator of the EmailService

`@NiagaraType(agent=@AgentOn(types={"email:EmailService"}))` `[CERT]` (`:24-28`) — a `BObject implements
BISecurityDashboardProviderAgent` `[CERT]` (`:29`), NOT a station component. The platform Security Dashboard
discovers it via the agent registry and calls `getSecurityDashboardItems(cx)` to render the email section
`[CERT]` (`:82`), with a section header, a hyperlink to the service's nav ORD, and `itemsVersion = 2` `[CERT]`
(`:70-80`). This is the same `@AgentOn` extension pattern the corpus documented for pxEditor [Block 210]
`[INFER]` (cross-block).

## 329.2 — The dispatch: 0 accounts → one INFO; else 22 filters

`getSecurityDashboardItems` `[CERT]` (`:82-117`):
- **No accounts** → a single `INFO` item `noEmailAccounts` `[CERT]` (`:85-90`).
- **Otherwise** → it runs **22 filter methods** and concatenates their items `[CERT]` (`:92-113`).

Every filter goes through one helper, `getSecurityDashboardItemsForFilter(accounts, predicate, status,
summaryKey, descKey)` `[CERT]` (`:379-401`): it streams the accounts, keeps those matching the predicate,
and — if any match — emits ONE dashboard item at the given `BSecurityItemStatus`, listing the matching account
display-names `[CERT]` (`:388-397`). So the 22 methods are 11 conditions × {enabled, disabled}.

## 329.3 — The posture matrix (the load-bearing table)

The severity of every condition is **gated on `account.getEnabled()`**: a DISABLED account is always graded
`INFO` (informational — it is not operational, so it is not a live risk), while the SAME condition on an ENABLED
account carries its real severity `[CERT]` (compare each enabled/disabled pair below). This is the design
throughline of the whole class `[INFER]`.

**ENABLED accounts** (the operational verdicts), all `[CERT]`:

| Condition | Predicate | Status | Cite |
|---|---|---|---|
| Debug ON | `debug && enabled` | **ALERT** | `:141-150` |
| Debug OFF | `!debug && enabled` | OK | `:152-161` |
| No TLS | `!useSsl && !useStartTls && enabled` | **ALERT** | `:185-194` |
| TLS on | `(useSsl \|\| useStartTls) && enabled` | OK | `:196-205` |
| TLS on, min-TLS `< 4` | `enabled && TLS && tlsMinProtocol.ordinal < 4` | **ALERT** | `:229-238` |
| TLS on, min-TLS `>= 4` | `enabled && TLS && ordinal >= 4` | OK | `:240-249` |
| Basic auth + TLS | `TLS && enabled && authenticator is Basic` | **WARNING** | `:251-262` |
| Basic auth + no TLS | `!TLS && enabled && Basic` | **ALERT** | `:264-276` |
| OAuth | `enabled && authenticator is OAuth` | OK | `:305-314` |
| No auth | `enabled && authenticator is NoAuth` | **WARNING** | `:327-336` |
| OAuth endpoint not `https:` | `enabled && OAuth && insecureEndpoint` | **ALERT** | `:349-360` |

**DISABLED accounts**: every one of the above conditions → `INFO` `[CERT]` (`:130-139`, `:163-183`, `:207-227`,
`:278-303`, `:316-325`, `:338-347`, `:362-373`).

## 329.4 — What the thresholds confirm across the focus

- **min-TLS `ordinal < 4`** — this is the exact threshold [Block 326] §326.5 predicted: `BSslTlsEnum` ordinals
  are bit-flags 1/2/4/8, so `< 4` means `tlsv1` (1) or `tlsv1_1` (2) — anything below TLS 1.2 — is flagged
  deprecated `[CERT]` (`:210`, `:232`). The account default `tlsv1_2` (4) sits exactly on the OK boundary
  `[INFER]`.
- **Debug ALERT** — validates [Block 326] §326.6: `debug` dumps the SMTP conversation, so an enabled account
  with debug on is an ALERT `[CERT]` (`:144-145`).
- **No-TLS ALERT** — validates [Block 326] §326.5: TLS is off by default, and an enabled cleartext account is an
  ALERT `[CERT]` (`:188-189`).
- **Auth grading** validates [Block 328] E5's hierarchy: OAuth = OK; Basic auth = WARNING (outdated) even WITH
  TLS, escalating to ALERT WITHOUT TLS (credentials in cleartext); NoAuth = WARNING `[CERT]`
  (`:257`, `:271`, `:331`).
- **OAuth endpoint** — `hasInsecureOauthEndpoint` = the `authServerMetadataEndpoint` string does NOT
  `trim().toLowerCase().startsWith("https:")` `[CERT]` (`:375-377`). So an OAuth token endpoint served over
  plain HTTP is an ALERT on a live account [Block 328] §328.3 `[CERT]` (`:355`). Note the check is a prefix
  string test — it confirms the SCHEME, not certificate validity `[INFER]`.

## 329.5 — What the dashboard does NOT grade

- The inbound alarm-ack spoofing exposure [Block 327] §327.6 — the dashboard has **no item** for
  `BEmailAlarmAcknowledger` sender-trust; it grades transport/auth config, not inbound-message trust `[CERT]`
  (the 22 filters, `:92-113`, none inspect the acknowledger) `[INFER]`. That risk is invisible here.
- The `persistent=false` mail-loss default [Block 325] §325.2 — not a security item; reliability, not posture
  `[INFER]`.
- The license/JavaMail provisioning faults [Block 324] — service-start concerns, not dashboard items.

## 329.6 — Connections

- [Block 326] §326.5-326.6 — TLS-off default, debug leak, and the ordinal-4 threshold, all realized as
  dashboard verdicts here.
- [Block 328] — the OAuth/Basic/NoAuth authenticators whose types §329.3 grades; §329.4 confirms the
  OAuth-endpoint-must-be-https rule.
- [Block 327] §327.6 — the inbound-ack exposure the dashboard does NOT cover (§329.5) — a coverage gap worth
  naming.
- [Block 210] — the `@AgentOn` extension pattern this agent uses.

## 329.7 — Self-verify

Block TYPE: **evidence**. Inline full read of the 402-line class (single cohesive source). The posture matrix
(§329.3) was transcribed filter-by-filter directly from the source; each row's status enum
(`securityStatusAlert`/`Warning`/`OK`/`Info`) and predicate were read at the cited lines. In-body citations are
bare `:line` against the ONE declared source (extern — not script-verifiable; token-checked by full read).
Load-bearing anchors, with filename for auditability:
`BEmailServiceSecurityDashboardProviderAgent.java:144` (debug ALERT),
`BEmailServiceSecurityDashboardProviderAgent.java:188` (no-TLS ALERT),
`BEmailServiceSecurityDashboardProviderAgent.java:210` and `:232` (min-TLS `<4`),
`BEmailServiceSecurityDashboardProviderAgent.java:257` / `:271` (basic-auth WARNING/ALERT split),
`BEmailServiceSecurityDashboardProviderAgent.java:309` (OAuth OK),
`BEmailServiceSecurityDashboardProviderAgent.java:375` (insecure-endpoint `startsWith("https:")`) — read verbatim.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 20 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 7 |
| INFER/CERT ratio | 0.35 |

`verify-block.sh` exit 0 (citations extern — token-checked by full read).

Evidence block: `[INFER]`s are the enabled-gating design read, cross-block confirmations, and the "what it does
not grade" coverage note — each anchored to cited `[CERT]` filters.
