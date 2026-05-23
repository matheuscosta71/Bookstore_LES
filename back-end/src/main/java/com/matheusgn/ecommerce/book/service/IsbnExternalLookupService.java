package com.matheusgn.ecommerce.book.service;

import com.matheusgn.ecommerce.config.BookBusinessProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * RN0019: valida ISBN contra catálogo público (Open Library).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IsbnExternalLookupService {

    public static final String ISBN_NOT_IN_OFFICIAL_BASE =
            "ISBN não encontrado na base oficial. Verificar digitação.";

    private final BookBusinessProperties bookBusinessProperties;

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(bookBusinessProperties.getIsbnExternal().getConnectTimeoutMs());
        f.setReadTimeout(bookBusinessProperties.getIsbnExternal().getReadTimeoutMs());
        return new RestTemplate(f);
    }

    public static String normalizeIsbnDigits(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("[\\s-]", "").toUpperCase().replaceAll("[^0-9X]", "");
    }

    public void assertExistsInOfficialCatalogOrThrow(String isbn) {
        if (!bookBusinessProperties.getIsbnExternal().isEnabled()) {
            return;
        }
        String n = normalizeIsbnDigits(isbn);
        if (n.isEmpty()) {
            throw new IllegalArgumentException(ISBN_NOT_IN_OFFICIAL_BASE);
        }
        String isbn13 = n.length() == 13 ? n : isbn10ToIsbn13(n);
        if (isbn13.length() != 13) {
            throw new IllegalArgumentException(ISBN_NOT_IN_OFFICIAL_BASE);
        }
        String url = "https://openlibrary.org/isbn/" + isbn13 + ".json";
        try {
            restTemplate().getForEntity(url, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException(ISBN_NOT_IN_OFFICIAL_BASE);
        } catch (Exception e) {
            log.warn("[IsbnExternalLookup] Falha ao consultar ISBN url={} — {}", url, e.toString());
            throw new IllegalStateException("Não foi possível validar o ISBN no serviço externo. Tente novamente.");
        }
    }

    private static String isbn10ToIsbn13(String isbn10) {
        if (isbn10 == null || isbn10.length() != 10) {
            return isbn10 == null ? "" : isbn10;
        }
        String core = isbn10.substring(0, 9);
        String isbn13Body = "978" + core;
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = isbn13Body.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return isbn13Body + check;
    }
}
