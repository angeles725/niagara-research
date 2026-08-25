# Block 496 — `framework-drivers` FD1: `opcUaCore-rt` — the OPC UA stack anchor (16 Tridium type-shim classes bundling Prosys OPC UA Java SDK 5.1.0-116 + Apache HttpCore NIO 4.4.13 AS-IS), its inherited security surface, and four config-default footguns (Basic128Rsa15 on, TLS 1.0/1.1 on, `ALL`≡username-only, self-signed accepted if trust-store-enrolled)

> **Focus:** `framework-drivers` (bootstrapped this session from `oem-honeywell-tail` U12), gap **FD1** — the
> anchor of the OPC UA suite. READ-ONLY, decompiled source (vineflower); no binary run. Markers §3.
> **Sources:** FUENTE 3 (code) — `organized/opcUaCore/opcUaCore-rt/vineflower/…` (decompiled) + `module.xml`
> + `MANIFEST.MF`. FUENTE 1 (corpus): [B127]/[B132] native-OPC/COM boundary (different module). FUENTE 2
> (niagara-help): not consulted — this is a decompilation-only gap (recorded zero, see §496.7).
> **Scope:** the SHARED CORE module only (the SDK carrier). The client/server *driver* modules that consume
> it are FD2/FD3, separate blocks. Evidence delegated to a `sonnet` sweep; all load-bearing file:line
> re-verified inline by the driver (the sweep's line numbers were offset-normalized and were REPLACED with
> real line numbers below).

## §496.1 — What Tridium actually authored: 16 classes `[CERT]`

`opcUaCore-rt` holds ~3114 vineflower `.java` files, but only **16** are Tridium-authored — all under the
single package `com.tridium.opcUaCore` (grep-verified: `find … -path '*com/tridium*' -name '*.java'` = 16;
**zero** `javax.baja.*` authored here — all `javax.baja` references are framework imports). The 16 are a
thin **type-system shim** that maps Niagara's Baja type system onto the Prosys SDK's Java objects:

| Class | Role |
|---|---|
| `BOpcTcpSecurityModes` | `BBitString` None=1/Sign=2/SignEncrypt=4 · DEFAULT=6 |
| `BOpcTcpSecurityPolicies` | `BBitString` Basic128Rsa15=1/Basic256=2/Basic256Sha256=4 · DEFAULT=7 |
| `BOpcHttpsSecurityPolicies` | `BBitString` TLS1.0=1/1.1=2/1.2=4 · DEFAULT=7 |
| `BOpcUserAuthenticationMethods` | `BBitString` Anon=1/UserPsw=2/Cert=4 · DEFAULT=2 |
| `OpcUaSecurityMode` | static factory: two BBitStrings → `List<SecurityMode>` for the SDK |
| `BSecurityMode` (enums) | frozen enum → Prosys `SecurityMode` |
| `BServerState` (enums) | frozen enum → Prosys `ServerState` |
| `BCertificateType` (enums) | frozen enum client/server/ca |
| `BUaDataType` (enums) | ~20 UA primitive types ↔ NodeId |
| `BUaAccessLevel` | Rd/Wr/HistRd/HistWr node-attribute flags |
| `BUaArgument` / `BUaArgumentVector` | UA method-argument struct/vector ↔ Prosys `Variant[]` |
| `BAlarmSeverities` | Niagara alarm state → UA severity (Offnormal 700 / Fault 900 / Normal 500 / Alert 600) |
| `OpcUaUnitsUtil` | Niagara engineering-unit → UA/UNECE unit-id table |
| `OpcUaCoreUtil` | Prosys `ServiceException`/`StatusException` → localized Niagara `Lexicon` strings |
| `OpcUaCertificateValidationListener` | the certificate-validation gate (§496.4) |

There is **no protocol logic** here — no session engine, no encoder. The 16 classes are the marshalling seam;
the wire/session/crypto work is 100% the bundled SDK.

## §496.2 — Bundled third-party: Prosys SDK 5.1.0-116 + Apache HttpCore NIO, vendored AS-IS `[CERT]`

- **Prosys OPC UA Java SDK for Java, version `5.1.0-116`** — `com/prosysopc/ua/UaApplication.java:20,40,119`
  (`static final String cp = "5.1.0-116"` and `logger.info("Prosys OPC UA SDK for Java version {}", "5.1.0-116")`).
- The **OPC Foundation stack is embedded inside Prosys' namespace** (`com.prosysopc.ua.stack.*` — client,
  server, cert, transport, encoding, builtintypes). There is **no** `org.opcfoundation.*` package `[CERT negative]`.
