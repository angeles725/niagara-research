# RESEARCH-STATE — focus: modbus (ACTIVO 19/22)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-07-30** sobre el **driver Modbus
> completo de Niagara N4** — los 6 módulos Tridium (`modbusCore`, `modbusTcp`, `modbusAsync`,
> `modbusSlave`, `modbusTcpSlave`, `modbusTcpSlaveMigrator`) más los 2 OEM Honeywell.
>
> **NO es una reapertura del focus `protocols`.** Ese focus cerró el **wire-level** (B131: MBAP, PDU por
> function code, framing RTU/CRC-16, ASCII/LRC, addressing y byte-order) y produjo la coda aplicada B137
> (plan de integración LOGO! 8). Lo que B131/B137 **no** cubren es el **driver** que rodea a ese wire: el
> árbol de componentes que el integrador configura, el modelo de puntos proxy, el lado servidor/esclavo,
> presets y file records, diagnóstico, licencia y el workflow real de Workbench. Ese es este focus.
>
> **Hallazgo que justifica el bootstrap**: la guía oficial de Tridium `docModbus` (**87 topics**) tiene
> **cero citas** en los 290 bloques del corpus — verificado `rg -il 'docModbus' *.md` → sin hits. B131 se
> construyó 100% por decompilación. Es la misma situación que el gap T9 del focus `tags`, donde la doc
> oficial aportó `[CERT-doc]` que el código no daba.
>
> Corpus en **INGLÉS** (convención desde B115; TARGETS.md fila 1). Numeración global de bloques
> (`niagara-mental-model-bloqueN.md`); máximo en disco al bootstrap = **B293** → este focus arranca en **B294**.
> Engram topic key: `research/niagara/modbus/{gaps,progress}`.

<!-- research-state.v1 -->
schema: research-state.v1
covered_blocks: 291
gaps_closed: 19
known_gaps: 22
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

focus: modbus
status: active
bootstrapped_on: 2026-07-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; siguiente libre derivado en vivo)

## Ángulo declarado (§b2)

Reconstruir el **driver Modbus de Niagara N4 como producto de integración**: qué componentes existe, qué
configura realmente un integrador, cómo un dato Modbus se convierte en un punto Niagara (y a la inversa,
en el lado esclavo), y qué falla. El eje transversal: **Niagara es a la vez maestro y esclavo Modbus**, y
las dos mitades comparten `modbusCore` — cuánto de la implementación es realmente simétrica.

Explícitamente FUERA de alcance (ya cerrado): la codificación en el cable. Ver [B131].

## Pre-flight e2 — existencia + tamaño MEDIDO (no estimado)

Fuente: `/home/cristian/modules/Prototipos/modulos/organized/`. Conteo sobre el pipeline **vineflower**
(canónico), colapsando los pipelines duplicados `decompiled/` + `procyon/` que inflaban el raw `.java` a
**564** y habrían mis-dimensionado el backlog (METHODOLOGY §13).

| Módulo | `-rt` | `-wb` | `-ux` | Total |
|---|---|---|---|---|
| `modbusCore` | 92 | 5 | 6 | **103** |
| `modbusTcp` | 11 | 2 | 3 | **16** |
| `modbusAsync` | 6 | 1 | 2 | **9** |
| `modbusSlave` | 7 | 1 | 2 | **10** |
| `modbusTcpSlave` | 7 | 1 | 2 | **10** |
| `modbusTcpSlaveMigrator` | — | 1 | — | **1** |
| **Driver Tridium** | **123** | **11** | **15** | **149** |
| `honeywellModbusDeviceManager` | 8 | 6 | — | **14** |
| `honeywellModbusSmartSensor` | 20 | 5 | — | **25** |
| **Total con OEM** | | | | **188** |

`modbusCore-rt` concentra el **62 %** de las clases del driver: es el módulo real, y los 5 restantes son
plumbing de transporte. Desglose por paquete (`modbusCore-rt`, 92 clases): `point` 14 · `messages` 13 ·
`client/datatypes` 11 · `enums` 10 · `client/point` 10 · `server/messages` 7 · `server/point` 6 ·
`datatypes` 4 · raíz 4 · `client` 2 · `client/enums` 2 · `server` 2 · `server/datatypes` 3 ·
`server/util` 2 · `util` 2 · `ui`/`client/ui`/`server/ui` 5.

