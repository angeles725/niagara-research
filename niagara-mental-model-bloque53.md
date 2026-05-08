# Bloque 53 — `app-readable.js` audit: SPA-Niagara bridge real en bundle producción Reflow 1.7.5

**Fecha**: 2026-05-07
**Método**: Audit estático del bundle producción `decompiled-rt/app-readable.js` (5.83 MB, 123,301 líneas) con triangulación grep multiline-aware + spot-check de regiones de código N≥3 antes de declarar findings. Aprendizaje aplicado del Bloque 51 #11 + Bloque 50 corrigendum 50.8 (sub-agents replican blind spots — orchestrator verifica con counts empíricos independientes).
**Fuentes primarias**:
- `/home/cristian/modules/Prototipos/Reflow/decompiled-rt/app-readable.js` (md5 `4dfbc3e3cb2ddca944110a0b77f36dfc`, build version baked = Reflow 1.7.5 RC1 build 43, compilado **Wed Jul 03 2024 17:18:24 GMT+0000**)
- Bloque 50 secciones 50.7 (TODOs originales) + 50.8 (corrigendum re-audit Tier 5)
- Bloque 51 sección #11 (lección triangulación)
- Bloque 52 (CSRF cross-frame — relacionado pero independiente)

**Versión analizada**: Reflow producción 1.7.5 RC1 build 43 (último release, ~22 meses sin update al 2026-05-07).

---

## 53.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit del bundle producción decompilado de Reflow para extraer el patrón **real** de bridge SPA↔Niagara: cómo se inicializa BajaScript desde el SPA, cómo se gestiona el lifecycle de subscribers, cómo se hace cleanup, cómo se enrutan hyperlinks cross-frame, y qué patrones son aprovechables para MX60.

Es un bloque **dual-purpose**:
1. **Refuta** mitos arrastrados de bloques 50/51 sobre el bundle producción (Phase D, bajaHeartbeat, axios stubs).
2. **Propone** patrones concretos para MX60 con tags KEEP / IMPROVE / SKIP — convirtiendo investigación pasiva en backlog de diseño activo.

### Qué corrige

| Bloque | Sección | Afirmación previa | Corrección |
|--------|---------|-------------------|------------|
| 50 | 50.8 (AP-3) | "~93% stubs no 100% — recuento empírico 70 stubs / 5 axios real en `rest.js`" | El recuento aplica al **código fuente** de desarrollo `rest.js`. En el **bundle producción** (`app-readable.js`) hay **0 axios.post**, **0 axios.get**, **0 fetch**, **0 XMLHttpRequest**, **0 jQuery**. Los `Promise.resolve` del bundle (10 hits) son **9× webpack async chunk loading** + **1× subscriber guard "already subscribed"**. AP-3 no aplica al bundle. |
| 50 | 50.7 (TODO-1, TODO-4, TODO-8) | "implementación real de `injectBaja`/`destroyApp`/Phase 5+ pendiente de auditar en `app-readable.js`" | Auditado. Documentado en este Bloque 53. **Phase 1-5+D NO existe en bundle** — era nomenclatura del audit fuente, no del runtime Reflow. **bajaHeartbeat NO existe en bundle** — Reflow producción usa lease nativo BajaScript, sin heartbeat custom. |
| 50 | 50.0 | "Phase D wiring `bajaHeartbeat.start(baja)` en `injectBaja` equivalent + `stop()` en `destroyApp`" (sección "Production gates Tier 2") | El wiring NUNCA existió en producción. `bajaHeartbeat.js` quedó como módulo de desarrollo no bundleado. Tarea de Phase D **se elimina del backlog**. |

### ¿Qué NO es este bloque?

- **NO** documenta cómo se construye el bundle (webpack config, chunk names) — solo el runtime resultante.
- **NO** cubre el módulo Java `nmodsreflow-rt/.jar` server-side — solo el frontend bundleado.
- **NO** redefine RBAC backend — solo el flow client-side de obtención de roles (Bloque 48 sigue siendo la fuente de verdad backend).
- **NO** documenta cada uno de los 59 callsites de `.subscribe(` ni cada uno de los 33 `.unsubscribe(` — solo el patrón mixin que los gobierna.

### Pregunta unificadora

> Estoy diseñando MX60 (greenfield Vue 3 sobre Niagara N4.14). ¿Cómo hizo Reflow producción el bridge SPA↔Niagara, qué partes copio, qué partes mejoro, qué tiro?

**Respuesta corta**: Copiá el patrón **wrapper singleton + Vue mixin + reference counting + debounced cleanup**. Mejorá el UUID (counter incremental → `crypto.randomUUID()`), descomponé el `injectBaja` monolítico en composables, manejá bfcache correctamente. Tirá `iView`, `regenerator-runtime`, el `setTimeout(...,1)` hack de sequencing, y el VueDevTools script-inject.

---

## 53.1 Triangulación metodológica — recuentos empíricos vs lectura

### 53.1.1 Lección directa del audit

Aplicando la lección dura del Bloque 51 #11 + corrigendum 50.8 (sub-agents replican blind spots cuando solo leen comments header, orchestrator verifica con `grep -c` independiente), arranqué este audit con counts empíricos antes de cualquier afirmación.

**Discrepancia metodológica detectada inmediatamente:**

```
.subscribe   (sin paréntesis):  195 hits
.subscribe(  (con paréntesis):   59 hits
                       ──────
                  136 falsos positivos
```

Los 136 hits restantes son **property references**, no callsites. Ejemplos:
- Vuex `store.subscribe` registrations
- `subscribed` adjective (e.g. `subscribedComponents`)
- Comments `// subscribe`
- Object keys (`{ subscribe: ... }` definitions)

**Regla metodológica para audits futuros**:

> **Para contar callsites de funciones, SIEMPRE incluir el paréntesis abriendo `(` en el grep pattern.** Sin paréntesis estás contando occurrencias de la cadena, no llamadas. La diferencia puede ser >3x — invalida el número.

### 53.1.2 Recuentos finales empíricos del audit

| Marker | Count | Comentario |
|--------|------:|-----------|
| `injectBaja` | 2 | 1 def línea 121406 + 1 caller línea 121554 (window.onload) |
| `destroyApp` | 1 | Definición única línea 121551, sin callers internos |
| `bajaHeartbeat` | 0 | NO existe en bundle producción |
| `Promise.resolve` | 10 | 9 webpack chunks + 1 subscriber guard |
| `axios.get` | 0 | Sin axios en bundle |
| `axios.post` | 0 | Sin axios en bundle |
| `fetch(` | 0 | Sin fetch nativo |
| `XMLHttpRequest` | 0 | Sin XHR raw |
| `jquery` (case-i) | 0 | Sin jQuery |
| `\$.ajax`, `\$.get`, `\$.post` | 0/0/0 | Sin jQuery AJAX |
| `.subscribe(` | 59 | Callsites reales |
| `.unsubscribe(` | 33 | Callsites reales |
| `beforeDestroy` | 77 | Vue lifecycle hooks |
| `lease: !0` | 5 | 2× ReflowService, 3× BatchResolve bulk |
| `new $baja.Subscriber` | 2 | 1 singleton wrapper + 1 caso aislado |
| `setInterval` | 20 | Para timers internos varios |
| `setTimeout` | 153 | Heavy use (debouncing, async sequencing) |
| `Phase 1-5/D` markers | 0 | Concepto NO existe en runtime Reflow |
| `csrf` (case-i) | 0 | CSRF maneja cross-frame fuera del bundle (Bloque 52) |
| `heartbeat` (case-i) | 0 | NO existe en bundle |

---

## 53.2 Mitos del backlog descartados

### 53.2.1 Mito: "Phase D requiere wiring `bajaHeartbeat.start(baja)`"

**Realidad**: Phase 1-5+D era nomenclatura del audit del **código fuente de desarrollo** (Bloque 50.4 originalmente). En el bundle producción **el concepto Phase no existe**. `bajaHeartbeat.js` es un módulo del repo `nmodsreflow-rt/src/rc/js/` que **NO se bundlea** en el output producción. Reflow producción usa el **lease nativo de BajaScript** (`Ord.make(...).get({lease: true})`) — el server detecta sesión muerta cuando el lease expira sin renew.

**Consecuencia**:
- Tarea "Phase D wiring" del Bloque 50 sección "Production gates Tier 2" → **ELIMINAR del backlog**.
- `bajaHeartbeat.js` queda como artefacto histórico del desarrollo — no llegó a runtime.

### 53.2.2 Mito: "AP-3 confirma ~93% stubs en runtime"

**Realidad**: AP-3 (corrigendum 50.8) detectó stubs en `rest.js` (código fuente). En el bundle producción decompilado:
- 0 axios calls totales
- 10 `Promise.resolve` totales (de los cuales 9 son `webpack.require.bind(...)` async chunk loading)
- TODA la HTTP a la Station va por `baja.Ord`, `baja.Subscriber`, `baja.BatchResolve`, `serverSideCall` — el cliente BajaScript la maneja internamente

`rest.js` con sus 70 stubs y 5 axios calls **es código de desarrollo paralelo** — los axios reales pueden corresponder a rutas custom del módulo (que tampoco terminan en este bundle frontend). El runtime SPA producción no tiene la dicotomía "stub vs real" — usa una sola HTTP path: BajaScript.

### 53.2.3 Mito: "destroyApp minimal es bug latente de leak"

**Realidad parcial**: Inicialmente al ver `window.destroyApp = function() { window.vueApp.$destroy() }` (línea 121551) sospeché bug — sin unsubscribe explícito. Pero al leer la sección 53.5 + 53.6 la arquitectura completa emerge:

1. `Vue.$destroy()` cascada → cada componente fires `beforeDestroy`
2. Componentes con mixin `Tt` ejecutan `$niagara.subscriber.unsubscribe(this.uuid)` automáticamente
3. Wrapper `me` decrementa reference count en su registry interno
4. Después de **250ms batched**, componentes sin observers se desuscriben del Subscriber raw

`destroyApp` minimal es **diseño limpio**, NO bug — IF la convención del mixin es respetada por todos los componentes. **Riesgo real**: si UN componente no usa el mixin y subscribe manualmente sin cleanup, leak silencioso. Es una **convención disciplinaria**, no enforcement.

---

## 53.3 `injectBaja` real (líneas 121406-121486)

### 53.3.1 Forma exterior

```js
window.injectBaja = Object(a["a"])(regeneratorRuntime.mark((function t() {
    var e, i, n = arguments;
    return regeneratorRuntime.wrap((function(t) { /* generator state machine */ }), t)
})));
```

**Análisis**: es una función async transpilada con `regenerator-runtime` (Babel-style). Equivale conceptualmente a:

```js
window.injectBaja = async function(workbench=false, widget=null) { /* ... */ }
```

### 53.3.2 Pasos del flujo (11 fases)

