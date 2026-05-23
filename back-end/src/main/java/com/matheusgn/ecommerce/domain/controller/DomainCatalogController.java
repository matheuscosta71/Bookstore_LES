package com.matheusgn.ecommerce.domain.controller;

import com.matheusgn.ecommerce.domain.dto.IdNameResponse;
import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/domain")
@RequiredArgsConstructor
@Tag(name = "Domain", description = "Catálogo de domínio (autores, editoras, etc.) para cadastro de livros")
public class DomainCatalogController {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final PricingGroupRepository pricingGroupRepository;

    @GetMapping("/authors")
    @Operation(summary = "Listar autores")
    public List<IdNameResponse> authors() {
        return authorRepository.findAll().stream()
                .sorted(Comparator.comparing(Author::getName))
                .map(a -> IdNameResponse.builder().id(a.getId()).name(a.getName()).build())
                .toList();
    }

    @GetMapping("/publishers")
    @Operation(summary = "Listar editoras")
    public List<IdNameResponse> publishers() {
        return publisherRepository.findAll().stream()
                .sorted(Comparator.comparing(Publisher::getName))
                .map(p -> IdNameResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();
    }

    @GetMapping("/suppliers")
    @Operation(summary = "Listar fornecedores")
    public List<IdNameResponse> suppliers() {
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName))
                .map(s -> IdNameResponse.builder().id(s.getId()).name(s.getName()).build())
                .toList();
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorias")
    public List<IdNameResponse> categories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(c -> IdNameResponse.builder().id(c.getId()).name(c.getName()).build())
                .toList();
    }

    @GetMapping("/pricing-groups")
    @Operation(summary = "Listar grupos de precificação")
    public List<IdNameResponse> pricingGroups() {
        return pricingGroupRepository.findAll().stream()
                .sorted(Comparator.comparing(PricingGroup::getName))
                .map(g -> IdNameResponse.builder().id(g.getId()).name(g.getName()).build())
                .toList();
    }
}
