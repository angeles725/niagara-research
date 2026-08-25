#!/usr/bin/env python3
"""SP-G10 — Frida interposition ("mirror") PoC against a DISPOSABLE nre.exe.

Frida 17.17.0 (gumjs rename): Module is stripped to getGlobalExportByName;
module enumeration goes through Process.getModuleByName + enumerateSymbols.
Hook targets (re-anchored at runtime, ASLR): the dsfspi DSA verify entrypoints.

Modes:
  log   — spawn `nre.exe -licenses`, hook the verify entrypoints, LOG calls
           and return values, never modify. Learns which entrypoint carries
           the license path and what return value means "valid".
  force — same, but FORCE the return value to FORCE_VAL ("the mirror").

Safety (METHODOLOGY §12/§19): spawns a FRESH nre.exe only; never attaches to
niagarad.exe/station.exe; performs NO file writes. The reversible license
tamper + byte-identical restore is driven OUTSIDE this script (bash). SECRETS
DISCIPLINE: logs structure (names, counts, return codes), never secret values.
"""
import sys
import time

import frida

MODE = sys.argv[1] if len(sys.argv) > 1 else "log"
FORCE_VAL = int(sys.argv[2]) if len(sys.argv) > 2 else 1
assert MODE in ("log", "force"), "mode must be log|force"

NRE = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\nre.exe"
CWD = r"C:\Honeywell\OptimizerSupervisor-N4.14.0.162"
ARGS = ["-licenses"]
TIMEOUT_S = 180

JS = r"""
'use strict';
var RET_MODE = %MODE%;
var FORCE_VAL = %FORCE%;
var hits = {};
var wanted = [
  'DsfSha1WithDsaSignature::verify',       // C++ verify (2 overloads: byte[] / ptr+len)
  'DsfSha1WithDsaSignature::sign',
  'Java_com_tridium_dsf_provider_spi_DsfSha1WithDsaSignatureSpi_engineVerify0___3B',
  'Java_com_tridium_dsf_provider_spi_DsfSha1WithDsaSignatureSpi_engineVerify0___3BII'
];

function hookByName(modName, baseName) {
  var m = null;
  try { m = Process.getModuleByName(modName); } catch (e) {}
  if (m === null) { send({ev:'skip', mod: modName, fn: baseName, why:'module not loaded'}); return; }
  // 17.x: module object carries enumerateSymbols()/findExportByName via methods
  var found = [];
  var syms = m.enumerateSymbols();
  for (var i = 0; i < syms.length; i++) {
    var s = syms[i];
    if ((s.name || '') === baseName) found.push(s.address);
  }
  if (found.length === 0) { send({ev:'skip', mod: modName, fn: baseName, why:'symbol not found'}); return; }
  send({ev:'hook', mod: modName, fn: baseName, addrs: found.map(function (a) { return a.toString(); })});
  found.forEach(function (addr) {
    Interceptor.attach(addr, {
      onEnter: function (args) {
        var k = modName + '::' + baseName;
        hits[k] = (hits[k] || 0) + 1;
        send({ev:'call', fn: k, n: hits[k], ra: (typeof this !== 'undefined') ? '' + this.returnAddress : ''});
      },
      onLeave: function (retval) {
        var k = modName + '::' + baseName;
        var v = -1;
        try { v = retval.toUInt32(); } catch (e) {}
        send({ev:'ret', fn: k, n: hits[k], val: v});
        if (RET_MODE === 'force') {
          retval.replace(FORCE_VAL);
          send({ev:'forced', fn: k, val: FORCE_VAL});
        }
      }
    });
  });
}

wanted.forEach(function (name) { hookByName('dsfspi.dll', name); });
"""

JS = JS.replace("%MODE%", repr(MODE)).replace("%FORCE%", str(FORCE_VAL))


def main():
    print(f"[run] mode={MODE} force={FORCE_VAL}", flush=True)
    device = frida.get_local_device()
    pid = device.spawn([NRE] + ARGS, cwd=CWD, stdio="inherit")
    print(f"[run] spawned pid={pid}", flush=True)
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
    print("[run] resumed; waiting for nre to exit", flush=True)

    deadline = time.time() + TIMEOUT_S
    while time.time() < deadline:
        try:
            alive = any(p.pid == pid for p in device.enumerate_processes())
            if not alive:
                break
        except Exception:
            break
        time.sleep(0.5)
    else:
        print("[run] TIMEOUT waiting for nre", flush=True)

    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
