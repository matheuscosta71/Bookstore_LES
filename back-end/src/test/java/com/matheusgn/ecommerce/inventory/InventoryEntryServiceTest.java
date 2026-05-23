package com.matheusgn.ecommerce.inventory;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.EntryReason;
import com.matheusgn.ecommerce.inventory.entity.InventoryEntry;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryEntryRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.inventory.service.InventoryEntryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Entradas manuais de estoque (RN005x no domínio): {@link com.matheusgn.ecommerce.inventory.service.InventoryEntryService}.
 * Regra “maior custo define preço de venda” não está neste serviço.
 */
@ExtendWith(MockitoExtension.class)
class InventoryEntryServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private InventoryEntryRepository inventoryEntryRepository;
    @Mock
    private InventoryBalanceService inventoryBalanceService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InventoryEntryService inventoryEntryService;

    private final UUID bookId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Nested
    @DisplayName("registerManualEntry")
    class Register {

        @Test
        @DisplayName("givenNonPositiveQuantity_whenRegister_thenIllegalArgument")
        void givenNonPositiveQuantity_whenRegister_thenIllegalArgument() {
            assertThatThrownBy(() -> inventoryEntryService.registerManualEntry(bookId, 0, BigDecimal.ONE, EntryReason.OTHER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positiva");
        }

        @Test
        @DisplayName("givenNullUnitCost_whenRegister_thenIllegalArgument")
        void givenNullUnitCost_whenRegister_thenIllegalArgument() {
            assertThatThrownBy(() -> inventoryEntryService.registerManualEntry(bookId, 1, null, EntryReason.OTHER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Custo unitário");
        }

        @Test
        @DisplayName("givenNegativeUnitCost_whenRegister_thenIllegalArgument")
        void givenNegativeUnitCost_whenRegister_thenIllegalArgument() {
            assertThatThrownBy(() -> inventoryEntryService.registerManualEntry(bookId, 2, new BigDecimal("-1"), EntryReason.OTHER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Custo unitário");
        }

        @Test
        @DisplayName("givenBookMissing_whenRegister_thenResourceNotFound")
        void givenBookMissing_whenRegister_thenResourceNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryEntryService.registerManualEntry(bookId, 5, new BigDecimal("10.50"), EntryReason.OTHER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("givenValidData_whenRegister_thenSavesEntryAndIncreasesStock")
        void givenValidData_whenRegister_thenSavesEntryAndIncreasesStock() {
            Book book = Book.builder()
                    .id(bookId)
                    .title("B")
                    .isbn("9780000000003")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            UUID entryId = UUID.randomUUID();
            when(inventoryEntryRepository.save(any(InventoryEntry.class))).thenAnswer(inv -> {
                InventoryEntry e = inv.getArgument(0);
                e.setId(entryId);
                return e;
            });

            InventoryEntry result = inventoryEntryService.registerManualEntry(bookId, 4, new BigDecimal("12.00"), EntryReason.OTHER);

            assertThat(result.getQuantity()).isEqualTo(4);
            assertThat(result.getUnitCost()).isEqualByComparingTo(new BigDecimal("12.00"));
            verify(inventoryEntryRepository).save(any(InventoryEntry.class));

            ArgumentCaptor<UUID> refIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(inventoryBalanceService).increaseStock(
                    eq(bookId),
                    eq(4),
                    eq(InventoryMovementType.ENTRY),
                    eq(InventoryReferenceType.MANUAL_ENTRY),
                    refIdCaptor.capture(),
                    eq(null));
            assertThat(refIdCaptor.getValue()).isEqualTo(entryId);
            verify(auditLogService).logCreate(eq("InventoryEntry"), eq(entryId), any());
        }

        @Test
        @DisplayName("givenNullReason_whenRegister_thenUsesOther")
        void givenNullReason_whenRegister_thenUsesOther() {
            Book book = Book.builder()
                    .id(bookId)
                    .title("B")
                    .isbn("9780000000003")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
            when(inventoryEntryRepository.save(any(InventoryEntry.class))).thenAnswer(inv -> {
                InventoryEntry e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            inventoryEntryService.registerManualEntry(bookId, 1, BigDecimal.ONE, null);

            ArgumentCaptor<InventoryEntry> cap = ArgumentCaptor.forClass(InventoryEntry.class);
            verify(inventoryEntryRepository).save(cap.capture());
            assertThat(cap.getValue().getReason()).isEqualTo(EntryReason.OTHER);
        }
    }
}
