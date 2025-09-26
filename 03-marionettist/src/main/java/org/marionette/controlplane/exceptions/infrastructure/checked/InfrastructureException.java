package org.marionette.controlplane.exceptions.infrastructure.checked;

public class InfrastructureException extends Exception {

    private final String userMessage;

    public InfrastructureException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public InfrastructureException(String message, Throwable cause, String userMessage) {
        super(message, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
    
}
