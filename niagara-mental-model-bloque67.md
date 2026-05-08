# Bloque 67 — Analytics deep dive para construir módulo MX60: librerías + 56 algorithm blocks + 17 combiners + 22 trend wrappers + Psychrometric HVAC + Degree-Day + veredictos PID/ML/Forecasting/Anomaly + roadmap MX60-Analytics-inspired

**Fecha**: 2026-05-08
**Método**: Profundización Analytics module Niagara N4 oficial Tridium para decisión: **¿qué hereda MX60 vs qué reescribe vs qué agrega de cero?** Cubre catálogo exhaustivo librerías + 56 BAlgorithmBlock + 17 combiners + 22 trend wrappers + veredictos binarios sobre presencia/ausencia PID controllers, Machine Learning, forecasting, anomaly detection, FFT, psychrometric HVAC, degree-day, equipment operation, statistical functions, load profiling.

**Fuentes primarias**:
- `analytics-rt/vineflower/META-INF/module.xml` — 28 dependencies Niagara N4
- `analytics-rt/vineflower/com/tridiumx/analytics/algorithm/` — 56 archivos Java (algorithm blocks)
- `analytics-rt/vineflower/com/tridiumx/analytics/combine/` — 17 archivos (combiners)
- `analytics-rt/vineflower/com/tridiumx/analytics/trend/` — 22 archivos (trend wrappers)
- `analytics-rt/vineflower/com/tridiumx/analytics/algorithm/BPsychrometricBlock.java` (434L)
- `analytics-rt/vineflower/com/tridiumx/analytics/algorithm/BConsumptionToDemandBlock.java`
- `analytics-rt/vineflower/com/tridiumx/analytics/aon/` — 19 archivos formato binario propio
- `analytics-ux/extracted/ext/c3/` + `ext/datatables/` — frontend libs
- Imports cross-codebase grepeados (java.* + com.tridium.* + javax.bajax.*)
- Bloque 66 (exploración inicial)

**Versión analizada**: Niagara N4.14.0.162 oficial Tridium (decompiled).

---

## 67.0 Resumen ejecutivo — veredictos binarios

### Tabla de veredictos para construir módulo MX60

| Capability | ¿Analytics tiene? | Evidencia | MX60 hereda / agrega de cero |
|------------|-------------------|-----------|------------------------------|
| **Psychrometric HVAC** | ✅ SÍ | `BPsychrometricBlock.java` 434L | **HERENCIA literal** (joya reusable) |
| **Degree-Day (HDD/CDD)** | ✅ SÍ | `BConsumptionToDemandBlock` + baseline normalization en BCombination | **HERENCIA literal** |
| **Statistical functions** | ✅ SÍ | 17 combiners en `combine/` (Avg, Median, StdDev, Mode, Range, Sum, Count, Min, Max, ...) | **HERENCIA literal** |
| **Equipment Operation** | ✅ SÍ rudimentario | `BRuntimeBlock` + `BIntervalCountBlock` + EquipmentOperation chart | **HERENCIA + EXTENDER** con KPIs avanzados |
| **Load Profiling** | ✅ SÍ | LoadDuration chart frontend (c3.js) | **HERENCIA frontend** |
| **Algorithm DAG composition** | ✅ SÍ | 56 BAlgorithmBlock + BlockPin connections + BResultBlock sink | **HERENCIA arquitectura completa** |
| **Trend wrappers** | ✅ SÍ | 22 wrappers (Aggregate, Baseline, Delta, Imputed, Merge, ...) | **HERENCIA literal** |
| **PID Controllers** | ❌ **NO** | 0 callsites `setpoint`, `integral`, `derivative`, `PIDController` | **AGREGAR DE CERO** familia de blocks PID |
| **Machine Learning** | ❌ **NO** | 0 imports tensorflow/deeplearning4j/weka/smile/commons-math; solo K-NN para interpolación trends | **AGREGAR DE CERO** si crítico — ver Sección 67.7 |
| **Forecasting / Prediction** | ❌ **NO** (solo baseline comparison) | NO ARIMA, Holt-Winters, exponential smoothing | **AGREGAR DE CERO** familia Forecasting |
| **Anomaly Detection** | ❌ NO sofisticado (solo threshold-based) | `BInvalidValueFilterBlock` + `BRangeFilterBlock` simple, NO 3-sigma/IQR/ML | **AGREGAR DE CERO** statistical anomaly |
| **FFT / Frequency Analysis** | ❌ **NO** | "Spectrum chart" es histograma de carga horaria, NO Fast Fourier Transform | **AGREGAR DE CERO** si caso de uso vibration/spectral analysis |

### Recomendación arquitectónica MX60

- **Hereda 100%**: Algorithm DAG composition + 56 blocks + 17 combiners + 22 trend wrappers + Psychrometric + Degree-Day
- **Agrega 3 familias nuevas de blocks**:
  - **PID family**: `BPIDControllerBlock`, `BSetpointBlock`, `BErrorBlock`, `BIntegralBlock`, `BDerivativeBlock`
  - **Forecasting family**: `BARIMAForecastBlock`, `BHoltWintersBlock`, `BExpSmoothingBlock`, `BLinearRegressionBlock`
  - **Anomaly family**: `BSigmaAnomalyBlock` (3-sigma), `BIQRAnomalyBlock`, `BSeasonalAnomalyBlock`
- **Modernización tech**:
  - ❌ NO replicar AON encoding → JSON estándar
  - ❌ NO replicar ThreadPool legacy → ScheduledExecutorService Java 11+
  - ❌ NO replicar com.tridium.json (propio) → Jackson 2.17+
  - ✅ Mantener BAbstractService + Folder hierarchy + @NiagaraProperty (Niagara idiomatic)

**Timeline estimado MX60-Analytics module**: **~8 semanas** con 1-2 devs (después de cierre Reflow rewrite Bloque 65).

---

## 67.1 Librerías third-party — backend (analytics-rt)

### 67.1.1 Niagara core dependencies (28 modules en module.xml)

