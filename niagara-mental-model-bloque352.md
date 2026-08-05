# Block 352 — `electronicSignature`: the signed-write pipeline re-derived from bytecode — license(cached) → credential(Base64-decoded, real LDAP/local auth) → mandatory-but-unconstrained reason → super-action → 500 ms settle → audit; fail-closed, on a 4-thread pool

> Focus **electronicSignature** — gap **ES2** (sign flow end-to-end). READ-ONLY. Corpus language: ENGLISH.
>
> Question (opened in [B350] §350.7 / §128-129): the exact ORDER of license-check → credential verify → reason check →
> super-action invoke → audit append inside `SecureHelper.performAction`, re-derived from **bytecode** because the
> decompiled source is string-scrubbed. Answer: the order is confirmed, the action runs asynchronously on a
> fixed-4-thread pool, it is FAIL-CLOSED, and three design choices surface — the signing password travels as reversible
> **Base64** (not a hash/challenge), the mandatory reason is **not validated against the configured `BReasonSet`**, and
> the per-write license check reads a **cached boolean** (runtime expiry is not re-queried).
>
> Sources (primary, N4.14, vendor **TridiumPS**), module root `organized/electronicSignature/electronicSignature-rt/`:
> - `vineflower/com/tridiumx/ps/model/BSecuredNumericWritable.java` — action slots + `checkLicense` + license flags (STRUCTURE).
> - `vineflower/com/tridiumx/ps/model/SecureHelper.java` — `performAction` frame (STRUCTURE).
> - `vineflower/com/tridiumx/ps/model/Helper.java` — thread pool + verify helpers (STRUCTURE).
> - `extracted/com/tridiumx/ps/model/{SecureHelper,Helper}.class` — **bytecode** (`javap -c -p`): the real invoke ORDER, Base64, sleep, branches. All bytecode citations below were token-verified by the driver (method@offset).
>
> Markers: `[CERT]` local primary — `javap` method@offset (extern, token-verified) or vineflower `file:line` · `[INFER]` deduction.
> Layer 22 (license/security) + compliance axis. Block TYPE: **evidence** (security). Builds on [B350] (identity) and [B351] (audit back-end).
>
> ⚠ **OBFUSCATION CAVEAT ([B350] header).** Decompiled `.java` is string-scrubbed (literals → `n`/`ln`); this block takes
> the call ORDER and all string literals from **bytecode** (`javap -c -p`; ldc constants + invoke targets are intact),
> using vineflower only for method/field STRUCTURE. Every load-bearing opcode below was re-run and read by the driver.

---

## 352.1 — The pipeline, in verified order

Every signed verb funnels to one method. `BSecuredNumericWritable.doSetWithAuthentication(BSecureDouble)` (`:449`) and its
siblings each call `SecureHelper.performAction(val, this, <actionSlot>, log)` `[CERT]` (`BSecuredNumericWritable.java:299`
setWithAuthentication, :311/:323/:335/:347 the emergency/auto/override twins). `BSecuredBooleanWritable` mirrors it exactly.

`SecureHelper.performAction` (`SecureHelper.java:1634`) does two things in order, confirmed in bytecode `[CERT]`
(`javap SecureHelper.class performAction`): **@7 `SecurePoint.checkLicense()`** then **@31 `Helper.submitAction(Runnable)`**
— i.e. the license is checked SYNCHRONOUSLY on the caller's thread, then the real work is handed to a lambda run on a
thread pool (§352.6). Any exception is caught in the outer frame (@37-81 `astore; … athrow`) and re-thrown.

The lambda `lambda$performAction$0` is the actual pipeline. Verified invoke order `[CERT]` (`javap SecureHelper.class
lambda$performAction$0`):

