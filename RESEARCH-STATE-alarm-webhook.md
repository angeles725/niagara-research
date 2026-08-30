# RESEARCH-STATE — focus: alarm-webhook (STOPPED 6/6, investigable=0 — reopened for AW5/AW6)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-30** a pedido de la sesión `Telegram`
> (teammate), para cerrar 4 huecos a nivel bytecode antes de escribir un módulo `-rt` custom
> `BMiWebhookRecipient extends BRecoverableRecipient` que serialice `BAlarmRecord`→JSON y haga POST a un
> backend Node→Telegram. El punto de extensión ya estaba confirmado ([Block 34] §34.6); esto es el
> DEEPENING code-level de los 4 detalles que faltaban.
>
> **NO es terreno virgen** — cobertura previa: [Block 34] §34.1.3 (pipeline routing), §34.6.4
> (BRecoverableRecipient, ruta inferida — ahora §14-corregida), §34.6.5 (BEmailRecipient SMTP + BPassword),
> §34.9.3 (Nre:Engine thread check), G1 (alarm-queue OOM). READ-ONLY sobre `organized/` (decompilado).
>
> **Ángulo:** los 4 seams Java que un recipient webhook custom toca — la clase base `BRecoverableRecipient`,
> el threading de despacho, el `module.xml`/registro, y el almacenamiento del token — leídos de fuente
> primaria (docSource Tridium, verificados 1:1 contra vineflower).

<!-- research-state.v1 -->
schema: research-state.v1
block_scope: shared-global
covered_blocks: 667
gaps_closed: 6
known_gaps: 7
investigable_open: 0
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

focus: alarm-webhook
status: stopped
bootstrapped_on: 2026-08-30
block_prefix: niagara-mental-model-bloqueN.md (numeración global; próximo libre: B672 (focus CERRADO))

## Coverage

- **Covered blocks**: 667 corpus-wide (this focus: B666-B671) (shared-global)
- **Coverage metric**: 6 / 6 investigable closed
- **Last iteration**: 2026-08-30 — AW6 closed (B671). Focus reopened 2026-08-30 for a 2nd teammate request (AW5/AW6).

## Gap-backlog (prioritized)

| Priority | Gap | Type | Status |
|---|---|---|---|
| high | AW1 decompile BRecoverableRecipient — constructor/lifecycle, retry thread, persistent-queue serialization format + path, firmas sendAlarm/dequeueMemory/dequeueDisk | decompiled-java | closed (B666) |
| high | AW2 threading — qué thread corre handleAlarm/sendAlarm, si la alarmQueue es unbounded (OOM), si BRecoverableRecipient desacopla el envío | decompiled-java | closed (B667) |
| medium | AW3 module.xml mínimo -rt — dependency alarm + registro @NiagaraType del recipient + permisos | decompiled-java | closed (B668) |
| medium | AW4 almacenamiento seguro del token — BPassword + keyring DPAPI (.km/.kr) aplicado a Authorization: Bearer | decompiled-java | closed (B669) |
| high | AW5 getters exactos de BAlarmRecord para toJson — firmas de uuid/timestamp/alarmClass/priority/sourceState/ackState/ackRequired/source + getAlarmFacet + enumerar alarmData | decompiled-java | closed (B670) |
| high | AW6 ACK externo vía oBIX — endpoint bajo /obix, contrato AckAlarmIn, referencia por uuid, shape del POST, auth/permisos, mapeo uuid | decompiled-java | closed (B671) |
| medium | AW3-G1 protection-domain del disk-write persistente — ¿el módulo subclase necesita la FilePermission `${protected.station.home}/alarm`, o el write corre bajo el dominio de alarm-rt? | live-station | requires-execution → §12 (needs live station: set persistent=true, force failed send, watch for AccessControlException) |

### Remittance (ya cubiertos, no son gaps)

- Pipeline de routing completo → [Block 34] §34.1.3. Nre:Engine thread check → §34.9.3.
- SMTP/BEmailRecipient credenciales → [Block 34] §34.6.5 + [Block 330]. Keyring .km/.kr DPAPI → [Block 5]§5.4, [Block 13]§13.2.
- Type-registration pipeline @NiagaraType→Registry → [Block 631]. Reference -rt skeleton → [Block 636]. Module signing → [Block 392]-[396], [637].
- Value-document encoding (.bog/ValueDoc) → [Block 5]/[Block 33].

## Iteration history

| # | Date | Gap closed | Block | Delegated? · model tier | New gaps uncovered |
|---|---|---|---|---|---|
| 1 | 2026-08-30 | AW1 BRecoverableRecipient anatomy | B666 | no · inline (single load-bearing class, doc-source) | 0 (§14 correction to B34 §34.6.4) |
| 2 | 2026-08-30 | AW2 threading / OOM | B667 | yes · Explore (backward call-graph trace) + driver re-verify | 0 |
| 3 | 2026-08-30 | AW3 module.xml + registration | B668 | no · inline (2 real module.xml) | AW3-G1 (requires-execution) |
| 4 | 2026-08-30 | AW4 BPassword bearer token | B669 | no · inline (BPassword + live SMTP reference) | 0 |
| — | 2026-08-30 | (focus reopened — 2nd teammate request) | — | — | AW5, AW6 seeded |
| 5 | 2026-08-30 | AW5 BAlarmRecord getters | B670 | no · inline (single doc-source class + BFacets) | 0 (1 correction: getAckRequired not isAckRequired) |
| 6 | 2026-08-30 | AW6 oBIX external ack | B671 | yes · Explore (obixDriver ack path) + driver re-verify | 0 |

## Blocked gaps (each tagged with what it needs)

- AW3-G1 (requires-execution): `tried:` static reading cannot decide the effective AccessControlContext at the
  `ValueDocEncoder` call site (it lives in alarm-rt code but is reached from the subclass). Needs a live station
  to observe whether the subclass module needs the `${protected.station.home}/alarm` FilePermission. Safe
  default meanwhile: declare it.

## Stop control (primary = read-only-investigable exhaustion, METHODOLOGY §8)

- **Open gaps — read-only investigable**: 0 → STOP (focus done)
- **Open gaps — requires-execution**: 1 (AW3-G1)
- **Open gaps — blocked**: 0
- Budget cap: none (scoped 4-gap request from teammate)

## Dismissed file types

- none (scoped focus; no census — reuses the base corpus already-extracted `organized/` tree)
