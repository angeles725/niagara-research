# Block 164 — chihuahua MX60 (`-ux`): RBAC / write-authorization (el gate que Reflow no tiene)

> Deep-read del subsistema de **autorización de escritura** de chihuahua (gap C2): cómo `ChiRbacHelper`
> gatea cada mutación, la cobertura sobre los 8 handlers de control, el único riesgo residual
> (OPERATOR_WRITE global-vs-category), y el modelo de capability frontend. Es el **eje de contraste más agudo
> con nmodsreflow** (que no gatea escritura, B150/B160). Fuente primaria, lectura directa.
>
> Focus: **chihuahua** (fuente propia). Cierra C2. Corpus language: Spanish. SDD tag `mx60-rbac-and-audit-trail`.
>
> Sources (base `…/chihuahua/chihuahua-ux/src/com/angeles/chihuahua/ux/`): `ChiRbacHelper.java`,
> `BChiServlet.java` (handlers doPost), `chihuahua-{ux,rt}/module-permissions.xml`, `JS/app/CapabilityStore.js`,
> `JS/app/DashboardApp.js`, `JS/lib/WritePoint.js`. `.env.local` NO leído.
> Markers: `[CERT]` `file:line` de la fuente · `[INFER]` deducción. Capa 26. Continúa [Block 163].

---

## 164.1 — `checkCanWrite`: el mecanismo `[CERT]`

`ChiRbacHelper` es `final`, package-private, **all-static**. El enforcement vive en
`checkCanWrite(req, resp)` `[CERT]` `ChiRbacHelper.java:142-179`:

1. `remoteUser = req.getRemoteUser()` `[CERT]` `:145`.
2. **Sin usuario → 401**: `{"error":"authentication required"}`, return `false` `[CERT]` `:149-158`.
3. **Un-escape antes del lookup**: `lookupUser = unescapeUsername(remoteUser)` `[CERT]` `:164` → delega a
   `javax.baja.naming.SlotPath.unescape` `:227-230`. Necesario porque `getRemoteUser()` devuelve el nombre
   **SlotPath-escapeado** (`"Cristian Angeles"` → `"Cristian$20Angeles"`); sin esto, usuarios con espacios/
   acentos caerían falsamente a `viewer` `[CERT]` `:160-163`.
4. **Lee el permiso**: `hasWrite = resolveOperatorWrite(lookupUser)` `[CERT]` `:165`.
5. **Sin write → 403**: `{"error":"forbidden","role":"viewer"}` `[CERT]` `:166-176,108-111`.
6. **Grant → return `true`** sin escribir respuesta `:178`.

`resolveOperatorWrite` `[CERT]` `:252-290` es el seam atado a la station:
`Sys.getService(BUserService.TYPE)` → `userService.getUser(username)` → `perms = user.getPermissions(user)` →
**`return perms != null && perms.has(BPermissions.OPERATOR_WRITE)`** `[CERT]` `:281-282`. Todo el cuerpo está
en `try/catch(Exception)` que **retorna `false`** ante cualquier error `[CERT]` `:254,284-289` → **falla
CERRADO** (un error de config/station da 403, no puerta abierta).

## 164.2 — Cobertura: los 8 handlers de control gatean PRIMERO `[CERT]`

`doPost` rutea 9 acciones mutantes `[CERT]` `BChiServlet.java:186-229`. Los 8 de control BMS llaman
`checkCanWrite` como **primera sentencia**, antes de leer/parsear el body (verificado: solo comentarios/javadoc
preceden al guard):

| Handler | `checkCanWrite` primero | Línea |
|---|---|---|
| `handleSetpointWrite` | **SÍ** (antes del body read `:687`) | `:680` |
| `handleAlarmLatch` | **SÍ** | `:1321` |
| `handleAlarmUnlatch` | **SÍ** | `:1409` |
| `handleAlarmNotesPost` | **SÍ** (reemplazó un 401 remoteUser-only viejo) | `:2014` |
| `handleAlarmAckAll` | **SÍ** (reemplazó un 403 remoteUser-only viejo) | `:2080` |
| `handleUpThresholdSet` | **SÍ** | `:1526` |
| `handleCarcamoThresholdSet` | **SÍ** | `:1641` |
| `handleDataloggerThresholdSet` | **SÍ** | `:1756` |
| `handleUserThemeSet` | **NO — exención intencional** (auth-only) | `:1005-1012` |

`[CERT]` **Sin gap entre las escrituras de control.** La única mutación que salta el gate OPERATOR_WRITE es
`handleUserThemeSet` `:993-1042`: el theme es preferencia cosmética per-usuario, no control BMS → exento del
write-gate PERO **no de auth** (anónimo → 401 `:1006-1012` antes de parsear) `[CERT]` `:996-998`. Los GET de
thresholds son lecturas y correctamente no-gateadas. `[INFER]` La cobertura es uniforme y deliberada.

## 164.3 — El único riesgo residual: OPERATOR_WRITE global-vs-category `[CERT]`