```js
async function injectBaja(workbench=false, widget=null) {
    // 1. Vue prototype injection (config flags)
    Vue.prototype.$workbench = workbench;
    Vue.prototype.$hasWidget = widget != null;

    // 2. Niagara connection config si workbench
    if (workbench) {
        window.require.config({
            config: { baja: { disableConnectionReuse: true } }
        });
    }

    // 3. Carga modules: BajaScript + 19 type plugins + webEditors views
    window.require([
        "baja!",
        "baja!bql:DynamicTimeRange,bql:DynamicTimeRangeType,alarm:AlarmRecord,control:Override,control:NumericOverride,control:EnumOverride,control:BooleanOverride,control:StringOverride,history:RootHistoryFolder,history:HistoryFolder,history:HistoryDevice,history:LocalDbHistory,history:HistoryMirror,history:HistorySpace,baja:UnitConversion,niagaraVirtual:NiagaraVirtualComponent,niagaraVirtual:NiagaraVirtualControlPoint,niagaraVirtual:NiagaraVirtualNumericWritable",
        "nmodule/webEditors/rc/servlets/views"
    ], async (baja, _, views) => {

        // 4. Get ReflowService (singleton) CON LEASE
        const service = await baja.Ord.make("service:nmodsreflow:ReflowService").get({ lease: true });

        // 5. Vue prototype: $baja + $bajaUsername
        Vue.prototype.$baja = baja;
        Vue.prototype.$bajaUsername = baja.getUserName();

        // 6. Roles via serverSideCall (BCx method invocation, NO REST)
        const roles = await service.serverSideCall({
            typeSpec: "nmodsreflow:ReflowUserCommands",
            methodName: "getRoles"
        });
        Vue.prototype.$bajaUserRoles = roles;
        Vue.prototype.$bajaViews = views;
        Vue.prototype.$widget = widget;

        // 7. Service como reactive (Vue.observable)
        Vue.prototype.$component = Vue.observable(service);

        // 8. Demo + Google Analytics flags
        const isDemo = service.get("demoMode") === true;
        const ga = service.get("ga");
        Vue.prototype.$isDemo = isDemo;

        // 9. Cross-frame routing (window.niagara.env.hyperlink)
        window.niagara = window.niagara || {};
        window.niagara.env = window.niagara.env || {};
        if (!window.niagara.env.hyperlink) {
            window.top.hyperlinkTargets = window.top.hyperlinkTargets || {};
            window.niagara.env.guid = "_root_";
            window.niagara.env.toHyperlink = (ord) => new Promise(resolve => {
                const n = ord.toString();
                resolve(n.charAt(0) === "/" ? n : baja.Ord.make(n).toUri());
            });
            window.niagara.env.hyperlink = (url, callback, target) => {
                const decoded = url ? decodeURI(url) : null;
                if (decoded && decoded.indexOf("|reflow:") > -1) {
                    // Reflow internal route → vueApp.$router.push
                    const path = decoded.split("|reflow:")[1];
                    const route = path ? unescape(path) : "/";
                    if (window.vueApp) window.vueApp.$router.push(route);
                    else window.location.href = "/nmodsreflow/#" + route;
                } else if (target && window.top.hyperlinkTargets[target]) {
                    // External target with fullScreen handling
                    return window.niagara.env.toHyperlink(url).then(resolved => {
                        let final = resolved;
                        if (/\/ord[/?]/.test(resolved)) {
                            final = final.replace("/ord?", "/ord/");
                            const fullScreenMatch = /\?(.+)$/.exec(final);
                            const hasFullScreen = fullScreenMatch && fullScreenMatch[1].indexOf("fullScreen=") !== -1;
                            if (hasFullScreen) return final.replace("fullScreen=false", "fullScreen=true");
                            final += "|view:?fullScreen=true";
                        }
                        if (typeof callback === "function") callback(final);
                        else window.top.hyperlinkTargets[target].location.assign(final);
                    });
                }
            };
        }

        // 10. Mount Vue (solo si !isConfig)
        if (!window.isConfig) {
            Vue.prototype.$documentElement = document;
            Vue.prototype.$isConfig = false;

            // VueDevTools dev hack (script tag inject hardcoded localhost:8098)
            if (ZQ /* dev flag */) {
                const dev = document.createElement("script");
                dev.type = "text/javascript";
                dev.src = "http://" + "" + ":8098";  // ← SIC: empty string concat, hardcoded
                window.__VUE_DEVTOOLS_HOST__ = "";
                window.__VUE_DEVTOOLS_PORT__ = 8098;
                document.getElementsByTagName("head")[0].appendChild(dev);
                s.a.connect("http://" + "", 8098);
            }

            // Google Analytics si demo + ga
            if (isDemo && ga) {
                Vue.use(VueAnalytics, { id: ga });
                console.log("Using Google Analytics [VueAnalytics] with ID: ", ga);
            }

            window.vueApp = new Vue({
                router: w_,
                store: La,
                iView: c.a,
                render: h => h(AS)  // root component
            });
            window.vueApp.$mount();

            // Vuex hydration
            window.vueApp.$store.commit("user/SET_USERNAME", baja.getUserName());
            window.vueApp.$store.commit("user/SET_ROLES", roles);
            window.vueApp.$store.commit("user/SET_IS_CONFIG", false);
            window.vueApp.$store.commit("demo/SET_IS_DEMO", isDemo);
            window.vueApp.$store.commit("SET_IS_MULTI_USER", service.getMultiUserConfig());
            window.vueApp.$store.commit("SET_SOCKET_TIMEOUT", 1000 * service.getSocketTimeout());
            if (JQ("headless")) window.vueApp.$store.commit("documentData/SET_VILLAIN_MODE", true);

            // 11. Initial data load (1ms delay hack — race condition mitigation)
            if (isDemo) {
                console.log("%c[Reflow] %cDemo Mode Active", "font-weight: bold; color: #2D8CF0", "font-weight: normal; color: #2D8CF0");
            } else {
                setTimeout(() => window.vueApp.$store.dispatch("load", service), 1);
            }
            document.getElementById("nmods-app").appendChild(window.vueApp.$el);
        }
    });
}
```

### 53.3.3 Observaciones críticas

1. **Single-shot lazy init**: `injectBaja` se llama UNA vez por carga de página (línea 121554). NO se invoca de nuevo. Si la sesión Niagara expira, el SPA NO reintenta — depende del lease y de errores en subsequent calls.

2. **Lease al obtener service singleton**: `baja.Ord.make("service:nmodsreflow:ReflowService").get({lease: true})`. El lease se renueva automáticamente por BajaScript mientras la pestaña está activa.

3. **Roles via `serverSideCall`** (NO REST): `service.serverSideCall({typeSpec: "nmodsreflow:ReflowUserCommands", methodName: "getRoles"})`. Esto es **BCx Java method invocation via BajaScript proxy**, type-safe, RBAC-checked en el server (validado en Bloque 48). MX60 debe heredar este patrón.

4. **`Vue.observable(service)`**: el ReflowService component se hace **reactive** — cualquier cambio en sus slots dispara re-render de componentes Vue que lo consumen via `this.$component`. Patrón limpio para singleton service reactive.

5. **Cross-frame hyperlink router**: Reflow soporta dos modos de navegación:
   - URL con `|reflow:<path>` → routing interno via `vueApp.$router.push`
   - URL sin marker → resolución a Ord + fullScreen handling + delegación a `window.top.hyperlinkTargets[target]`

   Esto permite que **un widget Reflow embebido en otro SPA Niagara** (Workbench iframe, Honeywell shell) navegue tanto interno como externo coherentemente.

6. **`isConfig` branch**: existe `injectConfig` paralela (líneas 121487-121550) — variante para "Config view" del módulo. Append a `#nmods-config`, render distinto (`iQ` no `AS`), sin iView constructor option, dispatch sincrónico (sin setTimeout 1ms). Build info baked en el branch Config (no en branch principal): version=1.7.5, RC1, build=43, time=Jul 03 2024.

7. **Hack VueDevTools**: el script-inject hardcodea `http://:8098` (literal con string vacío como host). Funciona solo si `ZQ` (dev flag) es truthy y solo si hay un VueDevTools server escuchando en localhost:8098. Es un dev artifact que llegó al bundle producción — innecesario, sospechoso, candidato a strip en MX60.

8. **`setTimeout(...,1)` hack** (línea 121469): `setTimeout(() => $store.dispatch("load", service), 1)` para diferir el dispatch. Es una **técnica de sequencing** que mete el dispatch al final del current tick — evita race con el `$mount()` en el mismo tick. Es un code smell — MX60 debería usar `await nextTick()` explícito o Promise chain.

---

## 53.4 `destroyApp`, `window.onload`, `pageshow` listener

### 53.4.1 `destroyApp` (línea 121551-121553)

```js
window.destroyApp = function() {
    window.vueApp.$destroy()
}
```

**1 línea de cleanup explícito.** Toda la limpieza de subscribers BajaScript se delega a:
- Vue lifecycle cascade (`$destroy()` propaga a children)
- `beforeDestroy` hooks de cada componente (77 hits totales en bundle)
- Mixin `Tt` que provee unsubscribe automático (sección 53.6)
- Wrapper `me` con reference counting + debounced 250ms unsubscribe (sección 53.5)

**Diseño limpio** IF mixin discipline. **Bug latente** si algún componente bypassea el mixin.

### 53.4.2 `window.onload` (líneas 121553-121554)

```js
window.onload = function() {
    (null == window.niagara.env || window.isConfig) && window.injectBaja()
}
```

`injectBaja()` se auto-invoca al `onload` SOLO si:
- `window.niagara.env` es `null`/`undefined` (caso normal: SPA standalone), O
- `window.isConfig` es truthy (modo Config view)

**Caveat**: si `niagara.env` ya existe (e.g. SPA embebido en widget Niagara host que pre-inyecta env), `injectBaja` NO se auto-llama — se espera invocación externa por el host.

### 53.4.3 `pageshow` listener — el hack contra bfcache (líneas 121555-121558)

```js
window.addEventListener("pageshow", function(t) {
    var persisted = t.persisted ||
                    (typeof window.performance !== "undefined" &&
                     window.performance.navigation.type === 2);
    if (persisted) window.location.reload(true);  // hard reload
});
```

**Decisión arquitectónica fea**: Reflow detecta back-forward cache restoration (browser navega "atrás" y restaura página de cache) y **fuerza reload completo**. Razón implícita: los subscribers BajaScript no sobreviven al freeze del bfcache, el SPA queda con state stale o conexiones rotas. En lugar de manejar el restore, lo aborta.

**Costo UX**: navegación back/forward = full reload. Pierde state de Vuex, scroll position, form data unsaved. Fricción visible para el usuario.

**Alternativa correcta para MX60**: detectar `pageshow.persisted`, evaluar conexión Niagara, si está rota **re-attach subscribers** sin reload. Si está OK, refresh state via store dispatch. UX más fluido, pero requiere infraestructura de re-subscribe que Reflow no tiene.

---

## 53.5 Subscriber wrapper `me` (líneas 3520-3650)

### 53.5.1 Forma exterior y registry interno

```js
var oe = null,           // singleton baja.Subscriber instance (lazy)
    re = [],             // active subscriptions: [{id, owner, callback, component}]
    ce = [],             // pending owners (subscribe in flight)
    ue = [],             // pending handles (subscribe in flight)
    de = [],             // pending unsubscribes (in-flight)
    he = false;          // debug flag

var me = {
    get $baja() { return Vue.prototype.$baja; },

    get subscriber() {
        if (oe === null) {
            oe = new this.$baja.Subscriber;
            var self = this;
            oe.attach("changed", function(e) {
                self._changed(this, e);  // fanout via re registry
            });
        }
        return oe;
    },

    subscribe: function(owner, components, callback) { /* ... */ },
    unsubscribe: function(owner, components, force) { /* ... */ },
    resolve: function(t) { /* batch ord resolution — cut off in this audit */ }
};
```

### 53.5.2 `subscribe(owner, components, callback)` — flujo detallado

```js
subscribe: function(owner, components, callback) {
    var self = this;

    // 1. Track pending state
    ce.push(owner);
    if (!Array.isArray(components)) components = [components];
    ue.push(components.map(c => c.$handle));

    // 2. Validate handles
    components.forEach(c => {
        if (!c || c.$handle === undefined || c.$handle === null)
            throw new Error("Cannot to subscribe to objects without handles");
    });

    // 3. REAL subscribe to baja.Subscriber singleton
    this.subscriber.subscribe(components).then(function() {

        // 4. Update registry
        components.forEach(c => {
            var existing = re.filter(r =>
                r.owner === owner && r.component.$handle === c.$handle
            );
            if (existing.length !== 0) {
                // Already subscribed by this owner → update callback only
                re = re.map(r => {
                    if (r.owner === owner && r.component.$handle === c.$handle) {
                        r.callback = callback;
                    }
                    return r;
                });
            } else {
                // New subscription
                re.push({
                    id: vt.generate(),  // unique ID per subscription
                    owner: owner,
                    callback: callback,
                    component: c
                });
            }
        });

        // 5. Cleanup pending tracking
        var idx = ce.indexOf(owner);
        if (idx !== -1) {
            ce.splice(idx, 1);
            ue.splice(idx, 1);

            // 6. Process pending unsubscribe-while-subscribing case
            var pendingUnsubIdx = -1;
            de.some((entry, i) => {
                if (entry.owner === owner) { pendingUnsubIdx = i; return true; }
                return false;
            });
            if (pendingUnsubIdx !== -1) {
                var pending = de.splice(pendingUnsubIdx, 1)[0];
                self.unsubscribe(pending.owner, pending.components);
            }
        }
    }).catch(err => { throw err; });
},
```

