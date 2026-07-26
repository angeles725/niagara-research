# JACE-8000 / WEB-8000 — Documentación práctica (comisionamiento en sitio)

Esta carpeta reúne la **parte práctica** del trabajo con controladores JACE-8000 / WEB-8000:
comisionamiento real, conexión a equipos en obra y bitácora de sesiones. Es distinta del
resto de `niagara-research`, que es investigación / exploración / decompilación — pero se
guarda acá porque es el mismo dominio Niagara N4 y comparte hallazgos.

## Convención de los documentos

Todo documento nuevo de esta carpeta arranca con este bloque de metadatos:

```
| Campo | Valor |
|-------|-------|
| **Fecha** | AAAA-MM-DD |
| **Proyecto (obra)** | <nombre de obra / cliente> |
| **Corpus / repo** | niagara-research (parte práctica) |
| **Equipo** | <modelo> "<nombre>" — <IP> |
| **Host ID** | <host id> (Niagara <versión>) |
| **Estación de ingeniería** | <supervisor / Workbench> |
```

Los archivos de bitácora se nombran `AAAA-MM-DD-<tema>.md` para orden cronológico.

## Equipos (no confundir Host IDs)

| Apodo | Host ID | Niagara | Documento base |
|-------|---------|---------|----------------|
| **CASINO** (activo) | `Qnx-TITAN-44A2-A77A-8526-363E` | Nuevo de fábrica (sin runtime) | Manual de inicio (abajo) |
| Alser / UN-RL1644 | `Qnx-TITAN-BB4C-…` | N4.14 | `documentacion-jace8000.docx` |
| Plan 8 Spyder | `Qnx-TITAN-BE9D-…` | N4.12 | `../../Manuales_Spyder_JACE8000/…` |

## Índice

| Fecha | Documento | Tema |
|-------|-----------|------|
| **2026-07-19** | **[Manual_Inicio_JACE8000_CASINO.docx](Manual_Inicio_JACE8000_CASINO.docx)** | **Manual de arranque en Word** (equipo nuevo de fábrica → station corriendo). Generador: `gen_manual_inicio.py` |
| 2026-07-19 | [2026-07-19-jace-casino-platform-siguientes-pasos.md](2026-07-19-jace-casino-platform-siguientes-pasos.md) | Estado real del JACE CASINO (nuevo, sin licencia ni station) + siguientes pasos (Commissioning Wizard) |
| 2026-07-07 | [2026-07-07-jace-casino-mac-dos-interfaces.md](2026-07-07-jace-casino-mac-dos-interfaces.md) | MAC address vs Host ID; por qué el JACE tiene dos interfaces Ethernet |
| 2026-03 | [documentacion-jace8000.md](documentacion-jace8000.md) (+`.docx`) | Manual de comisionamiento de otro JACE (Alser, Host ID BB4C, N4.14) |

Capturas de evidencia en `img/` (License Manager, Application Director, Platform Administration, Software Manager — 2026-07-19).

## Documentos relacionados (fuera de esta carpeta)

- `../../Manuales_Spyder_JACE8000/Plan_Comisionamiento_Spyder_WEB8000.docx` — Plan formal de enlace de los 8 Spyder (cotización ENL-SPYDER). Fases 0 / A-E que sigue esta bitácora.

> ⚠️ **CONFIDENCIAL:** varios documentos contienen credenciales, Host ID y MAC reales. Si se versiona en git, considerar redacción o `.gitignore`.
