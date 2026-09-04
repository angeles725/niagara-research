# Self-retro §18 (mejora de /research-sdd) — diagnóstico multi-sesión · oBIX en vivo como oráculo · canonización contra la fuente Tridium · 2026-09-03

Retrospectiva del proceso de investigación en la sesión larga del deshielo/timer de PANCCADIA (N4). No fue el loop clásico de bloques/gaps: fue un DIAGNÓSTICO en vivo de un sistema de producción, con investigación de código + fuente original + sistema vivo. PROPOSED deltas al kit research-sdd (propose-never-apply; el kit no se edita desde una corrida).

## Qué del PROCESO funcionó y merece formalizarse

1. **Diagnóstico DISTRIBUIDO multi-sesión — un patrón de investigación, no anécdota** `[ev: 3 sesiones esta corrida]`.
   Tres sesiones Claude trabajaron el MISMO problema en paralelo, cada una con una fuente distinta, cruzándose hallazgos por SendMessage:
   - una leyó el CÓDIGO (source, mayor fidelidad de comportamiento);
   - otra la FUENTE ORIGINAL Tridium en `organized/` (canonización) + QA + corpus (B729–B746);
   - otra el SISTEMA VIVO por oBIX contra el JACE (`[CERT-live]`).
   Cada una ARBITRÓ ramas que las otras no podían: el código dijo "armTrigger es correcto", la fuente Tridium dijo "falta el hook started()", el oBIX dijo "el ancla está null en vivo → no armó". Ninguna sola habría cerrado el diagnóstico.
   → **PROPOSED METHODOLOGY delta (§16 multi-focus / nueva nota):** cuando el target es un SISTEMA VIVO y hay varias hipótesis, el reparto por FUENTE (código / fuente-original / sistema-vivo) entre sesiones que se cruzan hallazgos cierra ramas más rápido que una sola sesión secuencial. Regla de higiene: cada sesión CITA su fuente con marcador, y una corrige a otra con evidencia (aquí: corregí la severidad MED de CompPan del peer leyendo el source vigente — su dato salió de una decompile vieja).

2. **La lectura del sistema VIVO (oBIX/Slot Sheet) es el ORÁCULO que arbitra entre hipótesis** `[ev: cada rama del diagnóstico]`.
   Las TRES fuentes ya son doctrina, pero el patrón concreto que faltaba: cuando dos hipótesis compiten y el código no las separa, LEER EL ESTADO DEL SISTEMA VIVO las decide. Ejemplos de la sesión: "¿jar viejo?" → los slots nextDefrostTime/defrostStart EXISTEN en el tipo vivo (oBIX) → no es jar viejo. "¿lógica mala o hook no disparó?" → el ancla en vivo está null con mode=interval → el hook no corrió. "¿deshiela?" → invocar el ciclo y leer defrostActive/resistanceOut por oBIX.
   → **PROPOSED METHODOLOGY §3/§12 delta:** elevar `[CERT-live] por oBIX/Slot Sheet` a oráculo de PRIMERA elección para arbitrar hipótesis en un sistema vivo, por encima de re-leer el código. Coletilla obligatoria: leer SIEMPRE el modo/condición junto al valor (un ancla null es by-design en modo schedule; sólo prueba fallo si el modo era el que debía armar).

3. **Canonizar contra la FUENTE ORIGINAL de Tridium en `organized/` para detectar desviaciones** `[ev: started() hook]`.
   El hallazgo decisivo NO salió de razonar nuestro código: salió de comparar nuestro `BDefrostController` contra el `BTimeTrigger` original de Tridium (`docSource`), y luego un SURVEY de ~25+ componentes first-party: "override atSteadyState AND NOT started = 0 hits en TODO Tridium". Ese cero convirtió una sospecha en regla: nuestro módulo era una desviación genuina del idiom universal, no una elección de estilo.
   → **PROPOSED METHODOLOGY §6 delta (técnica de búsqueda):** para validar si una implementación CUSTOM sobre el framework Baja es correcta, buscar el componente Tridium EQUIVALENTE en `organized/docSource` (por CONCEPTO, no por nombre de clase) y comparar hooks/guardas/primitivas; un survey que da 0 hits del anti-patrón en first-party es evidencia fuerte de desviación. Registrar el conteo (el cero también es dato).

## Qué NO funcionó / costo evitable
- Se persiguió un WARNING cosmético (`applyRunCmd` NotRunningException) como si fuera la causa raíz antes de verificar que `changed()` lo atrapa. Lección de research: clasificar el ruido (atrapado vs propaga) antes de invertir una rama entera.
- Hipótesis "montaje tardío" vs "modo/Save" vs "atSteadyState roto" no se separaron hasta preguntar al operador la SECUENCIA exacta (reinició con el módulo puesto o lo movió después). Un dato del operador cerró lo que el código no podía.

## Marcadores usados y su rendimiento
`[CERT]` (código, file:line) y `[CERT-live]` (oBIX) cargaron el 90% del diagnóstico; `[INFER]` quedó para el mecanismo de deploy al ATLAS (no verificado). El set de marcadores del kit funcionó sin fricción.

## Referencias
- Corpus de la sesión Niagara: B729 (canonización started/idiom), B730 (DEFAULT_ON_CLONE), B741 (QA), B742 (refactor), B746.
- Retros del kit build-n4-module (lado módulos): self-firing-timer, hidden-actions, station-atlas, qa-stack, self-retro-preview-gate.
