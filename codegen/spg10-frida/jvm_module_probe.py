#!/usr/bin/env python3
"""List which JVM/JNI modules nre.exe actually loads (post-boot), and probe JNI_GetCreatedJavaVMs."""
import time
import frida

JS = r"""
'use strict';
var mods = [];
Process.enumerateModules().forEach(function (m) {
  var n = (m.name || '').toLowerCase();
  if (n.indexOf('jvm') !== -1 || n.indexOf('java') !== -1 || n.indexOf('nre') !== -1 || n.indexOf('common') !== -1) {
    mods.push({name: m.name, base: m.base.toString(), size: m.size});
  }
});
send({ev:'jvm-mods', mods: mods});

// does jvm.dll export JNI_GetCreatedJavaVMs at runtime?
var g = null;
try { g = Process.getModuleByName('jvm.dll').getGlobalExportByName('JNI_GetCreatedJavaVMs'); } catch (e) {}
send({ev:'jvm-export', found: g !== null, addr: g ? g.toString() : 'null'});

// try calling JNI_GetCreatedJavaVMs(NULL, 0, &count) to see if VMs exist
if (g !== null) {
  try {
    var getCreated = new NativeFunction(g, 'int', ['pointer', 'int', 'pointer']);
    var n = Memory.alloc(4);
    var r = getCreated(NULL, 0, n);
    send({ev:'getCreated', ret: r, count: n.readInt()});
  } catch (e) {
    send({ev:'getCreated-err', err: '' + e});
  }
}
"""


def main():
    print("[run] jvm-module-probe", flush=True)
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
    # wait past the JVM boot, then re-inspect
    time.sleep(8)

    # re-check modules AFTER boot by loading a second script
    JS2 = r"""
    'use strict';
    var mods = [];
    Process.enumerateModules().forEach(function (m) {
      var n = (m.name || '').toLowerCase();
      if (n.indexOf('jvm') !== -1 || n.indexOf('java') !== -1) mods.push({name: m.name, base: m.base.toString(), size: m.size});
    });
    send({ev:'post-boot-jvm-mods', mods: mods});
    var g = null;
    try { g = Process.getModuleByName('jvm.dll').getGlobalExportByName('JNI_GetCreatedJavaVMs'); } catch (e) {}
    send({ev:'post-boot-jvm-export', found: g !== null, addr: g ? g.toString() : 'null'});
    if (g !== null) {
      var getCreated = new NativeFunction(g, 'int', ['pointer', 'int', 'pointer']);
      var n = Memory.alloc(4);
      var r = getCreated(NULL, 0, n);
      send({ev:'post-boot-getCreated', ret: r, count: n.readInt()});
    }
    """
    script2 = session.create_script(JS2)
    script2.on("message", on_message)
    script2.load()
    time.sleep(3)
    try:
        session.detach()
    except Exception:
        pass
    print("[run] done", flush=True)


if __name__ == "__main__":
    main()
