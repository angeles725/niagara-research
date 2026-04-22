# Niagara N4 — Mental Model · Bloque 13: Gaps profundos

**Sesión**: 2026-04-22
**Fuentes**: devguide (virtualComponents, niagaraRpc, report, search, uxMedia, naming), source `javax.baja.fox.*`, `crypto.*`, `virtual.*`, `license.*`, `niagaraNetwork-*`.

Cubierto los 8 bloques principales (1-3 infraestructura + 4-12 features), este bloque ataca los temas ortogonales que quedaron pendientes:
- Subscription licensing + Niagara Network (topología multi-station).
- Fox wire protocol (detalle de frames) + sensitive data encryption + virtual components.
- NiagaraRPC deep + Reports + Search + UxMedia + nav/root schemes.

---

## 13.1 Subscription licensing + Niagara Network

### 13.1.1 Subscription licensing runtime

**Diferencia con license file tradicional (Bloque 2.2)**: `.license` estático validado en boot. Subscription = **periodic check-in contra nCloud** (Niagara Cloud).

**`SubscriptionLicenseManager extends NLicenseManager`**:
- Check-in automático cada 24-48h (configurable) contra `/ncloud/licensing/checkin`.
- Cache local en `~/.Niagara*/subscription/entitlements.json`.
- Grace period 30 días si cloud cae.
- Cada check firmado digitalmente por station keypair (Bloque 3) para auth mutua.

**Validación remota**:
1. Boot: carga último entitlement válido desde disco.
2. Check-in: POST `{hostId, stationUuid, featureList, lastCheckin}` a nCloud.
3. Respuesta: `{features, expirationDate, nextCheckIn, gracePeriodDays}`.
4. Fallo network → grace period. Pasado → feature deshabilita.
5. Ack: station firma response, nCloud confirma autenticidad (certificate pinning).

**Grace semantics**:
- Sin conexión nCloud → features disponibles por grace.
- Expiración dentro de grace → warning alarm.
- Post-grace → feature service deniega nuevas ops.

### 13.1.2 nCloud endpoints

- `licensing.ncloud.honeywell.com`: valida subscriptions, entitlements.
- `devices.ncloud.honeywell.com`: device registry, auto-enrollment.
- `telemetry.ncloud.niagaracloud.com`: opcional UPH reports.

**Check-in request HTTPS**:
```json
POST /api/v1/stations/{stationId}/licensing/checkin
{
  "hostId": "Win-6E6E-10AC-D1DD-8276",
  "stationUuid": "550e8400-...",
  "requestedFeatures": ["station", "nCloudDriver", "honEdgeDriver"],
  "lastSuccessfulCheckIn": "2026-04-20T10:00:00Z",
  "buildVersion": "4.14.0.162",
  "signature": "base64(ECDSA(payload))"
}
```

**Response**: `{status, entitlements[{featureName, vendor, expiresAt, properties}], nextCheckInWindow, serverSignature}`.

Station: key en `~/.Niagara*/security/station-key.pem`, cert en `station-cert.pem`. nCloud root cert pre-bundled en `nCloudDriver-rt.jar`.

### 13.1.3 BNiagaraNetwork arquitectura

```
station:
  niagara | [BNiagaraNetwork]
    supervisors | [BFolder]
      MyMain | [BSupervisor]
        remoteStations | [BFolder]
          jace1 | [BStation] (ref a estación remota)
    subordinates | [BFolder]
      MySubord | [BSubordinate]
        foxLinks | [Folder]
```

**Roles**:
- **BSupervisor**: station en server/workstation. Centraliza history/alarm/schedule aggregation.
- **BSubordinate / BStation remote**: delegado en embedded (JACE). Reporta al Supervisor via Fox.
- **Fox links**: canales TCP/TLS multiplexados (1911 FOX / 4911 FoxS).

### 13.1.4 BSupervisor componente

