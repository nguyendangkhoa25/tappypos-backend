package com.tappy.pos.controller.customer;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.customer.CreateCustomerRequest;
import com.tappy.pos.model.dto.customer.CustomerDTO;
import com.tappy.pos.model.dto.customer.UpdateCustomerRequest;
import com.tappy.pos.model.dto.order.OrderDTO;
import com.tappy.pos.service.customer.CustomerService;
import com.tappy.pos.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@RequiresFeature("CUSTOMER")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    /**
     * POST /api/customers
     * Create a new customer
     * <p>
     * Request Body:
     * - name: Customer name (required)
     * - phone: Phone number (required, unique)
     * - email: Email address (optional, unique)
     * - notes: Additional notes (optional)
     * - zaloId: Zalo social ID (optional)
     * - facebookId: Facebook social ID (optional)
     * - preferredServices: Preferred services (optional)
     * - allergiesOrSensitivities: Allergies or sensitivities (optional)
     * - hairType: Hair type (optional)
     * - specialRequests: Special requests (optional)
     * <p>
     * Examples:
     * - POST /api/customers
     *   {
     *     "name": "John Doe",
     *     "phone": "0123456789",
     *     "email": "john@example.com",
     *     "zaloId": "zalo_123",
     *     "facebookId": "fb_123",
     *     "preferredServices": "Haircut, Shave",
     *     "hairType": "Straight",
     *     "specialRequests": "No hot water on face"
     *   }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDTO>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("Endpoint: POST /customers - Create new customer - name: {}, phone: {}, email: {}",
                request.getName(), request.getPhone(), request.getEmail());
        CustomerDTO customer = customerService.createCustomer(request);
        log.info("Response: Customer created with ID: {}", customer.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customer, "Customer created successfully"));
    }

    /**
     * GET /api/customers
     * Get all customers with optional pagination, search, and sorting
     * <p>
     * Query Parameters:
     * - page: Page number (0-based, default: 0)
     * - size: Page size (default: 20)
     * - search: Search term to search in name, phone, email (optional)
     * - sortBy: Sort by field name (e.g., "id", "name", "phone", "createdAt") (optional, default: "id")
     * - sortDirection: Sort direction "ASC" or "DESC" (optional, default: "DESC")
     * <p>
     * Examples:
     * - GET /api/customers?page=0&size=20
     * - GET /api/customers?page=0&size=20&search=john
     * - GET /api/customers?page=0&size=20&sortBy=name&sortDirection=ASC
     * - GET /api/customers?page=0&size=20&search=john&sortBy=createdAt&sortDirection=DESC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerDTO>>> getAllCustomers(
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "sortBy", required = false, defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "DESC") String sortDirection,
            Pageable pageable) {
        log.info("Endpoint: GET /customers - Get all customers with filters - search: {}, sortBy: {}, sortDirection: {}, page: {}, size: {}",
                searchTerm, sortBy, sortDirection, pageable.getPageNumber(), pageable.getPageSize());
        Page<CustomerDTO> customers = customerService.getAllCustomers(searchTerm, sortBy, sortDirection, pageable);
        log.info("Response: Retrieved {} customers from page {}", customers.getContent().size(), pageable.getPageNumber());
        return ResponseEntity.ok(ApiResponse.success(customers, "Customers retrieved successfully"));
    }

    /**
     * GET /api/customers/{id}
     * Get customer details by ID
     * <p>
     * Path Parameters:
     * - id: Customer ID (required)
     * <p>
     * Examples:
     * - GET /api/customers/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable Long id) {
        log.info("Endpoint: GET /customers/{} - Get customer by ID", id);
        CustomerDTO customer = customerService.getCustomerById(id);
        log.info("Response: Customer retrieved - id: {}, name: {}", customer.getId(), customer.getName());
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer retrieved successfully"));
    }

    @GetMapping("/walkin")
    @RequiresFeature({"POS", "PAWN", "ORDER", "CUSTOMER"})
    public ResponseEntity<ApiResponse<CustomerDTO>> getWalkinCustomer() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getWalkinCustomer()));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<CustomerDTO>> checkPhone(@RequestParam String phone) {
        CustomerDTO customer = customerService.findCustomerByPhone(phone);
        return ResponseEntity.ok(ApiResponse.success(customer, "Phone lookup completed"));
    }

    /**
     * PUT /api/customers/{id}
     * Update an existing customer
     * <p>
     * Path Parameters:
     * - id: Customer ID (required)
     * <p>
     * Request Body:
     * - name: Customer name (optional)
     * - email: Email address (optional, unique)
     * - notes: Additional notes (optional)
     * - zaloId: Zalo social ID (optional)
     * - facebookId: Facebook social ID (optional)
     * - preferredServices: Preferred services (optional)
     * - allergiesOrSensitivities: Allergies or sensitivities (optional)
     * - hairType: Hair type (optional)
     * - specialRequests: Special requests (optional)
     * <p>
     * Examples:
     * - PUT /api/customers/1
     *   {
     *     "name": "Jane Doe",
     *     "zaloId": "zalo_456",
     *     "preferredServices": "Styling",
     *     "specialRequests": "Allergic to some products"
     *   }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(
            @PathVariable Long id,
            @RequestBody UpdateCustomerRequest request) {
        log.info("Endpoint: PUT /customers/{} - Update customer", id);
        CustomerDTO customer = customerService.updateCustomer(id, request);
        log.info("Response: Customer updated - id: {}, name: {}", customer.getId(), customer.getName());
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer updated successfully"));
    }

    /**
     * DELETE /api/customers/{id}
     * Delete (soft delete) a customer
     * <p>
     * Path Parameters:
     * - id: Customer ID (required)
     * <p>
     * Examples:
     * - DELETE /api/customers/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        log.info("Endpoint: DELETE /customers/{} - Delete customer", id);
        customerService.deleteCustomer(id);
        log.info("Response: Customer deleted successfully - id: {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<java.util.List<CustomerDTO>>> getRecentCustomers(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Endpoint: GET /customers/recent limit={}", limit);
        return ResponseEntity.ok(ApiResponse.success(customerService.getRecentCustomers(limit), "Recent customers retrieved"));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getCustomerOrders(
            @PathVariable Long id, Pageable pageable) {
        log.info("Endpoint: GET /customers/{}/orders", id);
        Page<OrderDTO> orders = orderService.getOrdersByCustomerId(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Customer orders retrieved successfully"));
    }

    @GetMapping("/{id}/orders/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerOrderSummary(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Endpoint: GET /customers/{}/orders/summary from={} to={}", id, from, to);
        return ResponseEntity.ok(ApiResponse.success(orderService.getCustomerOrderSummary(id, from, to), "OK"));
    }

    @GetMapping("/{id}/orders/chart")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerOrderChart(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        log.info("Endpoint: GET /customers/{}/orders/chart from={} to={} granularity={}", id, from, to, granularity);
        return ResponseEntity.ok(ApiResponse.success(orderService.getCustomerOrderChart(id, from, to, granularity), "OK"));
    }
}

