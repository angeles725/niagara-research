# Bloque 76 — chihuahua-rt port: com.tridium.json, BControlPoint.getOutStatusValue, module deps, oBIX export de BComponent custom

> Investigación empírica para portar un generador de JSON desde un Program (jsonToolkit `InlineJsonWriter`) a un módulo propio `chihuahua-rt` (BAbstractService con slot frozen `baja:String output`, regenerado cada 120s vía `ScheduledExecutorService`, expuesto por oBIX en `/obix/config/.../output`).
> Compila contra **iC-Niagara-4.13.2.18**, deploya a station **4.14.0.162** (cross-version intencional).
>
> Fuentes (orden de research correcto, ver engram `tooling/research-entrypoints` #3057):
> - **niagara-help bajadoc** `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/bajadoc/` — API oficial soportada.
> - **module-navigator** `.../module-navigator/` (926 JARs, 51k clases) — solo `modules/*.jar`, NO indexa `nre.jar`.
> - **source decompilado** `/home/cristian/modules/Prototipos/modulos/organized/{mod}/{mod}-rt/vineflower/`.
> - **javap** sobre `nre.jar` (bin/ext) para `com.tridium.*` interno.
> - Verificado en AMBAS versiones: 4.14 (`/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/`) y 4.13 (`/mnt/c/Niagara/iC-Niagara-4.13.2.18/`).

---

## 76.1 — `com.tridium.json`: ¿API usable? ¿JSONWriter → String?

### Hallazgo central
`com.tridium.json` es la librería **`org.json` (JSON-Java) re-empaquetada** dentro de **`nre.jar`** (NO es un módulo, vive en `bin/ext/nre.jar`).

Clases presentes (`unzip -l nre.jar | rg com/tridium/json/`, 4.14):
`JSONObject`, `JSONArray`, `JSONWriter`, `JSONTokener`, `JSONStringer`, `JSONString`, `JSONPointer`, `JSONException`, `CDL`, `Cookie`/`CookieList`, `HTTP`/`HTTPTokener`, `XML`/`XMLTokener`/`XMLParserConfiguration`, `JSONML`, `Property`, `JSONUtil`, `JSONObject$Null`, subpaquetes `com/tridium/json/quick/QuickJSONWriter` y `com/tridium/json/pretty/PrettyJSONStringer`.

### `JSONWriter` — javap verbatim (4.14)
```
public class com.tridium.json.JSONWriter {
  protected char mode;
  protected java.lang.Appendable writer;
  public com.tridium.json.JSONWriter(java.lang.Appendable);     // <-- constructor
  public com.tridium.json.JSONWriter array();
  public com.tridium.json.JSONWriter endArray();
  public com.tridium.json.JSONWriter endObject();
  public com.tridium.json.JSONWriter key(java.lang.String);
  public com.tridium.json.JSONWriter object();
  public static java.lang.String valueToString(java.lang.Object);
  public com.tridium.json.JSONWriter value(boolean);
  public com.tridium.json.JSONWriter value(double);
  public com.tridium.json.JSONWriter value(long);
  public com.tridium.json.JSONWriter value(java.lang.Object);
}
```

### Veredictos 76.1
- **¿JSONWriter escribe a String?** SÍ. El constructor toma `java.lang.Appendable`. `java.io.StringWriter` y `java.lang.StringBuilder` implementan `Appendable`, así que:
  ```java
  StringWriter sw = new StringWriter();
  JSONWriter w = new JSONWriter(sw);
  w.object().key("baja").value("...").endObject();
  String json = sw.toString();
  ```
  Alternativa más directa: `JSONStringer` (extiende JSONWriter, escribe a un `StringBuilder` interno; `.toString()` da el String) o `JSONObject`/`JSONArray` construidos en memoria + `.toString()`.
- **¿API estable/soportada?** RIESGO MEDIO. **NO** aparece en el bajadoc oficial (`niagara-help/bajadoc/` no tiene `com/tridium/json/`). El prefijo `com.tridium.*` + ausencia de doc-jar = **API interna/no soportada**, no contractual. Vive en `nre.jar` (core runtime), por eso está en el classpath de cualquier módulo SIN declarar dependencia.
- **¿Diff 4.13 vs 4.14?** Superficie pública de `JSONWriter` **idéntica** (mismo constructor `Appendable`, mismos `object/key/value/array/endObject/endArray/valueToString`). Bajo riesgo de ruptura cross-version para este subset.
- **Idiomático Baja-native (sin JSON):** `BSimple.encodeToString()` serializa un valor Baja a su String slot (NO es JSON, es el formato de slot). Para JSON, lo más cercano al `InlineJsonWriter` de jsonToolkit es `com.tridium.json.JSONWriter(Appendable)`.

> Recomendación de diseño: si querés cero riesgo cross-version y cero dependencia de API interna, bundleá tu propio writer JSON (o Gson shaded, patrón que usa honPlantController). Si aceptás el riesgo bajo, `com.tridium.json.JSONWriter` está disponible gratis y es estable 4.13↔4.14.

---

## 76.2 — `BControlPoint.getOutStatusValue()`: firma y lectura genérica value+status

### Source verbatim — `control-rt/.../javax/baja/control/BControlPoint.java` (vineflower)
```java
public abstract class BControlPoint extends BComponent implements BIStatusValue {   // :47
   public final BStatusValue getStatusValue()  { return this.getOutStatusValue(); }  // :83
   public abstract BStatusValue getOutStatusValue();                                  // :99  <-- EXISTE
   public final Property getOutProperty() { return getOutStatusValue().getPropertyInParent(); } // :101
   public abstract void onExecute(BStatusValue var1, Context var2);                    // :133
}
```
- **Firma exacta:** `public abstract BStatusValue getOutStatusValue()`. Es **público** y abstracto en `BControlPoint`; cada subclase concreta (BNumericPoint, BBooleanPoint, BStringPoint, BEnumPoint) lo implementa devolviendo su `out`.
- **API soportada:** SÍ — `BControlPoint` y `BStatusValue` tienen bajadoc oficial.
- **Existe en 4.13 y 4.14** (clase base estable del control framework).

### Lectura genérica value+status sin castear — `baja/.../javax/baja/status/BStatusValue.java`
```java
public abstract class BStatusValue extends BStruct implements BIStatusValue {  // :26
   public BStatus getStatus();          // :31  -> flags (ok/fault/down/alarm/stale/overridden/null)
   public abstract BValue getValueValue();   // :74  -> el valor primitivo boxed (BDouble/BBoolean/BString/BEnum)
   public abstract Property getValueProperty(); // :76
}
```
Patrón genérico para CUALQUIER `BControlPoint` (numérico/bool/string/enum), sin instanceof ni cast a subclase:
```java
BStatusValue sv = point.getOutStatusValue();
BValue   value  = sv.getValueValue();   // BDouble/BBoolean/BString/BDynamicEnum...
BStatus  status = sv.getStatus();       // status.isValid(), .isFault(), .isNull(), .getBits(), .toString()
String   asText = value.encodeToString(); // o ((BIStatusValue)sv) según necesidad
```

### Veredicto 76.2
`getOutStatusValue()` es exactamente lo que se necesita: un punto de acceso uniforme `value + status` para cualquier punto genérico. `getValueValue()` da el valor boxed, `getStatus()` da el `BStatus`. Cero cast a subclases concretas.

---

## 76.3 — Dependencias de módulo, runtime profiles, peso de driver-rt

### `module.xml` verbatim (4.14 install; 4.13 equivalente)

**control-rt** — LIVIANO:
```xml
<module name="control-rt" ... moduleName="control" runtimeProfile="rt" nre="true">
  <dependency name="baja" .../>            <!-- ÚNICA dependencia -->
  <modulePart name="control-ux" runtimeProfile="ux"/>
  <modulePart name="control-wb" runtimeProfile="wb"/>
</module>
```

**driver-rt** — PESADO (15 dependencias):
```xml
<module name="driver-rt" ... moduleName="driver" runtimeProfile="rt" nre="true">
  <dependency name="alarm-rt"/>  <dependency name="baja"/>      <dependency name="bql-rt"/>
  <dependency name="chart-rt"/>  <dependency name="control-rt"/><dependency name="entityIo-rt"/>
  <dependency name="file-rt"/>   <dependency name="fox-rt"/>    <dependency name="gx-rt"/>
  <dependency name="history-rt"/><dependency name="net-rt"/>    <dependency name="platform-rt"/>
  <dependency name="query-rt"/>  <dependency name="schedule-rt"/><dependency name="web-rt"/>
  <modulePart name="driver-ux"/> <modulePart name="driver-wb"/>
</module>
```

### Veredictos 76.3
- **Module names a declarar como dependencia:** `control-rt` y `driver-rt` (el atributo `name` es con sufijo de profile; en Gradle: `api(":control-rt")`).
- **Runtime profile:** `rt` para lógica de station. Regla: `rt` solo importa `rt`. NO necesitás `-wb` para nada server-side. Como tu módulo es backend puro (BAbstractService + scheduler), declarás solo `chihuahua-rt`.
- **¿driver-rt es pesado?** SÍ — arrastra 15 módulos (alarm, bql, chart, fox, gx, history, net, platform, query, schedule, web...). `control-rt` en cambio depende **solo de `baja`**.
- **¿Podés evitar driver-rt?** SÍ, si solo leés puntos. `BControlPoint` vive en **control-rt** (no en driver). Solo necesitás `driver-rt` si referenciás `BDevice`/`BDeviceNetwork`/`BProxyExt` (paquete `javax.baja.driver.*`). Para iterar puntos por `BControlPoint` y leer `getOutStatusValue()` alcanza con **`baja` + `control-rt`**.
- `BAbstractService`, `BStatusValue`, `javax.baja.naming.*` están todos en **`baja`** (siempre disponible).

---

## 76.4 — Exposición oBIX de un BComponent/BAbstractService custom

### Quién expone el config tree
- El **servidor** oBIX (expone `/obix/config/...`) vive en el módulo **`obixDriver-rt`** (paquetes `javax.baja.obix.io`, `com.tridium.obix.server`, `com.tridium.obix.naming`). El driver `obix-rt` (paquete `obix.*`) es el CLIENTE/contracts. (Corrección a research previa que ubicaba el servlet en web-rt.)
- Encoder real: `javax.baja.obix.io.ObixEncoder` (642 líneas, ZKM-ofuscado). Modelo de **Agents por tipo**: `com.tridium.obix.server.BControlPointAgent` (puntos), `BObixAgent` (base genérico no-op), `BAlarmServiceAgent`, etc.

### Mecánica del encoder — `ObixEncoder.java` (vineflower)
```java
protected void encodeTarget(OrdTarget tgt) {                       // :363
   if (o instanceof BIObixEncodable) { ((BIObixEncodable)o).encode(this, tgt); }  // custom opcional
   else if (s instanceof Action) { this.encodeOp(tgt); }           // accion -> <op>
   else { this.encodeData(tgt); }                                  // GENÉRICO
}

protected void encodeData(OrdTarget tgt) {                         // :377
   BIObixAgent agent = this.getAgent(tgt);                         // :379
   if (agent == null || agent.encode(tgt, this)) {                 // si NO hay agent -> encodea IGUAL
      ObixUtils.encode(this, obj, bob, tgt);                       // encode genérico del BObject
      Property[] kids = ...;                                       // slots hijos
      if (agent != null) kids = agent.getChildren(kids);
      for (kid : kids)
         if (agent == null || agent.encodeChild(kid, this))
            this.encodeTarget(kid);                                // recursa en cada slot
   }
}

private BIObixAgent getAgent(OrdTarget tgt) {                      // :592
   AgentList list = obj.getAgents(tgt);                            // agents registrados para el TIPO
   ... return (agente cuyo type.is(BIObixAgent.TYPE)) o null si ninguno;
}
```
Mapeo de tipos — `com.tridium.obix.util.ObixUtils.encode()`:
```java
public static void encode(...) {                  // :217
   ... else if (val instanceof BValue) {
      if (val instanceof BSimple) encodeSimple(enc, obj, (BSimple)val, cx);  // :246
   }
}
static void encodeSimple(...) {                    // :302
   if (val instanceof BINumeric) encodeNumeric(...);          // -> <real>
   else if (val instanceof BString) obj.setVal(val.encodeToString());  // :383 -> <str>
   ...
}
```

### Veredicto 76.4 (la pregunta concreta)
**SÍ — un `BAbstractService` (o cualquier `BComponent`) con un slot frozen `baja:String output` se expone automáticamente en `/obix/config/<path>/output` como `<str ... val="..."/>`, sin código oBIX extra.** Razón empírica:
1. El encoder es **genérico**: `encodeTarget` → `encodeData` recorre los slots de CUALQUIER BComponent y recursa.
2. `getAgent()` devuelve **null** para un tipo sin agent oBIX registrado, y el `if (agent == null || ...)` igual ejecuta el encode genérico. El `BControlPointAgent` NO es requisito — solo añade ops (`set/`, `override/`) y el contract de punto a los control points.
3. `ObixUtils.encodeSimple` mapea `BString` → elemento oBIX `<str>` con `val = encodeToString()`.

**NO hace falta:** implementar `BIObixEncodable`, registrar un agent, agregar un facet, ni que sea un `BControlPoint`. `BIObixEncodable` es un override OPCIONAL (solo si querés serialización custom). `BObixServer`/`BIObixEncodable` están en bajadoc oficial = API soportada.

### Caveats operacionales 76.4
- **Path real:** un `BAbstractService` vive bajo `Config/Services`, así que el ORD es `/obix/config/Services/<nombre>/output` (no `/obix/config/<nombre>/output` a menos que lo montes directo en Config).
- **Read-only:** el slot se expone para LECTURA (`<str>` con `val`). Para escritura oBIX (PUT/POST) harían falta flags/handler de writability — pero el caso de uso es exponer `output` para leer, así que OK. (Coincide con Bloque 46 #891 / oBIX write protocol #665: solo `facets`/`wsAnnotation` son writable por slot; el resto read.)
- **RBAC:** el usuario oBIX necesita permiso de lectura (categoría) sobre el componente, igual que cualquier punto. (Bloque permisos N4.)
- **driver oBIX habilitado:** el servidor oBIX `/obix` requiere el feature `tridium, obixDriver` licenciado y el servicio montado (ya lo está si hoy ves un StringPoint en `/obix/config/...`).

> Conclusión de diseño: tu plan funciona tal cual. Reemplazar el StringPoint por un `BAbstractService` con slot frozen `baja:String output` lo expone igual por oBIX (como `<str>`), porque el encoder del servidor oBIX hace introspección genérica de slots y el modelo de Agents es solo enriquecimiento opcional para tipos conocidos.

---

## 76.5 — Resumen ejecutivo (4 puntos)

| # | Pregunta | Veredicto |
|---|----------|-----------|
| 1 | `com.tridium.json` usable / JSONWriter→String | **SÍ** escribe a `StringWriter`/`StringBuilder` (ctor `Appendable`). Es `org.json` re-empaquetado en `nre.jar`. **Interna/no soportada** (sin bajadoc), pero estable 4.13↔4.14. Alternativa cero-riesgo: bundlear writer propio/Gson shaded. |
| 2 | `BControlPoint.getOutStatusValue()` | **Existe**, `public abstract BStatusValue getOutStatusValue()`. Leer genérico: `getValueValue()` (BValue) + `getStatus()` (BStatus). API soportada, 4.13 y 4.14. |
| 3 | Deps de módulo / profiles | Declarar `control-rt` (solo necesita `baja`). `driver-rt` es PESADO (15 deps) y **solo hace falta si tocás `BDevice`**. Profile `rt`, sin `-wb`. |
| 4 | oBIX expone BComponent custom | **SÍ automático** como `<str name="output">` en `/obix/config/Services/<nombre>/output`. Encoder genérico (`ObixEncoder`+`ObixUtils`); agent por tipo es opcional. No requiere interfaz/facet/ser control point. |

### Cross-version (compile 4.13 / deploy 4.14)
Todas las APIs usadas (`BControlPoint.getOutStatusValue`, `BStatusValue`, `BAbstractService`, oBIX server encoder, `com.tridium.json.JSONWriter`) tienen superficie idéntica en 4.13 y 4.14. El único punto de fragilidad teórica es `com.tridium.json` por ser interna — pero verificado estable entre ambas versiones.
