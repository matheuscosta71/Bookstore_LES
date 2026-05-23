package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.customer.dto.TransactionMapper;
import com.matheusgn.ecommerce.customer.dto.TransactionResponse;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerTransactionRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerTransactionService {

    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> listByCustomerId(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }
        return transactionRepository.findByCustomer_IdOrderByTransactionDateDesc(customerId).stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }
}
