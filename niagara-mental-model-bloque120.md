# Block 120 — Spyder driver download/upload **WIRE protocol** (BACnet AtomicWriteFile vs LON file-transfer): deep-dive vs B77

> Research of the **actual on-the-wire transfer** that the two Honeywell Spyder drivers (`honeywellBacnetSpyder`, `honeywellLonSpyder`) use to push a compiled control binary to a physical Spyder controller, and to read it back. Goes DEEPER than [Block 77] (which gave the device hierarchy + the compile→download flow at high level) by tracing the concrete transport primitive, the per-section incremental download, the on-device integrity check, the retry/restore/version-gating, and the BACnet-object vs LON-NV transport split. Pairs with [Block 106] (the compiler that produces the FileSection0-5 / BOAC-CRC binary this block transfers).
>
> **Corpus language note**: this is a Spyder-ecosystem block → **English** (the legacy niagara corpus B1-B114 is Spanish; new Spyder blocks switched to English at B115).
>
> Sources (vineflower/CFR decompiled, deobfuscated — same ZKM-cleaned tree as B77):
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellBacnetSpyder/honeywellBacnetSpyder/vineflower/com/honeywell/bacnetSpyder/`
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellLonSpyder/honeywellLonSpyder/vineflower/com/honeywell/lonSpyder/`
> - `/home/cristian/modules/Prototipos/modulos/organized/honeywellBacnetSpyder/honeywellBacnetSpyder/extracted/XL10NextGenXML/BacnetSpyder.xml` (bundled model registry)
>
> Method: direct reading of the `xl10Controller/download`, `xl10Controller/upload`, `restorer` and `controllerInfo` packages of both drivers + verbatim verification of the transport calls, file numbers, retry loop, version gates and the model XML. Markers:
> `[CERT]` verified verbatim by me (`file:line`) · `[CERT-doc]` bundled doc (B116) · `[CERT-a]` sub-agent/secondary · `[INFER]` deduction.
>
> Layer 22 (deobfuscated OEM). Connects [Block 77] (the architecture this drills into), [Block 106] (the compiler producing the payload), [Block 116] (vendor doc), [Block 115] (the migrator that never touches this wire path — offline file-to-file).

Citations below use the per-driver base paths above; e.g. `bacnetSpyder/.../download/BBacnetFileUtil.java:78` = `<bacnet-base>/xl10Controller/download/BBacnetFileUtil.java`.

---

## 120.1 — The two transports at a glance `[CERT]`

Both drivers implement the SAME `ISpyderDownload`/`ISpyderUpload` contract (B77 §77.1) and consume the SAME compiled payload from the tool (`BValueList byteList` = the indexed list of per-store `BBlob`s, B106), but the **wire primitive is completely different**:

| | **BACnet Spyder** | **LON Spyder** |
|---|---|---|
| Transport primitive | **BACnet AtomicWriteFile / AtomicReadFile** to BACnet **File objects** (object-type 10) | **LonWorks file transfer** (`javax.baja.lonworks.util.LonFile` open/write/close to numbered files) |
| Transfer orchestration | per-file **Program-object** (type 16) **halt → write → re-run** handshake | plain `LonFile.open(fileNumber,true,true)` → `write(bytes,offset)` → `close()` (no program dance) |
| Metadata / version | written as BACnet **properties** (objectName 77, apduTimeout 11, retries 73, maxInfoFrames 63) + ApplVer into a File object | written as **NCIs** (`nciDeviceName`, `nciApplVerNew`) field-by-field + `doForceWrite()` |
| Device base class | `BBacnetDevice` (B77) | `BDynamicDevice` (B77) — NVs |
| Extra LON-only artifact | — | **Echelon ShortStack** image (SI data + SNVT descriptors + comm params) |
| File writer root | `BBacnetFileWriter implements IDeviceFileWriter` (`download/BBacnetFileWriter.java:71`) | `BXL10NextGenFileWriter implements IDeviceFileWriter` (`download/BXL10NextGenFileWriter.java:64`) |

