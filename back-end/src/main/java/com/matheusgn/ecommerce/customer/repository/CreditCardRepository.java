package com.matheusgn.ecommerce.customer.repository;

import com.matheusgn.ecommerce.customer.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    Optional<CreditCard> findByIdAndCustomer_Id(UUID id, UUID customerId);

    List<CreditCard> findByCustomer_IdAndActiveTrueOrderByPreferredDesc(UUID customerId);

    List<CreditCard> findByCustomer_IdAndPreferredTrueAndActiveTrue(UUID customerId);

    boolean existsByCustomer_IdAndPreferredTrueAndActiveTrue(UUID customerId);

    long countByCustomer_IdAndActiveTrue(UUID customerId);
}