| Step | @offset | Bytecode target | Role |
|---|---|---|---|
| **(a) license** | perform@7 | `SecurePoint.checkLicense()` | fail-fast, on caller thread |
| **(b) credential + (c) reason** | @8 | `Helper.verifyActionCredentials(param, point, actionName)` | re-auth + reason (throws on failure — §352.2/352.4) |
| valid-user gate | @87 → @92 | `getIsValidUser()` → `ifeq 210` | branch PAST the write on failure (§352.3) |
| **(d) real write** | @144 | `invokeSuperAction(param, action, point, log)` | dispatches to `super.doSet/doAuto/executeOverride` |
| settle | @147-150 | `ldc2_w 500l; Thread.sleep(J)` | 500 ms delay before read-back (§352.6) |
| read-back | @157 / @214 | `getNewValueAfterAction` / `getNewValueForFailedLogin` | value for the audit record |
| **(e) audit** | @230 | `logHistory(point, param, action, point, log, old, new, z)` → `Helper.appendFacetsHistory` → `HistorySpaceConnection.append` | writes the `BSecuredTrendRecord` ([B351]) |

**Ordered pipeline:** `checkLicense → verifyActionCredentials(cred+reason) → [valid?] → invokeSuperAction → sleep(500) →
logHistory`. The audit append is the LAST step and consumes the post-write read-back value — consistent with [B351]'s
finding that the record stores old+new value strings.

## 352.2 — Credential re-authentication: real LDAP/local auth, but the password arrives as reversible Base64

`verifyActionCredentials` → `verifyPrimaryCredentials`. Bytecode order `[CERT]` (`javap Helper.class
verifyPrimaryCredentials`): **@72 `Base64.getDecoder()` → @76 `Base64$Decoder.decode(String)`** (the password parameter is
Base64-DECODED to bytes), then **@125 `BUserService.getUser(userName)` → @142 `BUser.getAuthenticationScheme()` → @145
`instanceof com/tridium/ldap/BLdapAuthenticationScheme`**: if LDAP → **@155 `verifyLdapCredential(...)`** (a real JAAS
`LoginContext.login`); else → **@168 `instanceof BPasswordCache` → @190 `BPasswordCache.validate(String)`** against the
stored Niagara credential.

Two readings, kept distinct:
- **The authentication is genuine** — it is not a bypass. The submitted password is checked against the real Niagara user
  store (local `BPasswordCache` hash) or the enterprise LDAP directory via JAAS. `[CERT]` (invoke chain above).
- **But the signing credential is carried as reversible Base64, not a hash or challenge-response** `[CERT]` (the `decode`
  at @76 means the action parameter holds the cleartext-equivalent password, only Base64-wrapped). Within a station's
  object model this is recoverable cleartext: anything that captures the action parameter — a persisted invocation, a
  verbose log, a `.bog`, a Fox trace decrypted at the endpoint — recovers the signer's password `[INFER, from Base64
  reversibility]`. Base64 confirms hypothesis **h1** flagged in [B350] §350.7. For a 21 CFR Part 11 signing ceremony,
  transporting the credential as reversible-encoded cleartext (rather than a one-way challenge) is a notable weakness,
  though TLS on the Fox channel covers it in transit.

## 352.3 — Fail-closed: two independent guards before the write

A failed signature cannot reach the write `[CERT]` (`javap SecureHelper.class lambda$performAction$0`):
1. **Exception guard.** `verifyActionCredentials` (@8) throws `LocalizableRuntimeException("electronicSignature",
   "credentialsException")` synchronously on bad credentials (string `credentialsException` present in `Helper.class`
   `[CERT]`). The throw unwinds the lambda before any later opcode — `invokeSuperAction` (@144) is never reached.
2. **Branch guard.** Even on a non-throwing invalid state, **@87 `getIsValidUser()` → @92 `ifeq 210`** jumps to offset
   210 (the failed-value path), PAST `invokeSuperAction` (@144). The write opcode is unreachable when the user is invalid.

**Fail-closed: YES.** The real write is structurally guarded by both a synchronous throw and a branch.

Audit-on-failure nuance (`[CERT]`/`[INFER]`): the two failure modes are audited DIFFERENTLY. The `getIsValidUser`=false
branch falls through to `getNewValueForFailedLogin` (@214) then the common `logHistory` (@230) — so it DOES write a
`BSecuredTrendRecord`. But a thrown `credentialsException` (@8, the wrong-password case) unwinds to the outer catch in
`performAction` (@37-81), which logs to the system logger and re-throws — it does **not** flow to `logHistory` `[INFER,
from the throw at @8 preceding the @230 audit call]`. Consequence: the most common failure — a wrong password — is NOT
necessarily recorded in the secured history audit trail, only in the station log. This is a §11.10(e) blind spot on the
FRONT door that complements the back-door purge gap of [B351].

## 352.4 — Reason mandatory, but not validated against the configured `BReasonSet`

`verifyRequiredParameter` enforces a non-empty reason and nothing more `[CERT]` (`javap Helper.class
verifyRequiredParameter`): **@1 `ISecureParameter.getReasonForChange()` → @8 `String.contentEquals("")` → @11 `ifeq 37`**
(non-empty ⇒ pass); the empty branch loads the message `"Error during required parameter verification. Please provide
reason for change."` and **@23-36 `new LocalizableRuntimeException("electronicSignature","reasonException"); athrow`**.

