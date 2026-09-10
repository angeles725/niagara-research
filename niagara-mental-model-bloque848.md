# Block 848 — HARBOR engagement: what the job is, who does what, and how it is executed

> **Focus:** harbor-greenmax-lighting (project/context, DOCUMENT-mode §20). **Scope:** the non-technical spine
> of the job — the deliverable, the parties and their responsibilities, the execution method, and the site's
> human/access model — so a future session (or a hand-off) knows *who/what/how*, not just the control logic.
>
> **Evidence:** `[CERT-live]` = the SEJOFA proposal PDF (`docs/…SEJOFA…pdf`) + the two station backups read
> with `bog-nav`. No secret values (SECRETS DISCIPLINE): user names are station account labels already in the
> config, treated as roster facts, not credentials.

---

## 1. What the job is `[CERT-live]`

Reconfigure the **GreenMAX lighting control** of the HARBOR building and refresh its **dashboards**. The
operator's brief: replace ~349 per-circuit schedules with **4 master schedules**, an **HOA-style selector
(which of the 4) per panel and per circuit**, and a **force-ON override** (B842). The SEJOFA proposal frames a
narrower version — "16 tableros, horarios, **selector Manual/Horario** y control ON/OFF por salida" — so the
4-schedule design is a **richer variant** to confirm as in-scope or as a priced extension (B841 §5).

## 2. Who does what `[CERT-live]`

From the proposal's responsibilities matrix (§5) plus the station roster:

| Party | Role in the job |
|-------|-----------------|
| **Ingeniería SEJOFA** (provider) | Executes the config: backup, license install/activation/**validación básica**, GreenMAX functional configuration, final backup + basic operation guide. |
| **Cliente / integrador** | Provides valid **licenses + Host ID + model + OEM/marca + Niagara version + admin access**; delivers the **matriz de puntos, circuit names, horarios, operation criteria**; does the **physical wiring / addressing / point association**. |
| **Site operators / engineers** | Run the station day-to-day — the accounts already in the bog (§4). |
| **Both parties** | Validate licenses, points, horarios and deliverables before close-out. |

The **control-logic change itself** (the schedule redesign, B842–B847) is the engineering work; the
**descriptions/wiring reality** depend on the client's cuadro de cargas (B846-G2).

## 3. How it is executed `[CERT-live]`

- **Remote** ("los trabajos se realizarán de forma remota") — no site visit (viáticos/on-site are excluded, §7).
- **Where:** on the **Supervisor HM_BMS** (a Windows Workstation EC-Net4 4.3.58.18 install), because the
  lighting schedules run there (B841); the JACE HM_Central + BACnet plumbing stay untouched.
- **Tools:** Niagara **Workbench** over Fox/HTTP (the station serves Jetty/box — seen live in the console
  logs) and **PX Editor** for the dashboards. The control uses **stock kitControl** (B847) — no extra license.
  The Workbench for this site is installed by **`Distech Controls EC-Net 4 v4.3.58.18.4 Setup.exe`** (EC-Net 4
  = Distech's OEM Niagara 4; version **exactly matches** the stations' 4.3.58.18). Add-ons that layer on top:
  EC-NET Support Pack v4.6 (Distech device-config Wizards), EC-gfxProgram v6.2 (ECB/ECY graphical programming),
  Xpressgfx Points — `Install.bat` installs only these three, not the base platform. `[CERT-live]` (operator's
  Distech download folder, 2026-09-09).
- **Timeline / terms:** vigencia 30 días; **ejecución 2–3 semanas** desde anticipo + accesos + licencias + info;
  **garantía 90 días** sobre la configuración; ampliaciones se cotizan aparte.
- **Commercials:** $63,000 MXN + IVA ($15k licencias-servicio + $48k dashboard/config); 50% anticipo, 50%
  contra entrega. Licenses/pólizas are **not** supplied by SEJOFA (client-provided).

## 4. The site's human & access model `[CERT-live]`

The Supervisor holds **13 real user accounts** (`Services/UserService`): `admin`, `engineer`, `edgar`,
`Eduardo`, `Maximiliano`, `Luis`, `Sergio`, `Edith`, `Jeronimo`, `operador`, `BACnet` (machine account),
`IEE`, and the built-in `guest`. Authorization is **role + category** based:

- **Roles:** `admin` (`AdminRole`, full) and **`Mantenimiento`** (a scoped maintenance role).
- **Categories:** `Admin`, **`Operacion`**, `User` — the buckets that gate what each role may see/write.

This matters for the redesign's write surface: an operator changing a schedule selection / HOA must have a
role permitted on the `Operacion` category. If the HTML-dashboard route (B845) is taken, its write path must
re-authenticate against these accounts (the R14 second-login pattern, B834).

## 5. Constraints & risks that shape execution `[CERT-live]`

- **Licenses are the client's** — the job cannot start until valid licenses for the Host ID are installed
  (B838 is the procedure for this exact JACE).
- **No major upgrade:** AX→N4 / version upgrade is **excluded** (§7); the station stays on 4.3.58.18 — which
  also dictates the pre-4.13 servlet mechanism for any HTML dashboard (B845).
- **Scope delta** (§1 here) — the 4-schedule design vs the quoted 2-state selector.
- **Remote-only + client wiring** — SEJOFA does not touch field wiring or point mapping; correctness of the
  physical circuit→relay association is the client's responsibility.

---

## Self-verify

| # | Claim | Marker | Evidence |
|---|-------|--------|----------|
| 1 | Job = GreenMAX lighting reconfig (4 schedules + selector + override) + dashboards; proposal = narrower Manual/Horario | [CERT-live] | SEJOFA PDF pp.1–2; operator brief (B842) |
| 2 | Responsibilities split: SEJOFA config; client licenses+matriz+wiring; both validate | [CERT-live] | SEJOFA PDF §5 |
| 3 | Remote execution; on-site/viáticos excluded | [CERT-live] | SEJOFA PDF §7–8 |
| 4 | Work lands on the Supervisor HM_BMS (Workstation 4.3.58.18); JACE/BACnet untouched | [CERT-live] | B841; station facts |
| 5 | Tools: Workbench (Fox/box, seen in console logs) + PX Editor; stock kitControl | [CERT-live]/[INFER] | console_backup stack traces (box servlet); B847 |
| 6 | 2–3 wk execution, 30-day validity, 90-day warranty; $63k MXN (15k+48k), 50/50 | [CERT-live] | SEJOFA PDF §3,4,8 |
| 7 | 13 user accounts; roles admin + Mantenimiento; categories Admin/Operacion/User | [CERT-live] | bog `find --type b:User/b:Role/b:Category` |
| 8 | Licenses client-provided; no major upgrade; station stays 4.3.58.18 | [CERT-live] | SEJOFA PDF §7; B838/B841 |

**Tally:** 8 claims — 7 [CERT-live], 1 mixed (tools: PDF + console evidence + [INFER] on kitControl). No unmarked assertions.

## Connections
- **B841 §5** — the commercial frame this block expands.
- **B838** — the backup/licensing procedure SEJOFA's "instalación/activación de licencias" line refers to (same JACE).
- **B834** — the R14 second-login write-auth pattern relevant to §4's access model.
- **B842–B847** — the technical work this engagement delivers.

## Open gaps (RESEARCH-STATE-harbor-greenmax-lighting)
- **B848-G1** — Confirm which accounts/role (Mantenimiento vs admin) the client wants to own the schedule-selection + HOA writes, and on which category.
- **B848-G2** — Who "IEE" is (integrator company account?) and whether the redesign hand-off/audit should attribute to a named account.
- **B848-G3** — Confirm the scope delta resolution (4-schedule design in-scope vs priced extension) before build start.
