# Niagara N4 Communication Protocols — Research State

> Focus: wire-level / encoding-grade reverse-engineering of Niagara N4 communication
> PROTOCOLS as implemented in the shipped Java driver modules — the actual frame
> structure, opcodes, PDU encoding, framing and on-the-wire semantics, NOT the
> architecture/integration view (already covered by B7/B19/B23/B77/B93/B94/B120/B127).
> READ-ONLY. Corpus language: ENGLISH.
> Source root: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/` (977 module jars).
> Tools: `research-sdd/toolbelt/decompile-java.sh` (Vineflower/CFR/Procyon) + javap + strings.
> Mirrored in engram: `research/niagara/protocols-gaps`, `research/niagara/protocols-progress`.

## Why this focus exists

A 2026-06-29 protocol-coverage audit over the existing 130-block corpus found the
investigation was rigorous at ARCHITECTURE/integration level but SPARSE at WIRE level.
This focus closes the STATIC-investigable wire-level gaps (decompilable from the jars).
Three gaps were rated MAJOR by the audit: Modbus frame structure (zero coverage),
OPC DA/UA encoding (zero coverage), BACnet APDU service-PDU encoding (object model
covered, packet encoding not). Fox and LON have partial wire coverage to deepen.

## Coverage

- **Covered blocks (this focus)**: 5 — B131 (Modbus wire-level), B132 (OPC-UA binary + DA/COM boundary), B133 (BACnet APDU encoding + segmentation), B134 (Fox/Foxs protocol wire + SCRAM-SHA-256 auth), B135 (LON NV wire encoding + SNVT scaling + NI application buffer + file transfer).
- **Coverage metric**: 5 / 6 backlog items closed.
- **Last iteration**: 2026-06-29 — P5 LON NV wire encoding closed (B135).

## Gap-backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **P1** | Modbus wire-level: MBAP TCP header (transaction/protocol/length/unit id), function-code PDU encoding (FC 01-23 read/write coils/registers), RTU framing + CRC-16, register/coil/input addressing & byte/word order | `modbusCore-rt.jar`, `modbusTcp-rt.jar`, `modbusTcpSlave-rt.jar`, `modbusAsync-rt.jar` | **COVERED → B131** |
| high | **P2** | OPC wire encoding: OPC-UA binary encoding (variant types, NodeId, service request/response framing) + legacy OPC DA COM variant marshalling boundary | `opcUaCore-rt.jar`, `opcUaClient-rt.jar`, `opc-rt.jar` | **COVERED → B132** |
| high | **P3** | BACnet APDU service-PDU encoding: APDU types, segmentation (maxAPDU, multi-segment ACK windowing), AtomicWriteFile chunk sizing, application/context tag encoding | `bacnet-rt.jar`, `bacnetUtil-rt.jar` | **COVERED → B133** |
| med | **P4** | Fox/Foxs protocol wire: message/frame format, opcodes, session lifecycle, auth digest computation (static from the jar; live capture is a separate DYNAMIC gap) | `fox-rt.jar` | **COVERED → B134** |
| med | **P5** | LON NV wire encoding: network-variable update encoding, SNVT scaling/format on the wire, file-transfer framing (beyond the config-level coverage of B19/B77/B120) | `lonworks-rt.jar` | **COVERED → B135** |
| low | **P6** | Sox/Soxs presence audit in N4: confirm (negative finding) that the legacy NiagaraAX Sox protocol is absent/replaced in N4.14, and document what replaced it (platform.fox + Fox) | module index + `platform-rt.jar` | not covered |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026-06-29 | P1 Modbus wire-level | B131 | P1-dyn: live-wire confirmation of RTU CRC byte order (code emits CRC **high byte first**, opposite of spec low-first) → requires-execution / live serial device; P1-fc: register-type→FC mapping & FC20/21 file-record PDU live behavior (static skeleton only) |
| 2 | 2026-06-29 | P2 OPC wire encoding | B132 | P2-da-wire (NEW): OPC DA on-the-wire bytes are COM/DCOM (ORPC/NDR) produced by native proxy DLLs (B127), invisible to static Java → requires-execution (live DCOM capture) / native-RE (Ghidra of opcproxy/opccomn_ps); P2-ua-sec (NEW): UA secure-channel crypto chunk content (sign/encrypt padding, token derivation) is runtime-only → requires-execution; P2-ua-live (NEW): live UA-TCP handshake/session/subscription behavior → requires live server |
| 3 | 2026-06-29 | P3 BACnet APDU encoding + segmentation | B133 | P3-mstp (NEW): BACnet MS/TP data-link framing (preamble `0x55 0xFF`, frame-type, header-CRC/data-CRC, token passing) — only the BACnet/IP link was decompiled; the APDU is link-independent → STATIC-investigable (mstp classes in `bacnet-rt`); P3-sc (NEW): BACnet/SC secure-connect transport (`stack.link.sc.*` — `ScBvlcMessage`/`ScNpdu` websocket framing) → STATIC-investigable; P3-dyn (NEW): live confirmation of segmentation window negotiation + SegmentACK/NAK retransmission timing → requires live BACnet device |
| 4 | 2026-06-29 | P4 Fox/Foxs protocol wire | B134 | P4-dyn (NEW): live Fox handshake byte-trace — the concrete runtime salt/iteration-count/nonces/clientProof bytes of a real SCRAM-SHA-256 login + the actual frame stream on 1911/4911 → requires-execution (running station + client); cross-ref platform-native N6 live wire. P4-srp6 (NEW): the SRP6 key-exchange + session-key DATA encryption byte content (`com.tridium.crypto.core.exchange.*` in nre.jar) — the message format is static-knowable but the derived key + encrypted-payload bytes are runtime-only → STATIC-investigable (SRP6 class decompile) for the format / requires-execution for the bytes. P4-legacy (NEW): AX legacy-digest scheme (`BLegacyDigestAuthenticationScheme`) digest computation for pre-1.0.2 peers → STATIC-investigable (decompile the legacy scheme in `baja`/`auth` jar) |
| 5 | 2026-06-29 | P5 LON NV wire encoding | B135 | P5-phys (NEW): TP/FT-10 physical/link layer — 78.125 kbaud differential-Manchester line code, L2 16-bit CRC, beta1/beta2 slot timing, predictive p-persistent CSMA — is in the native `ldv` adapter driver below `driver.write(ldvHandle,…)`, NOT in `lonworks-rt.jar` → requires-execution / hardware (live FT-10 segment + protocol analyzer), candidate [CERT-hw]; cross-ref B127. P5-le (NEW): the LonTalk physical-wire little-endian byte order (Java app buffer proven big-endian §135.2; Neuron firmware re-orders natively) → requires-execution / native-RE. P5-rt (NEW): concrete runtime NV selectors / device ProgramIds / domain keys are per-network values, not jar literals → requires live network. |

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
- Open gaps — read-only investigable: 5 (P6 Sox-audit + P3-mstp BACnet MS/TP framing, P3-sc BACnet/SC transport + P4-srp6 SRP6 key-exchange format, P4-legacy AX legacy-digest scheme). Closed: P1 (B131), P2 (B132), P3 (B133), P4 (B134), P5 (B135).
- Blocked / requires-execution: 9 (platform live wire; field-bus runtime; P1-dyn RTU CRC live confirm; P2-da-wire COM/DCOM native; P2-ua-sec crypto chunk; P2-ua-live UA session; P3-dyn BACnet segmentation/SegmentACK live timing; P4-dyn Fox handshake byte-trace; NEW P5-phys TP/FT-10 line code native/hardware).
- Loop status: ACTIVE — next gap P6 (Sox/Soxs presence audit — negative finding: confirm legacy NiagaraAX Sox protocol absent/replaced in N4.14).
