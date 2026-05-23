package com.matheusgn.ecommerce.customer.repository;

import com.matheusgn.ecommerce.customer.entity.Customer;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UtilityClass
public class CustomerSpecifications {

    public static Specification<Customer> withFilters(
            String fullName,
            String email,
            String cpf,
            String phone,
            String code,
            LocalDate birthDate,
            Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(code)) {
                predicates.add(cb.equal(root.get("code"), code.trim()));
            }

            if (StringUtils.hasText(fullName)) {
                String pattern = "%" + fullName.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("fullName")), pattern));
            }
            if (StringUtils.hasText(email)) {
                predicates.add(cb.equal(cb.lower(root.get("email")), email.trim().toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(cpf)) {
                predicates.add(cb.equal(root.get("cpf"), cpf.replaceAll("\\D", "")));
            }
            if (StringUtils.hasText(phone)) {
                String pattern = "%" + phone.trim() + "%";
                predicates.add(cb.like(root.get("phone"), pattern));
            }
            if (birthDate != null) {
                predicates.add(cb.equal(root.get("birthDate"), birthDate));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
