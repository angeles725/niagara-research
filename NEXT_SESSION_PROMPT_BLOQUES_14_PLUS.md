# Prompt — Continuación Niagara N4 Mental Model (Bloques 14+)

Copiá TODO el contenido entre los `---` y pegalo como primer mensaje en la nueva sesión de Claude Code.

**Abrir sesión en**: `/home/cristian/niagara-research/`

---

```
Hola. Vengo de una sesión anterior (2026-04-22) donde cerramos un mental model
Niagara N4 en 13 bloques consolidados + INDEX maestro. Todo pushado a GitHub
privado: https://github.com/angeles725/niagara-research

Repo actual tiene:
- INDEX.md (índice maestro, rutas de lectura, gotchas transversales).
- niagara-mental-model.md (Bloques 1-3 Estructura/Licensing/Security).
- niagara-mental-model-bloque{4..13}.md (Baja/ORD+BOG+Queries/Control/Drivers/
  Alarm+History+Schedule/UI/Platform/Auth/Build/Gaps-profundos).
- 36 engram topic keys bajo project="niagara-research".

Objetivo de esta sesión: cerrar los gaps restantes con bloques 14-20. El usuario
quiere **cobertura 100% real** del framework — todo lo que falte por entender,
independiente del volumen. Autonomía total: vos auto-guiás, creás bloques
nuevos si hacen falta, te das retroalimentación, commitás + pushás a GitHub
después de cada bloque.

## Primero — hidratar contexto

Ejecutá ANTES de responderme:

1. `mem_context` con project="niagara-research" — trae session summary final del
   2026-04-22 con goal/discoveries/accomplished/next-steps.
2. `mem_search` con query="niagara mental model" project="niagara-research"
   — confirma que hay 36 observations bajo topic keys niagara/*.
3. Leé `/home/cristian/niagara-research/INDEX.md` entero — mapa completo de
   lo hecho + gotchas transversales + conexiones cross-bloque.
4. Leé también `/home/cristian/niagara-research/niagara-mental-model-bloque10.md`
   sección 10.2 (filesystem !config/!sys/!fox/!file) porque varios bloques
   nuevos profundizan ahí.

## Scope — Bloques 14-20 (nuevos)

Los 13 bloques previos cubren ~85-90% del framework. Estos 7 bloques nuevos
atacan los gaps empíricos que el usuario identificó:

### BLOQUE 14 — Point counting + License limits + Templates framework + Batch Editor + EasyTemplates

Gap crítico operacional. Usuario necesita entender **cuándo se cuenta un punto
para licensing** y cómo se manejan templates.

- **Point counting reglas**: cuándo un punto cuenta (virtual, linked, proxy,
  exportado, histórico), cuándo NO. `BIPointCountable` interface. Dónde se
  totaliza (LicenseManager). Diferencia "points" vs "devices" vs "devices+points".
- **License limits**: `point.limit`, `device.limit`, `history.limit`, `station.limit`
  features. Cómo se aplican (hard vs soft cap). Workflow cuando se excede.
- **Niagara Templates Framework**:
  - BComponentTemplate, BTemplateService.
  - Template = snapshot parametrizable de subtree.
  - Instance = clonar template + ajustar params.
  - Template binding: vincular slots del template a data fuentes.
- **EasyTemplates** (Honeywell-specific, C:\Users\equipo\Niagara4.14\OptimizerSupervisor\EasyTemplates):
  - Qué son, diferencia con Niagara Templates core.
  - Formato de archivo.
  - Cómo se usan en Workbench.
- **Batch Editor** (workbench tool):
  - Qué hace: editar N puntos a la vez.
  - Workflow: seleccionar → batch edit → aplicar.
  - Limitaciones.
- **Template / Match / Bind workflow** (LON-specific pero patrón generalizable):
  - Template define estructura esperada.
  - Match detecta devices que cumplen template.
  - Bind conecta templates a devices reales.

Fuentes: devguide-clean, decompilado modulo templates-rt, batchEditor-wb si existe,
docs/ directory si hay "Template" o "Batch" mentions.

### BLOQUE 15 — Workbench editing deep (wiresheet + property sheet + nav tree + point editing)

El Bloque 9.1 cubrió Workbench a alto nivel. Este profundiza **el workflow diario**:

- **Wiresheet editor**:
  - Cómo se renderiza un BComponent en wiresheet.
  - Drop de palette: qué pasa step-by-step.
  - Link dragging: UI + persistence en BOG.
  - Layout persistente (BWsAnnotation posición x,y de cada component).
  - Grupos, selección múltiple.
- **Property Sheet**:
  - Cómo se renderiza cada slot.
  - Facets controlan rendering (del Bloque 4.3.2).
  - Edit flow: cambio → validation → commit a BOG.
- **Nav tree (navigator)**:
  - BINavNode plugins.
  - Scope de un nav tree (station, module, file).
  - Drag from nav to wiresheet.
- **Point Manager / Device Manager views**:
  - Tabla editable de puntos.
  - Batch operations (subset del Bloque 14).
- **Workflow típico**: crear station → agregar driver → discover points → drag a
  folder → configurar proxy ext → linkear a control logic → deploy.

### BLOQUE 16 — Niagara Analytics Framework + Provisioning Service

Mencionado en 9.3.7 y 13.3.6 pero NO profundizado:

- **`BAnalyticsService`**: arquitectura, sub-componentes.
- **Rules engine**: definir reglas sobre data streams, trigger de events.
- **Analytics Web API** (completamente, Bloque 9.3.7 solo mencionó endpoints).
  - Endpoints: Query, GetValue, GetNode, Subscribe, PollSubscription, Invoke,
    GetRollup.
  - Formato JSON de requests/responses.
  - Subscription model.
- **Provisioning service**:
  - Cómo un Supervisor aprovisiona múltiples subordinados en bulk.
  - Batch jobs.
  - Integration con Station Copier.
- **Niagara Analytics vs Skyspark / external**: si existe integración.

Fuentes: devguide si hay analytics, decompilado niagaraAnalytics-rt, docs/ Honeywell.

### BLOQUE 17 — Filesystem layout COMPLETO + Native binaries + JRE embedded

El Bloque 10.2.3 cubrió filesystem semantics pero NO los paths reales completos
ni los binarios nativos. Este bloque es filesystem forensics puro.

#### 17.1 Install Home — `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\` (en WSL: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/`)