Props clave:
- `name`, `description`.
- `address` (BOrd): `station:|MyRemoteStation` o `host:192.168.1.10:1911`.
- `userName`, `password`: credenciales FOX.
- `connectionTimeout` (default 30s).
- `pollInterval` (default 1 min): refresco estado.
- `enabled`.
- Readonly: `status` (Connected/Disconnected/Error), `lastConnectTime`, `lastErrorTime`.

Servicios delegados:
- History collection (Bloque 8.2.9): referencias via BFoxHistorySpace.
- Alarm federation (Bloque 8.1.6 BStationRecipient): subordinado → supervisor para console + archive.
- Schedule sync (Bloque 8.3.8 BScheduleImportExt): master → subordinados.

### 13.1.5 BSubordinate discovery + health

**Discovery** (3 mecanismos):
1. Manual: admin añade BSupervisor.
2. Network discovery (UDP broadcast, mDNS) si `NiagaraNetwork > DiscoveryService` habilitado.
3. nCloud device registry (con nCloudDriver): stations registradas en mismo "site".

**Health**:
- Polling via `pollInterval`.
- Heartbeat topic via FOX subscription a remote `EngineMonitor.status`.
- Fallback: station cae → alarmas "Station Disconnected" → recipients.

### 13.1.6 Federation (history/alarm/schedule)

**History** (Bloque 8.2.9):
- Subordinado: `BIntervalHistoryExt`/`BCovHistoryExt` escriben local.
- `BHistoryExport` pushes a supervisor (configurado en NiagaraNetwork > Histories del subordinado).
- Supervisor: `BHistoryImport` pull periódico + retry.
- Políticas: `BHistoryNetworkExt.configRules` transforman capacity/fullPolicy/storageType al importar.

**Alarma federada**:
- Subordinado: BAlarmSourceExt → BAlarmClass local → BStationRecipient.
- Supervisor: console + archive si BAlarmArchive.
- Reverse sync: ack supervisor → FOX → subordinado limpia UNACKED_ALARM.

**Schedule replicada**:
- Master en Supervisor: BControlSchedule.
- BScheduleExport.doExecute() marshals tree via FOX, inlinea props flag USER_DEFINED_1.
- Subordinado: BScheduleImportExt con subscribeWindow randomizado (30-60s default) previene thundering herd.
- Sync periódica: onOutputChange event.

### 13.1.7 Scale considerations

**Típico Supervisor: 10-50 subordinados**:
- 1 FOX channel per subordinado, ~10-50KB/s promedio.
- History: 5-20 histories, 500KB-10MB/semana disk.
- Alarm: 10-100 alarms/min total, ~1MB open DB.
- Memory: 500MB-2GB (default -Xmx1024m).

**Bottlenecks**:
1. Single FOX broker saturado → subscription lag. Mitigación: múltiples canales con pooling.
2. History import concurrent → HistorySpaceConnection locks. Mitigación: stagger schedule.
3. Alarm archive con RDBMS lenta → cleared se acumulan en open. Config `retention.days`.
4. Network jitter > FOX timeout 30s → reconnect loop. Aumentar connectionTimeout.

**Optimizaciones**:
- Supervisor history `BCapacity.unlimited()` (disk grande); subordinado `makeByRecordCount(100000)` (menos memory).
- Alarm ackRequired=false para ruido (reduce roundtrip).
- Schedule sync `subscribeWindow.min=120s` para stagger.
- Monitor `/spy/sysManagers/leaseManager` para leases stale.

---

## 13.2 Fox wire + sensitive data + virtual components

### 13.2.1 Fox wire protocol — frame structure

```
TCP Socket (1911 plain / 4911 TLS)
  ↓
Fox Frame Header (16 bytes + payload)
  ├─ frame type / op code (1 byte)
  ├─ channel ID (2 bytes)
  ├─ sequence number (4 bytes)
  ├─ length (4 bytes)
  ├─ flags (1 byte)
  └─ reserved (4 bytes)
  ↓
Encrypted payload (AES-256 si TLS wrap)
  ├─ Message type (op code: ReadOp, WriteOp, LinkChange, etc.)
  ├─ Serialized data (ORD, BOG, values)
  └─ CRC32/MAC integridad
```

