package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.customer.entity.CustomerTransaction;
import com.matheusgn.ecommerce.customer.entity.TransactionType;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerTransactionRepository;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registra eventos no extrato do cliente ({@link CustomerTransaction}) após operações de negócio concluídas.
 */
@Service
@RequiredArgsConstructor
public class CustomerTransactionRecorder {

    private final CustomerTransactionRepository customerTransactionRepository;
    private final CustomerRepository customerRepository;

    /**
     * Uma linha {@link TransactionType#PURCHASE} por pedido com pagamento aprovado (valor total com frete).
     * RN0027 — incrementa {@code rankingScore} pelo valor inteiro da compra.
     */
    @Transactional
    public void recordPurchaseForCompletedOrder(SalesOrder order) {
        Instant when = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
        CustomerTransaction tx = CustomerTransaction.builder()
                .customer(order.getCustomer())
                .description("Compra — pedido " + order.getId())
                .amount(order.getTotalAmount())
                .transactionDate(when)
                .type(TransactionType.PURCHASE)
                .build();
        customerTransactionRepository.save(tx);

        UUID customerId = order.getCustomer().getId();
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        int delta = total.intValue();
        if (delta > 0) {
            customerRepository.findById(customerId).ifPresent(c -> {
                c.setRankingScore(c.getRankingScore() + delta);
                customerRepository.save(c);
            });
        }
    }
}
