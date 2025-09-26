package org.marionette.controlplane.adapters.inbound.dto;

import java.util.List;

public record ClassConfigDTO (String className, List<MethodConfigDTO> methodConfigs) {

    public ClassConfigDTO {
        methodConfigs = methodConfigs != null ? List.copyOf(methodConfigs) : List.of();
    }
}
