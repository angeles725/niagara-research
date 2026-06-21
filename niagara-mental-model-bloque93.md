# Bloque 93 — `knxnetIp` (vendor tridiumX): driver KNXnet/IP completo (tunneling/routing/management) + import de proyectos ETS `.knxproj` + modelado DPT, deofuscado

> Investigación empírica del módulo **`knxnetIp`** (325 java, vendor de terceros **`tridiumX`** — no Tridium core ni Honeywell): un **driver KNXnet/IP completo** para Niagara N4 — KNX sobre Ethernet/IP (EN 50090). Cubre los modos tunneling/routing/device-management/discovery, el framing KNXnet/IP + cEMI, el modelado de **Datapoint Types (DPT)**, y el **import de proyectos ETS** (`.knxproj`).
>
> 1 módulo (`knxnetIp-rt` + `-wb`). Paquetes: `com.tridiumX.knxnetIp.{driver, comms, comms/frames, comms/cemi, point, knxDataDefs, knxSpec, ets, ui, wb}`.
>
> Fuentes: `organized/knxnetIp/knxnetIp-rt/vineflower/com/tridiumX/knxnetIp/...` (+ `docKnxnetIp` para doc).
> Método: 1 sub-agente Explore + **verificación directa** de la cadena `extends` raíz (network/device/proxyExt), el puerto `KNXNETIP_PORT_NUMBER`, la BCU key default, la dirección multicast y el feature de licencia. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (service types, cEMI, DPT, flujo ETS, acciones) no re-verificada; `[INFER]` = deducción (zip-slip/XXE).
>
> Capa 22 (OEM deofuscados), continúa [Bloque 92]. **Distinto vendor: `tridiumX` (tercero), no Honeywell.** Contrasta con [Bloque 78] (C-Bus/EnOcean usan el **N-driver**): este usa el **driver framework estándar** de Niagara (`javax.baja.driver`). Conecta [Bloque 7]/[Bloque 32] (drivers de campo), [Bloque 75] (seguridad).

---

## 93.1 — Qué es + clases raíz verificadas `[CERT]`

Driver KNXnet/IP sobre el **framework de driver estándar** de Niagara (`javax.baja.driver`, **no** el N-driver del [Bloque 78]). Jerarquía verificada:

| Clase | Declaración verificada (archivo:línea) | Rol |
|-------|----------------------------------------|-----|
| `BKnxNetwork` | `final extends BDeviceNetwork implements BIService` (`driver/BKnxNetwork.java:68`) | Red KNX (una sola por station) |
| `BKnxDevice` | `final extends BDevice implements BIIncludeInTrace` (`driver/BKnxDevice.java:106`) | Gateway KNX IP |
| `BKnxProxyExt` | `abstract extends BProxyExt implements BIKnxPollable` (`point/BKnxProxyExt.java:99`) | Base de todos los puntos |

ProxyExt concretos: `BKnxBoolean/Numeric/Enum/StringProxyExt` `[CERT-a]`. **Licencia `[CERT]`**: `getFeature("tridium", "knxnetIp")` (`BKnxNetwork.java:163`) — vendor de licencia `"tridium"`, feature = nombre del módulo. Un `BKnxDevice` opera en 3 modos de conexión (`BConnectionMethodEnum`): directo (tunneling propio), proxy (reusa conexión de otro device), o routing vía instalación `[CERT-a]`.

---

## 93.2 — Frames KNXnet/IP + cEMI `[CERT]` (puerto/multicast) + `[CERT-a]`

**Puerto `[CERT]`**: `KNXNETIP_PORT_NUMBER = 3671` (`knxSpec/KnxSpec.java:68`, UDP/TCP). **Multicast `[CERT]`**: `SYSTEM_SETUP_MULTICAST_ADDRESS = "224.0.23.12"` (`:69`, usado en `BKnxInstallation`).

**Header KNXnet/IP `[CERT-a]`** (6 bytes): `headerSize=0x06`, `protocolVersion=0x10` (v1.0), `serviceType` (2B), `totalSize` (2B). Validación rechaza `protocolVersion != 0x10`.

**Service types `[CERT-a]`** (`BKnxIpFrameTypeEnum`): core search/description/connect/connectionstate/disconnect (0x0201–0x020A), device config request/ack (0x0310/0x0311), **tunnelling** request/ack (0x0420/0x0421), **routing** indication/lost/busy (0x0530–0x0532).

**cEMI `[CERT-a]`** (`comms/cemi`): `CemiMessage` (message code + ctrl1/ctrl2 + src/dst + NPDU), `CemiTpdu`/`CemiApdu`/`GroupValueApdu`. Message codes `L_Data_req=0x11`, `L_Data_con=0x2E`, `L_Data_ind=0x29`. `BTunnelConnection` hace el handshake completo (CONNECT → TUNNELLING con ACK secuenciado → CONNECTIONSTATE heartbeat 60 s → DISCONNECT, confirmación L_Data_con timeout def 3 s).

---

## 93.3 — Modelado de Datapoint Types (DPT) `[CERT-a]`

