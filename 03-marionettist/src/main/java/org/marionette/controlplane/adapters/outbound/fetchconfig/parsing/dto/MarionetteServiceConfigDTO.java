package org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto;

import java.util.List;

public class MarionetteServiceConfigDTO {

    private String serviceName;
    private List<MarionetteClassConfigDTO> classes;
    public String getServiceName() {
        return serviceName;
    }
    public List<MarionetteClassConfigDTO> getClasses() {
        return classes;
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    public void setClasses(List<MarionetteClassConfigDTO> classes) {
        this.classes = classes;
    }

    
}
