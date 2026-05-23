package com.matheusgn.ecommerce.inventory;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.Inventory;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovement;
import com.matheusgn.ecommerce.inventory.entity.InventoryMovementType;
import com.matheusgn.ecommerce.inventory.entity.InventoryReferenceType;
import com.matheusgn.ecommerce.inventory.repository.InventoryMovementRepository;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryBalanceServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @InjectMocks
    private InventoryBalanceService inventoryBalanceService;

    @Test
    void givenSufficientStock_whenDecrease_thenUpdatesQuantityAndSavesMovement() {
        UUID bookId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("Livro A")
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(10)
                .active(true)
                .build();
        Inventory inv = Inventory.builder()
                .book(book)
                .quantityAvailable(10)
                .quantityReserved(0)
                .build();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBook_Id(bookId)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> i.getArgument(0));

        inventoryBalanceService.decreaseStock(
                bookId,
                3,
                InventoryMovementType.SALE_OUTBOUND,
                InventoryReferenceType.ORDER,
                refId,
                null);

        assertThat(inv.getQuantityAvailable()).isEqualTo(7);
        assertThat(book.getStockQuantity()).isEqualTo(7);
        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(3);
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovementType.SALE_OUTBOUND);
    }

    @Test
    void givenInsufficientStock_whenDecrease_thenThrowsIllegalArgumentException() {
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("Livro B")
                .isbn("9780000000002")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(2)
                .active(true)
                .build();
        Inventory inv = Inventory.builder()
                .book(book)
                .quantityAvailable(2)
                .quantityReserved(0)
                .build();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBook_Id(bookId)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> inventoryBalanceService.decreaseStock(
                bookId,
                5,
                InventoryMovementType.SALE_OUTBOUND,
                InventoryReferenceType.ORDER,
                UUID.randomUUID(),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void givenPositiveQuantity_whenIncreaseWithExistingInventory_thenUpdatesQuantityAndSavesMovement() {
        UUID bookId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("Livro C")
                .isbn("9780000000003")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(5)
                .active(true)
                .build();
        Inventory inv = Inventory.builder()
                .book(book)
                .quantityAvailable(5)
                .quantityReserved(0)
                .build();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBook_Id(bookId)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> i.getArgument(0));

        inventoryBalanceService.increaseStock(
                bookId,
                3,
                InventoryMovementType.ENTRY,
                InventoryReferenceType.MANUAL_ENTRY,
                entryId,
                null);

        assertThat(inv.getQuantityAvailable()).isEqualTo(8);
        assertThat(book.getStockQuantity()).isEqualTo(8);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void givenNoInventory_whenIncrease_thenCreatesInventoryWithQuantityAndSavesMovement() {
        UUID bookId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        Book book = Book.builder()
                .id(bookId)
                .title("Livro D")
                .isbn("9780000000004")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(0)
                .active(true)
                .build();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBook_Id(bookId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> i.getArgument(0));

        inventoryBalanceService.increaseStock(
                bookId,
                4,
                InventoryMovementType.ENTRY,
                InventoryReferenceType.MANUAL_ENTRY,
                entryId,
                null);

        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        assertThat(invCaptor.getValue().getQuantityAvailable()).isEqualTo(4);
        assertThat(book.getStockQuantity()).isEqualTo(4);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void givenBookNotFound_whenIncrease_thenThrowsResourceNotFoundException() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryBalanceService.increaseStock(
                bookId,
                1,
                InventoryMovementType.ENTRY,
                InventoryReferenceType.MANUAL_ENTRY,
                UUID.randomUUID(),
                null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
