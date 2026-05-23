package com.matheusgn.ecommerce.integration;

import com.matheusgn.ecommerce.config.PageConstraints;
import org.junit.jupiter.api.DisplayName;
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
 * RNF0011: validação indireta — paginação, filtros e limite de tamanho (sem SLA de 1s em CI).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaginationAndQueryBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /books usa page padrão com size default")
    void booksList_usesDefaultPageSize() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(PageConstraints.DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /books respeita page e size solicitados")
    void booksList_respectsPageAndSize() throws Exception {
        mockMvc.perform(get("/books").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /books limita size acima do máximo a 100")
    void booksList_clampsOversizedPageRequest() throws Exception {
        mockMvc.perform(get("/books").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(PageConstraints.MAX_PAGE_SIZE));
    }

    @Test
    @DisplayName("GET /books com filtro por título retorna página (sem lista infinita)")
    void booksList_withTitleFilter_returnsPagedStructure() throws Exception {
        mockMvc.perform(get("/books").param("title", "InexistenteTituloXYZ123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    @DisplayName("GET /customers usa size default")
    void customersList_usesDefaultPageSize() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(PageConstraints.DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /customers respeita size e não retorna lista solta")
    void customersList_respectsPageAndSize() throws Exception {
        mockMvc.perform(get("/customers").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));
    }
}
