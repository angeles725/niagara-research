# 01 — Validación de licencias en Niagara N4: cómo funciona, modos de fallo y diagnóstico

> **Propósito**: documento de diagnóstico y despliegue. Describe cómo Niagara N4 valida licencias
> (entradas, qué verifica, modos de fallo) para que un integrador pueda diagnosticar licencias que no
> activan, se pierden o expiran, y desplegar licencias propias de forma correcta.
> **No** incluye cómo evadir la validación ni cómo generar claves.

Cada afirmación cita su fuente del corpus con `archivo:línea`. Marcadores de certeza: `[CERT]`
verificado contra binarios/instalación, `[CERT-doc]` documentación oficial, `[INFER]` inferencia.

---

## 1. Arquitectura: dónde vive la validación

La validación de licencias en N4 es de dos capas:

1. **Capa Java (autoridad de autenticidad)** — todo vive en **`baja.jar`** bajo
   `com/tridium/sys/license/`: `LicenseUtil`, `NLicenseManager`, `BSMANotificationSettings`,
   `dom/Feature` y `subscription/SubscriptionLicenseManager`
   (`niagara-mental-model-bloque41.md:329-344`). **No existe un `license-rt.jar` separado**
   (corrige la suposición del Bloque 32; `niagara-mental-model-bloque41.md:516,521`).
   Esta capa es la que verifica la firma DSA, el `hostId` y las expiraciones, y es la que
   autoriza/deniega features.
2. **Capa nativa (fast-path de presencia)** — `LicenseUtil::isFeaturePresent(vendor, feature)` en
   `nre.dll` (`niagara-mental-model-bloque126.md:157-158`): hace **text-match** de las cadenas
   `<license vendor="%s"` y `<feature name="%s"` sobre cada `.license` de
   `\security\licenses`, **sin verificar la firma** (`niagara-mental-model-bloque126.md:160-178`).
   La usa el launcher para el gate de Java agents; la seguridad real está en la capa Java
   (`niagara-mental-model-bloque126.md:169-177`).

**API pública**: `javax.baja.license.LicenseManager` con `checkFeature(vendor, feature)` que lanza
`FeatureNotLicensedException`; se obtiene con `Sys.getLicenseManager()`
(`niagara-mental-model.md:171-185`; `notes/02-licensing.md:246`).

**Superficie de monitoreo**: MBean JMX `com.tridium.sys.license:type=LicenseManager`
(`niagara-mental-model-bloque20.md:293`).

---

## 2. El archivo de licencia (`.license`)

### 2.1 Formato XML

Root con atributos (`notes/02-licensing.md:30-49`):

| Atributo | Requerido | Semántica |
|---|---|---|
| `version` | sí | Versión del formato de licencia |
| `vendor` | sí | Vendor; debe existir un `{vendor}.certificate` que lo respalde |
| `hostId` | sí | Binding de máquina (ver §4) |
| `expiration` | sí | `YYYY-MM-DD` o `never` — expiración maestra |
| `generated` | sí | Fecha de generación; si `now < generated` el archivo es inválido (anti reloj atrasado) |

Hijos: `<feature name="…"/>` (obligatorio `name`; `expiration` opcional que sobreescribe la maestra
si es más restrictiva; atributos libres `count="10"`, `point.limit`, `device.limit`, `sma.exempt`,
`brandId`, … interpretados por cada módulo) y `<signature>base64</signature>`
(`notes/02-licensing.md:51-58`).

**Instancia real** (HoneywellCentraLine.license, `niagara-mental-model-bloque126.md:146-152`):

```xml
<license vendor="HoneywellCentraLine" expiration="2027-03-31" hostId="Win-6E6E-10AC-D1DD-8276"
         version="4.15" generated="2026-04-02">
  <feature name="clCbus" expiration="2027-03-31" history.limit="none" point.limit="none"
           schedule.limit="none" device.limit="none"/>
  <signature>MC4CFQDOSizKvGQPhgjQ7JjqUSRDEDz3Zg…</signature>  <!-- firma elidida; valor íntegro en niagara-mental-model-bloque126.md:146-152 -->
</license>
```

### 2.2 Firmas

- La `<signature>` es **SHA-1 con DSA**: el base64 decodifica a DER
  `SEQUENCE { INTEGER r(20B), INTEGER s(20B) }` — DSA con `q` de 160 bits
  (`niagara-mental-model-bloque126.md:154`).
