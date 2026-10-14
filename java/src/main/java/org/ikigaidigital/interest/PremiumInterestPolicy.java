package org.ikigaidigital.interest;

/** Premium plan: 5% interest, starting after 45 days (day 46 onwards). */
public class PremiumInterestPolicy extends AnnualRateInterestPolicy {

    private static final int INTEREST_START_AFTER_DAYS = 45;

    public PremiumInterestPolicy() {
        super("premium", 0.05);
    }

    @Override
    protected boolean accruesInterestAt(int days) {
        return days > INTEREST_START_AFTER_DAYS;
    }
}
