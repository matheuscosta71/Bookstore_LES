package com.matheusgn.ecommerce.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Limites de paginação para listagens (ex.: catálogo, admin).
 * <p>
 * <b>RNF0011</b> (resposta em até 1 segundo) não é imposto aqui: {@link #MAX_PAGE_SIZE}
 * reduz carga por requisição, mas SLA de latência exige observabilidade, testes de carga
 * e orçamento por endpoint — não um único valor em código.
 */
public final class PageConstraints {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private PageConstraints() {}

    public static Pageable clamp(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }
}
