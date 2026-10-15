package org.ikigaidigital.adapter.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ikigaidigital.application.TimeDepositService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@OpenAPIDefinition(info = @Info(title = "XA Bank Time Deposit API", version = "1.0"))
@Tag(name = "Time deposits")
@RestController
@RequestMapping("/time-deposits")
class TimeDepositController {

    private final TimeDepositService service;

    TimeDepositController(TimeDepositService service) {
        this.service = service;
    }

    @Operation(summary = "List all time deposits with their withdrawals")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<TimeDepositResponse> getAll() {
        return service.findAll().stream().map(TimeDepositResponse::from).toList();
    }

    @Operation(
            summary = "Update the balances of all time deposits",
            description = "Applies one monthly interest cycle to every time deposit. "
                    + "Each call adds another month of interest, so the operation is not idempotent.")
    @ApiResponse(responseCode = "204", description = "Balances updated")
    @PostMapping("/update-balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateBalances() {
        service.updateAllBalances();
    }
}
