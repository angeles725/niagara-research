# BLOQUE 1 — Estructura del framework Niagara N4

Fecha: 2026-04-20
Fuente primaria: `niagara-help/devguide-clean/{modules,registry,station,execution}.txt`
Fuente empírica: `module-navigator` (926 JARs, 51,167 clases Vineflower) + module.xml reales de httpClient-rt y fox-rt
Validado contra: corpus de OptimizerSupervisor-N4.14.0.162

---

## 1.1 Módulos: la unidad fundamental de deployment

Un **módulo Niagara** es un JAR (PKZIP-compliant) con un manifiesto `META-INF/module.xml`. Es la unidad de deployment, de versionado, y de dependencia. El framework NO trabaja con JARs sueltos; trabaja con módulos.

### Regla del profile único por JAR
Un archivo módulo tiene contenido para **UN solo runtime profile**. Si un módulo lógico tiene componentes runtime + UI web + UI workbench, se parte en 3 JARs distintos (`httpClient-rt`, `httpClient-ux`, `httpClient-wb`). Cada uno es un "module part" del mismo módulo lógico.

### Nombre y parts
- **module name**: nombre lógico global (ej. `httpClient`). 1-25 ASCII chars. Debe ser único en el ecosistema.
- **module part name**: por default es `{moduleName}-{runtimeProfile}` (ej. `httpClient-rt`). Es el identificador concreto del JAR.
- **moduleParts**: el part con el profile MÁS BAJO (rt < ux < wb < se < doc) es el "master" y lista los hermanos en `<moduleParts>`. El resto NO puede listar parts.

---

## 1.2 Los 5 runtime profiles (jerarquía estricta)

| Profile | JRE requerido | Contenido típico | Ejemplo |
|---------|---------------|------------------|---------|
| **rt** | Java 8 Compact 3 | Data model + comm (Fox, Box, Web Servlets) | `control-rt`, `httpClient-rt` |
| **ux** | Java 8 Compact 3 | BajaUX: HTML5/CSS/JS para web UI | `control-ux` |
| **wb** | Java 8 SE | Código Workbench viejo (AWT/Swing, views, field editors) | `control-wb` |
| **se** | Java 8 SE | Dependencia directa de Java SE (DB, AWT, Swing) | `control-se` |
| **doc** | N/A | Documentación pura. Sin classes. | `control-doc` |

### Dependency matrix (crítica)
Una regla de hierro: un módulo **solo puede depender hacia abajo** en la pirámide.

| Declaring ↓ / Target → | rt | ux | wb | se | doc |
|------------------------|:--:|:--:|:--:|:--:|:---:|
| **rt** | ✓ | ✗ | ✗ | ✗ | ✗ |
| **ux** | ✓ | ✓ | ✗ | ✗ | ✗ |
| **wb** | ✓ | ✓ | ✓ | ✗ | ✗ |
| **se** | ✓ | ✓ | ✓ | ✓ | ✗ |
| **doc** | ✗ | ✗ | ✗ | ✗ | ✗ |

**Por qué importa**: si tenés un servicio que corre en el JACE (un embedded device con JVM restricta), TIENE que ser `-rt`. Si depende de una clase `-wb`, el JACE no puede cargar el módulo porque su JRE Compact 3 no tiene AWT/Swing.

### Cifras empíricas del corpus Honeywell OptimizerSupervisor (module-navigator inventory)
- **Total submódulos (JARs)**: 926
- **Módulos lógicos únicos**: 661
- **Breakdown**: 500 rt, 197 wb, 103 ux, 99 doc, 5 se, 22 standalone
- **Bytecode**: 532 v52 (Java 8) + 10 v53 (Java 9). La mayoría cumple Compact 3.

La mayoría absoluta es **rt** (runtime). Esto tiene sentido: es el nivel más bajo, lo que corre en cualquier deployment.

---

## 1.3 Schema oficial de `module.xml`

Ejemplo real del corpus (`httpClient-rt`):

