# XA Bank Time Deposit API

Solution to the [Time Deposit Refactoring Kata](ASSIGNMENT.md). The implementation lives in [`java/`](java/);
the other language folders are the untouched starting points of the kata.

## Project Overview

A junior developer left a verified interest calculation (`TimeDepositCalculator.updateBalance`) without any API or
storage. This solution:

- refactors the calculation so new plans can be added without touching existing ones, **with bit-for-bit identical
  results** (proven by characterization tests written before the refactor);
- stores time deposits and withdrawals in PostgreSQL;
- exposes **exactly two** REST endpoints, documented with OpenAPI/Swagger.

**Stack:** Java 17, Spring Boot 3.5, Spring JDBC, PostgreSQL 16, Flyway, springdoc-openapi, JUnit 5, Testcontainers.

## Architecture

A lightweight hexagonal layout: the domain knows nothing about Spring, HTTP or SQL.

```
org.ikigaidigital
├── TimeDeposit, Withdrawal, TimeDepositCalculator   domain (TimeDeposit and the calculator keep their original location)
├── interest/                                         domain: one InterestPolicy per plan type
├── application/                                      use cases (TimeDepositService) + outbound port (TimeDepositRepository)
└── adapter/
    ├── web/                                          REST controller + response records (inbound adapter)
    └── persistence/                                  JDBC implementation of the port (outbound adapter)
```

| Component | Responsibility |
|---|---|
| `TimeDepositCalculator` | Applies one month of interest to each deposit and rounds it (original rounding kept). Picks the policy by plan type. |
| `InterestPolicy` | Extension point: the unrounded monthly interest of one plan type. |
| `AnnualRateInterestPolicy` | Shared rule of all current plans: `balance × annualRate / 12`, nothing during the first 30 days. |
| `Basic` / `Student` / `PremiumInterestPolicy` | One business rule each (rate and plan-specific day window). |
| `TimeDepositService` | `findAll` (deposits + withdrawals) and `updateAllBalances` (transactional monthly run). |
| `TimeDepositRepository` | Port that keeps the application layer independent of the database. |
| `JdbcTimeDepositRepository` | Plain SQL against the required tables. |
| `TimeDepositController` | The two endpoints. |

**Data flow of `POST /time-deposits/update-balances`:**
controller → `TimeDepositService.updateAllBalances()` (one transaction) → `repository.findAllForUpdate()` (row lock) →
`calculator.updateBalance(deposits)` → `repository.updateBalances(deposits)` → `204 No Content`.

### Database

Created by Flyway ([`V1`](java/src/main/resources/db/migration/V1__create_time_deposits_and_withdrawals.sql)),
demo data seeded by [`V2`](java/src/main/resources/db/migration/V2__seed_demo_data.sql).

| `timeDeposits` | | `withdrawals` | |
|---|---|---|---|
| `id` | INTEGER, PK | `id` | INTEGER, PK |
| `planType` | VARCHAR(50), NOT NULL | `timeDepositId` | INTEGER, NOT NULL, FK → `timeDeposits.id` (indexed) |
| `days` | INTEGER, NOT NULL | `amount` | NUMERIC(19,2), NOT NULL |
| `balance` | NUMERIC(19,2), NOT NULL | `date` | DATE, NOT NULL |

One time deposit has zero or more withdrawals.

## Running the Application

**Prerequisites:** Docker. For running outside Docker or running tests: JDK 17 and Maven 3.9+.

### Option 1 – everything in Docker

```bash
cd java
docker compose up --build
```

### Option 2 – database in Docker, application locally

```bash
cd java
docker compose up -d postgres
mvn spring-boot:run
```

The connection defaults to `jdbc:postgresql://localhost:5432/time_deposits` (user/password `time_deposits`, local
development only) and can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. Flyway creates the schema and
the demo data on startup.

### Tests

```bash
cd java
mvn test
```

Persistence and API tests start a real PostgreSQL with Testcontainers, so Docker must be running. Without Docker
those test classes are reported as **skipped** (not passed) and only the unit tests run — check the surefire summary.

## API Documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI contract: <http://localhost:8080/v3/api-docs>

**Triggering the endpoints from Swagger:** open the Swagger UI, expand an operation, click **Try it out**, then
**Execute**. Run `GET /time-deposits` first, then `POST /time-deposits/update-balances`, then `GET` again to see the
new balances.

