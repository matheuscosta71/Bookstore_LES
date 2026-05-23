package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.config.PageConstraints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integração REST de clientes: paginação (RNF0011) e validação de cadastro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CustomerController — integração")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /customers retorna página com size default")
    void list_returnsPagedDefaultSize() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(PageConstraints.DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /customers com senhas divergentes retorna 400")
    void create_withMismatchedPasswords_returns400() throws Exception {
        String body = """
                {
                  "fullName": "Teste Validação",
                  "email": "valid-%s@test.com",
                  "cpf": "52998224725",
                  "phone": "31988887777",
                  "birthDate": "1992-03-15",
                  "password": "SenhaForte8!",
                  "confirmPassword": "OutraSenha8!",
                  "active": true
                }
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validação"));
    }
}
