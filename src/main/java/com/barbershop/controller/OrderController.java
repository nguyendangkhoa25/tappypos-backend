package com.barbershop.controller;

import com.barbershop.model.dto.ApiResponse;
import com.barbershop.model.dto.order.*;
import com.barbershop.service.OrderService;
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
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@RequestBody CreateOrderRequest request) {
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrders(
            @RequestBody(required = false) OrderFilterRequest filter,
            Pageable pageable) {
        if (filter == null) {
            filter = new OrderFilterRequest();
        }
        Page<OrderDTO> orders = orderService.getAllOrders(filter, pageable);
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

    @PutMapping("/{orderId}/items/{itemId}/assign")
    public ResponseEntity<ApiResponse<OrderDTO>> assignItemToEmployee(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Long employeeId) {
        OrderDTO order = orderService.assignItemToEmployee(orderId, itemId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(order, "Item assigned successfully"));
    }

    @PutMapping("/{orderId}/items/{itemId}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateItemStatus(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam String status) {
        OrderDTO order = orderService.updateItemStatus(orderId, itemId, status);
        return ResponseEntity.ok(ApiResponse.success(order, "Item status updated successfully"));
    }

    @PutMapping("/{id}/bill-preview")
    public ResponseEntity<ApiResponse<BillPreviewDTO>> getBillPreview(@PathVariable Long id) {
        BillPreviewDTO billPreview = orderService.getBillPreview(id);
        return ResponseEntity.ok(ApiResponse.success(billPreview, "Bill preview retrieved successfully"));
    }

    @PutMapping("/{id}/apply-discount-tax")
    public ResponseEntity<ApiResponse<OrderDTO>> applyDiscountTax(
            @PathVariable Long id,
            @RequestBody ApplyDiscountTaxRequest request) {
        OrderDTO order = orderService.applyDiscountAndTax(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Discount and tax applied successfully"));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<ApiResponse<OrderDTO>> startOrder(@PathVariable Long id) {
        OrderDTO order = orderService.startOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order started successfully"));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderDTO>> completeOrder(@PathVariable Long id) {
        OrderDTO order = orderService.completeOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order completed successfully"));
    }

    @PutMapping("/{id}/complete-with-modifications")
    public ResponseEntity<ApiResponse<OrderDTO>> completeOrderWithModifications(
            @PathVariable Long id,
            @RequestBody CompleteOrderRequest request) {
        OrderDTO order = orderService.completeOrderWithModifications(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order completed successfully"));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        OrderDTO order = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
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
            // Placeholder - will be updated with PDF generation in next step
            byte[] pdfBytes = new byte[0];

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bill_" + id + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getOrdersByStatus(
            @PathVariable String status,
            Pageable pageable) {
        Page<OrderDTO> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }
}
