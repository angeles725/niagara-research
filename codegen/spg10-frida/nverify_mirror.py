#!/usr/bin/env python3
"""SP-G10 native MODULE-side MIRROR against nverify.exe (disposable process).

Run `nverify.exe <target.jar>` under Frida, hook
  nre.dll SignatureUtil::checkFileSignature(char const*) -> int
and force its return value (0 = valid). The target jar is a DISPOSABLE
unsigned copy in scratch — the operator's real install is never touched.

Modes:
  log   — log call/return only (control)
  force — replace the return with FORCE_VAL (the mirror)
"""
import sys
import time

import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
FORCE_VAL = int(sys.argv[2]) if len(sys.argv) > 2 else 0
TARGET = sys.argv[3] if len(sys.argv) > 3 else r"C:\Users\equipo\AppData\Local\Temp\spg10-probe\tamper\aaphp-rt-unsigned.jar"
assert MODE in ("log", "force")

NVERIFY = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nverify.exe"
CWD = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162"
TIMEOUT_S = 120

JS = r"""
'use strict';
var MODE = %MODE%;
var FORCE_VAL = %FORCE%;
var hits = {};

function watch() {
  var m = null;
  try { m = Process.getModuleByName('nre.dll'); } catch (e) {}
  if (m === null) { send({ev:'wait', note:'nre.dll not loaded yet'}); return false; }
  var addr = null;
  var syms = m.enumerateSymbols();
  for (var i = 0; i < syms.length; i++) {
    if (syms[i].name === 'SignatureUtil::checkFileSignature') { addr = syms[i].address; break; }
  }
  if (addr === null) { send({ev:'skip', why: 'symbol not found'}); return false; }

  Interceptor.attach(addr, {
    onEnter: function (args) {
      var k = 'nre.dll::SignatureUtil::checkFileSignature';
      hits[k] = (hits[k] || 0) + 1;
      var file = '?'; try { file = args[0].readUtf8String(); } catch (e) {}
      send({ev:'call', fn: k, n: hits[k], file: file});
    },
    onLeave: function (retval) {
      var k = 'nre.dll::SignatureUtil::checkFileSignature';
      var v = -1; try { v = retval.toUInt32(); } catch (e) {}
      send({ev:'ret', fn: k, n: hits[k], val: v});
      if (MODE === 'force') {
        retval.replace(FORCE_VAL);
        send({ev:'forced', fn: k, val: FORCE_VAL});
      }
    }
  });
  send({ev:'hooked', addr: addr.toString()});
  return true;
}

if (!watch()) {
  var tries = 0;
  var iv = setInterval(function () {
    if (watch()) { clearInterval(iv); return; }
    if (++tries > 1000) { clearInterval(iv); send({ev:'giveup', why:'nre.dll never loaded'}); }
  }, 10);
}
"""

JS = JS.replace("%MODE%", repr(MODE)).replace("%FORCE%", str(FORCE_VAL))


def main():
    print(f"[run] nverify mirror mode={MODE} force={FORCE_VAL} target={TARGET}", flush=True)
    device = frida.get_local_device()
    pid = device.spawn([NVERIFY, TARGET], cwd=CWD, stdio="inherit")
    print(f"[run] spawned nverify pid={pid}", flush=True)
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
