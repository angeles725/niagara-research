# Tools acquired / used for niagara-research

Columns: name · path · WHY (used / adapted / downloaded / created / updated).

| Tool | Path | WHY |
|---|---|---|
| gen-catalog.py | tools/gen-catalog.py | used — regenerates CATALOG.md from block H1s |
| niagara-license-tool.py | tools/niagara-license-tool.py | created (earlier session, analizador-licencias thread) |
| bacnet-bbmd-verify.py (+ BACNET-BBMD-VERIFY.md) | tools/bacnet-bbmd-verify.py | created (2026-08-17, B444) — READ-ONLY BACnet/IP verifier: sends real Who-Is (unicast + broadcast) + Read-BDT/Read-FDT to prove whether Who-Is/I-Am crosses subnets (BBMD present) vs device reachable only by pinned-IP unicast. Answers "will Niagara auto-rebind a device on IP change?". Wire constants [CERT] from bacnet-rt bytecode. Runbook tailored per topology. |
| ghidra-scripts/Decompile{CheckFile,Dsfspi,HostId,License,Callers}.java | tools/ghidra-scripts/ | created (2026-08-01) — symbol-anchored Ghidra headless postScripts for dsfspi/nre licensing RE |
| ghidra-scripts/DecompileByString.java | tools/ghidra-scripts/DecompileByString.java | created (2026-08-07) — STRING-anchored Ghidra postScript: decompiles functions that reference a matched string, for symbol-stripped binaries (Mocana-static nverify.exe) where FUN_* names defeat name-filtering. Kit's ExportDecompiledC filters by name only; this fills that gap. |
| FLOSS (flare-floss) | /home/cristian/.local/bin/floss + kit corroborate-floss.sh | used (2026-08-07, NG4) — stack/decoded strings from native binaries (libciper.so ARM) beyond plain `strings` |
| capa (mandiant) | /home/cristian/.local/bin/capa + kit corroborate-capa.sh | available — capability ID on PE/ELF; prioritize functions before Ghidra |

## Reference toolchain per focus (user-recommended 2026-08-07, mapped to what's already installed)

The native-RE toolchain for platform-native (Ghidra ARM/x64, radare2, binwalk, yara, FLOSS, capa, vineflower/cfr/procyon)
is ALREADY installed + kit-wrapped. The following are NEW and belong to OTHER focuses, acquire when that focus opens:

| Tool | Repo | Focus it serves | Status |
|---|---|---|---|
| diffoscope 327 | /home/cristian/.local/share/research-sdd-tools/venv/bin/diffoscope | ACQUIRED 2026-08-07 (kit venv) — deep file/tree diff for license-diff focus (licensed 4.14 vs unlicensed 4.15) |
| pybog | bbartling/pybog | optimizersupervisor (.bog/.dist parse + diff two stations) | not yet acquired |
| diffoscope | reproducible-builds | **licensed-vs-unlicensed two-install diff** (analizador-licencias) | not yet acquired |
| japicmp 0.23.1 | /home/cristian/.local/share/research-sdd-tools/jars/japicmp-jar-with-dependencies.jar | ACQUIRED 2026-08-07 — jar-to-jar API diff; used in B390 (4.14->4.15 additive/backward-compat) |
| foxdissector | MartinoTommasini/foxdissector | protocols (FOX Wireshark dissector; FOXS/TLS needs keys) | not yet acquired |
| SootUp | soot-oss/SootUp | Java callgraph across modules (who-calls-X) | not yet acquired |
| Recaf | Col-E/Recaf | bytecode view when vineflower/cfr disagree | not yet acquired |
| arthas / btrace / byte-buddy | alibaba/btraceio/raphw | DYNAMIC JVM (§12 live-station phase) — attach to running station | not yet acquired |
| tridium/code-samples, summit18-building-niagara | tridium | reference "ground truth" for legit module structure vs decompiled | reference only |
| find-sec-bugs + spotbugs | SAST over Niagara module .jar bytecode (injection, crypto, auth defects) | chihuahua/nmodsreflow/electronicSignature/jsonToolkit security focuses | not yet acquired |
| dependency-check (OWASP) | CVE scan of bundled 3rd-party jars (Gson 2.9.0, JavaMail, jayway-jsonpath, d3, Vue) | any module with vendored deps | not yet acquired |
| semgrep + codeql | pattern/dataflow SAST over decompiled Java or JS (webChart/Reflow ES5) | cross-cutting security | not yet acquired |
| gitleaks | secret scan (twin of kit scan-secrets.sh) over corpus/config | live-install / analizador-licencias | not yet acquired |
| angr / qiling / frida / btrace / x64dbg | symbolic exec / emulation / dynamic instrumentation | NG1-G1 dataflow (angr), §12 live-station dynamic phase (frida/btrace) | not installed (angr/frida heavy pip; note before use) |
| Detect-It-Easy / pe-bear / cutter | packer/compiler ID + PE structure + Ghidra-GUI-lite | native triage (complements radare2/rabin2) | not yet acquired |
| ILSpy / dnSpyEx | .NET decompilers | only if a .NET assembly appears (none found so far; ilspycmd is UNUSABLE per detect-tools) | not yet acquired |

