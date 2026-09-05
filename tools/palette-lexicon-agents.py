#!/usr/bin/env python3
"""palette-lexicon-agents — palette / lexicon / agent census for N4 modules.

Tracked, stdlib-only port of the module-navigator command of the same name
(module-navigator/ is gitignored, so this is the durable copy). Reads
read-only from the extracted corpus:

  organized/<module>/<artifact>/extracted/
      module.palette          -> <p n= t= m=> palette entries
      <artifact>.lexicon      -> key=value lexicon (duplicate-bare-key report)
      META-INF/module.xml     -> <agent> registrations

For each artifact it reports:
  1. Palette census: every <p> entry with n=/t=/m= attributes.
  2. Lexicon keys + duplicate-bare-key report. A "bare key" is everything
     before the first '=' on a non-comment, non-blank line; a duplicate is a
     key defined more than once (the later line silently overrides the
     earlier one -- the B759 hazard). See corpus block B759 and B792.
  3. Agent registrations: <agent> elements inside <type> elements of module.xml.

Usage:
  palette-lexicon-agents.py <module> [--base-dir DIR] [--json]
  palette-lexicon-agents.py --all   [--base-dir DIR] [--json]

--base-dir defaults to ../organized relative to this script. --all runs the
census over every module directory under base-dir and prints a summary table
(module | palette | lexicon-keys | dup-keys | agents) plus the duplicate-key roll-up.
"""

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
import zipfile


# ---------------------------------------------------------------------------
# Pure parse helpers (callable directly from tests)
# ---------------------------------------------------------------------------

def parse_palette(text):
    """Parse module.palette XML; return a list of {n,t,m} entry dicts.

    Collects every <p> element in the document (the root <p> is a container;
    its descendants are the actual entries).
    """
    if not text:
        return []
    try:
        root = ET.fromstring(text)
    except ET.ParseError:
        return []

    entries = []
    for elem in root.iter("p"):
        entry = {}
        for attr in ("n", "t", "m"):
            if attr in elem.attrib:
                entry[attr] = elem.attrib[attr]
        if entry:
            entries.append(entry)
    return entries


def find_duplicate_keys(lexicon_text):
    """Return {bare_key: count} for every key defined more than once.

    Java .properties format: key=value, # comments, blank lines. The bare key
    is everything before the first '='. A duplicate silently overrides the
    earlier value (B759 hazard). Only entries with count > 1 are returned.
    """
    counts = {}
    for line in lexicon_text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" not in stripped:
            continue
        key = stripped.split("=", 1)[0].strip()
        if not key:
            continue
        counts[key] = counts.get(key, 0) + 1
    return {k: v for k, v in counts.items() if v > 1}


def count_lexicon_keys(lexicon_text):
    """Return the total number of key=value lines (comments/blanks excluded)."""
    n = 0
    for line in lexicon_text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" in stripped and stripped.split("=", 1)[0].strip():
            n += 1
    return n


def parse_agents(module_xml_text):
    """Parse <agent> registrations from module.xml text.

    Returns a list of {type_name, type_class, on_types} dicts.
    """
    if not module_xml_text:
        return []
    try:
        root = ET.fromstring(module_xml_text)
    except ET.ParseError:
        return []

    agents = []
    for type_elem in root.iter("type"):
        for agent_elem in type_elem.findall("agent"):
            on_types = [on.attrib.get("type", "") for on in agent_elem.findall("on")]
            agents.append({
                "type_name": type_elem.attrib.get("name", ""),
                "type_class": type_elem.attrib.get("class", ""),
                "on_types": on_types,
            })
    return agents


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _read_extracted_file(extracted_dir, filename):
    path = os.path.join(extracted_dir, filename)
    if os.path.isfile(path):
        try:
            with open(path, "r", encoding="utf-8", errors="replace") as f:
                return f.read()
        except IOError:
            return None
    return None


