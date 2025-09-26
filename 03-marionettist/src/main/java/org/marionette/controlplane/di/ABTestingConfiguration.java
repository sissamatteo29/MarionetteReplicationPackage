package org.marionette.controlplane.di;

import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.PrometheusFetchMarionetteNodesMetricsAdapter;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.PrometheusNonMarionetteNodesTracker;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.PrometheusOrderedMetricsMetadataAdapter;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration.PrometheusConfiguration;
import org.marionette.controlplane.adapters.outbound.fetchmetrics.prometheus.configuration.PrometheusConfigurationLoader;
import org.marionette.controlplane.domain.entities.ConfigRegistry;
import org.marionette.controlplane.domain.entities.abntest.AbnTestResultsStorage;
import org.marionette.controlplane.usecases.inbound.AbnTestAllSystemConfigurationsUseCase;
import org.marionette.controlplane.usecases.inbound.abntest.AbnTestAllSystemConfigurationsUseCaseImpl;
import org.marionette.controlplane.usecases.inbound.abntest.engine.AbnTestExecutor;
import org.marionette.controlplane.usecases.inbound.abntest.engine.NonMarionetteNodesTracker;
import org.marionette.controlplane.usecases.inbound.abntest.engine.SystemConfigurationsGenerator;
import org.marionette.controlplane.usecases.inbound.abntest.engine.UniformAbnTestExecutor;
import org.marionette.controlplane.usecases.inbound.abntest.engine.VariationPointsExtractor;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SystemConfigurationsRanker;
import org.marionette.controlplane.usecases.inbound.abntest.ranking.SystemMetricsAggregator;
import org.marionette.controlplane.usecases.inbound.downloadresult.AbnTestResultsDownloadUseCase;
import org.marionette.controlplane.usecases.inbound.downloadresult.AbnTestResultsDownloadUseCaseImpl;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.FetchMarionetteNodesMetricsGateway;
import org.marionette.controlplane.usecases.outbound.fetchmetrics.OrderedMetricsMetadataProvider;
import org.marionette.controlplane.usecases.outbound.servicemanipulation.ControlMarionetteServiceBehaviourGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ABTestingConfiguration {

    @Bean
    public AbnTestResultsStorage testResultsStorage() {
        return new AbnTestResultsStorage();
    }
    
    @Bean
    public PrometheusConfiguration loadConfiguration() {
        return PrometheusConfigurationLoader.loadFromEnv();
    }

    @Bean
    public FetchMarionetteNodesMetricsGateway fetchMarionetteNodesMetricsAdapter(PrometheusConfiguration config) {
        return new PrometheusFetchMarionetteNodesMetricsAdapter(config);
    }

    @Bean
    public VariationPointsExtractor variationPointsExtractor(ConfigRegistry globalRegistry) {
        return new VariationPointsExtractor(globalRegistry);
    }

    @Bean
    public SystemConfigurationsGenerator systemConfigurationsGenerator() {
        return new SystemConfigurationsGenerator();
    }

    @Bean
    public AbnTestExecutor abnTestExecutor(
        ConfigRegistry globalRegistry, 
        ControlMarionetteServiceBehaviourGateway controlMarionetteGateway,
        FetchMarionetteNodesMetricsGateway fetchMarionetteMetricsGateway,
        NonMarionetteNodesTracker nonMarionetteNodesTracker) {
        return new UniformAbnTestExecutor(globalRegistry, controlMarionetteGateway, fetchMarionetteMetricsGateway, nonMarionetteNodesTracker);
    }

    @Bean
    public NonMarionetteNodesTracker nonMarionetteNodesTracker(PrometheusConfiguration config) {
        return new PrometheusNonMarionetteNodesTracker(config);
    }

    @Bean
    public OrderedMetricsMetadataProvider metricsMetadataProvider(PrometheusConfiguration config) {
        return new PrometheusOrderedMetricsMetadataAdapter(config);
    }

    @Bean
    public SystemConfigurationsRanker ranker(SystemMetricsAggregator systemMetricsAggregator) {
        return new SystemConfigurationsRanker(systemMetricsAggregator);
    }
 
    @Bean SystemMetricsAggregator systemMetricsAggregator() {
        return new SystemMetricsAggregator();
    }

    @Bean 
    public AbnTestAllSystemConfigurationsUseCase abntestUseCase(
        VariationPointsExtractor variationPointsExtractor, 
        SystemConfigurationsGenerator systemConfigurationsGenerator, 
        AbnTestExecutor executor,
        SystemConfigurationsRanker ranker,
        AbnTestResultsStorage resultsStorage,
        OrderedMetricsMetadataProvider metricsMetadataProvider) {
        return new AbnTestAllSystemConfigurationsUseCaseImpl(
            variationPointsExtractor, 
            systemConfigurationsGenerator, 
            executor,
            ranker,
            resultsStorage,
            metricsMetadataProvider);
    }


    @Bean 
    public AbnTestResultsDownloadUseCase testResultsDownloadUseCase(AbnTestResultsStorage storage) {
        return new AbnTestResultsDownloadUseCaseImpl(storage);
    }
}
