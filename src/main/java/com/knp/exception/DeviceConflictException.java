package com.knp.exception;

import com.knp.service.auth.SessionInfo;

public class DeviceConflictException extends RuntimeException {

    private final SessionInfo existingSession;

    public DeviceConflictException(SessionInfo existingSession) {
        super("A session is already active from another device");
        this.existingSession = existingSession;
    }

    public SessionInfo getExistingSession() {
        return existingSession;
    }
}
