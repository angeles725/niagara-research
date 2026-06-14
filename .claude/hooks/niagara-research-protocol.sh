#!/usr/bin/env bash
# SessionStart hook — Niagara research protocol for this project.
# Emits additionalContext that Claude reads at the start of every session.
# Scope: project-local (.claude/settings.json), so it only applies to niagara-research.

set -euo pipefail

read -r -d '' CTX <<'EOF' || true
PROTOCOLO DE INVESTIGACIÓN NIAGARA N4 (este proyecto — niagara-research)

En este proyecto, toda sesión es investigación de Niagara N4. Antes de responder
cualquier pregunta de investigación seguí SIEMPRE este orden:

1. PRIMERO buscá en los bloques .md del propio proyecto:
   - niagara-mental-model-bloque*.md
   - INDEX.md
   - CATALOG.md
   Esa es la fuente de verdad ya destilada; revisala antes de abrir cualquier herramienta.

2. Herramientas de investigación disponibles (decidir CUÁL con el usuario):
   - niagara-help  -> /home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/tools/niagara_help.py
       (API pública: firmas oficiales, clases/métodos públicos, guías, jerarquías)
   - module-navigator -> /home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/module-navigator/tools/module_nav.py
       (código decompilado real: 51K clases com.tridium.*, callers, tokens, source)

3. Corpus extra de módulos de prototipos (distinto del set base 4.14):
   - /home/cristian/modules/Prototipos/modulos        (los .jar crudos)
   - /home/cristian/modules/Prototipos/modulos/organized   <- FUENTE PRIORITARIA YA EXTRAÍDA
     (Centraline, Sylk, Reflow y ~todos los módulos. NO re-decompilar: ya está hecho.)

   PRIORIDAD DE CONSULTA dentro de organized/ (de mayor a menor fidelidad):
   a) FUENTE ORIGINAL DE TRIDIUM (NO decompilada, con javadoc real) — usar SIEMPRE primero:
        organized/docSource/docSource-doc/extracted/<modulo-jar>/<paquete>/Clase.java
        ej: organized/docSource/docSource-doc/extracted/workbench-wb/javax/baja/workbench/BWbEditor.java
        (~5200 .java de fuente real: javax.baja.*, jerarquías BWidget/BWbEditor/BWbPlugin, etc.)
   b) Decompilados por módulo (cuando docSource no cubre esa clase):
        organized/<grupo>/<artefacto>/vineflower/<paquete>/Clase.java   (preferido)
        organized/<grupo>/<artefacto>/{decompiled,pipeline/procyon}/...  (alternativos)
        (~51000 .java decompilados — com.tridium.* y módulos de terceros)
   c) Bytecode para javap -p (firmas/flags exactos):
        organized/<grupo>/<artefacto>/extracted/<paquete>/Clase.class
   d) Bajadoc oficial: organized/docDeveloper/docDeveloper-doc/...*.bajadoc

   REGLA: para clases del framework (javax.baja.*) la fuente original de (a) gana sobre
   cualquier decompilado y sobre re-correr module-navigator. Verificá (a) ANTES de decompilar.

ACCIÓN OBLIGATORIA AL INICIO DE LA SESIÓN:
Antes de arrancar la investigación, decile al usuario que primero vas a revisar los
bloques .md del proyecto y preguntale si además debés usar niagara-help y/o
module-navigator para esta investigación. Esperá su respuesta antes de elegir herramienta.
EOF

jq -n --arg ctx "$CTX" \
  '{hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: $ctx}}'
