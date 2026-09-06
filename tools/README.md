# Tools acquired / used for niagara-research

Columns: name · path · WHY (used / adapted / downloaded / created / updated).

| Tool | Path | WHY |
|---|---|---|
| gen-catalog.py | tools/gen-catalog.py | used — regenerates CATALOG.md from block H1s |
| dashboard-preview.py | tools/dashboard-preview.py | used — local preview server for any -ux dashboard (serves rc/, mocks the servlet API, enforces the XHR guard, /hmi 1280x800 frame). **`--config-login` (2026-09-06, C9 R14):** previews the "second login inside the dashboard before any write" UX on the REAL rc/ with zero module edits — injects a native-looking modal + session chip + change_log strip and adds stateful mock endpoints `/api/config/login|logout` (+ a mock-only `/api/config/session`) (401 / 200+cookie, sliding TTL `--config-ttl`, demo `--config-password`); the SPA's own `fetch` to `/api/*` is intercepted: no session → modal, the write is held and re-issued after login; a write with a session appends a `change_log` row (surface B, `config_session`) shown in the strip; logout ends it at once. curl-proven 10/10 (runbook: `sources/probes/2026-09-06-c9-r14-config-login-modal-mock.md`). |
| bog-nav.py | tools/bog-nav.py | created (2026-09-05) — READ-ONLY navigator for a station `config.bog` (ZIP+file.xml BOG-XML). Walks the component tree, resolves `sourceOrd='h:xxxx'` link handles to component PATHS (answers "which link feeds Cuarto1.setpoint?" → `Cuarto1.setpoint --> Programacion/ColdRoom_1.setpoint`), classifies each value slot by external-write shape (StatusNumeric=complex, child-leaf bare `<real>` preferred per B826/B825; plain=simple; untyped=bare), and diffs two bogs. Grammar + handle-graph parser REUSED from `build-n4-module-kit/toolbelt/bog-audit.sh` (kit main 3f666a0). Subcommands: tree/slot/links/handle/path/find/writable/relays/hoa/tiles/grep/diff/selftest. `relays` = CHECK11 (own-module output→writable proxy, fallback+writeOnUp; PANCCADIA 22 targets, 17 no-fallback); `hoa` = CHECK8 (mode/HOA slots, auto vs override; 19 auto); `tiles` = CHECK18 (per-RoomPanel evapN tile→unit, flags Cuarto1 units 1/3 crossed); `links --dangling --src` = CHECK7 (tgt slot absent from source). Reads `.bog` and a `.dist` station backup (nested config.bog). stdlib, `--json`/`--csv` on every command, `selftest` (24). |
| module-find.py | tools/module-find.py | created (2026-09-05) — READ-ONLY finder for a module's Java SOURCE tree. Joins multi-line `@NiagaraProperty`/`@NiagaraAction` by PAREN BALANCE (a grep splits on the line break), flags complex (Status*) slots, filters by flags (o/s/h/r/t), follows `extends` (proves a servlet's name is inherited from BWebServlet), and finds STATIC (`setX(`/`.set("slot",`) and DYNAMIC (`obj.set(var,…)` runtime-resolved) writers of a slot. Source-scan engine REUSED from the same bog-audit.sh (kit main 3f666a0). Subcommands: slots/actions/writers/extends/ords/slot-types/ext-writable/compare/callers/grep/selftest. `slot-types` = per-type summary (count/OPERATOR/complex/TRANSIENT), the slot-type-doctrine input; `ext-writable` = S19 lint preview (OPERATOR complex property with no action → WARN + child-leaf note); `compare <root> <srcB>` = annotation-level schema diff between two versions (proven 4f5f1c7→a109249: defrostSkipped/lastSkipReason/forceDefrost ADDED, 0 schema-risk); `callers <method>` = call sites of a method. Prunes dot-dirs. stdlib, `--json`/`--csv` on every command, `selftest` (17). |
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
python3 tools/dashboard-preview.py --rc <module>/src/rc --prefix /dashboardpan [--port 8080] [--mock mock.json] [--config-login [--config-password 1234] [--config-ttl 120]]
```
Then open `http://localhost:<port><prefix>/`. The **`/hmi` route** (`http://localhost:<port>/hmi`) frames the dashboard inside a WEB-HMI10/CF **1280×800 panel bezel** (scaled to fit) — the HMI simulator, to see how it looks on the touch panel. `--mock <file.json>` is served for every `GET /api/*`; without it,
`/api/*` returns `{}` (layout/palette still previewable, values show `--`). `POST /api/*` returns `{"ok":true}`
and logs the body. Python 3 stdlib only. A module may ship its own thin wrapper for an **animated** mock — see the
worked example `DashboardPan-ux/preview-server.py` (builds the `{v,st}` payload with jitter + status colors).

## palette-lexicon-agents.py — palette / lexicon / agent census for N4 modules

Read-only census of a module's authoring surfaces from the extracted corpus
(`organized/<module>/<artifact>/extracted/`): the `module.palette` `<p n= t= m=>` entries, the
`<artifact>.lexicon` keys **with a duplicate-bare-key report** (the B759 silent-override hazard — a key
defined twice, later value wins), and the `<agent>` registrations in `META-INF/module.xml`. Tracked,
stdlib-only port of the (gitignored) `module-navigator` command of the same name — see block **B792** for the
37-module census (10 modules carry duplicate bare keys; worst `schedule-rt summary` ×3).

```
python3 tools/palette-lexicon-agents.py <module> [--base-dir organized] [--json]
python3 tools/palette-lexicon-agents.py --all    [--base-dir organized] [--json]   # corpus census + dup roll-up
```

`--base-dir` defaults to `../organized` next to the script. `--all` prints a `module | palette | lexKeys |
dupKeys | agents` table plus every `(module, artifact:key) ×count` duplicate. Pure helpers
`parse_palette` / `find_duplicate_keys` / `parse_agents` are unit-tested by `tools/tests/test_palette_lexicon_agents.py`
(one biting test — fails if duplicate detection is removed).
