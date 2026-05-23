package com.matheusgn.ecommerce.analytics.service;

import com.matheusgn.ecommerce.analytics.dto.SalesLineChartResponse;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalesLineChartService {

    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public SalesLineChartResponse lineChart(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = orderItemRepository.aggregateRevenueByDay(start, end);
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            LocalDate d = LocalDate.parse(row[0].toString());
            BigDecimal val = toBigDecimal(row[1]);
            byDay.put(d, val);
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            labels.add(d.toString());
            values.add(byDay.getOrDefault(d, BigDecimal.ZERO));
        }

        return SalesLineChartResponse.builder()
                .labels(labels)
                .values(values)
                .build();
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(v.toString());
    }
}