### 53.5.3 `unsubscribe(owner, components=null, force=false)` — flujo detallado

```js
unsubscribe: function(owner, components, force) {
    var self = this;
    components = components !== undefined ? components : null;
    force = force !== undefined ? force : false;

    // 1. If still pending subscribe OR force, queue for delayed unsubscribe
    if (ce.includes(owner) || force) {
        de.push({ owner: owner, components: components });
    }

    // 2. Remove from registry
    if (components === null) {
        // Unsubscribe ALL of owner's subscriptions
        re = re.filter(r => r.owner !== owner);
    } else {
        if (!Array.isArray(components)) components = [components];
        var handles = components.map(c => c.$handle);
        re = re.filter(r =>
            !(r.owner === owner && handles.includes(r.component.$handle))
        );
    }

    // 3. Debounced 250ms cleanup of orphan components
    setTimeout(function() {
        var orphans = [];
        components.forEach(c => {
            // Component is orphan if:
            //   - no remaining subscribers in re for this $handle
            //   - no pending subscribes for this $handle in ue
            var stillObserved = re.filter(r => r.component.$handle === c.$handle).length > 0;
            var pendingSubscribe = ue.filter(handles => handles.includes(c.$handle)).length > 0;
            if (!stillObserved && !pendingSubscribe) orphans.push(c);
        });

        if (orphans.length > 0) {
            self.subscriber.unsubscribe(orphans).catch(err => { throw err; });
        }
    }, 250);
}
```

### 53.5.4 Patrones arquitectónicos críticos

1. **Singleton baja.Subscriber compartido**: una sola instancia (`oe`) sirve a todos los componentes Vue. Ahorra recursos server-side (un solo channel BajaScript), pero exige el wrapper para multiplexar subscribers Vue.

2. **Reference counting via registry**: `re[]` mantiene `{id, owner, callback, component}` por subscription. Permite:
   - Múltiples owners observando el mismo component sin duplicar la suscripción real
   - Unsubscribe parcial por owner sin afectar otros

3. **Debounced unsubscribe (250ms)**: si un componente Vue se destruye y otro nuevo nace observando el mismo Niagara component, evita un round-trip subscribe/unsubscribe innecesario. **Optimización inteligente** — patrón directamente trasladable a MX60.

4. **Race handling (subscribe-while-pending)**: si llega `unsubscribe` antes de que el `subscribe` async termine, se encola en `de` y se procesa en el `.then()` del subscribe. Evita race conditions sutiles donde el unsubscribe se "perdería".

5. **`vt.generate()` para subscription IDs**: cada subscription tiene su propio ID único — distinto del `this.uuid` del Vue component (que es el "owner"). Permite identificar subscriptions individuales para debugging.

6. **Debug flag `he`**: 5 console.log gateados por `he`. En producción está `false`, en dev se puede flipear para tracing detallado. Patrón razonable.

### 53.5.5 `resolve(t)` — ord → component (líneas 3619-3665)

```js
resolve: function(t) {
    var self = this;
    return (async function() {
        var results, i, comp, single;

        if (!Array.isArray(t)) {
            // SINGLE ord branch — NO lease
            single = await self.$baja.Ord.make(t).get();
            return single;
        }

        // ARRAY branch — sequential resolution + lease per item
        results = [];
        i = 0;
        while (i < t.length) {
            try {
                comp = await self.$baja.Ord.make(t[i]).get();
                await comp.lease();              // ← LEASE per item
                results.push(comp);
            } catch (err) {
                results.push({ ord: t[i], error: err });  // placeholder on failure
            }
            i += 1;
        }
        return results;
    })();
}
```

**Asimetría crítica entre branches**:

| Branch | Lease | Timing | Use case |
|--------|-------|--------|----------|
| Array | ✅ Por cada componente | **Sequential** (await each) | Subscribed bulk resolves (mixin Tt) |
| Single | ❌ Sin lease | One-shot | Lookups puntuales |

**Performance gotcha**: el branch Array es **O(n) round-trips secuenciales**. Para 50 ords subscribed por un componente Vue → 50 await awaits seriales = ~50× latencia ord-to-component. El mixin `Tt` invoca este path en cada `mounted()`. Componentes con muchos ords pagan tiempo de carga lineal visible.

### 53.5.6 `resolveBetter(ord, callback)` + `resolveBatched()` — el path rápido subutilizado (líneas 3666-3700)

```js
// Variables de módulo:
//   se = pending resolve queue [{ord, callback}]
//   le = current debounce timer

resolveBetter: function(ord, callback) {
    clearTimeout(le);
    se.push({ ord: ord, callback: callback });
    le = setTimeout(() => this.resolveBatched(), 100);  // 100ms debounce
},

resolveBatched: function() {
    var self = this;
    return (async function() {
        var ords = se.map(t => t.ord);
        var callbacks = se.map(t => t.callback);
        var batch = new self.$baja.BatchResolve(ords);

        batch.resolve({ subscriber: self.subscriber })
            .then(() => {
                ords.forEach((ord, idx) => callbacks[idx](batch.get(idx)));
            })
            .catch(() => {
                // Partial success handling: try each individually
                ords.forEach((ord, idx) => {
                    var result;
                    try {
                        result = batch.get(idx);
                    } catch (e) {
                        callbacks[idx](null);
                    } finally {
                        if (result) callbacks[idx](result);
                    }
                });
            });

        se = [];
        clearTimeout(le);
        le = null;
    })();
}
```

**Ventajas sobre `resolve(array)`**:

- **Una sola llamada server-side** (`BatchResolve`) en lugar de N round-trips.
- **Debounce 100ms**: si múltiples componentes Vue solicitan ords cerca en el tiempo, se agregan en un batch. Reduce calls server.
- **Partial success**: si el batch falla parcialmente, intenta extraer los items que sí resolvieron.
- **Conecta con subscriber**: `batch.resolve({subscriber: this.subscriber})` — los componentes resueltos quedan ya enganchados al Subscriber singleton. Listos para `.subscribe()` directo.

**Anomalía**: el mixin `Tt` (sección 53.6) **NO usa este path**. Llama `resolve(subscribedOrds)` (path lento). El path rápido existe pero subutilizado en el bundle.

**Hipótesis**: el path lento podría ser legacy (versión antigua del wrapper) y `resolveBetter`/`resolveBatched` la versión "mejorada" que nunca completó la migración. Reforzaría la lectura de Bloque 51 sobre Reflow en migración WIP.

### 53.5.7 `_changed(component, prop)` — runtime fanout dispatcher (líneas 3686-3700)

```js
_changed: function(component, prop) {
    if (he) console.log("[subscriber][changed]",
        "prop: ", prop.getName(), prop,
        "component: ", component.getName(), component,
        "subscriptions: ", re.filter(sub => sub.component.$handle === component.$handle));

    re
        .filter(sub => sub.component.$handle === component.$handle)
        .forEach(sub => sub.callback(component, prop));
}
```

**Función**: invocada por el `attach("changed", ...)` del singleton Subscriber. Cada cambio en cualquier componente observado dispara este callback con `(component, prop)`.

**Algoritmo**:
1. Filtra `re[]` por `component.$handle` → todas las subscriptions interesadas en este componente
2. Para cada subscription matched, invoca `sub.callback(component, prop)`

**Observación crítica de performance**:

> **Linear scan O(n) por cada `changed` event.** Si Reflow tiene 100 subscriptions activas y 50 componentes cambian por segundo → 5,000 filtros lineales/seg. Aceptable para n=100, pero crece con la complejidad del SPA.

**MX60 implication**: indexar `re[]` por `$handle` en un `Map<handle, Subscription[]>` paralelo. Cada `_changed` event es O(1) lookup + iteración solo sobre las subscriptions de ese handle. Mantenimiento: insertar en ambas estructuras en `subscribe`, eliminar en `unsubscribe`.

### 53.5.8 API completa del wrapper `me` (cierre)

| Método | Tipo | Descripción | Auditado en |
|--------|------|-------------|-------------|
| `get $baja()` | getter | Acceso a `Vue.prototype.$baja` | 53.5.1 |
| `get subscriber()` | getter | Lazy-init singleton `oe` + attach `changed` → `_changed` | 53.5.1 |
| `subscribe(owner, comps, cb)` | async | Registra subscription en `re[]` + invoca `subscriber.subscribe()` | 53.5.2 |
| `unsubscribe(owner, comps, force)` | sync | Decrementa `re[]` + debounced 250ms cleanup | 53.5.3 |
| `resolve(t)` | async | Array (sequential + lease) o single (no lease) | 53.5.5 |
| `resolveBetter(ord, cb)` | async | Encola en `se[]`, debounce 100ms | 53.5.6 |
| `resolveBatched()` | async | Ejecuta `BatchResolve` sobre `se[]` queue | 53.5.6 |
| `_changed(comp, prop)` | sync | Dispatcher: filtra `re[]` por `$handle`, invoca callbacks | 53.5.7 |

**8 entradas totales**. Wrapper compacto y bien delimitado.

### 53.5.9 `$niagara` namespace — helper library completa

El subscriber wrapper `me` es solo UN sub-namespace del `$niagara` global. La estructura completa del namespace (líneas ~118160-118185):

```js
var aQ = {
    get $baja() { return Vue.prototype.$baja; },
    encode: niagaraEncode,    // helpers de encoding
    decode: niagaraDecode,
    uncamel: uncamel,
    ord: pe,                  // ver 53.5.15
    alarm: Na,                // alarm helpers
    bql: sa,                  // BQL helpers
    history: Sa,              // history helpers
    schedule: la,             // schedule helpers
    nav: Ci,                  // navigation helpers
    matrix: vi,               // matrix view helpers
    backups: Se,              // backup operations
    points: Ti,               // point helpers
    subscriber: me,           // ← lo auditado en 53.5
    util: {
        timerange: ka,
        facets: nQ
    }
};

Vue.prototype.$niagara = aQ;
```

Este `aQ` se inyecta como `Vue.prototype.$niagara` (en algún lugar no auditado todavía — probablemente en un Vue plugin install). Cada componente Vue puede acceder a `this.$niagara.ord.clean()`, `this.$niagara.alarm.<...>`, `this.$niagara.subscriber.subscribe()`, etc.

**14 sub-namespaces** — esto es una helper library propietaria de Reflow montada sobre BajaScript. **Cada uno** es candidato a auditar para MX60: muchos de los patrones probablemente son trasladables.

## 53.5.11 `$niagara.bql` (`sa`) — BQL query helpers (líneas 11414-11510)

**Hallazgo metodológico previo**: el grep inicial reportó **0 callsites de `$niagara.bql`** (con escape correcto: 1 callsite). Pero `sa` (la implementación interna) se invoca:
- `sa.query`: 1 callsite directo (`la = schedule.list`)
- `sa.remoteQuery`: 0 callsites directos (pero invocado vía `$niagara.bql.remoteQuery` 1 vez en search component)
- `sa.resolveQuery`: 0 callsites externos (usado solo internamente por `sa.query`)

**Total: 2 callsites en TODO el bundle de 123,301 líneas.** El namespace existe pero está casi sin uso. La mayoría de queries Niagara en Reflow van por **otro path** que descubrí inmediatamente después y documento en sección 53.5.11.

#### API completa de `sa`

