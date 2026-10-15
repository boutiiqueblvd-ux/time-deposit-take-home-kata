-- Known state for each test, replacing the demo seed data.
DELETE FROM withdrawals;
DELETE FROM "timeDeposits";

INSERT INTO "timeDeposits" (id, "planType", days, balance) VALUES
    (1, 'basic',   45,  1234567.00),
    (2, 'student', 200, 10000.00),
    (3, 'student', 366, 10000.00),
    (4, 'premium', 45,  10000.00),
    (5, 'premium', 46,  10000.00),
    (6, 'basic',   30,  10000.00);

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (1, 1, 500.00, '2026-02-15'),
    (2, 1, 250.50, '2026-01-15'),
    (3, 5, 100.00, '2026-03-01');
