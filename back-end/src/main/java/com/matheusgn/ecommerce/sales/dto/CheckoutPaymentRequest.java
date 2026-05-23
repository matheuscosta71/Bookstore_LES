package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPaymentRequest {

    @NotEmpty
    @Valid
    private List<PaymentLineRequest> lines;

    /** Novo cartão opcional para pagamento com cartão novo */
    private CreditCardCreateRequest newCreditCard;

    /** Obrigatório quando {@code newCreditCard} for informado */
    private Boolean saveNewCardToProfile;
}
