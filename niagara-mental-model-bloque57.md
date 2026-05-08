# Bloque 57 — Schedule + Nav + File + CSV domains audit + AP-33 file system disclosure

**Fecha**: 2026-05-07
**Método**: Audit cliente (`la`/Ci/relevant) + Java Commands (BReflowNav/File/CSVCommands.java) en Reflow-Clean-177. AP-27 search en cada uno + descubrimiento adicional del directorio `/http/` (BaseServlet + 34 response classes).
**Fuentes primarias**:
- `app-readable.js`: `la` (líneas 11522-11600), `Ci` (líneas 5195-5500+), Vuex `ua` schedule cache
- `BReflowNavCommands.java` (127 líneas, 2 métodos: bformat + getNavChildren)
- `BReflowFileCommands.java` (149 líneas, 1 método público + 3 helpers privados)
- `BReflowCSVCommands.java` (121 líneas, 1 método público)
- Sin `BReflowScheduleCommands.java` — domain usa BQL via `sa.query`

**Versión analizada**: Reflow producción 1.7.5 + Reflow-Clean-177.

---

## 57.0 Contexto, scope, qué corrige

### ¿Qué ES este bloque?

Audit combinado de 4 domains MEDIUM/LOW que comparten patrones similares:
- **Schedule** (`la`): wrapper sobre BQL — sin Java backend específico
- **Nav** (`Ci`): wrapper con yi.spec.NAV (`bformat`, `getNavChildren`) + client helpers complejos
- **File** (`BReflowFileCommands`): listFiles con path traversal vector
- **CSV** (`BReflowCSVCommands`): loadPointMap con misma vulnerabilidad

**Misión doble**:
1. Confirmar AP-27 en los 3 Commands restantes (Nav/File/CSV).
2. Descubrir AP-33 nuevo — **file system disclosure sin RBAC checks** en File/CSV Commands.

### Qué corrige / valida

| Bloque | Sección | Hallazgo | Validación |
|--------|---------|----------|-----------|
| 54 + 55 | AP-27 sistémico | Predicho en helpers Java | ✅ Confirmado en Nav (2 sites) y File (1 site) y CSV (1 site) — TODOS los Commands tienen el patrón |
| 50 | AP-10 destructive GETs | "5 backups via GET destructivo" | Apéndice — File Commands NO tiene write-via-GET pero **expone listing del filesystem sin RBAC** (AP-33 nuevo) |

### Pregunta unificadora

> ¿Cómo MX60 maneja file system access desde Commands con seguridad correcta?

**Respuesta corta**: NUNCA delegar `BFileSystem.INSTANCE.findFile(path)` directo desde un Commands con `requiredPermissions="r"`. Accesos a filesystem requieren validation de path (no traversal), whitelist de roots permitidos, RBAC explícito, y audit logging. Reflow's File/CSV Commands NO hacen ninguno de los cuatro.

---

## 57.1 Schedule domain — `la` namespace (cliente-only)

```js
la = {
    get $baja() { return Vue.prototype.$baja; },

    list: async function(parentOrd, includeIqVisionSupport = false) {
        try {
            var types = "schedule:WeeklySchedule,schedule:CalendarSchedule,schedule:TriggerSchedule";
            if (includeIqVisionSupport) types += ",TrendN4:TrendScheduleImport";

            var base = parentOrd ? pe.clean(parentOrd) : "station:|slot:/";
            var schedules = await sa.query(base + "|bql:select * from " + types);

            // Lease + read tags for each schedule
            var tagPromises = schedules.map(async (s) => { await s.lease(); return s.tags(); });
            var tags = [];
            await Promise.all(tagPromises).then(t => tags.push(...t));

            // Filter out schedules with r:ignore tag
            return schedules.filter((s, i) =>
                !tags[i].contains("r:ignore") || !tags[i].contains("r:Ignore")
            );
        } catch (err) {
            console.log("Error Loading Schedules", err);
            return [];
        }
    }
};
```

### 57.1.1 Hallazgos

