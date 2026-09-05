# Block 792 — `palette-lexicon-agents` extractor + census: duplicate bare lexicon keys across Tridium exemplars (B759 hazard confirmed real)

> **§20 DOCUMENT-mode capture** (not gap-discovery). Records a new module-navigator command and the one-shot
> census it produced over the 37 module-authoring exemplar modules. Answers: does the B759 "duplicate bare
> lexicon key" silent-override hazard actually occur in Tridium's OWN modules? **Yes — in 10 of 37.**
>
> Subject version: N4.14.0.162 (`organized/` corpus). Sources: the extractor
> `module-navigator/tools/module_nav_lib/palette_lexicon_agents.py` (LOCAL tool — `module-navigator/` is
> gitignored, not in the repo); the census read-only over `organized/<m>/<artifact>/extracted/<artifact>.lexicon`,
> `module.palette` (in each JAR), and `extracted/META-INF/module.xml`. Driver-verified: the census dup counts
> were re-grepped against the real lexicon files (see §792.4).
> Method: new CLI command + census run + inline driver re-grep verification.
> Markers: `[CERT]` local primary source (`file:line`/verbatim count) · `[INFER]` deduction.
>
> **Type:** `capture`. Layer: research tooling + module authoring (D8 palette/lexicon/agent census).
> Connects [Block 759] (lexicon bare-key hazard — this confirms it empirically), [Block 788] (operator-module
> lexicon census), [Block 713] (lexicon mechanism), [Block 772] (D6 authoring).

---

## 792.1 — The `palette-lexicon-agents` extractor `[CERT]`

New module-navigator command. Invocation:

```
python3 module-navigator/tools/module_nav.py --base-dir <organized-root> palette-lexicon-agents <module> [--json]
```

Per module it reports three authoring surfaces:

| Surface | Source read | Output |
|---|---|---|
| **Palette** | `module.palette` (XML in each artifact JAR) | `<p n= t= m=>` entry census (name / type / module-alias) + count |
| **Lexicon** | `organized/<m>/<artifact>/extracted/<artifact>.lexicon` | all `key=value` keys + a **duplicate-key report** (keys defined more than once — B759 silent-override hazard) |
| **Agents** | `extracted/META-INF/module.xml` | each `<agent>` registration (type / on) |

Implementation: `module-navigator/tools/module_nav_lib/palette_lexicon_agents.py` with pure helpers
`parse_palette`, `find_duplicate_keys`, `parse_agents` (so the parse logic is unit-testable off fixtures).
One biting test: `module-navigator/tests/test_palette_lexicon_agents.py` — a fixture module dir with a
duplicated bare key + one agent; it FAILS if the duplicate-detection is removed (asserts the key AND its
count of 2). `[CERT]` test passes (`python3 -m unittest tests.test_palette_lexicon_agents` → OK).

**Note:** `module-navigator/` is in `.gitignore`, so this tool is LOCAL-only — it is not committed to the
niagara-research repo. It is nonetheless the authoritative extractor on disk.

## 792.2 — Census scope `[CERT]`

Run over the **37 module-authoring exemplar modules** named in
`RESEARCH-STATE-module-authoring-exemplars.md` (Angle section): alarm, analytics, baja, bajaui, control,
driver, history, batchJob, provisioningNiagara, template, hierarchy, tagdictionary, query, queryTable, search,
rdb, rdbMySQL, rdbOracle, rdbSqlServer, rdbHsqlDb, report, saml, gauth, nss, electronicSignature, fox, net,
silk, tunnel, systemMonitor, systemDb, systemIndex, niagaraSystemIndex, kitControl, schedule, program,
analytics-lib. (No exact "38-module" list existed; the exemplar set is 37 and all exist in `organized/`.)

## 792.3 — THE FINDING: 10 modules carry duplicate bare lexicon keys `[CERT]`

The B759 hazard is REAL in Tridium's own modules — a lexicon key defined twice means the first value is
silently overridden. 10 of 37 modules exhibit it:

| Module · artifact | Duplicate keys (× count) |
|---|---|
| `schedule` · schedule-rt | **`summary` ×3** (two silent overrides — worst in corpus), `type` ×2, `dateSchedule` ×2, `dateRangeSchedule` ×2, `customSchedule` ×2, `scheduleReference` ×2, `weekAndDaySchedule` ×2 |
| `driver` · driver-rt | 20 keys ×2 — `commands.{addPoint,removePoint,editPoint,deleteAllPoint,resortPoint}.{displayName,label,icon,description}` |
| `electronicSignature` · -rt/-ux/-wb | -rt: `commands.addcustomer.description` ×2, `reasonSetName` ×2, `reasonForChange` ×2; -ux: `commands.addcustomer.description` ×2, `mgr.emptyName` ×2, `CustomerManager.displayName` ×2; -wb: `commands.addcustomer.description` ×2 |
| `provisioningNiagara` · -wb | `TakeTimeFactory.displayName` ×2, `.displayIcon` ×2, `TakeTimeStep.display` ×2, `StationSoftwareView.status.needCommissioningMissing` ×2, `.command.uninstallSelected.label` ×2 |
| `bajaui` · bajaui-wb | `commands.makeStationTemplate.{label,icon,description,name,type}` ×2 |
| `alarm` · alarm-rt | `command.refresh.icon` ×2, `command.filter.label` ×2, `command.filter.icon` ×2 |
| `queryTable` · queryTable-wb | `saveColumnsForAllUsersQuestion` ×2, `displayName` ×2 |
| `saml` · saml-rt | `idpCert` ×2, `samlPrototypes` ×2 |
| `tagdictionary` · tagdictionary-rt | `selectTagGroupDialog.info3` ×2 |

