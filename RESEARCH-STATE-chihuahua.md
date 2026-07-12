# RESEARCH-STATE — Focus `chihuahua`

> Focus de **código fuente PRIMARIO** (no decompilado): módulo Niagara N4 dashboard `chihuahua` (Honeywell
> MX60 BMS), autoría propia `com.angeles.chihuahua`. Método: **lectura directa + CodeGraph** (tenemos la
> fuente Java + JS + docs propios), NO decompilación. Objetivo declarado: reconstruir su arquitectura mapeando
> **las mismas dimensiones que el focus `nmodsreflow`**, para habilitar la comparación chihuahua↔Reflow y el
> análisis de brechas que el usuario pidió DESPUÉS de documentar.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`). Engram:
> `research/niagara/chihuahua/{progress,gaps}` (proyecto `niagara-research`).
<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 233
gaps_closed: 0
known_gaps: 0
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->


## Subject (dónde vive la fuente)

`/home/cristian/modulos_niagara_n4/Cliente/Honeywell/MX60/chihuahua/chihuahua/` — partes `chihuahua-{rt,ux,wb}/src`.
El corpus (bloques) vive en `niagara-research`; el sujeto es ese árbol de fuente.

## Sensibilidad

Proyecto de fuente, PERO despliegue de cliente real → hay `.env.local` (IP JACE / credenciales de deploy) y
`.env.local.example`. **SECRETS DISCIPLINE parcial:** no leer ni citar valores de `.env.local`/keystores; el
código fuente sí es citable normalmente.

## Ángulo (§b2)

Reconstruir identidad + espina HTTP + subsistemas + frontend + postura RBAC/audit + tooling WB, con las mismas
dimensiones que Reflow (B138-B155). Diferenciadores ya vistos: frontend ES5 IIFE `window.MX60` (no Vue),
**RBAC write-gate en cada endpoint mutante** (Reflow NO tenía), audit trail, parte Workbench `BBatchLinkEditor`
(Reflow no tiene), gestión SDD/openspec.

## Cobertura

- **Métrica:** 14 / 14 gaps cerrados (1.00). **Documentación de subsistemas COMPLETA** (C1-C14, B163-B176).
- **Bloques del focus:** B163 esqueleto · B164 RBAC · B165 servlet · B166 alarmas · B167 audit · B168 protección · B169 equipment/estado · B170 subscripción · B171 write-path · B172 WB tool · B173 links · B174 history · B175 schedule · B176 build/tests.
- **Comparación:** B177 (chihuahua↔Reflow, diferencias + brechas) — ENTREGADA. Objetivo del usuario completo.

## Backlog (matriz de cobertura → 14 gaps, derivada del barrido de auditoría §13)

| Gap | Descripción | Prioridad |
|---|---|---|
| C1 | Esqueleto: identidad, tri-parte, espina servlet `/mx60/` + dispatch + guards + mapa de endpoints + headline RBAC | **alta** (B163) |
| C2 | Security/RBAC deep: cada POST llama `checkCanWrite` primero; riesgo OPERATOR_WRITE global-vs-category; fail-closed | **alta** |
| C3 | Paridad de superficie servlet: endpoint×método×auth-guard chihuahua vs Reflow | alta |
| C4 | Alarms: `ChiAlarmHelper`/`ChiAlarmQueryHelper` (BQL, dedup, grouping, ackAll) | media |
| C5 | Audit trail: `auditLog` ring, merge con SecurityHistory logins, TODO login-history id | media |
| C6 | Threshold/protection state-machine: `BChiUp` 49 slots + control-tick 10s + allowlists | media |
| C7 | Equipment reader + config/state model: `ChiEquipmentReader`, auto-provisioning, persistencia `alarmLatches`/`userThemes` | media |
| C8 | Frontend subscription topology: `SubscriptionPool`/`EquipmentData`/`EquipmentSnapshotStore` (baja + fallback REST) | media |
| C9 | Frontend write-path: `_bajaSetBroken=true` (XHR setpoint) vs doc "baja-native preferred"; catálogo XHR vs baja | media |
| C10 | Workbench tool: `BBatchLinkEditor` validate/commit-transaction + `model/` (chihuahua-only) | media |
| C11 | Link export/import: `ChiLinkHelper` slot-path-ord → `chih-links.json`, idempotencia | baja |
| C12 | History: stride downsampling, range vocab, `equipment-histories` | baja |
| C13 | Schedule: NumericSchedule BQL + filtro BChiUp-parent + WebScheduler iframe | baja |
| C14 | Build/deploy + slot-freeze + test infra (niagaraTest discovery = 0, HANDOFF) | baja |

## Historia de iteraciones

| Iter | Gap | Bloque | delegado? · modelo | Notas |
|---|---|---|---|---|
| (bootstrap) | — | — | sí · audit sweep (general-purpose) | barrido §13 → matriz de cobertura + 14 gaps; spot-check §11 de 4 citas = OK |
| 1 | C1 | B163 | no · inline (sobre barrido) | esqueleto: identidad v1.3 tri-parte, servlet `/mx60/`+dispatch+guards+endpoint map, RBAC write-gate headline |
| 2 | C2 | B164 | sí · sonnet (deep-read) | RBAC: `checkCanWrite` fail-closed en los 8 handlers de control (theme exento auth-only); riesgo OPERATOR_WRITE global-vs-category (documentado :274-280); capability decorativo/server-autoritativo; spot-check §11 OK |
| 3 | C3 | B165 | sí · orquestado | superficie servlet: dispatch puro `RouteAction`, mapa GET/POST completo, 3 guards, 423 lock solo setpoint; 76 CERT; spot-check OK |
| 4 | C4 | B166 | sí · orquestado | alarmas: query BQL paginación por contador, latch en slot `alarmLatches`, notas en `^chihuahua-alarm-notes.json`, ackAll colecta+baja-native; 80 CERT |
| 5 | C5 | B167 | sí · orquestado | audit: ring ~500 fire-and-forget en 7 rutas, merge SecurityHistory; login-history TODO en realidad resuelto; NET-ADD vs Reflow; 86 CERT |
| 6 | C6 | B168 | sí · orquestado | protección: state-machine permanent-latch (sin hysteresis), control-tick 10s + COV, cascada asimétrica, allowlist thresholds; 56 CERT |
| 7 | C7 | B169 | sí · orquestado | equipment/estado: auto-provisioning seed-before-add, 88 equipos reales (vs 68 doc, §14 deriva), userThemes/auditLog persistentes .bog, alarmLatches por-BChiUp; 56 CERT |
| 8 | C8 | B170 | sí · orquestado | subscripción: window.MX60 IIFE, baja push vs fallback REST 5s (mutuamente excluyentes), SnapshotStore coalescing RAF+500ms (lo que Vue haría solo); 47 CERT |
| 9 | C9 | B171 | sí · orquestado | write-path: `_bajaSetBroken=true` latch → setpoint XHR-first (rama baja muerta); ≥4 POST vivos + ackAll híbrido; CapabilityStore decorativo; 34 CERT |
| 10 | C10 | B172 | sí · orquestado | WB tool: BBatchLinkEditor (rwi agent), validate→commit-transaction por-space (sin rollback atómico global), 6 helpers puros WSL-testables; sin equivalente Reflow; 47 CERT |
| 11 | C11 | B173 | sí · orquestado | links: export/import `chih-links.json` ORD slot-path estable, tmp-then-move atómico; gotcha setpoint CORREGIDO (getSlot vs get), guard atSteadyState; 49 CERT |
| 12 | C12 | B174 | sí · orquestado | history: stride downsampling buffer-first (arregla bug 98% descartado), cap 5000/100k, matching link-graph + fallback nombre, 8 rangos; 57 CERT |
| 13 | C13 | B175 | sí · orquestado | schedule: BQL NumericSchedule polimórfico + filtro BChiUp-parent, split listar(JSON)/editar(iframe WebScheduler nativo sin sandbox); 58 CERT |
| 14 | C14 | B176 | sí · orquestado | build/deploy: gradle multi-módulo, cross-version 4.13→4.14 slot-freeze, BUILD_ID -dirty, niagaraTest discovery=0 (bug plugin 7.6.17) + run-tests-wsl.sh; 58 CERT |

## Próxima acción

Escribir B163 (C1, esqueleto). Luego el loop por subsistema. La COMPARACIÓN chihuahua↔Reflow + brechas es un
bloque de síntesis POSTERIOR (lo pidió el usuario al final), no ahora.
