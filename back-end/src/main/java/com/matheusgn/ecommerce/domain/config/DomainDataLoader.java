package com.matheusgn.ecommerce.domain.config;

import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.entity.Category;
import com.matheusgn.ecommerce.domain.entity.Publisher;
import com.matheusgn.ecommerce.domain.entity.Supplier;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.config.InventoryDataMigration;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(0)
@RequiredArgsConstructor
public class DomainDataLoader implements ApplicationRunner {

    private final PricingGroupRepository pricingGroupRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensurePricingGroup("Econômico", new BigDecimal("20.0000"));
        ensurePricingGroup(InventoryDataMigration.DEFAULT_GROUP_NAME, new BigDecimal("25.0000"));
        ensurePricingGroup("Premium", new BigDecimal("35.0000"));

        seedAuthors();
        seedPublishers();
        seedSuppliers();
        seedCategories();
    }

    private void ensurePricingGroup(String name, BigDecimal percentage) {
        pricingGroupRepository.findByName(name)
                .orElseGet(() -> pricingGroupRepository.save(PricingGroup.builder()
                        .name(name)
                        .percentage(percentage)
                        .build()));
    }

    private void seedAuthors() {
        List<String> names = List.of(
                "Robert C. Martin",
                "Martin Fowler",
                "Eric Evans",
                "F. Scott Fitzgerald",
                "George Orwell",
                "Harper Lee",
                "J. D. Salinger",
                "J. K. Rowling",
                "George R. R. Martin");
        for (String n : names) {
            authorRepository.findByNameIgnoreCase(n)
                    .orElseGet(() -> authorRepository.save(Author.builder().name(n).build()));
        }
    }

    private void seedPublishers() {
        List<String> names = List.of("Pearson", "O'Reilly", "Novatec");
        for (String n : names) {
            publisherRepository.findByNameIgnoreCase(n)
                    .orElseGet(() -> publisherRepository.save(Publisher.builder().name(n).build()));
        }
    }

    private void seedSuppliers() {
        List<String> names = List.of("Distribuidora Alpha", "Livros Beta", "Supply Gamma");
        for (String n : names) {
            supplierRepository.findByNameIgnoreCase(n)
                    .orElseGet(() -> supplierRepository.save(Supplier.builder().name(n).build()));
        }
    }

    private void seedCategories() {
        List<String> names = List.of("Software", "Ficção", "Negócios", "Infantil", "Literatura");
        for (String n : names) {
            categoryRepository.findByNameIgnoreCase(n)
                    .orElseGet(() -> categoryRepository.save(Category.builder().name(n).build()));
        }
    }
}
