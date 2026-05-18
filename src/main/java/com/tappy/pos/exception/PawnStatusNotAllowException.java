package com.tappy.pos.exception;

public class PawnStatusNotAllowException extends BusinessException {
    public PawnStatusNotAllowException(String message) {
        super(message);
    }

    public PawnStatusNotAllowException() {
        super("Pawn status does not allow this action");
    }
}
