package com.matheusgn.ecommerce.sales.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class CreateExchangeBatchRequest {

    @NotEmpty(message = "Pelo menos um item deve ser selecionado para troca")
    private List<UUID> orderItemIds;
}
