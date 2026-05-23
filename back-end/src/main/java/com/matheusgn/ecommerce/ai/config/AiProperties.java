package com.matheusgn.ecommerce.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String baseUrl = "https://api.openai.com/v1";

    /** Bearer token (deixe vazio para falhar de forma controlada em produção sem chave). */
    private String apiKey = "";

    private String model = "gpt-4o-mini";

    /** Timeout de conexão HTTP com o provedor (ms). */
    private int connectTimeoutMs = 5_000;

    /** Timeout de leitura da resposta (ms). */
    private int readTimeoutMs = 60_000;
}