```xml
<module name="httpClient-rt"
        bajaVersion="0"
        vendor="Tridium"
        vendorVersion="4.14.0.162"
        description="Http Client"
        preferredSymbol="httpc"
        nre="true"
        autoload="true"
        installable="true"
        buildMillis="1718363199234"
        buildHost="ee033fd13409"
        moduleName="httpClient"
        runtimeProfile="rt"
        releaseDate="2024-05-28">
  <dependencies> ... </dependencies>
  <dirs/>
  <installation/>
  <types> ... </types>
  <permissions> ... </permissions>
  <moduleParts> ... </moduleParts>
</module>
```

### Atributos root
- **Obligatorios**: `name`, `vendor`, `vendorVersion`, `description`, `preferredSymbol`, `runtimeProfile`
- **Opcionales comunes**: `modulePartName`, `nre` (requiere NRE), `autoload` (se carga al boot sin import explícito), `installable`
- **Audit**: `buildMillis`, `buildHost`, `releaseDate` — trazabilidad

### Elementos internos
| Elemento | Función | Veces |
|----------|---------|-------|
| `<dependencies>` | Lista de `<dependency>` con `name`+`vendor`+`vendorVersion`. El framework resuelve antes de cargar el módulo | 0 ó 1 |
| `<dirs>` | Lista de `<dir>` con paths relativos a system-home que contiene el módulo | obligatorio |
| `<types>` | Mapa `baja-type-name → java-class-FQN`. Sin esto, un BComponent no aparece en el registry | 0 ó 1 |
| `<defs>` | Pares `name="..." value="..."` — mapa global colapsado por el registry. Convención: `{moduleName}.*` | 0 ó 1 |
| `<lexicons>` | Locale resources. Atributo `brand="*"` filtra por brand (Tridium, Honeywell, etc.) | 0 ó 1 |
| `<permissions>` | Declaración de Java permissions requeridos (station/workbench/all) | 0 ó 1 |
| `<moduleParts>` | Solo en el master, apunta a los JARs hermanos | 0 ó 1 |
| `<installation>` | Dependencias de deployment (ej. NRE version específica) | 0 ó 1 |

### Versionado — el formato Tridium
`major.minor.iteration.build[.patch]` — ej. `4.14.0.162` = Niagara 4.14, iteration 0, build 162. `vendor` (Tridium, Honeywell, terceros) es **case-insensitive** y crítico para licensing (el feature resolving usa `checkFeature(vendor, featureName)`).

### Quién genera module.xml
El plugin Gradle `niagara-module` (parte de NCoDE / N4 build) genera `module.xml` a partir de:
- `module-include.xml` (source, editado por dev)
- `module-permissions.xml` (source, declara permission groups)
- Scan de classes con annotations `@NiagaraType`, `@NiagaraProperty`, etc.

El dev edita source; Gradle produce el XML final embeddable en el JAR.

---

## 1.4 Station, Workbench, NRE — los 3 procesos

### Station
- **Qué es**: la unidad principal de server processing en Niagara. Una station = un proceso Java.
- **Database**: **un** archivo `config.bog` (BOG = "Baja Object Graph", XML serializado dentro de un ZIP) ubicado en `file:~stations/{name}/config.bog`.
- **Relación con host**: típicamente 1 station por host (JACE embedded o Supervisor). Es posible correr >1 station en un host si usan puertos distintos.
- **Boot** (`station.txt`):
  1. **Load** — deserializa `config.bog` a memoria como `BStation`. Mountea en ORD namespace `local:|station:`.
  2. **Service Registration** — framework registra todo lo que implementa `BIService`. Después se resuelve con `Sys.getService()`.
  3. **Service Initialization** — `serviceStarted()` callback en cada service. Permite init después del registro pero antes que se inicien componentes generales.
  4. **Component Start** — arranca TODO el árbol bajo `local:|station:` con `BComponent.start()`. Cascade de `started()` + `descendentsStarted()`.
  5. **Station Started** — callback `stationStarted()` en cada componente. **Regla**: external comm (sockets, drivers) debe esperar hasta acá.
  6. **Steady State** — timer (default pocos segs, configurable con `nre.steadystate` property) que dispara `atSteadyState()`. Control algorithms deben esperar acá antes de enviar comandos.
- **Shutdown** (inverso): Save (serializa memoria a config.bog) → Component Stop → Service Stop.

