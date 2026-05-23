package com.matheusgn.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    /** Chave enviada no header X-Admin-Key para endpoints administrativos */
    private String key = "8a3a0e3c-2b4f-4f8a-8f5c-6e0b8b8c9e12";

    /** Usuário padrão para autenticação administrativa de demonstração. */
    private String username = "admin";

    /** Senha padrão para autenticação administrativa de demonstração. */
    private String password = "admin";
}
