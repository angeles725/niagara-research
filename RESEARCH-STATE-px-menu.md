# RESEARCH-STATE — Focus `px-menu` (PX Menu-Button / Dropdown en Workbench)

> Focus multi-eje (METHODOLOGY §16) del target `niagara-research`. Ámbito: cómo construir un
> **"Menu Button / Dropdown Trigger"** (estilo SLDS) en el **PX Editor de Niagara N4, perfil Workbench**.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), mismo repo/hook. Corpus en Español
> (técnico EN), por continuidad con los 178 bloques previos.
> Engram topic key: `research/niagara/px-menu/{gaps,progress}`.
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 607
gaps_closed: 23
known_gaps: 33
investigable_open: 6
requires_execution_open: 3
blocked_open: 0
<!-- /research-state.v1 -->


## Ángulo declarado (§b2)

Reconstruir el **patrón de UI** (no un subsistema del framework): qué widgets/bindings de `bajaui`/`kitPx`
permiten emular un botón que despliega un menú vertical de opciones en un gráfico PX, con qué mecánica real
(decompilado) y qué sintaxis `.px` concreta lo materializa. Perfil objetivo: **Workbench** (no Hx/bajaux).

## Cobertura

**12 / 12 gaps originales** cerrados (100%). **REABIERTO 2026-07-26** con una fase DINÁMICA (§12) contra
station viva: **21 / 33** cerrados. Bloques nuevos: **B289-B293**.

> **Sesión aplicada 2026-07-28 — B293 (DOCUMENT-MODE §20), pedido del cliente.** El operador del sitio pidió
> que la pestaña padre quede **sombreada** tras pinchar una entrada del dropdown, y el integrador pidió que
> el menú quedara **heredado** por las páginas de contenido. Resultado: DOS rutas documentadas y desplegadas.
>
> - **Ruta A — shell HTML** (`shell.html` + `Shell.px`): navbar + `<iframe name="content">`. Cambiar
>   `target="_top"` por el nombre del frame hace que el navbar NO se recargue nunca ⇒ herencia gratis sobre
>   **todas** las vistas (incluidas las nativas), estado activo con una clase CSS, y el dropdown deja de
>   recortarse (B291 lo tenía en una franja de 56 px).
> - **Ruta B — `PxInclude`**: es el patrón **OFICIAL de Tridium** —
>   `docGraphics_CreatingANavigationMenu-1D868C52.txt`, "Create a global navigation menu using PxInclude".
>   Solo alcanza a los `.px` propios; las vistas nativas no embeben nada.
>
> **Nota de método**: el bloque estuvo a un paso de afirmar que no existía patrón oficial. Lo salvó consultar
> niagara-help (fuente 2). Calibración nueva: para PX, la doc responde al **nombre de clase/widget**, no a la
> intención descrita — `"PxInclude"` → 18 archivos; `"graphic template"`, `"Px template"`,
> `"navigation bar graphic reuse"`, `"web browser widget px"` → cero.
>
> **Plantillas `.px`**: no están en la doc, están en los módulos (~250). La base del editor es
> `easyTemplating-wb/res/PxFile.px`, y **trae un `ScrollPane` en la raíz** — que es exactamente lo que la
> guía manda borrar para un menú. El gotcha oficial es un default de plantilla, no un caso raro.

> **Fase dinámica 2026-07-26** — el focus se cerró en 2026-07-06 con evidencia estática (decompilado + doc).
> Una sesión aplicada contra la station VIVA `PRUEBAS` (OptimizerSupervisor N4.14.0.162, Honeywell,
> `https://localhost`) produjo evidencia `[CERT-live]` que **corrigió DOS bloques cerrados** — exactamente
> el escenario que METHODOLOGY §14 anticipa. Lo estático no estaba mal por descuido: estaba incompleto
> porque nadie había desplegado el patrón.
>
> **Correcciones aplicadas** (bloque viejo editado + bloque nuevo que lo explica):
> - **B187** §187.3/§187.4/§187.5 decía que el `^` de `file:` era "relativo al dir actual". Es un ANCLA
>   ABSOLUTA al station home (`BFileSystem.java:144-151`) y en la práctica mapea a `<station>/shared/`.
>   → corregido en B187, explicado en **B289**.
> - **B189** §189.4 declaraba el toggle de un botón "sin resolver en PX puro" y §189.5 recomendaba
>   `PopupBinding`. Ambas cosas revertidas: el toggle sale con `ToggleButton` + `SetPointBinding`, y
>   `PopupBinding` NO produce un dropdown (abre una ventana con caption literal `"Pop up"`).
>   → corregido en B189, explicado en **B292**.

