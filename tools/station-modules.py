#!/usr/bin/env python3
"""station-modules.py — resolve the module dependencies of a Niagara N4 station
against a local Workbench/platform install, and locate the missing jars.

WHY THIS EXISTS
    Copying someone else's station into your Workbench and double-clicking
    config.bog fails with, e.g.:

        com.tridium.file.types.bog.CannotLoadBogException ...
        Cannot load module 'dsb=dashboard'
        Cannot resolve dependency dashboard-rt-Tridium-4.14.0 for dashboard-wb-...
        ModuleNotFoundException: dashboard-rt

    The station's config.bog references component TYPES that live in MODULE
    PARTS (<module>-rt / -wb / -ux ...). To decode the bog, every referenced
    part AND its declared dependencies must be present in the install's
    `modules/` folder. A partial install (e.g. dashboard-wb present but
    dashboard-rt missing) throws exactly the error above.

    This tool answers, read-only:
      * which modules/types the station references,
      * which required module-parts are NOT installed (the real blockers),
      * whether the missing jar is already staged in the install's `sw/` tree
        (so it can just be copied into `modules/`), and
      * with --fix, copies the version-matched staged jars into `modules/`.

    Read-only over the station and the install unless --fix is passed.
    Stdlib only. Companion to tools/bog-nav.py (bog XML reader) and
    tools/module-find.py (module source scanner).

USAGE
    station-modules.py check <config.bog|.dist|file.xml> [--install ROOT] [--fix] [--json]
    station-modules.py doctor [--install ROOT] [--fix] [--json]
        check  — station-driven: what THIS station needs vs what is installed.
        doctor — install integrity: every installed part's unresolved deps
                 (catches the "wb without rt" class regardless of any station).

    --install ROOT  Niagara install root (holds modules/ and sw/). If omitted,
                    autodetects from a short candidate list, else errors.
    --fix           Copy version-matched jars found under sw/ into modules/.
                    Only copies Tridium-signed staged jars verbatim; never
                    fabricates a module. Prints every action.
    --json          Machine-readable output.

EXIT CODES
    0 = fully resolved (nothing missing)   1 = missing parts remain   2 = usage/IO error
"""
import sys, os, re, json, zipfile, shutil, argparse, hashlib
import xml.etree.ElementTree as ET

INSTALL_CANDIDATES = [
    "/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162",
    "/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162",
]


# ----------------------------- install index --------------------------------
def _read_zip_member(jar, name):
    try:
        with zipfile.ZipFile(jar) as z:
            return z.read(name)
    except (KeyError, zipfile.BadZipFile, OSError):
        return None


def parse_module_xml(raw):
    """Return dict for one module-part from its META-INF/module.xml bytes."""
    try:
        root = ET.fromstring(raw)
    except ET.ParseError:
        return None
    a = root.attrib
    types = set()
    for t in root.iter("type"):
        n = t.attrib.get("name")
        if n:
            types.add(n)
    deps = []
    for d in root.iter("dependency"):
        n = d.attrib.get("name")
        if n:
            deps.append(n)
    return {
        "part": a.get("name", ""),          # e.g. dashboard-wb  (the part jar name)
        "moduleName": a.get("moduleName", ""),  # e.g. dashboard
        "profile": a.get("runtimeProfile", ""),  # rt / wb / ux / se / doc
        "symbol": a.get("preferredSymbol", ""),
        "vendor": a.get("vendor", ""),
        "version": a.get("vendorVersion", ""),
        "types": types,
        "deps": deps,
    }


def index_install(root):
    """Index every module-part jar under <root>/modules/. Returns:
       parts: {partName -> meta}, and a staged sw/ jar map for fixes."""
    modules_dir = os.path.join(root, "modules")
    if not os.path.isdir(modules_dir):
        sys.exit(f"error: no modules/ under install root {root!r}")
    parts = {}
    for fn in sorted(os.listdir(modules_dir)):
        if not fn.endswith(".jar"):
            continue
        raw = _read_zip_member(os.path.join(modules_dir, fn), "META-INF/module.xml")
        if raw is None:
            continue
        meta = parse_module_xml(raw)
        if meta and meta["part"]:
            meta["jar"] = os.path.join(modules_dir, fn)
            parts[meta["part"]] = meta
    return parts, modules_dir


def index_sw(root):
    """Map partName -> [staged jar paths] found anywhere under <root>/sw/."""
    sw = os.path.join(root, "sw")
    staged = {}
    if not os.path.isdir(sw):
        return staged
    for dirpath, _, files in os.walk(sw):
        for fn in files:
            if fn.endswith(".jar"):
                staged.setdefault(fn[:-4], []).append(os.path.join(dirpath, fn))
    return staged


