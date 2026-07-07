# Bloque 207 — `easyBinding`: el módulo OEM Honeywell de auto-binding sobre kitPx (license-gated, assets cifrados)

> Research del focus **`px-editor-deep`** (gap X6, descubierto en X2): el subsistema **easyBinding** — un módulo
> **OEM Honeywell** (`com.honeywell.easybinding`, NO Tridium) de "binding fácil": un widget único que auto-detecta el
> tipo del punto y arma su converter, bundleando value/alarm/override sobre un `BBoundLabel` kitPx. License-gated, con
> assets de imagen **cifrados AES por marca**. Construido SOBRE kitPx (B193), no un reemplazo. Correcciones vs el
> pre-conteo: FQCN real `com.honeywell.easybinding`; clases reales rt=11/wb≈46/ux=5 (el conteo previo de 119 incluía
> duplicados de pipelines procyon+vineflower). NO cubre `svgBatik` (X4, el último gap).
>
> Sources (preservados §5): `sources/decompiled/easyBinding/{rt,wb,ux}/` — 11 rt + 7 wb-core + 5 ux `.java` (Vineflower).
> Barrido delegado (sonnet) 2026-07-06; 6 citas load-bearing token-checked literal. Method: lectura READ-ONLY del
> decompilado. **Nota §5**: el código está PARCIALMENTE OBFUSCADO (arrays `z[]` XOR-decoded en runtime) — la ESTRUCTURA
> es `[CERT]`, pero los VALORES de strings obfuscados (nombres de marca, tags exactos) son `[INFER]`.
> Markers (§3): `[CERT]` `file:line` · `[INFER]`. Tipo: EVIDENCE block. Citas relativas a `easyBinding/`.
>
> Capa PX (binding OEM). Connects [Block 193] (kitPx bindings), [Block 186] (BValueBinding), [Block 200] (templates/
> virtual points), [Block 204] (bajaux shims), [Block 197] (síntesis).

---

## 207.1 — Qué es easyBinding: OEM Honeywell, auto-binding sobre kitPx `[CERT]`

easyBinding es un módulo **Honeywell**, no Tridium: `package com.honeywell.easybinding.service`
(`rt/BEasyBindingSupportService.java:1`). Su idea: en vez de componer a mano un `BoundLabel` kitPx + sub-widgets de
alarma/override + elegir converters (B193), un único **`BEasyBindingWidget`** (`wb/BEasyBindingWidget.java`,
`BWidget implements BIAgent`) bundlea `valueBinding`/`alarmBinding`/`overrideBinding` + imágenes value/alarm/override +
un `content` (`BEasyBindingCanvasPane`). `[CERT]`

**Construido SOBRE kitPx, no en paralelo.** `BEasyBindingCanvasPane` embebe un `com.tridium.kitpx.BBoundLabel` literal
(`import` en `wb/BEasyBindingCanvasPane.java:4`) como su text-overlay, y las bindings easy descienden de la línea kitPx:
`BEasyBaseBinding extends BSecureBoundLabelBinding` (`wb/BEasyBaseBinding.java:18`). easyBinding es una **capa de authoring
de más alto nivel encima de las primitivas de binding de kitPx**. `[CERT]`

**"Easy" = auto-bound, no pre-bound.** `BEasyValueBinding.targetChanged()` inspecciona el tipo runtime del punto target
(Boolean/Numeric/Enum) y auto-construye el converter (`BIEbConverter`, `wb/converter/BIEbConverter.java`, unifica lo que
kitPx hace con `BStatusBooleanToSimple`/`BEnumToSimple`/`BNumericToSimple` separados en un `convert()` que despacha por
`instanceof`) desde los slots de imagen del propio widget — sin que el usuario elija converter. `[CERT]` `[INFER: auto-wire en bind]`

## 207.2 — El SupportService + el RPC bridge `[CERT]`

