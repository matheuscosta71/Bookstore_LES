package com.matheusgn.ecommerce.inventory.repository;

import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingGroupRepository extends JpaRepository<PricingGroup, UUID> {

    Optional<PricingGroup> findByName(String name);
}
