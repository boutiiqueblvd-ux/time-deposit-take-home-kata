package org.ikigaidigital;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Withdrawal made from a time deposit. Kept separate from {@link TimeDeposit}
 * so the shared TimeDeposit class does not change.
 */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}