```js
sa = {
    get $baja() { return Vue.prototype.$baja; },

    // Path 1: WebSocket-style via yi.spec.BQL (sección 53.5.11)
    remoteQuery: function(opts = {}) {
        var params = Object.assign({
            validateTypes: null,
            limit: 25,
            page: 1
        }, opts);
        return params.query
            ? yi.json(yi.spec.BQL, "query", params)  // ← serverSideCall a ReflowBQLCommands
            : [];
    },

    // Path 2: Ord-based BQL canonical
    query: async function(bqlOrd, resolveFlag = true, pageSize = 25) {
        var totalCount = 0;

        // Step 1: COUNT(*) probe via regex replace
        var countQuery = bqlOrd.replace(/(.*select )(.*)( from .*)/, "$1COUNT(*)$3");
        try {
            var countComp = await this.$baja.Ord.make(countQuery).get();
            await countComp.cursor({
                each: function() {
                    totalCount = this.get().get(niagaraEncode("COUNT(toString)"));
                }
            });
        } catch (err) {
            console.log("BQL Error", err);  // ← swallow
        }

        // Step 2: paginated cursor over result set
        var rows = [];
        if (totalCount != null && totalCount > 0) {
            var dataComp = await this.$baja.Ord.make(bqlOrd).get();
            var offset = 0;
            while (offset <= totalCount) {  // ← off-by-one (<=)
                await dataComp.cursor({
                    offset: offset,
                    limit: pageSize,
                    each: function() { rows.push(this.get()); }
                });
                offset += pageSize;
            }
        }

        // Step 3: optional resolve to leased reactive components
        if (!resolveFlag) return rows;
        return await this.resolveQuery(rows);
    },

    // Helper: convert BQL rows to leased reactive components
    resolveQuery: async function(rows) {
        var ords = rows.map(row => "station:|" + row.get("slotPath"));
        var batch = new this.$baja.BatchResolve(ords);
        var components = [];
        await batch.resolve({
            lease: true,  // ← TODOS los rows leased
            each: function() {
                components.push(Vue.observable(this));  // ← reactive wrap
            }
        });
        return components;
    }
};
```

#### Bugs y code smells en `sa.query`

1. **Regex COUNT(*) probe FRÁGIL**: `/(.*select )(.*)( from .*)/` greedy + case-sensitive + single-line.
   - Falla con subqueries (`(select ...) from`).
   - Falla con `SELECT` mayúsculas.
   - Falla con BQL multi-línea.
   - Si falla, `totalCount` queda `0` por silent error → no se itera ningún row.

2. **Pagination loop off-by-one**: `while (offset <= totalCount)` con `<=` genera UNA iteración extra cuando `totalCount % pageSize === 0`. La cursor podría retornar 0 rows (OK) o duplicar rows (depende de implementación BajaScript).

3. **No early exit**: si la cursor devuelve menos rows que `pageSize`, igual avanza `offset += pageSize` y reintenta. Wasted round-trips.

4. **Silent error swallow**: `console.log("BQL Error", err)` — no throw, no callback de error, no return code. La UI no sabe que falló — interpreta "0 results" como "no hay" en lugar de "error de query".

5. **`resolveQuery` lease ALL rows**: para una BQL que devuelve 10,000 rows, eso es 10K leases simultáneos. Memory + server load issue.

#### Patrón canonical confirmed: BQL via ord URL

```
station:|slot:/|bql:select * from baja:Component where name = 'pump1'
```

Esto es **BajaScript canónico**, no invención de Reflow. La query es **parte del ord**. `Ord.make(ord).get()` resuelve el query string incluido. **MX60 hereda este pattern obligatoriamente** — N4 no soporta otro modelo desde el cliente para queries arbitrarias.

---

## 53.5.12 `yi` — RPC wrapper sobre `serverSideCall` (líneas 5089-5180+)

### 53.5.12.1 Lo que parecía WebSocket es en realidad serverSideCall

Hallazgo principal del audit BQL: el namespace local `yi` (que veía en `yi.json(yi.spec.BQL, "query", ...)`) **NO es un WebSocket RPC layer**. Es un **wrapper alrededor de `$component.serverSideCall(...)`** del singleton ReflowService.

#### Definición de `yi`

```js
yi = {
    get $component() {
        return Vue.prototype.$component;  // ReflowService observable singleton
    },

    spec: {
        NAV:     "nmodsreflow:ReflowNavCommands",
        FILE:    "nmodsreflow:ReflowFileCommands",
        CSV:     "nmodsreflow:ReflowCSVCommands",
        HISTORY: "nmodsreflow:ReflowHistoryCommands",
        ALARM:   "nmodsreflow:ReflowAlarmCommands",
        USER:    "nmodsreflow:ReflowUserCommands",
        BQL:     "nmodsreflow:ReflowBQLCommands"
    },

    valueFromObject: function(obj) {
        // Convierte JS object → BComponent dinámico con un slot por key
        var component = new this.$baja.Component;
        Object.keys(obj).forEach(key => {
            if (obj[key] != null) {
                component.add({
                    slot: key,
                    value: this.wrappedValue(obj[key])
                });
            }
        });
        return component;
    },

    wrappedValue: function(t) {
        switch (typeof t) {
            case "string":
            case "number":
            case "boolean":
                return t;
            case "object":
                if (Array.isArray(t)) return t.join(",");  // ← arrays → CSV string
                if (t === null) return null;
                return this.valueFromObject(t);  // recursive
            default:
                return null;  // ← swallow default
        }
    },

    call: async function(typeSpec, methodName, value=null) {
        try {
            var wrapped = this.wrappedValue(value);
            var result = await this.$component.serverSideCall({
                typeSpec: typeSpec,
                methodName: methodName,
                value: wrapped || undefined
            });
            return result;
        } catch (err) {
            console.error("RPC Error: Server Side Call", typeSpec, methodName, value, err);
            return null;  // ← swallow → null
        }
    },

    string: async function(typeSpec, methodName, value=null) {
        var raw = await this.call(typeSpec, methodName, value);
        return String(raw);
    },

    json: async function(typeSpec, methodName, value=null) {
        var str = await this.string(typeSpec, methodName, value);
        // ... JSON.parse(str) — read truncated, asumido por el naming
    }
};
```

### 53.5.12.2 Mapping a clases Java backend

Cada `yi.spec.X` apunta a una clase Java `B<X>Commands` en el módulo `nmodsreflow-rt`. Las 7 typeSpecs:

| Spec | Java class (probable) | Methods invocados (vistos en grep) |
|------|----------------------|-------------------------------------|
| NAV | `BReflowNavCommands` | `getNavChildren` |
| FILE | `BReflowFileCommands` | `listFiles` |
| CSV | `BReflowCSVCommands` | (no vistos en este audit) |
| HISTORY | `BReflowHistoryCommands` | `getDeviceTree`, `getDevices`, `makeHistories`, `getData` |
| ALARM | `BReflowAlarmCommands` | `getClasses`, `querySources`, `getUuidsForSources`, `canAcknowledgeAlarms` (Bloque 48) |
| USER | `BReflowUserCommands` | `getRoles` (Bloque 48 + injectBaja sección 53.3), `getAllRoles` (TODO 48-5) |
| BQL | `BReflowBQLCommands` | `query` |

**Conexión cruzada con bloques previos**:
- Bloque 48 (`canAcknowledgeAlarms` BBoolean) → es invocado vía `yi.call(yi.spec.ALARM, "canAcknowledgeAlarms")`.
- Bloque 48 (`getRoles` ReflowUserCommands en injectBaja sección 53.3) → mismo pattern.
- TODO 48-5 (`getAllRoles` info disclosure RESUELTO) → vive aquí.

### 53.5.12.3 Patrón completo confirmed: serverSideCall + BComponent params

```
yi.json(yi.spec.BQL, "query", { query: "select * from ...", limit: 50 })
   ↓ (yi.json → yi.string → yi.call)
yi.wrappedValue({...}) → new BComponent + slots {query, limit}
   ↓
$component.serverSideCall({typeSpec: "nmodsreflow:ReflowBQLCommands", methodName: "query", value: BComponent})
   ↓ (BajaScript proxy → server)
BReflowBQLCommands.query(BComponent) → ejecuta BQL real → retorna BComponent o BString
   ↓ (server → cliente)
yi.string() → String(result)
yi.json() → JSON.parse(string)
```

**CONFIRMA empíricamente**: la HTTP de Reflow al backend es **TODA via `serverSideCall`** (BajaScript proxy). No hay REST endpoints custom desde el SPA — todo va por:
1. `Ord.make(...).get()` → resuelve componentes (Bloque 53.3)
2. `serverSideCall(...)` → invoca métodos sobre clases Java backend (Bloque 53.5.11)
3. BQL via ord URL (`station:|slot:/|bql:select ...`) → para queries arbitrarias

**NO HAY** REST/fetch/axios/XHR. Todo BajaScript canónico.

### 53.5.12.4 Code smells en `yi`

1. **Silent error → null** (`yi.call` catch): UI no detecta fallos de RPC. Si server crashea, retorna null que la UI puede confundir con "no data".

2. **`wrappedValue` array → CSV string**: pierde información para arrays de objetos complejos (cae al `default: return null`). Si quisieras pasar `[{a:1}, {b:2}]` falla silenciosamente.

3. **`wrappedValue` recursive object**: cada nivel del object se hace BComponent con slots. Para objetos profundos = BComponents anidados = overhead. No hay límite de recursión = stack overflow potencial.

4. **`null` value handling inconsistente**: `if (obj[key] != null)` skip nulls, pero el caller de `yi.call` puede pasar `value=null` y el wrapping produce `value: undefined` en serverSideCall. Asimetría.

5. **`yi.string` siempre `String(raw)`**: si raw es `null` (caso error), `String(null) === "null"`. Después `yi.json` haría `JSON.parse("null") === null`. Funciona accidentalmente, pero es frágil.

---

## 53.5.13 BQL injection vulnerability — search component (línea 39440-39466)

### 53.5.13.1 Donde vive el bug

Hay un componente Vue (probablemente "Search" / "Component finder") que arma una BQL query dinámicamente con input del usuario y la ejecuta vía `$niagara.bql.remoteQuery`. Schema simplificado:

```js
async function search(opts) {
    // opts.text/n/name → name search
    // opts.s/slot/path/slotpath → slotPath search
    // opts.p/parent → parent search
    // opts.d/display → displayName search
    // opts.only/o/type → type filter

    // Encoder helper (parcial — se aplica solo a name/slot/parent)
    var v = (t) => t.split("/")
        .map(s => encode(encode(decode(s))).toLowerCase())
        .join("/");

    var clauses = ["1=1"];

    // Name → encoded
    var names = (opts.text || []).concat(opts.n || []).concat(opts.name || []);
    if (names.length > 0) {
        clauses.push(names.map(v).map(t =>
            "name.toLowerCase LIKE '%" + t + "%'"
        ).join(" OR "));
    }

    // Slot → encoded
    var slots = (opts.s || []).concat(opts.slot || []).concat(opts.path || []).concat(opts.slotpath || []);
    if (slots.length > 0) {
        clauses.push(slots.map(v).map(t =>
            "slotPath.toString.toLowerCase LIKE '%" + t + "%'"
        ).join(" OR "));
    }

    // Parent → encoded
    var parents = (opts.p || []).concat(opts.parent || []);
    if (parents.length > 0) {
        clauses.push(parents.map(v).map(t =>
            "parent.name.toLowerCase LIKE '%" + t + "%'"
        ).join(" OR "));
    }

    // ⚠️ DisplayName → SOLO toLowerCase, SIN encoder v
    var displays = (opts.d || []).concat(opts.display || []);
    if (displays.length > 0) {
        clauses.push(displays.map(t =>
            "displayName.toLowerCase LIKE '%" + t.toLowerCase() + "%'"
        ).join(" OR "));
    }

    // Type filter — SIN encoder
    var types = "baja:Component";
    if (opts.types && opts.types.length > 0) {
        types = opts.types.join(",");
    }

    var where = "(" + clauses.join(") AND (") + ")";
    var bqlOrd = "station:|slot:/|bql:select * from " + types + " where " + where;

    return await $niagara.bql.remoteQuery({
        query: bqlOrd,
        validateTypes: this.internalTypeFilter,
        limit: opts.limit || 50,
        page: opts.page || 1
    });
}
```

### 53.5.13.2 La vulnerabilidad concreta

