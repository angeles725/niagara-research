# Niagara N4 — own-modules-audit (OMA4): `sdash` is the shop's most sophisticated dashboard (WebSocket sync, own ORD scheme, per-agent-permissioned command types) wrapped in a 2186-class uber-jar (96% Jackson + Apache Commons) still on the legacy SEJOFA_C signer

**Focus**: own-modules-audit · **Gap**: OMA4 (sdash-rt) · **Session**: 2026-08-29 · **Block**: B644
**Sources** (`[CERT]` direct artifact): `/mnt/c/…/modules/sdash-rt.jar` (`META-INF/module.xml`, entry histogram, signature blocks). Dev/demo module (not production — only chihuahua is, [B643]).

**Scope**: identify sdash + grade vs [B636]. Signing chain = [B639]/[B392] (REMIT); reference = [B636].

---

## 644.1 Identity + the 12 types — the most feature-rich of the fleet

`[CERT]` `sdash-rt.jar!module.xml` — `moduleName="sdash"`, vendor SEJOFA, `description="SEFOJA Dashboard RT"`, 12 types under `com.sejofa.sdash.*`:
- `BSdashService` (the service) + `BSdashScheme` (a custom **ORD scheme** `sdash:` — most operator modules don't define one)
- `BSdashChannelService` + `BSdashWebSocketAcceptor` (a **WebSocket push channel** — real-time dashboard, beyond chihuahua's polling)
- `BSdashSyncService` (a sync service)
- 6 command-agent types `BSdash{Alarm,History,BQL,File,Nav,User}Commands`, each an `@AgentOn` `SdashService` with `requiredPermissions="r"`
- `BDateRangeEnum`

Architecturally this is the shop's most advanced dashboard: WebSocket real-time, a dedicated sync service, its own ORD scheme, and **per-agent `requiredPermissions` declarations** — notably, the command agents DO scope permissions (`r`), unlike the blanket module-level `type="all"` ([B640] P1). So sdash is partly ahead of the systemic pattern.

---

## 644.2 The 2186 classes = an uber-jar (96% bundled libraries)

`[CERT]` entry histogram — the operator's own code is only ~105 classes (4.8%); the rest is shaded third-party:
```
com/fasterxml/jackson/**          1042   (full Jackson stack: core+databind+annotations)
org/apache/commons/collections4    543
org/apache/commons/lang3           362
org/apache/commons/io              212
com/flipkart/zjsonpatch             22   (JSON-Patch/diff, depends on Jackson)
com/sejofa/sdash/**                105   (own: responses, sockets, sync, util, commands)
```
`[CERT]` `META-INF/maven/*` POM fragments confirm each bundled lib. Non-class payload: an `rc/` web app (`index.html`, `js/{app,client,Alarms,Buildings,Equipment,History,Settings}.js`, `css/app.css`), `WEB-INF/web.xml` + `jetty-web.xml` (servlet context), `module.palette`, lexicon.

**Bloat verdict**: the size is explained (Jackson + 3 Apache Commons + zjsonpatch) but largely UNNECESSARY — Niagara's own platform ships Apache Commons and its own JSON tooling; shading full Jackson (1042 classes) to get JSON serialization is heavy where `jsonToolkit` ([B335]) or Gson (used elsewhere, [B643]/[B645]) would be far smaller. Shading avoids version conflicts but inflates the module and its every update.

---

## 644.3 Signing + permissions

`[CERT]` — the jar carries **BOTH** `META-INF/NIAGARA4.{SF,RSA}` AND `META-INF/SEJOFA_C.{SF,RSA}` — dual-signed, and it is the fleet's straggler still on the legacy `SEJOFA_C` chain ([B639]/[B640] P3). `[CERT]` module.xml declares a `<req-permission>` for **`REFLECTION`** (`purposeKey`: JSON serialization uses Java reflection) inside the permission block — legitimate for Jackson, but it grants the module broad JVM reflection access ([B635] `<java-permissions>` track → `checkTpk=true`). Plus the systemic `type="all"` groups.

---

## 644.4 Grade + recommendation (dev/demo priority)

- **Architecture**: strong (WebSocket, sync, own scheme, per-agent permissions) — reuse these patterns in the shop template ([B640]/OMA8), especially the per-agent `requiredPermissions`.
- **Uber-jar**: prefer Niagara-provided Commons + a lean JSON lib over shading full Jackson; if Jackson is required, factor it into a shared `jackson-rt` module rather than per-dashboard ([B640] cross-cutting dedup, mirrors the Gson dup in [B643]/[B645]).
- **Signing**: re-sign with `angelessignerCA`, drop the legacy `SEJOFA_C` block ([B639]).
- **Permissions**: keep the per-agent `r`; drop the module-level `type="all"`; keep `REFLECTION` only if Jackson stays.
- Severity is LOW — dev/demo, not production ([B643]).

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | sdash = SEFOJA Dashboard RT, 12 types incl. WebSocketAcceptor, SyncService, BSdashScheme (sdash: ORD scheme), 6 command agents requiredPermissions="r" | [CERT] | sdash-rt.jar!module.xml | ✅ unzip -p |
| 2 | 2186 classes: Jackson 1042 + commons-collections4 543 + lang3 362 + io 212 + zjsonpatch 22 + sejofa 105 | [CERT] | entry histogram + META-INF/maven | ✅ unzip -l |
| 3 | rc/ web app + WEB-INF/web.xml + jetty-web.xml | [CERT] | unzip -l | ✅ unzip |
| 4 | dual-signed NIAGARA4 + SEJOFA_C (legacy straggler) | [CERT] | unzip -l META-INF | ✅ unzip |
| 5 | declares <req-permission> REFLECTION (Jackson) + type=all groups | [CERT] | module.xml | ✅ unzip -p |

**Tally**: [CERT] ×5 · [INFER] ×0 · direct-artifact block. Histogram + signer + types token-checked against the jar this session.

## Connections

- **[B640]** — the size anomaly + signer straggler; sdash's per-agent perms are AHEAD of P1. **[B639]** — re-sign to ANGELES. **[B635]** — REFLECTION on the `<java-permissions>` track (checkTpk). **[B335]** — jsonToolkit as a lighter JSON path. **[B643]/[B645]** — the shared-JSON-lib dedup theme (Gson there, Jackson here).
- Forward: OMA8 (template: adopt per-agent perms + WebSocket pattern; factor shared libs).

## Gaps uncovered

- None new. Whether sdash's own 105 classes have their own defects is out of scope for a packaging audit (dev/demo module, not production).
