#!/usr/bin/env python3
"""Enumerate which Module.* methods exist, then hunt DSA verify (read-only)."""
import time
import frida

JS = r"""
'use strict';
var have = {};
['getBaseAddress','findBaseAddress','getGlobalModuleByName','enumerateExports',
 'enumerateModules','findExportByName','getExportByName','load'].forEach(function (k) {
  have[k] = typeof Module[k];
});
send({ev:'module-static', have: have});

// instance methods
var mm = Module.enumerateModules();
send({ev:'module-count', n: mm.length});
var m0 = mm[0];
send({ev:'m0-keys', keys: Object.keys(m0), name: m0.name});

// try common ways to find dsfspi
var dsf = null, how = null;
try { dsf = Module.getBaseAddress('dsfspi.dll'); how = 'getBaseAddress'; } catch (e) {}
if (dsf === null) { try { dsf = Module.findBaseAddress('dsfspi.dll'); how = 'findBaseAddress'; } catch (e) {} }
if (dsf === null) {
  for (var i = 0; i < mm.length; i++) {
    var n = (mm[i].name || '').toLowerCase();
    if (n.indexOf('dsfspi') !== -1) { dsf = mm[i].base; how = 'scan-' + mm[i].name; break; }
  }
}
send({ev:'dsfspi', base: dsf === null ? 'null' : dsf.toString(), how: how});

// hunt verify symbol
var hits = [];
for (var j = 0; j < mm.length; j++) {
  var ex = [];
  try { ex = Module.enumerateExports(mm[j].name); } catch (e2) { continue; }
  for (var k = 0; k < ex.length; k++) {
    if (ex[k].name.indexOf('DsfSha1WithDsaSignature') !== -1) {
      hits.push(mm[j].name + '::' + ex[k].name + '@' + ex[k].address.toString());
    }
  }
}
send({ev:'dsf-hits', n: hits.length, hits: hits});
"""


def main():
    print("[run]", flush=True)
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
