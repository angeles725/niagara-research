# Block 127 — Native driver DLLs: how the Java drivers reach the wire (`lon.dll` · `opc.dll`+`opcproxy`/`opccomn_ps` · `pcapBacEther.dll`)

> Research of the **Niagara N4 NATIVE field-bus shim layer** on the installed OptimizerSupervisor‑N4.14.0.162: the small native DLLs that sit UNDER the Java protocol drivers and actually touch the wire/OS. For each: WHAT native protocol/library it wraps, ITS JNI export surface (`Java_*` → which Java driver class binds), KEY imports, and the native↔Java boundary. Three distinct bridging strategies emerge — (1) a 64→32‑bit **out‑of‑process IPC** shim for LonWorks, (2) an in‑process **COM/DCOM** client for OPC DA, (3) an in‑process **raw‑packet (WinPcap/Npcap)** shim for BACnet/Ethernet. This closes gap **N4** and grounds the boot/JNI model of [Block 124]/[Block 125] in the driver layer.
>
> Sources (primary, READ‑ONLY):
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/{lon.dll, opc.dll, opcproxy.dll, opccomn_ps.dll, pcapBacEther.dll}`,
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/x86/ldvProxy.exe`.
> Method: radare2/rabin2 6.1.6 — `rabin2 -I` (identity/PE), `-l` (imported libs), `-i` (imports), `-E` (exports = JNI surface), `-s` (symbols); `strings` (manifest identities, source paths, format strings, COM interface names, Mocana/pcap API names). No decompiler needed — the export name‑mangling + imports + literal strings are decisive.
> Raw evidence preserved at:
> `…/audits/B127-{lon,opc,opcproxy,opccomn_ps,pcapBacEther,ldvProxy}-{info,imports,libs,exports,symbols,strings}.txt`.
> Markers: `[CERT]` observed in the binary (tool command + symbol/offset/string cited) · `[CERT-doc]` official document · `[CERT-web]` official web · `[CERT-a]` secondary/forum · `[INFER]` deduction.
>
> Native platform layer (Capa 25). Connects [Block 124] (boot path / `common.dll`+`nre.dll` are the libs these drivers link), [Block 125] (JNI bridge — these are exactly the `Java_com_tridium_*` name‑mangled natives B125 described, now in the driver layer), [Block 126] (`dsfspi` is the sibling native lib — same packaging conventions), [Block 19] (LON deep / NiagaraDriver), [Block 23] (BACnet networking stack), [Block 92] (`lonworks` Tridium driver / Excel‑10 wizards), [Block 120] (Spyder BACnet/LON wire protocol — the *application* layer above this *transport* layer).

---

## 127.1 — The five DLLs at a glance: three bridging strategies `[CERT]`

| DLL | Identity (manifest) | Built | Wraps (native lib/OS) | JNI class it backs | Bridge strategy |
|---|---|---|---|---|---|
| **`lon.dll`** (35 KB) | `Tridium.Niagara.LonDriverLib 4.14.0.22` — *"Niagara Lon Driver Library"* | 2024‑01‑08 | Echelon **OpenLDV / `wldv32`** (LonWorks Device driver), **out‑of‑process** | `com.tridium.platLon.BLonPlatformServiceWin32` | **64→32 IPC** via named pipe to `ldvProxy.exe` |
| **`opc.dll`** (176 KB) | `Tridium.Niagara.OpcDALibrary 4.14.0.22` — *"Niagara OPC Data Access Library"* | 2024‑01‑08 | **COM/DCOM** OPC DA classic (`ole32`+`oleaut32`) | `com.tridium.opc.*` (≈14 JNI classes) | **in‑process COM client** |
| **`opcproxy.dll`** (107 KB) | OPC Foundation DA proxy/stub | **2011‑01‑17** | MIDL‑generated **DCOM marshaller** for OPC DA interfaces (`rpcrt4`) | *(none — registered COM DLL)* | DCOM proxy/stub |
| **`opccomn_ps.dll`** (61 KB) | OPC Foundation Common proxy/stub | **2011‑01‑17** | MIDL **DCOM marshaller** for OPC Common interfaces (`rpcrt4`) | *(none — registered COM DLL)* | DCOM proxy/stub |
| **`pcapBacEther.dll`** (27 KB) | `Tridium.Niagara.BacnetEthernetLib 4.14.0.22` — *"Native support for Niagara WinPcap BACnet Ethernet driver"* | 2024‑01‑08 | **WinPcap/Npcap** (`wpcap.dll`+`packet.dll`) raw L2 frames | `com.tridium.platBacnet.BacnetEthernetAdapterWin32` | **in‑process raw‑packet** |

