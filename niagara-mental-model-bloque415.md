# Bloque 415 — Niagara Network Supervisor (II): niagaraDriver — modelo device/proxy del join

> **Qué documenta**: el driver `niagaraDriver` (106 clases, 11 paquetes en `niagaraDriver-rt`),
> con foco en las cuatro clases arquitecturales del gap N2: `BNiagaraNetwork` (contenedor de red
> como servicio Baja), `BNiagaraStation` (la subordinada representada como device-proxy en el árbol
> del supervisor), `BNiagaraProxyExt` (proxy de un punto individual con ciclo de suscripción
> Fox), y `BPointChannel` (el canal Fox `"point"` que ejecuta la suscripción en batch).
> Complementa N2 con los imports de history/file/schedule que [Bloque 266] nombró sin abrir.
>
> **Alcance**: `niagaraDriver-rt` únicamente. NO re-documenta el join ni `BSupervisorJoinJob`
> (→ B266), ni la resolución wb-vs-rt de tipos PX (→ B414).
>
> **Subject version**: Niagara N4 4.14.0.162 · build 2024-05-28
>
> **Fuentes** (decompilado Vineflower; alias de ruta):
> - `$ND` = `.../niagaraDriver/niagaraDriver-rt/vineflower/com/tridium/nd/`
> - Raíz: `/home/cristian/modules/Prototipos/modulos/organized/`
>
> **Método**: lectura inline directa de los 4 archivos principales + grep de líneas clave;
> enumeración de paquetes por Bash (`find … | wc -l`). 7 tokens `[CERT]` de peso verificados
> por número de línea antes de escribir.
> Marcadores: `[CERT]` = fuente primaria (file:line); `[INFER]` = deducción.

---

## 415.1 — Medición del gap N2: 106 clases en 11 paquetes `[CERT]`

El corpus `niagaraDriver-rt/vineflower/` contiene **106 archivos `.java`** bajo `com/tridium/nd/`,
distribuidos en un paquete raíz y 10 subpaquetes `[CERT]` (conteo en disco: `find $ND -name "*.java" | wc -l → 106`):

| Paquete | Clases (aprox.) | Tipos representativos |
|---|---|---|
| `nd/` (raíz) | 10 | `BNiagaraNetwork`, `BNiagaraStation`, `BNiagaraStationFolder`, `BStationWorker`, `BCyclicThreadPoolWorker` |
| `nd/point/` | 12 | `BNiagaraProxyExt`, `BPointChannel`, `BNiagaraPointDeviceExt`, `BNiagaraTuningPolicy`, `ClientWorker`, `ServerWorker` |
| `nd/history/` | 6 | `BNiagaraHistoryDeviceExt`, `BNiagaraHistoryImport`, `BNiagaraHistoryExport`, `BArchiveChannel` |
| `nd/file/` | 7 | `BNiagaraFileDeviceExt`, `BNiagaraFileImport`, `BNiagaraFileDescriptor` |
| `nd/schedule/` | 6 | `BNiagaraScheduleDeviceExt`, `BNiagaraScheduleImportExt`, `BScheduleChannel` |
| `nd/alarm/` | 2+ | `BAlarmChannel`, `BNiagaraAlarmDeviceExt` |
| `nd/discover/` | varios | utilidades de discovery y learn |
| `nd/sysdef/` | varios | `BBogProvider`, `BSysDefChannel`, `BLocalSysDefStation` |
| `nd/user/` | varios | `BUserSyncChannel`, `BNiagaraUserDeviceExt` |
| `nd/virtual/` | varios | `BNiagaraVirtualChannel`, `BNiagaraVirtualDeviceExt` |
| `nd/spy/`, `nd/util/` | varios | instrumentación y utilidades |

`[INFER]` La estimación del enunciado era 110; el conteo real sobre Vineflower es **106**. La diferencia
de 4 es consistente con clases internas o anónimas que el estimado previo pudo computar como separadas.

Este bloque no abre todos los 106 archivos — solo las 4 clases del modelo de device/proxy (§415.2-415.5)
y las extensiones de dominio de los imports (§415.6), siguiendo la regla e2 del focus.

---

## 415.2 — BNiagaraNetwork: contenedor de red y servicio Baja `[CERT]`

`BNiagaraNetwork` extiende `BDeviceNetwork` (el framework Baja para redes de dispositivos) e implementa
`BIService`, `NiagaraNetwork`, `BINiagaraNetwork`, `FoxServerConnectionListener` y
`BINiagaraPointContainer` `[CERT]` `$ND/BNiagaraNetwork.java:117-121`:

