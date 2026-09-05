# B784 · Real module.xml profile matrix + dependency version conventions (MAE13, D1)

> **Scope**: the CONCRETE `module.xml` facts a template must get right — the per-module PROFILE set, the `<dependency>`
> version format, and the header attribute roster — extracted verbatim from real Tridium manifests. Refines the
> generic module.xml MECHANISM (REMITTANCE: B12/B629-B636 anatomy, B754 versioning/survival matrix) with the actual
> values. Focus: `module-authoring-exemplars` (MAE13 / dimension D1). Kit destination: `METHODOLOGY.md` /
> `corpus-index.md`.
>
> **Sources**: FUENTE 3 packaged manifests — `fox`, `tunnel`, `alarm`, `bajaui`, `docMicros` `extracted/META-INF/
> module.xml`; verified this session at `organized/`. READ-ONLY. English (post-B115).

---

## 784.1 — Profile matrix: a module splits into per-profile parts `[CERT]`
A module = one `moduleName` split into per-profile parts; the `-rt` part enumerates its siblings in `<moduleParts>`.
Real parts on disk:

| Module | Parts present | `<moduleParts>` in -rt | -se | -doc |
|---|---|---|---|---|
| fox | fox-rt, fox-ux | `<modulePart name="fox-ux" runtimeProfile="ux"/>` | — | — |
| tunnel | tunnel-rt only | (single-part) | — | — |
| alarm | alarm-rt, alarm-ux, alarm-wb, **alarm-se** | ux/wb/se (`alarm-rt/…module.xml:139-141+`) | **yes** | — |
| bajaui | bajaui-ux, bajaui-wb | (no -rt part on disk) | — | — |

- **`-se` = the SERVER profile** — `alarm-se` header `runtimeProfile="se"` (`alarm-se/…module.xml:2`), same
  `moduleName="alarm"`, holding server-only classes (printer recipients + their FEs) and the widest dep set of any
  alarm part. Also seen in `obix-se`, `test-se`, `niagaraTest-se`.
- **`-doc` is NOT a part of a code module — it is a SEPARATE module** — docs ship as their own `runtimeProfile="doc"`
  module with an empty `<types/>` and a single `baja` dep: `docMicros-doc` header `runtimeProfile="doc"`
  (`docMicros-doc/…module.xml:2`), `<types/>` (:8). So a code module never carries a `-doc` part; a doc module is a
  standalone `doc<Name>/doc<Name>-doc`.

## 784.2 — `<dependency>`: a 3-part Tridium FLOOR, distinct from the 4-part build stamp `[CERT]`
Format: `<dependency name="<module>-<profile>" vendor="Tridium" vendorVersion="X.Y.Z"/>`. Vendor is ALWAYS `Tridium`.
The decisive convention: a dependency's `vendorVersion` is a **3-part floor** (`4.14.0`), while the module's OWN
`vendorVersion` is the **4-part build stamp** (`4.14.0.162`). Real examples:
- `<dependency name="fox-rt" vendor="Tridium" vendorVersion="4.14.0"/>` (`alarm-rt/…module.xml:9`).
- alarm-rt/-se/-ux self-stamp `vendorVersion="4.14.0.162"` (`alarm-rt/…module.xml:2`) but cross-reference each other
  at the 3-part `4.14.0` — the dep drops the 4th build segment [INFER: 3-part = minimum, per B754].
- Legacy/looser floor is valid: `docMicros-doc` → `<dependency name="baja" … vendorVersion="4.0"/>` (:4). The floor is
  author-chosen; NO dep in these files uses the `.162` build segment.
- Native install-time dep (fox-rt only): `<nre name="nre-core-*" version="4.14.0.162" … solvers="commissioning"/>`
  inside `<installation><dependencies>` — this form DOES use the 4-part version and a `name` glob.

This is the concrete rule behind B754's build-target advice: build against the LOWEST target's `niagara_home`, and
declare deps at the 3-part floor of that target — not the exact build.

## 784.3 — The `<module>` header attribute roster `[CERT]`
Verbatim (`alarm-rt/…module.xml:2`): `<module name="alarm-rt" bajaVersion="0" vendor="Tridium"
vendorVersion="4.14.0.162" description="…" preferredSymbol="a" nre="true" autoload="true" installable="true"
buildMillis="…" buildHost="…" moduleName="alarm" runtimeProfile="rt" releaseDate="…">`. Author-filled:
`vendor`, `vendorVersion` (own 4-part stamp), `description`, `preferredSymbol` (short handle: fox=`f`, alarm=`a`,
bajaui=`ui`, tunnel=`t`), `moduleName`, `runtimeProfile`; `name` = `<moduleName>-<profile>`. Constant: `bajaVersion="0"`
on ALL modules. Toolchain-filled: `buildMillis`/`buildHost`/`releaseDate` [INFER].

## Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Module splits into per-profile parts; -rt lists siblings in `<moduleParts>` (alarm rt/ux/wb/se; tunnel rt-only; fox rt/ux; bajaui ux/wb) | [CERT] | disk listing; alarm-rt/…module.xml:139-141 |
| 2 | `-se` = server profile (runtimeProfile="se", server-only classes); same moduleName | [CERT] | alarm-se/…module.xml:2 |
| 3 | `-doc` is a SEPARATE module (runtimeProfile="doc", empty `<types/>`), never a part of a code module | [CERT] | docMicros-doc/…module.xml:2,8 |
| 4 | Dep `vendorVersion` = 3-part Tridium floor (4.14.0); module's own = 4-part build stamp (4.14.0.162); legacy floor 4.0 valid | [CERT] | alarm-rt/…module.xml:2,9; docMicros-doc:4 |
| 5 | Header roster: author fills vendor/vendorVersion/description/preferredSymbol/moduleName/runtimeProfile; bajaVersion const "0" | [CERT] | alarm-rt/…module.xml:2 |

**Tally**: 5 [CERT], 0 [INFER on claims] (2 INFER notes on 3-part=minimum and toolchain fields). Spine grep-verified
inline this session at `organized/`.

## Connections
- **B12/B629-B636** (module.xml/anatomy mechanism — this block supplies the real values). **B754** (versioning +
  survival matrix — §784.2's 3-part-floor rule is the concrete form of B754's "build against the lowest target").
  **B780** (the palette/lexicon/@AgentOn artifacts these parts package). **B778** (a `-se`/`-rt` part is where a
  service/type is declared).

## Open gaps
- **MAE13-G1** — the `<installation>` block (native `<nre>`/`<jars>`/OS-specific `-rt-<os>` parts) is only sampled
  (fox-rt); a bounded follow-up for a module that ships native code.

## Kit implication (→ `METHODOLOGY.md` / `corpus-index.md`)
Add to the module.xml guidance: (1) split by profile part `-rt`/`-ux`/`-wb`/`-se` (server) with the `-rt` part listing
`<moduleParts>`; docs are a SEPARATE `runtimeProfile="doc"` module, never a `-doc` part of a code module. (2) A
`<dependency>` carries `vendor="Tridium"` and a **3-part floor** `vendorVersion` (`4.14.0`), NOT the module's own
4-part build stamp (`4.14.0.162`) — this is the concrete form of B754's "declare against the lowest target." (3) The
header roster: author fills vendor/vendorVersion/description/preferredSymbol/moduleName/runtimeProfile; `bajaVersion`
is constant `"0"`; build/host/date are toolchain-emitted.
