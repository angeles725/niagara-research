# Block 289 — The live station's file space: `^` resolves to `shared/`, and why a `.px` cannot live in `Drivers/`

> **DOCUMENT-MODE block (METHODOLOGY §20)** — CAPTURES what a live session against a running station
> produced, rather than DISCOVERING a gap from the backlog. Genre: part subject-mechanics, part procedure.
>
> **Corrects [Block 187]** on a load-bearing point: `^` in a `file:` ord is an ABSOLUTE anchor, not a
> relative prefix — and it anchors to `<station>/shared/`, not to the station folder. B187 has been edited
> in place with a CORRECTION note pointing here.
>
> **Sources**: (a) original Tridium source `javax.baja.file.BFileSystem` (docSource, non-decompiled);
> (b) the LIVE station `PRUEBAS` — OptimizerSupervisor N4.14.0.162, Honeywell, `https://localhost` — its
> on-disk station home and its own shipped `.px` files. Probe preserved at
> `sources/probes/live-20260727T012800Z-station-pruebas-filespace-and-obix.txt`.
>
> Markers: `[CERT]` verbatim in local code · `[CERT-live]` empirical against the running station ·
> `[INFER]` derived. **Corpus language: ENGLISH.**
>
> **SECRETS DISCIPLINE (live-install)**: this block cites structure only. No credential values, no
> keystore material. The service account used for the HTTP reads is referred to structurally.
>
> PX layer (naming + deployment). Connects [Block 187] (ords), [Block 185] (PopupBinding),
> [Block 181] (`.px` grammar), [Block 189] (the applied menu).

---

## 289.1 — Two spaces that look adjacent in the Nav tree and are not interchangeable `[CERT-live]`

A Niagara station exposes two disjoint namespaces. Confusing them is the single most expensive mistake
available here, because the Nav tree renders them as sibling branches:

| | Component space | File space |
|---|---|---|
| Ord | `station:\|slot:/Drivers/PRUEBAS` | `file:^px/menu.px` |
| Holds | components: networks, devices, points, `baja:Folder` | files: `.px`, images, backups |
| Nav branch | `Config` | `Files` |
| Persisted in | `config.bog` | the station home directory tree |

A `.px` is a FILE. It cannot be placed inside a `baja:Folder` of the component space — the operation does
not exist. `[CERT-live]` Verified against the running station: `Drivers/PRUEBAS` is a `baja:Folder` and
obix returns it with zero children:

```xml
<obj href=".../obix/config/Drivers/PRUEBAS/" is="/obix/def/baja:Folder" display="Folder">
</obj>
```

The correct construction is the inverse of what the adjacency suggests: the `.px` lives in the file space,
and its BINDINGS reach into the component space —
`<ValueBinding hyperlink="station:|slot:/Drivers/PRUEBAS|view:workbench:PropertySheet"/>`. `[INFER]`

## 289.2 — `^` is an ABSOLUTE anchor, not a relative prefix `[CERT]`

[Block 187] §187.3/§187.4/§187.5 described `file:^px/menu.px` as "relative to the current dir" / "the dir of
the current `.px`". That is false. `^` is a special ROOT registered when the `BFileSystem` is constructed:

```java
File stationHomeFile = Sys.getStationHome();
if (stationHomeFile != null)
{
  BLocalFileStore stationHomeStore = new BLocalFileStore(this, new FilePath("^"), stationHomeFile);
  this.stationHome = new BDirectory(stationHomeStore, LexiconText.make("baja", "nav.stationHome"));
  stationHome.icon = BIcon.std("database.png");
  roots.add(stationHome);
  specials.put("^", stationHome);
```
`sources/.../docSource/docSource-doc/extracted/baja/javax/baja/file/BFileSystem.java:144-151` `[CERT]`

The same constructor registers `~` for the user home
(`baseOrdStationToUserHome = "local:|file:~" + homeDiff`, `:157`). `[CERT]` So `^` and `~` are siblings in
kind: file-system ROOTS placed in a `specials` map, resolved by name — there is no base-relative
normalization involved.

**Practical consequence**: a `PopupBinding ord="file:^px/menu.px"` resolves to the same file regardless of
where the hosting graphic lives. The host and the menu do NOT need to be co-located. `[INFER]` Under B187's
old wording one would have moved files around to "bring them closer" for no reason.

## 289.3 — `^` maps to `<station>/shared/`, NOT to the station folder `[CERT-live]`

§289.2 settles that `^` is an anchor; it does not settle WHICH directory it anchors to. The code says
`Sys.getStationHome()`. Empirically, the directory the file space exposes as its root is the station's
**`shared/`** subfolder. Two independent observations converge:

**(a) What the Nav tree shows.** Under `Files`, Workbench lists exactly the contents of `<station>/shared/`:

```
domo   Imagenes   images   irmRepository   px   reflow   sdash   wifiCertificatesLog
```

