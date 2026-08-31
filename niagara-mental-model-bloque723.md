# B723 — Self-signed code-signing chain in Workbench + trusting the CA on a JACE + cross-version module compatibility (module-dev-workflow addendum)

> Focus: **module-dev-workflow** (closed 5/5) · ADDENDUM block. The end-to-end OPERATIONAL workflow to create
> your own code-signing certificate in Workbench (GUI), sign a module with it, and make a field controller (JACE)
> accept it. Walked through and VERIFIED WORKING by the operator this session (2026-08-30) while signing the
> `ColdRoomPan` module. Sources: the Tridium community guide (ddc-talk, [CERT-web]); corpus signing playbook
> [Block 18]; the operator's live run [CERT-live]. Complements [Block 18] (CLI/keytool + gradle side) with the
> Workbench GUI side + the JACE-trust step + a cross-version compatibility finding.

## 723.1 — The trust model (why any of this works)

[CERT-doc/INFER] Two-tier chain: a **CA** signs a **code-signing certificate**; the module is signed with the
code-signing cert. A host (Workbench, station, or JACE) **accepts** a signed module iff it **trusts the CA** that
signed the cert. So trusting the CA once covers every cert you sign with it, now and later. The private key
signs; the **public** cert (of the CA) is what you distribute so others can verify. Golden rule [CERT-live]:
**the private key never leaves the machine that signs** — only the CA's PUBLIC cert goes to field devices.

## 723.2 — Create the code-signing chain in Workbench (GUI)

[CERT-web ddc-talk / CERT-live] `Tools → Certificate Management` (AX Certificate Management), tabs
`User Key Store` / `User Trust Store`. Full sequence (operator did exactly this):

1. **Create the CA**: `User Key Store → New`. Fill Alias (e.g. `luisCA`), CN, **O** (required), **C** (required);
   OU/L/ST optional. Set **Certificate Usage = CA** (not the default Server), **Key Size = 4096**, and **Not
   After far out** (self-signed allows decades; operator used year 2090). OK → set a password (reused throughout).
   The CA appears **yellow** (not yet trusted).
2. **Trust the CA**: select the CA → `Export` (check **Export private key**, enter password, save). Then
   `User Trust Store → Import` → select the exported CA → OK → CA turns **green ✓** (trusted locally).
3. **Create the code-signing cert**: `User Key Store → New`. Different Alias/CN (e.g. `luissigner`), same O/C, but
   **Certificate Usage = Code Signing**. Set a password. It appears **yellow** (not yet signed by the CA).
4. **CSR**: select the code-signing cert → `Cert Request` → OK → password → save the `.csr`
   (default `user_home\.certManagement`).
5. **Sign the CSR with the CA**: `Tools → Certificate Signer Tool` → pick the `.csr`; the CA is preselected;
   enter the **CA password**; save (over the original is fine). ("Not After" here may default shorter — bump if wanted.)
6. **Import the signed cert back**: `User Key Store → Import` → the signed cert → OK. It prompts for the password
   (the alias already exists) and the private-key password; then the code-signing cert turns **green ✓**. Done —
   the signing identity is ready.

## 723.3 — Sign the module

[CERT-live] Two ways, both work:
- **Workbench (manual)**: `Tools → Jar Signer Tool` → select the module jar (`ColdRoomPan-rt.jar`) → select the
  code-signing alias (`luissigner`) → password → signs. (Operator used this; confirmed working.)
- **Gradle (automatic)**: the `niagaraSigning` block in the root `build.gradle.kts` with
  `aliases.set(listOf("luissigner"))` + `keystorePassword`/`keyPassword` from a private property (never
  hard-coded). [Block 18] §18.2.3. The default gradle build otherwise signs with the auto-generated DEV cert
  (`CN=…(Niagara4Modules)`, OU "For Development Purposes Only").

## 723.4 — Make the JACE accept it (the field-device step)

[CERT-live] You give the JACE ONLY the CA's **public** certificate — never the private key:
1. Export the CA's PUBLIC cert (from `User Trust Store → Export` — that store holds public-only, so the key can't
   leak; or `User Key Store → Export` with **Export private key UNCHECKED**).
2. Connect to the **JACE's Platform** (in Nav, the remote host, e.g. `192.168.1.140`).
3. `Certificate Management` (titled *Certificate Management for "<host>"*) → `User Trust Store → Import` → the CA
   `.pem` → OK. The CA shows **green ✓** on the JACE. The JACE now accepts any module signed by that CA's certs.
   (Operator confirmed: `luisCA` green in the `192.168.1.140` User Trust Store.)

## 723.5 — Cross-version compatibility (build target vs run target)

[CERT] A module's `module.xml` records its baja dependency at the **minor** version, not the patch:
`<dependency name="baja" vendor="Tridium" vendorVersion="4.15"/>` — i.e. "requires baja ≥ 4.15". So a module
built against 4.15.3.28 depends on "4.15" and runs on ANY 4.15.x. [CERT-live] `ColdRoomPan` built against
4.15.3.28 loaded fine on a Workbench install of **4.15.3.20** (palette shows its components) and is targeted at a
JACE of **4.15.3.28** — the patch numbers (.20 vs .28) are irrelevant to the dependency; only stable baja APIs
are used. (General rule, [Block 722]/corpus: build against the LOWEST target minor version → runs on that and
higher; building against a HIGHER minor and running on a lower one fails on the missing baja version.)

## Connections

- Signing playbook (keytool CLI + gradle DSL + truststore integrity warning) → [Block 18]. Cross-version +
  WSL build loop → [Block 722]. Module-permissions descriptor → [Block 721]. Focus `module-dev-workflow` runbook
  → [Block 711]–[Block 715]. Applied while signing/deploying `ColdRoomPan` (cold-room-module focus).

## Self-verify

| # | Claim | Marker | Citation | Verified |
|---|---|---|---|---|
| 1 | host accepts a signed module iff it trusts the signing CA; public cert distributed, private key never leaves | [CERT-live]/[INFER] | operator run 2026-08-30; ddc-talk | verified |
| 2 | Workbench GUI chain: CA (usage=CA) → trust → code-signing cert → CSR → Certificate Signer Tool → import | [CERT-web]/[CERT-live] | ddc-talk article; operator run | verified |
| 3 | sign the module via Jar Signer Tool (alias) or gradle niagaraSigning block | [CERT-live]/[CERT] | operator run; [Block 18] §18.2.3 | verified/cited |
| 4 | JACE trust = import CA PUBLIC into the JACE Platform User Trust Store | [CERT-live] | operator: luisCA green on 192.168.1.140 | verified |
| 5 | module dep recorded as minor "4.15"; runs on any 4.15.x (built .28, ran on .20 + .28) | [CERT]/[CERT-live] | ColdRoomPan-rt module.xml; operator run | verified |

**Tally:** [CERT-live] ×5 · [CERT-web] ×1 · [CERT] ×1 · [INFER] ×1. Block TYPE = operational/process (FUENTE-2
community doc + FUENTE-3 live operator verification).

## Open gaps

- None investigable. Note: on a LOCKED OEM (Honeywell) install whose trust root is the factory CA, self-signed
  trust still works at the User Trust Store level (per operator's existing `angelessigner` deployments); the
  install-level factory truststore must NOT be modified ([Block 18] §18.2.2 — breaks boot). Not a build/sign
  limitation, a trust-policy note.