> Both writers implement the shared `IDeviceFileWriter` interface and the same method shapes (`download(ISpyderDevice, BValueList, boolean fullDownload)`, `quickDownload(...)`, `getStoreOffset(String)`). The transport is the only thing that diverges — the *layout* (named stores at model-specific offsets) is identical. `[CERT]` `bacnetSpyder/.../download/BBacnetFileWriter.java:73,147`; `lonSpyder/.../download/BXL10NextGenFileWriter.java:66,94`.

---

## 120.2 — BACnet wire protocol: AtomicWriteFile into File objects, gated by the Program object `[CERT]`

The compiled binary is NOT written as device properties — it is streamed into **BACnet File objects** via the standard **AtomicWriteFile** service.

**The transport call** (`bacnetSpyder/.../download/BBacnetFileUtil.java:78,115`) `[CERT]`:
```java
((BBacnetNetwork)bBacnetDevice.getNetwork()).getBacnetComm()
    .atomicWriteFileStream(bBacnetDevice.getAddress(), bBacnetObjectIdentifier, offset, byArray);
```
- It first asserts the device supports the service: `if (!isServiceSupported("atomicWriteFile")) throw "Atomic File service not supported"` (`:67`). Read path mirrors it with `atomicReadFileStream` + `isServiceSupported("atomicReadFile")` (`:144,186`).
- **APDU chunking** `[CERT]` (`:71,103-134`): chunk size = `maxAPDULengthAccepted - 30`. If the whole payload fits OR the device advertises segmented-receive, it writes in one shot; otherwise it loops, copying `chunk`-sized slices and writing each at `offset + base` — i.e. it implements the streamed/segmented file write itself.

**The File objects** the driver writes (object-type 10 = FILE) `[CERT]` (`download/BBacnetFileWriter.java:90-93`, `BBacnetFileOffsetWriter.java:53`):

| File object instance | Constant | Content |
|---|---|---|
| **301** | `CONFIG_FILE_NUMBER` | configuration stores (NvConfig, ControlLoop, ControlConstants, Linearization, AI/DI/AO/DO, UnitConfig, Scheduler, WallModule…) |
| **302** | `PARAM_FILE_NUMBER` | parameter stores (ApplVerNew, DeviceName, AlarmDisable, ControlNonVolatile, Schedule, Holiday…) |
| **303** | `PROXY_FILE_NUMBER` | proxy / KF (wall-module) data |
| **304** | `FILE_OFFSET_NUMBER` | the file-offset table (`BBacnetFileOffsetWriter`) |
| **262** | `BOAC_CONFIG_FILE_NUMBER` | BOAC profile config |
| **263** | (literal) | BOAC bindings (data-sharing links) |

**The Program-object handshake** `[CERT]` — every file section is written between a halt and a re-run of a BACnet **Program object** (type 16), using the standard `program_change`(prop 90) / `program_state`(prop 92) properties:
- `haltTheProgramObject(fileNo, dev)` (`BBacnetFileUtil.java:207-246`): `writeProperty(addr, oid(16,fileNo), 90, writeEnumerated(5))` then reads `program_state` (92); enumerated `5` = HALT.
- `reRunProgramObject(fileNo, dev)` (`:273-312`): `writeProperty(... 90, writeEnumerated(1))` then checks state; `1` = RUN.
- Driver state machine constants (`:47-50`): `DS_START_FILE_READ_WRITE=2`, `DS_EXIT=5`, `FILE_INIT_RESP_PLUS=7`, `FILE_INIT_RESP_MINUS=8`.

The full `download()` order (`BBacnetFileWriter.java:128-145`) `[CERT]`: `writeDeviceInstance()` → `writeDeviceName()` (objectName, prop 77) → push APDU-timeout (prop 11), num-APDU-retries (prop 73), max-info-frames (prop 63) → `writeParameterFile()` (halt 302 → write sections → rerun 302) → `writeConfigurationFile()` (halt 301 …) → proxy file (halt 303 …) → if clearBinding/full: bindings (263) → if WriteNetworkImage/full: BOAC (262). Each section write is wrapped exactly like `a(boolean)`/`writeParameterFile()` show (`:179-200,335-355`).

