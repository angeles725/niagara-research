#!/usr/bin/env python3
"""SP-G10 census: are ANY dsfspi/nre crypto entrypoints hit during -licenses?

Hooks (log-only): checkFileSignature (module RSA), DsfSha1WithDsaSignature
verify/sign (C++), JNI engineVerify0/engineSign0, isFeaturePresent,
and Java_com_tridium_dsf_provider_spi engine* natives. Counts every hit.
"""
import time
import frida

JS = r"""
'use strict';
var hits = {};
var SUB = /(DsfSha1WithDsaSignature|checkFileSignature|isFeaturePresent|engineVerify0|engineSign0|engineInitVerify0|createDSASignature|parseDSASignature)/;

function watchMod(modName) {
  var m = null;
  try { m = Process.getModuleByName(modName); } catch (e) {}
  if (m === null) { send({ev:'nomod', mod: modName}); return; }
  var syms = m.enumerateSymbols();
  var n = 0;
  for (var i = 0; i < syms.length; i++) {
    var s = syms[i];
    if (SUB.test(s.name)) {
      n++;
      (function (name, addr) {
        Interceptor.attach(addr, {
          onEnter: function () {
            var k = modName + '::' + name;
            hits[k] = (hits[k] || 0) + 1;
            send({ev:'call', fn: k, n: hits[k]});
          },
          onLeave: function (retval) {
            var k = modName + '::' + name;
            var v = -1; try { v = retval.toUInt32(); } catch (e) {}
            send({ev:'ret', fn: k, val: v});
          }
        });
      })(s.name, s.address);
    }
  }
  send({ev:'watched', mod: modName, n: n});
}

watchMod('dsfspi.dll');
watchMod('nre.dll');
"""


def main():
    print("[run] census", flush=True)
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

    deadline = time.time() + 200
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
    print(f"[run] done. total hit events logged above", flush=True)


if __name__ == "__main__":
    main()
