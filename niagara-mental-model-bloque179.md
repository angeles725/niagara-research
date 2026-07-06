# Bloque 179 — PX Menu-Button / Dropdown (Workbench): no hay widget nativo, dos patrones viables

> Research del focus **`px-menu`**: construir un "Menu Button / Dropdown Trigger" (estilo Salesforce SLDS)
> en el **PX Editor de Niagara N4, perfil Workbench**. Este bloque es el FRAMING del focus: establece que
> NO existe un widget dropdown nativo y mapea los dos patrones de emulación viables (desarrollados en
> B180/B181) más la sintaxis `.px` (B182) y el workflow oficial del editor (G5, próximo).
>
> Sources: `modules/Prototipos/modulos/organized/kitPx/kitPx-wb/vineflower/com/tridium/kitpx/*Binding.java`
> (bindings decompilados de `kitPx`) · `niagara-mental-model-bloque35.md:80` (jerarquía Swing `bajaui`) ·
> `niagara-mental-model-bloque36.md` §36.2.2 (catálogo `kitPx`) · `niagara-mental-model-bloque22.md`
> (formato PX, `PxDecoder`, ausencia de scripting).
> Method: lectura READ-ONLY del decompilado + remisión a bloques del corpus. Markers (METHODOLOGY §3):
> `[CERT]` fuente primaria local (`file:line`) · `[INFER]` deducción.
>
> Capa UI/PX (Workbench). Connects [Block 36] (catálogo kitPx), [Block 35] (shell Swing + menús),
> [Block 22] (formato PX).

---

## 179.1 — No existe un widget "Menu Button / Dropdown" en el catálogo PX `[CERT]`

El catálogo de widgets `kitPx` (controles estándar arrastrables al canvas PX) no incluye ningún widget de
tipo dropdown, combo-box ni menú desplegable. Los controles interactivos disponibles son botones y editores
inline — ninguno despliega una lista vertical de opciones:

| Widget kitPx | Tipo | ¿Dropdown? |
|---|---|---|
| `BLocalizableButton`, `BImageButton`, `BRefreshButton`, `BSaveButton`, `BExportButton`, `BWbCommandButton` | Botones (acción simple) | No |
| `BTouchSlider`, `BAnalogMeter`, `BBargraph` | Indicadores/entrada analógica | No |
| `BSetPointFieldEditor`, `BGenericFieldEditor` | Editores inline | No |

Fuente: catálogo en `niagara-mental-model-bloque36.md` §36.2.2 (líneas 190-206). Ningún ítem es un menú.

Los **bindings** de `kitPx` (reutilizables sobre cualquier widget) tampoco incluyen uno que abra un menú de
opciones. La lista COMPLETA de clases `*Binding` decompiladas en
`organized/kitPx/kitPx-wb/vineflower/com/tridium/kitpx/` es:

`BActionBinding`, `BBoundLabelBinding`, `BButtonGroupBinding`, `BIncrementSetPointBinding`,
`BMomentaryToggleBinding`, `BMouseOverBinding`, `BPopupBinding`, `BSetPointBinding`, `BSpectrumBinding`,
`BSpectrumSetpointBinding` (+ sus variantes `BHxPx*` para el perfil Hx). `[CERT]`

Ninguno es un "dropdown/menu binding". El más cercano a un menú es `BPopupBinding` (abre OTRA vista PX en
una ventana — B180), no un menú anclado. `[INFER]`

## 179.2 — `BMenu`/`BMenuBar`/`BMenuItem` existen, pero son menús Swing del Workbench, NO widgets de canvas PX `[CERT]`

Confusión clásica: `bajaui` SÍ define `BMenu`, `BMenuBar`, `BMenuItem`, `BSubMenuItem`, `BActionMenuItem`
(`niagara-mental-model-bloque35.md:80`). Pero viven en la **jerarquía de widgets Swing del shell Workbench**
(`BWbShell`/`BNiagaraWbShell`), no en el sistema de widgets serializables del canvas PX. Son la barra de
menú de la aplicación y los menús contextuales de click-derecho, poblados por `BNavMenuAgent` /
`BComponentMenuAgent` (bloque 35 §), no elementos que se arrastren a un `CanvasPane` de un gráfico. `[CERT]`

Implicación: buscar un "Menu Button" tipo SLDS entre los widgets PX es infructuoso — hay que **emularlo**
con los bindings existentes. `[INFER]`

## 179.3 — Los dos patrones viables (mapa del focus) `[INFER]`

Sección de DISEÑO (no evidencia): consolida los caminos que el resto del focus desarrolla con citas.

| Patrón | Mecanismo | Anclado bajo el botón | Requiere punto en station | Esfuerzo | Bloque |
|---|---|---|---|---|---|
| **PopupBinding** | Clic → abre `menu.px` en una ventana `BNiagaraWbDialog` | No (ventana con `position`/`size` absolutos) | No | Bajo | B180 |
| **Toggle in-place** | `BValueBinding`+converter dinámico ata `visible` de un panel al `out` de un `BooleanWritable` | Sí | Sí | Alto | B181 |

Restricción transversal que fuerza el patrón in-place a depender de la station: **PX no tiene scripting ni
variables de UI locales** (`BPxScript` no existe; `niagara-mental-model-bloque22.md`), así que el estado
"menú abierto/cerrado" debe vivir en un punto del BOG. `[CERT]` (remisión a Block 22)

Recomendación de arranque: **PopupBinding** para Workbench (bajo esfuerzo, sin tocar la station). El in-place
solo si el anclaje visual es requisito duro. `[INFER]`

## 179.x — Connections

- **[Block 36]** — catálogo `kitPx` (widgets + bindings): la fuente de que no hay dropdown nativo (§179.1).
- **[Block 35]** — shell Workbench + jerarquía Swing `bajaui`: ubica `BMenu*` como menús del shell, no PX (§179.2).
- **[Block 22]** — formato PX + `PxDecoder` + ausencia de `BPxScript`: la restricción de "sin estado local" (§179.3).
- **B180** (próximo) — mecánica real de `kitPx:PopupBinding`.
- **B181** — patrón in-place (`BValueBinding` + converter → `visible`).
- **B182** — sintaxis `.px` verificada del `menu.px` + gotcha XParser.
- **G5** — workflow oficial del PX Editor (doc Tridium `niagara-help`).
