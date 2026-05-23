package com.matheusgn.ecommerce.domain;

import com.matheusgn.ecommerce.domain.config.DomainDataLoader;
import com.matheusgn.ecommerce.domain.entity.Author;
import com.matheusgn.ecommerce.domain.repository.AuthorRepository;
import com.matheusgn.ecommerce.domain.repository.CategoryRepository;
import com.matheusgn.ecommerce.domain.repository.PublisherRepository;
import com.matheusgn.ecommerce.domain.repository.SupplierRepository;
import com.matheusgn.ecommerce.inventory.repository.PricingGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DomainDataLoaderTest {

    @Autowired
    private DomainDataLoader domainDataLoader;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private PublisherRepository publisherRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PricingGroupRepository pricingGroupRepository;

    @Test
    void loaderRunsIdempotent_secondRunDoesNotDuplicate() {
        assertThat(pricingGroupRepository.count()).isGreaterThan(0L);
        assertThat(publisherRepository.count()).isGreaterThan(0L);
        assertThat(supplierRepository.count()).isGreaterThan(0L);
        assertThat(categoryRepository.count()).isGreaterThan(0L);
        assertThat(authorRepository.findByNameIgnoreCase("Robert C. Martin")).map(Author::getName).isPresent();

        long authorsAfterStartup = authorRepository.count();
        domainDataLoader.run(new DefaultApplicationArguments());
        domainDataLoader.run(new DefaultApplicationArguments());
        assertThat(authorRepository.count()).isEqualTo(authorsAfterStartup);
    }
}
