package com.matheusgn.ecommerce.inventory;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.inventory.service.ExchangeInventoryService;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeInventoryServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;
    @Mock
    private InventoryBalanceService inventoryBalanceService;

    @InjectMocks
    private ExchangeInventoryService exchangeInventoryService;

    @Test
    void apply_whenAlreadyRecorded_skips() {
        UUID exId = UUID.randomUUID();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.EXCHANGE, exId))
                .thenReturn(true);

        exchangeInventoryService.applyExchangeReturnToStock(exId);

        verify(exchangeRequestRepository, never()).findById(exId);
    }

    @Test
    void apply_valid_increasesStock() {
        UUID exId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("B")
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(5)
                .active(true)
                .build();
        OrderItem oi = OrderItem.builder()
                .book(book)
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.TEN)
                .exchangeRequested(true)
                .build();
        ExchangeRequest er = ExchangeRequest.builder()
                .id(exId)
                .order(SalesOrder.builder().build())
                .orderItem(oi)
                .customer(Customer.builder().build())
                .status(ExchangeStatus.RECEIVED)
                .returnToStock(true)
                .build();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.EXCHANGE, exId))
                .thenReturn(false);
        when(exchangeRequestRepository.findById(exId)).thenReturn(Optional.of(er));

        exchangeInventoryService.applyExchangeReturnToStock(exId);

        verify(inventoryBalanceService).increaseStock(
                eq(bookId),
                eq(1),
                eq(InventoryMovementType.EXCHANGE_RETURN),
                eq(InventoryReferenceType.EXCHANGE),
                eq(exId),
                isNull());
    }

    @Test
    void apply_notReceived_throws() {
        UUID exId = UUID.randomUUID();
        ExchangeRequest er = ExchangeRequest.builder()
                .id(exId)
                .status(ExchangeStatus.AUTHORIZED)
                .returnToStock(true)
                .build();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.EXCHANGE, exId))
                .thenReturn(false);
        when(exchangeRequestRepository.findById(exId)).thenReturn(Optional.of(er));

        assertThatThrownBy(() -> exchangeInventoryService.applyExchangeReturnToStock(exId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apply_notFound_throws() {
        UUID exId = UUID.randomUUID();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.EXCHANGE, exId))
                .thenReturn(false);
        when(exchangeRequestRepository.findById(exId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeInventoryService.applyExchangeReturnToStock(exId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenReceivedButReturnToStockFalse_whenApply_thenThrowsIllegalArgumentException() {
        UUID exId = UUID.randomUUID();
        ExchangeRequest er = ExchangeRequest.builder()
                .id(exId)
                .status(ExchangeStatus.RECEIVED)
                .returnToStock(false)
                .build();
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.EXCHANGE, exId))
                .thenReturn(false);
        when(exchangeRequestRepository.findById(exId)).thenReturn(Optional.of(er));

        assertThatThrownBy(() -> exchangeInventoryService.applyExchangeReturnToStock(exId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retorno");
    }
}
