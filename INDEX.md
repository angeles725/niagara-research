# Niagara N4 — Mental Model · Índice Maestro

**Actualizado**: 2026-04-22 (sesión bloques 14-20)
**Distribución analizada**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY con sub-agents Explore en paralelo, contrastando docs oficiales (devguide 82 topics HTML) contra source Java + decompilado Vineflower + 969 JARs indexados.

Este índice te guía entre los **20 bloques** de investigación. Cada bloque es un archivo `.md` independiente que puede leerse aislado, pero las conexiones están explícitamente marcadas entre sí.

Cobertura final estimada: **~92-95%** del framework Niagara N4.14 conceptualmente. Los gaps restantes están catalogados en el Bloque 20.10 (final gap analysis).

---

## Mapa completo

### Capa 1 — Infraestructura (Bloques 1-3)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 1 | Estructura del framework | [niagara-mental-model.md §1](niagara-mental-model.md) | Profiles rt/ux/wb, module.xml, NRE/Station/Workbench procesos, registry de tipos, Fox protocol |
| 2 | Licenciamiento | [niagara-mental-model.md §2](niagara-mental-model.md) | HostId, Cert, License XML, Features, SMA, LicenseManager API, Honeywell OEM overlay |
| 3 | Modelo de seguridad (sandbox JVM) | [niagara-mental-model.md §3](niagara-mental-model.md) | Cert chain pipeline, 19 permission groups (corregido: 25 en Bloque 18.4), policy files firmados, NiagaraSocketPermission, skipModuleValidation |

### Capa 2 — Modelo de programación (Bloques 4-6)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 4 | Baja Object Model | [niagara-mental-model-bloque4.md](niagara-mental-model-bloque4.md) | Slot system (21 flags), BObject→BValue→BComplex/BSimple/BStruct jerarquía, BComponent lifecycle callbacks, BFacets, dynamic slots, slot-o-matic-2000 annotations |
| 5 | Navegación + persistencia + queries | [niagara-mental-model-bloque5.md](niagara-mental-model-bloque5.md) | ORD (29 schemes), resolution pipeline, BOG format (handles + LoadOp + atomic writes), BQL / NEQL / Hierarchy / Tag Dictionary |
| 6 | Control Engine | [niagara-mental-model-bloque6.md](niagara-mental-model-bloque6.md) | Execution engine event-driven NO scan cycle, Clock API, BLink + 6 taxonomy, Knobs vs Links, BConversionLink, priority array 16-level, BRelation, kitControl 100+ blocks, ProxyExt, extensions |

### Capa 3 — Integraciones (Bloques 7-9)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 7 | Drivers Framework | [niagara-mental-model-bloque7.md](niagara-mental-model-bloque7.md) | Jerarquía 4 niveles (Container/Network/Device/Point), ProxyExt pipeline 5 fases, comm models (poll/COV/event), tuning policies, BACnet (IP/MSTP/PTP + BBMD + priority array mapping), Modbus/MQTT/LON/KNX/OBIX/SNMP |
| 8 | Alarm + History + Schedule | [niagara-mental-model-bloque8.md](niagara-mental-model-bloque8.md) | Alarm pipeline source→class→recipient + transitions + ack workflow + BAlarmArchive, BHistoryService connection-oriented + Interval/COV extensions + TrendRecord binary + Supervisor collection, Schedule contract isEffective/nextEvent stateless + DFS prioridad + DST |
| 9 | UI Stack | [niagara-mental-model-bloque9.md](niagara-mental-model-bloque9.md) | BWbShell + gx primitivos + Px declarative XML + bajaui widgets + Velocity, BajaScript v2 + ux + hx legacy 4 capas + RequireJS + JxBrowser, Jetty embedded + BWebService + BWebServlet + NiagaraRPC multi-transport + REST Analytics API |

