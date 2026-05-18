package com.tappy.pos.exception;

import com.tappy.pos.service.auth.SessionInfo;

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
