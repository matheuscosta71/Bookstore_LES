package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.config.AdminProperties;
import com.matheusgn.ecommerce.config.PageConstraints;
import com.matheusgn.ecommerce.customer.service.CustomerTransactionRecorder;
import com.matheusgn.ecommerce.exception.ForbiddenException;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.service.SalesOutboundService;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.Payment;
import com.matheusgn.ecommerce.sales.entity.PaymentType;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final OrderService orderService;
    private final AdminProperties adminProperties;
    private final SalesOutboundService salesOutboundService;
    private final CustomerTransactionRecorder customerTransactionRecorder;
    private final CouponService couponService;

    public void assertAdmin(String adminKey) {
        if (adminKey == null || !adminProperties.getKey().equals(adminKey)) {
            throw new ForbiddenException("Chave administrativa inválida");
        }
    }

    /**
     * Lista pedidos (logística) com filtros opcionais, mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(
            Pageable pageable,
            String adminKey,
            String orderNumber,
            String customerName,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal totalMin,
            BigDecimal totalMax) {
        assertAdmin(adminKey);
        Pageable p = PageConstraints.clamp(pageable);

        OrderStatus st = null;
        if (StringUtils.hasText(status)) {
            try {
                st = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido: " + status);
            }
        }

        BigDecimal tMin = totalMin;
        BigDecimal tMax = totalMax;
        if (tMin != null && tMax != null && tMin.compareTo(tMax) > 0) {
            BigDecimal tmp = tMin;
            tMin = tMax;
            tMax = tmp;
        }

        Specification<SalesOrder> spec = SalesOrderSpecifications.withAdminFilters(
                orderNumber, customerName, st, dateFrom, dateTo, tMin, tMax);
        return salesOrderRepository.findAll(spec, p).map(orderService::toResponse);
    }

    /**
     * RN0028 — Após retorno positivo da operadora: aprova pagamento, baixa estoque, extrato e resgate de cupons.
     */
    @Transactional
    public OrderResponse approvePayment(UUID orderId, String adminKey) {
        assertAdmin(adminKey);
        SalesOrder order = salesOrderRepository.findByIdWithItemsAndBooks(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (order.getStatus() != OrderStatus.EM_PROCESSAMENTO) {
            throw new IllegalArgumentException(
                    "Somente pedidos em processamento aguardando pagamento podem ser aprovados.");
        }
        order.setStatus(OrderStatus.APROVADO);
        salesOrderRepository.save(order);

        for (Payment p : order.getPayments()) {
            if (p.getPaymentType() == PaymentType.EXCHANGE_COUPON
                    || p.getPaymentType() == PaymentType.PROMOTIONAL_COUPON) {
                var coupon = couponService.loadAndValidate(
                        p.getCouponCode(),
                        order.getCustomer().getId(),
                        p.getAmount(),
                        p.getPaymentType());
                couponService.markRedeemed(coupon);
            }
        }

        salesOutboundService.applySalesOutbound(orderId);
        customerTransactionRecorder.recordPurchaseForCompletedOrder(order);
        return orderService.toResponse(
                salesOrderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado")));
    }

    /**
     * RN0028 — Pagamento não aprovado: encerra pedido sem baixa de estoque.
     */
    @Transactional
    public OrderResponse rejectPayment(UUID orderId, String adminKey) {
        assertAdmin(adminKey);
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (order.getStatus() != OrderStatus.EM_PROCESSAMENTO) {
            throw new IllegalArgumentException("Somente pedidos em processamento podem ser rejeitados.");
        }
        order.setStatus(OrderStatus.PAGAMENTO_RECUSADO);
        salesOrderRepository.save(order);
        return orderService.toResponse(order);
    }

    @Transactional
    public OrderResponse dispatch(UUID orderId, String adminKey) {
        assertAdmin(adminKey);
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (order.getStatus() != OrderStatus.APROVADO) {
            throw new IllegalArgumentException("Somente pedidos com pagamento aprovado podem ser despachados.");
        }
        order.setStatus(OrderStatus.EM_TRANSITO);
        salesOrderRepository.save(order);
        return orderService.toResponse(order);
    }

    @Transactional
    public OrderResponse markDelivered(UUID orderId, String adminKey) {
        assertAdmin(adminKey);
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (order.getStatus() != OrderStatus.EM_TRANSITO) {
            throw new IllegalArgumentException("Somente pedidos em trânsito podem ser marcados como entregues");
        }
        order.setStatus(OrderStatus.ENTREGUE);
        salesOrderRepository.save(order);
        return orderService.toResponse(order);
    }
}
