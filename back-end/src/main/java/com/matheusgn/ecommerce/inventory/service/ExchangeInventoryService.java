package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.sales.entity.ExchangeRequest;
import com.matheusgn.ecommerce.sales.entity.ExchangeStatus;
import com.matheusgn.ecommerce.sales.repository.ExchangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeInventoryService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryBalanceService inventoryBalanceService;

    @Transactional
    public void applyExchangeReturnToStock(UUID exchangeRequestId) {
        if (inventoryMovementRepository.existsByReferenceTypeAndReferenceId(
                InventoryReferenceType.EXCHANGE, exchangeRequestId)) {
            return;
        }
        ExchangeRequest er = exchangeRequestRepository.findById(exchangeRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de troca não encontrada"));
        if (er.getStatus() != ExchangeStatus.RECEIVED) {
            throw new IllegalArgumentException("Somente troca com status RECEBIDA pode gerar reentrada de estoque");
        }
        if (!Boolean.TRUE.equals(er.getReturnToStock())) {
            throw new IllegalArgumentException("Troca não configurada para retorno ao estoque");
        }
        inventoryBalanceService.increaseStock(
                er.getOrderItem().getBook().getId(),
                er.getOrderItem().getQuantity(),
                InventoryMovementType.EXCHANGE_RETURN,
                InventoryReferenceType.EXCHANGE,
                exchangeRequestId,
                null);
    }
}
