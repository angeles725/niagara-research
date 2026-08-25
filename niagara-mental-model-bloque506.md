# Block 506 — `framework-drivers` SYNTHESIS: the ten Tridium framework protocol drivers as one system — the SDK-bundling spectrum, the driver-vs-Service split, a five-tier security-posture ladder, the license-shape zoo, and the consolidated SEC feed

> **Focus:** `framework-drivers` — focus-closing synthesis (METHODOLOGY §8). Consolidates the 10 blocks
> [B496]–[B505] (FD1–FD10). READ-ONLY. Block TYPE = **SYNTHESIS/DESIGN** (a high `[INFER]` ratio is expected and
> healthy — it cross-references cited evidence rather than adding new decompilation). No new source claims; every
> fact traces to a numbered block. This block does NOT re-derive them — it names the patterns across them.
> **Bootstrapped:** AUDIT-FIRST (§13) 2026-08-25 from `oem-honeywell-tail` U12. **Scope:** the Tridium-authored,
> non-OEM protocol-driver slice; Modbus/SNMP were REMITTANCE ([modbus B294–B315]/[B476]).

## §506.1 — The catalog `[CERT]` (each fact in its block)

| # | Module | Block | Baja shape | Bundling | In-band security | License |
|---|---|---|---|---|---|---|
| FD1 | opcUaCore | [B496] | shared SDK carrier | **1 SDK AS-IS** (Prosys 5.1.0-116) | (config types) | none (in core) |
| FD2 | opcUaClient | [B497] | BNNetwork driver | thin (uses FD1) | TLS + cert + user token; **default STRONG** | `tridium:opcUaClient` |
| FD3 | opcUaServer | [B498] | BNNetwork driver | thin (uses FD1) | TLS + RBAC map; writable-by-default, cert=no-RBAC | `tridium:opcUaServer` |
| FD4 | obixDriver | [B499] | BDeviceNetwork + obix-rt | shared lib | TLS **optional** (default http+Basic) | `tridium:obixDriver` + foreign-limits + export |
| FD5 | mbus | [B500] | basicDriver serial | none | **none** (plaintext, no EN13757-3) | `tridium:mbus` + serial/tcpip |
| FD6 | openAdr | [B501] | **BService** (TridiumPS) | none (hand XElem) | TLSv1.2-min; Basic uncond.; **no XMLDSig** | `Tridium:openADR2b` + ven.limit |
| FD7 | opc (DA) | [B502] | BDeviceNetwork + JNI | opc.dll (native) | **OS/DCOM-delegated** (no in-band) | `tridium:opc` |
| FD8 | weather | [B505] | **BService** | none (hand HTTP) | plaintext default; **cleartext key** | **none** |
| FD9 | knxnetIp | [B503] | BDeviceNetwork (com.tridiumX) | **NO SDK, hand-rolled** | **none** (no KNX Secure) | `tridium:knxnetIp` + capacity quotas |
| FD10 | abstractMqttDriver | [B504] | BNNetwork abstract base | **N SDKs shaded** (Paho/AWS/…) | TLS + per-cloud auth; **1883-vs-TLS footgun** | `tridium:mqtt` (final, inherited) |

## §506.2 — Axis 1: the SDK-bundling spectrum (the focus's central finding) `[INFER]`

The recurring question "how much of a Tridium *driver* is actually Tridium?" has **three poles**:
- **One SDK, vendored AS-IS** — [B496] opcUaCore: 16 Tridium classes carrying the ~2900-class Prosys SDK
  unchanged; [B499] obixDriver similarly rides the shared `obix-rt`; [B502] opc rides the native `opc.dll`.
- **No SDK, fully hand-rolled** — [B503] knxnetIp: 189 Tridium classes implementing the entire KNXnet/IP wire
  stack; [B501] openAdr, [B505] weather, [B500] mbus also hand-write their protocol over Niagara's own
  XML/HTTP/serial APIs.
- **Many SDKs, shaded into a fat JAR** — [B504] abstractMqttDriver: 59 Tridium classes (3%) over Paho + AWS IoT +
  Jackson + Joda + JJWT.

`[INFER]` **Security consequence** (the CVE-blast, [B496 §496.2]/[B504 §504.1]): AS-IS and shaded bundling ship the
vendored versions byte-for-byte — a CVE in Prosys 5.1.0-116, Paho 1.2.5, or AWS IoT SDK 1.3.11 lands in every
N4.14.0.162 install and can only be patched by a Tridium module rebuild. Hand-rolled modules (KNX) carry no such
inherited surface but reimplement crypto/parsing themselves.

## §506.3 — Axis 2: driver-network vs Service `[CERT]`

Most framework drivers are `BDeviceNetwork`/`BNNetwork` → device → point-proxy trees (FD2/FD3/FD4/FD5/FD7/FD9/FD10).
**Two are Services** (`BAbstractService`, no device/point model): [B501] openAdr and [B505] weather — both are
outbound *clients to a single logical endpoint* (a VTN; a weather feed) rather than managers of many field
devices, and both surface their data as component slots the integrator links, not as pollable point proxies.
`[INFER]`: "driver" in the module names is a Baja-shape claim only for the network-based ones.

## §506.4 — Axis 3: the five-tier security-posture ladder `[INFER]`