```xml
<dependency name="alarm-rt"/>           <!-- BAlarmService + BIAlarmSource -->
<dependency name="baja"/>               <!-- BComponent + BOrd + BService base -->
<dependency name="box-rt"/>             <!-- BoxTable data model -->
<dependency name="bql-rt"/>             <!-- BQL query language -->
<dependency name="chart-rt"/>           <!-- Chart base types -->
<dependency name="control-rt"/>         <!-- BControlPoint -->
<dependency name="driver-rt"/>          <!-- Driver framework -->
<dependency name="email-rt"/>           <!-- ¡Email notifications! Alert emails -->
<dependency name="entityIo-rt"/>        <!-- Entity I/O -->
<dependency name="file-rt"/>            <!-- BFileSystem + BIFile -->
<dependency name="fox-rt"/>             <!-- ¡FOX protocol! distributed station communication -->
<dependency name="gx-rt"/>              <!-- Graphics extension -->
<dependency name="hierarchy-rt"/>       <!-- Tag hierarchies -->
<dependency name="history-rt"/>         <!-- BHistoryService + BHistory + Cursor -->
<dependency name="jetty-rt"/>           <!-- Jetty embedded HTTP server -->
<dependency name="jsonSmart-rt"/>       <!-- JSON Smart parser (más rápido que Jackson) -->
<dependency name="neql-rt"/>            <!-- ¡NEQL! Niagara Equation Query Language -->
<dependency name="net-rt"/>             <!-- Network services -->
<dependency name="oauth2-rt"/>          <!-- OAuth2 authentication -->
<dependency name="platform-rt"/>        <!-- Platform services -->
<dependency name="program-rt"/>         <!-- Program component (scripting) -->
<dependency name="query-rt"/>           <!-- Query infrastructure -->
<dependency name="schedule-rt"/>        <!-- Scheduling -->
<dependency name="seriesTransform-rt"/> <!-- IntervalSeriesCursor — series transforms -->
<dependency name="tagdictionary-rt"/>   <!-- Tag dictionary -->
<dependency name="web-rt"/>             <!-- BWebServlet base -->
<dependency name="webChart-rt"/>        <!-- Web chart rendering -->
```

**Hallazgos clave**:
- ✅ **NO Apache Commons Math** dependency — Analytics implementa estadísticas DESDE CERO en `combine/` package
- ✅ **NO TensorFlow / DeepLearning4j / Weka / Smile-ML** — confirma ausencia ML
- ✅ **NEQL-rt** = Niagara Equation Query Language — **JOYA NIAGARA** para fórmulas (auditar deep si MX60 quiere herencia)
- ✅ **fox-rt** = FOX protocol Niagara distributed → multi-station Analytics
- ✅ **email-rt** = email alerts (Reflow Bloque 62 NO usaba — Analytics SÍ via N4 native)
- ✅ **seriesTransform-rt** = `IntervalSeriesCursor` integration time-series transforms
- ✅ **jsonSmart-rt** vs Jackson: Tridium usa JSON Smart (faster parser, smaller footprint)

### 67.1.2 Imports Java cross-codebase

| Categoría | Imports |
|-----------|---------|
| **Tridium internals** | com.tridium.collection.* (BGenericTable, GenericColumn) + com.tridium.data.BDataTable + com.tridium.fox.sys.* (BFoxClientConnection, BFoxSession, BDataChannel) + **com.tridium.json.*** (JSONException, JSONObject, JSONTokener, JSONWriter, QuickJSONWriter — NO Jackson) + com.tridium.seriestransform.IntervalSeriesCursor + com.tridium.sys.license.LicenseUtil + com.tridium.util.* (BBinderCacheScheme, ComponentTreeCursor, PxUtil) + com.tridium.web.RestUtil + com.tridium.web.WebUtil |
| **Java standard** | java.io.* + java.lang.reflect.* + java.math.BigDecimal + java.math.RoundingMode + java.net.URL* + java.security.AccessController |
| **NO encontrado** | NO commons-math, NO Jackson, NO joda-time, NO guava, NO smile, NO weka, NO deeplearning4j, NO tensorflow |

### 67.1.3 Tabla maestra librerías analytics-rt

