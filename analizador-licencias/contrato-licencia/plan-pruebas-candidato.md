# Plan de pruebas candidato (esqueleto Anexo I/J) — controles de licencia Niagara N4

> **Qué es.** Catálogo de pruebas candidatas por área de control, derivado de los hallazgos ya documentados en
> el corpus `niagara-research`. Define, por prueba: el control bajo evaluación, la hipótesis, el criterio
> objetivo de éxito/fracaso y la evidencia clave — a nivel de **alcance/scoping**, no de procedimiento operativo.
> El detalle de ejecución se llena en `expediente-tecnico-plantilla.md` (§6.3), **una copia por prueba**.
>
> **NO ES EJECUCIÓN.** Ninguna prueba se inicia hasta que se cumpla el bloque de autorización de abajo. Este
> documento no contiene pasos de circunvención; describe *qué* se evaluaría y *cómo se mediría* el resultado.
>
> Fecha: 2026-08-10 · Deriva de: gaps `signing-pki` (SP-G3/G6/G8/G9a) + bloques citados.

---

## Precondición de autorización (bloquea TODO el plan)

- [ ] Acuerdo firmado auténticamente por representante **real e identificable** de Tridium con facultades.
- [ ] Alcance (producto, versión, host, cuenta, entorno) aprobado en **Anexo I** o **Acta del Anexo J**.
- [ ] Entorno = **laboratorio aislado**, copias de prueba, sin producción ni datos reales.
- [ ] Responsables (técnico + contractual), ventana y **criterio de aborto** definidos por prueba.

> Sin esto marcado, el plan queda en estado `BORRADOR / NO AUTORIZADO`.

## Convenciones

- **ID:** `TP-<área>-<n>`. **Éxito** = la evasión/alteración se demuestra. **Fracaso** = el control resiste
  (resultado igualmente valioso: documenta que el control es robusto).
- **Regla de secuencia:** primero **línea base legítima** (comportamiento normal), luego pruebas
  **no destructivas** (lectura/observación), y solo al final las que modifican copias de laboratorio.
- Cada fila enlaza al bloque que fundamenta la hipótesis y al gap del corpus cuando aplica.

---

## A. Identidad de host (host-id) — fundamento: B424

| ID | Control bajo prueba | Hipótesis (de B424) | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-HOST-01 | Dependencia del binding en las 4 fuentes | El host-id es un *fold* XOR **no criptográfico** de 8 bytes sobre 4 fuentes → alterar una fuente en un clon de laboratorio cambia el host-id | Éxito: cambia el host-id y la validación reacciona de forma predecible · Fracaso: host-id estable | host-id antes/después, log de validación de licencia |
| TP-HOST-02 | **Forjabilidad** del host-id | Al ser fold no-cripto, un host-id objetivo podría reproducirse fabricando las 4 fuentes en laboratorio | Éxito: se reproduce un host-id objetivo en lab aislado · Fracaso: no reproducible | derivación registrada de las 4 fuentes, host-id resultante |

*Riesgo/nota:* TP-HOST-02 evalúa una debilidad de diseño ya señalada (no-cripto). Solo en clon aislado; sin uso
de identidades reales de producción.

## B. Firma de módulo — fundamento: B379, B392, B395

| ID | Control bajo prueba | Hipótesis | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-SIG-01 | Flags `skip-*` de `nverify` | B379: existen 4 flags `skip-*`; ¿permiten cargar un módulo de firma inválida? | Éxito: el bypass carga el módulo alterado · Fracaso: rechazo / `exit(-6)` | salida de `nverify`, código de salida |
| TP-SIG-02 | Fail-closed del verificador | B379/B392: firma requerida inválida → `System.exit(-6)` | Éxito (evasión): carga pese a firma alterada · Fracaso (robusto): `exit(-6)` | log de arranque, exit code |
| TP-SIG-03 | Dependencia del root oculto | B395: la confianza cuelga de un root DSA-1024 embebido en `baja.jar` | Éxito: sustituir el ancla permite inyectar un vendor cert propio en lab · Fracaso: la cadena resiste | cadena de verificación antes/después |

## C. Entitlements / features — fundamento: B126 §126.6, B387