`BEasyBindingSupportService` (`rt/.../service/BEasyBindingSupportService.java:33`, `BAbstractService`) es un servicio de
station toggle-able (`getEnabled()`) con dos acciones: `updateVirtualEasyBindings` → submite el
`BEasyBindingNiagaraVirtualSupportJob` (§207.4), y `updatePxPagesWithEncryptedEasyPallet` → submite
`BUpgradeToEncryptedEasyWidgets`. `[CERT]`

`BEasyBindingUtil` (`rt/.../service/BEasyBindingUtil.java`, `@NiagaraRpc transports=box`) es el puente RPC para la UI:
`getNavTree` camina el árbol filtrado a `BControlPoint` bajo redes de driver reconocidas (Bacnet/Niagara/ModbusTcp…),
`checkFeatureLicense`/`checkEdgeController` gatean la UI por licencia, `save`/`saveTempFile` persisten config, y
`getBase64Image` descifra + Base64-encodea imágenes para el picker. `[CERT]`

## 207.3 — Los widgets y las DOS familias de binding `[CERT]`

`BEasyBindingWidget` maneja el `text` del `BBoundLabel` embebido programáticamente vía `BObjectToString` con formato
`%out.value%`. Existen **dos familias de binding paralelas** (gotcha — fácil de confundir por el naming `Easy` vs `Eb`):

| Familia | Superclase | Consumidor | Agente sobre |
|---|---|---|---|
| `BEasy{Value,Alarm,Override}Binding` | `BEasyBaseBinding extends BSecureBoundLabelBinding` (linaje kitPx) | runtime PX | `workbench:WebWidget` |
| `BEb{Value,Alarm,Override}Binding` | `javax.baja.ui.BValueBinding` directo (B186, sin ancestro kitPx) | preview web del wizard (JS-bridge) | `BWebWidget` |

Las `BEb*` serializan `getOrd().toString()` en una propiedad JSON `data`/`valueOrd`/`alarmOrd`/`overrideOrd` del
`BWebWidget` padre — el binding puente-JS del live-preview del wizard, distinto del `BEasy*` de runtime. `[CERT]`

## 207.4 — Los dos gotchas rt: rebind virtual→real y assets cifrados `[CERT]` + `[INFER]`

**1. `BEasyBindingNiagaraVirtualSupportJob` es una migración de REBINDING, no crea puntos** (`rt/.../service/
BEasyBindingNiagaraVirtualSupportJob.java:36`, `BSimpleJob`). Escanea los `BPxFile`, encuentra widgets
`easyBinding:EasyBindingWidget` cuyas bindings apuntan a un **punto placeholder de un `NiagaraVirtualDeviceExt`**
(`com.tridium.nv.comps`, un driver Niagara Virtual usado como punto de diseño), busca el `BControlPoint` real cuyo
`pointId` matchea, y **reescribe la ORD del binding** (`var1.setOrd(var3)`, `:290`) al punto real desplegado. `[CERT]`
Es el paso "diseño→dispositivo real": los easy widgets se autoran contra puntos virtuales placeholder y este job los
re-apunta a los puntos comisionados — el link arquitectural con templates (B200 usa virtual points como placeholder de
instanciación). `[INFER]` (no hay import directo a las clases de template; el link es por los virtual points)

**2. `BUpgradeToEncryptedEasyWidgets` NO cifra — RETAGGEA.** Hace string-replace de tags legacy a tags easy
(`Picture`→`EasyPicture`, `Label`→`EasyLabel`) sobre los PX files (`rt/.../BUpgradeToEncryptedEasyWidgets.java:140-142`,
vía arrays `z[]` obfuscados — los tags exactos son `[INFER]`, el mecanismo de replace es `[CERT]`). Los `EasyPicture`/
`EasyLabel` son los que rutean los bytes de imagen por el gate `decrypt` al pintar. `[CERT-estructura]`/`[INFER-tags]`

