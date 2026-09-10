#!/usr/bin/env python3
"""greenmax-matrix: build the lighting points matrix for a GreenMAX/Niagara site.

Reads the SUPERVISOR config.bog + the JACE config.bog (both via the bog-nav engine)
and the Supervisor `shared/px` folder, and emits ONE row per lighting circuit with the
full command chain traced end to end:

  [Supervisor] Schedule{k}.out -> SchdlGM{nn} R{k} (BooleanWritable).in10
      -> [JACE] NiagaraNetwork/HM_BMS/points/GreenMAX{n}/SchdlGM{nn} R{k} (BooleanPoint).out
      -> [JACE] BcpBacnetNetwork/GM{nn}ilum/points/Relay[{k}].BO.in16  -> physical relay

Columns: GM panel, electrical tablero (AL, from the Px filename), panel zone (from the Px),
relay index, circuit label (from the Px if present), the schedule + command-writable names,
whether each link in the chain is present, the BACnet device + object id, and a blank
DescripcionCircuito column for the client's cuadro-de-cargas description.

READ-ONLY. Reuses tools/bog-nav.py (the Bog graph parser). Stdlib only.

Usage:
  python3 tools/greenmax-matrix.py <supervisor.bog> <jace.bog> <px_dir> [-o out.csv]
"""
import sys, os, re, csv, glob, argparse, importlib.util

HERE = os.path.dirname(os.path.abspath(__file__))

