package com.matheusgn.ecommerce.auth.service;

import com.matheusgn.ecommerce.auth.entity.AdminUser;
import com.matheusgn.ecommerce.auth.repository.AdminUserRepository;
import com.matheusgn.ecommerce.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AdminUserBootstrapService {

    private final AdminUserRepository adminUserRepository;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void ensureDefaultAdminCredentials() {
        String username = adminProperties.getUsername().trim();
        String rawPassword = adminProperties.getPassword();

        AdminUser user = adminUserRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> AdminUser.builder()
                        .username(username)
                        .active(true)
                        .build());

        boolean changed = false;
        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            changed = true;
        }
        if (user.getId() == null || changed) {
            adminUserRepository.save(user);
        }
    }
}
