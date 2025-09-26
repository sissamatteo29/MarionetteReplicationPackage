package org.marionette.controlplane.adapters.outbound.changeconfig;

import static java.util.Objects.requireNonNull;

public record OutboundChangeBehaviourRequestDTO (String className, String methodName, String behaviourId) {

    public OutboundChangeBehaviourRequestDTO {

        // Required fields from API contract
        requireNonNull(className, "The class name in the request to modify behaviour was not present");
        requireNonNull(methodName, "The method name in the request to modify behaviour was not present");
        requireNonNull(behaviourId, "The behaviour id in the request to modify behaviour was not present");
        
    }
}
