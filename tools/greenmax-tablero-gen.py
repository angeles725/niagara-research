#!/usr/bin/env python3
"""greenmax-tablero-gen.py — HARBOR GreenMAX tablero Px transform (reusable).

Takes an ORIGINAL GreenMAX tablero .px (from the station backup) and produces the
REDESIGNED tablero (name-selector) as a Markdown file with the Px XML in a fenced
block, ready to paste into the PxView inside the panel's `GM<X>ilum` device folder.

Transform (see clients/distech-merida-harbor/docs/retro-2026-09-10-greenmax-replication.md):
  - REMOVE the per-circuit HOA buttons (ImageButton -> Relay[k].BO/active|inactive|auto).
  - REMOVE the calendar-popup Pictures (PopupBinding slot:R{k}).
  - KEEP the pill (Relay[k].BO value, IBooleanToSimple) and the LED
    (Relay[k].BO status, IStatusToSimple) exactly as the original.
  - APPEND a selector per circuit, positioned at each pill's (x, y+30):
        Picture BotonNormal + Label(NameMux{k} %out.value%) + ImageButton(SelR{k}/set)
    Selector ords are ABSOLUTE: station:|slot:/Iluminacion/<gm>/{NameMux,SelR}{k}
    (the control points live in Config/Iluminacion/<gm>/ — plain number, no leading
    zero, even when the device folder is GM0Xilum). Pills/LEDs stay RELATIVE slot:.
  - Optionally (--legend) add a read-only "Horarios" legend (1..5 + live name)
    bound to the shared station:|slot:/Iluminacion/Horarios/Horario{n}_Nombre.

Circuit count and pill positions are AUTO-DETECTED from the source Px (do not trust
file names or the panel number — GreenMAX07 TabAL8.px is GM08's 32-relay view).

Usage:
  python3 tools/greenmax-tablero-gen.py <original.px> --gm GM08 --out /path/out.md [--tab "GreenMAX07 TabAL8.px"] [--legend]

Read-only over the source; only writes --out (or stdout with --stdout).
"""
import argparse
import re
import sys
import xml.dom.minidom as minidom

IMG_NORMAL = "file:^Imagenes/The Harbor/Buttons/BotonNormal.png"


def detect_pills(text):
    """Return {circuit_index: (x, y)} for each pill (Relay[k].BO, 60x30 Picture)."""
    pos = {}
    for m in re.finditer(
        r'<Picture layout="([\d.]+),([\d.]+),60\.0,30\.0"[^>]*>\s*'
        r'<ValueBinding ord="slot:Relay\$5b(\d+)\$5d\$2eBO">\s*<IBooleanToSimple',
        text,
    ):
        pos[int(m.group(3))] = (float(m.group(1)), float(m.group(2)))
    return pos


def horario_legend(x=1080):
    """Read-only 'Horarios' legend: title + 5 rows 'N  <name>' bound to the shared names."""
    rows = [f'    <Label layout="{x}.0,140.0,240.0,18.0" font="bold 12.0pt Arial" '
            f'halign="left" text="Horarios:" />\n']
    for i, y in enumerate([162, 180, 198, 216, 234], 1):
        rows.append(
            f'    <Label layout="{x}.0,{y}.0,250.0,16.0" font="11.0pt Arial" halign="left">'
            f'<ValueBinding ord="station:|slot:/Iluminacion/Horarios/Horario{i}_Nombre">'
            f'<ObjectToString name="text" format="{i}  %out.value%" /></ValueBinding></Label>\n')
    return "".join(rows)


