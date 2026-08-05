# Block 350 — `electronicSignature`: the TridiumPS add-on that makes a Niagara point require a signed, re-authenticated, reason-bearing, dual-approved write — the 21 CFR Part 11 module the corpus never had

> Focus **electronicSignature** — first evidence block (ES1). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: identity, origin, the registered type system, the license gate, and the 21 CFR Part 11 mapping of the
> `electronicSignature` module (+ its companion `electronicSignatureRemote`). This is the FOUNDATION block of a
> new focus: it establishes WHAT the module is and HOW its pieces map to Part 11, and defers the method-level
> sign-flow mechanics and the security surfaces to child gaps (§350.7).
>
> Sources (primary, N4.14, module vendor **TridiumPS**), read inline:
> - `organized/electronicSignature/electronicSignature-rt/extracted/META-INF/module.xml` (identity + 39 registered types + deps) — CLEAN resource.
> - `organized/electronicSignature/electronicSignature-{rt,ux}/extracted/*.lexicon` — CLEAN resources (license + certification strings).
> - `organized/electronicSignature/electronicSignature-rt/extracted/com/tridiumx/ps/model/*.class` — bytecode string constants via `rg -a` (feature name).
> - `organized/electronicSignatureRemote/electronicSignatureRemote-rt/` — companion module.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[CERT-doc]` official reg text · `[INFER]` deduction.
> Layer 22 (license/security) + a NEW compliance axis. Block TYPE: **evidence** (foundation).
>
> ⚠ **OBFUSCATION CAVEAT (load-bearing for every later block of this focus).** The `decompiled/` and `vineflower/`
> Java trees of this module are STRING-SCRUBBED: string literals and many internal identifiers render as the token
> `n` / `ln` (e.g. `throw new nException("tridium:n")`, lexicon-in-source `I n that (i)`). The **bytecode** (`.class`)
> and the **`extracted/` resources** (module.xml, `.lexicon`) are INTACT. Therefore this focus cites STRINGS from
> bytecode/resources, never from the decompiled `.java`; decompiled `.java` is usable only for STRUCTURE (class/getter
> inventory, control-flow shape). Any exact string a sub-agent quotes "from the .java" is unverifiable there and was
> re-confirmed here against bytecode/resources. `[CERT]` for this focus = resource/bytecode; method-body mechanics = `[INFER]`.

---

## 350.1 — Identity and origin: Tridium **Professional Services**, not core, not a random third party

`module.xml:2` `[CERT]`: `name="electronicSignature-rt"`, **`vendor="TridiumPS"`**, `vendorVersion="4.14.1.30.11"`,
`preferredSymbol="esign"`, `moduleName="electronicSignature"`, `description="This Module is used to secure object
in Niagara environment."`. **TridiumPS = Tridium Professional Services** — Tridium's custom-engineering arm, not
the core `Tridium` vendor and not an unrelated OEM `[INFER]` (vendor string + `com.tridiumx.ps` = "ps" = professional
services). This resolves the origin question the audit opened: the module is first-party-adjacent (Tridium's own PS
group), which is why it is absent from BOTH the corpus AND `niagara-help` — PS deliverables are not part of the core
help set (`guide-search "21 CFR"|"part 11"|"electronic signature"` = 0, verified this session) `[CERT]`.

Namespace is SPLIT across two package roots `[CERT]` (module.xml types): `com.tridiumx.ps.*` (the secure-point
machinery and parameters) and `com.secured.*` (the config/reasons model, history, alarm, remote-auth). Dependencies
`[CERT]` (module.xml `<dependencies>`) name the subsystems it wires: **`ldap-rt`** (re-authentication),
**`email-rt`** (secondary-approver notification), **`history-rt`** (the audit trail), **`exportTags-rt`**,
`fox-rt`, `niagaraVirtual-rt`, `alarm-rt`, `control-rt`, `web-rt`.

## 350.2 — The type system: a point becomes "secured" by SUBSTITUTION, carried by a parameter VO

`module.xml:27-69` registers **39 component types** `[CERT]`. They fall into families:

