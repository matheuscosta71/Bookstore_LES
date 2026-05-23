package com.matheusgn.ecommerce.customer.repository;

import com.matheusgn.ecommerce.customer.entity.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, UUID> {

    List<CustomerTransaction> findByCustomer_IdOrderByTransactionDateDesc(UUID customerId);
}
