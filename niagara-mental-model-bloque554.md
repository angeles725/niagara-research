# Block 554 — The BOX fragment/mux protocol in detail: the `F;2.3;…` wire format, the fragmentation/reassembly engine, and the `u`/`r` push-vs-response marker (deepens B512)

**Session**: 2026-08-28
**Focus**: `apis` (gap API4-deep — the BOX fragment/mux wire; operator-requested)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the two `com.tridium.box.mux` classes; the wire format and
constants token-verified inline.
**Primary sources** `[CERT]`:
`organized/box/box-rt/vineflower/com/tridium/box/mux/BoxEnvelope.java` (300 lines),
`BoxEnvelopeDemux.java` (61 lines).

**Scope**: [Block 512] (API4) established BOX as N4's live-subscription substrate and named the fragment
protocol in passing. This block decompiles the MUX layer in full — the byte-level frame, the split/reassemble
engine, and the marker that distinguishes a SERVER PUSH from a response. Relevant to the multi-user live
dashboard ([Block 553]): every unsolicited change envelope a dashboard receives is framed here. B512 is
REMITTANCE for the channel/session model.

---

## 554.1 What the MUX layer does [CERT]

BOX carries JSON message "envelopes" that can exceed a single transport frame. The MUX layer
(`com.tridium.box.mux`) splits one logical envelope into ordered **fragments** each ≤ `maxMessageSize`, tags
each with the envelope id + fragment index, and reassembles them on the other end. It works identically over
both BOX transports (HTTP-poll v1, WebSocket v2), so a large subscription-change payload streams as multiple
fragments without a transport-specific chunking scheme.

## 554.2 The wire format [CERT]

`BoxEnvelope.toBoxFragmentBytes` builds each fragment byte-for-byte `[CERT] BoxEnvelope.java` (bytes shown
decimal → char):
```
F ; 2.3 ; <serverSessionId> ; <envelopeId> ; <fragmentCount> ; <fragmentIndex> ; <u|r> ; <payload>
```
- `70`=`'F'` (fragment marker), `59`=`';'` separators `[CERT]`.
- version literal **`"2.3"`** `[CERT]` (the BOX MUX protocol version).
- `serverSessionId` — the per-user BOX session ([Block 512]).
- `envelopeId` — groups fragments of one logical message.
- `fragmentCount` — total fragments in this envelope; `fragmentIndex` — this fragment's 0-based position.
- **`117`=`'u'` (unsolicited) or `114`=`'r'` (response)** `[CERT]` — the push-vs-reply marker (§554.5).
- `payload` — the raw fragment bytes.

The `fragmentOverhead` (the header length) is measured once by serializing a dummy `F;2.3;…;99;99;…` frame
with an empty payload `[CERT] :79` — so the split math always accounts for the exact header cost.

## 554.3 Constants + JVM tuning [CERT]

All `box.mux.*` system properties, read via `AccessController.doPrivileged` `[CERT] BoxEnvelope.java:37-44`:

| Constant | Property | Default |
|----------|----------|---------|
| `MUX_ENABLED` | `box.mux.muxEnabled` | **true** (on unless set to `"false"`) |
| `MIN_DELAY` | `box.mux.minDelay` | 0 ms |
| `MAX_DELAY` | `box.mux.maxDelay` | **200 ms** |
| `MAX_ENVELOPE_SIZE` | `box.mux.maxEnvelopeSize` | **1 048 576 (1 MB)** |

`MIN_DELAY`/`MAX_DELAY` bound the mux batching window (coalescing small messages before a flush); `1 MB` caps a
single logical envelope. These are the knobs to tune a dashboard's push behavior at the transport layer (the
per-session push cadence is separately capped by the `BrokerPoller` 2 s burst, [Block 512]).

## 554.4 Fragmentation — `append()` [CERT]

`BoxEnvelope.append(byte[] data)` `[CERT] :131-150`:
1. `maxFragmentLength = maxMessageSize − fragmentOverhead`.
2. If a partial last fragment exists, **top it up first** (`freeSpace = maxFragmentLength − lastFragment.length`)
   — fragments are packed full, not one-per-append.
