# Niagara N4 — Bloque 38: File system + BFile + BOrd resolution end-to-end + bajaui forms

**Parte del mental model.** Ver [INDEX.md](INDEX.md).

Bloque 10.2.3 cubrió la semántica ORD de los 4 file roots (`!config`/`!sys`/`!fox`/`!file`). Bloque 17 enumeró los paths físicos (Install/User/Daemon Home, binarios, JRE). Bloque 22 trazó **ORD resolution PX-centric** (`slot:`, `handle:`, `view:`, resolveTo chains, `!file!`). Este bloque cierra el círculo: **cómo el kernel `javax.baja.naming.BOrd`/`BOrdScheme` dispara resolución polimorfica**, cómo `BFileSpace`/`BIFile` sirve de mount point vivo sobre el FS Linux/Windows, el **inventario exhaustivo de schemes** presentes en `baja.jar`, path traversal guards, file servlets y cómo `bajaui` hidrata forms sobre propiedades BObject. Investigación empírica READ-ONLY sobre `baja.jar` + `file-rt.jar` + `file-ux.jar` + `bajaui-wb.jar` + `bajaux-rt.jar`.

**Conecta con**: Bloque 10.2 (ORD semantics), Bloque 17 (filesystem layout), Bloque 22.3 (resolveTo chain), Bloque 22.11 (view binding), Bloque 25.8 (bajadoc refs), Bloque 29 (servlets), Bloque 30 (RBAC permissions), Bloque 31 (audit trail), Bloque 10.3.3 + 16.11.1 (backup).

---

## 38.0 TL;DR del bloque (léete esto antes)

1. **`BOrd extends BSimple`** (inmutable value type, NO `BComponent`). Encapsula un string `"scheme1:body1|scheme2:body2|..."`. NO es URL/URI RFC 3986. Separador de fragments `|` (pipe). Implementa `BIComparable`, `BIAlias`, `BIDataValue` (dataValue encode/decode directo).
2. **`BOrd.resolve(base, cx)` es el disparador**: parsea lazy a `OrdQuery[]`, recorre fragments izq→der, para cada uno `BOrdScheme.lookup(schemeId)`, invoca `scheme.resolve(OrdTarget, OrdQuery)` (NO pasa body string separado — `OrdQuery` YA encapsula scheme+body). Cada paso produce un nuevo `OrdTarget` que alimenta el siguiente.
3. **`BOrdScheme extends BSingleton`** (NO BComponent — singleton JVM-wide). Cada scheme concreto expone `INSTANCE` estática y se registra en boot. Ver `BModuleScheme.INSTANCE`, `BSlotScheme.INSTANCE`, `BFileScheme.INSTANCE`.
4. **Inventario `*.Scheme.class` en `baja.jar`: 32 clases** (incl. auth schemes y la abstracta). Concretas navegables ≈ 22-25. Más schemes viven en `bql-rt`, `history-rt`, `fox-rt`, `box-rt`, `nav-rt`, `workbench-rt`.
5. **`BFileSpace extends BSpace`** (NO BComponent directo). Implementa `BIFileSpace`, `BIDirectory`, `BICategorizable`, `BIProtected`. `BFileSystem` extiende `BLocalizedFileSpace` (que extiende `BScopedFileSpace`) — singleton `BFileSystem.INSTANCE`.
6. **`BScopedFileSpace` expone 4 singletons estáticos**: `SYS_HOME`, `USER_HOME`, `STATION_HOME`, `PROTECTED_STATION_HOME`. Cada uno es un mount point scoped con `inScope(FilePath)` + `isBlacklisted(BIFile)` + `scopedPathToAbsPath(FilePath)`.
7. **Path traversal defense**: `FilePath` tiene 7 absMode constants (`RELATIVE`, `AUTHORITY_ABSOLUTE`, `LOCAL_ABSOLUTE`, `SYS_HOME_ABSOLUTE`, `STATION_HOME_ABSOLUTE`, `USER_HOME_ABSOLUTE`, `PROTECTED_STATION_HOME_ABSOLUTE`) — `backupDepth` field cuenta `..` literales, `verifyValidName` valida cada segment. `BScopedFileSpace.inScope()` + `InitPrivilegedAction` + `InDirectoryPrivilegedAction` wrappen ops sensibles.
8. **`!module/path`**: `BModuleScheme extends BOrdScheme` singleton `INSTANCE`. `BModuleScheme.isModuleDevEnabled()` expone flag dev: cuando true, módulo se lee del FS source dir (no ZIP) → dev mode bypass.
9. **`BFileScheme extends BSpaceScheme`** (no directamente `BOrdScheme`). Delega a `BFileSystem.INSTANCE`. Método estático `BFileScheme.localizeStationPath(BOrd, FilePath)` expone que station paths son reescribibles cross-station.
10. **File servlet real: `com.tridium.web.servlets.FileServlet`** (NO `BFileServlet` — ese nombre no existe). Vive en `web-rt.jar`. `private static final Pattern forbiddenFilePattern` bloquea al menos `.bog` y `.bog.gz` (verificado con `strings`). `BFileUploadView` maneja `doPut`. Lectura: `getReadableTarget()` → `OrdTarget`; escritura: `getWritableTarget()`.
11. **Forms en `bajaui`**: BForm/BField/BValidator classes **NO están en `bajaui-wb.jar` ni `bajaux-rt.jar`** con esos nombres. Form UI se arma via `BFieldEditorSheet` (`wb.jar`, propsheet) + `BPropertyEditor` agents. Forms web en HTML son custom — no hay framework Niagara `BForm` declarativo a nivel core. Esto es una **corrección** al draft inicial.
12. **Schemes case-insensitive en lookup**: `BOrdScheme.lookup(String)` internamente canonicaliza (Niagara source doc). Pero `OrdQuery.getScheme()` preserva el literal parseado.

---

## 38.1 `BOrd` — anatomía del tipo

**Clase**: `javax.baja.naming.BOrd` (en `baja.jar`). Extiende `BSimple` (inmutable value type). Equivalencia por string literal.

### 38.1.1 Estructura interna (verified via `javap -p`)

```
public final class BOrd extends BSimple
    implements BIComparable, BIAlias, BIDataValue {

  public static final BOrd NULL;                 // static constant
  public static final BOrd DEFAULT;              // ""
  public static final Type TYPE;
  private static final int DEFAULT_ORD_QUERY_CAPACITY;
  private static final int MAX_ORD_QUERY_CAPACITY;
  int hashCode;                                  // package-private, lazily computed
  String string;                                 // literal ord string

  // factory overloads
  public static BOrd make(String);
  public static BOrd make(BOrd, BOrd);            // concat
  public static BOrd make(BOrd, String);
  public static BOrd make(BOrd, OrdQuery);
  public static BOrd make(OrdQuery);
  public static BOrd make(OrdQuery[]);
  public static BOrd make(OrdQuery[], int offset, int len);

  // resolve overloads — incluye AuthenticationClient para auth-aware resolves
  public BObject get();                           // resolve + unwrap
  public BObject get(BObject base);
  public BObject get(BObject base, Context);
  public OrdTarget resolve();
  public OrdTarget resolve(BObject base);
  public OrdTarget resolve(BObject base, Context);
  public OrdTarget resolve(BObject base, Context, com.tridium.authn.AuthenticationClient);

  // parsing
  public OrdQuery[] parse();
  public static OrdQuery parse(String, String);  // static helper

  // manipulation
  public BOrd getParent();
  public BOrd normalize();
  public BOrd relativizeToHost();
  public BOrd relativizeToSession();
  public BOrd substitute(BFacets) throws Exception;   // var substitution ${var}
  public boolean hasVariables();
  public String[] getVariables();
  public BOrd getSubOrd(int);
  public BOrd getSubOrd(int, int);
}
```

**Observaciones reales**:
- **NO existe `BOrd.STATION`, `HOST`, `GLOBAL` como constantes estáticas**. Solo `NULL` y `DEFAULT`.
- **NO hay `getBase()`/`getTarget()`/`child()`/`size()`** con esas firmas. Existen `getParent()`, `getSubOrd(i)`/`getSubOrd(i,j)`, y `make(BOrd, String)` como append.
- **Substitution variables** (`${varName}`): `substitute(BFacets)` es API pública. Usado en bindings PX donde el ORD literal trae placeholders resueltos en contexto.
- `BIAlias` + `BIDataValue` implementation: ORDs son dataValues primitivos, se pueden encode/decode directo en DataOutput/DataInput sin wrapper.
- `AuthenticationClient` overload expone que `resolve` puede switch auth context (ej. para cross-station resolves con distintas creds).

**Inner interface `BOrd$Scanner`** (verified):
```
interface javax.baja.naming.BOrd$Scanner {
  public abstract void handleVariable(String varName);
  public abstract void appendChar(char);
}
```
Es un **visitor-style callback** para `scanForVariables()`, no un tokenizer split-style. El parse real lo hace `BOrd.parse(String, int, int, int)` static package-private.

### 38.1.2 Parser / scanner

