# Block 517 — `framework-drivers` FD11: `basicDriver` — the shared serial-driver base that 12 drivers ride, layering a message-dispatch/transaction engine (`BBasicNetwork`) + a serial binding (`BSerialNetwork`/`SerialComm`) + a network-state-gated poll scheduler on top of the generic driver framework, with a transport-agnostic `makeComm()` and NO built-in TCP

> **Focus:** `framework-drivers`, gap **FD11** — the recorded-not-seeded shared base flagged in [B500 §500.8]
> (mbus rides it). READ-ONLY, decompiled; no run. Markers §3. **REMITTANCE-checked GENUINE:** [B7] covers the
> GENERIC driver framework (`BDeviceNetwork`/`BDevice`/`BProxyExt`); `basicDriver` adds a distinct message
> engine + serial binding on top — uncovered.
> **Sources:** FUENTE 3 — `organized/basicDriver/basicDriver-rt/decompiled/com/tridium/basicdriver/…`. FUENTE 1 —
> [B7] (generic framework), [B500] (mbus, the exemplar), [modbus B294–B315] (modbusCore rides it). Evidence
> delegated to a `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline.

## §517.1 — What it adds over the generic framework `[CERT]`

`basicDriver` sits ON TOP of [B7]'s `BDeviceNetwork`/`BDevice`/`BProxyExt` and adds a request/response messaging
layer:

| Class | Base | Adds over [B7] |
|---|---|---|
| `BBasicNetwork` | `BLoadableNetwork` (abstract, `:61`) | the message-dispatch engine (§517.2); `abstract makeComm()` (`:215`) |
| `BSerialNetwork` | `BBasicNetwork` | serial binding: `serialPortConfig` (`BSerialHelper`) + `interMessageDelay` (`:51`) |
| `BBasicDevice` | `BLoadableDevice` (`:20`) | type-link `getNetworkType()→BBasicNetwork.TYPE` (`:28`) |
| `BBasicProxyExt` | `BProxyExt` (`:33`) | poll-subscription lifecycle (`readSubscribed` `:49`) + async write |
| `BBasicPollScheduler` | `BPollScheduler` (`:25`) | network-state gate in `doPoll` (`:36`) |
| `BCommPlugIn` | `BComponent` | a swappable `Comm` supplier component |

## §517.2 — The message-dispatch/transaction engine `[CERT]`

`BBasicNetwork` owns three Niagara workers (`dispatcher`/`worker`/`writeWorker`), a `BBasicPollScheduler`,
`retryCount`+`responseTimeout`, and the send API (`sendSync`/`sendAsync`/`sendAsyncWrite`). Transport is a `Comm`
(abstract `makeComm()` `:215`) composing `CommReceiver` (rx) / `CommTransmitter` (tx) / `CommTransactionManager`
(request↔response matched by message tag). `Comm.transmit(msg, timeout, retryCount)` runs the retry loop —
send → wait `responseTimeout` on a `CommTransaction` → retry `retryCount+1` times, counting `totalTimeoutMessages`;
unsolicited/untagged frames route to listeners. `[INFER]`: this is the reusable "poll a device, match the reply,
retry on timeout" machinery every serial fieldbus needs — [B7]'s generic network has only the component tree +
fault/status, none of this.

## §517.3 — Serial transport `[CERT]`

`BSerialNetwork` narrows the engine to serial: `serialPortConfig` (`BSerialHelper`: baud/data/stop/parity/port) +
`interMessageDelay` (`:51`), and implements `BISerialHelperParent` so the platform re-opens the port on config
change (`reopenPort`/`restartSerialNetwork`). `SerialComm` (extends `Comm`): `started()` opens the port and
launches a `SerialRcv:<name>` rx thread (priority 5); `performInterMessageDelay()` sleeps the deficit since the
last received message (min 10 ms) before each transmit — the fixed-ms gap [B309] noted Tridium uses instead of
RTU's baud-relative silence. `BSerialComm` (extends `BCommPlugIn`) is the same serial logic packaged as a
swappable component child rather than a subclass.

## §517.4 — Poll scheduler + proxy `[CERT]`

`BBasicPollScheduler.doPoll()` (`:36`) adds ONE concrete behaviour over the generic `BPollScheduler`: it **gates
polling on network state** — `shouldPoll = !isDisabled && !isDown && !isFault` — then calls `dev.poll()`, and
auto-unsubscribes a pollable on `NotRunningException`. The fast/normal/slow timing tiers live in the parent
`BPollScheduler` ([B7] side). `BBasicProxyExt.readSubscribed()` (`:49`) subscribes the proxy to the scheduler
directly (if `BIBasicPollable`) or via a `BBasicPollGroup` (`:60`); `write()` routes through
`network.postWrite(BasicWriteAsyncRequest)` — keeping writes OFF the poll thread.

## §517.5 — Who rides it + the no-TCP contract `[CERT]`

**12 driver modules extend basicDriver** (grep-measured): serial via `BSerialNetwork` — `mbus` ([B500]),
`aaphp`, `aapup`, `mcquay`, `andoverAC256`, `flexSerial`; transport-agnostic via `BBasicNetwork` directly —
`modbusCore` ([modbus B294–B315]), `ccn`, `nrio`, `tls`, `clPanelBus`, `honPlantControllerHMI`. `[CERT negative]`
**basicDriver ships NO TCP transport** (grep for tcp/socket classes = 0): `BBasicNetwork` is transport-agnostic
(abstract `makeComm()`), `BSerialNetwork` is the serial-only convenience sub-base, and **TCP is each driver's own
concern** — mbus supplied its own `MbusSocketComm` ([B500]), modbusCore/tls extend `BBasicNetwork` directly with
their own socket `Comm`. `[INFER]`: the split explains why [B500] found mbus with both a serial `SerialComm` (from
basicDriver) and a hand-rolled `MbusSocketComm` (its own).

## §517.6 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | basicDriver adds a messaging layer over B7's generic framework (6 classes) | `[CERT]` | table §517.1 | PASS |
| 2 | BBasicNetwork abstract=BLoadableNetwork; abstract makeComm; dispatcher/worker/writeWorker + Comm/transaction retry engine | `[CERT]` | `BBasicNetwork.java:61,215` | PASS |
| 3 | BSerialNetwork adds BSerialHelper serialPortConfig + interMessageDelay; SerialComm rx-thread + performInterMessageDelay | `[CERT]` | `BSerialNetwork.java:51` | PASS |
| 4 | BBasicPollScheduler.doPoll gates on !disabled/!down/!fault; BBasicProxyExt readSubscribed + async postWrite | `[CERT]` | `BBasicPollScheduler.java:36`; `BBasicProxyExt.java:49` | PASS |
| 5 | 12 modules ride it; basicDriver ships NO TCP (transport-agnostic makeComm; TCP is per-driver) | `[CERT]`/`[CERT neg]` | grep=12 modules; tcp-classes=0 | PASS |

**Tally:** 5 claims — all `[CERT]`/`[CERT negative]` load-bearing + 2 `[INFER]` (reusable-engine rationale, TCP-split
explanation). Block TYPE = **EVIDENCE**; FD11 CLOSED. REMITTANCE-checked genuine vs [B7]. All load-bearing tokens
re-verified inline.

## §517.7 — Connections & focus status

- **The shared base under [B500] mbus** and 11 other drivers (incl. modbusCore of the closed [modbus B294–B315]
  focus) — closes the recorded-not-seeded item from [B500 §500.8].
- **Extends [B7]'s generic driver framework** with the request/response engine + serial binding; the fixed-ms
  `interMessageDelay` ties to [B309] (Tridium's serial framing choice).
- **Transport model:** `BBasicNetwork` transport-agnostic (`makeComm`), `BSerialNetwork` serial, TCP per-driver —
  the pattern that made [B500]'s dual serial+TCP mbus comm make sense.
- **Focus status:** `framework-drivers` reopened for one recorded sub-item (FD11 = B517). Remaining
  recorded-not-seeded: 2 optional §12 live checks (requires-execution, not investigable). All static
  framework-driver surface now covered.
