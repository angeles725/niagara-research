# Block 315 — SYNTHESIS of the `modbus` focus (B294–B314): a driver whose defaults are wrong, whose concurrency is narrower than it looks, and whose documentation describes a different product

> Focus **modbus** — closing synthesis. 21 evidence blocks, 22 gaps, one prioritized backlog exhausted.
> This block consolidates the cross-cutting threads and **remits** to the blocks that established each
> finding; it re-derives nothing. READ-ONLY. Corpus language: ENGLISH.
>
> Scope of the focus: the **driver** — component tree, configuration, point model, acquisition engine,
> server side, diagnostics, licensing, Workbench layer, OEM layer. NOT the wire encoding, which
> [Block 131] closed in the `protocols` focus.
>
> Markers: this is a SYNTHESIS block. Every `[CERT]` here is a **remission** to a block that verified it —
> the citation is `[Block N] §N.x`, not a fresh `file:line`. `[INFER]` marks connections drawn **across**
> blocks that no single block asserts.
>
> Layer 26 (Communication protocols — driver focus). Consolidates [Block 294]–[Block 314].

---

## 315.1 — What the focus covered

| Area | Blocks |
|---|---|
| Architecture, modules, palettes, licensing | [294], [301] |
| Configuration surface (network → device → point) | [296], [297] |
| Acquisition engine (grouping, fragmentation, threading) | [295], [302], [305], [308], [309], [312] |
| Write path, presets, file records | [299], [310] |
| Server / slave side | [298], [303], [306], [311] |
| Diagnostics | [300] |
| Workbench + OEM layers | [304], [314] |
| Migration | [313] |

Bootstrapped 2026-07-30 on a measured surface of **188 distinct classes** (149 Tridium + 39 Honeywell OEM),
after collapsing duplicate decompiler pipelines that inflated the raw count to 564 `[Block 294]`.

The focus also produced the corpus's **first citations** of two official sources: the 87-topic `docModbus`
guide (zero prior citations) `[Block 294]` and the 2082-line TR100 Modbus Integration Guide `[Block 314]`
§314.7.

## 315.2 — Thread 1: the client is configured, the server is policed

The client/server split is not symmetric, and the asymmetry is structural rather than incidental.

| Dimension | Client | Server |
|---|---|---|
| Concrete `ProxyExt` types | **6** | **3** — no bit-field, no enum, no string `[Block 294]` §294.4, `[Block 298]` §298.5 |
| Own network properties | **3** | **0** `[Block 294]` §294.4 |
| What the network level adds | configuration | **policing** — a `Set<Integer>` of addresses that faults duplicates with `"Duplicate Device Address"` `[Block 298]` §298.2 |
| Acquisition | a poll engine `[Block 295]` | **none** — answers from four in-memory maps `[Block 298]` §298.1, `[Block 303]` §303.4 |
| Address model | two addresses (`dataAddress` + `absoluteAddress` with base offsets) `[Block 302]` §302.4 | one — ranges are declared instead `[Block 306]` §306.2 |

`[INFER]` the unifying reading: a **master is configured** (where to reach, how to interpret, how often),
a **slave is bounded** (which addresses exist, who may collide). Serving is O(range), not O(points) — which
is why the server needs no engine, and why its missing point types cost less than they appear to: string
data rides the file-record channel instead `[Block 299]` §299.4.

## 315.3 — Thread 2: four defaults that are wrong out of the box

The single most actionable output of the focus. All four ship as the unsafe or slow option.

| Default | Effect | Block |
|---|---|---|
| `usePresetMultipleRegister = false` | **a 32/64-bit write is NOT atomic** — FC 6 in a loop, N sequential messages, a reader sees a torn value | `[Block 299]` §299.2 |
| `criticalData = false` | a setpoint a remote master wrote is **losable on an ungraceful shutdown** (`Flags.NON_CRITICAL`) | `[Block 298]` §298.4 |
| address format = `hex` | typing the familiar `40001` yields **262145** | `[Block 296]` §296.3 |
| `dataSource = pointPoll` | **one Modbus transaction per point**, until a poll-config entry covers it | `[Block 295]` §295.1 |

