#!/usr/bin/env bash
# SessionStart hook — Niagara research protocol for this project.
# Emits additionalContext that Claude reads at the start of every session.
#
# UBICACIÓN: vive acá (tools/hooks/) y NO en .claude/, porque .claude/ está en
# .gitignore — mezcla config con estado local. Este archivo es el contenido
# valioso y tiene que viajar con el repo. Lo referencia .claude/settings.json
# vía $CLAUDE_PROJECT_DIR/tools/hooks/, que sí es local por máquina.
# Si clonás el repo en otra máquina, replicá ese settings.json.
#
# v3 (2026-07-26): tras dos auditorías independientes (una con evidencia de la
# sesión B271-B288, otra ciega). Cambios respecto de v2:
#   - PASO 0: cómo EMPEZAR (focus activo, estado, numeración) y cómo CERRAR
#   - regla de desempate cuando dos fuentes se contradicen
#   - CORREGIDO: docSource SÍ cubre com.tridium (590 .java) — la v2 mentía
#   - CATALOG.md es GENERADO: se corre gen-catalog.py, no se edita a mano
#   - set completo de marcadores (METHODOLOGY §3), no los 3 de la v2
#   - falla de herramienta ≠ resultado cero
#   - INDEX.md está congelado en B130: no es evidencia de ausencia
# Fallback: si falta jq, emitimos el contexto igual por stderr en vez de morir mudo.

set -euo pipefail

read -r -d '' CTX <<'EOF' || true
PROTOCOLO DE INVESTIGACIÓN NIAGARA N4 (proyecto niagara-research)

────────────────────────────────────────────────────────────────────────
PASO 0 — Orientarse ANTES de investigar (3 lecturas, siempre)
────────────────────────────────────────────────────────────────────────
  1. FOCUSES.md: qué focus está activo, cuáles quedaron planned/paused, y la
     cola sembrada para la próxima sesión. Si hay un focus planned/paused, ESE
     es el trabajo por defecto — ya tiene estado y backlog commiteados. No
     abras uno nuevo sin decirlo.
  2. RESEARCH-STATE-<focus>.md: gaps abiertos, cuál es el NEXT, y el envelope
     <!-- research-state.v1 --> con los contadores.
  3. Próximo número de bloque = el máximo de CATALOG.md + 1. La numeración es
     GLOBAL y compartida entre focuses; hay huecos históricos (falta B43), no
     los rellenes.
  4. mem_context + mem_search del tema antes de tocar archivos.

  Decile al usuario en una línea: focus · gap NEXT · próximo número de bloque.
  Si pide otra cosa, hacés otra cosa — pero el default no es preguntar en el vacío.

  NO APLICA como gate si la sesión es meta-trabajo (auditar el kit, tooling,
  retro) o una pregunta simple sobre un bloque existente. Decilo y seguí.

  METODOLOGÍA BASE — las referencias "§N" de los bloques apuntan acá:
  /home/cristian/investigacion/sdd-investigacion/research-sdd/METHODOLOGY.md
  (§3 marcadores · §5 SOURCES · §8 criterio de parada · §11 self-verify ·
   §12 fase dinámica · §14 consistencia · §16 multi-focus)
  Leé la sección puntual cuando la necesites; no la leas entera de arranque.

