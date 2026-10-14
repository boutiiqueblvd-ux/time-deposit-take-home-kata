package org.ikigaidigital.interest;

/** Student plan: 3% interest, no interest after one year (day 366 onwards). */
public class StudentInterestPolicy extends AnnualRateInterestPolicy {

    private static final int LAST_INTEREST_DAY = 365;

    public StudentInterestPolicy() {
        super("student", 0.03);
    }

    @Override
    protected boolean accruesInterestAt(int days) {
        return days <= LAST_INTEREST_DAY;
    }
}
