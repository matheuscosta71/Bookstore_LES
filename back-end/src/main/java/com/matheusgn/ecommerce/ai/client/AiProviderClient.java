package com.matheusgn.ecommerce.ai.client;

import com.matheusgn.ecommerce.ai.dto.ChatMessageDto;
import java.util.List;

/**
 * Integração desacoplada com provedor de IA (HTTP, mock em testes).
 */
public interface AiProviderClient {

    String complete(String systemPrompt, String userMessage);

    String complete(String systemPrompt, List<ChatMessageDto> history, String userMessage);
}
