package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.entity.Customer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CustomerMapper {

    public static CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .cpf(c.getCpf())
                .phone(c.getPhone())
                .birthDate(c.getBirthDate())
                .active(c.isActive())
                .rankingScore(c.getRankingScore())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