### `GET /time-deposits`

Returns all time deposits with their withdrawals, ordered by id. Response with the demo data (abbreviated):

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

Applies one monthly interest cycle to every time deposit. No request body. Responds with `204 No Content`.

```bash
curl -X POST http://localhost:8080/time-deposits/update-balances
```

After one call on the demo data, deposit 2 (basic, day 45) goes from `1234567.00` to `1235595.81`, deposit 3
(student, day 200) from `5000.00` to `5012.50`, and deposit 6 (premium, day 90) from `20000.00` to `20083.33`;
the other demo deposits are outside their interest window and stay unchanged.

## Design Decisions

**`updateBalance` was preserved, not rewritten.** The README declares it correct, so its signature, its
mutate-in-place contract and its rounding expression (`new BigDecimal(interest).setScale(2, HALF_UP)`) were kept.
Before refactoring, [`TimeDepositCalculatorCharacterizationTest`](java/src/test/java/org/ikigaidigital/TimeDepositCalculatorCharacterizationTest.java)
was committed: it compares the calculator against a verbatim copy of the original algorithm across ~18,000
combinations of plan, boundary days and balances using exact `double` equality. It passed before and after the
refactor. `TimeDeposit` is unchanged; withdrawals are a separate `Withdrawal` record so the shared class did not need
a new field.

**Interest calculation uses the Strategy pattern.** Only the plan-specific `if/else` chain moved into
`InterestPolicy` implementations. A new plan (e.g. Gold) is one new class registered with the calculator; no existing
policy changes. Plans that do not fit "annual rate / 12" implement `InterestPolicy` directly. The 30-day rule lives in
`AnnualRateInterestPolicy` rather than in the calculator because the brief says it applies to all *existing* plans,
so a future plan may opt out.

**The architecture is deliberately small.** No inbound-port interfaces (the controller calls the single service
directly), no JPA entities or mappers (two tables, a handful of SQL statements), no factories. The one interface on the
application boundary, `TimeDepositRepository`, exists so the use cases are testable without a database
(see `TimeDepositServiceTest`).

**Exactly two endpoints.** Verified by a test that reads the generated OpenAPI contract and by a test that
unmapped routes such as `POST /time-deposits` or `GET /time-deposits/{id}` are rejected. Swagger UI and `/v3/api-docs`
are documentation endpoints, not part of the API.

**Concurrency.** The monthly run locks the rows (`SELECT … FOR UPDATE`) inside one transaction, so two simultaneous
runs apply two months of interest instead of overwriting each other.

## Business Assumptions

| Ambiguity | Interpretation | Reason |
|---|---|---|
| "1% / 3% / 5% interest" | Annual rates; each run pays one twelfth (`balance × rate / 12`). | This is what the verified `updateBalance` does. |
| What one `POST` means | One monthly interest cycle (as a monthly scheduler would trigger). Not idempotent: each call adds another month. | `updateBalance` applies exactly one month per call. |
| `days` after an update | Not changed by the update. | `updateBalance` does not change it; day counting is outside the scope of the brief. |
| First 30 days | Interest from day 31 (`days > 30`). | Original code. |
| Student "no interest after 1 year" | Interest up to and including day 365; none from day 366. | Original code (`days < 366`). |
| Premium "starts after 45 days" | Interest from day 46. | Original code (`days > 45`). |
| Unknown plan types | No interest, balance unchanged. Plan types are case-sensitive (`basic`, `student`, `premium`). | Original code. |
| Withdrawals | Returned by the GET endpoint only; they do not affect interest. | The brief does not link them to the calculation, and `updateBalance` does not use them. |
| Withdrawal fields in the response | `id`, `amount`, `date` (the deposit id is implied by nesting). | Not specified by the brief. |
| Balance precision | Calculated in `double` (unchanged domain), stored as `NUMERIC(19,2)`. | Required decimal column; storing two decimals removes floating point noise. |
| Creating deposits | Data is seeded by a Flyway migration. | Only two endpoints are allowed and neither creates deposits. |
| Table/column names | Quoted camelCase identifiers (`"timeDeposits"`, `"planType"`, …). | Match the required schema exactly in PostgreSQL. |
| Invalid input | Not handled. | Explicitly out of scope in the brief. |

## AI-Assisted Development

See [AI_ASSISTANCE.md](AI_ASSISTANCE.md).
