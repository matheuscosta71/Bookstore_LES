package com.matheusgn.ecommerce.sales.service;

import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.entity.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreightService {

    private final AddressRepository addressRepository;

    /**
     * Frete simples: valor fixo base + percentual do subtotal + fator pelo CEP (dígitos).
     */
    public BigDecimal calculate(Cart cart, UUID addressId, UUID customerId) {
        Address address = addressRepository.findByIdAndCustomer_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado para o cliente"));

        BigDecimal subtotal = cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
        String zipDigits = address.getZipCode().replaceAll("\\D", "");
        int zipFactor = zipDigits.isEmpty() ? 0 : zipDigits.chars().map(Character::getNumericValue).sum();

        BigDecimal base = new BigDecimal("12.90");
        BigDecimal percent = subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cepPart = new BigDecimal(zipFactor).multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);

        return base.add(percent).add(cepPart).setScale(2, RoundingMode.HALF_UP);
    }
}
