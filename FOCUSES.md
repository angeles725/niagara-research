# Niagara Research — Focus Index

> Multi-focus corpus (METHODOLOGY §16). Un target maduro con varios ejes paralelos de investigación.
> Todos los focuses comparten la numeración global de bloques (`niagara-mental-model-bloqueN.md`) y el
> mismo repo git/hook; se distinguen por su `RESEARCH-STATE-<focus>.md` y su topic key en engram
> (`research/niagara/<focus>/gaps`, `.../progress`).

| Focus | Estado | RESEARCH-STATE | Ámbito | Bloques |
|---|---|---|---|---|
| (base) | stopped | `RESEARCH-STATE.md` | Framework Niagara N4.14 completo (Capas 1-25) + audit Reflow v1.7.5 + OEM Honeywell/Spyder + native platform RE | B1–B130 |
| optimizersupervisor | paused | `RESEARCH-STATE-optimizersupervisor.md` | Install vivo OptimizerSupervisor N4.14.0.162 (config.bog de stations vivas) | B123 |
| platform-native | stopped | `RESEARCH-STATE-platform-native.md` | RE nativo de la plataforma (launchers, JNI, licensing/crypto, driver DLLs, daemon) | B124–B130 |
| protocols | stopped | `RESEARCH-STATE-protocols.md` | Wire-level de protocolos (Modbus/OPC/BACnet/Fox/LON/Sox) + integración LOGO!8 | B131–B137 |
| nmodsreflow | stopped | `RESEARCH-STATE-nmodsreflow.md` | Arquitectura backend del módulo OEM NiagaraMods Reflow v1.7.7 `-rt` (service, HTTP/WS, subsistemas) — CERRADO, hilo de seguridad consolidado | B138–B150 |
| nmodsreflow-ux | stopped | `RESEARCH-STATE-nmodsreflow-ux.md` | Capa cliente/browser del módulo NiagaraMods Reflow v1.7.7 `-ux` (módulo fino de registro/loaders + SPA Vue embarcada) — CERRADO, paridad frontend con el backend | B151-B155 |
| live-station | stopped | `RESEARCH-STATE-live-station.md` | Validación DINÁMICA (§12) de la station Niagara N4 VIVA en 127.0.0.1 (WSL mirrored). `live-install` → SECRETS DISCIPLINE. Etapa A (runtime) + Etapa B (14 defectos de B150 con usuario `API`) — CERRADO, 13/14 con veredicto vivo | B156–B162 |
| chihuahua | stopped | `RESEARCH-STATE-chihuahua.md` | Módulo dashboard Niagara N4 de FUENTE PROPIA (`com.angeles.chihuahua`) para BMS Honeywell MX60. Lectura directa. Tri-parte rt/ux/wb, RBAC write-gate, frontend ES5 IIFE. Documentado (C1-C14) + comparado con Reflow (B177) — CERRADO | B163–B177 |
| px-menu | stopped | `RESEARCH-STATE-px-menu.md` | PX Menu-Button/Dropdown en Workbench: emulación de menú desplegable (PopupBinding vs toggle in-place), sintaxis `.px`, gramática PxDecoder/Encoder, catálogo de converters, workflow oficial del editor (niagara-help) — CERRADO 12/12 | B179–B190 |
| px-editor | stopped | `RESEARCH-STATE-px-editor.md` | El PX Editor en amplitud: la herramienta (`pxEditor-wb`), catálogo completo de widgets/bindings, media/perfiles (Wb/Hx/Mobile), theming (Palladium/CSS), animación=data-binding. Continúa px-menu — CERRADO 6/6 | B191–B196 |
| px-editor-deep | **active** | `RESEARCH-STATE-px-editor-deep.md` | Profundizar pxEditor-wb (D: sidebars/studio/make/commands/field-editors) + módulos vecinos (X: webChart, templates, kitPxGraphics/Hvac/N4svg, svgBatik, bajaux). **10/11** (…D2 B205; D4 B206; X6 B207 easyBinding). **Grupo D cerrado**. Próximo: X4 svgBatik (último) | B198– |

## Focus activo

**`px-editor-deep`** (ACTIVO, 3/11) — arrancado 2026-07-06. D1 `sidebars/` (B198) + X1 `webChart` (B199) + X2
`template` (B200) + D3 `make/` (B201) + D5 field-editors (B202) + X3 packs gráficos (B203) + X5 bajaux (B204) + D2 studio (B205) + D4 commands (B206) + X6 easyBinding (B207, OEM Honeywell). Próximo gap: **X4 (`svgBatik`)** — último. Continuar con
`/research-sdd niagara-research px-editor-deep continue`.

