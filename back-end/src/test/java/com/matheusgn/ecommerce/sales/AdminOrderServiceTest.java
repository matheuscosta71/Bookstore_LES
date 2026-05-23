package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.config.AdminProperties;
import com.matheusgn.ecommerce.customer.service.CustomerTransactionRecorder;
import com.matheusgn.ecommerce.exception.ForbiddenException;
import com.matheusgn.ecommerce.inventory.service.SalesOutboundService;
import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import com.matheusgn.ecommerce.sales.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private AdminProperties adminProperties;
    @Mock
    private SalesOutboundService salesOutboundService;
    @Mock
    private CustomerTransactionRecorder customerTransactionRecorder;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private UUID orderId;
    private String validKey;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        validKey = "secret-admin";
        when(adminProperties.getKey()).thenReturn(validKey);
    }

    private SalesOrder sampleOrder(OrderStatus status) {
        Customer c = Customer.builder().id(UUID.randomUUID()).build();
        Address addr = Address.builder().id(UUID.randomUUID()).customer(c).build();
        return SalesOrder.builder()
                .id(orderId)
                .customer(c)
                .deliveryAddress(addr)
                .freightAmount(BigDecimal.ONE)
                .itemsSubtotal(BigDecimal.TEN)
                .totalAmount(new BigDecimal("11.00"))
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("RN0028 — Aprovar pagamento")
    class Approve {

        @Test
        @DisplayName("givenEmProcessamento_whenApprove_thenAprovadoOutboundAndTransaction")
        void givenEmProcessamento_whenApprove_thenAprovado() {
            SalesOrder order = sampleOrder(OrderStatus.EM_PROCESSAMENTO);
            when(salesOrderRepository.findByIdWithItemsAndBooks(orderId)).thenReturn(Optional.of(order));
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderService.toResponse(any(SalesOrder.class))).thenReturn(
                    OrderResponse.builder().id(orderId).status(OrderStatus.APROVADO).build());

            OrderResponse res = adminOrderService.approvePayment(orderId, validKey);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.APROVADO);
            verify(salesOrderRepository).save(order);
            verify(salesOutboundService).applySalesOutbound(orderId);
            verify(customerTransactionRecorder).recordPurchaseForCompletedOrder(order);
        }

        @Test
        @DisplayName("givenWrongKey_whenApprove_thenForbidden")
        void givenWrongKey_whenApprove_thenForbidden() {
            assertThatThrownBy(() -> adminOrderService.approvePayment(orderId, "wrong"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("RF0038 — Despachar")
    class Dispatch {

        @Test
        @DisplayName("givenAprovado_whenDispatch_thenEmTransito")
        void givenAprovado_whenDispatch_thenEmTransito() {
            SalesOrder order = sampleOrder(OrderStatus.APROVADO);
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderService.toResponse(any(SalesOrder.class))).thenReturn(
                    OrderResponse.builder().id(orderId).status(OrderStatus.EM_TRANSITO).build());

            OrderResponse res = adminOrderService.dispatch(orderId, validKey);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.EM_TRANSITO);
            verify(salesOrderRepository).save(order);
        }

        @Test
        @DisplayName("givenWrongKey_whenDispatch_thenForbidden")
        void givenWrongKey_whenDispatch_thenForbidden() {
            assertThatThrownBy(() -> adminOrderService.dispatch(orderId, "wrong"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("givenEntregue_whenDispatch_thenThrows")
        void givenEntregue_whenDispatch_thenThrows() {
            SalesOrder order = sampleOrder(OrderStatus.ENTREGUE);
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.dispatch(orderId, validKey))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("givenEmProcessamento_whenDispatch_thenThrows")
        void givenEmProcessamento_whenDispatch_thenThrows() {
            SalesOrder order = sampleOrder(OrderStatus.EM_PROCESSAMENTO);
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.dispatch(orderId, validKey))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(salesOutboundService, never()).applySalesOutbound(any());
        }
    }

    @Nested
    @DisplayName("RF0039 — Entrega")
    class Deliver {

        @Test
        @DisplayName("givenEmTransito_whenDeliver_thenEntregue")
        void givenEmTransito_whenDeliver_thenEntregue() {
            SalesOrder order = sampleOrder(OrderStatus.EM_TRANSITO);
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderService.toResponse(any(SalesOrder.class))).thenReturn(
                    OrderResponse.builder().id(orderId).status(OrderStatus.ENTREGUE).build());

            OrderResponse res = adminOrderService.markDelivered(orderId, validKey);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
            verify(salesOrderRepository).save(order);
        }

        @Test
        @DisplayName("givenWrongKey_whenDeliver_thenForbidden")
        void givenWrongKey_whenDeliver_thenForbidden() {
            assertThatThrownBy(() -> adminOrderService.markDelivered(orderId, "bad-key"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("givenEmProcessamento_whenDeliver_thenThrows")
        void givenEmProcessamento_whenDeliver_thenThrows() {
            SalesOrder order = sampleOrder(OrderStatus.EM_PROCESSAMENTO);
            when(salesOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> adminOrderService.markDelivered(orderId, validKey))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