**Op codes principales**:
- `0x01` HELLO (handshake versión).
- `0x02` AUTHENTICATE (Digest/cert/OAuth/Kerberos).
- `0x10` READ_SLOT (BProxyFoxSession.get() → resolve ORD remoto).
- `0x11` WRITE_SLOT.
- `0x20` SUBSCRIBE.
- `0x21` UNSUBSCRIBE.
- `0x30` LINK_CHANGE.
- `0xFF` CLOSE_CHANNEL / PING / PONG.

**Multiplexado**: 1 TCP socket → N canales (BFoxChannel) con channel ID 16-bit. Operaciones concurrentes sin más sockets.

### 13.2.2 Session lifecycle

1. **TCP Connect** → 1911/4911.
2. **HELLO frame** (client): versión protocolo, opciones (compression/encryption/auth), client ID.
3. **HELLO response** (server): versión aceptada, cipher suite TLS, ticket sesión temp.
4. **AUTHENTICATE**: Digest / cert chain / OAuth token / Kerberos SPNEGO / Google Auth. Server valida contra BUserService o cert keystore. Success → session token.
5. **Crear canales** (lazy): per-operación channel ID asignado. Channel bound a session token. Timeout inactividad default 5 min → auto-close.
6. **Operaciones**: client FRAME con channel ID + sequence#, server procesa + responde mismo channel. Sequence# permite reassemble out-of-order.
7. **CLOSE**: CLOSE_CHANNEL frame → flush pending + ACK + libera recursos. Socket idle → server close auto en 30s.

**Session token persiste aunque cierren canales** — reconexión sin re-auth.

### 13.2.3 BSensitiveBox + master password

**BPassword**:
- **Non-reversible**: hash bcrypt/PBKDF2. Típico BUser.password. No recupera plaintext.
- **Reversible**: AES-256 cifrado. Requerido cuando debe enviarse a otro sistema (ej. SMTP driver).

**Master password / keyring**:
- Almacén: `${niagara.user.home}/.keyring/master.jceks` (Java KeyStore).
- Generación: primer boot station, prompt admin (min 8 chars).
- Uso: BPassword reversibles cifran con AES-256 derivada PBKDF2 de master.
- Cambio: requiere reencriptar todas props → O(n).

**BSensitiveBox**: wrapper conceptual. Detecta BPassword → encriptación automática con keyring.

### 13.2.4 BOG encryption modes

`reversibleEncodingKeySource` en root bajaObjectGraph:

1. **`none`**: máxima portabilidad. NO cifra reversibles (se ignoran). Usado en `.dist` sin master knowledge.
2. **`keyring`** (default station): clave derivada de master.jceks. BOG host-específico, NO portable sin master. SALT + iteration count en BOG para PBKDF2. Más seguro.
3. **`external`**: clave derivada de passphrase externa (env var `NIAGARA_BOG_PASSPHRASE`). Portable con passphrase. Dev/CI.

**Serialización** (BogPasswordObjectEncoder):
```
plaintext BPassword
  → apply salt (random 16 bytes)
  → PBKDF2(master, salt, 10000 iterations) → AES-256 key
  → AES-256-CBC encrypt
  → base64 encode
  → XML attr v="AES256$base64..."
```

**Fallback deserialize**: keyring → external → null si ninguno accesible.

### 13.2.5 Virtual components

Componentes sin implementación física local. ORD scheme `virtual:`.

**Usos**:
- **Gateways sintéticos**: `BVirtualGateway` proxea devices remotos como locales.
- **Synthetic points**: `BVirtualNumericPoint` con valor computado.
- **Namespace aliasing**: mismo device bajo dos nombres via mounting virtual.

| Aspecto | Regular | Virtual |
|---------|---------|---------|
| Instancia física | Sí | No (proxy/computed) |
| Persistencia | config.bog | BOG opcional (remoto/generado) |
| Lifecycle | start/stop | start=init proxy, stop=close links |
| Acceso remoto | FOX local | FOX remoto o on-demand |
| Autoridad | Propietario | Reference/delegado |

