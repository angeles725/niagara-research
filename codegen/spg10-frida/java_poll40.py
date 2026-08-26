#!/usr/bin/env python3
"""Final SP-G10a attempt: poll Java bridge for 40s post-resume (JVM boots ~8s in)."""
import time
import frida

JS = r"""
'use strict';
var tries = 0;
var iv = setInterval(function () {
  tries++;
  var found = false;
  try { found = (typeof Java !== 'undefined' && Java.available); } catch (e) {}
  if (found) {
    clearInterval(iv);
    send({ev:'java-ready', atTick: tries});
    Java.perform(function () {
      send({ev:'java-link', sample: '' + (Java.vm && Java.vm.version ? Java.vm.version : 'vm-present')});
    });
  } else if (tries % 200 === 0) {
    send({ev:'still-waiting', atTick: tries});
  }
  if (tries > 8000) { clearInterval(iv); send({ev:'gave-up', atTick: tries}); }
}, 5);
"""


def main():
    print("[run] java-poll-40s", flush=True)
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
    print("[run] resumed; polling 45s", flush=True)
    time.sleep(45)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
