# AI-Assisted Development

## Tools Used

| Tool | Setup |
|---|---|
| **Claude Code** (Anthropic, Claude Opus 5.5) | VS Code extension, agent mode with file editing, shell (Maven, git, Docker) and test execution. |
| **[`CLAUDE.md`](CLAUDE.md)** | Project rules loaded automatically by Claude Code in every session (hard constraints, working style, commands). Committed so the setup is reproducible. |
| **Session prompt** | A structured prompt defining the agent's role (senior backend engineer / reviewer) and a phased workflow, summarised below. |

### Reproducing the setup

1. Install Claude Code (CLI or VS Code extension) and open the repository root.
2. `CLAUDE.md` is picked up automatically.
3. Start the session with a prompt following the workflow below.

### Workflow enforced by the session prompt

1. **Repository forensic analysis**, no code: stack, architecture, existing business rules, risks.
2. **Design**: smallest architecture that meets the requirements; justify every class.
3. **Incremental implementation**: for each step report *change, reason, risk, verification*.
4. **Hidden-test review**: act as the external evaluator and try to reject the solution.
5. **Documentation and atomic commits.**

Key rules from the prompt: treat `updateBalance` as verified business logic; preserve existing behaviour; exactly two
endpoints; avoid over-engineering; prefer Testcontainers.

## AI Contributions

| Area | What the AI did |
|---|---|
| Analysis | Read all five language variants, extracted the exact business rules (`> 30`, `< 366`, `> 45`, rounding of the interest rather than the balance) and listed ambiguities and risks. |
| Regression safety | Proposed and wrote the characterization test that compares the refactored calculator with a verbatim copy of the original algorithm, committed *before* the refactor. |
| Refactoring | Extracted the plan rules into `InterestPolicy` strategies while keeping the original rounding expression. |
| Implementation | Spring Boot setup, Flyway migrations, JDBC adapter, service, controller, Docker files. |
| Tests | Policy boundary tests, service test with an in-memory port, Testcontainers persistence and API tests (including the "exactly two endpoints" contract test). |
| Documentation | README, this file, `CLAUDE.md`. |

## Human Decisions

- **Refactor `updateBalance`'s internals vs. leave it byte-for-byte untouched.** The AI flagged a conflict between
  "do not touch `updateBalance`" and the kata's requirement for an extensible design, and offered three options. The
  human chose to refactor the internals behind characterization tests, keeping signature and results identical,
  instead of leaving the method frozen (no extensibility) or adding a parallel calculator (duplicated rules).
- **Scope and rules:** the role, the phased workflow, the "no over-engineering" rule and the exact-two-endpoints
  constraint were defined by the human in the session prompt.
- **Review:** all generated code, assumptions (see README "Business Assumptions") and trade-offs were reviewed before
  committing.

## Why AI was used where it was

- **Analysis and characterization tests:** fast, exhaustive reading of the existing code and generation of
  large comparison grids where a human would sample a few cases.
- **Boilerplate (Spring/Flyway/Testcontainers wiring, docs):** low-risk, well-known patterns.
- **Not delegated:** the decision on how far `updateBalance` may change, and acceptance of business assumptions.
