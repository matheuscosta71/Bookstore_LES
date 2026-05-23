package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.Payment;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link com.matheusgn.ecommerce.sales.service.OrderService}: apenas listagem e consulta por cliente.
 * <p>
 * RNs de fluxo de venda (status, pagamento, cupom, troca) não estão neste serviço — ver {@code CartService},
 * {@code CheckoutService} e correlatos até o domínio evoluir.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("listByCustomer / getByCustomer")
    class Queries {

        @Test
        @DisplayName("givenNoOrders_whenListByCustomer_thenReturnsEmptyList")
        void givenNoOrders_whenListByCustomer_thenReturnsEmptyList() {
            UUID cid = UUID.randomUUID();
            when(salesOrderRepository.findByCustomer_IdOrderByCreatedAtDesc(cid)).thenReturn(List.of());

            assertThat(orderService.listByCustomer(cid)).isEmpty();
        }

        @Test
        @DisplayName("givenTwoOrders_whenListByCustomer_thenReturnsMappedResponses")
        void givenTwoOrders_whenListByCustomer_thenReturnsMappedResponses() {
            UUID cid = UUID.randomUUID();
            SalesOrder o1 = minimalOrder(cid, OrderStatus.EM_PROCESSAMENTO);
            SalesOrder o2 = minimalOrder(cid, OrderStatus.ENTREGUE);
            when(salesOrderRepository.findByCustomer_IdOrderByCreatedAtDesc(cid)).thenReturn(List.of(o1, o2));
            when(exchangeRequestRepository.findByOrder_IdInAndStatus(anyList(), eq(ExchangeStatus.RECEIVED)))
                    .thenReturn(Collections.emptyList());

            List<OrderResponse> list = orderService.listByCustomer(cid);

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getStatus()).isEqualTo(OrderStatus.EM_PROCESSAMENTO);
            assertThat(list.get(1).getStatus()).isEqualTo(OrderStatus.ENTREGUE);
        }

        @Test
        @DisplayName("givenMissingOrder_whenGetByCustomer_thenThrows")
        void givenMissingOrder_whenGetByCustomer_thenThrows() {
            UUID cid = UUID.randomUUID();
            UUID oid = UUID.randomUUID();
            when(salesOrderRepository.findByIdAndCustomer_Id(oid, cid)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getByCustomer(cid, oid))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("givenOrder_whenGetByCustomer_thenReturnsDetail")
        void givenOrder_whenGetByCustomer_thenReturnsDetail() {
            UUID cid = UUID.randomUUID();
            SalesOrder order = orderWithItemsAndPayments(cid);
            when(salesOrderRepository.findByIdAndCustomer_Id(order.getId(), cid)).thenReturn(Optional.of(order));
            when(exchangeRequestRepository.findByOrder_IdInAndStatus(anyList(), eq(ExchangeStatus.RECEIVED)))
                    .thenReturn(Collections.emptyList());

            OrderResponse res = orderService.getByCustomer(cid, order.getId());

            assertThat(res.getDeliveryAddressId()).isEqualTo(order.getDeliveryAddress().getId());
            assertThat(res.getItems()).hasSize(1);
            assertThat(res.getPayments()).hasSize(1);
            assertThat(res.getItems().get(0).getTitle()).isEqualTo("Título");
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("givenSalesOrder_whenToResponse_thenMapsTotalsAndLines")
        void givenSalesOrder_whenToResponse_thenMapsTotalsAndLines() {
            UUID cid = UUID.randomUUID();
            SalesOrder order = orderWithItemsAndPayments(cid);

            OrderResponse res = orderService.toResponse(order);

            assertThat(res.getCustomerId()).isEqualTo(cid);
            assertThat(res.getCustomerName()).isEqualTo("Maria");
            assertThat(res.getOrderNumber()).isNotBlank().startsWith("#");
            assertThat(res.getTotalAmount()).isEqualByComparingTo(order.getTotalAmount());
            assertThat(res.getFreightAmount()).isEqualByComparingTo(order.getFreightAmount());
            assertThat(res.getPayments().get(0).getPaymentType()).isEqualTo(PaymentType.CREDIT_CARD);
        }
    }

    private static SalesOrder minimalOrder(UUID customerId, OrderStatus status) {
        Customer c = Customer.builder().id(customerId).fullName("Cliente Teste").build();
        Address addr = Address.builder().id(UUID.randomUUID()).customer(c).build();
        return SalesOrder.builder()
                .id(UUID.randomUUID())
                .customer(c)
                .deliveryAddress(addr)
                .freightAmount(BigDecimal.ONE)
                .itemsSubtotal(BigDecimal.TEN)
                .totalAmount(new BigDecimal("11.00"))
                .status(status)
                .createdAt(Instant.parse("2025-01-01T12:00:00Z"))
                .updatedAt(Instant.parse("2025-01-01T12:00:00Z"))
                .build();
    }

    private static SalesOrder orderWithItemsAndPayments(UUID customerId) {
        Customer c = Customer.builder().id(customerId).fullName("Maria").build();
        Address addr = Address.builder().id(UUID.randomUUID()).customer(c).build();
        Book book = Book.builder()
                .id(UUID.randomUUID())
                .title("Título")
                .isbn("9781111111111")
                .salePrice(new BigDecimal("40.00"))
                .stockQuantity(5)
                .active(true)
                .build();
        SalesOrder order = SalesOrder.builder()
                .id(UUID.randomUUID())
                .customer(c)
                .deliveryAddress(addr)
                .freightAmount(new BigDecimal("5.00"))
                .itemsSubtotal(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("45.00"))
                .status(OrderStatus.EM_PROCESSAMENTO)
                .createdAt(Instant.parse("2025-02-01T10:00:00Z"))
                .updatedAt(Instant.parse("2025-02-01T10:00:00Z"))
                .build();
        OrderItem oi = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .book(book)
                .quantity(1)
                .unitPrice(book.getSalePrice())
                .totalPrice(new BigDecimal("40.00"))
                .exchangeRequested(false)
                .build();
        order.getItems().add(oi);
        Payment pay = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .paymentType(PaymentType.CREDIT_CARD)
                .amount(new BigDecimal("45.00"))
                .cardLastDigits("1111")
                .createdAt(Instant.parse("2025-02-01T10:00:01Z"))
                .build();
        order.getPayments().add(pay);
        return order;
    }
}