> **For the commissioner**: a Spyder download is a sequence of *halt-program → AtomicWriteFile chunks → run-program* per file object. A failure mid-way leaves the targeted program object halted — which is why a botched download can leave the controller in a non-running state until a clean re-download.

---

## 120.3 — The layout that goes on the wire: named stores → model offsets → File objects `[CERT]`

The payload is the `BValueList` of per-store `BBlob`s the compiler produced (B106). The writer addresses each store by **name → model-specific byte offset**, then writes the blob INTO the relevant File object at that offset.

`getStoreOffset(name)` (`BXL10NextGenFileWriter.java:617-624`, mirrored on BACnet) `[CERT]`:
```java
int model = objDevice.getCompiledFwModel();
ModelInfo mi = objDevice.getModelInfo(objDevice.getControllerModel(), model);
return mi.getFileVariables(name).getOffset();
```
So the FileSection0-5 layout of [Block 106] is, concretely, **a contiguous byte image per File object whose store boundaries are model-driven offsets** read from the model descriptor. The BACnet config writes (`BBacnetFileWriter.java:399-501`, methods `i()/j()/k()/l()…`) each do: `blob = byteList.get(<idx>)` → `offset = getStoreOffset("<Store>")` → `writeFileData(dev, oid(10,301), offset, blob)`. Verified store→index map (BACnet) includes: NvConfig=10, ControlLoop=12, ControlConstants=13, Linearization=14, AnalogInput=15, DigitalInput=16, AnalogOutput=17, DigitalOutput=18 `[CERT]` (`:402-468`). The param/config bundling helpers (`BBacnetFileOffsetWriter.bundleConfigBinaries/bundleParamBinaries:173-288`) enumerate the full store set verbatim (AlarmType, SvMapEntryConfig, NvConfig, ControlLoop, ControlConstants, Linearization, AI/DI/AO/DO, FloatingMotor, UnitConfig, ScalarConfig, StructuredConfig, Scheduler, WallModule, ControlVariables, PublicVariableGroupTable, PublicVariableSendTable, WallModuleBus*, …; params: ApplVerNew, DeviceName, AlarmDisable, ControlNonVolatile, AnalogInputOffset, DayLightSavings, Schedule, Holiday) `[CERT]`.

> This RESOLVES the "FileSection0..5" of [Block 106 §106.3] from an opaque section index into **named, model-offset stores grouped into BACnet File objects 301/302/303** — the section abstraction is the file-object boundary; the stores are the contents.

---

## 120.4 — Incremental download: the `getBinaryModified()` BitSet, one bit per store `[CERT]`

[Block 106] noted `getBinaryModified()` enables incremental download. Here is the exact mechanism, identical in both drivers:

Each store write is gated by `if (fullDownload || BUtilityClass.getBit(binaryModified, <bit>))` `[CERT]`:
- BACnet: `BBacnetFileWriter.java:402` (NvConfig=bit10), `:422` (ControlLoop=bit12), `:442` (ControlConstants=bit13), `:462` (Linearization=bit14), `:262` (bindings=bit35), `:315` (BOAC=bit36); proxy-offset = bit31 (`BBacnetFileOffsetWriter.java:93`).
- LON: `BXL10NextGenFileWriter.java:210` (ControlNonVolatile=bit0), `:297/:306` (AI/AO mode=bit2/3), `:343` (NvConfig=bit10), `:361` (ControlLoop=bit12), `:379/:388` (LonSiData=bit33 / LonAppInitData=bit34), … through bit25.

`binaryModified = objDevice.getBinaryModified()` is captured in `initilaizeDownload` (`BBacnetFileWriter.java:119`; `BXL10NextGenFileWriter.java:90`). After a full download the BACnet writer clears it: `objDevice.setBinaryModified(BBitSet.DEFAULT)` (`BBacnetFileWriter.java:143`); LON clears it only for Spyder-1 models (`BXL10NextGenFileWriter.java:114-116`) `[CERT]`.

