package com.outfit.ui.__marionette;

import java.util.ArrayList;
import java.util.List;

public class RuntimeMethodConfig {

    private String currentBehaviourId;

    private List<String> availableBehaviourIds = new ArrayList<>();

    public RuntimeMethodConfig(String currentBehaviourId, List<String> availableBehaviourIds) {
        this.currentBehaviourId = currentBehaviourId;
        this.availableBehaviourIds = availableBehaviourIds;
    }

    public String getCurrentBehaviourId() {
        return currentBehaviourId;
    }

    public List<String> getAvailableBehaviourIds() {
        return availableBehaviourIds;
    }

    public void setCurrentBehaviourId(String currentBehaviourId) {
        this.currentBehaviourId = currentBehaviourId;
    }

    public void setAvailableBehaviourIds(List<String> availableBehaviourIds) {
        this.availableBehaviourIds = availableBehaviourIds;
    }
}
