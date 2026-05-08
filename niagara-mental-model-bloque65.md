# Bloque 65 — Cierre Reflow + síntesis backlog MX60 final: 89 antipatterns + 38 reglas template + 200 implications + 7 decisiones arquitectónicas + stack MX60 + roadmap 10-16 sprints

**Fecha**: 2026-05-08
**Método**: Síntesis consolidada de los **15 bloques de audit Reflow** (50, 51, 53-64) en un single document accionable para arrancar greenfield rewrite MX60 sobre Niagara N4.14. Este bloque NO agrega audit nuevo — consolida todo lo aprendido en backlog ejecutable + decisiones arquitectónicas + roadmap de implementación.

**Fuentes**: 15 bloques previos `niagara-mental-model-bloque{50,51,53,54,55,56,57,58,59,60,61,62,63,64}.md` + INDEX.md.

**Versión analizada**: Reflow producción 1.7.5 (Jul 2024) + Reflow-Clean-177 (réplica clean-room).

---

## 65.0 Contexto: por qué este bloque cierra Reflow

### ¿Qué ES este bloque?

Bloque de **síntesis ejecutiva** después de 15 bloques de research denso. El research técnico está completo:
- Backend Java cubierto al 100% (HTTP REST + WebSocket + sync engine + dominios + helpers + servicios + ORD scheme + utils)
- Frontend Vue cubierto al ~95% (entry + router + Vuex 29 módulos + 17 mixins + 13 plugins + 10 lib + 12 views + 378 components mapeados)
- Stack tecnológico catalogado (Bloque 61) + reemplazos modernos
- Alarmas dedicado (Bloque 62)
- 89 antipatterns AP-1..89 numerados con severity
- 38 reglas template MX60 obligatorias
- 200 MX60 implications taggeadas KEEP/IMPROVE/SKIP/NEW
- 7 decisiones arquitectónicas claves identificadas

**Este bloque consolida todo en un single backlog ejecutable**.

### Por qué arrancar MX60 sin esta síntesis es error

Sin Bloque 65 el yo-2027 (o el siguiente dev) tiene que leer 15 archivos de research denso (~10K líneas total) para sacar la lista de prioridades. Bloque 65 es el **artefacto operacional** que permite arrancar greenfield el lunes que viene con un plan claro.

---

## 65.1 Síntesis ejecutiva — 7 decisiones arquitectónicas MX60

| # | Decisión | Razón | Bloque origen |
|---|----------|-------|---------------|
| 1 | **Regla 11 cx propagation END-TO-END (sync paths)** | AP-27 sistémico ~66 sites RBAC bypass latente | 53, 54, 55 |
| 2 | **Regla 12 filesystem whitelist + canonicalize + RBAC + audit + rate limit** | AP-33 file disclosure + AP-60 favorites traversal + AP-61 backup apply asymmetry | 57, 60 |
| 3 | **Regla 13 async commands BJobService o BoundedThreadPoolExecutor + cx propagation** | AP-42 thread DoS + AP-49 async cx bypass + AP-70 zero BJobService usage | 59, 60, 61 |
| 4 | **Regla 14 WebSocket upgrade Origin/Referer validation + connection limits per IP** | AP-43 CSWSH critical + AP-44 connection limits | 59 |
| 5 | **Alarmas WebSocket push obligatorio MX60** (vs polling 20s) | AP-73 polling latency + paged oncall | 62 |
| 6 | **Frontend stack Pinia 1:1 con 29 módulos Vuex + Vue 3 + TypeScript strict + Vuetify 3** | Vue 2.7 EOL + 4 deprecated libs + iView EOL 2020 | 61, 63 |
| 7 | **BComponent service container pattern (BReflowService blueprint)** | Niagara idiomatic — Properties composition + reactive cascade + lifecycle | 64 |

---

## 65.2 Antipatterns tally final — 89 entries

### 65.2.1 Distribución por severity

| Severity | Count | Acción MX60 |
|----------|-------|-------------|
| **CRITICAL** | 3 | AP-27 (cx sync), AP-43 (CSWSH), AP-49 (cx async) — **deal-breakers** |
| **HIGH** | 9 | AP-10 (backups GET), AP-21 (BQL injection), AP-33 (file disclosure), AP-39 (server↔client desync), AP-42 (thread DoS), AP-60 (favorites traversal), AP-61 (backup apply asymmetry), AP-74 (alarm thread no timeout), AP-76 (RLS BAlarmRecord) — **prioridad P0** |
| **MEDIUM** | 25 | varios — **prioridad P1** |
| **LOW** | 52 | code smells + cosmetic + perf — **prioridad P2** |

