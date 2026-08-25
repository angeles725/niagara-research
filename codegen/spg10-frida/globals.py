#!/usr/bin/env python3
"""Snapshot which Frida JS globals exist in a disposable nre.exe -licenses."""
import sys
import time
import frida

JS = r"""
'use strict';
var report = {};
['Module', 'Process', 'Interceptor', 'NativePointer', 'Memory', 'Thread', 'Script'].forEach(function (g) {
  report[g] = typeof globalThis[g];
});
send({ev:'globals', report: report});
"""


def main():
    print("[run] globals", flush=True)
    device = frida.get_local_device()
    pid = device.spawn(
        [r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe", "-licenses"],
        cwd=r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162",
        stdio="inherit",
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