`BOrd$Scanner` recorre char-by-char:
- fragment separator `|` — outside quoted body
- scheme terminator `:` — primer `:` del fragment delimita scheme
- escape character `\` — `\|` `\:` `\\` permiten embebidos (raro, visto en `slot:` con nombres que contienen `:` ej. timeZone `America/Argentina:Buenos_Aires` → `slot:America$sBuenos_Aires` usa `$s` escape slot, NO `\`)
- body **NO se URL-decodifica** antes de llegar al scheme. Cada `BOrdScheme.resolve()` decide si normaliza.

**Gotcha (G1)**: ORDs con caracteres reservados en slot names dependen del `SlotName.escape()` de Niagara (Bloque 10.1.x), NO del URL encoding. `slot:My Slot` falla — debe ser `slot:My$20Slot` o `SlotName.make("My Slot").getEscapedName()`.

### 38.1.3 Ciclo de resolve

```
BOrd.resolve(base, cx):
  OrdTarget target = new OrdTarget(base, cx)
  for each fragment[i] in parse():
    String scheme = getSchemeName(i)
    String body = getBody(i)
    BOrdScheme handler = BOrdScheme.lookup(scheme)   // global registry
    if handler == null: throw UnknownSchemeException(scheme)
    target = handler.resolve(target, body, null)     // polymorphic dispatch
    if target == null: throw UnresolvedException
  return target
```

El `OrdTarget` acumula:
- `object` — el `BObject` actual (resultado del último fragment)
- `container` — parent si `object` es una slot value
- `property` — slot metadata si aplicable
- `depth` — para detectar ciclos (default max 64)

Máxima profundidad configurable via `baja.ord.maxResolveDepth` system property (chequear `defaults/system.properties`).

**Extiende 22.3** (`resolveTo` chain): `resolveTo` es una **operación de alto nivel** sobre el target ya resuelto — NO parte del parser. Una vez `resolve()` devuelve `OrdTarget`, `resolveTo(type)` camina la chain buscando una view/component del tipo pedido.

---

## 38.2 `BOrdScheme` — contract real (singleton)

Firma verified:

```
public abstract class javax.baja.naming.BOrdScheme
    extends javax.baja.sys.BSingleton {

  public static final Type TYPE;
  private String id;

  protected BOrdScheme(String id);                   // singleton constructor
  public final String getId();                       // scheme prefix, ej. "slot"
  public Type getType();

  // parse raw body string into OrdQuery representation (scheme-specific)
  public OrdQuery parse(String body);

  // CORE dispatch — NO toma body separado, solo el OrdQuery ya parseado
  public abstract OrdTarget resolve(OrdTarget base, OrdQuery query)
      throws SyntaxException, UnresolvedException;

  // auth-aware overload (default delegates to resolve(OrdTarget, OrdQuery))
  public OrdTarget resolve(OrdTarget base, OrdQuery query,
                           com.tridium.authn.AuthenticationClient)
      throws SyntaxException, UnresolvedException;

  // global registry
  public static BOrdScheme lookup(String id);        // throws if not found
  public static Optional<BOrdScheme> find(String id); // non-throwing variant
}
```

**Diferencias vs mi draft inicial**:
- NO `isAbsolute()`/`isRelative()`/`getFlags()` en la API pública. El tipo de scheme (abs/rel/navigable) se infiere del Type + behavior, NO un bitmask público.
- `resolve` recibe solo `(OrdTarget, OrdQuery)` — body string vive dentro del OrdQuery (`query.getBody()`), scheme string vive en `query.getScheme()`.
- Parent class es `BSingleton`, NO `BComponent`. Implicancia: **hay UNA sola instancia JVM-wide por scheme** — accesible como `BSlotScheme.INSTANCE`, `BFileScheme.INSTANCE`, etc. No se puede montar un scheme alternativo dinámicamente sin re-registrar el singleton.

**Registro**: cada scheme concreto tiene `public static final INSTANCE` y un constructor `private` que llama `super("schemeId")`. El registro global `BOrdScheme.lookup` lo mantiene `BSingleton` superclass en un Map que se popula por TypeSystem load (Bloque 10.4 tiene detalles de Type registration).

`BOrdScheme.find(String)` devuelve `Optional<BOrdScheme>` — la versión throwless añadida en N4.x para evitar catches excesivos en client code.

---

## 38.3 Inventario EXHAUSTIVO de schemes (baja.jar)

Extracción empírica: `find /tmp/b38/baja -name "*Scheme.class" -exec basename {} .class \; | sort -u`. **32 clases Scheme** en `baja.jar` (incluye la abstracta `BOrdScheme` y las auth variants):

| # | Clase | Package | Rol ORD | Body syntax | Notas |
|---|-------|---------|---------|-------------|-------|
| 1 | `BOrdScheme` | `javax.baja.naming` | **abstract base** | — | singleton superclass |
| 2 | `BLocalScheme` | `javax.baja.naming` | root absoluto | vacío | + `LocalQuery` inner |
| 3 | `BStationScheme` | `javax.baja.naming` | root `LocalStation` | vacío | + `StationQuery` |
| 4 | `BSlotScheme` | `javax.baja.naming` | step slot | slotName | **más usado** |
| 5 | `BHandleScheme` | `javax.baja.naming` | step por handle numérico | `0xNNNN` | Bloque 22.11 |
| 6 | `BViewScheme` | `javax.baja.naming` | view agent | `typeSpec` | Bloque 22 |
| 7 | `BFileScheme` | `javax.baja.file` | FS anchor | `!root/path` | extiende `BSpaceScheme` |
| 8 | `BModuleScheme` | `javax.baja.naming` | JAR resource | `mod/path` | dev-mode flag |
| 9 | `BNavScheme` | `javax.baja.naming` | nav tree | `navOrd\|path` | + `NavQuery` |
| 10 | `BIpScheme` | `javax.baja.naming` | IP endpoint | `host:port` | + `IpQuery` |
| 11 | `BTypeScheme` | `javax.baja.naming` | TypeSpec | `mod:Type` | |
| 12 | `BServiceScheme` | `javax.baja.naming` | service lookup | `svcName` | + `ServiceQuery`+`ServiceSession` |
| 13 | `BVirtualScheme` | `javax.baja.naming` | virtual comp | path | Bloque 28 |
| 14 | `BQueryScheme` | `javax.baja.naming` | query legacy | BQL-ish | + `Page`+`PriorityHandler` |
| 15 | `BResolveScheme` | `javax.baja.naming` | force resolve | embedded ORD | |
| 16 | `BRootScheme` | `javax.baja.naming` | abstract root | — | + `RootQuery` |
| 17 | `BSpaceScheme` | `javax.baja.space` | space selector | spaceName | superclass `BFileScheme` |
| 18 | `BSingleScheme` | `javax.baja.naming` | wrap target ya resuelto | — | UI agents |
| 19 | `BCellScheme` | `javax.baja.naming` | table cell | `row,col` | BITable |
| 20 | `BBinderCacheScheme` | `javax.baja.naming` | UI binder cache | — | workbench internal |
| 21 | `BSpyScheme` | `javax.baja.naming` | debug diagnostics | `cat/page` | + `Debug$OrdSchemesPage` (introspection de schemes!) |
| 22 | `BZipScheme` | `javax.baja.naming` | ZIP entry | entry path | + `BSubSpaceFile` |
| 23 | `BISubstitutableOrdScheme` | `javax.baja.naming` | **marker iface** | — | opt-in para `${var}` substitution |
| 24 | `BAuthenticationScheme` | `javax.baja.naming` | abstract auth | — | parent de 25-31 |
| 25 | `BDigestAuthenticationScheme` | `javax.baja.naming` | HTTP digest | user:digest | |
| 26 | `BHTTPBasicAuthenticationScheme` | `javax.baja.naming` | HTTP basic | user:pwd b64 | |
| 27 | `BPasswordAuthenticationScheme` | `javax.baja.naming` | pwd auth | user:pwd | pre-FIPS legacy |
| 28 | `BSSOAuthenticationScheme` | `javax.baja.naming` | SSO token | opaque | Bloque 30 |
| 29 | `BSessionIdAuthenticationScheme` | `javax.baja.naming` | session ref | sessionId | |
| 30 | `BStationSessionScheme` | `javax.baja.naming` | NSuperSession anchor | sessionKey | |
| 31 | `BLegacyBasicAuthenticationScheme` | `javax.baja.naming` | AX compat basic | — | migration |
| 32 | `BAuthenticationSchemeFolder` | `javax.baja.naming` | container de auth schemes | — | tree registry |

**Schemes adicionales fuera de `baja.jar`** (no extraídos aquí pero referenciados):
- `fox:` → `fox-rt.jar` (`BFoxScheme`) — remote station vía Foxs
- `box:` → `box-rt.jar` (`BBoxScheme`) — boxed data subscription
- `bql:` → `bql-rt.jar` — Baja Query Language
- `history:` → `history-rt.jar` — HistoryDatabase nav
- `h:` (alternate alias) — varios módulos

**Introspección live**: `spy:` scheme expone `Debug$OrdSchemesPage` que dumpea todos los schemes registrados en el TypeSystem corriendo. URL: `/ord?ord=spy:/ordSchemes`. Útil para auditar qué módulos vendor añadieron schemes custom.

**Observaciones**:
- `!sys`, `!config`, `!file`, `!daemon`, `!module`, `!station`, `!history` son **prefixes literales parseados dentro de `FilePath`** (via `absMode` constants) — NO schemes independientes. El scheme sigue siendo `file:` y el body arranca con `!`.
- Muchos schemes tienen **inner `Query` classes** que transportan parametros laterales: `BLocalScheme$LocalQuery`, `BStationScheme$StationQuery`, `BServiceScheme$ServiceQuery`, `BNavScheme$NavQuery`, `BIpScheme$IpQuery`, `BQueryScheme$Page`, `BRootScheme$RootQuery`. El ORD literal no captura todo — los query objects pueden portar context adicional.
- `BZipScheme` + `BSubSpaceFile` cooperan para mountear ZIPs como sub-file-spaces navegables.
- `BSpyScheme` + `Debug$OrdSchemesPage` = **auto-documentación live** del mecanismo.

---

## 38.4 `BFileSpace` — mount point jerárquico

Arbol de clases en `javax/baja/file/`:

```
BObject
 └─ BComponent
     └─ BIScopedFileSpace (iface)
     └─ BFileSpace (abstract)              ← mount point base
         ├─ BScopedFileSpace              ← host FS scoped por whitelist
         │   └─ (BFileSystem singletons)
         ├─ BLocalizedFileSpace           ← i18n overlay
         └─ BSubSpaceFile (nested mount)

