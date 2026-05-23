package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import com.matheusgn.ecommerce.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesOutboundService {

    private final SalesOrderRepository salesOrderRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryBalanceService inventoryBalanceService;

    @Transactional
    public void applySalesOutbound(UUID orderId) {
        if (inventoryMovementRepository.existsByReferenceTypeAndReferenceId(InventoryReferenceType.ORDER, orderId)) {
            return;
        }
        SalesOrder order = salesOrderRepository.findByIdWithItemsAndBooks(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
        for (OrderItem item : order.getItems()) {
            inventoryBalanceService.decreaseStock(
                    item.getBook().getId(),
                    item.getQuantity(),
                    InventoryMovementType.SALE_OUTBOUND,
                    InventoryReferenceType.ORDER,
                    orderId,
                    null);
        }
    }
}
