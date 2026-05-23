package com.matheusgn.ecommerce.auth.bootstrap;

import com.matheusgn.ecommerce.auth.service.AdminUserBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class AdminUserStartupBootstrap implements ApplicationRunner {

    private final AdminUserBootstrapService adminUserBootstrapService;

    @Override
    public void run(ApplicationArguments args) {
        adminUserBootstrapService.ensureDefaultAdminCredentials();
    }
}
