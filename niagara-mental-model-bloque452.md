# Block 452 — IO-R-34 firmware vs IO-R-16 (gap B451-G1): 83.5% byte-identical shared codebase, same 57-function skeleton, I/O loops scaled to 16 UI / 8 AO

**Focus:** base corpus (field-I/O drivers) ∩ platform-native. Closes **B451-G1**. Continues [B451]/[B450].

**Origin:** grandchild gap from [B451] — decompile the IO-R-34 image (`T2102`) and diff it against the IO-R-16 (`T2101`) to confirm the shared-image relationship and map the larger I/O count.

**Method / tools:** same as [B451] — Ghidra headless 12.1 `analyzeHeadless -processor TI_MSP430:LE:16:default` + kit `ExportDecompiledC.java`; analysis **succeeded**. Byte diff via `objcopy` + `cmp`.

**Target:** `T2102_2_2.a43` — the **IO-R-34** firmware (device types `io34`/`io34sec`→tag `2102`, [B450] §450.2). `sha256 625bd378…95d5e`. Registered in `sources/SOURCES.md`.

**Scope:** the structural delta vs [B451]'s T2101. NOT: full per-function map; the primary/secondary controller role-split internals.

**Sources:** `[CERT]` decompiled C of both images (`scratchpad/nrio-fw/decomp/T210{1,2}_2_2.a43.c`), `[CERT]` `cmp` byte diff. Cross-refs [B445] §445.1 (I/O counts), [B451] (T2101 findings).

---

## 452.1 — Same codebase, scaled parameters

The two images are **83.5% byte-identical** (51 078 / 61 184 bytes match; 10 106 differ) and decompile to the **same 57-function skeleton**. `[CERT]` (`cmp -l`; both `.c` have 57 `FUN_`) The shared 83.5% is the common **RS-485 stack, main loop, info-flash config, and message dispatch** ([B451]); the ~16.5% delta is the I/O-count-specific handling. The IO-R-34 keeps the identical persistence pattern — `FUN_1e3a(0x330, 0x1000)` loads the config block from info-flash to RAM 0x330, matching T2101's `FUN_1c54(0x330, 0x1000)` ([B451] §451.1). `[CERT]`

This confirms the design behind [B450] §450.2's mapping: the IO-R-16 and IO-R-34 are **one firmware family** parameterised per board, and the IO-R-34's `io34`/`io34sec` device types share the single `2102` image across the board's two controllers.

## 452.2 — I/O loops scaled to the larger board

Where the IO-R-16 firmware iterates **4** (its AO count), the IO-R-34 firmware iterates **8**, and adds a **16**-wide loop — matching the IO-R-34's **16 UI / 10 DO / 8 AO** vs the IO-R-16's 8 UI / 4 DO / 4 AO ([B445] §445.1): `[CERT]`

| Construct | T2101 (IO-R-16) | T2102 (IO-R-34) | Maps to |
|---|---|---|---|
| AO channel loops | `uVar1 < 4` | `uVar8 < 8` (lines 26, 63, 155, 331) | 4 vs **8 AOs** |
| Input scan loop | (8-wide) | `uVar3 < 0x10` (line 213) | 8 vs **16 UIs** |
| AO DAC clamp | `0x1000` (12-bit) | `0x1000` (12-bit) | same 12-bit DAC |
| Scale/offset | per-channel chain | per-channel chain | same math ([B446]) |

The AO handler still clamps to **0x1000 (4096 = 12-bit)** — the IO-R-34's 8 analog outputs are the same 12-bit 0–10 V DACs as the IO-R-16's 4 ([B451] §451.3). `[CERT]` The T2102 image also retains a `uVar1 < 4` path (line 569) alongside the 8-wide loops — consistent with the board's **two-controller split** (each MSP430 services a subset of the 34 points; the same image runs on both, adapting by role), though the exact primary/secondary partition is left unmapped. `[INFER]`

## 452.3 — What the diff settles

- **Shared-image claim ([B450] §450.2) holds** at the binary level: it is literally the same code family, not two unrelated firmwares. `[CERT]`
- The **wire protocol, poll-slave loop, info-flash persistence, and unauthenticated-download posture** from [B451] apply **unchanged** to the IO-R-34 (that code is in the 83.5% common region). `[CERT]`+`[INFER]`
- The only material firmware difference is **channel count** (loop bounds and channel tables), not architecture — so nothing in [B445]–[B451] about the IO bus, addressing, or security changes for the IO-R-34.

---

## 452.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | T2101 vs T2102 = 83.5% byte-identical (51078/61184); same 57-function skeleton | `[CERT]` | `cmp -l` count; `FUN_` counts |
| 2 | IO-R-34 keeps identical info-flash config load `FUN_1e3a(0x330,0x1000)` | `[CERT]` | `T2102…c:245` vs `T2101…c` FUN_1c54 |
| 3 | AO loops iterate 8 (io34) vs 4 (io16); input loop 16-wide (0x10) | `[CERT]` | `T2102…c:26,63,155,213,331` |
| 4 | Same 12-bit DAC clamp 0x1000 and per-channel scale/offset | `[CERT]` | `T2102…c:173-176,589-593` |
| 5 | Loop counts match datasheet: IO-R-34 = 16 UI / 8 AO vs IO-R-16 8 UI / 4 AO | `[CERT]` | claim 3 + [B445] §445.1 |
| 6 | Shared image runs on both pri+sec controllers, adapting by role (4-wide path retained) | `[INFER]` | `T2102…c:569` + [B450] §450.2 |
| 7 | Protocol/persistence/security posture from [B451] apply unchanged (in the 83.5% common region) | `[CERT]`+`[INFER]` | claim 1 + [B451] |

**Tally:** 7 claims — 5 `[CERT]` · 2 `[INFER]`/mixed (labelled) · 0 unmarked. No contradictions with [B450]/[B451].

**Left out (named):** the primary/secondary controller role-partition mechanism; the 10-DO relay driver specifics; per-function map of the 16.5% delta.

## 452.5 — Connections
- **Closes B451-G1.** Confirms [B450] §450.2 (shared 2102 image) at the binary level and extends [B451]'s IO-R-16 findings to the IO-R-34.
- **Completes the nrio thread [B445]–[B452]** on the investigable side: physical/logical connection, conversions, firmware (both modules), protocol, native poller. Only **B449-G1** (`actrld`/`libplatnrio` on the live JACE QNX) remains, hardware-blocked.

## 452.6 — Open gaps
- **B452-G1** — the IO-R-34 primary/secondary role-split: how one image partitions the 34 points across two MSP430 controllers (which addresses/handlers each runs). Investigable (deeper decompile) but low value.
- **B449-G1** (carried) — `actrld`/`libplatnrio` native RE: requires the physical JACE-8000.
