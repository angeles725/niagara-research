# Block 354 — `electronicSignature`: the UI layer — the browser `btoa()` closes the Base64 loop, the signing dialog is a compliance FORM, and enforcement lives in the type (no plain `set`) + a PREFERRED menu agent that strips the raw `call` verb

> Focus **electronicSignature** — gap **ES6** (ux/wb layers). READ-ONLY. Corpus language: ENGLISH.
>
> Question (opened in [B350] §350.7): the browser bajaux field editors, the Workbench Swing managers/dialogs/profile,
> and — the loose end from [B352]/[B353] — WHERE the signing password is Base64-encoded (the server decodes it; who
> encodes?). Answer: the encode is the browser's native `btoa()` in `SecuredParentEditor.js`/`Util.js`; the wire form is
> `username;base64(password)`. The UI is a compliance FORM over server enforcement — the real gate is that the
> substituted `BSecured*Writable` TYPE exposes no plain `set`/`override`/`auto`, only `*WithAuthentication` verbs plus a
> generic `call` dispatcher that a PREFERRED menu agent strips from view and a view-profile hides WireSheet/SlotSheet.
>
> Sources (primary, N4.14, vendor **TridiumPS**), roots `.../electronicSignature-ux/` and `.../electronicSignature-wb/`:
> - `ux/vineflower/rc/fe/baja/{SecuredParentEditor,Util}.js` — the browser signing dialog + credential encode (JS STRUCTURE).
> - `ux/vineflower/com/tridiumx/ps/webeditors/BSecured*Editor.java`, `BSecuredComponentMenuAgentEditor.java`, `BRemoteAuthenticationView.java` — bajaux agents (STRUCTURE).
> - `wb/vineflower/com/secured/ui/mgr/**` — Swing managers/dialogs/profiles/commands (STRUCTURE).
> - `rt/vineflower/com/tridiumx/ps/model/BSecuredNumericWritable.java:156-168` — the action slot set (STRUCTURE, the enforcement anchor).
>
> Markers: `[CERT]` local primary (`file:line`; JS identifiers/`btoa`/`newAction` flags are NOT scrubbed) · `[INFER]` deduction.
> Layer 22 (license/security) + compliance axis. Block TYPE: **evidence** (security/UI). Builds on [B350] (type substitution), [B352] (server Base64-decode), [B353] (approver queue).
>
> ⚠ **OBFUSCATION CAVEAT ([B350] header).** Both `.java` AND `.js` are string-scrubbed (literals → `l`/`n`). This block
> relies on JS/Java STRUCTURE and non-string identifiers (`btoa`, `newAction` flag ints, `@AgentOn` types, method names),
> which the scrubber leaves intact; it does NOT cite scrubbed string literals. DOM-id claims are structural (the ids are
> code identifiers in the JS, not lexicon strings).

---

## 354.1 — WHERE the Base64 comes from: the browser's `btoa()` (loop closed)

The signing password is Base64-encoded CLIENT-SIDE by the native browser `btoa()`, then shipped as one string. Confirmed
at multiple call sites `[CERT]`:
- `SecuredParentEditor.js:235` (secondary sign) and `:372` (primary sign):
  `Util.authenticateUser(comp, baja.$("baja:String").make(credentials.username + ";" + btoa(credentials.password)))`.
- `SecuredParentEditor.js:810/817` `newComp.setPrimaryPassword(btoa(dom.find('#password').val()))` and `:818`
  `setSecondaryPassword(btoa(...#secondaryPassword...))` — the queued-request path ([B353]) also encodes here.
- `Util.js:349/416` (primary/secondary sign helpers) and `:825/954` (alarm-console sign) — same `btoa(credentials.password)`.

The credential travels to the server as the `baja:String` **`username;base64(password)`**, submitted through the
`authenticateUser` action whose wire command is `"authenticateUser," + credentials.toString()` `[CERT]` (`Util.js:120,158`).
The server's `Base64.getDecoder().decode` ([B352] §352.2) consumes exactly this `btoa()` output. **Loop closed:** encode =
browser `btoa` (reversible), decode = server `Base64` — the signing credential is reversible-encoded end to end, never
hashed on the wire. TLS on the Fox/HTTP channel is the only confidentiality layer; a browser-devtools observer or any
interception at the endpoint recovers the cleartext password by `atob()`.

