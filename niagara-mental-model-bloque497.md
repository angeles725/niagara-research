# Block 497 — `framework-drivers` FD2: `opcUaClient` — the OPC UA client driver (network→device→point over the Prosys `UaClient`), its security-config surface (single-select `BSecurityMode` enum, NOT the B496 bitstrings), plaintext-recoverable `BPassword` credentials, the `tridium:opcUaClient` license gate, and a one-dialog operator downgrade to None/anonymous

> **Focus:** `framework-drivers`, gap **FD2** — the client half of the OPC UA suite (consumes the FD1 SDK
> carrier [B496]). READ-ONLY, decompiled (vineflower); no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/opcUaClient/opcUaClient-{rt,wb}/vineflower/…`. FUENTE 1 — [B496] (SDK
> carrier + config types), [B392]/[B398] (N4 trust model). FUENTE 2 — not consulted (decompilation-only gap).
> Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline (sweep line numbers
> were offset-normalized → discarded; real lines below).

## §497.1 — Component tree `[CERT]`

`BNNetwork`-based device driver (`com.tridium.opcUaClient`):

| Class | Base | Role |
|---|---|---|
| `BOpcUaNetwork` | `BNNetwork` | network root; holds the license gate (§497.5) |
| `BOpcUaDeviceFolder` | `BNDeviceFolder` | optional grouping |
| `BOpcUaDevice` | `BNDevice` (impl `BINPollable`) | one OPC UA server connection; **owns the `UaClient` session + ALL security slots** |
| `BOpcUaClientPointDeviceExt` | `BNPointDeviceExt` | point-subscription extension (owns the Prosys `Subscription`) |
| `BOpcUaClientHistoryDeviceExt` / `…AlarmDeviceExt` | `BNPointDeviceExt` / — | history-import / alarm extensions |
| `BOpcUaClientProxyExt` | `BNProxyExt` | per-point proxy: `uaNodeId` → Niagara `BStatusValue` |

Standard Niagara driver shape (network→device→ext→proxy); the OPC-specific work is the session + node mapping.

## §497.2 — Session establishment + the B496 config split `[CERT]`

`BOpcUaDevice` connects via `new UaClient(serverEndpointUrl)` then `initialize()` (`BOpcUaDevice.java`):

```
:746  this.setCertificateValidator();
:747  SecurityMode securityMode = this.getSecurityMode().getSecurityMode();   // BSecurityMode(opcUaCore)→SDK
:748  this.uaClient.setSecurityMode(securityMode);
:752  ...setHttpsSecurityPolicies(new HttpsSecurityPolicy[]{TLS_1_0, TLS_1_1, TLS_1_2});   // HARDCODED
```

**REFINES [B496] (§14).** The client does **NOT** use the B496 bitstring types `BOpcTcpSecurityModes` /
`BOpcTcpSecurityPolicies` / `BOpcUserAuthenticationMethods` — grep for all three across `com/tridium/opcUaClient`
= **0 hits** `[CERT negative]`. Instead the client selects security with the **single-select `BSecurityMode`
enum** (opcUaCore, `enums/BSecurityMode.java:31` `DEFAULT = signEcriptBasic256Sha256`) and the **`BOpcUserAuthenticationMode`
enum** (client-local, `BOpcUserAuthenticationMode.java:22` `DEFAULT = userNameAndPassword`). So:
- The client's **default security is STRONG** — `signEncryptBasic256Sha256`, not the weak Basic128Rsa15 that
  B496's `BOpcTcpSecurityPolicies.DEFAULT=7` would suggest. B496's bitstring DEFAULTs are the **SERVER endpoint**
  config surface (see [B498]), not the client's.
- The client's HTTPS policy list is **hardcoded** to TLS 1.0/1.1/1.2 (`:752`) — not operator-configurable, and
  it still carries the deprecated TLS 1.0/1.1 (the one B496 footgun that DOES reach the client, but as fixed code).

## §497.3 — Credentials: plaintext-recoverable `BPassword` `[CERT]`

Slots on `BOpcUaDevice`: `userAuthenticationMode` (`BOpcUserAuthenticationMode`, DEFAULT `userNameAndPassword`),
`userName` (String), `password` (`BPassword`), `userAuthenticationCertificate` (`BCertificateAliasAndPassword`).
`UserIdentity` built in `checkAuthenticationMode()`:

```
:794  String password = this.getPassword().getValue().trim();     // BPassword → plaintext String
:799  client.setUserIdentity(new UserIdentity(username, password));  // UsernamePassword
:806  client.setUserIdentity(new UserIdentity(new Cert(userAuthCertificate), userAuthPrivateKey));  // X.509
:814  client.setUserIdentity(new UserIdentity());                    // Anonymous
```

`[INFER]` (framework-semantic): `BPassword.getValue()` returns the password as a recoverable `String`. Niagara
`BPassword` is reversible obfuscation persisted in `config.bog`, not a KMS/OS-protected secret — so a station
backup yields the plaintext upstream OPC UA credential. Same reversible-at-rest posture the corpus records
for other outbound-credential holders ([B482] cripto-at-rest; [B398] SEC `.bog` plaintext).

## §497.4 — Certificate handling: wires the B496 listener, no trust-all flag `[CERT]`

`setCertificateValidator()` (`BOpcUaDevice.java:721-723`) builds `OpcUaClientCertificateValidator` and
installs it on the `UaClient`. That validator's `validateCertificate()` runs Trusted (via Niagara
`CoreClientTrustManager.checkServerTrusted`) → SelfSigned → Validity → URI, then **delegates the verdict to
`OpcUaCertificateValidationListener.onValidate()`** — the shared opcUaCore component documented in [B496 §496.4],
constructed here with `BCertificateType.server`. `[CERT negative]`: no "trust all" / accept-untrusted boolean
exists in the client module; server-cert trust reduces to Niagara trust-store enrollment (the B496 model). The
client also does NOT directly configure a Prosys `PkiDirectoryCertificateStore` — it defers PKI to Niagara's
`ICryptoManager`/`CoreClientTrustManager` stack.

## §497.5 — License gate — closes B496's REVERSE-BACKLOG `[CERT]`

```
BOpcUaNetwork.java:70  public final Feature getLicenseFeature() {
BOpcUaNetwork.java:71     return Sys.getLicenseManager().getFeature("tridium", "opcUaClient");
```

Feature = **`tridium:opcUaClient`**, checked by the `BNNetwork` base at network start. This is the gate [B496 §496.6]
predicted was absent from the shared core and present in the consumer — **confirmed**. Grep of the rt classes
finds `getFeature`/license in this ONE site only.

## §497.6 — Config-UI weakening: one dialog to None / anonymous `[CERT]`

`opcUaClient-wb` field editors (`…/ui/`):
- `BSecurityModeFE` populates the dropdown most-secure-first; **`None` is selectable** — on save it checks
  `BSecurityMode.none.equals(...)` and shows `BDialog.confirm()`; confirm → `None` persisted, cancel → snaps
  back to `signEncryptBasic256Sha256`. Weak modes (Basic128Rsa15, Basic256-no-SHA256) are selectable with **no**
  dialog. (`ui/BSecurityModeFE.java:79-80,87,110`)
- `BOpcUserAuthenticationModeFE` offers `anonymous`; selecting it triggers the same `BDialog.confirm()`
  (`ui/BOpcUserAuthenticationModeFE.java:43,51`).

`[INFER]`: an authenticated Workbench operator can downgrade the channel to `None` (no sign/encrypt) or to
`anonymous`, each behind a single dismissible confirmation and with **no audit trail** in the UI layer — the
secure default protects only the operator who never opens the dropdown.

## §497.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | network→device→ext→proxy BNNetwork driver tree | `[CERT]` | `com.tridium.opcUaClient` classes §497.1 | PASS |
| 2 | client reads BSecurityMode enum→SDK; does NOT use B496 bitstrings (grep=0) | `[CERT]`/`[CERT neg]` | `BOpcUaDevice.java:747-748`; grep-neg | PASS |
| 3 | client default = signEncryptBasic256Sha256 (strong); auth default = userNameAndPassword | `[CERT]` | `BSecurityMode.java:31`; `BOpcUserAuthenticationMode.java:22` | PASS |
| 4 | HTTPS hardcoded TLS 1.0/1.1/1.2 | `[CERT]` | `BOpcUaDevice.java:752` | PASS |
| 5 | password via BPassword.getValue() plaintext String → UserIdentity | `[CERT]`+`[INFER]` | `BOpcUaDevice.java:794,799` | PASS |
| 6 | cert validator wires opcUaCore OpcUaCertificateValidationListener; no trust-all flag | `[CERT]` | `BOpcUaDevice.java:721-723`; grep-neg | PASS |
| 7 | license `tridium:opcUaClient` (closes B496 reverse-backlog) | `[CERT]` | `BOpcUaNetwork.java:70-71` | PASS |
| 8 | UI: None + anonymous selectable behind one confirm dialog | `[CERT]` | `ui/BSecurityModeFE.java:79-80`; `ui/BOpcUserAuthenticationModeFE.java:51` | PASS |

**Tally:** 8 claims — 6 `[CERT]`/`[CERT negative]` load-bearing + 2 `[INFER]` (plaintext-recovery, UI-downgrade)
on cited code. Block TYPE = **EVIDENCE**; ratio ~0.25, FD2 CLOSED. All load-bearing tokens re-verified inline.

## §497.8 — Connections & focus status

- **§14 REFINE of [B496]:** B496 §496.5 documented the bitstring DEFAULTs (Basic128Rsa15 on, TLS1.0/1.1 on) as
  "the config surface consumer modules read". This block narrows it: those bitstrings are the **SERVER endpoint**
  config ([B498]); the **CLIENT** uses single-select enums with a **strong** default (Basic256Sha256). Only the
  hardcoded HTTPS TLS-1.0/1.1 footgun reaches the client. Back-pointer added to B496 §496.5.
- Pairs with **[B498]** FD3 `opcUaServer` (same session; the exposure side). Both wire the shared cert listener
  and license via `getFeature("tridium", …)`.
- Trust model = the corpus-wide N4 file-store enrollment ([B392]/[B398]); credential-at-rest reversibility = [B482].
- **Focus status:** `framework-drivers` 2/10 (FD1–FD2 closed). NEXT after FD3 = FD4 `obixDriver`.
