# Bloque 96 — Familia "Venom": herramienta de Air Balancing (TAB) de cajas VAV para controladores Spyder/Stryker sobre BACnet y LON, deofuscada

> Investigación empírica de la familia OEM Honeywell **"Venom"**: una herramienta de comisionamiento **Air TAB** (Testing, Adjusting, Balancing) que calibra el flujo de aire de cajas **VAV** controladas por **Spyder/Stryker**, vía BACnet y LON. Producto Honeywell **licenciado aparte** (EULA propia). Arquitectura **MixIn dinámico**: un core abstracto agnóstico de transporte + dos implementaciones de protocolo.
>
> 3 módulos: `honeywellVenomTools` (49 java, core + UI + EULA), `honeywellVenomBacnet` (13 java), `honeywellVenomLon` (10 java).
>
> Fuentes: `organized/honeywellVenom{Tools,Bacnet,Lon}/<m>-rt/vineflower/com/honeywellVenom*/...`.
> Método: 2 sub-agentes Explore (core, BACnet+LON) + **verificación directa** de cada `extends`, la fórmula de calibración, el mecanismo de EULA, y el bug `getHotDefinition`. `[CERT]` = verificado por mí; `[CERT-a]` = cita del sub-agente (proceso de balance, enums, flujo de UI, scheduling) no re-verificada; `[INFER]` = deducción. **CORRIGENDUM a sub-agente**: el MixIn Venom **SÍ** implementa `BIMixIn` (la base lo declara; lo verifiqué) — el sub-agente lo negó mirando solo las subclases que lo heredan.
>
> Capa 22 (OEM deofuscados), continúa [Bloque 95]. Familia A del barrido del corpus. Conecta [Bloque 77] (Spyder = `honeywellBacnetSpyder`/`honeywellLonSpyder`, el controlador objetivo), [Bloque 92] (wizards LON Excel 10), [Bloque 75] (seguridad).

---

## 96.1 — Qué es: Air Balancing de cajas VAV + EULA propia `[CERT]` + `[CERT-a]`

"Balance" aquí = **balanceo de aire** (no agua ni carga). Pruebas `[CERT-a]`: unidades CFM / l·s⁻¹ / in.w.c. / sq.ft; la interfaz `IVenomBalanceDeviceDef` expone exactamente los puntos físicos de una caja VAV (`getKFactor`, `getDuctArea`, `getBoxFlow`, `getVelSenPress`, `getDamperPos`, `getMax/MinFlowSetpt`). El proceso implementa la fórmula de flujo en ducto **`Q = K·√VP`** — verificado `[CERT]` en `BVenomBalanceFlowCalibrator.java:146`: `newKF = (int)(actualFlow / Math.sqrt(deviceVP))`.

**Venom es un producto Honeywell licenciado aparte** (no N4 estándar): tiene EULA propia (Honeywell International, ley de Minnesota) embebida en el binario `[CERT-a]`. Verificación de aceptación `[CERT]`: solo chequea si existe `%NiagaraUserHome%\venomEULA.properties` con clave `CREATED` (`BVenomEulaDialog.java:42`, `CREATED_PROP`, flag `static wasAlreadyChecked`).

---

## 96.2 — El proceso de calibración `[CERT-a]`

Por cada caja VAV (`BVenomBalanceFlowCalibrator` + `BVenomBalanceZeroJob extends BSimpleJob` `[CERT]`):
1. **Zero-step**: cierra el damper 100%, lee el offset de presión de velocidad en reposo (`BalBoxZeroOffset`), lo escribe al controlador.
2. **Cal-step**: abre a máx/mín, promedia 5 muestras, calcula el K-Factor real `Q_actual/√VP_medido` (`BalBoxKFactorOffset`), lo escribe.
3. El controlador queda con sus setpoints de flujo ajustados a la física real del ducto.