# ----------------------------- station parse --------------------------------
def load_station_xml(path):
    """Return file.xml text from a .bog/.dist zip or a raw .xml path."""
    if path.endswith(".xml"):
        with open(path, "rb") as f:
            return f.read().decode("utf-8", "replace")
    try:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            if "file.xml" in names:
                return z.read("file.xml").decode("utf-8", "replace")
            # .dist backup: config.bog nested inside
            for n in names:
                if n.endswith("config.bog"):
                    inner = z.read(n)
                    with zipfile.ZipFile(__import__("io").BytesIO(inner)) as z2:
                        return z2.read("file.xml").decode("utf-8", "replace")
    except (zipfile.BadZipFile, KeyError, OSError) as e:
        sys.exit(f"error: cannot read station file {path!r}: {e}")
    sys.exit(f"error: no file.xml/config.bog inside {path!r}")


def station_refs(xml):
    """Return (alias->module map, {module -> set(TypeName)}) referenced by the bog.
       Module aliases are declared inline as m="alias=module"; types as
       t="alias:Type" (quotes may be ' or ")."""
    alias = {}
    for a, m in re.findall(r'm=["\']([A-Za-z0-9]+)=([A-Za-z0-9]+)["\']', xml):
        alias[a] = m
    types = {}
    for al, tn in re.findall(r't=["\']([A-Za-z0-9]+):([A-Za-z0-9_]+)["\']', xml):
        mod = alias.get(al)
        if mod:
            types.setdefault(mod, set()).add(tn)
    # modules referenced even if we didn't capture a concrete type
    for m in set(alias.values()):
        types.setdefault(m, set())
    return alias, types


# ----------------------------- resolution -----------------------------------
def installed_parts_for_module(parts, module):
    return {p: parts[p] for p in parts if parts[p]["moduleName"] == module}


def find_type_provider(parts, module, typename):
    for p, meta in parts.items():
        if meta["moduleName"] == module and typename in meta["types"]:
            return p
    return None


def resolve_deps(parts, seed_parts):
    """Transitive dependency closure over installed parts; collect any
       dependency whose part is NOT installed."""
    seen, missing, stack = set(), set(), list(seed_parts)
    while stack:
        p = stack.pop()
        if p in seen:
            continue
        seen.add(p)
        meta = parts.get(p)
        if not meta:
            continue
        for dep in meta["deps"]:
            if dep in parts:
                stack.append(dep)
            else:
                missing.add(dep)
    return seen, missing


def jar_version(path):
    """Real vendorVersion from a jar's META-INF/module.xml, or '' if unknown."""
    raw = _read_zip_member(path, "META-INF/module.xml")
    if raw is None:
        return ""
    meta = parse_module_xml(raw)
    return meta["version"] if meta else ""


def locate_fix(part, staged, want_version):
    """Pick a staged jar for `part` whose REAL manifest version matches the
       install (never trust the sw/<dir> path string — sw/1.0/ can hold a v1.0
       stub). Returns (path, version) or None."""
    cands = staged.get(part, [])
    if not cands:
        return None
    scored = [(c, jar_version(c)) for c in cands]
    if want_version:
        exact = [(c, v) for c, v in scored if v == want_version]
        if exact:
            return exact[0]
        # same major.minor.patch, differing build
        base = ".".join(want_version.split(".")[:3])
        near = [(c, v) for c, v in scored if v.startswith(base + ".") or v == base]
        if near:
            return sorted(near, key=lambda cv: cv[1])[-1]
    # no version hint: take the highest real version, never a bare "1.0" stub
    real = [(c, v) for c, v in scored if v and v != "1.0"] or scored
    return sorted(real, key=lambda cv: cv[1])[-1]


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def apply_fix(part, src, modules_dir):
    dst = os.path.join(modules_dir, part + ".jar")
    if os.path.exists(dst):
        return dst, "exists"
    shutil.copy2(src, dst)
    return dst, "copied"


# ----------------------------- commands -------------------------------------
def autodetect_install(explicit):
    if explicit:
        if not os.path.isdir(explicit):
            sys.exit(f"error: install root not found: {explicit!r}")
        return explicit
    for c in INSTALL_CANDIDATES:
        if os.path.isdir(os.path.join(c, "modules")):
            return c
    sys.exit("error: could not autodetect a Niagara install; pass --install ROOT")


