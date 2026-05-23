package com.matheusgn.ecommerce.customer.dto;

import com.matheusgn.ecommerce.customer.entity.Address;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AddressMapper {

    public static AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .nickname(a.getNickname())
                .street(a.getStreet())
                .number(a.getNumber())
                .complement(a.getComplement())
                .neighborhood(a.getNeighborhood())
                .city(a.getCity())
                .state(a.getState())
                .zipCode(a.getZipCode())
                .type(a.getType())
                .active(a.isActive())
                .build();
    }
}
