#!/usr/bin/env python3
"""
dashboard-preview — local preview server for ANY Niagara N4 dashboard (-ux) module.

Iterate on the design (HTML/CSS/JS) of a servlet-based dashboard WITHOUT the
gradle build + sign + deploy cycle: this serves the module's real `rc/` folder
over http://localhost and mocks the servlet API, so you edit → refresh → see.

It is a REUSABLE project tool (not tied to one module): point it at any dashboard
module's rc/ with --rc, and give the servlet mount prefix with --prefix. It
faithfully reproduces the real BWebServlet behaviour so front-end bugs surface
here first:
  * <prefix>/                 -> <rc>/index.html
  * <prefix>/<path>           -> <rc>/<path>        (config.html, img/..., css/..., js/...)
  * GET  <prefix>/api/<name>  -> mock JSON (see --mock)
  * POST <prefix>/api/<name>  -> {"ok":true} and logs the body
  * XHR GUARD enforced: any /api/* request WITHOUT header
    'X-Requested-With: XMLHttpRequest' is 302-redirected to index.html, exactly
    like the chihuahua-style dispatch guard — so a page that forgets the header
    shows "--" here too, before you ever compile. (This is what caught the real
    DashboardPan header regression — see docs/module-best-practices.md §2.)

MOCK DATA (--mock): the servlet's own JSON contract is module-specific, so supply it:
  --mock <file.json>   serve this file verbatim for every GET /api/<name>.
  (none)               serve {} — layout/palette iteration still works (values show
                       as "--"); pass --mock once you want live-looking data.
For an ANIMATED mock (jittered values, status colors), a module may ship its own
thin wrapper (see the DashboardPan-ux/preview-server.py worked example, which builds
the {v,st} payload in code). This generic tool keeps to static/empty by design.

USAGE:
  python3 tools/dashboard-preview.py --rc <module>/src/rc --prefix /dashboardpan
  python3 tools/dashboard-preview.py --rc ./src/rc --prefix /mymod --port 9000 --mock mock.json
then open http://localhost:<port><prefix>/

Requires only Python 3 stdlib. Ctrl+C to stop.
"""
import argparse
import json
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MIME = {
    ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8",
    ".js": "application/javascript; charset=utf-8", ".json": "application/json",
    ".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg",
    ".svg": "image/svg+xml", ".ico": "image/x-icon", ".map": "application/json",
    ".woff": "font/woff", ".woff2": "font/woff2", ".ttf": "font/ttf", ".eot": "application/vnd.ms-fontobject",
}


def make_handler(rc_dir, prefix, mock_obj):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):
            sys.stderr.write("  %s\n" % (fmt % args))

        def _has_xhr(self):
            return self.headers.get("X-Requested-With", "") == "XMLHttpRequest"

        def _json(self, obj, code=200):
            body = json.dumps(obj).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

        def _redirect(self, location):
            self.send_response(302)
            self.send_header("Location", location)
            self.end_headers()

        def _strip(self, path):
            path = path.split("?", 1)[0]
            if prefix and path.startswith(prefix):
                path = path[len(prefix):]
            return path or "/"

        def _serve_file(self, rel):
            rel = rel.lstrip("/")
            if rel in ("", "/"):
                rel = "index.html"
            if ".." in rel or rel.startswith(("/", "\\")):
                self.send_error(404)
                return
            full = os.path.join(rc_dir, *rel.split("/"))
            if not os.path.isfile(full):
                self.send_error(404, "Not found: %s" % rel)
                return
            with open(full, "rb") as f:
                data = f.read()
            self.send_response(200)
            self.send_header("Content-Type", MIME.get(os.path.splitext(full)[1].lower(), "application/octet-stream"))
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")  # always fresh while iterating
            self.end_headers()
            self.wfile.write(data)

        def _hmi(self):
            # WEB-HMI10/CF frame: a 1280x800 bezel around the live local dashboard
            # (same-origin localhost iframe -> its scripts run, unlike the artifact CSP).
            frame = ("""<!doctype html><html><head><meta charset="utf-8">
<title>HMI 1280x800 preview</title><style>
 html,body{margin:0;height:100%%;background:#0b0e12;color:#c7ced8;
   font:13px system-ui,sans-serif;display:flex;flex-direction:column;
   align-items:center;justify-content:center;overflow:hidden}
 #cap{margin:10px 0 8px;letter-spacing:.12em;color:#8b93a1;font-size:11px}
 #device{background:#171a1f;border:1px solid #2a2f37;border-radius:22px;padding:22px;
   box-shadow:0 20px 60px -20px #000;transform-origin:top center}
 #dot{width:7px;height:7px;border-radius:50%%;background:#2a2f37;margin:0 auto 12px}
 iframe{width:1280px;height:800px;border:0;display:block;background:#fff;border-radius:4px}
</style></head><body>
<div id="cap">HMI 10.1&quot; capacitivo &middot; 1280&times;800</div>
<div id="device"><div id="dot"></div><iframe src="%s/" title="dashboard"></iframe></div>
<script>var d=document.getElementById('device');
 function fit(){d.style.transform='scale('+Math.min(1,(innerWidth-60)/1324,(innerHeight-96)/892)+')';}
 addEventListener('resize',fit);fit();</script></body></html>""" % (prefix or "")).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(frame)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(frame)

        def do_GET(self):
            p = self._strip(self.path)
            if p == "/hmi":
                self._hmi()
                return
            if p.startswith("/api/"):
                if not self._has_xhr():
                    self._redirect((prefix or "") + "/index.html")
                    return
                self._json(mock_obj)   # same mock for any /api/<name> GET
                return
            self._serve_file(p)

        def do_POST(self):
            p = self._strip(self.path)
            if p.startswith("/api/") and not self._has_xhr():
                self._redirect((prefix or "") + "/index.html")
                return
            length = int(self.headers.get("Content-Length", 0) or 0)
            body = self.rfile.read(length).decode("utf-8", "replace") if length else ""
            sys.stderr.write("  [POST %s] %s\n" % (p, body))
            self._json({"ok": True})

    return Handler


def main():
    ap = argparse.ArgumentParser(description="Local preview server for a Niagara dashboard -ux module.")
    ap.add_argument("--rc", default="./src/rc", help="path to the module's rc/ folder (default ./src/rc)")
    ap.add_argument("--prefix", default="/dashboardpan", help="servlet mount prefix (default /dashboardpan)")
    ap.add_argument("--port", type=int, default=8080, help="port (default 8080)")
    ap.add_argument("--mock", help="JSON file served for every GET /api/<name> (default: empty {})")
    args = ap.parse_args()

    rc_dir = os.path.abspath(args.rc)
    if not os.path.isdir(rc_dir):
        sys.exit("ERROR: --rc no es un directorio: %s" % rc_dir)
    prefix = "/" + args.prefix.strip("/") if args.prefix.strip("/") else ""

    mock_obj = {}
    if args.mock:
        with open(args.mock, "r", encoding="utf-8") as f:
            mock_obj = json.load(f)
    else:
        sys.stderr.write("  (sin --mock: /api/* devuelve {} — el diseño/paleta se ve, los valores salen '--')\n")

    srv = ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(rc_dir, prefix, mock_obj))
    print("dashboard-preview -> http://localhost:%d%s/" % (args.port, prefix))
    print("  serving: %s" % rc_dir)
    print("  mock:    %s" % (args.mock or "(vacio)"))
    print("  Ctrl+C para detener. Edita rc/ y refresca (sin recompilar).")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\ndetenido.")


if __name__ == "__main__":
    main()
