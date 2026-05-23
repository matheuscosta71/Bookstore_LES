package com.matheusgn.ecommerce.customer.controller;

import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerResponse;
import com.matheusgn.ecommerce.customer.dto.CustomerUpdateRequest;
import com.matheusgn.ecommerce.customer.dto.PasswordChangeRequest;
import com.matheusgn.ecommerce.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import static com.matheusgn.ecommerce.config.PageConstraints.DEFAULT_PAGE_SIZE;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Cadastro de clientes (RF0021–RF0024, RF0028)")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "RF0021 — Cadastrar cliente")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        log.info("[CustomerController][create] Cadastro de cliente solicitado");
        CustomerResponse body = customerService.create(request);
        log.info("[CustomerController][create] Cliente criado customerId={}", body.getId());
        return ResponseEntity.created(URI.create("/customers/" + body.getId())).body(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "RF0022 — Alterar cliente")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @PatchMapping("/{id}/inactive")
    @Operation(summary = "RF0023 — Inativar cliente")
    public ResponseEntity<CustomerResponse> inactive(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.setActive(id, false));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Reativar cliente")
    public ResponseEntity<CustomerResponse> active(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.setActive(id, true));
    }

    @GetMapping
    @Operation(summary = "RF0024 — Consultar clientes com filtros opcionais (paginado)")
    public ResponseEntity<Page<CustomerResponse>> list(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return ResponseEntity.ok(
                customerService.findByFilters(fullName, email, cpf, phone, code, birthDate, active, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por id")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "RF0028 — Alterar apenas a senha")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody PasswordChangeRequest request) {
        customerService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
