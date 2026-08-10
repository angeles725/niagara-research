# Bloque 423 — `galileoKitPx`: kitPx OEM Honeywell Galileo — PIN-based access control sobre widgets PX (license-gated, ZKM obfuscado)

> Research del focus **`px-tail`**, gap **P3** (LOW). Documenta `galileoKitPx-wb` — el módulo kitPx OEM
> de Galileo Supervisor (Honeywell). Responde las cuatro preguntas del gap: ¿license-gated? ¿ofuscado?
> ¿qué añade sobre kitPx base de Tridium? ¿es binding masivo, templating, o widgets? Scope: las 19
> clases del pipeline `-wb` (única distribución; no hay `-rt` ni `-ux` en este módulo). Contraste
> directo con `easyBinding` [Bloque 207] (OEM Honeywell, binding masivo + AES) para validar si el
> patrón OEM se repite.
>
> **Versión del sujeto**: `galileoKitPx-wb` v1.4.2813.0, vendor `Honeywell.Galileo`, build
> 2024-11-18 (buildMillis 1731911711778). Dependencia Niagara ≥ 4.14.
>
> **Sources (READ-ONLY, decompilado CFR post-ZKM-deobf)**:
> `/home/cristian/modules/Prototipos/modulos/organized/galileoKitPx/galileoKitPx-wb/vineflower/`
> (fuente de-obfuscada; ZKM strings descifrados). Pre-de-obfuscación preservado en
> `vineflower.obfuscated-bak/`. Nota de proceso: `DEOBFUSCATION-NOTE.md` en el mismo directorio.
> `module.xml` leído desde `galileoKitPx-wb/extracted/META-INF/module.xml`.
>
> **Method**: lectura directa inline de las 19 clases (gap acotado, sin delegación). Verificación
> de obfuscación via `DEOBFUSCATION-NOTE.md` + comparación directa entre `vineflower/` y
> `vineflower.obfuscated-bak/`.
>
> **Markers** (METHODOLOGY §3): `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> **Connections**: [Bloque 207] (easyBinding, OEM Honeywell — punto de comparación directo),
> [Bloque 203] (kitPx packs base Tridium), [Bloque 193] (kitPx bindings, BValueBinding).

---

## 423.1 — Identidad del módulo: Honeywell Galileo, `-wb` único, 19 clases `[CERT]`

`galileoKitPx` no es un OEM independiente: es un módulo de **Honeywell Galileo Supervisor**, la misma
empresa que easyBinding (Honeywell), bajo la marca de producto "Galileo":

| Campo | Valor |
|---|---|
| vendor | `Honeywell.Galileo` |
| package base | `com.honeywell.galileo.kitpx` |
| module name | `galileoKitPx` |
| runtime profile | `-wb` únicamente |
| dependencia OEM | `galileoSupervisor-rt` v1.4 (Honeywell.Galileo) |
| dependencia Niagara | `kitPx-wb`, `bajaui-wb`, `gx-rt/wb`, `hx-wb`, `fox-rt`, `workbench-wb`, `control-rt` ≥ 4.14 |

`[CERT]` `galileoKitPx-wb/extracted/META-INF/module.xml` (atributos `vendor`, `vendorVersion`,
`runtimeProfile`; bloque `<dependencies>`).

Las 19 clases se agrupan en cuatro paquetes:

| Paquete | Clases | Contenido |
|---|---|---|
| `bindings` | 6 | BGalileoBinding + 4 binding concretas + HxPx render agent |
| `converter` | 3 | Converters booleano / status / text-command enriquecidos |
| `widgets` | 7 | BEasyPicture + BEasyLabel + HxPx agent + AWT impl + image cache |
| `util` | 3 | GalileoBindingUtil + GalileoConstant + KitpxUtils |

`[CERT]` `find galileoKitPx-wb/vineflower -name "*.java"` = 19 resultados (conteo directo).

---

## 423.2 — Obfuscación ZKM (Zelix KlassMaster) + de-obfuscación local `[CERT]`

El JAR original **SÍ está obfuscado**: usa **ZKM (Zelix KlassMaster)** con string encryption. El
`vineflower/` del corpus es la versión ya de-obfuscada (141 strings descifrados según la nota).

Evidencia directa: en el bak previo a de-obfuscación, `KitpxUtils` declara

```java
// vineflower.obfuscated-bak/com/honeywell/galileo/kitpx/util/KitpxUtils.java:19
private static final String[] z;
```

y en `a()` usa `z[0].getBytes(...)`, `z[2]` para la ignore-list, etc. Tras de-obfuscación, el
`vineflower/` tiene los strings en claro: `"honEasyBinding"`, `"module://icons/"`,
`"module://easybinding/"`, `"Utility class"`, etc.

`[CERT]` `DEOBFUSCATION-NOTE.md:1-11` (ZKM, 141 strings, proceso deobf); comparación directa
`vineflower.obfuscated-bak/KitpxUtils.java:19` (array `z[]`) vs `vineflower/KitpxUtils.java:9`
(campo `z` = string literal).

**Diferencia clave con easyBinding**: easyBinding (B207) usa un XOR `z[]` de runtime propio (código
en el módulo); galileoKitPx usa ZKM, un obfuscador comercial externo. Ambos ocultan strings en el
bytecode; el mecanismo difiere.

---

## 423.3 — License gating: XOR de imágenes (solo assets, no bindings) `[CERT]`

`KitpxUtils` tiene una **license gate específica para el descifrado de imágenes** mediante XOR:

```java
// KitpxUtils.java:28-32 (vineflower/, campo c = key derivada)
private static void a() {
    byte[] byArray = "honEasyBinding".getBytes(StandardCharsets.UTF_8);
    c = byArray[byArray.length - 5] + byArray.length;  // key XOR estática
}

