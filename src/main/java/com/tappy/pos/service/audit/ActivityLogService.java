package com.tappy.pos.service.audit;

import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.enums.ActivityAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    void logAsync(String tenantId, String actorUsername, String actorFullName,
                  ActivityAction action, String targetType, String targetId,
                  String description, String ipAddress);

    Page<ActivityLogDTO> getActivityLogs(String actorUsername, String action,
                                         String targetType,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable);
}
