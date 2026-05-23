package com.matheusgn.ecommerce.domain.controller;

import com.matheusgn.ecommerce.domain.dto.DomainNameCreateRequest;
import com.matheusgn.ecommerce.domain.dto.IdNameResponse;
import com.matheusgn.ecommerce.domain.service.DomainCatalogWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Domain create", description = "Cadastro rápido de autores, editoras e fornecedores")
public class DomainEntityCreateController {

    private final DomainCatalogWriteService domainCatalogWriteService;

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar autor (nome único; se já existir, retorna o existente)")
    public IdNameResponse createAuthor(@Valid @RequestBody DomainNameCreateRequest body) {
        return domainCatalogWriteService.createAuthor(body.getName());
    }

    @PostMapping("/publishers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar editora (nome único; se já existir, retorna a existente)")
    public IdNameResponse createPublisher(@Valid @RequestBody DomainNameCreateRequest body) {
        return domainCatalogWriteService.createPublisher(body.getName());
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar fornecedor (nome único; se já existir, retorna o existente)")
    public IdNameResponse createSupplier(@Valid @RequestBody DomainNameCreateRequest body) {
        return domainCatalogWriteService.createSupplier(body.getName());
    }
}
