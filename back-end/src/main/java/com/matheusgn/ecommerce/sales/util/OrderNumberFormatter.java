package com.matheusgn.ecommerce.sales.util;

import java.util.Locale;
import java.util.UUID;

/**
 * Número amigável para exibição (ex.: {@code #A1B2C}), derivado dos últimos 5 caracteres hex do UUID.
 */
public final class OrderNumberFormatter {

    private OrderNumberFormatter() {}

    public static String fromUuid(UUID id) {
        if (id == null) {
            return null;
        }
        String hex = id.toString().replace("-", "");
        return "#" + hex.substring(hex.length() - 5).toUpperCase(Locale.ROOT);
    }
}