**Fuente documental** (confirmada, preservada): guía oficial `docModbus` N4.14 — 87 topics HTML del
`docModbus-doc.jar` (sha256 `0aca2127…`), consolidados en
`sources/manuals/docModbus-N4.14-guide.md` (177 KB) y registrados en `sources/SOURCES.md`.
Además: `niagara-help/docs-text/TR100_Modbus_Integration_Guide_31-00748.txt` (guía OEM Honeywell TR100).

**Fuente original Tridium** (`docSource`): NO cubre `com.tridium.modbus*` — la evidencia primaria de código
es el decompilado vineflower `[CERT]` más los `bajadoc` de `devguide/modbusCore-rt`.

## Gap-backlog (prioritized)

| Priority | Gap | Artifact / source | Status |
|---|---|---|---|
| high | **M1** — Arquitectura del driver: 5 tipos de red, jerarquía `BModbusNetwork`→client/server, mapa de módulos, y qué dice la doc oficial vs el código | `docModbus` §Architecture/§Modules/§Components + `modbusCore-rt` raíz + `client`/`server` | **COVERED → B294** |
| high | **M2** — Configuración de red y de device: propiedades reales del property sheet, override network→device, base addresses, `BFlexAddress`, ping sintético | `docModbus` §NetworkConfiguration/§DeviceConfiguration/§Configuring* + `BModbusClientConfig`, `BModbusDevice`, `BFlexAddress` | **COVERED → B296** |
| high | **M3** — Modelo de puntos CLIENTE: los 6 ProxyExt, `BFlexAddress`, los 10 enums de datatype/byte-order, y cómo un registro se vuelve un `BStatusNumeric` | `modbusCore-rt/client/point` (10) + `point` (14) + `enums` (10) + `docModbus` §CreatingClientProxyPoints/§NewPointTypeWindow | **COVERED → B297** |
| high | **M4** — El lado SERVIDOR/esclavo: cómo la station EXPONE datos como slave Modbus; simetría real vs aparente con el cliente | `modbusCore-rt/server/**` (18) + `modbusSlave-rt` (7) + `modbusTcpSlave-rt` (7) + `docModbus` §ServerslaveConfiguration/§ModbusSlaveDevice | **COVERED → B298** |
| medium | **M5** — Presets, file records (FC 20/21) y string records: la superficie que B131 dejó como "esqueleto estático" (gap P1-fc del focus protocols) | `BModbusClientPreset*`, `BModbusFileRecord`, `BModbusStringRecord` + `docModbus` §AddingClientPresets/§AddingClientFileRecords | **COVERED → B299** |
| medium | **M6** — Diagnóstico y modos de fallo: exception status, comm status, debugging de mensajes, troubleshooting oficial | `BModbusClientExceptionStatus`, `BCommStatus(Enum)`, `ModbusErrorCodes`, `ModbusException` + `docModbus` §ExceptionResponses/§DebuggingMessages/§Troubleshooting | **COVERED → B300** |
| medium | **M7** — Licencia y límites operativos del feature `modbus` (cuántos devices/puntos/puertos) y qué habilita la licencia real del cliente | `docModbus` §LimitsImposedByTheModbusLicenses + `licenses/*.license` del install + comprobación en código | **COVERED → B301** (cierra además M1-lic y M1-gw) |
| medium | **M8** — El workflow de Workbench: device manager, point manager, discovery (¿existe?), y las 6 ventanas documentadas | `modbusCore-wb`/`-ux` (11) + `modbusTcp/Async/Slave -wb` + `docModbus` §Plugins/§Windows/§New*Window | **COVERED → B304** |
| low | **M9** — Los 2 módulos OEM Honeywell sobre Modbus (`honeywellModbusDeviceManager` 14, `honeywellModbusSmartSensor` 25): qué agregan sobre el driver base y qué queda sin cubrir tras B94/B95/B250 | `honeywellModbus*` + `TR100_Modbus_Integration_Guide_31-00748.txt` | pending |
| low | **M10** — `modbusTcpSlaveMigrator` (1 clase): qué migra, desde qué versión, y por qué el slave TCP necesitó un migrador | `modbusTcpSlaveMigrator-wb` + B25 (única mención previa) | pending |
| medium | **M12** — `ModbusTcpRxDriver` (358 líneas, abierto por B295 como M11-b): socket manager, política de reconexión, matching de transaction-id contra el contador de errores de red, manejo de frames parciales | `modbusTcp-rt/comm/ModbusTcpRxDriver.java` + los contadores de `BModbusNetwork` (B294 §294.7) | **COVERED → B305** (cierra además M2-b) |
| medium | **M18** — Write-through del SERVIDOR: cómo `BModbusServerProxyExt` y los puntos de la station pueblan los 4 `IntHashMap`, y qué pasa cuando un maestro escribe un coil que además maneja un punto (¿último que escribe gana, o el punto es autoritativo?). Arrastra M4-b desde B298→B302→B303 | `modbusCore-rt/server/point/**` | **COVERED → B306** |
| low | **M22** — Qué hilo del engine de Niagara invoca `updateOutput`/`writeDesired` sobre un proxy extension. Zanjaría de plano el condicional de B311 §311.4. NO es específico de Modbus: es una pregunta de framework (`BTuningPolicy`/threading del engine) que serviría a todos los focuses de driver | `driver-rt` + `baja` en docSource | pending |
| low | **M21** — Semántica de orden de la cola del `Worker`/`Queue` de Baja: ¿es FIFO? ¿un `dispatch()` concurrente puede colarse adelante? Hace falta para decir algo sobre EQUIDAD entre devices en una red serializada. Abierto por B308 §308.4 | `docSource` `javax/baja/util/{Worker,Queue}.java` | **COVERED → B312** |
| low | **M20** — Thread-safety de los 4 `IntHashMap` del servidor: las escrituras del maestro llegan por el hilo Rx y las de los puntos por el hilo del engine; NO se observó sincronización en ninguno de los dos paths. Requiere `javax.baja.nre.util.IntHashMap` + el contrato de threading de `updateOutput`. Abierto por B306 §306.5 como PREGUNTA ABIERTA, no como afirmación de defecto | `nre.jar` + `basicDriver-rt` | **COVERED → B311** (parcialmente determinable) |
| low | **M19** — Layout de la respuesta de EXCEPCIÓN: qué significa `byteCount` en un frame con el bit 7 del function code puesto, y dónde se escribe realmente el código de excepción. Abierto por B303 §303.5 (observación registrada, interpretación deferida a propósito) | `modbusCore-rt/messages/ModbusResponse` + B131 §131.4 | **COVERED → B307** |
| medium | **M17** — El layout PDU de FC 20/21 (sub-requests, reference type 6, framing por record). B131 §131.4 documentó el esqueleto del request; B299 cubrió el modelo de componentes pero NO los bytes. Abierto por B299 como M5-a | `modbusCore-rt/messages/Modbus{Read,Write}FileRequest` + `server/messages/*File*` | **COVERED → B310** |
| medium | **M16** — El path de SERVICIO de peticiones del esclavo (`server/messages/`, 7 clases: read/write, file read/write, FC 23 write-read): cómo se valida un PDU entrante contra los rangos y qué código de excepción devuelve una dirección fuera de rango. Abierto por B298 como M4-a | `modbusCore-rt/server/messages/**` + los dos `ModbusUnsolicitedReceive` | **COVERED → B303** |
| medium | **M15** — El path de LECTURA/decode por tipo de punto: `devicePoll(entry)` rebanando el buffer compartido + el camino `readUnsubscribed` de punto individual. Abierto por B297 como M3-a | `modbusCore-rt/client/point/*ProxyExt` | **COVERED → B302** |
| medium | **M14** — Los ajustes de la LÍNEA serial en sí (`serialPortConfig` = `BSerialHelper` de `serial-rt`, fuera de los jars Modbus): baud, paridad, bits, y cómo interactúan `maxRxInterCharacterDelay`/`minRxFrameEnd` con la regla de silencio de 3.5 caracteres de RTU. Abierto por B296 como M2-a | `serial-rt` + B294 §294.4 | **COVERED → B309** |
| medium | **M13** — La capa `Comm`/`dispatch` de `basicDriver` (abierto por B295 como M11-c): ¿`dispatch()` + `getResponse(0)` serializa por Comm o permite pipelining? qué significa el argumento `0` | `basicDriver-rt/com/tridium/basicdriver/comm/**` | **COVERED → B308** |
| high | **M11** — **El MOTOR de adquisición**: cómo el driver convierte N puntos en el mínimo de transacciones Modbus — poll groups, coalescing de registros contiguos, scheduling/tuning policy, el hilo Tx/Rx y la cola de mensajes. Es la pregunta "cómo obtiene los datos rápido" | `BModbusClientPollGroup`, `BModbusClientPointDeviceExt`, `modbusTcp/comm/ModbusTcp{Tx,Rx}Driver`, `modbusAsync/comm/*` + `docModbus` §ConfiguringForPolling/§ClientOperations | **COVERED → B295** |

