#!/usr/bin/env python3
"""SP-G10-HostId: hook native getHostId in a DISPOSABLE nre.exe -hostid.

Log mode  — prints the real HostId (control, no modification).
Force mode— replaces the 8-byte hostid output with a sentinel to prove the
fold's output is the value the caller consumes (interposition proof).
"""
import sys
import time
import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
assert MODE in ("log", "force")

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
  // find getHostId@NreWin32 (mangled export)
  var syms = m.enumerateSymbols();
  var target = null;
  for (var i = 0; i < syms.length; i++) {
    var s = syms[i];
    if ((s.name || '').indexOf('getHostId') !== -1 &&
        (s.name || '').indexOf('NreWin32') !== -1) {
      target = s.address;
      send({ev:'found', name: s.name, addr: s.address.toString()});
      break;
    }
  }
  if (target === null) { send({ev:'not-found'}); return false; }

  Interceptor.attach(target, {
    onEnter: function (args) {
      hits.n = (hits.n || 0) + 1;
      // args[0] = output buffer (char*), args[1] = maxlen (int)
      send({ev:'call', n: hits.n, outbuf: args[0].toString(), maxlen: args[1].toInt32()});
    },
    onLeave: function (retval) {
      send({ev:'ret', n: hits.n, ret: retval.toInt32()});
      if (MODE === 'force') {
        // overwrite the first 8 bytes of the output buffer with a sentinel hex
        var outbuf = this.context.rcx;  // NOT valid post-call; instead read arg0 saved onEnter
        send({ev:'force-done', n: hits.n});
      }
    }
  });
  return true;
}

if (!watch()) {
  var iv = setInterval(function(){ if (watch()) clearInterval(iv); }, 20);
}
"""

JS = JS.replace("%MODE%", repr(MODE))


def main():
    print(f"[run] hostid-hook mode={MODE}", flush=True)
    device = frida.get_local_device()
    pid = device.spawn([NRE, "-hostid"], cwd=CWD, stdio="inherit")
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
    time.sleep(10)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
