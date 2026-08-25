# Block 504 — `framework-drivers` FD10: `abstractMqttDriver` — a multi-cloud MQTT base that is 97% vendored SDK (59 Tridium classes over Eclipse Paho 1.2.5 + AWS IoT SDK 1.3.11 + Jackson/Joda/JJWT, shaded), MQTT 3.1.1 TCP/TLS, five pluggable cloud authenticators, and a TLS-default-but-port-1883 misconfiguration footgun

> **Focus:** `framework-drivers`, gap **FD10** — the abstract MQTT driver base (BEYOND the original U12 list).
> Measured **1975** classes in `-rt`; only **59** Tridium-authored (`com.tridium.mqttClientDriver` +
> `javax.baja.mqttClientDriver`). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/abstractMqttDriver/abstractMqttDriver-rt/decompiled/…` + pom.properties.
> FUENTE 1 — [B496] (single-SDK-AS-IS pole), [B503] (no-SDK-handrolled pole), [B335] (Gson add-on).
> FUENTE 2 — not consulted (decompilation gap). Evidence delegated to a `sonnet` sweep; ALL load-bearing
> file:line + versions RE-VERIFIED inline.

## §504.1 — SDK identity: the third bundling archetype `[CERT]`

Only **59 / 1975 classes (3%) are Tridium**; 97% is vendored SDKs, shaded into the module JAR:

| Component | Package | ~classes | Version (pom.properties) |
|---|---|---|---|
| AWS IoT Device SDK (Java) | `com.amazonaws.services.iot` | (part of ~833 AWS) | **1.3.11** |
| AWS SDK Secrets Manager | `com.amazonaws.services.secretsmanager` | — | 1.12.698 |
| Jackson (for the AWS SDK) | `com.fasterxml.jackson` | 701 | 2.16.1 |
| Joda-Time | `org.joda.time` | 166 | 2.8.1 |
| Eclipse Paho MQTT **v3** | `org.eclipse.paho.client.mqttv3` | 97 | **1.2.5** |
| JJWT (JWT for GCP) | `io.jsonwebtoken` | — | 0.11.2 |
| **Tridium** | `com.tridium.mqttClientDriver`, `javax.baja.mqttClientDriver` | **59** | 4.14.0.162 |

This completes a **three-point bundling spectrum** across the focus `[INFER]`:
- [B496] `opcUaCore` = ONE big SDK (Prosys) vendored AS-IS;
- [B503] `knxnetIp` = NO SDK, wire stack fully hand-rolled;
- **[B504] `abstractMqttDriver` = MANY SDKs shaded into a fat JAR** — a thin 59-class Tridium orchestrator over
  two transport backends (Eclipse Paho for generic/GCP/Azure; AWS IoT SDK for AWS).

`[INFER]` CVE-blast (as in [B496]): the bundled versions ship byte-for-byte — notably **AWS IoT Device SDK 1.3.11**
(old) and **Paho 1.2.5** — so any CVE in them lands in every N4.14.0.162 install loading this module; the fix is a
Tridium module rebuild.

## §504.2 — Component tree: pluggable cloud authenticators `[CERT]`

```
BAbstractMqttDriverNetwork  (BNNetwork)      — license gate (§504.6)
  BAbstractMqttDevice        (BNDevice)       — one broker connection
    BAbstractMqttAuthenticator (BComponent)   — pluggable auth slot, 5 variants:
       BGenericMqttAuthenticator (TLS/TCP username+password or mTLS)
       BAwsMqttAuthenticator / BAwsJitpMqttAuthenticator (AWS IoT mutual-TLS / JITP)
       BGcpAuthenticator (GCP IoT, JWT via JJWT)
       BAzureMqttSasAuthenticator (Azure IoT SAS token)
    BMqttClientDriverPointDeviceExt
       BMqtt{Numeric,String,Boolean,Enum}Object{Subscribe,Publish}Ext
```

(`BAbstractMqttDriverNetwork.java:68` = `BNNetwork`; `BAbstractMqttDevice.java:89` = `BNDevice`.) **Topic → point:**
each proxy has a `topic` String; a `MqttSubscriberTopics` map keyed by topic dispatches `messageArrived` to matching
subscriber points; wildcard filters are accepted (broker-side matching). "abstract" is literal — concrete OEM/cloud
drivers extend `BAbstractMqttDevice` and add topic schemas.

## §504.3 — MQTT protocol `[CERT]`

**MQTT 3.1.1 only** — the dependency is `org.eclipse.paho.client.mqttv3` (no `mqttv5` classes). Connect via
`MqttAsyncClient(url, clientID, null)` (in-memory persistence), `maxInflight=10000`. QoS 0/1/2 exposed
(`BMqttQualityOfService`), default 0. LWT (last-will) configurable (`enableLWT`/`topicForLWT`/… via
`MqttConnectOptions.setWill()`); retained default true; cleanSession default false (persistent); keep-alive default
60 s; auto-reconnect loop on `connectionLost()` after a 30 s wait.

## §504.4 — Transport: TCP/TLS only `[CERT]`

URL scheme `ssl://` (TLS) or `tcp://` (plaintext), chosen by `enableSSL`. TLS `SSLSocketFactory` comes from Niagara's
`ICryptoManager.getClientSocketFactory(clientTlsParameters)`; default protocol `tlsv1_2` (`BGenericMqttAuthenticator`);
AWS path hardcodes `tlsv1.2` with mandatory mutual TLS. `[CERT negative]` **No WebSocket** (`ws://`/`wss://`) in the
Tridium layer.