### 65.2.2 Antipatterns por categoría

| Categoría | Count | Ejemplos |
|-----------|-------|----------|
| RBAC / cx propagation | 3 | AP-27, AP-49, AP-76 |
| CSRF / Origin / WebSocket | 4 | AP-10, AP-31, AP-43, AP-44 |
| Filesystem / path traversal | 3 | AP-33, AP-60, AP-61 |
| DoS / resource exhaustion | 5 | AP-42, AP-44, AP-59, AP-73, AP-74 |
| BQL / injection | 1 | AP-21 (mitigated UUID canonicalization) |
| Async / threading | 4 | AP-42, AP-49, AP-70, AP-74 |
| Memory leaks | 3 | AP-45, AP-54, AP-82 |
| Code smell / duplication | 8 | AP-35, AP-37, AP-46, AP-55, AP-56, AP-79, AP-87, AP-89 |
| Outdated / deprecated | 6 | AP-63, AP-64, AP-65, AP-66, AP-67, AP-68 |
| Operability / DX | 5 | AP-37, AP-41, AP-47, AP-51, AP-71 |
| UX / browser compat | 3 | AP-72, AP-77, AP-83 |
| Sync correctness | 3 | AP-39, AP-58, AP-86 |
| Cosmetic / typos | 3 | AP-38, AP-88 |

### 65.2.3 Tabla maestra AP-1..89

[Para no inflar este bloque, ver INDEX.md cada bloque tiene su tabla. Resumen]:

**CRITICALs (3)**:
- AP-27: BOrd.make().get() sin cx en helpers (~66 sites cross-bloques 53-60)
- AP-43: WebSocket upgrade sin Origin/Referer validation = CSWSH
- AP-49: AsyncReflowCommand.run() spawn Thread sin cx propagation = RBAC bypass async

**HIGH (9)**:
- AP-10: backup GET destructivos (mitigado parcialmente Bloque 58 harden-backup-csrf, AP-39 desync producción)
- AP-21: BQL injection (mitigated UUID canonicalization)
- AP-33: filesystem disclosure findFile sin RBAC
- AP-39: cliente 1.7.5 vs server clean-177 desync producción
- AP-42: uncontrolled thread spawn AsyncReflowCommand
- AP-60: favorites filename traversal username concat
- AP-61: BackupManager.apply() sin sanitización vs create() asymmetry
- AP-74: AlarmData.query() Thread.join() sin timeout
- AP-76: missing RLS BAlarmRecord access multi-tenant disclosure

---

## 65.3 Reglas template MX60 — 38 reglas obligatorias

### 65.3.1 Reglas Java backend (1-19)

| # | Regla | Origen |
|---|-------|--------|
| 1 | Type-safe Property getters/setters | 53 template |
| 2 | UUID canonicalization before BQL | 54, 62 |
| 3 | BQL parameterized queries | 53 |
| 4 | MAX_QUERY_LIMIT enforcement | 53 |
| 5 | Defensive AlarmRecord copy (newCopy) | 62 |
| 6 | LinkedHashMap pagination consistency | 62 |
| 7 | Robust exception fallbacks (parsing) | 62 |
| 8 | BajaScript ack delegation (no reimplementar Niagara state mutations) | 62 |
| 9 | Privileged elevation only for non-RBAC ops | 53 |
| 10 | Response simple structure | 53 |
| 11 | **cx propagation END-TO-END (sync paths)** | 53, 54, 55 — **CRÍTICO** |
| 12 | **Filesystem whitelist + canonicalize + RBAC + audit + rate limit** | 57, 60 — **CRÍTICO** |
| 13 | **Async commands BJobService + cx propagation** | 59, 60 — **CRÍTICO** |
| 14 | **WebSocket Origin + connection limits per IP** | 59 — **CRÍTICO** |
| 15 | Error envelope estandarizado WebSocket | 59 |
| 16 | Rate limiting WebSocket per socket/IP | 59 |
| 17 | Server heartbeat + per-socket metrics | 59 |
| 18 | Thread spawn capture cx pre-spawn + propagate | 60 |
| 19 | Filename sanitizer factory + canonicalize | 60 |

### 65.3.2 Reglas alarmas (20-22)

