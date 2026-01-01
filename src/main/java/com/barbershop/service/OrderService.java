package com.barbershop.service;

import com.barbershop.model.dto.customer.CreateCustomerRequest;
import com.barbershop.model.dto.customer.CustomerDTO;
import com.barbershop.model.dto.order.*;
import com.barbershop.model.entity.Customer;
import com.barbershop.model.entity.Employee;
import com.barbershop.model.entity.Order;
import com.barbershop.model.entity.OrderItem;
import com.barbershop.repository.CustomerRepository;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import com.barbershop.repository.OrderItemRepository;
import com.barbershop.repository.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order with request: {}", request);
        // Get or create customer
        Customer customer = getOrCreateCustomer(request);

        // Use values from request (calculated on frontend)
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal commissionAmount = request.getCommissionAmount() != null ? request.getCommissionAmount() : BigDecimal.ZERO;

        // Calculate total from items (items already have tax calculated)
        BigDecimal totalAmount = request.getOrderItems() != null && !request.getOrderItems().isEmpty()
                ? request.getOrderItems().stream()
                .map(item -> item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        // Apply discount and add tax (values from frontend)
        totalAmount = totalAmount.subtract(discountAmount).add(taxAmount);

        // Determine initial status based on startImmediately flag
        Order.OrderStatus initialStatus = (request.getStartImmediately() != null && request.getStartImmediately())
                ? Order.OrderStatus.IN_PROGRESS
                : Order.OrderStatus.PENDING;

        Order order = Order.builder()
                .customer(customer)
                .status(initialStatus)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .taxPercentage(request.getTaxPercentage())
                .taxAmount(taxAmount)
                .commissionAmount(commissionAmount)
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {} in status: {}", savedOrder.getId(), initialStatus);

        // Add order items with tax and commission information
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = processOrderItems(request.getOrderItems(), savedOrder);
            savedOrder.setOrderItems(orderItems);
        }

        Order finalOrder = orderRepository.save(savedOrder);
        return mapToDTO(finalOrder);
    }

    public Page<OrderDTO> getAllOrders(OrderFilterRequest filter, Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(OrderSpecification.withFilters(filter), pageable);
        return orders.map(this::mapToDTO);
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToDTO(order);
    }

    public OrderDTO updateOrder(Long id, UpdateOrderRequest request) {
        log.info("Updating order ID {} with request: {}", id, request);
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Check if order can be modified (not completed or cancelled)
        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot modify a completed or cancelled order");
        }

        if (request.getAssignedEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(request.getAssignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            order.setAssignedEmployee(employee);
        }

        if (request.getStatus() != null) {
            order.setStatus(Order.OrderStatus.valueOf(request.getStatus()));
        }

        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        // Update order items if provided
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            updateOrderItems(order, request.getOrderItems());
            // Recalculate totals using values from frontend
            updateOrderTotals(order, request.getDiscountAmount(), request.getTaxAmount());
        } else if (request.getDiscountAmount() != null || request.getTaxAmount() != null || request.getCommissionAmount() != null) {
            // Update totals if only discount, tax, or commission changed
            updateOrderTotals(order, request.getDiscountAmount(), request.getTaxAmount());
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public OrderDTO assignItemToEmployee(Long orderId, Long itemId, Long employeeId) {
        log.info("Assigning item ID {} of order ID {} to employee ID {}", itemId, orderId, employeeId);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Employee employee = employeeRepository.findByIdActive(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        OrderItem item = order.getOrderItems().stream()
                .filter(oi -> oi.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        // Check if item can be reassigned (not completed)
        if (item.getStatus() == OrderItem.ItemStatus.COMPLETED) {
            throw new RuntimeException("Cannot reassign a completed item");
        }

        item.setAssignedEmployee(employee);
        item.setStatus(OrderItem.ItemStatus.IN_PROGRESS);
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public OrderDTO updateItemStatus(Long orderId, Long itemId, String status) {
        log.info("Updating status of item ID {} in order ID {} to {}", itemId, orderId, status);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        OrderItem item = order.getOrderItems().stream()
                .filter(oi -> oi.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        OrderItem.ItemStatus newStatus = OrderItem.ItemStatus.valueOf(status);
        item.setStatus(newStatus);

        if (newStatus == OrderItem.ItemStatus.COMPLETED) {
            item.setCompletedAt(java.time.LocalDateTime.now());
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public BillPreviewDTO getBillPreview(Long orderId) {
        log.info("Generating bill preview for order ID {}", orderId);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<BillPreviewDTO.BillItemDTO> billItems = new java.util.ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            billItems.add(BillPreviewDTO.BillItemDTO.builder()
                    .itemId(item.getId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getAmount())
                    .taxPercentage(item.getTaxPercentage())
                    .taxAmount(item.getTaxAmount())
                    .build());
            subtotal = subtotal.add(item.getAmount());
        }

        return BillPreviewDTO.builder()
                .orderId(order.getId())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getCustomer().getPhone())
                .items(billItems)
                .subtotal(subtotal)
                .discountAmount(order.getDiscountAmount())
                .taxPercentage(order.getTaxPercentage())
                .taxAmount(order.getTaxAmount())
                .total(order.getTotalAmount())
                .notes(order.getNotes())
                .build();
    }

    public OrderDTO startOrder(Long orderId) {
        log.info("Starting order ID {}", orderId);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Check if order is in PENDING status
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be started. Current status: " + order.getStatus());
        }

        // Change status to IN_PROGRESS
        order.setStatus(Order.OrderStatus.IN_PROGRESS);

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public OrderDTO completeOrder(Long orderId) {
        log.info("Completing order ID {}", orderId);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.complete();

        // Update employee earnings for items with assigned employees
        for (OrderItem item : order.getOrderItems()) {
            if (item.getAssignedEmployee() != null) {
                Employee employee = item.getAssignedEmployee();
                if (employee.getTotalEarned() == null) {
                    employee.setTotalEarned(BigDecimal.ZERO);
                }
                employee.setTotalEarned(employee.getTotalEarned().add(item.getAmount()));
                employeeRepository.save(employee);
            }
            item.setStatus(OrderItem.ItemStatus.COMPLETED);
            item.setCompletedAt(java.time.LocalDateTime.now());
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.softDelete();
        orderRepository.save(order);
    }

    public OrderDTO cancelOrder(Long id, String reason) {
        log.info("Cancelling order ID {} for reason: {}", id, reason);
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed order");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setNotes("Cancelled: " + (reason != null ? reason : "No reason provided"));
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    // Order Items methods
    public Page<OrderItemDTO> getOrderItems(Long employeeId, String status, Pageable pageable) {
        log.info("Getting order items with employeeId: {}, status: {}", employeeId, status);

        Page<OrderItemDTO> result;

        if (employeeId != null && status != null && !status.trim().isEmpty()) {
            // Both filters provided
            try {
                OrderItem.ItemStatus itemStatus = OrderItem.ItemStatus.valueOf(status.toUpperCase());
                result = orderItemRepository.findByAssignedEmployeeIdAndStatus(employeeId, itemStatus, pageable)
                        .map(item -> mapOrderItemToDTO(item, item.getOrder()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
                result = new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0);
            }
        } else if (employeeId != null) {
            // Only employee filter
            result = orderItemRepository.findByAssignedEmployeeId(employeeId, pageable)
                    .map(item -> mapOrderItemToDTO(item, item.getOrder()));
        } else if (status != null && !status.trim().isEmpty()) {
            // Only status filter
            try {
                OrderItem.ItemStatus itemStatus = OrderItem.ItemStatus.valueOf(status.toUpperCase());
                result = orderItemRepository.findByStatus(itemStatus, pageable)
                        .map(item -> mapOrderItemToDTO(item, item.getOrder()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
                result = new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0);
            }
        } else {
            // No filters
            result = orderItemRepository.findAllActive(pageable)
                    .map(item -> mapOrderItemToDTO(item, item.getOrder()));
        }

        return result;
    }

    // Work Queue methods
    public Page<OrderItemDTO> findAllWorkItems(Long employeeId, Pageable pageable) {
        log.info("Finding all work items for employeeId: {}", employeeId);

        Page<OrderItemDTO> result;

        if (employeeId != null) {
            // If employeeId is provided, get items assigned to that employee
            result = orderItemRepository.findByAssignedEmployeeId(employeeId, pageable)
                    .map(item -> mapOrderItemToDTO(item, item.getOrder()));
        } else {
            // If no employeeId, get ALL items from ALL orders
            result = orderItemRepository.findAllActive(pageable)
                    .map(item -> mapOrderItemToDTO(item, item.getOrder()));
        }

        return result;
    }

    @Transactional
    public OrderItemDTO pickupWorkItem(Long itemId, Long employeeId) {
        log.info("Employee ID {} picking up work item ID {}", employeeId, itemId);

        // Find the order item directly
        OrderItem item = orderItemRepository.findByIdWithOrder(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + itemId));

        // Verify item is available (PENDING status and no assigned employee)
        if (item.getStatus() != OrderItem.ItemStatus.PENDING) {
            throw new RuntimeException("Item is not available for pickup. Current status: " + item.getStatus());
        }

        if (item.getAssignedEmployee() != null) {
            throw new RuntimeException("Item is already assigned to employee: " + item.getAssignedEmployee().getName());
        }

        // Assign employee and change status to IN_PROGRESS
        Employee employee = employeeRepository.findByIdActive(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        item.setAssignedEmployee(employee);
        item.setStatus(OrderItem.ItemStatus.IN_PROGRESS);

        OrderItem savedItem = orderItemRepository.save(item);
        return mapOrderItemToDTO(savedItem, savedItem.getOrder());
    }

    @Transactional
    public OrderItemDTO releaseWorkItem(Long itemId) {
        log.info("Releasing work item ID {}", itemId);

        // Find the order item directly
        OrderItem item = orderItemRepository.findByIdWithOrder(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + itemId));

        // Verify item is assigned (either PENDING assigned or IN_PROGRESS)
        if (item.getAssignedEmployee() == null) {
            throw new RuntimeException("Only assigned items can be released");
        }

        // Release item - remove employee assignment and set status back to PENDING
        item.setAssignedEmployee(null);
        item.setStatus(OrderItem.ItemStatus.PENDING);

        OrderItem savedItem = orderItemRepository.save(item);
        return mapOrderItemToDTO(savedItem, savedItem.getOrder());
    }

    @Transactional
    public OrderItemDTO startWorkItem(Long itemId, Long employeeId) {
        log.info("Starting work item ID {} by employee ID {}", itemId, employeeId);

        // Find the order item directly
        OrderItem item = orderItemRepository.findByIdWithOrder(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + itemId));

        // Verify item is assigned to the requesting employee
        if (item.getAssignedEmployee() == null) {
            throw new RuntimeException("Item is not assigned to any employee");
        }

        if (!item.getAssignedEmployee().getId().equals(employeeId)) {
            throw new RuntimeException("Item is not assigned to you");
        }

        // Verify item status is PENDING (assigned but not yet started)
        if (item.getStatus() != OrderItem.ItemStatus.PENDING) {
            throw new RuntimeException("Only pending items can be started. Current status: " + item.getStatus());
        }

        // Change status to IN_PROGRESS
        item.setStatus(OrderItem.ItemStatus.IN_PROGRESS);

        OrderItem savedItem = orderItemRepository.save(item);
        return mapOrderItemToDTO(savedItem, savedItem.getOrder());
    }

    @Transactional
    public OrderItemDTO completeWorkItem(Long itemId, Long employeeId) {
        log.info("Completing work item ID {} by employee ID {}", itemId, employeeId);

        // Find the order item directly
        OrderItem item = orderItemRepository.findByIdWithOrder(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found with id: " + itemId));

        // Verify item is assigned to the requesting employee
        if (item.getAssignedEmployee() == null) {
            throw new RuntimeException("Only assigned items can be completed");
        }

        if (!item.getAssignedEmployee().getId().equals(employeeId)) {
            throw new RuntimeException("You can only complete items assigned to you");
        }

        // Mark item as completed
        item.setStatus(OrderItem.ItemStatus.COMPLETED);
        item.setCompletedAt(java.time.LocalDateTime.now());

        OrderItem savedItem = orderItemRepository.save(item);
        return mapOrderItemToDTO(savedItem, savedItem.getOrder());
    }

    private OrderItemDTO mapOrderItemToDTO(OrderItem item, Order order) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .orderId(order.getId())
                .orderCustomerName(order.getCustomer().getName())
                .orderCustomerPhone(order.getCustomer().getPhone())
                .orderCustomerEmail(order.getCustomer().getEmail())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getAmount())
                .taxPercentage(item.getTaxPercentage())
                .taxAmount(item.getTaxAmount())
                .status(item.getStatus().name())
                .assignedEmployeeId(item.getAssignedEmployee() != null ? item.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(item.getAssignedEmployee() != null ? item.getAssignedEmployee().getName() : null)
                .completedAt(item.getCompletedAt())
                .build();
    }

    /**
     * Get or create a customer for the order
     */
    private Customer getOrCreateCustomer(CreateOrderRequest request) {
        if (request.getCustomerId() != null) {
            return customerRepository.findByIdActive(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
        }

        CreateCustomerRequest custRequest = CreateCustomerRequest.builder()
                .name(request.getCustomerName())
                .phone(request.getCustomerPhone())
                .email(request.getCustomerEmail())
                .build();
        CustomerDTO custDTO = customerService.getOrCreateCustomer(custRequest);
        Customer customer = new Customer();
        customer.setId(custDTO.getId());
        customer.setName(custDTO.getName());
        customer.setPhone(custDTO.getPhone());
        customer.setEmail(custDTO.getEmail());
        log.info("Customer resolved with ID: {}", customer.getId());
        return customer;
    }


    /**
     * Process order items and create OrderItem entities
     */
    private List<OrderItem> processOrderItems(List<CreateOrderItemRequest> itemRequests,
                                              Order order) {
        return itemRequests.stream()
                .map(itemRequest -> createOrderItem(itemRequest, order))
                .collect(Collectors.toList());
    }

    /**
     * Create a single OrderItem entity from request
     * Values are calculated on frontend, backend just stores them
     */
    private OrderItem createOrderItem(CreateOrderItemRequest itemRequest, Order order) {
        OrderItem.OrderItemBuilder builder = OrderItem.builder()
                .order(order)
                .productId(itemRequest.getProductId())
                .productName(itemRequest.getProductName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .amount(itemRequest.getAmount() != null ? itemRequest.getAmount() : BigDecimal.ZERO)
                .amountBeforeTax(itemRequest.getAmountBeforeTax() != null ? itemRequest.getAmountBeforeTax() : BigDecimal.ZERO)
                .taxPercentage(itemRequest.getTaxPercentage() != null ? itemRequest.getTaxPercentage() : BigDecimal.ZERO)
                .taxAmount(itemRequest.getTaxAmount() != null ? itemRequest.getTaxAmount() : BigDecimal.ZERO)
                .commissionRate(itemRequest.getCommissionRate() != null ? itemRequest.getCommissionRate() : BigDecimal.ZERO)
                .commissionAmount(itemRequest.getCommissionAmount() != null ? itemRequest.getCommissionAmount() : BigDecimal.ZERO)
                .status(OrderItem.ItemStatus.PENDING);

        // Assign employee if provided
        if (itemRequest.getAssignedEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(itemRequest.getAssignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + itemRequest.getAssignedEmployeeId()));
            builder.assignedEmployee(employee);
        }

        return builder.build();
    }

    /**
     * Update order items by updating existing ones and adding new ones
     * This avoids JPA orphan deletion issues caused by clearing and recreating the collection
     */
    private void updateOrderItems(Order order, List<CreateOrderItemRequest> itemRequests) {
        if (order.getOrderItems() == null) {
            order.setOrderItems(new java.util.ArrayList<>());
        }

        // Create map of existing items by product ID for matching
        java.util.Map<Long, OrderItem> existingItemsMap = new java.util.HashMap<>();
        for (OrderItem existingItem : order.getOrderItems()) {
            if (existingItem.getProductId() != null) {
                existingItemsMap.put(existingItem.getProductId(), existingItem);
            }
        }

        // Process each item request
        java.util.Set<Long> processedProductIds = new java.util.HashSet<>();
        for (CreateOrderItemRequest itemRequest : itemRequests) {
            processedProductIds.add(itemRequest.getProductId());

            OrderItem existingItem = existingItemsMap.get(itemRequest.getProductId());
            if (existingItem != null) {
                // Update existing item
                updateOrderItem(existingItem, itemRequest);
            } else {
                // Add new item
                OrderItem newItem = createOrderItem(itemRequest, order);
                order.getOrderItems().add(newItem);
            }
        }

        // Remove items that are no longer in the request
        order.getOrderItems().removeIf(item -> !processedProductIds.contains(item.getProductId()));
    }

    /**
     * Update an existing OrderItem with new values
     * Values are calculated on frontend, backend just stores them
     */
    private void updateOrderItem(OrderItem item, CreateOrderItemRequest itemRequest) {
        // Update item fields with values from request (already calculated on frontend)
        item.setProductName(itemRequest.getProductName());
        item.setQuantity(itemRequest.getQuantity());
        item.setUnitPrice(itemRequest.getUnitPrice());
        item.setAmount(itemRequest.getAmount() != null ? itemRequest.getAmount() : BigDecimal.ZERO);
        item.setAmountBeforeTax(itemRequest.getAmountBeforeTax() != null ? itemRequest.getAmountBeforeTax() : BigDecimal.ZERO);
        item.setTaxPercentage(itemRequest.getTaxPercentage() != null ? itemRequest.getTaxPercentage() : BigDecimal.ZERO);
        item.setTaxAmount(itemRequest.getTaxAmount() != null ? itemRequest.getTaxAmount() : BigDecimal.ZERO);
        item.setCommissionRate(itemRequest.getCommissionRate() != null ? itemRequest.getCommissionRate() : BigDecimal.ZERO);
        item.setCommissionAmount(itemRequest.getCommissionAmount() != null ? itemRequest.getCommissionAmount() : BigDecimal.ZERO);

        // Update assigned employee if provided
        if (itemRequest.getAssignedEmployeeId() != null) {
            Employee employee = employeeRepository.findByIdActive(itemRequest.getAssignedEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + itemRequest.getAssignedEmployeeId()));
            item.setAssignedEmployee(employee);
        } else {
            item.setAssignedEmployee(null);
        }
    }

    /**
     * Update order totals based on items, discount, and tax
     * Values are calculated on frontend, backend just stores them
     */
    private void updateOrderTotals(Order order, BigDecimal requestDiscountAmount, BigDecimal requestTaxAmount) {
        List<OrderItem> items = order.getOrderItems() != null ? order.getOrderItems() : new java.util.ArrayList<>();

        BigDecimal subtotal = items.stream()
                .map(OrderItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = requestDiscountAmount != null ? requestDiscountAmount : (order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        BigDecimal taxAmount = requestTaxAmount != null ? requestTaxAmount : (order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO);

        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(taxAmount);

        order.setDiscountAmount(discountAmount);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
    }


    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getCustomer().getPhone())
                .customerEmail(order.getCustomer().getEmail())
                .customerNotes(order.getCustomer().getNotes())
                .customerHairType(order.getCustomer().getHairType())
                .customerPreferredServices(order.getCustomer().getPreferredServices())
                .customerSpecialRequests(order.getCustomer().getSpecialRequests())
                .assignedEmployeeId(order.getAssignedEmployee() != null ? order.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(order.getAssignedEmployee() != null ? order.getAssignedEmployee().getName() : null)
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxPercentage(order.getTaxPercentage())
                .taxAmount(order.getTaxAmount())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .orderItems(order.getOrderItems().stream()
                        .map(item -> OrderItemDTO.builder()
                                .id(item.getId())
                                .orderId(order.getId())
                                .orderCustomerName(order.getCustomer().getName())
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getAmount())
                                .taxPercentage(item.getTaxPercentage())
                                .taxAmount(item.getTaxAmount())
                                .status(item.getStatus().name())
                                .assignedEmployeeId(item.getAssignedEmployee() != null ? item.getAssignedEmployee().getId() : null)
                                .assignedEmployeeName(item.getAssignedEmployee() != null ? item.getAssignedEmployee().getName() : null)
                                .completedAt(item.getCompletedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}