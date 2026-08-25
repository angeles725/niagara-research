# Block 499 — `framework-drivers` FD4: `obixDriver` — the oBIX REST/HTTP driver (network→client→proxy over the shared `obix-rt` `ObixSession`), the Watch subscription model, and its headline security posture: HTTP Basic auth over a default `http://` lobby (credentials base64-in-the-clear unless the operator opts into HTTPS)

> **Focus:** `framework-drivers`, gap **FD4** — the largest fully-uncovered candidate (~141 cls). oBIX = Open
> Building Information eXchange (REST/HTTP + XML BAS protocol). READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/obixDriver/obixDriver-rt/decompiled/…` (this artifact ships `decompiled/`)
> + the shared transport lib `organized/obix/obix-rt/vineflower/obix/net/…`. FUENTE 1 — [B497]/[B498] (the
> driver-security pattern), [B392]/[B398]/[B482] (trust + at-rest). FUENTE 2 — not consulted (decompilation gap).
> Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line RE-VERIFIED inline (offsets discarded).
> **Scope:** the oBIX wire/session + `Obj` object model live in a SHARED dependency module `obix-rt` (the oBIX
> analogue of opcUaCore [B496] for OPC UA); `obixDriver` is the Baja driver over it. `obixDriver` also carries a
> SERVER/export side (`BObixServer`) — noted here, not deep-distilled (that is an export gap, not FD4's driver scope).

## §499.1 — Component tree + dual role `[CERT]`

`javax.baja.obix.driver.*` (client driver):

| Class | Base | Role |
|---|---|---|
| `BObixNetwork` | `BDeviceNetwork` (impl `BIService`) | network singleton; owns threadPool, tuning/history policies, the `server` (export side) and `exports` |
| `BObixClient` | `BDevice` | one per remote oBIX server; holds `lobby` URI, `authUser`/`authPass`, the live `ObixSession`, and 4 device exts (points/alarms/histories/schedules) |
| `BR2ObixClient` | `BObixClient` | R2 variant for servers that don't normalize hrefs |
| `BObixPointDeviceExt` | `BPointDeviceExt` (impl `WatchListener`) | owns the Watch subscription + read/write queues; `watchInterval` default 2 s |
| `BObixProxyExt` | `BProxyExt` (impl `BIObixPollable`) | per-point proxy: `href` → Niagara `BStatusValue` (`<real>`→BStatusNumeric, `<bool>`→BStatusBoolean, `<enum>`→BStatusEnum) |

**Dual role `[CERT]`:** the module also ships `com.tridium.obix.server.BObixServer extends BWebServlet`
(mounted at `/obix`, GET/PUT/POST + `/obix/soap`) — N4 exposing ITSELF as an oBIX server. Gated by the
`export` license sub-key (§499.5). Documented as present; the driver (client) is FD4's scope.

## §499.2 — Protocol mechanics: REST over the shared `ObixSession` `[CERT]`

The HTTP client is `obix.net.ObixSession` in the shared `obix-rt` module. REST verbs:
- `read(uri)` → **GET**; `write(obj)` → **PUT** `text/xml`; `invoke(href,in)` → **POST** `text/xml`
  (`ObixSession.java` send-dispatch).
- **Lobby discovery** (`ObixSession.open()`): GET the configured lobby URL, then follow the `batch`, `about`,
  and `watchService` refs from the lobby document — standard oBIX entry-point bootstrap.
- **XML codec:** the oBIX `Obj` graph (Bool/Real/Int/Enum/Str/Reltime/Abstime/Uri/List/Ref/Op) maps 1:1 to
  oBIX XML elements via `obix.io.ObixXmlDecoder`/`Encoder` (client, obix-rt) and `javax.baja.obix.io.ObixDecoder`/
  `Encoder` (server, Niagara `XParser`/`XWriter`). `BObixClient.makeStatusValue()`/`makeObj()` bridge Obj↔Baja.

`obix-rt` is a **shared lib** (like opcUaCore): the driver rides it and does not re-implement the wire.

## §499.3 — Watch subscription model `[CERT]`

`BObixPointDeviceExt` (a `WatchListener`) drives an oBIX Watch:
- `performSubscribe()` → `makeWatch(name, intervalMillis, this)`, then `watch.add(new Uri(href))` per point;
  changes arrive via `WatchListener.changed(obj)` → `ext.readOk(obj)`.
- **Poll interval** = `watchInterval`, default **2 s**. **Lease** = `max(interval×2, serverLease + watchSafetyFactor)`,
  `watchSafetyFactor` default **10 s** (`BObixClient.java:154` facets). `pollRefresh` (lease renewal) is scheduled
  at `staleTime × 0.75`.
- **Recovery:** `SessionWatch.closed()` → detach + re-attach with `keepSubs=true`. **Fallback:** if watch creation
  fails, `subscribeFailTryPolling()` drops to plain `BObixPollScheduler` polling.

## §499.4 — Authentication: HTTP Basic over a default `http://` lobby — the headline `[CERT]`

```
ObixSession.java:56   this.authHeader = "Basic " + Base64.encode(username + ':' + password);
ObixSession.java:279  conn.setRequestHeader("Authorization", this.authHeader);
```

Auth is **HTTP Basic, always** — no Digest/token/OAuth path exists. The header is re-sent on **every** request.
Credential storage is safe at rest: `authUser` (String) + **`authPass` = `BPassword`** (`BObixClient.java:156`,
`BPassword.DEFAULT`), fetched via `AccessController.doPrivileged(getAuthPass()::getValue)` — encrypted in
`config.bog`, not plaintext (`[CERT]`, corrects the naive "plaintext creds" hypothesis).

