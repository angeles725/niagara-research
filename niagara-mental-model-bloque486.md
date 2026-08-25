# Block 486 — The license BRAND system: one `tridium:brand` feature (brandId + four `PatternFilter` accept-lists) gates station↔station and Workbench↔station interoperability at the Fox handshake and the platform-daemon HTTP header — the OEM interop-lock mechanism

> **Focus:** `licensing`. **Question (operator):** cómo la licencia controla QUIÉN se conecta con QUÉ.
> READ-ONLY, decompiled source + on-disk XML; no binary run. Markers §3.
>
> **Sources:** `organized/baja/…/com/tridium/sys/license/Brand.java` + `dom/Feature.java` (Feature.Brand) +
> `NLicenseManager.java`; consumers `organized/fox/fox-rt/…/Acceptor.java`, `…/session/Tuner.java`,
> `platform/platform-rt/…/daemon/BDaemonSession.java` + `…/license/LicenseInfo.java`,
> `sources/decompiled/niagarad-ext/…/http/WebServer.java` + `…/license/Brand.java`; on-disk
> `Webs.license` `brand` feature.

## §486.1 — The `brand` feature `[CERT]`

Brand data lives in a single license feature `brand` (vendor `tridium`), loaded lazily in `Brand.init()`:
`checkFeature("tridium","brand")` (`Brand.java:68`); `brandId = feature.get("brandId")`, null → `LicenseException
("Missing brandId in brand feature")` (`:69-71`). Four accept-lists from feature attrs: `accept.station.in`,
`accept.station.out`, `accept.wb.in`, `accept.wb.out` (`:73-76`). Each is a `Brand.AcceptList` wrapping
`PatternFilter[]`: `feature.get(id, "*")` → `PatternFilter.parseList(str, ";")` (`:87-91`) — a **`;`-separated
list of glob `PatternFilter`, default `"*"`** (accept-all; `PatternFilter.java:88-90`). Getters
`getAcceptStationInString`/…/`getBrandId` (`:21-44`). The typed `Feature.Brand` DOM subclass reads the same XML
attrs (`dom/Feature.java:152-176`).

## §486.2 — Match semantics `[CERT]`

`AcceptList.accept(brandId)` (`Brand.java:105-121`): **returns true immediately if `brandId == null`** (an
unbranded peer is always accepted); else true if any pattern matches. `check()` (`:99-103`) throws
`LicenseException("Brand incompatibility [<id>] <brandId> != <patternString>")` on no match. `checkStationIn/Out`,
`checkWbIn/Out` (`:46-64`) delegate to the direction-appropriate list. `"*"` matches every string (accept-all).

## §486.3 — Enforcement — two channels `[CERT]`

**Channel 1 — Fox (station↔station, WB↔station):** `Acceptor.accept(FoxSession)` (`fox-rt/…/Acceptor.java:14-30`)
classifies the peer from the Fox hello (station if `station.name`/`app.name=="Station"`) and dispatches:
`acceptStationIn`→`Brand.checkStationIn(brandId)` (`:32-44`), `acceptStationOut`→`checkStationOut` (`:46-63`),
`acceptWbIn`→`checkWbIn` (`:65-72`), `acceptWbOut`→`checkWbOut` (`:74-81`). Each side advertises its own brand in
the hello (`BFoxConnection.java:204` / `BFoxService.java:1095` `hello.add("brandId", Brand.getBrandId())`); the
receiver runs it through its own accept-list. Invoked at handshake ("tune") from `session/Tuner.java:416,699,709`
(server) and `BFoxClientConnection.java:308` (client); a `LicenseException` propagates out of `Acceptor.accept`
and **aborts the connection** (`Tuner.java:415-422`).

**Channel 2 — Platform daemon (HTTP):** outbound `BDaemonSession` sets `Baja-Station-Brand` header (`:1238`) then
`checkServerBrand()` (`:1243-1253`) reads the server's `Baja-Station-Brand` and enforces `Brand.checkWbOut` (if
this side is WB/tool) or `checkStationOut` (`:1248-1250`). Inbound the daemon checks the incoming header:
`WebServer.java:1710-1715` → `Brand.checkWbIn(...)` / `checkStationIn(...)` (native `niagarad.license.Brand`
boolean variants).

