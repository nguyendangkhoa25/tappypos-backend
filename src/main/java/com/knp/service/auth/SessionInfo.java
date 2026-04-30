package com.knp.service.auth;

import java.time.LocalDateTime;

public record SessionInfo(
        String sessionId,
        String ipAddress,
        String userAgent,
        LocalDateTime loginAt
) {}
