package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.config.PageConstraints;
import com.matheusgn.ecommerce.customer.dto.CustomerCreateRequest;
import com.matheusgn.ecommerce.customer.dto.CustomerMapper;
import com.matheusgn.ecommerce.customer.dto.CustomerResponse;
import com.matheusgn.ecommerce.customer.dto.CustomerUpdateRequest;
import com.matheusgn.ecommerce.customer.dto.PasswordChangeRequest;
import com.matheusgn.ecommerce.customer.entity.Customer;
import com.matheusgn.ecommerce.customer.repository.CustomerRepository;
import com.matheusgn.ecommerce.customer.repository.CustomerSpecifications;
import com.matheusgn.ecommerce.exception.DuplicateResourceException;
import com.matheusgn.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerCodeGeneratorService customerCodeGeneratorService;
    private final AuditLogService auditLogService;

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String cpf = normalizeCpf(request.getCpf());
        String email = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("email", "E-mail já cadastrado");
        }
        if (customerRepository.existsByCpf(cpf)) {
            throw new DuplicateResourceException("cpf", "CPF já cadastrado");
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .cpf(cpf)
                .phone(request.getPhone().trim())
                .birthDate(request.getBirthDate())
                .password(passwordEncoder.encode(request.getPassword()))
                .code(customerCodeGeneratorService.nextCode())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Customer saved = customerRepository.save(customer);
        CustomerResponse response = CustomerMapper.toResponse(saved);
        auditLogService.logCreate("Customer", saved.getId(), response);
        return response;
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        String cpf = normalizeCpf(request.getCpf());
        String email = request.getEmail().trim().toLowerCase();

        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("email", "E-mail já cadastrado para outro cliente");
        }
        if (customerRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new DuplicateResourceException("cpf", "CPF já cadastrado para outro cliente");
        }

        customer.setFullName(request.getFullName().trim());
        customer.setEmail(email);
        customer.setCpf(cpf);
        customer.setPhone(request.getPhone().trim());
        customer.setBirthDate(request.getBirthDate());
        customer.setActive(Boolean.TRUE.equals(request.getActive()));

        Customer saved = customerRepository.save(customer);
        auditLogService.logUpdate("Customer", id, request);
        return CustomerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return CustomerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findByFilters(
            String fullName,
            String email,
            String cpf,
            String phone,
            String code,
            LocalDate birthDate,
            Boolean active,
            Pageable pageable) {
        String cpfNorm = cpf != null && !cpf.isBlank() ? normalizeCpf(cpf) : null;
        String emailNorm = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;

        Specification<Customer> spec = CustomerSpecifications.withFilters(
                fullName, emailNorm, cpfNorm, phone, code, birthDate, active);
        Pageable p = PageConstraints.clamp(pageable);
        return customerRepository.findAll(spec, p).map(CustomerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse setActive(UUID id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        customer.setActive(active);
        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void changePassword(UUID id, PasswordChangeRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerRepository.save(customer);
        auditLogService.logUpdate("Customer", id, Map.of("passwordChanged", true));
    }

    public static String normalizeCpf(String cpf) {
        if (cpf == null) {
            return "";
        }
        return cpf.replaceAll("\\D", "");
    }
}
