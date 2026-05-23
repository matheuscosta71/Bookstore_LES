package com.matheusgn.ecommerce.sales;

import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import com.matheusgn.ecommerce.sales.entity.Cart;
import com.matheusgn.ecommerce.sales.entity.CartStatus;
import com.matheusgn.ecommerce.sales.service.FreightService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private FreightService freightService;

    @Nested
    @DisplayName("RF0034 — cálculo")
    class Calculate {

        @Test
        @DisplayName("givenSubtotal100AndZip01310100_whenCalculate_thenReturnsExpectedFreight")
        void givenSubtotal100AndZip01310100_whenCalculate_thenReturnsExpectedFreight() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            Customer c = Customer.builder().id(customerId).build();
            Address addr = Address.builder()
                    .id(addressId)
                    .customer(c)
                    .zipCode("01310100")
                    .build();
            Cart cart = Cart.builder()
                    .customer(c)
                    .status(CartStatus.OPEN)
                    .totalAmount(new BigDecimal("100.00"))
                    .build();

            when(addressRepository.findByIdAndCustomer_Id(addressId, customerId)).thenReturn(Optional.of(addr));

            BigDecimal freight = freightService.calculate(cart, addressId, customerId);

            // base 12.90 + 2% of 100 = 2.00 + sum(zip digits)=6 * 0.15 = 0.90 => 15.80
            assertThat(freight).isEqualByComparingTo(new BigDecimal("15.80"));
        }

        @Test
        @DisplayName("givenMissingAddress_whenCalculate_thenThrows")
        void givenMissingAddress_whenCalculate_thenThrows() {
            UUID customerId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            Cart cart = Cart.builder()
                    .totalAmount(new BigDecimal("10.00"))
                    .build();

            when(addressRepository.findByIdAndCustomer_Id(addressId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> freightService.calculate(cart, addressId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
