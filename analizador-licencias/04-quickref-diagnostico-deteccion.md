# 04 — Quick reference: recognizing when something is wrong

> **Purpose**: the "where to look" companion to the archival manual (`00-manual-archival-licensing.md`).
> For each decision fragment (F1–F16) it gives the recognition cues, the surface to inspect, the
> look-alikes to disambiguate, and the legitimate response. It tells you how to **detect, understand,
> and respond** — never how to cause or reproduce a state.
> Sources: `01-diagnostico-despliegue.md`, `02-deteccion-tampering.md`, and the corpus files they cite.

---

## Triage by symptom (first look)

| Symptom you see | Look at | Fragment | Deep doc |
|---|---|---|---|
| Station won't start; `LicensingException: no valid license for hostId Win-…` | Boot/daemon log, `db/<hostId>/` vs root alias | F1 | `01` §7 #1, §9.1 |
| Licenses "lost" after NIC swap / VM clone | `Sys.getHostId()`, `CLONED_FILE`, daemon log | F1, F13 | `01` §9.2 |
| Feature silently absent, no error | Exact feature string vs license attributes | F3 | `01` §7 #6 |
| Valid-looking license reads invalid | `generated` field, station + OS clocks | F4, F15 | `01` §7 #4 |
| Whole license file ignored though one feature looks fine | Offline signature verification | F5 | `02` §1, Fase B |
| Module accepted on Windows supervisor, rejected on QNX JACE | License flags, platform class | F6, F10 | `02` §3.1 |
| `LicenseLimitExceededException` on add; restore aborts mid-way | `licenseManager.pointCount`, feature limits | F7, F14 | `01` §7 #7,#11 |
| Audit empty/truncated with recent activity; unsigned module warnings | Daemon log, SecurityDashboard, audit | F8 | `02` §3.2, §5 |
| Entitlements absent after restore, license file intact | LRT subscription directories | F11 | `01` §7, `02` §1 |
| SMA alarms flap with the clock; nCloud rejects what's local-valid | Station clock vs nCloud clock | F15 | `01` §7 #9,#10 |

---

## Fragment-by-fragment

### F1 · Identity resolution
- **Recognize**: licenses present on disk, features absent; boot may fail with `no valid license
  for hostId`. Root `licenses/` are aliases of `db/<hostId>/` — if the host changed, the alias
  points at a home that no longer matches. `db/` is **not** a fallback.
- **Where to look**: `Sys.getHostId()` vs `hostId` attribute of each `.license`; boot/daemon log;
  `diff` between root alias and `db/<hostId>/` files.
- **Don't confuse with**: a tampered/exfiltrated file (same symptom — a file whose `hostId` is
  foreign but that sits in this host's `db/` is a forensic signal, `02` §3.1).
- **Respond**: re-request a license for the current `hostId`; if cloned, regenerate NRE-id first.
  Copy (don't edit) the license trees before touching anything.

### F2 · Legacy identity
- **Recognize**: AX-era license carried into N4 — incompatible; a new license request is required.
- **Where to look**: `hostId` format (`^(Qnx|Win)-[A-Z0-9-]{14,19}$`); AX files won't match the
  N4 64-bit hash shape.
- **Don't confuse with**: F1 (host change) — here the file never matches any N4 host.
- **Respond**: request N4-generation license; keep the AX artifact only as evidence/history.

### F3 · Attribute gating
- **Recognize**: feature missing **with no explicit error**; everything else in the file validates.
- **Where to look**: exact string in the consulting code (`feature.getb("Sma.Exempt", false)`)
  vs the license's attribute; Workbench Tools → License Manager; `nre -licenses`.
- **Don't confuse with**: missing license, expiry, host mismatch — all produce feature absence
  (isolate by checking that *other* features of the same file load).
- **Respond**: correct the string; document the canonical names.

### F4 · Time validity
- **Recognize**: license invalid although wall clock is inside the validity range; validity alarms
  that appear and disappear with the clock.
- **Where to look**: `generated` (must be ≤ now — anti-clock-rollback); station clock and OS clock.
- **Don't confuse with**: expiry (check `expiration` separately); **manipulation** — a `generated`
  in the future is also an IOC for a re-generated file, `02` §3.1.
- **Respond**: fix NTP/clock discipline; re-validate.

### F5 · Authenticity gate
- **Recognize**: whole file discarded even though a feature inside looks correct — validation is
  all-or-nothing per file; any of the 5 checks failing drops the entire file.
- **Where to look**: verify the `<signature>` offline against the `{vendor}.certificate` (DSA,
  DER `SEQ{INTEGER r(20B), INTEGER s(20B)}`); SecurityDashboard for module-level signatures.
- **Don't confuse with**: F1/F4 — same discard outcome, different cause; isolate check-by-check.
- **Respond**: preserve the file as evidence; verify offline (Fase B, `02`); if it fails, it was
  edited or re-signed with a non-vendor key — treat as tampering.

### F6 · Two-key gate (`skipModuleValidation`)
- **Recognize**: module validation state that differs per host license, not per module.
- **Where to look**: license flags (`skipModuleValidation`, `smDeveloperMode`,
  `license.unreleasedSoftware=true`); SecurityDashboard module statuses; `exemptions.tes`
  (each entry logs an audit event).
- **Don't confuse with**: nothing — it is a license/config state, not a failure.
- **Respond**: in production these flags are IOCs (dev features that can't be removed from the
  license; mitigation is blacklisting sysprops in the launcher); audit which modules validated
  under which license.

### F7 · Counting
- **Recognize**: `LicenseLimitExceededException` at add; backup/restore aborting at point N
  (no partial restore); `PointLimitExceeded`/`DeviceLimitExceeded`/… alarms.
- **Where to look**: spy `licenseManager.pointCount`; feature attributes `point.limit`,
  `device.limit`, `history.limit`, `schedule.limit`… (`"none"`/absent = unlimited).
- **Don't confuse with**: a counting divergence bug (F14 / `02` §3.3) vs a genuine limit hit.
- **Respond**: reduce the model or obtain a larger entitlement — there is **no grace** for counts.

### F8 · Decision trace (incident B75)
- **Recognize**: unsigned module loaded — the factory default (`niagara.moduleVerificationMode=low`)
  is fail-open; classloader warning `No code signers for entry %s in module %s`; audit
  empty/truncated with recent activity.
- **Where to look**: daemon log (OS-level, not erasable by the Baja layer — `/systemlog`,
  `/getdaemonoutput`), SecurityDashboard, PolicySpy, audit.
- **Don't confuse with**: routine unsigned development modules (check the platform class and
  effective policy first).
