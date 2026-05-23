package com.matheusgn.ecommerce.demo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.demo.seed")
public class DemoSeedProperties {

    /**
     * Quando true, o runner tenta popular dados de demonstração na subida (idempotente via ISBN marcador).
     * Em testes automatizados, mantenha false em {@code src/test/resources/application.yml}.
     */
    private boolean enabled = false;

    /** Janela [1, daysBack] dias no passado para datas sintéticas dos pedidos demo. */
    private int daysBack = 21;

    /** ISBN-13 real do primeiro livro demo (The Great Gatsby, US); se já existir, o seed completo é ignorado. */
    private String markerIsbn = "9780743273565";
}
