# Block 563 — The SecurityDashboard contributor SPI: an in-station PULL aggregation where each subsystem self-reports its posture as localized items on a 4-level status (info/ok/warning/alert) — the framework side of what [Block 112] saw as a consumer

**Session**: 2026-08-28
**Focus**: `access-control` (gap AC5 — the `javax.baja.security.dashboard` SPI; the CONTRIBUTOR contract that
[Block 112] observed only as the nss dashboard consumer)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 8-class dashboard package + a sweep for real implementors.
Interfaces, status enum, and the aggregate schema token-verified inline.
**Primary sources** `[CERT]`:
- `organized/baja/baja/vineflower/javax/baja/security/dashboard/{BISecurityDashboardItemProvider,
  BISecurityDashboardProvider,BISecurityDashboardProviderAgent,BSecurityItemStatus,SecurityDashboardItem,
  SecurityDashboardItemBuilder,SecurityDashboardConstants,LexiconFormatInfo}.java`.

**Scope**: the SPI a module implements to publish its own security posture into the station's SecurityDashboard.
[Block 112] documented the nss SecurityDashboard as a defensive VIEW (the consumer); this opens the framework
contract on the producing side. Does NOT re-open the external audit checklist ([Block 398]/[Block 490]) —
complements it.

---

## 563.1 A two-level provider interface [CERT]

The contract is layered `[CERT]`:
- `interface BISecurityDashboardItemProvider extends BInterface` `[CERT] :13` — the minimal producer: two
  methods, `int getSecurityDashboardItemsVersion()` `[CERT] :19` and
  `List<SecurityDashboardItem> getSecurityDashboardItems(Context)` `[CERT] :46`. It contributes ITEMS into some
  section.
- `interface BISecurityDashboardProvider extends BISecurityDashboardItemProvider` `[CERT] :10` — adds section
  ownership: `LexiconFormatInfo getSecurityDashboardSectionHeader(Context)` `[CERT] :13` and
  `BOrd getSecurityDashboardSectionHyperlinkOrd()` `[CERT] :15`. A Provider owns a whole SECTION — a localized
  header plus a **hyperlink ORD that navigates the operator to where the issue is fixed**.
- `interface BISecurityDashboardProviderAgent extends BIAgent, BISecurityDashboardProvider` `[CERT] :9` — the
  registration form. Being a `BIAgent` means the dashboard DISCOVERS providers by agent lookup (`@AgentOn`),
  not a hard-coded list; any module can drop in a provider agent and appear on the dashboard.

The `getSecurityDashboardItemsVersion()` int is a cheap change-detector: the aggregator polls the version and
re-pulls the (potentially expensive) item list only when it changed.

## 563.2 The item and its 4-level status [CERT]

`SecurityDashboardItem` `[CERT] :7-12` = `(LexiconFormatInfo summary, LexiconFormatInfo description,
BSecurityItemStatus status)` — a localized summary + description + a severity. Everything is **lexicon-based**
(`LexiconFormatInfo`), so posture text is i18n from the start. `BSecurityItemStatus extends BFrozenEnum`
`[CERT] :26` is the vocabulary `[CERT] :27-35`:

| Ordinal | Tag | Meaning |
|---|---|---|
| 0 | `securityStatusInfo` | informational (DEFAULT) |
| 1 | `securityStatusOK` | posture is good |
| 2 | `securityStatusWarning` | attention needed |
| 3 | `securityStatusAlert` | active risk |

`SecurityDashboardItemBuilder` `[CERT] :16-64` is the ergonomic front door — `makeInfo/makeOk/makeWarning/
makeAlert(summary, description)` — so a subsystem writes `builder.makeAlert("HTTP port open", "…")` rather than
hand-building the enum. Default status is `info` `[CERT] :35`.

## 563.3 The aggregate is a versioned JSON document [CERT]

