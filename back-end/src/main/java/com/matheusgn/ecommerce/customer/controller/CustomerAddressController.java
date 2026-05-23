package com.matheusgn.ecommerce.customer.controller;

import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.AddressResponse;
import com.matheusgn.ecommerce.customer.dto.AddressUpdateRequest;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
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
@RequestMapping("/customers/{customerId}/addresses")
@RequiredArgsConstructor
@Tag(name = "Customer addresses", description = "RF0026 — Endereços de entrega")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @PostMapping
    @Operation(summary = "Adicionar endereço ao cliente")
    public ResponseEntity<AddressResponse> create(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddressCreateRequest request) {
        AddressResponse body = addressService.addAddress(customerId, request);
        return ResponseEntity
                .created(URI.create("/customers/" + customerId + "/addresses/" + body.getId()))
                .body(body);
    }

    @GetMapping
    @Operation(summary = "Listar endereços ativos do cliente")
    public ResponseEntity<List<AddressResponse>> list(@PathVariable UUID customerId) {
        return ResponseEntity.ok(addressService.listActiveByCustomer(customerId));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<AddressResponse> update(
            @PathVariable UUID customerId,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressUpdateRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(customerId, addressId, request));
    }

    @PatchMapping("/{addressId}/inactive")
    @Operation(summary = "Inativar endereço (exclusão lógica)")
    public ResponseEntity<AddressResponse> inactive(
            @PathVariable UUID customerId,
            @PathVariable UUID addressId) {
        return ResponseEntity.ok(addressService.deactivateAddress(customerId, addressId));
    }
}
