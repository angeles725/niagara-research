#!/usr/bin/env python3
"""SP-G10 native MODULE-side mirror: force checkFileSignature to return valid.

The module-integrity gate's native entry is
  nre.dll  SignatureUtil::checkFileSignature(char const*) -> int   (1 arg)
  dsfspi.dll DsfUtil::checkFileSignature(byte*, int, char const*, int) -> int

Modes:
  log   — log calls + return values (learn the 'valid' code).
  force — force the OUTER (SignatureUtil) return value to FORCE_VAL.
  force_user — force the OUTER return only on calls whose filename arg
               mentions 'chihuahua' (the operator's own module), else passthrough.

This tests "can a shim make the module verifier return valid", the module half
of SP-G10. The LICENSE half is Java-side (BC FIPS) on this install and needs a
Java-bridge agent (unavailable on this host's barebone agent) — recorded as a
typed wall separately.
"""
import sys
import time

import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
FORCE_VAL = int(sys.argv[2]) if len(sys.argv) > 2 else 0
assert MODE in ("log", "force", "force_user")

NRE = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe"
CWD = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162"
TIMEOUT_S = 200

JS = r"""
'use strict';
var MODE = %MODE%;
var FORCE_VAL = %FORCE%;
var hits = {};

function watch(name, filterFn) {
  var m = null;
  try { m = Process.getModuleByName('nre.dll'); } catch (e) {}
  var addr = null;
  if (m !== null) {
    var syms = m.enumerateSymbols();
    for (var i = 0; i < syms.length; i++) {
      if (syms[i].name === name) { addr = syms[i].address; break; }
    }
  }
  if (addr === null) { send({ev:'skip', fn: name, why: 'symbol not found'}); return; }

  Interceptor.attach(addr, {
    onEnter: function (args) {
      var k = 'nre.dll::' + name;
      hits[k] = (hits[k] || 0) + 1;
      var file = '?';
      try { file = args[0].readUtf8String(); } catch (e) {}
      send({ev:'call', fn: k, n: hits[k], file: file});
      this._file = file;
      this._match = (file.indexOf('chihuahua') !== -1);
    },
    onLeave: function (retval) {
      var k = 'nre.dll::' + name;
      var v = -1; try { v = retval.toUInt32(); } catch (e) {}
      send({ev:'ret', fn: k, n: hits[k], val: v, file: this._file, match: this._match});
      if (MODE === 'force' || (MODE === 'force_user' && this._match)) {
        retval.replace(FORCE_VAL);
        send({ev:'forced', fn: k, val: FORCE_VAL, file: this._file});
      }
    }
  });
}

watch('SignatureUtil::checkFileSignature');
"""

JS = JS.replace("%MODE%", repr(MODE)).replace("%FORCE%", str(FORCE_VAL))


def main():
    print(f"[run] native module mirror mode={MODE} force={FORCE_VAL}", flush=True)
    device = frida.get_local_device()
    pid = device.spawn([NRE, "-licenses"], cwd=CWD, stdio="inherit")
    print(f"[run] spawned pid={pid}", flush=True)
    session = device.attach(pid)
    script = session.create_script(JS)

    def on_message(msg, _data):
        if msg.get("type") == "send":
            print(f"[frida] {msg['payload']}", flush=True)
        elif msg.get("type") == "error":
            print(f"[frida-error] {msg.get('stack') or msg.get('description')}", flush=True)

    script.on("message", on_message)
    script.load()
    device.resume(pid)
    print("[run] resumed", flush=True)

    deadline = time.time() + TIMEOUT_S
    while time.time() < deadline:
        try:
            alive = any(p.pid == pid for p in device.enumerate_processes())
            if not alive:
                break
        except Exception:
            break
        time.sleep(0.5)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
