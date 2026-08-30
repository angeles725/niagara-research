# B720 — Station organization, reuse + synthesis (SO5, focus close): equipment templates, fleet provisioning, and the recommended-structure verdict

> Focus: **station-organization** · Gap **SO5** (reuse + synthesis + deliverable) — FOCUS-CLOSING block. Block
> TYPE = **SYNTHESIS** (high [INFER] ratio expected). Consolidates [Block 716]–[Block 719] + the template/
> provisioning focuses. Finalizes `docs/station-organization.md`.

## 720.1 — Reuse: equipment templates

[CERT] Once you have ONE equipment modeled well (points + logic + links + tags, SO1-4), the **template**
subsystem ([Block 577]–[Block 583]) captures it as a reusable unit: a `.ntpl` template with typed parameters
(`BConfigBinding` keyed by handle, [Block 591]) that you deploy N times, each instance re-bound to a different
device's points. `ApplicationTemplateInstaller` does a `ReplacingContext` swap so relations/links survive the
re-parametrization ([Block 578]/[Block 579]). This is the graphical equivalent of a custom `BComponent` type
instantiated N times (chihuahua's 77 `BChiUp`, [Block 169]) — build the "rooftop unit" or "VAV" once, stamp it
per zone.

## 720.2 — Reuse across a fleet: provisioning

[CERT] To roll a station structure (or its updates) out to many JACEs, the **provisioning** subsystem
([Block 567]–[Block 576]) drives it: `BBatchJob` over a `DeviceNetwork` runs steps per station (software dist,
template deploy, credential/license batch) over the two-credential `niagaraProv` Fox channel ([Block 568]).
Template deploy is a provisioning step ([Block 573]). So the SAME equipment template + tag scheme + hierarchy is
pushed to the whole fleet, not hand-built per site — the JACE's `ProvisioningNwExt` is the on-controller half
(empty on the seed station, [Block 686]).

## 720.3 — The recommended structure (verdict)

[INFER, consolidating SO1-4] For the operator's question — where do TC500/IO-R-34 points and their programming
go? — the maintainable answer is a **layered, tag-projected** structure:

| Layer | Where | What | Rule |
|---|---|---|---|
| **IO / points** | `/Drivers/<Net>/<Device>/points/` | proxy points (raw IO) | points-only; created by discovery/learn; one per physical signal (SO1) |
| **Equipment / logic** | `/Config` or `/Services`, per-equipment component NEAR its points | kitControl / BProgram / custom module | one component = one equipment; Philosophy B (near the points), tagged `equip` (SO2) |
| **Links** | between the two | BLinks + priority array | wire with the batch editor; keep handle/tag-stable (SO3) |
| **Navigation** | hierarchies over tags | operator views by equipment/location | tag once, navigate many; never duplicate the tree (SO4) |
| **Reuse** | templates + provisioning | one equipment template × N instances × fleet | build once, stamp/push (SO5) |

**Do NOT:** put control logic in `/Drivers` or inside a `points/` container; build a monolithic central `Logic`
folder far from the points; duplicate the physical tree to make an operator view; hardcode ORDs that break on
re-address.

**Do:** points under their device (points-only) · logic per equipment near its points, tagged `equip` · linked by
handle/tag · operator views via hierarchies over tags · reuse via templates + provisioning.

## 720.4 — The deliverable

[CERT] `docs/station-organization.md` is finalized with five sections: the driver/points layer (§1), the
equipment/logic layer (§2), linking (§3), navigation (§4), and reuse (§5). It is the how-to answer to "where does
the programming go when I add TC500/IO-R-34 points."

## Connections

- Templates → focus `template` [Block 577]–[Block 583]/[Block 591]; provisioning → focus `provisioning`
  [Block 567]–[Block 576]; on-controller ProvisioningNwExt → [Block 686]. Consolidates SO1-4 [Block 716]–[Block 719].
  Custom-module reuse → focus `chihuahua` [Block 169]. Deliverable: `docs/station-organization.md`.

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | equipment templates (.ntpl, typed params, ReplacingContext swap) for N-instance reuse | [CERT] | [Block 577]/[Block 578]/[Block 591] | cited |
| 2 | provisioning pushes the structure to a fleet (BatchJob, template-deploy step) | [CERT] | [Block 567]/[Block 573] | cited |
| 3 | recommended layered structure (IO/logic/links/nav/reuse) | [INFER] | 720.3 | synthesized |
| 4 | deliverable finalized (5 sections) | [CERT] | docs/station-organization.md | delivered |

**Tally:** [CERT] ×2 · [INFER] ×2. Block TYPE = **SYNTHESIS** — ratio expected-high. Re-cites verified blocks.

## Focus status

**SO5 CLOSED → station-organization investigable = 0 → focus STOP.** 5/5 gaps closed (SO1–SO5). Deliverable
`docs/station-organization.md` complete. No requires-execution, no blocked gaps. Next: §18 retro + push.
