package com.barbershop.service;

import com.barbershop.model.dto.CreateCustomerRequest;
import com.barbershop.model.dto.CustomerDTO;
import com.barbershop.model.dto.UpdateCustomerRequest;
import com.barbershop.model.entity.Customer;
import com.barbershop.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .notes(request.getNotes())
                .build();

        Customer saved = customerRepository.save(customer);
        return mapToDTO(saved);
    }

    public Page<CustomerDTO> getAllCustomers(Pageable pageable) {
        Page<Customer> customers = customerRepository.findAllActive(pageable);
        return customers.map(this::mapToDTO);
    }

    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return mapToDTO(customer);
    }

    public CustomerDTO getOrCreateCustomer(CreateCustomerRequest request) {
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            return customerRepository.findByPhone(request.getPhone())
                    .map(this::mapToDTO)
                    .orElseGet(() -> createCustomer(request));
        }
        return createCustomer(request);
    }

    public CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        if (request.getName() != null) {
            customer.setName(request.getName());
        }
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getNotes() != null) {
            customer.setNotes(request.getNotes());
        }

        Customer updated = customerRepository.save(customer);
        return mapToDTO(updated);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.softDelete();
        customerRepository.save(customer);
    }

    public Page<CustomerDTO> searchCustomers(String keyword, Pageable pageable) {
        Page<Customer> customers = customerRepository.searchByKeyword(keyword, pageable);
        return customers.map(this::mapToDTO);
    }

    private CustomerDTO mapToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}