### Capa 4 — Operaciones (Bloques 10-12)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 10 | Platform & Station lifecycle | [niagara-mental-model-bloque10.md](niagara-mental-model-bloque10.md) | Platform daemon niagarad (C/C++ nativo, puerto 5011/HTTPS), Station boot 8 fases (luego 6-phase refinado Bloque 20.3), service dependency resolution, file system !config/!sys/!fox/!file, spy pages, backup/restore `.dist`, Station Copier, commissioning, DR |
| 11 | Auth + RBAC runtime | [niagara-mental-model-bloque11.md](niagara-mental-model-bloque11.md) | BUser/BRole/BCategory 64-bit mask/BPermissions 6-bit rwi+RWI, BUserService + BRoleService, evaluation flow, 9 auth schemes (Digest SCRAM-SHA256, SAML 2.0, LDAP, Kerberos, Cert mTLS, Google TOTP, HTTP Basic), session lifecycle + AutoLogoff + modular enterprise features |
| 12 | Build system + dev lifecycle | [niagara-mental-model-bloque12.md](niagara-mental-model-bloque12.md) | Gradle + niagara-module plugin 7.6.17, tasks (jar/slotomatic/sign/dist/bajadoc/niagaraTest), multi-profile rt/ux/wb, AX→N4 migration + lexicons + BLexicon API + %lexicon(key)% placeholder, TestNG framework, .palette format, help deployment `-doc` profile, popup vs property editors |

### Capa 5 — Gaps profundos (Bloque 13)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 13 | Gaps profundos | [niagara-mental-model-bloque13.md](niagara-mental-model-bloque13.md) | Subscription licensing nCloud + Niagara Network federation Supervisor/Subordinate, Fox wire protocol frames + sensitive data keyring + BOG encryption + virtual components, NiagaraRPC JSON encoding + CSRF, Reports/Search/UxMedia N4.10+ + nav/root schemes |

### Capa 6 — Operaciones avanzadas + Templates + Licensing runtime (Bloques 14-16)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 14 | Point counting + Templates + Batch Editor | [niagara-mental-model-bloque14.md](niagara-mental-model-bloque14.md) | BIPointCountable rules + license limits runtime (point/device/history/camera/foxStream), federation counting en origen, Niagara Templates core (BTemplateService + .ntpl NO auto-propaga) vs EasyTemplates Honeywell (BEasyTemplatingService + .px/.etso + NEQL), 4 mecanismos coexistentes (Niagara/Easy/Palette/Station), Batch Editor BBatchJobService license provisioning, LON Template/Match/Bind ProgramId 8-byte wildcards |
| 15 | Workbench editing deep | [niagara-mental-model-bloque15.md](niagara-mental-model-bloque15.md) | Wiresheet BWsCanvas + glyph composite hierarchy + BWsAnnotation + state machine 5 states + manhattan routing, Property Sheet BWbComponentView + ComponentTableModel + FieldEditor 3-level resolution facets-driven, Nav tree BNavTree + BINavNode + BFoxProxySession reference counting, Point/Device Manager BFolderManager subclases + Template/Match/Bind integration DevTemplateMgr, workflow 5 fases end-to-end, polling limits empíricos (1-2k @ 1s safe, 5k @ 5s safe, 5k @ 1s marginal) |
| 16 | Analytics Framework + Provisioning Service | [niagara-mental-model-bloque16.md](niagara-mental-model-bloque16.md) | BAnalyticService (com.tridiumx.analytics sin s) + 55+ algorithm blocks (BPsychrometric HVAC + BConsumptionToDemand), NO BRule explícito (pollers BCyclic/BTriggered + BAnalyticAlert implements BIAlarmSource tight/loose coupling), BNaServlet /na Content-Type text/plain + 7 endpoints + subscription TTL 60s, roles NA_API+NA_charts, NO Skyspark connector, BProvisioningService + BNiagaraNetworkJob 2-stage FIXED (Initial + ForEachStation), step types (Backup online excluye .hdb, UpdateLicenses Initial single round-trip, Restore/Copy/Upgrade/Certificate/Report), escalera de escala PropSheet→PointMgr→BatchEditor→StationCopier→ProvisioningCopyStep→BNiagaraNetworkJob |