- **Single method**: `la.list(parentOrd, includeIqVisionSupport)`. Reflow tiene UN método para schedules — tan delgado que **podría haber sido inline en el componente que lo usa** (Vuex action `LOAD_SCHEDULES` línea 11614).

- **Type whitelist hardcoded**: 3 schedule types canónicos (Weekly/Calendar/Trigger) + 1 opcional (TrendN4 IQ Vision support — third-party integration).

- **Tag filter `r:ignore`/`r:Ignore`**: schedules marcados con tag `r:ignore` se excluyen del listing. **Convention**: tags namespace `r:` parecen ser de Reflow custom (probable: `r:ignore`, `r:hidden`, etc).

- **`pe.clean(parentOrd)`** + concatenación BQL — usa el ord helper de Bloque 53.5.15. Seguro porque `pe.clean` strip prefixes pero no acepta user input arbitrario aquí (parentOrd viene de niagara nav, no de form text).

- **NO Java backend específico** — confirma decisión arquitectónica: si BQL puede expresarlo, no crear Commands. Mismo principio que Points domain (Bloque 56.2.3).

### 57.1.2 Vuex caching layer

`ua` (schedule store, líneas 11600-11700):
- `folderCache`: cache por folder ord → list of schedule handles
- `scheduleCache`: cache por handle → schedule object
- `LOAD_SCHEDULES` action: chequea cache, si hit → return; si miss o invalidate → call `la.list()`, hidrata cache.

**MX60 implication**: pattern caching en Vuex para entities frecuentemente leídos. **KEEP** patrón con migración a Pinia (Vue 3) — `useScheduleStore()` con same caching logic.

### 57.1.3 MX60 implications schedule

- **KEEP**: BQL-only pattern para list + filter (no Java backend redundante)
- **KEEP**: tag filter convention (`r:ignore` etc) — útil para deprecation/hide sin data deletion
- **KEEP**: Vuex/Pinia cache layer con invalidate option

---

## 57.2 Nav domain — `Ci` namespace + `BReflowNavCommands.java`

### 57.2.1 `Ci` cliente — métodos clave

```js
Ci = {
    get $baja() { return Vue.prototype.$baja; },
    get $component() { return Vue.prototype.$component; },

    typeFilterArray(t) { /* normalize to array */ },

    bformat(target, format) {
        // yi.call(yi.spec.NAV, "bformat", { ord, format }) — server-side BFormat
    },

    getNavAgents(ord) {
        // ord.$bajaViews.listViews(ord) — usa Vue.prototype.$bajaViews
        // Filter out PxEditor view (Workbench-only editor)
    },

    getHistoryGroups() {
        // CRUZA múltiples niveles:
        // 1. getNavChildren("history:")
        // 2. para cada device, getNavChildren(device.ord)
        // 3. para cada history-folder, getNavChildren(folder.ord)
        // 4. construir tree {displayName, children: [{displayName, navName, ord}, ...]}
    },

    getNavChildren(ord, typeFilter, ...) { /* yi.json(yi.spec.NAV, "getNavChildren", {ord, typeFilter}) */ },

    // ... otros métodos no auditados aquí
};
```

### 57.2.2 `BReflowNavCommands.java` (127 líneas, 2 métodos públicos)

**Método 1 — `bformat(comp, arg, cx)`**:

```java
public BValue bformat(BComponent comp, BValue arg, Context cx) throws Exception {
    String output = "";
    if (arg.getType().equals(BComponent.TYPE)) {
        try {
            BComponent comps = (BComponent) arg;
            if (comps.get("ord") != null) {
                BOrd ord = BOrd.make(comps.get("ord").toString());
                if (comps.get("format") != null) {
                    String format = comps.get("format").toString();
                    output = BFormat.format(format, ord.get());  // ⚠️ ord.get() sin cx
                }
            }
        } catch (Exception e) {
            return BString.make(output);
        }
    }
    return BString.make(output);
}
```

