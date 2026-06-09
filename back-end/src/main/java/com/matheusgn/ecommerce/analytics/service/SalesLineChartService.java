package com.matheusgn.ecommerce.analytics.service;

import com.matheusgn.ecommerce.analytics.dto.SalesCategoryVolumeResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesCategoryVolumeResponse.CategoryVolumeSeries;
import com.matheusgn.ecommerce.analytics.dto.SalesLineChartResponse;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalesLineChartService {

    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public SalesCategoryVolumeResponse categoryVolumeChart(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }

        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<String> labels = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
            labels.add(ym.toString());
        }

        List<String> categories = categoryRepository.findAll().stream()
                .map(Category::getName)
                .sorted()
                .toList();

        List<Object[]> rows = orderItemRepository.aggregateVolumeByMonthAndCategory(start, end);

        Map<String, Map<String, Integer>> volumeMap = new HashMap<>();
        for (String month : labels) {
            volumeMap.put(month, new HashMap<>());
        }

        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) {
                continue;
            }
            String month = row[0].toString();
            String category = row[1].toString();
            int qty = ((Number) row[2]).intValue();

            if (volumeMap.containsKey(month)) {
                volumeMap.get(month).put(category, qty);
            }
        }

        List<CategoryVolumeSeries> series = new ArrayList<>();
        for (String category : categories) {
            List<Integer> volumes = new ArrayList<>();
            for (String month : labels) {
                int vol = volumeMap.get(month).getOrDefault(category, 0);
                volumes.add(vol);
            }
            series.add(CategoryVolumeSeries.builder()
                    .category(category)
                    .volumes(volumes)
                    .build());
        }

        return SalesCategoryVolumeResponse.builder()
                .labels(labels)
                .series(series)
                .build();
    }

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