and does NOT list `config.bog`, `console.txt`, `alarm/` or `history/` — all of which exist in `<station>/`
but not in `<station>/shared/`. `[CERT-live]`

**(b) What the station's own `.px` files reference.** The shipped graphics use `^`-anchored ords whose
targets are resolvable only under `shared/`:

| Ord in a real `.px` | File on disk |
|---|---|
| `ord="file:^px/Header.px"` (`Floorplan.px:17`) | `<station>/shared/px/Header.px` |
| `image="file:^images/Floorplan3D.png"` (`Floorplan.px:19`) | `<station>/shared/images/Floorplan3D.png` |
| `ord="file:^px/Dashboard/Home.px"` (`Bien.px`) | `<station>/shared/px/Dashboard/Home.px` |

Census of the anchor across the station's graphics — every single one lands under `shared/`: `[CERT-live]`

```
  13 file:^Imagenes/Iconos/icono      12 file:^px/Plantas/Planta
   9 file:^px/kitPxN                   5 file:^Imagenes/maquila
   5 file:^Imagenes/honeywell          4 file:^px/hesPxN
```

**Therefore**: `file:^px/menu.px` → `<station>/shared/px/menu.px`. A `.px` written to `<station>/px/` is
invisible to Workbench and unresolvable by the ord. `[CERT-live]` (This block exists partly because that
exact mistake was made and cost a round trip.)

Note the residual tension with §289.2: the code names `Sys.getStationHome()` while the observed root is
`shared/`. Whether `getStationHome()` itself returns the `shared` subdirectory for a station VM, or the
file space applies a further restriction, was NOT determined — see B289-G2.

## 289.4 — Identifying the RUNNING station among several homes `[CERT-live]`

A single machine carries multiple Niagara homes and multiple stations with the same name; writing into the
wrong one produces a silent no-op. Observed on this host: `PRUEBAS` exists under
`C:\ProgramData\Niagara4.14\...`, `C:\Users\equipo\Niagara4.14\...`, and `C:\ProgramData\Niagara4.15\...`,
plus a dozen dated variants (`PRUEBAS_01_02_2026`, `PRUEBAS_reflow`, …).

The discriminator is `config.bog.lock`: `[CERT-live]`

| Station home | `config.bog` | `config.bog.lock` | Verdict |
|---|---|---|---|
| `C:\ProgramData\Niagara4.14\OptimizerSupervisor\stations\PRUEBAS` | 2026-07-25 13:20 | **present, 2026-07-26 18:51** | RUNNING |
| `C:\Users\equipo\Niagara4.14\OptimizerSupervisor\stations\PRUEBAS` | 2026-07-02 13:00 | absent | not running |

A fresh `.lock` is the live one; corroborate with the station name returned by the running instance
(`<str name="stationName" val="PRUEBAS"/>` over obix, §290). `[INFER]`

## 289.5 — `PopupBinding` requires `kitPx` in the `<import>` `[CERT]`

[Block 181] §181.5 rule 3 requires `<import>` to declare every module used, and [Block 185] establishes
`PopupBinding` as a **kitPx** widget. A host graphic carrying the trigger therefore needs:

```xml
<import>
  <module name="baja"/>
  <module name="bajaui"/>
  <module name="gx"/>
  <module name="kitPx"/>
</import>
```

Omitting `kitPx` leaves `<PopupBinding>` unresolvable. The station's own `Floorplan.px:4-11` declares
`baja`, `bajaui`, `converters`, `gx`, `kitPx`, `vykonPro` — `kitPx` is present precisely because the file
uses `PxInclude`. `[CERT-live]` The menu file itself (`Label` + `GridPane` + `ValueBinding`) does NOT need
`kitPx`; only the host with the `PopupBinding` does. `[INFER]`

## 289.6 — A valid `.px` and an invalid one in the same folder `[CERT-live]`

`<station>/shared/px/` contains both real project graphics and at least one file that cannot parse. The
contrast is instructive because the invalid one *looks* plausible.

`Floorplan.px` — real, Niagara grammar: `[CERT-live]`

```xml
<px version="1.0" media="workbench:WbPxMedia">
  <CanvasPane name="content" viewSize="1000.0,800.0" scale="fitRatio" minScaleFactor="0.5" ... >
    <PxInclude layout="0.0,0.0,1000.0,130.0" ord="file:^px/Header.px"/>
    <Picture layout="80.0,210.0,800.0,499.0" image="file:^images/Floorplan3D.png" scale="fitRatio"/>
```

`Graphic.px` — same folder, **JavaFX** grammar: `[CERT-live]`

| `Graphic.px` writes | Niagara actually uses |
|---|---|
| `<BorderPane.north>` nested property element | `BorderPane` children by layout slot |
| `<Widget x="0" y="0" width="1920" height="80">` | `layout="x,y,w,h"` (B183 §183.5) |
| `<Ellipse centerX radiusX radiusY>` | `<Ellipse layout="...">` |
| `<Rectangle cornerRadius="10">` | no `cornerRadius` in the PX `Rectangle` |
| `font="bold 24pt sans-serif"` | `font="bold 24.0pt Arial"` (B183 §183.3) |
| `fill="linear(0%,0%,0%,100%,navy,dodgerblue)"` | `Brush`/gradient grammar of `gx` |

