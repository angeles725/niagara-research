# RESEARCH-STATE — focus: jace8000-sd (2 gaps closed incl. QNX partitions read via raw image; 3 child gaps requires-execution/blocked)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** a pedido del operador
> ("puedes ver los archivos del microSD que se utiliza para el JACE-8000"), confirmado como focus nuevo.
> Artefacto FÍSICO (`live-install` / §12 dynamic) → **SECRETS DISCIPLINE**: se cita ESTRUCTURA (rutas,
> formatos, tamaños, sha256, Host ID/creds enmascarados), nunca valores secretos.
>
> **Ángulo:** el contenido REAL del microSD de arranque del JACE-8000 — layout de particiones, cadena de
> boot AM335x, la imagen de firmware firmada, y los defaults de fábrica — leído READ-ONLY del hardware.
> Sibling físico del focus `jace8000` (B459-B475, que cubrió la station/plataforma vía red y serie).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 669
gaps_closed: 2
known_gaps: 5
investigable_open: 0
requires_execution_open: 2
blocked_open: 1
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: jace8000-sd
status: stopped (read-only-investigable exhausted; child gaps need imaging/binary/serial tooling)
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B673)

## Coverage

- **Covered blocks**: 669 corpus-wide (this focus: B672-B673) (shared-global)
- **Coverage metric**: 2 gaps closed (SD1 card anatomy, SD-G1 QNX partitions read)
- **Last iteration**: 2026-08-30 — SD-G1 closed (B673, QNX6 P2/P3 read via raw image)

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | SD1 what is on the boot microSD (partitions, boot chain, firmware, factory defaults) — read-only | live-hw physical | closed (B672) |
| high | SD-G1 read the QNX partitions 2 & 3 (identify FS + key contents) | live-hw raw image | closed (B673 — both QNX6; P2=live Niagara FS, P3=recovery slot) |
| medium | SD-G1b full recursive file tree + per-file extraction of P2/P3 (complete /opt/niagara module list, station files, keyring .km/.kr, logs) | requires qnx6 mount (kernel with CONFIG_QNX6FS_FS or VM) OR a full QNX6 tree parser | requires-execution |
| medium | SD-G2 n4-titan-am335x.signed internals (CertISW cert chain, payload layout, offline verify) | requires binary tooling (Ghidra/r2 + TI CertISW parser) | requires-execution |
| medium | SD-G3 confirm boot chain end-to-end live (does go 0x80FFFC00 verify the CertISW cert before exec) | live serial + boot capture | blocked (needs serial console; cf. jace8000 J7-G1) |

`tried:` SD-G1 — Windows/drvfs cannot read the QNX partitions (shown RAW/Unknown, no drive letter); ruled out
the "just read D:" path. Next rung = raw `dd`/imager of disk 1 partitions 2/3 then a QNX6 reader (not
attempted this session — requires-execution). SD-G3 — bounded by physical serial access to the unit.

## Remittance (ya cubiertos, no son gaps)

- JACE-8000 = QNX / ARM Cortex-A8, station/platform over network+serial → focus `jace8000` (B459-B475).
- Firmware signing (Honeywell PKI, ECDSA) → [Block 394]/[Block 392]. Recovery mechanics → [Block 463]/[Block 466].
- Live hardening + exposed-cred action → [Block 468].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-30 | (bootstrap — physical read) | — | no · inline (PowerShell interop, read-only) | SD-G1, SD-G2, SD-G3 seeded |
| 1 | 2026-08-30 | SD1 microSD contents | B672 | no · inline ([CERT-hw] physical inspection) | 3 child gaps (all non-read-only) |
| 2 | 2026-08-30 | SD-G1 QNX partitions read | B673 | no · inline ([CERT-hw] raw image parse: QNX6 superblock + string/offset scan) | SD-G1b (full tree/extraction, requires-execution) |

## Blocked gaps (each tagged with what it needs)

- SD-G3 — needs: a live serial console on the unit during boot to observe whether `go 0x80FFFC00` chain-verifies the CertISW certificate before executing the firmware. tried: static read of the FAT32 boot partition yields the boot command (`uEnv.txt`) but not runtime verification behavior; bounded by physical serial access to the unit (same wall as jace8000 J7-G1). No network/disk alternative.

(SD-G1 and SD-G2 are requires-execution, not blocked — they need imaging/binary tooling, not a live system; see the backlog table and Stop control.)

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 → STOP (all further work needs qnx6-mount/binary/serial tooling)
- **Open gaps — requires-execution**: 2 (SD-G1b full tree/extraction, SD-G2 CertISW internals)
- **Open gaps — blocked**: 1 (SD-G3)
- Budget cap: none

## Dismissed file types

- `System Volume Information\` on the FAT32 partition — Windows metadata, not JACE content.
