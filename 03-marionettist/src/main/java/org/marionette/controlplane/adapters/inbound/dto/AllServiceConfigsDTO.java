package org.marionette.controlplane.adapters.inbound.dto;

import java.util.List;

public record AllServiceConfigsDTO (List<ServiceConfigDTO> serviceConfigs) {

    public AllServiceConfigsDTO {
        // Defensive null check - if serviceConfigs is null, use empty list
        serviceConfigs = serviceConfigs != null ? List.copyOf(serviceConfigs) : List.of();
    }
    
}
