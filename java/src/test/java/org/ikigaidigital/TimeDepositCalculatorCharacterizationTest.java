package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the verified behaviour of {@link TimeDepositCalculator#updateBalance(List)}.
 * Written before any refactoring so that the refactored calculator can be proven
 * to produce bit-for-bit identical balances.
 */
class TimeDepositCalculatorCharacterizationTest {

    private static final List<String> PLAN_TYPES = List.of("basic", "student", "premium", "unknown", "Basic", "");
    private static final int[] DAYS = {0, 1, 29, 30, 31, 44, 45, 46, 60, 364, 365, 366, 367, 1000};
    private static final double[] BALANCES = {0.0, 0.01, 1.0, 150.0, 300.0, 999.99, 1000.0, 12345.67, 1234567.0, 99999999.99};

    @ParameterizedTest(name = "{0} plan, {1} days, balance {2} -> {3}")
    @CsvSource({
            "basic,    30,  10000.00,   10000.00",
            "basic,    31,  10000.00,   10008.33",
            "basic,    45,  1234567.00, 1235595.81",
            "basic,    150, 150.00,     150.13",
            "student,  30,  10000.00,   10000.00",
            "student,  31,  10000.00,   10025.00",
            "student,  365, 10000.00,   10025.00",
            "student,  366, 10000.00,   10000.00",
            "premium,  31,  10000.00,   10000.00",
            "premium,  45,  10000.00,   10000.00",
            "premium,  46,  10000.00,   10041.67",
            "premium,  400, 10000.00,   10041.67",
            "gold,     100, 10000.00,   10000.00",
            "BASIC,    100, 10000.00,   10000.00",
    })
    void applies_one_month_of_interest(String planType, int days, double balance, double expectedBalance) {
        List<TimeDeposit> deposits = List.of(new TimeDeposit(1, planType, balance, days));

        new TimeDepositCalculator().updateBalance(deposits);

        assertThat(deposits.get(0).getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void matches_the_original_implementation_exactly() {
        List<TimeDeposit> actual = sampleDeposits();
        List<TimeDeposit> expected = sampleDeposits();

        new TimeDepositCalculator().updateBalance(actual);
        OriginalTimeDepositCalculator.updateBalance(expected);

        for (int i = 0; i < actual.size(); i++) {
            TimeDeposit deposit = actual.get(i);
            assertThat(deposit.getBalance())
                    .as("%s plan, %d days", deposit.getPlanType(), deposit.getDays())
                    .isEqualTo(expected.get(i).getBalance());
        }
    }

    @Test
    void only_changes_the_balance() {
        TimeDeposit deposit = new TimeDeposit(7, "premium", 5000.0, 90);

        new TimeDepositCalculator().updateBalance(List.of(deposit));

        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo("premium");
        assertThat(deposit.getDays()).isEqualTo(90);
    }

    @Test
    void accepts_an_empty_list() {
        new TimeDepositCalculator().updateBalance(new ArrayList<>());
    }

    private static List<TimeDeposit> sampleDeposits() {
        List<TimeDeposit> deposits = new ArrayList<>();
        Random random = new Random(42);
        int id = 0;
        for (String planType : PLAN_TYPES) {
            for (int days : DAYS) {
                for (double balance : BALANCES) {
                    deposits.add(new TimeDeposit(++id, planType, balance, days));
                }
                for (int i = 0; i < 200; i++) {
                    double randomBalance = Math.round(random.nextDouble() * 10_000_000) / 100.0;
                    deposits.add(new TimeDeposit(++id, planType, randomBalance, days));
                }
            }
        }
        return deposits;
    }

    /** Verbatim copy of the original, verified implementation, used as the reference. */
    private static final class OriginalTimeDepositCalculator {
        static void updateBalance(List<TimeDeposit> xs) {
            for (int i = 0; i < xs.size(); i++) {
                double interest = 0;

                if (xs.get(i).getDays() > 30) {
                    if (xs.get(i).getPlanType().equals("student")) {
                        if (xs.get(i).getDays() < 366) {
                            interest += xs.get(i).getBalance() * 0.03 / 12;
                        }
                    } else if (xs.get(i).getPlanType().equals("premium")) {
                        if (xs.get(i).getDays() > 45) {
                            interest += xs.get(i).getBalance() * 0.05 / 12;
                        }
                    } else if (xs.get(i).getPlanType().equals("basic")) {
                        interest += xs.get(i).getBalance() * 0.01 / 12;
                    }
                }

                double a2d = xs.get(i).getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
                xs.get(i).setBalance(a2d);
            }
        }
    }
}