> **Operational meaning**: a "Quick Download" only re-transfers the stores whose dirty bit is set; "Full Download" forces every store and then resets the BitSet. This is the same `FB_NV_MODIFIED`/`compileStatusFlag` machinery B77 §77.4 surfaced — the BitSet is the per-store granularity behind it.

---

## 120.5 — On-device integrity: CRC embedded in the binary, plus a read-back CRC-correction `[CERT]`

The controller validates each transferred image by **CRC only** (no signature) — confirming the [Block 106 §106.3] security finding from the *transfer* side.

- The CRC is computed by the writer over the store body (skipping the 4-byte length/CRC header) and embedded into the header before transfer. File-offset store (`BBacnetFileOffsetWriter.generateFileOffsetBinaries:149-161`) `[CERT]`: `crc = a.calculateCRCChecksum(body, len); l.setCrc(crc); l.setLength(len)` (big-endian image). ApplVer (`BBacnetFileWriter.java:991-995`): CRC over the section, written into the first bytes (big-endian). LON ApplVer (`BXL10NextGenFileWriter.java:246-257`): `crc = calculateCRCChecksum(body,31)`; `byArray[0..1]=crc, [2]=0, [3]=31`.
- The **BOAC** path uses the table-based variant: `BCRCGenerator.calculateCRCChecksumForBOAC(d, n)` (`BBacnetFileWriter.java:292` for bindings; `BBacnetSpyderCompilation.java:3752`) — the 256-entry CRC-16 table of [Block 106 §106.3]. Bindings are little-endian (`toByteArrayInLittleEndianFormat`, `:286-295`).
- **Read-back CRC-correction** `[CERT]` (`download/BBacnetKFFileWriter.java:138-180`, method `a(...)`): after a quick proxy download the driver, for each CRC-correction segment, `reRunProgramObject(303)` → **`readFileData`** the segment back from the device → recompute `calculateCRCChecksum` over body → overwrite the first 4 bytes with the fresh CRC (little-endian, `convertCharToLittleEndianByteArray`) → `writeFileData` it back, and also patches its own cached compiled image (`storeCompiledData`). It is triggered only when `getCrcCorrectionProxyDataInfo()` is non-empty (`BBacnetKFFileWriter.java:120-124`).

> **[SECURITY CERT]** The transfer carries only a CRC-16; nothing in the write path computes or verifies a cryptographic signature (no `MessageDigest`/`Cipher` anywhere in the download packages). Anyone who can speak AtomicWriteFile to File objects 301/302/303 and recompute the CRC can inject arbitrary control logic. This is the *wire-level* confirmation of the B106 finding (which observed it at compile time). Same cross-cutting pattern as B75/B94/B98/B99.

---

## 120.6 — Retry, restore, and version gating (ToolVersion · firmware Version · Model 8) `[CERT]`

**Retry** `[CERT]` (`BBacnetFileUtil.java:74-97,111-134,182-205`): every Atomic read/write is wrapped in a **5-attempt** loop (`n=5; while(0<=n--)`) with `Thread.sleep(1000L)` between tries; on exhaustion it calls `BSpyderDownloadUtility.addRetryDownloadProp(device)` and throws `LocalizableRuntimeException("honeywellSpyderTool","Bacnet File read failed")`. Higher layers funnel exceptions through `((ISpyderDownload)dev).checkAndAddRetryDownloadProp(ex)` so a failed download is recorded as a retry property on the device for later resumption (B77 §77.4 retry logic, now located exactly).

**Restore + firmware version gating** `[CERT]` — `BBacnetDeviceRestorer extends BDeviceRestorer` (`restorer/BBacnetDeviceRestorer.java`) compares the *saved* component version `this.compVer` against firmware milestones to decide migrations on restore:
- `< 5.0.0`: rebuild proxy-point links (`backupProxyPointsLinks`/`convertProxyDeviceExtension`, `:58-72`).
- `< 6.1.0`: `BDeviceUtil.recoverModelInformation(dev, compVer, …)` (`:74-76`).
- `< 5.110`: recompute `setCompiledFwModel(getLowestModelId(modelName))` (`:77-82`).
- `< 6.112`: inject a `BBacnetNetMgmt` net-management child (`:83-88`).
- Object/point restorers add `< 4.3.0`, `< 4.9.0`, `< 5.113`, `< 5.206` gates (`BBACnetObjectRestorer.java:79,122,147`; `BBacnetOutputPointRestorer.java:50`). LON adds `< 6.1.0/5.110` device gates plus NV-level gates `1.14.0/1.18.0/1.19.11/1.19.16/4.9.0/5.113/5.202` (`restorer/BLonDeviceRestorer.java:38,41`; `BNVRestorer.java:108-267`).

