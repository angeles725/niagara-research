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
EOF

if command -v jq >/dev/null 2>&1; then
  jq -n --arg ctx "$CTX" \
    '{hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: $ctx}}'
else
  printf 'niagara-tools: falta jq, no se pudo emitir JSON\n' >&2
  printf '%s\n' "$CTX" >&2
  exit 0
fi
