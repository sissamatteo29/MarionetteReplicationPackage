package org.marionette.controlplane.domain.entities;

import static java.util.Objects.requireNonNull;

import java.util.Collection;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.BehaviourIdSet;
import org.marionette.controlplane.domain.values.MethodName;

/***
 * Entity
 */
public class MethodConfig {

    private final MethodName methodName;
    private final BehaviourId defaultBehaviourId;
    private final BehaviourId currentBehaviourId;
    private final BehaviourIdSet availableBehaviourIds;
    
    
    private MethodConfig(MethodName methodName, BehaviourId defaultBehaviourId, BehaviourId currentBehaviourId,
            BehaviourIdSet availableBehaviourIds) {
        this.methodName = requireNonNull(methodName, "The method name cannot be null");
        this.defaultBehaviourId = requireNonNull(defaultBehaviourId,
                "The default behaviour id for the MethodConfig object is a null value");
        this.currentBehaviourId = requireNonNull(currentBehaviourId,
                "The current behaviour id for the MethodConfig object is a null value");
        this.availableBehaviourIds = requireNonNull(availableBehaviourIds,
        "The set of available behaviour ids for the MethodConfig object is a null value");
        
        if (availableBehaviourIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "The list of available behaviour ids in the MethodConfig object contained 0 elements");
        }
        if (!availableBehaviourIds.contains(defaultBehaviourId)) {
            throw new IllegalArgumentException(
                    "The list of available behaviour ids does not contain the default behaviour id");
        }
        if (!availableBehaviourIds.contains(currentBehaviourId)) {
            throw new IllegalArgumentException(
                    "The list of available behaviour ids does not contain the current behaviour id");
        }
    }

    public static MethodConfig of(String methodName, String defaultBehaviourId, String currentBehaviourId,
            Collection<String> availableBehaviourIds) {

        MethodName methodN = new MethodName(methodName);
        BehaviourId defaultBehaviour = new BehaviourId(defaultBehaviourId);
        BehaviourId currentBehaviour = new BehaviourId(currentBehaviourId);
        BehaviourIdSet availableBehaviours = BehaviourIdSet.fromStringCollection(availableBehaviourIds);

        return new MethodConfig(methodN, defaultBehaviour, currentBehaviour, availableBehaviours);

    }

    public static MethodConfig copyOf(MethodConfig other) {

        requireNonNull(other, "Other cannot be null when copying a MethodConfig object");

        return new MethodConfig(other.getMethodName(), other.getDefaultBehaviourId(), other.getCurrentBehaviourId(), other.getAvailableBehaviourIds());
    }


    public MethodConfig withCurrentBehaviourId(BehaviourId newBehaviourId) {

        requireNonNull(newBehaviourId, "The newBehaviourId field cannot be null");

        if(!availableBehaviourIds.contains(newBehaviourId)) {
            throw new IllegalArgumentException("Impossible to find the behaviour id " + newBehaviourId + " among the available ones, which are " + availableBehaviourIds);
        }

        return new MethodConfig(getMethodName(), getDefaultBehaviourId(), newBehaviourId, getAvailableBehaviourIds());
    }

    public MethodName getMethodName() {
        return methodName;
    }

    public String methodNameAsString() {
        return methodName.getMethodName();
    }

    public BehaviourId getDefaultBehaviourId() {
        return defaultBehaviourId;
    }    
    
    public String defaultBehaviourIdAsString() {
        return defaultBehaviourId.getBehaviourId();
    }

    public BehaviourId getCurrentBehaviourId() {
        return currentBehaviourId;
    }

    public String currentBehaviourAsString() {
        return currentBehaviourId.getBehaviourId();
    }

    public BehaviourIdSet getAvailableBehaviourIds() {
        return availableBehaviourIds;
    }

    public int getNumberOfVariations() {
        return availableBehaviourIds.behaviourNumber();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + ((defaultBehaviourId == null) ? 0 : defaultBehaviourId.hashCode());
        result = prime * result + ((currentBehaviourId == null) ? 0 : currentBehaviourId.hashCode());
        result = prime * result + ((availableBehaviourIds == null) ? 0 : availableBehaviourIds.hashCode());
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
        MethodConfig other = (MethodConfig) obj;
        if (methodName == null) {
            if (other.methodName != null)
                return false;
        } else if (!methodName.equals(other.methodName))
            return false;
        if (defaultBehaviourId == null) {
            if (other.defaultBehaviourId != null)
                return false;
        } else if (!defaultBehaviourId.equals(other.defaultBehaviourId))
            return false;
        if (currentBehaviourId == null) {
            if (other.currentBehaviourId != null)
                return false;
        } else if (!currentBehaviourId.equals(other.currentBehaviourId))
            return false;
        if (availableBehaviourIds == null) {
            if (other.availableBehaviourIds != null)
                return false;
        } else if (!availableBehaviourIds.equals(other.availableBehaviourIds))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("MethodConfig{method=%s, default=%s, current=%s, available=%s}", 
            methodName, 
            defaultBehaviourId, 
            currentBehaviourId, 
            availableBehaviourIds);
    }



   

}