**ToolVersion** `[CERT]`: distinct from firmware version, the *tool* version is stamped during upload — `BUtilityClass.updateVersionNumber(comp, ((IOnlineNetworkInterfaceHandler)objDevice).getToolVersion())` (`upload/BBacnetUpload.java:351,1251`).

**Model 8** `[CERT]` — the bundled model registry (`extracted/XL10NextGenXML/BacnetSpyder.xml`) maps `model_id` to hardware. `model_id="8"` is the **Micro BACnet Spyder family**: `ModelMicroBACnet1..5` = PVB4024NS / PVB4022AS / PUB4024S / PUB1012S / PVB0000AS (`BacnetSpyder.xml`, the `ModelMicroBACnet*` entries). The full-size models use `model_id` 4/13/15 (`ModelBACnet1..4` = PVB6436AS/PVB6438NS/PUB6438S/PUB6438SR). Model IDs are resolved at runtime from this XML (`controllerInfo/BacnetSpyderModels.java:22` loads `module://honeywellBacnetSpyder/XL10NextGenXML/BacnetSpyder.xml`).

> This grounds the [Block 40 §40.2.6] "Model 8 issues" note: Model 8 is the **Micro** controller class, with smaller capacities — the per-model `getStoreOffset`/`getFileVariables` capacities differ, so a transfer sized for a full Spyder will overrun a Micro. The model gate (`getCompiledFwModel` + per-model `ModelInfo`) is what keeps the offsets correct per family. `[INFER]` (the capacity values live in the per-model XML, not read here).

---

## 120.7 — LON wire protocol: LonWorks file-transfer + NCI force-writes + ShortStack `[CERT]`

The LON Spyder uses **LonWorks file transfer**, not BACnet AtomicFile and not raw NV memory writes for the bulk image:

**Bulk stores → LonFile** `[CERT]` (`download/BXL10NextGenFileWriter.java:202-501`):
```java
LonFile lonFile = LonFile.createFile((BLonDevice)objDevice);   // javax.baja.lonworks.util.LonFile
lonFile.open(objDevice.getParamFileNumber(), true, true);       // param file
...
lonFile.write(arrDataToBeWritten, getStoreOffset("ControlNonVolatile"));   // each store at its offset
...
lonFile.close();
```
- Numbered files: `getParamFileNumber()` (param stores, `:205`), `getConfigFileNumber()` (config stores, `:323`), `getProxyFileNumber()`, `getFileOffsetFileNumber()`. The concrete numbers are assigned by model class in `device/BLonSpyder.java:1206-1217` `[CERT-a]`: **relay** model → config=0, param=1, proxy=2, fileOffset=3; **normal** model → config=1, param=2, proxy=3 (`getConfig/Param/Proxy/FileOffsetFileNumber` at `BLonSpyder.java:5282-5296`). `BLonSpyderFileOffsetWriter` opens a LonFile and writes the file-offset table (`:111,169-171`) plus a `Thread.sleep(3000L)` "Waiting for 3 seconds after writing FileOffset file" (`:387-388`) `[CERT-a]`.
- Same per-store dirty-bit gating and same `getStoreOffset(name)` model-offset resolution as BACnet (§120.3/§120.4).
- LON-specific config stores: **`LonSiData`** (store 33, bit33) and **`LonAppInitData`** (store 34, bit34) (`:379-395`) — the ShortStack self-identification + application-init data — plus **`SylkCOV`** (store 37, bit37, `BLonSpyderFileOffsetWriter.java:109-123`, log `"Writing Sylk COV store..."`) which carries the LON Sylk COV configuration into the config file `[CERT-a]`.

