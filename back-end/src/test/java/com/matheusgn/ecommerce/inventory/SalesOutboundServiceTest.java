package com.matheusgn.ecommerce.inventory;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.inventory.service.SalesOutboundService;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOutboundServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;
    @Mock
    private InventoryBalanceService inventoryBalanceService;

    @InjectMocks
    private SalesOutboundService salesOutboundService;

    @Test
    void applyOutbound_whenAlreadyProcessed_skips() {
        UUID orderId = UUID.randomUUID();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.ORDER, orderId))
                .thenReturn(true);

        salesOutboundService.applySalesOutbound(orderId);

        verify(salesOrderRepository, never()).findByIdWithItemsAndBooks(any());
        verify(inventoryBalanceService, never()).decreaseStock(any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void applyOutbound_decreasesPerItem() {
        UUID orderId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("B")
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(10)
                .active(true)
                .build();
        SalesOrder order = SalesOrder.builder()
                .id(orderId)
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .book(book)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(new BigDecimal("20"))
                .exchangeRequested(false)
                .build();
        order.setItems(new ArrayList<>());
        order.getItems().add(item);
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.ORDER, orderId))
                .thenReturn(false);
        when(salesOrderRepository.findByIdWithItemsAndBooks(orderId)).thenReturn(Optional.of(order));

        salesOutboundService.applySalesOutbound(orderId);

        verify(inventoryBalanceService).decreaseStock(
                eq(bookId),
                eq(2),
                eq(InventoryMovementType.SALE_OUTBOUND),
                eq(InventoryReferenceType.ORDER),
                eq(orderId),
                isNull());
    }

    @Test
    void givenOrderWithTwoItems_whenApplySalesOutbound_thenDecreasesStockForEachBook() {
        UUID orderId = UUID.randomUUID();
        UUID bookId1 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();
        Book book1 = Book.builder()
                .id(bookId1)
                .title("B1")
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(10)
                .active(true)
                .build();
        Book book2 = Book.builder()
                .id(bookId2)
                .title("B2")
                .isbn("9780000000002")
                .salePrice(BigDecimal.ONE)
                .stockQuantity(20)
                .active(true)
                .build();
        SalesOrder order = SalesOrder.builder()
                .id(orderId)
                .build();
        OrderItem item1 = OrderItem.builder()
                .order(order)
                .book(book1)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(new BigDecimal("20"))
                .exchangeRequested(false)
                .build();
        OrderItem item2 = OrderItem.builder()
                .order(order)
                .book(book2)
                .quantity(5)
                .unitPrice(BigDecimal.ONE)
                .totalPrice(new BigDecimal("5"))
                .exchangeRequested(false)
                .build();
        order.setItems(new ArrayList<>());
        order.getItems().add(item1);
        order.getItems().add(item2);
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.ORDER, orderId))
                .thenReturn(false);
        when(salesOrderRepository.findByIdWithItemsAndBooks(orderId)).thenReturn(Optional.of(order));

        salesOutboundService.applySalesOutbound(orderId);

        verify(inventoryBalanceService).decreaseStock(
                eq(bookId1),
                eq(2),
                eq(InventoryMovementType.SALE_OUTBOUND),
                eq(InventoryReferenceType.ORDER),
                eq(orderId),
                isNull());
        verify(inventoryBalanceService).decreaseStock(
                eq(bookId2),
                eq(5),
                eq(InventoryMovementType.SALE_OUTBOUND),
                eq(InventoryReferenceType.ORDER),
                eq(orderId),
                isNull());
    }
}
