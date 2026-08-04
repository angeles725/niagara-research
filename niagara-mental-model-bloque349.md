# Block 349 — SYNTHESIS of the `jsonToolkit` focus (B335–B348): a bidirectional JSON marshaller that generates on the engine thread, ships no transport, trusts its inbound sender, and gates on a three-layer license

> Focus **jsonToolkit** — closing synthesis. 14 evidence blocks, 14 gaps, one prioritized backlog exhausted.
> This block consolidates the cross-cutting threads and **remits** to the block that established each finding; it
> re-derives nothing. READ-ONLY. Corpus language: ENGLISH.
>
> Scope of the focus: the add-on module `com.tridiumx.jsonToolkit` (v4.14, namespace `tridiumx` = extended, NOT
> core) as a bidirectional JSON marshaller — 163 own classes (Gson + jayway-jsonpath bundled, DISMISSED).
>
> Markers: this is a SYNTHESIS block — every `[CERT]` is a **remission** to a block that verified it (`[Block N]
> §N.x`, not a fresh `file:line`); `[INFER]` marks connections drawn ACROSS blocks. Layer (integration/JSON) +
> Layer 22 (security). Consolidates [Block 335]–[Block 348].

---

## 349.1 — What the focus covered

| Area | Block |
|---|---|
| Service + 3-layer license + `runAsUser` | [335] |
| Outbound schema model (tree, binding, slot selection) | [336] |
| Generation trigger (COV → engine thread, debounce) | [337] |
| Outbound queries (BITable, 30 s timeout) | [338] |
| Transport (there is none) | [339] |
| Inbound core (route, JSONPath selectors, routers) | [340] |
| Inbound handlers (setpoint/ack/export-marker — SECURITY) | [341] |
| Outbound detail (15 properties, 8 styles, naming) | [342] |
| Relative schema (cross-station) | [343] |
| Inline Program writer (escape hatch) | [344] |
| JSON alarm recipient | [345] |
| Util / engine-cycle queues | [346] |
| ux + wb UI | [347] |
| Doc-synthesis (what the manual omits) | [348] |

Bootstrapped 2026-08-04 (right after the `email` focus) on 163 own classes; first corpus use of `docJsonToolkit`
(114 files, 33 cited).

## 349.2 — Thread 1: two symmetric directions, one identity

Outbound GENERATES JSON from a schema tree [Block 336]; inbound RECEIVES JSON and WRITES the station [Block 340].
Both hang under `BJsonSchemaService`, and both act under one identity: `runAsUser`, a superuser-only,
tamper-guarded, unlinkable property [Block 335] §335.4. That identity is the privilege boundary of the whole
module — outbound reads and inbound writes all execute as it `[INFER]`.

## 349.3 — Thread 2: the engine thread is where it all runs, bounded three ways

The most important operational fact `[INFER]`: outbound generation is **synchronous on the Niagara engine
thread**. A COV event calls `requestGenerateJson` directly (no enqueue) [Block 337] §337.3, re-running the WHOLE
schema tree + queries inline. Three bounds keep that safe: the `minWrite` single-shot **debounce** (latency)
[Block 337] §337.4, the `queriesMaxExecutionTime` **30 s timeout** that cancels + faults [Block 338] §338.2, and
the engine-cycle **queue** (one item per cycle, cap 1000, overflow REJECTS with `QueueFullException`)
[Block 346] §346.2. So the engine thread cannot be starved silently — but a large schema or slow query still
blocks it up to 30 s `[INFER]`. This is the thread the whole module lives or dies on.

## 349.4 — Thread 3: marshaller, not transport

jsonToolkit ships **no autonomous transport** — proven by two grep passes [Block 339] §339.1. The JSON lives in
the schema's `output` String slot and leaves only by PULL: a consumer reads the slot (obix / BQL / fox /
bajascript), or `BExporter` saves it to a file, or the alarm recipient auto-`BLink`s it to a publish point
[Block 339] §339.2-345 §345.3. To deliver externally you pair it with a transport the station already has. This
is the SAME "produces, doesn't transport" shape as the email module [Block 331] §331.1 `[INFER]` — the module
does the JSON work, the last mile is someone else's.

## 349.5 — Thread 4: the inbound trust model (the security story, measured)

