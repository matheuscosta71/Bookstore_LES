package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.AddressResponse;
import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.entity.CreditCard;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.customer.repository.CreditCardRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.sales.dto.CheckoutAddressRequest;
import com.matheusgn.ecommerce.sales.dto.CheckoutPaymentRequest;
import com.matheusgn.ecommerce.sales.dto.FreightRequest;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.dto.PaymentLineRequest;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartItem;
import com.matheusgn.ecommerce.sales.entity.CartPaymentLine;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.CartRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.service.CartExpirationService;
import com.matheusgn.ecommerce.sales.service.CartService;
import com.matheusgn.ecommerce.sales.service.CheckoutService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import com.matheusgn.ecommerce.sales.service.FreightService;
import com.matheusgn.ecommerce.sales.service.InventoryService;
import com.matheusgn.ecommerce.sales.service.OrderService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartService cartService;
    @Mock
    private FreightService freightService;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerAddressService customerAddressService;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CustomerCreditCardService customerCreditCardService;
    @Mock
    private CouponService couponService;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private OrderService orderService;
    @Mock
    private CartExpirationService cartExpirationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID customerId;
    private Customer customer;
    private Cart cart;
    private Book book;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        org.mockito.Mockito.lenient()
                .when(cartExpirationService.hasBlockingExpiredItems(any(Cart.class)))
                .thenReturn(false);
        lenient()
                .when(addressRepository.countByCustomer_IdAndTypeAndActiveTrue(eq(customerId), eq(AddressType.BILLING)))
                .thenReturn(1L);
        lenient()
                .when(addressRepository.countByCustomer_IdAndTypeAndActiveTrue(eq(customerId), eq(AddressType.DELIVERY)))
                .thenReturn(1L);
        lenient().doNothing().when(cartService).prepareCartForCheckout(eq(customerId));
        customer = Customer.builder().id(customerId).build();
        book = Book.builder()
                .id(UUID.randomUUID())
                .title("Livro")
                .isbn("9780000000002")
                .salePrice(new BigDecimal("50.00"))
                .stockQuantity(20)
                .active(true)
                .build();
        cart = Cart.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .status(CartStatus.OPEN)
                .totalAmount(new BigDecimal("50.00"))
                .items(new ArrayList<>())
                .paymentLines(new ArrayList<>())
                .build();
        cart.getItems().add(CartItem.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .book(book)
                .quantity(1)
                .unitPrice(book.getSalePrice())
                .totalPrice(new BigDecimal("50.00"))
                .build());
    }

    private AddressCreateRequest newAddressPayload() {
        return AddressCreateRequest.builder()
                .nickname("Casa")
                .street("Rua A")
                .number("10")
                .neighborhood("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310100")
                .type(AddressType.DELIVERY)
                .build();
    }

    @Nested
    @DisplayName("RNF0042 — Itens expirados no carrinho")
    class Rnf0042CartExpiry {

        @Test
        @DisplayName("givenItensExpirados_whenFinalizePurchase_thenBloqueiaComMensagemClara")
        void givenExpiredItems_whenFinalize_thenThrows() {
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(cartExpirationService.hasBlockingExpiredItems(cart)).thenReturn(true);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expirados");
        }
    }

    @Nested
    @DisplayName("RF0033 / RF0034 — Frete")
    class Freight {

        @Test
        @DisplayName("givenEmptyCart_whenCalculateFreight_thenThrows")
        void givenEmptyCart_whenCalculateFreight_thenThrows() {
            Cart empty = Cart.builder()
                    .customer(customer)
                    .status(CartStatus.OPEN)
                    .totalAmount(BigDecimal.ZERO)
                    .items(new ArrayList<>())
                    .build();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(empty);

            assertThatThrownBy(() -> checkoutService.calculateFreight(customerId,
                    FreightRequest.builder().addressId(UUID.randomUUID()).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vazio");
        }

        @Test
        @DisplayName("givenCartWithItems_whenCalculateFreight_thenPersistsFreightAndReturnsTotals")
        void givenCartWithItems_whenCalculateFreight_thenPersistsFreightAndReturnsTotals() {
            UUID addressId = UUID.randomUUID();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(freightService.calculate(cart, addressId, customerId)).thenReturn(new BigDecimal("10.00"));
            when(cartRepository.save(cart)).thenReturn(cart);

            var res = checkoutService.calculateFreight(customerId,
                    FreightRequest.builder().addressId(addressId).build());

            assertThat(res.getFreightAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(res.getGrandTotal()).isEqualByComparingTo(new BigDecimal("60.00"));
            verify(cartRepository).save(cart);
        }
    }

    @Nested
    @DisplayName("RF0035 — Endereço")
    class AddressRf {

        @Test
        @DisplayName("givenNoAddressPayload_whenApplyDelivery_thenThrows")
        void givenNoAddressPayload_whenApplyDelivery_thenThrows() {
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.applyDeliveryAddress(customerId, CheckoutAddressRequest.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("givenAddressId_whenApplyDelivery_thenLoadsFromRepository")
        void givenAddressId_whenApplyDelivery_thenLoadsFromRepository() {
            UUID aid = UUID.randomUUID();
            Address addr = Address.builder().id(aid).customer(customer).zipCode("01310100").build();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(addressRepository.findByIdAndCustomer_Id(aid, customerId)).thenReturn(Optional.of(addr));
            when(cartRepository.save(cart)).thenReturn(cart);

            checkoutService.applyDeliveryAddress(customerId,
                    CheckoutAddressRequest.builder().addressId(aid).build());

            verify(addressRepository).findByIdAndCustomer_Id(aid, customerId);
            verify(customerAddressService, never()).addAddress(any(), any());
            assertThat(cart.getDeliveryAddress()).isEqualTo(addr);
        }

        @Test
        @DisplayName("givenNewAddressAndSaveToProfileTrue_whenApply_thenUsesCustomerAddressService")
        void givenNewAddressAndSaveToProfileTrue_whenApply_thenUsesCustomerAddressService() {
            UUID savedId = UUID.randomUUID();
            Address addr = Address.builder().id(savedId).customer(customer).build();
            AddressCreateRequest payload = newAddressPayload();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(customerAddressService.addAddress(customerId, payload))
                    .thenReturn(AddressResponse.builder().id(savedId).build());
            when(addressRepository.findById(savedId)).thenReturn(Optional.of(addr));
            when(cartRepository.save(cart)).thenReturn(cart);

            checkoutService.applyDeliveryAddress(customerId, CheckoutAddressRequest.builder()
                    .newAddress(payload)
                    .saveToProfile(true)
                    .build());

            verify(customerAddressService).addAddress(customerId, payload);
            verify(addressRepository, never()).save(any(Address.class));
        }

        @Test
        @DisplayName("givenNewAddressAndSaveToProfileFalse_whenApply_thenSavesWithoutProfileService")
        void givenNewAddressAndSaveToProfileFalse_whenApply_thenSavesWithoutProfileService() {
            AddressCreateRequest payload = newAddressPayload();
            Address persisted = Address.builder().id(UUID.randomUUID()).customer(customer).build();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(addressRepository.save(any(Address.class))).thenReturn(persisted);
            when(cartRepository.save(cart)).thenReturn(cart);

            checkoutService.applyDeliveryAddress(customerId, CheckoutAddressRequest.builder()
                    .newAddress(payload)
                    .saveToProfile(false)
                    .build());

            verify(customerAddressService, never()).addAddress(any(), any());
            verify(addressRepository).save(any(Address.class));
        }
    }

    @Nested
    @DisplayName("RF0037 — Finalizar compra")
    class Finalize {

        @Test
        @DisplayName("givenNoFreight_whenFinalize_thenThrows")
        void givenNoFreight_whenFinalize_thenThrows() {
            cart.setDeliveryAddress(Address.builder().id(UUID.randomUUID()).customer(customer).build());
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Frete");
        }

        @Test
        @DisplayName("givenNoDeliveryAddress_whenFinalize_thenThrows")
        void givenNoDeliveryAddress_whenFinalize_thenThrows() {
            cart.setFreightAmount(new BigDecimal("10.00"));
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Endereço");
        }

        @Test
        @DisplayName("givenNoPaymentLines_whenFinalize_thenThrows")
        void givenNoPaymentLines_whenFinalize_thenThrows() {
            cart.setDeliveryAddress(Address.builder().id(UUID.randomUUID()).customer(customer).build());
            cart.setFreightAmount(new BigDecimal("10.00"));
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pagamento");
        }

        @Test
        @DisplayName("givenEmptyCart_whenFinalize_thenThrows")
        void givenEmptyCart_whenFinalize_thenThrows() {
            cart.getItems().clear();
            cart.setTotalAmount(BigDecimal.ZERO);
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vazio");
        }

        @Test
        @DisplayName("givenPaidLessThanGrandTotal_whenFinalize_thenThrows")
        void givenPaidLessThanGrandTotal_whenFinalize_thenThrows() {
            UUID addrId = UUID.randomUUID();
            cart.setDeliveryAddress(Address.builder().id(addrId).customer(customer).build());
            cart.setFreightAmount(new BigDecimal("10.00"));
            cart.getPaymentLines().add(CartPaymentLine.builder()
                    .cart(cart)
                    .paymentType(PaymentType.CREDIT_CARD)
                    .amount(new BigDecimal("10.00"))
                    .creditCardId(UUID.randomUUID())
                    .build());
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.finalizePurchase(customerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cobrir");
            verify(salesOrderRepository, never()).save(any(SalesOrder.class));
        }

        @Test
        @DisplayName("givenValidCheckout_whenFinalize_thenOrderIsEM_PROCESSAMENTOAndCartCheckedOut")
        void givenValidCheckout_whenFinalize_thenOrderIsEmProcessamentoAndCartCheckedOut() {
            UUID addrId = UUID.randomUUID();
            Address addr = Address.builder().id(addrId).customer(customer).build();
            cart.setDeliveryAddress(addr);
            cart.setFreightAmount(new BigDecimal("10.00"));
            UUID cardId = UUID.randomUUID();
            cart.getPaymentLines().add(CartPaymentLine.builder()
                    .cart(cart)
                    .paymentType(PaymentType.CREDIT_CARD)
                    .amount(new BigDecimal("60.00"))
                    .creditCardId(cardId)
                    .build());

            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)).thenReturn(Optional.of(
                    CreditCard.builder()
                            .id(cardId)
                            .customer(customer)
                            .cardholderName("Test")
                            .cardNumber("4111111111111111")
                            .brand("VISA")
                            .expirationMonth(12)
                            .expirationYear(2030)
                            .preferred(true)
                            .active(true)
                            .build()));
            when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
                SalesOrder o = inv.getArgument(0);
                if (o.getId() == null) {
                    o.setId(UUID.randomUUID());
                }
                return o;
            });
            when(orderService.toResponse(any(SalesOrder.class))).thenReturn(
                    OrderResponse.builder().id(UUID.randomUUID()).status(OrderStatus.EM_PROCESSAMENTO).build());

            OrderResponse res = checkoutService.finalizePurchase(customerId);

            assertThat(res.getStatus()).isEqualTo(OrderStatus.EM_PROCESSAMENTO);
            verify(inventoryService).assertInventoryAvailableForFulfillment(book.getId(), 1);
            verify(salesOrderRepository).save(any(SalesOrder.class));
            verify(auditLogService).logCreate(eq("SalesOrder"), any(UUID.class), any());

            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(cartCaptor.capture());
            assertThat(cartCaptor.getValue().getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        }

        @Test
        @DisplayName("givenEphemeralCard_whenFinalize_thenDeactivatesCard")
        void givenEphemeralCard_whenFinalize_thenDeactivatesCard() {
            UUID addrId = UUID.randomUUID();
            cart.setDeliveryAddress(Address.builder().id(addrId).customer(customer).build());
            cart.setFreightAmount(new BigDecimal("10.00"));
            UUID cardId = UUID.randomUUID();
            cart.setEphemeralCreditCardId(cardId);
            cart.getPaymentLines().add(CartPaymentLine.builder()
                    .cart(cart)
                    .paymentType(PaymentType.CREDIT_CARD)
                    .amount(new BigDecimal("60.00"))
                    .creditCardId(cardId)
                    .build());

            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)).thenReturn(Optional.of(
                    CreditCard.builder()
                            .id(cardId)
                            .customer(customer)
                            .cardholderName("Test")
                            .cardNumber("4111111111111111")
                            .brand("VISA")
                            .expirationMonth(12)
                            .expirationYear(2030)
                            .preferred(true)
                            .active(true)
                            .build()));
            when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
                SalesOrder o = inv.getArgument(0);
                if (o.getId() == null) {
                    o.setId(UUID.randomUUID());
                }
                return o;
            });
            when(orderService.toResponse(any(SalesOrder.class))).thenReturn(
                    OrderResponse.builder().id(UUID.randomUUID()).status(OrderStatus.EM_PROCESSAMENTO).build());

            checkoutService.finalizePurchase(customerId);

            verify(customerCreditCardService).deactivateCard(customerId, cardId);
        }
    }

    @Nested
    @DisplayName("RF0036 — validação de payload de pagamento")
    class PaymentPayload {

        @Test
        @DisplayName("givenNewCardWithoutSaveFlag_whenApplyPayment_thenThrows")
        void givenNewCardWithoutSaveFlag_whenApplyPayment_thenThrows() {
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            assertThatThrownBy(() -> checkoutService.applyPayment(customerId, CheckoutPaymentRequest.builder()
                    .lines(List.of(PaymentLineRequest.builder()
                            .paymentType(PaymentType.CREDIT_CARD)
                            .amount(new BigDecimal("50.00"))
                            .creditCardId(null)
                            .build()))
                    .newCreditCard(com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest.builder()
                            .cardholderName("A")
                            .cardNumber("4111111111111111")
                            .brand("VISA")
                            .expirationMonth(12)
                            .expirationYear(2030)
                            .build())
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("saveNewCardToProfile");
        }
    }
}