```java
public final class BNiagaraNetwork
    extends BDeviceNetwork
    implements BIService, NiagaraNetwork, BINiagaraNetwork,
               FoxServerConnectionListener, BINiagaraPointContainer {
```

**Device type declarado**: `getDeviceType()` retorna `BNiagaraStation.TYPE` `[CERT]`
`$ND/BNiagaraNetwork.java:217`. El framework infiere de esta declaración que cada "device" dentro de
esta red es un `BNiagaraStation`.

**Licencia**: `getLicenseFeature()` retorna feature `"niagaraDriver"` del vendor `"tridium"` `[CERT]`
`$ND/BNiagaraNetwork.java:225-226`. El uso del driver queda acotado por licencia del módulo.

**Mapa de stations activas**: mantiene `volatile Map<String, NiagaraStation> stations` `[CERT]`
`$ND/BNiagaraNetwork.java:138`, populado lazy por `getStation(name)` `[CERT]`
`$ND/BNiagaraNetwork.java:229-235`. El mapa es el registro en memoria de todas las
`BNiagaraStation` hijas activas, indexadas por nombre de station.

**Al arrancar** (`serviceStarted()`): actualiza la `localStation` con nombre e IP reales de la station
anfitrión, y se registra como `FoxServerConnectionListener` en `BFoxService` para recibir conexiones
Fox entrantes `[CERT]` `$ND/BNiagaraNetwork.java:269-275`.

---

## 415.3 — BNiagaraStation: la subordinada representada como device-proxy `[CERT]`

`BNiagaraStation` extiende `BDevice` e implementa `NiagaraStation`, `BINiagaraStation`,
`BIPollableHistorySource` y `BINiagaraPointContainer` `[CERT]` `$ND/BNiagaraStation.java:154`.
Es la clase que, en el árbol del supervisor, **representa** una estación remota (la subordinada).

**Constructor y self-registration**: en la construcción, la station se añade a sí misma como
`connectionTarget` de su propia `BFoxClientConnection` `[CERT]` `$ND/BNiagaraStation.java:328-329`:

```java
public BNiagaraStation() {
    this.getClientConnection().addConnectionTarget(this);
}
```

`[INFER]` Este registro es el que permite que el canal Fox (punto, historia, etc.) recupere la
`BNiagaraStation` a través de `getConnection().getConnectionTarget(BNiagaraStation.class)`.

**Identidad en el árbol**: `getStationName()` retorna `this.getName()` `[CERT]`
`$ND/BNiagaraStation.java:391`. El nombre del componente en el árbol del supervisor ES el nombre de
la station remota — no hay un campo separado.

**Transporte Fox**: `getScheme()` retorna `"foxs"` o `"fox"` según `clientConnection.getUseFoxs()`
`[CERT]` `$ND/BNiagaraStation.java:403-405`:

```java
public String getScheme() {
    return this.getClientConnection().getUseFoxs() ? "foxs" : "fox";
}
```

**Handshake hello**: `initHello()` envía al remoto: `foxServerPort`, `foxServerUseFoxs`, `version`,
`hostModel`, `hostModelVersion`, `sysDefVersion` `[CERT]` `$ND/BNiagaraStation.java:407-424`.
`clientOpened()` lee el hello del remoto y almacena `version`, `hostModel`, `hostModelVersion`
como properties visibles `[CERT]` `$ND/BNiagaraStation.java:425-440`. Estas propiedades son las
que Workbench muestra en la tabla de stations.

**DeviceExt por dominio**: cada `BNiagaraStation` lleva una DeviceExt por subsistema, todas con
`clientConnection` delegada a la station padre `[CERT]` `$ND/BNiagaraStation.java:154` (anotaciones
`@NiagaraProperties`):

| DeviceExt | Property en station | Rol |
|---|---|---|
| `BNiagaraPointDeviceExt` | `points` | suscripción de puntos proxy |
| `BNiagaraHistoryDeviceExt` | `histories` | import/export de historiales |
| `BNiagaraAlarmDeviceExt` | `alarms` | sincronización de alarmas |
| `BNiagaraScheduleDeviceExt` | `schedules` | import/export de schedules |
| `BNiagaraUserDeviceExt` | `users` | sincronización de usuarios |
| `BNiagaraSysDefDeviceExt` | `sysDef` | definición de sistema (BOG) |
| `BNiagaraVirtualDeviceExt` | `virtual` | puntos virtuales |
| `BNiagaraFileDeviceExt` | `files` | acceso a archivos remotos |

**Bootstrap / TLS transient exemption**: la property dinámica `"bootstrap"` controla si la conexión
Fox usa un `TransientExemptionApprover` para aceptar certificados aún no confiados. Al deshabilitarse,
elimina las exenciones transitorias del `CoreCryptoManager` `[CERT]`
`$ND/BNiagaraStation.java:549-590`.