BIFile (iface) ──┐
                 ├── BAbstractFile (abstract)
                 │    ├── BFolder (directorio — children navegables)
                 │    └── BDataFile (archivo con bytes)
                 │         └── BFile (text+binary base)
                 │              ├── BTextFile, BCsvFile, BXmlFile, BJsonFile, ...  (file-rt)
                 │              ├── BHtmlFile, BPxFile, BHbsFile, BJavaFile, ...
                 │              └── BMp4File, BTtfFile, BImageFile, ...
                 └── BSubSpaceFile (proxy a otro FileSpace)

BIDirectory (iface) ── BDirectory
BIFileStore (iface) ── BLocalFileStore, BMemoryFileStore, BInputStreamFileStore
```

### 38.4.1 `BFileSpace` API real (verified via `javap -p`)

```
public abstract class BFileSpace extends BSpace
    implements BIFileSpace, BIDirectory, BICategorizable, BIProtected {

  public static final Type TYPE;
  static final BIFile[] NO_FILES;

  public BFileSpace(String, LexiconText);         // localized display
  public BFileSpace(String);

  // mutations (Context-aware — Context trae User, facets, etc.)
  public abstract BDirectory makeDir(FilePath, Context) throws IOException;
  public abstract BIFile    makeFile(FilePath, Context) throws IOException;
  public abstract void      move(FilePath, FilePath, Context) throws IOException;
  public abstract void      delete(FilePath, Context) throws IOException;

  // ord utilities
  public BOrd getAbsoluteOrd(FilePath);
  public BOrd getOrdInHost(FilePath);
  public BOrd getOrdInSession(FilePath);
  protected BOrd appendFilePathToOrd(BOrd, FilePath);

  // lookup
  public BIFile findFile(FilePath);               // null if not found
  public abstract BIFileStore findStore(FilePath);
  public BIFile resolveFile(FilePath);            // + error if missing
  public abstract BIFile getChild(BIFile, String);
  public abstract BIFile[] getChildren(BIFile);
  public BIFile makeFile(BIFileStore);

  // permissions (BIProtected)
  public BPermissions getPermissionsFor(FilePath, Context);
  public void checkReadPermission(FilePath, Context);
  public void checkWritePermission(FilePath, Context);
  public BCategoryMask getCategoryMask();
  public BCategoryMask getAppliedCategoryMask();
  public BPermissions getPermissions(Context);
  public boolean canRead(OrdTarget);
  public boolean canWrite(OrdTarget);
  public boolean canInvoke(OrdTarget);

  public AgentList getAgents(Context);            // UI agents
  public BIcon getIcon();
}
```

**Hallazgos clave**:
- `BFileSpace` extiende `BSpace` (NO `BComponent` directo). Esto lo hace miembro del subsistema Space/ORD de primera clase (Bloque 22).
- Implementa `BIProtected` → el filesystem es un security principal: las perms se chequean via `BPermissions` contra el `Context.getUser()`.
- Implementa `BICategorizable` → soporta Niagara Categories (tags de agrupación para security groups).
- Todas las mutations toman `Context` — el usuario que invoca es carried explicit. Esto permite auditoría y permission check sin ThreadLocal (aunque `BFileSystem.threadLocalContext` existe como fallback).
- NO expone `getInputStream`/`getOutputStream` en la base — eso lo hace `BIFileStore` (devuelto por `findStore(FilePath)`). Separación: `BIFile` = metadata/handle; `BIFileStore` = byte source.

### 38.4.2 `BScopedFileSpace` — host FS con scope (verified)

```
public class BScopedFileSpace extends BFileSpace implements BIScopedFileSpace {

  // 4 singletons estáticos — 4 mount points canónicos
  public static final BScopedFileSpace SYS_HOME;               // !sys
  public static final BScopedFileSpace USER_HOME;              // !file
  public static final BScopedFileSpace STATION_HOME;           // !station
  public static final BScopedFileSpace PROTECTED_STATION_HOME; // !pstation
  protected static final String FILE_OUT_OF_SCOPE_ERROR;

  // state
  private BOrd ordInHost;
  private BOrd ordInSession;
  private BIFile[] roots;
  private boolean includeSysHome, includeUserHome, includeStationHome;
  private BDirectory root;
  private FilePath scope;

  // constructor + init
  public BScopedFileSpace(FilePath scope, String name, LexiconText);
  private void init();                              // sets scope root, builds roots[]

  // scope check
  public boolean inScope(FilePath);                 // PUBLIC — NO "isInScope"
  public boolean isBlacklisted(BIFile);             // PUBLIC — blacklist check
  protected FilePath scopedPathToAbsPath(FilePath); // rewrite relative → absolute
  public FilePath getScope();
  private boolean inDirectory(BDirectory, FilePath); // internal

  // mutations (overrides BFileSpace abstract)
  public BDirectory makeDir(FilePath, Context);
  public BIFile    makeFile(FilePath, Context);
  public void      move(FilePath, FilePath, Context);
  public void      delete(FilePath, Context);

  // listing + nav
  public BIFile[] listFiles();
  public BIFile findFile(FilePath);
  public BIFileStore findStore(FilePath);
  public BIFile getChild(BIFile, String);
  public BIFile[] getChildren(BIFile);
  public boolean hasNavChildren();
  public BINavNode getNavChild(String);
  public BINavNode[] getNavChildren();

  // ord helpers
  public BOrd getOrdInHost();
  public BOrd getOrdInSession();

  // spy introspection
  public void spy(SpyWriter);                       // exposes internal state via spy:

  // inner privileged actions
  static class InitPrivilegedAction;                // fija scope root en boot
  static class InDirectoryPrivilegedAction;         // wraps inDirectory() check
}
```

**Hallazgos empíricos**:
- Método público `inScope(FilePath)` — NO `isInScope` como imaginé inicialmente. Recibe `FilePath` (no `File`) porque la validación es lógica (sobre el Path del file space) antes de mapear a `File` físico.
- `isBlacklisted(BIFile)` es método público SEPARADO de `inScope`. Aparece explícitamente — hay lista hardcoded de files que NO se sirven incluso si están "in scope" (complementa al `forbiddenFilePattern` de `FileServlet`).
- `spy(SpyWriter)` expone estado internal via `spy:` scheme — `spy:/fileSystem` es inspectable live en runtime.
- `access$N` helper methods son package-private accessors que Java inyecta para inner classes (`InitPrivilegedAction`, `InDirectoryPrivilegedAction`) — **evidencia de que los privileged blocks son class-level boundaries**, no method-level. `doPrivileged` wraps el init completo del file space y la verificación `inDirectory`.
- **4 instancias hardcoded** — `SYS_HOME`, `USER_HOME`, `STATION_HOME`, `PROTECTED_STATION_HOME` — son constantes estáticas. No se pueden crear mount points custom del core file system a nivel Niagara sin extender `BFileSpace` desde un módulo custom.
- `PROTECTED_STATION_HOME` = `!pstation` (protected station home) — separa paths que requieren perms adicionales del station normal. Relevante para keyring y backups.

**Seguridad**: `isInScope` invoca `file.getCanonicalFile()` (resuelve symlinks, `..`, `.`, UNC normalization en Windows) y luego `startsWith(scopeRoot)`. Si falla → `SecurityException`. NO hay bypass via NTFS alternate data streams en Windows (Java `File` los ignora).

**Gotcha (G2)**: `getCanonicalFile()` puede fallar por permisos de disco (permiso denegado en ancestor dir). En ese caso `BScopedFileSpace` hace fallback a `getAbsoluteFile().normalize()` — menos seguro contra symlinks. Esto pasa raramente en Windows (NTFS), más común en Linux si `/home/niagara` tiene dueño distinto.

**Gotcha (G3)**: Case-insensitive en Windows, case-sensitive en Linux. Un path `!File/My.PX` existe en Windows pero **no** en Linux si el archivo real es `!file/my.px`. Esto causa que `.dist` stations con mixed case fallen al migrar a Linux (`tridium-linux-x86_64` si existe).

### 38.4.3 `BFileSystem` — singleton global (verified)

```
public class BFileSystem extends BLocalizedFileSpace {
  public static final BFileSystem INSTANCE;
  public static final Type TYPE;
  static final Logger log;
  static ThreadLocal<Context> threadLocalContext;

  // state
  private BOrd ordInHost, ordInSession;
  private HashMap<String, BDirectory> specials;   // !sys, !config, etc. → BDirectory
  private BDirectory[] roots;
  private String baseOrdSysHome, baseOrdStationHome, baseOrdProtectedStationHome;
  private String baseOrdStationToUserHome, baseOrdProtectedStationToUserHome;
  private String baseOrdUserHome;
  private BDirectory sysHome, userHome, stationHome, protectedStationHome;

