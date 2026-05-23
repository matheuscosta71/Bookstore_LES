package com.matheusgn.ecommerce.inventory.repository;

import com.matheusgn.ecommerce.inventory.entity.InventoryMovement;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InventoryMovementSpecifications {

    private InventoryMovementSpecifications() {}

    public static Specification<InventoryMovement> withFilters(
            UUID bookId,
            InventoryMovementType movementType,
            Instant fromInclusive,
            Instant toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (bookId != null) {
                Join<?, ?> bookJoin = root.join("book", JoinType.INNER);
                predicates.add(cb.equal(bookJoin.get("id"), bookId));
            }
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }
            if (fromInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
