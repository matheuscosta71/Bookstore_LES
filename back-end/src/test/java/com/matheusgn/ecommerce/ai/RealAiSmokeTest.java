package com.matheusgn.ecommerce.ai;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke opcional: chama o provedor real (OpenAI-compatible) quando {@code APP_AI_API_KEY} está definida.
 * Não executa na suíte local/CI sem a variável; nunca quebra o build.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "APP_AI_API_KEY", matches = ".+")
@DisplayName("Smoke IA (opcional, API real)")
class RealAiSmokeTest {

    @DynamicPropertySource
    static void registerAiKey(DynamicPropertyRegistry registry) {
        String key = System.getenv("APP_AI_API_KEY");
        if (key != null && !key.isBlank()) {
            registry.add("app.ai.api-key", () -> key);
        }
    }

    @Autowired
    private AiProviderClient aiProviderClient;

    @Test
    @DisplayName("complete retorna texto não vazio do provedor")
    void providerReturnsNonBlankText() {
        String reply = aiProviderClient.complete(
                "Responda apenas com a palavra pong.",
                "ping");
        assertThat(reply).isNotBlank();
    }
}