// KitpxUtils.java:109-117
private static byte[] a(byte[] byArray) {
    int n = 0;
    if (b != null) {                             // b = Feature; si null → no descifra
        for (int j = n; j < byArray.length - n; ++j) {
            byArray[j] = (byte)(byArray[j] ^ c); // XOR con key derivada
        }
    }
    return byArray;
}
```

`[CERT]` `com/honeywell/galileo/kitpx/util/KitpxUtils.java:28-32` (derivación key); `:41-57`
(`setLicense`); `:109-117` (XOR gate).

```java
// KitpxUtils.java:41-57
public static void setLicense(Feature feature) {
    try {
        if (b == null) { b = feature; d = true; }
    } catch (Exception ...) { ... }
}
```

`[CERT]` `com/honeywell/galileo/kitpx/util/KitpxUtils.java:41-57`.

**Lista de exclusión** (assets que NUNCA se descifran, aunque la licencia esté activa):

```java
// KitpxUtils.java (bloque static)
e.add("module://icons/");
e.add("module://easybinding/");
```

`[CERT]` `com/honeywell/galileo/kitpx/util/KitpxUtils.java:171-174`.

La exclusión de `module://easybinding/` confirma que galileoKitPx y easyBinding coexisten en el
mismo Supervisor Honeywell y que Galileo sabe explícitamente qué assets NO son suyos.

**Diferencia con easyBinding (B207)**: easyBinding usa AES (`SecretKeySpec`, `javax.crypto`); galileoKitPx
usa XOR con key estática derivada del nombre del feature. easyBinding es más robusto criptográficamente;
galileoKitPx es más liviano. Ambos son license-gated para sus assets.

La license gate solo afecta a la capa de imagen (`BEasyPicture`, `BEasyLabel`, `EasyImageManager`).
Las bindings PIN no tienen license check propio — requieren `galileoSupervisor-rt` instalado y
el mixin `BGalileoUserPin` presente en el usuario, pero no consultan un feature Niagara. `[INFER]`

---

## 423.4 — La feature central: PIN-based access control sobre widgets PX `[CERT]`

El aporte principal de galileoKitPx sobre kitPx base de Tridium NO es auto-binding ni templating:
es un sistema de **control de acceso basado en PIN de usuario** que añade dos propiedades a cada
binding:

| Propiedad | Tipo | Default | Semántica |
|---|---|---|---|
| `visibilityPin` | `int` | -1 | Nivel mínimo de PIN para que el widget sea **visible** |
| `actionPin` | `int` | -1 | Nivel mínimo de PIN para que el widget esté **habilitado** (clickable) |

`[CERT]` `BGalileoBinding.java:24` (`@NiagaraProperties` con ambas propiedades, defaultValue `-1`).

El nivel del usuario se lee del mixin `BGalileoUserPin` almacenado en el slot
`galileoSupervisor_GalileoUserPin` del objeto `BUser`:

```java
// GalileoBindingUtil.java:62-78
public static void enableWidgetBasedOnUserPin(BUser bUser, int n, int n2, BWidget bWidget) {
    BGalileoUserPin bGalileoUserPin =
        (BGalileoUserPin)bUser.get(SlotPath.escape("galileoSupervisor_GalileoUserPin"));
    if (bGalileoUserPin != null) {
        int n3 = bGalileoUserPin.getUserPinLevel();
        bWidget.setEnabled(n3 >= n);     // n = actionPin
        bWidget.setVisible(n3 >= n2);    // n2 = visibilityPin
    } else {
        bWidget.setEnabled(false);       // sin mixin: bloqueo total
        bWidget.setVisible(false);
    }
}
```

`[CERT]` `com/honeywell/galileo/kitpx/util/GalileoBindingUtil.java:62-78`.

Default `-1` significa "sin restricción": `userPinLevel >= -1` siempre es `true` para cualquier
nivel real (los niveles son enteros ≥ 0). `[INFER]` (la convención de nivel mínimo -1 = libre no
está declarada explícitamente en el código, se infiere del default y de la comparación `>=`).

El sistema opera en **dos perfiles simultáneamente**:
- **Workbench / AWT**: via `BWbShell.getActiveOrdTarget()` → `BFoxSession` → `getUserFromSession()`
- **Hx / web**: via `BHxWidgetShell.getActiveOrdTarget().getUser()`

`[CERT]` `BGalileoValueBinding.java:64-80` (Wb path); `BGalileoPopupBinding.java:274-280` (Hx path).

---

## 423.5 — Las 6 clases de bindings: kitPx subclasses + PIN `[CERT]`

Todas las bindings heredan el comportamiento PIN de `BGalileoBinding` y extienden las primitivas
**kitPx de Tridium** (B193):

| Clase | Superclase | Propósito |
|---|---|---|
| `BGalileoBinding` | `BValueBinding` (baja base) | Clase base; añade `visibilityPin` + `actionPin` |
| `BGalileoValueBinding` | `BGalileoBinding` | Binding de valor genérico; `targetChanged()` aplica PIN |
| `BGalileoBoundLabelBinding` | `BBoundLabelBinding` (kitPx Tridium) | BoundLabel kitPx con PIN |
| `BGalileoActionBinding` | `BActionBinding` (kitPx Tridium) | Action binding con PIN |
| `BGalileoPopupBinding` | `BBinding` (baja base) | Popup (mouse click→dialog) + PIN + Wb+Hx |
| `BHxPxGalileoPopupBinding` | `BHxPxBinding` | Render agent Hx para `GalileoPopupBinding` |

`[CERT]` `BGalileoBoundLabelBinding.java:40` (`extends BBoundLabelBinding`);
`BGalileoActionBinding.java:40` (`extends BActionBinding`); `BGalileoPopupBinding.java:80`
(`extends BBinding`); `BHxPxGalileoPopupBinding.java:63` (`extends BHxPxBinding`).

`BGalileoPopupBinding` abre un `BNiagaraWbDialog` con título, posición y tamaño configurables:

```java
// BGalileoPopupBinding.java:222-225
BNiagaraWbDialog bNiagaraWbDialog = new BNiagaraWbDialog(
    BPopupProfile.TYPE, bWbShell, bOrd,
    this.getTitle(), this.getPosition(), this.getSize(), this.getModal());
bNiagaraWbDialog.open();
```

`[CERT]` `com/honeywell/galileo/kitpx/bindings/BGalileoPopupBinding.java:222-225`.

En la capa Hx (`BHxPxGalileoPopupBinding`), el popup emite JavaScript `hx.popup('url', x, y, w, h,
external, modal, 'title')` y aplica el gate PIN via `GalileoBindingUtil.getWidgetEnabledAndVisible()`.
`[CERT]` `BHxPxGalileoPopupBinding.java:127-141`.

---

## 423.6 — Los 3 converters: status-aware, más ricos que el base Tridium `[CERT]`

Los converters galileo extienden `BConverter` (base baja) con **estados de equipo adicionales** que
el `IBooleanToSimple` estándar de Tridium no tiene:

### BGalileoBooleanToSimple — 9 estados vs 2 en Tridium base