From strongest to weakest in-band transport/auth:
1. **In-band strong** — OPC UA client [B497] (default Basic256Sha256, cert-validated).
2. **In-band present but flawed by default** — OPC UA server [B498] (Basic128Rsa15 offered, username token under
   `SecurityPolicy.NONE`, writable nodes, cert sessions with no RBAC); openAdr [B501] (TLSv1.2-min but Basic
   unconditional and **no XMLDSig**); obixDriver [B499] (**default `http://` lobby + Basic** = creds in clear).
3. **OS-delegated** — classic OPC DA [B502] (all auth is Windows/DCOM; no in-band security).
4. **Plaintext by protocol generation** — mbus [B500] (no EN 13757-3) and knxnetIp [B503] (no KNX Secure) — both
   predate their standard's crypto; not Tridium defects.
5. **Plaintext + weak at-rest** — weather [B505] (plaintext HTTP **and** a cleartext-`String` API key).

`[CERT]` **Credential-at-rest** is `BPassword` (reversible-but-encrypted) in every module that holds one — EXCEPT
weather's AirNow key, a plain `String` ([B505 §505.5]): the one at-rest outlier. Trust for the TLS/cert modules is
uniformly Niagara's file-store enrollment ([B392]/[B398] model), not CA-chain enforcement.

## §506.5 — Axis 4: the license-shape zoo `[CERT]`

All gates are `getFeature("tridium", <feature>)` on the network/service, but the *shape* varies widely:
- **boolean** presence — opc [B502];
- **per-transport sub-keys** — mbus serial/tcpip [B500];
- **per-role split** — opcUa client vs server [B497]/[B498];
- **foreign-device/point limits + export** — obix [B499];
- **numerical capacity quotas** (installations, interfaces) — knxnetIp [B503] (strictest);
- **VEN count** — openAdr `ven.limit` [B501];
- **dynamic module-name feature** — knxnetIp again;
- **final-on-abstract-base, inherited by all concrete drivers** — mqtt [B504];
- **NONE** — weather [B505] (the only ungated module).
`[INFER]`: license shape roughly tracks commercial value (metered per-connection for field buses and cloud;
free for the weather value-add).

## §506.6 — Provenance is not uniform `[CERT]`

Three publisher namespaces appear: **core `com.tridium.*`** (most), **`com.tridiumps.*` = TridiumPS** add-on
(openAdr [B501], sibling of [B350] electronicSignature), and **`com.tridiumX.*`** (knxnetIp [B503] — capital-X,
distinct from the lowercase `com.tridiumx` add-on family of [B335]/[B350]). `[INFER]`: "a Tridium framework driver"
spans core, professional-services, and separately-packaged tiers — a packaging distinction invisible from the
module list alone.

## §506.7 — Consolidated SEC feed (to [B398]/[B490]) `[INFER]`

New driver-level items this focus surfaces for the security catalog (all cited to their blocks):
- SEC — **obix default http+Basic** ([B499]): plaintext credentials unless operator sets https.
- SEC — **opcUa server writable-by-default nodes + username token under NONE + cert-session-no-RBAC** ([B498]).
- SEC — **openAdr Basic-over-http path + no payload signing** ([B501]).
- SEC — **weather cleartext API key + plaintext feeds** ([B505]).
- SEC — **mqtt 1883-vs-TLS default footgun + old shaded SDKs** ([B504]).
- SEC — **classic-OPC DCOM exposure** (hardening entirely outside Niagara) ([B502]).
- SEC — **supply-chain**: vendored SDK versions ship byte-for-byte (Prosys/Paho/AWS/Jackson) ([B496]/[B504]).

## §506.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | 10 modules FD1–FD10 catalogued with shape/bundling/security/license | `[CERT]` | [B496]–[B505] (§506.1) | PASS |
| 2 | three-pole SDK-bundling spectrum (AS-IS / none / shaded) | `[INFER]` | [B496],[B503],[B504] | PASS |
| 3 | two Service-tier (openAdr, weather) vs network-tier rest | `[CERT]` | [B501],[B505] | PASS |
| 4 | five-tier security ladder; BPassword at-rest except weather cleartext | `[CERT]`+`[INFER]` | per-block §s | PASS |
| 5 | license-shape zoo incl. weather=none | `[CERT]` | per-block license §s | PASS |
| 6 | three provenance namespaces (tridium/tridiumps/tridiumX) | `[CERT]` | [B501],[B503] | PASS |

**Tally:** 6 claims — 4 rest on `[CERT]` block citations, 2 are `[INFER]` cross-cutting patterns. SYNTHESIS block:
high-INFER-is-healthy; adds no new source claims. This CLOSES the `framework-drivers` focus.

## §506.9 — Focus closure & open threads

- **`framework-drivers` STOPPED — 10/10 investigable driver gaps closed** (B496–B505 + this synthesis B506).
  REMITTANCE held: Modbus ([modbus B294–B315]), SNMP ([B476]) not reopened.
- **Recorded-but-not-seeded future gaps** (each noted in its block, none investigable-blocking):
  `obix-rt` transport + `BObixServer` export ([B499]); `basicDriver` reference block ([B500]);
  `com.tridiumX` packaging identity ([B503]); optional §12 live checks (opcUa-server writable exposure [B498];
  NWS-feed live status [B505]) = requires-execution, not blocking.
- **Cross-focus:** feeds [B398]/[B490] security catalog (§506.7); the trust model reuses [B392]/[B398];
  opc meets [B127]/[B132] at the JNI line.
- **NEXT:** §18 self-retrospective, then push. No read-only-investigable work remains in this focus.
