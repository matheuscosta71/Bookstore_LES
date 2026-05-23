package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByIdAndCart_Id(UUID itemId, UUID cartId);
}