**Branch displayName (`opts.d`/`opts.display`)** SOLO aplica `.toLowerCase()`. Las otras tres (name/slot/parent) sí pasan por el encoder `v` que hace `encode(encode(decode(t)))` — defensa indirecta contra inyección porque el carácter `'` se encodea a `%27`.

**Test mental**: si un usuario ingresa el siguiente displayName en el search box:

```
') OR 1=1 OR ('1
```

→ `.toLowerCase()` → `') or 1=1 or ('1`
→ Concatenado en la cláusula: `displayName.toLowerCase LIKE '%') or 1=1 or ('1%'`
→ La `clauses.join(") AND (")` envuelve en paréntesis adicionales.
→ BQL final contains: `... AND (displayName.toLowerCase LIKE '%') or 1=1 or ('1%') AND ...`

**Resultado**: la cláusula se rompe, `or 1=1` matchea TODO, retorna todos los componentes — incluyendo aquellos que el usuario no debería ver (gating sería por ACLs server-side, pero la query hace bypass de filtros UX).

### 53.5.13.3 Severidad y blast radius — REFINADO post-audit Java

**Severidad inicial declarada**: MEDIUM-HIGH (audit cliente solo).

**Severidad confirmada post-audit Java side** (sección 53.6 nueva): **LOW-MEDIUM**.

**Razón del refinamiento**: el audit del lado Java (`BReflowBQLCommands.query()` línea 92) confirma que la query se ejecuta como `ord.get(cx)` con el `Context cx` del usuario llamador. Niagara aplica **RBAC nativo slot-por-slot** en la resolución del ord — el atacante NO puede ver componentes restringidos por ACL aunque inyecte BQL.

**Blast radius real**:
- ❌ NO permite info disclosure de componentes restringidos (RBAC nativo Niagara los filtra automáticamente).
- ❌ NO permite writes ni invocaciones (es solo SELECT BQL).
- ✅ SÍ permite **enumeración cruda** de componentes que el atacante ya tiene permiso de leer — bypass de filtros UX que la SPA aplicaría client-side.
- ✅ SÍ permite **manipular `validateTypes`** en el filter — atacante podría especificar typespecs custom que el desarrollador no quiso exponer en el dropdown UI (pero igual filtrados por RBAC).

**Conclusión**: AP-21 es **UX bypass**, no escalación de privilegios. Sigue siendo bug a fix, pero sin pánico — el RBAC nativo es el verdadero defensor.

**Caveat residual**: si MX60 implementa Commands custom **sin** delegar a `ord.get(cx)` (por ejemplo, ejecutando BQL con un Context elevado o sistema), AP-21 escalaría a HIGH. Disciplina obligatoria: **TODO ord lookup en métodos Commands DEBE usar el `cx` recibido como parámetro, NUNCA un Context construido ad-hoc**.

### 53.5.13.4 Por qué el bug existe

**Asimetría descuidada**: el encoder `v` parece haber sido pensado para paths Niagara (porque hace split por `/`). Por eso se aplica a name, slot, parent — todos pueden ser paths. Pero displayName **NO ES un path** — es un string libre. El programador quiso preservar el string crudo para que el LIKE haga match natural, y se olvidó de escapar.

### 53.5.13.5 Cómo lo hace MX60

**MX60 implication directa**: implementar un **BQL builder con escape obligatorio**:

```js
// Pseudo-API objetivo
const bql = new BqlBuilder()
    .from("baja:Component")
    .where("displayName.toLowerCase", "LIKE", `%${displayName}%`)  // builder escapa internamente
    .orderBy("name")
    .limit(50)
    .build();

await niagara.bql.query(bql);  // builder produce ord seguro
```

Reglas:
1. **NUNCA concatenar strings de usuario en BQL crudo.** El builder escapa `'` → `''` (BQL standard) automáticamente.
2. **Validar typespec** en una whitelist conocida — no aceptar `opts.types` arbitrario.
3. **Limit obligatorio** server-side (defense in depth) — no confiar solo en `opts.limit` cliente.
4. **Server-side audit log** de queries grandes / paginadas profundas para detectar enumeration.

---

## 53.5.14 Asignación de antipatterns nuevos

Tres antipatterns identificados en este audit, propuestos como continuación de la numeración Bloque 51 AP-13..AP-20:

| AP # | Patrón | Severidad | Sección |
|------|--------|-----------|---------|
| **AP-21** | BQL injection en displayName branch del search component | ~~MEDIUM-HIGH~~ → **LOW-MEDIUM** (refinado post-Java audit, RBAC cubre) | 53.5.13 |
| **AP-22** | `sa.query` regex COUNT(*) probe + pagination off-by-one + silent error swallow | **MEDIUM** | 53.5.11 |
| **AP-23** | `yi.call` silent error → `null` returns (UI no detecta fallos RPC) | **LOW-MEDIUM** | 53.5.12.4 |
| **AP-24** | `BReflowBQLCommands.query` server-side pagination ITERA TODA LA CURSOR (skip+take in-memory en lugar de cursor.offset/limit nativo) | **MEDIUM-HIGH** (DoS amplification con queries sin where) | 53.5.16.4 |
| **AP-25** | `Math.ceil(int/int)` integer division — pageCount underreports (siempre floor) | **LOW** | 53.5.16.4 |
| **AP-26** | Magic acceptance del arg via `toString()` fallback — defeated typing | **LOW-MEDIUM** | 53.5.16.4 |

Estos antipatterns son **MX60 backlog "no heredar"** — están en el lado código fuente Reflow legacy y MX60 debe diseñar el equivalente sin replicarlos.

---

## 53.5.15 `$niagara.ord` (`pe`) — ord manipulation helpers (líneas 3705-...)

> **Nota orden**: este sub-namespace fue auditado ANTES que `bql` (53.5.11), `yi` (53.5.12), y la BQL injection (53.5.13). Se mantiene aquí para preservar el flujo conceptual del audit final — `pe.clean()` lo usa el helper `v` del search component (sección 53.5.13.1) para encoding defensivo de paths.

Métodos auditados:

| Método | Función |
|--------|---------|
| `clean(t)` | Strip `local:|`, `foxs:|`, trailing `/` |
| `cleanCompare(t)` | Strip más prefixes (`history:|`, `station:|`, `file:` paths) + normalize image-library |
| `parent(t)` | Get parent ord (split by `/`, drop last) |
| `resolveRelative(t, e)` | Resolver `../` paths con base ord |
| `relativize(t, e)` | Hacer ord relativo respecto de base |
| `isRelativeTo(t, e)` | Boolean: ¿`e` está dentro de `t`? |
| `absolute(t, e)` | Hacer ord absoluto (joining paths) |
| `image(t)` | Convertir ord a image URL: `module://` → `/module/`, `file:` → `/ord/` |
| `sound(t)` | Idéntico a `image` (mismo conversion) |
| `has(t, e)` | (parcial — read truncado) check si path exists en componente |

**Pattern observado**: separar URL conventions Niagara (`local:|station:|`, `module://`, `file:`) de URL conventions web (`/module/`, `/ord/`). Cada hostname/scheme Niagara mapea a un path web.

**MX60 implication**: este helper completo es **trasladable directo**. Las URL conventions de N4.14 son las mismas que en N4.12 que en Reflow 1.7.5 — cambia muy poco.

---

## 53.5.16 Cross-reference Java side — `BReflowBQLCommands.query()` audit

### 53.5.16.1 Forma exterior de la clase

**Archivo**: `/home/cristian/modules/Prototipos/Reflow-Clean-177/nmodsreflow/nmodsreflow-rt/src/com/niagaramods/nmodsreflow/commands/BReflowBQLCommands.java` (120 líneas, 4.7 KB).

```java
@NiagaraType(agent={
    @AgentOn(types={"nmodsreflow:ReflowService"}, requiredPermissions="r")
})
public class BReflowBQLCommands
extends BComponent
implements BIServerSideCallHandler {

    private static final int DEFAULT_QUERY_LIMIT = 25;

    public BValue query(BComponent caller, BValue arg, Context cx) throws Exception {
        // ... ver 53.5.16.2
    }
}
```

**Anotaciones críticas**:

| Decoración | Significado |
|------------|-------------|
| `@AgentOn(types={"nmodsreflow:ReflowService"}, ...)` | Esta clase se attachea como **agent** al `BReflowService`. Cuando el cliente invoca `serverSideCall({typeSpec: "nmodsreflow:ReflowBQLCommands", ...})`, BajaScript routea a esta clase porque es agent del service del cliente. |
| `requiredPermissions="r"` | El usuario que invoca **debe tener READ permission** al `ReflowService`. Niagara enforce esto antes de invocar el método. |
| `extends BComponent` | Es un BComponent — sigue las reglas Niagara (slots, lifecycle, ACLs por slot). |
| `implements BIServerSideCallHandler` | Marker interface que declara "yo soy invocable desde BajaScript proxy via `serverSideCall`". |

### 53.5.16.2 Análisis del método `query`

```java
public BValue query(BComponent caller, BValue arg, Context cx) throws Exception {
    BObject obj;
    BOrd ord = null;
    String[] validateTypes = null;
    NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
    int limit = DEFAULT_QUERY_LIMIT;
    int page = 1;

    // ── BRANCH 1: arg es ya un BOrd ──
    if (arg.getType().is(BOrd.TYPE)) {
        ord = (BOrd) arg;
    }
    // ── BRANCH 2: arg es BComponent (caso normal: yi.valueFromObject) ──
    else if (arg.getType().equals(BComponent.TYPE)) {
        BComponent comps = (BComponent) arg;
        if (comps.get("query") != null) {
            ord = BOrd.make(comps.get("query").toString());  // ← USER STRING TO BOrd
            if (comps.get("validateTypes") != null) {
                validateTypes = comps.get("validateTypes").toString().split(",");
            }
            if (comps.get("page") != null) {
                page = format.parse(comps.get("page").toString()).intValue();
            }
            if (comps.get("limit") != null) {
                limit = format.parse(comps.get("limit").toString()).intValue();
            }
        }
    }
    // ── BRANCH 3: arg es cualquier otra cosa, toString() y treat como ord URL ──
    else {
        ord = BOrd.make(arg.toString());
    }

    // ── EJECUCIÓN ──
    if (ord != null && ord != BOrd.NULL && (obj = ord.get(cx)).getType().is(BITable.TYPE)) {
        BITable table = (BITable) obj;
        TableCursor c = table.cursor();
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(BINavNode.class, new NavNodeSerializer(validateTypes));
        mapper.registerModule(module);
        ObjectNode response = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        int count = 0;

        // ── PAGINATION LOOP ── ⚠️ ITERA TODA LA CURSOR
        while (c.next()) {
            BObject row;
            if (count < page * limit
                && count >= (page - 1) * limit
                && (row = (BObject) c.get()).getType().is(BINavNode.TYPE)) {
                BINavNode navNode = (BINavNode) row;
                Object node = mapper.valueToTree(navNode);
                results.add((JsonNode) node);
            }
            ++count;
        }

        response.set("items", results);
        response.put("limit", limit);
        response.put("page", page);
        response.put("pageCount", (int) Math.ceil(count / limit));  // ⚠️ INTEGER DIVISION
        response.put("total", count);
        return BString.make(response.toString());
    }

    return BString.make("[]");
}
```

### 53.5.16.3 Hallazgo clave — RBAC funciona, AP-21 es UX bypass

Línea 92: `(obj = ord.get(cx)).getType()...`

**El método usa `cx` (el Context recibido como parámetro) para resolver el ord.** Eso significa que Niagara aplica RBAC nativo automáticamente:
- Slot pruning (Bloque 22.1140 + 35.478): el server omite slots sin OPERATOR_READ del Component que llega al cliente.
- BSpace.canRead/canWrite/canInvoke evaluado contra el user del Context.
- HIDDEN flag filtering (Bloque 48).

**Confirmación**: el `cx` aquí es el caller's authenticated Context, propagado desde el BajaScript proxy. Si el atacante con BQL injection trata de leer un Component que NO tiene permiso, el resultado de `ord.get(cx)` lo excluirá automáticamente.

**Refinamiento de severidad AP-21**: bajada de **MEDIUM-HIGH** → **LOW-MEDIUM** (sección 53.5.13.3 actualizada).

