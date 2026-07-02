# Block 176 — chihuahua MX60: build/deploy + infraestructura de tests (gradle multi-módulo, slot-freeze 4.13→4.14, niagaraTest gap)

> **WHAT**: pipeline de BUILD/DEPLOY y estado de la infraestructura de TESTS del módulo Niagara **chihuahua** (MX60, Honeywell) — gradle multi-módulo (rt/ux/wb), `deploy.sh` (build→install a la station), disciplina de `vendorVersion`/slots frozen, watermark `BUILD_ID -dirty`, y el gap conocido `niagaraTest discovery = 0` con los seams pure-unit que SÍ corren en WSL.
> **Focus**: **chihuahua** (FUENTE PRIMARIA — código propio, no decompilado). Idioma: español.
> **Sources** (rutas reales bajo `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/`):
> - `deploy.sh`
> - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
> - `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts`, `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts`, `chihuahua/chihuahua-wb/chihuahua-wb.gradle.kts`
> - `BUILD_WORKFLOW.md` (§2, §3, §10, §11, §12), `CHANGELOG.md`
> - `chihuahua/HANDOFF.md`, `chihuahua/PORT-CHECKLIST.md`, `chihuahua/run-tests-wsl.sh`
> - `.env.local` / `.env.local.example` — **NO LEÍDOS** (solo se nota su existencia y propósito; jamás sus valores).
> **Markers**: `[CERT]` = verificado en archivo:línea o sección de doc citada al lado. `[INFER]` = deducción del autor, no textual. Marker SIEMPRE fuera de la cita.
> **Capa 26.** Continúa [Block 163].

---

## 176.1 — Estructura del build: multi-módulo gradle de 3 partes (rt/ux/wb)

El repo es un build gradle multi-proyecto de Tridium. El proyecto raíz se llama `chihuahua` `[CERT]` `settings.gradle.kts:134` y descubre subproyectos automáticamente vía el plugin `com.tridium.settings.multi-project` con `findProjects()` `[CERT]` `settings.gradle.kts:107,118-131`, que espera el layout `<part>/<part>.gradle.kts`.

Las tres partes, cada una con su `runtimeProfile`:

- **`chihuahua-rt`** — `runtimeProfile=rt`, backend (componentes `BComponent`). `moduleName="chihuahua"` `[CERT]` `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts:33-36`.
- **`chihuahua-ux`** — `runtimeProfile=ux`, frontend + servlet HTTP. Depende de `project(":chihuahua-rt")` `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:33-36,54`.
- **`chihuahua-wb`** — `runtimeProfile=wb`, vistas Workbench (`BBatchLinkEditor` como agent sobre `baja:Component`). Depende de `:workbench-wb`, `:bajaui-wb`, `:gx-rt` y de `project(":chihuahua-rt")` `[CERT]` `chihuahua/chihuahua-wb/chihuahua-wb.gradle.kts:34-58`.

Las tres partes comparten un mismo `moduleName` ("chihuahua") pero se empaquetan como 3 jars separados (`chihuahua-rt.jar`, `chihuahua-ux.jar`, `chihuahua-wb.jar`) `[CERT]` `deploy.sh:77`. `[INFER]` Es el patrón canónico de Niagara "un módulo, N partes por perfil de runtime": el installer reconcilia las partes juntas por versión compartida.

Plugins comunes por parte: `com.tridium.niagara-module` (tasks `jar`/`moduleTestJar`), `niagara-signing`, `bajadoc`, `niagara-jacoco` y `niagara-annotation-processors` (el annotation processor de Niagara genera la companion `_BXxx` y emite el bloque `<type>`/`<agent>` en `META-INF/module.xml` vía slotomatic) `[CERT]` `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts:8-29` y `chihuahua/chihuahua-wb/chihuahua-wb.gradle.kts:11-30`.

## 176.2 — Resolución de `niagara_home` y repos flat-file (por qué se configuran a mano)