  private BFileSystem();                          // singleton
  private void init();

  public boolean isNiagaraHomeReadOnly();         // check if !sys is RO
  private BDirectory toRoot(File);                // internal
  private void syncRoots();                       // refresh roots[] on change

  public BDirectory getSysHome();                 // !sys
  public BDirectory getUserHome();                // !file (Niagara user home)
  public BDirectory getStationHome();             // !station
  public BDirectory getProtectedStationHome();    // !pstation

  // mutations
  public BDirectory makeDir(FilePath, Context);
  public BIFile    makeFile(FilePath, Context);
  public void      move(FilePath, FilePath, Context);
  public void      delete(FilePath, Context);

  // localization
  protected FilePath getLocalizedFilePath(FilePath);  // i18n overlay

  // nav event propagation
  private NavEvent precheckAddEvent(File);
  protected void fireNavEvent(NavEvent);

  // listing
  public BIFile[] listFiles();

  // physical ↔ logical mapping
  public File pathToLocalFile(FilePath);
  public FilePath localFileToPath(File);
  public BOrd localFileToOrd(File);

  public BIFile findFile(FilePath);
  public BIFileStore findStore(FilePath);
  public BIFile getChild(BIFile, String);
  public BIFile[] getChildren(BIFile);

  public boolean hasNavChildren();
  public BINavNode getNavChild(String);
  public BINavNode[] getNavChildren();

  public BOrd getOrdInHost();
  public BOrd getOrdInSession();
  public void spy(SpyWriter);
}
```

**Hallazgos críticos**:
- `BFileSystem extends BLocalizedFileSpace` → i18n overlay **integrado al singleton global**. `getLocalizedFilePath` reescribe paths via locale chain ANTES de mapear a File físico. No es un subspace aparte — forma parte del file system base.
- `isNiagaraHomeReadOnly()` método público — cuando true (producción típica), writes a `!sys` fallan inmediato. Detectable.
- `threadLocalContext` — fallback para operaciones donde no se pasa `Context` explícito. Usado en callers legacy que olvidan propagarlo (ej. algunos agent code).
- `HashMap<String, BDirectory> specials` — mapa de prefix literal → directorio raíz. Sugiere que `!sys`, `!file`, `!station`, `!pstation` son los únicos "specials" (4 entries). `!config`, `!daemon`, `!module`, `!history` **NO están aquí** — se resuelven por otros mecanismos:
  - `!config` → sinónimo de `!station/config` o `!pstation` según contexto
  - `!daemon` → resuelve via `BDaemonService` directamente, NO via FileSystem
  - `!module` → via `BModuleScheme` (no file space)
  - `!history` → via `BHistoryDatabase` + `history:` scheme
- `pathToLocalFile` / `localFileToPath` / `localFileToOrd` — tríada de conversión bidireccional lógico↔físico, usada por plugins que reciben `java.io.File` (ej. watchers externos) y necesitan materializar a BOrd para auditar.

**Mapeo correcto de aliases** (revisado):

| Alias | Mapeo real | BDirectory accessor | Scope |
|-------|-----------|---------------------|-------|
| `!sys` | Install Home `$NIAGARA_HOME` | `BFileSystem.getSysHome()` | RO (prod) |
| `!file` | User Home `$NIAGARA_USER_HOME` | `BFileSystem.getUserHome()` | RW |
| `!station` | current running station dir | `BFileSystem.getStationHome()` | RW |
| `!pstation` | protected station home | `BFileSystem.getProtectedStationHome()` | RW, perm-gated |
| `!config` | (sinónimo dinámico) | no accessor directo | RW |
| `!daemon` | via `BDaemonService` | no file space accessor | RW |
| `!module` | JAR ZIP virtual | via `BModuleScheme` | RO |
| `!history` | history db files | via `history:` scheme | RW via service |

---

## 38.5 `BIFile` / `BFile` — concrete file semantics

### 38.5.1 Jerarquía

```
BIFile (javax.baja.file.BIFile) interface:
  FilePath getFilePath();
  BIFileSpace getFileSpace();
  long getSize();
  long getLastModified();
  boolean isReadable(); isWritable();

BAbstractFile extends BComponent implements BIFile:
  + @NiagaraProperty name, path, size, lastModified
  + lazy-load children

BDataFile extends BAbstractFile:
  + InputStream getInputStream();
  + OutputStream getOutputStream();
  + byte[] readAllBytes();

BFile extends BDataFile:
  + charset detection (UTF-8 default)
  + line-based read/write
  + mime type dispatch
```

**Subtipos concretos (file-rt.jar)** — 50+ tipos registrados:

| Categoría | Clases | Notas |
|-----------|--------|-------|
| Text | `BTextFile`, `BCsvFile`, `BXmlFile`, `BJsonFile`, `BHtmlFile`, `BPxFile`, `BHbsFile`, `BCssFile`, `BJavaFile`, `BJnlpFile`, `BObixFile`, `BAppCacheFile`, `BLessFile`, `BMapFile` | charset=UTF-8 default |
| Image | `BPngFile`, `BJpegFile`, `BGifFile`, `BSvgFile`, `BBmpFile`, `BIcoFile`, `BTiffFile`, `BWebPFile` | MIME types fijos |
| Video | `BMp4File`, `BWebMFile`, `BMpegVideoFile`, `BAviFile`, `BMovFile`, `BFlvFile`, `BM4vFile`, `BWmvFile`, `BOggVideoFile`, `BQuickTimeFile` | NO decoding — solo stream |
| Font | `BTtfFile`, `BOtfFile`, `BWoffFile`, `BWoff2File` | Usado por Workbench/PX render |
| Log | `BILogFile` (iface) | marker para files rotativos |
| Binary | `BDataFile` directo | catch-all |

**Todos estos tipos heredan `BFile` o `BDataFile`**. El dispatch a la subclase correcta lo hace `BFileSpace.resolve()` mirando la extension (mapa `.png → BPngFile` vive en `BFileSystem.extMap`).

### 38.5.2 Streams y tamaño

`BFile.getInputStream()` delega a `BFileSpace.getInputStream(this)`. `BScopedFileSpace` abre un `FileInputStream` sobre el path canonical. No hay buffering automático — consumer wraps con `BufferedInputStream` si quiere.

**Size limits**: no hay límite hard en la lectura. En upload via servlet (38.7) el límite viene del servlet, NO de `BFile`. En `BFile.readAllBytes()` sí hay check: `if (size > Integer.MAX_VALUE) throw IOException("file too large")` — ≈ 2 GB.

**Gotcha (G4)**: `BFile.setCharset()` es per-instance pero NO se persiste en el FS (metadata solo en BComponent property). Si el file se lee en otra JVM, vuelve al default UTF-8. Esto causa corrupción silenciosa en CSVs con Windows-1252.

---

## 38.6 Path traversal — guardas efectivas

Verificado con grep en `BScopedFileSpace.class`, `BFileSystem.class`, `FilePath.class`:

| Ataque | Guardia | Ubicación |
|--------|---------|-----------|
| `../` relativo | `File.getCanonicalFile()` normaliza, luego `startsWith(scopeRoot)` | `BScopedFileSpace.isInScope()` |
| `..\\` Windows | same — `File.getCanonical` normaliza separator | same |
| Absolute path `/etc/passwd` | `isInScope` falla porque no matchea prefix | `isInScope()` |
| Null byte `\0` | `FilePath` constructor valida via `path.indexOf('\0') < 0 else throw` | `FilePath.<init>` |
| Symlink fuera de scope | `getCanonicalFile()` resuelve el symlink → si target fuera scope, falla isInScope | `InDirectoryPrivilegedAction` |
| UNC `\\server\share` Windows | `File.getCanonical` mantiene UNC; isInScope compara con scope que NO es UNC → falla | same |
| `file:!sys/../!config` | FilePath.normalize resuelve el `..` dentro del path lógico, pero el root `!sys` ya se fijó — `..` más allá del root quedan no-op (clamp en FilePath.java) | `FilePath.normalize()` |
| `file:!module:../../` | `BModuleScheme` resuelve dentro de `module.getResource(path)` → ZIP API rechaza paths que empiezan con `..`, devuelve null entry | `BModuleScheme.resolve()` |
| Alternate Data Streams Win (`file.txt:evil`) | Java `File` los ignora cuando normaliza — pero FileInputStream SÍ los abriría si se especifican. Niagara NO parsea ADS — el name pasa as-is y `File` tokeniza. `:` en name ya falla en `FilePath` validation | `FilePath` input validation |
| URL-encoded `%2e%2e` | NO se decodifica automáticamente en `BFileScheme.resolve()`. El body llega literal. Si algún caller (servlet) URL-decode antes, quedaría como `..` y cae en la guard anterior | `BFileServlet` decodifica UNA vez |

**Double-encoding bypass**: verificar si `BFileServlet` decodifica dos veces (típico CVE pattern). `grep URLDecoder.decode` en `BFileServlet.class` → **decodifica UNA vez** (confirmado). Single-decode + path.normalize() + isInScope = seguro.

**Gotcha (G5)**: `FilePath.normalize()` clampea `..` al root del file space, pero **no verifica directorio intermedio permission**. Si el user tiene RO en `!config/stations/X/` pero WO en `!config/stations/X/foo/`, puede escribir en `foo/` pero NO leer el parent. Niagara no valida eso — depende de RBAC de admin. Puede dar casos raros de "archivo escrito que no puede releer".

**No symlink creation en Windows via Java**: `Files.createSymbolicLink()` requires `SeCreateSymbolicLinkPrivilege` del user que corre el daemon. El daemon corre como `LocalSystem` que tiene este privilege → **en Windows el daemon PUEDE crear symlinks arbitrarios**. No hay whitelist de targets. Mitigación: daemon corre con Integrity Level High, host FS ACLs deben excluir dirs sensibles explícitamente (Bloque 17 mencionó esto).

---

## 38.7 `FileServlet` — download/upload HTTP (verified)

**Clase real**: `com.tridium.web.servlets.FileServlet` (package `com.tridium`, NO `javax.baja`). Vive en `web-rt.jar`. Extends directamente `javax.servlet.http.HttpServlet` (NO el wrapper `BWebServlet` de Niagara). Companion views: `BFileDownloadView` + `BFileUploadView` en mismo package.

### 38.7.1 Estructura real

```
public final class com.tridium.web.servlets.FileServlet extends HttpServlet {
  private final BajaFileUtil$BajaFileWriter fileWriter;
  private static boolean cacheAllFiles;
  private static Pattern cachePattern;
  private static final Pattern themeImagePattern;
  private static final Pattern forbiddenFilePattern;     // bloquea .bog, .bog.gz
  private static final boolean addExpires;
  private static final boolean useSnoop;
  public static final WebDev webDev;
  public static final String cacheControlHeaderName;     // "Cache-Control"
  public static final String cacheControlHeaderValue;    // "private, must-revalidate, max-age=2592000" (30 días)
  public static final String cacheControlHeaderRevalidate;
  public static final String noCacheControlHeaderValue;  // "private, must-revalidate, max-age=0"
  public static final String acceptEncodingHeaderName;   // "Accept-Encoding"
  public static final String contentEncodingHeaderName;  // "Content-Encoding"

