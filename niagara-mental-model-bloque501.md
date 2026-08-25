# Block 501 — `framework-drivers` FD6: `openAdr` — the TridiumPS OpenADR 2.0 VEN (a Service-tier, not a driver-network; hand-rolled XElem for both 2.0a/2.0b, HTTP-pull-only), its event state machine writing `BActiveEiEventSignal.currentValue`, and its security posture: TLSv1.2-min + unconditional HTTP Basic + **no XMLDSig payload signing**

> **Focus:** `framework-drivers`, gap **FD6** — the OpenADR 2.0 demand-response client (~85 cls). VEN = Virtual
> End Node (client=N4) receiving DR events from a VTN (utility). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/openAdr/openAdr-rt/{vineflower,decompiled}/…`. FUENTE 1 — [B497]/[B499]
> (Basic-auth pattern), [B350-356] (the sibling TridiumPS add-on `electronicSignature`), [B392] (PKI/signing).
> FUENTE 2 — not consulted (decompilation gap). Evidence delegated to a `sonnet` sweep (which nested its own
> sub-sweeps); ALL load-bearing file:line RE-VERIFIED inline (vineflower tree; offsets discarded).
> **Provenance:** package root **`com.tridiumps.openadr`** — a **TridiumPS** (Tridium Professional Services)
> add-on, NOT core Tridium, same namespace family as `electronicSignature` [B350]. `[CERT]`

## §501.1 — Component tree: a Service, not a driver `[CERT]`

Unlike every other FD block, openAdr is **Service-tier** — no `BDeviceNetwork`/`BDevice`:

```
BOadrService  extends BAbstractService (impl BIRestrictedComponent)   — holds VEN children, license gate
  └─ BOadrVen (abstract) extends BAbstractService
       ├─ BOadrVen2A  — Profile 2.0a
       └─ BOadrVen2B  — Profile 2.0b (adds registration + reports)
            └─ events: BAllEvents → venEvents: BVenEvents → [dynamic BOadrEvent children]
                 └─ eiEvent: BEiEvent (eventDescriptor / eiActivePeriod / eiTargets)
```

(`BOadrService.java:49-50`; `ven/BOadrVen.java:132` `abstract … extends BAbstractService`.) `[INFER]`: modelling
a comms client as a `BService` (not a driver network) is unusual — it means there is no device-status/ping/poll
framework around it; the VEN manages its own poll timer.

## §501.2 — Both profiles, hand-rolled XElem, no SDK `[CERT]`

Both OpenADR 2.0 profiles are implemented in separate packages: **2.0a** (namespace `…/oadr-2.0a/2012/07`,
service `/OpenADR2/Simple/EiEvent`, EiEvent only) and **2.0b** (all five services). Services (2.0b):
EiRegisterParty (`oadrCreatePartyRegistration`/`oadrQueryRegistration`), EiEvent (`oadrRequestEvent`/
`oadrCreatedEvent`), EiReport (`oadrRegisterReport`/`oadrUpdateReport`), EiOpt (`oadrCreateOpt`/`oadrCancelOpt`),
OadrPoll (`oadrPoll`).

**Payloads are hand-built with `javax.baja.xml.XElem`** — `[CERT negative]`: zero `javax.xml.bind`/`@XmlRootElement`/
`JAXBContext`, no bundled `.xsd`/`.wsdl`, and **no vendored OpenADR SDK** (no enernoc/external refs). This is the
**opposite** of FD1's opcUaCore Prosys-SDK pattern: all protocol logic is hand-written Tridium code over the
Niagara XML API.

## §501.3 — Transport: HTTP pull only `[CERT]`

`oadrTransportName = "simpleHttp"`, `oadrHttpPullModel = true` (`ven/BOadrVen.java:135`). **No XMPP** — `[CERT
negative]` grep `smack|xmpp|jabber` = 0. The VEN polls the VTN via `oadrPoll` over `HttpURLConnection` (HTTP) or
`javax.baja.net.HttpsConnection` (TLS). `pollFrequency` default **60 s**, min 1 s (`ven/BOadrVen.java:142`); after
registration the VTN-supplied `oadrRequestedOadrPollFreq` overrides it. Poll loop via `Clock.schedulePeriodically`.

## §501.4 — Event handling: state machine → a signal property `[CERT]`

Enums: `BSignalName` (10: simple/electricity_price/…/load_control), `BSignalType` (11: level/price/setpoint/…),
`BEventStatus` (6: none/far/near/active/completed/cancelled), `BOptValue` (optIn default/optOut), `BOptReason`
(economic/emergency/mustRun/…). The **SIMPLE 0-3 DR levels** map to `signalName=simple`, `signalType=level`,
integer payload 0-3.

`ActiveEventUpdateThread` (a `Runnable` scheduled at absolute boundary times by `BEventInterface`) classifies each
event against boundary timestamps computed in `BEiActivePeriod` (far/near/active/completed from
dtstart/dtend ± randomization ± rampUp/recovery), updates `eventStatus`, and writes the signal:

```
ActiveEventUpdateThread.java:371
  ((BActiveEiEventSignal)…).setCurrentValue(new BStatusNumeric(interval.getPayload()));
