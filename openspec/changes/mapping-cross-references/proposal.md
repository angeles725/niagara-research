# Proposal — mapping-cross-references

**Project**: niagara-research
**Change**: mapping-cross-references
**Phase**: propose
**Date**: 2026-05-09
**Author**: SDD propose phase (auto+hybrid+auto-chain)

---

## 1. Problem statement

El mapping previo (`mapping-reflow-clean-177`) produjo un catálogo definitivo de 547 símbolos en
`docs/mappings/reflow-clean-177/index.json` — responde la pregunta **"¿qué es este archivo?"**.
Los bloques de investigación próximos (67+, en particular MX60 / Analytics) y los pull-requests
constructivos contra Reflow necesitan responder la pregunta complementaria: **"¿quién usa este
símbolo?"**. Hoy esa información se reconstruye manualmente con `rg` ad-hoc cada vez que se
analiza un componente, mixin, plugin, store module, REST endpoint, BOX method o clase Java.
Eso es repetitivo, inconsistente entre bloques, y no es queryable.

Necesitamos un grafo de uso queryable, persistente, y reutilizable, indexado por símbolo →
`used_at[]`, sibling de `index.json`, generado con la misma metodología que el mapping (per-kind
batching + ripgrep + jq merge).

## 2. Goals & non-goals

### Goals

1. Producir un artefacto canónico `xref.json` queryable con jq, sibling de `index.json`,
   con la misma `id` para hacer join trivial.
2. Cubrir los **10 symbol kinds** definidos en la exploración: `java-class`, `vue-component`,
   `store-module`, `mixin`, `plugin`, `lib-utility`, `rest-function`, `box-method`,
   `ws-command`, `rest-url`.
3. Marcar símbolos sin usos (`unused: true`) para detección de dead code.
4. Schema reutilizable para Analytics y MX60 (core kinds + extension per codebase).
5. Mantenimiento bajo: regenerable end-to-end con un solo comando una vez producido el
   pipeline (sin estado mutable que se desincronice).
6. Integración con `index.json` por `id` — sin duplicar `defined_at`, sin re-scanear definiciones.

### Non-goals

1. **NO** es una herramienta de análisis estático tipo TypeScript LSP / Babel AST. Es ripgrep
   + jq, file-level attribution.
2. **NO** resuelve aliases con precisión simbólica (`import { X as Y }` registra el archivo,
   no la transformación de nombre). Marcamos `usage_kind: import-renamed` cuando se detecta.
3. **NO** captura bindings dinámicos `<component :is="dynamicName">` ni
   `this[methodName]()` ni `require(variable)`. Estimado <5% del total.
4. **NO** construye una UI ni un grafo visual. La salida es JSON + Markdown plano. Cualquier
   visualización es trabajo posterior fuera de este SDD.
5. **NO** captura usos transitivos. Si A usa B y B usa `$niagara`, A no aparece como usuario
   de `$niagara` — solo B.
6. **NO** captura símbolos externos al módulo (Niagara framework, npm dependencies). Esos
   están en `dependencies[]` de `index.json`.

## 3. Deliverable shape (locked)

**Output location**: `/home/cristian/niagara-research/docs/mappings/reflow-clean-177/`
(sibling de `index.json`, mismo directorio — NO se crea `reflow-clean-177-xrefs/`,
se rectifica el path tentativo del state cacheado para mantener cohesión con el mapping).

**Files (target list)**:

| Archivo | Rol | Generación |
|---------|-----|------------|
| `xref.json` | Grafo canónico de usos, jq-queryable | Merge de shards via jq |
| `xref.md` | Resumen humano: top consumers por kind, símbolos unused, métricas | Generado desde `xref.json` con jq + plantilla |
| `xref-schema.md` | Documentación del schema + sección de reusabilidad Analytics/MX60 | Escrito a mano en spec/design |
| `xref-README.md` | Quick-start con ejemplos `rg`/`jq` (find unused, top consumers de `$niagara`, etc.) | Escrito a mano en apply |

**Shards intermedios** (NO se commitean al repo final, son artefactos de pipeline):
`xref-shard-{kind}.json` — uno por kind, generado por sub-agent dedicado.

## 4. Schema commitment (locked)

### 4.1 Per-entry shape

```json
{
  "id": "string",                // SAME id as index.json — key de join
  "symbol": "string",            // FQN para Java, nombre normalizado para resto
  "kind": "java-class|vue-component|store-module|mixin|plugin|lib-utility|rest-function|box-method|ws-command|rest-url",
  "defined_at": "string",        // path desde index.json — copiado, no re-scaneado
  "used_at": [
    {
      "path": "string",          // relative path del archivo que usa el símbolo
      "usage_kind": "string"     // ver enum abajo
    }
  ],
  "usage_count": "integer",      // length de used_at
  "unused": "boolean"            // usage_count == 0
}
```

