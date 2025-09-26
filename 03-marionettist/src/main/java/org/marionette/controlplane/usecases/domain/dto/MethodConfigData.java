package org.marionette.controlplane.usecases.domain.dto;

import java.util.List;

public record MethodConfigData (String methodName, String defaultBehaviourId, String currentBehaviourId, List<String> availableBehaviourIds) {

    public MethodConfigData {
        availableBehaviourIds = List.copyOf(availableBehaviourIds);
    }

}