**Metadata / version → NCIs** `[CERT]` (`BXL10NextGenFileWriter.java:516-585`): instead of BACnet properties, LON writes a `BNetworkConfig` (NCI):
- Device name → NCI `nciDeviceName`, field `name`, then `doForceWrite()` (`:580-585`).
- App GUID + version → NCI `nciApplVerNew`: 16 GUID bytes written as fields `GUID_0..15`, plus `revisionNumber`(+1), `brandID`, `majorVer`, `minorVer`, `typeOfAppl`, then `doForceWrite()` (`:553-573`). `endDownload()` re-stamps the real app UUID + tool brand (`:587-590`).

**ShortStack image** `[CERT]` (`compilation/shortstack/BShortStackImageGenerator.java`): the LON build generates an **Echelon ShortStack** micro-server image — `generateShortStackImage(BLonDevice, …)` (`:148`), `toNetBytes(int)` (`:181`), pulling the per-model `getShortStack()` descriptor (`:128`) and its **`getProgramID()`** (`:625`, the 8-byte LonMark program ID). It is built **at COMPILE time** (`compilation/BLonSpyderCompilation.java:2922`, reflectively-loaded class that must implement `IShortStack` or compile fails with `"CompileInterfaceNotImplemented"` `:2920`) and serialized via `toNetBytes(0)`→store **33 (LonSiData)** / `toNetBytes(1)`→store **34 (LonAppInitData)** (`BLonSpyderCompilation.java:3182-3198`) `[CERT-a]`; those two stores then ride the config LonFile to the device like any other store. Supporting structs: `BSnvtDescriptor`, `BSnvtStructure`, `BCommParamStruct`, `BAliasField`, `BAppInitMiscField`, `BLonSiData`, `BLonAppInitData`. This is the NV/SNVT-table side that has no BACnet analogue.

> **Why this matters**: the LON Spyder is a `BDynamicDevice` of NVs (B77 §77.3), an Echelon **ShortStack**-based node. The control image travels as a *file* (LonWorks FTP), but the node's network-visible interface (SNVTs, program ID, comm params) is described by the ShortStack image and the version/identity by NCIs.

**FB_NV_MODIFIED located** `[CERT-a]`: B77 §77.4 surfaced the `FB_NV_MODIFIED` log strings without pinning the value. The LON NV-restorer sets the compile-status / ShortStack flag to the int **`3`** when an NVO's `sDelta` resets to 0 — `restorer/BNVRestorer.java:248-251` (`"Setting the CompileStatus Flag to FB_NV_MODIFIED, since sDelta value of the NVO is reset to '0'"` / `"Setting ShortStackFlag to FB_NV_MODIFIED..."`). So `FB_NV_MODIFIED == 3`, the same compile-status value B106 §106.3 saw (`setCompileStatusFlag(3)`), which forces a recompile (regenerating the ShortStack) + re-download of the affected config stores.

**Retry / read-back** on LON wraps the same `addRetryDownloadProp` on `LonException` (`:101-104,143-144`), and the LON proxy writer performs the **same read-back-and-CRC-correct** step as BACnet — `download/BKFFileWriter.a(...):150-190` reads each proxy region back (`lonFile.read`), recomputes the CRC, and rewrites the corrected header `[CERT-a]`.

---

## 120.8 — BACnet-object vs LON-NV transport difference (the precise split) `[CERT]`

