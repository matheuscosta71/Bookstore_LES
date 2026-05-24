package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.sales.entity.Coupon;
import com.matheusgn.ecommerce.sales.entity.CouponType;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import com.matheusgn.ecommerce.sales.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CouponService couponService;

    @Nested
    @DisplayName("loadAndValidate — RF0036")
    class LoadAndValidate {

        @Test
        @DisplayName("givenExpiredCoupon_whenLoad_thenThrows")
        void rejectsExpired() {
            UUID customerId = UUID.randomUUID();
            Coupon c = Coupon.builder()
                    .code("PROMO")
                    .type(CouponType.PROMOTIONAL)
                    .amount(new BigDecimal("5.00"))
                    .active(true)
                    .redeemed(false)
                    .expirationDate(LocalDate.now().minusDays(1))
                    .build();
            when(couponRepository.findByCodeIgnoreCase("PROMO")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("PROMO", customerId, new BigDecimal("5.00"),
                    PaymentType.PROMOTIONAL_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("givenExchangeCouponOfOtherCustomer_whenLoad_thenThrows")
        void rejectsExchangeCouponWrongCustomer() {
            UUID owner = UUID.randomUUID();
            UUID other = UUID.randomUUID();
            Coupon c = Coupon.builder()
                    .code("TROCA-X")
                    .type(CouponType.EXCHANGE)
                    .amount(new BigDecimal("20.00"))
                    .active(true)
                    .redeemed(false)
                    .expirationDate(LocalDate.now().plusMonths(1))
                    .customer(Customer.builder().id(owner).build())
                    .build();
            when(couponRepository.findByCodeIgnoreCase("TROCA-X")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("TROCA-X", other, new BigDecimal("20.00"),
                    PaymentType.EXCHANGE_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("troca");
        }

        @Test
        @DisplayName("givenInactiveCoupon_whenLoad_thenThrows")
        void rejectsInactive() {
            UUID customerId = UUID.randomUUID();
            Coupon c = baseCoupon("OFF", CouponType.PROMOTIONAL).active(false).build();
            when(couponRepository.findByCodeIgnoreCase("OFF")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("OFF", customerId, new BigDecimal("5.00"),
                    PaymentType.PROMOTIONAL_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("givenRedeemedCoupon_whenLoad_thenThrows")
        void rejectsRedeemed() {
            UUID customerId = UUID.randomUUID();
            Coupon c = baseCoupon("USED", CouponType.PROMOTIONAL).redeemed(true).build();
            when(couponRepository.findByCodeIgnoreCase("USED")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("USED", customerId, new BigDecimal("5.00"),
                    PaymentType.PROMOTIONAL_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("utilizado");
        }

        @Test
        @DisplayName("givenPromoCouponWithWrongPaymentType_whenLoad_thenThrows")
        void rejectsTypeMismatch() {
            UUID customerId = UUID.randomUUID();
            Coupon c = baseCoupon("P", CouponType.PROMOTIONAL).build();
            when(couponRepository.findByCodeIgnoreCase("P")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("P", customerId, new BigDecimal("5.00"),
                    PaymentType.EXCHANGE_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("incompatível");
        }

        @Test
        @DisplayName("givenAmountDifferentFromCoupon_whenLoad_thenThrows")
        void rejectsAmountMismatch() {
            UUID customerId = UUID.randomUUID();
            Coupon c = baseCoupon("FIX", CouponType.PROMOTIONAL).amount(new BigDecimal("10.00")).build();
            when(couponRepository.findByCodeIgnoreCase("FIX")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("FIX", customerId, new BigDecimal("9.99"),
                    PaymentType.PROMOTIONAL_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Valor");
        }

        @Test
        @DisplayName("givenPromoRestrictedToOtherCustomer_whenLoad_thenThrows")
        void rejectsPromoWrongOwner() {
            UUID owner = UUID.randomUUID();
            UUID buyer = UUID.randomUUID();
            Coupon c = baseCoupon("VIP", CouponType.PROMOTIONAL)
                    .customer(Customer.builder().id(owner).build())
                    .build();
            when(couponRepository.findByCodeIgnoreCase("VIP")).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> couponService.loadAndValidate("VIP", buyer, new BigDecimal("5.00"),
                    PaymentType.PROMOTIONAL_COUPON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("promocional");
        }
    }

    @Nested
    @DisplayName("createExchangeCoupon — RF0044")
    class CreateExchange {

        @Test
        @DisplayName("givenCustomer_whenCreateExchangeCoupon_thenPersistsWithReference")
        void persistsWithCustomerReference() {
            UUID customerId = UUID.randomUUID();
            Customer ref = Customer.builder().id(customerId).build();
            when(customerRepository.getReferenceById(customerId)).thenReturn(ref);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

            Coupon saved = couponService.createExchangeCoupon(customerId, new BigDecimal("33.00"), "TROCA-ABCDEF12");

            assertThat(saved.getCode()).isEqualTo("TROCA-ABCDEF12");
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.isRedeemed()).isFalse();
            assertThat(saved.getType()).isEqualTo(CouponType.EXCHANGE);
            verify(couponRepository).save(any(Coupon.class));
        }
    }

    @Nested
    @DisplayName("markRedeemed")
    class Redeem {

        @Test
        @DisplayName("givenCoupon_whenMarkRedeemed_thenSavesRedeemedTrue")
        void marksRedeemed() {
            Coupon c = baseCoupon("R", CouponType.PROMOTIONAL).redeemed(false).build();
            when(couponRepository.save(c)).thenReturn(c);

            couponService.markRedeemed(c);

            assertThat(c.isRedeemed()).isTrue();
            verify(couponRepository).save(c);
        }
    }

    @Nested
    @DisplayName("admin coupon operations")
    class AdminCoupons {

        @Test
        @DisplayName("givenCreateCouponRequest_whenCreateCoupon_thenSavesCoupon")
        void adminCreatesCouponSuccessfully() {
            com.matheusgn.ecommerce.sales.dto.CreateCouponRequest req = com.matheusgn.ecommerce.sales.dto.CreateCouponRequest.builder()
                    .code("SAVE30")
                    .type(CouponType.PROMOTIONAL)
                    .amount(new BigDecimal("30.00"))
                    .expirationDate(LocalDate.now().plusMonths(3))
                    .build();

            when(couponRepository.existsByCodeIgnoreCase("SAVE30")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

            com.matheusgn.ecommerce.sales.dto.CouponResponse resp = couponService.createCoupon(req);

            assertThat(resp.getCode()).isEqualTo("SAVE30");
            assertThat(resp.getAmount()).isEqualByComparingTo("30.00");
            assertThat(resp.getType()).isEqualTo(CouponType.PROMOTIONAL);
            assertThat(resp.isActive()).isTrue();
            assertThat(resp.isRedeemed()).isFalse();
            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("givenDuplicateCode_whenCreateCoupon_thenThrowsException")
        void adminDuplicateCodeThrows() {
            com.matheusgn.ecommerce.sales.dto.CreateCouponRequest req = com.matheusgn.ecommerce.sales.dto.CreateCouponRequest.builder()
                    .code("DUPLICATE")
                    .type(CouponType.PROMOTIONAL)
                    .amount(new BigDecimal("10.00"))
                    .build();

            when(couponRepository.existsByCodeIgnoreCase("DUPLICATE")).thenReturn(true);

            assertThatThrownBy(() -> couponService.createCoupon(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Já existe");
        }

        @Test
        @DisplayName("givenCouponId_whenToggleActive_thenInvertsStatus")
        void adminTogglesActiveStatus() {
            UUID couponId = UUID.randomUUID();
            Coupon coupon = baseCoupon("ACTIVE_TEST", CouponType.PROMOTIONAL).active(true).build();
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(coupon)).thenReturn(coupon);

            com.matheusgn.ecommerce.sales.dto.CouponResponse resp = couponService.toggleActive(couponId);
            assertThat(resp.isActive()).isFalse();

            coupon.setActive(false);
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
            resp = couponService.toggleActive(couponId);
            assertThat(resp.isActive()).isTrue();
        }
    }

    private static Coupon.CouponBuilder baseCoupon(String code, CouponType type) {
        return Coupon.builder()
                .code(code)
                .type(type)
                .amount(new BigDecimal("5.00"))
                .active(true)
                .redeemed(false)
                .expirationDate(LocalDate.now().plusWeeks(1));
    }
}
