package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.CreateExchangeRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeRequestResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.service.ExchangeService;
import com.matheusgn.ecommerce.sales.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos do cliente")
public class CustomerOrderController {

    private final OrderService orderService;
    private final ExchangeService exchangeService;

    @GetMapping
    @Operation(summary = "Listar pedidos do cliente")
    public List<OrderResponse> list(@PathVariable UUID customerId) {
        return orderService.listByCustomer(customerId);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Detalhar pedido")
    public OrderResponse get(
            @PathVariable UUID customerId,
            @PathVariable UUID orderId) {
        return orderService.getByCustomer(customerId, orderId);
    }

    @PostMapping("/{orderId}/exchange-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar troca de item")
    public ExchangeRequestResponse requestExchange(
            @PathVariable UUID customerId,
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateExchangeRequest request) {
        return exchangeService.requestExchange(customerId, orderId, request);
    }
}