Clase central `BDataValueTypeDef`: cada instancia = un DPT (`kNX_ID` "DPST-9-1", `comObjectSize`, `mSBitOffset`, `encodingFormat`, `coefficient`, flags datetime/string). **Encoding formats** (`BKnxEncodingFormatEnum`): `bBoolean`(1.x), `uUnsignedInteger`(5.x/7.x), `vSignedInteger`(6.x), `fFloatingPoint`(9.x F16/14.x F32), `nEnumeration`(20.x), `aCharacter`(4.x), `tUnicodeString`(16.x), `hHexBytes`(raw).

**Codec** (`KnxCodecFuncs`): `decode/encodeFloat16` implementa el formato KNX **F16** (mantissa 11b + exp 4b + signo, valor = mantissa·2^exp/100, NaN=0x7FFF) y F32 IEEE-754. Los facets Niagara del punto se aplican dinámicamente desde el DPT (`BFacetDef` → `BKnxProxyExt.checkDataValueTypeFacets()`). Librería global `BKnxDataDefs` (con `version`+`signature` de integridad), contenedor por station `BKnxStationDataDefs`. DPT por defecto para GA sin tipo: `"DPST-1-1"` (boolean).

---

## 93.4 — Import de proyectos ETS `[CERT-a]`

Importa proyectos del **ETS (Engineering Tool Software)** del KNX Association. Formato `.knxproj` = **ZIP con XML** (`knx_master.xml` + topología + datos de fabricante). Interpreta por versión de namespace: `PreVersion20Interpreter` (<2.0), `Version20Interpreter` (2.0), `EtsProjectInterpreter` (posterior).

**Flujo GA→puntos `[CERT-a]`**: `BEtsProject` → `BEtsInstallation` → `BEtsGroupRange` → `BEtsGroupAddress` (address 16-bit + `datapointType` + nombre) → crea `BImportedPoint` (con `setDataValueTypeId` + `setGroupAddresses`) organizados en `BImportedPointGroup` reflejando la jerarquía de group ranges. Los datos de fabricante (`BEtsManufacturerData` → `BEtsApplicationProgram` → `BEtsComObject`/`BEtsParameter`) enriquecen los puntos. `.knxproj` con contraseña → `EncryptedZipInputStream` (ZipCrypto).

**point/actions `[CERT-a]`**: base `BKnxProxyExt` expone `pollNow` (A_GroupValue_Read), `writeNow`, `dump`. Acciones por DPT: switch (`On/Off/Toggle/Set/Control`, 1.x), step (`StepUp/Down/Break`, 3.x dimming/blinds), counter (`Increment/Decrement/Reset/Preset`). El write va: `encodeToBytes` → `GroupValueApdu(Write)` → `CemiMessage(L_Data_req)` → `BTunnelConnection.sendLDataWriteRequest()`.

---

## 93.5 — Seguridad `[CERT]` + `[CERT-a]` + `[INFER]`

**[ALTO CERT-a] Sin autenticación KNXnet/IP + sin KNX Secure.** El protocolo clásico no autentica frames; el módulo **no implementa KNX IP Secure ni KNX Data Secure** (cero clases/imports/constantes relacionadas). Cualquier host de la red puede inyectar `TunnelRequest`/`RoutingIndication`. Multicast `224.0.23.12:3671` sin control de acceso `[CERT]`.

**[MEDIO CERT] BCU key de fábrica hardcodeada.** `KnxSpec.java:169`: `k_BCUKey_Default = "4294967295"` (= `0xFFFFFFFF`, la BCU key de fábrica KNX = "sin clave"). Si se usa como fallback sin forzar cambio, cualquier herramienta KNX estándar se autoriza en los dispositivos.

**[BAJO-MEDIO CERT-a] ZipCrypto débil** para `.knxproj` con contraseña (`EncryptedZipInputStream`, esquema Traditional PKWARE — vulnerable a known-plaintext, no AES-256).

**[MEDIO INFER] Zip-Slip + XXE al importar `.knxproj`.** El parsing compara nombres de `ZipEntry` por igualdad pero no canonicaliza rutas (`../`) — la mitigación depende de `BZipSpace` de Niagara, no del driver. El XML se parsea con `XParser.make(stream)` sin deshabilitar explícitamente entidades externas → posible XXE si `XParser` no protege internamente. Ambos requieren un `.knxproj` malicioso importado por un operador.

---

## 93.6 — Conexiones

- **Vendor `tridiumX`** (tercero), no Honeywell ni Tridium core — único de su tipo en el corpus hasta ahora.
- **Contrasta con [Bloque 78]** (C-Bus/EnOcean = N-driver): KNX usa el **driver framework estándar** (`javax.baja.driver`), no el N-driver.
- **[Bloque 7]/[Bloque 32]** (drivers de campo): suma KNX al inventario de protocolos de campo soportados.
- **[Bloque 75]** (seguridad): aporta el caso "protocolo de campo sin cifrado + BCU key de fábrica + parsing de archivo de proyecto" (zip-slip/XXE).