`[INFER]` the first is a **correctness** setting the official guide presents as one of four routine things to
configure `[Block 299]` §299.2 — the guide gives no hint that leaving it alone corrupts multi-register
writes. The other three are performance or durability traps. None is documented as a trap.

`[INFER]` a fifth belongs on the list by consequence rather than by default value: base-address validation
**only fires in `modbus` format** `[Block 296]` §296.4 — so in the default `hex` format, no validation
happens at all. The unsafe default disables the safety net that would have caught it.

## 315.4 — Thread 3: everything serialises

The focus's most-corrected thread, and the one where a first reading proved wrong.

| Layer | Constraint | Block |
|---|---|---|
| Dispatch | **one worker thread per NETWORK**, blocking until response or timeout | `[Block 308]` §308.3 |
| Queue | FIFO, `enqueue` never `push`, cap **256**, `QueueFullException` uncaught | `[Block 312]` |
| Request size | fragmented at 125 registers / 2000 coils; **halved in ASCII** | `[Block 295]` §295.4 |
| Serial framing | fixed **ms** thresholds, not baud-relative t3.5 — ~20 ms added per frame at high baud | `[Block 309]` §309.3 |
| Rate | the group polls at its **fastest** member's rate (contagion) | `[Block 295]` §295.6 |

`[INFER]` the composite picture: per-device sockets and Rx threads `[Block 295]` §295.7, `[Block 305]`
§305.1 buy **isolation** — a dead device blocks only its own receive thread and cannot corrupt another's
framing — but **not throughput**, because the outbound stream funnels through one dispatcher. Raising baud
on a serial trunk buys less than the number suggests, for the same reason plus the fixed frame-end wait.

`[INFER]` the mitigating counterpart, from `[Block 312]` §312.2: the serialisation is **fair**. No device can
overtake another; a slow device delays but cannot starve.

## 315.5 — Thread 4: the code has a signature, and it repeats

Six independent instances of the same engineering pattern, each found while investigating something else:

| Instance | Block |
|---|---|
| Four literally copy-pasted loops in `Learn Optimum Device Poll Config` | `[Block 295]` §295.3 |
| `getActiveXxx`/`getPossibleXxx` accessor pairs with identical bodies | `[Block 295]` §295.3 |
| Two near-twin slave dispatchers (serial / TCP) | `[Block 303]` §303.2 |
| `System.out.println("how'd we get here")` shipped | `[Block 298]` §298.6 |
| `System.out.println("ModbusTcpServer.start()")` shipped | `[Block 311]` §311.5 |
| FC 23 handler class with no `case` reaching it | `[Block 303]` §303.3 |
| `MAX_READ_DATA_SIZE` / `MAX_WRITE_DATA_SIZE` declared with **no consumer** | `[Block 295]` §295.4 |
| `setPersistedData` called **inside** a byte loop (rebuild per byte) | `[Block 306]` §306.4 |

`[INFER]` none is individually serious; together they describe a module maintained by extension rather than
refactoring. The operationally relevant one is the last — a genuine performance defect whose fix would be
behaviour-preserving.

## 315.6 — Thread 5: the documentation describes an earlier product

Four **measured** defects in the official 87-topic guide — not impressions:

| Defect | Measurement | Block |
|---|---|---|
| §Modules names `modbuscore.jar` for the `modbusAsync` palette | the class is in `modbusAsync-rt.jar`; jar names are pre-N4 | `[Block 294]` §294.2 |
| §Modules requires "the feature `modbus`" | **no such feature exists** — there are four, one per palette | `[Block 301]` §301.1 |
| §Limits imposed by the Modbus licenses | illustrates with an **MS/TP** feature; `port.limit` occurs once in the licence, on `mstp` | `[Block 301]` §301.4 |
| 64-bit support | **0 hits** for `64.?bit\|double64\|long64` across all 87 topics, while the driver ships 3 sixty-four-bit datatypes | `[Block 296]` §296.7 |