## 354.2 — The signing dialog: one shared editor, one DOM form for both signers

Every secured field editor (`BSecuredNumericEditor`, `…Boolean`, `…Enum`, `…String`, `…ActiveInactive`, `…Auto`, `…Facet`,
and the `*OverrideEditor` variants) is a thin JS-delivery agent: `extends BSingleton implements BIJavaScript,
BIFormFactorMini`, `@AgentOn` the PARAMETER type (`electronicSignature:SecureDouble` etc.), delivering
`module://electronicSignature/rc/fe/baja/Secured*Editor.js` `[CERT]` (`BSecuredNumericEditor.java:18-21`). The JS prototype
chain is `Secured*Editor → SecuredParentEditor → BaseEditor (webEditors)` `[CERT]` (`SecuredParentEditor.js:56`,
`SecuredNumericEditor.js:63`).

`SecuredParentEditor` owns the shared dialog DOM `[CERT]` (structural DOM ids in the JS): a PRIMARY tab
(`#easPrimarySignature`) with `#username` (pre-filled `baja.getUserName()`), `#password`, `#idReason` (+`#idTextarea` for
"Other"), `#primaryComment`, `#btnPrimarySign`, `#legaltext` (the §11.100(c) legal acknowledgement paragraph), and a
SECONDARY tab (`#easSecondarySignature`) shown only when a two-level flow is required, with `#secondaryUsername`
(dropdown), `#secondaryPassword`, `#secondaryComments`, and the remote-request controls (`.btnRemote`/`.esSend`). This is
the operator's whole Part 11 experience: name, password, reason, comment, legal text — one form, server-verified.

## 354.3 — The real enforcement is the TYPE: no plain write verb exists

The compliance guarantee is not the dialog; it is that the substituted writable exposes NO unauthenticated write. The stock
`BNumericWritable` verbs (`set`, `override`, `auto`, `emergencyOverride`, `emergencyAuto`) are ABSENT from
`BSecuredNumericWritable`; in their place `[CERT]` (`BSecuredNumericWritable.java:156-168`):
- `setWithAuthentication`, `autoWithAuthentication`, `overrideWithAuthentication` — `newAction(256, …)` (operator-flag).
- `emergencyOverrideWithAuthentication`, `emergencyAutoWithAuthentication`, `changeFacetsWithAuthentication`, `logFailCase` — `newAction(0, …)`.
- `authenticateUser`, `getSecondaryUsers`, `getReasonList`, `getCustomerName`, `getSecondaryUserDetails` — `newAction(4, …)` (helper RPCs for the dialog).
- `call` — `newAction(2304, …)`: the GENERIC dispatcher the JS routes credentials through.

There is **no plain `set`/`override`/`auto`** `[CERT]` (absent from the slot list). So even a scripted caller cannot write
the point without going through a `*WithAuthentication` verb (credential-verified server-side, [B352]) or the `call`
dispatcher (which carries the same `authenticateUser,username;base64pw` payload and hits the same verification). The type
substitution — not the UI — is what removes the unauthenticated path. This corrects any reading that "the UI enforces
signing": the UI is convenience; the TYPE is enforcement.

## 354.4 — The UI reinforces it: a PREFERRED menu agent strips the raw `call`, a profile hides the wire/slot sheets

Two UI mechanisms make the enforced path the ONLY visible one:
1. **Menu substitution.** `BSecuredComponentMenuAgentEditor` (ux) and `BSecuredComponentMenuAgent extends BNavMenuAgent`
   (wb) register `@AgentOn(defaultAgent = Preference.PREFERRED, types = {SecuredBooleanWritable, SecuredNumericWritable,
   …})` `[CERT]` (`BSecuredComponentMenuAgentEditor.java:18-34`; `BSecuredComponentMenuAgent.java:84`), overriding the stock
   `webEditors` menu for all secured types. The wb agent explicitly removes the raw `call` verb from the Actions submenu
   `[CERT]` (`BSecuredComponentMenuAgent.java:140-141`: filter `getLabel().toLowerCase().contentEquals("call")` →
   `actions.remove(...)`), and the ux JS swaps in `SecuredDelete/Rename/SetDisplayName` commands and disables
   Cut/Copy/Duplicate for secured types. So an operator never sees a way to invoke `call` directly.