There is **no membership check** against the point's configured `BReasonSet`. The class `com/secured/model/config/BReasonSet`
appears in `Helper.class` only inside `getNextReasonSetID` (a config-time ID allocator) `[CERT]`, never in the write-path
validation. `BReasonSet` therefore populates the operator's UI dropdown, but the SERVER accepts any non-empty free string
as the reason-for-change `[INFER, from the contentEquals-only check + absence of a set-membership call]`. An API/fox caller
bypassing the UI can sign a change with an arbitrary reason. Part 11 §11.50(a)(3) (the "meaning" of the signature) is
enforced only as "non-empty", not "from the approved list".

## 352.5 — License checked every write, but from a startup-cached boolean

`checkLicense()` runs on every `performAction` (§352.1 @7), so the gate is at write-time, fail-fast — good. But it reads
cached booleans, not a live license query `[CERT]` (`BSecuredNumericWritable.java` `checkLicense`, fields `_isLicensed`
`:173` / `_withinCountRange` `:174`): it throws `FeatureNotLicensedException("tridium:eSignature")` when `_isLicensed` is
false and a point-count `LicenseException` when `_withinCountRange` is false. Those flags are set once, at
`atSteadyState()` (`:697-699`) via `registerPoint(this)` — station-startup time. The literal `tridium:eSignature` appears
only in the exception constructor, not in a runtime `Sys.getLicenseManager().checkFeature(...)` call `[CERT]` (bytecode
string `tridium:eSignature` present in `Helper.class`; no licenseManager invoke in the write path). Consequence: if the
eSignature feature expires or the SMA lapses while the station keeps running, the cached `_isLicensed` stays true and
signed writes keep succeeding until the next restart re-evaluates the license `[INFER, from cached-flag semantics]`.

## 352.6 — Operational shape: async on a fixed-4-thread pool, each write parked 500 ms

`Helper.submitAction` runs the pipeline on a module-static pool `[CERT]`: `actionHandlerThreadPool =
Executors.newFixedThreadPool(4)` (`Helper.java:145`), `submitAction` does `actionHandlerThreadPool.execute(action)`
(`:1899-1900`). Combined with the unconditional `Thread.sleep(500L)` after the write (§352.1 @147-150), each signed write
occupies one of four pool threads for at least 500 ms. Ceiling ≈ 4 / 0.5 s = **~8 signed writes per second station-wide**
`[INFER, from pool size × sleep]` — irrelevant for human-driven signing, but a hard cap for any batch/scripted signed
operation. The 500 ms is a settle delay so the control-point propagation completes before the audit reads back the new
value (@144 write precedes @150 sleep precedes @157 read-back); it is on the SUCCESS path only (the failure branch @210
skips it).

