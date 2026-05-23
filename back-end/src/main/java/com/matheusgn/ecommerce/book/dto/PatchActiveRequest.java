package com.matheusgn.ecommerce.book.dto;

import com.matheusgn.ecommerce.book.entity.BookLifecycleReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchActiveRequest {

    @NotNull
    @Schema(example = "false")
    private Boolean active;

    /**
     * Obrigatório quando o estado ativo/inativo muda (RN0015, RN0017).
     */
    @Schema(example = "Baixa giro de vendas")
    private String justification;

    @Schema(description = "Motivo conforme ativar ou inativar (manual). Automático usa FORA_DE_MERCADO no servidor.")
    private BookLifecycleReason reason;
}
