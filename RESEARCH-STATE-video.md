# RESEARCH-STATE — focus: video (DOCUMENT-MODE capture, B453–B456)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-08-19** en **modo document (§20)** a
> pedido explícito del usuario ("aterriza el paso a paso de V1, V2, V3 y V5, documenta cada caso").
> NO es un loop de descubrimiento: es CAPTURA outline-driven de cuatro how-to operativos para integrar una
> cámara AXIS (M2025-LE, homelab propio del operador) en Niagara N4. Trabajo cruzado con la sesión-cámara
> (que aporta el lado cámara/red/relay como `[CERT-live]`).
>
> **Outline (4 vías, 1 bloque c/u):**
> - **B453 — V1**: driver nativo `naxisVideo` en el Supervisor (licenciado; RTSP/H.264 + VAPIX Digest).
> - **B454 — V2**: embeber el relay MJPEG en un Web Widget Px (`BHxPxWebBrowser`; libre de licencia).
> - **B455 — V3**: PxImage refrescando snapshot (`BOrdToImage`, kitPx; lo más ligero, libre de licencia).
> - **B456 — V5**: módulo propio N4 (devkit; gate = firma de código, NO feature de video).
>
> **Deliverable humano:** `docs/video-axis-n4-integration.md` (matriz de opciones + desglose licencia).
>
> **Base de evidencia:** mecánicas N4 `[CERT]` file:line (corpus 4.14); endpoints VAPIX + relay + prueba
> SCRAM `[CERT-live]` (sesión-cámara, triangulado); licencia `[CERT-live]` (`Webs.license` vivo). Las
> SECUENCIAS de pasos son `[INFER]` — no ejecutadas contra estación viva en este trabajo (cada bloque abre
> un `B45x-G1` de validación en vivo).
>
> **Hallazgo transversal (auth):** el "Digest" de N4 es **SCRAM** (`BDigestAuthenticationScheme` →
> `ScramServer`/`ScramServerCallback`/`DigestLoginModule`), NO HTTP Digest RFC 7616 — confirmado en código
> `[CERT]` y empíricamente por la sesión-cámara `[CERT-live]` (cliente HTTPDigestAuthHandler inerte,
> todo→/prelogin). Relevante solo para hablar con la ESTACIÓN, no para el video (el driver/relay autentican
> contra la CÁMARA, que sí es Digest RFC 7616).

<!-- research-state.v1 -->
schema: research-state.v1
method: document-cycle-external
block_scope: shared-global
covered_blocks: 456
gaps_closed: 0
known_gaps: 4
investigable_open: 4
requires_execution_open: 4
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Outline cubierto 4/4 (document mode STOP: outline agotado, no gap-exhaustion). Los cuatro `B45x-G1` son de
**validación en vivo** (requieren estación + cámara alcanzable): promueven las secuencias `[INFER]` a `tried:`
`[CERT-live]` cuando se ejerzan. Además queda **B456-G1** (política de firma en producción para módulo
self-signed) como investigable contra el hilo signing-pki ([B400]).