```java
// BGalileoBooleanToSimple.java:49
@NiagaraType(adapter=@Adapter(from="baja:IBoolean", to="baja:Simple"))
@NiagaraProperties(value={
    @NiagaraProperty(name="on",         type="BSimple", ...),
    @NiagaraProperty(name="off",        type="BSimple", ...),
    @NiagaraProperty(name="overrideOn", type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="overrideOff",type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="alarmOn",    type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="alarmOff",   type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="down",       type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="disabled",   type="BSimple", ...),  // ← extra
    @NiagaraProperty(name="fault",      type="BSimple", ...)   // ← extra
})
```

`[CERT]` `com/honeywell/galileo/kitpx/converter/BGalileoBooleanToSimple.java:48-62`.

Lógica de prioridad en `convert()`: override > alarm > disabled > down > fault > on/off base.
`[CERT]` `BGalileoBooleanToSimple.java:152-207`.

### BGalileoStatusToSimple — 6 estados (BIStatus → Simple)

`from="baja:IStatus"` con `normal / override / alarm / down / fault / disabled`. Prioridad:
disabled > down > alarm > fault > override > normal.
`[CERT]` `com/honeywell/galileo/kitpx/converter/BGalileoStatusToSimple.java:46-47`.

### BGalileoTextCommToSimple — text command point con 9 salidas

`from="baja:IStatus"` para `BStringPoint`: mapea `out.value` `"I"`→on, `"O"`→off, numérico→number,
texto→text; superpone estados de status (override/alarm/disabled/down/fault).
`[CERT]` `com/honeywell/galileo/kitpx/converter/BGalileoTextCommToSimple.java:46-48`.

Estos converters son reutilizables directamente desde cualquier página PX con cualquier binding
(no solo con las bindings Galileo), ya que son adapters estándar del NRE registrados en `module.xml`.
`[INFER]` — el mecanismo de registro es el mismo que los converters base de Tridium (B193).

---

## 423.7 — Los widgets y la infraestructura de imagen XOR `[CERT]`

### BEasyPicture — imagen PX con descifrado XOR opcional

```java
// BEasyPicture.java:60-68
@NiagaraProperties(value={
    @NiagaraProperty(name="image",     type="BImage",     ...),
    @NiagaraProperty(name="scale",     type="BScaleMode", ...),
    @NiagaraProperty(name="halign",    type="BHalign",    ...),
    @NiagaraProperty(name="valign",    type="BValign",    ...),
    @NiagaraProperty(name="animate",   type="BBoolean",   defaultValue="BBoolean.TRUE"),
    @NiagaraProperty(name="encrypted", type="BBoolean",   defaultValue="BBoolean.TRUE")
})
public class BEasyPicture extends BWidget {
```

`[CERT]` `com/honeywell/galileo/kitpx/widgets/BEasyPicture.java:59-62`.

En `update()` (render Hx), el widget llama `KitpxUtils.getPlainBytesOfImageObject()` para descifrar
(si `encrypted=true` y licencia activa) y emite la imagen como `data:image/<ext>;base64,...` inline.
Soporta PNG, GIF (animado) y SVG. `[CERT]` `BHxPxEasyPicture.java:55-91`.

### BEasyLabel — label con imagen descifrada en `paint()`

Extiende `BLabel` (bajaui base); en `paint()`, `paintIcon()` y `animate()` llama
`KitpxUtils.getPlainImageOfBytes()` sobre `this.getImage()`. `[CERT]` `BEasyLabel.java:66-85`.

### EasyImageManager + EasyImageData — cache de imágenes con async loader

`EasyImageManager` mantiene un `HashMap<String, EasyImageData>` con imágenes cacheadas; un thread
daemon ("Ui:ImageManager") carga en background los assets no-módulo y llama
`KitpxUtils.getDecryptedBytes()` antes de decodificar. Cache TTL: 900 segundos para entradas > 10 KB.
`[CERT]` `EasyImageManager.java:87-128`; `:86-87` (eviction: `h > 10240 && l - e > 900000L`).

---

## 423.8 — Veredicto comparativo OEM: ¿el patrón easyBinding se repite? `[CERT]` + `[INFER]`

**Respuesta corta**: el patrón OEM SE REPITE en la estructura (Honeywell, license-gated para
imágenes, obfuscado, construido sobre kitPx de Tridium), pero el PROPÓSITO es radicalmente
diferente.

### Tabla comparativa

