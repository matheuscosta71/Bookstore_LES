package com.matheusgn.ecommerce.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProviderClient aiProviderClient;

    private static String customerJson(String emailSuffix) {
        return """
                {
                  "fullName": "Cliente IA",
                  "email": "ia-%s@test.com",
                  "cpf": "52998224725",
                  "phone": "31988887777",
                  "birthDate": "1992-03-15",
                  "password": "SenhaForte8!",
                  "confirmPassword": "SenhaForte8!",
                  "active": true
                }
                """.formatted(emailSuffix);
    }

    @Nested
    @DisplayName("POST /ai/recommendations/{customerId}")
    class Recommendations {

        @Test
        @DisplayName("deve retornar 200 com texto quando cliente existe e provedor responde")
        void shouldReturn200WithTextWhenCustomerExists() throws Exception {
            when(aiProviderClient.complete(anyString(), anyString())).thenReturn("lista de livros");

            MvcResult created = mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customerJson(UUID.randomUUID().toString())))
                    .andExpect(status().isCreated())
                    .andReturn();
            JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
            String customerId = node.get("id").asText();

            mockMvc.perform(post("/ai/recommendations/" + customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("lista de livros"));
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não existe")
        void shouldReturn404WhenCustomerMissing() throws Exception {
            mockMvc.perform(post("/ai/recommendations/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
        }

        @Test
        @DisplayName("deve retornar 503 quando provedor falha sem expor stack trace")
        void shouldReturn503WhenProviderFails() throws Exception {
            when(aiProviderClient.complete(anyString(), anyString()))
                    .thenThrow(new AiProviderException("timeout"));

            MvcResult created = mockMvc.perform(post("/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customerJson("r-" + UUID.randomUUID())))
                    .andExpect(status().isCreated())
                    .andReturn();
            String customerId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

            MvcResult err = mockMvc.perform(post("/ai/recommendations/" + customerId))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("Provedor de IA indisponível"))
                    .andExpect(jsonPath("$.detail").value("timeout"))
                    .andReturn();
            JsonNode body = objectMapper.readTree(err.getResponse().getContentAsString());
            assertThat(body.toString()).doesNotContain("stacktrace").doesNotContain("StackTrace");
        }
    }

    @Nested
    @DisplayName("POST /ai/chat")
    class Chat {

        @Test
        @DisplayName("deve responder 200 com reply do provedor")
        void shouldReturn200WithReply() throws Exception {
            when(aiProviderClient.complete(anyString(), anyString())).thenReturn("ok");

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Olá\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value("ok"));
        }

        @Test
        @DisplayName("deve retornar 400 quando mensagem vazia")
        void shouldReturn400WhenMessageInvalid() throws Exception {
            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validação"));
        }

        @Test
        @DisplayName("deve retornar 400 quando JSON ausente de message")
        void shouldReturn400WhenMessageMissing() throws Exception {
            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 503 quando provedor falha")
        void shouldReturn503WhenProviderFails() throws Exception {
            when(aiProviderClient.complete(anyString(), anyString()))
                    .thenThrow(new AiProviderException("falha"));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"ping\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("Provedor de IA indisponível"));
        }

        @Test
        @DisplayName("Cenário 1 — Recomendação de produto compatível")
        void shouldRecommendCompatibleProductWhenAvailable() throws Exception {
            String aiResponse = "Para aprender programação, recomendo os seguintes livros cadastrados: 'Clean Code' e 'Arquitetura Limpa'.";
            when(aiProviderClient.complete(anyString(), contains("Quero aprender programação"))).thenReturn(aiResponse);

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Quero aprender programação\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value(aiResponse));
        }

        @Test
        @DisplayName("Cenário 2 — Ausência de produto compatível")
        void shouldReturnAppropriateMessageWhenNoCompatibleProduct() throws Exception {
            String aiResponse = "Não encontrei recomendações compatíveis com a sua solicitação de bicicleta. Tente buscar por outros temas.";
            when(aiProviderClient.complete(anyString(), contains("Quero comprar uma bicicleta"))).thenReturn(aiResponse);

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Quero comprar uma bicicleta\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value(aiResponse));
        }
    }
}
