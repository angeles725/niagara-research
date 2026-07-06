# RESEARCH-STATE — Focus `px-menu` (PX Menu-Button / Dropdown en Workbench)

> Focus multi-eje (METHODOLOGY §16) del target `niagara-research`. Ámbito: cómo construir un
> **"Menu Button / Dropdown Trigger"** (estilo SLDS) en el **PX Editor de Niagara N4, perfil Workbench**.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`), mismo repo/hook. Corpus en Español
> (técnico EN), por continuidad con los 178 bloques previos.
> Engram topic key: `research/niagara/px-menu/{gaps,progress}`.

## Ángulo declarado (§b2)

Reconstruir el **patrón de UI** (no un subsistema del framework): qué widgets/bindings de `bajaui`/`kitPx`
permiten emular un botón que despliega un menú vertical de opciones en un gráfico PX, con qué mecánica real
(decompilado) y qué sintaxis `.px` concreta lo materializa. Perfil objetivo: **Workbench** (no Hx/bajaux).

## Cobertura

**1 / 5 gaps** cerrados (20%).

## Gap backlog (priorizado)

| Gap | Descripción | Estado | Bloque | Fuente confirmada |
|---|---|---|---|---|
| G1 | No existe widget dropdown nativo en PX; `bajaui` `BMenu/BMenuBar/BMenuItem` son menús Swing del Workbench, no widgets de canvas. Mapa de los 2 patrones viables. | **cerrado** | B179 | bloque35:80, bloque36 catálogo, kitPx bindings decompilados |
| G5 | **PX Editor (la herramienta) — workflow oficial**: crear una Px View, la paleta, agregar/enlazar widgets, agregar un binding, el property sheet. Doc oficial Tridium (`[CERT-doc]`). | investigable | — | `niagara-help/devguide-clean/px.txt`, `docs-text/docGraphics.txt`, `guides-clean/easyTemplating/Binding_the_Datapoints_using_Hyperlink_Ord.txt` |
| G2 | `kitPx:PopupBinding` — mecánica real: trigger, ventana `BNiagaraWbDialog`, props. | investigable | — | `organized/docSource/.../kitPx-wb/com/tridium/kitpx/BPopupBinding.java` |
| G3 | Patrón in-place (toggle visibility): `BValueBinding` + converter dinámico → `visible`; estado en la station; fricción del toggle. | investigable | — | `bajaui-wb/javax/baja/ui/BValueBinding.java` + `BWidget.java` |
| G4 | Sintaxis PX verificada del `menu.px` (estructura, `Label`+`hyperlink`, converter `visible`) + gotcha XParser (tag en 1 línea). | investigable | — | `.px` reales (Venom Cvahu101, hx warmupInclude, PxFile.px default) |

## Clasificación del backlog (§8)

- **read-only-investigable**: 4 (G5, G2, G3, G4) — G5 vía docs oficiales `[CERT-doc]` en niagara-help
  (fuente confirmada 2026-07-06); G2/G3/G4 con fuente decompilada/`.px` confirmada y leída.
- **requires-execution**: 0.
- **blocked**: 0.
- **Orden de ataque**: G5 (editor oficial, prioridad del usuario) → G2 (PopupBinding) → G3 (in-place) →
  G4 (sintaxis `menu.px`).

## Fuentes cross-target (niagara-help = target #3)

G5 cita documentación oficial que vive en el install `niagara-help`, no en el repo del corpus. Al escribir
el bloque G5, **preservar** los extractos relevantes en `sources/` de este target y registrarlos en
`SOURCES.md` (§5), citando el `.pdf/.txt` preservado — no la ruta volátil del install.

## Historial de iteraciones

| Iter | Gap | Bloque | Delegado? · tier | Resultado |
|---|---|---|---|---|
| 1 | G1 | B179 | no · inline | Framing: sin widget nativo; `BMenu*` = Swing WB (bloque35:80); mapa 2 patrones. |

## Notas

- Toda la evidencia primaria fue leída READ-ONLY en la sesión de origen (2026-07-06); las fuentes están
  confirmadas alcanzables antes de abrir cada iteración (SOURCE-BEFORE-AGENT).
- Perfil **Hx** (`BHxPxPopupBinding`) y **bajaux** quedan fuera de ámbito de este focus (el usuario fijó
  Workbench); se anotan como gaps futuros si se reabre.
