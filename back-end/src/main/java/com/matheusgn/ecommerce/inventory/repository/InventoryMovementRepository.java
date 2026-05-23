package com.matheusgn.ecommerce.inventory.repository;

import com.matheusgn.ecommerce.inventory.entity.InventoryMovement;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID>,
        JpaSpecificationExecutor<InventoryMovement> {

    boolean existsByReferenceTypeAndReferenceId(InventoryReferenceType referenceType, UUID referenceId);
}