- La raíz de confianza `Tridium.certificate` lleva `<publicKey algorthm="DSA">` (typo verbatim) con
  OID `1.2.840.10040.4.1` (id-dsa), primo de **1024 bits**, `generated="2003-07-16"`
  `expiration="never"` (`niagara-mental-model-bloque126.md:154`; `niagara-mental-model-bloque27.md:231`).
- Cadena de confianza de 2 niveles: **Tridium (root DSA) → certificate del vendor → firma del
  `.license`** (`notes/02-licensing.md:125`).
- Verificación Java: `LicenseUtil.verify(byte[], byte[], Version)` selecciona entre **dos claves
  públicas estáticas** (`masterPublicKey` legacy y `version2PublicKey`) según la versión del archivo
  (`niagara-mental-model-bloque41.md:373-388`).
- Motor nativo: `DsfSha1WithDsaSignature` de `dsfspi.dll` (librería **Mocana DSF expuesta como Java
  security provider**, JCE) con `parseDSAPublicKey`/`parseDSASignature`/`parseDERInteger`
  (`niagara-mental-model-bloque126.md:53,157`).
- Los `.license`/`.certificate` contienen **solo claves públicas y firmas** (públicas por diseño); no
  hay claves privadas en ellos (`niagara-mental-model-bloque126.md:176`).

> Nota de madurez criptográfica: DSA-1024/SHA-1 (raíz de 2003) protege *integridad de la licencia*,
> no confidencialidad. El esquema de licencias es el eslabón más débil de la plataforma frente a los
> módulos (RSA-2048) y binarios (Authenticode RSA-4096) (`niagara-mental-model-bloque126.md:168-176`).

### 2.3 Ubicación en disco

- `/security/licenses/` (raíz) y `/security/licenses/db/{hostId}/` (fuente canónica por host)
  (`niagara-mental-model.md:121`; `notes/02-licensing.md:18`; `niagara-mental-model-bloque40.md:599-610`).
- Los archivos de la raíz son **aliases bit-exactos** de `db/<hostId>/` del host actual (diff
  idéntico) (`niagara-mental-model-bloque40.md:607-610`; `notes/bloque40-D-lib-security.md:243`).
- Si el host actual solo tiene licencias con `hostId` ajeno, el arranque falla con
  `LicensingException: no valid license for hostId Win-…` — **`db/` no es fallback**
  (`notes/bloque40-D-lib-security.md:319`).
- Import: el cliente deja el `.license` en `licenses/inbox/`; LicenseManager lo detecta, lo valida
  (firma + hostId + cert) y lo mueve a `licenses/db/<hostId>/`, borrando la anterior en reemplazo
  (`niagara-mental-model-bloque40.md:683-691`; `notes/bloque40-D-lib-security.md:321-329`).
- Distros multi-host: el install puede traer juegos por host, p. ej. `Qnx-TITAN-…` para el
  controlador JACE/QNX, que el supervisor puede provisionar/sincronizar
  (`niagara-mental-model-bloque40.md:590-610`).

---

## 3. Pipeline de validación al arranque

Validación **por archivo completo** con 5 checks (`notes/02-licensing.md:60-67`):

1. `hostId` del archivo == HostId de la máquina → si no, rechazo.
2. `generated` ≤ ahora → si `now < generated`, rechazo (anti reloj atrasado).
3. `expiration` maestra > ahora → si no, rechazo.
4. Resolver `vendor` → debe existir `{vendor}.certificate`.
5. Verificar `<signature>` con la clave pública del certificate → si no, rechazo.

Si **cualquier** check falla, **todo el archivo se descarta** — no se cargan features parcialmente
(`notes/02-licensing.md:68`). Solo tras pasar se fusionan las features a la base de licencias global.

Además, al importar por inbox la validación es la misma (firma + hostId + cert)
(`niagara-mental-model-bloque40.md:683-691`).

---

## 4. HostId: el binding de máquina

- La licencia nombra un `hostId="Win-6E6E-10AC-D1DD-8276"` y **solo es válida en ese host**
  (`niagara-mental-model-bloque126.md:153`; `notes/02-licensing.md:23`). Mover el `.license` a otra
  máquina lo invalida automáticamente (`notes/02-licensing.md:23`).
- Formato: regex `^(Qnx|Win)-[A-Z0-9-]{14,19}$` — prefijo de OS + hash opaco de 16 hex en 4 grupos
  (`niagara-mental-model-bloque40.md:654-666`; `notes/bloque40-D-lib-security.md:306-317`).
  En Qnx el prefijo embebe el modelo de hardware (`Qnx-TITAN-…`), que no es parte del hash
  (`niagara-mental-model-bloque40.md:660-664`).
