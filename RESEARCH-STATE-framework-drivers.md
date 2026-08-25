# RESEARCH-STATE — focus: framework-drivers (PLANNED)

> Multi-focus corpus (METHODOLOGY §16). This focus was SEEDED by an AUDIT-FIRST coverage sweep (§13),
> NOT hand-guessed — see the coverage matrix in the iteration history below (2026-08-25). It grows out of
> `oem-honeywell-tail` gap **U12** ("Tridium framework drivers not deep-distilled"): the protocol driver
> modules that the corpus lists in passing but never opened as dedicated blocks. SNMP was already done
> ([Block 476]); Modbus is fully covered by the closed `modbus` focus (B294–B315) → **REMITTANCE, not
> re-opened here**.
>
> **Angle (§b2):** decompiled-Java driver modules under `organized/<module>/*-rt|-wb|-ux/vineflower/`.
> READ-ONLY. Corpus language for NEW blocks = **English** (post-B115 convention).
>
> Scope note: this is the *framework* (Tridium-authored, non-OEM) protocol-driver slice of U12. Each gap is
> one driver module (or a tight family). The OEM-Honeywell drivers are their own closed threads; the
> other-vendor OEM drivers were surveyed in [Block 495] (U10). Out of THIS focus: `weatherUnderground`
> (1 class, thin), and pure connector stubs (recorded as dismissed below).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 6
gaps_closed: 6
known_gaps: 10
investigable_open: 4
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: framework-drivers
status: active
seeded_from: AUDIT-FIRST coverage sweep 2026-08-25 (delegated sonnet; verified inline)
seeded_on: 2026-08-25
gaps_total: 10 investigable (FD1–FD10)
gaps_closed: 6 (FD1→B496 … FD5→B500, FD6→B501)
blocks_written: B496–B500 (FD1–FD5), B501 (FD6)
block_prefix: niagara-mental-model-bloqueN.md (shared global numbering)

## Gap-backlog (prioritized) — from the AUDIT-FIRST coverage matrix

All "Where" paths are under `/home/cristian/niagara-research/organized/`. Distinct-class counts are the
vineflower `.java` count over the module tree (single decompiler tree ⇒ raw ≈ distinct); RE-MEASURE before
using any count as a denominator (GAP NUMBERS ARE HYPOTHESES). All 8 candidate dirs existence-verified
2026-08-25.

