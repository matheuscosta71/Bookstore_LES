package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.CartPaymentLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartPaymentLineRepository extends JpaRepository<CartPaymentLine, UUID> {
}