All five are PE32+ x86‑64, `signed true` (`rabin2 -I`, B127-*-info.txt). The two 2024 Tridium libs that link the platform (`lon`, `pcapBacEther`) import `common.dll`/`nre.dll` (the [Block 124]/[Block 125] platform libs — e.g. `lon.dll` imports `common.dll!platLogf/platLog` and `nre.dll!?getInstance@Nre@@SAPEAV1@XZ`, B127-lon-imports.txt). The two `*_ps.dll` are **not Tridium** — they are the **OPC Foundation Core‑Components redistributable** (2011 build date, classic MIDL proxy/stub exports). `[CERT]`

The decisive observation: **the wire never enters the JVM directly.** Each Java driver reaches the bus through one of three native transports, and the choice is dictated by what the underlying technology requires — a 32‑bit‑only vendor driver (LON), a COM API (OPC), or kernel packet capture (BACnet/Ethernet). `[INFER]` (the *rationale*; the mechanisms below are `[CERT]`).

---

## 127.2 — `lon.dll` — LonWorks via a 64→32‑bit out‑of‑process IPC shim `[CERT]`

`lon.dll` exports exactly **four JNI natives** (`rabin2 -E`, B127-lon-exports.txt), all binding to one Java class:

```
Java_com_tridium_platLon_BLonPlatformServiceWin32_ldvOpen
Java_com_tridium_platLon_BLonPlatformServiceWin32_ldvClose
Java_com_tridium_platLon_BLonPlatformServiceWin32_ldvRead
Java_com_tridium_platLon_BLonPlatformServiceWin32_ldvWrite
```

The `ldv*` names are the **Echelon LonWorks Device driver (LDV) API** — `ldv_open`/`ldv_close`/`ldv_read`/`ldv_write` is the canonical OpenLDV/`wldv32` interface to a LonWorks network adapter. So the JNI class `com.tridium.platLon.BLonPlatformServiceWin32` is the Java face of the Echelon LDV driver. `[CERT]` (export names) / `[INFER]` (LDV = Echelon OpenLDV — see §127.2.2 for the `[CERT]` confirmation).

But `lon.dll` does **not** link `wldv32` itself. Its strings reveal an **inter‑process** design (B127-lon-strings.txt): `[CERT]`

```
LON interface using IPC to access %s
\\.\pipe\LDVPipe
\bin\x86\ldvProxy.exe
ldvOpen: ldvProxy not started %d
ldvOpen: lon %s device handle %d pipeline handle %d
K:\data\bamboo\build-dir\...\platLon\platLon-rt\src\native\tridium\win32\BLonPlatformServiceWin32.cpp
```

and its KERNEL32 imports are a textbook child‑process‑over‑pipe controller (`rabin2 -i`, B127-lon-imports.txt): `CreateProcessA` (launch the child), `CreateToolhelp32Snapshot`+`OpenProcess`+`TerminateProcess` (find/kill it), `CreateFileA`+`SetNamedPipeHandleState` (open the pipe in message mode), `WaitForSingleObject`+critical sections (sync). `[CERT]`

