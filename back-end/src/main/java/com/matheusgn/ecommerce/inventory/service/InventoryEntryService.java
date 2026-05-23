package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.EntryReason;
import com.matheusgn.ecommerce.inventory.entity.InventoryEntry;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.inventory.repository.InventoryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryEntryService {

    private final BookRepository bookRepository;
    private final InventoryEntryRepository inventoryEntryRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final AuditLogService auditLogService;

    @Transactional
    public InventoryEntry registerManualEntry(UUID bookId, int quantity, BigDecimal unitCost, EntryReason reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Custo unitário inválido");
        }
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        InventoryEntry entry = InventoryEntry.builder()
                .book(book)
                .unitCost(unitCost)
                .quantity(quantity)
                .reason(reason != null ? reason : EntryReason.OTHER)
                .build();
        inventoryEntryRepository.save(entry);
        inventoryBalanceService.increaseStock(
                bookId,
                quantity,
                InventoryMovementType.ENTRY,
                InventoryReferenceType.MANUAL_ENTRY,
                entry.getId(),
                null);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("bookId", bookId);
        audit.put("quantity", quantity);
        audit.put("unitCost", unitCost);
        audit.put("reason", entry.getReason().name());
        auditLogService.logCreate("InventoryEntry", entry.getId(), audit);
        return entry;
    }
}
