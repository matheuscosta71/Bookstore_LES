package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.dto.OrderItemResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.dto.PaymentResponse;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.Payment;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import com.matheusgn.ecommerce.sales.util.OrderNumberFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> listByCustomer(UUID customerId) {
        List<SalesOrder> orders = salesOrderRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<UUID> orderIds = orders.stream().map(SalesOrder::getId).toList();
        Map<UUID, String> couponByOrderId = new HashMap<>();
        List<ExchangeRequest> received =
                exchangeRequestRepository.findByOrder_IdInAndStatus(orderIds, ExchangeStatus.RECEIVED);
        for (ExchangeRequest er : received) {
            if (er.getGeneratedCouponCode() != null) {
                couponByOrderId.putIfAbsent(er.getOrder().getId(), er.getGeneratedCouponCode());
            }
        }
        return orders.stream()
                .map(o -> toResponse(o, couponByOrderId.get(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getByCustomer(UUID customerId, UUID orderId) {
        SalesOrder order = salesOrderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        String coupon = exchangeRequestRepository.findByOrder_IdInAndStatus(List.of(orderId), ExchangeStatus.RECEIVED)
                .stream()
                .map(ExchangeRequest::getGeneratedCouponCode)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse(null);
        return toResponse(order, coupon);
    }

    @Transactional(readOnly = true)
    public OrderResponse toResponse(SalesOrder order) {
        return toResponse(order, null);
    }

    @Transactional(readOnly = true)
    public OrderResponse toResponse(SalesOrder order, String exchangeCouponCode) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(OrderNumberFormatter.fromUuid(order.getId()))
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .status(order.getStatus())
                .freightAmount(order.getFreightAmount())
                .itemsSubtotal(order.getItemsSubtotal())
                .totalAmount(order.getTotalAmount())
                .deliveryAddressId(order.getDeliveryAddress().getId())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(this::toItemResponse).toList())
                .payments(order.getPayments().stream().map(this::toPaymentResponse).toList())
                .exchangeCouponCode(exchangeCouponCode)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem oi) {
        return OrderItemResponse.builder()
                .id(oi.getId())
                .bookId(oi.getBook().getId())
                .title(oi.getBook().getTitle())
                .quantity(oi.getQuantity())
                .unitPrice(oi.getUnitPrice())
                .totalPrice(oi.getTotalPrice())
                .exchangeRequested(oi.isExchangeRequested())
                .build();
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentType(p.getPaymentType())
                .amount(p.getAmount())
                .cardLastDigits(p.getCardLastDigits())
                .couponCode(p.getCouponCode())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
