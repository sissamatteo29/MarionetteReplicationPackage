package org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.mapping;

import java.util.ArrayList;
import java.util.List;

import org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto.MarionetteClassConfigDTO;
import org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto.MarionetteMethodConfigDTO;
import org.marionette.controlplane.adapters.outbound.fetchconfig.parsing.dto.MarionetteServiceConfigDTO;
import org.marionette.controlplane.usecases.domain.dto.ClassConfigData;
import org.marionette.controlplane.usecases.domain.dto.MethodConfigData;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;

public class MarionetteConfigMapper {

    public static ServiceConfigData toDomainServiceConfigData(MarionetteServiceConfigDTO marionetteServiceConfigDTO) {
        if (marionetteServiceConfigDTO == null) {
            throw new IllegalArgumentException("MarionetteServiceConfigDTO cannot be null");
        }
        if (marionetteServiceConfigDTO.getServiceName() == null) {
            throw new IllegalArgumentException("Service name cannot be null in MarionetteServiceConfigDTO");
        }
        if (marionetteServiceConfigDTO.getClasses() == null) {
            throw new IllegalArgumentException("Classes list cannot be null in MarionetteServiceConfigDTO");
        }

        System.out.println("DEBUG: Converting MarionetteServiceConfigDTO - serviceName: " + marionetteServiceConfigDTO.getServiceName() + ", classes size: " + marionetteServiceConfigDTO.getClasses().size());

        List<ClassConfigData> classConfigs = new ArrayList<>();
        for (MarionetteClassConfigDTO classConfigDTO : marionetteServiceConfigDTO.getClasses()) {
            if (classConfigDTO == null) {
                System.err.println("WARNING: Found null MarionetteClassConfigDTO in list, skipping...");
                continue;
            }
            classConfigs.add(toDomainClassConfigData(classConfigDTO));
        }
        return new ServiceConfigData(marionetteServiceConfigDTO.getServiceName(), classConfigs);
    }

    private static ClassConfigData toDomainClassConfigData(MarionetteClassConfigDTO classConfigDTO) {
        if (classConfigDTO == null) {
            throw new IllegalArgumentException("MarionetteClassConfigDTO cannot be null");
        }
        if (classConfigDTO.getName() == null) {
            throw new IllegalArgumentException("Class name cannot be null in MarionetteClassConfigDTO");
        }
        if (classConfigDTO.getMethods() == null) {
            throw new IllegalArgumentException("Methods list cannot be null in MarionetteClassConfigDTO");
        }

        String className = classConfigDTO.getName();
        System.out.println("DEBUG: Converting MarionetteClassConfigDTO - className: " + className + ", methods size: " + classConfigDTO.getMethods().size());

        List<MethodConfigData> methodConfigs = new ArrayList<>();
        for(MarionetteMethodConfigDTO methodConfigDTO : classConfigDTO.getMethods()) {
            if (methodConfigDTO == null) {
                System.err.println("WARNING: Found null MarionetteMethodConfigDTO in list for class " + className + ", skipping...");
                continue;
            }
            if (methodConfigDTO.getName() == null) {
                System.err.println("WARNING: Found MarionetteMethodConfigDTO with null name for class " + className + ", skipping...");
                continue;
            }
            if (methodConfigDTO.getCurrentBehaviourId() == null) {
                System.err.println("WARNING: Found MarionetteMethodConfigDTO with null currentBehaviourId for method " + methodConfigDTO.getName() + " in class " + className + ", skipping...");
                continue;
            }
            if (methodConfigDTO.getAvailableBehaviours() == null) {
                System.err.println("WARNING: Found MarionetteMethodConfigDTO with null availableBehaviours for method " + methodConfigDTO.getName() + " in class " + className + ", using empty list...");
                methodConfigDTO.setAvailableBehaviours(new ArrayList<>());
            }

            System.out.println("DEBUG: Converting MarionetteMethodConfigDTO - method: " + methodConfigDTO.getName() + 
                              ", currentBehaviourId: " + methodConfigDTO.getCurrentBehaviourId() + 
                              ", availableBehaviours: " + methodConfigDTO.getAvailableBehaviours());

           methodConfigs.add(new MethodConfigData(
                methodConfigDTO.getName(),
                methodConfigDTO.getCurrentBehaviourId(),
                methodConfigDTO.getCurrentBehaviourId(),        // TODO: expose data from Marionette service
                methodConfigDTO.getAvailableBehaviours()
            ));
        }

        return new ClassConfigData(className, methodConfigs);
    }

}
