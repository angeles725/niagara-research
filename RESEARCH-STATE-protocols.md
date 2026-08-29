# Niagara N4 Communication Protocols — Research State

> Focus: wire-level / encoding-grade reverse-engineering of Niagara N4 communication
> PROTOCOLS as implemented in the shipped Java driver modules — the actual frame
> structure, opcodes, PDU encoding, framing and on-the-wire semantics, NOT the
> architecture/integration view (already covered by B7/B19/B23/B77/B93/B94/B120/B127).
> READ-ONLY. Corpus language: ENGLISH.
> Source root: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/` (977 module jars).
> Tools: `research-sdd/toolbelt/decompile-java.sh` (Vineflower/CFR/Procyon) + javap + strings.
> Mirrored in engram: `research/niagara/protocols-gaps`, `research/niagara/protocols-progress`.
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 606
gaps_closed: 7
known_gaps: 6
investigable_open: 0
requires_execution_open: 5
blocked_open: 0
<!-- /research-state.v1 -->


## Why this focus exists

A 2026-06-29 protocol-coverage audit over the existing 130-block corpus found the
investigation was rigorous at ARCHITECTURE/integration level but SPARSE at WIRE level.
This focus closes the STATIC-investigable wire-level gaps (decompilable from the jars).
Three gaps were rated MAJOR by the audit: Modbus frame structure (zero coverage),
OPC DA/UA encoding (zero coverage), BACnet APDU service-PDU encoding (object model
covered, packet encoding not). Fox and LON have partial wire coverage to deepen.

## Coverage

- **Covered blocks (this focus)**: 6 — B131 (Modbus wire-level), B132 (OPC-UA binary + DA/COM boundary), B133 (BACnet APDU encoding + segmentation), B134 (Fox/Foxs protocol wire + SCRAM-SHA-256 auth), B135 (LON NV wire encoding + SNVT scaling + NI application buffer + file transfer), B136 (Sox/Soxs absence audit — negative finding, STATIC loop closed).
- **Coverage metric**: 6 / 6 backlog items closed.
- **Last iteration**: 2026-06-29 — P6 Sox/Soxs presence audit closed (B136); **STATIC loop STOPPED**.

> **Applied-integration coda (2026-06-29, B137) — does NOT change the 6/6 STOP.** B137 is an APPLIED/
> DESIGN block (Nivel 1), not a new static-wire gap: it consumes B131's Modbus wire findings to produce a
> LOGO! 8 ↔ Niagara Modbus TCP **integration plan** (driver config tree from the `modbusTcp`/`modbusCore`
> CONFIG classes + register-map crosswalk against the logosoft LOGO! corpus B66-B76 + byte-order
> reconciliation + Workbench recipe). It REFINES B131 §131.9 (network/device byte-order PROPERTY default
> is `order3210`/big-endian, not the `order1032` enum-DEFAULT constant). **Empirical validation is
> DEFERRED** to a future DYNAMIC phase against the bench LOGO (`192.168.0.100:502`, left Modbus-enabled);
> until then the interoperability conclusion is `[INFER]`. The static protocols backlog remains **6/6
> CLOSED, loop STOPPED**.

## Gap-backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **P1** | Modbus wire-level: MBAP TCP header (transaction/protocol/length/unit id), function-code PDU encoding (FC 01-23 read/write coils/registers), RTU framing + CRC-16, register/coil/input addressing & byte/word order | `modbusCore-rt.jar`, `modbusTcp-rt.jar`, `modbusTcpSlave-rt.jar`, `modbusAsync-rt.jar` | **COVERED → B131** |
| high | **P2** | OPC wire encoding: OPC-UA binary encoding (variant types, NodeId, service request/response framing) + legacy OPC DA COM variant marshalling boundary | `opcUaCore-rt.jar`, `opcUaClient-rt.jar`, `opc-rt.jar` | **COVERED → B132** |
| high | **P3** | BACnet APDU service-PDU encoding: APDU types, segmentation (maxAPDU, multi-segment ACK windowing), AtomicWriteFile chunk sizing, application/context tag encoding | `bacnet-rt.jar`, `bacnetUtil-rt.jar` | **COVERED → B133** |
| med | **P4** | Fox/Foxs protocol wire: message/frame format, opcodes, session lifecycle, auth digest computation (static from the jar; live capture is a separate DYNAMIC gap) | `fox-rt.jar` | **COVERED → B134** |
| med | **P5** | LON NV wire encoding: network-variable update encoding, SNVT scaling/format on the wire, file-transfer framing (beyond the config-level coverage of B19/B77/B120) | `lonworks-rt.jar` | **COVERED → B135** |
| low | **P6** | Sox/Soxs presence audit in N4: confirm (negative finding) that the legacy NiagaraAX Sox protocol is absent/replaced in N4.14, and document what replaced it (platform.fox + Fox) | module index + `platform-rt.jar` | **COVERED → B136** |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026-06-29 | P1 Modbus wire-level | B131 | P1-dyn: live-wire confirmation of RTU CRC byte order (code emits CRC **high byte first**, opposite of spec low-first) → requires-execution / live serial device; P1-fc: register-type→FC mapping & FC20/21 file-record PDU live behavior (static skeleton only) |
| 2 | 2026-06-29 | P2 OPC wire encoding | B132 | P2-da-wire (NEW): OPC DA on-the-wire bytes are COM/DCOM (ORPC/NDR) produced by native proxy DLLs (B127), invisible to static Java → requires-execution (live DCOM capture) / native-RE (Ghidra of opcproxy/opccomn_ps); P2-ua-sec (NEW): UA secure-channel crypto chunk content (sign/encrypt padding, token derivation) is runtime-only → requires-execution; P2-ua-live (NEW): live UA-TCP handshake/session/subscription behavior → requires live server |
| 3 | 2026-06-29 | P3 BACnet APDU encoding + segmentation | B133 | P3-mstp (NEW): BACnet MS/TP data-link framing (preamble `0x55 0xFF`, frame-type, header-CRC/data-CRC, token passing) — only the BACnet/IP link was decompiled; the APDU is link-independent → STATIC-investigable (mstp classes in `bacnet-rt`); P3-sc (NEW): BACnet/SC secure-connect transport (`stack.link.sc.*` — `ScBvlcMessage`/`ScNpdu` websocket framing) → STATIC-investigable; P3-dyn (NEW): live confirmation of segmentation window negotiation + SegmentACK/NAK retransmission timing → requires live BACnet device |
| 4 | 2026-06-29 | P4 Fox/Foxs protocol wire | B134 | P4-dyn (NEW): live Fox handshake byte-trace — the concrete runtime salt/iteration-count/nonces/clientProof bytes of a real SCRAM-SHA-256 login + the actual frame stream on 1911/4911 → requires-execution (running station + client); cross-ref platform-native N6 live wire. P4-srp6 (NEW): the SRP6 key-exchange + session-key DATA encryption byte content (`com.tridium.crypto.core.exchange.*` in nre.jar) — the message format is static-knowable but the derived key + encrypted-payload bytes are runtime-only → STATIC-investigable (SRP6 class decompile) for the format / requires-execution for the bytes. P4-legacy (NEW): AX legacy-digest scheme (`BLegacyDigestAuthenticationScheme`) digest computation for pre-1.0.2 peers → STATIC-investigable (decompile the legacy scheme in `baja`/`auth` jar) |
| 5 | 2026-06-29 | P5 LON NV wire encoding | B135 | P5-phys (NEW): TP/FT-10 physical/link layer — 78.125 kbaud differential-Manchester line code, L2 16-bit CRC, beta1/beta2 slot timing, predictive p-persistent CSMA — is in the native `ldv` adapter driver below `driver.write(ldvHandle,…)`, NOT in `lonworks-rt.jar` → requires-execution / hardware (live FT-10 segment + protocol analyzer), candidate [CERT-hw]; cross-ref B127. P5-le (NEW): the LonTalk physical-wire little-endian byte order (Java app buffer proven big-endian §135.2; Neuron firmware re-orders natively) → requires-execution / native-RE. P5-rt (NEW): concrete runtime NV selectors / device ProgramIds / domain keys are per-network values, not jar literals → requires live network. |
| 6 | 2026-06-29 | P6 Sox/Soxs presence audit (negative finding) | B136 | NONE read-only-investigable. Confirmed: NO `sox`/`sedona` jar in 973 modules, ZERO Sox protocol classes/refs across all 973 jars (raw-byte backstop) and at constant-pool depth in baja/platform-rt/fox-rt/niagaraDriver-rt+wb/provisioningNiagara-wb+ux/platDaemon-rt+wb/platCrypto-rt. Only vestigial Sedona-management UI residue (icons `x16/sox*.png`, platDaemon enable/disable Sedona + `SedonaSurrogateView sox={0},http={1}`, `SedonaParser` editor highlighter). Role carried by Fox (`BNiagaraStation.configureFoxClientConnection`/`BStationDiscoveryJob`→`MulticastServer`, B134) + platform.fox (B129). P6-jace (NEW, requires-execution): the actual Sox device wire (and the optional N4 Sedona Driver `sox-rt`) lives on a JACE, not this Supervisor → requires a JACE install + live Sedona device. |

## Blocked / out-of-static-scope gaps (each tagged with what it needs)

- Live wire capture of Fox 1911/4911 and platform 3011/5011 framing/handshake/auth digest → requires-execution (running station + daemon + client). Tracked also in RESEARCH-STATE-platform-native.md (N6 live wire). **STATIC portion now CLOSED by B134** — the Fox framing, opcodes, FoxMessage codec, channel/circuit muxing, tune state machine, and the SCRAM-SHA-256 digest computation are all source-confirmed; only the on-the-wire BYTE TRACE (runtime salt/iter/nonce/proof + frame bytes) remains as **P4-dyn** (requires-execution).
- Live LON/OPC/BACnet field-bus runtime behavior → requires field hardware / live servers. Tracked in platform-native N4 driver runtime.
- **P1-dyn (NEW, B131)**: confirm RTU CRC byte order against a live Modbus serial device — Tridium `writeCRC` emits the CRC **high byte first** (`[CERT]` ModbusOutputStream.java:57-62, bytecode-confirmed), which is the reverse of the spec's low-first rule; transmit/receive are internally self-consistent → requires live serial device (candidate `[CERT-hw]`).
- **P2-da-wire (NEW, B132)**: OPC DA on-the-wire bytes are COM/DCOM (ORPC/NDR) marshalled by the **native** proxy DLLs (B127), not by `opc-rt.jar` (Java stops at the `native` JNI boundary, `[CERT]` ComObjectClient.java:61-63 / OpcSyncIo.java:31-39) → requires-execution (live DCOM packet capture) or native-RE (Ghidra of opcproxy/opccomn_ps).
- **P2-ua-sec (NEW, B132)**: UA secure-channel cryptographic chunk content (Sign/SignAndEncrypt padding bytes, signature, token derivation) is computed at runtime over live keys → requires-execution.
- **P2-ua-live (NEW, B132)**: live UA-TCP handshake (HEL/ACK negotiated buffer sizes), session activation, subscription/monitored-item behavior → requires a live OPC-UA server.
- **P5-phys (NEW, B135)**: TP/FT-10 physical/link layer (78.125 kbaud differential-Manchester line code, L2 16-bit CRC, beta1/beta2 slot timing, p-persistent CSMA) lives in the native `ldv` adapter driver below `driver.write(ldvHandle,…)` (`[CERT]` NLonLinkLayer.java:274), NOT in `lonworks-rt.jar` (Java app buffer is big-endian, §135.2) → requires-execution / hardware (live FT-10 segment + protocol analyzer), candidate `[CERT-hw]`; cross-ref B127 ldvProxy native side.

## Stop control

- Primary criterion: read-only-investigable backlog exhaustion (METHODOLOGY §8).
- **STATIC LOOP STOPPED (2026-06-29).** Read-only-investigable backlog = **0**. All six prioritized backlog items closed.
- **Blocks written this focus**: B131, B132, B133, B134, B135, B136. **Coverage ratio: 6 / 6.**
- Closed gaps: P1 Modbus (B131), P2 OPC (B132), P3 BACnet (B133), P4 Fox (B134), P5 LON (B135), P6 Sox-absence (B136).

> **UPDATE 2026-07-26 (B279): P3-mstp is RECLASSIFIED — it was NOT decompile-reachable.** The MS/TP
> data-link framing is not in the Java corpus at all: the preamble `0x55` appears nowhere in 50 798
> decompiled files, `platMstp-rt` contains zero CRC code, and the JNI boundary is
> `sendFrame0(handle, destAddr, data, dataExpectingReply)` — preamble, frame types, CRC-8/CRC-16 and token
> passing all live in the native `mstpnpsdk` / `platmstp` libraries, which are absent from this Supervisor
> install (MS/TP is a JACE-side capability). Closing it now needs **native-RE** (Ghidra on a JACE image) or
> a **live RS-485 capture** — the same disposition as P5-phys for LON. What *is* documented: the exact
> boundary, the EMSTP false lead, and the full configuration surface. See [B279].
>
> **UPDATE 2026-07-26 (B280): P3-sc is CLOSED.** BACnet/SC turned out to be the opposite of P3-mstp —
> fully implemented in Java, 40 classes across `com.tridium.bacnet.stack.link.sc` (17, connection/topology)
> and `…sc.message` (23, codec), with **no native methods**. Documented: the 13 BVLC-SC function codes
> (0-12), the header layout (function / controlFlags / messageId / optional VMACs / destination+data
> options / payload), the four control-flag bits, the strict-decoder behaviour, the **48-bit VMAC**
> (broadcast = all-ones, device VMACs randomly generated), and the connection timing defaults
> (reconnect 2 s → 600 s). Also **corrects two figures in B23 §23.24/§23.27**: neither `49152` nor `TLSv1`
> appears anywhere in `bacnet-rt` — the transport is WebSocket URIs, so the port comes from the `wss://`
> URI rather than a compiled-in constant. See [B280].
>
> NOTE on P3-mstp / P3-sc / P4-srp6 / P4-legacy: these spin-off gaps (BACnet MS/TP framing, BACnet/SC transport, SRP6 key-exchange message format, AX legacy-digest scheme) were uncovered during B133/B134 as *further-depth* items inside already-closed protocol blocks. They are decompile-reachable but were NOT part of the 6-item prioritized backlog and are NOT required to close the protocols static loop; they are parked as optional deepening for a future focus pass, not blockers of the STOP. The loop stops on the prioritized read-only set = 0.

