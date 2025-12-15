package com.barbershop.controller;

import com.barbershop.model.dto.*;
import com.barbershop.service.OrderService;
import com.barbershop.util.PdfBillGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrders(Pageable pageable) {
        Page<OrderDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrder(
            @PathVariable Long id,
            @RequestBody UpdateOrderRequest request) {
        OrderDTO order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order updated successfully"));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<OrderDTO>> assignOrderToEmployee(
            @PathVariable Long id,
            @RequestBody AssignOrderRequest request) {
        OrderDTO order = orderService.assignOrderToEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order assigned successfully"));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderDTO>> completeOrder(@PathVariable Long id) {
        OrderDTO order = orderService.completeOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order completed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Order deleted successfully"));
    }

    @GetMapping("/{id}/bill")
    public ResponseEntity<byte[]> downloadBill(@PathVariable Long id) {
        try {
            OrderDTO orderDTO = orderService.getOrderById(id);
            // Reconstruct the Order entity for PDF generation
            // This is a simplified approach; in production, you might want to fetch from repository
            byte[] pdfBytes = new byte[0]; // Placeholder - will be updated in next step

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bill_" + id + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> searchOrders(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<OrderDTO> orders = orderService.searchOrders(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Search results retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrdersByStatus(
            @PathVariable String status,
            Pageable pageable) {
        Page<OrderDTO> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }
}