K-Factor efectivo del summary = `mfgKF + kfOffset` (calibración como delta sobre fábrica) `[CERT-a]`. `BVenomBalanceStatusEnum` modela el ciclo: `notConfigured`(KF<25) → `notZeroed` → `zeroed` → `balanced`; soporta single-duct y **dual-duct (Hot Deck)** con `BVenomBalanceBalancerSummaryHD extends BVenomBalanceBalancerSummary` `[CERT-a]`. UI Workbench: tabla de red (`BVenomBalanceNetworkView extends BWbComponentView`, abstracta — subclases por protocolo) con botones Zero Cal / Balance (wizard `BVenomBalanceFlowCalView`) / Damper override / Reheat / Export.

---

## 96.3 — El patrón MixIn dinámico `[CERT]`

`BVenomBalanceService extends BComponent implements BIService` `[CERT]` inyecta en cada device soportado un `BVenomBalanceMixIn extends BComponent implements BIMixIn` (abstract, `:39`) al arrancar — **sin modificar las clases del controlador**. La UI opera solo sobre la interfaz abstracta → agnóstica al transporte. Jerarquía concreta verificada `[CERT]`:

```
BVenomBalanceMixIn (core, implements BIMixIn)
 ├─ BVenomBalanceBacnetMixIn (abstract, :26)
 │   ├─ BVenomBalanceBacnetSpyderMixIn  implements IVenomBalanceSpyderAppLogicFeatures (:12)
 │   └─ BVenomBalanceBacnetStrykerMixIn implements IVenomBalanceSpyderAppLogicFeatures (:14)
 └─ BVenomBalanceLonMixIn (abstract, :29)
     ├─ BVenomBalanceLonSpyderMixIn  implements IVenomBalanceSpyderAppLogicFeatures (:12)
     └─ BVenomBalanceLonStrykerMixIn  (SIN la interfaz — :11)
```

**Spyder vs Stryker** (dos líneas de controlador) `[CERT-a]`: **Spyder** expone capacidades como bitmask `appLogicFeatures` (bit1=balance, bit2=reheat, bit3=periphHeat, bit5=fan) + `appLogicCategory` (2=VAV, 4=DualFlowRing). **Stryker** las consulta vía actions sobre un sub-componente `ProdFBTemplate` (BACnet) o vía NCIs `nciHeat` (LON).

---

## 96.4 — `spyderMods`: extensiones de punto para el wizard `[CERT]` + `[CERT-a]`

No inyecta lógica de balance (eso es de los MixIns); agrega `BPointExtension` a los `BInputPoint` del Spyder para un `wizardValue` editable desde Workbench `[CERT-a]`. Base verificada `[CERT]`: `BInputPointExtCommon extends BPointExtension` (abstract), que escribe a `xl10_constant`/`xl10_nci` del Spyder vía `updateContantValueFromExternalTool`/`updateNSValueFromExternalTool`. Variantes: numérica (K-Factor, flujos), enum, y `BInputPointVavInletExt` (área de inlet: convierte pulgadas-diámetro/ft²/m² a ft² con `area = π(d/2)²/144`). Todos los constructores llaman `VenomUtil.legacyJarCheck()` (excepción si el módulo legado `honeywellVenom` está presente) `[CERT-a]`.

---

## 96.5 — El I/O al transporte: directo, bypass del proxy `[CERT-a]`

Ambos MixIns hacen **I/O síncrono directo** sobre el protocolo, NO usan el mecanismo de proxy/subscription del driver:
- **BACnet** (`BVenomBalanceBacnetMixIn`): `BBacnetComm.readProperty/writeProperty(address, objId, 85)` (85 = Present_Value), encoding Asn de Tridium, 3 reintentos. Puntos = Analog Value: Spyder Cool AV 1000-1017, Hot AV 2000-2017. Lee flujo por AV 1013, presión por AV 1014.
- **LON** (`BVenomBalanceLonMixIn`): Network Management directo `NmUtil.fetchNv/setNvValue`; device debe estar `configOnline`; direcciona por string `"nvName.field"`; lee `nvoBoxFlow`/`nvoVelSenPress`, escribe NCIs (`nciKFactor`…) y overrides NVIs.

Sentinel de error compartido: `-2.1474836E9F` `[CERT-a]`.