**BVirtualGateway típico**:
```java
BVirtualGateway extends BAbstractGateway {
  @NiagaraProperty BString remoteStationOrd;  // "ip:192.168.1.10|fox:|station:|slot:/..."
  @NiagaraProperty BString remoteDevicePath;

  started() {
    // lazy: resolve remoteStationOrd → BProxyFoxSession
    // crea canales FOX lectura/escritura
    // expone localmente como device local
  }
}
```

**Schema resolve**:
```
virtual:/gw/device1/points/temp
  → BVirtualScheme.resolve()
    → lookup BVirtualGateway gw
    → gw.resolve("device1/points/temp")
    → delegado a remote station via FOX
```

**Lifecycle**:
1. Mount (addChild): registra en espacio.
2. Start: abre FOX session remota, subscribes a cambios.
3. Query (get por ORD): proxea al remoto via FOX.
4. Unmount: cierra FOX session, cleanup.
5. Stop station: auto-cleanup todas sesiones virtuales.

---

## 13.3 NiagaraRPC deep + misc (reports, search, UxMedia, nav)

### 13.3.1 NiagaraRPC deep

`@NiagaraRpc` (desde N4.1) marca métodos (static o instance) remotamente invocables via FOX, BOX (BajaScript), o Web Servlets REST.

**Props anotación**:
- `permissions`: `"RWI"`, `"unrestricted"`, o específicos. User debe autenticar.
- `transports`: array `@Transport(type=web|box|fox)`. Limita canales.
- `isSecure`: si true, solo canal encriptado. Context incluye facet `isSecure`.
- `protectedTargets`: ORDs adicionales (ej. `service:baja:UserService` con permisos "I").

**Encoding JSON**:
- JSON Object → Map<String, Object>.
- JSON Array → List<Object>.
- Number → double.
- Boolean, String → tipos nativos.
- **Context**: siempre último argumento. User info, language, facets (isSecure, remoteAddr, transportType).
- **Null NO soportado** como argumento.

**Invocación via NiagaraRpcServlet**:
- Single RPC: `POST /rpc/methodName/ord`, body JSON array args. Response `{value: returnValue}`.
- Multi RPC: `POST /rpc`, body JSON array `[{ord, methodName, args}, ...]`.
- CSRF header `x-niagara-csrfToken` requerido.

Return types: Map, List, JSONObject, JSONArray, primitivos.

**Error handling**: excepciones capturadas por servlet → JSON error. No RPC discovery nativo (gap — clients hardcodean method names).

**N4.6+**: facet `transportType` indica canal usado (tag names de TransportType enum).

### 13.3.2 Reports framework

**Componentes**:
- **BReport**: struct con `reportName`, `fileName`, `mimeType`, `content` (byte[]). Formato abierto (típicamente PDF binario).
- **BReportSource** (extends BComponent): override `handleGenerate()`. Schedule (BTimeTrigger) para ejecución auto. Action `generate` manual o por schedule.
- **BReportRecipient**: router (archivo, email).
- **BReportService** (BIService): contenedor central.

**Flujo**: schedule → generate action fires → handleGenerate() → create BReport → fire out topic → Recipient routes.

**Sin soporte nativo para templates** (BReportTemplate/BReportSection no en docs básicos). Integración BQL/NEQL posible pero delegada a implementación custom.

### 13.3.3 Search framework

**BSearchService** (N4.0+): capa simple sobre query APIs.

Props:
- `defaultScheme`: `"neql"` default.
- `defaultScopes`: vector BSearchScope|BOrd. Ej: `"station:"`, `"sys:"`.
- `maxConcurrentSearches`: 50 default.
- `maxResultsPerSearch`: 500 default.
- `searchTaskTimeToLive`: 2 min default. Reset si subscribed/accedido.
- `activeSearchContainer`: hidden, evita que property sheet suscriba tasks.

