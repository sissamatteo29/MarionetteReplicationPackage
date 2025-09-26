package org.marionette.controlplane.usecases.domain.dto;

import java.util.List;

public record ClassConfigData (String className, List<MethodConfigData> methodConfigData) {

    public ClassConfigData {
        methodConfigData = List.copyOf(methodConfigData);
    }

}
