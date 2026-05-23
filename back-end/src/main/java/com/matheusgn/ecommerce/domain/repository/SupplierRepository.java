package com.matheusgn.ecommerce.domain.repository;

import com.matheusgn.ecommerce.domain.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByNameIgnoreCase(String name);
}
