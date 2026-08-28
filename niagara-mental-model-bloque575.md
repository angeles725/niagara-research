# Block 575 — License distribution: the supervisor collects each subordinate's license summary by Host ID, fetches updates from the online Tridium portal OR the local license database (brand-gated), and pushes them back — licenses ride the software-installable inventory, and the objects are the DSA-signed vendor licenses of the PKI thread

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV9 — the license management chain: portal → supervisor → subordinate)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 5-class `license` package + the portal/local branch.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/license/
  {BSupervisorLicenses,BLicenseStationExt,BLicenseInstallable,BLicenseStatus,BConvertToPerpetualLicenseModeJobStep}.java`.

**Scope**: how a supervisor licenses a fleet. Ties the license OBJECTS ([Block 392]/[Block 395] signing-pki;
[Block 386–391] license-diff) to their fleet DISTRIBUTION. Does NOT re-open license verification internals
([Block 395]) or Host ID derivation ([Block 424]) — connects.

---

## 575.1 `BSupervisorLicenses` — the fleet license manager over the channel [CERT]

`BSupervisorLicenses extends BComponent` `[CERT] :55`. Its operations delegate to the PV2 provisioning channel
([Block 568]) `[CERT] :89-118`: `getLicenseSummaries(String[] hostIds)`, `getLicenses(String hostId)`,
`getUpdatedLicenses(BEnvLicenseSummary[])`, `exportLicenses(hostIds, OutputStream)` — all
`this.getChannel().<op>(...)`. So it inventories licenses **by Host ID** ([Block 424] `getHostId`) across the
fleet through the same `niagaraProv` channel that carries software and lifecycle.

## 575.2 Two update sources: online portal vs local database [CERT]

The update logic branches on connectivity `[CERT] :188-191`:
```java
if (PortalLicenseUtil.requestLicensesOnline()) {
   updatedLicenses = PortalLicenseUtil.getPortalUpdates(licenseSummaries);              // ONLINE Tridium portal
} else {
   updatedLicenses = PortalLicenseUtil.getUpdatedLicenses(licenseSummaries, LicenseDatabase.LOCAL_INSTANCE); // LOCAL db
}
```
Same branch for certificates (`getPortalUpdates(certSummaries)` `[CERT] :217-218`). Whether the online path is
used is **brand-gated**: `getBrandProps().get("license.onlineRequest", true)` `[CERT] :79` (default true, but an
OEM brand can disable it). So a fleet is licensed either by reaching the Tridium/Honeywell licensing portal or,
offline, from the supervisor's `LicenseDatabase.LOCAL_INSTANCE` — the objects returned are `VendorLicense[]` /
`VendorCertificate[]` (the DSA-1024-signed vendor licenses/certs of [Block 392]/[Block 395]).

## 575.3 Licenses ARE installables [CERT]

`BLicenseInstallable` has `installableName = "licenses"` `[CERT] :44`. So a fleet's licenses are modeled as an
INSTALLABLE and ride the same software-distribution inventory ([Block 569] `BSoftwareContainer`) and install
machinery as modules/dists — "install licenses" is a step like "install a module". `BLicenseStationExt extends
BProvisioningStationExt` `[CERT]` is the per-station license ext (async completion via [Block 571] PV5).
`BLicenseStatus extends BFrozenEnum` `[CERT] :14` is the status vocabulary, and
`BConvertToPerpetualLicenseModeJobStep` `[CERT]` is a step that flips a station to perpetual-license mode.

## 575.4 Thesis [CERT-synthesis]

License provisioning closes the loop with the signing-pki thread: the SAME DSA-signed `VendorLicense`/
`VendorCertificate` objects that [Block 392]/[Block 395] showed are validated against a root hidden in
`baja.jar` are here DISTRIBUTED across a fleet, keyed by Host ID ([Block 424]), from either the online portal or
a local database, and installed like software. Provisioning does not mint or alter licenses (it moves signed
blobs); trust still rests on the vendor signature ([Block 395]). The online/offline split + brand gate is the
only operational choice — an air-gapped OEM fleet is licensed from `LicenseDatabase.LOCAL_INSTANCE`, an
internet-connected one from the portal.

## 575.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BSupervisorLicenses delegates getLicenseSummaries/getLicenses/getUpdatedLicenses/exportLicenses to the niagaraProv channel, keyed by hostId | [CERT] | BSupervisorLicenses.java:89-118 | token-checked ✓ |
| 2 | Update source branches: PortalLicenseUtil.getPortalUpdates (online) vs getUpdatedLicenses(LocalInstance) (offline) | [CERT] | :188-191,217-218 | token-checked ✓ |
| 3 | Online path brand-gated by license.onlineRequest (default true) | [CERT] | :79 | token-checked ✓ |
| 4 | Objects are VendorLicense/VendorCertificate (DSA-signed, B392/B395) | [CERT] | :109-110,217-218 | token-checked ✓ |
| 5 | Licenses modeled as installable (BLicenseInstallable name="licenses"); BLicenseStationExt async; perpetual-mode step | [CERT] | BLicenseInstallable.java:44; BLicenseStationExt/BConvertToPerpetual*.java | token-checked ✓ |
| 6 | Provisioning distributes signed blobs; trust rests on vendor signature, not provisioning | [CERT-synthesis] | rows 2-4 + [B395] | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 568]** (PV2) — the niagaraProv channel carries the license summary/get/export commands.
- **[Block 392]/[Block 395]** — the DSA-signed vendor licenses/certs distributed here; trust root in baja.jar.
- **[Block 424]** — Host ID, the key licenses are bound to.
- **[Block 386–391]/[Block 442]** — license-diff: what a licensed vs unlicensed station looks like on disk.
- **[Block 569]** (PV3) — licenses ride the same installable inventory as software.

## Open gaps (this block)

- The portal wire (`PortalLicenseUtil.getPortalUpdates` HTTP endpoint/auth to the Tridium licensing server) is
  named-not-traced — an external-service child gap (requires-execution / [CERT-web]). Focus continues at PV10
  (ux RPC surface), the final gap.
