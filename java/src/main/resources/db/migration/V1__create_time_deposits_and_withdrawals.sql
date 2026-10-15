-- Names are quoted so the tables and columns match the required schema (camelCase) exactly.
CREATE TABLE "timeDeposits" (
    id         INTEGER        PRIMARY KEY,
    "planType" VARCHAR(50)    NOT NULL,
    days       INTEGER        NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL
);

CREATE TABLE withdrawals (
    id              INTEGER        PRIMARY KEY,
    "timeDepositId" INTEGER        NOT NULL REFERENCES "timeDeposits" (id),
    amount          NUMERIC(19, 2) NOT NULL,
    date            DATE           NOT NULL
);

CREATE INDEX idx_withdrawals_time_deposit_id ON withdrawals ("timeDepositId");
