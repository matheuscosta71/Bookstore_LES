package com.matheusgn.ecommerce.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import org.junit.jupiter.api.BeforeEach;
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
class BookControllerIntegrationTest {

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

    private String createBookAndGetId() throws Exception {
        String json = bookHelper.createBookJson("Clean Code", "9780132350884", new BigDecimal("89.90"), 10);
        MvcResult result = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    @Nested
    @DisplayName("RF0011 — Cadastrar livro (validação HTTP)")
    class Rf0011Validation {

        @Test
        @DisplayName("cadastro com dados válidos retorna 201")
        void create_valid_returns201() throws Exception {
            String json = bookHelper.createBookJson("Clean Code", "9780132350884", new BigDecimal("89.90"), 10);
            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Clean Code"))
                    .andExpect(jsonPath("$.isbn").value("9780132350884"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("não permite cadastro sem título")
        void create_withoutTitle_returns400() throws Exception {
            String body = """
                    {
                      "title": "",
                      "price": 10.00,
                      "isbn": "9780132350884",
                      "stockQuantity": 1
                    }
                    """;
            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("não permite preço negativo")
        void create_negativePrice_returns400() throws Exception {
            String body = bookHelper.createBookJson("Livro", "9780132350884", new BigDecimal("-1.00"), 1);
            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("não permite estoque negativo")
        void create_negativeStock_returns400() throws Exception {
            String body = bookHelper.createBookJson("Livro", "9780132350884", new BigDecimal("10.00"), -1);
            mockMvc.perform(post("/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("RF0012 e RF0016 — Ativar / inativar")
    class Rf0012Rf0016PatchActive {

        @Test
        @DisplayName("inativar livro existente")
        void patchInactive() throws Exception {
            String id = createBookAndGetId();
            mockMvc.perform(patch("/books/" + id + "/active")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    """
                                    {"active": false, "justification": "Baixa de giro", "reason": "BAIXA_ROTACAO"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @DisplayName("ativar livro previamente inativado")
        void patchActivate() throws Exception {
            String id = createBookAndGetId();
            mockMvc.perform(patch("/books/" + id + "/active")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    """
                                    {"active": false, "justification": "Baixa", "reason": "BAIXA_ROTACAO"}
                                    """))
                    .andExpect(status().isOk());
            mockMvc.perform(patch("/books/" + id + "/active")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    """
                                    {"active": true, "justification": "Retorno", "reason": "RETORNO_ESTOQUE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("erro 404 ao alterar ativo de livro inexistente")
        void patchActive_notFound() throws Exception {
            UUID random = UUID.randomUUID();
            mockMvc.perform(patch("/books/" + random + "/active")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    """
                                    {"active": false, "justification": "x", "reason": "BAIXA_ROTACAO"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("RF0013 — Inativação automática (integração)")
    class Rf0013AutoInactivate {

        @Test
        @DisplayName("POST inactivate-automatic inativa livro com estoque zero e venda abaixo do mínimo")
        void postAutomatic_inactivatesEligible() throws Exception {
            String id = createBookAndGetId();
            String updateBody = bookHelper.updateBookJson(
                    "Clean Code",
                    "9780132350884",
                    new BigDecimal("89.90"),
                    0,
                    true,
                    "Software",
                    new BigDecimal("30.00"));
            mockMvc.perform(put("/books/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/books/inactivate-automatic")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"minimumSalesValue\": 50.00}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/books/" + id).param("includeInactive", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.lastLifecycleReason").value("FORA_DE_MERCADO"));
        }
    }

    @Nested
    @DisplayName("RF0014 — Alterar cadastro")
    class Rf0014Update {

        @Test
        @DisplayName("alterar livro existente retorna 200")
        void update_existing_returns200() throws Exception {
            String id = createBookAndGetId();
            String body = bookHelper.updateBookJson(
                    "Novo título",
                    "9780132350884",
                    new BigDecimal("99.00"),
                    5,
                    true,
                    "Ficção",
                    null);
            mockMvc.perform(put("/books/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Novo título"))
                    .andExpect(jsonPath("$.category").value("Ficção"));
        }

        @Test
        @DisplayName("alterar livro inexistente retorna 404")
        void update_notFound_returns404() throws Exception {
            UUID random = UUID.randomUUID();
            String body = bookHelper.updateBookJson(
                    "X",
                    "9780000000000",
                    BigDecimal.ONE,
                    1,
                    true,
                    "Software",
                    null);
            mockMvc.perform(put("/books/" + random)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("RF0015 — Consulta com filtros")
    class Rf0015Filters {

        @Test
        @DisplayName("consulta por título isolado")
        void filterByTitle() throws Exception {
            createBookAndGetId();
            mockMvc.perform(get("/books").param("title", "Clean"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
        }

        @Test
        @DisplayName("consulta por ISBN isolado")
        void filterByIsbn() throws Exception {
            createBookAndGetId();
            mockMvc.perform(get("/books").param("isbn", "9780132350884"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].isbn").value("9780132350884"));
        }

        @Test
        @DisplayName("consulta por filtros combinados")
        void filterCombined() throws Exception {
            createBookAndGetId();
            mockMvc.perform(get("/books")
                            .param("author", "Martin")
                            .param("category", "Software")
                            .param("isbn", "9780132350884"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].isbn").value("9780132350884"));
        }

        @Test
        @DisplayName("retorna lista vazia quando não há resultados")
        void filterNoResults_emptyArray() throws Exception {
            mockMvc.perform(get("/books").param("title", "inexistente-xyz-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Sanidade")
    class Sanity {

        @Test
        @DisplayName("GET por id inexistente retorna 404")
        void getById_notFound() throws Exception {
            mockMvc.perform(get("/books/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
