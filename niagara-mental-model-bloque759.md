# B759 · Lexicon / i18n + the -doc/help profile — display-name and help authoring for a module, code-grounded

> **Scope**: how a module authors its display names (module.lexicon), how localization/locale selection works,
> when a label comes from a facet vs the lexicon, and how a `-doc`/help profile is shipped. Includes the current
> state of OUR modules' lexicons. Foco: **module-authoring** (MA6).
>
> **Sources**: FUENTE 3 — `organized/baja/baja/vineflower/javax/baja/util/Lexicon.java`, `.../sys/{BObject,BBoolean,BEnumRange}.java`;
> Tridium `control-rt.lexicon`, `niagaraLexiconFr`, `clLexiconFr_V1_3`, `docHoneywellSpyder`; our
> `ColdRoomPan-rt/module.lexicon` etc. FUENTE 2 — devguide `localization.txt`, `deployingHelp.txt`,
> `modules.txt`. FUENTE 1 — B25 (help/bajadoc overview), B116 (docHoneywellSpyder help-bundle exemplar), module-anatomy B629-636.

---

## 759.1 — `module.lexicon` format + resolution `[CERT]`
- Java `.properties` (`key=value`, `#` comments) at the JAR ROOT, named `<moduleName>.lexicon` (in the source
  tree it is `module.lexicon`; the build renames it) — `localization.txt:45-48`.
- Resolution: `Lexicon.make(Class)` uses the declaring module; `BObject.getLexicon() =
  getType().getModule().getLexicon()` (`BObject.java:98-100`) — every component resolves against ITS declaring
  module's lexicon. `get(key)` checks the locale file then the root fallback; **`getText(key)` returns the KEY
  ITSELF when missing** (`Lexicon.java:206-221`).
- **Key convention (in practice): bare TYPE name and bare SLOT name** — e.g. `ColdRoom=Cuarto frio`,
  `setpoint=Consigna`. These keys are **module-global**, so a slot name shared by two types COLLIDES (one
  label wins). The dotted `<type>.<slot>` form is NOT used for slot resolution (Tridium's dotted keys are
  `%lexicon()%`/enum indirection).

## 759.2 — Display-name derivation + the friendly fallback `[CERT]`
Slot display name resolves in order (`localization.txt:89-116`): (1) a `displayNames` NameMap slot on the
instance (BFormat), (2) the LEXICON keyed by slot name, (3) a **Slot-Default fallback**: frozen →
`TextUtil.toFriendly(name)` (camelCase → "Camel Case"), dynamic → `SlotPath.unescape(name)`. So a MISSING key
silently becomes the friendly-cased raw name — which is why Tridium's `control-rt.lexicon` is only ~51 lines
and localizes almost no slots (it leans on `toFriendly`). Type display name = the lexicon keyed by the type
name, same fallback.

## 759.3 — Facet labels vs lexicon `[CERT]`
- Boolean `trueText`/`falseText`: **facet first, lexicon fallback** (`BBoolean.toString:133-149`; the facet
  string is itself a BFormat, so it can embed `%lexicon(module:key)%`).
- Frozen enum tag: **lexicon first** (module = declaring type, key = the tag), `toFriendly` fallback — this is
  why our ColdRoomPan lexicon lists `single=Simple`, `staged=Por etapas`, `schedule=Horario` for
  `BStagingMode`/`BDefrostMode`.
- Dynamic `BEnumRange`: label from the range's `"lexicon"` FACET option (`BEnumRange.java:130,158-183`), not
  the module lexicon.

## 759.4 — i18n / locale selection `[CERT]`
- A locale file has the SAME base name (`<module>.lexicon`), disambiguated by a `language` attribute in a
  `<lexicons>` manifest entry OR a locale sub-directory. Two conventions: a provider module carrying one
  language for many modules (`niagaraLexiconFr` → `<lexicon module="control" resource="control.lexicon"
  language="fr" default="true"/>`), or a locale subdir (`clLexiconFr/.../fr/control.lexicon`). The
  `<lexicons>` manifest element is ONLY for a module that supplies lexicons FOR OTHER modules — a module's own
  root lexicon is auto-registered, so our built manifests have no `<lexicons>` (correct).
