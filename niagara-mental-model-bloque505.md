# Block 505 — `framework-drivers` FD8: `weather` — the Tridium NWS/EPA weather Service (not a driver): hardcoded US-gov XML feeds fetched over plaintext HTTP by default, weather data exposed as component slots (no point proxies), a cleartext-String AirNow API key, NO license gate, and an external-dependency durability risk (de-escalated from the sweep's "dead endpoint" claim)

> **Focus:** `framework-drivers`, gap **FD8** (last, low-priority) — the core Tridium `weather` module (~52 cls).
> NOT `weatherUnderground` (1-cls stub, dismissed) nor Reflow's weather widget ([B236], app-level). READ-ONLY,
> decompiled; no run. Markers §3.
> **Sources:** FUENTE 3 — `organized/weather/weather-rt/decompiled/…`. FUENTE 1 — [B501] (Service-tier peer),
> [B499] (plaintext-default pattern). FUENTE 2 — n/a. **FUENTE web** — NWS notification pages (below), used to
> FALSIFY a sweep claim. Evidence delegated to a `sonnet` sweep; load-bearing file:line RE-VERIFIED inline, and
> one operational conclusion DE-ESCALATED after web check (§505.7).

## §505.1 — A Service, not a driver `[CERT]`

Like [B501] openAdr, `weather` is Service-tier — no `BDeviceNetwork`/`BDevice` anywhere:
`BWeatherService extends BAbstractService` (`javax/baja/weather/BWeatherService.java:104`), holding per-location
`BWeatherReport` components, each with a `BWeatherProvider` (abstract) — concrete `BNwsWeatherProvider` (HTTP to
US-gov feeds) or `BFoxWeatherProvider` (clones a peer station's report over Fox). Weather data lives on
`BCurrentConditions`/`BForecast` as **typed component slots** (`temp`→BStatusNumeric °F, `state`→BStatusEnum,
`humidity`→BStatusNumeric %RH…) — `[CERT]` **no point-proxy layer**: values are read directly by Baja slot path.

## §505.2 — Data sources: hardcoded US-gov XML over plaintext HTTP `[CERT]`

`BNwsWeatherProvider` default endpoints (all `flags=4`, US government, plain hostnames):

| Feed | Server | Path |
|---|---|---|
| Current obs | `www.weather.gov` (`:67`) | `/xml/current_obs/` (`:68`) |
| Forecast (NDFD) | `graphical.weather.gov` (`:65`) | `/xml/sample_products/browser_interface/ndfdBrowserClientByDay.php` (`:66`) |
| Advisories (CAP) | `alerts.weather.gov` (`:69`) | `/cap/` |
| Air Quality | `www.airnowapi.org` (`:71`) | `/aq/observation/zipCode/current/…` |

HTTP client is **hand-rolled** (`FeedReader.getFeed()`), no 3rd-party SDK — builds
`(secure ? "https://" : "http://") + host + uri` (`FeedReader.java:55`) with manual redirect-follow (max 5). `[CERT]`
All provider call sites pass `secure=false` → **plaintext HTTP (port 80) by default**; HTTPS only if the operator
edits the server properties to https hosts (the redirect logic does upgrade if the host 301s).

## §505.3 — Data model `[CERT]`

Parsing is Baja's own `XParser`/`XElem` (no DOM/SAX/3rd-party). `NwsCurrentReader` maps NWS current-obs elements
(`temp_f`, `relative_humidity`, `wind_mph`, `pressure_in`, `dewpoint_f`, `visibility_mi`, …) to the slots;
windchill/heat-index derived locally if absent. `NwsForecastReader.parseDwml()` extracts NDFD DWML
max/min temp + precip-probability + weather-summary into `BForecast` day slots. EPA AirNow adds `ozone`/
`particulateMatter`. CAP/Atom advisories → `BAdvisory` in a `BAdvisoryContainer` with an alarm ext.

## §505.4 — Polling `[CERT]`

`updatePeriod` default **1 hour**, min 15 min (`BWeatherService.java:105`); `Clock.schedulePeriodically` fires each
`BWeatherReport` as a queued `Invocation` on a worker thread. `staleThresholdTime` default 3 h (marks data stale on
fetch failure).

## §505.5 — Auth & security `[CERT]`

- NWS feeds (obs/forecast/advisories) need **no authentication** (public XML).
- **AirNow API key** is stored as a **cleartext `String`** (`airQualityApiKey`, `BWeatherService.java:106` — NOT a
  `BPassword`), appended to the URL as `&API_KEY=<key>` (`airQualityApiKeyField`). `[INFER]`: a plaintext secret in
  `config.bog` **and** on the wire (plaintext HTTP query string by default) — weaker at rest than every other FD
  module's `BPassword` credential, and log/proxy-exposed in the URL.
- Transport plaintext by default (§505.2).

## §505.6 — License gate: NONE `[CERT negative]`

grep of the whole `weather-rt` for `getFeature`/`getLicenseFeature`/`LicenseManager` = **0**. `[CERT]` This is the
**only framework-drivers module with no license gate** — weather is unmetered/free (consistent with it being a
value-add Service over free public feeds, not a metered protocol driver).

## §505.7 — External-dependency durability risk (DE-ESCALATION of the sweep) `[CERT-web]`/`[INFER]`

The sweep asserted the NWS NDFD/current-obs XML endpoints were "decommissioned in early 2023 → forecast broken by
default on every live N4.14." **A web check does NOT support that operational conclusion** and it is DOWNGRADED:
- `[CERT]` the module hardcodes the legacy NWS XML feeds (§505.2) and was built 2024-05-28.
- `[CERT-web]` (NWS notification pages, retrieved 2026-08-25) NWS has **migrated the NDFD XML Web Service onto new
  AWS architecture with WSDL differences affecting legacy SOAP users**, and `ndfdBrowserClientByDay.php` is still
  referenced as an active interface — i.e. **not a confirmed decommission**, but a migrated/changed service.
- `[INFER]` therefore the real finding is an **external-dependency durability RISK**: a hardcoded dependency on
  legacy US-gov XML feeds whose backing infrastructure NWS is actively changing, over plaintext HTTP, with a
  3-hour stale-marker as the only failure signal. Whether N4's specific legacy client still returns data is
  **UNVERIFIED** (requires a live fetch, a §12 requires-execution check) — NOT asserted as broken.

This DE-ESCALATION (a subtracted over-claim) is the block's honest core; the sweep's certainty was the hypothesis
that FALSIFY-BEFORE-REPORTING caught.

## §505.8 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Service-tier (BWeatherService=BAbstractService); data as component slots, no point proxies | `[CERT]` | `BWeatherService.java:104`; BCurrentConditions slots | PASS |
| 2 | hardcoded NWS/EPA XML endpoints; hand-rolled FeedReader; plaintext HTTP default (secure=false) | `[CERT]` | `BNwsWeatherProvider.java:65-71`; `FeedReader.java:55` | PASS |
| 3 | XParser/XElem parse; current+forecast+AQ+advisory model | `[CERT]` | `NwsCurrentReader`/`NwsForecastReader` | PASS |
| 4 | poll default 1h min 15min; stale 3h | `[CERT]` | `BWeatherService.java:105` | PASS |
| 5 | AirNow apiKey = cleartext String (NOT BPassword), in URL query | `[CERT]`+`[INFER]` | `BWeatherService.java:106` | PASS |
| 6 | NO license gate (only FD module without one) | `[CERT negative]` | grep getFeature/license = 0 | PASS |
| 7 | NWS endpoints migrated (AWS/WSDL), NOT confirmed-decommissioned — durability risk, live-status unverified | `[CERT-web]`+`[INFER]` | NWS notices (2026-08-25); DE-ESCALATION | PASS |

**Tally:** 7 claims — 4 `[CERT]`/`[CERT negative]` load-bearing + `[CERT-web]` + `[INFER]` (at-rest key, durability).
Block TYPE = **EVIDENCE**; FD8 CLOSED. One sweep over-claim DE-ESCALATED via web falsification. Load-bearing tokens
re-verified inline.

## §505.9 — Connections & focus status

- Service-tier peer of [B501] openAdr (both `BAbstractService`, not drivers); shares plaintext-HTTP-default
  fragility with [B499] oBIX.
- **Weakest credential-at-rest in the focus:** AirNow key as cleartext `String` (every other FD module used
  `BPassword`) — feed to [B398]/[B490].
- **License outlier:** the only FD module with no gate.
- **Methodology note:** §505.7 is a FALSIFY-BEFORE-REPORTING / DE-ESCALATION case — the sweep's "dead endpoint"
  escalation was refuted by web check and downgraded to an unverified durability risk. Recorded for the §18 retro.
- **Focus status:** `framework-drivers` **10/10 — ALL investigable gaps closed.** NEXT = focus-closing SYNTHESIS
  block + §18 retrospective + push.
