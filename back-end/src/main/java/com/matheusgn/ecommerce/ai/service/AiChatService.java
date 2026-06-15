package com.matheusgn.ecommerce.ai.service;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.dto.ChatRequest;
import com.matheusgn.ecommerce.ai.dto.ChatResponse;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.feedback.entity.Feedback;
import com.matheusgn.ecommerce.feedback.repository.FeedbackRepository;
import com.matheusgn.ecommerce.sales.entity.OrderItem;
import com.matheusgn.ecommerce.sales.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String SYSTEM = """
            Você é o assistente virtual da Livraria Matheus GN, uma livraria online em português. \
            Você DEVE responder usando APENAS os livros listados no catálogo fornecido abaixo. \
            NUNCA invente livros que não estejam no catálogo. \
            Quando o usuário perguntar sobre livros disponíveis, recomendações, preços ou se é possível \
            comprar, responda com base no catálogo real da loja. \
            Informe título, autor, categoria e preço (R$). \
            Se o estoque de um livro for 0, avise que está temporariamente indisponível. \
            Seja amigável, conciso e útil. O usuário PODE comprar livros diretamente no site. \
            REGRA IMPORTANTE SOBRE ATIVIDADES INCOMPATÍVEIS COM LEITURA: \
            Se o usuário pedir livros para ler enquanto faz uma atividade que IMPEDE a leitura \
            (por exemplo: dormir, nadar, cozinhar, dirigir, tomar banho, fazer exercício físico, \
            correr, malhar, andar de bicicleta, jogar futebol, ou qualquer atividade que ocupe \
            as mãos, os olhos ou exija atenção total), você DEVE: \
            1) Gentilmente explicar que não é possível ler durante essa atividade. \
            2) Sugerir livros SOBRE o tema da atividade, se houver no catálogo. \
            3) Sugerir que o usuário leia ANTES ou DEPOIS da atividade. \
            Exemplo: se o usuário pedir 'livros para ler nadando', responda algo como: \
            'Não é possível ler enquanto nada, mas posso recomendar livros sobre natação ou \
            esportes aquáticos para você ler antes ou depois do treino!' \
            Nunca sugira que é possível ler durante atividades incompatíveis com leitura.""";

    private final AiProviderClient aiProviderClient;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest request) {
        StringBuilder ctx = new StringBuilder();

        // 1. Inject the store's book catalog
        List<Book> activeBooks = bookRepository.findAll().stream()
                .filter(Book::isActive)
                .toList();

        if (!activeBooks.isEmpty()) {
            ctx.append("=== CATÁLOGO DE LIVROS DA LOJA ===\n");
            for (Book b : activeBooks) {
                ctx.append("- ").append(b.getTitle());
                if (b.getAuthor() != null && !b.getAuthor().isBlank()) {
                    ctx.append(" | Autor: ").append(b.getAuthor());
                }
                if (b.getCategory() != null) {
                    ctx.append(" | Categoria: ").append(b.getCategory());
                }
                if (b.getSalePrice() != null) {
                    ctx.append(" | Preço: R$ ").append(b.getSalePrice());
                }
                if (b.getStockQuantity() != null) {
                    ctx.append(" | Estoque: ").append(b.getStockQuantity()).append(" un.");
                }
                if (b.getSynopsis() != null && !b.getSynopsis().isBlank()) {
                    String syn = b.getSynopsis();
                    if (syn.length() > 150) {
                        syn = syn.substring(0, 147) + "...";
                    }
                    ctx.append(" | Sinopse: ").append(syn);
                }
                ctx.append("\n");
            }
            ctx.append("=== FIM DO CATÁLOGO ===\n\n");
        }

        // 2. Inject customer context if available
        if (request.getCustomerId() != null) {
            Customer c = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (c != null) {
                ctx.append("Cliente: ").append(c.getFullName()).append("\n");
                List<OrderItem> items = orderItemRepository.findRecentForCustomer(
                        request.getCustomerId(), PageRequest.of(0, 10));
                if (!items.isEmpty()) {
                    ctx.append("Compras recentes: ");
                    for (OrderItem oi : items) {
                        ctx.append(oi.getBook().getTitle()).append("; ");
                    }
                    ctx.append("\n");
                }
                List<Feedback> fb = feedbackRepository.findByCustomerIdOrderByCreatedAtDesc(request.getCustomerId());
                if (!fb.isEmpty()) {
                    ctx.append("Feedbacks recentes: ");
                    for (Feedback f : fb.stream().limit(5).toList()) {
                        ctx.append(f.getBook().getTitle()).append("(").append(f.getRating()).append(") ");
                    }
                    ctx.append("\n");
                }
            }
        }

        // 3. Append user message
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            ctx.append("=== HISTÓRICO DA CONVERSA ===\n");
            for (var msg : request.getHistory()) {
                ctx.append("user".equals(msg.getRole()) ? "Usuário: " : "Assistente: ")
                   .append(msg.getContent())
                   .append("\n");
            }
            ctx.append("=== FIM DO HISTÓRICO ===\n\n");
        }

        ctx.append("\nMensagem do usuário: ").append(request.getMessage());

        String reply;
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            reply = aiProviderClient.complete(SYSTEM, request.getHistory(), ctx.toString());
        } else {
            reply = aiProviderClient.complete(SYSTEM, ctx.toString());
        }
        return ChatResponse.builder().reply(reply).build();
    }
}

