# Block 449 — The actrld native poller and poll-health machine (gaps B448-G2 + B448-G1): platNrio spawns /proc/boot/actrld, drives /dev/ser2 @115200 over JNI, and marks a device down after 3 failed pings

**Focus:** base corpus (field-I/O drivers) ∩ platform-native. Closes **B448-G2** and **B448-G1**. Continues [B448]; refines [B448] §448.4.

**Origin:** grandchild gaps from [B448] — (G2) the full platform service behind `actrld` and how a `Trunk` maps to a serial node; (G1) the message `status` semantics and the poll retry/health state machine.

**Scope:** the `platNrio` platform service (Qnx + Atlas impls), the native `actrld` process, the JNI surface, the trunk↔serial mapping, and the device-down health counter. NOT: the C internals of `libplatnrio`/`actrld` (native binary, not decompiled here).

**Sources:**
- **FUENTE 1 (corpus):** [B448] §448.1/§448.4 (baud 115200, `maxFailsUntilDown`=3, actrld polls host-side), platform-native focus ([B124]–[B130], [B379]–[B385]: JACE = QNX; JNI/native daemons).
- **FUENTE 2 (niagara-help):** `[CERT-doc]` `aActrldToNrio` (actrld = low-level daemon on the host, separate from the station).
- **FUENTE 3 (code):** `[CERT]` `platNrio-rt/decompiled/.../BNrioPlatformServiceQnx.java`, `BNrioPlatformServiceAtlas.java`, `BNrioPlatformService.java`; `nrio-rt/.../BNrioDevice.java`, `BNrioNetwork.java`. (Read from `decompiled/` — the `vineflower/` copy mangles the identifiers to `n`.)

---

## 449.1 — actrld is a spawned native QNX process, not just an RPC

The `platNrio` module carries a per-platform `BNrioPlatformService` with two concrete impls — **`BNrioPlatformServiceQnx`** (JACE-7/QNX) and **`BNrioPlatformServiceAtlas`** (JACE-8000). On init the service: `[CERT]` (`BNrioPlatformServiceQnx.java:107-140`)

1. **Loads the native library** `libplatnrio` (log: "Loading platnrio native library"). `[CERT]`
2. **Gates on the device node** — checks `new File("/dev/actrl1")` exists before proceeding. `[CERT]`
3. **Spawns the native daemon**: `new ProcessBuilder("/proc/boot/actrld", "-n", String.valueOf(numTrunks)).start()`, holding the handle in a `Process actrld` field; `stop()` calls `actrld.destroy()`. `[CERT]`

So `actrld` is a **native binary in the QNX boot image** (`/proc/boot/actrld`), launched with `-n <numTrunks>` — one process serving all trunks. This is the daemon `aActrldToNrio` describes; the corpus now has its path and launch. `[CERT]`+`[CERT-doc]`

## 449.2 — Trunk ↔ serial node, and the JNI control surface

The platform service exposes native (JNI) entry points into `libplatnrio`: `[CERT]` (`BNrioPlatformServiceQnx.java:229-237`, `BNrioPlatformServiceAtlas.java:336-355`)

```
native int  open0(int handle)
native void close0(int handle)
native void setPortParams0(int handle, String comPort, int baudrate)
native void discover0(int handle, Vector<byte[]> uids)
native void enablePolling0(int handle, int addr)
native void disablePolling0(int handle, int addr)
```

Wrapped by `open/close/setPortParams/discover/enablePolling(handle, addr)/disablePolling`. Key bindings: `[CERT]`
- **Serial node**: on Atlas, `setPortParams(testHandle, "/dev/ser2", 115200)` — so the JACE-8000 **COM2 = `/dev/ser2`** at **115200 baud**, confirming [B448] §448.1 at the OS layer. (COM1 ↔ `/dev/ser1` by symmetry `[INFER]`.)
- **Discover**: `discover0(handle, Vector<byte[]>)` fills a vector of raw 6-byte **UIDs** — the native side does the broadcast query; the Java driver then assigns logical addresses ([B448] §448.3).
- **Per-address polling**: `enablePolling0(handle, addr)` / `disablePolling0(handle, addr)` turn polling on/off for one module address — the fine-grained control under the Thrift `enableActrld`/`disableActrld` façade seen in [B448] §448.4.
- **Stats readback**: `new ProcessBuilder("/proc/boot/cat", "/dev/actrl" + trunk).start()` — the driver reads polling statistics from the **`/dev/actrl<trunk>`** device node (one node per trunk), matching `aActrldToNrio`'s "memory-compare of IoStatus" living in the daemon. `[CERT]`

