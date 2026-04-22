# Niagara N4 — Mental Model · Índice Maestro

**Completado**: 2026-04-22
**Distribución analizada**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY con sub-agents Explore en paralelo, contrastando docs oficiales (devguide 82 topics HTML) contra source Java + decompilado Vineflower + 926 JARs indexados.

Este índice te guía entre los 13 bloques de investigación. Cada bloque es un archivo `.md` independiente que puede leerse aislado, pero las conexiones están explícitamente marcadas entre sí.

---

## Mapa completo

### Capa 1 — Infraestructura (Bloques 1-3)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 1 | Estructura del framework | [niagara-mental-model.md §1](niagara-mental-model.md) | Profiles rt/ux/wb, module.xml, NRE/Station/Workbench procesos, registry de tipos, Fox protocol |
| 2 | Licenciamiento | [niagara-mental-model.md §2](niagara-mental-model.md) | HostId, Cert, License XML, Features, SMA, LicenseManager API, Honeywell OEM overlay |
| 3 | Modelo de seguridad (sandbox JVM) | [niagara-mental-model.md §3](niagara-mental-model.md) | Cert chain pipeline, 19 permission groups, policy files firmados, NiagaraSocketPermission, skipModuleValidation |

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
| 10 | Platform & Station lifecycle | [niagara-mental-model-bloque10.md](niagara-mental-model-bloque10.md) | Platform daemon niagarad (C/C++ nativo, puerto 5011/HTTPS), Station boot 8 fases, service dependency resolution, file system !config/!sys/!fox/!file, spy pages, backup/restore `.dist`, Station Copier, commissioning, DR |
| 11 | Auth + RBAC runtime | [niagara-mental-model-bloque11.md](niagara-mental-model-bloque11.md) | BUser/BRole/BCategory 64-bit mask/BPermissions 6-bit rwi+RWI, BUserService + BRoleService, evaluation flow, 9 auth schemes (Digest SCRAM-SHA256, SAML 2.0, LDAP, Kerberos, Cert mTLS, Google TOTP, HTTP Basic), session lifecycle + AutoLogoff + modular enterprise features |
| 12 | Build system + dev lifecycle | [niagara-mental-model-bloque12.md](niagara-mental-model-bloque12.md) | Gradle + niagara-module plugin 7.6.17, tasks (jar/slotomatic/sign/dist/bajadoc/niagaraTest), multi-profile rt/ux/wb, AX→N4 migration + lexicons + BLexicon API + %lexicon(key)% placeholder, TestNG framework, .palette format, help deployment `-doc` profile, popup vs property editors |

### Capa 5 — Gaps profundos (Bloque 13)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 13 | Gaps profundos | [niagara-mental-model-bloque13.md](niagara-mental-model-bloque13.md) | Subscription licensing nCloud + Niagara Network federation Supervisor/Subordinate, Fox wire protocol frames + sensitive data keyring + BOG encryption + virtual components, NiagaraRPC JSON encoding + CSRF, Reports/Search/UxMedia N4.10+ + nav/root schemes |

---

## Cómo leer este mental model

### Si sos **nuevo a Niagara**

Leé en orden: 1 → 2 → 3 → 4 → 5. Los primeros 5 bloques te dan la base conceptual completa. Podés profundizar en 6-13 según necesidad del proyecto.

### Si venís a **implementar un driver**

Ruta: 4 (entender slots) → 5 (ORD para referencias) → 6 (ControlPoint + ProxyExt) → **7 (driver framework)** → 8 (extensions integration).

### Si venís a **implementar una UI**

Ruta: 4 (BComponent) → 5 (ORD + BOG) → 6 (control extensions) → **9 (UI stack completo)** → 13.3 (NiagaraRPC + UxMedia modern).

### Si venís a **investigar seguridad**

Ruta: **3 (sandbox JVM)** → 11 (RBAC user/role/permission) → 9.3 (web auth) → 13.2 (Fox wire + sensitive data + keyring).

### Si venís a **operar/administrar**

Ruta: 1 (estructura) → 2 (licensing) → **10 (platform+station)** → 11.3 (session mgmt) → 12 (build lifecycle) → 13.1 (Niagara Network topology).

### Si venís a **debuggear problemas**

Empezá por los gotchas transversales (sección siguiente), después buscá el bloque específico.

---

## Gotchas transversales — por frecuencia

Los gotchas más críticos repetidos o conectados entre bloques:

### Concurrency y threading

- **Bloque 6.1.5**: engine thread único — un callback lento congela el station.
- **Bloque 6.1.6**: NO topology sort en links — stack overflow en loops recursivos profundos. Mitigar con `Flags.ASYNC`.
- **Bloque 6.2.7**: feedback loops A→B→A no detectados globalmente. Solo self-link.
- **Bloque 9.3.2**: Jetty worker thread ≠ engine thread. Llamar `.get()` de BComponent en servlet = riesgo deadlock. Usar FOX/BOX calls o `post()`.
- **Bloque 4.3.3**: re-entrar `.add()` dentro de `added()` callback = deadlock.

