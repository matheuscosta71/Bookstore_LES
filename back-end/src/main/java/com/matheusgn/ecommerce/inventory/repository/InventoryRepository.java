package com.matheusgn.ecommerce.inventory.repository;

import com.matheusgn.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByBook_Id(UUID bookId);

    boolean existsByBook_Id(UUID bookId);

    void deleteByBook_Id(UUID bookId);

    List<Inventory> findByQuantityAvailable(int quantityAvailable);
}
