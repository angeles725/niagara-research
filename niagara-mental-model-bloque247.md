# Bloque 247 — U6 legacy AX / ASCOT residue: `honeywellAXPlatinum(+HR)` (AX-era Px UI widgets) + `honeywellASC` (minimal ASCOT VAV)

> Empirical coverage of gap U6 (coverage-audit `audits/2026-07-12-coverage-audit.md`): three legacy Honeywell
> OEM modules. All TRIVIAL by measured pre-flight (§13 e2): `honeywellAXPlatinum` = 3 classes,
> `honeywellAXPlatinumHR` = 1, `honeywellASC` = 1. Non-standard layout (single-profile, no `-rt/-ux/-wb`
> split — a Niagara-AX-era packaging, pre-N4). Read inline; a proven-thinness coverage closure.
>
> **Focus**: `oem-honeywell-tail`, gap U6 (MED). Sixth block of the focus.
>
> **Sources**: `organized/{honeywellAXPlatinum,honeywellAXPlatinumHR,honeywellASC}/**/vineflower/**`.
>
> **Method**: read inline. `[CERT]` = observed by me at the cited `file:line`; `[INFER]` = deduction.
>
> Capa 22 (OEM). **Conecta fuerte**: [Bloque 107] (ASCOT `ascCommon`/`ascBacnet`/`ascLon` — which explicitly
> did NOT cover `honeywellASC`, this closes that note), [Bloque 10]/[Bloque 24] + PX blocks (Px widgets /
> palettes).

---

## 247.1 — `honeywellAXPlatinum` + `HR`: legacy AX Px UI widgets `[CERT]`

"AX Platinum" = a Honeywell UI product from the **Niagara AX** era (pre-N4); these modules survived into the
N4 organized/ tree as legacy residue. Package `com.hon.ui`.

- **`BHonAnimator extends BWidget`** (`com/hon/ui/BHonAnimator.java:12`) — a Px **multi-frame image animator
  widget**: an `active` boolean + `imageInactive` + `image0`…`image8` (`:13-23`) — 9 animation frames cycled on
  a Px graphic. A stock building-automation UI element (e.g. a spinning fan / animated equipment icon).
- **`BHonPalette extends BComponent`** (`com/hon/ui/BHonPalette.java:11`) — a Workbench **palette root**:
  `getNavChildren()` returns `BINavNode[]` (`:18`), the standard nav-tree hook that makes the module's widgets
  appear as a draggable palette in the editor.
- **`honeywellAXPlatinumHR`** = one class, `BHonHrPalette extends BComponent` (same `getNavChildren()` palette
  shape) — the **HR (high-resolution) variant** of the palette. `[INFER]` A parallel widget set sized for
  higher-DPI displays.

`[INFER]` These are cosmetic Px UI assets (animator + palette organizers), not runtime/control logic — legacy
AX graphics kept for backward compatibility. No services, no protocol, no license gate.

---

## 247.2 — `honeywellASC`: minimal ASCOT VAV class `[CERT]`

- **`AscVav`** (`com/honeywell/asc/AscVav.java:3`) — a plain Java class (NOT a `BComponent`/Niagara type), with
  no observable public API surface beyond the class declaration (minimal/stub). `[INFER]` A small ASCOT (a
  Honeywell/Trend controller family) VAV helper or data holder.
- **Closes the [Bloque 107] note**: B107 covered the ASCOT stack `ascCommon`/`ascBacnet`/`ascLon` but explicitly
  flagged `honeywellASC` as NOT covered. This block confirms `honeywellASC` is a near-empty residual module — a
  single minimal `AscVav` class — so the B107 gap resolves as **proven-thinness**, not a missed subsystem.

---

## 247.3 — Conexiones

- **[Bloque 107]** (ASCOT `ascCommon`/`ascBacnet`/`ascLon`): `honeywellASC` is the module B107 named-but-did-not-
  cover; resolved here as a 1-class stub (`AscVav`), not a parallel ASCOT subsystem.
- **PX blocks / [Bloque 10] (UI stack)**: `BHonAnimator`/`BHonPalette` are AX-era Px widgets + palette roots —
  the legacy cousins of the N4 PX widget ecosystem (px-menu/px-editor focuses).

---

## 247.4 — Self-verify

- **Claims observed by me** (`[CERT]`): `BHonAnimator extends BWidget` + its 9 image-frame slots
  (`BHonAnimator.java:12-23`), `BHonPalette`/`BHonHrPalette extends BComponent` + `getNavChildren()`
  (`BHonPalette.java:11-18`), `AscVav` minimal class (`AscVav.java:3`). `[INFER]` = the legacy-AX-UI
  characterization + the ASCOT-stub reading.
- **Block TYPE**: EVIDENCE (trivial modules). U6 covered as legacy-residue / proven-thinness — no runtime
  subsystem hidden here.
- **New gaps queued**: none. Next per RESEARCH-STATE-oem-honeywell-tail: U7 Forge onboarding
  (`fcEasyOnboard` + model-sync variants), or U1b/U1c.
