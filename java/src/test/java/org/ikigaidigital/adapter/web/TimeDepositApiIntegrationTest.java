package org.ikigaidigital.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikigaidigital.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql("/fixtures.sql")
class TimeDepositApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void get_returns_all_time_deposits_with_the_required_schema() throws Exception {
        JsonNode deposits = getJson("/time-deposits");

        assertThat(deposits).hasSize(6);
        JsonNode first = deposits.get(0);
        assertThat(fieldNames(first)).containsExactlyInAnyOrder("id", "planType", "balance", "days", "withdrawals");
        assertThat(first.get("id").asInt()).isEqualTo(1);
        assertThat(first.get("planType").asText()).isEqualTo("basic");
        assertThat(first.get("balance").decimalValue()).isEqualByComparingTo("1234567.00");
        assertThat(first.get("days").asInt()).isEqualTo(45);

        JsonNode withdrawals = first.get("withdrawals");
        assertThat(withdrawals).hasSize(2);
        assertThat(fieldNames(withdrawals.get(0))).containsExactlyInAnyOrder("id", "amount", "date");
        assertThat(withdrawals.get(0).get("amount").decimalValue()).isEqualByComparingTo("250.50");
        assertThat(withdrawals.get(0).get("date").asText()).isEqualTo("2026-01-15");
        assertThat(deposits.get(1).get("withdrawals")).isEmpty();
    }

    @Test
    void post_applies_one_month_of_interest_and_persists_it() throws Exception {
        mockMvc.perform(post("/time-deposits/update-balances")).andExpect(status().isNoContent());

        assertThat(balancesById()).containsExactlyInAnyOrderEntriesOf(Map.of(
                1, "1235595.81", // basic, day 45: 1% / 12
                2, "10025.00",   // student within the first year: 3% / 12
                3, "10000.00",   // student after one year: no interest
                4, "10000.00",   // premium, day 45: not started yet
                5, "10041.67",   // premium, day 46: 5% / 12
                6, "10000.00")); // first 30 days: no interest
    }

    @Test
    void every_post_applies_another_month_of_interest() throws Exception {
        mockMvc.perform(post("/time-deposits/update-balances")).andExpect(status().isNoContent());
        mockMvc.perform(post("/time-deposits/update-balances")).andExpect(status().isNoContent());

        assertThat(balancesById()).containsEntry(5, "10083.51"); // 10041.67 + round(41.840...) = 10041.67 + 41.84
    }

    @Test
    void the_api_contract_exposes_exactly_the_two_required_endpoints() throws Exception {
        JsonNode paths = getJson("/v3/api-docs").get("paths");

        assertThat(fieldNames(paths)).containsExactlyInAnyOrder("/time-deposits", "/time-deposits/update-balances");
        assertThat(fieldNames(paths.get("/time-deposits"))).containsExactly("get");
        assertThat(fieldNames(paths.at("/~1time-deposits/get/responses/200/content"))).containsExactly("application/json");
        assertThat(fieldNames(paths.get("/time-deposits/update-balances"))).containsExactly("post");
    }

    @Test
    void no_other_operations_are_routed() throws Exception {
        mockMvc.perform(post("/time-deposits")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/time-deposits/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/time-deposits/update-balances")).andExpect(status().isMethodNotAllowed());
    }

    private Map<Integer, String> balancesById() throws Exception {
        Map<Integer, String> balances = new HashMap<>();
        for (JsonNode deposit : getJson("/time-deposits")) {
            balances.put(deposit.get("id").asInt(), deposit.get("balance").decimalValue().setScale(2).toPlainString());
        }
        return balances;
    }

    private JsonNode getJson(String path) throws Exception {
        String body = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
