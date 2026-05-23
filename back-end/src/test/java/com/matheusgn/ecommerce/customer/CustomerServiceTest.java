package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerUpdateRequest;
import com.matheusgn.ecommerce.customer.dto.PasswordChangeRequest;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerCodeGeneratorService;
import com.matheusgn.ecommerce.customer.service.CustomerService;
import com.matheusgn.ecommerce.exception.DuplicateResourceException;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link com.matheusgn.ecommerce.customer.service.CustomerService}.
 * <p>
 * RNs não cobertas aqui: endereços obrigatórios de cobrança/entrega; ranking; validação forte de cartão —
 * estão em DTOs / {@code CustomerCreditCardService} ou não existem no código atual.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String STRONG_PASSWORD = "SenhaSegura1!";

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

    private UUID customerId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22");
        customer = Customer.builder()
                .id(customerId)
                .code("CUST-000001")
                .fullName("Maria Silva")
                .email("maria@email.com")
                .cpf("52998224725")
                .phone("31999998888")
                .birthDate(LocalDate.of(1990, 5, 20))
                .password("hash")
                .active(true)
                .createdAt(Instant.parse("2025-01-01T12:00:00Z"))
                .updatedAt(Instant.parse("2025-01-01T12:00:00Z"))
                .build();
    }

    @Nested
    @DisplayName("RF0021 — Cadastrar cliente")
    class Rf0021 {

        @Test
        @DisplayName("givenNewCustomer_whenCreate_thenPersistsEncodedPassword")
        void givenNewCustomer_whenCreate_thenPersistsEncodedPassword() {
            when(customerRepository.existsByEmailIgnoreCase("maria@email.com")).thenReturn(false);
            when(customerRepository.existsByCpf("52998224725")).thenReturn(false);
            when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("HASH_BCRYPT");
            when(customerCodeGeneratorService.nextCode()).thenReturn("CUST-000001");
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(customerId);
                c.setCreatedAt(Instant.now());
                c.setUpdatedAt(Instant.now());
                return c;
            });

            CustomerCreateRequest req = CustomerCreateRequest.builder()
                    .fullName("Maria Silva")
                    .email("maria@email.com")
                    .cpf("52998224725")
                    .phone("31999998888")
                    .birthDate(LocalDate.of(1990, 5, 20))
                    .password(STRONG_PASSWORD)
                    .confirmPassword(STRONG_PASSWORD)
                    .active(true)
                    .build();

            var response = customerService.create(req);

            assertThat(response.getEmail()).isEqualTo("maria@email.com");
            assertThat(response.getCpf()).isEqualTo("52998224725");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("HASH_BCRYPT");
        }

        @Test
        @DisplayName("givenDuplicateEmail_whenCreate_thenDuplicateResourceException")
        void givenDuplicateEmail_whenCreate_thenDuplicateResourceException() {
            when(customerRepository.existsByEmailIgnoreCase("maria@email.com")).thenReturn(true);

            CustomerCreateRequest req = CustomerCreateRequest.builder()
                    .fullName("Maria")
                    .email("maria@email.com")
                    .cpf("52998224725")
                    .phone("31999998888")
                    .birthDate(LocalDate.of(1990, 5, 20))
                    .password(STRONG_PASSWORD)
                    .confirmPassword(STRONG_PASSWORD)
                    .build();

            assertThatThrownBy(() -> customerService.create(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("field", "email");
        }

        @Test
        @DisplayName("givenDuplicateCpf_whenCreate_thenDuplicateResourceException")
        void givenDuplicateCpf_whenCreate_thenDuplicateResourceException() {
            when(customerRepository.existsByEmailIgnoreCase("outro@email.com")).thenReturn(false);
            when(customerRepository.existsByCpf("52998224725")).thenReturn(true);

            CustomerCreateRequest req = CustomerCreateRequest.builder()
                    .fullName("Maria")
                    .email("outro@email.com")
                    .cpf("52998224725")
                    .phone("31999998888")
                    .birthDate(LocalDate.of(1990, 5, 20))
                    .password(STRONG_PASSWORD)
                    .confirmPassword(STRONG_PASSWORD)
                    .build();

            assertThatThrownBy(() -> customerService.create(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("field", "cpf");
        }
    }

    @Nested
    @DisplayName("RF0022 — Alterar cliente")
    class Rf0022 {

        @Test
        @DisplayName("givenExistingCustomer_whenUpdate_thenPersistsWithoutReencodingPassword")
        void givenExistingCustomer_whenUpdate_thenPersistsWithoutReencodingPassword() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.existsByEmailIgnoreCaseAndIdNot("nova@email.com", customerId)).thenReturn(false);
            when(customerRepository.existsByCpfAndIdNot("52998224725", customerId)).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            CustomerUpdateRequest req = CustomerUpdateRequest.builder()
                    .fullName("Maria Santos")
                    .email("nova@email.com")
                    .cpf("52998224725")
                    .phone("31988887777")
                    .birthDate(LocalDate.of(1990, 5, 20))
                    .active(true)
                    .build();

            var response = customerService.update(customerId, req);

            assertThat(response.getFullName()).isEqualTo("Maria Santos");
            org.mockito.Mockito.verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("givenMissingCustomer_whenUpdate_thenResourceNotFound")
        void givenMissingCustomer_whenUpdate_thenResourceNotFound() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            CustomerUpdateRequest req = CustomerUpdateRequest.builder()
                    .fullName("X")
                    .email("x@x.com")
                    .cpf("52998224725")
                    .phone("1")
                    .birthDate(LocalDate.now())
                    .active(true)
                    .build();

            assertThatThrownBy(() -> customerService.update(customerId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("RF0023 — Inativar")
    class Rf0023 {

        @Test
        @DisplayName("givenActiveCustomer_whenSetInactive_thenReturnsInactive")
        void givenActiveCustomer_whenSetInactive_thenReturnsInactive() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = customerService.setActive(customerId, false);

            assertThat(response.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("RF0024 — Filtros")
    class Rf0024 {

        @Test
        @DisplayName("givenFilters_whenFindByFilters_thenDelegatesToRepository")
        void givenFilters_whenFindByFilters_thenDelegatesToRepository() {
            when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(customer)));

            var page = customerService.findByFilters("Maria", null, null, null, null, null, true, PageRequest.of(0, 20));

            assertThat(page.getContent()).hasSize(1);
            verify(customerRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("RF0028 — Senha")
    class Rf0028 {

        @Test
        @DisplayName("givenNewPassword_whenChangePassword_thenSavesEncodedHash")
        void givenNewPassword_whenChangePassword_thenSavesEncodedHash() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(passwordEncoder.encode("NovaSenha12!")).thenReturn("NOVO_HASH");
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            customerService.changePassword(customerId, PasswordChangeRequest.builder()
                    .newPassword("NovaSenha12!")
                    .build());

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("NOVO_HASH");
        }
    }
}
