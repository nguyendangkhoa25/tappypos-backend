package com.tappy.pos.controller.room;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.room.*;
import com.tappy.pos.service.room.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lodging (hotel / motel / homestay) room management — the Room Board, walk-in check-in,
 * in-room folio, and checkout. Additive to POS: checkout produces a normal completed order
 * tagged source=ROOM. Gated by the ROOM feature.
 */
@Slf4j
@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@RequiresFeature("ROOM")
public class RoomController {

    private final RoomService roomService;

    // ── Board / rooms ─────────────────────────────────────────────────────────

    @GetMapping("/board")
    public ResponseEntity<ApiResponse<List<RoomDTO>>> getBoard() {
        return ResponseEntity.ok(ApiResponse.success(roomService.getBoard(), "Sơ đồ phòng"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDTO>> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("POST /rooms - {}", request.getRoomNumber());
        return ResponseEntity.ok(ApiResponse.success(roomService.createRoom(request), "Đã thêm phòng"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomDTO>> updateRoom(@PathVariable Long id,
                                                           @RequestBody CreateRoomRequest request) {
        log.info("PUT /rooms/{}", id);
        return ResponseEntity.ok(ApiResponse.success(roomService.updateRoom(id, request), "Đã cập nhật phòng"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        log.info("DELETE /rooms/{}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá phòng"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomDTO>> setRoomStatus(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body) {
        log.info("PUT /rooms/{}/status - {}", id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success(roomService.setRoomStatus(id, body.get("status")), "Đã cập nhật trạng thái phòng"));
    }

    // ── Stays ───────────────────────────────────────────────────────────────────

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<RoomStayDTO>> checkIn(@Valid @RequestBody CheckInRequest request) {
        log.info("POST /rooms/check-in - room={}", request.getRoomId());
        return ResponseEntity.ok(ApiResponse.success(roomService.checkIn(request), "Đã nhận phòng"));
    }

    @GetMapping("/stays")
    public ResponseEntity<ApiResponse<Page<RoomStayDTO>>> listStays(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                roomService.listStays(status, PageRequest.of(page, size)), "Danh sách lượt thuê"));
    }

    @GetMapping("/stays/{stayId}")
    public ResponseEntity<ApiResponse<RoomStayDTO>> getStay(@PathVariable Long stayId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getStay(stayId), "Chi tiết lượt thuê"));
    }

    // ── Folio ─────────────────────────────────────────────────────────────────

    @PostMapping("/stays/{stayId}/items")
    public ResponseEntity<ApiResponse<RoomStayItemDTO>> addFolioItem(@PathVariable Long stayId,
                                                                     @Valid @RequestBody AddFolioItemRequest request) {
        log.info("POST /rooms/stays/{}/items", stayId);
        return ResponseEntity.ok(ApiResponse.success(roomService.addFolioItem(stayId, request), "Đã thêm dịch vụ vào phòng"));
    }

    @DeleteMapping("/stays/{stayId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeFolioItem(@PathVariable Long stayId, @PathVariable Long itemId) {
        log.info("DELETE /rooms/stays/{}/items/{}", stayId, itemId);
        roomService.removeFolioItem(stayId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá dịch vụ"));
    }

    // ── Checkout / cancel ─────────────────────────────────────────────────────

    @PostMapping("/stays/{stayId}/checkout")
    public ResponseEntity<ApiResponse<RoomStayDTO>> checkout(@PathVariable Long stayId,
                                                             @RequestBody(required = false) CheckoutRequest request) {
        log.info("POST /rooms/stays/{}/checkout", stayId);
        return ResponseEntity.ok(ApiResponse.success(roomService.checkout(stayId, request), "Đã trả phòng"));
    }

    @PostMapping("/stays/{stayId}/cancel")
    public ResponseEntity<ApiResponse<RoomStayDTO>> cancelStay(@PathVariable Long stayId) {
        log.info("POST /rooms/stays/{}/cancel", stayId);
        return ResponseEntity.ok(ApiResponse.success(roomService.cancelStay(stayId), "Đã huỷ lượt thuê"));
    }
}