Inbound is built for a TRUSTED sender behind an authenticated transport `[INFER]`. The gates are real but partial
[Block 341]:
- Point WRITES **are authorized** — `userHasWritePermission` checks `runAsUser` holds operator/admin-write; NOT a
  bypass [Block 341] §341.2. But the JSON sender picks the priority-array slot (no whitelist), and the write
  commits context-free.
- Alarm ACK **is gated** on `runAsUser` adminWrite (stronger than email's From-only check [Block 327]), but the
  recorded acker name is taken verbatim from the JSON — an audit-integrity spoof [Block 341] §341.3.
- Export-marker registration has **NO ACL** — any inbound message can register any ORD → registry poisoning
  [Block 341] §341.4. The sharpest finding.
- `BJsonArrayForEachSelector` has no size guard, and `learnMode` auto-creates slots [Block 340] §340.3-340.5.
The meta-lesson of the focus: a delegated sweep OVERSTATED the setpoint case as a "bypass"; the driver's
framework-semantic re-read DOWNGRADED it to "authorized, sender-picks-priority" [Block 341] §341.8 — the most
valuable act was subtracting a false finding, not adding one `[INFER]`.

## 349.6 — Thread 5: declarative model with an escape hatch

A schema is a member TREE walked top-down into one `JSONWriter` [Block 336] §336.2: object/array/property leaves,
15 property types, 8 query-result styles, a 3-stage key-naming pipeline [Block 342]. Queries feed array content
as any BITable-returning ORD (BQL/NEQL/history/transform) [Block 338]. When the declarative tree cannot express
something, `BInlineJsonWriter` drops a `BProgram` into the tree with the shared `JSONWriter` [Block 344] — the
facility [Block 76] already reused. The relative schema swaps the station base for a stream of query rows to
aggregate subordinate stations via `sys:` ORDs over Fox [Block 343].

## 349.7 — Thread 6: the operational license gate

Running requires the license feature `tridium/jsonToolkit` (prototype "DR-JSON"), AND the per-direction `import`/
`export` attributes, AND a valid SMA (an EXPIRED SMA disables import/export at runtime, not just upgrades)
[Block 335] §335.2-335.3. Tie-in: the client licenses in memory (QNX-TITAN / Win-2E48) carry expired SMA — so
jsonToolkit import/export would be dead on them absent `sma.exempt` `[INFER]`. Same class of blocker as the
`email` feature [Block 324]: not a programming problem, a licensing one.

## 349.8 — Answering the request, and doc reliability

The focus began from "can we document the jsonToolkit module?" — yes, fully: 14 blocks, the whole outbound +
inbound + UI + license surface. The official `docJsonToolkit` is ACCURATE on the configuration happy path (a rare
whole-focus doc↔code agreement) but SILENT on the failure modes decompilation surfaced — the engine-thread
synchronous regen, the queue reject, `null`→`""`, and the four inbound-trust surfaces [Block 348] §348.3.

## 349.9 — Connections & what remains

- `email` focus [Block 324]–[Block 334] — the sibling service subsystem; both are license-gated, both are
  marshallers-not-transports, both have a spoofable inbound-alarm path.
- [Block 76] — the `InlineJsonWriter` reuse (chihuahua-rt); [Block 32] §32.3 — the bundled-Jayway CVE thread.
- `niagara-network-supervisor` (planned) — the Fox `sys:` join the relative schema [Block 343] consumes.
- **Open child gaps (named, not closed)**: the inbound-trust surfaces [Block 341] §341.3-341.4 are candidates for
  a DYNAMIC (§12) validation against a live station — mark `jsonToolkit-G1` (export-registration ACL bypass) and
  `jsonToolkit-G2` (alarm-ack attribution spoof), both requires-execution / live-station, out of scope for the
  static focus.

## 349.10 — Self-verify

Block TYPE: **synthesis** (remissions, no fresh `file:line` — a high `[INFER]` ratio is EXPECTED and correct per
§11). Every `[CERT]`-remission points to a block carrying the verified citation; the `[INFER]`s are the
cross-block threads. Coverage: 14/14 gaps closed (B335–B348); Gson + jayway-jsonpath DISMISSED per census; two
named child gaps (`jsonToolkit-G1`/`G2`, requires-execution) deferred.

`verify-block.sh` exit 0 — tally: CERT (remissions) 1 · CERT-doc 0 · INFER 9 · ratio 9.00. The high ratio and
the "zero file:line" WARN are BY DESIGN for a synthesis block: it cites `[Block N] §N.x` remissions, not fresh
`file:line`; each remitted block carries the token-checked citation.
