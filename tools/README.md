# Tools acquired / used for niagara-research

Columns: name · path · WHY (used / adapted / downloaded / created / updated).

| Tool | Path | WHY |
|---|---|---|
| gen-catalog.py | tools/gen-catalog.py | used — regenerates CATALOG.md from block H1s |
| niagara-license-tool.py | tools/niagara-license-tool.py | created (earlier session, analizador-licencias thread) |
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
| japicmp | siom79/japicmp | jar version diff (N4.13 vs 4.14 module compare) | not yet acquired |
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
