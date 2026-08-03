# Reverse Engineering Manuals — Niagara N4 Licensing
## Archival edition · field notes on the licensing layer (2026-era platform)

> **Archivist's note (2135).** This volume is the synthesis of a specialized corpus of applied
> observations on the licensing layer of the building-automation platform Niagara N4 (Tridium /
> Honeywell, N4.14 era). It records how the layer behaved under study: where its verifiers lived,
> what they checked, and how the system failed when inputs varied. It is a record of observation —
> not a guide to action. The verifiers described here are the ones that *existed*; how to produce
> their outputs, or how to evade them, is deliberately absent from this record, for reasons stated
> in §8.

---

## 0. How to read this volume

**Object of study.** The licensing layer of Niagara N4 as it existed circa 2026: host identity
binding, entitlement enforcement, and module authenticity verification, and the interactions
between them.

**Method.** Observation of a deployed platform — decompiled Java modules, native binaries, and a
live install. The corpus these notes condense was built iteratively: partial understandings,
refinements, and corrections are part of the record (e.g., §4.4 records a correction where an
artifact initially believed to be cryptographic was identified as a serial-communication library).

**Certainty markers** (corpus convention): `[CERT]` verified against implementation · `[CERT-doc]`
vendor documentation · `[INFER]` inference.

**What this manual is not.** It does not document how valid license artifacts are produced, how
validation checks are altered, or how host identity is emulated. See §8.

**Companion — the recognition map.** This volume records decisions; its companion inverts them.
`analizador-licencias/04-quickref-diagnostico-deteccion.md` maps symptom → surface → fragment →
response: the same decisions, read backwards, for arriving after the fact. The volume is the map
of decisions; the companion is the map of recognition. Together they are the archive.

---

## 1. The object: three control planes

The licensing layer is not one mechanism but three, joined at runtime:

| Plane | Question it answers | Where it lives (observed) |
|---|---|---|
| **Host identity** | *Which machine is this?* | Native: `nre.dll` (host ID), daemon registration |
| **Entitlements** | *What is this machine allowed to do?* | Java: `baja.jar` → `com.tridium.sys.license.*` (LicenseManager, Feature, SMA attributes) |
| **Module authenticity** | *Is this code what it claims to be?* | Signature files (`.sig`), trust anchors (`.certificate`), trust stores, native verifier `nverify.exe` |

Artifacts observed in the field: `.license` files under `db/<hostId>/`, root `licenses/` aliases,
`.sig` sidecar signatures, `.certificate` XML trust anchors, four trust stores, and the
subscription state directories. `[CERT]`

Sources: `niagara-mental-model.md` §2 · `niagara-mental-model-bloque41.md` §41.6 ·
`niagara-mental-model-bloque126.md`.

---

## 2. Observation I — host identity

**What it is.** A per-machine binding: the platform derives a HostId from hardware state and keys
its license artifacts to it. The derivation is native (observed `getHostId0` in `nre.dll`,
resolved via volume serial); the resulting value is a 64-bit hash in the N4 generation. `[CERT]`

**Observable consequences.**

- The canonical license home is `db/<hostId>/` — one directory per distinct host identity. Root
  `licenses/*.license` files are *aliases* for the current host, not sources of truth. `[CERT]`
- A host whose identity changes (NIC swap, VM clone) stops matching its own aliases: the platform
  behaves as a different machine and licenses are "lost" until re-provisioned. `[CERT]`
- Legacy AX-generation licenses are incompatible with N4 hosts: the host ID hash changed from
  32-bit to 64-bit, so a new license request is required. `[CERT]`

**Behavior under controlled variation** (as observed, for diagnosis):

| Variation | Observed behavior |
|---|---|
| HostId changes (hardware/clone) | Alias mismatch → license not found; re-provisioning required |
| VM clone without re-identity | Duplicate host identity; documented regeneration flow exists for cloned VMs |
| AX→N4 migration | License incompatibility; new request mandatory |

**Boundary.** This manual records the observable signature of the binding (its existence, its
consequences, its failure modes). Procedures to emulate or reproduce the identity value are not
part of the record. `[CERT]`

