# Bloque 102 — `genericUIFramework`: dos frameworks UI Honeywell empaquetados (motor MVC tipo Struts + `BajaUICreator` cargador de widgets PX), base de los wizards, deofuscado

> Investigación empírica del módulo OEM Honeywell **`genericUIFramework`** (39 java, Workbench-only): **dos frameworks de UI independientes empaquetados juntos** — (A) un motor MVC request/response/action al estilo Struts 1.x (config XML, sesiones, beans, validadores, forwards) y (B) `baja/ui/creator`, un cargador/registro de widgets PX para Workbench con widgets custom. Originado en el proyecto **WebVision** de Honeywell; es la **base de infraestructura de los wizards** (termostato, etc.).
>
> 1 módulo. Paquetes: `generic/ui/framework` (16) + `cfg` (8) + `demo` (6); `baja/ui/creator` (5) + `lib/widgets` (2) + `demo` (1).
>
> Fuentes: `organized/genericUIFramework/.../vineflower/com/honeywell/{generic/ui/framework, baja/ui/creator}/...`.
> Método: 1 sub-agente Explore + **verificación directa** de cada `extends`, el ORD scheme `wid:` y el widget de password. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (flujo MVC, cfg XML, demos, reflexión) no re-verificada; `[INFER]` = deducción.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 101]. **Cierra el barrido de verificación del corpus** (familia F, la última). Conecta [Bloque 97]/[Bloque 98] (los wizards de termostato que se apoyan en esta UI), [Bloque 75] (seguridad).

---

## 102.1 — Dos frameworks en uno `[CERT]`

El JAR empaqueta dos frameworks de propósito distinto, sin acoplamiento entre sí `[CERT-a]`:
- **(A) `generic/ui/framework`**: motor de flujo MVC **independiente de Workbench** (demo CLI), estilo Struts 1.x — sesiones con timeout, beans con scope request/session, validadores encadenados, forwards view/action, todo configurado por XML.
- **(B) `baja/ui/creator`**: cargador y registro de widgets **PX** para Workbench. `BajaUICreator` decodifica un `.px`, construye el árbol `BWidget` e indexa por ID.

La idea unificadora `[INFER]`: los wizards Honeywell usan (B) para gestionar el árbol PX y (A) para el flujo de pantallas/validación; el wiring widget↔bean lo hace el código del action.

---

## 102.2 — Framework A: motor MVC `[CERT]` + `[CERT-a]`

Clases raíz verificadas `[CERT]`: `AbstractAction extends RequestHandler` (`generic/ui/framework/…:5`), `AbstractValidator extends RequestHandler`, `NOPAction extends AbstractAction`; `UIFwSession/UIFwRequest/UIFwResponse` implementan interfaces propias.

Configuración por XML (`cfg`, 8 clases) `[CERT-a]`: `<GenericUIFrameworkCfg>` con `SessionTimeout` (minutos), `ActionList` (cada `Action` = clase + beans request/session + validadores + forwards), `ValidatorList`. `ForwardCfg` tipo `"view"` (retorna al UI) o `"action"` (encadena a otra acción). Los beans/actions/validators se instancian **por reflexión** desde el className del XML (`BeanCfg.getNewBeanInstance()` etc.) `[CERT-a]`.

---

## 102.3 — Framework B: `BajaUICreator` + el ORD scheme `wid:` `[CERT]`

`BajaUICreator` (sin extends, wrapper de `PxDecoder`) `[CERT]`: decodifica el `.px` → árbol `BWidget` → indexa `Map<String,BWidget>` por ID + extrae PxProperties. API: `getWidgetById(id)`, `getWidgetByPath(...)`, `getWidgetFacetsById`, `recreateUI()` `[CERT-a]`.

