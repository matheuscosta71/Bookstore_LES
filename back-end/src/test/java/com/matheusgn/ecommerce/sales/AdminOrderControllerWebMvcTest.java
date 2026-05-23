package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrderService adminOrderService;

    @Test
    @DisplayName("GET /admin/orders with X-Admin-Key returns page of orders")
    void list_ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(adminOrderService.listOrders(
                        any(Pageable.class),
                        eq("secret-key"),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()))
                .thenReturn(
                new PageImpl<>(
                        List.of(OrderResponse.builder()
                                .id(orderId)
                                .status(OrderStatus.EM_PROCESSAMENTO)
                                .build()),
                        PageRequest.of(0, 20),
                        1));

        mockMvc.perform(get("/admin/orders").header("X-Admin-Key", "secret-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PATCH /admin/orders/{id}/dispatch with X-Admin-Key returns 200")
    void dispatch_ok() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(adminOrderService.dispatch(eq(orderId), eq("secret-key"))).thenReturn(
                OrderResponse.builder()
                        .id(orderId)
                        .status(OrderStatus.EM_TRANSITO)
                        .build());

        mockMvc.perform(patch("/admin/orders/{orderId}/dispatch", orderId)
                        .header("X-Admin-Key", "secret-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_TRANSITO"));
    }
}