- `qnx6read.py` — read-only QNX6 (Power-Safe) filesystem reader (superblock + 2-level indirection + short/long dir names). Born in focus jace8000-sd (B674) to walk the JACE-8000 microSD QNX6 partitions from a raw image with no `qnx6` kernel driver / no sudo. Usage: `QNX6_IMG=/path/disk.img python3 tools/qnx6read.py P2`. Provenance: 2026-08-30.
| hdbread.py | tools/hdbread.py | created — read-only Niagara .hdb history reader (header+schema+cleartext record walk, --mask) for focus jace-history-audit B699 |
| corpus-nav.py | tools/corpus-nav.py | created (2026-08-31) — corpus navigator over the mental-model blocks + docs/ + retros/. argparse, Python 3 stdlib only, deterministic. Builds an in-memory index at startup (globs block files, parses CATALOG.md for the N↔title map). Robust to the historical numbering gaps; `find` over the ~720 blocks runs in <0.2s. |

## corpus-nav.py commands

Read-only navigator for the knowledge corpus. Run `tools/corpus-nav.py <cmd> -h` for flags.

| Command | What it does |
|---|---|
| `find <query>` | full-text, case-insensitive substring search; prints `B<N> · <title>` + matching `lineno: text`. Flags: `--in blocks\|docs\|retros\|all` (default `blocks`), `--limit` (max matching lines, default 50). |
| `grep <regex>` | same output as `find` but the query is a `re` regex (case-insensitive). Same `--in` / `--limit` flags. |
| `show <N>` | block B<N>'s title + a section outline (`##`/`###` headings) + its verbatim `## Connections` section. |
| `list` | every block as `B<N> · <title>` (from CATALOG.md). `--focus <f>` filters to a focus. |
| `by-marker <marker>` | blocks containing an evidence marker (`INFER`, `CERT`, `CERT-hw`, `CERT-live`, ...); prints `B<N> · count · title`. Handles combined tokens like `[CERT-doc/INFER]`. |
| `by-focus <focus>` | blocks belonging to a focus. Heuristic: union of (1) blocks whose body carries a `Focus: **<focus>**` header tag and (2) block numbers cited in `RESEARCH-STATE-<focus>.md`. |
| `connections <N>` | forward links (block refs inside B<N>'s `## Connections`) + reverse links (whole-corpus scan for blocks that reference B<N>). |
| `stats` | totals: #blocks, #docs, #retros, #focuses, blocks-with-Connections, and evidence-marker occurrence counts. |

Corpus-format notes handled: block references appear as `[Block N]` (dominant), `[Bloque N]`, `[BN]`, or bare `bloqueN`; the consolidated file is label `1-3` and the test-infra file is `TI`; `docs` scope is the 12 direct-child `*.md` guides (subdirs like `docs/JACE8000/` are excluded, matching CATALOG conventions).

## dashboard-preview.py — local preview for a dashboard `-ux` module (design ↔ real, before compiling)

Iterate on a servlet-based dashboard's HTML/CSS/JS **without** the gradle build + sign + deploy cycle. Serves
the module's real `rc/` over `http://localhost` and mocks the servlet API, so you edit → refresh → see. Reusable
across dashboard modules (point it at any `rc/`); reproduces the chihuahua-style **XHR guard** so front-end bugs
(e.g. a fetch missing `X-Requested-With`) surface here before you compile.

```
python3 tools/dashboard-preview.py --rc <module>/src/rc --prefix /dashboardpan [--port 8080] [--mock mock.json]
```
Then open `http://localhost:<port><prefix>/`. The **`/hmi` route** (`http://localhost:<port>/hmi`) frames the dashboard inside a WEB-HMI10/CF **1280×800 panel bezel** (scaled to fit) — the HMI simulator, to see how it looks on the touch panel. `--mock <file.json>` is served for every `GET /api/*`; without it,
`/api/*` returns `{}` (layout/palette still previewable, values show `--`). `POST /api/*` returns `{"ok":true}`
and logs the body. Python 3 stdlib only. A module may ship its own thin wrapper for an **animated** mock — see the
worked example `DashboardPan-ux/preview-server.py` (builds the `{v,st}` payload with jitter + status colors).
