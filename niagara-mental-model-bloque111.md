# Bloque 111 — `honEagleHawkHMI`: el **HMI local embebido** de los controladores físicos EAGLE/HAWK — la plataforma de panel de operador que el migrador del [Bloque 90] **retira** al subir a BEATS ADV, deofuscado

> Investigación empírica del módulo OEM Honeywell `honEagleHawkHMI` (`com.honeywell.hmi.*`, symbol `hmi`, vendor **Honeywell `4.14.0.3.2.88`**, build **2025-01-21**, descripción *"Honeywell Human Machine Interface"*). Sub-jars `{rt, ux, wb}`.
>
> **Qué es** `[CERT]`: NO es un dashboard web ni una vista de Workbench. Es el **HMI LOCAL del hardware** — el firmware Java que dibuja la **pantalla de operador física embebida** en los controladores **EAGLE/HAWK** (la línea EHN4/Ciper50/CP-NX pre-BEATS, [Bloque 90.5]). Login por **PIN de 5 dígitos**, **FAL** (Fast Access List = atajos del operador), edición local de schedules/calendars, vistas de alarma, settings de idioma/fecha-hora/watchdog. Renderiza sobre una **librería nativa `libhmi`** (JNI) alimentada por **screendefs JSON** (51 ficheros con fuentes + bitmaps crudos), NO sobre HTTP/servlet.
>
> **El hallazgo de anclaje** `[CERT]`: este módulo **ES** exactamente lo que el `removeLegacyServices()` del migrador EagleHawk→PlantController del [Bloque 90.5] **elimina** al hacer upgrade a BEATS ADVANCED. B90 lo describió como *"EagleHawk HMI service + authenticators + LED recipients"*: aquí están las tres piezas verbatim — `BHonEagleHawkHmiService` (el service), `BHonEagleHawkHmiAuthenticator` (el authenticator PIN, mix-in en cada `baja:User`) y `BHmiAlarmConsoleRecipient` (el recipient de alarmas del panel). Es la **plataforma de HMI legacy** que el stack moderno reemplaza por `platHMI` ([Bloque 91]).
>
> Fuentes: `organized/honEagleHawkHMI/honEagleHawkHMI-{rt,ux,wb}/vineflower/com/honeywell/hmi/` + `rc/screendefs/*.json` + los tres `META-INF/module.xml`. Decompilación vineflower **limpia** (nombres Honeywell **no ofuscados** — legible).
> Método: **verificación directa por mí** de cada `extends`/`implements`, de los `module.xml` (deps + tipos + permisos), del bucle nativo del `ServiceThread`, del `IHmiNative` (los 11 métodos `native`), del modelo PIN (`BHonEagleHawkHmiAuthenticator`/`BHonEagleHawkHmiPinStrength`/`BUserPinConfiguration`), del FAL (`BFastAccessList` + `BFALServerSideCallHandler` + `BFastAccessListWidget`), del `AlarmSubscriber` y del rendering por screendef (`BHome.create`). `[CERT]` = verificado verbatim por mí; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 110]. **ATERRIZA el [Bloque 32]** (línea 217: *"honEagleHawkHMI ×3 — UI framework para controllers EAGLE/HAWK"*, descripción correcta pero vaga). **Cierra la cola fina del inventario OEM Honeywell.** Conecta fuerte con [Bloque 90] (`honPlantController` — el migrador que lo retira), [Bloque 91] (`platHMI` — su sucesor en BEATS ADV), [Bloque 86] (PanelBus/OnboardIO — el destino de la migración), [Bloque 82] (tags `hon:` — el FAL usa `hon:FALname`), [Bloque 75] (seguridad).

---

## 111.1 — Identidad, sub-jars y conteo real `[CERT]`

| | Valor |
|---|---|
| Módulo | `honEagleHawkHMI` (rt/ux/wb), symbol `hmi`, vendor **Honeywell `4.14.0.3.2.88`** |
| Build | **2025-01-21** (`buildMillis=1737465803907`, host `azu-hce-vbf-w13`) — **N4.14, recompilado, NO código muerto** |
| Descripción | *"Honeywell Human Machine Interface"* |
| Deps clave | `control-rt`, `driver-rt`, `alarm-rt`, `gx-rt`, `schedule-rt`, `tagdictionary-rt` + **`honTagDictionary-rt`** (Honeywell) `[CERT, module.xml]` |
| Permiso especial | **`LOAD_LIBRARIES` para `libhmi`** (*"needs to load the limhmi native library to function"*) `[CERT, module.xml]` |

