package org.marionette.controlplane.domain.entities;

import org.marionette.controlplane.domain.entities.ServiceMetadata.ServiceStatus;
import org.marionette.controlplane.domain.values.BehaviourId;
import org.marionette.controlplane.domain.values.ClassName;
import org.marionette.controlplane.domain.values.MethodName;
import org.marionette.controlplane.domain.values.ServiceName;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class ConfigRegistry {

    private final Map<ServiceName, ServiceConfig> templateConfigurations = new ConcurrentHashMap<>(); // First config
    private final Map<ServiceName, ServiceConfig> runtimeConfigurations = new ConcurrentHashMap<>();
    private final Map<ServiceName, ServiceMetadata> serviceMetadata = new ConcurrentHashMap<>();
    private volatile Instant lastDiscovery = Instant.now();

    public synchronized void addDiscoveredService(ServiceName serviceName, ServiceConfig templateConfig,
            URI endpoint) {

        requireNonNull(serviceName, "The service name cannot be null");
        requireNonNull(templateConfig, "The service configuration cannot be null");
        requireNonNull(endpoint, "The endpoint to contact the service cannot be null");

        templateConfigurations.put(serviceName, templateConfig);
        // If this is the first time we see this service, use template as runtime
        if (!runtimeConfigurations.containsKey(serviceName)) {
            runtimeConfigurations.put(serviceName, ServiceConfig.copyOf(templateConfig));
        }
        serviceMetadata.put(serviceName, ServiceMetadata.discoveredNew(serviceName, endpoint));
        System.out.println("Added service: " + serviceName + " (template stored, runtime preserved)");
    }

    public synchronized void updateRuntimeConfiguration(ServiceName serviceName, ServiceConfig newRuntimeConfig) {

        requireNonNull(serviceName, "The service name cannot be null");
        requireNonNull(newRuntimeConfig, "The runtime configuration for the service cannot be null");

        if (!templateConfigurations.containsKey(serviceName)) {
            throw new IllegalArgumentException("Cannot update runtime config for unknown service: " + serviceName);
        }

        runtimeConfigurations.put(serviceName, newRuntimeConfig);
        updateServiceStatus(serviceName, ServiceStatus.MODIFIED);
    }

    public synchronized void resetToTemplate(ServiceName serviceName) {
        requireNonNull(serviceName, "The service name cannot be null when resetting it to template");
        ServiceConfig template = templateConfigurations.get(serviceName);
        if (template != null) {
            runtimeConfigurations.put(serviceName, ServiceConfig.copyOf(template));
            updateServiceStatus(serviceName, ServiceStatus.RESET_TO_TEMPLATE);
        }
    }

    public synchronized void markServiceUnavailable(ServiceName serviceName) {
        requireNonNull(serviceName, "The service name cannot be null when marking it unavailable");
        updateServiceStatus(serviceName, ServiceStatus.UNAVAILABLE);
    }

    public synchronized void removeService(ServiceName serviceName) {
        templateConfigurations.remove(serviceName);
        runtimeConfigurations.remove(serviceName);
        serviceMetadata.remove(serviceName);
    }

    public void modifyCurrentBehaviourForMethod(ServiceName serviceName, ClassName className, MethodName methodName,
            BehaviourId newBehaviourId) {

        requireNonNull(serviceName,
                "Service name cannot be null when trying to modify the current behaviour of a method in the global configuration");
        requireNonNull(className,
                "Class name cannot be null when trying to modify the current behaviour of a method in the global configuration");
        requireNonNull(methodName,
                "Method name cannot be null when trying to modify the current behaviour of a method in the global configuration");
        requireNonNull(newBehaviourId,
                "The new behaviour id cannot be null when trying to modify the current behaviour of a method in the global configuration");

        if (!runtimeConfigurations.containsKey(serviceName)) {
            throw new IllegalArgumentException("The service " + serviceName + " does not exist in the ConfigRegistry");
        }

        ServiceConfig modifiedServiceConfig = runtimeConfigurations.get(serviceName).withNewBehaviourForMethod(
                className,
                methodName, newBehaviourId);
        runtimeConfigurations.put(serviceName, modifiedServiceConfig);
    }

    public ServiceConfig getRuntimeConfiguration(ServiceName serviceName) {
        return runtimeConfigurations.get(serviceName);
    }

    public ServiceConfig getTemplateConfiguration(ServiceName serviceName) {
        return templateConfigurations.get(serviceName);
    }

    public Map<ServiceName, ServiceConfig> getAllRuntimeConfigurations() {
        return Map.copyOf(runtimeConfigurations);
    }

    public Map<ServiceName, ServiceMetadata> getAllServiceMetadata() {
        return Map.copyOf(serviceMetadata);
    }

    public ServiceMetadata getMetadataOfService(ServiceName serviceName) {
        requireNonNull(serviceName, "The service name cannot be null");
        if(!serviceMetadata.containsKey(serviceName)) {
            System.out.println("Could not find metadata for the service " + serviceName);
        }

        return serviceMetadata.get(serviceName);

    }

    public URI getEndpointOfService(ServiceName serviceName) {
        requireNonNull(serviceName, "The service name cannot be null");
        if(!serviceMetadata.containsKey(serviceName)) {
            System.out.println("Could not find metadata for the service " + serviceName);
        }

        return serviceMetadata.get(serviceName).getEndpoint();

    }

    public boolean isServiceModified(ServiceName serviceName) {
        ServiceConfig template = templateConfigurations.get(serviceName);
        ServiceConfig runtime = runtimeConfigurations.get(serviceName);

        if (template == null || runtime == null)
            return false;

        return !template.equals(runtime);
    }

    public Set<ServiceName> getServicesNeedingDiscovery() {
        return serviceMetadata.entrySet().stream()
                .filter(entry -> entry.getValue().getStatus() == ServiceStatus.UNAVAILABLE ||
                        entry.getValue().getLastSeen().isBefore(Instant.now().minusSeconds(300))) // 5 minutes old
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Instant getLastDiscovery() {
        return lastDiscovery;
    }

    public void updateLastDiscovery() {
        this.lastDiscovery = Instant.now();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ConfigRegistry State ===\n");
        sb.append("Last discovery: ").append(lastDiscovery).append("\n");
        sb.append("Total services: ").append(runtimeConfigurations.size()).append("\n\n");

        runtimeConfigurations.forEach((serviceName, config) -> {
            ServiceMetadata metadata = serviceMetadata.get(serviceName);
            boolean modified = isServiceModified(serviceName);

            sb.append("Service: ").append(serviceName.getServiceName()).append("\n");
            sb.append("  Status: ").append(metadata != null ? metadata.getStatus() : "UNKNOWN").append("\n");
            sb.append("  Modified: ").append(modified ? "YES" : "NO").append("\n");
            sb.append("  Classes: ").append(config.getClassConfigurations().size()).append("\n");
            if (metadata != null) {
                sb.append("  Endpoint: ").append(metadata.getEndpoint()).append("\n");
                sb.append("  Last seen: ").append(metadata.getLastSeen()).append("\n");
            }
            sb.append(" Content: " + config);
            sb.append("\n");
        });

        return sb.toString();
    }

    private void updateServiceStatus(ServiceName serviceName, ServiceStatus status) {
        ServiceMetadata current = serviceMetadata.get(serviceName);
        if (current != null) {
            serviceMetadata.put(serviceName, current.withStatus(status).withLastSeen(Instant.now()));
        }
    }


    public List<ServiceConfig> getAllServiceConfigs() {
        return runtimeConfigurations.values().stream().toList();
    }

    public List<ClassConfig> getClassConfigsForService(ServiceName serviceName) {
        return runtimeConfigurations.get(serviceName).getClassConfigsList();
    }

    public List<MethodConfig> getMethodConfigsForServiceAndClass(ServiceName serviceName, ClassName className) {
        return runtimeConfigurations.get(serviceName).getMethodConfigsForClass(className);
    }

    public BehaviourId getCurrentBehaviourIdForMethod(ServiceName serviceName, ClassName className, MethodName methodName) {
        return runtimeConfigurations.get(serviceName).getCurrentBehaviourIdForMethod(className, methodName);
    }

    /**
     * Flushes all configurations and metadata from the registry.
     * This method is useful when you want to restart discovery from scratch.
     */
    public synchronized void flushAll() {
        System.out.println("🧹 Flushing ConfigRegistry - clearing all configurations and metadata...");
        templateConfigurations.clear();
        runtimeConfigurations.clear();
        serviceMetadata.clear();
        lastDiscovery = Instant.now();
        System.out.println("✅ ConfigRegistry flushed successfully");
    }
}