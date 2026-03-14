package com.knp.service;

import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.customer.CreateCustomerRequest;
import com.knp.model.dto.customer.CustomerDTO;
import com.knp.model.dto.customer.UpdateCustomerRequest;
import com.knp.model.entity.Customer;
import com.knp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MessageService messageService;

    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Request: Create new customer - name: {}, phone: {}, email: {}",
                request.getName(), request.getPhone(), request.getEmail());

        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .notes(request.getNotes())
                .zaloId(request.getZaloId())
                .facebookId(request.getFacebookId())
                .preferredServices(request.getPreferredServices())
                .allergiesOrSensitivities(request.getAllergiesOrSensitivities())
                .hairType(request.getHairType())
                .specialRequests(request.getSpecialRequests())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully - id: {}, name: {}", saved.getId(), saved.getName());
        return mapToDTO(saved);
    }

    public Page<CustomerDTO> getAllCustomers(String searchTerm, String sortBy, String sortDirection, Pageable pageable) {
        log.info("Request: Get all customers - search: {}, sortBy: {}, sortDirection: {}, page: {}, size: {}",
                searchTerm, sortBy, sortDirection, pageable.getPageNumber(), pageable.getPageSize());

        // Create Sort object with custom sorting
        Sort.Direction direction = Sort.Direction.fromString(
                sortDirection != null && sortDirection.equalsIgnoreCase("ASC") ? "ASC" : "DESC");
        Sort sort = Sort.by(direction, sortBy != null && !sortBy.trim().isEmpty() ? sortBy : "id");

        // Create new Pageable with custom sort
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Page<Customer> customers;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            log.debug("Searching customers by keyword: {}", searchTerm);
            customers = customerRepository.searchByKeyword(searchTerm, pageableWithSort);
        } else {
            log.debug("Retrieving all active customers");
            customers = customerRepository.findAllActive(pageableWithSort);
        }

        log.info("Retrieved {} customers from page {}", customers.getContent().size(), pageable.getPageNumber());
        return customers.map(this::mapToDTO);
    }

    public CustomerDTO getCustomerById(Long id) {
        log.info("Request: Get customer - id: {}", id);
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Customer not found - id: {}", id);
                    String errorMessage = messageService.getMessage("error.customer.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        log.info("Retrieved customer - id: {}, name: {}", customer.getId(), customer.getName());
        return mapToDTO(customer);
    }

    public CustomerDTO getOrCreateCustomer(CreateCustomerRequest request) {
        log.info("Request: Get or create customer - phone: {}", request.getPhone());
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            return customerRepository.findByPhone(request.getPhone())
                    .map(customer -> {
                        log.debug("Customer already exists - id: {}, phone: {}", customer.getId(), customer.getPhone());
                        return mapToDTO(customer);
                    })
                    .orElseGet(() -> {
                        log.debug("Creating new customer - phone: {}", request.getPhone());
                        return createCustomer(request);
                    });
        }
        log.warn("Phone number not provided, creating new customer");
        return createCustomer(request);
    }

    public CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request) {
        log.info("Request: Update customer - id: {}", id);
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Customer not found for update - id: {}", id);
                    String errorMessage = messageService.getMessage("error.customer.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });

        if (request.getName() != null) {
            log.debug("Updating name - id: {}, old: {}, new: {}", id, customer.getName(), request.getName());
            customer.setName(request.getName());
        }
        if (request.getEmail() != null) {
            log.debug("Updating email - id: {}, old: {}, new: {}", id, customer.getEmail(), request.getEmail());
            customer.setEmail(request.getEmail());
        }
        if (request.getNotes() != null) {
            log.debug("Updating notes - id: {}", id);
            customer.setNotes(request.getNotes());
        }
        if (request.getZaloId() != null) {
            log.debug("Updating zaloId - id: {}", id);
            customer.setZaloId(request.getZaloId());
        }
        if (request.getFacebookId() != null) {
            log.debug("Updating facebookId - id: {}", id);
            customer.setFacebookId(request.getFacebookId());
        }
        if (request.getPreferredServices() != null) {
            log.debug("Updating preferredServices - id: {}", id);
            customer.setPreferredServices(request.getPreferredServices());
        }
        if (request.getAllergiesOrSensitivities() != null) {
            log.debug("Updating allergiesOrSensitivities - id: {}", id);
            customer.setAllergiesOrSensitivities(request.getAllergiesOrSensitivities());
        }
        if (request.getHairType() != null) {
            log.debug("Updating hairType - id: {}", id);
            customer.setHairType(request.getHairType());
        }
        if (request.getSpecialRequests() != null) {
            log.debug("Updating specialRequests - id: {}", id);
            customer.setSpecialRequests(request.getSpecialRequests());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully - id: {}, name: {}", updated.getId(), updated.getName());
        return mapToDTO(updated);
    }

    public void deleteCustomer(Long id) {
        log.info("Request: Delete customer - id: {}", id);
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> {
                    log.error("Customer not found for deletion - id: {}", id);
                    String errorMessage = messageService.getMessage("error.customer.not.found", id);
                    return new ResourceNotFoundException(errorMessage);
                });
        customer.softDelete();
        customerRepository.save(customer);
        log.info("Customer deleted successfully (soft delete) - id: {}, name: {}", customer.getId(), customer.getName());
    }

    private CustomerDTO mapToDTO(Customer customer) {
        log.debug("Converting Customer to DTO - id: {}, name: {}", customer.getId(), customer.getName());
        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .notes(customer.getNotes())
                .zaloId(customer.getZaloId())
                .facebookId(customer.getFacebookId())
                .preferredServices(customer.getPreferredServices())
                .allergiesOrSensitivities(customer.getAllergiesOrSensitivities())
                .hairType(customer.getHairType())
                .specialRequests(customer.getSpecialRequests())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}

