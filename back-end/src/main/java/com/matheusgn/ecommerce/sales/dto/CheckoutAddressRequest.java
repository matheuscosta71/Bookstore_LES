package com.matheusgn.ecommerce.sales.dto;

import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutAddressRequest {

    /** Se informado, usa endereço já cadastrado */
    private UUID addressId;

    /** Novo endereço (mutuamente exclusivo com addressId) */
    private AddressCreateRequest newAddress;

    /** Obrigatório quando {@code newAddress} for informado: se true, endereço fica no perfil; se false, uso apenas no pedido */
    private Boolean saveToProfile;
}