Mapear cada subdirectorio:
- `bin/` — executables + native libs. Lista TODAS las .dll/.so/.exe y documentá
  qué hace cada una:
  - `niagarad.exe` (ya en Bloque 10.1).
  - `nre.exe`, `console.exe`, `hdbt.exe`, `n4mig.exe`.
  - `traylcon.dll`, `honImport.dll`, `libciper.so`, `libciper.so.sig`,
    `vcruntime140.dll`, `msvcp140.dll`, `msvcr120.dll`, `common.dll`,
    `cppunit.dll`, `alarmDialog.dll`, `dsfspi.dll`, `lon.dll`, `njre.dll`,
    `nre.dll`.
  - `ext/` — qué JARs vive acá (Bouncy Castle FIPS, etc.).
  - `policy/` — los 3 archivos firmados del Bloque 3.2.
  - `install-data/` — datos de instalación.
- `lib/` — librerías shared.
- `knx/` — KNX data tables + cache.
- `jre/` — JDK embebido. Versión exacta, configuración, qué módulos incluye.
- `modules/` — los 926 JARs del corpus (ya analizados).
- `defaults/` — platform.bog + system.properties + otros defaults.
- `docs/` — docs PDF/HTML Honeywell (distinto de `niagara-help`).
- `etc/` — config files.
- `niagara-help/` — devguide + bajadoc + guides.
- `security/` — licenses + certs + keystores install-level.
- `sw/` — signers registry binary, otros.
- `spyderApps/`, `printout/`, `px/`, `Palettes_and_Misc/`.
- `conversion/`, `cleanDist/`.
- `JxBrowser/`, `module-navigator/`.

