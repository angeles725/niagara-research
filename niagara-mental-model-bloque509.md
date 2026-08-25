# Block 509 — `apis` API3: the oBIX SERVER — N4 as an oBIX host (`BObixServer` = `BWebServlet`+`Soaplet` at `/obix`), the lobby-agent map that publishes the WHOLE station tree (`/obix/config`, `/obix/ord`) with no export allowlist, per-user server Watches, and RBAC-gated writes but no per-object read ACL

> **Focus:** `apis`, gap **API3** — N4 AS an oBIX server (the export/host side). CLOSES the future-gap flagged in
> [B499 §499.8] (obixDriver's `BObixServer`, deferred there). The CLIENT driver + shared `obix-rt` transport are
> [B499]; this is the SERVER surface. READ-ONLY, decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/obixDriver/obixDriver-rt/vineflower/{javax/baja/obix/driver, com/tridium/obix/…}`.
> FUENTE 1 — [B499] (client + Obj model), [B508] (web-tier auth/mount this rides), [B458] (`[CERT-live]` oBIX
> extraction — corroborates readability). Evidence delegated to a `sonnet` sweep; ALL load-bearing file:line
> RE-VERIFIED inline.

## §509.1 — The server component + mount `[CERT]`

`BObixServer extends BWebServlet implements Soaplet` (`BObixServer.java:64`), a child property of `BObixNetwork`
(the dual role [B499] noted). It self-registers with the N4 web container via `servletName = "obix"` (`:65`) →
mounts at **`/obix`** (`:71`), with **`/obix/soap`**, `/obix/wsdl`, `/obix/xsd` for the SOAP variant. **License
gate:** `serviceStarted()` sets `licensed = getLicenseFeature().getb("export", false)` (`:372`); an unlicensed
server answers **HTTP 403 "Unlicensed oBIX Server"** (`:227`) — the `export` sub-key of `tridium:obixDriver`
([B499 §499.5]) gates the server half.

## §509.2 — Lobby + component→oBIX mapping `[CERT]`

`GET /obix` returns `<obj is="obix:Lobby">` with one `<ref>` per lobby agent (`BObixLobby.encode`). Lobby agents
are discovered by `@AgentOn(types={"obixDriver:ObixLobby"})`. URI→ORD: `BObixServer.resolve()` strips `/obix` and
dispatches to the lobby agent by first path segment. Baja→oBIX element map (`ObixUtils.encode`): `BIBoolean`→
`bool`, `BIEnum`→`enum`, `BFloat/BDouble`→`real`, `BInteger`→`int`, `BString`→`str`, `BAbsTime`→`abstime`,
`BOrd`→`uri`, `BComplex`→`obj`. A `BControlPoint` is encoded by `BControlPointAgent` — reads the `out`
`BStatusValue`, emits `is="obix:Point"` + val/status/unit/range.

## §509.3 — REST + SOAP verbs `[CERT]`

`BObixServer.service(WebOp)`: **GET** → `encoder.encode(target)` (the Lobby for `/obix`); **PUT** →
`ObixUtils.serviceWrite` (decode body → `parent.set(prop, val, user)`); **POST** → `ObixUtils.serviceInvoke`
(`BIObixInvocable` like `BObixWatch`/`BObixOp` direct, else resolve the Baja `Action` and `invoke`, waiting up to
5000 ticks for the engine cycle). **SOAP** at `/obix/soap` tunnels through `XElemTunnel`, dispatching by SOAP body
element name; the WSDL declares `read`/`write`/`invoke`.

## §509.4 — Server-side Watch service `[CERT]`

`BObixWatchService` (lobby `watchService`, `/obix/watchService`) exposes a `make` op → creates a `BObixWatch`
live child, binds it to the creating `BUser`, default lease 30 s (per-watch 15 s, renewed on access). `BObixWatch`
ops: `add`/`remove`/`pollChanges`/`pollRefresh`/`delete`; change detection via a Baja `Subscriber` that flags
changed entries (`ConcurrentHashMap`). **Per-user isolation** (`BObixWatch.java:340-342`): `checkUser()` throws
`PermissionErr("Cannot access/modify another user's watch")` on `requestUser != this.user` (identity by
reference). Expired watches are swept on every `resolve()`.

## §509.5 — Auth & permissions (the security core) `[CERT]`/`[INFER]`

- **Authentication** is entirely the N4 web tier's ([B508]): `BObixServer.service()` does no credential check —
  SCRAM/Basic is enforced upstream and the authenticated `BUser` arrives via the `WebOp`/`ObixDecoder.getUser()`.
- **Writes are RBAC-gated** `[CERT]`: `ObixUtils.serviceWrite` → `parent.set(pary[idx], val, ot.getUser())`
  (`ObixUtils.java:558`); a denied write throws `PermissionErr` → oBIX `<err is="obix:PermissionErr">` (`:564`).
  Read responses set `writable="true"` only when `cx.canWrite()`.
- **Reads have NO oBIX-level export allowlist for the wide lobbies** `[CERT]`+`[INFER]`: `BStationLobbyAgent`
  (`/obix/config`) maps a URI to `"station:|" + decode(uri)` (`BStationLobbyAgent.java:38`) → **any station slot**;
  `BOrdLobbyAgent` (`/obix/ord`) does `BOrd.make(URLDecoder.decode(uri)).resolve(null, cx)` (`BOrdLobbyAgent.java:44`)
  → **any Baja ORD**. Both carry only `requiredPermissions = "r"` as `@AgentOn` registration metadata. `[INFER]`:
  so the read surface is bounded by the user's Niagara READ RBAC (whether each component re-checks read is a
  framework detail not settled here), but there is **no additional oBIX allowlist** on `config`/`ord` — a
  read-capable account can enumerate the whole tree / resolve arbitrary ORDs. [B458] `[CERT-live]` already
  extracted history + config this way with a normal account, corroborating the readable surface. The only
  CURATED namespace is `/obix/continuousControl` (§509.6).

## §509.6 — What is exposed by default `[CERT]`

| Lobby | Path | Exposes |
|---|---|---|
| `about` | `/obix/about` | server identity |
| **`config`** | **`/obix/config/*`** | **entire station component tree** (`station:\|slot:…`) — no export marking needed |
| **`ord`** | **`/obix/ord/<ord>`** | **any Baja ORD** resolved for the user |
| `continuousControl` | `/obix/continuousControl/*` | ONLY explicit `BObixExport` writable proxies (the curated namespace) |
| `histories` / `alarms` / `bql` / `contract` / `units` | `/obix/…` | history / alarm / BQL / type-contract / unit surfaces |
| `watchService` | `/obix/watchService` | per-user Watch mgmt (§509.4) |

`[INFER]` **SEC:** unlike `continuousControl` (an explicit `BObixExportFolder` allowlist), `config` and `ord`
publish the whole ORD space by default once the server is licensed and the request is authenticated — the oBIX
server is a broad read surface, curated only for writes (exports) not for reads. SOAP (`/obix/soap`) offers the
same via `read`/`write`/`invoke`.

## §509.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | BObixServer=BWebServlet+Soaplet at /obix (+soap/wsdl/xsd); child of BObixNetwork | `[CERT]` | `BObixServer.java:64,65,71` | PASS |
| 2 | license: export sub-key gates server; unlicensed → 403 | `[CERT]` | `BObixServer.java:372,227` | PASS |
| 3 | REST GET/PUT/POST + SOAP read/write/invoke dispatch | `[CERT]` | `BObixServer.service`; `ObixUtils.serviceWrite/Invoke` | PASS |
| 4 | server Watch: make→BObixWatch per-user, checkUser isolation, 30s lease | `[CERT]` | `BObixWatch.java:340-342`; `BObixWatchService` | PASS |
| 5 | writes RBAC-gated (parent.set(...,user)→PermissionErr) | `[CERT]` | `ObixUtils.java:558,564` | PASS |
| 6 | /obix/config = whole station tree; /obix/ord = arbitrary ORD; no read allowlist; continuousControl = only exports | `[CERT]`+`[INFER]` | `BStationLobbyAgent.java:38`; `BOrdLobbyAgent.java:44` | PASS |

**Tally:** 6 claims — all `[CERT]` load-bearing + `[INFER]` (read-surface breadth) on cited code, corroborated
live by [B458]. Block TYPE = **EVIDENCE**; API3 CLOSED. All load-bearing tokens re-verified inline.

## §509.8 — Connections & focus status

- **Closes [B499 §499.8]** — the obixDriver server half is now documented.
- Rides [B508]'s web tier: `/obix` is a `BWebServlet` mount authenticated by the same SCRAM/Basic negotiation;
  the RBAC on writes reuses the Baja permission model [B508] describes for `/ord`.
- **SEC feed to [B398]/[B490]:** the oBIX server's `config`/`ord` lobbies are a broad read surface (whole-tree
  enumeration for a read-capable account, no per-export allowlist) — the same class of exposure [B458] exploited
  legitimately; the operator mitigation is RBAC read-scoping + not licensing `export` where not needed.
- **Focus status:** `apis` 3/8 (API1–API3 closed). NEXT = API5 (`BAuthenticationScheme` SPI framework side).