### Workbench
- **Qué es**: la GUI IDE de Niagara (Java Swing desktop app). Es un PROCESO DISTINTO a la station.
- **Se conecta a la station** por Fox protocol (remoto) o abre config.bog local.
- **Módulos `-wb`**: los que usa Workbench (views, field editors, property sheets). Compilados para Java SE (pueden usar AWT/Swing).

### NRE (Niagara Runtime Environment)
- **Qué es**: el runtime que bootstrappea la JVM con el classpath de Niagara, la security policy firmada, y el sistema de módulos. Una capa debajo de la station.
- **El atributo `nre="true"` en module.xml**: indica que el módulo requiere el NRE para correr (la mayoría de los `-rt` lo tienen).
- **Install dependency** (del fox-rt): `<nre name="nre-core-*" version="4.14.0.162" desc="NRE core" solvers="commissioning"/>`

Jerarquía mental:
```
Host (máquina física)
  └── NRE (bootstrap, security, classpath)
        └── Station VM (proceso JVM único)
              └── BStation (root component)
                    └── Services + Component tree
```

Workbench corre en su propia JVM separada, típicamente en una máquina de desarrollo/operación, y se conecta a las stations vía Fox.

---

## 1.5 sys.registry — el índice de types

**Qué es**: una "small database" construida por el Niagara runtime cuando detecta que un módulo se agregó/cambió/removió. Indexa:
- Módulos instalados (sin abrir cada JAR)
- Jerarquía de classes (sin cargar classes reales en JVM)
- Agents registrados en cada type
- Mapeo `file extension → BIFile Type`
- Mapeo `ord scheme → BOrdScheme Type`
- Defs globales (name/value pairs)
- Lexicons por módulo

**API runtime**: `Sys.getRegistry()` → `Registry`. Usa wrappers livianos:
- `ModuleInfo` (wrapper de `BModule`)
- `TypeInfo` (wrapper de `Type`)

**Por qué es eficiente**: el registry se construye una vez (on module install/change) y queda cacheado. Consultar `Sys.getRegistry().getType("control:NumericPoint")` NO carga la clase Java — devuelve un `TypeInfo` metadata-only. La carga real se dispara cuando instanciás (`TypeInfo.getType().newInstance()`).

**Trigger del log**: el mensaje `"up-to-date"` en el log significa que el registry detectó que ningún módulo cambió desde el último boot y usa el índice cacheado. `"Loaded"` significa que re-escaneó. Un bounce de desarrollo típicamente muestra "Loaded"; un reboot limpio sin cambios muestra "up-to-date".

**Agents**: special `BObject` types que proveen servicios para OTROS types — late binding. Ejemplos: views, popup menus, exporters. Un módulo los registra en su `<types>`:
```xml
<type name="PropertySheet" class="com.tridium.workbench.propsheet.BPropertySheet">
  <agent requiredPermissions="r"><on type="baja:Component"/></agent>
</type>
```
Esto dice: "BPropertySheet es un agent para baja:Component, requiere permission `r` (read)".

**Debug**: el registry tiene spy pages (navegables desde Workbench o HTTP spy UI). La clase real es `com.tridium.sys.registry.NRegistry` — decompilable en el corpus.

---

## 1.6 Fox protocol

**Qué es**: el protocolo propietario de Niagara para comunicación station↔station y workbench↔station.

### Puertos (validados en código decompilado, `BFoxScheme.java`, `BFoxsScheme.java`)
- **Fox** (plain TCP): puerto **1911** — `com.tridium.fox.sys.BFoxScheme.DEFAULT_PORT`
- **FoxS** (TLS): puerto **4911** — `com.tridium.fox.sys.BFoxsScheme.DEFAULT_PORT`
- **Multicast** (discovery): puerto **1911** — `com.tridium.fox.session.Fox.MULTICAST_PORT`

### Por qué los módulos de Tridium pueden abrir sockets sin pedir permission explícito
Todo módulo Tridium **que use Fox** lo hace via `fox-rt` como dependencia. El `fox-rt` declara en su `module.xml`:

```xml
<java-permissions type="station">
  <java-permission action="accept,connect,listen,resolve"
    class="com.tridium.nre.security.NiagaraSocketPermission"
    name="*:1-100000"/>
  ...
</java-permissions>
```