> **Conteo honesto** `[CERT]`: el inventario reportado de **~237 java estaba inflado** por los decompilados triples (`decompiled/`, `procyon/pipeline/`, `vineflower/` — 219 solo en `-rt`). Los **`.java` reales de UN decompiler (vineflower)** son: **rt = 73, ux = 2, wb = 4 → 79 clases reales**. Mismo patrón de inflado por triple-decompilado que el [Bloque 110.1] corrigió (los "147" eran 49). El número real de este bloque es **79**.

**El reparto por runtime profile** `[CERT, module.xml]`:
- **`-rt` (73)** = el **HMI nativo on-device**: service, authenticator PIN, las **56 vistas** `com.honeywell.hmi.view.*`, enums, alarma, util, `IHmiNative`. Corre **dentro del controlador**.
- **`-ux` (2)** = el **editor web del FAL**: `BFastAccessListWidget` (widget JS) + `BFALServerSideCallHandler` (RPC server-side).
- **`-wb` (4)** = el **editor del FAL en Workbench**: `BFastAccessListView`, `BMyTable`(+controller), `BMessageNotificationHandler`.

> **Las 3 superficies de UI** `[CERT]`: hay que distinguirlas. (a) La **pantalla física del operador** (rt, nativa vía `libhmi`+screendefs) — para quien está parado frente al controlador. (b) El **widget web** (ux, `BIJavaScript`) y (c) la **vista Workbench** (wb) — ambos para que el **ingeniero configure el FAL** desde un navegador o desde Workbench. La (a) **renderiza**; (b)/(c) **editan la lista** que (a) muestra.

---

## 111.2 — `BHonEagleHawkHmiService`: el service nativo y su bucle de eventos `[CERT]`

`BHonEagleHawkHmiService extends BComponent implements BIService` (`:28`) — **NO** es un `BAbstractService`; es un `BComponent` plano que implementa `BIService` directamente. Propiedades `[CERT]`:

| Slot | Tipo | Función |
|------|------|---------|
| `enabled` | `boolean` | gate maestro: `changed()` arranca/para el service |
| `language` | `String` | idioma del panel (`ModuleLex.switchLanguage`) |
| `safetyWarningMessage` / `welcomeMessage` | `String` | textos legales/bienvenida del arranque |
| `home` | `BHome` | la vista raíz del panel (árbol de vistas hijo) |
| `loginView` | `BLoginView` (hidden) | pantalla de login PIN |
| `lastUnsuccessfullLogin` / `unsuccessfullLoginCounter` | `BAbsTime` / `int` (hidden) | **tracking de login fallido** (lockout) |

**El puente nativo** `[CERT]`: `loadLibrary()` hace `System.loadLibrary("libhmi")` envuelto en `AccessController.doPrivileged` (de ahí el permiso `LOAD_LIBRARIES`). `libhmi` es la **librería C del hardware del display** — el firmware del panel. `IHmiNative` declara **11 métodos `native`** `[CERT]`:

```
hmiOpen0() / hmiClose0(handle)                          // abre/cierra conexión al display
hmiWaitEvent0(hmiHandle, viewHandle, timeout, eventData)// bloquea esperando evento del panel físico
hmiCreateView0(handle, layer, screendef, data, rights)  // empuja un screendef JSON al display → viewHandle
hmiDestroyView0 / HmiAccept0 / HmiUpdateContent0
hmiGet/SetElementProperty0 / hmiGet/SetLBRowElementValue0 // lee/escribe elementos y filas de listbox
```

