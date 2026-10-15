# XA Bank Time Deposit API

Solution to the [Time Deposit Refactoring Kata](ASSIGNMENT.md). The implementation lives in [`java/`](java/); the other language folders are the untouched starting points of the kata.

## Project Overview

A junior developer left a verified interest calculation (`TimeDepositCalculator.updateBalance`) without an API or persistence layer. This solution:

- Refactors the calculation so new plans can be added without modifying existing plan policies, while preserving the original calculation results across the characterization-test cases.
- Stores time deposits and withdrawals in PostgreSQL.
- Exposes **exactly two REST endpoints**, documented with OpenAPI/Swagger.

**Stack:** Java 17, Spring Boot 3.5, Spring JDBC, PostgreSQL 16, Flyway, springdoc-openapi, JUnit 5, Testcontainers.

## Architecture

The application uses a lightweight hexagonal layout: the domain does not depend on Spring, HTTP, or SQL.

```text
org.ikigaidigital
├── TimeDeposit, Withdrawal, TimeDepositCalculator
│   └── Domain model and calculator; original location retained
├── interest/
│   └── Domain policies: one InterestPolicy per plan type
├── application/
│   └── Use cases (TimeDepositService) and outbound port (TimeDepositRepository)
└── adapter/
    ├── web/
    │   └── REST controller and response records (inbound adapter)
    └── persistence/
        └── JDBC implementation of the repository port (outbound adapter)
```

| Component | Responsibility |
|---|---|
| `TimeDepositCalculator` | Applies one monthly interest cycle to each deposit, retains the original rounding expression, and selects the policy for the plan type. |
| `InterestPolicy` | Extension point that calculates the unrounded monthly interest for one plan type. |
| `AnnualRateInterestPolicy` | Shared behavior for current plans: `balance × annualRate / 12`, with no interest during the first 30 days. |
| `BasicInterestPolicy`, `StudentInterestPolicy`, `PremiumInterestPolicy` | Encapsulate the rate and plan-specific day window for each plan. |
| `TimeDepositService` | Implements `findAll` (deposits and withdrawals) and `updateAllBalances` (transactional monthly update). |
| `TimeDepositRepository` | Outbound port that keeps the application service independent of the database implementation. |
| `JdbcTimeDepositRepository` | Executes SQL against the required PostgreSQL tables. |
| `TimeDepositController` | Exposes the two REST endpoints. |

**Data flow for `POST /time-deposits/update-balances`:**

```text
Controller
  → TimeDepositService.updateAllBalances() (transaction)
  → repository.findAllForUpdate() (row lock)
  → calculator.updateBalance(deposits)
  → repository.updateBalances(deposits)
  → 204 No Content
```

## Database

The schema is created by Flyway migration [`V1`](java/src/main/resources/db/migration/V1__create_time_deposits_and_withdrawals.sql). Demo data is seeded by [`V2`](java/src/main/resources/db/migration/V2__seed_demo_data.sql).

| Table | Column | Type / constraint |
|---|---|---|
| `timeDeposits` | `id` | `INTEGER`, primary key |
| `timeDeposits` | `planType` | `VARCHAR(50)`, not null |
| `timeDeposits` | `days` | `INTEGER`, not null |
| `timeDeposits` | `balance` | `NUMERIC(19,2)`, not null |
| `withdrawals` | `id` | `INTEGER`, primary key |
| `withdrawals` | `timeDepositId` | `INTEGER`, not null, foreign key to `timeDeposits.id` (indexed) |
| `withdrawals` | `amount` | `NUMERIC(19,2)`, not null |
| `withdrawals` | `date` | `DATE`, not null |

A time deposit can have zero or more withdrawals.

## Running the Application

### Prerequisites

- Docker
- JDK 17 and Maven 3.9+ if running the application locally or running tests

### Option 1: Run everything in Docker

```bash
cd java
docker compose up --build
```

### Option 2: Run PostgreSQL in Docker and the application locally

```bash
cd java
docker compose up -d postgres
mvn spring-boot:run
```

The default connection is `jdbc:postgresql://localhost:5432/time_deposits`, with the local-development credentials `time_deposits` / `time_deposits`. Override the connection settings with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Flyway creates the schema and seeds the demo data at startup.

## Tests

```bash
cd java
mvn test
```

Persistence and API tests use Testcontainers to start PostgreSQL, so Docker must be running for those tests. When Docker is unavailable, those test classes are reported as **skipped**, not passed; only the tests that can run without Docker execute. Check the Maven Surefire summary to distinguish passed, failed, and skipped tests.

## API Documentation

When the application is running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI contract: <http://localhost:8080/v3/api-docs>

To try the endpoints in Swagger UI, open an operation, select **Try it out**, and then select **Execute**. Run `GET /time-deposits`, then `POST /time-deposits/update-balances`, and then `GET /time-deposits` again to inspect the updated balances.

### `GET /time-deposits`

Returns all time deposits with their withdrawals, ordered by deposit ID. Example response, abbreviated:

```bash
curl http://localhost:8080/time-deposits
```