#### 17.2 User Home (Workbench) — `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\`

- `certManagement/` — certs del Workbench usuario.
- `EasyTemplates/` — templates Honeywell (Bloque 14 pero path acá).
- `gradle/wrapper/` — Gradle wrapper para builds.
- `help/` y `help/docDeveloper/` — docs extra usuario.
- `security/` — user keystore (workbench signing certs).
- `etc/`.

#### 17.3 Daemon Home — `C:\ProgramData\Niagara4.14\OptimizerSupervisor\`

Distinto del user home. Ejecutable por el daemon niagarad.
- `security/` — daemon keystore, master password keyring.
- `daemon/` — daemon logs, config (daemon.properties).
- `etc/`.
- `registry/` — caché del type registry del station.
- `stations/` — config.bog + histories + alarm DBs de stations.

#### 17.4 JRE / JDK embedded

- Versión (probable Azul Zulu o Oracle/OpenJDK).
- Módulos Java incluidos (FIPS-compliant crypto = BouncyCastle FIPS provider).
- JVM flags estándar (Xms/Xmx, GC tuning).
- Security provider config.

### BLOQUE 18 — Module signing standalone + module-permissions.xml + CSRF + Header Auth + requestingPermissions

Este bloque es **crítico para el usuario** porque viene de la saga httpapi donde
no pudieron firmar módulos custom contra cert Honeywell. Necesita entender
**cómo firmar módulos sin usar Workbench** (CLI / gradle standalone).

- **Module signing standalone**:
  - Gradle task `sign` / `signMods` uso directo (sin Workbench UI).
  - Generación de dev certificate self-signed (openssl / keytool commands).
  - Keystore `.jks` vs `.p12` / `.pfx`.
  - Config `niagaraSigning { certAlias = ...; keystore = ... }` en build.gradle.
  - `nverify.exe` (Bloque 3.9) para validar manualmente.
  - CLI standalone: ¿existe `nsign` o similar fuera de gradle?
  - **Caso específico**: firmar contra cert propio en instalación que tiene
    `signing.properties` hardcoded a Honeywell CA. Opciones:
      a) skipModuleValidation license-gated (Bloque 3.10).
      b) Self-signed con verificationMode=LOW.
      c) Re-firmar todos los archivos policy (rompe integridad).
      d) Usar `-Dniagara.classLoader.skipModuleValidation=true` system property.
- **`module-permissions.xml` deep**:
  - Formato completo (source vs runtime transformation, Bloque 3.3).
  - Los 19 permission groups (Bloque 3.4) — ejemplos concretos de cada uno
    con parámetros reales.
  - Cómo requestear permisos nuevos (workflow dev).
  - Validation contra module-permissions.xml en load.
- **CSRF protection**:
  - Mencionado en 9.3.6 como header `x-niagara-csrfToken`.
  - Cómo se genera el token.
  - Dónde se valida (servlet filter? annotation?).
  - Bypass en local (si aplica para desarrollo).
- **Header Authentication**:
  - `BHttpHeaderCallbackHandler` mencionado en 11.3.2.
  - Flow completo: qué headers espera, HELLO message protocol.
  - Cómo se usa en integraciones (ej. reverse proxy con pre-auth).
- **Requesting permissions** flow completo:
  - `javax.baja.security.permissions` annotations.
  - Runtime permission check vs static declaration.
  - SecurityManager.checkPermission() interception.

### BLOQUE 19 — LON deep + NRIO + NiagaraDriver (N driver) + BOX protocol

Bloque 7.3.3 cubrió LON básico. Este profundiza:

- **LON Template/Match/Bind workflow**:
  - LonMark profiles.
  - XIF files (LON device description).
  - learnNv discovery.
  - Bind Tool en Workbench.
  - NV subscriptions.
