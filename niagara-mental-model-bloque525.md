# B525 — signing-pki: the DYNAMIC verification & hardening runbook (document mode §20) — consolidating B518–B524's live findings into operator procedures

**Focus:** `signing-pki` · **Mode:** document/capture (§20) · **Product:** `docs/niagara-signing-hardening-guide.md` · **Language:** English.

**Scope.** Captures the *dynamic* half of the focus into one operator runbook: every `[CERT-live]` finding from the §12/§19 session (B518–B524) → its re-verification command and its hardening action, using the agreed toolset (editor, WDAC/AppLocker, `keytool`, FIM, `nverify`). This block **records the consolidated deliverable** and cites it; the full content lives in the doc (`docs/niagara-signing-hardening-guide.md`). Defensive only. SECRETS DISCIPLINE observed.

---

## 1. What was captured (outline → sections)

| Doc § | Content | Source blocks |
|---|---|---|
| 1 | Load-time/event-triggered model; the `nre.exe -licenses` fresh-JVM oracle | [B519] §2, [B518] |
| 2 | Ground-truth re-measurement (sha256 + same-PID invariant) | [B518], [B524] |
| 3 | Read-only posture probes: `moduleVerificationMode`, `commandLinePropertyBlacklist`, `program.requireSigning`, `nverify` per-JAR, `keytool` truststore, license oracle | [B519], [B398], [B521], [B392], [B518] |
| 4 | H1–H7 hardening actions with editor + new-state verify commands | [B522] |
| 5 | What is already strong (fail-closed crypto, sound trust chains) | [B518], [B522] §3 |
| 6 | The mirror, distilled for operators (license = BC-FIPS Java-side; module chokepoint flippable in-process) | [B524] |
| 7 | Toolchain map (question → tool → block) | B518–B524 |

## 2. New exact command facts re-verbatimized for the doc (not re-derived — re-measured this pass) `[CERT-live]`

- `keytool` lives at `jre/bin/keytool.exe` (the install's bundled JRE), not `bin/`. `-list -keystore security/truststore.jks -storepass changeit` → **1 entry**: `niagaramoduledev, 15/01/2026, trustedCertEntry` (SHA-256 `83:7B:38:E8:AF:D4:…`). Confirms [B392]/[B398]'s dev-anchor + default-password finding on the exact file. `[CERT-live]`
- `defaults/system.properties` live line numbers: `niagara.moduleVerificationMode=low` (442), `#program.requireSigning=false` (447), `#niagara.commandLinePropertyBlacklist=…` (474). `[CERT-live]`
- `bin/policy/signing.properties` build pin: `issuerDN=CN=Honeywell CodeSign RSA CA…`, `subjectDN=CN=Niagara4Modules Code Signing` (auto-generated, "DO NOT MODIFY"). `[CERT]`
- `modules/*.jar` = **400** jars (the inventory surface for H7). `[CERT-live]`

## 3. Self-verify

| # | Claim | Marker | Evidence |
|---|---|---|---|
| 1 | Deliverable exists and cites blocks, not re-derived claims | `[CERT]` | docs/niagara-signing-hardening-guide.md:1-30 (header + evidence base) |
| 2 | keytool truststore = 1 dev anchor + changeit | `[CERT-live]` | run this pass; [B392]/[B398] |
| 3 | system.properties lines (442/447/474) as cited | `[CERT-live]` | grep this pass; [B519] |
| 4 | 400 modules/*.jar (H7 inventory) | `[CERT-live]` | ls | wc -l this pass |
| 5 | Hardening actions are operator recommendations, none applied | `[CERT]` | doc §4 states "recommendations the operator applies"; no config write performed |

**Tally:** 4 `[CERT-live]`, 1 `[CERT]`, 0 `[INFER]`. No unmarked claims; no config write performed.

## 4. Connections

- Consolidates the dynamic thread [B518]–[B524] + [B398]; operator-side counterpart to `docs/niagara-licensing.md` (which consolidates the *licensing/entitlement* thread).
- Feeds [B522] H1/H4/H6 with copy-paste commands; marks H4+H6 as the controls that survive the demonstrated mirror [B524].
- No new gap. Open items unchanged: SP-G9a, SP-G10a, SP-G6, SP-G8, SP-G3a (blocked), SP-G4 (blocked), SP-G9b (blocked).