def _read_from_jar(jar_path, member_name):
    if not os.path.isfile(jar_path):
        return None
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            if member_name in zf.namelist():
                return zf.read(member_name).decode("utf-8", errors="replace")
    except (zipfile.BadZipFile, IOError, KeyError):
        pass
    return None


def collect_artifact_data(artifact_dir, artifact_name):
    """Collect palette + lexicon + agents for one artifact directory."""
    result = {
        "artifact": artifact_name,
        "palette_entries": [],
        "lexicon_keys": 0,
        "duplicate_keys": {},
        "agents": [],
    }
    extracted_dir = os.path.join(artifact_dir, "extracted")
    jar_path = os.path.join(artifact_dir, artifact_name + ".jar")

    palette_text = _read_extracted_file(extracted_dir, "module.palette")
    if palette_text is None:
        palette_text = _read_from_jar(jar_path, "module.palette")
    if palette_text is not None:
        result["palette_entries"] = parse_palette(palette_text)

    lexicon_filename = artifact_name + ".lexicon"
    lexicon_text = _read_extracted_file(extracted_dir, lexicon_filename)
    if lexicon_text is None:
        lexicon_text = _read_from_jar(jar_path, lexicon_filename)
    if lexicon_text is not None:
        result["lexicon_keys"] = count_lexicon_keys(lexicon_text)
        result["duplicate_keys"] = find_duplicate_keys(lexicon_text)

    module_xml_text = _read_extracted_file(
        os.path.join(extracted_dir, "META-INF"), "module.xml")
    if module_xml_text is not None:
        result["agents"] = parse_agents(module_xml_text)

    return result


def _find_artifacts(module_dir):
    artifacts = []
    if not os.path.isdir(module_dir):
        return artifacts
    for entry in sorted(os.listdir(module_dir)):
        artifact_dir = os.path.join(module_dir, entry)
        if not os.path.isdir(artifact_dir):
            continue
        has_jar = os.path.isfile(os.path.join(artifact_dir, entry + ".jar"))
        has_extracted = os.path.isdir(os.path.join(artifact_dir, "extracted"))
        if has_jar or has_extracted:
            artifacts.append((entry, artifact_dir))
    return artifacts


def collect_module(base_dir, module_name):
    """Return the list of per-artifact data dicts for a module (or [])."""
    module_dir = os.path.join(base_dir, module_name)
    return [collect_artifact_data(ad, an)
            for an, ad in _find_artifacts(module_dir)]


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def _module_report(module_name, all_data):
    sep = "=" * 65
    out = ["", "  " + sep,
           "  PALETTE / LEXICON / AGENTS: {}".format(module_name),
           "  " + sep, ""]
    tp = sum(len(d["palette_entries"]) for d in all_data)
    tl = sum(d["lexicon_keys"] for d in all_data)
    ta = sum(len(d["agents"]) for d in all_data)
    td = sum(len(d["duplicate_keys"]) for d in all_data)
    out += ["  Artifacts scanned:   {}".format(len(all_data)),
            "  Palette entries:     {}".format(tp),
            "  Lexicon keys:        {}".format(tl),
            "  Duplicate bare keys: {}".format(td),
            "  Agent registrations: {}".format(ta), ""]
    for d in all_data:
        out += ["  " + "-" * 60, "  Artifact: {}".format(d["artifact"]), ""]
        out.append("  [PALETTE]  {} entries".format(len(d["palette_entries"])))
        for e in d["palette_entries"][:50]:
            parts = ["{}={}".format(k, e[k]) for k in ("n", "t", "m") if k in e]
            out.append("    <p {}>".format("  ".join(parts)))
        if len(d["palette_entries"]) > 50:
            out.append("    ... and {} more".format(len(d["palette_entries"]) - 50))
        out.append("")
        out.append("  [LEXICON]  {} keys total".format(d["lexicon_keys"]))
        if d["duplicate_keys"]:
            out.append("  *** DUPLICATE BARE KEYS DETECTED (B759 hazard) ***")
            for key, count in sorted(d["duplicate_keys"].items()):
                out.append("    DUP key={!r}  occurrences={}".format(key, count))
        else:
            out.append("    No duplicate bare keys.")
        out.append("")
        out.append("  [AGENTS]   {} registration(s)".format(len(d["agents"])))
        for ag in d["agents"]:
            out.append("    type_name={!r}  class={!r}  on={}".format(
                ag["type_name"], ag["type_class"], ag["on_types"]))
        if not d["agents"]:
            out.append("    (none)")
        out.append("")
    return "\n".join(out)