def _load_bognav():
    spec = importlib.util.spec_from_file_location("bognav", os.path.join(HERE, "bog-nav.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

BN = _load_bognav()

# ---- name decoders / matchers ------------------------------------------------
RELAY_RE = re.compile(r'^Relay\[(\d+)\]\.BO$')
# schedules: old pattern "Schedule{k}" (GM01) OR new pattern bare "R{k}" (GM02+)
SCHED_RE = re.compile(r'^(?:Schedule|R)(\d+)$')
# command writable: old "SchdlGM{nn} R{k}" (GM01) OR new "HorR{k}" (GM02+)
WRITABLE_RE = re.compile(r'^(?:SchdlGM0*\d+\s+R|HorR)(\d+)$')
# per-circuit description / tablero, stored as kitControl:StringConst (GM02+)
LABEL_RE = re.compile(r'^labelR(\d+)$')
TAB_RE = re.compile(r'^tabR(\d+)$')
GMFOLDER_RE = re.compile(r'^GM0*(\d+)ilum$')          # supervisor proxy folder / JACE bacnet device
SCHDL_RE = re.compile(r'^SchdlGM0*(\d+)\s+R(\d+)$')    # JACE readback point (feeds BACnet BO.in16)

# shared nav-bar headers present in every panel Px (NOT a zone)
NAV_HEADERS = {"periferico", "calle 60", "prolongacion montejo", "vialidad montejo",
               "horario", "iluminacion", "iluminación"}

def _un(s):  # decode bog slot-name escapes ($20 space, $5b [ , $5d ] , $2e .)
    return (s.replace('$20', ' ').replace('$5b', '[').replace('$5d', ']')
             .replace('$2e', '.').replace('$2d', '-'))

# ---- Px mining ---------------------------------------------------------------
def parse_px(px_dir):
    """-> (gm -> {'tableros':set, 'zone':str, 'labels':{relay_k:label}})"""
    out = {}
    for pxf in sorted(glob.glob(os.path.join(px_dir, "GreenMAX*.px"))):
        base = os.path.basename(pxf)
        if re.search(r'Vent|Iluminacion', base):
            continue
        m = re.match(r'GreenMAX0*(\d+)\s+Tab(AL\d+)', base)
        if not m:
            continue
        gm = int(m.group(1)); tab = m.group(2)
        e = out.setdefault(gm, {'tableros': set(), 'zone': '', 'labels': {}})
        e['tableros'].add(tab)
        txt = open(pxf, encoding='utf-8', errors='replace').read()
        # relay -> circuit label, paired by document order
        toks = re.findall(r'ord="slot:Relay\$5b(\d+)\$5d\$2eBO"|text="(AL\d+-\d+)"', txt)
        cur = None
        for relay, label in toks:
            if relay:
                cur = int(relay)
            elif label and cur is not None:
                e['labels'].setdefault(cur, label)
        # panel zone = a distinctive text label that is not the shared nav bar / status / number
        for t in re.findall(r'text="([^"]{2,30})"', txt):
            import html
            tt = html.unescape(t).strip()
            low = tt.lower()
            if (low not in NAV_HEADERS and not re.fullmatch(r'\d+', tt)
                    and not re.match(r'(?i)(auto|fault|down|overridden|normal|error|manual|green)', tt)
                    and 'comunicaci' not in low and 'AL' != tt[:2]):
                if not e['zone']:
                    e['zone'] = tt
                elif tt not in e['zone'] and len(e['zone']) < 40:
                    e['zone'] += " / " + tt
    return out

# ---- bog mining --------------------------------------------------------------
def _strconst_value(cc):
    """kitControl:StringConst -> its out.value (html-unescaped), or ''."""
    import html
    slot = cc.slots.get('out') or {}
    v = slot.get('value')
    if v is None:
        v = (slot.get('child') or {}).get('value')
    return html.unescape(v).strip() if v else ''

def sup_panels(bog):
    """Supervisor: gm -> {relays,scheds,writables:{k},labels:{k},tabs:{k},sched_linked:{k}}"""
    panels = {}
    for h, c in bog.handle_map.items():
        g = GMFOLDER_RE.match(c.name)
        if g and c.simple_type() == 'NiagaraPointFolder':
            gm = int(g.group(1))
            e = panels.setdefault(gm, {'relays': set(), 'scheds': set(), 'writables': {},
                                       'labels': {}, 'tabs': {}, 'sched_linked': {}})
            for ch in c.children:
                cc = bog.handle_map.get(ch)
                if not cc:
                    continue
                nm = _un(cc.name)
                mr = RELAY_RE.match(nm); ms = SCHED_RE.match(nm); mw = WRITABLE_RE.match(nm)
                ml = LABEL_RE.match(nm); mt = TAB_RE.match(nm)
                if mr:
                    e['relays'].add(int(mr.group(1)))
                elif mw:                               # writable check before schedule (HorR vs R)
                    e['writables'][int(mw.group(1))] = cc
                elif ms:
                    e['scheds'].add(int(ms.group(1)))
                elif ml:
                    e['labels'][int(ml.group(1))] = _strconst_value(cc)
                elif mt:
                    e['tabs'][int(mt.group(1))] = _strconst_value(cc)
    # confirm schedule.out -> writable.in10 links (both naming patterns)
    wh = {c.handle: (gm, k) for gm, e in panels.items() for k, c in e['writables'].items()}
    for lk in bog.link_list:
        if lk.get('tgt_slot') == 'in10' and lk.get('container_h') in wh:
            gm, k = wh[lk['container_h']]
            src = bog.handle_map.get(lk.get('src_h'))
            if src and (SCHED_RE.match(_un(src.name)) or 'Schedule' in src.name):
                panels[gm]['sched_linked'][k] = True
    return panels

def jace_panels(bog):
    """JACE: gm -> {bo:{k:{objid,dev}}, feed_ok:{k:bool}}"""
    panels = {}
    dev_of = {}  # bacnet device handle -> gm
    for h, c in bog.handle_map.items():
        g = GMFOLDER_RE.match(c.name)
        if g and c.simple_type() == 'BacnetDevice':
            gm = int(g.group(1)); dev_of[h] = gm
            panels.setdefault(gm, {'bo': {}, 'feed_ok': {}, 'device': c.name})
    # relay BOs under each device + objectId slot if present
    for h, c in bog.handle_map.items():
        mr = RELAY_RE.match(_un(c.name))
        if not mr:
            continue
        # climb to owning bacnet device
        p = c
        dev_gm = None
        seen = 0
        while p is not None and seen < 8:
            g = GMFOLDER_RE.match(p.name)
            if g and p.simple_type() == 'BacnetDevice':
                dev_gm = int(g.group(1)); break
            p = bog.handle_map.get(p.parent_h); seen += 1
        if dev_gm is None:
            continue
        k = int(mr.group(1))
        objid = ''
        for sn, sv in c.slots.items():
            if 'objectId' in sn or sn == 'objectId':
                objid = sv.get('value') or ''
        panels.setdefault(dev_gm, {'bo': {}, 'feed_ok': {}, 'device': ''})
        panels[dev_gm]['bo'][k] = {'objid': objid}
    # confirm each Relay[k].BO is DRIVEN (any incoming link at in10/in16/in9 — the write
    # priority is NOT uniform: GM01 uses in16, the newer panels use in10). Attribute the BO
    # to its owning GM bacnet device by climbing parents; also record the write priority seen.
    DRIVE_SLOTS = {'in10', 'in16', 'in9'}
    bo_to_gm = {}
    for h, c in bog.handle_map.items():
        mr = RELAY_RE.match(_un(c.name))
        if not mr:
            continue
        p = c; dev_gm = None; seen = 0
        while p is not None and seen < 8:
            g = GMFOLDER_RE.match(p.name)
            if g and p.simple_type() == 'BacnetDevice':
                dev_gm = int(g.group(1)); break
            p = bog.handle_map.get(p.parent_h); seen += 1
        if dev_gm is not None:
            bo_to_gm[c.handle] = (dev_gm, int(mr.group(1)))
    for lk in bog.link_list:
        if lk.get('tgt_slot') in DRIVE_SLOTS and lk.get('container_h') in bo_to_gm:
            gm, k = bo_to_gm[lk['container_h']]
            e = panels.setdefault(gm, {'bo': {}, 'feed_ok': {}, 'device': ''})
            e['feed_ok'][k] = True
            e.setdefault('feed_prio', {})[k] = lk.get('tgt_slot')
    return panels

# ---- main --------------------------------------------------------------------
def main(argv=None):
    ap = argparse.ArgumentParser(description="Build the GreenMAX lighting points matrix.")
    ap.add_argument('supervisor'); ap.add_argument('jace'); ap.add_argument('px_dir')
    ap.add_argument('-o', '--out', default='matriz-puntos-DRAFT.csv')
    a = ap.parse_args(argv)

    sup = sup_panels(BN.Bog(a.supervisor))
    jac = jace_panels(BN.Bog(a.jace))
    px = parse_px(a.px_dir)

    fields = ["GM", "Tablero", "Zona", "Relay", "DescripcionCircuito",
              "CircuitLabel_Px", "Schedule", "CmdWritable", "SchedLink_in10", "JACE_driven",
              "JACE_write_prio", "BACnet_device", "BACnet_objectId"]
    rows = []
    for gm in sorted(set(sup) | set(jac) | set(px)):
        s = sup.get(gm, {}); j = jac.get(gm, {}); p = px.get(gm, {})
        relays = sorted(set(s.get('relays', set())) | set(j.get('bo', {}).keys()))
        px_tab = "/".join(sorted(p.get('tableros', []))) or ''
        for k in relays:
            # description: prefer the in-station labelR{k} StringConst (GM02+), else the Px
            # circuit label (GM01 old pattern, e.g. "AL3-1"); tablero: prefer tabR{k} else Px filename
            desc = s.get('labels', {}).get(k, '') or p.get('labels', {}).get(k, '')
            tab = s.get('tabs', {}).get(k, '') or px_tab or "(no Px)"
            rows.append({
                "GM": f"GM{gm:02d}",
                "Tablero": tab,
                "Zona": p.get('zone', ''),
                "Relay": k,
                "DescripcionCircuito": desc,
                "CircuitLabel_Px": p.get('labels', {}).get(k, ''),
                "Schedule": f"R{k}/Schedule{k}" if k in s.get('scheds', set()) else '',
                "CmdWritable": _un(s['writables'][k].name) if k in s.get('writables', {}) else '',
                "SchedLink_in10": "yes" if s.get('sched_linked', {}).get(k) else "?",
                "JACE_driven": "yes" if j.get('feed_ok', {}).get(k) else "?",
                "JACE_write_prio": j.get('feed_prio', {}).get(k, ''),
                "BACnet_device": j.get('device', '') or (f"GM{gm:02d}ilum" if gm in jac else ''),
                "BACnet_objectId": j.get('bo', {}).get(k, {}).get('objid', ''),
            })
    with open(a.out, 'w', newline='', encoding='utf-8') as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader(); w.writerows(rows)

    # summary
    tot_desc = sum(1 for r in rows if r['DescripcionCircuito'])
    print(f"panels={len(set(sup)|set(jac))}  circuits={len(rows)}  with-description={tot_desc}  -> {a.out}")
    print(f"{'GM':<5}{'Tablero':<12}{'Zona':<18}{'relays':>7}{'desc':>6}{'schedOK':>8}{'feedOK':>7}")
    for gm in sorted(set(sup) | set(jac)):
        gr = [r for r in rows if r['GM'] == f"GM{gm:02d}"]
        so = sum(1 for r in gr if r['SchedLink_in10'] == 'yes')
        fo = sum(1 for r in gr if r['JACE_driven'] == 'yes')
        dc = sum(1 for r in gr if r['DescripcionCircuito'])
        z = (px.get(gm, {}).get('zone', '') or '')[:16]
        t = (gr[0]['Tablero'] if gr else '')[:11]
        print(f"GM{gm:02d}  {t:<12}{z:<18}{len(gr):>7}{dc:>6}{so:>8}{fo:>7}")
    return 0

if __name__ == '__main__':
    sys.exit(main())
