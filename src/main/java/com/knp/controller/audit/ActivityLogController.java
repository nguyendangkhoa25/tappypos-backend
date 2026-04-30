package com.knp.controller.audit;

import com.knp.model.dto.audit.ActivityLogDTO;
import com.knp.model.dto.ApiResponse;
import com.knp.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import com.knp.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
@RequiresFeature("ACTIVITY_LOG")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

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
}