| Dimensión | `easyBinding` [B207] | `galileoKitPx` [B423] |
|---|---|---|
| OEM | Honeywell (brand principal) | Honeywell Galileo (producto Supervisor) |
| Vendor string | `com.honeywell.easybinding` | `com.honeywell.galileo.kitpx` |
| License gate | SÍ — para wizard + assets | SÍ — SOLO para descifrado de assets |
| Cifrado assets | AES (`SecretKeySpec`, `javax.crypto`) | XOR con key derivada de `"honEasyBinding"` |
| Obfuscación | `z[]` XOR custom (runtime) | ZKM string encryption (obfuscador comercial) |
| Propósito central | Auto-binding: wizard hornea palettes custom | PIN-based access control: habilita/oculta widgets por nivel de usuario |
| ¿Es binding masivo? | SÍ — `BIEbConverter` auto-selecciona converter | NO — las bindings son kitPx con PIN añadido |
| ¿Es templating? | Parcialmente — palette wizard genera tipos pre-bound | NO |
| ¿Son widgets propios? | `BEasyBindingWidget`, `BEasyLabel`, `BEasyPicture` (rebrand) | `BEasyPicture`, `BEasyLabel` (imagen cifrada) |
| Construido sobre kitPx | SÍ — `BEasyBaseBinding extends BSecureBoundLabelBinding` | SÍ — `BGalileoBoundLabelBinding extends BBoundLabelBinding` |
| Perfil | `-wb` (authoring) + parte en `-rt` + `-ux` | `-wb` únicamente |
| Dependencia cross-módulo | `NiagaraVirtualDeviceExt` para rebind | `galileoSupervisor-rt` para `BGalileoUserPin` |

`[CERT]` para cada fila: easyBinding → B207 §§207.1-207.5; galileoKitPx → §§423.3-423.7 (este bloque).

### Qué comparten (patrón OEM confirmado)

1. **Mismo vendor raíz**: ambos son Honeywell; galileoKitPx incluso deriva su XOR key del string
   `"honEasyBinding"` — referencia literal al otro módulo. `[CERT]` `KitpxUtils.java:37`.
2. **License gate para assets**: ambos cifran imágenes y las descifran solo con licencia activa.
3. **Obfuscación del JAR**: ambos usan alguna forma de string obfuscation en el bytecode.
4. **Construidos sobre kitPx Tridium**, no en paralelo.
5. **Compatibles entre sí**: galileoKitPx excluye explícitamente `module://easybinding/` de su
   descifrado, lo que implica coexistencia en el mismo Supervisor. `[CERT]` `KitpxUtils.java:172`.

### Qué difiere (propósito distinto)

easyBinding automatiza el AUTHORING (un ingeniero corre el wizard, genera palettes, los operadores
arrastran widgets ya cableados). galileoKitPx implementa RBAC (Role-Based Access Control) sobre
PX: el administrador configura `actionPin`/`visibilityPin` por widget; el sistema compara el
`userPinLevel` del usuario autenticado y habilita/oculta en tiempo de render. Son capas ortogonales
que operan en momentos distintos del ciclo PX. `[INFER]`

---

## 423.x — Connections

- **[Bloque 207]** (easyBinding, OEM Honeywell): punto de comparación directo. galileoKitPx comparte
  la estructura OEM (license-gated, obfuscado, sobre kitPx) pero su propósito es acceso RBAC,
  no auto-binding. El string `"honEasyBinding"` en la key XOR de KitpxUtils es una referencia
  literal al módulo hermano — confirman coexistencia en el mismo Supervisor Honeywell.

- **[Bloque 193]** (kitPx bindings base Tridium): galileoKitPx extiende `BBoundLabelBinding` y
  `BActionBinding` directamente, igual que easyBinding extiende `BSecureBoundLabelBinding`.
  El patrón "OEM subclasea el binding kitPx y añade comportamiento" se repite en ambos.

- **[Bloque 203]** (kitPx packs base): contraste de propósito — los packs Tridium (Graphics/Hvac/
  N4svg) son palettes BOG; galileoKitPx no aporta una palette de símbolos sino bindings de control
  de acceso. Propósito radicalmente diferente al de los packs.

- **[Bloque 194]** (perfiles Wb/Hx): galileoKitPx soporta ambos perfiles (`BGalileoPopupBinding`
  en Wb + `BHxPxGalileoPopupBinding` en Hx; `BHxPxEasyPicture` en Hx) sin un `-ux` dedicado —
  el soporte Hx está integrado directamente en el `-wb` como singletons.
