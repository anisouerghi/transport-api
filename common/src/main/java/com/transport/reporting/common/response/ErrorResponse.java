package com.transport.reporting.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Reponse d'erreur standardisee.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
    private List<String> details;

    public ErrorResponse() {
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> details) {
        return of(status, error, message, null, path, details);
    }

    public static ErrorResponse of(
            int status, String error, String message, String errorCode, String path, List<String> details) {
        ErrorResponse body = new ErrorResponse();
        body.timestamp = Instant.now();
        body.status = status;
        body.error = error;
        body.message = message;
        body.errorCode = errorCode;
        body.path = path;
        body.details = details;
        return body;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
