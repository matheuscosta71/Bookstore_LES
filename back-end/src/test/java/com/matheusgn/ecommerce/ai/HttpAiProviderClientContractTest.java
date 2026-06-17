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

    @Test
    @DisplayName("deve retornar recomendacoes quando no fallback apos primeira interacao")
    void shouldReturnRecommendationsInFallbackAfterFirstInteraction() throws Exception {
        AiProperties props = new AiProperties();
        props.setBaseUrl("https://api.openai.com/v1"); // non-local to trigger fallback
        props.setApiKey(""); // default/blank to trigger fallback

        // Mock BookRepository
        var bookRepositoryMock = org.mockito.Mockito.mock(com.matheusgn.ecommerce.book.repository.BookRepository.class);
        var book = com.matheusgn.ecommerce.book.entity.Book.builder()
                .id(java.util.UUID.randomUUID())
                .title("Clean Code")
                .author("Robert C. Martin")
                .category("Tecnologia")
                .salePrice(new java.math.BigDecimal("89.90"))
                .stockQuantity(5)
                .active(true)
                .build();
        org.mockito.Mockito.when(bookRepositoryMock.findAll()).thenReturn(java.util.List.of(book));

        HttpAiProviderClient client = client(props);
        org.springframework.test.util.ReflectionTestUtils.setField(client, "bookRepository", bookRepositoryMock);

        java.util.List<com.matheusgn.ecommerce.ai.dto.ChatMessageDto> history = java.util.List.of(
                new com.matheusgn.ecommerce.ai.dto.ChatMessageDto("user", "oi"),
                new com.matheusgn.ecommerce.ai.dto.ChatMessageDto("assistant", 
                        "Olá! Eu sou o assistente virtual da Livraria Matheus GN. " +
                        "Como posso ajudar você hoje? Você pode me pedir recomendações de livros por tema " +
                        "(como Ficção, Literatura, Infantil, Romance) ou perguntar sobre os livros disponíveis no nosso acervo!")
        );

        String reply = client.complete("system", history, "Mensagem do usuário: sim");

        assertThat(reply).contains("Clean Code");
        assertThat(reply).contains("sugestões");
    }

    @Test
    @DisplayName("deve retornar fallback assertivo para Romance quando nenhum livro de Romance estiver cadastrado")
    void shouldReturnAssertiveRomanceFallbackWhenNoRomanceBooks() throws Exception {
        AiProperties props = new AiProperties();
        props.setBaseUrl("https://api.openai.com/v1");
        props.setApiKey("");

        // Mock BookRepository returning only a Technology book (no romance)
        var bookRepositoryMock = org.mockito.Mockito.mock(com.matheusgn.ecommerce.book.repository.BookRepository.class);
        var book = com.matheusgn.ecommerce.book.entity.Book.builder()
                .id(java.util.UUID.randomUUID())
                .title("Clean Code")
                .author("Robert C. Martin")
                .category("Tecnologia")
                .salePrice(new java.math.BigDecimal("89.90"))
                .stockQuantity(5)
                .active(true)
                .build();
        org.mockito.Mockito.when(bookRepositoryMock.findAll()).thenReturn(java.util.List.of(book));

        HttpAiProviderClient client = client(props);
        org.springframework.test.util.ReflectionTestUtils.setField(client, "bookRepository", bookRepositoryMock);

        String reply = client.complete("system", "quais livros são de romance?");

        assertThat(reply).contains("não encontrei livros do tema Romance");
        assertThat(reply).doesNotContain("Clean Code");
    }

    @Test
    @DisplayName("deve retornar fallback assertivo estatico para Romance quando banco estiver vazio")
    void shouldReturnAssertiveRomanceFallbackStaticWhenDbEmpty() {
        AiProperties props = new AiProperties();
        props.setBaseUrl("https://api.openai.com/v1");
        props.setApiKey("");

        // BookRepository returns empty list
        var bookRepositoryMock = org.mockito.Mockito.mock(com.matheusgn.ecommerce.book.repository.BookRepository.class);
        org.mockito.Mockito.when(bookRepositoryMock.findAll()).thenReturn(java.util.List.of());

        HttpAiProviderClient client = client(props);
        org.springframework.test.util.ReflectionTestUtils.setField(client, "bookRepository", bookRepositoryMock);

        String reply = client.complete("system", "quais livros são de romance?");

        assertThat(reply).contains("não encontrei livros do tema Romance");
    }

    @Test
    @DisplayName("deve retornar recomendacao de infantil mesmo com erro de grafia ou plural como infanis")
    void shouldReturnInfantilRecommendationsWhenQueryHasTypo() throws Exception {
        AiProperties props = new AiProperties();
        props.setBaseUrl("https://api.openai.com/v1");
        props.setApiKey("");

        var bookRepositoryMock = org.mockito.Mockito.mock(com.matheusgn.ecommerce.book.repository.BookRepository.class);
        var book = com.matheusgn.ecommerce.book.entity.Book.builder()
                .id(java.util.UUID.randomUUID())
                .title("Harry Potter")
                .author("J. K. Rowling")
                .category("Infantil")
                .salePrice(new java.math.BigDecimal("299.00"))
                .stockQuantity(5)
                .active(true)
                .build();
        org.mockito.Mockito.when(bookRepositoryMock.findAll()).thenReturn(java.util.List.of(book));

        HttpAiProviderClient client = client(props);
        org.springframework.test.util.ReflectionTestUtils.setField(client, "bookRepository", bookRepositoryMock);

        // Test with typo "infanis"
        String reply1 = client.complete("system", "livros infanis");
        assertThat(reply1).contains("Harry Potter");

        // Test with plural "infantis"
        String reply2 = client.complete("system", "infantis");
        assertThat(reply2).contains("Harry Potter");
    }

    @Test
    @DisplayName("deve retornar recomendacao de ficcao mesmo com erro de grafia como ficicao")
    void shouldReturnFictionRecommendationsWhenQueryHasTypoFicicao() throws Exception {
        AiProperties props = new AiProperties();
        props.setBaseUrl("https://api.openai.com/v1");
        props.setApiKey("");

        var bookRepositoryMock = org.mockito.Mockito.mock(com.matheusgn.ecommerce.book.repository.BookRepository.class);
        var book = com.matheusgn.ecommerce.book.entity.Book.builder()
                .id(java.util.UUID.randomUUID())
                .title("1984")
                .author("George Orwell")
                .category("Ficção")
                .salePrice(new java.math.BigDecimal("45.00"))
                .stockQuantity(5)
                .active(true)
                .build();
        org.mockito.Mockito.when(bookRepositoryMock.findAll()).thenReturn(java.util.List.of(book));

        HttpAiProviderClient client = client(props);
        org.springframework.test.util.ReflectionTestUtils.setField(client, "bookRepository", bookRepositoryMock);

        // Test with typo "ficição"
        String reply = client.complete("system", "ficição");
        assertThat(reply).contains("1984");
    }
}