## Pistas ya levantadas para M11 (scouting, NO son hallazgos cerrados)

Reconocimiento hecho el 2026-07-30 al priorizar M11 a pedido del usuario. Se registran acá para que la
iteración que abra M11 no las re-derive; **ninguna está cerrada ni tiene bloque**, y la última es una
HIPÓTESIS que debe falsarse antes de entrar a un bloque:

- El agrupamiento lo produce la acción `learnOptimumDevicePollConfig` sobre `BDevicePollConfigTable`
  (`BDevicePollConfigTable.java:23-33`), que delega en
  `BModbusClientDevice.getOptimumDevicePollConfigEntryList()` (`BModbusClientDevice.java:683`).
- El algoritmo: separa los puntos en 4 listas por tipo de registro, ordena por dirección absoluta y forma
  runs de direcciones **estrictamente consecutivas** (`difference != 1` → `break`). Un run de 1 no genera
  entry — ese punto se sigue polleando individualmente. Offsets de reconstrucción: `+40001` holding,
  `+30001` input, `+1` coil (`BModbusClientDevice.java:709-818`).
- Los 4 bloques del algoritmo son copy-paste literal, y `getActiveXxxPollEntries()` /
  `getPossibleXxxPollEntries()` son pares de métodos con cuerpo idéntico salvo `synchronized`
  (`BDevicePollConfigTable.java:62-204`).
