package com.matheusgn.ecommerce.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank
    @Schema(description = "Mensagem do usuário", example = "Quais livros de arquitetura você recomenda?")
    private String message;

    @Schema(description = "Opcional: enriquece o contexto com dados do cliente")
    private UUID customerId;

    @Schema(description = "Opcional: histórico da conversa")
    private List<ChatMessageDto> history;
}