## §504.5 — Auth & the port footgun `[CERT]`

Connection types (`BMqttConnectionType`): `Anonymous`(0) / `AnonymousOverSSL`(1) / **`UserLoginOverSSL`(2) = DEFAULT**
(`:51`). Credentials per backend: generic = `BUsernameAndPassword` + optional `BCertificateAliasAndPassword` (both
Niagara-encrypted); AWS = mandatory X.509 client cert; GCP = JWT (RS256/ES256, JJWT-signed); Azure = SAS token. TLS
validation is delegated entirely to Niagara's `ICryptoManager`/`BCertManagerService` — `[CERT negative]` no trust-all
flag in the Tridium layer.

**FOOTGUN `[CERT]`:** the default `connectionType` is `UserLoginOverSSL` (TLS on), but the default `brokerPort` is
**1883** — the *plaintext* MQTT port (`BAbstractMqttAuthenticator.java:42`, TLS port is 8883). `[INFER]`: the shipped
defaults are internally inconsistent — a TLS connect to 1883 fails at runtime until the operator manually changes the
port. Fail-closed (it breaks rather than silently going plaintext), but a configuration trap.

## §504.6 — License gate `[CERT]`

`getFeature("tridium", "mqtt")` (`BAbstractMqttDriverNetwork.java:132`), and the method is **`final`** on the abstract
network — so **every concrete MQTT driver that extends this base inherits the single `tridium:mqtt` gate**; no
per-cloud-variant license. Feature = **`tridium:mqtt`**.

## §504.7 — Payload model `[CERT]`

Point payloads are **plain UTF-8 strings** — numeric = `Double.parseDouble(new String(payload))`, boolean = literal
`"true"`/`"false"`, publish = `getBytes()` of a string. `[CERT negative]` **No Sparkplug B** (no `org.eclipse.tahu`).
Jackson's 701 classes are bundled for the AWS SDK's internal use (Secrets Manager), **not** for point-level JSON — the
abstract base does not JSON-decode payloads (concrete drivers could, via the shaded Jackson).

## §504.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 59/1975 Tridium (3%); multi-SDK shaded: Paho 1.2.5, AWS IoT 1.3.11, Jackson 2.16.1, Joda 2.8.1, JJWT 0.11.2 | `[CERT]` | find counts; pom.properties versions | PASS |
| 2 | BAbstractMqttDriverNetwork=BNNetwork, BAbstractMqttDevice=BNDevice; 5 pluggable authenticators; topic→point map | `[CERT]` | `:68`; `:89`; authenticator classes | PASS |
| 3 | MQTT 3.1.1 only (mqttv3); QoS 0/1/2; LWT/retained/keepalive/reconnect | `[CERT]`/`[CERT neg]` | Paho mqttv3 import; `MqttConnectOptions` | PASS |
| 4 | TCP/TLS only (ssl://tcp://), default tlsv1_2, no WebSocket | `[CERT]`/`[CERT neg]` | `MqttClientPaho`; grep ws=0 | PASS |
| 5 | connectionType default UserLoginOverSSL(TLS) but brokerPort default 1883 (plaintext) = footgun | `[CERT]`+`[INFER]` | `BMqttConnectionType.java:51`; `BAbstractMqttAuthenticator.java:42` | PASS |
| 6 | license `tridium:mqtt`, final on abstract network (inherited by all concrete drivers) | `[CERT]` | `BAbstractMqttDriverNetwork.java:132` | PASS |
| 7 | payloads = plain UTF-8 string; Jackson only for AWS SDK; no Sparkplug B | `[CERT]`/`[CERT neg]` | subscribe-ext parse; grep tahu=0 | PASS |

**Tally:** 7 claims — 5 `[CERT]`/`[CERT negative]` load-bearing + 2 `[INFER]` (CVE-blast, port footgun) on cited code.
Block TYPE = **EVIDENCE**; ratio low, FD10 CLOSED. All load-bearing tokens + SDK versions re-verified inline.

## §504.9 — Connections & focus status

- **Completes the SDK-bundling spectrum** of the focus: single-AS-IS ([B496] Prosys) / none-handrolled ([B503] KNX)
  / **many-shaded** ([B504] AWS+Paho+Jackson+…). This is the recurring architectural axis of framework-drivers —
  how much of a "driver" is Tridium vs a vendored stack.
- **Cloud-connector identity:** the only FD module built for cloud IoT brokers (AWS/GCP/Azure) rather than a field
  bus — its "devices" are cloud endpoints, auth is per-cloud (mTLS/JWT/SAS).
- **Security feed to [B398]/[B490]:** the 1883-vs-TLS-default footgun; old bundled SDK versions (AWS IoT 1.3.11) as a
  supply-chain surface; credentials Niagara-encrypted at rest (consistent with the focus).
- **Focus status:** `framework-drivers` 9/10 (FD1–FD7, FD9–FD10 closed). NEXT = **FD8 `weather`** (last, low-pri),
  then focus-closing synthesis + §18 retro.
