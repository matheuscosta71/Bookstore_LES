package com.matheusgn.ecommerce.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.matheusgn.ecommerce.ai.config.AiProperties;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class HttpAiProviderClient implements AiProviderClient {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String complete(String systemPrompt, String userMessage) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new AiProviderException("Chave de API de IA não configurada (app.ai.api-key)");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", aiProperties.getModel());
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userMessage);

        RestClient client = restClientBuilder.baseUrl(aiProperties.getBaseUrl()).build();
        try {
            String raw = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey().trim())
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new AiProviderException("Resposta do provedor de IA inválida");
            }
            return content.asText();
        } catch (AiProviderException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new AiProviderException("Falha ao chamar provedor de IA: HTTP " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new AiProviderException("Falha ao processar resposta da IA", e);
        }
    }
}
