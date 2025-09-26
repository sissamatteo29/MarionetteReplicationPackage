package org.marionette.controlplane.usecases.domain.dto;

import java.util.List;

public record ServiceConfigData (String serviceName, List<ClassConfigData> classConfigs) {

    public ServiceConfigData {
        classConfigs = List.copyOf(classConfigs);
    }

}