- **Secured point wrappers** (`com.tridiumx.ps.model`): `BSecuredNumericWritable`, `BSecuredBooleanWritable`,
  `BSecuredEnumWritable`, `BSecuredStringWritable` and their read-only `*Point` twins `[CERT]` (module.xml:32-39).
  These are drop-in replacements for the stock `BNumericWritable` etc.: the module secures a point by REPLACING its
  type, not by attaching an extension `[INFER]` (the writable types are peers of the stock control points).
- **Secure parameter value-objects** (`com.tridiumx.ps.model.parameters`): `BSecureDouble`, `BSecureBoolean`,
  `BSecureEnum`, `BSecureString`, `BSecureAuto`, `BSecure{Numeric,Boolean,Enum,String}Override`,
  `BSecureActiveInactive`, `BSecureFacet`, `BSecureCall` `[CERT]` (module.xml:42-53). These are the carriers of the
  in-flight signature data (user, password, reason, comments, old/new value) passed INTO the signed action `[INFER]`.
- **Config / reasons model** (`com.secured.model.config`): `BCustomer`, `BReasons`, `BReasonSet`, `BZone`,
  and `BSecureUserMixIn` — an agent ON `baja:User` `[CERT]` (module.xml:56 has child slots) that adds a
  **Level-2 authenticator role** to each user (who may act as a SECOND signer) `[INFER]`.
- **Dual/remote authentication**: `BSecondaryRemoteAuthentication`, `BRemoteRequestParameter` `[CERT]` (module.xml:61-62).
- **Central config**: `BSecuredDashboardConfiguration` (the singleton hub) `[CERT]` (module.xml:40).
- **Audit + alarm**: `BSecuredTrendRecord`, `BSecuredTrendRecordForAlarm`, `BProtectedConsoleRecipient`,
  `BHistoryMaintenance` `[CERT]` (module.xml:54-55,68-69).
- **Re-auth plumbing**: `BLDAPAuthenticationJob`, `BLDAPUserNamePasswordCallbackHandler` `[CERT]` (module.xml:41,67).

## 350.3 — The license gate: feature **`tridium:eSignature`** with a point-count limit

The module is a LICENSED feature, gate confirmed from **bytecode** (obfuscation-proof) `[CERT]`: the string constant
`tridium:eSignature` appears in every secured-point `.class` (`BSecuredNumericWritable.class`,
`BSecuredBooleanPoint.class`, …) and `point.limit` + `eSignature` appear in `BSecuredDashboardConfiguration.class`
`[CERT]` (`rg -a` over `extracted/com/tridiumx/ps/model/*.class`). The lexicon carries the operator-facing failure
messages `[CERT]` (`electronicSignature-rt.lexicon:73,85,86`): `license.pointCount.exceeded=Secured point count
exceeded.`, `license.expired.message=License feature is expired.`, `license.missing.message=License feature is
missing.`. So the gate has the same SHAPE as jsonToolkit [Block 335] / email [Block 324]: a Tridium license feature
(`tridium:eSignature`) plus a per-license **point-count limit** (`point.limit`) — exceed the count or lose the
feature and the secured write is refused `[INFER]` (the exact enforcement path is a child gap, §350.7, because it
lives in scrubbed method bodies).

## 350.4 — The 21 CFR Part 11 mapping: the signature is a re-authenticated action with meaning and a witness

The module is a mechanical implementation of FDA 21 CFR Part 11 (electronic records / electronic signatures). Each
Part 11 obligation maps to a concrete, cited artifact:

