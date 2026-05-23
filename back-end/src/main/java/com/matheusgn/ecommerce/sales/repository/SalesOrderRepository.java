package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

    /** Sem "payments" no graph: dois {@code List} em fetch simultâneo causam MultipleBagFetchException. */
    @EntityGraph(attributePaths = {"customer", "deliveryAddress", "items", "items.book"})
    Page<SalesOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SalesOrder> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    Optional<SalesOrder> findByIdAndCustomer_Id(UUID orderId, UUID customerId);

    @Query("select distinct o from SalesOrder o left join fetch o.items i left join fetch i.book where o.id = :id")
    Optional<SalesOrder> findByIdWithItemsAndBooks(@Param("id") UUID id);
}
