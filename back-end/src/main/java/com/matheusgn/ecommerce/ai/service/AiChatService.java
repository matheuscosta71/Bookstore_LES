package com.matheusgn.ecommerce.ai.service;

import com.matheusgn.ecommerce.ai.client.AiProviderClient;
import com.matheusgn.ecommerce.ai.dto.ChatRequest;
import com.matheusgn.ecommerce.ai.dto.ChatResponse;
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
            Você é um chatbot de uma livraria em português. Responda com clareza, \
            ajudando em buscas, dúvidas e sugestões de livros.""";

    private final AiProviderClient aiProviderClient;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest request) {
        StringBuilder ctx = new StringBuilder();
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
        ctx.append("\nMensagem do usuário: ").append(request.getMessage());
        String reply = aiProviderClient.complete(SYSTEM, ctx.toString());
        return ChatResponse.builder().reply(reply).build();
    }
}
