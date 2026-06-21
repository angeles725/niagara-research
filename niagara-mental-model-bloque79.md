# Bloque 79 — Utilidades Honeywell: suite BACnet (honBACnetUtilities) + config wireless host WiFi/BLE (honAdvWirelessCfg)

> Investigación empírica de dos módulos Honeywell deofuscados, de **dominios distintos** pero ambos extensiones OEM sobre el framework:
> - **`honBACnetUtilities`** — suite de aplicación BACnet (import/export EDE, devices/points/alarms/schedules) **sobre el stack BACnet core de Tridium**.
> - **`honAdvWirelessCfg`** — configurador de **plataforma/host** WiFi + BLE para controladoras Honeywell **Beats** (NO es un driver de protocolo de campo).
>
> Strings descifradas (ZKM); nombres internos `a`/`b`/`c` aún mangled. Clases public, slots, enums y lexicon legibles.
>
> Fuentes: `organized/honBACnetUtilities/{rt,wb}/vineflower/com/honeywell/hon/...` y `organized/honAdvWirelessCfg/{rt,wb}/vineflower/com/Honeywell/honAdvWirelessCfg/...`
>
> Método: sub-agente + **verificación directa** de declaraciones de clase. `[CERT]` = verbatim verificado esta sesión; `[CERT-a]` = cita del sub-agente (lexicon/acciones) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (drivers/utilidades OEM deofuscados), continúa [Bloque 77](niagara-mental-model-bloque77.md)/[Bloque 78](niagara-mental-model-bloque78.md). honBACnetUtilities ATERRIZA [Bloque 7] (BACnet); honAdvWirelessCfg toca [Bloque 10] (Platform).

---

## 79.1 — honBACnetUtilities: suite de aplicación sobre el BACnet core `[CERT]`

A diferencia del Spyder ([Bloque 77]), que ES un device, `honBACnetUtilities` es una **capa de herramientas** que extiende el stack BACnet nativo de Tridium con sabor Honeywell. Declaraciones verificadas:

```java
// runtime (com/honeywell/hon/rt/)
public class BHonBacnetService      extends BComponent    implements BIService {            // bacnetservice/BHonBacnetService.java:117
public class BHonBacnetDevice       extends BBacnetDevice implements BIBacnetObjectContainer {// device/BHonBacnetDevice.java:130
public class BHonBacnetNumericPoint extends BNumericPoint { ... }                            // point/BHonBacnetNumericPoint.java:82
```
Puntos por tipo (todos extienden el control-point core): `BHonBacnetNumericPoint:BNumericPoint`, `...BooleanPoint:BBooleanPoint`, `...EnumPoint:BEnumPoint`, `...StringPoint:BStringPoint` `[CERT-a]`. Alarmas: `BHonBacnetNotificationClass extends BBacnetNotificationClass` `[CERT-a]`.

> Clave: hereda `BBacnetDevice` (mismo core que el Spyder), por lo que el address MS/TP + Device Instance vienen de la pila Tridium. El módulo añade **gestión por encima**: servicio, import/export, jobs.

**Acciones de `BHonBacnetService`** `[CERT-a]` (NiagaraActions): `readPointProperty`, `writePointProperty`, `getPropertySpecifications`, `AddDeviceToPingList`/`Remove`, `createPropertyPoints(BOrd)`, `createBacnetVirtualObject(BOrd)`, `sendIAm()`.
**Acciones de `BHonBacnetDevice`**: `ping`, `importFromEde`, `resetConfig`, `upload`, `download`, `checkTimeOfDeviceRestart`.

---

## 79.2 — honBACnetUtilities: import EDE / export masivo (la función estrella) `[CERT-a]`

El valor del módulo es **carga/descarga masiva de configuración BACnet**:

- **Importador EDE** (proyectos CARE): wizard `BHonBacnetObjectsImporterWizard` ("BACnet Objects Importer") con 4 pasos — `importDataPoints`, `importNotifClasses`, `importSchedules`, `importCalendars`. Lee `.bnt`/`.csv` con delimitador configurable. `[CERT-a]` `honBACnetUtilities-rt` lexicon:268-300.
- **Export job** `BBacnetExportJob extends BSimpleJob` — exporta puntos, schedules, calendars, alarmas, history logs. La vista WB `exportTool` ofrece: Numeric/Boolean/Enum Point, Schedule, Calendar, Alarm Notification Class, History Trend Logs. `[CERT-a]`
- **Discovery** `BHonDiscoverNotificationClassesJob extends BBacnetDiscoverConfigJob` (objectType=15, notification classes). `[CERT-a]`
- Schedules Honeywell: `BHonNumericSchedule`/`BHonBooleanSchedule`/`BHonEnumSchedule`/`BHonCalendarSchedule` + export dedicado. Workbench: `BHonBacnetNetworkView:BWbView`, `BHonBacnetExportView:BWbComponentView`, `BHonCalendarScheduler:BWbView`, editores `hx` (`BHonHxScheduler:BHxScheduler`). `[CERT-a]`

**Errores/licencia** `[CERT-a]`: el servicio **requiere licencia** — `BHonBacnetService.NotLicensed = "Hon Bacnet Service not licensed!"`. Import: `failedImportDataPoints`/`failedImportNotifClasses`/`failedImportSchedules`; `imported`/`notImported`.

> Para el integrador: este módulo es la vía Honeywell para **migrar/poblar** una red BACnet desde archivos EDE (proyectos CARE) en lote, en vez de crear puntos a mano. Gate = licencia `Hon Bacnet Service`.

