#!/usr/bin/env python3
"""module-find: read-only finder for a Niagara N4 module's Java SOURCE tree.

WHY: bog-nav navigates a station's saved graph; this navigates the MODULE that defines the
types in it. Niagara slots/actions are declared with multi-line @NiagaraProperty /
@NiagaraAction annotations whose args wrap across lines, and a slot's writers are the
`.set("slot", ...)` / setSlot(...) calls scattered across a servlet or logic class. A naive
grep splits on the line break, misses the paren-balanced tail (the flags= / type= that decide
OPERATOR-vs-hidden), and cannot follow `extends` to say a servlet's name is inherited from
BWebServlet. This walks the tree, joins each annotation by PAREN BALANCE (bog-audit D9 lesson),
and answers those questions structurally.

Slot/action/extends/.set() extraction REUSED from build-n4-module-kit/toolbelt/bog-audit.sh
(kit main 3f666a0), source-dir scanning section: the paren-balanced @NiagaraProperty /
@NiagaraAction join, the `class X extends Y` capture, and the `.set("SLOT", ...)` writer scan.

Subcommands:
  slots [--type T] [--flags F] [--name RE]   @NiagaraProperty declarations (type/flags/min)
  actions [--name RE]                        @NiagaraAction declarations
  writers <slotName>                         who writes a slot: setSlot(/ .set("slot",
  extends [--of CLASS]                       class -> superclass map (follow a chain with --of)
  ords [--name RE]                           ORD-shaped string literals in the source
  grep <regex>                               regex over class names / slot names / action names

Read-only. Prints rows; --json for machine output. Prunes dot-dirs. stdlib only; py3 -> exit 3.
"""
import sys, os, re, json, argparse
from collections import defaultdict

_FLAG_MAP = {'HIDDEN': 'h', 'OPERATOR': 'o', 'READONLY': 'r', 'SUMMARY': 's', 'TRANSIENT': 't'}


class Module:
    """Parsed module source: slots, actions, extends, writers — keyed by class name."""
    def __init__(self, root):
        self.root = root
        self.src_slots = defaultdict(dict)    # class -> {slot: {type,min,flags}}
        self.src_actions = defaultdict(dict)  # class -> {action: flags_char}
        self.src_extends = {}                 # class -> superclass
        self.writers = defaultdict(set)       # slot -> {class:line writer strings}
        self.dyn_writers = defaultdict(set)   # class -> {".set(var, ...)" runtime writes}
        self.ords = []                        # (class, ord_string)
        self.files = 0
        self._scan()

    def _scan(self):
        for cur, dirs, files in os.walk(self.root):
            dirs[:] = sorted(d for d in dirs if not d.startswith('.'))  # prune dot-dirs (D9b)
            for fname in sorted(files):
                if not fname.endswith('.java'):
                    continue
                cls = fname[:-5]
                try:
                    content = open(os.path.join(cur, fname),
                                   encoding='utf-8', errors='replace').read()
                except Exception:
                    continue
                self.files += 1
                self._scan_file(cls, content)

    def _scan_file(self, cls, content):
        ext_m = re.search(r'\bclass\s+' + re.escape(cls) + r'\s+extends\s+(\w+)', content)
        if ext_m:
            self.src_extends[cls] = ext_m.group(1)

        # writers: .set("SLOT", ...) and setSlot(...) — record the writer's class
        for m in re.finditer(r'\.set\(\s*"([^"]+)"\s*,', content):
            self.writers[m.group(1)].add(f'{cls}: .set("{m.group(1)}", ...)')
        for m in re.finditer(r'\bset([A-Z]\w*)\s*\(', content):
            slot = m.group(1)[0].lower() + m.group(1)[1:]
            self.writers[slot].add(f'{cls}: set{m.group(1)}(...)')
        # dynamic write: obj.set(<identifier>, ...) — slot resolved at runtime (facade servlets)
        for m in re.finditer(r'(\w+)\.set\(\s*([A-Za-z_]\w*)\s*,', content):
            self.dyn_writers[cls].add(f'{cls}: {m.group(1)}.set({m.group(2)}, ...)  [slot resolved at runtime]')

        # ORD-shaped literals
        for m in re.finditer(r'"((?:slot|station|local|history|h|module|file):[^"]+)"', content):
            self.ords.append((cls, m.group(1)))

        # paren-balanced @NiagaraProperty / @NiagaraAction join (bog-audit D9)
        lines = content.split('\n')
        i = 0
        while i < len(lines):
            ln = lines[i]
            if '@NiagaraProperty' not in ln and '@NiagaraAction' not in ln:
                i += 1
                continue
            buf = ln
            depth = ln.count('(') - ln.count(')')
            j = i + 1
            while depth > 0 and j < len(lines):
                buf += ' ' + lines[j]
                depth += lines[j].count('(') - lines[j].count(')')
                j += 1
            i = j

            if '@NiagaraProperty' in buf:
                nm = re.search(r'name\s*=\s*"([^"]+)"', buf)
                if not nm:
                    continue
                tm = re.search(r'type\s*=\s*"([^"]+)"', buf)
                fm = re.search(r'flags\s*=\s*(Flags\.[A-Za-z_|]+|[A-Za-z_][A-Za-z0-9_.|]*)', buf)
                min_val = None
                mm = re.search(r'BFacets\.MIN[^,)]*,\s*BDouble\.make\(\s*(-?[0-9.]+)[dDfFlL]?\s*\)', buf)
                if mm:
                    try: min_val = float(mm.group(1))
                    except ValueError: pass
                self.src_slots[cls][nm.group(1)] = {
                    'type': tm.group(1) if tm else '',
                    'min': min_val,
                    'flags': fm.group(1) if fm else '',
                }
            elif '@NiagaraAction' in buf:
                nm = re.search(r'name\s*=\s*"([^"]+)"', buf)
                if not nm:
                    continue
                fm = re.search(r'flags\s*=\s*Flags\.(\w+)', buf)
                flag_char = 'o'
                if fm:
                    fw = fm.group(1).upper()
                    flag_char = _FLAG_MAP.get(fw, fw[0].lower() if fw else 'o')
                self.src_actions[cls][nm.group(1)] = flag_char

    def resolve_chain(self, cls):
        """Follow extends until unknown; return list [cls, super, super...]."""
        chain, seen = [cls], set()
        while cls in self.src_extends and cls not in seen:
            seen.add(cls)
            cls = self.src_extends[cls]
            chain.append(cls)
        return chain


