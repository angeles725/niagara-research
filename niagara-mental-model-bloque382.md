# Block 382 — `libciper.so` decompiled with DWARF (Ghidra grade): the QNX-ARM Sylk masterslave file-transfer protocol — 496-byte records, ≤485-byte blocks, dual CRC-16-CCITT + CRC-32 integrity, no crypto

> **Focus `platform-native` — Ghidra sub-pass NG4.** [B126 §126.5] identified `libciper.so` at SYMBOL grade
> (257 `Java_com_honeywell_comm_JNIRequest_*` exports, `crypto false`, CRC-only, ECDSA-P256 `.sig`) and
> corrected the "cipher" premise. This block decompiles the FUNCTION BODIES — and the binary ships **DWARF
> debug info** (`-g2 -O0`, GNU C11 `qnx700`, armv7-a Thumb), so Ghidra recovers real struct names, field
> types, and function signatures. Result: the Honeywell Sylk/Spyder **masterslave serial file-transfer
> protocol** reconstructed from the native library, grounding the tool-side analysis of [B120]. READ-ONLY.
>
> Sources (primary): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/libciper.so`
> (ELF32 **ARM** LE, QNX 7.0, `GCC 5.4.0 [qnx700]`, `-fstack-protector-strong -march=armv7-a -mthumb`;
> sha256 in evidence). Ghidra 12.1.2 `analyzeHeadless` (ARM:LE:32:v8) + `ExportDecompiledC.java` over 33
> Sylk/CRC/serial functions, aided by the binary's own DWARF.
> Raw evidence: `audits/B382-libciper-decomp.c` (33 functions), `audits/B382-libciper-ghidra.txt`.
> Markers: `[CERT]` observed in the decompiled body (`audits/…:line`) · `[INFER]` deduction.
>
> Native platform layer (Capa 25) ↔ Spyder/Sylk (Capa 22). Connects [Block 126] (§126.5 — upgrades its
> symbol-grade read to body grade, confirms `crypto false`), [Block 120] (the masterslave file-transfer +
> CRC-16 wire protocol analyzed tool-side — this is its QNX-ARM native counterpart), [Block 121] (Sylk
> terminal-property model), [Block 125] (JNI binding pattern — these are `com.honeywell.comm.JNIRequest` natives).

---

## 382.1 — Identity confirmed at body grade: DWARF-rich QNX-ARM, no crypto `[CERT]`

The DWARF producer string (Ghidra import summary) is
`GNU C11 5.4.0 [qnx700] -g2 -O0 -fstack-protector-strong -march=armv7-a -mfpu=vfpv3 -mfloat-abi=hard -mthumb`.
So the library is **unoptimized (`-O0`) with full debug info** — every function decompiles cleanly with its
source-level name, parameter names, and named struct fields, and every function carries a
`__stack_chk_guard`/`__stack_chk_fail` canary (`-fstack-protector-strong`). This confirms [B126 §126.5]'s
`crypto false` at body grade: the only integrity primitives present are CRCs (§382.4), not ciphers. `[CERT]`

> **Tool note (FLOSS — near-zero result, registered per methodology):** FLOSS was run to extract
> stack/decoded strings; on a DWARF-carrying, unobfuscated library its output adds nothing over static
> `strings` + DWARF. Recorded so a future pass does not re-attempt it as if it might reveal hidden strings. `[CERT]`

---

## 382.2 — The JNI boundary: a fixed 496-byte record marshalled to Java `[CERT]`

`Java_com_honeywell_comm_JNIRequest_jniReadSylkFileData(JNIEnv*, jobject, jint fileId, jshort addr,
jint fileposition, jint datalen)` (`audits/B382-libciper-decomp.c:233-282`) is the representative JNI entry:
- It zeroes a **`SylkFileDataRecord` of `0x1f0` = 496 bytes** (`memset(&readResponseRecord,0,0x1f0)`), calls
  the native `masterslaveReadSylkFileData(&rec, fileId, addr, fileposition, datalen)`, then `memcpy`s the
  496-byte result. `[CERT]`
- It returns a `jbyteArray` **only when `blockHeader.status == 0`** (`NewByteArray(length)` +
  `SetByteArrayRegion(...,length, rec.block)`), returning NULL otherwise. So a failed read surfaces to Java
  as a null array, not an exception. `[CERT]`
- Debug is gated on `(g_DbgTopic & 8) && g_DbgLevel == DBG_FULL` — a global topic-bitmask + level (the
  library's tracing model). `[CERT]`

The 496-byte fixed record IS the native↔Java marshalling contract [B125] described generically; here it is
the concrete Sylk record. `[CERT]`

---

## 382.3 — The file-transfer protocol: blocks, sectors, and a ≤485-byte payload cap `[CERT]`

The DWARF struct `FileBlockRecord` (496 bytes) has a `blockHeader` whose `file` member is a **union of a
`blk` variant and a `crc` variant** — the two record shapes of the transfer. `[CERT]`

- **Data blocks** — `buildFileBlockRecord(ioCommand, fileId, addr, sectorNum, totalSectors, blockNum,
  totalBlocks, blockBuffer, bufferSize)` (`:288-334`): fills `blk.{ioCommand,fileId,addr,sectorNum,
  totalSectors,blockNum,totalBlocks,blockSize}`, `memcpy`s the payload, and — crucially — **rejects
  `bufferSize >= 0x1e5` (485)** ("buffserSize exceeds FileBlockRecord payload"). So a file is transferred as
  a **sequence of sector/block-numbered records, each ≤ 485 payload bytes**, wrapped by `dataToRecord` +
  `buildMessage(mtFileBlock,...)`. `[CERT]`
- **Read request** — `masterslaveReadSylkFileData(...)` (`:607-693`): builds a request record with
  `ioCommand = 0x1f`, `request = 0x01`, `status = 0xff` (pending), `length = datalength`, `fileposition`,
  caps `datalength < 0x1e5` (485), then `buildMessage(mtFileData,...)` → `SetSendMessage` →
  **`requestOneMessage()`** (a synchronous request/response). On `== 1` it sets `status = 0` and returns the
  496-byte response; on failure `status = 2`; on oversize `status = 1`. So the **status byte is the
  protocol's result channel** (0 ok / 1 too-big / 2 request-failed). `[CERT]`
- **File open/status/close** — `masterslavefileopenv2(fileId, filemode, addr, fileposition, requesttype,
  length)` (`:700+`), `masterslavefilestatus`, `masterslavefileclosev2` complete the file-verb set, each
  building a record and dispatching a message. `[CERT]`

So `libciper.so` implements a **master/slave (Niagara=master, Spyder/Sylk device=slave) block-oriented file
protocol over serial**, with a 485-byte block cap and a fixed 496-byte record frame. `[CERT]`

---

## 382.4 — Integrity is TWO CRCs, no encryption `[CERT]`

- **Message level: CRC-16-CCITT** — `crc_ccitt_add(uint16_t crc, uchar *ptr, size_t n)` (`:895-914`) is the
  classic table-driven CCITT CRC-16: `crc = crc_tabccitt[(*ptr ^ (crc>>8)) & 0xff] ^ (crc<<8)` per byte
  (polynomial 0x1021). `[CERT]`
- **File level: CRC-32 + length** — `buildFileBlockCRCRecord(ioCommand, fileId, addr, fileLength, fileCRC32)`
  (`:340-367`) emits the `crc` union variant carrying **`fileCRC32` and `fileLength`** in a 12-byte payload
  (`dataToRecord(...,0xc)`), so a completed transfer is validated by a whole-file CRC-32 and its declared
  length. `[CERT]`

Two independent CRCs (per-message CRC-16, per-file CRC-32) and **no cipher anywhere** — this is exactly
[B126 §126.5]'s "CRC-only, crypto false", now grounded in the algorithm bodies. The `.so.sig` ECDSA-P256
signature ([B126 §126.1]) is the ONLY cryptographic protection of this library, and it is applied by the
embedded platform to the library file, not by the library to its wire traffic. `[CERT]/[INFER]` (the
transfer itself is unauthenticated/unencrypted — integrity-checked only). `[CERT]`

---

## 382.5 — Defensive-security summary `[CERT]`

1. **Serial file transfer is integrity-checked but NOT authenticated or encrypted** `[CERT]` (§382.4): CRC-16
   (message) + CRC-32 (file) detect corruption, not tampering; anyone on the serial/Sylk bus could inject a
   valid-CRC record. Security rests on physical bus access (a wired Sylk/S-BUS segment), not on the protocol.
2. **Fixed 496-byte records + 485-byte block cap** `[CERT]` (§382.2/§382.3): the size checks (`< 0x1e5`) are
   real bounds — no unbounded copy in the paths read; `memcpy`s are gated on the cap.
3. **Stack-protector on every function** `[CERT]` (§382.1): `-fstack-protector-strong`, so stack-smash of a
   Sylk record aborts via `__stack_chk_fail` rather than executing.
4. **`status` byte is the sole result channel** `[CERT]` (§382.3): Java sees success/failure only through the
   record's status field (0/1/2) and a null jbyteArray — no richer error propagation across the JNI boundary.

No secrets present (this is embedded-device comms code, no keys); analysis on an isolated copy; nothing mutated.

---

## 382.6 — Self-verify

**Token re-checks** (load-bearing `[CERT]`):
1. DWARF producer `GNU C11 … qnx700 … -O0 -fstack-protector-strong … -mthumb` — ✓ (Ghidra import summary, evidence file).
2. `jniReadSylkFileData` marshals `0x1f0`=496-byte `SylkFileDataRecord`, returns jbyteArray only on `status==0` — ✓ (`decomp:257-275`).
3. `buildFileBlockRecord` rejects `bufferSize >= 0x1e5` (485), fills blk.{sector/block numbering} — ✓ (`decomp:305-327`).
4. `masterslaveReadSylkFileData`: `ioCommand=0x1f`, `request=0x01`, `status=0xff`→0, `< 0x1e5`, `requestOneMessage()`, status 0/1/2 — ✓ (`decomp:640-688`).
5. `crc_ccitt_add`: `crc_tabccitt[(*ptr ^ (crc>>8))] ^ (crc<<8)` (CRC-16-CCITT) — ✓ (`decomp:908-911`).
6. `buildFileBlockCRCRecord`: `fileCRC32` + `fileLength`, 0xc-byte payload — ✓ (`decomp:355-360`).
7. `Java_com_honeywell_comm_JNIRequest_*` namespace — ✓ (`rabin2 -E`, evidence).
8. `crypto false` / no cipher symbols — ✓ (B126 §126.5 remitted + no crypto in bodies).

**8/8 load-bearing tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 22 · `[INFER]` 2 (transfer-unauthenticated reading; `.sig`-vs-wire scope).
Ratio ≈ 0.09 — low; EVIDENCE block over DWARF-rich decompiled bodies. Upgrades B126 §126.5 from symbol to
body grade; no re-derivation (B126's identity/`crypto false`/ECDSA-`.sig` facts are REMITTED).

---

## 382.x — Connections

- **[Block 126]** — *upgrades §126.5 to body grade.* B126 read `libciper.so`'s 257 symbols and correctly
  said "not a cipher lib, CRC-only, crypto false"; B382 decompiles the bodies (with DWARF) and reconstructs
  the actual masterslave file protocol + both CRC algorithms, confirming crypto-absence in the code itself.
- **[Block 120]** — the masterslave file-transfer + CRC-16 protocol B120 analyzed tool-side is implemented
  here in the QNX-ARM native library: 485-byte blocks, sector/block numbering, `mtFileBlock`/`mtFileData`.
- **[Block 121]** — `handleTerminalPropertyMessage`/`handlePublicVariableMessage` are the Sylk
  terminal-property/public-variable receive handlers (state flags `rcvIOCommandStatus`/`msgCompleteStatus`).
- **[Block 125]** — the `Java_com_honeywell_comm_JNIRequest_*` natives follow the JNI name-mangling bind B125
  documented; this is the Honeywell (not Tridium) native JNI surface.
- **Forward — reopened focus**: NG1-G1 (nverify `skip-*` gate sites), NG2b (nre.dll native bodies). The
  Ghidra sub-pass's core binaries (nverify/njre/plat/libciper) are now at body grade → a sub-pass SYNTHESIS
  block + §18 retro are the natural terminal artifacts.