---

## 96.6 — Scheduling + time master `[CERT]` + `[CERT-a]`

**BACnet** (patrón master→export→objeto estándar):
- `BSpyderLinkableSchedule extends BEnumSchedule implements BIAlarmSource, BIStatus` `[CERT]`: aplana el schedule Niagara a un proxy de **máx 4 eventos/día** (alarma `scheduleTruncated` si excede); escritura 30 s tras cambio + 15 min periódico `[CERT-a]`.
- `BSpyderLinkableScheduleExport extends BBacnetScheduleExport` `[CERT]`: escribe físicamente el objeto **BACnet Schedule (tipo 17)** en el controlador.
- `BBacnetSpyderTimeMaster extends BComponent` `[CERT]`: Niagara como **time master** — cada 24 h itera los devices y hace `timeSynch` a los que su display name sea "BACnetSpyder" o "Asc BACnet V A V" `[CERT-a]`.

**LON** (directo a NV):
- `BDirectLonSchedule extends BEnumSchedule` `[CERT]`: escribe directo un `BPseudoNV(128)` = tod_event (`{currentState, nextState, timeToNextState}`, cap 2879 min). `BEnumSchedDirectLonLinkExt` es la versión add-on. **No hay time master LON** `[CERT-a]`.
- NCI polling (solo LON): `BLonNciPollingFolder extends BLonPointFolder` `[CERT]` fuerza re-lectura periódica de NCIs (no se auto-actualizan como NVs); `BNciRefreshExt` por lista de nombres.

---

## 96.7 — Seguridad y bugs `[CERT]` + `[CERT-a]`

**[BUG CRÍTICO CERT] Stryker BACnet: `getHotDefinition()` devuelve la definición COOL.** `BVenomBalanceBacnetStrykerMixIn.java:24-31`: `getCoolDefinition()` y `getHotDefinition()` retornan ambos `VenomBalanceBacnetStrykerCoolDef.getInstance()` — y **no existe** una clase `StrykerHotDef` (solo `StrykerCoolDef`, verificado). En un Stryker dual-duct, el balance del anillo caliente escribe en los AV del frío → sobrescritura de calibración.

**[ALTO CERT] EULA bypass trivial.** Solo verifica la existencia de `venomEULA.properties` con `CREATED` (sin firma ni binding a usuario/máquina); crear el fichero a mano omite el diálogo. Path con `\\` hardcodeado → solo Windows. Flag `static wasAlreadyChecked` salta la verificación tras la primera vez en la sesión. El texto legal completo (que prohíbe descompilar) está embebido en claro en el binario.

**[MEDIO CERT-a] Acceso directo al protocolo sin autorización por operación.** Cualquier componente con acceso a la red BACnet/LON puede instanciar un MixIn y leer/escribir setpoints de calibración sobre los controladores. El override usa el priority array BACnet completo (1-16) → puede pisar overrides de emergencia (nivel 1).

**[BAJO CERT-a] Bugs de robustez**: `inprogressflag` static en `BSpyderLinkableSchedule` (race multi-instancia); cast sin `instanceof` en `BNciRefreshExt` (`ClassCastException`); `isParentLegal()` de `BBacnetSpyderTimeMaster` con ramas idénticas (código muerto); filtro de devices por display-name (fallo silencioso si Honeywell renombra el tipo); doble sobrescritura en `BEnumSchedDirectLonLinkExt.update()`. Sin credenciales embebidas.

---

## 96.8 — Conexiones

- **[Bloque 77]** (Spyder `honeywellBacnetSpyder`/`honeywellLonSpyder`): el controlador objetivo del balanceo. Venom inyecta MixIns sobre esos devices.
- **Stryker**: la otra línea de controlador Honeywell (consulta capacidades vía `ProdFBTemplate`/NCI en vez de bitmask).
- **[Bloque 92]** (wizards LON): comparten el dominio de comisionamiento HVAC Honeywell sobre LON.
- **[Bloque 75]** (seguridad): suma EULA bypass + escritura de setpoints sin autorización por operación + el bug hot/cool.
