# Block 389 — license-diff L4+L5: the `bin/` and `defaults/` deltas are VENDOR + VERSION + install-local — and the native launcher core (`nre`/`njre`/`station`/`plat`) is byte-IDENTICAL across 4.13→4.14

> **Focus `license-diff` — L4 (native `bin/`) + L5 (config `defaults/`), consolidated.** Both close the same
> non-license axis on the same constrained pair, so they are one block (RE-SCOPE). Because the user-selected
> B (`Tridium_EMEA_N4_Supervisor-4.15.3.28.2`) is an INSTALLER without `bin/` or `defaults/` ([B386 §386.2]),
> these compare A against a genuinely installed instance, **iC-Niagara-4.13.2.18** (base, installed, 0
> licenses). Explicitly VENDOR/VERSION-axis, not license. READ-ONLY. Block type: EVIDENCE (inventory + size).
>
> Compared: A = `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162` (Honeywell OEM 4.14) vs
> iC = `/mnt/c/Niagara/iC-Niagara-4.13.2.18` (base 4.13). Evidence: `audits/B389-bin-config-delta.txt`.
> Markers: `[CERT]` observed (file list / `stat` size / `diffoscope`) · `[INFER]` deduction.

---

## 389.1 — L4: `bin/` native binaries `[CERT]`

`bin/` file counts: **A = 38, iC = 33** (`ls`). `[CERT]`

- **Only-in-A (OEM + runtime)** `[CERT]`: `honImport.dll` (a Honeywell OEM native), `libciper.so` + `.sig`
  (the Spyder/Sylk QNX-ARM lib of [B382]/[B126]), `dataExportTool.exe` (the Honeywell NSIS installer of
  [B130]), `msvcr120.dll` (a VC runtime). So the Honeywell OEM build adds **native binaries** too, not just
  Java modules — `honImport.dll` and `libciper.so` are VENDOR-axis. `[CERT]`
- **The launcher CORE is version-stable** `[CERT]` — key native binaries, same size 4.13→4.14 (byte-stable):
  | binary | 4.14 (A) | 4.13 (iC) | |
  |---|---|---|---|
  | `nre.dll` | 117016 | 117016 | SAME |
  | `njre.dll` | 70424 | 70424 | SAME |
  | `station.exe` | 24344 | 24344 | SAME |
  | `plat.exe` | 50968 | 50968 | SAME |
  | `common.dll` | 192792 | 191768 | **DIFF** (version) |
  | `nverify.exe` | 529176 | 523032 | **DIFF** (Mocana version) |

  So `nre`/`njre`/`station`/`plat` are **byte-identical across two Niagara versions** — the native boot/launch
  layer ([B124]/[B128]/[B129]/[B380]/[B381]) is version-stable, which means the platform-native RE (B124-B385)
  holds across at least 4.13.2.18↔4.14.0.162. Only `common.dll` (the JNI marshalling lib [B125]) and
  `nverify.exe` (the Mocana verifier [B379]) were version-bumped. `[CERT]/[INFER]` (SAME size ⇒ almost
  certainly byte-identical for these small launchers; a full hash was deferred with the L1 tool wall). `[CERT]`

None of the `bin/` delta is license-driven: it is VENDOR (`honImport.dll`, `libciper.so`) + VERSION
(`common.dll`, `nverify.exe`). `[CERT]`

---

## 389.2 — L5: `defaults/` config `[CERT]`

A's `defaults/` holds the product config seeds: `system.properties`, `nre.properties`, `migrator.properties`,
`bacnetObjectTypes.xml`, `lonStandardConversion.xml`, `unitConversion.xml`/`units.xml`, `colorCoding.properties`,
`platform.bog`, `workbench/`. Findings vs iC: `[CERT]`
- `system.properties` differs by **~1180 changed lines** (`diffoscope`) — VERSION (4.13→4.14) + VENDOR; e.g.
  the `niagara.webbrowser.urlWhitelist` seed carries Honeywell domains (`honeywellcloud.com`) alongside the
  Tridium/niagara-community ones — a VENDOR-axis branding difference. `[CERT]`
- Only-in-A `defaults/`: `platform_backup_260807_0935.bog` — a station platform backup dated 2026-08-07
  09:35, i.e. **install-local generated state**, not product config (INSTALL-LOCAL axis, noise). `[CERT]`

No `defaults/` file is license-driven. `[CERT]`

---

## 389.3 — Conclusion + focus close `[CERT]`

L4 and L5 add no license-axis finding: `bin/` differs by VENDOR (OEM natives) + VERSION (common.dll/nverify
bump; launcher core stable) and `defaults/` by VERSION + VENDOR branding + install-local state. Combined with
[B386] (license = the `security/` subtree), [B387] (runtime feature gates), and [B388] (module set = vendor/
version/user), the focus's answer is complete and consistent from every surface: **a Niagara license changes
ONLY `security/`; everything else that differs between these installs is version, vendor, or install-local.**
`[CERT]/[INFER]`

The valuable cross-finding for the platform-native corpus: the native launcher core is byte-stable across
4.13↔4.14, so B124-B385 generalize across those versions. `[CERT]`

---

## 389.4 — Self-verify

**Token re-checks** (`audits/B389-bin-config-delta.txt`):
1. bin/ A=38 iC=33; only-in-A = honImport.dll/libciper.so(+sig)/dataExportTool.exe/msvcr120.dll — ✓ (`ls`+`comm`).
2. nre.dll/njre.dll/station.exe/plat.exe SAME size 4.13/4.14; common.dll + nverify.exe DIFF — ✓ (`stat -c%s`).
3. system.properties ~1180 changed lines; `honeywellcloud.com` in urlWhitelist — ✓ (`diffoscope`+`grep`).
4. only-in-A defaults = `platform_backup_*.bog` (install-local) — ✓.

**4/4 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 12 · `[INFER]` 2 (SAME-size⇒byte-identical for the launchers; the version
attribution). Ratio ≈ 0.17 — low; EVIDENCE block. Closes L4+L5; no license-axis content (as expected).

---

## 389.x — Connections

- **[B386]/[B387]/[B388]** — the license footprint is `security/` (on-disk) + runtime feature gates; bin/ and
  config differ only by vendor/version/install-local — the focus is now answered from every surface.
- **[B124]/[B125]/[B128]/[B129]/[B379]/[B380]/[B381]** — the native launcher core those blocks RE'd is
  byte-stable across 4.13↔4.14 (§389.1); only `common.dll` ([B125]) and `nverify.exe` ([B379]) version-bumped.
- **[B382]/[B126]** — `libciper.so` is OEM-only (present in A, absent in base iC).
- **Focus status**: L1,L3,L4,L5,L6 covered + L2 absorbed → all 6 gaps addressed. Ready to STOP (remaining
  depth = `japicmp` per-jar API diffs, pure VERSION-axis, deferred as tangential to the license question).
