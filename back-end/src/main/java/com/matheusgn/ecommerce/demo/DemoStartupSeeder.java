package com.matheusgn.ecommerce.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class DemoStartupSeeder implements ApplicationRunner {

    private final DemoDataSeederService demoDataSeederService;

    @Override
    public void run(ApplicationArguments args) {
        demoDataSeederService.seedIfNeeded();
    }
}