### 127.2.1 — `ldvProxy.exe` is the missing 32‑bit half `[CERT]`
`bin/x86/ldvProxy.exe` exists and is **PE32 (Intel 80386 / 32‑bit), console** (`file`, B127-ldvProxy-libs.txt). It is the **named‑pipe server** end of the bridge — its strings (B127-ldvProxy-strings.txt) show `CreateNamedPipeA` / `ConnectNamedPipe` / `DisconnectNamedPipe` on the **same** pipe `\\.\pipe\LDVPipe`, plus `pipeServer: ReadFile failed` / `pipeServer: client disconnected`. `[CERT]`

### 127.2.2 — `ldvProxy.exe` is what actually calls Echelon LDV `[CERT]`
`ldvProxy.exe` strings carry the **stdcall‑decorated Echelon entrypoints** it resolves and the driver module names it loads: `[CERT]`

```
_ldv_open@4   _ldv_close@4   _ldv_read@12   _ldv_write@12
ldv32     wldv32
ldv_openP==NULL err=%u   ldv_readP==NULL err=%u   ...
```

`wldv32`/`ldv32` is the **Windows LonWorks Device driver (Echelon OpenLDV)**; the `@4`/`@12` decorations are its 32‑bit stdcall signatures. The `…P==NULL` messages are `GetProcAddress` failure diagnostics — i.e. `ldvProxy.exe` **dynamically loads** `wldv32.dll` and resolves `ldv_open/close/read/write` at runtime (which is why `wldv32` is not in its static import table). `[CERT]` / `[INFER]` (LoadLibrary+GetProcAddress, deduced from the `P==NULL` resolve‑failure strings).

### 127.2.3 — The full LON wire path `[CERT]/[INFER]`
```
Java BLonPlatformServiceWin32.ldvOpen/Read/Write/Close
   → JNI lon.dll (64-bit)                         [CERT export names]
   → CreateProcessA bin\x86\ldvProxy.exe          [CERT strings/imports]
   → named pipe \\.\pipe\LDVPipe (message mode)   [CERT both ends]
   → ldvProxy.exe (32-bit pipe server)            [CERT PE32 + pipe-server strings]
   → LoadLibrary wldv32 / ldv_open/read/write/close   [CERT decorated symbols]
   → LonWorks network adapter (U10/U20/PCLTA/iLON)     [INFER — the OpenLDV target hardware]
```

**Why the out‑of‑process shim:** Echelon's `wldv32`/OpenLDV driver is **32‑bit only**, but the Niagara runtime is a 64‑bit JVM ([Block 124]); a 64‑bit process cannot load a 32‑bit DLL in‑process, so `lon.dll` forks a 32‑bit `ldvProxy.exe` and tunnels the four LDV calls over a named pipe. The dedicated `bin/x86/` directory exists for exactly this. `[INFER]` (the *reason*; every mechanism is `[CERT]`). This is the native transport beneath the LON driver work of [Block 19]/[Block 92].

---

## 127.3 — `opc.dll` — an in‑process OPC DA classic COM/DCOM client `[CERT]`

`opc.dll` (*"Niagara OPC Data Access Library"*, `Tridium.Niagara.OpcDALibrary`) exports a **large JNI surface** — ≈52 `Java_com_tridium_opc_*` natives across ≈14 classes (`rabin2 -E`, B127-opc-exports.txt). The class taxonomy mirrors the OPC DA 2.05/3.0 classic COM object model exactly: `[CERT]`

