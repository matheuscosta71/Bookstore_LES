package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.entity.CreditCard;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CreditCardMapper {

    public static CreditCardResponse toResponse(CreditCard c) {
        return CreditCardResponse.builder()
                .id(c.getId())
                .cardholderName(c.getCardholderName())
                .cardNumberMasked(CardNumberMasker.maskLastFour(c.getCardNumber()))
                .brand(c.getBrand())
                .expirationMonth(c.getExpirationMonth())
                .expirationYear(c.getExpirationYear())
                .preferred(c.isPreferred())
                .active(c.isActive())
                .build();
    }
}
