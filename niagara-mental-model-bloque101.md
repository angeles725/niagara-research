# Bloque 101 — Ecosistema de Air Balancing Honeywell (II): `airFlowBalancer` (VAV genérico LON+BACnet) + `kitCat` (VAV IP del IPC/CIPer, multi-estación), deofuscados

> Investigación empírica de dos herramientas más de **Air Balancing (TAB)** de cajas VAV Honeywell, que completan el ecosistema iniciado en el [Bloque 96] (Venom): **`airFlowBalancer`** (45 java, VAV single-duct genérico sobre LON y BACnet) y **`kitCat`** (48 java, VAV IP del controlador IPC 3036/CIPer, con balanceo multi-estación y frontend web). Las tres usan la misma fórmula `Q = K·√VP` pero apuntan a familias de controlador distintas.
>
> 2 módulos: `airFlowBalancer` (`com.honeywell.flowbalancer`) + `kitCat` (`com.honeywell.kitCat`, con `-rt`/`-ux`/`-wb`/`-doc`).
>
> Fuentes: `organized/airFlowBalancer/.../com/honeywell/flowbalancer/...` + `organized/kitCat/.../com/honeywell/kitCat/...`.
> Método: 2 sub-agentes Explore + **verificación directa** de cada `extends`, la fórmula de calibración, el feature de licencia y el mecanismo de EULA. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (métodos, validator, virtual balancing, estados) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 100]. Familia E del barrido. Conecta [Bloque 96] (Venom, la 1ª herramienta de balancing), [Bloque 99] (IPC/CIPer, target de kitCat), [Bloque 77]/[Bloque 88] (Stryker/ascLon, Sylk), [Bloque 75] (seguridad).

---

## 101.1 — El ecosistema de Air Balancing Honeywell: 3 herramientas `[CERT]`

Las tres calibran cajas VAV con la **misma fórmula** `Q = K·√VP` (zero offset + K-Factor offset), pero son productos independientes para targets distintos:

| Herramienta | Bloque | Target | Protocolo | Métodos | Distintivo |
|-------------|--------|--------|-----------|---------|-----------|
| **Venom** | [96] | Spyder/Stryker | BACnet + LON | KFactor | MixIn dinámico, EULA |
| **airFlowBalancer** | 101 | VAV single-duct genérico | LON + BACnet | MinMax, KFactor, Zero | templates `ComFBTemplate`/`ProdFBTemplate`, validator |
| **kitCat** | 101 | VAV IP (IPC 3036/CIPer) | F1/PVID (BACnet ID) | KFactor + PD/PI + DualDuct | tag-driven `hkc:`, virtual multi-estación, PWA |

Fórmula verificada en las tres `[CERT]`: kitCat `newKF = actualFlow / Math.sqrt(deviceVP)` (`BBalanceFlowCalibrator.java:226`), igual que Venom (`BVenomBalanceFlowCalibrator:146`).

---

## 101.2 — `airFlowBalancer`: calibración activa VAV genérica LON+BACnet `[CERT]` + `[CERT-a]`

Herramienta de **calibración activa** (escribe al controlador) de VAV single-duct. Clases raíz verificadas `[CERT]`:

| Clase | `extends` verificado (archivo:línea) |
|-------|--------------------------------------|
| `BWsVavBalancer` | `extends BComponent` (`…:22`, servicio singleton) |
| `BAttainSetpointFlowJob` | `extends BSimpleJob` (`…:26`) |
| `BVAVSingleDuctActionsTemplate` | `abstract extends BComponent` (`device/templates/…:27`) |
| `BBacnetVAVSingleDuctActionsTemplate` / `BLonVAVSingleDuctActionsTemplate` | `extends BVAVSingleDuctActionsTemplate` (`…:16`/`:18`) |
| `BFlowBalancingView` | `extends BWbComponentView` (`ui/…:81`) |

**3 métodos `[CERT-a]`** (`BBalancingMethod extends BFrozenEnum`, verificado `[CERT]`: MinMax/KFactor/Zero/None): MinMax (registra flujo a máx/mín), KFactor (`K = measuredFlow/√P`, escribe K-Factor), Zero (cierra, mide offset residual, escribe `SensedCoolAirPressureOffset`). **Abstracción dual de protocolo** vía templates que mapean a NVs LON (`nciKFactor`, `nvoBoxFlow`…, con soporte de 2 generaciones de firmware) o objetos BACnet (`CfgKFactor`, `BoxFlow`… vía `ProdFBTemplate`). Detecta Stryker/`ascLon` (compat ≥1.3.0) `[CERT-a]`. `validator` (16 clases, el paquete mayor): valida **parámetros de entrada del usuario** (rangos 0-32535 para kFactor/flow/setpoints), no consistencia del sistema; incluye un `StringValidator` genérico (email/IP/URL) reusado de una lib Honeywell más amplia.

---

## 101.3 — `kitCat`: balancing del IPC/CIPer, tag-driven y multi-estación `[CERT]` + `[CERT-a]`

Herramienta para VAV **IP del controlador IPC 3036/CIPer** ([Bloque 99]) — feature de licencia **`"IPVAV"`** (verificado `KitCatLicense.java:40`). Opera en 2 capas: `BBalancingService` en el supervisor + Function Blocks F1 dentro del IPC. Clases raíz verificadas `[CERT]`:

| Clase | `extends` verificado (archivo:línea) |
|-------|--------------------------------------|
| `BBalancingService` | `extends BComponent implements BIService, BIRestrictedComponent` (`balance/…:170`) |
| `BKitCatPassThru` | `abstract extends BFunctionBlock implements IHoneywellComponent` (`componentLibrary/f1/…:34`) |
| `BVirtualBalancingService` | `extends BVirtualBalanceComponent` (`balance/virtual/…:68`) |
| `BVirtualBalanceGateway` | `extends BVirtualGateway` (`balance/virtual/…:30`) |
| `BKitCatTagDictionary` | `extends BSmartTagDictionary` (`tagging/…:35`) |

**Function Blocks F1 `[CERT-a]`**: `BKitCatPassThru extends BFunctionBlock` (+ `BNumeric/Boolean/EnumPassThru`) son bloques de la librería F1 que viven **dentro del programa de control del IPC** y exponen señales al árbol Niagara; su `xfer()` pone `OUT` en `fault` si no hay licencia. `BVavInletConversion extends BNumericPoint` convierte diámetro→área. `BTcpIpSettingsToDeviceId` deriva el BACnet Device ID de los últimos octetos de la IP del adapter `fec0` (esquema del IPC 3036).

**Tag-driven `[CERT-a]`**: namespace `hkc:` con 25 marker tags; el descubrimiento es por **NEQL** (`"hkc:BalancingTool AND hkc:KFactor"`), sin ORDs hardcodeados. Soporta 4 tipos de caja: PD (presión dependiente, damper %), PI (presión independiente, Q=K√VP), y sus variantes Dual Duct (cold+hot deck) — jerarquía `BBalanceData` → `BBalanceDataPresDep`/`PresInd` → `…DD`.

**Virtual balancing `[CERT-a]`**: NO es simulación — es **balanceo multi-estación desde un supervisor central**. `BVirtualBalanceGateway` descubre estaciones remotas por BQL (`niagaraDriver:NiagaraStation`) y crea proxies `BVirtualBalancingService` que espejan el servicio remoto vía componentes virtuales Niagara. `BBalanceZeroCalMgmtJob extends BSimpleJob` coordina zero-cal en paralelo de a pares (evita interacción de presión entre cajas adyacentes). Frontend: PWA BajaScript (`kitCat.built.min.js`, `BalanceManager.js`).

---

## 101.4 — La EULA compartida y bypasseable `[CERT]`

`kitCat` reusa el **mismo patrón de EULA que Venom** ([Bloque 96]): producto licenciado con aceptación verificada solo por la existencia de un fichero de propiedades. Verificado: `KitCatEULA.properties` (`IKitCatEula`), clave `CREATED`, flag `static wasAlreadyChecked`, path con `"\\"` (`BKitCatEulaDialog.java:43`, **solo Windows**). Bypass idéntico: crear el fichero a mano con `CREATED=1`. En JACE Linux el path con backslash hace que la EULA nunca se marque aceptada.

---

## 101.5 — Seguridad `[CERT]` + `[CERT-a]`

**airFlowBalancer:**
- **[BUG CERT] Condición de validación imposible.** `BVAVSingleDuctActionsTemplate.java:567`: `if (factoryKFactor <= 0.0 && factoryKFactor >= 10.0)` — ningún número cumple ambas (debió ser `||`); el reset del K-Factor offset inválido nunca ocurre.
- **[BAJO CERT-a]** `==` para comparar strings en `DynamicBean` (`"Infinity"` nunca detectado); excepciones de escritura tragadas con `printStackTrace()` → el balanceo puede "completar" en la UI sin escribir al device; actuator travel time hasta 500 s con la caja en override.

**kitCat:**
- **[ALTO CERT] EULA bypass** (igual que Venom: fichero sin firma + flag static + path Windows).
- **[MEDIO CERT-a] Restauración de setpoints sin auth dedicada.** `doRestoreBalanceData()` escribe el slot `fallback` de todos los `BControlPoint` del backup (action `restoreBalanceData`, hidden pero invocable vía Fox) → sobrescribir setpoints de todas las VAV del edificio.
- **[MEDIO CERT-a] Reboot del IPC desde Niagara.** `BModifyTcpIpSettings.doCopy()` puede invocar `reboot` de la plataforma → pérdida temporal de control. Licencia evaluada una sola vez en class-load (sin recovery en caliente).

---

## 101.6 — Conexiones

- **[Bloque 96]** (Venom): la 1ª de las 3 herramientas de balancing. kitCat reusa su patrón de EULA; los tres comparten `Q = K·√VP`.
- **[Bloque 99]** (IPC 3036/CIPer): el target de kitCat — los PassThru F1 corren dentro del controlador, BACnet ID derivado de `fec0`.
- **[Bloque 77]/[Bloque 88]** (Stryker/ascLon, Sylk): airFlowBalancer detecta Stryker/ascLon; ambas tocan VAV genéricas.
- **[Bloque 75]** (seguridad): suma EULA bypass (×2) + escritura de setpoints sin auth + bugs de validación.