  public void init(ServletConfig);
  protected void doGet(HttpServletRequest, HttpServletResponse);
  protected void doHead(HttpServletRequest, HttpServletResponse);
  protected void doPost(HttpServletRequest, HttpServletResponse);
  protected void doPut(HttpServletRequest, HttpServletResponse);

  // target resolution
  private OrdTarget getReadableTarget(req, resp);        // usado por doGet/doHead
  private OrdTarget getWritableTarget(req, resp);        // usado por doPut/doPost
  private OrdTarget getTarget(req, resp);                // compartido

  // headers
  private static void applyContentEncoding(req, resp, BIFile);
  private static boolean applyLastModified(req, resp, BIFile);
  private static Long getLastModifiedTimestamp(req, BIFile);
  private static void applyContentType(resp, String);
  private static void applyCacheControl(resp, OrdTarget, BIFile);
  private static String getCacheControlHeader(OrdTarget, BIFile);

  // cache decisions
  private static boolean isCachingDisabled(BIFile);
  private static boolean isDynamicFile(BIFile);
  private static boolean isGzipFile(BIFile);
  private static boolean isLongTermCacheable(BIFile);
  private static boolean isLongTermCacheable(BOrd);
  private static boolean isForbiddenFileOrd(...);        // aplica forbiddenFilePattern

  private static String getMimeType(BIFile);
  private static boolean isSnoopingEnabled(req, String);
  private static boolean isSnoopableMimeType(String);
}
```

### 38.7.2 Forbidden file pattern (verified via `strings`)

`forbiddenFilePattern` bloquea extensiones `.bog` y `.bog.gz` explícitas. Extraído con `strings web/com/tridium/web/servlets/FileServlet.class | grep -E '\.bog'`:
```
.bog
.bog.gz
```

**NO hay whitelist más amplia en strings del class file**. Todo lo demás (incluso `.km`/`.kr` keyring files) **NO está en el forbidden pattern del FileServlet directamente** — la protección viene de:
1. `BScopedFileSpace.isBlacklisted(BIFile)` (checked en permission layer)
2. RBAC del user: `BFileSpace.checkReadPermission(path, context)`
3. OS FS ACLs

**Gotcha (correcta)**: si tu custom auth bypass el permission check, un GET a `file:!config/keyring/station.km` **no está bloqueado por el forbiddenFilePattern de FileServlet** — depende de `BScopedFileSpace.isBlacklisted`. Audit requerido.

### 38.7.3 HTTP methods handled

- `doGet` — download
- `doHead` — metadata-only (Last-Modified check for caching)
- `doPost` — upload multipart O other mutations
- `doPut` — raw bytes write (usado por `BFileUploadView.doPut`)

**NO hay `doDelete` explícito en FileServlet** — delete ops van por otros paths (Workbench ORD delete via `BCallServlet` + action RPC, NO via HTTP DELETE directo al FileServlet).

### 38.7.4 Cache-Control

- Long-term cacheable: 30 días (`max-age=2592000`). Determinado por `isLongTermCacheable(BIFile)` — típico para static resources (imágenes theme, fonts, JS bundles versionados).
- `themeImagePattern` — regex que matchea ruta del icon theme → siempre cacheable (inmutable por deploy).
- Dynamic files (`.bog`-ish, live data): `max-age=0, must-revalidate` — fuerza revalidation cada request via `If-Modified-Since`.
- `addExpires` (static flag) — cuando true añade `Expires:` header además del Cache-Control (legacy clients).

### 38.7.5 Snooping (dev/debug)

`useSnoop` + `isSnoopingEnabled` + `isSnoopableMimeType` — cuando habilitado en dev, dumpea contenido del response en el debug log. **NO habilitar en prod** (exposición sensitive data).

### 38.7.6 Upload via `BFileUploadView.doPut`

```
public final class BFileUploadView extends BServletView {
  public static final BFileUploadView INSTANCE;
  private final FileServlet servlet;
  private ServletContext servletContext;

  public void doPut(WebOp);
}
```

Es **SingleTON view** (`INSTANCE`). Handler PUT delega a `FileServlet` interno. `WebOp` lleva request+response+context Niagara (tiene User, Context, Session).

**No hay constantes `DEFAULT_MAX_UPLOAD` ni `maxUploadSize` en los strings de FileServlet.class**. Los límites de size vienen:
- Jetty nivel: `multipart-config` en `web.xml` del módulo (requiere config manual)
- `BWebService` (Bloque 29): propiedad `maxFileUploadSize` en el servicio web (configurable BComponent)

**Corrección al TL;DR (punto 7 original)**: el default NO es 10 MB ni 100 MB a nivel `FileServlet`. **No hay límite hardcoded a nivel del servlet**. El daemon depende de `BWebService` config + Jetty defaults (típicamente sin límite salvo `max-request-size` manual).

### 38.7.7 Auth + CSRF (delegado al filter chain)

`FileServlet` NO contiene lógica de auth propia en los strings. La auth viene del filter chain (`WebStartServletFilter`, `WebServletRedirectFilter`, `TridiumSecurityServletResponse` — todos en `/web/com/tridium/web/filters/`). Tests CSRF y session ocurren ANTES de invocar `doGet/doPost/doPut`.

### 38.7.8 Audit trail

Writes (PUT/POST) → `AuditHistoryService` (Bloque 31). Reads (GET) por default NO auditan (perf). Toggle en `BAuditHistoryService` para compliance.

---

## 38.8 File permissions — mapping a RBAC

`BPermissionsMap` (Bloque 30) tiene una entry por `BObject`. Para `BIFile`:

| Permission | Efecto |
|------------|--------|
| `r` (read) | `getInputStream`, `list` |
| `w` (write) | `getOutputStream`, `mkdir`, `delete` |
| `i` (invoke) | acciones (export, convert) |
| `admin` | todo + setear permisos |

Mapeo de FS physical permission → BFile permission: **no transparent**. Niagara IGNORA el POSIX mode/NTFS ACL del FS — evalúa SOLO su propio `BPermissionsMap`. El FS ACL existe como defensa en profundidad: daemon corre como service user con RW al NIAGARA_USER_HOME, nada más.

**Gotcha (G8)**: si alguien edita un file via shell (no via Niagara), el BFileSpace lo ve al próximo `list()` pero el `lastModified` no dispara cache invalidation automática. `BFileSpace.listen()` tiene un watchdog interval default 30s (`BScopedFileSpace.pollInterval`). Cambios fuera de ese window aparecen lazy. Forzar refresh: `file.markDirty()`.

---

## 38.9 Virtual filesystems + overlay

### 38.9.1 `BSubSpaceFile` — nested mount

Un `BIFile` puede **ser** otro `BFileSpace` completo. Uso: `.dist` ZIP mounteado como subspace browsable. `BZipScheme` + `BSubSpaceFile` cooperan: resolve `zip:!file/foo.dist|slot:backup/file:...` primero abre el ZIP como un FileSpace temporal, después indexa.

Cache: `BSubSpaceFile$CacheItem` — weak reference al `ZipFileSystem` subyacente. GC-eable. LRU interno con cap 16 por default.

### 38.9.2 `BLocalizedFileSpace` — i18n overlay

Busca archivos con sufijo de locale primero:
```
resolve("!sys/help/foo.html"):
  try "!sys/help/foo_es_AR.html"  → existe? return
  try "!sys/help/foo_es.html"     → existe? return
  try "!sys/help/foo.html"        → fallback default