**Hallazgos**:
- ⚠️ `ord.get()` sin cx → **AP-27 site**.
- ✅ Strict typing del arg (rechaza no-BComponent).
- ⚠️ Catch silencioso retorna `BString.make("")` — el cliente no sabe si fue error o resultado vacío legítimo.
- ✅ `BFormat.format(format, value)` — usa el built-in Niagara format engine (Bloque 49 cubre).

**Método 2 — `getNavChildren(comp, arg, cx)`**:

```java
public BValue getNavChildren(BComponent comp, BValue arg, Context cx) throws Exception {
    BObject obj;
    BOrd ord = null;
    String[] typeFilter = null;
    JSONArray list = new JSONArray();

    // Triple-branch arg acceptance (AP-26 confirmed)
    if (arg.getType().is(BOrd.TYPE)) ord = (BOrd) arg;
    else if (arg.getType().equals(BComponent.TYPE)) {
        BComponent comps = (BComponent) arg;
        if (comps.get("ord") != null) {
            ord = BOrd.make(comps.get("ord").toString());
            if (comps.get("typeFilter") != null) {
                typeFilter = comps.get("typeFilter").toString().split(",");
            }
        }
    } else ord = BOrd.make(arg.toString());  // ← AP-26 magic

    if (ord != null && ord != BOrd.NULL && (obj = ord.get()).getType().is(BINavNode.TYPE)) {
                                              //  ⚠️ ord.get() sin cx (AP-27)
        BINavNode nav = (BINavNode) obj;
        for (BINavNode child : nav.getNavChildren()) {
            // ... build JSON node with name/displayName/ord/icon/type/hasChildren/validType
        }
    }
    return BString.make(list.toString());
}
```

**Hallazgos adicionales**:
- ⚠️ AP-26 magic toString fallback (branch 3).
- ⚠️ AP-27 site (segundo en este archivo).
- ✅ TypeFilter validation: usa `Sys.getType(typeStr)` que arroja exception si type no existe — implícitamente valida que typeFilter contiene types reales.
- ⚠️ El catch del Sys.getType es **`empty catch block`** (líneas 109-111) — silencia el error sin loguear. Bug pattern.

**Total Nav: 2 AP-27 sites** + 1 empty catch + 1 silent error catch.

### 57.2.3 MX60 implications nav

- **KEEP**: `getNavChildren` server-side returning JSON con typed metadata (name/displayName/ord/icon/type/hasChildren/validType)
- **KEEP**: `BFormat.format` server-side wrapping en bformat
- **IMPROVE**: empty catch blocks → log + propagate
- **IMPROVE**: ord.get() → ord.get(cx) en TODOS los métodos
- **IMPROVE**: catch silencioso retornando empty string → throw o struct con error explícito

---

## 57.3 File domain — `BReflowFileCommands.java` + AP-33 disclosure

### 57.3.1 Auditar `listFiles(comp, arg, cx)`

```java
public BValue listFiles(BComponent comp, BValue arg, Context cx) throws Exception {
    String filePath = null;
    if (arg != null) {
        String path = arg.toString();              // ← AP-26 magic
        if (path.contains("^"))         filePath = path.substring(path.indexOf("^"));
        else if (path.contains("module://"))  filePath = path.substring(path.lastIndexOf("module://"));
        else                                  filePath = arg.toString();
    }
    String json = filePath != null ? fileList(filePath) : fileList();
    return BString.make(json);
}
```

**Magic path prefixes**:
- `^` → station root path (Niagara convention)
- `module://` → module-resource path

**Si NO matchea ninguno** → `arg.toString()` raw como path. **Esto significa**: cliente puede mandar arbitrary path string y `listFiles(arg)` lo trata como FilePath.

### 57.3.2 `fileList(path)` — el caller

```java
private static String fileList(String path) throws Exception {
    JSONArray fileList = new JSONArray();
    if (path.startsWith("module://")) {
        BOrd ord = BOrd.make(path);
        BIFile rootDirectory = (BIFile) ord.get();        // ⚠️ ord.get() sin cx
        fileList = filesForModuleDirectory(rootDirectory);
    } else {
        BIFile rootDirectory = BFileSystem.INSTANCE.findFile(new FilePath(path));
                              // ⚠️ NO RBAC check, NO path traversal validation
        fileList = filesForDirectory(rootDirectory);
    }
    return fileList.toString();
}
```