### 4.2 Top-level envelope

```json
{
  "schema_version": "1.0",
  "xref_for": "reflow-clean-177",
  "generated_at": "ISO-8601",
  "total_entries": 536,
  "entries": [ /* per-entry shapes */ ]
}
```

### 4.3 Symbol kind enum (10 valores, locked)

`java-class`, `vue-component`, `store-module`, `mixin`, `plugin`, `lib-utility`,
`rest-function`, `box-method`, `ws-command`, `rest-url`.

(Los archivos de config/build de `index.json` se excluyen del xref — no son símbolos
referenciables.)

### 4.4 Usage kind enum (locked, 17 valores)

| usage_kind | Aplica a kinds | Significado |
|------------|----------------|-------------|
| `import` | todos los kinds JS/Vue | `import X from '...'` estándar |
| `import-renamed` | lib-utility, plugin | `import { X as Y } from '...'` |
| `extends` | java-class, vue-component | `extends Foo` (Java) o `extends FooComponent` (Vue) |
| `implements` | java-class | `implements IFoo` (Java) |
| `invoke` | java-class, lib-utility | llamada directa a método/función |
| `inject` | plugin | `this.$pluginName.method()` o `inject` (composition API) |
| `map-state` | store-module | `mapState('module', ...)` |
| `map-getter` | store-module | `mapGetters('module', ...)` |
| `map-action` | store-module | `mapActions('module', ...)` |
| `dispatch` | store-module | `store.dispatch('module/action')` |
| `commit` | store-module | `store.commit('module/mutation')` |
| `template` | vue-component | `<ComponentName>` en template |
| `mixin-ref` | mixin | aparece en `mixins: [...]` array |
| `rest-call` | rest-function | invocación vía `$niagara.method()` o `niagara.method()` |
| `box-call` | box-method | invocación vía `$niagara.method()` (BOX path) |
| `ws-call` | ws-command | invocación vía `websocket.cmd()` o `$niagara.ws.cmd()` |
| `dynamic-bind` | vue-component | `<component :is="x">` cuando se puede inferir nombre |

## 5. Pipeline strategy (locked)

**Approach**: per-kind batched sub-agents → JSON shards → jq merge → canonical `xref.json`
+ derivative Markdown. Mismo modelo que `mapping-reflow-clean-177` (probado y archivado).

### 5.1 Stages

1. **Stage 0 — Inputs preflight**: leer `index.json`, agrupar entries por `kind`, generar
   manifest de símbolos por shard (1 shard = 1 kind, salvo `vue-component` que se splitea).
2. **Stage 1 — Per-kind shards (10 sub-agents en paralelo donde haga sentido)**:
   cada sub-agent recibe el subset de símbolos de SU kind + las patrones rg específicos del
   kind + el output schema. Produce `xref-shard-{kind}.json`.
3. **Stage 2 — Merge & validate**: jq merge de los 10 shards → `xref.json`. Validación:
   - todo `id` aparece en `index.json`
   - `usage_count == length(used_at)`
   - `unused == (usage_count == 0)`
   - cada `usage_kind` en el enum locked
4. **Stage 3 — Markdown derivation**: jq + plantilla → `xref.md` (top 20 consumers por kind,
   lista de símbolos unused, métricas). Sin sub-agent — pura transformación.
5. **Stage 4 — Schema + README**: escritos a mano en design/apply (`xref-schema.md`,
   `xref-README.md`).

### 5.2 Order de batches (por valor estructural, mismo orden que exploración recomendó)

1. `store-module` (30 símbolos, ~240 edges, alta densidad — 248 archivos consumidores)
2. `vue-component` (378 símbolos, ~750 edges, mayor volumen — splittear en 5 shards de ~75)
3. `plugin` (13 símbolos, ~260 edges, dominado por `$niagara` con 268 ocurrencias)
4. `mixin` (18 símbolos, ~72 edges)
5. `rest-function` (28 símbolos, ~84 edges)
6. `box-method` (21 símbolos, ~63 edges)
7. `java-class` (77 símbolos, ~110 edges, intra-Java only)
8. `lib-utility` (10 símbolos, ~30 edges)
9. `ws-command` (11 símbolos, ~28 edges)
10. `rest-url` (28 URLs, ~42 edges)

### 5.3 Sub-agent budget per shard