def _flags_have(decl_flags, want):
    """True if the annotation flags= string names every flag in `want` (o/s/h/r/t or words)."""
    df = (decl_flags or '').upper()
    for w in want:
        long = {'o': 'OPERATOR', 's': 'SUMMARY', 'h': 'HIDDEN', 'r': 'READONLY', 't': 'TRANSIENT'}.get(w, w.upper())
        if long not in df:
            return False
    return True


# StatusNumeric/StatusBoolean/StatusEnum in either annotation form seen in the field:
# the module-name form "baja:StatusNumeric" or the Java-class form "BStatusNumeric".
_COMPLEX_TYPE = re.compile(r'(?:baja:|B)?Status\w+$')


def cmd_slots(mod, args):
    name_rx = re.compile(args.name, re.I) if args.name else None
    rows = []
    for cls in sorted(mod.src_slots):
        for slot, s in mod.src_slots[cls].items():
            if args.type and args.type not in (s['type'] or ''):
                continue
            if name_rx and not name_rx.search(slot):
                continue
            if args.flags and not _flags_have(s['flags'], list(args.flags)):
                continue
            complex_ = bool(_COMPLEX_TYPE.search(s['type'] or ''))
            rows.append({'class': cls, 'slot': slot, 'type': s['type'],
                         'flags': s['flags'], 'min': s['min'], 'complex': complex_})
    if args.json:
        print(json.dumps(rows, indent=2)); return
    for r in rows:
        cx = '  [COMPLEX]' if r['complex'] else ''
        mn = f"  min={r['min']}" if r['min'] is not None else ''
        print(f"{r['class']}.{r['slot']}  <{r['type']}>  flags={r['flags'] or '-'}{mn}{cx}")
    if not rows:
        print('(no matching @NiagaraProperty)')