The other 27 modules had zero duplicate keys.

## 792.4 — Driver verification of the dup census `[CERT]`

Re-grepped against the real lexicon files (not the tool's output):

| Claim | Check | Result |
|---|---|---|
| `schedule-rt` `summary` ×3 | `grep -c '^summary=' organized/schedule/schedule-rt/extracted/schedule-rt.lexicon` | **3** ✓ |
| `driver-rt` resort/remove/editPoint ×2 | `grep -oE ... \| uniq -c` | all **2** ✓ |
| `alarm-rt` `command.filter.icon` ×2 | `grep -c '^command\.filter\.icon='` | **2** ✓ |
| `electronicSignature-rt` `commands.addcustomer.description` ×2 | `grep -c ...` | **2** ✓ |

## 792.5 — Palette / agent census (summary) `[CERT]`

Largest palettes: `analytics-lib` 11067 entries, `bajaui` 540, `report` 182, `kitControl` 163, `analytics`
153, `program` 140, `tagdictionary` 123. Largest agent counts (from `module.xml`): `provisioningNiagara` 103,
`history` 62, `electronicSignature` 50, `kitControl` 52, `bajaui` 45, `analytics` 43. Modules with 0 agents:
query, rdbHsqlDb, net, silk, systemMonitor, systemIndex. Full per-module numbers in the census run output.

## 792.6 — Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | New `palette-lexicon-agents` command exists with 3 pure parse helpers + 1 biting test | `[CERT]` | `module_nav_lib/palette_lexicon_agents.py`, `tests/test_palette_lexicon_agents.py` | Y — test run OK |
| 2 | `module-navigator/` is gitignored (tool local-only) | `[CERT]` | `git check-ignore module-navigator` | Y |
| 3 | 10 of 37 exemplar modules have duplicate bare lexicon keys | `[CERT]` | census + §792.4 re-greps | Y — 4 spot-checks re-grepped |
| 4 | `schedule-rt` `summary` defined 3× (worst case) | `[CERT]` | `grep -c '^summary=' schedule-rt.lexicon` = 3 | Y |
| 5 | B759 hazard is empirically real, not theoretical | `[INFER]` | from claims 3-4 | deduction |

**Tally:** `[CERT]` ×4 · `[INFER]` ×1. Capture block — ratio not an exhaustion signal (§11).

## 792.7 — Kit implication → research tooling / `build-n4-module-kit`

1. **Authoring guard (biting check)**: a build-n4-module lint/check should FAIL on DUPLICATE keys in a
   module's `.lexicon` (the B759 hazard). [Block 788] proposed exactly this "lexicon dup-bare-keys → FAIL"
   regression guard after auditing the operator's OWN 3 modules (ColdRoomPan/CompPan/DashboardPan) and
   finding ZERO duplicates — i.e. the check is clean on our code. This census supplies the cross-corpus
   evidence that it BITES on real defects: 10 of 37 Tridium modules trip it (worst `schedule-rt` `summary`
   ×3). So the guard catches live defects, not a theoretical hazard. Destination: a toolbelt check or
   `types/logic.md` lexicon note.
2. **tool-registry.md**: the research-sdd kit's `toolbelt/tool-registry.md` does NOT currently list any
   module_nav commands, so the new `palette-lexicon-agents` command was NOT registered there — left as a
   retro note for explorador's team (research-sdd kit lane) to decide whether to start listing module_nav
   commands in the registry.
3. **Durability port**: the extractor is also available as a TRACKED, stdlib-only standalone at
   `tools/palette-lexicon-agents.py` (with `tools/tests/test_palette_lexicon_agents.py` + `tools/README.md`
   entry) so it survives beyond this machine, unlike the gitignored `module-navigator` copy. `--all` runs the
   corpus-wide census (57 / 662 modules carry duplicate bare keys corpus-wide; 10 / 37 within the exemplar set).

## 792.8 — Connections
- [Block 759] lexicon bare-key hazard (this block confirms it empirically across 10 modules).
- [Block 788] operator-module lexicon census (ColdRoomPan 32 keys / CompPan 56 keys) — same extractor family.
- [Block 713] lexicon mechanism; [Block 772] D6 authoring.
- No open gaps (capture block).
