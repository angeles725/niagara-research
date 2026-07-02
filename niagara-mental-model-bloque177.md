# Block 177 — Síntesis comparativa: chihuahua MX60 ↔ nmodsreflow, diferencias + análisis de brechas

> **Bloque de síntesis cross-focus** (no decompilado/lectura nueva): compara el módulo dashboard propio
> **chihuahua** (focus B163-B176, fuente primaria) contra el OEM de tercero **NiagaraMods Reflow** (focus
> B138-B162, decompilado + validado en vivo), dimensión por dimensión, y responde la pregunta del usuario:
> qué diferencias hay y qué le faltaría a chihuahua para "llegar a ser como Reflow". READ-ONLY consolidante.
>
> Focus: **chihuahua** — bloque de comparación (posterior a la documentación). Corpus language: Spanish.
> Fuente: los bloques de ambos focuses (cada afirmación re-cita el `[Block N]` que la verificó).
> Markers: `[CERT]` re-cita hallazgo ya verificado en su bloque fuente · `[INFER]` juicio comparativo.
> Capa 26. Consolida [Block 138]-[Block 162] (Reflow) × [Block 163]-[Block 176] (chihuahua).

---

## 177.1 — Naturaleza: producto comercial vs módulo a medida `[INFER]`

No son el mismo tipo de cosa, y eso enmarca todo lo demás:

- **Reflow** es un **producto comercial de tercero** (NiagaraMods, `com.niagaramods`), licenciado (RSA host-bound,
  [Block 139]), multi-sitio, distribuido como JAR (lo estudiamos por decompilado + validación viva). Su valor es
  **amplitud configurable**: un dashboard genérico que cualquier integrador arma sin programar.
- **chihuahua** es un **módulo propio a la medida** (`com.angeles.chihuahua`, [Block 163]) para UN sitio
  (Honeywell MX60, agua/bombeo), con fuente, SDD/openspec y lógica de control de dominio. Su valor es **foco,
  seguridad y profundidad de control**.

`[INFER]` "Ser como Reflow" para chihuahua no es cerrar defectos — es sumar **amplitud/genericidad** que hoy no
tiene (ni necesitaba). En varios ejes chihuahua ya está por delante.

## 177.2 — Matriz de comparación por dimensión

| Dimensión | Reflow | chihuahua | Lidera |
|---|---|---|---|
| Estructura módulo | tri-parte `-rt/-ux`, sin WB ([Block 138]) | tri-parte `-rt/-ux/-wb` ([Block 163]) | chihuahua (+WB) |
| **Auth de escritura** | **NINGÚN gate** — config-write a read-level (viva, [Block 160]) | **`checkCanWrite` fail-closed en cada endpoint** ([Block 164]) | **chihuahua (muy)** |
| **Audit trail** | inexistente; autor forjable ([Block 150] item 12) | ring `auditLog` + merge SecurityHistory ([Block 167]) | **chihuahua** |
| Frontend | **Vue 2.6.14 SPA** reactiva, transpilada ([Block 153]) | ES5 IIFE `window.MX60`, throttling a mano ([Block 170]) | Reflow (riqueza UI) |
| Sync de estado | **config.json compartido, JSON-Patch en vivo por WS multi-usuario** ([Block 143]) | estado en slots BComponent `.bog`, sin sync WS colaborativo ([Block 169]) | Reflow (colaboración) |
| Config end-user | floorplans, dashboardCards, buildings, navigation, temas, weather — **editables desde la UI** ([Block 154]) | vistas fijas a medida (HomeMap, cards, 3D) ([Block 170]) | Reflow (configurabilidad) |
| Datos en vivo | BajaScript vía reactividad Vue ([Block 153]) | BajaScript + fallback REST 5s a mano ([Block 170]) | empate (Reflow más ergonómico) |
| **Control de dominio** | dashboard genérico, sin lógica de control | **protecciones/latch/cascada HVAC, control-tick 10s** ([Block 168]) | **chihuahua** |
| Tooling de ingeniería | ninguno visible | **BatchLinkEditor WB + export/import links** ([Block 172]/[Block 173]) | **chihuahua** |
| Testabilidad | decompilado, sin tests | dispatch puro + `model/` puro WSL-testable ([Block 165]/[Block 176]) | chihuahua (aunque niagaraTest roto) |
| Licensing | RSA host-bound ([Block 139]) | ninguno (interno) | N/A |
| Superficie de ataque | ancha, mutable sin auth ([Block 150]) | acotada + gateada ([Block 164]) | **chihuahua** |

## 177.3 — Donde chihuahua ya LIDERA `[CERT]`

1. **Seguridad de escritura.** El eje más agudo: Reflow no gatea la mutación (probado EN VIVO — un usuario
   read-level reescribió su config, [Block 160]); chihuahua gatea cada POST con `checkCanWrite` fail-closed
   sobre el bit `OPERATOR_WRITE` ([Block 164]). chihuahua no tiene el defecto central de Reflow.
2. **Audit trail.** chihuahua registra cada escritura (ring + merge de logins nativos, [Block 167]); Reflow no
   tiene almacén de auditoría y su "autor" es forjable ([Block 150] item 12).