- **Apache HttpCore NIO 4.4.13** (`org/apache/http/nio/…`, pom.properties `artifactId=httpcore-nio version=4.4.13`)
  for the OPC-UA-over-HTTPS transport.
- **No Bouncy Castle in this jar** `[CERT negative]` — crypto primitives come from the JVM / platform layer.
- **Strategy = AS-IS vendoring, not shading:** the SDK keeps its original `com.prosysopc.ua` package. `[INFER]`
  security consequence: a CVE in Prosys 5.1.0-116 or HttpCore 4.4.13 lands byte-for-byte in every N4.14.0.162
  install that loads this module, and there is no repackaging layer to patch around it — the fix is a Tridium
  module rebuild. (Pinned to this version; DRIFT-check on any newer N4 build.)

## §496.3 — Module identity & the SDK-carrier pattern `[CERT]`

`module.xml`: `<module name="opcUaCore-rt" vendor="Tridium" vendorVersion="4.14.0.162" nre="true"
autoload="true" installable="true" runtimeProfile="rt">` — `nre="true"`+`autoload="true"` = it loads at NRE
startup. `MANIFEST.MF:3` `Implementation-Version: 4.14.0.162`. The client/server driver modules declare a
`<dependency name="opcUaCore-rt">` and **do not re-bundle the SDK** — they consume the one shared copy. This
is the same "thin Tridium layer over a big vendored SDK" pattern the audit flagged for `abstractMqttDriver`
(FD10) — a cross-focus hypothesis to test there.

## §496.4 — Certificate validation: self-signed accepted iff trust-store-enrolled `[CERT]`

`OpcUaCertificateValidationListener.onValidate()` (`OpcUaCertificateValidationListener.java:25`) is Tridium's
**sole** cert-validation configuration point; it implements Prosys `DefaultCertificateValidatorListener` and
runs after the SDK computed which `CertificateCheck`s passed. Logic (real lines):

```
:47  if (!passedChecks.contains(CertificateCheck.Trusted))   return ValidationResult.Reject;
:50  if (!passedChecks.contains(CertificateCheck.Signature))  return ValidationResult.Reject;
:53  if (!passedChecks.contains(CertificateCheck.Validity))   return ValidationResult.Reject;
:63  if (!passedChecks.contains(CertificateCheck.Uri))        return ValidationResult.Reject;
:88  if (passedChecks.contains(CertificateCheck.SelfSigned)) { /* log only */ }
:92  return ValidationResult.AcceptPermanently;
```

`[INFER]` (framework-semantic, from the code): a self-signed cert is **NOT hard-rejected**. `SelfSigned`
merely logs and falls through to `AcceptPermanently`. The real gate is `CertificateCheck.Trusted`, i.e.
membership in the `PkiDirectoryCertificateStore` trusted dir — so the security of the OPC UA channel reduces
to **trust-store hygiene** (who can drop a `.der` into the trusted folder), not to any intrinsic rejection of
self-signed peers. This matches the corpus-wide N4 trust posture ([B392] signing/PKI: trust = file-store
enrollment, not CA-chain enforcement).

## §496.5 — Four config-default footguns `[CERT]`

Verified DEFAULT constants (real lines):

1. **`BOpcTcpSecurityPolicies.DEFAULT = 7`** (`:19`) → **Basic128Rsa15 enabled by default** alongside
   Basic256/Basic256Sha256. Basic128Rsa15 is deprecated/insecure (SHA-1, RSA-1.5) in the OPC UA spec.
2. **`BOpcHttpsSecurityPolicies.DEFAULT = 7`** (`:20`) → **TLS 1.0 + TLS 1.1 enabled by default** on the HTTPS
   transport (both deprecated).
3. **`BOpcUserAuthenticationMethods.DEFAULT = 2` and `ALL = DEFAULT`** (`:17,:25`) → the constant literally
   named `ALL` is **UsernamePassword-only**, NOT the union of Anon+UserPsw+Cert. A caller reaching for `ALL`
   expecting "all methods" silently gets one. (Same misnomer in the other three BBitStrings: every `ALL ≡ DEFAULT`.)
4. **`OpcUaSecurityMode.makeSecurityModes()` NONE footgun** (`:12-13`):
   `if (tcpModeBits == 0 || (tcpModeBits & 1) != 0) list.add(SecurityMode.NONE);` — a zeroed mode bitfield
   **silently enables the unauthenticated/unencrypted NONE mode** (fails OPEN, not closed). Mitigant: the
   shipped default is `BOpcTcpSecurityModes.DEFAULT = 6` (`:18`, Sign+SignEncrypt, NONE bit clear), so NONE is
   OFF out of the box — the footgun fires only if config clears all mode bits. Also note the Baja type exposes
   **only the three legacy TCP policies**; the Prosys `SecurityMode` class defines AES128/AES256 modes that
   `BOpcTcpSecurityPolicies` gives no path to select `[CERT]` (`SecurityMode.java` in the SDK vs the 3-bit Baja enum).

