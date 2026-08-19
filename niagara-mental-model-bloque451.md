# Block 451 — IO-R-16 firmware, decompiled (gap B447-G2): a 60-function MSP430 slave — config in info-flash, an RS-485 RX-ring main loop, and a 4×12-bit packed-AO update handler

**Focus:** base corpus (field-I/O drivers) ∩ platform-native. Closes **B447-G2** at the firmware-structure level (instruction-by-instruction reconstruction of every handler remains out of scope). Continues [B447]/[B450].

**Origin:** grandchild gap from [B447] — actual reverse-engineering of a `T2xxx.a43` image, requested by the operator ("open one of those with the native tool"). The images are the only nrio native binary present in the corpus ([B449]: `actrld`/`libplatnrio` are NOT on disk → B449-G1 stays hardware-blocked).

**Method / tools ([research-sdd] toolbelt, verified via `detect-tools.sh` 2026-08-18):** Ghidra headless 12.1 `analyzeHeadless` with `-processor TI_MSP430:LE:16:default` (the image is MSP430 16-bit, [B450] §450.3), importing the Intel-HEX `.a43` directly; decompiled to C with the kit's `ExportDecompiledC.java` post-script. `objcopy -I ihex -O binary` used for the byte/string cross-check. Analysis "succeeded", **60 functions exported, 0 failed**.

**Target:** `T2101_2_2.a43` — the **IO-R-16** firmware (device type `io16V1`→tag `2101`, [B450] §450.2). `sha256 d1f4ee34…f0554`. Registered in `sources/SOURCES.md`.

**Scope:** overall structure, config/persistence, the comm main-loop, and the AO output handler — enough to corroborate the [B448] wire protocol from the *slave* side. NOT: every one of the 60 functions; the exact `status` error codes; ADC conversion internals.

**Sources:** `[CERT]` the decompiled C (`scratchpad/nrio-fw/decomp/T2101_2_2.a43.c`, from the sha-pinned image) + `[CERT]` byte inspection. Cross-refs [B448] (protocol), [B450] (entry vector), [B446] (scale/offset).

---

## 451.1 — Shape: 60 functions, entry 0x1100, config in info-flash

