package com.tappy.pos.service.audit;

import com.tappy.pos.model.dto.audit.ActivityLogDTO;
import com.tappy.pos.model.enums.ActivityAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    /**
     * Record an activity entry asynchronously (fire-and-forget).
     *
     * <p>The description is supplied as an i18n message <b>key</b> plus its arguments — it is NOT a
     * rendered string. The text is rendered in the reader's locale when the log is listed
     * ({@link #getActivityLogs}). Keys live in {@code i18n/messages_vi.properties} and
     * {@code i18n/messages.properties} under the {@code activity.*} namespace and must stay in sync.
     *
     * <p>Pass already-formatted display strings as {@code args} for numbers/money (e.g. a pre-formatted
     * total) so {@link java.text.MessageFormat} does not re-format them per locale.
     *
     * @param messageKey i18n key, e.g. {@code activity.product.created}
     * @param ipAddress  client IP, or {@code null}
     * @param args       message arguments (entity names, formatted amounts, …)
     */
    void logAsync(String tenantId, String actorUsername, String actorFullName,
                  ActivityAction action, String targetType, String targetId,
                  String messageKey, String ipAddress, Object... args);

    Page<ActivityLogDTO> getActivityLogs(String actorUsername, String action,
                                         String targetType,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable);
}
