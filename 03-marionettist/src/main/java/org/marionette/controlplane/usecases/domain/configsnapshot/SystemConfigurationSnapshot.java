package org.marionette.controlplane.usecases.domain.configsnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.marionette.controlplane.domain.entities.ConfigRegistry;

public record SystemConfigurationSnapshot(
    Map<String, ServiceSnapshot> services,  // service name to service snapshot
    Instant capturedAt
) {
    public SystemConfigurationSnapshot {
        services = Map.copyOf(services);
    }
    
    public static SystemConfigurationSnapshot fromConfigRegistry(ConfigRegistry configRegistry) {
        Map<String, ServiceSnapshot> services = configRegistry
            .getAllRuntimeConfigurations()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getServiceName(),
                entry -> ServiceSnapshot.fromServiceConfig(entry.getValue())
            ));
            
        return new SystemConfigurationSnapshot(services, Instant.now());
    }

    public Set<String> getServiceNamesList() {
        return Set.copyOf(services.keySet());
    }

    public ServiceSnapshot getServiceSnapshotByName(String serviceName) {
        return services.get(serviceName);
    }
}