- ~~**HIPÓTESIS a falsar en M11**: el algoritmo no impone tope de tamaño de grupo…~~ → **REFUTADA por B295
  §295.4.** El clamp EXISTE, pero no en `ModbusMessageConst` (esas dos constantes efectivamente están
  muertas): está en `BModbusClientDevice.readRegisters` (`maxReadSize = 125 - 125 % minReadSize`) y
  `readStatusRegisters` (`maxReadSize = 2000`), ambos con un `do…while` que FRAGMENTA la petición. Un run de
  9999 no produce un PDU ilegal: produce 80 transacciones. **Hallazgo mejor que la hipótesis**: en modo
  ASCII (`modbusMode == 0`) el driver PARTE su propio techo a la mitad (125→62, 2000→1000).

## Coverage

- **Covered blocks (this focus)**: 2 — B294 (M1, arquitectura del driver + 2 correcciones a la doc oficial),
  B295 (M11, motor de adquisición: devicePoll/pointPoll, coalescing, fragmentación, threading),
  B296 (M2, superficie de configuración: override de un solo switch, base addresses, `BFlexAddress`, ping sintético),
  B297 (M3, modelo de puntos cliente: 6 ProxyExt, 3 enums de tipo de registro, 8 datatypes, permutaciones de byte order),
  B298 (M4, lado servidor/esclavo: mapa en memoria, 4 rangos declarados, persistencia asimétrica, `criticalData`),
  B299 (M5, path de escritura + presets + file records: escritura multi-registro NO atómica por defecto),
  B300 (M6, diagnóstico: el signo del código de error indica el origen del fallo; contadores de red no alarmables),
  B301 (M7, licencia: NO existe un feature `modbus`, son cuatro; cierra M1-lic y M1-gw),
  B302 (M15, path de lectura: slicing del buffer compartido, códigos 102/103, ciclo de suscripción),
  B303 (M16, servicio de peticiones del esclavo: dispatcher por function code; FC 23 muerto en el servidor),
  B304 (M8, capa Workbench: 6 device managers, point manager de 1299 líneas, y CERO discovery en todo el driver),
  B305 (M12, `ModbusTcpRxDriver`: máquina de 3 estados, packet-mode vs byte-mode, length leído de 1 byte),
  B306 (M18, write-through del servidor: último que escribe gana, sin arbitraje; blob de persistencia reconstruido por byte),
  B307 (M19, respuesta de excepción: `byteCount` ES el código de excepción — §14 corrige B303),
  B308 (M13, capa de dispatch: `wait(0)` = esperar para siempre; el dispatcher es POR RED y de UN hilo → los envíos se SERIALIZAN — §14 matiza B295),
  B309 (M14, framing serial: el timing RTU SÍ está en Java, pero con umbrales en ms FIJOS, no la regla t3.5 relativa al baud),
  B310 (M17, PDU de FC 20/21: un solo sub-request hard-codeado, reference type 6, byte count calculado distinto en read y write),
  B311 (M20, thread-safety: `IntHashMap` sin garantías + maestro en hilo dedicado + cero locks → condicional fuerte, NO afirmación de defecto),
  B312 (M21, cola del dispatcher: FIFO por javadoc, `enqueue` nunca `push` → orden garantizado; tope 256 y `QueueFullException` sin capturar).
