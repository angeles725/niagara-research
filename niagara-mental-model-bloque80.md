# Bloque 80 — Stack Galileo: visualización web realtime sobre Niagara vía SignalR 1.4 (deofuscado)

> Investigación empírica de la familia **`galileo*`** deofuscada: un stack OEM (Honeywell/Centraline multi-marca) que sirve puntos, alarmas y schedules a clientes **web/mobile en tiempo real** usando el protocolo **SignalR 1.4 de Microsoft** sobre WebSocket. Es un **análogo arquitectónico de Reflow/MX60** ([Bloques 47-65]) con un transporte distinto.
>
> 4 módulos: `galileoSignalR` (transporte realtime), `galileoPointViewer` (servicio de puntos), `galileoSupervisor` (branding/licencias multi-OEM), `galileoKitPx` (widgets Px + converters).
> Strings descifradas (ZKM); nombres internos `a`/`b`/`c` aún mangled.
>
> Fuentes: `organized/galileo{SignalR,PointViewer,Supervisor,KitPx}/.../vineflower/com/honeywell/{signals,galileo}/...`
> Método: sub-agente + **verificación directa** de declaraciones de clase y protocol version. `[CERT]` = verbatim verificado; `[CERT-a]` = cita del sub-agente no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 79]. Conecta con [Bloque 9] (UI/Servlets/BajaScript) y [Bloques 47-51] (consumir Niagara desde SPA externa).

---

## 80.1 — Galileo en conjunto: dashboard realtime multi-OEM `[CERT]`

Cuatro capas, todas servicios `BComponent implements BIService` (salvo KitPx que es Workbench):

| Módulo | Clase raíz (verificada) | Rol |
|--------|------------------------|-----|
| galileoSignalR | `BSignalService extends BComponent implements BIService, SignalService` (:49) | transporte realtime |
| galileoPointViewer | `BPointListViewService extends BComponent implements BIService` (:100) | servicio de puntos/vistas |
| galileoSupervisor | `BGalileoService extends BComponent implements BIService` (:61) | branding + licencias + temas |
| galileoKitPx | `BGalileoBinding extends BValueBinding` (:25), converters `extends BConverter` | widgets Px + converters |

> Dependencia central `[CERT-a]`: PointViewer **exige** el SignalService; si no existe lo **crea solo** al arrancar (`BPointListViewService` log `"SignalService" not found, adding it...`). SignalR es el bus; el resto produce/consume sobre él.

---

## 80.2 — galileoSignalR: SignalR 1.4 implementado dentro de Niagara `[CERT]`

El hallazgo fuerte: Honeywell **portó el protocolo SignalR 1.4 de Microsoft a un servlet Niagara**. Verificado:
```java
public class SignalRServlet extends WebSocketServlet { ... }   // transport/SignalRServlet.java:59
SIGNALR_PROTOCOL_VERSION = "1.4"                               // SignalRConstants.java
// transports: "webSockets", "serverSentEvents", "longPolling", "foreverFrame"
```

**Endpoints HTTP** `[CERT-a]` (estilo SignalR clásico): `/negotiate` (handshake → ConnectionToken Base64 128B + ConnectionId UUID + KeepAliveTimeout), `/connect`, `/start`, `/send`, `/ping`, `/abort`, `/reconnect`, `/poll`. El `/send` lleva el JSON SignalR `{I:invocationId, H:hub, M:method, A:[args]}` y responde `{I, R:result}` o `{I, E:error}`.

**Tres hubs** (todos `extends BAbstractHub`, y `BAbstractHub extends BComponent implements Hub`, :56) `[CERT]`:
- **`BControlPointHub`** (:72) — lease/suscripción de control points + notificación de cambios (value/override/alarm).
- **`BScheduleHub`** (:74) — distribución de `BWeeklySchedule`.
- **`BAlarmHub`** (:73) — `routeAlarm(BAlarmRecord)`; convierte a `BSentienceAlarmRecordV2` (uuid, timestamp, priority, condition, category, isActive, isAcknowledged, units, description, value, previousValue) `[CERT-a]`.

**Invocación por reflection** `[CERT-a]`: `BAbstractHub.invokeHubMethod()` busca métodos `@HubMethod` con aridad coincidente; JSON→tipos Java automático; cleanup timer 30 s. Errores: HTTP 401 `"Invalid connection token."`, HTTP 501 endpoint no soportado.

> Nota "Sentience": el `BSentienceAlarmRecordV2` conecta con el ecosistema **Sentience** de Honeywell (ver engram `SentienceModelSync`) — Galileo es la capa de presentación de ese stack cloud.

---

## 80.3 — galileoPointViewer: servicio de puntos + session tracking `[CERT]`

`BPointListViewService` (:100) lee/opera puntos y los expone a la UI. Slots `[CERT-a]`: `worker` (`BPointListViewWorker` thread pool), `pointListServlet`, `groupingReference`, `pointUpdateWaitTime` (10-600 s), `viewMode` (`BSelectModeEnum`: LiveMode/EngineeringMode), `galileoTheme`. Operaciones de punto: `set`, `override`, `emergencyOverride`, `active`/`inactive`, relinquish.