def cmd_actions(mod, args):
    name_rx = re.compile(args.name, re.I) if args.name else None
    rows = []
    for cls in sorted(mod.src_actions):
        for a, f in mod.src_actions[cls].items():
            if name_rx and not name_rx.search(a):
                continue
            rows.append({'class': cls, 'action': a, 'flags': f})
    if args.json:
        print(json.dumps(rows, indent=2)); return
    for r in rows:
        print(f"{r['class']}.{r['action']}()  f={r['flags']}")
    if not rows:
        print('(no matching @NiagaraAction)')


def cmd_writers(mod, args):
    w = sorted(mod.writers.get(args.slot, []))
    dyn = sorted({x for s in mod.dyn_writers.values() for x in s})
    if args.json:
        print(json.dumps({'slot': args.slot, 'writers': w, 'dynamic': dyn}, indent=2)); return
    if w:
        for line in w:
            print(f"  {line}")
    else:
        print(f"{args.slot}: no STATIC writer (.set(\"{args.slot}\",...) or "
              f"set{args.slot[:1].upper()}{args.slot[1:]}(...))")
    if dyn:
        print("  dynamic writers (slot chosen at runtime — any slot could be the target):")
        for line in dyn:
            print(f"    {line}")


def cmd_extends(mod, args):
    if args.of:
        chain = mod.resolve_chain(args.of)
        if args.json:
            print(json.dumps({'class': args.of, 'chain': chain}, indent=2)); return
        print(' -> '.join(chain))
        return
    if args.json:
        print(json.dumps(mod.src_extends, indent=2)); return
    for cls in sorted(mod.src_extends):
        print(f"{cls} -> {mod.src_extends[cls]}")


def cmd_ords(mod, args):
    name_rx = re.compile(args.name, re.I) if args.name else None
    seen = set()
    rows = []
    for cls, ordv in mod.ords:
        if name_rx and not name_rx.search(ordv):
            continue
        key = (cls, ordv)
        if key in seen:
            continue
        seen.add(key)
        rows.append({'class': cls, 'ord': ordv})
    if args.json:
        print(json.dumps(rows, indent=2)); return
    for r in rows:
        print(f"{r['class']}  {r['ord']}")
    if not rows:
        print('(no ORD literals)')


def cmd_grep(mod, args):
    rx = re.compile(args.regex, re.I)
    rows = []
    for cls in mod.src_slots:
        for slot in mod.src_slots[cls]:
            if rx.search(slot):
                rows.append(('slot', cls, slot))
    for cls in mod.src_actions:
        for a in mod.src_actions[cls]:
            if rx.search(a):
                rows.append(('action', cls, a))
    for cls in set(list(mod.src_extends) + [v for v in mod.src_extends.values()]):
        if rx.search(cls):
            rows.append(('class', cls, mod.src_extends.get(cls, '')))
    if args.json:
        print(json.dumps([{'kind': k, 'class': c, 'name': n} for k, c, n in rows], indent=2)); return
    for k, c, n in rows:
        print(f"{k:<7} {c}  {n}")
    if not rows:
        print('(no matches)')


# in-memory selftest module: exercises multi-line annotation join, flags filter,
# extends chain, and the .set()/setX writer scan.
_SELFTEST_FILES = {
    'BColdRoom': '''
package com.x;
public class BColdRoom extends BComponent {
  @NiagaraProperty(
    name = "setpoint",
    type = "baja:StatusNumeric",
    flags = Flags.OPERATOR | Flags.SUMMARY,
    facets = @Facet("BFacets.make(BFacets.MIN, BDouble.make(-40.0))")
  )
  @NiagaraProperty(name = "hidden1", type = "baja:Double", flags = Flags.HIDDEN)
  @NiagaraAction(name = "applySetpoint", flags = Flags.OPERATOR)
  public void doApplySetpoint() { setSetpoint(BStatusNumeric.make(1.0)); }
  void f() { this.set("setpoint", x); }
  final String ORD = "slot:/Programacion/ColdRoom_1";
}
''',
    'BDashboardServlet': '''
package com.x;
public class BDashboardServlet extends BWebServlet {
  public String getServletName() { return "dashboardpan"; }
  void handleSetpointWrite() { parent.set(prop, toSet, null); }
}
''',
}


