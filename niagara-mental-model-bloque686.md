# B686 — JACE_UMBRELLA NiagaraNetwork (SC2): the field controller lists no supervisor and manages no subordinate — but that does NOT prove standalone (framework-semantic refine)

> Focus: **jace-station-config** · Gap **SC2** (NiagaraNetwork deployed config — is this JACE subordinate to
> the supervisor?). Sources: `config.bog` file.xml `/Drivers/NiagaraNetwork` subtree (L802–870), extracted
> READ-ONLY from the JACE-8000 boot microSD. Redacted evidence:
> `sources/probes/B685-jace-station-config/niagaranetwork-subtree.txt`.
> **SECRETS DISCIPLINE (live-install):** structure only; credential values masked. Marker `[CERT-hw]` (SD
> artifact, same lineage as `jace8000-sd`).

## 686.1 — Verdict, stated precisely (framework-semantic REFINE)

[CERT-hw] `<p n="NiagaraNetwork" m="nd=niagaraDriver" t="nd:NiagaraNetwork">` (L803). Independently re-measured
(RE-MEASURE A DRAMATIC NEGATIVE): `grep -c 'BNiagaraStation|nd:NiagaraStation'` = **0**. The only station nodes
are `localStation` (`nd:LocalSysDefStation`, L810) and `sysDefProvider/JACE_UMBRELLA` (`nd:ProviderStation`,
flag `f="h"`, L829) — **both self-descriptors of this same station**, not remote peers. `exportTags` = 0.

The SC2 sweep returned a flat verdict "STANDALONE." **DE-ESCALATION / REFINE** — that overstates what the JACE
config can prove. In Niagara's supervisor↔subordinate model (focus `niagara-network-supervisor` [Block 414]–
[Block 420]: the device-proxy `BNiagaraStation` + DeviceExts live on the SUPERVISOR pointing DOWN at the
subordinate), a subordinate JACE does **not** necessarily list its supervisor — the supervisor holds the
inbound link. So the determinable facts are narrower than "standalone":

- **This JACE is NOT acting as a supervisor** — it manages zero remote `BNiagaraStation` device nodes and holds
  zero `SupervisorLicenses` (686.3). [CERT-hw]
- **This JACE lists no OUTBOUND NiagaraNetwork station link** — no supervisor node, no peer node. [CERT-hw]
- **Whether a supervisor manages it INBOUND is NOT determinable from this config** — that link, if it exists,
  lives in the supervisor's own NiagaraNetwork, not here. [INFER, framework model B420]

So: from its own config the JACE is a leaf that neither supervises nor is shown joined upward. It is reached
by whatever connects IN over foxs:4911 (686.2) — Workbench, or a supervisor reaching down — which this file
cannot distinguish. Confirming an inbound supervisor would need the supervisor's config or a live Fox
station-inventory (requires-execution).

## 686.2 — NiagaraNetwork subtree map

[CERT-hw]

| Node | type | configured content | L |
|---|---|---|---|
| NiagaraNetwork | nd:NiagaraNetwork | alarmSourceInfo, PingMonitor | 803 |
| localStation | nd:LocalSysDefStation | stationName JACE_UMBRELLA, foxPort 1911 / foxsPort 4911, services NameList, PersistTask (lastSuccess 2026-08-19) | 810 |
| sysDefProvider/JACE_UMBRELLA | nd:ProviderStation (f="h") | self-mirror: same name + ports; 4× nd:SysDefVersion | 829 |
| tuningPolicies | nd:NiagaraTuningPolicyMap | defaultPolicy (empty) | 843 |
| historyPolicies | d:HistoryNetworkExt | onDemandPollScheduler; defaultRule capacity 0:0, fullPolicy=roll | 847 |
| workers | nd:CyclicThreadPoolWorker | maxThreads=max | 855 |
| virtualPolicies | nd:NiagaraVirtualNetworkExt | DefaultNiagaraVirtualCache (empty) | 858 |
| ProvisioningNwExt | pn:ProvisioningNiagaraNetworkExt | SupervisorLicenses (empty) + StationPollScheduler (empty) | 864 |