────────────────────────────────────────────────────────────────────────
LAS TRES FUENTES — acumulativas, no alternativas
────────────────────────────────────────────────────────────────────────
  Cada una da algo distinto y ninguna reemplaza a otra. "Elegir cuál" no es opción.

  OBLIGATORIO consultar las TRES antes de: cerrar un gap · afirmar que algo NO
  existe o NO está documentado · contradecir un bloque previo.
  Para lookups intermedios usá la que corresponda y seguí.

  (Real: saltear niagara-help produjo la conclusión falsa "el framing MS/TP no
  está documentado" cuando la doc oficial tenía frame types, CRC-16/CRC-32,
  COBS y las máquinas de estado. Hubo que revisar el bloque entero.)

  ── FUENTE 1: los bloques del propio proyecto ──
    rg -i '<término>' CATALOG.md
    rg -il '<término>' niagara-mental-model-bloque*.md
    mem_search '<término>'
    SUFICIENCIA: al menos DOS términos distintos — el nombre Niagara y el del
    dominio/protocolo. Si un hit parece cercano, ABRÍ el bloque y leé la sección.
    OJO: INDEX.md está congelado en ~B130. "No aparece en INDEX" NO es evidencia
    de ausencia. CATALOG.md es el autoritativo para existencia y conteo.
    Si un bloque ya contesta: NO lo re-derives. Citalo y avanzá.
    (Real: B27 ya tenía una conclusión que una pasada posterior reconstruyó de cero.)

  ── FUENTE 2: niagara-help (guías y docs oficiales de Tridium) ──
    .../OptimizerSupervisor-N4.14.0.162/niagara-help/tools/niagara_help.py
      find · guide-search · devguide-search · class · slots · source-grep · freshness
    ✔ DA: hardware, diagnóstico, workflow de Workbench, seguridad, EngNotes
      (así aparecieron el coprocesador SAM4S, la cola de 5, el "machine user" de
       BACnet/SC, las máquinas MNSM/RFSM, tTurnaround)
    ✘ NO DA: internals de encoding/codecs — 6 consultas seguidas dieron cero
    Consultalo igual SIEMPRE: es barato y el cero también es dato.

  ── FUENTE 3: código (corpus organized/ + module-navigator) ──
    Corpus ya extraído, NO re-decompilar: /home/cristian/modules/Prototipos/modulos/organized
    module_nav.py: .../OptimizerSupervisor-N4.14.0.162/module-navigator/tools/module_nav.py

    PRIORIDAD, de mayor a menor fidelidad:
     a) FUENTE ORIGINAL DE TRIDIUM (no decompilada, con javadoc real):
          organized/docSource/docSource-doc/extracted/<jar>/<paquete>/Clase.java
        Cubre javax.baja.* (~2600 .java en extracted/) Y ~590 de com.tridium.*
        (ej. test-wb, kitLon-rt). VERIFICÁ ACÁ SIEMPRE, incluso para com.tridium,
        antes de caer a (b).
     b) Decompilado: organized/<módulo>/<artefacto>/vineflower/... (preferido).
        decompiled/ y procyon/ son EL MISMO código con otro decompilador, no
        cobertura extra — usalos solo si vineflower sale ilegible.
     c) Bytecode para javap -p: organized/<módulo>/<artefacto>/extracted/...
     d) RECURSOS EMPAQUETADOS (XML, .properties, tablas) — miralos ANTES de
        inferir de la lógica. Fueron oro repetidas veces (objectTypes.xml,
        vendors.xml en bacnet-rt: enums completos que el código solo indexa).
          module_nav.py resources <módulo> --type xml
        Un enum, un mapa de vendors o una tabla de tipos casi nunca está en el
        código: está en un XML adentro del jar.

    Todo artefacto externo que el bloque CITE se registra en sources/SOURCES.md
    (ruta · tipo · origen · fecha · sha256 · bloques que lo citan). METHODOLOGY §5.

────────────────────────────────────────────────────────────────────────
CUANDO DOS FUENTES SE CONTRADICEN
────────────────────────────────────────────────────────────────────────
  No es empate, hay orden.

  Para el COMPORTAMIENTO REAL (defaults, límites, valores de esta versión):
      código > doc oficial > bloque propio previo
  El código es lo que corre; la doc describe intención y suele estar vieja.
  (Real: maxInfoFrames — un bloque propio decía 1, la doc de Tridium decía 50,
   el código decía 20. Ganó 20.)

  Para la INTENCIÓN / el contrato / el porqué: doc oficial > código.
  Evidencia empírica de sistema vivo ([CERT-hw]/[CERT-live]) gana sobre ambos.

  OBLIGATORIO ante contradicción: citá las lecturas con marcador y file:line o
  §sección, nombrá cuál ganó y por qué, y si perdió un bloque propio CORREGILO.
  Un valor contradicho que queda vivo envenena la próxima pasada — así llegó acá
  el "default 1".