### Capa 7 — Filesystem forensics + Dev ops + Security operacional (Bloques 17-18)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 17 | Filesystem forensics + JRE embebido | [niagara-mental-model-bloque17.md](niagara-mental-model-bloque17.md) | Install Home completo (bin/ executables 13+ + DLLs + libciper.so ARM 32-bit QNX + ext/ BouncyCastle FIPS/Std + policy/ PKCS7-signed + META-INF/ trust root + modules/ 969 JARs + defaults/ system.properties críticos), User Home (/home/cristian/Niagara4.14/OptimizerSupervisor/ verificado WSL) + Daemon Home (ProgramData Windows), comparativa 3 Homes trust boundary, JRE Azul Zulu JDK 1.8.0_412 x64 + 10 security providers + FIPS mode BCFKS keystore, defaults críticos (moduleVerificationMode=low, circuitMaxReceiveBuffer=10MB, heap 1GB conservador) |
| 18 | Module signing + permissions + CSRF + HELLO/SCRAM | [niagara-mental-model-bloque18.md](niagara-mental-model-bloque18.md) | Plugin `com.tridium:niagara-signing-plugin:1.0.10` única vía standalone, `.sig` 256B raw RSA-2048 (NO PKCS7), cert Honeywell hardcoded signing.properties (year 9999 eternal), bypass matrix (Webs.license developer feature skipModuleValidation=true, Honeywell.license no), corrección Bloque 3.4: 3 groups que SIEMPRE requieren firma son ACCESS_CLASS+REFLECTION+MBEAN_PERMISSION (no BINDING/PRIVILEGE), source `<niagara-permission-groups>` vs runtime `<java-permissions>` formato distinto, CSRF CsrfProtectedFilter + `x-niagara-csrfToken`, HELLO+SCRAM-SHA256 6-step flow via BHttpHeaderCallbackHandler, exemptions.tes TES binary bypass user-level |

### Capa 8 — Drivers verticales + Protocolos wire (Bloque 19)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 19 | LON deep + NRIO + NiagaraDriver + BOX protocol | [niagara-mental-model-bloque19.md](niagara-mental-model-bloque19.md) | LON 6 módulos (lonworks-rt/ux/wb + lonHoneywell + lonSiebe + ascLon), XIF/LNML format ejemplos Rio/Mnlrv3, ProgramId 8-byte breakdown + wildcards (Tridium 80 00, Honeywell 80 00 0c, Siebe 80 00 16), 7 NM verbs hex (0x50 QUERY_DOMAIN..0x70 RESET), SNVT conversions table, throughput TP/FT-10 200 NV/s vs TP/XF-1250 3000 NV/s, NRIO Honeywell RS-485 con unsolicited push + redundancia Pri/Sec, otros drivers Honeywell (honEdgeDriver/honConnectedPower/bport/maxpro/honPlantController libplantctrl.so/honeywellBacnetDeviceManager/honAdvWirelessCfg), NiagaraDriver BNiagaraNetwork + BNiagaraStation + 6 device extensions + 6 Fox channels multiplexados, NO HA nativa (BSupervisorFailover no existe), BOX protocol (Building Object eXchange) verificado WebSocket+JSON distinto Fox, 8 BOX channels, corrección Bloque 9.2: BajaScript browser usa BOX no Fox |

### Capa 9 — Misc residuales + Gap analysis final (Bloque 20)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 20 | BApp + net + BAbstractService + Monitors + JobService + gap analysis | [niagara-mental-model-bloque20.md](niagara-mental-model-bloque20.md) | BApp (NO BAbstractApp) extends BComponent + subclase BWebApp, net-rt centralizado (BInternetAddress + HttpConnection + BHttpProxyService CIDR exclusions), BAbstractService 2 sync callbacks + async futures N4+ + 6-phase boot + 3 fault states, systemMonitor-rt (NO stationMonitor) + 10 monitor classes, EngineManager + LeaseManager (4 tipos leases) + ResourceManager + JMX ports 9010/9011, BJobService sin license vs BBatchJobService provisioning-gated + BJob lifecycle + MonitorWorker 2-sec, persistent policies defaults (History 500 roll, Alarm file ADB sin auto-ack, Audit sin auto-delete, Backup sin retention, Session 5/30s→10s+15min auto-logoff), NO BLoggingService/BDebugService/BLexiconService standalone, **final gap analysis: 27+ áreas sin cubrir** (transaction semantics, clustering HA, perf tuning, enterprise vendor deep, FIPS workflow, federation providers, security rotation, production gotchas) |

---

## Cómo leer este mental model

### Si sos **nuevo a Niagara**

Leé en orden: 1 → 2 → 3 → 4 → 5. Los primeros 5 bloques te dan la base conceptual completa. Podés profundizar en 6-20 según necesidad del proyecto.

### Si venís a **implementar un driver**

Ruta: 4 (slots) → 5 (ORD) → 6 (ControlPoint + ProxyExt) → **7 (driver framework)** → 8 (extensions) → 14.10 (device templates) → **19 (LON/NRIO/otros drivers Honeywell)**.

### Si venís a **implementar una UI**

Ruta: 4 (BComponent) → 5 (ORD + BOG) → 6 (control) → **9 (UI stack completo)** → 13.3 (NiagaraRPC + UxMedia) → **15 (Workbench editing deep)** → 19.17 (BOX protocol para BajaScript browser).

