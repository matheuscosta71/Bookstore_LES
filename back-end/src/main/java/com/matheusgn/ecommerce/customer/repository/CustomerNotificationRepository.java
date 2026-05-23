package com.matheusgn.ecommerce.customer.repository;

import com.matheusgn.ecommerce.customer.entity.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, UUID> {
}