It additionally splits opening tags across lines, which violates the `XParser` rule of [Block 181] §181.5 on
its own. The widget census over the station's genuine graphics shows what the real vocabulary looks like —
`Label` 29, `Brush` 22, `ObjectToString` 14, `BoundLabelBinding` 14, `BoundLabel` 14, `ActiveStateSimple` 11,
`MouseOverBinding` 10, `Polygon` 9 — none of which appear in `Graphic.px`. `[CERT-live]`

`Graphic.px` carries authoring artifacts (`<!-- AQUÍ PUEDES AGREGAR TU CONTENIDO -->`, `© 2025 Tu Empresa`)
consistent with LLM-generated JavaFX-flavoured markup. `[INFER]` It is a useful negative fixture: a `.px`
can be well-formed XML and still be un-openable, so XML validity is a necessary and NOT a sufficient check.

## 289.7 — Self-verify

| Claim | Evidence | Marker |
|---|---|---|
| `^` registered as a special file-system root | `specials.put("^", stationHome)` `BFileSystem.java:151` | `[CERT]` |
| `^` built from `Sys.getStationHome()` | `BFileSystem.java:144-147` | `[CERT]` |
| `~` is its user-home sibling | `baseOrdStationToUserHome` `:157` | `[CERT]` |
| B187 called `^` "relative to current dir" — false | B187 §187.3/§187.4 vs the code above | `[CERT]` |
| Nav `Files` shows `<station>/shared/` contents | tree vs directory listing, probe §E | `[CERT-live]` |
| `Files` omits `config.bog`/`console.txt`/`alarm/` | same comparison | `[CERT-live]` |
| Real ords resolve under `shared/` | `Floorplan.px:17,19`, `Bien.px`; census of 4 anchor prefixes | `[CERT-live]` |
| ⇒ `file:^px/x.px` = `<station>/shared/px/x.px` | composed from the two above | `[CERT-live]` |
| Which dir `getStationHome()` itself returns | NOT determined — B289-G2 | (open) |
| `Drivers/PRUEBAS` is an empty `baja:Folder` | obix body, probe §D | `[CERT-live]` |
| A `.px` cannot live in the component space | derived from the two-space split | `[INFER]` |
| Running station identified by fresh `config.bog.lock` | two homes compared, probe §E | `[CERT-live]` |
| `PopupBinding` is a kitPx widget needing the import | B185 + `Floorplan.px:4-11` | `[CERT]` |
| `Graphic.px` uses JavaFX grammar, not PX | six-row attribute comparison, probe §F | `[CERT-live]` |
| `Graphic.px` also splits tags across lines | file body vs B181 §181.5 | `[CERT-live]` |
| `Graphic.px` is LLM-generated | authoring comments only — suggestive, not proof | `[INFER]` |
| XML-valid ≠ openable | `Graphic.px` parses as XML yet uses non-PX widgets | `[INFER]` |

Tally: **[CERT] 5 / [CERT-live] 9 / [INFER] 4** — 1 claim explicitly left open (B289-G2).

---

## 289.x — Connections and open gaps

- **[Block 187]** — **CORRECTED by this block** (§289.2/§289.3). B187 now carries an inline CORRECTION note
  in §187.3/§187.4/§187.5 pointing here. This is the METHODOLOGY §14 consistency rule applied: a
  contradicted value left alive poisons the next pass.
- **[Block 185]** — `PopupBinding` mechanics; §289.5 adds the import requirement its examples assume.
- **[Block 181]** — the `XParser` one-line-tag rule; §289.6 is a field sighting of a file that breaks it.
- **[Block 189]** — the applied `menu.px`. Its deployment path is settled by §289.3: `<station>/shared/px/`.
- **[Block 182/183]** — `layout` and the value grammars that §289.6 contrasts against JavaFX.

### Open gaps

| ID | Gap | Class |
|---|---|---|
| ~~**B289-G1**~~ | **CLOSED (2026-07-26).** `local:\|foxs:\|station:\|file:^px/x.px` raised `UnknownSchemeException` (`RegistryDatabase.getOrdScheme:272`) because of the `station:` segment: the working ord the site uses is **`local:\|foxs:\|file:^px/Dashboard/Home.px`** — `file:` chains straight after `foxs:`, never after `station:`. `[CERT-live]` This follows from §289.1: `station:` opens the COMPONENT space and `file:` is its own scheme over the FILE space; chaining them mixes two disjoint namespaces. | closed |
| **B289-G2** | Does `Sys.getStationHome()` itself return `<station>/shared/`, or does the file space apply a further restriction on top of it? §289.3 settles the OBSERVED mapping empirically but not its mechanism. | STATIC |
