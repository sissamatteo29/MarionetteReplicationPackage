package org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration;

import java.util.List;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.domain.PrometheusMetricConfig;

public class PrometheusConfigurationLoader {

    private final PrometheusUrlDiscovery urlDiscovery;
    private final MetricsConfigurationParser metricsParser;
    private final IncludedServicesParser servicesParser;
    private final ConfigurationLogger logger;

    public PrometheusConfigurationLoader() {
        this.urlDiscovery = new PrometheusUrlDiscovery();
        this.metricsParser = new MetricsConfigurationParser();
        this.servicesParser = new IncludedServicesParser();
        this.logger = new ConfigurationLogger();
    }

    // Constructor for dependency injection (testability)
    public PrometheusConfigurationLoader(PrometheusUrlDiscovery urlDiscovery,
            MetricsConfigurationParser metricsParser,
            IncludedServicesParser servicesParser,
            ConfigurationLogger logger) {
        this.urlDiscovery = urlDiscovery;
        this.metricsParser = metricsParser;
        this.servicesParser = servicesParser;
        this.logger = logger;
    }

    public static PrometheusConfiguration loadFromEnv() {
        return new PrometheusConfigurationLoader().load();
    }

    public PrometheusConfiguration load() {
        logger.logLoadStart();

        try {
            // Step 1: Discover Prometheus URL
            String prometheusUrl = urlDiscovery.discoverPrometheusUrl();
            logger.logUrlDiscoveryResult(prometheusUrl);

            // Step 2: Parse metrics configuration
            List<PrometheusMetricConfig> metricsConfigs = metricsParser.parseMetricsFromEnvironment();
            logger.logMetricsParsingResult(metricsConfigs);

            // Step 3: Parse included services
            List<String> includedServices = servicesParser.parseIncludedServicesFromEnvironment();
            logger.logServicesParsingResult(includedServices);

            // Step 4: Assemble final configuration
            PrometheusConfiguration configuration = new PrometheusConfiguration(
                    prometheusUrl,
                    metricsConfigs,
                    includedServices);

            logger.logFinalConfiguration(configuration);
            return configuration;

        } catch (Exception e) {
            logger.logError("Failed to load Prometheus configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }
}