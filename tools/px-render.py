#!/usr/bin/env python3
"""px-render.py — render a Niagara N4 Px (.px) as a self-contained HTML page.

A Px is Niagara Presentation XML: a CanvasPane (viewSize="W,H") holding widgets
positioned absolutely by layout="x,y,w,h". This tool walks that tree and emits an
HTML page that reproduces the screen faithfully in a browser — floor plan, buttons,
LEDs, labels and logos in the same positions — embedding every referenced image as a
data: URI so the output is a single portable file (good for an Artifact / client demo).

SOLO LECTURA. Stdlib only. Reusable for any Px of any N4 station.

Image ords understood: file:^<rel>  resolves against the station's shared/ root
(file:^Imagenes/... -> shared/Imagenes/... ; file:^px/... -> shared/px/...).
The shared/ root is auto-detected (a dir containing Imagenes/ at/above the .px), or
pass it with --shared.

Widget coverage: Label (text+font), Picture (static image OR ValueBinding via
IBooleanToSimple=On/Off image, IStatusToSimple=status LED), ImageButton (text button
or invisible control zone), BackButton. Bindings have no live station, so per-relay
ON/OFF is DEMO state derived deterministically from the relay ord (--on-ratio, --seed)
— clearly a mock, never presented as live.

Usage:
  px-render.py <file.px> [--shared DIR] [--out HTML] [--on-ratio 0.7] [--seed TAG]
               [--title TITLE] [--subtitle TEXT]
  px-render.py selftest

Examples:
  px-render.py "clients/distech-merida-harbor/.../shared/px/GreenMAX07 TabAL7.px" \
               --out clients/distech-merida-harbor/deliverables/px-gm07.html
"""
import argparse, base64, hashlib, html, os, re, sys
import xml.etree.ElementTree as ET

# ---------- shared-root resolution ----------

def find_shared(px_path, override=None):
    """Locate the station 'shared/' root that image ords resolve against.
    A valid root contains an 'Imagenes' dir (and usually a 'px' dir)."""
    if override:
        return override
    d = os.path.dirname(os.path.abspath(px_path))
    for _ in range(8):
        if os.path.isdir(os.path.join(d, "Imagenes")):
            return d
        parent = os.path.dirname(d)
        if parent == d:
            break
        d = parent
    # common case: <shared>/px/<file>.px  -> shared is one level up from px dir
    guess = os.path.dirname(os.path.dirname(os.path.abspath(px_path)))
    return guess


_MIME = {"svg": "image/svg+xml", "jpg": "image/jpeg", "jpeg": "image/jpeg",
         "gif": "image/gif", "png": "image/png"}


class Assets:
    def __init__(self, shared, organized=None):
        self.shared = shared
        # organized/ corpus root for module:// resources (repo default if not given)
        self.organized = organized or os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "organized")
        self.cache = {}
        self.missing = set()

    def _module_candidates(self, mod, rel):
        # 1) the station's own bundled copy under shared/px/<mod>/<rel>
        yield os.path.join(self.shared, "px", mod, rel)
        # 2) the extracted corpus: organized/<mod>/<mod>-{wb,rt,ux}/extracted/<rel>
        for kind in ("wb", "rt", "ux"):
            yield os.path.join(self.organized, mod, "%s-%s" % (mod, kind), "extracted", rel)
        # 3) any artifact of that module in the corpus
        import glob as _glob
        for p in _glob.glob(os.path.join(self.organized, mod, "*", "extracted", rel)):
            yield p

    def resolve(self, ref):
        if not ref:
            return None
        if ref.startswith("file:^"):
            p = os.path.join(self.shared, ref[len("file:^"):])
            return p if os.path.exists(p) else None
        if ref.startswith("module://"):
            path = ref[len("module://"):]
            seg = path.split("/", 1)
            if len(seg) != 2:
                return None
            mod, rel = seg
            for p in self._module_candidates(mod, rel):
                if os.path.exists(p):
                    return p
            return None
        return None

    def datauri(self, ref):
        p = self.resolve(ref)
        if not p:
            if ref:
                self.missing.add(ref)
            return None
        if p in self.cache:
            return self.cache[p]
        ext = p.rsplit(".", 1)[-1].lower()
        with open(p, "rb") as f:
            b = f.read()
        uri = "data:%s;base64,%s" % (_MIME.get(ext, "image/png"), base64.b64encode(b).decode())
        self.cache[p] = uri
        return uri

