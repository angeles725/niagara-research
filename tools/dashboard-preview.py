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


EDITOR_SNIPPET = """
<style id="__ed_css">
  #__ed{position:fixed;top:10px;left:10px;z-index:2147483647;width:300px;max-height:92vh;
        overflow:auto;background:rgba(20,22,26,.94);color:#e6e9ee;font:12px system-ui,sans-serif;
        border:1px solid #3a3f47;border-radius:10px;box-shadow:0 12px 40px -12px #000;padding:12px}
  #__ed h4{margin:0 0 6px;font-size:12px;letter-spacing:.06em}
  #__ed .hint{color:#9aa2ad;font-size:11px;line-height:1.5;margin-bottom:8px}
  #__ed table{width:100%;border-collapse:collapse;font-size:11px}
  #__ed th,#__ed td{padding:2px 3px;text-align:left;border-bottom:1px solid #2a2f37;white-space:nowrap}
  #__ed th{color:#9aa2ad;font-weight:600}
  #__ed td b{color:#7fd1a8}
  #__ed .btns{display:flex;flex-wrap:wrap;gap:6px;margin-top:10px}
  #__ed button{background:#2b303a;color:#e6e9ee;border:1px solid #3a3f47;border-radius:6px;
               padding:6px 8px;font-size:11px;cursor:pointer}
  #__ed button:hover{background:#353b46}
  #__ed .warn{background:#3a2b2b;border-color:#5a3a3a}
  #__ed textarea{width:100%;height:70px;margin-top:8px;background:#11141a;color:#cfe;border:1px solid #2a2f37;
                 border-radius:6px;font:11px monospace;padding:6px;display:none}
  #__ed .ok{color:#7fd1a8;font-size:11px;min-height:14px;margin-top:4px}
</style>
<script id="__ed_js">
(function(){
  if (window.__labelEditor) return; window.__labelEditor = true;
  function ready(){
    if (typeof ui==='undefined' || typeof SENSORS==='undefined'){ return setTimeout(ready,150); }
    ui.editPuntos = true;
    var box=document.createElement('div'); box.id='__ed';
    box.innerHTML =
      '<h4>EDITOR DE LABELS (solo preview)</h4>'+
      '<div class="hint">Abr\\u00ed un cuarto (clic en la lista de la derecha). '+
      'Arrastr\\u00e1 el <b>punto</b> (c\\u00edrculo) y la <b>etiqueta</b> para moverlos. '+
      'Los valores se actualizan abajo. Copi\\u00e1 y pas\\u00e1melos, o los aplico yo.</div>'+
      '<div id="__ed_room"></div>'+
      '<table><thead><tr><th>tag</th><th>rx,ry</th><th>rlx,rly</th><th>side</th></tr></thead>'+
      '<tbody id="__ed_body"></tbody></table>'+
      '<div class="btns">'+
      '<button id="__ed_copy">Copiar valores</button>'+
      '<button id="__ed_all">Copiar TODO</button>'+
      '<button id="__ed_reset" class="warn">Reset respaldo local</button>'+
      '</div><div class="ok" id="__ed_ok"></div><textarea id="__ed_ta" readonly></textarea>';
    document.body.appendChild(box);
    var fc=document.getElementById('frameC'); if(fc) fc.classList.add('edit');

    function rows(all){
      return SENSORS.filter(function(s){ return all || s.cuarto===ui.cuarto; })
        .map(function(s){
          return s.tag+': rx:'+(+s.rx).toFixed(2)+', ry:'+(+s.ry).toFixed(2)+
                 ', rlx:'+(+s.rlx).toFixed(2)+', rly:'+(+s.rly).toFixed(2)+', rside:"'+s.rside+'"';
        }).join('\\n');
    }
    function render(){
      var fc2=document.getElementById('frameC'); if(fc2) fc2.classList.add('edit');
      if(ui) ui.editPuntos=true;
      var r=document.getElementById('__ed_room');
      var b=document.getElementById('__ed_body');
      if(ui.cuarto==null){ r.textContent='Ning\\u00fan cuarto abierto.'; b.innerHTML=''; return; }
      var nm=(typeof CUARTOS!=='undefined'&&CUARTOS.find)?(CUARTOS.find(function(c){return c.id===ui.cuarto;})||{}).nombre:'';
      r.innerHTML='<b>'+(nm||('Cuarto '+ui.cuarto))+'</b>';
      var html='';
      SENSORS.filter(function(s){return s.cuarto===ui.cuarto;}).forEach(function(s){
        html+='<tr><td>'+s.tag+'</td><td><b>'+(+s.rx).toFixed(2)+','+(+s.ry).toFixed(2)+
              '</b></td><td>'+(+s.rlx).toFixed(2)+','+(+s.rly).toFixed(2)+'</td><td>'+s.rside+'</td></tr>';
      });
      b.innerHTML=html;
    }
    function copy(txt,msg){
      var ok=document.getElementById('__ed_ok'); var ta=document.getElementById('__ed_ta');
      ta.style.display='block'; ta.value=txt; ta.select();
      try{ (navigator.clipboard&&navigator.clipboard.writeText)?navigator.clipboard.writeText(txt):document.execCommand('copy'); ok.textContent=msg+' (copiado)'; }
      catch(e){ ok.textContent=msg+' (selecciona y Ctrl+C)'; }
    }
    document.getElementById('__ed_copy').onclick=function(){
      if(ui.cuarto==null){ document.getElementById('__ed_ok').textContent='Abr\\u00ed un cuarto primero.'; return; }
      copy(rows(false),'Cuarto '+ui.cuarto);
    };
    document.getElementById('__ed_all').onclick=function(){ copy(rows(true),'Todos los cuartos'); };
    document.getElementById('__ed_reset').onclick=function(){
      try{ localStorage.removeItem('panccadia.puntos.v11'); }catch(e){}
      document.getElementById('__ed_ok').textContent='Respaldo local borrado. Recargando...';
      setTimeout(function(){ location.reload(); },500);
    };
    setInterval(render,250); render();
  }
  ready();
})();
</script>
"""


def make_handler(rc_dir, prefix, mock_obj, editor=False):
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
            ext = os.path.splitext(full)[1].lower()
            if editor and ext == ".html":
                txt = data.decode("utf-8", "replace")
                snip = EDITOR_SNIPPET
                low = txt.lower()
                idx = low.rfind("</body>")
                txt = (txt[:idx] + snip + txt[idx:]) if idx != -1 else (txt + snip)
                data = txt.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", MIME.get(ext, "application/octet-stream"))
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
    ap.add_argument("--editor", action="store_true",
                    help="inject a preview-only label editor (drag points/labels, export rx/ry/rlx/rly/rside); "
                         "never touches the module rc/")
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

    srv = ThreadingHTTPServer(("127.0.0.1", args.port), make_handler(rc_dir, prefix, mock_obj, args.editor))
    print("dashboard-preview -> http://localhost:%d%s/" % (args.port, prefix))
    print("  serving: %s" % rc_dir)
    print("  mock:    %s" % (args.mock or "(vacio)"))
    if args.editor:
        print("  editor:  ON (editor de labels inyectado; NO toca el modulo)")
    print("  Ctrl+C para detener. Edita rc/ y refresca (sin recompilar).")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\ndetenido.")


if __name__ == "__main__":
    main()
