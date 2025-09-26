package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration.PrometheusConfiguration;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain.PrometheusMetricConfig;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto.PrometheusApiResponse;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto.PrometheusQueryData;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.dto.PrometheusResult;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.FetchMarionetteNodesMetricsGateway;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.domain.AggregateMetric;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PrometheusFetchMarionetteNodesMetricsAdapter implements FetchMarionetteNodesMetricsGateway {

    private final PrometheusConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PrometheusFetchMarionetteNodesMetricsAdapter(PrometheusConfiguration config) {
        this.httpClient = createHttpClient();
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<AggregateMetric> fetchMetricsForService(String serviceName, Duration timeSpan, Duration samplingPeriod) {

        List<AggregateMetric> metrics = new ArrayList<>();

        for (PrometheusMetricConfig metricConfig : config.getMetrics()) {
            try {
                // Build the API url
                String query = PrometheusQueryBuilder.buildQuery(
                        config.getPrometheusUrl(),
                        config.getInternalPath(),
                        metricConfig.getQuery(),
                        serviceName,
                        timeSpan,
                        samplingPeriod);

                // Fire the query
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(query))
                        .timeout(Duration.ofSeconds(20))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse JSON response with defensive error handling
                    TypeReference<PrometheusApiResponse<PrometheusQueryData>> typeRef = new TypeReference<PrometheusApiResponse<PrometheusQueryData>>() {
                    };

                    PrometheusApiResponse<PrometheusQueryData> apiResponse;
                    try {
                        apiResponse = objectMapper.readValue(response.body(), typeRef);
                    } catch (Exception parseException) {
                        System.err.println("Failed to parse Prometheus JSON response: " + parseException.getMessage());
                        System.err.println("Raw response body: " + response.body());
                        continue; // Skip this metric and continue with others
                    }

                    // Defensive null check for apiResponse
                    if (apiResponse == null) {
                        System.err.println("Null API response from Prometheus");
                        continue;
                    }

                    logPrometheusResponse(apiResponse);

                    if ("success".equals(apiResponse.getStatus())) {
                        // Convert Prometheus data to your domain objects with null safety
                        try {
                            AggregateMetric metric = convertToAggregateMetric(
                                    apiResponse.getData(), metricConfig);
                            if (metric != null) {
                                metrics.add(metric);
                            }
                        } catch (Exception conversionException) {
                            System.err.println("Failed to convert Prometheus data to AggregateMetric: " + conversionException.getMessage());
                            conversionException.printStackTrace();
                        }
                    } else {
                        // Handle error response
                        System.err.println("Prometheus API error: " + apiResponse.getError());
                    }
                } else {
                    System.err.println("HTTP error: " + response.statusCode());
                }
            } catch (Exception e) {
                System.out.println("Catching exception when sending request out for service " + serviceName);
                e.printStackTrace();
            }

        }

        logResult(metrics);

        return metrics;

    }

    private AggregateMetric convertToAggregateMetric(PrometheusQueryData data,
            PrometheusMetricConfig config) {
        // Convert PrometheusQueryData to your AggregateMetric domain object
        // This will depend on your specific domain model

        if (data == null) {
            System.err.println("PrometheusQueryData is null, cannot convert to AggregateMetric");
            return null;
        }

        if (data.getResult() == null || data.getResult().isEmpty()) {
            System.err.println("No results in Prometheus response data");
            return null;
        }

        // Example conversion logic
        PrometheusResult firstResult = data.getResult().get(0); // Take first instance
        
        if (firstResult == null) {
            System.err.println("First result in Prometheus data is null");
            return null;
        }

        // Log if multiple
        if (data.getResult().size() > 1) {
            System.err.println("The response from prometheus has multiple values, using first one");
        }

        // For instant queries (vector)
        if ("vector".equals(data.getResultType()) && firstResult.getValue() != null) {
            Object[] valueArray = firstResult.getValue();
            if (valueArray != null && valueArray.length >= 2) {
                try {
                    double timestamp = ((Number) valueArray[0]).doubleValue();
                    String value = (String) valueArray[1];

                    // Create your AggregateMetric object
                    return new AggregateMetric(
                            config.getDisplayName(),
                            Double.parseDouble(value),
                            Instant.ofEpochSecond((long) timestamp),
                            config.getUnit());
                } catch (Exception e) {
                    System.err.println("Error converting Prometheus value array to AggregateMetric: " + e.getMessage());
                    return null;
                }
            } else {
                System.err.println("Invalid or incomplete value array in Prometheus response");
                return null;
            }
        }

        // For range queries (matrix) with defensive checks
        List<Object[]> valuesArray = firstResult.getValues();
        if ("matrix".equals(data.getResultType()) && valuesArray != null && !valuesArray.isEmpty()) {
            System.err.println("Received a response from Prometheus of type matrix...");
            // TODO: Implement matrix handling if needed
        }

        return null;
    }

    private void logPrometheusResponse(PrometheusApiResponse<PrometheusQueryData> apiResponse) {
        System.out.println("=== Prometheus API Response ===");
        
        if (apiResponse == null) {
            System.out.println("Status: NULL RESPONSE");
            System.out.println("================================\n");
            return;
        }
        
        System.out.println("Status: " + apiResponse.getStatus());

        if (apiResponse.getData() != null) {
            PrometheusQueryData data = apiResponse.getData();
            System.out.println("Result Type: " + data.getResultType());
            System.out.println("Number of Results: " + (data.getResult() != null ? data.getResult().size() : 0));

            if (data.getResult() != null && !data.getResult().isEmpty()) {
                System.out.println("Sample Result:");
                PrometheusResult firstResult = data.getResult().get(0);

                if (firstResult == null) {
                    System.out.println("  First result is null");
                } else {
                    // Print metric labels with null safety
                    if (firstResult.getMetric() != null) {
                        System.out.print("  Metric Labels: {");
                        firstResult.getMetric().forEach((key, value) -> System.out.print(key + "=" + value + " "));
                        System.out.println("}");
                    } else {
                        System.out.println("  Metric Labels: null");
                    }

                    // Print value/values with defensive null checks
                    if (firstResult.getValue() != null) {
                        Object[] valueArray = firstResult.getValue();
                        if (valueArray != null && valueArray.length >= 2) {
                            System.out.println("  Value: [" + valueArray[0] + ", " + valueArray[1] + "]");
                        } else {
                            System.out.println("  Value: Invalid or incomplete value array");
                        }
                    }

                    // Defensive check for getValues() - store in local variable to avoid multiple calls
                    List<Object[]> valuesArray = firstResult.getValues();
                    if (valuesArray != null) {
                        System.out.println("  Values count: " + valuesArray.size());
                        if (!valuesArray.isEmpty()) {
                            Object[] firstValue = valuesArray.get(0);
                            if (firstValue != null && firstValue.length >= 2) {
                                System.out.println("  First value: [" + firstValue[0] + ", " + firstValue[1] + "]");
                            } else {
                                System.out.println("  First value: Invalid or incomplete value array");
                            }
                        }
                    } else {
                        System.out.println("  Values: null (no range data)");
                    }
                }
            }
        } else {
            System.out.println("Data: null");
        }

        // Print warnings and errors if present
        if (apiResponse.getWarnings() != null && !apiResponse.getWarnings().isEmpty()) {
            System.out.println("Warnings: " + apiResponse.getWarnings());
        }

        if (apiResponse.getError() != null) {
            System.out.println("Error: " + apiResponse.getError());
            System.out.println("Error Type: " + apiResponse.getErrorType());
        }

        System.out.println("================================\n");
    }

    private void logResult(List<AggregateMetric> metrics) {
        System.out.println("=== Final Aggregate Metrics ===");
        System.out.println("Total metrics collected: " + metrics.size());

        if (metrics.isEmpty()) {
            System.out.println("No metrics were successfully converted.");
        } else {
            System.out.println("Metrics Summary:");
            for (int i = 0; i < metrics.size(); i++) {
                AggregateMetric metric = metrics.get(i);
                System.out.printf("  [%d] %s: %.2f %s (at %s)%n",
                        i + 1,
                        metric.name(),
                        metric.value(),
                        metric.unit() != null ? metric.unit() : "units",
                        metric.timestamp().toString());
            }
        }

        System.out.println("===============================\n");
    }

}
