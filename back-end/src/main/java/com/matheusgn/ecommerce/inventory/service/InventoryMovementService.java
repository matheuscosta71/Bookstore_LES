package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.inventory.dto.InventoryMovementResponse;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovement;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {

    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> list(
            UUID bookId,
            InventoryMovementType movementType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        Instant from = startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to = endDate != null ? endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Specification<InventoryMovement> spec =
                InventoryMovementSpecifications.withFilters(bookId, movementType, from, to);
        return inventoryMovementRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private InventoryMovementResponse toResponse(InventoryMovement m) {
        return InventoryMovementResponse.builder()
                .id(m.getId())
                .bookId(m.getBook().getId())
                .bookTitle(m.getBook().getTitle())
                .movementType(m.getMovementType())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .quantity(m.getQuantity())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
