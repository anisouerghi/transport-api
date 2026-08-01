package com.transport.reporting.dto;

/**
 * Résultat métier d'une tentative d'envoi d'e-mail (jamais d'exception technique brute).
 */
public final class EmailSendResult {

    private final boolean success;
    private final String message;
    private final String errorCode;

    private EmailSendResult(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static EmailSendResult ok(String message) {
        return new EmailSendResult(true, message, null);
    }

    public static EmailSendResult fail(String errorCode, String message) {
        return new EmailSendResult(false, message, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
