package com.matheusgn.ecommerce.customer.controller;

import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CreditCardResponse;
import com.matheusgn.ecommerce.customer.dto.CreditCardUpdateRequest;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/cards")
@RequiredArgsConstructor
@Tag(name = "Customer cards", description = "RF0027 — Cartões de crédito")
public class CustomerCreditCardController {

    private final CustomerCreditCardService creditCardService;

    @PostMapping
    @Operation(summary = "Adicionar cartão")
    public ResponseEntity<CreditCardResponse> create(
            @PathVariable UUID customerId,
            @Valid @RequestBody CreditCardCreateRequest request) {
        CreditCardResponse body = creditCardService.addCard(customerId, request);
        return ResponseEntity
                .created(URI.create("/customers/" + customerId + "/cards/" + body.getId()))
                .body(body);
    }

    @GetMapping
    @Operation(summary = "Listar cartões ativos")
    public ResponseEntity<List<CreditCardResponse>> list(@PathVariable UUID customerId) {
        return ResponseEntity.ok(creditCardService.listActiveCards(customerId));
    }

    @PutMapping("/{cardId}")
    @Operation(summary = "Atualizar cartão")
    public ResponseEntity<CreditCardResponse> update(
            @PathVariable UUID customerId,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreditCardUpdateRequest request) {
        return ResponseEntity.ok(creditCardService.updateCard(customerId, cardId, request));
    }

    @PatchMapping("/{cardId}/preferred")
    @Operation(summary = "Definir cartão como preferencial")
    public ResponseEntity<CreditCardResponse> preferred(
            @PathVariable UUID customerId,
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(creditCardService.setPreferred(customerId, cardId));
    }

    @PatchMapping("/{cardId}/inactive")
    @Operation(summary = "Inativar cartão")
    public ResponseEntity<CreditCardResponse> inactive(
            @PathVariable UUID customerId,
            @PathVariable UUID cardId) {
        return ResponseEntity.ok(creditCardService.deactivateCard(customerId, cardId));
    }
}
