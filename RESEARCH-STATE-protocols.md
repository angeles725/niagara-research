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

- **Covered blocks (this focus)**: 1 — B131 (Modbus wire-level).
- **Coverage metric**: 1 / 6 backlog items closed.
- **Last iteration**: 2026-06-29 — P1 Modbus wire-level closed (B131).

## Gap-backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **P1** | Modbus wire-level: MBAP TCP header (transaction/protocol/length/unit id), function-code PDU encoding (FC 01-23 read/write coils/registers), RTU framing + CRC-16, register/coil/input addressing & byte/word order | `modbusCore-rt.jar`, `modbusTcp-rt.jar`, `modbusTcpSlave-rt.jar`, `modbusAsync-rt.jar` | **COVERED → B131** |
| high | **P2** | OPC wire encoding: OPC-UA binary encoding (variant types, NodeId, service request/response framing) + legacy OPC DA COM variant marshalling boundary | `opcUaCore-rt.jar`, `opcUaClient-rt.jar`, `opc-rt.jar` | not covered |
| high | **P3** | BACnet APDU service-PDU encoding: APDU types, segmentation (maxAPDU, multi-segment ACK windowing), AtomicWriteFile chunk sizing, application/context tag encoding | `bacnet-rt.jar`, `bacnetUtil-rt.jar` | not covered |
| med | **P4** | Fox/Foxs protocol wire: message/frame format, opcodes, session lifecycle, auth digest computation (static from the jar; live capture is a separate DYNAMIC gap) | `fox-rt.jar` | not covered |
| med | **P5** | LON NV wire encoding: network-variable update encoding, SNVT scaling/format on the wire, file-transfer framing (beyond the config-level coverage of B19/B77/B120) | `lonworks-rt.jar` | not covered |
| low | **P6** | Sox/Soxs presence audit in N4: confirm (negative finding) that the legacy NiagaraAX Sox protocol is absent/replaced in N4.14, and document what replaced it (platform.fox + Fox) | module index + `platform-rt.jar` | not covered |

## Iteration history

| # | Date | Gap closed | Block | New gaps uncovered |
|---|---|---|---|---|
| 1 | 2026-06-29 | P1 Modbus wire-level | B131 | P1-dyn: live-wire confirmation of RTU CRC byte order (code emits CRC **high byte first**, opposite of spec low-first) → requires-execution / live serial device; P1-fc: register-type→FC mapping & FC20/21 file-record PDU live behavior (static skeleton only) |

## Blocked / out-of-static-scope gaps (each tagged with what it needs)

- Live wire capture of Fox 1911/4911 and platform 3011/5011 framing/handshake/auth digest → requires-execution (running station + daemon + client). Tracked also in RESEARCH-STATE-platform-native.md (N6 live wire).
- Live LON/OPC/BACnet field-bus runtime behavior → requires field hardware / live servers. Tracked in platform-native N4 driver runtime.
- **P1-dyn (NEW, B131)**: confirm RTU CRC byte order against a live Modbus serial device — Tridium `writeCRC` emits the CRC **high byte first** (`[CERT]` ModbusOutputStream.java:57-62, bytecode-confirmed), which is the reverse of the spec's low-first rule; transmit/receive are internally self-consistent → requires live serial device (candidate `[CERT-hw]`).

## Stop control

- Primary criterion: read-only-investigable backlog exhaustion (METHODOLOGY §8).
- Open gaps — read-only investigable: 5 (P2-P6). Closed: P1 (B131).
- Blocked / requires-execution: 3 (Fox/platform live wire; field-bus runtime; P1-dyn RTU CRC live confirm).
- Loop status: ACTIVE — next gap P2 (OPC wire encoding).