| JNI class (`com.tridium.opc.jni…`) | OPC DA COM concept it wraps |
|---|---|
| `OpcEnv` (`initEnv`/`initThread`) | COM apartment init (`CoInitializeEx` per thread) |
| `ComObjectClient` (`query`/`release`) | generic `IUnknown::QueryInterface`/`Release` |
| `client.common.OpcCommon` (`setClientName`/`get/setLocaleId`/`getErrorString`/`availableLocales`) | `IOPCCommon` |
| `client.common.OpcServerList2.discoverServers` | `IOPCServerList2` (server enumeration) |
| `client.common.OpcShutdown` (`advise`/`unadvise`) | `IOPCShutdown` callback sink |
| `client.da.OpcDaServer` (`createLocalServer`/`createRemoteServer`/`addGroup`/`removeGroup`/`getStatus`) | `IOPCServer` |
| `client.da.OpcGroupStateMgt` (`get/setState`) | `IOPCGroupStateMgt` |
| `client.da.OpcItemMgt` (`addItems`/`removeItems`/`validateItems`/`setActiveState`) | `IOPCItemMgt` |
| `client.da.OpcSyncIo` (`read`/`writeBoolean`/`writeNumeric`/`writeString`/`writeArray`) | `IOPCSyncIO` |
| `client.da.OpcAsyncIo2` (`readAsync`/`writeAsync`/`refresh`/`advise`/`unadvise`/`cancel2`) | `IOPCAsyncIO2` + `IOPCDataCallback` |
| `client.da.OpcBrowse` / `OpcBrowseServerAddressSpace` (`browse`/`goRoot`/`goDown`/`listBranches`/`listItems`/`queryOrganization`) | `IOPCBrowseServerAddressSpace` |
| `client.da.OpcItemProperties` (`get{Boolean,Numeric,String}Property`/`queryAvailableProperties`) | `IOPCItemProperties` |
| `client.da.OpcNTSecurity` / `OpcPrivateSecurity` (`logon`/`logoff`/`changeUser`/`queryImpersonationLevel`) | DCOM security / impersonation |

The COM backing is proven by the imports (`rabin2 -i`, B127-opc-imports.txt): `ole32!CoInitializeEx`, **`ole32!CoInitializeSecurity`** (DCOM authentication setup), `CoGetClassObject`, `CLSIDFromString`/`IIDFromString`/`StringFromIID`, `CoTaskMemFree`, plus the OLE‑Automation marshalling helpers `oleaut32!SafeArray*` (`SafeArrayCopy/Destroy/GetDim/GetLBound/PutElement`) and `Variant*` (`VariantClear/Copy/Init`) and `SysReAllocString`/`SysStringLen`. The OPC callback interfaces appear as mangled C++ type strings — `.?AUIOPCDataCallback@@` and `.?AUIOPCShutdown@@` (B127-opc-strings.txt) — i.e. `opc.dll` itself **implements** `IOPCDataCallback`/`IOPCShutdown` so the OPC server can push data‑change and shutdown notifications back into the JVM. `[CERT]`

Boundary facts `[CERT]`:
- **Local vs remote**: `OpcDaServer.createLocalServer` vs `createRemoteServer` — the latter is **DCOM** to an OPC server on another host (the `OpcNTSecurity`/impersonation + `CoInitializeSecurity` machinery exists precisely for cross‑machine DCOM auth).
- **Threading**: `OpcEnv.initThread` shows every Java thread that calls OPC must enter a COM apartment first — the classic STA/MTA discipline surfaced to the JNI layer.
- This is an **in‑process** bridge (no child process, unlike LON): the COM runtime + DCOM does the cross‑process/host hop, not a Tridium proxy.

---

## 127.4 — `opcproxy.dll` + `opccomn_ps.dll` — the OPC Foundation DCOM proxy/stubs `[CERT]`

These two are **not** Tridium code and have **no JNI**. They export only the COM‑server quartet (`rabin2 -E`): `DllGetClassObject`, `DllCanUnloadNow`, `DllRegisterServer`, `DllUnregisterServer`, **`GetProxyDllInfo`** (B127-opcproxy-exports.txt / B127-opccomn_ps-exports.txt). `GetProxyDllInfo` + a dependency on **`rpcrt4.dll`** is the unmistakable signature of a **MIDL‑generated DCOM proxy/stub** DLL. Their 2011 build date marks them as the **OPC Foundation "OPC Core Components" redistributable**, shipped alongside Niagara so the OPC interfaces can be marshalled. `[CERT]`