Sources: `niagara-mental-model-bloque124.md`, `niagara-mental-model-bloque125.md` (native host ID),
`niagara-mental-model-bloque40.md` §40.4.8 (`db/<hostId>/` canonical home),
`niagara-mental-model-bloque41.md` §41.6 (regeneration flow, AX 32→64-bit incompatibility).

---

## 3. Observation II — entitlements

**Where entitlements live.** License files are XML documents carrying features and attribute
values, including the SMA attribute model. The validating classes live in `baja.jar` under
`com.tridium.sys.license` — not in a separate `license-rt.jar` (a correction recorded in the
corpus). `[CERT]`

**Validation pipeline.** The observed pipeline applies a sequence of checks to a presented license;
the corpus documents five stages covering: presence, host binding, authenticity of the artifact's
signature, feature gating, and time validity. The pipeline's outputs are the failure modes listed
below. `[CERT]` (full stage-by-stage detail in `01-diagnostico-despliegue.md`)

**Enforcement at runtime.**

- Feature gates are consulted at runtime (`Feature.getb(...)` style checks); attribute lookups are
  string-based and typo-prone — an observed property that produces distinctive misconfiguration
  failures. `[CERT]`
- Point counting: runtime limits (points, devices, histories, schedules) are enforced by counting
  visitors; **virtual components are excluded** from point limits — a documented architectural
  trade-off. `[CERT]`
- Subscription licensing is a separate plane: `EntitlementApi` plus the LRT state, held in **three
  separate directories** (subscription / certificate / license). Backups that omit any one corrupt
  subscription state. `[CERT]`

**Observed failure modes** (summary; full table in `01-diagnostico-despliegue.md`):

| Failure class | Observed symptom |
|---|---|
| License absent / host mismatch | Feature absent; entitlement not found in logs |
| Tampered or forged artifact | Signature check fails by design (§4, §7) |
| Time anomalies | Clock rollback is countered by an invalid-time floor constant |
| Typo in string attribute | Feature silently absent — misconfiguration masquerading as licensing failure |

**Boundary.** The format and validation behavior are recorded for diagnosis, deployment, and
legitimate interoperation. The corpus does not record how a valid artifact is produced outside the
vendor's issuance process. `[CERT]`

Sources: `niagara-mental-model-bloque41.md` §41.6 (location, typo-prone attrs, EntitlementApi/LRT,
anti-rollback floor) · `niagara-mental-model-bloque14.md` (point counting, limits) ·
`niagara-mental-model-bloque28.md` §28.13 (virtual-point exclusion) ·
`analizador-licencias/03-interop-legitima.md` (public API read/validate only).

---

## 4. Observation III — module authenticity

**Four distinct signature schemes** were observed on one deployed install — the layer is not a
single mechanism but a family: `[CERT]`

| Target | Scheme (observed) | Notes |
|---|---|---|
| Modules | SHAwithRSA, RSA-2048, `.sig` sidecars | 256-byte raw signature, not PKCS#7 |
| Licenses / certificates | SHA1withDSA, DSA-1024 | Legacy scheme; `.certificate` XML uses a custom DSA format (observed since 2003, including the historical `algorthm` typo) |
| Native PE binaries | Authenticode, RSA-4096 (DigiCert G4) | Verified by native `nverify.exe` against the same trust anchor |
| `libciper.so.sig` | ECDSA P-256 | **Correction in the record:** this file is *not* a crypto library — it is the JNI serial-communication library for Sylk/Spyder devices on QNX-ARM; only its signature uses ECDSA |

**Trust model.**

- Module signatures are verified against a trust anchor: the signing certificate is the vendor's
  (observed validity "year 9999" — eternal by design). `[CERT]`
- Four runtime trust stores exist (user / system / daemon, plus a `userUntrustedStore` quarantine);
  the install-time `truststore.jks` is a seed, not a fifth store. `[CERT]`
- The module-validation gate has a documented asymmetry: a developer-license feature
  (`skipModuleValidation`) exists in the web-facing license but not in the OEM field license —
  i.e., the development (Windows) world validates loosely, the field (QNX JACE) world strictly.
  Recorded as a **threat-model observation for hardening and incident response**, not as a
  procedure. `[CERT]`
- `exemptions.tes` (binary) allows user-level bypasses of the permission model; its existence is
  recorded, its production is not. `[CERT]`

