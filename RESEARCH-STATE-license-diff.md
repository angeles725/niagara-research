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
covered_blocks: 0
gaps_closed: 0
known_gaps: 6
investigable_open: 6
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
| high | **L1** | Inventory + manifest classification: the changed set (only-in-A / only-in-B / same-path-diff-bytes), first attribution by axis (license/version/vendor) | sha256 manifests A,B | **pending (NEXT)** |
| high | L2 | `security/` diff — the license artifacts themselves: `.license`/`.certificate`, the per-host license `db/`, DSA/RSA sig structure. What a license IS on disk (license axis; remit [B126]/[B2] for the format) | A/B security/ | pending |
| med | L3 | Modules delta — which `.jar` are present only-in-licensed (Honeywell OEM `opt*`/`hon*`) vs only-in-unlicensed; is any module LICENSE-gated vs merely vendor/version? | A/B modules/ | pending |
| med | L4 | `bin/` native binaries diff — do the native launchers/DLLs differ 4.14 vs 4.15 (version axis; ties to platform-native B124-B385)? | A/B bin/ | pending |
| med | L5 | Config / system / generated-state diff — defaults, daemon config, registry-mirrored files, `system.properties` etc. | A/B (config) | pending |
| low | L6 | What a license ENABLES — feature-gate map: does the unlicensed run demo-limited? (remit [B126] `isFeaturePresent`, [B2] `LicenseManager`, [B380] `fips140-2`/`developer` gates) — attribute which behaviors are license-gated | corpus + license files | pending |

## Iteration history

| # | Date | Gap closed | Block | Delegated? · tier | New gaps |
|---|---|---|---|---|---|

## Stop control

- Bootstrapped 2026-08-07. Investigable = 6. NEXT = L1 (inventory classification, running SHA-256 manifest).
- Not yet at STOP.
