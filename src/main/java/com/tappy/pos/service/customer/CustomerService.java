package com.tappy.pos.service.customer;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.storage.R2CleanupService;
import com.tappy.pos.service.storage.R2StorageService;
import com.tappy.pos.model.dto.customer.CreateCustomerRequest;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.customer.UpdateCustomerRequest;
import com.tappy.pos.model.entity.customer.Customer;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.customer.CustomerRepository;
import com.tappy.pos.repository.order.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final EntityManager entityManager;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;
    private final R2StorageService r2StorageService;
    private final R2CleanupService r2CleanupService;

    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Request: Create new customer - name: {}, phone: {}, email: {}",
                request.getName(), request.getPhone(), request.getEmail());

        Customer customer = Customer.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .notes(request.getNotes())
                .idNumber(request.getIdNumber())
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
        if (request.getIdNumber() != null) {
            customer.setIdNumber(request.getIdNumber());
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

    /** Returns all customers whose birthday is in {@code month} (1–12), ordered by day ascending. */
    public List<CustomerDTO> getCustomersByBirthdayMonth(int month) {
        return customerRepository.findByBirthdayMonth(month)
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
                .idNumber(customer.getIdNumber())
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
                .avatarUrl(customer.getAvatarUrl())
                .build();
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    /**
     * Upload and store an avatar for a customer.
     * Resizes to 256×256 JPEG (85%), stores in R2 under customers/{tenantId}/{id}.jpg,
     * removes the old file after DB commit (fire-and-forget).
     */
    public CustomerDTO uploadAvatar(Long id, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/jpeg")
                && !contentType.startsWith("image/png")
                && !contentType.startsWith("image/webp"))) {
            throw new BadRequestException(messageService.getMessage("error.user.avatar.invalid.type"));
        }

        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.customer.not.found", id)));

        String oldKey = r2StorageService.keyFromUrl(customer.getAvatarUrl());

        byte[] compressed;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(256, 256)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            compressed = out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to process avatar image for customer: {}", id, e);
            throw new BadRequestException(messageService.getMessage("error.user.avatar.process.failed"));
        }

        String key = "customers/" + tenantContext.getCurrentTenantId() + "/" + id + ".jpg";
        String url = r2StorageService.upload(key, compressed, "image/jpeg");
        customer.setAvatarUrl(url.isBlank() ? null : url + "?v=" + System.currentTimeMillis());
        Customer saved = customerRepository.save(customer);

        r2CleanupService.deleteAsync(oldKey);
        log.info("Customer avatar uploaded — customerId: {}, key: {}", id, key);
        return mapToDTO(saved);
    }

    /**
     * Remove the customer's avatar from R2 and clear the avatarUrl field.
     */
    public CustomerDTO deleteAvatar(Long id) {
        Customer customer = customerRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.getMessage("error.customer.not.found", id)));

        String oldKey = r2StorageService.keyFromUrl(customer.getAvatarUrl());
        customer.setAvatarUrl(null);
        Customer saved = customerRepository.save(customer);

        r2CleanupService.deleteAsync(oldKey);
        log.info("Customer avatar deleted — customerId: {}", id);
        return mapToDTO(saved);
    }

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsSummary(LocalDate from, LocalDate to) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.atTime(LocalTime.MAX);

        long totalCustomers  = customerRepository.countAllActive();
        long activeCustomers = orderRepository.countActiveCustomers(dtFrom, dtTo);
        long newCustomers    = customerRepository.countNewInPeriod(dtFrom, dtTo);
        BigDecimal totalRevenue = orderRepository.getTotalRevenueFromNamedCustomers(dtFrom, dtTo);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        double avgSpend = activeCustomers > 0
                ? totalRevenue.doubleValue() / activeCustomers
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCustomers",  totalCustomers);
        result.put("activeCustomers", activeCustomers);
        result.put("newCustomers",    newCustomers);
        result.put("totalRevenue",    totalRevenue);
        result.put("avgSpend",        avgSpend);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAnalyticsTrend(LocalDate from, LocalDate to,
                                                        String granularity, String metric) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo   = to.atTime(LocalTime.MAX);

        String trunc  = switch (granularity == null ? "day" : granularity) {
            case "week"  -> "week";
            case "month" -> "month";
            case "year"  -> "year";
            default      -> "day";
        };
        String fmt = switch (trunc) {
            case "month" -> "YYYY-MM";
            case "year"  -> "YYYY";
            default      -> "YYYY-MM-DD";
        };

        String sql = switch (metric == null ? "revenue" : metric) {
            case "visits" -> String.format(
                "SELECT TO_CHAR(DATE_TRUNC('%s', o.completed_at), '%s') AS label, COUNT(o.id) AS value " +
                "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                "WHERE o.deleted = false AND o.status = 'COMPLETED' " +
                "AND o.tenant_id = current_setting('app.current_tenant', true) " +
                "AND c.deleted = false AND c.phone != '0000000000' " +
                "AND o.completed_at BETWEEN :from AND :to " +
                "GROUP BY label ORDER BY label", trunc, fmt);
            case "new" -> String.format(
                "SELECT TO_CHAR(DATE_TRUNC('%s', c.created_at), '%s') AS label, COUNT(c.id) AS value " +
                "FROM customers c " +
                "WHERE c.deleted = false AND c.phone != '0000000000' " +
                "AND c.tenant_id = current_setting('app.current_tenant', true) " +
                "AND c.created_at BETWEEN :from AND :to " +
                "GROUP BY label ORDER BY label", trunc, fmt);
            default -> String.format(
                "SELECT TO_CHAR(DATE_TRUNC('%s', o.completed_at), '%s') AS label, " +
                "COALESCE(SUM(o.total_amount), 0) AS value " +
                "FROM orders o JOIN customers c ON o.customer_id = c.id " +
                "WHERE o.deleted = false AND o.status = 'COMPLETED' " +
                "AND o.tenant_id = current_setting('app.current_tenant', true) " +
                "AND c.deleted = false AND c.phone != '0000000000' " +
                "AND o.completed_at BETWEEN :from AND :to " +
                "GROUP BY label ORDER BY label", trunc, fmt);
        };

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("from", dtFrom)
                .setParameter("to", dtTo)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(Map.of("label", row[0].toString(), "value", row[1]));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopCustomersRanking(int limit, boolean allTime,
                                                             LocalDate from, LocalDate to,
                                                             String sortBy) {
        boolean byFrequency = "count".equalsIgnoreCase(sortBy);
        List<Object[]> rows;
        if (allTime) {
            rows = byFrequency
                    ? orderRepository.getTopCustomersAllTimeByFrequency(PageRequest.of(0, limit))
                    : orderRepository.getTopCustomersAllTime(PageRequest.of(0, limit));
        } else {
            LocalDateTime dtFrom = from.atStartOfDay();
            LocalDateTime dtTo   = to.atTime(LocalTime.MAX);
            rows = byFrequency
                    ? orderRepository.getTopCustomersByFrequency(dtFrom, dtTo, PageRequest.of(0, limit))
                    : orderRepository.getTopCustomersByRange(dtFrom, dtTo, PageRequest.of(0, limit));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("name",       row[0] != null ? row[0].toString() : "");
            item.put("orderCount", ((Number) row[1]).longValue());
            item.put("totalSpend", row[2]);
            item.put("customerId", row[3] != null ? row[3].toString() : null);
            result.add(item);
        }
        return result;
    }
}

