# RESEARCH-STATE — focus: backup-licensing-ops (DOCUMENT-MODE capture, B838)

> Multi-focus corpus (METHODOLOGY §16). Focus **BOOTSTRAPEADO 2026-09-07** en **modo document (§20)** a
> pedido explícito del operador ("crea un corpus en base a esto"), capturando el hilo vivido en sesión:
> qué respaldos tener y cómo hacerlos, cómo restaurar, y cómo activar/ver licencias en un **JACE-8000 QNX
> TITAN** (station HM_Central, Distech EC-Net, N4.3.58.18). NO es loop de descubrimiento: es CAPTURA
> outline-driven de un procedimiento operativo, verificado verbatim contra la doc oficial Tridium
> (`niagara-help`, N4.14.0.162) — todo `[CERT-doc]` salvo el contexto vivo `[CERT-live]` y una técnica
> derivada `[INFER]`.
>
> **Outline (1 bloque consolidado):**
> - **B838** — Backup (3 tipos + clone USB), restore (consola microUSB + passphrase + trampa de
>   factory-recovery), rollback ligero `.dist`, system passphrase (protección + verificación no destructiva),
>   licenciamiento (licencia=XML/SMA, pull online por License Manager, station-restart≠reboot), y la realidad
>   del salto 4.3→4.15 (migración mayor: QNX 6.5→7.0 en 4.8, SMA gate, OEM EC-Net, recompilar módulos).
>
> **Distinción de focus:** el `jace8000` (B459–) RE-a la plataforma QNX embebida; ESTE captura el
> PROCEDIMIENTO del operador sobre las mismas features. El `jace8000-sd` (B674) da el lado en-disco de las
> licencias; `license-diff` (B386–B391/B442–B443) el diferencial SMA/autorización.
>
> **Base de evidencia:** doc oficial `[CERT-doc]` file:§ (16 guías niagara-help); valores de plataforma
> observados en vivo `[CERT-live]` (Platform Administration de HM_Central, 2026-09-07); la técnica de
> verificación no destructiva del passphrase es `[INFER]` (derivada de la regla [CERT-doc] "old passphrase
> required"). SECRETS DISCIPLINE: Host ID/valores enmascarados; solo estructura.

<!-- research-state.v1 -->
schema: research-state.v1
method: document-cycle-external
block_scope: shared-global
covered_blocks: 838, 849, 850
gaps_closed: 0
known_gaps: 5
investigable_open: 4
requires_execution_open: 1
blocked_open: 0
deferred_open: 0
undocumented_findings: 0
<!-- /research-state.v1 -->

## Coverage / open items

Outline cubierto 1/1 (document mode STOP: outline agotado, no gap-exhaustion). Gaps abiertos:
- **B838-G1** *(requires-execution)* — captura viva de la sesión de consola de restore en un JACE autorizado
  (numeración real del menú, tiempo de montaje) → `[CERT-live]`.
- **B838-G2** *(investigable)* — especificidad del licenciamiento Distech EC-Net: servidor de licencias
  marca-Distech vs `licensing.tridium.com`, y host/puerto exacto que marca el JACE (regla de firewall).
- **B838-G3** *(investigable)* — el procedimiento `.dist` de restore en detalle (Distribution File Installer
  vs Station Copier + prompts de passphrase) como recipe propia. **Parcial:** B849 cubre la ruta de EDICIÓN
  del BOG offline (passphrase desconocido); G3 sigue abierto para el lado `.dist`.
- **B850-G1** *(investigable)* — causa raíz del cuelgue del subsistema I/O (`platmstp` serial + stack IP) del
  JACE-8000 tras un reinicio por import de licencia: ¿defecto conocido Distech/Tridium (secure-storage SD,
  re-enumeración de módulo de opción, o race del daemon)? ¿hay build con fix? Para prevenirlo, no solo recuperar.
- **B849-G1** *(investigable)* — prompts de passphrase del Station Copier en la etapa copy-to-target
  (¿el destino re-cifra con SU system passphrase de forma automática, o vuelve a pedir?) como recipe anclado.

## B849 (document-mode capture 2026-09-09) — abrir/correr una copia de station offline: las 4 barreras

Outline 1/1 (COMPLETO end-to-end sobre la copia HARBOR HM_BMS): §1-6 **passphrase** (por qué pregunta el
Station Transfer Wizard; ruta conocido / desconocido con Bog File Protection tool → force passphrase — borra
passwords cifrados, NO lógica/Px; guardrail §6: no reemplazar la station de producción entera). §7
**módulos faltantes** (CannotLoadBogException/ModuleNotFoundException dashboard-rt; el decode necesita cada
module-part + sus <dependencies> en modules/; fix copiar sw/<ver>/<part>.jar, ojo sw/1.0 = stub; herramienta
tools/station-modules.py). §8 **install-commit** ("bad return code to commit file instance": la carpeta traía
un station anidado; un station válido = una sola carpeta con un config.bog raíz). §9 **"SecretBytes has been
closed"** al fijar el password offline (BBogSpace.getEncodingContext:173-207; fix confirmado: cerrar+reabrir
el bog, re-entrar passphrase, editar+Save en una sola sesión). Marcadores: §1-6 `[CERT-doc]`, §7/§8
`[CERT-live]`, §9 `[CERT]` código + `[CERT-live]` fix; guardrail/estructura `[INFER]`.

## B850 (document-mode capture 2026-09-09) — outage HARBOR HM_Central: "la licencia rompio comms" -> I/O colgado

Post-mortem del incidente en vivo (JACE-8000 QNX HM_Central). Tras un import de licencia + reinicio, cayo TODO
BACnet (MS/TP `devctl rx frame failed`/`errno 264` + IP `Host is down`). Cadena de exoneracion: licencia
descartada 2x (diff = superconjunto v4.4->v4.15 + reinstalar la vieja no cambio nada), features nuevas =
permisos no interruptores, Max Master 60 era el bueno, COM remap no ayudo, Router Table todo "Ok". Causa real =
subsistema I/O del JACE colgado (driver serial QNX + stack IP), disparado por el reinicio. FIX = power-cycle
FISICO del JACE (station-restart NO lo limpia). Arbol de decision + 2 licencias verificadas (JACE
Qnx-TITAN-...621D + Supervisor Win-D5C2-...A7CE, v4.15, SMA 2027). Todo [CERT-live] salvo NPB-8000 COM rules
[CERT-doc], ECB-PTU-107 [CERT-web], y el modelo de licencia [INFER]. Gap abierto: B850-G1.
