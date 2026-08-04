# Block 335 — `BJsonSchemaService`: the jsonToolkit service gated by a three-layer license (feature + import/export attributes + SMA), with a superuser-only `runAsUser` identity that inbound writes assume

> Focus **jsonToolkit** — first evidence block (J1). READ-ONLY. Corpus language: ENGLISH.
>
> Scope: the service entry point `com.tridiumx.jsonToolkit.outbound.schema.support.BJsonSchemaService` and the
> `LicenseLimit` interface it implements — what the service is, the multi-layer license gate, the SMA check, and
> the `runAsUser` security identity that later gates inbound writes (J7). First block of the `jsonToolkit` focus.
>
> Sources (primary, decompiled N4.14.0.162), read in full inline:
> `organized/jsonToolkit/jsonToolkit-rt/vineflower/com/tridiumx/jsonToolkit/outbound/schema/support/BJsonSchemaService.java`
> (204 lines), `.../util/LicenseLimit.java` (98 lines). Module: `jsonToolkit` v4.14.0.162, vendor Tridium,
> "Utilities to help marshal niagara components to/from JSON", symbol `jstk`.
>
> Markers: `[CERT]` local primary source (`file:line`) · `[INFER]` deduction. Layer (integration/JSON marshalling)
> + Layer 22 (license/security). Block TYPE: **evidence**.

---

## 335.1 — What the service is: an add-on, restricted, unlinkable

`public final class BJsonSchemaService extends BAbstractService implements BIRestrictedComponent,
BIUnlinkableSlotsContainer, LicenseLimit` `[CERT]` (`BJsonSchemaService.java:59`). Note the namespace is
`com.tridiumx.jsonToolkit` — the `tridiumx` ("extended") family, an ADD-ON, not core `com.tridium` `[CERT]`
(package decl). Like the email service [Block 324] §324.1 it is a singleton `BAbstractService`
(`getServiceTypes()` = `{TYPE}`) `[CERT]` (`:176-178`) and a `BIRestrictedComponent`: it must sit under
`Services`, forbids duplicates, AND its parenting requires a **superuser context**
(`checkContextForSuperUser`) `[CERT]` (`:109-113`) — a stronger add gate than email's `[INFER]`. Icon
`braces.png`, logger `"jsonToolkit"` `[CERT]` (`:67-70`).

## 335.2 — The license gate has THREE layers

The service implements `LicenseLimit`, whose constants fix the identity `[CERT]` (`LicenseLimit.java:14-18`):
feature name **`jsonToolkit`**, prototype **`"DR-JSON or DR-S-JSON"`**, attributes `import` / `export` /
`sma.exempt`. The gate is not one check but three:

1. **Feature present** — `checkLicense()` = `Sys.getLicenseManager().getFeature("tridium","jsonToolkit")` `[CERT]`
   (`:43-46`). `started()` calls it and, on `FeatureNotLicensedException`, puts the service into **fault** with
   the unlicensed message `[CERT]` (`BJsonSchemaService.java:115-122`). And `service()` (the static accessor the
   rest of the module uses) THROWS `FeatureNotLicensedException` if the service is in fault `[CERT]` (`:159-166`)
   — so an unlicensed service is not merely faulted, it is unreachable by callers `[INFER]`.
2. **Per-operation attribute** — export and import are SEPARATE license attributes:
   `checkExportLicensed()`/`checkImportLicensed()` → `checkSmaAndLicenseAttribute("export"|"import")` →
   `checkLicenseAttribute(attr)` reads `feature.getb(attr, false)` `[CERT]` (`LicenseLimit.java:48-70`). So a
   license can grant EXPORT but not IMPORT (or vice-versa) — the outbound and inbound halves are independently
   licensable `[CERT]`. A missing attribute throws `"JSON Toolkit license attribute <x> missing"` `[CERT]`
   (`:56-58`).
3. **SMA** — see §335.3.

## 335.3 — The SMA gate: an expired maintenance contract disables the module

`checkSmaAndLicenseAttribute` (and thus every import/export op) also runs `checkSma()` `[CERT]`
(`LicenseLimit.java:60`, `:85-92`): unless the license carries the `sma.exempt` attribute, it fetches
`NLicenseManager.getLicenseMaintenanceExpiration(Sys.getBajaVendor())` and, if that expiration is in the PAST,
throws `FeatureNotLicensedException(NO_SMA_MSG "smaExpired")` `[CERT]` (`:64-66`, `:85-97`). So **an expired SMA
disables jsonToolkit's import/export**, not just future upgrades `[CERT]`. Operational tie-in: the corpus's client
licenses carry EXPIRED SMA (QNX-TITAN SMA 2024-07-11, Win-2E48) — if such a station licenses jsonToolkit without
`sma.exempt`, its JSON import/export would be dead on the SMA check `[INFER]` (cross-reference to the license
memory; must be confirmed against the actual license attributes). `initLicenseProperty()` surfaces the failure as
a component fault + `NO_LIC_MSG` `[CERT]` (`:26-36`).

## 335.4 — `runAsUser`: the security identity inbound writes assume, superuser-guarded

