#!/usr/bin/env python3
"""Frida 17.x idioms: locate dsfspi + hunt DSA verify in nre.exe (read-only)."""
import time
import frida

JS = r"""
'use strict';
var pm = Process.getModuleByName('nre.dll');
send({ev:'nre', base: pm.base.toString(), size: pm.size, keys: Object.keys(pm).sort()});

var mm = null;
try { mm = Process.getModuleByName('dsfspi.dll'); } catch (e) {}
send({ev:'dsfspi', found: mm !== null, base: mm ? mm.base.toString() : 'null', keys: mm ? Object.keys(mm).sort() : []});

function findDsfV() {
  if (mm === null) return [];
  var out = [];
  var syms = [];
  try { syms = mm.enumerateSymbols(); } catch (e) {}
  send({ev:'dsf-sym-total', n: syms.length});
  for (var i = 0; i < syms.length; i++) {
    var s = syms[i];
    if ((s.name || '').indexOf('DsfSha1WithDsaSignature') !== -1) {
      out.push(s.name + '@' + s.address.toString());
    }
  }
  return out;
}
var hits = findDsfV();
send({ev:'dsf-hits', n: hits.length, hits: hits});
"""


def main():
    print("[run] idioms", flush=True)
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