---

## 79.3 — honAdvWirelessCfg: configuración de plataforma WiFi + BLE (Beats) `[CERT]`

`honAdvWirelessCfg` NO es un driver de campo: es un **servicio de plataforma** que configura la radio del **controlador físico Honeywell Beats** (host), creado 2022. Declaración verificada (paquete `com/Honeywell/honAdvWirelessCfg/`, con `H` mayúscula):

```java
public class BHonWirelessPlatformService extends BPlatformService { ... }  // BHonWirelessPlatformService.java:44  (poll 5s, action refresh)
```

Modela dos tecnologías con jerarquías de `BComponent`/`BVector` `[CERT-a]`:
- **WiFi**: `BBeatsWifiSettings`, `BBeatsWifiNetwork`, clientes/canales/MAC como `BVector` (`BBeatsWifiClientListVector`, `BBeatsWifiChannelListVector`, `BBeatsWifiMacAddressVector`), supplicant (`BBeatsSupplicantNetBlock`).
- **BLE**: `BBeatsBleNetworkVector` + managers BLE.

**Jerarquía de seguridad WiFi** `[CERT]` (herencia real verificada):
```java
public class BBeatsWifiWPA2PSKSettings extends BBeatsWifiWPAPSKSettings { ... }  // :18
//  ↑ WPA2-PSK hereda WPA-PSK hereda BBeatsWifiSecuritySettings
```
Enums de radio `[CERT-a]`: `BBeatsWifiModeConfigEnum` (Off/Client/AccessPoint), `BBeatsWifiFreqBandEnum` (2.4/5/4.9 GHz), `BBeatsHapdHwModeEnum` (802.11a/b/g/n), `BBeatsHtCapabilityEnum` (HT20/HT40), `BBeatsWifiKeyManagementEnum` (NONE/WPA-PSK/WPA-EAP/IEEE802.1x), `BBeatsWifiEncryptionMethod` (NONE/TKIP/CCMP).

---

## 79.4 — honAdvWirelessCfg: modos, estados y validaciones `[CERT-a]`

**Dos modos WiFi**:
- **Client (STA)**: conectarse a un AP existente — `clientModeEnabled`, `wpaState` (attach state), `connectedSsid`, `updateGateway`.
- **Access Point (SAP)**: el controlador ES el AP — SAP IP adapter (IPv4/netmask) + **servidor DHCP embebido** (subnet, lease time, rango de clientes 1-16, máx clientes), SSID/broadcast/passkey/canal/hw-mode/WPA, whitelist MAC, inactivity timeout.

**Máquina de estados WiFi** (monitoreada): `Stopped`/`Failed`, `SAP_Starting`/`SAP_Running`, `STA_Starting`/`STA_Scanning`/`STA_Associating`/`STA_Associated`/`STA_4Way_Handshake`/`STA_Running`/`STA_Disconnected`.

**Seguridad enterprise**: WEP40/104, WPA/WPA-PSK, WPA2/WPA2-PSK, **WPA2-EAP con certificados** (TLS/PEAP/TTLS; CA cert, client cert, private key, Phase1/Phase2).

**Validaciones operacionales (lexicon)** — lo que ve el integrador:
| Validación | Mensaje |
|------------|---------|
| SSID | "Invalid SSID. max 32 chars, ...alphanumeric+'-'/'_'..." |
| Password | "Password must be between 8 and 64 characters" / "must not contain any space" / "is default and it must be modified" |
| DHCP rango | "Valid range is 1 to 16 clients"; "AP adapter IP cannot be in dhcp client range"; "not in the adapter subnet" |
| Red dup | "Network already exists"; "SSID Field can not be empty" |
| Certs | "Invalid Client Certificate Path"; "Need to specify CA directory or file path"; "Can not save CA Cert File and Directory simultaneously" |

**Advertencia irreversible** `[CERT-a]`: `radioConfig.dialog.ccwarningtext = "Setting country code is permanent. This operation cannot be undone!"` — el **country code de radio es permanente**.

**BLE** `[CERT-a]`: status/mode, passcode con validez (start/end), adapter on/off + remaining time, versiones (bootloader/app/BLE/firmware).

---

## 79.5 — Síntesis

| | **honBACnetUtilities** | **honAdvWirelessCfg** |
|---|---|---|
| Dominio | Aplicación BACnet (sobre driver core) | Plataforma/host (radio del controlador) |
| Base | `BBacnetDevice`/`BNumericPoint` + `BIService` | `BPlatformService` |
| Función estrella | import EDE (CARE) + export masivo puntos/schedules/alarms | config WiFi (Client/AP+DHCP) + BLE + seguridad WPA2-EAP |
| Gate | licencia `Hon Bacnet Service` | — (servicio de plataforma) |
| Producto | redes BACnet Honeywell | controladores Honeywell **Beats** |

**Para el integrador:**
1. **honBACnetUtilities** = atajo para poblar/migrar redes BACnet desde EDE en lote; hereda el core Tridium, así que el transporte BACnet es el estándar. Necesita licencia.
2. **honAdvWirelessCfg** = configura la radio del propio controlador Beats (no dispositivos de campo): WiFi como cliente o como AP con DHCP, BLE, seguridad enterprise. Cuidado: **country code permanente** y password default debe cambiarse.
3. Son módulos OEM **fuertemente acoplados a hardware/productos Honeywell**.

**Pendiente conocido**: nombres de clases internas ofuscados (`a`/`b`/`c`) — irreversible (ZKM).
