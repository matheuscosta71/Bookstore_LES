package com.matheusgn.ecommerce.inventory;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.service.PricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RN0013 (preço por grupo de precificação): lógica em {@link com.matheusgn.ecommerce.inventory.service.PricingService},
 * não em {@code BookService}.
 */
@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private PricingService pricingService;

    private final UUID bookId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("recalculateSalePrice")
    class Recalculate {

        @Test
        @DisplayName("givenBookNotFound_whenRecalculate_thenResourceNotFound")
        void givenBookNotFound_whenRecalculate_thenResourceNotFound() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pricingService.recalculateSalePrice(bookId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("givenCostNull_whenRecalculate_thenIllegalArgument")
        void givenCostNull_whenRecalculate_thenIllegalArgument() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(null)
                    .pricingGroup(PricingGroup.builder().percentage(new BigDecimal("25")).build())
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> pricingService.recalculateSalePrice(bookId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Custo");
        }

        @Test
        @DisplayName("givenPricingGroupNull_whenRecalculate_thenIllegalArgument")
        void givenPricingGroupNull_whenRecalculate_thenIllegalArgument() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(new BigDecimal("100.00"))
                    .pricingGroup(null)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> pricingService.recalculateSalePrice(bookId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo de precificação");
        }

        @Test
        @DisplayName("givenGroupPercentageNull_whenRecalculate_thenIllegalArgument")
        void givenGroupPercentageNull_whenRecalculate_thenIllegalArgument() {
            PricingGroup g = PricingGroup.builder().name("G").percentage(null).build();
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(new BigDecimal("100.00"))
                    .pricingGroup(g)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> pricingService.recalculateSalePrice(bookId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Percentual");
        }

        @Test
        @DisplayName("givenCostAndGroup_whenRecalculate_thenPersistsSalePrice")
        void givenCostAndGroup_whenRecalculate_thenPersistsSalePrice() {
            PricingGroup g = PricingGroup.builder()
                    .name("Padrão")
                    .percentage(new BigDecimal("25"))
                    .build();
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(new BigDecimal("100.00"))
                    .pricingGroup(g)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = pricingService.recalculateSalePrice(bookId);

            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("125.00"));
            verify(bookRepository).save(any(Book.class));
        }
    }

    @Nested
    @DisplayName("applyAutomaticSalePriceIfEligible (RF0052)")
    class ApplyAutomatic {

        @Test
        @DisplayName("givenBookMissing_whenApply_thenNoOp")
        void givenBookMissing_whenApply_thenNoOp() {
            when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

            pricingService.applyAutomaticSalePriceIfEligible(bookId);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("givenCostNull_whenApply_thenNoOp")
        void givenCostNull_whenApply_thenNoOp() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(null)
                    .pricingGroup(PricingGroup.builder().percentage(new BigDecimal("25")).build())
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));

            pricingService.applyAutomaticSalePriceIfEligible(bookId);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("givenCostAndGroup_whenApply_thenPersistsSalePrice")
        void givenCostAndGroup_whenApply_thenPersistsSalePrice() {
            PricingGroup g = PricingGroup.builder()
                    .name("Padrão")
                    .percentage(new BigDecimal("25"))
                    .build();
            Book b = Book.builder()
                    .id(bookId)
                    .title("T")
                    .isbn("9780000000002")
                    .stockQuantity(1)
                    .active(true)
                    .salePrice(BigDecimal.TEN)
                    .costPrice(new BigDecimal("80.00"))
                    .pricingGroup(g)
                    .build();
            when(bookRepository.findById(bookId)).thenReturn(Optional.of(b));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            pricingService.applyAutomaticSalePriceIfEligible(bookId);

            verify(bookRepository).save(any(Book.class));
            assertThat(b.getSalePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }
}
