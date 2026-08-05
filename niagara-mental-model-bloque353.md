# Block 353 — `electronicSignature`: the dual-signature / remote path is the RIGOROUS half — asynchronous queued approval, server-enforced second-signer eligibility, hard self-approval block, real email notify — over a Fox supervisor↔JACE transport

> Focus **electronicSignature** — gap **ES3** (dual-signature / remote transport), also closing the **ES5** residual
> (secondary-signer credential handling). READ-ONLY. Corpus language: ENGLISH.
>
> Question (opened in [B350] §350.7 / §131): how `BSecondaryRemoteAuthentication` QUEUES a second-signature request,
> NOTIFIES the approver (email), PERSISTS it, and how it travels supervisor↔JACE — for 21 CFR Part 11 §11.200(b)
> (a second, distinct signer). Answer: the remote path is ASYNCHRONOUS (queue + later approval), the second signer's
> eligibility is ENFORCED server-side (role intersection with the primary's configured Level-2 role), self-approval is
> HARD-BLOCKED, email notification is genuinely wired (gated off by default), and the transport is Fox to the
> point-owning JACE. This is the module's most rigorous surface — a sharp contrast with the audit-protection gap
> ([B351]) and the unconstrained-reason gap ([B352]).
>
> Sources (primary, N4.14, vendor **TridiumPS**), roots `organized/electronicSignature/electronicSignature-rt/` and
> `organized/electronicSignatureRemote/electronicSignatureRemote-rt/`:
> - `vineflower/com/secured/model/config/BSecondaryRemoteAuthentication.java` — the approver queue/container (STRUCTURE).
> - `vineflower/com/secured/model/config/{BRemoteRequestParameter,BSecureUserMixIn}.java` — request filter + Level-2 role (STRUCTURE).
> - `vineflower/com/tridiumx/ps/model/Helper.java` — addRemoteRequest, verifySecondaryCredentials, the queue map (STRUCTURE).
> - `vineflower/com/secured/email/BEmailConfiguration.java` — approver email (imports + call graph).
> - `extracted/com/tridiumx/ps/model/Helper.class`, `extracted/com/secured/email/BEmailConfiguration.class` — **bytecode** (`javap`/`strings`): self-approval throw, Base64, email refs. All bytecode citations token-verified by the driver.
> - `extracted/META-INF/module.xml` — deps (`email-rt`, `fox-rt`, `niagaraDriver-rt`) + `BEmailConfiguration` type.
>
> Markers: `[CERT]` local primary — `javap` method@offset / bytecode string / vineflower `file:line` (extern, token-verified) · `[INFER]` deduction.
> Layer 22 (license/security) + compliance axis. Block TYPE: **evidence** (security). Builds on [B350] (identity), [B352] (the pipeline whose `getIsSecondaryRemote`/`processSecondary`/`isSecondaryApproved` branches this block explains).
>
> ⚠ **OBFUSCATION CAVEAT ([B350] header).** Decompiled `.java` is string-scrubbed; string-dependent claims (email usage,
> the self-approval message, role guards) are taken from **bytecode strings** / **compiled class-ref imports** (which
> survive scrubbing), never from decompiled literals. Structure/call-graph from vineflower.

---

## 353.1 — Two dual-signature modes: local synchronous, remote asynchronous

The [B352] pipeline branches on `getIsSecondaryRemote()` / `getProcessSecondary()` / `getIsSecondaryApproved()`. Those
select between two modes:

- **LOCAL dual-signature (synchronous).** Both signers' credentials are present in ONE action invocation; the pipeline
  verifies primary then secondary and writes in a single `performAction`. Network-atomic. `[INFER, from the single-action
  presence of both credentials in the [B352] trace]`.
- **REMOTE dual-signature (asynchronous).** When `isSecondaryRemote=true, processSecondary=false`, `performAction` does
  NOT write — it QUEUES the request via `Helper.addRemoteRequest(...)` and returns. A second signer submits a SEPARATE
  invocation later with `processSecondary=true, isSecondaryApproved=true/false`, which drives the real write (or audits a
  rejection). `[CERT]` (`Helper.java` addRemoteRequest at :1604-1621; the [B352] branch structure).

The remote mode is the Part 11 witness/approver workflow: sign now, a qualified second person approves later.

## 353.2 — The queue: `BSecondaryRemoteAuthentication`, a station-singleton container; requests in memory or in the `.bog`

`BSecondaryRemoteAuthentication extends BComponent` `[CERT]` (`BSecondaryRemoteAuthentication.java:125`), a singleton under
`BSecuredDashboardConfiguration`. Relevant slots `[CERT]` (`:123-124`): properties `status`, `faultCause`, `loginUserName`,
`shouldStoreStationRemoteRequestInBogFile` (boolean, default **false**), `refreshInterval` (RelTime 30s); actions
(flags 260 = operator+hidden) `processRemoteRequest`, `getRemoteRequests`, `hasPermission`, and **`resetRequests`**.

Pending requests are `ISecureParameter` components (`BSecureDouble`/`BSecureBoolean`/…) carrying primary signer, reason,
point ORD/handle, action name, old/new value, `requestTime`, a `uniqueHash` for dedup, and the secondary/pending flags.
Two persistence modes `[CERT]` (`Helper.java`):
- `shouldStoreStationRemoteRequestInBogFile=true` → the request is `.add(name, val)`'d as a **dynamic child component**,
  persisted in the station `.bog`.
- default `false` → stored in a **static in-memory `Map<String, ArrayList<BComponent>> remoteRequests`** (`Helper.java:146`),
  keyed by component handle. **Not persisted — lost on station restart** `[CERT]` field + `[INFER, from in-memory Map
  semantics]`. Operationally: by default, a pending Part 11 approval evaporates silently on a reboot; the primary must
  re-sign. Choosing bog persistence trades that for a request (with its metadata) living in the config file.

**Password hygiene (a genuine security touch).** Before a request is queued, both credential fields are zeroed:
`setPrimaryPassword("")` + `setSecondaryPassword("")` `[CERT]` (`Helper.java:1604-1605`), and `resetRequests` re-zeroes
passwords across all pending requests. So the QUEUE never holds the signer passwords — unlike the in-flight action
parameter, which does carry them Base64-encoded ([B352] §352.2). This meaningfully limits the Base64 exposure window to
the live invocation, not the persisted/queued request.

## 353.3 — Second-signer authentication: identical Base64+LDAP/local path, PLUS three enforced guards (closes ES5)

`verifySecondaryCredentials` reuses the primary mechanism and adds enforcement. Verified in bytecode `[CERT]`
(`javap Helper.class verifySecondaryCredentials`): **Base64.getDecoder().decode @140/@144** (the second signer's password
ALSO travels reversible Base64 — closes the **ES5** residual: parity with the primary path, same exposure), then
`instanceof BLdapAuthenticationScheme → verifyLdapCredential @213/@223` else `BPasswordCache.validate`. On top of the
shared auth, three guards fire, each a hard throw confirmed by its bytecode message string `[CERT]` (`strings Helper.class`):
1. **Self-approval block** — `primaryUserName.contentEquals(userName)` (@107) → throw *"…Please provide Primary user and
   secondary user that are different."* The same person cannot be both signers. §11.200(b)'s "distinct" requirement is
   ENFORCED, not advisory.
2. **Primary-has-configured-secondaries** — throw *"Primary user do not have configured secondary users."*
3. **Secondary-is-authorized** — throw *"Secondary user is not authorized to perform 2nd level authentication."*

(Plus *"Please configure secondary authenticator role for primary user."* when the primary has no Level-2 role set.)

This is the module's most careful code path: the second signer is authenticated with the same rigor as the first,
provably cannot be the same user, and must be pre-authorized.

## 353.4 — Eligibility is real: `BSecureUserMixIn` Level-2 role, checked against the PRIMARY user's configuration

`BSecureUserMixIn` is a mixin agent on `baja:User` `[CERT]` (`@AgentOn(types={"baja:User"})`, implements `BIMixIn`,
`BSecureUserMixIn.java:36`). It adds ONE property, `level2AuthenticatorRole` (String, with `RoleFE`/`RolesEditor` field
editors) `[CERT]` (`:36-64`). Its display name string `"E-Signature Role Configuration"` survives scrubbing (direct return).

The enforcement is asymmetric and deliberate `[CERT]` (`Helper.java` verify path, ~:1119-1129): the check reads the
**PRIMARY** user's `level2AuthenticatorRole`, then requires the **secondary** user to hold at least one of those roles.
The same lookup drives `getSecondaryUsers` (who may approve for this primary) and the email recipient list (§353.5). So a
station configures, per primary user, WHICH role a second signer must have — and the server verifies it at approval time.
The secondary user's own mixin is read only for display, never for the gate `[CERT]` (`getSecondaryUserDetails`). Second-
signer eligibility is therefore a genuine RBAC control, not a UI hint.

## 353.5 — Email notification is genuinely wired (RE-MEASURED), gated off by default

The `email-rt` dependency is not vestigial — it is USED. Re-measured by three independent methods `[CERT]`:
1. **Compiled class-refs** (survive scrubbing): `BEmailConfiguration.java:12-17` imports `javax.baja.email.{BEmail,
   BEmailAddress, BEmailAddressList, BEmailPart, BOutgoingAccount, BTextPart}`; `module.xml:11` declares `email-rt` and
   `:27` registers `com.secured.email.BEmailConfiguration`.
2. **Call sites**: `Helper.addRemoteRequest` calls `getDashBoardConfiguration().getEmailConfiguration().createAndSendEmail(
   remoteRequest)` at all three queue branches (`Helper.java:1610/1616/1621`) — i.e. queueing a request triggers the notify.
3. **Bytecode** (`strings BEmailConfiguration.class`): `shouldSendRemoteRequestEmails`, `outgoingAccount`, and a method
   returning `javax/baja/email/BEmail`, a `BOutgoingAccount` field.

`createAndSendEmail` is guarded by `shouldSendRemoteRequestEmails` (default **false**) and requires a configured,
operational `outgoingAccount` `[CERT]` (`BEmailConfiguration.java` structure). Recipients are computed from the primary's
Level-2 role: all users holding that role with a non-empty `getEmail()` `[CERT]` (`getReceivers`). Template tokens
`[pointName]`, `[userName]`, `[requestTime]`. So the module can email every eligible approver when a signature is pending —
but only if an operator turns it on and wires an SMTP account. (Ties to the corpus email focus: this rides `BOutgoingAccount`
from [B324-B334], not a private SMTP.)

## 353.6 — Remote transport: a nominal Fox subtype, polling the point-owning JACE

`electronicSignatureRemote-rt` contributes one type, `BRemoteSecureNiagaraProxyExt extends BSecureNiagaraProxyExt`
`[CERT]` ([B350] §350.5) — a NOMINAL subtype (its own `TYPE`/Logger, effectively no new behavior) `[INFER, sub-agent
structural read; all logic in the base]`. The transport is **Fox** (deps `fox-rt` + `niagaraDriver-rt`, `module.xml:14,18`
`[CERT]`): the supervisor's `BSecondaryRemoteAuthentication` discovers a JACE's queue component via a BQL query over Fox and
`invoke("…","getRemoteRequests",param)` on the JACE, tagging each returned request with `remoteJace=<stationName>`
`[CERT]` (`BSecondaryRemoteAuthentication.java` getJACERemoteRequest/getRemoteOrd, ~:419-483, STRUCTURE).

**Persistence topology** `[CERT]`/`[INFER]`: the AUTHORITATIVE queue lives on the station that OWNS the point (the JACE for
a JACE point; the supervisor for a supervisor point). The supervisor holds only a read-only fetched COPY (tagged
`remoteJace`) for the approver's review; the actual approval is routed back to the owning JACE via the Fox proxy ext, where
`performAction` executes and `processRemoteRequest` removes it from the JACE's queue. A JACE that keeps its queue in memory
(the default) therefore loses pending approvals on ITS own reboot, independent of the supervisor.

## 353.7 — Where this leaves the module's Part 11 posture

Across ES2/ES4/ES3, the module is **strong at the signing ceremony and weak at the record**:
- **Strong (this block):** distinct second signer enforced, eligibility by role enforced server-side, credentials verified
  against real auth stores, passwords zeroed out of the persisted queue, approver notification available.
- **Weak (prior blocks):** the reason is only non-empty, not from the approved set ([B352] §352.4); the license is a
  cached flag ([B352] §352.5); a wrong-password attempt is not written to the secured history ([B352] §352.3); and the
  audit trail itself is plaintext and purgeable without a signature ([B351]). 

The dual-signer front door is bolted; the audit-record back room is not. For a §11.200(b) claim the flow is defensible;
for §11.10(e) (protected audit trail) it is not.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Remote dual-sig is ASYNC: queue then later approval (processSecondary=false→addRemoteRequest, no write) | `[CERT]` | `Helper.java:1604-1621`; [B352] branch trace | ✅ read |
| 2 | `BSecondaryRemoteAuthentication extends BComponent`, singleton; slots incl. resetRequests, shouldStore…BogFile | `[CERT]` | `BSecondaryRemoteAuthentication.java:123-125` | ✅ read |
| 3 | Queue persistence: in-memory `Map remoteRequests` (default) or `.bog` child (flag) | `[CERT]` | `Helper.java:146`; flag `:123` | ✅ read |
| 4 | Passwords zeroed before queueing | `[CERT]` | `Helper.java:1604-1605` | ✅ read |
| 5 | Secondary credential ALSO Base64 + LDAP/BPasswordCache (closes ES5) | `[CERT]` | `javap Helper.class verifySecondaryCredentials` @140,@144,@213,@223 | ✅ ran |
| 6 | Self-approval hard-blocked (contentEquals→throw "…different") | `[CERT]` | `javap` @107; `strings Helper.class` | ✅ ran |
| 7 | Two more enforced guards: primary-has-secondaries; secondary-authorized | `[CERT]` | `strings Helper.class` (message literals) | ✅ ran |
| 8 | `BSecureUserMixIn` = agent on baja:User, adds `level2AuthenticatorRole` | `[CERT]` | `BSecureUserMixIn.java:36-64` | ✅ read |
| 9 | Eligibility check uses PRIMARY's role, requires SECONDARY to hold it (enforced) | `[CERT]` | `Helper.java:~1119-1129` | ✅ read |
| 10 | Email wired: imports javax.baja.email + 3 createAndSendEmail call sites + bytecode refs | `[CERT]` | `BEmailConfiguration.java:12-17`; `Helper.java:1610/1616/1621`; `strings BEmailConfiguration.class` | ✅ ran (RE-MEASURE ×3) |
| 11 | Email gated by `shouldSendRemoteRequestEmails`=false default; recipients by role+email | `[CERT]` | `BEmailConfiguration.java` structure; `module.xml:11,27` | ✅ read |
| 12 | Transport = Fox (deps fox-rt, niagaraDriver-rt); supervisor polls JACE getRemoteRequests, tags remoteJace | `[CERT]` | `module.xml:14,18`; `BSecondaryRemoteAuthentication.java:~419-483` | ✅ read |
| 13 | `BRemoteSecureNiagaraProxyExt extends BSecureNiagaraProxyExt`, nominal subtype | `[CERT]`/`[INFER]` | [B350] §350.5; sub-agent structural read | ⚠ subtype identity CERT, "no overrides" INFER |
| 14 | Default in-memory queue ⇒ pending approvals lost on reboot | `[INFER]` | from claim 3 | ⚠ deduction |
| 15 | Local dual-sig is synchronous (both creds in one action) | `[INFER]` | from [B352] single-action trace | ⚠ deduction |

Marker tally: `[CERT]` ×12 · `[INFER]` ×3 (+1 mixed). [INFER]/[CERT] = 3/12 = 0.25 — healthy evidence block; every
enforcement mechanism is `[CERT]` from bytecode/structure I re-ran, `[INFER]`s are operational consequences.

RE-MEASURE applied to the email claim (a dramatic "the dependency is actually used" positive): confirmed by THREE
independent methods (compiled class-ref imports, call sites, bytecode strings) per the RE-MEASURE-A-DRAMATIC rule.
Framework-semantic check on the delegated eligibility/self-approval claims: re-ran `javap`/`strings` myself; the guard
message literals in the constant pool confirm server-side enforcement (not UI-only).

## Connections

- [Block 350] — ES1: `BSecondaryRemoteAuthentication`, `BSecureUserMixIn`, `BRemoteSecureNiagaraProxyExt`, `email-rt` dep, §11.200(b) mapping (§350.4-350.5).
- [Block 352] — ES2: the pipeline whose `getIsSecondaryRemote`/`processSecondary`/`isSecondaryApproved` branches this block explains; the Base64 credential finding this block extends to the secondary signer (closing ES5).
- [Block 351] — ES4: the audit back-end; §353.7 contrasts the enforced dual-signer front door with the unprotected audit trail.
- [Block 324-334] — the `email` service focus: `BOutgoingAccount` is the SMTP substrate this module's approver-notify rides on.

## Open gaps after this block

- ES5 (credential handling) — **CLOSED** here by remittance: §353.3 confirms the secondary path is Base64+LDAP/BPasswordCache identical to primary, with self-approval + role enforcement added. No separate block needed.
- ES6 (ux/wb layers — web editors, Swing credential dialog, PX/Hx bindings) and ES7 (mutable ESignAcknowledgement vs baked lexicon) remain queued. ES4-G1 (live-permission reachability) still requires-execution.
