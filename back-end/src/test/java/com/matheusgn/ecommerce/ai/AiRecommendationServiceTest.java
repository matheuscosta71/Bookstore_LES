package com.matheusgn.ecommerce.ai;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import com.matheusgn.ecommerce.ai.service.AiRecommendationService;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.feedback.entity.Feedback;
import com.matheusgn.ecommerce.feedback.repository.FeedbackRepository;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @Mock
    private AiProviderClient aiProviderClient;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private AiRecommendationService aiRecommendationService;

    private static UUID customerId() {
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    }

    private Customer mockCustomer() {
        Customer c = mock(Customer.class);
        lenient().when(c.getFullName()).thenReturn("João Silva");
        lenient().when(c.getEmail()).thenReturn("joao@livraria.com");
        return c;
    }

    private static Book sampleBook(String title, String category) {
        return Book.builder()
                .id(UUID.randomUUID())
                .title(title)
                .category(category)
                .isbn("9780000000001")
                .salePrice(BigDecimal.TEN)
                .stockQuantity(5)
                .active(true)
                .build();
    }

    private static OrderItem orderItemWithBook(Book book) {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .book(book)
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.TEN)
                .exchangeRequested(false)
                .build();
    }

    @Test
    @DisplayName("given customer e sem histórico quando recommend então contexto mínimo e lista vazia do provedor")
    void givenNoHistory_whenRecommend_thenMinimalContextAndProviderCalled() {
        UUID id = customerId();
        Customer customer = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), contains("Sem histórico de compras recente")))
                .thenReturn("Sugestão");

        var res = aiRecommendationService.recommend(id);

        assertThat(res.getText()).isEqualTo("Sugestão");
        verify(aiProviderClient).complete(anyString(), contains("Sem feedback registrado"));
    }

    @Test
    @DisplayName("given histórico de compras quando recommend então contexto inclui título e categoria do livro")
    void givenPurchaseHistory_whenRecommend_thenContextIncludesTitlesAndCategories() {
        UUID id = customerId();
        Customer customer = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        Book b = sampleBook("Clean Code", "Software");
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(orderItemWithBook(b)));
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), contains("Clean Code (Software)")))
                .thenReturn("Leia também Refactoring");

        var res = aiRecommendationService.recommend(id);

        assertThat(res.getText()).isEqualTo("Leia também Refactoring");
        verify(aiProviderClient).complete(anyString(), contains("Histórico recente de compras"));
    }

    @Test
    @DisplayName("given feedbacks quando recommend então contexto inclui título e nota")
    void givenFeedbacks_whenRecommend_thenContextIncludesFeedbackLines() {
        UUID id = customerId();
        Customer cust = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(cust));
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        Book b = sampleBook("DDD", "Software");
        Feedback fb = Feedback.builder()
                .id(UUID.randomUUID())
                .customer(cust)
                .book(b)
                .rating(5)
                .comment("Excelente")
                .build();
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of(fb));
        when(aiProviderClient.complete(anyString(), contains("DDD nota 5")))
                .thenReturn("ok");

        aiRecommendationService.recommend(id);

        verify(aiProviderClient).complete(anyString(), contains("Feedbacks:"));
        verify(aiProviderClient).complete(anyString(), contains("Excelente"));
    }

    @Test
    @DisplayName("given provedor retorna texto vazio quando recommend então resposta vazia")
    void givenProviderReturnsEmpty_whenRecommend_thenEmptyText() {
        UUID id = customerId();
        Customer customer = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), anyString())).thenReturn("");

        var res = aiRecommendationService.recommend(id);

        assertThat(res.getText()).isEmpty();
    }

    @Test
    @DisplayName("given AiProviderClient lança exceção quando recommend então propaga")
    void givenProviderThrows_whenRecommend_thenPropagates() {
        UUID id = customerId();
        Customer customer = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), anyString()))
                .thenThrow(new AiProviderException("falha"));

        assertThatThrownBy(() -> aiRecommendationService.recommend(id))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("falha");
    }

    @Test
    @DisplayName("deve enviar system prompt fixo de livraria e user com dados do cliente")
    void shouldPassSystemAndUserPrompts() {
        UUID id = customerId();
        Customer customer = mockCustomer();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(orderItemRepository.findRecentForCustomer(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), anyString())).thenReturn("x");

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);

        aiRecommendationService.recommend(id);

        verify(aiProviderClient).complete(systemCaptor.capture(), userCaptor.capture());
        assertThat(systemCaptor.getValue()).contains("livraria");
        assertThat(userCaptor.getValue()).contains("João Silva").contains("joao@livraria.com");
    }
}
