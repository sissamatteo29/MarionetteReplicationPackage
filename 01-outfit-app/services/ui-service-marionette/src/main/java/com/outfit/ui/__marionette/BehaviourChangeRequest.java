package com.outfit.ui.__marionette;

public class BehaviourChangeRequest {

    private String className;

    private String methodName;

    private String behaviourId;

    // Getters and setters
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getBehaviourId() {
        return behaviourId;
    }

    public void setBehaviourId(String behaviourId) {
        this.behaviourId = behaviourId;
    }
}