**El `ServiceThread` (daemon)** `[CERT]` es un **bucle de eventos hardware** clásico de panel embebido:
1. `hmiWaitEvent(-1,-1,100,...)` hasta recibir `hmiReady` → `hmiOpen()` obtiene el `_hmiHandle`.
2. Si no hay vista, crea `BHome` (`getHome().create(handle)`).
3. En loop: `hmiWaitEvent(hmiHandle, viewHandle, 100, eventData)` y despacha por ordinal del evento:
   - **key press** (caso 3): `Helper.getKey()` → `BHmiKeyEnum`; teclas físicas **BACK / wheel / etc.** navegan el árbol de vistas (`destroyUpToHome`, `onKeyPressed`). Hay una tecla dedicada que salta a **TimePrograms** (atajo de scheduling), pidiendo login si no hay usuario.
   - **value changed** (caso 4): `onIsValueChangeAccepted` → `HmiAccept(...)` → `onValueChanged` (edición de un setpoint en el panel, con confirmación nativa).
   - **set focus** (caso 5).
4. **Auto-logoff** `[CERT]`: si hay usuario y `now - lastActionTime > autoLogoffPeriod` (del authenticator del user, default 10 min) → `setUser(null)` + vuelve a `Home`.

> **La señal "legacy hardware"** `[CERT]`: el modelo de interacción es **rueda + botones físicos** (`BHmiKeyEnum.wheel`, "BT_BACK"), pantalla monocroma pequeña (los bitmaps de los screendefs son arrays de bytes monocromos — `STAR30 30×30`, `ALARMS30`). Es un **panel de operador local de hardware de campo**, no un browser. Eso ancla por qué B90 lo trata como plataforma a retirar: el hardware BEATS ADV trae otro HMI (`platHMI`, [Bloque 91]).

---

## 111.3 — El modelo de rendering: screendefs JSON + native, NO web `[CERT]`

Cada `BHmiView` (abstracta, `extends BComponent`) define `create(int hmiHandle)` / `updateContent()` / `onKeyPressed` / `onValueChanged` / `onSetFocus`. El patrón de `create()` (verificado en `BHome.create`) `[CERT]`:

```java
BOrd fileOrd = BOrd.make("module://honEagleHawkHMI/rc/screendefs/home.json");
String screendef = new String(((BIFile)fileOrd.resolve(this).get()).read());  // lee el screendef estático
// inyecta la definición de hijos (menú/items) dentro del JSON:
screendefBuilder.insert(nPos+1, this.getChildren());
// arma el "data" runtime (TIME, HOME, $TIMEOUT, valores actuales):
Helper.appendToData("TIME", timestamp, data);  ...
this._viewHandle = IHmiNative.hmiCreateView(this._hmiHandle, 0, screendef, data.toString(), 0); // ← empuja al display
```

**El modelo es: layout estático en JSON + datos runtime, empujados al firmware por JNI** `[CERT]`. Los **51 screendefs** (`rc/screendefs/*.json`) son la "plantilla" de cada pantalla (fuentes, imágenes bitmap, listbox, botones); la clase Java solo **inyecta los datos vivos** y llama `hmiCreateView`. `updateContent()` refresca por `HmiUpdateContent`/`hmiSetElementProperty`. **No hay HTTP, ni servlet, ni HTML** en el path rt — todo va al display nativo.

**Taxonomía de las 56 vistas** `com.honeywell.hmi.view.*` `[CERT, module.xml]`:

| Grupo | Vistas | Qué muestra/edita en el panel |
|---|---|---|
| **Shell** | `BHmiView`(base abstracta), `BHome`, `BMenu` | raíz + navegación |
| **Login / PIN** | `BLoginView`, `BLoginUserPinView`, `BLoginUserOptions`, `BUserOptionsView`, `BChangePINView`, `BNewPinView`, `BForceExpirationMsgView`, `BAutoLogoutDelayView` | autenticación PIN local (111.4) |
| **Puntos** | `BDataPointList`, `BSinglePointView`, `BSinglePointOverridesView`, `BSinglePointViewHelp`, `BFastAccessList`, `BFalAssignment` | navegar/forzar puntos + el **FAL** (111.5) |
| **Scheduling** | `BTimePrograms`, `BSchedules`, `BSchedule`, `BScheduleSwitchPointsView`, `BEditSwitchPointView`, `BDeleteSwitchPointView`, `BCopySwitchPointsView`, `BScheduleValuesView`, `BScheduleExeptionsView` | editar weekly schedules **localmente** (111.6) |
| **Calendarios** | `BCalendars`, `BCalendar`, `BCalendarControl`, `BNewCalendarEntryView`(+Name), `BDeleteCalendarEntryView`, `BCalendarReferenceEventView`, `BCustomEventView`, `BDateRangeEventView`, `BWeekAndDayEventView`, `BSpecificDateEventView`, `BSetCalendarReferenceView` | excepciones de calendario |
| **Alarmas** | `BAlarmList`, `BAlarmDetailsView`, `BAlarmIconHelp` | ver/ack alarmas en el panel (111.6) |
| **Settings** | `BControllerSettings`, `BControllerInformation`, `BLocaleSettingsView`, `BDateTimeSettingsView`, `BAutoSaveSettingsView`, `BWatchdogSettingsView`, `BChangeLanguageView` | config local del controlador |
| **Filtros / msgs** | `BFilterSelectionView`, `BNameFilter`, `BNetworkFilter`, `BPointFolderFilter`, `BSafetyWarningView`, `BWelcomeMessageView`, `BMultiselectionListView`, `BTextEditControl` | filtrado de listas + mensajes/edición de texto |

---

## 111.4 — El modelo de auth: PIN de 5 dígitos como **mix-in sobre `baja:User`** `[CERT]`

`BHonEagleHawkHmiAuthenticator extends BComponent implements BIMixIn` (`:34`), registrado en `module.xml` como **agent `on type="baja:User"`**. La clave del modelo `[CERT]`:

- **NO crea un esquema de usuarios/roles propio.** Es un **mix-in que el service inyecta en CADA `baja:User`**: `serviceStarted()` → `getComponentSpace().enableMixIn(BHonEagleHawkHmiAuthenticator.TYPE)` (y `disableMixIn` en `serviceStopped`). Así, cada usuario Niagara estándar gana una **credencial PIN paralela** para el panel local; la **identidad, roles y permisos siguen siendo los del `baja:User`**. El login del panel resuelve a un `BUser` (`setUser(BUser)` / `getUser()`).
- Slots `[CERT]`: `pin` (`BPassword`, `fieldWidth=5`), `pinConfiguration` (`BUserPinConfiguration`), `autoLogoffPeriod` (`BRelTime`, default **10 min**), action `removePin`.

**El PIN es exactamente 5 dígitos numéricos** `[CERT]`: `checkPassword` instancia `new BHonEagleHawkHmiPinStrength(5, 0, 0, 5, 0, 5)` = `minLength=5, minDigits=5, maxLength=5` (sobre `BPasswordStrength`). Cinco caracteres, los cinco dígitos.

**Storage y validación reusan la infraestructura de passwords de Niagara** `[CERT]` — no es crypto casera:
- Al setear el PIN, `setConvertedPin()` → `convertToPbkdf2Password()` lo re-encoda a **`BPbkdf2HmacSha256PasswordEncoder`** (mismo encoder que las passwords Niagara). El slot `pin` queda **hasheado PBKDF2-HMAC-SHA256**, no en claro.
- `validate(BPassword usersPin)` → `currentPin.getPasswordEncoder().validate(passChars)` (compara contra el hash).
- **No-reuse / historial** `[CERT]`: `isDuplicatePin()` consulta `getPinConfiguration().getPasswordHistory()` (`BUserPinConfiguration extends BUserPasswordConfiguration`), y `changeIntervalCheck(scheme.getGlobalPasswordConfiguration())` aplica la **política global de passwords** de la station (edad mínima, etc.). El PIN respeta el régimen de contraseñas del `BPasswordAuthenticationScheme` del user.
- **Lockout** `[CERT]`: el service trackea `lastUnsuccessfullLogin` + `unsuccessfullLoginCounter` (anti fuerza-bruta del panel).