`[INFER]` the pattern is consistent: the guide was carried over from NiagaraAX and never re-verified against
N4.14. What it still supplies well is *intent* — why byte order varies by vendor, the correct order of
operations for polling, the RS-485 electrical limits `[Block 294]` §294.1, `[Block 295]` §295.1. What it
cannot supply is anything added after it was written.

`[INFER]` and one thing it gets right that a naive reading would call an error: "four Modbus modules" is
correct, because it counts **palettes**, and `modbusCore` has none `[Block 294]` §294.2. That was a claim I
formed and withdrew before it entered a block.

## 315.7 — Thread 6: two internal corrections, both from continuing to read

The focus corrected itself twice. Neither came from a review pass — both came from the next gap.

**[Block 307] → [Block 303] §303.5.** B303 recorded two exception paths writing `byteCount` = 1 and 2 with
empty payloads and flagged them as inconsistent, naming two possible explanations and **deferring**. Reading
the serialiser for a later gap showed the second explanation was right: on a frame whose function code
carries bit 7, `byteCount` **is** the Modbus exception code — 1 = Illegal Function, 2 = Illegal Data
Address. Both were correct. `[INFER]` had B303 guessed, a false defect claim would have sat in the corpus.

**[Block 308] → [Block 295] §295.7.** B295 concluded that per-device sockets meant Modbus TCP "scales with
device count". The socket inventory was right; the throughput reading was not — the outbound path is
serialised by a network-level dispatcher. `[INFER]` the failure mode here is different and worth naming: not
a guess, but **an inference from a partial measurement**. B295 measured where `Comm` objects are constructed
and inferred concurrency from it, without measuring the send path.

## 315.8 — Thread 7: the OEM supplies what the protocol cannot

`[Block 304]` §304.1 measured **zero** discovery in the Tridium driver — verified with two differently-keyed
queries plus a positive control against `bacnet-wb`. That is the protocol, not an omission: a Modbus slave
cannot be asked what its registers mean.

`[Block 314]` completes it. Honeywell's OEM modules **do** ship discovery — device-level and point-level —
and the mechanism is to **compile the register map in** (§314.3: register 3 is CO₂ on a TR50). It is a
built-in device profile, not protocol discovery, and it works because one vendor owns both ends.

`[INFER]` the sharpest evidence is the counter-example inside the same product line `[Block 314]` §314.7:
the **TR100 has no Modbus module** — only a 2082-line integration guide. So Honeywell integrates its own two
devices two different ways: the TR50's register map lives in a class, the TR100's lives in a PDF and a human
is the compiler. That is `[Block 304]`'s thesis demonstrated end to end.

## 315.9 — What this focus did NOT resolve

Stated plainly, because a synthesis that only lists wins is a sales document.

- **Thread safety of the server maps** — `[Block 311]` established that `IntHashMap` offers no guarantees
  (0 `synchronized`, 0 `volatile`, verified over the complete original source), that the master writes from
  a dedicated thread, and that no lock exists on either write path. The conclusion remains a **conditional**:
  the engine-side thread identity was never measured. Gap M22 was closed by **re-scope**, not by answering —
  it belongs to a `driver-framework` focus that does not yet exist. **The corpus does not claim this driver
  has a data race.**
- **Live behaviour** — every conclusion here is static. The FC 20/21 response handling, whether the 256-deep
  queue is ever filled, whether the RTU timing misbehaves at the extremes, and the CRC byte-order question
  `[Block 131]` raised all need a real device. These inherit the `P1-dyn` disposition already tracked in
  `RESEARCH-STATE-protocols.md` and were deliberately **not** duplicated into this focus's backlog.
