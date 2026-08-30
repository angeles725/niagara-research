# B679 — JACE-8000 `niagarad` platform daemon: a thin QNX-7.0 native launcher that starts the JVM running `com.tridium.niagarad.NiagaraDaemon`, and DROPS PRIVILEGES (`setgid`+`setuid` to the `niagarad` user, refuses to run as root) — the opposite of the Windows `plat.exe` that ran as LocalSystem ([Block 381]) (focus jace8000-qnx-native, QN3; §19 [CERT] + §14 vs B381)

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN3 (the platform daemon binary on QNX, :3011/:5011).
> **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` from the ARM ELF symbols + strings.
> **Sources:** `sources/probes/B672-jace8000-sd/qn3-niagarad-symbols.txt` · binary
> `local-sd-image/bin-arm/niagarad` (ELF32 ARM, sha256 `ecd2b73013f4d073a22fcd594f20bb44c7b64bb60c6dcda5ef0912a72f4eb609`; gitignored) ·
> `[CERT]` [Block 381] (Windows `plat.exe`), [Block 628] (daemon credential frame), [Block 460]/[Block 461]
> (platform daemon), [Block 678] (`libnjre` launcher).
>
> **Bottom line:** `niagarad` on the JACE is a **thin native launcher**, not the daemon logic itself. It links
> `libnjre.so` and calls `JavaLauncher::getInstance()` to start the JVM running the Java class
> **`com.tridium.niagarad.NiagaraDaemon`** — the platform-daemon protocol (:3011/:5011, HTTP Basic/Digest,
> [Block 628]) lives in Java, not in this binary. The native part's notable job is **privilege management**: it
> does `setgid` then `setuid` to the dedicated **`niagarad`** account (uid 200) and **refuses to start if it
> still holds root** (`"ERROR: root permissions gained, preventing startup"`). This is the **opposite** of the
> Windows `plat.exe`, which ran as **LocalSystem** ([Block 381]).

---

## §679.1 — Thin native launcher over a Java daemon `[CERT]`

`niagarad` (ELF32 ARM, QNX **7.0.0** per the `crt1S.S … branches/7.0.0/trunk` startup strings `[CERT]`) is
small. Its `NEEDED` set is `libnjre.so`, `libc++.so.1`, `libsocket.so.3`, `libdsfspi.so`, `libc.so.4`
`[CERT]`. Its only meaningful imported symbol is `JavaLauncher::getInstance()` from `libnjre.so`
`[CERT qn3-niagarad-symbols.txt]`, and the string table names the Java entry point
**`com/tridium/niagarad/NiagaraDaemon`** (source `niagaradExeQnx.cpp`) `[CERT]`. So the flow is:
`niagarad` (native) → `libnjre` `JavaLauncher` → JVM → `com.tridium.niagarad.NiagaraDaemon` (Java). The daemon
*behavior* — the :3011/:5011 listener, the HTTP Basic/Digest-MD5 + shared-secret credential frame ([Block 628]),
the platform command surface (`NativePlatformProvider`, [Block 678] §678.2) — is all in Java, reached through
JNI into `libnre.so`. This native binary just brings the VM up under the right identity.

## §679.2 — Privilege drop: runs as `niagarad`, refuses root `[CERT]` (§14 contrast with B381)

The native `main` performs a deliberate privilege drop `[CERT qn3-niagarad-symbols.txt]`:
```
ERROR: setgid failed for %s group, exiting: %s
ERROR: setuid failed for %s user, exiting: %s
ERROR: root permissions gained, preventing startup
```
It `setgid`s to a group and `setuid`s to a user (the `niagarad` account, uid/gid 200 from `/etc/passwd`,
[Block 674] §674.2), and it **aborts if it detects it still has root** after the drop. So on the JACE the
platform daemon runs **de-privileged** as a dedicated low-rights account, and a failure to drop is fatal (fail
closed).

**§14 contrast with the Windows Supervisor ([Block 381]):** `plat.exe` (the Windows platform daemon) runs as
**LocalSystem** (the highest local privilege, auto-start service). The JACE `niagarad` does the opposite — it
**refuses** elevated privilege and drops to `niagarad`. So the embedded controller's daemon has a *tighter*
privilege posture than the Windows supervisor's. (The `station` process runs as its own `station` account, uid
300 — [Block 674] — same de-privileging pattern.) `niagarad_admin` appears as a role/group string, the
admin-capable identity for platform operations.

## §679.3 — What is NOT here (correctly delegated) `[CERT]`

No port literals (`3011`/`5011`), no TLS/SSL strings, no listener logic in the native binary `[CERT — absent
in strings]`. That is expected: those belong to the Java `NiagaraDaemon` + the platform config, not the
launcher. The credential frame and TLS-only :5011 posture are already documented ([Block 628], [Block 657]/
[Block 468]); this block establishes only that the *native* entry point is a privilege-dropping JVM launcher.

## §679.4 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | niagarad NEEDED libnjre/libdsfspi/libsocket/libc++/libc.so.4; QNX 7.0.0 | [CERT] | readelf -d; crt startup strings |
| 2 | Launches JVM class com/tridium/niagarad/NiagaraDaemon via JavaLauncher::getInstance (libnjre) | [CERT] | nm -D; strings |
| 3 | Drops privileges: setgid+setuid to niagarad user; aborts if root retained | [CERT] | setgid/setuid/"root permissions gained" strings |
| 4 | §14 contrast: Windows plat.exe = LocalSystem (B381); JACE niagarad = de-privileged | [CERT] + [CERT] | §679.2; [Block 381] |
| 5 | No ports/TLS in the native binary (delegated to Java) | [CERT absent] | strings |

**Tally:** 5 claims — 5 [CERT] (incl. one [CERT-absent] negative). 0 unmarked.

## §679.5 — Connections

- **[Block 381]** — Windows `plat.exe` (LocalSystem); this is the QNX twin with the opposite privilege posture.
- **[Block 678]** — the `libnjre` `JavaLauncher` this daemon uses; `libnre` `NativePlatformProvider` it drives.
- **[Block 628]** — the daemon credential frame (HTTP Basic/Digest-MD5 + shared secret), Java side.
- **[Block 460]/[Block 461]** — the platform daemon (:3011/:5011) and its credential store.
- **[Block 674]** — the `niagarad`/`station` accounts (uid 200/300) this drops into.
