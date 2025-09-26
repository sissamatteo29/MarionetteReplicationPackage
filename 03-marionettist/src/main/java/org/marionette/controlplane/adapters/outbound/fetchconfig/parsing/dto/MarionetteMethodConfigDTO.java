package org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto;

import java.util.List;

public class MarionetteMethodConfigDTO {

    private String name;
    private String currentBehaviour;
    private List<String> availableBehaviours;
    public String getCurrentBehaviourId() {
        return currentBehaviour;
    }
    public void setCurrentBehaviour(String currentBehaviour) {
        this.currentBehaviour = currentBehaviour;
    }
    public List<String> getAvailableBehaviours() {
        return availableBehaviours;
    }
    public void setAvailableBehaviours(List<String> availableBehaviours) {
        this.availableBehaviours = availableBehaviours;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

}
