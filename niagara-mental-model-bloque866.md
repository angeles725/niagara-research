# Block 866 — B854-G2 and B856-G1 residual closures: GreenMAX11 TabAL9 stub verdict + kitPx-ux first-ship wall

> **Focus:** harbor-greenmax-lighting.  
> **Scope:** Two narrow residual gaps from the harbor corpus — whether `GreenMAX11 TabAL9.px`
> is an empty placeholder or a real panel, and whether the N4 minor version where `kitPx-ux.jar`
> first shipped can be pinned beyond the [INFER] N4.7–N4.10 bound established in B856 §2.3.
>
> **Evidence sources:**
> - `[CERT]` = verbatim file content at cited path, line count, byte size, widget count.
> - `[INFER]` = derived from [CERT] evidence.
> - `unavailable` = typed wall — query attempted, artifact absent from corpus.

---

## 1. B854-G2 — `GreenMAX11 TabAL9.px`: empty stub or real panel?

### 1.1 File facts

Path: `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/shared/px/GreenMAX11 TabAL9.px`

| Attribute | Value |
|---|---|
| Byte size | 200 bytes |
| Line count | 14 |
| `<import>` modules | `gx`, `bajaui` only |
| `<content>` structure | `ScrollPane > CanvasPane name="content" viewSize="500,400"` |
| CanvasPane children | **none** |
| Relay bindings (`slot:Relay*`) | **0** |
| Widget count | **0** |

`[CERT]` `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/shared/px/GreenMAX11 TabAL9.px:1-14`

The `viewSize='500,400'` is the bare Workbench new-file default (no content was ever placed).
The import list (`gx`, `bajaui`) is the minimal skeleton — a real GreenMAX panel imports
`kitPx`, `control`, `converters` in addition. `[INFER]` from comparison with populated panels
in the same `shared/px/` directory.

### 1.2 Comparison: the real GM11 panel

`GreenMAX11 TabAL10.px` (same directory, same station backup):

| Attribute | Value |
|---|---|
| Line count | 1614 |
| `<content>` structure | `ScrollPane > CanvasPane viewSize="1370.0,780.0"` |
| Relay bindings | 26 (`slot:Relay$5b1$5d$2eBO` … `slot:Relay$5b26$5d$2eBO`) |
| Imports | `baja`, `bajaui`, `control`, `converters`, `gx`, `kitPx` |
| Panel title label | `"GreenMAX 11"` (confirmed) |

`[CERT]` `clients/distech-merida-harbor/HM_BMS_backup_08_09_2026/shared/px/GreenMAX11 TabAL10.px:1-20,70`

GM11's relay count = 26 matches B854's per-panel survey. `[CERT-live]` (owned by B854).

### 1.3 Verdict

**`GreenMAX11 TabAL9.px` is an orphaned empty placeholder — never populated.** It holds only
the Workbench skeleton (`ScrollPane > CanvasPane`, no imports for kitPx or control, no relay
bindings, 200 bytes). The actual GM11 tablero view is `GreenMAX11 TabAL10.px` (1614 lines,
26 relays, proper import list). The `TabAL9` file can be ignored or deleted from the station
backup; it presents no real view when opened. `[CERT]`

No cross-panel confusion: `GreenMAX07 TabAL8.px` was a naming anomaly (GM08's view mislabeled
as AL8 — documented in B854). `GreenMAX11 TabAL9.px` is a different kind of anomaly: the label
is plausible (AL9 could be a tablero suffix), but the content is empty — never built out.

---

## 2. B856-G1 — `kitPx-ux.jar` first-ship N4 minor version: typed unavailable wall

### 2.1 Queries attempted

| Query | Tool | Result |
|---|---|---|
| `module_nav.py resources kitPx-ux` | `module-navigator/tools/module_nav.py` | "JAR not found: `.../modules/kitPx-ux.jar`" |
| `niagara_help.py find "kitPx-ux"` | `niagara-help/tools/niagara_help.py` | "No results for 'kitPx-ux' across all sources." |
| Scan `organized/` for N4.7–N4.11 installs | `ls organized/ \| grep -E 'n4\.(7\|8\|9\|10\|11)'` | **No entries found** — corpus has no install between N4.4 and N4.14 |
| `find organized/ -name 'module.xml' -path '*kitPx-ux*'` | `find` | Only two results: `organized/kitPx/kitPx-ux/{extracted,vineflower}/META-INF/module.xml` — both N4.14.0.162 |

### 2.2 Standing bound

From B856 §2.3 (carried forward unchanged):

| Fact | Marker |
|---|---|
| N4.3 (EC-Net 4.3.58.18): `kitPx-ux.jar` absent | [CERT] — `organized/_client-ecnet-4.3.58.18-n4.4/` directory listing |
| N4.14.0.162: `kitPx-ux.jar` present | [CERT] — `organized/kitPx/kitPx-ux/extracted/META-INF/module.xml:2` |
| UxMedia (Px-as-JSON → bajaux render) introduced N4.10 | [CERT] from B13 §13.3.4 |
| bajaux itself exists since N4.0 | [CERT-doc] `uiFromAxToN4.txt:L121` |

Working bound: `kitPx-ux.jar` appeared somewhere in N4.7–N4.10. **[INFER]** — unchanged from
B856 §2.3. The gap **remains partial**: pinning to a specific minor version requires an N4.7,
N4.8, or N4.9 install outside the current corpus.

---

## Self-verify table

| Claim | Evidence | Marker | Verified |
|---|---|---|---|
| TabAL9.px = 200 bytes | `wc -c` output | [CERT] | ✓ |
| TabAL9.px = 0 relay bindings | `<CanvasPane/>` has no children | [CERT] | ✓ |
| TabAL9.px imports = gx + bajaui only | File lines 3–6 | [CERT] | ✓ |
| TabAL9.px CanvasPane viewSize = 500,400 | File line 10 | [CERT] | ✓ |
| TabAL10.px = 1614 lines, 26 relays | Line count + grep `slot:Relay` | [CERT] | ✓ |
| TabAL10.px is the real GM11 panel | Label `"GreenMAX 11"` + relay count matches B854 | [CERT] | ✓ |
| No N4.7–N4.11 kitPx-ux in corpus | Directory scan returns no matches | unavailable wall | ✓ |
| module_nav + niagara_help return no results | Tool output literal | unavailable wall | ✓ |

**Tally — [CERT]: 6 · [INFER]: 1 · unavailable walls: 2 · invented facts: 0**

---

## Connections

- **B854** — per-panel relay counts confirmed (`GreenMAX11` = 26 relays via TabAL10.px
  [CERT-live]); TabAL9 identified as the empty co-existing stub for that same panel. B854-G2
  **CLOSED** by this block.
- **B856** — §2.3 N4.7–N4.10 kitPx-ux bound unchanged; B856-G1 **PARTIAL** (unavailable wall
  typed, gap stays open pending an N4.7/N4.8/N4.9 install in the corpus).

---

## Open gaps

- **B856-G1** (partial, investigable) — exact N4 minor version where `kitPx-ux.jar` first
  shipped; requires a corpus install between N4.4 and N4.14. Bound N4.7–N4.10 stands [INFER].
- All other harbor investigable gaps are either req-exec (live station) or client-input-blocked
  (see RESEARCH-STATE coverage section).