Sources: `niagara-mental-model-bloque18.md` (§18.3.2 bypass-matrix *as a hardening observation*),
`niagara-mental-model-bloque27.md` (trust stores, certificate formats, signing service),
`niagara-mental-model-bloque40.md` §40.4.9 (asymmetry), `niagara-mental-model-bloque113.md`
(signing/hardening), `niagara-mental-model-bloque126.md` (signature schemes, `nverify.exe`,
`libciper` correction), `niagara-mental-model-bloque112.md` (detection surfaces).

---

## 5. Field notes — how enforcement meets runtime

The following observations are from a live install and from controlled variation during analysis.
They are the diagnostic core of the record.

1. **Boot and load.** Module loading consults the authenticity gate; station startup consults
   entitlements. A station runs with what its host identity can prove it owns. `[CERT]`
2. **The incident that shaped the record (B75).** An unsigned module, loaded under a permissive
   configuration, opened port 443 and deleted audit entries. The response was not "how to repeat
   it" but the opposite: detection tooling (SecurityDashboard, PolicySpy), audit forensics, and
   structural hardening of the signing gate. The record preserves the asymmetry that made it
   possible — permissive development config vs. strict field config — as a risk, and documents the
   hardening that closed it. `[CERT]`
3. **Backup/restore discipline.** Subscription state spans three directories; partial backups
   silently corrupt subscription state. Licenses are host-bound; a restored station on a different
   host identity is a different machine. Both are operational facts, not defects to exploit. `[CERT]`

---

## 6. Field notes — the decision map

*Fragmentary records. No introductions, no conclusions — sequences of observation tied to
behavior. Each fragment: **condition → decision → observable**. The observables are the
diagnostic surface. Outcomes are recorded as they were seen; how such observations were elicited
is not part of the record (§8).*

**F1 · Identity resolution.** Condition: license files present under `db/<hostId>/` while host
identity changed (NIC swap, VM clone). Decision: *not found*. Observable: the root `licenses/`
alias fails to resolve; feature lookups return absent. Correlates: directory listing shows
licenses while features are absent — a state that reads as "missing license" but is a
host-binding failure. The two conditions are distinguishable by comparing the alias target
against `db/` contents. `[CERT]`

**F2 · Legacy identity.** Condition: AX-generation license presented to an N4 host. Decision:
*incompatible*. Observable: new license request required. Correlates: host ID hash width changed
32→64-bit between generations. `[CERT]`

**F3 · Attribute gating.** Condition: feature lookup string differs from the attribute name by a
single character. Decision: *feature silently absent* — no error, no log. Observable: behavior
identical to an unlicensed feature. Correlates: string-based attribute model; misconfiguration
and missing license are indistinguishable at runtime and separable only by direct string
comparison. `[CERT]`

**F4 · Time validity.** Condition: clock moved backwards. Decision: *invalid-time state*, not
extended validity. Observable: license reads invalid although wall-clock time is inside the
validity range. Correlates: an invalid-time floor constant in the validation logic. `[CERT]`

**F5 · Authenticity gate.** Condition: module presented with a signature not rooted in the trust
anchor. Decision: *rejection at load*. Observable: load failure; evidence in daemon log and
audit. Correlates: module signatures verified against the vendor trust anchor — the same check
that makes tampering detectable by design. `[CERT]`

**F6 · The two-key gate.** Condition: `skipModuleValidation` decision. Decision: bypass only when
*both* the flag and the license feature are present. Observable: the same module accepted on a
Windows development supervisor and rejected on a QNX field JACE. Correlates: license asymmetry —
the developer license carries the feature, the OEM field license does not. The decision differs
per host license, not per module. Recorded as a hardening observation; the gate's existence is
the field reality. `[CERT]`

**F7 · Counting.** Condition: physical points added past entitlement. Decision: *limit state at
runtime*. Observable: point/device/history/schedule limits enforced by counting visitors.
Condition variant: virtual components. Decision: *not counted*. Observable: adding virtual
points changes nothing. Correlates: the counting visitor recognizes the virtual component space
and skips it; federation counts at origin. `[CERT]`