> **Veredicto de seguridad del auth** `[CERT]`: el modelo es **honesto** — PIN hasheado PBKDF2, historial, política global, lockout, todo sobre el motor de passwords de Niagara, sin reinventar crypto. La **única superficie débil intrínseca** es el espacio del PIN: **5 dígitos = 100.000 combinaciones**, mitigado por el counter de fallos y el auto-logoff. Es un trade-off deliberado de ergonomía (rueda + dígitos en un panel físico) vs. una password completa. Aporta al [Bloque 75] como ejemplo de credencial reducida para HMI de campo.

---

## 111.5 — El FAL (Fast Access List): atajos del operador, editados por **server-side call** `[CERT]`

El **FAL = Fast Access List** es la **lista curada de atajos** (puntos / schedules / calendarios) que el operador ve y maneja rápido en el panel, sin navegar todo el árbol.

**Lado on-device** `[CERT]`: `BFastAccessList extends BHmiView`. Slots: `objectsList` (`BVector` de `BFalAssignment`), `singlePointView`/`scheduleView`/`calendarView`/`nameFilterView`, `updateIntervalMillis=15s` (min 5s / max 120s). Cada entrada `BFalAssignment` lleva un `objectOrd` (`BOrd`) a un `BControlPoint`, `BControlSchedule` o reference point. `makeHmiDisplayName()` produce el nombre de operador.

**Membresía registrada como tag `hon:` (puente al [Bloque 82])** `[CERT]`: al agregar/quitar un objeto, `BFastAccessList` hace `Helper.addTag/removeTag(obj, Id.newId("hon:FALname"), getDisplayName())`. O sea, la pertenencia al FAL se **marca como tag `hon:FALname` sobre el propio punto** — coherente con la familia de tags `hon:` que el [Bloque 82] catalogó.

**Lado editor — `BFALServerSideCallHandler`** `[CERT]`: `extends BSingleton implements BIServerSideCallHandler` (`javax.baja.box`), agent `on FastAccessList` con `requiredPermissions="ri"`. Es el **RPC de servidor** que el widget JS (ux) y la vista Workbench (wb) llaman para manipular el FAL en la station. Métodos `[CERT]`:

| Server-side call | Qué hace |
|---|---|
| `syncObjectList` | reconcilia la lista (`fal.syncObjectList()`) |
| `createAssignmentList` | devuelve **JSON** `{"assignmentList":[{"name":..,"type":..}]}` (type = controlPoint / schedule / referencePoint, localizado) |
| `addObject` | recibe un `BOrd`, resuelve el objeto y `fal.addObject()` |
| `removeObjects` | `BVector` de índices → quita las entradas |
| `moveUp` / `moveDown` | `reorder()` de las propiedades del `objectsList` |

> **La arquitectura del FAL en una línea** `[CERT]`: el ingeniero, desde **Workbench (wb) o un navegador (ux, `BFastAccessListWidget` → `FastAccessListWidget.js`)**, usa **server-side calls** (BajaScript box RPC) para construir la lista; el FAL vive en la station como `BVector` de `BFalAssignment` + tags `hon:FALname`; el **operador en el panel físico** ve `BFastAccessList` renderizada nativamente y accede rápido a esos puntos/schedules. Es el "favoritos/shortcuts" del panel local. El migrador de B90 **preserva el FAL** al migrar (B90.5 lista "FALs" entre lo que migra a `platHMI`).

---

## 111.6 — Scheduling y alarmas locales `[CERT]`

**Scheduling on-device** `[CERT]`: las vistas `BSchedule`/`BCalendar` envuelven `BWeeklySchedule`/`BCalendarSchedule` de Niagara (`schedule-rt`). El operador edita **localmente en el panel**: switch points (`BEditSwitchPointView`, `BCopySwitchPointsView`, `BScheduleValuesView`), excepciones y eventos de calendario (`BDateRangeEventView`, `BWeekAndDayEventView`, `BSpecificDateEventView`). Es scheduling Niagara estándar editado por screens nativas — autonomía de campo sin Workbench.