`[INFER]` Conexión con el join (B266/B414): el parámetro `station` que recibe `doJoin()` en
`BSupervisorJoinJob` es precisamente una `BNiagaraStation` del árbol del supervisor. Todo slot virtual
que `doJoin()` crea — incluyendo el `BSubstitutePxView` documentado en B414 §414.3 — vive dentro de
esta `BNiagaraStation`. La station es el nodo de anclaje del join en el árbol supervisor.

---

## 415.4 — BNiagaraProxyExt: el proxy de un punto individual `[CERT]`

`BNiagaraProxyExt` extiende `BProxyExt` (el framework Baja de puntos proxy de driver) e implementa
`BISubLicenseable`, `INiagaraProxyExt` e `IProxyActionParent` `[CERT]`
`$ND/point/BNiagaraProxyExt.java:107`.

**Properties clave**:
- `pointId` (String) — el ORD del punto en la station remota, key de la suscripción `[CERT]` `:109`
- `subscriptionStatus` (flags=READONLY+SUMMARY) — estado visible en UI

**messageId**: asignado en construcción vía `NiagaraProxyExtSupport.newMessageId()` `[CERT]` `:121`.
Es el identificador de multiplexación de la suscripción dentro del canal Fox `"point"` — permite
enrutar respuestas push del remoto al proxy correcto sin desencriptar el ORD.

**Modo read-only**: `getMode()` retorna `BReadWriteMode.readonly` `[CERT]` `:163`. Los proxy points
de Niagara son read-only por defecto; la escritura requiere el comando `"write"` explícito.

**Ciclo de vida de suscripción**:

| Fase | Método | Delegación |
|---|---|---|
| `started()` | registra en `station.getPoints().registerProxyExt(this)` | `[CERT]` `:276-278` |
| Cambio de `pointId` | dispara `forceUnsubscribe()` automático | `[CERT]` `:269-273` |
| Envío sub | `sendingSubscribe()` → `NiagaraProxyExtSupport.sendingSubscribe(this)` | `[CERT]` `:249-250` |
| Confirmación sub | `sentSubscribe()` → `NiagaraProxyExtSupport.sentSubscribe(this)` | `[CERT]` `:251-252` |
| `stopped()` | `station.getPoints().unregisterProxyExt(this)` | `[CERT]` `:308` |

`[INFER]` La máquina de estados de suscripción (`BSubscriptionState`: `unsubscribed → pending →
sendingSubscribe → subscribed → …`) está encapsulada en `NiagaraProxyExtSupport` (paquete `nv`), no
en este archivo — separación entre el protocolo Fox (en `nd/`) y la máquina de estado (en `nv/`).

---

## 415.5 — BPointChannel: canal Fox "point" y protocolo de suscripción `[CERT]`

`BPointChannel` extiende `BFoxChannel`, registrado con nombre de canal `"point"` `[CERT]`
`$ND/point/BPointChannel.java:88,102`:

```java
public class BPointChannel extends BFoxChannel {
    public BPointChannel() { super("point"); }
```

**Cifrado**: `useSharedKeyEncryption()` retorna `true` `[CERT]` `:121`. El canal de puntos usa
la clave compartida Fox (no la clave pública del certificado).

**Comandos request/response** sobre el canal:

| Comando | Rol |
|---|---|
| `"sub"` | suscribir N puntos (batch) |
| `"unsub"` | desuscribir |
| `"change"` | push de cambio de valor desde remoto |
| `"getActionDefault"` | valor por defecto de acción |
| `"invoke"` | invocar acción remota |
| `"write"` | escritura de punto |
| `"fetchRemoteTags"` | fetch de tags remotos |

`[CERT]` `$ND/point/BPointChannel.java:130-147`

**Circuitos Fox** (streams bidireccionales): `"discover"`, `"discoverSlots"`, `"discoverFiles"`,
`"discoverPartialSlots"` — usados durante el Learn de la station `[CERT]` `:148-158`.

**Protocolo de suscripción en batch** — `subscribe(ArrayList<INiagaraProxyExt>)` `[CERT]` `:166`:

Envía un único `FoxRequest("sub")` con un `FoxMessage("pt")` por cada punto `[CERT]` `:179-185`:

```java
FoxMessage msg = new FoxMessage("pt");
msg.add("mid", ext.getMessageId());              // ID de multiplexación
msg.add("pid", ext.getPointId());                // ORD del punto en el remoto
msg.add("t",   ext.getPointType());              // tipo: b/n/e/s
msg.add("minSend", policy.getMinUpdateTime());   // cadencia mínima (ms)
msg.add("maxSend", policy.getMaxUpdateTime());   // cadencia máxima (ms)
```