### Si venís a **investigar seguridad**

Ruta: **3 (sandbox JVM)** → 11 (RBAC user/role/permission) → 9.3 (web auth) → 13.2 (Fox wire + sensitive data + keyring) → **17 (filesystem security + 3 Homes trust boundary + JRE)** → **18 (module signing + permissions real + CSRF + HELLO/SCRAM + exemptions.tes)**.

### Si venís a **operar/administrar**

Ruta: 1 → 2 (licensing) → **10 (platform+station)** → 11.3 (session) → 12 (build) → 13.1 (Niagara Network) → **14 (point counting + templates)** → **15 (Workbench workflow end-to-end)** → **16 (Analytics + Provisioning Supervisor)** → **20 (monitors + persistent policies defaults)**.

### Si venís a **federar multi-station**

Ruta: 13.1 (Niagara Network) → 13.2 (Fox wire) → **16.9-16.18 (Provisioning Service BNiagaraNetworkJob 2-stage)** → **19.11 (NiagaraDriver BNiagaraNetwork + 6 device extensions + 6 Fox channels)** → 19.14 (NO HA nativa gap).

### Si venís a **analytics + dashboards**

Ruta: 5.3 (BQL/NEQL) → 8.2 (History) → 9.3.7 (Analytics Web API overview) → **16.1-16.8 (Analytics Framework deep — BAnalyticService + 55 blocks + BAnalyticAlert BIAlarmSource + BNaServlet)**.

### Si venís a **debuggear problemas**

Empezá por los gotchas transversales (sección siguiente), después buscá el bloque específico. Para performance: **20.5.1 EngineManager $HogsPage** es el primer stop.

---

## Gotchas transversales — por frecuencia

Los gotchas más críticos repetidos o conectados entre bloques:

### Concurrency y threading

- **Bloque 6.1.5**: engine thread único — callback lento congela station.
- **Bloque 6.1.6**: NO topology sort en links — stack overflow en loops recursivos. Mitigar `Flags.ASYNC`.
- **Bloque 6.2.7**: feedback loops A→B→A no detectados globalmente. Solo self-link.
- **Bloque 9.3.2**: Jetty worker thread ≠ engine thread. Llamar `.get()` en servlet = deadlock. Usar FOX/BOX calls o `post()`.
- **Bloque 4.3.3**: re-entrar `.add()` dentro de `added()` callback = deadlock.
- **Bloque 15.13.1**: **last write wins** sin merge — 2 Workbench concurrentes editando sobrescriben silent.
- **Bloque 20.5.1**: `EngineManager$HogsPage` es tool debugging para identificar callbacks pesados.

### Persistencia y encryption

- **Bloque 5.2.2**: keySource `none` portable sin passwords reversibles; `keyring` seguro host-específico; `external` portable con passphrase.
- **Bloque 13.2.4**: master.jceks no accesible → BPassword reversibles empty silencioso. Debugging opaco.
- **Bloque 10.3.3**: online backup excluye `.hdb`/`.adb` — necesitás offline para integridad completa. Confirmado en Bloque 16.11.1 (BProvisioningBackupStep).
- **Bloque 5.2.7**: LoadOp corre en engine thread — heavy deserialización bloquea callbacks.
- **Bloque 14.6.3**: Niagara Templates NO auto-propagan a instances cuando cambia template source. Requiere `BUpgradeTemplateJob` explícito.
- **Bloque 20.7.3**: BJob NO persistido BOG default — crash mid-job = job perdido (excepto BStationSaveJob).

### Security / Auth

- **Bloque 3.3.4 + 18.4.4**: los 3 groups que SIEMPRE requieren firma son **ACCESS_CLASS + REFLECTION + MBEAN_PERMISSION** (NO "BINDING"/"PRIVILEGE" como decía originalmente).
- **Bloque 3.2**: 3 archivos `bin/policy/` firmados PKCS7 — modificar rompe integridad.
- **Bloque 10.1.2**: platform credentials (5011) ≠ station BUser (FOX). Dos cuentas separadas.
- **Bloque 11.3.4 + 20.8.5**: default lockout 5 failures/30s → 10 seg + session 15 min auto-logoff. Configurable.
- **Bloque 11.3.5**: password complexity NO enforcement nativo.
- **Bloque 13.2.3**: FOX session 24h — re-auth silent cert-based.
- **Bloque 17.6**: `moduleVerificationMode=low` hardcoded en defaults/system.properties — base del bypass.
- **Bloque 18.3.2**: `skipModuleValidation` requires AND (flag + license feature). Webs.license tiene, Honeywell.license NO.
- **Bloque 18.9**: `exemptions.tes` user-level — puerta trasera oficial menos invasiva que flags JVM.

