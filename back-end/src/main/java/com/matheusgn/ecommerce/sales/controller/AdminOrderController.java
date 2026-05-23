package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin — pedidos")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "Listar pedidos (paginado; filtros opcionais: número/id, cliente, status, datas, total)")
    public Page<OrderResponse> list(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal totalMin,
            @RequestParam(required = false) BigDecimal totalMax,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminOrderService.listOrders(
                pageable, adminKey, orderNumber, customerName, status, dateFrom, dateTo, totalMin, totalMax);
    }

    @PatchMapping("/{orderId}/approve-payment")
    @Operation(summary = "RN0028 — Aprovar pagamento (operadora); baixa estoque e extrato")
    public OrderResponse approvePayment(
            @PathVariable UUID orderId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return adminOrderService.approvePayment(orderId, adminKey);
    }

    @PatchMapping("/{orderId}/reject-payment")
    @Operation(summary = "RN0028 — Rejeitar pagamento; sem baixa de estoque")
    public OrderResponse rejectPayment(
            @PathVariable UUID orderId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return adminOrderService.rejectPayment(orderId, adminKey);
    }

    @PatchMapping("/{orderId}/dispatch")
    @Operation(summary = "Despachar pedido (APROVADO → EM_TRANSITO)")
    public OrderResponse dispatch(
            @PathVariable UUID orderId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return adminOrderService.dispatch(orderId, adminKey);
    }

    @PatchMapping("/{orderId}/deliver")
    @Operation(summary = "Confirmar entrega (EM_TRANSITO → ENTREGUE)")
    public OrderResponse deliver(
            @PathVariable UUID orderId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        return adminOrderService.markDelivered(orderId, adminKey);
    }
}