- Derivación nativa: `nre.dll`/`njre.dll` usan `GetVolumeInformationA` (KERNEL32) y emiten
  `ERROR: Host Id cannot be found/generated.` con marcadores de debug `>>> hostid.debug >>>` /
  `nre_hostid_debug` (`niagara-mental-model-bloque124.md:68,103,173`). Accesor C++
  `NreWin32::getHostId` y JNI `Java_com_tridium_nre_platform_NativePlatformProvider_getHostId0`
  (`niagara-mental-model-bloque124.md:103`).
- Bridge JNI decompilado (`getHostId0`): llena un buffer de 0x40 (64 bytes) y lo envuelve como UTF
  string (`niagara-mental-model-bloque125.md:136-142`).
- Lado Java: `BSystemPlatformService.getHostId()` (platform-rt) es lo que los validadores comparan
  (normalizado con `.toUpperCase()`) (`niagara-mental-model-bloque139.md:92-93,127`).
  El HostId también viaja por Fox en el `hello` (`Nre.getHostId()`)
  (`sources/decompiled/fox-rt/com/tridium/fox/sys/BFoxConnection.java:200`).
- **Cambia con el hardware/identidad**: NIC swap, clonado de VM, cambio de MachineGuid → el `hostId`
  derivado cambia y las licencias dejan de matchear. Hay regeneración de NRE-id post-clonado
  (`CLONED_FILE` detecta colisión de NRE-id; `niagara-mental-model-bloque41.md:534`).
- Caso especial: auto-licencia por prefijo de host — p. ej. `honBacnetHelper` autoriza si
  `Sys.getHostId().startsWith("HON-NADV")`, y si no cae a `getFeature("Honeywell", …).check()`
  (`niagara-mental-model-bloque246.md:38-45`).
- Privacidad: `getHostId()` se envía como query param en claro a `weather.niagaramodules.com`
  (hallazgo de exposición de información) (`niagara-mental-model-bloque60.md:509,531-542`).

> **Gap del corpus**: no hay descripción del algoritmo exacto del hash de 32 vs 64 bits; los hechos
> nativos registrados son la entrada (`GetVolumeInformationA`), el buffer de salida de 64 bytes y el
> formato opaco `Win-`/`Qnx-` (`niagara-mental-model-bloque126.md` passim).

---

## 5. Features y SMA (Software Maintenance Agreement)

- Una feature = `<feature name="…">` con atributos. Todo acceso es **string-based con defaults**:
  `Feature.get/get(String,String)/getb/geti`, `list()`, `load/save(XElem)`
  (`niagara-mental-model-bloque41.md:348-361`).
- Presencia en runtime: `checkFeature(vendor, name)` lanza `FeatureNotLicensedException` si la
  feature falta, está vencida o es inválida (`niagara-mental-model.md:98-102`). Patrón canónico de
  gate en módulos: `Sys.getLicenseManager().getFeature("tridium","modbusTcp")`
  (`sources/decompiled/modbusTcp-rt/com/tridium/modbusTcp/BModbusTcpNetwork.java:51-53`).
- **SMA no es una feature, es un atributo** (`niagara-mental-model.md:88`): `sma.exempt="true"` →
  funciona sin SMA activa; `sma.exempt="false"` → exige SMA vigente
  (`niagara-mental-model.md:90-96`). Formas vistas: `sma="YYYY-MM-DD"` y
  `sma.expiration="YYYY-MM-DD"` + `sma.exempt="true|false"` (`niagara-mental-model-bloque32.md:287-300`).
- Master SMA por vendor: `NLicenseManager.getLicenseMaintenanceExpiration(vendor)` devuelve los
  millis de expiración del `.license` de ese vendor (`notes/02-licensing.md:136-140,162-165`).
- Chequeo típico (httpClient `LicenseUtil.java`): `isSmaExempt()` =
  `getLicense().getb("sma.exempt", false)`; `checkSma()` lanza `FeatureNotLicensedException(NO_SMA_MSG)`
  si `!isSmaExempt() && sma.get() < System.currentTimeMillis()` (`notes/02-licensing.md:149-174`).
- **Enforcement suave**: SMA alarma pero **no detiene** la operación del módulo (a diferencia de la
  expiración de feature, que es dura); gatea upgrades y updates de nCloud
  (`notes/02-licensing.md:188-189`; `niagara-mental-model-bloque32.md:311-320`).
  `BSMAExpirationMonitor` es un `BComponent` pasivo (solo alarma, no bloquea)
  (`niagara-mental-model.md:104-110`).
