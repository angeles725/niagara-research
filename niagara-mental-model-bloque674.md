# B674 — JACE-8000 microSD QNX6 tree fully walked (custom read-only reader, no mount): P2 is the complete QNX Niagara root filesystem (98 dirs / 599 files — `/opt/niagara` install with 173 module JARs + 10 native `.so`, full JRE, the `JACE_UMBRELLA` station with `config.bog`/alarm.adb/histories, and the keyrings `/etc/km/.km` + `/home/niagara/security/.kr` + `/.fskey/.key`); P3 is the factory-recovery partition (`n4clean.tar.gz`, maint image, boot chain) — closing SD-G1b (focus jace8000-sd; §12/§19 [CERT-hw])

> **Focus:** `jace8000-sd` (§16). **Gap closed:** SD-G1b (full recursive file tree + inode-level structure of
> the QNX6 partitions). **Phase:** §12 dynamic + §19 build — a purpose-built QNX6 reader run over the raw image.
> **Marker:** `[CERT-hw]` — the real card's filesystem, parsed inode-by-inode.
> **Tool (born here, §10):** `tools/qnx6read.py` — read-only QNX6 Power-Safe reader (superblock → inode file
> with 2-level block indirection → directory walk with short + Longfile-backed long names). No kernel `qnx6`
> driver and no sudo were available, so the tree was reconstructed directly from the image bytes.
> **Sources:** `sources/probes/B672-jace8000-sd/{qnx-p2-tree.txt,qnx-p3-tree.txt}` (full listings, Host ID
> masked) · `[CERT]` [Block 672]/[Block 673] (card geometry + QNX6 identity), [Block 473] (`JACE_UMBRELLA`
> config.bog live), [Block 466] (key domains), [Block 392]/[Block 395] (certs/signing).
> **SECRETS DISCIPLINE (live-install):** every secret-bearing file is listed by **name + size only** — no
> contents dumped; the Host ID in the license path is masked; the raw image and any extracted bytes stay in
> the scratchpad, never committed.
>
> **Bottom line:** the QNX6 partitions are now fully enumerated. **P2 = the JACE-8000's live QNX root
> filesystem** — 98 directories / 599 files: `/opt/niagara` (the install: `bin/` natives, `jre/`, `defaults/`,
> **`modules/` with 173 JARs**, `security/{certificates,licenses}`) and `/home/niagara` (daemon home:
> `registry/registry.db`, `security/` keyring + keystores, and **`stations/JACE_UMBRELLA/`** with `config.bog`,
> `alarm/alarm.adb`, and the history `.hdb` set). **P3 = the factory-recovery partition** — a full boot chain
> plus `maint/n4-titan-am335x-maint.signed` and **`n4clean.tar.gz` (a ~43 MB clean factory image)**.

---

## §674.1 — How it was walked: a custom QNX6 reader, no mount `[CERT-hw]`

With no `qnx6` kernel driver in this WSL2 kernel and no sudo ([Block 673] §673.1), `tools/qnx6read.py` reads
the QNX6 Power-Safe structures straight from the image:
- QNX6 superblock at partition+`0x2000` (magic `0x68191122`); header is **72 bytes**, then three 80-byte
  root nodes (Inode, Bitmap, Longfile).
- The **inode file** is itself a QNX6 file: the Inode root node has `levels=2` block indirection (each pointer
  → indirect block of 256 `u32` → … → data). Device block b lives at byte `partitionStart + (b + OFF)*1024`,
  with **`OFF=12`** auto-detected by validating inode 1 as a directory.
- Inodes are 128 B (`di_size@0`, `di_mode@32`, `di_block_ptr[16]@36`, `di_filelevels@100`); directory entries
  are 32 B (`de_inode@0`, `de_size@4`, short name in `[5:5+size]`; `de_size==0xff` → a long name resolved from
  the **Longfile** tree). Walking from inode 1 (`QNX6_ROOTINO`) yields the whole tree.
Validation that the parse is correct: the walk reproduces the exact inode counts from the superblock (P2 →
**98 dirs + 599 files = 697 ≈ 699 used inodes**, [Block 673] §673.2) and yields sensible Unix modes, real
filenames, and file sizes. READ-ONLY throughout.

## §674.2 — P2 = the QNX Niagara root filesystem `[CERT-hw]`

Top level: `/.boot  /.fskey  /etc  /home  /opt  /root  /var  /zip`. The two load-bearing trees:

