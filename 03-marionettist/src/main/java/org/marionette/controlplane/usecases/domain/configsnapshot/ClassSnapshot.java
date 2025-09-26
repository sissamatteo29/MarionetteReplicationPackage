package org.marionette.controlplane.usecases.domain.configsnapshot;

import java.util.Map;
import java.util.stream.Collectors;

import org.marionette.controlplane.domain.entities.ClassConfig;

public record ClassSnapshot(
    String className,
    Map<String, String> methodBehaviors  // methodName -> currentBehavior (ONLY current state)
) {
    public ClassSnapshot {
        methodBehaviors = Map.copyOf(methodBehaviors);
    }
    
    public static ClassSnapshot fromClassConfig(ClassConfig classConfig) {
        Map<String, String> methodBehaviors = classConfig.getMethodsConfigurations()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getMethodName(),
                entry -> entry.getValue().currentBehaviourAsString()
            ));
            
        return new ClassSnapshot(classConfig.classNameAsString(), methodBehaviors);
    }
}