> **§14 REFINED in [B497]/[B498]:** the DEFAULT constants above are the **SERVER endpoint** config surface
> ([B498] `BOpcTcpEndpoint`) — Basic128Rsa15/TLS1.0-1.1-on and the `make(true)`≡6 mode apply there. The
> **CLIENT** ([B497]) does NOT read these bitstrings at all; it uses the single-select `BSecurityMode` enum
> whose `DEFAULT = signEncryptBasic256Sha256` (STRONG), so the client's out-of-box crypto is Basic256Sha256,
> not Basic128Rsa15. Only the hardcoded HTTPS TLS-1.0/1.1 reaches the client.

## §496.6 — No license gate in the core `[CERT negative]`

grep for `getFeature|license|License|Feature(` across all 16 Tridium classes = **0 hits**. The OPC UA license
feature (if any) is enforced in the consumer driver modules (`opcUaClient`/`opcUaServer` = FD2/FD3), not in
the shared SDK carrier. To be confirmed when those blocks open (a REVERSE-BACKLOG note on FD2/FD3).

## §496.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Exactly 16 Tridium classes, all `com.tridium.opcUaCore`, 0 authored `javax.baja` | `[CERT]` | `find -path '*com/tridium*'`=16 (§496.1) | PASS |
| 2 | Bundled SDK = Prosys OPC UA Java 5.1.0-116; no org.opcfoundation; HttpCore NIO 4.4.13; no BouncyCastle | `[CERT]`/`[CERT neg]` | `UaApplication.java:20,40,119`; pom.properties | PASS |
| 3 | AS-IS vendoring (com.prosysopc kept); shared carrier via dependency | `[CERT]` | `module.xml:2`; MANIFEST:3 | PASS |
| 4 | Self-signed NOT hard-rejected; gate = CertificateCheck.Trusted (trust-store) | `[CERT]`+`[INFER]` | `OpcUaCertificateValidationListener.java:47,50,53,63,88,92` | PASS |
| 5 | DEFAULTs: TcpPolicies=7 (Basic128Rsa15 on), Https=7 (TLS1.0/1.1 on), Auth=2 & ALL≡DEFAULT | `[CERT]` | `.java:19 / :20 / :17,:25` | PASS |
| 6 | makeSecurityModes bits=0 → NONE (fail-open); but DEFAULT modes=6 so OFF by default | `[CERT]` | `OpcUaSecurityMode.java:12-13`; `BOpcTcpSecurityModes.java:18` | PASS |
| 7 | No license/feature gate in the 16 core classes | `[CERT negative]` | grep getFeature/license = 0 | PASS |

**Tally:** 7 claims — 6 `[CERT]`/`[CERT negative]` load-bearing + 2 `[INFER]` (§496.2 CVE-blast, §496.4
trust-reduction) resting on cited code. Block TYPE = **EVIDENCE** (decompilation). [INFER]/[CERT] ratio low
(~0.25); FD1's investigable evidence is well-covered, gap CLOSED. All 7 load-bearing tokens re-verified
inline by the driver against real line numbers (sweep offsets discarded).

## §496.8 — Connections & focus status

- **Opens the `framework-drivers` focus** (first block, FD1). Frames FD2 (`opcUaClient`) and FD3
  (`opcUaServer`): both consume this SDK carrier, so their security posture is set by the BBitString configs
  documented here — FD2/FD3 must check where each config default is actually read and whether the driver UI
  lets an operator weaken it. Carries a REVERSE-BACKLOG note: the license gate (§496.6) is expected in FD2/FD3.
- **Cross-focus:** the AS-IS-vendored-SDK pattern (§496.3) is a hypothesis to test on **FD10 `abstractMqttDriver`**
  (1978 cls — likely the same thin-shim-over-SDK shape).
- **Trust model:** §496.4 self-signed-if-enrolled is the OPC UA instance of the corpus-wide N4 file-store trust
  posture ([B392] signing/PKI; [B398] SEC live audit truststore `changeit`). Not a new weakness — the same
  "enrollment = trust" model applied to a new protocol.
- **Native vs Java:** distinct from [B127]/[B132] which covered the classic-OPC-DA **native** `opc.dll`/COM
  boundary; OPC UA is pure-Java (Prosys), no JNI. (The classic-DA Java tree is FD7.)
- **Focus status:** `framework-drivers` 1/10 (FD1 closed). NEXT = FD2 `opcUaClient`.
