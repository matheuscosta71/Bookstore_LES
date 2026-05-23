package com.matheusgn.ecommerce.sales.repository;

import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.entity.SalesOrder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.matheusgn.ecommerce.customer.entity.Customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@UtilityClass
public class SalesOrderSpecifications {

    private static final ZoneId BR = ZoneId.of("America/Sao_Paulo");

    /**
     * Filtros para listagem admin (número/id do pedido, cliente, status, data e faixa de total).
     */
    public static Specification<SalesOrder> withAdminFilters(
            String orderNumberOrId,
            String customerName,
            OrderStatus status,
            LocalDate dateFrom,
            LocalDate dateTo,
            BigDecimal totalMin,
            BigDecimal totalMax) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(orderNumberOrId)) {
                String raw = orderNumberOrId.trim();
                String q = raw.startsWith("#") ? raw.substring(1).trim() : raw;
                if (StringUtils.hasText(q)) {
                    try {
                        UUID id = UUID.fromString(q);
                        predicates.add(cb.equal(root.get("id"), id));
                    } catch (IllegalArgumentException ignored) {
                        String lower = q.toLowerCase(Locale.ROOT);
                        var idStr = root.get("id").as(String.class);
                        Predicate likeUuidStr = cb.like(cb.lower(idStr), "%" + lower + "%");
                        var noDash = cb.function(
                                "replace", String.class, idStr, cb.literal("-"), cb.literal(""));
                        Predicate likeHex = cb.like(cb.lower(noDash), "%" + lower.replace("-", "") + "%");
                        predicates.add(cb.or(likeUuidStr, likeHex));
                    }
                }
            }

            if (StringUtils.hasText(customerName)) {
                Join<SalesOrder, Customer> cust = root.join("customer");
                String pattern = "%" + customerName.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(cust.get("fullName")), pattern));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (dateFrom != null) {
                Instant start = dateFrom.atStartOfDay(BR).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (dateTo != null) {
                Instant end = dateTo.atTime(LocalTime.MAX).atZone(BR).toInstant();
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }

            if (totalMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), totalMin));
            }
            if (totalMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), totalMax));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
