package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.sales.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link com.matheusgn.ecommerce.sales.service.InventoryService#assertAvailableStock}.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private InventoryBalanceService inventoryBalanceService;

    @InjectMocks
    private InventoryService inventoryService;

    private final UUID bookId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Book activeBook(int stockQty) {
        return Book.builder()
                .id(bookId)
                .title("Livro A")
                .isbn("9780000000001")
                .stockQuantity(stockQty)
                .salePrice(java.math.BigDecimal.TEN)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("assertAvailableStock")
    class AssertStock {

        @Test
        @DisplayName("givenUnknownBook_whenAssert_thenResourceNotFound")
        void givenUnknownBook_whenAssert_thenResourceNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.assertAvailableStock(bookId, 1))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Livro não encontrado");
        }

        @Test
        @DisplayName("givenInactiveBook_whenAssert_thenIllegalArgument")
        void givenInactiveBook_whenAssert_thenIllegalArgument() {
            Book b = activeBook(10);
            b.setActive(false);
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> inventoryService.assertAvailableStock(bookId, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inativo");
        }

        @Test
        @DisplayName("givenSellableSufficient_whenAssert_thenOk")
        void givenSellableSufficient_whenAssert_thenOk() {
            Book b = activeBook(5);
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));
            when(inventoryBalanceService.getSellableQuantity(bookId)).thenReturn(5);

            assertThatCode(() -> inventoryService.assertAvailableStock(bookId, 5)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("givenSellableInsufficient_whenAssert_thenThrows")
        void givenSellableInsufficient_whenAssert_thenThrows() {
            Book b = activeBook(10);
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));
            when(inventoryBalanceService.getSellableQuantity(bookId)).thenReturn(3);

            assertThatThrownBy(() -> inventoryService.assertAvailableStock(bookId, 4))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estoque insuficiente")
                    .hasMessageContaining("Livro A");
        }
    }
}