# ---------- parsing helpers ----------

def parse_layout(s):
    if not s:
        return None
    try:
        x, y, w, h = (float(v) for v in s.split(","))
        return x, y, w, h
    except ValueError:
        return None

def parse_font(s):
    """'bold 26.0pt Arial' -> ('bold','26','Arial'); '11.0pt Arial' -> ('normal','11','Arial')."""
    if not s:
        return ("normal", "13", "Arial")
    weight = "normal"
    toks = s.split()
    if toks and toks[0] in ("bold", "italic"):
        weight = toks[0]
        toks = toks[1:]
    size, fam = "13", "Arial"
    for tk in toks:
        if "pt" in tk:
            size = tk.replace("pt", "").split(".")[0]
        else:
            fam = tk
    return (weight, size, fam)

def relay_num(ord_):
    """slot:Relay$5b7$5d$2eBO -> 7 (handles $5b/$5d escaping for [ ])."""
    if not ord_:
        return None
    d = ord_.replace("$5b", "[").replace("$5d", "]")
    m = re.search(r"\[(\d+)\]", d)
    return int(m.group(1)) if m else None

# ---------- render ----------

def render(px_path, shared=None, on_ratio=0.7, seed=None, title=None, subtitle=None, organized=None):
    shared = find_shared(px_path, shared)
    A = Assets(shared, organized)
    tag_seed = seed or os.path.splitext(os.path.basename(px_path))[0]

    def relay_on(k):
        if k is None:
            return True
        h = int(hashlib.md5(("%s:r%d" % (tag_seed, k)).encode()).hexdigest(), 16)
        return (h % 1000) / 1000.0 < on_ratio

    root = ET.parse(px_path).getroot()
    canvas = next((e for e in root.iter("CanvasPane")), None)
    if canvas is None:
        raise SystemExit("no CanvasPane found in %s" % px_path)
    vw, vh = canvas.get("viewSize", "1370.0,780.0").split(",")
    CW, CH = int(float(vw)), int(float(vh))

    parts = []
    for e in canvas:
        lay = parse_layout(e.get("layout"))
        if not lay:
            continue
        x, y, w, h = lay
        base = "position:absolute;left:%gpx;top:%gpx;width:%gpx;height:%gpx;" % (x, y, w, h)
        t = e.tag
        if t == "Label":
            wt, sz, fam = parse_font(e.get("font"))
            txt = html.unescape(e.get("text") or "")
            parts.append(
                '<div style="%sdisplay:flex;align-items:center;font-family:%s,Arial,sans-serif;'
                'font-weight:%s;font-size:%spx;color:#16233a;line-height:1.05;overflow:hidden;'
                'white-space:pre-wrap;">%s</div>' % (base, fam, wt, sz, html.escape(txt)))
        elif t == "Picture":
            img = e.get("image")
            vb = e.find("ValueBinding")
            if img:
                uri = A.datauri(img)
                if uri:
                    parts.append('<img src="%s" style="%sobject-fit:contain;" alt="">' % (uri, base))
            elif vb is not None:
                k = relay_num(vb.get("ord"))
                on = relay_on(k)
                kinds = [c.tag for c in vb]
                if "IBooleanToSimple" in kinds:
                    ib = vb.find("IBooleanToSimple")
                    tv = ib.find("Image[@name='trueValue']")
                    fv = ib.find("Image[@name='falseValue']")
                    ref = (tv.get("value") if on else fv.get("value")) if (tv is not None and fv is not None) else None
                    uri = A.datauri(ref) if ref else None
                    if uri:
                        parts.append('<img src="%s" style="%sobject-fit:contain;" alt="">' % (uri, base))
                elif "IStatusToSimple" in kinds:
                    st = vb.find("IStatusToSimple")
                    okref = st.find("Image[@name='ok']") if st is not None else None
                    uri = A.datauri(okref.get("value")) if okref is not None else None
                    if on and uri:
                        parts.append('<img src="%s" style="%sobject-fit:contain;" alt="">' % (uri, base))
                    else:
                        parts.append('<div style="%sdisplay:grid;place-items:center;"><span style="width:60%%;'
                                     'height:60%%;max-width:16px;max-height:16px;border-radius:50%%;'
                                     'background:#9aa4ac;"></span></div>' % base)
        elif t == "BoundLabel":
            # BoundLabel = dynamic label: either a bound image toggle (ValueBinding +
            # IBooleanToSimple/IStatusToSimple) or bound text (BoundLabelBinding whose
            # value lives on the live station). No live station -> image uses demo state;
            # bound text falls back to the slot's leaf name.
            vb = e.find("ValueBinding")
            blb = e.find("BoundLabelBinding")
            if vb is not None:
                k = relay_num(vb.get("ord"))
                on = relay_on(k)
                kinds = [c.tag for c in vb]
                if "IBooleanToSimple" in kinds:
                    ib = vb.find("IBooleanToSimple")
                    tv = ib.find("Image[@name='trueValue']")
                    fv = ib.find("Image[@name='falseValue']")
                    ref = (tv.get("value") if on else fv.get("value")) if (tv is not None and fv is not None) else None
                    uri = A.datauri(ref) if ref else None
                    if uri:
                        parts.append('<img src="%s" style="%sobject-fit:contain;" alt="">' % (uri, base))
                elif "IStatusToSimple" in kinds:
                    st = vb.find("IStatusToSimple")
                    okref = st.find("Image[@name='ok']") if st is not None else None
                    uri = A.datauri(okref.get("value")) if okref is not None else None
                    if on and uri:
                        parts.append('<img src="%s" style="%sobject-fit:contain;" alt="">' % (uri, base))
                    else:
                        parts.append('<div style="%sdisplay:grid;place-items:center;"><span style="width:60%%;'
                                     'height:60%%;max-width:16px;max-height:16px;border-radius:50%%;'
                                     'background:#9aa4ac;"></span></div>' % base)
            else:
                leaf = ""
                if blb is not None:
                    ordv = blb.get("ord", "")
                    leaf = ordv.rstrip("/").split("/")[-1].split(":")[-1]
                parts.append('<div style="%sdisplay:flex;align-items:center;justify-content:center;'
                             'font-family:Arial,sans-serif;font-size:12px;color:#16233a;background:#eef1f5;'
                             'border:1px solid #cdd4de;border-radius:4px;text-align:center;padding:2px;">%s</div>'
                             % (base, html.escape(leaf)))
        elif t == "ImageButton":
            txt = (e.get("text") or "").strip()
            if txt:
                parts.append(
                    '<div style="%sdisplay:flex;align-items:center;justify-content:center;'
                    'font-family:Arial,sans-serif;font-size:11px;font-weight:600;color:#00123F;'
                    'background:#eef1f5;border:1px solid #cdd4de;border-radius:4px;">%s</div>'
                    % (base, html.escape(txt)))
            else:
                parts.append('<div style="%s" title="control"></div>' % base)
        elif t == "BackButton":
            parts.append('<div style="%sdisplay:grid;place-items:center;color:#00123F;font-size:20px;">&lsaquo;</div>' % base)

    name = os.path.splitext(os.path.basename(px_path))[0]
    ttl = title or ("Px · " + name)
    sub = subtitle or ("Pantalla Px real reproducida desde el config.bog: <b>%s</b>. "
                       "Plano, botones On/Off, LEDs de estado y etiquetas en las mismas posiciones."
                       % html.escape(name))
    body = "\n".join(parts)
    out = (
        '<title>%s</title>\n'
        '<style>\n'
        ':root{color-scheme:light}\n'
        'body{margin:0;background:#0b1220;font-family:Arial,Helvetica,sans-serif;}\n'
        '.bar{background:#001D68;color:#fff;padding:10px 18px;font-size:13px;display:flex;gap:12px;align-items:center;}\n'
        '.bar b{font-weight:700}.bar .tag{margin-left:auto;font-size:11px;background:#76B900;color:#0c1207;font-weight:700;border-radius:4px;padding:3px 8px;}\n'
        '.note{background:#e9edf3;color:#3a4453;font-size:12px;padding:8px 18px;border-bottom:1px solid #d2d8e2;}\n'
        '.stage{width:100%%;overflow:auto;background:#f4f2ec;}\n'
        '.canvas{position:relative;width:%dpx;height:%dpx;transform-origin:top left;background:#fff;}\n'
        '.canvas img{-webkit-user-drag:none;user-select:none;}\n'
        '</style>\n'
        '<div class="bar"><b>Px actual</b> (Niagara Workbench Px Editor) <span class="tag">RÉPLICA</span></div>\n'
        '<div class="note">%s</div>\n'
        '<div class="stage"><div class="canvas" id="cv">\n%s\n</div></div>\n'
        '<script>function fit(){var cv=document.getElementById("cv");var w=cv.parentElement.clientWidth;'
        'var s=Math.min(1,w/%d);cv.style.transform="scale("+s+")";cv.parentElement.style.height=(%d*s)+"px";}\n'
        'window.addEventListener("resize",fit);fit();</script>\n'
        % (html.escape(ttl), CW, CH, sub, body, CW, CH)
    )
    return out, {"canvas": (CW, CH), "widgets": len(parts), "shared": shared,
                 "missing": sorted(A.missing), "assets": len(A.cache)}