Y `fox-rt` está firmado por el cert Tridium oficial — que SÍ está en el trust anchor. Por eso cualquier módulo Tridium que consuma fox-rt HEREDA el permiso a través de la cadena de calls (siempre que el caller chain no interrumpa con `doPrivileged()` desprivilegiado).

**Consecuencia para nosotros** (sesión httpapi): un módulo custom con `<permissions>` declarados pide literalmente `NiagaraSocketPermission` — idéntico a lo que pide fox-rt. Pero como NO está firmado por el cert Tridium trust anchor, la distribución Honeywell rechaza el grant. **Es el mismo permission, con distinto firmante.**

### Types clave del módulo fox-rt (del `<types>`)
- `BFoxService` — el service de station que expone Fox
- `BFoxSession`, `BFoxConnection`, `BFoxClientConnection`, `BFoxServerConnection`
- `BFoxScheme` (ord scheme `fox:`), `BFoxsScheme` (ord scheme `foxs:`)
- `BFoxComponentSpace`, `BFoxStationSpace` — cómo expone el árbol de componentes remoto
- `BFoxFileSpace`, `BFoxFileStore` — file transfer sobre Fox
- `BSpyChannel`, `BFoxSpySpace` — el spy/debug channel

### Por qué es layered sobre un solo socket
Fox es un protocolo multiplexado sobre **un único TCP socket por sesión**. Dentro del socket corren canales virtuales (`BFoxChannel`): `DataChannel` (queries), `FileChannel` (transfer), `SpyChannel` (debug), `UserChannel` (auth), etc. Por eso una sola conexión workbench↔station maneja todo.

---

## 1.7 Consecuencias prácticas del mental model (Bloque 1)

1. **Todo módulo custom con código backend tiene que ser `-rt`**. Si toca HTTP, drivers, alarmas, timers — va en rt. ux/wb son solo para UI.
2. **Las dependencias son direccionales**. Un `-rt` no puede depender de clases de un `-wb` aunque el source compilaría. El framework lo va a rechazar al cargar.
3. **`module.xml` es el contrato real**, no los annotations del source. El registry y el security manager leen esto para decidir qué types existen y qué permissions tiene el módulo.
4. **`autoload="true"` implica que el módulo se carga al boot de toda station** que lo tenga instalado, sin import explícito. Cuidado con side effects de static initializers.
5. **Un módulo con `<permissions>` DECLARA un grant**. Si el framework no puede validar la firma del JAR contra el trust anchor, RECHAZA cargar el módulo entero. No es "grado por grado" — es binario.
6. **El registry cachea**. Un cambio en `module.xml` que no invalide el cache no se refleja. En dev, borrar `!registry/` fuerza rebuild.
7. **Fox es el transport universal**. Si un módulo custom necesita comunicarse con otra station, usar `fox-rt` como dependencia y heredar sus permissions es la ruta oficial — NO abrir sockets directamente.

---

## Topic keys engram (referencias cruzadas)

- `niagara/estructura/framework` — este bloque completo
- `niagara/honeywell-oem-signing-lockdown` (sesión httpapi #302) — por qué nuestro módulo con `<permissions>` fue rechazado
- `niagara/httpclient-permission-model` (sesión httpapi) — el módulo oficial httpClient y qué permissions tiene

## Archivos leídos
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide-clean/modules.txt` — schema oficial
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide-clean/registry.txt` — sys.registry
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide-clean/station.txt` — station boot/shutdown
- `/home/cristian/Honeywell/OptimizerSupervisor-N4.14.0.162/niagara-help/devguide-clean/execution.txt` — BComponent lifecycle
- `/home/cristian/modules/Prototipos/modulos/organized/httpClient/httpClient-rt/extracted/META-INF/module.xml` — ejemplo real
- `/home/cristian/modules/Prototipos/modulos/organized/fox/fox-rt/extracted/META-INF/module.xml` — Fox permissions
- `/home/cristian/modules/Prototipos/modulos/organized/fox/fox-rt/vineflower/com/tridium/fox/sys/BFox{s,}Scheme.java` — puertos Fox
