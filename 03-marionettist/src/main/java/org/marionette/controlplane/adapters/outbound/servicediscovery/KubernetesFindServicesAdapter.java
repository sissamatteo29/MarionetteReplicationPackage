package org.marionette.controlplane.adapters.outbound.servicediscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.marionette.controlplane.usecases.domain.dto.DiscoveredServiceMetadata;
import org.marionette.controlplane.usecases.outbound.servicediscovery.FindCandidateServicesPort;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.util.Config;

public class KubernetesFindServicesAdapter implements FindCandidateServicesPort {

    private final CoreV1Api coreV1Api;

    public KubernetesFindServicesAdapter() throws RuntimeException {
        try {
            // Auto-configure from cluster (if running inside K8s) or kubeconfig
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            this.coreV1Api = new CoreV1Api();
        } catch (Exception e) {
            throw new RuntimeException("Impossible to create the KubernetesServiceDiscoveryAdapter"); // TODO convert to
                                                                                                      // domain exc
        }
    }

    @Override
    public List<DiscoveredServiceMetadata> findCandidateServices() {

        System.out.println("=== Discovering Microservice Endpoints in Kubernetes cluster ===");

        List<DiscoveredServiceMetadata> discoveredServices = new ArrayList<>();

        for (Map.Entry<String, String> serviceWithEndpoint : getMicroserviceEndpoints().entrySet()) {
            discoveredServices.add(
                    new DiscoveredServiceMetadata(serviceWithEndpoint.getKey(),
                            serviceWithEndpoint.getValue()));
        }

        logDiscoveredServices(discoveredServices);
        
        return discoveredServices;

    }

    public Map<String, String> getMicroserviceEndpoints() {
        Map<String, String> endpoints = new HashMap<>();

        try {
            V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(
                    null, null, null, null, null, null, null, null, null, null);

            for (V1Service service : serviceList.getItems()) {
                String serviceName = service.getMetadata().getName();
                String namespace = service.getMetadata().getNamespace();

                // Skip system services (optional - you might want to include them)
                if (isSystemService(serviceName, namespace)) {
                    continue;
                }

                if (hasPortConfigured(service)) {
                    V1ServicePort port = service.getSpec().getPorts().get(0);
                    String endpoint = formatKubernetesURL(serviceName, namespace, port.getPort());
                    endpoints.put(serviceName, endpoint);
                }
            }
        } catch (ApiException e) {
            System.err.println("Exception when getting services: " + e.getResponseBody());
        }

        return endpoints;
    }

    // Get microservices in a specific namespace (if you know your namespace)
    public Map<String, String> getMicroserviceEndpointsInNamespace(CoreV1Api api, String namespace) {
        Map<String, String> endpoints = new HashMap<>();

        try {
            V1ServiceList serviceList = api.listNamespacedService(
                    namespace, null, null, null, null, null, null, null, null, null, null);

            for (V1Service service : serviceList.getItems()) {
                String serviceName = service.getMetadata().getName();

                if (hasPortConfigured(service)) {
                    V1ServicePort port = service.getSpec().getPorts().get(0);
                    String endpoint = formatKubernetesURL(serviceName, namespace, port.getPort());
                    endpoints.put(serviceName, endpoint);
                }
            }
        } catch (ApiException e) {
            System.err.println("Exception: " + e.getResponseBody());
        }

        return endpoints;
    }

    // Helper method to identify system services you might want to skip
    private boolean isSystemService(String serviceName, String namespace) {
        return "kube-system".equals(namespace) ||
           "kube-public".equals(namespace) ||
           "kube-node-lease".equals(namespace) ||
           "ingress-nginx".equals(namespace) ||  // Add this
           "monitoring".equals(namespace) ||     // Add this
           serviceName.startsWith("kube-") ||
           serviceName.equals("kubernetes");
    }

    private String formatKubernetesURL(String serviceName, String namespace, int portNumber) {
        return String.format("http://%s.%s.svc.cluster.local:%d",
                serviceName, namespace, portNumber);
    }

    private boolean hasPortConfigured(V1Service service) {
        return service.getSpec().getPorts() != null && !service.getSpec().getPorts().isEmpty();
    }

    private void logDiscoveredServices(List<DiscoveredServiceMetadata> discoveredServices) {
        System.out.println("=== DISCOVERED ENDPOINTS ===");

        discoveredServices.forEach((metadata) -> {
            System.out.println(metadata.serviceName() + " -> " + metadata.endpoint());
        });
    }

}
