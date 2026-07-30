# RESEARCH-STATE — focus: modbus (ACTIVO 3/14)

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
gaps_closed: 3
known_gaps: 14
investigable_open: 11
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
| high | **M3** — Modelo de puntos CLIENTE: los 6 ProxyExt, `BFlexAddress`, los 10 enums de datatype/byte-order, y cómo un registro se vuelve un `BStatusNumeric` | `modbusCore-rt/client/point` (10) + `point` (14) + `enums` (10) + `docModbus` §CreatingClientProxyPoints/§NewPointTypeWindow | pending |
| high | **M4** — El lado SERVIDOR/esclavo: cómo la station EXPONE datos como slave Modbus; simetría real vs aparente con el cliente | `modbusCore-rt/server/**` (18) + `modbusSlave-rt` (7) + `modbusTcpSlave-rt` (7) + `docModbus` §ServerslaveConfiguration/§ModbusSlaveDevice | pending |
| medium | **M5** — Presets, file records (FC 20/21) y string records: la superficie que B131 dejó como "esqueleto estático" (gap P1-fc del focus protocols) | `BModbusClientPreset*`, `BModbusFileRecord`, `BModbusStringRecord` + `docModbus` §AddingClientPresets/§AddingClientFileRecords | pending |
| medium | **M6** — Diagnóstico y modos de fallo: exception status, comm status, debugging de mensajes, troubleshooting oficial | `BModbusClientExceptionStatus`, `BCommStatus(Enum)`, `ModbusErrorCodes`, `ModbusException` + `docModbus` §ExceptionResponses/§DebuggingMessages/§Troubleshooting | pending |
| medium | **M7** — Licencia y límites operativos del feature `modbus` (cuántos devices/puntos/puertos) y qué habilita la licencia real del cliente | `docModbus` §LimitsImposedByTheModbusLicenses + `licenses/*.license` del install + comprobación en código | pending |
| medium | **M8** — El workflow de Workbench: device manager, point manager, discovery (¿existe?), y las 6 ventanas documentadas | `modbusCore-wb`/`-ux` (11) + `modbusTcp/Async/Slave -wb` + `docModbus` §Plugins/§Windows/§New*Window | pending |
| low | **M9** — Los 2 módulos OEM Honeywell sobre Modbus (`honeywellModbusDeviceManager` 14, `honeywellModbusSmartSensor` 25): qué agregan sobre el driver base y qué queda sin cubrir tras B94/B95/B250 | `honeywellModbus*` + `TR100_Modbus_Integration_Guide_31-00748.txt` | pending |
| low | **M10** — `modbusTcpSlaveMigrator` (1 clase): qué migra, desde qué versión, y por qué el slave TCP necesitó un migrador | `modbusTcpSlaveMigrator-wb` + B25 (única mención previa) | pending |
| medium | **M12** — `ModbusTcpRxDriver` (358 líneas, abierto por B295 como M11-b): socket manager, política de reconexión, matching de transaction-id contra el contador de errores de red, manejo de frames parciales | `modbusTcp-rt/comm/ModbusTcpRxDriver.java` + los contadores de `BModbusNetwork` (B294 §294.7) | pending |
| medium | **M14** — Los ajustes de la LÍNEA serial en sí (`serialPortConfig` = `BSerialHelper` de `serial-rt`, fuera de los jars Modbus): baud, paridad, bits, y cómo interactúan `maxRxInterCharacterDelay`/`minRxFrameEnd` con la regla de silencio de 3.5 caracteres de RTU. Abierto por B296 como M2-a | `serial-rt` + B294 §294.4 | pending |
| medium | **M13** — La capa `Comm`/`dispatch` de `basicDriver` (abierto por B295 como M11-c): ¿`dispatch()` + `getResponse(0)` serializa por Comm o permite pipelining? qué significa el argumento `0` | `basicDriver-rt/com/tridium/basicdriver/comm/**` | pending |
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
  B296 (M2, superficie de configuración: override de un solo switch, base addresses, `BFlexAddress`, ping sintético).
- **Coverage metric**: 3 / 14 backlog items closed.
- **Last iteration**: 2026-07-30 — M2 cerrado (B296).

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps uncovered |
|---|---|---|---|---|---|
| 1 | 2026-07-30 | M1 arquitectura del driver | B294 | no · inline | M1-lic (¿el `port.limit` del ejemplo de licencia aplica al feature `modbus` o es copy-paste de MS/TP? → se investiga en M7); M1-gw (`BModbusTcpGateway` es un device de nivel-red, no una red: verificar cómo se cuenta para la licencia y para el poll scheduler → M2/M7) |
| 3 | 2026-07-30 | M2 superficie de configuración | B296 | no · inline | **M14** (ajustes de la línea serial: `BSerialHelper` de `serial-rt` vs la regla de 3.5 caracteres de RTU); M2-b (`rxProcessMode` en gateway y device TCP → se pliega a **M12**) |
| 2 | 2026-07-30 | M11 motor de adquisición | B295 | no · inline | M11-a (path de ESCRITURA: FC5/6 vs FC15/16, `usePresetMultipleRegister`/`useForceMultipleCoil`, `MAX_WRITE_DATA_SIZE` muerto → se pliega a **M5**); **M11-b** (`ModbusTcpRxDriver`, 358 líneas: socket manager, política de reconexión, matching de transaction-id, frames parciales — NO abierto); **M11-c** (¿`dispatch()`/`getResponse(0)` serializa por Comm o permite pipelining? qué significa el `0` — requiere leer la capa `Comm` de basicDriver) |

## Stop control

- Primary criterion: read-only-investigable backlog exhaustion (METHODOLOGY §8).
- Loop status: **ACTIVO**. investigable_open = 11.
- Blocked / requires-execution: ninguno todavía. Un eventual gap dinámico (verificar contra un dispositivo
  Modbus vivo) heredaría el gap **P1-dyn** ya abierto por B131 en `RESEARCH-STATE-protocols.md` — no se
  duplica acá.
