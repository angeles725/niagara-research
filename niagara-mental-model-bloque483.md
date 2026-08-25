# Block 483 — The anatomy of a Niagara license: the complete `<license>` / `<feature>` / `<signature>` / `<certificate>` composition, every parsed attribute and its field, the open-ended limit/attribute key set, and the four file variants

> **Focus:** `licensing`. **Question (operator):** cómo está compuesta la licencia, qué la caracteriza.
> Consolidates the scattered structure notes ([B126 §126.6], [B14], [B322], [B387], [B480]) into one
> code+example anatomy. READ-ONLY, decompiled source + on-disk XML; Host IDs masked, no sig/key values.
>
> **Sources:** `organized/baja/…/com/tridium/sys/license/{LicenseFile,NFeature,LicenseUtil,CertificateFile,
> Brand}.java` + `dom/{VendorLicense,Feature,LicenseDatabase}.java`; on-disk
> `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162/security/licenses/{Honeywell,HoneywellCentraLine,Webs}.license`
> + `security/certificates/{Honeywell,HoneywellCentraLine,Tridium}.certificate`. Markers §3.

## §483.1 — The `<license>` root `[CERT]`

Root qname is `license` (single) or `licenses` (container → iterate `elems("license")`); else
`XException("Missing <license> element")` (`LicenseFile.java:47-62`). Attributes read (parse order → field → type):

| Attr | Field | Type / parsing | Cite |
|---|---|---|---|
| `vendor` | `vendor` (String) | selects the vendor `.certificate` (`getCertificate(vendor)`) | `LicenseFile.java:78-79` |
| `hostId` | `hostId` (String) | validated by subclass `isLicenseHostIdValid()` (`equals(Nre.getHostId())`) | `:84-85` |
| `generated` | `generated` (long) | `parseDate(str,true)` **start-of-day**; required ≥0 | `:95-101` |
| `expiration` | `expiration` (long) | `parseDate(str,false)` **end-of-day**; `"never"`→`Long.MAX_VALUE` | `:115-121` |
| `maintenanceExpiration` | `maintenanceExpiration` (Optional<Long>) | end-of-day; absent→empty (SMA) | `:132` |
| `unreleasedSwAccessExpiration` | (Optional<Long>) | end-of-day; "unreleased module" grace | `:131` |
| `version` | `licenseVersion` (Version) | `new Version(str)`; optional | `:134-137` |

