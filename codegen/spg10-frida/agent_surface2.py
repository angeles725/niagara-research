#!/usr/bin/env python3
"""Check the injected agent's global surface AFTER the standard frida reinstall."""
import time
import frida

JS = r"""
'use strict';
var keys = Object.getOwnPropertyNames(globalThis).filter(function(k){
  return /^(Frida|Gum|Java|Module|Process|Interceptor|Native|ObjC|Script|Memory|Thread)/.test(k);
});
send({ev:'globals', keys: keys});
send({ev:'java-type', t: typeof Java});
send({ev:'module-static', have: Object.keys(Module)});
"""


def main():
    print("[run] agent-surface", flush=True)
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
    time.sleep(10)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