| # | Regla | Origen |
|---|-------|--------|
| 20 | Alarm priority mapping validation | 62 |
| 21 | BQL safety + thread timeout + MAX_QUERY_LIMIT alarms | 62 |
| 22 | Ack intent capture + sound playback try-catch | 62 |

### 65.3.3 Reglas frontend Vue/Pinia (23-31)

| # | Regla | Origen |
|---|-------|--------|
| 23 | Vuex/Pinia namespaced modules ONLY | 63 |
| 24 | Vue.set / Vue 3 Proxy reactive | 63 |
| 25 | Plugin boot order CRÍTICO baja → niagara | 63 |
| 26 | CSRF pre-issue meta tag + refresh on 403 | 63 |
| 27 | Lazy-load all routes | 63 |
| 28 | eventBus solo view-layer events | 63 |
| 29 | Subscriber cleanup obligatorio composable useSubscriber | 63 |
| 30 | mapState/mapGetters preferred | 63 |
| 31 | JSON Patch FULL RFC 6902 (no SUBSET) | 63 |

### 65.3.4 Reglas backend services (32-38)

| # | Regla | Origen |
|---|-------|--------|
| 32 | Custom Jackson serializer factory naming centralize | 64 |
| 33 | Service container Property injection (no raw fields) | 64 |
| 34 | Date range enum + calculator dispatch | 64 |
| 35 | Reactive Property change cascade | 64 |
| 36 | ORD scheme custom resolver | 64 |
| 37 | Privilege escalation AccessController + Thread.join (combinar Regla 13) | 64 |
| 38 | Utility helper static factories stateless | 64 |

---

## 65.4 MX60 implications — 200 entries clasificadas

### 65.4.1 Por tag

| Tag | Count |
|-----|-------|
| **KEEP** literal | 76 |
| **IMPROVE** | 89 |
| **SKIP** | 8 |
| **NEW** | 27 |

### 65.4.2 Top 20 implications priorizadas P0/P1

**P0 — deal breakers (deben estar día 1)**:

| # | Tag | Descripción | Bloque |
|---|-----|-------------|--------|
| Reglas 11+13+18 | NEW | cx propagation sync + async + Thread spawn — pre-commit hook obligatorio | 53, 59, 60 |
| 92 | NEW | CSRF Origin validation WebSocket + connection limits | 59 |
| 102 | KEEP | BasicContext wrapping con facets (remoteHost, remotePort) | 59 |
| 110 | IMPROVE | AsyncReflowCommand → BoundedThreadPoolExecutor / BJobService | 59 |
| 127 | NEW | Filename sanitizer factory + canonicalize todo user-derived path | 60 |
| 142 | IMPROVE | Vue 2 → Vue 3.5+ migration (378 components) | 61, 63 |
| 158 | NEW | BJobService obligatorio — 0 sites Reflow es ROOT CAUSE async APs | 61 |
| 161 | NEW | Alarms WebSocket push obligatorio MX60 (vs polling 20s) | 62 |
| 175 | KEEP | UUID canonicalization before BQL universal anti-injection | 62 |
| 176 | KEEP | Pinia stores 1:1 con 29 módulos Vuex actuales | 63 |
| 180 | IMPROVE | Subscriber lifecycle ESLint rule enforcement | 63 |

**P1 — core architecture**:

| # | Tag | Descripción | Bloque |
|---|-----|-------------|--------|
| 130 | IMPROVE | HistoryDataResponse cache singleton BHistoryDatabase | 60 |
| 131 | IMPROVE | ConfigResponse `?file=` whitelist | 60 |
| 132 | IMPROVE | BackupManager.apply() sanitización symmetric con create() | 60 |
| 133 | NEW | HistoryDataResponse MAX_LIMIT 10000 server-side | 60 |
| 159 | NEW | Stack frontend = Vue 3 + Pinia + Vuetify 3 + Vite 5 + TS strict + Vitest + Playwright + pnpm | 61 |
| 160 | NEW | Stack backend = N4.14 + Java 11+ + Gradle 8.5+ + Jackson 2.17 + JUnit 5 + Spotless+SpotBugs+Checkstyle+ErrorProne + GH Actions | 61 |
| 177 | NEW | Plugin boot order documentar CLAUDE.md MX60 (baja → niagara) | 63 |
| 179 | IMPROVE | JSON Patch full RFC 6902 (lib rfc6902 / fast-json-patch) — NO subset | 63 |
| 193 | NEW | Analytics-lib Jackson serializer factory pattern (Bloque 66 prep) | 64 |

---

## 65.5 Stack MX60 final recomendado

