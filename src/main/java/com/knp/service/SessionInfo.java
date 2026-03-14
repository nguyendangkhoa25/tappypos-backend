package com.knp.service;

import java.time.LocalDateTime;

public record SessionInfo(
        String sessionId,
        String ipAddress,
        String userAgent,
        LocalDateTime loginAt
) {}
