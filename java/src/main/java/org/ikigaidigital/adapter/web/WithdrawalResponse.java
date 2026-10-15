package org.ikigaidigital.adapter.web;

import org.ikigaidigital.Withdrawal;

import java.math.BigDecimal;
import java.time.LocalDate;

record WithdrawalResponse(int id, BigDecimal amount, LocalDate date) {

    static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}
