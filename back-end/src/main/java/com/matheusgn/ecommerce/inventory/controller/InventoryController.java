package com.matheusgn.ecommerce.inventory.controller;

import com.matheusgn.ecommerce.inventory.dto.ExchangeReentryRequest;
import com.matheusgn.ecommerce.inventory.dto.InventoryBookResponse;
import com.matheusgn.ecommerce.inventory.dto.InventoryEntryRequest;
import com.matheusgn.ecommerce.inventory.dto.InventoryMovementResponse;
import com.matheusgn.ecommerce.inventory.dto.SalesOutboundRequest;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.service.ExchangeInventoryService;
import com.matheusgn.ecommerce.inventory.service.InventoryBookService;
import com.matheusgn.ecommerce.inventory.service.InventoryEntryService;
import com.matheusgn.ecommerce.inventory.service.InventoryMovementService;
import com.matheusgn.ecommerce.inventory.service.SalesOutboundService;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Admin — estoque (RF0051, RF0053, RF0054)")
public class InventoryController {

    private final AdminOrderService adminOrderService;
    private final InventoryEntryService inventoryEntryService;
    private final InventoryBookService inventoryBookService;
    private final InventoryMovementService inventoryMovementService;
    private final SalesOutboundService salesOutboundService;
    private final ExchangeInventoryService exchangeInventoryService;

    @PostMapping("/entries")
    @Operation(summary = "RF0051 — Registrar entrada manual de estoque (livro cadastrado + quantidade + custo unitário)")
    public ResponseEntity<Void> createEntry(
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody InventoryEntryRequest request) {
        adminOrderService.assertAdmin(adminKey);
        inventoryEntryService.registerManualEntry(
                request.getBookId(), request.getQuantity(), request.getUnitCost(), request.getReason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books/{bookId}")
    @Operation(summary = "Consultar saldo de estoque por livro")
    public ResponseEntity<InventoryBookResponse> getByBook(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable UUID bookId) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(inventoryBookService.getByBookId(bookId));
    }

    @GetMapping("/movements")
    @Operation(summary = "Listar movimentações de estoque")
    public ResponseEntity<Page<InventoryMovementResponse>> listMovements(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) InventoryMovementType movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(inventoryMovementService.list(bookId, movementType, startDate, endDate, pageable));
    }

    @PostMapping("/sales-outbound")
    @Operation(summary = "RF0053 — Baixa manual por pedido (idempotente). Fluxo normal: PATCH /admin/orders/{id}/approve-payment após operadora.")
    public ResponseEntity<Void> salesOutbound(
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody SalesOutboundRequest request) {
        adminOrderService.assertAdmin(adminKey);
        salesOutboundService.applySalesOutbound(request.getOrderId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reentries/exchange")
    @Operation(summary = "RF0054 — Reentrada por troca (idempotente; também ao receber troca com retorno ao estoque)")
    public ResponseEntity<Void> exchangeReentry(
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody ExchangeReentryRequest request) {
        adminOrderService.assertAdmin(adminKey);
        exchangeInventoryService.applyExchangeReturnToStock(request.getExchangeRequestId());
        return ResponseEntity.noContent().build();
    }
}
