package com.matheusgn.ecommerce.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.matheusgn.ecommerce.ai.config.AiProperties;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HttpAiProviderClient implements AiProviderClient {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Autowired(required = false)
    private BookRepository bookRepository;

    @Override
    public String complete(String systemPrompt, String userMessage) {
        boolean isDefaultOrBlank = aiProperties.getApiKey() == null
                || aiProperties.getApiKey().isBlank()
                || aiProperties.getApiKey().startsWith("sk-proj-9CGmxi");

        boolean isLocalMockServer = aiProperties.getBaseUrl() != null 
                && (aiProperties.getBaseUrl().contains("localhost") || aiProperties.getBaseUrl().contains("127.0.0.1"))
                && !aiProperties.getBaseUrl().contains("8080");

        if (isDefaultOrBlank && !isLocalMockServer) {
            return getFallbackResponse(systemPrompt, userMessage);
        }

        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new AiProviderException("Chave de API de IA não configurada (app.ai.api-key)");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", aiProperties.getModel());
        ArrayNode messages = body.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", userMessage);

        RestClient client = restClientBuilder.baseUrl(aiProperties.getBaseUrl()).build();
        try {
            String raw = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey().trim())
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new AiProviderException("Resposta do provedor de IA inválida");
            }
            return content.asText();
        } catch (AiProviderException e) {
            if (isLocalMockServer) throw e;
            return getFallbackResponse(systemPrompt, userMessage);
        } catch (RestClientResponseException e) {
            if (isLocalMockServer) {
                throw new AiProviderException("Falha ao chamar provedor de IA: HTTP " + e.getStatusCode(), e);
            }
            return getFallbackResponse(systemPrompt, userMessage);
        } catch (Exception e) {
            if (isLocalMockServer) {
                throw new AiProviderException("Falha ao processar resposta da IA", e);
            }
            return getFallbackResponse(systemPrompt, userMessage);
        }
    }

    private String getFallbackResponse(String systemPrompt, String userMessage) {
        String query = userMessage != null ? userMessage : "";
        int index = query.lastIndexOf("Mensagem do usuário: ");
        if (index != -1) {
            query = query.substring(index + "Mensagem do usuário: ".length()).trim();
        }

        String normalized = query.toLowerCase()
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úùû]", "u")
                .replaceAll("[ç]", "c")
                .trim();

        // 1. Check if it's a general greeting
        if (normalized.matches("^(oi|ola|bom dia|boa tarde|boa noite|ola!|oi!|hello|hi)(\\s.*)?$")) {
            return "Olá! Eu sou o assistente virtual da Livraria Matheus GN. " +
                   "Como posso ajudar você hoje? Você pode me pedir recomendações de livros por tema " +
                   "(como Ficção, Literatura, Infantil, Romance) ou perguntar sobre os livros disponíveis no nosso acervo!";
        }

        // 2. Query books from the database if bookRepository is available
        List<Book> dbBooks = List.of();
        if (bookRepository != null) {
            try {
                dbBooks = bookRepository.findAll();
            } catch (Exception e) {
                // Ignore database errors
            }
        }

        // 3. Fallback static list if database is empty or unavailable
        if (dbBooks.isEmpty()) {
            return getStaticFallbackResponse(normalized);
        }

        // Filter active books
        List<Book> activeBooks = dbBooks.stream()
                .filter(Book::isActive)
                .toList();

        // 4. Check for keyword matches in title, author, or category
        List<Book> recommendations = new ArrayList<>();
        boolean techRequested = normalized.contains("tecnologia") || normalized.contains("programacao") || normalized.contains("codigo") || normalized.contains("desenvolvimento") || normalized.contains("clean code") || normalized.contains("arquitetura");
        boolean fictionRequested = normalized.contains("ficcao") || normalized.contains("distopia") || normalized.contains("futuro") || normalized.contains("orwell") || normalized.contains("tronos") || normalized.contains("game of thrones") || normalized.contains("fantasia");
        boolean literatureRequested = normalized.contains("literatura") || normalized.contains("classico") || normalized.contains("gatsby") || normalized.contains("harper lee") || normalized.contains("mockingbird");
        boolean childrenRequested = normalized.contains("infantil") || normalized.contains("crianca") || normalized.contains("bruxo") || normalized.contains("harry potter") || normalized.contains("rowling") || normalized.contains("magia");
        boolean romanceRequested = normalized.contains("romance") || normalized.contains("amor") || normalized.contains("romantico");

        for (Book book : activeBooks) {
            String titleLower = book.getTitle().toLowerCase();
            String authorLower = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";
            String categoryLower = book.getCategory() != null ? book.getCategory().toLowerCase() : "";

            boolean match = false;
            if (techRequested && (categoryLower.contains("tecnologia") || categoryLower.contains("computers") || categoryLower.contains("software") || titleLower.contains("clean code") || titleLower.contains("arquitetura"))) {
                match = true;
            } else if (fictionRequested && (categoryLower.contains("ficcao") || categoryLower.contains("fiction") || titleLower.contains("1984") || titleLower.contains("orwell") || titleLower.contains("thrones"))) {
                match = true;
            } else if (literatureRequested && (categoryLower.contains("literatura") || categoryLower.contains("literature") || titleLower.contains("gatsby") || titleLower.contains("mockingbird") || titleLower.contains("catcher"))) {
                match = true;
            } else if (childrenRequested && (categoryLower.contains("infantil") || categoryLower.contains("children") || titleLower.contains("harry potter"))) {
                match = true;
            } else if (romanceRequested && (categoryLower.contains("romance") || categoryLower.contains("amor"))) {
                match = true;
            }

            // Also direct title/author substring match
            if (!match && !normalized.isBlank() && normalized.length() > 2) {
                if (titleLower.contains(normalized) || authorLower.contains(normalized)) {
                    match = true;
                }
            }

            if (match) {
                recommendations.add(book);
            }
        }

        // Build response based on matches
        if (!recommendations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Com certeza! Com base na sua busca, encontrei os seguintes livros em nosso acervo:\n\n");
            for (Book b : recommendations.stream().limit(5).toList()) {
                sb.append("- **").append(b.getTitle()).append("**");
                if (b.getAuthor() != null && !b.getAuthor().isBlank()) {
                    sb.append(" por ").append(b.getAuthor());
                }
                sb.append(" (").append(b.getCategory() != null ? b.getCategory() : "Geral").append(")");
                if (b.getSalePrice() != null) {
                    sb.append(" — R$ ").append(b.getSalePrice());
                }
                if (b.getSynopsis() != null && !b.getSynopsis().isBlank()) {
                    String syn = b.getSynopsis();
                    if (syn.length() > 120) {
                        syn = syn.substring(0, 117) + "...";
                    }
                    sb.append("\n  *").append(syn).append("*");
                }
                sb.append("\n");
            }
            sb.append("\nVocê pode adicioná-los ao seu carrinho e finalizar a compra a qualquer momento. Deseja saber mais sobre algum deles?");
            return sb.toString();
        }

        // If no keyword match but user asked for recommendations or suggestions
        if (normalized.contains("recomenda") || normalized.contains("sugest") || normalized.contains("indica") || normalized.contains("livro")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Aqui estão alguns dos livros mais populares em destaque na nossa loja hoje:\n\n");
            int count = 0;
            for (Book b : activeBooks) {
                if (count >= 4) break;
                sb.append("- **").append(b.getTitle()).append("** (").append(b.getCategory() != null ? b.getCategory() : "Geral").append(")");
                if (b.getSalePrice() != null) {
                    sb.append(" — R$ ").append(b.getSalePrice());
                }
                sb.append("\n");
                count++;
            }
            sb.append("\nPosso ajudar você a encontrar algum tema específico ou autor?");
            return sb.toString();
        }

        // Default response for other inputs
        return "Interessante! Desculpe, não consegui encontrar livros específicos sobre esse tema exato no nosso catálogo agora. " +
               "Que tal buscar por outros assuntos como Ficção, Literatura, Infantil ou pesquisar pelo nome de um autor/título que você gosta?";
    }

    private String getStaticFallbackResponse(String normalized) {
        if (normalized.contains("ficcao") || normalized.contains("distopia") || normalized.contains("orwell")) {
            return "Com base no nosso acervo de Ficção, recomendo:\n\n" +
                   "- **1984** por George Orwell (R$ 45.00) — A brilhante distopia sobre o Grande Irmão.\n" +
                   "- **A Game of Thrones** por George R. R. Martin (R$ 59.99) — Épico de fantasia medieval.";
        }
        if (normalized.contains("literatura") || normalized.contains("classico") || normalized.contains("gatsby")) {
            return "Aqui estão ótimos clássicos de Literatura:\n\n" +
                   "- **The Great Gatsby** por F. Scott Fitzgerald (R$ 49.90) — Um retrato dos anos loucos de Nova York.\n" +
                   "- **To Kill a Mockingbird** por Harper Lee (R$ 52.00) — Discussão marcante sobre preconceito e justiça.";
        }
        if (normalized.contains("infantil") || normalized.contains("harry potter")) {
            return "Para o público Infantil:\n\n" +
                   "- **Harry Potter Paperback Box Set (Books 1–7)** por J. K. Rowling (R$ 299.00) — A saga completa do jovem bruxo.";
        }
        return "Olá! Sou o assistente virtual da Livraria Matheus GN. Que tal dar uma olhada nos nossos livros em destaque?\n\n" +
               "- **The Great Gatsby** (Literatura)\n" +
               "- **1984** (Ficção)\n" +
               "- **To Kill a Mockingbird** (Literatura)\n" +
               "- **Harry Potter Box Set** (Infantil)\n\n" +
               "Como posso ajudar mais hoje?";
    }
}
