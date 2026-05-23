package com.matheusgn.ecommerce.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RNF0043 e resumo de vendas admin: endpoints sob /analytics com X-Admin-Key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsControllerIntegrationTest {

    private static final String ADMIN_KEY = "8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /analytics/sales-history/line-chart")
    class LineChart {

        @Test
        @DisplayName("deve retornar labels e values alinhados ao período")
        void shouldReturnLabelsAndValuesForRange() throws Exception {
            mockMvc.perform(get("/analytics/sales-history/line-chart")
                            .header("X-Admin-Key", ADMIN_KEY)
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-03"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labels").isArray())
                    .andExpect(jsonPath("$.values").isArray())
                    .andExpect(jsonPath("$.labels.length()").value(3));
        }
    }

    @Nested
    @DisplayName("GET /analytics/sales-history")
    class SalesHistorySummary {

        @Test
        @DisplayName("deve retornar 200 com estrutura de resumo no período")
        void shouldReturnSummaryForPeriod() throws Exception {
            mockMvc.perform(get("/analytics/sales-history")
                            .header("X-Admin-Key", ADMIN_KEY)
                            .param("startDate", "2026-01-01")
                            .param("endDate", "2026-01-31"))
                    .andExpect(status().isOk());
        }
    }
}
