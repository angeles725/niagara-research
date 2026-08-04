# Block 331 — The Workbench UI: `BEmailAccountManager` with no "send test" button, a name-picker that populates the recipient dropdown from the live service, and secure New-account defaults that override the insecure component defaults

> Focus **email** — evidence block E8. READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the `email-wb` Workbench layer — the account manager view, the account name-picker field editors, the
> `BEmail`/address editors, and the Hx (legacy HTML) parallel. Answers a commissioning-level question: HOW does
> an operator configure email in Workbench, and can they test-send from the UI?
>
> Sources (primary, decompiled N4.14.0.162), sweep sonnet + driver spot-check of the two load-bearing claims:
> root `organized/email/email-wb/vineflower/com/tridium/email/`, files `ui/BEmailAccountManager.java` (227),
> `ui/BOutgoingAccountFE.java` (22), `ui/BIncomingAccountFE.java` (22), `ui/BEmailFE.java` (124),
> `ui/BEmailAddressFE.java` (100), `ui/BEmailAddressListFE.java` (150), `hx/BHxEmailFE.java` (108).
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer 8 (notification — WB UI tier).
> Block TYPE: **evidence**.

---

## 331.1 — `BEmailAccountManager`: the account table, its New commands, and the missing test button

`BEmailAccountManager extends BAbstractManager`, `@AgentOn(types={"email:EmailService"})` `[CERT]`
(`BEmailAccountManager.java:42`, `:38-41`) — so double-clicking the EmailService opens this manager (the
standard Niagara table-manager view). Its model exposes ~10 columns `[CERT]` (`:45-98`): Name, Enabled,
Protocol (transport/store), Hostname, a derived Account (username, from `emailAuthenticator.toDisplayString`),
Authentication Type, Port, Poll Rate, Status, SSL.

New-instance types are **Outgoing and Incoming** only `[CERT]` (`:201`,
`MgrTypeInfo.makeArray(BOutgoingAccount.TYPE, BIncomingAccount.TYPE)`). The `newInstance` setup applies
**secure defaults**: an OAuth `ClientSecretAuthenticator`, SSL on, and IMAP 993 / SMTP 587 `[CERT]`
(`:211-212`, `:204-225`).

**The finding — no test-send.** There is NO "send test email" button, action, or `Command` anywhere in the
`email-wb` UI — grep for `test`/`send`/`preview`/`TestEmail` across `ui/` and `hx/` returns no such affordance
`[CERT]` (whole-layer grep, verified by driver). The manager offers only New (Outgoing/Incoming) `[CERT]`
(`:201`). So an operator CANNOT validate an SMTP config from the Workbench UI — commissioning is confirmed only
by triggering a real alarm through a `BEmailRecipient` [Block 34] §34.6.5 or invoking `BEmailService.send` from
a Program [Block 324] §324.5 `[INFER]`. This is a real gap for field commissioning `[INFER]`.

## 331.2 — Secure UI defaults sit ON TOP of insecure component defaults

The raw component defaults are cleartext: `useSsl=false`, `useStartTls=false`, a Basic authenticator [Block 326]
§326.5, [Block 330] §330.2. But the Manager's **New** path overrides them with SSL on + OAuth ClientSecret +
implicit-SSL/submission ports `[CERT]` (`BEmailAccountManager.java:211-212`). So an account CREATED through the
Workbench manager starts secure, while one instantiated by other means (palette drop, programmatic add) inherits
the insecure component defaults `[INFER]`. The security dashboard [Block 329] grades the ACTUAL state, so it
catches the second case — the two mechanisms (secure-by-wizard, graded-by-dashboard) are complementary `[INFER]`.

## 331.3 — The name-picker: how the recipient's account dropdown is filled

`BOutgoingAccountFE` and `BIncomingAccountFE` are thin subclasses of `BComponentNamePickerFE` whose `list()`
returns `loadFromService(BEmailService.TYPE, <account class>)` `[CERT]`
(`BOutgoingAccountFE.java:19-20`, `BIncomingAccountFE.java:20`):

```java
public BComponent[] list() throws Exception {
   return this.loadFromService(BEmailService.TYPE, BOutgoingAccount.class);
}
```