def _census(base_dir, as_json):
    modules = sorted(
        m for m in os.listdir(base_dir)
        if os.path.isdir(os.path.join(base_dir, m)) and _find_artifacts(
            os.path.join(base_dir, m)))
    rows = []
    for m in modules:
        data = collect_module(base_dir, m)
        dups = {}
        for d in data:
            for art_key, cnt in d["duplicate_keys"].items():
                dups["{}:{}".format(d["artifact"], art_key)] = cnt
        rows.append({
            "module": m,
            "palette": sum(len(d["palette_entries"]) for d in data),
            "lexicon_keys": sum(d["lexicon_keys"] for d in data),
            "dup_keys": dups,
            "agents": sum(len(d["agents"]) for d in data),
        })
    if as_json:
        print(json.dumps({"base_dir": base_dir, "modules": rows}, indent=2))
        return
    print("")
    print("  {:<24} {:>8} {:>8} {:>8} {:>7}".format(
        "module", "palette", "lexKeys", "dupKeys", "agents"))
    print("  " + "-" * 60)
    dirty = []
    for r in rows:
        print("  {:<24} {:>8} {:>8} {:>8} {:>7}".format(
            r["module"], r["palette"], r["lexicon_keys"], len(r["dup_keys"]),
            r["agents"]))
        if r["dup_keys"]:
            dirty.append(r)
    print("")
    print("  Modules with DUPLICATE bare lexicon keys (B759 hazard): {} / {}".format(
        len(dirty), len(rows)))
    for r in dirty:
        for k, c in sorted(r["dup_keys"].items()):
            print("    {}  {}  x{}".format(r["module"], k, c))


def _default_base_dir():
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.normpath(os.path.join(here, "..", "organized"))


def main(argv=None):
    p = argparse.ArgumentParser(description="Palette/lexicon/agent census for N4 modules.")
    p.add_argument("module", nargs="?", help="module name (e.g. alarm)")
    p.add_argument("--all", action="store_true", help="run the census over all modules")
    p.add_argument("--base-dir", default=_default_base_dir(),
                   help="organized/ root (default: ../organized next to this script)")
    p.add_argument("--json", action="store_true", help="JSON output")
    args = p.parse_args(argv)

    if not os.path.isdir(args.base_dir):
        print("ERROR: base-dir not found: {}".format(args.base_dir), file=sys.stderr)
        return 2
    if args.all:
        _census(args.base_dir, args.json)
        return 0
    if not args.module:
        p.error("give a module name or --all")
    data = collect_module(args.base_dir, args.module)
    if not data:
        msg = "No artifacts for module '{}' under {}".format(args.module, args.base_dir)
        if args.json:
            print(json.dumps({"module": args.module, "error": msg}, indent=2))
        else:
            print("  WARNING: " + msg)
        return 0
    if args.json:
        print(json.dumps({
            "module": args.module,
            "artifacts": [{
                "artifact": d["artifact"],
                "palette_count": len(d["palette_entries"]),
                "palette_entries": d["palette_entries"],
                "lexicon_keys": d["lexicon_keys"],
                "duplicate_bare_keys": d["duplicate_keys"],
                "agents": d["agents"],
            } for d in data],
        }, indent=2))
    else:
        print(_module_report(args.module, data))
    return 0


if __name__ == "__main__":
    sys.exit(main())