El SDK de compilación se resuelve por una cadena de fallback: gradle property → system property → env `NIAGARA_HOME` → env `niagara_home` `[CERT]` `settings.gradle.kts:9-15` (espejada en `build.gradle.kts:51-56` y documentada en `PORT-CHECKLIST.md §Build environment`).

El root `build.gradle.kts` declara **manualmente** repos flat-file apuntando a `$niagara_home/bin/ext` y `$niagara_home/modules` para que los subproyectos resuelvan `:baja`, `:nre`, `:web-rt`, etc. `[CERT]` `build.gradle.kts:58-68`. El comentario explica el porqué: el convention plugin `com.tridium.convention.niagara-home-repositories` de la serie 7.6.x **no existe** en el set de plugins `7.3.40` que shippea iSMA 4.13.2.18, así que se replica su comportamiento a mano `[CERT]` `build.gradle.kts:44-56`. Las versiones de plugin gradle Tridium son fijas: `gradlePluginVersion="7.3.40"` y `settingsPluginVersion="7.3.0"` `[CERT]` `settings.gradle.kts:84-85`.

`.env.local` y `.env.local.example` existen en el repo raíz. `[INFER]` Su propósito es aportar overrides de entorno de máquina (paths de `NIAGARA_HOME`, `JAVA8`, `STATION_MODULES`) sin tocar el repo; `deploy.sh` toma esas mismas variables con `${VAR:-default}`. **No se leyó su contenido** — solo se documenta su existencia y función.

## 176.3 — Disciplina de `vendorVersion` y bump por slots frozen

El vendor se fija en el root: `defaultVendor("ANGELES")` y `defaultModuleVersion("1.3")` (atributo `vendorVersion` de todas las partes) `[CERT]` `build.gradle.kts:22-37`. El historial de bumps está documentado inline y es la traza de por qué existe la disciplina:

- `1.0 → 1.1`: release RBAC + audit-trail, nuevo slot frozen `auditLog` en `BChiDashboardService`.
- `1.1 → 1.2`: acciones frozen nuevas `exportLinks`/`importLinks`.
- `1.2 → 1.3`: nueva parte `chihuahua-wb` (perfil wb) — "las tres partes deben publicar a la misma versión para que el installer las reconcilie consistentemente" `[CERT]` `build.gradle.kts:28-36`.

**Regla dura**: cuando se agrega/quita/modifica un slot **frozen** (`@NiagaraProperty`/`@NiagaraAction`/`@NiagaraTopic`) en un `BComponent` **ya instanciado** en la station, reiniciar la station NO alcanza: si `vendorVersion` no cambió, Niagara trata el módulo como el mismo y **no reconcilia los slots frozen de la instancia existente** → el slot no aparece en el Property Sheet aunque el bytecode sí lo tenga `[CERT]` `BUILD_WORKFLOW.md §10 (líneas 341-343)`. Y aún tras el bump + restart, si el slot no aparece: **cerrar y reabrir Workbench** — el restart de la station no refresca el cache de tipos del cliente `[CERT]` `BUILD_WORKFLOW.md §10 (línea 352)` y `CHANGELOG.md §"Operational notes — frozen slot activation"` (confirmado 2026-05-25 desplegando `exportLinks`/`importLinks`).

Cambio de solo-código (cuerpo de método, sin tocar estructura de slots) NO necesita bump: el bytecode recargado al reiniciar basta `[CERT]` `BUILD_WORKFLOW.md §10 (línea 365)`.

## 176.4 — `deploy.sh`: flujo build → backup → copy → verify → activación manual

`deploy.sh` es el path canónico WSL. Uso: `./deploy.sh <ux|rt|wb|all> [--bump]` `[CERT]` `deploy.sh:44-50`. Corre con `set -euo pipefail` `[CERT]` `deploy.sh:26`. Pasos:

