# Skill Registry — niagara-research
Generated: 2026-04-19

## User Skills (relevant to this workspace)

| Skill | Triggers | Relevance |
|-------|----------|-----------|
| `niagara-module-generator` | create module, new module, BComponent, @NiagaraProperty, BWebServlet, BAbstractService, module RT/UX, Niagara, Tridium | Only if user pivots from research to implementation |
| `engram:memory` | ALWAYS ACTIVE | Mandatory — save discoveries, conventions, gotchas proactively |
| `sdd-explore` | investigate idea, compare approaches | If user wants deep-dive on a sub-topic of the 3 bloques |

## Project Conventions

None detected. Workspace created fresh 2026-04-19 by prior session — contains only `NEXT_SESSION_PROMPT.md` seed.

## Compact Rules (auto-resolved for sub-agents)

### Research/investigation work
- Primary sources first: `docs/` folder in Honeywell install (PDFs, HTMLs)
- Read-only: never modify any file in `/mnt/c/Honeywell/*` or `C:\ProgramData\Niagara4.14\*`
- Decompile via `javap -c` only — no class-file modification
- Cross-reference every claim with file path + line number OR class name + method
- If a finding contradicts prior engram memory, STOP and report with evidence

### Memory protocol
- Save every non-obvious discovery with `mem_save` using topic_key `niagara/{area}/{subtopic}` where area ∈ {estructura, licensing, security}
- Update — don't duplicate — when refining a known topic
- Link every note in `niagara-mental-model.md` back to the engram topic key

### Language/tone
- Rioplatense Spanish, warm/direct mentor tone (Gentleman style)
- No emojis
- Explain the WHY behind every correction with technical reasoning
