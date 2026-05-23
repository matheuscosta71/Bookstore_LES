package com.matheusgn.ecommerce.sales.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderNumberFormatterTest {

    @Test
    void fromUuid_lastFiveHexUppercase() {
        UUID id = UUID.fromString("f57c9b0f-61cc-4ad7-83ef-dd942b6fe90d");
        assertThat(OrderNumberFormatter.fromUuid(id)).isEqualTo("#FE90D");
    }
}
