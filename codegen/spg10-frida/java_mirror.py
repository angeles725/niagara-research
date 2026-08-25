#!/usr/bin/env python3
"""SP-G10 Java-layer hook: log or force LicenseUtil.verify => true (the mirror).

The license verify path on this bcfips install is JAVA-side (LicenseFile ->
LicenseUtil.verify -> Signature.getInstance -> BouncyCastleFipsProvider), NOT
the native dsfspi DSA (live census proved zero DsfSha1WithDsaSignature::verify
calls). So the interposition target is the Java method
com.tridium.sys.license.LicenseUtil.verify (all overloads).

Modes:
  log   — tally calls/return values, never modify.
  force — return true for every LicenseUtil.verify overload ("the mirror").
"""
import re
import sys
import time

import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
assert MODE in ("log", "force")

NRE = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe"
CWD = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162"
TIMEOUT_S = 200

JS = r"""
'use strict';
var MODE = %MODE%;

function install() {
  if (typeof Java === 'undefined' || !Java.available) return false;
  Java.perform(function () {
    try {
      var LU = Java.use('com.tridium.sys.license.LicenseUtil');
      var n = 0;
      LU.verify.overloads.forEach(function (ov) {
        ov.implementation = function () {
          n++;
          send({ev:'verify-call', n: n, args: ov.argumentTypes.length});
          if (MODE === 'force') {
            send({ev:'forced-true', n: n, args: ov.argumentTypes.length});
            return true;
          }
          var r = ov.apply(this, arguments);
          send({ev:'verify-ret', n: n, ret: r});
          return r;
        };
      });
      send({ev:'hooked', overloads: LU.verify.overloads.length});
    } catch (e) {
      send({ev:'hook-err', err: '' + e});
    }
  });
  return true;
}

// The JVM is attached after spawn; wait for Java.available before installing.
if (!install()) {
  send({ev:'waiting-javavm'});
  var tries = 0;
  var iv = setInterval(function () {
    if (install()) { clearInterval(iv); return; }
    if (++tries > 1000) {
      clearInterval(iv);
      send({ev:'giveup', note:'Java VM never became available in 10s'});
    }
  }, 10);
}
"""

JS = JS.replace("%MODE%", repr(MODE))


def main():
    print(f"[run] java mode={MODE}", flush=True)
    device = frida.get_local_device()
    pid = device.spawn([NRE, "-licenses"], cwd=CWD, stdio="inherit")
    print(f"[run] spawned pid={pid}", flush=True)
    session = device.attach(pid)
    script = session.create_script(JS)
    hooked_state = {"hooked": False}

    def on_message(msg, _data):
        if msg.get("type") == "send":
            p = msg["payload"]
            if p.get("ev") == "hooked":
                hooked_state["hooked"] = True
            print(f"[frida] {p}", flush=True)
        elif msg.get("type") == "error":
            print(f"[frida-error] {msg.get('stack') or msg.get('description')}", flush=True)

    script.on("message", on_message)
    script.load()
    # Wait for the Java bridge to install the hook BEFORE resuming the process,
    # otherwise nre can run license verification ahead of us.
    print("[run] waiting for hook install", flush=True)
    t0 = time.time()
    while not hooked_state["hooked"] and time.time() - t0 < 30:
        time.sleep(0.2)
    if hooked_state["hooked"]:
        device.resume(pid)
        print("[run] resumed", flush=True)
    else:
        print("[run] hook never installed; resuming anyway", flush=True)
        device.resume(pid)

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
