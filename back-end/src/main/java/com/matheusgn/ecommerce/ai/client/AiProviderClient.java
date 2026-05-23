package com.matheusgn.ecommerce.ai.client;

/**
 * Integração desacoplada com provedor de IA (HTTP, mock em testes).
 */
public interface AiProviderClient {

    String complete(String systemPrompt, String userMessage);
}
