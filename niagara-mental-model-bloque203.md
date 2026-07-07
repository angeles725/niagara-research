# Bloque 203 — Los packs de widgets gráficos (kitPxGraphics/Hvac/N4svg/Building): palettes BOG, no código

> Research del focus **`px-editor-deep`** (gap X3): los packs de widgets gráficos HVAC/SVG —
> `kitPxGraphics`, `kitPxHvac`, `kitPxN4svg`, `kitPxBuilding`. Hallazgo central: los tres primeros NO tienen
> código Java — son **bibliotecas de símbolos empaquetadas como `module.palette` (BOG)** de widgets pre-bound +
> miles de recursos de imagen; el "widget" es data pura (una imagen switcheada por un converter). `kitPxBuilding`
> es la excepción: sí trae componentes Java tipados. Confirma B196 (animación = data binding). NO cubre `svgBatik`
> (X4, el motor de render SVG) ni `bajaux` (X5).
>
> Sources (preservados §5): `sources/decompiled/kitPx-graphics-packs/` — 3 `module.palette` (BOG, ~10K líneas) +
> 7 `.java` de kitPxBuilding (Vineflower). Recursos de imagen (986 png/87 gif en Graphics; 279 gif en Hvac; 973 svg
> en N4svg) NO preservados — referenciados por ord `module://`, citados por estructura/conteo. Barrido inline
> (gap liviano, evidencia directa) 2026-07-06.
> Method: lectura READ-ONLY de las palettes + `ls`/`grep` de recursos. Markers (§3): `[CERT]` `file:line`/conteo · `[INFER]`.
> Tipo: EVIDENCE block.
>
> Capa PX (librerías de widgets). Connects [Block 192] (widgets bajaui), [Block 193] (ValueBinding+converters),
> [Block 196] (animación=data binding On/Off), [Block 183] (gx:Image), [Block 197] (síntesis).

---

## 203.1 — Qué es un pack de widgets gráficos: una `module.palette` de widgets pre-bound `[CERT]`

Un pack gráfico NO es código: es un `module.palette` — un **BOG** (`<bajaObjectGraph version="4.0">`) que define un
árbol de widgets pre-construidos y pre-bindeados, organizados en `UnrestrictedFolder` por categoría (Boilers,
Chillers, Electrical…), + un directorio de recursos de imagen servidos por ord `module://<pack>/...`. `kitPxGraphics`
solo trae 986 PNG + 87 GIF + `module.palette` + `module.xml`; cero `.java`. `[CERT]` (conteo `find`)

El usuario arrastra un símbolo de la palette al canvas y obtiene un widget YA bindeado. El patrón de cada widget es un
**BoundLabel/Picture con un `ValueBinding` cuya imagen la resuelve un converter** — nada de lógica compilada. `[CERT]`

## 203.2 — `kitPxGraphics` / `kitPxHvac`: símbolos raster (BoundLabel + On/Off) `[CERT]`

En `kitPxGraphics` cada símbolo es un `kitPx:BoundLabel` con un `ui:ValueBinding` cuya `image` es un
`conv:IBooleanToSimple` que mapea `trueValue`→imagen ON (`.gif` animado) y `falseValue`→imagen OFF (`.png`):

```xml
<!-- sources/decompiled/kitPx-graphics-packs/kitPxGraphics.palette:6-14 (BoilerBuderusLeft) -->
<p n="BoilerBuderusLeft" m="kitPx=kitPx" t="kitPx:BoundLabel">
 <p n="layout" t="ui:Layout" v="0.0,0.0,150.0,80.0"/>
 <p n="bnd" t="ui:ValueBinding">
  <p n="image" m="conv=converters" t="conv:IBooleanToSimple">
   <p n="trueValue"  t="gx:Image" v="module://kitPxGraphics/Boilers/Buderus/Boiler_Buderus_Left/..._On.gif"/>
   <p n="falseValue" t="gx:Image" v="module://kitPxGraphics/Boilers/Buderus/Boiler_Buderus_Left/..._Off.png"/>
  </p>
 </p>
 <p n="compPreviewWidget" f="hR" t="kitPx:BoundLabel"> ... </p>
</p>
```

Cada widget lleva un `compPreviewWidget` (flag `f="hR"`) con la imagen OFF estática — el thumbnail de la paleta. `[CERT]`
`kitPxHvac` es idéntico en modelo: **206 widgets `kitPx:BoundLabel`** (`grep -c` sobre su palette) sobre 279 GIF. `[CERT]`

