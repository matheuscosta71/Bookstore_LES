package com.matheusgn.ecommerce.book.repository;

import com.matheusgn.ecommerce.book.entity.Book;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class BookSpecifications {

    public static Specification<Book> withFilters(
            String title,
            String author,
            String category,
            String isbn,
            String code,
            boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (StringUtils.hasText(code)) {
                predicates.add(cb.equal(root.get("code"), code.trim()));
            }

            if (StringUtils.hasText(title)) {
                String pattern = "%" + title.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }
            if (StringUtils.hasText(author)) {
                String pattern = "%" + author.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("author")), pattern));
            }
            if (StringUtils.hasText(category)) {
                String pattern = "%" + category.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("category")), pattern));
            }
            if (StringUtils.hasText(isbn)) {
                predicates.add(cb.equal(root.get("isbn"), isbn.trim()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
