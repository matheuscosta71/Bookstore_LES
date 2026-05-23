package com.matheusgn.ecommerce.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.book.BookIntegrationTestHelper;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryAnalyticsIntegrationTest {

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
    void givenValidAdminKey_whenGetSalesHistory_thenReturns200() throws Exception {
        mockMvc.perform(get("/analytics/sales-history")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.totalItemsSold").exists())
                .andExpect(jsonPath("$.orderCount").exists());
    }

    @Test
    void givenValidAdminKey_whenGetSalesHistoryByBooks_thenReturns200() throws Exception {
        mockMvc.perform(get("/analytics/sales-history/books")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").isArray());
    }

    @Test
    void givenValidAdminKey_whenGetSalesHistoryByCategories_thenReturns200() throws Exception {
        mockMvc.perform(get("/analytics/sales-history/categories")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void givenBookCreated_whenPostInventoryEntry_thenReturns204() throws Exception {
        String isbn = "978" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String createBody = bookHelper.createBookJson("Integration Stock", isbn, new BigDecimal("50.00"), 0);

        MvcResult created = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        String bookId = node.get("id").asText();

        String entryBody = """
                {
                  "bookId": "%s",
                  "quantity": 3,
                  "unitCost": 25.50,
                  "reason": "PURCHASE"
                }
                """.formatted(bookId);

        mockMvc.perform(post("/inventory/entries")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody))
                .andExpect(status().isNoContent());
    }
}
