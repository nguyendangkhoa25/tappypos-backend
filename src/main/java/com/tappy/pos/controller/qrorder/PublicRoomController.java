package com.tappy.pos.controller.qrorder;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.qrorder.PublicMenuDTO;
import com.tappy.pos.model.dto.qrorder.PublicOrderRequest;
import com.tappy.pos.model.dto.room.GuestRequestRequest;
import com.tappy.pos.model.dto.room.PublicRoomDTO;
import com.tappy.pos.model.dto.room.PublicRoomOrderResult;
import com.tappy.pos.model.dto.room.RoomRequestDTO;
import com.tappy.pos.service.qrorder.PublicRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated in-room guest API. No JWT and no @RequiresFeature — access is gated in the
 * service by checking the tenant has ROOM enabled. The guest page sends X-Tenant-ID (from the URL),
 * so TenantInterceptor sets RLS-scoped tenant context before each call. Mapped under /public/rooms/**.
 */
@RestController
@RequestMapping("/public/rooms")
@RequiredArgsConstructor
public class PublicRoomController {

    private final PublicRoomService publicRoomService;

    @GetMapping("/{qrToken}")
    public ApiResponse<PublicRoomDTO> resolveRoom(@PathVariable String qrToken) {
        return ApiResponse.success(publicRoomService.resolveRoom(qrToken));
    }

    @GetMapping("/{qrToken}/menu")
    public ApiResponse<PublicMenuDTO> getMenu(@PathVariable String qrToken) {
        // qrToken in the path keeps the URL guest-scoped; the menu itself is tenant-wide.
        return ApiResponse.success(publicRoomService.getMenu());
    }

    @PostMapping("/{qrToken}/orders")
    public ApiResponse<PublicRoomOrderResult> submitOrder(@PathVariable String qrToken,
                                                          @Valid @RequestBody PublicOrderRequest request) {
        return ApiResponse.success(publicRoomService.submitOrder(qrToken, request));
    }

    @PostMapping("/{qrToken}/requests")
    public ApiResponse<RoomRequestDTO> submitRequest(@PathVariable String qrToken,
                                                     @Valid @RequestBody GuestRequestRequest request) {
        return ApiResponse.success(publicRoomService.submitRequest(qrToken, request));
    }
}
