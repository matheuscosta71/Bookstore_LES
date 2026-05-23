package com.matheusgn.ecommerce.customer.controller;

import com.matheusgn.ecommerce.customer.dto.TransactionResponse;
import com.matheusgn.ecommerce.customer.service.CustomerTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/transactions")
@RequiredArgsConstructor
@Tag(name = "Customer transactions", description = "RF0025 — Transações do cliente")
public class CustomerTransactionController {

    private final CustomerTransactionService transactionService;

    @GetMapping
    @Operation(summary = "Listar transações do cliente")
    public ResponseEntity<List<TransactionResponse>> list(@PathVariable UUID customerId) {
        return ResponseEntity.ok(transactionService.listByCustomerId(customerId));
    }
}
