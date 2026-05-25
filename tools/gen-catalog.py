#!/usr/bin/env python3
"""
gen-catalog.py — Generate CATALOG.md from niagara-mental-model*.md files.

Run from the repo root:
    python3 tools/gen-catalog.py

Output: CATALOG.md (idempotent — safe to run multiple times).
"""

import re
import os
from pathlib import Path

# ---------------------------------------------------------------------------
# Repository root — always relative to this script's location
# ---------------------------------------------------------------------------
REPO_ROOT = Path(__file__).resolve().parent.parent

CATALOG_PATH = REPO_ROOT / "CATALOG.md"

# ---------------------------------------------------------------------------
# Title overrides for blocks whose H1 lacks a descriptive title.
# Source: INDEX.md "## Repositorio" section descriptions (manually curated).
# ---------------------------------------------------------------------------
TITLE_OVERRIDES: dict[str, str] = {
    "27": "Network surface + puertos + certManagement",
    "28": "Discovery cross-protocol + Virtual Components",
    "29": "Web tier + Servlets + Jetty filter chain",
    "30": "Enterprise auth federation + FIPS + key rotation",
    "31": "Performance tuning + observability + thread pools",
    "32": "Honeywell modules + SMA + non-HTTP + runtime semantics",
    "33": "History system deep + Batch editor",
    "34": "Alarm framework deep + .adb format",
    "35": "Nav + Workbench shell + views registry",
    "45": "History/Trend chart consumption",
}

# ---------------------------------------------------------------------------
# Snapshot files to exclude from the main catalog (listed in a separate section)
# ---------------------------------------------------------------------------
SNAPSHOTS = ["niagara-mental-model.2026-04-19.md"]

# ---------------------------------------------------------------------------
# H1 normalization: strip known prefixes to get the descriptive title only.
#
# Handled patterns:
#   "# Niagara N4 — Mental Model · Bloque N: T"
#   "# Niagara N4 — Bloque N: T"
#   "# Bloque N — T"
#   "# Bloque #N — T"
#   "# Bloque TI — T"
#   "# Bloque #N — T"
# ---------------------------------------------------------------------------
_H1_PREFIX_RE = re.compile(
    r"^#\s+"
    r"(?:Niagara\s+N4\s+[—\-]+\s+)?(?:Mental\s+Model\s+[·\-—]+\s+)?"
    r"Bloque\s+(?:#?\d+|TI)\s*[—:\-]+\s*",
    re.IGNORECASE,
)

# Matches H1 lines that contain ONLY block metadata (no descriptive title follows).
# e.g. "# Niagara N4 — Mental Model · Bloque 27"
_H1_NO_TITLE_RE = re.compile(
    r"^(?:Niagara\s+N4\s+[—\-]+\s+)?(?:Mental\s+Model\s+[·\-—]+\s+)?Bloque\s+(?:#?\d+|TI)\s*$",
    re.IGNORECASE,
)


def extract_h1(path: Path) -> str:
    """Return the first H1 line from a file, or empty string if absent."""
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("# "):
                return line
    return ""


def normalize_title(h1: str) -> str:
    """Strip block-number prefixes from an H1 string, returning only the title.

    Returns empty string when the H1 contains no descriptive title (only block metadata).
    """
    if not h1:
        return ""
    # Remove leading "# "
    text = h1.lstrip("# ").strip()
    # If the entire H1 is just block metadata (no title), return empty so override kicks in.
    if _H1_NO_TITLE_RE.match(text):
        return ""
    # Remove known prefix patterns that precede the actual title
    text = _H1_PREFIX_RE.sub("", "# " + text).lstrip("# ").strip()
    return text


# ---------------------------------------------------------------------------
# Filename → block key extraction
# ---------------------------------------------------------------------------
_FILENAME_RE = re.compile(
    r"^niagara-mental-model"
    r"(?:"
    r"-(bloque(\d+)(?:-[\w-]+)?)"  # group 2 = numeric block number
    r"|-(bloque-(test-infrastructure))"  # group 4 = TI
    r"|\.md$"  # consolidated 1-3 file
    r")?\.md$"
)


def parse_filename(filename: str) -> dict | None:
    """
    Parse a niagara-mental-model*.md filename into a sortable entry dict.

    Returns None for files that should be skipped (snapshots handled separately).
    Returns a dict with keys: sort_key, block_label, filename, title_override_key.
    """
    if filename in SNAPSHOTS:
        return None

    m = _FILENAME_RE.match(filename)
    if not m:
        return None

    # Consolidated bloques 1-3
    if filename == "niagara-mental-model.md":
        return {
            "sort_key": (0, 0),        # always first
            "block_label": "1-3",
            "filename": filename,
            "title_override_key": None,
        }

    # Numeric bloque (4-75 etc.)
    if m.group(2):
        num = int(m.group(2))
        return {
            "sort_key": (1, num),
            "block_label": str(num),
            "filename": filename,
            "title_override_key": str(num),
        }

    # Test infrastructure bloque
    if m.group(4):
        return {
            "sort_key": (2, 0),        # always last
            "block_label": "TI",
            "filename": filename,
            "title_override_key": "TI",
        }

    return None


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def collect_entries() -> list[dict]:
    """Scan REPO_ROOT for niagara-mental-model*.md and build sorted entries."""
    entries = []

    for path in REPO_ROOT.glob("niagara-mental-model*.md"):
        filename = path.name
        parsed = parse_filename(filename)
        if parsed is None:
            continue

        h1 = extract_h1(path)
        title = normalize_title(h1)

        # Apply override if no descriptive title was extracted
        override_key = parsed["title_override_key"]
        if (not title or title.lower().startswith("bloque")) and override_key in TITLE_OVERRIDES:
            title = TITLE_OVERRIDES[override_key]

        # Fallback to raw H1 text (stripped of "# ")
        if not title and h1:
            title = h1.lstrip("# ").strip()

        entries.append({
            "sort_key": parsed["sort_key"],
            "block_label": parsed["block_label"],
            "filename": filename,
            "title": title or "(no title)",
        })

    entries.sort(key=lambda e: e["sort_key"])
    return entries


def render_catalog(entries: list[dict]) -> str:
    """Render the full CATALOG.md content."""
    lines = []

    lines.append(
        "<!-- AUTOGENERADO por tools/gen-catalog.py — NO EDITAR A MANO. "
        "Regenerar: python3 tools/gen-catalog.py -->"
    )
    lines.append("")
    lines.append("# Catálogo de bloques")
    lines.append("")
    lines.append(f"Total: **{len(entries)} bloques**")
    lines.append("")
    lines.append("| Bloque | Archivo | Título |")
    lines.append("|--------|---------|--------|")

    for e in entries:
        link = f"[{e['filename']}]({e['filename']})"
        lines.append(f"| {e['block_label']} | {link} | {e['title']} |")

    lines.append("")
    lines.append("## Snapshots")
    lines.append("")
    lines.append("Archivos de snapshot histórico (excluidos del catálogo activo):")
    lines.append("")
    for s in SNAPSHOTS:
        lines.append(f"- [{s}]({s}) — snapshot sesión httpapi (2026-04-19)")

    lines.append("")

    return "\n".join(lines)


def main() -> None:
    entries = collect_entries()
    content = render_catalog(entries)

    CATALOG_PATH.write_text(content, encoding="utf-8")
    print(f"Generated {CATALOG_PATH.name} — {len(entries)} blocks listed.")
    for e in entries:
        print(f"  Bloque {e['block_label']:>4}  {e['filename']}")


if __name__ == "__main__":
    main()
