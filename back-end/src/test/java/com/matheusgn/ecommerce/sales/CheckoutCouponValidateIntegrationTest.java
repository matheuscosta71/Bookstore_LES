package com.matheusgn.ecommerce.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.sales.entity.Coupon;
import com.matheusgn.ecommerce.sales.entity.CouponType;
import com.matheusgn.ecommerce.sales.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutCouponValidateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private static final String VALID_CUSTOMER_JSON = """
            {
              "fullName": "Cupom Teste",
              "email": "cupom.validate@test.com",
              "cpf": "52998224725",
              "phone": "31988887777",
              "birthDate": "1992-03-15",
              "password": "SenhaForte8!",
              "confirmPassword": "SenhaForte8!",
              "active": true
            }
            """;

    private String createCustomerAndGetId() throws Exception {
        var result = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CUSTOMER_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    @Test
    @DisplayName("POST coupon/validate retorna amount do cupom de troca")
    void validateExchangeCoupon_returnsAmount() throws Exception {
        String id = createCustomerAndGetId();
        UUID customerId = UUID.fromString(id);
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        couponRepository.save(Coupon.builder()
                .code("TROCA-INTTEST-01")
                .type(CouponType.EXCHANGE)
                .amount(new BigDecimal("25.50"))
                .active(true)
                .redeemed(false)
                .expirationDate(LocalDate.now().plusDays(30))
                .customer(customer)
                .build());

        mockMvc.perform(post("/customers/" + id + "/checkout/coupon/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "TROCA-INTTEST-01",
                                  "paymentType": "EXCHANGE_COUPON"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25.5));
    }

    @Test
    @DisplayName("cupom inexistente retorna 400")
    void unknownCoupon_returnsBadRequest() throws Exception {
        String id = createCustomerAndGetId();

        mockMvc.perform(post("/customers/" + id + "/checkout/coupon/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "NAO-EXISTE",
                                  "paymentType": "EXCHANGE_COUPON"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tipo de pagamento incompatível com cupom retorna 400")
    void wrongPaymentType_returnsBadRequest() throws Exception {
        String id = createCustomerAndGetId();
        UUID customerId = UUID.fromString(id);
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        couponRepository.save(Coupon.builder()
                .code("PROMO-INTTEST-01")
                .type(CouponType.PROMOTIONAL)
                .amount(new BigDecimal("10.00"))
                .active(true)
                .redeemed(false)
                .expirationDate(LocalDate.now().plusDays(30))
                .customer(customer)
                .build());

        mockMvc.perform(post("/customers/" + id + "/checkout/coupon/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PROMO-INTTEST-01",
                                  "paymentType": "EXCHANGE_COUPON"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