def cmd_selftest(mod_ignored, args):
    import tempfile
    fails = []

    def check(cond, label):
        print(('  ok   ' if cond else '  FAIL ') + label)
        if not cond:
            fails.append(label)

    with tempfile.TemporaryDirectory() as d:
        sub = os.path.join(d, '.git'); os.makedirs(sub)  # dot-dir must be pruned
        open(os.path.join(sub, 'BNoise.java'), 'w').write('class BNoise extends BFake {}')
        for name, body in _SELFTEST_FILES.items():
            open(os.path.join(d, name + '.java'), 'w').write(body)
        m = Module(d)

        check('BNoise' not in m.src_extends, 'dot-dir (.git) pruned')
        check(m.src_slots['BColdRoom']['setpoint']['type'] == 'baja:StatusNumeric',
              'multi-line @NiagaraProperty joined by paren balance')
        check('OPERATOR' in m.src_slots['BColdRoom']['setpoint']['flags'],
              'flags captured across the line break')
        check(m.src_slots['BColdRoom']['setpoint']['min'] == -40.0, 'MIN facet parsed')
        check(_COMPLEX_TYPE.search(m.src_slots['BColdRoom']['setpoint']['type']) is not None,
              'StatusNumeric detected as complex')
        check(m.src_actions['BColdRoom'].get('applySetpoint') == 'o', '@NiagaraAction OPERATOR flag')
        w = m.writers.get('setpoint', set())
        check(any('.set("setpoint"' in x for x in w) and any('setSetpoint' in x for x in w),
              'both .set("setpoint",..) and setSetpoint(..) writers found')
        check(m.resolve_chain('BDashboardServlet') == ['BDashboardServlet', 'BWebServlet'],
              'extends chain resolved (servlet name inherited from BWebServlet)')
        check(any('parent.set(prop' in x for x in m.dyn_writers.get('BDashboardServlet', set())),
              'dynamic .set(prop,..) writer detected (runtime-resolved slot)')
        check(('BColdRoom', 'slot:/Programacion/ColdRoom_1') in m.ords, 'ORD literal captured')

    if fails:
        print(f'\nSELFTEST FAILED: {len(fails)} check(s)'); sys.exit(1)
    print('\nSELFTEST OK')


def build_parser():
    p = argparse.ArgumentParser(prog='module-find.py', description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('root', nargs='?', help='module src root (omit for selftest)')
    p.add_argument('--json', action='store_true')
    sub = p.add_subparsers(dest='cmd', required=True)

    s = sub.add_parser('slots', help='@NiagaraProperty declarations')
    s.add_argument('--type', help='substring of the slot type')
    s.add_argument('--flags', help='require these flags (o/s/h/r/t or OPERATOR..)')
    s.add_argument('--name', help='regex on slot name')
    s.set_defaults(func=cmd_slots)

    a = sub.add_parser('actions', help='@NiagaraAction declarations')
    a.add_argument('--name', help='regex on action name')
    a.set_defaults(func=cmd_actions)

    w = sub.add_parser('writers', help='who writes a slot')
    w.add_argument('slot')
    w.set_defaults(func=cmd_writers)

    e = sub.add_parser('extends', help='class -> superclass map')
    e.add_argument('--of', help='resolve the full chain of one class')
    e.set_defaults(func=cmd_extends)

    o = sub.add_parser('ords', help='ORD-shaped string literals')
    o.add_argument('--name', help='regex on the ORD')
    o.set_defaults(func=cmd_ords)

    g = sub.add_parser('grep', help='regex over class/slot/action names')
    g.add_argument('regex')
    g.set_defaults(func=cmd_grep)

    st = sub.add_parser('selftest', help='in-memory assertions (no tree needed)')
    st.set_defaults(func=cmd_selftest)
    return p


def main(argv=None):
    args = build_parser().parse_args(argv)
    if args.cmd == 'selftest':
        args.func(None, args); return
    if not args.root:
        sys.stderr.write('module-find: a src root is required for this command\n'); sys.exit(2)
    if not os.path.isdir(args.root):
        sys.stderr.write(f'module-find: not a directory: {args.root}\n'); sys.exit(3)
    args.func(Module(args.root), args)


if __name__ == '__main__':
    if sys.version_info[0] < 3:
        sys.stderr.write('module-find: requires python3\n'); sys.exit(3)
    main()
