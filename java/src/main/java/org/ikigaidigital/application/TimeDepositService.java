package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.Withdrawal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TimeDepositService {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public List<TimeDepositWithWithdrawals> findAll() {
        Map<Integer, List<Withdrawal>> withdrawalsByDepositId = repository.findAllWithdrawals().stream()
                .collect(Collectors.groupingBy(Withdrawal::timeDepositId));

        return repository.findAll().stream()
                .map(deposit -> new TimeDepositWithWithdrawals(
                        deposit, withdrawalsByDepositId.getOrDefault(deposit.getId(), List.of())))
                .toList();
    }

    /**
     * Runs one monthly interest cycle for every deposit using the verified calculator.
     * Rows stay locked until commit, so concurrent runs are applied one after the other
     * instead of overwriting each other's balances.
     */
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> deposits = repository.findAllForUpdate();
        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);
    }
}
