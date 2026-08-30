# B681 — JACE-8000 `libcommon.so` (shared native runtime) + `libbacnet.so`: libcommon carries the hardware EngineWatchdog (shmem-based), the network config layer (NetCfgIoPkt: DHCP/IPv6) and i18n — and it links **OpenSSL `libcrypto.so.2`**, a SECOND crypto stack alongside the Mocana in `libdsfspi` ([Block 677]); libbacnet is the **BACnet/Ethernet** link adapter (`/dev/bn-%s`), complementing the MS/TP driver ([Block 680]) (focus jace8000-qnx-native, QN5; §19 [CERT])

> **Focus:** `jace8000-qnx-native` (§16). **Gap closed:** QN5 (the common native runtime + BACnet native).
> **Phase:** static RE, READ-ONLY. **Marker:** `[CERT]` from ARM ELF symbols/strings.
> **Sources:** `sources/probes/B672-jace8000-sd/qn5-common-bacnet-symbols.txt` · binaries
> `local-sd-image/bin-arm/{libcommon,libbacnet}.so` (gitignored; sha256 in the probe) ·
> `[CERT]` [Block 677] (Mocana crypto), [Block 680] (field-bus drivers), [Block 133] (BACnet wire).
>
> **Bottom line:** `libcommon.so` is the big shared native runtime the launchers/daemon link (`nre`,
> `niagarad`, `libnre` all `NEEDED` it). It provides the **hardware watchdog** (`EngineWatchdog` over shared
> memory), the **network-config plane** (`NetCfgIoPkt`: DHCP + IPv6 enable/auto), and i18n `MessageBundle` —
> and it links **OpenSSL `libcrypto.so.2`**. That means the JACE runs **two crypto stacks**: Mocana NanoCrypto
> (static, in `libdsfspi`, for the Niagara JCE provider — [Block 677]) and **OpenSSL libcrypto** (shared, in
> `libcommon`, for platform/TLS use). `libbacnet.so` is the **BACnet/Ethernet** link adapter
> (`BacnetEthernetAdapterQnx`, device `/dev/bn-%s`) — the raw-Ethernet BACnet datalink, alongside the MS/TP
> driver of [Block 680].

---

## §681.1 — `libcommon.so`: the shared runtime `[CERT]`

`NEEDED`: `libsocket.so.3`, `libc++.so.1`, **`libcrypto.so.2`**, `libc.so.4` `[CERT]`. It is the largest of
the platform natives (1885 functions) and is linked by `nre`/`niagarad`/`libnre`. Subsystems visible in its
symbols/strings `[CERT]`:
- **`EngineWatchdog` / `EngineWatchdogQnx`** — the station/engine hardware watchdog, coordinated through
  **shared memory** (`shmemCreate`, `shmemUnlock`, `addWatchdog`, `getWatchdog`, `unloadList`/`unlockList`).
  This is the watchdog that resets the controller if the engine hangs (the JACE-9000 live session saw the
  hardware watchdog, [Block 665]; here it is the JACE-8000 native implementation).
- **`NetCfgIoPkt`** — the network configuration plane: `isDhcpEnabled`/`setDhcpEnabled`, `isIpv6Enabled`/
  `setIpv6Enabled`, `isIpv6AutoEnabled`/`setIpv6AutoEnabled`, `isEnabled`/`setEnabled`, `canUseDhcp`, plus a
  `dhcpMonitorThreadEntry` thread and `/tmp/dhcp.conf`. So DHCP/IPv6 adapter config is done natively here
  (paired with the `-Dniagara.dhcpd.*` launcher props of [Block 678]).
- **`MessageBundle`** — localized message bundles (i18n) at the native layer; `/sys/info/version` (QNX system
  version query).

## §681.2 — Two crypto stacks on the JACE `[CERT]` (security-relevant, feeds QN8)