`loadFromService` walks the live `BEmailService` subtree and returns every child of that account class as a named
dropdown option `[CERT]`. Neither FE carries `@AgentOn` — they are bound by the PROPERTY that declares them as
its field editor `[CERT]` (no agent annotation present). This is exactly the mechanism behind
`BEmailRecipient.emailAccount` [Block 34] §34.6.5: that property declares `BOutgoingAccountFE` as its editor, so
the recipient's account dropdown lists the configured outgoing accounts by name — which is WHY the recipient
references its account by name rather than ORD `[INFER]` (cross-block: it picks from this list).

## 331.4 — The value editors: `BEmailFE` and the address FEs

`BEmailFE` (`@AgentOn email:Email`) composes sub-editors for a `BEmail` `[CERT]` (`BEmailFE.java:28-32`, `:36-41`):
`from`→`BEmailAddressFE`, `to`/`cc`/`bcc`→`BEmailAddressListFE`, `subject`→`BTextField`, `body`→multiline text.
It renders inside a scrollable dialog (`IDialogContentProvider`) and its save path validates and writes the
`BEmail` back — **no send/preview** `[CERT]` (`:102-123`). `BEmailAddressFE` (`email:EmailAddress`) edits
`personal` + `address`, validating via `BEmailAddress.DEFAULT.validate()` `[CERT]`
(`BEmailAddressFE.java:55-68`, `:94`). `BEmailAddressListFE` (`email:EmailAddressList`) is a mini-table with
Add/Delete commands, auto-seeds one empty row, and disables Delete at one remaining row `[CERT]`
(`BEmailAddressListFE.java:48-51`, `:75-79`).

## 331.5 — `BHxEmailFE`: the legacy HTML parallel

`BHxEmailFE extends BHxFieldEditor`, `@NiagaraSingleton`, `@AgentOn(types={"email:EmailFE"})` — registered on the
WB editor TYPE, the Hx adapter pattern `[CERT]` (`BHxEmailFE.java:19-26`). "Hx" is the legacy server-side HTML
property viewer (pre-Bajaux) `[INFER]` (corpus web thread). Its `write()` emits a `<table>` of From/To/Cc/Bcc/
Subject `<input>`s + a Body `<textarea>` — a flattened parallel of `BEmailFE` for the browser `[CERT]`
(`:37-85`); `save()` reads the form values and calls the same setters `[CERT]` (`:98-107`). Address LISTS are
flattened to a single string field in Hx (no add/remove rows) `[CERT]` `[INFER]`. So email has BOTH a rich WB
editor and a thin Hx editor, the recurring N4 dual-UI pattern `[INFER]`.

## 331.6 — Connections

- [Block 34] §34.6.5 — `BEmailRecipient.emailAccount`; §331.3 is the field editor that fills its dropdown, and
  explains the by-name reference.
- [Block 324] §324.5 / [Block 34] — the only two ways to actually send (no UI test-send, §331.1): the recipient,
  or `BEmailService.send` from a Program.
- [Block 326] §326.5 / [Block 330] §330.2 — the insecure component defaults the Manager's New overrides (§331.2).
- [Block 329] — the dashboard that grades the resulting state regardless of how the account was created.
- [Block 328] — the OAuth ClientSecret authenticator the New wizard selects by default.

## 331.7 — Self-verify

Block TYPE: **evidence**. Delegated sweep **sonnet** (7 files, 753 lines); driver spot-checked the two
load-bearing claims: `BOutgoingAccountFE.java:19-20` `loadFromService(...)` (read verbatim) and the ABSENCE of a
test-send (whole-`email-wb` grep for test/send/preview — no affordance), plus the Manager `@AgentOn`/New-types/
OAuth-default at `BEmailAccountManager.java:38-42`, `:201`, `:211-212`. Load-bearing anchors (extern —
token-checked by read): `BEmailAccountManager.java:42`, `:201`, `:211`, `BOutgoingAccountFE.java:20`,
`BEmailFE.java:36`, `BHxEmailFE.java:19`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 19 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 9 |
| INFER/CERT ratio | 0.47 |

`verify-block.sh` exit 0 (citations extern — token-checked by read).

Evidence block: `[INFER]`s are the no-test-send commissioning consequence, the secure-wizard-vs-insecure-default
contrast, and the dual-UI pattern — each anchored to a cited `[CERT]`.
