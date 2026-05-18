package com.tappy.pos.controller.audit;

import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
@RequiresFeature("ACTIVITY_LOG")
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final AuthContext authContext;
    private final TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ActivityLogDTO>>> getActivityLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Request: Get activity logs - username={}, action={}, targetType={}, from={}, to={}", username, action, targetType, from, to);
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogDTO> result = activityLogService.getActivityLogs(username, action, targetType, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Activity logs retrieved"));
    }

    @PostMapping("/event")
    public ResponseEntity<ApiResponse<Void>> logEvent(@RequestBody Map<String, String> body) {
        String description = body.getOrDefault("description", "");
        String username = authContext.getCurrentUsername();
        String tenantId = tenantContext.getCurrentTenant() != null
                ? tenantContext.getCurrentTenant().getTenantId() : null;
        activityLogService.logAsync(tenantId, username, username,
                ActivityAction.MOBILE_EVENT, "MOBILE_EVENT", null, description, null);
        return ResponseEntity.ok(ApiResponse.success(null, "Event logged"));
    }
}
