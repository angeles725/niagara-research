# Block 568 — `BNiagaraProvisioningChannel` (the "niagaraProv" Fox channel): the supervisor↔subordinate provisioning control plane — ~20 circuit commands over installables, platform-daemon station lifecycle, licenses, and filesystem — and it does NOT bypass platform auth (it delegates to a separately-authenticated daemon session)

**Session**: 2026-08-28
**Focus**: `provisioning` (gap PV2 — the custom Fox channel carrying provisioning tasks; distinct from the
backup-over-Fox circuit [Block 472])
**Distribution**: Honeywell OptimizerSupervisor-N4.14.0.162
**Method**: READ-ONLY, inline bounded read of the 1452-line channel + its dispatch table + the daemon-surrogate
path + the BPlatformConnection credential model + the BFoxChannel base gate. An early "auth bypass" hypothesis
was RAISED and REFUTED by the surrogate evidence (§568.4).
**Primary sources** `[CERT]`:
- `organized/provisioningNiagara/provisioningNiagara-wb/vineflower/com/tridium/provisioningNiagara/
  {BNiagaraProvisioningChannel,BPlatformConnection,BProvisioningNiagaraNetworkExt}.java`.
- `organized/fox/fox-rt/vineflower/com/tridium/fox/sys/BFoxChannel.java:298-313`.

**Scope**: the provisioning control channel — its command surface, the two-credential model, and its gating.
Complements [Block 472] (backup circuit) and [Block 39 §39.5] (BPlatformConnection per-station, REMITTANCE). Does
NOT re-open the Fox channel API ([Block 513]) or the platform daemon protocol ([Block 460]) — connects.

---

## 568.1 A registered `BFoxChannel` named "niagaraProv" [CERT]

`public class BNiagaraProvisioningChannel extends BFoxChannel` `[CERT] :71`. It is registered on the Fox
connection to each subordinate by the network ext:
`channelRegistry.add("niagaraProv", new BNiagaraProvisioningChannel())` `[CERT]
BProvisioningNiagaraNetworkExt.java:256`, and retrieved for use via
`session.getConnection().getChannels().get("niagaraProv", BNiagaraProvisioningChannel.TYPE)` `[CERT] :480-482`.
So it rides the SAME authenticated Fox connection the supervisor already holds to the subordinate ([Block 513]
`BFoxChannelRegistry`) — a dedicated named channel, parallel to the `backup` channel of [Block 472].

## 568.2 ~20 circuit commands across four domains [CERT]

`circuitOpened(FoxCircuit)` `[CERT] :95` dispatches on `circuit.command` to a large surface `[CERT]`:
- **Installables / software**: `findInstallable`, `getInstallableByPath`, `findInstallables`, `getInstallables`,
  `registerInstallable`, `registerStationInstallable`.
- **Platform-daemon station lifecycle**: `startStation`, `stopStation`, `killStation`, `saveStation`,
  `getStationOutput` (a chunked console stream, `ChunkedInputStream`/`ChunkedOutputStream`).
- **Licenses / certificates**: `getUpdatedLicenses`, `getLicenseSummaries`, `getCertificateSummaries`,
  `importLicenseFile`, `importLicenseArchiveFile`, `getLicensedHostIds`, `getLicenses`, `exportLicenses` (portal
  license flow, `PortalLicenseUtil`, `BLicenseArchiveFile`).
- **Filesystem**: `getFilesystemAttributes` (`BFilesystemAttributes` on the daemon filesystem).

Each command has a client method (String args, opens a circuit) and a server handler (`FoxCircuit` arg) — the
standard [Block 513] request/circuit split. This is the provisioning CONTROL PLANE: everything the supervisor
needs to inventory software, drive station lifecycle, sync licenses, and inspect the filesystem of a subordinate.

## 568.3 The two-credential model — Fox session + platform-daemon session [CERT]

Station-lifecycle commands do NOT run over the Fox session's authority alone. `getStationSurrogate(request)`
`[CERT] :397-405`:
```java
BNiagaraStation s = getStation(request);
BPlatformConnection conn = (BPlatformConnection) s.getMixIn(BPlatformConnection.TYPE);
return BStationSurrogate.make(conn.getDaemonSession(), s.getStationName());
```
`BPlatformConnection` is a mixin on the `BNiagaraStation` device-proxy that holds its OWN
`credentials` (`BUsernameAndPassword`, `[CERT] BPlatformConnection.java:121`) and maintains an authenticated
`BDaemonSession` / `BDaemonSecureSession` (platcrypto, `DaemonSSLRequiredException` ⇒ TLS-required) `[CERT]
:7-13,131`. So each subordinate carries **two independent authenticated contexts**: the Fox STATION session
(SCRAM, [Block 457]) and a PLATFORM-DAEMON session (platform username/password over TLS, [Block 460]). Lifecycle
ops flow through the daemon session; installable/license queries flow over Fox.

