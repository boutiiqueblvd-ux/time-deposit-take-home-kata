package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InterestPolicyTest {

    private static final double BALANCE = 12_000.0;

    @ParameterizedTest(name = "basic, day {0} -> {1}")
    @CsvSource({"0, 0", "30, 0", "31, 10", "365, 10", "366, 10", "1000, 10"})
    void basic_pays_1_percent_after_the_first_30_days(int days, double expectedInterest) {
        assertInterest(new BasicInterestPolicy(), days, expectedInterest);
    }

    @ParameterizedTest(name = "student, day {0} -> {1}")
    @CsvSource({"30, 0", "31, 30", "365, 30", "366, 0", "1000, 0"})
    void student_pays_3_percent_until_one_year(int days, double expectedInterest) {
        assertInterest(new StudentInterestPolicy(), days, expectedInterest);
    }

    @ParameterizedTest(name = "premium, day {0} -> {1}")
    @CsvSource({"30, 0", "31, 0", "45, 0", "46, 50", "366, 50"})
    void premium_pays_5_percent_after_45_days(int days, double expectedInterest) {
        assertInterest(new PremiumInterestPolicy(), days, expectedInterest);
    }

    @Test
    void new_plans_plug_into_the_calculator_without_changing_existing_policies() {
        InterestPolicy gold = new AnnualRateInterestPolicy("gold", 0.12) { };
        TimeDepositCalculator calculator = new TimeDepositCalculator(List.of(new BasicInterestPolicy(), gold));
        TimeDeposit goldDeposit = new TimeDeposit(1, "gold", BALANCE, 60);
        TimeDeposit basicDeposit = new TimeDeposit(2, "basic", BALANCE, 60);

        calculator.updateBalance(List.of(goldDeposit, basicDeposit));

        assertThat(goldDeposit.getBalance()).isEqualTo(12_120.0);
        assertThat(basicDeposit.getBalance()).isEqualTo(12_010.0);
    }

    private static void assertInterest(InterestPolicy policy, int days, double expectedInterest) {
        TimeDeposit deposit = new TimeDeposit(1, policy.planType(), BALANCE, days);

        assertThat(policy.calculateMonthlyInterest(deposit)).isCloseTo(expectedInterest, within(1e-9));
    }
}
