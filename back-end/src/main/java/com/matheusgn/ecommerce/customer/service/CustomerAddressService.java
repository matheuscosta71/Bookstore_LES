package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.customer.dto.AddressCreateRequest;
import com.matheusgn.ecommerce.customer.dto.AddressMapper;
import com.matheusgn.ecommerce.customer.dto.AddressResponse;
import com.matheusgn.ecommerce.customer.dto.AddressUpdateRequest;
import com.matheusgn.ecommerce.customer.entity.Address;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.AddressRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AddressResponse addAddress(UUID customerId, AddressCreateRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        Address address = Address.builder()
                .customer(customer)
                .nickname(request.getNickname().trim())
                .street(request.getStreet().trim())
                .number(request.getNumber().trim())
                .complement(request.getComplement() != null ? request.getComplement().trim() : null)
                .neighborhood(request.getNeighborhood().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim().toUpperCase())
                .zipCode(request.getZipCode().replaceAll("\\D", ""))
                .type(request.getType())
                .active(true)
                .build();

        Address saved = addressRepository.save(address);
        auditLogService.logCreate("Address", saved.getId(), AddressMapper.toResponse(saved));
        return AddressMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listActiveByCustomer(UUID customerId) {
        assertCustomerExists(customerId);
        return addressRepository.findByCustomer_IdAndActiveTrueOrderByNicknameAsc(customerId).stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse updateAddress(UUID customerId, UUID addressId, AddressUpdateRequest request) {
        Address address = addressRepository.findByIdAndCustomer_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        address.setNickname(request.getNickname().trim());
        address.setStreet(request.getStreet().trim());
        address.setNumber(request.getNumber().trim());
        address.setComplement(request.getComplement() != null ? request.getComplement().trim() : null);
        address.setNeighborhood(request.getNeighborhood().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim().toUpperCase());
        address.setZipCode(request.getZipCode().replaceAll("\\D", ""));
        address.setType(request.getType());
        address.setActive(Boolean.TRUE.equals(request.getActive()));

        Address saved = addressRepository.save(address);
        auditLogService.logUpdate("Address", saved.getId(), request);
        return AddressMapper.toResponse(saved);
    }

    @Transactional
    public AddressResponse deactivateAddress(UUID customerId, UUID addressId) {
        Address address = addressRepository.findByIdAndCustomer_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));
        address.setActive(false);
        Address saved = addressRepository.save(address);
        auditLogService.logUpdate("Address", saved.getId(), Map.of("active", false));
        return AddressMapper.toResponse(saved);
    }

    private void assertCustomerExists(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }
    }
}
