# Retro — Renderizar Px reales a web + construir el demo de dashboard para el cliente (HARBOR)

Date: 2026-09-09
Author: Opus 4.8 (research session, con Cristian)
Focus: harbor-greenmax-lighting
Scope: convertir las pantallas Px reales de una estación N4 en HTML navegable, iterar un demo
de dashboard web para presentarle al cliente HARBOR (Distech-Mérida, iluminación GreenMAX), y
dejar en Px XML la parte visual de un modelo nuevo (5 horarios + HOA por horario).

## Por qué esta retro

Esta sesión abrió un modo de trabajo distinto al de investigar el corpus: **producir entregables
visuales para un cliente** a partir de los activos reales de la estación (config.bog, Px, imágenes),
manteniendo la disciplina de evidencia. Salieron una herramienta reusable y varias trampas de
proceso (sobre todo: no inventar estilo, partir de lo que el cliente YA tiene) que conviene fijar.

## Qué se produjo / qué funcionó

- **`tools/px-render.py`** (herramienta nueva, stdlib, `selftest` verde): renderiza cualquier Px
  (.px, Presentation XML) a un HTML autocontenido, reproduciendo los widgets del `CanvasPane` en su
  `layout="x,y,w,h"` y embebiendo cada imagen como data URI. Cubre `Label`, `Picture`,
  `ImageButton`, `BackButton`, `BoundLabel`. Autodetecta la raíz `shared/`. Resuelve tanto
  `file:^…` (copia de la estación) como `module://…` (recursos de módulo: `kitPxGraphics`, `kitPx`,
  desde `shared/px/<mod>` o el corpus `organized/<mod>/<mod>-{wb,rt,ux}/extracted/`). Reusable para
  cualquier estación/Px. Convirtió "no puedo ver su Px sin Workbench" en un preview en segundos.
- **Renders fieles de sus pantallas reales**: menú principal, hub `Iluminacion`, menú de tableros
  GreenMAX, y un tablero de detalle (GreenMAX07, 232 widgets). Sirvieron como BASE de diseño.
- **Demo interactivo del modelo nuevo** (`harbor-menu-horarios.html`): sobre la distribución del
  menú real (5 columnas por nivel), con **5 horarios maestros + HOA (Auto/Override/Off)** y
  **selector de horario por circuito**, todo en vivo; incluye un **editor de horario semanal tipo
  Niagara** (rejilla 7 días × 24 h, crear/mover/ajustar/borrar bloques, línea de "ahora"). Datos
  reales (349 circuitos) desde la matriz.
- **`GreenMAX_Iluminacion_horarios.px`**: el Px del menú real + los 5 horarios y su HOA agregados
  como widgets **visuales** (ImageButton toolBar, sin bindings), listo para abrir en Px Editor.

## Trampas y disciplina (para la próxima)

- **NO inventar estilo — partir de lo que el cliente ya tiene.** Los primeros intentos ("muy feo",
  "no dashboardpan") se fueron porque impuse una estética mía. Lo que el cliente quería era: (a) su
  **paleta de marca** (Leviton azul `#001D68` + verde `#76B900`, sacada con PIL de una captura de su
  sitio, no aproximada) y (b) la **distribución de su propio Px**. Lección: cuando el cliente dice
  "como nuestro estilo", eso es una referencia concreta a EXTRAER, no una licencia para diseñar.
- **"Estás creando mal" → verificar, no adivinar.** Ante el reclamo de que el render estaba mal,
  auditar la estructura real del Px encontró el defecto objetivo: no embebía imágenes `module://`
  (huecos en TODAS las pantallas). El resto (posiciones, tipos de widget) estaba bien (0 anidados,
  0 tags sin manejar). Regla del protocolo aplicada: verificar contra la fuente antes de aceptar o
  rebatir un juicio; corregir con prueba.
- **Corrección de modelo con evidencia del cliente.** B842/B847 asumían **4 horarios** y **HOA por
  circuito**. El cliente confirmó **5 horarios** y **HOA por horario en el menú** (Override en un
  horario enciende todos sus circuitos). Se registró en memoria como corrección a esos bloques (aún
  pendiente el puntero dentro de B842/B847 cuando se toquen). El modelo verificado de B847 se
  reorganiza, no se tira: el `HoaMux` (BBooleanSelect numberValues=3) sube al nivel de horario (5) y
  el `SchedMux` por circuito pasa a numberValues=5. `numberValues` 3–10 sigue siendo [CERT].
- **La lógica NO va en Px.** El cliente pidió "poner la lógica en Px Editor"; hubo que corregir: Px
  solo dibuja y enlaza; schedules/HOA/mux son componentes de la estación (wire sheet). Se puede sin
  módulo (schedule + kitControl stock).
- **Artifacts: límite de 5 watches.** Publicar muchas iteraciones agota los watches de la sesión
  (avisos "watch limit reached"); es inocuo para el entregable, solo significa que la sesión no
  escucha republicaciones externas de esos artifacts.

## Reusable / para el kit

- `px-render.py` es genérico y candidato a subir al toolbelt del kit research-sdd (hoy vive en
  `tools/` del proyecto, junto a `bog-nav.py` / `module-find.py` / `greenmax-matrix.py`).
- Patrón de entregable-cliente: extraer paleta real (PIL sobre captura) + distribución real
  (px-render) → construir el demo SOBRE esa base, no sobre un diseño propio.

## Connections

- **B841/B846** — mapa y matriz de HARBOR (16 tableros, 349 circuitos) que alimentan los datos.
- **B842/B847** — modelo de control; corregido a 5 horarios + HOA por horario (memoria de decisión).
- **Retro 2026-09-07 (oBIX)** — misma familia: usar los activos reales de la estación como fuente,
  con disciplina de evidencia.