def selftest():
    import tempfile
    px = ('<?xml version="1.0" encoding="UTF-8"?>\n<px version="1.0"><import/>'
          '<content><ScrollPane><CanvasPane name="content" viewSize="400.0,300.0">'
          '<Label layout="10.0,10.0,200.0,30.0" text="Cuarto 1" font="bold 18.0pt Arial"/>'
          '<ImageButton layout="10.0,50.0,60.0,20.0" text="Auto"/>'
          '<Picture layout="10.0,80.0,40.0,40.0">'
          '<ValueBinding ord="slot:Relay$5b1$5d$2eBO"><IBooleanToSimple name="image">'
          '<Image name="trueValue" value="file:^Imagenes/On.png"/>'
          '<Image name="falseValue" value="file:^Imagenes/Off.png"/></IBooleanToSimple></ValueBinding>'
          '</Picture></CanvasPane></ScrollPane></content></px>')
    d = tempfile.mkdtemp()
    os.makedirs(os.path.join(d, "Imagenes"))
    os.makedirs(os.path.join(d, "px"), exist_ok=True)
    png = base64.b64decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
    for nm in ("On.png", "Off.png"):
        with open(os.path.join(d, "Imagenes", nm), "wb") as f:
            f.write(png)
    fp = os.path.join(d, "px", "test.px")
    open(fp, "w").write(px)
    out, meta = render(fp)
    assert meta["canvas"] == (400, 300), meta
    assert meta["widgets"] == 3, meta                     # Label + ImageButton + bound Picture
    assert "Cuarto 1" in out and "font-size:18px" in out
    assert "Auto" in out
    assert not meta["missing"], meta                      # both images resolved
    assert "data:image/png;base64" in out                # boolean On image embedded
    # a bound Picture whose image is missing emits nothing but never crashes:
    out2, meta2 = render(fp, shared=tempfile.mkdtemp())
    assert meta2["widgets"] == 2 and meta2["missing"], meta2
    print("selftest OK — canvas 400x300, 3 widgets embedded; missing-asset path safe")