Temporal gates (each sets `error` and aborts): `Nre.getHostId()==null`→"HostId not supported" (`:41`); clock floor
`now<parseDate("2015-01-01")`→"Current system time appears invalid…" (`:106`); anti-backdate
`now<generated-129600000L` (36 h, `MILLIS_IN_36_HOURS`)→"Current date is earlier…" (`:110`); `now>expiration`→
"License file is expired" (`:126`). **Tridium-only** version gate (`:139-168`): rejects if
`licenseVersion.major()!=curVer.major()` ("License for older version") or `minor()<curVer.minor()` ("…maintenance
not active"); `maintenanceExpiration≥baja.releaseDate` relaxes curVer to 4.0. `VendorLicense` DOM (edit/save) is
stricter: root MUST be `license` (`VendorLicense.java:251-252`); new blank defaults `generated=now`,
`expiration="never"`.

On-disk (masked): `<license vendor="Honeywell" expiration="2027-03-31" hostId="Win-****-****-****-****"
version="4.15" generated="2026-04-02">` (`Webs.license` identical shape, `vendor="Tridium"`). None of the three
examples carry `maintenanceExpiration`/`unreleasedSwAccessExpiration`.

## §483.2 — The `<feature>` children `[CERT]`

Anatomy `<feature name="…" [expiration="…"] [any attr="…"]…/>`, parsed by `loadFeature()`:
- `name` required (`:208`); `expiration` optional end-of-day, **clamped to `min(license.exp, feature.exp)`**
  (`:210-217`), absent→`MAX_VALUE`.
- **Every other attribute is copied verbatim into `feature.props` (Properties)** (`:221-231`) — this is why the
  key set is OPEN-ENDED.
- Key = `LicenseUtil.toKey(vendor,name)` = `lower(vendor)+':'+lower(name)` (`NFeature.java:19`).
- Readers: `get` raw; `geti` `Integer.parseInt` (no "none"); `getb` strict `"true"/"false"` else
  `IllegalStateException`; **`LicenseUtil.parseLimit`** = the canonical limit reader: absent→0,
  `"none"`(ci)→`Integer.MAX_VALUE`, else parseInt (`LicenseUtil.java:590-602`); `parseList` = `;`-delimited.
- Merge across files: later `expiration` wins (`Math.max`, `NFeature.java:102-104`); duplicate `tridium:brand`
  → fatal "Cannot have multiple branded licenses" (`NLicenseManager.java:224-226`).

**Attribute-key families** (open-ended; observed live in `Webs.license`):
- `*.limit` counters (parseLimit): `point/points/dataPoint/device/history/historyExt/historyRecord/schedule/
  camera/foxStream/zone/accessZone/proxyext/algorithm/alert/alarm/port/station/station.entity/local.entity/
  component/console/credential/dictionary/display/dvr/nvr/elevator/file/foreign{Device,History,Point,Schedule}/
  hierarchy/reader/resource/session/tenant/utility/ven/asureId/app…` (+ real typo keys `point.limt`, non-dotted
  `fileLimit`/`serverLimit` read only by code asking that exact key) + edge variants
  (`edgeLite1_*.limit`, `edgeLite1_device.percentage`).
- boolean (getb): `moduleDev, skipModuleValidation, sma.exempt, export, import, virtual, guestEnabled,
  bacnetWrite, historyImport, enterprise`, capability toggles `fox/http/https(ssl)/tcpip/serial/px/ui/ui.wb/
  ui.wb.admin/admin/kerberos/sox/ada`.
- brand (Brand.java): `brandId, accept.station.in, accept.station.out, accept.wb.in, accept.wb.out` (`;`-lists of
  `PatternFilter`, default `*`).
- descriptive strings: `owner, project, vendor.name, manufacturer, type, systemType, rev, limitsPolicy, module`.

**Real feature names:** `Honeywell.license` 27 vendor apps (`spyderProgrammable, maxproVideo (camera.limit=16),
honEasyBinding, honEasyTemplate, HBDashboard, …`); `HoneywellCentraLine.license` = single `clCbus`;
`Webs.license` ~130 Tridium-core (`brand, accessControl, alarm, analytics, bacnet, bacnetSc, crypto, demoStation,
developer, station (station.limit=128, guestEnabled=true), …`). In these DEMO licenses almost every limit is
`"none"` (unlimited) — the only real numeric caps seen are `station.limit="128"` and `camera.limit="16"`.

## §483.3 — The `<signature>` element `[CERT]`

A single `<signature>` child, Base64(MIME) of the DSA signature = DER `SEQUENCE{ INTEGER r, INTEGER s }`
(ASN.1 prefixes `30 2c 02 14…` / `30 2e 02 15…`). Decoded `Base64.getMimeDecoder().decode(sigElem.string())`
(`LicenseFile.java:82-83`); missing → "Missing signature element" (`:199-203`). Optional `algorithm` attr
switches to the explicit-algorithm / v2 path (none of the three files carry it → default DSA). Verify:
`root.removeContent(sigElem)` → `xml = LicenseUtil.encode(root)` → `verify(xml, sig, publicKey[, algorithm])`
(`:170-181`). **Canonicalization** (`LicenseUtil.encode`, `:645-689`): `<qname` + attrs in stored order
(no escape) + `>\n`; children recursed; text nodes + `\n`; `</qname>\n`; chars written as Latin-1 bytes. Bespoke,
non-XML-canonical — any reorder/reformat breaks the signature (cf. [B482 §482.4], [B323]).

## §483.4 — The `<certificate>` / `<publicKey>` `[CERT]`

Binds a vendor to the DSA public key that signs that vendor's licenses; the cert itself is signed by the
embedded master key. `CertificateFile.load()`: root must be `certificate` (`:32-34`); `vendor` (`:36`);
`<publicKey>` required (`:37-41`); algorithm = `algorithm` attr, **fallback to the misspelled `algorthm`, default
"DSA"** (`:43-46`); key = `Base64…decode` → `toPublicKey(data, algorithm)` (X509EncodedKeySpec / SPKI); expiration
end-of-day, `now<=expiration`; `<signature>` stripped, `encode(root)`, verified against the **Version-selected
embedded key** (`version="2.0"`→ECDSA P-256, else master DSA-1024) (`:69-84`; `LicenseUtil.java:718-724`).
On-disk (masked): `<certificate version="1.0" vendor="Tridium" generated="2003-07-16" expiration="never">` with
`<publicKey algorthm="DSA">…SPKI Base64…</publicKey>` + `<signature>`. Certificates have **NO `hostId`** (not
node-locked) and **NO `<feature>`** children. Trust chain: embedded master DSA-1024 / v2 ECDSA →verifies vendor
`.certificate` →cert's publicKey →verifies that vendor's `.license`.

## §483.5 — File variants `[CERT]`

- **Node-locked `.license`**: `<license>` with `hostId`; valid iff `hostId.equals(Nre.getHostId())` (the normal
  on-disk case).
- **Subscription license**: same `<license>` anatomy, but liveness enforced by recurring online entitlement +
  key-rotation (`SubscriptionLicenseManager`/`EntitlementCheck`), not static expiration ([B480]).
- **Vendor `.certificate`**: root `certificate`, has `<publicKey>`, no `hostId`, no features.
- **`.lar`**: a ZIP of `licenses/<hostId>/<name>.license` entries (`LicenseDatabase.java:238,308`) — a container,
  no new fields ([B479 §479.3]).

## §483.6 — Labeled anatomy (real `Honeywell.license`, masked)

```
<license                                 root: "license" | "licenses" wrapper       [LicenseFile:47,52]
   vendor="Honeywell"                    → vendor; picks vendor .certificate         [:78-79]
   hostId="Win-****-****-****-****"       → hostId; must == Nre.getHostId()           [:84-85 / NodeLocked:54]
   version="4.15"                        → licenseVersion; tridium major/minor gate  [:134-167]
   generated="2026-04-02"                → parseDate startOfDay; anti-backdate 36h    [:95-110]
   expiration="2027-03-31">              → parseDate endOfDay; now>exp ⇒ expired      [:115-127]
   (maintenanceExpiration/unreleasedSwAccessExpiration → Optional<Long>, absent here [:131-132])
 <feature name="maxproVideo"             → key=lower(vendor):lower(name)              [:208 / NFeature:19]
    expiration="2027-03-31"              → min(license.exp, feature.exp)              [:210-217]
    camera.limit="16"                    → parseLimit ⇒ 16 (real cap; "none"⇒MAX)     [LicenseUtil:590-602]
    point.limit="none" …/>              → all extra attrs → NFeature.props           [:221-231]
 <feature name="brand" brandId="…" accept.station.in="*" …/>   Brand + PatternFilter lists [Brand:66-90]
 <signature> …DER SEQ{r,s} Base64… </signature>   strip→encode(root)→DSA verify vs cert.publicKey [:170-181]
</license>
```

## §483.7 — Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|---|---|---|---|
| 1 | Root `license`/`licenses`; attrs vendor/hostId/generated(SOD)/expiration(EOD,never→MAX)/maint/unreleased/version | `[CERT]` | `LicenseFile.java:47-137` | PASS |
| 2 | Temporal gates: 2015 floor, 36h backdate, expired; tridium version gate | `[CERT]` | `LicenseFile.java:41-168` | PASS |
| 3 | feature = name+expiration(clamped)+open-ended props; key lower(vendor):lower(name); parseLimit none→MAX | `[CERT]` | `LicenseFile.java:208-231`; `NFeature.java:19`; `LicenseUtil.java:590-602` | PASS |
| 4 | dup brand fatal; feature merge by max expiration | `[CERT]` | `NLicenseManager.java:220-229` | PASS |
| 5 | Real feature sets: Honeywell 27, CentraLine clCbus, Webs ~130; caps mostly "none" (station.limit=128, camera.limit=16) | `[CERT]` | on-disk `{Honeywell,HoneywellCentraLine,Webs}.license` | PASS |
| 6 | `<signature>` = Base64 DER SEQ{r,s} DSA; strip→encode→verify; algorithm attr → v2 path | `[CERT]` | `LicenseFile.java:82-181` | PASS |
| 7 | `.certificate`: root certificate, `<publicKey algorthm="DSA">` (typo), no hostId/features, verified by embedded master | `[CERT]` | `CertificateFile.java:32-84` | PASS |
| 8 | Variants: node-locked / subscription / certificate / .lar (ZIP of licenses/<hostId>/*.license) | `[CERT]` | `LicenseDatabase.java:238,308` | PASS |

**Tally:** 8 claims, 8 `[CERT]`, 0 `[INFER]`.

## §483.8 — Connections

- The "qué la caracteriza" reference for the licensing focus; feeds `docs/niagara-licensing.md` §1.
- Byte-level crypto composition of the `<signature>`/`<publicKey>` (DER SEQ{r,s}, SPKI) is the native
  counterpart in @Segundo's `dsfspi` pass (`parseDSASignature`/`parseDERInteger`/`parseDSAPublicKey`).
- Builds on [B126] (schema), [B14] (limits), [B482] (verify/encode + master keys), [B480] (subscription).
