package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.CartResponse;
import com.matheusgn.ecommerce.sales.dto.CartUpsertItemRequest;
import com.matheusgn.ecommerce.sales.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers/{customerId}/cart")
@RequiredArgsConstructor
@Tag(name = "Carrinho")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar item ao carrinho")
    public CartResponse addItem(
            @PathVariable UUID customerId,
            @Valid @RequestBody CartUpsertItemRequest request) {
        log.info("[CartController][addItem] customerId={} bookId={} quantity={}",
                customerId, request.getBookId(), request.getQuantity());
        return cartService.addItem(customerId, request);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Alterar quantidade de um item")
    public CartResponse updateItem(
            @PathVariable UUID customerId,
            @PathVariable UUID itemId,
            @Valid @RequestBody CartUpsertItemRequest request) {
        log.info("[CartController][updateItem] customerId={} itemId={} quantity={}",
                customerId, itemId, request.getQuantity());
        return cartService.updateItemQuantity(customerId, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover item do carrinho")
    public void removeItem(@PathVariable UUID customerId, @PathVariable UUID itemId) {
        log.info("[CartController][removeItem] customerId={} itemId={}", customerId, itemId);
        cartService.removeItem(customerId, itemId);
    }

    @GetMapping
    @Operation(summary = "Visualizar carrinho")
    public CartResponse getCart(@PathVariable UUID customerId) {
        log.debug("[CartController][getCart] customerId={}", customerId);
        return cartService.getCart(customerId);
    }
}