1. **(opcional) bump** de `defaultModuleVersion` en `build.gradle.kts` con `--bump`: lee la versión actual con `rg`, calcula `MAJOR.(MINOR+1)`, la reescribe con `sd` `[CERT]` `deploy.sh:58-64` (solo bumpea el minor — no cruza el major).
2. **Build**: arma tasks `:chihuahua-<m>:jar` por módulo y corre `./gradlew` con `-Pniagara_home=$NIAGARA_HOME` y `-Porg.gradle.java.installations.paths=$JAVA8` `[CERT]` `deploy.sh:66-72`.
3. **Backup**: si ya hay un jar en la station, lo copia a `_backups/chihuahua-<m>-pre-deploy-<TS>.jar` `[CERT]` `deploy.sh:81-84`.
4. **Copy + verify**: copia el jar generado (desde `$NIAGARA_HOME/modules`) a la station y verifica integridad por `md5sum` origen==destino, abortando si difieren `[CERT]` `deploy.sh:76-90`.
5. **Activación (manual, del humano)**: el script NO reinicia la station ni hace el smoke test — solo IMPRIME qué activación hace falta según qué se tocó `[CERT]` `deploy.sh:20-23,92-124`:
   - **wb**: cerrar y reabrir Workbench (no requiere restart de station) `[CERT]` `deploy.sh:99-104`.
   - **rt/ux con `--bump`** (slot frozen): reiniciar la station completa; si no aparece, reabrir Workbench `[CERT]` `deploy.sh:112-115`.
   - **rt/ux sin bump**: solo frontend → refrescar browser (Ctrl+Shift+R); Java → reiniciar station `[CERT]` `deploy.sh:117-120`.

`[INFER]` Nótese que el jar de origen que copia el paso 4 es `$SRC_MODULES/$JAR` = `$NIAGARA_HOME/modules/chihuahua-<m>.jar` `[CERT]` `deploy.sh:35,78` — es decir, la task `jar` del plugin Niagara instala el jar directamente en `modules/` del SDK, y `deploy.sh` lo re-copia de ahí a la station. Existe además un fast-path Windows-nativo `build-and-deploy.ps1` (~5s vs ~90s del bridge WSL/NTFS) `[CERT]` `BUILD_WORKFLOW.md §4 (líneas 191-200)`.

## 176.5 — Cross-version: compilar contra 4.13.2.18, desplegar a 4.14.0.162 (slot-freeze concern)

Los defaults de `deploy.sh` revelan una asimetría de versiones deliberada:

- **Compile / SDK** (`NIAGARA_HOME`): `/mnt/c/Niagara/iC-Niagara-4.13.2.18` `[CERT]` `deploy.sh:32` (concordante con `BUILD_WORKFLOW.md §2 (línea 143)` y con la nota de plugins 7.3.40 de iSMA 4.13.2.18 en `build.gradle.kts:47`).
- **Deploy target** (`STATION_MODULES`): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules` `[CERT]` `deploy.sh:34` (la station real del cliente; `HANDOFF.md §Build and Test → Prerequisites` declara `niagara_home = C:\Honeywell\OptimizerSupervisor-N4.14.0.162` en el host Windows).

Es decir: **se compila contra el SDK 4.13.2.18 (plugins 7.3.40) pero se despliega a una station Optimizer N4.14.0.162 (plugins 7.6.17)** `[CERT]` `deploy.sh:32,34` + `BUILD_WORKFLOW.md §11 (línea 396)`. `[INFER]` Este cross-version es el corazón del "slot-freeze concern": la reconciliación de slots frozen la hace el runtime 4.14 sobre bytecode compilado con headers 4.13 — la disciplina de `vendorVersion` (§176.3) es precisamente el mecanismo que fuerza al runtime 4.14 a reconciliar la estructura de slots aunque el jar venga de un toolchain distinto. `[INFER]` El riesgo residual: cualquier API/tipo `baja` presente en 4.14 pero ausente en 4.13.2.18 (o viceversa) no se detecta en compile-time; solo aparece como fallo de carga de tipo en el arranque de la station — de ahí el paso de verificación §5 (types count) y el fallback "revisá el log de arranque" `[CERT]` `BUILD_WORKFLOW.md §10 (línea 363)`. Toolchain Java: Java 8 obligatorio (`org.gradle.java.installations.paths` → `java-8-openjdk-amd64`), Niagara N4.14 lo requiere `[CERT]` `deploy.sh:33` + `BUILD_WORKFLOW.md §2 (línea 145)`.

## 176.6 — Watermark `BUILD_ID` con sufijo `-dirty` (task `generateBuildId`)

`chihuahua-ux` registra una task `generateBuildId` que genera `src/rc/js/lib/Version.js` + `src/rc/version.json` con un identificador por-build, siempre regenerado (`outputs.upToDateWhen { false }`) `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:68-72`. Formato del id: `<gitShortSHA>-<unixSeconds>`, con `nogit` cuando git no está disponible `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:74-103`.

**Sufijo `-dirty`**: se anexa `-dirty` cuando el `git status --porcelain` de las fuentes **shippeadas** (`chihuahua/chihuahua-rt/src` + `chihuahua/chihuahua-ux/src`) tiene cambios sin commitear al momento del build. Está scopeado a `src/` a propósito, para que ruido no-shippeado (`.atl`, `_backups`, el propio script de build, archivos generados gitignoreados) nunca dispare un dirty falso `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:91-103` + `CHANGELOG.md §"version indicator + dirty-aware BUILD_ID (PR #16)"`.

`compileJava` y `jar` dependen de `generateBuildId`; la task `jar` además sustituye `__BUILD_ID__` en `index.html` (cache-buster `?v=<sha>-<unix>` en cada asset) leyendo el `version.json` recién escrito `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:122-145`. `Version.js`/`version.json` están gitignoreados (auto-generados) `[CERT]` `chihuahua/chihuahua-ux/chihuahua-ux.gradle.kts:63-67,107`.

**Para qué sirve operativamente**: un watermark `v <BUILD_ID>` visible (abajo-derecha, `pointer-events:none`) deja confirmar de un vistazo qué build corre tras deploy+login; leer el SHA → `git show <sha>` muestra el cambio exacto, y la **ausencia de `-dirty`** garantiza que el SHA es fiel al código corriendo `[CERT]` `CHANGELOG.md §"How to verify a deploy"`. `[INFER]` Es el contra-mecanismo directo del cross-version §176.5: como el jar puede venir de un árbol con ediciones locales, el `-dirty` es la señal de que el SHA NO captura todo el código en runtime.

## 176.7 — El gap central: `niagaraTest discovery = 0` (bug de plugin 7.6.17)

El estado de tests está gobernado por un bug conocido del plugin `com.tridium.niagara-module` **7.6.17** (el de N4.14): `moduleTestAnnotationProcessor` **nunca produce la metadata** que `writeTestModuleXml` necesita, así que las clases `srcTest/` anotadas con `@NiagaraType` se saltan silenciosamente → `Total tests run: 0` `[CERT]` `chihuahua/HANDOFF.md §"STATUS — tests are NOT running"` (líneas 10-16) + `BUILD_WORKFLOW.md §11 (línea 396)`.

La decisión inline vive en el gradle de rt: los tests en `srcTest/test/` quedan como **documentación / esqueletos autorizados**, NO como gate de build `[CERT]` `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts:38-49`. Se intentaron y fallaron 5 workarounds (TestNG `BTestNg`, `@NiagaraType`+`Sys.loadType`, región AUTO GENERATED, bloque `testModuleManifest`, y mover tests a `src/main/.../tests/`) — el último bloqueado por `Dependency test-wb has invalid runtime profile wb` (el módulo rt no puede depender de `test-wb`, perfil wb, como api dep normal) `[CERT]` `chihuahua/HANDOFF.md §STATUS (líneas 17-25)`. Por eso `test-wb` entra solo como `moduleTestImplementation` (classpath de compile de test) para que los tests compilen y sigan siendo doc válida `[CERT]` `chihuahua/chihuahua-rt/chihuahua-rt.gradle.kts:50-59`.

**Consecuencia**: `./gradlew :chihuahua-rt:test`, `:chihuahua-ux:test` y `niagaraTest` reportan `NO-SOURCE` / discovery roto y NO deben usarse como gate `[CERT]` `BUILD_WORKFLOW.md §11 (líneas 398-402)`. Engram: `honeywell-mx60-chihuahua/test-discovery-decision`. El `PORT-CHECKLIST.md §"Open items"` aún lista "niagaraTest discovery — currently shows 0 tests… Investigation pending" `[CERT]` `chihuahua/PORT-CHECKLIST.md:127-128`.

> ⚠️ **Contradicción documental**: `HANDOFF.md §"Unit Tests"` (líneas 92-136) documenta invocaciones `gradlew niagaraTest --tests ...` con conteos (101 pure-unit, cobertura JaCoCo ≥70%) como si corrieran. Ese bloque es **intención histórica pre-decisión**; el propio HANDOFF lo desmiente arriba: "DOCUMENT what was intended, but do NOT expect them to produce a non-zero count on this plugin version" `[CERT]` `chihuahua/HANDOFF.md:33-36`. La fuente de verdad operativa es `BUILD_WORKFLOW.md §11` + `run-tests-wsl.sh`, no ese bloque.

## 176.8 — Lo que SÍ corre: taxonomía (a)/(b)/(c) y el runner WSL pure-unit

La política "todo es doc-only" fue refinada el 2026-05-12 a una taxonomía de 3 tipos `[CERT]` `BUILD_WORKFLOW.md §11 (líneas 371-377)`:

- **Tipo (a)** — JUnit puro, sin init de baja/NRE (lógica pura, parsing JSON, math de thresholds). **Corre en WSL standalone** vía `./chihuahua/run-tests-wsl.sh`.
- **Tipo (b)** — usa tipos baja que requieren el kernel NRE inicializado (`BAbsTime.make()`, `Sys.loadType()`). Solo Workbench con station Windows. Compile-only aquí.
- **Tipo (c)** — integration tests con station live + componentes ensamblados. Smoke test manual post-deploy.

`run-tests-wsl.sh` es un runner `JUnitCore` standalone (NO usa gradle/`niagaraTest`, elude el bug del plugin). Arma un classpath con junit 4.13.2 + hamcrest 1.3 (desde el cache gradle) + jars Niagara (`baja`, `alarm-rt`, `schedule-rt`, `history-rt`, `web-rt`, `bql-rt`, `control-rt`, y para wb: `workbench-wb`, `bajaui-wb`, `gx-rt`, más `nre.jar` y el servlet-api) `[CERT]` `chihuahua/run-tests-wsl.sh:14-59`. Flujo en 3 pasos: (1) compila prod sources de rt+ux+wb, (2) compila las suites tipo-a, (3) corre `JUnitCore` con `-source 8 -target 8` `[CERT]` `chihuahua/run-tests-wsl.sh:61-113`. **`exit 0 = todo verde; exit ≠ 0 = regresión real accionable**` `[CERT]` `chihuahua/run-tests-wsl.sh:122-128` + `BUILD_WORKFLOW.md §11 (línea 381)`.

`[INFER]` Nota de drift interno del script: el header comenta "9 suites" (v5) `[CERT]` `chihuahua/run-tests-wsl.sh:6-8`, pero el cuerpo compila/corre **12 suites** (agrega `SearchResultUtilTest`, `DirectionLabelUtilTest`, `DirectionButtonUtilTest` del part wb) `[CERT]` `chihuahua/run-tests-wsl.sh:79-112` — el comentario de cabecera quedó desactualizado respecto al runner real. Las suites tipo-a cubren los 3 parts: ux (`ChiJsonUtilTest`, `ChiThresholdHelperTest`, `ChiAlarmHelperTest`, `ChiAuditHelperTest`, `ChiHistoryStrideTest`), rt (`ChiLinkHelperTest`), wb (`PendingLinkTest`, `PendingLinkBuilderTest`, `LinkSlotNameUtilTest`, +3).

**Excluida a propósito**: `ChiHistoryHelperTest` (15 tests con `BAbsTime.make()`) es tipo (b) estructural — el kernel NRE no es booteable en WSL; permanece compile-fixed solo para que `:jar` compile, se corre en Workbench `[CERT]` `chihuahua/run-tests-wsl.sh:9-11,116-120` + `BUILD_WORKFLOW.md §11 (línea 392)`.

## 176.9 — Qué asertan los esqueletos y qué implica para Strict TDD

Los tests son **contrato escrito + documentación viva**, no fantasía: para tipo (a) el gate RED→GREEN ES ejecutable vía el runner WSL después de cada commit que toca código tipo-a `[CERT]` `BUILD_WORKFLOW.md §11 (líneas 406-409)`. Para tipo (b)/(c) el gate NO es automatizable → se sustituye por (1) review humano del test + (2) smoke test empírico en la station; los apply-phases que tocan (b)/(c) se marcan `user-verified-post-deploy` en `apply-progress` `[CERT]` `BUILD_WORKFLOW.md §11 (líneas 404,410-412)`.

Qué asertan los esqueletos, según `HANDOFF.md §"Files Created" → Java Tests` `[CERT]` `chihuahua/HANDOFF.md:380-394`:
- **pure-unit (WSL)**: `ChiJsonUtilTest` (20, serialización JSON stateless), `ChiEquipmentReaderTest` (21), `ChiAlarmHelperTest` (31, shape de `AlarmData` + `countFromAlarmDataList` con invariante `high+med+low==total`), `BChiServletTest` (29).
- **integration (station Windows)**: `BChiDashboardServiceTest` (4), `BChiServletIntegrationTest` (8, requiere `STATION_BASE_URL` ajustado).

El aporte real de estos contratos: la **validación de esquema** JSON del §Schema Validation del HANDOFF (config, equipment, alarms, alarmCounts) confirma que la salida Java matchea las fixtures del prototipo frontend — 4/4 shapes validados 2026-05-04 `[CERT]` `chihuahua/HANDOFF.md:236-351`. `[INFER]` Ese es el verdadero rol de la infra de tests hoy: en ausencia de `niagaraTest` como gate, los esqueletos + `run-tests-wsl.sh` (tipo a) + la validación de esquema documentada son el sustituto del gate automatizado, y el smoke test manual cubre lo que el kernel NRE no deja correr en WSL.

## 176.x — Connections

- **[Block 163]** — *chihuahua MX60 (`-rt/-ux/-wb`): identidad, espina HTTP servlet y postura RBAC*. Es el bloque inaugural del focus y fija la **identidad y el target de build** (las 3 partes rt/ux/wb, el servlet `mx60`, la postura RBAC sobre `BChiDashboardService`). Este Block 176 documenta cómo esas 3 partes se **compilan, versionan y despliegan**, y el estado de sus tests — el reverso operativo de la identidad que 163 describe. El bump `1.0→1.1` por el slot frozen `auditLog` (§176.3) es exactamente la feature RBAC/audit-trail que 163 describe.
- **Contraparte de comparación — postura build/deploy de Reflow**: Reflow es un módulo Niagara que en el corpus conocemos **por JAR decompilado** — nunca vimos su pipeline de build ni sus fuentes de test. `[INFER]` chihuahua es el caso opuesto y complementario: FUENTE PRIMARIA con todo el toolchain visible (gradle multi-módulo, `deploy.sh`, `run-tests-wsl.sh`, disciplina de `vendorVersion`). La asimetría es metodológicamente útil: en Reflow inferimos estructura de slots/tipos desde bytecode reconciliado; en chihuahua tenemos la causa (código + `@NiagaraProperty`/slotomatic) y podemos observar directamente el mecanismo de reconciliación de slots frozen (§176.3, §176.5) que en Reflow solo pudimos inferir por sus efectos.