2. **View profiles.** `BESProfile extends BWbProfile` (Workbench) and `BWebESProfile extends BDefaultWbWebProfile` (web
   Workbench) override `hasView(...)` to return `false` for `wiresheet:WireSheet` and `workbench:SlotSheet` on
   `BSecured*Writable` targets `[CERT]` (`BESProfile.java:23`, `BWebESProfile.java:22`, sub-agent structural read). They
   are VIEW-filter profiles (not login profiles): an operator assigned this profile cannot open the wire sheet or slot
   sheet on a secured point, closing the "edit the slot directly" escape from the engineering UI.

Neither is cryptographic — a BajaScript-console user could still assemble `component.invoke({slot:"setWithAuthentication",
value:<param>})` — but that STILL requires valid credentials the server checks. The UI removes the unauthenticated-looking
paths; it never weakens the server gate `[INFER, from §354.3 type + §352 server verify]`.

## 354.5 — The rest of the UI surface: managers, approver views, protected consoles

- **Config managers (wb Swing):** `BReasonsConfigurationManager`, `BReasonSetConfigurationManager`,
  `BSecuredDashboardConfigurationManager`, `BZoneConfigurationManager`, over a generic `BCustomMgrEditDialog` `[CERT]`
  (package structure). These edit the reason sets, zones, and the dashboard/history config — Part 11 setup, not signing.
- **Approver review (both profiles):** ux `BRemoteAuthenticationView extends BSingleton` and wb `BSecondaryRemoteWbView
  extends BWbView`, both `@AgentOn("electronicSignature:SecondaryRemoteAuthentication", requiredPermissions="ri")` with
  `approve/rejectRemoteSecondaryRequest` actions `[CERT]` (class headers). They render the [B353] pending queue, and on
  Approve open the SAME typed editor dialog — so the approver's password also flows through the §354.1 `btoa` path. Both
  gate on `remoteAuthentication.hasPermission(componentHandle;manager;actionName;user)` before showing a request.
- **Protected consoles/maintenance (wb):** `BProtectedWbAlarmConsole`, `BProtectedAlarmDetailsDialog`,
  `BProtectedDatabaseMaintenanceView` — the Swing twins of the [B351] UX maintenance views; same "guard is a view, not the
  action layer" caveat from [B351] §351.4 applies.
- **Secured commands (wb):** `DeleteCommand`/`RenameCommand`/`SetDisplayNameCommand` intercept structural edits on secured
  points, present the ES auth dialog via `BUIUtil.showPopUp()`, then `updateHistory` on the dashboard config `[CERT]`
  (`DeleteCommand.java:46-126`) — so even deleting/renaming a secured point is signed and audited (a nice contrast with
  the UNSIGNED history purge of [B351] §351.3: editing the point is gated, purging its audit history is not).

## 354.6 — What ES6 settles