- The framework picks the locale from `Sys.getLanguage()` (VM default, overridable by `-locale:<lang>`); the
  web tier precedence is `User.language` → HTTP `Accept-Language` → station default.

## 759.5 — OUR modules' lexicon state `[CERT]`
- ColdRoomPan-rt: fully populated Spanish (46 lines). DashboardPan-rt: populated English (37). DashboardPan-ux:
  1 type key only. **CompPan-rt: header only, ZERO keys → every type/slot renders via `toFriendly`.**
  DashboardPan-wb: empty. Our Spanish is HARD-CODED in-place in the single root lexicon (the "one language
  in-place" model), NOT via the locale-file indirection — fine for a single-language deployment, but there is
  no `_es`/`_en` variant to switch.
- Actionable: fill `CompPan-rt/module.lexicon` (it is the one gap that leaves compressor slots showing raw
  camelCase); the others are in good shape.

## 759.6 — The `-doc`/help profile `[CERT]`
- Help ships as a SEPARATE module part with `runtimeProfile="doc"` (`deployingHelp.txt:10`) — a `doc/` tree of
  HTML + a `toc.xml` (JavaHelp TOC 1.0 DTD, `<tocitem text= target= image=>`) + `style.css`. Exemplar =
  `docHoneywellSpyder` (B116): empty `<types>`, payload is the `doc/` bundle.
- Two guide-help conventions: **Guide-on-Target** (per component type) →
  `doc/<moduleName>-<TypeName>.html`; **On-View** (per Workbench view) → `doc/<moduleName>-<ViewTypeName>.html`.
  The resolution root is the lexicon key `help.guide.base` (e.g. `control-rt.lexicon:10
  help.guide.base=module://docUser/doc`); absent it, Guide-on-Target looks in the module's own `doc/`.
- Build: the doc part applies `com.tridium.niagara-doc`, `indexJars(...)` for full-text search, and a `docCopy`
  task; it injects the help stylesheet + Index/Prev/Next nav from the TOC. **bajadoc** (API reference from
  Javadoc) is a separate `com.tridium.bajadoc` plugin + an aggregating `com.tridium.bajadoc-module` part.
- **Our modules today have NO -doc part, no `doc/` dir, no toc.xml, no `help.guide.base`.** To add one:
  a `ColdRoomPan-doc` part (`runtimeProfile=doc`) + `src/doc/toc.xml` + `ColdRoomPan-ColdRoom.html` etc.
  (Guide-on-Target) + optional `help.guide.base` in the rt lexicon. Low priority vs the lexicon.

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | module.lexicon = .properties at jar root <moduleName>.lexicon; resolves against declaring module; missing key → key itself | [CERT] | localization.txt:45-48; Lexicon.java:206-221; BObject.java:98-100 |
| 2 | Keys = bare type/slot name, module-global (collide); missing → toFriendly fallback | [CERT] | localization.txt:89-116; control-rt.lexicon |
| 3 | trueText/falseText facet-first; frozen enum lexicon-first; BEnumRange from "lexicon" facet option | [CERT] | BBoolean.java:133-149; BEnumRange.java:130,158-183 |
| 4 | Locale files = same base name + language attr or subdir; picked by Sys.getLanguage()/Accept-Language | [CERT] | niagaraLexiconFr manifest; localization.txt:191-222 |
| 5 | Our state: ColdRoomPan ES full, DashboardPan-rt EN, CompPan-rt EMPTY, no locale variants | [CERT] | our module.lexicon files |
| 6 | -doc part runtimeProfile=doc + doc/ + toc.xml; Guide-on-Target doc/<mod>-<Type>.html; we ship none | [CERT] | deployingHelp.txt:10,486-540; docHoneywellSpyder; our repos |

**Tally**: 6 [CERT]. No unmarked claims.

## Connections
- **B25** (help/bajadoc overview), **B116** (docHoneywellSpyder help bundle), module-anatomy **B629-636**
  (manifest `<lexicons>`), **B730** (lexicon mentioned), **B755** (enum/flag bits the labels describe).
  Forward: **B760** (audit — CompPan lexicon is a punch-list item).

## Open gaps
- **B759-G1**: authoring a real `-doc` part for our modules (toc.xml + Guide-on-Target HTML) — an
  implementation task (requires-execution).
