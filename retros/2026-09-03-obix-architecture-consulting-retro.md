<!-- review-status: applied 2026-09-05 · kit 272e1ad · PARTIAL — shipped: #1 (§7 re-cite by block id), #2 (§3 teammate claim carries no marker), PR #434; any further delta DEFERRED pending review -->
<!-- Marker lifecycle: the maintainer flips 'pending' to 'applied <date> · kit <sha>' (or 'dismissed') once these deltas are reviewed in the kit; sweep-retros.sh reads this marker (METHODOLOGY §18). -->
# Retro — niagara-research · tooling · 2026-09-03 · Research-SDD self-retrospective (3/3)

> TERCER research-sdd retro del día (previos: oBIX quick-mode; verificar claims cross-session). Este tramo:
> consultoría de arquitectura (pipeline oBIX→cloudflared→Supabase→viewer 3D, lectura híbrida, escritura por
> mini-PC) apoyada en el hallazgo oBIX [CERT] del día, + coordinación con dos sesiones peer. NO corrió loop
> de descubrimiento. Sólo lo que TRANSFIERE al loop; las lecciones de build/doc viven en el kit
> build-n4-module (`2026-09-03-dashboard-contract-port-spec.md`). READ-ONLY sobre el kit — PROPOSES (§18).

## Contexto
El hallazgo oBIX [CERT] (engram #7991: "oBIX PUT escribe una propiedad de BComponent, gated por
operatorWrite; caveat HTTP Basic vs SCRAM") fue la BASE autoritativa de varias decisiones de arquitectura
tomadas después (camino de escritura, "Supabase no es el camino de escritura", endpoint en la mini-PC, el
`/out/` es de control points no de propiedades). Se re-citó el finding repetidamente.

## Proposed kit deltas

> Sólo lo NUEVO. Lo ya cubierto va abajo.

| # | Proposed change | Target (file · §/section) | Evidence | Type | Priority |
|---|---|---|---|---|---|
| 1 | Un finding [CERT] bien citado es un ACTIVO que se cobra después: cuando fundamente una decisión de diseño posterior, RE-CÍTALO por su id/observación en vez de re-derivar de memoria. Nombrar el "quick-mode terminal = engram finding + seed" (retro previo) debe incluir que el seed/observación es la referencia canónica a re-citar, no un archivo muerto. | `METHODOLOGY.md §7` (state/memory) + §20 (quick-mode) | El [CERT] #7991 (oBIX write) fundó ~4 decisiones de arquitectura del día (camino de escritura, endpoint mini-PC, `/out/`), cada una citando #7991 en vez de re-investigar. | reinforce | MEDIUM |
| 2 | Al CORREGIR a un teammate desde conocimiento del source, marca el grado: [CERT] si lo tracé a file:line, [INFER] si es razonado-pero-no-trazado — y dilo explícitamente. Un teammate merece el mismo rigor de marcador que un bloque. | `METHODOLOGY.md §3` (markers) | Corregí el `/out/` del peer (es de control points, no de propiedades planas) marcándolo explícito como "no tracé el path de LECTURA a source, verifícalo" — [INFER], no [CERT], aunque el path de ESCRITURA sí estaba trazado. | reinforce | MEDIUM |
| 3 | Verifica los DATOS contra el contrato antes de afirmar su forma — incluso para autocorregirte. Un artefacto de datos real (un export) desmiente o confirma una caución mejor que el razonamiento. | `METHODOLOGY.md §3/§14` | Había cauteleado al peer que "oBIX podría dar ords crudos distintos a la fachada"; leer el export real (285 claves, todas `CuartoN/slot` `{v,st}` = 57 slots × 5) lo desmintió y corregí mi propia caución. | reinforce | LOW |

## Already covered (do NOT re-add)
- **Verify-before-assert / claims de teammate sin marcador** — deltas #2/#3 son aplicaciones del §3 ya
  propuestas en el retro cross-session del día; aquí sólo se afinan (marca el GRADO; verifica con el dato real).
- **Quick-mode terminal = finding + seed** — propuesto en el retro oBIX del día; #1 sólo agrega "re-cita el finding cuando lo reuses".

## Nota de alcance honesta
Este tramo fue consultoría + documentación + coordinación multi-sesión, NO una investigación: sin bloque,
sin fila de CATALOG, sin gap cerrado. Los tres deltas son refuerzos pequeños a §3/§7/§20. El retro sustantivo
de research del día sigue siendo el de oBIX quick-mode; el finding [CERT] #7991 es lo que dio fruto aquí.