- **Gotcha**: atributos string-based y **typo-prone silencioso** — `feature.getb("Sma.Exempt", false)`
  devuelve `false` sin error (`niagara-mental-model-bloque41.md:363-365`).
- **Gotcha de reloj**: SMA se evalúa contra `Clock.time()` **local**; un reloj **atrasado** hace que
  una SMA vencida parezca vigente localmente (el check `sma.get() < now` usa el reloj local). El
  servidor de nCloud verifica con su **propio** reloj — el reloj local no engaña a nCloud
  (`niagara-mental-model-bloque32.md:339,341-345`).

---

## 6. Límites de runtime (point/device/history/schedule counting)

- `NLicenseManager` agrega contadores: `getPointCount()`, `getDeviceCount()`, `getHistoryCount()`,
  `getScheduleCount()`; se actualizan en `added()`/`removed()` de `BComponent.add()/addChild()`
  (`niagara-mental-model-bloque14.md:43-55`). En el tope, `NLicenseManager.checkBeforeAdd()` REJECT
  (`niagara-mental-model-bloque14.md:110-117`).
- Qué cuenta / qué no (`niagara-mental-model-bloque14.md:26-41`):
  - Cuenta: cada `BControlPoint` instanciado local (1 punto, sin importar tipo RW/RO), proxy point
    con `BProxyExt` bound, puntos virtuales/kitControl con `out` **si implementan `BIPointCountable`**,
    punto offline (hasta borrarlo).
  - No cuenta: template no instanciado, `BHistoryExt`/`BAlarmSourceExt` (dimensión aparte), BLink/
    folder/service/network.
  - **Virtual points NO inflan `getPointCount()`**: `BPointCountVisitor` salta `BVirtualComponentSpace`
    (`niagara-mental-model-bloque28.md:1274-1289`); confirmado en vivo con 50 subordinates federados
    (`niagara-mental-model-bloque28.md:1293`).
  - **Federación cuenta en origen**: los puntos exportados Sub→Super cuentan solo en el origen;
    `foxStream.limit` en el endpoint consumidor; archivo de historias en el `history.limit` del origen
    (`niagara-mental-model-bloque14.md:129-133,205-217,683`).
- Límites conocidos: `point.limit`, `device.limit`, `history.limit`, `historyExt.limit`,
  `historyRecord.limit`, `schedule.limit`, `camera.limit`, `foxStream.limit`, `zone.limit`
  (Honeywell), `proxyext.limit`, `algorithm.limit`, `alert.limit`, `Dictionary.limit` (tags, default 2)
  (`niagara-mental-model-bloque14.md:63-80`; `niagara-mental-model-bloque269.md:29-46`).
  `"none"` = ilimitado; atributo omitido = ilimitado por default (`niagara-mental-model-bloque14.md:64`).
- Comportamiento en el tope: crear un punto con count==limit lanza `LicenseLimitExceededException` en
  `BComponent.add()` (`niagara-mental-model-bloque14.md:84-87,110-117`). **No hay gracia** para
  counts (la gracia de 24-48 h es solo para expiración de features y subscription offline)
  (`niagara-mental-model-bloque14.md:94-96,681`).
- Backup/restore con más puntos que el límite **aborta** (sin restore parcial) — ej.: falla en el
  punto 5001 (`niagara-mental-model-bloque14.md:100-108`).
- Alarmas automáticas: `PointLimitExceeded`, `DeviceLimitExceeded`, `HistoryLimitExceeded`,
  `ScheduleLimitExceeded`, `LicenseExpired`, `LicenseExpiresIn` (`niagara-mental-model-bloque14.md:227-233`).
- Ejemplos reales del install: `nCloudDriver` `history.limit="500" point.limit="1000" device.limit="1"`;
  `eSignature` `point.limit="500"`; `maxproVideo` `camera.limit="16" foxStream.limit="none"`;
  `smartKey` `device.limit="6"`; `developer` `moduleDev="true" skipModuleValidation="true"`;
  `station` `station.limit="128"` (`niagara-mental-model-bloque14.md:148-194`; `notes/02-licensing.md:80`).

---

## 7. Modos de fallo — tabla maestra

