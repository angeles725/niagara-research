# §18 retro — focus license-diff (B386-B389, 2026-08-07)

Focus CLOSED 6/6. Answer: a Niagara license changes ONLY the `security/` subtree on disk (B386) + runtime
feature gates (B387); modules/bin/config differ by vendor/version/user (B388/B389).

## Proposed kit delta

- **LD-A (MED, PROMPT-LOOP e2 pre-flight extension):** before a TWO-ARTIFACT DIFF, verify both artifacts are
  the same TYPE (installed instance vs installer-package vs dist-archive). The user-selected "unlicensed"
  install here was actually an INSTALLER PACKAGE (908 files, no bin/security/defaults) — a full-install-vs
  -installer diff is dominated by artifact-type noise, not the target axis. e2 checks a source EXISTS + its
  SIZE; it should also check the COMPARISON PAIR is type-compatible when the gap is a diff. Evidence: B386 §386.2.
- **LD-B (LOW, finding worth a methodology example):** "absent-feature = uncapped, not disabled" — an unlicensed
  Niagara station runs with limits set to Integer.MAX_VALUE (B387 §387.4). A good example of RE-MEASURE /
  FALSIFY: the intuitive assumption (unlicensed = crippled) is wrong; the code sets uncapped. Already covered by
  existing FALSIFY-BEFORE-REPORTING rule — cite as an instance, not a new delta.

## Already-covered (not re-proposed)
3-axis attribution (version/vendor/license) is just honest [INFER] discipline; diffoscope tool acquisition is
normal PROVISION; RE-PAIR when a source is unsuitable is GAP-PREMISES-ARE-HYPOTHESES + source-before-agent.
