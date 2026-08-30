# B683 — JACE-8000 `libpower.so` (power/UPS monitoring) + `station` launcher: PowerdQnx exposes primary-power/UPS/battery state (`/dev/powerd/batteryState`), and the `station` binary launches `com.tridium.sys.station.Station` as the de-privileged `station` user (uid 300, `station_owners`), refusing root — same fail-closed pattern as `niagarad` ([Block 679]) (focus jace8000-qnx-native, QN7; §19 [CERT])

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN7 (power subsystem + the station launcher).
> **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]`.
> **Sources:** `sources/probes/B672-jace8000-sd/qn7-power-station-symbols.txt` · binaries
> `local-sd-image/bin-arm/{libpower.so,station}` (gitignored; sha256 in the probe) · `[CERT]` [Block 679]
> (niagarad), [Block 674] (accounts), [Block 681] (watchdog).
>
> **Bottom line:** `libpower.so` (`PowerdQnx`, device `/dev/powerd/batteryState`) is the JACE's **power / UPS /
> battery monitor**: it reports whether **primary power is present**, **UPS power/battery** state, and the
> onboard **battery** (and a second battery) charge/test state — the inputs behind power-loss handling and
> graceful shutdown. The `station` binary is the **station launcher** (`stationExeQnx.cpp`): it boots the JVM
> into `com.tridium.sys.station.Station` (via `com.tridium.nre.bootstrap.Bootstrap`) and, exactly like
> `niagarad` ([Block 679]), **drops privileges** — `setgid`+`setuid` to the dedicated **`station`** account
> (uid 300, group `station_owners`, [Block 674]) and **refuses to run as root**.

---

## §683.1 — `libpower.so` = PowerdQnx (power/UPS/battery) `[CERT]`

JNI class `com.tridium.platPower.PowerdQnx` (`PowerdQnx.cpp`), device node **`/dev/powerd/batteryState`**,
`NEEDED` `libc++`+`libnre`+`libc.so.4` `[CERT]`. Entry points `[CERT qn7-power-station-symbols.txt]`:
- Primary power: `isPrimaryPresent0`.
- UPS: `isUpsPowerPresent0`, `isUpsBatteryGood0`.
- Battery (dual): `getBatteryState0`, `getBatteryCharge0`, `getBatteryChargeTime0`, `getBatteryTestTime0`,
  `getBattery2TestTime0`, `isBatteryGood0`, `isBattery2Good0`, `testBattery0`; `open0`/`close0`.
So the JACE natively monitors **mains/primary presence + UPS + one or two backup batteries** through a QNX
`powerd` resource manager. These are the signals that drive power-fail detection and orderly shutdown (the
"power" side that pairs with the `EngineWatchdog` of [Block 681]).

## §683.2 — `station` = the de-privileged station launcher `[CERT]`

The `station` binary (`stationExeQnx.cpp`), `NEEDED` `libdsfspi`+`libnre`+`libcommon`+`libsocket`+`libc.so.4`
`[CERT]`. It boots the JVM into **`com.tridium.sys.station.Station`** through the bootstrap class
**`com.tridium.nre.bootstrap.Bootstrap`** `[CERT]`, running stations out of `/home/niagara/stations`
([Block 674]). Its privilege handling is identical to `niagarad` ([Block 679]) `[CERT]`:
```
setgid / setgid failed ; setuid / setuid failed
ERROR: root permissions gained, preventing startup
```
It drops to the **`station`** user (uid 300) / **`station_owners`** group and aborts if root is retained
(fail-closed). So on the JACE the **two long-running Niagara processes are both de-privileged**: `niagarad` →
`niagarad` (uid 200), `station` → `station` (uid 300). Neither runs as root — a tighter posture than the
Windows Supervisor (`plat.exe` = LocalSystem, [Block 381]).

## §683.3 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | libpower = PowerdQnx, device /dev/powerd/batteryState | [CERT] | qn7 evidence |
| 2 | Reports primary present, UPS power/battery, battery(2) charge/test | [CERT] | isPrimaryPresent0/isUps*/getBattery* |
| 3 | station launches com.tridium.sys.station.Station via nre.bootstrap.Bootstrap | [CERT] | strings |
| 4 | station drops privileges to station uid 300 / station_owners, refuses root | [CERT] | setuid/setgid + "root permissions gained" |
| 5 | Both niagarad and station run de-privileged (vs Windows LocalSystem) | [CERT] | §683.2; [Block 679]/[Block 381] |

**Tally:** 5 claims — 5 [CERT]. 0 unmarked.

## §683.4 — Connections

- **[Block 679]** — `niagarad` (same de-privileging pattern); the two daemons complete the process model.
- **[Block 681]** — `EngineWatchdog`; power (this block) + watchdog are the JACE's reliability pair.
- **[Block 674]** — the `station`/`niagarad` accounts (uid 300/200) these drop into.
- **[Block 381]** — Windows `plat.exe` LocalSystem; contrast with the JACE's de-privileged daemons.