- **Blocked / requires-execution (each tagged with the access it needs):**
  1. Platform live wire 3011/5011 framing/handshake → requires-execution (running daemon + client). Cross-ref RESEARCH-STATE-platform-native N6.
  2. Field-bus runtime behavior (LON/OPC/BACnet) → requires field hardware / live servers.
  3. **P1-dyn** — RTU CRC byte order (code emits CRC high-byte-first vs spec low-first) → requires a live Modbus serial device (candidate [CERT-hw]).
  4. **P2-da-wire** — OPC DA on-the-wire COM/DCOM (ORPC/NDR) bytes → requires-execution (live DCOM capture) or native-RE (Ghidra of opcproxy/opccomn_ps).
  5. **P2-ua-sec** — UA secure-channel crypto chunk content (sign/encrypt padding, token derivation) → requires-execution (live keys).
  6. **P2-ua-live** — live UA-TCP HEL/ACK handshake + session/subscription behavior → requires a live OPC-UA server.
  7. **P3-dyn** — BACnet segmentation window negotiation + SegmentACK/NAK retransmit timing → requires a live BACnet device.
  8. **P4-dyn** — **CERRADO [CERT-live] (B606, §12)**: Fox SCRAM byte-trace en foxs:4911 (1911 cerrado). challenge method=n4digest, keyExchange=null.1 (TLS-only), SCRAM-SHA-256 salt16B **i=10000** (CONFIRMA B457 PBKDF2-10k en canal Fox). Frame flow hello→kerberos(off)→username→challenge→authMessage1/2→welcome. Platform 3011/5011 sigue blocked-on-platform-creds.
  9. **P5-phys** — TP/FT-10 78.125 kbaud differential-Manchester line code + L2 16-bit CRC + slot timing (native `ldv` below `driver.write`) → requires-execution / hardware (live FT-10 segment + protocol analyzer), candidate [CERT-hw].
  10. **P6-jace** — the actual Sox device wire + the optional N4 Sedona Driver (`sox-rt`) live on a JACE, not this Supervisor → requires a JACE install + live Sedona device.

- Loop status: **STOPPED** (static read-only-investigable exhausted). Re-opens only if a live system/JACE/hardware arrives → DYNAMIC phase (METHODOLOGY §12), which would reclassify the requires-execution gaps above.