The UI is a faithful, complete Part 11 signing front-end (dual tabs, legal text, reason, approver flow), and it confirms
two cross-block facts: the Base64 credential originates in the browser `btoa` (§354.1, closing [B352]/[B353]'s open end),
and the enforcement is the TYPE + server, with the UI merely removing unauthenticated-looking affordances (§354.3-354.4).
The module's UI even signs structural edits (delete/rename, §354.5) — which throws the [B351] audit-purge gap into sharper
relief: the product signs the point's every write and rename, yet lets the record of those signatures be cleared without
one.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Password Base64-encoded client-side by `btoa`, wire form `username;base64(pw)` | `[CERT]` | `SecuredParentEditor.js:235,372,810,817,818`; `Util.js:349,416,825,954` | ✅ read |
| 2 | Credential submitted via `authenticateUser,` command through the action | `[CERT]` | `Util.js:120,158` | ✅ read |
| 3 | Editors: `BSingleton`+`BIJavaScript`+`BIFormFactorMini`, `@AgentOn` param type, deliver Secured*Editor.js | `[CERT]` | `BSecuredNumericEditor.java:18-21` | ✅ read |
| 4 | JS chain Secured*Editor → SecuredParentEditor → BaseEditor | `[CERT]` | `SecuredParentEditor.js:56`; `SecuredNumericEditor.js:63` | ✅ read |
| 5 | Dialog DOM: primary/secondary tabs, username/password/reason/comment/legaltext ids | `[CERT]` | `SecuredParentEditor.js` (structural ids) | ✅ read |
| 6 | Writable exposes NO plain set/override/auto — only *WithAuthentication + call + helpers | `[CERT]` | `BSecuredNumericWritable.java:156-168` | ✅ read |
| 7 | `call` = newAction(2304), the generic dispatcher | `[CERT]` | `BSecuredNumericWritable.java:168` | ✅ read |
| 8 | Menu agent @AgentOn PREFERRED on secured types; strips raw `call` from Actions menu | `[CERT]` | `BSecuredComponentMenuAgentEditor.java:18-34`; `BSecuredComponentMenuAgent.java:84,140-141` | ✅ read |
| 9 | Profiles BESProfile/BWebESProfile hide WireSheet+SlotSheet on secured types | `[CERT]` | `BESProfile.java:23`; `BWebESProfile.java:22` | ✅ read (structural) |
| 10 | Approver views @AgentOn SecondaryRemoteAuthentication, requiredPermissions "ri", approve/reject actions | `[CERT]` | `BRemoteAuthenticationView.java:18-36`; `BSecondaryRemoteWbView.java:127` | ✅ read |
| 11 | Secured Delete/Rename/SetDisplayName commands sign + updateHistory | `[CERT]` | `DeleteCommand.java:46-126` | ✅ read |
| 12 | UI removes affordances but server/type is the real gate (scripted invoke still needs valid cred) | `[INFER]` | from §354.3 + [B352] | ⚠ deduction |
| 13 | End-to-end credential is reversible-encoded (btoa↔Base64decode), TLS the only confidentiality | `[INFER]` | from §354.1 + [B352] §352.2 | ⚠ deduction |

Marker tally: `[CERT]` ×11 · `[INFER]` ×2. [INFER]/[CERT] = 2/11 = 0.18 — healthy evidence block. The load-bearing
finding (client `btoa` + no-plain-set type) is `[CERT]` from files I read; the `[INFER]`s are the security consequences.

Framework-semantic check: corrected the sweep's "type exposes only *WithAuthentication" to the precise slot list — there
IS a `call` dispatcher and `authenticateUser`/getter RPCs; the load-bearing truth (no plain unauthenticated `set`) holds,
and the `call` verb is what the menu agent explicitly strips. Re-read the JS `btoa` sites and the `newAction` flags myself.

## Connections

- [Block 350] — ES1: type substitution (`BSecured*Writable`), the §11.100(c) legal text (rendered at `#legaltext`).
- [Block 352] — ES2: the server `Base64.getDecoder().decode` this block shows is fed by browser `btoa` (loop closed); the credential-reversibility finding extended to the wire.
- [Block 353] — ES3/ES5: the approver queue these views render; the secondary `btoa` path.
- [Block 351] — ES4: §354.5's SIGNED delete/rename vs the UNSIGNED audit purge — the sharpest statement of the module's front-strong/back-weak asymmetry.
- webEditors `BaseEditor` / bajaux — the field-editor substrate ([B199], [B204]); `BNavMenuAgent` / `Preference.PREFERRED` menu-agent mechanics.

## Open gaps after this block

- ES7 (mutable `ESignAcknowledgement` property vs baked lexicon — can the legal text at `#legaltext` diverge from the §11.100(c) certification?) — the last investigable gap; NEXT.
- ES4-G1 (live-permission reachability of the audit purge) remains requires-execution.
