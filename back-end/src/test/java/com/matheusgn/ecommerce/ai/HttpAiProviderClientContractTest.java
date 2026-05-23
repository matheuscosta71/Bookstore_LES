package com.matheusgn.ecommerce.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.ai.client.HttpAiProviderClient;
import com.matheusgn.ecommerce.ai.config.AiProperties;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato HTTP com estilo OpenAI {@code /chat/completions}: payload, headers, parsing e erros.
 */
class HttpAiProviderClientContractTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void shutdown() throws Exception {
        server.shutdown();
    }

    private static String baseUrl(MockWebServer server) {
        String base = server.url("/v1").toString();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static HttpAiProviderClient client(AiProperties props) {
        return new HttpAiProviderClient(props, new ObjectMapper(), RestClient.builder());
    }

    private static HttpAiProviderClient clientWithTimeouts(AiProperties props, int connectMs, int readMs) {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(connectMs))
                .build();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(java.time.Duration.ofMillis(readMs));
        return new HttpAiProviderClient(props, new ObjectMapper(), RestClient.builder().requestFactory(factory));
    }

    @Test
    @DisplayName("deve enviar POST com model, messages system/user e Authorization Bearer")
    void shouldSendExpectedJsonPayloadAndBearer() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}")
                .addHeader("Content-Type", "application/json"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("sk-test-secret");
        props.setModel("gpt-test-model");

        assertThat(client(props).complete("Sys aqui", "User aqui")).isEqualTo("ok");

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer sk-test-secret");

        JsonNode body = new ObjectMapper().readTree(req.getBody().readUtf8());
        assertThat(body.path("model").asText()).isEqualTo("gpt-test-model");
        assertThat(body.path("messages").size()).isEqualTo(2);
        assertThat(body.path("messages").path(0).path("role").asText()).isEqualTo("system");
        assertThat(body.path("messages").path(0).path("content").asText()).isEqualTo("Sys aqui");
        assertThat(body.path("messages").path(1).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").path(1).path("content").asText()).isEqualTo("User aqui");
    }

    @Test
    @DisplayName("deve mapear choices[0].message.content para texto")
    void shouldMapAssistantContent() {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"Resposta\"}}]}"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThat(client(props).complete("s", "u")).isEqualTo("Resposta");
    }

    @Test
    @DisplayName("HTTP 500 do provedor deve virar AiProviderException")
    void http500_shouldThrowAiProviderException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("err"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("HTTP 429 do provedor deve virar AiProviderException")
    void http429_shouldThrowAiProviderException() {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("rate limit"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("JSON inválido deve virar AiProviderException")
    void invalidJson_shouldThrow() {
        server.enqueue(new MockResponse().setBody("not-json{{{"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    @DisplayName("choices vazio deve virar AiProviderException")
    void emptyChoices_shouldThrow() {
        server.enqueue(new MockResponse().setBody("{\"choices\":[]}"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    @DisplayName("content null deve virar AiProviderException")
    void nullContent_shouldThrow() {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":null}}]}"));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    @DisplayName("api-key vazia deve falhar antes da rede")
    void blankApiKey_shouldThrowWithoutRequest() throws Exception {
        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("  ");

        assertThatThrownBy(() -> client(props).complete("s", "u"))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Chave");
        assertThat(server.takeRequest(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @Disabled("Leitura lenta com MockWebServer nem sempre dispara read-timeout do JDK HttpClient em todos os SO/JDK.")
    @DisplayName("read timeout deve resultar em AiProviderException")
    void readTimeout_shouldThrowAiProviderException() {
        server.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"late\"}}]}")
                .setBodyDelay(5, TimeUnit.SECONDS));

        AiProperties props = new AiProperties();
        props.setBaseUrl(baseUrl(server));
        props.setApiKey("k");

        assertThatThrownBy(() -> clientWithTimeouts(props, 2000, 80).complete("s", "u"))
                .isInstanceOf(AiProviderException.class);
    }
}
