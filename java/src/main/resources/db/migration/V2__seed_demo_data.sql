-- Demo data: there is deliberately no endpoint to create deposits, so the database is seeded here.
-- The rows cover each plan on both sides of its interest boundaries.
INSERT INTO "timeDeposits" (id, "planType", days, balance) VALUES
    (1, 'basic',   20,  10000.00),  -- first 30 days: no interest
    (2, 'basic',   45,  1234567.00),
    (3, 'student', 200, 5000.00),
    (4, 'student', 400, 5000.00),   -- after one year: no interest
    (5, 'premium', 40,  20000.00),  -- before day 46: no interest
    (6, 'premium', 90,  20000.00);

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (1, 2, 500.00,  '2026-01-15'),
    (2, 2, 250.00,  '2026-02-15'),
    (3, 6, 1000.00, '2026-03-01');
