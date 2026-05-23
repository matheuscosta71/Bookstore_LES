package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CreditCardMapper;
import com.matheusgn.ecommerce.customer.dto.CreditCardResponse;
import com.matheusgn.ecommerce.customer.dto.CreditCardUpdateRequest;
import com.matheusgn.ecommerce.customer.entity.CreditCard;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CreditCardRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerCreditCardService {

    private final CustomerRepository customerRepository;
    private final CreditCardRepository creditCardRepository;

    @Transactional
    public CreditCardResponse addCard(UUID customerId, CreditCardCreateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        long activeCount = creditCardRepository.countByCustomer_IdAndActiveTrue(customerId);
        boolean wantsPreferred = Boolean.TRUE.equals(request.getPreferred());

        CreditCard card = CreditCard.builder()
                .customer(customer)
                .cardholderName(request.getCardholderName().trim())
                .cardNumber(request.getCardNumber().replaceAll("\\s", ""))
                .brand(request.getBrand().trim())
                .expirationMonth(request.getExpirationMonth())
                .expirationYear(request.getExpirationYear())
                .preferred(false)
                .active(true)
                .build();

        if (activeCount == 0) {
            card.setPreferred(true);
        } else if (wantsPreferred) {
            clearPreferredForCustomer(customerId);
            card.setPreferred(true);
        } else {
            boolean hasPreferred = creditCardRepository.existsByCustomer_IdAndPreferredTrueAndActiveTrue(customerId);
            card.setPreferred(!hasPreferred);
        }

        return CreditCardMapper.toResponse(creditCardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> listActiveCards(UUID customerId) {
        assertCustomerExists(customerId);
        return creditCardRepository.findByCustomer_IdAndActiveTrueOrderByPreferredDesc(customerId).stream()
                .map(CreditCardMapper::toResponse)
                .toList();
    }

    @Transactional
    public CreditCardResponse updateCard(UUID customerId, UUID cardId, CreditCardUpdateRequest request) {
        CreditCard card = creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));

        boolean active = Boolean.TRUE.equals(request.getActive());

        card.setCardholderName(request.getCardholderName().trim());
        card.setCardNumber(request.getCardNumber().replaceAll("\\s", ""));
        card.setBrand(request.getBrand().trim());
        card.setExpirationMonth(request.getExpirationMonth());
        card.setExpirationYear(request.getExpirationYear());
        card.setActive(active);

        if (!active) {
            card.setPreferred(false);
        } else if (Boolean.TRUE.equals(request.getPreferred())) {
            clearPreferredForCustomer(customerId);
            card.setPreferred(true);
        } else {
            card.setPreferred(false);
            ensurePreferredExists(customerId);
        }

        creditCardRepository.save(card);
        if (!active) {
            ensurePreferredExists(customerId);
        }
        return CreditCardMapper.toResponse(creditCardRepository.findById(cardId).orElseThrow());
    }

    @Transactional
    public CreditCardResponse setPreferred(UUID customerId, UUID cardId) {
        CreditCard card = creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
        if (!card.isActive()) {
            throw new IllegalArgumentException("Cartão inativo não pode ser preferencial");
        }
        clearPreferredForCustomer(customerId);
        card.setPreferred(true);
        return CreditCardMapper.toResponse(creditCardRepository.save(card));
    }

    @Transactional
    public CreditCardResponse deactivateCard(UUID customerId, UUID cardId) {
        CreditCard card = creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
        card.setActive(false);
        card.setPreferred(false);
        creditCardRepository.save(card);
        ensurePreferredExists(customerId);
        return CreditCardMapper.toResponse(
                creditCardRepository.findByIdAndCustomer_Id(cardId, customerId).orElseThrow());
    }

    private void clearPreferredForCustomer(UUID customerId) {
        List<CreditCard> preferred = creditCardRepository.findByCustomer_IdAndPreferredTrueAndActiveTrue(customerId);
        for (CreditCard c : preferred) {
            c.setPreferred(false);
        }
        creditCardRepository.saveAll(preferred);
    }

    private void ensurePreferredExists(UUID customerId) {
        if (creditCardRepository.existsByCustomer_IdAndPreferredTrueAndActiveTrue(customerId)) {
            return;
        }
        List<CreditCard> active = creditCardRepository.findByCustomer_IdAndActiveTrueOrderByPreferredDesc(customerId);
        if (!active.isEmpty()) {
            CreditCard pick = active.get(0);
            pick.setPreferred(true);
            creditCardRepository.save(pick);
        }
    }

    private void assertCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }
    }
}
