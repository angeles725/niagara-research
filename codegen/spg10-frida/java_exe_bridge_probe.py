#!/usr/bin/env python3
"""Check whether the Java bridge appears when spawning the STANDARD java.exe."""
import time
import frida

JS = r"""
'use strict';
function check() {
  var t = typeof Java;
  var avail = 'n/a';
  try { if (t !== 'undefined') avail = '' + Java.available; } catch (e) {}
  send({ev:'check', typeJava: t, available: avail, jvmLoaded: (Process.findModuleByName('jvm.dll') !== null)});
}
check();
var iv = setInterval(check, 500);
"""


def main():
    print("[run] java.exe bridge probe", flush=True)
    device = frida.get_local_device()
    pid = device.spawn(
        [r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\jre\bin\java.exe", "-version"],
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
    time.sleep(6)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
