package com.knp.service;

import com.knp.model.dto.ActivityLogDTO;
import com.knp.model.enums.ActivityAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    void logAsync(String tenantId, String actorUsername, String actorFullName,
                  ActivityAction action, String targetType, String targetId,
                  String description, String ipAddress);

    Page<ActivityLogDTO> getActivityLogs(String actorUsername, String action,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable);
}
