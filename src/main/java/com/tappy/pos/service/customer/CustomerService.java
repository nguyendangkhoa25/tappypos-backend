package com.tappy.pos.service.customer;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.model.dto.customer.CreateCustomerRequest;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.customer.UpdateCustomerRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

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
                .idCardNumber(request.getIdCardNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .idCardIssuedDate(request.getIdCardIssuedDate())
                .idCardIssuedPlace(request.getIdCardIssuedPlace())
                .permanentAddress(request.getPermanentAddress())
                .build();
        customer.setTenantId(tenantContext.getCurrentTenantId());

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully - id: {}, name: {}", saved.getId(), saved.getName());

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.CUSTOMER_CREATED, "CUSTOMER", String.valueOf(saved.getId()),
                "Thêm khách hàng: " + saved.getName(), null);

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
        if (request.getIdCardNumber() != null) {
            log.debug("Updating idCardNumber - id: {}", id);
            customer.setIdCardNumber(request.getIdCardNumber());
        }
        if (request.getDateOfBirth() != null) {
            log.debug("Updating dateOfBirth - id: {}", id);
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            log.debug("Updating gender - id: {}", id);
            customer.setGender(request.getGender());
        }
        if (request.getIdCardIssuedDate() != null) {
            log.debug("Updating idCardIssuedDate - id: {}", id);
            customer.setIdCardIssuedDate(request.getIdCardIssuedDate());
        }
        if (request.getIdCardIssuedPlace() != null) {
            log.debug("Updating idCardIssuedPlace - id: {}", id);
            customer.setIdCardIssuedPlace(request.getIdCardIssuedPlace());
        }
        if (request.getPermanentAddress() != null) {
            log.debug("Updating permanentAddress - id: {}", id);
            customer.setPermanentAddress(request.getPermanentAddress());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully - id: {}, name: {}", updated.getId(), updated.getName());

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.CUSTOMER_UPDATED, "CUSTOMER", String.valueOf(updated.getId()),
                "Cập nhật khách hàng: " + updated.getName(), null);

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
        if ("0000000000".equals(customer.getPhone())) {
            throw new BadRequestException(messageService.getMessage("error.customer.cannot.delete.walkin"));
        }
        customer.softDelete();
        customerRepository.save(customer);
        log.info("Customer deleted successfully (soft delete) - id: {}, name: {}", customer.getId(), customer.getName());
    }

    public List<CustomerDTO> getRecentCustomers(int limit) {
        return customerRepository.findTop(PageRequest.of(0, limit))
                .stream().map(this::mapToDTO).collect(java.util.stream.Collectors.toList());
    }

    public Long getCustomerCount() {
        log.info("Request: Get customer count");
        long count = customerRepository.countAllActive();
        log.info("Total active customers: {}", count);
        return count;
    }

    public CustomerDTO getWalkinCustomer() {
        return getCustomerByPhone("0000000000");
    }

    public CustomerDTO findCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone).map(this::mapToDTO).orElse(null);
    }

    public CustomerDTO getCustomerByPhone(String phone) {
        log.info("Request: Get customer by phone - phone: {}", phone);
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    log.error("Customer not found - phone: {}", phone);
                    String errorMessage = messageService.getMessage("error.customer.not.found", phone);
                    return new ResourceNotFoundException(errorMessage);
                });
        log.info("Retrieved customer by phone - id: {}, name: {}", customer.getId(), customer.getName());
        return mapToDTO(customer);
    }

    public CustomerDTO getCustomerByEmail(String email) {
        log.info("Request: Get customer by email - email: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Customer not found - email: {}", email);
                    String errorMessage = messageService.getMessage("error.customer.not.found", email);
                    return new ResourceNotFoundException(errorMessage);
                });
        log.info("Retrieved customer by email - id: {}, name: {}", customer.getId(), customer.getName());
        return mapToDTO(customer);
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
                .idCardNumber(customer.getIdCardNumber())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender())
                .idCardIssuedDate(customer.getIdCardIssuedDate())
                .idCardIssuedPlace(customer.getIdCardIssuedPlace())
                .permanentAddress(customer.getPermanentAddress())
                .createdAt(customer.getCreatedAt())
                .loyaltyPoints(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0)
                .totalSpent(customer.getTotalSpent())
                .walkIn(customer.isWalkIn())
                .build();
    }
}