- **Coverage metric**: 19 / 22 backlog items closed.
- **Last iteration**: 2026-07-30 — M21 cerrado (B312).

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps uncovered |
|---|---|---|---|---|---|
| 1 | 2026-07-30 | M1 arquitectura del driver | B294 | no · inline | M1-lic (¿el `port.limit` del ejemplo de licencia aplica al feature `modbus` o es copy-paste de MS/TP? → se investiga en M7); M1-gw (`BModbusTcpGateway` es un device de nivel-red, no una red: verificar cómo se cuenta para la licencia y para el poll scheduler → M2/M7) |
| 19 | 2026-07-30 | M21 orden de la cola del dispatcher | B312 | no · inline | ninguno nuevo. FIFO confirmado por javadoc original + `BBasicWorker.post()` usa `enqueue` (nunca `push`, que sí existe) → **ningún device puede adelantarse a otro**: la serialización es JUSTA, no sólo serial. Hallazgo colateral: tope de cola **256** y `QueueFullException` que `post()` NO captura |
| 18 | 2026-07-30 | M20 thread-safety de los mapas del servidor | B311 | no · inline | **M22** (qué hilo del engine llama `updateOutput` — zanjaría el condicional). Cerrado como **parcialmente determinable**: medido que `IntHashMap` no da garantías (fuente original, 3 queries sobre las 372 líneas), que el maestro escribe desde el hilo `ModTcpSlave:UnsolRcv` y que no hay locks; NO medido el hilo del lado engine → conclusión CONDICIONAL a propósito. Colateral: **segundo** `System.out.println` de depuración shippeado |
| 17 | 2026-07-30 | M17 layout PDU de FC 20/21 | B310 | no · inline | ninguno nuevo. El resto de `P1-fc` (comportamiento VIVO de FC20/21) queda bajo la disposición `P1-dyn` que ya trackea RESEARCH-STATE-protocols.md — NO se duplica acá |
| 16 | 2026-07-30 | M14 línea serial / framing RTU | B309 | no · inline | ninguno nuevo. **Resultado POSITIVO donde B279 (MS/TP) fue negativo**: el timing de framing RTU sí es alcanzable en Java. Hallazgo: Tridium usa umbrales en ms FIJOS (`minRxFrameEnd` 20 ms, `maxRxInterCharacterDelay` 50 ms) en vez de la regla t3.5 relativa al baud — a 115200 eso son ~20 ms de latencia añadida por frame |
| 15 | 2026-07-30 | M13 capa Comm/dispatch | B308 | no · inline | **M21** (orden de la cola del Worker). **§14: matiza B295 §295.7** — el inventario de sockets/hilos por device está bien, pero el path de ENVÍO pasa por un dispatcher único por RED (`BBasicWorker`, un hilo) y `execute()` bloquea en `transmit()`: los envíos se serializan. Los sockets por device dan AISLAMIENTO, no throughput paralelo. Puntero añadido en B295 |
| 14 | 2026-07-30 | M19 layout de la respuesta de excepción | B307 | no · inline | ninguno. **§14: corrige B303 §303.5** — la "inconsistencia" de `byteCount` 1 vs 2 no era un defecto: en un frame de excepción ese campo ES el código Modbus (1=Illegal Function, 2=Illegal Data Address) y `data` no se serializa. Puntero añadido en B303 |
| 13 | 2026-07-30 | M18 write-through del servidor | B306 | no · inline | **M20** (thread-safety de los 4 IntHashMap — planteado como pregunta abierta, NO como defecto). **Cierra el arrastre de M4-b** abierto por B298 y postergado por B302 y B303 |
| 12 | 2026-07-30 | M12 `ModbusTcpRxDriver` | B305 | no · inline | ninguno nuevo. **Cierra M2-b** (`rxProcessMode` = packet-mode vs byte-mode). Aporta a **M19**: en recepción el código de excepción está en el offset 8 del frame TCP, pero M19 sigue abierto (falta el lado emisor) |
| 11 | 2026-07-30 | M8 workflow de Workbench | B304 | no · inline | ninguno nuevo. **Hallazgo negativo mayor**: el driver Modbus NO tiene discovery (0 hits de `Discover`/`LearnJob` en todos los módulos; control positivo contra `bacnet-wb`) — es el hueco con forma de Modbus en B28 |
| 10 | 2026-07-30 | M16 servicio de peticiones del esclavo | B303 | no · inline | **M18** (write-through del servidor — M4-b sigue SIN cerrar, se arrastra explícitamente); **M19** (layout de la respuesta de excepción, interpretación deferida) |
| 9 | 2026-07-30 | M15 path de lectura/decode | B302 | no · inline | ninguno nuevo. **M4-b re-scopeado a M16** (el servidor no tiene análogo de `devicePoll`); M5-b (`IPropertyValidator`) queda como ítem menor dentro de M16, no merece gap propio |
| 8 | 2026-07-30 | M7 licencia y límites | B301 | no · inline | ninguno nuevo. **CIERRA M1-lic** (el `port.limit` de la doc es de MS/TP, no aparece en ningún feature modbus) y **M1-gw** (el gateway TCP gasta la licencia `modbusTcp`, heredada y `final`) |
| 7 | 2026-07-30 | M6 diagnóstico y modos de fallo | B300 | no · inline | ninguno nuevo |
| 6 | 2026-07-30 | M5 escritura/presets/file records | B299 | no · inline | **M17** (layout PDU de FC 20/21); M5-b (`IPropertyValidator` del preset → **M15**). **Cierra la mitad WRITE de `P1-fc`** del focus protocols (B131) |
| 5 | 2026-07-30 | M4 lado servidor/esclavo | B298 | no · inline | **M16** (path de servicio de peticiones del esclavo); M4-b (write-through de puntos al mapa → se pliega a **M15**) |
| 4 | 2026-07-30 | M3 modelo de puntos cliente | B297 | no · inline | **M15** (path de lectura/decode por tipo de punto); M3-b (`StringProxyExt` ↔ file records → se pliega a **M5**) |
| 3 | 2026-07-30 | M2 superficie de configuración | B296 | no · inline | **M14** (ajustes de la línea serial: `BSerialHelper` de `serial-rt` vs la regla de 3.5 caracteres de RTU); M2-b (`rxProcessMode` en gateway y device TCP → se pliega a **M12**) |
| 2 | 2026-07-30 | M11 motor de adquisición | B295 | no · inline | M11-a (path de ESCRITURA: FC5/6 vs FC15/16, `usePresetMultipleRegister`/`useForceMultipleCoil`, `MAX_WRITE_DATA_SIZE` muerto → se pliega a **M5**); **M11-b** (`ModbusTcpRxDriver`, 358 líneas: socket manager, política de reconexión, matching de transaction-id, frames parciales — NO abierto); **M11-c** (¿`dispatch()`/`getResponse(0)` serializa por Comm o permite pipelining? qué significa el `0` — requiere leer la capa `Comm` de basicDriver) |

## Stop control

- Primary criterion: read-only-investigable backlog exhaustion (METHODOLOGY §8).
- Loop status: **ACTIVO**. investigable_open = 3.
- Blocked / requires-execution: ninguno todavía. Un eventual gap dinámico (verificar contra un dispositivo
  Modbus vivo) heredaría el gap **P1-dyn** ya abierto por B131 en `RESEARCH-STATE-protocols.md` — no se
  duplica acá.
