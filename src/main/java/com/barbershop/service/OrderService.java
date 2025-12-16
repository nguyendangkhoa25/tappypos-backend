package com.barbershop.service;

import com.barbershop.model.dto.customer.CreateCustomerRequest;
import com.barbershop.model.dto.customer.CustomerDTO;
import com.barbershop.model.dto.order.*;
import com.barbershop.model.entity.*;
import com.barbershop.repository.EmployeeRepository;
import com.barbershop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerService customerService;

    public OrderDTO createOrder(CreateOrderRequest request) {
        // Get or create customer
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = orderRepository.findByIdActive(request.getCustomerId())
                    .map(Order::getCustomer)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
        } else {
            CreateCustomerRequest custRequest = CreateCustomerRequest.builder()
                    .name(request.getCustomerName())
                    .phone(request.getCustomerPhone())
                    .email(request.getCustomerEmail())
                    .build();
            CustomerDTO custDTO = customerService.getOrCreateCustomer(custRequest);
            customer = new Customer();
            customer.setId(custDTO.getId());
            customer.setName(custDTO.getName());
            customer.setPhone(custDTO.getPhone());
            customer.setEmail(custDTO.getEmail());
        }

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            totalAmount = request.getOrderItems().stream()
                    .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Order order = Order.builder()
                .customer(customer)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .notes(request.getNotes())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Add order items
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = request.getOrderItems().stream()
                    .map(item -> OrderItem.builder()
                            .order(savedOrder)
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                            .build())
                    .collect(Collectors.toList());
            savedOrder.setOrderItems(orderItems);
        }

        Order finalOrder = orderRepository.save(savedOrder);
        return mapToDTO(finalOrder);
    }

    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAllActive(pageable);
        return orders.map(this::mapToDTO);
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToDTO(order);
    }

    public OrderDTO assignOrderToEmployee(Long orderId, AssignOrderRequest request) {
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Employee employee = employeeRepository.findByIdActive(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        order.setAssignedEmployee(employee);
        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public OrderDTO completeOrder(Long orderId) {
        Order order = orderRepository.findByIdActive(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.complete();

        // Update employee earnings
        if (order.getAssignedEmployee() != null) {
            Employee employee = order.getAssignedEmployee();
            if (employee.getTotalEarned() == null) {
                employee.setTotalEarned(BigDecimal.ZERO);
            }
            employee.setTotalEarned(employee.getTotalEarned().add(order.getTotalAmount()));
            employeeRepository.save(employee);
        }

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public OrderDTO updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

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

        Order updated = orderRepository.save(order);
        return mapToDTO(updated);
    }

    public void deleteOrder(Long id) {
        Order order = orderRepository.findByIdActive(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        order.softDelete();
        orderRepository.save(order);
    }

    public Page<OrderDTO> searchOrders(String keyword, Pageable pageable) {
        Page<Order> orders = orderRepository.searchByCustomerKeyword(keyword, pageable);
        return orders.map(this::mapToDTO);
    }

    public Page<OrderDTO> getOrdersByStatus(String status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        return orders.map(this::mapToDTO);
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
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .orderItems(order.getOrderItems().stream()
                        .map(item -> OrderItemDTO.builder()
                                .id(item.getId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