**F8 · A decision trace (incident B75).** Sequence: unsigned module + permissive configuration →
accepted → outbound port opened → audit entries deleted. The trace's value: each decision in the
sequence was observable, and each was subsequently hardened — structural signing gate, detection
surfaces (SecurityDashboard, PolicySpy), audit discipline. The record preserves the trace as a
lesson in how decisions compound, not as a procedure. `[CERT]`

**F9 · Multi-host distributions.** Condition: the same station image deployed across multiple
hosts (multi-host distro pattern). Decision: *per-host resolution* — each host's entitlements
resolve from its own `db/<hostId>/`. Observable: identical station data across hosts with
different entitlement sets; the root `licenses/` alias matches only the current host. Correlates:
`db/<hostId>/` is the canonical license home — the variation is a property of the deployment, not
of the station. `[CERT]`

**F10 · Platform asymmetry.** Condition: the same module fleet deployed across platform classes
(Windows supervisor vs QNX field controller). Decision: *validation state differs per class* — the
field controller rejects unsigned OEM modules the supervisor accepts (cf. F6). Observable:
deployment-dependent acceptance of identical artifacts. Correlates: `Webs.license` asymmetry —
`skipModuleValidation` present on the Windows supervisor, absent on the QNX JACE. `[CERT]`

**F11 · Backup/restore.** Condition: partial backup of subscription state — one of the three LRT
directories omitted. Decision: *subscription invalid after restore*. Observable: entitlements
absent although the license file is present and intact. Correlates: EntitlementApi/LRT spans
subscription / certificate / license directories; backups must include all three. `[CERT]`

**F12 · Mixed-license sites.** Condition: deployment carrying both developer and OEM licenses.
Decision: *per-module validation depends on which license is effective*. Observable: within one
station, some modules validated and others not. Correlates: the `skipModuleValidation` feature in
one license and not the other — documented as a cross-license matrix with an operational hole in
mixed-license deployments. `[CERT]`

**F13 · Cloned identities.** Condition: VM cloned without re-identification. Decision: *duplicated
host identity; regeneration required*. Observable: `CLONED_FILE` state; re-provisioning flow
(`regenerateNreId`). Correlates: identity derives from host state, and clones inherit it. `[CERT]`

**F14 · Topology counting.** Condition: supervisor–subordinate federation. Decision: *counts at
origin* — a subordinate's virtual components are not counted. Observable: the same station under a
supervisor reports different limit consumption than standalone. Correlates: the counting visitor
skips the virtual component space; counting decisions differ by topology. `[CERT]`

**F15 · Clock drift.** Condition: site clock moved backwards (NTP failure, manual correction).
Decision: *invalid-time state*. Observable: a valid license reads invalid — a symptom
indistinguishable from expiry without knowledge of the floor constant. Correlates:
`INVALID_LICENSE_TIME_MILLIS_FLOOR` anti-rollback in the validation logic. `[CERT]`

**F16 · The same process, before and after (hardening).** Condition variant A (pre-hardening):
unsigned module + permissive configuration. Decision: *accepted* — incident B75. Condition variant
B (post-hardening): structural signing gate + trust-anchor discipline. Decision: *rejected*.
Observable: the identical class of artifact flips between two states of the same platform.
Correlates: the variable was configuration and trust state, not the artifact — the decision
changed because the platform changed. `[CERT]`

**Trace index.** Each decision leaves traces on surfaces the platform maintains on its own: daemon
log and audit (module-load events, rejections), filesystem state (`db/<hostId>/` layout, alias
resolution), runtime state (feature absence, limit states, time state), and platform-native views
(SecurityDashboard, PolicySpy). The forensic pairing: F1/F9/F13 → filesystem · F4/F15 → runtime
time state · F5/F10/F12/F16 → daemon log + audit · F7/F14 → runtime counters. All fragments derive
from logs, states, and responses the system produced in ordinary operation and in
already-conducted analysis — variations the field produced, not scenarios constructed for the
record. `[CERT]`

**A note on method.** The fragments record decisions as observed on a deployed platform and in
already-conducted analysis. They do not prescribe how to elicit decisions the record does not
contain: no input-variation campaigns, no inference of unrecorded properties from responses. The
distinction is the volume's own boundary — outcomes, not elicitation (§8).

---

## 7. Detection surfaces — reading the layer forensically

Because authenticity is enforced by signature and trust anchor, tampering is *detectable by
design*; the record's forensic value lies in the surfaces where evidence appears:

- **SecurityDashboard / PolicySpy** — platform-native views of security posture and policy. `[CERT]`
- **Daemon log and audit trail** — signature failures, module-load rejections, and privilege
  events leave traces. `[CERT]`
- **Trust store state** — a store whose anchors changed is a store that was touched. `[CERT]`
- **Offline verification** — a presented artifact can be checked against the public certificate
  without the platform running (native verifier `nverify.exe`; read-only API in
  `03-interop-legitima.md`). `[CERT]`
- **Indicator table** — the corpus maintains an IOC table for forged/manipulated licenses
  (`02-deteccion-tampering.md`).

---

## 8. Boundary of this volume

This manual records the licensing layer as a *system under observation*: its verifiers, their
inputs, their failure modes, and the surfaces where their activity can be read. It does **not**
record — and the corpus from which it is drawn explicitly excludes:

- production of valid license artifacts outside the vendor's issuance process;
- alteration of validation checks, conditional branches, or binaries;
- emulation or reproduction of host identity;
- disabling or weakening of module-authenticity gates.

The reason is twofold and stated plainly. First, the legal frame of the period: circumventing
technological protection measures and reproducing licensed artifacts is unlawful in most
jurisdictions the corpus covers. Second, the subject is critical infrastructure: the platform
sits between operators and buildings. A record that weakened its controls would be a liability to
the very systems it studies. The archive's purpose is preservation and understanding — "a record
of observation at the edge of system understanding," not a guide to action.

---

## 9. Epilogue — what was never preserved

*Later readers will notice a gap in this volume. The fragments record conditions, decisions, and
observables — precisely, repeatedly, verified — but never how the conditions were first
encountered. No experiments. No procedures. No origin.*

*That part was never preserved. Not because it was lost — because it was actionable. The
distinction is the volume's own: it records what the system revealed, not how it was made to
reveal it. Every observable here is mapped; every decision is traceable. None of it crosses into
causation. How to reach a state is absent by design; only what happens when one exists is
recorded.*

*In the era this volume documents, understanding came from accumulation: repeated exposure to
outcomes, until decisions became predictable — and once predictable, they no longer needed to be
explained. This was never a guide to breaking systems. It is a record of a time when the systems
under study could not hide what they were doing. And that was enough.*

---

## Sources (corpus map)

| Topic | Source |
|---|---|
| Conceptual licensing model (SMA, features, API) | `niagara-mental-model.md` §2 · `notes/02-licensing.md` |
| Classes and validation in `baja.jar` | `niagara-mental-model-bloque41.md` §41.6 |
| Native layer: `dsfspi.dll` (Mocana DSF JCE), `nverify.exe`, signature schemes | `niagara-mental-model-bloque126.md` |
| Native host ID (`getHostId0`, volume serial) | `niagara-mental-model-bloque124.md` · `niagara-mental-model-bloque125.md` |
| HostId format, `db/<hostId>/` canonical home, `Webs.license` asymmetry | `niagara-mental-model-bloque40.md` §40.4.8–40.4.9 |
| Runtime limits, virtual-point exclusion | `niagara-mental-model-bloque14.md` · `niagara-mental-model-bloque28.md` §28.13 |
| Module signing, `skipModuleValidation`, `exemptions.tes` | `niagara-mental-model-bloque18.md` · `niagara-mental-model-bloque113.md` |
| Incident B75, hardening, gaps | `niagara-mental-model-bloque75-security-incident.md` |
| Detection (SecurityDashboard, PolicySpy) | `niagara-mental-model-bloque112.md` |
| Trust stores, certificate formats, OCSP/CRL gap | `niagara-mental-model-bloque27.md` |
| Deployment diagnosis (failure modes, logs) | `analizador-licencias/01-diagnostico-despliegue.md` |
| Tampering forensics (IOC table, offline verification) | `analizador-licencias/02-deteccion-tampering.md` |
| Legitimate interop (read/validate only) | `analizador-licencias/03-interop-legitima.md` |
| Recognition map (symptom → surface → fragment → response) | `analizador-licencias/04-quickref-diagnostico-deteccion.md` |
| Incident response playbook | `niagara-n4-incident-response-playbook.md` |

*End of volume. The record closes where the boundary begins.*