────────────────────────────────────────────────────────────────────────
DISCIPLINA DE EVIDENCIA
────────────────────────────────────────────────────────────────────────
  Marcadores (set completo en METHODOLOGY §3):
    [CERT-hw] / [CERT-live]  empírico contra sistema vivo — rango más alto
    [CERT]      verbatim en código local (citá file:line)
    [CERT-doc]  verbatim en doc oficial de Tridium (citá §sección)
    [CERT-web]  fuente web oficial (URL + fecha)
    [INFER]     derivado — decilo, no lo disfraces de hecho
  SIN CITA NO HAY MARCADOR: si no podés citar, es [INFER].
  [INFER] es legítimo; lo prohibido es la afirmación sin marcador.

  Si investigás una sospecha y resulta nada, DECILO IGUAL: una sospecha
  descartada con evidencia vale tanto como un hallazgo.

  FALLA DE HERRAMIENTA ≠ RESULTADO CERO:
   · CERO REAL (corrió, no hay hits) → registralo con la query LITERAL para que
     la próxima pasada no reintente.
   · FALLA (traceback, índice viejo, comando inexistente) → NO es un cero.
     Probá `niagara_help.py freshness`. Una fuente que no se pudo consultar
     BLOQUEA cualquier hallazgo negativo: es "no verificado", nunca "no existe".

  UN GAP ESTÁ CERRADO cuando: la afirmación central es [CERT] con cita, el
  self-verify no tiene claims sin marcador, y sabés nombrar qué quedó afuera.
  Si no llegás, ESTRECHALO: cerrá lo verificado y bautizá lo que queda como gap
  hijo con ID citable B<bloque>-G<n> (ej. B276-G1/G2, cerrados después por B288,
  que los nombra en su encabezado). Registralo en el RESEARCH-STATE.
  Un gap hijo con nombre se puede cerrar; "queda pendiente profundizar" se pierde.

────────────────────────────────────────────────────────────────────────
HEURÍSTICAS DE BÚSQUEDA (aprendidas a los golpes)
────────────────────────────────────────────────────────────────────────
  · Para encontrar un encoder/emisor seguí la CADENA DE LLAMADAS, no grepees por
    nombre de clase: en bacnet-rt el emisor ASN se llama según el CONCEPTO BACNET
    (BBacnetLogRecord), no según la clase Niagara (BBacnetTrendRecord).
  · Antes de concluir "X no existe": dos nombres distintos + `module_nav.py
    callers <Clase>`.
  · Una constante escrita distinto en dos lugares (hex vs decimal) se escapa de
    un grep ingenuo.

────────────────────────────────────────────────────────────────────────
CERRAR: qué deja una sesión terminada
────────────────────────────────────────────────────────────────────────
  · Bloque en INGLÉS (convención desde B115), numerado según PASO 0.
    Estructura: encabezado con fuentes y alcance · secciones numeradas ·
    self-verify con tabla de claims + tally · Connections · gaps abiertos.
    Si dudás del formato, copiá la estructura del bloque de número más alto.
    Un bloque sin tabla self-verify NO está terminado.
  · CATALOG.md NO se edita a mano — es GENERADO. Corré desde la raíz:
        python3 tools/gen-catalog.py
    La fila sale del H1 del bloque, así que escribí un H1 descriptivo.
    INDEX.md sí es manual, si lo tocás.
  · RESEARCH-STATE-<focus>.md: la línea del gap, la sección Coverage, y los
    contadores del envelope research-state.v1.
  · Si el bloque nuevo CORRIGE a uno viejo: corregilo en el nuevo Y editá el
    viejo con un puntero. Vale igual para bloques propios de la misma sesión.
  · mem_save con el topic key del focus: research/niagara/<focus>/gaps o
    .../progress (esquema en FOCUSES.md). No inventes claves.
  · mem_session_summary antes de cerrar.

  Reportá al usuario, una línea por fuente: qué query y qué dio (incluidos los ceros).
EOF

if command -v jq >/dev/null 2>&1; then
  jq -n --arg ctx "$CTX" \
    '{hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: $ctx}}'
else
  printf 'niagara-research-protocol: falta jq, no se pudo emitir JSON\n' >&2
  printf '%s\n' "$CTX" >&2
  exit 0
fi