### Persistencia y encryption

- **Bloque 5.2.2**: keySource `none` = portable pero sin passwords reversibles; `keyring` = seguro pero host-específico; `external` = portable con passphrase.
- **Bloque 13.2.4**: master.jceks no accesible → BPassword reversibles empty silencioso. Debugging opaco.
- **Bloque 10.3.3**: online backup excluye `.hdb`/`.adb` — para integridad completa necesitás offline backup.
- **Bloque 5.2.7**: LoadOp corre en engine thread — heavy deserialización bloquea callbacks.

### Security / Auth

- **Bloque 3.3.4**: ACCESS_CLASS, REFLECTION, MBEAN_PERMISSION requieren firma obligatoria aun en mode LOW.
- **Bloque 3.2**: 3 archivos de `bin/policy/` firmados PKCS7 — modificar rompe integridad.
- **Bloque 10.1.2**: platform credentials (5011) ≠ station BUser (FOX). Dos cuentas separadas.
- **Bloque 11.3.4**: sin límite concurrent sessions nativo. Failed login lockout per-user, NO per-IP.
- **Bloque 11.3.5**: password complexity NO enforcement nativo. LDAP/AD hereda del directorio.
- **Bloque 13.2.3**: FOX session token expiry 24h — re-auth silent cert-based.

### Scale y performance

- **Bloque 5.3.5**: Hierarchy service es caro (NEQL per nivel × N). Cachear.
- **Bloque 5.3.5**: NEQL NO aggregate functions. Para stats combinar con BQL.
- **Bloque 8.2.8**: HistorySpaceConnection AutoCloseable — try-with-resources obligatorio o DB locked.
- **Bloque 13.1.7**: Supervisor bottleneck ~50 subordinados. FOX broker saturation + history import contention.
- **Bloque 13.2.5**: FOX channel exhaustion ~1000 per session. Leaks bloquean.

### Licensing

- **Bloque 2.1**: SMA es atributo de feature, no feature propia.
- **Bloque 2.6**: `Feature.getb("skipModuleValidation", false)` license-gated bypass.
- **Bloque 13.1.1**: Subscription grace silencioso si nCloud cae. Monitoring alarm `SubscriptionExpiresIn` crítica.

### Build y dev

- **Bloque 1.1**: profile matrix estricto — `rt` NO puede importar de `wb` nunca.
- **Bloque 12.1.4**: `module-include.xml` NO editable manual — regenerado cada build.
- **Bloque 12.2.7**: lexicon fallback silent si falta key — logging recomendado.

### UI

- **Bloque 9.1.5**: `axvelocity` license requerida para `.pxvm` files.
- **Bloque 9.2.3**: HxOps mismo orden en write/save/update/process obligatorio.
- **Bloque 9.3.5**: `sameSite=None` requiere `secure=true` (HTTPS) en browsers modernos.

### Runtime semantics

- **Bloque 4.2.4**: BComponent NO linkable directamente como valor — usar BOrd (BSimple, linkable).
- **Bloque 4.2.1**: `equals()` default en BObject = identidad. Solo BValue compara por valor. Usar `equivalent()`.
- **Bloque 6.3.6**: priority levels 1 (emergency) y 8 (manual) **sí persisten en BOG** (excepción al flag TRANSIENT de los 16 inputs).
- **Bloque 8.3.7**: Schedule DST ambiguity 2:30am fall-back → Niagara usa hora estándar (winter offset).

---

## Conexiones clave entre bloques

Grafo de referencias cross-bloque (cuáles bloques se referencian entre sí):

```
Bloque 1 (Estructura) ─── fundamenta ───> 2, 3, 4, 10
Bloque 2 (Licensing) ─── base para ───> 13.1 (subscription)
Bloque 3 (Security JVM) ─── complementa ───> 11 (RBAC runtime), 13.1 (nCloud signing), 13.2 (keyring)
Bloque 4 (Baja) ─── substrate para ───> 5, 6, 7, 8, 9, 10, 12
Bloque 5 (ORD/BOG) ─── mecanismo para ───> 6 (links persistidos), 10.2.3 (filesystem semantics), 13.2 (BOG encryption)
Bloque 6 (Control) ─── consumido por ───> 7 (drivers ProxyExt), 8 (extensions)
Bloque 7 (Drivers) ─── conecta con ───> 6.2.6 (priority array mapping a BACnet)
Bloque 8 (Alarm/History/Schedule) ─── federado en ───> 13.1 (Supervisor/Subordinate)
Bloque 9 (UI) ─── consume ───> 11 (auth schemes), 13.3 (NiagaraRPC, UxMedia)
Bloque 10 (Platform) ─── contexto para ───> 11.3 (session lifecycle dos contextos)
Bloque 11 (RBAC) ─── ortogonal a ───> Bloque 3 (sandbox JVM)
Bloque 12 (Build) ─── genera ───> Bloques 4-9 artifacts
Bloque 13 (Gaps) ─── profundiza ───> 1.5 (Fox), 2 (subscription), 5.1 (schemes), 5.2 (BOG encryption), 9.3.6 (RPC), 9.1 (UxMedia)
```

