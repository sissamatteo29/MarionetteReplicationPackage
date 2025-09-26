package org.marionette.controlplane.usecases.domain.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.marionette.controlplane.domain.entities.ClassConfig;
import org.marionette.controlplane.domain.entities.MethodConfig;
import org.marionette.controlplane.domain.entities.ServiceConfig;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;
import org.marionette.controlplane.usecases.domain.dto.ClassConfigData;
import org.marionette.controlplane.usecases.domain.dto.MethodConfigData;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;

public class ServiceConfigDataMapper {

    // From data to domain objects
    public static ServiceConfig toDomainServiceConfig(ServiceConfigData serviceConfigData) {
        if (serviceConfigData == null) {
            throw new IllegalArgumentException("ServiceConfigData cannot be null");
        }
        if (serviceConfigData.serviceName() == null) {
            throw new IllegalArgumentException("Service name cannot be null in ServiceConfigData");
        }
        if (serviceConfigData.classConfigs() == null) {
            throw new IllegalArgumentException("Class configs list cannot be null in ServiceConfigData");
        }

        System.out.println("DEBUG: Converting ServiceConfigData - serviceName: " + serviceConfigData.serviceName() + ", classConfigs size: " + serviceConfigData.classConfigs().size());

        ServiceConfig resultingServiceConfig = new ServiceConfig(new ServiceName(serviceConfigData.serviceName()));

        for(ClassConfigData rawClassConfig : serviceConfigData.classConfigs()) {
            if (rawClassConfig == null) {
                System.err.println("WARNING: Found null ClassConfigData in list, skipping...");
                continue;
            }
            ClassConfig domainClassConfig = toDomainClassConfig(rawClassConfig);
            resultingServiceConfig = resultingServiceConfig.withAddedClassConfiguration(domainClassConfig.getClassName(), domainClassConfig);
        }

        return resultingServiceConfig;
    }

    private static ClassConfig toDomainClassConfig(ClassConfigData rawClassConfig) {
        if (rawClassConfig == null) {
            throw new IllegalArgumentException("ClassConfigData cannot be null");
        }
        if (rawClassConfig.className() == null) {
            throw new IllegalArgumentException("Class name cannot be null in ClassConfigData");
        }
        if (rawClassConfig.methodConfigData() == null) {
            throw new IllegalArgumentException("Method configs list cannot be null in ClassConfigData");
        }

        System.out.println("DEBUG: Converting ClassConfigData - className: " + rawClassConfig.className() + ", methodConfigs size: " + rawClassConfig.methodConfigData().size());

        ClassConfig resultingClassConfig = new ClassConfig(new ClassName(rawClassConfig.className()));

        for(MethodConfigData rawMethodConfig : rawClassConfig.methodConfigData()) {
            if (rawMethodConfig == null) {
                System.err.println("WARNING: Found null MethodConfigData in list for class " + rawClassConfig.className() + ", skipping...");
                continue;
            }
            MethodConfig domainMethodConfig = toDomainMethodConfig(rawMethodConfig);
            resultingClassConfig = resultingClassConfig.withAddedMethodConfig(domainMethodConfig.getMethodName(), domainMethodConfig);
        }

        return resultingClassConfig;
    }

    private static MethodConfig toDomainMethodConfig(MethodConfigData rawMethodConfig) {
        if (rawMethodConfig == null) {
            throw new IllegalArgumentException("MethodConfigData cannot be null");
        }
        if (rawMethodConfig.methodName() == null) {
            throw new IllegalArgumentException("Method name cannot be null in MethodConfigData");
        }
        if (rawMethodConfig.defaultBehaviourId() == null) {
            throw new IllegalArgumentException("Default behaviour ID cannot be null in MethodConfigData for method: " + rawMethodConfig.methodName());
        }
        if (rawMethodConfig.currentBehaviourId() == null) {
            throw new IllegalArgumentException("Current behaviour ID cannot be null in MethodConfigData for method: " + rawMethodConfig.methodName());
        }
        if (rawMethodConfig.availableBehaviourIds() == null) {
            throw new IllegalArgumentException("Available behaviour IDs cannot be null in MethodConfigData for method: " + rawMethodConfig.methodName());
        }

        System.out.println("DEBUG: Converting MethodConfigData - method: " + rawMethodConfig.methodName() + 
                          ", default: " + rawMethodConfig.defaultBehaviourId() + 
                          ", current: " + rawMethodConfig.currentBehaviourId() + 
                          ", available: " + rawMethodConfig.availableBehaviourIds());

        return MethodConfig.of(
            rawMethodConfig.methodName(),
            rawMethodConfig.defaultBehaviourId(),
            rawMethodConfig.currentBehaviourId(),
            rawMethodConfig.availableBehaviourIds()
        );
    }



    // From domain to data
    public static ServiceConfigData fromDomainServiceConfig(ServiceConfig domainServiceConfig) {

        String serviceName = domainServiceConfig.serviceNameAsString();
        List<ClassConfigData> classConfigs = new ArrayList<>();
        for(Map.Entry<ClassName, ClassConfig> classEntry : domainServiceConfig.getClassConfigurations().entrySet()) {
            ClassConfigData classConfigData = fromDomainClassConfig(classEntry.getValue());
            classConfigs.add(classConfigData);
        }

        return new ServiceConfigData(serviceName, classConfigs);

    }

    private static ClassConfigData fromDomainClassConfig(ClassConfig domainClassConfig) {
        String className = domainClassConfig.classNameAsString();

        List<MethodConfigData> methodConfigs = new ArrayList<>();
        for(Map.Entry<MethodName, MethodConfig> methodEntry : domainClassConfig.getMethodsConfigurations().entrySet()) {
            MethodConfigData methodConfig = fromDomainMethodConfig(methodEntry.getValue());
            methodConfigs.add(methodConfig);
        }

        return new ClassConfigData(className, methodConfigs);
    }

    private static MethodConfigData fromDomainMethodConfig(MethodConfig domainMethodConfig) {
        List<String> availableBehaviourIds = domainMethodConfig.getAvailableBehaviourIds().getBehaviours().stream().map(el -> el.getBehaviourId()).collect(Collectors.toList());
        
        return new MethodConfigData(
            domainMethodConfig.methodNameAsString(),
            domainMethodConfig.defaultBehaviourIdAsString(),
            domainMethodConfig.currentBehaviourAsString(),
            availableBehaviourIds
        );
    }






}