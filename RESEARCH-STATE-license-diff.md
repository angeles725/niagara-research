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
covered_blocks: 3
gaps_closed: 3
known_gaps: 6
investigable_open: 3
requires_execution_open: 0
blocked_open: 0
<!-- /research-state.v1 -->

## Coverage

- **Covered blocks (this focus)**: 0 — bootstrapped 2026-08-07.
- **Coverage metric**: 0 / 6 seeded gaps.
- **Method**: SHA-256 manifest of both trees → classify changed set (only-in-A / only-in-B / same-path-diff)
  → `diffoscope` ONLY on the license-axis subset (never diffoscope 30 GB of mostly-identical product bytes).

## Gap-backlog (prioritized)

| Pr. | ID | Gap | Artifact / source | Status |
|---|---|---|---|---|
| high | **L1** | Inventory + classification + license-axis answer | manifests A,B,iC | **covered → B386** |
| high | L2 | (mostly ABSORBED by B386) licensed `security/` structure vs corpus [B126]/[B2] — B has no security/, so not a byte-diff | A security/ | partial → B386 |
| med | **L3** | Module inventory delta (86 OEM + 66 user + 42/532 version); NO module license-gated | A/B modules/ | **covered → B388** |
| med | L4 | `bin/` native binaries diff — do the native launchers/DLLs differ 4.14 vs 4.15 (version axis; ties to platform-native B124-B385)? | A/B bin/ | pending |
| med | L5 | Config / system / generated-state diff — defaults, daemon config, registry-mirrored files, `system.properties` etc. | A/B (config) | pending |
| low | **L6** | Runtime feature-gate map + enforcement + signature chain | corpus + license files | **covered → B387** |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps |
|---|---|---|---|---|---|
| 1 | 2026-08-07 | L1 inventory + license-axis answer | B386 | no · inline | 0 new. B(4.15 EMEA)=INSTALLER not install (908 vs 66559); re-paired vs iC-Niagara-4.13.2.18 (installed unlicensed). LICENSE AXIS: unlicensed has NO security/; a license materializes security/licenses+db/<HostId>+certificates+truststore.jks. diffoscope 327 validated. |
| 2 | 2026-08-07 | L6 runtime feature-gate map | B387 | yes · sonnet (gate-site sweep) + inline verify | 0 new. 178 grants (27 Hon+1 CL+150 Webs demo, developer skipModuleValidation=true). API LicenseManager.get/checkFeature; unlicensed→limits MAX_VALUE (UNCAPPED not disabled); heap.limit exceed→System.exit(-3); SMA=checkModuleReleaseDate at module load; signature-verified via hardcoded DSA+ECDSA master keys — CONFIRMS B126 §126.6 inference (native isFeaturePresent=text-match, Java=real verify). |

## Stop control

- Bootstrapped 2026-08-07. L1 CLOSED (B386). Investigable = 5 (L2 partial, L3 modules-version, L4 bin, L5 config, L6 feature-gate). NEXT = L3 (module version delta 4.14↔4.15 with japicmp) OR L6 (feature-gate map). Not at STOP.
| 3 | 2026-08-07 | L3 module delta | B388 | no · inline | 0 new. A=684 B=574 modules; only-A 152 = 86 Honeywell OEM (vendor) + ~66 user/3rd-party (chihuahua/nmodsreflow/electronicSignature); only-B 42 = 4.15 base (entsec/accessControl/cloudLink); 532 common differ by version. NO module license-gated — confirms B387 from disk. |
