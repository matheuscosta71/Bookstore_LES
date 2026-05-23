package com.matheusgn.ecommerce.config;

import com.matheusgn.ecommerce.auth.IssuedBearerTokenRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloqueia uso do Bearer de cliente em rotas {@code /admin/**}.
 */
@Component
@RequiredArgsConstructor
public class CustomerBearerAdminPathFilter extends OncePerRequestFilter {

    private final IssuedBearerTokenRegistry tokenRegistry;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path == null || !path.startsWith("/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.length() < 8 || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7).trim();
        if (tokenRegistry.isCustomerToken(token)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Acesso negado\",\"status\":403,\"detail\":\"Acesso restrito a administradores.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