- **NRIO** (Niagara Remote I/O):
  - Qué es (probablemente módulo Honeywell para I/O remoto).
  - Hardware específico (controladores Honeywell con I/O expansion).
  - Cómo se configura, qué puntos expone.
- **NiagaraDriver (`niagaraDriver-rt`)**:
  - BAbstractNiagaraDevice: driver que conecta station-a-station.
  - Tunneling: Supervisor ↔ Subordinates sobre este driver.
  - Relación con NiagaraNetwork (Bloque 13.1.3).
- **BOX protocol** (BajaScript-over-HTTP):
  - Formato wire (similar a Fox pero HTTP-based).
  - Usado por BajaScript en browser (Bloque 9.2.1).
  - Mensajes BOX vs Fox frames.
  - Multiplexado sobre un HTTP connection.

### BLOQUE 20 — BApp / BAbstractApp + net module + app layer + misc remaining

- **BApp / BAbstractApp**: capa de aplicación sobre BComponent. Qué agrega.
- **`net` module**: capacidades de red low-level (sockets propios? IP helpers?).
- **Misc que quedó pendiente**:
  - BAbstractService internals.
  - Station Monitor / EngineMonitor (menciondo en Bloques 10/13 pero no deep).
  - JobService (en Bloque 10.2.2 mencionado en boot order, no profundizado).
  - Persistent policies (cómo subsystems configuran retention).

## Metodología (igual que sesiones previas)

1. Crear task list con todos los bloques (14-20) en `TaskCreate`.
2. Por cada bloque:
   - Lanzar **3 sub-agents Explore en PARALELO con `run_in_background=true`**.
   - Cada sub-agent cubre una tercera parte del bloque.
   - Cada sub-agent devuelve markdown listo para pegar.
   - NO guardes engram en sub-agents — devolver contenido íntegro.
3. Cuando los 3 sub-agents completen, sintetizar en archivo `niagara-mental-model-bloque{N}.md`.
4. `mem_save` con 3 topic keys nuevos bajo `niagara/<categoría>/<tema>`.
5. `git add + git commit + git push origin main`.
6. Auto-continuar con el siguiente bloque sin preguntar.

## Reglas duras

- **READ-ONLY total** — nunca modificar el install Honeywell ni el repo git
  más allá de commits.
- **Sub-agents con `run_in_background=true`** para no bloquear mi contexto.
- **Empírico sobre docs**: si doc oficial y código decompilado difieren, CÓDIGO
  gana. Anotar discrepancias.
- **Verificar paths** antes de afirmar que existen — muchos paths en Windows
  están en `/home/cristian/Honeywell/...` via WSL.
- **Ningún emoji**.
- **Español técnico neutro**, formal y directo.
- **Target por bloque**: 300-600 líneas densas. No relleno.

## Después de Bloque 20

Actualizar `INDEX.md`:
- Agregar capa "Filesystem forensics + Dev ops" para Bloques 17-18.
- Agregar capa "Módulos verticales específicos" para Bloques 19-20.
- Actualizar tabla de gotchas transversales con nuevos hallazgos.
- Actualizar grafo de conexiones.

Cierre con `mem_session_summary` que incluya:
- Goal: cerrar gaps restantes (14-20).
- Discoveries: hallazgos nuevos críticos.
- Accomplished: bloques 14-20 + INDEX refresh + commits.
- Next steps: práctica real, updates puntuales, sesión Analytics deep si aplica.
- Relevant files.

## Primer paso concreto

1. Hidratá contexto (los 4 pasos arriba).
2. Crear task list completa para Bloques 14-20.
3. Mostrame:
   a) Confirmación de que leíste INDEX.md y tenés context.
   b) Cualquier ajuste al scope de los 7 bloques (si ves gap adicional o
      overlap innecesario).
   c) Propuesta de orden: ¿ir 14→20 secuencial o hay dependencias que
      cambien el orden?
