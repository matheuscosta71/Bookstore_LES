package com.matheusgn.ecommerce.analytics;

import com.matheusgn.ecommerce.analytics.service.SalesLineChartService;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesLineChartServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SalesLineChartService salesLineChartService;

    @Test
    void emptyRange_returnsEmptyLabelsAndValues() {
        when(orderItemRepository.aggregateRevenueByDay(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        var res = salesLineChartService.lineChart(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 3));

        assertThat(res.getLabels()).containsExactly("2026-01-01", "2026-01-02", "2026-01-03");
        assertThat(res.getValues()).allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void givenRevenueOnOneDay_whenLineChart_thenLabelHasValue() {
        List<Object[]> rows = Collections.singletonList(new Object[]{"2026-01-02", new BigDecimal("150.50")});
        when(orderItemRepository.aggregateRevenueByDay(any(Instant.class), any(Instant.class)))
                .thenReturn(rows);

        var res = salesLineChartService.lineChart(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 3));

        assertThat(res.getLabels()).hasSize(3);
        int idx = res.getLabels().indexOf("2026-01-02");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        assertThat(res.getValues().get(idx)).isEqualByComparingTo(new BigDecimal("150.50"));
    }

    @Test
    void categoryVolumeChart_mapsMonthlyVolumesAndTreatsEmptyCategories() {
        Category cat1 = Category.builder().name("Ficção").build();
        Category cat2 = Category.builder().name("Software").build();
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        List<Object[]> rows = List.of(
                new Object[]{"2026-01", "Ficção", 10},
                new Object[]{"2026-02", "Ficção", 15},
                new Object[]{"2026-02", "Software", 5}
        );
        when(orderItemRepository.aggregateVolumeByMonthAndCategory(any(Instant.class), any(Instant.class)))
                .thenReturn(rows);

        var res = salesLineChartService.categoryVolumeChart(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 1)
        );

        assertThat(res.getLabels()).containsExactly("2026-01", "2026-02", "2026-03");
        assertThat(res.getSeries()).hasSize(2);

        var ficcaoSeries = res.getSeries().stream()
                .filter(s -> s.getCategory().equals("Ficção"))
                .findFirst().orElseThrow();
        assertThat(ficcaoSeries.getVolumes()).containsExactly(10, 15, 0);

        var softwareSeries = res.getSeries().stream()
                .filter(s -> s.getCategory().equals("Software"))
                .findFirst().orElseThrow();
        assertThat(softwareSeries.getVolumes()).containsExactly(0, 5, 0);
    }
}