| Concern | BACnet Spyder | LON Spyder |
|---|---|---|
| Bulk binary transfer | AtomicWriteFile → File objects 301/302/303/304/262/263 (`BBacnetFileUtil.java:78`) | LonWorks file transfer → param/config file numbers (`BXL10NextGenFileWriter.java:206,216`) |
| Transfer gating per file | Program-object halt(90←5)/run(90←1), state(92) (`BBacnetFileUtil.java:207,273`) | none — open/write/close (`:135-153`) |
| Chunking | self-segmented at `maxAPDU-30` (`:71`) | delegated to `LonFile` |
| Device identity / version | properties: objectName(77), apduTimeout(11), retries(73), maxInfoFrames(63); ApplVer in File 302 | NCIs: `nciDeviceName.name`, `nciApplVerNew.{GUID_0..15,revisionNumber,brandID,majorVer,minorVer,typeOfAppl}` + `doForceWrite()` |
| Network interface model | BACnet objects (7 types, B77 §77.2) + data-sharing BOAC bindings (File 263) | NVs/SNVTs + **ShortStack image** (SI/SNVT/comm-param) |
| Read-back / verify | AtomicReadFile + CRC-correction rewrite (`BBacnetKFFileWriter.java:146-164`); upload via readFileData + readPropertyMultiple (`upload/BBacnetUpload.java:1457,1550`) | `BLonUpload`/`BSpyderIILonUpload` read NVs/LonFiles back |
| Integrity | CRC-16 (CCITT) + BOAC table CRC, no signature | CRC-16, no signature |
| Incremental | `getBinaryModified()` BitSet per store | identical BitSet per store |

> **The one-line synthesis**: same compiled image, same per-store layout, same CRC-16-only integrity, same dirty-bit incremental — but **BACnet moves it as AtomicWriteFile into File objects fenced by Program-object halt/run, while LON moves it as LonWorks file transfer with identity/version carried in NCIs and an Echelon ShortStack image generated alongside.**

---

## 120.9 — doc↔code deltas (honest) `[CERT]`/`[INFER]`

- **B77 §77.4 said the download flow was `[INFER]`/`[CERT-a]`** ("produces binary + checksum; writeProxyFile; addRetryDownloadProp"). This block UPGRADES that to `[CERT]`: the transport is BACnet AtomicWriteFile to numbered File objects with a Program-object handshake (BACnet) / LonWorks file transfer + NCIs (LON), with the exact file numbers, retry counts and CRC steps. The B77 `BBacnetKFFileWriter.writeProxyFile` log string `"KF Write - FullDownload flag - "` is verified verbatim (`BBacnetKFFileWriter.java:99`).
- **B116 `[CERT-doc]` said "Compile + Generate XIF"** and capacity limits (Lon Relay 300 FB / BACnet Relay 200 FB). The transfer code here is consistent (relay variants `BBacnetRelayUpload`, `BSpyderRelayCompilation`) but the per-FB capacity numbers live in the model XML, not the download path → those stay `[CERT-doc]`.
- **Per-model store offsets/capacities** (the values behind `getFileVariables(name).getOffset()/getCapacity()`) are read from `BacnetSpyder.xml` at runtime; I confirmed the *mechanism* and the *model→hw_id/model_id* table `[CERT]`, but the individual offset integers were NOT enumerated → `[INFER]` for any specific Model-8 capacity claim (micro-gap, see G6b).
- **A parallel LON sweep sub-agent corroborated and enriched the LON findings** (concrete file numbers, `FB_NV_MODIFIED==3` location, SylkCOV store 37, ShortStack-at-compile-time, LON proxy read-back). The core LON transport claims (LonFile open/write/close, NCIs, ShortStack) are my own direct reads (`[CERT]`); the agent-only refinements I did not re-open are marked `[CERT-a]`. The agent also flagged a useful caveat: the user's framing "LON uses NVs (vs BACnet objects)" is only half-right — LON uses **NVs only for identity/version metadata**; the **bulk binary uses LonWorks file transfer**, not NV writes. This block reflects that corrected understanding.
- **`"Model 8"` is not a runtime `== 8` test in the download path** `[CERT-a]`: branching is by predicate (`isSpyder1Model`, `isSpyderRelayModel`, old-firmware via `getMaxDevicesSupported()==1`); "Model8" is a model-info key (`factory/ModelFactory8.java`). The Model-8 (Micro) distinction is in capacities/offsets, not a numeric branch.

---

## 120.10 — Self-verify + marker tally

