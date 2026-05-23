package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.ExchangeReceiveRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeRequestResponse;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.service.ExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/exchange-requests")
@RequiredArgsConstructor
@Tag(name = "Admin — trocas")
public class AdminExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping
    @Operation(summary = "Listar solicitações por status do pedido (ex.: EM_TROCA)")
    public List<ExchangeRequestResponse> list(
            @RequestParam OrderStatus status,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return exchangeService.listByOrderStatus(status, adminKey);
    }

    @PatchMapping("/{exchangeRequestId}/authorize")
    @Operation(summary = "Autorizar troca")
    public ExchangeRequestResponse authorize(
            @PathVariable UUID exchangeRequestId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return exchangeService.authorize(exchangeRequestId, adminKey);
    }

    @PatchMapping("/{exchangeRequestId}/receive")
    @Operation(summary = "Confirmar recebimento dos itens e gerar cupom de troca")
    public ExchangeRequestResponse receive(
            @PathVariable UUID exchangeRequestId,
            @Valid @RequestBody ExchangeReceiveRequest request,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return exchangeService.receive(exchangeRequestId, request, adminKey);
    }
}
