package com.matheusgn.ecommerce.book.service;

import com.matheusgn.ecommerce.book.dto.BookCreateRequest;
import com.matheusgn.ecommerce.book.dto.BookMapper;
import com.matheusgn.ecommerce.book.dto.BookResponse;
import com.matheusgn.ecommerce.book.dto.BookUpdateRequest;
import com.matheusgn.ecommerce.book.dto.PatchActiveRequest;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.entity.BookLifecycleReason;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.repository.BookSpecifications;
import com.matheusgn.ecommerce.config.BookBusinessProperties;
import com.matheusgn.ecommerce.config.PageConstraints;
import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.inventory.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final String AUTO_INACTIVATION_JUSTIFICATION =
            "Inativação automática: estoque zero e vendas abaixo do mínimo parametrizado.";

    private final BookRepository bookRepository;
    private final PricingGroupRepository pricingGroupRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final BookCodeGeneratorService bookCodeGeneratorService;
    private final AuditLogService auditLogService;
    private final PricingService pricingService;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final IsbnExternalLookupService isbnExternalLookupService;
    private final BookBusinessProperties bookBusinessProperties;

    @Transactional
    public BookResponse create(BookCreateRequest request) {
        isbnExternalLookupService.assertExistsInOfficialCatalogOrThrow(request.getIsbn());
        if (bookRepository.existsByBarcode(request.getBarcode().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um livro com este código de barras.");
        }

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor não encontrado"));
        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Editora não encontrada"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado"));
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
        if (categories.size() != request.getCategoryIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais categorias não foram encontradas");
        }

        Book book = BookMapper.toEntity(request);
        book.setPricingGroup(resolvePricingGroup(request.getPricingGroupId()));
        book.setAuthorRef(author);
        book.setPublisher(publisher);
        book.setSupplier(supplier);
        book.setCategories(categories);
        book.setAuthor(author.getName());
        syncLegacyCategoryFields(book);

        book.setCode(bookCodeGeneratorService.nextCode());
        Book saved = bookRepository.save(book);
        inventoryBalanceService.initializeInventoryForBook(saved.getId(), saved.getStockQuantity());
        pricingService.applyAutomaticSalePriceIfEligible(saved.getId());
        saved = bookRepository.findByIdWithCategories(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));
        BookResponse response = BookMapper.toResponse(saved);
        auditLogService.logCreate("Book", saved.getId(), response);
        return response;
    }

    private PricingGroup resolvePricingGroup(UUID pricingGroupId) {
        return pricingGroupRepository.findById(pricingGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo de precificação não encontrado"));
    }

    private void syncLegacyCategoryFields(Book book) {
        if (book.getCategories() == null || book.getCategories().isEmpty()) {
            book.setCategory(null);
            book.setCategoryRef(null);
            return;
        }
        Category first = book.getCategories().iterator().next();
        book.setCategory(first.getName());
        book.setCategoryRef(first);
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> findByFilters(
            String title,
            String author,
            String category,
            String isbn,
            String code,
            boolean includeInactive,
            Pageable pageable) {
        Pageable p = PageConstraints.clamp(pageable);
        Specification<Book> spec = BookSpecifications.withFilters(title, author, category, isbn, code, includeInactive);
        return bookRepository.findAll(spec, p).map(BookMapper::toResponse);
    }

    @Transactional
    public void inactivateBooksAutomatically(BigDecimal minimumSalesValue) {
        List<Book> candidates = bookRepository.findByStockQuantity(0);
        for (Book book : candidates) {
            BigDecimal maxSale = book.getMaxSaleValue();
            boolean hasRelevantSale = maxSale != null && maxSale.compareTo(minimumSalesValue) >= 0;
            if (!hasRelevantSale) {
                book.setActive(false);
                book.setLastLifecycleReason(BookLifecycleReason.FORA_DE_MERCADO);
                book.setLastLifecycleJustification(AUTO_INACTIVATION_JUSTIFICATION);
            }
        }
        if (!candidates.isEmpty()) {
            bookRepository.saveAll(candidates);
        }
    }

    @Transactional(readOnly = true)
    public BookResponse findById(UUID id, boolean includeInactive) {
        Book book = bookRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));
        if (!includeInactive && !book.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado");
        }
        return BookMapper.toResponse(book);
    }

    @Transactional
    public BookResponse update(UUID id, BookUpdateRequest request, String salesManagerKeyHeader) {
        Book book = bookRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));

        String prevIsbn = book.getIsbn();
        if (!prevIsbn.trim().equals(request.getIsbn().trim())) {
            isbnExternalLookupService.assertExistsInOfficialCatalogOrThrow(request.getIsbn());
        }

        if (bookRepository.existsByBarcodeAndIdNot(request.getBarcode().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um livro com este código de barras.");
        }

        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autor não encontrado"));
        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Editora não encontrada"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado"));
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
        if (categories.size() != request.getCategoryIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais categorias não foram encontradas");
        }

        book.setPricingGroup(resolvePricingGroup(request.getPricingGroupId()));
        book.setAuthorRef(author);
        book.setPublisher(publisher);
        book.setSupplier(supplier);
        book.setCategories(categories);
        book.setAuthor(author.getName());
        BookMapper.applyUpdate(book, request);
        syncLegacyCategoryFields(book);

        assertPriceWithinMarginOrManagerAuthorized(book, request.getPrice(), salesManagerKeyHeader);

        inventoryBalanceService.syncQuantityFromBook(book.getId(), book.getStockQuantity());
        Book saved = bookRepository.save(book);
        pricingService.applyAutomaticSalePriceIfEligible(saved.getId());
        saved = bookRepository.findByIdWithCategories(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));
        auditLogService.logUpdate("Book", id, request);
        return BookMapper.toResponse(saved);
    }

    private void assertPriceWithinMarginOrManagerAuthorized(Book book, BigDecimal requestedPrice, String salesManagerKey) {
        pricingService.minimumSalePriceForBook(book).ifPresent(min -> {
            if (requestedPrice.compareTo(min) < 0) {
                if (!bookBusinessProperties.matchesSalesManagerKey(salesManagerKey)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Preço abaixo da margem mínima definida pelo grupo de precificação; "
                                    + "é necessária autorização do gerente de vendas (header X-Sales-Manager-Key).");
                }
            }
        });
    }

    @Transactional
    public void delete(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado");
        }
        inventoryRepository.deleteByBook_Id(id);
        bookRepository.deleteById(id);
    }

    @Transactional
    public BookResponse setActive(UUID id, PatchActiveRequest request) {
        Book book = bookRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));

        boolean target = Boolean.TRUE.equals(request.getActive());
        if (book.isActive() == target) {
            return BookMapper.toResponse(book);
        }

        if (!StringUtils.hasText(request.getJustification()) || request.getReason() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Justificativa e motivo são obrigatórios ao alterar ativação do livro.");
        }

        if (target) {
            validateActivationReason(request.getReason());
            isbnExternalLookupService.assertExistsInOfficialCatalogOrThrow(book.getIsbn());
        } else {
            validateManualInactivationReason(request.getReason());
        }

        book.setActive(target);
        book.setLastLifecycleReason(request.getReason());
        book.setLastLifecycleJustification(request.getJustification().trim());
        bookRepository.save(book);
        Book reloaded = bookRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livro não encontrado"));
        return BookMapper.toResponse(reloaded);
    }

    private void validateActivationReason(BookLifecycleReason r) {
        if (r == BookLifecycleReason.FORA_DE_MERCADO
                || r == BookLifecycleReason.BAIXA_ROTACAO
                || r == BookLifecycleReason.CONTEUDO_DESATUALIZADO
                || r == BookLifecycleReason.OUTRA_INATIVACAO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Motivo inválido para ativação; use RETORNO_ESTOQUE, DEMANDA_RENOVADA ou OUTRA_ATIVACAO.");
        }
        if (r != BookLifecycleReason.RETORNO_ESTOQUE
                && r != BookLifecycleReason.DEMANDA_RENOVADA
                && r != BookLifecycleReason.OUTRA_ATIVACAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de ativação inválido.");
        }
    }

    private void validateManualInactivationReason(BookLifecycleReason r) {
        if (r == BookLifecycleReason.FORA_DE_MERCADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A categoria FORA_DE_MERCADO é reservada à inativação automática (RN0016).");
        }
        if (r == BookLifecycleReason.RETORNO_ESTOQUE
                || r == BookLifecycleReason.DEMANDA_RENOVADA
                || r == BookLifecycleReason.OUTRA_ATIVACAO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Motivo inválido para inativação manual.");
        }
        if (r != BookLifecycleReason.BAIXA_ROTACAO
                && r != BookLifecycleReason.CONTEUDO_DESATUALIZADO
                && r != BookLifecycleReason.OUTRA_INATIVACAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de inativação inválido.");
        }
    }
}