```

`[CERT]` **The module does NOT drive a control point** — it only writes `BActiveEiEventSignal.currentValue`
(`BStatusNumeric`, system-managed). `[INFER]`: the integrator must wire a Niagara **link** from that property to a
downstream `BWritablePoint`; there is no `drive()`/`setOut()` in the pipeline. Opt-out events are skipped and their
active signals cleared (`ActiveEventUpdateThread.java:173-186`); EiOpt sends `oadrCreateOpt`/`oadrCancelOpt`.

## §501.5 — Security / auth: TLSv1.2-min + unconditional Basic, but NO payload signing `[CERT]`

- **TLS:** minimum protocol forced to **TLSv1.2** at startup (`ven/BOadrVen.java:293` `setHttpsMinProtocol(tlsv1_2)`);
  HTTPS path builds an `SSLSocketFactory` from Niagara's `ICryptoManager`. Module manifest declares `SSLSockets=true`.
- **Client certificate:** OPTIONAL — `clientCertificateAlias` (String, default `""`, `ven/BOadrVen.java:144`); when
  set, `ClientTlsParameters` presents that keystore cert in the handshake; when empty, server-auth-only TLS.
- **HTTP Basic, unconditional:** built on BOTH the HTTP (`ven/BOadrVen.java:544-546`) and HTTPS
  (`:607-609`) POST paths — `"Basic " + Base64(VTNUsername + ":" + VTNPassword.getValue())` on `Authorization`,
  with **no guard** (blank creds still send `Basic <b64 of ":">`). `VTNPassword` is a `BPassword` (`:140`,
  encrypted at rest). `[INFER]`: because an HTTP (non-TLS) `postMsg` path exists, a VTN URL with scheme `http`
  sends the Base64 Basic credential in the clear — the same transport footgun as oBIX [B499] — though OpenADR 2.0
  mandates TLS and the TLSv1.2-min applies once `https` is used.
- **Payload signing (XMLDSig): NOT implemented** `[CERT negative]` — grep `XMLDSig|SignedInfo|DigestValue|
  javax.xml.crypto.dsig` = 0. The `oadrSignedObject` element is emitted as a **plain schema-compliance wrapper**
  only; no signature is generated or verified. `[INFER]`: OpenADR 2.0b's optional XML-signature integrity layer is
  absent, so message authenticity/integrity rests entirely on the TLS channel (and, if configured, mutual-TLS
  client cert) — there is no application-layer signing of DR events or reports. Notable given [B392]'s emphasis on
  N4's signing surfaces; this add-on opts out of the OpenADR signing option.
- **IDs:** registrationId/VTNId/VENId are plain `String` properties.

## §501.6 — License gate `[CERT]`

`getFeature("Tridium", "openADR2b")` (`BOadrService.java:144,194,199`), attribute **`ven.limit`** —
`"none"`=unlimited, integer=cap, else 0. Per-VEN enforcement blocks adding VENs past the cap
(`BOadrVen` licenseLimit checks). Feature = **`Tridium:openADR2b`** (note vendor cased `Tridium`, and a single
`2b` feature covers both profiles).

## §501.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | TridiumPS add-on (com.tridiumps.openadr); Service-tier (BOadrService/BOadrVen = BAbstractService), not a driver | `[CERT]` | `BOadrService.java:49-50`; `ven/BOadrVen.java:132` | PASS |
| 2 | both 2.0a+2.0b, hand-rolled XElem, no JAXB, no vendored SDK | `[CERT]`/`[CERT neg]` | profile pkgs; grep JAXB/SDK=0 | PASS |
| 3 | HTTP pull only (simpleHttp/oadrPoll), no XMPP; poll default 60s | `[CERT]`/`[CERT neg]` | `ven/BOadrVen.java:135,142`; grep xmpp=0 | PASS |
| 4 | event FSM (far/near/active/…) → writes BActiveEiEventSignal.currentValue, no direct point drive | `[CERT]`+`[INFER]` | `ActiveEventUpdateThread.java:371` | PASS |
| 5 | TLSv1.2-min; client cert optional; Basic auth unconditional (HTTP+HTTPS); VTNPassword=BPassword | `[CERT]` | `ven/BOadrVen.java:293,144,544-546,607-609,140` | PASS |
| 6 | XMLDSig payload signing NOT implemented (oadrSignedObject = plain wrapper) | `[CERT negative]` | grep dsig=0; `OadrMessage` wrapper | PASS |
| 7 | license `Tridium:openADR2b` + ven.limit | `[CERT]` | `BOadrService.java:144` | PASS |

**Tally:** 7 claims — 5 `[CERT]`/`[CERT negative]` load-bearing + 3 `[INFER]` (service-tier consequence,
http-Basic exposure, signing-absent consequence) on cited code. Block TYPE = **EVIDENCE**; ratio moderate,
FD6 CLOSED. All load-bearing tokens re-verified inline.

## §501.8 — Connections & focus status

- **TridiumPS provenance** ties it to [B350-356] `electronicSignature` (same `com.tridiumps.*` add-on family) —
  a second data point that TridiumPS ships hand-rolled, non-core protocol/compliance modules.
- Security across the focus: openAdr enforces **TLSv1.2-min** (stronger than oBIX's unenforced TLS [B499]) but
  **omits OpenADR's XML-signature layer** — integrity is channel-only. Feed to [B398]/[B490]: a VEN pointed at an
  `http://` VTN leaks Basic creds (oBIX-class footgun); XMLDSig-absent means no app-layer replay/tamper protection
  on DR events.
- Architectural outlier: the only FD module modelled as a `BService` rather than a driver network.
- **Focus status:** `framework-drivers` 6/10 (FD1–FD6 closed). NEXT = FD7 `opc` (classic DA Java tree).
