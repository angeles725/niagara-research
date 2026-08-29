# Niagara N4 — own-modules-audit (OMA6): `httpClientGAngeles` is NOT an own-built exemplar — it is Tridium's `com.tridiumx.httpClient` add-on REPACKAGED under vendor SEJOFA with the SMA license gate "neutralized" and re-signed; its clean manifest is inherited, not authored

**Focus**: own-modules-audit · **Gap**: OMA6 (httpClientGAngeles) · **Session**: 2026-08-29 · **Block**: B641
**Sources** (`[CERT]` direct artifact): `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/modules/httpClientGAngeles-{rt,ux,wb}.jar`.

**Scope**: OMA1 ([B640]) flagged this as the shop's best-built module (0 custom permissions, real `vendorVersion`). Direct inspection reframes WHY. Reference = [B636]; licensing/SMA internals = [B477]/[B392] (REMIT); code-signing = [B639] (REMIT).

---

## 641.1 It is a rebranded Tridium add-on, not original code

`[CERT]` `httpClientGAngeles-rt.jar!META-INF/module.xml`:
```xml
<module name="httpClientGAngeles-rt" vendor="SEJOFA" vendorVersion="4.14.0.162"
   description="httpClientGAngeles runtime (Clean Build, SMA gate neutralized)"
   preferredSymbol="httpcga" moduleName="httpClientGAngeles" runtimeProfile="rt">
```
`[CERT]` package histogram — every class lives under **`com/tridiumx/httpClient/...`** (Tridium's add-on namespace), NOT under a `com/sejofa/*` or `com/angeles/*` namespace:
```
com/tridiumx/httpClient/util (14) · datatypes/auth (14) · ws (8) · datatypes/payload (8)
datatypes/options (8) · util/trigger (7) · conditions (6) · comm/client (6) …
```
So `httpClientGAngeles` = **Tridium's `httpClient` add-on** (the same `com.tridiumx.httpClient` documented as an outbound-HTTP driver, cf. [B504]-family/MediaTypes constant [B611]) taken wholesale, renamed `httpClientGAngeles`, given `vendor="SEJOFA"`, and re-signed. The 87 types / 122 classes are Tridium's, not the operator's.

---

## 641.2 What "SMA gate neutralized" means

`[CERT]` the manifest description literally says **"Clean Build, SMA gate neutralized."** `com.tridiumx.httpClient` is a LICENSED Tridium add-on (an SMA = Software Maintenance Agreement / feature-license gate, the licensing layer of [B477]/[B335] jsonToolkit's 3-layer gate model). "SMA gate neutralized" = the license/feature check was **patched out** so the module runs without the entitlement. Combined with §641.1 (re-signed under SEJOFA/ANGELES, [B639]) and the live posture `moduleVerificationMode=low` + `smDeveloperMode` ([B398]/[B635]), the module loads and runs unlicensed.

This is a factual licensing-integrity observation, not a build-quality one: the module is "clean" in the [B636] sense (proper manifest) precisely because it inherited Tridium's engineering — but it is a modified vendor binary with its license enforcement removed. That is a licensing/compliance exposure (running a Tridium add-on outside its SMA), separate from the code-quality axis this focus grades. It belongs on the operator's risk register, and the fix is entitlement (license the real `httpClient` or drop it), not a code change.

---

## 641.3 Grading vs the reference — inherited, not authored

`[CERT]` — tri-profile (`-rt` 122cls/87types, `-ux` 9/9, `-wb` 18/11), `vendorVersion=4.14.0.162` tracking the real Niagara version, **0 `<niagara-permission-groups>`** (falls to the minimal base grant, [B635]). Every property OMA1 praised is Tridium's original manifest hygiene, carried through the repackage. So:

- It is a valid POSITIVE reference for what a clean manifest looks like ([B640] Pattern 1/2 targets: 0 custom perms, real version) — but as a TEMPLATE to copy, copy the *shape*, not the provenance.
- It does NOT demonstrate the operator's own modules can't reach that bar — it shows Tridium already met it. The operator's own modules (chihuahua/angeles/etc.) are the ones to bring up to this shape.
- The correction to [B640]: httpClientGAngeles as "the best-built" is true structurally but MISLEADING as "the shop's exemplar of good practice" — it is repackaged vendor code. The honest exemplar of the operator's OWN good practice is narrower (e.g. the palette habit, the `niagara-tools` KB, [B639]).

---

## 641.4 What this establishes + recommendation

- **Compliance**: running `com.tridiumx.httpClient` with the SMA gate neutralized is a licensing exposure. Recommend: license the genuine Tridium `httpClient` add-on if the capability is needed, or replace it with an in-house outbound-HTTP component. Do not treat "it works" (enabled by `moduleVerificationMode=low`) as authorization.
- **Template**: copy httpClientGAngeles's manifest SHAPE (0 custom perms, real `vendorVersion`) into the shop template (OMA8) — but author original code under a `com/sejofa/*` namespace, don't fork vendor modules.
- **Signer**: it is signed `NIAGARA4` block-name under the SEJOFA/ANGELES migration ([B639]); consistent with re-signing a foreign jar.

---

## Self-verify

| # | Claim | Marker | Citation | Checked |
|---|---|---|---|---|
| 1 | all classes under com/tridiumx/httpClient/* (Tridium's namespace), not com/sejofa or com/angeles | [CERT] | package histogram (this session) | ✅ unzip |
| 2 | manifest vendor=SEJOFA, vendorVersion=4.14.0.162, description "Clean Build, SMA gate neutralized" | [CERT] | httpClientGAngeles-rt.jar!module.xml | ✅ unzip -p verbatim |
| 3 | 0 `<niagara-permission-groups>` (inherited minimal grant) | [CERT] | manifest scan ([B640]) | ✅ unzip -p |
| 4 | it is Tridium's licensed httpClient add-on with its SMA/feature gate removed | [CERT]/[INFER] | description verbatim + com.tridiumx namespace + [B477] SMA model | ✅ read+derive |
| 5 | tri-profile 122/9/18 classes; real Niagara version tracked | [CERT] | manifest scan | ✅ unzip -l |

**Tally**: [CERT] ×4 · [INFER] ×1 · direct-artifact block. Manifest description + namespace token-checked verbatim.

## Connections

- **[B640]** — reframes "the exemplar": structurally clean but inherited/vendor-forked, not own-authored. **[B477]/[B335]** — the SMA/feature-license model that was neutralized (REMIT). **[B639]** — re-signed under the SEJOFA/ANGELES migration. **[B398]/[B635]** — `moduleVerificationMode=low`/`smDeveloperMode` let a modified vendor jar load. **[B636]** — the manifest shape it exemplifies.
- Forward: OMA8 template copies the SHAPE; OMA8 risk-register lists the SMA exposure.

## Gaps uncovered

- None new for the backlog. Whether other operator modules also fork/neutralize vendor add-ons is worth a one-line check in OMA7 (the SEJOFA dashboards), noted there.
