#!/usr/bin/env python3
"""Report the exact Frida runtime version + module/proc API surface in-nre."""
import time
import frida

JS = r"""
'use strict';
send({ev:'frida', version: (typeof Frida !== 'undefined') ? Frida.version : 'no-Frida-global'});
send({ev:'gum', version: (typeof Gum !== 'undefined') ? Gum.version : 'no-Gum-global'});
var m = typeof Module;
send({ev:'module-keys', keys: (m === 'function') ? Object.keys(Module) : 'Module is ' + m});
var p = typeof Process;
send({ev:'process-keys', keys: (p === 'object') ? Object.keys(Process) : 'Process is ' + p});
send({ev:'thread', keys: (typeof Thread === 'function') ? Object.keys(Thread) : 'Thread is ' + typeof Thread});
"""


def main():
    print("[run] runtime", flush=True)
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
