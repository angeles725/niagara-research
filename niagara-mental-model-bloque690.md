# B690 — JACE_UMBRELLA tags + hierarchy (SC6): the biggest service in the config is 100% the stock Niagara v1.5 dictionary — zero applied tags, empty hierarchy

> Focus: **jace-station-config** · Gap **SC6** (TagDictionaryService + HierarchyService deployed). Sources:
> `config.bog` file.xml (SD P2, READ-ONLY). Redacted evidence:
> `sources/probes/B685-jace-station-config/tags-hierarchy.txt`.
> **SECRETS DISCIPLINE:** structure only. Marker `[CERT-hw]` (SD artifact). Tag subsystem internals =
> REMITTANCE focus `tags` [Block 260]–[Block 270]; hierarchy engine = focus `hierarchy` [Block 584]–[Block 590].
> This block answers only what THIS station deploys.

## 690.1 — The heavy `td:` weight is entirely one stock dictionary

[CERT-hw] TagDictionaryService (L285) is the single largest service in the config (~L285–731). Its 144 `td:`
occurrences are **all** inside that block (measured: first `td:` = L285, last = L724, count = 144; `grep -c`
outside the range = 0). It holds exactly ONE dictionary:

| dictionary | type | version | tag defs | relations | tag groups | stock/custom |
|---|---|---|---|---|---|---|
| Niagara | td:NiagaraTagDictionary | 1.5 | 25 | 8 | 0 | **STOCK** |

No Haystack dictionary, no custom/site dictionary. `schemaVersion=2` (standard N4).

- **25 tag definitions** (L293–518): bindHints, device, displayName, geoAddr/geoCity/geoCoord/geoCountry/
  geoCounty/geoPostalCode/geoState/geoStreet, hasPxView, input, name, network, node, output, point,
  alarmablePoint, schedule, station, targetSlotHint, template, vendor, version — the canonical Niagara tag set.
- **8 relation definitions** (L521–546): child, childDevice, childPoint, childNullProxyPoint, parent,
  parentDevice, parentNetwork, tagGroup — the canonical Niagara relations.

## 690.2 — Auto-tag rules + neqlize: all default

[CERT-hw] `neqlizeOptions` (L286) carries the verbatim N4 default exclusion lists (excluded relations
n:child/n:parent/n:tagGroup; excluded tags n:bindHints/n:displayName/n:geo*/n:hasPxView/n:history/… ). No site
overrides. `tagRules` (`td:TagRuleList`, L547–728) = the 6 canonical `IsTypeCondition` auto-tag rules that ship
with every N4 station: `object tags` (baja:Complex), `component tags` (baja:Component), `network tags`
(driver:DeviceNetwork), `device tags` (driver:Device), `point tags` (control:ControlPoint), `schedule tags`
(schedule:AbstractSchedule). No site-authored rule.

## 690.3 — Zero applied tags, empty hierarchy

[CERT-hw] **No component in the station carries an applied tag**: `grep -c 'n="tags"|BasicTagList'` over the
whole config = **0**. The dictionary is DEFINED but never USED — which follows necessarily from SC1–SC3: this
station has no field components to tag (only the Services layer + one NRIO relay point; no populated driver
tree, no control program). The auto-tag rules would fire only against components that do not exist here.

[CERT-hw] HierarchyService (`hierarchy:HierarchyService`, L225) has an **empty body** — no level definitions
(no Group/Query/Relation), installed but unconfigured.

## 690.4 — Verdict + the seed-station thesis, now decisive

[CERT-hw]+[INFER] The single biggest service in the config contributes **zero site semantics**: it is the
factory Niagara v1.5 dictionary that every N4 station carries, with default rules and default neqlize
exclusions, applied to nothing. This is the fifth independent confirmation of the seed-station read (SC1
template marker, SC2 no supervisor join, SC3 one relay point on a down module, SC5 no egress, and now SC6 stock
tags / empty hierarchy). [INFER] The `config.bog` is essentially the `NewJACEProvisioningStation.ntpl` template
(B685) instantiated with the barest site delta (one relay output, one admin user) — not a working field
application. The "heavy tagging" first impression from the bootstrap scan (`td:` ×144) was a MEASUREMENT
ARTIFACT of the stock dictionary's verbosity, not evidence of real modeling.

## Connections

- Tag subsystem (dictionaries, neqlize, relations, auto-tag rules) → focus `tags` [Block 260]–[Block 270];
  hierarchy engine → focus `hierarchy` [Block 584]–[Block 590]. Seed-station thesis chain → [Block 685]/
  [Block 686]/[Block 687]/[Block 689] (this focus). Template origin → focus `template` [Block 577].

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | one dictionary: Niagara v1.5 stock, 25 tags / 8 relations | [CERT-hw] | L290/L291 | grep-confirmed |
| 2 | all 144 td: inside TagDictionaryService (L285–731) | [CERT-hw] | grep first/last/count | measured |
| 3 | neqlize + 6 auto-tag rules all default (no site rule) | [CERT-hw] | L286/L547 | grep-confirmed |
| 4 | zero applied tags on components | [CERT-hw] | grep -c = 0 | grep-confirmed |
| 5 | HierarchyService empty | [CERT-hw] | L225 | grep-confirmed |
| 6 | heavy td: is stock verbosity, not site modeling (seed-station) | [INFER] | 690.1–690.3 + B685 | reasoned |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. The two absence claims (td: outside
range = 0; applied tags = 0) were independently grep-measured. 6/6 load-bearing citations confirmed. Evidence-
file secret-scan clean.

## Open gaps (this focus)

SC6 CLOSED. Next investigable: **SC7** (platform/orchestration services deployed — WebService/FoxService/
BoxService/JobService/BatchJobService/ProgramService/TemplateService/ProvisioningNiagara + any Program objects).
Then **SC8** (supporting stores + synthesis). SC4-G1 remains requires-execution.
