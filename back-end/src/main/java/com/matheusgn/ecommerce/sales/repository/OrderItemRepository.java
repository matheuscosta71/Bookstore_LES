package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    Optional<OrderItem> findByIdAndOrder_Id(UUID itemId, UUID orderId);

    @Query("""
            select coalesce(sum(oi.totalPrice), 0), coalesce(sum(oi.quantity), 0), count(distinct so.id)
            from OrderItem oi join oi.order so
            where so.createdAt >= :start and so.createdAt < :end
            """)
    Object[] aggregateTotals(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select b.id, b.title, coalesce(sum(oi.totalPrice), 0), coalesce(sum(oi.quantity), 0)
            from OrderItem oi join oi.book b join oi.order so
            where so.createdAt >= :start and so.createdAt < :end
            group by b.id, b.title
            order by b.title
            """)
    List<Object[]> aggregateByBook(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select b.category, coalesce(sum(oi.totalPrice), 0), coalesce(sum(oi.quantity), 0)
            from OrderItem oi join oi.book b join oi.order so
            where so.createdAt >= :start and so.createdAt < :end
            group by b.category
            order by b.category
            """)
    List<Object[]> aggregateByCategory(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = """
            select cast(cast(o.created_at as date) as varchar), coalesce(sum(oi.total_price), 0)
            from order_items oi
            join sales_orders o on o.id = oi.order_id
            where o.created_at >= ?1 and o.created_at < ?2
            group by cast(cast(o.created_at as date) as varchar)
            order by 1
            """, nativeQuery = true)
    List<Object[]> aggregateRevenueByDay(Instant start, Instant end);

    @Query(value = """
            select FORMATDATETIME(o.created_at, 'yyyy-MM'), b.category, cast(coalesce(sum(oi.quantity), 0) as integer)
            from order_items oi
            join sales_orders o on o.id = oi.order_id
            join books b on b.id = oi.book_id
            where o.created_at >= ?1 and o.created_at < ?2
            group by FORMATDATETIME(o.created_at, 'yyyy-MM'), b.category
            order by 1, 2
            """, nativeQuery = true)
    List<Object[]> aggregateVolumeByMonthAndCategory(Instant start, Instant end);

    @Query("""
            select oi from OrderItem oi
            join fetch oi.book
            join fetch oi.order o
            where o.customer.id = :customerId
            order by o.createdAt desc
            """)
    List<OrderItem> findRecentForCustomer(@Param("customerId") UUID customerId, Pageable pageable);
}
