package com.matheusgn.ecommerce.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.entity.CustomerTransaction;
import com.matheusgn.ecommerce.customer.entity.TransactionType;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTransactionRepository transactionRepository;

    private static final String VALID_CUSTOMER_JSON = """
            {
              "fullName": "Ana Teste",
              "email": "ana.integration@test.com",
              "cpf": "52998224725",
              "phone": "31988887777",
              "birthDate": "1992-03-15",
              "password": "SenhaForte8!",
              "confirmPassword": "SenhaForte8!",
              "active": true
            }
            """;

    private String createCustomerAndGetId() throws Exception {
        MvcResult result = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CUSTOMER_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    @Nested
    @DisplayName("RF0021 / validação")
    class Rf0021 {

        @Test
        @DisplayName("POST cliente válido retorna 201")
        void postCreated() throws Exception {
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CUSTOMER_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("ana.integration@test.com"))
                    .andExpect(jsonPath("$.cpf").value("52998224725"));
        }

        @Test
        @DisplayName("e-mail duplicado retorna 409")
        void duplicateEmail() throws Exception {
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CUSTOMER_JSON))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CUSTOMER_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("e-mail inválido retorna 400")
        void invalidEmail() throws Exception {
            String body = """
                    {
                      "fullName": "X",
                      "email": "nao-e-email",
                      "cpf": "52998224725",
                      "phone": "1",
                      "birthDate": "1990-01-01",
                      "password": "SenhaForte8!",
                      "confirmPassword": "SenhaForte8!"
                    }
                    """;
            mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("RF0022 / RF0023 / RF0028")
    class Rf0022Rf0023Rf0028 {

        @Test
        @DisplayName("PUT atualiza cliente")
        void putUpdate() throws Exception {
            String id = createCustomerAndGetId();
            String body = """
                    {
                      "fullName": "Ana Atualizada",
                      "email": "ana.integration@test.com",
                      "cpf": "52998224725",
                      "phone": "31900001111",
                      "birthDate": "1992-03-15",
                      "active": true
                    }
                    """;
            mockMvc.perform(put("/customers/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Ana Atualizada"));
        }

        @Test
        @DisplayName("PATCH inactive inativa cliente")
        void patchInactive() throws Exception {
            String id = createCustomerAndGetId();
            mockMvc.perform(patch("/customers/" + id + "/inactive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("PATCH password altera só senha (204)")
        void patchPassword() throws Exception {
            String id = createCustomerAndGetId();
            mockMvc.perform(patch("/customers/" + id + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"OutraSenha9!\"}"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("RF0024 — Filtros")
    class Rf0024 {

        @Test
        @DisplayName("GET com filtro combinado")
        void combinedFilters() throws Exception {
            createCustomerAndGetId();
            mockMvc.perform(get("/customers")
                            .param("fullName", "Ana")
                            .param("email", "ana.integration@test.com")
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("GET sem resultados retorna lista vazia")
        void emptyList() throws Exception {
            mockMvc.perform(get("/customers").param("fullName", "NomeInexistente999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("RF0025 — Transações")
    class Rf0025 {

        @Test
        @DisplayName("GET transações do cliente")
        void listTransactions() throws Exception {
            String idStr = createCustomerAndGetId();
            UUID id = UUID.fromString(idStr);
            Customer c = customerRepository.findById(id).orElseThrow();

            transactionRepository.save(CustomerTransaction.builder()
                    .customer(c)
                    .description("Compra livro")
                    .amount(new BigDecimal("59.90"))
                    .transactionDate(Instant.parse("2026-03-22T15:00:00Z"))
                    .type(TransactionType.PURCHASE)
                    .build());

            mockMvc.perform(get("/customers/" + id + "/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].description").value("Compra livro"));
        }
    }

    @Nested
    @DisplayName("RF0026 — Endereços")
    class Rf0026 {

        @Test
        @DisplayName("POST e GET endereços")
        void addresses() throws Exception {
            String id = createCustomerAndGetId();
            String addr = """
                    {
                      "nickname": "Casa",
                      "street": "Rua A",
                      "number": "100",
                      "complement": "ap 2",
                      "neighborhood": "Centro",
                      "city": "BH",
                      "state": "MG",
                      "zipCode": "30130000",
                      "type": "DELIVERY"
                    }
                    """;
            mockMvc.perform(post("/customers/" + id + "/addresses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addr))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nickname").value("Casa"));

            mockMvc.perform(get("/customers/" + id + "/addresses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("RF0027 — Cartões")
    class Rf0027 {

        @Test
        @DisplayName("dois cartões e PATCH preferred")
        void cardsPreferred() throws Exception {
            String id = createCustomerAndGetId();

            String card1 = """
                    {
                      "cardholderName": "Ana",
                      "cardNumber": "4111111111111111",
                      "brand": "VISA",
                      "expirationMonth": 12,
                      "expirationYear": 2030,
                      "preferred": false
                    }
                    """;
            MvcResult r1 = mockMvc.perform(post("/customers/" + id + "/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(card1))
                    .andExpect(status().isCreated())
                    .andReturn();
            String cardId1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asText();

            String card2 = """
                    {
                      "cardholderName": "Ana",
                      "cardNumber": "5555555555554444",
                      "brand": "MASTERCARD",
                      "expirationMonth": 11,
                      "expirationYear": 2031,
                      "preferred": false
                    }
                    """;
            MvcResult r2 = mockMvc.perform(post("/customers/" + id + "/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(card2))
                    .andExpect(status().isCreated())
                    .andReturn();
            String cardId2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asText();

            mockMvc.perform(patch("/customers/" + id + "/cards/" + cardId2 + "/preferred"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.preferred").value(true));

            mockMvc.perform(get("/customers/" + id + "/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            mockMvc.perform(patch("/customers/" + id + "/cards/" + cardId1 + "/inactive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }
    }

    @Nested
    @DisplayName("404")
    class NotFound {

        @Test
        @DisplayName("GET cliente inexistente")
        void customerNotFound() throws Exception {
            mockMvc.perform(get("/customers/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