### 65.5.1 Frontend

```yaml
framework: Vue 3.5+
language: TypeScript strict (day-1)
state: Pinia 2.2+ (1:1 con 29 módulos Vuex actuales)
routing: Vue Router 4.x (lazy-load all routes)
ui_kit: Vuetify 3.5+ (vs iView deprecated)
charts: D3 7.9 KEEP o recharts 2.10+
http: axios 1.7+ (upgrade desde 0.21.4)
websocket: WebSocket nativo (REMOVER socket.io-client vestigial 8KB)
dates: dayjs 1.11.13 KEEP
colors: colord 2.9+ (vs tinycolor2)
clipboard: @vueuse/core useClipboard
cookies: js-cookie 3.x
drag: @vueuse/core useDraggable + SortableJS
masonry: CSS Grid nativo (NO vue-masonry)
resize: @vueuse/core useResizeObserver
build: Vite 5.4+ (KEEP)
testing_unit: Vitest 1.0+
testing_e2e: Playwright 1.40+
testing_component: @vue/test-utils 2.4+
linting: ESLint 8.x + TypeScript-ESLint 6.x + Prettier
package_manager: pnpm 8+
polyfills: NONE (ES2020+ targets)
```

### 65.5.2 Backend

```yaml
runtime: Niagara N4.14 (constraint)
java: 11+ (upgrade desde 8 — sealed classes, records, var, text blocks)
api_subset: BComponent + BOrd + BService + BHistory + BAlarm + BFileSystem + BUser
async: BJobService o BoundedThreadPoolExecutor (Regla 13) — NO new Thread() raw
context: cx propagation explícita (Reglas 11+13+18)
filesystem: PathValidator + canonicalize + RBAC + audit (Reglas 12+19)
csrf: CsrfGuard pattern KEEP literal + Origin validation WebSocket (Regla 14)
reflection: VarHandle / MethodHandles.Lookup (Java 11+) donde aplique
json: Jackson 2.17+ + custom serializer factory pattern (Regla 32)
json_patch: flipkart-zjsonpatch 0.4.14+ (audit CVEs primero) o RFC 7396 alternativo
build: Gradle 8.5+ + tridium plugins + niagara-signing habilitado
quality: Spotless 6.x + SpotBugs 4.8+ + Checkstyle 10.x + ErrorProne 2.24+
testing: JUnit 5 + Mockito 5.x + AssertJ 3.24+ + Niagara station test harness
logging: SLF4J + Logback (Niagara native)
ci_cd: GitHub Actions + pre-commit hooks anti-AP (grep `new Thread\(`, `BOrd.make\(.*\)\.get\(\)` sin cx, filesystem concat user)
```

### 65.5.3 APIs externas

```yaml
weather: proxy backend MX60 con HTTPS + API keys env vars + cache 24h + key incluye config hash
external_apis: allowlist explícita default deny + security review nuevo API
```

---

## 65.6 Pre-commit hooks anti-AP obligatorios MX60

### 65.6.1 Backend Java

```bash
# Anti-AP-27 / AP-49 cx propagation
grep -rn 'BOrd\.make([^)]*)\.get(\s*null' src/ && echo "AP-27 FAIL" && exit 1
grep -rn 'new Thread([^,]*)\.start()' src/ | grep -v 'BJobService' && echo "AP-42 FAIL" && exit 1

# Anti-AP-33 / AP-60 / AP-61 filesystem traversal
grep -rn 'BFileSystem\.INSTANCE.*findFile.*\+' src/ && echo "AP-33 cousin FAIL" && exit 1
grep -rn '"\^reflow/[a-z]*/" + [a-zA-Z]* + ".json"' src/ && echo "AP-60 FAIL" && exit 1

# Anti-AP-43 CSWSH
grep -rn 'WebSocketServletFactory' src/ | grep -v 'Origin' && echo "AP-43 FAIL" && exit 1

# Anti-AP-67 AccessController.doPrivileged sin justificación
grep -rn 'AccessController\.doPrivileged' src/ | grep -v '/\* doPriv: ' && echo "AP-67 NEEDS JUSTIFICATION" && exit 1
```

### 65.6.2 Frontend Vue 3 + TypeScript

```bash
# Anti-AP-82 subscriber leak — ESLint rule
# Cualquier llamada .subscribe() debe tener .unsubscribe() en mismo composable
# (custom ESLint rule TBD)

# Anti-AP-86 JSON Patch SUBSET — usar lib full RFC 6902
grep -rn 'STATE_DELTA' src/ | grep -v 'rfc6902\|fast-json-patch' && echo "AP-86 FAIL" && exit 1

# Anti-AP-72 sound playback sin try-catch
grep -rn 'invokeSoundOrd' src/components/alarms/ | grep -v 'try' && echo "AP-72 FAIL" && exit 1
```

---

## 65.7 Roadmap MX60 — 10-16 sprint estimado

### 65.7.1 Frontend epics (5-7 sprints)

| Epic | Effort | Dependencies |
|------|--------|--------------|
| Vue 3 + TypeScript migration (378 components) | 2-3 sprints | — |
| Vuex → Pinia rewrite (29 stores 1:1) | 1 sprint | Vue 3 done |
| iView → Vuetify 3 migration (150+ components) | 1-2 sprints | Vue 3 done |
| 7 utility libs upgrade (axios 1.7+, @vueuse, js-cookie, colord, CSS Grid masonry, vuedraggable 4.x, etc.) | 1 sprint | — |
| Subscriber lifecycle ESLint rule + composable refactor | 0.5 sprint | Vue 3 done |
| WebSocket push for alarms (vs polling) | 0.5-1 sprint | Backend Regla 13/15/16/17 done |

### 65.7.2 Backend epics (5.5-9 sprints)

| Epic | Effort | Dependencies |
|------|--------|--------------|
| Java 8 → Java 11+ upgrade (sealed classes, records, var) | 1-2 sprints | — |
| AP-27 refactor 66+ sites cx propagation (Regla 11) | 2-3 sprints | Java 11 done |
| AP-33+60+61 filesystem refactor (Regla 12+19 PathValidator) | 1 sprint | — |
| AP-43+44+49 WebSocket hardening (Reglas 13+14+15+16+17) | 1-2 sprints | — |
| BWebServlet wrapper o custom guards explícitos | 0.5-1 sprint | — |
| BJobService thread pool migration (9 raw threads) | 0.5 sprint | Regla 13 done |

### 65.7.3 Cross-cutting (1-2 sprints)

| Epic | Effort |
|------|--------|
| CI/CD GitHub Actions + pre-commit hooks anti-AP | 0.5 sprint |
| Test harness obligatorio (JUnit 5 + Vitest + Playwright + station test) | 0.5 sprint |
| Migration scripts config v0→v15 (extender configMigration.js Bloque 63) | 0.5-1 sprint |
| Documentation (CLAUDE.md MX60 + plugin interface contracts + Architecture RFCs) | continuous |

### 65.7.4 Total estimate

**TOTAL: 10-16 sprint** (= **5-8 meses con 1-2 devs full-time** o **3-5 meses con 3-4 devs**).

---

## 65.7 Métricas finales del research

| Métrica | Valor |
|---------|-------|
| Bloques de research Reflow | 15 (50, 51, 53-64) |
| Líneas auditoría | ~10K líneas markdown total |
| Antipatterns identificados | 89 (AP-1..89) |
| Reglas template MX60 | 38 |
| MX60 implications | 200 |
| Decisiones arquitectónicas clave | 7 |
| Líneas Java auditadas | ~7K LOC |
| Líneas Vue auditadas | ~95% de 459 archivos |
| Vuex módulos catalogados | 29 (14 persistent + 14 transient + 1 root) |
| REST endpoints catalogados | 28 |
| WebSocket commands catalogados | 8 builtin + 4 custom |
| Sound assets | 11 archivos MP3 (~448KB) |
| Bundle reduction potential | ~158KB (8KB socket.io + 150KB iView CSS) |

---

## 65.8 Riesgos y open questions para MX60

### 65.8.1 Riesgos críticos

1. **Migration de configuration**: Reflow tiene `configMigration.js v0→v14` (600L+). MX60 debe poder leer config v14 producción → v15+ MX60 schema.
2. **N4.14 → N4.x future versions**: si Niagara upgrade a Java 17+, AccessController.doPrivileged removal forzará refactor anticipado.
3. **Multi-station consistency**: Reflow asume single-station. MX60 multi-supervisor scenarios?
4. **Bundle producción 1.7.5 redeployment**: AP-39 server↔client desync — MX60 debe asegurar atomicity bundle + server.

### 65.8.2 Open questions

1. **AP-87 RangeCalculator semantics**: "Compare Last Year" -2 offset es bug o intencional? Documentar business intent o fixear.
2. **AP-76 RLS BAlarmRecord**: multi-tenant model intencional o bug? Decisión arquitectónica explícita.
3. **Sound library extensibility**: upload custom sounds runtime o assets pre-bundled?
4. **WebSocket vs Server-Sent Events (SSE)**: para alarms push, ¿WS más complejo o SSE suficiente con simplicidad?
5. **Vue 3 + Composition API vs Options API**: ambos son válidos en Vue 3. Recomendación: Composition para greenfield (better TypeScript inference).
6. **Niagara N4.14 → N4.15+ roadmap**: ¿hay constraints que lleguen pronto?
7. **License model**: BReflowService propiedades licensing/limits. ¿MX60 hereda o reescribe?

---

## 65.9 Lista de bloques completos — referencia rápida

| Bloque | Tema | Estado |
|--------|------|--------|
| 50 | Reflow-Clean-177 audit Par A `frontend↔ux` | ✅ |
| 51 | Reflow-Clean-177 audit Par A real `frontend↔rt` + app-readable.js | ✅ |
| 52 | CSRF cliente cross-frame Plan E | ✅ |
| 53 | app-readable.js bridge SPA-Niagara + injectBaja + Vue mixin Tt | ✅ |
| 54 | Alarm domain client↔server arquitectónico | ✅ |
| 55 | History domain + AP-27 sistémico + fetchMethod switch | ✅ |
| 56 | Points domain (sin Java backend = decisión arquitectónica positiva) | ✅ |
| 57 | Schedule + Nav + File + CSV domains + AP-33 file disclosure HIGH | ✅ |
| 58 | HTTP infrastructure (BaseServlet 367L + 34 Response classes + CsrfGuard + sync) | ✅ |
| 59 | WebSocket layer trinity 3/3 (BReflowChannelService + Acceptor + Vue components) | ✅ |
| 60 | Sync engine deep dive + Response outliers + BackupManager + AP-60+61 HIGH | ✅ |
| 61 | Catálogo librerías + APIs + stack MX60 recomendado | ✅ |
| 62 | Alarmas dedicado (backend + frontend + integración Niagara N4 Alarm Console) | ✅ |
| 63 | Frontend Reflow Vue 2.7 audit completo (Vuex 29 módulos + 17 mixins + 13 plugins + 10 lib) | ✅ |
| 64 | `-ux` modules + `-rt` remanentes (BReflowService blueprint + utils + history/) | ✅ |
| **65** | **Cierre Reflow + síntesis backlog MX60 final (THIS)** | ✅ |

---

## 65.10 Capa 17 cerrada — qué sigue

**Capa 17 (Reflow audit cross-stack)** = **100% COMPLETA**.

**Próximo bloque**: **Bloque 66+ — Pivote Analytics module Niagara N4** (`/home/cristian/modules/Prototipos/modulos/organized/analytics` + `analytics-lib`).

**Profundidad esperada Analytics**: mismo nivel que Reflow — estructura, funciones, APIs, cross-references, integración BHistoryService, patterns KEEP/IMPROVE/SKIP/NEW para potencial integración con MX60.

---

## 65.11 Decisiones arquitectónicas MX60 — checklist final

Usar este checklist el lunes que arranca MX60 greenfield:

- [ ] Crear repo Git con structure: `modules/mx60-rt/` + `modules/mx60-ux/` + `frontend/`
- [ ] Arrancar `gradle init` + `pnpm init` con stack Bloque 65.5
- [ ] Crear `CLAUDE.md` con plugin boot order baja → niagara documentado
- [ ] Crear `.github/workflows/ci.yml` con pre-commit hooks Bloque 65.6
- [ ] Crear primer `BMx60Service extends BComponent implements BIService` blueprint con Properties composition
- [ ] Crear primer Pinia store con namespace + TypeScript strict
- [ ] Test harness inicial (JUnit 5 + Vitest hello world)
- [ ] Documentar Reglas 11-19 + 23-31 en CLAUDE.md como mandatory
- [ ] Plan de migration config v14 → v15 MX60 (extender `configMigration.js`)
- [ ] Decision arquitectónica RLS BAlarmRecord (Implication #172)
- [ ] Decision arquitectónica WebSocket vs SSE para alarms push (Bloque 65.8.2)
- [ ] Decision arquitectónica CSWSH whitelist origins (Regla 14)

---

**End of Bloque 65** — cierre Reflow + síntesis backlog MX60 final.

**Siguiente**: Bloque 66+ — Pivote Analytics module Niagara N4 + analytics-lib.