- **Respond**: incident playbook; collect state-based and OS-level evidence **first** (erasability
  matrix: state, policy, daemon log survive; audit/history only partially); then harden —
  verification mode, signing gate, trust anchors.

### F9 · Multi-host distributions
- **Recognize**: identical station image across hosts with different entitlement sets.
- **Where to look**: `db/<hostId>/` per-host directories; multi-host install bundles.
- **Don't confuse with**: F1 — here each host resolves its own home correctly by design.
- **Respond**: provision per host (inbox import or `BLicenseService` fleet provisioning).

### F10 · Platform asymmetry
- **Recognize**: the same artifact accepted on a Windows supervisor and rejected on a QNX JACE.
- **Where to look**: `Webs.license` (has `skipModuleValidation`) vs OEM field license (doesn't);
  platform class of the host.
- **Don't confuse with**: F6 — same gate, viewed across deployments.
- **Respond**: treat QNX field behavior as the lockdown baseline; never cite supervisor acceptance
  as proof a module is field-safe.

### F11 · Backup/restore
- **Recognize**: entitlements absent after restore although the license file is present and intact.
- **Where to look**: the three LRT directories (subscription / certificate / license) — a partial
  backup omits some and corrupts subscription state.
- **Don't confuse with**: F1 (host change) — the host didn't change, the state was truncated.
- **Respond**: restore all three directories; make full-state backup a standing procedure.

### F12 · Mixed-license sites
- **Recognize**: within one station, some modules validated and others not.
- **Where to look**: which license is effective per vendor (License Manager view); the
  cross-license matrix (`skipModuleValidation` in one license, not the other).
- **Don't confuse with**: F10 — the divergence is per-module within one host, not per platform.
- **Respond**: unify the license set; document which modules depend on which license's features.

### F13 · Cloned identities
- **Recognize**: duplicated host identity across a fleet; `CLONED_FILE` collision state; daemon
  log `ERROR: Host Id cannot be found/generated.` or `>>> hostid.debug >>>` markers.
- **Where to look**: daemon log; compare `Sys.getHostId()` across hosts.
- **Don't confuse with**: F1 — the symptom is identity collision, not a single host losing its file.
- **Respond**: regenerate NRE-id per clone; re-provision licenses for the new identities.

### F14 · Topology counting
- **Recognize**: the same station under a supervisor reports different limit consumption than
  standalone; virtual points never change the count.
- **Where to look**: `pointCount` on the subordinate vs the supervisor; `BVirtualComponentSpace`.
- **Don't confuse with**: F7 — here the count is *correct per design* (counts at origin).
- **Respond**: read the counter of the host that owns the points; size entitlements accordingly.

### F15 · Clock drift
- **Recognize**: valid license reads invalid; SMA alarms flapping; nCloud rejects what the local
  clock says is valid.
- **Where to look**: station and OS clocks; SMA checks use local `Clock.time()`; nCloud uses its
  own clock. Anti-rollback: `generated` check and the invalid-time floor.
- **Don't confuse with**: expiry (F4) — distinguish by checking `expiration` vs clock state.
- **Respond**: NTP discipline. Note SMA enforcement is soft (alarms, does not stop the module);
  feature expiry is hard.

### F16 · Before/after hardening
- **Recognize**: the identical artifact class flips from accepted to rejected between two states
  of the same platform — the variable was configuration/trust state, not the artifact.
- **Where to look**: `niagara.moduleVerificationMode`, trust anchors, effective policy (PolicySpy),
  `exemptions.tes` contents.
- **Don't confuse with**: a platform change/upgrade — verify the hardening checklist (trust
  anchors, no residual entries, empty `exemptions.tes`).
- **Respond**: this flip is the desired outcome; verify and keep the hardened state.

---

## Evidence collection (before anything else)

- Copy — never edit — `/security/licenses/` and `/security/licenses/db/<hostId>/` (all `.license`
  and `.certificate`), and the four runtime trust stores.
- Extract the daemon log (`/systemlog`, `/getdaemonoutput`, `console.log`) — OS-level, durable.
- Capture SecurityDashboard and PolicySpy.
- Copy station backup and audit (`~audits`).
- Record `Sys.getHostId()` and the clocks (station + OS) for anti-clock checks.
- Collect order matters: state-based and OS-level surfaces survive; audit/history are only
  partially erasable. (`02` §4–§5)

*This sheet records recognition, not causation. It tells you what happened and where to look —
never how to reach the state.*
