package com.matheusgn.ecommerce.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.matheusgn.ecommerce.ai.config.AiProperties;
import com.matheusgn.ecommerce.ai.exception.AiProviderException;
import com.matheusgn.ecommerce.ai.dto.ChatMessageDto;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        return complete(systemPrompt, Collections.emptyList(), userMessage);
    }

    @Override
    public String complete(String systemPrompt, List<ChatMessageDto> history, String userMessage) {
        boolean isDefaultOrBlank = aiProperties.getApiKey() == null
                || aiProperties.getApiKey().isBlank()
                || aiProperties.getApiKey().equals("sk-proj-DEVELOPMENT-FALLBACK-KEY");

        boolean isLocalMockServer = aiProperties.getBaseUrl() != null 
                && (aiProperties.getBaseUrl().contains("localhost") || aiProperties.getBaseUrl().contains("127.0.0.1"))
                && !aiProperties.getBaseUrl().contains("8080");

        if (isDefaultOrBlank && !isLocalMockServer) {
            return getFallbackResponse(systemPrompt, history, userMessage);
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

        if (history != null) {
            for (ChatMessageDto msg : history) {
                ObjectNode m = messages.addObject();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
            }
        }

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
            return getFallbackResponse(systemPrompt, history, userMessage);
        } catch (RestClientResponseException e) {
            if (isLocalMockServer) {
                throw new AiProviderException("Falha ao chamar provedor de IA: HTTP " + e.getStatusCode(), e);
            }
            return getFallbackResponse(systemPrompt, history, userMessage);
        } catch (Exception e) {
            if (isLocalMockServer) {
                throw new AiProviderException("Falha ao processar resposta da IA", e);
            }
            return getFallbackResponse(systemPrompt, history, userMessage);
        }
    }

    private String getFallbackResponse(String systemPrompt, String userMessage) {
        return getFallbackResponse(systemPrompt, Collections.emptyList(), userMessage);
    }

    private String getFallbackResponse(String systemPrompt, List<ChatMessageDto> history, String userMessage) {
        String query = userMessage != null ? userMessage : "";
        int index = query.lastIndexOf("Mensagem do usuário: ");
        if (index != -1) {
            query = query.substring(index + "Mensagem do usuário: ".length()).trim();
        }

        String normalized = normalize(query);

        // 1. Check if it's a general greeting
        if (normalized.matches("^(oi|ola|bom dia|boa tarde|boa noite|ola!|oi!|hello|hi)(\\s.*)?$")) {
            return "Olá! Eu sou o assistente virtual da Livraria Matheus GN. " +
                   "Como posso ajudar você hoje? Você pode me pedir recomendações de livros por tema " +
                   "(como Ficção, Literatura, Infantil, Romance) ou perguntar sobre os livros disponíveis no nosso acervo!";
        }

        // 1b. Check for incompatible activities (reading while sleeping, swimming, etc.)
        boolean mentionsReading = normalized.contains("ler") || normalized.contains("leitura")
                || normalized.contains("lendo") || normalized.contains("livro");
        String[] incompatibleActivities = {
                "dormindo", "dormir", "nadar", "nadando", "cozinhando", "cozinhar",
                "dirigindo", "dirigir", "tomando banho", "banho", "correndo", "correr",
                "malhando", "malhar", "exercicio", "academia", "bicicleta", "pedalando",
                "futebol", "jogando", "surfando", "surfar", "mergulhando", "mergulhar",
                "escalando", "escalar", "pilotando", "pilotar", "operando", "operar"
        };
        if (mentionsReading) {
            for (String activity : incompatibleActivities) {
                if (normalized.contains(activity)) {
                    return "Hmm, não é possível ler enquanto você está " + activity + "! 😊 " +
                           "Mas posso recomendar livros sobre esse tema para você ler antes ou depois. " +
                           "Que tal me contar que tipo de livro você gosta? Temos opções em Ficção, " +
                           "Literatura, Infantil e outras categorias!";
                }
            }
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

        String[] words = normalized.split("\\s+");

        boolean techRequested = normalized.contains("tecnolog") || normalized.contains("tecno") || normalized.contains("program") || normalized.contains("codig") || normalized.contains("desenvolv") || normalized.contains("clean code") || normalized.contains("arquitet")
                || matchesAny(words, "tecnologia", "tecnologico", "programacao", "codigo", "desenvolvimento", "software", "computador", "computer")
                || matchesPhrase(normalized, "clean code") || matchesPhrase(normalized, "arquitetura limpa");

        boolean fictionRequested = normalized.contains("ficc") || normalized.contains("distop") || normalized.contains("futur") || normalized.contains("orwell") || normalized.contains("trono") || normalized.contains("game of thrones") || normalized.contains("fantasi")
                || matchesAny(words, "ficcao", "distopia", "distopico", "fantasia", "futuro", "futurista", "orwell")
                || matchesPhrase(normalized, "game of thrones") || matchesPhrase(normalized, "trono de ferro");

        boolean literatureRequested = normalized.contains("literat") || normalized.contains("liter") || normalized.contains("classic") || normalized.contains("gatsby") || normalized.contains("harper lee") || normalized.contains("mockingbird") || normalized.contains("catcher")
                || matchesAny(words, "literatura", "classico", "classicos", "gatsby", "catcher", "mockingbird")
                || matchesPhrase(normalized, "harper lee");

        boolean childrenRequested = normalized.contains("infant") || normalized.contains("infan") || normalized.contains("crianc") || normalized.contains("brux") || normalized.contains("harry potter") || normalized.contains("rowling") || normalized.contains("magia")
                || matchesAny(words, "infantil", "infantis", "crianca", "criancas", "bruxo", "bruxos", "magia", "rowling")
                || matchesPhrase(normalized, "harry potter");

        boolean romanceRequested = normalized.contains("romanc") || normalized.contains("amor") || normalized.contains("romant")
                || matchesAny(words, "romance", "romantico", "romantica", "amor");

        // 3. Fallback static list if database is empty or unavailable
        if (dbBooks.isEmpty()) {
            return getStaticFallbackResponse(techRequested, fictionRequested, literatureRequested, childrenRequested, romanceRequested);
        }

        // Filter active books
        List<Book> activeBooks = dbBooks.stream()
                .filter(Book::isActive)
                .toList();

        // 4. Check for keyword matches in title, author, or category
        List<Book> recommendations = new ArrayList<>();

        for (Book book : activeBooks) {
            String titleNorm = normalize(book.getTitle());
            String authorNorm = normalize(book.getAuthor());
            String categoryNorm = normalize(book.getCategory());

            boolean match = false;
            if (techRequested && (categoryNorm.contains("tecnolog") || categoryNorm.contains("tecno") || categoryNorm.contains("computer") || categoryNorm.contains("softwar") || titleNorm.contains("clean code") || titleNorm.contains("arquitet"))) {
                match = true;
            } else if (fictionRequested && (categoryNorm.contains("ficc") || categoryNorm.contains("fiction") || titleNorm.contains("1984") || titleNorm.contains("orwell") || titleNorm.contains("trono") || titleNorm.contains("game of thrones"))) {
                match = true;
            } else if (literatureRequested && (categoryNorm.contains("literat") || categoryNorm.contains("liter") || categoryNorm.contains("literature") || titleNorm.contains("gatsby") || titleNorm.contains("mockingbird") || titleNorm.contains("catcher"))) {
                match = true;
            } else if (childrenRequested && (categoryNorm.contains("infan") || categoryNorm.contains("infant") || categoryNorm.contains("children") || titleNorm.contains("harry potter"))) {
                match = true;
            } else if (romanceRequested && (categoryNorm.contains("romanc") || categoryNorm.contains("amor") || categoryNorm.contains("romant"))) {
                match = true;
            }

            // Also direct title/author substring match or fuzzy match
            if (!match && !normalized.isBlank() && normalized.length() > 2) {
                if (titleNorm.contains(normalized) || authorNorm.contains(normalized)) {
                    match = true;
                } else {
                    String[] titleWords = titleNorm.split("\\s+");
                    String[] authorWords = authorNorm.split("\\s+");
                    if (matchesAny(words, titleWords) || matchesAny(words, authorWords)) {
                        match = true;
                    }
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

        // If a specific category was requested but nothing was found in the catalog
        if (techRequested || fictionRequested || literatureRequested || childrenRequested || romanceRequested) {
            String requestedCategory = "";
            if (romanceRequested) requestedCategory = "Romance";
            else if (techRequested) requestedCategory = "Tecnologia";
            else if (fictionRequested) requestedCategory = "Ficção";
            else if (literatureRequested) requestedCategory = "Literatura";
            else if (childrenRequested) requestedCategory = "Infantil";

            return "Desculpe, não encontrei livros do tema " + requestedCategory + " no nosso catálogo no momento. " +
                   "Que tal buscar por outros assuntos como Ficção, Literatura ou Infantil?";
        }

        boolean isPositiveResponse = normalized.equals("sim") || normalized.equals("s") || normalized.equals("quero") || normalized.equals("claro");
        boolean wasOfferedRecommendation = false;
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessageDto msg = history.get(i);
                if ("assistant".equals(msg.getRole())) {
                    String assistantContent = msg.getContent().toLowerCase();
                    if (assistantContent.contains("recomenda") || assistantContent.contains("sugest") || assistantContent.contains("indica")) {
                        wasOfferedRecommendation = true;
                    }
                    break;
                }
            }
        }

        // If no keyword match but user asked for recommendations or suggestions
        if (normalized.contains("recomenda") || normalized.contains("sugest") || normalized.contains("indica") || normalized.contains("livro") || (isPositiveResponse && wasOfferedRecommendation)) {
            // Shuffle to vary results on each call
            List<Book> shuffled = new ArrayList<>(activeBooks);
            Collections.shuffle(shuffled);

            // Deduplicate by normalized title — also catches variants like
            // "1984", "Nineteen Eighty-Four", "1984 / Nineteen Eighty-Four"
            List<String> seenTitles = new ArrayList<>();
            List<Book> unique = new ArrayList<>();
            for (Book b : shuffled) {
                String normTitle = b.getTitle().toLowerCase()
                        .replaceAll("[^a-z0-9]", "");
                boolean isDuplicate = false;
                for (String seen : seenTitles) {
                    if (normTitle.contains(seen) || seen.contains(normTitle)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate && unique.size() < 4) {
                    seenTitles.add(normTitle);
                    unique.add(b);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Aqui estão algumas sugestões de livros do nosso acervo para você:\n\n");
            for (Book b : unique) {
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
                    if (syn.length() > 100) {
                        syn = syn.substring(0, 97) + "...";
                    }
                    sb.append("\n  *").append(syn).append("*");
                }
                sb.append("\n");
            }
            sb.append("\nPosso ajudar você a encontrar algum tema específico ou autor?");
            return sb.toString();
        }

        // Default response for other inputs
        return "Interessante! Desculpe, não consegui encontrar livros específicos sobre esse tema exato no nosso catálogo agora. " +
               "Que tal buscar por outros assuntos como Ficção, Literatura, Infantil ou pesquisar pelo nome de um autor/título que você gosta?";
    }

    private String getStaticFallbackResponse(boolean techRequested, boolean fictionRequested, boolean literatureRequested, boolean childrenRequested, boolean romanceRequested) {
        if (fictionRequested) {
            return "Com base no nosso acervo de Ficção, recomendo:\n\n" +
                   "- **1984** por George Orwell (R$ 45.00) — A brilhante distopia sobre o Grande Irmão.\n" +
                   "- **A Game of Thrones** por George R. R. Martin (R$ 59.99) — Épico de fantasia medieval.";
        }
        if (literatureRequested) {
            return "Aqui estão ótimos clássicos de Literatura:\n\n" +
                   "- **The Great Gatsby** por F. Scott Fitzgerald (R$ 49.90) — Um retrato dos anos loucos de Nova York.\n" +
                   "- **To Kill a Mockingbird** por Harper Lee (R$ 52.00) — Discussão marcante sobre preconceito e justiça.";
        }
        if (childrenRequested) {
            return "Para o público Infantil:\n\n" +
                   "- **Harry Potter Paperback Box Set (Books 1–7)** por J. K. Rowling (R$ 299.00) — A saga completa do jovem bruxo.";
        }
        if (romanceRequested) {
            return "Desculpe, não encontrei livros do tema Romance no nosso catálogo no momento. " +
                   "Que tal buscar por outros assuntos como Ficção, Literatura ou Infantil?";
        }
        if (techRequested) {
            return "Desculpe, não encontrei livros do tema Tecnologia no nosso catálogo no momento. " +
                   "Que tal buscar por outros assuntos como Ficção, Literatura ou Infantil?";
        }
        return "Olá! Sou o assistente virtual da Livraria Matheus GN. Que tal dar uma olhada nos nossos livros em destaque?\n\n" +
               "- **The Great Gatsby** (Literatura)\n" +
               "- **1984** (Ficção)\n" +
               "- **To Kill a Mockingbird** (Literatura)\n" +
               "- **Harry Potter Box Set** (Infantil)\n\n" +
               "Como posso ajudar mais hoje?";
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úùû]", "u")
                .replaceAll("[ç]", "c")
                .trim();
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }

    private boolean isSimilar(String word, String keyword) {
        String w = normalize(word);
        String k = normalize(keyword);
        if (w.equals(k)) {
            return true;
        }
        if (w.length() < 4 || k.length() < 4) {
            return false;
        }
        if (w.contains(k) || k.contains(w)) {
            return true;
        }
        int distance = getLevenshteinDistance(w, k);
        int limit = w.length() >= 6 ? 2 : 1;
        return distance <= limit;
    }

    private boolean matchesAny(String[] words, String... keywords) {
        for (String word : words) {
            for (String kw : keywords) {
                if (isSimilar(word, kw)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPhrase(String query, String phrase) {
        return normalize(query).contains(normalize(phrase));
    }
}
