package com.matheusgn.ecommerce.analytics.service;

import com.matheusgn.ecommerce.analytics.dto.BookSalesHistoryRow;
import com.matheusgn.ecommerce.analytics.dto.CategorySalesHistoryRow;
import com.matheusgn.ecommerce.analytics.dto.SalesHistoryBooksResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesHistoryCategoriesResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesHistorySummaryResponse;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public SalesHistorySummaryResponse salesHistorySummary(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Object[] row = unwrapAggregateRow(orderItemRepository.aggregateTotals(start, end));
        BigDecimal revenue = row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
        long qty = toLong(row[1]);
        long orders = toLong(row[2]);
        return SalesHistorySummaryResponse.builder()
                .totalRevenue(revenue)
                .totalItemsSold(qty)
                .orderCount(orders)
                .build();
    }

    @Transactional(readOnly = true)
    public SalesHistoryBooksResponse salesHistoryByBooks(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Object[]> rows = orderItemRepository.aggregateByBook(start, end);
        List<BookSalesHistoryRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(BookSalesHistoryRow.builder()
                    .bookId((UUID) r[0])
                    .title((String) r[1])
                    .revenue(r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO)
                    .quantitySold(toLong(r[3]))
                    .build());
        }
        return SalesHistoryBooksResponse.builder().books(list).build();
    }

    @Transactional(readOnly = true)
    public SalesHistoryCategoriesResponse salesHistoryByCategories(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Object[]> rows = orderItemRepository.aggregateByCategory(start, end);
        List<CategorySalesHistoryRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(CategorySalesHistoryRow.builder()
                    .category(r[0] != null ? (String) r[0] : null)
                    .revenue(r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO)
                    .quantitySold(toLong(r[2]))
                    .build());
        }
        return SalesHistoryCategoriesResponse.builder().categories(list).build();
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    /**
     * Spring Data / Hibernate pode devolver uma única linha como {@code Object[]} ou
     * como {@code Object[]} com um único elemento que é a linha {@code Object[]}.
     */
    private static Object[] unwrapAggregateRow(Object result) {
        if (!(result instanceof Object[] arr)) {
            throw new IllegalArgumentException("Resultado de agregação inesperado");
        }
        if (arr.length == 1 && arr[0] instanceof Object[] nested) {
            return nested;
        }
        return arr;
    }
}
