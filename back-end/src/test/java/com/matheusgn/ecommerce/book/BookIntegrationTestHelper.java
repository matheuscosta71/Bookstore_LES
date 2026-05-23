package com.matheusgn.ecommerce.book;

import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.config.InventoryDataMigration;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JSON de criação de livro alinhado ao domínio carregado em testes de integração.
 */
public final class BookIntegrationTestHelper {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final PricingGroupRepository pricingGroupRepository;

    public BookIntegrationTestHelper(
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository,
            SupplierRepository supplierRepository,
            CategoryRepository categoryRepository,
            PricingGroupRepository pricingGroupRepository) {
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.supplierRepository = supplierRepository;
        this.categoryRepository = categoryRepository;
        this.pricingGroupRepository = pricingGroupRepository;
    }

    public String createBookJson(String title, String isbn, BigDecimal price, int stock) {
        return createBookJson(title, isbn, price, stock, "Software");
    }

    public String createBookJson(String title, String isbn, BigDecimal price, int stock, String categoryName) {
        UUID authorId = authorRepository.findByNameIgnoreCase("Robert C. Martin").orElseThrow().getId();
        UUID publisherId = publisherRepository.findByNameIgnoreCase("Pearson").orElseThrow().getId();
        UUID supplierId = supplierRepository.findByNameIgnoreCase("Distribuidora Alpha").orElseThrow().getId();
        UUID categoryId = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow().getId();
        UUID pricingGroupId = pricingGroupRepository
                .findByName(InventoryDataMigration.DEFAULT_GROUP_NAME)
                .orElseThrow()
                .getId();
        String barcode = "T" + isbn.replaceAll("\\D", "");
        BigDecimal cost = price.multiply(new BigDecimal("0.6")).setScale(2, java.math.RoundingMode.HALF_UP);
        return """
                {
                  "title": "%s",
                  "authorId": "%s",
                  "publisherId": "%s",
                  "supplierId": "%s",
                  "categoryIds": ["%s"],
                  "publicationYear": 2020,
                  "edition": "1ª",
                  "pageCount": 200,
                  "synopsis": "Sinopse de teste.",
                  "heightCm": 23.0,
                  "widthCm": 15.2,
                  "depthCm": 2.5,
                  "weightKg": 0.45,
                  "barcode": "%s",
                  "price": %s,
                  "costPrice": %s,
                  "pricingGroupId": "%s",
                  "isbn": "%s",
                  "stockQuantity": %d,
                  "active": true
                }
                """
                .formatted(
                        title,
                        authorId,
                        publisherId,
                        supplierId,
                        categoryId,
                        barcode,
                        price.toPlainString(),
                        cost.toPlainString(),
                        pricingGroupId,
                        isbn,
                        stock);
    }

    public String updateBookJson(
            String title,
            String isbn,
            BigDecimal price,
            int stock,
            boolean active,
            String categoryName,
            BigDecimal maxSaleValue) {
        UUID authorId = authorRepository.findByNameIgnoreCase("Robert C. Martin").orElseThrow().getId();
        UUID publisherId = publisherRepository.findByNameIgnoreCase("Pearson").orElseThrow().getId();
        UUID supplierId = supplierRepository.findByNameIgnoreCase("Distribuidora Alpha").orElseThrow().getId();
        UUID categoryId = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow().getId();
        UUID pricingGroupId = pricingGroupRepository
                .findByName(InventoryDataMigration.DEFAULT_GROUP_NAME)
                .orElseThrow()
                .getId();
        String barcode = "T" + isbn.replaceAll("\\D", "");
        BigDecimal cost = price.multiply(new BigDecimal("0.6")).setScale(2, java.math.RoundingMode.HALF_UP);
        String maxPart = maxSaleValue == null ? "null" : maxSaleValue.toPlainString();
        return """
                {
                  "title": "%s",
                  "authorId": "%s",
                  "publisherId": "%s",
                  "supplierId": "%s",
                  "categoryIds": ["%s"],
                  "publicationYear": 2020,
                  "edition": "1ª",
                  "pageCount": 200,
                  "synopsis": "Sinopse de teste.",
                  "heightCm": 23.0,
                  "widthCm": 15.2,
                  "depthCm": 2.5,
                  "weightKg": 0.45,
                  "barcode": "%s",
                  "price": %s,
                  "costPrice": %s,
                  "pricingGroupId": "%s",
                  "isbn": "%s",
                  "maxSaleValue": %s,
                  "stockQuantity": %d,
                  "active": %s
                }
                """
                .formatted(
                        title,
                        authorId,
                        publisherId,
                        supplierId,
                        categoryId,
                        barcode,
                        price.toPlainString(),
                        cost.toPlainString(),
                        pricingGroupId,
                        isbn,
                        maxPart,
                        stock,
                        active);
    }
}
