package com.matheusgn.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cart.item")
public class CartItemProperties {

    /**
     * Tempo após o qual o item é considerado expirado para finalidade de compra (última atividade).
     */
    private int expirationMinutes = 30;

    /**
     * RNF0045: avisar o cliente este número de minutos antes do fim do prazo do item.
     */
    private int warningBeforeExpirationMinutes = 5;
}
