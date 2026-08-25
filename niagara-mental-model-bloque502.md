# Block 502 — `framework-drivers` FD7: `opc` (classic OPC DA) — the Java driver-component layer over the `opc.dll` JNI shim (network→device→group→item, ItemID→serverHandle), local-ProgID vs remote-CLSID/DCOM activation, and its Windows-delegated security (NT `changeUser` / private `logon`, creds held as `BPassword`)

> **Focus:** `framework-drivers`, gap **FD7** — classic OPC DA (OLE for Process Control, Data Access; COM/DCOM,
> Windows-only). NOT OPC UA (FD1–FD3). Measured **53** distinct `com.tridium.opc` classes in `-rt` (the audit's
> "64" counted `-wb`/dupes; RE-MEASURED). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/opc/opc-rt/vineflower/…`. FUENTE 1 — **[B127]/[B132]** cover the NATIVE side
> of this same JNI boundary (`opc.dll`/`opcproxy`, the COM/DCOM edge); this block is the uncovered **Java** half,
> NOT a re-derivation of those. FUENTE 2 — not consulted (decompilation gap). Evidence delegated to a `sonnet`
> sweep; ALL load-bearing file:line RE-VERIFIED inline (vineflower; offsets discarded).

## §502.1 — Component tree `[CERT]`

Standard Baja driver hierarchy (`com.tridium.opc.client`):

| Class | Base | Role |
|---|---|---|
| `BOpcNetwork` | `BDeviceNetwork` | network root; two Worker/CoalesceQueue pairs (rd/wr), thread pool, license gate; calls `OpcEnv.initializeEnv()` at start (`BOpcNetwork.java:71`) |
| `BOpcDevice` | `BDevice` | abstract device = one OPC server identity: `address` (default `localhost`), `programId`, `classId`, `local` flag (`BOpcDevice.java:69`) |
| `BOpcDaClient` | `BOpcDevice` | concrete device; owns the live `OpcDaServer`, a `BOpcPointDeviceExt`, and a `BOpcDASecurity` child (`BOpcDaClient.java:122`) |
| `BOpcPointDeviceExt` | `BPointDeviceExt` | owns the OPC **Group** (`groupName` default `tridium`, `updateRate` 1000 ms, `percentDeadband`, `batchLimit` 500); drives subscribe/read/write batches |
| `BOpcProxyExt` | `BProxyExt` (impl `BIOpcPollable`) | per-point proxy; `id` = the OPC **ItemID** string (e.g. `Channel1.Device1.Tag1`), plus `serverHandle` (int from the server), `opcDataType`/`opcQuality` (`BOpcProxyExt.java:99`) |

**ItemID → point:** the `id` string is passed verbatim to `OpcItemMgt.addItems()`; the server returns a
`serverHandle` int stored on the proxy and used for every subsequent read/write; OPC quality bits are decoded to
Baja `BStatus`.

## §502.2 — The JNI boundary: the Java side of B127/B132 `[CERT]`

`OpcEnv` loads the native lib and does COM init:
```
OpcEnv.java:256  System.loadLibrary("opc");        // opc.dll (AccessController.doPrivileged)
OpcEnv.java:249  private static native void initEnv();     // CoInitialize(Ex) equiv
OpcEnv.java:251  private static native void initThread();  // per-thread COM init (ThreadLocal-gated)
```
Every COM interface is a Java class extending `ComObjectClient`, which holds the opaque COM pointer as
`long peer` and carries the IUnknown edge: `native long query(long,String iid)` (QueryInterface) + `native void
release(long)` (`ComObjectClient.java:61-63`). The whole classic-OPC surface crosses as native methods, e.g.
`OpcDaServer`: `createLocalServer(ProgID)` `:97`, `createRemoteServer(CLSID,host)` `:99` (DCOM), `addGroup(…)`
`:95`; plus `OpcItemMgt.addItems/removeItems/validateItems`, `OpcSyncIo.read/writeArray/writeNumeric`,
`OpcAsyncIo2.advise/readAsync/writeAsync`. `[INFER]`: this is exactly the Java caller of the native `opcproxy`
COM plumbing that [B127]/[B132] documented from the DLL side — the two blocks meet at `System.loadLibrary("opc")`.
**FD7 is the only framework-drivers module that is Windows-only / JNI-bound** (the rest are pure Java).

## §502.3 — OPC DA session model `[CERT]`

`BOpcDaClient.doAttach()`: if `local` → `OpcDaServer.newServer(ProgID)` → native `createLocalServer`; else →
`OpcDaServer.newServer(CLSID, address)` → native `createRemoteServer(CLSID, host)` (**remote DCOM activation**).
Then per point-ext: `addGroup(name, active, updateRate, clientHandle, timebias, deadband, localeId)` returns the
group COM pointer + a server-revised update rate. Items are added in batches ≤500 (`addItems(ids[], handles[],
active[], datatypes[])`), server handles stored back on each proxy. **Reads:** default `cov` (async callback via
`OpcAsyncIo2.advise` → `OnDataChange`), or sync cache read (`OpcSyncIo.read(handles, cache=true)`). **Writes:**
sync (`writeArray`/`writeBoolean`/`writeNumeric`/`writeString`) or async (`writeAsync`), by `writeMode`.

## §502.4 — Browse & discovery `[CERT]`

Full `IOPCBrowseServerAddressSpace` (IID `39c13a4f-…`) via `OpcBrowseServerAddressSpace`: `goRoot`/`goDown`/
`listBranches`/`listItems` natives, exposed as `browse(branch, flat, mgt)` / `getGroups` / `getItems` /
`queryOrganization` (hierarchical vs flat). `BOpcPointDiscoveryJob` materializes browsed items as `BOpcProxyExt`;
`BOpcDeviceDiscoveryJob` enumerates servers on a host via `OpcServerList2.discoverServersInNetwork()`.

## §502.5 — Security: two COM interfaces, auth delegated to Windows/DCOM `[CERT]`

Credentials live in a `BOpcDASecurity` child: `loginName` (String) + `loginPassword` (**`BPassword`**,
`BOpcDASecurity.java:36`), fetched with `AccessController.doPrivileged(getLoginPassword()::getValue)` at connect.
Two paths, chosen by the `privateSecurity` flag:
- **NT security** (`IOPCSecurityNT`): `OpcNTSecurity.changeUser(peer, user, pass)` native (`:25`) — sets the
  Windows impersonation token for the DCOM connection.
- **Private security** (`IOPCSecurityPrivate`): `OpcPrivateSecurity.logon(peer, name, pwd)` native (`:23`) —
  server-proprietary logon.

If the server advertises neither interface, the security slot is hidden. `[INFER]`: **all authentication is at the
Windows/DCOM layer** — the Java code only forwards a `BPassword`-held credential into a COM security interface; it
adds no application-layer auth or transport encryption. Remote connections are raw DCOM (`createRemoteServer(CLSID,
host)`); DCOM identity/impersonation/firewalling are Windows configuration, entirely outside this module. This is
the classic-OPC security posture the industry has long flagged (DCOM hardening is the deployment's burden) — a
protocol/OS property, not a Tridium defect, but a sharp contrast with the OPC-UA drivers (FD1–FD3) that carry
their own TLS/cert/token security in-band.

## §502.6 — License gate `[CERT]`

`getFeature("tridium", "opc")` (`BOpcNetwork.java:324`), the `BDeviceNetwork` license hook. Feature =
**`tridium:opc`**; sole license site in the module.

## §502.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | network/device/client/group/item tree; 53 com.tridium.opc classes; ItemID→serverHandle | `[CERT]` | `BOpcNetwork.java:71`, `BOpcDevice.java:69`, `BOpcProxyExt.java:99`; find=53 | PASS |
| 2 | JNI: System.loadLibrary("opc") + native COM methods (createLocal/RemoteServer, addGroup, query/release) | `[CERT]` | `OpcEnv.java:256`; `OpcDaServer.java:95-99`; `ComObjectClient.java:61-63` | PASS |
| 3 | Java side of the B127/B132 native opc.dll boundary; only Windows-only/JNI module in FD | `[CERT]`+`[INFER]` | loadLibrary + native decls | PASS |
| 4 | session: local ProgID vs remote CLSID+host (DCOM); groups + batched items (≤500); sync/async IO | `[CERT]` | `OpcDaServer.java:57,97,99`; addGroup/addItems | PASS |
| 5 | full IOPCBrowseServerAddressSpace + server discovery | `[CERT]` | `OpcBrowseServerAddressSpace`; `OpcServerList2` | PASS |
| 6 | security: NT changeUser + private logon natives; creds=BPassword via doPrivileged; auth = Windows/DCOM | `[CERT]`+`[INFER]` | `OpcNTSecurity.java:25`; `OpcPrivateSecurity.java:23`; `BOpcDASecurity.java:36` | PASS |
| 7 | license `tridium:opc` | `[CERT]` | `BOpcNetwork.java:324` | PASS |

**Tally:** 7 claims — 5 `[CERT]` load-bearing + 2 `[INFER]` (native-boundary identity, Windows-delegated auth) on
cited code. Block TYPE = **EVIDENCE**; ratio low, FD7 CLOSED. All load-bearing tokens re-verified inline.

## §502.8 — Connections & focus status

- **Meets [B127]/[B132] at the JNI line** — those documented `opc.dll`/`opcproxy` natively; this is the Java caller
  of the same COM boundary (`System.loadLibrary("opc")`). No re-derivation; the two halves now connect.
- **Security contrast:** OPC UA (FD1–FD3) carries in-band TLS/cert/token; classic OPC DA (FD7) has **no in-band
  security at all** — it rides Windows DCOM, with `BPassword` only forwarding a credential into a COM security
  interface. Feed to [B398]/[B490]: an OPC-DA remote device implies DCOM exposure whose hardening lives entirely
  outside Niagara.
- License model matches the focus (`getFeature("tridium","opc")`).
- **Focus status:** `framework-drivers` 7/10 (FD1–FD7 closed). NEXT = FD9 `knxnetIp` (then FD10, FD8).
