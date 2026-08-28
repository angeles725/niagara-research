# Block 570 — Fleet onboarding: two discovery paths (DHCP for edge / Niagara-network scan), "privileged" bootstrap steps that run over the platform daemon and handle out-of-box DEFAULT credentials, and the reciprocal-connection step that makes a new subordinate dial back to the supervisor

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV4 — the bootstrap/discovery flow: how a brand-new controller is found,
initialized, and joined)
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 10-class `bootstrap` package + the privileged-step handling in
`BForEachStationStage`. SECRETS DISCIPLINE: cite the credential-handling STRUCTURE, never a value.
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/bootstrap/
  {BAbstractDiscoveryStep,BDhcpDiscoveryStep,BNiagaraNetworkDiscoveryStep,BEnableBootstrapStep,BRenameStationStep,
  BSetupReciprocalConnectionStep,BDeviceBootstrapExt}.java`.
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/javax/baja/provisioningNiagara/
  BForEachStationStage.java:74-101`.

**Scope**: the onboarding path for a new controller. Does NOT re-open the batch engine ([Block 567]) or the
daemon protocol ([Block 460]) — builds on both.

---

## 570.1 Two discovery paths [CERT]

`BAbstractDiscoveryStep extends BNetworkJobStep implements BIPrivilegedDeviceJobStep` `[CERT] :25` is the base.
Two concrete finders `[CERT]`:
- `BDhcpDiscoveryStep` `[CERT] :22` — finds edge controllers via **DHCP** (records
  `DhcpdLeaseSettingsDeviceInfo`): the supervisor acts as/reads a DHCP lease source, the Edge-10-startup path.
- `BNiagaraNetworkDiscoveryStep` `[CERT] :30` — scans the network for Niagara stations (`useHostname` toggle;
  `doRun` over a `BDeviceNetwork`; a `JobCompletionHandler extends Subscriber`; records
  `LearnStationDeviceInfo`).

Both produce `DeviceInfo` records that seed the join.

## 570.2 "Privileged" = runs over the platform daemon and handles DEFAULT out-of-box credentials [CERT]

The bootstrap steps — discovery, `BEnableBootstrapStep`, `BRenameStationStep`, set-system-passphrase — all
implement the marker `BIPrivilegedDeviceJobStep` `[CERT]` (from `batchJob/driver`). The meaning is in the
executor `BForEachStationStage` `[CERT] :74-101`:
```java
if (step instanceof BIPrivilegedDeviceJobStep) {
   try (ProvisioningConnectionUtil connectionUtil = new ProvisioningConnectionUtil(device, null)) {
      BDaemonSession daemonSession = connectionUtil.getDaemonSession();     // PLATFORM daemon, not station Fox
      ... require daemon Version >= 4.4 ...
      XElem authInfo = connectionUtil.getDaemonResponse(new AuthenticationInfoMessage(false));
      if (authInfo.elem("auth").elem("user").getb("default", false)) {      // device still at DEFAULT creds
         XElem systemPasswordElem = connectionUtil.getDaemonResponse(new SystemPasswordMessage());  // fetch system pw
      }
   }
}
```
So a **privileged** step operates through the **platform-daemon** connection (`ProvisioningConnectionUtil` →
`BDaemonSession`), NOT the station Fox session — because at bootstrap the station is not yet a normal
credentialed member. It explicitly detects a device still on **factory-DEFAULT auth**
(`auth.user.default == true`) and retrieves the `SystemPasswordMessage` to drive it out of the default state.
This is the one place provisioning deliberately handles out-of-box credentials — the most sensitive moment in
the fleet lifecycle. It requires daemon **≥ 4.4** `[CERT]`.

## 570.3 The reciprocal connection: the new subordinate dials back [CERT]

`BSetupReciprocalConnectionStep extends BDeviceJobStep` `[CERT] :56` carries `(username, password:BPassword,
address:BOrd)` `[CERT] :57-60`. Its `doRun` `[CERT] :41-49` opens a session to the new device, resolves the
device's OWN station root (`BOrd.make("station:|slot:/").get(session)`), fetches its `BNiagaraNetwork`s
(`getNiagaraNetworks(remoteStation)`), and configures a connection back to the **supervisor** (the
`address`/`username`/`password`). So after supervisor→subordinate is established (the device proxy), this step
makes subordinate→supervisor — a **bidirectional** link. The credentials the subordinate uses to reach the
supervisor are pushed here (a `BPassword`, [Block 562] AC4).

## 570.4 The onboarding sequence [CERT-synthesis]

Assembled: **discover** (DHCP or Niagara scan → `DeviceInfo`) → **enable bootstrap** (`BEnableBootstrapStep`,
privileged) → **initialize** (rename via `BRenameStationStep`, set system passphrase — privileged, over the
daemon, from factory defaults) → **setup reciprocal connection** (subordinate dials back) → device is a managed
fleet member with a two-way link. `BDeviceBootstrapExt` (`BDeviceExt` + `BIMixIn`) `[CERT] :25` marks a device
in this bootstrap state. The security-critical property: the privileged phase runs over the platform daemon and
can act on a device still at factory-default credentials — powerful by necessity, and the reason discovery/
bootstrap should run on a trusted provisioning network.

## 570.5 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | Two discovery steps: BDhcpDiscoveryStep (DHCP/edge) + BNiagaraNetworkDiscoveryStep (network scan), both extend BAbstractDiscoveryStep (BNetworkJobStep) | [CERT] | bootstrap/*.java:22,25,30 | token-checked ✓ |
| 2 | Bootstrap steps implement BIPrivilegedDeviceJobStep marker | [CERT] | BAbstractDiscoveryStep.java:25; BEnableBootstrapStep.java:20; BRenameStationStep.java:22 | token-checked ✓ |
| 3 | Privileged handling: ProvisioningConnectionUtil→BDaemonSession, requires daemon ≥4.4, reads AuthenticationInfoMessage, fetches SystemPasswordMessage when auth.user.default=true | [CERT] | BForEachStationStage.java:74-101 | token-checked ✓ |
| 4 | BSetupReciprocalConnectionStep (username/password:BPassword/address) configures subordinate's BNiagaraNetwork to dial back to supervisor | [CERT] | BSetupReciprocalConnectionStep.java:41-60 | token-checked ✓ |
| 5 | BDeviceBootstrapExt = BDeviceExt + BIMixIn marks bootstrap state | [CERT] | BDeviceBootstrapExt.java:25 | token-checked ✓ |
| 6 | Onboarding sequence discover→enable→init(privileged/daemon/default-creds)→reciprocal | [CERT-synthesis] | rows 1-5 | reasoned ✓ |

**Marker tally**: [CERT] ×5 · [CERT-synthesis] ×1 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 5 of 6
rows token-verified inline.

## Connections

- **[Block 567]** (PV1) — the batch engine; `BForEachStationStage` is the Niagara-network per-station stage that
  special-cases privileged steps.
- **[Block 460]** — the platform daemon; privileged steps run over `BDaemonSession`, including at factory defaults.
- **[Block 568]** (PV2) — the provisioning channel; discovery/bootstrap complements the control channel.
- **[Block 562]** (AC4) — the reciprocal `password` is a `BPassword`.
- **[Block 466]** — the system passphrase this phase sets on a fresh device.

## Open gaps (this block)

- The DHCP discovery wire (how `DhcpdLeaseSettingsDeviceInfo` is populated — supervisor-run dhcpd vs read) is
  named, not fully traced — Edge-10-startup territory (`docProvisioning`/`Edge10Startup`), open on demand. Focus
  continues at PV5 (async-action protocol).
