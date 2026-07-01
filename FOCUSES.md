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
| **nmodsreflow** | **active** | `RESEARCH-STATE-nmodsreflow.md` | Arquitectura backend del módulo OEM NiagaraMods Reflow v1.7.7 `-rt` (service, HTTP/WS, subsistemas) | B138– |

## Focus activo

**nmodsreflow** — iniciado 2026-07-01, ángulo "Arquitectura backend -rt" (confirmado con el usuario).
Fuente: JAR embarcado `nmodsreflow.77` decompilado (Vineflower) en
`/home/cristian/modules/Prototipos/modulos/organized/nmodsreflow77/`. Próximo gap: R2 (canal WebSocket).
