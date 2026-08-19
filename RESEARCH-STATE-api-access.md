# RESEARCH-STATE — focus: api-access (DOCUMENT-MODE capture, B457)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-19** en **modo document (§20)**: captura
> del MÉTODO reproducible para autenticarse programáticamente a una estación Niagara N4 viva, verificado en
> vivo contra la estación PROPIA del operador (cuenta de servicio `API2`, credenciales válidas — acceso
> legítimo por el protocolo real, equivalente a Workbench; NO bypass). Trabajo cruzado con la sesión "camara"
> (que hizo la verificación en vivo); corroborado desde el código por este lado.
>
> **Bloque:** B457 — GET /prelogin → SCRAM-SHA-256 (client-first / server-first r,s,i / client-final con
> proof + verificación de v=) → `POST action=acceptEula` (commit de sesión) → autenticado (JSESSIONID +
> niagara_userid), listo para oBIX/BQL.
>
> **Artefactos [CERT-live]:** `sources/probes/B457-n4-login/niagara-n4-client.py` (impl de referencia, stdlib,
> sin secretos) + `http-digest.py` (cliente Digest/Basic RFC 7616 para cámaras/IoT, NO para N4).
>
> **Base de evidencia:** SCRAM/SHA-256 `[CERT]` código (`BDigestAuthenticationScheme:44`→`DigestLoginModule:26`→
> `ScramServer`; `UserKeyFactory:14` PBKDF2-HMAC-SHA256) + `[CERT-live]` cross-session (handshake, acceptEula,
> oBIX). Triangulación de tres vías (sondeo /prelogin + corpus + prueba directa).
>
> **SECRETS DISCIPLINE:** usuario/rol citados, nunca la contraseña. **Acción pendiente del operador: ROTAR las
> credenciales de prueba API2 (expuestas en chat).**

<!-- research-state.v1 -->
schema: research-state.v1
method: document-cycle-external
block_scope: shared-global
covered_blocks: 457
gaps_closed: 0
known_gaps: 2
investigable_open: 2
requires_execution_open: 2
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Outline cubierto 1/1 (el método de login). Gaps abiertos: **B457-G1** (recetas BQL/history sobre oBIX
autenticado) y **B457-G2** (lifecycle de sesión `niagara_userid` + CSRF/token en writes — este bloque solo
ejerció lecturas). Acción de seguridad (no-gap): rotar credenciales API2.
