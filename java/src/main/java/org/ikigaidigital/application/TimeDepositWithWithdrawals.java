package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;

import java.util.List;

public record TimeDepositWithWithdrawals(TimeDeposit timeDeposit, List<Withdrawal> withdrawals) {
}