**Alarmas locales** `[CERT]`: dos piezas.
- `AlarmSubscriber extends javax.baja.sys.Subscriber` — un thread (cada 10 s) que hace **BQL-subscribe a todo** `control:ControlPoint` y `driver:IPointFolder` (`subscribeToMatches`), para que el panel refleje el estado de alarma vivo de los puntos. Se activa solo con usuario logueado (`setUser` → `subscribe`/`unsubscribe`).
- `BHmiAlarmConsoleRecipient extends com.tridium.alarm.BConsoleRecipient` — el **recipient de alarmas del panel** (rutea alarmas a la consola del HMI). Vistas: `BAlarmList`, `BAlarmDetailsView`, `BAlarmIconHelp` (ver/ack en el panel).

> Este `BHmiAlarmConsoleRecipient` es el *"LED recipient"* que B90.5 menciona entre lo que `removeLegacyServices()` elimina — el recipient que en EagleHawk encendía el indicador/consola de alarma del panel.

---

## 111.7 — La relación LEGACY con el [Bloque 90]: qué retira el migrador y por qué `[CERT]`

El [Bloque 90.5] destiló el migrador `BOnlineMigrationJob` de `honPlantControllerMigrator`, que actualiza una station **EagleHawk legacy → BEATS ADVANCED** (PanelBus/OnboardIO, [Bloque 86]). Su secuencia incluye dos pasos que tocan **este** módulo `[CERT, B90.5]`:

```
migra HMI (devices, alarmas, descriptors, FALs, schedules, HMINetwork)  ← preserva el contenido del HMI EagleHawk
removeLegacyServices()  ← ELIMINA: EagleHawk HMI service + authenticators + LED recipients
addHonPlantControllerServices()  ← instala el stack BEATS (platHMI, B91)
```

**El mapeo exacto, ahora verificado de los dos lados** `[CERT]`:

| B90 dice que `removeLegacyServices()` elimina… | …que en `honEagleHawkHMI` es |
|---|---|
| *"EagleHawk HMI service"* | `BHonEagleHawkHmiService` (111.2) |
| *"authenticators"* | `BHonEagleHawkHmiAuthenticator` (mix-in PIN en cada user, 111.4) |
| *"LED recipients"* | `BHmiAlarmConsoleRecipient` (111.6) |

Y lo que el migrador **migra (no descarta)** — *"FALs, schedules, alarmas"* — son justo el FAL (111.5) y las weekly schedules/calendars (111.6): el **contenido funcional** se trasvasa al nuevo `platHMI` de BEATS, pero la **plataforma de servicio EagleHawk se retira**.

**Por qué es "legacy"** `[CERT]` (con matiz importante):
- **Hardware legacy, no código muerto.** El target es la línea **EAGLE/HAWK = EHN4 / Ciper50 / CP-NX** (`StationType.EAGLEHAWK = "EHN4/Ciper50/CP-NX"`, [Bloque 90.5]) — controladores **pre-BEATS** con panel de operador físico. El módulo está **recompilado para N4.14 (build ene-2025)**: sigue shippeando y mantenido. "Legacy" = **plataforma de hardware previa**, retirada al migrar a BEATS ADV — no abandonware.
- El **sucesor** en BEATS ADVANCED es `platHMI` ([Bloque 91], la misma familia `honPlantController`). El controlador BEATS corre Ubuntu Core/snap ([Bloque 90]) con un HMI distinto; por eso el upgrade in-situ retira el service EagleHawk e instala el nuevo.

---

## 111.8 — Hallazgos, seguridad y cierre `[CERT]`

**Hallazgos CERT**:
1. **HMI local embebido, no web** — `BHonEagleHawkHmiService` (BComponent + BIService) corre un `ServiceThread` daemon sobre **`libhmi` (JNI, 11 native methods)** alimentado por **51 screendefs JSON** (`hmiCreateView(handle, layer, screendef, data, rights)`). Modelo de interacción rueda+botones, pantalla monocroma. Es el firmware del panel de operador del hardware EAGLE/HAWK.
2. **Auth PIN como mix-in sobre `baja:User`** — `BHonEagleHawkHmiAuthenticator implements BIMixIn`, inyectado por `enableMixIn` en cada user. **PIN de 5 dígitos** (`BHonEagleHawkHmiPinStrength(5,0,0,5,0,5)`), **hasheado PBKDF2-HMAC-SHA256**, con historial/no-reuse, política global y lockout (`unsuccessfullLoginCounter`). **No crea roles** — reusa la identidad/permisos del `baja:User`.
3. **FAL = Fast Access List** — `BVector` de `BFalAssignment` (atajos a puntos/schedules/calendars), membresía marcada con tag **`hon:FALname`** ([Bloque 82]), editado por **server-side calls** (`BFALServerSideCallHandler implements BIServerSideCallHandler`) desde el widget JS (ux) o la vista Workbench (wb), y renderizado nativamente en el panel.
4. **Es exactamente lo que retira `removeLegacyServices()` del [Bloque 90]** — service + authenticator + alarm recipient. Cierra el lazo legacy EagleHawk↔BEATS.

