package com.tappy.pos.controller.qrorder;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.qrorder.PublicOrderResponse;
import com.tappy.pos.model.dto.qrorder.PublicTableDTO;
import com.tappy.pos.service.qrorder.PublicOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated QR table-ordering API. No JWT and no @RequiresFeature — access is gated in the
 * service by checking the tenant has TABLE_SERVICE enabled. The customer page sends X-Tenant-ID
 * (from the URL), so TenantInterceptor sets RLS-scoped tenant context before each call.
 * Mapped under /public/** (context path /api → /api/public/**, which SecurityConfig permits).
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicOrderController {

    private final PublicOrderService publicOrderService;

    @GetMapping("/tables/{qrToken}")
    public ApiResponse<PublicTableDTO> resolveTable(@PathVariable String qrToken) {
        return ApiResponse.success(publicOrderService.resolveTable(qrToken));
    }

    @GetMapping("/shop")
    public ApiResponse<PublicTableDTO> getShop() {
        return ApiResponse.success(publicOrderService.getShop());
    }

    @GetMapping("/menu")
    public ApiResponse<PublicMenuDTO> getMenu() {
        return ApiResponse.success(publicOrderService.getMenu());
    }

    @PostMapping("/tables/{qrToken}/orders")
    public ApiResponse<PublicOrderResponse> submitOrder(@PathVariable String qrToken,
                                                        @Valid @RequestBody PublicOrderRequest request) {
        return ApiResponse.success(publicOrderService.submitOrder(qrToken, request));
    }

    @PostMapping("/orders")
    public ApiResponse<PublicOrderResponse> submitShopOrder(@Valid @RequestBody PublicOrderRequest request) {
        return ApiResponse.success(publicOrderService.submitShopOrder(request));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<PublicOrderResponse> getOrderStatus(@PathVariable Long orderId) {
        return ApiResponse.success(publicOrderService.getOrderStatus(orderId));
    }
}
