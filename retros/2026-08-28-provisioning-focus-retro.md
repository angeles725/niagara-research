<!-- review-status: pending -->
<!-- kit-retro: include -->

# §18 Retrospective — provisioning focus (2026-08-28)

**Run**: niagara-research, focus `provisioning`, 2026-08-28
**Blocks written**: B567–B576 (10 investigable gaps PV1–PV10)
**Coverage**: 10/10 investigable gaps closed; 0 requires-execution
**Driver**: self-paced /research-sdd (Opus), AUDIT-FIRST seed delegated to sonnet, verified inline

---

## Summary

Clean run on a subsystem that was heavily REMITTANCE to B39 — the AUDIT-FIRST sweep correctly carved the 10
genuinely-uncovered surfaces from the already-documented 46-step catalog. The security-relevant gaps (PV2
channel, PV6 robot, PV8 credentials) were the highest value. One security hypothesis was raised and refuted with
evidence (PV2). One recurring sweep-count error reappeared (PV10). Two kit observations, one reinforcing D1 from
the access-control retro.

---

## Delta proposals

### D1 (reinforces access-control-retro D1) — sweep class-count for a whole JAR ≠ the target PACKAGE (NEW, HIGH)

**What happened.** The sweep reported the `-ux` layer as "~48 classes with 40+ `BUx*Factory` step builders"
(PV10). Measured: the `com.tridium.provisioningNiagara.ux` PACKAGE is **3 classes** (RpcUtil + CssResource +
JsBuild); there is **no `*Factory` class** at all. The sweep had counted the entire `-ux` JAR (which includes
other packages) and invented a "step-builder factory" pattern that does not exist. Combined with the
access-control retro's 64→256 and 8→10, that is **three wrong sweep numbers in one run**.

**Proposed delta.** Same as access-control D1, plus a specific clause: when a sweep reports a class COUNT, it
must state the exact SCOPE of the count (a package path vs a whole JAR/module) — "N classes" is meaningless
without the boundary. §13 should require `<count> in <exact package/dir>` form, and any architectural pattern the
sweep names ("factory", "builder") must cite one concrete file or be dropped.

### D2 — "specialization of a generic module" gaps should point the sweep at the GENERIC module too (NEW, MEDIUM)

**What happened.** Three provisioning gaps turned out to be thin specializations of reusable modules the corpus
had never opened: PV1 → `batchJob` (generic device-network batch engine), PV7 → `template` (generic templating).
The provisioning blocks documented the specialization and named the generic module as a child gap, but the
AUDIT-FIRST sweep had framed them as provisioning-internal, so the genuinely-reusable substrate (`batchJob`,
`template`) surfaced only during the block, not the seed. `batchJob` and `template` are now un-opened
candidate focuses that the sweep could have flagged up front.

**Proposed delta.** The AUDIT-FIRST sweep prompt should ask: for each gap, "is this a specialization of a
generic/base module, and if so is that base module itself covered in the corpus?" When the base is uncovered,
the sweep should surface it as a SEPARATE candidate focus in its return, not fold it silently into the specialized
gap. This would have named `batchJob`/`template` as focuses on day one.

---

## What went well (keep)

- **PV2 security hypothesis discipline.** I raised "the niagaraProv channel tunnels daemon ops to bypass platform
  auth" and then REFUTED it with the `getStationSurrogate` → `BPlatformConnection.getDaemonSession()` evidence
  (separate authenticated daemon session). Raising-then-refuting a vuln with code beats both silent omission and
  an unverified alarm — this is the bar for security claims and should stay the norm.
- REMITTANCE discipline held: the run never re-derived B39's 46-step catalog, B472's backup wire, or B511's BJob
  base — each was cited and deepened, not repeated.
- Security-relevant findings (robot escape hatch, two-credential model, unrestricted-but-self-gating RPCs) were
  each tied back to the B392 cross-cut rather than reported in isolation.

---

## Child gaps surfaced (named, out of scope)

- `template` module engine (`.ntpl` format, parameter binding, `BTemplateService`) — candidate NEW focus (PV7).
- `batchJob` generic engine internals beyond the driver sub-framework — mostly covered by PV1, low residue.
- niagaraProv Fox open-permission bit (analogous to backup bit 48) — requires-execution (PV2).
- Portal license wire (`PortalLicenseUtil` HTTP to the Tridium licensing server) — external-service / `[CERT-web]` (PV9).
- DHCP discovery wire (`DhcpdLeaseSettingsDeviceInfo`, Edge-10 startup) — `docProvisioning`/Edge10Startup (PV4).