> Backlog EXPANDIDO 2026-07-06 (pedido del usuario: documentación exhaustiva de "cómo funciona, sus reglas,
> su sintaxis"). Tres capas de evidencia: gramática autoritativa (`PxDecoder/PxEncoder` decompilados,
> `[CERT]`), workflow oficial (`niagara-help` docs, `[CERT-doc]`), y validación empírica (cientos de `.px`
> reales). El focus deja de ser solo "el menú" y pasa a documentar el **PX Editor y su formato**.

## Gap backlog (priorizado)

| Gap | Descripción | Estado | Bloque | Fuente confirmada |
|---|---|---|---|---|
| G1 | Sin widget dropdown nativo; `BMenu*`=Swing WB; mapa 2 patrones. | **cerrado** | B179 | bloque35:80, bloque36, kitPx bindings |
| G5 | **PX Editor (herramienta) — workflow oficial**: crear Px View, paleta, bind widgets, add binding, property sheet. | **cerrado** | B180 | `sources/text-extracts/docGraphics-px-editor.md` (11 rangos preservados de docGraphics.txt) |
| G6 | **Gramática autoritativa del formato PX**: `PxDecoder`/`PxEncoder` — serialización, mapeo tipo→clase vía `<import>`, atributo=prop-simple vs sub-elemento=binding, tag en 1 línea. | **cerrado** | B181 | `sources/decompiled/bajaui-wb-px/{PxDecoder,PxEncoder}.java` (preservados) |
| G7 | **Sistema de layout**: `CanvasPane` (absoluto `layout="x,y,w,h"`) vs `GridPane`/`FlowPane` (add-order) vs `EdgePane` (5 slots nombrados). `layout`=prop de `BWidget`. (§14: `BBorderPane`≠5-regiones, es CSS-box.) | **cerrado** | B182 | `sources/decompiled/bajaui-wb-layout/` (6 clases preservadas) |
| G8 | **Serialización de valores**: `BColor` (#rrggbb/#aarrggbb/nombres/rgb()), `BFont` (`[bold][italic]<size>pt <name>`), `BBrush` (solid vs linear/radialGradient + `stop(offset% color)`), `BPoint`/`BSize` (`x,y`), `BInsets` (CSS-shorthand). Lado encode autoritativo. | **cerrado** | B183 | `sources/decompiled/gx-rt/` (6 clases preservadas) |
| G12 | `Parser` parse-side: `parseColor` (hex 3/6/8, nombres, rgb/rgba), `parseFont` (bold/italic/pt), `parseBrush` (sólido/gradiente stop()), `parseInsets`. Simetría round-trip con encode-side confirmada. | **cerrado** | B190 | `sources/decompiled/gx-parser/Parser.java` (preservado) |
| G9 | **Catálogo de converters** (104 clases; familia `BI*ToSimple` widget-facing). `BIBooleanToSimple` (trueValue/falseValue + type-guard) SÍ produce boolean usable para `visible`, sin coerción. Gotcha init() reseed. | **cerrado** | B184 | `sources/decompiled/converters-rt/` (5 clases preservadas) |
| G2 | `kitPx:PopupBinding` — extends BBinding, @AgentOn bajaui:Widget; trigger MOUSE_RELEASED button1 (clic, no hover); abre BNiagaraWbDialog (Workbench) con position/size absolutos; props title/position/size/modal + ord heredada. | **cerrado** | B185 | `sources/decompiled/kitPx-wb/BPopupBinding.java` (preservado) |
| G3 | `BValueBinding` (patrón in-place): `getOnWidget` = slot converter dinámico (motor `visible`); `hyperlink` (nav izq) + `popupEnabled` (menú acciones der vía NavMenuUtil); `degradeBehavior` heredado de BBinding. | **cerrado** | B186 | `sources/decompiled/bajaui-wb-px/BValueBinding.java` + `BBinding.java` (preservados) |
| G10 | Ord schemes: cadena `scheme:body\|...`; 15 schemes (remisión bloque35); relevantes al menú (slot/station/file/history/view/module); relativo(`^`/`..`) vs absoluto + `BOrd.make(base,rel).normalize()`; portabilidad. | **cerrado** | B187 | bloque35:3 + `sources/decompiled/baja-naming/BOrd.java` + `.px` reales |
| G11 | `BPxInclude` (extends BWidget): embebe una `.px` por su `ord` (carga async, `root`, `baseOrd`, `variables`, `reload` on-change). Al ser BWidget su `visible` es togglable → menú reutilizable in-place. | **cerrado** | B188 | `sources/decompiled/bajaui-wb-px/BPxInclude.java` (preservado) |
| B293 | **Menú HEREDADO + tab activo** (pedido del cliente): ruta A shell HTML (navbar + iframe) vs ruta B `PxInclude` (patrón oficial Tridium); dónde viven las plantillas `.px` (~250 en módulos, base = `easyTemplating/res/PxFile.px` con `ScrollPane` raíz); dialecto real del editor; `BBorder` orden libre; `BRadioButtonGroupBinding` OEM. | **cerrado** | B293 | guía `docGraphics_CreatingANavigationMenu-1D868C52.txt` + `BBorder.java` + `BHalign.java` + `BRadioButtonGroupBinding.java` + station viva |
| G4 | **Síntesis aplicada culminante** (DESIGN/APPLIED): `menu.px` completo patrón A (PopupBinding) + patrón B (in-place toggle visible), integrando B179-B188 con cada decisión respaldada. Reglas tag-1-línea, GridPane columnCount=1, fricción del toggle. | **cerrado** | B189 | síntesis [Block 179-188] |

## Clasificación del backlog (§8)

- STOP 2026-07-06: 12 bloques (B179-B190), 12/12, read-only-investigable = 0.
- **REABIERTO 2026-07-26** (fase dinámica §12, station viva). Estado actual:
  **read-only-investigable: 6** (B289-G2, B291-G3, B292-G2, B293-G3, B293-G4, B293-G5) ·
  **requires-execution: 6** (B290-G1, B290-G2, B291-G1, B291-G2, B292-G1, B293-G1, B293-G2 — necesitan la
  station) · **blocked: 0**. 18/31 cerrados, bloques B289-B293.
- **B293-G1 y B293-G6 CERRADOS 2026-07-28 con render vivo en DOS perfiles**:
  · **Workbench** — el navbar RENDERIZA (cierra media B291-G2), pero todos los links dieron 404/403. Causa
    en código: `Href2Ord.hrefToOrd` mete el href como ord relativo al del widget. **CORRIGE B291 §291.6**:
    dentro de `BWebBrowser` va el ORD CRUDO, no `/ord/<encoded>`. Ver B293 §293.10.
  · **Navegador** — PASA END-TO-END (B293 §293.11). El clic navega el IFRAME, la pestaña PADRE queda
    sombreada, el `Dashboard/Home.px` de producción entra sin tocarlo, y **el menú sobrevive un
    `AlarmDbView` NATIVO** — la prueba observada de que ruta A ≠ ruta B.
- **B293-G2 CERRADO 2026-07-28**: `fullScreen=true` como VIEW PARAMETER (`BHxProfile.fullScreenKey`, default
  `"false"` ⇒ el chrome es opt-out) elimina tab strip + selector de vista + icon rail. Verificado con
  `HistoryChartBuilder`, que además ejercitó la rama "el ord ya trae `|view:`". **RUTA A COMPLETA para el
  perfil navegador**: menú heredado, dropdown anclado, pestaña padre sombreada, vistas nativas incluidas,
  sin chrome de Niagara.
- **Orden de ataque sugerido**: **B293-G7** (el mismo test de clic bajo Workbench, donde
  `BWebBrowserView:540` intercepta a nivel widget) → **B293-G4** (~250 `.px` OEM sin abrir: la mejor guía de
  estilo disponible para este sitio) → B292-G1 (tab activo en PX puro) → **B293-G4** (~250 `.px` OEM sin abrir:
  la mejor guía de estilo disponible para este sitio) → B292-G1 (`ButtonGroupBinding` + hyperlink → tab
  activo en PX puro, ahora mejor acotado: el blocker es la PERSISTENCIA, no la exclusividad — B293 §293.5)
  → B291-G2 (perfiles) → B289-G2 (mecanismo de `^`).
- **requires-execution**: 0. **blocked**: 0.
- **Orden de ataque**: G5 (editor oficial) → G6 (gramática/reglas) → G7 (layout) → G8 (valores) →
  G9 (converters) → G2 (PopupBinding) → G3 (in-place) → G10 (ords) → G11 (PxInclude) → G4 (síntesis `menu.px`).
- **Fuera de ámbito** (gaps futuros si se reabre): perfil **Hx** (`BHxPxPopupBinding`) y **bajaux** moderno.

## Fuentes cross-target (niagara-help = target #3)

G5 cita documentación oficial que vive en el install `niagara-help`, no en el repo del corpus. Al escribir
el bloque G5, **preservar** los extractos relevantes en `sources/` de este target y registrarlos en
`SOURCES.md` (§5), citando el `.pdf/.txt` preservado — no la ruta volátil del install.

## Historial de iteraciones

| Iter | Gap | Bloque | Delegado? · tier | Resultado |
|---|---|---|---|---|
| 1 | G1 | B179 | no · inline | Framing: sin widget nativo; `BMenu*` = Swing WB (bloque35:80); mapa 2 patrones. |
| 2 | G5 | B180 | yes · sonnet | Workflow oficial PX Editor (docGraphics.txt): New Px View wizard, paleta/árbol, bind por drag-ord, Add Binding=slot hijo, tipos de binding oficiales, hyperlink/HyperlinkLabel, reglas ords relativos + degradeBehavior. 11 rangos preservados en sources/. |
| 3 | G6 | B181 | yes · sonnet | Gramática PxDecoder/PxEncoder: raíz `px`/`version="1.0"`, secciones fijas, tipo resuelto vía `<import>` (no namespace), atributo=prop-frozen-simple no-default vs sub-elemento=binding/slot-hijo, tag en 1 línea (raíz del gotcha XParser), errores=XException. 2 fuentes preservadas. |
| 4 | G7 | B182 | yes · sonnet | Layout: `layout`=prop de `BWidget` (por eso atributo del hijo). CanvasPane=absoluto+viewSize/scale; GridPane=row-major add-order (columnCount def 2); FlowPane=horizontal+wrap; EdgePane=5 slots nombrados. §14: BBorderPane≠5-regiones (es CSS-box). Menú vertical → GridPane columnCount=1. 6 clases preservadas. |
| 5 | G8 | B183 | yes · sonnet | Serialización gx (lado encode): BColor #rrggbb/#aarrggbb+nombres+rgb(); BFont `[bold][italic][underline]<size>pt <name>`; BBrush BNF gradientes + `stop(offset% color)`; BPoint/BSize `x,y`; BInsets CSS-shorthand. Parser (parse-side) ubicado en vineflower → gap G12. 6 clases gx preservadas. |
| 6 | G9 | B184 | yes · sonnet | Converters (104 clases): familia BI*ToSimple=widget-facing. BConverter.convert(from,to,ctx) contrato. BIBooleanToSimple: trueValue/falseValue (default TRUE/FALSE) + type-guard getType()==to.getType() → SÍ produce boolean para `visible` sin coerción. Gotcha init() reseed ambos slots al valor actual. Hermanos BINumericToSimple (lookup-map), BIStatusToSimple (status bits). 5 clases preservadas. |
| 7 | G2 | B185 | no · inline | PopupBinding: extends BBinding @AgentOn Widget; started() linkTo mouseEvent; released() con isButton1Down()→popup() (clic, no hover; corrige bloque36); popup() abre BNiagaraWbDialog vía BWbShell (Workbench) con position/size absolutos → NO anclado. Props title/position(100,100)/size(800x600)/modal. 1 fuente preservada. |
| 8 | G3 | B186 | no · inline | BValueBinding: @AgentOn Widget+Value. getOnWidget(prop)=busca slot con name==prop, si BConverter convierte from(out punto)→propiedad (motor visible). Props hyperlink(nav izq)/summary/popupEnabled(menú acciones der vía NavMenuUtil reflection). degradeBehavior de BBinding. Integra con B184. 2 fuentes preservadas. |
| 9 | G10 | B187 | no · inline | Ord schemes: cadena scheme:body\|...; 15 schemes por remisión bloque35:3; relevantes al menú slot/station/file/history/view/module; relativo (^/..) vs absoluto + BOrd.make(base,rel).normalize(); regla portabilidad (ords relativos). BOrd.java preservada. |
| 10 | G11 | B188 | no · inline | BPxInclude extends BWidget: embebe .px por ord (async load, root, baseOrd, variables, reload on-change). Al ser BWidget su visible es togglable → menú reutilizable in-place (DRY). Remisión bloque22. 1 fuente preservada. |
| 11 | G4 | B189 | no · inline | SÍNTESIS DESIGN/APPLIED (ratio 0.69 sano): menu.px completo patrón A (GridPane columnCount=1 + PopupBinding file:^) y B (visible←ValueBinding+IBooleanToSimple←menuOpen). Reglas tag-1-línea, valores gx, type-guard, ords relativos, degradeBehavior. Fricción toggle (BooleanWritable sin action toggle nativa) + gotcha init(). Integra B179-B188. |
| 12 | G12 | B190 | no · inline | Parser parse-side: parseColor (hex 3/6/8+nombres+rgb/rgba), parseFont (bold/italic/pt), parseBrush (sólido/gradiente stop()), parseInsets. Round-trip encode↔parse cerrado. Parser.java preservada. |

## Notas

- Toda la evidencia primaria fue leída READ-ONLY en la sesión de origen (2026-07-06); las fuentes están
  confirmadas alcanzables antes de abrir cada iteración (SOURCE-BEFORE-AGENT).
- Perfil **Hx** (`BHxPxPopupBinding`) y **bajaux** quedan fuera de ámbito de este focus (el usuario fijó
  Workbench); se anotan como gaps futuros si se reabre.

---

## Fase dinámica — 2026-07-26 (station viva `PRUEBAS`)

Evidencia `[CERT-live]` contra OptimizerSupervisor N4.14.0.162 (Honeywell) en `https://localhost`.
Probe preservada: `sources/probes/live-20260727T012800Z-station-pruebas-filespace-and-obix.txt`.
Aplica SECRETS DISCIPLINE (live-install): se cita estructura, nunca valores de credenciales.

| # | Gap | Bloque | Resultado |
|---|---|---|---|
| 13 | D1 — ¿Dónde viven realmente los `.px` de una station? Los dos espacios (component vs file). | **B289** | `^` es ANCLA ABSOLUTA (`BFileSystem.java:144-151`), y mapea a `<station>/shared/`, no a `<station>/`. **Corrige B187.** Un `.px` NO puede vivir en un `baja:Folder` del component space. Discriminar la station VIVA por `config.bog.lock` fresco. |
| 14 | D2 — Acceso HTTP programático a una station viva. | **B290** | Un usuario nuevo toma `DigestScheme` → el server IGNORA el header Basic y devuelve 302 a `/login`. Con `HTTPBasicScheme`: `/obix` y `/obix/config/` = 200, `/` sigue 302 (superficies de auth distintas). `config.bog` es un ZIP con `file.xml` → estructura offline sin Workbench, pero refleja el estado EN DISCO, no el runtime. |
| 15 | D3 — ¿Qué JavaScript se puede usar con solo `.px`, sin compilar módulos? | **B291** | `BWebWidget` filtra su ord a `ViewQuery` → exige vista de módulo. `BWebBrowser extends BWidget` con ord LIBRE → es la vía. La station sirve el file space: `/file/px/...` crudo vs `/ord/file:%5E...` envuelto en perfil Hx (6 marcadores de chrome vs 0). Trampa del iframe `about:blank`: las rutas absolutas no resuelven; el CSP permite `data:`. Dialecto ES5. |
| 16 | D4 — El toggle de un botón, dado por irresoluble en B189 §189.4. | **B292** | RESUELTO en PX puro: `BToggleButton.selected` (`:33-51`) + `BSetPointBinding` (`:49-54`, "attempts to use a set action to save") + `IBooleanToSimple` sobre `visible`. **Corrige B189 §189.4 y §189.5.** `PopupBinding` NO es un dropdown: abre ventana con caption literal `"Pop up"`. |
| 17 | B289-G1 — el ord de pathbar que tiraba `UnknownSchemeException`. | **B289** | CERRADO: el culpable era el segmento `station:`. El ord correcto es `local:\|foxs:\|file:^px/...` — `file:` encadena tras `foxs:`, nunca tras `station:`. Coherente con los dos espacios disjuntos (§289.1). |

### Gaps hijo abiertos (8)

| ID | Gap | Clase |
|---|---|---|
| B289-G2 | ¿`Sys.getStationHome()` devuelve `<station>/shared/`, o el file space aplica una restricción encima? §289.3 fija el mapeo OBSERVADO, no su mecanismo. | STATIC |
| B290-G1 | **CERRADO [CERT-live] (B605, §12)**: SÍ — cliente no-browser con handshake SCRAM alcanza obix (200); Basic rechazado (401). Premisa corregida: API2.authenticationSchemeName=DigestScheme (=SCRAM), NO hay cuenta Basic que 'evitar' — SCRAM es la puerta. | CLOSED |
| B290-G2 | **CERRADO [CERT-live] (B607, §12)**: superficie de escritura obix ejercida — set(NumericWritable5)→oráculo independiente→restore byte-idéntico; cliente SCRAM no-browser escribe, no solo lee; write sin CSRF. | CLOSED |
| B291-G1 | ¿`BWebBrowser` acepta un ord HTTP absoluto (`ip:host\|http:/file/...`) para saltear el envoltorio desde el widget? Variante escrita, no confirmada. | DYNAMIC |
| B291-G2 | Comportamiento de un `.px` con `WebBrowser` entre perfiles: verificado bajo `view:hx:HxPxView`, NO bajo Px View de Workbench ni perfil mobile. La clase vive en `workbench-wb`. | DYNAMIC |
| B291-G3 | ¿Una página servida por `/file/` puede usar la API bajaux (suscripciones, BajaScript) en vez de fetch/DOM plano? ¿Es alcanzable el contexto RequireJS de B204? | STATIC |
| B292-G1 | `BButtonGroupBinding` end-to-end sobre un `EnumWritable`: ¿los radio buttons generados admiten hyperlink? Es la pieza que falta para un navbar con "ítem activo" persistente. | DYNAMIC |
| B292-G2 | ¿`ActiveStateSimple` sobre `stroke` vs `background` lo decide el atributo `background` declarado, o el tipo de widget? Dos archivos del sitio correlacionan; sin lectura de código. | STATIC |

### Deliverables desplegados en la station

`<station>/shared/px/` — `dropdown.px` (Patrón B, **confirmado funcionando** por el operador),
`navbar.px` (barra horizontal con hover), `webmenu.px` + `webmenu/menu.html` (menú HTML/CSS/JS embebido
vía `WebBrowser`), `webmenu-v1.px` / `webmenu-v2.px` (dos variantes de branding para elección del cliente).
Requiere un `BooleanWritable` en la station (`Drivers/PRUEBAS/MenuOpen`) — **los slots son
case-sensitive**.
