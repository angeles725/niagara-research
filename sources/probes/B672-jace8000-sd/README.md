# Evidence — JACE-8000 boot microSD (physical, read-only)

**Focus:** jace8000-sd · **Block:** B672 (bootstrap) · **Date:** 2026-08-30
**Marker:** `[CERT-hw]` — physical inspection of the operator's real JACE-8000 boot microSD.

## How captured
The microSD was inserted into the operator's Windows host. WSL does not auto-mount removable
drives; a `drvfs` read-only mount of `D:` did not attach, so the FAT32 boot partition was read
**read-only via Windows PowerShell interop** from WSL (`Get-ChildItem` / `Get-Content` /
`Get-FileHash` / `Get-Disk|Get-Partition` / raw byte read of the image header). No file on the
card was modified. READ-ONLY discipline over the subject was preserved.

## Card geometry (physical disk 1, USB "Mass Storage Device", MBR, 4,018,143,232 B ≈ 4 GB)
| Part | Type | Size (B) | Offset (B) | Windows-visible? |
|---|---|---|---|---|
| 1 | FAT32 (XINT13) | 134,217,728 | 1,048,576 | YES → drive `D:` (this evidence) |
| 2 | Unknown (QNX) | 3,576,692,736 | 135,266,304 | NO |
| 3 | Unknown (QNX) | 268,435,456 | 3,711,959,040 | NO |

Only partition 1 (FAT32 boot) is readable by Windows; partitions 2 and 3 are the QNX
filesystems and are opaque to Windows (would need raw imaging + a QNX-aware reader).

## Files on partition 1 (FAT32 boot), with sha256
| File | Size (B) | LastWrite | sha256 |
|---|---|---|---|
| mlo | 34,152 | 2015-10-07 | 6FD7994E0E87058B86EE096AA6D784489B46FB9EC940EFE09917EEB5AE8789F5 |
| u-boot.img | 268,984 | 2015-10-08 | 721AB762B5C11DB12421067642AC7BA5D63A55163A034366E68EF568B3B9D5F5 |
| uEnv.txt | 78 | 2026-08-19 | CEF2C5BB88E9E0300F81841126452EDF52CFC2E750017A6988CB5851E9710D6D |
| n4-titan-am335x.signed | 40,264,708 | 2026-08-19 | 15B5EBA1134D7A8CD7305D187FF140379E1F6806B798262CB0B778076D96E201 |
| fac.properties | 144 | 2022-04-25 | FF9C46437D767A0AA54DDBA8D52294D22BD5F1DD12EFEB5613D1C15C5A1780DF |
| System Volume Information\ | — | — | Windows metadata, not JACE content |

## SECRETS DISCIPLINE
`fac.properties` contains a factory credential (`facPw`). The preserved copy in this directory
(`fac.properties.redacted`) has the password value MASKED. The value is the well-known Honeywell
WEBs golden-image factory default; it is NOT committed verbatim. The large signed firmware image
(40 MB) is NOT copied into the repo — only its sha256 and header are recorded here.
