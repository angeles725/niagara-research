# Tools acquired / used for niagara-research

Columns: name · path · WHY (used / adapted / downloaded / created / updated).

| Tool | Path | WHY |
|---|---|---|
| gen-catalog.py | tools/gen-catalog.py | used — regenerates CATALOG.md from block H1s |
| niagara-license-tool.py | tools/niagara-license-tool.py | created (earlier session, analizador-licencias thread) |
| ghidra-scripts/Decompile{CheckFile,Dsfspi,HostId,License,Callers}.java | tools/ghidra-scripts/ | created (2026-08-01) — symbol-anchored Ghidra headless postScripts for dsfspi/nre licensing RE |
| ghidra-scripts/DecompileByString.java | tools/ghidra-scripts/DecompileByString.java | created (2026-08-07) — STRING-anchored Ghidra postScript: decompiles functions that reference a matched string, for symbol-stripped binaries (Mocana-static nverify.exe) where FUN_* names defeat name-filtering. Kit's ExportDecompiledC filters by name only; this fills that gap. |
