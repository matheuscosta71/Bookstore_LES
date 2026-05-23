package com.matheusgn.ecommerce.auth.service;

import com.matheusgn.ecommerce.auth.dto.CustomerLoginRequest;
import com.matheusgn.ecommerce.auth.dto.CustomerLoginResponse;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.auth.IssuedBearerTokenRegistry;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.AuthenticationFailedException;
import com.matheusgn.ecommerce.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    // #region agent log
    private static void agentLog(String hypothesisId, String message, String dataJson) {
        try {
            String line = "{\"sessionId\":\"4b6485\",\"hypothesisId\":\"" + hypothesisId
                    + "\",\"location\":\"CustomerAuthService.login\",\"message\":\"" + message
                    + "\",\"data\":" + dataJson + ",\"timestamp\":" + System.currentTimeMillis() + "}\n";
            Files.writeString(
                    Path.of("/home/dell/Documentos/Trabalhos-clientes/Clientes/Matheus-GN/matheus-gn/.cursor/debug-4b6485.log"),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // debug instrumentation only
        }
    }
    // #endregion

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final IssuedBearerTokenRegistry issuedBearerTokenRegistry;

    @Transactional(readOnly = true)
    public CustomerLoginResponse login(CustomerLoginRequest request) {
        String email = request.getEmail().trim();
        Customer customer = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationFailedException("E-mail ou senha inválidos"));

        boolean pwdMatch = passwordEncoder.matches(request.getPassword(), customer.getPassword());
        // #region agent log
        agentLog(
                "H1",
                "after lookup",
                "{\"active\":" + customer.isActive() + ",\"passwordMatch\":" + pwdMatch + "}");
        // #endregion

        if (!pwdMatch) {
            throw new AuthenticationFailedException("E-mail ou senha inválidos");
        }
        if (!customer.isActive()) {
            throw new ForbiddenException(
                    "Seu cadastro está inativo. Não é possível acessar a loja com esta conta. Entre em contato com o suporte.");
        }

        String accessToken = UUID.randomUUID().toString();
        CustomerLoginResponse body = CustomerLoginResponse.builder()
                .authenticated(true)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .customerId(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .role("CLIENT")
                .build();
        issuedBearerTokenRegistry.registerCustomer(accessToken);
        return body;
    }
}