3. **Profundidad de control.** chihuahua ejecuta lógica de protección real (trip/latch permanente/cascada
   asimétrica, [Block 168]); Reflow es un visualizador, no controla equipo.
4. **Tooling propio.** Parte Workbench (`BatchLinkEditor`, commit atómico por-space, [Block 172]) + export/import
   idempotente de links ([Block 173]) — Reflow no tiene nada equivalente.
5. **Mantenibilidad.** Fuente propia, SDD/openspec, dispatch puro testeable, ADRs en el código. Reflow es una
   caja negra licenciada.

## 177.4 — Donde Reflow LIDERA `[CERT]`

1. **Framework frontend.** Vue 2.6.14 reactiva ([Block 153]) da UI rica con poco esfuerzo; chihuahua reimplementa
   a mano en ES5 (coalescing RAF, stores) lo que Vue haría solo ([Block 170] §170.6) — más código, más frágil.
2. **Colaboración en vivo.** El `config.json` compartido de Reflow se sincroniza en tiempo real entre TODOS los
   clientes por WS con deltas JSON-Patch ([Block 143]) — varios operadores ven cambios al instante. chihuahua no
   tiene sync colaborativo: el estado vive en slots y se refresca por request ([Block 169]/[Block 170]).
3. **Configurabilidad end-user.** Reflow deja armar floorplans, dashboardCards, navegación, buildings, temas y
   weather **desde la propia UI, sin programar** ([Block 154]); chihuahua tiene vistas fijas codificadas a medida.
4. **Amplitud de dashboard.** Reflow trae tipos de página y widgets genéricos + integraciones externas (weather/
   unsplash, [Block 149]); chihuahua cubre solo lo que su sitio necesita.
5. **Empaquetado como producto.** Reflow es reutilizable/licenciable multi-sitio; chihuahua es de un sitio.

## 177.5 — Qué le faltaría a chihuahua para "ser como Reflow" (roadmap de brechas) `[INFER]`

Ordenado por esfuerzo/impacto — todo esto es SUMAR amplitud, no arreglar defectos:

1. **Motor de config editable desde la UI** — hoy las vistas son fijas ([Block 170]). Igualar a Reflow exige un
   modelo de config declarativo (floorplans/cards/navegación como datos, no como código) + editores de UI. Es el
   cambio más grande: convierte a chihuahua de "módulo a medida" en "plataforma configurable".
2. **Sync colaborativo en vivo (WS + JSON-Patch)** — portar el patrón de [Block 143]: un `config` compartido
   difundido por WebSocket con deltas, para multi-operador en tiempo real. **PERO** con el gate RBAC de chihuahua
   ([Block 164]) puesto sobre el delta (justo lo que a Reflow le falta) — lo mejor de ambos.
3. **Adoptar un framework reactivo** (o un micro-reactivo propio) para no reimplementar throttling/stores a mano
   ([Block 170] §170.6). Tensión: rompe la regla ES5-sin-transpiler del proyecto ([Block 163]).
4. **Widgets/tipos de página genéricos** (dashboardCards, tabla configurable, más tipos de gráfico) para breadth.
5. **Integraciones externas opcionales** (weather, etc.) — con la salvedad de que la de Reflow fuga el HostID
   ([Block 149] item 11): copiar la feature, no el defecto.
6. **Empaquetado multi-sitio** si se quiere reutilizar: parametrizar el parque (hoy los `*_DATA` están hardcoded,
   [Block 169]) y separar lo específico del sitio.

## 177.6 — Veredicto `[INFER]`

chihuahua **no está "atrás" de Reflow** — es una herramienta distinta y, en los ejes que más importan para un BMS
en producción (**seguridad de escritura, auditoría, control de dominio, mantenibilidad**), está **por delante**.
Lo que Reflow tiene y chihuahua no es **amplitud configurable y colaboración en vivo**: un producto genérico que
el usuario final arma solo. "Llegar a ser como Reflow" significa invertir en esa genericidad (config editable +
sync WS + framework reactivo), idealmente **conservando el gate RBAC y el audit que Reflow no tiene**. La
pregunta de negocio previa es si esa amplitud vale el costo, dado que el foco a medida es hoy una fortaleza.

## 177.x — Connections

- **[Block 160]/[Block 164]** — el eje central: Reflow config-write sin auth (vivo) vs chihuahua gateado.
- **[Block 143]/[Block 169]** — sync colaborativo WS (Reflow) vs estado en slots (chihuahua): la brecha #2.
- **[Block 153]/[Block 170]** — Vue SPA vs ES5 IIFE: la brecha de framework #3.
- **[Block 154]/[Block 170]** — configurabilidad end-user (Reflow) vs vistas fijas (chihuahua): la brecha #1.
- **[Block 172]/[Block 167]/[Block 168]** — lo que chihuahua tiene de más: WB tool, audit, control de dominio.
- **Focus `chihuahua`** — este bloque cierra el objetivo del usuario (documentar → comparar → brechas).
