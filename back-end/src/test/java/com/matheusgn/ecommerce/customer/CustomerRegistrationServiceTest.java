package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerResponse;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerCodeGeneratorService;
import com.matheusgn.ecommerce.customer.service.CustomerService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RNF0031–RNF0033: política de senha, confirmação e persistência com hash no cadastro ({@link CustomerService#create}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cadastro de cliente (RNFs senha e hash)")
class CustomerRegistrationServiceTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CustomerCodeGeneratorService customerCodeGeneratorService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CustomerService customerService;

    private static final UUID ID = UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22");

    @BeforeAll
    static void setupValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Nested
    @DisplayName("RNF0031 — Senha forte (Bean Validation)")
    class Rnf0031 {

        @Test
        @DisplayName("deve aceitar senha forte válida")
        void shouldAcceptStrongPassword() {
            CustomerCreateRequest req = baseRequestBuilder()
                    .password("SenhaForte1!")
                    .confirmPassword("SenhaForte1!")
                    .build();
            Set<ConstraintViolation<CustomerCreateRequest>> v = validator.validate(req);
            assertThat(v).isEmpty();
        }

        @Test
        @DisplayName("deve rejeitar senha sem maiúscula")
        void shouldRejectWithoutUppercase() {
            CustomerCreateRequest req = baseRequestBuilder()
                    .password("senhafraca1!")
                    .confirmPassword("senhafraca1!")
                    .build();
            assertThat(validator.validate(req)).isNotEmpty();
        }

        @Test
        @DisplayName("deve rejeitar senha sem caractere especial")
        void shouldRejectWithoutSpecialChar() {
            CustomerCreateRequest req = baseRequestBuilder()
                    .password("SenhaFraca12")
                    .confirmPassword("SenhaFraca12")
                    .build();
            assertThat(validator.validate(req)).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("RNF0032 — Confirmação de senha")
    class Rnf0032 {

        @Test
        @DisplayName("deve rejeitar quando confirmPassword difere")
        void shouldRejectMismatchedConfirmation() {
            CustomerCreateRequest req = baseRequestBuilder()
                    .password("SenhaForte1!")
                    .confirmPassword("OutraSenha1!")
                    .build();
            Set<ConstraintViolation<CustomerCreateRequest>> v = validator.validate(req);
            assertThat(v).extracting(ConstraintViolation::getMessage)
                    .anyMatch(m -> m.toLowerCase().contains("coincid"));
        }
    }

    @Nested
    @DisplayName("RNF0033 — Hash BCrypt na persistência")
    class Rnf0033 {

        @Test
        @DisplayName("deve persistir hash, nunca senha em claro")
        void shouldSaveEncodedPassword() {
            when(customerRepository.existsByEmailIgnoreCase("novo@registro.com")).thenReturn(false);
            when(customerRepository.existsByCpf("52998224725")).thenReturn(false);
            when(passwordEncoder.encode("SenhaForte1!")).thenReturn("$2a$HASH");
            when(customerCodeGeneratorService.nextCode()).thenReturn("CUST-000099");
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(ID);
                c.setCreatedAt(Instant.now());
                c.setUpdatedAt(Instant.now());
                return c;
            });

            CustomerCreateRequest req = baseRequestBuilder()
                    .email("novo@registro.com")
                    .password("SenhaForte1!")
                    .confirmPassword("SenhaForte1!")
                    .build();

            CustomerResponse res = customerService.create(req);
            assertThat(res.getEmail()).isEqualTo("novo@registro.com");

            ArgumentCaptor<Customer> cap = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(cap.capture());
            assertThat(cap.getValue().getPassword()).isEqualTo("$2a$HASH").isNotEqualTo("SenhaForte1!");
        }
    }

    private static CustomerCreateRequest.CustomerCreateRequestBuilder baseRequestBuilder() {
        return CustomerCreateRequest.builder()
                .fullName("Cliente Registro")
                .email("base@registro.com")
                .cpf("52998224725")
                .phone("31999999999")
                .birthDate(LocalDate.of(1991, 6, 10));
    }
}