**Sesión 2026-07-06 — subsistema PX**: 19 bloques (B179-B197). `px-menu` CERRADO 12/12 (B179-B190, el menú +
formato/gramática). `px-editor` CERRADO 6/6 (B191-B196, el editor en amplitud). `B197` síntesis cross-focus
(7 capas). Coverage-audit honesto: espinazo completo, ~35-40% del universo de clases PX → pendientes en
`px-editor-deep`.

**px-editor** (capa UI/PX, amplitud) — CERRADO 2026-07-06, 6 bloques B191-B196. El PX Editor completo más
allá del menú: la herramienta `pxEditor-wb` (B191: BPxEditor/BStudio/BMakeWidget wizard, load/save/clone por
PxEncoder/Decoder) → catálogo de widgets bajaui (B192: botones/inputs/contenedores/datos-por-modelo) → los 9
bindings kitPx (B193: split BBinding/BValueBinding) → media/perfiles (B194: Wb permisivo, Hx agent-gated,
Mobile whitelist, bajaux sin PxMedia) → theming (B195: Palladium Java vs `.ux-*` CSS) → animación=data-binding
(B196). Junto con px-menu (B179-B190), el subsistema PX queda reconstruido end-to-end.

**(px-menu cerrado)** — `px-menu` CERRADO 2026-07-06 (12/12 gaps, B179-B190).

**px-menu** (capa UI/PX) — CERRADO 2026-07-06, 12 bloques B179-B190. Cómo construir un "Menu Button /
Dropdown" (estilo SLDS) en el PX Editor perfil Workbench. **Framing** (B179: sin widget nativo, `BMenu*`=Swing
WB, 2 patrones) → **workflow oficial del editor** (B180, docGraphics.txt `[CERT-doc]`) → **gramática/sintaxis**
(B181 PxDecoder/Encoder + tag-1-línea, B182 layout panes + §14 BBorderPane≠5-regiones, B183 valores gx) →
**motor del binding** (B184 converters + BIBooleanToSimple type-guard, B186 BValueBinding) → **los 2 patrones**
(B185 PopupBinding, B186 in-place) → **ords/includes** (B187, B188) → **síntesis** (B189 menu.px completo) →
**round-trip** (B190 Parser). Tres capas de evidencia: decompilado `[CERT]`, doc oficial `[CERT-doc]`, `.px`
reales. Deliverable: `scratchpad/menu.px`. Sin fase dinámica pendiente (todo read-only static).

**(base cerrado)** — `chihuahua` CERRADO 2026-07-02 (14/14 subsistemas + comparación con Reflow).

**chihuahua** (fuente propia) — CERRADO 2026-07-02, 15 bloques B163-B177. Módulo dashboard MX60 (Honeywell, dominio agua/bombeo,
6 plantas). Tri-parte `chihuahua-{rt,ux,wb}`, servlet `BChiServlet` en `/mx60/` con dispatch puro + guards
CSRF-lite, **RBAC write-gate (`checkCanWrite`) en cada endpoint mutante** (el contraste agudo con Reflow, que
no gatea). Bootstrap con B163 (esqueleto) + backlog de 14 gaps (barrido de auditoría §13). La comparación
chihuahua↔Reflow y el análisis de brechas son bloques de síntesis POSTERIORES (pedido del usuario).

**live-station** (dinámico §12) — CERRADO 2026-07-02, 7 bloques B156-B162. Primera validación `[CERT-hw]`
end-to-end de la station Niagara N4 VIVA. Etapa A mapeó el runtime (Reflow 1.7.7 en `/nmodsreflow/`, usuario
`API`=HTTPBasicScheme, cert default). Etapa B verificó los 14 defectos de B150: **config-write sin auth
CONFIRMADO** (V1-V3/V12, read-level sobrescribe config, restore byte-idéntico), backups **auth-gated** (V4/V10
NO reproducen — §14 corrige B144), reads 500 con payloads triviales, V7/V8 (BQL) diferido al canal WS
(requires-execution). Cero secretos exfiltrados; station intacta (`bf70f28f`). §14: refina tesis uniforme de
B150 §150.1 (gate NO uniforme: config abierto, backups gated).

**nmodsreflow-ux** (capa cliente `-ux`) — CERRADO 2026-07-02, 5 bloques B151-B155, superficie cliente
completamente mapeada (registro de vistas → loaders/iframe → SPA Vue 2.6.14 → wiring REST/WS → seguridad
cliente). §14: corrigió B50 (Vue 2.7→2.6.14). Confirmó B143/B144/B145 desde el cliente. NEXT-ACTION =
verificación dinámica sobre station viva (requiere hardware/decisión humana).

**nmodsreflow** (backend `-rt`) — CERRADO 2026-07-02, 13 bloques B138-B150, superficie completamente mapeada,
síntesis de seguridad cross-focus en B150. Residual R3 (mount `/module/<name>/`) no perseguido.
