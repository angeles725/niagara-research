# Niagara Research — Focus Index

> Multi-focus corpus (METHODOLOGY §16). Un target maduro con varios ejes paralelos de investigación.
> Todos los focuses comparten la numeración global de bloques (`niagara-mental-model-bloqueN.md`) y el
> mismo repo git/hook; se distinguen por su `RESEARCH-STATE-<focus>.md` y su topic key en engram
> (`research/niagara/<focus>/gaps`, `.../progress`).

| Focus | Estado | RESEARCH-STATE | Ámbito | Bloques |
|---|---|---|---|---|
| (base) | stopped | `RESEARCH-STATE.md` | Framework Niagara N4.14 completo (Capas 1-25) + audit Reflow v1.7.5 + OEM Honeywell/Spyder + native platform RE | B1–B130 |
| optimizersupervisor | paused | `RESEARCH-STATE-optimizersupervisor.md` | Install vivo OptimizerSupervisor N4.14.0.162 (config.bog de stations vivas) | B123 |
| platform-native | stopped | `RESEARCH-STATE-platform-native.md` | RE nativo de la plataforma (launchers, JNI, licensing/crypto, driver DLLs, daemon) | B124–B130 |
| protocols | stopped | `RESEARCH-STATE-protocols.md` | Wire-level de protocolos (Modbus/OPC/BACnet/Fox/LON/Sox) + integración LOGO!8 | B131–B137 |
| nmodsreflow | stopped | `RESEARCH-STATE-nmodsreflow.md` | Arquitectura backend del módulo OEM NiagaraMods Reflow v1.7.7 `-rt` (service, HTTP/WS, subsistemas) — CERRADO, hilo de seguridad consolidado | B138–B150 |
| nmodsreflow-ux | stopped | `RESEARCH-STATE-nmodsreflow-ux.md` | Capa cliente/browser del módulo NiagaraMods Reflow v1.7.7 `-ux` (módulo fino de registro/loaders + SPA Vue embarcada) — CERRADO, paridad frontend con el backend | B151-B155 |
| live-station | stopped | `RESEARCH-STATE-live-station.md` | Validación DINÁMICA (§12) de la station Niagara N4 VIVA en 127.0.0.1 (WSL mirrored). `live-install` → SECRETS DISCIPLINE. Etapa A (runtime) + Etapa B (14 defectos de B150 con usuario `API`) — CERRADO, 13/14 con veredicto vivo | B156–B162 |

## Focus activo

**(ninguno activo)** — `live-station` CERRADO 2026-07-02.

**live-station** (dinámico §12) — CERRADO 2026-07-02, 7 bloques B156-B162. Primera validación `[CERT-hw]`
end-to-end de la station Niagara N4 VIVA. Etapa A mapeó el runtime (Reflow 1.7.7 en `/nmodsreflow/`, usuario
`API`=HTTPBasicScheme, cert default). Etapa B verificó los 14 defectos de B150: **config-write sin auth
CONFIRMADO** (V1-V3/V12, read-level sobrescribe config, restore byte-idéntico), backups **auth-gated** (V4/V10
NO reproducen — §14 corrige B144), reads 500 con payloads triviales, V7/V8 (BQL) diferido al canal WS
(requires-execution). Cero secretos exfiltrados; station intacta (`bf70f28f`). §14: refina tesis uniforme de
B150 §150.1 (gate NO uniforme: config abierto, backups gated).

**nmodsreflow-ux** (capa cliente `-ux`) — CERRADO 2026-07-02, 5 bloques B151-B155, superficie cliente
completamente mapeada (registro de vistas → loaders/iframe → SPA Vue 2.6.14 → wiring REST/WS → seguridad
cliente). §14: corrigió B50 (Vue 2.7→2.6.14). Confirmó B143/B144/B145 desde el cliente. NEXT-ACTION =
verificación dinámica sobre station viva (requiere hardware/decisión humana).

**nmodsreflow** (backend `-rt`) — CERRADO 2026-07-02, 13 bloques B138-B150, superficie completamente mapeada,
síntesis de seguridad cross-focus en B150. Residual R3 (mount `/module/<name>/`) no perseguido.
