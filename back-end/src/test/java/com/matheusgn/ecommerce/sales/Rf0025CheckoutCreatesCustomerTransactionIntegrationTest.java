package com.matheusgn.ecommerce.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.book.BookIntegrationTestHelper;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Rf0025CheckoutCreatesCustomerTransactionIntegrationTest {

    private static final String ADMIN_KEY = "8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PricingGroupRepository pricingGroupRepository;

    private BookIntegrationTestHelper bookHelper;

    @BeforeEach
    void setUp() {
        bookHelper = new BookIntegrationTestHelper(
                authorRepository, publisherRepository, supplierRepository, categoryRepository, pricingGroupRepository);
    }

    @Test
    @DisplayName("RF0025 / RN0028 — após aprovar pagamento admin, GET /transactions lista PURCHASE")
    void finalizePurchase_createsCustomerTransaction() throws Exception {
        String customerId = createCustomer();
        String bookId = createBook();
        String addressId = createAddress(customerId);
        createBillingAddress(customerId);
        String cardId = createCard(customerId);

        mockMvc.perform(post("/customers/{customerId}/cart/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": \"" + bookId + "\", \"quantity\": 1}"))
                .andExpect(status().isCreated());

        MvcResult freight = mockMvc.perform(post("/customers/{customerId}/checkout/freight", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\": \"" + addressId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode freightJson = objectMapper.readTree(freight.getResponse().getContentAsString());
        BigDecimal grandTotal = freightJson.get("grandTotal").decimalValue();

        mockMvc.perform(post("/customers/{customerId}/checkout/address", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId": "%s", "newAddress": null, "saveToProfile": null}
                                """.formatted(addressId)))
                .andExpect(status().isNoContent());

        String paymentBody = """
                {"lines": [{"paymentType": "CREDIT_CARD", "amount": %s, "creditCardId": "%s"}],
                "newCreditCard": null, "saveNewCardToProfile": null}
                """.formatted(grandTotal.toPlainString(), cardId);
        mockMvc.perform(post("/customers/{customerId}/checkout/payment", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isNoContent());

        MvcResult fin = mockMvc.perform(post("/customers/{customerId}/checkout/finalize", customerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(grandTotal.doubleValue()))
                .andReturn();
        String orderId = objectMapper.readTree(fin.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        patch("/admin/orders/{orderId}/approve-payment", orderId).header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/customers/{customerId}/transactions", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("PURCHASE"))
                .andExpect(jsonPath("$[0].amount").value(grandTotal.doubleValue()));
    }

    private String createCustomer() throws Exception {
        String uniq = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String body = """
                {
                  "fullName": "Tx Cliente",
                  "email": "tx.%s@test.com",
                  "cpf": "52998224725",
                  "phone": "31988887777",
                  "birthDate": "1992-03-15",
                  "password": "SenhaForte8!",
                  "confirmPassword": "SenhaForte8!",
                  "active": true
                }
                """.formatted(uniq);
        MvcResult r = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String createBook() throws Exception {
        String isbn = "978" + String.format("%010d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000L);
        String body = bookHelper.createBookJson("Livro RF0025", isbn, new BigDecimal("50.00"), 10);
        MvcResult r = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String createAddress(String customerId) throws Exception {
        String body = """
                {
                  "nickname": "Casa",
                  "street": "Rua A",
                  "number": "100",
                  "neighborhood": "Centro",
                  "city": "São Paulo",
                  "state": "SP",
                  "zipCode": "01310100",
                  "type": "DELIVERY"
                }
                """;
        MvcResult r = mockMvc.perform(post("/customers/" + customerId + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private void createBillingAddress(String customerId) throws Exception {
        String body = """
                {
                  "nickname": "Cobrança",
                  "street": "Rua B",
                  "number": "200",
                  "neighborhood": "Centro",
                  "city": "São Paulo",
                  "state": "SP",
                  "zipCode": "01310100",
                  "type": "BILLING"
                }
                """;
        mockMvc.perform(post("/customers/" + customerId + "/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String createCard(String customerId) throws Exception {
        String body = """
                {
                  "cardholderName": "Tx",
                  "cardNumber": "4111111111111111",
                  "brand": "VISA",
                  "expirationMonth": 12,
                  "expirationYear": 2030,
                  "preferred": true
                }
                """;
        MvcResult r = mockMvc.perform(post("/customers/" + customerId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }
}
