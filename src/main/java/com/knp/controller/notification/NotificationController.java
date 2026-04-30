package com.knp.controller.notification;

import com.knp.model.dto.ApiResponse;
import com.knp.model.dto.notification.CreateNotificationRequest;
import com.knp.model.dto.notification.NotificationDTO;
import com.knp.model.entity.notification.Notification;
import com.knp.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@RequiresFeature("NOTIFICATION")
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /notifications?page=0&size=20&type=MARKETING */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Notification.NotificationType notifType = null;
        if (type != null) {
            try { notifType = Notification.NotificationType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException ignored) { }
        }
        Page<NotificationDTO> result = notificationService.getForCurrentUser(
                PageRequest.of(page, size), notifType);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** GET /notifications/unread-count */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    /** POST /notifications  (admin sends to specific users or broadcasts) */
    @PostMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> create(
            @RequestBody @Valid CreateNotificationRequest req) {
        log.info("POST /notifications - title={}, targets={}", req.getTitle(),
                req.getTargetUserIds() == null ? "ALL" : req.getTargetUserIds().size());
        List<NotificationDTO> created = notificationService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    /** PUT /notifications/{id}/read */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDTO>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(id)));
    }

    /** PUT /notifications/read-all */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        int updated = notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated)));
    }

    /** DELETE /notifications/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