### Scale y performance

- **Bloque 5.3.5**: Hierarchy service caro (NEQL per nivel × N). Cachear.
- **Bloque 5.3.5**: NEQL NO aggregate functions. Para stats combinar con BQL.
- **Bloque 8.2.8**: HistorySpaceConnection AutoCloseable — try-with-resources obligatorio o DB locked.
- **Bloque 13.1.7 + 19.13**: Supervisor bottleneck ~50 subordinados. FOX broker saturation + history import contention. Confirmado NiagaraDriver.
- **Bloque 13.2.5 + 19.11.3**: FOX channel exhaustion ~1000/session. Leaks bloquean (station restart required para cleanup).
- **Bloque 15.14.1**: polling limits empíricos: 1-2k puntos @ 1s SAFE, 5k @ 5s SAFE, 5k @ 1s MARGINAL.
- **Bloque 17.5.5**: heap default `-Xmx1024M` conservador. Explica Supervisor bottleneck.
- **Bloque 19.6.3**: LON max ~32K devices/domain (254 subnets × 127 nodes).
- **Bloque 20.8.4**: Backup restore sin chunking/resume — timeouts en high-latency.

### Licensing

- **Bloque 2.1**: SMA es atributo de feature, no feature propia.
- **Bloque 2.6 + 18.3.2**: `Feature.getb("skipModuleValidation", false)` license-gated bypass.
- **Bloque 13.1.1**: Subscription grace silencioso si nCloud cae. Alarm `SubscriptionExpiresIn` crítica.
- **Bloque 14.1.2**: history records dimensión SEPARADA de point.limit (`history.limit` / `historyExt.limit` / `historyRecord.limit`).
- **Bloque 14.3**: `sma.exempt="true"` atributo raro (http feature) — operational tier sin SMA continuo.
- **Bloque 14.3**: `nCloudDriver` tiene limits restrictivos (point.limit=1000, device.limit=1).
- **Bloque 14.4**: federation counting en origen (Sub), NO en Supervisor.
- **Bloque 14.2.2**: hard cap inmediato (no grace) para count. Grace 24-48h solo para expiry.

### Build y dev

- **Bloque 1.1**: profile matrix estricto — `rt` NO puede importar de `wb` nunca.
- **Bloque 12.1.4**: `module-include.xml` NO editable manual — regenerado cada build.
- **Bloque 12.2.7**: lexicon fallback silent si falta key.
- **Bloque 14.11.3**: Batch Editor sin pattern replacement `${i}` — workaround BajaScript.
- **Bloque 18.1.3**: `.sig` 256B raw RSA-2048 (no PKCS7) — jarsigner estándar NO sirve, solo plugin `com.tridium:niagara-signing-plugin:1.0.10`.
- **Bloque 18.4.3**: formato runtime `<java-permissions>` vs source `<niagara-permission-groups>` difieren — transformación en gradle build.

### UI

- **Bloque 9.1.5**: `axvelocity` license requerida para `.pxvm` files.
- **Bloque 9.2.3**: HxOps orden write/save/update/process obligatorio.
- **Bloque 9.3.5**: `sameSite=None` requiere `secure=true` (HTTPS) browsers modernos.
- **Bloque 15.1.3**: `BWsAnnotation` persist per-folder, no global — copy+paste a otra carpeta genera nueva anotación independiente.
- **Bloque 15.3.2**: wiresheet routing solo manhattan (NO bezier/straight nativo).
- **Bloque 15.14**: copy+paste NO auto-incrementa IPs/BACnet IDs — requiere BajaScript.
- **Bloque 15.14**: NO zoom in/out nativo en Workbench (sí web wiresheet ux).

### Runtime semantics

- **Bloque 4.2.4**: BComponent NO linkable directo — usar BOrd (BSimple).
- **Bloque 4.2.1**: `equals()` default en BObject = identidad. Solo BValue compara valor. Usar `equivalent()`.
- **Bloque 6.3.6**: priority levels 1 (emergency) y 8 (manual) **persisten en BOG** (excepción al TRANSIENT).
- **Bloque 8.3.7**: Schedule DST 2:30am fall-back → Niagara usa hora estándar (winter offset).
- **Bloque 15.8.4**: Facets NO enforced en load — solo en edit UI. API puede crear valores fuera range.
- **Bloque 15.14**: device offline proxy points siguen contando hacia `point.limit` (no auto-delete).

