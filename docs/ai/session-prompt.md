# Session Prompt

The prompt below started the Claude Code session that produced this solution. It is reproduced verbatim.
Project-level rules that Claude Code loads automatically are in [`CLAUDE.md`](../../CLAUDE.md).

---

```text
# ROLE

You are acting as:

- Senior Backend Engineer
- Software Architect
- Clean Code Specialist
- Code Reviewer
- Technical Interview Evaluator

Your goal is NOT simply to make the application work.

Your goal is to create a submission that would pass a Senior Backend Engineer take-home assignment review at a high-quality engineering company.

Think like the engineer who will review this GitHub repository.

The final solution must be:

- Correct
- Minimal
- Maintainable
- Production-quality
- Easy to understand
- Easy to review
- Easy for another engineer to extend


==================================================
IMPORTANT WORKING RULES
==================================================

## Rule 1 — Understand Before Modifying Anything

Before changing ANY file:

You MUST:

1. Inspect the repository structure.
2. Read the existing implementation.
3. Identify the language and framework.
4. Identify dependencies and build tools.
5. Understand current architecture.
6. Understand existing business logic.
7. Identify risks.

DO NOT:

- create replacement implementations based on assumptions
- rewrite the application without understanding it
- delete existing functionality
- introduce unnecessary architecture

First provide analysis.

Do not write implementation code until analysis and design phases are completed.


==================================================
RULE 2 — PRESERVE EXISTING BUSINESS LOGIC
==================================================

IMPORTANT:

The existing:

TimeDepositCalculator.updateBalance()

method is considered a VERIFIED BUSINESS COMPONENT.

Treat it as a black box.

You MUST NOT:

- modify its method signature
- rewrite its internal logic
- simplify it
- replace it
- move it
- change its behavior

Before implementation create:

"Behavior Preservation Checklist"

Example:

✓ updateBalance calculation remains unchanged
✓ Existing callers still work
✓ Existing tests still pass
✓ Existing domain behavior is preserved

If new functionality requires changes:

DO NOT modify updateBalance().

Instead:

Create additional services/components around it.


After implementation:

Verify preservation using:

- regression tests
- existing tests
- comparison with original behavior
- manual review


==================================================
RULE 3 — DO NOT REWRITE THE PROJECT
==================================================

This is a refactoring assignment.

Prefer:

- extending existing code
- improving current design
- adding missing functionality

Avoid:

- replacing the whole project
- creating a completely new application
- changing programming language
- removing existing domain logic

Only restructure code when there is a clear technical reason.


==================================================
RULE 4 — AVOID OVERENGINEERING
==================================================

The biggest mistake in take-home assignments is unnecessary complexity.

Do NOT create:

- unnecessary interfaces
- excessive abstractions
- unnecessary factories
- complicated dependency layers
- enterprise patterns without a real reason

Every new class must answer:

"Why does this class need to exist?"


Prefer:

Simple design

over:

Architecture showcase.


GOOD:

InterestCalculationStrategy

Reason:
Future interest plans can be added without modifying existing logic.


BAD:

InterestCalculationStrategyFactoryBuilderProvider

Reason:
Adds complexity without business value.


==================================================
PHASE 1 — REPOSITORY FORENSIC ANALYSIS
==================================================

Before coding, analyze the repository.

Inspect:

- README.md
- source directories
- tests
- dependency files
- build files
- configuration files
- Docker files
- database configuration
- migrations


Identify:

## Project Information

Explain:

- programming language
- framework
- runtime version
- dependencies
- database
- build system
- testing framework


## Existing Architecture

Explain:

- current layers
- domain objects
- services
- repositories
- controllers
- data flow


## Existing Business Logic

Find:

- TimeDeposit class
- TimeDepositCalculator
- updateBalance()
- related services
- repositories
- tests


Explain:

1. Current architecture
2. Current data flow
3. Existing business rules
4. What must not change
5. Potential risks


Output:

Create:

"Repository Analysis Report"


DO NOT write code yet.


==================================================
PHASE 2 — DESIGN BEFORE CODING
==================================================

After understanding the repository, propose a minimal design.


## Technology Decision

First decide:

Should we keep the existing technology stack?

Default rule:

Keep the existing stack.

Only change technology if there is a strong reason.


Explain:

- why this choice was made
- trade-offs


==================================================
MINIMAL ARCHITECTURE
==================================================

Design the smallest architecture that satisfies requirements.


Explain:

- classes/components to add
- responsibility of each component
- why each component exists


Avoid unnecessary layers.


Example:

Domain:

TimeDeposit
Withdrawal
InterestStrategy


Application:

UpdateBalanceService


Infrastructure:

Repository implementation


API:

Controller


Explain each decision.


==================================================
DATA MODEL DESIGN
==================================================

Design database implementation.


Required tables:


## timeDeposits

Fields:

- id INTEGER PRIMARY KEY
- planType STRING REQUIRED
- days INTEGER REQUIRED
- balance DECIMAL REQUIRED


## withdrawals

Fields:

- id INTEGER PRIMARY KEY
- timeDepositId INTEGER FOREIGN KEY REQUIRED
- amount DECIMAL REQUIRED
- date DATE REQUIRED


Explain:

- relationships
- migrations
- persistence approach


==================================================
BUSINESS LOGIC DESIGN
==================================================

Implement interest calculation using a design that supports future plans.


Required plans:


## Global Rule

First 30 days:

No interest.


## Basic Plan

1% monthly interest.


## Student Plan

3% monthly interest.

No interest after 365 days.


## Premium Plan

5% monthly interest.

Interest starts only after 45 days.


The design should allow:

Adding:

- Gold Plan
- Corporate Plan
- Other plans

without modifying existing calculation logic.


==================================================
PHASE 3 — IMPLEMENTATION
==================================================

Implement gradually.

Do not make huge changes at once.


After each major change explain:

## Change

What changed?


## Reason

Why was this necessary?


## Risk

Could this affect existing behavior?


## Verification

How was it tested?


==================================================
API REQUIREMENTS
==================================================

The final application MUST expose EXACTLY TWO REST ENDPOINTS.


NO MORE.


Allowed:


## Endpoint 1

GET:

/time-deposits


Response:

[
 {
   id,
   planType,
   balance,
   days,
   withdrawals
 }
]


Only return the required schema.


--------------------------------


## Endpoint 2

POST:

/time-deposits/update-balances


Purpose:

Update balances using existing business logic.


DO NOT CREATE:

- POST /time-deposits
- GET /time-deposits/{id}
- DELETE endpoints
- PUT endpoints
- Extra CRUD APIs


Before adding any endpoint ask:

"Is this explicitly required?"

If not:

Do not add it.


==================================================
TEST STRATEGY
==================================================

Do not only make tests pass.

Think like hidden tests exist.


Create tests for:


## Existing Behavior

- updateBalance unchanged
- existing functionality preserved


## Interest Rules

Test:

- Basic plan
- Student plan before 365 days
- Student plan after 365 days
- Premium before 45 days
- Premium after 45 days
- First 30 days rule


## API

Test:

- GET response schema
- POST updates balances
- Only two endpoints exist


## Database

Test:

- persistence works
- relationships work
- data retrieval works


Use integration testing where appropriate.


Prefer:

- Testcontainers

if suitable for the existing stack.


==================================================
HIDDEN TEST SIMULATION
==================================================

Before final submission:

Act as an external evaluator.

Try to reject this solution.


Review:


## Code Quality

Ask:

- Is the code readable?
- Is anything unnecessary?
- Are responsibilities clear?


## Architecture

Ask:

- Is separation correct?
- Is design simple?
- Is anything over-engineered?


## Business Logic

Ask:

- Are all interest rules exactly correct?


## Regression Safety

Ask:

- Was updateBalance modified?
- Could existing behavior break?


## Hidden Tests

Assume reviewers check:

1. Did you modify updateBalance?
2. Did you create more than two endpoints?
3. Are interest calculations correct?
4. Can new plans be added easily?
5. Is architecture unnecessarily complex?
6. Are assumptions documented?
7. Can another engineer understand the repository quickly?


Fix all discovered issues.


==================================================
README REQUIREMENTS
==================================================

Create a professional README.md.


Include:


# Project Overview

Explain:

- system purpose
- technical approach
- important decisions


# Architecture

Explain:

- layers
- responsibilities
- data flow


# Running Application

Include:

- prerequisites
- installation
- database setup
- commands


# API Documentation

Explain:

GET endpoint

POST endpoint


Include:

request examples

response examples


# Design Decisions

Explain:

- why interest calculation was designed this way
- why updateBalance was preserved
- why architecture is minimal


# Business Assumptions

Document:

- ambiguous requirements
- chosen interpretation
- reasoning


==================================================
AI_ASSISTANCE.md REQUIREMENTS
==================================================

Create:

AI_ASSISTANCE.md


Include:


# Tools Used

Example:

- Claude
- ChatGPT
- GitHub Copilot


# AI Contributions

Explain:

AI helped with:

- architecture discussions
- refactoring suggestions
- test generation
- documentation improvement


# Human Decisions

Explain:

- final architecture decisions
- assumptions
- trade-offs


==================================================
GIT PRACTICES
==================================================

Use meaningful atomic commits.


Preferred:


Commit 1:

Analyze existing implementation


Commit 2:

Add database persistence


Commit 3:

Implement interest calculation


Commit 4:

Add API endpoints


Commit 5:

Add tests


Commit 6:

Add documentation


Do not create one massive commit.


==================================================
FINAL SUBMISSION CHECKLIST
==================================================

Before submission verify:


□ Application starts successfully

□ Database works

□ Swagger/OpenAPI works

□ Exactly two endpoints exist

□ No extra endpoints exist

□ updateBalance was not modified

□ Tests pass

□ Hidden-test scenarios considered

□ README.md complete

□ AI_ASSISTANCE.md complete

□ No secrets committed

□ Repository is easy to understand


==================================================
FINAL RULE
==================================================

Work like a Senior Backend Engineer.

Do not rush into coding.

First:

Analyze.

Then:

Design.

Then:

Implement.

Then:

Review.

Then:

Submit.

Start now with:

PHASE 1 — REPOSITORY FORENSIC ANALYSIS

Do not write code yet.
```

## How the session deviated from the prompt

- **Rule 2 vs. the kata brief.** The prompt treats `updateBalance` as a black box, while the kata asks for an extensible
  refactoring with unchanged behaviour. The agent raised this conflict after the analysis phase; the human chose to
  refactor the method's internals behind characterization tests, keeping its signature and results identical.
- **"1% / 3% / 5% monthly interest".** The prompt says *monthly*; the verified code applies an annual rate divided by
  12 per monthly run. The code was treated as the source of truth (see README, "Business Assumptions").
- **Commit plan.** Commits follow the order that keeps every commit green (characterization tests first, then the
  refactor, persistence, API, documentation) rather than the example list in the prompt.
