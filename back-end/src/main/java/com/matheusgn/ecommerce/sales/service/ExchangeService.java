package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.service.CustomerNotificationService;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.CreateExchangeRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeReceiveRequest;
import com.matheusgn.ecommerce.sales.dto.ExchangeRequestResponse;
import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final com.matheusgn.ecommerce.inventory.service.ExchangeInventoryService exchangeInventoryService;
    private final AdminOrderService adminOrderService;
    private final AuditLogService auditLogService;
    private final CustomerNotificationService customerNotificationService;

    @Transactional
    public ExchangeRequestResponse requestExchange(UUID customerId, UUID orderId, CreateExchangeRequest request) {
        SalesOrder order = salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        if (order.getStatus() != OrderStatus.ENTREGUE) {
            throw new IllegalArgumentException("Troca só permitida para pedido entregue");
        }
        if (exchangeRequestRepository.existsByOrder_IdAndStatusIn(order.getId(),
                EnumSet.of(ExchangeStatus.REQUESTED, ExchangeStatus.AUTHORIZED))) {
            throw new IllegalArgumentException("Já existe troca em andamento para este pedido");
        }
        OrderItem item = orderItemRepository.findByIdAndOrder_Id(request.getOrderItemId(), orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Item não pertence ao pedido"));
        if (item.isExchangeRequested()) {
            throw new IllegalArgumentException("Item já possui solicitação de troca");
        }
        if (exchangeRequestRepository.existsByOrderItem_Id(item.getId())) {
            throw new IllegalArgumentException("Item já possui solicitação de troca");
        }

        item.setExchangeRequested(true);

        ExchangeRequest er = ExchangeRequest.builder()
                .order(order)
                .orderItem(item)
                .customer(order.getCustomer())
                .status(ExchangeStatus.REQUESTED)
                .build();

        order.setStatus(OrderStatus.EM_TROCA);
        exchangeRequestRepository.save(er);
        orderItemRepository.save(item);
        salesOrderRepository.save(order);

        return toResponse(er);
    }

    @Transactional(readOnly = true)
    public List<ExchangeRequestResponse> listByOrderStatus(OrderStatus orderStatus, String adminKey) {
        adminOrderService.assertAdmin(adminKey);
        return exchangeRequestRepository.findByOrder_StatusOrderByCreatedAtDesc(orderStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExchangeRequestResponse authorize(UUID exchangeRequestId, String adminKey) {
        adminOrderService.assertAdmin(adminKey);
        ExchangeRequest er = exchangeRequestRepository.findById(exchangeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
        if (er.getStatus() != ExchangeStatus.REQUESTED) {
            throw new IllegalArgumentException("Somente solicitações pendentes podem ser autorizadas");
        }
        if (er.getOrder().getStatus() != OrderStatus.EM_TROCA) {
            throw new IllegalArgumentException("Pedido não está com status EM_TROCA");
        }
        er.setStatus(ExchangeStatus.AUTHORIZED);
        er.getOrder().setStatus(OrderStatus.TROCA_AUTORIZADA);
        exchangeRequestRepository.save(er);
        customerNotificationService.notifyExchangeAuthorized(
                er.getCustomer().getId(), er.getOrder().getId(), er.getId());
        return toResponse(er);
    }

    @Transactional
    public ExchangeRequestResponse receive(UUID exchangeRequestId, ExchangeReceiveRequest request, String adminKey) {
        adminOrderService.assertAdmin(adminKey);
        ExchangeRequest er = exchangeRequestRepository.findById(exchangeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
        if (er.getStatus() != ExchangeStatus.AUTHORIZED) {
            throw new IllegalArgumentException("Somente solicitações autorizadas podem ser recebidas");
        }
        if (er.getOrder().getStatus() != OrderStatus.TROCA_AUTORIZADA) {
            throw new IllegalArgumentException("Pedido não está com status TROCA_AUTORIZADA");
        }

        er.setReturnToStock(request.getReturnToStock());
        er.setStatus(ExchangeStatus.RECEIVED);

        String code = newUniqueCouponCode();
        BigDecimal amount = er.getOrderItem().getTotalPrice();
        couponService.createExchangeCoupon(er.getCustomer().getId(), amount, code);
        er.setGeneratedCouponCode(code);

        if (Boolean.TRUE.equals(request.getReturnToStock())) {
            exchangeInventoryService.applyExchangeReturnToStock(er.getId());
        }

        er.getOrder().setStatus(OrderStatus.ENTREGUE);
        exchangeRequestRepository.save(er);
        customerNotificationService.notifyExchangeReceived(er.getCustomer().getId(), er.getOrder().getId(), code);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("status", ExchangeStatus.RECEIVED.name());
        audit.put("returnToStock", Boolean.TRUE.equals(er.getReturnToStock()));
        audit.put("orderId", er.getOrder().getId());
        auditLogService.logUpdate("ExchangeRequest", er.getId(), audit);
        return toResponse(er);
    }

    private String newUniqueCouponCode() {
        String code;
        do {
            code = "TROCA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (couponRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    private ExchangeRequestResponse toResponse(ExchangeRequest er) {
        return ExchangeRequestResponse.builder()
                .id(er.getId())
                .orderId(er.getOrder().getId())
                .orderItemId(er.getOrderItem().getId())
                .customerId(er.getCustomer().getId())
                .bookId(er.getOrderItem().getBook().getId())
                .bookTitle(er.getOrderItem().getBook().getTitle())
                .orderStatus(er.getOrder().getStatus())
                .exchangeStatus(er.getStatus())
                .returnToStock(er.getReturnToStock())
                .generatedCouponCode(er.getGeneratedCouponCode())
                .createdAt(er.getCreatedAt())
                .updatedAt(er.getUpdatedAt())
                .build();
    }
}
