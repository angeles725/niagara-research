# spg10-frida — the three-way mirror toolkit (license + module + HostId)

Tooling built and used to answer "can a shim make the verifier return a self-consistent **everything
valid** view?" for Niagara N4.14.0.162 (Honeywell OEM `OptimizerSupervisor`). **Defensive-reimpl only**
(METHODOLOGY §19): instruments a DISPOSABLE `nre.exe`; never touches the live `station.exe`; redistributes
no vendor code.

> All paths relative to the install `C:\Honeywell\OptimizerSupervisor-N4.14.0.162`.
> SECRETS DISCIPLINE: Host IDs are shown as `Win-XXXX…` format only — no values, no credentials.

---

## 1. What each mirror is, and its status

| Mirror | Where it lives in code | Tool here | Status |
|---|---|---|---|
| **License signature** | `com.tridium.sys.license.LicenseUtil.verify(...)` (Java, `baja.jar`) | `javaagent/LicenseMirrorAgent.java` → `bin/LicenseMirrorAgent.jar` | ✅ **EXECUTED live** ([B528]) — tampered license flips `{invalid}`→`{valid}` |
| **Module signature** | `nre.dll::SignatureUtil::checkFileSignature` (native) | `nre_module_mirror2.py` (Frida native `Interceptor`) | ✅ **EXECUTED live** ([B524]) — forced return flips both ways (`FATAL failed signature check` vs clean) |
| **HostId** | **NATIVE fold** `nre.dll::NreWin32::getHostId` (NOT Java) — see §2 | `hostid_hook.py` (Frida, **log-only**) | ⚠ **hook point DEMONSTRATED** ([B535]); forcing NOT implemented (dual-use scope) |

So the **"complete" mirror is a combination**:
- `full-mirror-agent.jar` rewrites BOTH Java `LicenseUtil.verify` AND Java
  `NodeLockedLicenseManager$NodeLockedLicense.isLicenseHostIdValid()` → `return true`.
- **But** the HostId `moved file` gate is **native** (`common.dll`/`nre.dll`), so a Java-only rewrite
  does NOT unseat a wrong-host license — proven ([B534]). Native forcing = `hostid_hook.py` `force` mode
  (not yet written).

## 2. HostId — direct answer to "¿está el hostId?"

**No hay un "JAR de HostId"** — el HostId **no es Java**. Es un fold nativo de 8 bytes (`nre.dll`) que
combina 4 inputs: hidden key (`hid3`) + RegisteredOwner + product id + C: volume serial ([B424]/[B535]).
Lo que existe para HostId es `hostid_hook.py`:

```bash
hostid_hook.py log     # ✅ probado: resuelve NreWin32::getHostId, imprime el HostId real
hostid_hook.py force   # ❌ NO implementado — quedaría el forzado del valor (decisión de alcance)
```

## 3. The JARs (bin/) — already compiled, ready to use

| JAR | Premain-Class | Re-verifiable with |
|---|---|---|
| `bin/LicenseMirrorAgent.jar` | `spg10.LicenseMirrorAgent` | `unzip -p bin/LicenseMirrorAgent.jar META-INF/MANIFEST.MF` |
| `bin/full-mirror-agent.jar` | `spg10.FullMirrorAgent` | (same) |
| `bin/ProviderOrderProbe.jar` | `spg10.ProviderOrderProbe` | (same) |

Use against a disposable process:

```bash
nre.exe -@javaagent:C:/path/to/full-mirror-agent.jar=force -licenses
```

## 4. Rebuild everything (source of truth = the .java files)

```bash
./build-all.sh          # recompiles the 3 JARs against the install's asm-9.6.jar
```

Build prerequisites: the install's own `jre/bin/javac.exe` and `bin/ext/asm-9.6.jar` (both present in the
install); `zip` for packaging.

## 5. File map

- `javaagent/*.java` — the source for the 3 agents (license / full / provider-probe).
- `bin/*.jar` — compiled agents (built from the `.java` by `build-all.sh`).
- `*.py` — Frida driver scripts (native module mirror, hostid hook, census/diagnostics).
- `sources/probes/B524-…/` and `B528-…/` (corpus) — preserved run logs + RUN notes.

See the corpus blocks for the full evidence: [B524] (module mirror + SP-G10), [B528] (license mirror),
[B534] (HostId negative), [B535] (fold re-anchor), [B533] (persistence vector).
