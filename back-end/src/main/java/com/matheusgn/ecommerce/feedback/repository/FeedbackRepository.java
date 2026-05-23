package com.matheusgn.ecommerce.feedback.repository;

import com.matheusgn.ecommerce.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    @Query("""
            select f from Feedback f
            join fetch f.book
            where f.customer.id = :customerId
            order by f.createdAt desc
            """)
    List<Feedback> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);
}
