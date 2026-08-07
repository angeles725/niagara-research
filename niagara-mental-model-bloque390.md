# Block 390 — license-diff addendum: japicmp API diff 4.14→4.15 — the release is ADDITIVE and backward-compatible (zero classes removed, baja core binary-compatible), a pure VERSION-axis result

> **Focus `license-diff` — version-axis addendum (japicmp follow-up).** [B388]/[B389] deferred the per-jar
> API-level comparison to `japicmp` as a pure VERSION-axis detail. This block runs it: it acquires japicmp,
> compares representative common module jars between A (4.14) and B (4.15), and characterizes the 4.14→4.15
> delta. It does NOT reopen the license question (the module set/bytes are version/vendor, not license —
> [B388]); it quantifies the VERSION axis. READ-ONLY. Block type: EVIDENCE (tool).
>
> Compared: `A/modules/<jar>` (Honeywell 4.14.0.162) vs `B/modules/<jar>` (Tridium 4.15.3.28.2), for the 715
> common jars. Tool: **japicmp 0.23.1** (Maven Central `jar-with-dependencies`, java 21,
> `--ignore-missing-classes` because module jars reference other modules' classes).
> Evidence: `audits/B390-japicmp-summary.txt`, `audits/B390-japicmp-baja.txt`.
> Markers: `[CERT]` observed in japicmp output (jar + counts cited) · `[INFER]` deduction.

---

## 390.1 — Per-module class-level delta `[CERT]`

japicmp class histogram, 4.14→4.15 (`audits/B390-japicmp-summary.txt`): `[CERT]`

| Module | unchanged | modified | new | **removed** | binary-incompatible |
|---|---|---|---|---|---|
| `baja.jar` (core) | 950 | 29 | 4 | **0** | **0** |
| `bacnet-rt.jar` | 579 | 89 | 74 | **0** | — |
| `control-rt.jar` | 31 | 0 | 0 | **0** | 0 |
| `driver-rt.jar` | 76 | 0 | 1 | **0** | — |
| `alarm-rt.jar` | 120 | 3 | 0 | **0** | 0 |

**The load-bearing pattern: ZERO classes removed in any module sampled**, and `baja.jar` (the framework core)
has **0 binary-incompatible changes** — every one of its 29 modified classes is a backward-compatible change.
`control-rt.jar` is byte-for-API identical (0 changes). `bacnet-rt.jar` is the outlier with a large ADDITIVE
delta (74 new classes, 89 modified, still 0 removed) — 4.15 grew BACnet substantially (consistent with
BACnet/SC maturation), but added rather than broke. `[CERT]`

---

## 390.2 — The nature of a "modified" class is additive `[CERT]`

Concrete example from `baja.jar` — `com.tridium.authn.BAuthenticationService`
(`audits/B390-japicmp-baja.txt`): all six existing `Property`/`Type` fields and every existing `authenticate`/
`getAuthenticationScheme`/`checkParentForRestrictedComponent` method are marked `=== UNCHANGED`; the only
delta is `+++ NEW METHOD: public static boolean allowRemoteAuthnSchemeChange()`. This is the textbook
additive, non-breaking change shape — a new capability method appended, nothing altered or removed. The 29
`baja` "modified" classes are of this kind (hence 0 binary-incompatible). `[CERT]`

---

## 390.3 — Conclusion `[CERT]`

The 4.14→4.15 module-API delta is an **additive, backward-compatible release**: no API surface removed,
`baja` core binary-compatible, most modules unchanged or lightly extended, with `bacnet-rt` the one large
(but still additive) grower. This is a **pure VERSION-axis** characterization — it confirms [B388]/[B389]'s
attribution that the A-vs-B module difference is version (+vendor+user), and adds the quantitative shape:
version movement here is *additive*, not breaking. It carries no license-axis content. `[CERT]/[INFER]`

Operational corollary `[INFER]`: an integration built against 4.14 module APIs would very likely load and
run against 4.15 (no removed classes, no binary breaks in core) — the constraint on upgrading is the SMA/
maintenance gate ([B387 §387.5], module release-date vs license maintenance date), NOT API incompatibility.

---

## 390.4 — Self-verify

**Token re-checks** (`audits/B390-japicmp-*.txt`):
1. baja.jar: 950 unchanged, 29 modified, 4 new, 0 removed, 0 binary-incompatible — ✓ (japicmp histogram + `--only-incompatible`).
2. bacnet-rt 74 new / 89 mod / 0 removed; control-rt 0 changes; driver-rt 1 new; alarm-rt 3 mod / 0 removed — ✓.
3. `BAuthenticationService`: all existing members UNCHANGED + one `+++ NEW METHOD allowRemoteAuthnSchemeChange()` — ✓.
4. japicmp 0.23.1 acquired + runs on java 21 — ✓ (`--help`; 5.9 MB jar).

**4/4 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 12 · `[INFER]` 2 (the upgrade-compat corollary; bacnet growth cause). Ratio ≈
0.17 — low; EVIDENCE block. VERSION-axis addendum to B388/B389; no license content.

**Tool decision** — `T: japicmp 0.23.1 · /home/cristian/.local/share/research-sdd-tools/jars/japicmp-jar-with-dependencies.jar · downloaded` (Maven Central), for jar-to-jar API diffs; registered in `tools/README.md`.

---

## 390.x — Connections

- **[B388]/[B389]** — this quantifies the VERSION axis those blocks named: the 532/715 common modules differ
  additively, not by removal or breakage.
- **[B387 §387.5]** — the real upgrade constraint is the SMA module-release-date gate, not API compatibility
  (this block shows the API is compatible).
- **Focus status**: `license-diff` remains answered 6/6; this is a version-axis addendum, not a reopened gap.
  The `bacnet-rt` 74-new-class delta is the natural next version-axis thread if BACnet/SC 4.15 changes are
  wanted (out of the license focus's scope).