Actions:
- `search(BSearchParams) → BOrd` (async, a BSearchTask).
- `retrieveResults(BResultsRequest) → BSearchResultSet` con startIndex + maxResults paginación.
- `getSearchScopes() → BVector` de scopes con QueryHandlers/SearchProviders.

**BISearchProvider**: alternativa a QuerySchemes. Registran como agents sobre BOrdSchemes + scopes. `search(BOrd queryOrd, BIObject scope, Context) → Stream<Entity>`. Ej. BBqlSearchProvider.

Loop típico: `searchService.retrieveResults()` hasta `resultsComplete()`.

### 13.3.4 UxMedia (N4.10+)

**Concepto**: Px media nativa HTML5/JS puro en browser. Diverge de Hx (que construye bajaui en station y convierte a JS strings). UxMedia 100% bajaux + BajaScript.

**Motivaciones**:
- Performance: offload rendering/layout al browser, alivia JACE.
- DevEx: JS tools open-source, rapid TDD.
- UX: browser features modernas.
- Futuro: cloud-native flexibility.

**UxModel paradigm**:
Cliente abre Px → station retorna JSON (no HTML strings):
```json
{ type: "CanvasPane", kids: [{ type: "Label", properties: { text: "Hello" } }] }
```
Browser convierte JSON → UxModel (API wrapper). Panes con children reciben UxModel en `doLoad()`, instancian via `kidModel.toSpandrel()`.

**APIs**:
- bajaux: widgets JS puros. Spandrel (N4.10) para tree declarations.
- BajaScript: bindings + ORD resolution.
- `BIJavaScriptWidget`: marca bajaux como reimplementation de bajaui widget.
- Type Extensions: custom Bindings + Converters (extend `nmodule/bajaui/rc/baja/binding/Binding`, `nmodule/converter/rc/Converter`).
- Styling: CSS classes `ux-Widget`, `ux-Widget-style` (external), `-t-Widget-style` (Tridium private).

**Widgets N4.10**: 50+ (Button, GridPane, Slider, TabbedPane, DashboardPane, ReportPane, kitPx).

**Audition Mode**: HTML5 Hx Profile con View Selection enabled. Media Settings Command preview sin cambiar stored. `niagara.preferUxMedia=true` force globalmente.

### 13.3.5 Nav / Root schemes

**`root:`** (`BRootScheme`, desde Baja 1.0): parse `root:` → maps siempre a `BNavRoot.INSTANCE`. Static ORD `BRootScheme.ORD`. Base para nav traversal.

**`nav:`** (`BNavScheme`, desde N4.13): traversa `BINavNode` descendientes por nombre. Separator `/` para niveles.

Ejemplos:
- `root:|nav:folder`
- `root:|nav:folder/subFolder`
- `root:|nav:folder/subFolder/myHost`

**Uso Workbench Navigator**: root: entry point. nav: drill-down en folder hierarchies. Alternativa a `slot://` para navegación.

### 13.3.6 Misc gaps

1. **RPC Discovery**: sin endpoint estándar para listar `@NiagaraRpc` methods. Clients hardcodean names. Propuesta futura: `/rpc/introspect/ord` con metadata.
2. **Report Templates**: BReportTemplate/BReportSection no en docs. Integration con iReport/JasperReports delegada.
3. **BAbsTime/BOrd JSON**: no especificado. Asumible: BOrd→string, BAbsTime→ms long (double JSON), BList→JSON array.
4. **Search indexing**: SearchService NO menciona pre-indexing (FTS). Full-scan runtime.
5. **UxMedia Type Extensions**: "development status" — ecosystem inmaduro.
6. **Nav scheme limitations**: requiere base BINavNode. No absoluto `nav:root/folder` sin composición.

---

## Síntesis del bloque

### Modelo mental

**Tres grupos ortogonales de funcionalidad** que complementan los Bloques 1-12:

1. **Subscription + Niagara Network**: arquitectura multi-station con licensing cloud. Extiende Bloque 2 (licensing) con runtime cloud checks; extiende Bloque 6/7/8 con federation Supervisor/Subordinate via FOX.