## §486.4 — `brandId` semantics + one-brand rule `[CERT]`

`brandId` = the OEM/brand identity string (e.g. `Webs`, `Tridium`). It stamps the on-disk license filename
(`LicenseInfo.getBaseName` = `TextUtil.capitalize(brandId)` → `Webs.license`, `LicenseInfo.java:208-213`), the
platform brand part, and the license summary. **One brand per host:** `NLicenseManager.addFeature` — a second
feature whose key == `tridium:brand` → `fatalLicenseFault = "Cannot have multiple branded licenses"` +
`LicenseDatabaseException` (`NLicenseManager.java:27,220-227`). (The "n branded licenses" some decompilers show
is this same string mis-rendered; vineflower is authoritative.)

## §486.5 — Cross-brand gating is MUTUAL `[CERT]`

`LicenseInfo.allowsStationAccess(checkBrand)` (`LicenseInfo.java:191-201`): null brand → allow; else
**`my accept.station.in` must accept THEIR brandId AND THEIR `accept.station.out` must accept MY brandId** —
a two-way handshake. `allowsWbAccess` (`:179-189`) identical for `accept.wb.*`. So brand X↔Y connect only if X's
in-list matches Y AND Y's out-list matches X. Default `"*"` (absent attr) and `null` peer brand both mean
"accept anything" → a stock all-`*` license imposes **no** cross-brand restriction; an OEM narrows these lists
(e.g. `accept.station.in="TheirBrand"`) to **lock interop to its own brand**.

## §486.6 — On-disk (Webs.license) `[CERT]`

```xml
<feature name="brand" brandId="Webs" accept.station.in="*" accept.station.out="*"
         accept.wb.in="*" accept.wb.out="*"/>
```
brandId `Webs`, all four accept-lists `*` → open/non-restrictive posture (accepts any brand, both directions).
An interop lock would replace the `*`s with explicit `;`-lists of brandId patterns.

## §486.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | `brand` feature = brandId + 4 accept-lists (PatternFilter `;`-lists, default `*`) | `[CERT]` | `Brand.java:68-91` | PASS |
| 2 | null brandId always accepted; `*` matches all; mismatch → LicenseException | `[CERT]` | `Brand.java:99-121`; `PatternFilter.java:88-90` | PASS |
| 3 | Fox `Acceptor` gates station-in/out + wb-in/out at handshake; LicenseException aborts | `[CERT]` | `Acceptor.java:14-81`; `Tuner.java:415-422` | PASS |
| 4 | Platform HTTP `Baja-Station-Brand` header gated in/out (BDaemonSession/WebServer) | `[CERT]` | `BDaemonSession.java:1238-1253`; `WebServer.java:1710-1715` | PASS |
| 5 | brandId stamps license filename; one brand/host (dup → fatal) | `[CERT]` | `LicenseInfo.java:208-213`; `NLicenseManager.java:220-227` | PASS |
| 6 | Cross-brand mutual (my-in accepts theirs AND their-out accepts mine); `*`/null = open | `[CERT]` | `LicenseInfo.java:179-201` | PASS |

**Tally:** 6 claims, 6 `[CERT]`, 0 `[INFER]`.

## §486.8 — Connections

- Answers "quién se conecta con quién" — the license's brand is the interop/OEM-lock control on both Fox and
  platform-HTTP channels. Builds on [B483] (brand feature anatomy), [B420]/[B414-B419] (Fox
  supervisor↔subordinate), [B478 §C] (platform daemon). Feeds `docs/niagara-licensing.md`.
- Open: [B486-G1] whether a Fox/SC connection ALSO cross-checks the peer certificate/host beyond brand (SRP6/PKI
  is [B420]); brand is the license-layer gate, orthogonal to transport auth.
