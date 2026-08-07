# Block 388 — license-diff L3: the module inventory delta is VENDOR + VERSION + user-content — 86 Honeywell OEM modules, 42 4.15-base modules, ~66 user/3rd-party — and NOT ONE module is added or removed by licensing

> **Focus `license-diff` — L3, the module delta.** [B386] (on-disk) and [B387] (runtime) answered the license
> question; this block closes the module-inventory axis and confirms, from the disk side, [B387]'s finding
> that **no module is license-gated by presence** — the module SET differs only by vendor, version, and
> user-installed content. READ-ONLY. Block type: EVIDENCE (inventory). This is explicitly a VENDOR/VERSION
> -axis result, not a license one — stated so the reader does not mis-attribute it.
>
> Compared: A = `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules` (Honeywell OEM 4.14) vs
> B = `…/Tridium_EMEA_N4_Supervisor-4.15.3.28.2/modules` (Tridium base 4.15 installer). Distinct top-level
> module names (profile suffix `-rt/-wb/-ux/-se/-doc` stripped). Evidence: `audits/B388-module-delta.txt`.
> Markers: `[CERT]` observed (counts/names on disk) · `[INFER]` deduction.

---

## 388.1 — The three-way partition `[CERT]`

Distinct modules: **A = 684, B = 574; only-in-A = 152, only-in-B = 42, common = 532** (`audits/B388-module-delta.txt`). `[CERT]`

| Bucket | n | Axis | Examples |
|---|---|---|---|
| only-A, Honeywell OEM | **86** | VENDOR | `honeywellBacnetSpyder`, `honeywellLonSpyder`, `honeywellSpyderTool`, `honAlarmExt`, `honIrmConfig`, `honEagleHawkHMI`, `honPlantController*`, `honMqttDriver`, `honeywellVenom*`, `clHVAC*`/`clCBus`/`clPanelBus` (CentraLine), `galileo*`, `SylkActuatorAnalytics`, `maxproVideo`, `redLink`, `XL10Wizards` |
| only-A, user + 3rd-party + OEM theme/doc | **~66** | USER-CONTENT | `chihuahua`, `angeles`, `casino`, `banbajio`, `carcamos`, `datacenter`, `sanluis`, `sukarne`/`sukarne3d`, `sejofadashboard`, `nmodsreflow`, `electronicSignature`(+Remote), `easyBinding`, `httpClientGAngeles`, `themeHoneywell`/`themeOptimizer`, `docHoneywell*` |
| only-B | **42** | VERSION (4.15 base) | `accessControl`, `accessDriver`, `entsec*`, `cloudLink*`, `ldapDriver`, `nacDriver`, `intrusionSmartKey*`, `videoHx`, `jodaTime`, `timesync`, `niagaraTenantBilling` |
| common | **532** | VERSION (byte-differ) | the Tridium base set present in both, differing 4.14↔4.15 |

`[CERT]`

---

## 388.2 — What each bucket means `[CERT]`

- **86 OEM modules = the VENDOR axis** `[CERT]`: these are the Honeywell/CentraLine/galileo rebrand + the
  Spyder/Sylk/Venom/IRM device drivers and tools — exactly the OEM surface the corpus documented (Spyder
  [B106/B120/B121], honIrmConfig [B242], honAlarmExt [B378], easyBinding [B207], honeywellSpyderTool
  [B259]). They are present because A is a Honeywell OEM distribution, not because it is licensed. Their
  RUNTIME activation is gated by the matching license features (`spyderProgrammable`, `honEasyBinding`, …
  [B387]) — but the JARs are on disk regardless. `[CERT]`
- **~66 user-content + 3rd-party** `[CERT]`: `chihuahua` (the user's own dashboard module, [B163-B177]),
  `angeles`/`httpClientGAngeles`/`casino`/`banbajio`/`sanluis`/`sukarne`/`sejofadashboard` (the user's own
  projects), `nmodsreflow` ([B138-B155]), `electronicSignature` (TridiumPS, [B350-B356]), `easyBinding` — all
  independently installed, NOT part of the base product and NOT license-controlled on disk. This bucket is
  install-local NOISE for the license question. `[CERT]`
- **42 only-B + 532 common = the VERSION axis** `[CERT]`: `only-B` are 4.15 base modules the 4.14 Honeywell
  cut either postdates or omits (`entsec*`, `accessControl`, `cloudLink*`, `ldapDriver`); the 532 common
  modules are the shared base, byte-differing purely by build (4.14 vs 4.15). A per-jar API diff would need
  `japicmp` (not installed — deferred, as it is a pure version-axis detail tangential to the license
  question). `[CERT]/[INFER]`

---

## 388.3 — The load-bearing conclusion `[CERT]`

**Not one module is present-or-absent because of licensing.** Every only-in-A / only-in-B / common difference
is attributable to VENDOR (Honeywell OEM ships 86 extra modules), VERSION (4.14 vs 4.15 base set + bytes), or
USER-CONTENT (the operator's own ~66 modules). This confirms [B387] from the disk side: the license does not
add or remove JARs; it is a runtime feature-gate over whatever modules are installed ([B387 §387.3]), and its
on-disk footprint is confined to `security/` ([B386]). A license moved to an unlicensed machine would find
the same module tree — the modules would simply fail their runtime feature checks, not be missing. `[CERT]/[INFER]`

---

## 388.4 — Self-verify

**Token re-checks** (`audits/B388-module-delta.txt`):
1. A=684, B=574, only-A=152, only-B=42, common=532 — ✓ (`comm` over distinct module names).
2. only-A Honeywell-OEM-branded = 86 (`grep -icE '^(hon|centraline|cl|spyder|galileo|sylk|maxpro|redlink|xl10)'`) — ✓.
3. user-content bucket incl. `chihuahua`/`nmodsreflow`/`electronicSignature`/`angeles` — ✓ (listed).
4. only-B incl. `entsec*`/`accessControl`/`cloudLink*`/`ldapDriver` (4.15 base) — ✓.

**4/4 tokens re-verified.**

**Marker tally**: `[CERT]` ≈ 12 · `[INFER]` 2 (the version-attribution of common bytes; the moved-license
corollary). Ratio ≈ 0.17 — low; EVIDENCE block. Confirms B387 from disk; remits OEM/3rd-party modules to
their corpus blocks.

---

## 388.x — Connections

- **[B387]** — this is the disk-side confirmation of B387's runtime finding: modules are feature-gated at
  runtime, not by file presence.
- **[B386]** — the license footprint is `security/` only; the module tree is vendor/version/user.
- **OEM/3rd-party module homes**: [B106/B120/B121] Spyder/Sylk, [B242] honIrmConfig, [B378] honAlarmExt,
  [B207] easyBinding, [B259] honeywellSpyderTool, [B138-B155] nmodsreflow, [B350-B356] electronicSignature,
  [B163-B177] chihuahua.
- **Remaining (this focus)**: L4 (`bin/` native 4.14 vs 4.15 — VERSION axis, must re-pair vs an installed
  instance since B is an installer without `bin/`), L5 (config/defaults — VERSION axis).