- **Depth on the OEM modules** — `[Block 314]` documented the delta against the driver only. Firmware-OTA
  integrity `[Block 94]` §94.4 and the sensor alarm registers belong to `oem-honeywell-tail`, paused at 9/17.
- **The `-ux` browser layer** — inventoried, not read `[Block 304]` §304.4. A scope call: the managers mirror
  their Workbench twins and nothing observed suggested otherwise.

## 315.10 — Self-verify

`verify-block.sh` tally (COMPUTED — `adj` strips the header legend):

| Marker | raw | adj |
|---|---|---|
| `[CERT]` | 2 | 1 |
| `[CERT-doc]` / `[CERT-hw]` / `[CERT-live]` / `[CERT-web]` / `[CERT-a]` | 0 | 0 |
| `[INFER]` | 14 | 13 |
| **[INFER]/[CERT*] ratio** | | **13/1 = 13.00** |

**The 13.00 needs explaining rather than excusing.** It is a deliberate authoring decision, not a marker
failure: in this block the **remissions carry the evidence**, and a remission is written
`[Block N] §N.x` — not `[CERT]`. Tagging a remission `[CERT]` would be wrong, because this block did not
verify those facts; the remitted block did, each with its own token-check and citation-resolution pass.
So the script sees **70 remissions** (45 of them section-precise, `[Block N] §N.x`) as unmarked prose,
and one lone `[CERT]`. Counts measured, not estimated.

The consequence to be aware of: **`verify-block.sh` cannot audit a synthesis block the way it audits an
evidence block.** Its arithmetic assumes primary citations are present. That is a real limitation of the
gate for this block type, and it is raised as a kit proposal in the §18 retro rather than worked around by
mislabelling markers here.

**Block type: SYNTHESIS.** Per METHODOLOGY §11 the `[INFER]`/`[CERT]` ratio is read differently here: a high
ratio is **expected and healthy**, because the block's value is precisely the cross-block connections that no
single block asserts. It is **not** an exhaustion signal — the underlying evidence was verified in the 21
blocks this consolidates, each with its own token-check.

Verification performed for this block:

| Check | Result |
|---|---|
| Remission count | **70** total, **45** section-precise (`rg -o` over the block) |
| Every remitted section exists | **checked** — all were authored in this focus and re-read while writing |
| No new factual claim introduced without a remission | **checked** — every table row traces to a block |
| Counts re-checked against `RESEARCH-STATE-modbus.md` | 22/22 gaps, 21 evidence blocks (B294–B314), `investigable_open = 0` |
| Corrections listed match the pointers on disk | **checked** — B303 and B295 both carry §14 pointers added when the corrections landed |

`[INFER]` one honest note about this block's own reliability: a synthesis is where a focus is most likely to
overstate itself, because the individual caveats are exactly what gets compressed away. §315.9 exists to
counterweight that, and the two corrections in §315.7 are recorded as **method failures caught late**, not as
successes.

Model tier: **no delegation — inline** (session constraint; see the §18 retro).

## 315.x — Connections

- **[Block 131]** — the wire level this focus sits on; its `P1-fc` read/write halves were closed here (`[Block 295]` §295.2, `[Block 299]` §299.1, `[Block 310]`), its live remainder was not.
- **[Block 137]** — the LOGO! 8 integration plan; its register map is exactly what `Learn Optimum Device Poll Config` would chew on, and §315.3's defaults are what would decide whether that integration behaves.
- **[Block 28]** — cross-protocol discovery; §315.8 is the Modbus-shaped hole in it, now measured and explained.
- **[Block 4]/[Block 7]** — the driver framework beneath; M22 was re-scoped there.
- **[Block 94]/[Block 95]/[Block 250]** — the OEM/hardware view, remitted throughout `[Block 314]` §314.1.
- **`oem-honeywell-tail`** (paused 9/17) — the natural next home for OEM depth.

**Focus status**: **STOPPED 22/22**, read-only investigable backlog exhausted (METHODOLOGY §8).
