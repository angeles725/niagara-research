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
| px-editor-deep | **stopped** | `RESEARCH-STATE-px-editor-deep.md` | Profundizar pxEditor-wb (D: sidebars/studio/make/commands/field-editors) + módulos vecinos (X: webChart, templates, kitPxGraphics/Hvac/N4svg, svgBatik, bajaux). **CERRADO 11/11** (B198-B208 + síntesis B209). Grupo D (D1-D5) + Grupo X (X1-X6). Todo el subsistema PX deep documentado | B198–B209 |
| px-editor-core | **stopped** | `RESEARCH-STATE-px-editor-core.md` | La INFRAESTRUCTURA de pxEditor-wb nombrada-no-abierta — C1 event bus ✅B210, C2 API base ✅B211, C3 factory/WidgetInserter ✅B212, C4 util/property ✅B213, C5 fieldeditors ✅B214 + síntesis B215. **CERRADO 5/5**. 5 hilos: BPxEditor hub, selección=nexo (+§14), @AgentOn=extensión, undo=Command, delgado sobre bajaux | B210–B215 |
| nmodsreflow-builder | **stopped** | `RESEARCH-STATE-nmodsreflow-builder.md` | Reflow como CONSTRUCTOR de dashboards (ángulo PRODUCTO). **CERRADO 12/12** (B216-B227): stack/libs (B216, §14 d3 presente), modelo dashboard+persistencia **[CERT-live]** (B217), catálogo 20 widgets (B218), assets embebidos+ORD→URL (B219), upload=out-of-band (B220), motor JSON-Patch+control multiusuario (B221), Mapbox "3D"=2D (B222), editor+masonry (B223), render gauge/chart d3/iView (B224), síntesis Parte A (B225), chihuahua-builder+portabilidad (B226), modernización stack (B227) | B216–B227 |

## Focus activo

**(ninguno activo)** — `nmodsreflow-builder` CERRADO 12/12 (2026-07-12, B216-B227). §18 retro pendiente de correr.

**nmodsreflow-builder** (ángulo PRODUCTO/BUILDER) — CERRADO 2026-07-12, 12 bloques B216-B227. Cómo Reflow crea/edita/
actualiza dashboards y agrega contenido dentro del módulo. Hallazgos: servidor delgado (persiste/parchea JSON opaco),
composición 100% cliente Vue; "editá-y-se-actualiza" = JSON-Patch RFC-6902 (fast-json-patch cliente / flipkart server)
+ control multiusuario cooperativo; catálogo 20 widgets (add=dropdown, no paleta drag); layout=masonry; editor=iframe
live-preview; assets=delegados a servlets nativos Niagara (`/module/`, `/ord/`), upload=out-of-band (Workbench);
"3D"=Mapbox 2D. **§14**: d3 SÍ presente (aliaseado, corrige B216); circle=iView (corrige B218). Validación **[CERT-live]**
contra station N4 viva (B217 §217.8) + dashboard real de disco (B218). Parte B (B226): chihuahua NO tiene builder
(dashboard fijo) pero tiene **3D real Three.js** que Reflow no, y lidera en RBAC/audit; plan de portabilidad 5 piezas.
Modernización (B227): Vue2 EOL→Vue3/Pinia/Vite/TS, mapbox→MapLibre, mantener JSON-Patch+d3.

**(px-editor-core cerrado)** — `px-editor-core` CERRADO 5/5 (2026-07-06, B210-B214 evidencia + B215 síntesis).
Infra interna de pxEditor-wb: C1 event bus (B210), C2 API base root (B211), C3 factory/WidgetInserter (B212),
C4 util/property (B213), C5 fieldeditors converters (B214). Síntesis B215: 5 hilos (BPxEditor hub-and-spoke,
selección=nexo +§14 corrige B211, @AgentOn=mecanismo de extensión uniforme, undo=Command en la infra, capa
delgada sobre bajaux). §14: B213→B211 (SelectedWidgets dispara PxWidgetEvent, no PxSelectionEvent). **Todo el
subsistema PX de Niagara N4 documentado end-to-end**: 4 focuses (px-menu B179-190, px-editor B191-196,
px-editor-deep B198-209, px-editor-core B210-215) + síntesis B197/B209/B215.

**px-editor-deep** (capa herramienta/render deep) — CERRADO 2026-07-06, 11 bloques B198-B208 + síntesis B209.
Grupo D interno (sidebars B198, studio B205, make B201, commands B206, field-editors B202) + Grupo X vecinos
(webChart B199, templates B200, packs B203, svgBatik B208, bajaux B204, easyBinding B207 OEM Honeywell). 4 hilos
transversales (B209): bajaux base web unificadora, 2 sistemas chart, undo=Command, alto nivel sobre kitPx.

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