**The load-bearing risk is in transit, not at rest** `[CERT]`+`[INFER]`: the `lobby` property default is
`"http://url/to/lobby/here"` (`BObixClient.java:154`, scheme = **http**). TLS is supported — `ObixSession`
builds an `HttpsConnection` when the URL scheme is `https` — but **nothing enforces or warns** it. With the
default/any `http://` lobby, the base64 Basic credential (trivially reversible) and every Watch payload cross
the wire **unencrypted**. HTTPS is opt-in by operator URL choice, with no runtime rejection of plaintext targets.
(Hostname/cert verification for the `https` path lives in `baja-rt`'s `HttpsConnection`, out of this module —
an unresolved sub-point, not a claim.)

## §499.5 — License gate `[CERT]`

`getFeature("tridium", "obixDriver")` (`BObixNetwork.java:149` + static helper `:265`), enforced at network
`started()` via `checkObixLicense()`. Feature = **`tridium:obixDriver`**, with sub-keys:
- `foreignDevice.limit` — cap on non-Tridium `BObixClient`s;
- `foreignPoint.limit` — cap on proxy points against non-Tridium servers;
- `export` (boolean) — gates the `BObixServer` export side.

Tridium-vs-foreign is decided by `BObixClient.isTridiumServer()` reading the remote lobby/about
(`vendorName contains "Tridium" && productName contains "Niagara"`) — the limits apply only to FOREIGN servers,
i.e. Niagara↔Niagara oBIX is unmetered, third-party oBIX is capped.

## §499.6 — Security notes (distilled) `[CERT]`/`[INFER]`

- **S1/S2 (primary):** HTTP Basic over a default-`http` lobby, no TLS enforcement → credentials + data in the
  clear unless the operator sets `https` (§499.4). This is the module's dominant exposure.
- **S3 (unconfirmed):** server-side XML parse (`ObixDecoder`/`BObixServer`) uses `XParser.make(...)` with no
  visible external-entity-disabling call → **possible XXE**, but the DOCTYPE/entity policy is inside
  `javax.baja.xml.XParser` (`baja-rt`), NOT verifiable here — flagged as a gap, not a confirmed vuln.
- **S6:** client writes (`performWrite`/`batchWrite`) do **not** re-check license before PUT/POST; the
  `foreignPoint.limit` gate is at subscribe-time only → a subscribed proxy keeps write capability.
- **S7:** `debugRequests`/`debugResponses`/`debugWatch` (all default false) dump full oBIX XML incl. written
  values to `System.out` if enabled (credentials are NOT in that dump — they're in the Authorization header).
- **S4/S5 (mitigations):** creds are `BPassword` at rest (not plaintext); server side has
  `allowSessionReuse` (default true) that can be tightened.

## §499.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BObixNetwork=BDeviceNetwork, BObixClient=BDevice, proxy=BProxyExt; dual role (BObixServer export) | `[CERT]` | `BObixNetwork.java:70-71`; `BObixClient.java:153`; `com.tridium.obix.server.BObixServer` | PASS |
| 2 | REST GET/PUT/POST over shared obix-rt ObixSession; lobby-ref discovery; Obj↔XML 1:1 | `[CERT]` | `ObixSession` send/open; `obix.io`/`javax.baja.obix.io` codecs | PASS |
| 3 | Watch model: interval default 2s, lease=max(2×int, serverLease+10s), pollRefresh@0.75, polling fallback | `[CERT]` | `BObixPointDeviceExt` (2s); `BObixClient.java:154` (10s) | PASS |
| 4 | auth = HTTP Basic always, base64(user:pass), re-sent every request | `[CERT]` | `ObixSession.java:56,279` | PASS |
| 5 | default lobby = http:// (not https); TLS opt-in only, unenforced → creds in clear | `[CERT]`+`[INFER]` | `BObixClient.java:154` | PASS |
| 6 | creds at rest = BPassword (not plaintext), doPrivileged getValue | `[CERT]` | `BObixClient.java:156,195-200` | PASS |
| 7 | license `tridium:obixDriver` + foreignDevice/foreignPoint.limit + export sub-keys | `[CERT]` | `BObixNetwork.java:149,265` | PASS |

**Tally:** 7 claims — 6 `[CERT]` load-bearing + `[INFER]` (transit exposure) on cited code. XXE is an explicit
UNCONFIRMED gap (S3), not a claim. Block TYPE = **EVIDENCE**; ratio ~0.3, FD4 CLOSED (driver scope). All
load-bearing tokens re-verified inline.

## §499.8 — Connections & focus status

- Same driver-security shape as [B497]/[B498]: BPassword-at-rest creds, per-role license `getFeature("tridium",…)`,
  operator-configurable weakening. Divergence: oBIX's weakness is **transport by default** (http lobby + Basic),
  where OPC UA's client default was strong.
- `obix-rt` is a shared oBIX lib (transport + Obj model) — the oBIX analogue of [B496] opcUaCore. A dedicated
  `obix-rt`/`BObixServer` export block is a POSSIBLE future gap (recorded, not seeded — out of FD4's driver scope).
- Security feed to [B490]/[B398]: S1/S2 (Basic-over-http default) joins the SEC catalog of plaintext-by-default
  surfaces alongside [B498] (username token under SecurityPolicy.NONE) and [B398] (HTTP :80 open).
- **Focus status:** `framework-drivers` 4/10 (FD1–FD4 closed). NEXT = FD5 `mbus`.
