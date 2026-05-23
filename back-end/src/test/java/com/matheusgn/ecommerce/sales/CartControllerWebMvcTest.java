package com.matheusgn.ecommerce.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.sales.dto.CartResponse;
import com.matheusgn.ecommerce.sales.dto.CartUpsertItemRequest;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.service.CartService;
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
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @Test
    @DisplayName("POST /customers/{id}/cart/items returns 201")
    void postItem_created() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(cartService.addItem(eq(customerId), any(CartUpsertItemRequest.class))).thenReturn(
                CartResponse.builder()
                        .id(UUID.randomUUID())
                        .status(CartStatus.OPEN)
                        .totalAmount(new BigDecimal("20.00"))
                        .items(Collections.emptyList())
                        .build());

        mockMvc.perform(post("/customers/{customerId}/cart/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CartUpsertItemRequest.builder().bookId(bookId).quantity(2).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /customers/{id}/cart returns 200")
    void getCart_ok() throws Exception {
        UUID customerId = UUID.randomUUID();
        when(cartService.getCart(customerId)).thenReturn(
                CartResponse.builder()
                        .id(UUID.randomUUID())
                        .status(CartStatus.OPEN)
                        .totalAmount(BigDecimal.ZERO)
                        .items(Collections.emptyList())
                        .build());

        mockMvc.perform(get("/customers/{customerId}/cart", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }
}