Their job: when `opc.dll` calls an OPC interface on a server in a **different apartment, process, or machine**, COM needs to serialize the interface's methods across the boundary. The proxy (client side) and stub (server side) code lives in these DLLs, keyed by interface IID. Their strings enumerate exactly the interfaces they marshal `[CERT]`:
- **`opccomn_ps.dll`** (the *Common* proxy/stub): `IOPCCommon`, `IOPCShutdown`, `IOPCServerList`, `IOPCServerList2`, `IOPCEnumGUID`, plus the MIDL runtime scaffolding `CStdStubBuffer_*` and `IUnknown_{QueryInterface,AddRef,Release}_Proxy` (B127-opccomn_ps-strings.txt).
- **`opcproxy.dll`** (the *DA* proxy/stub): the entire OPC DA constant/enum namespace — `OPC_QUALITY_*`, `OPC_PROPERTY_*`, `tagOPCBROWSETYPE`, `IOPCServerPublicGroups`, `OPC_WRITE_BEHAVIOR_*` (B127-opcproxy-strings.txt) — i.e. it marshals `IOPCServer`/`IOPCItemMgt`/`IOPCSyncIO`/`IOPCAsyncIO2`/`IOPCBrowseServerAddressSpace`/`IOPCItemProperties`/`IOPCDataCallback`.

So the OPC layering is three native pieces: **`opc.dll`** (Tridium JNI + COM‑client logic) → COM/DCOM runtime → **`opcproxy.dll` + `opccomn_ps.dll`** (interface marshallers, registered once via `regsvr32`/`DllRegisterServer`) → the OPC DA server (local or remote). `[CERT]/[INFER]` (the registration‑and‑marshal flow is the standard COM model; the IID inventory is `[CERT]`).

---

## 127.5 — `pcapBacEther.dll` — BACnet/Ethernet via WinPcap/Npcap raw L2 frames `[CERT]`

`pcapBacEther.dll` (*"Native support for Niagara WinPcap BACnet Ethernet driver"*, `Tridium.Niagara.BacnetEthernetLib`) exports **six JNI natives** on one class (`rabin2 -E`, B127-pcapBacEther-exports.txt):

```
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_queryForAdapters0
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_getAddress0
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_bacnetOpen0
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_bacnetClose0
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_bacnetRead0
Java_com_tridium_platBacnet_BacnetEthernetAdapterWin32_bacnetWrite0
```

It links **`wpcap.dll` + `packet.dll`** (WinPcap/Npcap) directly (`rabin2 -l`, B127-pcapBacEther-libs.txt) and imports the full live‑capture API (`rabin2 -i`, B127-pcapBacEther-imports.txt): `[CERT]`

| Imported pcap/Packet symbol | Role in the BACnet/Ethernet driver |
|---|---|
| `pcap_findalldevs` / `pcap_freealldevs` | back `queryForAdapters0` — enumerate NICs |
| `pcap_create`+`pcap_set_snaplen`/`set_promisc`/`set_timeout`/`set_buffer_size`+`pcap_activate` | back `bacnetOpen0` — open a NIC for capture |
| `pcap_compile`+`pcap_setfilter`+`pcap_freecode` | install a **BPF filter** (capture only BACnet frames) |
| `pcap_setmintocopy` | Npcap latency tuning (push frames up promptly) |
| `pcap_next_ex` | back `bacnetRead0` — receive a frame |
| `pcap_sendpacket` | back `bacnetWrite0` — transmit a frame |
| `pcap_close` / `pcap_breakloop` | back `bacnetClose0` |
| `pcap_geterr` | error text |
| `PacketOpenAdapter`/`PacketRequest`/`PacketCloseAdapter` (packet.dll) | low‑level NPF — used to read the adapter **MAC address** (`getAddress0`) |

