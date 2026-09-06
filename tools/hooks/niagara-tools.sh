#!/usr/bin/env bash
# SessionStart hook — TOOLBELT del proyecto niagara-research.
# Emite additionalContext con la UBICACIÓN y el uso de cada herramienta local:
# el código decompilado (organized/), module-navigator, niagara-help y el
# toolbelt de RE del kit research-sdd. Complementa al hook de PROTOCOLO
# (niagara-research-protocol.sh); este es la TARJETA de "dónde vive cada cosa".
#
# UBICACIÓN: vive en tools/hooks/ (NO en .claude/, que está en .gitignore) y lo
# referencia .claude/settings.json por $CLAUDE_PROJECT_DIR/tools/hooks/, así las
# dos piezas viajan con el repo. Paths verificados 2026-08-24.
# Fallback: si falta jq, emite el contexto por stderr en vez de morir mudo.

set -euo pipefail

read -r -d '' CTX <<'EOF' || true
HERRAMIENTAS DEL PROYECTO NIAGARA-RESEARCH — dónde vive cada cosa y cómo se usa

Las tres fuentes del PROTOCOLO se apoyan en estas herramientas locales. Rutas
absolutas verificadas; NO re-decompilar ni re-descargar lo que ya está acá.

────────────────────────────────────────────────────────────────────────
1. organized/ — el CÓDIGO decompilado de los módulos N4 (FUENTE 3 del protocolo)
────────────────────────────────────────────────────────────────────────
  Raíz: /home/cristian/niagara-research/organized/   (~665 módulos ya extraídos)
  OJO: el hook de protocolo cita /home/cristian/modules/Prototipos/modulos/organized
  — esa ruta NO existe en este entorno; el corpus vivo es el de acá adentro.

  PRIORIDAD de fidelidad (de mayor a menor), por METHODOLOGY §6:
   a) FUENTE ORIGINAL TRIDIUM (javadoc real, no decompilada):
        organized/docSource/docSource-doc/extracted/<jar>/<paquete>/Clase.java
      Cubre javax.baja.* y ~590 clases com.tridium.*. VERIFICÁ ACÁ PRIMERO.
   b) Decompilado preferido:
        organized/<módulo>/<artefacto>/vineflower/...
   c) Mismo código, otro decompilador (usar solo si vineflower sale ilegible):
        organized/<módulo>/<artefacto>/{decompiled,procyon}/...
   d) Bytecode para javap -p:
        organized/<módulo>/<artefacto>/extracted/...
  RECURSOS empaquetados (XML/.properties/tablas de enums/vendors): MIRALOS antes
  de inferir de la lógica — un enum casi nunca está en el código, está en un XML.
  (Nota: la capa de licensing NO tiene docSource original; su [CERT] sale de
   organized/baja/baja/vineflower/{javax/baja/license,com/tridium/sys/license}/.)

────────────────────────────────────────────────────────────────────────
2. module-navigator — consultas estructuradas sobre organized/
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/module-navigator/tools/module_nav.py <cmd>
  Comandos útiles: resources <módulo> --type xml · callers <Clase> · callees <Clase>
  · class/method/field/string/inheritance indexes. Para "¿X no existe?" usá
  `callers <Clase>` con DOS nombres distintos antes de concluir ausencia.

────────────────────────────────────────────────────────────────────────
3. niagara-help — doc OFICIAL de Tridium (FUENTE 2 del protocolo)
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/niagara-help/tools/niagara_help.py <cmd>
  Comandos: find · guide-search · devguide-search · class · slots · source-grep · freshness
  Cubre: guías, devguide, bajadoc, EngNotes (hardware, diagnóstico, workflow WB,
  seguridad). NO cubre internals de encoding/codecs. El cero también es dato:
  registralo con la query literal. Copia en el install:
    /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/tools/niagara_help.py

────────────────────────────────────────────────────────────────────────
4. corpus-nav — navegación de NUESTROS bloques (FUENTE 1 del protocolo)
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/tools/corpus-nav.py <cmd>
  Comandos: find <q> · grep <regex> · show <N> · list [--focus <f>] ·
  by-marker <marcador> · by-focus <focus> · connections <N> · stats
  Navega los 720+ bloques + docs/ + retros/. Reindexá en CADA corrida, así
  toma bloques nuevos solo (sin mantenimiento). Búsqueda LÉXICA (por palabra
  exacta); la SEMÁNTICA (por significado) aún NO existe — es la propuesta P1
  de retros/2026-08-31-corpus-navigation-improvement-retro.md. Stdlib puro,
  ~0.1-0.2s por búsqueda. Doc en tools/README.md.

────────────────────────────────────────────────────────────────────────
5. RE toolbelt (kit research-sdd) — binarios nativos (.dll/.exe/.so)
────────────────────────────────────────────────────────────────────────
  KIT=~/investigacion/sdd-investigacion/research-sdd
  $KIT/toolbelt/detect-tools.sh --require ghidra     (gate del entorno)
  $KIT/toolbelt/decompile-native.sh {quick,r2,ghidra,ghidra-evidence} <bin>
  $KIT/toolbelt/corroborate-native.sh <bin>          (r2 estático — corrobora)
  Disponibles: Ghidra 12.1.3 + radare2 + objdump/readelf/nm. REGLA: un decompile
  NO es evidencia hasta corroborarlo (offset sin anclar puede pegar a un binario
  gemelo — ver B424). Binarios N4 en /mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/bin/.

────────────────────────────────────────────────────────────────────────
6. Registro y catálogo (al cerrar)
────────────────────────────────────────────────────────────────────────
  sources/SOURCES.md — todo artefacto externo que un bloque CITE se registra acá
  (ruta · tipo · origen · fecha · sha256 · bloques que lo citan). METHODOLOGY §5.
  python3 tools/gen-catalog.py — regenera CATALOG.md desde el H1 de cada bloque.
  CATALOG.md NO se edita a mano.