| ID | Control bajo prueba | Hipótesis | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-ENT-01 | Gate nativo texto vs. verificación DSA | B126 §126.6: `isFeaturePresent` nativo = **coincidencia de TEXTO**, no verificación DSA (contraste con B323/B395 lado Java) | Éxito: el gate nativo acepta un feature-texto que la verificación DSA rechazaría · Fracaso: consistencia entre ambos | resultado del gate nativo vs. verificación Java |
| TP-ENT-02 | Modo sin licencia | B387: sin licencia corre **UNCAPPED** (límites → `MAX_VALUE`), no deshabilitado; heap-limit → `exit(-3)` | Éxito: se confirma operación uncapped en lab · Fracaso: comportamiento distinto al documentado | límites efectivos observados, logs |

## D. Expiración — fundamento: B126, B392 (área **parcial** en el mapa)

| ID | Control bajo prueba | Hipótesis | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-EXP-01 | Fail-closed ante licencia vencida (= SP-G3) | El efecto runtime de una licencia expirada no está confirmado empíricamente | Éxito (evasión): sigue operando expirada · Fracaso (robusto): fail-closed en boot | log de arranque con licencia de prueba expirada |

## E. Certificados y revocación — fundamento: B392, B287 (CRL **parcial**)

| ID | Control bajo prueba | Hipótesis | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-CRL-01 | Enforcement de revocación (= SP-G6) | B287: `BIssuerCertAndCrl` modelado; enforcement `[INFER]` | Éxito (evasión): cert revocado aceptado · Fracaso (robusto): conexión rechazada | traza de handshake BACnet/SC o TLS con cert revocado |
| TP-TRUST-01 | Truststore password default | B392: `truststore.jks` con password `changeit` | Éxito: inyectar un cert de firma propio en el truststore permite cargar módulos auto-firmados en lab · Fracaso: bloqueado por otra capa | contenido del truststore antes/después, resultado de carga |

## F. Validación de integridad — fundamento: B393, B396

| ID | Control bajo prueba | Hipótesis | Éxito / Fracaso | Evidencia clave |
|---|---|---|---|---|
| TP-INT-01 | Tamper de datos no firmados | B393: audit/history/`.bog` **sin** firma/MAC/checksum (solo GCM per-campo) | Éxito (evasión de integridad de datos): alteración no detectada · Fracaso: detectada | hash antes/después, resultado de cualquier verificación del sistema |
| TP-INT-02 | Registro syslog como evidencia | B396: syslog offload = record **plano**, sin firma per-mensaje | Éxito: un registro puede alterarse sin la firma → es resistencia, no evidencia · Fracaso: existe integridad per-mensaje | registro original vs. alterado |

---

## Mapa a gaps del corpus y a la plantilla §6.3

| Prueba | Gap `signing-pki` | Área del mapa | Campo §6.3 que la formaliza |
|---|---|---|---|
| TP-HOST-01/02 | (nuevo, deriva de B424) | 1. host-id | b (hash), d (baseline), g (tabla de cambios) |
| TP-SIG-01/02/03 | SP-G8 (parcial) | 2. firma de módulo | e (procedimiento), h (binarios antes/después) |
| TP-ENT-01/02 | (deriva de B126/B387) | 3. entitlements | c (hipótesis), e |
| TP-EXP-01 | **SP-G3** | 4. expiración | d (baseline), i (log de arranque) |
| TP-CRL-01 | **SP-G6** | 5. certificados | i (traza/pcap) |
| TP-TRUST-01 | (deriva de B392) | 5. certificados | h, i |
| TP-INT-01/02 | **SP-G7** (cerrado) → verificación | 6. integridad | h (hashes), i (evidencia) |
| TP-SIG-* / provider | SP-G9a | 5. certificados/provider | e, i |

**Orden sugerido de ejecución (cuando se autorice):** primero las de **observación** (TP-ENT-02, TP-EXP-01,
TP-INT-02, TP-CRL-01) que no modifican binarios; luego las de **modificación en copia de laboratorio**
(TP-SIG-*, TP-ENT-01, TP-HOST-*, TP-TRUST-01, TP-INT-01), cada una con su expediente §6.3, su criterio de
aborto y su mecanismo de restauración.

> **Recordatorio (§6.3 cierre + §6.4):** evidencia insuficiente → `NO VERIFICADA` / `NO REPRODUCIBLE`; sin
> publicación de métodos de evasión; sin uso fuera de los fines de seguridad acordados; retención según §8.4.
