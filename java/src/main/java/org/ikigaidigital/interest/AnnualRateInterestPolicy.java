package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Plans paying one twelfth of an annual rate per monthly run.
 * Applies the rule shared by all current plans: no interest during the first 30 days.
 */
public abstract class AnnualRateInterestPolicy implements InterestPolicy {

    private static final int NO_INTEREST_PERIOD_DAYS = 30;
    private static final int MONTHS_PER_YEAR = 12;

    private final String planType;
    private final double annualRate;

    protected AnnualRateInterestPolicy(String planType, double annualRate) {
        this.planType = planType;
        this.annualRate = annualRate;
    }

    @Override
    public String planType() {
        return planType;
    }

    @Override
    public final double calculateMonthlyInterest(TimeDeposit deposit) {
        int days = deposit.getDays();
        if (days <= NO_INTEREST_PERIOD_DAYS || !accruesInterestAt(days)) {
            return 0;
        }
        // Operand order matches the original calculation so results stay bit-for-bit identical.
        return deposit.getBalance() * annualRate / MONTHS_PER_YEAR;
    }

    /** Plan-specific restriction on top of the 30-day rule. */
    protected boolean accruesInterestAt(int days) {
        return true;
    }
}