### Analytics + Provisioning + Federation

- **Bloque 16.1.1**: package `com.tridiumx.analytics` (con **x**), clase `BAnalyticService` (sin **s**), path `/Services/AnalyticService` — naming peculiaridad histórica.
- **Bloque 16.3.1**: NO existe `BRule` — pollers `BCyclicPoller`/`BTriggeredPoller` + `BAnalyticAlert` combinados.
- **Bloque 16.5.5**: Analytics Web API subscription TTL **60 seg** auto-expire — requiere poll cada 2-5 seg.
- **Bloque 16.5.1**: Analytics API usa `Content-Type: text/plain` (no `application/json`) — peculiaridad.
- **Bloque 16.10.1**: `BNiagaraNetworkJob` 2-stage FIXED (Initial + ForEachStation) — no arbitrary stages.
- **Bloque 19.13**: Fox channel leak en proxy delete mid-subscription → station restart required para cleanup.
- **Bloque 19.14**: NO HA nativa NiagaraDriver — `BSupervisorFailover` no existe.
- **Bloque 19.17**: BajaScript browser usa **BOX** (no Fox). Corrección al Bloque 9.2.

---

## Conexiones clave entre bloques

Grafo de referencias cross-bloque:

```
Bloque 1 (Estructura) ─── fundamenta ───> 2, 3, 4, 10, 17
Bloque 2 (Licensing) ─── base para ───> 13.1, 14.1-14.4, 16.1.3
Bloque 3 (Security JVM) ─── complementa ───> 11, 13.1, 13.2, 17.4, 18
Bloque 4 (Baja) ─── substrate para ───> 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 20
Bloque 5 (ORD/BOG) ─── mecanismo para ───> 6, 10.2.3, 13.2, 15.1.3, 20
Bloque 6 (Control) ─── consumido por ───> 7, 8, 15.3, 16.2
Bloque 7 (Drivers) ─── conecta con ───> 6.2.6, 14.12, 15.10, 15.11, 19
Bloque 8 (Alarm/History/Schedule) ─── federado en ───> 13.1, 16.4, 19.11.6, 20.8
Bloque 9 (UI) ─── consume ───> 11, 13.3, 15, 16.5, 19.17 (BOX corrección)
Bloque 10 (Platform) ─── contexto para ───> 11.3, 17, 20.3.2 (boot phases refinement)
Bloque 11 (RBAC) ─── ortogonal a ───> Bloque 3; consumido por ───> 20.8.5
Bloque 12 (Build) ─── genera ───> 4-9 artifacts; plugin 18.1.1
Bloque 13 (Gaps) ─── profundiza ───> 1.5, 2, 5.1, 5.2, 9.3.6, 9.1; expandido por 19.11 (NiagaraDriver)
Bloque 14 (Templates + Counting) ─── integra ───> 2, 4, 8, 10.3, 13.1, 16.17 (escala)
Bloque 15 (Workbench) ─── consume ───> 4, 5, 6, 7, 8, 9.1, 11, 14; conecta 16
Bloque 16 (Analytics + Provisioning) ─── extends ───> 8, 9.3.7, 13.1, 14.15, 15; implementation BAbstractService Bloque 20.3
Bloque 17 (Filesystem + JRE) ─── establece ───> paths físicos que 18, 20 citan
Bloque 18 (Signing + Perms + CSRF) ─── corrige ───> 3.4 (3 groups sign); expande ───> 9.3.6, 11.3, 12.1
Bloque 19 (LON/NRIO/NiagaraDriver/BOX) ─── profundiza ───> 7.3.3, 13.1, 13.2, 14.12; corrige ───> 9.2 (BajaScript usa BOX)
Bloque 20 (BApp + Misc + Gap) ─── refina ───> 10.2.2 (boot 6-phase), 18.4 (JMX MBEAN); gap analysis para todo
```

---

## Engram topic keys (toda la memoria persistente)

**Total: 57 topic keys** bajo `project: niagara-research`.

