package org.marionette.controlplane.adapters.outbound.changeconfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.marionette.controlplane.adapters.outbound.changeconfig.KubernetesServiceUrlParser.KubernetesServiceInfo;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ChangeBehaviourData;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ControlMarionetteServiceBehaviourGateway;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Config;

public class ControlMarionetteServiceBehaviourAdapter implements ControlMarionetteServiceBehaviourGateway {

    private final CoreV1Api coreV1Api;
    private final RestTemplate restTemplate;
    private final ControlMarionetteServiceBehaviourConfig config;

    public ControlMarionetteServiceBehaviourAdapter() {
        try {

            this.config = ControlMarionetteServiceBehaviourConfig.defaultConfig();
            // Try to use in-cluster config first, fall back to default config
            ApiClient client;
            try {
                // This works when running inside the cluster
                client = Config.fromCluster();
                System.out.println("Using in-cluster Kubernetes configuration");
            } catch (Exception e) {
                // Fall back to default config (for local development)
                client = Config.defaultClient();
                System.out.println("Using default Kubernetes configuration");
            }

            // Set reasonable timeouts
            client.setConnectTimeout(config.connectionTimeout());
            client.setReadTimeout(config.readTimeout());
            client.setWriteTimeout(config.writeTimeout());

            Configuration.setDefaultApiClient(client);
            this.coreV1Api = new CoreV1Api(client); // Pass the configured client
            this.restTemplate = new RestTemplate();

        } catch (Exception e) {
            System.err.println("Error building ChangeConfigService: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible to build the object ChangeConfigService", e);
        }
    }

    @Override
    public void changeMarionetteServiceBehaviour(String serviceEndpoint, ChangeBehaviourData changeBehaviourData) {
        Optional<KubernetesServiceInfo> k8sInfo = KubernetesServiceUrlParser.parseServiceUrl(serviceEndpoint);

        if (k8sInfo.isPresent()) {
            KubernetesServiceInfo info = k8sInfo.get();

            System.out.println("Extracted the following data from the service endpoint \n"
                + "     namespace: " + info.namespace());


            notifyAllServiceInstances(
                    info.namespace(),
                    changeBehaviourData.serviceName(),
                    changeBehaviourData.className(),
                    changeBehaviourData.methodName(),
                    changeBehaviourData.newBehaviourId());
        } else {
            System.out.println("There was a problem parsing the kubernetes URL endpoint");
        }
    }

    public void notifyAllServiceInstances(String namespace, String serviceName, String className, String methodName,
            String newBehavior) {
        try {
            System.out.println("Looking up service: " + serviceName + " in namespace: " + namespace);

            // Step 1: Get the Service to find its selector
            V1Service service = coreV1Api.readNamespacedService(serviceName, namespace, null);
            Map<String, String> selector = service.getSpec().getSelector();

            if (selector == null || selector.isEmpty()) {
                System.err.println("Service " + serviceName + " has no selector");
                return;
            }

            // Step 2: Convert selector map to label selector string
            String labelSelector = selector.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(","));

            System.out.println("Using label selector from service: " + labelSelector);

            // Step 3: Find pods using the service's selector
            V1PodList podList = coreV1Api.listNamespacedPod(
                    namespace, null, null, null, null,
                    labelSelector, // Use the service's own selector
                    null, null, null, null, null);

            List<V1Pod> runningPods = podList.getItems().stream()
                    .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                    .filter(pod -> pod.getStatus().getPodIP() != null)
                    .collect(Collectors.toList());

            System.out.println("Found " + runningPods.size() + " running pods for service " + serviceName);

            if (runningPods.isEmpty()) {
                System.err.println("No running pods found for service: " + serviceName);
                return;
            }

            notifyPods(runningPods, className, methodName, newBehavior);

        } catch (ApiException e) {
            System.err.println("Kubernetes API error - Code: " + e.getCode() + ", Message: " + e.getMessage());
            System.err.println("Response body: " + e.getResponseBody());

            if (e.getCode() == 404) {
                System.err.println("Service " + serviceName + " not found in namespace " + namespace);
            } else if (e.getCode() == 403) {
                System.err.println("Permission denied. Check RBAC permissions for the service account");
            }
        } catch (Exception e) {
            System.err.println("Error discovering pods for service " + serviceName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyPods(List<V1Pod> pods, String className, String methodName, String newBehavior) {
        OutboundChangeBehaviourRequestDTO request = new OutboundChangeBehaviourRequestDTO(
                className,
                methodName,
                newBehavior);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OutboundChangeBehaviourRequestDTO> entity = new HttpEntity<>(request, headers);

        // Notify all pods in parallel
        List<CompletableFuture<String>> futures = pods.stream()
                .map(pod -> CompletableFuture.supplyAsync(() -> notifySinglePod(pod, entity)))
                .collect(Collectors.toList());

        // Wait for all and report results
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long successCount = results.stream().filter("Success"::equals).count();
        System.out.println("Notification complete: " + successCount + "/" + results.size() + " pods updated");
    }

    
    private String notifySinglePod(V1Pod pod, HttpEntity<OutboundChangeBehaviourRequestDTO> request) {
        String podIP = pod.getStatus().getPodIP();
        String podName = pod.getMetadata().getName();

        // Extract the actual port from pod specification
        Optional<Integer> servicePort = extractServicePort(pod);

        if (servicePort.isEmpty()) {
            System.err.println("Could not determine service port for pod: " + podName);
            return "Failed - No service port found";
        }

        try {
            String podUrl = "http://" + podIP + ":" + servicePort.get() + config.marionetteNodeInternalPath();
            System.out.println("Notifying pod: " + podName + " at URL: " + podUrl);

            String response = restTemplate.postForObject(podUrl, request, String.class);
            System.out.println("Pod " + podName + " (" + podIP + "): " + response);
            return response != null ? response : "Success";

        } catch (Exception e) {
            System.err.println("Failed to notify pod " + podName + ": " + e.getMessage());
            return "Failed";
        }
    }

    private Optional<Integer> extractServicePort(V1Pod pod) {
        if (pod.getSpec() == null || pod.getSpec().getContainers() == null) {
            return Optional.empty();
        }

        // Strategy 1: Look for a port named "http" or "web"
        Optional<Integer> namedPort = pod.getSpec().getContainers().stream()
                .flatMap(container -> container.getPorts() != null ? container.getPorts().stream() : Stream.empty())
                .filter(port -> port.getName() != null)
                .filter(port -> port.getName().equalsIgnoreCase("http") ||
                        port.getName().equalsIgnoreCase("web") ||
                        port.getName().equalsIgnoreCase("marionette"))
                .map(port -> port.getContainerPort())
                .findFirst();

        if (namedPort.isPresent()) {
            return namedPort;
        }

        // Strategy 2: Look for common HTTP ports
        Optional<Integer> commonHttpPort = pod.getSpec().getContainers().stream()
                .flatMap(container -> container.getPorts() != null ? container.getPorts().stream() : Stream.empty())
                .map(port -> port.getContainerPort())
                .filter(port -> isCommonHttpPort(port))
                .findFirst();

        if (commonHttpPort.isPresent()) {
            return commonHttpPort;
        }

        // Strategy 3: Take the first exposed port (last resort)
        return pod.getSpec().getContainers().stream()
                .flatMap(container -> container.getPorts() != null ? container.getPorts().stream() : Stream.empty())
                .map(port -> port.getContainerPort())
                .findFirst();
    }

    private boolean isCommonHttpPort(Integer port) {
        return port != null && (port == 8080 || port == 8000 || port == 3000 || port == 80 || port == 9090);
    }

}
