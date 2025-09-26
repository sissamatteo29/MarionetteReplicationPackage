package org.marionette.controlplane.usecases.inbound.abntest.domain;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;

public record SingleBehaviourSelection(VariationPoint variationPoint, BehaviourId selectedBehaviour) {
    
    public boolean isForService(String serviceName) {
        return variationPoint.serviceName().getServiceName().equals(serviceName);
    }
    
    public boolean isForClass(String className) {
        return variationPoint.className().getClassName().equals(className);
    }
    
    public boolean isForMethod(String methodName) {
        return variationPoint.methodName().getMethodName().equals(methodName);
    }

    public ServiceName getServiceName() {
        return variationPoint.serviceName();
    }

    public ClassName getClassName() {
        return variationPoint.className();
    }

    public MethodName getMethodName() {
        return variationPoint.methodName();
    }
    
    public String getFullMethodPath() {
        return String.format("%s.%s.%s", 
            variationPoint.serviceName().getServiceName(),
            variationPoint.className().getClassName(),
            variationPoint.methodName().getMethodName());
    }
}