package com.matheusgn.ecommerce.auth;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro em memória dos tokens emitidos no login (demo acadêmica; reinício do servidor invalida).
 */
@Component
public class IssuedBearerTokenRegistry {

    private final Set<String> adminTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> customerTokens = ConcurrentHashMap.newKeySet();

    public void registerAdmin(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            adminTokens.add(accessToken.trim());
        }
    }

    public void registerCustomer(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            customerTokens.add(accessToken.trim());
        }
    }

    public boolean isCustomerToken(String token) {
        return token != null && customerTokens.contains(token.trim());
    }
}
