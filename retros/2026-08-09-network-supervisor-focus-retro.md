# §18 Self-retro — focus `niagara-network-supervisor` (B414–B420)

> Fecha: 2026-08-09. Focus cerrado 6/7 (investigable=0; N5 requires-execution). Modo: orquestado-auto, un
> sub-agente por bloque, tier sonnet. Este retro DEDUPLICA contra `2026-08-09-database-focus-retro.md` (mismo
> día): las 4 fricciones grandes (covered_blocks drift, §14 back-pointer en el gate, registro de child-gaps en
> el estado, scoping de verify-state) YA están propuestas ahí — no se repiten. Aquí solo lo NUEVO de este focus.
> NO edita el kit; propone deltas para revisión humana.

## Deltas NUEVOS (no cubiertos por el retro de database)

| # | Fricción (evidencia) | Delta propuesto | Target | Prioridad |
|---|---|---|---|---|
| 1 | **Tier override anidado no disponible en sub-agente.** En B415 el sub-agente intentó un sub-sweep con `model:'haiku'` y falló ("tipo de agente no disponible"), reenrutando a Bash. El nested-delegation con tier del PROMPT-LOOP (§ MODEL TIER "governs NESTED sub-sweeps") no es ejecutable cuando el que delega es a su vez un sub-agente en este harness. | Documentar en PROMPT-LOOP que el nested-tier-override puede no estar disponible en modo orquestado (sub-agente que sub-delega); recomendar Bash directo o Workflow para fan-out determinista. | `PROMPT-LOOP.md` (MODEL TIER) | MEDIUM |
| 2 | **§14 back-pointer: cumplimiento MIXTO.** En este focus los sub-agentes SÍ pusieron el puntero en 2/3 correcciones (B267←B417, B417←B419) pero lo OMITIERON en 1/3 (B405←B418, lo puso el driver). En el focus database fue peor (0/3). Refuerza el delta #1 del retro database, con evidencia adicional de que la instrucción explícita en el prompt mejora el cumplimiento pero no lo garantiza. | Confirma la prioridad HIGH del delta #1 del retro database (gate self-verify que verifique la edición del bloque viejo). | `PROMPT-LOOP.md` step 5 | HIGH (refuerzo) |

## Lo que este focus hizo BIEN (patrón a preservar)

- **Verificar antes de alarmar.** El focus refutó TRES riesgos de seguridad planteados como `[INFER]` sin
  verificar (B267§267.3 lado equivocado del canal; B417 password-en-UI; B419 clave-recuperable-pasiva). La regla
  adoptada del focus `tags` ("todo claim de permisos se verifica contra la semántica real") funcionó: framework-
  semantic checks 1/2 confirmados en B417, y SRP6 documentado con código en B419. Es el contra-ejemplo de valor
  de la disciplina de evidencia — vale como caso de estudio para METHODOLOGY §11.

## Estado

- `review-status: pending` (deltas para revisión humana; el kit NO se edita desde una corrida).
- Engram: espejado en `research/niagara/retro`.
