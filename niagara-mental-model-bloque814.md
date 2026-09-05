# B814 · Authoring a MODULE tag dictionary — ship a smart dictionary so your components are nav/search/hierarchy addressable `[CERT]`

> The AUTHOR-side residue over the tag framework ([Block 260]–[Block 270]): not the engine, but how a MODULE ships
> its OWN dictionary so its component types get tagged automatically — and become addressable by NEQL/search,
> hierarchy, and relations WITHOUT hand-tagging each instance. Answers the operator's "components should be
> nav/search/hierarchy addressable". Deliverable: a `types/logic.md` "ship a tag dictionary" recipe.
>
> **Sources**: docSource ORIGINAL (`javax.baja.tagdictionary.*`, `javax.baja.tag.*`, `javax.baja.sys.BNamespace`,
> `javax.baja.neql.BNamespaceScheme` — driver-verified by grep); REMITTANCE [Block 260] (tag API), [Block 261]
> (engine/`n:` built-in/indexes/JSON import-export), [Block 268]/[Block 269] (UI/docs), [Block 781] (categories/
> relations/hierarchy declaration), [Block 782] (query/search surface), [Block 584]-[Block 586] (hierarchy model).
> Markers: `[CERT]` docSource `file:line` · `[INFER]`.
>
> **Type:** `mixed` (framework REMITTANCE + the authoring recipe). Connects [Block 781]/[Block 782], [Block 260]-270.

## 814.1 — The three authoring primitives `[CERT]`
- **Namespace** — `javax.baja.sys.BNamespace` ("a namespace that holds only simple values", `BNamespace.java:11`;
  hidden from the nav tree, `:41`). Your module's tags live under a vendor namespace (e.g. `angeles:`); a tag with
  no prefix falls in the service's DEFAULT namespace (`BTagDictionaryService.java:93`). `[CERT]`
- **Dictionary** — `javax.baja.tagdictionary.BTagDictionary`: a component that holds tag/group/relation
  definitions, JSON-importable/exportable (its import/export keys are `JSON_NAMESPACE/JSON_TAGS/JSON_GROUPS/
  JSON_RELATIONS`, [Block 261]). Registered under the station's `BTagDictionaryService` (which HOSTS the
  dictionaries + a default-namespace pointer). `[CERT]`
- **Smart dictionary (the key to "no hand-tagging")** — `javax.baja.tagdictionary.BSmartTagDictionary` extends
  `BTagDictionary` and implements `javax.baja.tag.SmartTagDictionary` (`BSmartTagDictionary.java:45-46`). It holds a
  `rules` `BTagRuleList` (`:54-58`) that **"determine implied tags and relations for an `Entity`"** (`BTagRule.java:44`).
  So instead of tagging each `BColdRoom` by hand, a rule auto-applies. `[CERT]`

## 814.2 — The rule shape (auto-tag your types) `[CERT]`
A `BTagRule` = a `condition` + the `tags` it implies:
- `@NiagaraProperty condition` (`BTagRule.java:52-55`, a `BTagRuleCondition`; default `com.tridium.tagdictionary.condition.BNever`) — the test against an `Entity` (a component). Conditions compose (type match, existing-tag match, NEQL — [Block 263] condition algebra).
- `@NiagaraProperty tags = BTagInfoList` (`BTagRule.java:60-64`) — the tags (and implied relations) applied when the condition matches.
So a rule reads: "IF the entity is (condition: `type is angeles:BColdRoom`) THEN imply tags `angeles:equip`, `angeles:coldRoom` + a relation to its parent". The `BSmartTagDictionary` evaluates its `rules` over the component space → every matching component is addressable with zero per-instance tagging. `[CERT/INFER-recipe]`

## 814.3 — How a module SHIPS it `[CERT-grounded / INFER]`
1. Define the module's `BNamespace` (vendor prefix) `[CERT]`.
2. Author a `BSmartTagDictionary` with one `BTagRule` per component TYPE the module exposes (condition = type/tag
   match, tags = the domain tags + relations). `[INFER, recipe from §814.1-2]`
3. SHIP it: put the dictionary component in the module's `module.palette` ([Block 780]) so an integrator drags it
   under `Services/TagDictionaryService`; OR provide it as a JSON dictionary import ([Block 261]). Registration is
   by PLACEMENT under `BTagDictionaryService` (the service hosts whatever dictionaries it contains) — the same
   "register-by-placement" idiom as a service ([Block 778]). `[INFER, grounded]`
4. Result: the module's components resolve through `neql:`/`BNamespaceScheme` ([Block 782]), appear in
   tag-based nav/hierarchy ([Block 584]-586/[Block 781]), and search — without hand-tagging. `[INFER via B782/B781]`

## 814.4 — Kit implication → `types/logic.md` "ship a tag dictionary" `[CERT-grounded]`
Add a recipe: (a) declare a vendor `BNamespace`; (b) a `BSmartTagDictionary` + one `BTagRule` per exposed type
(condition→tags/relations) so components auto-tag; (c) ship it in `module.palette` for placement under
`TagDictionaryService`, or as a JSON dictionary. Contrast with the WRONG way (hand-tagging each instance, or
`exportTags` which is NOT the tag subsystem — [Block 266] §14). Pairs with [Block 781] (relations/hierarchy) and
[Block 782] (the query surface that then finds them). For the operator's modules: a `angeles` smart dictionary
would make ColdRoomPan/CompPan/DashboardPan components queryable by `equip`/`coldRoom`/`compressor` tags instead
of ORD paths (ties [Block 800]/[Block 813]: a servlet could resolve by tag-query instead of a brittle ORD).

## 814.5 — Self-verify
| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | Tags live under a `BNamespace`; unprefixed = service default namespace | `[CERT]` | `BNamespace.java:11,41`; `BTagDictionaryService.java:93` | Y — grep |
| 2 | `BSmartTagDictionary` holds `rules` (BTagRuleList) that imply tags/relations for an Entity | `[CERT]` | `BSmartTagDictionary.java:45-58`; `BTagRule.java:44` | Y — grep |
| 3 | A `BTagRule` = `condition` (default BNever) + `tags` (BTagInfoList) | `[CERT]` | `BTagRule.java:52-64` | Y — grep |
| 4 | Ship via palette placement under TagDictionaryService or JSON import; auto-tag by rule | `[INFER]` | §814.3, grounded in B780/B778/B261 | recipe |
| 5 | Result: NEQL/nav/hierarchy addressable without hand-tagging | `[INFER]` | [B782]/[B781]/[B584-586] | grounded |

**Tally:** `[CERT]` ×3 · `[INFER]` ×2. DESIGN/authoring block — the INFER recipe composes verified primitives (§11).

## 814.6 — Connections & open gaps
- REMITTANCE: [Block 260]-[Block 270] (tag framework — engine, API, `n:`, JSON, UI, docs), [Block 781]
  (categories/relations/hierarchy declaration), [Block 782] (query/search), [Block 584]-[Block 586] (hierarchy),
  [Block 780] (palette), [Block 778] (register-by-placement), [Block 266] (exportTags is NOT tags, §14).
- **B814-G1** (build/PoC, requires-execution): build an `angeles` `BSmartTagDictionary` with a `BColdRoom→equip/coldRoom`
  rule, place it under `TagDictionaryService`, and confirm a NEQL query (`n:equip and angeles:coldRoom`) returns
  the components without any per-instance tag — the recipe proven live.