### `/opt/niagara` — the Niagara install
| Path | Contents |
|---|---|
| `bin/` | 10 native `.so` (`libnre.so`, `libnjre.so`, `libdsfspi.so`, `libcommon.so`, `libbacnet.so`, `libplatccn/mstp/nrio.so`, `libpower.so`, `libserial.so`) + `niagarad`, `nre`, `station`, `nreVersion.xml`, `policy/`, `META-INF/`, `ext/` |
| `jre/` | a full JRE (incl. `jre/lib/security/{cacerts, cacerts.bcfks, cacerts.bcfks.sig, cacerts.sig, java.security, java.policy, blacklisted.certs}`) — the signed-truststore set from [Block 392] |
| `defaults/` | `system.properties` (31 KB), `nre.properties`, `platform.bog` + `platform_backup_260817_0402.bog`, `bacnetObjectTypes.xml` (123 KB), `units.xml`, etc. |
| `etc/` | `brand.properties`, `extensions.properties`, `loading-splash.gif` |
| **`modules/`** | **173 `*.jar`** — this JACE's actual module set (`alarm-rt/ux/wb`, `bacnet-*`, `baja.jar`, `backup-*`, `app-*`, `apachePoi-rt`, `airFlowBalancer`, `axvelocity-*`, … the OEM/Honeywell + Tridium set) |
| `security/` | `certificates/{Honeywell,HoneywellCentraLine,Tridium}.certificate`; `licenses/{Honeywell,HoneywellCentraLine,Webs}.license` + a `db/` and a **`<HostId>/`** subdir named by the Host ID (`Qnx-TITAN-44A2-****-****-363E`, masked — matches [Block 473]) holding the same three `.license` files + `inbox/` |

### `/home/niagara` — the daemon home
| Path | Contents |
|---|---|
| `daemon/` | `daemon.properties`, `daemonlog.properties` |
| `registry/` | `registry.db` (843 KB — the module/type registry), `registry.chk` |
| `security/` | **`.kr`** (665 B keyring), `keystore.jceks` (4.5 KB), `cacerts.jceks`, `untrusted.jceks`, `exemptions.tes`, `signing/signers` (33 KB) |
| **`stations/JACE_UMBRELLA/`** | **`config.bog`** (7843 B), `alarm/alarm.adb` (17 KB), `dataRecovery/`, `history/station/seg0..seg7/*.hdb` (incl. `SecurityHistory.hdb`, `AuditHistory.hdb`, `LogHistory.hdb`, device/network job records), `shared/` |
| `logging/`, `shared/`, `sw/`, `etc/` | daemon logging + provisioning working dirs |

This confirms, on physical media, the JACE-8000 layout [Block 462] sketched (`/opt/niagara` install +
`/home/niagara` user/stations), and pins the **exact station** `JACE_UMBRELLA` whose `config.bog` was pulled
live over Fox in [Block 473].

## §674.3 — Keyrings and secrets located on disk `[CERT-hw]` (structure only)

The read-at-rest key material referenced abstractly in the corpus is now located, by name + size (contents
never dumped — SECRETS DISCIPLINE):

| File | Size | Role (corpus link) |
|---|---|---|
| `/.fskey/.key` | 156 B | QNX filesystem encryption key |
| `/etc/km/.km` | 32 B | machine keyring ("km") — the daemon-home / machine-only key domain ([Block 466]) |
| `/home/niagara/security/.kr` | 665 B | Niagara keyring ("kr") ([Block 466]/[Block 13]) |
| `/home/niagara/security/keystore.jceks` | 4476 B | station keystore |
| `/etc/passwd` / `/etc/shadow` (+`opasswd`/`oshadow`) | 180/343 B | QNX OS accounts |
| `/opt/niagara/security/certificates/*.certificate` | ~835 B each | vendor certs (Honeywell/CentraLine/Tridium — [Block 395]) |

**Security note (consistent with [Block 468]/[Block 672]):** the OS `shadow`, the JACE keyrings, the station
`config.bog`, and the `SecurityHistory`/`AuditHistory` are all readable off this card by anyone with a card
reader — physical possession of the SD is full compromise of the controller's data-at-rest (the machine-key
domain protects the *live-running* decrypt, but the key files themselves sit here). Handle the card as a
secret; the exposed factory credential ([Block 672] §672.5) compounds it.

