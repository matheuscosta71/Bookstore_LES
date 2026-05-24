package com.matheusgn.ecommerce.sales.controller;

import com.matheusgn.ecommerce.sales.dto.CouponResponse;
import com.matheusgn.ecommerce.sales.dto.CreateCouponRequest;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import com.matheusgn.ecommerce.sales.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin — cupons")
public class AdminCouponController {

    private final CouponService couponService;
    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "Listar todos os cupons (paginado)")
    public Page<CouponResponse> list(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        adminOrderService.assertAdmin(adminKey);
        return couponService.listAllCoupons(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar novo cupom (promocional ou troca)")
    public CouponResponse create(
            @RequestHeader("X-Admin-Key") String adminKey,
            @Valid @RequestBody CreateCouponRequest request) {
        adminOrderService.assertAdmin(adminKey);
        return couponService.createCoupon(request);
    }

    @PatchMapping("/{couponId}/toggle-active")
    @Operation(summary = "Ativar/Desativar um cupom")
    public CouponResponse toggleActive(
            @PathVariable UUID couponId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        adminOrderService.assertAdmin(adminKey);
        return couponService.toggleActive(couponId);
    }
}