────────────────────────────────────────────────────────────────────────
7. dashboard-preview — iterar diseño de un módulo -ux ANTES de compilar
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/tools/dashboard-preview.py --rc <mod>/src/rc --prefix /dashboardpan
  Ruta /hmi (http://localhost:<port>/hmi) = SIMULADOR HMI: el dashboard en un marco
  de panel WEB-HMI10/CF 1280x800, escalado a la ventana — para ver cómo queda.
  Sirve la carpeta rc/ real por http://localhost + mockea la API del servlet, para
  editar HTML/CSS/JS → refrescar → ver, SIN build+firma+deploy. Reusable para
  cualquier módulo dashboard. Reproduce la GUARDA XHR (atrapa el bug del header
  X-Requested-With antes de compilar). --mock <file.json> para datos; sin él, /api/*
  da {} (el diseño/paleta se ve igual). Ejemplo con mock animado: el propio módulo
  puede traer un preview-server.py (ver DashboardPan-ux/preview-server.py).
  --config-login (C9 R14): previsualiza el "segundo login dentro del dashboard antes de
  escribir" sobre el rc/ REAL sin tocar el módulo — inyecta un modal nativo + chip de
  sesión + tira change_log y mockea /api/config/login|logout (+ /api/config/session solo-mock) con estado (401/200+
  cookie, TTL deslizante --config-ttl, clave demo --config-password). Escribir sin
  sesión → 403 + modal; con sesión → 200 + fila change_log (superficie B).

────────────────────────────────────────────────────────────────────────
8. bog-nav — navegar el config.bog de una ESTACIÓN (grafo guardado)
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/tools/bog-nav.py <config.bog|file.xml> <cmd>
  SOLO LECTURA. Un config.bog es un ZIP cuyo file.xml es el árbol BOG-XML de
  componentes (<p h='handle' t='pfx:Type'>) + links que viven en el componente
  DESTINO y apuntan al origen por sourceOrd='h:xxxx'. Reutiliza el motor de
  gramática + grafo de handles de bog-audit.sh del kit (main 3f666a0).
  Comandos: tree · slot <path|h:handle> [slot] [--src] · links [--to][--from][--slot]
   [--dangling --src] · handle <h:xxxx> · path <h:xxxx> · find --type PFX:Type ·
   writable [--module][--klass][--src] · relays [--module] · hoa [--module][--all] ·
   tiles · grep · diff <bogB> · selftest.  --json Y --csv en TODOS.  Lee .bog y .dist
   (backup de estación con config.bog anidado).
  Contesta (probado en PANCCADIA config.bog):
   · ¿qué link alimenta Cuarto1.setpoint? → resuelve h:xxxx a RUTA
     (Cuarto1.setpoint → Programacion/ColdRoom_1.setpoint) — grep NO puede.
   · Cuarto1.setpoint es ORIGEN, no destino → la escritura externa PEGA y propaga.
   · relays (CHECK11): 22 salidas propias → proxy writable, 17 SIN fallback (B810).
   · hoa (CHECK8): 19 slots mode/HOA en auto, 0 override de priority-array.
   · tiles (CHECK18): Cuarto1 tiene las unidades 1/3 CRUZADAS (tile≠unidad física).
   · writable clasifica por forma de escritura externa (StatusNumeric = complejo,
     hijo `…/value` bare <real> PREFERIDO por B826/B825; simple; bare).
   · --src rellena el tipo de un slot "frozen" que el bog guarda sin t= (double).

────────────────────────────────────────────────────────────────────────
9. module-find — buscar en el CÓDIGO Java de un módulo
────────────────────────────────────────────────────────────────────────
  python3 /home/cristian/niagara-research/tools/module-find.py <src-root> <cmd>
  SOLO LECTURA, poda dot-dirs. Une los @NiagaraProperty/@NiagaraAction multilínea
  por BALANCE DE PARÉNTESIS (un grep parte en el salto de línea y pierde la cola
  flags=/type=). Reutiliza el motor de escaneo de fuente de bog-audit.sh.
  Comandos: slots [--type][--flags o/s/h/r/t][--name] · actions [--name] ·
   writers <slot> · extends [--of CLASS] · ords [--name] · slot-types · ext-writable ·
   compare <root> <srcB> · callers <method> · grep · selftest.  --json Y --csv en TODOS.
  Contesta (probado en cliente Leon-Guanjuato):
   · ¿el nombre del servlet lo hereda de BWebServlet? extends --of BDashboardServlet
     → BDashboardServlet -> BWebServlet.
   · slots --flags OPERATOR --type BStatusNumeric → BRoomPanel.setpoint (el caso
     que la lint S19 debe marcar: propiedad compleja OPERATOR sin acción).
   · ext-writable = preview de la lint S19 (compleja OPERATOR sin acción → WARN +
     nota del hijo `…/value`); slot-types = tabla resumen por tipo Java.
   · compare <root> <srcB> = diff de esquema entre dos versiones (4f5f1c7→a109249:
     defrostSkipped/lastSkipReason/forceDefrost AÑADIDOS, 0 riesgo de esquema).
   · writers <slot> distingue escritor ESTÁTICO (setX(/.set("slot",) del DINÁMICO
     (obj.set(prop,…) resuelto en runtime — así escribe el servlet el setpoint).
EOF

if command -v jq >/dev/null 2>&1; then
  jq -n --arg ctx "$CTX" \
    '{hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: $ctx}}'
else
  printf 'niagara-tools: falta jq, no se pudo emitir JSON\n' >&2
  printf '%s\n' "$CTX" >&2
  exit 0
fi