**Token checks (load-bearing `[CERT]`, grep/read-confirmed verbatim against source) — 12 checked, 12 pass:**
1. `atomicWriteFileStream(...)` transport call — `BBacnetFileUtil.java:78,115` ✓
2. File-object numbers 301/302/303/304/262/263 — `BBacnetFileWriter.java:90-93`, `BBacnetFileOffsetWriter.java:53`, `:267,298` ✓
3. Program-object handshake `writeProperty(...,90,enum 5/1)` + read 92 — `BBacnetFileUtil.java:213,217,220,279,283` ✓
4. 5-retry + `Thread.sleep(1000L)` + `addRetryDownloadProp` — `BBacnetFileUtil.java:74-97,182-205` ✓
5. `getBinaryModified()` BitSet gating per store — `BBacnetFileWriter.java:402,422,442,462`; `BXL10NextGenFileWriter.java:210,343,361` ✓
6. CRC-correction read-back rewrite — `BBacnetKFFileWriter.java:146-164` ✓
7. BOAC table CRC `calculateCRCChecksumForBOAC` — `BBacnetFileWriter.java:292` ✓
8. LON `LonFile.createFile/open/write/close` transport — `BXL10NextGenFileWriter.java:135-153,205,216,322-323`; `BLonSpyderFileOffsetWriter.java:111,169-171` ✓
9. LON NCIs `nciDeviceName`/`nciApplVerNew` + `doForceWrite()` — `BXL10NextGenFileWriter.java:553-585` ✓
10. ShortStack image gen + `getProgramID()` — `BShortStackImageGenerator.java:148,181,625` ✓
11. Firmware version gates 5.0.0/5.110/6.1.0/6.112/5.113 — `BBacnetDeviceRestorer.java:58,74,77,83`; `BBACnetObjectRestorer.java:147` ✓
12. Model 8 = Micro BACnet family (PVB4024NS…) — `extracted/XL10NextGenXML/BacnetSpyder.xml` `ModelMicroBACnet*` `model_id="8"` ✓

**Marker tally**: ≈ **58 `[CERT]`** · 3 `[CERT-doc]` (inherited from B116) · 7 `[CERT-a]` (LON sub-agent refinements: file numbers, FB_NV_MODIFIED=3, SylkCOV store, ShortStack-at-compile, LON read-back, Model8 predicate) · 5 `[INFER]`. **`[INFER]`/`[CERT]` ratio ≈ 0.09** — evidence-rich; the download/upload packages are clean vineflower and read directly. The only residual gap is the per-model XML capacity values (not code) → micro-gap **G6b**.

**Artifacts**: this block file exists; CATALOG regenerated; INDEX.md + RESEARCH-STATE.md updated; engram mirrored.

---

## 120.11 — Connections

- **[Block 77]** — this block DRILLS INTO B77. B77 gave the device hierarchy + `ISpyder*` contract + the compile→download flow at `[INFER]`/`[CERT-a]`; B120 nails the wire transport, file numbers, Program-object handshake, retry counts and CRC steps as `[CERT]`, and locates the `"KF Write - FullDownload flag"` and `FB_NV_MODIFIED` machinery precisely.
- **[Block 106]** — the OTHER half: B106 is the compiler that produces the FileSection0-5 / CRC-16(+BOAC) binary; B120 is how that binary travels. B120 resolves "FileSection0..5" into named model-offset stores grouped into File objects 301/302/303, and confirms the CRC-only (no signature) integrity at the wire.
- **[Block 116]** — vendor doc; B120's transport is consistent with B116's "Compile + Generate XIF" and the relay capacity limits (which stay `[CERT-doc]`).
- **[Block 115]** — the Spyder→IRM migrator deliberately does NOT use this wire path (offline file-to-file BOG transpile); B120 is the runtime download path the migrated app would later take on the target side.
- **[Block 88]** — Sylk/S-Bus runtime driver; the proxy/wall-module (KF) file (object 303) carries the Sylk wall-module config that B88 communicates physically.
- **[Block 7]** — Tridium driver framework: BACnet AtomicFile + Program object and LonWorks file transfer are framework services the OEM driver consumes, not reimplements (B77 §77.1).
