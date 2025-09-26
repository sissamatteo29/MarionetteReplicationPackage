package org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto;

import java.util.List;

public class MarionetteClassConfigDTO {

    private String name;
    private List<MarionetteMethodConfigDTO> methods;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<MarionetteMethodConfigDTO> getMethods() {
        return methods;
    }
    public void setMethods(List<MarionetteMethodConfigDTO> methods) {
        this.methods = methods;
    }

}
