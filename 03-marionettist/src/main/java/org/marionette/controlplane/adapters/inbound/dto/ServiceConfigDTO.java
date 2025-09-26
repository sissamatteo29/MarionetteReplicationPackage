package org.marionette.controlplane.adapters.inbound.dto;

import java.util.List;

public record ServiceConfigDTO (String serviceName, List<ClassConfigDTO> classConfigs) {

    public ServiceConfigDTO {
        // Defensive null check - if classConfigs is null, use empty list
        classConfigs = classConfigs != null ? List.copyOf(classConfigs) : List.of();
    }
}