| Part 11 requirement | Artifact (this module) | Citation |
|---|---|---|
| §11.100(c) — signature is legally binding, equivalent to handwritten | The certification statement the operator acknowledges: *"I hereby certify that (i) I am the user […] and (iii) it is my intent that the above electronic signature be my legally binding signature just as if it were my handwritten signature."* | `electronicSignature-ux.lexicon:262-264` `[CERT-doc]` |
| §11.200(a)(1)(i) — re-authentication at the moment of signing | dependency `ldap-rt` + `BLDAPAuthenticationJob` / `BLDAPUserNamePasswordCallbackHandler` (JAAS re-login) for LDAP users; local password re-entry otherwise | module.xml:41,67 `[CERT]`; flow `[INFER]` |
| §11.50(a)(3) — the MEANING/reason of the signature | `BReasons` / `BReasonSet` pre-configured reason sets; reason-for-change is a mandatory action parameter | module.xml:29-30 `[CERT]`; enforcement `[INFER]` |
| §11.200(b) — a second, distinct signer (witness/approver) | `BSecondaryRemoteAuthentication` + `BSecureUserMixIn`'s Level-2 authenticator role | module.xml:56,61 `[CERT]`; flow `[INFER]` |
| §11.10(e) — a protected, computer-generated audit trail | `BSecuredTrendRecord` written to a dedicated history; fields (clean getters, decompiled): oldValue, value, primarySigner, secondarySigner, reason, pointName, oldStatus, status, primarySignerComments, secondarySignerComments, operation, remoteAuthenticationAction, fullNamePrimarySigner, fullNameSecondarySigner | module.xml:54 `[CERT]`; `BSecuredTrendRecord.java` getter list `[CERT]` |

The action surface that enforces this is the **`*WithAuthentication` action set** `[CERT]`
(`electronicSignature-rt.lexicon:95-104`): `setWithAuthentication`, `overrideWithAuthentication`,
`autoWithAuthentication`, `emergencyOverrideWithAuthentication`, `emergencyAutoWithAuthentication`,
`active/inactiveWithAuthentication`, `changeFacetsWithAuthentication`. Every stock write verb has a signed twin —
the secured point exposes ONLY the signed twins and makes the bare verb throw `[INFER]` (the "requires a digital
signature — use the With Authentication action instead" message exists but only in scrubbed source; the lexicon twin
set is the clean evidence).

## 350.5 — `electronicSignatureRemote`: one type, for the subordinate JACE

The companion module `electronicSignatureRemote-rt` contributes essentially ONE own type,
`BRemoteSecureNiagaraProxyExt extends BSecureNiagaraProxyExt` `[CERT]` (its `-rt` tree). It is a thin subtype:
its purpose is to give a distinct registered type identity for secured proxy points living on a SUBORDINATE station,
so the dual-signature request can travel supervisor↔JACE over Fox while the config hub
(`BSecuredDashboardConfiguration`) and the approver queue (`BSecondaryRemoteAuthentication`) live on the supervisor
`[INFER]` (the remote transport itself is in the main module, a child gap). This is the same supervisor/subordinate
split the corpus documented for the network join [Block 266].

## 350.6 — Why this is three DIFFERENT "signatures" (disambiguation, corpus hygiene)

This focus closes a naming confusion worth recording once, because the corpus now holds three unrelated "signing"
subsystems that are easy to conflate:

1. **Module code-signing** — signing the `.jar` so a station will load it ([Block 18], [Block 113]; feature/keystore).
2. **`signingService`** — a centralized PKI **certificate**-signing service (a requester uploads a CSR, a central CA
   signs it): documented partially at [Block 27] §27.7.6 (`niagara.signingRequester.*`). NOT electronic signature.
3. **`electronicSignature`** (this block) — 21 CFR Part 11 electronic **signature of an operator on an action**.

They share the English word "sign" and nothing else. `signingService` was mis-identified (by a third party) as the
Part 11 module; the evidence (its lexicon is entirely CSR/CA/signing-profile) refutes that — the Part 11 module is
`electronicSignature`.

## 350.7 — What this block does NOT resolve (child gaps seeded for the focus)

Deferred because they live in scrubbed method bodies (need bytecode-level `javap` reading or dynamic verification):

