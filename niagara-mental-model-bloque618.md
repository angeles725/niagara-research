# Block 618 — graphql-admin (GQL-G7): BOX has an open channel-registration seam — a GraphQL subscription channel plugs in without a separate WebSocket

> **What**: Whether a custom module can register a NEW BOX channel type (for GraphQL subscriptions) that
> rides the existing BOX WebSocket transport, or must roll its own WebSocket endpoint. Answer: BOX
> auto-discovers EVERY concrete `BBoxChannel` subclass from the type registry and adds it by name — a
> genuine, open extension seam. A `BGraphqlSubscriptionChannel extends BBoxChannel` with a unique
> `getChannelName()` is auto-registered on service start; no separate WebSocket, no core modification.
> **Scope**: `com.tridium.box.BBoxChannel` (the abstract channel) + `BBoxService` (registry discovery +
> dispatch). The BOX wire protocol, `BBoxWebSocketAcceptor`, and mux/fragment are REMITTANCE to
> [B512]/[B554]; the "roll your own WebSocket" alternative to [B59].
> **Block type**: EVIDENCE (code seam) + DESIGN verdict.
> **Subject version**: Niagara N4.14.0.162.
> **Sources**:
> - `organized/box/box-rt/vineflower/com/tridium/box/BBoxChannel.java`
> - `organized/box/box-rt/vineflower/com/tridium/box/BBoxService.java`
> **Method**: vineflower, driver-read. Markers: `[CERT]` `file:line`; `[INFER]` = design verdict.

---

## 618.1 — `BBoxChannel` is an abstract, registry-discovered extension point `[CERT]`

`public abstract class BBoxChannel extends BComponent` with two abstract methods `[CERT]`
(`BBoxChannel.java:11,19,21`):
- `boolean service(String key, Object arg, BoxWriter out, BoxOp op)` — handle a request on this channel and
  write the response/push (`:19`).
- `String getChannelName()` — the channel's wire name (`:21`).

The corpus ships ~13 concrete channels as the precedent set: `BAlarmChannel`, `BHistoryChannel`,
`BOrdChannel`, `BRegistryChannel`, `BSysChannel`, `BTimeZoneChannel`, `BTransferChannel`, `BUnitChannel`,
`BServerSessionChannel`, `BFoxBoxChannel`, … — each an independent `BBoxChannel` subclass.

## 618.2 — `BBoxService` auto-registers every registry-known channel `[CERT]`

On startup `BBoxService` walks the TYPE REGISTRY for concrete `BBoxChannel` subclasses and adds each by name
`[CERT]` (`BBoxService.java:293-296`):

```java
for (TypeInfo t : Sys.getRegistry().getConcreteTypes(BBoxChannel.TYPE.getTypeInfo())) {
  BBoxChannel channel = (BBoxChannel) t.getInstance();
  if (this.get(channel.getChannelName()) == null)
    this.add(channel.getChannelName(), channel);   // dynamic add, keyed by channel name
}
```

Dispatch then routes an incoming BOX message to the channel by name: `BBoxChannel channel =
(BBoxChannel)this.get(channelName)` `[CERT]` (`:516`; `getChannelName(boxMessage)` at `:494,501`). The
built-in channels are also declared as frozen `@NiagaraProperty` slots (`:56-113`), but the registry loop
at `:293` ADDS any registry-discovered channel on top — so a module's own channel needs no property slot,
only a concrete `BBoxChannel` type with a unique name.

**This is an open extension seam** `[CERT]`: registration is by TYPE PRESENCE IN THE REGISTRY (a module
shipping the type), not a hardcoded switch. Any module that declares `com.tridium.box` (or transitively via
`baja`) as a dependency and ships a concrete `BBoxChannel` participates.

## 618.3 — Design map (GQL-G7) `[INFER]`

For GraphQL SUBSCRIPTIONS, the native-integrated path is:
- Ship `BGraphqlSubscriptionChannel extends BBoxChannel` with `getChannelName() = "graphql"` (unique).
  On station start `BBoxService` auto-adds it. Its `service(key, arg, out, op)` handles subscribe/unsubscribe
  and pushes updates over the SAME BOX WebSocket transport (`BBoxWebSocketAcceptor`, [B512]) the built-in
  channels use — the client's existing BOX WS session multiplexes the new channel by name (mux/fragment
  per [B554]).
- The push data is produced by the resolver call-site ([B614]) run as the subscriber's Context, gated by
  `canRead()` ([B612]) per pushed node.

Contrast — the ALTERNATIVE is a SEPARATE WebSocket endpoint, as Reflow did with `BReflowWebSocketAcceptor`
([B59]). That is a deliberate choice (full protocol control, independent of BOX), NOT a necessity: the BOX
channel seam gives real-time push with zero new transport code and automatic session/auth reuse. Recommended:
use the BOX channel seam unless the GraphQL subscription protocol must be wire-independent of BOX.

**Caveat** `[INFER]`: a BOX channel rides the BOX session, whose user/auth is the established BOX session
principal — the same RBAC applies, but the subscription's per-node `canRead()` must be re-checked on every
push (a category change mid-subscription must stop leaking), not only at subscribe time.

## 618.4 — Connections

- **[B512]/[B554]** — the BOX wire, `BBoxWebSocketAcceptor`, ssession sub/unsub, mux/fragment: the transport
  a custom channel rides. B618 is the SEAM into it.
- **[B59]** — Reflow's separate `BReflowWebSocketAcceptor`: the alternative (own WS) B618 contrasts.
- **[B614] (GQL-G4)** — the resolver call-site producing the pushed data; **[B612] (GQL-G3)** — the per-push
  `canRead()` gate.
- Forward: **GQL-G9** synthesis (the reference architecture assembling G1–G8).

---

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | `BBoxChannel` is abstract `extends BComponent`; abstract `service(...)` + `getChannelName()` | `[CERT]` | BBoxChannel.java:11,19,21 | ✓ read |
| 2 | `BBoxService` walks the registry for concrete `BBoxChannel` types, adds each by name | `[CERT]` | BBoxService.java:293-296 | ✓ read |
| 3 | Dispatch routes a BOX message to a channel by name (`this.get(channelName)`) | `[CERT]` | BBoxService.java:494,501,516 | ✓ read |
| 4 | ~13 concrete channels ship as the precedent set (Alarm/History/Ord/Sys/…) | `[CERT]` | box-rt/*Channel.java (ls) | ✓ listed |
| 5 | A custom `BBoxChannel` (unique name) auto-registers → GraphQL subscription rides BOX WS | `[INFER]` | design from #1-#3 + [B512] | ✓ reasoned |
| 6 | Alternative = separate WebSocket (B59); the seam makes it unnecessary | `[INFER]` | contrast with [B59] | ✓ reasoned |
| 7 | Per-push `canRead()` must be re-checked, not only at subscribe | `[INFER]` | security deduction + [B612] | ✓ reasoned |

**Tally**: `[CERT]` = 4 · `[INFER]` = 3 · others = 0. **Ratio** ≈ 0.75 — block type = DESIGN/APPLIED (the seam
is `[CERT]`; the subscription design is `[INFER]`), so the ratio is expected/healthy (§11). G7 closed.
**Tokens checked**: `:293-296` registry-discovery loop and `:516` dispatch read directly; BBoxChannel abstract
contract read; concrete-channel set enumerated by directory listing.