### 57.3.3 AP-33 NUEVO — File system disclosure

**Vulnerabilidad**:

1. `path` viene del cliente sin validation. Cliente puede mandar:
   - `"^"` → station root (legítimo)
   - `"^/some/path"` → arbitrary station file path
   - `"module://niagara"` → module resources (legítimo)
   - `"../../../etc/passwd"` o paths sensibles si el FilePath constructor permite traversal

2. `BFileSystem.INSTANCE.findFile(new FilePath(path))` — **NO aplica RBAC** sobre filesystem operations. Niagara FileSystem es un namespace separado del component tree, sus ACLs no son `BSpace.canRead`.

3. `filesForDirectory(rootDirectory)` enumera children recursivamente. Cualquier user con `r` al ReflowService obtiene **listing de archivos del station file system**.

**Severity**: **HIGH** dependiendo de qué exponga la station file system. Concretamente:
- Si el station tiene `passwords.txt`, `licenses.txt`, `*.bog` (database dumps), backup files con credentials → **info disclosure crítico**.
- Si solo expone módulos públicos (`module://`) → **MEDIUM** (revela structure pero no credentials).

**Mitigación correcta**:
1. **Path whitelist**: solo permitir paths bajo `^stations/<current>/` (current station only) o `module://` (module resources).
2. **Path canonicalization + traversal check**: rechazar `..` en path.
3. **RBAC explícito**: `cx.getUser().getPermissionsFor(BFileService.TYPE).hasOperatorRead()` antes de listar.
4. **File-level ACL**: si el station tiene archivos con perms restricted, validar individualmente.

### 57.3.4 Misma vulnerabilidad en `BReflowCSVCommands.loadPointMap`

```java
public BValue loadPointMap(BComponent comp, BValue arg, Context cx) throws Exception {
    String filePath = null;
    if (arg != null) {
        String path = arg.toString();
        if (path.contains("^")) filePath = path.substring(path.indexOf("^"));
        else if (path.contains("module://")) filePath = path.substring(path.lastIndexOf("module://"));
        else filePath = arg.toString();
    }
    if (filePath.startsWith("module://")) {
        BOrd ord = BOrd.make(filePath);
        csvFile = (BIFile) ord.get();              // ⚠️ ord.get() sin cx (AP-27)
    } else {
        csvFile = BFileSystem.INSTANCE.findFile(new FilePath(filePath));
        // ⚠️ NO RBAC, NO path validation
    }
    JSONArray json = parseCsvPointMap(csvFile);
    return BString.make(json.toString());
}
```

**Same pattern**: arg → path → BFileSystem.findFile sin RBAC. **AP-33 confirmado en CSV también**.

**Severity adicional**: `parseCsvPointMap` lee TODO el contenido del archivo y lo serializa. Cliente recibe **contents** de cualquier archivo accesible. **HIGH severity** — esto es file read attacker-controlled.

### 57.3.5 MX60 implications file/csv

- **AP-33 nuevo** — file system access SIN RBAC en File/CSV Commands. Severidad **HIGH** (potencial info disclosure de credentials/backups/configs).
- **Regla 12 NUEVA template MX60**:

> **Acceso a filesystem desde Commands DEBE**:
> 1. Validar path contra whitelist explícita (e.g., `^stations/<current>/exports/`).
> 2. Canonicalizar path + rechazar `..` traversal.
> 3. Aplicar RBAC: `cx.getUser().hasFilePermission(canonical, READ)` o equivalente.
> 4. Audit logging de cada acceso (who, when, path).
> 5. Rate limiting para evitar enumeration attacks.

---

## 57.4 Nuevos antipatterns catalogados

| AP # | Patrón | Severidad | Sección |
|------|--------|-----------|---------|
| **AP-33** | File/CSV Commands acceden `BFileSystem.INSTANCE.findFile(path)` SIN RBAC, SIN path validation, SIN traversal check — file system disclosure attacker-controlled | **HIGH** (potencial info disclosure de credentials/backups/configs sensibles) | 57.3.3 + 57.3.4 |

