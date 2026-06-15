package com.matheusgn.ecommerce.ai.service;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.dto.RecommendationResponse;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.feedback.entity.Feedback;
import com.matheusgn.ecommerce.feedback.repository.FeedbackRepository;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private static final String SYSTEM = """
            Você é o assistente de recomendações da Livraria Matheus GN. \
            Com base no histórico de compras, categorias preferidas e feedbacks do cliente, \
            sugira de 3 a 5 livros do CATÁLOGO DA LOJA fornecido abaixo. \
            NUNCA invente livros que não estejam no catálogo. \
            NÃO recomende livros que o cliente já comprou. \
            Informe título, autor, categoria e preço (R$). \
            Seja conciso e amigável.""";

    private final AiProviderClient aiProviderClient;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        List<OrderItem> recent = orderItemRepository.findRecentForCustomer(customerId, PageRequest.of(0, 15));
        List<Feedback> feedbacks = feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        String context = buildContext(customer, recent, feedbacks);
        String text = aiProviderClient.complete(SYSTEM, context);
        return RecommendationResponse.builder().text(text).build();
    }

    private String buildContext(Customer customer, List<OrderItem> recent, List<Feedback> feedbacks) {
        StringBuilder sb = new StringBuilder();

        // Inject book catalog
        List<Book> activeBooks = bookRepository.findAll().stream()
                .filter(Book::isActive)
                .toList();
        if (!activeBooks.isEmpty()) {
            sb.append("=== CATÁLOGO DE LIVROS DA LOJA ===\n");
            for (Book b : activeBooks) {
                sb.append("- ").append(b.getTitle());
                if (b.getAuthor() != null && !b.getAuthor().isBlank()) {
                    sb.append(" | Autor: ").append(b.getAuthor());
                }
                if (b.getCategory() != null) {
                    sb.append(" | Categoria: ").append(b.getCategory());
                }
                if (b.getSalePrice() != null) {
                    sb.append(" | Preço: R$ ").append(b.getSalePrice());
                }
                sb.append("\n");
            }
            sb.append("=== FIM DO CATÁLOGO ===\n\n");
        }

        sb.append("Nome: ").append(customer.getFullName()).append("\n");
        sb.append("E-mail: ").append(customer.getEmail()).append("\n");

        if (!recent.isEmpty()) {
            String titles = recent.stream()
                    .map(oi -> oi.getBook().getTitle() + " (" + oi.getBook().getCategory() + ")")
                    .collect(Collectors.joining("; "));
            sb.append("Histórico recente de compras (livros/categorias): ").append(titles).append("\n");
        } else {
            sb.append("Sem histórico de compras recente.\n");
        }

        if (!feedbacks.isEmpty()) {
            sb.append("Feedbacks:\n");
            for (Feedback f : feedbacks.stream().limit(10).toList()) {
                sb.append("- ")
                        .append(f.getBook().getTitle())
                        .append(" nota ")
                        .append(f.getRating());
                if (f.getComment() != null && !f.getComment().isBlank()) {
                    sb.append(" — ").append(f.getComment());
                }
                sb.append("\n");
            }
        } else {
            sb.append("Sem feedback registrado.\n");
        }

        return sb.toString();
    }
}