Esto CONFIRMA B196 literal: "animar" un símbolo HVAC = el `IBooleanToSimple` switchea la imagen ON/OFF según el dato
bound (un `on` boolean del equipo). El GIF animado (On) da la ilusión de movimiento; no hay tweening. `[CERT]` (remisión B196)

## 203.3 — `kitPxN4svg`: símbolos vectoriales (`ui:Picture` + SVG + converters ricos) `[CERT]`

`kitPxN4svg` sigue el mismo modelo pero con **dos diferencias**:

1. El widget es **`ui:Picture`** (no `kitPx:BoundLabel`) — un widget bajaui base, no kitPx
   (`kitPxN4svg.palette:5`), con la imagen SVG switcheada igual por `IBooleanToSimple` (`On.svg`/`Off.svg`, `:9-11`). `[CERT]`
2. Usa **recursos vectoriales SVG** (973 `.svg`: Boilers, Chillers, RTU, Electrical, ground planes) en vez de raster —
   escalables. `[CERT]`

Y soporta binding MÁS rico que el On/Off boolean: los conteos de converter en su palette muestran
`conv:IBooleanToSimple`×57, **`conv:NumericToSimpleMap`×31**, `conv:INumericToSimple`×31, `conv:IStatusToSimple`×2
(`grep -c` sobre `kitPxN4svg.palette`) — o sea muchos símbolos N4svg mapean un valor NUMÉRICO o de STATUS a distintas
imágenes (ej. un chiller con N estados), no solo dos. Es la generación más sofisticada de packs. `[CERT]` `[INFER: más moderno]`

## 203.4 — `kitPxBuilding`: la excepción con componentes Java tipados `[CERT]`

A diferencia de los tres packs de-solo-recursos, `kitPxBuilding` SÍ trae código: componentes de equipo tipados
(`sources/decompiled/kitPx-graphics-packs/kitPxBuilding/`) — `BEquipment`+`BEquipmentState`, `BDamperEquipment`+
`BDamperState`, `BDualSwitch`+`BDualSwitchState`, `BLoHiEquipment`, `BKnob`, `BPointer`, más `BKitPxBuildingBaseEnum`/
`BKitPxBuildingIEnumTypeExt` (enums de estado) y `BKitPxBuildingJsBuild` (bundle bajaux). `[CERT]` (`ls`)

Es un pack de **widgets de equipo compuestos** (un damper, un knob, un pointer de gauge) con ESTADO tipado
(enums Off/On/Alarm…), no imágenes switcheadas — más cercano a los widgets kitPx reales (B192/B193) que a una
biblioteca de símbolos. Tiene un `JsBuild` → también renderiza en bajaux (superficie web). `[INFER]` (los nombres +
el JsBuild; no se abrió el detalle de cada clase — candidato a profundización propia si se requiere)

## 203.5 — Connections

- **[Block 196]** (animación = data binding): CONFIRMADO en los packs raster/vector — el símbolo HVAC "animado" es un
  `BoundLabel`/`Picture` cuya imagen la switchea `IBooleanToSimple` (On.gif/Off.png/On.svg) según el dato bound; el GIF
  animado da el movimiento. No hay tweening (§203.2).
- **[Block 193]** (ValueBinding + converters): cada símbolo usa un `ui:ValueBinding` + un converter
  (`IBooleanToSimple`/`NumericToSimpleMap`/`IStatusToSimple`) — el mecanismo de B193/B184 aplicado a `image` en vez de
  `text`/`visible`.
- **[Block 192]** (widgets bajaui): N4svg usa `ui:Picture` (widget bajaui base), Graphics/Hvac usan `kitPx:BoundLabel`
  (widget kitPx); `kitPxBuilding` define widgets de equipo propios.
- **[Block 183]** (gx:Image): los recursos se referencian como `gx:Image` con ord `module://<pack>/...`; el pack sirve
  el binario, la palette lo bindea.
- **Contraste con [Block 199]** (webChart): webChart es render dinámico (D3/JS); estos packs son símbolos estáticos
  switcheados por dato — dos maneras distintas de "graficar" en PX.
- **Fuera de scope** (feeds): `svgBatik` (X4, cómo se rasteriza el SVG de N4svg en Workbench), el detalle de las clases
  de equipo de `kitPxBuilding` (BEquipmentState enums, BKnob/BPointer render).
