# RESEARCH-STATE — focus: jace8000 (live embedded controller, §12 dynamic)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-19** — angle: the **JACE-8000 as a
> live embedded QNX controller** (distinct from `platform-native`, which RE'd the Windows *supervisor*
> binaries). Subject = a real, commissioned JACE-8000 at `192.168.1.140` (`admin` account). This is a
> `live-install` target → **SECRETS DISCIPLINE** (cite structure/format, never a credential/passphrase/key
> VALUE; authenticate out-of-band from scratchpad; zero secrets in blocks/sources/engram).
>
> Phase = **§12 dynamic** (supervised, read-first). Live probes are driver-inline (`no·inline` is the
> COMPLIANT tier for §12 — live credentials are never handed to a sub-agent). Operator granted broad test
> authorization. Reuses the `api-access` SCRAM login tool (`sources/probes/B457-n4-login/`).
>
> **User questions driving the backlog:** what architecture the JACE has (Linux/Windows? → **QNX**), how to
> access the station, how to enter the JACE filesystem/system, how to enter the platform (and *without*
> Workbench), whether the platform protocol can be reverse-engineered to pull a copy of the station `.bog`,
> and how to recover a station when platform access is lost.
>
> **SECURITY / operator action:** the `admin` credentials were pasted in chat → **ROTATE them**. Live TLS
> posture already flagged (default expired platform cert on :5011; `ForRecoveryPurposes` self-signed cert on
> :443/:4911 regenerated 2026-08-19).

<!-- research-state.v1 -->
schema: research-state.v1
method: dynamic-live
block_scope: shared-global
covered_blocks: 458
gaps_closed: 3
known_gaps: 13
investigable_open: 9
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Coverage: 3/13 gaps closed (J1, J3, J4). Next gap: **J5** (B462).

| Priority | Gap | Detail | Status |
|---|---|---|---|
| high | J1 hardware & OS architecture | TI Sitara ARM Cortex-A8 (NPM6xx module) + QNX Neutrino RTOS + Oracle HotSpot JVM — NOT Linux/Windows | **covered B459** |
| high | J3 platform daemon (niagarad) | :3011 (HTTP) / :5011 (TLS 1.3) both 403-to-all-methods, no auth challenge; platform account ≠ station user; daemon = highest-privilege niagarad | **covered B460** |
| high | J4 accessing the station | SCRAM login → bajaux ORD navigator browses whole tree; minimal module set (no oBIX/Hx/help); Fox :4911 TLS; PlatformServices visible station-side | **covered B461** |
| medium | J3-G1 platform-protocol handshake bytes | Exact digest scheme / nonce / servlet path Workbench POSTs to the daemon — deferred to J8 RE | pending |
| high | J5 entering the JACE filesystem (NEXT B462) | The routes into the QNX tree: platform File Transfer (:5011), station file space (/file, ^/! roots), serial QNX console (micro-DEBUG USB), SSH (disabled). Tree + privilege each yields | pending |
| high | J7 station recovery WITHOUT platform access | Factory Recovery button, USB clone backup/restore (no Workbench), serial recovery, dist reinstall + station restore, passphrase-lost scenario — what each recovers/loses | pending |
| high | J8 platform-protocol RE → obtain station .bog without Workbench | The operator's explicit ask: can the platform File Transfer / Station Copier protocol (or a station-side Backup) be driven without Workbench to pull the `.bog`? Auth required, feasibility, RE path from platform-native corpus | pending |
| medium | J2 QNX filesystem layout & boot | Partition/mount tree, niagarad boot sequence, where the station & niagara live on flash, RAM-disk vs persistent, factory image | pending |
| medium | J6 platform auth, System Passphrase & at-rest secrets | Platform accounts vs station users, System Passphrase role (encrypts sensitive .bog data), where credentials live, factory defaults | pending |
| medium | J9 backup / distribution / cloning | Station backup .dist/.bog, BackupService, cloning a JACE, host-id binding as a clone blocker | pending |
| medium | J10 Host ID & licensing on QNX/ARM | getHostId on the JACE vs the Windows fold-XOR (B424), Host ID format, .license location, recovery implications | pending |
| medium | J11 live security posture | Certs (expired default platform, ForRecoveryPurposes), open/closed ports, hardening (SSH/telnet/Fox-plaintext off) — measured live | pending |

## Iteration history

| Block | Gap | Delegated? · tier | Notes |
|---|---|---|---|
| B459 | J1 | no·inline (§12 live) | architecture: QNX + ARM + JVM |
| B460 | J3 | no·inline (§12 live) | platform daemon: 403-to-all, platform≠station creds |
| B461 | J4 | no·inline (§12 live) | station access: bajaux ORD navigator, minimal modules |
