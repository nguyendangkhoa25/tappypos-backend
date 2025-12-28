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
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order with request: {}", request);
        // Get or create customer
        Customer customer = getOrCreateCustomer(request);

        // Calculate financial totals
        FinancialTotals totals = calculateFinancialTotals(request.getOrderItems(),
                request.getDiscountAmount(), request.getTaxPercentage());

        // Determine initial status based on startImmediately flag
        Order.OrderStatus initialStatus = (request.getStartImmediately() != null && request.getStartImmediately())
                ? Order.OrderStatus.IN_PROGRESS
                : Order.OrderStatus.PENDING;

        Order order = Order.builder()
                .customer(customer)
                .status(initialStatus)
                .totalAmount(totals.totalAmount)
                .discountAmount(totals.discountAmount)
                .taxPercentage(totals.taxPercentage)
                .taxAmount(totals.taxAmount)
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {} in status: {}", savedOrder.getId(), initialStatus);

        // Add order items with tax calculation
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = processOrderItems(request.getOrderItems(), savedOrder, totals.taxPercentage);
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

    public OrderDTO assignOrderToEmployee(Long orderId, AssignOrderRequest request) {
        log.info("Assigning order ID {} to employee ID {}", orderId, request.getEmployeeId());
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        order.setAssignedEmployee(employee);
        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
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
            updateOrderItems(order, request.getOrderItems(), request.getTaxPercentage());
            // Recalculate totals
            updateOrderTotals(order, request.getDiscountAmount(), request.getTaxPercentage());
        } else if (request.getDiscountAmount() != null || request.getTaxPercentage() != null) {
            // Recalculate totals if only discount or tax changed
            updateOrderTotals(order, request.getDiscountAmount(), request.getTaxPercentage());
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
                    .totalPrice(item.getTotalPrice())
                    .taxPercentage(item.getTaxPercentage())
                    .taxAmount(item.getTaxAmount())
                    .build());
            subtotal = subtotal.add(item.getTotalPrice());
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

    public OrderDTO completeOrderWithModifications(Long orderId, CompleteOrderRequest request) {
        log.info("Completing order ID {} with modifications: {}", orderId, request);
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Check if order can be completed
        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("This order is already completed or cancelled");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Update items with modifications if provided
        if (request.getItemUpdates() != null && !request.getItemUpdates().isEmpty()) {
            for (CompleteOrderRequest.OrderItemUpdate itemUpdate : request.getItemUpdates()) {
                OrderItem item = order.getOrderItems().stream()
                        .filter(oi -> oi.getId().equals(itemUpdate.getItemId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemUpdate.getItemId()));

                if (itemUpdate.getQuantity() != null) {
                    item.setQuantity(itemUpdate.getQuantity());
                }
                if (itemUpdate.getUnitPrice() != null) {
                    item.setUnitPrice(itemUpdate.getUnitPrice());
                }

                // Recalculate total price
                item.setTotalPrice(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
                item.setStatus(OrderItem.ItemStatus.COMPLETED);
                item.setCompletedAt(java.time.LocalDateTime.now());

                totalAmount = totalAmount.add(item.getTotalPrice());
            }
        } else {
            // If no item updates, calculate total from all items
            totalAmount = order.getOrderItems().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Mark all remaining items as completed if not already updated
        for (OrderItem item : order.getOrderItems()) {
            if (item.getStatus() != OrderItem.ItemStatus.COMPLETED) {
                item.setStatus(OrderItem.ItemStatus.COMPLETED);
                item.setCompletedAt(java.time.LocalDateTime.now());
            }
        }

        // Apply discount if provided
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : order.getDiscountAmount();
        BigDecimal afterDiscount = totalAmount.subtract(discountAmount);

        // Apply tax if provided
        BigDecimal taxPercentage = request.getTaxPercentage() != null ? request.getTaxPercentage() : order.getTaxPercentage();
        BigDecimal taxAmount = afterDiscount.multiply(taxPercentage != null ? taxPercentage : BigDecimal.ZERO)
                .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);

        // Update order totals
        order.setTotalAmount(afterDiscount.add(taxAmount));
        order.setDiscountAmount(discountAmount);
        order.setTaxPercentage(taxPercentage != null ? taxPercentage : BigDecimal.ZERO);
        order.setTaxAmount(taxAmount);

        // Update order notes if provided
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }

        // Mark order as completed
        order.complete();

        // Update employee earnings
        for (OrderItem item : order.getOrderItems()) {
            if (item.getAssignedEmployee() != null) {
                Employee employee = item.getAssignedEmployee();
                if (employee.getTotalEarned() == null) {
                    employee.setTotalEarned(BigDecimal.ZERO);
                }
                employee.setTotalEarned(employee.getTotalEarned().add(item.getTotalPrice()));
                employeeRepository.save(employee);
            }
        }

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
                employee.setTotalEarned(employee.getTotalEarned().add(item.getTotalPrice()));
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


    public Page<OrderDTO> getOrdersByStatus(String status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        return orders.map(this::mapToDTO);
    }

    public OrderDTO applyDiscountAndTax(Long id, ApplyDiscountTaxRequest request) {
        log.info("Applying discount and tax to order ID {}: {}", id, request);
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        // Check if order can be modified (not completed or cancelled)
        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot modify a completed or cancelled order");
        }

        // Calculate subtotal from items
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply discount and tax
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);

        BigDecimal taxPercentage = request.getTaxPercentage() != null ? request.getTaxPercentage() : BigDecimal.ZERO;
        BigDecimal taxAmount = afterDiscount.multiply(taxPercentage).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);

        // Update order with discount and tax
        order.setDiscountAmount(discountAmount);
        order.setTaxPercentage(taxPercentage);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(afterDiscount.add(taxAmount));

        // Update each item with tax information
        for (OrderItem item : order.getOrderItems()) {
            item.setTaxPercentage(taxPercentage);
            item.setTaxAmount(item.getTotalPrice().multiply(taxPercentage).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
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
     * Calculate financial totals (subtotal, discount, tax, and final total)
     */
    private FinancialTotals calculateFinancialTotals(List<CreateOrderItemRequest> items,
                                                     BigDecimal requestDiscountAmount,
                                                     BigDecimal requestTaxPercentage) {
        // Calculate subtotal
        BigDecimal subtotal = items != null && !items.isEmpty()
                ? items.stream()
                    .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        // Apply discount
        BigDecimal discountAmount = requestDiscountAmount != null ? requestDiscountAmount : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);

        // Calculate tax
        BigDecimal taxPercentage = requestTaxPercentage != null ? requestTaxPercentage : BigDecimal.ZERO;
        BigDecimal taxAmount = afterDiscount.multiply(taxPercentage)
                .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);

        // Calculate total
        BigDecimal totalAmount = afterDiscount.add(taxAmount);

        return new FinancialTotals(subtotal, discountAmount, taxPercentage, taxAmount, totalAmount);
    }

    /**
     * Process order items and create OrderItem entities
     */
    private List<OrderItem> processOrderItems(List<CreateOrderItemRequest> itemRequests,
                                             Order order,
                                             BigDecimal taxPercentage) {
        return itemRequests.stream()
                .map(itemRequest -> createOrderItem(itemRequest, order, taxPercentage))
                .collect(Collectors.toList());
    }

    /**
     * Create a single OrderItem entity from request
     */
    private OrderItem createOrderItem(CreateOrderItemRequest itemRequest, Order order, BigDecimal taxPercentage) {
        // Ensure taxPercentage is not null
        if (taxPercentage == null) {
            taxPercentage = BigDecimal.ZERO;
        }

        // Calculate item total
        BigDecimal itemTotal = itemRequest.getTotalPrice() != null
                ? itemRequest.getTotalPrice()
                : itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity()));

        // Calculate item tax
        BigDecimal itemTax = itemRequest.getTaxAmount() != null
                ? itemRequest.getTaxAmount()
                : itemTotal.multiply(taxPercentage).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);

        OrderItem.OrderItemBuilder builder = OrderItem.builder()
                .order(order)
                .productId(itemRequest.getProductId())
                .productName(itemRequest.getProductName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .totalPrice(itemTotal)
                .taxPercentage(taxPercentage)
                .taxAmount(itemTax)
                .status(OrderItem.ItemStatus.READY);

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
    private void updateOrderItems(Order order, List<CreateOrderItemRequest> itemRequests, BigDecimal requestTaxPercentage) {
        BigDecimal taxPercentage = requestTaxPercentage != null ? requestTaxPercentage : (order.getTaxPercentage() != null ? order.getTaxPercentage() : BigDecimal.ZERO);

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
                updateOrderItem(existingItem, itemRequest, taxPercentage);
            } else {
                // Add new item
                OrderItem newItem = createOrderItem(itemRequest, order, taxPercentage);
                order.getOrderItems().add(newItem);
            }
        }

        // Remove items that are no longer in the request
        order.getOrderItems().removeIf(item -> !processedProductIds.contains(item.getProductId()));
    }

    /**
     * Update an existing OrderItem with new values
     */
    private void updateOrderItem(OrderItem item, CreateOrderItemRequest itemRequest, BigDecimal taxPercentage) {
        // Calculate new totals
        BigDecimal itemTotal = itemRequest.getTotalPrice() != null
                ? itemRequest.getTotalPrice()
                : itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity()));

        BigDecimal itemTax = itemRequest.getTaxAmount() != null
                ? itemRequest.getTaxAmount()
                : itemTotal.multiply(taxPercentage).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);

        // Update item fields
        item.setProductName(itemRequest.getProductName());
        item.setQuantity(itemRequest.getQuantity());
        item.setUnitPrice(itemRequest.getUnitPrice());
        item.setTotalPrice(itemTotal);
        item.setTaxPercentage(taxPercentage);
        item.setTaxAmount(itemTax);

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
     */
    private void updateOrderTotals(Order order, BigDecimal requestDiscountAmount, BigDecimal requestTaxPercentage) {
        List<OrderItem> items = order.getOrderItems() != null ? order.getOrderItems() : new java.util.ArrayList<>();

        BigDecimal subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = requestDiscountAmount != null ? requestDiscountAmount : (order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);
        BigDecimal taxPercentage = requestTaxPercentage != null ? requestTaxPercentage : (order.getTaxPercentage() != null ? order.getTaxPercentage() : BigDecimal.ZERO);
        BigDecimal taxAmount = afterDiscount.multiply(taxPercentage)
                .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalAmount = afterDiscount.add(taxAmount);

        order.setDiscountAmount(discountAmount);
        order.setTaxPercentage(taxPercentage);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);

        // Update item tax amounts if tax percentage changed
        if (requestTaxPercentage != null) {
            for (OrderItem item : items) {
                item.setTaxPercentage(taxPercentage);
                item.setTaxAmount(item.getTotalPrice().multiply(taxPercentage)
                        .divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
            }
        }
    }
    private static class FinancialTotals {
        final BigDecimal subtotal;
        final BigDecimal discountAmount;
        final BigDecimal taxPercentage;
        final BigDecimal taxAmount;
        final BigDecimal totalAmount;

        FinancialTotals(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxPercentage,
                       BigDecimal taxAmount, BigDecimal totalAmount) {
            this.subtotal = subtotal;
            this.discountAmount = discountAmount;
            this.taxPercentage = taxPercentage;
            this.taxAmount = taxAmount;
            this.totalAmount = totalAmount;
        }
    }


    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
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
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
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