No alarm/points/schedules network-ext on the container; the policy containers are present as skeletons but hold
no rules (default-provisioned). The `nd:BogProvider` + 4× `nd:SysDefVersion` are the station's own system-
definition versioning, self-referential.

## 686.3 — ProvisioningNiagaraNetworkExt: capable but never promoted

[CERT-hw] `pn:ProvisioningNiagaraNetworkExt` (L865) is wired (module `pn=provisioningNiagara`), but both its
functional children are empty: `licenses` = `pn:SupervisorLicenses` empty body (L866); `pollScheduler` =
`pn:StationPollScheduler` empty body (L868). No provisioning robots, no jobs. The provisioning subsystem
internals are REMITTANCE (focus `provisioning` [Block 567]–[Block 576]); the finding here is DEPLOYMENT state:
this station is provisioning-EXTENSION-capable but has never been promoted into a managed fleet — consistent
with 686.1 (not acting as a supervisor) and with the template-default posture (B685: provisioned from
`NewJACEProvisioningStation.ntpl`).

## 686.4 — How this JACE is actually reached

[CERT-hw]+[INFER] The localStation exposes foxPort 1911 and foxsPort 4911 (L813–814); the FoxService enforces
`foxsOnly=true` + TLS 1.3 (B685 §685.3, L210–216), cert alias `foxsCert="default"` (L217) with a
`certAliasAndPassword` slot (L218, `purposeId=SERVER_CERT`, value MASKED). So the ONLY station-protocol door is
inbound foxs:4911. This is exactly the door B473 used to pull this same `config.bog` LIVE over Fox with station
admin (focus `jace8000`), and the door a supervisor would use to reach down. Management is therefore inbound
(Workbench / supervisor / the operator's own Fox client [Block 471]), not via any outbound link this station
holds. [INFER: the config shows the listener + cert; it does not record who dials in.]

## Connections

- Supervisor↔subordinate device-proxy model (BNiagaraStation on the supervisor side) → focus
  `niagara-network-supervisor` [Block 414]–[Block 420]. Provisioning subsystem → focus `provisioning`
  [Block 567]. This same config.bog pulled live over Fox → [Block 473]; the Fox client + TLS-1.3-only →
  [Block 471]/[Block 474]. Skeleton + FoxService hardening → [Block 685] (this focus, SC1).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | 0 remote BNiagaraStation nodes (only self localStation + ProviderStation) | [CERT-hw] | grep -c = 0; L810/L829 | re-measured |
| 2 | not acting as supervisor: SupervisorLicenses empty, 0 subordinates | [CERT-hw] | L866 | grep-confirmed |
| 3 | no exportTags / tag-join | [CERT-hw] | grep -c = 0 | grep-confirmed |
| 4 | localStation foxPort 1911 / foxsPort 4911; foxsOnly TLS1.3, cert "default" | [CERT-hw] | L813/L814/L217 | grep-confirmed |
| 5 | ProvisioningNwExt wired but functionally empty | [CERT-hw] | L865/L866/L868 | grep-confirmed |
| 6 | "standalone" overstates it — inbound supervisor not determinable from JACE config | [INFER] | framework model B420 + absence of inbound record | reasoned (REFINE) |

**Tally:** [CERT-hw] ×5 · [INFER] ×1. Ratio 0.2. Block TYPE = **EVIDENCE**. The dramatic negative (0 remote
stations) was independently re-measured; the sub-agent's flat "standalone" was REFINED down to what the config
actually proves (a DE-ESCALATION, §11). 5/5 load-bearing citations grep-confirmed. Evidence file secret-scan:
clean.

## Open gaps (this focus)

SC2 CLOSED. Next: **SC3** (NrioNetwork — the physical field IO: the `io34_1_2` Nrio34Module + relay output
seen in B685 §685.4, L873–931). One requires-execution follow-up NOTED (not a new backlog row yet): confirming
an inbound supervisor link would need the supervisor's config or a live Fox inventory — deferred to the
existing live-access wall (focus `jace8000` J-series).
