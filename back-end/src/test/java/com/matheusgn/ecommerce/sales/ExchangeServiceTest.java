package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.exception.ForbiddenException;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.CreateExchangeRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeReceiveRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import com.matheusgn.ecommerce.sales.service.ExchangeService;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.service.CustomerNotificationService;
import com.matheusgn.ecommerce.inventory.service.ExchangeInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private ExchangeRequestRepository exchangeRequestRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private ExchangeInventoryService exchangeInventoryService;
    @Mock
    private AdminOrderService adminOrderService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CustomerNotificationService customerNotificationService;

    @InjectMocks
    private ExchangeService exchangeService;

    private UUID customerId;
    private UUID orderId;
    private UUID itemId;
    private Customer customer;
    private Book book;
    private SalesOrder order;
    private OrderItem orderItem;
    private String adminKey;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        adminKey = "k";
        customer = Customer.builder().id(customerId).build();
        book = Book.builder()
                .id(UUID.randomUUID())
                .title("Livro")
                .isbn("9780000000001")
                .salePrice(new BigDecimal("15.00"))
                .stockQuantity(10)
                .active(true)
                .build();
        order = SalesOrder.builder()
                .id(orderId)
                .customer(customer)
                .status(OrderStatus.ENTREGUE)
                .build();
        orderItem = OrderItem.builder()
                .id(itemId)
                .order(order)
                .book(book)
                .quantity(1)
                .unitPrice(book.getSalePrice())
                .totalPrice(book.getSalePrice())
                .exchangeRequested(false)
                .build();
    }

    @Nested
    @DisplayName("RF0040 — Solicitar troca")
    class Rf0040 {

        @Test
        @DisplayName("givenEntregueOrder_whenRequest_thenCreatesExchange")
        void givenEntregueOrder_whenRequest_thenCreatesExchange() {
            when(salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)).thenReturn(Optional.of(order));
            when(exchangeRequestRepository.existsByOrder_IdAndStatusIn(any(), any())).thenReturn(false);
            when(orderItemRepository.findByIdAndOrder_Id(itemId, orderId)).thenReturn(Optional.of(orderItem));
            when(exchangeRequestRepository.existsByOrderItem_Id(itemId)).thenReturn(false);
            when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            var res = exchangeService.requestExchange(customerId, orderId,
                    CreateExchangeRequest.builder().orderItemId(itemId).build());

            assertThat(res.getExchangeStatus()).isEqualTo(ExchangeStatus.REQUESTED);
            assertThat(res.getOrderStatus()).isEqualTo(OrderStatus.EM_TROCA);
        }

        @Test
        @DisplayName("givenOrderOfAnotherCustomer_whenRequest_thenNotFound")
        void givenOrderOfAnotherCustomer_whenRequest_thenNotFound() {
            when(salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exchangeService.requestExchange(customerId, orderId,
                    CreateExchangeRequest.builder().orderItemId(itemId).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("givenOrderNotEntregue_whenRequest_thenThrows")
        void givenOrderNotEntregue_whenRequest_thenThrows() {
            order.setStatus(OrderStatus.EM_PROCESSAMENTO);
            when(salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> exchangeService.requestExchange(customerId, orderId,
                    CreateExchangeRequest.builder().orderItemId(itemId).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entregue");
        }

        @Test
        @DisplayName("givenWrongOrderItem_whenRequest_thenNotFound")
        void givenWrongOrderItem_whenRequest_thenNotFound() {
            when(salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)).thenReturn(Optional.of(order));
            when(exchangeRequestRepository.existsByOrder_IdAndStatusIn(any(), any())).thenReturn(false);
            when(orderItemRepository.findByIdAndOrder_Id(itemId, orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exchangeService.requestExchange(customerId, orderId,
                    CreateExchangeRequest.builder().orderItemId(itemId).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("RF0042 — Listagem")
    class Rf0042 {

        @Test
        @DisplayName("givenEmTrocaFilter_whenList_thenMayBeEmpty")
        void givenEmTrocaFilter_whenList_thenMayBeEmpty() {
            when(exchangeRequestRepository.findByOrder_StatusOrderByCreatedAtDesc(OrderStatus.EM_TROCA))
                    .thenReturn(Collections.emptyList());

            var list = exchangeService.listByOrderStatus(OrderStatus.EM_TROCA, adminKey);

            assertThat(list).isEmpty();
            verify(adminOrderService).assertAdmin(adminKey);
        }
    }

    @Nested
    @DisplayName("RF0041 — Autorizar")
    class Rf0041 {

        @Test
        @DisplayName("givenRequestedExchange_whenAuthorize_thenTrocaAutorizada")
        void givenRequestedExchange_whenAuthorize_thenTrocaAutorizada() {
            ExchangeRequest er = ExchangeRequest.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .orderItem(orderItem)
                    .customer(customer)
                    .status(ExchangeStatus.REQUESTED)
                    .build();
            order.setStatus(OrderStatus.EM_TROCA);
            when(exchangeRequestRepository.findById(eq(er.getId()))).thenReturn(Optional.of(er));
            when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            var res = exchangeService.authorize(er.getId(), adminKey);

            assertThat(res.getOrderStatus()).isEqualTo(OrderStatus.TROCA_AUTORIZADA);
            assertThat(res.getExchangeStatus()).isEqualTo(ExchangeStatus.AUTHORIZED);
            verify(adminOrderService).assertAdmin(adminKey);
            verify(customerNotificationService).notifyExchangeAuthorized(eq(customerId), eq(orderId), eq(er.getId()));
        }

        @Test
        @DisplayName("givenBadAdminKey_whenAuthorize_thenForbidden")
        void givenBadAdminKey_whenAuthorize_thenForbidden() {
            UUID exchangeId = UUID.randomUUID();
            doThrow(new ForbiddenException("negado")).when(adminOrderService).assertAdmin("bad");

            assertThatThrownBy(() -> exchangeService.authorize(exchangeId, "bad"))
                    .isInstanceOf(ForbiddenException.class);
            verify(adminOrderService).assertAdmin("bad");
        }
    }

    @Nested
    @DisplayName("RF0043 / RF0044 — Recebimento e cupom")
    class Receive {

        @Test
        @DisplayName("givenAuthorizedExchange_whenReceiveWithStock_thenIncreasesStockAndCreatesCoupon")
        void givenAuthorizedExchange_whenReceiveWithStock_thenIncreasesStockAndCreatesCoupon() {
            order.setStatus(OrderStatus.TROCA_AUTORIZADA);
            ExchangeRequest er = ExchangeRequest.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .orderItem(orderItem)
                    .customer(customer)
                    .status(ExchangeStatus.AUTHORIZED)
                    .build();
            when(exchangeRequestRepository.findById(eq(er.getId()))).thenReturn(Optional.of(er));
            when(couponRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
            when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            var res = exchangeService.receive(er.getId(),
                    ExchangeReceiveRequest.builder().returnToStock(true).build(), adminKey);

            assertThat(res.getExchangeStatus()).isEqualTo(ExchangeStatus.RECEIVED);
            assertThat(res.getGeneratedCouponCode()).isNotBlank();
            verify(exchangeInventoryService).applyExchangeReturnToStock(er.getId());
            verify(adminOrderService).assertAdmin(adminKey);
            verify(auditLogService).logUpdate(eq("ExchangeRequest"), eq(er.getId()), any());

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(couponService).createExchangeCoupon(eq(customerId), eq(orderItem.getTotalPrice()), codeCaptor.capture());
            assertThat(codeCaptor.getValue()).startsWith("TROCA-");
            verify(customerNotificationService).notifyExchangeReceived(eq(customerId), eq(orderId), anyString());
        }

        @Test
        @DisplayName("givenReturnToStockFalse_whenReceive_thenNeverIncreasesStock")
        void givenReturnToStockFalse_whenReceive_thenNeverIncreasesStock() {
            order.setStatus(OrderStatus.TROCA_AUTORIZADA);
            ExchangeRequest er = ExchangeRequest.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .orderItem(orderItem)
                    .customer(customer)
                    .status(ExchangeStatus.AUTHORIZED)
                    .build();
            when(exchangeRequestRepository.findById(eq(er.getId()))).thenReturn(Optional.of(er));
            when(couponRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
            when(exchangeRequestRepository.save(any(ExchangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            exchangeService.receive(er.getId(),
                    ExchangeReceiveRequest.builder().returnToStock(false).build(), adminKey);

            verify(exchangeInventoryService, never()).applyExchangeReturnToStock(any());
            verify(auditLogService).logUpdate(eq("ExchangeRequest"), eq(er.getId()), any());
            verify(customerNotificationService).notifyExchangeReceived(eq(customerId), eq(orderId), anyString());
        }
    }
}
