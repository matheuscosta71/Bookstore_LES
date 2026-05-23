package com.matheusgn.ecommerce.inventory.repository;

import com.matheusgn.ecommerce.inventory.entity.InventoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryEntryRepository extends JpaRepository<InventoryEntry, UUID> {
}
