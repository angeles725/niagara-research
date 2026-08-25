# Block 498 — `framework-drivers` FD3: `opcUaServer` — N4 as an OPC UA server (address space from the component space, endpoint :52520), the server-side security exposure (writable-by-default nodes, username token under `SecurityPolicy.NONE`, anonymous fall-through, cert sessions with NO Niagara RBAC), and the `tridium:opcUaServer` gate

> **Focus:** `framework-drivers`, gap **FD3** — the server half of the OPC UA suite (N4 EXPOSING itself;
> consumes the FD1 SDK carrier [B496]). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/opcUaServer/opcUaServer-rt/decompiled/…` (this artifact ships a
> `decompiled/` tree, not vineflower — line numbers cite that tree). FUENTE 1 — [B496] (config types + cert
> listener), [B497] (client counterpart), [B11]/[B30] (Niagara auth/RBAC), [B392]/[B398] (trust model).
> Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §498.1 — Component tree: exposing the component space as an address space `[CERT]`

`BOpcUaServer extends BNNetwork` (`BOpcUaServer.java:186`) implements `SessionManagerListener` — the driver
network placed in the station. Structure:

| Class | Role |
|---|---|
| `BOpcTcpEndpoint` (BStruct) | endpoint config: `enabled=true`, `port=52520`, `securityMode`, `securityPolicies` (`BOpcTcpEndpoint.java:32,36`) |
| `BOpcUaNamespace extends BNDevice` | one node-manager per exported UA namespace; holds `points` + `AlarmExt` |
| `BOpcUaServerProxyExt` | per-point export/import proxy — a Niagara `BControlPoint` is exposed as a UA node **only if** it carries this ext |
| `OpcUaIoManagerListener` | live read/write handler (Prosys IoManager) |
| `OpcUaHistorian` / `BOpcUaServerAlarmDeviceExt` | UA history reads / event-alarm exposure |

`BOpcUaNamespace.startNodeSpace()` builds a `NodeManagerUaNode` and adds only the `BControlPoint` children that
have a `BOpcUaServerProxyExt`. **The exposed surface is opt-in per point.**

## §498.2 — Endpoint & security exposure `[CERT]`

`startServer()` → `initialize(port, tcpEnable, appName)` constructs the Prosys `UaServer`, binds
`0.0.0.0:52520` by default. Security modes fed to the server (`BOpcUaServer.java:588-589`):

```
List securityModes = OpcUaSecurityMode.makeSecurityModes(endpoint.getSecurityMode(), endpoint.getSecurityPolicies());
this.server.getSecurityModes().addAll(securityModes);
```

**This is where B496's bitstrings live** (the client [B497] does not use them). Endpoint defaults
(`BOpcTcpEndpoint.java:32`): `securityMode = BOpcTcpSecurityModes.make(true)` and
`securityPolicies = BOpcTcpSecurityPolicies.DEFAULT`.

- `make(true)` = `ALL` = `DEFAULT` = **6** (Sign + SignEncrypt; **NONE bit clear**) — verified in
  `BOpcTcpSecurityModes.make(boolean){ return v ? ALL : EMPTY; }` and `ALL=DEFAULT=6` ([B496 §496.5]). So the
  server does **NOT** offer an unauthenticated NONE endpoint by default `[CERT]` (correcting the FD3-sweep's
  open worry). NONE only appears if config sets the mode bits to 0/None (the B496 `makeSecurityModes` bits=0
  fail-open).
- `securityPolicies DEFAULT=7` → **Basic128Rsa15 (deprecated) offered by default** alongside Basic256/Basic256Sha256.
  This is the B496 footgun that **does** apply here (server-side).

**User token policies** (`BOpcUaServer.java:591-598`), gated on `BOpcUserAuthenticationMethods` bits:

```
:591 if (methods.includes(1)) server.addUserTokenPolicy(UserTokenPolicies.ANONYMOUS);
:594 if (methods.includes(2)) server.addUserTokenPolicy(new UserTokenPolicy("username_plain",
:595        UserTokenType.UserName, null, null, SecurityPolicy.NONE.getPolicyUri()));
:597 if (methods.includes(4)) server.addUserTokenPolicy(UserTokenPolicies.SECURE_CERTIFICATE);
```

Default `BOpcUserAuthenticationMethods.DEFAULT` = username-only ([B496]: bit 2). `[INFER]`: the username token
is registered under **`SecurityPolicy.NONE`** — the password field is not independently encrypted; its
confidentiality relies entirely on the channel's SecurityMode. With a Sign-only (not SignEncrypt) channel the
username/password crosses the wire unencrypted.

## §498.3 — User authentication → Niagara mapping (the load-bearing security story) `[CERT]`

`server.setUserValidator(new OpcUaUserValidator(...))` (`BOpcUaServer.java:600`). `OpcUaUserValidator.onValidate()`:

- **UserName token** → `BUserService.getService().getUser(name)` (`OpcUaUserValidator.java:73,105`), then requires
  the Niagara user's auth scheme be **`BOpcUaAuthenticationScheme`** (else `LOGIN_INTERFACE_NOT_SUPPORTED`); on
  match, runs a full `BAuthenticationService.authenticate(...)` JAAS login and registers a `BOpcUaServerSession`
  (a `NiagaraSession`) in `SessionManager`. `[INFER]`: a UA username session therefore carries **that Niagara
  user's real RBAC/roles** — Niagara access control applies to its reads/writes. A stock username/password user
  with the DEFAULT scheme is **rejected** — the operator must assign the OPC UA scheme.
- **Anonymous** → no explicit branch; execution **falls through to `return true`** (`OpcUaUserValidator.java:91,97`).
  `[INFER]`: if the server has Anonymous enabled (bit 1), the validator accepts the session with **no credential
  check**. (Anonymous is off by default, bit 2 only.)
- **Certificate** → delegated to `OpcUaServerCertificateValidator` (PKI trust only); **no `BUser` lookup, not
  added to `SessionManager`** `[CERT]`. `[INFER]`: a certificate-authenticated UA session has **no Niagara RBAC
  identity** — its authorization at the Niagara layer is undefined, unlike the username path.

## §498.4 — Certificate handling `[CERT]`

Server app cert from Niagara `IKeyStore` via `certAliasAndPassword` → Prosys `ApplicationIdentity`
(`BOpcUaServer.java:580-587`). Client-cert validator `OpcUaServerCertificateValidator` wired at init
(`:573-574`); its constructor builds `new OpcUaCertificateValidationListener(BCertificateType.client)` — the
shared [B496] listener, type=client. Validation runs trust (Niagara `CoreClientTrustManager.checkClientTrusted`
against `USER_TRUST_STORE`, anchored by the `caCertificate` slot) → self-signed → validity → URI, then the B496
listener verdict. `[CERT negative]`: no auto-accept path for unknown client certs — they get `Bad_CertificateUntrusted`.

## §498.5 — Write exposure: writable by default `[CERT]`

`BOpcUaNamespace.checkAddAccessLevel()` (`BOpcUaNamespace.java:169-172,790`):

```
AL_EXPORT = AccessLevelType.of(CurrentRead);                 // read-only
AL_IMPORT = AccessLevelType.of(CurrentRead, CurrentWrite);   // read + WRITE
if (point instanceof BIOpcExport) node.setAccessLevel(AL_EXPORT); else node.setAccessLevel(AL_IMPORT);
```

`[INFER]`: only points whose proxy implements the marker interface `BIOpcExport` are read-only. The standard
`BOpcUaServerProxyExt` (import/bidirectional) path gets **`CurrentRead + CurrentWrite`** — a connected OPC UA
client **can write values into exposed Niagara control points** (handled by `OpcUaIoManagerListener`), subject
to the session's Niagara RBAC (§498.3) — which for a certificate-only session is undefined.

## §498.6 — License gate `[CERT]`

`getFeature("tridium", "opcUaServer")` (`BOpcUaServer.java:340`), enforced by `isServerLicensed()`
(`:449-458`) called from `startServer()` before any `UaServer` is created; unlicensed → fault, no server.
Feature = **`tridium:opcUaServer`**. Together with [B497] this closes the [B496 §496.6] reverse-backlog: the
OPC UA license split is per-role (`tridium:opcUaClient` / `tridium:opcUaServer`), neither in the shared core.

## §498.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BOpcUaServer=BNNetwork; per-point opt-in export via BOpcUaServerProxyExt; endpoint :52520 | `[CERT]` | `BOpcUaServer.java:186`; `BOpcTcpEndpoint.java:36` | PASS |
| 2 | server uses B496 bitstrings via makeSecurityModes; default mode=6 (no NONE); policies=7 (Basic128Rsa15 on) | `[CERT]` | `BOpcUaServer.java:588`; `BOpcTcpEndpoint.java:32`; `make(boolean)` body | PASS |
| 3 | username token registered under SecurityPolicy.NONE; anonymous/cert gated on method bits | `[CERT]`+`[INFER]` | `BOpcUaServer.java:591-598` | PASS |
| 4 | username→BUser+OPC-scheme→RBAC session; anonymous falls through return true; cert = no BUser/RBAC | `[CERT]`+`[INFER]` | `OpcUaUserValidator.java:73,91,97,105` | PASS |
| 5 | client-cert validator wires B496 listener (type=client); no auto-accept | `[CERT]`/`[CERT neg]` | `OpcUaServerCertificateValidator` ctor; `BOpcUaServer.java:573-574` | PASS |
| 6 | non-export points get CurrentRead+CurrentWrite → clients can write | `[CERT]`+`[INFER]` | `BOpcUaNamespace.java:171,790` | PASS |
| 7 | license `tridium:opcUaServer` gate before server start | `[CERT]` | `BOpcUaServer.java:340,449-458` | PASS |

**Tally:** 7 claims — 5 `[CERT]`/`[CERT negative]` load-bearing + several `[INFER]` (token confidentiality,
RBAC mapping, write reach) on cited code. Block TYPE = **EVIDENCE**; ratio ~0.4, FD3 CLOSED. All load-bearing
tokens re-verified inline against the `decompiled/` tree.

## §498.8 — Connections & focus status

- Server twin of **[B497]** (client). Shared: the [B496] SDK carrier, cert listener, and the `getFeature("tridium",…)`
  license split. Divergence: server uses the **bitstring** config (the B496 footguns land here); client uses enums.
- **Security standouts for [B490]/[B398] (SEC feed):** (a) writable-by-default exposed nodes; (b) username token
  under `SecurityPolicy.NONE` (confidentiality = channel-only); (c) certificate sessions with **no Niagara RBAC**;
  (d) anonymous = unconditional `return true` if enabled; (e) Basic128Rsa15 offered by default. None require a
  live host — all are `[CERT]`/`[INFER]` from code; a live-station confirm of exposed-node writability is an
  optional §12 follow-up (requires-execution, NOT registered as blocking — investigable set stands).
- RBAC mapping ties to [B11]/[B30] (Niagara auth schemes + user model); trust store to [B392]/[B398].
- **Focus status:** `framework-drivers` 3/10 (FD1–FD3 closed). NEXT = FD4 `obixDriver`.