`SecurityDashboardConstants` `[CERT] :4-10` defines the serialized shape the aggregator emits: a schema
`version` (`STATION_SCHEMA_VERSION_VALUE = 1`), `stationName`, `timestamp`, and `sections` — each section a
`sectionHeader` + `ord` + its items. So the dashboard the nss view ([Block 112]) renders is a **station-wide
JSON roll-up (schema v1)**: every registered provider becomes one section, every item a row, each carrying its
own fix-hyperlink. The `version` on both the document and each provider makes the whole thing pollable.

## 563.4 It is broadly adopted [CERT]

A sweep for implementors outside `baja` confirms the SPI is real infrastructure, not a stub — contributors
found `[CERT]`: `email` (`BEmailServiceSecurityDashboardProviderAgent`), `web` (`BWebService`), `orion`
(`BOrionService`), `program` (`BProgramObjectsSecurityDashboardProviderAgent`), `bacnet` Secure-Connect
(`BScDashboardProvider` + `BScSchemeSecurityDashboardItemProvider`), and the `abstractMqttDriver` Azure/GCP
authenticators. So a station's SecurityDashboard is assembled from many subsystems each grading itself.

## 563.5 Relation to the external checklist [CERT-synthesis]

This is the IN-STATION, self-reporting analog of the external audit tool from [Block 398]/[Block 490]: the
checklist runs from OUTSIDE against `niagara_home` + live ports; the SecurityDashboard runs from INSIDE, each
module self-grading with a fix-hyperlink. They are complementary — the dashboard cannot flag what no provider
reports (a subsystem with no provider agent is silent on the dashboard, even if misconfigured), which is exactly
why the external checklist still matters. A useful posture check is "which installed modules DON'T contribute a
provider" — those are dashboard blind spots.

## 563.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | ItemProvider (version + getItems) → Provider (section header + hyperlink ORD) → ProviderAgent (BIAgent, discovered) | [CERT] | BISecurityDashboard{ItemProvider:13-46,Provider:10-15,ProviderAgent:9} | token-checked ✓ |
| 2 | SecurityDashboardItem = LexiconFormatInfo summary+description + BSecurityItemStatus (lexicon/i18n) | [CERT] | SecurityDashboardItem.java:7-12 | token-checked ✓ |
| 3 | BSecurityItemStatus BFrozenEnum: info(0)/ok(1)/warning(2)/alert(3), default info | [CERT] | BSecurityItemStatus.java:27-35 | token-checked ✓ |
| 4 | Builder makeInfo/Ok/Warning/Alert(summary,description) | [CERT] | SecurityDashboardItemBuilder.java:31-64 | token-checked ✓ |
| 5 | Aggregate = versioned JSON (schema v1): stationName+timestamp+sections[sectionHeader+ord+items] | [CERT] | SecurityDashboardConstants.java:4-10 | token-checked ✓ |
| 6 | Real contributors: email/web/orion/program/bacnet-SC/mqtt | [CERT] | implementor sweep (paths cited) | grep-confirmed ✓ |
| 7 | Complements external checklist; a module without a provider is a dashboard blind spot | [CERT-synthesis] | rows 1,6 + [B398] | reasoned ✓ |

**Marker tally**: [CERT] ×6 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 6 of 7
rows token-verified inline.

## Connections

- **[Block 112]** — the nss SecurityDashboard CONSUMER/view; this is the producer SPI feeding it.
- **[Block 398]/[Block 490]** — the external audit checklist; complementary (outside-in vs inside-out).
- **[Block 324]** (email) / **[Block 287]** (bacnet-SC) — two concrete providers whose posture items land here.
- **[Block 558]–[Block 562]** — the RBAC posture (weak password policy, plain.1 encoder) is exactly the kind of
  finding a provider would surface as `warning`/`alert`.

## Open gaps (this block)

- The exact aggregator (which service walks the agents and emits the JSON, and the servlet/RPC that serves it)
  is named-not-opened — likely the nss `BSecurityService` ([Block 112] territory), low value here. Focus
  continues at AC6 (audit-trail wiring).
