# Campaign 9 — research candidates + kit/client/station backlog

Author: investigador1 (Opus). Seeds recorded across Campaign 8 (B805/B808/B810/B812/B813/B814/B815/B816/B817 +
station findings). Ranked by **value × tractability**; each tagged **KIT** (build-n4-module kit), **CLIENT** (our
module source), or **STATION** (PANCCADIA/REFLOW config). Same shape as campaign8-research-candidates.md.

## Ranked backlog

| # | Item | Class | Value | Tract. | Seed | Note |
|---|---|---|---|---|---|---|
| 1 | **Protection-latch seam scaffold** — pure-Java SR latch (set-dominant, first-out capture, trip reason, explicit operator reset, optional B803 step-up on reset, BAlarmSourceExt hookup) + JUnit | **KIT** (fixture) | HIGH | HIGH | B805 §805.3 gap (Tridium ships no SR latch) | **C9 PR1 fixture — built + tested now (§19 PoC below)** |
| 2 | **Write-path coverage lint + the W1-W13 matrix tests** — every dashboard-writable slot has a matrix row/test; lint on `Clock.schedule(≤0)` + link-target writes | **KIT** | HIGH | HIGH | B816 (the user's "solid" mandate) | pure-seam, deterministic; QA has the W-list |
| 3 | **Station config: `fallback=false` on the 22 relays** | **STATION** | HIGH | HIGH | B810 §810.8 / B816 | deploy-safety: relay HOLDS (resistance/compressor ON) on stop/reload with null fallback; config-only, no build |
| 4 | **rt component-lifecycle testable seam** + kit runner `run-station-test.sh` + `build.sh` moduleTestJar step | **KIT** | HIGH | MED | B815 | backs the lifecycle-test mandate; the RUN needs a station (BTest has no deterministic clock) |
| 5 | **DashboardPan servlet hardening** — server-side facet enforcement, `x-niagara-csrfToken` (not just X-Requested-With), parse-error→400, per-ORD write lock | **CLIENT** | HIGH | MED | B813/B803 §803.5/B796 | security; the CSRF-token correction is live |
| 6 | **Structure lints L1-L9** (module layout conformance) | **KIT** | MED | HIGH | B817 | mechanical; statically decidable |
| 7 | **Heartbeat/liveness monitor scaffold** + lint "component owns tickets but no `lastTick`" | **KIT** | MED | HIGH | B812/B775 §775.6 | the author-built independent monitor Tridium doesn't ship |
| 8 | **Health surface in ColdRoomPan/CompPan** — fault-status slot + alarm ext + `lastTick` | **CLIENT** | MED | MED | B808/B805 §805.4 | who-watches at the component level |
| 9 | **`station-load.sh`** probe recipe (companero) | **KIT** (tool) | MED | MED | companero | copy a running station without mounting the FS |
| 10 | **bog-audit CHECK12 + station-logic CHECK13+** (Cuarto 1 tile-number crossing, relay double-source, link direction) | **KIT** (checks) / **STATION** (fixes) | MED | MED | B816 + station | some need station-topology knowledge |
| 11 | **Station config: Cuarto 1 links + re-hide `intervalExpired`** | **STATION** | MED | HIGH | station | config fixes |
| 12 | **Tag dictionary `angeles`** | **CLIENT** | LOW-MED | MED | B814 | nav/search/hierarchy addressability |

## Recommendation
C9 research/build core = **#1 protection-latch fixture** (built below) + **#2 write-path coverage lint & matrix tests**
— together they discharge the user's "I want the change/overlap-testing part solid" mandate. Pair with the one urgent
**STATION** safety fix **#3 (`fallback=false` on the 22 relays)**. #4-#7 are the next KIT tier; #5/#8/#12 are CLIENT;
#10/#11 are station-topology-dependent. Kit lints (#2/#6/#7) extend the C6/C8 lintable-vs-advisory doctrine.