Because `performAction` returns as soon as the lambda is SUBMITTED (not completed), the signed action is effectively
asynchronous to the caller — the `checkLicense` throw is synchronous, but a `credentialsException` surfaces from a pool
thread `[INFER, from submit-then-return structure]`. This matters for callers expecting the classic synchronous Niagara
action-invoke contract.

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | All signed verbs → `SecureHelper.performAction(val,this,slot,log)` | `[CERT]` | `BSecuredNumericWritable.java:299,311,323,335,347,449` | ✅ read |
| 2 | performAction order: checkLicense@7 then submitAction@31 | `[CERT]` | `javap SecureHelper.class performAction` | ✅ ran |
| 3 | Lambda order: verifyActionCredentials@8 → getIsValidUser@87 → ifeq210@92 → invokeSuperAction@144 → sleep500@147-150 → logHistory@230 | `[CERT]` | `javap SecureHelper.class lambda$performAction$0` | ✅ ran |
| 4 | Password Base64-decoded before auth (h1) | `[CERT]` | `javap Helper.class verifyPrimaryCredentials` @72,@76 | ✅ ran |
| 5 | Auth = LDAP JAAS (instanceof BLdapAuthenticationScheme→verifyLdapCredential) else BPasswordCache.validate | `[CERT]` | same @142,@145,@155,@168,@190 | ✅ ran |
| 6 | Fail-closed: throw@8 + ifeq210@92 both precede write@144 | `[CERT]` | `javap` lambda + strings `credentialsException` | ✅ ran |
| 7 | Reason mandatory: getReasonForChange→contentEquals("")→ifeq→throw reasonException | `[CERT]` | `javap Helper.class verifyRequiredParameter` @1,@8,@11,@23-36 | ✅ ran |
| 8 | No BReasonSet membership check in write path (only in getNextReasonSetID) | `[CERT]` | `javap Helper.class` BReasonSet refs | ✅ ran |
| 9 | License via cached `_isLicensed`/`_withinCountRange`, set at atSteadyState | `[CERT]` | `BSecuredNumericWritable.java:173-174,697-699,checkLicense` | ✅ read |
| 10 | String `tridium:eSignature` only in exception, no runtime licenseManager call | `[CERT]` | `strings Helper.class`; no invoke in path | ✅ ran |
| 11 | Fixed-4-thread pool, submitAction.execute | `[CERT]` | `Helper.java:145,1899-1900` | ✅ read |
| 12 | Base64 = reversible cleartext transport of the signing credential | `[INFER]` | from claim 4 | ⚠ deduction |
| 13 | Wrong-password (thrown) case not written to secured history, only system log | `[INFER]` | from throw@8 preceding audit@230 | ⚠ deduction |
| 14 | ~8 signed writes/sec ceiling (4 threads × 500ms) | `[INFER]` | from claims 3,11 | ⚠ deduction |
| 15 | Runtime license expiry not caught until restart | `[INFER]` | from claim 9 cached-flag semantics | ⚠ deduction |

Marker tally: `[CERT]` ×11 · `[INFER]` ×4. [INFER]/[CERT] = 4/11 = 0.36 — healthy for an evidence block; every
mechanism (order, Base64, auth chain, fail-closed, reason, license, pool) is `[CERT]` from bytecode I re-ran, and the
`[INFER]`s are the security/operational CONSEQUENCES built on those verified mechanics.

Framework-semantic check applied to the delegated sweep's claims (items 2-8, 10): re-ran every `javap` myself and read the
opcode order/targets directly; corrected the sweep's "sleep is in success path" and "failed attempts audited" framings to
distinguish the thrown-credential path (not audited to history) from the getIsValidUser branch (audited) — items 6/13.

## Connections

- [Block 350] — ES1: the `*WithAuthentication` verb set (§350.4), the license feature `tridium:eSignature`+`point.limit` (§350.3), and the h1 Base64 / reason hypotheses this block confirms/refines (§350.7).
- [Block 351] — ES4: the audit back-end this pipeline's step (e) writes to; §352.3's thrown-credential blind spot complements §351's purge gap — both are §11.10(e) weaknesses (one front-door, one back-door).
- Core auth: `javax.baja.user.BUserService`, `javax.baja.security.BPasswordCache`, `com.tridium.ldap.BLdapAuthenticationScheme` (JAAS) — the real authentication substrate the module re-invokes per signed write.

## Open gaps after this block

- ES5 (credential handling) is now largely ANSWERED by §352.2 (Base64 confirmed; auth = BPasswordCache/LDAP) — reverse-backlog: ES5 narrows to "does the secondary/remote signer path reuse verifyPrimaryCredentials or a distinct verifySecondaryCredentials, and does it Base64 too?" (the bytecode showed a `verifySecondaryCredentials` sibling). Re-scope ES5 accordingly.
- ES3 (dual-signature / remote transport), ES6 (ux/wb layers), ES7 (mutable ESignAcknowledgement) remain queued. ES4-G1 (live-permission) still requires-execution.
