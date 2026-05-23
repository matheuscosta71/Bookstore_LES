package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.CheckoutAddressRequest;
import com.matheusgn.ecommerce.sales.dto.CheckoutPaymentRequest;
import com.matheusgn.ecommerce.sales.dto.CouponValidateRequest;
import com.matheusgn.ecommerce.sales.dto.CouponValidateResponse;
import com.matheusgn.ecommerce.sales.dto.FreightRequest;
import com.matheusgn.ecommerce.sales.dto.FreightResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.service.CheckoutService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers/{customerId}/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CouponService couponService;

    @PostMapping("/freight")
    @Operation(summary = "Calcular frete")
    public FreightResponse freight(
            @PathVariable UUID customerId,
            @Valid @RequestBody FreightRequest request) {
        log.info("[CheckoutController][freight] Calcular frete customerId={} addressId={}",
                customerId, request.getAddressId());
        return checkoutService.calculateFreight(customerId, request);
    }

    @PostMapping("/address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Definir endereço de entrega")
    public void address(
            @PathVariable UUID customerId,
            @Valid @RequestBody CheckoutAddressRequest request) {
        log.info("[CheckoutController][address] Aplicar endereço customerId={} addressId={} newAddress={}",
                customerId, request.getAddressId(), request.getNewAddress() != null);
        checkoutService.applyDeliveryAddress(customerId, request);
    }

    @PostMapping("/coupon/validate")
    @Operation(summary = "Validar cupom e retornar valor (para montar linhas de pagamento)")
    public CouponValidateResponse validateCoupon(
            @PathVariable UUID customerId,
            @Valid @RequestBody CouponValidateRequest request) {
        log.info("[CheckoutController][validateCoupon] Validar cupom customerId={} paymentType={}",
                customerId, request.getPaymentType());
        var coupon = couponService.validateForCheckout(
                request.getCode().trim(), customerId, request.getPaymentType());
        return CouponValidateResponse.builder()
                .amount(coupon.getAmount())
                .build();
    }

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Definir formas de pagamento")
    public void payment(
            @PathVariable UUID customerId,
            @Valid @RequestBody CheckoutPaymentRequest request) {
        log.info("[CheckoutController][payment] Definir pagamento customerId={} lines={}",
                customerId, request.getLines() != null ? request.getLines().size() : 0);
        checkoutService.applyPayment(customerId, request);
    }

    @PostMapping("/finalize")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Finalizar compra")
    public OrderResponse finalize(@PathVariable UUID customerId) {
        log.info("[CheckoutController][finalize] Finalizar compra customerId={}", customerId);
        return checkoutService.finalizePurchase(customerId);
    }
}