| # | Síntoma | Causa raíz | Dónde confirmar |
|---|---|---|---|
| 1 | Station no arranca; `LicensingException: no valid license for hostId Win-…` | El host actual no tiene licencia con su `hostId` (raíz = alias de `db/<hostId>/`; `db/` no es fallback) | `notes/bloque40-D-lib-security.md:319` · log de arranque |
| 2 | Licencia "no activa" tras NIC swap / clonado de VM | El `hostId` derivado cambió (MAC/MachineGuid) | Regenerar NRE-id post-clonado; `CLONED_FILE` detecta colisión (`niagara-mental-model-bloque41.md:534`) |
| 3 | Archivo descartado entero aunque una sola feature parezca bien | Validación all-or-nothing por archivo: cualquier check falla → descarte total | `notes/02-licensing.md:60-68` |
| 4 | `generated` en el futuro → inválido | Reloj de la máquina atrasado al momento de validar (o archivo manipulado) | Check 2 del pipeline; `generated` vs `now` |
| 5 | Feature vencida lanza `FeatureNotLicensedException` | `expiration` maestra o de feature < ahora | `notes/02-licensing.md:253`; alarmas `LicenseExpired`/`LicenseExpiresIn` |
| 6 | Feature no aparece aunque el archivo diga que está | 1) hostId ajeno, 2) firma inválida, 3) typo en el atributo consultado (string-based, silencioso) | `niagara-mental-model-bloque41.md:363-365`; Workbench License Manager |
| 7 | `LicenseLimitExceededException` al crear punto/device | count == límite de la feature (`point.limit`/`device.limit`/…) | `niagara-mental-model-bloque14.md:84-117`; spy `licenseManager.pointCount` |
| 8 | Divergencia entre `licenseManager.pointCount` y el count del nav point manager | Bug de conteo (o punto virtual contado mal) | `niagara-mental-model-bloque28.md:1387` |
| 9 | SMA alarma pero el módulo sigue operando | SMA es enforcement suave (alarma, no bloquea); gatea upgrades/nCloud | `notes/02-licensing.md:188-189`; `niagara-mental-model.md:104-110` |
| 10 | SMA "vigente" localmente pero nCloud la rechaza | Reloj local atrasado vs reloj del servidor nCloud | `niagara-mental-model-bloque32.md:339-345` |
| 11 | Backup/restore aborta a mitad | Restore con más puntos que el límite → abort sin restore parcial | `niagara-mental-model-bloque14.md:100-108` |
| 12 | Station nunca llega a RUNNING; `IOException: Keystore was tampered with` | `cacerts.jceks` corrupto/editado | `niagara-mental-model-bloque27.md:883` |
| 13 | Launcher rechaza el boot tras importar un cert al truststore de install | `keytool -importcert` invalida `NIAGARA4.SF` del install | `niagara-mental-model-bloque18.md:211-215,628` |
| 14 | Tool grisado en Workbench (p. ej. Batch Editor) | Feature de license ausente (p. ej. `provisioning` para `BBatchJobService`) | `niagara-mental-model-bloque14.md:695`; `niagara-mental-model-bloque20.md:588` |
| 15 | `.pxvm` inop aunque la license exista | `axvelocity-rt.jar` ausente del install (license sin módulo = nada) | `niagara-mental-model-bloque36.md:G6` |

---

## 8. Logs y superficie de diagnóstico

- **Workbench `Tools → License Manager`**: import por `licenses/inbox/`, lista features por vendor,
  validación en vivo (firma + hostId + cert) (`niagara-mental-model-bloque40.md:683-691`).
- **CLI**: `nre -licenses` lista features + propiedades (`notes/02-licensing.md:259`);
  `plat.exe` con vista de plataforma incluye resumen de licencia (`niagara-mental-model-bloque14.md:239-241`).
- **JMX**: MBean `com.tridium.sys.license:type=LicenseManager`; spy
  `licenseManager.pointCount` vs Workbench (`niagara-mental-model-bloque28.md:1293,1387`).
- **Alarmas**: `LicenseExpired`, `LicenseExpiresIn`, `PointLimitExceeded`, `DeviceLimitExceeded`,
  `HistoryLimitExceeded`, `ScheduleLimitExceeded` (`niagara-mental-model-bloque14.md:227-233`).
- **Log del daemon** (OS-level, no borrable por la capa Baja): leíble por `/systemlog`/`/getdaemonoutput`;
  `~logging` (`SystemFilePaths.java:32`) (`niagara-mental-model-bloque112.md:83-110`).
- **Mensajes nativos**: `ERROR: Host Id cannot be found/generated.` y marcadores
  `>>> hostid.debug >>>` / `nre_hostid_debug` (`niagara-mental-model-bloque124.md:68,103,173`).

