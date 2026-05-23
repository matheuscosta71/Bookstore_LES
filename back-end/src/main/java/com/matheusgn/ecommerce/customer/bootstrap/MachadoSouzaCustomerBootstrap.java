package com.matheusgn.ecommerce.customer.bootstrap;

import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Garante um cliente da loja (tabela {@code customers}) com credenciais conhecidas quando o seed de demo
 * não roda (ex.: ISBN marcador já existente). Se {@link com.matheusgn.ecommerce.demo.DemoDataSeederService}
 * tiver criado o mesmo e-mail, este runner apenas ignora.
 * <p>
 * Não cria nem altera {@link com.matheusgn.ecommerce.auth.entity.AdminUser}: administradores
 * são outra entidade e outras credenciais (ex.: usuário {@code admin} em {@code application.yml}).
 * Este cadastro é apenas para login de cliente no e-commerce.
 */
@Component
@Order(110)
@RequiredArgsConstructor
@Slf4j
public class MachadoSouzaCustomerBootstrap implements ApplicationRunner {

    private static final String EMAIL = "machado.souza@example.com";

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final CustomerAddressService customerAddressService;
    private final CustomerCreditCardService customerCreditCardService;

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.existsByEmailIgnoreCase(EMAIL)) {
            log.debug("Cliente Machado Souza (e-mail {}) já existe; seed ignorado.", EMAIL);
            return;
        }

        log.info("Criando cliente de desenvolvimento Machado Souza (e-mail {})", EMAIL);

        var created = customerService.create(CustomerCreateRequest.builder()
                .fullName("Machado Souza")
                .email(EMAIL)
                .cpf("05801457490")
                .phone("51988776655")
                .birthDate(LocalDate.of(1990, 4, 22))
                .password("Senh@Forte")
                .confirmPassword("Senh@Forte")
                .active(true)
                .build());

        UUID id = created.getId();

        var delivery = AddressCreateRequest.builder()
                .nickname("Residência")
                .street("Rua Borges de Medeiros")
                .number("1280")
                .complement("Apto 302")
                .neighborhood("Centro Histórico")
                .city("Porto Alegre")
                .state("RS")
                .zipCode("90020130")
                .type(AddressType.DELIVERY)
                .build();
        customerAddressService.addAddress(id, delivery);

        var billing = AddressCreateRequest.builder()
                .nickname("Cobrança")
                .street("Av. Ipiranga")
                .number("6681")
                .neighborhood("Partenon")
                .city("Porto Alegre")
                .state("RS")
                .zipCode("90619900")
                .type(AddressType.BILLING)
                .build();
        customerAddressService.addAddress(id, billing);

        var card = CreditCardCreateRequest.builder()
                .cardholderName("Machado Souza")
                .cardNumber("4111111111111111")
                .brand("VISA")
                .expirationMonth(11)
                .expirationYear(2031)
                .preferred(true)
                .build();
        customerCreditCardService.addCard(id, card);

        log.info("Cliente Machado Souza criado com id={}", id);
    }
}