def transform(text, gm, add_legend=False):
    pos = detect_pills(text)
    if not pos:
        sys.exit("NO PILLS FOUND (no Relay[k].BO 60x30 Pictures) — inspect the Px manually")
    n = max(pos)
    missing = [k for k in range(1, n + 1) if k not in pos]
    hoa0 = len(re.findall(r'Relay\$5b\d+\$5d\$2eBO/(?:active|inactive|auto)', text))
    pop0 = len(re.findall(r'PopupBinding ord="slot:R\d+"', text))

    # remove HOA buttons + calendar popups
    text = re.sub(
        r'\s*<ImageButton\b[^>]*>\s*<ActionBinding ord="slot:Relay\$5b\d+\$5d\$2eBO/'
        r'(?:active|inactive|auto)"[^>]*>.*?</ActionBinding>\s*</ImageButton>',
        '', text, flags=re.S)
    text = re.sub(
        r'\s*<Picture\b[^>]*>\s*<PopupBinding ord="slot:R\d+"[^>]*/>\s*</Picture>',
        '', text, flags=re.S)

    # build selectors at (pillX, pillY+30)
    sel = []
    for k in sorted(pos):
        x, yp = pos[k]
        xi, yi = int(x), int(yp + 30)
        sel.append(
            f'    <Picture layout="{xi}.0,{yi}.0,60.0,22.0" image="{IMG_NORMAL}" scale="fitWidth" />\n'
            f'    <Label layout="{xi}.0,{yi}.0,60.0,20.0" halign="center" font="bold 10.0pt Arial">'
            f'<ValueBinding ord="station:|slot:/Iluminacion/{gm}/NameMux{k}">'
            f'<ObjectToString name="text" format="%out.value%" /></ValueBinding></Label>\n'
            f'    <ImageButton layout="{xi}.0,{yi}.0,60.0,22.0" buttonStyle="none">'
            f'<ActionBinding ord="station:|slot:/Iluminacion/{gm}/SelR{k}/set" '
            f'widgetEvent="actionPerformed" /></ImageButton>\n')
    if add_legend:
        sel.append(horario_legend())

    # grow viewSize if the selectors/legend extend past it
    maxy = max(yp for _, yp in pos.values()) + 30 + 22 + 20
    bumped = None
    vm = re.search(r'viewSize="([\d.]+),([\d.]+)"', text)
    if vm:
        vw, vh = float(vm.group(1)), float(vm.group(2))
        if maxy > vh:
            text = text.replace(vm.group(0), f'viewSize="{vw:.1f},{maxy:.1f}"')
            bumped = (vh, round(maxy))

    if text.count('</CanvasPane>') != 1:
        sys.exit(f"expected exactly one </CanvasPane>, found {text.count('</CanvasPane>')}")
    text = text.replace('</CanvasPane>', ''.join(sel) + '  </CanvasPane>')

    minidom.parseString(text)  # validate XML (raises on malformed)
    stats = dict(circuits=n, missing=missing, hoa_removed=hoa0, popups_removed=pop0,
                 pills=text.count('GreenMax On.png'), leds=text.count('<IStatusToSimple'),
                 namemux=len(re.findall(rf'/Iluminacion/{gm}/NameMux\d+', text)),
                 selrset=len(re.findall(rf'/Iluminacion/{gm}/SelR\d+/set', text)),
                 view_bumped=bumped)
    return text, n, stats


def main():
    ap = argparse.ArgumentParser(description="Generate a redesigned GreenMAX tablero Px.")
    ap.add_argument("src", help="original tablero .px from the station backup")
    ap.add_argument("--gm", required=True, help="control-folder tag, e.g. GM08 (plain number, no leading zero)")
    ap.add_argument("--out", help="output .md path (fenced Px). Omit with --stdout.")
    ap.add_argument("--tab", default="", help="PxView/tablero display name for the doc header")
    ap.add_argument("--legend", action="store_true", help="also add the read-only 'Horarios' legend")
    ap.add_argument("--stdout", action="store_true", help="print the .md to stdout instead of a file")
    args = ap.parse_args()

    with open(args.src) as f:
        src = f.read()
    xml, n, stats = transform(src, args.gm, add_legend=args.legend)

    tab = args.tab or args.src.rsplit("/", 1)[-1]
    doc = (f"# {args.gm} — Tablero (`{tab}`) con selector de nombre\n\n"
           f"> {n} circuitos. Original de {args.gm}: HOA+popups quitados, pills+LEDs conservados, "
           f"{n} selectores NameMux/SelR (ords absolutos Iluminacion/{args.gm}/)"
           f"{'; + leyenda Horarios' if args.legend else ''}.\n\n```xml\n" + xml.rstrip() + "\n```\n")

    if args.stdout or not args.out:
        sys.stdout.write(doc)
    else:
        with open(args.out, "w") as f:
            f.write(doc)
        sys.stderr.write(f"written {args.out}\n")
    sys.stderr.write(f"{args.gm}: " + " ".join(f"{k}={v}" for k, v in stats.items()) + "\n")


if __name__ == "__main__":
    main()
