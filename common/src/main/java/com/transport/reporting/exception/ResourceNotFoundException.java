package com.transport.reporting.exception;

/**
 * Exception levee lorsqu'une ressource est introuvable.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