`libcommon` links **OpenSSL `libcrypto.so.2`** `[CERT]`, whereas `libdsfspi` statically embeds **Mocana
NanoCrypto** ([Block 677]). So the controller carries **two independent crypto implementations**:
- **Mocana NanoCrypto** (static, `libdsfspi`) → the Niagara **DSF JCE provider** (AES-256-CBC keyring, CTR-DRBG,
  DSA/RSA) — the FIPS-gated Java crypto ([Block 380]/[Block 677]).
- **OpenSSL libcrypto.so.2** (shared, `libcommon`) → platform-level crypto (likely TLS transport for the
  daemon/station HTTPS on :5011/:443, and general native use).
This dual-stack is worth noting for the security verdict (QN8): patch surface and FIPS-boundary questions apply
to BOTH, and the OpenSSL `libcrypto.so.2` version is its own vulnerability-tracking concern `[INFER — exact
OpenSSL version + whether TLS actually routes through it are a follow-up, QN5-G1]`.

## §681.3 — `libbacnet.so`: BACnet/Ethernet adapter `[CERT]`

`NEEDED`: `libc++.so.1`, `libsocket.so.3`, `libc.so.4` `[CERT]`. JNI class
`com.tridium.platBacnet.BacnetEthernetAdapterQnx` (`BacnetEthernetAdapterQnx.cpp`), device node **`/dev/bn-%s`**
`[CERT]`. Entry points: `bacnetOpen0`/`bacnetClose0`, `bacnetRead0`/`bacnetWrite0`, `getAddress0`. This is the
**BACnet-over-Ethernet (ISO 8802-3) datalink** adapter — raw Ethernet frames, distinct from BACnet/IP (Annex J,
which rides UDP through `libsocket`) and from BACnet MS/TP (`libplatmstp`, [Block 680]). So the JACE's native
BACnet datalinks are: **MS/TP** (RS-485, `libplatmstp`) + **Ethernet** (`libbacnet`); BACnet/IP is handled in
the Java `bacnet` driver over the normal socket stack ([Block 133]).

## §681.4 — Self-verify

| # | Claim | Marker | Cite |
|---|---|---|---|
| 1 | libcommon NEEDED libsocket/libc++/**libcrypto.so.2**/libc.so.4 | [CERT] | readelf -d libcommon.so |
| 2 | libcommon provides EngineWatchdog (shmem-based HW watchdog) | [CERT] | EngineWatchdog* symbols |
| 3 | libcommon provides NetCfgIoPkt (DHCP/IPv6 config) + dhcpMonitor thread | [CERT] | NetCfgIoPkt* symbols; /tmp/dhcp.conf |
| 4 | JACE runs TWO crypto stacks: Mocana (libdsfspi) + OpenSSL libcrypto (libcommon) | [CERT] + [CERT] | §681.2; [Block 677] |
| 5 | libbacnet = BACnet/Ethernet adapter (BacnetEthernetAdapterQnx, /dev/bn-%s) | [CERT] | nm -D; strings |
| 6 | JACE native BACnet datalinks = MS/TP + Ethernet; BACnet/IP via Java+sockets | [CERT] + [INFER] | §681.3; [Block 680]/[Block 133] |

**Tally:** 6 claims — 5 [CERT], plus [INFER] items flagged inline (OpenSSL version/TLS routing → QN5-G1;
BACnet/IP path). 0 unmarked.

## §681.5 — Connections

- **[Block 677]** — Mocana crypto (libdsfspi); §681.2 is the second (OpenSSL) stack.
- **[Block 678]/[Block 679]** — `nre`/`niagarad` link `libcommon`; the `-Dniagara.dhcpd.*` props pair with NetCfgIoPkt.
- **[Block 680]** — the MS/TP field-bus driver; `libbacnet` is the Ethernet sibling.
- **[Block 133]** — BACnet wire; BACnet/IP is the Java+socket path, not these natives.
- **[Block 665]** — the JACE hardware watchdog (live JACE-9000); `EngineWatchdog` is the native side.