3. Split the remainder into `maxFragmentLength` chunks, each a new fragment (`fragmentCount++`).
4. `ensureCapacity()` guards against exceeding `maxEnvelopeSize`.

`maxPayloadSize = maxEnvelopeSize − ceil(maxEnvelopeSize/maxMessageSize)·fragmentOverhead` `[CERT] :174-177` —
the usable payload after subtracting per-fragment header overhead; `willFit(n)` checks a new payload against it.

## 554.5 Reassembly — `BoxEnvelopeDemux` [CERT]

The receiver keeps `pendingEnvelopes: Map<envelopeId, BoxEnvelope>` `[CERT] BoxEnvelopeDemux.java:16`.
`receiveFragment(envelopeId, fragmentCount, fragmentIndex, payload, op)` `[CERT] :41-56`:
1. `openEnvelope` — `computeIfAbsent` creates a "collecting" envelope on the first fragment of that id.
2. `setFragmentCount` + `receiveFragment(payload, index)` places the fragment in its indexed slot
   (index-bounds-checked, `BoxEnvelope.java:152-158`).
3. `isComplete()` = no slot is null `[CERT] :160-172` → fire the `onComplete` callback, then **remove the
   envelope from pending** (`finally`). So reassembly is id-keyed and order-independent (fragments can arrive
   in any order; the index places them).

## 554.6 `u` vs `r` — the push marker (ties the dashboard) [CERT]

An envelope is built one of three ways `[CERT] :48-68`: `unsolicited(...)` → `unsolicited=true` → frame marker
`'u'` (a SERVER PUSH, `op=null`); `response(clientEnvelope, payload)` → `false` → `'r'` (a reply to a client
op, reusing the client's envelopeId + op); `collecting(...)` → an inbound envelope being reassembled. For the
multi-user dashboard ([Block 553]): every live change the `ProxyBroker` pushes to a subscribed session is an
**`unsolicited` (`'u'`) envelope** — the client demuxes it and applies the change without having asked. The
`'r'` path is request/response (e.g. a `sub` acknowledgement). This is the byte-level distinction between "the
data changed, here's the delta" (dashboard push) and "here's your reply."

## 554.7 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Wire format F;2.3;serverSessionId;envelopeId;fragmentCount;fragmentIndex;u\|r;payload | [CERT] | BoxEnvelope.java toBoxFragmentBytes | token-checked ✓ |
| 2 | 'F'=70, ';'=59, version "2.3", 'u'=117/'r'=114 | [CERT] | toBoxFragmentBytes body | token-checked ✓ |
| 3 | Constants: MUX_ENABLED default on, MIN_DELAY 0, MAX_DELAY 200, MAX_ENVELOPE_SIZE 1MB (box.mux.*) | [CERT] | BoxEnvelope.java:37-44 | token-checked ✓ |
| 4 | append() tops up last fragment then splits by maxFragmentLength; ensureCapacity guards maxEnvelopeSize | [CERT] | BoxEnvelope.java:131-150,174-177 | token-checked ✓ |
| 5 | Demux: pending map by envelopeId, indexed slots, isComplete=no-null→onComplete+remove | [CERT] | BoxEnvelopeDemux.java:41-56 + isComplete | token-checked ✓ |
| 6 | unsolicited→'u' (server push, op=null) vs response→'r' (reply) | [CERT] | BoxEnvelope.java:48-68 | token-checked ✓ |

**Marker tally**: [CERT] ×6 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). All rows token-verified inline
(this is a wire-format spec).

## Connections

- **[Block 512]** (API4) — the BOX channel/session/ProxyBroker model this MUX layer serves (REMITTANCE).
- **[Block 553]** (API10) — the multi-user dashboard: §554.6's `'u'` envelope is the push a dashboard receives.
- **[Block 59]** — the Reflow WebSocket layer, a peer transport that does NOT use this BOX mux (its own framing).

## Open gaps (this block)

- The `respond()` per-op JSON dispatch (BoxEnvelope.java:193+) into `BBoxService` is the channel-op handler,
  already mapped by [Block 512]; not re-derived. Focus stays stopped (this is an API4 sub-item, not a new gap).