def main():
    ap = argparse.ArgumentParser(description="Render a Niagara Px as a self-contained HTML page.")
    ap.add_argument("px", help="path to .px file (or 'selftest')")
    ap.add_argument("--shared", help="station shared/ root (auto-detected if omitted)")
    ap.add_argument("--organized", help="organized/ corpus root for module:// resources (repo default)")
    ap.add_argument("--out", help="output .html path (default: stdout)")
    ap.add_argument("--on-ratio", type=float, default=0.7, help="demo fraction of relays ON (0..1)")
    ap.add_argument("--seed", help="demo-state seed tag (default: px basename)")
    ap.add_argument("--title", help="page <title>")
    ap.add_argument("--subtitle", help="note line under the header")
    args = ap.parse_args()

    if args.px == "selftest":
        selftest()
        return

    out, meta = render(args.px, shared=args.shared, on_ratio=args.on_ratio,
                       seed=args.seed, title=args.title, subtitle=args.subtitle,
                       organized=args.organized)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(out)
        mb = len(out.encode()) / 1048576
        print("wrote %s  (%d widgets, %d assets, %.2f MB, canvas %dx%d)"
              % (args.out, meta["widgets"], meta["assets"], mb, *meta["canvas"]))
        print("shared root:", meta["shared"])
        if meta["missing"]:
            print("missing assets (%d):" % len(meta["missing"]))
            for m in meta["missing"]:
                print("  ", m)
    else:
        sys.stdout.write(out)


if __name__ == "__main__":
    main()
