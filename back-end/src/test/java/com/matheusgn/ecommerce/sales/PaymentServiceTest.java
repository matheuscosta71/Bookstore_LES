package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.entity.CreditCard;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.customer.repository.CreditCardRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.customer.service.CustomerTransactionRecorder;
import com.matheusgn.ecommerce.customer.dto.CreditCardResponse;
import com.matheusgn.ecommerce.sales.dto.CheckoutPaymentRequest;
import com.matheusgn.ecommerce.sales.dto.PaymentLineRequest;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.repository.CartRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.service.CartExpirationService;
import com.matheusgn.ecommerce.sales.service.CartService;
import com.matheusgn.ecommerce.sales.service.CheckoutService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import com.matheusgn.ecommerce.sales.service.FreightService;
import com.matheusgn.ecommerce.inventory.service.SalesOutboundService;
import com.matheusgn.ecommerce.sales.service.InventoryService;
import com.matheusgn.ecommerce.sales.service.OrderService;
import com.matheusgn.ecommerce.sales.support.SalesTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cenários de pagamento do checkout ({@link CheckoutService#applyPayment}).
 * Não existe {@code PaymentService} no domínio; esta classe agrupa testes de pagamento.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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
    private SalesOutboundService salesOutboundService;
    @Mock
    private OrderService orderService;
    @Mock
    private CartExpirationService cartExpirationService;
    @Mock
    private CustomerTransactionRecorder customerTransactionRecorder;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID customerId;
    private Customer customer;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customerId = SalesTestFixtures.CUSTOMER_ID;
        customer = SalesTestFixtures.customer();
        cart = SalesTestFixtures.openCartWithOneItem(
                customer, SalesTestFixtures.book(new BigDecimal("40.00")), new BigDecimal("40.00"));
    }

    @Nested
    @DisplayName("RF0036 — cartão e cupom")
    class Rf0036 {

        @Test
        @DisplayName("givenExistingCard_whenApplyPayment_thenResolvesCardFromRepository")
        void usesExistingCard() {
            UUID cardId = UUID.randomUUID();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)).thenReturn(Optional.of(
                    CreditCard.builder()
                            .id(cardId)
                            .customer(customer)
                            .cardholderName("A")
                            .cardNumber("4111111111111111")
                            .brand("VISA")
                            .expirationMonth(12)
                            .expirationYear(2030)
                            .preferred(true)
                            .active(true)
                            .build()));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            checkoutService.applyPayment(customerId, CheckoutPaymentRequest.builder()
                    .lines(List.of(PaymentLineRequest.builder()
                            .paymentType(PaymentType.CREDIT_CARD)
                            .amount(new BigDecimal("40.00"))
                            .creditCardId(cardId)
                            .build()))
                    .build());

            verify(creditCardRepository).findByIdAndCustomer_Id(cardId, customerId);
            verify(couponService, never()).loadAndValidate(any(), any(), any(), any());
        }

        @Test
        @DisplayName("givenNewCardAndSaveToProfileTrue_whenApply_thenDoesNotSetEphemeralId")
        void newCardSavedToProfile_noEphemeral() {
            UUID newCardId = UUID.randomUUID();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(customerCreditCardService.addCard(eq(customerId), any(CreditCardCreateRequest.class)))
                    .thenReturn(CreditCardResponse.builder().id(newCardId).build());
            when(creditCardRepository.findByIdAndCustomer_Id(newCardId, customerId)).thenReturn(Optional.of(
                    CreditCard.builder()
                            .id(newCardId)
                            .customer(customer)
                            .cardholderName("A")
                            .cardNumber("4222222222222222")
                            .brand("MC")
                            .expirationMonth(11)
                            .expirationYear(2031)
                            .preferred(false)
                            .active(true)
                            .build()));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            checkoutService.applyPayment(customerId, CheckoutPaymentRequest.builder()
                    .lines(List.of(PaymentLineRequest.builder()
                            .paymentType(PaymentType.CREDIT_CARD)
                            .amount(new BigDecimal("40.00"))
                            .creditCardId(null)
                            .build()))
                    .newCreditCard(CreditCardCreateRequest.builder()
                            .cardholderName("A")
                            .cardNumber("4222222222222222")
                            .brand("MC")
                            .expirationMonth(11)
                            .expirationYear(2031)
                            .build())
                    .saveNewCardToProfile(true)
                    .build());

            assertThat(cart.getEphemeralCreditCardId()).isNull();
            verify(customerCreditCardService).addCard(eq(customerId), any(CreditCardCreateRequest.class));
        }

        @Test
        @DisplayName("givenCardPlusPromoCoupon_whenApply_thenValidatesCouponLine")
        void cardAndPromoComposition() {
            UUID cardId = UUID.randomUUID();
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)).thenReturn(Optional.of(
                    CreditCard.builder()
                            .id(cardId)
                            .customer(customer)
                            .cardholderName("A")
                            .cardNumber("4111111111111111")
                            .brand("VISA")
                            .expirationMonth(12)
                            .expirationYear(2030)
                            .preferred(true)
                            .active(true)
                            .build()));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            checkoutService.applyPayment(customerId, CheckoutPaymentRequest.builder()
                    .lines(List.of(
                            PaymentLineRequest.builder()
                                    .paymentType(PaymentType.CREDIT_CARD)
                                    .amount(new BigDecimal("35.00"))
                                    .creditCardId(cardId)
                                    .build(),
                            PaymentLineRequest.builder()
                                    .paymentType(PaymentType.PROMOTIONAL_COUPON)
                                    .amount(new BigDecimal("5.00"))
                                    .couponCode("PROMO5")
                                    .build()))
                    .build());

            verify(couponService).loadAndValidate(eq("PROMO5"), eq(customerId), eq(new BigDecimal("5.00")),
                    eq(PaymentType.PROMOTIONAL_COUPON));
        }

        @Test
        @DisplayName("givenCouponValidationFails_whenApply_thenPropagates")
        void invalidCouponPropagates() {
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);
            when(couponService.loadAndValidate(eq("BAD"), eq(customerId), any(), eq(PaymentType.PROMOTIONAL_COUPON)))
                    .thenThrow(new IllegalArgumentException("Cupom inativo"));

            assertThatThrownBy(() -> checkoutService.applyPayment(customerId, CheckoutPaymentRequest.builder()
                    .lines(List.of(PaymentLineRequest.builder()
                            .paymentType(PaymentType.PROMOTIONAL_COUPON)
                            .amount(new BigDecimal("5.00"))
                            .couponCode("BAD")
                            .build()))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("givenUnnecessaryCoupon_whenApplyPayment_thenThrowsException")
        void unnecessaryCouponThrows() {
            when(cartService.getOpenCartOrThrow(customerId)).thenReturn(cart);

            CheckoutPaymentRequest req = CheckoutPaymentRequest.builder()
                    .lines(List.of(
                            PaymentLineRequest.builder()
                                    .paymentType(PaymentType.EXCHANGE_COUPON)
                                    .amount(new BigDecimal("30.00"))
                                    .couponCode("TROCA1")
                                    .build(),
                            PaymentLineRequest.builder()
                                    .paymentType(PaymentType.EXCHANGE_COUPON)
                                    .amount(new BigDecimal("30.00"))
                                    .couponCode("TROCA2")
                                    .build(),
                            PaymentLineRequest.builder()
                                    .paymentType(PaymentType.EXCHANGE_COUPON)
                                    .amount(new BigDecimal("30.00"))
                                    .couponCode("TROCA3")
                                    .build()
                    ))
                    .build();

            assertThatThrownBy(() -> checkoutService.applyPayment(customerId, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("desnecessário");
        }
    }
}
