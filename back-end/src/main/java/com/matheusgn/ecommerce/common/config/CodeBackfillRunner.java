package com.matheusgn.ecommerce.common.config;

import com.matheusgn.ecommerce.book.entity.Book;
import com.matheusgn.ecommerce.book.repository.BookRepository;
import com.matheusgn.ecommerce.book.service.BookCodeGeneratorService;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.service.CustomerCodeGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(5)
@RequiredArgsConstructor
public class CodeBackfillRunner implements ApplicationRunner {

    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;
    private final BookCodeGeneratorService bookCodeGeneratorService;
    private final CustomerCodeGeneratorService customerCodeGeneratorService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Book book : bookRepository.findAll()) {
            if (book.getCode() == null || book.getCode().isBlank()) {
                book.setCode(bookCodeGeneratorService.nextCode());
                bookRepository.save(book);
            }
        }
        for (Customer c : customerRepository.findAll()) {
            if (c.getCode() == null || c.getCode().isBlank()) {
                c.setCode(customerCodeGeneratorService.nextCode());
                customerRepository.save(c);
            }
        }
    }
}
