package org.marionette.controlplane.usecases.inbound.readconfigs;

import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.entities.ServiceConfig;
import org.marionette.controlplane.domain.values.ServiceName;
import org.marionette.controlplane.usecases.domain.dto.ServiceConfigData;
import org.marionette.controlplane.usecases.domain.mappers.ServiceConfigDataMapper;
import org.marionette.controlplane.usecases.inbound.ReadAllMarionetteConfigsUseCase;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReadAllMarionetteConfigsUseCaseImpl implements ReadAllMarionetteConfigsUseCase {

    private final ConfigRegistry globalRegistry;

    public ReadAllMarionetteConfigsUseCaseImpl(ConfigRegistry globalRegistry) {
        
        requireNonNull(globalRegistry, "The reference to the global registry cannot be null");

        this.globalRegistry = globalRegistry;
    }

    @Override
    public ReadAllMarionetteConfigsResponse execute() {
        
        List<ServiceConfigData> serviceConfigs = new ArrayList<>();
        for(Map.Entry<ServiceName, ServiceConfig> serviceEntry : globalRegistry.getAllRuntimeConfigurations().entrySet()) {
            ServiceConfigData serviceConfig = ServiceConfigDataMapper.fromDomainServiceConfig(serviceEntry.getValue());
            serviceConfigs.add(serviceConfig);
        }
        
        return new ReadAllMarionetteConfigsResponse(serviceConfigs);

    }
    
}