The service holds a `runAsUser` String property, `security` facet, default `"-unassigned-"` `[CERT]`
(`BJsonSchemaService.java:40-44`, `:61`). This is the Niagara user identity that jsonToolkit operations execute
as `[INFER]` (the name + the J7 setpoint-write path). It is protected three ways, all `[CERT]`:

- **Superuser-only edit** — `getPropertyValidator` throws `CannotValidateException("Must have superuser
  permissions to edit the runAsUser slot")` if a non-superuser modifies it (`:124-131`).
- **Tamper flag** — `changed()` sets `nonSuperUserModifiedRunAsUser` when a non-superuser changes it at runtime
  (`:133-137`), and `getRunAsUserInternal()` throws `PermissionException("runAsUser was modified by a non super
  user")` rather than return a tampered identity (`:147-153`).
- **Unlinkable** — `runAsUser` is the sole member of `UNLINKABLE_SLOTS`, returned by both
  `getUnlinkableSourceSlots`/`getUnlinkableTargetSlots` (`BIUnlinkableSlotsContainer`) `[CERT]` (`:66`, `:139-145`)
  — so it cannot be driven by a wiresheet link, only set directly by a superuser `[INFER]`.

This is the privilege boundary of the whole module: whatever user `runAsUser` names is the authority under which
inbound JSON writes points and acks alarms (J7) `[INFER]`. The controls ensure only a superuser chooses it.

## 335.5 — The other service slots (pointers)

- `globalCovSlotFilter` (`BSubscriptionSlotBlacklist`) — a global blacklist of slots excluded from outbound COV
  subscriptions `[CERT]` (`:50-52`) → **J3** (subscription pipeline).
- `exportMarkerRegister` (`BExportMarkerRegister`, hidden flag=4) — the registry of export markers the inbound
  side can register/deregister `[CERT]` (`:53-57`, static `register()` `:168-170`) → **J7** (export-marker
  security).
- `SMAExpirationMonitor` (`BSMAExpirationMonitor`) — a child that monitors the SMA state §335.3 `[CERT]` (`:45-48`).

## 335.6 — Execution: a per-type thread pool, and auditing

Actions are posted to a `ModuleThreadPool.getInstance(TYPE).post(...)` `[CERT]` (`:172-174`) — a per-module-type
worker pool (the audit's flooding question is J12) `[INFER]`. `audit(...)` writes an `AuditEvent` via
`Sys.getAuditor()`, defaulting the username to `"json"` when no context user is present `[CERT]` (`:180-195`) — so
jsonToolkit-driven changes ARE audited, attributed to the context user or `"json"` `[INFER]`.

## 335.7 — What this block does NOT resolve

- The OUTBOUND generation pipeline (schema model, subscriptions, queries, exporter) → **J2–J5**.
- The INBOUND pipeline (selectors, routers, handlers) and how `runAsUser` authorizes writes → **J6–J7**.
- `ModuleThreadPool` depth/backpressure → **J12**.
- The exact license attributes on the client's real licenses (SMA/import/export) — a live-license question, not
  answerable from decompiled source `[INFER]`.

## 335.8 — Connections

- [Block 324] §324.1-324.2 — the email service, same `BAbstractService` + license-feature-gate shape;
  jsonToolkit adds import/export attributes + an SMA gate + a superuser add-context.
- License memory (QNX-TITAN / Win-2E48, expired SMA) — §335.3 shows an expired SMA would disable jsonToolkit
  import/export absent `sma.exempt`.
- [Block 32] §32.3 — flagged jsonToolkit's Jayway dependency-CVE surface; this focus documents the module that
  bundles it (Gson 2.9.0 + jayway-jsonpath, DISMISSED per the census).
- J7 (inbound handlers) — the `runAsUser` identity §335.4 is the authority those writes assume.

## 335.9 — Self-verify

Block TYPE: **evidence**. Inline full read of both source files (302 lines); the load-bearing license + security
claims were read verbatim, not delegated: the feature gate (`LicenseLimit.java:43-46`), the import/export
attribute split (`:48-70`), the SMA gate (`:85-97`), and the `runAsUser` superuser/tamper/unlinkable controls
(`BJsonSchemaService.java:124-153`, `:139-145`). Extern anchors (token-checked by read):
`BJsonSchemaService.java:59`, `:115`, `:159`, `:124`, `:147`, `LicenseLimit.java:45`, `:56`, `:88`.

`verify-block.sh` marker tally (computed):

| Marker | count (adj) |
|---|---|
| CERT (extern file:line) | 25 |
| CERT-doc / CERT-hw / CERT-live / CERT-web / CERT-a | 0 |
| INFER | 10 |
| INFER/CERT ratio | 0.38 |

`verify-block.sh` exit 0 (citations extern — token-checked by read).

Evidence block: `[INFER]`s are the privilege-boundary reading, the unlicensed-unreachable deduction, and the
license cross-reference — each anchored to a cited `[CERT]`. First `jsonToolkit` block; the doc corpus
(`docJsonToolkit`, 115 files) enters as `[CERT-doc]` from J2 onward.
