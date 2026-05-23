package com.matheusgn.ecommerce.analytics.controller;

import com.matheusgn.ecommerce.analytics.dto.SalesHistoryBooksResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesHistoryCategoriesResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesHistorySummaryResponse;
import com.matheusgn.ecommerce.analytics.dto.SalesLineChartResponse;
import com.matheusgn.ecommerce.analytics.service.SalesAnalyticsService;
import com.matheusgn.ecommerce.analytics.service.SalesLineChartService;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin — análise de vendas (RF0055)")
public class AnalyticsController {

    private final AdminOrderService adminOrderService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final SalesLineChartService salesLineChartService;

    @GetMapping("/sales-history")
    @Operation(summary = "RF0055 — Resumo de vendas no período (data início e fim)")
    public ResponseEntity<SalesHistorySummaryResponse> salesHistory(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(salesAnalyticsService.salesHistorySummary(startDate, endDate));
    }

    @GetMapping("/sales-history/books")
    @Operation(summary = "RF0055 — Histórico comparativo por produto (livro)")
    public ResponseEntity<SalesHistoryBooksResponse> salesHistoryBooks(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(salesAnalyticsService.salesHistoryByBooks(startDate, endDate));
    }

    @GetMapping("/sales-history/categories")
    @Operation(summary = "RF0055 — Histórico comparativo por categoria")
    public ResponseEntity<SalesHistoryCategoriesResponse> salesHistoryCategories(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(salesAnalyticsService.salesHistoryByCategories(startDate, endDate));
    }

    @GetMapping("/sales-history/line-chart")
    @Operation(summary = "RF0055 — Receita por dia no período (gráfico de linhas)")
    public ResponseEntity<SalesLineChartResponse> salesLineChart(
            @RequestHeader("X-Admin-Key") String adminKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(salesLineChartService.lineChart(startDate, endDate));
    }
}
