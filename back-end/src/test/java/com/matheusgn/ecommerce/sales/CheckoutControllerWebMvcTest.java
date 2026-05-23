package com.matheusgn.ecommerce.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.sales.dto.FreightRequest;
import com.matheusgn.ecommerce.sales.dto.FreightResponse;
import com.matheusgn.ecommerce.sales.dto.OrderResponse;
import com.matheusgn.ecommerce.sales.entity.OrderStatus;
import com.matheusgn.ecommerce.sales.service.CheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CheckoutService checkoutService;

    @Test
    @DisplayName("POST /customers/{id}/checkout/finalize returns 201")
    void finalize_created() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(checkoutService.finalizePurchase(customerId)).thenReturn(
                OrderResponse.builder()
                        .id(UUID.randomUUID())
                        .status(OrderStatus.EM_PROCESSAMENTO)
                        .totalAmount(new BigDecimal("60.00"))
                        .build());

        mockMvc.perform(post("/customers/{customerId}/checkout/finalize", customerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    }

    @Test
    @DisplayName("POST /customers/{id}/checkout/freight returns totals")
    void freight_ok() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(checkoutService.calculateFreight(eq(customerId), any(FreightRequest.class))).thenReturn(
                FreightResponse.builder()
                        .freightAmount(new BigDecimal("10.00"))
                        .itemsSubtotal(new BigDecimal("50.00"))
                        .grandTotal(new BigDecimal("60.00"))
                        .build());

        mockMvc.perform(post("/customers/{customerId}/checkout/freight", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                FreightRequest.builder().addressId(addressId).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grandTotal").value(60.0));
    }
}