def cmd_check(args):
    root = autodetect_install(args.install)
    parts, modules_dir = index_install(root)
    staged = index_sw(root)
    xml = load_station_xml(args.target)
    _, refs = station_refs(xml)

    missing_parts = {}   # part -> reason
    reported = []
    seed = set()
    for module in sorted(refs):
        inst = installed_parts_for_module(parts, module)
        for tn in sorted(refs[module]):
            prov = find_type_provider(parts, module, tn)
            if prov:
                seed.add(prov)
            else:
                # type not provided by any installed part of this module
                need = f"{module}-rt"  # runtime is the usual provider of component types
                missing_parts.setdefault(need, f"provides {module}:{tn} (used by station)")
        if not inst and not refs[module]:
            missing_parts.setdefault(f"{module}-rt", f"module {module} referenced, no part installed")
        reported.append((module, sorted(inst), sorted(refs[module])))

    # transitive dependency check over the installed parts the station touches
    _, dep_missing = resolve_deps(parts, seed)
    for dm in dep_missing:
        missing_parts.setdefault(dm, "dependency of an installed part the station uses")

    fixes = plan_fixes(missing_parts, parts, staged, modules_dir, args.fix)

    if args.json:
        print(json.dumps({
            "install": root, "station": args.target,
            "modules": {m: {"installed": i, "types": t} for m, i, t in reported},
            "missing": missing_parts, "fixes": fixes,
        }, indent=2))
    else:
        print(f"install : {root}")
        print(f"station : {args.target}")
        print(f"modules referenced: {len(reported)}")
        for m, inst, ts in reported:
            tag = ",".join(sorted({parts[p]['profile'] for p in inst})) if inst else "— NONE —"
            tsug = f"  types:{','.join(ts)}" if ts else ""
            print(f"  {m:<18} parts:[{tag}]{tsug}")
        print_missing_and_fixes(missing_parts, fixes)
    return 0 if not missing_parts else 1


def cmd_doctor(args):
    root = autodetect_install(args.install)
    parts, modules_dir = index_install(root)
    staged = index_sw(root)
    missing = {}
    for p, meta in parts.items():
        for dep in meta["deps"]:
            if dep not in parts:
                missing.setdefault(dep, f"required by {p}")
    fixes = plan_fixes(missing, parts, staged, modules_dir, args.fix)
    if args.json:
        print(json.dumps({"install": root, "installed_parts": len(parts),
                          "missing": missing, "fixes": fixes}, indent=2))
    else:
        print(f"install : {root}")
        print(f"installed module-parts: {len(parts)}")
        print_missing_and_fixes(missing, fixes)
    return 0 if not missing else 1


def plan_fixes(missing, parts, staged, modules_dir, do_fix):
    fixes = {}
    # a rough version hint from any installed Tridium part
    ver = next((m["version"] for m in parts.values()
                if m["vendor"] == "Tridium" and m["version"]), "")
    for part in sorted(missing):
        hit = locate_fix(part, staged, ver)
        entry = {"reason": missing[part]}
        if hit:
            src, src_ver = hit
            entry["staged"] = src
            entry["staged_version"] = src_ver
            entry["cp"] = f'cp "{src}" "{os.path.join(modules_dir, part + ".jar")}"'
            if ver and src_ver and src_ver != ver:
                entry["warn"] = f"staged version {src_ver} != install {ver}"
            if do_fix:
                dst, action = apply_fix(part, src, modules_dir)
                entry["action"] = action
                entry["dst"] = dst
                entry["sha256"] = sha256(dst)
        else:
            entry["staged"] = None
            entry["note"] = "not staged in sw/ — obtain from the source supervisor's modules/ or a matching install"
        fixes[part] = entry
    return fixes


def print_missing_and_fixes(missing, fixes):
    if not missing:
        print("\nOK — all required module-parts are installed. Nothing missing.")
        return
    print(f"\nMISSING module-parts: {len(missing)}")
    for part in sorted(missing):
        e = fixes.get(part, {})
        print(f"  ✗ {part}  ({e.get('reason','')})")
        if e.get("staged"):
            vtag = f" (v{e.get('staged_version','?')})"
            if e.get("action") == "copied":
                print(f"      → COPIED{vtag} from {e['staged']}")
                print(f"        sha256 {e['sha256']}")
            elif e.get("action") == "exists":
                print(f"      → already present at {e['dst']}")
            else:
                print(f"      staged in sw/{vtag}: {e['staged']}")
                print(f"      fix: {e['cp']}")
            if e.get("warn"):
                print(f"      ⚠ {e['warn']}")
        else:
            print(f"      {e.get('note','')}")
    print("\nAfter copying jars into modules/, RESTART Workbench, then reopen config.bog.")


def main():
    ap = argparse.ArgumentParser(prog="station-modules.py", add_help=True,
                                 description="Resolve a station's module deps against a Niagara install.")
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("check", help="station-driven dependency check")
    c.add_argument("target", help="config.bog | .dist backup | file.xml")
    c.add_argument("--install"); c.add_argument("--fix", action="store_true"); c.add_argument("--json", action="store_true")
    c.set_defaults(func=cmd_check)
    d = sub.add_parser("doctor", help="install-integrity check (unresolved deps of installed parts)")
    d.add_argument("--install"); d.add_argument("--fix", action="store_true"); d.add_argument("--json", action="store_true")
    d.set_defaults(func=cmd_doctor)
    args = ap.parse_args()
    sys.exit(args.func(args))


if __name__ == "__main__":
    main()
