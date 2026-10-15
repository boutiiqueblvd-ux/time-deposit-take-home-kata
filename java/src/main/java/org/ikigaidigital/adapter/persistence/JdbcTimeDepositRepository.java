package org.ikigaidigital.adapter.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Table and column names are quoted to match the required camelCase schema exactly.
 */
@Repository
class JdbcTimeDepositRepository implements TimeDepositRepository {

    private static final String SELECT_TIME_DEPOSITS =
            "SELECT id, \"planType\", balance, days FROM \"timeDeposits\" ORDER BY id";

    private static final RowMapper<TimeDeposit> TIME_DEPOSIT_MAPPER = (rs, rowNum) -> new TimeDeposit(
            rs.getInt("id"),
            rs.getString("planType"),
            rs.getBigDecimal("balance").doubleValue(),
            rs.getInt("days"));

    private static final RowMapper<Withdrawal> WITHDRAWAL_MAPPER = (rs, rowNum) -> new Withdrawal(
            rs.getInt("id"),
            rs.getInt("timeDepositId"),
            rs.getBigDecimal("amount"),
            rs.getObject("date", LocalDate.class));

    private final JdbcTemplate jdbcTemplate;

    JdbcTimeDepositRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TimeDeposit> findAll() {
        return jdbcTemplate.query(SELECT_TIME_DEPOSITS, TIME_DEPOSIT_MAPPER);
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        return jdbcTemplate.query(SELECT_TIME_DEPOSITS + " FOR UPDATE", TIME_DEPOSIT_MAPPER);
    }

    @Override
    public List<Withdrawal> findAllWithdrawals() {
        return jdbcTemplate.query(
                "SELECT id, \"timeDepositId\", amount, date FROM withdrawals ORDER BY date, id",
                WITHDRAWAL_MAPPER);
    }

    @Override
    public void updateBalances(List<TimeDeposit> deposits) {
        jdbcTemplate.batchUpdate(
                "UPDATE \"timeDeposits\" SET balance = ? WHERE id = ?",
                deposits,
                deposits.size(),
                (ps, deposit) -> {
                    ps.setBigDecimal(1, toMoney(deposit.getBalance()));
                    ps.setInt(2, deposit.getId());
                });
    }

    // The domain calculates in double while the column is DECIMAL(19,2). valueOf takes the shortest
    // decimal representation and the scale strips floating point noise such as 100.01000000000001.
    private static BigDecimal toMoney(double balance) {
        return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
    }
}