### Capa 1 (Bloques 1-3) — 10 keys
- `niagara/estructura/profiles-rt-ux-wb`, `niagara/estructura/registry-types`, `niagara/estructura/fox-protocol`
- `niagara/licensing/sma-attribute-model`, `niagara/licensing/honeywell-oem-overlay`, `niagara/licensing/license-manager-api`
- `niagara/security/cert-chain-pipeline`, `niagara/security/permission-groups-19-table`, `niagara/security/skip-module-validation-bypass`, `niagara/security/policy-files-triple-signed`

### Capa 2 (Bloques 4-6) — 9 keys
- `niagara/baja/slot-system`, `niagara/baja/type-hierarchy`, `niagara/baja/lifecycle-facets-dynamic`
- `niagara/navigation/ord-system`, `niagara/persistence/bog-format`, `niagara/queries/bql-neql-hierarchy-tags`
- `niagara/execution/engine-thread-model`, `niagara/execution/link-model-binding`, `niagara/control/kitcontrol-blocks`

### Capa 3 (Bloques 7-9) — 9 keys
- `niagara/drivers/framework-generico`, `niagara/drivers/bacnet-detalle`, `niagara/drivers/otros-modbus-lon-obix-snmp`
- `niagara/subsystems/alarm-pipeline`, `niagara/subsystems/history-service`, `niagara/subsystems/schedule-subsystem`
- `niagara/ui/workbench-px-gx`, `niagara/ui/bajascript-ux-hx`, `niagara/ui/servlets-jetty-webservices`

### Capa 4 (Bloques 10-12) — 9 keys
- `niagara/platform/daemon-niagarad`, `niagara/platform/station-boot-filesystem`, `niagara/platform/backup-dist-disaster-recovery`
- `niagara/auth/user-role-category-permission`, `niagara/auth/authentication-schemes`, `niagara/auth/session-autologoff-enterprise`
- `niagara/build/gradle-plugin`, `niagara/build/ax-n4-migration-lexicons`, `niagara/build/testing-palettes-help-editors`

### Capa 5 (Bloque 13) — 3 keys
- `niagara/advanced/subscription-niagaranetwork`, `niagara/advanced/fox-wire-crypto-virtual`, `niagara/advanced/rpc-reports-search-uxmedia-nav`

### Capa 6 (Bloques 14-16) — 9 keys
- `niagara/licensing/point-counting-limits-runtime`, `niagara/templates/niagara-core-vs-easytemplates-honeywell`, `niagara/operations/batch-editor-lon-template-match-bind`
- `niagara/ui/wiresheet-editor-glyphs-state-machine`, `niagara/ui/property-sheet-nav-tree-fieldeditor`, `niagara/ui/point-device-manager-workflow-end-to-end`
- `niagara/analytics/framework-core-algorithms-pollers-alerts`, `niagara/analytics/web-api-rest-servlet-subscription`, `niagara/provisioning/service-niagaranetworkjob-stages-steps`

### Capa 7 (Bloques 17-18) — 6 keys
- `niagara/filesystem/install-home-layout`, `niagara/filesystem/user-daemon-homes`, `niagara/platform/jre-embebido-azul-zulu-jdk8`
- `niagara/security/module-signing-standalone-gradle-plugin`, `niagara/security/module-permissions-xml-source-runtime`, `niagara/security/csrf-header-auth-annotations-exemptions`

### Capa 8 (Bloque 19) — 3 keys
- `niagara/drivers/lon-deep-xif-lnml-snvt-programid`, `niagara/drivers/nrio-niagaradriver-station-federation`, `niagara/protocols/box-protocol-websocket-json`

### Capa 9 (Bloque 20) — 3 keys
- `niagara/misc/bapp-webapp-net-module-httpproxy`, `niagara/misc/babstractservice-lifecycle-monitors-engine-lease-resource`, `niagara/misc/bjobservice-persistent-policies-gap-analysis`

---

## Qué NO cubre este mental model — gap analysis final consolidado

De los 20 bloques, las 27+ áreas específicas no cubiertas o cubiertas superficialmente (catálogo completo en Bloque 20.10):

### Arquitectura + patrones
1. Transaction semantics multi-step (rollback/compensation)
2. Clustering + distributed topology HA (confirmado NO nativo en NiagaraDriver)
3. Module lifecycle hooks pre/post load
4. Performance tuning specifics (thread pools, GC, I/O)
5. Custom type system `Sys.loadType()` extensions
6. Migration patterns `config.bog` schema evolution intra-N4

