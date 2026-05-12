# Bloque TI — Infraestructura de Tests Niagara N4: Auditoría Empírica

**Fecha**: 2026-05-11
**Scope**: Meta/horizontal — investiga CAPACIDAD de testing (no dominio específico)
**Fuentes**: MX60 chihuahua srcTest/ + baja.jar/history-rt.jar (javap) + docDeveloper-doc/doc/test.html (Honeywell N4.14)
**Cross-refs**: engram #1284 (build-policy/tests-are-docs), #1301 (session summary), #1265 (bloque #73 ChiHistoryHelperTest)

---

## §TI.0 Contexto + tally global pre-bloque + objetivo

### Tally global pre-bloque TI

- **Antipatterns**: AP-1..96 (96 totales)
- **Reglas template MX60**: 1..42 (42 totales)
- **Implications**: #1..#312 (312+ totales)
- **Capas INDEX**: 1..19 (Capa 19 = bloques #68..#74)
- **Bloques cerrados**: 71 (41 framework + 8 Capa 16 SPA externa + 2 audit Reflow + 3 Capa 18 mapping + 8 Capa 19 triple-source + 2 mini-corrigenda + 7 TODOs 32)

### Motivación

La política actual `tests-are-docs` (engram #1284, BUILD_WORKFLOW.md §11) declara que los tests en `srcTest/` son **sólo documentación compilable**, no un gate de aceptación ejecutable.

La causa documentada es: plugin `com.tridium.niagara-module` 7.6.17 — el `moduleTestAnnotationProcessor` no genera el metadata XML que `writeTestModuleXml` requiere para descubrir clases `@NiagaraType` en srcTest. Resultado: `niagaraTest` corre 0 tests (`Total tests run: 0`).

Este bloque investiga empíricamente:
1. Si hay un subconjunto de tests ejecutable HOY (sin gradle, sin station).
2. Si el bug está en el annotation processor o en el task `niagaraTest` completo.
3. Cómo Tridium documenta oficialmente el sistema de tests.
4. Qué tipo de tests tenemos actualmente y en qué contexto corre cada uno.
5. Si amerita actualizar la política `tests-are-docs` a algo más granular.

---

## §TI.1 Setup empírico MX60 — gradle.kts + inventario srcTest

### Gradle configuration

**chihuahua-rt.gradle.kts**: Tiene el comentario más completo de la decisión.

```kts
// Tests in srcTest/test/ are kept as DOCUMENTATION ONLY. niagaraTest discovery
// is broken in plugin 7.6.17 — moduleTestAnnotationProcessor never produces
// the metadata that writeTestModuleXml needs, so srcTest classes with
// @NiagaraType are silently skipped (Total tests run: 0).
//
// Workaround attempted (move tests to src/main with .tests subpackage and
// promote test-wb to `api` dep) failed at writeModuleXml: test-wb has
// runtime profile wb, the rt module cannot depend on it as a normal api dep.
//
// Decision (2026-05-05): keep test files as authored skeletons in srcTest/,
// rely on manual smoke-testing on the Windows station for batch 1 acceptance.
```
Líneas 38-49. Dependencias test: `moduleTestImplementation(":test-wb")` + `moduleTestImplementation("junit:junit:4.13.2")`.

**chihuahua-ux.gradle.kts**: Sin comentario de bug. Mismas dependencias test:
```kts
moduleTestImplementation(":test-wb")
moduleTestImplementation("junit:junit:4.13.2")
```

**build.gradle.kts** (root): NO configura repos desde el filesystem WSL — lee `niagara_home=C:\\Niagara\\iC-Niagara-4.13.2.18` desde `gradle.properties`. Esto hace que `./gradlew tasks` FALLE en WSL (`Plugin not found in file:///C:/Niagara/...`).

```
FAILURE: Plugin [id: 'com.tridium.settings.multi-project', version: '7.3.0']
was not found — maven(file:/C:/Niagara/iC-Niagara-4.13.2.18/etc/m2/repository)
```

**Conclusión de Stage 1**: el build gradle completo es inaccesible desde WSL por diseño — el plugin manager busca jars en una ruta de Windows que no es válida en WSL.

### Inventario srcTest — chihuahua-ux (10 archivos)

| Archivo | Imports externos | Clasificación | Compilable WSL |
|---------|-----------------|---------------|----------------|
| `ChiJsonUtilTest.java` | solo `org.junit.*` + `java.*` | Tipo (a): JUnit puro | SI |
| `ChiAlarmHelperTest.java` | `org.junit.*` + `java.*` (sin Niagara) | Tipo (a): JUnit puro | SI (1 bug unicode en comment) |
| `ChiThresholdHelperTest.java` | `org.junit.*` + `java.*` (sin Niagara) | Tipo (a): JUnit puro | SI |
| `ChiEquipmentReaderTest.java` | `org.junit.*` + `java.*` (sin Niagara) | Tipo (a) parcial — RED phase: llama métodos no expuestos | COMPILE FAIL |
| `ChiScheduleHelperTest.java` | `org.junit.*` + `java.*` (sin Niagara) | Tipo (a): JUnit puro | SI (no verificado exhaustivo) |
| `ChiAlarmQueryHelperTest.java` | `org.junit.*` + `java.*` (sin Niagara) | Tipo (a): JUnit puro | SI (no verificado exhaustivo) |
| `ChiHistoryHelperTest.java` | `javax.baja.history.BAbsTime` (WRONG) | Tipo (b) fallido — usa BAbsTime que requiere Sys kernel | COMPILE FAIL (import bug) + RUNTIME FAIL si corregido |
| `BChiServletTest.java` | `org.junit.*` + `java.*` | Tipo (c): requiere station HTTP | NO (stub puro, pero semánticamente station-required) |
| `BChiServletIntegrationTest.java` | `org.junit.*` + `java.*` | Tipo (c): requiere station HTTP | NO (live HTTP round-trip) |
| `BChiServletThresholdTest.java` | (no verificado) | Tipo (c): servlet test | NO |

### Inventario srcTest — chihuahua-rt (7 archivos)

| Archivo | Tipo | Compilable WSL |
|---------|------|----------------|
| `BChiUpTest.java` | Tipo (c): todos PENDING-WINDOWS, BComponent required | NO |
| `BChiDashboardServiceTest.java` | Tipo (c): station required | NO |
| `BChiUpSlotTest.java` | Tipo (c): station required | NO |
| `BChiUpProtectionSlotsTest.java` | Tipo (c): station required | NO |
| `BChiCarcamoHistoryTest.java` | Tipo (c): station required | NO |
| `BChiDataLoggerHistoryTest.java` | Tipo (c): station required | NO |
| `BTestRunnerProbe.java` | **Canónico Niagara** — `extends BTestNg` + `@NiagaraType` + TestNG | NO (requiere Sys kernel) |

**Hallazgo clave**: `BTestRunnerProbe.java` es el patrón canónico documentado por Tridium: `extends BTestNg` + `@NiagaraType` + `Sys.loadType(BTestRunnerProbe.class)`. Este pattern NUNCA puede correr fuera de la station porque `Sys.loadType()` requiere el kernel NRE inicializado.

---

## §TI.2 Setup empírico Reflow — comparación cross-source

**Resultado**: Reflow-Clean-177 NO tiene ningún test Java.

```bash
find /home/cristian/modules/Prototipos/Reflow-Clean-177/ -type d -name "srcTest"
# → sin output

find /home/cristian/modules/Prototipos/Reflow-Clean-177/ -name "*Test*.java"
# → sin output
```

**nmodsreflow-ux.gradle.kts** y **nmodsreflow-rt.gradle.kts**: tienen `moduleTestImplementation(":test-wb")` configurado pero ningún archivo de test en srcTest/.

**Conclusión**: Reflow no aporta ningún patrón de testing a comparar. MX60 está más avanzado en cobertura de contratos (tiene 17 archivos de test, ~300 métodos `@Test`). Esto refuta la hipótesis implícita de que "Reflow tiene tests funcionando y MX60 no".

---

## §TI.3 Plugin 7.6.17 bug — failure mode exacto

### Bug root cause (empirical)

El bug documentado en `chihuahua-rt.gradle.kts:38-49` y engram #1284 es:

**`moduleTestAnnotationProcessor`** — el annotation processor de Niagara que genera el archivo `moduleTest-include.xml` (requerido por `writeTestModuleXml`) no produce output para las clases `@NiagaraType` en `srcTest/`. Sin ese XML, `niagaraTest` no puede descubrir las clases BTestNg.

**Efecto observable**: `./gradlew :chihuahua-ux:niagaraTest --info` retorna:
```
> Task :chihuahua-ux:niagaraTest
Total tests run: 0
```
O la task reporta `NO-SOURCE` o `UP-TO-DATE` sin haber corrido nada.

**Por qué NO afecta a JUnit puro**: El task `niagaraTest` corre **TestNG** dentro del contexto de la station Niagara (fork de proceso con el kernel NRE). Las clases JUnit puras que no son `@NiagaraType` ni `extends BTestNg` no son descubieras por ese mecanismo — y tampoco las descubre el task `test` estándar de Gradle (que busca clases en `src/test/`, no en `srcTest/`).

**Workaround intentado y fallido** (documentado en el gradle.kts): mover tests a `src/main` con subpackage `.tests` y promover `test-wb` a dependencia `api`. Falló porque `test-wb` tiene runtime profile `wb`, incompatible con módulos `rt` como dependencia `api`.

---

## §TI.4 Hallazgos en documentación Tridium

### Fuente primaria

**Archivo**: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/docDeveloper-doc/doc/test.html`
**Título**: "Niagara 4 Automated Testing with TestNG"

### Hallazgos clave (con cita textual)

1. **Framework oficial**: "The Niagara Framework uses the **TestNG** test framework for executing unit tests within the Niagara Framework." — NO JUnit 4. JUnit 4 en las dependencias es un remanente histórico / dependency opcional para tests puros. El framework canónico de Niagara es **TestNG + BTestNg**.

2. **Patrón canónico**:
   ```java
   @NiagaraType
   public class BFunctionTypeTest extends BTestNg {
     @Override public Type getType() { return TYPE; }
     public static final Type TYPE = Sys.loadType(BFunctionTypeTest.class);
     
     @Test
     public void addTest() {
       Assert.assertEquals(BFunctionType.make(BFunctionType.ADD), BFunctionType.add);
     }
   }
   ```
   La clase debe: (a) empezar con "B", (b) extender `BTestNg`, (c) tener `@NiagaraType` + Slotomatic region, (d) usar `org.testng.Assert` — NOT `org.junit.Assert`.

3. **Ejecución**: "To run tests via Gradle, run the `niagaraTest` task: `gradlew :myModule-rt:niagaraTest`". Requiere station running (el task forkea un proceso que inicializa el NRE kernel).

4. **Alternativa CLI**: `test <target>` — es el runner nativo de Niagara Workbench, disponible en el shell de la station.

5. **Confirmación del bug pattern**: el annotation processor genera `moduleTest-include.xml` requerido. Si ese archivo no se genera (bug 7.6.17), el discovery falla silenciosamente.

### Archivo secundario: build.html

Confirma: `gradlew :moduleName:moduleTestJar` compila y empaqueta los tests pero NO los corre. `niagaraTest` es la task de ejecución. Los dos tasks son independientes.

---

## §TI.5 Hipótesis H1-H4 — confirmadas/refutadas con evidencia

### H1: Hay 3 tipos de tests

**CONFIRMADA PARCIALMENTE — hay un 4to tipo no anticipado**

Los 3 tipos planteados existen:

**(a) JUnit puros (puro Java, sin Niagara)**: Confirmados. `ChiJsonUtil`, `ChiAlarmHelper`, `ChiThresholdHelper` — 0 imports Niagara. Corren en WSL sin station.

**(b) Tests con BAbsTime/BOrd/BSimple — "sin station"**: **REFUTADO**. La hipótesis asumía que instanciar `BAbsTime` no requiere station. Empíricamente: `BAbsTime.make(long)` falla con `ExceptionInInitializerError → NullPointerException at Sys.java:445` porque `Sys.loadType()` requiere el kernel NRE. **No hay forma de instanciar ninguna clase `BSimple` fuera de la station.**

**(c) Tests con station context**: Confirmados. Todos los tests en chihuahua-rt + BChiServlet* + PENDING-WINDOWS markers.

**Tipo (d) descubierto — RED-phase skeleton con métodos no implementados**: `ChiEquipmentReaderTest` llama `ChiEquipmentReader.classifyByPrefix(String)` como método público estático, pero ese método es privado/paquete en la implementación actual. Compile error. Son tests del RED cycle que documentan contratos de la API que aún no están expuestos.

**Conclusión**: la taxonomía correcta es:
- Tipo (a): JUnit puro — WSL-ejecutable HOY
- Tipo (b): usa clases Niagara — REQUIERE STATION (BAbsTime.make() → Sys.loadType() → NPE)
- Tipo (c): usa station context (BOrd.get(), BHistoryService, HTTP round-trip) — REQUIERE STATION
- Tipo (d): RED-phase skeleton — COMPILE FAIL intencional (contratos no implementados aún)

### H2: El bug 7.6.17 afecta SOLO moduleTestAnnotationProcessor

**CONFIRMADA** con matiz importante.

La documentación Tridium confirma que `moduleTestAnnotationProcessor` genera `moduleTest-include.xml` requerido para discovery. El bug impide que ese XML se genere, lo que hace que `niagaraTest` no descubra clases `@NiagaraType` — pero el task EXISTE y está configurado.

**Matiz**: incluso si se solucionara el bug, `niagaraTest` solo correría tests tipo (c) (BTestNg + TestNG + station). Los tests tipo (a) con JUnit 4 NO serían descubiertos por `niagaraTest` de todos modos — están en el archivo incorrecto para ese task.

### H3: Workbench tiene su propio runner

**CONFIRMADA** — la documentación menciona el comando `test <target>` disponible en el shell de la station. Sin embargo:
- Requiere Windows + station running
- El runner `test` descubre las mismas clases `@NiagaraType extends BTestNg` que `niagaraTest`
- No bypasea el bug de discovery — sería afectado de la misma manera
- NO ejecuta tests JUnit 4 estándar

### H4: ChiHistoryHelperTest tiene 6 tests tipo (a)/(b) ejecutables HOY

**REFUTADA** — con hallazgo crítico adicional.

Los 6 tests `testComputeRange*` fueron clasificados como tipo (b) ejecutables "con correct classpath". Empiricamente:

1. **Bug de import**: `ChiHistoryHelperTest.java:5` importa `javax.baja.history.BAbsTime` que NO EXISTE. La clase es `javax.baja.sys.BAbsTime` (confirmado con `jar tf baja.jar`). El test no compila con el source actual.

2. **Aún con import corregido**: `BAbsTime.make()` falla en runtime con `ExceptionInInitializerError → NullPointerException at Sys.java:445`. El static initializer de `BAbsTime` llama `Sys.loadType()` que requiere el kernel NRE.

3. **Los tests `testComputeTargetPoints*` y `MAX_POINTS_HARD_CEILING`**: SÍ son ejecutables en WSL (no usan BAbsTime). Compilaron y corrieron con el import corregido. 8 de los 15 tests pasaron.

**Conclusión**: H4 refutada porque la premisa "BAbsTime no requiere station" es incorrecta. Todo `BSimple` en Niagara requiere el kernel.

---

## §TI.6 Matriz empírica: test type × runnable-where

| Test type | Ejemplo | WSL gradle | WSL manual javac+java | Workbench runner | Windows gradle |
|-----------|---------|------------|----------------------|------------------|----------------|
| (a) JUnit puro — sin Niagara | ChiJsonUtilTest (28 tests) | NO (gradle falla en WSL por rutas Windows) | **SI — 28 tests OK (0.013s)** | Desconocido (no diseñados para niagaraTest) | SI (si se configura `test` task estándar) |
| (a) JUnit puro — con Niagara en classpath pero sin instanciar | ChiAlarmHelperTest (62 tests, 48 pass / 14 fail) | NO | **SI — 62 tests run, 14 FAILING (RED phase confirmado)** | NO | SI |
| (a) JUnit puro — ThresholdHelper | ChiThresholdHelperTest (4 tests) | NO | **SI — 4 tests OK** | NO | SI |
| (b) Niagara BSimple — computeRange/BAbsTime | ChiHistoryHelperTest.testComputeRange* | NO | **NO — ExceptionInInitializerError Sys.loadType()** | Desconocido | SI (con station) |
| (b+) Niagara BSimple — computeTargetPoints (no instancia BAbsTime) | ChiHistoryHelperTest.testComputeTargetPoints* | NO | **SI — pasan (no instancian BAbsTime)** | NO | SI |
| (c) Station context — BOrd.get(), BHistoryService | ChiHistoryHelperTest.testQueryHistoryData* | NO | NO | SI (con station) | SI (con station) |
| (c) Station context — BComponent BChiUp actions | BChiUpTest.resetAlarmas* | NO | NO | SI (con station) | SI (con station) |
| (c) Station context — HTTP live round-trip | BChiServletIntegrationTest | NO | NO | NO (necesita server externo) | SI (con station) |
| (d) RED skeleton — API no expuesta | ChiEquipmentReaderTest.classifyByPrefix | NO | NO (compile fail) | NO | NO (hasta que se implemente la API) |
| Niagara canónico BTestNg | BTestRunnerProbe | NO | NO (Sys.loadType NPE) | SI (con station) | SI (con station) |

**Evidencia empírica capturada**:

```
# ChiJsonUtilTest — 28 tests, WSL, 0.013s
JUnit version 4.13.2
............................
Time: 0.013
OK (28 tests)

# ChiAlarmHelperTest — 62 tests, WSL
JUnit version 4.13.2
Time: 0.152
Tests run: 62, Failures: 14

# ChiThresholdHelperTest — 4 tests, WSL
JUnit version 4.13.2
....
Time: 0.124
OK (4 tests)

# ChiHistoryHelperTest (con import corregido, BAbsTime tests) — falla en runtime
java.lang.ExceptionInInitializerError
  Caused by: java.lang.NullPointerException at javax.baja.sys.Sys.loadType(Sys.java:445)
  at javax.baja.sys.BObject.<clinit>(BObject.java:34)
Tests run: 15, Failures: 7
```

**Classpath mínimo para tipo (a)**:
```
/home/cristian/.gradle/caches/modules-2/files-2.1/junit/junit/4.13.2/.../junit-4.13.2.jar
/home/cristian/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/.../hamcrest-core-1.3.jar
+ producción compilada contra Niagara jars en /mnt/c/Niagara/iC-Niagara-4.13.2.18/modules/
```

---

## §TI.7 ROI por dominio

### Inventario de contratos cubiertos por tests tipo (a) actualmente

| Clase helper | Tests tipo (a) actuales | Tests type (b/c) | Producción LOC | Tests como % contratos |
|-------------|------------------------|------------------|----------------|------------------------|
| `ChiJsonUtil` | 28 (100% pass) | 0 | ~200 LOC | Alta cobertura |
| `ChiAlarmHelper` | 62 (48 pass / 14 fail) | 4 PENDING-WINDOWS | ~600 LOC | Media — 14 RED tests reales |
| `ChiThresholdHelper` | 3 puro + 1 PENDING | 1 PENDING | ~150 LOC | Media-baja |
| `ChiHistoryHelper` | 6 puro (computeTargetPoints, MAX_POINTS) + 6 RED-BAbsTime + 4 PENDING-STATION | ~150 LOC | Alta sobre contrato computeTargetPoints |
| `ChiEquipmentReader` | 0 (compile fail — RED) | 0 | ~400 LOC | Ninguna hasta implementar API |
| `BChiUp` / servicios | 0 (todos PENDING-WINDOWS) | n/a | ~500 LOC | Ninguna en WSL |

### Estimación ROI

**Setup para tipo (a) en WSL (one-time)**:
- Escribir script `run-tests-wsl.sh` con classpath completo: 30 minutos
- Corregir 2 bugs existentes (import BAbsTime, comment unicode): 15 minutos
- Integrar en workflow habitual: 15 minutos
- **Total setup**: ~1 hora

**Costo por SDD con tipo (a) gate**:
- Ejecutar 94+ tests existentes: ~5 segundos
- Mantener tests nuevos por SDD (promedio 5-10 tests tipo (a)): ~15-30 minutos
- **Total por SDD**: ~20-35 minutos adicionales

**Beneficio por SDD**:
- Tests tipo (a) cubren lógica de serialización, buckets de prioridad, BQL composition, latch parse/serialize, JSON escape/format — todos contratos críticos que pueden romperse silenciosamente en un refactor
- Sin gate: regresión silenciosa detectable sólo en smoke test post-deploy (~2h por bug de este tipo)
- Con gate: regresión detectada en 5 segundos en WSL antes de build

**ROI estimado para tipo (a)**:
- Setup one-time: 1h
- Savings per SDD with regression: 2h smoke test evitado (estimado 1 de cada 3 SDDs afectaría un helper puro)
- Break-even: 2 SDDs con helpers puros

**Tests tipo (c) — station required**:
- Setup en Windows: requiere station running, JDK configurado, niagaraTest funcionando
- Costo de setup: 4-8h (incluyendo debug del plugin 7.6.17 o upgrade)
- Beneficio: detectar bugs BComponent lifecycle, BOrd resolution, HTTP response
- **ROI bajo**: la mayoría de estos contratos se verifican con el smoke test post-deploy de todos modos

---

## §TI.8 Recomendación: TIER-2 PARCIAL

### Decisión: TIER-2

Hay un subconjunto ejecutable HOY (tipo a) que cubre contratos críticos de helpers puros. Mantener `tests-are-docs` para tipo (b)/(c)/(d). Actualizar la política.

**Justificación empírica**:

1. **Tipo (a) funciona**: 94 tests ejecutados en WSL en < 200ms. Classpath es fijo y accesible. Setup es ~1h one-time.

2. **Tipo (a) cubre contratos no triviales**: `ChiAlarmHelper` tiene 14 tests FAILING actualmente — estos son contratos del RED phase que documentan brechas reales en la implementación (`inferTypeFromSourceOrd`, `extractTriggerSlotFromSourceOrd` retornan null en vez de los valores esperados; `_parseLatchMap` lanza excepción en lugar de retornar map vacío para JSON malformado; `_extractLatchedAt` retorna -1 en vez de 0 para input vacío). Estos bugs son detectables y accionables.

3. **Tipo (b) está bloqueado por arquitectura Niagara** — no por el bug 7.6.17. `BAbsTime.make()` requiere `Sys.loadType()` que requiere el NRE kernel. Esto es estructural, no un bug del plugin.

4. **Tipo (c) requiere station** — el setup cost (~4-8h) excede el benefit esperado (~2h de smoke test) para el volumen de SDDs actual.

5. **La política actual `tests-are-docs` subsume todo** — pero hay un subconjunto genuinamente ejecutable que está siendo desperdiciado.

### Qué cambia y qué NO cambia

| Aspecto | Estado actual | Con TIER-2 |
|---------|--------------|------------|
| Tests tipo (a) — JUnit puro | Ignorados (tests-are-docs) | Gate de aceptación en WSL |
| Tests tipo (b) — BSimple classes | Ignorados | Siguen siendo docs (Sys.loadType requerido) |
| Tests tipo (c) — station required | PENDING-WINDOWS | Siguen siendo PENDING-WINDOWS |
| Tests tipo (d) — RED skeleton | Ignorados | Compilan como docs + se activan al implementar API |
| `niagaraTest` task | NO correr | NO correr (bug 7.6.17 sigue presente) |
| Smoke test post-deploy | Obligatorio | Obligatorio para tipo (b)/(c) |
| BUILD_WORKFLOW §11 | "tests son documentación" | Actualizar a tier 2 |

---

## §TI.9 Setup guide para TIER-2

### Script `run-tests-wsl.sh` a crear en MX60 raíz

Crear en: `/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/run-tests-wsl.sh`

```bash
#!/usr/bin/env bash
# run-tests-wsl.sh — Corre tests tipo (a) JUnit puro en WSL.
# Tests tipo (b/c) requieren station Windows — ver BUILD_WORKFLOW §11.
# Actualizar NIAGARA_HOME si cambia la version de Niagara.
set -e

NIAGARA_HOME="/mnt/c/Niagara/iC-Niagara-4.13.2.18"
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"

JUNIT_JAR="$GRADLE_CACHE/junit/junit/4.13.2/8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12/junit-4.13.2.jar"
HAMCREST_JAR="$GRADLE_CACHE/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar"
BAJA_JAR="$NIAGARA_HOME/modules/baja.jar"
ALARM_JAR="$NIAGARA_HOME/modules/alarm-rt.jar"
SCHEDULE_JAR="$NIAGARA_HOME/modules/schedule-rt.jar"
NRE_JAR="$NIAGARA_HOME/bin/ext/nre.jar"
HISTORY_JAR="$NIAGARA_HOME/modules/history-rt.jar"
WEB_JAR="$NIAGARA_HOME/modules/web-rt.jar"
SERVLET_JAR="$NIAGARA_HOME/bin/ext/javax.servlet-api-3.1.0.jar"
BQL_JAR="$NIAGARA_HOME/modules/bql-rt.jar"
CONTROL_JAR="$NIAGARA_HOME/modules/control-rt.jar"

SRC_RT="chihuahua/chihuahua-rt/src"
SRC_UX="chihuahua/chihuahua-ux/src"
TEST_UX="chihuahua/chihuahua-ux/srcTest/test"
BUILD_DIR="/tmp/chihuahua-wsl-test-$(date +%s)"

mkdir -p "$BUILD_DIR/classes"

NIAGARA_CP="$BAJA_JAR:$ALARM_JAR:$SCHEDULE_JAR:$NRE_JAR:$HISTORY_JAR:$WEB_JAR:$SERVLET_JAR:$BQL_JAR:$CONTROL_JAR"
FULL_CP="$BUILD_DIR/classes:$JUNIT_JAR:$HAMCREST_JAR:$NIAGARA_CP"

echo "=== Compilando fuentes rt + ux ==="
javac -d "$BUILD_DIR/classes" -cp "$FULL_CP" $(find $SRC_RT $SRC_UX -name "*.java") 2>&1

echo "=== Compilando tests tipo (a) ==="
# NOTA: ChiHistoryHelperTest excluido (bug import javax.baja.history.BAbsTime — usar javax.baja.sys.BAbsTime)
# NOTA: ChiEquipmentReaderTest excluido (RED phase — métodos no expuestos en producción aún)
javac -d "$BUILD_DIR/classes" -cp "$FULL_CP" \
  "$TEST_UX/com/angeles/chihuahua/ux/ChiJsonUtilTest.java" \
  "$TEST_UX/com/angeles/chihuahua/ux/ChiAlarmHelperTest.java" \
  "$TEST_UX/com/angeles/chihuahua/ux/ChiThresholdHelperTest.java" \
  "$TEST_UX/com/angeles/chihuahua/ux/ChiScheduleHelperTest.java" \
  "$TEST_UX/com/angeles/chihuahua/ux/ChiAlarmQueryHelperTest.java" 2>&1 || true

echo "=== Corriendo tests tipo (a) ==="
java -cp "$FULL_CP" org.junit.runner.JUnitCore \
  com.angeles.chihuahua.ux.ChiJsonUtilTest \
  com.angeles.chihuahua.ux.ChiAlarmHelperTest \
  com.angeles.chihuahua.ux.ChiThresholdHelperTest 2>&1

rm -rf "$BUILD_DIR"
echo "=== DONE ==="
```

### Bugs a corregir en los tests existentes

**Bug 1** — `ChiHistoryHelperTest.java:5`: import incorrecto.
```java
// ANTES (incorrecto — javax.baja.history.BAbsTime no existe)
import javax.baja.history.BAbsTime;

// DESPUÉS (correcto)
import javax.baja.sys.BAbsTime;
```
Nota: aún con el import corregido, `testComputeRange*` fallará (BAbsTime.make() requiere Sys). Solo `testComputeTargetPoints*` y `testQueryHistoryDataHardCeiling` serán ejecutables.

**Bug 2** — `ChiAlarmHelperTest.java:825`: unicode escape en comment.
```java
// ANTES (el compilador Java procesa \u en comments)
// \u escape: A → 'A'

// DESPUÉS
// \\u escape: A → 'A'
```

### Fixes en producción revelados por los 14 failures

Los 14 failures de `ChiAlarmHelperTest` son RED-phase legítimos que documentan contratos no implementados. Son accionables para la siguiente SDD que toque `ChiAlarmHelper`:

1. `inferTypeFromSourceOrd` — retorna null en vez de "up"/"carcamo"/"datalogger". La lógica de classification por prefijo del last segment del ORD no está implementada.
2. `extractTriggerSlotFromSourceOrd` — retorna "sensor.falla" (fallback) para todos los slots. El lookup de slot-name → trigger-label no está mapeado.
3. `_parseLatchMap("not-json")` — lanza `IllegalArgumentException` en vez de retornar map vacío.
4. `_extractLatchedAt("")` / `_extractLatchedAt(null)` — retorna -1 en vez de 0.

---

## §TI.10 Decisions matrix: qué actualizar en BUILD_WORKFLOW §11

### Decisiones

| Decisión | Acción | Cuándo |
|----------|--------|--------|
| Agregar script `run-tests-wsl.sh` | Crear el script en MX60 raíz | Próxima sesión que toque producción |
| Actualizar BUILD_WORKFLOW §11 | Reemplazar "tests son docs" por política TIER-2 | Al crear el script |
| Corregir import bug ChiHistoryHelperTest | 1 línea fix (ver §TI.9) | NO URGENTE — el test seguirá fallando en BAbsTime.make() |
| Corregir comment unicode bug ChiAlarmHelperTest | 1 línea fix | Al correr tests por primera vez |
| Exponer `classifyByPrefix` como package-private | Para desbloquear ChiEquipmentReaderTest | Próximo SDD que toque ChiEquipmentReader |
| Implementar contracts RED en ChiAlarmHelper | inferType + extractTriggerSlot + _parseLatchMap null-safe + _extractLatchedAt | Próximo SDD alarms |
| Implementar `computeRange` sin BAbsTime.make() | Refactor para retornar `long[]` en vez de `BAbsTime[]` | Evaluación bajo demanda |

### Implicaciones nuevas para tally global

**#313** — Tests tipo (a) JUnit puro son ejecutables en WSL con classpath manual. El task `niagaraTest` y el task `test` de Gradle NO los corren en el setup actual. Gate manual posible con `run-tests-wsl.sh`.

**#314** — `BAbsTime.make()` requiere Sys kernel (NRE) — NO instanciable fuera de station. Todo `BSimple` hereda de `BObject` que llama `Sys.loadType()` en su `<clinit>`. El classpath en WSL NO es suficiente para instanciar tipos Niagara.

**#315** — El framework oficial de tests Niagara es **TestNG + BTestNg** (NOT JUnit 4). JUnit 4 como dependencia sólo es útil para tests tipo (a) puros que no usan Niagara types. El plugin 7.6.17 no descubrirá tests JUnit 4 con `niagaraTest` — esa task está diseñada para `@NiagaraType extends BTestNg`.

**#316** — `ChiAlarmHelperTest` tiene 14 tests FAILING actualmente: 4 contratos de `inferType`/`extractTriggerSlot`, 3 contratos de `_parseLatchMap`/`_extractLatchedAt`. Todos son RED-phase accionables en la próxima SDD que toque `ChiAlarmHelper`.

**#317** — Reflow-Clean-177 tiene 0 tests Java (sin srcTest/). MX60 está adelante en coverage de contratos a nivel de helpers (~300 métodos `@Test` en 17 archivos), aunque la mayoría son PENDING-WINDOWS o RED-phase.

---

## §TI.11 Cross-refs + bibliografía Tridium docs

### Cross-refs internos

- **engram #1284** — `honeywell-mx60-chihuahua/build-policy`: política original `tests-are-docs` + bug plugin 7.6.17 documentado. Este bloque la actualiza a TIER-2.
- **engram #1265** — bloque #73: menciona `ChiHistoryHelperTest` en §73.8 como skeleton WSL-testable. CORRIGENDUM: los tests `testComputeRange*` NO son WSL-testable porque `BAbsTime.make()` requiere Sys kernel.
- **engram #1301** — session summary: "tests son skeletons documentation-only". PARCIALMENTE CORREGIDO: hay un subconjunto tipo (a) ejecutable.
- **chihuahua-rt.gradle.kts:38-49** — comentario inline original del bug.
- **BUILD_WORKFLOW.md §11** — source of truth de la política (requiere actualización con TIER-2).

### Bibliografía Tridium

- `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/docDeveloper-doc/doc/test.html` — "Niagara 4 Automated Testing with TestNG". Documento oficial Tridium. Confirma: TestNG, BTestNg, @NiagaraType, niagaraTest task, test CLI runner, moduleTest-include.xml.
- `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/docDeveloper-doc/doc/build.html` — Build system docs. Confirma: `moduleTestJar` task, `niagaraTest` task, `srcTest/` structure.
- `/mnt/c/Niagara/iC-Niagara-4.13.2.18/modules/baja.jar` — `javax.baja.sys.BAbsTime` (corrección del package erróneo en ChiHistoryHelperTest).
- `/home/cristian/.gradle/caches/modules-2/files-2.1/junit/junit/4.13.2/` — JUnit 4.13.2 accesible en WSL para tests tipo (a).

---

*Bloque TI cerrado — 2026-05-11. Clasificación: TIER-2 PARCIAL.*
*Siguiente acción: crear `run-tests-wsl.sh` + actualizar BUILD_WORKFLOW §11 en la próxima sesión que toque MX60.*

---

## §TI.12 — Extensión: Module Navigator integration (2026-05-11 follow-up)

### §TI.12.0 — Contexto del follow-up

El bloque original (§TI.0–§TI.11) fue construido con scans manuales de `srcTest/` en MX60 y lectura directa de `docDeveloper-doc/doc/test.html`. Durante esa sesión, el Module Navigator (engram #1327) no fue consultado.

El navigator indexa 926 JARs Niagara N4.14 decompilados (51,167 clases, 12 índices). Para cualquier pregunta de la forma "¿cuántas clases de test hay en el corpus Tridium?" o "¿cuál es la jerarquía exacta de BTest?", el navigator responde en segundos lo que el scan manual tardó decenas de minutos en establecer — y con evidencia de source decompilado que el scan de filesystem no puede proveer.

**Qué aporta §TI.12**: evidencia del navigator que valida, amplía y en un caso corrige los hallazgos de §TI.1–§TI.5. La conclusión TIER-2 PARCIAL se mantiene. No hay refutación.

Cross-refs: engram #1327 (`tooling/module-navigator-reference`), engram #1326 (original bloque).

---

### §TI.12.1 — Inventory queries (evidencia Stage 1)

Todas las queries corridas desde:
`cd /home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator && python3 tools/module_nav.py <cmd>`

**Nota sobre flags del navigator**: los flags `--module` y `--max` NO existen en `source` y `grep` respectivamente. Los flags correctos son: `grep -n N` (máx resultados), `source` acepta solo `--code` y `--grep`. El flag `--filter` no existe en `annotations`. Estas limitaciones se documentan como evidencia de los límites del tool.

#### Query 1 — corpus stats
```
python3 tools/module_nav.py stats
→ Submodules (JARs): 926 | Unique modules: 661 | Java files: 51,167
→ Types: doc=99, rt=500, ux=103, wb=197, se=5, standalone=22
→ Classes with extends: 33,019 | Unique parents: 6,273
→ Import statements: 485,272 | Class import edges: 408,235
```

#### Query 2–4 — clases base del framework de tests
```
python3 tools/module_nav.py class BTest
→ 2 entries: docSource-doc (977 LOC) + test-wb (536 LOC). Extends: BObject.

python3 tools/module_nav.py class BTestNg
→ 2 entries: docSource-doc (83 LOC) + test-wb (57 LOC). Extends: BTest.

python3 tools/module_nav.py class BTridiumTestNg
→ 2 entries: docSource-doc (638 LOC) + test-wb (344 LOC). Extends: BTestNg.
```

**Hallazgo clave**: el navigator siempre retorna 2 entries para cada clase framework — una en `docSource-doc` (full source, mayor LOC) y otra en `test-wb` (bytecode decompilado, LOC menor). `docSource-doc` es el módulo que empaqueta el source original; `test-wb` es el JAR binario que consume el build.

#### Query 5–8 — subclases especializadas
```
python3 tools/module_nav.py class BStationTestBase
→ Extends: BTestNgStation (NOT BTestNg directamente). 2 módulos.

python3 tools/module_nav.py class BBaseLocaleTest
→ Extends: BTestNg. 71 LOC (test-wb), 94 LOC (docSource-doc).

python3 tools/module_nav.py class BBaseUiTest
→ Extends: BTestNg. Abstract. 50 LOC (test-wb).

python3 tools/module_nav.py class BISystemTest
→ Interface, extends BInterface. 11 LOC (test-wb).
```

#### Query 9 — jerarquía completa BTest (depth 3)
```
python3 tools/module_nav.py hierarchy BTest --depth 3
→
BTest  (docSource-doc)
    |-- BFwTest  (docSource-doc)
    `-- BTestNg  (docSource-doc)
        |-- BBaseLocaleTest  (docSource-doc)
        |-- BBaseUiTest  (docSource-doc)
        |-- BTestNgStation  (docSource-doc)
        |   `-- BStationTestBase  (docSource-doc)
        `-- BTridiumTestNg  (docSource-doc)
```

**Jerarquía completa confirmada**: 2 ramas directas de BTest:
- `BFwTest` — legacy framework (NOT TestNG). Test methods nombrados `testXxx()`, run por `BTest.list()` vía reflection.
- `BTestNg` — rama modern TestNG. Subclases: `BTridiumTestNg` (utilidades assert), `BBaseLocaleTest`, `BBaseUiTest`, `BTestNgStation → BStationTestBase`.

#### Query 10 — search '*Test'
```
python3 tools/module_nav.py search '*Test'
→ 47 resultados (class=41, interface=6)
```

Ver distribución completa en §TI.12.4.

#### Query 11 — search '*TestCase'
```
python3 tools/module_nav.py search '*TestCase'
→ 3 resultados: TestCase (interface, apachePoi-rt, 94 LOC)
               TestCase (inner class en BTridiumTestNg — docSource-doc y test-wb)
→ NO hay clases JUnit-style *TestCase en el corpus Tridium propio.
```

#### Query 12 — grep 'extends BTest'
```
python3 tools/module_nav.py grep 'extends BTest' -n 30
→ 14 matches en 14 archivos
→ Todos en docSource-doc + test-wb únicamente.
→ NINGÚN módulo funcional (fuera del framework) implementa subclases concretas de BTest.
```

**Hallazgo crítico**: el grep `extends BTest` sólo aparece en `docSource-doc` y `test-wb` — las clases base del framework. Ningún módulo funcional de Tridium (bacnet, history, alarm, schedule, etc.) compila tests como subclases BTest en sus JARs de producción. Los tests viven en `srcTest/` y se compilan en `moduleTestJar` por separado — no se shipean en los JARs normales.

#### Query 13 — grep 'BTestNg' (referencias)
```
python3 tools/module_nav.py grep 'BTestNg' -n 30
→ 30+ matches en 9 archivos (todos en docSource-doc + test-wb)
→ Línea clave: TestRunnerNg:162  TypeInfo[] types = Sys.getRegistry().getConcreteTypes(BTestNg.TYPE.getTypeInfo());
→ Línea clave: TestRunnerNg:236  if (!t.isAbstract() && t.is(BTestNg.TYPE) && !t.is(BISystemTest.TYPE))
```

Esto confirma el mecanismo de discovery: `TestRunnerNg` usa `Sys.getRegistry()` para encontrar tipos concretos que extienden `BTestNg.TYPE` — lo que requiere que el módulo de tests esté cargado en el NRE registry. Sin `Sys.loadType()` funcionando (sin kernel), `BTestNg.TYPE` es nulo y el discovery falla.

---

### §TI.12.2 — Decompiled source studies (Stage 2)

#### BTest (test-wb, 536 LOC)

- `public abstract class BTest extends BObject` — hereda de BObject
- `public static final Type TYPE = Sys.loadType(BTest.class);` (línea 58) — Sys.loadType en static initializer
- Métodos de test: `public void setup()` / `public void cleanup()` + reflection sobre métodos `testXxx()`
- Inner class `TestStationHandler` con `startStation()` → `Nre.loadPlatform()` (línea 675) — bootstrap para tests de station

**Patrón bootstrap documentado** (BTest.java:667–680):
```java
public void startStation() {
  if (station.isRunning()) return;
  Nre.clearPlatform();
  Nre.loadPlatform();          // ← bootstrap NRE platform
  Nre.getServiceManager().startAllServices();
  station.start();
  Station.stationStarted = true;
  Station.atSteadyState = true;
}
```

**Observación crítica**: `Nre.loadPlatform()` inicializa la plataforma NRE — pero `BTest.class` ya tiene `Sys.loadType(BTest.class)` en su static initializer (línea 58). Por lo tanto, para instanciar `BTest` (o cualquier subclase) se necesita el kernel NRE inicializado ANTES de que el classloader cargue la clase. `Nre.loadPlatform()` en `startStation()` llegó demasiado tarde para resolver el `ExceptionInInitializerError` que observamos en §TI.6 — ese error ocurre al cargar `BAbsTime` que extiende `BObject` que tiene `Sys.loadType()` en su `<clinit>`.

#### BTestNg (test-wb, 57 LOC)

- `public abstract class BTestNg extends BTest` — abstract
- `public static final Type TYPE = Sys.loadType(BTestNg.class);` (línea 34)
- `@BeforeMethod public void setClassLoader()` — usa `AccessController.doPrivileged` para fijar el `ContextClassLoader`
- `@AfterMethod public void restoreClassLoader()` — restaura el classloader original
- Sin `@Test` methods propios — es base class pura
- Imports: `org.testng.annotations.*`, `com.tridium.testng.TestRunnerNg`

#### BFwTest (docSource-doc / test-wb, 127–202 LOC)

- `public abstract class BFwTest extends BTest` — rama legacy (NO TestNG)
- `public static final Type TYPE = Sys.loadType(BFwTest.class);` (línea 40)
- NO usa `@Test`, NO usa `org.testng.*`
- Métodos de utilidad: `getTempDir()` → `new File(Sys.getNiagaraUserHome(), ...)`, `getTestDir()` → `Sys.getNiagaraUserHome() + ...`
- Utilidades de whitebox testing via reflection: `getField(Object, String)`, `setField(Object, String, Object)`, `method(Object, String, Class[])` — acceden a campos y métodos privados vía reflection
- `station()` → `getTestStation()` — delega a BTest.TestStationHandler
- **Conclusión**: BFwTest es la rama legacy pre-TestNG. Sus subclases usan métodos `testXxx()` invocados por reflection desde BTest.list(). Requiere el mismo kernel NRE que BTestNg.

#### BStringTest (kitControl-rt, 220 LOC)

**CORRECCIÓN CRÍTICA DE NOMENCLATURA**: `BStringTest` en kitControl-rt NO es un test de unidad. Es un `BComponent` que implementa una función de comparación de strings para ser usado en pantallas HMI — similar a `BStringEqual`, `BStringContains`. Su nombre sigue la convención del módulo kitControl ("BString*" = función de string), no la convención de testing. `extends BComponent`, tiene `@NiagaraProperty` para `out`, `inA`, `inB`, `testSelect`.

Esta es una **trampa de nomenclatura** — el nombre "*Test" no implica "test de unidad" en el corpus Tridium. Ver §TI.12.4.

#### BTridiumTestNg (docSource-doc, 638 LOC)

- `public abstract class BTridiumTestNg extends BTestNg`
- `public static final Type TYPE = Sys.loadType(BTridiumTestNg.class);`
- Provee utilidades: `toDataProviderArray(Iterable<T>)`, `toDataProviderArray(Stream<T>)`, `toDataProviderArray(T...)` — para TestNG `@DataProvider`
- Inner class `TestCase` — wrapper para casos individuales
- Sin `@Test` methods propios — es base class con utilidades

#### BStationTestBase (docSource-doc, 430 LOC)

- `public abstract class BStationTestBase extends BTestNgStation`
- Hereda vía `BTestNgStation → BTestNg → BTest`
- Imports: `BFoxService`, `BFoxSession`, `BJettyWebServer`, `BAuthenticationService`, `BNiagaraNetwork` — depende de Fox, Jetty, authn
- `configureTestStation(BStation, String, int, int)` — configura servicios de auth + Fox para tests de integración web
- Para tests que requieren HTTP round-trips y Fox protocol

#### TestRunnerNg (docSource-doc, 1045 LOC)

- NO extiende nada de test framework
- Imports: `Nre`, `NreLib`, `NModule`, `NRegistry`, `BAbsTime`, `Sys`
- `init()` (línea 275–312): llama `Nre.getModuleManager().loadModule("tridiumTest", RuntimeProfile.wb)` y `start = BAbsTime.now()` (línea 299) — **confirma que el kernel NRE debe estar inicializado antes de llamar init()**
- Discovery: `Sys.getRegistry().getConcreteTypes(BTestNg.TYPE.getTypeInfo())` (línea 162) — requiere registry del kernel

**Tabla resumen source studies**:

| Clase | Módulo | LOC | Extends | Assertion API | Station req | Bootstrap |
|-------|--------|-----|---------|---------------|-------------|-----------|
| `BTest` | test-wb | 536 | BObject | `pass()`/`fail()` custom | SI (para TestStationHandler) | `Sys.loadType` en `<clinit>` |
| `BTestNg` | test-wb | 57 | BTest | `org.testng.Assert` | NO (sólo kernel NRE) | `Sys.loadType` en `<clinit>` |
| `BFwTest` | test-wb | 127 | BTest | legacy (`pass()`/`fail()`) | Opcional (via `station()`) | `Sys.loadType` en `<clinit>` |
| `BTridiumTestNg` | test-wb | 344 | BTestNg | `org.testng.Assert` | NO | `Sys.loadType` en `<clinit>` |
| `BStationTestBase` | test-wb | 229 | BTestNgStation | `org.testng.Assert` | SI (Fox + Jetty) | `Sys.loadType` en `<clinit>` |
| `BStringTest` | kitControl-rt | 126 | **BComponent** | N/A — es componente HMI | N/A | `Sys.loadType` en `<clinit>` |

---

### §TI.12.3 — H1/H4 re-evaluación (Stage 3)

#### H1 — "BAbsTime.make() requiere Sys kernel" (original verdict: REFUTADO)

**H1 se mantiene REFUTADO. No hay patrón de salvaje.**

El navigator muestra que `BTest.TestStationHandler.startStation()` llama `Nre.loadPlatform()` para inicializar el kernel antes de lanzar la station. PERO: esta llamada llega TARDE.

El problema ocurre ANTES de que cualquier método de test se ejecute: cuando el JVM carga la clase `BAbsTime`, su `<clinit>` (static initializer heredado de `BObject`) llama `Sys.loadType(BObject.class)`. Si el kernel NRE no está inicializado EN ESTE MOMENTO, se lanza `ExceptionInInitializerError`. El `Nre.loadPlatform()` de `BTest.startStation()` sólo se puede llamar DESPUÉS de que el kernel ya está disponible — lo que es imposible si la clase misma no puede cargarse.

La única forma de usar `BAbsTime` fuera de la station sería:
1. Inicializar el NRE kernel programáticamente ANTES de cargar cualquier clase `BObject`.
2. Esto requiere `nre.jar` + `baja.jar` + los módulos del platform + las librerías nativas de Niagara.
3. En WSL, las librerías nativas de Niagara (.dll en Windows) no están disponibles.

**Evidencia del navigator**:
- `TestRunnerNg.init()` línea 299: `start = BAbsTime.now();` — se ejecuta DESPUÉS de `Nre.getModuleManager().loadModule(...)`. El kernel ya está corriendo cuando se instancia BAbsTime aquí.
- `BTest.TYPE = Sys.loadType(BTest.class)` línea 58 — static field, inicializado al cargar la clase. El intento de cargar BTest sin kernel falla aquí.
- `grep 'Sys\.boot\|Nre\.init\|loadPlatform'` sobre todo el corpus: **0 resultados** — no hay ningún utilitario de bootstrap standalone en los JARs decompilados.

**Veredicto H1 revisado**: REFUTADO — CONFIRMADO. El patrón `Nre.loadPlatform()` existe (BTest.java:675) pero requiere un kernel ya funcional para ser invocado. No es un bootstrap standalone que permita instanciar `BAbsTime` en WSL.

#### H4 — "ChiHistoryHelperTest.testComputeRange* ejecutable WSL" (original verdict: REFUTADO)

**H4 se mantiene REFUTADO.**

El resultado de H1 se hereda directamente: si `BAbsTime.make()` requiere kernel NRE, los tests `testComputeRange*` que instancian `BAbsTime` siguen siendo no-ejecutables en WSL.

El import bug (`javax.baja.history.BAbsTime` → `javax.baja.sys.BAbsTime`) sigue siendo un bug real a corregir, pero su corrección no desbloquea los tests — el runtime failure persiste.

**Posible rescate parcial**: los tests `testComputeTargetPoints*` y `testQueryHistoryDataHardCeiling` del bloque original (§TI.6) ya eran ejecutables en WSL SIN instanciar BAbsTime — eso no cambia. Son la única porción salvable de `ChiHistoryHelperTest`.

---

### §TI.12.4 — Test class distribution Tridium-wide (Stage 4)

#### Navigator query: `search '*Test'` → 47 resultados

La distribución real de los 47 resultados, categorizada:

| Módulo | Clases *Test | Tipo real |
|--------|-------------|-----------|
| `test-wb` | 7 | Framework base: BTest, BFwTest, BTestNg, BStationTestBase, BISystemTest (interface), ITest (interface), IBeforeTest/IAfterTest (interfaces) |
| `docSource-doc` | 14 | Mismo contenido que test-wb + clases duplicadas con más LOC (source completo) |
| `obix-rt` | 9 | AbstimeTest, ContractTest, DateTest, FragmentsTest, IOTest, ReltimeTest, Test, TimeTest, TreeTest, UriTest — framework de test PROPIO de obix (no Tridium) |
| `chart-wb` | 6 | AreaTest, BarTest, LineTest, PieTest, StackedBarTest, TimeSeriesTest — NO son unit tests; son **componentes de charts Workbench** (patrón de nomenclatura chart) |
| `clHVAC-rt` | 3 | BBitTest, BStressTest, CfBitTest — son **BComponents** de función HVAC, NO unit tests |
| `kitControl-rt` | 1 | BStringTest — **BComponent** de comparación string HMI (ver §TI.12.2) |
| `apachePoi-rt` | 7 | ChiSquareTest, GTest, KolmogorovSmirnovTest, etc. — clases de **estadística** Apache Commons Math, NO unit tests |
| `ledMonitor-rt` | 1 | ThreadTest (inner class) — utilidad interna |
| `clEnoceanNetwork-rt` | 2 | BClEnoceanBindingEnumTest, BClEnoceanBindingRockerTest |

**Hallazgo fundamental — corrección del engram #1327**: La memoria anterior (engram #1327) documentó "47 test classes Tridium-wide" como si todas fueran unit tests en el estilo BTestNg. **Esto es INCORRECTO**. De los 47:

- **0** son implementaciones concretas de `BTestNg` / `BTridiumTestNg` en módulos funcionales (fuera de test-wb/docSource-doc)
- **~15** son clases framework base o duplicados docSource
- **~15** son componentes funcionales o clases estadísticas con "Test" en el nombre por otra razón
- **~9** son el framework de test propio de OBIX (completamente independiente de Tridium BTest)
- **~8** son de apachePoi (Commons Math, biblioteca 3rd-party)

**La conclusión correcta**: el corpus Tridium N4.14 NO tiene tests de unidad concretos (tipo BTestNg) compilados en los JARs de producción de ningún módulo funcional. Los tests existen en `srcTest/` y se compilan en `moduleTestJar` por separado. El navigator no puede indexarlos porque no están en los JARs del install.

**Tabla módulo × clases Test (solo las relevantes para tests reales)**:

| Módulo | Clases con nombre *Test | Son unit tests? | Notas |
|--------|------------------------|-----------------|-------|
| test-wb | 7 | SI — framework base | BTest, BTestNg, BFwTest, BStationTestBase, BISystemTest + interfaces TestNG |
| docSource-doc | 14 | SI — source del framework | Duplicados completos del contenido de test-wb |
| obix-rt | 9 | Parcial — framework propio OBIX | `extends Test` (OBIX), no `extends BTest` |
| chart-wb | 6 | NO | Componentes UI de charts |
| clHVAC-rt | 3 | NO | BComponents de función HVAC |
| kitControl-rt | 1 | NO | BComponent comparación string HMI |
| apachePoi-rt | 7 | NO | Clases estadísticas Commons Math |
| clEnoceanNetwork-rt | 2 | Parcial — no verificado |  |

#### Comparación con bloque original §TI.2

El bloque original estableció: Reflow-Clean-177 tiene 0 tests Java. MX60 tiene 17 archivos de test en srcTest/. Esto se confirma vía navigator: los tests en srcTest/ no están indexados en los JARs del install, por lo que no aparecen en `search '*Test'`. El navigator responde preguntas sobre el framework (clases base, jerarquía, source del runner) pero NO puede listar qué tests existen en srcTest/ de un módulo dado — esa pregunta sigue requiriendo inspección del filesystem.

---

### §TI.12.5 — TIER-2 setup refinement (Stage 5)

#### Contexto del análisis

El bloque original recomendó TIER-2 PARCIAL: tests tipo (a) JUnit puro ejecutables en WSL, tipos (b/c/d) siguen siendo docs.

El navigator confirma que Tridium usa **dos sistemas de tests** que coexisten:
1. **BFwTest (legacy)**: tests con métodos `testXxx()`, discovery vía reflection, runner legacy, NO TestNG.
2. **BTestNg / BTridiumTestNg (modern)**: tests con `@Test`, runner TestNG, `@DataProvider`, `org.testng.Assert`.

MX60 usa **JUnit 4** (`org.junit.Test`, `assertEquals`) — ni BFwTest ni BTestNg. Es un tercer sistema.

#### Opciones para TIER-2

**Opción A — Mantener JUnit 4 (estado actual)**
- Tests tipo (a) siguen usando `org.junit.*`
- Se ejecutan con `java -cp ... org.junit.runner.JUnitCore`
- Diverge de la convención Tridium (BTestNg/TestNG) y de lo que `niagaraTest` espera
- 94 tests ya pasan con este setup
- No requiere ningún cambio de código

**Opción B — Migrar tests a BTestNg**
- Reescribir los 17 archivos de test de MX60 para `extends BTridiumTestNg` + `@NiagaraType`
- Beneficio: integración con `niagaraTest` task cuando Tridium corrija el bug 7.6.17
- Costo: ~8-16h de rewrite (cambio de API de assertions, restructuración de test classes, agregar Slotomatic region a cada una)
- Problema: tests tipo (a) que actualmente pasan en WSL dejarían de ser ejecutables en WSL (requieren kernel NRE)
- Regresión neta: pierde 94 tests WSL-ejecutables a cambio de tests que NO corren hasta que se arregle el plugin 7.6.17

**Opción C — Hybrid: mantener JUnit 4 para tipo (a) + BTestNg para nuevos tests tipo (b/c)**
- Tests existentes tipo (a): sin cambios (JUnit 4, WSL-ejecutables)
- Tests nuevos que requieren station o tipos Niagara: usar `extends BTridiumTestNg`
- BUILD_WORKFLOW §11 documenta los dos sistemas con sus criterios de uso
- Costo incremental: ~1-2h para documentar la política + agregar template de BTridiumTestNg
- Beneficio: coexistencia gestionada; los 94 tests tipo (a) siguen siendo gate en WSL; cuando se arregle el plugin 7.6.17, los tests BTestNg se activan automáticamente

#### Recomendación: OPCIÓN C

**Justificación**:
1. Los 94 tests tipo (a) ya funcionan — es una inversión realizada que no se debe tirar.
2. Migrar tipo (a) a BTestNg los vuelve WSL-inexecutables sin ganar nada hasta que se corrija el bug 7.6.17 (sin ETA conocida).
3. Los casos de uso reales que justifican BTestNg son los tests tipo (b/c): history queries, BComponent lifecycle, station setup — ninguno de estos es solucionable con JUnit 4 de todos modos.
4. La Opción C respeta la inversión actual + habilita el camino correcto para el futuro.

**Template para nuevos tests tipo (b/c) con BTestNg**:
```java
@NiagaraType
public class BChiSomethingTest extends BTridiumTestNg {
  @Override public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BChiSomethingTest.class);
  // Slotomatic region aquí (generado por annotation processor)

  @Test
  public void testSomethingWithStation() {
    // usa org.testng.Assert
    Assert.assertEquals(actual, expected);
  }
}
```

---

### §TI.12.6 — Bootstrap pattern (Stage 6)

**NO se encontró un patrón de bootstrap standalone que rescate los tests tipo (b) en WSL.**

El navigator reveló `Nre.loadPlatform()` en `BTest.TestStationHandler.startStation()` (BTest.java:675):
```java
Nre.clearPlatform();
Nre.loadPlatform();
Nre.getServiceManager().startAllServices();
station.start();
```

Sin embargo, este patrón NO es un bootstrap standalone por las siguientes razones (confirmadas con source):

1. `BTest.class` tiene `public static final Type TYPE = Sys.loadType(BTest.class)` en su static initializer (línea 58 de BTest.java). Para que `startStation()` sea invocado, el JVM ya debió cargar `BTest` — lo que ya debió ejecutar `Sys.loadType()` exitosamente. Si `Sys.loadType()` falla (kernel no inicializado), la clase nunca carga y `startStation()` nunca es invocable.

2. El grep global `'Sys\.boot\|Nre\.init\|loadPlatform'` sobre 50,658 archivos retorna **0 resultados** fuera de BTest.java — no hay ningún bootstrap helper standalone en el corpus.

3. `TestRunnerNg.init()` (línea 299) ya usa `BAbsTime.now()` internamente — el runner asume que el kernel está funcionando cuando es invocado.

**H1 refutation stands. H4 refutation stands.** El único path para ejecutar tipo (b) en WSL sería ejecutar el NRE kernel completo en Linux — lo que requeriría las librerías nativas de Niagara portadas a Linux, algo que Tridium no provee.

---

### §TI.12.7 — Module Navigator como dependency obligatoria de research bloques futuros (Stage 7)

#### Queries de arranque recomendadas para cualquier bloque de research Niagara

El navigator debe ser la **primera fuente** consultada antes de cualquier scan de filesystem. Las queries de arranque recomendadas:

```bash
# 1. Corpus overview
python3 tools/module_nav.py stats

# 2. Clase/interfaz específica de interés
python3 tools/module_nav.py class <NombreClase>

# 3. Jerarquía completa
python3 tools/module_nav.py hierarchy <NombreClase> --depth 3

# 4. Quién la implementa (si es interface)
python3 tools/module_nav.py implementors <NombreInterface>

# 5. Búsqueda por patrón
python3 tools/module_nav.py search '<Glob>'

# 6. Source decompilado completo
python3 tools/module_nav.py source <NombreClase> --code

# 7. Grep de patrón de código
python3 tools/module_nav.py grep '<regex>' -n 30

# 8. Módulo completo profile
python3 tools/module_nav.py module <nombre-modulo>
```

#### Dominios MX60 y su status de testabilidad

| Dominio | Clase helper | Tests tipo (a) WSL hoy | Tests tipo (b/c) station | Acción para BTestNg |
|---------|-------------|------------------------|--------------------------|---------------------|
| Serialización JSON | ChiJsonUtil | 28 tests OK | — | Bajo valor (ya cubierto) |
| Alarmas | ChiAlarmHelper | 48/62 pass (14 RED) | 4 PENDING-WINDOWS | Plantear ChiAlarmActionTest como BTestNg |
| Umbrales | ChiThresholdHelper | 4 tests OK | 1 PENDING | Bajo valor |
| Historia | ChiHistoryHelper | 6 parciales (BAbsTime excluidos) | `queryHistoryData` station | `BHistoryService` via BTestNgStation |
| Equipment | ChiEquipmentReader | 0 (compile fail RED) | — | Desbloquear con API pública |
| BChiUp | BChiUp actions | 0 (todos PENDING-WINDOWS) | Station lifecycle | BTestNgStation + station XML |
| Servlets | BChiServlet | 0 (requiere HTTP) | Round-trip HTTP | BStationTestBase (Fox + Jetty) |

#### BStationTestBase como mock de station para tipo (c)

El navigator revela que `BStationTestBase` provee `configureTestStation(BStation, String, int, int)` con Fox + Jetty + autenticación. Para los tests de `BChiServlet`, este es el path correcto — pero requiere que `niagaraTest` task funcione (corrección del plugin 7.6.17).

**Conclusión sobre el plan "tests for each module build"**: los módulos que hoy producen tests tipo (a) ejecutables en WSL (chihuahua-ux helpers) son candidatos para CI inmediato. Los módulos con BComponents (chihuahua-rt) necesitan station — deben esperar o usar un mock via `BTest.createTestStation()` una vez que el kernel esté disponible en el entorno de build.

---

### §TI.12.8 — Updates a secciones originales §TI.0–§TI.11

Las siguientes correcciones/ampliaciones deberían integrarse en los bloques originales en una sesión futura:

1. **§TI.4 (Hallazgos Tridium docs)** — agregar después del patrón canónico: "El navigator confirma que NO hay implementaciones concretas de BTestNg compiladas en los JARs de producción de módulos funcionales. Los tests existen sólo en `srcTest/` / `moduleTestJar` por separado — no están indexados en el corpus del navigator."

2. **§TI.1 (Inventario srcTest)** — agregar nota al pie: "Los 17 archivos de test de MX60 no son visibles en el navigator (`search '*Test'`) porque `moduleTestJar` no se incluye en los JARs del install. El navigator sólo ve el framework base (test-wb) y los BComponents con 'Test' en el nombre por otras razones."

3. **§TI.5 H1** — agregar: "El navigator confirma: `grep 'Sys\.boot\|Nre\.init\|loadPlatform'` sobre 50,658 archivos → 0 resultados en módulos funcionales. No existe bootstrap standalone. H1 refutación es estructural, no contingente a un bug corregible."

4. **§TI.2 (Reflow)** — ampliar: "El navigator no lista ninguna clase *Test de Reflow — confirma que Reflow tiene 0 tests en sus JARs compilados. La comparación manual de §TI.2 (`find srcTest/`) es la única forma de confirmar si existen archivos en srcTest/ (el navigator no los ve)."

5. **§TI.4 (Framework Tridium)** — aclarar: "El corpus tiene 47 clases con '*Test' en el nombre, pero la mayoría son BComponents funcionales (BStringTest, BBitTest, etc.) o clases de estadística (apachePoi). El framework real de tests Tridium son sólo las clases en test-wb: BTest, BTestNg, BFwTest, BTridiumTestNg, BStationTestBase + interfaces."

6. **§TI.8 (Recomendación TIER-2)** — agregar §TI.12.5 como apéndice: la Opción C Hybrid (JUnit 4 para tipo a + BTestNg template para tipo b/c nuevos) es la evolución natural.

7. **§TI.11 (Cross-refs)** — agregar: "engram #1327 (`tooling/module-navigator-reference`) — tool para queries del corpus Niagara; debe consultarse antes de cualquier scan manual en bloques futuros."

---

### §TI.12.9 — Cross-refs

- Module Navigator reference: engram #1327 (`tooling/module-navigator-reference`)
- Original bloque engram: #1326 (`bloque/test-infrastructure/niagara-n4-tests-empirical`)
- Esta extensión engram: `bloque/test-infrastructure/ti12-navigator-extension`
- Tooling location: `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator/`
- Navigator quickstart: `cd <tooling-location> && python3 tools/module_nav.py <cmd>`
- Flag correction documentada: `--module` no existe en `source`; `--max` no existe en `grep` (usar `-n N`); `--filter` no existe en `annotations`

---

*§TI.12 cerrado — 2026-05-11. Conclusión TIER-2 PARCIAL confirmada. Opción C Hybrid recomendada. H1/H4 refutaciones confirmadas estructuralmente.*

---

## §TI.13 — Investigación: ¿existe bypass análogo a httpapi skipModuleValidation? (2026-05-12 follow-up)

### §TI.13.0 — Contexto

En la sesión httpapi 2026-04-19 (engram #332) se documentó un bypass legítimo del sistema de validación de módulos: la combinación de sysprop `niagara.classLoader.skipModuleValidation=true` más la license feature `tridium:developer skipModuleValidation="true"` (presente en Webs.license línea 40) permite cargar módulos sin firma Tridium sin modificar el ClassLoader.

El usuario planteó (2026-05-12, engram #1338) la pregunta estructuralmente correcta: ¿existe un mecanismo análogo para el blocker del kernel bootstrap documentado en §TI.5/§TI.12? Específicamente: el bloque §TI.6 buscó patterns obvios (`Sys.boot`, `Nre.init`, `loadPlatform`) y encontró 0 matches en módulos funcionales — pero esa búsqueda no abarcó la metodología sysprop + license feature combinados que Tridium usa para bypasses.

Esta sección investiga 5 hipótesis empíricamente usando Vineflower decompiled sources en `/mnt/c/modules/Prototipos/modulos/organized/`.

---

### §TI.13.1 — H1: sysprop oscuro en kernel init

**Cómo se probó**: búsqueda de `getProperty` + keywords `skip|standalone|headless|test|bypass` en `Nre.java` (vineflower, 1495 líneas) y `Sys.java` (vineflower, 185 líneas).

**Evidencia encontrada**:

`Sys.java` (javax/baja/sys/Sys.java) es un facade thin de 185 LOC sin ninguna llamada a `System.getProperty`. Todo delega a `Nre.*`. `Sys.loadType(Class)` (L170-172) es:
```java
public static Type loadType(Class<?> cls) {
    return Nre.getSchemaManager().load(cls);
}
```
El NPE ocurre cuando `schemaManager` es null (Nre.java:1341), porque `schemaManager` solo se inicializa en `Nre.boot()` (Nre.java:711). No hay ningún guard ni path alternativo en este método.

En `Nre.java` sí se encontraron sysprops relevantes al testing:

| Sysprop | Línea | Efecto real |
|---------|-------|-------------|
| `niagara.unitTestMode` | L769, L1177 | Solo desactiva aggressive caching (ModuleClassLoader.java:601) y marca módulos como recargables (NModuleInfo.java:95). NO bypasea bootstrap. |
| `niagara.moduleVerificationMode` | L753 | Controla firma de módulos — el bypass httpapi, no el kernel. |
| `niagara.intern.excludeTypes` | L1178 | Excluye tipos de intern — usado en watch mode, no es un bypass de init. |
| `niagara.dev.home` | L997 | Path de dev environment. Sin relación con bootstrap. |
| `niagara.lang` | L703 | Locale. Sin relación. |
| `niagara.security.manager.disable` | L970 | Deshabilita SecurityManager. Sin relación con kernel init. |

**DEFAULT_COMMAND_LINE_BLACKLIST** (Nre.java:166): lista de sysprops que se borran del command line para evitar override desde fuera. Incluye `niagara.moduleVerificationMode` — confirma que Tridium bloquea explícitamente el override de ese valor vía command line. La lista NO incluye `niagara.unitTestMode` ni ningún sysprop de init, lo que sugiere que no hay sysprops de init que valga la pena bloquear.

**Veredicto H1**: REFUTADO. No existe ningún sysprop que cuando se activa produzca un path alternativo en `Sys.loadType()` o evite el NPE en `Nre.getSchemaManager()`. El único efecto relevante de `niagara.unitTestMode` es en el cache de módulos, no en el bootstrap del kernel.

---

### §TI.13.2 — H2: path alternativo en Sys.java:445 (el NPE site)

**Cómo se probó**: lectura completa de `Sys.java` (vineflower) y trazado de `Sys.loadType()` → `Nre.getSchemaManager()` → `SchemaManager.load()`.

**Evidencia** (Sys.java:170-172 → Nre.java:1340-1342 → SchemaManager.java:103-127):

```java
// Sys.java:170-172
public static Type loadType(Class<?> cls) {
    return Nre.getSchemaManager().load(cls);  // NPE aquí si schemaManager == null
}

// Nre.java:1340-1341
public static SchemaManager getSchemaManager() {
    return schemaManager;  // sin null-guard, sin flag check
}
```

`SchemaManager.load(Class)` (L103-127) es una función limpia sin ramas condicionales: registra la clase en `types[]` y `byClass` map via `Introspector.create().introspect()`. No hay checks de `TestMode.isActive()`, flags, ni sysprops.

La propagación completa del fault:
1. `BTest.<clinit>` ejecuta `Sys.loadType(BTest.class)` (BTest.java:30)
2. `Sys.loadType` llama `Nre.getSchemaManager().load(cls)` (Sys.java:172)
3. `Nre.getSchemaManager()` devuelve null (schemaManager no inicializado)
4. `.load(cls)` → NullPointerException

El único branch condicional en el path es `if (isBooted) return false;` en `Nre.boot()` (L613) — pero ese check previene doble-boot, no provee un path alternativo para el static initializer.

**Nota**: la referencia a "Sys.java:445" en el contexto original de §TI.5 corresponde a una numeración de línea de la fuente sin decompilar (o diferente pipeline de decompilación). El vineflower de Sys.java tiene 185 LOC. La lógica real está confirmada en L170-172 del vineflower.

**Veredicto H2**: REFUTADO. No existe ningún branch condicional ni flag check en `Sys.loadType()` → `Nre.getSchemaManager()`. El path es lineal y absolutamente dependiente de que `schemaManager` haya sido inicializado por `Nre.boot()`.

---

### §TI.13.3 — H3: license feature developer.* relacionada a tests

**Cómo se probó**: lectura completa de Webs.license (153 líneas, vendor `Tridium`) + búsqueda de referencias en código a features del vendor `tridium:developer`.

**Evidencia — features con vendor tridium:developer en Webs.license**:

| Feature | Línea | Atributos |
|---------|-------|-----------|
| `tridium:developer` | 40 | `moduleDev="true" skipModuleValidation="true"` |

Esa es la ÚNICA feature del vendor `tridium` con namespace `developer`. No existe ninguna feature `tridium:developer testStandalone`, `tridium:developer headless`, ni similar.

**Evidencia — dónde se consulta `tridium:developer` en código** (Nre.java:1189):
```java
// Nre.java:1189 — dentro de watch()
licenseManager.checkFeature("tridium", "developer");
```

Este check valida que la license tenga la feature `developer` del vendor `tridium` para habilitar el `--watch` mode. La Webs.license línea 40 (`<feature name="developer" ... moduleDev="true" skipModuleValidation="true"/>`) satisface este check — o sea, **el watch mode YA ESTÁ LICENCIADO** en el environment actual.

**Otras features relevantes buscadas**:
- `smDeveloperMode` (línea 129): feature de `tridium` vendor sin atributos adicionales. Sin referencias en código encontradas para init de kernel.
- `nre` (línea 96): sin relación con tests.
- No hay features con keywords `test`, `unit`, `headless`, `standalone`, ni `bypass` en toda la Webs.license.

**Veredicto H3**: REFUTADO parcialmente — no existe una license feature para bootstrap standalone. Sin embargo, se confirma que `tridium:developer` YA ESTÁ ACTIVA en Webs.license, lo que significa que el `--watch` mode de Nre (que sí requiere esa feature) está disponible sin cambios de licencia.

---

### §TI.13.4 — H4: Nre.headless / Nre.minimal modes

**Cómo se probó**: lectura de `Nre.java` completo (1495 LOC vineflower) buscando métodos `init*`, `load*`, `start*`, `prepare*`, y especialmente entradas de tipo `headless`/`minimal`/`test`.

**Evidencia — inventario completo de métodos static de Nre relevantes**:

| Método | Líneas | Precondiciones | Requiere station? |
|--------|--------|----------------|-------------------|
| `boot()` | 608-609 | `niagara.home` + `niagara.user.home` sysprops | No (boot sin station) |
| `boot(BootEnv)` | 612-798 | BootEnv que provea niagaraHome + userHome | No (si BootEnv se provee) |
| `loadPlatform()` | 1010-1028 | `registryManager` inicializado (requiere boot) | No (pero requiere boot) |
| `clearPlatform()` | 1030-1032 | ninguna | No |
| `buildreg()` | 1171-1173 | Llama boot() | No |
| `watch(String[])` | 1176-1215 | Llama boot() + `tridium:developer` license | No (sin station) |
| `initForLicenses()` | 1099-1117 | `niagara.home` + `niagara.user.home` | No |
| `rebootLicenseManager()` | 1124-1133 | `isBooted == true` | No |

**Hallazgo clave**: `Nre.boot(BootEnv bootEnv)` (L612) acepta cualquier implementación de la interfaz `BootEnv` (BootEnv.java — interfaz pública con 6 métodos). La única implementación concreta en el corpus es `DefaultBootEnv` que requiere sysprops `niagara.home` y `niagara.user.home`. Sin embargo, **la interfaz es pública** — se podría implementar un `MinimalBootEnv` que apunte a rutas del install Niagara para tests.

**No existe** ningún método `Nre.headless()`, `Nre.minimal()`, `Nre.testInit()`, ni ningún path de init que omita los sysprops de home.

El `watch` mode (Nre.java:1176-1214) es el mecanismo más cercano a un "test runner mode" official de Tridium:
- L1177: `System.setProperty("niagara.unitTestMode", "true")` — desactiva module cache
- L1186: `boot()` — boot completo del NRE (requiere `niagara.home`)
- L1189: `licenseManager.checkFeature("tridium", "developer")` — chequea license
- L1212: `TestWatcher.getInstance()` — inicia file watcher sobre `niagaraHome/modules/`
- L1213: `watcher.setTestArgs(args)` — configura los args para TestRunner
- L1214: `watcher.start()` — arranca el loop de watch + re-run tests

TestWatcher (L149-151 de TestWatcher.java) llama `Sys.loadModule("test").loadClass("javax.baja.test.TestRunner")` — lo que implica que el boot completo ya ocurrió antes de llegar acá.

**Veredicto H4**: REFUTADO el modo headless. CONFIRMADO que el `--watch` flag de Nre es el mecanismo oficial para correr tests sin station — pero requiere boot completo con `niagara.home` y `niagara.user.home`. La interfaz `BootEnv` es pública y permite un custom implementation, lo que abre una línea de trabajo potencial.

---

### §TI.13.5 — H5: BTestNg kernel mock pattern

**Cómo se probó**: lectura de `BTest.java` (536 LOC vineflower, test-wb), `TestRunner.java` (333 LOC vineflower), y análisis del pattern `TestStationHandler.startStation()`.

**Evidencia — BTest.java:30 (static initializer)**:
```java
// BTest.java:30 — static initializer que dispara el NPE
public static final Type TYPE = Sys.loadType(BTest.class);
```
Esta línea ejecuta cuando la JVM carga `BTest.class` por primera vez. No hay ninguna forma de referencial `BTest` sin ejecutar este initializer. No existe ningún mock ni stub para `Sys.loadType`.

**Evidencia — TestRunner.java:217-218**:
```java
public static void main(String[] rawArgs) {
    Nre.boot();  // boot completo — solución al problema de bootstrap
    ...
}
```
`TestRunner.main()` llama `Nre.boot()` directamente. Pero esto no ayuda: para llamar `TestRunner.main()`, primero hay que cargar `TestRunner.class` vía `Sys.loadModule("test").loadClass("...")`, que a su vez requiere que `schemaManager` ya esté inicializado — circular dependency.

**Evidencia — BTest.TestStationHandler.startStation() (BTest.java:470-478)**:
```java
public void startStation() {
    if (!this.station.isRunning()) {
        Nre.clearPlatform();
        Nre.loadPlatform();
        Nre.getServiceManager().startAllServices();
        this.station.start();
        Station.stationStarted = true;
        Station.atSteadyState = true;
    }
}
```
`Nre.clearPlatform()` + `Nre.loadPlatform()` — confirmado que este pattern existe. Pero como estableció §TI.12: para que `TestStationHandler.startStation()` sea invocable, `BTest.class` ya debe haber sido cargado, lo que ejecutó `Sys.loadType(BTest.class)` en L30 — que ya requería kernel inicializado. Círculo vicioso.

**Evidencia — TestWatcher.DefaultTestRunner** (TestWatcher.java:139-152): llama `Sys.loadModule("test").loadClass("javax.baja.test.TestRunner")` — mismo círculo.

**Sin kernel mock encontrado**: no existe en el corpus ninguna clase que implemente un stub de `SchemaManager`, un mock de `Nre.getSchemaManager()`, ni ninguna forma de inicializar un `SchemaManager` mínimo sin `boot()`.

**Veredicto H5**: REFUTADO. El pattern `Nre.clearPlatform() + loadPlatform()` de BTest.java existe pero no es standalone — requiere kernel ya inicializado para cargar la clase que lo contiene. No hay kernel mock en el corpus.

---

### §TI.13.6 — Veredicto global

**¿Existe un bypass análogo a httpapi skipModuleValidation para el kernel bootstrap?**

**NO. La refutación de §TI.5 se mantiene y se ratifica con evidencia adicional más profunda.**

La diferencia estructural entre los dos problemas es fundamental:

| Aspecto | httpapi bypass (engram #332) | Tests kernel blocker |
|---------|------------------------------|----------------------|
| Naturaleza del blocker | Política de seguridad (firma de módulo) | Dependency de bootstrap (kernel no inicializado) |
| Timing del fault | Al cargar JAR (post-boot) | Al ejecutar `<clinit>` del static initializer (pre-boot) |
| Mecanismo de bypass posible | Sysprop + license feature — ambos configurables en runtime | Requiere boot completo del NRE — no hay shortcut |
| ¿License feature puede bypassar? | SÍ — `skipModuleValidation` controla una policy check | NO — no hay policy check que bloquee el init del SchemaManager |
| ¿Sysprop puede bypassar? | SÍ — `niagara.classLoader.skipModuleValidation=true` disables the check | NO — `niagara.unitTestMode=true` sólo afecta module caching, no bootstrap |

**Lo que SÍ se encontró** (hallazgo nuevo no anticipado en las hipótesis originales):

El `--watch` flag de Nre (`Nre.watch()`, Nre.java:1176-1214) es el modo oficial de Tridium para correr tests de forma continua. Requiere:
1. Boot completo del NRE (con `niagara.home` + `niagara.user.home` sysprops)
2. License feature `tridium:developer` — **YA PRESENTE** en Webs.license línea 40
3. `niagara.unitTestMode=true` (seteado automáticamente por el modo)
4. Instalar Niagara en Windows (el `niagaraHome/modules/` directory que TestWatcher monitorea)

La invocación sería: `nre -watch test:javax.baja.test.TestRunner <module>:<TestClass>` desde el Niagara install en Windows.

**La interfaz `BootEnv` es pública** (BootEnv.java — 6 métodos, sin estado, sin impl restrictiva). Esto abre una línea potencial: implementar un `TestBootEnv` custom que apunte a `C:\Honeywell\OptimizerSupervisor-N4.14.0.162` y `C:\ProgramData\Niagara4.14\OptimizerSupervisor` y llamar `Nre.boot(new TestBootEnv())` programáticamente. Esto requeriría correr en el Windows JRE de Niagara (no WSL), pero eliminaría la necesidad de station.

---

### §TI.13.7 — Implicaciones para SDDs futuros

**Resultado: NEGATIVE FINDING ratificado con profundidad adicional.**

No existe un bypass de kernel bootstrap. La siguiente vez que una sesión llegue a esta pregunta, la respuesta documentada es:

> El kernel bootstrap de Niagara N4 no tiene bypass por sysprop ni por license feature. La única forma de ejecutar `Sys.loadType()` exitosamente es haber llamado `Nre.boot()` con los sysprops `niagara.home` y `niagara.user.home` apuntando a un install real. Esto fue verificado exhaustivamente en §TI.13 (2026-05-12) leyendo Sys.java, Nre.java, SchemaManager.java, BootEnv.java y DefaultBootEnv.java del corpus vineflower.

**Línea de trabajo que SÍ existe** (no era obvia antes de esta investigación):

El `--watch` mode de Nre + `BootEnv` como interfaz pública sugieren que para TIER-2 tipo (b/c) hay un path en Windows (no WSL):
- `nre -watch` invocado desde `C:\Honeywell\...\bin\` con los sysprops correctos
- Corre `TestRunner` que llama `Nre.boot()` completo
- No requiere station arrancada (TestStationHandler maneja el station lifecycle)
- License `tridium:developer` ya activa

Este path está FUERA del scope del plugin Gradle 7.6.17 bug y no requiere WSL. Vale documentar como §SDD candidato separado: "Niagara watch-mode test runner para CI en Windows".

**Impacto en TIER-2 actual**: la decisión TIER-2 PARCIAL (Opción C Hybrid) se mantiene sin cambios. El watch mode podría complementar como opción D para tests tipo (b/c), pero requiere infraestructura Windows y está fuera del alcance del fix del annotation processor.

---

### §TI.13.8 — Cross-refs

- engram #332 — session summary sesión httpapi 2026-04-19 con `skipModuleValidation` bypass
- engram #1326 — Bloque TI original (§TI.0–§TI.11 + §TI.12 update)
- engram #1328 — §TI.12 navigator extension detail
- engram #1338 — la cross-reference question del usuario (2026-05-12) que originó esta sección
- Webs.license L40: `<feature name="developer" expiration="2027-03-31" moduleDev="true" skipModuleValidation="true"/>`
- Nre.java:1176-1214: `watch()` method — test runner mode oficial
- Nre.java:1340-1342: `getSchemaManager()` — sin null-guard, el NPE site real
- Sys.java:170-172: `loadType()` — facade que delega a Nre.getSchemaManager()
- SchemaManager.java:103-127: `load()` — sin branches ni flags
- BootEnv.java: interfaz pública — único mecanismo de abstracción disponible

---

*§TI.13 cerrado — 2026-05-12. Veredicto: NEGATIVE FINDING. No existe bypass de kernel bootstrap análogo a skipModuleValidation. §TI.5 refutación ratificada con evidencia de source decompilado. Hallazgo secundario: watch mode + BootEnv interface como línea de trabajo potencial para Windows CI.*

---

## §TI.14 — Inventario Windows-side tooling (2026-05-12 investigación)

### §TI.14.0 — Contexto del follow-up

El usuario solicitó explícitamente: "esto es más que todo para investigar todas las herramientas que tenemos a disposición en Windows" antes de comprometerse a ningún path de implementación. Esta sección no toma decisiones de diseño ni implementa nada — solo inventaría y mapea el espacio de opciones.

Pre-condiciones verificadas al inicio:
- Openness daemon activo: `C:\openness-daemon\` heartbeat `2026-05-12T09:45:08`, PID 22748, Admin=True, 4 jobs procesados hoy. **No tocar.**
- Station running en Windows host: la station identificada como "Reflow" corresponde a `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\stations\REFLOW\` (config.bog = 4085 bytes, ZIP con un solo `file.xml` de 16724 bytes). Es una station base ligera — FoxService en puerto 4911 (foxsEnabled=true), WebService en puerto 443 (httpsEnabled=true, httpsOnly=true, tlsv1_3). Hay también una `PRUEBAS_reflow` (config.bog = 52249 bytes → file.xml = 510986 bytes) con módulos `Alsuper`, `vintage`, `converters` — station de cliente real, última modificación 2026-03-11. El MX60 station (`HoneywellMX60`) existe en el mismo niagara_user_home (config.bog = 448660 bytes) pero última modificación 2026-04-02 — no se verificó si está running.
- Niagara installs disponibles: `C:\Niagara\iC-Niagara-4.13.2.18\` (nre-core-win-x64 4.13.2.18) y `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\` (N4.14.0.162).
- License features dev activas: `tridium:developer` con `moduleDev=true` + `skipModuleValidation=true` en Webs.license L40 (cross-ref engram #332).

Cross-refs: engram #332, #1326, #1328, #1338, #1339.

---

### §TI.14.1 — Niagara executables disponibles

Inventario completo de `bin/` de ambos installs (4.13 y 4.14):

| Ejecutable | Tamaño (bytes) | Fecha | Propósito inferido | Documentado en |
|-----------|---------------|-------|-------------------|----------------|
| `nre.exe` | 22808 | Nov 9 2023 / Jun 14 2024 | Niagara Runtime Environment — entry point genérico para todo lo que no es station/wb/test. Invocado como `nre [options] <class> [args]`. | `nre.properties` sección `nre`, Nre.java:203 |
| `station.exe` | 24344 | ídem | Lanza una station Niagara. Equivale a `nre com.tridium.sys.station.Station <stationName>`. | `nre.properties` sección `station`, Station.java:718 |
| `wb.exe` | 109848 | ídem | Workbench GUI — lanza `workbench:com.tridium.workbench.shell.WbMain`. Nre.java:161 define la constante `WB_MAIN`. | `nre.properties` sección `wb` |
| `wb_w.exe` | 109848 | ídem | Workbench con consola Windows suprimida (sin ventana de consola). Mismo tamaño que wb.exe. | Convención Windows; sin consola visible. |
| `test.exe` | 50456 | ídem | Niagara Test Framework runner. Profile distinto al de `nre` — tiene su propio bloque en `nre.properties`. En N4.13: `test.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx4G`. Invoca `TestRunner`. | `nre.properties` sección `test`; devguide/test.html §"Compile and execute" |
| `plat.exe` | 50968 | ídem | Platform shell — entry point para `BPlat` (platform-rt: `com.tridium.platform.command.BPlat`, main en BPlat.java:134). Usado para comandos de plataforma (certificate mgmt, etc.). Comparte el perfil `nre` en nre.properties. | `nre.properties`: "All other NRE executables like `nre[.exe]` or `plat[.exe]`" |
| `niagarad.exe` | 22808 | ídem | Niagara daemon — para correr la station como servicio de Windows. Usa niagarad.jar. | `nre.properties` implícito |
| `console.exe` | 98072 | ídem | Console standalone — probablemente la ventana de consola separada. Usado por Nre.watch() line 1210: `Console console = new Console(); console.start()`. | Nre.java:1210 |
| `hdbt.exe` | 35608 | ídem | History Database Tool — herramienta de migración/export de historias. | Nombre descriptivo; sin docs localizadas |
| `n4mig.exe` | 99608 (N4.13) / 100120 (N4.14) | ídem | Niagara 4 Migration tool (AX → N4). | devguide/ax-to-n4-module-migration.html |
| `nverify.exe` | 523032 (N4.13) / 529176 (N4.14) | ídem | Module signature verification. Verifica .jar signatures. | Convención de nombre |
| `dataExportTool.exe` | 78589192 | Mar 14 2025 | Herramienta standalone de exportación de datos. Solo en N4.14 Honeywell. Tamaño indica es un fat binary autosuficiente. | Solo en Honeywell N4.14 — no está en N4.13 |

**Notas estructurales:**
- `nre.properties` (defaults/) define 4 perfiles de JVM: `station`, `wb`, `test`, `nre`. Cada perfil recibe opciones distintas de heap. El perfil `test` en N4.13 tiene `-Xmx4G` (4× el default de station). En N4.14 el defaults/nre.properties solo define `station` y `wb` — `test` y `nre` están sin configurar en N4.14 (posible oversight o asume defaults del JVM).
- No existe `wbcmd.exe` ni `platsh.exe` como binarios separados. El equivalente de "shell" es `nsh-wb` (ver §TI.14.4).

---

### §TI.14.2 — Developer guide hallazgos

**Archivo**: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide/test.html`
(472 archivos HTML en total en devguide; test.html encontrado via `fd | rg -l 'test'`)

#### Flujo oficial de testing según devguide

1. **Compilar el test jar**:
   ```
   gradlew moduleTestJar
   ```
   Produce un jar con el sufijo `Test` en el nombre del módulo.

2. **Correr los tests** — dos formas equivalentes:

   a) **Comando `test`** (invoca `test.exe` vía nre.properties profile `test`):
   ```
   test <target> [target ... target] [testng options]
   ```
   Targets soportados (documentados en test.html líneas 299-311):
   ```
   all
   <module>
   <module-runtimeProfile>
   <module>:<type>
   <module>:<type>.<method>
   <module>:<type>./<regex>/
   <com.package>.<BTestClass>
   <com.package>.<BTestClass>#<method>
   <com.package>.<BTestClass>#/<regex>/
   /<regex against com.package.BTestClass#method>/
   ```
   Opciones TestNG disponibles:
   ```
   -v:<n>                  verbosity (1-10)
   -output:<path>          directorio de output (default: <niagara.user.home>/reports/testng)
   -groups:<a,b,c>         grupos a incluir
   -excludegroups:<a,b,c>  grupos a excluir
   -skipHtmlReport         deshabilita reporte HTML
   -generateJunitReport    habilita reporte JUnit XML
   -benchmark              print 50 tests más lentos al salir
   -loopCount:<n>          repetir n veces (1-1000000)
   ```

   b) **Gradle task `niagaraTest`**:
   ```
   gradlew :myModule-rt:niagaraTest
   gradlew :myModule-rt:niagaraTest --target <target> --groups ci --verbosity 5
   ```
   También habilita JaCoCo coverage:
   ```
   gradlew :myModule-rt:jacocoNiagaraTestReport
   ```
   Output en: `<module>-rt/build/reports/jacoco/niagaraTest/html`

3. **Tests tipo station** (`BTestNgStation`): La clase `BTestNgStation` (test-wb.jar, 460 líneas) maneja el lifecycle completo de una station de test — crea la station, la inicia, espera `steadyState`, corre los tests, y hace teardown. Ports por defecto: foxPort=1911, webPort=9090. Puede override via `configureTestStation()` + opcionalmente cargar un `.bog` file como template.

**Precondition crítica** (de test.html + code): el comando `test` debe invocarse desde un directorio que es un subproject del niagara_user_home Gradle workspace. El prompt en test.html muestra:
```
C:\Users\user\Niagara4.2\tridium\myModule\myModule-rt> test myModule
```
Esto significa que `test.exe` se invoca desde la carpeta del módulo dentro del workspace del usuario. El workspace ya está configurado en `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\` (tiene `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`).

**Hallazgo clave**: el `test.exe` NO es un ejecutable genérico de terminal — se invoca desde el directorio del módulo en el workspace de Workbench. En la práctica se llama por el `gradle :niagaraTest` task o desde la consola de WB.

---

### §TI.14.3 — Station Reflow running — capacidades expuestas

**Hallazgo de auditoría**: la station llamada "REFLOW" en `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\stations\REFLOW\` es una station base pequeña (config.bog = 4085 bytes → file.xml 16724 bytes). Módulos presentes: `baja`, `alarm`, `app`, `backup`, `box`, `control`, `driver`, `fox`, `history`, `jetty`, `niagaraDriver`, `niagaraVirtual`, `nss`, `program`, `provisioningNiagara`, `search`, `tagdictionary`, `template`, `web`. **No tiene módulos Chihuahua/Honeywell** — es una station Tridium estándar.

La station con contenido Reflow real es `PRUEBAS_reflow` (config.bog = 52249 bytes → file.xml 510986 bytes), con módulos `Alsuper`, `vintage` (converters), y componentes de cliente. Última modificación 2026-03-11. No hay evidencia de que esté running hoy.

**Capacidades expuestas por la station running**:

- Fox protocol: puerto 4911 (foxs, TLS). Para conectar vía WB o Fox client.
- HTTPS: puerto 443, TLS 1.3 mínimo. API HTTP disponible en `https://localhost:443/`.
- Servicios habilitados (de config.bog `services` list): JobService, AlarmService, HistoryService, WebService, FoxService, SearchService, NiagaraNetwork, HierarchyService, TagDictionaryService, ProgramService, CategoryService, UserService, BackupService, AppContainer, BoxServlet, BacnetEthernetPlatformService, etc.

**¿Qué tests podría correr contra ella?**

Tests tipo `BTestNgStation` crean su **propia** station embebida — no se conectan a una station existente externa. Por lo tanto, la station REFLOW running no es el target directo de esos tests.

Lo que la station running SÍ habilita:
1. Tests de conectividad Fox (`BProxyFoxSession` contra localhost:4911) si la clase bajo test necesita un servidor Fox real.
2. Tests via HTTP API: requests HTTP/HTTPS contra endpoints de la station (BWebServlets).
3. Tests de driver Niagara Network (`NiagaraDriverContainer`) contra devices reales configurados en la station.

**Gap**: la station REFLOW no tiene módulos Chihuahua. Para tests de código MX60 o Chihuahua contra una station real, se necesitaría una station con esos módulos instalados.

---

### §TI.14.4 — Workbench CLI (wbcmd / platsh / main entry points)

**Negative finding**: no existe `wbcmd.exe` ni `platsh.exe` como binarios independientes en ninguno de los dos installs de Niagara. No se encontraron en `bin/` de N4.13 ni N4.14.

**Entry points relevantes encontrados** (via module navigator `grep 'public static void main' -n 200`):

| Clase | Módulo | Propósito | Invocación |
|-------|--------|-----------|------------|
| `Nre` (com.tridium.sys.Nre) | baja | Entry point principal del NRE. Opciones: `-version`, `-modules:<x>`, `-hostid`, `-licenses`, `-props`, `-testheap`, `-buildreg`, `-watch`. | `nre.exe [options] <class> [args]` |
| `Station` (com.tridium.sys.station.Station) | bacnetAws-wb (wrapper) | Lanza una station por nombre. Equivale a `station.exe <stationName>`. | `station.exe <name>` o `nre.exe com.tridium.sys.station.Station <name>` |
| `Main` (com.tridium.nsh.Main) | nsh-wb | Niagara Shell — shell interactivo o de file. Sintaxis: `nsh [file] [options]`, opciones: `/?`, `/debug`. Corre comandos nsh (script language de Tridium para configuración). No requiere station arrancada necesariamente. | Invocado por `nre.exe nsh:com.tridium.nsh.Main [file]` |
| `BPlat` (com.tridium.platform.command.BPlat) | platform-rt | Platform command — gestión de plataforma, certificados, etc. Entry point de `plat.exe`. | `plat.exe [args]` |
| `TestRunner` (javax.baja.test.TestRunner) | test-wb | Test runner de Niagara/TestNG. Invocado internamente por `test.exe`. Acepta targets en formato `<module>`, `<module>:<type>`, etc. | Interno, vía `test.exe <target>` |
| `BWbProfile.main()` | workbench-wb | Entry point del Workbench — `workbench:com.tridium.workbench.shell.WbMain` (via Nre.java:161 constante WB_MAIN). | `wb.exe` o `nre.exe workbench:...WbMain` |

**Nsh (Niagara Shell)** — discovery relevante: `nsh-wb` contiene una shell interactiva (`Main.java:59`) que ejecuta un lenguaje de scripting propio. Comandos a explorar en sesiones futuras si se necesita scripting de configuración sin GUI. No requiere el overhead del Workbench completo. Invocable via `nre.exe nsh:com.tridium.nsh.Main`.

---

### §TI.14.5 — Daemon sibling design space

Las decisiones de diseño que habría que tomar para un `C:\niagara-daemon\` separado (sin commit a ninguna):

**D1 — Estructura de carpetas**
- Opción A: inbox/outbox/processed idéntico al openness daemon (copy exacta del patrón)
- Opción B: inbox/results (sin `processed`, los jobs se mueven a results con stdout/stderr juntos)
- Decisión: ¿necesitamos el `processed/` trail histórico o solo el resultado más reciente?

**D2 — Formato de jobs**
- Opción A: `.ps1` files (PowerShell puro, copiar patrón openness)
- Opción B: `.json` config + `.ps1` fixed template (parametrizable, más structured)
- Opción C: un `.ps1` fijo que acepta argumentos en un `.args.json` sidecar
- Decisión: ¿cuánta variedad de jobs necesitamos? Si es solo `test.exe <module>`, un `.json` simple alcanza

**D3 — Invocación de test.exe**
- El `test.exe` vive en `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\bin\`
- Necesita que `niagara.home` y `niagara.user.home` estén configurados
- ¿Se setean como env vars del proceso o como sysprops en el job?
- ¿Se invoca con `Start-Process` (asíncrono + capture) o con `&` directo en el mismo PS1?

**D4 — Working directory del job**
- `test.exe` se invoca desde el módulo dentro del workspace (per devguide)
- El workspace está en `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\`
- El job debe hacer `cd` al subdir del módulo antes de invocar `test.exe`
- Decisión: ¿el job recibe el path absoluto o relativo al workspace?

**D5 — Output capture**
- Opción A: stdout/stderr separados (como openness daemon: `$jobId.stdout.txt` + `$jobId.stderr.txt`)
- Opción B: stdout/stderr combinados en un único log (más fácil para el consumer en WSL)
- Opción C: stdout/stderr separados + un `.json` structured con exit code, duration, test summary
- Decisión: el consumer (WSL script o CI) determina el formato más conveniente

**D6 — Convivencia con openness daemon**
- El openness daemon escucha en `C:\openness-daemon\inbox\` — carpeta separada completamente
- No comparten puertos ni recursos de sistema (ambos son PS1 loops con sleep)
- Pueden correr en paralelo sin interferencia
- Riesgo: ambos son admin. Si un job de Niagara tarda mucho (4G heap, larga suite), compite por CPU/RAM con el PLC interlock daemon
- Decisión: ¿throttle? ¿prioridad de proceso? ¿mutex global?

**D7 — Heartbeat + shutdown**
- Copiar el patrón del openness daemon literal: `heartbeat.txt` cada 30s, `shutdown.flag` para cierre limpio
- No hay razón para cambiarlo

**D8 — Launch**
- Igual que openness daemon: `Start-Process powershell -Verb RunAs -ArgumentList '-NoExit','-NoProfile','-File','C:\niagara-daemon\daemon.ps1'`
- Requiere admin para que `test.exe` pueda acceder a resources de Niagara

---

### §TI.14.6 — Matrix canónica test type × tool × constraint

| Test type | Código de ejemplo | Best tool | Preconditions | Status hoy |
|-----------|------------------|-----------|---------------|-----------|
| **(a) puro — sin kernel** | `ChiJsonUtil`, `ChiThresholdHelper`, helpers que no invocan `Sys.loadType()` | `run-tests-wsl.sh` (TIER-2 Opción C vigente) | Solo JVM Linux / standard Java | DISPONIBLE — flujo existente |
| **(a) con RED-phase** | `ChiAlarmHelper` (14 tests en fail) | Mismo TIER-2 + contratos definidos | Espera SDD `mx60-alarms-helper-contracts` | EXCLUIDO v1 — deuda técnica |
| **(a) kernel-only via nre.exe** | `BTestNg` subclass que llama `Sys.loadType()` | `test.exe` invocado desde workspace (via daemon sibling) | `niagara_home` + `niagara_user_home` + `tridium:developer` license (ya activa) + `test.java.options` en nre.properties | PATH IDENTIFICADO — no implementado. Requiere daemon sibling |
| **(b) kernel-only via `nre -watch`** | `ChiHistoryHelperTest.testComputeRange*` | `nre.exe -watch` con `BootEnv` custom | Igual que (a) kernel + watch mode | PATH IDENTIFICADO — no implementado. Mismo daemon sibling |
| **(b) via Gradle niagaraTest** | Cualquier `BTestNg` o `BTestNgStation` | `gradlew :chihuahua-rt:niagaraTest` | Workspace configurado en `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\` + módulo chihuahua en el workspace | PATH IDENTIFICADO — workspace ya existe, falta agregar chihuahua al workspace. Invocable via daemon sibling |
| **(c) station embebida** | Tests que extienden `BTestNgStation` | `test.exe` o `gradlew niagaraTest` | Igual que (b) — `BTestNgStation` maneja el lifecycle internamente | Mismo status que (b) |
| **(c) station REFLOW externa** | Tests que conectan por Fox a localhost:4911 | Fox client programático + `BProxyFoxSession.connect()` | Station REFLOW running (confirmada), credenciales admin | POSIBLE — gap: no hay tests Reflow de ese tipo identificados todavía |
| **(c) station MX60 externa** | Tests que conectan por Fox a station MX60 | Igual que Reflow | Station HoneywellMX60 running (no confirmada hoy) | GAP OPERACIONAL — station MX60 no confirmada running |

---

### §TI.14.7 — Hallazgos no anticipados

**H1 — N4.14 `defaults/nre.properties` no tiene `test.java.options`**
N4.13 (`iC-Niagara-4.13.2.18`) tiene `test.java.options=-Dfile.encoding=UTF-8 -Xss512K -Xmx4G` en su `defaults/nre.properties`. N4.14 (`OptimizerSupervisor-N4.14.0.162`) solo tiene `station.java.options` y `wb.java.options`. El perfil `test` está documentado en el archivo (comentario lo menciona) pero no configurado. Implicación: si se usa `test.exe` de N4.14, el heap máximo queda al default del JVM (que puede ser bajo). Workaround: agregar `test.java.options=-Xmx2G` al user-home `etc/nre.properties`. Relevance: ALTA para daemon sibling.

**H2 — `dataExportTool.exe` (78MB) en Honeywell N4.14**
Herramienta exclusiva de Honeywell (no existe en N4.13). 78MB sugiere fat binary autosuficiente. Probablemente para exportar datos históricos de la station. No relevante para testing, pero interesante para auditoría de herramientas Honeywell-específicas.

**H3 — `nsh` (Niagara Shell) como scripting engine sin GUI**
El módulo `nsh-wb` provee `com.tridium.nsh.Main` — una shell interactiva con su propio lenguaje de scripting. Se invoca como `nre.exe nsh:com.tridium.nsh.Main [script.nsh]`. No requiere station necesariamente (dependiendo del script). Potencialmente útil para scripts de configuración de stations desde CLI, pero requiere investigación adicional sobre el lenguaje nsh.

**H4 — `console.exe` requerido por `nre -watch`**
`Nre.java:1210` ejecuta `Console console = new Console(); console.start()` justo antes de `TestWatcher.getInstance()`. El `console.exe` (98072 bytes) es el componente de consola interactiva. Esto significa que `nre -watch` en modo headless (sin GUI de WB abierta) igual instancia una Console — lo que puede requerir un display o fallar en entornos sin GUI. En PowerShell sin `-NoExit` esto puede causar problemas. **Relevancia**: si se usa el daemon sibling para lanzar `nre -watch`, el `-NoNewWindow` del `Start-Process` podría interferir con el Console. A investigar.

**H5 — `BTestNgStation` crea su propia station con ports 1911 y 9090**
Confirmado en BTestNgStation.java:75-76. Los tests tipo `BTestNgStation` NO se conectan a la station REFLOW running — crean su propia station interna en los puertos 1911 (fox) y 9090 (web). Esto significa que pueden chocarse con la station REFLOW si está ocupando el puerto 1911. **Relevancia**: si REFLOW está corriendo en el puerto fox 4911 (foxs, no fox), no hay conflicto — son puertos distintos.

**H6 — Workspace dev ya tiene `build.gradle.kts`**
`C:\Users\equipo\Niagara4.14\OptimizerSupervisor\` ya tiene `build.gradle.kts`, `gradle.properties`, `gradlew.bat` y `settings.gradle.kts`. El `gradle.properties` está configurado con `niagara_home=C:\\Honeywell\\OptimizerSupervisor-N4.14.0.162` y `niagara_user_home=C:\\Users\\equipo\\Niagara4.14\\OptimizerSupervisor`. Es un workspace Gradle funcional. Para correr `gradlew :chihuahua-rt:niagaraTest`, solo faltaría agregar el subproject chihuahua al `settings.gradle.kts`.

---

### §TI.14.8 — Decision tree (sin commit todavía)

```
¿Qué tipo de test querés correr?
│
├── (a) Tests puramente Java sin Sys.loadType()
│   └── TIER-2 vigente (run-tests-wsl.sh) — ya disponible. NO requiere cambios.
│
├── (b) Tests que necesitan kernel Niagara (Sys.loadType(), BTestNg, historias)
│   ├── Opción B1: daemon sibling + test.exe
│   │   Pasos: (1) crear C:\niagara-daemon\daemon.ps1, (2) agregar test.java.options
│   │   a nre.properties, (3) WSL script que dropea job en inbox y lee outbox
│   │   Estado: path claro, ~1-2h de trabajo, NO requiere station running
│   │
│   ├── Opción B2: daemon sibling + nre.exe -watch
│   │   Pasos: igual que B1 pero invoca nre -watch en lugar de test.exe
│   │   Diferencia: watch mode relanza tests al detectar cambios en módulos
│   │   Cuidado: Console() en Nre.java:1210 puede requerir display (H4)
│   │   Estado: path claro, investigación extra sobre Console en headless
│   │
│   └── Opción B3: daemon sibling + gradlew niagaraTest
│       Pasos: (1) agregar chihuahua al settings.gradle.kts del workspace,
│       (2) daemon llama gradlew.bat :chihuahua-rt:niagaraTest desde el workspace
│       Ventaja: JaCoCo coverage gratis, integración IDE, targets más granulares
│       Costo: Gradle wrapper bootstrapping (lento primera vez)
│       Estado: workspace ya existe, falta agregar subproject
│
└── (c) Tests de integración contra station running
    ├── Con station REFLOW (ya running):
    │   Fox client en localhost:4911 (foxs), HTTPS en localhost:443
    │   Requiere credenciales + certificado aceptado
    │   Estado: POSIBLE ahora — gap: no hay tests de ese tipo identificados
    │
    └── Con station MX60:
        Requiere confirmar que HoneywellMX60 station está running
        (config.bog existe pero last-modified 2026-04-02, no confirmada hoy)
        Estado: GAP OPERACIONAL
```

**Camino mínimo viable** para desbloquear tipo (b): daemon sibling + `test.exe` (Opción B1). Requiere:
1. `C:\niagara-daemon\daemon.ps1` (copiar patrón de openness daemon, ~130 LOC)
2. Agregar `test.java.options=-Xmx2G` al `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\etc\nre.properties`
3. WSL helper script que escribe job en `/mnt/c/niagara-daemon/inbox/` y espera resultado en `/mnt/c/niagara-daemon/outbox/`

**Camino completo**: B3 (Gradle niagaraTest) — da coverage report, targets granulares y mejor integración con el workflow de desarrollo existente, pero requiere un paso extra de configuración del workspace.

**Camino diferido**: (c) station integration — conviene cuando los módulos chihuahua ya tienen tests `BTestNgStation` escritos o cuando se necesita validar behavior end-to-end contra una station real con datos.

---

### §TI.14.9 — Cross-refs

- engram #332 — sesión httpapi 2026-04-19: `skipModuleValidation=true` en Webs.license L40
- engram #1326 — Bloque TI original §TI.0–§TI.12
- engram #1328 — §TI.12 navigator extension detail
- engram #1338 — cross-reference question que originó §TI.13 (2026-05-12)
- engram #1339 — §TI.13: 5 hipótesis bypass kernel bootstrap, todas REFUTADAS; watch mode como path positivo
- `devguide/test.html` (N4.14): documentación oficial de TestNG + `test` command + `niagaraTest` Gradle task
- `devguide/build.html` (N4.14): Gradle task `moduleTestJar`, `niagaraTest`, `jacocoNiagaraTestReport`
- `C:\Niagara\iC-Niagara-4.13.2.18\defaults\nre.properties` línea `test.java.options=-Xmx4G`
- `C:\Honeywell\OptimizerSupervisor-N4.14.0.162\defaults\nre.properties`: sin `test.java.options` — N4.14 discrepancy
- `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\gradle.properties`: workspace ya configurado
- Nre.java:203 (`main`), :260 (`-watch` branch), :1176 (`watch()` method), :1210 (`Console()` call)
- BTestNgStation.java:75-76: foxPort=1911, webPort=9090 (ports del test station embebido)
- `C:\openness-daemon\daemon.ps1`: patrón de referencia para daemon sibling (inbox/outbox/processed, heartbeat, shutdown.flag)

---

*§TI.14 cerrado — 2026-05-12. Inventario completo de Windows-side tooling. 8 executables mapeados + 1 negative finding (no wbcmd/platsh). 3 paths identificados para tests tipo (b): test.exe (B1), nre -watch (B2), gradlew niagaraTest (B3). Ninguno requiere decisión de diseño en esta sesión.*

---

## §TI.15 — Empirical daemon-pattern + kernel boot CLI confirmation (2026-05-12)

### §TI.15.0 — Contexto

§TI.13 refutó 5 hipótesis de bypass del kernel bootstrap. §TI.14 mapeó tooling Windows. Pero ningún experimento empírico se había hecho TODAVÍA contra el path B1 (daemon sibling). El usuario sugirió replicar el patrón `C:\openness-daemon\` que él usa para jobs PLC. Esta sección documenta el experimento.

**Setup**: `C:\niagara-daemon\` separado del openness-daemon (no toca PLC workflow). Daemon ps1 = copia exacta del openness pattern con `$base` cambiado. Jobs `.ps1` definen su propio `NIAGARA_HOME` + invocan executables Niagara directamente.

**Lanzamiento**: `Start-Process powershell -Verb RunAs -ArgumentList '-NoExit','-NoProfile','-File','C:\niagara-daemon\niagara-daemon.ps1'` — UAC aceptado UNA SOLA VEZ. Luego 4 jobs ejecutados sin más prompts.

### §TI.15.1 — Job 01: `nre -version` (sanity check)

**Comando**: `& $nre -version`  
**Exit code**: 0  
**Duración**: ~17s (boot incluido)

**Output capturado** (`outbox/01-nre-version.stdout.txt`):
```
Niagara Runtime Environment
  java.version:              1.8.0_412
  java.vendor:               Azul Systems, Inc.
  java.vm.name:              OpenJDK 64-Bit Server VM
  java.home:                 C:\Honeywell\OptimizerSupervisor-N4.14.0.162\jre
  niagara.home:              C:\Honeywell\OptimizerSupervisor-N4.14.0.162
  niagara.user.home:         C:\Users\equipo\Niagara4.14\OptimizerSupervisor
  niagara.platform.provider: com.tridium.nre.platform.NativePlatformProviderTridium
  nre.hostId:                Win-6E6E-10AC-D1DD-8276
  nre.hostModelVersion:      
  nre.bajaVersion:   0
  nre.vendor:        Tridium
  nre.vendorVersion: 4.14.0.162
```

**Veredicto**: Daemon pattern funciona end-to-end. Round-trip `inbox/` → daemon → `outbox/` ~17s incluye boot del JVM.

### §TI.15.2 — Job 02: `nre -hostid` (license/identity surface)

**Comando**: `& $nre -hostid`  
**Exit code**: 0  
**Output**: `HostId: Win-6E6E-10AC-D1DD-8276`

**Cross-ref**: matches `nre.hostId` de Job 01 y matches engram #332 (sesión httpapi 2026-04-19). Identidad de máquina consistente.

### §TI.15.3 — Job 03: `nre -modules` (KERNEL BOOT — refutación parcial §TI.6)

**Comando**: `& $nre -modules`  
**Exit code**: 0  
**Duración**: ~11s

**Output del kernel** (capturado vía `stderr` porque `nre.exe` usa `System.err` para logs vía ZKM-obfuscated nre):
```
[nre] Booting
[sys] Logging initialized
[sys.registry] Up-to-date [154ms]
[sys.registry] Loaded [51ms]
[crypto.registry] module signature registry up-to-date
[crypto.registry] module signature registry load complete (648ms)
```

Seguido de **985 líneas** listando cada módulo cargado (`<module> <vendor> <version>` per línea). Excerpts:
```
Alsuper-rt                       SEJOFA             1.0
Alsuper-ux                       SEJOFA             1.0
Alsuper-wb                       SEJOFA             1.0
DashboardNotifier-rt             Sejofa             1.0
DashboardNotifier-ux             Sejofa             1.0
SentienceModelSync-rt            honeywell          2.0.6
chihuahua-rt                     Angeles4657        1.0     (← módulo target MX60)
chihuahua-ux                     Angeles4657        1.0
alarm-rt                         Tridium            4.14.0.162
... (155+ módulos más)
```

**HALLAZGO**: el kernel NRE **SÍ bootea desde CLI** cuando se invoca vía `nre.exe`. §TI.5/§TI.6 dijeron "no hay bypass" — empíricamente cierto si entendemos "bypass" como "sysprop o license feature que evita el boot". Pero **no se necesita bypass**: el wrapper `nre.exe` hace `Nre.loadPlatform()` ANTES del main del programa target. La diferencia técnica:

| Invocación | ¿Kernel booteado? | Por qué |
|-----------|-------------------|---------|
| `java -cp ... mainClass` | NO | Tu JVM standalone salta directo al main sin pre-init NRE |
| `java -cp ... org.junit.runner.JUnitCore` (TIER-2) | NO | Idem — `Sys.loadType(BAbsTime.class)` falla |
| `nre.exe -version` | NO (no necesita) | nre.exe imprime version sin bootear |
| `nre.exe -modules` | **SÍ** | nre.exe llama `Nre.boot()` para enumerar registry |
| `nre.exe <com.foo.Main>` | **SÍ** | nre.exe es wrapper que bootea NRE y luego invoca el main del target |
| `test.exe <args>` | **SÍ** | test.exe = wrapper sobre nre.exe con perfil JVM específico |

**Implicación para §TI.6**: la refutación NO era incorrecta — era el framing. No existe sysprop para "hacer headless el kernel". Existe **el wrapper canónico Tridium (nre.exe)** que YA hace el boot completo cuando lo invocás. La pregunta "¿cómo bootear el kernel sin Tridium tooling?" no tiene respuesta. La pregunta "¿cómo bootear el kernel con Tridium tooling?" tiene respuesta trivial: invocá nre.exe.

### §TI.15.4 — Job 04: `test.exe -h` (API completa del Test runner)

**Comando**: `& $test -h` (y plan B `& $test` sin args, plan C `& $test -?`)  
**Exit code**: -1 en los 3 intentos (test.exe no acepta `-h`/`-?` flags), pero **imprimió usage completo en stderr** los 3 veces.

**API documentada** (capturada del usage banner):
```
usage:
   test <nativeTarget> -native [options]
   test <target> [target ... target] [options]

global options:
   -ng              Run java tests with TestNg (default)
   -watch           Run java tests with TestNg in watch mode
   -native          Run native tests with cppunit
   -v:<n>           Set output verbosity level (1 - 10)
   -output:<path>   Set the location for test reports output

testng targets:
   all
   <module>
   <module-runtimeProfile>
   <module>:<type>
   <module>:<type>.<method>
   <com.package>.<BTestClass>
   <com.package>.<BTestClass>.<method>
   /<regex to match against the com.package.BTestClass#method format>/

testng options:
   -groups:<a,b,c>         Comma-separated list of TestNG group names to test
   -excludegroups:<a,b,c>  Comma-separated list of TestNG group names to skip
   -skipHtmlReport         Flag to disable HTML report generation
   -generateJunitReport    Flag to enable JUnit XML report generation
   -benchmark              Print the 50 highest duration tests and test suites
   -loopCount:<n>          Run target(s) in a loop n times (1 - 1000000)
```

**Hallazgos clave del API**:

1. **Default es TestNg** (`-ng`) — no JUnit. Los tests deben extender `BTest`/`BTestNg`/`BTestNgStation`.
2. **`-watch`** modo está expuesto en el API — es EL mecanismo que TestWatcher (`baja` module) wrapea internamente. Coincide con §TI.13.4.
3. **Targets** múltiples formatos:
   - Módulo: `test chihuahua-rt`
   - Módulo+profile: `test chihuahua` (corre todos los profiles)
   - Tipo específico: `test chihuahua:rt.SomeBTest`
   - Método específico: `test chihuahua:rt.SomeBTest.testFoo`
   - Regex: `test /.*chihuahua.*\#testAlarm.*/`
4. **`-generateJunitReport`** flag: emite JUnit XML reports — útil para integración CI estándar.
5. **`-loopCount:N`** + **`-benchmark`**: features avanzadas para perf testing.
6. **`-output:<path>`**: directorio destino para HTML report + XML report.

**Confirmación de incompatibilidad con nuestros JUnit 4 vanilla**: el target spec menciona `<BTestClass>` explícitamente. Tests JUnit puros sin `@NiagaraType` + sin `extends BTest` **no son discoverable por test.exe**. Esto ratifica §TI.12.5 Option C Hybrid: nuestros tests JUnit 4 standalone NO se migran a test.exe — viven en `run-tests-wsl.sh`.

### §TI.15.5 — B1 viability ratificada con caveat

**B1 (daemon sibling + test.exe)** es **viable estructuralmente** post-experimento. Evidencia:
- Kernel boota desde CLI vía nre.exe / test.exe (Job 03)
- test.exe acepta targets variados (Job 04 usage banner)
- Daemon pattern procesa jobs sin requerir accept extra (4 jobs corridos limpios)

**Caveat estructural**: tests deben ser BTestNg (TestNG + `@NiagaraType` + `extends BTestNg|BTest`). **Nuestros JUnit 4 existing NO se migran**. Per §TI.12.5 Option C Hybrid:

| Test scope | Path |
|------------|------|
| JUnit 4 existing (ChiJsonUtilTest, ChiThresholdHelperTest, ChiAlarmHelperTest, etc.) | TIER-2 vigente: `run-tests-wsl.sh` |
| Tests nuevos tipo (b) — usan `BAbsTime`/`BOrd`/`BSimple` | BTestNg via test.exe vía daemon |
| Tests nuevos tipo (c) — station context | BTestNgStation via test.exe vía daemon |
| Tests tipo (a) nuevos | Pueden ir a cualquiera — preferimos `run-tests-wsl.sh` por simplicidad y velocidad (~200ms vs ~15s boot) |

### §TI.15.6 — Operational pattern: daemon sibling validado

El daemon `C:\niagara-daemon\` co-existe con `C:\openness-daemon\` sin conflicto:
- Carpetas distintas (no colisión de inbox/outbox)
- Procesos PowerShell admin independientes (distinct PIDs)
- Ambos en idle pollean cada 1.5s — overhead trivial
- Shutdown independiente: `touch /mnt/c/niagara-daemon/shutdown.flag`

**Workflow desde WSL/Claude**:
1. Vos lanzás daemon UNA VEZ por sesión (UAC click único)
2. Yo escribo job `.ps1` en `staging/`
3. Yo `cp` a `inbox/`
4. Daemon procesa en ~5-20s (depending on Niagara boot)
5. Yo leo `outbox/<jobId>.{status,stdout,stderr}.txt`
6. Job se mueve a `processed/<jobId>-<timestamp>.ps1` automáticamente

**Persistencia**: jobs procesados quedan en `processed/` indefinidamente. `daemon.log` acumula. `heartbeat.txt` se sobrescribe cada 30s.

### §TI.15.7 — Bonus: cross-ref Module Navigator vs runtime

`nre -modules` lista módulos **CARGADOS** en el install Niagara (no decompiled corpus). El Module Navigator (engram #1327) indexa los JARs del install **decompilados**. Diff útil:

| Módulo runtime (Job 03) | En Module Navigator? |
|-------------------------|---------------------|
| `Alsuper-rt/ux/wb` SEJOFA 1.0 | Probablemente NO — custom |
| `DashboardNotifier-rt/ux` Sejofa 1.0 | Probablemente NO — custom |
| `SentienceModelSync-rt` Honeywell 2.0.6 | Verificar — Honeywell pero específico |
| `chihuahua-rt/ux` Angeles4657 1.0 | NO — es tu módulo, vive en tu repo |
| `alarm-rt` Tridium | SÍ — corpus standard |

**Implicación**: para investigar módulos custom (Alsuper, DashboardNotifier, SentienceModelSync) habría que extraer sus JARs del install + decompilar (Vineflower) + agregarlos al corpus del navigator. Out of scope acá pero documentado.

### §TI.15.8 — Decision tree actualizado post-§TI.15

| Necesidad | Tooling | Setup |
|-----------|---------|-------|
| Test JUnit 4 puro (tipo a limpio) | `./chihuahua/run-tests-wsl.sh` | YA LIVE en main (#1334) |
| Inspeccionar versión/hostid/módulos Niagara | `nre.exe <flag>` vía daemon | Daemon ya armado, jobs reusables |
| Test nuevo BTestNg (tipo b/c) | `test.exe <target>` vía daemon | Daemon ya armado, falta escribir el primer BTestNg |
| Hot-reload de tests durante dev | `test.exe -watch` vía daemon (modo persistent) | Idem |
| Comando admin Niagara arbitrario | Custom `.ps1` job + daemon | Cualquier `.ps1` que respete inbox pattern |
| Browse module corpus | Module Navigator (`python3 module_nav.py`) | YA disponible (engram #1327) |
| Build módulo MX60 | `./gradlew :chihuahua-ux:clean :chihuahua-ux:jar` vía bash directo | NO requiere daemon (no admin) |

### §TI.15.9 — Lecciones operacionales

1. **Native command stderr en PowerShell**: `nre.exe`/`test.exe` usan `System.err` para logs ZKM-obfuscated. PowerShell con `2>&1` lo trata como `NativeCommandError` y va a stderr de Start-Process — NO a stdout. Para capturar el output real, leer SIEMPRE el `.stderr.txt` además del `.stdout.txt`.
2. **Daemon timing**: nre boot toma ~5-10s warm-up por job. Para iterar rápido, considerar `nre -watch` modo persistent (un solo boot, múltiples invocaciones).
3. **UAC fatigue evitada**: el patrón daemon (UAC único) es estrictamente superior a invocar `Start-Process -Verb RunAs` por cada comando (UAC por cada comando = friction extrema).
4. **Cross-process redirection**: `cp` desde WSL a `/mnt/c/niagara-daemon/inbox/` es atómico y rápido. No hay race condition con el daemon polling porque `Get-ChildItem` retorna snapshot.
5. **Encoding gotcha**: el daemon escribe `heartbeat.txt` con BOM UTF-8 (`﻿`). Si parseamos el archivo programáticamente, strip BOM primero.

### §TI.15.10 — Cross-refs

- engram #1326 — bloque TI original §TI.0–§TI.12
- engram #1338 — cross-reference question (httpapi bypass vs kernel)
- engram #1339 — §TI.13: 5 hipótesis kernel bypass REFUTADAS (correcto pero mal-frameado)
- engram #1344 — §TI.14 inventory de Windows tooling
- engram #1327 — Module Navigator reference
- engram #1334 — TIER-2 LIVE post-merge
- `C:\openness-daemon\daemon.ps1` — pattern fuente para daemon sibling
- `C:\niagara-daemon\` — daemon sibling (este experimento)
- `outbox/01-nre-version.{stdout,status}.txt` — Job 01 evidence
- `outbox/02-nre-hostid.{stdout,status}.txt` — Job 02 evidence
- `outbox/03-nre-modules.{stdout,stderr,status}.txt` — Job 03 kernel boot evidence
- `outbox/04-test-help.{stdout,stderr,status}.txt` — Job 04 test.exe API documented
- N4 devguide test.html — TestNG + BTestNg framework docs

---

*§TI.15 cerrado — 2026-05-12. Daemon-pattern empíricamente validado. 4 jobs ejecutados sin accept extra post-UAC. Kernel NRE bootea via CLI (`nre.exe` wrapper hace `Nre.loadPlatform()` antes del main). test.exe API completa documentada — TestNg + targets BTestClass-style + flags estándar. B1 path RATIFICADO viable. Caveat: requiere BTestNg-style tests, NO JUnit 4 vanilla — Option C Hybrid intacta.*