3. Cuando confirme, arrancás con Bloque 14 (3 sub-agents paralelos en
   background).

Arrancamos.
```

---

## Referencias engram principales a recuperar

Bajo `project="niagara-research"`:

- `niagara/estructura/*` (3 keys) — Bloque 1
- `niagara/licensing/*` (3 keys) — Bloque 2
- `niagara/security/*` (4 keys) — Bloque 3
- `niagara/baja/*` (3 keys) — Bloque 4
- `niagara/navigation/ord-system`, `niagara/persistence/bog-format`, `niagara/queries/bql-neql-hierarchy-tags` — Bloque 5
- `niagara/execution/*`, `niagara/control/*` (3 keys) — Bloque 6
- `niagara/drivers/*` (3 keys) — Bloque 7
- `niagara/subsystems/*` (3 keys) — Bloque 8
- `niagara/ui/*` (3 keys) — Bloque 9
- `niagara/platform/*` (3 keys) — Bloque 10
- `niagara/auth/*` (3 keys) — Bloque 11
- `niagara/build/*` (3 keys) — Bloque 12
- `niagara/advanced/*` (3 keys) — Bloque 13

**Total**: 36 topic keys con toda la memoria previa.

## Paths clave para investigación Bloques 14-20

```
/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/     # Install home (WSL)
├── bin/                        # Executables + native libs
│   ├── ext/                    # JARs adicionales (Bouncy Castle FIPS)
│   ├── policy/                 # 3 archivos firmados PKCS7 (Bloque 3.2)
│   └── install-data/           # Datos de instalación
├── lib/                        # Shared libs
├── knx/                        # KNX data
├── jre/                        # JDK embedded
├── modules/                    # 926 JARs corpus
├── niagara-help/               # devguide + bajadoc
├── security/                   # Install-level keystores, licenses
├── Palettes_and_Misc/          # palettes .palette
└── docs/                       # PDFs Honeywell

/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/ (equivalentes Windows):
- Workbench User Home = C:\Users\equipo\Niagara4.14\OptimizerSupervisor\
- Daemon User Home = C:\ProgramData\Niagara4.14\OptimizerSupervisor\
```

## Binarios nativos a documentar en Bloque 17

```
niagarad.exe / nre.exe / console.exe / hdbt.exe / n4mig.exe / nverify.exe
alarmDialog.dll / common.dll / cppunit.dll / dsfspi.dll
honImport.dll / lon.dll / njre.dll / nre.dll / traylcon.dll
libciper.so + libciper.so.sig (Linux/POSIX nativo)
msvcp140.dll / msvcr120.dll / vcruntime140.dll (Microsoft runtimes)
```

## Cómo abrir la nueva sesión

```bash
cd /home/cristian/niagara-research
claude
```

En la primera línea, pegá todo el bloque de texto entre los `---` (arriba de este archivo).

## Si se interrumpe a mitad

El mental model es incremental. Los archivos `.md` quedan en disco + GitHub aunque se cierre la sesión. Engram preserva los topic keys. Para retomar:

1. `mem_context` con project="niagara-research" — ves hasta dónde llegó.
2. `git log --oneline` — ves qué commits hay.
3. `ls niagara-mental-model-bloque*.md` — ves qué archivos existen.
4. Continuar desde el siguiente bloque pendiente.

---

**Archivo**: `/home/cristian/niagara-research/NEXT_SESSION_PROMPT_BLOQUES_14_PLUS.md`

**Sesión origen**: 2026-04-22 (Bloques 1-13 + INDEX completados).

**Estado actual del repo**: https://github.com/angeles725/niagara-research último commit `8e3dbbb`.

Pegá el prompt entre `---`, Claude hidrata context, propone plan, confirmás, arranca loop autónomo. Esperá ~1-2 hrs para los 7 bloques (cada uno 10-15 min sub-agents + síntesis + push).
