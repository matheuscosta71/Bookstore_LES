package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.sales.dto.CouponResponse;
import com.matheusgn.ecommerce.sales.dto.CreateCouponRequest;
import com.matheusgn.ecommerce.sales.entity.Coupon;
import com.matheusgn.ecommerce.sales.entity.CouponType;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public Page<CouponResponse> listAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new IllegalArgumentException("Já existe um cupom cadastrado com este código.");
        }

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new com.matheusgn.ecommerce.exception.ResourceNotFoundException("Cliente não encontrado"));
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .type(request.getType())
                .amount(request.getAmount())
                .active(true)
                .expirationDate(request.getExpirationDate())
                .customer(customer)
                .redeemed(false)
                .build();

        coupon = couponRepository.save(coupon);
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse toggleActive(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new com.matheusgn.ecommerce.exception.ResourceNotFoundException("Cupom não encontrado"));
        coupon.setActive(!coupon.isActive());
        coupon = couponRepository.save(coupon);
        return toResponse(coupon);
    }

    public CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .amount(coupon.getAmount())
                .active(coupon.isActive())
                .expirationDate(coupon.getExpirationDate())
                .redeemed(coupon.isRedeemed())
                .customerId(coupon.getCustomer() != null ? coupon.getCustomer().getId() : null)
                .customerName(coupon.getCustomer() != null ? coupon.getCustomer().getFullName() : null)
                .build();
    }
}
