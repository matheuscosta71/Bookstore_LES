package com.matheusgn.ecommerce.customer.dto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CardNumberMasker {

    public static String maskLastFour(String raw) {
        if (raw == null || raw.length() < 4) {
            return "****";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "****" + last4;
    }
}