**Session tracking dual** `[CERT-a]`: al arrancar instala `FoxServiceSubscriber` (sesiones Workbench, vía `BFoxService.getServerConnections()`) y `BoxServiceSubscriber` (sesiones web/tablet, vía `BBoxService`) — sabe **quién está usando la app** por ambos canales.

**RPC** `[CERT-a]` (`BPointListViewRpc extends BObject`, `@NiagaraRpc` transports box/web): `checkFeatureLicense` (feature **`honPointListView`**), `checkEdgeController` (¿conectado a supervisor?), `getLanguage` (locale+decimal), `getSelectedTheme`. Soporta múltiples vendors/temas: TREND, CENTRALINE, SBC, ALERTON, HONEYWELL_BMS, WEBS, COMFORTPOINT, COMFORTANDENERGY.

> Gate de licencia: `honPointListView` (y `trendIq` para trend). Sin licencia → `"Unlicensed Feature honPointListView"`.

---

## 80.4 — galileoSupervisor: branding y licencias multi-OEM `[CERT]`

`BGalileoService` (:61) detecta la **marca** del sistema desde la licencia (`GalileoLicenseManager.getBrand()`) y reconfigura la UI de la station en consecuencia `[CERT-a]`:

| Brand | Theme module / favicon |
|-------|------------------------|
| ComfortAndEnergy / ComfortPoint | `themeCE` |
| UnifiedSupervisor | `themeUnifiedSupervisor` |
| CentraLine | `themeCentraLine` |
| SBC (Saia) | `themeSBC` + login CSS/logo dedicados |
| Trend | `themeIQVision` + login CSS/logo |
| Alerton | `themeAlerton` |
| Webs | `themeHoneywell` |

Además `[CERT-a]`: habilita el MixIn `BGalileoUserPin` (PIN de usuario), vincula `BTimeTrigger`→`dateTime`, configura `BWebService` (favicon/loginCss/logo), limpia el legacy `PointUtilityService`, y para SBC agrega el `SaiaPG5DataImportWizard`. Es el **integrador de marca** del stack: el mismo binario se re-marca por licencia.

---

## 80.5 — galileoKitPx: widgets Px + converters status→simple `[CERT]`

La capa de presentación en Workbench (Px/Picture). `BGalileoBinding extends BValueBinding` (:25, slots `visibilityPin`/`actionPin`); converters `extends BConverter` mapean tipos Niagara a `baja:Simple` para iconografía.

**Lógica de status→visual verificada** (`BGalileoBooleanToSimple`, :50) `[CERT-a]` — decodifica los status bits a estados visuales:
```
bit 32  (0x20)      → override   → overrideOn/overrideOff
bits 136 (128|8)    → alarm      → alarmOn/alarmOff
bit 1               → disabled
bit 4               → down
bit 2               → fault
else                → on/off (valor normal)
```
Constante `STATUS_ALARM = 136`. Widgets: `BEasyPicture`/`BEasyLabel` (+ variantes `Hx`), `EasyImageManager`. Converters análogos para status y texto. Bindings: value/action/popup/boundLabel.

> Esto **mapea el modelo de status de Niagara** ([Bloque 4] status bits) a iconos de dashboard — la misma necesidad que Reflow resuelve en JS, aquí resuelta como converters Px nativos.

---

## 80.6 — Síntesis + paralelo con Reflow/MX60

**Arquitectura**: `Supervisor` (marca/licencia) + `SignalR` (bus realtime WebSocket) + `PointViewer` (productor de datos de punto + session tracking) + `KitPx` (presentación Px). El cliente (browser/mobile) habla SignalR 1.4 contra el servlet; los hubs hacen lease/push de puntos/alarmas/schedules.

**Paralelo con Reflow ([Bloques 47-65])** — dos formas de hacer lo mismo:

| | **Galileo** | **Reflow/MX60** |
|---|---|---|
| Transporte realtime | **SignalR 1.4** (WebSocket/SSE/LongPoll/ForeverFrame) | **BOX** subscriptions + oBIX REST |
| Push de cambios | hubs (`@HubMethod` reflection) | lease BOX 10 s + reconnect manual ([Bloque 47]) |
| Alarmas | `BAlarmHub.routeAlarm` → JSON | alarm channel BOX |
| Presentación | converters Px nativos (Workbench) | SPA Vue/Vite externa |
| Multi-marca | sí, por licencia (8 brands) | no (single-tenant) |

**Para MX60**: Galileo demuestra un **patrón de hub realtime server-side** (SignalR) como alternativa al lease BOX. Si MX60 necesita push robusto, el modelo de hubs con reflection + transports con fallback es una referencia probada en producción Honeywell. Pero SignalR 1.4 es **legacy** (Microsoft lo reemplazó por SignalR Core/WebSocket nativo) — no adoptar el protocolo viejo, sí el patrón.

**Pendiente conocido**: nombres de clases internas ofuscados (`a`/`b`/`c`) — irreversible (ZKM).