| Priority | Gap | Where | Status |
|---|---|---|---|
| high | **FD1 opcUaCore** — the OPC UA stack anchor: what Tridium actually authored (~16 cls) vs the bundled **Prosys OPC UA Java SDK** (`com.prosysopc.ua.*`, ~2900 cls); SDK bundling strategy + inherited SDK security surface (SecureChannel / CertificateValidator / UserIdentityToken) | `opcUaCore-rt` (3114 vf, 16 Tridium) | **COVERED → B496** (16-class type-shim; Prosys SDK 5.1.0-116 + HttpCore NIO 4.4.13 vendored AS-IS; no license gate in core; footguns: Basic128Rsa15+TLS1.0/1.1 on by default, `ALL`≡username-only, self-signed accepted iff trust-store-enrolled, makeSecurityModes bits=0→NONE) |
| high | **FD2 opcUaClient** — client-side driver: session establishment, secure-channel lifecycle, certificate validation, user auth (anonymous / username / X.509) | `opcUaClient-rt,-wb` (46 vf) | **COVERED → B497** (BNNetwork tree; client uses single-select BSecurityMode enum default=Basic256Sha256 STRONG, NOT the B496 bitstrings [§14 refine]; BPassword plaintext-recoverable; wires B496 cert listener, no trust-all; license `tridium:opcUaClient` [closes B496 reverse-backlog]; UI: None/anonymous behind one confirm) |
| high | **FD3 opcUaServer** — server-side exposure (symmetric with FD2): what N4 exposes as an OPC UA server, endpoint/security policy, auth surface | `opcUaServer-rt,-wb` (47 vf) | **COVERED → B498** (endpoint :52520, uses B496 bitstrings; default mode=6 no-NONE but policies=7 Basic128Rsa15-on; username token under SecurityPolicy.NONE; anonymous=return-true if enabled; username→Niagara RBAC via BOpcUaAuthenticationScheme, cert session=NO RBAC; nodes writable-by-default; license `tridium:opcUaServer`) |
| high | **FD4 obixDriver** — oBIX REST/HTTP BAS-protocol driver (largest fully-uncovered module): component/point model, session + auth surface, XML/JSON encoding. NOT the oBIX *usage* in Reflow/chihuahua (that is app-level) | `obixDriver-rt,-wb` (141 vf) | **COVERED → B499** (network→client→proxy over shared obix-rt ObixSession; REST GET/PUT/POST + Watch model 2s; HEADLINE: HTTP Basic over default http:// lobby → creds base64-in-clear unless operator sets https; authPass=BPassword at rest; license tridium:obixDriver + foreignDevice/foreignPoint.limit + export; XXE unconfirmed [baja-rt XParser]; dual role incl. BObixServer export) |
| medium | **FD5 mbus** — M-Bus (EN 13757) energy-metering driver: serial/IP transport, device/point model, decode of metering telegrams | `mbus-rt,-wb` (118 vf) | **COVERED → B500** (rides basicDriver base; serial MbusSerialComm 8E1/300-baud + TCP MbusSocketComm gateway 192.168.1.10:6021; SND_NKE/REQ_UD2→RSP_UD cycle, Java-array DIF/VIF decoder NOT XML; poll 30/45/90s + primary/secondary/live-point discovery; license tridium:mbus + serial/tcpip sub-keys; SEC: plaintext-only, ZERO EN 13757-3 encryption [crypto grep=0], icmpPing stub) |
| medium | **FD6 openAdr** — OpenADR 2.0 demand-response client: VEN/VTN model, event handling, energy-grid signalling (distinct from every existing focus) | `openAdr-rt` (85 vf) | **COVERED → B501** (TridiumPS add-on com.tridiumps.openadr; Service-tier NOT driver; both 2.0a/2.0b hand-rolled XElem no-SDK; HTTP-pull-only simpleHttp/oadrPoll poll 60s no-XMPP; event FSM→BActiveEiEventSignal.currentValue [integrator links]; SEC: TLSv1.2-min + client-cert-optional + UNCONDITIONAL Basic auth [http+https] + VTNPassword=BPassword; NO XMLDSig payload signing; license Tridium:openADR2b+ven.limit) |
| medium | **FD7 opc (classic DA)** — the Java driver-component layer of classic OPC DA (point-proxy model, COM/DCOM session lifecycle). The NATIVE side (opc.dll/opcproxy JNI shim, COM boundary) is already in [B127]/[B132] — this is the uncovered Java tree | `opc-rt,-wb` (64 vf) | pending |
| low | **FD8 weather** — Tridium weather integration driver (lower security relevance; architecturally clean; completeness) | `weather-rt,-ux,-wb` (52 vf) | pending |
| medium | **FD9 knxnetIp** — KNXnet/IP building-automation driver (BEYOND original U12 list — surfaced by the audit; fits the framework-driver theme, sizeable) | `knxnetIp-rt,-wb` (325 vf) | pending |
| medium | **FD10 abstractMqttDriver** — MQTT driver base (BEYOND original U12 list): likely bundles an MQTT SDK the way opcUaCore bundles Prosys — verify the Tridium-vs-SDK split first | `abstractMqttDriver-rt,-wb` (1978 vf) | pending |

### REMITTANCE (already covered — will NOT be opened)

- `modbusCore` / `modbusAsync` / `modbusTcp` / `modbusSlave` (+ `modbusTcpSlave`, `modbusTcpSlaveMigrator`) —
  all inside the closed **`modbus` focus (B294–B315)**, confirmed against `RESEARCH-STATE-modbus.md`.
- SNMP (`nSnmp`) — done by [Block 476] (see `oem-honeywell-tail` U12 note).

### Dismissed file types / modules (recorded, NOT gaps)

- `weatherUnderground-rt` — 1 class; connector stub, no distinct driver logic. Dismissed as THIN.
- `bacnetOws` (14 cls), `basicDriver` (24 cls) — spotted in the audit; small. `basicDriver` may later serve
  as a REFERENCE block for the driver base-class contract, but neither is a priority gap. Recorded, not seeded.

## Stop control (METHODOLOGY §8)

- **Open gaps — read-only investigable**: **4** (FD7–FD10). All existence-verified; source present.
- **Gaps closed**: 6 (FD1→B496 … FD5→B500, FD6→B501).
- **requires-execution / blocked**: 0. (Optional §12 live-confirm of server writable-node exposure noted in
  B498 §498.8 — NOT registered as blocking; the investigable set stands.)
- **Coverage metric**: 6 / 10 investigable gaps closed.
- **NEXT**: FD7 `opc` (classic DA). Then FD9 → FD10 → FD8.
- **POSSIBLE future gap (recorded, NOT seeded):** dedicated `obix-rt` transport/`Obj` model + `BObixServer`
  export block — out of FD4's driver scope (B499 §499.8).
- **RESOLVED (was REVERSE-BACKLOG from B496 §496.6):** OPC UA license split confirmed per-role —
  `tridium:opcUaClient` (B497) / `tridium:opcUaServer` (B498), neither in the shared core.

## Iteration history

| It | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| it.0 (bootstrap) | 2026-08-25 | — (AUDIT-FIRST seed) | — | FD1–FD8 from the U12 candidate list (modbus* = REMITTANCE, weatherUnderground = THIN); FD9 (knxnetIp) + FD10 (abstractMqttDriver) surfaced BEYOND the original U12 list. Coverage matrix delegated to a `sonnet` general-purpose sweep (22 tool-uses), then existence + class-counts + modbus-REMITTANCE re-verified inline by the driver. |
| it.1 | 2026-08-25 | **FD1** opcUaCore — 16-class Tridium type-shim + Prosys SDK 5.1.0-116 + HttpCore NIO 4.4.13 vendored AS-IS; no core license gate; 4 security-default footguns + self-signed-if-enrolled cert model. Delegated `sonnet` sweep (21 tool-uses, structural), then ALL load-bearing file:line re-verified inline against real line numbers (sweep offsets were normalized/wrong → discarded). §14: none. verify-block exit 0, ratio 0.36 EVIDENCE. | **B496** | none net-new (REVERSE-BACKLOG note: license gate expected in FD2/FD3) · yes · sonnet |
| it.2 | 2026-08-25 | **FD2** opcUaClient — BNNetwork driver; client uses single-select BSecurityMode enum (default Basic256Sha256 STRONG), NOT the B496 bitstrings → **§14 REFINE of B496** (back-pointer added B496 §496.5); BPassword plaintext-recoverable; wires B496 cert listener; license `tridium:opcUaClient` (closes B496 reverse-backlog); UI None/anonymous behind one confirm. `sonnet` sweep (12 tool-uses), all load-bearing re-verified inline (offsets discarded). verify-block exit 0, ratio 0.27. | **B497** | none net-new · yes · sonnet |
| it.3 | 2026-08-25 | **FD3** opcUaServer — BNNetwork; exposes component space as UA address space (opt-in per BOpcUaServerProxyExt), endpoint :52520; uses B496 bitstrings (default mode=6 no-NONE, policies=7 Basic128Rsa15-on); username token under SecurityPolicy.NONE; anonymous=return-true if enabled; username→Niagara RBAC via BOpcUaAuthenticationScheme, cert session=NO RBAC; nodes writable-by-default; license `tridium:opcUaServer`. `sonnet` sweep (10 tool-uses, decompiled/ tree), all load-bearing re-verified inline. §14: none new (co-refines B496 w/ FD2). verify-block exit 0, ratio 0.59 EVIDENCE (security-heavy [INFER], each cited). Optional §12 live-confirm noted, NOT blocking. | **B498** | none net-new · yes · sonnet |
| it.4 | 2026-08-25 | **FD4** obixDriver — network→client→proxy over shared obix-rt ObixSession (REST GET/PUT/POST, lobby-ref discovery, Obj↔XML 1:1); Watch model default 2s + polling fallback; HEADLINE: HTTP Basic auth over default `http://` lobby → creds base64-in-clear unless operator opts https; authPass=BPassword at rest (not plaintext); license tridium:obixDriver + foreignDevice/foreignPoint.limit + export; XXE unconfirmed (baja-rt XParser); dual role (BObixServer export noted). `sonnet` sweep (24 tool-uses, decompiled/ + obix-rt vineflower), all load-bearing re-verified inline. verify-block exit 0, ratio 0.24. | **B499** | none net-new (obix-rt/BObixServer = possible future gap, recorded not seeded) · yes · sonnet |
| it.5 | 2026-08-25 | **FD5** mbus — M-Bus (EN 13757) meter driver over the shared `basicDriver` base (BSerialNetwork/BBasicDevice/BBasicProxyExt); serial 8E1/300-baud + TCP gateway; SND_NKE(0x40)/REQ_UD2(0x5B, FCB toggle)→RSP_UD, DIF/VIF decoder in Java arrays (NOT XML); poll 30/45/90s + primary/secondary-wildcard/live-point discovery; license tridium:mbus + serial/tcpip sub-keys; SEC headline: plaintext-only, ZERO EN 13757-3 encryption (crypto grep=0), "password" VIF=data-descriptor not credential, icmpPing stub. `sonnet` sweep (29 tool-uses, decompiled/), all load-bearing re-verified inline. verify-block exit 0, ratio 0.12. | **B500** | none net-new (basicDriver reference block = possible future gap, recorded not seeded) · yes · sonnet |
| it.6 | 2026-08-25 | **FD6** openAdr — TridiumPS add-on (com.tridiumps.openadr, sibling of electronicSignature B350); OpenADR 2.0 VEN modelled as a Service (BOadrService/BOadrVen=BAbstractService), NOT a driver; both 2.0a/2.0b hand-rolled XElem, no JAXB/SDK; HTTP-pull-only (simpleHttp/oadrPoll, poll 60s), no XMPP; event FSM (far/near/active/…) via ActiveEventUpdateThread → writes BActiveEiEventSignal.currentValue (integrator links to a point). SEC: TLSv1.2-min forced, client-cert optional, HTTP Basic UNCONDITIONAL (http+https paths), VTNPassword=BPassword; **NO XMLDSig payload signing** (oadrSignedObject=plain wrapper) → integrity channel-only. License Tridium:openADR2b + ven.limit. `sonnet` sweep (nested sub-sweeps), all load-bearing re-verified inline (vineflower). verify-block exit 0, ratio 0.40. | **B501** | none net-new · yes · sonnet |