---

## 9. Guías paso a paso

### 9.1 "La licencia no activa" (recién instalada o importada)

1. Verificar el `hostId` del archivo vs `Sys.getHostId()` de la máquina (Workbench / spy).
   Si no matchea → el archivo es de otra máquina: pedir una licencia para este `hostId`
   (`niagara-mental-model-bloque126.md:153`).
2. Verificar `generated` ≤ fecha actual (reloj de la máquina; anti-reloj-atrasado)
   (`notes/02-licensing.md:60-67`).
3. Verificar `expiration` no vencida y que exista `{vendor}.certificate` para el vendor del archivo.
4. Confirmar que el archivo esté en `db/<hostId>/` (fuente canónica) y que la raíz sea alias
   bit-exacto; si no, reimportar por `licenses/inbox/` (`niagara-mental-model-bloque40.md:607-610,683-691`).
5. Revisar el log de arranque/daemon por `LicensingException` o `Keystore was tampered with`
   (`notes/bloque40-D-lib-security.md:319`; `niagara-mental-model-bloque27.md:883`).

### 9.2 "Se perdieron las licencias" tras cambio de host / VM

- El `hostId` derivado cambió con el hardware (MAC/MachineGuid). El alias de la raíz dejó de matchear.
- Regenerar NRE-id post-clonado (`CLONED_FILE` detecta colisión) y pedir re-emisión al vendor para el
  nuevo `hostId` (`niagara-mental-model-bloque41.md:534`; `niagara-mental-model-bloque40.md:654-666`).

### 9.3 "Llego al tope de puntos/devices"

- Leer la feature: `getFeature(vendor,name).get("point.limit")` / `device.limit`; `"none"` = ilimitado
  (`notes/02-licensing.md:215-223`).
- Recordar: virtual points y federación no inflan el count local; verificar si el excedente es real
  (`niagara-mental-model-bloque28.md:1274-1314`).
- Sin gracia para counts: o se reduce el modelo, o el vendor emite límite mayor
  (`niagara-mental-model-bloque14.md:94-96`).

### 9.4 Backup/restore falla

- Restore aborta (sin restore parcial) si el backup supera los límites — reducir modelo o actualizar
  licencia antes de restaurar (`niagara-mental-model-bloque14.md:100-108`).

---

## 10. Despliegue legítimo

- Import vía `licenses/inbox/` → validación → move a `licenses/db/<hostId>/` (reemplazo borra la
  anterior) (`niagara-mental-model-bloque40.md:683-691`).
- Provisioning de flota: `BLicenseService` (verificación + sync de licencias) participa en el
  `BNiagaraNetworkJob` 2-stage (Initial `BUpdateLicensesJobStep` network-wide, luego por station);
  el gate del Batch Editor es la feature `provisioning` de `BBatchJobService`
  (`niagara-mental-model-bloque16.md:461-524`; `niagara-mental-model-bloque20.md:588`).
- Multi-host: el supervisor puede provisionar/sincronizar licencias Qnx del install
  (`niagara-mental-model-bloque40.md:590-610`).
- Recordatorio: cada vendor (Tridium, Honeywell, HoneywellCentraLine, Alerton, SaiaBurgessControls,
  Trend) tiene su propio `.license`/`.certificate`; los módulos OEM consultan features por su vendor
  (`sources/decompiled/easyBinding/rt/EbLicenseUtil.java:115-141`).

---

## Fuentes

- `niagara-mental-model.md` §2 — modelo conceptual, SMA, API
- `notes/02-licensing.md` — esquema XML, pipeline de 5 checks, SMA, límites
- `niagara-mental-model-bloque41.md` §41.6 — clases en `baja.jar`, `Feature`, `LicenseUtil`, EntitlementApi
- `niagara-mental-model-bloque126.md` — nativo: `dsfspi.dll`, `nverify.exe`, `isFeaturePresent`, firma DSA
- `niagara-mental-model-bloque124.md` / `bloque125.md` — HostId nativo (`getHostId0`)
- `niagara-mental-model-bloque40.md` §40.4.8-40.4.9 — layout `db/<hostId>/`, `Webs.license` asimétrica
- `niagara-mental-model-bloque14.md` / `bloque28.md` — límites de runtime, virtual points, federación
- `niagara-mental-model-bloque18.md` / `bloque27.md` / `bloque113.md` — firmas, truststores, hardening
- `niagara-mental-model-bloque112.md` — detección/forense (daemon log, SecurityDashboard)
