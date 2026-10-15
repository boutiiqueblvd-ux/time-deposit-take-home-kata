package org.ikigaidigital.adapter.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

record TimeDepositResponse(int id, String planType, BigDecimal balance, int days, List<WithdrawalResponse> withdrawals) {

    static TimeDepositResponse from(TimeDepositWithWithdrawals source) {
        TimeDeposit deposit = source.timeDeposit();
        return new TimeDepositResponse(
                deposit.getId(),
                deposit.getPlanType(),
                BigDecimal.valueOf(deposit.getBalance()).setScale(2, RoundingMode.HALF_UP),
                deposit.getDays(),
                source.withdrawals().stream().map(WithdrawalResponse::from).toList());
    }
}
