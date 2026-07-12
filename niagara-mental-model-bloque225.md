# Block 225 — Reflow dashboard-builder (X): SÍNTESIS de producto — cómo Reflow construye un dashboard editable end-to-end

> **Qué documenta.** Consolida el focus `nmodsreflow-builder` (B216-B224) en un modelo mental único de PRODUCTO:
> cómo un usuario crea, edita, actualiza y enriquece un dashboard de Reflow, y qué arquitectura lo sostiene. Bloque
> de SÍNTESIS (terminal de la Parte A). Cierra BG10.
>
> **Alcance.** Síntesis cross-block de B216-B224. No introduce evidencia nueva (re-cita `file:line`/`BF:` ya
> verificados). La comparación con chihuahua es BG11 (B226); la modernización BG13 (B227).
>
> **Método / markers.** Bloque de síntesis/design → predomina `[INFER]` (tejido), anclado a `[CERT]` ya
> verificados en los bloques fuente. Ratio alto ESPERADO y sano (METHODOLOGY §11).

---

## 225.1 — El modelo mental en una frase `[INFER]`

**Reflow es un editor de dashboards SPA (Vue 2) que trata al dashboard como UN documento JSON opaco; el servidor
Niagara sólo lo persiste, lo parchea (JSON-Patch) y sirve assets, delegando el render de imágenes y la
autenticación a la plataforma.** Toda la inteligencia de composición vive en el cliente; el módulo Java `-rt` es
una capa delgada de persistencia + proxy.

## 225.2 — El flujo end-to-end (crear → editar → persistir → propagar → enriquecer) `[CERT]`/`[INFER]`

1. **Modelo** (B217): el dashboard = `dashboardCards.cards[]`, un pool plano de tarjetas
   `{id, type, enabled, config, width?, height?}`; las superficies (landing/pages/buildings) referencian IDs. Todo
   el estado Vuex persistente (menos ~38 keys efímeras) se serializa a `^reflow/config.json` — un blob opaco al Java.
2. **Crear/editar** (B218, B223): el editor corre como un shell externo con un **iframe de preview en vivo**
   (`isConfig`); "agregar widget" = `newCard()`→`ADD_CARD`→drawer con `<Select>` de 20 tipos; mover = drag en la
   lista del sidebar; redimensionar = `<Select>` width/height; layout = grilla **masonry**.
3. **Persistir** (B217, B221): cualquier mutación no-efímera auto-dispara `save` (debounce 3s). Single-user →
   full-write `config_update`; multi-user → `config_delta` (JSON-Patch RFC-6902), aplicado server-side con
   `flipkart-zjsonpatch` bajo `doPrivileged`, con control cooperativo de escritura (`configControl` token).
4. **Propagar** (B217, B221): full-write → broadcast `config-reload` → los demás ven modal "Reload Required";
   delta → broadcast `delta` → los demás aplican el patch a su Vuex reactivo (fast-json-patch) **sin reload** →
   edición colaborativa en caliente.
5. **Enriquecer** (B219, B220, B222, B224): el usuario agrega contenido con —
   - **arte de equipos embebido** (`image-library`, 25 JPG HVAC) + **iconos** (FontAwesome 1853) — por nav-RPC;
   - **fotos propias** subidas OUT-OF-BAND (Workbench) al file space, referenciadas `file:^Imagenes/…`;
   - **la vista geo** (`building-map`, Mapbox GL **2D**) con markers de edificios;
   - **charts** (`historyChart` = **D3.js**), **gauges** (`gage`=SVG, `circle`=iView), tablas, toggles, etc.
   Toda imagen se resuelve por `$ord.image()` → `/module/` o `/ord/` (servlets nativos de Niagara).

## 225.3 — El stack por capa (resumen) `[CERT]`

| Capa | Tecnología | Bloque |
|---|---|---|
| UI framework | Vue 2.6.14 + Vuex 3.5.1 + vue-router 3.4.5 (hash) | B216 |
| Layout | Masonry.js (directiva `v-masonry`) | B223 |
| Charts | **D3.js** (`<d3chart>`, aliaseado) | B224 |
| Gauges | SVG bespoke (`gage`) + iView `<Circle>` (`circle`) | B224 |
| Mapa | mapbox-gl (2D) + vue-mapbox wrappers | B222 |
| Iconos | FontAwesome (light/regular/solid) | B219 |
| Reorder | SortableJS/vuedraggable | B223 |
| Free-position | vue-drag-resize (solo floorplan) | B223 |
| HTTP cliente | wrapper propio (NO axios) | B216 |
| Sync/patch | fast-json-patch (cliente) ↔ flipkart-zjsonpatch (server) | B217/B221 |
| Persistencia Java | jackson + apache-commons-io (TeeOutputStream) | B216 |
| Assets/auth | delegado a servlets nativos Niagara (`/module/`, `/ord/`, nav-RPC, login SCRAM) | B219/B220 |

## 225.4 — Hilos transversales `[INFER]`

1. **Servidor delgado, cliente gordo**: el Java nunca modela el dashboard; recibe/guarda/parchea un JSON opaco y
   sirve assets. La composición es 100% cliente (B216 §216.1, B217 §217.3).
2. **Apoyarse en la plataforma Niagara**: imágenes por `/module/`+`/ord/`, navegación de assets por nav-RPC, login
   por SCRAM, sin reimplementar nada de eso (B219 §219.4, B220). "Upload" = cero código (Workbench lo hace).
3. **El dashboard como documento + patches**: editar = mutar Vuex → diff → JSON-Patch → aplicar+rebroadcast. Es
   event-sourcing ligero; habilita colaboración en caliente y un historial de backups (B221, B217).
4. **Editar-sobre-vista-viva**: el editor no es un canvas separado — es un iframe del dashboard real flagueado
   `isConfig` (B223 §223.1). Lo que ves es lo que hay.
5. **Todo es opcional/gateado**: weather-map por licencia, weather por `weather.enabled`, columnas configurables,
   estilos de mapa elegibles — el producto es muy parametrizable sin tocar código (B218 §218.2, B222 §222.3).
6. **Lección metodológica (§14)**: el grep negativo de nombre-de-librería sobre bundle aliaseado NO prueba ausencia
   (d3 estaba presente aliaseado, B224). La presencia se prueba por idiom/tag, no por el string del paquete.

## 225.5 — Conexiones y qué queda

- Consolida **[Block 216]**–**[Block 224]** (BG1-BG9 del focus). Divergencia de versión: corpus static 1.7.7.75,
  station viva 1.7.5-43 (schema config v14 común, B217 §217.8).
- **Pendiente del focus**: **BG11** (B226) — documentar `chihuahua` con estas mismas dimensiones + comparación de
  capacidad builder + plan de portabilidad (pedido del usuario); **BG13** (B227) — modernización del stack (qué
  stack hoy / qué mejorar, pedido del usuario).
- **Fase dinámica abierta**: experimento de escritura supervisado en la station viva (crear un dashboard más
  completo end-to-end) — backup `bf70f28f…` listo; opcional según decisión del usuario.
