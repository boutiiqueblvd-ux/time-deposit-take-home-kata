# Project rules for AI agents

The solution lives in `java/` (Spring Boot 3.5, Java 17, Maven). Other language folders are the original kata and stay untouched.

## Hard constraints
- `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` is verified business logic: never change its signature or its results.
  `TimeDepositCalculatorCharacterizationTest` must stay green; never edit its reference copy of the original algorithm.
- Do not change the public API of `TimeDeposit`.
- The API has exactly two endpoints: `GET /time-deposits` and `POST /time-deposits/update-balances`. Do not add others.
- New interest plans are new `InterestPolicy` classes; do not modify existing policies to add a plan.

## Working style
- Analyse before changing code; write a characterization test before refactoring behaviour.
- Prefer the simplest design that meets the requirement; every new class must have a clear reason to exist.
- Document ambiguous requirements and the chosen interpretation in README.md ("Business Assumptions").
- Small atomic commits with conventional prefixes (`test:`, `refactor:`, `feat:`, `docs:`, `build:`).

## Commands (from `java/`)
- `mvn test` – all tests (Docker required for Testcontainers)
- `docker compose up --build` – run database and application
