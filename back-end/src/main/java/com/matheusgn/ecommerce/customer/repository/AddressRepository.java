package com.matheusgn.ecommerce.customer.repository;

import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    Optional<Address> findByIdAndCustomer_Id(UUID id, UUID customerId);

    List<Address> findByCustomer_IdAndActiveTrueOrderByNicknameAsc(UUID customerId);

    long countByCustomer_IdAndTypeAndActiveTrue(UUID customerId, AddressType type);
}
