#!/usr/bin/env python3
"""SP-G10 native MODULE-side mirror via nre.exe -licenses (disposable process).

Hook the OUTER nre.dll SignatureUtil::checkFileSignature (1 arg: filename)
during `nre.exe -licenses`. This is the exact entry the station/module-load
path consumes. Modes:

  log          — log call/return (which file, what code).
  force_valid  — force EVERY checkFileSignature to 0 (valid)   [the bypass]
  force_user_invalid — force a NON-ZERO (invalid) code only for the
                 operator's own module jar if it appears (mechanism demo),
                 everything else passthrough.
  force_invalid_all — force EVERY checkFileSignature to 1 (invalid) to show
                 the verdict IS consumed from this return value.

No file on the install is modified in any mode — only the in-process return
value, in a disposable nre.exe.
"""
import sys
import time

import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
INVALID = 1
TIMEOUT_S = 200

NRE = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe"
CWD = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162"

JS = r"""
'use strict';
var MODE = %MODE%;
var hits = {};

function watch() {
  var m = null;
  try { m = Process.getModuleByName('nre.dll'); } catch (e) {}
  if (m === null) return false;
  var addr = null;
  var syms = m.enumerateSymbols();
  for (var i = 0; i < syms.length; i++) {
    if (syms[i].name === 'SignatureUtil::checkFileSignature') { addr = syms[i].address; break; }
  }
  if (addr === null) { send({ev:'skip'}); return true; }

  Interceptor.attach(addr, {
    onEnter: function (args) {
      hits.calls = (hits.calls || 0) + 1;
      var file = '?'; try { file = args[0].readUtf8String(); } catch (e) {}
      send({ev:'call', n: hits.calls, file: file});
    },
    onLeave: function (retval) {
      var v = -1; try { v = retval.toUInt32(); } catch (e) {}
      var name = 'nre.dll::SignatureUtil::checkFileSignature';
      if (MODE === 'force_valid') {
        retval.replace(0);
        send({ev:'forced', n: hits.calls, val: 0, orig: v});
      } else if (MODE === 'force_invalid_all') {
        retval.replace(1);
        send({ev:'forced', n: hits.calls, val: 1, orig: v});
      }
      send({ev:'ret', n: hits.calls, val: v});
    }
  });
  send({ev:'hooked', addr: addr.toString()});
  return true;
}

if (!watch()) {
  var tries = 0;
  var iv = setInterval(function () {
    if (watch()) { clearInterval(iv); return; }
    if (++tries > 1500) { clearInterval(iv); send({ev:'giveup'}); }
  }, 5);
}
"""

JS = JS.replace("%MODE%", repr(MODE))


def main():
    print(f"[run] mirror via nre mode={MODE}", flush=True)
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
        time.sleep(0.3)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