Test/diagnostic Niagara actions on the service: `openTest`, `configTest`, `discoverTest`, `setLogicalAddressTest`, `pingTest`, `enablePollingTest`, `disablePollingTest`, `waitForStatusChangeTest`. `[CERT]` (`BNrioPlatformServiceQnx.java:42`)

**Refines [B448] §448.4:** the station→platform control is a *two-layer* path — a platform-service RPC (`enableActrld`/`disableActrld`, Thrift) on top of a native implementation that spawns `/proc/boot/actrld` and drives the bus via JNI + `/dev/ser2`. The station never opens the serial port itself; the platform daemon + actrld own it.

## 449.3 — Poll-health machine (B448-G1): 3 strikes = down

Per-device health is a **ping-failure counter**, not a status-code table: `[CERT]` (`BNrioDevice.java`)
- Each failed ping does `++pingFailCount`; when `pingFailCount > maxFailsUntilDown` (network property `nUntilDown`, **default 3**, [B448] §448.1) the device is marked **down** and the counter is clamped at the max.
- `isDown()` ≡ `pingFailCount == maxFailsUntilDown`; a successful ping resets it. This is the mechanism behind the doc's "device down" alarm and the `monitor` 30 s ping ([B448] §448.1). `[CERT]`

**Message-level status:** `NrioMessage.isSuccess()` ≡ `status == 0`; any non-zero `status` byte is a module-reported error. The firmware-download path adds an explicit **retry loop** — `sendDownLoadMessage(request, retries)` re-sends up to `retries` times on error before failing the job ([B447]). `[CERT]` (`BNrioNetwork.sendDownLoadMessage`, `NrioMessage.isSuccess`)

A per-code error table is **not** enumerated in the driver — beyond `0 = OK` the specific non-zero codes are interpreted module-side, so B448-G1 closes on the *health/retry* mechanics with the status-code enumeration named as out-of-reach from this side.

---

## 449.4 — Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | actrld = native binary `/proc/boot/actrld`, spawned by platNrio via ProcessBuilder with `-n <numTrunks>` | `[CERT]` | `BNrioPlatformServiceQnx.java:126` |
| 2 | Service loads `libplatnrio`; gates on `/dev/actrl1` existing before spawning | `[CERT]` | `BNrioPlatformServiceQnx.java:107-121` |
| 3 | Two platform impls: Qnx (JACE-7) and Atlas (JACE-8000) | `[CERT]` | class names in `platNrio` |
| 4 | JNI surface: open0/close0/setPortParams0/discover0/enablePolling0/disablePolling0 | `[CERT]` | `BNrioPlatformServiceQnx.java:229-237` |
| 5 | JACE-8000 COM2 = `/dev/ser2` @115200 (native setPortParams) | `[CERT]` | `BNrioPlatformServiceAtlas.java:355` |
| 6 | discover0 returns Vector of 6-byte UIDs; enable/disablePolling0 per module address | `[CERT]` | `BNrioPlatformServiceQnx.java:178-186,235` |
| 7 | Poll stats read from `/dev/actrl<trunk>` (one node per trunk) | `[CERT]` | `BNrioPlatformServiceQnx.java:213` |
| 8 | Device down when `++pingFailCount > maxFailsUntilDown` (default 3), clamped; reset on success | `[CERT]` | `BNrioDevice.java`; `BNrioNetwork.nUntilDown` |
| 9 | Message OK ≡ `status==0`; firmware download retries up to N via `sendDownLoadMessage` | `[CERT]` | `NrioMessage.isSuccess`, `BNrioNetwork.sendDownLoadMessage` |

**Tally:** 9 claims — 8 `[CERT]` · 1 `[CERT]`+`[INFER]` (COM1↔ser1) · 0 unmarked. Refines (does not contradict) [B448] §448.4.

**Left out (named):** the C internals of `actrld`/`libplatnrio` (native binary — Ghidra-grade, requires-RE); the per-code non-zero `status` error table (module-side); the Thrift IDL field-by-field (low value — the method set is enable/disable/poll).

## 449.5 — Connections
- **Closes B448-G2 + B448-G1; refines [B448] §448.4** (two-layer control; adds `/dev/ser2`, `/proc/boot/actrld`, `/dev/actrl<trunk>`).
- **Joins platform-native focus** ([B124]–[B130], [B379]–[B385]): another JACE native daemon spawned from `/proc/boot` and driven by JNI — same pattern as the platform launchers/daemons documented there.
- **Confirms** [B445] §445.4 / [B448] §448.4: the station never touches RS-485; the native daemon owns `/dev/ser2`.

## 449.6 — Open gaps
- **B449-G1** — `actrld` / `libplatnrio` native RE (Ghidra): the actual polling loop, IoStatus memory-compare, and the non-zero `status` code table. Requires native RE (`platNrio` native lib + `/proc/boot/actrld` binary, not in the jar corpus).
