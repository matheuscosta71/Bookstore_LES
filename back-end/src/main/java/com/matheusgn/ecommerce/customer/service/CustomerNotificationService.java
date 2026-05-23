package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.entity.CustomerNotification;
import com.matheusgn.ecommerce.customer.repository.CustomerNotificationRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * RNF0046: registro de notificação ao cliente (ex.: troca autorizada). Persistido para consulta futura ou integração com e-mail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    public static final String TYPE_EXCHANGE_AUTHORIZED = "EXCHANGE_AUTHORIZED";
    public static final String TYPE_EXCHANGE_RECEIVED = "EXCHANGE_RECEIVED";

    private final CustomerNotificationRepository customerNotificationRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void notifyExchangeAuthorized(UUID customerId, UUID orderId, UUID exchangeRequestId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        String message = String.format(
                "Sua solicitação de troca foi autorizada. Pedido: %s. Acompanhe o status do pedido para os próximos passos.",
                orderId);
        CustomerNotification n = CustomerNotification.builder()
                .customer(customer)
                .type(TYPE_EXCHANGE_AUTHORIZED)
                .message(message)
                .createdAt(Instant.now())
                .build();
        customerNotificationRepository.save(n);
        log.info("[RNF0046] Notificação de troca autorizada customerId={} orderId={} exchangeRequestId={}",
                customerId, orderId, exchangeRequestId);
    }

    @Transactional
    public void notifyExchangeReceived(UUID customerId, UUID orderId, String couponCode) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        String message = String.format(
                "Recebemos o item da troca. Seu cupom de troca: %s. Use no checkout em compras futuras.",
                couponCode);
        CustomerNotification n = CustomerNotification.builder()
                .customer(customer)
                .type(TYPE_EXCHANGE_RECEIVED)
                .message(message)
                .createdAt(Instant.now())
                .build();
        customerNotificationRepository.save(n);
        log.info("[RNF0046] Notificação de troca recebida customerId={} orderId={} couponCode={}",
                customerId, orderId, couponCode);
    }
}