`[INFER]` El diseño batch permite suscribir cientos de puntos en un único roundtrip Fox. El parámetro
`maxSend` actúa como heartbeat máximo de actualización; `minSend` como throttle de deduplicación.
La cadencia proviene del `BNiagaraTuningPolicy` asignado al punto (o la policy por defecto de la red).

---

## 415.6 — Extensiones de dominio: los imports de history, file y schedule `[CERT]`

Los tres dominios que [Bloque 266] nombró sin abrir comparten la misma `BFoxClientConnection` de la
`BNiagaraStation` padre; la separación es por canal Fox nombrado, no por conexión TCP distinta.

**BNiagaraHistoryDeviceExt** (`history/`): extiende `BHistoryDeviceExt` `[CERT]`
`$ND/history/BNiagaraHistoryDeviceExt.java:37`. Expone dos canales Fox sobre la conexión de la station:
`"archive"` (`BArchiveChannel`) y `"history"` (`BHistoryChannel`) `[CERT]` `:71-76`:

```java
public final BArchiveChannel getClientArchiveChannel() {
    return (BArchiveChannel)((BNiagaraStation)this.getParent())
        .getClientConnection().getChannels().get("archive", BArchiveChannel.TYPE);
}
public final BHistoryChannel getClientHistoryChannel() {
    return (BHistoryChannel)((BNiagaraStation)this.getParent())
        .getClientConnection().getChannels().get("history", BHistoryChannel.TYPE);
}
```

Los descriptores de import/export son `BNiagaraHistoryImport` (extiende `BHistoryImport`, implementa
`Interest`) `[CERT]` `$ND/history/BNiagaraHistoryImport.java:35` y `BNiagaraHistoryExport`.

**BNiagaraFileDeviceExt** (`file/`): extiende `BDescriptorDeviceExt` `[CERT]`
`$ND/file/BNiagaraFileDeviceExt.java:15`. No abre un canal Fox propio; delega
`getClientConnection()` directamente a la station padre `[CERT]` `:55-56`:

```java
public BFoxClientConnection getClientConnection() {
    return this.getNiagaraStation().getClientConnection();
}
```

El acceso a archivos remotos usa el canal Fox de archivos estándar del framework (`BFileChannel`).

**BNiagaraScheduleDeviceExt** (`schedule/`): extiende `BScheduleDeviceExt`, implementa `Interest`.
`BNiagaraScheduleImportExt` extiende `BScheduleImportExt` e implementa `Interest`; su `doExecute()`
envía un `FoxRequest("schedule", "import")` con el supervisor ID y la versión de la subordinada
`[CERT]` `$ND/schedule/BNiagaraScheduleImportExt.java:40-41`:

```java
FoxRequest req = new FoxRequest("schedule", "import");
BNiagaraScheduleDeviceExt.setSupervisorId(req, this.getSupervisorId());
```

El canal de schedules es `BScheduleChannel` (nombre `"schedule"`).

**Resumen de canales Fox por dominio**:

| Canal Fox | Clase | Uso |
|---|---|---|
| `"point"` | `BPointChannel` | suscripción de puntos proxy (§415.5) |
| `"archive"` | `BArchiveChannel` | import de historiales en formato archivo |
| `"history"` | `BHistoryChannel` | import de historiales en streaming |
| `"schedule"` | `BScheduleChannel` | import/export de schedules |
| `"file"` | framework `BFileChannel` | acceso a archivos remotos |

---

## 415.7 — Conexiones

- **[Bloque 266]** §266.5 — nombró `BNiagaraStation` y los imports de history/file/schedule como
  piezas del join sin abrirlos. B415 los abre: la `BFoxClientConnection` de la station es el canal
  que transporta también las credenciales del join (el hello Fox del handshake).
- **[Bloque 414]** §414.3 — confirmó que `doJoin()` persiste `BSubstitutePxView` dentro del
  parámetro `station` (la `BNiagaraStation`). B415 documenta qué es esa station: un `BDevice` proxy
  que mantiene la `BFoxClientConnection` a la subordinada real y cuyos DeviceExt son los distintos
  canales Fox de importación.
- **[Bloque 267]** §267.4 — `BJoinProfileManager` con password como property normal; la misma
  `BFoxClientConnection` de `BNiagaraStation` transporta esas credenciales en el hello del handshake.
- **[Bloque 258]** §258.1 — criterio de perfil rt/wb; `BNiagaraStation` y su `BPointChannel` están
  en `niagaraDriver-rt`, presente tanto en supervisores como en JACE.
