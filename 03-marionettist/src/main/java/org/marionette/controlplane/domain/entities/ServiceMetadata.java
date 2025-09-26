
package org.marionette.controlplane.domain.entities;

import java.net.URI;
import java.time.Instant;

import org.marionette.controlplane.domain.values.ServiceName;

import static java.util.Objects.requireNonNull;

public class ServiceMetadata {
    private final ServiceName serviceName;
    private final URI endpoint;
    private final Instant lastSeen;
    private final ServiceStatus status;

    private ServiceMetadata(ServiceName serviceName, URI endpoint, Instant lastSeen, ServiceStatus status) {

        requireNonNull(serviceName, "The service name cannot be null");
        requireNonNull(endpoint, "The service endpoint cannot be null");
        requireNonNull(lastSeen, "The timestamp cannot be null");
        requireNonNull(status, "The service status cannot be null");

        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.lastSeen = lastSeen;
        this.status = status;
    }

    public static ServiceMetadata of(String serviceName, String endpointUri, Instant lastSeen, ServiceStatus status) {

        // No need for null checks, code crashes immediately with constructor

        return new ServiceMetadata(
                new ServiceName(serviceName),
                URI.create(endpointUri),
                lastSeen,
                status);
    }

    public static ServiceMetadata of(ServiceName serviceName, URI endpointUri, Instant lastSeen, ServiceStatus status) {
        return new ServiceMetadata(serviceName, endpointUri, lastSeen, status);
    }

    public static ServiceMetadata discoveredNew(ServiceName serviceName, URI endpointUri) {
        return new ServiceMetadata(serviceName, endpointUri, Instant.now(), ServiceStatus.DISCOVERED);
    }

    public ServiceMetadata withStatus(ServiceStatus newStatus) {
        return new ServiceMetadata(serviceName, endpoint, lastSeen, newStatus);
    }

    public ServiceMetadata withLastSeen(Instant newLastSeen) {
        return new ServiceMetadata(serviceName, endpoint, newLastSeen, status);
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public enum ServiceStatus {
        DISCOVERED, // Newly found
        AVAILABLE, // Responding to health checks
        MODIFIED, // Has runtime changes from template
        UNAVAILABLE, // Was found before but not responding now
        RESET_TO_TEMPLATE // Recently reset to template config
    }
}
