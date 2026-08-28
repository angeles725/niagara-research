# Block 582 — `BTemplateChannel`: a single-command Fox channel ("template") whose one circuit `upgradeTemplate` takes a `deployedSlotPath`, runs the UpgradeUtil job server-side, and STREAMS job-event "running" messages to a terminal complete/failed/canceled/error

**Session**: 2026-08-28
**Focus**: `template` (gap T6 — the BTemplateChannel Fox wire; deepens [Block 200 §200.4] channel existence)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of `BTemplateChannel`; dispatch, request field, and the streamed
response states token-verified inline.
**Primary sources** `[CERT]`:
- `organized/template/template-rt/vineflower/com/tridium/template/BTemplateChannel.java`.

**Scope**: the remote-upgrade transport. [Block 200 §200.4] noted the channel is Fox-registered and an
"upgradeTemplate" circuit exists; T6 opens the wire. Applies the [Block 513] Fox channel/circuit model and drives
[Block 579]'s UpgradeUtil. Does NOT re-open the upgrade transaction ([Block 579]) — transports it.

---

## 582.1 One channel, one circuit, no one-shot commands [CERT]

`BTemplateChannel extends BFoxChannel` `[CERT] :30`, registered under `CHANNEL_NAME = "template"` `[CERT] :32`.
The one-shot request path is deliberately closed: `process(FoxRequest)` `[CERT] :48-50` throws
`InvalidCommandException` for every command — the channel does NOTHING synchronously. All work is circuit-based:
`circuitOpened(FoxCircuit)` `[CERT] :53-58` dispatches exactly ONE command:
```java
if (command == "upgradeTemplate") this.upgradeTemplate(circuit);
else throw new InvalidCommandException(command);
```
So the entire remote surface of the template subsystem over Fox is a single streaming circuit — `upgradeTemplate`.

## 582.2 The `upgradeTemplate` circuit: request + streamed response [CERT]

`upgradeTemplate(circuit)` `[CERT] :62-129`:
1. **Request**: `BOrd deployedOrd = BOrd.make(request.getString("deployedSlotPath"))` `[CERT] :68` — the client
   sends the ORD/slot-path of the DEPLOYED template to upgrade.
2. **Guard**: if the deployed template component resolves null →
   `sendResponse(circuit, "…nullTemplateComponent…", "error")` `[CERT] :74-79` and stop.
3. **Run + stream**: it runs the upgrade as a JOB and streams progress back over the circuit — for each job
   event, `sendResponse(circuit, "…jobEvent…", "running")` `[CERT] :103-104`; on job end,
   `sendResponse(circuit, "…jobEnd…", "running")` `[CERT] :113`.
4. **Terminal state** `[CERT] :118-129`: the final `jobResult` is mapped from the job's end state —
   `"running"` / `"canceled"` / `"complete"` / `"failed"` — and sent as the last response.

So the wire is a **streaming-progress circuit**: one request (deployedSlotPath), then N `"running"` messages
carrying live job events (the `UpgradeUtil` save→transfer→restore progress, [Block 579]), terminated by exactly
one `complete`/`failed`/`canceled` (or an early `error`). This is what lets Workbench/browser show a live
progress bar while a remote template upgrade runs on the station.

## 582.3 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BTemplateChannel extends BFoxChannel, CHANNEL_NAME="template" | [CERT] | BTemplateChannel.java:30,32 | token-checked ✓ |
| 2 | process() throws InvalidCommandException for all commands (no one-shot path) | [CERT] | :48-50 | token-checked ✓ |
| 3 | circuitOpened dispatches exactly one command: upgradeTemplate | [CERT] | :53-58 | token-checked ✓ |
| 4 | Request carries deployedSlotPath; null component → "error" response | [CERT] | :68,74-79 | token-checked ✓ |
| 5 | Streams job events as "running" sendResponse; terminal state running/canceled/complete/failed | [CERT] | :103-129 | token-checked ✓ |

**Marker tally**: [CERT] ×5 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 5 rows token-verified
inline.

## Connections

- **[Block 200 §200.4]** — noted the channel + upgradeTemplate circuit existence; T6 opens the wire.
- **[Block 513]** — the Fox channel/circuit API this uses; **[Block 568]** (PV2) — the sibling provisioning
  channel that carries template ops across a fleet (which ultimately drives this circuit per station).
- **[Block 579]** (T5) — the UpgradeUtil job whose events this circuit streams.
- **[Block 511]** — BJob (the job whose state maps to the terminal running/canceled/complete/failed).

## Open gaps (this block)

- Whether a client can CANCEL a running upgrade mid-stream (a cancel circuit/flag) is not present here — the
  channel has only `upgradeTemplate`; cancellation would ride the job service, not this channel. Low value.
  Focus continues at T7 (TemplateManager resolution + memory scheme), the final gap.
