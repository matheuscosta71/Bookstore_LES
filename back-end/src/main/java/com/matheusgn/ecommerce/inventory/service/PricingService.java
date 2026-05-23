package com.matheusgn.ecommerce.inventory.service;

import com.matheusgn.ecommerce.book.dto.BookMapper;
import com.matheusgn.ecommerce.book.dto.BookResponse;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final BookRepository bookRepository;

    /**
     * RN0014: preço mínimo permitido pelo grupo = custo × (1 + %/100), quando custo e grupo existem.
     */
    public Optional<BigDecimal> minimumSalePriceForBook(Book book) {
        if (book.getCostPrice() == null) {
            return Optional.empty();
        }
        PricingGroup group = book.getPricingGroup();
        if (group == null || group.getPercentage() == null) {
            return Optional.empty();
        }
        BigDecimal factor = BigDecimal.ONE.add(
                group.getPercentage().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        return Optional.of(book.getCostPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * RF0052: quando custo e grupo com percentual existem, preço de venda = custo × (1 + %/100).
     * Não altera o livro se custo ou grupo/percentual estiverem ausentes.
     */
    @Transactional
    public void applyAutomaticSalePriceIfEligible(UUID bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }
        if (book.getCostPrice() == null) {
            return;
        }
        PricingGroup group = book.getPricingGroup();
        if (group == null || group.getPercentage() == null) {
            return;
        }
        BigDecimal factor = BigDecimal.ONE.add(
                group.getPercentage().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal sale = book.getCostPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        book.setSalePrice(sale);
        bookRepository.save(book);
    }

    @Transactional
    public BookResponse recalculateSalePrice(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
        if (book.getCostPrice() == null) {
            throw new IllegalArgumentException("Custo do livro não definido");
        }
        PricingGroup group = book.getPricingGroup();
        if (group == null) {
            throw new IllegalArgumentException("Grupo de precificação não definido");
        }
        if (group.getPercentage() == null) {
            throw new IllegalArgumentException("Percentual do grupo de precificação não definido");
        }
        BigDecimal factor = BigDecimal.ONE.add(
                group.getPercentage().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal sale = book.getCostPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        book.setSalePrice(sale);
        bookRepository.save(book);
        return BookMapper.toResponse(book);
    }
}
