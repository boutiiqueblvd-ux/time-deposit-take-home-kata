package org.ikigaidigital.interest;

/** Basic plan: 1% interest. */
public class BasicInterestPolicy extends AnnualRateInterestPolicy {

    public BasicInterestPolicy() {
        super("basic", 0.01);
    }
}
