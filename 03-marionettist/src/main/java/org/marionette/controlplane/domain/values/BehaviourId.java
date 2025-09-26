package org.marionette.controlplane.domain.values;

import java.util.Objects;

public class BehaviourId {

    private String behaviourId;

    public BehaviourId(String behaviourId) {
        if(behaviourId.isBlank()) {
            throw new IllegalArgumentException("Trying to create a behaviourId object with an empty string");
        }
        this.behaviourId = Objects.requireNonNull(behaviourId, "Trying to create a behaviourId object passing a null value");
    }

    public String getBehaviourId() {
        return this.behaviourId;
    }

    public void setBehaviourId(String behaviourId) {
        if(behaviourId.isBlank()) {
            throw new IllegalArgumentException("Trying to set a behaviourId with an empty string");
        }
        this.behaviourId = Objects.requireNonNull(behaviourId, "Trying to set a behaviourId passing a null value");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((behaviourId == null) ? 0 : behaviourId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BehaviourId other = (BehaviourId) obj;
        if (behaviourId == null) {
            if (other.behaviourId != null)
                return false;
        } else if (!behaviourId.equals(other.behaviourId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BehaviourId [behaviourId=" + behaviourId + "]";
    }
    
}
