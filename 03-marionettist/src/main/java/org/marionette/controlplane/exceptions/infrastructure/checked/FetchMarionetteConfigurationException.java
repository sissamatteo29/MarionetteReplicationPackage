package org.marionette.controlplane.exceptions.infrastructure.checked;

public class FetchMarionetteConfigurationException extends InfrastructureException {

    public FetchMarionetteConfigurationException(String message, String userMessage) {
        super(message, userMessage);
    }

    public FetchMarionetteConfigurationException(String message, Throwable cause, String userMessage) {
        super(message, cause, userMessage);
    }

}