## §674.4 — P3 = the factory-recovery partition (full tree) `[CERT-hw]`

P3 has just **2 dirs / 8 files**:
```
/.boot
/fac.properties            144 B   (factory defaults, again — [Block 672] §672.5)
/mlo                     34,152 B   ] the boot chain, mirrored from P1
/u-boot.img             268,984 B   ]
/uEnv.txt                    78 B   ]
/n4-titan-am335x.signed 27,149,316 B   (a 27 MB signed image — NOTE: DIFFERENT size than P1's 40 MB copy)
/maint/n4-titan-am335x-maint.signed  1,374,248 B   (a ~1.3 MB maintenance image)
/maint/n4-titan-am335x-maint.ver             9 B   (its version string)
/n4clean.tar.gz         43,313,692 B   (~43 MB — a CLEAN factory image tarball = the factory-reset payload)
```
So P3 is a **self-contained factory-recovery slot**: its own boot chain + a clean-image tarball + a
maintenance image — the on-card mechanism behind the JACE-8000 "factory defaults / recovery" story
([Block 463]). The P3 `.signed` main image (27 MB) differing in size from P1's deployed image (40 MB,
[Block 672]) indicates P3 holds a *different* (factory/clean) build than the one currently booting. `[INFER
from size delta]`

## §674.5 — What remains (SD-G2 / SD-G3) `[CERT-hw]`

The tree is complete; per-file extraction is now trivial with the reader (the raw image is preserved in the
scratchpad). Deliberately NOT done here (SECRETS / scope): dumping secret file *contents*. Still open:
- **SD-G2** (requires binary tooling): the CertISW/`.signed` image internals + `n4clean.tar.gz` unpack.
- **SD-G3** (blocked): live serial confirmation of the boot-time signature check.
`config.bog` itself is already available decoded via the live Fox pull ([Block 473]); it need not be
extracted from the card.

## §674.6 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | QNX6 tree walked with a custom reader (no mount/sudo); OFF=12, inode file levels=2 | [CERT-hw] | tools/qnx6read.py; §674.1 |
| 2 | Parse validated: P2 = 98 dirs + 599 files ≈ 699 used inodes | [CERT-hw] | qnx-p2-tree.txt; [Block 673] |
| 3 | /opt/niagara = install: 10 native .so, full jre, defaults, 173 module JARs, certs+licenses | [CERT-hw] | qnx-p2-tree.txt |
| 4 | /home/niagara/stations/JACE_UMBRELLA = config.bog + alarm.adb + history .hdb set | [CERT-hw] | qnx-p2-tree.txt |
| 5 | keyrings on disk: /.fskey/.key, /etc/km/.km, /home/niagara/security/.kr, keystore.jceks (+ OS shadow) | [CERT-hw] | qnx-p2-tree.txt; [Block 466] |
| 6 | licenses under a Host-ID dir Qnx-TITAN-44A2-****-****-363E (matches B473) | [CERT-hw] | qnx-p2-tree.txt; [Block 473] |
| 7 | P3 = recovery: boot chain + maint image + n4clean.tar.gz (43 MB) + 27 MB .signed (≠ P1's 40 MB) | [CERT-hw] | qnx-p3-tree.txt; [Block 672] |
| 8 | physical possession of the card = full data-at-rest exposure | [CERT-hw] + [INFER] | §674.3 |

**Tally:** 8 claims — 8 [CERT-hw] (two carry an [INFER] synthesis, flagged inline: P3 build-delta, physical
exposure). 0 unmarked. Secret file CONTENTS deliberately not read (scope/SECRETS).

## §674.7 — Connections

- **[Block 672]/[Block 673]** — card geometry + QNX6 identity; this block completes them with the full tree.
- **[Block 462]** — JACE-8000 QNX filesystem layout (docs); verified on media here.
- **[Block 473]** — live Fox pull of `JACE_UMBRELLA/config.bog`; same station, here on disk.
- **[Block 466]/[Block 13]** — the `.km`/`.kr` key domains, now located at `/etc/km/.km` + `/home/niagara/security/.kr`.
- **[Block 392]/[Block 395]** — cacerts/cacerts.bcfks + vendor `.certificate` files, present under `/opt/niagara`.
- **[Block 463]** — recovery mechanics; P3's `n4clean.tar.gz` + maint image are the on-card payloads.
- **[Block 468]/[Block 672]** — exposed-credential + data-at-rest posture, reinforced by §674.3.
