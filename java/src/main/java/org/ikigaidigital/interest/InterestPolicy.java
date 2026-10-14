package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Interest rule of a single plan type. New plans are added by implementing this
 * interface and registering it with the calculator; existing policies stay untouched.
 */
public interface InterestPolicy {

    /** Plan type this policy applies to, as stored on {@link TimeDeposit#getPlanType()}. */
    String planType();

    /** Interest earned in one monthly run, before rounding. */
    double calculateMonthlyInterest(TimeDeposit deposit);
}
