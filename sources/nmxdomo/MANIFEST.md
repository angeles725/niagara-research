# Source: NiagaraMods Domo (nmxdomo) — Niagara custom login module

Vendor package **NiagaraMods Domo 1.0 Beta #4** (March 22, 2024). Provided by the
operator (Cristian) on 2026-09-06. Domo replaces / customizes the Niagara WebService
login screen by shipping three `web:LoginTemplate` implementations.

## Provenance

| Artifact | Path | sha256 | Notes |
|---|---|---|---|
| Original package (zip) | `C:\Users\equipo\Downloads\nmxdomo-b4.zip` (WSL: `/mnt/c/Users/equipo/Downloads/nmxdomo-b4.zip`) | `85e2621771c6c39d6bf872b09867e31331386242e48fbb7485b50dc8ffbea3a0` | vendor package, lives on the Windows side (not copied whole into the repo) |
| Module runtime jar | inside zip → `nmxdomo-rt.jar` | `aa0a003f0fbdfc0b9d7fa6b587b9a620abcaa73e60d9f260d72dbef091192679` | `nmxdomo-rt` vendorVersion 1.0.0.4, depends web-rt/baja 4.10; bundles jknack handlebars + antlr4 + commons-lang3/text |
| Example themes (code) | `sources/nmxdomo/example-themes/` (copied) | — | basic (from-scratch base), built-ins/panel-left, built-ins/floating-card, logo, unsplash — each a `login.hbs` (+ `domo.css`) |

## Vendor README (Domo Beta #4)

The full vendor README was supplied verbatim in the research session that created
Block 832 (custom-login focus). Key facts are captured there under `[CERT-web]`.
Headline points: supports Niagara 4.10–4.13+; digest auth only (no SSO); no
Lexicon/localization; three templates `DomoPanelLeft` / `DomoFloatingCard` (turn-key)
and `DomoUserCustom` (bring your own HTML/HBS/JS/CSS from `file:^domo/`); Handlebars
templating (handlebars.java); a `$domo` browser JS API (`login`, `check2FA`,
`twoFactorLogin`, `resetPass`, `checkPassword`, `unsplash`); requires WebService CSP
edits for external fonts/animations/Unsplash. Known issue: 4.14 Beta 2FA + password
reset; Firefox login bug.

## Registered module types (`nmxdomo-rt.jar` META-INF/module.xml)

- `com.niagaramods.nmxdomo.BDomoFloatingCard` → name `DomoFloatingCard`
- `com.niagaramods.nmxdomo.BDomoPanelLeft` → name `DomoPanelLeft`
- `com.niagaramods.nmxdomo.BDomoUserCustom` → name `DomoUserCustom`
- `com.niagaramods.nmxdomo.BDomoLoginTemplate` (17.5 KB engine class; the three above are thin subclasses)