- model: `sonnet` (mismo que mapping-apply, no necesita arquitectura).
- input: subset de símbolos del kind + patrones rg + schema target.
- output: `xref-shard-{kind}.json` válido.
- token budget estricto: ≤80 símbolos por shard. `vue-component` splitea en 5 shards de ~75.

## 6. Effort estimate

| Stage | Sub-agents | Modelo | Tiempo estimado |
|-------|------------|--------|-----------------|
| 0 — Preflight | 0 (inline) | — | 5 min |
| 1 — Shards (10 kinds + 4 sub-shards de vue-component = 14 sub-agents) | 14 | sonnet | 45–60 min wall-clock con paralelismo |
| 2 — Merge & validate | 0 (inline jq) | — | 10 min |
| 3 — Markdown derivation | 0 (inline jq) | — | 10 min |
| 4 — Schema + README | 0 (inline en apply) | — | 15 min |
| **Total** | **14** | — | **~1.5 h wall-clock** |

Output esperado: `xref.json` ~300–400 KB, ~536 entries, ~1,400–1,800 edges.

## 7. Reusability commitment for Analytics / MX60

El schema y pipeline son **reusables a nivel core**. Cada codebase añade extensions.

### Core (cualquier codebase Java + Vue)

- Kinds: `java-class`, `vue-component`, `store-module`, `mixin`, `plugin`, `lib-utility`.
- Schema fields: `id`, `symbol`, `kind`, `defined_at`, `used_at`, `usage_count`, `unused`.
- usage_kinds core: `import`, `import-renamed`, `extends`, `implements`, `invoke`, `inject`,
  `map-state`, `map-getter`, `map-action`, `dispatch`, `commit`, `template`, `mixin-ref`,
  `dynamic-bind`.

### Extensions per codebase

| Codebase | Extensions | Nuevos kinds |
|----------|------------|--------------|
| Reflow-Clean-177 | REST + WebSocket + BOX | `rest-function`, `rest-url`, `box-method`, `ws-command` |
| Analytics (Niagara N4) | DAG algorithm blocks + BQL queries + BOX | `algorithm-block`, `bql-query`, `box-method` |
| MX60 | A definir según presencia REST/WS — heredará core mínimo |

El `xref-schema.md` documentará el contrato core + cómo extender. Cuando se haga el
mapping de Analytics, este SDD es el blueprint copy-and-adapt.

## 8. Out of scope (explicit)

1. **Resolución de aliases simbólica** — `import { X as Y }` registra archivo, no nombre transformado.
2. **Bindings dinámicos** — `<component :is="varName">`, `this[methodName]()`, `require(var)`.
3. **Usos transitivos** — solo direct usage, no propagación.
4. **Símbolos externos** — Niagara framework, npm deps, JDK classes (en `dependencies[]` de index.json).
5. **Runtime introspection** — sin ejecución de código, sin reflexión.
6. **AST-perfect parsing** — ripgrep + heurísticas, no Babel/TypeScript LSP/javaparser.
7. **Edición o refactor** — read-only sobre el source, write-only sobre `docs/mappings/`.
8. **UI / visualización de grafo** — fuera de scope. Output es JSON + Markdown.
9. **CI integration / pre-commit hooks** — fuera de scope, regeneración manual on-demand.

## 9. Risks (open)

1. **Falsos positivos en strings/comentarios HTML** — `<ComponentName` puede matchear
   `<!-- <ComponentName -->`. Mitigación: filtros de contexto rg en sub-agents.
2. **Falsos negativos por aliases** — aceptado en non-goals, marcado como `import-renamed`.
3. **Plugin `$niagara` overload** — 268 ocurrencias, mezcla REST + BOX + WS. Sub-agent debe
   discriminar por nombre de método consultando rest.js / box.js / websocket.js.
4. **Vue component splitting** — 378 símbolos en 5 shards requiere consistencia entre shards
   (mismo schema, mismos patrones). Riesgo de drift entre shards mitigado con prompt template
   común.
5. **Validación post-merge** — un shard inválido rompe el merge. Mitigación: validar shards
   individualmente antes de merge final.

## 10. Approval criteria

Este proposal queda lockeado cuando:

- Schema (sección 4) acordado sin cambios pendientes.
- Output location (sección 3) confirmado en `docs/mappings/reflow-clean-177/`.
- Pipeline (sección 5) acordado: 14 sub-agents sonnet, jq merge, ~1.5h wall-clock.
- Out of scope (sección 8) aceptado sin push-back.

Las fases siguientes (`sdd-spec` + `sdd-design`) pueden correr en paralelo: `sdd-spec`
formaliza requirements + acceptance criteria por kind, `sdd-design` detalla los rg patterns
por kind y el contrato del jq merge.
