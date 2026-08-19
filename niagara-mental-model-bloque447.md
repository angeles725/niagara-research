# Block 447 — IO-R firmware images and the upgrade path (gap B445-G2): plaintext Intel HEX (MSP430) shipped in the jar, flashed over RS-485 with no cryptographic integrity

**Focus:** base corpus (field-I/O drivers + a signing-pki data point). Closes **B445-G2**. Continues [B445].

**Origin:** child gap from [B445] — the `.a43` firmware images under `nrio-rt/.../download/` and the Device-Manager "Upgrade Firmware" path, plus their integrity posture (ties to the [B394]/[B395] signing-pki thesis: integrity weakens toward the physical I/O edge).

**Scope:** (1) what the `.a43` files are, (2) how they are packaged, (3) the upgrade job → hex-parse → RS-485 block-write pipeline, (4) the integrity model. NOT: the MSP430 memory map internals or disassembly of the firmware payload.

**Sources:**
- **FUENTE 1 (corpus):** [B394] (PanelBus IO firmware = raw unsigned flash), [B395]/[B392] (signing-pki: Niagara signs code+delivery, not always the edge), [B445] §445.3 (UPS warning: interrupting a flash bricks the module).
- **FUENTE 2 (niagara-help):** `[CERT-doc]` `IO-R-16_MtgWiring_-TRI` L115-118 / L959-965 (upgrade launched from Nrio Device Manager; do not interrupt; ≈ <2 min).
- **FUENTE 3 (code + binary):** `[CERT]` `organized/nrio/nrio-rt/extracted/download/*.a43` (6 files), `job/BUpgradeFirmwareJob.java`, `util/FirmwareUpgradeUtils.java`, `messages/WriteDownLoad{Start,Data,Stop}.java`, `messages/NrioMessageConst.java`.

---

## 447.1 — The `.a43` images are plaintext Intel HEX for an MSP430

Six firmware images ship as `nrio-rt.jar` resources under `download/`: `[CERT]`

`T2029_1_34`, `T2030_1_34`, `T2034_1_34` (~31.5 KB), `T2041_2_2`, `T2101_2_2`, `T2102_2_2` (~37–38 KB).

Each is **ASCII Intel HEX** (the `.a43` extension = IAR/TI MSP430 toolchain output), verifiable by the record structure: `[CERT]`

```
:101100003140000A3C4008023D404E06B0128C3986   → :LL AAAA TT data… CC
:00110001EE                                    → start-address record (type 01 payload区)
:00000001FF                                    → Intel HEX end-of-file
```

`:` start, `LL`=byte count (0x10=16), `AAAA`=load address, `TT`=record type (00 data), trailing `CC`=two's-complement checksum. Data addresses span **0x1100–0xFFFF**, the classic MSP430F2xx flash map — consistent with the small 8/16-bit I/O processor inside each IO-R module. No binary wrapper, no header signature, no encryption: the payload is human-readable hex text sitting in the module jar. `[CERT]`

## 447.2 — Upgrade pipeline: jar resource → IntelHexFile → RS-485 block writes

`[CERT]` (`BUpgradeFirmwareJob.java`, `FirmwareUpgradeUtils.java`)

1. **Trigger** — the Nrio Device Manager's *Upgrade Firmware* command starts `BUpgradeFirmwareJob` for a selected `NrioModule`. `[CERT-doc]`
2. **Select image** — `accessNet.getFirmwareFile(device.getDeviceType())` returns the matching `.a43` `BIFile` (keyed by device type; see [B448] type enum). Null ⇒ abort.
3. **Parse** — `IntelHexFile.make(downLoadFile)` then `hexFile.readMemoryBlocks()` → a `Vector<IntelHexFile.MemoryBlock>`.
4. **Stream** — the network is put in download mode (`setDownLoadInProcess(true)`), then each memory block is pushed with `writeMemoryBlock(...)` using the protocol trio `WriteDownLoadStart` / `WriteDownLoadData` / `WriteDownLoadStop` (message types 10/12/11). Chunking constants: `MAX_MEMORY_DOWN_LOAD_SIZE = 64`, `DOWN_LOAD_MESSAGE_SIZE = 128`. 1-second guard sleeps bracket the transfer; `downLoadError` aborts the job. `[CERT]` (`NrioMessageConst:16-29`)
5. **Finish** — `setDownLoadInProcess(false)`; interrupting power/comm here is what the guide warns can brick the module. `[CERT-doc]` ([B445] §445.3)

