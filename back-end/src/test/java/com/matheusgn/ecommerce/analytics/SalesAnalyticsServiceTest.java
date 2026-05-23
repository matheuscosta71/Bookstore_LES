package com.matheusgn.ecommerce.analytics;

import com.matheusgn.ecommerce.analytics.service.SalesAnalyticsService;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private SalesAnalyticsService salesAnalyticsService;

    @Test
    void summary_mapsTotals() {
        Object[] row = new Object[] { new BigDecimal("150.00"), 12L, 3L };
        when(orderItemRepository.aggregateTotals(any(Instant.class), any(Instant.class))).thenReturn(row);

        var res = salesAnalyticsService.salesHistorySummary(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(res.getTotalRevenue()).isEqualByComparingTo("150.00");
        assertThat(res.getTotalItemsSold()).isEqualTo(12L);
        assertThat(res.getOrderCount()).isEqualTo(3L);
    }

    @Test
    void invertedRange_throws() {
        assertThatThrownBy(() -> salesAnalyticsService.salesHistorySummary(
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void byBooks_mapsRows() {
        UUID id = UUID.randomUUID();
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { id, "Livro A", new BigDecimal("40.00"), 2L });
        when(orderItemRepository.aggregateByBook(any(Instant.class), any(Instant.class)))
                .thenReturn(rows);

        var res = salesAnalyticsService.salesHistoryByBooks(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(res.getBooks()).hasSize(1);
        assertThat(res.getBooks().get(0).getTitle()).isEqualTo("Livro A");
        assertThat(res.getBooks().get(0).getQuantitySold()).isEqualTo(2L);
    }

    @Test
    void givenRepositoryReturnsCategoryRows_whenSalesHistoryByCategories_thenMapsResponse() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { "Ficção", new BigDecimal("30.00"), 4L });
        when(orderItemRepository.aggregateByCategory(any(Instant.class), any(Instant.class)))
                .thenReturn(rows);

        var res = salesAnalyticsService.salesHistoryByCategories(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(res.getCategories()).hasSize(1);
        assertThat(res.getCategories().get(0).getCategory()).isEqualTo("Ficção");
        assertThat(res.getCategories().get(0).getQuantitySold()).isEqualTo(4L);
        assertThat(res.getCategories().get(0).getRevenue()).isEqualByComparingTo("30.00");
    }

    @Test
    void givenNoSalesInPeriod_whenSummary_thenReturnsZeros() {
        Object[] row = new Object[] { null, null, null };
        when(orderItemRepository.aggregateTotals(any(Instant.class), any(Instant.class))).thenReturn(row);

        var res = salesAnalyticsService.salesHistorySummary(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(res.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(res.getTotalItemsSold()).isEqualTo(0L);
        assertThat(res.getOrderCount()).isEqualTo(0L);
    }

    @Test
    void givenEmptyBookRows_whenByBooks_thenReturnsEmptyList() {
        when(orderItemRepository.aggregateByBook(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        var res = salesAnalyticsService.salesHistoryByBooks(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31));

        assertThat(res.getBooks()).isEmpty();
    }
}