**AP-27 confirmados nuevos sites** (no son AP nuevos, ya catalogado):
- BReflowNavCommands.java: 2 sites (bformat línea 67, getNavChildren línea 96)
- BReflowFileCommands.java: 1 site (fileList línea 86)
- BReflowCSVCommands.java: 1 site (loadPointMap línea 79)

**Total agregado AP-27 cross-bloques 53-57**: ~28+ sites. Deuda técnica masiva confirmada.

---

## 57.5 Síntesis MX60 implications — incrementales

| # | Patrón | Tag | Razón |
|---|--------|-----|-------|
| 87 | Schedule via BQL only (sin Java backend) | **KEEP** | Mismo principio que Points (Bloque 56) — abstracciones bajas por default. |
| 88 | Tag filter convention `r:ignore` para hide sin delete | **KEEP** | Útil para deprecation. MX60 → namespace `mx:` o equivalente. |
| 89 | Vuex/Pinia cache layer con invalidate option | **KEEP** | Migrar a Pinia con same logic. |
| 90 | Multi-level fetch para tree building (`getHistoryGroups` 3 niveles) | **IMPROVE** | N+1 queries pattern. MX60 → endpoint server-side que devuelve tree completo en una llamada. |
| 91 | Empty catch blocks en Java Commands | **IMPROVE** | Log + propagate. NUNCA `catch (Exception e) {}`. |
| 92 | Silent catch retornando empty string/list | **IMPROVE** | Throw o response.error slot estructurado. |
| 93 | TypeFilter validation con `Sys.getType()` exception | **KEEP pattern** (mejorado) | OK para validar pero loguear el type inválido en lugar de silenciar. |
| 94 | File system access via `BFileSystem.INSTANCE.findFile(path)` raw | **AP-33 — NUNCA** | MX60 → whitelist + canonicalize + RBAC + audit (Regla 12) |
| 95 | Magic path prefixes `^` y `module://` parseo manual | **IMPROVE** | Builder pattern: `FilePathBuilder.fromUserInput(path).validate()` |

### Resumen agregado

- **39 KEEP** (+2): schedule BQL-only, tag filter convention, multi-level fetch refactored
- **31 IMPROVE** (+4): multi-level → single fetch, empty catch fix, silent catch fix, file path builder
- **9 NEW** (sin cambios — Regla 12 fusionada con AP-33)
- **5 SKIP** (sin cambios)

**Tabla MX60 acumulada: 84 entries**.

---

## 57.6 Próximos hilos

**El Bloque 58 cambia de scope** — descubrí `/http/` directory con artefactos críticos:
- `BaseServlet.java` (367 líneas) — el HTTP router que sirve los 20 REST endpoints
- 34 response classes en `/http/responses/` — pattern por endpoint
- `CsrfGuard.java` (143 líneas) — server-side CSRF (cliente-side cubierto en Bloque 50/52)
- 5 archivos en `/http/sockets/` — SocketServlet + WebSocket implementation
- `BReflowChannelService.java` (281 líneas), `BReflowWebSocketAcceptor.java` (505 líneas) — pieza más grande del módulo

Plus AP-27 candidates en `/sync/` (BReflowSyncService, ConfigIO) y `/backups/` (BackupManager).

**Bloque 58 redefinido**: HTTP infrastructure deep dive — BaseServlet routing pattern, Response class architecture, CsrfGuard server-side, sync/backup AP-27 sites + audit del WebSocket layer (BReflowChannelService + BReflowWebSocketAcceptor).

Esto es **el audit más importante restante** porque:
- BaseServlet revela cómo Reflow rutea los 20 REST endpoints (template para MX60)
- 34 response classes son **patrón de organización** directamente trasladable
- WebSocket layer es la real-time infrastructure (Bloque 51 documented from cliente, este audit lo hace server-side)
- ConfigIO + BReflowSyncService son donde vive el sync engine RFC 6902 JSON Patch (Bloque 51 AP-17)