**El binding es unidireccional-manual** `[CERT-a]` (no hay notificación automática widget↔bean):
- `BWidgetIdBinding extends BBinding` `[CERT]` (`:21`): agente que se incrusta como child de cualquier widget; su `ord` toma un `wid:` ORD (ej. `wid:lb1`).
- `BWidgetIdOrdScheme extends BOrdScheme` `[CERT]`: registra el esquema `"wid"` (verificado `super("wid")`); `resolve()` retorna `BString.make(query.getBody())` — sin lookup en el árbol, el ORD **es** el ID literal.
- `BajaUICreator.loadWidgetIdRefs()` recorre el árbol y registra cada `BWidgetIdBinding` en `widgetsByIds` `[CERT-a]`. El código cliente hace `getWidgetById("lb1")` y opera el widget imperativamente.
- `BRadioButtonGroupBinding extends BBinding`: comparte un `ToggleCommandGroup` por nombre; con `groupScope=localToPx` prefija el `instanceNumber` para aislar grupos cuando hay varias instancias del mismo PX. `BWidgetIdBinding.started()` además repara links de `BListDropDown`/`BTextDropDown`/`BTable` (workaround de bugs de Niagara) `[CERT-a]`.

---

## 102.4 — Widgets custom `[CERT]`

- **`BPasswordField extends BTextField`** (`lib/widgets/…:8`): aplica `PasswordRenderer` (enmascara) + `setAllowCopying(false)` (verificado, bloquea copia al portapapeles). 4 líneas efectivas.
- **`BSpinControl extends BGridPane`** (`:20`): spinner + text field editable (`BSpinnerButton` + `BTextField`), props `min/max/step/precision/rotate`, publica `Topic valueChanged` (`BFloat`), clamp o wrap según `rotate` `[CERT-a]`. Aporta sobre el `BSpinnerButton` estándar el campo editable + evento tipado.

`BToggleButtonGroupScopeEnum extends BFrozenEnum`. La `demo` (Bean1/Bean2/BeanCfg/`BDemo extends BFrame`/CommandLineUI) son ejemplos toy confirmados `[CERT-a]` (p.ej. `MyValidator.validate()` siempre retorna false).

---

## 102.5 — Origen WebVision + quién lo usa `[CERT-a]`

`module.xml` `runtimeProfile='wb'` (solo Workbench). `Demo.px` conserva el path de build `C:/…/WebVision_Trunk/Assets/WsBajaUICreator/…` → el módulo nació como **"WsBajaUICreator" dentro del trunk de WebVision** (el producto supervisor Honeywell). Trae `lib/px/Wizard.px` (template de wizard reutilizable) → es la **base de los wizards de Workbench Honeywell** (termostato [Bloque 97]/[Bloque 98], AX→N4, etc.), que instancian `BajaUICreator` con su propio PX y usan el framework `generic` para el flujo de pasos `[INFER]`.

---

## 102.6 — Seguridad `[CERT]` + `[CERT-a]`

- **[BAJO CERT] `BPasswordField`** enmascara y bloquea copia correctamente; sin logging del valor. Matiz `[CERT-a]`: como hereda de `BTextField`, en `buic.demo.mode=true` el `populateSampleData4TextFields()` inyectaría el sampleData también en campos de password (confusión, no fuga de la real).
- **[MEDIO CERT-a] Instanciación por reflexión desde XML.** beans/actions/validators se cargan por className tomado del XML de config; si el XML es modificable por un atacante → instanciación de clases arbitrarias del classpath.
- **[BAJO CERT-a] Sesiones sin expiración por defecto.** Si nunca se llama `loadConfiguration()`, el `sessionTimeout` por defecto da ~3420 años → memory leak potencial si se crean sesiones sin cerrarlas. `Log` escribe a `System.out` (no al log de Niagara).

---

## 102.7 — Conexiones

- **[Bloque 97]/[Bloque 98]** (wizards de termostato): consumidores de esta infraestructura — `BajaUICreator` + PX de wizard + flujo MVC.
- **[Bloque 75]** (seguridad): suma instanciación por reflexión desde config XML.
- **Cierra el barrido del corpus**: con este bloque quedan destiladas las 6 familias de OEM Honeywell sin cobertura detectadas en el barrido (B Smart Edge/Device Manager → A Venom → C wizards termostato → D IPC/CIPer → E balancing → F este framework).