**El cifrado real** vive en `EncryptDecrypt` (`rt/EncryptDecrypt.java:44`): deriva una key **AES** de un string (el nombre
del license Feature) — `new SecretKeySpec(this.a.getBytes(UTF_8), "AES")` (el algoritmo `z[1]` obfuscado, verificado como
AES por el import `javax.crypto.spec.SecretKeySpec`, `:9`). `[CERT-estructura]` Efecto: los assets de imagen custom
(elegidos por marca vía el Easy Palette wizard) quedan cifrados AES-at-rest, keyed al feature de licencia OEM, así una
palette de una marca no puede abrir sus imágenes sin esa licencia. Las marcas OEM específicas (Honeywell/Trend/CentraLine/
etc. reportadas por el barrido) están tras strings obfuscados `z[]` → `[INFER]`. `[INFER]`

## 207.5 — El palette wizard + la superficie ux/web `[CERT]`

**El mecanismo de "widget pre-bound"**: `BEasyPaletteBuilder` (`wb/tool/BEasyPaletteBuilder.java`, `BWbTool` del menú
Tools, gated por `EbLicenseUtil.checkEasyBindingFeature()`) abre un `BWizard` (`EasyPaletteWizardMain`) que, vía un
`EasyPaletteJarGenerator`, hornea un `BPaletteFile` (palette JAR real) con tipos `BEasyBindingWidget`/`BEasyLabel` y las
imágenes elegidas pre-cargadas. Un ingeniero corre el wizard una vez (elige íconos value/alarm/override por tipo de
equipo) → genera una palette custom; los usuarios arrastran un widget generado y solo setean la ORD del punto — el resto
del wiring ya está horneado + `targetChanged()` lo completa. `[CERT]` `[INFER: flujo de uso]`

**La superficie ux/web** son shims `BSingleton` (patrón B199/B202/B204): `BBaseWidget` (`ux/.../BBaseWidget.java:13`,
`BSingleton implements BIJavaScript, BIFormFactorMax`), `BEasyBinding` (extends `BBaseWidget`, XOR-decodea una ORD `JsInfo`
a su módulo JS), `BExtRequireJsConfig` (`BIRequireJsConfig`, registra el path RequireJS). `BEasyBindingView` es un `BWbView`
que hostea el `BWebWidget` (el SPA web del wizard que habla con los RPC de `BEasyBindingUtil`). Confirma que easyBinding
también renderiza en la superficie bajaux (B204). `[CERT]`

## 207.6 — Connections

- **[Block 193]** (kitPx bindings): easyBinding es una capa de authoring ENCIMA de kitPx — `BEasyBaseBinding extends
  BSecureBoundLabelBinding` y `BEasyBindingCanvasPane` embebe un `BBoundLabel` kitPx; NO es una reimplementación paralela.
- **[Block 186]** (BValueBinding): la familia `BEb*Binding` (preview web del wizard) extiende `BValueBinding` directo — las
  únicas bindings del módulo sin ancestro kitPx.
- **[Block 200]** (templates/virtual points): el rebind virtual→real de `BEasyBindingNiagaraVirtualSupportJob` (§207.4) es el
  puente con la instanciación de templates (los Niagara Virtual points son el placeholder de diseño); link arquitectural.
- **[Block 204]** (bajaux shims): la superficie ux usa el mismo patrón `BSingleton`+`BIJavaScript`+`JsInfo` que webChart
  (B199) y los field editors (B202).
- **[Block 194]** (media/perfiles): easyBinding tiene variantes Hx (`BHxPxEasyBindingWidget`) además de Wb/bajaux — el mismo
  eje de perfiles Wb/Hx que B194.
- **Contexto del cliente**: es un módulo OEM Honeywell license-gated con cifrado de assets por marca — relevante para
  despliegues Honeywell (MX60/OptimizerSupervisor). El cifrado ata las palettes a la licencia del OEM.
- **Fuera de scope** (último gap): **X4 `svgBatik`** — el motor de render/import SVG (B196/B203 lo mencionaron sin abrir).
