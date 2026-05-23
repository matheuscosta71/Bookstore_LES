package com.matheusgn.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.book")
public class BookBusinessProperties {

    private IsbnExternal isbnExternal = new IsbnExternal();

    /** Chave em {@code X-Sales-Manager-Key}; padrão alinhado ao admin para demo. */
    private String salesManagerKey = "8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12";

    @Getter
    @Setter
    public static class IsbnExternal {
        private boolean enabled = true;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 8000;
    }

    public boolean matchesSalesManagerKey(String headerValue) {
        return headerValue != null && headerValue.equals(salesManagerKey);
    }
}
