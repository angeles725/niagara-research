# Block 162 — SÍNTESIS TERMINAL del focus `live-station`: los 14 defectos de B150 contra la station viva

> **Bloque terminal del focus** (METHODOLOGY §8 terminal trigger / §12): consolida la validación DINÁMICA
> completa de la station Niagara N4 VIVA — Etapa A (mapa del runtime) + Etapa B (verificación de los 14 defectos
> de [Block 150] §150.2). NO es decompilado nuevo: teje los veredictos `[CERT-hw]` de B156-B161 en un mapa de
> explotabilidad vivo, contrastándolo con el análisis estático del corpus. READ-ONLY consolidante.
>
> Focus: **live-station** — bloque TERMINAL, cierra el focus. Corpus language: Spanish (technical EN).
>
> **`live-install` → SECRETS DISCIPLINE (invariante cumplido):** en las 6 iteraciones del focus **cero
> secretos exfiltrados** — bodies de config solo en backup de scratchpad (hash citado), HostID no capturado,
> credencial del usuario `API` solo en tránsito efímero. La station quedó **pristina** (`bf70f28f…`).
>
> Fuente: los bloques B156-B161 de este focus (evidencia `[CERT-hw]` en `sources/probes/`) + [Block 150] §150.2.
> Markers: `[CERT-hw]` re-cita medición viva ya verificada · `[CERT]` re-cita decompilado · `[INFER]` síntesis.
>
> Capa 27 (runtime vivo). Consolida [Block 156]-[Block 161]; contrasta [Block 150] y [Block 144]/[Block 145]/[Block 149].

---

## 162.1 — Mapa del runtime vivo (Etapa A) `[CERT-hw]`

Station **`Station`** en `DESKTOP-4AAQ77H`/`192.168.100.100`, módulo Reflow **1.7.7 (Ago 2025)** vivo. Puertos
seguros: foxs 4911, https 443 (http 80→redirect), platform HTTP(S) 3011/5011 (gated). Cert default
`ForRecoveryPurposes` (mismo en 3 puertos), TLS1.3-only, HSTS. Usuario `API` = HTTPBasicScheme. Servlet Reflow
en **`/nmodsreflow/`** (resuelve B138 §178). Versión Niagara core NO disclosed a read-level (blocked). Detalle:
[Block 156]-[Block 158].

## 162.2 — Veredicto vivo de los 14 defectos de [Block 150] §150.2 `[CERT-hw]`

| # | Defecto (B150) | Veredicto vivo | Evidencia |
|---|---|---|---|
| 1 | Config-write sin auth (WS sync-delta) | **CONFIRMADO por paridad** | B160 §160.2 |
| 2 | Config-write sin auth (REST overwrite) | **CONFIRMADO** (200, oracle) | B160 §160.1 |
| 3 | Config-write sin auth (REST delta) | **CONFIRMADO por paridad** | B160 §160.2 |
| 4 | Traversal destructivo (backups) | **NO reproducido — GATED 403** | B161 §161.2 |
| 5 | Traversal de escritura por header (notes) | **reachable** (500, no gated) | B159/B161 |
| 6 | Traversal de LECTURA (`?file=`) | **NO reproducido** (`{status:500}`) | B159 §159.2 |
| 7 | BQL injection (uuid) | **diferido — canal WS** | B161 §161.5 |
| 8 | BQL arbitrario read-level | **diferido — canal WS** | B161 §161.5 |
| 9 | `doPrivileged` anchos ×4 | **transversal** (los writes que aplican corren bajo él) | B160/B143 |
| 10 | Wipe de config sin token | **NO reproducido — GATED 403** | B161 §161.3 |
| 11 | SSRF-flavored + fuga HostID | **fetch outbound CONFIRMADO**; HostID no observado | B159 §159.3 |
| 12 | Audit trail forjable | **CONFIRMADO** (Client-* forjados, 200) | B160 §160.3 |
| 13 | Taint URL-decode | **mecanismo presente, read bloqueado** (500) | B159 §159.2 |
| 14 | CSP `unsafe-*` + input reflejado | **CSP CONFIRMADA**; reflexión parcial | B156/B159 |

## 162.3 — La lección `[CERT-hw]`: código correcto, explotabilidad NO uniforme `[INFER]`

El corpus mapeó los code-paths **correctamente** (`[CERT]` decompilado 1.7.7) — la station viva no refutó
ninguno a nivel de código. Lo que la fase dinámica **añadió** es la explotabilidad REAL, que el estático no
podía ver:

- **Asimetría de gate:** [Block 150] §150.1 postuló que TODA la superficie mutante era alcanzable a la sola
  sesión autenticada. En vivo es **falso para backups** (gated 403) y **cierto para config/notes** (200/500).
  El gate NO es uniforme — el subsistema backups exige más permiso.
- **Asimetría lectura/escritura:** los sinks de LECTURA (`?file=`, EquipmentNote read) **500-ean** con payloads
  triviales (no exfiltran), mientras las ESCRITURAS de config **se aplican limpias**. La escritura es el vector
  vivo real, no la lectura.
- **Hardening de despliegue:** cert default (débil) pero TLS1.3+HSTS (fuerte); versión gated; backups POST+auth
  (no el GET-shaped sin auth de B144). El despliegue vivo es más duro que el peor caso del decompilado.

`[INFER]` **Postura agregada viva:** el defecto explotable confirmado de mayor impacto es el **config-write sin
auth a read-level** (V1-V3/V12) — un usuario mínimo reescribe la config de la station y forja el autor. Los
destructivos (wipe/traversal) del corpus **NO son alcanzables** por ese usuario en esta station. La superficie
real es más acotada que la agregación teórica de B150 §150.3, pero el núcleo (config mutable sin autorización)
está **probado contra el hardware**.

## 162.4 — Estado terminal y handoff `[INFER]`

`[CERT-hw]` Etapa A cerrada (runtime mapeado); Etapa B: 13/14 con veredicto vivo. **read-only-investigable = 0**
para Etapa B por HTTP: el único pendiente (V7/V8, BQL exacto) vive en el **canal WS command-invoke** y es
**requires-execution** (§8/§19) — portar el protocolo Fox/box WS. Por §8 el focus alcanza su trigger terminal.

**Handoff (NEXT-ACTION):** una futura reapertura acotada (§8) podría portar un cliente WS command-invoke para
cerrar V7/V8 (inyección BQL vía `getAlarmByUuid`) — es la única superficie viva no cubierta. Requiere build de
un probe WS (no read-only HTTP), por lo que sale del loop estático. El usuario revocará el usuario `API` al
cierre; la autorización rung-3 de sesión **expira aquí**.

## 162.5 — Connections

- **[Block 150]** — la síntesis estática que este focus verificó punta a punta contra el hardware; tesis
  §150.1 **refinada** (§162.3) y superficie §150.3 **acotada** por los gates vivos.
- **[Block 144]/[Block 145]/[Block 149]** — code-paths confirmados existentes; comportamiento vivo refinado
  (§14): backups gated (B161), reads 500 (B159), config-write abierto (B160).
- **[Block 156]-[Block 161]** — las 6 iteraciones consolidadas aquí.
- **Focus `live-station`** — **CERRADO** (terminal). Contribución al corpus: primera validación `[CERT-hw]`
  end-to-end de la tesis de seguridad nmodsreflow contra una station Niagara viva, con cero secretos y station
  intacta.
