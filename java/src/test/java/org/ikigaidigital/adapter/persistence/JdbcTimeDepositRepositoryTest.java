package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JdbcTimeDepositRepository.class})
@Sql("/fixtures.sql")
class JdbcTimeDepositRepositoryTest {

    @Autowired
    private JdbcTimeDepositRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void finds_all_time_deposits() {
        List<TimeDeposit> deposits = repository.findAll();

        assertThat(deposits).extracting(TimeDeposit::getId).containsExactly(1, 2, 3, 4, 5, 6);
        TimeDeposit first = deposits.get(0);
        assertThat(first.getPlanType()).isEqualTo("basic");
        assertThat(first.getDays()).isEqualTo(45);
        assertThat(first.getBalance()).isEqualTo(1234567.00);
    }

    @Test
    void finds_withdrawals_with_their_time_deposit_ordered_by_date() {
        assertThat(repository.findAllWithdrawals()).containsExactly(
                new Withdrawal(2, 1, new BigDecimal("250.50"), LocalDate.of(2026, 1, 15)),
                new Withdrawal(1, 1, new BigDecimal("500.00"), LocalDate.of(2026, 2, 15)),
                new Withdrawal(3, 5, new BigDecimal("100.00"), LocalDate.of(2026, 3, 1)));
    }

    @Test
    void updates_balances_rounded_to_two_decimals() {
        TimeDeposit deposit = repository.findAll().get(1);
        deposit.setBalance(0.1 + 0.2 + 10000);

        repository.updateBalances(List.of(deposit));

        assertThat(storedBalance(deposit.getId())).isEqualByComparingTo("10000.30");
        assertThat(storedBalance(1)).isEqualByComparingTo("1234567.00");
    }

    @Test
    void rejects_withdrawals_of_unknown_time_deposits() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO withdrawals (id, \"timeDepositId\", amount, date) VALUES (99, 999, 1.00, '2026-01-01')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private BigDecimal storedBalance(int id) {
        return jdbcTemplate.queryForObject("SELECT balance FROM \"timeDeposits\" WHERE id = ?", BigDecimal.class, id);
    }
}