Comentario cargante en `resolveOperatorWrite` `[CERT]` `ChiRbacHelper.java:274-280`:

> "BATCH 1 ASSUMPTION (load-bearing): OPERATOR_WRITE is treated as a single GLOBAL capability. … If the station
> ever uses category-scoped permissions, `has(OPERATOR_WRITE)` on the global set could report true for a
> partially-privileged user and pass the guard on EVERY endpoint (false positive)."

`[INFER]` **El riesgo:** el guard lee un set de permisos **global** (`user.getPermissions(user)`), no
per-categoría/per-ord. Un usuario con OPERATOR_WRITE en solo *algunas* categorías se trata como operador pleno
y pasa el gate en *todos* los endpoints — un falso-positivo de escalada, mitigado solo por una convención de
config de deploy (out-of-band, `workbench-config-gate.md`). Es el único hueco de este subsistema, y está
**documentado en el código**, no oculto.

## 164.4 — Sin permiso Niagara propio: reutiliza el bit de plataforma `[CERT]`

Ambos `module-permissions.xml` (`-ux` y `-rt`) son **stubs vacíos** — boilerplate Tridium idéntico, los 3
grupos (`all`/`workbench`/`station`) solo comentarios, el único `req-permission` de ejemplo comentado `[CERT]`.
chihuahua **NO declara permiso Niagara custom**: reutiliza a propósito el built-in `OPERATOR_WRITE` (ADR D1).

## 164.5 — Capability endpoint: server autoritativo, frontend decorativo `[CERT]`

- **Backend** `GET /api/user/capability` → `handleUserCapability` (401 sin usuario) → `resolveCapabilityJson` →
  **`{"username","role":"operador|viewer","canWrite":<bool>}`** `[CERT]` `BChiServlet.java:1065-1092`,
  `ChiRbacHelper.java:190-205,84-99`. Fallback no-autenticado devuelve `viewer` `:193-198`.
- **Frontend** `CapabilityStore.js` (ES5 IIFE): XHR `GET /mx60/api/user/capability` con `X-Requested-With`,
  **deny-by-default** `{role:'viewer',canWrite:false}` que se mantiene ante cualquier no-200/parse-error/timeout
  `[CERT]` `CapabilityStore.js:36-40,108-124`. `DashboardApp._applyWriteGuard(cap.canWrite)` **oculta/deshabilita
  todos los controles de escritura** si `canWrite=false` `[CERT]` `DashboardApp.js:184,208`; `WritePoint.js`
  también corta client-side si `CapabilityStore.canWrite()===false` `:49-55`.
- **Autoritativo vs decorativo** (ADR D6): "server is authoritative; this store is DECORATIVE ONLY … the 403
  server-side guard (ChiRbacHelper) is the real enforcement point" `[CERT]` `CapabilityStore.js:18-20`.

## 164.6 — ADRs y testability seam `[CERT]`

- **D1** — capability por el bit `OPERATOR_WRITE`, no por nombre de rol (frágil) `[CERT]` `ChiRbacHelper.java:15-18`.
- **D2** — `checkCanWrite` es el ÚNICO enforcement point y DEBE ser la primera llamada de todo write handler,
  antes de parsear el body `[CERT]` `:20-24`.
- **D6** — server autoritativo, store frontend decorativo (§164.5).
- **Testability seam** — todas las APIs atadas a station (`BUserService`, `BPermissions`, `SlotPath`) quedan
  aisladas dentro de `checkCanWrite`/`resolveOperatorWrite`/`unescapeUsername`, dejando la lógica pura
  WSL-unit-testeable `[CERT]` `:26-31`.

## 164.7 — Connections (el contraste con Reflow)

`[INFER]` **La diferencia arquitectónica con nmodsreflow es de CAPA de gateo:**
- **Reflow** ([Block 146]/[Block 150] §150.1): los command agents gatean a `"r"` vía `@AgentOn` — el gate
  cabalga la *registración del agente*, no el *dato*; y el router REST `BaseServlet` **no tiene gate alguno**
  (grep negativo), por lo que los statics invocados directo lo bypassean → config-write a read-level
  (confirmado en vivo, [Block 160]).
- **chihuahua**: `checkCanWrite` es la **primera sentencia del handler** (gatea la *acción/dato*, no una
  registración), con el bit `OPERATOR_WRITE`, fail-closed, uniforme sobre los 8 endpoints mutantes. **No hay
  bypass REST porque el gate está DENTRO del handler.**

- **[Block 146]/[Block 150]/[Block 160]** — el modelo de auth de Reflow (gate mal ubicado, bypass REST).
- **[Block 163]** — el headline RBAC que este bloque profundiza.
- **Backlog** — C5 (audit) es el complemento natural (toda escritura gateada además se audita); C3 (paridad de
  superficie) tabulará esto endpoint-por-endpoint contra Reflow.
- **Focus `chihuahua`** — C2 cerrado; el gate de escritura queda documentado como el diferenciador de seguridad.
