package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.customer.dto.CreditCardCreateRequest;
import com.matheusgn.ecommerce.customer.entity.CreditCard;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CreditCardRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerCreditCardService;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCreditCardServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @InjectMocks
    private CustomerCreditCardService service;

    private UUID customerId;
    private UUID cardId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33");
        cardId = UUID.fromString("d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44");
        customer = Customer.builder()
                .id(customerId)
                .fullName("João")
                .email("joao@email.com")
                .cpf("11144477735")
                .phone("11999999999")
                .birthDate(java.time.LocalDate.of(1988, 1, 1))
                .password("x")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("RF0027 — Cartão preferencial")
    class Preferred {

        @Test
        @DisplayName("primeiro cartão ativo torna-se preferencial automaticamente")
        void firstCardBecomesPreferred() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(creditCardRepository.countByCustomer_IdAndActiveTrue(customerId)).thenReturn(0L);
            when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> {
                CreditCard c = inv.getArgument(0);
                c.setId(cardId);
                return c;
            });

            CreditCardCreateRequest req = CreditCardCreateRequest.builder()
                    .cardholderName("João")
                    .cardNumber("4111111111111111")
                    .brand("VISA")
                    .expirationMonth(12)
                    .expirationYear(2030)
                    .preferred(false)
                    .build();

            var response = service.addCard(customerId, req);

            assertThat(response.isPreferred()).isTrue();
        }

        @Test
        @DisplayName("definir preferencial remove dos demais")
        void preferredClearsOthers() {
            CreditCard other = CreditCard.builder()
                    .id(UUID.randomUUID())
                    .customer(customer)
                    .cardholderName("A")
                    .cardNumber("4222222222222222")
                    .brand("VISA")
                    .expirationMonth(12)
                    .expirationYear(2030)
                    .preferred(true)
                    .active(true)
                    .build();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(creditCardRepository.countByCustomer_IdAndActiveTrue(customerId)).thenReturn(1L);
            when(creditCardRepository.findByCustomer_IdAndPreferredTrueAndActiveTrue(customerId)).thenReturn(List.of(other));
            when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> {
                CreditCard c = inv.getArgument(0);
                c.setId(cardId);
                return c;
            });
            when(creditCardRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            CreditCardCreateRequest req = CreditCardCreateRequest.builder()
                    .cardholderName("Novo")
                    .cardNumber("4111111111111111")
                    .brand("VISA")
                    .expirationMonth(12)
                    .expirationYear(2030)
                    .preferred(true)
                    .build();

            service.addCard(customerId, req);

            verify(creditCardRepository).saveAll(any());
        }

        @Test
        @DisplayName("setPreferred em cartão inexistente lança exceção")
        void setPreferred_notFound() {
            when(creditCardRepository.findByIdAndCustomer_Id(cardId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setPreferred(customerId, cardId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
