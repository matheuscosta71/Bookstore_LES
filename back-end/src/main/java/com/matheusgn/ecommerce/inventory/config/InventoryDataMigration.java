package com.matheusgn.ecommerce.inventory.config;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.inventory.entity.PricingGroup;
import com.matheusgn.ecommerce.inventory.repository.InventoryRepository;
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
@Order(1)
@RequiredArgsConstructor
public class InventoryDataMigration implements ApplicationRunner {

    public static final String DEFAULT_GROUP_NAME = "Padrão";

    private final PricingGroupRepository pricingGroupRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final com.matheusgn.ecommerce.inventory.service.InventoryBalanceService inventoryBalanceService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        PricingGroup defaultGroup = pricingGroupRepository.findByName(DEFAULT_GROUP_NAME)
                .orElseGet(() -> pricingGroupRepository.save(PricingGroup.builder()
                        .name(DEFAULT_GROUP_NAME)
                        .percentage(new BigDecimal("25.0000"))
                        .build()));

        List<Book> books = bookRepository.findAll();
        for (Book book : books) {
            if (book.getPricingGroup() == null) {
                book.setPricingGroup(defaultGroup);
            }
            if (book.getCostPrice() == null) {
                book.setCostPrice(book.getSalePrice());
            }
            bookRepository.save(book);
            if (!inventoryRepository.existsByBook_Id(book.getId())) {
                inventoryBalanceService.initializeInventoryForBook(book.getId(), book.getStockQuantity());
            }
        }
    }
}