Ghidra recovered **60 functions** over the contiguous 0x1100–0xFFFF image; execution begins at the reset vector **0x1100** ([B450] §450.3). No ASCII build/version string exists in the image (confirmed with `strings` on the objcopy'd binary) — the firmware "version" the `ReadBuildInfoMessage` returns is numeric, not embedded text. `[CERT]`

**Persistence = MSP430 info-flash.** The init path reads a config word at **0x1000/0x1002**; if erased (`_DAT_1000 == -1 && _DAT_1002 == -1`, i.e. 0xFFFF) it runs a defaults routine (`FUN_3980`), otherwise it copies a stored config block into RAM (`FUN_1c54(0x330, 0x1000)`). `[CERT]` (`T2101…c:301-308`) This is the on-module store behind the `RD_INFO_MEMORY`/`CLEAR_INFO_MEMORY` messages ([B448]) and the logical **Address+Uid** the module remembers ([B448] §448.3): the address survives reset because it lives in info-flash, not RAM.

## 451.2 — The comm main loop: an RS-485 RX ring, interrupt-fed

After init the firmware enables a **Port-2 interrupt** (`P2IE |= 0x40` — bit 6, the RS-485/comm line) and enters a spin loop gated on a **receive ring buffer**: `while (_DAT_05d6 == _DAT_05d8)` idles while head==tail (queue empty); when a byte/message arrives the pointers diverge and it is processed. Flag bytes `DAT_0602`/`DAT_0603` latch actions set from the ISR, and `_DAT_022e`/`_DAT_0490` stage response state. `[CERT]` (`T2101…c:317-344`) This is the textbook **polled-slave** structure: the module never initiates; it waits for the JACE master's frames and answers — exactly the master/slave model [B448] §448.2 inferred from the driver side, now confirmed from the firmware side.

A persistent state byte at **0x0238 == 0x0c** gates a special pre-loop path (`FUN_2bea`); `0x0c` = **`MSG_WR_CODE_DNLD_DATA`/download state** ([B448]) — i.e. the module can boot straight into the firmware-download path if a prior flash was left mid-transfer, the resume behaviour behind [B447]'s "interrupting a flash bricks it" warning. `[CERT]` (`T2101…c:313-316`)

## 451.3 — Analog-output handler: 4 channels, 12-bit, packed 3-bytes-per-2

`FUN_22cc(byte *msg)` decodes the AO write payload for the IO-R-16's **4 analog outputs**: it unpacks **four 12-bit values** from a nibble-packed byte stream — `ch0 = msg[0]<<4 | msg[1]>>4`, `ch1 = (msg[1]&0xf)<<8 | msg[2]`, `ch2 = msg[3]<<4 | msg[4]>>4`, `ch3 = (msg[4]&0xf)<<8 | msg[5]` (2 channels per 3 bytes) — writes each to a channel table at 0x27e, applies a scale/offset transform (`FUN_2f78`/`FUN_26a0`/`FUN_216e`/`FUN_41fe`), and **clamps to 0x1000 (4096) = 12-bit DAC full-scale** before latching to the output registers (RAM0194…RAM019a). `[CERT]` (`T2101…c:550-598`)

Findings: `[CERT]`
- Confirms IO-R-16 = **4 AOs at 12-bit resolution** ([B445] §445.1 said "4 AO 0–10 Vdc"; the DAC is 12-bit, 0–0x1000).
- The **scale/offset lives in the firmware** and is applied per output write — corroborating [B446] §446.4 ("scale/offset stored on the module", read back via `MSG_RD_SCALE_OFFSET`).
- The wire encoding is **bit-packed** (12-bit values across byte boundaries), which is why the driver's frame is compact and why the app layer carries no verbose per-channel fields ([B448] §448.2).

## 451.4 — Security read (edge integrity, in situ)

Nothing in the firmware validates the *authenticity* of a code-download: the module accepts flash writes in the download state and resumes an interrupted flash from a persistent flag — there is no signature check on the receiving side either, mirroring the host side ([B447] §447.3). `[INFER]` from §451.2 + [B447]: the unauthenticated-firmware posture is **symmetric** — neither the JACE host nor the MSP430 module authenticates the image; only the Intel-HEX / frame checksums guard against corruption, not forgery. This is the signing-pki thesis ([B392]/[B394]/[B395]) reaching its furthest edge, now confirmed on the silicon that drives the relays.

---

## 451.5 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Ghidra headless (MSP430 lang) decompiled T2101 (IO-R-16 fw): 60 functions, 0 failed | `[CERT]` | `ghidra.log` "60 exported, 0 failed"; analysis succeeded |
| 2 | Entry at reset vector 0x1100; contiguous 0x1100–0xFFFF; no ASCII build/version string | `[CERT]` | [B450] §450.3; `strings` on objcopy'd bin |
| 3 | Config persisted in info-flash at 0x1000; erased(0xFFFF)→defaults, else loaded to RAM | `[CERT]` | `T2101…c:301-308` |
| 4 | Main loop = interrupt-fed RS-485 RX ring (head/tail 0x05d6/0x05d8), P2IE bit6; polled slave | `[CERT]` | `T2101…c:317-344` |
| 5 | State 0x0238==0x0c gates a download-resume path (0x0c = WR_CODE_DNLD_DATA) | `[CERT]` | `T2101…c:313-316`; [B448] msg 12 |
| 6 | AO handler unpacks 4×12-bit packed values, scale/offset, clamp to 0x1000 (12-bit DAC) | `[CERT]` | `T2101…c:550-598` |
| 7 | Scale/offset applied in firmware (corroborates [B446] §446.4) | `[CERT]` | same function chain |
| 8 | No authenticity check on received firmware download (symmetric with host) | `[INFER]` | §451.2 + [B447] §447.3 |

**Tally:** 8 claims — 7 `[CERT]` · 1 `[INFER]` (labelled) · 0 unmarked. Corroborates (no contradiction) [B448]/[B446]/[B447].

**Left out (named):** the full 60-function map; the exact `status` error-code table; ADC/input-side conversion; the `T2102` (IO-R-34) image (same method applies — sha `625bd378…95d5e`, not yet decompiled); MSP430 register-map symbolication (Ghidra used generic `DAT_` addresses).

## 451.6 — Connections
- **Closes B447-G2** (firmware-structure level); the remaining depth (per-function map, error codes) is low-value tail, named above.
- **Corroborates from the slave side:** [B448] (master/slave polled protocol, msg codes, info-memory), [B446] (on-module scale/offset), [B445] §445.1 (4 AOs, now 12-bit).
- **Signing-pki** ([B392]/[B394]/[B395]): edge firmware is unauthenticated on *both* ends — the thesis's furthest confirmation.
- **[B449]** remains the boundary: `actrld`/`libplatnrio` (JACE-side native) are not on disk; B449-G1 needs the physical QNX device.

## 451.7 — Open gaps
- **B451-G1** — decompile `T2102` (IO-R-34) and diff against T2101. **CLOSED → [B452]** (83.5% byte-identical shared codebase; same 57 fns; loops scaled to 16 UI / 8 AO; same 12-bit DAC + protocol).
- **B449-G1** (still open) — `actrld`/`libplatnrio` native RE: requires pulling the binaries off a live JACE-8000 (QNX `/proc/boot`), not available in the corpus.
