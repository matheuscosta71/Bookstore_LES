package com.matheusgn.ecommerce.book;

import com.matheusgn.ecommerce.book.dto.BookCreateRequest;
import com.matheusgn.ecommerce.book.dto.BookResponse;
import com.matheusgn.ecommerce.book.dto.BookUpdateRequest;
import com.matheusgn.ecommerce.book.dto.PatchActiveRequest;
import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.entity.BookLifecycleReason;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.service.BookCodeGeneratorService;
import com.matheusgn.ecommerce.book.service.BookService;
import com.matheusgn.ecommerce.book.service.IsbnExternalLookupService;
import com.matheusgn.ecommerce.config.BookBusinessProperties;
import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import com.matheusgn.ecommerce.inventory.service.InventoryBalanceService;
import com.matheusgn.ecommerce.inventory.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private PricingGroupRepository pricingGroupRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryBalanceService inventoryBalanceService;
    @Mock
    private BookCodeGeneratorService bookCodeGeneratorService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PricingService pricingService;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private IsbnExternalLookupService isbnExternalLookupService;
    @Mock
    private BookBusinessProperties bookBusinessProperties;

    @InjectMocks
    private BookService bookService;

    private UUID bookId;
    private UUID authorId;
    private UUID publisherId;
    private UUID supplierId;
    private UUID categoryId;
    private UUID pricingGroupId;
    private Book sampleBook;
    private PricingGroup pricingGroup;

    @BeforeEach
    void setUp() {
        bookId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        authorId = UUID.randomUUID();
        publisherId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        pricingGroupId = UUID.randomUUID();
        pricingGroup = PricingGroup.builder()
                .id(pricingGroupId)
                .name("Padrão")
                .percentage(new BigDecimal("25"))
                .build();
        Category cat = Category.builder().id(categoryId).name("Software").build();
        sampleBook = Book.builder()
                .id(bookId)
                .code("LIVR-000001-01")
                .title("Clean Code")
                .author("Robert C. Martin")
                .category("Software")
                .salePrice(new BigDecimal("89.90"))
                .costPrice(new BigDecimal("50.00"))
                .pricingGroup(pricingGroup)
                .isbn("9780132350884")
                .maxSaleValue(new BigDecimal("100.00"))
                .stockQuantity(10)
                .active(true)
                .publicationYear(2008)
                .edition("1ª")
                .pageCount(464)
                .synopsis("S")
                .heightCm(BigDecimal.valueOf(23))
                .widthCm(BigDecimal.valueOf(15.2))
                .depthCm(BigDecimal.valueOf(2.5))
                .weightKg(BigDecimal.valueOf(0.45))
                .barcode("T9780132350884")
                .categories(Set.of(cat))
                .build();
    }

    private BookCreateRequest sampleCreateRequest() {
        return BookCreateRequest.builder()
                .title("DDD")
                .authorId(authorId)
                .publisherId(publisherId)
                .supplierId(supplierId)
                .categoryIds(List.of(categoryId))
                .publicationYear(2003)
                .edition("1ª")
                .pageCount(400)
                .synopsis("Syn")
                .heightCm(BigDecimal.valueOf(23))
                .widthCm(BigDecimal.valueOf(15))
                .depthCm(BigDecimal.valueOf(3))
                .weightKg(BigDecimal.valueOf(0.5))
                .barcode("BAR9780321125217")
                .price(new BigDecimal("120.00"))
                .pricingGroupId(pricingGroupId)
                .isbn("9780321125217")
                .stockQuantity(5)
                .active(true)
                .build();
    }

    private BookUpdateRequest sampleUpdateRequest() {
        return BookUpdateRequest.builder()
                .title("Novo título")
                .authorId(authorId)
                .publisherId(publisherId)
                .supplierId(supplierId)
                .categoryIds(List.of(categoryId))
                .publicationYear(2008)
                .edition("1ª")
                .pageCount(464)
                .synopsis("S")
                .heightCm(BigDecimal.valueOf(23))
                .widthCm(BigDecimal.valueOf(15.2))
                .depthCm(BigDecimal.valueOf(2.5))
                .weightKg(BigDecimal.valueOf(0.45))
                .barcode("T9780132350884")
                .price(new BigDecimal("99.00"))
                .pricingGroupId(pricingGroupId)
                .isbn("9780132350884")
                .maxSaleValue(new BigDecimal("100.00"))
                .stockQuantity(3)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("RF0011 — Cadastrar livro")
    class Rf0011Create {

        @Test
        @DisplayName("givenValidCreateRequest_whenCreate_thenPersistsAndReturnsResponse")
        void givenValidCreateRequest_whenCreate_thenPersistsAndReturnsResponse() {
            doNothing().when(isbnExternalLookupService).assertExistsInOfficialCatalogOrThrow(any());
            when(pricingGroupRepository.findById(pricingGroupId)).thenReturn(Optional.of(pricingGroup));
            when(authorRepository.findById(authorId))
                    .thenReturn(Optional.of(Author.builder().id(authorId).name("Eric Evans").build()));
            when(publisherRepository.findById(publisherId))
                    .thenReturn(Optional.of(Publisher.builder().id(publisherId).name("Pearson").build()));
            when(supplierRepository.findById(supplierId))
                    .thenReturn(Optional.of(Supplier.builder().id(supplierId).name("Alpha").build()));
            when(categoryRepository.findAllById(anyList()))
                    .thenReturn(List.of(Category.builder().id(categoryId).name("Software").build()));
            when(bookCodeGeneratorService.nextCode()).thenReturn("LIVR-000042-42");

            AtomicReference<Book> savedRef = new AtomicReference<>();
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                savedRef.set(b);
                return b;
            });
            when(bookRepository.findByIdWithCategories(any(UUID.class)))
                    .thenAnswer(inv -> Optional.ofNullable(savedRef.get()));

            BookResponse response = bookService.create(sampleCreateRequest());

            assertThat(response.getTitle()).isEqualTo("DDD");
            assertThat(response.getIsbn()).isEqualTo("9780321125217");
            assertThat(response.isActive()).isTrue();
            verify(bookCodeGeneratorService).nextCode();
            verify(bookRepository).save(any(Book.class));
            verify(inventoryBalanceService).initializeInventoryForBook(any(UUID.class), eq(5));
            verify(pricingService).applyAutomaticSalePriceIfEligible(any(UUID.class));
        }

        @Test
        @DisplayName("givenUnknownPricingGroupId_whenCreate_thenThrowsNotFound")
        void givenUnknownPricingGroupId_whenCreate_thenThrowsNotFound() {
            UUID pgId = UUID.randomUUID();
            doNothing().when(isbnExternalLookupService).assertExistsInOfficialCatalogOrThrow(any());
            when(authorRepository.findById(authorId))
                    .thenReturn(Optional.of(Author.builder().id(authorId).name("A").build()));
            when(publisherRepository.findById(publisherId))
                    .thenReturn(Optional.of(Publisher.builder().id(publisherId).name("P").build()));
            when(supplierRepository.findById(supplierId))
                    .thenReturn(Optional.of(Supplier.builder().id(supplierId).name("S").build()));
            when(categoryRepository.findAllById(anyList()))
                    .thenReturn(List.of(Category.builder().id(categoryId).name("Software").build()));
            when(pricingGroupRepository.findById(pgId)).thenReturn(Optional.empty());

            BookCreateRequest request = BookCreateRequest.builder()
                    .title("X")
                    .authorId(authorId)
                    .publisherId(publisherId)
                    .supplierId(supplierId)
                    .categoryIds(List.of(categoryId))
                    .publicationYear(2000)
                    .edition("1")
                    .pageCount(1)
                    .synopsis("s")
                    .heightCm(BigDecimal.ONE)
                    .widthCm(BigDecimal.ONE)
                    .depthCm(BigDecimal.ONE)
                    .weightKg(BigDecimal.ONE)
                    .barcode("BAR999")
                    .price(BigDecimal.TEN)
                    .pricingGroupId(pgId)
                    .isbn("9780321125217")
                    .stockQuantity(1)
                    .build();

            assertThatThrownBy(() -> bookService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("givenValidRequest_whenCreate_thenAppliesGeneratedCodeFromBookCodeGenerator")
        void givenValidRequest_whenCreate_thenAppliesGeneratedCodeFromBookCodeGenerator() {
            doNothing().when(isbnExternalLookupService).assertExistsInOfficialCatalogOrThrow(any());
            when(pricingGroupRepository.findById(pricingGroupId)).thenReturn(Optional.of(pricingGroup));
            when(authorRepository.findById(authorId))
                    .thenReturn(Optional.of(Author.builder().id(authorId).name("A").build()));
            when(publisherRepository.findById(publisherId))
                    .thenReturn(Optional.of(Publisher.builder().id(publisherId).name("P").build()));
            when(supplierRepository.findById(supplierId))
                    .thenReturn(Optional.of(Supplier.builder().id(supplierId).name("S").build()));
            when(categoryRepository.findAllById(anyList()))
                    .thenReturn(List.of(Category.builder().id(categoryId).name("Software").build()));
            when(bookCodeGeneratorService.nextCode()).thenReturn("LIVR-000099-18");
            AtomicReference<Book> savedRef = new AtomicReference<>();
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                savedRef.set(b);
                return b;
            });
            when(bookRepository.findByIdWithCategories(any(UUID.class)))
                    .thenAnswer(inv -> Optional.ofNullable(savedRef.get()));

            bookService.create(
                    BookCreateRequest.builder()
                            .title("Refactoring")
                            .authorId(authorId)
                            .publisherId(publisherId)
                            .supplierId(supplierId)
                            .categoryIds(List.of(categoryId))
                            .publicationYear(1999)
                            .edition("1ª")
                            .pageCount(300)
                            .synopsis("Syn")
                            .heightCm(BigDecimal.valueOf(23))
                            .widthCm(BigDecimal.valueOf(15))
                            .depthCm(BigDecimal.valueOf(3))
                            .weightKg(BigDecimal.valueOf(0.5))
                            .barcode("BAR9780201485677")
                            .price(new BigDecimal("99.00"))
                            .pricingGroupId(pricingGroupId)
                            .isbn("9780201485677")
                            .stockQuantity(2)
                            .build());

            verify(bookCodeGeneratorService).nextCode();
            verify(pricingService).applyAutomaticSalePriceIfEligible(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("RF0012 — Inativar cadastro")
    class Rf0012Inactivate {

        @Test
        @DisplayName("deve inativar livro existente")
        void inactivate_existing() {
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.of(sampleBook));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            BookResponse response = bookService.setActive(
                    bookId,
                    PatchActiveRequest.builder()
                            .active(false)
                            .justification("x")
                            .reason(BookLifecycleReason.BAIXA_ROTACAO)
                            .build());

            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("deve retornar 404 ao inativar livro inexistente")
        void inactivate_notFound() {
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () -> bookService.setActive(
                                    bookId,
                                    PatchActiveRequest.builder()
                                            .active(false)
                                            .justification("x")
                                            .reason(BookLifecycleReason.BAIXA_ROTACAO)
                                            .build()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("não deve falhar se livro já estiver inativo (idempotente)")
        void inactivate_alreadyInactive() {
            sampleBook.setActive(false);
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.of(sampleBook));

            BookResponse response = bookService.setActive(
                    bookId,
                    PatchActiveRequest.builder()
                            .active(false)
                            .justification("x")
                            .reason(BookLifecycleReason.BAIXA_ROTACAO)
                            .build());

            assertThat(response.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("RF0013 — Inativação automática")
    class Rf0013AutoInactivate {

        @Test
        @DisplayName("estoque 0 e sem venda relevante → inativa")
        void stockZero_noRelevantSale_inactivates() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("X")
                    .salePrice(BigDecimal.TEN)
                    .isbn("9780000000001")
                    .stockQuantity(0)
                    .maxSaleValue(null)
                    .active(true)
                    .build();

            when(bookRepository.findByStockQuantity(0)).thenReturn(List.of(b));
            when(bookRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            bookService.inactivateBooksAutomatically(new BigDecimal("50.00"));

            assertThat(b.isActive()).isFalse();
            assertThat(b.getLastLifecycleReason()).isEqualTo(BookLifecycleReason.FORA_DE_MERCADO);
            verify(bookRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("estoque 0 e maxSaleValue abaixo do parâmetro → inativa")
        void stockZero_saleBelowMin_inactivates() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("X")
                    .salePrice(BigDecimal.TEN)
                    .isbn("9780000000002")
                    .stockQuantity(0)
                    .maxSaleValue(new BigDecimal("30.00"))
                    .active(true)
                    .build();

            when(bookRepository.findByStockQuantity(0)).thenReturn(List.of(b));
            when(bookRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            bookService.inactivateBooksAutomatically(new BigDecimal("50.00"));

            assertThat(b.isActive()).isFalse();
            verify(bookRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("estoque > 0 não é processado (lista vazia de candidatos com estoque zero)")
        void stockPositive_neverInList() {
            when(bookRepository.findByStockQuantity(0)).thenReturn(List.of());

            bookService.inactivateBooksAutomatically(new BigDecimal("50.00"));

            verify(bookRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("estoque 0 com venda relevante (>= parâmetro) → mantém ativo")
        void stockZero_relevantSale_keepsActive() {
            Book b = Book.builder()
                    .id(bookId)
                    .title("X")
                    .salePrice(BigDecimal.TEN)
                    .isbn("9780000000003")
                    .stockQuantity(0)
                    .maxSaleValue(new BigDecimal("80.00"))
                    .active(true)
                    .build();

            when(bookRepository.findByStockQuantity(0)).thenReturn(List.of(b));
            when(bookRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            bookService.inactivateBooksAutomatically(new BigDecimal("50.00"));

            assertThat(b.isActive()).isTrue();
            verify(bookRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("RF0014 — Alterar cadastro")
    class Rf0014Update {

        @Test
        @DisplayName("deve alterar dados de livro existente")
        void update_success() {
            when(pricingGroupRepository.findById(pricingGroupId)).thenReturn(Optional.of(pricingGroup));
            when(authorRepository.findById(authorId))
                    .thenReturn(Optional.of(Author.builder().id(authorId).name("Outro").build()));
            when(publisherRepository.findById(publisherId))
                    .thenReturn(Optional.of(Publisher.builder().id(publisherId).name("P").build()));
            when(supplierRepository.findById(supplierId))
                    .thenReturn(Optional.of(Supplier.builder().id(supplierId).name("S").build()));
            when(categoryRepository.findAllById(anyList()))
                    .thenReturn(List.of(Category.builder().id(categoryId).name("Software").build()));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bookRepository.findByIdWithCategories(any(UUID.class))).thenReturn(Optional.of(sampleBook));
            when(pricingService.minimumSalePriceForBook(any())).thenReturn(Optional.empty());

            BookResponse response = bookService.update(bookId, sampleUpdateRequest(), null);

            assertThat(response.getTitle()).isEqualTo("Novo título");
            assertThat(response.getCategory()).isEqualTo("Software");
            assertThat(response.getPrice()).isEqualByComparingTo("99.00");
            verify(inventoryBalanceService).syncQuantityFromBook(eq(bookId), eq(3));
            verify(pricingService).applyAutomaticSalePriceIfEligible(bookId);
        }

        @Test
        @DisplayName("não deve alterar livro inexistente")
        void update_notFound() {
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.empty());

            BookUpdateRequest request = BookUpdateRequest.builder()
                    .title("X")
                    .authorId(authorId)
                    .publisherId(publisherId)
                    .supplierId(supplierId)
                    .categoryIds(List.of(categoryId))
                    .publicationYear(2008)
                    .edition("1ª")
                    .pageCount(464)
                    .synopsis("S")
                    .heightCm(BigDecimal.valueOf(23))
                    .widthCm(BigDecimal.valueOf(15.2))
                    .depthCm(BigDecimal.valueOf(2.5))
                    .weightKg(BigDecimal.valueOf(0.45))
                    .barcode("T9780000000000")
                    .price(BigDecimal.ONE)
                    .pricingGroupId(pricingGroupId)
                    .isbn("9780000000000")
                    .stockQuantity(1)
                    .active(true)
                    .build();

            assertThatThrownBy(() -> bookService.update(bookId, request, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("RF0015 — Consulta com filtros")
    class Rf0015Filters {

        @Test
        @DisplayName("deve retornar lista vazia quando não houver resultados")
        void emptyResults() {
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

            var result = bookService.findByFilters("nada", null, null, null, null, false, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("deve retornar livros quando o repositório encontra correspondências")
        void returnsMatches() {
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleBook)));

            var result = bookService.findByFilters("Clean", "Martin", "Software", null, null, false, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        }
    }

    @Nested
    @DisplayName("RF0016 — Ativar cadastro")
    class Rf0016Activate {

        @Test
        @DisplayName("deve ativar livro previamente inativado")
        void activate_afterInactive() {
            sampleBook.setActive(false);
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.of(sampleBook));
            doNothing().when(isbnExternalLookupService).assertExistsInOfficialCatalogOrThrow(any());
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            BookResponse response = bookService.setActive(
                    bookId,
                    PatchActiveRequest.builder()
                            .active(true)
                            .justification("retorno")
                            .reason(BookLifecycleReason.RETORNO_ESTOQUE)
                            .build());

            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("deve retornar 404 ao ativar livro inexistente")
        void activate_notFound() {
            when(bookRepository.findByIdWithCategories(bookId)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () -> bookService.setActive(
                                    bookId,
                                    PatchActiveRequest.builder()
                                            .active(true)
                                            .justification("x")
                                            .reason(BookLifecycleReason.RETORNO_ESTOQUE)
                                            .build()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
