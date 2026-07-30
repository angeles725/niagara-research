# RESEARCH-STATE — focus: modbus (ACTIVO 1/11)

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
gaps_closed: 1
known_gaps: 11
investigable_open: 10
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
| high | **M2** — Configuración de red y de device: propiedades reales del property sheet, poll config por device, register ranges, timing/tuning, serial vs ethernet | `docModbus` §NetworkConfiguration/§DeviceConfiguration/§Configuring* + `BModbusClientConfig`, `BDevicePollConfigTable`, `BModbusRegisterRangeTable` | pending |
| high | **M3** — Modelo de puntos CLIENTE: los 6 ProxyExt, `BFlexAddress`, los 10 enums de datatype/byte-order, y cómo un registro se vuelve un `BStatusNumeric` | `modbusCore-rt/client/point` (10) + `point` (14) + `enums` (10) + `docModbus` §CreatingClientProxyPoints/§NewPointTypeWindow | pending |
| high | **M4** — El lado SERVIDOR/esclavo: cómo la station EXPONE datos como slave Modbus; simetría real vs aparente con el cliente | `modbusCore-rt/server/**` (18) + `modbusSlave-rt` (7) + `modbusTcpSlave-rt` (7) + `docModbus` §ServerslaveConfiguration/§ModbusSlaveDevice | pending |
| medium | **M5** — Presets, file records (FC 20/21) y string records: la superficie que B131 dejó como "esqueleto estático" (gap P1-fc del focus protocols) | `BModbusClientPreset*`, `BModbusFileRecord`, `BModbusStringRecord` + `docModbus` §AddingClientPresets/§AddingClientFileRecords | pending |
| medium | **M6** — Diagnóstico y modos de fallo: exception status, comm status, debugging de mensajes, troubleshooting oficial | `BModbusClientExceptionStatus`, `BCommStatus(Enum)`, `ModbusErrorCodes`, `ModbusException` + `docModbus` §ExceptionResponses/§DebuggingMessages/§Troubleshooting | pending |
| medium | **M7** — Licencia y límites operativos del feature `modbus` (cuántos devices/puntos/puertos) y qué habilita la licencia real del cliente | `docModbus` §LimitsImposedByTheModbusLicenses + `licenses/*.license` del install + comprobación en código | pending |
| medium | **M8** — El workflow de Workbench: device manager, point manager, discovery (¿existe?), y las 6 ventanas documentadas | `modbusCore-wb`/`-ux` (11) + `modbusTcp/Async/Slave -wb` + `docModbus` §Plugins/§Windows/§New*Window | pending |
| low | **M9** — Los 2 módulos OEM Honeywell sobre Modbus (`honeywellModbusDeviceManager` 14, `honeywellModbusSmartSensor` 25): qué agregan sobre el driver base y qué queda sin cubrir tras B94/B95/B250 | `honeywellModbus*` + `TR100_Modbus_Integration_Guide_31-00748.txt` | pending |
| low | **M10** — `modbusTcpSlaveMigrator` (1 clase): qué migra, desde qué versión, y por qué el slave TCP necesitó un migrador | `modbusTcpSlaveMigrator-wb` + B25 (única mención previa) | pending |
| high | **M11** — **El MOTOR de adquisición**: cómo el driver convierte N puntos en el mínimo de transacciones Modbus — poll groups, coalescing de registros contiguos, scheduling/tuning policy, el hilo Tx/Rx y la cola de mensajes. Es la pregunta "cómo obtiene los datos rápido" | `BModbusClientPollGroup`, `BModbusClientPointDeviceExt`, `modbusTcp/comm/ModbusTcp{Tx,Rx}Driver`, `modbusAsync/comm/*` + `docModbus` §ConfiguringForPolling/§ClientOperations | pending |

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
- **HIPÓTESIS a falsar en M11**: el algoritmo no impone tope de tamaño de grupo — `consecutivePointsToPoll`
  admite hasta **9999** por facets (`BDevicePollConfigEntry.java:52`), mientras el protocolo tope a 125
  holding registers por FC03. Las constantes `MAX_READ_DATA_SIZE = 255` / `MAX_WRITE_DATA_SIZE = 16`
  existen (`ModbusMessageConst.java:26-27`) pero **no tienen un solo consumidor** en los 5 jars del driver
  (`rg` sobre `modbusCore/Tcp/Async/TcpSlave/Slave-rt` → solo la declaración). Si no hay clamp aguas abajo,
  un run largo produciría un request ilegal. **Falta recorrer el path de construcción del mensaje antes de
  afirmarlo** — ver HARD RULE "RE-MEASURE A DRAMATIC NEGATIVE".

## Coverage

- **Covered blocks (this focus)**: 1 — B294 (M1, arquitectura del driver + mapa de módulos + 2 correcciones a la doc oficial).
- **Coverage metric**: 1 / 11 backlog items closed.
- **Last iteration**: 2026-07-30 — M1 cerrado (B294).

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps uncovered |
|---|---|---|---|---|---|
| 1 | 2026-07-30 | M1 arquitectura del driver | B294 | no · inline | M1-lic (¿el `port.limit` del ejemplo de licencia aplica al feature `modbus` o es copy-paste de MS/TP? → se investiga en M7); M1-gw (`BModbusTcpGateway` es un device de nivel-red, no una red: verificar cómo se cuenta para la licencia y para el poll scheduler → M2/M7) |

## Stop control

- Primary criterion: read-only-investigable backlog exhaustion (METHODOLOGY §8).
- Loop status: **ACTIVO**. investigable_open = 10.
- Blocked / requires-execution: ninguno todavía. Un eventual gap dinámico (verificar contra un dispositivo
  Modbus vivo) heredaría el gap **P1-dyn** ya abierto por B131 en `RESEARCH-STATE-protocols.md` — no se
  duplica acá.