**Caveat para MX60 (DISCIPLINA OBLIGATORIA)**:

> **TODO método Commands DEBE usar el `cx` recibido como parámetro para CUALQUIER ord/component lookup.**

Si MX60 implementa un Commands con `ord.get(null)` o un Context construido ad-hoc (`Sys.makeContext(...)`), el RBAC se rompe y AP-21-equivalent escala a info disclosure HIGH. Esta es **la lección operacional más importante** del audit Java.

### 53.5.16.4 Bugs server-side adicionales descubiertos

**AP-24 (NUEVO)** — **Pagination ITERA TODA LA CURSOR**:

```java
while (c.next()) {
    if (count < page * limit && count >= (page - 1) * limit && ...) {
        results.add(...);  // skip + take manual
    }
    ++count;
}
```

La cursor itera **TODOS los rows** del result set para determinar `total` y filtrar la página solicitada. Para una BQL que retorna 100,000 rows con `page=1, limit=25`, el server itera 100K rows en memoria ANTES de devolver 25.

**Severidad**: **MEDIUM-HIGH** (DoS amplification — un cliente con `query=select * from baja:Component` sin where clause puede tumbar el server). N4.14 BQL cursor SÍ soporta `offset`/`limit` nativos — este código NO los usa.

**AP-25 (NUEVO)** — **`Math.ceil(int/int)` integer division bug**:

```java
response.put("pageCount", (int) Math.ceil(count / limit));
```

Java integer division: `count / limit` ya es int (truncado). `Math.ceil` de un int siempre es ese int (no hace ceiling después de la pérdida de precisión).

**Caso patológico**: 26 results con limit=25 → `26/25 = 1` (integer) → `Math.ceil(1) = 1` → cliente cree que hay 1 page (debería ser 2).

**Fix correcto**: `(count + limit - 1) / limit` o `(int) Math.ceil((double) count / limit)`.

**Severidad**: **LOW** — bug funcional, no de seguridad. Cliente paginará incorrectamente.

**AP-26 (NUEVO)** — **Magic acceptance del arg via `toString()`**:

Branch 3 del método (líneas 89-91):
```java
} else {
    ord = BOrd.make(arg.toString());
}
```

Si el cliente manda CUALQUIER tipo no-BOrd, no-BComponent (e.g., un BString, BInteger, BList), el método hace `toString()` y trata el resultado como ord URL. Es **defeated typing** — pierde la información de tipo del arg, asume que es serializable a un ord válido.

**Risk concreto**: si un atacante manda un BList con `[BString.make("station:|slot:/|bql:..."), ...]`, el `toString()` del BList podría dar algo como `"[station:|slot:/|bql:...]"` que SÍ se parsearía como ord (con prefix garbage). Comportamiento undefined.

**Mejor**: rechazar explícitamente todo arg que no sea BOrd o BComponent con `query` slot. Throw exception. **Failsafe defaults**.

### 53.5.16.5 Pattern para MX60 — template Java Commands

Extrayendo el patrón canónico (estilo de Reflow + correcciones):

```java
@NiagaraType(agent={
    @AgentOn(types={"<module>:<MainService>"}, requiredPermissions="r")
})
public class BMx60<X>Commands
extends BComponent
implements BIServerSideCallHandler {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 1000;  // ← server-side cap defense-in-depth

    public BValue <method>(BComponent caller, BValue arg, Context cx) throws Exception {
        // 1. Validate arg STRICTLY (no toString fallback)
        if (!(arg.getType().equals(BComponent.TYPE))) {
            throw new IllegalArgumentException("Expected BComponent params, got " + arg.getType());
        }

        BComponent params = (BComponent) arg;

        // 2. Extract typed params with defaults + validation
        int limit = Math.min(getIntSlot(params, "limit", DEFAULT_LIMIT), MAX_LIMIT);
        int page  = Math.max(getIntSlot(params, "page", 1), 1);

        // 3. Build query SAFELY — no string concat with user input
        BqlBuilder bql = new BqlBuilder()
            .from(getValidatedType(params))   // whitelist check
            .where(buildSafeWhereClause(params))  // builder escapes user input
            .limit(limit)
            .offset((page - 1) * limit);
        BOrd queryOrd = bql.toOrd();

        // 4. Execute con cx — RBAC delegado
        BObject obj = queryOrd.get(cx);
        if (!obj.getType().is(BITable.TYPE)) return BString.make("{\"items\":[]}");

        // 5. Cursor con offset/limit nativos (N4.14 supported)
        BITable table = (BITable) obj;
        TableCursor c = table.cursor();
        c.offset(bql.getOffset());
        c.limit(bql.getLimit());

        // 6. Iterate y construir response
        ArrayNode results = ...;  // Jackson
        while (c.next()) {
            BObject row = (BObject) c.get();
            // serialize via custom serializer with validation
            results.add(...);
        }

        // 7. PageCount correcto
        int totalCount = (int) table.size();  // BITable.size() if available
        int pageCount = (totalCount + limit - 1) / limit;

        // 8. Response estructurada (no JSON string blob — usar BComponent slots)
        BComponent response = new BComponent();
        response.add("items", BString.make(results.toString()));
        response.add("page", BInteger.make(page));
        response.add("limit", BInteger.make(limit));
        response.add("pageCount", BInteger.make(pageCount));
        response.add("total", BInteger.make(totalCount));
        return response;
    }
}
```

**Reglas obligatorias** (decisión arquitectónica MX60):

1. **`cx` propagation**: TODO ord lookup usa el `cx` recibido. **NUNCA `Sys.makeContext` ad-hoc**.
2. **Strict typing del arg**: rechazar arg no esperado con exception explícita. NO `toString()` fallback.
3. **Cap de limit server-side**: `MAX_LIMIT` constante. Cliente no puede pedir 100K results.
4. **BqlBuilder server-side**: queries armadas con builder que escapa user input. NUNCA `BOrd.make(userString)`.
5. **Cursor offset/limit nativos**: usar las APIs de TableCursor, no skip+take manual.
6. **PageCount correcto**: aritmética integer ceiling `(n + d - 1) / d` o doble cast.
7. **Response BComponent estructurada**: slots tipados, no JSON string blob.
8. **Validate types whitelist**: `validateTypes` no llega del cliente — viene de constantes server-side o config validada.

### 53.5.16.6 Detalles secundarios del audit Java

- **Slot-o-Matic auto-generated TYPE region**: convención Niagara estándar (Bloque 35). El comment "Generated Sun Mar 15 18:02:51 CST 2026" es del re-compile local — el .class fue compilado de un source más viejo (consistente con bundle Reflow 1.7.5 Jul 2024).
- **CFR 0.152 decompiled**: header indica que esta versión es decompilación de bytecode. El source original probablemente está perdido. Los comentarios (si había) están borrados.
- **`format.parse(...).intValue()`**: parsea page/limit con `NumberFormat.getInstance(Locale.getDefault())`. Si el cliente manda un string no-numérico, throws `ParseException` → sube por `throws Exception` → `yi.call` lo agarra y devuelve null. Manejo silencioso, mensaje genérico.
- **NavNodeSerializer custom**: serializer Jackson con `validateTypes` (whitelist). Sin auditar en este pase — probable filter sobre type spec strings.
- **`BString.make(response.toString())`**: el método retorna BString con el JSON serializado. El cliente (`yi.string` → `yi.json`) parsea. Es un JSON-string-en-BString — overhead doble (Jackson serialize + Niagara wrap + cliente unwrap + JSON.parse). MX60 → BComponent estructurada nativa.

### 53.5.16.7 Tabla síntesis — Reflow Java vs MX60 Java

| Aspecto | Reflow `BReflowBQLCommands.query` | MX60 `BMx60BqlCommands.query` |
|---------|-----------------------------------|-------------------------------|
| `@NiagaraType` agent | ✅ correcto | KEEP |
| `requiredPermissions="r"` | ✅ correcto | KEEP |
| `BIServerSideCallHandler` | ✅ correcto | KEEP |
| `cx` propagation a `ord.get(cx)` | ✅ correcto (post-fix Reflow-Clean-177) | KEEP — disciplina obligatoria |
| Triple-branch arg acceptance | ⚠️ AP-26 magic | IMPROVE → strict typing |
| BQL string from user | 🚨 AP-21 vector | IMPROVE → BqlBuilder |
| Pagination skip+take | 🚨 AP-24 O(n) | IMPROVE → cursor offset/limit |
| `Math.ceil(int/int)` | 🚨 AP-25 bug | IMPROVE → `(n+d-1)/d` |
| Response como JSON string | ⚠️ overhead | IMPROVE → BComponent slots |
| `validateTypes` desde cliente | ⚠️ user-controlled | IMPROVE → whitelist server-side |
| Custom NavNodeSerializer | ✅ patrón válido | KEEP |
| Limit cap server-side | ❌ ausente | NEW → MAX_LIMIT constant |
| Error responses estructurados | ❌ ausente (`BString.make("[]")`) | NEW → response.error slot |

---

## 53.6 Vue mixin pattern (líneas 2880-3010)

### 53.6.1 El mixin `Tt` y su forma

Reflow define un mixin Vue que cada componente que necesita observar Niagara components hereda. Se usa con `mixins: [Tt]` en componentes consumidores.

**Schema del mixin** (extracto):

```js
var Tt = {
    data: function() {
        return {
            subscribedComponents: [],
            subscribedComponentActions: {},
            subscriberLoading: false,
            subscriberCallbacks: false,
            subscribeToActions: false  // configurable per-component
        };
    },

    methods: {
        subscribe: function() {
            var self = this;
            return regeneratorRuntime.wrap(/* async */ () => {
                if (/* condición de skip */) return;

                self.subscriberLoading = true;

                // 1. Resolve ords to components
                var components = await self.$niagara.subscriber.resolve(self.subscribedOrds);
                self.$set(self, "subscribedComponents", components);

                // 2. Optionally parse actions for each
                if (self.subscribeToActions) {
                    components.forEach(c => self.parseActions(c));
                }

                // 3. Subscribe via wrapper
                self.$niagara.subscriber.subscribe(
                    self.uuid,
                    self.subscribedComponents,
                    self.updateSubscribedComponents
                );

                self.subscriberLoading = false;
                self.subscriberLoaded();  // empty hook for override
            });
        },

        updateSubscribedComponents: function(updatedComp, prevState) {
            // In-place update: replace the matching component by $handle
            this.$set(this, "subscribedComponents",
                this.subscribedComponents.map(c =>
                    c.$handle === updatedComp.$handle ? updatedComp : c
                )
            );

            // Re-parse actions ONLY if it wasn't a value-only change
            if (this.subscribeToActions &&
                (prevState === null ||
                 (prevState.$slotName &&
                  !prevState.$slotName.startsWith("out") &&
                  !prevState.$slotName.startsWith("in") &&
                  prevState.$slotName !== "overrideExpiration"))) {
                this.parseActions(updatedComp);
            }

            if (this.subscriberCallbacks) {
                this.subscriberUpdated(updatedComp, prevState);  // empty hook
            }
        },

        parseActions: async function(comp) {
            var displayNames = comp.get("displayNames");

            // RBAC-aware filter: HIDDEN flag excludes action from UI
            var actions = comp.getSlots()
                .actions()
                .filter(a => (comp.getFlags(a) & this.$baja.Flags.HIDDEN) === 0)
                .toArray();

            var parsed = [];
            await Promise.all(actions.map(async (action) => {
                var displayName = action.$displayName;
                if (displayNames) displayName = displayNames.get(action.getName()) || displayName;

                var defaultValue = await comp.getActionParameterDefault({
                    slot: action.getName()
                });

                parsed.push({
                    name: action.getName(),
                    displayName: displayName,
                    param: defaultValue !== null ? defaultValue.getType().toString() : null,
                    default: defaultValue
                });
            }));

            this.$set(this.subscribedComponentActions, comp.$handle, parsed);
        },

        // Empty hooks (template method pattern)
        subscriberLoaded: function() {},
        subscriberUpdated: function() {},

        // Convenience accessors
        subscribedPoint: function(t) {
            if (!t) return null;
            var ord = t.ord || t;
            return this.subscribedComponents.find(c =>
                c.getNavOrd && this.$niagara.ord.clean(c.getNavOrd()) === this.$niagara.ord.clean(ord)
            );
        },
        subscribedValue: function(t) { /* ... resolve + .get("out").getValue() */ },
        subscribedValueDisplay: function(t) { /* ... .getValueDisplay() */ },
        subscribedStatus: function(t) { /* ... .getStatus() */ },
        subscribedActions: function(t) { /* ... return cached parsed actions */ },
        subscribedFacets: function(t) { /* ... .getFacets() */ }
    },

    mounted: function() {
        var self = this;
        this.$nextTick(function() {
            self.subscribe();
        });
    },

    beforeDestroy: function() {
        this.$niagara.subscriber.unsubscribe(this.uuid);
    }
};
```

