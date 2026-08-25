#!/usr/bin/env python3
"""Dump the loaded agent's version + the injected script runtime fingerprint."""
import time
import frida

JS = r"""
'use strict';
send({ev:'frida-ver', v: (typeof Frida !== 'undefined') ? (Frida.version || 'no Frida.version') : 'no Frida'});
send({ev:'global-keys', keys: Object.getOwnPropertyNames(globalThis).filter(function(k){return /^(Frida|Gum|Java|Module|Process|Interceptor|Native)/.test(k);})});
"""


def main():
    print("[run] agent-version", flush=True)
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
    time.sleep(8)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