---

## Engram topic keys (toda la memoria persistente)

Todas las observaciones guardadas en engram bajo `project: niagara-research`:

### Capa 1 (Bloques 1-3)
- `niagara/estructura/profiles-rt-ux-wb`, `niagara/estructura/registry-types`, `niagara/estructura/fox-protocol`
- `niagara/licensing/sma-attribute-model`, `niagara/licensing/honeywell-oem-overlay`, `niagara/licensing/license-manager-api`
- `niagara/security/cert-chain-pipeline`, `niagara/security/permission-groups-19-table`, `niagara/security/skip-module-validation-bypass`, `niagara/security/policy-files-triple-signed`

### Capa 2 (Bloques 4-6)
- `niagara/baja/slot-system`, `niagara/baja/type-hierarchy`, `niagara/baja/lifecycle-facets-dynamic`
- `niagara/navigation/ord-system`, `niagara/persistence/bog-format`, `niagara/queries/bql-neql-hierarchy-tags`
- `niagara/execution/engine-thread-model`, `niagara/execution/link-model-binding`, `niagara/control/kitcontrol-blocks`

### Capa 3 (Bloques 7-9)
- `niagara/drivers/framework-generico`, `niagara/drivers/bacnet-detalle`, `niagara/drivers/otros-modbus-lon-obix-snmp`
- `niagara/subsystems/alarm-pipeline`, `niagara/subsystems/history-service`, `niagara/subsystems/schedule-subsystem`
- `niagara/ui/workbench-px-gx`, `niagara/ui/bajascript-ux-hx`, `niagara/ui/servlets-jetty-webservices`

### Capa 4 (Bloques 10-12)
- `niagara/platform/daemon-niagarad`, `niagara/platform/station-boot-filesystem`, `niagara/platform/backup-dist-disaster-recovery`
- `niagara/auth/user-role-category-permission`, `niagara/auth/authentication-schemes`, `niagara/auth/session-autologoff-enterprise`
- `niagara/build/gradle-plugin`, `niagara/build/ax-n4-migration-lexicons`, `niagara/build/testing-palettes-help-editors`

### Capa 5 (Bloque 13)
- `niagara/advanced/subscription-niagaranetwork`, `niagara/advanced/fox-wire-crypto-virtual`, `niagara/advanced/rpc-reports-search-uxmedia-nav`

**Total**: 36 topic keys en engram + 13 archivos markdown consolidados en GitHub.

---

## Qué NO cubre este mental model

Honestidad sobre límites:

1. **Drivers vendor-específicos avanzados** (ej. BACnet FFT Honeywell custom extensions): cubierto el framework pero no cada variante OEM propietaria.
2. **Implementaciones internas de clases no decompilables**: algunas bytecode-only (CertificateChainValidator interno, NiagaraSocketPermission.implies) quedaron con reglas inferidas del caller, no código fuente.
3. **ZKM obfuscated classes**: `com.tridium.sys.Nre` (1,495 líneas) y 37 callsites `checkFeature` quedaron parcialmente opaco.
4. **Niagara Analytics Framework completo**: mencionado en 9.3.7 REST. El rules engine + analytics addon necesita sesión dedicada.
5. **Enterprise Security addon específicos por vendor**: cubierta estructura modular (11.3.7), no cada addon Honeywell.
6. **Upgrade paths específicos** (N4.X → N4.Y detalle): cubierto framework general (12.2), no playbooks per-version.

Para cualquiera de estos, el mental model actual es suficiente para orientarse y atacar con investigación puntual.

---

## Próximos pasos recomendados

Con los 13 bloques cerrados, tenés **~85-90%** del framework entendido conceptualmente. Lo que queda es:

1. **Práctica**: implementar un módulo end-to-end (driver simple + control logic + UI) usando el conocimiento.
2. **Debugging real**: cuando surja un problema de producción, usar el mental model para localizar el bloque relevante.
3. **Updates puntuales**: cuando Tridium/Honeywell release N4.15+ o features nuevas, actualizar bloques específicos en vez de re-investigar.
4. **Contribución inversa**: si identificás gotchas nuevos en uso real, agregarlos a los bloques correspondientes vía commit directo.

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
├── niagara-mental-model.2026-04-19.md (snapshot sesión httpapi)
├── NEXT_SESSION_PROMPT.md            (plantilla original)
├── NEXT_SESSION_PROMPT_MODULE_NAVIGATOR.md
├── notes/                            (borradores source)
│   ├── 01-estructura.md
│   ├── 02-licensing.md
│   └── 03-security.md
└── .atl/                             (SDD registry)
```

---

**Sesión cerrada**: 2026-04-22 — Mental model Niagara N4 consolidado end-to-end.

Si este mental model te ahorró horas de investigación o te evitó un bug de producción, el objetivo está cumplido.
