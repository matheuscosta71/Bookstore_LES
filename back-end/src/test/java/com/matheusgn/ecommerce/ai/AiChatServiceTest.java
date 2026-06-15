package com.matheusgn.ecommerce.ai;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.dto.ChatRequest;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import com.matheusgn.ecommerce.ai.service.AiChatService;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private AiProviderClient aiProviderClient;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private AiChatService aiChatService;

    private static Book book(String title) {
        return Book.builder()
                .id(UUID.randomUUID())
                .title(title)
                .category("Ficção")
                .isbn("9781111111111")
                .salePrice(BigDecimal.ONE)
                .stockQuantity(1)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("given só mensagem sem customerId quando chat então contexto mínimo")
    void givenMessageOnly_whenChat_thenMinimalUserContext() {
        when(aiProviderClient.complete(anyString(), contains("Mensagem do usuário: Olá"))).thenReturn("ok");

        var res = aiChatService.chat(ChatRequest.builder().message("Olá").build());

        assertThat(res.getReply()).isEqualTo("ok");
        verify(aiProviderClient).complete(anyString(), contains("Mensagem do usuário: Olá"));
    }

    @Test
    @DisplayName("given customerId com histórico e feedback quando chat então contexto enriquecido")
    void givenCustomerContext_whenChat_thenIncludesPurchasesAndFeedback() {
        UUID cid = UUID.randomUUID();
        Customer c = mock(Customer.class);
        when(c.getFullName()).thenReturn("Ana");
        when(customerRepository.findById(cid)).thenReturn(Optional.of(c));
        OrderItem oi = OrderItem.builder()
                .id(UUID.randomUUID())
                .book(book("Livro A"))
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.TEN)
                .exchangeRequested(false)
                .build();
        when(orderItemRepository.findRecentForCustomer(cid, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(List.of(oi));
        Feedback fb = Feedback.builder()
                .id(UUID.randomUUID())
                .customer(c)
                .book(book("Livro B"))
                .rating(4)
                .build();
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(cid)).thenReturn(List.of(fb));
        when(aiProviderClient.complete(anyString(), contains("Livro A"))).thenReturn("resposta");

        var res = aiChatService.chat(ChatRequest.builder().customerId(cid).message("Sugestões?").build());

        assertThat(res.getReply()).isEqualTo("resposta");
        verify(aiProviderClient).complete(anyString(), contains("Cliente: Ana"));
        verify(aiProviderClient).complete(anyString(), contains("Feedbacks recentes:"));
    }

    @Test
    @DisplayName("given customerId inexistente quando chat então ignora contexto e só envia mensagem")
    void givenUnknownCustomer_whenChat_thenOnlyUserMessageInContext() {
        UUID cid = UUID.randomUUID();
        when(customerRepository.findById(cid)).thenReturn(Optional.empty());
        when(aiProviderClient.complete(anyString(), contains("Mensagem do usuário: teste"))).thenReturn("x");

        aiChatService.chat(ChatRequest.builder().customerId(cid).message("teste").build());

        verify(aiProviderClient).complete(anyString(), contains("Mensagem do usuário: teste"));
    }

    @Test
    @DisplayName("given cliente sem compras e sem feedback quando chat então só nome e mensagem")
    void givenCustomerNoHistory_whenChat_thenNameAndMessageOnly() {
        UUID cid = UUID.randomUUID();
        Customer c = mock(Customer.class);
        when(c.getFullName()).thenReturn("Pedro");
        when(customerRepository.findById(cid)).thenReturn(Optional.of(c));
        when(orderItemRepository.findRecentForCustomer(cid, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(List.of());
        when(feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(cid)).thenReturn(List.of());
        when(aiProviderClient.complete(anyString(), contains("Mensagem do usuário: oi"))).thenReturn("ok");

        aiChatService.chat(ChatRequest.builder().customerId(cid).message("oi").build());

        verify(aiProviderClient).complete(anyString(), contains("Cliente: Pedro"));
    }

    @Test
    @DisplayName("given provedor retorna vazio quando chat então reply vazio")
    void givenProviderEmpty_whenChat_thenEmptyReply() {
        when(aiProviderClient.complete(anyString(), anyString())).thenReturn("");

        var res = aiChatService.chat(ChatRequest.builder().message("x").build());

        assertThat(res.getReply()).isEmpty();
    }

    @Test
    @DisplayName("given provedor lança AiProviderException quando chat então propaga")
    void givenProviderFails_whenChat_thenPropagates() {
        when(aiProviderClient.complete(anyString(), anyString()))
                .thenThrow(new AiProviderException("indisponível"));

        assertThatThrownBy(() -> aiChatService.chat(ChatRequest.builder().message("x").build()))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    @DisplayName("deve usar system prompt de chatbot de livraria")
    void shouldSendFixedSystemPrompt() {
        when(aiProviderClient.complete(anyString(), anyString())).thenReturn("y");
        ArgumentCaptor<String> sys = ArgumentCaptor.forClass(String.class);

        aiChatService.chat(ChatRequest.builder().message("z").build());

        verify(aiProviderClient).complete(sys.capture(), anyString());
        assertThat(sys.getValue()).contains("assistente").contains("Livraria");
    }
}
