package org.marionette.controlplane.adapters.outbound.changeconfig;

import static java.util.Objects.requireNonNull;

public record InboundChangeBehaviourRequestDTO (String className, String methodName, String behaviourId) {

    public InboundChangeBehaviourRequestDTO {

        // Required fields from API contract
        requireNonNull(className, "The class name in the request to modify behaviour was not present");
        requireNonNull(methodName, "The method name in the request to modify behaviour was not present");
        requireNonNull(behaviourId, "The new behaviour id in the request to modify behaviour was not present");
        
    }
}
