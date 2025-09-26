package org.marionette.controlplane.domain.entities;

import java.util.Map;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;

/**
 * Does not contain duplicate elements following the convention on equality expressed by MethodConfig equals() method 
 * @param methodName
 */
public class ClassConfig {

    private final ClassName className;
    private final Map<MethodName, MethodConfig> methodConfigs;

    public ClassConfig(ClassName className) {
        this.className = className;
        methodConfigs = new HashMap<>();
    }

    public ClassConfig(ClassName className, Map<MethodName, MethodConfig> methodConfigs) {
        this.className = className;

        // Defensive copy, content unmodifiable
        this.methodConfigs = new HashMap<>(methodConfigs);
    }
    
    public static ClassConfig copyOf(ClassConfig other) {
        requireNonNull(other, "Trying to copy a ClassConfig object which is null");

        ClassConfig copy = new ClassConfig(other.getClassName());
        copy.methodConfigs.putAll(other.getMethodsConfigurations());
        return copy;
    }

    public ClassConfig withAddedMethodConfig(MethodName methodName, MethodConfig methodConfig) {
        requireNonNull(methodConfigs, "Trying to add a null MethodConfig object inside a ClassConfig");
        requireNonNull(methodName, "Trying to add a method configuration with a null name to the ClassConfig object");
        
        ClassConfig copy = initialiseCopy();
        copy.methodConfigs.put(methodName, methodConfig);
        return copy;
    }

    public ClassConfig withAddedAll(Map<MethodName, MethodConfig> configurations) {
        requireNonNull(configurations, "Trying to add configurations to a ClassConfig object with a null map");
        
        ClassConfig copy = initialiseCopy();
        copy.methodConfigs.putAll(configurations);
        return copy;
    }

    public ClassConfig withRemovedMethodConfig(MethodName methodName) {
        requireNonNull(methodName, "Trying to remove a MethodConfig object inside a ClassConfig with a null MethodName reference");
        ClassConfig copy = initialiseCopy();
        copy.methodConfigs.remove(methodName);
        return copy;
    }

    public ClassConfig withNewBehaviourForMethod(MethodName method, BehaviourId newBehaviour) {
        requireNonNull(method, "The method name cannot be null");
        requireNonNull(newBehaviour, "The newBehaviour cannot be null");
        if(!methodConfigs.containsKey(method)) {
            throw new IllegalArgumentException("The method with name " + method.getMethodName() + " does not exist in the current class configuration");
        }

        ClassConfig copy = initialiseCopy();
        MethodConfig newMethodConfig = copy.methodConfigs.get(method).withCurrentBehaviourId(newBehaviour);
        copy.methodConfigs.put(method, newMethodConfig);
        return copy;
    }

    public ClassName getClassName() {
        return className;
    }

    public String classNameAsString() {
        return className.getClassName();
    }

    public MethodConfig getMethodConfigByName(MethodName methodName) {
        return methodConfigs.get(methodName);
    }

    public Map<MethodName, MethodConfig> getMethodsConfigurations() {
        return Collections.unmodifiableMap(methodConfigs);       // Immutable view of the map, content of the map immutable by design
    }

    public List<MethodConfig> getMethodConfigsList() {
        return methodConfigs.values().stream().toList();
    }

    public BehaviourId getCurrentBehaviourIdForMethod(MethodName methodName) {
        return methodConfigs.get(methodName).getCurrentBehaviourId();
    }

    private ClassConfig initialiseCopy() {
        ClassConfig copy = new ClassConfig(getClassName());
        copy.methodConfigs.putAll(getMethodsConfigurations());
        return copy;
    }

    @Override
    public String toString() {
        if (methodConfigs.isEmpty()) {
            return String.format("ClassConfig{class=%s, methods=empty}", className);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ClassConfig{class=%s, methods=[\n", className));

        methodConfigs.forEach((methodName, methodConfig) -> {
            sb.append("    ").append(methodConfig).append("\n");
        });

        sb.append("  ]}");
        return sb.toString();
    }

    
}
