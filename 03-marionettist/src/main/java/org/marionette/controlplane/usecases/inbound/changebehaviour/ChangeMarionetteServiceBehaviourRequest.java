package org.marionette.controlplane.usecases.inbound.changebehaviour;

import static java.util.Objects.requireNonNull;

public record ChangeMarionetteServiceBehaviourRequest (
    String serviceName, 
    String className, 
    String methodName, 
    String newBehaviourId
) {

    public ChangeMarionetteServiceBehaviourRequest {

        requireNonNull(serviceName, "The service name in the request to change behaviour cannot be null");
        requireNonNull(className, "The class name in the request to change behaviour cannot be null");
        requireNonNull(methodName, "The method name in the request to change behaviour cannot be null");
        requireNonNull(newBehaviourId, "The new behaviour id in the request to change behaviour cannot be null");
        

    }




}