- **ES2 — the sign flow end-to-end**: the exact order of license-check → credential verify → reason check → super-action
  invoke → audit append inside `SecureHelper.performAction` / `Helper.verify*Credentials` (method names come from the
  sub-agent's structural read of scrubbed source; must be re-derived from bytecode). `[INFER]` today.
- **ES3 — dual-signature / remote transport**: how `BSecondaryRemoteAuthentication` queues, notifies (email), and
  re-invokes the approval; in-memory vs BOG-file persistence of pending requests.
- **ES4 — the audit trail's protection**: is `BSecuredTrendRecord` tamper-evident, and does `BHistoryMaintenance`
  (clear/delete/clearOld actions) let an admin purge the Part 11 audit trail WITHOUT a signature? (audit-flagged
  by the sweep — a potential §11.10(e) gap).
- **ES5 — credential handling**: the sweep reported the password travels Base64-encoded (not encrypted) and a
  hardcoded post-action `Thread.sleep(500)`; both are hypotheses over scrubbed source, to be confirmed from bytecode.
- **ES6 — ux/wb layers**: the web editors (`com.tridiumx.ps.webeditors`) and the Workbench Swing credential dialog +
  PX/Hx bindings (`com.secured.ui.mgr` / `BHxSecureActionBinding`) — how the acknowledgement is shown and captured.
- **ES7 — the mutable `ESignAcknowledgement`**: the sweep flagged the certification text also exists as an
  admin-writable property on `BSecuredDashboardConfiguration` (could diverge from the baked lexicon) — verify.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Vendor is TridiumPS, v4.14.1.30.11, symbol esign | `[CERT]` | module.xml:2 | ✅ token-checked |
| 2 | 39 component types registered; two families (Secured*Writable/Point + Secure* params) | `[CERT]` | module.xml:27-69 | ✅ counted |
| 3 | Namespace split `com.tridiumx.ps.*` + `com.secured.*` | `[CERT]` | module.xml types | ✅ |
| 4 | Deps include ldap-rt, email-rt, history-rt, exportTags-rt | `[CERT]` | module.xml `<dependencies>` | ✅ |
| 5 | License feature is `tridium:eSignature` + `point.limit` | `[CERT]` | bytecode `rg -a` on `*.class` | ✅ bytecode-confirmed |
| 6 | License failure messages (pointCount/expired/missing) | `[CERT]` | electronicSignature-rt.lexicon:73,85,86 | ✅ |
| 7 | §11.100(c) certification statement verbatim | `[CERT-doc]` | electronicSignature-ux.lexicon:262-264 | ✅ verbatim |
| 8 | `*WithAuthentication` action set (10 signed verbs) | `[CERT]` | electronicSignature-rt.lexicon:95-104 | ✅ |
| 9 | Audit record `BSecuredTrendRecord` field schema | `[CERT]` | BSecuredTrendRecord.java (clean getters) | ✅ |
| 10 | Decompiled Java is string-scrubbed; bytecode/resources intact | `[CERT]` | observed: `nException`, `I n that (i)` vs clean `.class`/lexicon | ✅ |
| 11 | electronicSignatureRemote = one thin subtype BRemoteSecureNiagaraProxyExt | `[CERT]` | electronicSignatureRemote-rt | ✅ |
| 12 | Sign-flow method order, base64 password, 500ms sleep, purge-without-sig | `[INFER]` | scrubbed source (sub-agent structural read) — DEFERRED ES2/ES4/ES5 | ⚠ hypothesis |

Tally: `[CERT]` ×10 · `[CERT-doc]` ×1 · `[INFER]` ×1 load-bearing (+ scoped inline INFER on flow). [INFER]/[CERT]
ratio ≈ 0.09 for the foundation claims — LOW, as expected for an evidence/foundation block resting on clean
resources + bytecode. The flow mechanics are honestly parked as `[INFER]`/child gaps, not smuggled in as `[CERT]`.

## Connections

- **License-gate shape** mirrors [Block 335] (jsonToolkit: `tridium:jsonToolkit` + point limit) and [Block 324]
  (email: `tridium:email`) — a third instance of the same Tridium feature + count-limit pattern.
- **Module code-signing** ([Block 18], [Block 113]) and **`signingService`/PKI** ([Block 27] §27.7.6) are the two
  OTHER "signing" subsystems disambiguated in §350.6.
- **Supervisor↔subordinate split** ([Block 266]) is the pattern `electronicSignatureRemote` follows.
- **Audit trail** ties to `history-rt` (the corpus's history-domain blocks) — the Part 11 record is a Niagara history.

## Open gaps (registered to RESEARCH-STATE-electronicSignature)

ES2 sign-flow · ES3 dual/remote transport · ES4 audit-trail protection + purge · ES5 credential handling ·
ES6 ux/wb layers · ES7 mutable ESignAcknowledgement. All investigable (source present, obfuscation notwithstanding —
bytecode readable). None blocked-on-tool.
