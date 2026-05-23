package com.matheusgn.ecommerce.auth.service;

import com.matheusgn.ecommerce.auth.IssuedBearerTokenRegistry;
import com.matheusgn.ecommerce.auth.dto.AdminLoginRequest;
import com.matheusgn.ecommerce.auth.dto.AdminLoginResponse;
import com.matheusgn.ecommerce.auth.entity.AdminUser;
import com.matheusgn.ecommerce.auth.repository.AdminUserRepository;
import com.matheusgn.ecommerce.exception.AuthenticationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final IssuedBearerTokenRegistry issuedBearerTokenRegistry;

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        AdminUser adminUser = adminUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AuthenticationFailedException("Usuário ou senha inválidos"));

        if (!adminUser.isActive() || !passwordEncoder.matches(password, adminUser.getPassword())) {
            throw new AuthenticationFailedException("Usuário ou senha inválidos");
        }

        String accessToken = UUID.randomUUID().toString();
        AdminLoginResponse body = AdminLoginResponse.builder()
                .authenticated(true)
                .role("ADMIN")
                .username(adminUser.getUsername())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
        issuedBearerTokenRegistry.registerAdmin(accessToken);
        return body;
    }
}
