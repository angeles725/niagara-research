# RESEARCH-STATE — focus: email (ACTIVE 4/10)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-04** a pedido explícito del usuario
> ("vamos a abrir bloques nuevos dedicados al modulo email"), tras una consulta sobre envío de alarmas por
> correo desde Workbench/station.
>
> **NO es terreno virgen** — backlog audit-first (no lista de deseos). Cobertura previa verificada:
> - **[Bloque 34]** §34.6.5 — `com.tridium.email.alarm.BEmailRecipient`: slots, flujo `handleAlarm` (7 pasos),
>   `emailAccount` (referencia por-nombre), credenciales vía keyring/BPassword, nota OAuth2. **Es la cobertura
>   más profunda de email hoy** → `BEmailRecipient` queda **REMITTANCE**, con un delta fino: B34 documenta
>   `BSmsAlarmAcknowledger` pero NO `BEmailAlarmAcknowledger` (ack por reply-to UUID inbound).
> - **[Bloque 27]** — puertos SMTP (25/465/587), STARTTLS, trust stores, egress por proxy.
> - **[Bloque 31]** — sysprop `niagara.email.maxNumberOfRetriesBeforeDiscard=6`.
> - **[Bloque 8]** — enumeración de recipients (menciona `BEmailRecipient`).
>
> **Ángulo declarado (§b2)**: el módulo `email` como SUBSISTEMA de servicio — el motor SMTP que efectivamente
> ENVÍA (BEmailService + BOutgoingAccount + sesión JavaMail), su gate de licencia `tridium/email`, el lado
> inbound (POP3/IMAP + ack), OAuth2, seguridad, y las capas wb/ux. El corpus documenta CÓMO una alarma se
> vuelve correo (el recipient), NO el servicio que lo envía ni su licencia.

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 322
gaps_closed: 4
known_gaps: 10
investigable_open: 6
requires_execution_open: 0
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: email
status: active
bootstrapped_on: 2026-08-04
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B328)

## Pre-flight e2 — existencia + tamaño MEDIDO

Conteo sobre el pipeline **vineflower** (canónico). Raíz:
`/home/cristian/modules/Prototipos/modulos/organized/email/`.

| Artefacto | Clases | Desglose por paquete |
|---|---|---|
| `email-rt` | **43** | `javax/baja/email` 23 · `/converters` 9 · `com/tridium/email` 8 · `/alarm` 2 · `/se` 1 |
| `email-ux` | **11** | `com/tridium/email/ux` + `/ux/fe` |
| `email-wb` | **7** | `com/tridium/email/ui` + `/hx` |
| **Total** | **61** | Todas confirmadas en disco (audit 2026-08-04). Sin fuente Javadoc en `docSource/` para `javax/baja/email`. |

Deps de módulo notables (email-rt/module.xml): `alarm-rt`, `oauth2-rt`, `web-rt`, `jsonSmart-rt`, `entityIo-rt`, `net-rt`.

## Coverage

- **Covered blocks**: 322 (corpus-wide, shared-global)
- **Coverage metric**: 4 / 10 closed
- **Last iteration**: 2026-08-04 — E4 closed (inbound + alarm ack, B327)

## Gap-backlog (prioritized)

Formato canónico de 4 columnas exigido por `research-sdd-status.sh`.

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | E1 BEmailService — ciclo de vida, gate de licencia tridium/email, dependencia runtime JavaMail, contenedor de cuentas | decompiled-java | closed (B324) |
| high | E2 BOutgoingAccount — pipeline de envío SMTP: cola memoria/disco, maxSendablePerDay, reset midnight, retry | decompiled-java | closed (B325) |
| high | E3 sesión TLS — MailPlatformHandlerSe + BEmailAccount: Properties JavaMail, useSsl vs useStartTls, tlsMinProtocol | decompiled-java | closed (B326) |
| medium | E4 lado inbound — BIncomingAccount (POP3/IMAP) + BEmailAlarmAcknowledger (ack por reply-to UUID) | decompiled-java | closed (B327) |
| medium | E5 OAuth2 SMTP — BAbstractOAuthEmailAuthenticator (client-secret vs client-cert), XOAUTH2, tie a oauth2-rt | decompiled-java | pending |
| medium | E6 security dashboard — BEmailServiceSecurityDashboardProviderAgent: matriz de posturas ALERT/WARNING/OK | decompiled-java | pending |
| medium | E7 account base + authenticators — BEmailAccount, BEmailClientAuthenticator y variantes, migración de credenciales deprecadas | decompiled-java | pending |
| medium | E8 email-wb — UI de Workbench: BEmailAccountManager, BOutgoingAccountFE (name-picker), field editors | decompiled-java | pending |
| low | E9 converters — javax.baja.email.converters (9): adaptadores BEmailAddress/List ↔ String | decompiled-java | pending |
| low | E10 email-ux — capa browser: BEmailAccountUxManager + JsBuild/CssResource + type-ext editors | decompiled-java | pending |

### Remittance (no son gaps — ya cubiertos)

- `BEmailRecipient` (alarma→correo) — **[Bloque 34] §34.6.5**. Delta fino absorbido por E4 (`BEmailAlarmAcknowledger`).

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| — | 2026-08-04 | (bootstrap — audit-first) | — | yes · sonnet (audit sweep) | 10 seeded |
| 1 | 2026-08-04 | E1 BEmailService | B324 | no · inline (constraint: single load-bearing class, 167 lines) | 0 |
| 2 | 2026-08-04 | E2 BOutgoingAccount | B325 | yes · sonnet (961-line sweep) | 0 |
| 3 | 2026-08-04 | E3 TLS session | B326 | no · inline (constraint: focused 3-file read) | 0 |
| 4 | 2026-08-04 | E4 inbound + alarm ack | B327 | yes · sonnet (2-file sweep) + driver re-read | 0 |

## Blocked gaps (each tagged with what it needs)

- none — all 10 gaps are read-only investigable (source confirmed on disk).

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 6   ← el loop ESTÁTICO para cuando esto llega a 0
- **Open gaps — requires-execution**: 0
- **Open gaps — blocked**: 0
- Consecutive iterations with empty backlog (secondary): 0/2
- Budget cap: none

## Dismissed file types

- none
