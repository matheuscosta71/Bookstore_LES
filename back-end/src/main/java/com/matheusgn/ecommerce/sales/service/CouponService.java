package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.sales.entity.Coupon;
import com.matheusgn.ecommerce.sales.entity.CouponType;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CustomerRepository customerRepository;

    /**
     * Valida cupom para checkout e retorna o cupom (sem exigir valor aplicado).
     * Usado para o front obter {@code coupon.amount} antes de montar {@code PaymentLineRequest}.
     */
    public Coupon validateForCheckout(String code, UUID customerId, PaymentType paymentType) {
        if (paymentType != PaymentType.EXCHANGE_COUPON && paymentType != PaymentType.PROMOTIONAL_COUPON) {
            throw new IllegalArgumentException("Informe EXCHANGE_COUPON ou PROMOTIONAL_COUPON");
        }
        return loadCouponForPayment(code, customerId, paymentType);
    }

    public Coupon loadAndValidate(String code, UUID customerId, BigDecimal appliedAmount, PaymentType paymentType) {
        Coupon coupon = loadCouponForPayment(code, customerId, paymentType);

        if (appliedAmount.compareTo(coupon.getAmount()) != 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser igual ao valor do cupom");
        }

        return coupon;
    }

    private Coupon loadCouponForPayment(String code, UUID customerId, PaymentType paymentType) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado: " + code));

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Cupom inativo");
        }
        if (coupon.isRedeemed()) {
            throw new IllegalArgumentException("Cupom já utilizado");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cupom expirado");
        }

        if (paymentType == PaymentType.EXCHANGE_COUPON && coupon.getType() != CouponType.EXCHANGE) {
            throw new IllegalArgumentException("Tipo de cupom incompatível");
        }
        if (paymentType == PaymentType.PROMOTIONAL_COUPON && coupon.getType() != CouponType.PROMOTIONAL) {
            throw new IllegalArgumentException("Tipo de cupom incompatível");
        }
        if (coupon.getType() == CouponType.PROMOTIONAL && coupon.getCustomer() != null
                && !coupon.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Cupom promocional não pertence a este cliente");
        }
        if (coupon.getType() == CouponType.EXCHANGE && coupon.getCustomer() != null
                && !coupon.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Cupom de troca não pertence a este cliente");
        }

        return coupon;
    }

    @Transactional
    public void markRedeemed(Coupon coupon) {
        coupon.setRedeemed(true);
        couponRepository.save(coupon);
    }

    @Transactional
    public Coupon createExchangeCoupon(UUID customerId, BigDecimal amount, String code) {
        Customer customer = customerRepository.getReferenceById(customerId);
        Coupon c = Coupon.builder()
                .code(code)
                .type(CouponType.EXCHANGE)
                .amount(amount)
                .active(true)
                .expirationDate(LocalDate.now().plusYears(1))
                .customer(customer)
                .redeemed(false)
                .build();
        return couponRepository.save(c);
    }

    /**
     * RN0036: cupom de troco quando o valor em cupons excede o total da compra (somente cupons, sem cartão).
     */
    @Transactional
    public Coupon issueTrocoExchangeCoupon(UUID customerId, BigDecimal trocoAmount) {
        if (trocoAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de troco inválido");
        }
        String code = generateUniqueCouponCode("TROCO-");
        return createExchangeCoupon(customerId, trocoAmount.setScale(2, RoundingMode.HALF_UP), code);
    }

    private String generateUniqueCouponCode(String prefix) {
        String code;
        do {
            code = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (couponRepository.existsByCodeIgnoreCase(code));
        return code;
    }
}
