# Niagara N4 — LICENSED vs UNLICENSED install diff — Research State

> Focus: compare a **LICENSED** install against an **UNLICENSED** one, on disk, with `diffoscope` + SHA-256
> manifests, to document WHAT a Niagara license actually changes on the filesystem. READ-ONLY over both
> installs. `live-install` artifacts → **SECRETS DISCIPLINE**: cite license/cert STRUCTURE (HostId format,
> DSA/RSA sig layout, feature names) — never a private secret value (the `.license`/`.certificate` files
> carry only public keys + signatures by design, per [B126]). Corpus language: ENGLISH.
>
> **Compared pair (user-selected 2026-08-07):**
> - **A = LICENSED**: `/mnt/c/Honeywell/OptimizerSupervisor-N4.14.0.162` — Honeywell OEM, N4.14.0.162,
>   3 `.license` (Honeywell/HoneywellCentraLine/Webs), 973 modules. The install already reverse-engineered
>   in the `platform-native` / base corpus (B124-B385, B2, B126).
> - **B = UNLICENSED**: `/mnt/c/Users/equipo/Downloads/Tridium_EMEA_N4_Supervisor-4.15.3.28.2` — Tridium base
>   EMEA supervisor, N4.15.3.28.2, 0 `.license`, 776 modules.
>
> **THREE-AXIS CAVEAT (load-bearing):** this pair differs on THREE axes at once — (1) **license state**
> (licensed vs unlicensed, the target axis), (2) **version** (4.14.0.162 → 4.15.3.28.2), (3) **vendor**
> (Honeywell OEM rebrand → Tridium base). Every diff finding MUST be attributed to an axis; only
> license-axis findings answer the focus question. A same-name-different-bytes file is most likely
> version-driven; a whole subtree present in only one is likely vendor-driven; `security/licenses` +
> `security/certificates` + license-DB + feature-gated config are the license-axis signal.
> Tools: `diffoscope 327` (kit venv `/home/cristian/.local/share/research-sdd-tools/venv`), SHA-256 manifests,
> `japicmp` (when a same-name jar differs and a bytecode-level API diff is wanted).
> Mirrored in engram: `research/niagara/license-diff/gaps`, `research/niagara/license-diff/progress`.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 438
gaps_closed: 6
known_gaps: 6
investigable_open: 0
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage

- **Focus block artifacts**: 7 — B386-B391 + corrective addendum B442.
- **Coverage metric**: 6 / 6 seeded gaps; B442 is a correction/addendum, not a new gap.
- **Method**: SHA-256 manifest of both trees → classify changed set (only-in-A / only-in-B / same-path-diff)
  → `diffoscope` ONLY on the license-axis subset (never diffoscope 30 GB of mostly-identical product bytes).

## Gap-backlog (prioritized)

| Priority | Gap | Artifact / source | Status |
|---|---|---|---|
| — | **L1** Inventory + classification + license-axis answer | manifests A,B,iC | ✅ B386; scope corrected by B442 |
| — | **L2** Security artifact structure | A security/ | ✅ absorbed by B386 |
| — | **L3** Module inventory delta | A/B modules/ | ✅ B388 |
| — | **L4** Native binary diff | A/B bin/ | ✅ B389 |
| — | **L5** Config/defaults diff | A/iC defaults/ | ✅ B389 |
| — | **L6** Runtime feature gates + signature chain | corpus + license files | ✅ B387 |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps |
|---|---|---|---|---|---|
| 1 | 2026-08-07 | L1 inventory + license-axis answer | B386 | no · inline | 0 new. B(4.15 EMEA)=INSTALLER not install (908 vs 66559); re-paired vs iC-Niagara-4.13.2.18 (installed unlicensed). LICENSE AXIS: unlicensed has NO security/; a license materializes security/licenses+db/<HostId>+certificates+truststore.jks. diffoscope 327 validated. |
| 2 | 2026-08-07 | L6 runtime feature-gate map | B387 | yes · sonnet (gate-site sweep) + inline verify | 0 new. 178 grants (27 Hon+1 CL+150 Webs demo, developer skipModuleValidation=true). API LicenseManager.get/checkFeature; unlicensed→limits MAX_VALUE (UNCAPPED not disabled); heap.limit exceed→System.exit(-3); SMA=checkModuleReleaseDate at module load; signature-verified via hardcoded DSA+ECDSA master keys — CONFIRMS B126 §126.6 inference (native isFeaturePresent=text-match, Java=real verify). |
| 3 | 2026-08-07 | L3 module delta | B388 | no · inline | 0 new. A=684 B=574 modules; only-A 152 = Honeywell OEM + user/third-party; only-B 42 = 4.15 base; common differences are version-axis. |
| 4 | 2026-08-07 | L4+L5 bin/config delta | B389 | no · inline | 0 new. Native launcher core stable across 4.13→4.14; defaults delta is version/vendor. Focus complete 6/6. |
| 5 | 2026-08-07 | addendum: japicmp 4.14→4.15 | B390 | no · inline | 0 new. Additive, binary-compatible version-axis result. |
| 6 | 2026-08-07 | addendum: 4.15 BACnet additions | B391 | no · inline | 0 new. Protocol/version axis, not license. |
| 7 | 2026-08-11 | corrective addendum: 4.10 live state + principal JAR ownership | B442 | no · inline (§12 read-only live probe) | 0 new. `nre -licenses` confirms none/none while baseline `security/` exists; corrects B386's cross-version wording. 800-JAR census maps seven runtime boundaries + signing plugin. Focus remains STOPPED 6/6. |

## Stop control

- Bootstrapped 2026-08-07. **ALL 6 GAPS COVERED (L1 B386, L6 B387, L3 B388, L4+L5 B389, L2 absorbed B386). investigable=0. STOPPED.** Corrected by B442: validated HostId-bound `.license` records and loaded runtime features define license state; parent `security/` presence is version-dependent. Modules/bin/config remain vendor/version/user axes (B388/B389).