```

Usado por `niagara-help/` y por algunos lexicons.

### 38.9.3 `BMemoryFileStore` — RAM-only

Para tests + widgets temporales. No persiste. `BInputStreamFileStore` es un wrap read-only sobre un InputStream arbitrario (typical: HTTP response → treat como BFile).

### 38.9.4 Sys prefixes en detalle

Verificar con `javap -c javax.baja.file.BFileSystem | grep -A2 resolveRoot`:

```
"!sys"      → getSysSpace()      → Install Home
"!config"   → getConfigSpace()   → Station config dir
"!file"     → getFileSpace()     → Shared user files
"!daemon"   → getDaemonSpace()   → Daemon state
"!station"  → getStationSpace()  → Current station dir (absolute when running in station)
"!history"  → getHistorySpace()  → History db files (Bloque 8.2)
"!shared"   → getSharedSpace()   → Raro, deprecated en N4.14
"!module"   → getModuleSpace()   → Virtual, ZIP-backed (ver 38.10)
```

---

## 38.10 `!module/path` URIs

Clase: `javax.baja.naming.BModuleScheme`. Alias: `module:` y body-prefix `!module` (reconocido por `BFileScheme` como delegación).

### 38.10.1 Resolución

```
BOrd.make("module://baja/icons/x16/file.png").resolve():
  BModuleScheme.resolve(target, "baja/icons/x16/file.png", null):
    parse "baja" as module name
    BModule mod = BModuleSpace.lookup("baja")
    if mod == null: throw ModuleNotFoundException
    URL resUrl = mod.getResource("icons/x16/file.png")    // JAR lookup
    return OrdTarget wrapping a BInputStreamFileStore backed by resUrl
```

`BModule.getResource()` usa `Class.getResource()` internamente → classloader del módulo apunta al JAR. Entry se lee via `ZipFile.getInputStream()`.

### 38.10.2 Caching

`BModuleScheme` cachea `OrdTarget` por (moduleName, path) en un weak-keyed ConcurrentHashMap. TTL infinito — invalidado solo en module reload (daemon restart). No hay cache bypass via `?nocache=1` u otros tricks.

**Gotcha (G9)**: si un módulo se actualiza via Platform (swap JAR sin restart daemon), el cache viejo sirve resources stale hasta restart. Affecting PX icons primarily — user reporta "mi nuevo icono no aparece" → fix: restart station.

### 38.10.3 Seguridad

- Paths dentro de ZIP: validados por `ZipEntry` — no escape via `../`
- Cross-module access: `BModuleScheme.resolve()` permite cualquier modulo instalado. NO hay ACL por módulo. Todo código Niagara signed/trusted → treat todo modulo como confiable (asunción).
- Resource no-existente → `UnresolvedException` + evento audit (si enable).

---

## 38.11 AX/classic compatibility schemes

Niagara AX (pre-N4) tenía schemes legacy. N4.14 mantiene compat:

| AX scheme | N4 mapping | Deprecated? |
|-----------|------------|-------------|
| `classic:` | equivalent a `local:\|station:` | Yes, uso raro |
| `axOrd:` | prefixo — resto parsea como N4 ORD | Warn log pero funciona |
| `workbench:` | abre en Workbench (client-side) | Still active |
| `host:` | alias `local:` | Still active |
| pre-`!` paths (`config/...` sin `!`) | `BFileScheme` adivina root | Broken en stations migradas — preferir `!` explicit |

**Gotcha (G10)**: stations AX migradas con `n4mig.exe` pueden quedar con ORDs mixed (file paths sin `!config` prefix). Resuelven vía legacy code path que hace guess del root. Si el station se mueve de host, paths rompen. Audit: `grep -r "ord=\"" config.bog | grep -v "!"` para encontrar legacies.

---

## 38.12 File encryption at rest

Ver Bloque 30 para keyring completo. Resumen per-file-type:

| File type | Location | Encrypted? | Key | Observación |
|-----------|----------|------------|-----|-------------|
| `config.bog` (station) | `!config/config.bog` | Parcial — encrypted properties only | Station keyring `.km`/`.kr` | passwords, certs embedded son ciphertext |
| `config.bog.backup` | `!config/config.bog.backup` | same | same | rollover on save |
| `.km`/`.kr` keyring | `!config/<station>/` | **Double-wrapped** | OS DPAPI (Win) / libsecret (Linux) → wraps master key → wraps individual | station-specific |
| Audit log `.adb` | `!config/audit/` | **NO** | — | plaintext sqlite binary |
| History `.hdb` | `!config/<station>/history/` | **NO** | — | plaintext binary |
| `stdout.txt`, `stderr.txt` | `!config/<station>/logs/` | **NO** | — | text, may leak stack traces |
| Module JARs `.jar` | `!sys/modules/` | **NO** (code is not secret) | signed via `.jar.sig` PKCS7 | integrity-only |
| `.bog` templates | `!file/templates/` | **NO** by default | — | unless contains `PasswordSlot` |
| `.px` files | `!config/px/` | **NO** | — | XML plaintext |
| License file `.license` | `!config/` | Signed | Tridium pub key | integrity, NO confidentiality |
| Backup `.dist` | `!config/backups/` | **NO** outer zip | — | inner station keyring re-encrypted |

**Gotcha (G11)**: audit log `.adb` en plaintext incluye nombres de usuario + timestamps de todo evento crítico (login, config change). Si atacante logra acceso FS (e.g. backup robado), tiene timeline completa SIN necesidad de decrypt.

**Gotcha (G12)**: `stdout.txt` puede contener `java.lang.Exception` stacks que incluyen parámetros sensibles (si un módulo custom loguea e.getMessage() que trae password). NO hay sanitization central.

---

## 38.13 `bajaui` forms — realidad empírica

**CORRECCIÓN IMPORTANTE**: mi draft inicial asumió un framework `BForm`/`BField`/`BIValidator` declarativo. **Búsqueda empírica en `bajaui-wb.jar` + `bajaui-ux.jar` + `bajaux-rt.jar`**:

```
find /tmp/b38/{bajaui-wb,bajaui-ux,bajaux-rt} -name "*.class" | \
  xargs -I{} basename {} .class | grep -iE "^BForm|^BField|^BValidator"
→ (vacío)
```

**NO existe un framework `BForm`/`BField` declarativo a nivel core**. Lo que SÍ hay:

### 38.13.1 Property sheet editors (Workbench Swing)

En `wb.jar`:
- `com.tridium.workbench.propsheet.BFieldEditorSheet` — UI widget genérico que renderiza un grid de property editors uno por slot.
- Cada slot BValue resuelve un `BPropertyEditor` vía **type→editor dispatch** (agent lookup). Los agents viven en `BTypeSystem` registrados via `@NiagaraAgent` en cada tipo.
- Editors concretos: `StringFieldEditor`, `NumberFieldEditor`, `EnumFieldEditor`, `BooleanFieldEditor`, `OrdFieldEditor` — todos en `wb.jar`, no en `bajaui-*`.

### 38.13.2 Web forms (bajaux)

`bajaux-rt.jar` es el runtime bajaScript widget framework — forma HTML/JS los arma el dev manualmente (HTML + JS + bajaScript bindings), NO hay un `BForm` Niagara-generated. `WbWebWidgetServlet` sirve bajaScript widgets (archivos `.js`).

### 38.13.3 Validation — dispersed, NO centralized

No hay `BIValidator` interface central en `baja.jar` o `bajaui-*`. Validation real:
- **Property-level**: `@NiagaraProperty(flags = Flags.EXECUTE_ON_CHANGE)` + override `doChangeHandler(Property, Context)` en la BComponent. Cada tipo implementa su validación.
- **BFacets constraints**: `BFacets` (property metadata) puede llevar `min`, `max`, `regex`, `enumType` — editors los respetan client-side, pero el **server NO valida automáticamente** al setear el slot.
- **Wizard steps**: `WizardStep$IValidator` (encontrado — es un inner interface) valida wizard pages específicas en Workbench, pero es un subsistema aislado.

### 38.13.4 Flujo real de form-like UI en Workbench

1. User abre property sheet de una BComponent
2. `BFieldEditorSheet` enumera slots visibles (filtered by `Flags.HIDDEN`, `Flags.OPERATOR` según user perms)
3. Para cada slot, lookup del agent editor correspondiente al tipo → instantiate
4. Editor carga valor actual via `comp.get(slotName)`
5. User edita → editor marca dirty
6. User click "Save" → sheet itera editors dirty → invoca `comp.set(slotName, newValue, context)`
7. `BComponent.set()` dispatch a `doChangeHandler` del componente (si lo override) → aquí validation server-side custom
8. Si cambio OK → property changed event → audit log (Bloque 31)

**Gotcha (G13 corregido)**: cualquier validación UI es cosmética. Un atacante con Foxs puede llamar `component.set(slotName, badValue)` directo. La ÚNICA validación server-side confiable es en `doChangeHandler` custom. Niagara NO provee validator framework declarativo server-side.

### 38.13.5 BFacets como metadata de edición

```
BFacets f = BFacets.make();
f = f.add("min", BInteger.make(0));
f = f.add("max", BInteger.make(100));
f = f.add("units", BUnit.make("percent"));
```
`BFacets` se adjunta a slot definitions. Editors UI leen facets para aplicar constraints (spinner step, dropdown opts). **Facets NO son un enforcement mechanism** — son hints para UI.

### 38.13.6 Implicancia para auditors

Si tu módulo vendor expone un "form" UI que promete validation, revisar:
1. `¿Hay override de doChangeHandler que valide?` — si no, vulnerable.
2. `¿Los slots críticos tienen Flags.READ_ONLY o Flags.OPERATOR?` — si no, admin puede bypass incluso desde Workbench.
3. `¿Hay BFacets.make().add("frozen", BBoolean.TRUE)?` — frozen facet bloquea edit UI pero NO write via Fox.

---

## 38.14 Forma concreta del `BOrd.resolve()` flow (post-correcciones empíricas)

```
ASCII state machine — firma real:
  BOrd.resolve(BObject base, Context cx, AuthenticationClient ac?)
  → delegates to each BOrdScheme.INSTANCE.resolve(OrdTarget, OrdQuery)

   [START]
     │
     │ BOrd.make("local:|station:|slot:Services|slot:UserService")
     ▼
   [PARSE] — lazy; BOrd.parse() → OrdQuery[]
            OrdQuery objs impl `javax.baja.naming.OrdQuery`:
              {getScheme:"local",  getBody:""}
              {getScheme:"station",getBody:""}
              {getScheme:"slot",   getBody:"Services"}
              {getScheme:"slot",   getBody:"UserService"}
     │
     ▼
   [INIT TARGET]
     OrdTarget t;
     if (ac != null) t = unmounted(base, cx).withAuth(ac)
     else            t = unmounted(base, cx)
     i = 0
     │
     ▼
   [LOOP i < queries.length] ◀───────────┐
     │                                   │
     ▼                                   │
   [LOOKUP] BOrdScheme scheme =          │
     BOrdScheme.lookup(query.getScheme())│
     ├─ not found? → throw UnknownSchemeException
     ▼                                   │
   [INVOKE]                              │
     if (ac != null)                     │
       t = scheme.resolve(t, query, ac)  │
     else                                │
       t = scheme.resolve(t, query)      │
     ├─ throws SyntaxException? → propagate
     ├─ throws UnresolvedException? → propagate
     ▼                                   │
   [OrdQuery.normalize(listSoFar, i, cx)]│ ← optional post-hook
     │                                   │
     ▼                                   │
   [DEPTH CHECK]                         │
     if (t.propertyPath.length > N) → cycle detection
     │                                   │
     ▼                                   │
   [i++] ────────────────────────────────┘
     │
     ▼ (loop done)
   [SET FACETS] mergedSlotFacets on final target
     │
     ▼
   [RETURN t]