## 568.4 It does NOT bypass platform auth [CERT] — hypothesis refuted

A tempting reading is that the channel tunnels daemon operations (start/stop/**kill** a station!) through the
station Fox connection, sidestepping the platform login. **The evidence refutes that.** The daemon operations are
delegated to a `BStationSurrogate` built from `conn.getDaemonSession()` (§568.3) — a session that was itself
authenticated with platform credentials over TLS. The channel is the SUPERVISOR-side dispatcher; the actual
daemon action rides an independently-authenticated daemon session. So killing a subordinate station via
provisioning still requires valid platform-daemon credentials for that station (held in `BPlatformConnection`),
not merely a Fox login.

## 568.5 Gating: standard Fox `BPermissions`, no inline check needed [CERT]

A whole-file grep for `checkPermission`/`getPermissions`/`PermissionException`/`isSuperUser` in the channel
returns **nothing** — there is no per-command permission check in `BNiagaraProvisioningChannel`. That is not a
gap: the `BFoxChannel` base gates at the object level via the session context —
`getPermissionsFor(object) → ((BIProtected)object).getPermissions(getSessionContext())` `[CERT]
BFoxChannel.java:298-313`, and encoding is permission-filtered (`encodePropertyValue(..., BPermissions, ...)`).
So access is controlled by (a) whether the Fox user's session may open this channel/reach these objects, and
(b) whether the per-subordinate platform-daemon credentials are present and valid. The security posture is
therefore "supervisor holds two valid credential sets per subordinate", consistent with the [Block 392]
cross-cut (strong control over who-may-act), not an unauthenticated control plane.

## 568.6 Self-verify

| # | Claim | Marker | Citation | Verdict |
|---|-------|--------|----------|---------|
| 1 | BNiagaraProvisioningChannel extends BFoxChannel; registered as "niagaraProv" on the Fox connection | [CERT] | :71; BProvisioningNiagaraNetworkExt.java:256,480-482 | token-checked ✓ |
| 2 | circuitOpened dispatches ~20 commands: installables, station lifecycle, licenses, filesystem | [CERT] | :95-160 | token-checked ✓ |
| 3 | Station lifecycle (start/stop/kill/save) + chunked console stream via daemon | [CERT] | :197-312 | token-checked ✓ |
| 4 | getStationSurrogate uses BPlatformConnection.getDaemonSession() (separate authenticated daemon session) | [CERT] | :397-405 | token-checked ✓ |
| 5 | BPlatformConnection holds own credentials (BUsernameAndPassword) + BDaemonSecureSession, TLS-required | [CERT] | BPlatformConnection.java:7-13,121,131 | token-checked ✓ |
| 6 | No inline permission check in channel; gated by BFoxChannel.getPermissionsFor (BIProtected/BPermissions) | [CERT] | grep(∅) + BFoxChannel.java:298-313 | token-checked ✓ |
| 7 | Does NOT bypass platform auth — daemon ops require valid platform creds (hypothesis raised + refuted) | [CERT] | rows 4-5 | reasoned ✓ |

**Marker tally**: [CERT] ×7 · [INFER] ×0. Block TYPE = EVIDENCE (decompilation). 6 of 7 rows token-verified
inline; row 7 is a refuted hypothesis (evidence-backed).

## Connections

- **[Block 513]** — the Fox channel/circuit API and `BFoxChannelRegistry` this channel uses.
- **[Block 472]** — the sibling `backup` channel (one circuit); this is the multi-command control channel.
- **[Block 460]** — the platform daemon (:3011/:5011); `BPlatformConnection` holds the authenticated session to it.
- **[Block 39 §39.5]** — `BPlatformConnection`/`BPlatformWorker` per-station (REMITTANCE); PV2 adds the channel
  command surface + the two-credential model.
- **[Block 392]** — strong control over "who may act"; two credential sets per subordinate is an instance.

## Open gaps (this block)

- The exact Fox permission bit required to open "niagaraProv" (analogous to backup's bit 48, [Block 475]) is not
  pinned here — a requires-execution / deeper-gate child gap. Focus continues at PV3 (software distribution
  engine).
