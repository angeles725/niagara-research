#!/usr/bin/env python3
"""SP-G10 probe diagnostic — enumerate loaded modules + find the DSA verify
entry in the disposable nre.exe process (read-only, log only)."""
import sys
import time
import frida

TIMEOUT_S = 180

JS = r"""
'use strict';
var names = [];
Module.enumerateModules().forEach(function (m) {
  var n = (m.name || '').toLowerCase();
  if (n.indexOf('dsf') !== -1 || n.indexOf('mocana') !== -1 ||
      n.indexOf('crypto') !== -1 || n.indexOf('nre') !== -1 || n.indexOf('baja') !== -1) {
    names.push(m.name + ' @ ' + m.base);
  }
});
send({ev:'modules', names: names});

// hunt the DSA verify symbol across every loaded module
var found = [];
Module.enumerateModules().forEach(function (m) {
  var ex = [];
  try { ex = Module.enumerateExports(m.name); } catch (e) { return; }
  ex.forEach(function (e) {
    if (e.name.indexOf('DsfSha1WithDsaSignature') !== -1) {
      found.push({mod: m.name, name: e.name, addr: e.address.toString()});
    }
  });
});
send({ev:'dsf-symbols', count: found.length, found: found});
"""


def main():
    print("[run] enumerate", flush=True)
    device = frida.get_local_device()
    pid = device.spawn(
        [r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe", "-licenses"],
        cwd=r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162",
        stdio="inherit",
    )
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
    print("[run] resumed; waiting", flush=True)

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