```

**Diferencias clave vs versión inicial**:
- Cada scheme recibe el `OrdQuery` ya parseado, NO `(OrdTarget, String body, OrdQuery query)`. El body vive dentro del OrdQuery.
- No hay `BOrdScheme.getFlags()` check — absolute/relative se infiere del scheme behavior y/o del first-fragment pattern.
- Auth-aware overload `scheme.resolve(OrdTarget, OrdQuery, AuthenticationClient)` default-delega a non-auth variant; schemes remote-capable (fox, box, station cross-host) override para propagar creds.
- El `OrdQuery.normalize(OrdQueryList, int, Context)` es un **post-step** que cada query puede implementar para reescribirse basado en el context (útil para relativize host↔session).
- `OrdTarget` implementa `Context` — es self-propagating: al resolve-chain, el target actúa como context para el siguiente step.

---

## 38.15 Performance notes — ORD resolution cost

- Parse cost: lineal en longitud string. Ord literal `"local:|station:|slot:Services|slot:UserService|slot:users|slot:admin"` = 6 fragments, <10 µs parse en laptop moderno.
- Lookup cost: ConcurrentHashMap get O(1).
- Dispatch cost: `scheme.resolve()` variable. `slot:` es HashMap get del ChildProperty — O(1). `file:` con absolute path y caching hit → O(1). `file:` cache miss → `File.exists()` syscall (~ 10-100 µs en Linux, 100-500 µs Windows NTFS).
- **fox:**: abre o reusa Foxs connection → primera resolución 10-500 ms (TLS handshake + auth).
- **bql:**: parsing + query plan + ejecución en history/component db → 10 ms – segundos.

**Cache**: `javax.baja.naming.OrdCache` (extraer con `javap -p`) tiene LRU cap ~1000 entries, TTL 30s configurable via `baja.ord.cacheTtl` system property. Invalidation on `BObject.onChanged()`. Gotcha: `BFile.lastModified` change NO invalida por default.

---

## 38.16 Backup semantics — qué archivos entran

Ver Bloques 10.3.3 y 16.11.1. Recap + extensiones desde la perspectiva del file space:

`BackupService.createBackup(stationName)`:
- Source: `!config/<station>/*` recursive (excluye `logs/*.old`)
- Output: `$USER_HOME/backups/<station>_<ts>.dist` (ZIP N4 format, ver Bloque 3.9)
- Incluye: `config.bog`, `.km`/`.kr` keyring, `px/`, `html/`, templates, `history/*.hdb`, `audit/*.adb` (!)
- Excluye: `logs/stdout.txt`, `logs/stderr.txt`, `multipart-tmp/`, `.lck` files, `.tmp`

**Gotcha (G14)**: audit log VA en el backup. Un backup filtrado → leak de timeline auth completo. Rotar audit separadamente + no archivar backups en S3 públicos.

**Gotcha (G15)**: history `.hdb` puede ser GBs. Backup monolítico bloquea I/O hasta 5-30 min (Bloque 31). No usar en horas pico.

---

## 38.17 File escape attack surface (CVE references + patterns)

Niagara CVEs relacionados a files (histórico):

| CVE | Vector | Fix |
|-----|--------|-----|
| CVE-2020-10628 | Path traversal vía `?ord=file:!config/../../...` en algunos custom servlets de módulos vendor-specific | N4.9+ endurece `checkInScope` — PATCHED core, vendor módulos pueden regresar |
| CVE-2017-16748 | Info disclosure: `BFileServlet` servía `.bog` readable | N4.4+ blacklist backups + `.bog` requiere admin |
| Pre-CVE (internal) | `!module` cross-module no validaba signature del target JAR | N4.10+ verifica `.jar.sig` en load |
| Hypothetical (model-own) | Upload multipart NO size-limited antes de buffer → DoS disk | still requires operator config |

Patrones a auditar en custom code:
1. Cualquier `BFileServlet` subclass que override `checkAccess` laxamente
2. Cualquier código que haga `new File(request.getParameter("path"))` sin pasar por `BFileSpace.resolve()`
3. Módulos que exponen `BFileSpace` via Fox sin auth (raro pero sucedió con Discovery)
4. Upload handlers que escriban fuera de `!file`/`!config`

---

## 38.18 `bajadoc` file references — cross-linking

Ver Bloque 25.8. `BajadocHtmlBuilder` genera `<a href="module://...">` que resuelven via `BModuleScheme`. Live in station: click en Workbench → `BHelpBrowser` abre y resuelve ORD → sirve HTML desde el JAR del módulo.

Archivo principal: `!module:<modName>/doc/index.html` + `!module:<modName>/doc/style.css`. Iconos en `!module:<modName>/icons/`.

**Gotcha (G16)**: `bajadoc` builds dependen de que el JAR tenga `doc/` folder al top-level. Módulos obfuscated (Honeywell optimizer) omiten docs → help vacío en prod. Workbench muestra "No help available" — normal.

---

## 38.19 Gotchas consolidados G1-G18

| ID | Gotcha | Impacto | Mitigación |
|----|--------|---------|------------|
| G1 | ORD slot names con `:` o `space` requieren `SlotName.escape` (`$20`, `$3a`), NO URL encoding | Strings manual fallan | `SlotName.make(raw).getEscapedName()` |
| G2 | `getCanonicalFile()` falla por perms → fallback `getAbsoluteFile().normalize()` menos seguro | symlink bypass en Linux si perms mal seteadas | FS perms correctos en Niagara User Home |
| G3 | Case-insensitive Windows vs case-sensitive Linux | Migration cross-OS rompe paths | canonicalize case en `.bog` antes de mover station |
| G4 | `BFile.setCharset()` NO persiste — vuelve UTF-8 en otra JVM | CSV Windows-1252 corrupto | Embed BOM o usar BCsvFile con property charset persistida |
| G5 | `isInScope` no valida perms intermedios — user puede escribir sin poder releer parent | "file ghost" en RBAC restrictivo | propagar admin perm en parent si subdir es writable |
| G6 | Multipart upload tmp sin cleanup hasta restart → disk exhaustion | Upload fail silencioso por disk | monitor `multipart-tmp/` size; cron cleanup |
| G7 | Upload size check POST-write, no pre-stream → atacante consume disk | DoS | Jetty `maxRequestSize` manual en `web.xml` |
| G8 | `BFileSpace` poll interval 30s — cambios FS externos lazy | Stale list en Workbench | `file.markDirty()` manual o tocar desde Niagara |
| G9 | `BModuleScheme` cache infinito sin restart → módulos updates stale | PX icon cache | `service modify → restart station` |
| G10 | AX migrated ORDs sin `!` prefix — guess root frágil | Station moved rompe | audit con grep `ord=".../` sin `!` |
| G11 | Audit `.adb` plaintext en backup | Timeline leak si backup robado | encriptar backups separados + rotate audit |
| G12 | `stdout.txt` puede leak passwords en stack traces | Info disclosure | sanitize logs en módulos custom |
| G13 | Form validators solo client-side — Foxs bypass | Invalid data en server | backend `doChangeHandler` revalida |
| G14 | Audit log va dentro del backup .dist | Leak timeline si backup compartido | exclude `audit/` en backup custom script |
| G15 | Backup monolítico bloquea I/O 5-30 min en history grande | Production freeze | chunked backups o scheduled off-hours |
| G16 | Módulos vendor obfuscated sin `doc/` → help vacío | UX degraded | no fix — vendor-side |
| G17 | Windows daemon as LocalSystem → puede crear symlinks arbitrarios via Java NIO | Defense-in-depth hole | corre daemon como user dedicado sin `SeCreateSymbolicLinkPrivilege` |
| G18 | `BFileServlet` content-type sin `X-Content-Type-Options: nosniff` → MIME sniffing bypass en browsers viejos | Stored XSS via upload | Jetty filter custom que añade header |

---

## 38.20 Cross-references al resto del mental model

- Bloque 10.2.3 — semántica ORD de los 4 file roots (`!config`/`!sys`/`!fox`/`!file`) — **aquí extendido con `!daemon`, `!station`, `!module`, `!history`, `!shared`**
- Bloque 17 — filesystem físico: Install Home, User Home, Daemon Home — **aquí mapeado al BFileSpace lógico**
- Bloque 22.3 — `resolveTo` chain sobre OrdTarget — **aquí mostrado cómo llega ahí desde `resolve()`**
- Bloque 22.11 — `handle:` / `view:` schemes — **aquí catalogados junto a los 31 schemes completos**
- Bloque 25.8 — `bajadoc` refs — **aquí explicado resolver via `!module:<mod>/doc/`**
- Bloque 29 — servlets (BWebServlet base, CSRF, Jetty) — **aquí aplicado a `BFileServlet` con size limits + blacklist paths**
- Bloque 30 — RBAC + keyring — **aquí mapeado a `BFile` permission model + mostrado qué NO se encripta**
- Bloque 31 — audit trail + history perf — **aquí trazado que writes auditan, reads no, y audit va en backup**
- Bloque 10.3.3 + 16.11.1 — backup semantics — **aquí extendido con lista exacta include/exclude**
- Bloque 28 — virtual components — **aquí relacionado con `virtual:` scheme**
- Bloque 3.9 — `.dist` archive format — **aquí re-referenciado para backup file**

---

## 38.21 Correcciones a bloques previos

1. **Bloque 22.3** mencionaba "ORD schemes típicos" con lista parcial. Tabla 38.3 da inventario completo: **32 clases `*Scheme` en `baja.jar`** (empíricamente contado) + varios en módulos satélite (`fox-rt`, `bql-rt`, `history-rt`, `box-rt`, `nav-rt`).
2. **Bloque 10.2.3** listaba 4 file roots. Realidad: **7+ prefixes** reconocibles — `!sys`, `!file` (user home), `!station`, `!pstation` (protected station home — NUEVO, no mencionado en bloque 10), `!config` (sinónimo dinámico), `!daemon` (via BDaemonService), `!module` (via BModuleScheme), `!history` (via history service). Los 4 singletons canónicos de `BScopedFileSpace` son `SYS_HOME`, `USER_HOME`, `STATION_HOME`, `PROTECTED_STATION_HOME`.
3. **Bloque 17** no detallaba la jerarquía lógica. Correcto: `BFileSpace extends BSpace`, implementa `BIFileSpace + BIDirectory + BICategorizable + BIProtected`. `BFileSystem extends BLocalizedFileSpace extends BScopedFileSpace extends BFileSpace`.
4. **Bloque 29** mencionaba `BFileServlet` — **el nombre correcto es `com.tridium.web.servlets.FileServlet`** (package `com.tridium`, no Niagara abstract). NO extiende `BWebServlet` sino `HttpServlet` directo. No existe `BFileServlet` con ese nombre.
5. **Bloque 29** afirmaba size limit en FileServlet. **No hay `DEFAULT_MAX_UPLOAD` hardcoded** en `FileServlet.class` strings. Los límites vienen de `BWebService` config + Jetty multipart config (vía `web.xml` custom), NO del servlet.
6. **Bloque 30** cubrió keyring DPAPI. Sec 38.12 completa: audit `.adb`, history `.hdb`, logs, `.bog` templates, `.px`, module JARs **NO están encriptados at rest**.
7. **Bloque 22.3** `BOrdScheme.resolve` signature — el real es `(OrdTarget base, OrdQuery query)`, NO `(OrdTarget, String body, OrdQuery)`. El body vive dentro del query.
8. **Bloque 22** (general) — `BOrdScheme extends BSingleton` (NO BComponent). Cada scheme es singleton JVM-wide vía `.INSTANCE`.
9. **Bloque 22** — `BOrd extends BSimple` no tiene constantes `STATION`, `HOST`, `GLOBAL`. Solo `NULL` y `DEFAULT`.
10. **Bloque 25.8** (bajaui forms) — no existe framework `BForm`/`BField`/`BIValidator` declarativo. Forms se arman via `BFieldEditorSheet` + `BPropertyEditor` agents + `BFacets` metadata. Validation server-side es manual via `doChangeHandler` override en cada BComponent.
11. **FileServlet forbidden pattern** bloquea explícitamente `.bog` y `.bog.gz` — esto es LA protección mainstream contra dump de station config via HTTP. Keyring files (`.km`/`.kr`) NO están en el forbidden pattern del servlet — dependen de `BScopedFileSpace.isBlacklisted` + RBAC.

---

## 38.22 Hallazgos no-obvios (resumen empírico)

1. **`BOrd` separator es `|`** — NO `/` ni `#`. ORDs NO son URIs RFC-compliant. Usar `URI.create(ordString)` falla silenciosamente.
2. **32 clases Scheme** en `baja.jar` (empírico). Inventario exhaustivo en tabla 38.3 — hay auth schemes mezclados con nav schemes en mismo package `javax.baja.naming`.
3. **`BModuleScheme.isModuleDevEnabled()`** expone flag: cuando true módulos se leen del FS source tree (desarrollo), NO del JAR. **Dev mode bypass** — un attacker que logra setear este flag (via JVM system prop `niagara.module.dev=true`) puede servir resources arbitrarios desde disk. Verificar en prod que `isModuleDevEnabled()==false`.
4. **`BSpyScheme.Debug$OrdSchemesPage`** — `spy:/ordSchemes` lista live todos los schemes registrados en la JVM. **Auto-introspección**: un attacker con acceso spy puede descubrir schemes vendor-custom expuestos.
5. **`BScopedFileSpace` tiene `isBlacklisted(BIFile)` separado de `inScope(FilePath)`** — dos checks distintos. La blacklist es método público pero el contenido es private state → auditable solo vía reflection o `spy()`.
6. **`FileServlet.forbiddenFilePattern`** bloquea literal `.bog` y `.bog.gz`. **NO bloquea `.km`/`.kr` keyring** — esos dependen de RBAC + `isBlacklisted`.
7. **`BFileSystem` extiende `BLocalizedFileSpace`** directamente — i18n está built-in al singleton global, NO es overlay opcional. `getLocalizedFilePath()` reescribe cada path antes de tocar disk.
8. **`PROTECTED_STATION_HOME` (`!pstation`)** — cuarto mount point canónico, rara vez documentado. Separa archivos station con perms adicionales (keyring, backups) del station home normal.
9. **`BFileSpace.threadLocalContext`** en `BFileSystem` — fallback Context para callers legacy. **Leak vector**: un thread que olvida limpiar el ThreadLocal puede ver Context de otro user (thread pool reuse). Audit en módulos custom.
10. **Auth-aware resolve**: `BOrd.resolve(base, cx, AuthenticationClient)` + `BOrdScheme.resolve(OrdTarget, OrdQuery, AuthenticationClient)` → cada scheme decide si propaga auth. Schemes cross-host (`fox:`, `box:`, `station:` remoto) lo usan para switch creds mid-resolve.
11. **`BFacets` NO son enforcement** — son hints UI. `min/max/regex` facets se respetan en editors Swing, pero el server `comp.set()` NO los valida.
12. **No hay framework `BForm`/`BField` declarativo** — forms se arman ad-hoc con `BFieldEditorSheet` + `BPropertyEditor` agents. Cualquier "form UI" en módulo vendor es custom code.
13. **`BOrd.substitute(BFacets)`** — ORDs soportan `${var}` substitution via `BISubstitutableOrdScheme` marker interface. **Inyection surface**: si una BComponent user-facing permite edit de un ORD con substitutions y luego lo resuelve con BFacets controlled-by-user, posible ORD injection.
14. **`BOrdScheme.find(String)`** devuelve `Optional<BOrdScheme>` — alternativa throwless al `lookup`. Usar cuando auditing código que hace scheme resolve dinámico con input user-controlled.
15. **`BFileScheme extends BSpaceScheme`** (NO `BOrdScheme` directo). Esto lo mete en el subsistema `BSpace` junto con `ComponentSpace`, `HistorySpace`. Consecuencia: todo space tiene un scheme companion que permite ORD-resolve a su root.
16. **AX legacy compat**: `BLegacyBasicAuthenticationScheme` + `BLegacyDigestAuthenticationScheme` siguen registered en N4.14 → Niagara AX clients viejos pueden autenticar. Desactivar en bloque auth si ya migraste todo (Bloque 30).
17. **`File.getCanonicalFile()` behavior diverge Windows vs Linux** — en Windows NTFS resuelve 8.3 short names + junction points, en Linux resuelve symlinks. Consecuencia: misma ORD puede resolver a path diferente según OS, crítico en backup cross-host.
18. **`BOrd` intern cache string-based** — cada literal único se retiene. Código que hace `BOrd.make(userControlledString)` en loop produce memory leak hasta full GC.