2. **Fox wire + sensitive data + virtual**: profundiza Bloque 1.5 (Fox high-level) con frame format real, detalla cómo Bloque 5.2 cifra passwords en BOG, introduce componentes virtuales como proxies cross-station.

3. **NiagaraRPC + reports + search + UxMedia + nav**: profundiza Bloque 9.3.6 (RPC high-level) con JSON encoding + CSRF + transports, añade subsystems laterales (reports, search) que usan infraestructura de bloques anteriores.

### Conexiones

- **Bloque 2** (licensing): Subscription = runtime cloud validation sobre licensing file base.
- **Bloque 3** (security JVM): station cert firma nCloud requests, valida responses. Certificate pinning.
- **Bloque 5.1** (ORD): `virtual:`, `nav:`, `root:` son schemes registrados. Complementarios a slot:/host:/file:.
- **Bloque 5.2** (BOG): `reversibleEncodingKeySource` = mecanismo concreto de encryption at rest.
- **Bloque 7** (drivers): virtual gateways proxean drivers remotos.
- **Bloque 8** (alarm/history/schedule): federation Supervisor depende de recipients, exporters/imports de esos subsystems.
- **Bloque 9** (UI): UxMedia reemplaza Hx para Px delivery moderno. NiagaraRPC vive en BWebServlet.

### Gotchas críticos

1. **Subscription grace silencioso**: si nCloud cae, grace period arranca sin notificación visible. Monitoring alarm `SubscriptionExpiresIn` crítica.
2. **FOX handshake timeout 30s**: WAN latency alta → reconnect loop. Aumentar `connectionTimeout` per environment.
3. **Keyring open silent fail**: master.jceks no accesible → BPassword reversible = empty. Debugging opaco.
4. **Virtual component circular refs**: RemoteA → B → A = deadlock FOX. Validar DAG topology.
5. **FOX session token expiry 24h** default. Post-expiry requiere re-auth (silent si cert-based).
6. **AES-256 BOG grande (~1MB)**: puede tardar segundos en decrypt boot. No paralelizable.
7. **FOX channel exhaustion** (~1000 max per session). Leaks bloquean nuevas ops. Monitor `/spy/sysManagers`.
8. **NiagaraRPC null args no soportados**: si método acepta parámetro opcional, cliente debe omitirlo o usar optional contract.
9. **UxMedia audition vs stored**: Audition Mode preview solo; cambios no persisten sin "Apply Settings".
10. **nav:/root: navigation Workbench-centric**: NOT generalizable a integraciones cross-system.
11. **Supervisor bottleneck a 50+ subordinados**: single FOX broker + history import contention. Plan scale-out con shards.

### Qué habilita

Con Bloques 1-13 podés:
- Diseñar topología multi-station Supervisor/Subordinate con subscription licensing correcto.
- Debuggear a nivel wire el FOX handshake cuando reconexión falla.
- Cifrar credenciales en BOG con 3 modos según portability vs security.
- Exponer device remoto como componente local via virtual gateway.
- Construir UxMedia Px pages modernas con BajaScript.
- Integrar REST API externa via NiagaraRpcServlet con CSRF + JSON encoding.
- Implementar reports PDF y búsquedas globales con NEQL/BQL.

**Siguiente y último**: consolidación final — índice maestro + revisión de gaps transversales.

---

## Engram topic keys

- `niagara/advanced/subscription-niagaranetwork` — Subscription licensing nCloud, Supervisor/Subordinate federation history/alarm/schedule.
- `niagara/advanced/fox-wire-crypto-virtual` — Fox frame format, op codes, session lifecycle, BSensitiveBox keyring, BOG encryption modes, virtual components.
- `niagara/advanced/rpc-reports-search-uxmedia-nav` — NiagaraRPC JSON encoding + CSRF + multi-RPC, reports framework, SearchService, UxMedia Px-as-JSON, nav/root schemes.

---

**Sesión cerrada**: 2026-04-22 — Bloque 13 consolidado. Investigación de contenido cerrada. Próximo paso: consolidación final con índice maestro.