**Seguridad (aporta al [Bloque 75])**:
- **PIN reducido** `[CERT]`: 5 dígitos = 10⁵ combinaciones; mitigado por counter de fallos + auto-logoff (10 min). Crypto correcta (PBKDF2), pero espacio de credencial chico por diseño de panel físico. Riesgo: acceso físico al hardware + fuerza bruta lenta.
- **JNI con `doPrivileged`** `[CERT]`: `System.loadLibrary("libhmi")` bajo `AccessController.doPrivileged` y permiso `LOAD_LIBRARIES` declarado — superficie nativa C **no auditable desde el bytecode** (igual que el `libplantctrl` del [Bloque 90.4]). Toda la lógica del display vive en `libhmi`, fuera del corpus Java.
- **`BIServerSideCallHandler` con `requiredPermissions="ri"`** `[CERT]`: el RPC del FAL exige permiso `r`(read)+`i`(invoke) — los `addObject`/`removeObjects`/`moveUp` están gateados por el permission model de Niagara. **Sin** validación de ORD inyectado más allá del permiso (el `addObject` resuelve un `BOrd` recibido del cliente — perfil de riesgo bajo, input de operador autorizado, similar al BQL-injection-bajo de [Bloque 110.7]/[Bloque 109.4]).
- **`AlarmSubscriber` BQL amplio** `[CERT]`: subscribe a `select * from control:ControlPoint` y `driver:IPointFolder` cada 10 s — costo de suscripción global por sesión de operador; no es un defecto de seguridad pero sí de escala en stations grandes.
- **Limpio en lo grave** `[CERT]`: sin `Runtime.exec`, sin crypto casera (usa el encoder PBKDF2 de Niagara), sin credenciales hardcodeadas en el Java. La superficie real está en `libhmi` (nativo) y en el espacio del PIN.

**Matiz / aterrizaje al [Bloque 32]** `[CERT]`: B32 (línea 217) lo listó como *"honEagleHawkHMI ×3 — UI framework para controllers EAGLE/HAWK"*. **Correcto pero vago.** Precisión: NO es un "UI framework" genérico — es el **HMI de operador local embebido** (native `libhmi` + screendefs), con **auth PIN como mix-in de user** y el **FAL**, y es la **plataforma legacy** que el migrador del [Bloque 90] retira al subir a BEATS ADV. No requiere corrección a B90 (su descripción de `removeLegacyServices()` es exacta y ahora verificada de los dos lados).

**Para MX60 / Honeywell**: el patrón replicable es el **HMI de campo desacoplado del editor**: el panel físico renderiza por screendefs declarativos + un puente nativo, mientras la **configuración (FAL) se edita remoto por server-side RPC** y se persiste como tags sobre los propios puntos. La credencial reducida (PIN) como **mix-in sobre el user estándar** evita duplicar el modelo de identidad — el operador del panel ES un user Niagara con una segunda credencial. La contracara: dependencia de una librería nativa cerrada (`libhmi`) y un espacio de PIN chico que exige lockout/logoff agresivos.

**Pendiente conocido**: el interior de `libhmi` (C nativo) y el contenido completo de los 51 screendefs JSON (layouts/bitmaps) quedan fuera del alcance del corpus Java. Las 56 vistas se taxonomizaron por grupo (111.3) y se verificaron las representativas (`BHome`, `BFastAccessList`, login/PIN); el detalle screen-por-screen de cada editor de switch-point/calendar-event es mecánico y no se destiló clase-por-clase.
