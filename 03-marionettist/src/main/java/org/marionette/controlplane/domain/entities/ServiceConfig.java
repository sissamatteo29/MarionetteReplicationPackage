package org.marionette.controlplane.domain.entities;

import java.util.Map;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ServiceConfig {

    private final ServiceName serviceName;
    private final Map<ClassName, ClassConfig> classConfigs;

    public ServiceConfig(ServiceName serviceName) {
        this.serviceName = serviceName;
        this.classConfigs = new HashMap<>();
    }

    public ServiceConfig(ServiceName serviceName, Map<ClassName, ClassConfig> initialConfigs) {
        this.serviceName = serviceName;
        this.classConfigs = new HashMap<>(initialConfigs);   // Immutable contents
    }


    public static ServiceConfig copyOf(ServiceConfig other) {
        requireNonNull(other, "The ServiceConfig other reference cannot be null when trying to copy the content");
        ServiceConfig copy = new ServiceConfig(other.getServiceName());
        copy.classConfigs.putAll(other.getClassConfigurations());
        return copy;
    }

    public ServiceConfig withAddedClassConfiguration(ClassName className, ClassConfig classConfig) {
        requireNonNull(className, "The class name cannot be null");
        requireNonNull(classConfig, "The class configuration cannot be null");

        ServiceConfig copy = initialiseCopy();
        copy.classConfigs.put(className, classConfig);
        return copy;

    }

    public ServiceConfig withAddedAll(Map<ClassName, ClassConfig> classConfigs) {
        requireNonNull(classConfigs, "Trying to add a null map to the service configuration");
        
        ServiceConfig copy = initialiseCopy();
        copy.classConfigs.putAll(classConfigs);
        return copy;

    }

    public ServiceConfig withRemovedClassConfiguration(ClassName className) {
        requireNonNull(className, "The class name cannot be null");

        ensureMapContainsKey(className);

        ServiceConfig copy = initialiseCopy();
        copy.classConfigs.remove(className);
        return copy;
    }

    public ServiceConfig withNewBehaviourForMethod(ClassName className, MethodName methodName,
            BehaviourId newBehaviourId) {
        requireNonNull(className, "The class name cannot be null");
        requireNonNull(methodName, "The method name cannot be null");
        requireNonNull(newBehaviourId, "The behaviour id cannot be null");

        ensureMapContainsKey(className);

        ServiceConfig copy = initialiseCopy();
        ClassConfig modifiedClassConfig = copy.classConfigs.get(className).withNewBehaviourForMethod(methodName, newBehaviourId);
        copy.classConfigs.put(className, modifiedClassConfig);
        return copy;

    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public String serviceNameAsString() {
        return serviceName.toString();
    }

    public Map<ClassName, ClassConfig> getClassConfigurations() {
        return Collections.unmodifiableMap(classConfigs);
    }

    public List<ClassConfig> getClassConfigsList() {
        return classConfigs.values().stream().toList();
    }

    public List<MethodConfig> getMethodConfigsForClass(ClassName className) {
        return classConfigs.get(className).getMethodConfigsList();
    }

    public BehaviourId getCurrentBehaviourIdForMethod(ClassName className, MethodName methodName) {
        return classConfigs.get(className).getCurrentBehaviourIdForMethod(methodName);
    }



    private void ensureMapContainsKey(ClassName className) {
        if (!classConfigs.containsKey(className)) {
            throw new IllegalArgumentException(
                    "The class " + className + " does not exist in the configuration of the service");
        }
    }

    private ServiceConfig initialiseCopy() {
        ServiceConfig copy = new ServiceConfig(getServiceName());
        copy.classConfigs.putAll(getClassConfigurations());
        return copy;
    }

    @Override
    public String toString() {
        if (classConfigs.isEmpty()) {
            return String.format("ServiceConfig{service=%s, classes=empty}", serviceName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ServiceConfig{service=%s, classes=[\n", serviceName));

        classConfigs.forEach((className, classConfig) -> {
            // Indent the class config representation
            String classConfigStr = classConfig.toString().replace("\n", "\n  ");
            sb.append("  ").append(classConfigStr).append("\n");
        });

        sb.append("]}");
        return sb.toString();
    }

}