```json
[
  {
    "id": 2,
    "planType": "basic",
    "balance": 1234567.00,
    "days": 45,
    "withdrawals": [
      { "id": 1, "amount": 500.00, "date": "2026-01-15" },
      { "id": 2, "amount": 250.00, "date": "2026-02-15" }
    ]
  },
  {
    "id": 3,
    "planType": "student",
    "balance": 5000.00,
    "days": 200,
    "withdrawals": []
  }
]
```

### `POST /time-deposits/update-balances`

Applies one monthly interest cycle to every time deposit. The request has no body and responds with `204 No Content`.

```bash
curl -X POST http://localhost:8080/time-deposits/update-balances
```

After one call with the demo data, the documented expected changes are:

- Deposit 2 (`basic`, day 45): `1234567.00` → `1235595.81`
- Deposit 3 (`student`, day 200): `5000.00` → `5012.50`
- Deposit 6 (`premium`, day 90): `20000.00` → `20083.33`

The other demo deposits are outside their interest windows and remain unchanged.

## Design Decisions

### Preserve calculation behavior while refactoring internals

The verified `updateBalance` behavior was preserved while its internals were refactored. The method's signature, mutate-in-place contract, and original rounding expression (`new BigDecimal(interest).setScale(2, HALF_UP)`) were retained.

Before the refactor, `TimeDepositCalculatorCharacterizationTest` was committed. It compares the calculator with a verbatim copy of the original algorithm across approximately 18,000 combinations of plan, boundary days, and balances. The test uses exact `double` equality and was reported to pass before and after the refactor. This is evidence of identical results for the tested combinations, not a proof for every possible input. `TimeDeposit` remains unchanged; withdrawals are represented by a separate `Withdrawal` record so the shared class did not need a new field.

### Strategy pattern for interest policies

Only the plan-specific conditional logic moved into `InterestPolicy` implementations. Adding a plan such as Gold can be done by adding and registering a new policy without changing existing policies. Plans that do not fit the annual-rate-divided-by-12 rule can implement `InterestPolicy` directly.

The 30-day rule lives in `AnnualRateInterestPolicy` because the brief says it applies to all existing plans. A future plan can opt out by using a different implementation.

### Deliberately small architecture

There are no inbound-port interfaces, JPA entities, mappers, or factories. The controller calls the single application service directly; JDBC is sufficient for the small number of tables and SQL statements. The `TimeDepositRepository` interface exists at the application boundary so use cases can be tested without a database, as shown by `TimeDepositServiceTest`.

### Exactly two business API endpoints

The endpoint count is checked against the generated OpenAPI contract, and unmapped routes such as `POST /time-deposits` and `GET /time-deposits/{id}` are tested as rejected. Swagger UI and `/v3/api-docs` are documentation endpoints, not business API endpoints.

### Concurrency

The monthly update is designed to lock the deposit rows using `SELECT ... FOR UPDATE` within a transaction. This is intended to prevent concurrent updates from overwriting each other's work. The actual guarantee depends on the transaction boundaries and database behavior; see the persistence implementation and tests when reviewing this claim.

## Business Assumptions

| Ambiguity | Interpretation | Reason |
|---|---|---|
| “1% / 3% / 5% interest” | Annual rates; one run pays one twelfth (`balance × rate / 12`). | This matches the verified `updateBalance` calculation. |
| Meaning of one `POST` | One monthly interest cycle. The operation is not idempotent: each call adds another month of interest when a deposit qualifies. | The original calculation applies one monthly cycle per call. |
| `days` after an update | The value is not changed by the update. | The original calculation does not change it; day counting is outside the scope of the brief. |
| First 30 days | Interest starts on day 31 (`days > 30`). | Original business rule. |
| Student plan after one year | Interest is applied through day 365; not from day 366 (`days < 366`). | Original business rule. |
| Premium plan start | Interest starts on day 46 (`days > 45`). | Original business rule. |
| Unknown plan types | No interest; balance remains unchanged. Plan names are case-sensitive (`basic`, `student`, `premium`). | Original behavior. |
| Withdrawals | Returned by the GET endpoint; they do not affect interest. | The brief does not connect withdrawals to the calculation, and `updateBalance` does not use them. |
| Withdrawal fields in the response | `id`, `amount`, and `date`; the deposit ID is implied by nesting. | The brief does not specify these response details. |
| Balance precision | The domain calculation remains in `double`; the database stores balances as `NUMERIC(19,2)`. | The domain model is unchanged and the database column requires decimal precision. |
| Creating deposits | Demo data is inserted by Flyway. | Only two business endpoints are allowed, and neither creates deposits. |
| Table and column names | Quoted camelCase identifiers such as `"timeDeposits"` and `"planType"`. | Matches the required PostgreSQL schema. |
| Invalid input | No additional validation is implemented. | Explicitly outside the scope of the brief. |

## AI-Assisted Development

See [`AI_ASSISTANCE.md`](AI_ASSISTANCE.md) for the tools, workflow, division of responsibilities, and verification approach used during development.