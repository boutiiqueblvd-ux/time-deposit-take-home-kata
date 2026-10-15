package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;

import java.util.List;

/** Outbound port for time deposit storage. */
public interface TimeDepositRepository {

    List<TimeDeposit> findAll();

    /** Same as {@link #findAll()}, but locks the rows until the surrounding transaction ends. */
    List<TimeDeposit> findAllForUpdate();

    List<Withdrawal> findAllWithdrawals();

    void updateBalances(List<TimeDeposit> deposits);
}