### 53.6.2 Observaciones sobre el mixin

1. **`mounted` → `$nextTick(subscribe)`**: difiere el subscribe a después de que el DOM y los children components estén listos. Patrón limpio.

2. **`beforeDestroy` → unsubscribe automático**: una sola línea, pero **es la pieza que justifica el `destroyApp` minimal** (sección 53.4.1).

3. **`parseActions` filtra `Flags.HIDDEN`**: enforcement RBAC visual — acciones marcadas HIDDEN por permission categories Niagara se ocultan automáticamente de la UI Reflow. **Patrón obligatorio para MX60**.

4. **`updateSubscribedComponents` slot-aware**: re-parse de actions solo si cambió la **definición** del componente (slots que NO sean `out*`, `in*`, `overrideExpiration`). Si solo cambió el **valor** de un punto, NO re-parsea (caro). Optimización significativa.

5. **In-place update por `$handle`**: NO replace de array completo (caro para Vue reactivity). Map con replace selectivo del item que matchea por handle.

6. **Template method hooks** (`subscriberLoaded`, `subscriberUpdated`): vacíos por defecto, los componentes consumidores los overridean si necesitan reaccionar. Patrón anticuado para Vue 2; en Vue 3 → composables con composition API.

7. **`subscribedOrds`**: el componente tiene que **definir** la propiedad `subscribedOrds` (data o computed) para que el mixin la consuma. Convención implícita — no documentada en el mixin, depende de disciplina.

---

## 53.7 UUID generation, lease usage, build version

### 53.7.1 UUID — `QQ++` counter (línea 121361)

```js
this.uuid = QQ.toString(); QQ += 1;
```

**`QQ` es un global module-scoped counter.** Cada componente Vue instanciado obtiene un UUID = string del counter actual, luego incrementa.

Implicaciones:
- ❌ **NO es UUID real** — es solo un entero secuencial.
- ❌ **No globalmente único** — dos pestañas distintas del SPA tienen contadores independientes que producen los mismos valores.
- ❌ **Reset al reload** — pierde correlación con server-side state si el server registra estos IDs.
- ✅ Suficiente para multiplexar subscribers dentro de UNA pestaña (que es el scope del wrapper `me`).

Para MX60: **migrar a `crypto.randomUUID()`** (estándar web, 36 chars, colisión negligible) si el ID se loggea, exporta, o cruza pestañas. Si solo es scope-local, el counter es funcionalmente OK pero `crypto.randomUUID()` no cuesta nada más.

### 53.7.2 Lease usage — los 5 callsites

```js
// 1-2. ReflowService singleton get (líneas 121424, 121511)
baja.Ord.make("service:nmodsreflow:ReflowService").get({ lease: !0 });

// 3-5. BatchResolve bulk fetch (líneas 11507, 18128, 19139)
new $baja.BatchResolve(ords).resolve({ lease: !0 });
```

**Patrón consistente**: lease para componentes que el SPA necesita mantener vivos durante la sesión. BajaScript renueva el lease automáticamente mientras hay actividad. Sin lease, el server-side podría liberar el component reference después de un timeout.

**MX60 implication**: heredar tal cual. `lease: true` en singleton service + bulk resolves.

### 53.7.3 Build version baked

Encontrado en `injectConfig` (líneas 121521-121526):

```js
Vue.prototype.$build = {
    mode: "production",
    rc: "RC1",
    number: "43",
    time: "Wed Jul 03 2024 17:18:24 GMT+0000 (Coordinated Universal Time)",
    version: "1.7.5"
};
```

**Reflow producción es 1.7.5 RC1 build 43, compilado el 3 de julio de 2024**. Al 2026-05-07 son ~22 meses sin update. Confirmación empírica del staleness señalado al inicio del audit.

Curiosamente este `$build` se inyecta SOLO en `injectConfig` (modo Config view), no en `injectBaja` (modo SPA principal). Inconsistencia menor — la SPA principal no expone su build version a componentes via `$build`.

---

## 53.8 Síntesis MX60 implications — KEEP / IMPROVE / SKIP

| # | Patrón Reflow | Tag | Razón / Cómo trasladarlo a MX60 |
|---|---|---|---|
| 1 | Vue.prototype injection (`$baja`, `$component`, `$bajaUserRoles`, `$bajaUsername`, `$bajaViews`) | **KEEP** | Patrón idiomático Vue 2. En Vue 3 → `app.provide()` + `inject()` o globalProperties. Misma idea, API moderna. |
| 2 | `serverSideCall` para RBAC y ops backend (no REST) | **KEEP** | Type-safe (typeSpec + methodName), RBAC-checked en el server (Bloque 48), no necesita CSRF separado. |
| 3 | `Ord.make(...).get({lease: true})` para singleton service | **KEEP** | Lease nativo BajaScript renueva automáticamente. Sin lease = riesgo de stale ref. |
| 4 | `Vue.observable(service)` — service component reactive | **KEEP** | En Vue 3 → `reactive(service)` o `shallowReactive` según depth required. |
| 5 | Singleton `baja.Subscriber` + wrapper con registry `re[]` | **KEEP** | Diseño correcto. Un solo channel server-side, multiplex client-side. |
| 6 | Reference counting + debounced 250ms unsubscribe | **KEEP** | Optimización real. Trasladar literal — el 250ms se puede exponer como config. |
| 7 | Race handling: pending-unsubscribe-during-subscribe queue (`de[]`) | **KEEP** | Defensa contra race sutil. Replicar el patrón. |
| 8 | Vue mixin auto-subscribe (`mounted`) / auto-unsubscribe (`beforeDestroy`) | **KEEP**, MIGRADO | En Vue 3 → composable `useSubscribedOrds(ords, callback)` con `onMounted` + `onBeforeUnmount`. Misma semántica, mejor reutilización. |
| 9 | `parseActions` filtra `Flags.HIDDEN` (RBAC-aware UI) | **KEEP** | Obligatorio. Sin esto, MX60 muestra acciones que el usuario no puede ejecutar → bugs UX + leak de info. |
| 10 | In-place update de `subscribedComponents` por `$handle` | **KEEP** | Performance Vue reactivity. Replicar. |
| 11 | Slot-aware re-parse guard (skip re-parse si solo cambió valor) | **KEEP** | Ahorra trabajo caro (`getActionParameterDefault` round-trip). Replicar. |
| 12 | `BatchResolve` con `lease: true` para bulk fetch | **KEEP** | Eficiente para inicialización masiva. Replicar. |
| 13 | UUID = global counter `QQ++` | **IMPROVE** | `crypto.randomUUID()`. Cero costo, ganamos uniqueness cross-tab y debugging. |
| 14 | `injectBaja` monolítico de 80+ líneas | **IMPROVE** | Descomponer en composables: `useBajaClient()`, `useReflowService()`, `useUserRoles()`, `useHyperlinkRouter()`, `mountApp()`. Testeable individualmente. |
| 15 | `setTimeout(...,1)` para sequencing dispatch | **IMPROVE** | Code smell. `await nextTick()` o Promise chain explícita. |
| 16 | `pageshow` + `persisted` → forced reload | **IMPROVE** | Implementar bfcache restore real: detectar persisted, evaluar conexión Niagara, re-attach subscribers, re-fetch state. UX mucho mejor. |
| 17 | `destroyApp = Vue.$destroy()` minimal (delegado a mixin discipline) | **IMPROVE** | Agregar safety net: registry global de subscribers manuales, drain emergency en destroyApp. Defensa contra el caso "alguien no usa el mixin". |
| 18 | `subscriberLoaded` / `subscriberUpdated` template hooks vacíos | **IMPROVE** | Composition API → composable con events explícitos: `useSubscribedOrds(...).onLoaded(cb)` / `.onUpdated(cb)`. |
| 19 | `subscribedOrds` convención implícita | **IMPROVE** | Composable recibe `ords` como argumento explícito. Evita convención no documentada. |
| 20 | Cross-frame `niagara.env.hyperlink` con `|reflow:` marker | **KEEP** + **IMPROVE** | Patrón válido. Reemplazar `|reflow:` por `|mx60:` o equivalente. Mantener la diferenciación interno/externo. |
| 21 | VueDevTools script-inject hardcoded | **SKIP** | Vue 3 dev tools nativos en browser, sin hacks. |
| 22 | `regenerator-runtime` async transpile | **SKIP** | Native `async/await` en todos los browsers modernos (target ES2020+). |
| 23 | `iView` UI library | **SKIP** | Vue 2 only, unmaintained. MX60 → Element Plus / Naive UI / Ant Design Vue 4 / Vuetify 3 / o headless (Radix/Headless UI). |
| 24 | Build info baked SOLO en `injectConfig` (no en main SPA) | **IMPROVE** | MX60: build info en ambos paths, expuesto consistente via `app.config.globalProperties.$build`. |
| 25 | `bajaHeartbeat` custom heartbeat | **SKIP** | Lease nativo BajaScript es suficiente — el módulo desarrollo nunca llegó a producción por buena razón. |
| 26 | "Phase 1-5/D" lifecycle naming | **SKIP** | No existe en runtime real, era nomenclatura de audit fuente. |
| 27 | `resolve(array)` sequential await + lease per item | **IMPROVE** | Performance bug subutilizado: el mixin `Tt` invoca este path lento en lugar del `resolveBatched` rápido. MX60 → unificar en una sola API que SIEMPRE batche internamente. |
| 28 | `resolveBetter` + `resolveBatched` BatchResolve con debounce 100ms | **KEEP** | El path correcto. Trasladar la idea: queue + debounce + `BatchResolve.resolve({subscriber})`. |
| 29 | `_changed(comp, prop)` linear scan O(n) por event | **IMPROVE** | Para n grande es O(n) por event. MX60 → indexar por `$handle` con `Map<handle, Subscription[]>` paralelo a `re[]`. Lookup O(1). |
| 30 | `$niagara` namespace 14 sub-libs (ord, alarm, bql, history, schedule, nav, matrix, backups, points, subscriber, util.*) | **KEEP** (pattern) + **AUDIT PENDING** (cada sub-lib) | Patrón "helper library propietaria sobre BajaScript" es excelente. Cada sub-lib individual (alarm/bql/history/etc) requiere audit propio para decidir KEEP/IMPROVE/SKIP por método. |
| 31 | `$niagara.ord.clean/cleanCompare/parent/resolveRelative/relativize/absolute/image/sound` | **KEEP** | URL conventions Niagara → web. Trasladable directo a MX60 sin cambios significativos (N4.14 = N4.12 = 1.7.5 en este aspecto). |
| 32 | `resolve(single)` SIN lease vs `resolve(array)` CON lease — asimetría implícita | **IMPROVE** | API inconsistente. MX60 → lease como parámetro explícito (`resolve(t, {lease: true/false})`), default explícito documentado. |
| 33 | `yi` RPC wrapper sobre `$component.serverSideCall` (`call`/`string`/`json`) | **KEEP** | Patrón excelente: encapsula serverSideCall + type marshalling + parsing de response. MX60 → composable `useReflowCommands()` o similar con la misma trinity. |
| 34 | Naming convention typeSpecs: `<module>:<Module>Commands` (NAV/FILE/CSV/HISTORY/ALARM/USER/BQL) | **KEEP** | Organización limpia. MX60 → `mx60:Mx60<X>Commands` por capa de funcionalidad. |
| 35 | `valueFromObject` JS object → BComponent dinámico con slots | **KEEP** | Magic glue para parámetros estructurados a métodos Java. MX60 hereda este pattern (no hay alternativa cleaner en BajaScript). |
| 36 | BQL via ord URL `station:|slot:/|bql:select * from ... where ...` | **KEEP** | Canonical BajaScript, no es invento de Reflow. MX60 forzado a usarlo. |
| 37 | BQL string concatenation directa (search component línea 39461) | **IMPROVE** | **AP-21 vulnerability**. MX60 → BQL builder con escape obligatorio (`'` → `''`), validation de typespecs en whitelist, limit server-side defense-in-depth. |
| 38 | `yi.call` silent error → `null` return + console.error solo | **IMPROVE** | UI no detecta fallos RPC. MX60 → propagar error o exponer estado (loading/error/data triple). |
| 39 | `sa.query` regex COUNT + pagination off-by-one + silent error | **IMPROVE** | 3 bugs concurrentes. MX60 → no usar regex probe (BajaScript cursor expone `total`); pagination con while early-exit; errors propagados. |
| 40 | `wrappedValue` array → join(",") + objects complejos → null | **IMPROVE** | Lossy para arrays de objetos. MX60 → JSON serialization explícita o BList/BVector estructurado. |
| 41 | `@AgentOn(types=..., requiredPermissions="r")` Java decoration | **KEEP** | Pattern correcto Niagara. Permite RBAC enforcement automático antes de invocar el método. MX60 → adoptar literal con permission level adecuado por command. |
| 42 | `BIServerSideCallHandler` interface + `query(BComponent caller, BValue arg, Context cx)` signature | **KEEP** | Convención Niagara estándar. MX60 → mismo signature exacto, no inventar custom interfaces. |
| 43 | `cx` propagation a TODO `ord.get(cx)` server-side (RBAC delegation) | **KEEP** + **disciplina obligatoria** | Pieza más crítica. MX60 → policy: ningún Commands method puede usar `ord.get(null)` ni `Sys.makeContext`. ESLint-equivalent o pre-commit Java rule. |
| 44 | Triple-branch arg acceptance (BOrd / BComponent / `toString()` fallback) | **IMPROVE** | AP-26. MX60 → strict typing: throw IllegalArgumentException si arg no es BComponent con slots esperados. |
| 45 | Pagination skip+take iterando toda la cursor | **IMPROVE** | AP-24. MX60 → `c.offset(n)` + `c.limit(m)` nativos N4.14. Cap server-side `MAX_LIMIT`. |
| 46 | `Math.ceil(int/int)` integer division | **IMPROVE** | AP-25. MX60 → `(count + limit - 1) / limit` o `Math.ceil((double) count / limit)`. |
| 47 | Response como `BString.make(jsonString)` (JSON serialized to BString) | **IMPROVE** | Overhead doble. MX60 → `BComponent` estructurado con slots tipados, deserialización nativa cliente. |
| 48 | `validateTypes` desde cliente (string array)  | **IMPROVE** | User-controlled types pueden bypassar UI filters. MX60 → whitelist server-side de typespecs permitidos. |
| 49 | `BString.make("[]")` en error case | **IMPROVE** | Cliente no distingue "0 results" de "error". MX60 → response.error slot con código + message. |
| 50 | `MAX_LIMIT` cap server-side ausente | **NEW** | Patrón nuevo en MX60: constante por command, defense-in-depth contra cliente que pide 100K rows. |
| 51 | `BqlBuilder` server-side con escape automático | **NEW** | Patrón nuevo en MX60: builder que produce ords seguros, NUNCA `BOrd.make(userString)` directamente. Cubre AP-21 + AP-26 + sanitization general. |

