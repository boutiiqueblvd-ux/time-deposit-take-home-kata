package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.Withdrawal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDepositServiceTest {

    private final InMemoryRepository repository = new InMemoryRepository();
    private final TimeDepositService service = new TimeDepositService(repository, new TimeDepositCalculator());

    @Test
    void update_saves_the_balances_calculated_by_updateBalance() {
        repository.deposits.add(new TimeDeposit(1, "basic", 1234567.00, 45));
        repository.deposits.add(new TimeDeposit(2, "premium", 10000.00, 45));

        service.updateAllBalances();

        assertThat(repository.saved).extracting(TimeDeposit::getBalance).containsExactly(1235595.81, 10000.00);
    }

    @Test
    void find_all_attaches_each_deposits_withdrawals() {
        repository.deposits.add(new TimeDeposit(1, "basic", 100.00, 10));
        repository.deposits.add(new TimeDeposit(2, "student", 200.00, 10));
        Withdrawal withdrawal = new Withdrawal(7, 1, new BigDecimal("5.00"), LocalDate.of(2026, 1, 1));
        repository.withdrawals.add(withdrawal);

        List<TimeDepositWithWithdrawals> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).withdrawals()).containsExactly(withdrawal);
        assertThat(result.get(1).withdrawals()).isEmpty();
    }

    private static final class InMemoryRepository implements TimeDepositRepository {
        private final List<TimeDeposit> deposits = new ArrayList<>();
        private final List<Withdrawal> withdrawals = new ArrayList<>();
        private List<TimeDeposit> saved = List.of();

        @Override
        public List<TimeDeposit> findAll() {
            return deposits;
        }

        @Override
        public List<TimeDeposit> findAllForUpdate() {
            return deposits;
        }

        @Override
        public List<Withdrawal> findAllWithdrawals() {
            return withdrawals;
        }

        @Override
        public void updateBalances(List<TimeDeposit> deposits) {
            saved = List.copyOf(deposits);
        }
    }
}
