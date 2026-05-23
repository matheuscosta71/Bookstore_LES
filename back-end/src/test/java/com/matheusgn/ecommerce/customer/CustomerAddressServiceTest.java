package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.AddressUpdateRequest;
import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.AddressType;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.customer.service.CustomerAddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RNF0034: endereços podem ser adicionados/alterados sem reescrever o cadastro geral do cliente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressService — isolamento de endereço")
class CustomerAddressServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CustomerAddressService customerAddressService;

    private static AddressCreateRequest createRequest() {
        return AddressCreateRequest.builder()
                .nickname("Casa")
                .street("Rua das Flores")
                .number("100")
                .complement("Apto 2")
                .neighborhood("Centro")
                .city("Belo Horizonte")
                .state("mg")
                .zipCode("30130000")
                .type(AddressType.DELIVERY)
                .build();
    }

    @Test
    @DisplayName("addAddress persiste só o endereço vinculado ao cliente, sem salvar Customer")
    void addAddress_savesOnlyAddressRow() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).fullName("João").build();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        customerAddressService.addAddress(customerId, createRequest());

        ArgumentCaptor<Address> cap = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(cap.capture());
        assertThat(cap.getValue().getCustomer()).isEqualTo(customer);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(auditLogService).logCreate(eq("Address"), any(), any());
    }

    @Test
    @DisplayName("updateAddress altera campos do endereço sem atualizar entidade Customer")
    void updateAddress_doesNotTouchCustomerEntity() {
        UUID customerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).build();
        Address existing = Address.builder()
                .id(addressId)
                .customer(customer)
                .nickname("Casa")
                .street("Rua A")
                .number("1")
                .neighborhood("N")
                .city("C")
                .state("MG")
                .zipCode("30000000")
                .type(AddressType.DELIVERY)
                .active(true)
                .build();
        when(addressRepository.findByIdAndCustomer_Id(addressId, customerId)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressUpdateRequest upd = AddressUpdateRequest.builder()
                .nickname("Trabalho")
                .street("Av. Brasil")
                .number("500")
                .complement(null)
                .neighborhood("Savassi")
                .city("Belo Horizonte")
                .state("mg")
                .zipCode("30140000")
                .type(AddressType.BILLING)
                .active(true)
                .build();

        customerAddressService.updateAddress(customerId, addressId, upd);

        verify(addressRepository).save(any(Address.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }
}