## 447.3 — Integrity model: format checksum only, no crypto

The **only** integrity mechanism in the whole path is the **Intel HEX per-record checksum** (the format's own last byte). The upgrade code does **no signature check, no hash, no CRC-32, no version/authenticity validation** of the parsed image before flashing it — `FirmwareUpgradeUtils` goes straight from `IntelHexFile.make` to `writeMemoryBlock`. `[CERT]` (`FirmwareUpgradeUtils.java:22-115`)

**This confirms the signing-pki thesis at the I/O edge** ([B394]/[B395]/[B392]): Niagara cryptographically controls *who may run which modules* (RSA-2048 module signing, hidden TPK roots) but the firmware that actually drives relays and analog outputs is **unsigned plaintext hex** shipped inside a jar and streamed over an unauthenticated RS-485 bus. Consequences: `[INFER]` from the above `[CERT]` facts —
- Anyone able to replace the `download/*.a43` resource in the module (or MITM the RS-485 during a download) could flash arbitrary MSP430 code to the I/O processor — a physical/local-adjacent risk, not remote.
- There is no rollback/authenticity signal; a wrong-but-checksum-valid image would be accepted.

This is strictly weaker than the module layer and matches [B394]'s "PanelBus IO firmware is a raw unsigned flash image" for the *other* I/O family — two independent I/O lines, same posture.

---

## 447.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | 6 `.a43` images ship as `nrio-rt.jar` `download/` resources (~31–38 KB) | `[CERT]` | `extracted/download/*.a43`, `ls` sizes |
| 2 | `.a43` = ASCII Intel HEX (`:LLAAAATT…CC`, EOF `:00000001FF`), addresses 0x1100–0xFFFF (MSP430) | `[CERT]` | head/tail byte dump of `T2034_1_34.a43` |
| 3 | Upgrade = `BUpgradeFirmwareJob` → `IntelHexFile.make`→`readMemoryBlocks`→`writeMemoryBlock` over WriteDownLoad{Start,Data,Stop} (msg 10/12/11) | `[CERT]` | `BUpgradeFirmwareJob.java:117-122`, `FirmwareUpgradeUtils.java:22-115`, `NrioMessageConst:27-29` |
| 4 | Chunk constants MAX_MEMORY_DOWN_LOAD_SIZE=64, DOWN_LOAD_MESSAGE_SIZE=128; download-in-process flag + 1 s guards | `[CERT]` | `NrioMessageConst:16-17`, `BUpgradeFirmwareJob.java` |
| 5 | No signature/hash/CRC-32 verification of the image before flash — only Intel HEX per-record checksum | `[CERT]` | `FirmwareUpgradeUtils.java` (no crypto call between parse and write) |
| 6 | Firmware replacement / RS-485 MITM during download could flash arbitrary code (local-adjacent) | `[INFER]` | derived from claims 2+5 |
| 7 | Matches signing-pki thesis: integrity weakest at the I/O edge; parallels [B394] PanelBus | `[CERT]`+corpus | [B394]/[B395] |

**Tally:** 7 claims — 5 `[CERT]` · 1 `[INFER]` (labelled) · 1 corpus-cross-ref · 0 unmarked. No contradictions.

**Left out (named):** the `Txxxx_p_n` naming scheme (processor family vs point-count vs revision — the `_1_34` / `_2_2` suffixes are unresolved); MSP430 payload disassembly; whether the module rejects a mismatched-device image server-side.

## 447.5 — Connections
- **Closes B445-G2.** Reinforces [B394] (PanelBus raw flash) and [B395]/[B392] (signing-pki: code signed, edge data/firmware not).
- **Protocol** → [B448]: the WriteDownLoad messages and device-type keying are part of the wire protocol documented there.
- **Ops** → [B445] §445.3 UPS guidance now has its mechanism: an interrupted block-write leaves the MSP430 flash partially written.

## 447.6 — Open gaps
- **B447-G1** — device-type→image mapping + naming. **CLOSED → [B450]** (io16V1→2101, io34/sec→2102, etc.; host selects by type only, no authenticity check).
- **B447-G2** — MSP430 payload. **CLOSED → [B451]** (Ghidra-decompiled T2101/IO-R-16: 60 fns, info-flash config, RS-485 RX-ring polled slave, 4×12-bit AO handler; edge firmware unauthenticated on both ends). Triage in [B450] §450.3.
