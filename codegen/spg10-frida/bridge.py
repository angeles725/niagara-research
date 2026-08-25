#!/usr/bin/env python3
"""Probe: does the Java bridge ever appear in spawned nre.exe? Resume-first,
then poll typeof Java + Java.available for the process lifetime."""
import time
import frida

JS = r"""
'use strict';
var tries = 0;
var iv = setInterval(function () {
  tries++;
  var t = typeof Java;
  var avail = 'n/a';
  if (t !== 'undefined') {
    try { avail = Java.available; } catch (e) { avail = 'err:' + e; }
  }
  if (tries % 100 === 0 || (t !== 'undefined')) {
    send({ev:'poll', t: tries, typeJava: t, available: avail});
  }
  if (tries > 3000) { clearInterval(iv); send({ev:'done', note:'end of poll'}); }
}, 5);
"""


def main():
    print("[run] bridge-probe", flush=True)
    device = frida.get_local_device()
    pid = device.spawn(
        [r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe", "-licenses"],
        cwd=r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162", stdio="inherit",
    )
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
    deadline = time.time() + 60
    while time.time() < deadline:
        try:
            alive = any(p.pid == pid for p in device.enumerate_processes())
            if not alive:
                break
        except Exception:
            break
        time.sleep(0.2)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