### Resumen agregado (actualizado post-Java audit)

- **22 KEEP** (patrones a heredar literal): Vue prototype injection, serverSideCall, lease, observable service, singleton subscriber + wrapper, ref counting, debounced unsubscribe (250ms subs + 100ms resolve), race handling, mixin lifecycle (migrado a composable), HIDDEN flag filter, in-place updates, slot-aware re-parse, BatchResolve, `$niagara.ord` URL helpers, `$niagara` namespace pattern, `yi` RPC wrapper trinity, typeSpec naming convention, `valueFromObject` BComponent params, BQL via ord URL, **`@AgentOn` decoration**, **`BIServerSideCallHandler` interface**, **`cx` propagation discipline**.
- **22 IMPROVE** (heredar el qué, mejorar el cómo): UUID, injectBaja descomposición, sequencing sin setTimeout hack, bfcache real, destroyApp safety net, hooks → composables explícitos, ords arg explícito, cross-frame router con marker propio, build info consistente, `resolve` siempre batched, `_changed` indexed lookup, lease como param explícito, BQL builder con escape (cliente), `yi` errors propagated, `sa.query` bugs eliminados, `wrappedValue` no-lossy serialization, **strict arg typing (vs AP-26)**, **cursor offset/limit nativo (vs AP-24)**, **pageCount math correcto (vs AP-25)**, **response BComponent estructurada**, **validateTypes whitelist server**, **error response estructurado**.
- **2 NEW** (MX60-only patterns no presentes en Reflow): **`MAX_LIMIT` server-side cap**, **`BqlBuilder` con escape**.
- **5 SKIP** (no replicar): VueDevTools hack, regenerator-runtime, iView, bajaHeartbeat custom, "Phase" nomenclature.

**51 entries totales** — backlog de diseño concreto y accionable para MX60 desde día 1, ahora con cobertura **client + server + Java decorations**.

---

## 53.9 Estado de TODOs Bloque 50 actualizado

| TODO Bloque 50 | Estado pre-Bloque 53 | Estado post-Bloque 53 |
|---|---|---|
| TODO-1 (NEEDS_RUNTIME) | NEEDS_RUNTIME | NEEDS_RUNTIME (no afectado) |
| TODO-4 (`app-readable.js` 5.8MB) | PENDIENTE | ✅ **RESUELTO** — auditado en este bloque |
| TODO-8 (`app-readable.js` injectBaja real) | PENDIENTE | ✅ **RESUELTO** — sección 53.3 |
| Phase D wiring (sección 50.0 Production gates Tier 2) | PENDIENTE | ❌ **ELIMINADO** — el wiring nunca existió en runtime, mito descartado |
| AP-3 stubs runtime % | OUTDATED-DRIFTED ~93% | ⚠️ **REFORMULADO** — AP-3 NO aplica al bundle producción (sección 53.2.2) |

---

## 53.10 Lección metodológica capitalizada

Este bloque consolida **dos lecciones duras** del audit anterior y las extiende:

1. **Counts con paréntesis para callsites**: `grep -c "func("` no `grep -c "func"`. Diferencia 3x+ posible. Sin esto los recuentos invalidan conclusiones.

2. **Triangulación orchestrator-driven cuando sub-agents replican blind spots** (Bloque 51 #11): apliqué el audit yo mismo (orchestrator), con counts empíricos antes de leer código, antes de declarar findings. **Resultado**: detecté inmediatamente que las 195 occurrencias de `.subscribe` eran 59 callsites + 136 false positives. Si hubiera tomado el 195 como gospel habría sobre-estimado el "subscriber surface" del bundle por 3x.

3. **Mitos del session opener** (Phase D, bajaHeartbeat, axios stubs): los tres se cayeron al primer round de counts empíricos. Lección: **lo que el session opener afirma como hecho sigue siendo hipótesis hasta que el audit empírico lo confirme**. El session opener es continuidad, no autoridad — su contenido se transcribe de sesiones previas que pudieron haber declarado prematuramente.

4. **Tag MX60 implication desde el inicio** (decisión de esta sesión, sección "Research strategy"): cada finding tiene KEEP / IMPROVE / SKIP en la tabla 53.8. Esto convierte el bloque de documentación pasiva en **backlog de diseño activo para MX60** — vale como input directo a sprint planning del producto.

---

## 53.11 Próximos hilos sugeridos

### Hilos cerrados en este bloque (extension 2026-05-07)

- ✅ `me.resolve(t)` — auditado en sección 53.5.5. Asimetría Array (sequential + lease) vs Single (no lease) documentada.
- ✅ `me.resolveBetter` + `resolveBatched` — auditado en sección 53.5.6. Path rápido con BatchResolve + debounce 100ms (subutilizado por el mixin Tt).
- ✅ `_changed(component, prop)` — auditado en sección 53.5.7. Linear scan O(n) por event. MX60 → indexed lookup.
- ✅ API completa del wrapper `me` cerrada — 8 métodos/getters totales (sección 53.5.8).
- ✅ `$niagara` namespace structure mapeado — 14 sub-libs (sección 53.5.9).
- ✅ `$niagara.ord` (`pe`) — URL helpers auditados (sección 53.5.15).
- ✅ `$niagara.bql` (`sa`) — auditado en sección 53.5.11. 2 callsites en TODO el bundle. 3 bugs identificados (regex COUNT fragil, pagination off-by-one, silent errors).
- ✅ `yi` RPC wrapper — auditado en sección 53.5.12. **Hallazgo masivo**: NO es WebSocket — es wrapper sobre `$component.serverSideCall(...)`. 7 typeSpecs Reflow Commands (NAV/FILE/CSV/HISTORY/ALARM/USER/BQL).
- ✅ BQL injection en search component — vulnerabilidad MEDIUM-HIGH documentada en sección 53.5.13. AP-21 nuevo.
- ✅ Antipatterns nuevos AP-21, AP-22, AP-23 en sección 53.5.14.
- ✅ Cross-reference Java side `BReflowBQLCommands.query()` — auditado en sección 53.5.16. **Confirma RBAC nativo Niagara aplica** vía `cx` propagation, **AP-21 refinado a LOW-MEDIUM** (UX bypass, no info disclosure). 3 nuevos antipatterns server-side AP-24/25/26 + template MX60 Java Commands con 8 reglas obligatorias.

### Hilos abiertos para sesiones futuras

**HIGH value para MX60 (cada uno = potencial Bloque dedicado o sub-corrigendum)**:

- `$niagara.alarm` (`Na`) — alarm helpers. Reflow tiene gestión propia de alarms; sub-lib probable trasladable directo a MX60.
- `$niagara.bql` (`sa`) — BQL query helpers. **CRÍTICO** — toda query Niagara en MX60 va a usar BQL.
- `$niagara.history` (`Sa`) — history retrieval. Necesario si MX60 muestra trends/charts.
- `$niagara.schedule` (`la`) — Niagara BSchedule helpers. Common BAS feature.
- `$niagara.nav` (`Ci`) — navigation tree helpers. Probable wrapper sobre BajaScript nav.
- `$niagara.points` (`Ti`) — point read/write helpers. Hot path en cualquier dashboard.
- `$niagara.util.timerange` (`ka`) — time range helpers. Audited from `Bloque 49` parcialmente.
- `$niagara.util.facets` (`nQ`) — facets helpers. Bloque 49 cubre el server-side; resta cliente.

**MEDIUM value**:

- `$niagara.matrix` (`vi`) — matrix view helpers (probable Reflow-specific, evaluar si MX60 lo necesita).
- `$niagara.backups` (`Se`) — backup operations (cubierto en Bloque 50 AP-10 + Bloque 52; este sub-namespace es el cliente).
- Sample 5-8 callsites de `.subscribe(` específicos en componentes Vue concretos para validar mixin compliance (¿hay excepciones que bypasean el mixin?).
- Cross-reference con módulo Java server-side (`nmodsreflow-rt/src/com/nmods/.../ReflowService.java`) — confirmar slots/actions: `getRoles`, `demoMode`, `ga`, `getMultiUserConfig`, `getSocketTimeout`.

**LOW priority / curiosity**:

- `$build` solo en injectConfig anomalía — investigar por qué la SPA principal no expone version.
- `vt.generate()` definition — qué algoritmo usa para subscription IDs (nanoid? UUID? counter custom?).
- `niagaraEncode` / `niagaraDecode` / `uncamel` helpers superficiales del namespace.