### Enterprise + vendor-specific
7. Honeywell modules deep (platPower, jsonToolkit, honPlantController `libplantctrl.so`)
8. SMA licensing flow operacional
9. Remote diagnostics channels vendor-specific
10. FIPS compliance workflow operacional completo

### Third-party integration
11. LDAP/SAML/OAuth federation providers deep
12. External datasources (Oracle, SQL Server, timeseries externos)
13. Serial + Modbus TCP non-HTTP deep (`fox.sys` + `ndriver` packages)
14. Skyspark o alternativas analytics third-party

### Security
15. Key rotation workflow (`master.jceks`, TLS certs)
16. Token expiry cross-protocol
17. RBAC enforcement en method invocation level + Auditor integration

### Production gotchas probables
18. TimeZone handling en multi-zone archives
19. `Clock.time()` drift en RTC sync events
20. ForkJoinPool parallelism vs blocking I/O tuning
21. Audit event loss si `BAuditService` unavailable
22. Job exception handling framework-level persistence
23. Large backup restore timeout (sin chunking/resume)
24. History archive DB compaction blocks UI
25. Session timeout clock skew multi-server
26. Lockout window edge case en clock adjustment backward
27. Audit retention sin auto-delete built-in

Para cualquiera de estos, el mental model actual (20 bloques) es suficiente para orientarse y atacar con investigación puntual adicional.

---

## Próximos pasos recomendados

Con los 20 bloques cerrados, tenés **~92-95%** del framework Niagara N4.14 entendido conceptualmente. Lo que queda:

1. **Práctica**: implementar un módulo end-to-end (driver simple + control logic + UI + proxy points + Analytics algorithm) usando el conocimiento. El Bloque 15.13 (workflow 5 fases) es la receta.
2. **Debugging real**: cuando surja un problema de producción, usar el mental model para localizar el bloque relevante + gotchas transversales.
3. **Updates puntuales**: cuando Tridium/Honeywell release N4.15+ o features nuevas, actualizar bloques específicos en vez de re-investigar.
4. **Contribución inversa**: si identificás gotchas nuevos en uso real, agregarlos a los bloques correspondientes vía commit directo.
5. **Deep dives en gaps**: elegir 1-2 de los 27+ gaps (ej. HA clustering, FIPS workflow, transaction semantics) para profundizar según criticidad del deployment.

---

## Repositorio

GitHub: https://github.com/angeles725/niagara-research (privado)

Estructura del repo:
```
/
├── INDEX.md (este archivo)
├── niagara-mental-model.md           (Bloques 1-3)
├── niagara-mental-model-bloque4.md   (Baja Object Model)
├── niagara-mental-model-bloque5.md   (ORD + BOG + Queries)
├── niagara-mental-model-bloque6.md   (Control Engine)
├── niagara-mental-model-bloque7.md   (Drivers Framework)
├── niagara-mental-model-bloque8.md   (Alarm + History + Schedule)
├── niagara-mental-model-bloque9.md   (UI Stack)
├── niagara-mental-model-bloque10.md  (Platform + Station)
├── niagara-mental-model-bloque11.md  (Auth + RBAC)
├── niagara-mental-model-bloque12.md  (Build + Dev Lifecycle)
├── niagara-mental-model-bloque13.md  (Gaps profundos)
├── niagara-mental-model-bloque14.md  (Point counting + Templates + Batch Editor)
├── niagara-mental-model-bloque15.md  (Workbench editing deep)
├── niagara-mental-model-bloque16.md  (Analytics + Provisioning)
├── niagara-mental-model-bloque17.md  (Filesystem forensics + JRE)
├── niagara-mental-model-bloque18.md  (Module signing + permissions + CSRF + SCRAM)
├── niagara-mental-model-bloque19.md  (LON + NRIO + NiagaraDriver + BOX)
├── niagara-mental-model-bloque20.md  (BApp + net + Monitors + JobService + gap analysis)
├── niagara-mental-model.2026-04-19.md (snapshot sesión httpapi)
├── NEXT_SESSION_PROMPT.md, NEXT_SESSION_PROMPT_MODULE_NAVIGATOR.md (plantillas)
├── notes/                            (borradores source)
└── .atl/                             (SDD registry)
```

---

**Sesión cerrada**: 2026-04-22 — Mental model Niagara N4 consolidado en **20 bloques** con ~92-95% coverage conceptual. 57 topic keys engram. 27+ gaps documentados honestamente para futuro.

Si este mental model te ahorró horas de investigación o te evitó un bug de producción, el objetivo está cumplido.
