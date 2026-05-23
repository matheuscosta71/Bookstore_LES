package com.matheusgn.ecommerce.book.controller;

import com.matheusgn.ecommerce.book.dto.AutomaticInactivationRequest;
import com.matheusgn.ecommerce.book.dto.BookCreateRequest;
import com.matheusgn.ecommerce.book.dto.BookResponse;
import com.matheusgn.ecommerce.book.dto.BookUpdateRequest;
import com.matheusgn.ecommerce.book.dto.PatchActiveRequest;
import com.matheusgn.ecommerce.book.service.BookService;
import com.matheusgn.ecommerce.inventory.service.PricingService;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import static com.matheusgn.ecommerce.config.PageConstraints.DEFAULT_PAGE_SIZE;

@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Cadastro e gestão de livros")
public class BookController {

    private final BookService bookService;
    private final PricingService pricingService;
    private final AdminOrderService adminOrderService;

    @PostMapping
    @Operation(summary = "Criar livro")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookCreateRequest request) {
        log.info("[BookController][create] Criar livro");
        BookResponse body = bookService.create(request);
        log.info("[BookController][create] Livro criado bookId={}", body.getId());
        return ResponseEntity
                .created(URI.create("/books/" + body.getId()))
                .body(body);
    }

    @GetMapping
    @Operation(summary = "Consultar livros com filtros opcionais (paginado)")
    public ResponseEntity<Page<BookResponse>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return ResponseEntity.ok(
                bookService.findByFilters(title, author, category, isbn, code, includeInactive, pageable));
    }

    @PostMapping("/inactivate-automatic")
    @Operation(summary = "Inativar livros automaticamente (RF0013)")
    public ResponseEntity<Void> inactivateAutomatic(@Valid @RequestBody AutomaticInactivationRequest request) {
        log.info("[BookController][inactivateAutomatic] Inativação automática minimumSalesValue={}",
                request.getMinimumSalesValue());
        bookService.inactivateBooksAutomatically(request.getMinimumSalesValue());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar livro por id")
    public ResponseEntity<BookResponse> getById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(bookService.findById(id, includeInactive));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar livro (RN0014: preço abaixo da margem exige X-Sales-Manager-Key)")
    public ResponseEntity<BookResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody BookUpdateRequest request,
            @RequestHeader(value = "X-Sales-Manager-Key", required = false) String salesManagerKey) {
        return ResponseEntity.ok(bookService.update(id, request, salesManagerKey));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover livro")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("[BookController][delete] Remover livro bookId={}", id);
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Ativar ou inativar livro")
    public ResponseEntity<BookResponse> patchActive(
            @PathVariable UUID id,
            @Valid @RequestBody PatchActiveRequest request) {
        log.info("[BookController][patchActive] bookId={} active={}", id, request.getActive());
        return ResponseEntity.ok(bookService.setActive(id, request));
    }

    @PostMapping("/{bookId}/recalculate-sale-price")
    @Operation(summary = "RF0052 — Recalcular preço de venda (custo × (1 + % grupo)); também aplicado ao salvar livro quando elegível")
    public ResponseEntity<BookResponse> recalculateSalePrice(
            @PathVariable UUID bookId,
            @RequestHeader("X-Admin-Key") String adminKey) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(pricingService.recalculateSalePrice(bookId));
    }
}
