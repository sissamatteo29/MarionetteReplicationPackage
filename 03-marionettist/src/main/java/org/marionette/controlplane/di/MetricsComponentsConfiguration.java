package org.marionette.controlplane.di;

import org.marionette.controlplane.adapters.inbound.metrics.ConfigurablePrometheusClient;
import org.marionette.controlplane.adapters.inbound.metrics.KubernetesPrometheusDiscovery;
import org.marionette.controlplane.adapters.inbound.metrics.MetricsConfiguration;
import org.marionette.controlplane.adapters.inbound.metrics.PrometheusConfigurationResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


@Configuration
@EnableAsync
@EnableScheduling
public class MetricsComponentsConfiguration {

    /**
     * Namespace configuration
     */
    @Bean
    public String namespace() {
        return System.getenv("KUBERNETES_NAMESPACE") != null ? System.getenv("KUBERNETES_NAMESPACE") : "default";
    }

    /**
     * RestTemplate configured for Prometheus API calls
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configure URI template handling to avoid double encoding
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        restTemplate.setUriTemplateHandler(factory);

        return restTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Kubernetes service discovery for Prometheus
     */
    @Bean
    public KubernetesPrometheusDiscovery kubernetesPrometheusDiscovery(
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        return new KubernetesPrometheusDiscovery(restTemplate, objectMapper);
    }

    /**
     * Prometheus configuration resolver - handles all configuration sources
     */
    @Bean
    public PrometheusConfigurationResolver prometheusConfigurationResolver(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            KubernetesPrometheusDiscovery prometheusDiscovery) {
        return new PrometheusConfigurationResolver(restTemplate, objectMapper, prometheusDiscovery);
    }

    /**
     * Configurable Prometheus client for metrics collection
     */
    @Bean
    public ConfigurablePrometheusClient configurablePrometheusClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            PrometheusConfigurationResolver configResolver,
            MetricsConfiguration metricsConfiguration) {
        return new ConfigurablePrometheusClient(restTemplate, objectMapper, configResolver, metricsConfiguration);
    }
}