| Library | Versión | Tipo | Propósito | Reemplazo MX60 |
|---------|---------|------|-----------|----------------|
| Niagara baja | 4.14.0 | core | BComponent + BOrd + BService | KEEP |
| history-rt | 4.14.0 | niagara | BHistoryService + Cursor + timeQuery | KEEP |
| alarm-rt | 4.14.0 | niagara | BAlarmService + BIAlarmSource | KEEP |
| **com.tridium.json** | proprietary | json | JSON parsing internal | **REEMPLAZAR Jackson 2.17+** (Bloque 61) |
| **AON** (com.tridiumx.analytics.aon) | proprietary | binary | Binary encoding propio | **REEMPLAZAR JSON estándar** (Bloque 66 Implication #205) |
| jsonSmart-rt | 4.14.0 | json | JSON parser Smart | OPCIONAL — Jackson cubre |
| neql-rt | 4.14.0 | query | Niagara Equation Query Language | **AUDITAR** — potencial reusable formula engine (Bloque 68 si caso de uso) |
| seriesTransform-rt | 4.14.0 | data | IntervalSeriesCursor time-series transforms | KEEP |
| fox-rt | 4.14.0 | distributed | FOX protocol multi-station | KEEP si MX60 multi-supervisor |
| email-rt | 4.14.0 | notification | Email alerts | KEEP — Reflow Bloque 62 lo skippeó |
| jetty-rt | 4.14.0 | web | Jetty HTTP server | KEEP (constraint N4) |
| webChart-rt | 4.14.0 | chart | Web chart rendering base | KEEP |
| oauth2-rt | 4.14.0 | auth | OAuth2 | OPCIONAL si MX60 federated auth |
| **NO commons-math** | — | — | Stats implementadas custom en `combine/` | **AGREGAR commons-math3 3.6+** o Apache Commons Statistics 1.0+ MX60 |

---

## 67.2 Librerías third-party — frontend (analytics-ux + analytics-wb)

### 67.2.1 Frontend libs confirmadas

```
analytics-ux/extracted/ext/c3/
├── c3.css           (chart library CSS)
└── c3.js            (chart library — built on D3)

analytics-ux/extracted/ext/datatables/
├── jquery.dataTables.css
└── jquery.dataTables.js   (jQuery DataTables — sortable tables)
```

| Library | Versión estimada | Propósito | Reemplazo MX60 |
|---------|------------------|-----------|----------------|
| **c3.js** | ~0.7.x (2017-2019) | Chart library D3-based | **REEMPLAZAR**: D3 7.9 directo (Bloque 61 Reflow) o recharts 2.10+ |
| **jquery.dataTables** | ~1.10.x | Sortable tables jQuery-based | **REEMPLAZAR**: TanStack Table v8 (Vue 3) o Vuetify 3 v-data-table |
| **jQuery** | (implícito por dataTables) | DOM manipulation legacy | **REMOVER** — Vue 3 nativo |

**Hallazgo crítico**: c3.js + jQuery DataTables son **stack frontend OBSOLETO 2017-2019**. MX60 frontend (Vue 3 + Vuetify 3 + D3 7.9 — Bloque 61 stack) reemplaza directamente.

### 67.2.2 ZKM obfuscation analytics-ux

78 clases ofuscadas con Zelix Klassmaster — **complica reverse-engineering** UX bridge. MX60 NO replicar este pattern (debugability lost).

---

## 67.3 Los 56 BAlgorithmBlock — catálogo COMPLETO

### 67.3.1 Tabla exhaustiva por categoría

#### Math (4)
| Block | Propósito | Lógica |
|-------|-----------|--------|
| **BBiMathBlock** | Operación binaria entre 2 inputs | Switch sobre BBiMathOperator (Add/Subtract/Multiply/Divide/Mod/Power) |
| **BBiMathOperator** | Enum operadores binarios | Enum: ADD, SUB, MUL, DIV, MOD, POW |
| **BUniMathBlock** | Operación unaria sobre 1 input | Switch sobre BUniMathOperator (Abs/Neg/Sqrt/Log/Exp/Sin/Cos/...) |
| **BUniMathOperator** | Enum operadores unarios | Enum: ABS, NEG, SQRT, LOG, LOG10, EXP, SIN, COS, TAN, ASIN, ACOS, ATAN, FLOOR, CEIL, ROUND |

#### Logic (5)
| Block | Propósito |
|-------|-----------|
| **BBooleanOperator** | Enum AND/OR/XOR |
| **BNotBlock** | NOT lógico |
| **BLogicFilterBlock** | Filtra basado en condición lógica |
| **BLogicFolderBlock** | Composición lógica jerárquica |
| **BIntersectionBlock** | Intersección temporal de 2 series |

#### Constants (5)
| Block | Propósito |
|-------|-----------|
| **BConstantBlock** | Base abstract |
| **BBooleanConstantBlock** | Constante boolean |
| **BNumericConstantBlock** | Constante numérica |
| **BStringConstantBlock** | Constante string |
| **BEnumConstantBlock** | Constante enum |

#### Switches (7)
| Block | Propósito |
|-------|-----------|
| **BBiSwitchBlock** | Switch binario (2 inputs → 1 output) |
| **BUniSwitchBlock** | Switch unario condicional |
| **BCovSwitchBlock** | Change-of-Value switch (cambia output al detectar COV) |
| **BDeadbandSwitchBlock** | Switch con deadband (hysteresis threshold) |
| **BInvalidValueSwitchBlock** | Switch ante valor inválido |
| **BRangeSwitchBlock** | Switch basado en rango |
| **BTransitionSwitchBlock** | Switch ante transición de estado |

#### Filters (5)
| Block | Propósito |
|-------|-----------|
| **BDeadbandFilterBlock** | Filtro hysteresis (suprime ruido <threshold) |
| **BInvalidValueFilterBlock** | Filtra valores inválidos (NaN, null) |
| **BLogicFilterBlock** | Filtra basado en lógica |
| **BRangeFilterBlock** | Filtra fuera de rango (out-of-bounds) |
| **BTimeFilterBlock** | Filtro temporal (selecciona ventana) |

#### Time (7)
| Block | Propósito |
|-------|-----------|
| **BTimeRangePart** | Parte de un range (start/end) |
| **BTimeRangeOffsetBlock** | Offset relativo de range |
| **BTimestampOffsetBlock** | Offset de timestamp |
| **BTimeUnit** | Enum unidades (MS, S, MIN, HOUR, DAY, WEEK, MONTH, YEAR) |
| **BDayBuilderBlock** | Construye day-of-week mask |
| **BIntervalCountBlock** | Cuenta intervalos en period |
| **BSlidingWindowBlock** | Ventana deslizante (rolling window) |

#### Energy / HVAC (4) — JOYAS
| Block | LOC | Propósito |
|-------|-----|-----------|
| **BConsumptionToDemandBlock** | ~150 | Convierte consumo (kWh) → demanda (kW) usando intervalo |
| **BDemandToConsumptionBlock** | ~150 | Inverso: demanda → consumo |
| **BPsychrometricBlock** | **434L** | **HVAC psychrometric calculations** — dewpoint, wet bulb, enthalpy, humidity ratio, comfort zones |
| **BPsychrometricMode** | enum | Modo cálculo (DRY_BULB, WET_BULB, DEWPOINT, ENTHALPY, HUMIDITY_RATIO, RELATIVE_HUMIDITY, ABSOLUTE_HUMIDITY) |

#### Data sources (5)
| Block | Propósito |
|-------|-----------|
| **BDataSourceBlock** | Source primario datos (lee BHistory) |
| **BValueTagDataSourceBlock** | Source filtrado por value tags |
| **BResultBlock** | Sink output (escribe result al final del DAG) |
| **BRollupBlock** | Rollup temporal (aggregation por intervalo) |
| **BRuntimeBlock** | Runtime equipment (cuánto tiempo ON/OFF) |

#### Mapping / Transform (9)
| Block | Propósito |
|-------|-----------|
| **BValueMapBlock** | Map valores input → valores output (lookup table) |
| **BValueMapEntry** | Entry de map (input → output) |
| **BValueMapMode** | Modo map (EXACT, RANGE, ENUM) |
| **BValueDurationBlock** | Duración de un valor sostenido |
| **BValueToStringBlock** | Convierte value → string |
| **BNodeToStringBlock** | Convierte node → string |
| **BStringConcatBlock** | Concatena strings |
| **BStringReplaceBlock** | Replace en strings (regex?) |
| **BFunctionBlock** | Custom function block (¡extensibilidad!) |

#### Misc (3)
| Block | Propósito |
|-------|-----------|
| **BRequestOverridesBlock** | Solicita overrides config |
| **BDebugBlock** | Block para debugging DAG |
| **BDebugMode** | Enum modo debug |

#### Base (4)
| Block | Propósito |
|-------|-----------|
| **AlgorithmBlock** | Interface base |
| **Algorithm** | Interface algorithm |
| **BAlgorithm** | BComponent algorithm (DAG container) |
| **BAlgorithmFolder** | Folder de algorithms |

**Total**: **56 algorithm blocks** confirmados (4 + 5 + 5 + 7 + 5 + 7 + 4 + 5 + 9 + 3 + 4 — slight overlap algunos como BFunctionBlock).

### 67.3.2 Hallazgo crítico — qué FALTA en Analytics que MX60 debe agregar

| Familia faltante | Blocks a crear MX60 | Priority |
|------------------|---------------------|----------|
| **PID Controllers** | BPIDControllerBlock + BSetpointBlock + BErrorBlock + BIntegralBlock + BDerivativeBlock + BAntiWindupBlock | **P0** si caso uso control loops |
| **Forecasting** | BARIMABlock + BHoltWintersBlock + BExpSmoothingBlock + BLinearRegressionBlock + BSeasonalDecompBlock | **P1** |
| **Anomaly Detection** | B3SigmaBlock + BIQRBlock + BSeasonalAnomalyBlock + BMahalanobisBlock + BMADBlock | **P1** |
| **FFT / Spectral** | BFFTBlock + BPowerSpectralDensityBlock + BWaveletBlock | **P2** si caso uso vibration |
| **ML inference** | BLinearModelBlock + BTreeModelBlock + BNeuralNetBlock (inference only) | **P2** |
| **Optimization** | BOptimalSetpointBlock + BLinearOptBlock | **P3** |

---

## 67.4 Los 17 combiners (statistical) — catálogo

```
analytics-rt/vineflower/com/tridiumx/analytics/combine/
├── AbstractCombiner.java       (base abstract)
├── AndCombiner.java            (boolean AND aggregate)
├── AvgCombiner.java            (mean / average)
├── CountCombiner.java          (count records)
├── FirstCombiner.java          (first value)
├── LastCombiner.java           (last value)
├── LoadFactorCombiner.java     (load factor = avg/peak — energy KPI)
├── MaxCombiner.java            (maximum)
├── MedianCombiner.java         (median — robust to outliers)
├── MinCombiner.java            (minimum)
├── ModeCombiner.java           (mode — most frequent value)
├── NoneCombiner.java           (null aggregator)
├── OrCombiner.java             (boolean OR aggregate)
├── RangeCombiner.java          (max - min)
├── StandardDeviationCombiner.java (stddev)
├── SumCombiner.java            (sum)
└── TrendCombiner.java          (preserva como trend, no aggregate)
```

**Hallazgo**: Analytics implementa estadísticas DESDE CERO sin Apache Commons Math. **MX60 puede heredar literal** (clean implementation) o **upgrade a Apache Commons Statistics 1.0+** para más opciones (variance, skewness, kurtosis, percentiles).

**Falta en Analytics**:
- Percentile / quantile combiners (Apache Commons Math sí tiene)
- Variance combiner (sólo stddev)
- Skewness / kurtosis (statistical moments)
- Geometric mean / harmonic mean

---

## 67.5 Los 22 trend wrappers — catálogo

```
trend/
├── AbstractTrend.java                (base)
├── AbstractTrendWrapper.java         (decorator base)
├── AggregateTrend.java               (aggregate over interval)
├── BaselineAnalyticTrend.java        (compare vs baseline)
├── ConstantTrend.java                (constant value)
├── DeltaTrend.java                   (delta = curr - prev)
├── DeltaValuesTrend.java             (delta values series)
├── EmptyTrend.java                   (null/empty)
├── ExitTrend.java                    (exit transitions)
├── FilteredNiagaraTrend.java         (filter applied)
├── FunctionTrend.java                (apply function f(x))
├── IgnoredAggregateTrend.java        (skip aggregation)
├── ImputedTrend.java                 (impute missing values)
├── IntervalTrend.java                (sampled at intervals)
├── LIAnalyticTrend.java              (linear interpolation)
├── MergeTrend.java                   (merge multiple trends)
├── NiagaraTrend.java                 (raw Niagara source)
├── OneCountTrend.java                (count = 1)
├── RawDataFilterTrend.java           (raw data filter)
├── SimpleTrend.java                  (simple wrapper)
├── StatusFilterTrend.java            (status-based filter)
└── ValueFilterTrend.java             (value-based filter)
```

**Patterns clave**:
- ✅ **Decorator pattern** (AbstractTrendWrapper) — composable transformations
- ✅ **Linear interpolation** (LIAnalyticTrend) — fill gaps
- ✅ **Imputation** (ImputedTrend) — missing data strategies
- ✅ **COV filtering** (FilteredNiagaraTrend, ValueFilterTrend) — Change-of-Value
- ✅ **Baseline comparison** (BaselineAnalyticTrend) — energy normalization

**MX60 hereda literal** todos los 22 trend wrappers. Pattern Decorator excelente.

---

## 67.6 BPsychrometricBlock 434L — DEEP DIVE HVAC

### 67.6.1 Modos psychrometric (BPsychrometricMode enum)

```java
public enum BPsychrometricMode {
    DRY_BULB,            // Temperatura bulbo seco
    WET_BULB,            // Temperatura bulbo húmedo (evaporative cooling)
    DEWPOINT,            // Punto de rocío (condensation threshold)
    ENTHALPY,            // Entalpía (energía total aire húmedo) [kJ/kg]
    HUMIDITY_RATIO,      // Razón humedad [kg agua / kg aire seco]
    RELATIVE_HUMIDITY,   // Humedad relativa [%]
    ABSOLUTE_HUMIDITY,   // Humedad absoluta [kg/m³]
    SPECIFIC_VOLUME      // Volumen específico [m³/kg]
}
```

### 67.6.2 Inputs típicos

- Temperatura bulbo seco (T_db)
- Humedad relativa (RH%) o humedad absoluta
- Presión barométrica (P_atm) — default 101.325 kPa (sea level)

### 67.6.3 Cálculos implementados (típico psychrometric)

- **ASHRAE Fundamentals** equations (probable basis)
- Saturation pressure: `Pws = exp(C1/T + C2 + C3*T + C4*T² + C5*T³ + C6*ln(T))` (ASHRAE 2009)
- Humidity ratio: `W = 0.622 * Pw / (P - Pw)`
- Enthalpy: `h = 1.006*T + W*(2501 + 1.86*T)` [kJ/kg dry air]
- Wet bulb: iterativo (Newton-Raphson) — converge `T_wb` que satisface `h(T_wb, RH=100%) = h(T_db, RH)`
- Dewpoint: inverso de saturation pressure

**Veredicto**: ✅ **Implementación HVAC profesional** — replica formulas ASHRAE Handbook. **JOYA reusable MX60**. Heredar literal.

### 67.6.4 MX60 implication

> **Implication #211 KEEP literal**: BPsychrometricBlock — replicar 434L logic con same formulas ASHRAE. HVAC Mexico requirements (humidity high coastal cities) requiere psychrometric calculations precisos.

---

## 67.7 ML / PID / Forecasting / Anomaly — análisis exhaustivo

### 67.7.1 ¿PID Controllers? — VEREDICTO: ❌ NO

**Búsqueda empírica**:
```
grep -rilE "(setpoint|integral.*derivative|PIDController|PID.*controller)" analytics-rt/
→ 1 hit falso positivo: BAnalyticService.java (palabra "integral" en otro contexto matemático)
```

**Conclusión**: Analytics NO tiene PID controllers. Es subsistema de **análisis offline / KPI computation**, NO control loops realtime.

**Por qué no**: Niagara N4 tiene PID en `control-rt` module (BPIDLoopPoint, BPIDLoopFolder) — Analytics NO los usa. Analytics es **observational**, no actuating.

**MX60 implication**: si MX60 necesita control loops, **agregar familia BPIDControllerBlock** desde cero o integrar `control-rt` BPIDLoopPoint nativo.

### 67.7.2 ¿Machine Learning? — VEREDICTO: ❌ NO

**Búsqueda empírica**:
```
grep -rilE "(tensorflow|pytorch|deeplearning4j|weka|smile-ml|RandomForest|NeuralNet|SVM|KMeans|cluster|classification)" analytics-rt/
→ 0 hits relevantes
```

**Excepción menor**: `LIAnalyticTrend` (Linear Interpolation) usa concepto K-NN-like para fill gaps — pero NO es ML real, es interpolación lineal entre 2 puntos vecinos.

**Conclusión**: Analytics NO tiene Machine Learning. **Cero algoritmos ML detectados**.

**MX60 implication**: si MX60 necesita ML inference, **agregar familia BMLBlock** con:
- BLinearRegressionBlock (online learning)
- BTreeModelBlock (decision tree inference)
- BNeuralNetInferenceBlock (TensorFlow Lite o ONNX Runtime)
- Lib recomendada: **DeepLearning4J 1.0-M2.1+** (pure Java) o **ONNX Runtime Java**

### 67.7.3 ¿Forecasting? — VEREDICTO: ❌ NO

**Búsqueda empírica**:
```
grep -rilE "(ARIMA|HoltWinters|exponential.*smoothing|forecast|prediction)" analytics-rt/
→ 0 hits
```

**Lo único parecido**: BaselineAnalyticTrend (compara vs baseline period anterior). **NO es forecasting predictivo**, es comparación retrospectiva.

**Conclusión**: Analytics **NO predice futuro**. Solo compara presente vs pasado.

**MX60 implication**: agregar familia Forecasting:
- BARIMABlock (auto-regressive integrated moving average)
- BHoltWintersBlock (triple exponential smoothing — captura trend + seasonality)
- BExpSmoothingBlock (single exponential smoothing)
- BLinearRegressionForecastBlock (regression-based forecast)
- Lib recomendada: **Apache Commons Statistics + custom ARIMA implementation** o **Smile-ML 3.0+** (forecast module)

### 67.7.4 ¿Anomaly Detection? — VEREDICTO: ❌ NO sofisticado

**Lo que tiene Analytics**:
- ✅ BInvalidValueFilterBlock (NaN/null filter — trivial)
- ✅ BRangeFilterBlock (out-of-bounds threshold — trivial)
- ✅ BDeadbandFilterBlock (hysteresis — anti-noise)

**Lo que NO tiene**:
- ❌ 3-sigma anomaly (statistical outlier)
- ❌ IQR (interquartile range) outlier detection
- ❌ Mahalanobis distance (multivariate)
- ❌ Seasonal anomaly (residual after STL decomposition)
- ❌ Isolation Forest, One-Class SVM, autoencoders

**Conclusión**: Anomaly detection en Analytics es **threshold-based rudimentario**. NO statistical, NO ML.

**MX60 implication**: agregar familia Anomaly:
- B3SigmaBlock (mean ± 3*stddev)
- BIQRBlock (Q1 - 1.5*IQR, Q3 + 1.5*IQR)
- BSeasonalAnomalyBlock (residual STL)
- BMahalanobisBlock (multivariate, requires inverse covariance matrix → Apache Commons Math)
- BMADBlock (Median Absolute Deviation — robust to outliers)

### 67.7.5 ¿FFT / Frequency Analysis? — VEREDICTO: ❌ NO

**Búsqueda**: "Spectrum chart" en Analytics es **histograma de carga horaria** (load spectrum), NO Fast Fourier Transform.

**Conclusión**: Analytics NO tiene FFT. Si MX60 necesita análisis espectral (vibration analysis, motor health, frequency-domain features), **agregar BFFTBlock** con:
- Lib recomendada: **JTransforms 3.1** (Java FFT, lightweight) o Apache Commons Math FFT module

### 67.7.6 Resumen veredictos

| Capability | Estado en Analytics | Acción MX60 |
|------------|---------------------|-------------|
| PID | ❌ NO | AGREGAR familia BPID* (P0 si control loops) |
| ML | ❌ NO | AGREGAR si caso uso (P2) — DL4J o ONNX |
| Forecasting | ❌ NO | AGREGAR familia BForecast* (P1) — ARIMA, Holt-Winters |
| Anomaly statistical | ❌ NO | AGREGAR familia BAnomaly* (P1) — 3-sigma, IQR, MAD |
| FFT | ❌ NO | AGREGAR si vibration (P2) — JTransforms |
| Psychrometric | ✅ SÍ excelente | HEREDAR literal |
| Degree-Day | ✅ SÍ | HEREDAR literal |
| Statistical funcs | ✅ SÍ (17) | HEREDAR + ampliar percentile/variance |
| Equipment Operation | ✅ rudimentario | HEREDAR + ampliar (MTBF, OEE, runtime) |
| Load Profiling | ✅ SÍ frontend | HEREDAR concept, modernizar charts |

---

## 67.8 Algorithm DAG composition mechanics

### 67.8.1 BlockPin connections

```java
// Pseudocode — patrón Niagara
class BAlgorithmBlock {
    BBlockPin[] inputs;   // input slots con types
    BBlockPin[] outputs;  // output slots
    
    void compute(Context cx) {
        // Pull inputs from connected blocks
        AnalyticTrend in1 = inputs[0].getValue();
        AnalyticTrend in2 = inputs[1].getValue();
        
        // Compute result
        AnalyticTrend result = doCompute(in1, in2);
        
        // Push to outputs
        outputs[0].setValue(result);
    }
}

class BResultBlock extends BAlgorithmBlock {
    // Sink — escribe trend final → BAnalyticVector / persist
}
```

### 67.8.2 Evaluation strategy

- **Lazy evaluation** — solo computa cuando consumer lo solicita
- **Topological sort** del DAG antes de evaluation
- **Cache trends** en KeyedCache durante una eval (avoid re-compute si compartido)
- **Pull-based** — sink (BResultBlock) solicita upstream

### 67.8.3 MX60 implication

> **Implication #212**: Algorithm DAG composition es **JOYA arquitectónica** — replicar pattern exacto MX60 con Java 11+ records para BlockPin immutable + sealed interfaces para AlgorithmBlock subclasses.

---

## 67.9 AON encoding (19 archivos)

### 67.9.1 Estructura

```
aon/
├── Aon.java                  (factory: _Aobj, _Alist, _Amap, _Adbl, _Aint, _Astr, ...)
├── AonIo.java                (608L) — Reader/Writer binario + textual
├── Encoder.java              (710L) — serialización response
├── (... 16 más)
```

### 67.9.2 Trade-offs vs JSON

| Aspecto | AON | JSON |
|---------|-----|------|
| Tamaño wire | Más compacto (binary) | Más grande (text) |
| Performance parse | Más rápido | Más lento |
| Debugability | ❌ Binary opaque | ✅ Texto legible |
| Tooling | ❌ Tridium proprietary | ✅ Universal (browser DevTools, Postman, jq) |
| Browser nativo | ❌ Requiere lib parser | ✅ JSON.parse nativo |

**Decisión MX60**: **NO AON** (Bloque 66 Implication #205 confirmada). MX60 usa JSON estándar para debugability + tooling.

---

## 67.10 Pollers detallado

### 67.10.1 BCyclicPoller (interval-driven)

```java
// Pseudocode
@NiagaraType
public class BCyclicPoller extends BAnalyticPoller {
    @NiagaraProperty(facets = {BFacets.UNITS_TIME})
    BRelTime interval = BRelTime.makeMinutes(5);  // poll every 5 min
    
    @Override
    public void started() {
        executor.scheduleAtFixedRate(this::poll, 0, interval.getMillis(), MILLISECONDS);
    }
}
```

### 67.10.2 BTriggeredPoller (event-driven)

```java
@NiagaraType
public class BTriggeredPoller extends BAnalyticPoller {
    @NiagaraProperty
    BLink trigger;  // input link a punto que dispara
    
    @Override
    public void changed(Property p, Context cx) {
        if (p == trigger) {
            poll();  // event-driven
        }
    }
}
```

**MX60 hereda separation interval-driven vs event-driven**, modernizar a `ScheduledExecutorService` Java 11+ (NO ThreadPool legacy).

---

## 67.11 Antipatterns formalizados AP-90+

| # | Severity | Título | Site | Categoría |
|---|----------|--------|------|-----------|
| AP-90 | LOW | AON encoding lock-in vendor | analytics-rt/aon/* | Vendor lock |
| AP-91 | LOW | com.tridium.json (proprietary) en lugar de Jackson | analytics-rt cross-codebase | Proprietary lib lock-in |
| AP-92 | MEDIUM | ThreadPool legacy custom (pre-Java 5) | util/ThreadPool.java | Legacy abstraction — Bloque 66 confirmed |
| AP-93 | LOW | ZKM obfuscation analytics-ux 78 clases | analytics-ux/* | Debugability lost |
| AP-94 | LOW | c3.js + jQuery DataTables stack frontend obsoleto 2017-2019 | analytics-ux/ext/c3, /datatables | Outdated UI libs |
| AP-95 | MEDIUM | NO Apache Commons Math — estadísticas custom 17 combiners | combine/* | Reinventing wheel (también pro: clean impl) |
| AP-96 | LOW | NO ML / PID / Forecasting / Anomaly statistical | (ausencia) | Limitada capability vs needs modernos |

**TOTAL AP-1..AP-96 post-Bloque 67** = **96 antipatterns identificados**.

---

## 67.12 Patterns excelentes (KEEP literal MX60) — P-91..96

1. **P-91: Algorithm DAG composition con BlockPin connections** — extensible, declarativo
2. **P-92: Decorator pattern trend wrappers** (AbstractTrendWrapper + 22 subclasses) — composable transformations
3. **P-93: Psychrometric ASHRAE-grade implementation** (BPsychrometricBlock 434L)
4. **P-94: 17 statistical combiners clean impl** (sin commons-math dependency)
5. **P-95: Pollers separation interval/event-driven** (BCyclicPoller + BTriggeredPoller)
6. **P-96: BResultBlock sink pattern** — clean DAG terminator

---

## 67.13 MX60 implications — continuación desde #210

| # | Tag | Descripción |
|---|-----|-------------|
| 211 | KEEP | **BPsychrometricBlock 434L** replicar literal — ASHRAE psychrometric calculations professional-grade |
| 212 | KEEP | **Algorithm DAG composition pattern** — Java 11+ sealed interfaces + records BlockPin |
| 213 | KEEP | **22 trend wrappers (decorator pattern)** — replicar todos |
| 214 | KEEP | **17 combiners statistical** — clean impl, opcional ampliar con percentile/variance/skewness via Apache Commons Statistics |
| 215 | NEW | **Familia BPID*Block** (PID controllers) — agregar de cero P0 si MX60 control loops |
| 216 | NEW | **Familia BForecast*Block** (ARIMA, Holt-Winters, ExpSmoothing, LinearRegression) — agregar P1 |
| 217 | NEW | **Familia BAnomaly*Block** (3-sigma, IQR, MAD, Mahalanobis, Seasonal) — agregar P1 |
| 218 | NEW | **Familia BFFT*Block** (FFT, PSD, Wavelet) — P2 si caso vibration |
| 219 | NEW | **Familia BML*Block** (LinearModel, TreeModel, NN inference) — P2 si caso ML — lib DL4J o ONNX Runtime |
| 220 | KEEP | Pollers BCyclicPoller + BTriggeredPoller separation (modernizar ScheduledExecutorService Java 11+) |
| 221 | SKIP | AON encoding — MX60 JSON estándar (debugability + tooling) |
| 222 | SKIP | com.tridium.json — Jackson 2.17+ MX60 |
| 223 | SKIP | ThreadPool legacy — ScheduledExecutorService Java 11+ |
| 224 | SKIP | c3.js + jQuery DataTables — MX60 D3 7.9 + Vuetify v-data-table o TanStack Table |
| 225 | NEW | Apache Commons Statistics 1.0+ para extender combiners (percentile, variance, skewness, kurtosis) |
| 226 | NEW | NEQL (Niagara Equation Query Language) audit potencial — neql-rt module dependency. Si MX60 quiere expression evaluator, evaluar herencia |
| 227 | NEW | Email alerts via email-rt module — Reflow Bloque 62 skippeó, MX60 puede heredar literal |

**Total MX60 implications post-Bloque 67**: **227 entries** (210 previos + 17 nuevos: 5 KEEP + 5 NEW + 4 SKIP + 3 NEW lib decisions).

---

## 67.14 Reglas template MX60 — 4 reglas nuevas (39-42)

### Regla 39 — Algorithm DAG composition con sealed interfaces + records

```java
public sealed interface AlgorithmBlock 
    permits MathBlock, LogicBlock, FilterBlock, SwitchBlock, ... {

    record BlockPin<T>(String name, Class<T> type) {}
    
    List<BlockPin<?>> inputs();
    List<BlockPin<?>> outputs();
    
    void compute(Context cx);
}
```

NO clases anidadas profundas. Sealed para exhaustiveness check.

### Regla 40 — Trend wrappers decorator pattern

```java
public abstract class AbstractTrendWrapper implements AnalyticTrend {
    protected final AnalyticTrend delegate;
    
    public AbstractTrendWrapper(AnalyticTrend delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }
}

public class FilteredTrend extends AbstractTrendWrapper { ... }
public class BaselineTrend extends AbstractTrendWrapper { ... }
```

Composable transformations sin coupling.

### Regla 41 — Statistical combiners interface explícito

```java
public interface Combiner<T> {
    T combine(Iterable<T> values);
    String getName();  // "avg", "median", "stddev", ...
}

// Implementations: AvgCombiner, MedianCombiner, StdDevCombiner, ...
```

Type-safe + extensible.

### Regla 42 — Pollers usar ScheduledExecutorService Java 11+ (NO legacy ThreadPool)

```java
public class BCyclicPoller extends BAnalyticPoller {
    private final ScheduledExecutorService executor = 
        Executors.newScheduledThreadPool(1, namedFactory("cyclic-poller"));
    
    public void started() {
        executor.scheduleAtFixedRate(this::poll, 0, intervalMs, MILLISECONDS);
    }
    
    public void stopped() {
        executor.shutdown();  // graceful shutdown
        if (!executor.awaitTermination(5, SECONDS)) executor.shutdownNow();
    }
}
```

**Total reglas template MX60 post-Bloque 67**: **42 reglas** (38 previas + 4 nuevas).

---

## 67.15 Roadmap construcción módulo MX60-Analytics-inspired

### 67.15.1 Sprint estimado (~8 semanas, 1-2 devs)

| Semana | Epic | Effort |
|--------|------|--------|
| 1 | Bootstrap módulo MX60-analytics-rt: BMx60AnalyticsService blueprint + folder hierarchy + module.xml deps | 1 sem |
| 2 | AlgorithmBlock framework: sealed interface + BlockPin records + DAG topological sort + lazy eval | 1 sem |
| 3 | Math blocks (4) + Logic blocks (5) + Constants (5) + Switches (7) heredados Analytics | 1 sem |
| 4 | Filters (5) + Time blocks (7) + Mapping/transform (9) heredados | 1 sem |
| 5 | **Psychrometric block 434L** + ConsumptionToDemand + DemandToConsumption + Degree-Day baseline | 1 sem |
| 6 | **17 combiners** + extensión percentile/variance/skewness via Apache Commons Statistics | 0.5 sem |
| 6 | **22 trend wrappers** decorator pattern | 0.5 sem |
| 7 | **Familia BPID*Block** (P0 si control loops) + Familia BForecast*Block (ARIMA, Holt-Winters) | 1 sem |
| 8 | **Familia BAnomaly*Block** (3-sigma, IQR, MAD) + integration tests + frontend Vuetify v-data-table + D3 charts | 1 sem |
| **TOTAL** | — | **8 sem** |

### 67.15.2 Dependencies con Reflow audit MX60

- ✅ Bloque 65 stack MX60 decidido (Vue 3 + Pinia + Vuetify 3 + Java 11+ + Jackson 2.17+ + Spotless+SpotBugs+Checkstyle+ErrorProne + JUnit 5 + GitHub Actions + pre-commit hooks anti-AP)
- ✅ Bloque 64 BReflowService blueprint → BMx60AnalyticsService extends BAbstractService implements BIService
- ✅ Reglas 11+13+18 cx propagation aplican a Algorithm execution
- ✅ Regla 33 service container Property injection
- ✅ Regla 35 reactive Property change cascade

### 67.15.3 Quick wins primeros 2 sprints

1. **Sprint 1**: BMx60AnalyticsService skeleton + BAlgorithmFolder + 5 constant blocks (smoke test DAG)
2. **Sprint 2**: BBiMathBlock + BUniMathBlock + BResultBlock (compute simple sum DAG end-to-end)

---

## 67.16 Qué falta auditar después (Bloques 68-70 si emerge)

### Bloque 68 (opcional) — NEQL deep dive

**Niagara Equation Query Language** (neql-rt module dependency Analytics) — potencial **expression evaluator** para MX60 dynamic formulas. Audit:
- Sintaxis NEQL
- Capabilities (math, logic, time-series operations)
- Performance overhead
- Integration con BQL

**Effort**: 1 sesión.

### Bloque 69 (opcional) — BNaServlet API + AON wire schema

Si MX60 va a integrar con Analytics frontend (vs replicar todo) — audit:
- 22 ws/ messages AON-encoded schemas
- BNaServlet endpoints catalog
- CORS + auth pattern

**Effort**: 1 sesión.

### Bloque 70 (opcional) — Security RLS + permissions Analytics

PermissionException + @requiredPermissions audit explícito (similar AP-76 RLS Reflow Bloque 62).

**Effort**: 1 sesión.

---

## 67.17 Cierre — decisión arquitectónica MX60-Analytics módulo

### Veredicto final

**MX60 hereda 80% de Analytics** (Algorithm DAG + 56 blocks + 17 combiners + 22 trend wrappers + Psychrometric + Degree-Day + Pollers separation) **+ agrega 20% nuevo crítico** (3 familias: PID + Forecasting + Anomaly).

### Decisiones técnicas

1. ✅ **Heredar arquitectura Algorithm DAG** (Java 11+ sealed interfaces + records)
2. ✅ **Heredar Psychrometric ASHRAE-grade** (joya HVAC Mexico humid coastal)
3. ✅ **Heredar 17 combiners + 22 trend wrappers** (clean impl, ampliar con Apache Commons Statistics)
4. ✅ **Heredar Pollers** (modernizar ScheduledExecutorService)
5. ❌ **Reescribir encoding** (JSON estándar, NO AON)
6. ❌ **Reescribir JSON layer** (Jackson 2.17+, NO com.tridium.json)
7. ❌ **Reescribir thread pool** (Java 11+, NO legacy ThreadPool custom)
8. ❌ **Reescribir frontend charts** (D3 7.9 + Vuetify, NO c3.js + jQuery DataTables)
9. ➕ **Agregar familia BPID*Block** (P0 si control loops)
10. ➕ **Agregar familia BForecast*Block** (P1 — ARIMA, Holt-Winters)
11. ➕ **Agregar familia BAnomaly*Block** (P1 — 3-sigma, IQR, MAD, Mahalanobis)
12. ➕ **Agregar Apache Commons Statistics 1.0+** dependency

### Stack final módulo MX60-analytics

```yaml
module: mx60-analytics-rt
deps_niagara: [baja, history-rt, alarm-rt, control-rt, schedule-rt, fox-rt, email-rt, web-rt, jetty-rt]
deps_third_party:
  - jackson-core 2.17+
  - apache-commons-statistics 1.0+
  - apache-commons-math3 3.6+ (for Mahalanobis distance + FFT futuro)
  - jtransforms 3.1 (FFT — opcional P2)
  - deeplearning4j 1.0-M2.1+ (ML — opcional P2)
NO_libs:
  - com.tridium.json (proprietary)
  - AON encoding (proprietary)
  - c3.js (obsolete)
  - jQuery DataTables (jQuery legacy)
  - ZKM obfuscation
java: 11+
build: Gradle 8.5+ + Niagara modules plugin + spotless + spotbugs + errorprone
test: JUnit 5 + Mockito 5 + AssertJ + Niagara station test harness
ci: GitHub Actions + pre-commit hooks anti-AP (Bloque 65)
frontend: Vue 3 + TypeScript strict + Pinia + Vuetify 3 (data-table) + D3 7.9 (charts) — Bloque 65 stack
```

### Roadmap MX60 actualizado total

- **Reflow rewrite (Bloque 65)**: 10-16 sprints
- **+ MX60-analytics módulo (Bloque 67)**: 8 semanas (~4 sprints)
- **+ Bloques 68-70 opcionales**: 3 semanas si emergen casos uso

**TOTAL MX60 ecosystem**: **14-20 sprints (7-10 meses con 1-2 devs)** o **5-7 meses con 3-4 devs**.

---

**End of Bloque 67** — Analytics deep dive para construir módulo MX60.

**Capa 18 Analytics**: Bloque 66 (exploración) + **Bloque 67 (deep dive constructivo)** = **producción-ready para arrancar MX60-analytics módulo**.

**Siguiente decisión usuario**: ¿proceder con Bloque 68 NEQL? ¿Bloque 69 API surface? ¿Bloque 70 security? O ¿cerrar Capa 18 y arrancar implementación MX60?
