# RESEARCH-STATE — Focus `chihuahua`

> Focus de **código fuente PRIMARIO** (no decompilado): módulo Niagara N4 dashboard `chihuahua` (Honeywell
> MX60 BMS), autoría propia `com.angeles.chihuahua`. Método: **lectura directa + CodeGraph** (tenemos la
> fuente Java + JS + docs propios), NO decompilación. Objetivo declarado: reconstruir su arquitectura mapeando
> **las mismas dimensiones que el focus `nmodsreflow`**, para habilitar la comparación chihuahua↔Reflow y el
> análisis de brechas que el usuario pidió DESPUÉS de documentar.
> Numeración global de bloques (`niagara-mental-model-bloqueN.md`). Engram:
> `research/niagara/chihuahua/{progress,gaps}` (proyecto `niagara-research`).

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

- **Métrica:** 2 / 14 gaps cerrados (0.14). C1 (B163), C2 RBAC (B164).
- **Bloques del focus:** B163 (identidad + espina servlet + headline RBAC), B164 (RBAC deep).

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

## Próxima acción

Escribir B163 (C1, esqueleto). Luego el loop por subsistema. La COMPARACIÓN chihuahua↔Reflow + brechas es un
bloque de síntesis POSTERIOR (lo pidió el usuario al final), no ahora.