Confirming strings (B127-pcapBacEther-strings.txt): `Ethernet adapter %s`, `Unable to open ethernet adapter`, `Unable to get ethernet adapter MAC address`, `Invalid Ethernet frame size`, `adapter BPF query too large, %zu > %d (MAX)`. `[CERT]`

So the JNI class `com.tridium.platBacnet.BacnetEthernetAdapterWin32` is a **raw Layer‑2 frame I/O adapter**: it bypasses the Windows TCP/IP stack entirely and reads/writes **BACnet/Ethernet (ISO 8802‑3)** frames straight off the NIC through Npcap. This is the data‑link transport beneath the BACnet networking stack of [Block 23] — specifically the *Ethernet* (ASHRAE 135 Clause 7) link option, distinct from BACnet/IP (which would use ordinary UDP sockets and need no pcap). `[CERT]` / `[INFER]` (the ISO‑8802 framing detail is deduced from "Ethernet frame" + raw L2 capture; the pcap mechanism is `[CERT]`).

Boundary facts `[CERT]`:
- Reading the MAC via `PacketRequest` (not pcap) shows it needs the **local hardware address** as the BACnet/Ethernet source SADR.
- Use of `pcap_sendpacket` (raw inject) + promiscuous capture means Niagara is its **own** BACnet/Ethernet data‑link — the OS provides no BACnet service.
- **Requires Npcap/WinPcap installed**; absent it, `bacnetOpen0` cannot resolve the adapter (the import is hard‑linked, so the DLL won't even load without `wpcap.dll`/`packet.dll`).

---

## 127.6 — The native↔Java boundary pattern (same as B125) `[CERT]`

Every JNI export here follows the **name‑mangling bind** convention [Block 125 §125.x] established for the platform natives: `Java_<fully_qualified_class_with_underscores>_<method>` with **no `RegisterNatives`** — the JVM resolves them by symbol name at `System.loadLibrary` time. The three driver libs simply place their natives in the `com.tridium.platLon` / `com.tridium.opc` / `com.tridium.platBacnet` packages. `[CERT]` (export names match the convention exactly; B127-*-exports.txt).

The two 2024 Tridium libs (`lon`, `pcapBacEther`) also share [Block 124]/[Block 125]'s platform plumbing — they import `common.dll!platLogf`/`platLog` for logging and `nre.dll!Nre::getInstance` to reach the runtime singleton (B127-lon-imports.txt; same pattern visible in pcapBacEther). `opc.dll`, being a pure COM client, does not need `nre`/`common` and links only the COM/CRT runtime. So the boundary has two flavors: **platform‑integrated** natives (LON, BACnet — log + Nre) vs **self‑contained** natives (OPC — COM only). `[CERT]`

---

## 127.7 — Defensive‑security findings (factual)

1. **Raw‑packet capability is present and powerful** `[CERT]` (§127.5): `pcapBacEther.dll` performs **promiscuous capture + arbitrary frame injection** (`pcap_set_promisc`, `pcap_sendpacket`) on a chosen NIC. This is legitimate for a BACnet/Ethernet data‑link, but it means the Niagara process (and anything that can drive this JNI class) has L2 send/sniff on that interface — a meaningful capability to inventory in a hardening review. It depends on Npcap/WinPcap being installed with the appropriate privilege.
2. **OPC = DCOM attack surface** `[CERT]` (§127.3/§127.4): `createRemoteServer` + `CoInitializeSecurity` + registered proxy/stubs mean OPC connectivity rides **classic DCOM** — historically a difficult‑to‑firewall, broad surface (RPC dynamic ports, machine‑wide COM launch/access permissions). The OPC proxy/stub DLLs are **2011** vintage (OPC Foundation redistributable), i.e. long‑lived unchanged marshalling code.
3. **Out‑of‑process LON shim is a trust/lifecycle boundary** `[CERT]` (§127.2): `lon.dll` spawns `bin\x86\ldvProxy.exe` and trusts whatever answers on `\\.\pipe\LDVPipe`. A local actor able to pre‑create that pipe name could interpose on the LON I/O path (`[INFER]` — classic named‑pipe squatting risk; the pipe name and server model are `[CERT]`, the exploitability is not assessed here). The proxy is the same DigiCert‑signed Tridium binary family ([Block 124]).
4. **No crypto in any of these DLLs** `[CERT]`: imports are KERNEL32/CRT + the transport lib (pcap / COM / pipe). Field‑bus confidentiality/auth, where it exists, lives above this layer (driver/protocol), not in the native shim.

No secrets were read; all evidence is import tables, export symbols, and literal strings (public by design).

---

## 127.8 — Self‑verify

**Token re‑checks** (load‑bearing `[CERT]` re‑confirmed by re‑running the tool):
1. `lon.dll` 4 JNI exports `…BLonPlatformServiceWin32_ldv{Open,Close,Read,Write}` — ✓ (`rabin2 -E`, B127-lon-exports.txt).
2. `lon.dll` IPC strings `\\.\pipe\LDVPipe`, `\bin\x86\ldvProxy.exe`, "LON interface using IPC to access %s" + imports `CreateProcessA`/`SetNamedPipeHandleState`/`CreateToolhelp32Snapshot` — ✓ (B127-lon-strings.txt + B127-lon-imports.txt).
3. `ldvProxy.exe` = PE32 32‑bit; named‑pipe **server** (`CreateNamedPipeA`/`ConnectNamedPipe` on `\\.\pipe\LDVPipe`); Echelon `_ldv_open@4`/`_ldv_read@12`/`_ldv_write@12`/`_ldv_close@4` + `wldv32`/`ldv32` — ✓ (B127-ldvProxy-libs.txt + B127-ldvProxy-strings.txt).
4. `opc.dll` JNI class taxonomy (`OpcDaServer.createLocalServer/createRemoteServer`, `OpcSyncIo.write*`, `OpcAsyncIo2.*`, `OpcBrowseServerAddressSpace.*`, `OpcItemMgt.*`, `OpcItemProperties.*`, `OpcNTSecurity`/`OpcPrivateSecurity`) — ✓ (B127-opc-exports.txt).
5. `opc.dll` COM imports `CoInitializeEx`/**`CoInitializeSecurity`**/`CoGetClassObject`/`CLSIDFromString` + `oleaut32!SafeArray*`/`Variant*`; callback type strings `.?AUIOPCDataCallback@@`/`.?AUIOPCShutdown@@` — ✓ (B127-opc-imports.txt + B127-opc-strings.txt).
6. `opcproxy.dll`/`opccomn_ps.dll` export only `Dll{GetClassObject,CanUnloadNow,RegisterServer,UnregisterServer}`+`GetProxyDllInfo`, link `rpcrt4.dll`, built 2011 — ✓ (B127-opcproxy-exports.txt / B127-opccomn_ps-exports.txt / -info.txt).
7. `opccomn_ps.dll` marshals `IOPCCommon`/`IOPCShutdown`/`IOPCServerList(2)`/`IOPCEnumGUID` (+`CStdStubBuffer_*`/`IUnknown_*_Proxy`); `opcproxy.dll` carries the `OPC_QUALITY_*`/`OPC_PROPERTY_*`/`tagOPCBROWSETYPE` DA namespace — ✓ (B127-opccomn_ps-strings.txt / B127-opcproxy-strings.txt).
8. `pcapBacEther.dll` 6 JNI exports `…BacnetEthernetAdapterWin32_{queryForAdapters0,getAddress0,bacnetOpen0,bacnetClose0,bacnetRead0,bacnetWrite0}` — ✓ (B127-pcapBacEther-exports.txt).
9. `pcapBacEther.dll` links `wpcap.dll`+`packet.dll`; imports `pcap_findalldevs/create/activate/compile/setfilter/next_ex/sendpacket/setmintocopy` + `PacketOpenAdapter/Request`; strings "Ethernet adapter %s"/"Invalid Ethernet frame size"/"BPF query too large" — ✓ (B127-pcapBacEther-libs.txt + -imports.txt + -strings.txt).
10. Manifest identities `Tridium.Niagara.{LonDriverLib,OpcDALibrary,BacnetEthernetLib}` + descriptions — ✓ (B127-*-strings.txt).
11. `lon`/`pcapBacEther` import `common.dll`(`platLogf`)+`nre.dll`(`Nre::getInstance`); `opc` imports neither — ✓ (B127-lon-imports.txt / B127-opc-imports.txt).

**11/11 load‑bearing tokens re‑verified** against re‑run tool output.

**Marker tally** (`grep -oE` over this file; raw totals `[CERT]`=43, `[INFER]`=11, `[CERT-doc/web/a]`=2 each, then minus the 1 header‑legend mention and the 1 self‑reference inside this very tally line): **`[CERT]` 41 load‑bearing** · `[CERT-doc]` 0 (legend only) · `[CERT-web]` 0 (legend only) · `[CERT-a]` 0 (legend only) · **`[INFER]` 9 load‑bearing**. Ratio **[INFER]/[CERT] ≈ 0.22** — low. The inferences are all *rationale/target* deductions (why a 64→32 shim, which physical LON adapter, ISO‑8802 framing, the COM registration flow, named‑pipe squatting risk) layered on top of fully `[CERT]` mechanisms (every export/import/string is observed). N4's static evidence is rich; the only deeper grade would require **decompiling** the JNI bodies (control flow inside `ldvOpen`/`bacnetRead0`) or **running** the drivers against live hardware (LON adapter, OPC server, BACnet/Ethernet segment) — i.e. the remaining LON/OPC/BACnet depth is requires‑execution, not static‑investigable.

---

## 127.x — Connections

- **[Block 125]** — these are the `Java_com_tridium_*` name‑mangling natives B125 described, now in the **driver** layer (vs the boot/JVM‑embedding layer). Same bind convention (no `RegisterNatives`), same `common.dll`/`nre.dll` platform plumbing for the Tridium libs.
- **[Block 124]** — the boot path: `lon`/`pcapBacEther` link `nre.dll!Nre::getInstance` (the runtime singleton B124 mapped) and `common.dll` logging; the 64‑bit JVM (B124) is *why* LON needs the 32‑bit `ldvProxy.exe` out‑of‑process shim. All five DLLs share B124's DigiCert‑G4 Authenticode signing.
- **[Block 126]** — `dsfspi.dll` is the sibling native lib (crypto provider); B127's drivers carry no crypto — confidentiality lives above the native shim.
- **[Block 19]** — LON deep / NiagaraDriver: B127 supplies the **native transport** (`lon.dll`→`ldvProxy.exe`→Echelon `wldv32`) under the Java LON driver B19 modeled.
- **[Block 92]** — `lonworks` Tridium driver + Excel‑10/XL15C wizards: `BLonPlatformServiceWin32` is the platform‑service face those wizards' driver ultimately rides on Windows.
- **[Block 23]** — BACnet networking stack: `pcapBacEther.dll` is the **BACnet/Ethernet (ISO 8802‑3) data‑link** option of that stack — raw L2 via Npcap, distinct from BACnet/IP.
- **[Block 120]** — Spyder BACnet/LON **wire** protocol: B120 analyzed the *application*‑layer file‑transfer/AtomicWriteFile over BACnet/LON; B127 is the *transport* beneath it on the supervisor side (how those PDUs leave the host: OPC has no role there, but LON/BACnet do).
- **Forward (open gaps)**: **N5** Workbench native shell (`wb.exe`/`wb_w.exe`); **N6** platform daemon TCP wire protocol (requires‑execution); **N7** migration/tools (`n4mig.exe`/`hdbt.exe`/…).
