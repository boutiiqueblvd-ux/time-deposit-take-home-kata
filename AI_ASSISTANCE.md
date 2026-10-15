# AI-Assisted Development

This document describes how AI tools were used during the implementation of the XA Bank Time Deposit API, which decisions remained human-owned, and how generated changes were checked.

## Tools Used

| Tool | Setup / purpose |
|---|---|
| Claude Code (Anthropic) | Used in VS Code with agent capabilities for repository inspection, file editing, shell commands, Maven, Git, Docker, and test execution. |
| `CLAUDE.md` | Project-level instructions loaded by Claude Code. It records hard constraints, working conventions, and relevant commands, and is committed so the workflow can be reproduced. |
| Session prompt | A structured prompt defined the agent's role as a senior backend engineer and reviewer and specified the phased workflow below. It is committed verbatim in [`docs/ai/session-prompt.md`](docs/ai/session-prompt.md), together with the points where the session deliberately deviated from it. |

**Model/version:** Claude Opus 5.5 (`claude-opus-5-5`), used through the Claude Code VS Code extension.

## Reproducing the Setup

1. Install Claude Code (CLI or VS Code extension) and open the repository root.
2. Ensure the committed `CLAUDE.md` is present.
3. Start a session with the prompt in [`docs/ai/session-prompt.md`](docs/ai/session-prompt.md).

Model output is not deterministic, so a new session follows the same workflow and constraints but will not produce byte-identical code.

## Workflow Enforced by the Session Prompt

1. **Repository forensic analysis — no code changes.** Identify the stack, architecture, existing business rules, constraints, and risks.
2. **Design.** Propose the smallest architecture that meets the requirements and justify each class or abstraction.
3. **Incremental implementation.** For every step, report the change, reason, risk, and verification.
4. **Adversarial / hidden-test review.** Review the solution as an external evaluator and look for cases that could invalidate it.
5. **Documentation and atomic commits.** Keep changes reviewable and document the solution and important decisions.

Key constraints included treating `updateBalance` as verified business logic, preserving existing behavior, exposing exactly two business API endpoints, avoiding over-engineering, and preferring Testcontainers for database-backed tests.

## AI Contributions

| Area | Contribution |
|---|---|
| Analysis | Read the five language variants, extracted the stated business-rule boundaries (`> 30`, `< 366`, `> 45`), noted that interest is rounded rather than the balance, and identified ambiguities and risks. |
| Regression safety | Proposed and wrote a characterization test that compares the refactored calculator with a verbatim copy of the original algorithm. The test was committed before the refactor. |
| Refactoring | Extracted plan-specific rules into `InterestPolicy` strategies while retaining the original rounding expression and calculation contract. |
| Implementation | Assisted with Spring Boot setup, Flyway migrations, the JDBC adapter, application service, controller, and Docker configuration. |
| Tests | Assisted with policy boundary tests, a service test using an in-memory repository port, Testcontainers persistence and API tests, and a contract test for the exactly-two-endpoints requirement. |
| Documentation | Assisted with the README, this document, and `CLAUDE.md`. |

AI contributions were treated as proposed implementation work and were reviewed against the requirements and existing behavior before acceptance.

## Human Decisions and Review

### Refactoring `updateBalance`

The requirements created a tension between “do not touch `updateBalance`” and the kata's extensibility requirement. The AI identified the trade-off and outlined three options. The human decision was to refactor the method's internals behind characterization tests while retaining its signature, mutation contract, rounding expression, and tested results. This avoided freezing the implementation entirely or introducing a second calculator with duplicated rules.

### Scope and business rules

The human defined the agent's role, phased workflow, no-over-engineering constraint, and exactly-two-business-endpoints requirement. Ambiguous business assumptions were documented in the README rather than silently treated as requirements.

### Code review and acceptance

Generated code, assumptions, and design trade-offs were reviewed before committing. The review focused on the areas most likely to change behavior or violate the brief:

- Interest eligibility at the day boundaries (`> 30`, `< 366`, and `> 45`).
- Rounding the interest using the original expression rather than rounding the balance through a different calculation.
- Preserving the calculator's signature and mutate-in-place behavior.
- Keeping the API limited to the two required business endpoints.
- Transactional persistence and row-locking behavior.
- SQL schema compatibility and the documented API contract.

Only checks actually performed should be described as verified. The test command and test report should be checked again in the target environment before submission.

## Verification Approach

The verification strategy combines several levels:

1. **Characterization test:** compares the refactored calculation with the original algorithm over approximately 18,000 combinations of plan, boundary days, and balances, using exact `double` equality.
2. **Policy boundary tests:** exercise the interest eligibility windows and plan-specific rules.
3. **Service test:** exercises application behavior through an in-memory implementation of the repository port.
4. **Persistence and API tests:** use Testcontainers to exercise PostgreSQL-backed persistence and HTTP behavior.
5. **API contract test:** checks the generated OpenAPI contract for exactly two business endpoints; tests also cover rejection of example unmapped routes.
6. **Manual / adversarial review:** considers unknown plan types, repeated monthly updates, and concurrency behavior against the documented assumptions.

### Run the tests

From the repository root:

```bash
cd java
mvn test
```

Docker must be available for the Testcontainers persistence and API tests. When Docker is unavailable, those tests may be skipped; a skipped test is not a passing test. Check the Maven Surefire summary and record the actual result before making claims about the final run.

**Verification result for this submission (2026-10-15, Windows 11, Docker Desktop with Docker Engine 29.8.2, Testcontainers 1.21.4):**

- `mvn test`: **47 tests run, 0 failures, 0 errors, 0 skipped** — the persistence and API tests (including the check that the API matches `java/openapi.yaml`) ran against PostgreSQL 16 started by Testcontainers. The same suite runs in the GitHub Actions CI workflow on every push.
- `docker compose up --build`: image built, PostgreSQL healthy, application started. A manual `GET` → `POST /time-deposits/update-balances` (204) → `GET` sequence on the demo data produced the balances documented in the README, Swagger UI loaded, and `POST /time-deposits` returned 405.

## Why AI Was Used

- **Repository analysis and characterization-test generation:** helped inspect existing implementations systematically and compare many input combinations rather than relying only on a few hand-picked examples.
- **Routine framework wiring:** accelerated familiar setup work for Spring Boot, Flyway, JDBC, Testcontainers, and documentation.
- **Documentation:** helped turn implementation decisions and assumptions into a reviewable explanation.

## What Was Not Delegated

The human retained responsibility for:

- Choosing how far `updateBalance` could be changed.
- Defining the scope and non-negotiable constraints.
- Deciding how ambiguous business requirements should be represented.
- Reviewing generated code, assumptions, and trade-offs.
- Accepting the final implementation and its verification evidence.

## Summary

AI was used to accelerate analysis, implementation, tests, and documentation. It did not replace responsibility for the business rules or the final engineering decisions. The intended outcome was a small, testable solution whose key behavior and constraints can be inspected and verified by another developer.
