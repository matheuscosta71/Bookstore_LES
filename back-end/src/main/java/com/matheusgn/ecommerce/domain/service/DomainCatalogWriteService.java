package com.matheusgn.ecommerce.domain.service;

import com.matheusgn.ecommerce.domain.dto.IdNameResponse;
import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DomainCatalogWriteService {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public IdNameResponse createAuthor(String name) {
        String trimmed = name.trim();
        return authorRepository
                .findByNameIgnoreCase(trimmed)
                .map(a -> toResponse(a.getId(), a.getName()))
                .orElseGet(
                        () -> {
                            Author saved = authorRepository.save(Author.builder().name(trimmed).build());
                            return toResponse(saved.getId(), saved.getName());
                        });
    }

    @Transactional
    public IdNameResponse createPublisher(String name) {
        String trimmed = name.trim();
        return publisherRepository
                .findByNameIgnoreCase(trimmed)
                .map(p -> toResponse(p.getId(), p.getName()))
                .orElseGet(
                        () -> {
                            Publisher saved = publisherRepository.save(Publisher.builder().name(trimmed).build());
                            return toResponse(saved.getId(), saved.getName());
                        });
    }

    @Transactional
    public IdNameResponse createSupplier(String name) {
        String trimmed = name.trim();
        return supplierRepository
                .findByNameIgnoreCase(trimmed)
                .map(s -> toResponse(s.getId(), s.getName()))
                .orElseGet(
                        () -> {
                            Supplier saved = supplierRepository.save(Supplier.builder().name(trimmed).build());
                            return toResponse(saved.getId(), saved.getName());
                        });
    }

    private static IdNameResponse toResponse(UUID id, String name) {
        return IdNameResponse.builder().id(id).name(name).build();
    }
}
